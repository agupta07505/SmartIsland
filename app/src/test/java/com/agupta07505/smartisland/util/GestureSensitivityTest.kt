/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun normalKeepsTheOriginalNumbers() {
        assertEquals(16f, n.scaled(16f), 0f)
        assertEquals(35f, n.scaled(35f), 0f)
        assertEquals(10f, n.scaled(10f), 0f)
        assertEquals(1f, n.dominance, 0f)
    }

    @Test
    fun highNeedsLessDistanceAndLowNeedsMore() {
        assertTrue(high.scaled(16f) < n.scaled(16f))
        assertTrue(low.scaled(16f) > n.scaled(16f))
        assertTrue(high.scaled(35f) < 35f && low.scaled(35f) > 35f)
    }

    @Test
    fun normalPillSwipeMatchesTheOldRule() {
        // Either axis past the threshold; the longer axis wins, a tie goes vertical.
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
    fun lowIgnoresDiagonalDragsAndKeepsClearOnes() {
        val t = low.scaled(16f)
        assertNull(low.pillSwipe(t + 5f, t + 2f, t))
        assertNull(low.pillSwipe(-(t + 5f), -(t + 2f), t))
        assertEquals(PillSwipeDirection.Left, low.pillSwipe(-(t * 2f), t * 0.5f, t))
        assertEquals(PillSwipeDirection.Up, low.pillSwipe(t * 0.5f, -(t * 2f), t))
    }

    @Test
    fun highFiresOnAShortSwipe() {
        val shortDrag = 13f
        assertNull(n.pillSwipe(shortDrag, 0f, n.scaled(16f)))
        assertEquals(PillSwipeDirection.Right, high.pillSwipe(shortDrag, 0f, high.scaled(16f)))
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
