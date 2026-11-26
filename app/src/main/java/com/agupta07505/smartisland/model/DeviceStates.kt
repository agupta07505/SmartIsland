/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Cards for the phone's own states: airplane mode, ringer mode, Do Not Disturb and modes. They ride on the Bluetooth
 * card like Wi-Fi and USB do, told apart by their category. Pure rules, reading the system is
 * util/DeviceStateEventMonitor.kt
 */
object DeviceStates {
    data class Card(val event: IslandEvent, val category: String, val title: String, val text: String)

    const val AIRPLANE_ON = "airplane_on"
    const val AIRPLANE_OFF = "airplane_off"
    const val RINGER_RING = "ringer_ring"
    const val RINGER_VIBRATE = "ringer_vibrate"
    const val RINGER_SILENT = "ringer_silent"

    private const val FILTER_PREFIX = "filter_"
    private const val MODE_PREFIX = "mode_"
    private const val OFF_SUFFIX = "_off"
    private val OFF_EVENTS = setOf(IslandEvent.AIRPLANE_MODE, IslandEvent.DND, IslandEvent.MODE)

    const val RINGER_MODE_SILENT = 0
    const val RINGER_MODE_VIBRATE = 1
    const val RINGER_MODE_NORMAL = 2

    fun eventOf(category: String?): IslandEvent? = when {
        category == null -> null
        category == AIRPLANE_ON || category == AIRPLANE_OFF -> IslandEvent.AIRPLANE_MODE
        category == RINGER_RING -> IslandEvent.RING
        category == RINGER_VIBRATE -> IslandEvent.VIBRATE
        category == RINGER_SILENT -> IslandEvent.SILENT
        category.startsWith(FILTER_PREFIX) -> IslandEvent.DND
        category.startsWith(MODE_PREFIX) -> IslandEvent.MODE
        else -> null
    }

    fun isDeviceState(category: String?): Boolean = eventOf(category) != null

    fun isOff(category: String?): Boolean =
        category != null && category.endsWith(OFF_SUFFIX) && eventOf(category) in OFF_EVENTS

    fun kindOf(category: String?): ModeKind? {
        val event = eventOf(category)
        if (category == null || (event != IslandEvent.DND && event != IslandEvent.MODE)) return null
        val name = category.removePrefix(FILTER_PREFIX).removePrefix(MODE_PREFIX).removeSuffix(OFF_SUFFIX)
        return ModeKind.entries.firstOrNull { it.name.lowercase() == name }
    }

    fun airplane(on: Boolean): Card = Card(
        IslandEvent.AIRPLANE_MODE, if (on) AIRPLANE_ON else AIRPLANE_OFF, "Airplane mode", if (on) "On" else "Off"
    )

    fun ringer(ringerMode: Int): Card? = when (ringerMode) {
        RINGER_MODE_NORMAL -> Card(IslandEvent.RING, RINGER_RING, "Ring", "Sound on")
        RINGER_MODE_VIBRATE -> Card(IslandEvent.VIBRATE, RINGER_VIBRATE, "Vibrate", "Vibration only")
        RINGER_MODE_SILENT -> Card(IslandEvent.SILENT, RINGER_SILENT, "Silent", "Sound and vibration off")
        else -> null
    }

    fun change(previous: ModeStatus, current: ModeStatus): Card? = when {
        current.active -> if (sameMode(previous, current)) null else modeCard(current, on = true)
        previous.active -> modeCard(previous, on = false)
        else -> null
    }

    private fun sameMode(a: ModeStatus, b: ModeStatus): Boolean =
        a.active == b.active && a.kind == b.kind && a.name == b.name && a.fromRule == b.fromRule

    private fun modeCard(status: ModeStatus, on: Boolean): Card {
        val prefix = if (status.fromRule) MODE_PREFIX else FILTER_PREFIX
        val category = prefix + status.kind.name.lowercase() + if (on) "" else OFF_SUFFIX
        val name = status.name.ifBlank { DndModes.defaultName(status.kind) }
        return Card(
            if (status.fromRule) IslandEvent.MODE else IslandEvent.DND,
            category,
            name,
            if (on) DndModes.detail(status) else "Off"
        )
    }
}
