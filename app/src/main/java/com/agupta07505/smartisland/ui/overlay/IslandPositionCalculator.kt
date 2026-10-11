/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Extracted from IslandOverlayView for better maintainability.
 */

package com.agupta07505.smartisland.ui.overlay

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandMode

/**
 * Calculates the expanded width of the island based on orientation and screen size.
 */
fun calculateExpandedWidth(
    isLandscape: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float
): Float {
    return if (isLandscape) {
        (screenHeightDp * 0.55f).coerceIn(280f, 420f)
    } else {
        (screenWidthDp * 0.92f).coerceIn(300f, 400f)
    }
}

/**
 * Returns a reasonable estimated height for the expanded card of a given mode.
 */
fun defaultEstimatedHeightForMode(mode: IslandMode): Dp {
    return when (mode) {
        IslandMode.Music -> 210.dp
        IslandMode.Notification -> 160.dp
        IslandMode.IncomingCall -> 140.dp
        IslandMode.Timer, IslandMode.Stopwatch -> 180.dp
        IslandMode.Battery -> 150.dp
        IslandMode.LiveActivity -> 170.dp
        IslandMode.Navigation -> 160.dp
        IslandMode.DownloadUpload -> 155.dp
        IslandMode.Hotspot -> 145.dp
        IslandMode.Bluetooth -> 150.dp
        IslandMode.Flashlight -> 130.dp
        IslandMode.ScreenRecording -> 140.dp
        IslandMode.Empty -> 135.dp
    }
}

/**
 * Calculates the vertical offset for the expanded island.
 */
fun calculateExpandedTopOffset(
    enableNotchMode: Boolean,
    hasCompanion: Boolean,
    statusBarHeightDp: Float,
    notchHeightDp: Float,
    circleSizeDp: Float,
    compactGapDp: Float
): Float {
    return if (enableNotchMode) {
        statusBarHeightDp * 0.15f
    } else if (hasCompanion) {
        (statusBarHeightDp + circleSizeDp + compactGapDp * 0.5f).coerceAtMost(48f)
    } else {
        (statusBarHeightDp * 0.35f).coerceIn(8f, 28f)
    }
}

/**
 * Calculates the horizontal offset for the secondary (companion) bubble when expanded.
 */
fun calculateSecondaryExpandedOffset(
    secondaryIsPill: Boolean,
    isCircleLeft: Boolean,
    isFullWidth: Boolean,
    expandedCompactX: Float,
    screenCenter: Float,
    miniPillWidth: Float,
    circleSize: Float,
    compactGap: Float
): Float {
    val secondaryWidth = if (secondaryIsPill) miniPillWidth else circleSize
    return if (isFullWidth) {
        if (isCircleLeft) {
            expandedCompactX - screenCenter - secondaryWidth - compactGap
        } else {
            expandedCompactX - screenCenter + miniPillWidth + compactGap
        }
    } else {
        0f
    }
}

const val COMPACT_INDICATOR_GAP_DP = 8f
