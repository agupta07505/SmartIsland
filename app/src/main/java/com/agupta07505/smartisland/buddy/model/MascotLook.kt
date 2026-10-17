/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.model

/** Plain 2D value types so the model has no Android imports and runs on the JVM. */
data class MascotPoint(val x: Float, val y: Float) {
    companion object { val Zero = MascotPoint(0f, 0f) }
}

data class MascotSize(val width: Float, val height: Float)

enum class MascotStyle(val id: String) {
    Minimal("minimal"), Robot("robot");

    companion object {
        fun fromId(id: String?, fallback: MascotStyle = Minimal): MascotStyle =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

enum class MascotShape(val id: String) {
    Ball("ball"), Egg("egg"), Squircle("squircle"), Pill("pill");

    companion object {
        fun fromId(id: String?, fallback: MascotShape = Ball): MascotShape =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

/** sRGB components in 0..1. */
data class MascotColor(val red: Float, val green: Float, val blue: Float, val alpha: Float = 1f) {
    /** Packed 0xAARRGGBB, for Compose `Color(Long)`. */
    fun toArgb(): Long {
        fun c(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt().toLong()
        return (c(alpha) shl 24) or (c(red) shl 16) or (c(green) shl 8) or c(blue)
    }

    /** WCAG relative luminance (alpha ignored). */
    fun luminance(): Double {
        fun lin(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin(red) + 0.7152 * lin(green) + 0.0722 * lin(blue)
    }

    companion object {
        /** The eyes: a soft black that still reads on the black island's edge. */
        val Ink = MascotColor(0.07f, 0.07f, 0.08f)
        /** The robot's face plate, where its lit eyes sit. */
        val Visor = MascotColor(0.09f, 0.09f, 0.11f)
        val Shine = MascotColor(1f, 1f, 1f, 0.55f)
        val IslandBlack = MascotColor(0f, 0f, 0f)

        /** WCAG contrast ratio between two colors (1..21). */
        fun contrast(a: MascotColor, b: MascotColor): Double {
            val la = a.luminance()
            val lb = b.luminance()
            val hi = maxOf(la, lb)
            val lo = minOf(la, lb)
            return (hi + 0.05) / (lo + 0.05)
        }
    }
}

/** The colors it comes in, in rainbow order after white so the swatches read as one row. */
enum class MascotPalette(
    val id: String,
    /** The body's lit top. */
    val light: MascotColor,
    /** The body's shaded bottom, and the robot's antenna and bolts. */
    val shade: MascotColor
) {
    Pearl("pearl", MascotColor(0.99f, 0.98f, 0.96f), MascotColor(0.84f, 0.85f, 0.90f)),
    Lemon("lemon", MascotColor(1.00f, 0.96f, 0.64f), MascotColor(0.97f, 0.83f, 0.30f)),
    Peach("peach", MascotColor(1.00f, 0.86f, 0.74f), MascotColor(0.98f, 0.64f, 0.50f)),
    Rose("rose", MascotColor(1.00f, 0.80f, 0.87f), MascotColor(0.95f, 0.54f, 0.68f)),
    Lilac("lilac", MascotColor(0.88f, 0.83f, 1.00f), MascotColor(0.69f, 0.59f, 0.96f)),
    Sky("sky", MascotColor(0.77f, 0.90f, 1.00f), MascotColor(0.43f, 0.67f, 0.97f)),
    Mint("mint", MascotColor(0.72f, 0.97f, 0.86f), MascotColor(0.40f, 0.83f, 0.66f));

    companion object {
        fun fromId(id: String?, fallback: MascotPalette = Pearl): MascotPalette =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

data class MascotLook(val style: MascotStyle, val shape: MascotShape, val palette: MascotPalette) {
    companion object {
        val Standard = MascotLook(MascotStyle.Minimal, MascotShape.Ball, MascotPalette.Pearl)
    }
}

/** Which side of the camera the buddy rests on. */
enum class MascotSide(val id: String) {
    Left("left"), Right("right");

    companion object {
        fun fromId(id: String?, fallback: MascotSide = Left): MascotSide =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

/**
 * How often the buddy strolls through the island. [Off] has no delay; the others wait a random
 * time in [delaySec] between visits.
 */
enum class MascotVisitFrequency(val id: String, val delaySec: ClosedFloatingPointRange<Float>?) {
    Off("off", null),
    Rare("rare", 600f..1200f),
    Normal("normal", 240f..540f),
    Frequent("frequent", 90f..180f);

    companion object {
        fun fromId(id: String?, fallback: MascotVisitFrequency = Normal): MascotVisitFrequency =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}
