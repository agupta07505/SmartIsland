/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Semi-expanded idle widget island: a tap on the widget glyph grows the collapsed cutout into a small island (about
 * twice the cutout), a tap outside closes it. Pure rules; drawing is ui/WidgetIsland.kt.
 */

/** Where on the collapsed pill a touch landed: glyph slot (left), glyph-free middle band, or value slot (right). */
enum class PillTapZone { Glyph, Middle, Value }

/**
 * Splits the pill into three zones; the middle band is the one [pillCenterTapHit] uses, so the camera hole keeps its
 * meaning. [xPx] is measured in the shell (the pill while collapsed).
 */
fun pillTapZone(xPx: Float, shellWidthPx: Float, pillWidthPx: Float): PillTapZone {
    val half = pillWidthPx * PILL_CENTER_ZONE_SHARE / 2f
    val centre = shellWidthPx / 2f
    return when {
        xPx < centre - half -> PillTapZone.Glyph
        xPx > centre + half -> PillTapZone.Value
        else -> PillTapZone.Middle
    }
}

/** What a tap on the collapsed pill does while an idle widget is on it and no card is. */
enum class WidgetTapAction {
    /** Tap on the glyph slot: grows the widget island (or closes it when open). */
    GlyphTap,

    /** Tap on the value slot: same as [GlyphTap], kept apart so it can get its own action. */
    ValueTap,

    /** The normal idle tap: show the stored cards the idle timeout put away. */
    RevealStoredCards,

    /** Nothing from the widgets: the pill's other taps decide. */
    None
}

/**
 * Tap rules of the collapsed pill while an idle widget is on it (both orientations): glyph and value slots both open
 * the widget island as two separate actions; the middle band shows the stored cards.
 */
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

/** How much bigger than the collapsed cutout the widget island is, on both axes. */
const val WIDGET_ISLAND_SCALE = 2f

/** The island is never smaller than this (dp): the small landscape circle doubled is too small for an icon and a value. */
const val WIDGET_ISLAND_MIN_WIDTH_DP = 124f
const val WIDGET_ISLAND_MIN_HEIGHT_DP = 64f

/** Corner radius (dp) of the open island; the cutout's own radius grows into it. */
const val WIDGET_ISLAND_CORNER_RADIUS_DP = 28f

/** Gap (dp) between the island and the screen edge. */
const val WIDGET_ISLAND_EDGE_MARGIN_DP = 8f

data class WidgetIslandSize(val widthDp: Float, val heightDp: Float)

/**
 * Size (dp) of the open island: [WIDGET_ISLAND_SCALE] times the cutout, at least the minimum, never wider than the
 * screen less the margins.
 */
fun widgetIslandSize(pillWidthDp: Float, pillHeightDp: Float, screenWidthDp: Float, contentHeightDp: Float = 0f): WidgetIslandSize {
    val maxWidth = (screenWidthDp - 2f * WIDGET_ISLAND_EDGE_MARGIN_DP).coerceAtLeast(0f)
    val width = maxOf(pillWidthDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_WIDTH_DP).coerceAtMost(maxWidth)
    val height = maxOf(pillHeightDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_HEIGHT_DP, contentHeightDp)
    return WidgetIslandSize(width, height)
}

/**
 * Open island's centre in dp from the screen centre: on the pill's own centre ([pillCentreDp]), moved inwards only as
 * far as needed to keep [WIDGET_ISLAND_EDGE_MARGIN_DP] to the screen edge (a landscape cutout sits in a corner).
 */
fun widgetIslandCentre(pillCentreDp: Float, islandWidthDp: Float, screenWidthDp: Float): Float {
    val limit = (screenWidthDp / 2f - WIDGET_ISLAND_EDGE_MARGIN_DP - islandWidthDp / 2f).coerceAtLeast(0f)
    return pillCentreDp.coerceIn(-limit, limit)
}

/**
 * Cards ranked at or above this ([IslandPriority.rank], lower = more important) close an open widget island: calls,
 * navigation, live and ongoing events.
 */
const val WIDGET_ISLAND_IMPORTANT_RANK = 20

/**
 * Whether a card of [mode] closes an open widget island on arrival: calls and live / ongoing events do; music,
 * downloads, notifications and system cards wait behind it.
 */
fun cardClosesWidgetIsland(mode: IslandMode): Boolean = IslandPriority.rank(mode) <= WIDGET_ISLAND_IMPORTANT_RANK

/**
 * Whether an open widget island must close by itself: it is expanded or an important card is on it; no widget is
 * available any more (a card waiting behind the island does not count); the widget has nothing to say (charger
 * unplugged, alarm rang); or the pill became the small round idle circle ([compact], landscape), which has no island.
 */
fun widgetIslandMustClose(
    expanded: Boolean,
    hasImportantCard: Boolean,
    widgetAvailable: Boolean,
    hasContent: Boolean = true,
    compact: Boolean = false
): Boolean = expanded || hasImportantCard || !widgetAvailable || !hasContent || compact

/**
 * True from the moment the widget island opens until it has shrunk back into the cutout: nothing else expands and the
 * cutout does not pulse.
 */
fun widgetIslandHoldsExpand(open: Boolean, shown: Boolean): Boolean = open || shown

/**
 * Rotation step a pill swipe moves an open island to: from [step] in [direction] (+1 next, -1 previous) over [count]
 * widgets, skipping those with [hasContent] false.
 */
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

/**
 * Auto-close timer of the widget island. [NEVER] (0, the default) keeps it open until a tap outside, a card or the
 * pill losing its widget closes it; otherwise [MIN_SECONDS]..[MAX_SECONDS].
 */
object WidgetIslandTimer {
    const val NEVER = 0
    const val MIN_SECONDS = 2
    const val MAX_SECONDS = 60
    const val DEFAULT_SECONDS = NEVER

    /** Stored value to a usable one: 0 or less is [NEVER], everything else stays within [MIN_SECONDS]..[MAX_SECONDS]. */
    fun normalize(seconds: Int): Int = if (seconds <= NEVER) NEVER else seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun closeDelayMs(seconds: Int): Long? = normalize(seconds).let { if (it == NEVER) null else it * 1000L }
}
