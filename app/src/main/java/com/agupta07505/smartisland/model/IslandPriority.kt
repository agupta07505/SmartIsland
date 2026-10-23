/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Which card owns the primary cutout and which goes to the secondary round cutout. */
object IslandPriority {
    fun rank(mode: IslandMode): Int = when (mode) {
        IslandMode.IncomingCall -> 0
        IslandMode.Navigation -> 10
        IslandMode.LiveActivity,
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording,
        IslandMode.Hotspot,
        IslandMode.Flashlight -> 20
        IslandMode.Music -> 30
        IslandMode.DownloadUpload -> 40
        IslandMode.Notification,
        IslandMode.Battery,
        IslandMode.Bluetooth,
        IslandMode.Empty -> 50
    }

    fun rank(n: IslandNotification): Int =
        rank(n.mode) + if (n.mode == IslandMode.Music && !n.mediaIsPlaying) 1 else 0

    /**
     * Cards that fall back to the idle cutout after [IslandTimings.STATIC_IDLE_TIMEOUT_MS] once nothing is actively
     * running: notifications, paused music, stopwatch / timer / recorders and transfers.
     */
    fun canIdleOut(n: IslandNotification): Boolean = when (n.mode) {
        IslandMode.Notification -> true
        IslandMode.Music -> !n.mediaIsPlaying
        IslandMode.Stopwatch,
        IslandMode.Timer,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording -> n.recordingPaused
        IslandMode.DownloadUpload -> n.transferState == TransferState.PAUSED
        else -> false
    }

    /**
     * Short events that pop into the primary cutout on arrival: ordinary notifications, battery and connection cards.
     * Over a live event they wait in the secondary cutout.
     */
    fun popsUp(mode: IslandMode): Boolean = when (mode) {
        IslandMode.Notification, IslandMode.Battery, IslandMode.Bluetooth -> true
        else -> false
    }

    /**
     * [popsUp] for [card] given the cards on the island: no short event takes the primary cutout from a live event;
     * the newcomer waits behind it.
     */
    fun popsUp(card: IslandNotification, cards: List<IslandNotification>): Boolean =
        popsUp(card.mode) && !hasLive(cards)

    /**
     * A live event: call, navigation, live activity, timer, stopwatch, recording, hotspot, flashlight, music or
     * transfer. Ordinary notifications and system cards are not.
     */
    fun isLive(mode: IslandMode): Boolean = mode != IslandMode.Empty && rank(mode) < rank(IslandMode.Notification)

    fun hasLive(cards: List<IslandNotification>): Boolean = cards.any { isLive(it.mode) }

    private fun placeBattery(sorted: List<IslandNotification>): List<IslandNotification> {
        val battery = sorted.filter { it.mode == IslandMode.Battery }
        if (battery.isEmpty()) return sorted
        val rest = sorted.filter { it.mode != IslandMode.Battery }
        if (rest.isEmpty()) return sorted
        return listOf(rest.first()) + battery + rest.drop(1)
    }

    /**
     * Cards in display order: equal ranks keep arrival order, except notifications, which are newest first. The
     * [popupKey] card goes first until its popup is over.
     */
    fun order(list: List<IslandNotification>, popupKey: String? = null): List<IslandNotification> {
        if (list.size < 2) return list
        val sorted = placeBattery(newestNotificationsFirst(list.sortedBy { rank(it) }))  // both sorts are stable
        if (popupKey == null) return sorted
        val popup = sorted.firstOrNull { it.key == popupKey } ?: return sorted
        if (!popsUp(popup, list)) return sorted
        return listOf(popup) + sorted.filter { it !== popup }
    }

    private fun newestNotificationsFirst(ordered: List<IslandNotification>): List<IslandNotification> {
        val newest = ordered.filter { it.mode == IslandMode.Notification }.sortedByDescending { it.timeMillis }
        if (newest.size < 2) return ordered
        val it = newest.iterator()
        return ordered.map { card -> if (card.mode == IslandMode.Notification) it.next() else card }
    }
}
