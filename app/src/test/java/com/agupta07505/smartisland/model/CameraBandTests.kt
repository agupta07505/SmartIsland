/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraBandTest {
    // A 120 dp wide, 34 dp tall pill at 3x density
    private val pillDp = 120f
    private val pillW = 360f
    private val pillH = 102f

    @Test
    fun centreCameraBand() {
        val missingBand = cameraBandFor(pillDp, null, 0f)
        assertEquals(0f, missingBand.shiftDp, 0.001f)
        assertEquals(pillDp * PILL_CENTER_ZONE_SHARE, missingBand.widthDp, 0.001f)
        assertEquals(CameraBand(0f, pillDp * PILL_CENTER_ZONE_SHARE), cameraBandFor(pillDp, 60f, 0f))
        assertEquals(CameraBand(0f, 0f), cameraBandFor(0f, 60f, 20f))

        val centredBand = cameraBandFor(pillDp, 60f, 20f)
        assertEquals(0f, centredBand.shiftDp, 0.001f)
        // the hole is 20 dp: the band grows to the 40 dp minimum target
        assertEquals(CUTOUT_TARGET_MIN_WIDTH_DP, centredBand.widthDp, 0.001f)

        // the slot plan and the band are fed the same camera
        val cameraCentre = 30f
        val plan = CollapsedSlots.plan(pillDp, cameraCentre, 24f)
        val sideBand = cameraBandFor(pillDp, cameraCentre, 24f)
        assertEquals(SlotSide.RightOfCamera, plan.side)
        assertTrue(sideBand.shiftDp < 0f)
        assertTrue(plan.glyphStartDp >= pillDp / 2f + sideBand.shiftDp + sideBand.widthDp / 2f - 0.001f)
    }

    @Test
    fun sideCameraBand() {
        // camera centre 30 dp from the pill's left edge: 30 dp left of the pill centre
        val leftBand = cameraBandFor(pillDp, 30f, 24f)
        assertEquals(-30f, leftBand.shiftDp, 0.001f)
        assertEquals(40f, leftBand.widthDp, 0.001f)

        val rightBand = cameraBandFor(pillDp, 100f, 24f)
        assertEquals(40f, rightBand.shiftDp, 0.001f)

        // a camera right at the pill's left edge: the band is pushed in so it starts at 0
        val clampedLeft = cameraBandFor(pillDp, 4f, 24f)
        assertEquals(0f, clampedLeft.shiftDp + pillDp / 2f - clampedLeft.widthDp / 2f, 0.001f)
        val right = cameraBandFor(pillDp, pillDp - 4f, 24f)
        assertEquals(pillDp, right.shiftDp + pillDp / 2f + right.widthDp / 2f, 0.001f)

        val wideBand = cameraBandFor(pillDp, 60f, 100f)
        assertEquals(pillDp * CAMERA_BAND_MAX_SHARE, wideBand.widthDp, 0.001f)

        val narrowBand = cameraBandFor(30f, 5f, 24f)
        assertEquals(15f, narrowBand.widthDp, 0.001f)
        assertEquals(-7.5f, narrowBand.shiftDp, 0.001f)
    }

    @Test
    fun centreTapHitbox() {
        val half = pillW * PILL_CENTER_ZONE_SHARE / 2f
        assertTrue(pillCenterTapHit(180f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f - half - 1f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f + half + 1f, 50f, pillW, pillW, pillH))

        // camera 30 dp left of the pill centre at 3x
        val shift = -90f
        val width = 120f
        // camera centre is at x = 180 - 90 = 90 px
        assertTrue(pillCenterTapHit(90f, 50f, pillW, pillW, pillH, shift, width))
        assertTrue(pillCenterTapHit(31f, 50f, pillW, pillW, pillH, shift, width))
        assertTrue(pillCenterTapHit(149f, 50f, pillW, pillW, pillH, shift, width))
        assertFalse(pillCenterTapHit(29f, 50f, pillW, pillW, pillH, shift, width))
        assertFalse(pillCenterTapHit(151f, 50f, pillW, pillW, pillH, shift, width))
        // the old centre is now glyph area
        assertFalse(pillCenterTapHit(250f, 50f, pillW, pillW, pillH, shift, width))
        assertFalse(pillCenterTapHit(90f, pillH + 1f, pillW, pillW, pillH, shift, width))

        // expanded: a 900 px wide shell whose top centre is the pill's centre
        assertTrue(pillCenterTapHit(450f - 90f, 40f, 900f, pillW, pillH, -90f, 120f))
        assertFalse(pillCenterTapHit(450f, 40f, 900f, pillW, pillH, -90f, 120f))

        assertFalse(pillCenterTapHit(180f, 50f, pillW, pillW, pillH, 0f, 0f))
    }

    @Test
    fun tapZoneLayout() {
        val splitShift = 60f
        val splitWidth = 120f // band x from 180 + 60 - 60 = 180 to 300
        assertEquals(PillTapZone.Glyph, pillTapZone(100f, pillW, pillW, splitShift, splitWidth))
        assertEquals(PillTapZone.Middle, pillTapZone(240f, pillW, pillW, splitShift, splitWidth))
        assertEquals(PillTapZone.Value, pillTapZone(330f, pillW, pillW, splitShift, splitWidth))

        assertEquals(PillTapZone.Glyph, pillTapZone(pillW * 0.1f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f, pillW, pillW))
        assertEquals(PillTapZone.Value, pillTapZone(pillW * 0.9f, pillW, pillW))

        // camera near the left end: glyph and value both sit right of it
        val oneSidedShift = -90f
        val oneSidedWidth = 120f
        assertEquals(PillTapZone.Middle, pillTapZone(90f, pillW, pillW, oneSidedShift, oneSidedWidth, SlotSide.RightOfCamera))
        assertEquals(PillTapZone.Glyph, pillTapZone(200f, pillW, pillW, oneSidedShift, oneSidedWidth, SlotSide.RightOfCamera))
        assertEquals(PillTapZone.Glyph, pillTapZone(340f, pillW, pillW, oneSidedShift, oneSidedWidth, SlotSide.RightOfCamera))
        // mirror: camera near the right end
        assertEquals(PillTapZone.Glyph, pillTapZone(20f, pillW, pillW, 90f, oneSidedWidth, SlotSide.LeftOfCamera))
        assertEquals(PillTapZone.Middle, pillTapZone(270f, pillW, pillW, 90f, oneSidedWidth, SlotSide.LeftOfCamera))
    }
}

class CameraBandZonesTest {
    private val screenDp = 400f
    private val pillDp = 120f

    @Test
    fun cameraBandBounds() {
        // pill left edge at 8 dp (clamped), camera 20 dp into it
        val centreBand = cameraBandFor(pillDp, 20f, 24f)
        assertEquals(-172f, bandCentreFromScreenCentreDp(screenDp, 8f, pillDp, centreBand), 0.001f)
        // classic band on a centred pill: 0
        assertEquals(0f, bandCentreFromScreenCentreDp(screenDp, 140f, pillDp, cameraBandFor(pillDp, null, 0f)), 0.001f)

        // hole centre at -172 (28 dp from the left), pill clamped to start at 8 dp
        val insideBand = cameraBandFor(pillDp, cameraCentreInPill(screenDp / 2f, -172f, 8f + pillDp / 2f, pillDp), 24f)
        assertTrue(
            centreBandOverlapsCutout(
                bandCentreFromScreenCentreDp(screenDp, 8f, pillDp, insideBand), insideBand.widthDp, 12f, 34f, -172f, 24f, 12f, 24f
            )
        )

        // hole at the very corner (-200, 8 dp wide)
        val inPill = cameraCentreInPill(screenDp / 2f, -200f, 8f + pillDp / 2f, pillDp)!! // -8 dp: outside the pill
        val outsideBand = cameraBandFor(pillDp, inPill, 8f)
        val outsideCentre = bandCentreFromScreenCentreDp(screenDp, 8f, pillDp, outsideBand)
        // band 40 dp wide starting at the pill's left edge
        assertEquals(-172f, outsideCentre, 0.001f)
        assertFalse(centreBandOverlapsCutout(outsideCentre, outsideBand.widthDp, 12f, 34f, -200f, 8f, 12f, 24f))

        val noHoleBand = cameraBandFor(pillDp, 30f, 24f)
        assertEquals(
            expandedCloseZone(false, 0f, 0f, 0f, 0f, 0f, pillDp, 34f),
            expandedCloseZone(false, 0f, 0f, 0f, 0f, 0f, pillDp, 34f, noHoleBand, 0f)
        )

        val classicBand = cameraBandFor(pillDp, null, 0f)
        assertFalse(classicBand.isOffCentre())
        assertEquals(SlotSide.Both, CollapsedSlots.plan(pillDp, null, 0f).side)
        assertNull(cameraCentreInPill(screenDp / 2f, null, screenDp / 2f, pillDp))
    }

    @Test
    fun cameraCloseTargets() {
        val offsetBand = cameraBandFor(pillDp, 30f, 24f) // shift -30
        val offset = expandedCloseZone(true, 48f, 34f, -30f, 12f, 12f, pillDp, 34f, offsetBand, pillCentreXDp = 0f)
        assertEquals(offsetBand.widthDp, offset.widthDp, 0.001f)
        assertEquals(-30f, offset.offsetXDp!!, 0.001f)
        assertEquals(34f, offset.heightDp, 0.001f)
        assertEquals(0f, offset.topDp, 0.001f)

        // pill centre pushed 130 dp left of the screen centre (-190..-70), hole at -196
        val externalBand = cameraBandFor(pillDp, 4f, 14f)
        val external = expandedCloseZone(true, 38f, 34f, -196f, 12f, 12f, pillDp, 34f, externalBand, pillCentreXDp = -130f, screenWidthDp = screenDp)
        assertEquals(-180f, external.offsetXDp!!, 0.001f)
        assertEquals(CUTOUT_TARGET_MIN_WIDTH_DP, external.widthDp, 0.001f)

        // manual hole 120 dp right of the centre, pill centred (-60..60)
        val manualBand = cameraBandFor(pillDp, pillDp / 2f + 120f, 24f)
        val manualZ = expandedCloseZone(true, 24f, 34f, 120f, 12f, 12f, pillDp, 34f, manualBand, pillCentreXDp = 0f, screenWidthDp = screenDp)
        assertEquals(120f, manualZ.offsetXDp!!, 0.001f)

        // hole centre -50 on a pill spanning -60..60
        val alignedBand = cameraBandFor(pillDp, 10f, 24f)
        val aligned = expandedCloseZone(true, 24f, 34f, -50f, 12f, 12f, pillDp, 34f, alignedBand, pillCentreXDp = 0f, screenWidthDp = screenDp)
        assertEquals(-40f, aligned.offsetXDp!!, 0.001f)

        val centredBand = cameraBandFor(pillDp, 61f, 24f) // 1 dp off: counts as centred
        val old = expandedCloseZone(true, 70f, 44f, 6f, 20f, 12f, pillDp, 34f)
        val now = expandedCloseZone(true, 70f, 44f, 6f, 20f, 12f, pillDp, 34f, centredBand, pillCentreXDp = 0f)
        assertEquals(old, now)
    }

    @Test
    fun tapZoneWeights() {
        val outsideBand = cameraBandFor(pillDp, pillDp / 2f + 120f, 24f)
        val z = expandedCloseZone(true, 24f, 34f, 190f, 12f, 12f, pillDp, 34f, outsideBand, pillCentreXDp = 0f)
        assertEquals(190f, z.offsetXDp!!, 0.001f)

        val classic = idleStripsFor(100f, cameraBandFor(100f, null, 0f))
        assertEquals(33f, classic.leftDp, 0.001f)
        assertEquals(34f, classic.bandDp, 0.001f)
        assertEquals(33f, classic.rightDp, 0.001f)

        val shifted = idleStripsFor(pillDp, cameraBandFor(pillDp, 30f, 24f)) // band 40 wide centred at 30: 10..50
        assertEquals(10f, shifted.leftDp, 0.001f)
        assertEquals(40f, shifted.bandDp, 0.001f)
        assertEquals(70f, shifted.rightDp, 0.001f)

        for (centre in listOf(-20f, 0f, 4f, 30f, 60f, 100f, 118f, 150f)) {
            val combined = idleStripsFor(pillDp, cameraBandFor(pillDp, centre, 24f))
            assertEquals(pillDp, combined.leftDp + combined.bandDp + combined.rightDp, 0.001f)
            assertTrue(combined.leftDp >= 0f && combined.rightDp >= 0f)
        }

        val (l, m, r) = idleStripsFor(pillDp, cameraBandFor(pillDp, 0f, 24f)).weights()
        assertTrue(l > 0f && m > 0f && r > 0f)
        val empty = idleStripsFor(0f, CameraBand(0f, 0f))
        assertEquals(IdleStrips(0f, 0f, 0f), empty)
    }

    @Test
    fun offCentreNeedsMoreThanTheTolerance() {
        assertFalse(CameraBand(CAMERA_CENTRED_TOLERANCE_DP, 40f).isOffCentre())
        assertTrue(CameraBand(-CAMERA_CENTRED_TOLERANCE_DP - 0.1f, 40f).isOffCentre())
    }

    @Test
    fun savedCameraBounds() {
        val narrowerArea = CutoutArea(centreXDp = 280f, topDp = 12f, widthDp = 40f, heightDp = 24f)
        val onPhone = narrowerArea.clampedTo(360f)
        assertEquals(180f, onPhone.centreXDp, 0.001f)
        assertEquals(40f, onPhone.widthDp, 0.001f)
        assertEquals(-180f, narrowerArea.copy(centreXDp = -500f).clampedTo(360f).centreXDp, 0.001f)

        val unchangedArea = CutoutArea(-120f, 12f, 40f, 24f)
        assertEquals(unchangedArea, unchangedArea.clampedTo(392f))
        assertEquals(unchangedArea, unchangedArea.clampedTo(0f))
        assertEquals(100f, unchangedArea.copy(widthDp = 400f).clampedTo(100f).widthDp, 0.001f)
    }
}

class CameraBandWiringTest {
    private val density = 3f
    private val pillDp = 120f
    private val pillHeightDp = 34f

    @Test
    fun cameraCentreInPillFollowsTheHole() {
        // 400 dp screen, pill centred (left edge at 140 dp)
        assertEquals(30f, cameraCentreInPill(200f, -30f, 200f, pillDp)!!, 0.001f)
        // centred hole sits in the middle of the pill
        assertEquals(60f, cameraCentreInPill(200f, 0f, 200f, pillDp)!!, 0.001f)
        // the pill clamped away from the centre
        assertEquals(20f, cameraCentreInPill(200f, -160f, 80f, pillDp)!!, 0.001f)
        // a hole outside the pill gives a negative centre
        assertEquals(-40f, cameraCentreInPill(200f, -160f, 140f, pillDp)!!, 0.001f)
        assertEquals(CameraBand(-40f, CUTOUT_TARGET_MIN_WIDTH_DP), cameraBandFor(pillDp, -40f, 20f))
        assertNull(cameraCentreInPill(200f, null, 200f, pillDp))
    }

    @Test
    fun tapOnTheCameraIsTheMiddleAndTheClassicCentreIsNot() {
        val centreInPill = cameraCentreInPill(200f, -40f, 200f, pillDp)!! // 20 dp from the left edge
        val band = cameraBandFor(pillDp, centreInPill, 20f)
        val side = CollapsedSlots.plan(pillDp, centreInPill, 20f).side
        val shellPx = pillDp * density
        val shift = band.shiftDp * density
        val width = band.widthDp * density
        val onCamera = centreInPill * density
        val atClassicCentre = pillDp / 2f * density
        assertEquals(PillTapZone.Middle, pillTapZone(onCamera, shellPx, shellPx, shift, width, side))
        assertTrue(pillCenterTapHit(onCamera, 10f, shellPx, shellPx, pillHeightDp * density, shift, width))
        // the old centre of the pill is now glyph strip
        assertFalse(pillCenterTapHit(atClassicCentre, 10f, shellPx, shellPx, pillHeightDp * density, shift, width))
        assertEquals(PillTapZone.Glyph, pillTapZone(atClassicCentre, shellPx, shellPx, shift, width, side))
    }

    @Test
    fun noCameraIsTodaysBehaviour() {
        val band = cameraBandFor(pillDp, null, 0f)
        val shellPx = pillDp * density
        for (x in listOf(5f, 100f, 180f, 260f, 355f)) {
            assertEquals(
                pillTapZone(x, shellPx, shellPx),
                pillTapZone(x, shellPx, shellPx, band.shiftDp * density, band.widthDp * density, SlotSide.Both)
            )
            assertEquals(
                pillCenterTapHit(x, 10f, shellPx, shellPx, 102f),
                pillCenterTapHit(x, 10f, shellPx, shellPx, 102f, band.shiftDp * density, band.widthDp * density)
            )
        }
    }

    @Test
    fun offCentreBandOverlapsCameraSoOpenAppsMovesOut() {
        // hole 40 dp left of the screen centre, pill centred
        val centreInPill = cameraCentreInPill(200f, -40f, 200f, pillDp)!!
        val band = cameraBandFor(pillDp, centreInPill, 20f)
        val overlaps = centreBandOverlapsCutout(
            bandCentreXDp = 0f + band.shiftDp, bandWidthDp = band.widthDp, bandTopDp = 12f, bandHeightDp = pillHeightDp,
            cutoutCentreXDp = -40f, cutoutWidthDp = 20f, cutoutTopDp = 12f, cutoutHeightDp = 20f
        )
        assertTrue(overlaps)
        // the classic band would have missed the hole
        assertFalse(
            centreBandOverlapsCutout(
                bandCentreXDp = 0f, bandWidthDp = pillDp * PILL_CENTER_ZONE_SHARE, bandTopDp = 12f, bandHeightDp = pillHeightDp,
                cutoutCentreXDp = -40f, cutoutWidthDp = 20f, cutoutTopDp = 12f, cutoutHeightDp = 20f
            )
        )
        // long press: in the band it is the normal action, outside it Open Apps
        assertFalse(longPressOpensApps(false, false, true, overlaps, touchInCentreBand = true))
        assertTrue(longPressOpensApps(false, false, true, overlaps, touchInCentreBand = false))
    }

    @Test
    fun holeOffsetDuringPulse() {
        val band = cameraBandFor(pillDp, 70f, 20f)
        assertEquals(10f, band.shiftDp, 0.001f)
        // the pulse moved the pill's centre 35.5 dp left: the hole is 35.5 dp further right of the shell's centre
        val moved = band.inMovedPill(-35.5f)
        assertEquals(45.5f, moved.shiftDp, 0.001f)
        assertEquals(band.widthDp, moved.widthDp, 0.001f)
    }

    @Test
    fun noMoveLeavesTheBandAsItWas() {
        val band = cameraBandFor(pillDp, 70f, 20f)
        assertEquals(band, band.inMovedPill(0f))
    }

    @Test
    fun theTapOnTheHoleOfAMovedPillHitsTheBand() {
        // a 191 dp shell (573 px at 3x) whose hole lies 45.5 dp right of its centre
        val shellPx = 573f
        val holeX = shellPx / 2f + 45.5f * 3f
        val pillHeightPx = pillHeightDp * density
        val moved = cameraBandFor(pillDp, 70f, 20f).inMovedPill(-35.5f)
        assertTrue(pillCenterTapHit(holeX, pillHeightPx / 2f, shellPx, shellPx, pillHeightPx, moved.shiftDp * 3f, moved.widthDp * 3f))
        // the settled band would have sat 35.5 dp short of the hole
        val settled = cameraBandFor(pillDp, 70f, 20f)
        assertFalse(pillCenterTapHit(holeX, pillHeightPx / 2f, shellPx, shellPx, pillHeightPx, settled.shiftDp * 3f, settled.widthDp * 3f))
    }

    @Test
    fun aPillThePulseReachedFarLeftKeepsTheBandOnTheHole() {
        // the left reaches further than the right, so the centre moves further left (here 90 dp, a 300 dp
        // shell); the hole lies 100 dp right of the shell's centre, and the band follows it
        val settled = cameraBandFor(pillDp, 70f, 20f)
        val moved = settled.inMovedPill(-90f)
        assertEquals(100f, moved.shiftDp, 0.001f)
        assertEquals(settled.widthDp, moved.widthDp, 0.001f)
        val shellPx = 300f * 3f
        val pillHeightPx = pillHeightDp * density
        val holeX = shellPx / 2f + 100f * 3f
        assertTrue(pillCenterTapHit(holeX, pillHeightPx / 2f, shellPx, shellPx, pillHeightPx, moved.shiftDp * 3f, moved.widthDp * 3f))
        // the shell's own middle is now the message, not the band
        assertFalse(pillCenterTapHit(shellPx / 2f, pillHeightPx / 2f, shellPx, shellPx, pillHeightPx, moved.shiftDp * 3f, moved.widthDp * 3f))
    }
}
