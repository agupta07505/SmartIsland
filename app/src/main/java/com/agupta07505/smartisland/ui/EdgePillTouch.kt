/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgePillAction
import com.agupta07505.smartisland.model.EdgePillLayout
import com.agupta07505.smartisland.model.EdgePillLayoutMath
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgePillZone
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.GlyphLift
import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchCompanionPlan
import com.agupta07505.smartisland.model.PILL_CONTENT_FOLLOW
import com.agupta07505.smartisland.model.PillOpenDrag
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeArming
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.model.TapGuard
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TouchZone
import com.agupta07505.smartisland.model.softFollow
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_EVENT_MS
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class EdgeDragResult(val liftAction: EdgePillAction, val end: Offset)

internal class EdgePillTouch(
    val ui: IslandInteractionState,
    val wiring: IslandWiring,
    val runner: IslandActionRunner,
    val scope: CoroutineScope,
    val context: Context,
    val enterWait: Array<Job?>,
    val pillBounds: Array<Rect?>,
    val hiddenState: State<Boolean>,
    val revealState: State<() -> Unit>,
    val layoutState: State<EdgePillLayout>,
    val openState: State<Boolean>,
    val awayState: State<Boolean>,
    val notificationState: State<IslandNotification?>,
    val planState: State<NotchCompanionPlan>,
    val clearanceState: State<() -> Float>,
    val cardWidthState: State<Float>
) {
    suspend fun AwaitPointerEventScope.handle() {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        ui.pillReleaseJob?.cancel()
        if (hiddenState.value) {
            // auto-hidden: the first touch only brings the pill back, it runs no action.
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                if (change == null || change.changedToUp()) break
            }
            revealState.value()
            return
        }
        ui.pillTouchActive = true
        ui.pillSettleGuardJob?.cancel()
        try {
            val cam = layoutState.value.camera
            val pillNow = layoutState.value.pill
            val sensitivity = GestureSensitivity.fromId(wiring.view.settings.gestureSensitivity)
            val slopPx = EdgePillRules.tapSlopDp(sensitivity) * density
            val yDp = pillNow.top + down.position.y / density
            val zone = EdgePillRules.zoneInPill(
                yDp, pillNow, EdgePillLayoutMath.sideZone(layoutState.value),
                EdgePillLayoutMath.holeCentreYDp(layoutState.value)
            )
            val openAtDown = openState.value
            var end = down.position
            var moved = false
            var released = false
            val bandSlopPx = sensitivity.scaled(TapGuard.CAMERA_BAND_SIDEWAYS_SLOP_DP) * density
            val tapGuard = TapGuard(slopPx, if (zone == EdgePillZone.Middle) minOf(slopPx, bandSlopPx) else slopPx)
            var replay: PointerInputChange? = null
            withTimeoutOrNull(HOLD_GESTURE_THRESHOLD_MS) {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null) { released = true; break }
                    end = change.position
                    tapGuard.update(end.y - down.position.y, end.x - down.position.x)
                    if (tapGuard.exceeded) { moved = true; replay = change; break }
                    if (change.changedToUp()) { released = true; break }
                }
            }
            if (!released && !moved) {
                val holdAction = EdgePillRules.hold(zone)
                if (holdAction == EdgePillAction.AppsShortcut) {
                    val glyphHold = EdgePillRules.glyphHold(
                        zone, notificationState.value != null, openState.value, awayState.value,
                        ui.widgetIslandOpen || ui.widgetIslandShown
                    )
                    if (glyphHold) {
                        glyphHoldPhase(down, tapGuard, cam, slopPx)
                        return
                    }
                    // As portrait: the hold arms silently and apps open when the finger lifts, unless it left the tap
                    // area
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        tapGuard.update(change.position.y - down.position.y, change.position.x - down.position.x)
                        if (change.changedToUp()) {
                            if (HoldGesture.opensAppsOnLift(true, tapGuard.exceeded)) {
                                runEdgeAction(holdAction, wiring, runner, notificationState.value)
                            }
                            break
                        }
                    }
                    return
                }
                if (holdAction != EdgePillAction.None) {
                    IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                        HapticKind.Hold)
                }
                // a hold: camera middle runs the normal long-press action
                runEdgeAction(holdAction, wiring, runner, notificationState.value)
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.changedToUp()) break
                }
                return
            }
            var liftAction = EdgePillAction.None
            if (moved) {
                val dragResult = dragPhase(down, end, replay, cam, sensitivity, slopPx) ?: return
                liftAction = dragResult.liftAction
                end = dragResult.end
            }
            val away = awayState.value
            val action = if (moved) {
                liftAction
            } else {
                val otherPill = planState.value.pillStays && clearanceState.value() > 0.5f
                EdgePillRules.tap(zone, away, openState.value, otherPill)
            }
            // An event opened the island under the finger: a plain tap must not toggle it straight back shut
            if (!moved && HoldGesture.tapOpenedUnderFinger(openAtDown, openState.value) &&
                action == EdgePillAction.ToggleCard) return
            if (moved && action != EdgePillAction.None) ui.touch()
            if (action == EdgePillAction.SelectOther) {
                ui.markCutout(TapSwap.CUTOUT_EDGE_PILL) { pillBounds[0] }
            }
            runEdgeAction(
                action, wiring, runner, notificationState.value, swipeDy = end.y - down.position.y,
                otherIndex = planState.value.pillCardIndex
            )
        } finally {
            ui.pillTouchActive = false
            schedulePillSettleGuard(ui, scope)
        }
    }

    /**
     * A hold on a glyph end of the collapsed pill: the hold buzzes and glows, then a drag along the edge, either way,
     * pulses the card again when the finger lifts there, and a lift without it opens the apps shortcut
     */
    private suspend fun AwaitPointerEventScope.glyphHoldPhase(
        down: PointerInputChange,
        tapGuard: TapGuard,
        cam: EdgeCamera,
        slopPx: Float
    ) {
        val strength = HapticStrength.fromId(wiring.view.settings.hapticStrength)
        IslandHaptics.play(context, strength, HapticKind.Hold)
        scope.launch { ui.holdGlow.animateTo(1f, tween(HOLD_GLOW_IN_MS)) }
        val glyphMaxSlidePx = EdgePillRules.glyphMaxSlideDp(layoutState.value.pill.height) * density
        var swiped = false
        var lifted = false
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                val along = change.position.y - down.position.y
                val across = change.position.x - down.position.x
                tapGuard.update(along, across)
                val now = EdgePillRules.glyphSwiped(along, across, slopPx)
                if (now != swiped) {
                    swiped = now
                    IslandHaptics.play(context, strength, HapticKind.Merge)
                    scope.launch { ui.pillArm.animateTo(if (now) 1f else 0f, swipeCalmSpring()) }
                    scope.launch { ui.holdGlow.animateTo(if (now) 0f else 1f, swipeCalmSpring()) }
                }
                ui.pillContentDragX = softFollow(along, PILL_CONTENT_FOLLOW, glyphMaxSlidePx)
                ui.pillDragOffsetX = EdgePillRules.leanAlongPx(along, density)
                ui.dragOffset = EdgePillRules.leanAcrossPx(EdgePillRules.armInto(across, cam.onLeft), density)
                if (change.changedToUp()) { lifted = true; break }
            }
        } finally {
            easeBackPillMotion(ui, scope)
            if (ui.pillArm.value != 0f) scope.launch { ui.pillArm.animateTo(0f, swipeCalmSpring()) }
            scope.launch { ui.holdGlow.animateTo(0f, swipeCalmSpring()) }
        }
        if (!lifted) return
        when (HoldGesture.glyphLift(swiped, appsArmed = true, slopExceeded = tapGuard.exceeded)) {
            GlyphLift.Repulse -> {
                ui.touch()
                wiring.actions.pulse.repulseCurrent()
            }
            GlyphLift.OpenApps -> runEdgeAction(EdgePillAction.AppsShortcut, wiring, runner, notificationState.value)
            GlyphLift.Nothing -> Unit
        }
    }

    private suspend fun AwaitPointerEventScope.dragPhase(
        down: PointerInputChange,
        start: Offset,
        firstChange: PointerInputChange?,
        cam: EdgeCamera,
        sensitivity: GestureSensitivity,
        slopPx: Float
    ): EdgeDragResult? {
        var end = start
        var replay = firstChange
        var liftAction = EdgePillAction.None
        // a drag into the screen on a pill with a card opens the island under the finger
        val pillSwipes = wiring.view.settings.enablePillSwipeActions
        val canOpen = pillSwipes && !openState.value && !awayState.value && notificationState.value != null &&
            !ui.widgetIslandOpen && !ui.widgetIslandShown
        var openMode = 0
        var openRangePx = 0f
        var armed = false
        var lifted = false
        val pillThresholdPx = sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP) * density
        val axisLock = AxisLock(slopPx)
        val arming = SwipeArming(sensitivity, pillThresholdPx)
            .also { it.lockWhen = { false } }
        var armShown = false
        var deadShown = false
        fun showCues(zone: TouchZone) {
            val armedNow = zone == TouchZone.Armed
            val deadNow = zone == TouchZone.Dead
            if (armedNow != armShown) {
                armShown = armedNow
                IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
                scope.launch { ui.pillArm.animateTo(if (armedNow) 1f else 0f, swipeCalmSpring()) }
            }
            if (deadNow != deadShown) {
                deadShown = deadNow
                scope.launch { ui.deadZone.animateTo(if (deadNow) 1f else 0f, swipeCalmSpring()) }
            }
        }
        val glyphMaxSlidePx = EdgePillRules.glyphMaxSlideDp(layoutState.value.pill.height) * density
        try {
            while (true) {
                val change = replay?.also { replay = null }
                    ?: (awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break)
                end = change.position
                // a card open leaves the cutout with taps only
                val swipesOn = !openState.value && EdgePillRules.swipesOn(
                    openState.value, wiring.view.settings.enablePillSwipeActions, wiring.view.settings.enableSwipeActions
                )
                axisLock.update(end.x - down.position.x, end.y - down.position.y)
                val alongPx = axisLock.filterY(end.y - down.position.y)
                val intoDragPx = EdgePillRules.armInto(axisLock.filterX(end.x - down.position.x), cam.onLeft)
                arming.update(
                    EdgePillRules.armAlong(alongPx),
                    intoDragPx,
                    SystemClock.uptimeMillis()
                )
                if (openMode == 0 && canOpen && EdgePillRules.opensUnderFinger(
                        (end.x - down.position.x) / density, (end.y - down.position.y) / density, cam.onLeft,
                        EdgePillRules.tapSlopDp(sensitivity)
                    )
                ) {
                    val drag = ui.pillOpen
                    val wasActive = drag.active
                    drag.settleJob?.cancel()
                    if (!wasActive) { drag.progress = 0f; drag.peekNext = false }
                    drag.closing = false
                    drag.active = true
                    if (wiring.actions.expand.dragExpand(true)) {
                        openMode = 1
                        openRangePx = PillOpenDrag.rangePx(layoutState.value.pill.width, cardWidthState.value, density)
                        ui.touch()
                    } else {
                        openMode = 2
                        if (wasActive) finishPillOpen(false, wiring, ui, scope) else drag.active = false
                    }
                }
                if (openMode == 1) {
                    // the island itself follows the finger now: the pill does not lean or slide
                    ui.pillDragOffsetX = 0f
                    ui.dragOffset = 0f
                    ui.pillContentDragX = 0f
                    val into = if (cam.onLeft) end.x - down.position.x else down.position.x - end.x
                    val p = PillOpenDrag.progress(into, slopPx, openRangePx)
                    ui.pillOpen.progress = p
                    if (PillOpenDrag.peeksNextEvent(p)) ui.pillOpen.peekNext = true
                    val past = PillOpenDrag.commits(p)
                    if (past != armed) {
                        armed = past
                        IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                            HapticKind.Merge)
                    }
                }
                // the island opening under the finger has its own cue
                showCues(
                    if (openMode == 1) TouchZone.Tap
                    else EdgePillRules.liftZone(arming.direction, openState.value, awayState.value, swipesOn)
                )
                val pullIntoPx = intoDragPx
                ui.swipeDownPull = if (EdgePillRules.pullAllowed(
                        swipesOn, openState.value, awayState.value, canOpen && openMode != 2,
                        sensitivity.isVertical(EdgePillRules.armAlong(alongPx), pullIntoPx)
                    ) && FIXED_PILL_SWIPE_DOWN_ACTION != SwipeAction.None
                ) SwipeDownPull.progress(pullIntoPx, slopPx, pillThresholdPx) else 0f
                // The collapsed cutout beside an open island isn't swipable, as in portrait
                if (openMode != 1 && !openState.value && !awayState.value && notificationState.value != null) {
                    val along = axisLock.axis == AxisLock.Axis.Vertical
                    ui.pillContentDragX = if (along) {
                        softFollow(alongPx, PILL_CONTENT_FOLLOW, glyphMaxSlidePx)
                    } else 0f
                    val otherPillLeans = openState.value && planState.value.pillStays
                    if (!openState.value || otherPillLeans) {
                        ui.pillDragOffsetX = EdgePillRules.leanAlongPx(alongPx, density)
                    }
                    if (!openState.value) {
                        ui.dragOffset = EdgePillRules.leanAcrossPx(intoDragPx, density)
                    }
                    if (along && System.currentTimeMillis() - ui.userInteractionTimestamp >= PILL_SWIPE_TOUCH_GAP_MS) ui.swipeTouched()
                }
                if (change.changedToUp()) { lifted = true; break }
            }
            if (lifted && openMode != 1 && !openState.value) {
                liftAction = EdgePillRules.armedAction(
                    arming.direction, openState.value, awayState.value,
                    EdgePillRules.swipesOn(openState.value, wiring.view.settings.enablePillSwipeActions,
                        wiring.view.settings.enableSwipeActions)
                )
            }
            if (liftAction == EdgePillAction.SwitchCard && notificationState.value != null) {
                expectEdgeEnter(ui, scope, enterWait, EdgePillRules.enterSign(end.y - down.position.y))
            }
        } finally {
            // lift, cancel (the handler restarted) or a drag that turned off the edge axis
            easeBackPillMotion(ui, scope)
            if (ui.pillArm.value != 0f) scope.launch { ui.pillArm.animateTo(0f, swipeCalmSpring()) }
            if (ui.deadZone.value != 0f) scope.launch { ui.deadZone.animateTo(0f, swipeCalmSpring()) }
        }
        if (openMode == 1) {
            val committed = lifted && PillOpenDrag.commits(ui.pillOpen.progress)
            if (committed) IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength),
                HapticKind.Swipe)
            finishPillOpen(committed, wiring, ui, scope)
            return null
        }
        return EdgeDragResult(liftAction, end)
    }
}

internal fun edgeEnterSpring(animSpeed: AnimationSpeed) =
    spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = animSpeed.stiffness(CARD_ENTER_STIFFNESS)
    )

private fun expectEdgeEnter(ui: IslandInteractionState, scope: CoroutineScope, wait: Array<Job?>, sign: Int) {
    wait[0]?.cancel()
    ui.pendingEnterSign = sign
    // As portrait's pill swipe: the switch pop stays out while the swipe's own slide runs
    ui.lastCardSwipeMs = System.currentTimeMillis()
    wait[0] = scope.launch {
        delay(CARD_ENTER_WAIT_EVENT_MS)
        if (ui.pendingEnterSign != 0) {
            ui.pendingEnterSign = 0
            easeBackPillMotion(ui, scope)
        }
    }
}

private fun runEdgeAction(
    action: EdgePillAction,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    notification: IslandNotification?,
    swipeDy: Float = 0f,
    otherIndex: Int = -1
) {
    when (action) {
        EdgePillAction.ToggleCard, EdgePillAction.OpenCard, EdgePillAction.CloseCard -> wiring.actions.expand.toggle()
        EdgePillAction.PutCardsAway -> wiring.actions.idle.dotTap()
        EdgePillAction.BringCardsBack -> wiring.actions.idle.revealCards()
        EdgePillAction.SwitchCard -> wiring.actions.card.switchEvent(swipeDy > 0f)
        EdgePillAction.PillSwipeAction -> {
            val swipe = SwipeAction.fromId(wiring.view.settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
            if (swipe != SwipeAction.None) runner.run(swipe, notification)
        }
        EdgePillAction.NormalLongPress -> {
            val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
            if (longPress != SwipeAction.None) runner.runLongPress(longPress)
        }
        EdgePillAction.SelectOther -> if (otherIndex >= 0) wiring.actions.card.selectPage(otherIndex)
        EdgePillAction.AppsShortcut -> wiring.actions.system.openAppsLauncher()
        EdgePillAction.None -> Unit
    }
}
