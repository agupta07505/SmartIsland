/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotTrackTest {
    @Test
    fun noCameraRestsInTheMiddleWhateverTheSide() {
        for (side in MascotSide.entries) {
            val t = MascotTrack.forPill(112f, 34f, 24f, null, null, side)
            assertEquals(56f, t.rest, 0.001f)
            assertNull(t.hidden)
            assertEquals(17f, t.baseline, 0.001f)
            assertEquals(false, t.mirrored)
        }
    }

    @Test
    fun leftRestSitsBesideTheCameraWithAGapAndInsideThePill() {
        val t = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Left)
        val hidden = t.hidden!!
        assertEquals(41f, hidden.start, 0.001f)
        assertEquals(71f, hidden.endInclusive, 0.001f)
        // Buddy's right edge stays left of the camera by the 4..8dp gap.
        val gap = hidden.start - (t.rest + t.size / 2f)
        assertTrue("gap $gap", gap in 3.99f..8.01f)
        assertTrue(t.rest - t.size / 2f >= 0f)
        assertEquals(false, t.mirrored)
    }

    @Test
    fun rightRestMirrorsTheLeftOneOnASymmetricPill() {
        val left = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Left)
        val right = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Right)
        assertEquals(112f - left.rest, right.rest, 0.001f)
        assertEquals(true, right.mirrored)
        assertEquals(left.hidden, right.hidden)
    }

    @Test
    fun offCentreCameraUsesTheWingOnTheBuddysOwnSide() {
        // Camera centred at 30dp in a 112dp pill: the left wing is tiny, the right one wide.
        val left = MascotTrack.forPill(112f, 34f, 24f, 30f, 30f, MascotSide.Left)
        val right = MascotTrack.forPill(112f, 34f, 24f, 30f, 30f, MascotSide.Right)
        assertTrue(left.rest - left.size / 2f >= 0f)
        assertTrue(right.rest + right.size / 2f <= 112f)
        assertTrue(right.rest > left.rest)
    }

    @Test
    fun tinyWingNeverPushesTheBuddyOffThePill() {
        val t = MascotTrack.forPill(76f, 34f, 24f, 60f, 38f, MascotSide.Left)
        assertTrue(t.rest >= t.size / 2f)
    }

    @Test
    fun scaledTrackScalesEverythingAndKeepsShape() {
        val t = MascotTrack.forPill(112f, 34f, 24f, 30f, 56f, MascotSide.Left)
        val big = t.scaled(2.5f)
        assertEquals(t.width * 2.5f, big.width, 0.001f)
        assertEquals(t.rest * 2.5f, big.rest, 0.001f)
        assertEquals(t.hidden!!.start * 2.5f, big.hidden!!.start, 0.001f)
        assertEquals(2.5f, big.scale, 0f)
        assertNotNull(big.hidden)
    }
}
