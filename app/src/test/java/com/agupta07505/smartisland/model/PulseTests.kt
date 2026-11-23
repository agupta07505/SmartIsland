/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulsePlacementTest {
    private val screenCentre = 196f
    private val width = 112f

    private val centreHole = CutoutArea(0f, 12f, 30f, 28f)
    private val leftHole = CutoutArea(-150f, 12f, 30f, 28f)
    private val rightHole = CutoutArea(150f, 12f, 30f, 28f)

    private fun cameraFor(hole: CutoutArea?): PulseCamera {
        val pillCentre = when {
            hole == null -> screenCentre
            hole.centreXDp < -SideCamera.THRESHOLD_DP -> 80f
            hole.centreXDp > SideCamera.THRESHOLD_DP -> 312f
            else -> screenCentre + hole.centreXDp
        }
        return PulsePlacement.camera(hole, screenCentre, pillCentre, width)
    }

    @Test fun noHoleIsACentreCameraWithNoGap() {
        val camera = cameraFor(null)
        assertEquals(CameraSide.Centre, camera.side)
        assertEquals(0f, camera.gapDp, 0.001f)
        assertNull(camera.centreInPillDp)
        assertNull(camera.sideZone)
    }

    @Test fun holeGivesSideWidthAndPlace() {
        val centre = cameraFor(centreHole)
        assertEquals(CameraSide.Centre, centre.side)
        assertEquals(30f, centre.gapDp, 0.001f)
        assertEquals(width / 2f, centre.centreInPillDp!!, 0.001f)
        assertNull(centre.sideZone)

        val left = cameraFor(leftHole)
        assertEquals(CameraSide.Left, left.side)
        assertEquals(22f, left.centreInPillDp!!, 0.001f)
        assertNotNull(left.sideZone)

        val right = cameraFor(rightHole)
        assertEquals(CameraSide.Right, right.side)
        assertEquals(90f, right.centreInPillDp!!, 0.001f)
        assertNotNull(right.sideZone)
    }

    @Test fun notchPlacementParity() {
        for (hole in listOf(null, centreHole, leftHole, rightHole)) {
            val camera = cameraFor(hole)
            val pill = PulsePlacement.peak(false, false, camera, width)
            val notch = PulsePlacement.peak(true, false, camera, width)
            assertEquals(pill.side, notch.side)
            assertEquals(pill.glyphFromCameraDp, notch.glyphFromCameraDp, 0.001f)
            assertEquals(pill.classic, notch.classic)
            assertFalse(pill.notch)
            assertTrue(notch.notch)
        }
    }

    @Test fun sideHoleStartsGlyphAfterIt() {
        for (hole in listOf(leftHole, rightHole)) {
            val camera = cameraFor(hole)
            val placed = PulsePlacement.peak(true, false, camera, width)
            assertEquals(camera.side, placed.side)
            assertEquals(
                SideCamera.glyphStartFromCameraDp(camera.side, camera.sideZone!!.plan, width),
                placed.glyphFromCameraDp, 0.001f
            )
        }
    }

    @Test fun notchWithoutHoleIsCentreLayout() {
        val placed = PulsePlacement.peak(true, false, cameraFor(null), width)
        assertEquals(CameraSide.Centre, placed.side)
        assertEquals(0f, placed.glyphFromCameraDp, 0.001f)
        assertTrue(placed.notch)
    }

    @Test fun landscapeNotchKeepsItsStartAlignedLayout() {
        val placed = PulsePlacement.peak(true, true, cameraFor(null), width)
        assertEquals(CameraSide.Left, placed.side)
        assertEquals(SLOT_START_PADDING_DP, placed.glyphFromCameraDp, 0.001f)
        assertTrue(placed.notch)
        assertFalse(placed.classic)
    }

    @Test fun landscapeClassicKeepsItsLayouts() {
        val centred = PulsePlacement.peak(false, true, cameraFor(null), width)
        assertEquals(CameraSide.Left, centred.side)
        assertTrue(centred.classic)
        assertFalse(centred.notch)

        val off = PulsePlacement.peak(false, true, cameraFor(null), width, CameraSide.Right)
        assertEquals(CameraSide.Right, off.side)
        assertEquals(LandscapeClassicPulse.OUTER_GLYPH_START_DP, off.glyphFromCameraDp, 0.001f)
        assertTrue(off.classic)
        assertFalse(off.notch)
    }

    @Test fun landscapeClassicLeftAndRightLayoutsKeepTheSettledGap() {
        listOf(CameraSide.Left, CameraSide.Right).forEach { side ->
            val placed = PulsePlacement.peak(false, true, cameraFor(null), width, side)
            assertEquals(side, placed.side)
            assertTrue(placed.classic)
        }
    }
}

class PulseScrollTest {
    @Test
    fun scrollVelocity() {
        assertEquals(PulseScroll.BASE_DP_PER_S, PulseScroll.velocityDpPerSec(0f), 0f)
        assertEquals(PulseScroll.BASE_DP_PER_S, PulseScroll.velocityDpPerSec(PulseScroll.FAST_FROM_DP), 0f)

        assertEquals(39f, PulseScroll.velocityDpPerSec(180f), 0f)
        assertEquals(PulseScroll.FAST_DP_PER_S, PulseScroll.velocityDpPerSec(PulseScroll.FAST_FULL_DP), 0f)
        assertEquals(PulseScroll.FAST_DP_PER_S, PulseScroll.velocityDpPerSec(1_000f), 0f)

        var last = 0f
        for (dp in 0..600 step 10) {
            val v = PulseScroll.velocityDpPerSec(dp.toFloat())
            assertTrue(v >= last)
            last = v
        }
    }

    @Test
    fun scrollDuration() {
        assertEquals(1_000L, PulseScroll.scrollMs(30f))
        assertEquals(6_250L, PulseScroll.scrollMs(300f))
        assertTrue(PulseScroll.scrollMs(200f) > PulseScroll.scrollMs(100f))
        assertTrue(PulseScroll.scrollMs(400f) > PulseScroll.scrollMs(300f))

        assertEquals(PulseScroll.scrollMs(100f) + PulseScroll.END_PAUSE_MS, PulseScroll.remainingMs(100f))

        assertEquals(0L, PulseScroll.scrollMs(0f))
        assertEquals(0L, PulseScroll.remainingMs(0f))
        assertEquals(0L, PulseScroll.remainingMs(-5f))
    }
}

class PulseValueTextTest {
    private fun card(mode: IslandMode, title: String = "", text: String = "", category: String? = null) =
        IslandNotification(
            key = "k", packageName = "pkg", appName = "App", title = title, text = text, timeMillis = 0L,
            mode = mode, category = category
        )

    @Test
    fun distanceIsTheMeasureInTheCard() {
        assertEquals("250 m", PulseValueText.distance("Turn left onto Main St", "250 m"))
        assertEquals("1.2 km", PulseValueText.distance("Head north", "Then 1.2 km to the exit"))
        assertEquals(PulseValueText.DISTANCE_FALLBACK, PulseValueText.distance("Head north", ""))
    }

    @Test
    fun etaIsMinutesArrivedOrActive() {
        assertEquals("5 min", PulseValueText.eta("Order arriving", "Courier is 5 min away"))
        assertEquals("Arrived", PulseValueText.eta("Driver arrived", ""))
        assertEquals("Active", PulseValueText.eta("Order", "Preparing"))
    }

    @Test
    fun hotspotNamesTheDevices() {
        assertEquals("On", PulseValueText.hotspot(null))
        assertEquals("0 devices", PulseValueText.hotspot(0))
        assertEquals("1 device", PulseValueText.hotspot(1))
        assertEquals("3 devices", PulseValueText.hotspot(3))
    }

    @Test
    fun pulseValueFormatting() {
        val hotspot = card(IslandMode.Hotspot, "Hotspot", "2 devices connected", "hotspot")
        assertEquals("2 devices", PulsePeak.of(hotspot, 0L).value)
        val none = card(IslandMode.Hotspot, "Hotspot", "No devices connected", "hotspot")
        assertEquals("0 devices", PulsePeak.of(none, 0L).value)
        val nav = card(IslandMode.Navigation, "Turn left onto Main St", "250 m")
        assertEquals("250 m", PulsePeak.of(nav, 0L).value)
        val live = card(IslandMode.LiveActivity, "Order arriving", "Courier is 5 min away")
        assertEquals("5 min", PulsePeak.of(live, 0L).value)
        assertEquals("ON", PulsePeak.of(card(IslandMode.Flashlight, "Flashlight", "Flashlight on"), 0L).value)

        val buds = card(IslandMode.Bluetooth, "Buds Pro", "Connected \u00b7 82%", "bluetooth")
        val peak = PulsePeak.of(buds, 0L)
        assertEquals(PulsePeak.ValueKind.Reading, peak.valueKind)
        assertEquals("82%", peak.value)
        val plain = PulsePeak.of(card(IslandMode.Bluetooth, "Speaker", "Connected", "bluetooth"), 0L)
        assertEquals(PulsePeak.ValueKind.Icon, plain.valueKind)
    }
}

class PulseWidthTest {
    @Test
    fun pulseWidthLimits() {
        val shortContentFit = PulseWidth.fit(baseDp = 120f, cameraGapDp = 30f, titleDp = 5f, valueDp = 10f, maxDp = 300f)
        assertEquals(120f, shortContentFit.widthDp, 0.001f)

        // lock screen: 8 + 22 on the left, value 100 + 12 on the right
        assertEquals(30f + 2f * 112f, PulseWidth.fit(120f, 30f, titleDp = 0f, valueDp = 100f, maxDp = 400f).widthDp, 0.001f)

        val longValueFit = PulseWidth.fit(120f, 30f, titleDp = 5f, valueDp = 100f, maxDp = 400f)
        assertEquals(30f + 2f * 112f, longValueFit.widthDp, 0.001f)

        val cappedFit = PulseWidth.fit(120f, 30f, titleDp = 400f, valueDp = 40f, maxDp = 250f)
        assertEquals(250f, cappedFit.widthDp, 0.001f)
        assertEquals(110f - 36f, cappedFit.titleMaxDp, 0.001f)

        val narrowRoomFit = PulseWidth.fit(120f, 30f, titleDp = 200f, valueDp = 40f, maxDp = 90f)
        assertEquals(120f, narrowRoomFit.widthDp, 0.001f)
    }

    @Test
    fun perSideWidthLimits() {
        val longTitleFit = PulseWidth.fit(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f)
        // left: 8 + 22 + 6 + 80 = 116 on each side of a 30 dp hole
        assertEquals(262f, longTitleFit.widthDp, 0.001f)
        assertEquals(80f, longTitleFit.titleMaxDp, 0.001f)

        // glyph 8 + 22 + gap 6 + title 40 = 76 left of the hole, value 30 + 12 = 42 right of it; the hole is 20 left of
        // the middle, so the left asks for 2 * (76 + 20) + 30 and the right for 2 * (42 - 20) + 30
        val narrowSideFit = PulseWidth.fit(120f, 30f, titleDp = 40f, valueDp = 30f, maxDp = 400f, shiftDp = -20f)
        assertEquals(30f + 2f * 96f, narrowSideFit.widthDp, 0.001f)
        assertEquals(40f, narrowSideFit.titleMaxDp, 0.001f)
        assertTrue(PulseWidth.rooms(narrowSideFit.widthDp, 30f, -20f).rightDp >= 42f)

        val plain = PulseWidth.fit(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f)
        val zero = PulseWidth.fit(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f, shiftDp = 0f)
        assertEquals(plain, zero)

        val asymmetricFit = PulseWidth.fitPerSide(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f)
        // left: 15 + 8 + 22 + 6 + 80 = 131 from the hole's centre, right stays at the settled 60
        assertEquals(191f, asymmetricFit.widthDp, 0.001f)
        assertEquals(80f, asymmetricFit.titleMaxDp, 0.001f)
        assertEquals(-35.5f, asymmetricFit.centreShiftDp, 0.001f)
        assertTrue(asymmetricFit.widthDp < PulseWidth.fit(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f).widthDp)

        val perSide = PulseWidth.fitPerSide(120f, 30f, titleDp = 76f, valueDp = 100f, maxDp = 400f)
        val symmetric = PulseWidth.fit(120f, 30f, titleDp = 76f, valueDp = 100f, maxDp = 400f)
        assertEquals(symmetric.widthDp, perSide.widthDp, 0.001f)
        assertEquals(symmetric.titleMaxDp, perSide.titleMaxDp, 0.001f)
        assertEquals(0f, perSide.centreShiftDp, 0.001f)
    }

    @Test
    fun emptySplitContent() {
        val shortMessageFit = PulseWidth.fitSplit(baseDp = 120f, cameraGapDp = 30f, messageDp = 20f, titleDp = 10f, maxDp = 300f)
        // left: 15 + 8 + 22 + 6 + 20 + 6 = 77 (glyph, then the message); right: the settled 60
        assertEquals(77f + 60f, shortMessageFit.widthDp, 0.001f)
        assertEquals(60f - 15f - 18f, shortMessageFit.titleMaxDp, 0.001f)
        assertEquals(-8.5f, shortMessageFit.centreShiftDp, 0.001f)

        val iconOnlyFit = PulseWidth.fitSplit(40f, 30f, messageDp = 0f, titleDp = 0f, maxDp = 400f,
            mediaIconDp = PulseWidth.MEDIA_ICON_DP)
        val none = PulseWidth.fitSplit(40f, 30f, messageDp = 0f, titleDp = 0f, maxDp = 400f)
        assertTrue(iconOnlyFit.widthDp >= none.widthDp)
    }

    @Test
    fun splitTextBudget() {
        // left: 15 + 8 + 22 + 6 + 100 + 6 = 157 from the hole's centre; right: 15 + 6 + 20 + 12 = 53, under the settled 60
        val longMessageFit = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 20f, maxDp = 400f)
        assertEquals(157f + 60f, longMessageFit.widthDp, 0.001f)
        assertEquals(-48.5f, longMessageFit.centreShiftDp, 0.001f)

        // right: 15 + 6 + 90 + 12 = 123, the title alone; the short message and the glyph need 77 on the left
        val longTitleFit = PulseWidth.fitSplit(120f, 30f, messageDp = 20f, titleDp = 90f, maxDp = 400f)
        assertEquals(77f + 123f, longTitleFit.widthDp, 0.001f)
        assertEquals(90f, longTitleFit.titleMaxDp, 0.001f)
        assertEquals(23f, longTitleFit.centreShiftDp, 0.001f)

        assertEquals(18f, PulseWidth.splitRightFixedDp(true), 0.001f)
        assertEquals(0f, PulseWidth.splitRightFixedDp(false), 0.001f)
        // the glyph's 22 dp and its gaps are on the left (15 + 8 + 22 + 6 = 51), the right keeps the settled pill's 20
        val glyphOnly = PulseWidth.fitSplit(40f, 30f, messageDp = 0f, titleDp = 0f, maxDp = 400f)
        assertEquals(15f + 8f + 22f + 6f + 20f, glyphOnly.widthDp, 0.001f)

        val noTitleFit = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 0f, maxDp = 400f)
        assertEquals(157f + 60f, noTitleFit.widthDp, 0.001f)

        val remainingFit = PulseWidth.fitSplit(120f, 30f, messageDp = 20f, titleDp = 200f, maxDp = 300f)
        // reach 150 per side: the title gets 150 - 15 - 18
        assertEquals(150f - 15f - 18f, remainingFit.titleMaxDp, 0.001f)
    }

    @Test
    fun splitWidthLimits() {
        val cappedFit = PulseWidth.fitSplit(120f, 30f, messageDp = 300f, titleDp = 300f, maxDp = 250f)
        assertEquals(250f, cappedFit.widthDp, 0.001f)
        assertEquals(125f - 15f - 18f, cappedFit.titleMaxDp, 0.001f)
        assertEquals(0f, cappedFit.centreShiftDp, 0.001f)

        // lock screen: nothing to show beside the glyph
        val emptyFit = PulseWidth.fitSplit(120f, 30f, 0f, 0f, 400f)
        assertEquals(120f, emptyFit.widthDp, 0.001f)
        assertEquals(0f, emptyFit.centreShiftDp, 0.001f)

        val centredFit = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 20f, maxDp = 200f, shiftDp = 10f)
        assertEquals(110f + 53f, centredFit.widthDp, 0.001f)

        assertEquals(120f, PulseWidth.fitSplit(120f, 30f, 100f, 90f, maxDp = 80f).widthDp, 0.001f)

        val shortCapFit = PulseWidth.fitSplit(120f, 30f, 300f, 200f, PulseWidth.MaxWidth(40f, 30f))
        assertEquals(120f, shortCapFit.widthDp, 0.001f)
        assertEquals(0f, shortCapFit.centreShiftDp, 0.001f)
    }

    @Test
    fun splitPulseWidth() {
        // the hole 10 dp right of the pill's centre: each side is sized from the hole, the centre moves from there
        val fit = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 20f, maxDp = 400f, shiftDp = 10f)
        assertEquals(157f + 53f, fit.widthDp, 0.001f)
        assertEquals(-42f, fit.centreShiftDp, 0.001f)

        val ltr = PulseWidth.centredMaxWidth(360f, 180f, 0f, false)
        val rtl = PulseWidth.centredMaxWidth(360f, 180f, 0f, false, rtl = true)
        val inLtr = PulseWidth.fitSplit(112f, 40f, 1000f, 0f, ltr, 0f)
        val inRtl = PulseWidth.fitSplit(112f, 40f, 1000f, 0f, rtl, 0f)
        // a long message fills the left room, the right keeps the settled pill's 56: 120 + 56 in LTR (the clock's side
        // is on the left), 136 + 56 in RTL (the icons' side is, and its reserve is the shorter one)
        assertEquals(176f, inLtr.widthDp, 0.001f)
        assertEquals(192f, inRtl.widthDp, 0.001f)

        assertEquals(
            PulseWidth.fitSplit(120f, 30f, 100f, 20f, 300f, 4f),
            PulseWidth.fitSplit(120f, 30f, 100f, 20f, PulseWidth.MaxWidth.symmetric(300f), 4f)
        )

        val plain = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 20f, maxDp = 400f)
        val icon = PulseWidth.fitSplit(120f, 30f, messageDp = 100f, titleDp = 20f, maxDp = 400f,
            mediaIconDp = PulseWidth.MEDIA_ICON_DP)
        assertEquals(18f, PulseWidth.MEDIA_ICON_DP, 0.001f)
        assertEquals(plain.widthDp + 18f, icon.widthDp, 0.001f)

        assertEquals(
            PulseWidth.fitSplit(120f, 30f, 100f, 20f, 400f),
            PulseWidth.fitSplit(120f, 30f, 100f, 20f, 400f, mediaIconDp = 0f)
        )
    }

    @Test
    fun splitMessageBudget() {
        // start padding 8, glyph 22, gap 6, gap before the hole 6
        assertEquals(100f - 42f, PulseWidth.splitMessageMaxDp(100f), 0.001f)
        assertEquals(0f, PulseWidth.splitMessageMaxDp(10f), 0.001f)

        assertEquals(PulseWidth.splitMessageMaxDp(100f) - 18f, PulseWidth.splitMessageMaxDp(100f, 18f), 0.001f)
    }

    @Test
    fun sideContentFit() {
        val noValueFit = PulseWidth.fitSide(60f, 30f, titleDp = 40f, valueDp = 0f, maxDp = 400f)
        assertEquals(30f + 22f + 6f + 40f + 12f, noValueFit.widthDp, 0.001f)

        val shortContentFit = PulseWidth.fitSide(baseDp = 120f, glyphFromCameraDp = 30f, titleDp = 10f, valueDp = 10f, maxDp = 300f)
        assertEquals(120f, shortContentFit.widthDp, 0.001f)

        val longTitleFit = PulseWidth.fitSide(120f, glyphFromCameraDp = 30f, titleDp = 90f, valueDp = 20f, maxDp = 400f)
        // 30 + 22 + 6 + 90 + 14 + 20 + 12
        assertEquals(194f, longTitleFit.widthDp, 0.001f)
        assertEquals(90f, longTitleFit.titleMaxDp, 0.001f)

        val noTitleFit = PulseWidth.fitSide(60f, 30f, titleDp = 0f, valueDp = 20f, maxDp = 400f)
        assertEquals(30f + 22f + 14f + 20f + 12f, noTitleFit.widthDp, 0.001f)

        val plain = PulseWidth.fitSide(60f, 30f, titleDp = 10f, valueDp = 14f, maxDp = 400f)
        val song = PulseWidth.fitSide(60f, 30f, 10f, PulseWidth.valueSideDp(14f, 40f), 400f)
        assertEquals(40f + PulseWidth.ARTIST_GAP_DP, song.widthDp - plain.widthDp, 0.001f)
    }

    @Test
    fun sidePulseWidth() {
        val cappedFit = PulseWidth.fitSide(120f, 30f, titleDp = 400f, valueDp = 40f, maxDp = 200f)
        assertEquals(200f, cappedFit.widthDp, 0.001f)
        assertEquals(200f - 58f - 66f, cappedFit.titleMaxDp, 0.001f)

        val withoutMessage = PulseWidth.fitSide(120f, 8f, titleDp = 40f, valueDp = 0f, maxDp = 400f)
        val withMessage = PulseWidth.fitSide(120f, 8f, titleDp = 40f, valueDp = 0f, maxDp = 400f, messageDp = 90f)
        // 8 + 22 + 6 + 40 + 6 + 90 + 12
        assertEquals(184f, withMessage.widthDp, 0.001f)
        assertTrue(withMessage.widthDp > withoutMessage.widthDp)

        val imageOnlyFit = PulseWidth.fitSide(60f, 8f, titleDp = 0f, valueDp = 0f, maxDp = 400f, messageDp = 50f, mediaIconDp = 18f)
        // 8 + 22 + 6 + 18 + 50 + 12
        assertEquals(116f, imageOnlyFit.widthDp, 0.001f)
    }

    @Test
    fun sideWidthBudget() {
        assertEquals(360f - 72f - 100f, PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 100f, 112f), 0.001f)
        assertEquals(100f + 112f - 72f, PulseWidth.sideMaxWidthDp(CameraSide.Right, 360f, 100f, 112f), 0.001f)
        assertEquals(0f, PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 300f, 112f), 0.001f)
        assertEquals(360f - 72f - 100f - 42f, PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 100f, 112f, 42f, false), 0.001f)
        assertEquals(100f + 112f - 72f - 42f, PulseWidth.sideMaxWidthDp(CameraSide.Right, 360f, 100f, 112f, 42f, true), 0.001f)

        // pill 112 wide at left 124 is centred on 180: the same room as the centred rule
        assertEquals(
            PulseWidth.centredMaxWidth(360f, 180f, 42f, true).symmetricWidthDp,
            PulseWidth.sideMaxWidthDp(CameraSide.Centre, 360f, 124f, 112f, 42f, true),
            0.001f
        )
        assertEquals(
            PulseWidth.centredMaxWidth(360f, 180f, 42f, true),
            PulseWidth.maxWidth(CameraSide.Centre, 360f, 124f, 112f, 42f, true)
        )

        val bare = PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 100f, 112f)
        assertEquals(bare, PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 100f, 112f, 42f, true), 0.001f)
        assertEquals(bare - 42f, PulseWidth.sideMaxWidthDp(CameraSide.Left, 360f, 100f, 112f, 42f, false), 0.001f)
        val bareRight = PulseWidth.sideMaxWidthDp(CameraSide.Right, 360f, 100f, 112f)
        assertEquals(bareRight, PulseWidth.sideMaxWidthDp(CameraSide.Right, 360f, 100f, 112f, 42f, false), 0.001f)
        assertEquals(bareRight - 42f, PulseWidth.sideMaxWidthDp(CameraSide.Right, 360f, 100f, 112f, 42f, true), 0.001f)
    }

    @Test
    fun centredWidthBudget() {
        // 360 wide, centred pill: left 180 - 60, right 360 - 180 - 44
        val ltrM = PulseWidth.centredMaxWidth(360f, 180f, 0f, false)
        assertEquals(120f, ltrM.leftDp, 0.001f)
        assertEquals(136f, ltrM.rightDp, 0.001f)
        assertEquals(256f, ltrM.widthDp, 0.001f)
        assertEquals(240f, ltrM.symmetricWidthDp, 0.001f)

        val near = PulseWidth.centredMaxWidth(360f, 100f, 0f, false)
        assertEquals(40f, near.leftDp, 0.001f)
        assertEquals(216f, near.rightDp, 0.001f)
        val edge = PulseWidth.centredMaxWidth(360f, 60f, 0f, false)
        assertEquals(0f, edge.leftDp, 0.001f)
        assertEquals(0f, PulseWidth.centredMaxWidth(360f, 30f, 0f, false).leftDp, 0.001f)

        assertEquals(PulseWidth.Reserves(60f, 44f), PulseWidth.edgeReserves(false))
        assertEquals(PulseWidth.Reserves(44f, 60f), PulseWidth.edgeReserves(true))
        // the clock is on the right now: left 180 - 44, right 360 - 180 - 60
        val rtlM = PulseWidth.centredMaxWidth(360f, 180f, 0f, false, rtl = true)
        assertEquals(136f, rtlM.leftDp, 0.001f)
        assertEquals(120f, rtlM.rightDp, 0.001f)
        assertEquals(256f, rtlM.widthDp, 0.001f)
        assertEquals(240f, rtlM.symmetricWidthDp, 0.001f)

        for (bubbleOnLeft in listOf(false, true)) {
            val mirroredRtl = PulseWidth.centredMaxWidth(360f, 100f, 42f, bubbleOnLeft, rtl = true)
            val ltr = PulseWidth.centredMaxWidth(360f, 260f, 42f, !bubbleOnLeft)
            assertEquals(ltr.rightDp, mirroredRtl.leftDp, 0.001f)
            assertEquals(ltr.leftDp, mirroredRtl.rightDp, 0.001f)
        }

        assertEquals(
            PulseWidth.centredMaxWidth(360f, 180f, 42f, true, rtl = true),
            PulseWidth.maxWidth(CameraSide.Centre, 360f, 124f, 112f, 42f, true, rtl = true)
        )
        // a side camera keeps 1 reserve on both screen sides, so there is nothing to swap
        assertEquals(
            PulseWidth.maxWidth(CameraSide.Left, 360f, 100f, 112f),
            PulseWidth.maxWidth(CameraSide.Left, 360f, 100f, 112f, rtl = true)
        )
    }

    @Test
    fun centredRoomTakesBubbleSpan() {
        // bubble 34 + 8: on the right the right room is 136 - 42 = 94, the left stays at 120
        val right = PulseWidth.centredMaxWidth(360f, 180f, 42f, false)
        assertEquals(120f, right.leftDp, 0.001f)
        assertEquals(94f, right.rightDp, 0.001f)
        // on the left the left room is 120 - 42 = 78, the right stays at 136
        val left = PulseWidth.centredMaxWidth(360f, 180f, 42f, true)
        assertEquals(78f, left.leftDp, 0.001f)
        assertEquals(136f, left.rightDp, 0.001f)
        // a bubble that takes more than the left room has leaves none, never a negative one
        assertEquals(0f, PulseWidth.centredMaxWidth(360f, 100f, 42f, true).leftDp, 0.001f)
    }

    @Test
    fun iconReserveShorterThanClock() {
        assertTrue(PulseWidth.EDGE_RESERVE_RIGHT_DP < PulseWidth.EDGE_RESERVE_LEFT_DP)
        // side cameras and the edge pill keep the full reserve
        assertTrue(PulseWidth.EDGE_RESERVE_RIGHT_DP < PulseWidth.EDGE_RESERVE_DP)
    }

    @Test
    fun asymmetricTextBudget() {
        val max = PulseWidth.centredMaxWidth(360f, 180f, 0f, false)
        // a value that asks for more than the room: the right edge stops at the battery's clearance, 44 dp from the end
        val long = PulseWidth.fitPerSide(112f, 40f, titleDp = 20f, valueDp = 1000f, max = max)
        assertEquals(360f - 44f, 180f + long.centreShiftDp + long.widthDp / 2f, 0.01f)
        // With no title, a short value leaves the pill at its settled reach.
        val short = PulseWidth.fitPerSide(112f, 40f, titleDp = 0f, valueDp = 10f, max = max)
        assertEquals(112f, short.widthDp, 0.001f)
        // A title still asks for its own room; the short value adds no further width.
        val titledShort = PulseWidth.fitPerSide(112f, 40f, titleDp = 20f, valueDp = 10f, max = max)
        assertEquals(132f, titledShort.widthDp, 0.001f)

        val fit = PulseWidth.fitPerSide(120f, 30f, titleDp = 5f, valueDp = 100f, maxDp = 400f)
        // right: 15 + 100 + 12 = 127, left stays at the settled 60
        assertEquals(187f, fit.widthDp, 0.001f)
        assertEquals(33.5f, fit.centreShiftDp, 0.001f)
        assertEquals(9f, fit.titleMaxDp, 0.001f)

        assertEquals(14f, PulseWidth.valueSideDp(14f, 0f, leadGapDp = 6f), 0.001f)
        val noArtistPlain = PulseWidth.fitPerSide(120f, 30f, titleDp = 80f, valueDp = 14f, maxDp = 400f)
        val grouped = PulseWidth.fitPerSide(120f, 30f, 80f, PulseWidth.valueSideDp(14f, 0f, 6f), 400f)
        assertEquals(noArtistPlain, grouped)

        // 6 before the artist, 100, 6 before the bars, 14: 126 on the right of a 30 dp hole, plus the end padding
        assertEquals(126f, PulseWidth.valueSideDp(14f, 100f, leadGapDp = 6f), 0.001f)
        val artistPlain = PulseWidth.fitPerSide(120f, 30f, titleDp = 5f, valueDp = 14f, maxDp = 400f)
        val song = PulseWidth.fitPerSide(120f, 30f, 5f, PulseWidth.valueSideDp(14f, 100f, 6f), 400f)
        assertEquals(60f + 15f + 126f + 12f, song.widthDp, 0.001f)
        assertEquals(artistPlain.titleMaxDp, song.titleMaxDp, 0.001f)
    }

    @Test
    fun symmetricMaxSharesEvenly() {
        val m = PulseWidth.MaxWidth.symmetric(200f)
        assertEquals(100f, m.leftDp, 0.001f)
        assertEquals(100f, m.rightDp, 0.001f)
        assertEquals(200f, m.widthDp, 0.001f)
        assertEquals(200f, m.symmetricWidthDp, 0.001f)
    }

    @Test
    fun maxWidthOfASideCameraIsSharedEvenly() {
        val m = PulseWidth.maxWidth(CameraSide.Left, 360f, 100f, 112f)
        assertEquals(360f - 72f - 100f, m.widthDp, 0.001f)
        assertEquals(m.leftDp, m.rightDp, 0.001f)
    }

    @Test
    fun splitLeftReachesFurtherThanTheRight() {
        // left may reach 150 from the settled centre, right 70: a long message takes the left cap, the right keeps 60
        val fit = PulseWidth.fitSplit(120f, 30f, 300f, 20f, PulseWidth.MaxWidth(150f, 70f))
        assertEquals(150f + 60f, fit.widthDp, 0.001f)
        assertEquals(-45f, fit.centreShiftDp, 0.001f)
    }

    @Test
    fun splitRightStopsAtItsOwnCap() {
        // right want: 15 + 6 + 12 + 200 = 233, capped at 100
        val fit = PulseWidth.fitSplit(120f, 30f, 20f, 200f, PulseWidth.MaxWidth(150f, 100f))
        assertEquals(77f + 100f, fit.widthDp, 0.001f)
        assertEquals(100f - 15f - 18f, fit.titleMaxDp, 0.001f)
    }

    @Test
    fun splitCapsFollowAHoleOffTheMiddle() {
        // the hole 10 right of the pill's centre: the left may reach 150 + 10 from the hole, the right 100 - 10
        val fit = PulseWidth.fitSplit(120f, 30f, 300f, 300f, PulseWidth.MaxWidth(150f, 100f), shiftDp = 10f)
        assertEquals(160f + 90f, fit.widthDp, 0.001f)
        assertEquals(10f + (90f - 160f) / 2f, fit.centreShiftDp, 0.001f)
    }

    @Test
    fun leftReachesFurtherThanRight() {
        // title and glyph on the left: 15 + 8 + 22 + 6 + 300 = 351, capped at 150; value right: 15 + 20 + 12 = 47 -> 60
        val fit = PulseWidth.fitPerSide(120f, 30f, 300f, 20f, PulseWidth.MaxWidth(150f, 70f))
        assertEquals(150f + 60f, fit.widthDp, 0.001f)
        assertEquals(-45f, fit.centreShiftDp, 0.001f)
    }


    @Test
    fun lineInsets() {
        assertEquals(14f, PulseWidth.lineInsetDp(34f, 22f, false, 3f, 14f), 0.001f)
        assertEquals(14f, PulseWidth.lineInsetDp(20f, 0f, true, 3f, 14f), 0.001f)

        // radius 34 on a 34 dp notch: at 3 dp the curve is about 20 dp in
        val inset = PulseWidth.lineInsetDp(34f, 34f, true, 3f, 14f)
        assertTrue(inset > 20f && inset < 25f)
    }


    @Test
    fun equivalentPulseWidensSame() {
        val value = PulseWidth.equivalentPulse(extraDp = 60f, baseDp = 120f, fraction = 0.125f, progress = 0.5f)
        assertEquals(120f * 0.125f * value, 30f, 0.001f)
        assertEquals(0f, PulseWidth.equivalentPulse(60f, 120f, 0.125f, -0.02f), 0.0001f)
        assertTrue(PulseWidth.equivalentPulse(60f, 120f, 0.125f, 1.015f) > 4f)
    }

    @Test
    fun aHoleOffTheMiddleGivesEachSideItsOwnRoom() {
        // 200 wide, hole 30 wide, 20 right of the middle: left 200 / 2 + 20 - 15 = 105, right 200 / 2 - 20 - 15 = 65
        val rooms = PulseWidth.rooms(200f, 30f, 20f)
        assertEquals(105f, rooms.leftDp, 0.001f)
        assertEquals(65f, rooms.rightDp, 0.001f)
    }

    @Test
    fun theShiftIsTheHolesCentreLessThePillsMiddle() {
        assertEquals(20f, PulseWidth.cameraShiftDp(80f, 120f), 0.001f)
        assertEquals(-12f, PulseWidth.cameraShiftDp(48f, 120f), 0.001f)
        assertEquals(0f, PulseWidth.cameraShiftDp(null, 120f), 0.001f)
    }

    @Test
    fun theGlyphShowsOnlyOnceItsRoomClearsTheHole() {
        // glyph needs 8 + 22 = 30 left of the hole
        assertEquals(0f, PulseWidth.revealAlpha(18f, 30f), 0.001f)
        assertEquals(0f, PulseWidth.revealAlpha(22f, 30f), 0.001f)
        assertEquals(0.5f, PulseWidth.revealAlpha(26f, 30f), 0.001f)
        assertEquals(1f, PulseWidth.revealAlpha(30f, 30f), 0.001f)
        assertEquals(1f, PulseWidth.revealAlpha(80f, 30f), 0.001f)
    }

    @Test
    fun asymmetricWidthBudget() {
        val shortContentFit = PulseWidth.fitPerSide(baseDp = 120f, cameraGapDp = 30f, titleDp = 5f, valueDp = 10f, maxDp = 300f)
        assertEquals(120f, shortContentFit.widthDp, 0.001f)
        assertEquals(0f, shortContentFit.centreShiftDp, 0.001f)

        val offsetFit = PulseWidth.fitPerSide(120f, 30f, titleDp = 80f, valueDp = 20f, maxDp = 400f, shiftDp = 10f)
        // the settled pill reaches 70 left and 50 right of the hole's centre, the title asks 131 on the left
        assertEquals(181f, offsetFit.widthDp, 0.001f)
        assertEquals(-30.5f, offsetFit.centreShiftDp, 0.001f)

        val centredFit = PulseWidth.fitPerSide(120f, 30f, titleDp = 300f, valueDp = 20f, maxDp = 250f)
        // the left edge stops at 125 from the settled centre, the right keeps the settled 60
        assertEquals(185f, centredFit.widthDp, 0.001f)
        assertEquals(74f, centredFit.titleMaxDp, 0.001f)
        assertEquals(-32.5f, centredFit.centreShiftDp, 0.001f)

        assertEquals(120f, PulseWidth.fitPerSide(120f, 30f, 100f, 90f, maxDp = 80f).widthDp, 0.001f)

        val noTitleFit = PulseWidth.fitPerSide(40f, 30f, titleDp = 0f, valueDp = 0f, maxDp = 400f)
        // 15 + 8 + 22 = 45 on the left, 15 + 12 = 27 on the right, both above the settled 20
        assertEquals(72f, noTitleFit.widthDp, 0.001f)
    }

    @Test
    fun aLongArtistStopsAtTheRoomOfTheValueSide() {
        val fit = PulseWidth.fitPerSide(120f, 30f, titleDp = 5f, valueDp = PulseWidth.valueSideDp(14f, 300f, 6f),
            max = PulseWidth.MaxWidth(150f, 70f))
        assertEquals(60f + 70f, fit.widthDp, 0.001f)
        assertEquals(9f, fit.titleMaxDp, 0.001f)
    }

    @Test
    fun theArtistGetsWhatTheValueSideLeavesAfterTheBars() {
        assertEquals(74f, PulseWidth.artistMaxDp(100f, 14f, leadGapDp = 6f), 0.001f)
        assertEquals(0f, PulseWidth.artistMaxDp(20f, 14f, leadGapDp = 6f), 0.001f)
    }

    @Test
    fun aFitChangeIsFollowedOnlyOnceThePillIsOut() {
        assertFalse(PulseWidth.followsFitChange(0f))
        assertFalse(PulseWidth.followsFitChange(0.6f))
        assertTrue(PulseWidth.followsFitChange(0.97f))
        assertTrue(PulseWidth.followsFitChange(1.015f))
    }

    @Test
    fun theClassicPulseTakesTheMessageToo() {
        val fit = PulseWidth.fitClassic(120f, 0f, titleDp = 40f, valueDp = 0f, maxDp = 400f, messageDp = 90f)
        assertEquals(184f, fit.widthDp, 0.001f)
    }

    @Test
    fun sideTextAllocation() {
        val bothRooms = PulseWidth.sideTextRooms(200f, 40f, 90f)
        assertEquals(40f, bothRooms.titleDp, 0.001f)
        assertEquals(154f, bothRooms.messageDp, 0.001f)

        val shortTitle = PulseWidth.sideTextRooms(100f, 30f, 300f)
        assertEquals(30f, shortTitle.titleDp, 0.001f)
        assertEquals(64f, shortTitle.messageDp, 0.001f)
        val longTitle = PulseWidth.sideTextRooms(100f, 200f, 300f)
        assertEquals(40f, longTitle.titleDp, 0.001f)
        assertEquals(54f, longTitle.messageDp, 0.001f)

        val titleOnlyRooms = PulseWidth.sideTextRooms(80f, 30f, 0f)
        assertEquals(80f, titleOnlyRooms.titleDp, 0.001f)
        assertEquals(0f, titleOnlyRooms.messageDp, 0.001f)
    }
}
