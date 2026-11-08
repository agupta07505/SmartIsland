/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

/**
 * Landscape expanded card top: portrait-style [baseDp] plus 1 pill height and 1 gap, so the collapsed pill beside
 * the card never overlaps it
 */
internal fun landscapeExpandedTopOffsetDp(
    baseDp: Float,
    pillHeightDp: Float,
    gapDp: Float,
    screenHeightDp: Float,
    expandedHeightDp: Float
): Float {
    val wanted = baseDp + pillHeightDp + gapDp
    val lowest = (screenHeightDp - expandedHeightDp - gapDp).coerceAtLeast(baseDp)
    return wanted.coerceAtMost(lowest)
}

/** Gap (dp) between the top edge and the app shortcuts window in landscape, whatever the collapsed cutout was. */
internal const val LANDSCAPE_LAUNCHER_TOP_GAP_DP = 4f

/** Gap between the top edge and the expanded card when the collapsed pill sits in a corner. */
internal const val LANDSCAPE_EDGE_GAP_DP = 10f

/** True if the collapsed cutout group would overlap the expanded card with [gapDp] spacing on each side. */
internal fun landscapePillOverlapsCard(
    groupStartDp: Float,
    groupEndDp: Float,
    cardLeftDp: Float,
    cardRightDp: Float,
    gapDp: Float
): Boolean = groupEndDp + gapDp > cardLeftDp && groupStartDp - gapDp < cardRightDp

/** Puts the landscape card below the pill when they overlap, else [edgeGapDp] from the top, kept within the screen. */
internal fun landscapeExpandedTopDp(
    pillOverlapsCard: Boolean,
    baseDp: Float,
    pillBottomDp: Float,
    gapDp: Float,
    edgeGapDp: Float,
    screenHeightDp: Float,
    expandedHeightDp: Float
): Float {
    val wanted = if (pillOverlapsCard) maxOf(baseDp, pillBottomDp + gapDp) else edgeGapDp
    val lowest = (screenHeightDp - expandedHeightDp - gapDp).coerceAtLeast(edgeGapDp)
    return wanted.coerceAtMost(lowest)
}
