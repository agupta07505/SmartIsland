/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class EdgeCameraSpot { Upper, Middle, Lower }

data class EdgeSideZone(
    val spot: EdgeCameraSpot,
    val bandTopDp: Float,
    val bandHeightDp: Float,
    val glyphTopDp: Float,
    val valueTopDp: Float
)

data class EdgeFadeSpan(val clearToDp: Float, val solidFromDp: Float, val fromBottom: Boolean)

object EdgeSideCamera {
    const val THRESHOLD_DP = SideCamera.THRESHOLD_DP

    const val GLYPH_HOLE_GAP_DP = SideCamera.GLYPH_HOLE_GAP_DP

    const val MIN_BAND_DP = EdgePillRules.MIDDLE_TARGET_HEIGHT_DP

    fun spotOf(cameraCentreYDp: Float?, screenHeightDp: Float): EdgeCameraSpot = when {
        cameraCentreYDp == null || screenHeightDp <= 0f -> EdgeCameraSpot.Middle
        cameraCentreYDp < screenHeightDp / 2f - THRESHOLD_DP -> EdgeCameraSpot.Upper
        cameraCentreYDp > screenHeightDp / 2f + THRESHOLD_DP -> EdgeCameraSpot.Lower
        else -> EdgeCameraSpot.Middle
    }

    fun isActive(flatEdge: Boolean, spot: EdgeCameraSpot): Boolean = !flatEdge && spot != EdgeCameraSpot.Middle

    fun bandLengthDp(holeLengthDp: Float): Float = SideCamera.bandWidthDp(holeLengthDp)

    fun nudgeRangeDp(holeLengthDp: Float): Float = SideCamera.nudgeRangeDp(holeLengthDp)

    fun holeFromEndDp(spot: EdgeCameraSpot, holeLengthDp: Float, yOffsetDp: Float): Float {
        val halfBand = bandLengthDp(holeLengthDp) / 2f
        return if (spot == EdgeCameraSpot.Lower) halfBand + yOffsetDp else halfBand - yOffsetDp
    }

    private fun glyphStartDp(holeFromEndDp: Float, holeLengthDp: Float, pillLengthDp: Float): Float {
        val hole = holeLengthDp.coerceAtLeast(0f)
        val band = bandLengthDp(hole)
        val bandStart = (holeFromEndDp - band / 2f).coerceAtLeast(0f)
        val bandEnd = (holeFromEndDp + band / 2f).coerceAtMost(pillLengthDp)
        if (bandEnd <= bandStart) return minOf(band, pillLengthDp) + SideCamera.CAP_PADDING_DP
        return minOf(bandEnd + SideCamera.CAP_PADDING_DP, holeFromEndDp + hole / 2f + GLYPH_HOLE_GAP_DP)
            .coerceAtLeast(SLOT_START_PADDING_DP)
    }

    fun minLengthDp(holeLengthDp: Float, holeFromEndDp: Float? = null): Float {
        val hole = holeLengthDp.coerceAtLeast(0f)
        val from = holeFromEndDp ?: (bandLengthDp(hole) / 2f)
        val glyphStart = glyphStartDp(from, hole, Float.MAX_VALUE)
        return glyphStart + EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP + SideCamera.PAIR_GAP_DP + EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP + SLOT_END_PADDING_DP
    }

    fun fittedLengthDp(requestedDp: Float, holeLengthDp: Float, holeFromEndDp: Float? = null): Float {
        val needed = minLengthDp(holeLengthDp, holeFromEndDp)
        val from = holeFromEndDp ?: return maxOf(requestedDp, needed)
        val holeOnPill = from - bandLengthDp(holeLengthDp) / 2f < requestedDp
        return if (holeOnPill) maxOf(requestedDp, needed) else requestedDp
    }

    fun desiredTopDp(
        spot: EdgeCameraSpot,
        screenHeightDp: Float,
        cameraCentreYDp: Float,
        holeLengthDp: Float,
        pillLengthDp: Float,
        yOffsetDp: Float
    ): Float {
        val halfBand = bandLengthDp(holeLengthDp) / 2f
        val nudge = yOffsetDp
        val raw = when (spot) {
            EdgeCameraSpot.Upper -> cameraCentreYDp - halfBand + nudge
            EdgeCameraSpot.Lower -> cameraCentreYDp + halfBand - pillLengthDp + nudge
            EdgeCameraSpot.Middle -> cameraCentreYDp + yOffsetDp - pillLengthDp / 2f
        }
        return raw.coerceIn(0f, (screenHeightDp - pillLengthDp).coerceAtLeast(0f))
    }

    fun zone(spot: EdgeCameraSpot, pillLengthDp: Float, holeCentreInPillDp: Float, holeLengthDp: Float): EdgeSideZone? {
        if (spot == EdgeCameraSpot.Middle || pillLengthDp <= 0f) return null
        val upper = spot == EdgeCameraSpot.Upper
        val hole = holeLengthDp.coerceAtLeast(0f)
        val slotH = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        val cam = if (upper) holeCentreInPillDp else pillLengthDp - holeCentreInPillDp
        val width = bandLengthDp(hole)
        var bandStart = (cam - width / 2f).coerceAtLeast(0f)
        var bandEnd = (cam + width / 2f).coerceAtMost(pillLengthDp)
        val holeInside = bandEnd > bandStart
        if (!holeInside) {
            bandStart = 0f
            bandEnd = minOf(width, pillLengthDp)
        }
        val glyphStart = glyphStartDp(cam, hole, pillLengthDp)
        val minEnd = minOf(bandStart + MIN_BAND_DP, pillLengthDp)
        val tapEnd = if (holeInside) maxOf(minEnd, minOf(bandEnd, glyphStart)).coerceAtLeast(bandStart) else bandEnd
        val valueStart = glyphStart + slotH + SideCamera.PAIR_GAP_DP
        val valueNear = maxOf(valueStart, pillLengthDp - SLOT_END_PADDING_DP - slotH)
        return if (upper) {
            EdgeSideZone(spot, bandStart, tapEnd - bandStart, glyphStart, valueNear)
        } else {
            EdgeSideZone(spot, pillLengthDp - tapEnd, tapEnd - bandStart, pillLengthDp - glyphStart - slotH,
                pillLengthDp - valueNear - slotH)
        }
    }

    fun fadeSpan(spot: EdgeCameraSpot, pillLengthDp: Float, holeCentreInPillDp: Float, holeLengthDp: Float,
            zone: EdgeSideZone): EdgeFadeSpan? {
        if (spot == EdgeCameraSpot.Middle || pillLengthDp <= 0f) return null
        val upper = spot == EdgeCameraSpot.Upper
        val cam = if (upper) holeCentreInPillDp else pillLengthDp - holeCentreInPillDp
        val glyph = if (upper) zone.glyphTopDp else pillLengthDp - zone.glyphTopDp - EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        // Solid from the nearest a glyph can slide to (followLimitDp), so a glyph following the finger is not faded
        val limit = followLimitDp(spot, pillLengthDp, holeCentreInPillDp, holeLengthDp, zone)
        val solid = (glyph - limit).coerceIn(0f, pillLengthDp)
        val clear = (cam + holeLengthDp.coerceAtLeast(0f) / 2f).coerceIn(0f, (solid - 1f).coerceAtLeast(0f))
        return EdgeFadeSpan(clear, solid, !upper)
    }

    fun followLimitDp(spot: EdgeCameraSpot, pillLengthDp: Float, holeCentreInPillDp: Float, holeLengthDp: Float,
            zone: EdgeSideZone): Float {
        if (spot == EdgeCameraSpot.Middle || pillLengthDp <= 0f) return 0f
        val upper = spot == EdgeCameraSpot.Upper
        val cam = if (upper) holeCentreInPillDp else pillLengthDp - holeCentreInPillDp
        val glyph = if (upper) zone.glyphTopDp else pillLengthDp - zone.glyphTopDp - EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
        val deadEnd = (cam + holeLengthDp.coerceAtLeast(0f) / 2f + GLYPH_HOLE_GAP_DP).coerceAtLeast(0f)
        return (glyph - deadEnd).coerceAtLeast(0f)
    }

    fun bubbleTopDp(spot: EdgeCameraSpot, pillTopDp: Float, pillLengthDp: Float, bubbleSizeDp: Float, gapDp: Float,
            screenHeightDp: Float): Float {
        return EdgePillRules.bubbleTopBesidePillDp(pillTopDp, pillLengthDp, bubbleSizeDp, gapDp, screenHeightDp,
            below = spot != EdgeCameraSpot.Lower)
    }

    fun bubbleLeftDp(pillLeftDp: Float, pillWidthDp: Float, bubbleSizeDp: Float, screenWidthDp: Float): Float =
        (pillLeftDp + (pillWidthDp - bubbleSizeDp) / 2f).coerceIn(0f, (screenWidthDp - bubbleSizeDp).coerceAtLeast(0f))
}
