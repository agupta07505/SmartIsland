/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape: how the collapsed cutout leaves when its last card goes: it slides away and fades, then the idle circle
 * opens at its spot.
 */
enum class PillExitDirection { Up, Left, Right }

/** "Near an edge": the pill's centre is within this share of the screen width from it. */
const val PILL_EXIT_EDGE_FRACTION = 0.25f

/** The pill slides no further than this (fading on the way), so a low pill does not fly across the screen. */
const val PILL_EXIT_MAX_TRAVEL_DP = 120f

/** Spare room past the screen edge so the pill is fully clear. */
const val PILL_EXIT_MARGIN_DP = 8f

/** Base exit length (scaled by the animation speed). */
const val PILL_EXIT_MS = 360

fun pillExitDirection(centerXDp: Float, screenWidthDp: Float): PillExitDirection {
    if (screenWidthDp <= 0f) return PillExitDirection.Up
    val fromLeft = screenWidthDp / 2f + centerXDp
    val fromRight = screenWidthDp - fromLeft
    val zone = screenWidthDp * PILL_EXIT_EDGE_FRACTION
    return when {
        fromLeft <= zone && fromLeft <= fromRight -> PillExitDirection.Left
        fromRight <= zone -> PillExitDirection.Right
        else -> PillExitDirection.Up
    }
}

/** Distance (dp) the pill slides: just past the edge it heads for, capped at [PILL_EXIT_MAX_TRAVEL_DP]. */
fun pillExitTravelDp(
    direction: PillExitDirection,
    centerXDp: Float,
    widthDp: Float,
    heightDp: Float,
    topDp: Float,
    screenWidthDp: Float
): Float {
    val toEdge = when (direction) {
        PillExitDirection.Up -> topDp + heightDp
        PillExitDirection.Left -> screenWidthDp / 2f + centerXDp + widthDp / 2f
        PillExitDirection.Right -> screenWidthDp / 2f - centerXDp + widthDp / 2f
    } + PILL_EXIT_MARGIN_DP
    return toEdge.coerceIn(0f, PILL_EXIT_MAX_TRAVEL_DP)
}

data class PillExitOffset(val dx: Float, val dy: Float)

/** Offset (dp) of the sliding pill at [progress] (0 = in place, 1 = gone). */
fun pillExitOffsetDp(direction: PillExitDirection, travelDp: Float, progress: Float): PillExitOffset {
    val d = travelDp * progress.coerceIn(0f, 1f)
    return when (direction) {
        PillExitDirection.Up -> PillExitOffset(0f, -d)
        PillExitDirection.Left -> PillExitOffset(-d, 0f)
        PillExitDirection.Right -> PillExitOffset(d, 0f)
    }
}

private fun smooth(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

fun pillExitAlpha(progress: Float): Float = 1f - smooth((progress - 0.3f) / 0.7f)
