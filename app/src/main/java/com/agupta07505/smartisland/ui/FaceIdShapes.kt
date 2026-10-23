/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val FACE_ID_DESIGN_W = 220f
internal const val FACE_ID_DESIGN_H = 185f
internal const val FACE_ID_HEAD_CX = 110f
internal const val FACE_ID_HEAD_CY = 92f
internal const val FACE_ID_EYE_R = 6.89f
internal const val FACE_ID_EYE_DX = 16.45f
internal const val FACE_ID_EYE_DY = 5.52f

internal val FACE_ID_DISC: FloatArray = floatArrayOf(
    103.2f, 37.2f, 98.9f, 38.0f, 98.5f, 38.2f, 97.8f, 38.2f, 97.6f, 38.4f, 95.0f, 38.9f, 94.9f, 39.2f, 94.3f,
    39.2f, 94.2f, 39.4f, 93.3f, 39.4f, 93.2f, 39.6f, 91.9f, 39.9f, 91.8f, 40.1f, 90.6f, 40.4f, 90.1f, 40.8f,
    89.5f, 40.8f, 89.4f, 41.1f, 88.3f, 41.3f, 87.7f, 41.8f, 86.6f, 42.0f, 86.5f, 42.3f, 84.7f, 43.0f, 84.3f,
    43.5f, 83.4f, 43.7f, 80.5f, 45.7f, 80.1f, 45.7f, 79.5f, 46.4f, 79.2f, 46.4f, 78.8f, 46.9f, 78.4f, 46.9f,
    77.0f, 47.9f, 76.9f, 48.3f, 76.4f, 48.3f, 74.7f, 50.0f, 74.4f, 50.0f, 69.5f, 54.6f, 68.8f, 55.6f, 67.9f,
    56.5f, 67.6f, 57.1f, 66.9f, 57.7f, 64.7f, 60.9f, 64.3f, 61.3f, 64.3f, 61.6f, 63.8f, 62.0f, 63.3f, 63.1f,
    63.1f, 63.2f, 63.1f, 63.6f, 62.6f, 63.9f, 62.6f, 64.3f, 61.1f, 66.6f, 61.1f, 66.9f, 60.2f, 68.5f, 60.2f,
    69.1f, 59.7f, 69.6f, 59.7f, 70.1f, 59.2f, 70.7f, 58.5f, 72.5f, 58.5f, 72.9f, 58.2f, 73.1f, 57.8f, 74.5f,
    57.8f, 75.3f, 57.5f, 75.5f, 57.5f, 76.1f, 57.2f, 76.4f, 56.8f, 77.6f, 56.6f, 79.7f, 56.3f, 79.8f, 55.6f,
    83.3f, 55.4f, 86.9f, 55.1f, 87.3f, 55.0f, 93.1f, 55.4f, 98.9f, 55.6f, 99.5f, 55.6f, 100.5f, 55.8f, 100.7f,
    55.8f, 101.8f, 56.1f, 101.9f, 56.1f, 103.1f, 56.3f, 104.2f, 56.6f, 104.3f, 56.6f, 105.3f, 57.3f, 107.2f,
    57.3f, 107.8f, 57.5f, 107.9f, 57.5f, 108.5f, 57.8f, 108.7f, 57.8f, 109.3f, 58.2f, 109.6f, 58.5f, 111.4f,
    58.7f, 111.5f, 58.7f, 112.0f, 59.4f, 113.8f, 60.2f, 114.9f, 60.2f, 115.4f, 60.9f, 116.8f, 61.4f, 117.3f,
    61.9f, 118.6f, 62.6f, 119.5f, 62.6f, 120.0f, 64.5f, 122.6f, 64.7f, 123.3f, 65.9f, 124.5f, 66.1f, 125.1f,
    67.4f, 126.4f, 67.4f, 126.8f, 68.1f, 127.4f, 68.8f, 128.5f, 73.4f, 133.1f, 73.8f, 133.1f, 74.2f, 133.8f,
    74.5f, 133.8f, 74.8f, 134.3f, 75.2f, 134.3f, 76.3f, 135.5f, 77.1f, 135.7f, 77.2f, 136.2f, 78.6f, 136.9f,
    78.9f, 137.4f, 80.0f, 138.1f, 80.5f, 138.1f, 80.8f, 138.6f, 81.9f, 139.3f, 82.4f, 139.3f, 82.8f, 139.8f,
    83.1f, 139.8f, 83.6f, 140.3f, 84.6f, 140.5f, 85.1f, 141.0f, 86.0f, 141.5f, 86.5f, 141.5f, 86.6f, 141.7f,
    89.6f, 142.9f, 89.7f, 143.2f, 90.3f, 143.2f, 90.5f, 143.4f, 91.7f, 143.9f, 93.7f, 144.4f, 93.8f, 144.6f,
    94.7f, 144.8f, 95.4f, 144.8f, 95.5f, 145.1f, 97.3f, 145.3f, 97.4f, 145.6f, 98.3f, 145.6f, 98.5f, 145.8f,
    99.5f, 145.8f, 99.7f, 146.0f, 100.9f, 146.0f, 101.0f, 146.3f, 102.4f, 146.3f, 102.6f, 146.5f, 104.0f, 146.5f,
    104.7f, 146.8f, 108.4f, 146.8f, 108.6f, 147.0f, 110.8f, 147.0f, 117.4f, 146.5f, 117.9f, 146.3f, 119.1f,
    146.3f, 119.6f, 146.0f, 121.6f, 145.8f, 121.7f, 145.6f, 122.6f, 145.6f, 122.7f, 145.3f, 123.6f, 145.3f,
    123.9f, 145.1f, 124.5f, 145.1f, 124.6f, 144.8f, 125.3f, 144.8f, 125.6f, 144.6f, 126.2f, 144.6f, 128.3f,
    143.9f, 128.5f, 143.6f, 130.3f, 143.2f, 130.9f, 142.7f, 131.5f, 142.7f, 132.5f, 142.0f, 133.6f, 141.7f,
    133.7f, 141.5f, 135.5f, 140.7f, 136.4f, 140.0f, 137.7f, 139.5f, 138.1f, 139.1f, 138.9f, 138.8f, 140.4f,
    137.9f, 140.7f, 137.4f, 141.1f, 137.4f, 141.8f, 136.9f, 142.2f, 136.4f, 142.8f, 136.2f, 143.1f, 135.7f,
    144.9f, 134.5f, 145.8f, 133.5f, 146.4f, 133.3f, 151.2f, 128.5f, 151.2f, 128.1f, 151.9f, 127.6f, 151.9f,
    127.4f, 152.6f, 126.8f, 152.9f, 126.2f, 154.3f, 124.6f, 154.5f, 124.0f, 156.0f, 122.2f, 156.0f, 121.9f,
    156.7f, 121.2f, 156.7f, 120.7f, 157.8f, 119.3f, 157.9f, 118.7f, 158.9f, 117.3f, 158.9f, 116.8f, 159.3f,
    116.5f, 159.8f, 114.9f, 160.6f, 113.8f, 160.8f, 112.7f, 161.0f, 112.6f, 161.8f, 110.8f, 161.8f, 110.3f,
    162.5f, 108.7f, 162.5f, 107.7f, 163.0f, 107.1f, 163.2f, 105.5f, 163.4f, 105.4f, 163.7f, 103.4f, 163.9f,
    103.2f, 163.9f, 102.4f, 164.2f, 102.2f, 164.4f, 100.7f, 164.6f, 97.1f, 164.9f, 97.0f, 165.0f, 91.3f, 164.9f,
    89.7f, 164.6f, 89.5f, 164.6f, 88.9f, 164.9f, 88.8f, 164.9f, 87.3f, 164.6f, 87.1f, 164.6f, 85.8f, 164.4f,
    85.7f, 164.4f, 83.3f, 164.2f, 83.0f, 163.9f, 80.6f, 163.7f, 80.4f, 163.7f, 79.8f, 163.4f, 79.7f, 163.3f,
    78.1f, 163.1f, 77.4f, 162.7f, 77.3f, 162.5f, 75.2f, 162.1f, 74.1f, 161.5f, 73.4f, 161.5f, 72.5f, 160.8f,
    70.7f, 160.6f, 70.5f, 160.1f, 68.6f, 159.8f, 68.4f, 159.8f, 67.8f, 159.6f, 67.7f, 159.6f, 67.1f, 159.1f,
    66.5f, 159.1f, 65.9f, 158.9f, 65.7f, 158.6f, 64.8f, 157.9f, 63.8f, 157.7f, 63.0f, 157.2f, 62.6f, 157.2f,
    62.2f, 156.2f, 60.8f, 155.5f, 60.2f, 155.5f, 59.8f, 154.8f, 59.2f, 154.5f, 58.6f, 153.3f, 57.6f, 153.3f,
    57.3f, 148.8f, 52.9f, 147.9f, 52.4f, 147.3f, 51.7f, 145.5f, 50.5f, 145.2f, 50.0f, 144.6f, 49.7f, 143.6f,
    48.8f, 142.6f, 48.3f, 142.3f, 47.8f, 141.9f, 47.8f, 141.6f, 47.3f, 140.2f, 46.6f, 139.6f, 46.0f, 139.0f,
    45.9f, 137.6f, 44.7f, 137.1f, 44.7f, 136.7f, 44.2f, 135.9f, 44.0f, 135.5f, 43.5f, 134.7f, 43.3f, 134.3f,
    42.8f, 134.0f, 42.8f, 132.7f, 41.8f, 131.5f, 41.3f, 130.9f, 41.3f, 129.8f, 40.6f, 128.0f, 40.1f, 127.9f,
    39.9f, 127.3f, 39.9f, 127.1f, 39.6f, 126.4f, 39.4f, 125.8f, 39.4f, 125.7f, 39.2f, 124.8f, 39.2f, 124.7f,
    38.9f, 123.0f, 38.4f, 122.2f, 38.4f, 122.1f, 38.2f, 120.8f, 38.2f, 120.6f, 38.0f, 119.7f, 37.7f, 118.1f,
    37.7f, 118.0f, 37.5f, 114.5f, 37.2f, 114.1f, 37.0f, 105.9f, 37.0f, 105.3f, 37.2f,
)

internal val FACE_ID_HOLE: FloatArray = floatArrayOf(
    97.3f, 60.8f, 99.1f, 62.4f, 100.8f, 64.0f, 102.6f, 65.7f, 104.4f, 67.3f, 106.1f, 68.9f, 108.2f, 70.5f, 110.2f,
    71.7f, 112.3f, 73.1f, 114.5f, 74.3f, 116.8f, 75.6f, 119.1f, 76.6f, 121.4f, 77.5f, 123.9f, 78.2f, 126.3f,
    79.0f, 128.9f, 79.7f, 131.5f, 80.2f, 134.3f, 80.4f, 137.0f, 80.6f, 139.7f, 80.9f, 142.5f, 80.7f, 145.3f,
    80.5f, 147.6f, 81.6f, 149.4f, 83.0f, 151.5f, 84.6f, 153.6f, 86.2f, 154.1f, 88.9f, 154.2f, 91.7f, 154.2f,
    94.5f, 153.9f, 97.2f, 153.5f, 99.8f, 153.0f, 102.4f, 152.3f, 104.9f, 151.4f, 107.3f, 150.5f, 109.6f, 149.4f,
    111.9f, 148.2f, 114.0f, 147.0f, 116.2f, 145.5f, 118.1f, 144.0f, 120.1f, 142.4f, 121.9f, 140.7f, 123.6f,
    139.0f, 125.2f, 137.2f, 126.9f, 135.2f, 128.1f, 133.1f, 129.6f, 131.0f, 130.8f, 128.7f, 132.0f, 126.5f,
    132.9f, 124.1f, 133.8f, 121.5f, 134.6f, 119.1f, 135.3f, 116.4f, 135.6f, 113.7f, 135.9f, 110.8f, 136.1f,
    108.0f, 136.1f, 105.2f, 135.8f, 102.5f, 135.5f, 99.9f, 134.9f, 97.4f, 134.3f, 94.8f, 133.4f, 92.5f, 132.5f,
    90.2f, 131.4f, 88.0f, 130.2f, 85.8f, 129.0f, 83.9f, 127.6f, 82.0f, 126.2f, 80.3f, 124.5f, 78.6f, 122.9f,
    76.9f, 121.2f, 75.2f, 119.3f, 73.9f, 117.3f, 72.5f, 115.2f, 71.1f, 113.1f, 70.1f, 111.0f, 69.1f, 108.6f,
    68.2f, 106.2f, 67.4f, 103.6f, 66.8f, 101.1f, 66.3f, 98.5f, 66.1f, 95.8f, 65.8f, 93.0f, 66.7f, 90.7f, 69.0f,
    89.8f, 71.1f, 88.7f, 73.3f, 87.6f, 75.4f, 86.4f, 77.4f, 85.1f, 79.3f, 83.7f, 81.2f, 82.3f, 83.0f, 80.7f,
    84.8f, 79.1f, 86.5f, 77.5f, 87.9f, 75.5f, 89.6f, 73.8f, 91.2f, 71.8f, 92.5f, 69.8f, 93.9f, 67.8f, 95.2f,
    65.6f, 96.4f, 63.2f,
)

internal val FACE_ID_TICK: FloatArray = floatArrayOf(
    89.7f, 95.3f, 101.7f, 107.4f, 131.0f, 74.7f, 134.0f, 72.6f, 137.5f, 72.0f, 140.9f, 73.0f, 143.5f, 75.4f,
    144.9f, 78.7f, 144.7f, 82.2f, 143.0f, 85.3f, 108.0f, 124.3f, 105.9f, 126.0f, 103.5f, 126.9f, 100.9f, 126.9f,
    98.4f, 126.2f, 96.3f, 124.7f, 76.4f, 103.6f, 76.1f, 100.1f, 77.2f, 96.7f, 79.7f, 94.2f, 83.1f, 93.1f,
    86.6f, 93.4f,
)

private fun faceIdCrossPoints(cx: Float, cy: Float, arm: Float, half: Float): FloatArray {
    val s = arm * 0.70710678f
    val t = half * 0.70710678f
    return floatArrayOf(
        cx - s - t, cy - s + t, cx - s + t, cy - s - t, cx, cy - 2f * t,
        cx + s - t, cy - s - t, cx + s + t, cy - s + t, cx + 2f * t, cy,
        cx + s + t, cy + s - t, cx + s - t, cy + s + t, cx, cy + 2f * t,
        cx - s + t, cy + s + t, cx - s - t, cy + s - t, cx - 2f * t, cy
    )
}

internal val FACE_ID_CROSS: FloatArray = faceIdCrossPoints(FACE_ID_HEAD_CX, FACE_ID_HEAD_CY, 31f, 7f)

internal const val FACE_ID_MORPH_POINTS = 160

private fun faceIdSignedArea(p: FloatArray): Float {
    val n = p.size / 2
    var a = 0f
    for (i in 0 until n) {
        val j = (i + 1) % n
        a += p[2 * i] * p[2 * j + 1] - p[2 * j] * p[2 * i + 1]
    }
    return a / 2f
}

internal fun faceIdResample(p: FloatArray, count: Int): FloatArray {
    val n = p.size / 2
    val cum = FloatArray(n + 1)
    for (i in 0 until n) {
        val j = (i + 1) % n
        cum[i + 1] = cum[i] + kotlin.math.hypot(p[2 * j] - p[2 * i], p[2 * j + 1] - p[2 * i + 1])
    }
    val total = cum[n]
    val out = FloatArray(count * 2)
    var seg = 0
    for (k in 0 until count) {
        val s = total * k / count
        while (seg < n - 1 && cum[seg + 1] < s) seg++
        val j = (seg + 1) % n
        val len = cum[seg + 1] - cum[seg]
        val t = if (len <= 0f) 0f else (s - cum[seg]) / len
        out[2 * k] = p[2 * seg] + (p[2 * j] - p[2 * seg]) * t
        out[2 * k + 1] = p[2 * seg + 1] + (p[2 * j + 1] - p[2 * seg + 1]) * t
    }
    return out
}

internal fun faceIdAlign(from: FloatArray, to: FloatArray): FloatArray {
    val n = from.size / 2
    var src = to
    if (faceIdSignedArea(from) * faceIdSignedArea(to) < 0f) {
        src = FloatArray(to.size)
        for (i in 0 until n) {
            src[2 * i] = to[2 * (n - 1 - i)]
            src[2 * i + 1] = to[2 * (n - 1 - i) + 1]
        }
    }
    var bestShift = 0
    var bestCost = Float.MAX_VALUE
    for (shift in 0 until n) {
        var cost = 0f
        for (i in 0 until n) {
            val j = (i + shift) % n
            val dx = from[2 * i] - src[2 * j]
            val dy = from[2 * i + 1] - src[2 * j + 1]
            cost += dx * dx + dy * dy
        }
        if (cost < bestCost) {
            bestCost = cost
            bestShift = shift
        }
    }
    val out = FloatArray(src.size)
    for (i in 0 until n) {
        val j = (i + bestShift) % n
        out[2 * i] = src[2 * j]
        out[2 * i + 1] = src[2 * j + 1]
    }
    return out
}

internal fun faceIdMorphOutlines(): Pair<FloatArray, FloatArray> {
    val face = faceIdResample(FACE_ID_HOLE, FACE_ID_MORPH_POINTS)
    val tick = faceIdAlign(face, faceIdResample(FACE_ID_TICK, FACE_ID_MORPH_POINTS))
    return face to tick
}

internal fun faceIdCrossMorphOutlines(): Pair<FloatArray, FloatArray> {
    val face = faceIdResample(FACE_ID_HOLE, FACE_ID_MORPH_POINTS)
    val cross = faceIdAlign(face, faceIdResample(FACE_ID_CROSS, FACE_ID_MORPH_POINTS))
    return face to cross
}

internal fun faceIdBlend(a: FloatArray, b: FloatArray, t: Float, out: FloatArray) {
    for (i in a.indices) out[i] = a[i] + (b[i] - a[i]) * t
}

// Tick split and edge arcs, in the same design units as the face.

internal const val FACE_ID_TICK_SPLIT_X = 110.5f

internal const val FACE_ID_SPLIT_POINTS = 128

internal const val FACE_ID_ARC_REACH_DP = 14f

/** Gap between the arcs and the cutout's edge (dp). */
internal const val FACE_ID_ARC_INSET_DP = 0.5f

/** Arc thickness choices. The accepted animation uses Sliver. */
internal enum class FaceIdArcThickness(val dp: Float) {
    Sliver(1.25f),
    Thin(2f),
    Medium(3f),
    Thick(4.5f)
}

internal class FaceIdSplitOutlines(
    val leftFrom: FloatArray,
    val leftTo: FloatArray,
    val rightFrom: FloatArray,
    val rightTo: FloatArray
)

internal fun faceIdClipX(p: FloatArray, splitX: Float, keepLeft: Boolean): FloatArray {
    val n = p.size / 2
    val out = ArrayList<Float>()
    fun inside(x: Float): Boolean = if (keepLeft) x <= splitX else x >= splitX
    for (i in 0 until n) {
        val j = (i + 1) % n
        val ax = p[2 * i]
        val ay = p[2 * i + 1]
        val bx = p[2 * j]
        val by = p[2 * j + 1]
        val aIn = inside(ax)
        val bIn = inside(bx)
        if (aIn) {
            out.add(ax)
            out.add(ay)
        }
        if (aIn != bIn) {
            val t = (splitX - ax) / (bx - ax)
            out.add(splitX)
            out.add(ay + (by - ay) * t)
        }
    }
    return out.toFloatArray()
}

private const val FACE_ID_HALF_PI = 1.5707964f
private const val FACE_ID_PI = 3.1415927f
private const val FACE_ID_ARC_SEGMENTS = 48
private const val FACE_ID_CAP_STEPS = 6

internal const val FACE_ID_ARC_OUTLINE_POINTS = 2 * (FACE_ID_ARC_SEGMENTS + 1) + 2 * (FACE_ID_CAP_STEPS - 1)

internal const val FACE_ID_ARC_TIP_WIDTH = 0.25f

private const val FACE_ID_MIN_GAP_STROKES = 3f

internal class FaceIdEdgeTrack(
    private val cx: Float,
    private val cy: Float,
    w: Float,
    h: Float,
    r: Float,
    val thickness: Float,
    reach: Float,
    inset: Float
) {
    private val d = inset + thickness / 2f
    private val a = max(w / 2f - d, 0.01f)
    private val b = max(h / 2f - d, 0.01f)
    private val rc = min(max(min(r, min(w / 2f, h / 2f)) - d, 0f), min(a, b))
    private val flatX = a - rc
    private val flatY = b - rc
    private val quarter = rc * FACE_ID_HALF_PI

    // end of each stretch, starting from the top-left half
    private val e0 = flatX
    private val e1 = e0 + quarter
    private val e2 = e1 + 2f * flatY
    private val e3 = e2 + quarter
    private val e4 = e3 + 2f * flatX
    private val e5 = e4 + quarter
    private val e6 = e5 + 2f * flatY
    private val e7 = e6 + quarter

    /** Length of the whole track. */
    val perimeter: Float = e7 + flatX

    /** Length of one arc along the track. */
    val arcLength: Float

    val arcStart: Float

    init {
        val reachUsed = min(reach, max(flatX * 0.6f, 0f))
        val wanted = 2f * reachUsed + 2f * quarter + 2f * flatY
        val longest = perimeter / 2f - FACE_ID_MIN_GAP_STROKES * thickness
        arcLength = min(wanted, max(longest, thickness))
        arcStart = (e1 + flatY) - arcLength / 2f
    }

    private val hws = FloatArray(FACE_ID_ARC_SEGMENTS + 1)
    private val sx = FloatArray(FACE_ID_ARC_SEGMENTS + 1)
    private val sy = FloatArray(FACE_ID_ARC_SEGMENTS + 1)
    private val tx = FloatArray(FACE_ID_ARC_SEGMENTS + 1)
    private val ty = FloatArray(FACE_ID_ARC_SEGMENTS + 1)
    private val pt = FloatArray(2)

    private fun wrap(s: Float): Float {
        var v = s % perimeter
        if (v < 0f) v += perimeter
        return v
    }

    fun pointAt(s: Float, out: FloatArray, at: Int) {
        val v = wrap(s)
        val x: Float
        val y: Float
        when {
            v <= e0 -> { x = cx - v; y = cy - b }
            v <= e1 -> {
                val th = -FACE_ID_HALF_PI - (v - e0) / max(rc, 1e-6f)
                x = cx - flatX + rc * cos(th); y = cy - flatY + rc * sin(th)
            }
            v <= e2 -> { x = cx - a; y = cy - flatY + (v - e1) }
            v <= e3 -> {
                val th = FACE_ID_PI - (v - e2) / max(rc, 1e-6f)
                x = cx - flatX + rc * cos(th); y = cy + flatY + rc * sin(th)
            }
            v <= e4 -> { x = cx - flatX + (v - e3); y = cy + b }
            v <= e5 -> {
                val th = FACE_ID_HALF_PI - (v - e4) / max(rc, 1e-6f)
                x = cx + flatX + rc * cos(th); y = cy + flatY + rc * sin(th)
            }
            v <= e6 -> { x = cx + a; y = cy + flatY - (v - e5) }
            v <= e7 -> {
                val th = -(v - e6) / max(rc, 1e-6f)
                x = cx + flatX + rc * cos(th); y = cy - flatY + rc * sin(th)
            }
            else -> { x = cx + flatX - (v - e7); y = cy - b }
        }
        out[at] = x
        out[at + 1] = y
    }

    internal fun halfWidthAt(k: Int): Float {
        val half = arcLength / 2f
        val fromCentre = abs((k.toFloat() / FACE_ID_ARC_SEGMENTS - 0.5f) * arcLength)
        val full = min(flatY, half)
        val t = if (half - full < 1e-4f) 0f else ((fromCentre - full) / (half - full)).coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)
        return thickness / 2f * (1f - (1f - FACE_ID_ARC_TIP_WIDTH) * smooth)
    }

    fun outlineInto(sStart: Float, out: FloatArray) {
        val last = FACE_ID_ARC_SEGMENTS
        for (k in 0..last) {
            pointAt(sStart + arcLength * k / last, pt, 0)
            sx[k] = pt[0]
            sy[k] = pt[1]
        }
        for (k in 0..last) {
            val k0 = max(k - 1, 0)
            val k1 = min(k + 1, last)
            val dx = sx[k1] - sx[k0]
            val dy = sy[k1] - sy[k0]
            val len = max(hypot(dx, dy), 1e-6f)
            tx[k] = dx / len
            ty[k] = dy / len
        }
        for (k in 0..last) hws[k] = halfWidthAt(k)
        var o = 0
        fun put(x: Float, y: Float) {
            out[o++] = x
            out[o++] = y
        }
        for (k in 0..last) put(sx[k] - ty[k] * hws[k], sy[k] + tx[k] * hws[k])
        val endAngle = atan2(tx[last], -ty[last])
        for (i in 1 until FACE_ID_CAP_STEPS) {
            val an = endAngle - FACE_ID_PI * i / FACE_ID_CAP_STEPS
            put(sx[last] + hws[last] * cos(an), sy[last] + hws[last] * sin(an))
        }
        for (k in last downTo 0) put(sx[k] + ty[k] * hws[k], sy[k] - tx[k] * hws[k])
        val startAngle = atan2(-tx[0], ty[0])
        for (i in 1 until FACE_ID_CAP_STEPS) {
            val an = startAngle - FACE_ID_PI * i / FACE_ID_CAP_STEPS
            put(sx[0] + hws[0] * cos(an), sy[0] + hws[0] * sin(an))
        }
    }

    fun leftOutlineInto(turn: Float, out: FloatArray) = outlineInto(arcStart + turn, out)

    fun rightOutlineInto(turn: Float, out: FloatArray) = outlineInto(arcStart + perimeter / 2f + turn, out)
}

internal fun faceIdSplitOutlines(track: FaceIdEdgeTrack): FaceIdSplitOutlines {
    val buffer = FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2)
    track.leftOutlineInto(0f, buffer)
    val arcLeft = faceIdResample(buffer, FACE_ID_SPLIT_POINTS)
    track.rightOutlineInto(0f, buffer)
    val arcRight = faceIdResample(buffer, FACE_ID_SPLIT_POINTS)
    val tickLeft = faceIdResample(faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, true), FACE_ID_SPLIT_POINTS)
    val tickRight = faceIdResample(faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, false), FACE_ID_SPLIT_POINTS)
    return FaceIdSplitOutlines(
        leftFrom = tickLeft,
        leftTo = faceIdAlign(tickLeft, arcLeft),
        rightFrom = tickRight,
        rightTo = faceIdAlign(tickRight, arcRight)
    )
}
