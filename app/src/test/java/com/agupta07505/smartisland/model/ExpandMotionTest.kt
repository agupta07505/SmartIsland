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

class ExpandMotionTest {
    // Landscape expand spring: stiffness 280, damping 0.86. Portrait: 400, 0.8.
    private val lk = 280f
    private val lz = 0.86f
    private val pk = 400f
    private val pz = 0.8f

    @Test
    fun cardContentStartsEarlyAndIsCompleteWhenTheShellIsAlmostGrown() {
        val delay = ExpandMotion.cardInDelayMs(lk, lz)
        val end = delay + ExpandMotion.cardInMs(lk, lz)
        assertTrue("delay was $delay ms", delay in 20..45)
        // The shell is 90 % grown at about 193 ms.
        assertTrue("end was $end ms", end in 170..215)
        assertTrue(end < 350)
    }

    @Test
    fun portraitIsFasterThanLandscape() {
        assertTrue(ExpandMotion.cardInDelayMs(pk, pz) < ExpandMotion.cardInDelayMs(lk, lz))
        assertTrue(
            ExpandMotion.cardInDelayMs(pk, pz) + ExpandMotion.cardInMs(pk, pz) <
                ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz)
        )
    }

    @Test
    fun pillContentIsGoneBeforeTheCardContentIsHalfWayIn() {
        val out = ExpandMotion.pillOutMs(lk, lz)
        val halfIn = ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz) / 2
        assertTrue("out was $out ms", out in 50..80)
        assertTrue(out < halfIn)
    }

    @Test
    fun followsTheSpeedSetting() {
        val relaxed = ExpandMotion.cardInDelayMs(lk * 0.5f, lz) + ExpandMotion.cardInMs(lk * 0.5f, lz)
        val normal = ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz)
        val snappy = ExpandMotion.cardInDelayMs(lk * 2f, lz) + ExpandMotion.cardInMs(lk * 2f, lz)
        assertTrue(snappy < normal && normal < relaxed)
    }

    @Test
    fun fadesNeverGetShorterThanAFewFrames() {
        assertEquals(ExpandMotion.MIN_FADE_MS, ExpandMotion.pillOutMs(1_000_000f, 1f))
        assertEquals(ExpandMotion.MIN_FADE_MS, ExpandMotion.cardInMs(1_000_000f, 1f))
    }
}
