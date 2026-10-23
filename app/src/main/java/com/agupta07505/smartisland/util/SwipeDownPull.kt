/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.exp

/**
 * The swipe-down "pull": while a finger drags the island down, the island stretches
 * downward with it, so a swipe down feels like pulling the island open instead of only sliding it. The stretch is read
 * off the drag (it follows the finger both ways) and springs back when the finger lifts.
 *
 * Pure maths only; the gesture handler feeds [progress] and the shell's `graphicsLayer` turns it into a scale.
 */
object SwipeDownPull {
    /** How far past 1 the progress may rubber-band when the finger keeps going after the swipe threshold. */
    const val OVERSHOOT = 0.15f

    /** Collapsed pill: wider by 5 % and taller by 12 % at full pull (the pill is small, so it shows more). */
    const val PILL_STRETCH_X = 0.05f
    const val PILL_STRETCH_Y = 0.12f

    /** Expanded card: a big surface, so a small stretch (1.2 % wide, 3 % tall) already reads. */
    const val EXPANDED_STRETCH_X = 0.006f
    const val EXPANDED_STRETCH_Y = 0.015f

    /**
     * How far (dp) the whole expanded island may travel down with a swipe down. It only leans a little rather than
     * following the finger down the screen.
     */
    const val EXPANDED_DOWN_MAX_DP = 6f

    /**
     * Downward travel (px) of the expanded island for a drag of [dragY] px: starts at a quarter of the finger's speed
     * and settles towards [EXPANDED_DOWN_MAX_DP] (never past it).
     */
    fun expandedDownOffsetPx(dragY: Float, density: Float): Float {
        if (dragY <= 0f) return 0f
        val maxPx = EXPANDED_DOWN_MAX_DP * density
        return maxPx * (1f - exp(-dragY / (4f * maxPx)))
    }

    /**
     * 0 while the finger has not moved down past the tap [slopPx] (the stretch never starts inside the tap area, so a
     * tap and the dead-zone shrink stay as they are); 1 at [thresholdPx], the point where a swipe down fires; past
     * that it keeps growing slowly up to `1 + OVERSHOOT`.
     */
    fun progress(dragY: Float, slopPx: Float, thresholdPx: Float): Float {
        if (dragY <= slopPx) return 0f
        val span = (thresholdPx - slopPx).coerceAtLeast(1f)
        val p = (dragY - slopPx) / span
        return if (p <= 1f) p else 1f + OVERSHOOT * (1f - exp(-2f * (p - 1f)))
    }

    /**
     * The floating-window handle at the bottom of the expanded island: while it is pulled down the card gets a little
     * narrower and taller (4.5 % / 10 % at full pull), squeezing its content.
     */
    const val HANDLE_SQUEEZE_X = 0.045f
    const val HANDLE_STRETCH_Y = 0.10f
    fun handleScaleX(progress: Float): Float = 1f - HANDLE_SQUEEZE_X * progress
    fun handleScaleY(progress: Float): Float = 1f + HANDLE_STRETCH_Y * progress

    fun pillScaleX(progress: Float): Float = 1f + PILL_STRETCH_X * progress
    fun pillScaleY(progress: Float): Float = 1f + PILL_STRETCH_Y * progress
    fun expandedScaleX(progress: Float): Float = 1f + EXPANDED_STRETCH_X * progress
    fun expandedScaleY(progress: Float): Float = 1f + EXPANDED_STRETCH_Y * progress

    /**
     * The shell scales around its centre, so a taller shell would grow up as well as down. Moving it down by half of
     * the added height keeps its top edge where it is: the stretch only reaches downward.
     */
    fun topAnchorShiftPx(scaleY: Float, heightPx: Float): Float = (scaleY - 1f) * heightPx / 2f

    fun deadShrinkWeight(progress: Float): Float = (1f - progress).coerceIn(0f, 1f)
}
