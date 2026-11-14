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

class IdleWidgetHealthEnvTest {

    @Test
    fun newKindsFromIdAndRoundTrip() {
        assertEquals(IdleWidgetKind.Aqi, IdleWidgetKind.fromId("aqi"))
        assertEquals(IdleWidgetKind.Steps, IdleWidgetKind.fromId("steps"))
        assertEquals(IdleWidgetKind.Uv, IdleWidgetKind.fromId("uv"))
        assertEquals(IdleWidgetKind.ScreenTime, IdleWidgetKind.fromId("screentime"))
        assertEquals(IdleWidgetKind.NextEvent, IdleWidgetKind.fromId("event"))

        val kinds = listOf(IdleWidgetKind.Aqi, IdleWidgetKind.Steps, IdleWidgetKind.Uv, IdleWidgetKind.ScreenTime, IdleWidgetKind.NextEvent)
        val serialized = IdleWidgets.serialize(kinds)
        assertEquals("aqi,steps,uv,screentime,event", serialized)
        assertEquals(kinds, IdleWidgets.parse(serialized))
    }

    @Test
    fun formatStepsFormatsNicely() {
        assertEquals("--", IdleWidgets.formatSteps(-1))
        assertEquals("0", IdleWidgets.formatSteps(0))
        assertEquals("850", IdleWidgets.formatSteps(850))
        assertEquals("4.2k", IdleWidgets.formatSteps(4200))
        assertEquals("9.9k", IdleWidgets.formatSteps(9940))
        assertEquals("10k", IdleWidgets.formatSteps(10000))
        assertEquals("25k", IdleWidgets.formatSteps(25600))
    }

    @Test
    fun aqiLabelsMatchEpaScale() {
        assertEquals("Good", WeatherDisplay.aqiLabel(35))
        assertEquals("Moderate", WeatherDisplay.aqiLabel(75))
        assertEquals("Poor", WeatherDisplay.aqiLabel(125))
        assertEquals("Unhealthy", WeatherDisplay.aqiLabel(175))
        assertEquals("Very bad", WeatherDisplay.aqiLabel(250))
        assertEquals("Hazardous", WeatherDisplay.aqiLabel(400))
    }

    @Test
    fun uvLabelsMatchStandardIndex() {
        assertEquals("Low", WeatherDisplay.uvLabel(1.2f))
        assertEquals("Moderate", WeatherDisplay.uvLabel(4.0f))
        assertEquals("High", WeatherDisplay.uvLabel(6.8f))
        assertEquals("Very high", WeatherDisplay.uvLabel(9.2f))
        assertEquals("Extreme", WeatherDisplay.uvLabel(12.0f))
    }

    @Test
    fun widgetIslandContentRulesForHealthWidgets() {
        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.Steps, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false))
        assertFalse(WidgetIslandContent.hasContent(IdleWidgetKind.Aqi, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false))
        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.Aqi, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = true))
        assertFalse(WidgetIslandContent.hasContent(IdleWidgetKind.Uv, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false))
        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.Uv, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = true))
    }

    @Test
    fun formatScreenTimeFormatsNicely() {
        assertEquals("0m", IdleWidgets.formatScreenTime(0L))
        assertEquals("<1m", IdleWidgets.formatScreenTime(30_000L))
        assertEquals("45m", IdleWidgets.formatScreenTime(45 * 60_000L))
        assertEquals("2h", IdleWidgets.formatScreenTime(2 * 3600_000L))
        assertEquals("2h 15m", IdleWidgets.formatScreenTime(2 * 3600_000L + 15 * 60_000L))

        // Compact mode
        assertEquals("0m", IdleWidgets.formatScreenTime(0L, compact = true))
        assertEquals("45m", IdleWidgets.formatScreenTime(45 * 60_000L, compact = true))
        assertEquals("2h", IdleWidgets.formatScreenTime(2 * 3600_000L + 15 * 60_000L, compact = true))
    }

    @Test
    fun formatEventCountdownRules() {
        val now = 10_000_000L
        assertEquals("now", IdleWidgets.formatEventCountdown(now, now))
        assertEquals("now", IdleWidgets.formatEventCountdown(now - 1000L, now))
        assertEquals("<1m", IdleWidgets.formatEventCountdown(now + 30_000L, now))
        assertEquals("in 25m", IdleWidgets.formatEventCountdown(now + 25 * 60_000L, now))
        assertEquals("in 2h", IdleWidgets.formatEventCountdown(now + 2 * 3600_000L, now))
        assertEquals("in 2h 15m", IdleWidgets.formatEventCountdown(now + 2 * 3600_000L + 15 * 60_000L, now))
    }

    @Test
    fun widgetIslandContentRulesForScreenTimeAndNextEvent() {
        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.ScreenTime, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false, hasScreenTime = true))
        assertFalse(WidgetIslandContent.hasContent(IdleWidgetKind.ScreenTime, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false, hasScreenTime = false))

        assertTrue(WidgetIslandContent.hasContent(IdleWidgetKind.NextEvent, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false, hasNextEvent = true))
        assertFalse(WidgetIslandContent.hasContent(IdleWidgetKind.NextEvent, charging = false, nextAlarmMs = 0L, nowMs = 0L, hasWeather = false, hasNextEvent = false))
    }

    @Test
    fun allNewWidgetsHaveLedIcons() {
        val newIcons = listOf(LedIcon.Leaf, LedIcon.Walk, LedIcon.Uv, LedIcon.Hourglass, LedIcon.Bell, LedIcon.Dnd)
        for (icon in newIcons) {
            val grid = LedFont.icon(icon)
            assertEquals(7, grid.cols)
            assertEquals(7, grid.rows)
            assertTrue(grid.bits.any { it })
        }
    }

    @Test
    fun parseAqiFromJson() {
        val sampleJson = """
            {
                "latitude": 28.6,
                "longitude": 77.2,
                "current": {
                    "time": "2026-11-14T01:00",
                    "interval": 3600,
                    "us_aqi": 182
                }
            }
        """.trimIndent()
        assertEquals(182, WeatherParser.parseAqi(sampleJson))
    }
}
