/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.CutoutRect
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.IdleDotTap
import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationSnap
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.RotationMorph
import com.agupta07505.smartisland.data.RotationMorphTracker
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.data.idleDot
import com.agupta07505.smartisland.data.idleDotTapAction
import com.agupta07505.smartisland.data.shouldShowIdleDot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeClassicPulseTest {
    private val band = LandscapeClassicPulse.CENTRE_BAND_DP

    @Test
    fun centreBandSplitsPillLayouts() {
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.sideOf(0f))
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.sideOf(band))
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.sideOf(-band))
        assertEquals(CameraSide.Left, LandscapeClassicPulse.sideOf(-band - 1f))
        assertEquals(CameraSide.Right, LandscapeClassicPulse.sideOf(band + 1f))
        assertEquals(CameraSide.Left, LandscapeClassicPulse.sideOf(-300f))
        assertEquals(CameraSide.Right, LandscapeClassicPulse.sideOf(300f))
    }

    @Test
    fun theSideIsHeldWhileAPulseRuns() {
        // a pulse that started on the left keeps it when the pill moves to the right or the centre
        assertEquals(CameraSide.Left, LandscapeClassicPulse.heldSide(CameraSide.Right, CameraSide.Left, true))
        assertEquals(CameraSide.Left, LandscapeClassicPulse.heldSide(CameraSide.Centre, CameraSide.Left, true))
        // the first frame of a pulse has nothing held yet and takes the live side
        assertEquals(CameraSide.Right, LandscapeClassicPulse.heldSide(CameraSide.Right, null, true))
        // between pulses the live side applies
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.heldSide(CameraSide.Centre, CameraSide.Left, false))
    }

    @Test
    fun classicPulseAnchor() {
        assertEquals(CameraSide.Left, LandscapeClassicPulse.anchorSide(true, CameraSide.Left, CameraSide.Centre))
        assertEquals(CameraSide.Right, LandscapeClassicPulse.anchorSide(true, CameraSide.Right, CameraSide.Centre))
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.anchorSide(true, CameraSide.Centre, CameraSide.Centre))
        assertEquals(CameraSide.Left, LandscapeClassicPulse.anchorSide(true, CameraSide.Centre, CameraSide.Left))
        assertEquals(CameraSide.Centre, LandscapeClassicPulse.anchorSide(false, CameraSide.Left, CameraSide.Centre))

        val side = LandscapeClassicPulse.anchorSide(true, CameraSide.Left, CameraSide.Centre)
        assertEquals(40f, PulseBubble.shiftDp(40f, side, bubbleOnLeft = false), 0.001f)
        val centred = LandscapeClassicPulse.anchorSide(true, CameraSide.Centre, CameraSide.Centre)
        assertEquals(20f, PulseBubble.shiftDp(40f, centred, bubbleOnLeft = false), 0.001f)
    }



    @Test
    fun centreContentGap() {
        // 120 - 8 - 22 - 30 - 12
        assertEquals(48f, LandscapeClassicPulse.centreGapDp(120f, 30f), 0.001f)

        assertEquals(0f, LandscapeClassicPulse.centreGapDp(60f, 40f), 0.001f)

        val noTitleGap = LandscapeClassicPulse.centreGapDp(120f, 30f)
        val noTitleFit = PulseWidth.fitClassic(baseDp = 120f, gapDp = noTitleGap, titleDp = 0f, valueDp = 30f, maxDp = 400f)
        assertEquals(120f, noTitleFit.widthDp, 0.001f)

        val withTitleGap = LandscapeClassicPulse.centreGapDp(120f, 30f)
        val withTitleFit = PulseWidth.fitClassic(120f, withTitleGap, titleDp = 80f, valueDp = 30f, maxDp = 400f)
        // 8 + 22 + 6 + 80 on the left, the 48 dp gap, then 30 + 12 for the value
        assertEquals(116f + 48f + 42f, withTitleFit.widthDp, 0.001f)
        assertEquals(80f, withTitleFit.titleMaxDp, 0.001f)

        val longValueGap = LandscapeClassicPulse.centreGapDp(120f, 20f)
        val classic = PulseWidth.fitClassic(120f, longValueGap, titleDp = 5f, valueDp = 100f, maxDp = 400f)
        val symmetric = PulseWidth.fit(120f, longValueGap, titleDp = 5f, valueDp = 100f, maxDp = 400f)
        assertTrue(classic.widthDp < symmetric.widthDp)
    }

    @Test
    fun widthStopsAtRoomTitleGetsRest() {
        val fit = PulseWidth.fitClassic(120f, gapDp = 48f, titleDp = 400f, valueDp = 30f, maxDp = 250f)
        assertEquals(250f, fit.widthDp, 0.001f)
        assertEquals(250f - 36f - 48f - 42f, fit.titleMaxDp, 0.001f)
    }
    @Test
    fun classicPulseOffset() {
        // screen 800, pill left edge 100, settled 120, status reserve on the far side
        val leftRoom = LandscapeClassicPulse.sideMaxWidthDp(CameraSide.Left, 800f, 100f, 120f)
        assertEquals(800f - PulseWidth.EDGE_RESERVE_DP - 100f, leftRoom, 0.001f)
        assertEquals(30f, LandscapeClassicPulse.centreShiftDp(CameraSide.Left, 120f, 180f), 0.001f)

        val rightRoom = LandscapeClassicPulse.sideMaxWidthDp(CameraSide.Right, 800f, 580f, 120f)
        assertEquals(580f + 120f - PulseWidth.EDGE_RESERVE_DP, rightRoom, 0.001f)
        assertEquals(-30f, LandscapeClassicPulse.centreShiftDp(CameraSide.Right, 120f, 180f), 0.001f)

        assertEquals(0f, LandscapeClassicPulse.centreShiftDp(CameraSide.Centre, 120f, 220f), 0.001f)
    }

    @Test
    fun theBubbleSpanComesOffTheRoom() {
        val without = LandscapeClassicPulse.sideMaxWidthDp(CameraSide.Left, 800f, 100f, 120f)
        val with = LandscapeClassicPulse.sideMaxWidthDp(CameraSide.Left, 800f, 100f, 120f, bubbleSpanDp = 40f)
        assertEquals(40f, without - with, 0.001f)
    }

    @Test
    fun sidePulseUsesOuterGlyphStart() {
        val fit = PulseWidth.fitSide(
            120f, LandscapeClassicPulse.OUTER_GLYPH_START_DP, titleDp = 60f, valueDp = 30f, maxDp = 400f
        )
        // 8 + 22 + 6 + 60 + pair gap + 30 + 12
        assertTrue(fit.widthDp > 120f)
        assertEquals(60f, fit.titleMaxDp, 0.001f)
    }

    @Test
    fun screenWidthCap() {
        val wideCapped = LandscapeClassicPulse.capped(PulseWidth.MaxWidth(400f, 360f), 800f)
        assertEquals(400f, wideCapped.widthDp, 0.001f)
        assertEquals(400f * 400f / 760f, wideCapped.leftDp, 0.001f)

        val room = PulseWidth.MaxWidth(150f, 120f)
        assertEquals(room, LandscapeClassicPulse.capped(room, 800f))
    }
}

class LandscapeIdleDotTest {

    private val settings = SmartIslandSettings(landscapeCircleSize = 26f)
    private val cameraState = OrientationState(
        isLandscape = true,
        screenWidthDp = 800f,
        screenHeightDp = 360f,
        camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 20f)
    )

    @Test
    fun idleDotGeometry() {
        // the pill's own glyph-free centre is the control, see PortraitPillTapTest
        val portrait = SmartIslandSettings(width = 120f, height = 34f, xOffset = 12f, yOffset = 9f, opacity = 0.9f)
        assertNull(idleDot(portrait, OrientationState(isLandscape = false)))

        assertNull(idleDot(SmartIslandSettings(enableNotchMode = true), OrientationState(isLandscape = false)))

        val roundDot = idleDot(settings, cameraState)!!
        assertEquals(roundDot.sizeDp, roundDot.widthDp, 0.001f)

        val cameraDot = idleDot(settings, cameraState)
        assertNotNull(cameraDot)
        assertEquals(-350f, cameraDot!!.centerXDp, 0.01f)
        assertEquals(7f, cameraDot.topDp, 0.01f)
        assertEquals(26f, cameraDot.sizeDp, 0.01f)
        assertEquals(33f, cameraDot.bottomDp, 0.01f)

        run {
            val idle = settings.forOrientation(cameraState, hasContent = false)
            val dot = idleDot(settings, cameraState)!!
            assertEquals(idle.xOffset, dot.centerXDp, 0.001f)
            assertEquals(idle.yOffset, dot.topDp, 0.001f)
            assertEquals(idle.opacity, dot.opacity, 0.001f)
        }
    }

    @Test
    fun idleDotVisibility() {
        val portraitDot = IdleDotGeometry(centerXDp = 0f, topDp = 0f, sizeDp = 34f, widthDp = 120f)
        assertFalse(shouldShowIdleDot(portraitDot, hasCards = true, isLandscape = false))
        assertFalse(shouldShowIdleDot(portraitDot, hasCards = false, isLandscape = false))

        assertTrue(shouldShowIdleDot(dot, hasCards = true, isLandscape = true))
        assertFalse(shouldShowIdleDot(dot, false))
        assertFalse(shouldShowIdleDot(null, true))
    }

    private val dot = IdleDotGeometry(centerXDp = -350f, topDp = 7f, sizeDp = 26f)

    @Test
    fun idleDotActions() {
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(hasCards = true, hasIdleStoredCards = false))
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(hasCards = true, hasIdleStoredCards = true))
        assertEquals(IdleDotTap.RevealStored, idleDotTapAction(hasCards = false, hasIdleStoredCards = true))
        assertEquals(IdleDotTap.None, idleDotTapAction(hasCards = false, hasIdleStoredCards = false))
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(true, false, emptyPillAvailable = true, emptyPillShown = true))
        assertEquals(IdleDotTap.RevealStored, idleDotTapAction(false, true, emptyPillAvailable = true))

        assertEquals(
            IdleDotTap.ShowEmptyPill,
            idleDotTapAction(hasCards = false, hasIdleStoredCards = false, emptyPillAvailable = true)
        )

        assertEquals(
            IdleDotTap.HideEmptyPill,
            idleDotTapAction(hasCards = false, hasIdleStoredCards = false, emptyPillAvailable = true, emptyPillShown = true)
        )
    }

}

class LandscapePillExitTest {
    private val screen = 800f

    @Test
    fun pillExitEdge() {
        assertEquals(PillExitDirection.Up, pillExitDirection(0f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(-120f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(150f, screen))
        assertEquals(PillExitDirection.Left, pillExitDirection(-360f, screen))
        assertEquals(PillExitDirection.Right, pillExitDirection(360f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(-300f, 0f))

        // centre 200 dp from the left edge: exactly on the border, still counts as near
        assertEquals(PillExitDirection.Left, pillExitDirection(-200f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(-199f, screen))
        assertEquals(PillExitDirection.Right, pillExitDirection(200f, screen))
        assertEquals(PillExitDirection.Up, pillExitDirection(199f, screen))
    }



    @Test
    fun travelPastEdgeCapped() {
        // pill 24 wide, centre 20 dp from the left edge: 20 + 12 + 8 margin = 40
        val left = pillExitTravelDp(PillExitDirection.Left, -380f, 24f, 60f, 10f, screen)
        assertEquals(40f, left, 0.001f)
        val right = pillExitTravelDp(PillExitDirection.Right, 380f, 24f, 60f, 10f, screen)
        assertEquals(40f, right, 0.001f)
        val up = pillExitTravelDp(PillExitDirection.Up, 0f, 120f, 36f, 0f, screen)
        assertEquals(44f, up, 0.001f)
        val tall = pillExitTravelDp(PillExitDirection.Up, 0f, 120f, 36f, 300f, screen)
        assertEquals(PILL_EXIT_MAX_TRAVEL_DP, tall, 0.001f)
    }

    @Test
    fun exitOffsetUsesClampedProgressInTheExitDirection() {
        assertEquals(PillExitOffset(0f, -50f), pillExitOffsetDp(PillExitDirection.Up, 100f, 0.5f))
        assertEquals(PillExitOffset(-50f, 0f), pillExitOffsetDp(PillExitDirection.Left, 100f, 0.5f))
        assertEquals(PillExitOffset(50f, 0f), pillExitOffsetDp(PillExitDirection.Right, 100f, 0.5f))
        assertEquals(PillExitOffset(0f, 0f).dy, pillExitOffsetDp(PillExitDirection.Up, 100f, 0f).dy + 0f, 0.0001f)
        assertEquals(-100f, pillExitOffsetDp(PillExitDirection.Left, 100f, 3f).dx, 0.001f)
        assertEquals(0f, pillExitOffsetDp(PillExitDirection.Right, 100f, -1f).dx, 0.001f)
    }


    @Test
    fun leavingPillIsSolidThenFades() {
        assertEquals(1f, pillExitAlpha(0f), 0.0001f)
        assertEquals(1f, pillExitAlpha(0.3f), 0.0001f)
        assertTrue(pillExitAlpha(0.65f) in 0.01f..0.99f)
        assertEquals(0f, pillExitAlpha(1f), 0.0001f)
    }
}

class LandscapeWidgetPillTest {

    private fun wanted(
        landscape: Boolean = true,
        enabled: Boolean = true,
        ids: String = "clock",
        pillHidden: Boolean = false,
        shadeOpen: Boolean = false,
        suppressed: Boolean = false,
        tickHold: Boolean = false,
        emptyPill: Boolean = false
    ) = landscapeWidgetPillWanted(landscape, enabled, ids, pillHidden, shadeOpen, suppressed, tickHold, emptyPill)

    @Test
    fun idleWidgetAvailability() {
        assertTrue(wanted(enabled = false, ids = "", emptyPill = true))
        assertTrue(wanted(pillHidden = true, emptyPill = true))
        assertFalse(wanted(landscape = false, emptyPill = true))
        assertFalse(wanted(shadeOpen = true, emptyPill = true))

        assertFalse(wanted(pillHidden = true))
        assertTrue(wanted(pillHidden = true, tickHold = true))
        assertTrue(wanted(suppressed = true, tickHold = true))

        assertFalse(wanted(ids = ""))
        assertFalse(wanted(ids = "nonsense,,"))

        assertFalse(wanted(pillHidden = true))
        assertFalse(wanted(shadeOpen = true))

        assertFalse(wanted(suppressed = true))
        assertFalse(wanted(ids = "clock,weather", suppressed = true))
    }

    @Test
    fun widgetPillEligibility() {
        assertFalse(wanted(landscape = false, tickHold = true))
        assertFalse(wanted(enabled = false, pillHidden = true, tickHold = true))
        assertFalse(wanted(shadeOpen = true, pillHidden = true, tickHold = true))

        assertTrue(wanted())
        assertTrue(wanted(ids = "clock,weather"))

        assertFalse(wanted(landscape = false))

        assertFalse(wanted(enabled = false))

        val held = IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = false, offAfterLongPress = true)
        assertFalse(wanted(suppressed = held))
    }

    @Test
    fun suppressionDefaultsOff() {
        assertTrue(landscapeWidgetPillWanted(true, true, "clock", false, false))
    }

    @Test
    fun tapCloseSuppression() {
        // "Off after tap to close" on: a tap that closed the island holds the widgets (and so the pill) off
        val held = IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false)
        assertFalse(wanted(suppressed = held))
        // the island opened again: the flag is cleared and the pill returns
        val cleared = IdleWidgetOff.suppressed(closedByTap = false, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false)
        assertTrue(wanted(suppressed = cleared))
        // switch off: the tap changes nothing
        val switchOff = IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = false, offAfterLongPress = false)
        assertTrue(wanted(suppressed = switchOff))
    }

    @Test
    fun widgetPillPlacement() {
        val suppressedPill = wanted(suppressed = true)
        val suppressedDot = IdleDotGeometry(centerXDp = -300f, topDp = 8f, sizeDp = 26f)
        assertFalse(pillOnIsland(hasCards = false, widgetPill = suppressedPill))
        // no separate dot either: with the pill gone the island's own idle circle is the cutout
        assertFalse(shouldShowIdleDot(suppressedDot, hasCards = pillOnIsland(false, suppressedPill), isLandscape = true))
        // A card still brings the pill and the dot
        assertTrue(pillOnIsland(hasCards = true, widgetPill = suppressedPill))

        assertTrue(pillOnIsland(hasCards = true, widgetPill = false))
        assertTrue(pillOnIsland(hasCards = false, widgetPill = true))
        assertTrue(pillOnIsland(hasCards = true, widgetPill = true))
        assertFalse(pillOnIsland(hasCards = false, widgetPill = false))

        val idleDot = IdleDotGeometry(centerXDp = -300f, topDp = 8f, sizeDp = 26f)
        // no card, widget pill on: the circle is the toggle beside the pill
        assertTrue(shouldShowIdleDot(idleDot, hasCards = pillOnIsland(false, true), isLandscape = true))
        // no card, no widget pill: the island's own idle circle is the dot, no separate one
        assertFalse(shouldShowIdleDot(idleDot, hasCards = pillOnIsland(false, false), isLandscape = true))
        assertEquals(true, shouldShowIdleDot(idleDot, hasCards = pillOnIsland(true, false), isLandscape = true))
    }
}

class OrientationSnapTest {
    @Test fun nothingSnapsBeforeTheFirstTurn() {
        val snap = OrientationSnap(windowMs = 700L)
        assertFalse(snap.observe(isLandscape = false, edgeCameraResolved = false, nowMs = 0L))
        assertFalse(snap.observe(isLandscape = false, edgeCameraResolved = false, nowMs = 5_000L))
    }

    @Test fun theConfigurationFlipOpensTheWindow() {
        val snap = OrientationSnap(windowMs = 700L)
        snap.observe(false, false, 1_000L)
        assertTrue(snap.observe(true, false, 2_000L))
    }

    @Test fun theOrientationReadArrivingAFrameLaterStillSnaps() {
        // the flag flipped first; the camera (and so the pill's new size) comes 2 frames after
        val snap = OrientationSnap(windowMs = 700L)
        snap.observe(false, false, 1_000L)
        assertTrue(snap.observe(true, false, 2_000L))
        assertTrue(snap.observe(true, false, 2_016L))
        assertTrue(snap.observe(true, true, 2_033L))
    }

    @Test fun readArrivingFirstSnapsToo() {
        val snap = OrientationSnap(windowMs = 700L)
        snap.observe(false, false, 1_000L)
        assertTrue(snap.observe(false, true, 2_000L))
        assertTrue(snap.observe(true, true, 2_040L))
    }

    @Test fun theWindowClosesAfterItsLength() {
        val snap = OrientationSnap(windowMs = 700L)
        snap.observe(false, false, 1_000L)
        assertTrue(snap.observe(true, true, 2_000L))
        assertTrue(snap.observe(true, true, 2_699L))
        assertFalse(snap.observe(true, true, 2_700L))
        assertFalse(snap.observe(true, true, 9_000L))
    }

    @Test fun aTurnBackOpensItAgain() {
        val snap = OrientationSnap(windowMs = 700L)
        snap.observe(false, false, 0L)
        snap.observe(true, true, 100L)
        assertFalse(snap.observe(true, true, 5_000L))
        assertTrue(snap.observe(false, false, 6_000L))
    }

    @Test fun defaultWindowIsTheMorphSettleWindow() {
        val snap = OrientationSnap()
        snap.observe(false, false, 0L)
        snap.observe(true, true, 10L)
        assertTrue(snap.observe(true, true, 10L + RotationMorph.SETTLE_WINDOW_MS - 1L))
        assertFalse(snap.observe(true, true, 10L + RotationMorph.SETTLE_WINDOW_MS))
    }
}

class RotationMorphTest {
    private val portrait = OrientationState(isLandscape = false, screenWidthDp = 360f)
    private val landscape = OrientationState(
        isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f,
        camera = LandscapeCamera(xOffsetDp = -380f, centerYDp = 180f)
    )
    private val reversed = landscape.copy(reversed = true, camera = LandscapeCamera(xOffsetDp = 380f, centerYDp = 180f))

    private fun rect(l: Float, t: Float, w: Float, h: Float) = CutoutRect(l, t, w, h, 10f, 1f)

    @Test fun rotationTurnsOnlyForKnownScreens() {
        assertEquals(1, RotationMorph.quarterTurns(portrait, landscape))
        assertEquals(3, RotationMorph.quarterTurns(portrait, reversed))
        assertEquals(3, RotationMorph.quarterTurns(landscape, portrait))
        assertEquals(1, RotationMorph.quarterTurns(reversed, portrait))
        assertEquals(2, RotationMorph.quarterTurns(landscape, reversed))
        assertEquals(0, RotationMorph.quarterTurns(landscape, landscape))
        assertFalse(RotationMorph.turned(OrientationState(), landscape))
        assertFalse(RotationMorph.turned(portrait, portrait))
        assertTrue(RotationMorph.turned(portrait, landscape))
    }

    @Test fun portraitTopCentreGoesToTheLeftEdgeMiddleInLandscape() {
        // 360 x 800 portrait, a 120 x 30 pill at the top centre
        val r = RotationMorph.rotate(rect(120f, 0f, 120f, 30f), 360f, 800f, 1)
        assertEquals(0f, r.left, 0.001f)
        assertEquals(30f, r.width, 0.001f)
        assertEquals(120f, r.height, 0.001f)
        assertEquals(180f - 60f, r.top, 0.001f)
    }

    @Test fun reverseLandscapeAnchor() {
        val r = RotationMorph.rotate(rect(120f, 0f, 120f, 30f), 360f, 800f, 3)
        assertEquals(800f, r.right, 0.001f)
        assertEquals(30f, r.width, 0.001f)
        assertEquals(120f, r.height, 0.001f)
        assertEquals(120f, r.top, 0.001f)
    }

    @Test fun turningBackLandsOnTheTopCentre() {
        val there = RotationMorph.rotate(rect(120f, 0f, 120f, 30f), 360f, 800f, 1)
        val back = RotationMorph.rotate(there, 800f, 360f, 3)
        assertEquals(120f, back.left, 0.001f)
        assertEquals(0f, back.top, 0.001f)
        assertEquals(120f, back.width, 0.001f)
        assertEquals(30f, back.height, 0.001f)
    }

    @Test fun halfTurnMirrorsPlace() {
        val r = RotationMorph.rotate(rect(10f, 20f, 30f, 40f), 800f, 360f, 2)
        assertEquals(760f, r.left, 0.001f)
        assertEquals(300f, r.top, 0.001f)
        assertEquals(30f, r.width, 0.001f)
        assertEquals(40f, r.height, 0.001f)
    }

    @Test fun oldPortraitFrameUsesLongSide() {
        assertEquals(360f to 800f, RotationMorph.oldFrame(portrait, landscape))
        assertEquals(800f to 360f, RotationMorph.oldFrame(landscape, portrait))
    }

    @Test fun lerpEndsAreTheRects() {
        val a = rect(0f, 0f, 100f, 20f).copy(radius = 10f)
        val b = rect(50f, 50f, 20f, 100f).copy(radius = 30f, opacity = 0.5f)
        assertEquals(a, RotationMorph.lerp(a, b, 0f))
        assertEquals(b.copy(radius = 10f), RotationMorph.lerp(a, b, 1f))
        val mid = RotationMorph.lerp(a, b, 0.5f)
        assertTrue(mid.radius <= minOf(mid.width, mid.height) / 2f)
        assertEquals(0.75f, mid.opacity, 0.001f)
    }

    @Test
    fun rotatedPillBounds() {
        val portraitRaw = SmartIslandSettings(width = 120f, height = 30f, xOffset = 0f, yOffset = 4f)
        val portraitR = RotationMorph.collapsedRect(portraitRaw, portrait, hasContent = false, hasCards = false, edgeShown = false)
        assertNotNull(portraitR)
        assertEquals(120f, portraitR!!.width, 0.001f)
        assertEquals(120f, portraitR.left, 0.001f)
        assertEquals(4f, portraitR.top, 0.001f)

        // centre camera, pill pushed to the left side, circle on the left
        val companionRaw = SmartIslandSettings(xOffset = -170f, circlePosition = SmartIslandSettings.CIRCLE_POSITION_LEFT)
        val companionPortrait = OrientationState(isLandscape = false, screenWidthDp = 400f, screenHeightDp = 800f)
        val alone = RotationMorph.collapsedRect(companionRaw, companionPortrait, false, false, false)!!
        val withCircle = RotationMorph.collapsedRect(companionRaw, companionPortrait, false, false, false, hasCompanion = true)!!
        assertEquals(8f, alone.left, 0.01f)
        assertEquals(8f + minOf(companionRaw.width, companionRaw.height) + 8f, withCircle.left, 0.01f)

        assertNull(RotationMorph.collapsedRect(SmartIslandSettings(), OrientationState(), false, false, false))

        val edgeRaw = SmartIslandSettings(landscapeLayoutStyle = LandscapeLayoutStyle.EDGE)
        val edge = RotationMorph.collapsedRect(edgeRaw, landscape, hasContent = true, hasCards = true, edgeShown = true)
        assertNotNull(edge)
        assertTrue(edge!!.height > edge.width)
        assertTrue(edge.left <= 1f)

        val missingRaw = SmartIslandSettings()
        assertNull(RotationMorph.collapsedRect(missingRaw, portrait, false, false, false, drawn = false))
    }

    @Test
    fun notchMorphBounds() {
        val sideRaw = SmartIslandSettings()
        val sidePortrait = OrientationState(isLandscape = false, screenWidthDp = 400f, screenHeightDp = 800f)
        val area = CutoutArea(centreXDp = -150f, topDp = 8f, widthDp = 20f, heightDp = 20f)
        val centred = RotationMorph.collapsedRect(sideRaw, sidePortrait, false, false, false)!!
        val side = RotationMorph.collapsedRect(sideRaw, sidePortrait, false, false, false, cameraArea = area)!!
        assertTrue(side.left + side.width / 2f < 200f - 60f)
        assertTrue(side.left + side.width / 2f < centred.left + centred.width / 2f)

        val notchRaw = SmartIslandSettings(enableNotchMode = true, width = 120f, height = 30f, yOffset = 4f)
        val notch = RotationMorph.collapsedRect(notchRaw, portrait, hasContent = false, hasCards = false, edgeShown = false)!!
        assertEquals(1f, notch.flatTop, 0f)
        assertEquals(0f, notch.top, 0f)
        val compact = RotationMorph.collapsedRect(notchRaw.copy(enableNotchMode = false), portrait, false, false, false)!!
        assertEquals(0f, compact.flatTop, 0f)

        val tuning = EdgePillTuning(flatEdge = true)
        val edgeRaw = SmartIslandSettings(landscapeLayoutStyle = LandscapeLayoutStyle.EDGE, edgePill = tuning)
        val edge = RotationMorph.collapsedRect(edgeRaw, landscape, hasContent = true, hasCards = true, edgeShown = true)!!
        assertEquals(1f, edge.flatLeft, 0f)
        assertEquals(0f, edge.flatRight, 0f)
        val off = RotationMorph.collapsedRect(edgeRaw.copy(edgePill = EdgePillTuning(flatEdge = false)), landscape, true, true, true)!!
        assertEquals(0f, off.flatLeft, 0f)
    }

    @Test fun trackerStartsAMorphOnATurnWithTheOldRectCarriedRound() {
        val t = RotationMorphTracker()
        t.observe(portrait, rect(120f, 0f, 120f, 30f), enabled = true, nowMs = 0L)
        assertEquals(0, t.token)
        t.observe(landscape, rect(0f, 100f, 30f, 150f), enabled = true, nowMs = 1_000L)
        assertEquals(1, t.token)
        val from = t.from!!
        assertEquals(30f, from.width, 0.001f)
        assertEquals(120f, from.height, 0.001f)
        // still in landscape: no new morph
        t.observe(landscape, rect(0f, 100f, 30f, 150f), enabled = true, nowMs = 1_100L)
        assertEquals(1, t.token)
    }

    @Test fun trackerIgnoresSecondTurn() {
        val t = RotationMorphTracker()
        t.observe(portrait, rect(120f, 0f, 120f, 30f), true, 0L)
        t.observe(landscape, rect(0f, 100f, 30f, 150f), true, 1_000L)
        // a late refresh flips the side: the same turn settling, not a new morph
        t.observe(reversed, rect(770f, 100f, 30f, 150f), true, 1_200L)
        assertEquals(1, t.token)
        // a real turn later is a new morph
        t.observe(portrait, rect(120f, 0f, 120f, 30f), true, 5_000L)
        assertEquals(2, t.token)
    }

    @Test fun trackerQuietWithEdgePillOff() {
        val t = RotationMorphTracker()
        t.observe(portrait, rect(120f, 0f, 120f, 30f), enabled = false, nowMs = 0L)
        t.observe(landscape, rect(0f, 100f, 30f, 150f), enabled = false, nowMs = 1_000L)
        assertEquals(0, t.token)
        assertNull(t.from)
    }

    @Test fun flatTopTurnsIntoFlatSide() {
        val top = rect(120f, 0f, 120f, 30f).copy(flatTop = 1f)
        val left = RotationMorph.rotate(top, 360f, 800f, 1)
        assertEquals(1f, left.flatLeft, 0f)
        assertEquals(0f, left.flatTop, 0f)
        val right = RotationMorph.rotate(top, 360f, 800f, 3)
        assertEquals(1f, right.flatRight, 0f)
        assertEquals(0f, right.flatLeft, 0f)
        val back = RotationMorph.rotate(left, 800f, 360f, 3)
        assertEquals(1f, back.flatTop, 0f)
        assertEquals(0f, back.flatLeft, 0f)
        assertEquals(1f, RotationMorph.rotate(top, 360f, 800f, 2).flatBottom, 0f)
    }

    @Test fun flatSidesSquareCorners() {
        val flat = rect(0f, 0f, 100f, 30f).copy(radius = 10f, flatTop = 1f)
        assertEquals(0f, flat.radiusTopLeft, 0f)
        assertEquals(0f, flat.radiusTopRight, 0f)
        assertEquals(10f, flat.radiusBottomLeft, 0f)
        assertEquals(10f, flat.radiusBottomRight, 0f)
        val round = rect(0f, 0f, 100f, 30f).copy(radius = 10f)
        assertEquals(5f, RotationMorph.lerp(flat, round, 0.5f).radiusTopLeft, 0.001f)
    }

    @Test fun trackerDoesNotCarryAStaleRectWhenNothingWasDrawn() {
        val t = RotationMorphTracker()
        t.observe(portrait, rect(120f, 0f, 120f, 30f), enabled = true, nowMs = 0L)
        t.observe(portrait, null, enabled = true, nowMs = 100L)
        t.observe(landscape, rect(0f, 100f, 30f, 150f), enabled = true, nowMs = 1_000L)
        assertEquals(0, t.token)
        assertNull(t.from)
    }

    @Test fun handOverKeepsCoverage() {
        for (opacity in listOf(1f, 0.8f, 0.5f)) {
            for (i in 0..10) {
                val h = i / 10f
                val covered = 1f - (1f - RotationMorph.surfaceAlpha(opacity, h)) * (1f - h * opacity)
                if (h < 1f) assertEquals(opacity, covered, 0.001f)
            }
            assertEquals(0f, RotationMorph.surfaceAlpha(opacity, 1f), 0f)
        }
    }

    @Test fun lerpInPlaceHitsTarget() {
        val a = rect(100f, 0f, 30f, 120f)
        val b = rect(300f, 200f, 40f, 40f)
        val end = RotationMorph.lerpInPlace(a, b, 1f)
        assertEquals(b.left, end.left, 0.001f)
        assertEquals(b.top, end.top, 0.001f)
        assertEquals(b.width, end.width, 0.001f)
        assertEquals(b.height, end.height, 0.001f)
        val start = RotationMorph.lerpInPlace(a, b, 0f)
        assertEquals(a.left, start.left, 0.001f)
        assertEquals(a.top, start.top, 0.001f)
    }

    @Test fun lerpInPlaceKeepsCentre() {
        val a = rect(100f, 0f, 30f, 120f)
        val b = rect(105f, 20f, 20f, 80f)
        for (i in 0..10) {
            val r = RotationMorph.lerpInPlace(a, b, i / 10f)
            assertEquals(115f, r.left + r.width / 2f, 0.001f)
            assertEquals(60f, r.top + r.height / 2f, 0.001f)
        }
    }

    @Test fun flushEdgeMorph() {
        val a = rect(0f, 100f, 30f, 150f)
        val b = rect(0f, 100f, 27f, 150f)
        for (i in 0..10) {
            assertEquals(0f, RotationMorph.lerpInPlace(a, b, i / 10f).left, 0.001f)
        }
    }

    @Test fun trackerSkipsMatchingMorph() {
        val t = RotationMorphTracker()
        t.observe(portrait, rect(120f, 0f, 120f, 30f), enabled = true, nowMs = 0L)
        val carried = RotationMorph.rotate(rect(120f, 0f, 120f, 30f), RotationMorph.oldFrame(portrait, landscape).first, RotationMorph.oldFrame(portrait, landscape).second, RotationMorph.quarterTurns(portrait, landscape))
        t.observe(landscape, carried.copy(left = carried.left + 1f), enabled = true, nowMs = 1_000L)
        assertEquals(0, t.token)
        assertEquals(1, t.skips)
    }

    @Test fun nearlySameNeedsSizeAndPlaceToMatch() {
        val a = rect(0f, 100f, 30f, 120f)
        assertTrue(RotationMorph.nearlySame(a, a.copy(top = 101f)))
        assertFalse(RotationMorph.nearlySame(a, a.copy(height = 40f)))
        assertFalse(RotationMorph.nearlySame(a, a.copy(left = 10f)))
        assertFalse(RotationMorph.nearlySame(a, a.copy(flatLeft = 1f)))
    }

    @Test fun targetIsReadyOnceACameraIsFoundOrTheReadIsSettled() {
        val noCamera = landscape.copy(camera = null)
        assertFalse(RotationMorph.targetReady(noCamera, settled = false))
        assertTrue(RotationMorph.targetReady(landscape, settled = false))
        assertTrue(RotationMorph.targetReady(noCamera, settled = true))
        assertTrue(RotationMorph.targetReady(portrait, settled = false))
    }
}

class RotationVeilTest {
    @Test
    fun veilDelay() {
        val lastRead = RotationVeil.SETTLED_READ_AT_MS
        val total = lastRead + RotationVeil.liftDelayMs(lastRead)
        assertEquals(lastRead + RotationVeil.MARGIN_MS, total)
        assertTrue("hidden at most the cap", total <= RotationVeil.MAX_HIDDEN_MS)

        assertEquals(40L, RotationVeil.liftDelayMs(660L))
        assertEquals(0L, RotationVeil.liftDelayMs(700L))
        assertEquals(0L, RotationVeil.liftDelayMs(900L))
    }

    @Test
    fun fadeIsOpaqueWhenShownGoneWhenHiddenAndNeverBackwards() {
        assertEquals(1f, RotationVeil.alpha(1f), 0.0001f)
        assertEquals(0f, RotationVeil.alpha(0f), 0.0001f)
        var last = -1f
        for (i in 0..20) {
            val a = RotationVeil.alpha(i / 20f)
            assertTrue("alpha must not fall as the veil lifts", a >= last - 0.0001f)
            last = a
        }
    }
}
