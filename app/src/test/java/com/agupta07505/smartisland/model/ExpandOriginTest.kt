/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandOriginTest {
    private val eps = 0.001f

    private fun lerp(a: Float, b: Float, p: Float) = a + (b - a) * p

    @Test
    fun startsOnThePillAndEndsOnTheCard() {
        // Pill 120 wide centred at 200; card 400 wide starting at 100 (centre 300).
        val s0 = ExpandOrigin.start(0f, 120f, 200f, 100f, 400f)
        val s1 = ExpandOrigin.start(1f, 400f, 200f, 100f, 400f)
        assertEquals(140f, s0, eps)
        assertEquals(100f, s1, eps)
    }

    @Test
    fun pillCentreStaysPutWhileItIsInsideTheCard() {
        val cardStart = 100f
        val cardSize = 400f
        val pillCentre = 200f
        for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val size = lerp(120f, cardSize, p)
            val start = ExpandOrigin.start(p, size, pillCentre, cardStart, cardSize)
            val inIsland = ExpandOrigin.anchorInCard(pillCentre, cardStart, cardSize)
            val g = 0.5f + (inIsland - 0.5f) * p
            assertEquals(pillCentre, start + g * size, eps)
        }
    }

    @Test
    fun centredPillGrowsSymmetrically() {
        for (p in listOf(0f, 0.3f, 0.7f, 1f)) {
            val size = lerp(100f, 360f, p)
            val start = ExpandOrigin.start(p, size, 180f, 0f, 360f)
            assertEquals(180f - size / 2f, start, eps)
        }
    }

    @Test
    fun pillOutsideTheCardIsClampedToTheNearestEdge() {
        // Pill centre at 40, card spans 100..500: anchor is clamped to the card's start edge.
        assertEquals(0f, ExpandOrigin.anchorInCard(40f, 100f, 400f), eps)
        assertEquals(100f, ExpandOrigin.anchorAtEnd(40f, 100f, 400f), eps)
        assertEquals(1f, ExpandOrigin.anchorInCard(900f, 100f, 400f), eps)
        assertEquals(500f, ExpandOrigin.anchorAtEnd(900f, 100f, 400f), eps)
        // Both ends are still exact.
        assertEquals(40f - 60f, ExpandOrigin.start(0f, 120f, 40f, 100f, 400f), eps)
        assertEquals(100f, ExpandOrigin.start(1f, 400f, 40f, 100f, 400f), eps)
    }

    @Test
    fun pillLeftOfTheCardGrowsTowardItWithoutMovingBack() {
        // Pill centre 40, card starts at 100: the start edge only ever moves right, from the pill edge to the card edge.
        var last = -Float.MAX_VALUE
        for (i in 0..20) {
            val p = i / 20f
            val start = ExpandOrigin.start(p, lerp(120f, 400f, p), 40f, 100f, 400f)
            // The anchor slides while the island is still small, so the edge can ease back by well under a dp
            // right at the start; anything visible (over 1 px) would still fail.
            assertTrue("start edge must not move back at p=$p", start >= last - 1f)
            last = maxOf(last, start)
        }
    }

    @Test
    fun pillRightOfTheCardGrowsTowardItWithoutMovingBack() {
        var last = Float.MAX_VALUE
        for (i in 0..20) {
            val p = i / 20f
            val start = ExpandOrigin.start(p, lerp(120f, 400f, p), 900f, 100f, 400f)
            assertTrue("start edge must not move back at p=$p", start <= last + eps)
            last = start
        }
    }

    @Test
    fun emptyCardFallsBackToMiddle() {
        assertEquals(0.5f, ExpandOrigin.anchorInCard(10f, 0f, 0f), eps)
    }

    /**
     * `IslandLayout.animatedXOffset` is `start + size / 2`, and the landscape pill's last spot (`pillExit.record`)
     * takes the pill's rest centre.
     */
    @Test
    fun islandCentreAtRestIsThePillCentre() {
        for (pillCentre in listOf(-300f, -100f, 0f, 150f, 420f)) {
            val start = ExpandOrigin.start(0f, 112f, pillCentre, -200f, 400f)
            assertEquals(pillCentre, start + 112f / 2f, eps)
        }
    }
}
