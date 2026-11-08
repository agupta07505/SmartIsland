/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.smoothstep
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp

// Maths for the "bubble" merge style (MergeStyle.BUBBLE): the 2 bodies first push into each other,
// then join. `merge` runs 0..1 over the whole thing, `contact` is where in that range they first touch.

// how far the bodies press into each other during the push
const val MERGE_PUSH_DEPTH_DP = 7f

private const val JOIN_SINK_DP = 14f

// how far the bridge between bubble and pill reaches into the pill
const val MERGE_NECK_INSIDE_DP = 10f

// bridge height at its widest, relative to the round cutout
const val MERGE_NECK_HEIGHT_FRACTION = 0.65f

// PUSH_SPAN and ICON_FADE_SPAN are fractions of the range after contact; the other *_SPAN values
// are fractions of the join phase (mergeJoin 0..1)
private const val PUSH_SPAN = 0.37f

private const val ICON_FADE_SPAN = 0.33f

private const val JOIN_MOVE_SPAN = 0.53f

private const val NECK_FADE_SPAN = 0.37f

private const val FLAT_FADE_SPAN = 0.15f

private const val PILL_SWELL_DP = 5f

private fun postContactSpan(contact: Float): Float = (1f - contact).coerceAtLeast(0.05f)

/** 0 until contact, 1 once the push is over. */
fun mergePush(merge: Float, contact: Float): Float =
    smoothstep((merge - contact) / (postContactSpan(contact) * PUSH_SPAN))

fun mergeJoin(merge: Float, contact: Float): Float {
    val span = postContactSpan(contact)
    return ((merge - contact - span * PUSH_SPAN) / (span * (1f - PUSH_SPAN))).coerceIn(0f, 1f)
}

fun mergeIconAlpha(merge: Float, contact: Float): Float =
    1f - smoothstep((merge - contact) / (postContactSpan(contact) * ICON_FADE_SPAN))

/** Flatness of the touching faces: grows with the push, rounds off again early in the join. */
fun mergeFaceFlat(merge: Float, contact: Float): Float =
    mergePush(merge, contact) * (1f - smoothstep(mergeJoin(merge, contact) / FLAT_FADE_SPAN))

fun mergeNearCornerFactor(flat: Float): Float = 1f - 0.65f * flat.coerceIn(0f, 1f)

private fun joinMove(merge: Float, contact: Float): Float = smoothstep(mergeJoin(merge, contact) / JOIN_MOVE_SPAN)

/**
 * Bubble centre travel toward the pill: crosses [gapDp], gets pressed in a bit by the push, then sinks
 * [JOIN_SINK_DP] into the pill during the join.
 */
fun mergeBubbleOffsetDp(merge: Float, contact: Float, gapDp: Float, bubbleWidthDp: Float, pillWidthDp: Float): Float {
    val m = merge.coerceIn(0f, 1f)
    if (m <= contact) return if (contact <= 0f) 0f else gapDp * (m / contact)
    // The near face moves in by the full push depth but the body only narrows by 0.6 of it (see
    // mergeBubbleScaleX), so the centre moves D - 0.6D/2 = 0.7D
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

// NOTE: the 0.7 in mergeBubbleOffsetDp comes from the 0.6 here, change them together
fun mergeBubbleScaleX(merge: Float, contact: Float, bubbleWidthDp: Float): Float {
    val narrow = if (bubbleWidthDp <= 0f) 1f else 1f - 0.6f * MERGE_PUSH_DEPTH_DP * mergePush(merge,
        contact) / bubbleWidthDp
    return narrow.coerceAtLeast(0.5f) * mergeBubbleShrink(merge, contact)
}

fun mergeBubbleScaleY(merge: Float, contact: Float): Float =
    (1f + 0.06f * mergePush(merge, contact)) * mergeBubbleShrink(merge, contact)

/** Damped-oscillation swell of the pill at join start. Can dip a little below zero as it rings out. */
fun mergePillSwellDp(merge: Float, contact: Float): Float {
    val j = mergeJoin(merge, contact)
    if (j <= 0f) return 0f
    return PILL_SWELL_DP * exp(-5f * j) * cos(9f * j) * smoothstep(j / 0.05f)
}

fun mergePillScaleY(merge: Float, contact: Float, pillHeightDp: Float): Float {
    val pushed = 0.08f * mergePush(merge, contact) * (1f - smoothstep(mergeJoin(merge, contact) / FLAT_FADE_SPAN))
    return 1f + pushed + if (pillHeightDp > 0f) mergePillSwellDp(merge, contact) / pillHeightDp else 0f
}

fun mergePillScaleX(merge: Float, contact: Float, pillWidthDp: Float): Float =
    1f + if (pillWidthDp > 0f) mergePillSwellDp(merge, contact) / pillWidthDp else 0f

/** Bridge height, 0..1 of [MERGE_NECK_HEIGHT_FRACTION]. Fully open when the join starts, closed 37% of the way through. */
fun mergeNeckHeightFactor(merge: Float, contact: Float): Float {
    val j = mergeJoin(merge, contact)
    return if (j <= 0f) 0f else 1f - smoothstep(j / NECK_FADE_SPAN)
}

/** Signed distance from the bubble centre to the pill's near edge after moving [offsetDp] (positive = toward the pill). */
fun mergeEdgeDistanceFromOffsetDp(distDp: Float, pillWidthDp: Float, offsetDp: Float): Float =
    abs(distDp) - pillWidthDp / 2f - offsetDp

fun mergeEdgeDistanceDp(distDp: Float, pillWidthDp: Float, travelDp: Float, merge: Float): Float =
    mergeEdgeDistanceFromOffsetDp(distDp, pillWidthDp, travelDp * merge.coerceIn(0f, 1f))

/** True on the frame [merge] crosses [contact] going up. Triggers the light haptic tick. */
fun mergeTouchCrossed(previous: Float, now: Float, contact: Float): Boolean {
    if (now >= 0.999f) return false
    val c = contact.coerceIn(0.02f, 0.98f)
    return previous < c && now >= c && now > previous
}

// Gooey join (Android 12+): blur bubble + neighbour + bridge, then alpha-threshold so they melt together

private const val JOIN_EPSILON = 1e-4f

/** Gooey effect only runs during the join; the push stays crisp. */
fun gooeyBridgeActive(merge: Float, contact: Float): Boolean =
    merge > 0f && merge < 1f && mergeJoin(merge, contact) > JOIN_EPSILON

fun gooeyBlurDp(circleDp: Float): Float = (circleDp * 0.22f).coerceIn(3f, 14f)

// padding around the shapes, in blur sigmas, so the blur isn't clipped
const val GOO_MARGIN_SIGMAS = 1.5f

private const val GOO_MIN_SCALED_BLUR_PX = 6f

/** Render the gooey layer at lower resolution when the blur is big enough to hide it, then scale back up. */
fun gooeyLayerScale(blurPx: Float): Float = when {
    blurPx >= 4f * GOO_MIN_SCALED_BLUR_PX -> 0.25f
    blurPx >= 2f * GOO_MIN_SCALED_BLUR_PX -> 0.5f
    else -> 1f
}

// Alpha row of the threshold color matrix: alpha' = alpha * GAIN + OFFSET (0..255 scale).
// The offset is chosen so 50% alpha stays 50%; higher gain = harder edge.
const val GOO_ALPHA_GAIN = 20f
const val GOO_ALPHA_OFFSET = -127.5f * (GOO_ALPHA_GAIN - 1f)

// rect in the bubble's own pixel space
data class GooRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

// Shapes for the gooey layer, in the bubble box's pixel space
data class GooShapes(val bubble: GooRect, val pill: GooRect, val neck: GooRect? = null) {
    /** Bounding box of everything plus [marginPx], so the blur doesn't get cut off. */
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

// Bubble, pill and bridge geometry in the moving bubble container's coordinates
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
