/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class HoldDecision(
    val buzz: Boolean,
    val armAppsShortcut: Boolean,
    val runAction: SwipeAction?,
    val glow: Boolean = false,
    /** Keep tracking the touch after the hold so a swipe can replay a pulse. */
    val readSwipe: Boolean = false
)

object HoldGesture {
    /**
     * On the glyph or value, keep tracking the touch so a swipe can replay a pulse.
     * The apps shortcut can still arm during that hold.
     */
    fun atThreshold(
        collapsedAtStart: Boolean,
        dragging: Boolean,
        pillGestureTriggered: Boolean,
        opensApps: Boolean,
        action: SwipeAction,
        onGlyph: Boolean = false
    ): HoldDecision {
        if (!collapsedAtStart || dragging || pillGestureTriggered) return HoldDecision(true, false, null)
        if (onGlyph) return HoldDecision(true, opensApps, null, glow = true, readSwipe = true)
        if (opensApps) return HoldDecision(false, true, null)
        return HoldDecision(true, false, action.takeIf { it != SwipeAction.None })
    }

    /**
     * A plain tap on the collapsed pill while an event is opening the island by itself. The island is already open
     * when the finger lifts, so the tap's toggle would shut it again before it finished opening. It was aimed at the
     * pill that was there, not the card that arrived, so it does nothing.
     */
    fun tapOpenedUnderFinger(openAtDown: Boolean, openNow: Boolean): Boolean = !openAtDown && openNow

    fun swipeZone(zone: PillTapZone): Boolean = zone != PillTapZone.Middle

    /**
     * Whether a hold at [zone] can start the swipe. While an event pulses the whole pill takes it, the middle band
     * included: the pulse covers the camera band's place with its own glyph or value, so no part of it should be dead
     */
    fun holdStartsSwipe(zone: PillTapZone, pulseRunning: Boolean): Boolean = pulseRunning || swipeZone(zone)

    fun glyphSwiped(dx: Float, dy: Float, tapSlop: Float, horizontal: Boolean): Boolean =
        horizontal && kotlin.math.abs(dx) > tapSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)

    fun glyphLift(swiped: Boolean, appsArmed: Boolean, slopExceeded: Boolean): GlyphLift = when {
        swiped -> GlyphLift.Repulse
        opensAppsOnLift(appsArmed, slopExceeded) -> GlyphLift.OpenApps
        else -> GlyphLift.Nothing
    }

    fun cornerActionAtThreshold(dragging: Boolean, triggered: Boolean, action: SwipeAction): SwipeAction? =
        action.takeIf { !dragging && !triggered && it != SwipeAction.None }

    fun opensAppsOnLift(armed: Boolean, slopExceeded: Boolean): Boolean = armed && !slopExceeded
}

enum class GlyphLift { Repulse, OpenApps, Nothing }
