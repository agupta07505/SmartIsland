/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.GestureSensitivity
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.PillSwipeDirection
import com.agupta07505.smartisland.util.SwipeArming
import com.agupta07505.smartisland.util.SwipeFeel
import com.agupta07505.smartisland.util.TapGuard
import com.agupta07505.smartisland.util.TouchZone
import com.agupta07505.smartisland.util.touchZone
import com.agupta07505.smartisland.util.IslandHaptics
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

/** Landscape, expanded: the collapsed pill stays at its own spot beside / above the card. */
@Composable
internal fun CornerPill(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    cb: IslandCallbacks,
    ui: IslandInteractionState,
    runner: IslandActionRunner,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val displayMetrics = context.resources.displayMetrics
    // Same finger-follow as the collapsed pill's drag, and the same actions it fires.
    val currentSecondaryIndex by rememberUpdatedState(secondary.secondaryIndex)
    val runCornerPillAction: (SwipeAction) -> Unit = { action ->
        runner.run(action, cb.currentNotifications.getOrNull(cb.currentSelectedIndex))
    }
    if (secondary.cornerPillShown) {
        val cornerPillShape = RoundedCornerShape(settings.cornerRadius.dp)
        Box(
            modifier = Modifier
                .width(settings.width.dp)
                .height(settings.height.dp)
                .graphicsLayer {
                    translationX = secondary.cornerPillCenterRel.toPx() + ui.cornerPillDragX
                    translationY = layout.collapsedTopShift.toPx() + ui.cornerPillDragY
                    alpha = secondary.cornerPillAlpha
                    // Dead zone (the finger moved, lifting does nothing): shrinks a little, like the main pill.
                    val cornerPull = ui.cornerSwipeDownPull
                    val deadShrink = 1f - PILL_DEAD_SHRINK * ui.cornerDeadZone.value *
                        com.agupta07505.smartisland.util.SwipeDownPull.deadShrinkWeight(cornerPull)
                    scaleX = deadShrink * com.agupta07505.smartisland.util.SwipeDownPull.pillScaleX(cornerPull)
                    val cornerScaleY = com.agupta07505.smartisland.util.SwipeDownPull.pillScaleY(cornerPull)
                    scaleY = deadShrink * cornerScaleY
                    translationY += com.agupta07505.smartisland.util.SwipeDownPull.topAnchorShiftPx(cornerScaleY, size.height)
                }
                .clip(cornerPillShape)
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity), cornerPillShape)
                .pointerInput(displayMetrics.density) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        ui.touch()
                        var dragX = 0f
                        var dragY = 0f
                        var isDragging = false
                        var triggered = false
                        val holdJob = scope.launch {
                            kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                            IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Hold)
                            if (!isDragging && !triggered) {
                                val longPress = SwipeAction.fromId(cb.currentSettings.longPressAction, SwipeAction.TogglePill)
                                if (longPress != SwipeAction.None) {
                                    triggered = true
                                    runner.runLongPress(longPress)
                                }
                            }
                        }
                        val sensitivity = GestureSensitivity.fromId(cb.currentSettings.gestureSensitivity)
                        val threshold = sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP) * displayMetrics.density
                        val tapSlop = sensitivity.scaled(10f) * displayMetrics.density
                        // Same rule as the main pill: fires when the finger lifts, only while still armed.
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
                                val fireDirection = if (!triggered && cb.currentSettings.enablePillSwipeActions) arming.direction else null
                                if (fireDirection != null) {
                                    triggered = true
                                    val action = when (fireDirection) {
                                        PillSwipeDirection.Left ->
                                            SwipeAction.fromId(cb.currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                        PillSwipeDirection.Right ->
                                            SwipeAction.fromId(cb.currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                        PillSwipeDirection.Up ->
                                            SwipeAction.fromId(cb.currentSettings.pillSwipeUpAction, SwipeAction.DismissCurrent)
                                        PillSwipeDirection.Down ->
                                            com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
                                    }
                                    runCornerPillAction(action)
                                } else if (!triggered && cb.currentSettings.enablePillSwipeActions && crossedOnce) {
                                    // Pulled back towards the start (or too diagonal): no gesture, and not a tap either.
                                } else if (!triggered && !tapGuard.exceeded) {
                                    // Same as the portrait mini pill: bring the other stored card in. One tick says the tap counted.
                                    IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Swipe)
                                    if (currentSecondaryIndex >= 0) cb.currentOnPageSelected(currentSecondaryIndex) else cb.currentOnToggle()
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
                                    -SwipeFeel.PILL_LEAN_Y_MAX_DP * displayMetrics.density, SwipeFeel.PILL_LEAN_Y_MAX_DP * displayMetrics.density
                                )
                                if (abs(dragX) >= threshold || abs(dragY) >= threshold) crossedOnce = true
                                tapGuard.update(dragX, dragY)
                                // Swipe down stretches the corner pill downward with the finger, like the main pill.
                                val pullAllowed = !triggered && cb.currentSettings.enablePillSwipeActions &&
                                    com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION != SwipeAction.None &&
                                    sensitivity.isVertical(dragX, dragY)
                                ui.cornerSwipeDownPull = if (pullAllowed) {
                                    com.agupta07505.smartisland.util.SwipeDownPull.progress(dragY, tapSlop, threshold)
                                } else 0f
                                if (!triggered && cb.currentSettings.enablePillSwipeActions &&
                                    arming.update(dragX, dragY, android.os.SystemClock.uptimeMillis())
                                ) {
                                    holdJob.cancel()
                                    IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Merge)
                                }
                                val deadNow = !triggered &&
                                    touchZone(tapGuard.exceeded, cb.currentSettings.enablePillSwipeActions && arming.direction != null) == TouchZone.Dead
                                if (deadNow != deadShown) {
                                    deadShown = deadNow
                                    scope.launch { ui.cornerDeadZone.animateTo(if (deadNow) 1f else 0f, swipeCalmSpring()) }
                                }
                            }
                        }
                        ui.cornerPillDragX = 0f
                        ui.cornerPillDragY = 0f
                        if (ui.cornerSwipeDownPull != 0f) {
                            val startPull = ui.cornerSwipeDownPull
                            scope.launch {
                                androidx.compose.animation.core.Animatable(startPull).animateTo(
                                    0f, swipeCalmSpring()
                                ) { ui.cornerSwipeDownPull = value }
                            }
                        }
                        if (deadShown) scope.launch { ui.cornerDeadZone.animateTo(0f, swipeCalmSpring()) }
                    }
                }
        ) {
            secondary.secondaryNotification?.let { other ->
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
