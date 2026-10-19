/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification

/**
 * Tells a ringing alarm (Rule_Book §4.10) from every other alarm-ish notification. Clock apps post the
 * ringing alarm with `CATEGORY_ALARM`, a full-screen intent and a Snooze action; the "Upcoming alarm",
 * "Snoozed until 7:10" and "Missed alarm" notifications share the category but none of the rest.
 * Timers are matched before this (a finished timer is a Timer, not an Alarm).
 */
object RingingAlarm {

    private val NOT_RINGING_WORDS = listOf(
        "upcoming", "snoozed", "snoozing", "missed alarm", "next alarm", "scheduled", "will ring"
    )

    /** The alarm notification says it is not ringing ("Upcoming alarm", "Snoozed until 7:10", "Missed alarm"). */
    fun saysNotRinging(title: String, text: String): Boolean {
        val shown = "$title $text".lowercase()
        return NOT_RINGING_WORDS.any { shown.contains(it) }
    }

    /** Pure rule; [shownText] is the notification's title and text, [actionLabels] its action titles. */
    fun isRinging(
        category: String?,
        hasFullScreenIntent: Boolean,
        actionLabels: List<String>,
        shownText: String,
        isClockApp: Boolean
    ): Boolean {
        if (saysNotRinging(shownText, "")) return false
        val isAlarmCategory = category.equals(Notification.CATEGORY_ALARM, ignoreCase = true)
        if (!isAlarmCategory && !isClockApp) return false
        val labels = actionLabels.map { it.lowercase() }
        val hasSnooze = labels.any { label -> IslandControl.AlarmSnooze.keywords.any { NotificationActionMatcher.hasWord(label, it) } }
        val hasStop = labels.any { label ->
            IslandControl.AlarmStop.keywords.any { NotificationActionMatcher.hasWord(label, it) } &&
                IslandControl.AlarmStop.excludes.none { NotificationActionMatcher.hasWord(label, it) }
        }
        return when {
            isAlarmCategory && hasFullScreenIntent -> true
            isAlarmCategory && hasSnooze -> true
            // A clock app that forgot the category: a Snooze and a Stop together only appear while it rings.
            isClockApp && hasSnooze && hasStop -> true
            else -> false
        }
    }

    fun isRinging(notification: Notification, packageName: String): Boolean = runCatching {
        val pkg = packageName.lowercase()
        isRinging(
            category = notification.category,
            hasFullScreenIntent = notification.fullScreenIntent != null,
            actionLabels = TimerStopwatchParser.extractActionLabels(notification),
            shownText = TimerStopwatchParser.extractFullText(notification),
            isClockApp = pkg.contains("clock") || pkg.contains("alarm")
        )
    }.getOrDefault(false)
}
