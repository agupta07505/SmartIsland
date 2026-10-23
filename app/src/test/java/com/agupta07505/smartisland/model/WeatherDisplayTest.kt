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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun theDefaultPillIsTodaysLook() {
        val slots = WeatherDisplay.slots(WeatherPill.TEMP_ICON, snapshot(), fahrenheit = false)
        assertEquals(WeatherSlotFace.Icon(WeatherLook.Rain, true), slots.left)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), slots.right)
        assertEquals(slots, WeatherDisplay.slots("nonsense", snapshot(), false))
    }

    @Test
    fun temperatureOnlyLeavesTheLeftSideEmpty() {
        val slots = WeatherDisplay.slots(WeatherPill.TEMP_ONLY, snapshot(), false)
        assertEquals(WeatherSlotFace.Empty, slots.left)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), slots.right)
    }

    @Test
    fun feelsLikeShowsTheTemperatureAndThenTheFeelsLikeWithATilde() {
        val slots = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(), false)
        assertEquals(WeatherSlotFace.Text("21\u00B0"), slots.left)
        assertEquals(WeatherSlotFace.Text("~18\u00B0"), slots.right)
        val fahrenheit = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(), true)
        assertEquals(WeatherSlotFace.Text("~65\u00B0"), fahrenheit.right)
    }

    @Test
    fun feelsLikeWithoutAValueLeavesTheRightSideEmpty() {
        val slots = WeatherDisplay.slots(WeatherPill.FEELS, snapshot(feels = Float.NaN), false)
        assertEquals(WeatherSlotFace.Empty, slots.right)
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
    fun aChangedFeelsLikeOnlyChangesTheFacesInThatMode() {
        val a = snapshot(feels = 18.4f)
        val b = snapshot(feels = 15.0f)
        assertEquals(WeatherDisplay.slots(WeatherPill.TEMP_ICON, a, false), WeatherDisplay.slots(WeatherPill.TEMP_ICON, b, false))
        assertFalse(WeatherDisplay.slots(WeatherPill.FEELS, a, false) == WeatherDisplay.slots(WeatherPill.FEELS, b, false))
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
    fun aCustomProviderNeverAsksForAirQuality() {
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
    fun theForecastUrlCarriesTheExtrasAndStillEndsWithTheTimezone() {
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
        // The hour from two hours ago and the hour with no temperature are left out.
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
    fun theLookupAsksAgainOnlyWhenTheChoicesNeedDataTheReadingLacks() {
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
        // A provider that never answers the feels like does not make it ask again and again.
        assertFalse(WeatherDisplay.missingData(WeatherPill.FEELS, emptyList(), snapshot(feels = Float.NaN)))
    }

    @Test
    fun anIconAnimatesOnlyWhenNothingHoldsItStill() {
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
    fun dailyRowsDisappearTheDayAfterTheReading() {
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
        // Without sun times it is 06:00 to 20:00.
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
