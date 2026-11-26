/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import java.util.concurrent.ConcurrentHashMap

/**
 * A live card (download, route, delivery) that ends without a notification of its own leaves the app's older ones
 * behind. The app or system often re-posts one a moment later (group summary refresh, "re-notify all") and that must
 * not open the island, an old card would just take the place of the one that ended. A new notification of the same
 * app is newer than the ended card, so it still opens
 */
object GoneCardGuard {
    /** How long after a live card ended its app's older notifications stay quiet. */
    const val WINDOW_MS = 4_000L

    class Gone(val packageName: String, val timeMillis: Long, val goneAtMs: Long)

    private fun endsQuietly(card: IslandNotification): Boolean = when (card.mode) {
        IslandMode.DownloadUpload, IslandMode.Navigation, IslandMode.LiveActivity -> !card.isEstimatedTransfer
        else -> false
    }

    /**
     * Live cards in [previous] that are not in [current] any more, whether or not the island was open when they went.
     * A card that only changed its mode under the same key (a download that turned into "Download complete") stays
     */
    fun vanished(
        previous: List<IslandNotification>,
        current: List<IslandNotification>,
        nowMs: Long
    ): List<Gone> {
        if (previous.isEmpty()) return emptyList()
        val currentKeys = current.mapTo(HashSet()) { it.key }
        return previous.filter { endsQuietly(it) && it.key !in currentKeys }
            // a card's time can lie ahead of now (a countdown), what ended can not be newer than its ending
            .map { Gone(it.packageName, minOf(it.timeMillis, nowMs), nowMs) }
    }

    /** [recent] without the entries whose window is over. */
    fun prune(recent: List<Gone>, nowMs: Long): List<Gone> = recent.filter { nowMs - it.goneAtMs <= WINDOW_MS }

    /** True when [arriving] is an older notification of the app a live card has just left (opening is wrong). */
    fun suppressesArrival(recent: List<Gone>, arriving: IslandNotification, nowMs: Long): Boolean {
        if (arriving.mode != IslandMode.Notification) return false
        return recent.any {
            it.packageName == arriving.packageName &&
                nowMs - it.goneAtMs <= WINDOW_MS &&
                arriving.timeMillis in 1 until it.timeMillis
        }
    }
}

object StaleCardSweep {
    /** Modes whose notification stays in the system shade while the card is on the island. */
    val SHADE_RESIDENT_MODES: Set<IslandMode> = setOf(
        IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch,
        IslandMode.ScreenRecording, IslandMode.AudioRecording
    )

    private val ISLAND_OWN_KEYS: Set<String> = setOf(InAppStopwatch.KEY, InAppTimer.KEY)

    fun keysToDrop(cards: List<IslandNotification>, activeKeys: Set<String>): List<String> =
        cards.filter {
            it.mode in SHADE_RESIDENT_MODES && it.key.isNotEmpty() && it.key !in activeKeys
                && it.key !in ISLAND_OWN_KEYS
        }.map { it.key }
}

class StalePostGuard {
    private val newestPostTime = ConcurrentHashMap<String, Long>()
    private val removedPostTime = ConcurrentHashMap<String, Long>()

    fun notePosted(key: String, postTime: Long) {
        newestPostTime.merge(key, postTime) { old, new -> maxOf(old, new) }
        trim()
    }

    fun noteRemoved(key: String, postTime: Long) {
        removedPostTime.merge(key, postTime) { old, new -> maxOf(old, new) }
        trim()
    }

    /** A snapshot posted at [postTime] is older than a newer post, or already removed. */
    fun isStale(key: String, postTime: Long): Boolean {
        val newest = newestPostTime[key]
        if (newest != null && postTime < newest) return true
        val removed = removedPostTime[key]
        return removed != null && postTime <= removed
    }

    fun clear() {
        newestPostTime.clear()
        removedPostTime.clear()
    }

    private fun trim() {
        if (newestPostTime.size > MAX_KEYS || removedPostTime.size > MAX_KEYS) {
            clear()
        }
    }

    private companion object {
        const val MAX_KEYS = 400
    }
}
