/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.hypot

/**
 * Where a touch on the island stands while the finger is down.
 *
 * - [Tap]: the finger has stayed within the tap slop; lifting now is a tap.
 * - [Dead]: the finger has moved too far for a tap but lifting would not fire a swipe either (short of the threshold,
 *   pulled back, too diagonal, or a side with no action). Lifting does nothing. This is the dead zone, and the island
 *   shows it (the shell shrinks a little) so the user can tell it apart from a tap and from an armed swipe.
 * - [Armed]: lifting now fires a swipe (the existing swell and tick).
 */
enum class TouchZone { Tap, Dead, Armed }

fun touchZone(slopExceeded: Boolean, liftFires: Boolean): TouchZone = when {
    liftFires -> TouchZone.Armed
    slopExceeded -> TouchZone.Dead
    else -> TouchZone.Tap
}

/** True while a card step (preview, reader, picture) follows the finger (`stepMode` 1 = deeper, -1 = back). */
fun stepFollowsFinger(stepMode: Int): Boolean = stepMode == 1 || stepMode == -1

fun deadZoneCue(slopExceeded: Boolean, liftFires: Boolean, stepFollowing: Boolean): Boolean =
    !stepFollowing && touchZone(slopExceeded, liftFires) == TouchZone.Dead

/**
 * Decides whether a touch can still end as a tap. The peak distance from the touch-down point counts, not the final
 * one: a finger that went past the slop and came back is a cancelled drag, not a tap.
 */
class TapGuard(private val slopPx: Float) {
    /** Largest distance from the touch-down point so far, in px. */
    var peak: Float = 0f
        private set

    val exceeded: Boolean get() = peak > slopPx

    fun update(dx: Float, dy: Float) {
        val d = hypot(dx, dy)
        if (d > peak) peak = d
    }
}
