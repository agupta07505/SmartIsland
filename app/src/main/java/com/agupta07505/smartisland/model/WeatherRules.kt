/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Smart display rules of the idle weather widget in 1 value (built by `SmartIslandSettings.weatherRules()`): show it
 * only when the weather is notable and/or keep it off the pill at night
 */
data class WeatherRulesPrefs(
    val mode: String = WeatherRules.MODE_ALWAYS,
    val triggers: String = WeatherRules.DEFAULT_TRIGGERS,
    val hotC: Int = WeatherRules.DEFAULT_HOT_C,
    val coldC: Int = WeatherRules.DEFAULT_COLD_C,
    val hideAtNight: Boolean = false,
    val nightStartHour: Int = WeatherRules.DEFAULT_NIGHT_START,
    val nightEndHour: Int = WeatherRules.DEFAULT_NIGHT_END
) {
    val active: Boolean get() = mode == WeatherRules.MODE_NOTABLE || hideAtNight
}

object WeatherRules {
    const val MODE_ALWAYS = "ALWAYS"
    const val MODE_NOTABLE = "NOTABLE"
    val VALID_MODES = setOf(MODE_ALWAYS, MODE_NOTABLE)

    const val TRIGGER_RAIN = "RAIN"
    const val TRIGGER_HOT = "HOT"
    const val TRIGGER_COLD = "COLD"
    const val TRIGGER_SEVERE = "SEVERE"
    val ALL_TRIGGERS = listOf(TRIGGER_RAIN, TRIGGER_HOT, TRIGGER_COLD, TRIGGER_SEVERE)
    const val DEFAULT_TRIGGERS = "RAIN,HOT,COLD,SEVERE"

    const val DEFAULT_HOT_C = 30
    const val MIN_HOT_C = 15
    const val MAX_HOT_C = 50
    const val DEFAULT_COLD_C = 5
    const val MIN_COLD_C = -30
    const val MAX_COLD_C = 20

    const val DEFAULT_NIGHT_START = 22
    const val DEFAULT_NIGHT_END = 6

    fun normalizeMode(value: String): String = if (value in VALID_MODES) value else MODE_ALWAYS

    fun parseTriggers(csv: String): List<String> {
        val selected = csv.split(',').map { it.trim() }.toSet()
        return ALL_TRIGGERS.filter { it in selected }
    }

    /** Never empty: a notable-only widget with no trigger could never show, so an empty list means the default. */
    fun normalizeTriggers(csv: String): String =
        parseTriggers(csv).ifEmpty { ALL_TRIGGERS }.joinToString(",")

    fun toggleTrigger(csv: String, id: String): String {
        val current = parseTriggers(csv)
        if (id !in ALL_TRIGGERS) return normalizeTriggers(csv)
        val next = if (id in current) {
            if (current.size == 1) current else current - id
        } else {
            current + id
        }
        return normalizeTriggers(next.joinToString(","))
    }

    fun normalizeHotC(value: Int): Int = value.coerceIn(MIN_HOT_C, MAX_HOT_C)

    fun normalizeColdC(value: Int): Int = value.coerceIn(MIN_COLD_C, MAX_COLD_C)

    fun normalizeHour(value: Int): Int = IdleSchedule.normalizeHour(value)

    fun isPrecipitation(code: Int): Boolean =
        code in 51..67 || code in 71..77 || code in 80..86 || code in 95..99

    fun isSevere(code: Int): Boolean = code in SEVERE_CODES

    private val SEVERE_CODES = setOf(56, 57, 65, 66, 67, 75, 82, 86, 95, 96, 99)

    fun isNight(startHour: Int, endHour: Int, hour: Int): Boolean = IdleSchedule.inWindow(startHour, endHour, hour)

    fun isNight(prefs: WeatherRulesPrefs, hour: Int): Boolean = prefs.hideAtNight
            && isNight(prefs.nightStartHour, prefs.nightEndHour, hour)

    fun isNotable(prefs: WeatherRulesPrefs, s: WeatherSnapshot): Boolean {
        val triggers = parseTriggers(prefs.triggers).ifEmpty { ALL_TRIGGERS }
        return triggers.any { id ->
            when (id) {
                TRIGGER_RAIN -> isPrecipitation(s.weatherCode)
                TRIGGER_HOT -> s.temperatureC >= normalizeHotC(prefs.hotC)
                TRIGGER_COLD -> s.temperatureC <= normalizeColdC(prefs.coldC)
                TRIGGER_SEVERE -> isSevere(s.weatherCode)
                else -> false
            }
        }
    }

    fun severeOverrides(prefs: WeatherRulesPrefs, s: WeatherSnapshot): Boolean =
        prefs.mode == MODE_NOTABLE && TRIGGER_SEVERE in parseTriggers(prefs.triggers) && isSevere(s.weatherCode)

    fun shouldShow(prefs: WeatherRulesPrefs, snapshot: WeatherSnapshot?, hour: Int): Boolean {
        if (!prefs.active) return true
        if (snapshot == null) return prefs.mode != MODE_NOTABLE && !isNight(prefs, hour)
        if (isNight(prefs, hour) && !severeOverrides(prefs, snapshot)) return false
        return prefs.mode != MODE_NOTABLE || isNotable(prefs, snapshot)
    }
}
