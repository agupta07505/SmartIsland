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
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-app rate limit for notification bursts: more than the threshold within a 30-second window starts a cooldown.
 */
object NotificationCooldownManager {
    const val DETECTION_WINDOW_MS = 30_000L

    // packageName -> deque of recent notification timestamps (elapsedRealtime)
    private val recentTimestamps = ConcurrentHashMap<String, ArrayDeque<Long>>()

    // packageName -> cooldown expiration timestamp (elapsedRealtime)
    private val cooldownExpirations = ConcurrentHashMap<String, Long>()

    /**
     * System events that come from the system (broadcasts and callbacks) rather than from an app's notification, so
     * the per-app list cannot cover them. Each one can be switched into the cooldown on its own.
     */
    val SYSTEM_EVENTS: List<IslandEvent> = listOf(
        IslandEvent.PLUGGED_IN,
        IslandEvent.PLUGGED_OUT,
        IslandEvent.FULLY_CHARGED,
        IslandEvent.LOW_BATTERY,
        IslandEvent.BATTERY_SAVER,
        IslandEvent.WIFI,
        IslandEvent.VPN,
        IslandEvent.WIFI_DISCONNECTED,
        IslandEvent.VPN_DISCONNECTED,
        IslandEvent.BLUETOOTH,
        IslandEvent.USB_CONNECTED,
        IslandEvent.USB_DISCONNECTED,
        IslandEvent.HEADPHONES_CONNECTED,
        IslandEvent.HEADPHONES_DISCONNECTED,
        IslandEvent.FLASHLIGHT
    )

    private const val SYSTEM_KEY_PREFIX = "system:"

    // packageName -> latest buffered notification during cooldown
    private val bufferedNotifications = ConcurrentHashMap<String, StatusBarNotification>()

    /** @return true when the notification is throttled and must not pop up or sound in the island. */
    fun shouldThrottle(
        sbn: StatusBarNotification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        currentTimeMs: Long = SystemClock.elapsedRealtime(),
        transferRelated: Boolean = false
    ): Boolean {
        if (!settings.enableNotificationCooldown) return false
        if (isExemptMode(mode)) return false
        // A download manager's or cloud app's own notification (a finished download has no progress bar, so its mode
        // is an ordinary notification) is never held back, and never counts towards the spam threshold.
        if (transferRelated) return false

        val pkg = sbn.packageName
        if (pkg in settings.notificationCooldownExcludedPackages) return false

        val cooldownExpiresAt = cooldownExpirations[pkg] ?: 0L
        if (currentTimeMs < cooldownExpiresAt) {
            bufferedNotifications[pkg] = sbn
            return true
        }

        if (cooldownExpiresAt != 0L && currentTimeMs >= cooldownExpiresAt) {
            cooldownExpirations.remove(pkg)
            recentTimestamps.remove(pkg)
        }

        val timestamps = recentTimestamps.getOrPut(pkg) { ArrayDeque() }
        synchronized(timestamps) {
            timestamps.addLast(currentTimeMs)
            while (timestamps.isNotEmpty() && (currentTimeMs - timestamps.first()) > DETECTION_WINDOW_MS) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= settings.notificationCooldownThreshold) {
                val durationMs = settings.notificationCooldownDurationMinutes * 60_000L
                cooldownExpirations[pkg] = currentTimeMs + durationMs
                timestamps.clear()
            }
        }

        return false
    }

    /**
     * Anti-spam for one system event announcement (a charger plugged in again and again, Wi-Fi that keeps
     * reconnecting).
     */
    fun shouldThrottleSystemEvent(
        eventId: String,
        settings: SmartIslandSettings,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (!settings.enableNotificationCooldown) return false
        if (eventId !in settings.notificationCooldownSystemEvents) return false

        val key = SYSTEM_KEY_PREFIX + eventId
        val cooldownExpiresAt = cooldownExpirations[key] ?: 0L
        if (currentTimeMs < cooldownExpiresAt) return true
        if (cooldownExpiresAt != 0L) {
            cooldownExpirations.remove(key)
            recentTimestamps.remove(key)
        }

        val timestamps = recentTimestamps.getOrPut(key) { ArrayDeque() }
        synchronized(timestamps) {
            timestamps.addLast(currentTimeMs)
            while (timestamps.isNotEmpty() && (currentTimeMs - timestamps.first()) > DETECTION_WINDOW_MS) {
                timestamps.removeFirst()
            }
            if (timestamps.size >= settings.notificationCooldownThreshold) {
                cooldownExpirations[key] = currentTimeMs + settings.notificationCooldownDurationMinutes * 60_000L
                timestamps.clear()
            }
        }
        return false
    }

    /** True for modes that are never rate-limited (calls, timers, music, navigation). */
    fun isExemptMode(mode: IslandMode): Boolean {
        return mode == IslandMode.IncomingCall ||
            mode == IslandMode.Timer ||
            mode == IslandMode.Stopwatch ||
            mode == IslandMode.Music ||
            mode == IslandMode.Navigation ||
            mode == IslandMode.Hotspot ||
            // Progress updates arrive every second or so; counting them as spam would hold back the "download finished" notification.
            mode == IslandMode.DownloadUpload ||
            mode == IslandMode.Battery ||
            mode == IslandMode.Flashlight ||
            mode == IslandMode.ScreenRecording ||
            mode == IslandMode.AudioRecording
    }

    /** Returns and clears the buffered notification once the cooldown has ended. */
    fun pollBufferedNotification(packageName: String): StatusBarNotification? {
        return bufferedNotifications.remove(packageName)
    }

    fun isPackageInCooldown(
        packageName: String,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        val expiresAt = cooldownExpirations[packageName] ?: return false
        return currentTimeMs < expiresAt
    }

    /** Remaining cooldown in seconds, 0 when not cooling down. */
    fun getRemainingCooldownSeconds(
        packageName: String,
        currentTimeMs: Long = SystemClock.elapsedRealtime()
    ): Int {
        val expiresAt = cooldownExpirations[packageName] ?: return 0
        val remainingMs = expiresAt - currentTimeMs
        return if (remainingMs > 0) (remainingMs / 1000L).toInt() else 0
    }

    fun clear() {
        recentTimestamps.clear()
        cooldownExpirations.clear()
        bufferedNotifications.clear()
    }
}
