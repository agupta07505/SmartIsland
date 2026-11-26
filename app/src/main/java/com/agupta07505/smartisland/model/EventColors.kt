/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The colours the user gave single events (`SmartIslandSettings.eventColors`): the
 * on-disk and backup form, and the checks that keep a bad entry from reaching the island. An event without an entry
 * keeps the colour it always had
 */
object EventColors {
    private const val SEPARATOR = '='

    private const val DEFAULT_RED = 0xFFEF4444L

    /**
     * The events that start with a colour of their own instead of their group's. Never stored: it is what an event
     * without an entry of its own draws, and what a reset gives back
     */
    private val DEFAULTS: Map<IslandEvent, Long> = mapOf(
        IslandEvent.USB_DISCONNECTED to DEFAULT_RED,
        IslandEvent.HEADPHONES_DISCONNECTED to DEFAULT_RED,
        IslandEvent.FILE_DELETE to DEFAULT_RED
    )

    fun defaultFor(event: IslandEvent): Long? = DEFAULTS[event]

    fun encode(colors: Map<String, Long>): Set<String> =
        clean(colors).mapTo(LinkedHashSet()) { (id, color) -> "$id$SEPARATOR$color" }

    fun decode(entries: Collection<String>?): Map<String, Long> {
        if (entries == null) return emptyMap()
        val result = LinkedHashMap<String, Long>()
        for (entry in entries) {
            val at = entry.indexOf(SEPARATOR)
            if (at <= 0) continue
            val color = entry.substring(at + 1).trim().toLongOrNull() ?: continue
            result[entry.substring(0, at)] = color
        }
        return clean(result)
    }

    fun clean(colors: Map<String, Long>): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        for (event in IslandEvent.entries) {
            val color = colors[event.id] ?: continue
            if (color != 0L) result[event.id] = color and 0xFFFFFFFFL
        }
        return result
    }
}
