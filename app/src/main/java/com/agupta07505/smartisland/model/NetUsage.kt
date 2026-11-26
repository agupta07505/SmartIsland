/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale

enum class NetConnection { None, Wifi, Mobile, Other }

data class DailyUsage(val dayKey: Int = 0, val wifiBytes: Long = 0L, val mobileBytes: Long = 0L)

/**
 * Rules for the net speed island's daily counter (bucket per connection, Wi-Fi / mobile split, midnight roll-over, size
 * text). Sampling: util/IdleWidgetData.kt, storage: util/NetUsageStore.kt
 */
object NetUsage {
    fun dayKey(year: Int, month0: Int, day: Int): Int = year * 10_000 + (month0 + 1) * 100 + day

    fun rolled(usage: DailyUsage, todayKey: Int): DailyUsage =
            if (usage.dayKey == todayKey) usage else DailyUsage(dayKey = todayKey)

    fun split(totalDelta: Long, mobileDelta: Long): Pair<Long, Long> {
        val total = totalDelta.coerceAtLeast(0L)
        val mobile = mobileDelta.coerceIn(0L, total)
        return (total - mobile) to mobile
    }

    fun add(usage: DailyUsage, todayKey: Int, wifiDelta: Long, mobileDelta: Long): DailyUsage {
        val day = rolled(usage, todayKey)
        return day.copy(
            wifiBytes = day.wifiBytes + wifiDelta.coerceAtLeast(0L),
            mobileBytes = day.mobileBytes + mobileDelta.coerceAtLeast(0L)
        )
    }

    /**
     * Today's bytes on the current connection: mobile counter on mobile data, Wi-Fi counter otherwise, none without a
     * connection
     */
    fun bytesFor(usage: DailyUsage, connection: NetConnection): Long = when (connection) {
        NetConnection.Mobile -> usage.mobileBytes
        NetConnection.Wifi, NetConnection.Other -> usage.wifiBytes
        NetConnection.None -> 0L
    }

    fun sizeText(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L).toDouble()
        return when {
            b < 1_000_000.0 -> "${Math.round(b / 1_000.0)} KB"
            b < 1_000_000_000.0 -> "${Math.round(b / 1_000_000.0)} MB"
            b < 10_000_000_000.0 -> String.format(Locale.US, "%.1f GB", b / 1_000_000_000.0)
            else -> "${Math.round(b / 1_000_000_000.0)} GB"
        }
    }
}

/**
 * When the idle net speed widget takes a turn: it shows only the busier direction and sits out unless that is above the
 * threshold
 */
object NetSpeedRules {
    const val OFF = 0
    const val KBPS_50 = 50
    const val KBPS_200 = 200
    const val KBPS_1000 = 1000
    val CHOICES = listOf(OFF, KBPS_50, KBPS_200, KBPS_1000)
    const val DEFAULT_KBPS = KBPS_50

    const val CONN_ANY = "ANY"
    const val CONN_WIFI = "WIFI"
    const val CONN_MOBILE = "MOBILE"
    val CONNECTION_CHOICES = listOf(CONN_ANY, CONN_WIFI, CONN_MOBILE)

    fun normalizeConnection(mode: String): String = if (mode in CONNECTION_CHOICES) mode else CONN_ANY

    fun connectionAllows(mode: String, connection: NetConnection): Boolean = when (normalizeConnection(mode)) {
        CONN_WIFI -> connection == NetConnection.Wifi
        CONN_MOBILE -> connection == NetConnection.Mobile
        else -> true
    }

    /**
     * Once shown, the widget stays until the speed falls below this share of the threshold, so it does not flicker on
     * the edge
     */
    const val RELEASE_PERCENT = 80

    /** Anything that is not a choice (an old or edited backup) becomes the default. */
    fun normalize(kbps: Int): Int = if (kbps in CHOICES) kbps else DEFAULT_KBPS

    fun thresholdBytes(kbps: Int): Long = normalize(kbps) * 1000L

    fun busier(downBytes: Long, upBytes: Long): Long = maxOf(downBytes, upBytes, 0L)

    /** Upload is the larger direction; a tie counts as download. */
    fun isUpload(downBytes: Long, upBytes: Long): Boolean = upBytes > downBytes

    /**
     * Whether the widget takes a turn. [wasShown] is the last answer (hysteresis): on at the threshold, off again only
     * below [RELEASE_PERCENT] of it. With [OFF] always shown
     */
    fun shouldShow(kbps: Int, downBytes: Long, upBytes: Long, wasShown: Boolean): Boolean {
        val threshold = thresholdBytes(kbps)
        if (threshold <= 0L) return true
        val speed = busier(downBytes, upBytes)
        return if (wasShown) speed * 100L >= threshold * RELEASE_PERCENT else speed >= threshold
    }
}
