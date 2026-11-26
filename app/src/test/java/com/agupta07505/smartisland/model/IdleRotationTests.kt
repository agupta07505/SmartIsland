/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleCloseSequence
import com.agupta07505.smartisland.util.SECONDARY_RETURN_BASE_MS
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleCloseSequenceTest {

    private val speeds = AnimationSpeed.entries

    @Test
    fun openingIsPillSpringOrSecondaryReturnWhicheverLonger() {
        for (speed in speeds) {
            val pill = pillSpringSettleMs(speed.userStiffness(PILL_SPRING_STIFFNESS)).toInt()
            val secondary = speed.userDurationMs(SECONDARY_RETURN_BASE_MS)
            assertEquals("$speed", maxOf(pill, secondary), IdleCloseSequence.openingMs(speed))
        }
        assertTrue(IdleCloseSequence.openingMs(AnimationSpeed.NORMAL) >= SECONDARY_RETURN_BASE_MS)
    }

    @Test
    fun closeSequenceTiming() {
        for (speed in speeds) {
            val combinedPlan = IdleCloseSequence.plan(speed)
            assertEquals(combinedPlan.totalMs, combinedPlan.step1Ms + combinedPlan.step2Ms)
        }

        for (speed in speeds) {
            val symmetricPlan = IdleCloseSequence.plan(speed)
            assertEquals("$speed", IdleCloseSequence.pillArrivalMs(speed), symmetricPlan.step2Ms)
        }

        for (speed in speeds) {
            val slowerPlan = IdleCloseSequence.plan(speed)
            assertTrue("$speed", slowerPlan.step1Ms > slowerPlan.step2Ms)
            assertEquals(IdleCloseSequence.STEP1_OVER_STEP2, slowerPlan.step1Ms / slowerPlan.step2Ms.toFloat(), 0.02f)
            assertTrue(slowerPlan.step1Ms >= IdleCloseSequence.MIN_STEP_MS && slowerPlan.step2Ms >= IdleCloseSequence.MIN_STEP_MS)
        }

        val relaxed = IdleCloseSequence.plan(AnimationSpeed.RELAXED).totalMs
        val normal = IdleCloseSequence.plan(AnimationSpeed.NORMAL).totalMs
        val snappy = IdleCloseSequence.plan(AnimationSpeed.SNAPPY).totalMs
        assertTrue("snappy $snappy normal $normal relaxed $relaxed", snappy < normal && normal < relaxed)

        val mirroredSpeed = AnimationSpeed.NORMAL
        val mirroredPlan = IdleCloseSequence.plan(mirroredSpeed, 280f to 0.86f)
        assertEquals(springTimeToProgressMs(280f, 0.86f, IdleCloseSequence.SETTLED).toInt(), mirroredPlan.step1Ms)
        // the pill's own exit is unchanged
        assertEquals(IdleCloseSequence.plan(mirroredSpeed).step2Ms, mirroredPlan.step2Ms)
    }

    @Test
    fun closeSpringTiming() {
        for (speed in speeds) {
            val settledPlan = IdleCloseSequence.plan(speed)
            val close = springTimeToProgressMs(settledPlan.closeStiffness, PILL_SPRING_DAMPING, IdleCloseSequence.SETTLED)
            val exit = springTimeToProgressMs(settledPlan.exitStiffness, PILL_SPRING_DAMPING, IdleCloseSequence.SETTLED)
            assertEquals("$speed close", settledPlan.step1Ms.toDouble(), close.toDouble(), 2.0)
            assertEquals("$speed exit", settledPlan.step2Ms.toDouble(), exit.toDouble(), 2.0)
        }

        val dampedPlan = IdleCloseSequence.plan(AnimationSpeed.NORMAL)
        for (stiffness in listOf(dampedPlan.closeStiffness, dampedPlan.exitStiffness)) {
            for (ms in 0..1000 step 10) {
                val p = springProgress(ms / 1000f, stiffness, PILL_SPRING_DAMPING)
                assertTrue("overshoot at $ms ms: $p", p <= 1f + 1e-4f)
            }
        }

        for (speed in speeds) {
            val fadedPlan = IdleCloseSequence.plan(speed)
            assertTrue(fadedPlan.contentOutMs in 1 until fadedPlan.step1Ms)
        }
    }

    @Test
    fun stiffnessForALengthRoundTripsThroughTheSpring() {
        for (ms in listOf(80, 150, 275, 400, 700)) {
            val k = IdleCloseSequence.criticalStiffnessForSettleMs(ms)
            assertEquals(ms.toDouble(), springTimeToProgressMs(k, 1f, IdleCloseSequence.SETTLED).toDouble(), 2.0)
        }
    }
}

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
        val d = SimpleDateFormat(IdleSlots.DAY_PATTERN, Locale.US)
        val m = SimpleDateFormat(IdleSlots.MONTH_PATTERN, Locale.US)
        val feb5 = GregorianCalendar(2026, Calendar.FEBRUARY, 5).time
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
    fun tickEligibility() {
        val none = IdleSlots.SpeedLead.None
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = true, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Date, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = true, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))

        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.DayMoments, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.None))
    }

    @Test
    fun tickProgressHopsThriceAndRests() {
        assertEquals(0f, IdleSlots.tickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.tickProgress(1f), 1e-6f)
        // the first two hops take 1 / 3.6 of the time each: each rests exactly on its step boundary (1/3 and 2/3) before the next starts
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
    fun lastHopLongerAndSofter() {
        // the switch takes 1.2 times as long as three equal hops, so the first hops keep their speed
        assertEquals(1.2f, IdleSlots.switchTimeScale(), 1e-6f)
        // time (share of the whole) a hop needs to cover 90% of its own step
        fun timeTo(from: Float, to: Float): Float {
            var f = 0f
            while (IdleSlots.tickProgress(f) < from + 0.9f * (to - from) && f < 1f) f += 0.0005f
            return f
        }
        val firstHop = timeTo(0f, 1f / 3f)
        val lastHopStart = 0.5556f
        val lastHop = timeTo(2f / 3f, 1f) - lastHopStart
        assertTrue(lastHop > firstHop * 1.4f)
        // softer: the last hop's speed at its start is lower than the first hop's
        val firstSpeed = IdleSlots.tickProgress(0.01f) / 0.01f
        val lastSpeed = (IdleSlots.tickProgress(lastHopStart + 0.01f) - IdleSlots.tickProgress(lastHopStart)) / 0.01f
        assertTrue(lastSpeed < firstSpeed)
    }

    @Test
    fun settledTickOvershootsThenLands() {
        val tick = 560
        assertEquals(0f, IdleSlots.settledTickProgress(0f, tick), 1e-6f)
        assertEquals(1f, IdleSlots.settledTickProgress(1f, tick), 1e-6f)
        val share = tick.toFloat() / (tick + IdleSlots.SETTLE_MS)
        // the snap ends at the overshoot, the settle then eases back without ever dropping below 1
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
        // none while the glyph hangs back, none while it settles, a clear stretch right after the snap starts
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
    fun heavyTickHangsThenSnaps() {
        assertEquals(0f, IdleSlots.heavyTickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.heavyTickProgress(1f), 1e-6f)
        // the pull-back ends exactly at -depth, at the end of the wind-up share
        assertEquals(-IdleSlots.HEAVY_WINDUP_DEPTH, IdleSlots.heavyTickProgress(IdleSlots.HEAVY_WINDUP_SHARE), 1e-5f)
        // slow: at half of the wind-up it has not yet gone half way back (smooth start), and it never goes below -depth
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
        // the snap after the wind-up is much faster than the wind-up: more distance in a tenth of the time
        val windStep = IdleSlots.heavyTickProgress(0.04f) - IdleSlots.heavyTickProgress(0f)
        val snapStep = IdleSlots.heavyTickProgress(0.46f) - IdleSlots.heavyTickProgress(0.40f)
        assertTrue(snapStep > -windStep * 3f)
    }

    @Test
    fun beatPlaysUnlessWaitingForChange() {
        // off (the default): the beat plays whether or not anything changed
        assertTrue(IdleSlots.beatDue(onlyOnChange = false, changed = false))
        assertTrue(IdleSlots.beatDue(onlyOnChange = false, changed = true))
        // on: it plays only when a value changed since the last beat
        assertFalse(IdleSlots.beatDue(onlyOnChange = true, changed = false))
        assertTrue(IdleSlots.beatDue(onlyOnChange = true, changed = true))
    }

    @Test
    fun beatPausedWhenPillHidden() {
        assertFalse(IdleSlots.beatPaused(screenOn = true, islandOpen = false))
        assertTrue(IdleSlots.beatPaused(screenOn = false, islandOpen = false))
        assertTrue(IdleSlots.beatPaused(screenOn = true, islandOpen = true))
        assertTrue(IdleSlots.beatPaused(screenOn = false, islandOpen = true))
    }

    @Test
    fun ticksFollowWeatherMotionRule() {
        assertTrue(IdleSlots.ticksAnimate(saverOn = false, systemAnimationsOff = false))
        assertFalse(IdleSlots.ticksAnimate(saverOn = true, systemAnimationsOff = false))
        assertFalse(IdleSlots.ticksAnimate(saverOn = false, systemAnimationsOff = true))
        assertFalse(IdleSlots.ticksAnimate(saverOn = true, systemAnimationsOff = true))
        // the same answer as the weather icon with its Battery Saver pause on, so the 2 never disagree
        for (saver in listOf(false, true)) for (off in listOf(false, true)) {
            assertEquals(
                WeatherDisplay.iconAnimates(enabled = true, pauseOnSaver = true, saverOn = saver, systemAnimationsOff = off),
                IdleSlots.ticksAnimate(saverOn = saver, systemAnimationsOff = off)
            )
        }
    }

    @Test
    fun beatNeedsViewAndMotion() {
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
    fun takeoverSlotsStartAtRest() {
        for (lagged in listOf(false, true)) {
            assertEquals(0f, IdleSlots.takeoverProgress(0f, 2200, lagged), 1e-6f)
            assertEquals(1f, IdleSlots.takeoverProgress(1f, 2200, lagged), 1e-6f)
            assertEquals(0f, IdleSlots.takeoverStretch(0f, 2200, lagged), 1e-6f)
            assertEquals(0f, IdleSlots.takeoverStretch(1f, 2200, lagged), 1e-6f)
        }
    }

    @Test
    fun takeoverRightLagsLeft() {
        val tickMs = 2200
        val span = IdleSlots.takeoverSpanMs(tickMs).toFloat()
        val lag = IdleSlots.STAGGER_MS.toFloat()
        for (clock in listOf(0.3f, 0.5f, 0.8f, 0.95f)) {
            val earlier = (clock * span - lag) / span
            assertEquals(IdleSlots.takeoverProgress(earlier, tickMs, lagged = false), IdleSlots.takeoverProgress(clock, tickMs, lagged = true), 1e-5f)
            assertEquals(IdleSlots.takeoverStretch(earlier, tickMs, lagged = false), IdleSlots.takeoverStretch(clock, tickMs, lagged = true), 1e-5f)
        }
        // the lagged slot has not moved yet during the lag
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
        // the settled tick scales the whole wind-up by 1 + SETTLE_OVERSHOOT
        assertTrue(low >= -IdleSlots.HEAVY_WINDUP_DEPTH * (1f + IdleSlots.SETTLE_OVERSHOOT) - 1e-4f)
    }

    @Test
    fun tickSwitchOverlap() {
        val span = IdleSlots.switchSpanMs()
        assertFalse(IdleSlots.beatClearOfSwitch(span - 1, 0L, 0L))
        assertTrue(IdleSlots.beatClearOfSwitch(span, 0L, 0L))
        assertTrue(IdleSlots.beatClearOfSwitch(100_000L, 100_000L, 0L))

        val approachingCycle = 5_000L
        assertTrue(IdleSlots.beatClearOfSwitch(10_000L, 3_000L, approachingCycle))
        assertTrue(IdleSlots.beatClearOfSwitch(10_000L, approachingCycle - IdleSlots.BEAT_MS - 1, approachingCycle))
        assertFalse(IdleSlots.beatClearOfSwitch(10_000L, approachingCycle - IdleSlots.BEAT_MS, approachingCycle))
        assertFalse(IdleSlots.beatClearOfSwitch(10_000L, approachingCycle, approachingCycle))

        val shortCycleCycle = IdleSlots.BEAT_MS + IdleSlots.switchSpanMs() - 1
        var since = 0L
        while (since <= shortCycleCycle) {
            assertFalse(IdleSlots.beatClearOfSwitch(since, since, shortCycleCycle))
            since += 50L
        }
    }
}

class IdleWidgetTest {
    @Test
    fun widgetOrderCodec() {
        val parsedKinds = IdleWidgets.parse("weather, clock,bogus,weather,alarm")
        assertEquals(listOf(IdleWidgetKind.Weather, IdleWidgetKind.Clock, IdleWidgetKind.NextAlarm), parsedKinds)

        val roundTripKinds = listOf(IdleWidgetKind.Date, IdleWidgetKind.Battery, IdleWidgetKind.NetSpeed)
        assertEquals(roundTripKinds, IdleWidgets.parse(IdleWidgets.serialize(roundTripKinds)))
    }

    @Test
    fun normalizeNeverReturnsAnEmptyList() {
        assertEquals(IdleWidgets.DEFAULT_IDS, IdleWidgets.normalize(""))
        assertEquals(IdleWidgets.DEFAULT_IDS, IdleWidgets.normalize("nope,also-nope"))
        assertEquals("date,weather", IdleWidgets.normalize("date,weather,date"))
    }

    @Test
    fun togglePreservesOrderAndKeepsAtLeastOneWidget() {
        assertEquals("clock,weather", IdleWidgets.toggle("clock", IdleWidgetKind.Weather))
        assertEquals("clock", IdleWidgets.toggle("clock,weather", IdleWidgetKind.Weather))
        assertEquals("clock", IdleWidgets.toggle("clock", IdleWidgetKind.Clock))
    }


    @Test
    fun pickWrapsAroundInBothDirections() {
        val kinds = listOf(IdleWidgetKind.Clock, IdleWidgetKind.Date, IdleWidgetKind.Weather)
        assertEquals(IdleWidgetKind.Clock, IdleWidgets.pick(kinds, 0))
        assertEquals(IdleWidgetKind.Date, IdleWidgets.pick(kinds, 4))
        assertEquals(IdleWidgetKind.Weather, IdleWidgets.pick(kinds, -1))
        assertNull(IdleWidgets.pick(emptyList(), 3))
    }

    @Test
    fun tempUnitFollowsRegionOnlyOnAuto() {
        assertTrue(IdleWidgets.useFahrenheit(IdleWidgets.UNIT_AUTO, "US"))
        assertFalse(IdleWidgets.useFahrenheit(IdleWidgets.UNIT_AUTO, "DE"))
        assertTrue(IdleWidgets.useFahrenheit(IdleWidgets.UNIT_FAHRENHEIT, "DE"))
        assertFalse(IdleWidgets.useFahrenheit(IdleWidgets.UNIT_CELSIUS, "US"))
    }

    @Test
    fun displayTempRoundsAndConverts() {
        assertEquals("21\u00B0", IdleWidgets.displayTemp(21.4f, false))
        assertEquals("70\u00B0", IdleWidgets.displayTemp(21.1f, true))
        assertEquals("-3\u00B0", IdleWidgets.displayTemp(-3.2f, false))
    }

    @Test
    fun shortSpeedStaysAFewCharacters() {
        assertEquals("0K", IdleWidgets.shortSpeed(0))
        assertEquals("0K", IdleWidgets.shortSpeed(-5))
        assertEquals("320K", IdleWidgets.shortSpeed(320_000))
        assertEquals("4.2M", IdleWidgets.shortSpeed(4_200_000))
        assertEquals("25M", IdleWidgets.shortSpeed(25_400_000))
    }

    @Test
    fun weatherCodesGroupToTheirLook() {
        assertEquals(WeatherLook.Clear, weatherLook(0))
        assertEquals(WeatherLook.PartlyCloudy, weatherLook(2))
        assertEquals(WeatherLook.Cloudy, weatherLook(3))
        assertEquals(WeatherLook.Fog, weatherLook(45))
        assertEquals(WeatherLook.Rain, weatherLook(63))
        assertEquals(WeatherLook.Snow, weatherLook(73))
        assertEquals(WeatherLook.Storm, weatherLook(95))
        assertEquals(WeatherLook.Cloudy, weatherLook(999))
    }

    @Test
    fun settingsKeepIdleChoicesInBackup() {
        val original = SmartIslandSettings.Default.copy(
            idleWidgetsEnabled = true,
            idleWidgetIds = "weather,clock",
            idleWidgetCycleSeconds = 10,
            idleWidgetTempUnit = IdleWidgets.UNIT_FAHRENHEIT
        )
        val restored = SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0"))
        assertTrue(restored.idleWidgetsEnabled)
        assertEquals("weather,clock", restored.idleWidgetIds)
        assertEquals(10, restored.idleWidgetCycleSeconds)
        assertEquals(IdleWidgets.UNIT_FAHRENHEIT, restored.idleWidgetTempUnit)
    }

    @Test
    fun settingsDefaultsAreOffAndBadBackupValuesFallBack() {
        assertFalse(SmartIslandSettings.Default.idleWidgetsEnabled)
        val broken = SmartIslandSettings.Default.copy(
            idleWidgetIds = "junk",
            idleWidgetCycleSeconds = 999,
            idleWidgetTempUnit = "K"
        )
        val restored = SmartIslandSettings.fromJson(broken.toJson(appVersion = "7.0.0"))
        assertEquals(IdleWidgets.DEFAULT_IDS, restored.idleWidgetIds)
        assertEquals(IdleWidgets.MAX_CYCLE_SECONDS, restored.idleWidgetCycleSeconds)
        assertEquals(IdleWidgets.UNIT_AUTO, restored.idleWidgetTempUnit)
    }
}

class IdleWidgetOffTest {
    @Test
    fun idleWidgetSuppressionRequiresItsMatchingSwitch() {
        assertTrue(IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = false, offAfterLongPress = true))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = false, offAfterTapClose = true, offAfterLongPress = true))
        assertTrue(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = false, offAfterLongPress = true))
        assertFalse(IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = true, offAfterLongPress = false))
        assertTrue(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = true, offWhenStored = true))
        assertFalse(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = true, offWhenStored = false))
        assertFalse(IdleWidgetOff.suppressed(false, false, false, false, hasStoredNotifications = false, offWhenStored = true))
    }

    @Test
    fun timersAreKeptInRange() {
        assertEquals(1.5f, IdleSlots.normalizeTickSeconds(0.2f), 1e-6f)
        assertEquals(10f, IdleSlots.normalizeTickSeconds(99f), 1e-6f)
        assertEquals(2.5f, IdleSlots.normalizeTickSeconds(Float.NaN), 1e-6f)
        assertEquals(2500L, IdleSlots.tickPeriodMs(2.5f))
        assertEquals(3000L, IdleSlots.tickPeriodMs(2.9f))
        assertEquals(IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgetOff.normalizeCycleSeconds(0))
        assertEquals(IdleWidgets.MAX_CYCLE_SECONDS, IdleWidgetOff.normalizeCycleSeconds(500))
    }

    @Test
    fun selectAllListsEveryWidgetOnce() {
        val parsed = IdleWidgets.parse(IdleWidgetOff.allIds())
        assertEquals(IdleWidgetKind.values().toList(), parsed)
        // the last widget cannot be switched off with the selector
        assertEquals("clock", IdleWidgets.toggle("clock", IdleWidgetKind.Clock))
    }
}

/** Notification-count and mode widgets in the shared widget rules */
class IdleWidgetNewKindsTest {
    @Test
    fun selectableWidgetIds() {
        assertEquals(IdleWidgetKind.Notifications, IdleWidgetKind.fromId("notifications"))
        assertEquals(IdleWidgetKind.Modes, IdleWidgetKind.fromId("modes"))
        val restoredKinds = listOf(IdleWidgetKind.Notifications, IdleWidgetKind.Modes, IdleWidgetKind.Date)
        assertEquals(restoredKinds, IdleWidgets.parse(IdleWidgets.serialize(restoredKinds)))

        assertEquals(IdleWidgetKind.LiveClock, IdleWidgetKind.fromId("liveclock"))
        assertEquals(IdleWidgetKind.Clock, IdleWidgetKind.fromId("clock"))
        val clockKinds = listOf(IdleWidgetKind.LiveClock, IdleWidgetKind.Date)
        assertEquals(clockKinds, IdleWidgets.parse(IdleWidgets.serialize(clockKinds)))
        assertTrue(IdleWidgetKind.LiveClock in IdleWidgets.parse(IdleWidgetOff.allIds()))
        // unlike the hour tick, the clock widget has something to say on the island
        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.LiveClock, false, 0L, 0L, false))
        assertFalse(WidgetIslandContent.hasContent(IdleWidgetKind.Clock, false, 0L, 0L, false))

        val all = IdleWidgets.parse(IdleWidgetOff.allIds())
        assertTrue(IdleWidgetKind.Notifications in all)
        assertTrue(IdleWidgetKind.Modes in all)
    }

    @Test
    fun clockTickSlots() {
        for (right in listOf(false, true)) {
            assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.LiveClock, right, ownTickRunning = false, lead = IdleSlots.SpeedLead.None))
        }

        val leftOnlyLead = IdleSlots.SpeedLead.None
        for (kind in listOf(IdleWidgetKind.Notifications, IdleWidgetKind.Modes)) {
            assertTrue(IdleSlots.takesSideTick(kind, right = false, ownTickRunning = false, lead = leftOnlyLead))
            assertFalse(IdleSlots.takesSideTick(kind, right = true, ownTickRunning = false, lead = leftOnlyLead))
            assertFalse(IdleSlots.takesSideTick(kind, right = false, ownTickRunning = true, lead = leftOnlyLead))
        }
    }

    @Test
    fun islandHasContentOnlyWhileThereIsSomethingToSay() {
        fun has(kind: IdleWidgetKind, count: Int = 0, mode: Boolean = false) =
            WidgetIslandContent.hasContent(kind, false, 0L, 0L, false, notificationCount = count, modeActive = mode)
        assertFalse(has(IdleWidgetKind.Notifications))
        assertTrue(has(IdleWidgetKind.Notifications, count = 2))
        assertFalse(has(IdleWidgetKind.Modes))
        assertTrue(has(IdleWidgetKind.Modes, mode = true))
        // existing widgets are unaffected by the extra arguments
        assertTrue(has(IdleWidgetKind.Date))
        assertTrue(has(IdleWidgetKind.Battery))
    }

    @Test
    fun islandGrowsForAListButNeverShrinksBelowTheNormalSize() {
        val plain = widgetIslandSize(120f, 32f, 400f)
        assertEquals(plain, widgetIslandSize(120f, 32f, 400f, contentHeightDp = 10f))
        val tall = widgetIslandSize(120f, 32f, 400f, contentHeightDp = 94f)
        assertEquals(94f, tall.heightDp, 0.001f)
        assertEquals(plain.widthDp, tall.widthDp, 0.001f)
    }
}
