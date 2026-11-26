/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Ringer modes are 1 setting at three levels (ring loudest, silent quietest), airplane mode is 1 at 2, on above
 * off. A morph knows which way it went so the glyph and texts can move that way. Levels of different settings are
 * never compared
 */
object RingerMorph {
    fun level(category: String?): Int? = when (category) {
        DeviceStates.RINGER_RING -> 2
        DeviceStates.RINGER_VIBRATE -> 1
        DeviceStates.RINGER_SILENT -> 0
        else -> null
    }

    fun airplaneLevel(category: String?): Int? = when (category) {
        DeviceStates.AIRPLANE_ON -> 1
        DeviceStates.AIRPLANE_OFF -> 0
        else -> null
    }

    fun direction(from: String?, to: String?): Int {
        val ringerFrom = level(from)
        val ringerTo = level(to)
        if (ringerFrom != null && ringerTo != null) return (ringerTo - ringerFrom).coerceIn(-1, 1)
        val airFrom = airplaneLevel(from)
        val airTo = airplaneLevel(to)
        if (airFrom != null && airTo != null) return (airTo - airFrom).coerceIn(-1, 1)
        return 0
    }
}
