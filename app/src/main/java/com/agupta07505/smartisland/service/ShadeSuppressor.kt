/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** The timing rules for telling a listener cancel from a real removal, and for a reply that is still being sent. */
internal object NotificationRemovalRules {
    /** A removal this soon after the listener hid the notification is its own cancel, not the user's. */
    const val INITIAL_SUPPRESSION_WINDOW_MS = 1500L

    /** A second hide this soon after the first does not cancel again at once. */
    const val RECENT_SUPPRESSION_WINDOW_MS = 300L

    fun isInReplyGrace(now: Long, graceUntilMs: Long?): Boolean = now < (graceUntilMs ?: 0L)

    /** A key with no hold has no timestamp here, i.e. [lastMarkedMs] is 0. */
    fun isRecentlyMarked(now: Long, lastMarkedMs: Long, windowMs: Long): Boolean = (now - lastMarkedMs) < windowMs

    /**
     * Whether a removal leaves the island card alone because the listener cancelled the notification itself a
     * moment ago. [reason] is null for the callback that carries none, which cannot be told from our own cancel
     */
    fun isOwnRecentCancel(reason: Int?, recentInitialSuppression: Boolean): Boolean =
        recentInitialSuppression &&
            (reason == null || reason == NotificationListenerService.REASON_LISTENER_CANCEL)
}

/**
 * Hides a notification from the system shade by cancelling it, and remembers which keys the listener cancelled
 * itself so a later removal is not taken for the user's. It retries a cancel that did not take, and gives up when
 * the island or the hiding is switched off. Starting it and cancelling its jobs are the service's to decide:
 * [scope] is the service's main scope and [clearAll] ends everything.
 */
internal class ShadeSuppressor(
    private val scope: CoroutineScope,
    private val settings: () -> SmartIslandSettings,
    private val readActive: () -> Array<StatusBarNotification>?,
    private val cancel: (String) -> Unit,
    private val clock: () -> Long
) {
    // Keys the listener cancelled itself to make them island-only
    private val keys = SuppressedKeys(clock, SUPPRESSED_KEY_TTL_MS, MAX_SUPPRESSED_KEYS)
    private val pendingJobs = ConcurrentHashMap<String, Job>()

    fun mark(key: String) = keys.mark(key)

    /** 0 when the key is not held. */
    fun lastMarked(key: String): Long = keys.lastMarked(key)

    fun clear(key: String) {
        pendingJobs.remove(key)?.cancel()
        keys.remove(key)
    }

    /** Forgets every key and leaves the retries running, for the settings that stop the hiding. */
    fun forgetKeys() = keys.clear()

    /** Drops the keys that have outlived their time. */
    fun dropExpiredKeys() = keys.cleanup()

    fun clearAll() {
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
        keys.clear()
    }

    fun suppress(key: String) {
        if (!settings().enabled || !settings().hideFromNotificationShade) return
        val activeSbn = runCatchingLogged(TAG, "Failed to get active notifications for key lookup") {
            readActive()?.find { it.key == key }
        }
        if (activeSbn != null && activeSbn.packageName in settings().disabledNotificationPackages) return

        val now = clock()
        val isRecentlySuppressed = NotificationRemovalRules.isRecentlyMarked(
            now, keys.lastMarked(key), NotificationRemovalRules.RECENT_SUPPRESSION_WINDOW_MS
        )

        mark(key)

        pendingJobs.remove(key)?.cancel()

        if (!isRecentlySuppressed) {
            runCatchingLogged(TAG, "sync cancel failed") { cancel(key) }
        }

        val job = scope.launch {
            runSuspendCatchingLogged(TAG, "Notification suppression retries failed") {
                repeat(3) { attempt ->
                    delay(100L * (attempt + 1)) // 100, 200, 300ms
                    if (!settings().enabled ||
                        !settings().hideFromNotificationShade
                    ) {
                        clear(key)
                        return@runSuspendCatchingLogged
                    }
                    val stillActive = runCatchingLogged(TAG, "Failed checking stillActive") {
                        readActive()?.any { it.key == key }
                    } ?: false
                    if (!stillActive) {
                        return@runSuspendCatchingLogged
                    }
                    try {
                        cancel(key)
                    } catch (e: Exception) {
                        AppLogRecorder.e(TAG, "cancel retry $attempt failed", e)
                    }
                }
                AppLogRecorder.w(TAG, "could not suppress after retries: $key")
            }
            pendingJobs.remove(key)
        }
        pendingJobs[key] = job
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
        const val MAX_SUPPRESSED_KEYS = 100
        const val SUPPRESSED_KEY_TTL_MS = 10 * 60 * 1000L
    }
}
