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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherConfigTest {
    @Test
    fun badValuesFallBackToTheDefaults() {
        assertEquals(WeatherConfig.SOURCE_AUTO, WeatherConfig.normalizeSource("satellite"))
        assertEquals(WeatherConfig.SOURCE_MANUAL, WeatherConfig.normalizeSource(WeatherConfig.SOURCE_MANUAL))
        assertEquals(WeatherConfig.PROVIDER_OPEN_METEO, WeatherConfig.normalizeProvider(""))
        assertEquals(WeatherConfig.PROVIDER_CUSTOM, WeatherConfig.normalizeProvider(WeatherConfig.PROVIDER_CUSTOM))
        assertEquals(WeatherConfig.DEFAULT_REFRESH_MINUTES, WeatherConfig.normalizeRefreshMinutes(7))
        assertEquals(WeatherConfig.REFRESH_MANUAL, WeatherConfig.normalizeRefreshMinutes(0))
        assertEquals(180, WeatherConfig.normalizeRefreshMinutes(180))
    }

    @Test
    fun refreshChoicesMatchSettings() {
        assertEquals(listOf(15, 30, 60, 180, 0), WeatherConfig.REFRESH_CHOICES)
        assertEquals(15 * 60_000L, WeatherConfig.refreshMs(15))
        assertEquals(3 * 60 * 60_000L, WeatherConfig.refreshMs(180))
        assertEquals(0L, WeatherConfig.refreshMs(WeatherConfig.REFRESH_MANUAL))
        // an unknown value counts as the default, not as Manual
        assertEquals(30 * 60_000L, WeatherConfig.refreshMs(-5))
    }

    @Test
    fun textsAreTrimmedAndCapped() {
        assertEquals("Berlin", WeatherConfig.normalizeCity("  Berlin  "))
        assertEquals(WeatherConfig.MAX_CITY_LENGTH, WeatherConfig.normalizeCity("x".repeat(500)).length)
        assertEquals(WeatherConfig.MAX_URL_LENGTH, WeatherConfig.normalizeCustomUrl("h".repeat(900)).length)
        assertEquals("abc", WeatherConfig.normalizeKey(" abc\n"))
    }

    @Test
    fun coordinatesAreChecked() {
        assertTrue(WeatherConfig.validCoordinates(52.5f, 13.4f))
        assertTrue(WeatherConfig.validCoordinates(-90f, 180f))
        assertFalse(WeatherConfig.validCoordinates(91f, 0f))
        assertFalse(WeatherConfig.validCoordinates(0f, -181f))
        assertFalse(WeatherConfig.validCoordinates(Float.NaN, 0f))
    }

    @Test
    fun manualPlaceNeedsCityAndCoords() {
        assertFalse(WeatherConfig.hasManualPlace(WeatherPrefs(source = WeatherConfig.SOURCE_MANUAL)))
        assertTrue(WeatherConfig.hasManualPlace(WeatherPrefs(manualCity = "Berlin", manualLat = 52.5f, manualLon = 13.4f)))
        assertFalse(WeatherConfig.hasManualPlace(WeatherPrefs(manualCity = "Nowhere", manualLat = 200f, manualLon = 0f)))
    }

    @Test
    fun customAddressesMustBeHttpsWithAHost() {
        assertTrue(WeatherConfig.isValidCustomUrl("https://example.com/v1/forecast"))
        assertTrue(WeatherConfig.isValidCustomUrl("  https://weather.local:8443/v1/forecast  "))
        assertFalse(WeatherConfig.isValidCustomUrl("http://example.com/v1/forecast"))
        assertFalse(WeatherConfig.isValidCustomUrl("example.com"))
        assertFalse(WeatherConfig.isValidCustomUrl("https://"))
        assertFalse(WeatherConfig.isValidCustomUrl("https://exa mple.com"))
        assertFalse(WeatherConfig.isValidCustomUrl(""))
        assertFalse(WeatherConfig.isValidCustomUrl("https://example.com/" + "a".repeat(400)))
    }

    @Test
    fun theDefaultProviderAsksOpenMeteoWithTheRoundedPlace() {
        val url = WeatherConfig.forecastUrl(WeatherPrefs(), 52.52f, 13.41f)!!
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=52.5&longitude=13.4&current="))
        assertTrue(url.contains("temperature_2m,weather_code,is_day,wind_speed_10m,wind_direction_10m"))
        assertTrue(url.endsWith("&timezone=auto"))
        // A key set for another provider is never sent to Open-Meteo
        val withKey = WeatherConfig.forecastUrl(WeatherPrefs(customKey = "secret"), 1f, 2f)!!
        assertFalse(withKey.contains("apikey"))
        assertFalse(withKey.contains("secret"))
    }

    @Test
    fun aCustomProviderGetsItsAddressAndKey() {
        val prefs = WeatherPrefs(
            provider = WeatherConfig.PROVIDER_CUSTOM,
            customUrl = "https://customer-api.example.com/v1/forecast",
            customKey = "a b&c"
        )
        val url = WeatherConfig.forecastUrl(prefs, 10.04f, -20.06f)!!
        assertTrue(url.startsWith("https://customer-api.example.com/v1/forecast?latitude=10.0&longitude=-20.1&current="))
        assertTrue(url.endsWith("&apikey=a+b%26c"))
        // an address that already has a query keeps it
        val withQuery = prefs.copy(customUrl = "https://example.com/forecast?model=best", customKey = "")
        val second = WeatherConfig.forecastUrl(withQuery, 1f, 2f)!!
        assertTrue(second.startsWith("https://example.com/forecast?model=best&latitude="))
        assertFalse(second.contains("apikey"))
    }

    @Test
    fun cacheSignatureInputs() {
        val base = WeatherPrefs()
        assertEquals(WeatherConfig.LEGACY_SIGNATURE, WeatherConfig.signature(base))
        // auto and Last known share 1: the place moves by itself, the reading is not for a different place choice
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(base.copy(source = WeatherConfig.SOURCE_LAST_KNOWN)))
        // the refresh interval does not matter
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(base.copy(refreshMinutes = 180)))
        val berlin = base.copy(source = WeatherConfig.SOURCE_MANUAL, manualCity = "Berlin", manualLat = 52.5f, manualLon = 13.4f)
        val paris = berlin.copy(manualCity = "Paris", manualLat = 48.9f, manualLon = 2.4f)
        assertNotEquals(WeatherConfig.signature(base), WeatherConfig.signature(berlin))
        assertNotEquals(WeatherConfig.signature(berlin), WeatherConfig.signature(paris))
        val custom = base.copy(provider = WeatherConfig.PROVIDER_CUSTOM, customUrl = "https://example.com/forecast")
        assertNotEquals(WeatherConfig.signature(base), WeatherConfig.signature(custom))
        // the key is not part of it (changing a key does not throw the reading away)
        assertEquals(WeatherConfig.signature(custom), WeatherConfig.signature(custom.copy(customKey = "k")))
    }

    @Test
    fun citySearchIsEncoded() {
        assertNull(WeatherConfig.geocodingUrl("a", "en"))
        assertNull(WeatherConfig.geocodingUrl("   ", "en"))
        val url = WeatherConfig.geocodingUrl("São Paulo", "pt")!!
        assertTrue(url.startsWith("https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o+Paulo"))
        assertTrue(url.contains("&language=pt"))
        // Malformed language setting, i.e. anything other than a plain code, falls back to English
        assertTrue(WeatherConfig.geocodingUrl("Oslo", "../x")!!.contains("&language=en"))
    }

    @Test
    fun cityHitsAreParsedRoundedAndFiltered() {
        val json = """
            {"results":[
              {"name":"Berlin","latitude":52.52437,"longitude":13.41053,"admin1":"Berlin","country":"Germany"},
              {"name":"Springfield","latitude":39.80172,"longitude":-89.64371,"admin1":"Illinois","country":"United States"},
              {"name":"","latitude":1.0,"longitude":2.0},
              {"name":"NoPlace"},
              {"name":"Far","latitude":123.0,"longitude":0.0}
            ]}
        """.trimIndent()
        val cities = WeatherConfig.parseCities(json)
        assertEquals(2, cities.size)
        assertEquals(52.5f, cities[0].lat, 0.001f)
        assertEquals(13.4f, cities[0].lon, 0.001f)
        assertEquals("Berlin, Germany", cities[0].label)
        assertEquals("Springfield, Illinois, United States", cities[1].label)
    }

    @Test
    fun cityParsingSurvivesOddAnswers() {
        assertTrue(WeatherConfig.parseCities("{}").isEmpty())
        assertTrue(WeatherConfig.parseCities("not json").isEmpty())
        assertTrue(WeatherConfig.parseCities("""{"results":[]}""").isEmpty())
    }

    @Test
    fun choicesSurviveBackupKeyDoesNot() {
        val original = SmartIslandSettings.Default.copy(
            weatherLocationSource = WeatherConfig.SOURCE_MANUAL,
            weatherManualCity = "Berlin, Germany",
            weatherManualLat = 52.5f,
            weatherManualLon = 13.4f,
            weatherRefreshMinutes = 180,
            weatherProvider = WeatherConfig.PROVIDER_CUSTOM,
            weatherCustomUrl = "https://example.com/v1/forecast"
        )
        assertEquals(original, SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0")))
        val withKey = original.copy(weatherCustomKey = "secret")
        val json = withKey.toJson(appVersion = "7.0.0")
        assertFalse(json.contains("secret"))
        assertEquals("", SmartIslandSettings.fromJson(json).weatherCustomKey)
    }

    @Test
    fun badStoredValuesRepaired() {
        val bad = SmartIslandSettings.Default.copy(
            weatherLocationSource = "teleport",
            weatherRefreshMinutes = 7,
            weatherProvider = "acme",
            weatherManualLat = 500f
        )
        val restored = SmartIslandSettings.fromJson(bad.toJson(appVersion = "7.0.0"))
        assertEquals(WeatherConfig.SOURCE_AUTO, restored.weatherLocationSource)
        assertEquals(WeatherConfig.DEFAULT_REFRESH_MINUTES, restored.weatherRefreshMinutes)
        assertEquals(WeatherConfig.PROVIDER_OPEN_METEO, restored.weatherProvider)
        assertEquals(0f, restored.weatherManualLat, 0f)
    }

    @Test
    fun anOlderBackupWithoutWeatherChoicesKeepsTheDefaults() {
        val old = SmartIslandSettings.Default.toJson(appVersion = "7.0.0")
        val restored = SmartIslandSettings.fromJson(old)
        assertEquals(SmartIslandSettings.Default.weatherPrefs(), restored.weatherPrefs())
    }

    @Test
    fun readoutIndependentCacheKey() {
        val base = WeatherPrefs()
        val changed = base.copy(pillMode = WeatherPill.FEELS, readouts = "RAIN,SUN,HOURLY")
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(changed))
    }

    @Test
    fun weatherRowSelection() {
        val prefs = WeatherPrefs(readouts = "HOURLY,bogus,AQI,RAIN,RAIN")
        assertEquals(listOf("RAIN", "AQI", "HOURLY"), prefs.effectiveReadouts())
        val custom = prefs.copy(provider = WeatherConfig.PROVIDER_CUSTOM)
        assertEquals(listOf("RAIN", "HOURLY"), custom.effectiveReadouts())
        assertEquals(emptyList<String>(), WeatherPrefs().effectiveReadouts())
    }

    @Test
    fun forecastUrlNoExtraRows() {
        val url = WeatherConfig.forecastUrl(WeatherPrefs(), 52.52f, 13.41f)!!
        assertFalse(url.contains("daily"))
        assertFalse(url.contains("hourly"))
        assertFalse(url.contains("timeformat"))
    }
}

class WeatherRefreshTest {
    private val min = 60_000L

    @Test
    fun refreshEligibility() {
        assertTrue(WeatherRefresh.shouldFetch(30 * min, hasWeather = false, ageMs = 0, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(0L, hasWeather = false, ageMs = 0, missingData = false))

        assertFalse(WeatherRefresh.shouldFetch(30 * min, hasWeather = true, ageMs = 29 * min, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(30 * min, hasWeather = true, ageMs = 30 * min, missingData = false))
        assertFalse(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 100 * min, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(15 * min, hasWeather = true, ageMs = 16 * min, missingData = false))

        assertFalse(WeatherRefresh.shouldFetch(0L, hasWeather = true, ageMs = 99_999 * min, missingData = false))
        assertFalse(WeatherRefresh.shouldFetch(0L, hasWeather = true, ageMs = 99_999 * min, missingData = true))

        assertFalse(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 2 * min, missingData = true))
        assertTrue(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 5 * min, missingData = true))
    }

    @Test
    fun retryIntervalFloor() {
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = false, ageMs = 0))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 40 * min))
        assertEquals(20 * min, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 10 * min))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 28 * min))
        assertEquals(WeatherRefresh.MANUAL_POLL_MS, WeatherRefresh.waitMs(0L, hasWeather = true, ageMs = 5 * min))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(0L, hasWeather = false, ageMs = 0))
    }
}

class WeatherRulesTest {
    private fun reading(tempC: Float = 18f, code: Int = 1) =
        WeatherSnapshot(temperatureC = tempC, weatherCode = code, isDay = true, fetchedAtMs = 0L)

    private val notable = WeatherRulesPrefs(mode = WeatherRules.MODE_NOTABLE)

    @Test
    fun weatherVisibility() {
        val defaultsRules = WeatherRulesPrefs()
        assertFalse(defaultsRules.active)
        assertTrue(WeatherRules.shouldShow(defaultsRules, reading(), 3))
        assertTrue(WeatherRules.shouldShow(defaultsRules, null, 3))

        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 1), 12))
        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 3), 12))

        listOf(51, 61, 71, 80, 85, 95).forEach { assertTrue("code $it", WeatherRules.shouldShow(notable, reading(18f, it), 12)) }
        assertFalse(WeatherRules.shouldShow(notable, reading(18f, 45), 12))

        assertFalse(WeatherRules.shouldShow(notable, null, 12))

        val nightRules = notable.copy(hideAtNight = true)
        assertFalse(WeatherRules.shouldShow(nightRules, reading(18f, 61), 23))
        assertTrue(WeatherRules.shouldShow(nightRules, reading(18f, 61), 9))
    }

    @Test
    fun weatherTriggerThresholds() {
        val thresholdRules = notable.copy(triggers = "HOT,COLD", hotC = 30, coldC = 5)
        assertTrue(WeatherRules.shouldShow(thresholdRules, reading(30f), 12))
        assertFalse(WeatherRules.shouldShow(thresholdRules, reading(29.9f), 12))
        assertTrue(WeatherRules.shouldShow(thresholdRules, reading(5f), 12))
        assertFalse(WeatherRules.shouldShow(thresholdRules, reading(5.1f), 12))

        val hotOnly = notable.copy(triggers = "HOT")
        assertFalse(WeatherRules.shouldShow(hotOnly, reading(18f, 95), 12))
        assertFalse(WeatherRules.shouldShow(hotOnly, reading(-10f), 12))
        assertTrue(WeatherRules.shouldShow(hotOnly, reading(35f), 12))

        listOf(56, 57, 65, 66, 67, 75, 82, 86, 95, 96, 99).forEach { assertTrue("code $it", WeatherRules.isSevere(it)) }
        listOf(0, 3, 45, 51, 53, 55, 61, 63, 71, 73, 80, 81, 85).forEach { assertFalse("code $it", WeatherRules.isSevere(it)) }
        val severeOnly = notable.copy(triggers = "SEVERE")
        assertTrue(WeatherRules.shouldShow(severeOnly, reading(18f, 65), 12))
        assertFalse(WeatherRules.shouldShow(severeOnly, reading(18f, 61), 12))

        val nightRules = WeatherRulesPrefs(hideAtNight = true)
        assertTrue(nightRules.active)
        assertFalse(WeatherRules.shouldShow(nightRules, reading(), 23))
        assertFalse(WeatherRules.shouldShow(nightRules, null, 23))
        assertTrue(WeatherRules.shouldShow(nightRules, reading(), 9))

        val severeRules = notable.copy(hideAtNight = true)
        assertTrue(WeatherRules.shouldShow(severeRules, reading(18f, 95), 23))
        val noSevere = severeRules.copy(triggers = "RAIN,HOT")
        assertFalse(WeatherRules.shouldShow(noSevere, reading(18f, 95), 23))
        // without the only-when-notable mode there is no alert to tell about
        assertFalse(WeatherRules.shouldShow(WeatherRulesPrefs(hideAtNight = true), reading(18f, 95), 23))
    }

    @Test
    fun nightWindowHandlesDaytimeMidnightAndEmptyRanges() {
        assertTrue(WeatherRules.isNight(22, 6, 23))
        assertTrue(WeatherRules.isNight(22, 6, 0))
        assertTrue(WeatherRules.isNight(22, 6, 5))
        assertFalse(WeatherRules.isNight(22, 6, 6))
        assertFalse(WeatherRules.isNight(22, 6, 12))
        assertTrue(WeatherRules.isNight(22, 6, 22))
        assertTrue(WeatherRules.isNight(1, 5, 1))
        assertFalse(WeatherRules.isNight(1, 5, 5))
        assertFalse(WeatherRules.isNight(7, 7, 7))
    }


    @Test
    fun triggerListsNormalised() {
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

class WeatherDisplayTest {
    private val now = 1_760_000_000_000L
    private val nowSec = now / 1000L

    private fun snapshot(feels: Float = 18.4f, extras: WeatherExtras? = null, detailed: Boolean = true) = WeatherSnapshot(
        temperatureC = 21.2f,
        weatherCode = 61,
        isDay = true,
        fetchedAtMs = now,
        feelsLikeC = feels,
        detailed = detailed,
        extras = extras
    )

    @Test
    fun weatherSlotSelection() {
        val defaultSlotsSlots = WeatherDisplay.slots(WeatherPill.TEMP_ICON, snapshot(), fahrenheit = false)
        assertEquals(WeatherSlotFace.Icon(WeatherLook.Rain, true), defaultSlotsSlots.left)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), defaultSlotsSlots.right)
        assertEquals(defaultSlotsSlots, WeatherDisplay.slots("nonsense", snapshot(), false))

        val temperatureSlots = WeatherDisplay.slots(WeatherPill.TEMP_ONLY, snapshot(), false)
        assertEquals(WeatherSlotFace.Empty, temperatureSlots.left)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), temperatureSlots.right)

        val feelsLikeSlots = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(), false)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), feelsLikeSlots.left)
        assertEquals(WeatherSlotFace.Text("~18\u00B0"), feelsLikeSlots.right)
        val feelsLikeFahrenheit = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(), true)
        assertEquals(WeatherSlotFace.Text("~65\u00B0"), feelsLikeFahrenheit.right)

        val unavailableSlots = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(feels = Float.NaN), false)
        assertEquals(WeatherSlotFace.Empty, unavailableSlots.right)

        val a = snapshot(feels = 18.4f)
        val b = snapshot(feels = 15.0f)
        assertEquals(WeatherDisplay.slots(WeatherPill.TEMP_ICON, a, false), WeatherDisplay.slots(WeatherPill.TEMP_ICON, b, false))
        assertFalse(WeatherDisplay.slots(WeatherPill.FEELS, a, false) == WeatherDisplay.slots(WeatherPill.FEELS, b, false))
    }

    @Test
    fun conditionTextIsShortAndTurnsSunnyIntoClearAtNight() {
        val slots = WeatherDisplay.slots(WeatherPill.TEXT, snapshot(), false)
        assertEquals(WeatherSlotFace.Empty, slots.left)
        assertEquals(WeatherSlotFace.Text("Rain"), slots.right)
        assertEquals("Sunny", WeatherDisplay.shortName(WeatherLook.Clear, true))
        assertEquals("Clear", WeatherDisplay.shortName(WeatherLook.Clear, false))
        WeatherLook.values().forEach { assertTrue(WeatherDisplay.shortName(it, true).length <= 5) }
    }

    @Test
    fun theSmallCirclePicksOneNumber() {
        assertEquals("21\u00B0", WeatherDisplay.compactText(WeatherPill.TEMP_ICON, snapshot(), false))
        assertEquals("18\u00B0", WeatherDisplay.compactText(WeatherPill.FEELS, snapshot(), false))
        assertEquals("21\u00B0", WeatherDisplay.compactText(WeatherPill.FEELS, snapshot(feels = Float.NaN), false))
        assertEquals("21\u00B0", WeatherDisplay.compactText(WeatherPill.TEXT, snapshot(), false))
    }

    @Test
    fun readoutListsAreNormalised() {
        assertEquals(listOf("RAIN", "UV"), WeatherReadouts.parse("UV, RAIN ,RAIN,bogus"))
        assertEquals("HIGH_LOW,AQI", WeatherReadouts.normalize("AQI,HIGH_LOW"))
        assertEquals("", WeatherReadouts.normalize("nothing"))
        assertEquals("RAIN,UV", WeatherReadouts.toggle("RAIN", "UV"))
        assertEquals("UV", WeatherReadouts.toggle("RAIN,UV", "RAIN"))
    }

    @Test
    fun customProviderSkipsAirQuality() {
        val ids = listOf(WeatherReadouts.RAIN, WeatherReadouts.AQI)
        assertEquals(ids, WeatherReadouts.effective(ids, WeatherConfig.PROVIDER_OPEN_METEO))
        assertEquals(listOf(WeatherReadouts.RAIN), WeatherReadouts.effective(ids, WeatherConfig.PROVIDER_CUSTOM))
    }

    @Test
    fun theLookupAsksOnlyForTheSelectedRows() {
        assertEquals("", WeatherConfig.readoutQuery(emptyList()))
        assertEquals("", WeatherConfig.readoutQuery(listOf(WeatherReadouts.HUMIDITY, WeatherReadouts.AQI)))
        val daily = WeatherConfig.readoutQuery(listOf(WeatherReadouts.HIGH_LOW, WeatherReadouts.SUN))
        assertTrue(daily.contains("&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset"))
        assertFalse(daily.contains("hourly"))
        assertTrue(daily.endsWith("&timeformat=unixtime&forecast_days=2"))
        val hourly = WeatherConfig.readoutQuery(listOf(WeatherReadouts.HOURLY))
        assertTrue(hourly.contains("&hourly=temperature_2m,weather_code"))
        assertFalse(hourly.contains("daily"))
    }

    @Test
    fun forecastQueryParameters() {
        val prefs = WeatherPrefs(readouts = "RAIN,HOURLY")
        val url = WeatherConfig.forecastUrl(prefs, 52.52f, 13.41f)!!
        assertTrue(url.contains("apparent_temperature,relative_humidity_2m"))
        assertTrue(url.contains("&daily=precipitation_probability_max"))
        assertTrue(url.contains("&hourly=temperature_2m,weather_code"))
        assertTrue(url.endsWith("&timezone=auto"))
        assertEquals(
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=52.5&longitude=13.4&current=us_aqi&timezone=auto",
            WeatherConfig.airQualityUrl(52.52f, 13.41f)
        )
    }

    private val forecast = """
        {"utc_offset_seconds":3600,
         "current":{"time":$nowSec,"temperature_2m":21.2,"apparent_temperature":18.4,"relative_humidity_2m":64,"weather_code":61,"is_day":1,"wind_speed_10m":12.5,"wind_direction_10m":240},
         "daily":{"time":[1],"temperature_2m_max":[24.1],"temperature_2m_min":[11.0],"precipitation_probability_max":[40],"uv_index_max":[6.4],"sunrise":[${nowSec - 20_000}],"sunset":[${nowSec + 20_000}]},
         "hourly":{"time":[${nowSec - 7200},${nowSec - 1800},${nowSec + 1800},${nowSec + 5400},${nowSec + 9000}],
                   "temperature_2m":[19.0,20.5,21.0,null,22.0],"weather_code":[3,61,61,2,0]}}
    """.trimIndent()

    @Test
    fun aPlainAnswerKeepsTheOldFieldsAndNoExtras() {
        val s = WeatherParser.parseForecast(forecast, emptyList(), now)!!
        assertEquals(21.2f, s.temperatureC, 0.001f)
        assertEquals(61, s.weatherCode)
        assertEquals(12.5f, s.windKmh, 0.001f)
        assertEquals(240, s.windDirectionDeg)
        assertEquals(18.4f, s.feelsLikeC, 0.001f)
        assertTrue(s.detailed)
        assertNull(s.extras)
    }

    @Test
    fun anAnswerWithRowsFillsTheExtras() {
        val all = WeatherReadouts.ALL - WeatherReadouts.AQI
        val x = WeatherParser.parseForecast(forecast, all, now)!!.extras!!
        assertEquals(24.1f, x.highC, 0.001f)
        assertEquals(11.0f, x.lowC, 0.001f)
        assertEquals(40, x.rainChancePct)
        assertEquals(64, x.humidityPct)
        assertEquals(6.4f, x.uvIndex, 0.001f)
        assertEquals(nowSec - 20_000, x.sunriseSec)
        assertEquals(3600, x.utcOffsetSec)
        assertEquals(all.toSet(), x.asked)
        assertEquals(-1, x.aqi)
        // the hour from 2 hours ago and the hour with no temperature are left out
        assertEquals(listOf(nowSec - 1800, nowSec + 1800, nowSec + 9000), x.hourly.map { it.epochSec })
    }

    @Test
    fun missingPiecesOfTheAnswerBecomeUnknownNotZero() {
        val sparse = """{"current":{"temperature_2m":5.0}}"""
        val s = WeatherParser.parseForecast(sparse, listOf(WeatherReadouts.HIGH_LOW, WeatherReadouts.UV), now)!!
        assertTrue(s.feelsLikeC.isNaN())
        val x = s.extras!!
        assertTrue(x.highC.isNaN())
        assertEquals(-1, x.rainChancePct)
        assertEquals(-1f, x.uvIndex, 0f)
        assertTrue(WeatherParser.parseForecast("not json", emptyList(), now) == null)
        assertTrue(WeatherParser.parseForecast("""{"current":{}}""", emptyList(), now) == null)
    }

    @Test
    fun airQualityIsReadFromItsOwnAnswer() {
        assertEquals(42, WeatherParser.parseAqi("""{"current":{"us_aqi":42}}"""))
        assertNull(WeatherParser.parseAqi("""{"current":{"us_aqi":null}}"""))
        assertNull(WeatherParser.parseAqi("""{"current":{}}"""))
        assertNull(WeatherParser.parseAqi("oops"))
    }

    @Test
    fun extrasSurviveTheStoredForm() {
        val original = WeatherParser.parseForecast(forecast, WeatherReadouts.ALL - WeatherReadouts.AQI, now)!!.extras!!.copy(aqi = 77)
        assertEquals(original, WeatherExtras.fromJson(original.toJson()))
        val sparse = WeatherExtras(rainChancePct = 10)
        assertEquals(sparse.rainChancePct, WeatherExtras.fromJson(sparse.toJson())!!.rainChancePct)
        assertTrue(WeatherExtras.fromJson(sparse.toJson())!!.highC.isNaN())
        assertNull(WeatherExtras.fromJson(null))
        assertNull(WeatherExtras.fromJson("{broken"))
    }

    @Test
    fun fetchMissingReadouts() {
        val old = snapshot(detailed = false)
        assertFalse(WeatherDisplay.missingData(WeatherPill.TEMP_ICON, emptyList(), old))
        assertTrue(WeatherDisplay.missingData(WeatherPill.FEELS, emptyList(), old))
        assertTrue(WeatherDisplay.missingData(WeatherPill.TEMP_ICON, listOf(WeatherReadouts.UV), old))
        val noRows = snapshot()
        assertFalse(WeatherDisplay.missingData(WeatherPill.FEELS, emptyList(), noRows))
        assertTrue(WeatherDisplay.missingData(WeatherPill.TEMP_ICON, listOf(WeatherReadouts.UV), noRows))
        val withUv = snapshot(extras = WeatherExtras(asked = setOf(WeatherReadouts.UV)))
        assertFalse(WeatherDisplay.missingData(WeatherPill.TEMP_ICON, listOf(WeatherReadouts.UV), withUv))
        assertTrue(WeatherDisplay.missingData(WeatherPill.TEMP_ICON, listOf(WeatherReadouts.UV, WeatherReadouts.RAIN), withUv))
        // A provider that never answers the feels like does not make it ask again and again
        assertFalse(WeatherDisplay.missingData(WeatherPill.FEELS, emptyList(), snapshot(feels = Float.NaN)))
    }

    @Test
    fun iconAnimatesOnlyWhenFree() {
        assertTrue(WeatherDisplay.iconAnimates(true, true, saverOn = false, systemAnimationsOff = false))
        assertFalse(WeatherDisplay.iconAnimates(false, true, saverOn = false, systemAnimationsOff = false))
        assertFalse(WeatherDisplay.iconAnimates(true, true, saverOn = true, systemAnimationsOff = false))
        assertTrue(WeatherDisplay.iconAnimates(true, false, saverOn = true, systemAnimationsOff = false))
        assertFalse(WeatherDisplay.iconAnimates(true, false, saverOn = false, systemAnimationsOff = true))
    }

    private val extras = WeatherExtras(
        highC = 24.1f, lowC = 11f, rainChancePct = 40, humidityPct = 64, uvIndex = 6.4f,
        sunriseSec = nowSec - 20_000, sunsetSec = nowSec + 20_000, aqi = 42,
        hourly = listOf(HourPoint(nowSec + 1800, 21f, 61)), asked = WeatherReadouts.ALL.toSet()
    )

    @Test
    fun onlyRowsWithAValueAreAvailable() {
        val s = snapshot(extras = extras)
        assertEquals(WeatherReadouts.ALL, WeatherDisplay.availableReadouts(WeatherReadouts.ALL, s, now))
        val partial = snapshot(extras = extras.copy(aqi = -1, uvIndex = -1f, hourly = emptyList()))
        assertEquals(
            listOf(WeatherReadouts.HIGH_LOW, WeatherReadouts.RAIN, WeatherReadouts.HUMIDITY, WeatherReadouts.SUN),
            WeatherDisplay.availableReadouts(WeatherReadouts.ALL, partial, now)
        )
        assertTrue(WeatherDisplay.availableReadouts(WeatherReadouts.ALL, snapshot(), now).isEmpty())
    }

    @Test
    fun dailyRowsExpireNextDay() {
        val s = snapshot(extras = extras.copy(utcOffsetSec = 0))
        val tomorrow = now + 24 * 3_600_000L
        val shown = WeatherDisplay.availableReadouts(WeatherReadouts.ALL, s, tomorrow)
        assertFalse(WeatherReadouts.HIGH_LOW in shown)
        assertFalse(WeatherReadouts.SUN in shown)
        assertTrue(WeatherReadouts.HUMIDITY in shown)
        assertTrue(WeatherReadouts.AQI in shown)
    }

    @Test
    fun theLocalDayFollowsThePlacesOffset() {
        val evening = 86_400L * 20_000 + 21 * 3600L + 1800L
        assertTrue(WeatherDisplay.sameLocalDay(evening, evening + 7200L, 0))
        assertFalse(WeatherDisplay.sameLocalDay(evening, evening + 7200L, 2 * 3600))
    }

    @Test
    fun theIslandGrowsWithTheRows() {
        assertEquals(0f, WeatherDisplay.islandContentHeightDp(emptyList()), 0f)
        val one = WeatherDisplay.islandContentHeightDp(listOf(WeatherReadouts.RAIN))
        val two = WeatherDisplay.islandContentHeightDp(listOf(WeatherReadouts.RAIN, WeatherReadouts.UV))
        val three = WeatherDisplay.islandContentHeightDp(listOf(WeatherReadouts.RAIN, WeatherReadouts.UV, WeatherReadouts.SUN))
        assertEquals(one, two, 0f)
        assertTrue(three > two)
        val hourlyOnly = WeatherDisplay.islandContentHeightDp(listOf(WeatherReadouts.HOURLY))
        assertTrue(hourlyOnly > 2f * WeatherDisplay.ISLAND_PAD_DP + WeatherDisplay.ISLAND_HEADER_DP)
        assertTrue(WeatherDisplay.islandContentHeightDp(WeatherReadouts.ALL) <= WeatherDisplay.ISLAND_MAX_DP)
    }

    @Test
    fun statTextIsShortAndUsesTheChosenUnit() {
        assertEquals("24\u00B0 / 11\u00B0", WeatherDisplay.stat(WeatherReadouts.HIGH_LOW, extras, false, true)!!.value)
        assertEquals("75\u00B0 / 52\u00B0", WeatherDisplay.stat(WeatherReadouts.HIGH_LOW, extras, true, true)!!.value)
        assertEquals("40%", WeatherDisplay.stat(WeatherReadouts.RAIN, extras, false, true)!!.value)
        assertEquals("64%", WeatherDisplay.stat(WeatherReadouts.HUMIDITY, extras, false, true)!!.value)
        assertEquals("6 High", WeatherDisplay.stat(WeatherReadouts.UV, extras, false, true)!!.value)
        assertEquals("42 Good", WeatherDisplay.stat(WeatherReadouts.AQI, extras, false, true)!!.value)
        assertNull(WeatherDisplay.stat(WeatherReadouts.HOURLY, extras, false, true))
    }

    @Test
    fun uvAndAirQualityLabelsFollowTheirScales() {
        assertEquals("Low", WeatherDisplay.uvLabel(2.4f))
        assertEquals("Moderate", WeatherDisplay.uvLabel(3f))
        assertEquals("High", WeatherDisplay.uvLabel(7f))
        assertEquals("Very high", WeatherDisplay.uvLabel(10f))
        assertEquals("Extreme", WeatherDisplay.uvLabel(11f))
        assertEquals("Good", WeatherDisplay.aqiLabel(50))
        assertEquals("Moderate", WeatherDisplay.aqiLabel(51))
        assertEquals("Poor", WeatherDisplay.aqiLabel(150))
        assertEquals("Hazardous", WeatherDisplay.aqiLabel(400))
    }

    @Test
    fun clockTextUsesThePlacesOffsetAndTheClockStyle() {
        val noonUtc = 86_400L * 20_000 + 12 * 3600L + 5 * 60L
        assertEquals("12:05", WeatherDisplay.clockText(noonUtc, 0, true))
        assertEquals("12:05", WeatherDisplay.clockText(noonUtc, 0, false))
        assertEquals("14:05", WeatherDisplay.clockText(noonUtc, 2 * 3600, true))
        assertEquals("2:05", WeatherDisplay.clockText(noonUtc, 2 * 3600, false))
        assertEquals("19:05", WeatherDisplay.clockText(noonUtc, 7 * 3600, true))
        assertEquals("7:05", WeatherDisplay.clockText(noonUtc, 7 * 3600, false))
        assertEquals("00:05", WeatherDisplay.clockText(noonUtc, 12 * 3600, true))
        assertEquals("12:05", WeatherDisplay.clockText(noonUtc, 12 * 3600, false))
        assertEquals("14", WeatherDisplay.hourLabel(noonUtc, 2 * 3600, true))
        assertEquals("2p", WeatherDisplay.hourLabel(noonUtc, 2 * 3600, false))
        assertEquals("12a", WeatherDisplay.hourLabel(noonUtc, 12 * 3600, false))
    }

    @Test
    fun theUpcomingHoursStartAfterNowAndAreCapped() {
        val x = WeatherExtras(hourly = (0 until 10).map { HourPoint(nowSec + it * 3600L - 1800L, 20f, 3) })
        val next = WeatherDisplay.upcomingHours(x, now)
        assertEquals(WeatherDisplay.HOURLY_SHOWN, next.size)
        assertTrue(next.all { it.epochSec > nowSec })
        assertEquals(nowSec + 1800L, next.first().epochSec)
    }

    @Test
    fun anHourIsDayBetweenSunriseAndSunset() {
        val midnight = 86_400L * 20_000
        val x = WeatherExtras(sunriseSec = midnight + 6 * 3600L, sunsetSec = midnight + 19 * 3600L)
        assertTrue(WeatherDisplay.isDayAt(x, midnight + 12 * 3600L))
        assertFalse(WeatherDisplay.isDayAt(x, midnight + 22 * 3600L))
        assertFalse(WeatherDisplay.isDayAt(x, midnight + 3 * 3600L))
        // without sun times it is 06:00 to 20:00
        assertTrue(WeatherDisplay.isDayAt(WeatherExtras(), midnight + 7 * 3600L))
        assertFalse(WeatherDisplay.isDayAt(WeatherExtras(), midnight + 21 * 3600L))
    }

    @Test
    fun theNewChoicesSurviveABackupAndBadOnesAreRepaired() {
        val original = SmartIslandSettings.Default.copy(
            weatherPillMode = WeatherPill.FEELS,
            weatherIslandReadouts = "RAIN,SUN,HOURLY",
            weatherIconAnimated = false,
            weatherIconPauseOnSaver = false
        )
        assertEquals(original, SmartIslandSettings.fromJson(original.toJson(appVersion = "7.0.0")))
        val bad = SmartIslandSettings.Default.copy(weatherPillMode = "flashy", weatherIslandReadouts = "SUN,bogus,SUN")
        val restored = SmartIslandSettings.fromJson(bad.toJson(appVersion = "7.0.0"))
        assertEquals(WeatherPill.TEMP_ICON, restored.weatherPillMode)
        assertEquals("SUN", restored.weatherIslandReadouts)
        val prefs = original.weatherPrefs()
        assertEquals(WeatherPill.FEELS, prefs.pillMode)
        assertEquals(listOf("RAIN", "SUN", "HOURLY"), prefs.effectiveReadouts())
    }

    @Test
    fun theDefaultsKeepTodaysBehaviour() {
        val d = SmartIslandSettings.Default
        assertEquals(WeatherPill.TEMP_ICON, d.weatherPillMode)
        assertEquals("", d.weatherIslandReadouts)
        assertTrue(d.weatherIconAnimated)
        assertNotNull(d.weatherPrefs())
        assertFalse(WeatherDisplay.missingData(d.weatherPillMode, d.weatherPrefs().effectiveReadouts(), snapshot(detailed = false)))
    }
}
