/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp

/**
 * Pure maths for the "bubble" merge style (see [com.agupta07505.smartisland.util.MergeStyle.BUBBLE]): push, then join.
 */

/** How far (dp) the two bodies press into each other during the push. */
const val MERGE_PUSH_DEPTH_DP = 7f

private const val JOIN_SINK_DP = 14f

/** How far (dp) inside the pill the bridge between bubble and pill reaches. */
const val MERGE_NECK_INSIDE_DP = 10f

/** Height of the bridge at its widest, as a share of the round cutout's height. */
const val MERGE_NECK_HEIGHT_FRACTION = 0.65f

private const val PUSH_SPAN = 0.37f

private const val ICON_FADE_SPAN = 0.33f

private const val JOIN_MOVE_SPAN = 0.53f

private const val NECK_FADE_SPAN = 0.37f

private const val FLAT_FADE_SPAN = 0.15f

private const val PILL_SWELL_DP = 5f

private fun smooth(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun postContactSpan(contact: Float): Float = (1f - contact).coerceAtLeast(0.05f)

/** 0..1 while the bodies press into each other: zero until contact, one when the push is over. */
fun mergePush(merge: Float, contact: Float): Float =
    smooth((merge - contact) / (postContactSpan(contact) * PUSH_SPAN))

fun mergeJoin(merge: Float, contact: Float): Float {
    val span = postContactSpan(contact)
    return ((merge - contact - span * PUSH_SPAN) / (span * (1f - PUSH_SPAN))).coerceIn(0f, 1f)
}

fun mergeIconAlpha(merge: Float, contact: Float): Float =
    1f - smooth((merge - contact) / (postContactSpan(contact) * ICON_FADE_SPAN))

/** How flat the touching faces are (0 = round, 1 = pressed flat): grows with the push, rounds off early in the join. */
fun mergeFaceFlat(merge: Float, contact: Float): Float =
    mergePush(merge, contact) * (1f - smooth(mergeJoin(merge, contact) / FLAT_FADE_SPAN))

fun mergeNearCornerFactor(flat: Float): Float = 1f - 0.65f * flat.coerceIn(0f, 1f)

private fun joinMove(merge: Float, contact: Float): Float = smooth(mergeJoin(merge, contact) / JOIN_MOVE_SPAN)

/**
 * How far (dp) the bubble's centre has moved toward the pill from its resting place: it crosses the [gapDp] between
 * the cutouts, is pressed in a little by the push, then goes [JOIN_SINK_DP] inside the pill in the join.
 */
fun mergeBubbleOffsetDp(merge: Float, contact: Float, gapDp: Float, bubbleWidthDp: Float, pillWidthDp: Float): Float {
    val m = merge.coerceIn(0f, 1f)
    if (m <= contact) return if (contact <= 0f) 0f else gapDp * (m / contact)
    // The left face moves in by the full depth while the body narrows by 0.6 of it, so the centre moves 0.7 of it.
    val pushedEnd = gapDp + 0.7f * MERGE_PUSH_DEPTH_DP
    val pushed = gapDp + 0.7f * MERGE_PUSH_DEPTH_DP * mergePush(m, contact)
    if (mergeJoin(m, contact) <= 0f) return pushed
    val target = gapDp + bubbleWidthDp / 2f + minOf(JOIN_SINK_DP, pillWidthDp / 2f)
    return pushedEnd + (target - pushedEnd) * joinMove(m, contact)
}

fun mergeBubbleShrink(merge: Float, contact: Float): Float {
    val s = joinMove(merge, contact)
    return 1f - s * s
}

fun mergeBubbleScaleX(merge: Float, contact: Float, bubbleWidthDp: Float): Float {
    val narrow = if (bubbleWidthDp <= 0f) 1f else 1f - 0.6f * MERGE_PUSH_DEPTH_DP * mergePush(merge, contact) / bubbleWidthDp
    return narrow.coerceAtLeast(0.5f) * mergeBubbleShrink(merge, contact)
}

/** Height of the bubble as a multiple of its resting height: a little taller while pressed, then shrinks away. */
fun mergeBubbleScaleY(merge: Float, contact: Float): Float =
    (1f + 0.06f * mergePush(merge, contact)) * mergeBubbleShrink(merge, contact)

/** The pill's swell (dp, can dip slightly below zero): kicks as the join starts and rings out. */
fun mergePillSwellDp(merge: Float, contact: Float): Float {
    val j = mergeJoin(merge, contact)
    if (j <= 0f) return 0f
    return PILL_SWELL_DP * exp(-5f * j) * cos(9f * j) * smooth(j / 0.05f)
}

fun mergePillScaleY(merge: Float, contact: Float, pillHeightDp: Float): Float {
    val pushed = 0.08f * mergePush(merge, contact) * (1f - smooth(mergeJoin(merge, contact) / FLAT_FADE_SPAN))
    return 1f + pushed + if (pillHeightDp > 0f) mergePillSwellDp(merge, contact) / pillHeightDp else 0f
}

/** The pill's width as a multiple of its resting width: only the swell widens it. */
fun mergePillScaleX(merge: Float, contact: Float, pillWidthDp: Float): Float =
    1f + if (pillWidthDp > 0f) mergePillSwellDp(merge, contact) / pillWidthDp else 0f

/**
 * Height of the bridge between bubble and pill (0..1 of [MERGE_NECK_HEIGHT_FRACTION] circles): open at the join's
 * start, closed by 37 % of it.
 */
fun mergeNeckHeightFactor(merge: Float, contact: Float): Float {
    val j = mergeJoin(merge, contact)
    return if (j <= 0f) 0f else 1f - smooth(j / NECK_FADE_SPAN)
}

/**
 * Signed distance (dp, positive toward the pill) from the bubble's centre to the pill's near edge when the bubble's
 * centre has moved [offsetDp] toward the pill.
 */
fun mergeEdgeDistanceFromOffsetDp(distDp: Float, pillWidthDp: Float, offsetDp: Float): Float =
    abs(distDp) - pillWidthDp / 2f - offsetDp

fun mergeEdgeDistanceDp(distDp: Float, pillWidthDp: Float, travelDp: Float, merge: Float): Float =
    mergeEdgeDistanceFromOffsetDp(distDp, pillWidthDp, travelDp * merge.coerceIn(0f, 1f))

/**
 * The moment two merging cutouts touch: [merge] has just crossed [contact] on its way up (merging in). Used for the
 * light haptic tick.
 */
fun mergeTouchCrossed(previous: Float, now: Float, contact: Float): Boolean {
    if (now >= 0.999f) return false
    val c = contact.coerceIn(0.02f, 0.98f)
    return previous < c && now >= c && now > previous
}

// Gooey join (Android 12+): the bubble, its neighbour and the bridge between them blurred and alpha-thresholded

private const val JOIN_EPSILON = 1e-4f

/** True while the gooey join is drawn: the join part of a merge at [merge] with first contact at [contact]. The push is crisp. */
fun gooeyBridgeActive(merge: Float, contact: Float): Boolean =
    merge > 0f && merge < 1f && mergeJoin(merge, contact) > JOIN_EPSILON

fun gooeyBlurDp(circleDp: Float): Float = (circleDp * 0.22f).coerceIn(3f, 14f)

/** Margin around the shapes, in blur sigmas, that the gooey layer covers. */
const val GOO_MARGIN_SIGMAS = 1.5f

private const val GOO_MIN_SCALED_BLUR_PX = 6f

/** Resolution the gooey layer is recorded at (1 = full, 0.5, 0.25), drawn back up by the same factor. */
fun gooeyLayerScale(blurPx: Float): Float = when {
    blurPx >= 4f * GOO_MIN_SCALED_BLUR_PX -> 0.25f
    blurPx >= 2f * GOO_MIN_SCALED_BLUR_PX -> 0.5f
    else -> 1f
}

/** Alpha row of the threshold matrix: alpha' = alpha * [GOO_ALPHA_GAIN] + [GOO_ALPHA_OFFSET] (0..255 scale), 50 % maps to 50 %. */
const val GOO_ALPHA_GAIN = 20f
const val GOO_ALPHA_OFFSET = -127.5f * (GOO_ALPHA_GAIN - 1f)

/** A rectangle in the bubble's own pixel space. */
data class GooRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/** The shapes the gooey layer draws, in the bubble box's pixel space (the box is [widthPx] x [heightPx]). */
data class GooShapes(val bubble: GooRect, val pill: GooRect, val neck: GooRect? = null) {
    /** All shapes plus [marginPx] on every side: the area the layer must cover so the blur is not cut off. */
    fun bounds(marginPx: Float): GooRect {
        var l = minOf(bubble.left, pill.left)
        var t = minOf(bubble.top, pill.top)
        var r = maxOf(bubble.right, pill.right)
        var b = maxOf(bubble.bottom, pill.bottom)
        neck?.let {
            l = minOf(l, it.left); t = minOf(t, it.top); r = maxOf(r, it.right); b = maxOf(b, it.bottom)
        }
        return GooRect(l - marginPx, t - marginPx, r + marginPx, b + marginPx)
    }
}

/**
 * Where the bubble, the pill and the bridge between them are, in the bubble box's pixel space, while it merges.
 * The box travels with the bubble's centre, so the bubble is always centred in it and only scales about its centre.
 *
 * @param toward +1 when the pill is to the right of the bubble, -1 when to the left
 * @param bubbleScaleX / [bubbleScaleY] the bubble's current scale
 * @param edgePx signed distance from the bubble's centre to the pill's near edge at rest size (see [mergeEdgeDistanceFromOffsetDp]); negative once inside
 * @param pillScaleX / [pillScaleY] the pill's swell; it scales about its own centre
 * @param neckHeightPx height of the bridge (0 = none); it runs from the bubble's centre to [neckInsidePx] inside the pill's near edge
 */
fun gooeyShapes(
    widthPx: Float,
    heightPx: Float,
    toward: Float,
    bubbleScaleX: Float,
    bubbleScaleY: Float,
    edgePx: Float,
    pillWidthPx: Float,
    pillHeightPx: Float,
    pillScaleX: Float = 1f,
    pillScaleY: Float = 1f,
    neckHeightPx: Float = 0f,
    neckInsidePx: Float = 0f
): GooShapes {
    val cx = widthPx / 2f
    val cy = heightPx / 2f
    val bw = widthPx * bubbleScaleX
    val bh = heightPx * bubbleScaleY
    val bubble = GooRect(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
    val nearEdge = cx + toward * edgePx
    val pillCenter = nearEdge + toward * pillWidthPx / 2f
    val halfW = pillWidthPx * pillScaleX / 2f
    val halfH = pillHeightPx * pillScaleY / 2f
    val pill = GooRect(pillCenter - halfW, cy - halfH, pillCenter + halfW, cy + halfH)
    val neckEnd = nearEdge + toward * neckInsidePx
    val neck = if (neckHeightPx > 0f && (neckEnd - cx) * toward > 0f) {
        GooRect(minOf(cx, neckEnd), cy - neckHeightPx / 2f, maxOf(cx, neckEnd), cy + neckHeightPx / 2f)
    } else null
    return GooShapes(bubble, pill, neck)
}
