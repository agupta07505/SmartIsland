/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

/**
 * Locks one touch to one axis. Once the finger has moved [slopPx] in either direction, the longer axis wins and the
 * other is ignored until the finger lifts.
 *
 * One instance per touch. Feed it the raw drag so far with [update], and pass each move through [filterX] / [filterY].
 */
class AxisLock(private val slopPx: Float) {
    enum class Axis { Horizontal, Vertical }

    /** The locked axis, or null while the finger is still inside the slop. */
    var axis: Axis? = null
        private set

    /** Feeds the raw drag from the touch-down point. Returns true only for the call that locked the axis. */
    fun update(rawDx: Float, rawDy: Float): Boolean {
        if (axis != null) return false
        if (maxOf(abs(rawDx), abs(rawDy)) < slopPx) return false
        axis = if (abs(rawDx) > abs(rawDy)) Axis.Horizontal else Axis.Vertical
        return true
    }

    fun filterX(dx: Float): Float = if (axis == Axis.Vertical) 0f else dx

    fun filterY(dy: Float): Float = if (axis == Axis.Horizontal) 0f else dy
}
