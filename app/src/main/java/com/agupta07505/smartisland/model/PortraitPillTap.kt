/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

/**
 * Share of the pill's width, around its centre, that counts as the centre tap when no camera band applies. Portrait has
 * no separate idle pill, so the pill's own centre is the control
 */
const val PILL_CENTER_ZONE_SHARE = 0.34f

data class CameraBand(val shiftDp: Float, val widthDp: Float)

const val CAMERA_BAND_MAX_SHARE = 0.5f

fun cameraBandFor(pillWidthDp: Float, cameraCentreInPillDp: Float?, cameraWidthDp: Float): CameraBand {
    val classic = CameraBand(0f, pillWidthDp * PILL_CENTER_ZONE_SHARE)
    if (pillWidthDp <= 0f || cameraCentreInPillDp == null || cameraWidthDp <= 0f) return classic
    val width = maxOf(cameraWidthDp, CUTOUT_TARGET_MIN_WIDTH_DP).coerceAtMost(pillWidthDp * CAMERA_BAND_MAX_SHARE)
    val half = width / 2f
    // Width <= half the pill, so the range below is never empty
    val centre = cameraCentreInPillDp.coerceIn(half, pillWidthDp - half)
    return CameraBand(centre - pillWidthDp / 2f, width)
}

/**
 * The band in a shell whose wide pulse has moved the pill's centre [centreShiftDp] right of the settled one
 * ([PulseWidth.Fit.centreShiftDp]): the hole stays where it is on the screen, so it lies that much nearer the shell's centre
 */
fun CameraBand.inMovedPill(centreShiftDp: Float): CameraBand = copy(shiftDp = shiftDp - centreShiftDp)

fun cameraCentreInPill(screenCentreDp: Float, cameraCentreXDp: Float?, pillCentreDp: Float,
        pillWidthDp: Float): Float? =
    cameraCentreXDp?.let { screenCentreDp + it - (pillCentreDp - pillWidthDp / 2f) }

const val CAMERA_CENTRED_TOLERANCE_DP = 4f

fun CameraBand.isOffCentre(): Boolean = abs(shiftDp) > CAMERA_CENTRED_TOLERANCE_DP

fun bandCentreFromScreenCentreDp(screenWidthDp: Float, pillLeftDp: Float, pillWidthDp: Float, band: CameraBand): Float =
    pillLeftDp + pillWidthDp / 2f + band.shiftDp - screenWidthDp / 2f

fun pillCenterTapHit(
    xPx: Float,
    yPx: Float,
    shellWidthPx: Float,
    pillWidthPx: Float,
    pillHeightPx: Float,
    bandShiftPx: Float = 0f,
    bandWidthPx: Float = pillWidthPx * PILL_CENTER_ZONE_SHARE
): Boolean {
    if (shellWidthPx <= 0f || pillWidthPx <= 0f || pillHeightPx <= 0f || bandWidthPx <= 0f) return false
    val half = bandWidthPx / 2f
    val centre = shellWidthPx / 2f + bandShiftPx
    return yPx in 0f..pillHeightPx && xPx in (centre - half)..(centre + half)
}

data class CutoutArea(val centreXDp: Float, val topDp: Float, val widthDp: Float, val heightDp: Float) {
    fun clampedTo(screenWidthDp: Float): CutoutArea {
        if (screenWidthDp <= 0f) return this
        val half = screenWidthDp / 2f
        return copy(centreXDp = centreXDp.coerceIn(-half, half), widthDp = widthDp.coerceAtMost(screenWidthDp))
    }
}

const val CUTOUT_TARGET_MIN_WIDTH_DP = 40f
const val CUTOUT_TARGET_MIN_HEIGHT_DP = 28f

fun cutoutTouchTarget(area: CutoutArea): CutoutArea {
    val w = maxOf(area.widthDp, CUTOUT_TARGET_MIN_WIDTH_DP)
    val h = maxOf(area.heightDp, CUTOUT_TARGET_MIN_HEIGHT_DP)
    val centreY = area.topDp + area.heightDp / 2f
    return CutoutArea(area.centreXDp, (centreY - h / 2f).coerceAtLeast(0f), w, h)
}

/** Reach (dp) the pill's centre band must have into the camera cutout. */
const val CUTOUT_OVERLAP_MIN_DP = 1f

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
    if (cutoutCentreXDp == null || bandWidthDp <= 0f || bandHeightDp <= 0f || cutoutWidthDp <= 0f
        || cutoutHeightDp <= 0f) return false
    val horizontal = minOf(bandCentreXDp + bandWidthDp / 2f, cutoutCentreXDp + cutoutWidthDp / 2f) -
        maxOf(bandCentreXDp - bandWidthDp / 2f, cutoutCentreXDp - cutoutWidthDp / 2f)
    val vertical = minOf(bandTopDp + bandHeightDp, cutoutTopDp + cutoutHeightDp) - maxOf(bandTopDp, cutoutTopDp)
    return horizontal >= minOverlapDp && vertical >= minOverlapDp
}

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

fun portraitCenterTapIsIdleControl(isLandscape: Boolean, notchMode: Boolean, hasCards: Boolean): Boolean =
    !isLandscape && hasCards

data class ExpandedCloseZone(
    val widthDp: Float,
    val heightDp: Float,
    val offsetXDp: Float?,
    val topDp: Float
)

fun expandedCloseZone(
    hasHardwareCutout: Boolean,
    cutoutWidthDp: Float,
    cutoutHeightDp: Float,
    cutoutXOffsetDp: Float,
    cutoutTopDp: Float,
    windowTopDp: Float,
    pillWidthDp: Float,
    pillHeightDp: Float,
    cameraBand: CameraBand? = null,
    pillCentreXDp: Float = 0f,
    screenWidthDp: Float? = null
): ExpandedCloseZone {
    if (!hasHardwareCutout || cutoutWidthDp <= 0f || cutoutHeightDp <= 0f) {
        return ExpandedCloseZone(pillWidthDp * PILL_CENTER_ZONE_SHARE, pillHeightDp, null, 0f)
    }
    val top = (cutoutTopDp - windowTopDp).coerceAtLeast(0f)
    if (cameraBand != null && cameraBand.isOffCentre()) {
        val holeOnPill = abs(cutoutXOffsetDp - pillCentreXDp) <= pillWidthDp / 2f
        val centre = if (holeOnPill) {
            pillCentreXDp + cameraBand.shiftDp
        } else {
            val limit = screenWidthDp?.let { (it / 2f - cameraBand.widthDp / 2f).coerceAtLeast(0f) }
            if (limit == null) cutoutXOffsetDp else cutoutXOffsetDp.coerceIn(-limit, limit)
        }
        return ExpandedCloseZone(
            widthDp = cameraBand.widthDp,
            heightDp = cutoutHeightDp,
            offsetXDp = centre,
            topDp = top
        )
    }
    return ExpandedCloseZone(
        widthDp = minOf(cutoutWidthDp, pillWidthDp * PILL_CENTER_ZONE_SHARE),
        heightDp = cutoutHeightDp,
        offsetXDp = cutoutXOffsetDp,
        topDp = top
    )
}
