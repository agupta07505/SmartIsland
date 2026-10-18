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

    /**
     * Free space needed on each side of the camera hole by the music pill's right-hand content
     * (the waveform + the slot's end padding + a little air). The pill grows symmetrically to
     * provide it when a narrow pill or a wide hole leaves less than that.
     */
    const val MUSIC_WING_DP = 40f

    /** The widest the pill grows to for an alert's text; beyond this the text scrolls and fades. */
    const val MAX_ALERT_PILL_WIDTH_DP = 280f

    /** Room the end padding, the dot and the gap between them take on the right-hand side. */
    private const val ALERT_FIXED_WING_DP = 12f + 8f + 6f
    private const val ALERT_TEXT_DP_PER_CHAR = 6.2f
    private const val ALERT_MAX_CHARS = 42

    /**
     * Free space the active alert's right-hand pill content needs on each side of the camera hole, so
     * the pill can grow symmetrically around it (0 when the mode needs none).
     */
    fun contentWingDp(active: com.agupta07505.smartisland.model.IslandNotification?): Float = when {
        active == null -> 0f
        active.mode == com.agupta07505.smartisland.model.IslandMode.Music -> MUSIC_WING_DP
        active.mode == com.agupta07505.smartisland.model.IslandMode.Notification &&
            (active.title.isNotBlank() || active.text.isNotBlank()) -> {
            val (strong, soft) = alertPillLabel(active)
            val chars = (strong.length + (if (soft.isBlank()) 0 else 2 + soft.length)).coerceAtMost(ALERT_MAX_CHARS)
            ALERT_FIXED_WING_DP + chars * ALERT_TEXT_DP_PER_CHAR
        }
        else -> 0f
    }

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
        buddyOnPill: Boolean,
        contentWingDp: Float = 0f
    ): Float {
        val freeWing = (baseWidthDp - (holeWidthDp ?: 0f)) / 2f
        val buddyNeed = if (buddyOnPill) buddySizeDp(pillHeightDp) + 2f * BUDDY_MARGIN_DP else 0f
        val shortfall = max(0f, max(buddyNeed, contentWingDp) - freeWing)
        val width = baseWidthDp + 2f * shortfall
        // An alert's text can ask for a lot of room; the pill stops at a fixed width and the text scrolls.
        return if (contentWingDp > 0f) width.coerceAtMost(max(baseWidthDp, MAX_ALERT_PILL_WIDTH_DP)) else width
    }

    /** A buddy tucked behind the camera needs no wing, so the pill goes back to its base width. */
    fun effectiveWidthDp(
        settings: SmartIslandSettings,
        holeWidthDp: Float?,
        notificationCount: Int,
        tucked: Boolean = false,
        contentWingDp: Float = 0f
    ): Float = effectiveWidthDp(
        settings.width, settings.height, holeWidthDp, buddyOnPill(settings, notificationCount) && !tucked,
        contentWingDp
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
