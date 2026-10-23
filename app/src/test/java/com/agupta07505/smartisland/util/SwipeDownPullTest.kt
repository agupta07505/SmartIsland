/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeDownPullTest {
    private val slop = 10f
    private val threshold = 50f

    @Test
    fun noPullInsideTheTapAreaOrWhenDraggingUp() {
        assertEquals(0f, SwipeDownPull.progress(0f, slop, threshold), 0f)
        assertEquals(0f, SwipeDownPull.progress(10f, slop, threshold), 0f)
        assertEquals(0f, SwipeDownPull.progress(-80f, slop, threshold), 0f)
    }

    @Test
    fun pullGrowsFromTheSlopToOneAtTheThreshold() {
        assertEquals(0.5f, SwipeDownPull.progress(30f, slop, threshold), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(50f, slop, threshold), 0.001f)
    }

    @Test
    fun pastTheThresholdItOnlyRubberBandsUpToTheOvershoot() {
        val a = SwipeDownPull.progress(60f, slop, threshold)
        val b = SwipeDownPull.progress(200f, slop, threshold)
        assertTrue(a > 1f && b > a)
        assertTrue(b <= 1f + SwipeDownPull.OVERSHOOT)
    }

    @Test
    fun aThresholdBelowTheSlopDoesNotDivideByZero() {
        val p = SwipeDownPull.progress(20f, 10f, 5f)
        assertTrue(p.isFinite() && p > 0f)
    }

    @Test
    fun theStretchIsTallerThanWideAndTheCardStretchesLessThanThePill() {
        val p = 1f
        assertTrue(SwipeDownPull.pillScaleY(p) > SwipeDownPull.pillScaleX(p))
        assertTrue(SwipeDownPull.expandedScaleY(p) > SwipeDownPull.expandedScaleX(p))
        assertTrue(SwipeDownPull.pillScaleY(p) > SwipeDownPull.expandedScaleY(p))
        assertEquals(1f, SwipeDownPull.pillScaleY(0f), 0f)
        assertEquals(1f, SwipeDownPull.expandedScaleX(0f), 0f)
    }

    @Test
    fun topAnchorShiftKeepsTheTopEdgeStill() {
        // Height 100 scaled by 1.1 grows 10 px around the centre; moving down 5 px puts the top edge back where it was.
        assertEquals(5f, SwipeDownPull.topAnchorShiftPx(1.1f, 100f), 0.0001f)
        assertEquals(0f, SwipeDownPull.topAnchorShiftPx(1f, 100f), 0f)
    }

    @Test
    fun deadShrinkFadesOutAsThePullGrows() {
        assertEquals(1f, SwipeDownPull.deadShrinkWeight(0f), 0f)
        assertEquals(0f, SwipeDownPull.deadShrinkWeight(1f), 0f)
        assertEquals(0f, SwipeDownPull.deadShrinkWeight(1.2f), 0f)
    }

    @Test
    fun expandedIslandOnlyLeansOnASwipeDown() {
        val density = 2f
        val max = SwipeDownPull.EXPANDED_DOWN_MAX_DP * density
        assertEquals(0f, SwipeDownPull.expandedDownOffsetPx(0f, density), 0f)
        assertEquals(0f, SwipeDownPull.expandedDownOffsetPx(-80f, density), 0f)
        // Starts at a quarter of the finger's speed ...
        assertEquals(2.26f, SwipeDownPull.expandedDownOffsetPx(10f, density), 0.05f)
        // ... grows with the drag but never passes the cap, however far the finger goes.
        val near = SwipeDownPull.expandedDownOffsetPx(100f, density)
        val far = SwipeDownPull.expandedDownOffsetPx(2000f, density)
        assertTrue(far > near && far <= max)
        assertTrue(far < 100f)
    }

    @Test
    fun expandedStretchIsSmall() {
        assertTrue(SwipeDownPull.expandedScaleY(1f) <= 1.02f)
    }

    @Test
    fun handleSqueezeGrowsWithThePullAndIsOffAtRest() {
        assertEquals(1f, SwipeDownPull.handleScaleX(0f), 0f)
        assertEquals(1f, SwipeDownPull.handleScaleY(0f), 0f)
        assertEquals(0.955f, SwipeDownPull.handleScaleX(1f), 0.0001f)
        assertEquals(1.10f, SwipeDownPull.handleScaleY(1f), 0.0001f)
        // Narrower and taller, and the rubber-band past the line squeezes a little more but stays bounded.
        val max = 1f + SwipeDownPull.OVERSHOOT
        assertTrue(SwipeDownPull.handleScaleX(max) < SwipeDownPull.handleScaleX(1f))
        assertTrue(SwipeDownPull.handleScaleY(max) > SwipeDownPull.handleScaleY(1f))
        assertTrue(SwipeDownPull.handleScaleX(max) > 0.94f)
        assertTrue(SwipeDownPull.handleScaleY(max) < 1.12f)
    }

    @Test
    fun handleProgressIsMeasuredAgainstTheFireLine() {
        // The handle's 4 dp slop and 44 dp fire line (density 1): half way is 24 dp, the line itself is 1.
        assertEquals(0.5f, SwipeDownPull.progress(24f, 4f, 44f), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(44f, 4f, 44f), 0.001f)
    }
}
