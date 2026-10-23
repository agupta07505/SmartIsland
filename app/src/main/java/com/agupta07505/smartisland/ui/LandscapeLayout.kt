/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

/**
 * Landscape expanded card top: the portrait-style [baseDp] plus one pill height and one gap, so the collapsed pill
 * beside the card never overlaps it.
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

/**
 * Gap (dp) between the screen's top edge and the app shortcuts window in landscape, whatever the collapsed cutout was
 * (with or without content).
 */
internal const val LANDSCAPE_LAUNCHER_TOP_GAP_DP = 4f

/** Gap between the screen's top edge and the expanded card when the collapsed pill sits in a corner. */
internal const val LANDSCAPE_EDGE_GAP_DP = 10f

/**
 * True when the collapsed cutout group (pill plus companion circle, [groupStartDp]..[groupEndDp] from the left screen
 * edge) would touch the expanded card ([cardLeftDp]..[cardRightDp]) once [gapDp] of breathing room is added on both
 * sides.
 */
internal fun landscapePillOverlapsCard(
    groupStartDp: Float,
    groupEndDp: Float,
    cardLeftDp: Float,
    cardRightDp: Float,
    gapDp: Float
): Boolean = groupEndDp + gapDp > cardLeftDp && groupStartDp - gapDp < cardRightDp

/**
 * Top of the expanded card in landscape.
 *
 * - Pill clear of the card (left / right corner): [edgeGapDp] from the top of the screen.
 * - Pill above the card (middle): one [gapDp] below the pill's bottom edge [pillBottomDp], never
 *   higher than [baseDp].
 * Either way the card is held back so its bottom plus one gap still fits on screen.
 */
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
