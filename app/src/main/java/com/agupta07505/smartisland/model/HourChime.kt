/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Clock widget: once an hour the pill ticks the hour (left slot) and minutes (right slot) over; the status bar
 * already shows the time. Drawing is in ui/HourChime.kt.
 */
object HourChime {
    const val MINUTES_BEFORE = "59"
    const val MINUTES_AFTER = "00"

    /** The old time is shown this long (ms) before the tick, so "59" is seen. */
    const val HOLD_BEFORE_MS = 700
    /** Tick length (ms), including its slow suspended pull-back ([IdleSlots.heavyTickProgress]). */
    const val TICK_MS = 2200
    /** The new time stays this long (ms), then the pill goes back to what it showed. */
    const val HOLD_AFTER_MS = 2500
    const val FADE_IN_MS = 160
    const val FADE_OUT_MS = 260

    /** Next full hour strictly after [nowMs] in [zone] (right for half-hour zones and daylight saving). */
    fun nextFullHourMs(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        return cal.timeInMillis
    }

    fun hourOf(ms: Long, zone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(zone).also { it.timeInMillis = ms }.get(Calendar.HOUR_OF_DAY)

    fun previousHour(hour: Int): Int = ((hour % 24) + 23) % 24

    fun hourLabel(hour: Int, is24: Boolean): String {
        val h = ((hour % 24) + 24) % 24
        return if (is24) "%02d".format(h) else (if (h % 12 == 0) 12 else h % 12).toString()
    }
}

class HourChimeLabels(val oldHour: String, val newHour: String)

fun hourChimeLabels(newHour: Int, is24: Boolean): HourChimeLabels =
    HourChimeLabels(HourChime.hourLabel(HourChime.previousHour(newHour), is24), HourChime.hourLabel(newHour, is24))
