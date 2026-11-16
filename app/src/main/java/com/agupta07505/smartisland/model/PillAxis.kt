/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 * Modified for Static Boat (2026)
 */

package com.agupta07505.smartisland.model

/**
 * Maps screen-space finger movement into the collapsed pill's own frame, so one gesture table works in every
 * orientation. In the pill frame, x runs along the pill and y is negative toward the pill's edge ("up").
 * Portrait and the horizontal landscape pill are the identity. Pure logic, see PillAxisTest.
 */
enum class PillAxis {
    Horizontal,
    VerticalLeft,
    VerticalRight;

    /** Returns (along, away) from a screen delta (dx, dy). Toward the edge is negative in the second value. */
    fun toPillFrame(dx: Float, dy: Float): Pair<Float, Float> = when (this) {
        Horizontal -> dx to dy
        // Camera on the left edge: moving left is toward the edge (negative); along the pill is vertical.
        VerticalLeft -> dy to dx
        // Camera on the right edge: moving right is toward the edge, so flip the sign.
        VerticalRight -> dy to -dx
    }

    companion object {
        fun of(vertical: Boolean, cameraOnRight: Boolean): PillAxis = when {
            !vertical -> Horizontal
            cameraOnRight -> VerticalRight
            else -> VerticalLeft
        }
    }
}
