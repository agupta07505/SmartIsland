/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.BubbleIntoCutout
import com.agupta07505.smartisland.model.CollapsedGroupPlacement
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.TorchCardSize
import com.agupta07505.smartisland.ui.expanded.BATTERY_EXPANDED_HEIGHT
import com.agupta07505.smartisland.ui.expanded.NAVIGATION_EXPANDED_HEIGHT
import com.agupta07505.smartisland.ui.expanded.RING_GLANCE_HEIGHT
import com.agupta07505.smartisland.ui.expanded.SPLIT_ISLAND_GAP
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleCloseSequence
import com.agupta07505.smartisland.util.SECONDARY_RETURN_BASE_MS

internal const val EXPANDED_WIDTH_RATIO = 0.95f
internal const val LANDSCAPE_EXPANDED_WIDTH_RATIO = 0.5f
internal const val LANDSCAPE_EXPANDED_MIN_WIDTH_DP = 340f
internal const val LANDSCAPE_EXPANDED_MAX_WIDTH_DP = 560f
internal const val LANDSCAPE_SPLIT_PICKER_WIDTH_RATIO = 0.30f
internal const val LANDSCAPE_SPLIT_PICKER_MIN_WIDTH_DP = 240f
internal const val LANDSCAPE_SPLIT_PICKER_MAX_WIDTH_DP = 300f
internal const val LANDSCAPE_SPLIT_TOTAL_WIDTH_RATIO = 0.62f
internal const val LANDSCAPE_EXPANDED_LIFT_DP = 14f

internal const val LAUNCHER_EXPANDED_LIFT_DP = 6f
internal const val LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP = 240f
internal const val LANDSCAPE_PICKER_SCREEN_MARGIN_DP = 16f

internal fun splitPickerLeftFraction(islandWidthDp: Float, screenWidthDp: Float): Float =
    if (islandWidthDp <= 0f) 0.5f
    else (1f - splitPickerWidthDp(screenWidthDp) / islandWidthDp).coerceIn(0.3f, 0.9f)

internal fun splitPickerWidthDp(screenWidthDp: Float): Float =
    (screenWidthDp * LANDSCAPE_SPLIT_PICKER_WIDTH_RATIO)
        .coerceIn(LANDSCAPE_SPLIT_PICKER_MIN_WIDTH_DP, LANDSCAPE_SPLIT_PICKER_MAX_WIDTH_DP)

internal fun splitCardMaxWidthDp(screenWidthDp: Float): Float {
    val picker = splitPickerWidthDp(screenWidthDp)
    val gap = SPLIT_ISLAND_GAP.value
    val share = screenWidthDp * LANDSCAPE_SPLIT_TOTAL_WIDTH_RATIO - gap - picker
    val fits = screenWidthDp - LANDSCAPE_PICKER_SCREEN_MARGIN_DP - gap - picker
    return share
        .coerceAtMost(fits)
        .coerceAtMost(LANDSCAPE_EXPANDED_MAX_WIDTH_DP)
        .coerceAtLeast(LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP)
}

internal fun collapsedTopShiftDp(isLandscape: Boolean, enableNotchMode: Boolean, yOffsetDp: Float): Float =
    if (isLandscape && !enableNotchMode) yOffsetDp.coerceAtLeast(0f) else 0f

internal fun landscapeStatusBarHeightDp(context: Context): Float {
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
    splitCardWidthDp: Float? = null
): Float {
    return if (isLandscape && pickerSideBySide) {
        // Card island (its natural width) + gap + picker island
        val card = (splitCardWidthDp ?: splitCardMaxWidthDp(screenWidthDp))
            .coerceIn(LANDSCAPE_SPLIT_CARD_MIN_WIDTH_DP, splitCardMaxWidthDp(screenWidthDp))
        (card + SPLIT_ISLAND_GAP.value + splitPickerWidthDp(screenWidthDp))
            .coerceAtMost((screenWidthDp - LANDSCAPE_PICKER_SCREEN_MARGIN_DP).coerceAtLeast(0f))
    } else if (isLandscape) {
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
    } else if (hasCompanion || !isLandscape) {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    } else if (isLandscape) {
        statusBarHeightDp
    } else {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    }
    // landscape: a little higher than the status bar line
    return if (isLandscape) (offset - LANDSCAPE_EXPANDED_LIFT_DP).coerceAtLeast(0f) else offset
}

/** Lone card in portrait: top edge just under the camera cutout, inside a window starting at [windowTopDp]. */
internal fun singleCardTopBelowCutoutDp(cutoutBottomDp: Float, windowTopDp: Float,
        gapDp: Float = SINGLE_CARD_CUTOUT_GAP_DP): Float =
    (cutoutBottomDp + gapDp - windowTopDp).coerceAtLeast(0f)

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
internal const val EXPANDED_SIDE_SWIPE_THRESHOLD_DP = 48f
internal const val PILL_SWIPE_THRESHOLD_DP = 16f
internal const val DRAG_MAX_OFFSET_DP = 100f
internal const val PILL_DEAD_SHRINK = 0.04f
internal const val EXPANDED_DEAD_SHRINK = 0.015f

internal const val LOCK_CUE_OUTLINE_DP = 2.5f
internal const val LOCK_CUE_OUTLINE_ALPHA = 0.55f
internal const val HOLD_GLOW_ALPHA = 0.5f
internal const val HOLD_GLOW_WIDTH_DP = 9f
internal const val HOLD_GLOW_IN_MS = 140
internal const val LOCK_CUE_IN_MS = 60
internal const val LOCK_CUE_OUT_MS = 100
internal const val COMPACT_INDICATOR_GAP_DP = 8f
internal const val SINGLE_CARD_CUTOUT_GAP_DP = 4f
internal const val HOLD_GESTURE_THRESHOLD_MS = 300L
internal const val SECONDARY_RETURN_MS = SECONDARY_RETURN_BASE_MS
internal val MERGE_IN_EASING = CubicBezierEasing(0.55f, 0.05f, 0.35f, 1f)
/** Secondary bubble's glide into the cutout (model/BubbleIntoCutout.kt). */
internal val GLIDE_EASING = CubicBezierEasing(
    BubbleIntoCutout.GLIDE_X1,
    BubbleIntoCutout.GLIDE_Y1,
    BubbleIntoCutout.GLIDE_X2,
    BubbleIntoCutout.GLIDE_Y2
)
internal val MERGE_OUT_EASING = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

internal fun islandSpringDamping(isLandscape: Boolean): Float = if (isLandscape) 0.86f else 0.8f
internal fun islandSpringStiffness(isLandscape: Boolean): Float = if (isLandscape) 280f else 400f

internal fun idleClosePlan(
    speed: AnimationSpeed,
    edge: Boolean
): IdleCloseSequence.Plan =
    IdleCloseSequence.plan(
        speed,
        if (edge) speed.userStiffness(islandSpringStiffness(true)) to islandSpringDamping(true) else null
    )

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 175.dp
        IslandMode.Notification -> 135.dp
        IslandMode.IncomingCall -> 115.dp
        IslandMode.CallEnded -> 132.dp
        IslandMode.Battery -> BATTERY_EXPANDED_HEIGHT
        IslandMode.LiveActivity -> 180.dp
        IslandMode.Navigation -> NAVIGATION_EXPANDED_HEIGHT
        IslandMode.DownloadUpload -> 146.dp
        IslandMode.Hotspot -> 125.dp
        // the ring glance cards (timer, stopwatch, Bluetooth) are 94 dp
        IslandMode.Bluetooth, IslandMode.Timer, IslandMode.Stopwatch -> RING_GLANCE_HEIGHT
        IslandMode.Flashlight -> TorchCardSize.ESTIMATE_HEIGHT_DP.dp
        IslandMode.ScreenRecording, IslandMode.AudioRecording -> 115.dp
        IslandMode.Empty, null -> 135.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    // Expanded: the next event's bubble becomes the collapsed cutout whatever the number of events, no bubble stays
    // beside the card
    else -> listOf(CompactNotificationShape.MiniPill)
}

internal data class NotchGroupGeometry(
    val mainLeft: Float,
    val earLeft: Float,
    val earOnLeft: Boolean,
    val groupStart: Float,
    val groupEnd: Float
)

internal fun notchGroupGeometry(
    screenWidth: Float,
    xOffset: Float,
    notchWidth: Float,
    earWidth: Float,
    gap: Float,
    edgeMargin: Float,
    hasEar: Boolean,
    preferLeft: Boolean
): NotchGroupGeometry {
    val mainLeft = screenWidth / 2f + xOffset - notchWidth / 2f
    val mainRight = mainLeft + notchWidth
    val need = gap + earWidth
    val leftFits = mainLeft - edgeMargin >= need
    val rightFits = screenWidth - edgeMargin - mainRight >= need
    // The ear's side is decided even with no ear shown
    val onLeft = if (preferLeft) leftFits || !rightFits else !rightFits && leftFits
    val earLeft = if (onLeft) mainLeft - gap - earWidth else mainRight + gap
    return NotchGroupGeometry(
        mainLeft = mainLeft,
        earLeft = earLeft,
        earOnLeft = onLeft,
        groupStart = if (hasEar && onLeft) earLeft else mainLeft,
        groupEnd = if (hasEar && !onLeft) earLeft + earWidth else mainRight
    )
}

internal data class CollapsedGroupSpan(val mainLeftPx: Float, val startPx: Float, val endPx: Float) {
    fun windowXPx(screenWidthPx: Float): Int = ((startPx + endPx) / 2f - screenWidthPx / 2f).toInt()
}

/** [notchXOffsetPx] is null outside notch mode; the notch then fixes the pill and the group takes its ear's side. */
internal fun collapsedGroupSpan(
    desiredMainLeftPx: Float,
    screenWidthPx: Float,
    edgePaddingPx: Float,
    mainWidthPx: Float,
    circleSizePx: Float,
    compactGapPx: Float,
    isSplitMode: Boolean,
    isCircleLeft: Boolean,
    notchXOffsetPx: Float?
): CollapsedGroupSpan {
    val notch = notchXOffsetPx?.let {
        notchGroupGeometry(
            screenWidthPx, it, mainWidthPx, circleSizePx, compactGapPx, edgePaddingPx, isSplitMode, isCircleLeft
        )
    }
    val mainLeftPx = CollapsedGroupPlacement.clampedMainLeftPx(
        desiredMainLeftPx, screenWidthPx, edgePaddingPx, mainWidthPx, circleSizePx, compactGapPx, isSplitMode,
        isCircleLeft
    )
    return CollapsedGroupSpan(
        mainLeftPx = notch?.mainLeft ?: mainLeftPx,
        startPx = notch?.groupStart ?: CollapsedGroupPlacement.groupStartPx(
            mainLeftPx, circleSizePx, compactGapPx, isSplitMode, isCircleLeft
        ),
        endPx = notch?.groupEnd ?: CollapsedGroupPlacement.groupEndPx(
            mainLeftPx, mainWidthPx, circleSizePx, compactGapPx, isSplitMode, isCircleLeft
        )
    )
}
