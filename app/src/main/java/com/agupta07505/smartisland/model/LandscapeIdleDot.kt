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

/** Idle dot (the small round idle cutout) as a standalone tap target. Landscape only. */
data class IdleDotGeometry(
    val centerXDp: Float,
    val topDp: Float,
    val sizeDp: Float,
    val opacity: Float = 1f,
    val widthDp: Float = sizeDp
) {
    val bottomDp: Float get() = topDp + sizeDp
}

/**
 * Idle dot geometry for the stored [raw] settings (null in portrait), using the same idle-cutout maths as
 * [forOrientation] so it sits where the idle cutout would.
 */
fun idleDot(raw: SmartIslandSettings, state: OrientationState): IdleDotGeometry? {
    // Portrait has no separate idle pill; the pill's own centre is the control ([PortraitPillTap]).
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
 * Whether the separate dot is drawn: landscape only, with at least one card on the island (otherwise the idle cutout
 * is the dot). Takes tap and long press; never animates.
 */
fun shouldShowIdleDot(dot: IdleDotGeometry?, hasCards: Boolean, isLandscape: Boolean = true): Boolean =
    dot != null && hasCards && isLandscape

enum class IdleDotTap {
    RevealStored,

    /** Cards on the island go away immediately, as the idle timeout would. */
    PutAwayNow,

    /** Landscape, nothing on the island: show the empty pill (long press opens the app shortcuts). */
    ShowEmptyPill,

    HideEmptyPill,

    /** Nothing to reveal or put away; the tap is still consumed (the dot is always a target). */
    None
}

/**
 * Same rule in both orientations (portrait: the pill's centre, landscape: the dot): cards go away in the two-step
 * close, stored cards come back.
 */
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
