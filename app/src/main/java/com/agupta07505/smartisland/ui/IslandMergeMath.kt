/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * How far (dp) the secondary bubble slides toward the primary cutout to fuse with it. [distDp] is the signed centre-
 * to-centre distance.
 */
internal fun mergeTravelDp(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float {
    val gap = (kotlin.math.abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)
    // Sink deep enough that the whole bubble is inside the pill (0.5 = flush with its edge), but never
    // past the pill's far side.
    val sink = (bubbleWidthDp * 1.15f).coerceAtMost(pillWidthDp / 2f + bubbleWidthDp / 2f)
    return gap + sink
}

internal fun mergeGapDp(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float =
    (kotlin.math.abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)

/**
 * The bubble's outline while it merges: the corners that face the pill go flat during the push (bubble style);
 * otherwise the plain rounded shape.
 */
internal fun bubbleMergeShape(
    bubbleMerge: Boolean,
    merge: Float,
    distDp: Float,
    pillWidthDp: Float,
    bubbleWidthDp: Float,
    cornerDp: androidx.compose.ui.unit.Dp
): androidx.compose.ui.graphics.Shape {
    if (!bubbleMerge) return RoundedCornerShape(cornerDp)
    val contact = mergeContactFraction(distDp, pillWidthDp, bubbleWidthDp)
    val near = cornerDp * mergeNearCornerFactor(mergeFaceFlat(merge, contact))
    return if (distDp >= 0f) {
        androidx.compose.foundation.shape.AbsoluteRoundedCornerShape(cornerDp, near, near, cornerDp)
    } else {
        androidx.compose.foundation.shape.AbsoluteRoundedCornerShape(near, cornerDp, cornerDp, near)
    }
}

/** Share of the merge (0..1) at which the bubble first touches the pill's edge. */
internal fun mergeContactFraction(distDp: Float, pillWidthDp: Float, bubbleWidthDp: Float): Float {
    val travel = mergeTravelDp(distDp, pillWidthDp, bubbleWidthDp)
    if (travel <= 0f) return 0f
    val gap = (kotlin.math.abs(distDp) - pillWidthDp / 2f - bubbleWidthDp / 2f).coerceAtLeast(0f)
    return (gap / travel).coerceIn(0f, 1f)
}

/** The primary pill swells (0..1..0) as the bubble touches it and sinks in, from contact to the end. */
internal fun mergeSwell(merge: Float, contact: Float): Float {
    val span = (1f - contact).coerceAtLeast(0.05f)
    val t = ((merge - contact) / span).coerceIn(0f, 1f)
    return kotlin.math.sin(Math.PI.toFloat() * t)
}

internal fun mergeBubbleAlpha(merge: Float): Float {
    val t = ((merge - 0.9f) / 0.1f).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}

private const val MIN_STRETCH_PEAK = 0.2f

/**
 * Liquid stretch toward the pill: grows as the bubble nears the pill's edge, peaks at contact (the moment the two
 * pull together), then relaxes while the bubble sinks into the pill.
 */
internal fun mergeStretch(merge: Float, contact: Float): Float {
    val m = merge.coerceIn(0f, 1f)
    // A cutout that already touches the pill (contact 0, e.g. beside a wide pill or an open island) must not skip the
    // rise and stay stretched at merge 0, or the round cutout would look warped at rest.
    val peak = contact.coerceIn(MIN_STRETCH_PEAK, 0.999f)
    val start = (peak - 0.35f).coerceAtLeast(0f)
    val rise = ((m - start) / (peak - start)).coerceIn(0f, 1f)
    val fall = if (m > peak) 1f - ((m - peak) / (1f - peak)).coerceIn(0f, 1f) else 1f
    val ease = rise * rise * (3f - 2f * rise)
    return 0.3f * ease * fall
}
