/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.GlyphLift
import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PillOpenDrag
import com.agupta07505.smartisland.model.StepDrag
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.WidgetTapAction
import com.agupta07505.smartisland.model.idleWidgetTapAction
import com.agupta07505.smartisland.model.pillCenterTapHit
import com.agupta07505.smartisland.model.pillTapZone
import com.agupta07505.smartisland.model.portraitCenterTapIsIdleControl
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.readerTapAccepted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun TouchState.finishTouch() {
    // a plain tap on the picker was the picker's (its row or its button handled it)
    if (childScrolled || (pickerTouchAtStart && !isDragging)) return
    // The island closed under the finger (auto-collapse, the shade, a call)
    if (wasExpandedAtStart && !wiring.view.expanded) return
    if (glyphHoldArmed) {
        // a swipe held at the lift pulses the current card again, else the armed apps shortcut opens, else nothing
        when (HoldGesture.glyphLift(glyphSwiped, appsShortcutPending, tapGuard.exceeded)) {
            GlyphLift.Repulse -> wiring.actions.pulse.repulseCurrent()
            GlyphLift.OpenApps -> wiring.actions.system.openAppsLauncher()
            GlyphLift.Nothing -> Unit
        }
        return
    }
    if (HoldGesture.opensAppsOnLift(appsShortcutPending, tapGuard.exceeded)) {
        wiring.actions.system.openAppsLauncher()
        return
    }
    val currentNotification = wiring.view.notifications.getOrNull(wiring.view.selectedIndex)
    if (pillOpenMode == 1) {
        releasePillOpen()
    } else if (pillCloseMode == 1) {
        releasePillClose(currentNotification)
    } else if (wasExpandedAtStart) {
        releaseExpanded(currentNotification)
    } else {
        releaseCollapsed()
    }
}

private fun TouchState.releasePillOpen() {
    pillOpenMode = 0
    val committed = PillOpenDrag.commits(ui.pillOpen.progress)
    if (committed) IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
        HapticKind.Swipe)
    finishPillOpen(committed, wiring, ui, scope)
}

private fun TouchState.releasePillClose(currentNotification: IslandNotification?) {
    pillCloseMode = 0
    val committed = PillOpenDrag.closeCommits(ui.pillOpen.progress, -dragAccumulatorY, pillCloseThresholdPx)
    if (committed) runner.run(SwipeAction.Collapse, currentNotification)
    finishPillClose(committed, wiring, ui, scope)
}

private fun TouchState.settleStepDrag(hapticStrength: HapticStrength) {
    stepSmoothJob?.cancel()
    val committed = StepDrag.commits(stepTarget)
    val openedMode = stepMode
    stepMode = 0
    if (stepHaptic.onLift(committed)) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
    val settleMs = wiring.view.settings.animationSpeed(isLandscape)
        .durationMs(if (committed) StepDrag.SETTLE_MS else StepDrag.GO_BACK_SETTLE_MS)
    val startProgress = ui.stepDrag.progress
    scope.launch {
        Animatable(startProgress).animateTo(
            targetValue = if (committed) 1f else 0f,
            animationSpec = tween(
                durationMillis = settleMs,
                easing = FastOutSlowInEasing
            )
        ) {
            ui.stepDrag.progress = value
            ui.stepDrag.heightDp = ui.stepDrag.currentHeightDp()
        }
        if (!committed) {
            // Back to the old height: now the old step comes back (same height, so no jump)
            if (openedMode == 1) wiring.actions.card.back() else wiring.actions.card.deeper()
        }
        val settled = ui.stepDrag.heightDp
        var waited = 0L
        ui.stepDrag.progress = if (committed) 1f else 0f
        // the two copies of the card show the same step now
        ui.stepDrag.settled = true
        while (waited < StepDrag.HANDOVER_MAX_MS
            && abs(ui.stepDrag.shellHeightDp - settled) > StepDrag.HANDOVER_TOLERANCE_DP) {
            delay(16)
            waited += 16
        }
        ui.stepDrag.reset()
    }
}

private fun TouchState.releaseExpanded(currentNotification: IslandNotification?) {
    if (torchOpenAtStart) return
    // A selected text: a tap anywhere on the island drops it, a drag (the reader scrolling, a handle) keeps it, and
    // neither opens the app or swipes. A touch that just made the selection ends the same way
    if (stepMode != 1 && stepMode != -1) {
        if (selectingAtStart) {
            if (!tapGuard.exceeded) wiring.actions.card.clearTextSelection()
            return
        }
        if (wiring.view.textSelecting) return
    }
    val totalElapsedMs = System.currentTimeMillis() - pressTimeMs
    val swipeLocked = ui.swipeLock.isLocked(SystemClock.uptimeMillis())
    val swipeUpThreshold = -sensitivity.scaled(SWIPE_THRESHOLD_DP) * density
    val swipeDownThreshold = sensitivity.scaled(SWIPE_THRESHOLD_DP) * density
    val isHorizontalSwipe = isDragging && !swipeLocked && wiring.view.settings.enableSwipeActions &&
        abs(dragAccumulatorX) > sensitivity.scaled(EXPANDED_SIDE_SWIPE_THRESHOLD_DP) * density &&
        sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)
    val stepVertical = stepsActive && isDragging && !swipeLocked
        && sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)
    val hapticStrength = HapticStrength.fromId(wiring.view.settings.hapticStrength)
    if (stepMode == 1 || stepMode == -1) {
        settleStepDrag(hapticStrength)
    } else if (stepVertical && dragAccumulatorY > swipeDownThreshold) {
        if (wiring.actions.card.deeper()) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
    } else if (stepVertical && dragAccumulatorY < swipeUpThreshold) {
        if (wiring.actions.card.back()) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
    } else if (stepsActive && isHorizontalSwipe) {
    } else if (isHorizontalSwipe) {
        val action = if (dragAccumulatorX < 0f) {
            SwipeAction.fromId(wiring.view.settings.swipeLeftAction, SwipeAction.None)
        } else {
            SwipeAction.fromId(wiring.view.settings.swipeRightAction, SwipeAction.None)
        }
        commitExpandedSwipe(
            action, dragAccumulatorX, currentNotification, runner, ui, scope,
            wiring.view.notifications, wiring.view.selectedIndex
        )
    } else if (!stepsActive && isDragging && !swipeLocked && wiring.view.settings.enableSwipeActions
        && dragAccumulatorY < swipeUpThreshold &&
        sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)) {
        val isHold = isHoldRegistered || totalElapsedMs >= HOLD_GESTURE_THRESHOLD_MS
        val actionStr = if (isHold) wiring.view.settings.swipeHoldUpAction else wiring.view.settings.swipeUpAction
        val action = SwipeAction.fromId(actionStr, if (isHold) SwipeAction.DismissAll else SwipeAction.DismissCurrent)
        runner.run(action, currentNotification)
    } else if (!stepsActive && isDragging && !swipeLocked && wiring.view.settings.enableSwipeActions
        && dragAccumulatorY > swipeDownThreshold &&
        sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)) {
        val action = FIXED_EXPANDED_SWIPE_DOWN_ACTION
        if (wiring.actions.card.deeper()) {
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Swipe)
        } else {
            runner.run(action, currentNotification)
        }
    } else if (!tapGuard.exceeded && !pickerTouchAtStart && !selectorLocked && !cardLockedAtStart &&
        readerTapAccepted(readerStartedOnScroll, readerMovesAtDown, readerTracker.moveCount)) {
        if (!isHoldRegistered) {
            if (currentNotification != null) {
                wiring.actions.card.open(currentNotification)
            } else {
                SmartIslandRepositories.notificationRepository(context).resetTimer()
            }
        }
    }
}

private fun TouchState.releaseCollapsed() {
    if (pillGestureTriggered) return
    val isPillSwipeEnabled = wiring.view.settings.enablePillSwipeActions || idleWidgetSwipe()
    val fireDirection = if (isPillSwipeEnabled) pillArming.direction else null

    if (fireDirection != null) {
        pillGestureTriggered = true
        commitPillSwipe(fireDirection, dragAccumulatorX, wiring, runner, ui, scope, maxSlidePx)
    } else if ((isPillSwipeEnabled && crossedOnce) || tapGuard.exceeded) {
    } else if (!isHoldRegistered) {
        tapCollapsedPill()
    }
}

private fun TouchState.tapCollapsedPill() {
    // An event opened the island under the finger: this tap must not toggle it straight back shut
    // This tap started on the collapsed pill. Lifting won't close a card that just opened
    if (HoldGesture.tapOpenedUnderFinger(wasExpandedAtStart, wiring.view.expanded)) return
    val widgetTap = if (wiring.view.notifications.isEmpty()) {
        idleWidgetTapAction(
            widgetOnPill = ui.idleWidgetCycle != null && ui.idleFrameNow != null,
            compact = ui.idleFrameNow?.compact == true,
            zone = pillTapZone(
                downPosition.x, shellWidthPx, settings.width * density,
                bandShiftPx = bandShiftPx, bandWidthPx = bandWidthPx, side = slotSide
            ),
            hasStoredCards = wiring.view.hasIdleStoredCards
        )
    } else {
        WidgetTapAction.None
    }
    if (settings.autoHidePill && ui.isAutoHidden) {
        ui.isAutoHidden = false
        ui.touch()
    } else if (widgetTap == WidgetTapAction.GlyphTap && ui.onWidgetGlyphTap != null) {
        ui.onWidgetGlyphTap?.invoke()
    } else if (widgetTap == WidgetTapAction.ValueTap && ui.onWidgetValueTap != null) {
        // idle widget pill, value slot: same as the glyph slot, with its own hook
        ui.onWidgetValueTap?.invoke()
    } else if (wiring.view.notifications.isEmpty() && wiring.view.hasIdleStoredCards) {
        wiring.actions.idle.revealCards()
    } else if (portraitCenterTapIsIdleControl(isLandscape, settings.enableNotchMode,
        wiring.view.notifications.isNotEmpty()) &&
        pillCenterTapHit(
            xPx = downPosition.x, yPx = downPosition.y, shellWidthPx = shellWidthPx,
            pillWidthPx = settings.width * density,
            pillHeightPx = settings.height * density,
            bandShiftPx = bandShiftPx, bandWidthPx = bandWidthPx
        )
    ) {
        wiring.actions.idle.dotTap()
    } else if (isLandscape && !settings.enableNotchMode && wiring.view.notifications.isEmpty()) {
        wiring.actions.idle.dotTap()
    } else if (wiring.view.notifications.isNotEmpty()) {
        // (Shortcuts on an empty island need a hold, i.e. a long press.)
        wiring.actions.expand.toggle()
    }
}

internal fun TouchState.endLoop() {
    loopDone = true
    if (pillOpenMode == 1) {
        pillOpenMode = 0
        finishPillOpen(false, wiring, ui, scope)
    }
    if (pillCloseMode == 1) {
        // a child took the drag, or the island closed
        pillCloseMode = 0
        finishPillClose(!wiring.view.expanded, wiring, ui, scope)
    }
    if (stepMode == 1 || stepMode == -1) {
        stepSmoothJob?.cancel()
        stepMode = 0
        ui.stepDrag.reset()
    }
}

internal fun TouchState.closeTouch() {
    ui.pillTouchActive = false
    // Collapsed pill's lean still off after the release ease gets one more ease
    if (!wasExpandedAtStart) schedulePillSettleGuard(ui, scope)
    if (!loopDone) {
        holdJob.cancel()
        if (pillOpenMode == 1) {
            pillOpenMode = 0
            finishPillOpen(false, wiring, ui, scope)
        }
        if (pillCloseMode == 1) {
            pillCloseMode = 0
            finishPillClose(!wiring.view.expanded, wiring, ui, scope)
        }
        if (stepMode == 1 || stepMode == -1) {
            stepSmoothJob?.cancel()
            if (stepMode == 1) wiring.actions.card.back() else wiring.actions.card.deeper()
            stepMode = 0
            ui.stepDrag.reset()
        }
        ui.pillContentDragX = 0f
        ui.pillDragOffsetX = 0f
        ui.dragOffset = 0f
        ui.cardDragX = 0f
        ui.swipeDownPull = 0f
        ui.pendingCardEnterSign = 0
        ui.pendingCardFromKey = null
        ui.pendingEnterSign = 0
        ui.cardSwipeSnap = false
        scope.launch {
            ui.deadZone.snapTo(0f)
            ui.pillArm.snapTo(0f)
        }
        scope.launch { ui.holdGlow.animateTo(0f, swipeCalmSpring()) }
        if (pillOpenMode == 1) {
            pillOpenMode = 0
            finishPillOpen(false, wiring, ui, scope)
        }
        easeBackPillMotion(ui, scope)
        if (ui.cardDragX != 0f) easeCardBack(ui, scope)
    }
}

internal fun TouchState.finishGesture() {
    holdJob.cancel()
    ui.stepDrag.swipeNeighbour = 0
    // The incoming card (if the swipe changed it) takes over the content
    easeBackPillMotion(ui, scope)
    if (ui.pendingCardEnterSign == 0 && ui.cardDragX != 0f) {
        easeCardBack(ui, scope)
    }
    if (ui.pillArm.value != 0f) {
        scope.launch { ui.pillArm.animateTo(0f, swipeCalmSpring()) }
    }
    if (ui.deadZone.value != 0f) {
        scope.launch { ui.deadZone.animateTo(0f, swipeCalmSpring()) }
    }
    if (ui.holdGlow.value != 0f) {
        scope.launch { ui.holdGlow.animateTo(0f, swipeCalmSpring()) }
    }
}

internal fun finishPillOpen(commit: Boolean, wiring: IslandWiring, ui: IslandInteractionState, scope: CoroutineScope) {
    val drag = ui.pillOpen
    drag.settleJob?.cancel()
    // Something closed the island meanwhile (the card is gone)
    val open = commit && wiring.view.expanded
    drag.closing = !open
    if (!open) wiring.actions.expand.dragExpand(false)
    val from = drag.progress
    drag.settleJob = scope.launch {
        Animatable(from).animateTo(
            targetValue = if (open) 1f else 0f,
            animationSpec = swipeEaseBackSpring()
        ) { drag.progress = value }
        var waited = 0L
        while (waited < PILL_OPEN_HANDBACK_MAX_MS && !drag.springsAtRest(open)) {
            delay(16)
            waited += 16
        }
        drag.active = false
        drag.progress = 0f
        drag.closing = false
        drag.peekNext = false
    }
}

private const val PILL_OPEN_HANDBACK_MAX_MS = 700L

private const val PILL_CLOSE_REFUSED_CHECK_MS = 120L

internal fun finishPillClose(collapse: Boolean, wiring: IslandWiring, ui: IslandInteractionState,
        scope: CoroutineScope) {
    val drag = ui.pillOpen
    drag.settleJob?.cancel()
    drag.closing = collapse
    drag.settleJob = scope.launch {
        var open = !collapse
        val settle: (Float) -> Job = { to ->
            launch {
                Animatable(drag.progress).animateTo(
                    targetValue = to,
                    animationSpec = swipeEaseBackSpring()
                ) { drag.progress = value }
            }
        }
        var anim = settle(if (open) 1f else 0f)
        if (collapse) {
            delay(PILL_CLOSE_REFUSED_CHECK_MS)
            // Refused (too soon after a toggle): back to open
            if (wiring.view.expanded) {
                anim.cancel()
                open = true
                drag.closing = false
                anim = settle(1f)
            }
        }
        anim.join()
        var waited = 0L
        while (waited < PILL_OPEN_HANDBACK_MAX_MS && !drag.springsAtRest(open)) {
            delay(16)
            waited += 16
        }
        drag.active = false
        drag.progress = 0f
        drag.closing = false
        drag.peekNext = false
    }
}
