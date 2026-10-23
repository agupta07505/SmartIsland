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
    fun refreshChoicesAreTheFiveTheSettingsOffer() {
        assertEquals(listOf(15, 30, 60, 180, 0), WeatherConfig.REFRESH_CHOICES)
        assertEquals(15 * 60_000L, WeatherConfig.refreshMs(15))
        assertEquals(3 * 60 * 60_000L, WeatherConfig.refreshMs(180))
        assertEquals(0L, WeatherConfig.refreshMs(WeatherConfig.REFRESH_MANUAL))
        // An unknown value counts as the default, not as Manual.
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
    fun aManualPlaceNeedsACityAndValidCoordinates() {
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
        // A key set for another provider is never sent to Open-Meteo.
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
        // An address that already has a query keeps it.
        val withQuery = prefs.copy(customUrl = "https://example.com/forecast?model=best", customKey = "")
        val second = WeatherConfig.forecastUrl(withQuery, 1f, 2f)!!
        assertTrue(second.startsWith("https://example.com/forecast?model=best&latitude="))
        assertFalse(second.contains("apikey"))
    }

    @Test
    fun aCustomProviderWithoutAValidAddressHasNoUrl() {
        assertNull(WeatherConfig.forecastUrl(WeatherPrefs(provider = WeatherConfig.PROVIDER_CUSTOM), 1f, 2f))
        assertNull(WeatherConfig.forecastUrl(WeatherPrefs(provider = WeatherConfig.PROVIDER_CUSTOM, customUrl = "http://x.com"), 1f, 2f))
        assertFalse(WeatherConfig.providerUsable(WeatherPrefs(provider = WeatherConfig.PROVIDER_CUSTOM)))
        assertTrue(WeatherConfig.providerUsable(WeatherPrefs()))
    }

    @Test
    fun theSignatureChangesWithThePlaceChoiceAndTheProviderOnly() {
        val base = WeatherPrefs()
        assertEquals(WeatherConfig.LEGACY_SIGNATURE, WeatherConfig.signature(base))
        // Auto and Last known share one: the place moves by itself, the reading is not for a different place choice.
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(base.copy(source = WeatherConfig.SOURCE_LAST_KNOWN)))
        // The refresh interval does not matter.
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(base.copy(refreshMinutes = 180)))
        val berlin = base.copy(source = WeatherConfig.SOURCE_MANUAL, manualCity = "Berlin", manualLat = 52.5f, manualLon = 13.4f)
        val paris = berlin.copy(manualCity = "Paris", manualLat = 48.9f, manualLon = 2.4f)
        assertNotEquals(WeatherConfig.signature(base), WeatherConfig.signature(berlin))
        assertNotEquals(WeatherConfig.signature(berlin), WeatherConfig.signature(paris))
        val custom = base.copy(provider = WeatherConfig.PROVIDER_CUSTOM, customUrl = "https://example.com/forecast")
        assertNotEquals(WeatherConfig.signature(base), WeatherConfig.signature(custom))
        // The key is not part of it (changing a key does not throw the reading away).
        assertEquals(WeatherConfig.signature(custom), WeatherConfig.signature(custom.copy(customKey = "k")))
    }

    @Test
    fun theCitySearchAddressIsEncodedAndNeedsTwoLetters() {
        assertNull(WeatherConfig.geocodingUrl("a", "en"))
        assertNull(WeatherConfig.geocodingUrl("   ", "en"))
        val url = WeatherConfig.geocodingUrl("São Paulo", "pt")!!
        assertTrue(url.startsWith("https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o+Paulo"))
        assertTrue(url.contains("&language=pt"))
        // A language that is not a plain code is replaced by English.
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
    fun cityParsingSurvivesAnswersThatAreNotWhatWasExpected() {
        assertTrue(WeatherConfig.parseCities("{}").isEmpty())
        assertTrue(WeatherConfig.parseCities("not json").isEmpty())
        assertTrue(WeatherConfig.parseCities("""{"results":[]}""").isEmpty())
    }

    @Test
    fun weatherChoicesSurviveABackupButTheKeyDoesNot() {
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
    fun badStoredWeatherValuesAreRepairedOnRestore() {
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
    fun pillAndReadoutChoicesNeverChangeWhichPlaceAReadingBelongsTo() {
        val base = WeatherPrefs()
        val changed = base.copy(pillMode = WeatherPill.FEELS, readouts = "RAIN,SUN,HOURLY")
        assertEquals(WeatherConfig.signature(base), WeatherConfig.signature(changed))
    }

    @Test
    fun theSelectedRowsAreReadSortedAndAirQualityIsDroppedForACustomProvider() {
        val prefs = WeatherPrefs(readouts = "HOURLY,bogus,AQI,RAIN,RAIN")
        assertEquals(listOf("RAIN", "AQI", "HOURLY"), prefs.effectiveReadouts())
        val custom = prefs.copy(provider = WeatherConfig.PROVIDER_CUSTOM)
        assertEquals(listOf("RAIN", "HOURLY"), custom.effectiveReadouts())
        assertEquals(emptyList<String>(), WeatherPrefs().effectiveReadouts())
    }

    @Test
    fun theForecastUrlAsksForNoExtraRowsByDefault() {
        val url = WeatherConfig.forecastUrl(WeatherPrefs(), 52.52f, 13.41f)!!
        assertFalse(url.contains("daily"))
        assertFalse(url.contains("hourly"))
        assertFalse(url.contains("timeformat"))
    }
}

class WeatherRefreshTest {
    private val min = 60_000L

    @Test
    fun withNoReadingItAlwaysAsksEvenInManual() {
        assertTrue(WeatherRefresh.shouldFetch(30 * min, hasWeather = false, ageMs = 0, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(0L, hasWeather = false, ageMs = 0, missingData = false))
    }

    @Test
    fun aReadingIsAskedAgainOnlyWhenItIsOldEnough() {
        assertFalse(WeatherRefresh.shouldFetch(30 * min, hasWeather = true, ageMs = 29 * min, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(30 * min, hasWeather = true, ageMs = 30 * min, missingData = false))
        assertFalse(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 100 * min, missingData = false))
        assertTrue(WeatherRefresh.shouldFetch(15 * min, hasWeather = true, ageMs = 16 * min, missingData = false))
    }

    @Test
    fun manualNeverAsksOnItsOwnOnceThereIsAReading() {
        assertFalse(WeatherRefresh.shouldFetch(0L, hasWeather = true, ageMs = 99_999 * min, missingData = false))
        assertFalse(WeatherRefresh.shouldFetch(0L, hasWeather = true, ageMs = 99_999 * min, missingData = true))
    }

    @Test
    fun aMissingWindIsAskedAgainButNotMoreOftenThanTheRetryPeriod() {
        assertFalse(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 2 * min, missingData = true))
        assertTrue(WeatherRefresh.shouldFetch(180 * min, hasWeather = true, ageMs = 5 * min, missingData = true))
    }

    @Test
    fun theWaitFollowsTheIntervalAndNeverGoesBelowTheRetryPeriod() {
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = false, ageMs = 0))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 40 * min))
        assertEquals(20 * min, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 10 * min))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(30 * min, hasWeather = true, ageMs = 28 * min))
        assertEquals(WeatherRefresh.MANUAL_POLL_MS, WeatherRefresh.waitMs(0L, hasWeather = true, ageMs = 5 * min))
        assertEquals(WeatherRefresh.RETRY_MS, WeatherRefresh.waitMs(0L, hasWeather = false, ageMs = 0))
    }
}
