/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerClockTest {
    @Test
    fun secondsAreTruncatedLikeTheShadeChronometer() {
        assertEquals(4L, TimerClock.secondsLeft(4_999L))
        assertEquals(5L, TimerClock.secondsLeft(5_000L))
        assertEquals(0L, TimerClock.secondsLeft(999L))
        assertEquals(0L, TimerClock.secondsLeft(-50L))
    }

    @Test
    fun legacyCardsKeepTheirOwnRounding() {
        assertEquals(5L, TimerClock.displaySeconds(4_600L, exact = false, legacyBiasMs = 500L))
        assertEquals(5L, TimerClock.displaySeconds(4_100L, exact = false, legacyBiasMs = 999L))
        assertEquals(4L, TimerClock.displaySeconds(4_999L, exact = true, legacyBiasMs = 999L))
    }

    @Test
    fun tickLandsJustAfterTheSecondChanges() {
        assertEquals(405L, TimerClock.msToNextTick(4_400L))
        assertEquals(5L, TimerClock.msToNextTick(4_000L))
    }

    @Test
    fun wholeSecondReadingShowsThatSecondNow() {
        val target = TimerClock.targetFromShownSeconds(10_000L, 299L)
        assertEquals(299L, TimerClock.secondsLeft(target - 10_000L))
        // and moves on within a second
        assertEquals(298L, TimerClock.secondsLeft(target - 11_000L))
    }

    @Test
    fun pauseValueIsTheBaseMinusThePostTimeNotTheDriftedText() {
        // Paused with 12:00.250 left; the post is 3.4 s old when inflated, so the view prints 11:56 (truncated).
        val postElapsed = 1_000_000L
        val base = postElapsed + 720_250L
        val shown = (720_250L - 3_400L) / 1000L
        assertEquals(720_250L, TimerClock.frozenFromChronometer(base, postElapsed, shown, 3_400L))
    }

    @Test
    fun unrelatedBaseIsRejectedSoTheTextIsUsed() {
        // The app does not use the base as a deadline: base - postElapsed disagrees with the printed text.
        assertNull(TimerClock.frozenFromChronometer(5_000_000L, 1_000_000L, 30L, 100L))
        assertNull(TimerClock.frozenFromChronometer(900_000L, 1_000_000L, null, 0L))
    }

    @Test
    fun pausedStopwatchIsRecoveredExactlyWhateverTheAgeOfThePost() {
        // Paused at 05:07.300. Inflated `age` ms after the post the stopped chronometer prints elapsed + age.
        val postElapsed = 2_000_000L
        val base = postElapsed - 307_300L
        for (age in listOf(0L, 40L, 999L, 1_000L, 25_000L, 600_000L)) {
            val shown = (307_300L + age) / 1000L
            assertEquals("age $age", 307_300L, TimerClock.elapsedFromChronometer(base, postElapsed, shown, age))
        }
    }

    @Test
    fun stopwatchBaseThatDoesNotMatchThePrintedTextIsRejected() {
        assertNull(TimerClock.elapsedFromChronometer(1_000_000L, 2_000_000L, 5L, 0L))
        assertNull(TimerClock.elapsedFromChronometer(2_500_000L, 2_000_000L, null, 0L))
    }

    @Test
    fun countUpTickLandsJustAfterTheBoundary() {
        assertEquals(605L, TimerClock.msToNextTickUp(400L))
        assertEquals(1_005L, TimerClock.msToNextTickUp(3_000L))
    }
}
