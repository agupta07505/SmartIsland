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

class StopwatchStateTest {
    @Test
    fun aRunningStopwatchCountsFromItsStart() {
        val s = StopwatchState().start(now = 1_000L)
        assertEquals(4_000L, s.elapsedMs(now = 5_000L))
    }

    @Test
    fun pauseFreezesTheTimeAndResumeContinuesFromIt() {
        val paused = StopwatchState().start(1_000L).pause(now = 6_000L)
        assertFalse(paused.running)
        assertEquals(5_000L, paused.elapsedMs(now = 99_000L))
        val resumed = paused.resume(now = 10_000L)
        assertTrue(resumed.running)
        assertEquals(8_000L, resumed.elapsedMs(now = 13_000L))
    }

    @Test
    fun theTileTogglesStartPauseResume() {
        var s = StopwatchState()
        s = s.toggle(0L); assertTrue(s.active && s.running)
        s = s.toggle(1_000L); assertTrue(s.active && !s.running)
        s = s.toggle(2_000L); assertTrue(s.active && s.running)
    }

    @Test
    fun lapsOnlyCountWhileRunning() {
        val running = StopwatchState().start(0L).lap().lap()
        assertEquals(2, running.laps)
        assertEquals(2, running.pause(1_000L).lap().laps)
    }

    @Test
    fun resetClearsEverything() {
        assertEquals(StopwatchState(), StopwatchState().start(0L).lap().reset())
        assertEquals(0L, StopwatchState().reset().elapsedMs(5_000L))
    }

    @Test
    fun theSavedFormRoundTrips() {
        val s = StopwatchState().start(1_000L).lap().pause(4_000L)
        assertEquals(s, StopwatchState.decode(s.encode()))
    }

    @Test
    fun brokenSavedFormsGiveAnEmptyStopwatch() {
        assertEquals(StopwatchState(), StopwatchState.decode(null))
        assertEquals(StopwatchState(), StopwatchState.decode("nonsense"))
        assertEquals(StopwatchState(), StopwatchState.decode("true,true,x,0,0"))
        assertEquals(StopwatchState(), StopwatchState.decode("false,false,0,0,0"))
    }
}
