/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.net.TrafficStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Monitors live network throughput for the active tethering/hotspot interface.
 * Falls back to device traffic deltas when interface-level stats are unavailable.
 */
class HotspotTrafficMonitor {

    data class ThroughputSnapshot(
        val rxBytesPerSec: Long = 0L,
        val txBytesPerSec: Long = 0L
    ) {
        val totalBytesPerSec: Long get() = rxBytesPerSec + txBytesPerSec

        fun formatRx(): String = "↓ " + formatSpeed(rxBytesPerSec)
        fun formatTx(): String = "↑ " + formatSpeed(txBytesPerSec)
        fun formatTotal(): String = formatSpeed(totalBytesPerSec)

        private fun formatSpeed(bytesPerSec: Long): String {
            val b = bytesPerSec.coerceAtLeast(0L)
            return when {
                b >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", b / (1024f * 1024f))
                b >= 1024 -> String.format(Locale.US, "%.0f KB/s", b / 1024f)
                else -> "$b B/s"
            }
        }
    }

    private val _throughput = MutableStateFlow(ThroughputSnapshot())
    val throughput: StateFlow<ThroughputSnapshot> = _throughput.asStateFlow()

    private var pollingJob: Job? = null
    private var lastRxBytes = -1L
    private var lastTxBytes = -1L
    private var lastTimestamp = -1L

    fun start(scope: CoroutineScope) {
        if (pollingJob != null) return
        lastRxBytes = readRxBytes()
        lastTxBytes = readTxBytes()
        lastTimestamp = System.currentTimeMillis()

        pollingJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val currentRx = readRxBytes()
                val currentTx = readTxBytes()

                val timeDeltaSec = ((now - lastTimestamp) / 1000f).coerceAtLeast(0.1f)
                val rxDelta = if (lastRxBytes >= 0 && currentRx >= lastRxBytes) currentRx - lastRxBytes else 0L
                val txDelta = if (lastTxBytes >= 0 && currentTx >= lastTxBytes) currentTx - lastTxBytes else 0L

                val rxSpeed = (rxDelta / timeDeltaSec).toLong()
                val txSpeed = (txDelta / timeDeltaSec).toLong()

                lastRxBytes = currentRx
                lastTxBytes = currentTx
                lastTimestamp = now

                _throughput.value = ThroughputSnapshot(rxSpeed, txSpeed)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _throughput.value = ThroughputSnapshot()
        lastRxBytes = -1L
        lastTxBytes = -1L
    }

    private fun readRxBytes(): Long {
        for (iface in KNOWN_TETHER_INTERFACES) {
            val bytes = getInterfaceRxBytes(iface)
            if (bytes > 0) return bytes
        }
        val total = TrafficStats.getTotalRxBytes()
        return if (total != TrafficStats.UNSUPPORTED.toLong()) total else 0L
    }

    private fun readTxBytes(): Long {
        for (iface in KNOWN_TETHER_INTERFACES) {
            val bytes = getInterfaceTxBytes(iface)
            if (bytes > 0) return bytes
        }
        val total = TrafficStats.getTotalTxBytes()
        return if (total != TrafficStats.UNSUPPORTED.toLong()) total else 0L
    }

    private fun getInterfaceRxBytes(iface: String): Long {
        return try {
            val method = TrafficStats::class.java.getMethod("getRxBytes", String::class.java)
            (method.invoke(null, iface) as? Long) ?: -1L
        } catch (_: Throwable) {
            -1L
        }
    }

    private fun getInterfaceTxBytes(iface: String): Long {
        return try {
            val method = TrafficStats::class.java.getMethod("getTxBytes", String::class.java)
            (method.invoke(null, iface) as? Long) ?: -1L
        } catch (_: Throwable) {
            -1L
        }
    }

    companion object {
        private val KNOWN_TETHER_INTERFACES = listOf("ap0", "wlan0", "swlan0", "rndis0")
    }
}
