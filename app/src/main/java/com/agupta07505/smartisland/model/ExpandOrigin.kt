/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** The expanded card grows out of the collapsed pill's centre. */
object ExpandOrigin {
    fun anchorInCard(pillCentre: Float, cardStart: Float, cardSize: Float): Float {
        if (cardSize <= 0f) return 0.5f
        return ((pillCentre - cardStart) / cardSize).coerceIn(0f, 1f)
    }

    /** Anchor position at the end: the pill centre, pulled inside the card when it lies outside. */
    fun anchorAtEnd(pillCentre: Float, cardStart: Float, cardSize: Float): Float =
        pillCentre.coerceIn(cardStart, cardStart + maxOf(cardSize, 0f))

    /**
     * Start edge of the island on this axis.
     * @param size the island's current size on this axis (its own spring).
     */
    fun start(
        progress: Float,
        size: Float,
        pillCentre: Float,
        cardStart: Float,
        cardSize: Float
    ): Float {
        val endAnchor = anchorAtEnd(pillCentre, cardStart, cardSize)
        val anchor = pillCentre + (endAnchor - pillCentre) * progress
        val inIsland = 0.5f + (anchorInCard(pillCentre, cardStart, cardSize) - 0.5f) * progress
        return anchor - inIsland * size
    }
}
