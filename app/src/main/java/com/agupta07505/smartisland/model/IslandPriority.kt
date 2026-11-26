/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

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
        IslandMode.CallEnded,
        IslandMode.Empty -> 50
    }

    fun rank(n: IslandNotification): Int =
        rank(n.mode) + if (n.mode == IslandMode.Music && !n.mediaIsPlaying) 1 else 0

    private val URGENT_PULSE_EVENTS = setOf(
        IslandEvent.LOW_BATTERY, IslandEvent.BATTERY_SAVER, IslandEvent.WIFI_DISCONNECTED,
        IslandEvent.VPN_DISCONNECTED, IslandEvent.TIMER_FINISHED,
        // Turning airplane mode on or off can trigger a Wi-Fi event in the same burst.
        // Airplane mode stays in the urgent queue, where ties keep arrival order.
        IslandEvent.AIRPLANE_MODE
    )

    private val RESPONSIVE_PULSE_EVENTS = setOf(
        IslandEvent.PLUGGED_IN, IslandEvent.PLUGGED_OUT, IslandEvent.FULLY_CHARGED,
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED,
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED,
        IslandEvent.CALL_ENDED
    )

    fun pulseRank(n: IslandNotification): Int {
        val event = IslandEvent.forNotification(n)
        return when {
            n.mode == IslandMode.IncomingCall -> 0
            n.mode == IslandMode.Navigation -> 10
            event != null && event in URGENT_PULSE_EVENTS -> 15
            n.mode == IslandMode.Music -> 30 + if (n.mediaIsPlaying) 0 else 1
            n.mode == IslandMode.DownloadUpload -> 40
            isLive(n.mode) -> 20
            event != null && event in RESPONSIVE_PULSE_EVENTS -> 45
            n.mode == IslandMode.Notification -> 50
            else -> 60
        }
    }

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
     * Cards a tap on the camera puts away until the next tap, whatever their state does meanwhile: music, timer,
     * stopwatch, recorders, transfers. A finished timer and a failed or done transfer come back. Calls, navigation and
     * notifications are not in this set
     */
    fun staysAwayAfterTap(n: IslandNotification): Boolean = when (n.mode) {
        IslandMode.Music,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording -> true
        IslandMode.Timer -> !n.timerFinished && n.timerRemainingSeconds != 0L
        IslandMode.DownloadUpload -> n.transferState != TransferState.FAILED
            && n.transferState != TransferState.COMPLETE
        else -> false
    }

    fun isLive(mode: IslandMode): Boolean = mode != IslandMode.Empty && rank(mode) < rank(IslandMode.Notification)

    fun hasLive(cards: List<IslandNotification>): Boolean = cards.any { isLive(it.mode) }

    fun revealsStoredOnOpen(cards: List<IslandNotification>, hasStoredAway: Boolean): Boolean =
        hasStoredAway && hasLive(cards)

    private fun placeBattery(sorted: List<IslandNotification>): List<IslandNotification> {
        val battery = sorted.filter { it.mode == IslandMode.Battery }
        if (battery.isEmpty()) return sorted
        val rest = sorted.filter { it.mode != IslandMode.Battery }
        if (rest.isEmpty()) return sorted
        return listOf(rest.first()) + battery + rest.drop(1)
    }

    fun order(list: List<IslandNotification>): List<IslandNotification> {
        if (list.size < 2) return list
        return placeBattery(newestNotificationsFirst(list.sortedBy { rank(it) }))  // both sorts are stable
    }

    private fun newestNotificationsFirst(ordered: List<IslandNotification>): List<IslandNotification> {
        val newest = ordered.filter { it.mode == IslandMode.Notification }.sortedByDescending { it.timeMillis }
        if (newest.size < 2) return ordered
        val it = newest.iterator()
        return ordered.map { card -> if (card.mode == IslandMode.Notification) it.next() else card }
    }
}
