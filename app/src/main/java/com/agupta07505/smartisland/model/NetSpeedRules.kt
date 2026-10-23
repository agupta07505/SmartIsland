/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * When the idle net speed widget takes a turn: it shows only the busier direction and sits out unless that is above
 * the threshold.
 */
object NetSpeedRules {
    /** Threshold choices in KB/s (1 KB = 1000 bytes, like the speed text). 0 = Off: always in the rotation. */
    const val OFF = 0
    const val KBPS_50 = 50
    const val KBPS_200 = 200
    const val KBPS_1000 = 1000
    val CHOICES = listOf(OFF, KBPS_50, KBPS_200, KBPS_1000)
    const val DEFAULT_KBPS = KBPS_50

    /** Connection the widget shows on: any, Wi-Fi only, or mobile data only (watch the speed only where it costs data). */
    const val CONN_ANY = "ANY"
    const val CONN_WIFI = "WIFI"
    const val CONN_MOBILE = "MOBILE"
    val CONNECTION_CHOICES = listOf(CONN_ANY, CONN_WIFI, CONN_MOBILE)

    fun normalizeConnection(mode: String): String = if (mode in CONNECTION_CHOICES) mode else CONN_ANY

    /** Whether the widget may show on [connection] under [mode]; no connection never matches Wi-Fi or mobile. */
    fun connectionAllows(mode: String, connection: NetConnection): Boolean = when (normalizeConnection(mode)) {
        CONN_WIFI -> connection == NetConnection.Wifi
        CONN_MOBILE -> connection == NetConnection.Mobile
        else -> true
    }

    /**
     * Once shown, the widget stays until the speed falls below this share of the threshold, so it does not flicker on
     * the edge.
     */
    const val RELEASE_PERCENT = 80

    /** Anything that is not a choice (an old or edited backup) becomes the default. */
    fun normalize(kbps: Int): Int = if (kbps in CHOICES) kbps else DEFAULT_KBPS

    fun thresholdBytes(kbps: Int): Long = normalize(kbps) * 1000L

    fun busier(downBytes: Long, upBytes: Long): Long = maxOf(downBytes, upBytes, 0L)

    /** True when upload is the larger direction; a tie counts as download. */
    fun isUpload(downBytes: Long, upBytes: Long): Boolean = upBytes > downBytes

    /**
     * Whether the widget takes a turn. [wasShown] is the last answer (hysteresis): on at the threshold, off again
     * only below [RELEASE_PERCENT] of it. With [OFF] always shown.
     */
    fun shouldShow(kbps: Int, downBytes: Long, upBytes: Long, wasShown: Boolean): Boolean {
        val threshold = thresholdBytes(kbps)
        if (threshold <= 0L) return true
        val speed = busier(downBytes, upBytes)
        return if (wasShown) speed * 100L >= threshold * RELEASE_PERCENT else speed >= threshold
    }
}
