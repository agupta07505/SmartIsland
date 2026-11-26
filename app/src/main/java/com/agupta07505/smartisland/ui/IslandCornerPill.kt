/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.os.SystemClock
import android.util.DisplayMetrics
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.PillSwipeDirection
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeArming
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.model.SwipeFeel
import com.agupta07505.smartisland.model.TapGuard
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TouchZone
import com.agupta07505.smartisland.model.touchZone
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Landscape, expanded: the collapsed pill stays at its own spot beside / above the card. */
@Composable
internal fun CornerPill(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    runner: IslandActionRunner,
    scope: CoroutineScope,
    context: Context
) {
    val displayMetrics = context.resources.displayMetrics
    // Same finger-follow as the collapsed pill's drag, and the same actions it fires
    val currentSecondaryIndex by rememberUpdatedState(secondary.cards.secondaryIndex)
    val runCornerPillAction: (SwipeAction) -> Unit = { action ->
        runner.run(action, wiring.view.notifications.getOrNull(wiring.view.selectedIndex))
    }
    val cornerBounds = remember { arrayOfNulls<Rect>(1) }
    if (secondary.shown.cornerPill.value) {
        val cornerPillShape = RoundedCornerShape(settings.cornerRadius.dp)
        Box(
            modifier = Modifier
                .width(settings.width.dp)
                .height(settings.height.dp)
                .graphicsLayer { applyCornerPillLayer(layout, secondary, ui) }
                .onGloballyPositioned { coords -> if (!ui.tapSwapActive) cornerBounds[0] = coords.boundsInWindow() }
                .clip(cornerPillShape)
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity), cornerPillShape)
                .pointerInput(displayMetrics.density) {
                    awaitEachGesture {
                        cornerPillGesture(
                            ui, wiring, runner, scope, context, displayMetrics, cornerBounds,
                            { currentSecondaryIndex }, runCornerPillAction
                        )
                    }
                }
        ) {
            secondary.cards.secondaryNotification?.let { other ->
                IslandCollapsedContent(
                    mode = other.mode,
                    notification = other,
                    collapsedAlpha = 1f,
                    settings = settings
                )
            }
        }
    }
}

private fun GraphicsLayerScope.applyCornerPillLayer(
    layout: IslandLayout,
    secondary: IslandSecondary,
    ui: IslandInteractionState
) {
    translationX = secondary.cornerPillCenterRel.toPx() + ui.cornerPillDragX
    translationY = layout.collapsedTopShift.toPx() + ui.cornerPillDragY
    alpha = secondary.anims.cornerPillAlpha.value * ui.cutoutSwapAlpha(TapSwap.CUTOUT_PILL_SPOT)
    // dead zone (finger moved, lifting does nothing): shrinks a little like the main pill
    val cornerPull = ui.cornerSwipeDownPull
    val deadShrink = 1f - PILL_DEAD_SHRINK * ui.cornerDeadZone.value *
        SwipeDownPull.deadShrinkWeight(cornerPull)
    scaleX = deadShrink * SwipeDownPull.pillScaleX(cornerPull)
    val cornerScaleY = SwipeDownPull.pillScaleY(cornerPull)
    scaleY = deadShrink * cornerScaleY
    translationY += SwipeDownPull.topAnchorShiftPx(cornerScaleY, size.height)
}

private suspend fun AwaitPointerEventScope.cornerPillGesture(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    scope: CoroutineScope,
    context: Context,
    displayMetrics: DisplayMetrics,
    cornerBounds: Array<Rect?>,
    secondaryIndex: () -> Int,
    runCornerPillAction: (SwipeAction) -> Unit
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    ui.touch()
    var dragX = 0f
    var dragY = 0f
    var isDragging = false
    var triggered = false
    val holdJob = scope.launch {
        delay(HOLD_GESTURE_THRESHOLD_MS)
        IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Hold)
        HoldGesture.cornerActionAtThreshold(
            dragging = isDragging,
            triggered = triggered,
            action = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
        )?.let { longPress ->
            triggered = true
            runner.runLongPress(longPress)
        }
    }
    val sensitivity = GestureSensitivity.fromId(wiring.view.settings.gestureSensitivity)
    val threshold = sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP) * displayMetrics.density
    val tapSlop = sensitivity.scaled(10f) * displayMetrics.density
    // Same rule as the main pill: fires when the finger lifts, only while still armed
    val arming = SwipeArming(sensitivity, threshold).also { it.lockWhen = { false } }
    var crossedOnce = false
    val axisLock = AxisLock(tapSlop)
    var rawDragX = 0f
    var rawDragY = 0f
    val tapGuard = TapGuard(tapSlop)
    var deadShown = false
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        if (change.changedToUp()) {
            change.consume()
            holdJob.cancel()
            val fireDirection = if (!triggered && wiring.view.settings.enablePillSwipeActions) arming.direction else null
            if (fireDirection != null) {
                triggered = true
                runCornerPillAction(cornerSwipeAction(fireDirection, wiring))
            } else if (!triggered && wiring.view.settings.enablePillSwipeActions && crossedOnce) {
                // Pulled back towards the start (or too diagonal): no gesture, not a tap either
            } else if (!triggered && !tapGuard.exceeded) {
                cornerPillTap(ui, wiring, context, cornerBounds, secondaryIndex())
            }
            break
        }
        val dx = change.positionChange().x
        val dy = change.positionChange().y
        if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
            isDragging = true
            rawDragX += dx
            rawDragY += dy
            if (axisLock.update(rawDragX, rawDragY)) {
                if (axisLock.axis == AxisLock.Axis.Vertical) dragX = 0f else dragY = 0f
            }
            dragX += axisLock.filterX(dx)
            dragY += axisLock.filterY(dy)
            change.consume()
            if (abs(dragX) > 5f * displayMetrics.density || abs(dragY) > 5f * displayMetrics.density) {
                holdJob.cancel()
            }
            ui.cornerPillDragX = (dragX * 0.35f).coerceIn(-24f * displayMetrics.density, 24f * displayMetrics.density)
            ui.cornerPillDragY = (dragY * 0.35f).coerceIn(
                -SwipeFeel.PILL_LEAN_Y_MAX_DP * displayMetrics.density,
                    SwipeFeel.PILL_LEAN_Y_MAX_DP * displayMetrics.density
            )
            if (abs(dragX) >= threshold || abs(dragY) >= threshold) crossedOnce = true
            tapGuard.update(dragX, dragY)
            // Swipe down stretches the corner pill downward with the finger, like the main pill
            val pullAllowed = !triggered && wiring.view.settings.enablePillSwipeActions &&
                FIXED_PILL_SWIPE_DOWN_ACTION != SwipeAction.None &&
                sensitivity.isVertical(dragX, dragY)
            ui.cornerSwipeDownPull = if (pullAllowed) {
                SwipeDownPull.progress(dragY, tapSlop, threshold)
            } else 0f
            if (!triggered && wiring.view.settings.enablePillSwipeActions &&
                arming.update(dragX, dragY, SystemClock.uptimeMillis())
            ) {
                holdJob.cancel()
                IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Merge)
            }
            val deadNow = !triggered &&
                touchZone(tapGuard.exceeded, wiring.view.settings.enablePillSwipeActions
                    && arming.direction != null) == TouchZone.Dead
            if (deadNow != deadShown) {
                deadShown = deadNow
                scope.launch { ui.cornerDeadZone.animateTo(if (deadNow) 1f else 0f, swipeCalmSpring()) }
            }
        }
    }
    releaseCornerPill(ui, scope, deadShown)
}

private fun cornerSwipeAction(direction: PillSwipeDirection, wiring: IslandWiring): SwipeAction =
    when (direction) {
        PillSwipeDirection.Left ->
            SwipeAction.fromId(wiring.view.settings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
        PillSwipeDirection.Right ->
            SwipeAction.fromId(wiring.view.settings.pillSwipeRightAction, SwipeAction.NextNotification)
        PillSwipeDirection.Up ->
            SwipeAction.fromId(wiring.view.settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
        PillSwipeDirection.Down ->
            FIXED_PILL_SWIPE_DOWN_ACTION
    }

private fun cornerPillTap(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    context: Context,
    cornerBounds: Array<Rect?>,
    currentSecondaryIndex: Int
) {
    // same as the portrait mini pill: bring the other stored card in, 1 tick says the tap counted
    IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Swipe)
    if (!turnAroundOnCutoutTap(ui, wiring, currentSecondaryIndex, wiring.actions.card.selectPage)) {
        if (currentSecondaryIndex >= 0) {
            ui.markCutout(TapSwap.CUTOUT_PILL_SPOT) { cornerBounds[0] }
            wiring.actions.card.selectPage(currentSecondaryIndex)
        } else wiring.actions.expand.toggle()
    }
}

private fun releaseCornerPill(ui: IslandInteractionState, scope: CoroutineScope, deadShown: Boolean) {
    ui.cornerPillDragX = 0f
    ui.cornerPillDragY = 0f
    if (ui.cornerSwipeDownPull != 0f) {
        val startPull = ui.cornerSwipeDownPull
        scope.launch {
            Animatable(startPull).animateTo(
                0f, swipeCalmSpring()
            ) { ui.cornerSwipeDownPull = value }
        }
    }
    if (deadShown) scope.launch { ui.cornerDeadZone.animateTo(0f, swipeCalmSpring()) }
}
