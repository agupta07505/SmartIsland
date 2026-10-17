/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.model

import kotlin.math.max
import kotlin.math.min

/**
 * Where the buddy lives inside the closed pill, in dp from the pill's top-left corner.
 * (Port of `NotchMascotTrack`; the strolling helpers arrive with the Visits phase.)
 */
data class MascotTrack(
    val width: Float,
    val height: Float,
    /** The buddy's square. */
    val size: Float,
    /** Its centre while resting. */
    val rest: Float,
    /** The camera hole: walking there, it is behind it. Null when the device has no hardware cutout. */
    val hidden: ClosedFloatingPointRange<Float>?,
    /** The height of its centre. */
    val baseline: Float,
    /** The top of the pill over it. */
    val ceiling: Float = 0f,
    /** It rests right of the camera, and every stroll runs the other way. */
    val mirrored: Boolean = false,
    /** How many times larger than the pill it is drawn (the Settings preview shows the pill bigger). */
    val scale: Float = 1f
) {
    /**
     * How high a hop of [share] of the buddy's size lifts it, kept clear of the pill's top so it never
     * leaves the pill.
     */
    fun hop(share: Float): Float =
        min(size * share, max(scale, baseline - size / 2f - ceiling - (if (hidden == null) 1f else 2f) * scale))

    /** The same track [by] times larger. */
    fun scaled(by: Float) = MascotTrack(
        width * by, height * by, size * by, rest * by,
        hidden?.let { (it.start * by)..(it.endInclusive * by) },
        baseline * by, ceiling * by, mirrored, scale * by
    )

    companion object {
        /**
         * The track for a collapsed pill.
         *
         * [holeWidthDp] is the camera hole's width and [holeCenterDp] its centre in pill coordinates; both
         * null when there is no hardware cutout. A pill with no camera rests the buddy in the middle
         * whatever the side. With a camera it rests beside the hole, [gap] from it (4..8dp).
         */
        fun forPill(
            pillWidthDp: Float,
            pillHeightDp: Float,
            sizeDp: Float,
            holeWidthDp: Float?,
            holeCenterDp: Float?,
            side: MascotSide
        ): MascotTrack {
            if (holeWidthDp == null || holeCenterDp == null) {
                return MascotTrack(
                    width = pillWidthDp, height = pillHeightDp, size = sizeDp, rest = pillWidthDp / 2f,
                    hidden = null, baseline = pillHeightDp / 2f
                )
            }
            val half = holeWidthDp / 2f
            val holeStart = (holeCenterDp - half).coerceIn(0f, pillWidthDp)
            val holeEnd = (holeCenterDp + half).coerceIn(0f, pillWidthDp)
            // The wing on the buddy's side: from that edge of the pill to the hole.
            val wing = if (side == MascotSide.Left) holeStart else pillWidthDp - holeEnd
            val gap = max(4f, min(8f, wing - sizeDp - 6f))
            val rest = max(sizeDp / 2f, wing - gap - sizeDp / 2f)
            return MascotTrack(
                width = pillWidthDp, height = pillHeightDp, size = sizeDp,
                rest = if (side == MascotSide.Right) pillWidthDp - rest else rest,
                hidden = holeStart..holeEnd,
                baseline = pillHeightDp / 2f + 0.5f,
                mirrored = side == MascotSide.Right
            )
        }
    }
}
