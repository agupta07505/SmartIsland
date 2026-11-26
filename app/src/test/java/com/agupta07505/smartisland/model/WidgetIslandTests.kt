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
        // not charging, the battery island still opens: it says when the battery runs out
        assertTrue(has(IdleWidgetKind.Battery, charging = false))
        assertTrue(has(IdleWidgetKind.Battery, charging = true))
    }

    @Test
    fun alarmIslandWhileUpcoming() {
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
    fun islandClosesNoContentOrSpace() {
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, hasContent = false))
        assertFalse(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, hasContent = true))
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true, compact = true))
    }


    @Test
    fun callsAndLiveCloseIsland() {
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

    @Test fun dateAndDurationTexts() {
        assertEquals(
        "Thursday 24th of February 2026",
        WidgetIslandContent.englishLongDate("Thursday", 24, "February", 2026)
        )
        assertTrue(WidgetIslandContent.usesEnglishOrdinalDate("en"))
        assertFalse(WidgetIslandContent.usesEnglishOrdinalDate("de"))
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

    @Test fun batteryAndSpeedTexts() {
        assertEquals("8.4 W", WidgetIslandContent.wattsText(8.4))
        assertEquals("4.20 V", WidgetIslandContent.voltsText(4200))
        assertEquals("36.2°C", WidgetIslandContent.batteryTempText(362, fahrenheit = false))
        assertEquals("97.2°F", WidgetIslandContent.batteryTempText(362, fahrenheit = true))
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

class WidgetIslandTest {
    // A 120 dp wide pill at 3x density: 360 px. Collapsed, the shell is the pill (centre 180 px)
    private val pillW = 360f

    @Test
    fun centreTapBand() {
        val half = pillW * PILL_CENTER_ZONE_SHARE / 2f
        assertEquals(PillTapZone.Glyph, pillTapZone(pillW * 0.12f, pillW, pillW))
        assertEquals(PillTapZone.Glyph, pillTapZone(180f - half - 1f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f - half + 1f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f + half - 1f, pillW, pillW))
        assertEquals(PillTapZone.Value, pillTapZone(180f + half + 1f, pillW, pillW))
        assertEquals(PillTapZone.Value, pillTapZone(pillW * 0.9f, pillW, pillW))

        for (x in 0..360 step 6) {
            val inBand = pillCenterTapHit(x.toFloat(), 10f, pillW, pillW, 100f)
            assertEquals("x=$x", inBand, pillTapZone(x.toFloat(), pillW, pillW) == PillTapZone.Middle)
        }
    }

    @Test
    fun idleTapTargets() {
        for (stored in listOf(false, true)) {
            // even with stored cards waiting: only the middle band shows them
            assertEquals(WidgetTapAction.GlyphTap, idleWidgetTapAction(true, false, PillTapZone.Glyph, hasStoredCards = stored))
            assertEquals(WidgetTapAction.ValueTap, idleWidgetTapAction(true, false, PillTapZone.Value, hasStoredCards = stored))
        }

        assertEquals(WidgetTapAction.RevealStoredCards, idleWidgetTapAction(true, false, PillTapZone.Middle, hasStoredCards = true))
        assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, false, PillTapZone.Middle, hasStoredCards = false))

        // landscape's small idle circle is not the collapsed pill: no widget island, whatever the zone or the stored cards
        for (zone in PillTapZone.values()) {
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, true, zone, hasStoredCards = false))
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, true, zone, hasStoredCards = true))
        }

        for (zone in PillTapZone.values()) {
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(false, false, zone, hasStoredCards = true))
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(false, true, zone, hasStoredCards = false))
        }
    }

    @Test
    fun widgetIslandDimensions() {
        val wide = widgetIslandSize(pillWidthDp = 100f, pillHeightDp = 40f, screenWidthDp = 400f)
        assertEquals(200f, wide.widthDp, 0.001f)
        assertEquals(80f, wide.heightDp, 0.001f)
        // the narrowest portrait pill (76 dp) doubled is wider than the floor, but its height (28 dp) is not tall enough
        val narrow = widgetIslandSize(pillWidthDp = 76f, pillHeightDp = 28f, screenWidthDp = 400f)
        assertEquals(152f, narrow.widthDp, 0.001f)
        assertEquals(WIDGET_ISLAND_MIN_HEIGHT_DP, narrow.heightDp, 0.001f)
        // the small landscape circle doubled is too small for an icon and a value: both axes take the floor
        val circle = widgetIslandSize(pillWidthDp = 30f, pillHeightDp = 30f, screenWidthDp = 800f)
        assertEquals(WIDGET_ISLAND_MIN_WIDTH_DP, circle.widthDp, 0.001f)
        assertEquals(WIDGET_ISLAND_MIN_HEIGHT_DP, circle.heightDp, 0.001f)

        val size = widgetIslandSize(pillWidthDp = 300f, pillHeightDp = 40f, screenWidthDp = 360f)
        assertEquals(360f - 2f * WIDGET_ISLAND_EDGE_MARGIN_DP, size.widthDp, 0.001f)
    }

    @Test
    fun widgetIslandPosition() {
        assertEquals(0f, widgetIslandCentre(0f, 124f, 400f), 0.001f)
        assertEquals(40f, widgetIslandCentre(40f, 124f, 400f), 0.001f)
        assertEquals(-40f, widgetIslandCentre(-40f, 124f, 400f), 0.001f)

        // 800 dp wide screen, 124 dp island: its centre may be at most 400 - 8 - 62 = 330 dp from the middle
        assertEquals(330f, widgetIslandCentre(395f, 124f, 800f), 0.001f)
        assertEquals(-330f, widgetIslandCentre(-395f, 124f, 800f), 0.001f)
        // an island as wide as the screen allows is centred
        assertEquals(0f, widgetIslandCentre(50f, 384f, 400f), 0.001f)
    }

    @Test
    fun idleEndStatesCloseOpens() {
        assertFalse(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = true, hasImportantCard = false, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = true, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = false))
    }

    @Test
    fun closeTimerDefaultsNever() {
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.DEFAULT_SECONDS)
        assertNull(WidgetIslandTimer.closeDelayMs(WidgetIslandTimer.DEFAULT_SECONDS))
        assertEquals(3_000L, WidgetIslandTimer.closeDelayMs(3))
        assertEquals(2_000L, WidgetIslandTimer.closeDelayMs(2))
        assertEquals(10_000L, WidgetIslandTimer.closeDelayMs(10))
        assertNull(WidgetIslandTimer.closeDelayMs(WidgetIslandTimer.NEVER))
    }

    @Test
    fun closeTimerValuesAreNormalised() {
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.normalize(0))
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.normalize(-5))
        // 1 second is below the shortest setting
        assertEquals(WidgetIslandTimer.MIN_SECONDS, WidgetIslandTimer.normalize(1))
        assertEquals(5, WidgetIslandTimer.normalize(5))
        assertEquals(WidgetIslandTimer.MAX_SECONDS, WidgetIslandTimer.normalize(9_999))
    }

    @Test
    fun islandHoldsExpandOpenShrinking() {
        assertFalse(widgetIslandHoldsExpand(open = false, shown = false))
        assertTrue(widgetIslandHoldsExpand(open = true, shown = false))
        assertTrue(widgetIslandHoldsExpand(open = true, shown = true))
        assertTrue(widgetIslandHoldsExpand(open = false, shown = true))
    }
}
