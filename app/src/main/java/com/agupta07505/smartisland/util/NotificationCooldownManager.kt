/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages intelligent notification rate-limiting and anti-spam cooldown.
 * When an application posts multiple notifications in a rapid burst exceeding
 * the threshold within a 30-second sliding window, it enters a cooldown period.
 *
 * During cooldown, rapid successive alerts are suppressed from vibrating/popping up
 * the island. The latest notification is buffered and released after the cooldown
 * duration expires.
 */
object NotificationCooldownManager {
    const val DETECTION_WINDOW_MS = 30_000L

    // packageName -> deque of recent notification timestamps (elapsedRealtime)
    private val recentTimestamps = ConcurrentHashMap<String, ArrayDeque<Long>>()

    // packageName -> cooldown expiration timestamp (elapsedRealtime)
    private val cooldownExpirations = ConcurrentHashMap<String, Long>()

    // packageName -> latest buffered notification during cooldown
    private val bufferedNotifications = ConcurrentHashMap<String, StatusBarNotification>()

    /**
     * Checks if this notification should be throttled / held back.
     *
     * @return true if the notification is throttled (should NOT pop/sound in the island),
     *         false if it should be displayed normally.
     */
    fun shouldThrottle(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (!settings.enableNotificationCooldown) return false
        if (isExemptMode(mode)) return false

        val pkg = sbn.packageName
        if (pkg in settings.notificationCooldownExcludedPackages) return false

        val cooldownExpiresAt = cooldownExpirations[pkg] ?: 0L
        if (currentTimeMs < cooldownExpiresAt) {
            // Actively in cooldown — buffer the latest message and throttle
            bufferedNotifications[pkg] = sbn
            return true
        }

        // Clean up expired cooldown state if any
        if (cooldownExpiresAt != 0L && currentTimeMs >= cooldownExpiresAt) {
            cooldownExpirations.remove(pkg)
            recentTimestamps.remove(pkg)
        }

        // Sliding window detection
        val timestamps = recentTimestamps.getOrPut(pkg) { ArrayDeque() }
        synchronized(timestamps) {
            timestamps.addLast(currentTimeMs)
            while (timestamps.isNotEmpty() && (currentTimeMs - timestamps.first()) > DETECTION_WINDOW_MS) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= settings.notificationCooldownThreshold) {
                // Threshold exceeded! Trigger cooldown for subsequent notifications
                val durationMs = settings.notificationCooldownDurationMinutes * 60_000L
                cooldownExpirations[pkg] = currentTimeMs + durationMs
                timestamps.clear()
            }
        }

        return false
    }

    /**
     * Returns true if the given IslandMode should never be rate-limited
     * (e.g. phone calls, timers, music, navigation).
     */
    fun isExemptMode(mode: IslandMode): Boolean {
        return mode == IslandMode.IncomingCall ||
            mode == IslandMode.Timer ||
            mode == IslandMode.Stopwatch ||
            mode == IslandMode.Music ||
            mode == IslandMode.Navigation ||
            mode == IslandMode.Hotspot ||
            mode == IslandMode.Battery ||
            mode == IslandMode.Flashlight ||
            mode == IslandMode.ScreenRecording ||
            // A ringing alarm and a recorder's per-second updates must never be throttled away.
            mode == IslandMode.Alarm ||
            mode == IslandMode.VoiceRecording ||
            mode == IslandMode.PrivacyIndicator
    }

    /**
     * Retrieves and clears the latest buffered notification for a package when its cooldown completes.
     */
    fun pollBufferedNotification(packageName: String): StatusBarNotification? {
        return bufferedNotifications.remove(packageName)
    }

    /**
     * Checks whether a package is currently cooling down.
     */
    fun isPackageInCooldown(
        packageName: String,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        val expiresAt = cooldownExpirations[packageName] ?: return false
        return currentTimeMs < expiresAt
    }

    /**
     * Returns remaining cooldown seconds for a package, or 0 if not cooling down.
     */
    fun getRemainingCooldownSeconds(
        packageName: String,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Int {
        val expiresAt = cooldownExpirations[packageName] ?: return 0
        val remainingMs = expiresAt - currentTimeMs
        return if (remainingMs > 0) (remainingMs / 1000L).toInt() else 0
    }

    /**
     * Clears all cooldown states, timestamps, and buffered notifications.
     */
    fun clear() {
        recentTimestamps.clear()
        cooldownExpirations.clear()
        bufferedNotifications.clear()
    }
}
