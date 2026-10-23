/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Smart display rules of the idle weather widget in one value (built by `SmartIslandSettings.weatherRules()`): show
 * it only when the weather is notable and/or keep it off the pill at night.
 */
data class WeatherRulesPrefs(
    /** [WeatherRules.MODE_ALWAYS] or [WeatherRules.MODE_NOTABLE]. */
    val mode: String = WeatherRules.MODE_ALWAYS,
    /** Comma list of [WeatherRules] trigger ids; only used with [WeatherRules.MODE_NOTABLE]. */
    val triggers: String = WeatherRules.DEFAULT_TRIGGERS,
    /** Notable at or above this temperature (Celsius). */
    val hotC: Int = WeatherRules.DEFAULT_HOT_C,
    /** Notable at or below this temperature (Celsius). */
    val coldC: Int = WeatherRules.DEFAULT_COLD_C,
    val hideAtNight: Boolean = false,
    /** Local hour (0-23) the night starts. */
    val nightStartHour: Int = WeatherRules.DEFAULT_NIGHT_START,
    /** Local hour (0-23) the night ends (the widget is back from this hour on). */
    val nightEndHour: Int = WeatherRules.DEFAULT_NIGHT_END
) {
    /** True when the rules can take the widget off the pill at all; false = it always takes its turn. */
    val active: Boolean get() = mode == WeatherRules.MODE_NOTABLE || hideAtNight
}

/**
 * Rules for when the weather widget takes a turn on the pill. Open-Meteo has no alert feed, so a "severe alert" is a
 * severe code in the current stored reading.
 */
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

    fun parseTriggers(csv: String): List<String> =
        csv.split(',').map { it.trim() }.filter { it in ALL_TRIGGERS }.distinct().sortedBy { ALL_TRIGGERS.indexOf(it) }

    /** Never empty: a notable-only widget with no trigger could never show, so an empty list means the default. */
    fun normalizeTriggers(csv: String): String =
        parseTriggers(csv).ifEmpty { parseTriggers(DEFAULT_TRIGGERS) }.joinToString(",")

    /** Switches [id] on or off; the last trigger cannot be switched off. */
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

    fun normalizeHour(value: Int): Int = value.coerceIn(0, 23)

    /** Rain, drizzle, showers, snow or a thunderstorm (WMO codes 51-67, 71-77, 80-86, 95-99). */
    fun isPrecipitation(code: Int): Boolean =
        code in 51..67 || code in 71..77 || code in 80..86 || code in 95..99

    /** Thunderstorm, heavy rain or snow, violent showers, freezing rain or drizzle. */
    fun isSevere(code: Int): Boolean = code in SEVERE_CODES

    private val SEVERE_CODES = setOf(56, 57, 65, 66, 67, 75, 82, 86, 95, 96, 99)

    /** Whether the night window contains [hour]; wraps midnight (22 to 6); start = end means no night. */
    fun isNight(startHour: Int, endHour: Int, hour: Int): Boolean {
        val start = normalizeHour(startHour)
        val end = normalizeHour(endHour)
        val h = normalizeHour(hour)
        return when {
            start == end -> false
            start < end -> h in start until end
            else -> h >= start || h < end
        }
    }

    fun isNight(prefs: WeatherRulesPrefs, hour: Int): Boolean = prefs.hideAtNight && isNight(prefs.nightStartHour, prefs.nightEndHour, hour)

    /** True when the reading is notable under the selected triggers. */
    fun isNotable(prefs: WeatherRulesPrefs, s: WeatherSnapshot): Boolean {
        val triggers = parseTriggers(prefs.triggers).ifEmpty { parseTriggers(DEFAULT_TRIGGERS) }
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

    /** Whether a severe alert is on and the person asked to be told about those (it then also shows at night). */
    fun severeOverrides(prefs: WeatherRulesPrefs, s: WeatherSnapshot): Boolean =
        prefs.mode == MODE_NOTABLE && TRIGGER_SEVERE in parseTriggers(prefs.triggers) && isSevere(s.weatherCode)

    /**
     * Whether the weather widget may take a turn at local [hour]:
     * - Rules off (always, no night rule): yes.
     * - No reading yet: yes without rules (the widget's own "nothing to show yet" check still applies); no with "only
     * when notable", since nothing is known to be notable.
     * - Night: no, unless a selected severe alert is on.
     * - Only when notable: yes only when a selected trigger fires.
     */
    fun shouldShow(prefs: WeatherRulesPrefs, snapshot: WeatherSnapshot?, hour: Int): Boolean {
        if (!prefs.active) return true
        if (snapshot == null) return prefs.mode != MODE_NOTABLE && !isNight(prefs, hour)
        if (isNight(prefs, hour) && !severeOverrides(prefs, snapshot)) return false
        return prefs.mode != MODE_NOTABLE || isNotable(prefs, snapshot)
    }
}
