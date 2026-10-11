/*
 * Realistic Dynamic Island Fork
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srcpolygot.dynamicisland.domain.model.IslandMode

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

const val COMPACT_INDICATOR_GAP_DP = 8f
