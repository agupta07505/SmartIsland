/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

internal fun mergeTravelDp(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float {
    val gap = (abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)
    val sink = (bubbleWidthDp * 1.15f).coerceAtMost(pillWidthDp / 2f + bubbleWidthDp / 2f)
    return gap + sink
}

internal fun mergeGapDp(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float =
    (abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)

internal fun bubbleMergeShape(
    bubbleMerge: Boolean,
    merge: Float,
    distDp: Float,
    pillWidthDp: Float,
    bubbleWidthDp: Float,
    cornerDp: Dp
): Shape {
    if (!bubbleMerge) return RoundedCornerShape(cornerDp)
    val contact = mergeContactFraction(distDp, pillWidthDp, bubbleWidthDp)
    val near = cornerDp * mergeNearCornerFactor(mergeFaceFlat(merge, contact))
    return if (distDp >= 0f) {
        AbsoluteRoundedCornerShape(cornerDp, near, near, cornerDp)
    } else {
        AbsoluteRoundedCornerShape(near, cornerDp, cornerDp, near)
    }
}

/** Gap (dp) kept between a leaning pill and the screen edge, covers the swell of an armed or merging pill. */
internal const val LEAN_EDGE_MARGIN_DP = 4f

/**
 * Keeps a swipe's sideways lean on screen and the same both ways: lean (negative = left) is capped at the room toward
 * the nearer screen edge. [leftDp] is the pill's resting left edge, [screenWidthDp] 0 when unknown
 */
internal fun pillLeanWithinScreenDp(leanDp: Float, leftDp: Float, pillWidthDp: Float, screenWidthDp: Float): Float {
    if (leanDp == 0f) return 0f
    val maxLeft = (leftDp - LEAN_EDGE_MARGIN_DP).coerceAtLeast(0f)
    val maxRight = if (screenWidthDp > 0f) {
        (screenWidthDp - (leftDp + pillWidthDp) - LEAN_EDGE_MARGIN_DP).coerceAtLeast(0f)
    } else Float.MAX_VALUE
    val room = minOf(maxLeft, maxRight)
    return leanDp.coerceIn(-room, room)
}

/**
 * Secondary bubble rides along with the pill's swipe lean (negative = left) so the gap between them stays put. Only
 * held on screen: the room toward each edge is the bubble's own, not the pill's
 */
internal fun bubbleFollowLeanDp(pillLeanDp: Float, bubbleLeftDp: Float, bubbleWidthDp: Float,
        screenWidthDp: Float): Float {
    if (pillLeanDp == 0f) return 0f
    val roomLeft = (bubbleLeftDp - LEAN_EDGE_MARGIN_DP).coerceAtLeast(0f)
    val roomRight = if (screenWidthDp > 0f) {
        (screenWidthDp - (bubbleLeftDp + bubbleWidthDp) - LEAN_EDGE_MARGIN_DP).coerceAtLeast(0f)
    } else Float.MAX_VALUE
    return pillLeanDp.coerceIn(-roomLeft, roomRight)
}

/**
 * Edge layout's ear bubble rides the edge with the pill's lean ([alongPx], negative = up), held to the ear's own room
 * above and below. Returns px, 0 without a lean or a density
 */
internal fun edgeEarFollowPx(alongPx: Float, earTopDp: Float, earHeightDp: Float, screenHeightDp: Float,
        density: Float): Float {
    if (alongPx == 0f || density <= 0f) return 0f
    return bubbleFollowLeanDp(alongPx / density, earTopDp, earHeightDp, screenHeightDp) * density
}

/** Share of the merge (0..1) at which the bubble first touches the pill's edge. */
internal fun mergeContactFraction(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float {
    val travel = mergeTravelDp(distDp, pillWidthDp, bubbleWidthDp)
    if (travel <= 0f) return 0f
    val gap = (abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)
    return (gap / travel).coerceIn(0f, 1f)
}

/** Primary pill swells (0..1..0) as the bubble touches it and sinks in. */
internal fun mergeSwell(merge: Float, contact: Float): Float {
    val span = (1f - contact).coerceAtLeast(0.05f)
    val t = ((merge - contact) / span).coerceIn(0f, 1f)
    return sin(Math.PI.toFloat() * t)
}

internal fun mergeBubbleAlpha(merge: Float): Float {
    val t = ((merge - 0.9f) / 0.1f).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}

private const val MIN_STRETCH_PEAK = 0.2f

internal fun mergeStretch(merge: Float, contact: Float): Float {
    val m = merge.coerceIn(0f, 1f)
    // a cutout that already touches the pill
    val peak = contact.coerceIn(MIN_STRETCH_PEAK, 0.999f)
    val start = (peak - 0.35f).coerceAtLeast(0f)
    val rise = ((m - start) / (peak - start)).coerceIn(0f, 1f)
    val fall = if (m > peak) 1f - ((m - peak) / (1f - peak)).coerceIn(0f, 1f) else 1f
    val ease = rise * rise * (3f - 2f * rise)
    return 0.3f * ease * fall
}

/** Slide merge pulls the collapsed bubble in under the pill so it never covers or shades the pill's content. */
internal fun secondaryBehindPill(bubbleMerge: Boolean, expanded: Boolean, notchMode: Boolean): Boolean =
    !bubbleMerge && !expanded && !notchMode
