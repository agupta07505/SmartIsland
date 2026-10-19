/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether looping decorations (recording dots, pulsing bells) may run: false once the overlay's lifecycle drops
 * below STARTED, which `SmartIslandOverlayService` does on `ACTION_SCREEN_OFF` (Rule_Book §4.3, §5). Callers
 * skip their `rememberInfiniteTransition` entirely while false, so nothing ticks with the screen off even if a
 * frame clock keeps running. A global screen-on flag can be folded in here later without touching callers.
 */
@Composable
fun rememberIslandAnimationsActive(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    return state.isAtLeast(Lifecycle.State.STARTED)
}
