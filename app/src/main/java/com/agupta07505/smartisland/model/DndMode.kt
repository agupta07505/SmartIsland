/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Do Not Disturb / mode idle widget: the pill's left slot shows the active mode's glyph, the island its glyph and
 * name. Pure rules; reading the system is util/DndModeReader.kt.
 */
enum class ModeKind { Dnd, TotalSilence, AlarmsOnly, Bedtime, Driving, Theater, Gaming, Work, Schedule, Custom }

/**
 * Active mode: [name] is the system's own (else [DndModes.defaultName]); [iconRes] / [iconPackage] point at its icon,
 * else the widget falls back to a glyph per [kind].
 */
data class ModeStatus(
    val active: Boolean,
    val kind: ModeKind = ModeKind.Dnd,
    val name: String = "",
    val fromRule: Boolean = false,
    /** Plain interruption filter in effect ([DndModes.FILTER_*]); 0 otherwise. */
    val filter: Int = 0,
    val iconRes: Int = 0,
    val iconPackage: String = ""
) {
    companion object {
        val Off = ModeStatus(active = false)
    }
}

object DndModes {
    /** Platform values of NotificationManager.INTERRUPTION_FILTER_*. */
    const val FILTER_UNKNOWN = 0
    const val FILTER_ALL = 1
    const val FILTER_PRIORITY = 2
    const val FILTER_NONE = 3
    const val FILTER_ALARMS = 4

    fun filterActive(filter: Int): Boolean = filter == FILTER_PRIORITY || filter == FILTER_NONE || filter == FILTER_ALARMS

    /** Plain Do Not Disturb as [filter] describes it, for phones that cannot tell which mode is on. */
    fun fromFilter(filter: Int): ModeStatus = when (filter) {
        FILTER_PRIORITY -> ModeStatus(true, ModeKind.Dnd, defaultName(ModeKind.Dnd), filter = filter)
        FILTER_NONE -> ModeStatus(true, ModeKind.TotalSilence, defaultName(ModeKind.TotalSilence), filter = filter)
        FILTER_ALARMS -> ModeStatus(true, ModeKind.AlarmsOnly, defaultName(ModeKind.AlarmsOnly), filter = filter)
        else -> ModeStatus.Off
    }

    fun defaultName(kind: ModeKind): String = when (kind) {
        ModeKind.Dnd -> "Do Not Disturb"
        ModeKind.TotalSilence -> "Total silence"
        ModeKind.AlarmsOnly -> "Alarms only"
        ModeKind.Bedtime -> "Bedtime"
        ModeKind.Driving -> "Driving"
        ModeKind.Theater -> "Theater"
        ModeKind.Gaming -> "Gaming"
        ModeKind.Work -> "Work"
        ModeKind.Schedule -> "Schedule"
        ModeKind.Custom -> "Mode"
    }

    /**
     * Mode kind from its name, for phones without mode types (before Android 14) and maker-specific names ("Sleep",
     * "Drive", "Game mode", ...). Unknown is [ModeKind.Custom].
     */
    fun kindFromName(name: String): ModeKind {
        val n = name.lowercase()
        return when {
            n.contains("bedtime") || n.contains("sleep") || n.contains("night") -> ModeKind.Bedtime
            n.contains("driv") || n.contains("car mode") || n.contains("vehicle") -> ModeKind.Driving
            n.contains("theat") || n.contains("cinema") || n.contains("movie") -> ModeKind.Theater
            n.contains("game") || n.contains("gaming") || n.contains("immers") -> ModeKind.Gaming
            n.contains("work") || n.contains("office") || n.contains("meeting") || n.contains("focus") -> ModeKind.Work
            n.contains("schedul") || n.contains("calendar") || n.contains("event") -> ModeKind.Schedule
            n.contains("disturb") || n.contains("dnd") || n.contains("silent") -> ModeKind.Dnd
            else -> ModeKind.Custom
        }
    }

    /** Most specific mode wins: driving, then bedtime, then the rest; schedule and custom last. */
    fun rank(kind: ModeKind): Int = when (kind) {
        ModeKind.Driving -> 0
        ModeKind.Bedtime -> 1
        ModeKind.Theater -> 2
        ModeKind.Gaming -> 3
        ModeKind.Work -> 4
        ModeKind.Schedule -> 5
        ModeKind.Custom -> 6
        ModeKind.Dnd -> 7
        ModeKind.TotalSilence -> 8
        ModeKind.AlarmsOnly -> 9
    }

    fun pick(active: List<ModeStatus>): ModeStatus? = active.minByOrNull { rank(it.kind) }

    fun withName(kind: ModeKind, name: String?): String = name?.trim().takeUnless { it.isNullOrEmpty() } ?: defaultName(kind)

    fun detail(status: ModeStatus): String = when {
        status.fromRule -> "Mode is on"
        status.filter == FILTER_NONE -> "Nothing can interrupt"
        status.filter == FILTER_ALARMS -> "Only alarms can interrupt"
        else -> "Only priority interruptions"
    }
}
