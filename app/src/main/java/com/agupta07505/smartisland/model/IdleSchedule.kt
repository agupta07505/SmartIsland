/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar

/** Schedule choices as one value for the rotation and tick layers. Hidden lists are comma lists of [IdleWidgetKind.id]. */
data class IdleSchedulePrefs(
    val workEnabled: Boolean = false,
    val workHidden: String = "",
    val workStartHour: Int = IdleSchedule.DEFAULT_WORK_START,
    val workEndHour: Int = IdleSchedule.DEFAULT_WORK_END,
    val workWeekdaysOnly: Boolean = true,
    val nightEnabled: Boolean = false,
    val nightHidden: String = "",
    val nightStartHour: Int = IdleSchedule.DEFAULT_NIGHT_START,
    val nightEndHour: Int = IdleSchedule.DEFAULT_NIGHT_END,
    val chargingEnabled: Boolean = false,
    val chargingHidden: String = "",
    /** Id of the widget that always leads the rotation; empty = none. */
    val pinnedFirst: String = ""
) {
    val needsClock: Boolean get() = workEnabled || nightEnabled
}

/**
 * Pure rules for which idle widgets rotate when (work hours, night, charging), the rotation order and the pinned
 * widget that leads it.
 */
object IdleSchedule {
    enum class Context { Day, Work, Night, Charging }

    const val DEFAULT_WORK_START = 9
    const val DEFAULT_WORK_END = 17
    const val DEFAULT_NIGHT_START = 22
    const val DEFAULT_NIGHT_END = 6

    fun normalizeHour(value: Int): Int = value.coerceIn(0, 23)

    /**
     * Whether [hour] is in the window from [start] up to (not including) [end]; may wrap midnight; equal ends are
     * empty.
     */
    fun inWindow(start: Int, end: Int, hour: Int): Boolean {
        val s = normalizeHour(start)
        val e = normalizeHour(end)
        val h = normalizeHour(hour)
        return when {
            s == e -> false
            s < e -> h in s until e
            else -> h >= s || h < e
        }
    }

    /** [dayOfWeek] is a [Calendar] constant (SUNDAY = 1 .. SATURDAY = 7). */
    fun isWeekday(dayOfWeek: Int): Boolean = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

    /** Schedule in force now. Charging beats night, night beats work; a switched-off rule never applies. */
    fun contextFor(prefs: IdleSchedulePrefs, charging: Boolean, hour: Int, dayOfWeek: Int): Context = when {
        prefs.chargingEnabled && charging -> Context.Charging
        prefs.nightEnabled && inWindow(prefs.nightStartHour, prefs.nightEndHour, hour) -> Context.Night
        prefs.workEnabled && inWindow(prefs.workStartHour, prefs.workEndHour, hour) &&
            (!prefs.workWeekdaysOnly || isWeekday(dayOfWeek)) -> Context.Work
        else -> Context.Day
    }

    fun hiddenFor(prefs: IdleSchedulePrefs, context: Context): String = when (context) {
        Context.Charging -> prefs.chargingHidden
        Context.Night -> prefs.nightHidden
        Context.Work -> prefs.workHidden
        Context.Day -> ""
    }

    fun parseHidden(stored: String): Set<IdleWidgetKind> =
        stored.split(',').mapNotNull { IdleWidgetKind.fromId(it.trim()) }.toSet()

    fun hiddenKinds(prefs: IdleSchedulePrefs, context: Context): Set<IdleWidgetKind> = parseHidden(hiddenFor(prefs, context))

    fun allows(prefs: IdleSchedulePrefs, context: Context, kind: IdleWidgetKind): Boolean = kind !in hiddenKinds(prefs, context)

    /** [kinds] without the widgets the schedule in force keeps out, in order. */
    fun filter(kinds: List<IdleWidgetKind>, prefs: IdleSchedulePrefs, context: Context): List<IdleWidgetKind> {
        val hidden = hiddenKinds(prefs, context)
        return if (hidden.isEmpty()) kinds else kinds.filter { it !in hidden }
    }

    /** Rotation order: the pinned widget (if in [kinds]) first, the rest as stored. */
    fun ordered(kinds: List<IdleWidgetKind>, pinnedFirst: String): List<IdleWidgetKind> {
        val pin = IdleWidgetKind.fromId(pinnedFirst) ?: return kinds
        return if (pin in kinds && kinds.first() != pin) listOf(pin) + (kinds - pin) else kinds
    }

    fun normalizePinned(id: String): String = IdleWidgetKind.fromId(id.trim())?.id ?: ""

    fun togglePinned(current: String, kind: IdleWidgetKind): String = if (normalizePinned(current) == kind.id) "" else kind.id

    fun moveToFront(stored: String, kind: IdleWidgetKind): String {
        val list = IdleWidgets.parse(stored)
        return if (kind in list) IdleWidgets.serialize(listOf(kind) + (list - kind)) else stored
    }

    /**
     * Moves [from] to the place [to] holds in the stored list (the others shift); unknown or absent widgets change
     * nothing.
     */
    fun move(stored: String, from: IdleWidgetKind, to: IdleWidgetKind): String {
        val list = IdleWidgets.parse(stored).toMutableList()
        val fromIndex = list.indexOf(from)
        val toIndex = list.indexOf(to)
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return stored
        list.removeAt(fromIndex)
        list.add(toIndex, from)
        return IdleWidgets.serialize(list)
    }

    fun normalizeHidden(stored: String): String =
        stored.split(',').mapNotNull { IdleWidgetKind.fromId(it.trim()) }.distinct().joinToString(",") { it.id }

    fun toggleHidden(stored: String, kind: IdleWidgetKind): String {
        val current = normalizeHidden(stored).split(',').filter { it.isNotEmpty() }
        return if (kind.id in current) current.filter { it != kind.id }.joinToString(",") else (current + kind.id).joinToString(",")
    }
}
