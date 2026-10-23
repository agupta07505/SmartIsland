/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.PowerManager
import android.os.SystemClock
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TransferKind
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pure state machine behind the network-speed fallback: it turns a stream of "bytes per second" samples into IDLE /
 * DOWNLOAD / UPLOAD with hysteresis, so a short burst never flashes a card and a brief stall never drops one.
 */
class SpeedTracker(
    private val startBytesPerSec: Long = START_BPS,
    private val stopBytesPerSec: Long = STOP_BPS,
    private val startAfterMs: Long = START_AFTER_MS,
    private val stopAfterMs: Long = STOP_AFTER_MS
) {
    enum class Phase { IDLE, DOWNLOAD, UPLOAD }

    var phase: Phase = Phase.IDLE
        private set
    private var candidate: Phase = Phase.IDLE
    private var candidateSince = 0L
    private var lastBusy = 0L

    fun reset() {
        phase = Phase.IDLE
        candidate = Phase.IDLE
        candidateSince = 0L
        lastBusy = 0L
    }

    fun onSample(nowMs: Long, rxBytesPerSec: Long, txBytesPerSec: Long): Phase {
        val dir = direction(rxBytesPerSec, txBytesPerSec)
        if (dir != candidate) {
            candidate = dir
            candidateSince = nowMs
        }

        if (phase != Phase.IDLE) {
            val alive = if (phase == Phase.DOWNLOAD) rxBytesPerSec else txBytesPerSec
            if (alive >= stopBytesPerSec) lastBusy = nowMs
            if (nowMs - lastBusy >= stopAfterMs) {
                phase = Phase.IDLE
            } else if (candidate != Phase.IDLE && candidate != phase && nowMs - candidateSince >= startAfterMs) {
                // The traffic flipped direction (a download finished and an upload took over).
                phase = candidate
                lastBusy = nowMs
            }
            if (phase != Phase.IDLE) return phase
        }

        if (candidate != Phase.IDLE && nowMs - candidateSince >= startAfterMs) {
            phase = candidate
            lastBusy = nowMs
        }
        return phase
    }

    private fun direction(rx: Long, tx: Long): Phase = when {
        tx >= startBytesPerSec && tx > rx + rx / 2 -> Phase.UPLOAD
        rx >= startBytesPerSec && rx >= tx -> Phase.DOWNLOAD
        else -> Phase.IDLE
    }

    companion object {
        const val START_BPS = 700L * 1024L
        const val STOP_BPS = 150L * 1024L
        const val START_AFTER_MS = 4_000L
        const val STOP_AFTER_MS = 6_000L

        fun formatSpeed(bytesPerSec: Long): String {
            val mb = bytesPerSec / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB/s", mb)
            } else {
                "${(bytesPerSec / 1024L).coerceAtLeast(1L)} KB/s"
            }
        }
    }
}

/**
 * Last-resort download/upload detection for transfers that show no usable notification (the app's notification
 * channel is blocked, or the app shows none).
 */
class TransferSpeedMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val enabled: () -> Boolean
) {
    private val tracker = SpeedTracker()
    private var job: Job? = null
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var postedKey: String? = null
    private var cardStartedAt = 0L
    private var lastPostAt = 0L

    fun start() {
        if (job?.isActive == true) return
        if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED.toLong()) return
        job = scope.launch {
            runSuspendCatchingLogged(TAG, "Transfer speed loop failed") { loop() }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        tracker.reset()
        clearCard()
    }

    private suspend fun loop() {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastAt = SystemClock.elapsedRealtime()
        while (true) {
            delay(SAMPLE_MS)
            val now = SystemClock.elapsedRealtime()
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val elapsed = now - lastAt
            // Counters can restart (interface changes), so a negative step counts as zero.
            val deltaRx = (rx - lastRx).coerceAtLeast(0L)
            val deltaTx = (tx - lastTx).coerceAtLeast(0L)
            lastRx = rx
            lastTx = tx
            lastAt = now
            if (rx < 0L || tx < 0L || elapsed <= 0L) continue

            if (!shouldWatch()) {
                tracker.reset()
                clearCard()
                delay(IDLE_EXTRA_MS)
                continue
            }

            val share = if (isVpnActive()) 2L else 1L
            val rxPerSec = deltaRx * 1000L / elapsed / share
            val txPerSec = deltaTx * 1000L / elapsed / share
            when (val phase = tracker.onSample(now, rxPerSec, txPerSec)) {
                SpeedTracker.Phase.IDLE -> clearCard()
                else -> postCard(phase, if (phase == SpeedTracker.Phase.DOWNLOAD) rxPerSec else txPerSec, now)
            }
        }
    }

    private fun shouldWatch(): Boolean {
        if (!enabled()) return false
        if (powerManager?.isInteractive == false) return false
        if (audioManager?.isMusicActive == true) return false
        val cards = repository.notifications.value
        if (cards.any { it.mode == IslandMode.DownloadUpload && !it.isEstimatedTransfer }) return false
        if (cards.any { it.mode == IslandMode.Hotspot }) return false
        return true
    }

    private fun isVpnActive(): Boolean {
        val cm = connectivityManager ?: return false
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull() ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun postCard(phase: SpeedTracker.Phase, bytesPerSec: Long, now: Long) {
        val upload = phase == SpeedTracker.Phase.UPLOAD
        val key = if (upload) KEY_UPLOAD else KEY_DOWNLOAD
        val other = if (upload) KEY_DOWNLOAD else KEY_UPLOAD
        if (postedKey == other) {
            repository.removeNotification(other)
            postedKey = null
        }
        if (postedKey == key && now - lastPostAt < POST_EVERY_MS) return
        if (postedKey != key) cardStartedAt = System.currentTimeMillis()
        postedKey = key
        lastPostAt = now
        repository.postNotification(
            IslandNotification(
                key = key,
                packageName = "com.android.systemui",
                appName = "Network",
                title = if (upload) "Uploading" else "Downloading",
                text = "Network activity",
                timeMillis = cardStartedAt,
                category = "progress",
                mode = IslandMode.DownloadUpload,
                transferKind = if (upload) TransferKind.UPLOAD else TransferKind.DOWNLOAD,
                transferDetail = SpeedTracker.formatSpeed(bytesPerSec),
                isEstimatedTransfer = true
            )
        )
    }

    private fun clearCard() {
        postedKey?.let { repository.removeNotification(it) }
        postedKey = null
    }

    companion object {
        private const val TAG = "TransferSpeedMonitor"
        const val KEY_DOWNLOAD = "system_net_download"
        const val KEY_UPLOAD = "system_net_upload"
        private const val SAMPLE_MS = 1_000L
        private const val IDLE_EXTRA_MS = 2_000L
        private const val POST_EVERY_MS = 2_000L
    }
}
