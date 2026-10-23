/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandMode

internal const val EXPANDED_WIDTH_RATIO = 0.95f
internal const val LANDSCAPE_EXPANDED_WIDTH_RATIO = 0.5f
internal const val LANDSCAPE_EXPANDED_MIN_WIDTH_DP = 340f
internal const val LANDSCAPE_EXPANDED_MAX_WIDTH_DP = 560f
// Landscape with the sibling picker open: two islands (card left, picker right) with a gap.
internal const val LANDSCAPE_SPLIT_PICKER_WIDTH_RATIO = 0.30f
internal const val LANDSCAPE_SPLIT_PICKER_MIN_WIDTH_DP = 240f
internal const val LANDSCAPE_SPLIT_PICKER_MAX_WIDTH_DP = 300f
// The whole split group (card + gap + picker) is about this share of the screen width, which
// with the shorter picker makes the two islands cover roughly 30% of the screen.
internal const val LANDSCAPE_SPLIT_TOTAL_WIDTH_RATIO = 0.62f
// Landscape: the expanded island sits this much higher than the status bar line.
internal const val LANDSCAPE_EXPANDED_LIFT_DP = 14f

/** The app-shortcuts launcher window sits this much higher than other expanded cards (both orientations; never above the screen edge). */
internal const val LAUNCHER_EXPANDED_LIFT_DP = 6f
internal const val LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP = 240f
internal const val LANDSCAPE_PICKER_SCREEN_MARGIN_DP = 16f

internal fun splitPickerLeftFraction(islandWidthDp: Float, screenWidthDp: Float): Float =
    if (islandWidthDp <= 0f) 0.5f
    else (1f - splitPickerWidthDp(screenWidthDp) / islandWidthDp).coerceIn(0.3f, 0.9f)

/** Width of the picker island in the landscape split layout. */
internal fun splitPickerWidthDp(screenWidthDp: Float): Float =
    (screenWidthDp * LANDSCAPE_SPLIT_PICKER_WIDTH_RATIO)
        .coerceIn(LANDSCAPE_SPLIT_PICKER_MIN_WIDTH_DP, LANDSCAPE_SPLIT_PICKER_MAX_WIDTH_DP)

/**
 * Widest the card island may get in the split layout: what is left of the split group's share of the screen after the
 * gap and the picker, never so wide that card + gap + picker would not fit the screen minus its margin, and never
 * below the minimum card width.
 */
internal fun splitCardMaxWidthDp(screenWidthDp: Float): Float {
    val picker = splitPickerWidthDp(screenWidthDp)
    val gap = com.agupta07505.smartisland.ui.expanded.SPLIT_ISLAND_GAP.value
    val share = screenWidthDp * LANDSCAPE_SPLIT_TOTAL_WIDTH_RATIO - gap - picker
    val fits = screenWidthDp - LANDSCAPE_PICKER_SCREEN_MARGIN_DP - gap - picker
    return share
        .coerceAtMost(fits)
        .coerceAtMost(LANDSCAPE_EXPANDED_MAX_WIDTH_DP)
        .coerceAtLeast(LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP)
}

/**
 * Landscape: the overlay window always starts at the top of the screen, so the expanded island sits under the status
 * bar wherever the collapsed cutout was (a camera circle is mid-height on the side).
 */
internal fun collapsedTopShiftDp(isLandscape: Boolean, enableNotchMode: Boolean, yOffsetDp: Float): Float =
    if (isLandscape && !enableNotchMode) yOffsetDp.coerceAtLeast(0f) else 0f

/** Returns the landscape status bar height in dp, or 0f when the device doesn't define one. */
internal fun landscapeStatusBarHeightDp(context: android.content.Context): Float {
    val id = context.resources.getIdentifier("status_bar_height_landscape", "dimen", "android")
    if (id <= 0) return 0f
    val px = context.resources.getDimensionPixelSize(id)
    return if (px > 0) px / context.resources.displayMetrics.density else 0f
}

internal fun calculateExpandedWidth(
    isLandscape: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float,
    ratio: Float = EXPANDED_WIDTH_RATIO,
    pickerSideBySide: Boolean = false,
    // Measured natural width of the card island (split layout); null = not measured yet.
    splitCardWidthDp: Float? = null
): Float {
    return if (isLandscape && pickerSideBySide) {
        // Card island (its natural width) + gap + picker island, never wider than the screen
        // minus a margin.
        val card = (splitCardWidthDp ?: splitCardMaxWidthDp(screenWidthDp))
            .coerceIn(LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP, splitCardMaxWidthDp(screenWidthDp))
        (card + com.agupta07505.smartisland.ui.expanded.SPLIT_ISLAND_GAP.value + splitPickerWidthDp(screenWidthDp))
            .coerceAtMost((screenWidthDp - LANDSCAPE_PICKER_SCREEN_MARGIN_DP).coerceAtLeast(0f))
    } else if (isLandscape) {
        // Landscape: about half of the screen width, centred, so the card takes the same top strip a heads-up notification does.
        (screenWidthDp * LANDSCAPE_EXPANDED_WIDTH_RATIO)
            .coerceIn(LANDSCAPE_EXPANDED_MIN_WIDTH_DP, LANDSCAPE_EXPANDED_MAX_WIDTH_DP)
    } else {
        screenWidthDp * ratio
    }
}

internal fun calculateExpandedTopOffset(
    enableNotchMode: Boolean,
    hasCompanion: Boolean,
    statusBarHeightDp: Float,
    notchHeightDp: Float = 35f,
    circleSizeDp: Float = 34f,
    compactGapDp: Float = COMPACT_INDICATOR_GAP_DP,
    isLandscape: Boolean = false
): Float {
    val offset = if (enableNotchMode) {
        maxOf(statusBarHeightDp, notchHeightDp) + 8f
    } else if (hasCompanion) {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    } else {
        statusBarHeightDp
    }
    // Landscape: a little higher than the status bar line, never above the screen edge.
    return if (isLandscape) (offset - LANDSCAPE_EXPANDED_LIFT_DP).coerceAtLeast(0f) else offset
}

internal fun calculateSecondaryExpandedOffset(
    secondaryIsPill: Boolean,
    isCircleLeft: Boolean,
    isFullWidth: Boolean,
    expandedCompactX: Float,
    screenCenter: Float,
    miniPillWidth: Float,
    circleSize: Float,
    compactGap: Float
): Float {
    return if (secondaryIsPill) {
        val secCenter = expandedCompactX + miniPillWidth / 2f
        if (isFullWidth) secCenter - screenCenter else 0f
    } else if (isCircleLeft) {
        val secCenter = expandedCompactX - compactGap - circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else -(miniPillWidth + compactGap) / 2f
    } else {
        val secCenter = expandedCompactX + miniPillWidth + compactGap + circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else ((miniPillWidth + compactGap) / 2f)
    }
}
internal const val SWIPE_THRESHOLD_DP = 35f
/** Expanded island left / right swipe fire distance (dp). */
internal const val EXPANDED_SIDE_SWIPE_THRESHOLD_DP = 48f
internal const val PILL_SWIPE_THRESHOLD_DP = 16f
internal const val DRAG_MAX_OFFSET_DP = 100f
/** Dead-zone indicator (finger moved, lifting does nothing): how much the shell shrinks. The pill shows it more than the big card. */
internal const val PILL_DEAD_SHRINK = 0.04f
internal const val EXPANDED_DEAD_SHRINK = 0.015f

/** The swipe-lock cue. */
internal const val LOCK_CUE_OUTLINE_DP = 2.5f
internal const val LOCK_CUE_OUTLINE_ALPHA = 0.55f
internal const val LOCK_CUE_IN_MS = 60
internal const val LOCK_CUE_OUT_MS = 100
internal const val COMPACT_INDICATOR_GAP_DP = 8f
internal const val HOLD_GESTURE_THRESHOLD_MS = 300L
internal const val SECONDARY_RETURN_MS = com.agupta07505.smartisland.model.SECONDARY_RETURN_BASE_MS
internal val MERGE_IN_EASING = androidx.compose.animation.core.CubicBezierEasing(0.55f, 0.05f, 0.35f, 1f)
internal val MERGE_OUT_EASING = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

internal fun islandSpringDamping(isLandscape: Boolean): Float = if (isLandscape) 0.86f else 0.8f
internal fun islandSpringStiffness(isLandscape: Boolean): Float = if (isLandscape) 280f else 400f

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 175.dp
        IslandMode.Notification -> 135.dp
        IslandMode.IncomingCall, IslandMode.Battery -> 115.dp
        IslandMode.LiveActivity -> 180.dp
        IslandMode.Navigation -> com.agupta07505.smartisland.ui.expanded.NAVIGATION_EXPANDED_HEIGHT
        IslandMode.DownloadUpload, IslandMode.Hotspot -> 160.dp
        IslandMode.Bluetooth, IslandMode.Flashlight, IslandMode.ScreenRecording, IslandMode.AudioRecording,
        IslandMode.Timer, IslandMode.Stopwatch -> 115.dp
        IslandMode.Empty, null -> 135.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    notificationCount == 2 -> listOf(CompactNotificationShape.MiniPill)
    else -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
}
