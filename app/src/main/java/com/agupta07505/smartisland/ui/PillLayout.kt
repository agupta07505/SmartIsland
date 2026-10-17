/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandSettings
import kotlin.math.max

/**
 * One source of truth for how wide the collapsed pill is and when it hides while idle. The overlay
 * composable, the window params and the touch region must all agree on these numbers or touches drift
 * away from what is drawn, so none of them reads `settings.width` or `hideWhenIdle` directly.
 */
object PillLayout {
    /** Breathing room between the buddy and the pill edge or the camera hole, per side. */
    const val BUDDY_MARGIN_DP = 6f

    /** The buddy's size for a pill of [pillHeightDp]: 6dp smaller than the pill, clamped to 18..24dp. */
    fun buddySizeDp(pillHeightDp: Float): Float = (pillHeightDp - 6f).coerceIn(18f, 24f)

    /** True while the buddy is on and resting: enabled and nothing else is on the island. */
    fun buddyOnPill(settings: SmartIslandSettings, notificationCount: Int): Boolean =
        settings.buddyEnabled && notificationCount == 0

    /**
     * Width of the collapsed pill. It is [baseWidthDp] unless the buddy is resting and the free space
     * between the camera hole and each pill edge is smaller than the buddy needs; then both sides grow
     * by exactly the shortfall (symmetric, so the hole stays centred). [holeWidthDp] is the true hole
     * width, or null when the device has no hardware cutout.
     */
    fun effectiveWidthDp(
        baseWidthDp: Float,
        pillHeightDp: Float,
        holeWidthDp: Float?,
        buddyOnPill: Boolean
    ): Float {
        if (!buddyOnPill) return baseWidthDp
        val needed = buddySizeDp(pillHeightDp) + 2f * BUDDY_MARGIN_DP
        val freeWing = (baseWidthDp - (holeWidthDp ?: 0f)) / 2f
        val shortfall = max(0f, needed - freeWing)
        return baseWidthDp + 2f * shortfall
    }

    /** A buddy tucked behind the camera needs no wing, so the pill goes back to its base width. */
    fun effectiveWidthDp(
        settings: SmartIslandSettings,
        holeWidthDp: Float?,
        notificationCount: Int,
        tucked: Boolean = false
    ): Float = effectiveWidthDp(
        settings.width, settings.height, holeWidthDp, buddyOnPill(settings, notificationCount) && !tucked
    )

    /**
     * Whether the overlay window is hidden while nothing is happening. A resting buddy overrides
     * "Hide when idle" (the buddy tucks itself away after a while instead).
     */
    fun windowIdleHidden(settings: SmartIslandSettings, notificationCount: Int): Boolean =
        settings.hideWhenIdle && notificationCount == 0 && !settings.enableAppShortcuts && !settings.buddyEnabled

    /** Whether the pill itself shrinks to nothing while idle (the composable's view of the same rule). */
    fun pillIdleHiding(settings: SmartIslandSettings, notificationCount: Int): Boolean =
        settings.hideWhenIdle && notificationCount == 0 && !settings.buddyEnabled
}
