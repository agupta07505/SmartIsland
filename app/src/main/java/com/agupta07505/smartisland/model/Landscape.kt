/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

object LandscapeLayoutStyle {
    const val CLASSIC = "classic"
    const val EDGE = "edge"

    /** Layout used until the owner picks one: the edge pill. */
    const val DEFAULT = EDGE

    val VALID_IDS: Set<String> = setOf(CLASSIC, EDGE)

    fun fromId(id: String?): String = if (id != null && id in VALID_IDS) id else DEFAULT

    fun isEdge(id: String?): Boolean = fromId(id) == EDGE
}

fun landscapeWidgetPillWanted(
    isLandscape: Boolean,
    widgetsEnabled: Boolean,
    widgetIds: String,
    pillHidden: Boolean,
    shadeOpen: Boolean,
    widgetsSuppressed: Boolean = false,
    tickHold: Boolean = false,
    emptyPillShown: Boolean = false
): Boolean = isLandscape && !shadeOpen && (
    emptyPillShown ||
        (tickHold && widgetsEnabled) ||
        (widgetsEnabled && !widgetsSuppressed && !pillHidden && IdleWidgets.parse(widgetIds).isNotEmpty())
    )

fun pillOnIsland(hasCards: Boolean, widgetPill: Boolean): Boolean = hasCards || widgetPill

/**
 * What the pill's incoming-card slide ([IslandInteractionState.pillEnter]) does when the shown card or widget changes
 * or the content layer returns
 */
enum class PillEnterStep {
    Start,

    SettleStale,

    Nothing
}

const val PILL_ENTER_REST_PX = 0.5f

fun pillEnterStep(pendingEnterSign: Int, enterOffsetPx: Float): PillEnterStep = when {
    pendingEnterSign != 0 -> PillEnterStep.Start
    abs(enterOffsetPx) > PILL_ENTER_REST_PX -> PillEnterStep.SettleStale
    else -> PillEnterStep.Nothing
}

/**
 * Landscape: how the collapsed cutout leaves when its last card goes. It slides away and fades, then the idle circle
 * opens at its spot
 */
enum class PillExitDirection { Up, Left, Right }

const val PILL_EXIT_EDGE_FRACTION = 0.25f

/** Pill slides no further than this (fading on the way), so a low pill does not fly across the screen. */
const val PILL_EXIT_MAX_TRAVEL_DP = 120f

/** Spare room past the screen edge so the pill is fully clear. */
const val PILL_EXIT_MARGIN_DP = 8f

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

fun pillExitOffsetDp(direction: PillExitDirection, travelDp: Float, progress: Float): PillExitOffset {
    val d = travelDp * progress.coerceIn(0f, 1f)
    return when (direction) {
        PillExitDirection.Up -> PillExitOffset(0f, -d)
        PillExitDirection.Left -> PillExitOffset(-d, 0f)
        PillExitDirection.Right -> PillExitOffset(d, 0f)
    }
}

fun pillExitAlpha(progress: Float): Float = 1f - smoothstep((progress - 0.3f) / 0.7f)
