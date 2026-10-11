/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.overlay

/**
 * Shared gesture thresholds and helpers used by the island overlay.
 */
object IslandGestureHandlers {

    /** Minimum drag distance (in px) to trigger a dismiss or floating-window action. */
    const val MIN_SWIPE_DISTANCE_PX = 48f

    /** Long-press duration before “Clear All” becomes available. */
    const val LONG_PRESS_DURATION_MS = 300L

    /** Scale factor applied during a quick switch between notifications. */
    const val SWITCH_SCALE_DOWN = 0.92f

    /** Spring damping used for the tactile bounce after switching. */
    const val SWITCH_BOUNCE_DAMPING = 0.65f

    /** Spring stiffness for the tactile bounce. */
    const val SWITCH_BOUNCE_STIFFNESS = 650f
}
