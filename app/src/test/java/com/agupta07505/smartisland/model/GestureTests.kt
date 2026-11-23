/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.CollapsedPulse.Waiting
import com.agupta07505.smartisland.model.LauncherPage.Apps
import com.agupta07505.smartisland.model.LauncherPage.Recents
import com.agupta07505.smartisland.model.LauncherPage.Utilities
import com.agupta07505.smartisland.model.LauncherSwipe.Down
import com.agupta07505.smartisland.model.LauncherSwipe.Left
import com.agupta07505.smartisland.model.LauncherSwipe.Right
import com.agupta07505.smartisland.model.LauncherSwipe.Up
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCollapsePauseTest {
    @Test fun countdownResumesWithBonusUpToFullTime() {
        assertEquals(2800L, AutoCollapsePause.resumeDelayMs(1000L, 5000L))
        assertEquals(1800L, AutoCollapsePause.resumeDelayMs(0L, 5000L))
        assertEquals(5000L, AutoCollapsePause.resumeDelayMs(4000L, 5000L))
        assertEquals(5000L, AutoCollapsePause.resumeDelayMs(5000L, 5000L))
    }

    @Test fun outsideTouchPassthrough() {
        assertTrue(AutoCollapsePause.outsideTouchPassesThrough(autoExpanded = true, held = false))
        assertTrue(AutoCollapsePause.outsideTouchPassesThrough(autoExpanded = false, held = true))
        assertTrue(AutoCollapsePause.outsideTouchPassesThrough(autoExpanded = true, held = true))
        assertFalse(AutoCollapsePause.outsideTouchPassesThrough(autoExpanded = false, held = false))
    }
}

class AutoHideHoldTest {
    @Test fun onlyLandscapeClassicHolds() {
        assertTrue(AutoHideHold.applies(isLandscape = true, notchMode = false, edgeStyle = false))
        assertFalse(AutoHideHold.applies(isLandscape = false, notchMode = false, edgeStyle = false))
        assertFalse(AutoHideHold.applies(isLandscape = true, notchMode = true, edgeStyle = false))
        assertFalse(AutoHideHold.applies(isLandscape = true, notchMode = false, edgeStyle = true))
    }

    @Test
    fun edgeLayoutAlsoHoldsTheCountdownDuringGestures() {
        assertTrue(AutoHideHold.holds(applies = true, pulseBusy = true, gestureActive = false))
        assertTrue(AutoHideHold.holds(applies = true, pulseBusy = false, gestureActive = true))
        assertTrue(AutoHideHold.holds(applies = true, pulseBusy = true, gestureActive = true))
        assertFalse(AutoHideHold.holds(applies = true, pulseBusy = false, gestureActive = false))
        assertTrue(AutoHideHold.holds(applies = false, pulseBusy = true, gestureActive = false))
        assertFalse(AutoHideHold.holds(applies = false, pulseBusy = false, gestureActive = true))
    }


    @Test
    fun remainingHoldTime() {
        assertEquals(AutoHideHold.SETTLE_MS, AutoHideHold.settleLeftMs(10_000L, 10_000L))
        assertEquals(700L, AutoHideHold.settleLeftMs(10_000L, 10_000L + AutoHideHold.SETTLE_MS - 700L))
        assertEquals(0L, AutoHideHold.settleLeftMs(10_000L, 10_000L + AutoHideHold.SETTLE_MS))
        assertEquals(0L, AutoHideHold.settleLeftMs(10_000L, 60_000L))

        assertEquals(0L, AutoHideHold.settleLeftMs(0L, System.currentTimeMillis()))
    }
}

class FlightTapHoldTest {
    @Test
    fun heldTapComesBackWhenTheFlightEnds() {
        val hold = FlightTapHold()
        assertNull(hold.setFlying(true))
        assertTrue(hold.flying)
        hold.hold("b")
        assertEquals("b", hold.setFlying(false))
        assertFalse(hold.flying)
    }

    @Test
    fun lastTapWins() {
        val hold = FlightTapHold()
        hold.setFlying(true)
        hold.hold("b")
        hold.hold("c")
        assertEquals("c", hold.setFlying(false))
    }

    @Test
    fun heldTapRunsOnlyOnce() {
        val hold = FlightTapHold()
        hold.setFlying(true)
        hold.hold("b")
        assertEquals("b", hold.setFlying(false))
        assertNull(hold.setFlying(true))
        assertNull(hold.setFlying(false))
    }

    @Test
    fun noTapMeansNothingToRun() {
        val hold = FlightTapHold()
        hold.setFlying(true)
        assertNull(hold.setFlying(false))
        assertNull(hold.setFlying(false))
    }
}

class HideSinkTest {
    @Test fun everyLayoutSinksButLandscapeClassic() {
        assertTrue(HideSink.applies(isLandscape = false, notchMode = false, edgeStyle = false))
        assertTrue(HideSink.applies(isLandscape = false, notchMode = true, edgeStyle = false))
        assertTrue(HideSink.applies(isLandscape = true, notchMode = true, edgeStyle = false))
        assertTrue(HideSink.applies(isLandscape = true, notchMode = false, edgeStyle = true))
        assertFalse(HideSink.applies(isLandscape = true, notchMode = false, edgeStyle = false))
    }

    @Test fun sinkRunsFromInPlaceToSunk() {
        assertEquals(0f, HideSink.sink(1f), 0f)
        assertEquals(1f, HideSink.sink(0f), 0f)
        assertEquals(0.25f, HideSink.sink(0.75f), 1e-6f)
        assertEquals(0f, HideSink.sink(1.2f), 0f)
        assertEquals(1f, HideSink.sink(-0.3f), 0f)
    }

    @Test fun scaleReachesTheEndScaleWhenSunk() {
        assertEquals(1f, HideSink.scale(0f, 0.2f), 0f)
        assertEquals(0.2f, HideSink.scale(1f, 0.2f), 1e-6f)
        assertEquals(0.6f, HideSink.scale(0.5f, 0.2f), 1e-6f)
    }

    @Test fun solidForHalfTheSinkThenGone() {
        assertEquals(1f, HideSink.fade(1f), 0f)
        assertEquals(1f, HideSink.fade(0.5f), 1e-6f)
        assertEquals(0f, HideSink.fade(0f), 1e-6f)
        assertTrue(HideSink.fade(0.3f) in 0f..1f)
        assertTrue(HideSink.fade(0.4f) > HideSink.fade(0.2f))
    }

    @Test
    fun hidePivotOrigin() {
        val hole = SinkHole(centreXDp = 10f, centreYDp = 20f, widthDp = 24f, heightDp = 24f)
        val anchoredPill = SinkHole(centreXDp = 0f, centreYDp = 30f, widthDp = 120f, heightDp = 34f)
        val anchoredPivot = HideSink.pivot(hole, anchoredPill, windowTopDp = 8f)
        assertEquals(10f, anchoredPivot.xDp, 0f)
        assertEquals(12f, anchoredPivot.yDp, 0f)
        assertEquals(0.2f, anchoredPivot.endScaleX, 1e-6f)
        assertEquals(24f / 34f, anchoredPivot.endScaleY, 1e-6f)

        val tiny = SinkHole(0f, 0f, 2f, 2f)
        val huge = SinkHole(0f, 0f, 400f, 400f)
        val boundedPill = SinkHole(0f, 0f, 100f, 30f)
        assertEquals(HideSink.MIN_SCALE, HideSink.pivot(tiny, boundedPill, 0f).endScaleX, 0f)
        assertEquals(1f, HideSink.pivot(huge, boundedPill, 0f).endScaleX, 0f)

        val missingPill = SinkHole(centreXDp = 5f, centreYDp = 30f, widthDp = 120f, heightDp = 34f)
        val missingPivot = HideSink.pivot(null, missingPill, windowTopDp = 10f)
        assertEquals(5f, missingPivot.xDp, 0f)
        assertEquals(20f, missingPivot.yDp, 0f)
        assertEquals(HideSink.FALLBACK_SCALE, missingPivot.endScaleX, 0f)
        assertEquals(HideSink.FALLBACK_SCALE, missingPivot.endScaleY, 0f)
    }

    @Test
    fun portraitHidePivot() {
        val offsetArea = CutoutArea(centreXDp = 4f, topDp = 8f, widthDp = 20f, heightDp = 20f)
        val offsetPivot = HideSink.portraitPivot(offsetArea, false, false, 12f, 100f, 34f, 0f)
        assertEquals(4f, offsetPivot.xDp, 0f)
        assertEquals(6f, offsetPivot.yDp, 0f)
        assertEquals(0.2f, offsetPivot.endScaleX, 1e-6f)
        assertEquals(20f / 34f, offsetPivot.endScaleY, 1e-6f)

        val notchArea = CutoutArea(centreXDp = 0f, topDp = 8f, widthDp = 20f, heightDp = 20f)
        assertEquals(18f, HideSink.portraitPivot(notchArea, true, false, 12f, 100f, 34f, 0f).yDp, 0f)
        assertEquals(18f, HideSink.portraitPivot(notchArea, false, true, 12f, 100f, 34f, 0f).yDp, 0f)

        val empty = CutoutArea(centreXDp = 0f, topDp = 8f, widthDp = 0f, heightDp = 20f)
        for (area in listOf(null, empty)) {
            val missingPivot = HideSink.portraitPivot(area, false, false, 12f, 100f, 34f, 7f)
            assertEquals(7f, missingPivot.xDp, 0f)
            assertEquals(17f, missingPivot.yDp, 0f)
            assertEquals(HideSink.FALLBACK_SCALE, missingPivot.endScaleX, 0f)
        }
    }

    @Test
    fun edgeHidePivot() {
        run {
            val left = EdgeCamera(
                onLeft = true, centerYDp = 100f, screenWidthDp = 800f, screenHeightDp = 360f,
                centerFromEdgeDp = 20f, holeWidthDp = 24f, holeLengthDp = 24f, cutoutReported = true
            )
            val pill = EdgeRect(left = 4f, top = 88f, width = 40f, height = 80f)
            val pivot = HideSink.edgePivot(left, pill)
            assertEquals(20f - 400f, pivot.xDp, 0f)
            assertEquals(100f, pivot.yDp, 0f)
            assertEquals(24f / 40f, pivot.endScaleX, 1e-6f)
            assertEquals(24f / 80f, pivot.endScaleY, 1e-6f)
            val right = HideSink.edgePivot(left.copy(onLeft = false), pill)
            assertEquals(800f - 20f - 400f, right.xDp, 0f)
        }

        val camera = EdgeCamera(onLeft = true, centerYDp = 100f, screenWidthDp = 800f, screenHeightDp = 360f)
        val fallbackPill = EdgeRect(left = 0f, top = 60f, width = 32f, height = 80f)
        val fallbackPivot = HideSink.edgePivot(camera, fallbackPill)
        assertEquals(HideSink.DEFAULT_HOLE_DP / 32f, fallbackPivot.endScaleX, 1e-6f)
        assertEquals(HideSink.DEFAULT_HOLE_DP / 80f, fallbackPivot.endScaleY, 1e-6f)
        assertEquals(100f, fallbackPivot.yDp, 0f)
    }
}

class HoldGestureTest {
    private fun hold(
        collapsed: Boolean = true,
        dragging: Boolean = false,
        claimed: Boolean = false,
        opensApps: Boolean = false,
        action: SwipeAction = SwipeAction.TogglePill
    ) = HoldGesture.atThreshold(collapsed, dragging, claimed, opensApps, action)

    private fun glyph(
        collapsed: Boolean = true,
        dragging: Boolean = false,
        claimed: Boolean = false,
        opensApps: Boolean = true,
        action: SwipeAction = SwipeAction.TogglePill
    ) = HoldGesture.atThreshold(collapsed, dragging, claimed, opensApps, action, onGlyph = true)

    @Test
    fun holdEligibility() {
        val toggle = hold()
        assertTrue(toggle.buzz)
        assertFalse(toggle.armAppsShortcut)
        assertEquals(SwipeAction.TogglePill, toggle.runAction)

        val noAction = hold(action = SwipeAction.None)
        assertTrue(noAction.buzz)
        assertNull(noAction.runAction)
        assertFalse(noAction.armAppsShortcut)

        val shortcut = hold(opensApps = true)
        assertFalse(shortcut.buzz)
        assertFalse(shortcut.glow)
        assertFalse(shortcut.readSwipe)
        assertTrue(shortcut.armAppsShortcut)
        assertNull(shortcut.runAction)

        assertFalse(hold().glow)
        assertFalse(hold(opensApps = true).glow)

        for (collapsed in listOf(false, true)) for (dragging in listOf(false, true))
            for (claimed in listOf(false, true)) for (apps in listOf(false, true))
                for (action in SwipeAction.values()) {
                    val exclusive = hold(collapsed, dragging, claimed, apps, action)
                    assertFalse(exclusive.armAppsShortcut && exclusive.runAction != null)
                    assertTrue(exclusive.buzz || exclusive.armAppsShortcut)
                }
    }

    @Test
    fun glyphHoldEligibility() {
        val armed = glyph(opensApps = true)
        assertTrue(armed.buzz)
        assertTrue(armed.glow)
        assertTrue(armed.readSwipe)
        assertTrue(armed.armAppsShortcut)
        assertNull(armed.runAction)

        val noShortcut = glyph(opensApps = false, action = SwipeAction.TogglePill)
        assertTrue(noShortcut.buzz && noShortcut.glow && noShortcut.readSwipe)
        assertFalse(noShortcut.armAppsShortcut)
        assertNull(noShortcut.runAction)

        for (d in listOf(glyph(collapsed = false), glyph(dragging = true), glyph(claimed = true))) {
            assertTrue(d.buzz)
            assertFalse(d.glow || d.readSwipe || d.armAppsShortcut)
            assertNull(d.runAction)
        }
    }

    @Test
    fun glyphSwipeNeedsTravelBeyondSlop() {
        assertTrue(HoldGesture.glyphSwiped(-30f, 2f, 10f, true))
        assertTrue(HoldGesture.glyphSwiped(30f, -2f, 10f, true))
        assertFalse(HoldGesture.glyphSwiped(8f, 0f, 10f, true))
        assertFalse(HoldGesture.glyphSwiped(-30f, 0f, 10f, false))
        assertFalse(HoldGesture.glyphSwiped(12f, 40f, 10f, true))
        assertFalse(HoldGesture.glyphSwiped(0f, 40f, 10f, false))
    }


    @Test
    fun liftAfterGlyphHoldPrefersSwipe() {
        assertEquals(GlyphLift.Repulse, HoldGesture.glyphLift(true, true, true))
        assertEquals(GlyphLift.Repulse, HoldGesture.glyphLift(true, false, true))
        assertEquals(GlyphLift.OpenApps, HoldGesture.glyphLift(false, true, false))
        // pulled back to the middle (slop was exceeded) or no shortcut: nothing happens
        assertEquals(GlyphLift.Nothing, HoldGesture.glyphLift(false, true, true))
        assertEquals(GlyphLift.Nothing, HoldGesture.glyphLift(false, false, false))
    }

    @Test
    fun anExpandedIslandADragOrAClaimedTouchOnlyBuzz() {
        for (d in listOf(hold(collapsed = false), hold(dragging = true), hold(claimed = true))) {
            assertTrue(d.buzz)
            assertFalse(d.armAppsShortcut)
            assertNull(d.runAction)
        }
        // also when the apps shortcut would have applied
        val d = hold(collapsed = false, opensApps = true)
        assertTrue(d.buzz)
        assertFalse(d.armAppsShortcut)
    }

    @Test
    fun cornerPillRunsTheActionOnlyForAStillUnclaimedTouch() {
        assertEquals(SwipeAction.TogglePill, HoldGesture.cornerActionAtThreshold(false, false, SwipeAction.TogglePill))
        assertNull(HoldGesture.cornerActionAtThreshold(true, false, SwipeAction.TogglePill))
        assertNull(HoldGesture.cornerActionAtThreshold(false, true, SwipeAction.TogglePill))
        assertNull(HoldGesture.cornerActionAtThreshold(false, false, SwipeAction.None))
    }

    @Test
    fun armedShortcutOpensOnLiftInsideTapArea() {
        assertTrue(HoldGesture.opensAppsOnLift(armed = true, slopExceeded = false))
        assertFalse(HoldGesture.opensAppsOnLift(armed = true, slopExceeded = true))
        assertFalse(HoldGesture.opensAppsOnLift(armed = false, slopExceeded = false))
        assertFalse(HoldGesture.opensAppsOnLift(armed = false, slopExceeded = true))
    }

    @Test
    fun edgeHoldSwipeZones() {
        assertTrue(HoldGesture.swipeZone(PillTapZone.Glyph))
        assertTrue(HoldGesture.swipeZone(PillTapZone.Value))
        assertFalse(HoldGesture.swipeZone(PillTapZone.Middle))
    }

    @Test
    fun pulseStateDecidesZones() {
        assertTrue(HoldGesture.holdStartsSwipe(PillTapZone.Middle, pulseRunning = true))
        assertTrue(HoldGesture.holdStartsSwipe(PillTapZone.Glyph, pulseRunning = true))
        assertTrue(HoldGesture.holdStartsSwipe(PillTapZone.Value, pulseRunning = true))
        assertFalse(HoldGesture.holdStartsSwipe(PillTapZone.Middle, pulseRunning = false))
        assertTrue(HoldGesture.holdStartsSwipe(PillTapZone.Glyph, pulseRunning = false))
        assertTrue(HoldGesture.holdStartsSwipe(PillTapZone.Value, pulseRunning = false))
    }


    @Test
    fun tapIgnoresLateOpen() {
        assertTrue(HoldGesture.tapOpenedUnderFinger(openAtDown = false, openNow = true))
        assertFalse(HoldGesture.tapOpenedUnderFinger(openAtDown = false, openNow = false))
        assertFalse(HoldGesture.tapOpenedUnderFinger(openAtDown = true, openNow = true))
        assertFalse(HoldGesture.tapOpenedUnderFinger(openAtDown = true, openNow = false))
    }

}

class LauncherCloseTest {
    @Test fun pillShownWhileOpenOrClosingOnly() {
        assertTrue(LauncherClose.shown(open = true, expanded = true, closing = false))
        assertTrue(LauncherClose.shown(open = false, expanded = false, closing = true))
        assertFalse(LauncherClose.shown(open = false, expanded = false, closing = false))
        assertFalse(LauncherClose.shown(open = true, expanded = false, closing = false))
    }

    @Test
    fun holdEndsAfterTheCloseWindow() {
        assertEquals(1_600L, LauncherClose.holdUntil(1_000L))
    }
}

class LauncherPagerTest {
    @Test
    fun launcherPageOrder() {
        assertEquals(Utilities, launcherPageAfter(Apps, Up, recentsEnabled = true))
        assertEquals(Utilities, launcherPageAfter(Apps, Down, recentsEnabled = true))
        assertEquals(Apps, launcherPageAfter(Utilities, Up, recentsEnabled = true))
        assertEquals(Apps, launcherPageAfter(Utilities, Down, recentsEnabled = true))
        assertEquals(Recents, launcherPageAfter(Apps, Left, true))
        assertEquals(Recents, launcherPageAfter(Apps, Right, true))
        assertEquals(Apps, launcherPageAfter(Recents, Left, true))
        assertEquals(Apps, launcherPageAfter(Recents, Right, true))
        assertEquals(Apps, launcherPageAfter(Apps, Left, false))
        assertEquals(Apps, launcherPageAfter(Recents, Right, false))
        assertEquals(Utilities, launcherPageAfter(Utilities, Left, true))
        assertEquals(Utilities, launcherPageAfter(Utilities, Right, false))

        assertEquals(Utilities, launcherPageAfter(Recents, Up, true, lastAppsLevelPage = Recents))
        assertEquals(Recents, launcherPageAfter(Utilities, Up, true, lastAppsLevelPage = Recents))
        // recents turned off in the meantime: back to Apps
        assertEquals(Apps, launcherPageAfter(Utilities, Down, false, lastAppsLevelPage = Recents))
    }

    @Test fun openLauncherClaimsDragsPastThreshold() {
        assertTrue(launcherOwnsDrag(true, 0f, 3f))
        assertTrue(launcherOwnsDrag(true, -2f, 0f))
        assertFalse(launcherOwnsDrag(true, 0.2f, 0.1f))
        assertFalse(launcherOwnsDrag(false, 30f, 30f))
    }

    @Test fun longerAxisPicksSwipe() {
        assertNull(launcherSwipeFor(10f, -20f, 48f))
        assertEquals(Up, launcherSwipeFor(8f, -60f, 48f))
        assertEquals(Down, launcherSwipeFor(-8f, 60f, 48f))
        assertEquals(Left, launcherSwipeFor(-70f, 12f, 48f))
        assertEquals(Right, launcherSwipeFor(70f, -12f, 48f))
        // A diagonal drag counts as vertical unless it clearly leans sideways, i.e. has more horizontal travel
        assertEquals(Up, launcherSwipeFor(60f, -60f, 48f))
    }
}

class OutsideTapHeldTest {
    @Test
    fun outsideTapClosesUnlessHeldOrHandledByReply() {
        assertFalse(AutoCollapsePause.outsideTapCloses(held = true, inputActive = false))
        assertTrue(AutoCollapsePause.outsideTapCloses(held = false, inputActive = false))
        assertTrue(AutoCollapsePause.outsideTapCloses(held = true, inputActive = true))
    }


}

class PortraitPillTapTest {
    // A 120 dp wide, 34 dp tall pill at 3x density: 360 x 102 px. Collapsed, the shell is the pill (centre 180 px)
    private val pillW = 360f
    private val pillH = 102f

    @Test
    fun centreTapBounds() {
        val half = pillW * PILL_CENTER_ZONE_SHARE / 2f
        assertTrue(pillCenterTapHit(180f, 50f, pillW, pillW, pillH))
        assertTrue(pillCenterTapHit(180f - half + 1f, 50f, pillW, pillW, pillH))
        assertTrue(pillCenterTapHit(180f + half - 1f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f - half - 1f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f + half + 1f, 50f, pillW, pillW, pillH))

        // left glyph (about 12% in) and right glyph (about 90% in)
        assertFalse(pillCenterTapHit(pillW * 0.12f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(pillW * 0.9f, 50f, pillW, pillW, pillH))

        // expanded: the shell is 900 px wide and 600 px tall; the pill's centre band sits at the top centre
        assertTrue(pillCenterTapHit(450f, 40f, 900f, pillW, pillH))
        assertFalse(pillCenterTapHit(450f, pillH + 10f, 900f, pillW, pillH))
        assertFalse(pillCenterTapHit(150f, 40f, 900f, pillW, pillH))

        assertFalse(pillCenterTapHit(0f, 0f, 0f, pillW, pillH))
        assertFalse(pillCenterTapHit(0f, 0f, pillW, 0f, pillH))
        assertFalse(pillCenterTapHit(0f, 0f, pillW, pillW, 0f))
    }

    @Test
    fun theCentreIsAnIdleControlInPortraitWithCards() {
        assertTrue(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = false, hasCards = true))
        assertFalse(portraitCenterTapIsIdleControl(isLandscape = true, notchMode = false, hasCards = true))
        assertTrue(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = true, hasCards = true))
        assertFalse(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = false, hasCards = false))
    }

    @Test
    fun closeTapBand() {
        val noCutout = expandedCloseZone(false, 60f, 40f, 5f, 8f, 12f, 120f, 34f)
        assertEquals(120f * PILL_CENTER_ZONE_SHARE, noCutout.widthDp, 0.0001f)
        assertEquals(34f, noCutout.heightDp, 0.0001f)
        assertEquals(null, noCutout.offsetXDp)
        assertEquals(0f, noCutout.topDp, 0.0001f)

        // hole detected 70 x 44 dp, 6 dp right of centre, top at 20 dp; the window sits 12 dp down: the zone starts 8 dp into it
        val detectedZ = expandedCloseZone(true, 70f, 44f, 6f, 20f, 12f, 120f, 34f)
        assertEquals(120f * PILL_CENTER_ZONE_SHARE, detectedZ.widthDp, 0.0001f)
        assertEquals(44f, detectedZ.heightDp, 0.0001f)
        assertEquals(6f, detectedZ.offsetXDp!!, 0.0001f)
        assertEquals(8f, detectedZ.topDp, 0.0001f)

        val clamped = expandedCloseZone(true, 150f, 40f, 0f, 4f, 12f, 112f, 34f)
        assertEquals(112f * PILL_CENTER_ZONE_SHARE, clamped.widthDp, 0.0001f)
        assertEquals(0f, clamped.topDp, 0.0001f)

        val degenerate = expandedCloseZone(true, 0f, 0f, 0f, 0f, 0f, 120f, 34f)
        assertEquals(null, degenerate.offsetXDp)
    }

    // band: 38 dp wide (centre 0), 34 dp tall from y = 12
    private fun overlap(cutX: Float?, cutW: Float = 30f, cutTop: Float = 12f, cutH: Float = 24f) =
        centreBandOverlapsCutout(0f, 38f, 12f, 34f, cutX, cutW, cutTop, cutH)

    @Test
    fun centredCutoutOverlapsTheBand() = assertTrue(overlap(0f))

    @Test
    fun cutoutMustIntersectTheBand() {
        assertFalse(overlap(34f)) // cutout 19..49: 0 dp
        assertTrue(overlap(33f))   // cutout 18..48, band -19..19: 1 dp
        assertFalse(overlap(33.5f)) // cutout 18.5..48.5: 0.5 dp
        assertFalse(overlap(0f, cutTop = -40f))
        assertFalse(overlap(0f, cutTop = 60f))
    }


    @Test
    fun noKnownCutoutNeverOverlaps() = assertFalse(overlap(null))

    @Test
    fun longPressRules() {
        // overlapping: glyphs open the apps, the middle band keeps the normal long press
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = false))
        assertFalse(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = true))
        // not overlapping: anywhere
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = false, touchInCentreBand = true))
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = false, touchInCentreBand = false))
        // notch mode behaves as overlapping
        assertTrue(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = false))
        assertFalse(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = true))
        // landscape: the collapsed pill always opens the apps, whatever the overlap flags say
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true))
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = true, touchInCentreBand = true))
        // landscape idle cutout (no card, no widget pill): never the shortcut, only the normal long press
        assertFalse(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true, pillHasContent = false))
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true, pillHasContent = true))
        assertFalse(longPressOpensApps(true, false, false, false, false))
        // the launcher switched off: unchanged behaviour
        assertFalse(longPressOpensApps(false, false, false, false, false))
    }

    @Test
    fun cutoutTargetGrowsToFinger() {
        val t = cutoutTouchTarget(CutoutArea(10f, 12f, 12f, 12f))
        assertEquals(CUTOUT_TARGET_MIN_WIDTH_DP, t.widthDp, 0.001f)
        assertEquals(CUTOUT_TARGET_MIN_HEIGHT_DP, t.heightDp, 0.001f)
        assertEquals(10f, t.centreXDp, 0.001f)
        assertEquals(4f, t.topDp, 0.001f)   // centre y 18, half height 14
        assertEquals(0f, cutoutTouchTarget(CutoutArea(0f, 2f, 12f, 12f)).topDp, 0.001f)
        assertEquals(60f, cutoutTouchTarget(CutoutArea(0f, 0f, 60f, 40f)).widthDp, 0.001f)
    }
}

class QuickCardHoldTest {

    private fun card(key: String, mode: IslandMode, category: String? = null) = IslandNotification(
        key = key,
        packageName = "pkg.$key",
        appName = key,
        title = key,
        text = "",
        timeMillis = 0L,
        category = category,
        mode = mode
    )

    private val music = card("music", IslandMode.Music)
    private val plugged = card("system_battery", IslandMode.Battery, category = "battery_plugged_in")
    private val message = card("message", IslandMode.Notification)

    private fun entry(c: IslandNotification) = CollapsedPulse.entryOf(c)

    private fun pulsing(c: IslandNotification, queued: List<IslandNotification> = emptyList()) =
        CollapsedPulse.State(
            pulsing = entry(c),
            pulseEndsAtMs = 10_000L,
            queue = queued.map { Waiting(entry(it), 0L) }
        )

    /** Music pulses, [cards] wait behind it */
    private fun queued(vararg cards: IslandNotification) =
        CollapsedPulse.State(pulsing = entry(music), pulseEndsAtMs = 10_000L, queue = cards.map { Waiting(entry(it), 0L) })

    private val idle = CollapsedPulse.State()

    private fun start(raw: List<IslandNotification>, pulse: CollapsedPulse.State, at: Long = 0L) =
        QuickCardHold.update(QuickCardHold.State(), raw, pulse, nowMs = at)

    private fun QuickCardHold.State.next(raw: List<IslandNotification>, pulse: CollapsedPulse.State, at: Long) =
        QuickCardHold.update(this, raw, pulse, nowMs = at)

    private fun QuickCardHold.State.keys() = QuickCardHold.cards(this).map { it.key }

    @Test
    fun aCardRemovedBeforeItsTurnStaysWhileItWaits() {
        val s = start(listOf(music, plugged), queued(plugged)).next(listOf(music), queued(plugged), 2_500L)
        assertEquals(listOf("system_battery"), s.keys())
    }

    @Test
    fun anUnnoticedCardIsNotHeld() {
        val s = start(listOf(music, plugged), idle).next(listOf(music), idle, 2_500L)
        assertTrue(s.keys().isEmpty())
    }

    @Test
    fun aCardThatLostItsPulseIsReleased() {
        val s = start(listOf(music, plugged), queued(plugged)).next(listOf(music), queued(plugged), 2_500L)
        val dropped = s.next(listOf(music), queued(), 5_000L)
        assertTrue(dropped.keys().isEmpty())
    }

    @Test
    fun liveEventBubbleQueue() {
        val during = start(listOf(music, plugged), pulsing(plugged)).next(listOf(music), pulsing(plugged), 2_000L)
        assertEquals(listOf("system_battery"), during.keys())
        val over = during.next(listOf(music), idle, 4_000L)
        assertEquals(listOf("system_battery"), over.keys())
        assertEquals(4_000L + QuickCardHold.BUBBLE_WAIT_MS, QuickCardHold.nextDeadlineMs(over))
        assertEquals(listOf("system_battery"), over.next(listOf(music), idle, 7_499L).keys())
        assertTrue(over.next(listOf(music), idle, 7_500L).keys().isEmpty())
        assertNull(QuickCardHold.nextDeadlineMs(over.next(listOf(music), idle, 7_500L)))
    }

    @Test
    fun noLiveEventBehindMeansNoWait() {
        val during = start(listOf(plugged), pulsing(plugged)).next(listOf(), pulsing(plugged), 2_000L)
        assertEquals(listOf("system_battery"), during.keys())
        assertTrue(during.next(listOf(), idle, 4_000L).keys().isEmpty())
    }

    @Test
    fun theWaitEndsAtOnceWhenTheLiveEventLeaves() {
        val during = start(listOf(music, plugged), pulsing(plugged)).next(listOf(music), pulsing(plugged), 2_000L)
        val waiting = during.next(listOf(music), idle, 4_000L)
        assertTrue(waiting.next(listOf(), idle, 4_500L).keys().isEmpty())
    }

    @Test
    fun removedCardPulseBubble() {
        val s = start(listOf(music, plugged), pulsing(plugged)).next(listOf(music, plugged), idle, 3_000L)
        val removed = s.next(listOf(music), idle, 3_100L)
        assertEquals(listOf("system_battery"), removed.keys())
        assertEquals(3_100L + QuickCardHold.BUBBLE_WAIT_MS, QuickCardHold.nextDeadlineMs(removed))
    }

    @Test
    fun aCardPostedAgainReplacesItsCopy() {
        val held = start(listOf(music, plugged), queued(plugged)).next(listOf(music), queued(plugged), 2_500L)
        assertTrue(held.next(listOf(music, plugged), queued(plugged), 3_000L).keys().isEmpty())
    }

    @Test
    fun onlyQuickCardsAreHeld() {
        val s = start(listOf(music, message), pulsing(message)).next(listOf(music), pulsing(message), 2_000L)
        assertTrue(s.keys().isEmpty())
    }

    @Test
    fun aCardATapOpenedTheIslandOnStaysWhileItIsKept() {
        val held = start(listOf(music, plugged), queued(plugged))
            .next(listOf(music), queued(plugged), 2_500L)
        assertEquals(listOf("system_battery"), held.keys())

        // the tap settled the pulse, the island is open on the card
        val id = entry(plugged).id
        val open = QuickCardHold.update(held, listOf(music), idle, nowMs = 3_000L, kept = setOf(id))
        assertEquals(listOf("system_battery"), open.keys())
        assertNull(open.held.first().releaseAtMs)

        // the island closed: the copy goes
        val closed = QuickCardHold.update(open, listOf(music), idle, nowMs = 9_000L)
        assertTrue(closed.keys().isEmpty())
    }

    @Test
    fun aKeptCardThatLeavesWhileTheIslandIsOpenIsHeldToo() {
        val id = entry(plugged).id
        val known = QuickCardHold.update(QuickCardHold.State(), listOf(plugged), idle, nowMs = 0L, kept = setOf(id))
        val gone = QuickCardHold.update(known, emptyList(), idle, nowMs = 1_000L, kept = setOf(id))
        assertEquals(listOf("system_battery"), gone.keys())
    }
}

class StepDragTest {
    @Test fun progressMathIsSafe() {
        assertEquals(0f, StepDrag.progress(5f, 10f, 100f), 0.0001f)
        assertEquals(0.5f, StepDrag.progress(60f, 10f, 100f), 0.0001f)
        assertEquals(1f, StepDrag.progress(500f, 10f, 100f), 0.0001f)
        assertEquals(0f, StepDrag.progress(50f, 10f, 0f), 0f)
    }

    @Test
    fun commitsOnlyPastThePoint() {
        assertFalse(StepDrag.commits(StepDrag.COMMIT_PROGRESS - 0.01f))
        assertTrue(StepDrag.commits(StepDrag.COMMIT_PROGRESS))
    }

    @Test
    fun stepCrossfade() {
        assertEquals(1f, StepDrag.oldStepAlpha(0f), 0.0001f)
        assertEquals(0f, StepDrag.newStepAlpha(0f), 0.0001f)
        assertEquals(0f, StepDrag.oldStepAlpha(StepDrag.CROSSFADE_END), 0.0001f)
        assertEquals(1f, StepDrag.newStepAlpha(StepDrag.CROSSFADE_END), 0.0001f)
        assertEquals(0f, StepDrag.oldStepAlpha(1f), 0f)
        assertEquals(1f, StepDrag.newStepAlpha(1f), 0f)

        for (i in 0..20) {
            val p = i / 20f
            assertEquals(1f, StepDrag.oldStepAlpha(p) + StepDrag.newStepAlpha(p), 0.0001f)
        }
    }

    @Test
    fun heightRunsWithTheProgress() {
        assertEquals(100f, StepDrag.heightDp(100f, 200f, 0f), 0.0001f)
        assertEquals(150f, StepDrag.heightDp(100f, 200f, 0.5f), 0.0001f)
        assertEquals(200f, StepDrag.heightDp(100f, 200f, 1f), 0.0001f)
    }

    @Test
    fun endOfDragHaptic() {
        val h = StepDragHaptic()
        assertFalse(h.onProgress(0.5f))
        assertFalse(h.onProgress(0.99f))
        assertTrue(h.onProgress(1f))
        // staying at the end, pulling back and returning: still only the one buzz
        assertFalse(h.onProgress(1f))
        assertFalse(h.onProgress(0.3f))
        assertFalse(h.onProgress(1f))
        assertFalse(h.onLift(committed = true))
    }

    @Test
    fun stepFinishedBeforeTheEndBuzzesOnceAtTheLift() {
        val h = StepDragHaptic()
        h.onProgress(0.6f)
        assertTrue(h.onLift(committed = true))
        assertFalse(h.onLift(committed = true))
    }

    @Test
    fun shortPullThatGoesBackNeverBuzzesAtTheLift() {
        val h = StepDragHaptic()
        h.onProgress(0.2f)
        assertFalse(h.onLift(committed = false))
    }

    @Test
    fun dragSmoothing() {
        var shown = 0f
        var last = shown
        repeat(60) {
            shown = StepDrag.smoothed(shown, 1f, 16f)
            assertTrue(shown >= last)
            assertTrue(shown <= 1f)
            last = shown
        }
        assertEquals(1f, shown, 0.0001f)

        var at60 = 0f
        repeat(6) { at60 = StepDrag.smoothed(at60, 1f, 1000f / 60f) }
        var at120 = 0f
        repeat(12) { at120 = StepDrag.smoothed(at120, 1f, 1000f / 120f) }
        assertEquals(at60, at120, 0.01f)

        assertEquals(0.5f, StepDrag.smoothed(0.4995f, 0.5f, 16f), 0f)
        assertEquals(0.3f, StepDrag.smoothed(0.3f, 0.9f, 0f), 0f)

        val next = StepDrag.smoothed(0.8f, 0.2f, 16f)
        assertTrue(next < 0.8f && next > 0.2f)
    }
}
