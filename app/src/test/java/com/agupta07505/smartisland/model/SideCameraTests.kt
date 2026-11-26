/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SideCameraTest {
    private val eps = 0.001f

    @Test
    fun cameraOffsetPicksSide() {
        assertEquals(CameraSide.Centre, SideCamera.sideOf(null))
        assertEquals(CameraSide.Centre, SideCamera.sideOf(0f))
        assertEquals(CameraSide.Centre, SideCamera.sideOf(-30f))
        assertEquals(CameraSide.Centre, SideCamera.sideOf(45f))
        assertEquals(CameraSide.Centre, SideCamera.sideOf(-60f))
        assertEquals(CameraSide.Centre, SideCamera.sideOf(60f))
        assertEquals(CameraSide.Left, SideCamera.sideOf(-60.01f))
        assertEquals(CameraSide.Right, SideCamera.sideOf(60.01f))
        assertEquals(CameraSide.Left, SideCamera.sideOf(-150f))
        assertEquals(CameraSide.Right, SideCamera.sideOf(150f))
    }


    @Test fun circleChoiceFollowsCameraSide() {
        assertFalse(SideCamera.circleLeft(CameraSide.Left, userChoiceLeft = true))
        assertFalse(SideCamera.circleLeft(CameraSide.Left, userChoiceLeft = false))
        assertTrue(SideCamera.circleLeft(CameraSide.Right, userChoiceLeft = true))
        assertTrue(SideCamera.circleLeft(CameraSide.Right, userChoiceLeft = false))
        assertTrue(SideCamera.circleLeft(CameraSide.Centre, userChoiceLeft = true))
        assertFalse(SideCamera.circleLeft(CameraSide.Centre, userChoiceLeft = false))
    }

    private fun leftEdge(centre: Float, width: Float) = centre - width / 2f
    private fun rightEdge(centre: Float, width: Float) = centre + width / 2f

    @Test
    fun anchoredGrowth() {
        val leftCentre = 100f
        val leftGrown = leftCentre + SideCamera.anchoredShiftDp(CameraSide.Left, 20f)
        assertEquals(leftEdge(leftCentre, 100f), leftEdge(leftGrown, 120f), eps)
        assertEquals(rightEdge(leftCentre, 100f) + 20f, rightEdge(leftGrown, 120f), eps)

        val rightCentre = 300f
        val rightGrown = rightCentre + SideCamera.anchoredShiftDp(CameraSide.Right, 20f)
        assertEquals(rightEdge(rightCentre, 100f), rightEdge(rightGrown, 120f), eps)
        assertEquals(leftEdge(rightCentre, 100f) - 20f, leftEdge(rightGrown, 120f), eps)

        val shrinkingCentre = 100f
        val shrunk = shrinkingCentre + SideCamera.anchoredShiftDp(CameraSide.Left, -30f)
        assertEquals(leftEdge(shrinkingCentre, 130f), leftEdge(shrunk, 100f), eps)
    }

    @Test fun centreCameraDoesNotShift() = assertEquals(0f, SideCamera.anchoredShiftDp(CameraSide.Centre, 40f), eps)

    @Test fun bandIsAtLeast40AndNeverNarrowerThanTheHole() {
        assertEquals(40f, SideCamera.bandWidthDp(30f), eps)
        assertEquals(40f, SideCamera.bandWidthDp(0f), eps)
        assertEquals(70f, SideCamera.bandWidthDp(70f), eps)
    }

    @Test fun nudgeRangeKeepsTheHoleInsideTheBand() {
        assertEquals(5f, SideCamera.nudgeRangeDp(30f), eps)
        assertEquals(2f, SideCamera.nudgeRangeDp(36f), eps)
        assertEquals(0f, SideCamera.nudgeRangeDp(50f), eps)
        assertEquals(0f, SideCamera.nudgeRangeDp(60f), eps)
        assertEquals(0f, SideCamera.nudgeRangeDp(70f), eps)
    }

    @Test
    fun sideCameraPlacement() {
        // screen 392: hole centre at x = 46, band 40 wide
        assertEquals(26f, SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 30f, 150f, 0f), eps)

        // hole centre at x = 346: the pill ends at 366 and so starts at 216
        val left = SideCamera.desiredLeftDp(CameraSide.Right, 392f, 150f, 30f, 150f, 0f)
        assertEquals(216f, left, eps)
        assertEquals(366f, left + 150f, eps)

        val l = SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 30f, 150f, 0f)
        val r = SideCamera.desiredLeftDp(CameraSide.Right, 392f, 150f, 30f, 150f, 0f)
        assertEquals(l, 392f - (r + 150f), eps)

        fun at(x: Float) = SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 30f, 150f, x)
        assertEquals(29f, at(3f), eps)
        assertEquals(31f, at(100f), eps)
        assertEquals(21f, at(-100f), eps)

        assertEquals(
            SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 70f, 150f, 0f),
            SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 70f, 150f, 40f),
            eps
        )
    }

    @Test fun narrowScreenPlacement() =
        assertEquals(20f, SideCamera.desiredLeftDp(CameraSide.Left, 320f, -120f, 30f, 150f, 0f), eps)

    @Test
    fun holeClearance() {
        for (x in listOf(-300f, -12f, 0f, 12f, 300f)) {
            val left = SideCamera.desiredLeftDp(CameraSide.Left, 392f, -150f, 30f, 150f, x)
            val holeInPill = (196f - 150f) - left
            val nudgedBand = SideCamera.zone(CameraSide.Left, 150f, holeInPill, 30f)!!.band
            val bandLeft = 75f + nudgedBand.shiftDp - nudgedBand.widthDp / 2f
            val bandRight = bandLeft + nudgedBand.widthDp
            assertTrue("x=$x", holeInPill - 15f >= bandLeft - eps && holeInPill + 15f <= bandRight + eps)
        }

        // the pill sits at the edge margin; the hole centre is only 6 dp inside it
        val clipped = SideCamera.zone(CameraSide.Left, 150f, 6f, 30f)!!
        assertEquals(26f, clipped.band.widthDp, eps)
        assertEquals(-62f, clipped.band.shiftDp, eps)
        assertEquals(23f, clipped.plan.glyphStartDp, eps)
        assertEquals(59f, clipped.plan.valueStartDp, eps)
        assertEquals(79f, clipped.plan.valueMaxWidthDp!!, eps)

        val outside = SideCamera.zone(CameraSide.Left, 150f, -40f, 30f)!!
        assertEquals(40f, outside.band.widthDp, eps)
        assertEquals(-55f, outside.band.shiftDp, eps)
        assertEquals(44f, outside.plan.glyphStartDp, eps)
        val r = SideCamera.zone(CameraSide.Right, 150f, 190f, 30f)!!
        assertEquals(40f, r.band.widthDp, eps)
        assertEquals(55f, r.band.shiftDp, eps)

        val wide = SideCamera.zone(CameraSide.Left, 150f, 35f, 70f)!!
        assertEquals(70f, wide.band.widthDp, eps)
        // hugs the hole (35 + 35 + 2)
        assertEquals(72f, wide.plan.glyphStartDp, eps)

        for (hw in listOf(20f, 30f, 40f)) for (cam in listOf(-10f, 0f, 6f, 15f, 28f)) {
            val clear = SideCamera.zone(CameraSide.Left, 150f, cam, hw)!!
            assertTrue("clips hw=$hw cam=$cam", clear.plan.glyphStartDp >= SLOT_START_PADDING_DP - eps)
            if (cam >= 0f) {
                assertTrue("hole gap hw=$hw cam=$cam", clear.plan.glyphStartDp >= minOf(cam + hw / 2f + SideCamera.GLYPH_HOLE_GAP_DP, clear.bandEndFromCameraEdgeDp + SideCamera.CAP_PADDING_DP) - eps)
            }
        }
    }

    @Test fun centreHasNoSideZone() = assertNull(SideCamera.zone(CameraSide.Centre, 150f, 75f, 30f))

    @Test
    fun mirroredTouchZones() {
        val left = SideCamera.zone(CameraSide.Left, 150f, 28f, 30f)!!
        assertEquals(-47f, left.band.shiftDp, eps)
        assertEquals(40f, left.band.widthDp, eps)
        assertEquals(SlotSide.RightOfCamera, left.plan.side)
        // the glyph sits 2 dp beyond the hole's edge (28 + 15 + 2)
        assertEquals(45f, left.plan.glyphStartDp, eps)
        assertEquals(81f, left.plan.valueStartDp, eps)
        assertEquals(138f, left.plan.valueEndDp, eps)
        assertEquals(57f, left.plan.valueMaxWidthDp!!, eps)
        assertEquals(48f, left.bandEndFromCameraEdgeDp, eps)

        val spaced = SideCamera.zone(CameraSide.Left, 150f, 28f, 30f)!!
        val gap = spaced.plan.valueStartDp - (spaced.plan.glyphStartDp + SLOT_GLYPH_SIZE_DP)
        assertEquals(SideCamera.PAIR_GAP_DP, gap, eps)
        assertTrue(gap > SLOT_PAIR_GAP_DP)
        assertTrue(gap < CUTOUT_TARGET_MIN_WIDTH_DP)

        val l = SideCamera.zone(CameraSide.Left, 150f, 28f, 30f)!!
        val r = SideCamera.zone(CameraSide.Right, 150f, 122f, 30f)!!
        assertEquals(-l.band.shiftDp, r.band.shiftDp, eps)
        assertEquals(l.band.widthDp, r.band.widthDp, eps)
        assertEquals(SlotSide.LeftOfCamera, r.plan.side)
        assertEquals(150f - (l.plan.glyphStartDp + SLOT_GLYPH_SIZE_DP), r.plan.glyphStartDp, eps)
        assertEquals(150f - l.plan.valueEndDp, r.plan.valueStartDp, eps)
        assertEquals(150f - l.plan.valueStartDp, r.plan.valueEndDp, eps)
        assertEquals(l.plan.valueMaxWidthDp!!, r.plan.valueMaxWidthDp!!, eps)

        val narrow = SideCamera.zone(CameraSide.Left, 90f, 28f, 30f)!!
        assertEquals(0f, narrow.plan.valueMaxWidthDp!!, eps)
    }

    @Test fun bandAndSlotsStayInside() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (pw in listOf(110f, 150f, 180f)) for (hw in listOf(24f, 30f, 52f, 70f)) {
            for (cam in listOf(2f, 10f, 28f, 40f)) {
                val inPill = if (side == CameraSide.Left) cam else pw - cam
                val z = SideCamera.zone(side, pw, inPill, hw)!!
                val bandLeft = pw / 2f + z.band.shiftDp - z.band.widthDp / 2f
                val bandRight = bandLeft + z.band.widthDp
                val tag = "$side pw=$pw hw=$hw cam=$cam"
                assertTrue("band in pill $tag", bandLeft >= -eps && bandRight <= pw + eps)
                val glyphEnd = z.plan.glyphStartDp + SLOT_GLYPH_SIZE_DP
                if (side == CameraSide.Left) {
                    assertTrue("glyph after band (less its margin) $tag", z.plan.glyphStartDp >= bandRight - GLYPH_BAND_MARGIN_MAX - eps)
                    assertTrue("value after glyph $tag", z.plan.valueStartDp >= glyphEnd - eps)
                } else {
                    assertTrue("glyph before band (less its margin) $tag", glyphEnd <= bandLeft + GLYPH_BAND_MARGIN_MAX + eps)
                    assertTrue("value before glyph $tag", z.plan.valueEndDp <= z.plan.glyphStartDp + eps)
                }
                assertTrue("value width not negative $tag", z.plan.valueMaxWidthDp!! >= 0f)
            }
        }
    }

    private val GLYPH_BAND_MARGIN_MAX = 30f


    @Test fun fadeEndsAtNearestSlide() {
        assertEquals(12f, SideCamera.fadeEndFromCameraDp(30f, 18f), 0.001f)
        assertEquals(30f, SideCamera.fadeEndFromCameraDp(30f, 0f), 0.001f)
    }
}

class SideCameraSlotsTest {
    private val widths = listOf(144f, 158f, 175f, 200f)
    private val holes = listOf(24f, 40f, 70f)

    private fun zoneFor(side: CameraSide, width: Float, hole: Float): SideCameraZone {
        val cam = SideCamera.bandWidthDp(hole) / 2f + 8f
        val centreInPill = if (side == CameraSide.Left) cam else width - cam
        return SideCamera.zone(side, width, centreInPill, hole)!!
    }

    @Test
    fun glyphStartShift() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (w in widths) for (hole in holes) {
            val plan = zoneFor(side, w, hole).plan
            val shellWidths = listOf(0.85f, 1f, 1.06f, 1.125f).map { w * it }
            val distances = shellWidths.map { shell ->
                val shift = SideCamera.startShiftDp(side, shell, w)
                val glyphLeft = plan.glyphStartDp + shift
                if (side == CameraSide.Left) glyphLeft else shell - (glyphLeft + SLOT_GLYPH_SIZE_DP)
            }
            for (d in distances) assertEquals("$side $w $hole", distances.first(), d, 0.001f)
        }

        assertEquals(10f, SideCamera.startShiftDp(CameraSide.Centre, 160f, 140f), 0.001f)
        assertEquals(10f, SideCamera.endShiftDp(CameraSide.Centre, 160f, 140f), 0.001f)
        assertEquals(0f, SideCamera.startShiftDp(CameraSide.Centre, 120f, 140f), 0.001f)

        assertEquals(0f, SideCamera.startShiftDp(CameraSide.Left, 160f, 140f), 0.001f)
        assertEquals(20f, SideCamera.endShiftDp(CameraSide.Left, 160f, 140f), 0.001f)
        assertEquals(20f, SideCamera.startShiftDp(CameraSide.Right, 160f, 140f), 0.001f)
        assertEquals(0f, SideCamera.endShiftDp(CameraSide.Right, 160f, 140f), 0.001f)
        assertEquals(-20f, SideCamera.startShiftDp(CameraSide.Right, 120f, 140f), 0.001f)
    }

    @Test fun slotsStayInShellOutsideBandAtIdleContentPeak() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (w in widths) for (hole in holes) {
            val zone = zoneFor(side, w, hole)
            val plan = zone.plan
            val valueMax = plan.valueMaxWidthDp!!
            for (shell in listOf(w, w * 1.06f, w * 1.125f)) {
                val shift = SideCamera.startShiftDp(side, shell, w)
                val glyphLeft = plan.glyphStartDp + shift
                val glyphRight = glyphLeft + SLOT_GLYPH_SIZE_DP
                val valueLeft = plan.valueStartDp + shift
                val valueRight = valueLeft + valueMax
                val bandLeft = w / 2f + zone.band.shiftDp - zone.band.widthDp / 2f + shift
                val bandRight = bandLeft + zone.band.widthDp
                val label = "$side $w $hole $shell"
                assertTrue(label, glyphLeft >= 0f && valueLeft >= 0f)
                assertTrue(label, glyphRight <= shell && valueRight <= shell)
                if (side == CameraSide.Left) {
                    assertTrue(label, glyphLeft - bandRight >= -30f)
                    assertEquals(label, SideCamera.PAIR_GAP_DP, valueLeft - glyphRight, 0.001f)
                } else {
                    assertTrue(label, bandLeft - glyphRight >= -30f)
                    assertEquals(label, SideCamera.PAIR_GAP_DP, glyphLeft - valueRight, 0.001f)
                }
            }
        }
    }
}

class SideCameraDeadZoneTest {
    private val eps = 0.001f
    private val pill = 150f

    @Test
    fun deadZoneExtent() {
        assertEquals(20f + 15f + SideCamera.GLYPH_HOLE_GAP_DP, SideCamera.deadZoneEndDp(20f, 30f), eps)

        val z = SideCamera.zone(CameraSide.Left, pill, 20f, 30f)!!
        val glyph = SideCamera.glyphStartFromCameraDp(CameraSide.Left, z.plan, pill)
        val dead = SideCamera.deadZoneEndDp(20f, 30f)
        assertEquals(0f, SideCamera.slideTowardCameraLimitDp(dead, glyph), eps)
    }

    @Test fun glyphStartFromCameraEdge() {
        val l = SideCamera.zone(CameraSide.Left, pill, 20f, 30f)!!
        val r = SideCamera.zone(CameraSide.Right, pill, pill - 20f, 30f)!!
        assertEquals(SideCamera.glyphStartFromCameraDp(CameraSide.Left, l.plan, pill), SideCamera.glyphStartFromCameraDp(CameraSide.Right, r.plan, pill), eps)
    }

    @Test fun clipNeverHidesRestingGlyph() {
        assertEquals(8f, SideCamera.contentClipFromCameraDp(37f, 8f), eps)
        assertEquals(37f, SideCamera.contentClipFromCameraDp(37f, 60f), eps)
    }

}

class SideCameraGesturesTest {
    private val eps = 0.001f
    private val screen = 392f
    private val pill = 150f
    private val pillHeight = 34f

    /** Left camera: hole centre 46 dp from the screen's left (-150 from the centre) */
    private val leftBand get() = SideCamera.zone(CameraSide.Left, pill, 20f, 30f)!!.band
    private val rightBand get() = SideCamera.zone(CameraSide.Right, pill, 130f, 30f)!!.band

    private fun tap(x: Float, band: CameraBand): Boolean =
        pillCenterTapHit(x, 10f, pill, pill, pillHeight, band.shiftDp, band.widthDp)

    @Test fun leftBandIsOnTheHoleAndBiggerThanIt() {
        assertTrue(tap(20f, leftBand))
        // the hole spans 5..35 in the pill
        assertTrue(tap(5f, leftBand)); assertTrue(tap(35f, leftBand))
        assertTrue(tap(2f, leftBand)); assertTrue(tap(38f, leftBand))
        assertTrue(leftBand.widthDp > 30f)
    }

    @Test
    fun tapBandsMirror() {
        assertFalse(tap(45f, leftBand))
        assertFalse(tap(100f, leftBand))
        assertTrue(tap(130f, rightBand))
        assertTrue(tap(112f, rightBand)); assertTrue(tap(148f, rightBand))
        assertFalse(tap(105f, rightBand))
        assertFalse(tap(40f, rightBand))
    }


    @Test fun bandStaysInsideThePill() {
        for (band in listOf(leftBand, rightBand)) {
            val left = pill / 2f + band.shiftDp - band.widthDp / 2f
            assertTrue(left >= -eps && left + band.widthDp <= pill + eps)
        }
    }

    @Test fun sideBandOverlapsTheHoleSoNoExtraTouchRectIsNeeded() {
        val centre = bandCentreFromScreenCentreDp(screen, 26f, pill, leftBand)
        assertEquals(-150f, centre, eps)
        assertTrue(centreBandOverlapsCutout(centre, leftBand.widthDp, 12f, 34f, -150f, 30f, 12f, 30f))
        val rCentre = bandCentreFromScreenCentreDp(screen, 216f, pill, rightBand)
        assertEquals(150f, rCentre, eps)
        assertTrue(centreBandOverlapsCutout(rCentre, rightBand.widthDp, 12f, 34f, 150f, 30f, 12f, 30f))
    }

    @Test fun closeZoneIsTheSideBandOnTheHole() {
        // the collapsed pill's centre from the screen centre: 26 + 75 - 196 = -95
        val z = expandedCloseZone(true, 30f, 30f, -150f, 12f, 12f, pill, pillHeight, leftBand, pillCentreXDp = -95f, screenWidthDp = screen)
        assertEquals(40f, z.widthDp, eps)
        assertEquals(-150f, z.offsetXDp!!, eps)
        val r = expandedCloseZone(true, 30f, 30f, 150f, 12f, 12f, pill, pillHeight, rightBand, pillCentreXDp = 95f, screenWidthDp = screen)
        assertEquals(40f, r.widthDp, eps)
        assertEquals(150f, r.offsetXDp!!, eps)
    }

    @Test
    fun idleTouchStrips() {
        val left = idleStripsFor(pill, leftBand)
        assertEquals(0f, left.leftDp, eps)
        assertEquals(40f, left.bandDp, eps)
        assertEquals(110f, left.rightDp, eps)

        val right = idleStripsFor(pill, rightBand)
        assertEquals(110f, right.leftDp, eps)
        assertEquals(40f, right.bandDp, eps)
        assertEquals(0f, right.rightDp, eps)

        for (band in listOf(leftBand, rightBand)) {
            val combined = idleStripsFor(pill, band)
            assertEquals(pill, combined.leftDp + combined.bandDp + combined.rightDp, eps)
        }
    }

    @Test fun slotSideFollowsTheCameraSide() {
        assertEquals(SlotSide.RightOfCamera, SideCamera.zone(CameraSide.Left, pill, 20f, 30f)!!.plan.side)
        assertEquals(SlotSide.LeftOfCamera, SideCamera.zone(CameraSide.Right, pill, 130f, 30f)!!.plan.side)
    }
}

/** Side camera: the glyph hugs the camera cap */
class SideCameraHugEndsTest {
    private val widths = listOf(144f, 158f, 200f)

    @Test fun farEndShiftNeverNegative() {
        assertEquals(30f, SideCamera.farEndShiftDp(30f), 0.001f)
        assertEquals(0f, SideCamera.farEndShiftDp(-12f), 0f)
    }

    /** Distance of the slot's far edge from the pill's far end */
    @Test fun valueKeepsItsDistanceFromTheFarEndWhenTheShellGrows() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (w in widths) {
            val cam = SideCamera.TAP_BAND_MIN_DP / 2f
            val zone = SideCamera.zone(side, w, if (side == CameraSide.Left) cam else w - cam, 24f)!!
            val plan = zone.plan
            val sign = if (side == CameraSide.Right) -1f else 1f
            val gaps = listOf(1f, 1.1f, 1.6f, 2f).map { f ->
                val shell = w * f
                val left = plan.valueStartDp + SideCamera.startShiftDp(side, shell, w) +
                    sign * SideCamera.farEndShiftDp(shell - w)
                if (side == CameraSide.Left) shell - (left + plan.valueMaxWidthDp!!) else left
            }
            for (g in gaps) assertEquals("$side $w", gaps.first(), g, 0.001f)
        }
    }

    @Test fun glyphKeepsItsDistanceFromTheCameraAtEveryWidth() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (w in widths) {
            val cam = SideCamera.TAP_BAND_MIN_DP / 2f
            val plan = SideCamera.zone(side, w, if (side == CameraSide.Left) cam else w - cam, 24f)!!.plan
            val d = listOf(0.85f, 1f, 1.6f).map { f ->
                val shell = w * f
                val left = plan.glyphStartDp + SideCamera.startShiftDp(side, shell, w)
                if (side == CameraSide.Left) left else shell - (left + SLOT_GLYPH_SIZE_DP)
            }
            for (x in d) assertEquals("$side $w", d.first(), x, 0.001f)
        }
    }

    @Test fun valueNeverOverlapsGlyph() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (w in widths) {
            val cam = SideCamera.TAP_BAND_MIN_DP / 2f
            val plan = SideCamera.zone(side, w, if (side == CameraSide.Left) cam else w - cam, 24f)!!.plan
            val sign = if (side == CameraSide.Right) -1f else 1f
            for (f in listOf(0.85f, 1f, 1.125f, 1.6f, 2f)) {
                val shell = w * f
                val shift = SideCamera.startShiftDp(side, shell, w)
                val g = plan.glyphStartDp + shift
                val v = plan.valueStartDp + shift + sign * SideCamera.farEndShiftDp(shell - w)
                val gap = if (side == CameraSide.Left) v - (g + SLOT_GLYPH_SIZE_DP) else g - (v + plan.valueMaxWidthDp!!)
                assertTrue("$side $w $f", gap >= SideCamera.PAIR_GAP_DP - 0.001f)
            }
        }
    }
}

class SideCameraGrowthTest {
    private val settled = 140f
    private val sizes = listOf(100f, 140f, 157.5f, 175f, 210f)

    private fun start(side: CameraSide, progress: Float, size: Float, pillCentre: Float = -20f): Float {
        val fraction = if (side == CameraSide.Left) 0f else 1f
        return ExpandOrigin.startAnchored(
            progress = progress,
            size = size,
            pillAnchor = pillCentre + (fraction - 0.5f) * settled,
            pillAnchorFraction = fraction,
            pillCentre = pillCentre,
            cardStart = -170f,
            cardSize = 340f
        )
    }

    @Test fun leftCameraKeepsLeftEdge() {
        val edge = -20f - settled / 2f
        for (size in sizes) assertEquals(edge, start(CameraSide.Left, 0f, size), 0.001f)
    }

    @Test fun rightCameraKeepsRightEdge() {
        val edge = -20f + settled / 2f
        for (size in sizes) assertEquals(edge, start(CameraSide.Right, 0f, size) + size, 0.001f)
    }

    @Test fun shellCentreMovesByTheAnchoredShift() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (size in sizes) {
            val centre = start(side, 0f, size) + size / 2f
            val settledCentre = start(side, 0f, settled) + settled / 2f
            assertEquals(SideCamera.anchoredShiftDp(side, size - settled), centre - settledCentre, 0.001f)
        }
    }

    @Test fun openIslandEndsOnCard() {
        for (side in listOf(CameraSide.Left, CameraSide.Right)) {
            val open = start(side, 1f, 340f)
            assertEquals(ExpandOrigin.start(1f, 340f, -20f, -170f, 340f), open, 0.001f)
            assertEquals(-170f, open, 0.001f)
        }
    }

    @Test fun anchoredOriginAtTheCentreAnchorEqualsTheClassicOne() {
        for (progress in listOf(0f, 0.3f, 0.7f, 1f)) for (size in sizes) {
            val a = ExpandOrigin.startAnchored(progress, size, -20f, 0.5f, -20f, -170f, 340f)
            assertEquals(ExpandOrigin.start(progress, size, -20f, -170f, 340f), a, 0.001f)
        }
    }

    @Test fun bandStaysOnTheHoleWhileTheShellStretches() {
        val hole = 40f
        for (side in listOf(CameraSide.Left, CameraSide.Right)) for (cam in listOf(30f, 40f, 60f)) {
            val centreInPill = if (side == CameraSide.Left) cam else settled - cam
            val zone = SideCamera.zone(side, settled, centreInPill, hole)!!
            for (size in sizes) {
                val shellLeft = start(side, 0f, size)
                val settledLeft = start(side, 0f, settled)
                val bandCentreOnScreen = settledLeft + settled / 2f + zone.band.shiftDp
                val shift = SideCamera.bandShiftInShellDp(side, zone.band.shiftDp, size, settled)
                assertEquals(bandCentreOnScreen, shellLeft + size / 2f + shift, 0.001f)
            }
        }
    }

    @Test fun centreBandShift() {
        assertEquals(12f, SideCamera.bandShiftInShellDp(CameraSide.Left, 12f, settled, settled), 0.001f)
        assertEquals(32f, SideCamera.bandShiftInShellDp(CameraSide.Left, 12f, 100f, settled), 0.001f)
        assertEquals(12f, SideCamera.bandShiftInShellDp(CameraSide.Centre, 12f, 200f, settled), 0.001f)
    }
}

class SideCameraArtistTest {
    private val bars = 14f

    @Test
    fun artistWidthBudget() {
        val sharedFit = PulseWidth.fitSide(60f, 30f, titleDp = 40f, valueDp = PulseWidth.valueSideDp(bars, 90f), maxDp = 400f)
        assertEquals(40f, sharedFit.titleMaxDp, 0.001f)
        assertEquals(90f + sharedFit.titleMaxDp, PulseWidth.sideArtistMaxDp(sharedFit.widthDp, 30f, hasTitle = true, valueDp = bars), 0.001f)

        val cappedFit = PulseWidth.fitSide(60f, 30f, titleDp = 40f, valueDp = PulseWidth.valueSideDp(bars, 300f), maxDp = 150f)
        assertEquals(150f, cappedFit.widthDp, 0.001f)
        // 30 + 22 + 6 before the title, 14 + 12 after the value side, 6 and 14 for the artist's gap and the bars
        val cappedRoom = PulseWidth.sideArtistMaxDp(cappedFit.widthDp, 30f, hasTitle = true, valueDp = bars)
        assertEquals(150f - 58f - SideCamera.PAIR_GAP_DP - SLOT_END_PADDING_DP - PulseWidth.ARTIST_GAP_DP - bars, cappedRoom, 0.001f)
        assertEquals(0f, cappedFit.titleMaxDp, 0.001f)

        val withTitle = PulseWidth.sideArtistMaxDp(200f, 30f, hasTitle = true, valueDp = bars)
        val without = PulseWidth.sideArtistMaxDp(200f, 30f, hasTitle = false, valueDp = bars)
        assertEquals(SLOT_PAIR_GAP_DP, without - withTitle, 0.001f)

        val centredGap = LandscapeClassicPulse.centreGapDp(100f, bars)
        val centredFit = PulseWidth.fitClassic(100f, centredGap, titleDp = 60f, valueDp = PulseWidth.valueSideDp(bars, 80f), maxDp = 400f)
        assertEquals(60f, centredFit.titleMaxDp, 0.001f)
        val centredRoom = PulseWidth.sideArtistMaxDp(centredFit.widthDp, SLOT_START_PADDING_DP, hasTitle = true, valueDp = bars, gapDp = centredGap)
        assertEquals(80f + centredFit.titleMaxDp, centredRoom, 0.001f)
    }

    @Test
    fun aCentredClassicPillWithoutAnArtistKeepsItsFit() {
        val gap = LandscapeClassicPulse.centreGapDp(100f, bars)
        assertEquals(
            PulseWidth.fitClassic(100f, gap, 60f, bars, 400f),
            PulseWidth.fitClassic(100f, gap, 60f, PulseWidth.valueSideDp(bars, 0f), 400f)
        )
    }

    private fun styledSide(style: String, x: Float = 0f, screenDp: Float = 392f): CameraSide {
        val settings = SmartIslandSettings(cameraCutoutStyle = style, cameraCutoutX = x)
        return SideCamera.sideOf(CutoutPlacement.styledArea(settings, screenDp)?.centreXDp)
    }

    @Test
    fun artistSideOverrides() {
        for (screen in listOf(360f, 392f, 412f)) {
            assertEquals(CameraSide.Left, styledSide(SmartIslandSettings.CUTOUT_STYLE_LEFT, screenDp = screen))
            assertEquals(CameraSide.Right, styledSide(SmartIslandSettings.CUTOUT_STYLE_RIGHT, screenDp = screen))
            assertEquals(CameraSide.Centre, styledSide(SmartIslandSettings.CUTOUT_STYLE_CENTER, screenDp = screen))
            assertEquals(CameraSide.Centre, styledSide(SmartIslandSettings.CUTOUT_STYLE_TEARDROP, screenDp = screen))
        }

        assertEquals(CameraSide.Centre, styledSide(SmartIslandSettings.CUTOUT_STYLE_AUTO))

        val centre = SmartIslandSettings.CUTOUT_STYLE_CENTER
        assertEquals(CameraSide.Centre, styledSide(centre, x = -60f))
        assertEquals(CameraSide.Left, styledSide(centre, x = -61f))
        assertEquals(CameraSide.Centre, styledSide(centre, x = 60f))
        assertEquals(CameraSide.Right, styledSide(centre, x = 61f))

        val left = SmartIslandSettings.CUTOUT_STYLE_LEFT
        assertEquals(CameraSide.Left, styledSide(left, x = 50f))
        assertEquals(CameraSide.Centre, styledSide(left, x = 100f))
    }
}
