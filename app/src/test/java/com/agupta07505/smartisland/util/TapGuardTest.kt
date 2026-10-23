/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapGuardTest {
    @Test
    fun aTouchThatNeverLeavesTheSlopIsStillATap() {
        val g = TapGuard(slopPx = 30f)
        g.update(0f, 0f)
        g.update(10f, 12f)
        assertFalse(g.exceeded)
        assertEquals(15.62f, g.peak, 0.01f)
    }

    @Test
    fun distanceIsMeasuredOnBothAxesTogether() {
        // 3-4-5: exactly the slop is still a tap, a hair more is not.
        assertFalse(TapGuard(5f).also { it.update(3f, 4f) }.exceeded)
        assertTrue(TapGuard(4.9f).also { it.update(3f, 4f) }.exceeded)
    }

    @Test
    fun aSidewaysDragUnderTheSwipeThresholdIsNotATap() {
        // The expanded rule must look at both axes, not only the vertical offset: 25 dp sideways with 0 dp up/down is not a tap.
        val slop = 10f
        val g = TapGuard(slop)
        g.update(25f, 0f)
        assertTrue(g.exceeded)
    }

    @Test
    fun aDragThatCameBackStaysCancelled() {
        val g = TapGuard(10f)
        g.update(40f, 0f)
        g.update(1f, 0f)
        assertTrue(g.exceeded)
        assertEquals(40f, g.peak, 0.001f)
    }

    @Test
    fun theSlopIsBelowEverySwipeThresholdSoADeadZoneExists() {
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
        // A swipe that fires always wins, whatever the slop says.
        assertEquals(TouchZone.Armed, touchZone(slopExceeded = false, liftFires = true))
    }

    @Test
    fun aStepThatFollowsTheFingerIsOnlyModeOneAndMinusOne() {
        assertTrue(stepFollowsFinger(1))
        assertTrue(stepFollowsFinger(-1))
        assertFalse(stepFollowsFinger(0))
        // 2 = nothing to open here, the plain swipe rules apply.
        assertFalse(stepFollowsFinger(2))
    }

    @Test
    fun theDeadZoneCueIsOffWhileAStepFollowsTheFinger() {
        // Past the slop, short of the swipe threshold: Dead for a plain swipe...
        assertTrue(deadZoneCue(slopExceeded = true, liftFires = false, stepFollowing = false))
        // ...but a step drag decides on lift by its own progress, so no shrink.
        assertFalse(deadZoneCue(slopExceeded = true, liftFires = false, stepFollowing = true))
        // A tap and an armed swipe never show it.
        assertFalse(deadZoneCue(slopExceeded = false, liftFires = false, stepFollowing = false))
        assertFalse(deadZoneCue(slopExceeded = true, liftFires = true, stepFollowing = false))
    }
}
