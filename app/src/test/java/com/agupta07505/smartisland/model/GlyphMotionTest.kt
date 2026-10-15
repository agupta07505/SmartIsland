/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMotionTest {
    private fun motion(kind: IdleWidgetKind, look: WeatherLook? = null, day: Boolean = true, wind: Float = -1f, off: Boolean = false) =
        GlyphMotions.forWidget(kind, look, day, wind, off)

    @Test
    fun windPeriodShrinksAsWindGrows() {
        val light = GlyphMotions.windPeriodMs(10f)
        val fresh = GlyphMotions.windPeriodMs(30f)
        val gale = GlyphMotions.windPeriodMs(55f)
        assertTrue(light > fresh && fresh > gale)
    }

    @Test
    fun windPeriodEndsAreClamped() {
        assertEquals(GlyphMotions.WIND_SLOWEST_MS, GlyphMotions.windPeriodMs(GlyphMotions.WIND_CALM_KMH))
        assertEquals(GlyphMotions.WIND_FASTEST_MS, GlyphMotions.windPeriodMs(GlyphMotions.WIND_STRONG_KMH))
        assertEquals(GlyphMotions.WIND_FASTEST_MS, GlyphMotions.windPeriodMs(200f))
    }

    @Test
    fun calmOrUnknownWindDoesNotMove() {
        assertEquals(0, GlyphMotions.windPeriodMs(0f))
        assertEquals(0, GlyphMotions.windPeriodMs(1.9f))
        assertEquals(0, GlyphMotions.windPeriodMs(-1f))
        assertEquals(0, GlyphMotions.windPeriodMs(Float.NaN))
        assertFalse(motion(IdleWidgetKind.Wind, wind = -1f).isRunning)
    }

    @Test
    fun windWidgetSwaysWithItsSpeed() {
        val m = motion(IdleWidgetKind.Wind, wind = 25f)
        assertEquals(GlyphMotionKind.WindSway, m.kind)
        assertEquals(GlyphMotions.windPeriodMs(25f), m.periodMs)
    }

    @Test
    fun sunTurnsOnlyForClearDay() {
        assertEquals(GlyphMotionKind.SunSpin, motion(IdleWidgetKind.Weather, WeatherLook.Clear, day = true).kind)
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Clear, day = false).isRunning)
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Cloudy).isRunning)
        assertFalse(motion(IdleWidgetKind.Weather, null).isRunning)
    }

    @Test
    fun rainDropsFallOnlyWhenRaining() {
        assertEquals(GlyphMotionKind.RainFall, motion(IdleWidgetKind.Weather, WeatherLook.Rain).kind)
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Snow).isRunning)
    }

    @Test
    fun otherWidgetsDoNotMove() {
        for (kind in IdleWidgetKind.values()) {
            if (kind == IdleWidgetKind.Weather || kind == IdleWidgetKind.Wind) continue
            assertFalse(kind.name, motion(kind, WeatherLook.Rain, wind = 40f).isRunning)
        }
    }

    @Test
    fun removeAnimationsStopsEverything() {
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Clear, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.Weather, WeatherLook.Rain, off = true).isRunning)
        assertFalse(motion(IdleWidgetKind.Wind, wind = 40f, off = true).isRunning)
    }

    @Test
    fun sunAngleWrapsAFullTurn() {
        assertEquals(0f, GlyphMotions.sunAngle(0f), 0.001f)
        assertEquals(180f, GlyphMotions.sunAngle(0.5f), 0.001f)
        assertEquals(0f, GlyphMotions.sunAngle(1f), 0.001f)
        assertEquals(90f, GlyphMotions.sunAngle(1.25f), 0.001f)
    }

    @Test
    fun windOffsetStaysInsideTheSwayRange() {
        for (i in 0..20) {
            val o = GlyphMotions.windOffsetShare(i / 20f)
            assertTrue(Math.abs(o) <= GlyphMotions.WIND_SWAY_SHARE + 1e-6f)
        }
        assertEquals(0f, GlyphMotions.windOffsetShare(0f), 0.001f)
    }

    @Test
    fun dropFadesInAtTopAndOutAtBottomAndLoops() {
        val top = GlyphMotions.drop(0f)
        val mid = GlyphMotions.drop(0.5f)
        val bottom = GlyphMotions.drop(1f)
        assertEquals(-0.4f, top.offsetShare, 0.001f)
        assertEquals(0f, top.alpha, 0.001f)
        assertEquals(0f, mid.offsetShare, 0.001f)
        assertEquals(1f, mid.alpha, 0.001f)
        assertEquals(top.offsetShare, bottom.offsetShare, 0.001f)
        assertEquals(GlyphMotions.drop(0.25f).alpha, GlyphMotions.drop(1.25f).alpha, 0.001f)
    }

    @Test
    fun morphRunsOnlyWhenTheGlyphChanges() {
        assertFalse(WeatherMorph.changed(WeatherLook.Rain, true, WeatherLook.Rain, true))
        assertTrue(WeatherMorph.changed(WeatherLook.Clear, true, WeatherLook.Rain, true))
        assertTrue(WeatherMorph.changed(WeatherLook.Rain, true, WeatherLook.Clear, true))
    }

    @Test
    fun dayTurningToNightMorphsOnlyWhereTheGlyphDiffers() {
        assertTrue(WeatherMorph.changed(WeatherLook.Clear, true, WeatherLook.Clear, false))
        assertTrue(WeatherMorph.changed(WeatherLook.PartlyCloudy, false, WeatherLook.PartlyCloudy, true))
        assertFalse(WeatherMorph.changed(WeatherLook.Cloudy, true, WeatherLook.Cloudy, false))
        assertFalse(WeatherMorph.changed(WeatherLook.Rain, true, WeatherLook.Rain, false))
    }

    @Test
    fun morphStartsOnTheOldGlyphAndEndsOnTheNewOne() {
        val start = WeatherMorph.frame(0f)
        assertEquals(1f, start.outgoing.alpha, 0.001f)
        assertEquals(0f, start.incoming.alpha, 0.001f)
        assertEquals(1f, start.outgoing.scale, 0.001f)
        assertEquals(0f, start.outgoing.rotationDegrees, 0.001f)
        val end = WeatherMorph.frame(1f)
        assertEquals(0f, end.outgoing.alpha, 0.001f)
        assertEquals(1f, end.incoming.alpha, 0.001f)
        assertEquals(1f, end.incoming.scale, 0.001f)
        assertEquals(0f, end.incoming.rotationDegrees, 0.001f)
    }

    @Test
    fun morphCrossFadeNeverDipsAndStaysInRange() {
        var p = 0f
        while (p <= 1f) {
            val f = WeatherMorph.frame(p)
            assertEquals(1f, f.outgoing.alpha + f.incoming.alpha, 0.001f)
            assertTrue(f.outgoing.scale in WeatherMorph.MIN_SCALE..1f)
            assertTrue(f.incoming.scale in WeatherMorph.MIN_SCALE..1f)
            p += 0.05f
        }
        // Out-of-range progress is clamped.
        assertEquals(WeatherMorph.frame(1f), WeatherMorph.frame(1.7f))
        assertEquals(WeatherMorph.frame(0f), WeatherMorph.frame(-0.3f))
    }

    @Test
    fun morphTurnsBothGlyphsTheSameWayRound() {
        val mid = WeatherMorph.frame(0.5f)
        assertEquals(WeatherMorph.TURN_DEGREES / 2f, mid.outgoing.rotationDegrees, 0.001f)
        assertEquals(-WeatherMorph.TURN_DEGREES / 2f, mid.incoming.rotationDegrees, 0.001f)
    }

    // ---- Attention states ----

    @Test
    fun fastSpeedStartsAtOneMegabytePerSecond() {
        assertFalse(AttentionStates.isFastSpeed(999_999L, wasFast = false))
        assertTrue(AttentionStates.isFastSpeed(1_000_000L, wasFast = false))
        assertTrue(AttentionStates.isFastSpeed(5_000_000L, wasFast = false))
    }

    @Test
    fun fastSpeedKeepsPulsingInsideTheHysteresisBand() {
        // Just under 1 MB/s: a running pulse carries on, a stopped one stays off, so it cannot flicker around the limit.
        assertTrue(AttentionStates.isFastSpeed(900_000L, wasFast = true))
        assertFalse(AttentionStates.isFastSpeed(900_000L, wasFast = false))
        assertTrue(AttentionStates.isFastSpeed(AttentionStates.FAST_SPEED_STOP_BPS, wasFast = true))
        assertFalse(AttentionStates.isFastSpeed(AttentionStates.FAST_SPEED_STOP_BPS - 1, wasFast = true))
        assertFalse(AttentionStates.isFastSpeed(0L, wasFast = true))
    }

    @Test
    fun fastSpeedRuleIsStableWhenRunAgain() {
        for (bps in longArrayOf(0L, 500_000L, 800_000L, 900_000L, 1_000_000L, 3_000_000L)) {
            for (was in listOf(false, true)) {
                val once = AttentionStates.isFastSpeed(bps, was)
                assertEquals(once, AttentionStates.isFastSpeed(bps, once))
            }
        }
    }

    @Test
    fun alarmGlowsOnlyInTheLastTenMinutes() {
        val now = 1_000_000_000L
        val ten = AttentionStates.ALARM_SOON_MS
        assertEquals(10 * 60_000L, ten)
        assertFalse(AttentionStates.isAlarmSoon(now + ten, now))
        assertTrue(AttentionStates.isAlarmSoon(now + ten - 1, now))
        assertTrue(AttentionStates.isAlarmSoon(now + 1, now))
        assertFalse(AttentionStates.isAlarmSoon(now + 3 * 60 * 60_000L, now))
    }

    @Test
    fun alarmStopsGlowingWhenItFiresOrIsCleared() {
        val now = 1_000_000_000L
        assertFalse(AttentionStates.isAlarmSoon(now, now))
        assertFalse(AttentionStates.isAlarmSoon(now - 1, now))
        assertFalse(AttentionStates.isAlarmSoon(0L, now))
    }

    @Test
    fun alarmRecheckWaitsForTheWindowThenForTheAlarm() {
        val now = 1_000_000_000L
        val ten = AttentionStates.ALARM_SOON_MS
        // Far off: wake up shortly after the window opens, but never later than the recheck cap.
        assertEquals(AttentionStates.ALARM_RECHECK_MAX_MS, AttentionStates.alarmRecheckDelayMs(now + 5 * 60 * 60_000L, now))
        // Just outside the window: wake up as it opens, so the next look sees it as soon.
        val wait = AttentionStates.alarmRecheckDelayMs(now + ten + 5_000L, now)!!
        assertEquals(5_001L, wait)
        assertTrue(AttentionStates.isAlarmSoon(now + ten + 5_000L, now + wait))
        // Inside the window: wake up when the alarm rings (capped), and nothing is left after that.
        assertEquals(30_000L, AttentionStates.alarmRecheckDelayMs(now + 30_000L, now))
        assertNull(AttentionStates.alarmRecheckDelayMs(now, now))
        assertNull(AttentionStates.alarmRecheckDelayMs(0L, now))
    }

    @Test
    fun attentionOnlyAppliesToItsOwnWidget() {
        fun at(kind: IdleWidgetKind, fast: Boolean = false, soon: Boolean = false, off: Boolean = false) =
            AttentionStates.forWidget(kind, fast, soon, off)
        assertEquals(GlyphAttention.SpeedPulse, at(IdleWidgetKind.NetSpeed, fast = true))
        assertEquals(GlyphAttention.AlarmGlow, at(IdleWidgetKind.NextAlarm, soon = true))
        assertEquals(GlyphAttention.None, at(IdleWidgetKind.NetSpeed, fast = false))
        assertEquals(GlyphAttention.None, at(IdleWidgetKind.NextAlarm, soon = false))
        // A flag for another widget never leaks across.
        assertEquals(GlyphAttention.None, at(IdleWidgetKind.NextAlarm, fast = true))
        assertEquals(GlyphAttention.None, at(IdleWidgetKind.NetSpeed, soon = true))
        assertEquals(GlyphAttention.None, at(IdleWidgetKind.Weather, fast = true, soon = true))
    }

    @Test
    fun attentionStaysStaticWhenAnimationsAreOff() {
        assertEquals(GlyphAttention.None, AttentionStates.forWidget(IdleWidgetKind.NetSpeed, true, false, animationsOff = true))
        assertEquals(GlyphAttention.None, AttentionStates.forWidget(IdleWidgetKind.NextAlarm, false, true, animationsOff = true))
    }

    @Test
    fun attentionLoopsAreSlow() {
        assertEquals(0, AttentionStates.periodMs(GlyphAttention.None))
        assertTrue(AttentionStates.periodMs(GlyphAttention.SpeedPulse) >= 2_000)
        assertTrue(AttentionStates.periodMs(GlyphAttention.AlarmGlow) >= 2_000)
    }

    @Test
    fun breathRisesAndFallsSmoothly() {
        assertEquals(0f, AttentionStates.breath(0f), 0.0001f)
        assertEquals(1f, AttentionStates.breath(0.5f), 0.0001f)
        assertEquals(0f, AttentionStates.breath(1f), 0.0001f)
        assertEquals(AttentionStates.breath(0.25f), AttentionStates.breath(0.75f), 0.0001f)
        var p = 0f
        while (p <= 1f) {
            assertTrue(AttentionStates.breath(p) in 0f..1f)
            p += 0.05f
        }
    }

    @Test
    fun pulseAndGlowStayInsideTheirRanges() {
        assertEquals(1f, AttentionStates.pulseScale(0f), 0.0001f)
        assertEquals(1f + AttentionStates.SPEED_PULSE_GROWTH, AttentionStates.pulseScale(1f), 0.0001f)
        assertEquals(AttentionStates.GLOW_MIN_ALPHA, AttentionStates.glowAlpha(0f), 0.0001f)
        assertEquals(AttentionStates.GLOW_MAX_ALPHA, AttentionStates.glowAlpha(1f), 0.0001f)
        assertEquals(AttentionStates.GLOW_MIN_SCALE, AttentionStates.glowScale(0f), 0.0001f)
        assertEquals(AttentionStates.GLOW_MAX_SCALE, AttentionStates.glowScale(1f), 0.0001f)
        // Out-of-range breath is clamped.
        assertEquals(AttentionStates.pulseScale(1f), AttentionStates.pulseScale(2f), 0.0001f)
        assertEquals(AttentionStates.glowAlpha(0f), AttentionStates.glowAlpha(-1f), 0.0001f)
    }
}
