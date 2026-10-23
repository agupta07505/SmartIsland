/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale

/** Kind of network the device is on now; [Other] is ethernet, a VPN with no known carrier, and so on. */
enum class NetConnection { None, Wifi, Mobile, Other }

/**
 * Traffic counted for one local day ([dayKey] is yyyyMMdd). Wi-Fi also holds ethernet and anything that is not mobile
 * data.
 */
data class DailyUsage(val dayKey: Int = 0, val wifiBytes: Long = 0L, val mobileBytes: Long = 0L)

/**
 * Rules for the net speed island's daily counter (bucket per connection, Wi-Fi / mobile split, midnight roll-over,
 * size text). Sampling: util/IdleWidgetData.kt; storage: util/NetUsageStore.kt.
 */
object NetUsage {
    fun dayKey(year: Int, month0: Int, day: Int): Int = year * 10_000 + (month0 + 1) * 100 + day

    /** [usage] if it is today's, else an empty day (the counter restarts at midnight). */
    fun rolled(usage: DailyUsage, todayKey: Int): DailyUsage = if (usage.dayKey == todayKey) usage else DailyUsage(dayKey = todayKey)

    /**
     * Splits one sample ([totalDelta]) into mobile ([mobileDelta], clamped to the total since the counters are read a
     * moment apart) and the rest, Wi-Fi. Negative values (a counter reset) count as nothing.
     */
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
     * Today's bytes on the current connection: mobile counter on mobile data, Wi-Fi counter otherwise; none without a
     * connection.
     */
    fun bytesFor(usage: DailyUsage, connection: NetConnection): Long = when (connection) {
        NetConnection.Mobile -> usage.mobileBytes
        NetConnection.Wifi, NetConnection.Other -> usage.wifiBytes
        NetConnection.None -> 0L
    }

    /** "850 KB", "340 MB", "1.2 GB", "12 GB" (decimal units, like the speed text). */
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
