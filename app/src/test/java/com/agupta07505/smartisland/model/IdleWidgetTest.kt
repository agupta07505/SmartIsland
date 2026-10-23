/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleWidgetTest {
    @Test
    fun parseKeepsOrderAndDropsUnknownAndRepeats() {
        val kinds = IdleWidgets.parse("weather, clock,bogus,weather,alarm")
        assertEquals(listOf(IdleWidgetKind.Weather, IdleWidgetKind.Clock, IdleWidgetKind.NextAlarm), kinds)
    }

    @Test
    fun serializeRoundTrips() {
        val kinds = listOf(IdleWidgetKind.Date, IdleWidgetKind.Battery, IdleWidgetKind.NetSpeed)
        assertEquals(kinds, IdleWidgets.parse(IdleWidgets.serialize(kinds)))
    }

    @Test
    fun normalizeNeverReturnsAnEmptyList() {
        assertEquals(IdleWidgets.DEFAULT_IDS, IdleWidgets.normalize(""))
        assertEquals(IdleWidgets.DEFAULT_IDS, IdleWidgets.normalize("nope,also-nope"))
        assertEquals("date,weather", IdleWidgets.normalize("date,weather,date"))
    }

    @Test
    fun toggleAddsAtTheEndAndRemoves() {
        assertEquals("clock,weather", IdleWidgets.toggle("clock", IdleWidgetKind.Weather))
        assertEquals("clock", IdleWidgets.toggle("clock,weather", IdleWidgetKind.Weather))
    }

    @Test
    fun theLastWidgetCannotBeSwitchedOff() {
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
    fun temperatureUnitFollowsTheRegionOnlyWhenAuto() {
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
    fun settingsKeepTheIdleWidgetChoicesInABackup() {
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
