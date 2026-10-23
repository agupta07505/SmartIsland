/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    @Test
    fun aRunningTimerCountsDownToItsEnd() {
        val s = TimerState().start(now = 1_000L, lengthMs = 60_000L)
        assertEquals(41_000L, s.remainingAt(now = 20_000L))
        assertEquals(0L, s.remainingAt(now = 99_000L))
    }

    @Test
    fun pauseFreezesWhatIsLeftAndResumeContinuesFromIt() {
        val paused = TimerState().start(0L, 60_000L).pause(now = 15_000L)
        assertFalse(paused.running)
        assertEquals(45_000L, paused.remainingAt(now = 500_000L))
        val resumed = paused.resume(now = 100_000L)
        assertTrue(resumed.running)
        assertEquals(35_000L, resumed.remainingAt(now = 110_000L))
    }

    @Test
    fun toggleSwitchesBetweenPauseAndResume() {
        var s = TimerState().start(0L, 60_000L)
        s = s.toggle(1_000L); assertFalse(s.running)
        s = s.toggle(2_000L); assertTrue(s.running)
    }

    @Test
    fun addMinuteExtendsARunningAndAPausedTimer() {
        val running = TimerState().start(0L, 60_000L).addMinute(now = 10_000L)
        assertEquals(110_000L, running.remainingAt(now = 10_000L))
        val paused = TimerState().start(0L, 60_000L).pause(10_000L).addMinute(10_000L)
        assertEquals(110_000L, paused.remainingAt(now = 99_000L))
    }

    @Test
    fun addMinuteNeverPassesTheLongestLength() {
        val s = TimerState().start(0L, TimerState.MAX_MS).addMinute(now = 0L)
        assertEquals(TimerState.MAX_MS, s.remainingAt(now = 0L))
    }

    @Test
    fun theLengthIsKeptInsideTheAllowedRange() {
        assertEquals(1_000L, TimerState().start(0L, 0L).remainingAt(0L))
        assertEquals(TimerState.MAX_MS, TimerState().start(0L, Long.MAX_VALUE / 2).remainingAt(0L))
    }

    @Test
    fun aTimerIsDueOnlyWhileItRunsAndItsTimeHasCome() {
        val s = TimerState().start(0L, 5_000L)
        assertFalse(s.isDue(4_999L))
        assertTrue(s.isDue(5_000L))
        assertFalse(s.pause(1_000L).isDue(99_000L))
        assertFalse(TimerState().isDue(99_000L))
    }

    @Test
    fun aFinishedTimerStaysFinishedAndIgnoresButtons() {
        val done = TimerState().start(0L, 5_000L).finish()
        assertTrue(done.finished)
        assertEquals(0L, done.remainingAt(10_000L))
        assertEquals(done, done.addMinute(10_000L))
        assertEquals(done, done.resume(10_000L))
        assertFalse(done.isDue(10_000L))
    }

    @Test
    fun resetClearsEverything() {
        assertEquals(TimerState(), TimerState().start(0L, 5_000L).reset())
    }

    @Test
    fun theSavedFormRoundTrips() {
        val states = listOf(
            TimerState().start(1_000L, 90_000L),
            TimerState().start(1_000L, 90_000L).pause(5_000L),
            TimerState().start(1_000L, 90_000L).finish()
        )
        states.forEach { assertEquals(it, TimerState.decode(it.encode())) }
    }

    @Test
    fun brokenSavedTextGivesNoTimer() {
        assertEquals(TimerState(), TimerState.decode(null))
        assertEquals(TimerState(), TimerState.decode("nonsense"))
        assertEquals(TimerState(), TimerState.decode("true,true,x,0,false"))
        assertEquals(TimerState(), TimerState.decode("false,false,0,0,false"))
    }
}
