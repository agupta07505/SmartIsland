/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.PILL_CONTENT_FOLLOW
import com.agupta07505.smartisland.model.PillOpenDrag
import com.agupta07505.smartisland.model.PillSwipeDirection
import com.agupta07505.smartisland.model.StepDrag
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.model.SwipeFeel
import com.agupta07505.smartisland.model.SwipeSideways
import com.agupta07505.smartisland.model.deadZoneCue
import com.agupta07505.smartisland.model.launcherOwnsDrag
import com.agupta07505.smartisland.model.softFollow
import com.agupta07505.smartisland.model.stepFollowsFinger
import com.agupta07505.smartisland.model.swipeChangesCard
import com.agupta07505.smartisland.ui.expanded.readingTextOwnsDrag
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsDrag
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.SwipeCardOutcome
import com.agupta07505.smartisland.util.predictCardChange
import com.agupta07505.smartisland.util.swipeIsNoOp
import com.agupta07505.smartisland.util.swipeNeighbourHint
import kotlinx.coroutines.launch
import kotlin.math.abs

/** One pointer event of the touch; true when the touch is over. */
internal fun TouchState.onPointerChange(change: PointerInputChange): Boolean {
    if (change.pressed && change.positionChange() != Offset.Zero) {
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastDragResetMs >= 100L) {
            lastDragResetMs = nowMs
            ui.onDragActive?.invoke()
        }
    }

    // A cancelled touch arrives as an up that is already consumed: the touch is over, so the hold timer must not run on
    // (a picker's own consumed up is handled just below)
    if (!pickerTouchAtStart && !change.pressed && change.changedToUpIgnoreConsumed() && !change.changedToUp()) {
        holdJob.cancel()
        return true
    }

    // a picker row consumes its own (accepted) tap
    if (change.changedToUp() || (pickerTouchAtStart && change.changedToUpIgnoreConsumed())) {
        if (!textSelectedNow()) change.consume()
        holdJob.cancel()
        finishTouch()
        return true
    }
    if (wasExpandedAtStart && !wiring.view.expanded) {
        holdJob.cancel()
        return true
    }
    val dragAmountY = change.positionChange().y
    val dragAmountX = change.positionChange().x
    if (wasExpandedAtStart && (childScrolled ||
            (change.isConsumed && abs(dragAmountY) > abs(dragAmountX)) ||
            // a drag on the picker rows belongs to the list even at its top/bottom edge
            siblingPickerOwnsDrag(
                pickerOpenAtStart, downYDp, dragAmountX, dragAmountY,
                sideBySide = isLandscape, downXFraction = downXFraction,
                pickerLeftFraction = pickerLeftFraction.value,
                pickerOnLeft = pickerOnLeft.value) ||
            readingTextOwnsDrag(readingTextAtStart && !stepsActive, dragAmountX, dragAmountY) ||
            // The launcher window's own page swipes
            launcherOwnsDrag(launcherAtStart, dragAmountX, dragAmountY))
    ) {
        childScrolled = true
        holdJob.cancel()
    return false
    }
    if (readerHasFirstClaim(dragAmountX, dragAmountY)) {
        // Not consumed and not followed: the reader's scroll only starts when nothing above it has taken the move
        tapGuard.update(readerProbeX, readerProbeY)
        return false
    }
    if (abs(dragAmountY) > 0.5f || abs(dragAmountX) > 0.5f) followDrag(change, dragAmountX, dragAmountY)
    return false
}

/** Leave the touch unconsumed while text is selected, except a plain tap on an existing selection (the outside tap would see it too). */
private fun TouchState.textSelectedNow(): Boolean =
    (selectingAtStart || wiring.view.textSelecting) &&
        (isHoldRegistered || tapGuard.exceeded || !selectingAtStart)

/**
 * The island moved with the text on slow swipes. Give the reader a few dp to claim the drag
 * before the island starts moving.
 */
private fun TouchState.readerHasFirstClaim(dx: Float, dy: Float): Boolean {
    if (!wasExpandedAtStart || stepMode != 0 || ui.stepDrag.active) return false
    if (wiring.view.cardDepth != CardDepth.Reader) return false
    val scroll = ui.stepDrag.readerScroll ?: return false
    readerProbeX += dx
    readerProbeY += dy
    if (abs(readerProbeY) <= abs(readerProbeX) || abs(readerProbeY) > READER_YIELD_DP * density) return false
    return if (readerProbeY < 0f) scroll.canScrollForward else scroll.canScrollBackward
}

/** How far a drag on a scrollable reader belongs to its scroll before the card's steps may start. */
private const val READER_YIELD_DP = 24f

private fun TouchState.followDrag(change: PointerInputChange, dragAmountX: Float, dragAmountY: Float) {
    accumulateDrag(dragAmountX, dragAmountY)
    updatePillOpenDrag()
    updateStepDrag()
    updatePillCloseDrag()
    updateSwipeDownPull()
    if (!textSelectedNow()) change.consume()
    if (abs(dragAccumulatorY) > 5f * density || abs(dragAccumulatorX) > 5f * density) {
        holdJob.cancel()
    }
    if (wasExpandedAtStart) {
        followExpanded()
    } else if (pillOpenMode == 1) {
        // The island itself follows the finger (ui.pillOpen)
        ui.pillDragOffsetX = 0f
        ui.dragOffset = 0f
        ui.pillContentDragX = 0f
        crossedOnce = true
    } else {
        followCollapsed()
    }
    updateDeadZoneCue()
}

private fun TouchState.accumulateDrag(dragAmountX: Float, dragAmountY: Float) {
    isDragging = true
    rawDragX += dragAmountX
    rawDragY += dragAmountY
    if (axisLock.update(rawDragX, rawDragY)) {
        // Drop what the other axis gathered before the lock
        if (axisLock.axis == AxisLock.Axis.Vertical) dragAccumulatorX = 0f else dragAccumulatorY = 0f
    }
    dragAccumulatorY += axisLock.filterY(dragAmountY)
    dragAccumulatorX += axisLock.filterX(dragAmountX)
    tapGuard.update(dragAccumulatorX, dragAccumulatorY)
}

private fun TouchState.updatePillOpenDrag() {
    if (pillOpenMode == 0 && dragAccumulatorY > tapSlop &&
        sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY) && pillOpenAllowed()
    ) {
        val pillOpen = ui.pillOpen
        val wasActive = pillOpen.active
        pillOpen.settleJob?.cancel()
        if (!wasActive) pillOpen.progress = 0f
        pillOpen.active = true
        if (wiring.actions.expand.dragExpand(true)) {
            pillOpenMode = 1
            pillOpenRangePx = PillOpenDrag.rangePx(settings.height, pillOpen.cardHeightDp(), density)
            // The pill's own swipe arming and hold start over and stay off for this touch
            pillArming.reset()
            holdJob.cancel()
            ui.touch()
        } else {
            pillOpenMode = 2
            if (wasActive) finishPillOpen(false, wiring, ui, scope) else pillOpen.active = false
        }
    }
    if (pillOpenMode == 1) {
        val openProgress = PillOpenDrag.progress(dragAccumulatorY, tapSlop, pillOpenRangePx)
        ui.pillOpen.progress = openProgress
        val pastCommit = PillOpenDrag.commits(openProgress)
        if (pastCommit != pillOpenArmed) {
            pillOpenArmed = pastCommit
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
        }
    }
}

private fun TouchState.updateStepDrag() {
    startStepDrag()
    if (stepMode == 1 || stepMode == -1) {
        stepTarget = StepDrag.progress(dragAccumulatorY * stepMode, tapSlop, stepRangePx)
        if (stepHaptic.onProgress(stepTarget)) {
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Swipe)
        }
    }
}

private fun TouchState.startStepDrag() {
    // a card step follows the finger
    if (wasExpandedAtStart && wiring.view.settings.enableSwipeActions && !pickerOpenAtStart && stepMode == 0 &&
        !ui.swipeLock.isLocked(SystemClock.uptimeMillis()) &&
        sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY) && abs(dragAccumulatorY) > tapSlop
    ) {
        val depthNow = wiring.view.cardDepth
        stepMode = when {
            torchOpenAtStart -> 2
            dragAccumulatorY > 0f -> if (wiring.actions.card.deeper()) 1 else 2
            depthNow != CardDepth.First -> if (wiring.actions.card.back()) -1 else 2
            else -> 2
        }
        if (stepFollowsFinger(stepMode)) {
            // a step decides on lift by its own progress
            expandedArming.reset()
            ui.stepDrag.fromDp = ui.stepDrag.shellHeightDp
            ui.stepDrag.progress = 0f
            ui.stepDrag.cardKey = wiring.view.notifications.getOrNull(wiring.view.selectedIndex)?.key
            ui.stepDrag.heightDp = ui.stepDrag.fromDp
            ui.stepDrag.active = true
            stepTarget = 0f
            stepSmoothJob?.cancel()
            stepSmoothJob = scope.launch {
                var lastNanos = 0L
                while (true) {
                    withFrameNanos { now ->
                        val dtMs = if (lastNanos == 0L) 16f else ((now - lastNanos) / 1_000_000f).coerceIn(1f, 50f)
                        lastNanos = now
                        val shown = ui.stepDrag.progress
                        val next = StepDrag.smoothed(shown, stepTarget, dtMs)
                        if (next != shown) {
                            ui.stepDrag.progress = next
                            ui.stepDrag.heightDp = ui.stepDrag.currentHeightDp()
                        }
                    }
                }
            }
        }
    }
}

private fun TouchState.updatePillCloseDrag() {
    if (pillCloseMode == 0 && stepMode == 2 && dragAccumulatorY < -tapSlop &&
        sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)
    ) {
        if (pillCloseAllowed()) {
            val closeDrag = ui.pillOpen
            closeDrag.settleJob?.cancel()
            closeDrag.progress = 1f
            closeDrag.closing = false
            closeDrag.peekNext = true
            closeDrag.active = true
            pillCloseMode = 1
            pillCloseRangePx = PillOpenDrag.rangePx(settings.height, closeDrag.cardHeightDp(), density)
            expandedArming.reset()
            holdJob.cancel()
            ui.touch()
        } else {
            pillCloseMode = 2
        }
    }
    if (pillCloseMode == 1) {
        val dragUp = -dragAccumulatorY
        val openProgress = PillOpenDrag.closeProgress(dragUp, tapSlop, pillCloseRangePx)
        ui.pillOpen.progress = openProgress
        val pastCommit = PillOpenDrag.closeCommits(openProgress, dragUp, pillCloseThresholdPx)
        if (pastCommit != pillCloseArmed) {
            pillCloseArmed = pastCommit
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
        }
    }
}

private fun TouchState.updateSwipeDownPull() {
    val pullAllowed = if (wasExpandedAtStart) {
        !torchOpenAtStart && wiring.view.settings.enableSwipeActions && !pickerOpenAtStart &&
            (stepsActive || FIXED_EXPANDED_SWIPE_DOWN_ACTION != SwipeAction.None)
    } else {
        !pillGestureTriggered && wiring.view.settings.enablePillSwipeActions &&
            FIXED_PILL_SWIPE_DOWN_ACTION != SwipeAction.None
    } && sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY) && pillOpenMode != 1
    ui.swipeDownPull = if (pullAllowed && stepMode != 1 && stepMode != -1) {
        SwipeDownPull.progress(
            dragAccumulatorY, tapSlop,
            (if (wasExpandedAtStart) sensitivity.scaled(SWIPE_THRESHOLD_DP) else sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP)) *
                density
        )
    } else 0f
}

private fun TouchState.followExpanded() {
    if (torchOpenAtStart) {
        ui.dragOffset = 0f
        return
    }
    tickExpandedArming()
    leanExpandedCard()
    // a swipe down only leans the island a little; a swipe up follows the finger
    val swipeUpAvailable = wiring.view.settings.enableSwipeActions &&
        SwipeAction.fromId(wiring.view.settings.swipeUpAction, SwipeAction.DismissCurrent).let {
            it != SwipeAction.None && !swipeIsNoOp(it, wiring.view.notifications, wiring.view.selectedIndex)
        }
    ui.dragOffset = if (dragAccumulatorY > 0f) {
        SwipeDownPull.expandedDownOffsetPx(dragAccumulatorY, density)
    } else if (pillCloseMode == 1 || stepMode == 1 || stepMode == -1 || stepsActive || !swipeUpAvailable) {
        0f
    } else {
        dragAccumulatorY.coerceAtLeast(-DRAG_MAX_OFFSET_DP * density)
    }
}

private fun TouchState.tickExpandedArming() {
    if (wiring.view.settings.enableSwipeActions && !stepFollowsFinger(stepMode) && pillCloseMode != 1 &&
        expandedArming.update(dragAccumulatorX, dragAccumulatorY, SystemClock.uptimeMillis())
    ) {
        // Tick when lifting would fire
        val dir = expandedArming.direction
        val live = if (stepsActive) {
            // Only up and down mean something in the preview / reader / picture steps
            dir == PillSwipeDirection.Up || dir == PillSwipeDirection.Down
        } else when (dir) {
            PillSwipeDirection.Left -> SwipeAction.fromId(wiring.view.settings.swipeLeftAction, SwipeAction.None).let {
                it != SwipeAction.None && !swipeIsNoOp(it, wiring.view.notifications, wiring.view.selectedIndex)
            }
            PillSwipeDirection.Right -> SwipeAction.fromId(wiring.view.settings.swipeRightAction, SwipeAction.None).let {
                it != SwipeAction.None && !swipeIsNoOp(it, wiring.view.notifications, wiring.view.selectedIndex)
            }
            else -> true
        }
        if (expandedArming.enteredLock) {
            // Pulled back: a dull buzz instead of a tick, and the lock cue
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Lock)
            ui.startLockCue(scope)
        } else if (live) {
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
        }
    }
}

private fun TouchState.leanExpandedCard() {
    // The island leans sideways with the finger
    ui.cardDragX = if (cardFollows && sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)) {
        val sideAction = expandedSideAction(wiring.view.settings, dragAccumulatorX)
        when {
            sideAction == SwipeAction.None || swipeIsNoOp(sideAction, wiring.view.notifications,
                wiring.view.selectedIndex) -> 0f
            // a swipe that will bring another card leans fully
            swipeChangesCard(sideAction) &&
                predictCardChange(sideAction, wiring.view.notifications,
                    wiring.view.selectedIndex) != SwipeCardOutcome.Stays -> {
                val hint = swipeNeighbourHint(sideAction)
                if (ui.stepDrag.swipeNeighbour != hint) ui.stepDrag.swipeNeighbour = hint
                SwipeSideways.leanPx(dragAccumulatorX, density)
            }
            else -> SwipeSideways.leanPx(
                dragAccumulatorX, density, SwipeSideways.STAY_SHARE
            )
        }
    } else 0f
}

private fun TouchState.followCollapsed() {
    ui.pillDragOffsetX = softFollow(dragAccumulatorX, 0.35f, 24f * density)
    ui.dragOffset = softFollow(
        dragAccumulatorY, 0.35f, SwipeFeel.PILL_LEAN_Y_MAX_DP * density
    )

    val pillHorizontal = sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)
    ui.pillContentDragX = if (pillHorizontal) {
        softFollow(dragAccumulatorX, PILL_CONTENT_FOLLOW, maxSlidePx)
    } else 0f
    if (pillHorizontal && System.currentTimeMillis() - ui.userInteractionTimestamp >= PILL_SWIPE_TOUCH_GAP_MS) {
        ui.swipeTouched()
    }

    if (abs(dragAccumulatorX) >= pillThresholdPx || abs(dragAccumulatorY) >= pillThresholdPx) {
        crossedOnce = true
    }
    if (glyphHoldArmed) updateGlyphSwipe()
    if (!pillGestureTriggered && !glyphHoldArmed && (wiring.view.settings.enablePillSwipeActions || idleWidgetSwipe())) {
        if (pillArming.update(dragAccumulatorX, dragAccumulatorY, SystemClock.uptimeMillis())) {
            holdJob.cancel()
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
            val armed = pillArming.direction != null
            scope.launch { ui.pillArm.animateTo(if (armed) 1f else 0f, swipeCalmSpring()) }
        }
    }
}

/** After a glyph hold: the glow gives way to the arm cue (with its tick) while a left or right swipe is held. */
private fun TouchState.updateGlyphSwipe() {
    val now = HoldGesture.glyphSwiped(
        dragAccumulatorX, dragAccumulatorY, tapSlop, sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)
    )
    if (now == glyphSwiped) return
    glyphSwiped = now
    IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
    scope.launch { ui.pillArm.animateTo(if (now) 1f else 0f, swipeCalmSpring()) }
    scope.launch { ui.holdGlow.animateTo(if (now) 0f else 1f, swipeCalmSpring()) }
}

private fun TouchState.updateDeadZoneCue() {
    val liftFires = if (wasExpandedAtStart) {
        wiring.view.settings.enableSwipeActions &&
            expandedSwipeFires(expandedArming.direction, stepsActive, wiring)
    } else {
        !pillGestureTriggered && (wiring.view.settings.enablePillSwipeActions || idleWidgetSwipe())
            && pillArming.direction != null
    }
    showDeadZone(!pillGestureTriggered && !glyphHoldArmed && pillOpenMode != 1 && pillCloseMode != 1
        && deadZoneCue(tapGuard.exceeded, liftFires, stepFollowsFinger(stepMode)))
}
