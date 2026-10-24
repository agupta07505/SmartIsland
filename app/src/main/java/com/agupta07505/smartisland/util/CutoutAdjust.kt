/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Not saved. True while a cutout slider is being dragged in settings, the overlay (same process) fades the island
 * almost out and rings the hole so it can be placed by eye
 */
object CutoutAdjust {
    /** Island opacity while adjusting: not 0, so its layout stays. */
    const val ISLAND_ALPHA = 0.01f

    val dragging = MutableStateFlow(false)
}
