/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape classic has no camera hole, so the pill's own place picks the pulse layout, the way the camera side does
 * in portrait. [pillCentreXDp] is the pill's centre from the screen centre, negative to the left. The bubble side
 * needs no rule here, landscape already puts the companion circle on the inner side of an off-centre pill
 * (`landscapeContentPillFor`) with the same [CENTRE_BAND_DP].
 */
object LandscapeClassicPulse {
    const val CENTRE_BAND_DP = 30f

    fun sideOf(pillCentreXDp: Float): CameraSide = when {
        pillCentreXDp < -CENTRE_BAND_DP -> CameraSide.Left
        pillCentreXDp > CENTRE_BAND_DP -> CameraSide.Right
        else -> CameraSide.Centre
    }

    /**
     * The layout side a pulse uses: chosen when it starts and held until it ends, so a pill that moves meanwhile (the
     * position slider, a fold) keeps its layout. [held] is the side the running pulse started with, null before one
     */
    fun heldSide(live: CameraSide, held: CameraSide?, pulsing: Boolean): CameraSide =
        if (pulsing) held ?: live else live

    fun anchorSide(widePulse: Boolean, classicSide: CameraSide, cameraSide: CameraSide): CameraSide =
        if (widePulse && classicSide != CameraSide.Centre) classicSide else cameraSide

    const val MAX_WIDTH_FRACTION = 0.5f

    fun capped(max: PulseWidth.MaxWidth, screenWidthDp: Float): PulseWidth.MaxWidth {
        val cap = screenWidthDp * MAX_WIDTH_FRACTION
        val width = max.widthDp
        if (width <= cap || width <= 0f) return max
        val scale = cap / width
        return PulseWidth.MaxWidth(max.leftDp * scale, max.rightDp * scale)
    }

    // Some events looked cramped. Keep the gap the settled pill has for that value's width.
    fun centreGapDp(settledWidthDp: Float, valueDp: Float): Float =
        (settledWidthDp - SLOT_START_PADDING_DP - SLOT_GLYPH_SIZE_DP - valueDp - SLOT_END_PADDING_DP)
            .coerceAtLeast(0f)
    const val OUTER_GLYPH_START_DP = SLOT_START_PADDING_DP

    /**
     * Room an off-centre pill may grow into, outer edge fixed. [pillLeftDp] is the settled pill's left edge from the
     * screen's left, [bubbleSpanDp] the companion circle that rides out with the growing edge
     */
    fun sideMaxWidthDp(
        side: CameraSide,
        screenWidthDp: Float,
        pillLeftDp: Float,
        settledWidthDp: Float,
        bubbleSpanDp: Float = 0f
    ): Float = PulseWidth.sideMaxWidthDp(side, screenWidthDp, pillLeftDp, settledWidthDp, bubbleSpanDp)

    fun centreShiftDp(side: CameraSide, settledWidthDp: Float, widthDp: Float): Float {
        val half = (widthDp - settledWidthDp) / 2f
        return when (side) {
            CameraSide.Left -> half
            CameraSide.Right -> -half
            CameraSide.Centre -> 0f
        }
    }
}
