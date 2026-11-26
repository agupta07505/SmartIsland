/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

fun smoothstep(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

fun lerpFloat(from: Float, to: Float, t: Float): Float = from + (to - from) * t

fun ramp(value: Float, start: Float, end: Float): Float =
    if (end <= start) (if (value >= end) 1f else 0f) else ((value - start) / (end - start)).coerceIn(0f, 1f)
