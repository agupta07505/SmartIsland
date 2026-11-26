/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRulesTest {
    private val now = 1_000_000_000L
    private val min = 60_000L

    @Test fun islandLooksAheadOneDay() {
        assertTrue(CalendarRules.islandShows(now + CalendarRules.LOOK_AHEAD_MS, now))
        assertFalse(CalendarRules.islandShows(now + CalendarRules.LOOK_AHEAD_MS + 1, now))
        assertFalse(CalendarRules.islandShows(now - 1, now))
    }

    @Test fun pickNextTakesSoonest() {
        val past = NextEvent("Past", now - min, now + min)
        val later = NextEvent("Later", now + 90 * min, now + 120 * min)
        val soon = NextEvent("Soon", now + 10 * min, now + 40 * min)
        assertEquals(soon, CalendarRules.pickNext(listOf(past, later, soon), now))
        assertNull(CalendarRules.pickNext(listOf(past), now))
        assertNull(CalendarRules.pickNext(emptyList(), now))
    }

    @Test fun shortTitleNeverEmpty() {
        assertEquals("Standup", CalendarRules.shortTitle("  Standup ", 9))
        assertEquals("Team  sync".replace("  ", " "), CalendarRules.shortTitle("Team   sync", 9))
        assertEquals("Quarterly\u2026", CalendarRules.shortTitle("Quarterly planning", 10))
        assertEquals("Event", CalendarRules.shortTitle("   ", 9))
    }
}

class DayMomentTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(h: Int, m: Int, day: Int = 15): Long =
        Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.MARCH, day, h, m, 0) }.timeInMillis

    @Test
    fun directionsAndTimes() {
        assertFalse(DayMoment.Sunrise.towardsDown)
        assertFalse(DayMoment.Afternoon.towardsDown)
        assertTrue(DayMoment.Sunset.towardsDown)
        assertTrue(DayMoment.Midnight.towardsDown)
        assertEquals(6 to 0, DayMoment.Sunrise.hour to DayMoment.Sunrise.minute)
        assertEquals(12 to 0, DayMoment.Afternoon.hour to DayMoment.Afternoon.minute)
        assertEquals(17 to 30, DayMoment.Sunset.hour to DayMoment.Sunset.minute)
        assertEquals(23 to 0, DayMoment.Midnight.hour to DayMoment.Midnight.minute)
    }

    @Test fun nextMomentAfterNowOrTomorrowsSunrise() {
        assertEquals(DayMoment.Sunrise to at(6, 0), DayMoments.next(at(3, 10), zone))
        assertEquals(DayMoment.Afternoon to at(12, 0), DayMoments.next(at(6, 0), zone))
        assertEquals(DayMoment.Afternoon to at(12, 0), DayMoments.next(at(9, 45), zone))
        assertEquals(DayMoment.Sunset to at(17, 30), DayMoments.next(at(12, 0), zone))
        assertEquals(DayMoment.Midnight to at(23, 0), DayMoments.next(at(17, 31), zone))
        assertEquals(DayMoment.Sunrise to at(6, 0, 16), DayMoments.next(at(23, 0), zone))
        assertEquals(DayMoment.Sunrise to at(6, 0, 16), DayMoments.next(at(23, 59), zone))
    }

    @Test
    fun previousDayMoment() {
        val cases = listOf(
            DayMoment.Sunrise to DayMoment.Midnight,
            DayMoment.Afternoon to DayMoment.Sunrise,
            DayMoment.Sunset to DayMoment.Afternoon,
            DayMoment.Midnight to DayMoment.Sunset
        )
        for ((input, expected) in cases) {
            assertEquals("$input", expected, DayMoments.previous(input))
        }

        // sunrise and sunset share the twilight glyph, but never follow each other, so a tick always changes what is drawn
        val glyph = mapOf(
            DayMoment.Sunrise to "twilight", DayMoment.Sunset to "twilight",
            DayMoment.Afternoon to "sun", DayMoment.Midnight to "moon"
        )
        DayMoment.values().forEach { assertFalse(glyph[it] == glyph[DayMoments.previous(it)]) }
    }

    @Test
    fun incomingGlyphPose() {
        assertEquals(MomentMotion.RiseFromBottom, DayMoments.motion(DayMoment.Sunrise))
        assertEquals(MomentMotion.SinkIntoBottom, DayMoments.motion(DayMoment.Sunset))
        // sunrise: the glyph itself never moves (the horizon line is fixed); the sun inside it comes up from hidden to risen
        for (p in listOf(0f, 0.5f, 1f)) assertEquals(0f, DayMoments.incomingPose(DayMoment.Sunrise, p).offsetShare, 0.001f)
        assertEquals(0f, DayMoments.incomingPose(DayMoment.Sunrise, 0f).lift, 0.001f)
        assertEquals(1f, DayMoments.incomingPose(DayMoment.Sunrise, 1f).lift, 0.001f)
        assertEquals(1f, DayMoments.incomingPose(DayMoment.Sunrise, 1f).alpha, 0.001f)
        // sunset: the old sun ends hidden below the bottom edge
        assertEquals(0f, DayMoments.outgoingPose(DayMoment.Sunset, 0f).offsetShare, 0.001f)
        assertTrue(DayMoments.outgoingPose(DayMoment.Sunset, 1f).offsetShare >= 1f)
        assertEquals(1f, DayMoments.incomingPose(DayMoment.Sunset, 1f).alpha, 0.001f)

        for (m in listOf(DayMoment.Afternoon, DayMoment.Midnight)) {
            assertEquals(MomentMotion.GrowFromPrevious, DayMoments.motion(m))
            val start = DayMoments.incomingPose(m, 0f)
            val end = DayMoments.incomingPose(m, 1f)
            assertEquals(0f, start.offsetShare, 0.001f)
            assertTrue(start.scale < 1f)
            assertEquals(1f, end.scale, 0.001f)
            assertEquals(1f, end.alpha, 0.001f)
        }

        for (m in DayMoment.values()) {
            val a = DayMoments.incomingPose(m, -0.1f)
            val b = DayMoments.incomingPose(m, 0f)
            assertEquals(a, b)
        }
    }

    @Test
    fun glyphsMatchTheirMoments() {
        assertEquals(MomentGlyph.Horizon, DayMoments.glyph(DayMoment.Sunrise))
        assertEquals(MomentGlyph.Horizon, DayMoments.glyph(DayMoment.Sunset))
        assertEquals(MomentGlyph.Sun, DayMoments.glyph(DayMoment.Afternoon))
        assertEquals(MomentGlyph.Moon, DayMoments.glyph(DayMoment.Midnight))
    }
}

class HourChimeTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")

    private fun at(h: Int, m: Int, s: Int = 0, ms: Int = 0, zone: TimeZone = ist): Long =
        Calendar.getInstance(zone).also { it.clear(); it.set(2026, Calendar.OCTOBER, 11, h, m, s); it.set(Calendar.MILLISECOND, ms) }.timeInMillis

    @Test
    fun nextHourBoundary() {
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 0, 0, 1), ist))
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 59, 59, 999), ist))
        assertEquals(at(13, 0), HourChime.nextFullHourMs(at(12, 30), ist))
        assertEquals(at(14, 0), HourChime.nextFullHourMs(at(13, 0), ist))

        val midnightNext = HourChime.nextFullHourMs(at(23, 15), ist)
        assertEquals(0, HourChime.hourOf(midnightNext, ist))
        assertTrue(midnightNext > at(23, 15))

        val nepal = TimeZone.getTimeZone("Asia/Kathmandu")
        val halfHourNext = HourChime.nextFullHourMs(at(9, 20, zone = nepal), nepal)
        assertEquals(at(10, 0, zone = nepal), halfHourNext)
    }

    @Test
    fun hourOfReadsTheLocalHour() {
        assertEquals(13, HourChime.hourOf(at(13, 0), ist))
        assertEquals(1, HourChime.hourOf(at(1, 0), ist))
    }

    @Test
    fun previousHourWrapsAtMidnight() {
        assertEquals(23, HourChime.previousHour(0))
        assertEquals(0, HourChime.previousHour(1))
        assertEquals(12, HourChime.previousHour(13))
    }

    @Test fun hourLabelsIn24hAnd12h() {
        assertEquals("01", HourChime.hourLabel(1, true))
        assertEquals("13", HourChime.hourLabel(13, true))
        assertEquals("00", HourChime.hourLabel(0, true))
        assertEquals("1", HourChime.hourLabel(1, false))
        assertEquals("1", HourChime.hourLabel(13, false))
        assertEquals("12", HourChime.hourLabel(0, false))
        assertEquals("12", HourChime.hourLabel(12, false))
    }

    @Test
    fun chimeLabelsPairTheOldAndNewHour() {
        val a = hourChimeLabels(13, true)
        assertEquals("12", a.oldHour); assertEquals("13", a.newHour)
        val b = hourChimeLabels(1, true)
        assertEquals("00", b.oldHour); assertEquals("01", b.newHour)
        val c = hourChimeLabels(0, false)
        assertEquals("11", c.oldHour); assertEquals("12", c.newHour)
        val d = hourChimeLabels(13, false)
        assertEquals("12", d.oldHour); assertEquals("1", d.newHour)
    }
}

class StopwatchStateTest {
    @Test
    fun aRunningStopwatchCountsFromItsStart() {
        val s = StopwatchState().start(now = 1_000L)
        assertEquals(4_000L, s.elapsedMs(now = 5_000L))
    }

    @Test
    fun pauseFreezesResumeContinues() {
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

class TimerRingMemoryTest {
    private val id = "com.clock|7"

    @Test
    fun ringKeepsArcOnReseen() {
        val m = TimerRingMemory()
        m.feed(id, 60_000L, null, 0L)
        m.feed(id, 30_000L, null, 30_000L)
        assertEquals(0.5f, m.fraction(id, 30_000L, 30_000L)!!, 0.001f)
        // glyph recreated after hide and reveal: same total, same arc
        assertEquals(0.25f, m.fraction(id, 15_000L, 45_000L)!!, 0.001f)
    }

    @Test
    fun timerIdentity() {
        val m = TimerRingMemory()
        m.feed(TimerRingMemory.idOf("0|com.clock|7|null|1000"), 60_000L, null, 0L)
        m.release(TimerRingMemory.idOf("0|com.clock|7|null|1000"), 10_000L)
        val newId = TimerRingMemory.idOf("0|com.clock|7|tag|1000")
        m.feed(newId, 50_000L, null, 10_100L)
        assertEquals(50_000f / 60_000f, m.fraction(newId, 50_000L, 10_100L)!!, 0.001f)

        assertEquals("com.clock|7", TimerRingMemory.idOf("0|com.clock|7|null|10123"))
        assertEquals("smartisland.timer", TimerRingMemory.idOf("smartisland.timer"))
    }

    @Test
    fun pauseAndResumeKeepTheTotal() {
        val m = TimerRingMemory()
        m.feed(id, 60_000L, null, 0L)
        m.feed(id, 40_000L, null, 20_000L)
        m.feed(id, 40_000L, null, 60_000L)
        assertEquals(40_000f / 60_000f, m.fraction(id, 40_000L, 60_000L)!!, 0.001f)
    }

    @Test
    fun timeAddedGrowsTheTotal() {
        val m = TimerRingMemory()
        m.feed(id, 60_000L, null, 0L)
        m.feed(id, 30_000L, null, 30_000L)
        m.feed(id, 90_000L, null, 31_000L)
        assertEquals(1f, m.fraction(id, 90_000L, 31_000L)!!, 0.001f)
        assertEquals(0.5f, m.fraction(id, 45_000L, 31_000L)!!, 0.001f)
    }

    @Test
    fun realTotalFromTheSourceWinsOverWhatWasSeen() {
        val m = TimerRingMemory()
        m.feed(id, 30_000L, 120_000L, 0L)
        assertEquals(0.25f, m.fraction(id, 30_000L, 0L)!!, 0.001f)
    }

    @Test
    fun finishedTimerStartsFreshWhenItRestarts() {
        val m = TimerRingMemory()
        m.feed(id, 60_000L, null, 0L)
        m.feed(id, 0L, null, 60_000L)
        assertEquals(0f, m.fraction(id, 0L, 60_000L)!!, 0.001f)
        m.feed(id, 20_000L, null, 70_000L)
        assertEquals(1f, m.fraction(id, 20_000L, 70_000L)!!, 0.001f)
    }

    @Test
    fun removedTimerIsForgottenAfterTheGrace() {
        val m = TimerRingMemory()
        m.feed(id, 60_000L, null, 0L)
        m.release(id, 1_000L)
        assertTrue(m.has(id, 1_000L + TimerRingMemory.GRACE_MS))
        assertFalse(m.has(id, 1_001L + TimerRingMemory.GRACE_MS))
        m.feed(id, 10_000L, null, 20_000L)
        assertEquals(1f, m.fraction(id, 10_000L, 20_000L)!!, 0.001f)
    }

    @Test
    fun firstSeenMidRunStartsFull() {
        val m = TimerRingMemory()
        m.feed(id, 25_000L, null, 0L)
        assertEquals(1f, m.fraction(id, 25_000L, 0L)!!, 0.001f)
    }

    @Test
    fun unknownTimerHasNoRing() {
        assertNull(TimerRingMemory().fraction(id, 5_000L, 0L))
    }

    @Test
    fun timersFromOneAppKeepApart() {
        val m = TimerRingMemory()
        m.feed("com.clock|1", 60_000L, null, 0L)
        m.feed("com.clock|2", 600_000L, null, 0L)
        assertEquals(0.5f, m.fraction("com.clock|1", 30_000L, 0L)!!, 0.001f)
        assertEquals(0.05f, m.fraction("com.clock|2", 30_000L, 0L)!!, 0.001f)
    }
}

class TimerStateTest {
    @Test
    fun runningTimerCountsDownToItsEnd() {
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
    fun addMinuteCapsAtLongest() {
        val s = TimerState().start(0L, TimerState.MAX_MS).addMinute(now = 0L)
        assertEquals(TimerState.MAX_MS, s.remainingAt(now = 0L))
    }

    @Test
    fun lengthIsKeptInsideTheAllowedRange() {
        assertEquals(1_000L, TimerState().start(0L, 0L).remainingAt(0L))
        assertEquals(TimerState.MAX_MS, TimerState().start(0L, Long.MAX_VALUE / 2).remainingAt(0L))
    }

    @Test
    fun timerIsDueOnlyWhileItRunsAndItsTimeHasCome() {
        val s = TimerState().start(0L, 5_000L)
        assertFalse(s.isDue(4_999L))
        assertTrue(s.isDue(5_000L))
        assertFalse(s.pause(1_000L).isDue(99_000L))
        assertFalse(TimerState().isDue(99_000L))
    }

    @Test
    fun finishedTimerStaysFinishedAndIgnoresButtons() {
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
    fun timerStateCodec() {
        val states = listOf(
            TimerState().start(1_000L, 90_000L),
            TimerState().start(1_000L, 90_000L).pause(5_000L),
            TimerState().start(1_000L, 90_000L).finish()
        )
        states.forEach { assertEquals(it, TimerState.decode(it.encode())) }

        assertEquals(TimerState(), TimerState.decode(null))
        assertEquals(TimerState(), TimerState.decode("nonsense"))
        assertEquals(TimerState(), TimerState.decode("true,true,x,0,false"))
        assertEquals(TimerState(), TimerState.decode("false,false,0,0,false"))

        val s = TimerState().start(0L, 90_000L)
        assertEquals(90_000L, TimerState.decode(s.encode()).totalMs)
        val old = TimerState.decode("true,false,0,45000,false")
        assertTrue(old.active)
        assertEquals(45_000L, old.remainingMs)
        assertEquals(0L, old.totalMs)
    }

    @Test
    fun startKeepsLengthAddedGrows() {
        val s = TimerState().start(0L, 60_000L)
        assertEquals(60_000L, s.totalMs)
        assertEquals(120_000L, s.addMinute(now = 5_000L).totalMs)
        assertEquals(60_000L, s.pause(now = 5_000L).totalMs)
    }
}
