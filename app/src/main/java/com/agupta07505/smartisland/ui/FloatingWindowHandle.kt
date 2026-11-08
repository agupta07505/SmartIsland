/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val HANDLE_FIRE_DP = 44f

private const val HANDLE_DISARM_DP = 36f

private const val PILL_BOTTOM_INSET_DP = 5f

private const val HANDLE_SLOP_DP = 4f

private const val HANDLE_TAP_SLOP_DP = 8f

private const val HANDLE_TAP_MAX_MS = 400L

private const val HANDLE_PULSE_MIN = 0.5f

private val HANDLE_PAUSED_COLOR = Color(0xFFFF453A).copy(alpha = 0.9f)

/** White pill at the bottom of the expanded island: pull for a floating window, tap to hold the auto-collapse. */
@Composable
internal fun BoxScope.FloatingWindowHandle(
    enabled: Boolean,
    pullEnabled: Boolean,
    visible: Boolean,
    paused: Boolean,
    fade: () -> Float,
    wiring: IslandWiring,
    ui: IslandInteractionState
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    var tinted by remember { mutableStateOf(paused) }
    // Shrinks in place, then grows back; the tint changes as it grows
    LaunchedEffect(paused) {
        if (tinted == paused) return@LaunchedEffect
        pulse.animateTo(HANDLE_PULSE_MIN, tween(90))
        tinted = paused
        pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val pillColor by animateColorAsState(
        if (tinted) HANDLE_PAUSED_COLOR else Color.White.copy(alpha = 0.45f),
        tween(140),
        label = "handleTint"
    )
    // Touch area stays put (outer box), only the drawn pill follows the finger
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .size(width = 88.dp, height = 20.dp)
            .then(
                if (!enabled) Modifier else Modifier.pointerInput(density, pullEnabled) {
                    val firePx = HANDLE_FIRE_DP * density
                    val disarmPx = HANDLE_DISARM_DP * density
                    val slopPx = HANDLE_SLOP_DP * density
                    val tapSlopPx = HANDLE_TAP_SLOP_DP * density
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        ui.handleOwnsTouch = true
                        ui.onHandleHeld?.invoke(true)
                        ui.userTouched()
                        ui.handleReleaseJob?.cancel()
                        down.consume()
                        var armed = false
                        var fire = false
                        var tap = false
                        var moved = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    // An island that closed under the finger (any cause) opens nothing, the pull is
                                    // cancelled
                                    fire = pullEnabled && armed && wiring.view.expanded
                                    tap = !moved && change.uptimeMillis - down.uptimeMillis <= HANDLE_TAP_MAX_MS
                                    break
                                }
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (abs(dx) > tapSlopPx || abs(dy) > tapSlopPx) moved = true
                                if (pullEnabled) {
                                    ui.handlePull = SwipeDownPull.progress(dy, slopPx, firePx)
                                    // Same tick whichever way the finger crosses the line: down arms it, back up
                                    // disarms it
                                    val nowArmed = if (armed) dy >= disarmPx else dy >= firePx
                                    if (nowArmed != armed) {
                                        IslandHaptics.play(context,
                                            HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Swipe)
                                    }
                                    armed = nowArmed
                                }
                                change.consume()
                            }
                        } finally {
                            ui.handleOwnsTouch = false
                            ui.onHandleHeld?.invoke(false)
                            // Everything returns on one spring, whether the floating window opens or not
                            val from = ui.handlePull
                            if (from != 0f) {
                                ui.handleReleaseJob?.cancel()
                                ui.handleReleaseJob = scope.launch {
                                    Animatable(from).animateTo(0f, swipeEaseBackSpring()) { ui.handlePull = value }
                                    ui.handlePull = 0f
                                }
                            }
                        }
                        if (fire) wiring.actions.system.openFloatingWindow() else if (tap) ui.onHandleTap?.invoke()
                    }
                }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = PILL_BOTTOM_INSET_DP.dp)
                .graphicsLayer {
                    alpha = if (visible) fade().coerceIn(0f, 1f) else 0f
                    // The pill has no travel of its own
                    val pull = ui.handlePull
                    val sx = SwipeDownPull.handleScaleX(pull)
                    val sy = SwipeDownPull.handleScaleY(pull)
                    scaleX = pulse.value / sx
                    scaleY = pulse.value / sy
                    translationY = -PILL_BOTTOM_INSET_DP * density * (sy - 1f)
                }
                .size(width = 60.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(pillColor)
        )
    }
}
