/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Content timing while the island expands, tied to the shell's own growth. */
object ExpandMotion {
    const val PILL_OUT_END_PROGRESS = 0.25f
    const val CARD_IN_START_PROGRESS = 0.1f
    const val CARD_IN_END_PROGRESS = 0.9f

    /** A fade is never shorter than a couple of frames, however stiff the spring. */
    const val MIN_FADE_MS = 50

    private fun at(progress: Float, stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, progress).toInt()

    fun pillOutMs(stiffness: Float, damping: Float): Int =
        at(PILL_OUT_END_PROGRESS, stiffness, damping).coerceAtLeast(MIN_FADE_MS)

    fun cardInDelayMs(stiffness: Float, damping: Float): Int =
        at(CARD_IN_START_PROGRESS, stiffness, damping)

    fun cardInMs(stiffness: Float, damping: Float): Int =
        (at(CARD_IN_END_PROGRESS, stiffness, damping) - cardInDelayMs(stiffness, damping)).coerceAtLeast(MIN_FADE_MS)
}
