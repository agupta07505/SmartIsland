/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

/**
 * 1 per touch. Past [slopPx] the longer axis wins until lift. Feed [update], then pass moves through [filterX] /
 * [filterY]
 */
class AxisLock(private val slopPx: Float) {
    enum class Axis { Horizontal, Vertical }

    /** Null until the finger is past the slop. */
    var axis: Axis? = null
        private set

    /** True only for the call that locked the axis. */
    fun update(rawDx: Float, rawDy: Float): Boolean {
        if (axis != null) return false
        if (maxOf(abs(rawDx), abs(rawDy)) < slopPx) return false
        axis = if (abs(rawDx) > abs(rawDy)) Axis.Horizontal else Axis.Vertical
        return true
    }

    fun filterX(dx: Float): Float = if (axis == Axis.Vertical) 0f else dx

    fun filterY(dy: Float): Float = if (axis == Axis.Horizontal) 0f else dy
}
