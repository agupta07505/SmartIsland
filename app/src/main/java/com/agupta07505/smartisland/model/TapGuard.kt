/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.hypot

enum class TouchZone { Tap, Dead, Armed }

fun touchZone(slopExceeded: Boolean, liftFires: Boolean): TouchZone = when {
    liftFires -> TouchZone.Armed
    slopExceeded -> TouchZone.Dead
    else -> TouchZone.Tap
}

fun stepFollowsFinger(stepMode: Int): Boolean = stepMode == 1 || stepMode == -1

fun deadZoneCue(slopExceeded: Boolean, liftFires: Boolean, stepFollowing: Boolean): Boolean =
    !stepFollowing && touchZone(slopExceeded, liftFires) == TouchZone.Dead

class TapGuard(
    private val slopPx: Float,
    private val horizontalSlopPx: Float = slopPx
) {
    var peak: Float = 0f
        private set

    var peakX: Float = 0f
        private set

    val exceeded: Boolean get() = peak > slopPx || peakX > horizontalSlopPx

    fun update(dx: Float, dy: Float) {
        val d = hypot(dx, dy)
        if (d > peak) peak = d
        val x = abs(dx)
        if (x > peakX) peakX = x
    }

    companion object {
        const val CAMERA_BAND_SIDEWAYS_SLOP_DP = 3f
    }
}
