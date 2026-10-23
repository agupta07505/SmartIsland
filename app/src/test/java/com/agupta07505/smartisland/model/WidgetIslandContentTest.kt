/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetIslandContentTest {
    private val now = 1_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun has(kind: IdleWidgetKind, charging: Boolean = false, alarm: Long = 0L, weather: Boolean = false) =
        WidgetIslandContent.hasContent(kind, charging, alarm, now, weather)

    @Test
    fun clockNeverHasAnIsland() {
        assertFalse(has(IdleWidgetKind.Clock, charging = true, alarm = now + hour, weather = true))
    }

    @Test
    fun dateAndSpeedAlwaysHaveContent() {
        assertTrue(has(IdleWidgetKind.Date))
        assertTrue(has(IdleWidgetKind.NetSpeed))
    }

    @Test
    fun weatherNeedsAReading() {
        assertFalse(has(IdleWidgetKind.Weather))
        assertTrue(has(IdleWidgetKind.Weather, weather = true))
    }

    @Test
    fun batteryOnlyWhileCharging() {
        // Not charging, the battery island still opens: it says when the battery runs out.
        assertTrue(has(IdleWidgetKind.Battery, charging = false))
        assertTrue(has(IdleWidgetKind.Battery, charging = true))
    }

    @Test
    fun alarmHasAnIslandWheneverOneIsStillToCome() {
        assertFalse(has(IdleWidgetKind.NextAlarm, alarm = 0L))
        assertFalse(has(IdleWidgetKind.NextAlarm, alarm = now - 1L))
        assertTrue(has(IdleWidgetKind.NextAlarm, alarm = now + 1L))
        assertTrue(has(IdleWidgetKind.NextAlarm, alarm = now + 12 * hour + 1L))
        assertTrue(has(IdleWidgetKind.NextAlarm, alarm = now + 30 * hour))
    }

    @Test
    fun windNeedsAReadingLikeWeather() {
        assertFalse(has(IdleWidgetKind.Wind))
        assertTrue(has(IdleWidgetKind.Wind, weather = true))
    }

    @Test
    fun mustCloseWhenContentGoesAway() {
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, hasContent = false))
        assertFalse(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, hasContent = true))
    }

    @Test
    fun theSmallIdleCircleNeverHoldsAnIsland() {
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, compact = true))
    }

    @Test
    fun onlyCallsAndLiveEventsCloseTheIsland() {
        listOf(IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.LiveActivity, IslandMode.Timer, IslandMode.Stopwatch,
            IslandMode.ScreenRecording, IslandMode.AudioRecording, IslandMode.Hotspot, IslandMode.Flashlight)
            .forEach { assertTrue(it.name, cardClosesWidgetIsland(it)) }
        listOf(IslandMode.Music, IslandMode.DownloadUpload, IslandMode.Notification, IslandMode.Battery, IslandMode.Bluetooth)
            .forEach { assertFalse(it.name, cardClosesWidgetIsland(it)) }
    }

    @Test
    fun swipeSkipsWidgetsWithoutContent() {
        // widgets 0..3, only 0 and 2 have content
        val has = { i: Int -> i == 0 || i == 2 }
        assertEquals(2, nextStepWithContent(step = 0, direction = 1, count = 4, hasContent = has))
        assertEquals(4, nextStepWithContent(step = 2, direction = 1, count = 4, hasContent = has))
        assertEquals(-2, nextStepWithContent(step = 0, direction = -1, count = 4, hasContent = has))
        // only the current one has content: spring back
        assertNull(nextStepWithContent(step = 0, direction = 1, count = 3, hasContent = { it == 0 }))
        assertNull(nextStepWithContent(step = 0, direction = 1, count = 1, hasContent = { true }))
        assertEquals(1, nextStepWithContent(step = 0, direction = 1, count = 2, hasContent = { true }))
    }

    @Test
    fun ordinalSuffixes() {
        assertEquals("st", WidgetIslandContent.ordinalSuffix(1))
        assertEquals("nd", WidgetIslandContent.ordinalSuffix(2))
        assertEquals("rd", WidgetIslandContent.ordinalSuffix(3))
        assertEquals("th", WidgetIslandContent.ordinalSuffix(4))
        assertEquals("th", WidgetIslandContent.ordinalSuffix(11))
        assertEquals("th", WidgetIslandContent.ordinalSuffix(12))
        assertEquals("th", WidgetIslandContent.ordinalSuffix(13))
        assertEquals("st", WidgetIslandContent.ordinalSuffix(21))
        assertEquals("nd", WidgetIslandContent.ordinalSuffix(22))
        assertEquals("rd", WidgetIslandContent.ordinalSuffix(23))
        assertEquals("th", WidgetIslandContent.ordinalSuffix(24))
        assertEquals("st", WidgetIslandContent.ordinalSuffix(31))
    }

    @Test
    fun longDateReadsLikeTheSpec() {
        assertEquals(
            "Thursday 24th of February 2026",
            WidgetIslandContent.englishLongDate("Thursday", 24, "February", 2026)
        )
        assertTrue(WidgetIslandContent.usesEnglishOrdinalDate("en"))
        assertFalse(WidgetIslandContent.usesEnglishOrdinalDate("de"))
    }

    @Test
    fun durations() {
        assertEquals("<1m", WidgetIslandContent.durationText(30_000L))
        assertEquals("42m", WidgetIslandContent.durationText(42 * 60_000L + 59_000L))
        assertEquals("5h 12m", WidgetIslandContent.durationText(5 * hour + 12 * 60_000L))
        assertEquals("2h", WidgetIslandContent.durationText(2 * hour))
        assertEquals("1d 2h", WidgetIslandContent.durationText(26 * hour))
        assertEquals("<1m", WidgetIslandContent.durationText(-5L))
    }

    @Test
    fun wattsFromMicroOrMilliamps() {
        // 2 A in microamps at 4.2 V = 8.4 W; the sign does not matter
        assertEquals(8.4, WidgetIslandContent.batteryWatts(2_000_000L, 4200)!!, 0.001)
        assertEquals(8.4, WidgetIslandContent.batteryWatts(-2_000_000L, 4200)!!, 0.001)
        // the same 2 A reported in milliamps
        assertEquals(8.4, WidgetIslandContent.batteryWatts(2_000L, 4200)!!, 0.001)
        assertNull(WidgetIslandContent.batteryWatts(0L, 4200))
        assertNull(WidgetIslandContent.batteryWatts(2_000_000L, 0))
        assertNull(WidgetIslandContent.batteryWatts(Long.MIN_VALUE, 4200))
    }

    @Test
    fun batteryTexts() {
        assertEquals("8.4 W", WidgetIslandContent.wattsText(8.4))
        assertEquals("4.20 V", WidgetIslandContent.voltsText(4200))
        assertEquals("36.2°C", WidgetIslandContent.batteryTempText(362, fahrenheit = false))
        assertEquals("97.2°F", WidgetIslandContent.batteryTempText(362, fahrenheit = true))
    }

    @Test
    fun speedTexts() {
        assertEquals("0 KB/s", WidgetIslandContent.speedText(0L))
        assertEquals("320 KB/s", WidgetIslandContent.speedText(320_000L))
        assertEquals("4.2 MB/s", WidgetIslandContent.speedText(4_200_000L))
        assertEquals("12 MB/s", WidgetIslandContent.speedText(12_300_000L))
    }

    @Test
    fun everyWeatherCodeHasAName() {
        listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99, 1234).forEach {
            assertTrue(WidgetIslandContent.weatherName(it, true).isNotBlank())
        }
        assertEquals("Sunny", WidgetIslandContent.weatherName(0, true))
        assertEquals("Clear", WidgetIslandContent.weatherName(0, false))
        assertEquals("Rainy", WidgetIslandContent.weatherName(63, true))
    }

    @Test
    fun batteryMetricSettingIsValidated() {
        assertTrue(WidgetIslandContent.BATTERY_WATT in WidgetIslandContent.VALID_BATTERY_METRICS)
        assertTrue(WidgetIslandContent.BATTERY_VOLT in WidgetIslandContent.VALID_BATTERY_METRICS)
        assertNotNull(WidgetIslandContent.VALID_BATTERY_METRICS)
    }
}
