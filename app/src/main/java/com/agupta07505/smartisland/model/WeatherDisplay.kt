/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object WeatherPill {
    const val TEMP_ICON = "TEMP_ICON"
    const val TEMP_ONLY = "TEMP_ONLY"
    const val FEELS = "FEELS"
    const val TEXT = "TEXT"
    val VALID = listOf(TEMP_ICON, TEMP_ONLY, FEELS, TEXT)

    fun normalize(value: String): String = if (value in VALID) value else TEMP_ICON
}

object WeatherReadouts {
    const val HIGH_LOW = "HIGH_LOW"
    const val RAIN = "RAIN"
    const val HUMIDITY = "HUMIDITY"
    const val UV = "UV"
    const val SUN = "SUN"
    const val AQI = "AQI"
    const val HOURLY = "HOURLY"
    val ALL = listOf(HIGH_LOW, RAIN, HUMIDITY, UV, SUN, AQI, HOURLY)

    fun parse(csv: String): List<String> =
        csv.split(',').map { it.trim() }.filter { it in ALL }.distinct().sortedBy { ALL.indexOf(it) }

    fun encode(ids: Collection<String>): String = parse(ids.joinToString(",")).joinToString(",")

    fun normalize(csv: String): String = encode(parse(csv))

    fun toggle(csv: String, id: String): String {
        val current = parse(csv)
        return encode(if (id in current) current - id else current + id)
    }

    /** Air quality comes from Open-Meteo's own service only, so a custom provider never gets it. */
    fun effective(ids: List<String>, provider: String): List<String> =
        if (provider == WeatherConfig.PROVIDER_CUSTOM) ids.filter { it != AQI } else ids
}

data class HourPoint(val epochSec: Long, val tempC: Float, val code: Int)

/** Extra readings behind the island rows. Unknown numbers are NaN, negative or zero as noted. */
data class WeatherExtras(
    val highC: Float = Float.NaN,
    val lowC: Float = Float.NaN,
    /** Chance of rain today in percent; -1 = unknown. */
    val rainChancePct: Int = -1,
    val humidityPct: Int = -1,
    val uvIndex: Float = -1f,
    val sunriseSec: Long = 0L,
    val sunsetSec: Long = 0L,
    val utcOffsetSec: Int = 0,
    val aqi: Int = -1,
    val hourly: List<HourPoint> = emptyList(),
    /** Readouts the lookup asked for, so a missing value is not asked for again and again. */
    val asked: Set<String> = emptySet()
) {
    fun toJson(): String {
        val o = JSONObject()
        if (!highC.isNaN()) o.put("hi", highC.toDouble())
        if (!lowC.isNaN()) o.put("lo", lowC.toDouble())
        o.put("rain", rainChancePct)
        o.put("hum", humidityPct)
        o.put("uv", uvIndex.toDouble().takeIf { !it.isNaN() } ?: -1.0)
        o.put("sr", sunriseSec)
        o.put("ss", sunsetSec)
        o.put("off", utcOffsetSec)
        o.put("aqi", aqi)
        val hours = JSONArray()
        hourly.forEach { hours.put(JSONArray().put(it.epochSec).put(it.tempC.toDouble()).put(it.code)) }
        o.put("h", hours)
        o.put("ask", JSONArray(asked.toList()))
        return o.toString()
    }

    companion object {
        fun fromJson(text: String?): WeatherExtras? {
            if (text.isNullOrBlank()) return null
            val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
            val hours = ArrayList<HourPoint>()
            val array = o.optJSONArray("h")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONArray(i) ?: continue
                    val temp = item.optDouble(1)
                    if (temp.isNaN()) continue
                    hours += HourPoint(item.optLong(0), temp.toFloat(), item.optInt(2, 3))
                }
            }
            val asked = HashSet<String>()
            val askedArray = o.optJSONArray("ask")
            if (askedArray != null) for (i in 0 until askedArray.length()) asked += askedArray.optString(i)
            return WeatherExtras(
                highC = if (o.has("hi")) o.optDouble("hi").toFloat() else Float.NaN,
                lowC = if (o.has("lo")) o.optDouble("lo").toFloat() else Float.NaN,
                rainChancePct = o.optInt("rain", -1),
                humidityPct = o.optInt("hum", -1),
                uvIndex = o.optDouble("uv", -1.0).toFloat(),
                sunriseSec = o.optLong("sr", 0L),
                sunsetSec = o.optLong("ss", 0L),
                utcOffsetSec = o.optInt("off", 0),
                aqi = o.optInt("aqi", -1),
                hourly = hours,
                asked = asked
            )
        }
    }
}

sealed interface WeatherSlotFace {
    object Empty : WeatherSlotFace
    data class Icon(val look: WeatherLook, val isDay: Boolean) : WeatherSlotFace
    data class Text(val text: String) : WeatherSlotFace
}

data class WeatherSlots(val left: WeatherSlotFace, val right: WeatherSlotFace)

data class WeatherStat(val label: String, val value: String)

/** Turns the weather service's answers into a [WeatherSnapshot]. */
object WeatherParser {
    const val HOURS_KEPT = 24

    fun parseForecast(body: String, readouts: List<String>, nowMs: Long): WeatherSnapshot? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val current = root.optJSONObject("current") ?: return null
        if (!current.has("temperature_2m")) return null
        val offset = root.optInt("utc_offset_seconds", 0)
        val humidity = if (current.has("relative_humidity_2m")) current.optInt("relative_humidity_2m", -1) else -1
        val extras = if (readouts.isEmpty()) null else buildExtras(root, readouts, offset, humidity, nowMs)
        return WeatherSnapshot(
            temperatureC = current.optDouble("temperature_2m", 0.0).toFloat(),
            weatherCode = current.optInt("weather_code", 3),
            isDay = current.optInt("is_day", 1) != 0,
            fetchedAtMs = nowMs,
            windKmh = if (current.has("wind_speed_10m")) current.optDouble("wind_speed_10m", -1.0).toFloat() else -1f,
            windDirectionDeg = if (current.has("wind_direction_10m")) current.optInt("wind_direction_10m", -1) else -1,
            feelsLikeC = if (current.has("apparent_temperature")) current.optDouble("apparent_temperature").toFloat() else Float.NaN,
            detailed = true,
            extras = extras
        )
    }

    private fun buildExtras(root: JSONObject, readouts: List<String>, offset: Int, humidity: Int, nowMs: Long): WeatherExtras {
        val daily = root.optJSONObject("daily")
        fun day(key: String): Double = daily?.optJSONArray(key)?.optDouble(0) ?: Double.NaN
        fun dayLong(key: String): Long = daily?.optJSONArray(key)?.optLong(0, 0L) ?: 0L
        val high = day("temperature_2m_max")
        val low = day("temperature_2m_min")
        val rain = day("precipitation_probability_max")
        val uv = day("uv_index_max")
        val hours = ArrayList<HourPoint>()
        val hourly = root.optJSONObject("hourly")
        val times = hourly?.optJSONArray("time")
        val temps = hourly?.optJSONArray("temperature_2m")
        val codes = hourly?.optJSONArray("weather_code")
        if (times != null && temps != null) {
            val nowSec = nowMs / 1000L
            for (i in 0 until minOf(times.length(), temps.length())) {
                val at = times.optLong(i, 0L)
                val temp = temps.optDouble(i)
                if (at < nowSec - 3600L || temp.isNaN()) continue
                hours += HourPoint(at, temp.toFloat(), codes?.optInt(i, 3) ?: 3)
                if (hours.size >= HOURS_KEPT) break
            }
        }
        return WeatherExtras(
            highC = high.toFloat(),
            lowC = low.toFloat(),
            rainChancePct = if (rain.isNaN()) -1 else Math.round(rain).toInt(),
            humidityPct = humidity,
            uvIndex = if (uv.isNaN()) -1f else uv.toFloat(),
            sunriseSec = dayLong("sunrise"),
            sunsetSec = dayLong("sunset"),
            utcOffsetSec = offset,
            hourly = hours,
            asked = readouts.toSet()
        )
    }

    /** US air quality index of an Open-Meteo air-quality answer; null when missing. */
    fun parseAqi(body: String): Int? {
        val current = runCatching { JSONObject(body).optJSONObject("current") }.getOrNull() ?: return null
        if (!current.has("us_aqi") || current.isNull("us_aqi")) return null
        return current.optInt("us_aqi", -1).takeIf { it >= 0 }
    }
}

/** Pure rules for what the weather pill and island draw. */
object WeatherDisplay {
    const val ISLAND_PAD_DP = 8f
    const val ISLAND_HEADER_DP = 36f
    const val ISLAND_GAP_DP = 6f
    const val ISLAND_STAT_ROW_DP = 28f
    const val ISLAND_HOURLY_DP = 46f
    const val ISLAND_MAX_DP = 220f
    const val HOURLY_SHOWN = 4

    fun slots(mode: String, s: WeatherSnapshot, fahrenheit: Boolean): WeatherSlots {
        val temp = IdleWidgets.displayTemp(s.temperatureC, fahrenheit)
        val look = weatherLook(s.weatherCode)
        return when (WeatherPill.normalize(mode)) {
            WeatherPill.TEMP_ONLY -> WeatherSlots(WeatherSlotFace.Empty, WeatherSlotFace.Text(temp))
            WeatherPill.FEELS -> WeatherSlots(
                WeatherSlotFace.Text(temp),
                feelsText(s, fahrenheit)?.let { WeatherSlotFace.Text(it) } ?: WeatherSlotFace.Empty
            )
            WeatherPill.TEXT -> WeatherSlots(WeatherSlotFace.Empty, WeatherSlotFace.Text(shortName(look, s.isDay)))
            else -> WeatherSlots(WeatherSlotFace.Icon(look, s.isDay), WeatherSlotFace.Text(temp))
        }
    }

    fun feelsText(s: WeatherSnapshot, fahrenheit: Boolean): String? =
        if (s.feelsLikeC.isNaN()) null else "~" + IdleWidgets.displayTemp(s.feelsLikeC, fahrenheit)

    /** The small round pill has one value: feels-like in that mode, else the temperature. */
    fun compactText(mode: String, s: WeatherSnapshot, fahrenheit: Boolean): String =
        if (WeatherPill.normalize(mode) == WeatherPill.FEELS && !s.feelsLikeC.isNaN()) {
            IdleWidgets.displayTemp(s.feelsLikeC, fahrenheit)
        } else {
            IdleWidgets.displayTemp(s.temperatureC, fahrenheit)
        }

    /** At most five letters: the pill's value slot is narrow. */
    fun shortName(look: WeatherLook, isDay: Boolean): String = when (look) {
        WeatherLook.Clear -> if (isDay) "Sunny" else "Clear"
        WeatherLook.PartlyCloudy -> "Fair"
        WeatherLook.Cloudy -> "Cloud"
        WeatherLook.Fog -> "Fog"
        WeatherLook.Rain -> "Rain"
        WeatherLook.Snow -> "Snow"
        WeatherLook.Storm -> "Storm"
    }

    /** True when the lookup has to ask again to get what the choices need. */
    fun missingData(pillMode: String, readouts: List<String>, s: WeatherSnapshot): Boolean {
        val needsFeels = WeatherPill.normalize(pillMode) == WeatherPill.FEELS
        if (!needsFeels && readouts.isEmpty()) return false
        if (!s.detailed) return true
        val asked = s.extras?.asked.orEmpty()
        return readouts.any { it !in asked }
    }

    fun iconAnimates(enabled: Boolean, pauseOnSaver: Boolean, saverOn: Boolean, systemAnimationsOff: Boolean): Boolean =
        enabled && !systemAnimationsOff && !(pauseOnSaver && saverOn)

    /** Selected readouts that have a value now. Daily values count only on the day they were fetched. */
    fun availableReadouts(selected: List<String>, s: WeatherSnapshot, nowMs: Long): List<String> {
        val x = s.extras ?: return emptyList()
        val sameDay = sameLocalDay(s.fetchedAtMs / 1000L, nowMs / 1000L, x.utcOffsetSec)
        return selected.filter { id ->
            when (id) {
                WeatherReadouts.HIGH_LOW -> sameDay && !x.highC.isNaN() && !x.lowC.isNaN()
                WeatherReadouts.RAIN -> sameDay && x.rainChancePct >= 0
                WeatherReadouts.HUMIDITY -> x.humidityPct >= 0
                WeatherReadouts.UV -> sameDay && x.uvIndex >= 0f
                WeatherReadouts.SUN -> sameDay && x.sunriseSec > 0L && x.sunsetSec > 0L
                WeatherReadouts.AQI -> x.aqi >= 0
                WeatherReadouts.HOURLY -> upcomingHours(x, nowMs, 1).isNotEmpty()
                else -> false
            }
        }
    }

    /** Height the island needs for [available] rows; 0 = the plain island size. */
    fun islandContentHeightDp(available: List<String>): Float {
        if (available.isEmpty()) return 0f
        val rows = (available.count { it != WeatherReadouts.HOURLY } + 1) / 2
        var height = 2f * ISLAND_PAD_DP + ISLAND_HEADER_DP
        if (rows > 0) height += ISLAND_GAP_DP + rows * ISLAND_STAT_ROW_DP
        if (WeatherReadouts.HOURLY in available) height += ISLAND_GAP_DP + ISLAND_HOURLY_DP
        return height.coerceAtMost(ISLAND_MAX_DP)
    }

    fun stat(id: String, x: WeatherExtras, fahrenheit: Boolean, is24: Boolean): WeatherStat? = when (id) {
        WeatherReadouts.HIGH_LOW ->
            WeatherStat("High / Low", IdleWidgets.displayTemp(x.highC, fahrenheit) + " / " + IdleWidgets.displayTemp(x.lowC, fahrenheit))
        WeatherReadouts.RAIN -> WeatherStat("Rain chance", "${x.rainChancePct}%")
        WeatherReadouts.HUMIDITY -> WeatherStat("Humidity", "${x.humidityPct}%")
        WeatherReadouts.UV -> WeatherStat("UV index", "${Math.round(x.uvIndex)} ${uvLabel(x.uvIndex)}")
        WeatherReadouts.SUN ->
            WeatherStat("Sun", "\u2191" + clockText(x.sunriseSec, x.utcOffsetSec, is24) + " \u2193" + clockText(x.sunsetSec, x.utcOffsetSec, is24))
        WeatherReadouts.AQI -> WeatherStat("Air quality", "${x.aqi} ${aqiLabel(x.aqi)}")
        else -> null
    }

    fun uvLabel(uv: Float): String = when (Math.round(uv)) {
        in Int.MIN_VALUE..2 -> "Low"
        in 3..5 -> "Moderate"
        in 6..7 -> "High"
        in 8..10 -> "Very high"
        else -> "Extreme"
    }

    fun aqiLabel(aqi: Int): String = when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Poor"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very bad"
        else -> "Hazardous"
    }

    fun upcomingHours(x: WeatherExtras, nowMs: Long, count: Int = HOURLY_SHOWN): List<HourPoint> =
        x.hourly.filter { it.epochSec > nowMs / 1000L }.take(count)

    /** Whether the sun is up at [epochSec], from the day's sunrise and sunset; 06:00 to 20:00 when unknown. */
    fun isDayAt(x: WeatherExtras, epochSec: Long): Boolean {
        val t = timeOfDay(epochSec, x.utcOffsetSec)
        if (x.sunriseSec <= 0L || x.sunsetSec <= 0L) return t in 6 * 3600 until 20 * 3600
        val rise = timeOfDay(x.sunriseSec, x.utcOffsetSec)
        val set = timeOfDay(x.sunsetSec, x.utcOffsetSec)
        return if (rise < set) t in rise until set else t >= rise || t < set
    }

    fun clockText(epochSec: Long, offsetSec: Int, is24: Boolean): String {
        val t = timeOfDay(epochSec, offsetSec)
        val hour = t / 3600
        val minute = (t % 3600) / 60
        return if (is24) {
            String.format(Locale.US, "%02d:%02d", hour, minute)
        } else {
            String.format(Locale.US, "%d:%02d", if (hour % 12 == 0) 12 else hour % 12, minute)
        }
    }

    fun hourLabel(epochSec: Long, offsetSec: Int, is24: Boolean): String {
        val hour = timeOfDay(epochSec, offsetSec) / 3600
        return if (is24) String.format(Locale.US, "%02d", hour) else "${if (hour % 12 == 0) 12 else hour % 12}${if (hour < 12) "a" else "p"}"
    }

    fun sameLocalDay(aSec: Long, bSec: Long, offsetSec: Int): Boolean =
        Math.floorDiv(aSec + offsetSec, 86_400L) == Math.floorDiv(bSec + offsetSec, 86_400L)

    private fun timeOfDay(epochSec: Long, offsetSec: Int): Int = Math.floorMod(epochSec + offsetSec, 86_400L).toInt()
}
