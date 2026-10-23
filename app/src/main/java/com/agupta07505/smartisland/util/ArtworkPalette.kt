/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.graphics.Bitmap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** [hue] in degrees (0 until 360) and [saturation] 0..1 are HSV. */
data class ArtworkTint(val hue: Float, val saturation: Float, val chroma: Float = 0f, val labHue: Float = hue)

/**
 * Finds the colour that stands out most in album art, like the notification shade's media tint: pixels vote for their
 * hue, vivid and bright ones count more, near-black and grey ones do not vote.
 */
object ArtworkPalette {
    private const val HUE_BINS = 36
    private const val SAMPLE_SIZE = 32
    private const val MIN_VALUE = 0.15f
    private const val MIN_SATURATION = 0.12f

    private const val MIN_COLOURED_SHARE = 0.05f

    /** Null when the picture has no real colour (black and white, grey, empty). */
    fun dominantTint(pixels: IntArray): ArtworkTint? {
        val weight = DoubleArray(HUE_BINS)
        val sinSum = DoubleArray(HUE_BINS)
        val cosSum = DoubleArray(HUE_BINS)
        val satSum = DoubleArray(HUE_BINS)
        var opaque = 0
        var coloured = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            opaque++
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val hi = max(r, max(g, b))
            val lo = min(r, min(g, b))
            val v = hi / 255f
            val s = if (hi == 0) 0f else (hi - lo).toFloat() / hi
            if (v < MIN_VALUE || s < MIN_SATURATION) continue
            coloured++
            val d = (hi - lo).toFloat()
            var hue = when (hi) {
                r -> 60f * (((g - b) / d) % 6f)
                g -> 60f * ((b - r) / d + 2f)
                else -> 60f * ((r - g) / d + 4f)
            }
            if (hue < 0f) hue += 360f
            val bin = (hue / (360f / HUE_BINS)).toInt().coerceIn(0, HUE_BINS - 1)
            val w = ((0.25f + s) * v).toDouble()
            val rad = Math.toRadians(hue.toDouble())
            weight[bin] += w
            sinSum[bin] += sin(rad) * w
            cosSum[bin] += cos(rad) * w
            satSum[bin] += s * w
        }
        if (opaque == 0 || coloured < opaque * MIN_COLOURED_SHARE) return null

        // A hue that straddles two bins must not lose to a smaller, tidier one: score each bin with its neighbours.
        var best = 0
        var bestScore = -1.0
        for (i in 0 until HUE_BINS) {
            val score = weight[i] + 0.5 * (weight[(i + HUE_BINS - 1) % HUE_BINS] + weight[(i + 1) % HUE_BINS])
            if (score > bestScore) {
                bestScore = score
                best = i
            }
        }
        var sx = 0.0
        var sy = 0.0
        var sw = 0.0
        var ss = 0.0
        for (offset in -1..1) {
            val i = (best + offset + HUE_BINS) % HUE_BINS
            sx += cosSum[i]
            sy += sinSum[i]
            sw += weight[i]
            ss += satSum[i]
        }
        if (sw <= 0.0) return null
        var hue = Math.toDegrees(atan2(sy, sx)).toFloat()
        if (hue < 0f) hue += 360f
        // CIELAB mean of the pixels in the winning bin and its two neighbours.
        var labA = 0.0
        var labB = 0.0
        var labCount = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val hi = max(r, max(g, b))
            val lo = min(r, min(g, b))
            if (hi / 255f < MIN_VALUE || (if (hi == 0) 0f else (hi - lo).toFloat() / hi) < MIN_SATURATION) continue
            val d = (hi - lo).toFloat()
            var h = when (hi) {
                r -> 60f * (((g - b) / d) % 6f)
                g -> 60f * ((b - r) / d + 2f)
                else -> 60f * ((r - g) / d + 4f)
            }
            if (h < 0f) h += 360f
            val bin = (h / (360f / HUE_BINS)).toInt().coerceIn(0, HUE_BINS - 1)
            val away = minOf((bin - best + HUE_BINS) % HUE_BINS, (best - bin + HUE_BINS) % HUE_BINS)
            if (away > 1) continue
            val lab = LabColor.fromSrgb(r, g, b)
            labA += lab[1]
            labB += lab[2]
            labCount++
        }
        val chroma = if (labCount == 0) 0f else LabColor.chromaOf(labA / labCount, labB / labCount).toFloat()
        val labHue = if (labCount == 0) hue % 360f else LabColor.hueOf(labA, labB).toFloat()
        return ArtworkTint(hue = hue % 360f, saturation = (ss / sw).toFloat().coerceIn(0f, 1f), chroma = chroma, labHue = labHue)
    }

    /** Samples a small copy of the bitmap; null when it cannot be read or has no real colour. */
    fun fromBitmap(bitmap: Bitmap): ArtworkTint? = runCatchingLogged("ArtworkPalette", "Could not read the artwork colour") {
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatchingLogged null
        } else {
            bitmap
        }
        val small = Bitmap.createScaledBitmap(readable, SAMPLE_SIZE, SAMPLE_SIZE, true)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        small.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        dominantTint(pixels)
    }
}
