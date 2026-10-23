/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * The numbers behind how calm the swipe gestures feel, kept together for tuning. (The stretch, rubber-band and lean
 * numbers sit with their maths: [SwipeDownPull], `RUBBER_BAND_*` in ExpandedSwipe.kt.)
 */
object SwipeFeel {
    /**
     * Every spring that lets a swipe's temporary state go (arm swell, dead-zone shrink, lock cue) is critically
     * damped: it stops at rest, no overshoot.
     */
    const val RETURN_DAMPING_RATIO = 1f

    /** Compose's "medium" stiffness. */
    const val RETURN_STIFFNESS = 400f

    /**
     * Stiffness of the springs that bring a swipe's drag (shell, content, lean, pull, card) back to rest: a third of
     * Compose's StiffnessMedium (1500), still critically damped, so the release glides.
     */
    const val EASE_BACK_STIFFNESS = 550f

    /** How far (dp) the collapsed pill and the corner pill lean vertically with the finger (35 % of the drag, capped). */
    const val PILL_LEAN_Y_MAX_DP = 6f

    /** The small squash when a switch puts another card in the island: a shallow dip and a return that barely overshoots. */
    const val SWITCH_SQUASH_SCALE = 0.96f
    const val SWITCH_RETURN_DAMPING_RATIO = 0.8f
    const val SWITCH_RETURN_STIFFNESS = 600f
}
