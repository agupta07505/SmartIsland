/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.SwipeDownPull
import kotlinx.coroutines.launch

private const val HANDLE_FIRE_DP = 44f

private const val HANDLE_DISARM_DP = 36f

private const val PILL_BOTTOM_INSET_DP = 5f

private const val HANDLE_SLOP_DP = 4f

/**
 * A small white pill at the bottom of the expanded island. Pulling it down and lifting the finger opens the current
 * notification in a floating window.
 */
@Composable
internal fun BoxScope.FloatingWindowHandle(
    enabled: Boolean,
    visible: Boolean,
    fade: () -> Float,
    cb: IslandCallbacks,
    ui: IslandInteractionState
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    // The touch area stays put (it is the outer box); only the drawn pill follows the finger.
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .size(width = 88.dp, height = 20.dp)
            .then(
                if (!enabled) Modifier else Modifier.pointerInput(density) {
                    val firePx = HANDLE_FIRE_DP * density
                    val disarmPx = HANDLE_DISARM_DP * density
                    val slopPx = HANDLE_SLOP_DP * density
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        ui.handleOwnsTouch = true
                        // The island's auto-collapse countdown waits for as long as the finger is on the pull.
                        ui.onHandleHeld?.invoke(true)
                        ui.userTouched()
                        // A new touch takes the pull over from a spring that is still running.
                        ui.handleReleaseJob?.cancel()
                        down.consume()
                        var armed = false
                        var fire = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    // An island that closed under the finger (any cause) opens nothing: the pull is cancelled.
                                    fire = armed && cb.currentExpanded
                                    break
                                }
                                val dy = change.position.y - down.position.y
                                ui.handlePull = SwipeDownPull.progress(dy, slopPx, firePx)
                                // Same tick whichever way the finger crosses the line: down arms it, back up disarms it.
                                val nowArmed = if (armed) dy >= disarmPx else dy >= firePx
                                if (nowArmed != armed) {
                                    IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Swipe)
                                }
                                armed = nowArmed
                                change.consume()
                            }
                        } finally {
                            ui.handleOwnsTouch = false
                            ui.onHandleHeld?.invoke(false)
                            // Everything returns on one spring, whether the floating window opens or not.
                            val from = ui.handlePull
                            if (from != 0f) {
                                ui.handleReleaseJob?.cancel()
                                ui.handleReleaseJob = scope.launch {
                                    Animatable(from).animateTo(0f, swipeEaseBackSpring()) { ui.handlePull = value }
                                    ui.handlePull = 0f
                                }
                            }
                        }
                        if (fire) cb.currentOnOpenFloatingWindow()
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
                    // The pill has no travel of its own.
                    val pull = ui.handlePull
                    val sx = SwipeDownPull.handleScaleX(pull)
                    val sy = SwipeDownPull.handleScaleY(pull)
                    scaleX = 1f / sx
                    scaleY = 1f / sy
                    translationY = -PILL_BOTTOM_INSET_DP * density * (sy - 1f)
                }
                .size(width = 60.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.45f))
        )
    }
}
