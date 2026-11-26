/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.os.Build
import androidx.annotation.RequiresApi
import com.agupta07505.smartisland.model.CallActionRole
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.VoipCallDetector

object CallStyleExtras {
    private const val EXTRA_CALL_TYPE = "android.callType"
    private const val EXTRA_ANSWER_INTENT = "android.answerIntent"
    private const val EXTRA_DECLINE_INTENT = "android.declineIntent"
    private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"

    const val CALL_TYPE_INCOMING = 1
    const val CALL_TYPE_ONGOING = 2
    const val CALL_TYPE_SCREENING = 3

    private const val COMPAT_TEMPLATE_KEY = "androidx.core.app.extras.COMPAT_TEMPLATE"

    /**
     * True for the platform CallStyle and for the androidx compat CallStyle (what apps use below Android 12, or when
     * they build through NotificationCompat), which has its own template name
     */
    fun isCallTemplate(n: Notification): Boolean = quietly(false) {
        val extras = n.extras ?: return@quietly false
        extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$CallStyle" ||
            extras.getString(COMPAT_TEMPLATE_KEY)?.endsWith("CallStyle") == true
    }

    private val CHANNEL_CALL_HINT = Regex("(^|[^a-z])(voip|calls?|ringing|ringtone)([^a-z]|$)")
    private val CHANNEL_NOT_CALL =
        listOf("missed", "history", "ended", "summary", "reminder", "message", "record", "log")

    /** Channels named like "voip_notification" or "incoming_calls", for apps with no call category or CallStyle. */
    fun channelLooksLikeCall(n: Notification): Boolean {
        val id = quietly { n.channelId }?.lowercase() ?: return false
        if (CHANNEL_NOT_CALL.any { id.contains(it) }) return false
        return CHANNEL_CALL_HINT.containsMatchIn(id)
    }

    fun callType(n: Notification): Int =
        quietly(0) { n.extras?.getInt(EXTRA_CALL_TYPE, 0) ?: 0 }

    private fun pendingIntent(n: Notification, key: String): PendingIntent? = quietly {
        val extras = n.extras ?: return@quietly null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(key, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<PendingIntent>(key)
        }
    }

    /** Hard evidence only: CallStyle type, an answer button or an answer intent. */
    fun isRinging(n: Notification): Boolean {
        val type = callType(n)
        if (type == CALL_TYPE_INCOMING || type == CALL_TYPE_SCREENING) return true
        if (type == CALL_TYPE_ONGOING) return false
        val labels = quietly(emptyList()) { n.actions.orEmpty().map { it.title?.toString().orEmpty() } }
        if (labels.any { VoipCallDetector.classifyCallAction(it) == CallActionRole.Answer }) return true
        return pendingIntent(n, EXTRA_ANSWER_INTENT) != null
    }

    /**
     * Extends [isRinging] for VoIP apps with custom buttons: call category, full-screen intent, chronometer, ringing
     * text
     */
    fun looksRinging(n: Notification): Boolean {
        if (isRinging(n)) return true
        if (callType(n) == CALL_TYPE_ONGOING) return false
        if (quietly { n.category } != Notification.CATEGORY_CALL) return false
        val extras = quietly { n.extras }
        val timerRunning = quietly(false) { extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true }
        if (timerRunning) return false
        if (quietly { n.fullScreenIntent } != null) return true
        val text = quietly("") {
            "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)}"
        }.lowercase()
        return INCOMING_WORDS.any { text.contains(it) }
    }

    /** App's own status line ("Calling...", "Dialing") as written, for outgoing calls not yet picked up. */
    fun connectingStatus(n: Notification): String? {
        if (isRinging(n)) return null
        val extras = quietly { n.extras } ?: return null
        val timerRunning = quietly(false) { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) }
        if (timerRunning) return null
        val texts = quietly(emptyList()) {
            listOf(Notification.EXTRA_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_BIG_TEXT)
                .mapNotNull { extras.getCharSequence(it)?.toString() }
        }
        return connectingStatusOf(texts)
    }

    fun connectingStatusOf(texts: List<String>): String? =
        texts.map { it.trim() }.firstOrNull { textLooksConnecting(it) }

    /** Pure text check, the whole line must be a short "dialing" status. */
    fun textLooksConnecting(text: String): Boolean {
        val cleaned = text.trim().lowercase().trimEnd('.', '\u2026', ' ')
        if (cleaned.isEmpty() || cleaned.length > 24) return false
        return CONNECTING_PHRASES.any { cleaned == it }
    }

    private val CONNECTING_PHRASES = listOf(
        "calling", "dialing", "dialling", "connecting", "ringing", "outgoing call", "outgoing",
        "calling now", "llamando", "marcando", "chamando", "呼叫中", "正在呼叫", "拨号中", "発信中", "вызов"
    )

    private val INCOMING_WORDS = listOf(
        "incoming", "ringing", "is calling", "calling you", "llamada entrante", "appel entrant",
        "eingehender anruf", "来电", "着信", "входящий"
    )

    fun callerName(n: Notification): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) callerNameApi28(n) else null

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun callerNameApi28(n: Notification): String? = quietly {
        val extras = n.extras ?: return@quietly null
        val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable("android.callPerson", android.app.Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<android.app.Person>("android.callPerson")
        }
        person?.name?.toString()?.takeIf { it.isNotBlank() }
    }

    /** Adds the CallStyle intents the actions lack: untitled actions get dropped when read. */
    fun withCallIntents(
        n: Notification,
        actions: List<IslandNotificationAction>
    ): List<IslandNotificationAction> {
        val result = actions.toMutableList()
        // This fixes call answer button mismatch, on dialers with 3 buttons
        // The AxionOS dialer port labels the extra action "answer to voicemail".
        fun useCallIntent(role: CallActionRole, label: String, intent: PendingIntent?) {
            if (intent == null) return
            result.removeAll { it.pendingIntent == intent || VoipCallDetector.classifyCallAction(it.title) == role }
            result.add(IslandNotificationAction(label, intent))
        }
        useCallIntent(CallActionRole.Answer, "Answer", pendingIntent(n, EXTRA_ANSWER_INTENT))
        val decline = pendingIntent(n, EXTRA_DECLINE_INTENT)
        if (decline != null) useCallIntent(CallActionRole.HangUp, "Decline", decline)
        else useCallIntent(CallActionRole.HangUp, "Hang up", pendingIntent(n, EXTRA_HANG_UP_INTENT))
        return result
    }
}
