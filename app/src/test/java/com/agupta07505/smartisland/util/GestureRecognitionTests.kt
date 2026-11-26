/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.PillSwipeDirection
import com.agupta07505.smartisland.model.TapGuard
import com.agupta07505.smartisland.model.TouchZone
import com.agupta07505.smartisland.model.deadZoneCue
import com.agupta07505.smartisland.model.stepFollowsFinger
import com.agupta07505.smartisland.model.touchZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisLockTest {
    @Test
    fun staysOpenInsideTheSlop() {
        val lock = AxisLock(slopPx = 20f)
        assertFalse(lock.update(10f, 12f))
        assertNull(lock.axis)
        assertEquals(5f, lock.filterX(5f), 0f)
        assertEquals(5f, lock.filterY(5f), 0f)
    }

    @Test
    fun verticalDragIgnoresSidewaysMovement() {
        val lock = AxisLock(slopPx = 20f)
        assertTrue(lock.update(6f, 25f))
        assertEquals(AxisLock.Axis.Vertical, lock.axis)
        assertEquals(0f, lock.filterX(30f), 0f)
        assertEquals(7f, lock.filterY(7f), 0f)
    }

    @Test
    fun horizontalIgnoresVertical() {
        val lock = AxisLock(slopPx = 20f)
        assertTrue(lock.update(-30f, 8f))
        assertEquals(AxisLock.Axis.Horizontal, lock.axis)
        assertEquals(0f, lock.filterY(-40f), 0f)
        assertEquals(-7f, lock.filterX(-7f), 0f)
    }

    @Test
    fun lockNeverFlipsWithinOneTouch() {
        val lock = AxisLock(slopPx = 20f)
        lock.update(0f, 30f)
        assertFalse(lock.update(200f, 30f))
        assertEquals(AxisLock.Axis.Vertical, lock.axis)
    }
}

class GestureSensitivityTest {
    private val n = GestureSensitivity.NORMAL
    private val low = GestureSensitivity.LOW
    private val high = GestureSensitivity.HIGH

    @Test
    fun unknownIdsFallBackToNormal() {
        assertEquals(n, GestureSensitivity.fromId(null))
        assertEquals(n, GestureSensitivity.fromId("twitchy"))
        assertEquals(low, GestureSensitivity.fromId("low"))
        assertEquals(high, GestureSensitivity.fromId("high"))
    }

    @Test
    fun sensitivityThresholds() {
        assertEquals(16f, n.scaled(16f), 0f)
        assertEquals(35f, n.scaled(35f), 0f)
        assertEquals(10f, n.scaled(10f), 0f)
        assertEquals(1f, n.dominance, 0f)

        assertTrue(high.scaled(16f) < n.scaled(16f))
        assertTrue(low.scaled(16f) > n.scaled(16f))
        assertTrue(high.scaled(35f) < 35f && low.scaled(35f) > 35f)

        val shortDrag = 13f
        assertNull(n.pillSwipe(shortDrag, 0f, n.scaled(16f)))
        assertEquals(PillSwipeDirection.Right, high.pillSwipe(shortDrag, 0f, high.scaled(16f)))
    }

    @Test
    fun normalPillSwipeMatchesTheOldRule() {
        // either axis past the threshold; the longer axis wins, a tie goes vertical
        assertEquals(PillSwipeDirection.Left, n.pillSwipe(-20f, 5f, 16f))
        assertEquals(PillSwipeDirection.Right, n.pillSwipe(20f, -5f, 16f))
        assertEquals(PillSwipeDirection.Up, n.pillSwipe(5f, -20f, 16f))
        assertEquals(PillSwipeDirection.Down, n.pillSwipe(-5f, 20f, 16f))
        assertEquals(PillSwipeDirection.Down, n.pillSwipe(20f, 20f, 16f))
        assertEquals(PillSwipeDirection.Up, n.pillSwipe(-20f, -20f, 16f))
        assertNull(n.pillSwipe(10f, 10f, 16f))
        assertNull(n.pillSwipe(0f, 0f, 16f))
    }

    @Test
    fun lowIgnoresDiagonalDrags() {
        val t = low.scaled(16f)
        assertNull(low.pillSwipe(t + 5f, t + 2f, t))
        assertNull(low.pillSwipe(-(t + 5f), -(t + 2f), t))
        assertEquals(PillSwipeDirection.Left, low.pillSwipe(-(t * 2f), t * 0.5f, t))
        assertEquals(PillSwipeDirection.Up, low.pillSwipe(t * 0.5f, -(t * 2f), t))
    }

    @Test
    fun axisChecksFollowDominance() {
        assertTrue(n.isHorizontal(10f, 9f))
        assertFalse(n.isHorizontal(10f, 10f))
        assertTrue(n.isVertical(10f, 10f))
        assertFalse(low.isHorizontal(10f, 9f))
        assertFalse(low.isVertical(10f, 9f))
        assertTrue(low.isHorizontal(20f, 9f))
    }
}

class TapGuardTest {
    @Test
    fun slopTouchIsStillATap() {
        val g = TapGuard(slopPx = 30f)
        g.update(0f, 0f)
        g.update(10f, 12f)
        assertFalse(g.exceeded)
        assertEquals(15.62f, g.peak, 0.01f)
    }

    @Test
    fun distanceUsesBothAxes() {
        // 3-4-5: exactly the slop is still a tap, a hair more is not
        assertFalse(TapGuard(5f).also { it.update(3f, 4f) }.exceeded)
        assertTrue(TapGuard(4.9f).also { it.update(3f, 4f) }.exceeded)
    }

    @Test
    fun shortSidewaysDragNotATap() {
        // the expanded rule must look at both axes, not only the vertical offset: 25 dp sideways with 0 dp up/down is not a tap
        val slop = 10f
        val g = TapGuard(slop)
        g.update(25f, 0f)
        assertTrue(g.exceeded)
    }

    @Test
    fun dragThatCameBackStaysCancelled() {
        val g = TapGuard(10f)
        g.update(40f, 0f)
        g.update(1f, 0f)
        assertTrue(g.exceeded)
        assertEquals(40f, g.peak, 0.001f)
    }

    @Test
    fun slopBelowSwipeThresholds() {
        for (s in GestureSensitivity.entries) {
            val slop = s.scaled(10f)
            assertTrue(s.id, slop < s.scaled(16f))
            assertTrue(s.id, slop < s.scaled(35f))
        }
    }

    @Test
    fun zones() {
        assertEquals(TouchZone.Tap, touchZone(slopExceeded = false, liftFires = false))
        assertEquals(TouchZone.Dead, touchZone(slopExceeded = true, liftFires = false))
        assertEquals(TouchZone.Armed, touchZone(slopExceeded = true, liftFires = true))
        // A swipe that fires always wins, whatever the slop says
        assertEquals(TouchZone.Armed, touchZone(slopExceeded = false, liftFires = true))
    }

    @Test
    fun fingerStepOnlyPlusMinusOne() {
        assertTrue(stepFollowsFinger(1))
        assertTrue(stepFollowsFinger(-1))
        assertFalse(stepFollowsFinger(0))
        // 2 = nothing to open here, the plain swipe rules apply
        assertFalse(stepFollowsFinger(2))
    }

    @Test
    fun deadZoneCueOffWhileStepping() {
        // past the slop, short of the swipe threshold: Dead for a plain swipe...
        assertTrue(deadZoneCue(slopExceeded = true, liftFires = false, stepFollowing = false))
        // ...but a step drag decides on lift by its own progress, so no shrink
        assertFalse(deadZoneCue(slopExceeded = true, liftFires = false, stepFollowing = true))
        // A tap and an armed swipe never show it
        assertFalse(deadZoneCue(slopExceeded = false, liftFires = false, stepFollowing = false))
        assertFalse(deadZoneCue(slopExceeded = true, liftFires = true, stepFollowing = false))
    }
}

class ReaderScrollGuardTest {
    @Test
    fun aReaderThatNeverScrolledAllowsTaps() {
        val t = ReaderScrollTracker()
        assertFalse(t.touchStartsOnScroll(1_000L))
    }

    @Test
    fun aTouchOnAFlingingReaderIsNotATap() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        assertTrue(t.touchStartsOnScroll(5_000L)) // still flinging, however long
    }

    @Test
    fun tapAfterFling() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        t.onScrollState(false, 2_000L)
        assertTrue(t.touchStartsOnScroll(2_000L + READER_SCROLL_SETTLE_MS - 1))
        assertFalse(t.touchStartsOnScroll(2_000L + READER_SCROLL_SETTLE_MS))
    }

    @Test
    fun anOffsetChangeCountsAsActivity() {
        val t = ReaderScrollTracker()
        t.onMoved(3_000L)
        assertTrue(t.touchStartsOnScroll(3_100L))
        assertFalse(t.touchStartsOnScroll(3_400L))
    }

    @Test
    fun resetForgetsOffscreenScroll() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        t.reset()
        assertFalse(t.touchStartsOnScroll(1_050L))
    }

    @Test
    fun readerTapEligibility() {
        assertTrue(readerTapAccepted(startedOnScroll = false, movesAtDown = 4, movesAtLift = 4))
        assertFalse(readerTapAccepted(startedOnScroll = true, movesAtDown = 4, movesAtLift = 4))
        assertFalse(readerTapAccepted(startedOnScroll = false, movesAtDown = 4, movesAtLift = 5))
    }
}
