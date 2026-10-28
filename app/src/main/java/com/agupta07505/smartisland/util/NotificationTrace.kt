/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Notification trace for the diagnostic log: which posts get a detailed line, progress-bar spam kept out of the
 * 3000-entry buffer, a short description of a post
 */
object NotificationTrace {
    private val MESSENGER_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

    private const val SAME_LINE_MIN_GAP_MS = 5_000L
    private const val MAX_TRACKED_KEYS = 400

    private val lastLine = ConcurrentHashMap<String, Pair<String, Long>>()

    fun isMessenger(packageName: String): Boolean = packageName in MESSENGER_PACKAGES

    fun isDetailed(packageName: String, notification: Notification): Boolean =
        isMessenger(packageName) || quietly(false) { CallDiagnostics.isCallLike(notification) }

    /** Rate limit for repeated identical decisions (a progress bar re-posting every second would fill the buffer). */
    fun shouldLog(key: String, signature: String, detailed: Boolean,
            nowMs: Long = System.currentTimeMillis()): Boolean {
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
        fun len(key: String) = quietly(-1) { extras?.getCharSequence(key)?.length ?: 0 }
        return buildString {
            append("pkg=${sbn.packageName} key=${sbn.key} id=${sbn.id} tag=${sbn.tag}")
            append(" category=${n.category} channel=${quietly { n.channelId }}")
            append(" flags=0x${Integer.toHexString(n.flags)}")
            append(" groupSummary=${(n.flags and Notification.FLAG_GROUP_SUMMARY) != 0}")
            append(" ongoing=${(n.flags and Notification.FLAG_ONGOING_EVENT) != 0}")
            append(" fgService=${(n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0}")
            append(" group=${quietly { n.group }}")
            append(" importance=${importance ?: "?"}")
            append(" fullScreenIntent=${n.fullScreenIntent != null}")
            append(" template=${quietly { extras?.getString(Notification.EXTRA_TEMPLATE) }}")
            append(" actions=${quietly(-1) { n.actions?.size ?: 0 }}")
            append(" titleLen=${len(Notification.EXTRA_TITLE)} textLen=${len(Notification.EXTRA_TEXT)}")
            append(" postTime=${sbn.postTime}")
        }
    }
}

/**
 * Trace of the tap swap for diagnosing it on a device, off unless switched on:
 * `adb shell setprop log.tag.SmartIslandSwap VERBOSE`, then `adb logcat -s SmartIslandSwap`
 */
object SwapTrace {
    private const val TAG = "SmartIslandSwap"

    fun enabled(): Boolean = Log.isLoggable(TAG, Log.VERBOSE)

    inline fun log(event: String, detail: () -> String = { "" }) {
        if (enabled()) emit(event, detail())
    }

    fun emit(event: String, detail: String) {
        Log.v(TAG, "${SystemClock.uptimeMillis()} $event $detail".trimEnd())
    }
}

/**
 * A small log of its own for 1 issue: the text selection's toolbar (reader, pill-held text, reply box) not showing.
 * It is always on, needs no recording switch and holds only the last [MAX_LINES] lines, which Developer options
 * shows, copies and exports. Every line also goes to [AppLogRecorder] so a full report holds them too.
 * Temporary: remove it with its card once the toolbar is confirmed on a device
 */
object SelectionToolbarLog {
    const val TAG = "SelectionToolbarDiag"
    const val MAX_LINES = 300

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    /** Composition and layout log the same thing again and again, a line equal to the last one is dropped. */
    fun log(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "[$stamp] $message"
        _lines.value = _lines.value.let { old ->
            if (old.lastOrNull()?.substringAfter("] ") == message) old else (old + line).takeLast(MAX_LINES)
        }
        AppLogRecorder.d(TAG, message)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    /** The whole log as text for Copy and Export. */
    fun asText(): String =
        if (_lines.value.isEmpty()) "No selection toolbar events logged." else _lines.value.joinToString("\n")
}
