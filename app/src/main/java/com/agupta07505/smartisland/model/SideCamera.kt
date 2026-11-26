/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class CameraSide { Left, Centre, Right }

data class SideCameraZone(
    val side: CameraSide,
    val band: CameraBand,
    val plan: CollapsedSlotPlan,
    val bandEndFromCameraEdgeDp: Float
)

object SideCamera {
    const val THRESHOLD_DP = 60f

    const val TAP_BAND_MIN_DP = 40f

    const val CAP_PADDING_DP = 4f

    const val PAIR_GAP_DP = 14f

    const val NUDGE_MAX_DP = 12f

    const val GLYPH_HOLE_GAP_DP = 2f

    fun sideOf(holeCentreXDp: Float?): CameraSide = when {
        holeCentreXDp == null -> CameraSide.Centre
        holeCentreXDp < -THRESHOLD_DP -> CameraSide.Left
        holeCentreXDp > THRESHOLD_DP -> CameraSide.Right
        else -> CameraSide.Centre
    }

    fun circleLeft(side: CameraSide, userChoiceLeft: Boolean): Boolean = when (side) {
        CameraSide.Left -> false
        CameraSide.Right -> true
        CameraSide.Centre -> userChoiceLeft
    }

    fun anchoredShiftDp(side: CameraSide, widthChangeDp: Float): Float = when (side) {
        CameraSide.Left -> widthChangeDp / 2f
        CameraSide.Right -> -widthChangeDp / 2f
        CameraSide.Centre -> 0f
    }

    fun startShiftDp(side: CameraSide, shellWidthDp: Float, settledWidthDp: Float): Float {
        val extra = shellWidthDp - settledWidthDp
        return when (side) {
            CameraSide.Left -> 0f
            CameraSide.Right -> extra
            CameraSide.Centre -> extra.coerceAtLeast(0f) / 2f
        }
    }

    fun endShiftDp(side: CameraSide, shellWidthDp: Float, settledWidthDp: Float): Float {
        val extra = (shellWidthDp - settledWidthDp).coerceAtLeast(0f)
        return when (side) {
            CameraSide.Left -> extra
            CameraSide.Right -> 0f
            CameraSide.Centre -> extra / 2f
        }
    }

    fun bandShiftInShellDp(side: CameraSide, bandShiftDp: Float, shellWidthDp: Float, settledWidthDp: Float): Float =
        bandShiftDp - anchoredShiftDp(side, shellWidthDp - settledWidthDp)

    fun farEndShiftDp(extraDp: Float): Float = maxOf(0f, extraDp)

    fun deadZoneEndDp(holeCentreFromCameraEdgeDp: Float, holeWidthDp: Float): Float =
        (holeCentreFromCameraEdgeDp + holeWidthDp.coerceAtLeast(0f) / 2f + GLYPH_HOLE_GAP_DP).coerceAtLeast(0f)

    fun glyphStartFromCameraDp(side: CameraSide, plan: CollapsedSlotPlan, pillWidthDp: Float): Float = when (side) {
        CameraSide.Right -> pillWidthDp - plan.glyphStartDp - SLOT_GLYPH_SIZE_DP
        else -> plan.glyphStartDp
    }

    fun contentClipFromCameraDp(deadEndDp: Float, glyphStartFromCameraDp: Float): Float =
        minOf(deadEndDp, glyphStartFromCameraDp).coerceAtLeast(0f)

    fun slideTowardCameraLimitDp(deadEndDp: Float, glyphStartFromCameraDp: Float): Float =
        (glyphStartFromCameraDp - deadEndDp).coerceAtLeast(0f)

    /**
     * Where the fade into the camera ends: the nearest a glyph can be slid to, so a glyph following the finger is never
     * faded
     */
    fun fadeEndFromCameraDp(glyphStartFromCameraDp: Float, slideLimitDp: Float): Float =
        (glyphStartFromCameraDp - slideLimitDp.coerceAtLeast(0f)).coerceAtLeast(0f)

    fun bandWidthDp(holeWidthDp: Float): Float = maxOf(holeWidthDp.coerceAtLeast(0f), TAP_BAND_MIN_DP)

    fun nudgeRangeDp(holeWidthDp: Float): Float =
        ((bandWidthDp(holeWidthDp) - holeWidthDp.coerceAtLeast(0f)) / 2f).coerceIn(0f, NUDGE_MAX_DP)

    fun desiredLeftDp(
        side: CameraSide,
        screenWidthDp: Float,
        holeCentreXDp: Float,
        holeWidthDp: Float,
        pillWidthDp: Float,
        xOffsetDp: Float
    ): Float {
        val holeCentre = screenWidthDp / 2f + holeCentreXDp
        val halfBand = bandWidthDp(holeWidthDp) / 2f
        val nudge = xOffsetDp.coerceIn(-nudgeRangeDp(holeWidthDp), nudgeRangeDp(holeWidthDp))
        return when (side) {
            CameraSide.Left -> holeCentre - halfBand + nudge
            CameraSide.Right -> holeCentre + halfBand - pillWidthDp + nudge
            CameraSide.Centre -> screenWidthDp / 2f + xOffsetDp - pillWidthDp / 2f
        }
    }

    fun zone(side: CameraSide, pillWidthDp: Float, holeCentreInPillDp: Float, holeWidthDp: Float): SideCameraZone? {
        if (side == CameraSide.Centre || pillWidthDp <= 0f) return null
        val left = side == CameraSide.Left
        val cam = if (left) holeCentreInPillDp else pillWidthDp - holeCentreInPillDp
        val width = bandWidthDp(holeWidthDp)
        var bandStart = (cam - width / 2f).coerceAtLeast(0f)
        var bandEnd = (cam + width / 2f).coerceAtMost(pillWidthDp)
        val holeInsidePill = bandEnd > bandStart
        if (bandEnd <= bandStart) {
            bandStart = 0f
            bandEnd = minOf(width, pillWidthDp)
        }
        val afterBand = bandEnd + CAP_PADDING_DP
        val glyphStart = if (holeInsidePill) {
            minOf(afterBand, cam + holeWidthDp.coerceAtLeast(0f) / 2f + GLYPH_HOLE_GAP_DP)
                .coerceAtLeast(SLOT_START_PADDING_DP)
        } else afterBand
        val valueStart = glyphStart + SLOT_GLYPH_SIZE_DP + PAIR_GAP_DP
        val valueEnd = pillWidthDp - SLOT_END_PADDING_DP
        val valueMax = (valueEnd - valueStart).coerceAtLeast(0f)
        val bandWidth = bandEnd - bandStart
        val bandCentreCam = (bandStart + bandEnd) / 2f
        val bandCentreLeft = if (left) bandCentreCam else pillWidthDp - bandCentreCam
        val band = CameraBand(bandCentreLeft - pillWidthDp / 2f, bandWidth)
        val plan = if (left) {
            CollapsedSlotPlan(SlotSide.RightOfCamera, glyphStart, valueStart, valueEnd, valueMax)
        } else {
            CollapsedSlotPlan(
                SlotSide.LeftOfCamera,
                glyphStartDp = pillWidthDp - glyphStart - SLOT_GLYPH_SIZE_DP,
                valueStartDp = pillWidthDp - valueEnd,
                valueEndDp = pillWidthDp - valueStart,
                valueMaxWidthDp = valueMax
            )
        }
        return SideCameraZone(side, band, plan, bandEnd)
    }
}
