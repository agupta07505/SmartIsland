/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.os.SystemClock

/** Runs an activity PendingIntent without leaving the foreground app when possible. */
object SilentActionGuard {
    const val WINDOW_MS = 2500L

    @Volatile private var armedPackage: String? = null
    @Volatile private var expiresAt = 0L

    fun arm(packageName: String, now: Long = SystemClock.elapsedRealtime()) {
        armedPackage = packageName
        expiresAt = now + WINDOW_MS
    }

    fun disarm() {
        armedPackage = null
        expiresAt = 0L
    }

    /** True exactly once when [openedPackage] is the armed one and the window has not expired. */
    fun consume(openedPackage: String, now: Long = SystemClock.elapsedRealtime()): Boolean {
        val armed = armedPackage ?: return false
        if (now > expiresAt) {
            disarm()
            return false
        }
        if (armed != openedPackage) return false
        disarm()
        return true
    }
}
