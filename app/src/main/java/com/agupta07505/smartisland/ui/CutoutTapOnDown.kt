/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.agupta07505.smartisland.model.SwapTouchGuard

/**
 * Runs [onTap] as the finger lands on a cutout while [enabled] holds (an open island swapping to it), so the swap
 * starts a tap's length before release. Sits before the `clickable`, which keeps every other state and skips the
 * click of a touch handled here ([SwapTouchGuard.claimClick])
 */
@Composable
internal fun Modifier.cutoutTapOnDown(
    guard: SwapTouchGuard,
    enabled: () -> Boolean,
    onTap: () -> Unit
): Modifier {
    // The pointer block outlives recompositions, so it reads the latest lambdas (the cutout shows another card after a swap)
    val enabledNow = rememberUpdatedState(enabled)
    val onTapNow = rememberUpdatedState(onTap)
    return pointerInput(guard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val fire = enabledNow.value()
            guard.down(fire)
            if (!fire) return@awaitEachGesture
            onTapNow.value()
            waitForUpOrCancellation()
        }
    }
}
