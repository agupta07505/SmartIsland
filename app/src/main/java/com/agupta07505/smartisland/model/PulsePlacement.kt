/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

internal class PulsePeakPlacement(
    val side: CameraSide,
    val glyphFromCameraDp: Float,
    val notch: Boolean = false,
    val classic: Boolean = false
)

internal class PulseCamera(
    val side: CameraSide,
    val gapDp: Float,
    val centreInPillDp: Float?,
    val sideZone: SideCameraZone?
)

internal object PulsePlacement {

    fun camera(hole: CutoutArea?, screenCentreDp: Float, pillCentreDp: Float, pillWidthDp: Float): PulseCamera {
        val side = SideCamera.sideOf(hole?.centreXDp)
        val gap = hole?.widthDp ?: 0f
        val centreInPill = cameraCentreInPill(screenCentreDp, hole?.centreXDp, pillCentreDp, pillWidthDp)
        return PulseCamera(side, gap, centreInPill, centreInPill?.let { SideCamera.zone(side, pillWidthDp, it, gap) })
    }

    fun peak(
        notchMode: Boolean,
        isLandscape: Boolean,
        camera: PulseCamera,
        settledWidthDp: Float,
        classicSide: CameraSide = CameraSide.Centre
    ): PulsePeakPlacement {
        val sideZone = camera.sideZone
        return when {
            notchMode && isLandscape -> PulsePeakPlacement(CameraSide.Left, SLOT_START_PADDING_DP, notch = true)
            isLandscape && classicSide != CameraSide.Centre ->
                PulsePeakPlacement(classicSide, LandscapeClassicPulse.OUTER_GLYPH_START_DP, classic = true)
            isLandscape -> PulsePeakPlacement(CameraSide.Left, SLOT_START_PADDING_DP, classic = true)
            camera.side != CameraSide.Centre && sideZone != null -> PulsePeakPlacement(
                camera.side,
                SideCamera.glyphStartFromCameraDp(camera.side, sideZone.plan, settledWidthDp),
                notch = notchMode
            )
            else -> PulsePeakPlacement(CameraSide.Centre, 0f, notch = notchMode)
        }
    }
}
