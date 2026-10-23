/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** CIELAB / LCh maths for the media card's colours. */
object LabColor {
    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883
    private const val EPS = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    /** CIELAB (D65) of an sRGB colour given as 0..255 channels: [L, a, b]. */
    fun fromSrgb(r: Int, g: Int, b: Int): DoubleArray {
        val lr = toLinear(r / 255.0)
        val lg = toLinear(g / 255.0)
        val lb = toLinear(b / 255.0)
        val x = 0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb
        val y = 0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb
        val z = 0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb
        val fx = f(x / XN)
        val fy = f(y / YN)
        val fz = f(z / ZN)
        return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    /** sRGB (0..1 channels, [r, g, b]) of the LCh colour with lightness [l], chroma [c] and hue [hueDeg]. */
    fun lchToSrgb(l: Double, c: Double, hueDeg: Double): FloatArray {
        var lo = 0.0
        var hi = c.coerceAtLeast(0.0)
        if (!inGamut(l, hi, hueDeg)) {
            repeat(30) {
                val mid = (lo + hi) / 2.0
                if (inGamut(l, mid, hueDeg)) lo = mid else hi = mid
            }
            hi = lo
        }
        val rgb = lchToLinear(l, hi, hueDeg)
        return floatArrayOf(fromLinear(rgb[0]).toFloat(), fromLinear(rgb[1]).toFloat(), fromLinear(rgb[2]).toFloat())
    }

    private fun inGamut(l: Double, c: Double, hueDeg: Double): Boolean {
        val rgb = lchToLinear(l, c, hueDeg)
        return rgb.all { it >= -1e-4 && it <= 1.0 + 1e-4 }
    }

    private fun lchToLinear(l: Double, c: Double, hueDeg: Double): DoubleArray {
        val h = Math.toRadians(hueDeg)
        val a = c * cos(h)
        val b = c * sin(h)
        val fy = (l + 16.0) / 116.0
        val fx = fy + a / 500.0
        val fz = fy - b / 200.0
        val x = XN * fInverse(fx)
        val y = YN * (if (l > KAPPA * EPS) fy.pow(3) else l / KAPPA)
        val z = ZN * fInverse(fz)
        return doubleArrayOf(
            3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z
        )
    }

    fun chromaOf(a: Double, b: Double): Double = hypot(a, b)

    fun hueOf(a: Double, b: Double): Double {
        val h = Math.toDegrees(atan2(b, a))
        return if (h < 0.0) h + 360.0 else h
    }

    private fun f(t: Double): Double = if (t > EPS) cbrt(t) else (KAPPA * t + 16.0) / 116.0

    private fun fInverse(f: Double): Double {
        val cube = f * f * f
        return if (cube > EPS) cube else (116.0 * f - 16.0) / KAPPA
    }

    private fun toLinear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun fromLinear(c: Double): Double {
        val v = c.coerceIn(0.0, 1.0)
        return if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
    }

    fun lightnessOf(rgb: FloatArray): Double =
        fromSrgb(Math.round(rgb[0] * 255f), Math.round(rgb[1] * 255f), Math.round(rgb[2] * 255f))[0]
}
