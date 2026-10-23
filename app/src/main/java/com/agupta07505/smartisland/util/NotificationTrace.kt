/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

/**
 * Helpers for the diagnostic log's notification trace: which posts deserve a detailed line, how to keep progress-bar
 * spam out of a 3000-entry buffer, and a short description of a post.
 */
object NotificationTrace {
    private val MESSENGER_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

    private const val SAME_LINE_MIN_GAP_MS = 5_000L
    private const val MAX_TRACKED_KEYS = 400

    private val lastLine = ConcurrentHashMap<String, Pair<String, Long>>()

    fun isMessenger(packageName: String): Boolean = packageName in MESSENGER_PACKAGES

    fun isDetailed(packageName: String, notification: Notification): Boolean =
        isMessenger(packageName) || runCatching { CallDiagnostics.isCallLike(notification) }.getOrDefault(false)

    /**
     * Rate limit for repeated identical decisions (a progress bar that re-posts every second would otherwise fill the
     * buffer).
     */
    fun shouldLog(key: String, signature: String, detailed: Boolean, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (detailed) return true
        if (lastLine.size > MAX_TRACKED_KEYS) lastLine.clear()
        val previous = lastLine[key]
        if (previous != null && previous.first == signature && nowMs - previous.second < SAME_LINE_MIN_GAP_MS) {
            return false
        }
        lastLine[key] = signature to nowMs
        return true
    }

    fun forget(key: String) {
        lastLine.remove(key)
    }

    /** One-line summary of a post: identifiers and flags only, never the message content. */
    fun brief(sbn: StatusBarNotification, importance: Int?): String {
        val n = sbn.notification
        val extras = n.extras
        fun len(key: String) = runCatching { extras?.getCharSequence(key)?.length ?: 0 }.getOrDefault(-1)
        return buildString {
            append("pkg=${sbn.packageName} key=${sbn.key} id=${sbn.id} tag=${sbn.tag}")
            append(" category=${n.category} channel=${runCatching { n.channelId }.getOrNull()}")
            append(" flags=0x${Integer.toHexString(n.flags)}")
            append(" groupSummary=${(n.flags and Notification.FLAG_GROUP_SUMMARY) != 0}")
            append(" ongoing=${(n.flags and Notification.FLAG_ONGOING_EVENT) != 0}")
            append(" fgService=${(n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0}")
            append(" group=${runCatching { n.group }.getOrNull()}")
            append(" importance=${importance ?: "?"}")
            append(" fullScreenIntent=${n.fullScreenIntent != null}")
            append(" template=${runCatching { extras?.getString(Notification.EXTRA_TEMPLATE) }.getOrNull()}")
            append(" actions=${runCatching { n.actions?.size ?: 0 }.getOrDefault(-1)}")
            append(" titleLen=${len(Notification.EXTRA_TITLE)} textLen=${len(Notification.EXTRA_TEXT)}")
            append(" postTime=${sbn.postTime}")
        }
    }
}
