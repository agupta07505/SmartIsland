/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale

/**
 * What the semi-expanded widget island says per widget. Pure rules only (when there is something to show, the texts and the
 * unit maths), so they can be unit tested; the drawing is `IdleWidgetIslandContent` in ui/IdleWidgets.kt.
 *
 * The clock is not here on purpose: its job is the hourly tick (HourChimeLayer), it never sits on the pill and has no island.
 */
object WidgetIslandContent {
    /** Kept for [alarmInWindow]; the alarm's island itself does not wait for this window (see [hasContent]). */
    const val ALARM_WINDOW_MS = 12 * 60 * 60 * 1000L

    /** Battery island line: watts or volts, chosen in settings. */
    const val BATTERY_WATT = "WATT"
    const val BATTERY_VOLT = "VOLT"
    val VALID_BATTERY_METRICS = setOf(BATTERY_WATT, BATTERY_VOLT)

    /**
     * Whether [kind] has anything to show in its island right now. A pill glyph tap on a widget without content does nothing, and an open
     * island whose content goes away (charger unplugged, alarm rang) closes.
     *  - Date, net speed: always.
     *  - Weather and wind: once a reading is there.
     *  - Battery: only while charging (the status bar has the level).
     *  - Next alarm: whenever an alarm is still to come, however far away.
     *  - Notifications: while at least one message or mail is waiting.
     *  - Modes: while a mode or Do Not Disturb is on.
     *  - Clock: never.
     */
    fun hasContent(
        kind: IdleWidgetKind,
        charging: Boolean,
        nextAlarmMs: Long,
        nowMs: Long,
        hasWeather: Boolean,
        notificationCount: Int = 0,
        modeActive: Boolean = false
    ): Boolean = when (kind) {
        IdleWidgetKind.Clock, IdleWidgetKind.DayMoments -> false
        IdleWidgetKind.Date, IdleWidgetKind.NetSpeed -> true
        IdleWidgetKind.Weather, IdleWidgetKind.Wind -> hasWeather
        IdleWidgetKind.Battery -> charging
        IdleWidgetKind.NextAlarm -> nextAlarmMs > nowMs
        IdleWidgetKind.Notifications -> notificationCount > 0
        IdleWidgetKind.Modes -> modeActive
    }

    fun alarmInWindow(nextAlarmMs: Long, nowMs: Long): Boolean =
        nextAlarmMs > nowMs && nextAlarmMs - nowMs <= ALARM_WINDOW_MS

    /** "st", "nd", "rd" or "th" for a day of the month (11th, 12th and 13th are "th"). */
    fun ordinalSuffix(day: Int): String {
        val d = Math.floorMod(day, 100)
        if (d in 11..13) return "th"
        return when (d % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    /** "Thursday 24th of February 2026". */
    fun englishLongDate(weekdayName: String, day: Int, monthName: String, year: Int): String =
        "$weekdayName $day${ordinalSuffix(day)} of $monthName $year"

    /** The English wording only makes sense in English; other languages use their own long date. */
    fun usesEnglishOrdinalDate(language: String): Boolean = language.equals("en", ignoreCase = true)

    /** Time left as "5h 12m", "42m", "1d 2h" or "<1m". Rounded down to the minute. */
    fun durationText(ms: Long): String {
        val totalMinutes = (ms.coerceAtLeast(0L) / 60_000L)
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return when {
            totalMinutes < 1 -> "<1m"
            days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
            hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            else -> "${minutes}m"
        }
    }

    /**
     * Charging power in watts from the battery's current reading and its voltage. Android defines the current in microamps, but a number of
     * devices report milliamps: anything below [MILLIAMP_CEILING] is read as milliamps. The sign differs by device too, so it is dropped.
     * Null when either value is missing (0 or the unsupported marker).
     */
    fun batteryWatts(currentRaw: Long, voltageMv: Int): Double? {
        if (voltageMv <= 0 || currentRaw == Long.MIN_VALUE) return null
        val magnitude = Math.abs(currentRaw)
        if (magnitude == 0L) return null
        val amps = if (magnitude < MILLIAMP_CEILING) magnitude / 1_000.0 else magnitude / 1_000_000.0
        return amps * voltageMv / 1_000.0
    }

    private const val MILLIAMP_CEILING = 20_000L

    fun wattsText(watts: Double): String = String.format(Locale.US, "%.1f W", watts)

    fun voltsText(voltageMv: Int): String = String.format(Locale.US, "%.2f V", voltageMv / 1_000.0)

    /** Battery temperature (tenths of a degree Celsius, as the system reports it) in the chosen unit, one decimal: "36.2°C". */
    fun batteryTempText(tenthsC: Int, fahrenheit: Boolean): String {
        val c = tenthsC / 10.0
        return if (fahrenheit) String.format(Locale.US, "%.1f°F", c * 9.0 / 5.0 + 32.0)
        else String.format(Locale.US, "%.1f°C", c)
    }

    /** Speed for the island ("0 KB/s", "320 KB/s", "4.2 MB/s"): bytes per second in. */
    fun speedText(bytesPerSecond: Long): String {
        val b = bytesPerSecond.coerceAtLeast(0L).toDouble()
        return when {
            b < 1_000.0 -> "0 KB/s"
            b < 1_000_000.0 -> "${Math.round(b / 1_000.0)} KB/s"
            b < 10_000_000.0 -> String.format(Locale.US, "%.1f MB/s", b / 1_000_000.0)
            else -> "${Math.round(b / 1_000_000.0)} MB/s"
        }
    }

    /**
     * The name of a WMO weather code. Open-Meteo, the weather source, reports only the code, no text, so these are the standard WMO names it documents.
     */
    fun weatherName(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Sunny" else "Clear"
        1 -> if (isDay) "Mostly sunny" else "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rainy"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snowy"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm, hail"
        else -> "Cloudy"
    }
}
