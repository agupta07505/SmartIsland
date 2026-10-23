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
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRulesTest {
    private fun reading(tempC: Float = 18f, code: Int = 1) =
        WeatherSnapshot(temperatureC = tempC, weatherCode = code, isDay = true, fetchedAtMs = 0L)

    private val notable = WeatherRulesPrefs(mode = WeatherRules.MODE_NOTABLE)

    @Test
    fun defaultsBehaveLikeBeforePhase3() {
        val rules = WeatherRulesPrefs()
        assertFalse(rules.active)
        assertTrue(WeatherRules.shouldShow(rules, reading(), 3))
        assertTrue(WeatherRules.shouldShow(rules, null, 3))
    }

    @Test
    fun aMildDryReadingIsNotNotable() {
        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 1), 12))
        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 3), 12))
    }

    @Test
    fun rainSnowAndStormsAreNotable() {
        listOf(51, 61, 71, 80, 85, 95).forEach { assertTrue("code $it", WeatherRules.shouldShow(notable, reading(18f, it), 12)) }
        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 45), 12))
    }

    @Test
    fun temperatureThresholdsAreInclusive() {
        val rules = notable.copy(triggers = "HOT,COLD", hotC = 30, coldC = 5)
        assertTrue(WeatherRules.shouldShow(rules, reading(30f), 12))
        assertFalse(WeatherRules.shouldShow(rules, reading(29.9f), 12))
        assertTrue(WeatherRules.shouldShow(rules, reading(5f), 12))
        assertFalse(WeatherRules.shouldShow(rules, reading(5.1f), 12))
    }

    @Test
    fun onlySelectedTriggersCount() {
        val hotOnly = notable.copy(triggers = "HOT")
        assertFalse(WeatherRules.shouldShow(hotOnly, reading(18f, 95), 12))
        assertFalse(WeatherRules.shouldShow(hotOnly, reading(-10f), 12))
        assertTrue(WeatherRules.shouldShow(hotOnly, reading(35f), 12))
    }

    @Test
    fun severeCodesAreHeavyStuffOnly() {
        listOf(56, 57, 65, 66, 67, 75, 82, 86, 95, 96, 99).forEach { assertTrue("code $it", WeatherRules.isSevere(it)) }
        listOf(0, 3, 45, 51, 53, 55, 61, 63, 71, 73, 80, 81, 85).forEach { assertFalse("code $it", WeatherRules.isSevere(it)) }
        val severeOnly = notable.copy(triggers = "SEVERE")
        assertTrue(WeatherRules.shouldShow(severeOnly, reading(18f, 65), 12))
        assertFalse(WeatherRules.shouldShow(severeOnly, reading(18f, 61), 12))
    }

    @Test
    fun nothingIsNotableWithoutAReading() {
        assertFalse(WeatherRules.shouldShow(notable, null, 12))
    }

    @Test
    fun nightWindowWrapsMidnight() {
        assertTrue(WeatherRules.isNight(22, 6, 23))
        assertTrue(WeatherRules.isNight(22, 6, 0))
        assertTrue(WeatherRules.isNight(22, 6, 5))
        assertFalse(WeatherRules.isNight(22, 6, 6))
        assertFalse(WeatherRules.isNight(22, 6, 12))
        assertTrue(WeatherRules.isNight(22, 6, 22))
    }

    @Test
    fun nightWindowWithinOneDayAndEmptyWindow() {
        assertTrue(WeatherRules.isNight(1, 5, 1))
        assertFalse(WeatherRules.isNight(1, 5, 5))
        assertFalse(WeatherRules.isNight(7, 7, 7))
    }

    @Test
    fun hideAtNightKeepsAlwaysModeOffThePillAtNight() {
        val rules = WeatherRulesPrefs(hideAtNight = true)
        assertTrue(rules.active)
        assertFalse(WeatherRules.shouldShow(rules, reading(), 23))
        assertFalse(WeatherRules.shouldShow(rules, null, 23))
        assertTrue(WeatherRules.shouldShow(rules, reading(), 9))
    }

    @Test
    fun nightHidesNotableWeatherToo() {
        val rules = notable.copy(hideAtNight = true)
        assertFalse(WeatherRules.shouldShow(rules, reading(18f, 61), 23))
        assertTrue(WeatherRules.shouldShow(rules, reading(18f, 61), 9))
    }

    @Test
    fun aSelectedSevereAlertShowsAtNight() {
        val rules = notable.copy(hideAtNight = true)
        assertTrue(WeatherRules.shouldShow(rules, reading(18f, 95), 23))
        val noSevere = rules.copy(triggers = "RAIN,HOT")
        assertFalse(WeatherRules.shouldShow(noSevere, reading(18f, 95), 23))
        // Without the only-when-notable mode there is no alert to tell about.
        assertFalse(WeatherRules.shouldShow(WeatherRulesPrefs(hideAtNight = true), reading(18f, 95), 23))
    }

    @Test
    fun triggerListsAreNormalisedAndNeverEmpty() {
        assertEquals("RAIN,HOT,COLD,SEVERE", WeatherRules.normalizeTriggers(""))
        assertEquals("RAIN,HOT,COLD,SEVERE", WeatherRules.normalizeTriggers("nonsense"))
        assertEquals("RAIN,SEVERE", WeatherRules.normalizeTriggers("SEVERE, RAIN,RAIN,x"))
    }

    @Test
    fun theLastTriggerCannotBeSwitchedOff() {
        assertEquals("RAIN", WeatherRules.toggleTrigger("RAIN", "RAIN"))
        assertEquals("RAIN,COLD", WeatherRules.toggleTrigger("RAIN", "COLD"))
        assertEquals("COLD", WeatherRules.toggleTrigger("RAIN,COLD", "RAIN"))
        assertEquals("RAIN", WeatherRules.toggleTrigger("RAIN", "bogus"))
    }

    @Test
    fun valuesAreClamped() {
        assertEquals(WeatherRules.MIN_HOT_C, WeatherRules.normalizeHotC(-99))
        assertEquals(WeatherRules.MAX_HOT_C, WeatherRules.normalizeHotC(99))
        assertEquals(WeatherRules.MIN_COLD_C, WeatherRules.normalizeColdC(-99))
        assertEquals(WeatherRules.MAX_COLD_C, WeatherRules.normalizeColdC(99))
        assertEquals(0, WeatherRules.normalizeHour(-3))
        assertEquals(23, WeatherRules.normalizeHour(30))
        assertEquals(WeatherRules.MODE_ALWAYS, WeatherRules.normalizeMode("nonsense"))
    }

    @Test
    fun rulesSurviveABackup() {
        val original = SmartIslandSettings.Default.copy(
            weatherShowRule = WeatherRules.MODE_NOTABLE,
            weatherNotableTriggers = "RAIN,SEVERE",
            weatherHotC = 35,
            weatherColdC = -2,
            weatherHideAtNight = true,
            weatherNightStartHour = 23,
            weatherNightEndHour = 7
        )
        val restored = SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0"))
        assertEquals(original.weatherRules(), restored.weatherRules())
    }

    @Test
    fun badStoredRulesAreRepairedOnRestore() {
        val bad = SmartIslandSettings.Default.copy(
            weatherShowRule = "x",
            weatherNotableTriggers = "",
            weatherHotC = 500,
            weatherColdC = -500,
            weatherNightStartHour = 99,
            weatherNightEndHour = -4
        )
        val rules = SmartIslandSettings.fromJson(bad.toJson(appVersion = "7.0.0")).weatherRules()
        assertEquals(WeatherRules.MODE_ALWAYS, rules.mode)
        assertEquals(WeatherRules.DEFAULT_TRIGGERS, rules.triggers)
        assertEquals(WeatherRules.MAX_HOT_C, rules.hotC)
        assertEquals(WeatherRules.MIN_COLD_C, rules.coldC)
        assertEquals(23, rules.nightStartHour)
        assertEquals(0, rules.nightEndHour)
    }

    @Test
    fun anOlderBackupKeepsTheDefaultRules() {
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings.Default.toJson(appVersion = "7.0.0"))
        assertEquals(WeatherRulesPrefs(), restored.weatherRules())
    }
}
