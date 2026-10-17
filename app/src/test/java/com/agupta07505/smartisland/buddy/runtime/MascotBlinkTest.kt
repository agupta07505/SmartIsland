/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotBlinkTest {
    @Test
    fun intervalDoublesInBatterySaver() {
        assertEquals(2.6f, MascotBlink.interval(false).start, 0f)
        assertEquals(5.2f, MascotBlink.interval(true).start, 0f)
        assertEquals(12.8f, MascotBlink.interval(true).endInclusive, 0f)
    }

    @Test
    fun nextDelayStaysInsideTheInterval() {
        val rng = Random(42)
        repeat(500) {
            val normal = MascotBlink.nextDelayMs(false, rng)
            assertTrue("$normal", normal in 2600L..6400L)
            val saver = MascotBlink.nextDelayMs(true, rng)
            assertTrue("$saver", saver in 5200L..12800L)
        }
    }

    @Test
    fun aboutOneBlinkInFiveIsADouble() {
        val rng = Random(7)
        val doubles = (0 until 5000).count { MascotBlink.isDouble(rng) }
        assertTrue("$doubles", doubles in 700..1100)
    }

    @Test
    fun blinkTimingsMatchTheReference() {
        assertEquals(170, MascotBlink.singleDurationMs)
        assertEquals(440, MascotBlink.doubleDurationMs)
        assertEquals(MascotBlink.SHUT_SCALE, MascotBlink.single.first().scale, 0f)
        // Every blink ends with the eyes open again.
        assertEquals(1f, MascotBlink.single.last().scale, 0f)
        assertEquals(1f, MascotBlink.double.last().scale, 0f)
    }

    @Test
    fun eyesGrowHeavySoonerLateAtNight() {
        assertEquals(36, MascotBlink.blinksBeforeSleep(14))
        assertEquals(12, MascotBlink.blinksBeforeSleep(23))
        assertEquals(12, MascotBlink.blinksBeforeSleep(5))
        assertEquals(36, MascotBlink.blinksBeforeSleep(6))
    }
}
