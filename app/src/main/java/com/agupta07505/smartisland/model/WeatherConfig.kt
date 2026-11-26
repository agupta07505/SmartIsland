/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

data class WeatherPrefs(
    val source: String = WeatherConfig.SOURCE_AUTO,
    val manualCity: String = "",
    val manualLat: Float = 0f,
    val manualLon: Float = 0f,
    /** Minutes between lookups; [WeatherConfig.REFRESH_MANUAL] (0) = only when asked. */
    val refreshMinutes: Int = WeatherConfig.DEFAULT_REFRESH_MINUTES,
    val provider: String = WeatherConfig.PROVIDER_OPEN_METEO,
    val customUrl: String = "",
    /** Optional API key of the custom service, sent as the `apikey` parameter. Never part of a backup. */
    val customKey: String = "",
    val pillMode: String = WeatherPill.TEMP_ICON,
    val readouts: String = ""
) {
    val refreshMs: Long get() = WeatherConfig.refreshMs(refreshMinutes)

    fun effectiveReadouts(): List<String> = WeatherReadouts.effective(WeatherReadouts.parse(readouts), provider)
}

data class CityResult(val name: String, val admin: String, val country: String, val lat: Float, val lon: Float) {
    val label: String
        get() = listOf(name, admin.takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) },
            country.takeIf { it.isNotBlank() })
            .filterNotNull()
            .joinToString(", ")
}

object WeatherConfig {
    const val SOURCE_AUTO = "AUTO"

    const val SOURCE_MANUAL = "MANUAL"

    const val SOURCE_LAST_KNOWN = "LAST_KNOWN"
    val VALID_SOURCES = setOf(SOURCE_AUTO, SOURCE_MANUAL, SOURCE_LAST_KNOWN)

    const val PROVIDER_OPEN_METEO = "OPEN_METEO"
    const val PROVIDER_CUSTOM = "CUSTOM"
    val VALID_PROVIDERS = setOf(PROVIDER_OPEN_METEO, PROVIDER_CUSTOM)

    const val REFRESH_MANUAL = 0
    const val DEFAULT_REFRESH_MINUTES = 30

    val REFRESH_CHOICES = listOf(15, 30, 60, 180, REFRESH_MANUAL)

    const val OPEN_METEO_FORECAST = "https://api.open-meteo.com/v1/forecast"
    const val OPEN_METEO_GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    const val OPEN_METEO_AIR_QUALITY = "https://air-quality-api.open-meteo.com/v1/air-quality"

    const val CURRENT_FIELDS =
        "temperature_2m,weather_code,is_day,wind_speed_10m,wind_direction_10m,apparent_temperature,relative_humidity_2m"

    const val MAX_CITY_LENGTH = 80
    const val MAX_URL_LENGTH = 300
    const val MAX_KEY_LENGTH = 120
    const val MIN_SEARCH_LENGTH = 2

    /** A cached reading from a version without choices belongs to the default place and provider. */
    val LEGACY_SIGNATURE: String get() = signature(WeatherPrefs())

    fun normalizeSource(value: String): String = if (value in VALID_SOURCES) value else SOURCE_AUTO

    fun normalizeProvider(value: String): String = if (value in VALID_PROVIDERS) value else PROVIDER_OPEN_METEO

    fun normalizeRefreshMinutes(value: Int): Int = if (value in REFRESH_CHOICES) value else DEFAULT_REFRESH_MINUTES

    fun refreshMs(minutes: Int): Long = normalizeRefreshMinutes(minutes) * 60_000L

    fun normalizeCity(value: String): String = value.trim().take(MAX_CITY_LENGTH)

    fun normalizeCustomUrl(value: String): String = value.trim().take(MAX_URL_LENGTH)

    fun normalizeKey(value: String): String = value.trim().take(MAX_KEY_LENGTH)

    fun validCoordinates(lat: Float, lon: Float): Boolean =
        !lat.isNaN() && !lon.isNaN() && lat in -90f..90f && lon in -180f..180f

    fun hasManualPlace(prefs: WeatherPrefs): Boolean =
        prefs.manualCity.isNotBlank() && validCoordinates(prefs.manualLat, prefs.manualLon)

    fun round1(value: Double): Float = (Math.round(value * 10.0) / 10.0).toFloat()

    fun isValidCustomUrl(url: String): Boolean {
        val text = url.trim()
        if (text.isEmpty() || text.length > MAX_URL_LENGTH) return false
        if (!text.startsWith("https://", ignoreCase = true)) return false
        if (text.any { it.isWhitespace() }) return false
        return runCatching { !URI(text).host.isNullOrBlank() }.getOrDefault(false)
    }

    fun forecastUrl(prefs: WeatherPrefs, lat: Float, lon: Float): String? {
        val base = if (prefs.provider == PROVIDER_CUSTOM) {
            val custom = prefs.customUrl.trim()
            if (!isValidCustomUrl(custom)) return null
            custom
        } else {
            OPEN_METEO_FORECAST
        }
        val query = String.format(
            Locale.US,
            "latitude=%.1f&longitude=%.1f&current=%s%s&timezone=auto",
            lat, lon, CURRENT_FIELDS, readoutQuery(prefs.effectiveReadouts())
        )
        val key = prefs.customKey.trim()
        val keyPart = if (prefs.provider == PROVIDER_CUSTOM
            && key.isNotEmpty()) "&apikey=" + URLEncoder.encode(key, "UTF-8") else ""
        return base + (if ('?' in base) "&" else "?") + query + keyPart
    }

    fun readoutQuery(readouts: List<String>): String {
        val daily = ArrayList<String>()
        if (WeatherReadouts.HIGH_LOW in readouts) { daily += "temperature_2m_max"; daily += "temperature_2m_min" }
        if (WeatherReadouts.RAIN in readouts) daily += "precipitation_probability_max"
        if (WeatherReadouts.UV in readouts) daily += "uv_index_max"
        if (WeatherReadouts.SUN in readouts) { daily += "sunrise"; daily += "sunset" }
        val hourly = WeatherReadouts.HOURLY in readouts
        if (daily.isEmpty() && !hourly) return ""
        val sb = StringBuilder()
        if (daily.isNotEmpty()) sb.append("&daily=").append(daily.joinToString(","))
        if (hourly) sb.append("&hourly=temperature_2m,weather_code")
        return sb.append("&timeformat=unixtime&forecast_days=2").toString()
    }

    fun airQualityUrl(lat: Float, lon: Float): String =
        String.format(Locale.US, "$OPEN_METEO_AIR_QUALITY?latitude=%.1f&longitude=%.1f&current=us_aqi&timezone=auto",
            lat, lon)

    fun signature(prefs: WeatherPrefs): String {
        val place = if (prefs.source == SOURCE_MANUAL) String.format(Locale.US, "M%.1f,%.1f", prefs.manualLat,
            prefs.manualLon) else "D"
        val provider = if (prefs.provider == PROVIDER_CUSTOM) "C" + prefs.customUrl.trim().hashCode() else "O"
        return "$place|$provider"
    }

    fun geocodingUrl(query: String, language: String): String? {
        val text = query.trim()
        if (text.length < MIN_SEARCH_LENGTH) return null
        val lang = language.lowercase(Locale.US).takeIf { it.length in 2..3 && it.all { c -> c in 'a'..'z' } } ?: "en"
        return OPEN_METEO_GEOCODING + "?name=" + URLEncoder.encode(text.take(MAX_CITY_LENGTH), "UTF-8") +
            "&count=6&language=$lang&format=json"
    }

    /**
     * Places of an Open-Meteo geocoding answer, rounded to 1 decimal, entries without a name or valid place are
     * dropped
     */
    fun parseCities(json: String): List<CityResult> {
        val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return emptyList()
        val out = ArrayList<CityResult>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            if (name.isEmpty() || !item.has("latitude") || !item.has("longitude")) continue
            val lat = round1(item.optDouble("latitude"))
            val lon = round1(item.optDouble("longitude"))
            if (!validCoordinates(lat, lon)) continue
            out += CityResult(
                name = name,
                admin = item.optString("admin1", "").trim(),
                country = item.optString("country", "").trim(),
                lat = lat,
                lon = lon
            )
        }
        return out
    }
}

object WeatherRefresh {
    const val RETRY_MS = 5 * 60 * 1000L

    /** With the Manual interval the stored reading is checked this often (ms) so a refresh done in Settings shows up. */
    const val MANUAL_POLL_MS = 60 * 1000L

    fun shouldFetch(refreshMs: Long, hasWeather: Boolean, ageMs: Long, missingData: Boolean): Boolean {
        if (!hasWeather) return true
        if (refreshMs <= 0L) return false
        return ageMs >= refreshMs || (missingData && ageMs >= RETRY_MS)
    }

    fun waitMs(refreshMs: Long, hasWeather: Boolean, ageMs: Long): Long {
        if (!hasWeather) return RETRY_MS
        if (refreshMs <= 0L) return MANUAL_POLL_MS
        if (ageMs >= refreshMs) return RETRY_MS
        return (refreshMs - ageMs).coerceAtLeast(RETRY_MS)
    }
}
