/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

/**
 * How see-through a swiped card is, as a function of how far it sits from the middle: the further the card is pushed
 * the fainter it gets (the look of Android's swipe-to-dismiss).
 */
object SwipeFade {
    /** The lowest opacity the expanded card reaches at (or past) its slide limit. */
    const val CARD_MIN_ALPHA = 0.3f

    /** 1 at the middle, [minAlpha] at [maxPx] or further, smoothly in between (ease in and out). Odd in [offsetPx]. */
    fun alpha(offsetPx: Float, maxPx: Float, minAlpha: Float): Float {
        if (maxPx <= 0f) return 1f
        val s = (abs(offsetPx) / maxPx).coerceIn(0f, 1f)
        val eased = s * s * (3f - 2f * s)
        return 1f - (1f - minAlpha) * eased
    }
}
