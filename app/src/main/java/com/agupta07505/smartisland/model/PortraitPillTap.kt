/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Portrait has no separate idle pill. */
const val PILL_CENTER_ZONE_SHARE = 0.34f

/** True when a touch at ([xPx], [yPx]) in a shell [shellWidthPx] wide lands in the glyph-free centre band of the pill. */
fun pillCenterTapHit(
    xPx: Float,
    yPx: Float,
    shellWidthPx: Float,
    pillWidthPx: Float,
    pillHeightPx: Float
): Boolean {
    if (shellWidthPx <= 0f || pillWidthPx <= 0f || pillHeightPx <= 0f) return false
    val half = pillWidthPx * PILL_CENTER_ZONE_SHARE / 2f
    val centre = shellWidthPx / 2f
    return yPx in 0f..pillHeightPx && xPx in (centre - half)..(centre + half)
}

/** The camera hole in dp: centre X from the screen centre, top edge from the screen top. */
data class CutoutArea(val centreXDp: Float, val topDp: Float, val widthDp: Float, val heightDp: Float)

/** Smallest touch target (dp) over the camera cutout; a real hole is often smaller than a finger. */
const val CUTOUT_TARGET_MIN_WIDTH_DP = 40f
const val CUTOUT_TARGET_MIN_HEIGHT_DP = 28f

/**
 * The camera cutout grown to at least the minimum touch target around its own centre (the top never goes above the
 * screen).
 */
fun cutoutTouchTarget(area: CutoutArea): CutoutArea {
    val w = maxOf(area.widthDp, CUTOUT_TARGET_MIN_WIDTH_DP)
    val h = maxOf(area.heightDp, CUTOUT_TARGET_MIN_HEIGHT_DP)
    val centreY = area.topDp + area.heightDp / 2f
    return CutoutArea(area.centreXDp, (centreY - h / 2f).coerceAtLeast(0f), w, h)
}

/** How far (dp) the pill's centre band must reach into the camera cutout, on both axes, to count as overlapping it. */
const val CUTOUT_OVERLAP_MIN_DP = 1f

/**
 * True when the pill's centre band overlaps the camera cutout by at least [minOverlapDp] on both axes (screen-space
 * dp). A null cutout never overlaps.
 */
fun centreBandOverlapsCutout(
    bandCentreXDp: Float,
    bandWidthDp: Float,
    bandTopDp: Float,
    bandHeightDp: Float,
    cutoutCentreXDp: Float?,
    cutoutWidthDp: Float,
    cutoutTopDp: Float,
    cutoutHeightDp: Float,
    minOverlapDp: Float = CUTOUT_OVERLAP_MIN_DP
): Boolean {
    if (cutoutCentreXDp == null || bandWidthDp <= 0f || bandHeightDp <= 0f || cutoutWidthDp <= 0f || cutoutHeightDp <= 0f) return false
    val horizontal = minOf(bandCentreXDp + bandWidthDp / 2f, cutoutCentreXDp + cutoutWidthDp / 2f) -
        maxOf(bandCentreXDp - bandWidthDp / 2f, cutoutCentreXDp - cutoutWidthDp / 2f)
    val vertical = minOf(bandTopDp + bandHeightDp, cutoutTopDp + cutoutHeightDp) - maxOf(bandTopDp, cutoutTopDp)
    return horizontal >= minOverlapDp && vertical >= minOverlapDp
}

/** Collapsed pill: does a long press open the Open Apps shortcut (the quick-launch grid)? */
fun longPressOpensApps(
    isLandscape: Boolean,
    notchMode: Boolean,
    shortcutsEnabled: Boolean,
    centreOverlapsCutout: Boolean,
    touchInCentreBand: Boolean,
    pillHasContent: Boolean = true
): Boolean {
    if (!shortcutsEnabled) return false
    if (isLandscape) return pillHasContent
    return if (centreOverlapsCutout || notchMode) !touchInCentreBand else true
}

/** Whether a tap in the pill's centre band is the idle control at all: portrait, with cards on the island. */
fun portraitCenterTapIsIdleControl(isLandscape: Boolean, notchMode: Boolean, hasCards: Boolean): Boolean =
    !isLandscape && hasCards

/**
 * Where the transparent close target of the expanded island sits, in dp from the top centre of the overlay window.
 * [offsetXDp] is null when the target follows the pill's own sideways offset.
 */
data class ExpandedCloseZone(
    val widthDp: Float,
    val heightDp: Float,
    val offsetXDp: Float?,
    val topDp: Float
)

/** Close target of the expanded island in portrait. */
fun expandedCloseZone(
    hasHardwareCutout: Boolean,
    cutoutWidthDp: Float,
    cutoutHeightDp: Float,
    cutoutXOffsetDp: Float,
    cutoutTopDp: Float,
    windowTopDp: Float,
    pillWidthDp: Float,
    pillHeightDp: Float
): ExpandedCloseZone {
    if (!hasHardwareCutout || cutoutWidthDp <= 0f || cutoutHeightDp <= 0f) {
        return ExpandedCloseZone(pillWidthDp * PILL_CENTER_ZONE_SHARE, pillHeightDp, null, 0f)
    }
    return ExpandedCloseZone(
        // The detected width is the hole plus padding, raised to the pill's minimum width (76 dp), so on its own it
        // reached over the glyphs beside the band.
        widthDp = minOf(cutoutWidthDp, pillWidthDp * PILL_CENTER_ZONE_SHARE),
        heightDp = cutoutHeightDp,
        offsetXDp = cutoutXOffsetDp,
        topDp = (cutoutTopDp - windowTopDp).coerceAtLeast(0f)
    )
}

