/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Hides the island while the phone turns until the new orientation has settled. */
object RotationVeil {
    /** Wait before each orientation read after a turn (ms, one after the other); the last read is the settled one. */
    val REFRESH_GAPS_MS: LongArray = longArrayOf(0L, 150L, 450L)

    /** When the settled (last) read happens, in ms after the turn. */
    val SETTLED_READ_AT_MS: Long = REFRESH_GAPS_MS.sum()

    /** Wait after the last read so the window has the settled metrics before the island is drawn again. */
    const val MARGIN_MS: Long = 60L

    /** The island is never hidden longer than this, whatever the system does. */
    const val MAX_HIDDEN_MS: Long = 700L

    /** Base length (ms) of the fade-out, scaled by the animation speed. */
    const val HIDE_MS: Int = 90

    /** Wait after the last orientation read before lifting the veil. @param hiddenForMs how long the veil has been down. */
    fun liftDelayMs(hiddenForMs: Long): Long =
        minOf(MARGIN_MS, MAX_HIDDEN_MS - hiddenForMs).coerceAtLeast(0L)

    /** Island opacity for a veil level (0 = hidden, 1 = shown): the same fade curve the pill uses to arrive. */
    fun alpha(visible: Float): Float = pillExitAlpha(pillSlideProgress(visible))
}
