/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar

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
    val pinnedFirst: String = ""
) {
    val needsClock: Boolean get() = workEnabled || nightEnabled
}

object IdleSchedule {
    enum class Context { Day, Work, Night, Charging }

    const val DEFAULT_WORK_START = 9
    const val DEFAULT_WORK_END = 17
    const val DEFAULT_NIGHT_START = 22
    const val DEFAULT_NIGHT_END = 6

    fun normalizeHour(value: Int): Int = value.coerceIn(0, 23)

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

    fun isWeekday(dayOfWeek: Int): Boolean = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

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
        IdleWidgets.parse(stored).toSet()

    fun hiddenKinds(prefs: IdleSchedulePrefs, context: Context): Set<IdleWidgetKind> =
        parseHidden(hiddenFor(prefs, context))

    fun allows(prefs: IdleSchedulePrefs, context: Context, kind: IdleWidgetKind): Boolean =
        kind !in hiddenKinds(prefs, context)

    fun filter(kinds: List<IdleWidgetKind>, prefs: IdleSchedulePrefs, context: Context): List<IdleWidgetKind> {
        val hidden = hiddenKinds(prefs, context)
        return if (hidden.isEmpty()) kinds else kinds.filter { it !in hidden }
    }

    fun ordered(kinds: List<IdleWidgetKind>, pinnedFirst: String): List<IdleWidgetKind> {
        val pin = IdleWidgetKind.fromId(pinnedFirst) ?: return kinds
        return if (pin in kinds && kinds.first() != pin) listOf(pin) + (kinds - pin) else kinds
    }

    fun normalizePinned(id: String): String = IdleWidgetKind.fromId(id.trim())?.id ?: ""

    fun togglePinned(current: String, kind: IdleWidgetKind): String =
        if (normalizePinned(current) == kind.id) "" else kind.id

    fun moveToFront(stored: String, kind: IdleWidgetKind): String {
        val list = IdleWidgets.parse(stored)
        return if (kind in list) IdleWidgets.serialize(listOf(kind) + (list - kind)) else stored
    }

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
        IdleWidgets.serialize(IdleWidgets.parse(stored))

    fun toggleHidden(stored: String, kind: IdleWidgetKind): String {
        val hidden = IdleWidgets.parse(stored)
        val updated = if (kind in hidden) hidden - kind else hidden + kind
        return IdleWidgets.serialize(updated)
    }
}
