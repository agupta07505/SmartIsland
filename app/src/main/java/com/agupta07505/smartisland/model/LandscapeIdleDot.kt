/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation

/**
 * Idle dot (the small round idle cutout), as a standalone tap target. LANDSCAPE ONLY.
 *
 * Landscape: while a card is on the island the collapsed pill uses the landscape pill settings,
 * so the idle circle is not on screen. For fullscreen video and games the dot stays available as a
 * separate round target at the idle spot: one tap puts the collapsed cards away right away instead
 * of waiting for the idle timer ([com.agupta07505.smartisland.util.IslandTimings.STATIC_IDLE_TIMEOUT_MS]).
 *
 * Portrait has no separate dot: the glyph-free centre of the collapsed pill (and of the expanded island's
 * top, where the pill sits) is the same control, see [PortraitPillTap].
 *
 * All values are dp. [centerXDp] is measured from the screen centre. [topDp] is measured from the screen top (the
 * overlay window starts there in landscape).
 */
data class IdleDotGeometry(
    val centerXDp: Float,
    val topDp: Float,
    val sizeDp: Float,
    /** Pill opacity of the idle cutout (landscape circle opacity). */
    val opacity: Float = 1f,
    /** Width of the target: the circle's size (round). */
    val widthDp: Float = sizeDp
) {
    val bottomDp: Float get() = topDp + sizeDp
}

/**
 * Geometry of the idle dot for the stored [raw] settings (landscape; null in portrait). Uses the
 * same idle-cutout maths as [forOrientation] (camera cutout, corner, middle, fine tuning, reverse
 * landscape), so the dot sits exactly where the idle cutout would be.
 */
fun idleDot(raw: SmartIslandSettings, state: OrientationState): IdleDotGeometry? {
    // Portrait: no separate idle pill. The pill's own centre is the control ([PortraitPillTap]).
    if (!state.isLandscape) return null
    val idle = raw.forOrientation(state, hasContent = false)
    if (idle.enableNotchMode) return null
    return IdleDotGeometry(
        centerXDp = idle.xOffset,
        topDp = idle.yOffset.coerceAtLeast(0f),
        sizeDp = minOf(idle.width, idle.height),
        opacity = idle.opacity
    )
}

/**
 * Whether the separate dot is drawn: landscape only, with at least one card on the island (with no card the island's own
 * idle cutout is the dot), collapsed or expanded. It is a control only: it takes tap and long press, is always drawn
 * above the pill and the expanded card, and never animates (it is just on or off). Portrait never draws it.
 */
fun shouldShowIdleDot(dot: IdleDotGeometry?, hasCards: Boolean, isLandscape: Boolean = true): Boolean =
    dot != null && hasCards && isLandscape

/** What a tap on the idle dot (the idle circle) does. */
enum class IdleDotTap {
    /** Cards the idle timeout put away come back. */
    RevealStored,

    /** Cards on the island go away immediately, as the idle timeout would do. */
    PutAwayNow,

    /** Landscape, nothing on the island: show the empty pill (long press opens the app shortcuts). */
    ShowEmptyPill,

    /** The empty pill is shown: hide it. */
    HideEmptyPill,

    /** Nothing to reveal or put away; the tap is still consumed (the dot is always a target). */
    None
}

/** The same rule in both orientations (portrait: the pill's centre, landscape: the dot): cards on the island go away in the two-step close, stored cards come back. */
fun idleDotTapAction(
    hasCards: Boolean,
    hasIdleStoredCards: Boolean,
    emptyPillAvailable: Boolean = false,
    emptyPillShown: Boolean = false
): IdleDotTap = when {
    hasCards -> IdleDotTap.PutAwayNow
    hasIdleStoredCards -> IdleDotTap.RevealStored
    emptyPillShown -> IdleDotTap.HideEmptyPill
    emptyPillAvailable -> IdleDotTap.ShowEmptyPill
    else -> IdleDotTap.None
}
