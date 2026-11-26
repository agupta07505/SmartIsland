/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object ShellStateGuard {
    /** Island must have been at rest this long (ms) with an inconsistent shell before the watchdog acts. */
    const val REST_MS = 600L

    /**
     * The window can resize before the content splits. Keep the single shell until both sides agree.
     */
    fun twoIslandsDrawn(overlayFlag: Boolean, contentSplitShown: Boolean): Boolean = overlayFlag && contentSplitShown

    fun waitMs(holdActive: Boolean, holdMs: Int): Long = REST_MS + if (holdActive) holdMs.toLong() else 0L

    fun stuckReason(
        expanded: Boolean,
        atRest: Boolean,
        overlayFlag: Boolean,
        splitWanted: Boolean,
        twoIslandsDrawn: Boolean,
        singleSurfaceAlpha: Float,
        radiusDp: Float
    ): String? {
        if (!expanded || !atRest) return null
        // A stale split flag can outlive the picker or picture that set it.
        if (overlayFlag && !splitWanted) return REASON_HOLD
        if (!twoIslandsDrawn && singleSurfaceAlpha < SURFACE_BACK) return REASON_NO_SURFACE
        if (!twoIslandsDrawn && radiusDp <= MIN_RADIUS_DP) return REASON_NO_RADIUS
        return null
    }

    fun canForceSingle(reason: String?): Boolean = reason == REASON_HOLD || reason == REASON_NO_SURFACE

    const val REASON_HOLD = "hold-without-island"
    const val REASON_NO_SURFACE = "single-without-surface"
    const val REASON_NO_RADIUS = "single-without-radius"

    private const val SURFACE_BACK = 0.98f
    private const val MIN_RADIUS_DP = 0.5f
}
