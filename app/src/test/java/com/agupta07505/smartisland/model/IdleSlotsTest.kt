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

class IdleSlotsTest {
    @Test
    fun glyphScaleIsKeptInRange() {
        assertEquals(70, IdleSlots.normalizeGlyphScale(10))
        assertEquals(100, IdleSlots.normalizeGlyphScale(100))
        assertEquals(150, IdleSlots.normalizeGlyphScale(900))
        assertTrue(IdleSlots.GLYPH_SCALES.all { it in IdleSlots.MIN_GLYPH_SCALE..IdleSlots.MAX_GLYPH_SCALE })
        assertTrue(IdleSlots.DEFAULT_GLYPH_SCALE in IdleSlots.GLYPH_SCALES)
    }

    @Test
    fun dateIsDayAndAbbreviatedMonth() {
        assertEquals("dd", IdleSlots.DAY_PATTERN)
        assertEquals("MMM", IdleSlots.MONTH_PATTERN)
        val d = java.text.SimpleDateFormat(IdleSlots.DAY_PATTERN, java.util.Locale.US)
        val m = java.text.SimpleDateFormat(IdleSlots.MONTH_PATTERN, java.util.Locale.US)
        val feb5 = java.util.GregorianCalendar(2026, java.util.Calendar.FEBRUARY, 5).time
        assertEquals("05", d.format(feb5))
        assertEquals("Feb", m.format(feb5))
    }

    @Test
    fun alarmGlyphAndHourAlternate() {
        assertFalse(IdleSlots.alarmShowsHour(0))
        assertTrue(IdleSlots.alarmShowsHour(1))
        assertFalse(IdleSlots.alarmShowsHour(2))
        assertTrue(IdleSlots.alarmShowsHour(3))
    }

    @Test
    fun alarmHourFollowsTheClockFormat() {
        assertEquals("07", IdleSlots.alarmHourText(7, true))
        assertEquals("19", IdleSlots.alarmHourText(19, true))
        assertEquals("7", IdleSlots.alarmHourText(7, false))
        assertEquals("7", IdleSlots.alarmHourText(19, false))
        assertEquals("12", IdleSlots.alarmHourText(0, false))
        assertEquals("12", IdleSlots.alarmHourText(12, false))
        assertEquals("00", IdleSlots.alarmHourText(0, true))
        assertEquals("05", IdleSlots.alarmMinuteText(5))
        assertEquals("30", IdleSlots.alarmMinuteText(30))
    }

    @Test
    fun batteryPowerTexts() {
        assertEquals("8.4W", IdleSlots.shortWatts(8.4))
        assertEquals("18W", IdleSlots.shortWatts(18.2))
        assertEquals("10W", IdleSlots.shortWatts(9.96))
        assertEquals("36°", IdleSlots.shortBatteryTemp(362, false))
        assertEquals("97°", IdleSlots.shortBatteryTemp(362, true))
        assertTrue(IdleSlots.batteryShowsPower(true, 5.0))
        assertFalse(IdleSlots.batteryShowsPower(true, null))
        assertFalse(IdleSlots.batteryShowsPower(false, 5.0))
    }

    @Test
    fun speedLeadFollowsTheFasterSide() {
        assertEquals(IdleSlots.SpeedLead.Upload, IdleSlots.speedLead(5_000, 80_000))
        assertEquals(IdleSlots.SpeedLead.Download, IdleSlots.speedLead(900_000, 80_000))
        assertEquals(IdleSlots.SpeedLead.None, IdleSlots.speedLead(200, 300))
        assertEquals(IdleSlots.SpeedLead.None, IdleSlots.speedLead(5_000, 5_000))
        assertTrue(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Upload, right = false))
        assertTrue(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Download, right = true))
        assertFalse(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Download, right = false))
    }

    @Test
    fun sideTickSkipsAlarmGlyphHourAndSelfTickingSlots() {
        val none = IdleSlots.SpeedLead.None
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = true, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Date, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = true, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))
    }

    @Test
    fun tickProgressHopsThriceAndRests() {
        assertEquals(0f, IdleSlots.tickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.tickProgress(1f), 1e-6f)
        // The first two hops take 1 / 3.6 of the time each: each rests exactly on its step boundary (1/3 and 2/3) before the next starts.
        assertEquals(1f / 3f, IdleSlots.tickProgress(0.20f), 1e-4f)
        assertEquals(1f / 3f, IdleSlots.tickProgress(0.27f), 1e-4f)
        assertEquals(2f / 3f, IdleSlots.tickProgress(0.50f), 1e-4f)
        assertEquals(2f / 3f, IdleSlots.tickProgress(0.55f), 1e-4f)
        var last = 0f
        for (i in 0..100) {
            val v = IdleSlots.tickProgress(i / 100f)
            assertTrue(v >= last - 1e-6f)
            last = v
        }
    }

    @Test
    fun theLastHopIsLongerAndSofterThanTheFirst() {
        // The switch takes 1.2 times as long as three equal hops, so the first hops keep their speed.
        assertEquals(1.2f, IdleSlots.switchTimeScale(), 1e-6f)
        // Time (share of the whole) a hop needs to cover 90% of its own step.
        fun timeTo(from: Float, to: Float): Float {
            var f = 0f
            while (IdleSlots.tickProgress(f) < from + 0.9f * (to - from) && f < 1f) f += 0.0005f
            return f
        }
        val firstHop = timeTo(0f, 1f / 3f)
        val lastHopStart = 0.5556f
        val lastHop = timeTo(2f / 3f, 1f) - lastHopStart
        assertTrue(lastHop > firstHop * 1.4f)
        // Softer: the last hop's speed at its start is lower than the first hop's.
        val firstSpeed = IdleSlots.tickProgress(0.01f) / 0.01f
        val lastSpeed = (IdleSlots.tickProgress(lastHopStart + 0.01f) - IdleSlots.tickProgress(lastHopStart)) / 0.01f
        assertTrue(lastSpeed < firstSpeed)
    }

    @Test
    fun settledTickGoesPastItsPlaceThenLandsExactly() {
        val tick = 560
        assertEquals(0f, IdleSlots.settledTickProgress(0f, tick), 1e-6f)
        assertEquals(1f, IdleSlots.settledTickProgress(1f, tick), 1e-6f)
        val share = tick.toFloat() / (tick + IdleSlots.SETTLE_MS)
        // The snap ends at the overshoot, the settle then eases back without ever dropping below 1.
        assertEquals(1f + IdleSlots.SETTLE_OVERSHOOT, IdleSlots.settledTickProgress(share, tick), 1e-4f)
        var max = 0f
        var last = Float.MAX_VALUE
        for (i in 0..400) {
            val f = i / 400f
            val v = IdleSlots.settledTickProgress(f, tick)
            if (v > max) max = v
            if (f >= share) {
                assertTrue(v >= 1f - 1e-6f)
                assertTrue(v <= last + 1e-6f)
                last = v
            }
        }
        assertEquals(1f + IdleSlots.SETTLE_OVERSHOOT, max, 1e-3f)
    }

    @Test
    fun stretchFollowsTheSpeedOfTheSnap() {
        val tick = 560
        val share = tick.toFloat() / (tick + IdleSlots.SETTLE_MS)
        // None while the glyph hangs back, none while it settles, a clear stretch right after the snap starts.
        assertEquals(0f, IdleSlots.stretchIntensity(0f, tick), 1e-6f)
        assertEquals(0f, IdleSlots.stretchIntensity(share * IdleSlots.HEAVY_WINDUP_SHARE, tick), 1e-6f)
        assertEquals(0f, IdleSlots.stretchIntensity(share + 0.02f, tick), 1e-6f)
        assertEquals(0f, IdleSlots.stretchIntensity(1f, tick), 1e-6f)
        var peak = 0f
        var at = 0f
        for (i in 0..400) {
            val f = i / 400f
            val v = IdleSlots.stretchIntensity(f, tick)
            assertTrue(v in 0f..1f)
            if (v > peak) { peak = v; at = f }
        }
        assertTrue(peak > 0.6f)
        assertTrue(at > share * IdleSlots.HEAVY_WINDUP_SHARE && at < share * 0.8f)
    }

    @Test
    fun theRightSlotLagsByAFewTensOfMilliseconds() {
        assertTrue(IdleSlots.STAGGER_MS in 60..90)
        assertTrue(IdleSlots.SETTLE_MS in 80..100)
    }

    @Test
    fun heavyTickHangsBackSlowlyThenSnapsThrough() {
        assertEquals(0f, IdleSlots.heavyTickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.heavyTickProgress(1f), 1e-6f)
        // The pull-back ends exactly at -depth, at the end of the wind-up share.
        assertEquals(-IdleSlots.HEAVY_WINDUP_DEPTH, IdleSlots.heavyTickProgress(IdleSlots.HEAVY_WINDUP_SHARE), 1e-5f)
        // Slow: at half of the wind-up it has not yet gone half way back (smooth start), and it never goes below -depth.
        var min = 0f
        var last = Float.NEGATIVE_INFINITY
        for (i in 0..200) {
            val f = i / 200f
            val v = IdleSlots.heavyTickProgress(f)
            if (v < min) min = v
            assertTrue(v <= 1f + 1e-6f)
            if (f >= IdleSlots.HEAVY_WINDUP_SHARE) { assertTrue(v >= last - 1e-6f); last = v }
        }
        assertEquals(-IdleSlots.HEAVY_WINDUP_DEPTH, min, 1e-5f)
        // The snap after the wind-up is much faster than the wind-up: more distance in a tenth of the time.
        val windStep = IdleSlots.heavyTickProgress(0.04f) - IdleSlots.heavyTickProgress(0f)
        val snapStep = IdleSlots.heavyTickProgress(0.46f) - IdleSlots.heavyTickProgress(0.40f)
        assertTrue(snapStep > -windStep * 3f)
    }

    @Test
    fun dayMomentsNeverTakeTheSideTick() {
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.DayMoments, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.None))
    }

    @Test
    fun beatPlaysEveryTimeUnlessItWaitsForAChange() {
        // Off (the default): the beat plays whether or not anything changed.
        assertTrue(IdleSlots.beatDue(onlyOnChange = false, changed = false))
        assertTrue(IdleSlots.beatDue(onlyOnChange = false, changed = true))
        // On: it plays only when a value changed since the last beat.
        assertFalse(IdleSlots.beatDue(onlyOnChange = true, changed = false))
        assertTrue(IdleSlots.beatDue(onlyOnChange = true, changed = true))
    }

    @Test
    fun beatIsPausedWhileThePillCannotBeSeen() {
        assertFalse(IdleSlots.beatPaused(screenOn = true, islandOpen = false))
        assertTrue(IdleSlots.beatPaused(screenOn = false, islandOpen = false))
        assertTrue(IdleSlots.beatPaused(screenOn = true, islandOpen = true))
        assertTrue(IdleSlots.beatPaused(screenOn = false, islandOpen = true))
    }

    @Test
    fun ticksFollowTheWeatherIconsMotionRule() {
        assertTrue(IdleSlots.ticksAnimate(saverOn = false, systemAnimationsOff = false))
        assertFalse(IdleSlots.ticksAnimate(saverOn = true, systemAnimationsOff = false))
        assertFalse(IdleSlots.ticksAnimate(saverOn = false, systemAnimationsOff = true))
        assertFalse(IdleSlots.ticksAnimate(saverOn = true, systemAnimationsOff = true))
        // The same answer as the weather icon with its Battery Saver pause on, so the two never disagree.
        for (saver in listOf(false, true)) for (off in listOf(false, true)) {
            assertEquals(
                WeatherDisplay.iconAnimates(enabled = true, pauseOnSaver = true, saverOn = saver, systemAnimationsOff = off),
                IdleSlots.ticksAnimate(saverOn = saver, systemAnimationsOff = off)
            )
        }
    }

    @Test
    fun beatRunsOnlyWhenInViewAndAllowedToMove() {
        assertTrue(IdleSlots.beatRuns(paused = false, animates = true))
        assertFalse(IdleSlots.beatRuns(paused = true, animates = true))
        assertFalse(IdleSlots.beatRuns(paused = false, animates = false))
        assertFalse(IdleSlots.beatRuns(paused = true, animates = false))
    }

    @Test
    fun takeoverSpanAddsTheSettleAndTheLag() {
        assertEquals(2200 + IdleSlots.SETTLE_MS + IdleSlots.STAGGER_MS, IdleSlots.takeoverSpanMs(2200))
    }

    @Test
    fun takeoverSlotsStartAtRestAndLandExactly() {
        for (lagged in listOf(false, true)) {
            assertEquals(0f, IdleSlots.takeoverProgress(0f, 2200, lagged), 1e-6f)
            assertEquals(1f, IdleSlots.takeoverProgress(1f, 2200, lagged), 1e-6f)
            assertEquals(0f, IdleSlots.takeoverStretch(0f, 2200, lagged), 1e-6f)
            assertEquals(0f, IdleSlots.takeoverStretch(1f, 2200, lagged), 1e-6f)
        }
    }

    @Test
    fun takeoverRightSlotIsTheStaggerBehindTheLeft() {
        val tickMs = 2200
        val span = IdleSlots.takeoverSpanMs(tickMs).toFloat()
        val lag = IdleSlots.STAGGER_MS.toFloat()
        for (clock in listOf(0.3f, 0.5f, 0.8f, 0.95f)) {
            val earlier = (clock * span - lag) / span
            assertEquals(IdleSlots.takeoverProgress(earlier, tickMs, lagged = false), IdleSlots.takeoverProgress(clock, tickMs, lagged = true), 1e-5f)
            assertEquals(IdleSlots.takeoverStretch(earlier, tickMs, lagged = false), IdleSlots.takeoverStretch(clock, tickMs, lagged = true), 1e-5f)
        }
        // The lagged slot has not moved yet during the lag.
        assertEquals(0f, IdleSlots.takeoverProgress(lag / span, tickMs, lagged = true), 1e-6f)
        assertEquals(0f, IdleSlots.takeoverStretch(lag / span * 0.5f, tickMs, lagged = true), 1e-6f)
    }

    @Test
    fun takeoverGoesJustPastItsPlaceAndStaysInRange() {
        var peak = 0f
        var low = 0f
        for (lagged in listOf(false, true)) {
            for (i in 0..400) {
                val clock = i / 400f
                val p = IdleSlots.takeoverProgress(clock, 2200, lagged)
                val s = IdleSlots.takeoverStretch(clock, 2200, lagged)
                peak = maxOf(peak, p)
                low = minOf(low, p)
                assertTrue(s in 0f..1f)
            }
        }
        assertTrue(peak > 1f)
        assertTrue(peak <= 1f + IdleSlots.SETTLE_OVERSHOOT + 1e-4f)
        // The settled tick scales the whole wind-up by 1 + SETTLE_OVERSHOOT.
        assertTrue(low >= -IdleSlots.HEAVY_WINDUP_DEPTH * (1f + IdleSlots.SETTLE_OVERSHOOT) - 1e-4f)
    }

    @Test
    fun beatStaysOutOfASwitchThatIsPlaying() {
        val span = IdleSlots.switchSpanMs()
        assertFalse(IdleSlots.beatClearOfSwitch(span - 1, 0L, 0L))
        assertTrue(IdleSlots.beatClearOfSwitch(span, 0L, 0L))
        assertTrue(IdleSlots.beatClearOfSwitch(100_000L, 100_000L, 0L))
    }

    @Test
    fun beatStaysOutOfTheNextAutomaticSwitch() {
        val cycle = 5_000L
        assertTrue(IdleSlots.beatClearOfSwitch(10_000L, 3_000L, cycle))
        assertTrue(IdleSlots.beatClearOfSwitch(10_000L, cycle - IdleSlots.BEAT_MS - 1, cycle))
        assertFalse(IdleSlots.beatClearOfSwitch(10_000L, cycle - IdleSlots.BEAT_MS, cycle))
        assertFalse(IdleSlots.beatClearOfSwitch(10_000L, cycle, cycle))
    }

    @Test
    fun aCycleShorterThanABeatPlusASwitchHasNoBeats() {
        val cycle = IdleSlots.BEAT_MS + IdleSlots.switchSpanMs() - 1
        var since = 0L
        while (since <= cycle) {
            assertFalse(IdleSlots.beatClearOfSwitch(since, since, cycle))
            since += 50L
        }
    }
}
