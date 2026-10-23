/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

data class NextEvent(val title: String, val beginMs: Long, val endMs: Long)

/**
 * Next-event rules for the date widget's semi-expanded island (next 24 h only). Reading the calendar is in
 * util/CalendarReader.kt.
 */
object CalendarRules {
    const val LOOK_AHEAD_MS = 24L * 60L * 60L * 1000L

    const val ISLAND_EVENT_HEIGHT_DP = 96f

    fun islandShows(beginMs: Long, nowMs: Long): Boolean = beginMs > nowMs && beginMs - nowMs <= LOOK_AHEAD_MS

    fun pickNext(events: List<NextEvent>, nowMs: Long): NextEvent? = events.filter { it.beginMs > nowMs }.minByOrNull { it.beginMs }

    /** Title cut to [max] chars with an ellipsis; an empty title becomes "Event". */
    fun shortTitle(title: String, max: Int): String {
        val clean = title.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return "Event"
        return if (clean.length <= max) clean else clean.take((max - 1).coerceAtLeast(1)).trimEnd() + "\u2026"
    }
}
