/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object RotationVeil {
    val REFRESH_GAPS_MS: LongArray = longArrayOf(0L, 150L, 450L)

    val SETTLED_READ_AT_MS: Long = REFRESH_GAPS_MS.sum()

    /** Wait after the last read so the window has the settled metrics before the island is drawn again. */
    const val MARGIN_MS: Long = 60L

    const val MAX_HIDDEN_MS: Long = 700L

    const val HIDE_MS: Int = 90

    fun liftDelayMs(hiddenForMs: Long): Long =
        minOf(MARGIN_MS, MAX_HIDDEN_MS - hiddenForMs).coerceAtLeast(0L)

    fun alpha(visible: Float): Float = pillExitAlpha(pillSlideProgress(visible))
}
