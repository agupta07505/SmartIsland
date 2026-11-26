/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class PillTapZone { Glyph, Middle, Value }

fun pillTapZone(
    xPx: Float,
    shellWidthPx: Float,
    pillWidthPx: Float,
    bandShiftPx: Float = 0f,
    bandWidthPx: Float = pillWidthPx * PILL_CENTER_ZONE_SHARE,
    side: SlotSide = SlotSide.Both
): PillTapZone {
    val half = bandWidthPx / 2f
    val centre = shellWidthPx / 2f + bandShiftPx
    return when {
        xPx in (centre - half)..(centre + half) -> PillTapZone.Middle
        side != SlotSide.Both -> PillTapZone.Glyph
        xPx < centre - half -> PillTapZone.Glyph
        else -> PillTapZone.Value
    }
}

enum class WidgetTapAction {
    GlyphTap,

    ValueTap,

    RevealStoredCards,

    None
}

fun idleWidgetTapAction(
    widgetOnPill: Boolean,
    compact: Boolean,
    zone: PillTapZone,
    hasStoredCards: Boolean
): WidgetTapAction {
    if (!widgetOnPill || compact) return WidgetTapAction.None
    return when (zone) {
        PillTapZone.Middle -> if (hasStoredCards) WidgetTapAction.RevealStoredCards else WidgetTapAction.None
        PillTapZone.Glyph -> WidgetTapAction.GlyphTap
        PillTapZone.Value -> WidgetTapAction.ValueTap
    }
}

const val WIDGET_ISLAND_SCALE = 2f

const val WIDGET_ISLAND_MIN_WIDTH_DP = 124f
const val WIDGET_ISLAND_MIN_HEIGHT_DP = 64f

const val WIDGET_ISLAND_CORNER_RADIUS_DP = 28f

const val WIDGET_ISLAND_EDGE_MARGIN_DP = 8f

data class WidgetIslandSize(val widthDp: Float, val heightDp: Float)

fun widgetIslandSize(pillWidthDp: Float, pillHeightDp: Float, screenWidthDp: Float,
        contentHeightDp: Float = 0f): WidgetIslandSize {
    val maxWidth = (screenWidthDp - 2f * WIDGET_ISLAND_EDGE_MARGIN_DP).coerceAtLeast(0f)
    val width = maxOf(pillWidthDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_WIDTH_DP).coerceAtMost(maxWidth)
    val height = maxOf(pillHeightDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_HEIGHT_DP, contentHeightDp)
    return WidgetIslandSize(width, height)
}

fun widgetIslandCentre(pillCentreDp: Float, islandWidthDp: Float, screenWidthDp: Float): Float {
    val limit = (screenWidthDp / 2f - WIDGET_ISLAND_EDGE_MARGIN_DP - islandWidthDp / 2f).coerceAtLeast(0f)
    return pillCentreDp.coerceIn(-limit, limit)
}

const val WIDGET_ISLAND_IMPORTANT_RANK = 20

fun cardClosesWidgetIsland(mode: IslandMode): Boolean = IslandPriority.rank(mode) <= WIDGET_ISLAND_IMPORTANT_RANK

fun widgetIslandMustClose(
    expanded: Boolean,
    hasImportantCard: Boolean,
    widgetAvailable: Boolean,
    hasContent: Boolean = true,
    compact: Boolean = false
): Boolean = expanded || hasImportantCard || !widgetAvailable || !hasContent || compact

fun widgetIslandHoldsExpand(open: Boolean, shown: Boolean): Boolean = open || shown

fun nextStepWithContent(step: Int, direction: Int, count: Int, hasContent: (Int) -> Boolean): Int? {
    if (count < 2 || direction == 0) return null
    val dir = if (direction > 0) 1 else -1
    var target = step
    repeat(count - 1) {
        target += dir
        if (hasContent(Math.floorMod(target, count))) return target
    }
    return null
}

object WidgetIslandTimer {
    const val NEVER = 0
    const val MIN_SECONDS = 2
    const val MAX_SECONDS = 60
    const val DEFAULT_SECONDS = NEVER

    fun normalize(seconds: Int): Int = if (seconds <= NEVER) NEVER else seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun closeDelayMs(seconds: Int): Long? = normalize(seconds).let { if (it == NEVER) null else it * 1000L }
}
