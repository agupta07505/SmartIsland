/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/** Stuck cards after the listener was unbound. */
object StaleCardSweep {
    /** The modes whose notification stays in the system shade while the card is on the island. */
    val SHADE_RESIDENT_MODES: Set<IslandMode> = setOf(
        IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch,
        IslandMode.ScreenRecording, IslandMode.AudioRecording
    )

    private val ISLAND_OWN_KEYS: Set<String> = setOf(InAppStopwatch.KEY, InAppTimer.KEY)

    fun keysToDrop(cards: List<IslandNotification>, activeKeys: Set<String>): List<String> =
        cards.filter {
            it.mode in SHADE_RESIDENT_MODES && it.key.isNotEmpty() && it.key !in activeKeys && it.key !in ISLAND_OWN_KEYS
        }.map { it.key }
}
