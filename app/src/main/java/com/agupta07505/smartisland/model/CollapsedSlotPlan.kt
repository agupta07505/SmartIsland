/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

const val SLOT_GLYPH_SIZE_DP = 22f
const val SLOT_START_PADDING_DP = 8f
const val SLOT_END_PADDING_DP = 12f
const val SLOT_PAIR_GAP_DP = 6f

enum class SlotSide { Both, RightOfCamera, LeftOfCamera }

data class CollapsedSlotPlan(
    val side: SlotSide,
    val glyphStartDp: Float,
    val valueStartDp: Float,
    val valueEndDp: Float,
    val valueMaxWidthDp: Float?
)

object CollapsedSlots {
    private const val MIN_ROOM_DP = SLOT_START_PADDING_DP + SLOT_GLYPH_SIZE_DP + SLOT_PAIR_GAP_DP

    fun plan(pillWidthDp: Float, cameraCentreDp: Float?, cameraGapDp: Float): CollapsedSlotPlan {
        val endEdge = pillWidthDp - SLOT_END_PADDING_DP
        if (cameraCentreDp == null || cameraGapDp <= 0f) {
            return CollapsedSlotPlan(SlotSide.Both, SLOT_START_PADDING_DP, SLOT_START_PADDING_DP, endEdge, null)
        }
        val band = cameraBandFor(pillWidthDp, cameraCentreDp, cameraGapDp)
        val zoneCentre = pillWidthDp / 2f + band.shiftDp
        val zoneWidth = band.widthDp
        val camLeft = zoneCentre - zoneWidth / 2f
        val camRight = zoneCentre + zoneWidth / 2f
        val leftRoom = camLeft
        val rightRoom = pillWidthDp - camRight
        val pairValueStart = { from: Float -> from + SLOT_GLYPH_SIZE_DP + SLOT_PAIR_GAP_DP }
        return when {
            leftRoom >= MIN_ROOM_DP && rightRoom >= MIN_ROOM_DP -> CollapsedSlotPlan(
                SlotSide.Both,
                SLOT_START_PADDING_DP,
                camRight,
                endEdge,
                (endEdge - camRight).coerceAtLeast(0f)
            )
            leftRoom < rightRoom -> {
                val glyph = maxOf(camRight + SLOT_START_PADDING_DP, SLOT_START_PADDING_DP)
                val start = pairValueStart(glyph)
                CollapsedSlotPlan(SlotSide.RightOfCamera, glyph, start, endEdge, (endEdge - start).coerceAtLeast(0f))
            }
            else -> {
                val glyph = SLOT_START_PADDING_DP
                val start = pairValueStart(glyph)
                CollapsedSlotPlan(SlotSide.LeftOfCamera, glyph, start, camLeft - SLOT_START_PADDING_DP,
                    (camLeft - SLOT_START_PADDING_DP - start).coerceAtLeast(0f))
            }
        }
    }

    fun zone(pillWidthDp: Float, cameraCentreDp: Float?, cameraGapDp: Float): CollapsedZone {
        val band = cameraBandFor(pillWidthDp, cameraCentreDp, cameraGapDp)
        return CollapsedZone(band, plan(pillWidthDp, cameraCentreDp, cameraGapDp))
    }
}

data class CollapsedZone(val band: CameraBand, val plan: CollapsedSlotPlan) {
    val side: SlotSide get() = plan.side
}

/**
 * Where the collapsed pill sits on the screen (px). Shared by the touch rectangle, the window layout and the collapsed
 * window params
 */
object CollapsedGroupPlacement {
    /**
     * Pill left edge kept on screen. The companion circle (secondary cutout) takes room on its side only while the
     * group is split, with no room left the lower bound wins
     */
    fun clampedMainLeftPx(
        desiredMainLeftPx: Float,
        screenWidthPx: Float,
        edgePaddingPx: Float,
        mainWidthPx: Float,
        circleSizePx: Float,
        compactGapPx: Float,
        isSplitMode: Boolean,
        isCircleLeft: Boolean
    ): Float {
        val groupWidthPx = mainWidthPx + if (isSplitMode) compactGapPx + circleSizePx else 0f
        val (minMainLeftPx, maxMainLeftPx) = when {
            !isSplitMode -> edgePaddingPx to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx)
            isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to
                (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
            else -> edgePaddingPx to (screenWidthPx - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
        }
        return desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
    }

    fun groupStartPx(
        mainLeftPx: Float,
        circleSizePx: Float,
        compactGapPx: Float,
        isSplitMode: Boolean,
        isCircleLeft: Boolean
    ): Float =
        if (isCircleLeft && isSplitMode) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx

    fun groupEndPx(
        mainLeftPx: Float,
        mainWidthPx: Float,
        circleSizePx: Float,
        compactGapPx: Float,
        isSplitMode: Boolean,
        isCircleLeft: Boolean
    ): Float =
        if (!isCircleLeft
            && isSplitMode) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
}
