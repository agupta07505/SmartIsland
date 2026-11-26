/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.sqrt

data class EdgeCamera(
    val onLeft: Boolean,
    val centerYDp: Float,
    val screenWidthDp: Float,
    val screenHeightDp: Float,
    val centerFromEdgeDp: Float = 0f,
    val holeWidthDp: Float = 0f,
    val tuning: EdgePillTuning = EdgePillTuning(),
    val withContent: Boolean = false,
    val holeCentreYDp: Float = centerYDp,
    val holeLengthDp: Float = 0f,
    val cutoutReported: Boolean = false
) {
    val spot: EdgeCameraSpot
        get() = if (cutoutReported) EdgeSideCamera.spotOf(holeCentreYDp, screenHeightDp) else EdgeCameraSpot.Middle

    val centredYDp: Float
        get() = if (tuning.flatEdge) screenHeightDp / 2f else centerYDp

    val sideLayout: Boolean
        get() = EdgeSideCamera.isActive(tuning.flatEdge, spot)

    val pillHeightDp: Float
        get() {
            val base = if (withContent) tuning.contentHeightDp(screenHeightDp) else tuning.heightDp
            return if (sideLayout) EdgeSideCamera.fittedLengthDp(base, holeLengthDp,
                EdgeSideCamera.holeFromEndDp(spot, holeLengthDp, tuning.yOffsetDp)) else base
        }

    val pillWidthDp: Float
        get() = tuning.widthDp.coerceIn(EdgePillTuning.WIDTH.start, EdgePillTuning.WIDTH.endInclusive)

    val holeAbsXDp: Float
        get() = if (centerFromEdgeDp <= 0f) {
            if (onLeft) pillWidthDp / 2f else screenWidthDp - pillWidthDp / 2f
        } else if (onLeft) centerFromEdgeDp else screenWidthDp - centerFromEdgeDp
}

data class EdgeRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class EdgePillLayout(val pill: EdgeRect, val secondary: EdgeRect?, val camera: EdgeCamera)

data class EdgeValueAlong(val topDp: Float, val lengthDp: Float, val fromBottom: Boolean) {
    val centreDp: Float get() = topDp + lengthDp / 2f
}

data class EdgeGlyphSlots(
    val left: Float,
    val glyphTop: Float,
    val valueTop: Float,
    val width: Float,
    val height: Float,
    val valueLeft: Float = left,
    val valueWidth: Float = width
) {
    val centreX: Float get() = left + width / 2f
}

data class EdgeCardPlacement(
    val cardCentreOffsetDp: Float,
    val pillCentreOffsetDp: Float,
    val pillCentreYDp: Float,
    val onLeft: Boolean = true,
    val pillEdgeOffsetDp: Float = pillCentreOffsetDp,
    val cardEdgeOffsetDp: Float = cardCentreOffsetDp
)

object EdgePillLayoutMath {
    const val PILL_WIDTH_DP = 24f
    const val PILL_HEIGHT_DP = 72f
    const val MAX_PILL_WIDTH_DP = 64f

    fun resolveCamera(
        cameraXOffsetDp: Float?,
        cameraCenterYDp: Float?,
        rotationOnLeft: Boolean,
        screenWidthDp: Float,
        screenHeightDp: Float,
        cameraWidthDp: Float = 0f,
        tuning: EdgePillTuning = EdgePillTuning(),
        withContent: Boolean = false,
        cameraLengthDp: Float = 0f
    ): EdgeCamera {
        if (tuning.cameraManual) {
            val height = screenHeightDp.coerceAtLeast(0f)
            val holeY = (if (rotationOnLeft) tuning.cameraYDp else height - tuning.cameraYDp).coerceIn(0f, height)
            val classicY = holeY.coerceIn(PILL_HEIGHT_DP / 2f,
                (height - PILL_HEIGHT_DP / 2f).coerceAtLeast(PILL_HEIGHT_DP / 2f))
            return EdgeCamera(
                rotationOnLeft, classicY, screenWidthDp, screenHeightDp, 0f, tuning.cameraLengthDp, tuning, withContent,
                holeY, tuning.cameraLengthDp, true
            )
        }
        val onLeft = if (cameraXOffsetDp != null) cameraXOffsetDp < 0f else rotationOnLeft
        val centerY = (cameraCenterYDp ?: (screenHeightDp / 2f))
            .coerceIn(PILL_HEIGHT_DP / 2f, (screenHeightDp - PILL_HEIGHT_DP / 2f).coerceAtLeast(PILL_HEIGHT_DP / 2f))
        val fromEdge = if (cameraXOffsetDp != null) (screenWidthDp / 2f - abs(cameraXOffsetDp))
            .coerceAtLeast(0f) else 0f
        val reported = cameraXOffsetDp != null && cameraCenterYDp != null
        val holeY = (cameraCenterYDp ?: (screenHeightDp / 2f)).coerceIn(0f, screenHeightDp.coerceAtLeast(0f))
        val holeLength = if (cameraLengthDp > 0f) cameraLengthDp else cameraWidthDp
        return EdgeCamera(onLeft, centerY, screenWidthDp, screenHeightDp, fromEdge, cameraWidthDp, tuning, withContent,
            holeY, holeLength, reported)
    }

    /**
     * [layout] with its pill [extraDp] thicker while a pulse draws its progress line: the screen-edge side stays where
     * it is and the pill grows toward the screen's inside, where the line runs. The bubble keeps its place
     */
    fun thickened(layout: EdgePillLayout, extraDp: Float): EdgePillLayout {
        if (extraDp <= 0f) return layout
        val pill = layout.pill
        val left = if (layout.camera.onLeft) pill.left else pill.left - extraDp
        return layout.copy(pill = EdgeRect(left, pill.top, pill.width + extraDp, pill.height))
    }

    fun compute(
        camera: EdgeCamera,
        pillWidthDp: Float = camera.pillWidthDp,
        pillHeightDp: Float = camera.pillHeightDp,
        bubbleSizeDp: Float = EdgePillTuning.bubbleSizeFor(pillWidthDp),
        hasSecondary: Boolean,
        topDp: Float? = null
    ): EdgePillLayout {
        val t = camera.tuning
        val left = if (camera.onLeft) t.xOffsetDp else camera.screenWidthDp - pillWidthDp - t.xOffsetDp
        val top = topDp ?: pillTop(camera, pillHeightDp)
        val pill = EdgeRect(left, top, pillWidthDp, pillHeightDp)
        val secondary = if (hasSecondary && camera.sideLayout) {
            // Side camera: the bubble sits past the pill's far end on the pill's centre line
            EdgeRect(
                EdgeSideCamera.bubbleLeftDp(left, pillWidthDp, bubbleSizeDp, camera.screenWidthDp),
                EdgeSideCamera.bubbleTopDp(camera.spot, top, pillHeightDp, bubbleSizeDp, t.bubbleGapDp,
                    camera.screenHeightDp),
                bubbleSizeDp,
                bubbleSizeDp
            )
        } else if (hasSecondary) {
            val below = EdgePillRules.secondaryBelow(holeCentreYDp(camera, top, pillHeightDp), camera.screenHeightDp,
                t.bubblePos)
            EdgeRect(
                EdgeSideCamera.bubbleLeftDp(left, pillWidthDp, bubbleSizeDp, camera.screenWidthDp),
                EdgePillRules.bubbleTopBesidePillDp(top, pillHeightDp, bubbleSizeDp, t.bubbleGapDp,
                    camera.screenHeightDp, below),
                bubbleSizeDp,
                bubbleSizeDp
            )
        } else null
        return EdgePillLayout(pill, secondary, camera)
    }

    fun pillTop(camera: EdgeCamera, pillHeightDp: Float = camera.pillHeightDp): Float =
        if (camera.sideLayout) {
            EdgeSideCamera.desiredTopDp(camera.spot, camera.screenHeightDp, camera.holeCentreYDp, camera.holeLengthDp,
                pillHeightDp, camera.tuning.yOffsetDp)
        } else {
            EdgePillRules.pillTopDp(camera.centredYDp + camera.tuning.yOffsetDp, pillHeightDp)
                .coerceIn(0f, (camera.screenHeightDp - pillHeightDp).coerceAtLeast(0f))
        }

    private const val HOLE_ON_MIDDLE_DP = 0.5f

    const val GLYPH_SLOT_WIDTH_DP = 24f
    const val GLYPH_SLOT_HEIGHT_DP = 22f

    const val VALUE_SIDE_PAD_DP = 2f

    const val VALUE_MIN_WIDTH_DP = 16f

    const val VALUE_TEXT_HEIGHT_DP = 13f

    fun valueWidthDp(pillWidthDp: Float, pillHeightDp: Float, cornerRadiusDp: Float, slotTopDp: Float,
            slotHeightDp: Float): Float {
        val across = pillWidthDp - 2f * VALUE_SIDE_PAD_DP
        val radius = cornerRadiusDp.coerceIn(0f, pillWidthDp / 2f)
        val fromEnd = minOf(slotTopDp, pillHeightDp - slotTopDp - slotHeightDp).coerceAtLeast(0f) +
            ((slotHeightDp - VALUE_TEXT_HEIGHT_DP) / 2f).coerceAtLeast(0f)
        val chord = if (radius <= 0f || fromEnd >= radius) {
            across
        } else {
            val dy = radius - fromEnd
            2f * sqrt((radius * radius - dy * dy).coerceAtLeast(0f)) - 2f * VALUE_SIDE_PAD_DP
        }
        return minOf(across, chord).coerceAtLeast(VALUE_MIN_WIDTH_DP)
            .coerceAtMost(pillWidthDp.coerceAtLeast(VALUE_MIN_WIDTH_DP))
    }

    private fun slotsFor(layout: EdgePillLayout, glyphTop: Float, valueTop: Float): EdgeGlyphSlots {
        val w = GLYPH_SLOT_WIDTH_DP
        val h = GLYPH_SLOT_HEIGHT_DP
        val pill = layout.pill
        val valueW = valueWidthDp(pill.width, pill.height, layout.camera.tuning.cornerRadiusDp, valueTop, h)
        return EdgeGlyphSlots((pill.width - w) / 2f, glyphTop, valueTop, w, h, (pill.width - valueW) / 2f, valueW)
    }

    fun glyphSlots(layout: EdgePillLayout): EdgeGlyphSlots {
        val h = GLYPH_SLOT_HEIGHT_DP
        val pill = layout.pill
        sideZone(layout)?.let { return slotsFor(layout, it.glyphTopDp, it.valueTopDp) }
        val inset = (layout.camera.tuning.cornerRadiusDp * 0.35f).coerceIn(4f, 12f)
        val glyphTop = inset.coerceAtMost((pill.height - h).coerceAtLeast(0f))
        val valueTop = (pill.height - inset - h).coerceAtLeast(glyphTop + h)
        val hole = holeSpanInPill(layout)
            ?: return slotsFor(layout, glyphTop, valueTop)
        return slotsFor(
            layout,
            clearOfHole(glyphTop, h, hole, pill.height, EdgePulseLength.GLYPH_HOLE_GAP_DP, above = true),
            clearOfHole(valueTop, h, hole, pill.height, EdgePulseLength.VALUE_HOLE_GAP_DP, above = false)
        )
    }

    /**
     * The hole's span from the pill's top, for a centred layout whose hole is off the pill's middle (the flat look,
     * or a pill offset from the camera); null when no hole is reported or it is on the middle, where the glyphs at the
     * ends are clear of it and keep their places
     */
    private fun holeSpanInPill(layout: EdgePillLayout): ClosedFloatingPointRange<Float>? {
        val camera = layout.camera
        if (!camera.cutoutReported || camera.holeLengthDp <= 0f) return null
        val pill = layout.pill
        val centre = holeCentreYDp(layout) - pill.top
        if (abs(centre - pill.height / 2f) <= HOLE_ON_MIDDLE_DP) return null
        return (centre - camera.holeLengthDp / 2f)..(centre + camera.holeLengthDp / 2f)
    }

    /**
     * A slot's top that keeps it off the hole: where it is when it does not touch the hole's span and its gap, else on
     * the hole's [above] side (the glyph's) or below it (the value's), turned to the other side when that side has no
     * room in the pill
     */
    private fun clearOfHole(top: Float, slotH: Float, hole: ClosedFloatingPointRange<Float>, pillHeight: Float,
            gapDp: Float, above: Boolean): Float {
        val aboveTop = hole.start - gapDp - slotH
        val belowTop = hole.endInclusive + gapDp
        if (top + slotH <= hole.start - gapDp || top >= belowTop) return top
        val fitsAbove = aboveTop >= 0f
        val fitsBelow = belowTop + slotH <= pillHeight
        return when {
            above && fitsAbove -> aboveTop
            !above && fitsBelow -> belowTop
            fitsAbove -> aboveTop
            fitsBelow -> belowTop
            else -> top
        }
    }

    /**
     * Room for the settled value turned along the strip, as the pulse's value is: from the far end (12 dp in) back to
     * the hole's edge with a camera in the middle (the hole's own place, not the pill's middle, which differs in the flat look or
     * with an offset pill), or to the glyph's end with a camera toward 1 end. The text
     * keeps to the far end, so the pulse's value crossfades onto it. Never shorter than the old slot
     */
    fun valueAlong(layout: EdgePillLayout): EdgeValueAlong {
        val pill = layout.pill
        val camera = layout.camera
        val slotH = GLYPH_SLOT_HEIGHT_DP
        val zone = sideZone(layout)
        if (zone == null) {
            val holeY = holeCentreYDp(layout) - pill.top
            val top = holeY + camera.holeLengthDp.coerceAtLeast(0f) / 2f + EdgePulseLength.VALUE_HOLE_GAP_DP
            val length = (pill.height - SLOT_END_PADDING_DP - top).coerceAtLeast(slotH)
            return EdgeValueAlong((pill.height - SLOT_END_PADDING_DP - length).coerceAtLeast(0f), length, true)
        }
        return if (zone.spot == EdgeCameraSpot.Lower) {
            val bottom = zone.glyphTopDp - SLOT_PAIR_GAP_DP
            val length = (bottom - SLOT_END_PADDING_DP).coerceAtLeast(slotH)
            EdgeValueAlong(SLOT_END_PADDING_DP, length, false)
        } else {
            val top = zone.glyphTopDp + slotH + SLOT_PAIR_GAP_DP
            val length = (pill.height - SLOT_END_PADDING_DP - top).coerceAtLeast(slotH)
            EdgeValueAlong(pill.height - SLOT_END_PADDING_DP - length, length, true)
        }
    }

    /**
     * The hole's centre from the screen's top, the one place the pulse, the hidden target and the touch geometry read:
     * the reported hole's own centre, also when the pill is not on it (the notch look sits at the screen's middle, and
     * the pill's offset moves it off the hole); the pill's middle when no hole is known
     */
    fun holeCentreYDp(layout: EdgePillLayout): Float =
        holeCentreYDp(layout.camera, layout.pill.top, layout.pill.height)

    fun holeCentreYDp(camera: EdgeCamera, pillTopDp: Float, pillHeightDp: Float): Float =
        if (camera.sideLayout || (camera.cutoutReported && camera.holeLengthDp > 0f)) camera.holeCentreYDp
        else pillTopDp + pillHeightDp / 2f

    fun sideZone(layout: EdgePillLayout): EdgeSideZone? {
        val camera = layout.camera
        if (!camera.sideLayout) return null
        return EdgeSideCamera.zone(camera.spot, layout.pill.height, camera.holeCentreYDp - layout.pill.top,
            camera.holeLengthDp)
    }

    fun followLimitDp(layout: EdgePillLayout): Float? {
        val zone = sideZone(layout) ?: return null
        val camera = layout.camera
        return EdgeSideCamera.followLimitDp(camera.spot, layout.pill.height, camera.holeCentreYDp - layout.pill.top,
            camera.holeLengthDp, zone)
    }

    fun holeLocalX(layout: EdgePillLayout): Float = layout.camera.holeAbsXDp - layout.pill.left

    fun holeLocalY(layout: EdgePillLayout): Float = layout.camera.centerYDp - layout.pill.top

    fun cardPlacement(camera: EdgeCamera, cardWidthDp: Float,
            pillWidthDp: Float = camera.pillWidthDp): EdgeCardPlacement {
        val t = camera.tuning
        val half = camera.screenWidthDp / 2f
        val cardLeft = EdgePillRules.cardLeftDp(camera.screenWidthDp, pillWidthDp + t.xOffsetDp, cardWidthDp,
            camera.onLeft, t.cardGapDp)
        val pillLeft = if (camera.onLeft) t.xOffsetDp else camera.screenWidthDp - pillWidthDp - t.xOffsetDp
        return EdgeCardPlacement(
            cardCentreOffsetDp = cardLeft + cardWidthDp / 2f - half,
            pillCentreOffsetDp = pillLeft + pillWidthDp / 2f - half,
            pillCentreYDp = pillTop(camera) + camera.pillHeightDp / 2f,
            onLeft = camera.onLeft,
            pillEdgeOffsetDp = (if (camera.onLeft) pillLeft else pillLeft + pillWidthDp) - half,
            cardEdgeOffsetDp = (if (camera.onLeft) cardLeft else cardLeft + cardWidthDp) - half
        )
    }

    fun touchBounds(layout: EdgePillLayout, padDp: Float = 8f): EdgeRect {
        val rects = listOfNotNull(layout.pill, layout.secondary)
        val l = rects.minOf { it.left } - padDp
        val t = rects.minOf { it.top } - padDp
        val r = rects.maxOf { it.right } + padDp
        val b = rects.maxOf { it.bottom } + padDp
        val cl = l.coerceAtLeast(0f)
        val cr = r.coerceAtMost(layout.camera.screenWidthDp)
        return EdgeRect(cl, t.coerceAtLeast(0f), cr - cl,
            b.coerceAtMost(layout.camera.screenHeightDp) - t.coerceAtLeast(0f))
    }
}
