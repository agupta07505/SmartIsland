/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.model.SwipeSideways
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeSidewaysTest {
    private val density = 3f

    @Test
    fun swipeLean() {
        assertEquals(0f, SwipeSideways.leanPx(0f, density), 0f)
        assertEquals(-SwipeSideways.leanPx(80f, density), SwipeSideways.leanPx(-80f, density), 1e-4f)
        val max = SwipeSideways.maxLeanPx(density)
        assertTrue(SwipeSideways.leanPx(10_000f, density) <= max + 1e-3f)
        assertTrue(SwipeSideways.leanPx(10_000f, density) > max * 0.99f)

        assertTrue(SwipeSideways.leanPx(60f, density) > SwipeSideways.leanPx(30f, density))
        // eased, so for a short drag it is a fraction of the finger's travel (never follows it one to one)
        assertTrue(SwipeSideways.leanPx(30f, density) < 30f * SwipeSideways.FOLLOW + 1e-3f)

        val full = SwipeSideways.leanPx(10_000f, density)
        // A long drag, so the soft follow (tanh) has reached its limit; at 200 px it is only about 88 % of it
        val stay = SwipeSideways.leanPx(10_000f, density, SwipeSideways.STAY_SHARE)
        assertTrue(stay < full)
        assertEquals(SwipeSideways.maxLeanPx(density) * SwipeSideways.STAY_SHARE, stay, SwipeSideways.maxLeanPx(density) * 0.01f)
    }

    @Test
    fun progressIsClampedAndSymmetric() {
        assertEquals(0f, SwipeSideways.progress(0f, 30f), 0f)
        assertEquals(0.5f, SwipeSideways.progress(-15f, 30f), 1e-6f)
        assertEquals(1f, SwipeSideways.progress(90f, 30f), 0f)
        assertEquals(0f, SwipeSideways.progress(10f, 0f), 0f)
    }

    @Test
    fun swipeStretch() {
        assertEquals(1f, SwipeSideways.scaleX(0f), 0f)
        assertEquals(1f, SwipeSideways.scaleY(0f), 0f)
        assertTrue(SwipeSideways.scaleX(1f) > 1f && SwipeSideways.scaleX(1f) < 1.05f)
        assertTrue(SwipeSideways.scaleY(1f) < 1f && SwipeSideways.scaleY(1f) > 0.98f)

        val width = 1000f
        val sx = SwipeSideways.scaleX(1f)
        // dragged right: the shell moves right by half of the added width, so its LEFT edge is unchanged
        val shiftRight = SwipeSideways.edgeAnchorShiftPx(sx, width, 12f)
        assertEquals(0f, (-width * sx / 2f + shiftRight) - (-width / 2f), 1e-3f)
        // dragged left: the RIGHT edge is unchanged
        val shiftLeft = SwipeSideways.edgeAnchorShiftPx(sx, width, -12f)
        assertEquals(0f, (width * sx / 2f + shiftLeft) - (width / 2f), 1e-3f)
        assertEquals(0f, SwipeSideways.edgeAnchorShiftPx(sx, width, 0f), 0f)
    }

    @Test
    fun releaseLetsLeanGo() {
        assertEquals(10f, SwipeSideways.totalLeanPx(0f, 10f, 0f), 1e-6f)
        assertEquals(5f, SwipeSideways.totalLeanPx(0f, 10f, 0.5f), 1e-6f)
        assertEquals(0f, SwipeSideways.totalLeanPx(0f, 10f, 1f), 1e-6f)
        // A new finger adds its own lean on top, and a progress past 1 does not reverse the release
        assertEquals(4f, SwipeSideways.totalLeanPx(4f, 10f, 1.4f), 1e-6f)
    }

    @Test
    fun swipeCardOffsets() {
        val separation = 900f
        for (sign in intArrayOf(1, -1)) {
            for (p in floatArrayOf(0f, 0.2f, 0.5f, 0.9f, 1f)) {
                val gap = SwipeSideways.incomingX(sign, separation, p) - SwipeSideways.outgoingX(sign, separation, p)
                assertEquals(sign * separation, gap, 1e-3f)
            }
        }

        val endpoints = 900f
        // swiped left (sign +1): the new card comes from the right, the old one leaves to the left
        assertEquals(endpoints, SwipeSideways.incomingX(1, endpoints, 0f), 0f)
        assertEquals(0f, SwipeSideways.incomingX(1, endpoints, 1f), 0f)
        assertEquals(0f, SwipeSideways.outgoingX(1, endpoints, 0f), 0f)
        assertEquals(-endpoints, SwipeSideways.outgoingX(1, endpoints, 1f), 0f)
        // swiped right: mirrored
        assertEquals(-endpoints, SwipeSideways.incomingX(-1, endpoints, 0f), 0f)
        assertEquals(endpoints, SwipeSideways.outgoingX(-1, endpoints, 1f), 0f)
    }
}

class SwipeDownPullTest {
    private val slop = 10f
    private val threshold = 50f

    @Test
    fun pullProgress() {
        assertEquals(0f, SwipeDownPull.progress(0f, slop, threshold), 0f)
        assertEquals(0f, SwipeDownPull.progress(10f, slop, threshold), 0f)
        assertEquals(0f, SwipeDownPull.progress(-80f, slop, threshold), 0f)
        assertEquals(0.5f, SwipeDownPull.progress(30f, slop, threshold), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(50f, slop, threshold), 0.001f)

        val a = SwipeDownPull.progress(60f, slop, threshold)
        val b = SwipeDownPull.progress(200f, slop, threshold)
        assertTrue(a > 1f && b > a)
        assertTrue(b <= 1f + SwipeDownPull.OVERSHOOT)

        val p = SwipeDownPull.progress(20f, 10f, 5f)
        assertTrue(p.isFinite() && p > 0f)

        // the handle's 4 dp slop and 44 dp fire line (density 1): half way is 24 dp, the line itself is 1
        assertEquals(0.5f, SwipeDownPull.progress(24f, 4f, 44f), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(44f, 4f, 44f), 0.001f)
    }


    @Test
    fun cardAndPillStretch() {
        val p = 1f
        assertTrue(SwipeDownPull.pillScaleY(p) > SwipeDownPull.pillScaleX(p))
        assertTrue(SwipeDownPull.expandedScaleY(p) > SwipeDownPull.expandedScaleX(p))
        assertTrue(SwipeDownPull.pillScaleY(p) > SwipeDownPull.expandedScaleY(p))
        assertEquals(1f, SwipeDownPull.pillScaleY(0f), 0f)
        assertEquals(1f, SwipeDownPull.expandedScaleX(0f), 0f)
    }

    @Test
    fun topAnchorShiftKeepsTheTopEdgeStill() {
        // height 100 scaled by 1.1 grows 10 px around the centre; moving down 5 px puts the top edge back where it was
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
        // starts at a quarter of the finger's speed ...
        assertEquals(2.26f, SwipeDownPull.expandedDownOffsetPx(10f, density), 0.05f)
        // ... grows with the drag but never passes the cap, however far the finger goes
        val near = SwipeDownPull.expandedDownOffsetPx(100f, density)
        val far = SwipeDownPull.expandedDownOffsetPx(2000f, density)
        assertTrue(far > near && far <= max)
        assertTrue(far < 100f)
    }

    @Test
    fun handleSqueezeGrowsWithPull() {
        assertEquals(1f, SwipeDownPull.handleScaleX(0f), 0f)
        assertEquals(1f, SwipeDownPull.handleScaleY(0f), 0f)
        assertEquals(0.955f, SwipeDownPull.handleScaleX(1f), 0.0001f)
        assertEquals(1.10f, SwipeDownPull.handleScaleY(1f), 0.0001f)
        // narrower and taller, and the rubber-band past the line squeezes a little more but stays bounded
        val max = 1f + SwipeDownPull.OVERSHOOT
        assertTrue(SwipeDownPull.handleScaleX(max) < SwipeDownPull.handleScaleX(1f))
        assertTrue(SwipeDownPull.handleScaleY(max) > SwipeDownPull.handleScaleY(1f))
        assertTrue(SwipeDownPull.handleScaleX(max) > 0.94f)
        assertTrue(SwipeDownPull.handleScaleY(max) < 1.12f)
    }
}

class SwipeFadeTest {
}

class SwipeNeighbourTest {
    @Test
    fun onlyNextPrevHintNeighbour() {
        assertEquals(1, swipeNeighbourHint(SwipeAction.NextNotification))
        assertEquals(1, swipeNeighbourHint(SwipeAction.NextPrevious))
        assertEquals(-1, swipeNeighbourHint(SwipeAction.PreviousNotification))
        assertEquals(2, swipeNeighbourHint(SwipeAction.NextSibling))
        assertEquals(2, swipeNeighbourHint(SwipeAction.PreviousEvent))
    }


    @Test
    fun likelyTargetMeasuredFirst() {
        assertEquals(listOf(1, -1), neighbourProbeOrder(1))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(-1))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(2))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(0))
    }
}
