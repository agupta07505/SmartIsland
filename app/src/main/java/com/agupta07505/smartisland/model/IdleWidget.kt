/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Idle widgets: small live readouts the collapsed cutout shows while no event is on the island; several take turns.
 * Pure logic; drawing in ui/IdleWidgets.kt, data in util/IdleWidgetData.kt.
 */
enum class IdleWidgetKind(val id: String) {
    /** The hour tick: no widget on the pill, only the hourly tick over the cutout (ui/HourChime.kt). */
    Clock("clock"),
    /**
     * Time on the right with a clock glyph on the left; the island adds the full date. Follows the system 12 / 24 h
     * setting.
     */
    LiveClock("liveclock"),
    Date("date"),
    Weather("weather"),
    /** Wind speed from the weather lookup: glyph on the left, speed on the right; the island adds the direction. */
    Wind("wind"),
    Battery("battery"),
    NextAlarm("alarm"),
    NetSpeed("speed"),
    /**
     * Sunrise, afternoon, sunset and midnight glyphs that tick in at fixed local times (ui/DayMoment.kt); never takes
     * a turn.
     */
    DayMoments("daymoments"),
    /** Unread messages and mail count on the left; the island lists the apps. Sits out while there are none. */
    Notifications("notifications"),
    /** Active Do Not Disturb / mode glyph on the left; the island names it. Sits out while no mode is on. */
    Modes("modes");

    companion object {
        fun fromId(id: String): IdleWidgetKind? = values().firstOrNull { it.id == id }
    }
}

object IdleWidgets {
    /** Temperature units: follow the device region, or force Celsius / Fahrenheit. */
    const val UNIT_AUTO = "AUTO"
    const val UNIT_CELSIUS = "C"
    const val UNIT_FAHRENHEIT = "F"
    val VALID_UNITS = setOf(UNIT_AUTO, UNIT_CELSIUS, UNIT_FAHRENHEIT)

    const val DEFAULT_IDS = "clock"
    const val MIN_CYCLE_SECONDS = 3
    const val MAX_CYCLE_SECONDS = 60
    const val DEFAULT_CYCLE_SECONDS = 6

    fun parse(stored: String): List<IdleWidgetKind> =
        stored.split(',').map { it.trim() }.mapNotNull { IdleWidgetKind.fromId(it) }.distinct()

    fun serialize(kinds: List<IdleWidgetKind>): String = kinds.distinct().joinToString(",") { it.id }

    /**
     * Switches [kind] on (added at the end) or off. The last widget cannot be switched off here: the master switch
     * does that.
     */
    fun toggle(stored: String, kind: IdleWidgetKind): String {
        val current = parse(stored)
        val next = if (kind in current) {
            if (current.size == 1) current else current - kind
        } else {
            current + kind
        }
        return serialize(next)
    }

    fun normalize(stored: String): String = parse(stored).let { if (it.isEmpty()) DEFAULT_IDS else serialize(it) }

    /** Widget at rotation step [step] (any integer; wraps around); null when the list is empty. */
    fun pick(kinds: List<IdleWidgetKind>, step: Int): IdleWidgetKind? =
        if (kinds.isEmpty()) null else kinds[Math.floorMod(step, kinds.size)]

    /** Fahrenheit when forced, or when automatic and the region uses it. */
    fun useFahrenheit(unit: String, regionCountry: String): Boolean = when (unit) {
        UNIT_FAHRENHEIT -> true
        UNIT_CELSIUS -> false
        else -> regionCountry.uppercase() in FAHRENHEIT_REGIONS
    }

    private val FAHRENHEIT_REGIONS = setOf("US", "BS", "BZ", "KY", "PW", "LR", "FM", "MH")

    fun displayTemp(celsius: Float, fahrenheit: Boolean): String {
        val value = if (fahrenheit) celsius * 9f / 5f + 32f else celsius
        return "${Math.round(value)}\u00B0"
    }

    /** Wind speed units: follow the device region (mph in the US, UK and a few others, else km/h), or force one. */
    const val WIND_KMH = "KMH"
    const val WIND_MS = "MS"
    const val WIND_MPH = "MPH"
    const val WIND_KN = "KN"
    val VALID_WIND_UNITS = setOf(UNIT_AUTO, WIND_KMH, WIND_MS, WIND_MPH, WIND_KN)

    private val MPH_REGIONS = setOf("US", "GB", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")

    fun resolveWindUnit(unit: String, regionCountry: String): String = when (unit) {
        WIND_KMH, WIND_MS, WIND_MPH, WIND_KN -> unit
        else -> if (regionCountry.uppercase() in MPH_REGIONS) WIND_MPH else WIND_KMH
    }

    fun windValue(kmh: Float, unit: String): Float = when (unit) {
        WIND_MS -> kmh / 3.6f
        WIND_MPH -> kmh * 0.621371f
        WIND_KN -> kmh * 0.539957f
        else -> kmh
    }

    fun windUnitLabel(unit: String): String = when (unit) {
        WIND_MS -> "m/s"
        WIND_MPH -> "mph"
        WIND_KN -> "kn"
        else -> "km/h"
    }

    /** Pill wind text: the rounded number ("12"); "--" while the speed is unknown. */
    fun displayWind(kmh: Float, unit: String): String =
        if (kmh < 0f) "--" else Math.round(windValue(kmh, unit)).toString()

    /** Island wind text with its unit: "12 km/h", "3.3 m/s". */
    fun windIslandText(kmh: Float, unit: String): String {
        if (kmh < 0f) return "--"
        val value = windValue(kmh, unit)
        return if (unit == WIND_MS) String.format(java.util.Locale.US, "%.1f %s", value, windUnitLabel(unit))
        else "${Math.round(value)} ${windUnitLabel(unit)}"
    }

    /** Compass point the wind comes from ("N", "NE", ...); null when the direction is unknown. */
    fun compass(degrees: Int): String? {
        if (degrees < 0) return null
        val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return points[((degrees % 360) + 22) / 45 % 8]
    }

    /** Short pill speed text ("320K", "4.2M"). */
    fun shortSpeed(bytesPerSecond: Long): String {
        val b = bytesPerSecond.coerceAtLeast(0L).toDouble()
        return when {
            b < 1_000.0 -> "0K"
            b < 1_000_000.0 -> "${Math.round(b / 1_000.0)}K"
            b < 10_000_000.0 -> String.format(java.util.Locale.US, "%.1fM", b / 1_000_000.0)
            else -> "${Math.round(b / 1_000_000.0)}M"
        }
    }
}

/** Weather lookup result: temperature in Celsius and the WMO weather code Open-Meteo reports. */
data class WeatherSnapshot(
    val temperatureC: Float,
    val weatherCode: Int,
    val isDay: Boolean,
    val fetchedAtMs: Long,
    /** Wind speed in km/h; negative = not known (an older stored reading). */
    val windKmh: Float = -1f,
    /** Wind direction in degrees; negative = not known. */
    val windDirectionDeg: Int = -1,
    /** Feels-like temperature in Celsius; NaN = not known. */
    val feelsLikeC: Float = Float.NaN,
    /** True when the lookup asked for the feels-like and humidity fields. */
    val detailed: Boolean = false,
    /** Extra readings behind the island rows; null = none asked for. */
    val extras: WeatherExtras? = null
)

/** Weather code grouped to the few icons the pill can show. */
enum class WeatherLook { Clear, PartlyCloudy, Cloudy, Fog, Rain, Snow, Storm }

fun weatherLook(code: Int): WeatherLook = when (code) {
    0 -> WeatherLook.Clear
    1, 2 -> WeatherLook.PartlyCloudy
    3 -> WeatherLook.Cloudy
    45, 48 -> WeatherLook.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherLook.Rain
    71, 73, 75, 77, 85, 86 -> WeatherLook.Snow
    95, 96, 99 -> WeatherLook.Storm
    else -> WeatherLook.Cloudy
}
