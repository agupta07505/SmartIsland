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

class SwipeSidewaysTest {
    private val density = 3f

    @Test
    fun leanIsZeroAtRestOddAndNeverPastTheLimit() {
        assertEquals(0f, SwipeSideways.leanPx(0f, density), 0f)
        assertEquals(-SwipeSideways.leanPx(80f, density), SwipeSideways.leanPx(-80f, density), 1e-4f)
        val max = SwipeSideways.maxLeanPx(density)
        assertTrue(SwipeSideways.leanPx(10_000f, density) <= max + 1e-3f)
        assertTrue(SwipeSideways.leanPx(10_000f, density) > max * 0.99f)
    }

    @Test
    fun leanGrowsWithTheFingerAndStaysShortOfTheFinger() {
        assertTrue(SwipeSideways.leanPx(60f, density) > SwipeSideways.leanPx(30f, density))
        // Eased, so for a short drag it is a fraction of the finger's travel (never follows it one to one).
        assertTrue(SwipeSideways.leanPx(30f, density) < 30f * SwipeSideways.FOLLOW + 1e-3f)
    }

    @Test
    fun aSwipeThatKeepsTheCardLeansLess() {
        val full = SwipeSideways.leanPx(10_000f, density)
        // A long drag, so the soft follow (tanh) has reached its limit; at 200 px it is only about 88 % of it.
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
    fun stretchIsSmallAndWidensWhileSqueezingTheHeight() {
        assertEquals(1f, SwipeSideways.scaleX(0f), 0f)
        assertEquals(1f, SwipeSideways.scaleY(0f), 0f)
        assertTrue(SwipeSideways.scaleX(1f) > 1f && SwipeSideways.scaleX(1f) < 1.05f)
        assertTrue(SwipeSideways.scaleY(1f) < 1f && SwipeSideways.scaleY(1f) > 0.98f)
    }

    @Test
    fun theTrailingEdgeStaysWhereItIs() {
        val width = 1000f
        val sx = SwipeSideways.scaleX(1f)
        // Dragged right: the shell moves right by half of the added width, so its LEFT edge is unchanged.
        val shiftRight = SwipeSideways.edgeAnchorShiftPx(sx, width, 12f)
        assertEquals(0f, (-width * sx / 2f + shiftRight) - (-width / 2f), 1e-3f)
        // Dragged left: the RIGHT edge is unchanged.
        val shiftLeft = SwipeSideways.edgeAnchorShiftPx(sx, width, -12f)
        assertEquals(0f, (width * sx / 2f + shiftLeft) - (width / 2f), 1e-3f)
        assertEquals(0f, SwipeSideways.edgeAnchorShiftPx(sx, width, 0f), 0f)
    }

    @Test
    fun releaseLetsTheLeanGoOnTheSwapProgress() {
        assertEquals(10f, SwipeSideways.totalLeanPx(0f, 10f, 0f), 1e-6f)
        assertEquals(5f, SwipeSideways.totalLeanPx(0f, 10f, 0.5f), 1e-6f)
        assertEquals(0f, SwipeSideways.totalLeanPx(0f, 10f, 1f), 1e-6f)
        // A new finger adds its own lean on top, and a progress past 1 does not reverse the release.
        assertEquals(4f, SwipeSideways.totalLeanPx(4f, 10f, 1.4f), 1e-6f)
    }

    @Test
    fun theTwoCardsOfASwapAreExactlyOneCardApart() {
        val w = 900f
        for (sign in intArrayOf(1, -1)) {
            for (p in floatArrayOf(0f, 0.2f, 0.5f, 0.9f, 1f)) {
                val gap = SwipeSideways.incomingX(sign, w, p) - SwipeSideways.outgoingX(sign, w, p)
                assertEquals(sign * w, gap, 1e-3f)
            }
        }
    }

    @Test
    fun theCardsStartAndEndOnTheRightSides() {
        val w = 900f
        // Swiped left (sign +1): the new card comes from the right, the old one leaves to the left.
        assertEquals(w, SwipeSideways.incomingX(1, w, 0f), 0f)
        assertEquals(0f, SwipeSideways.incomingX(1, w, 1f), 0f)
        assertEquals(0f, SwipeSideways.outgoingX(1, w, 0f), 0f)
        assertEquals(-w, SwipeSideways.outgoingX(1, w, 1f), 0f)
        // Swiped right: mirrored.
        assertEquals(-w, SwipeSideways.incomingX(-1, w, 0f), 0f)
        assertEquals(w, SwipeSideways.outgoingX(-1, w, 1f), 0f)
    }
}
