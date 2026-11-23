/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedGroupPlacementTest {
    private val screen = 1080f
    private val edge = 24f
    private val main = 300f
    private val circle = 90f
    private val gap = 24f

    private fun clamp(desired: Float, split: Boolean, circleLeft: Boolean, edgePx: Float = edge) =
        CollapsedGroupPlacement.clampedMainLeftPx(desired, screen, edgePx, main, circle, gap, split, circleLeft)

    @Test
    fun groupScreenBounds() {
        assertEquals(390f, clamp(390f, split = false, circleLeft = false), 0f)
        assertEquals(390f, clamp(390f, split = true, circleLeft = true), 0f)
        assertEquals(390f, clamp(390f, split = true, circleLeft = false), 0f)

        assertEquals(edge, clamp(-50f, split = false, circleLeft = false), 0f)
        assertEquals(screen - edge - main, clamp(5000f, split = false, circleLeft = true), 0f)

        assertEquals(0f, clamp(-50f, split = false, circleLeft = false, edgePx = 0f), 0f)
        assertEquals(screen - main, clamp(5000f, split = false, circleLeft = false, edgePx = 0f), 0f)

        assertEquals(edge + circle + gap, clamp(0f, split = true, circleLeft = true), 0f)
        assertEquals(screen - edge - main, clamp(5000f, split = true, circleLeft = true), 0f)
        assertEquals(edge, clamp(0f, split = true, circleLeft = false), 0f)
        assertEquals(screen - edge - (main + gap + circle), clamp(5000f, split = true, circleLeft = false), 0f)

        for (split in listOf(false, true)) for (left in listOf(false, true)) {
            var d = -500f
            while (d <= 1600f) {
                val v = clamp(d, split, left)
                assertTrue(v >= edge - 1e-3f)
                assertTrue(v + main <= screen - edge + 1e-3f)
                d += 37f
            }
        }
    }

    @Test fun noRoomLeftLowerBoundWins() {
        val narrow = CollapsedGroupPlacement.clampedMainLeftPx(100f, 200f, edge, main, circle, gap, true, true)
        assertEquals(edge + circle + gap, narrow, 0f)
        val narrowRight = CollapsedGroupPlacement.clampedMainLeftPx(100f, 200f, edge, main, circle, gap, true, false)
        assertEquals(edge, narrowRight, 0f)
    }

    @Test fun groupEdgesFollowTheCircleSide() {
        assertEquals(100f, CollapsedGroupPlacement.groupStartPx(100f, circle, gap, false, true), 0f)
        assertEquals(400f, CollapsedGroupPlacement.groupEndPx(100f, main, circle, gap, false, false), 0f)
        assertEquals(100f - gap - circle, CollapsedGroupPlacement.groupStartPx(100f, circle, gap, true, true), 0f)
        assertEquals(400f, CollapsedGroupPlacement.groupEndPx(100f, main, circle, gap, true, true), 0f)
        assertEquals(100f, CollapsedGroupPlacement.groupStartPx(100f, circle, gap, true, false), 0f)
        assertEquals(400f + gap + circle, CollapsedGroupPlacement.groupEndPx(100f, main, circle, gap, true, false), 0f)
    }
}

class CollapsedSlotPlanTest {
    @Test
    fun collapsedSlotBounds() {
        val centredPlan = CollapsedSlots.plan(120f, 60f, 40f)
        assertEquals(SlotSide.Both, centredPlan.side)
        assertEquals(SLOT_START_PADDING_DP, centredPlan.glyphStartDp, 0.001f)
        assertEquals(28f, centredPlan.valueMaxWidthDp!!, 0.001f)

        val noGapPlan = CollapsedSlots.plan(120f, 60f, 0f)
        assertEquals(SlotSide.Both, noGapPlan.side)
        assertNull(noGapPlan.valueMaxWidthDp)

        val leftPlan = CollapsedSlots.plan(130f, 20f, 24f)
        assertEquals(SlotSide.RightOfCamera, leftPlan.side)
        // the 24 dp hole is judged by its 40 dp band (0..40)
        assertEquals(40f + SLOT_START_PADDING_DP, leftPlan.glyphStartDp, 0.001f)
        assertTrue(leftPlan.valueStartDp >= leftPlan.glyphStartDp + SLOT_GLYPH_SIZE_DP)
        assertEquals(130f - SLOT_END_PADDING_DP, leftPlan.valueEndDp, 0.001f)

        for (gap in listOf(24f, 52f, 90f)) {
            val sideBySidePlan = CollapsedSlots.plan(130f, 20f, gap)
            assertEquals(SLOT_PAIR_GAP_DP, sideBySidePlan.valueStartDp - sideBySidePlan.glyphStartDp - SLOT_GLYPH_SIZE_DP, 0.001f)
        }

        val boundedPlan = CollapsedSlots.plan(60f, 5f, 40f)
        assertTrue(boundedPlan.valueMaxWidthDp!! >= 0f)
    }

    @Test fun cameraAtTheRightEndPutsBothLeftOfIt() {
        val plan = CollapsedSlots.plan(130f, 110f, 24f)
        assertEquals(SlotSide.LeftOfCamera, plan.side)
        assertEquals(SLOT_START_PADDING_DP, plan.glyphStartDp, 0.001f)
        assertEquals(46f, plan.valueMaxWidthDp!!, 0.001f)
    }

    @Test fun offCentreWithRoomStaysSplit() {
        // band 40..80: 40 dp left of it and 70 right, both fit a glyph
        val plan = CollapsedSlots.plan(150f, 60f, 24f)
        assertEquals(SlotSide.Both, plan.side)
        assertEquals(80f, plan.valueStartDp, 0.001f)
    }

    @Test fun cameraLeftNeverNegativeStart() {
        // the pill sits right of the camera
        for (centre in listOf(-200f, -60f, -30f, -10f, 0f)) {
            val plan = CollapsedSlots.plan(100f, centre, 20f)
            assertEquals(SlotSide.RightOfCamera, plan.side)
            assertTrue("glyph at $centre", plan.glyphStartDp >= 0f)
            assertTrue("value at $centre", plan.valueStartDp >= 0f)
            assertTrue("width at $centre", plan.valueMaxWidthDp!! >= 0f)
        }
        // the band is clamped onto the left end (0..40 dp)
        assertEquals(40f + SLOT_START_PADDING_DP, CollapsedSlots.plan(100f, -30f, 20f).glyphStartDp, 0.001f)
    }

    @Test fun holeOutsideTheRightEndPutsTheGlyphsLeftOfTheBand() {
        for (centre in listOf(110f, 140f, 260f)) {
            val plan = CollapsedSlots.plan(100f, centre, 20f)
            assertEquals(SlotSide.LeftOfCamera, plan.side)
            assertEquals(SLOT_START_PADDING_DP, plan.glyphStartDp, 0.001f)
            // the band is clamped onto the right end (60..100 dp): the value stops before it
            assertTrue("value end at $centre", plan.valueEndDp <= 60f - SLOT_START_PADDING_DP + 0.001f)
        }
    }

    @Test fun outsideHolesMirror() {
        val left = CollapsedSlots.plan(110f, -90f, 20f)
        val right = CollapsedSlots.plan(110f, 200f, 20f)
        assertEquals(SlotSide.RightOfCamera, left.side)
        assertEquals(SlotSide.LeftOfCamera, right.side)
        val leftBand = cameraBandFor(110f, -90f, 20f)
        val rightBand = cameraBandFor(110f, 200f, 20f)
        assertTrue(left.glyphStartDp >= 110f / 2f + leftBand.shiftDp + leftBand.widthDp / 2f)
        assertTrue(right.valueEndDp <= 110f / 2f + rightBand.shiftDp - rightBand.widthDp / 2f)
    }

    @Test fun noPlanEverStartsBeforeThePill() {
        for (width in listOf(40f, 100f, 200f)) for (centre in -300..400 step 25) for (gap in listOf(0f, 12f, 40f)) {
            val plan = CollapsedSlots.plan(width, centre.toFloat(), gap)
            assertTrue("w=$width c=$centre g=$gap glyph", plan.glyphStartDp >= 0f)
            assertTrue("w=$width c=$centre g=$gap value", plan.valueStartDp >= 0f)
        }
    }

    @Test fun narrowHoleJudgedByBand() {
        // hole 20 dp at the left corner of a 105 dp pill
        val band = cameraBandFor(105f, 22f, 20f)
        val plan = CollapsedSlots.plan(105f, 22f, 20f)
        assertEquals(SlotSide.RightOfCamera, plan.side)
        assertTrue(plan.glyphStartDp >= 105f / 2f + band.shiftDp + band.widthDp / 2f)
    }

    @Test fun glyphsNeverOverlapTheBand() {
        for (width in listOf(105f, 112f, 130f, 150f, 200f)) for (centre in -120..320 step 5) for (gap in listOf(12f, 20f, 24f, 30f, 40f)) {
            val c = centre.toFloat()
            val band = cameraBandFor(width, c, gap)
            val bandLeft = width / 2f + band.shiftDp - band.widthDp / 2f
            val bandRight = bandLeft + band.widthDp
            val plan = CollapsedSlots.plan(width, c, gap)
            val glyphEnd = plan.glyphStartDp + SLOT_GLYPH_SIZE_DP
            when (plan.side) {
                SlotSide.Both, SlotSide.LeftOfCamera -> assertTrue("w=$width c=$c g=$gap glyph", glyphEnd <= bandLeft + 0.001f)
                SlotSide.RightOfCamera -> assertTrue("w=$width c=$c g=$gap glyph", plan.glyphStartDp >= bandRight - 0.001f)
            }
            if (plan.side == SlotSide.Both) assertTrue("w=$width c=$c g=$gap value", plan.valueStartDp >= bandRight - 0.001f)
            if (plan.side == SlotSide.LeftOfCamera) assertTrue("w=$width c=$c g=$gap value", plan.valueEndDp <= bandLeft + 0.001f)
        }
    }

    @Test fun zoneIsTheBandAndThePlanOfOneHole() {
        for (c in listOf(-50f, 10f, 22f, 60f, 100f, 180f)) {
            val zone = CollapsedSlots.zone(112f, c, 24f)
            assertEquals(cameraBandFor(112f, c, 24f), zone.band)
            assertEquals(CollapsedSlots.plan(112f, c, 24f), zone.plan)
            assertEquals(zone.plan.side, zone.side)
        }
        // no hole: the classic centred band, glyphs on both sides
        val none = CollapsedSlots.zone(112f, null, 0f)
        assertEquals(cameraBandFor(112f, null, 0f), none.band)
        assertEquals(SlotSide.Both, none.side)
    }
}
