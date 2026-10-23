/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import com.agupta07505.smartisland.model.IslandNotificationAction

/**
 * Reads the extras of `Notification.CallStyle`, so a ringing call is recognised even when the app's buttons have
 * no (or an unexpected) label, or the notification is not flagged ongoing (a heads-up incoming call usually is not).
 *
 * Every read is guarded: a malformed OEM bundle must never break notification handling.
 */
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
     * they build through NotificationCompat), which has its own template name.
     */
    fun isCallTemplate(n: Notification): Boolean = runCatching {
        val extras = n.extras ?: return@runCatching false
        extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$CallStyle" ||
            extras.getString(COMPAT_TEMPLATE_KEY)?.endsWith("CallStyle") == true
    }.getOrDefault(false)

    private val CHANNEL_CALL_HINT = Regex("(^|[^a-z])(voip|calls?|ringing|ringtone)([^a-z]|$)")
    private val CHANNEL_NOT_CALL = listOf("missed", "history", "ended", "summary", "reminder", "message", "record", "log")

    /**
     * VoIP apps put their ringing and in-call notifications on a channel named like "voip_notification" or
     * "incoming_calls". Used when the app sets no call category and no CallStyle.
     */
    fun channelLooksLikeCall(n: Notification): Boolean {
        val id = runCatching { n.channelId }.getOrNull()?.lowercase() ?: return false
        if (CHANNEL_NOT_CALL.any { id.contains(it) }) return false
        return CHANNEL_CALL_HINT.containsMatchIn(id)
    }

    fun callType(n: Notification): Int =
        runCatching { n.extras?.getInt(EXTRA_CALL_TYPE, 0) ?: 0 }.getOrDefault(0)

    private fun pendingIntent(n: Notification, key: String): PendingIntent? = runCatching {
        val extras = n.extras ?: return@runCatching null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(key, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<PendingIntent>(key)
        }
    }.getOrNull()

    /**
     * True when the notification itself says the call is ringing: CallStyle type, an answer button, or an answer
     * intent. Strict on purpose; it decides whether the shade copy is removed.
     */
    fun isRinging(n: Notification): Boolean {
        val type = callType(n)
        if (type == CALL_TYPE_INCOMING || type == CALL_TYPE_SCREENING) return true
        if (type == CALL_TYPE_ONGOING) return false
        val labels = runCatching { n.actions.orEmpty().map { it.title?.toString().orEmpty() } }
            .getOrDefault(emptyList())
        if (labels.any { VoipCallDetector.classifyCallAction(it) == CallActionRole.Answer }) return true
        return pendingIntent(n, EXTRA_ANSWER_INTENT) != null
    }

    /**
     * [isRinging] plus guesses for VoIP apps that keep their buttons in a custom view: a call-category notification
     * with a full-screen intent and no running chronometer, or one that says "incoming" / "ringing".
     */
    fun looksRinging(n: Notification): Boolean {
        if (isRinging(n)) return true
        if (callType(n) == CALL_TYPE_ONGOING) return false
        if (n.category != Notification.CATEGORY_CALL) return false
        val extras = n.extras
        val timerRunning = runCatching { extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true }
            .getOrDefault(false)
        if (timerRunning) return false
        if (n.fullScreenIntent != null) return true
        val text = runCatching {
            "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)}"
        }.getOrDefault("").lowercase()
        return INCOMING_WORDS.any { text.contains(it) }
    }

    /**
     * The app's own status line ("Calling...", "Dialing", "Ringing"), exactly as written, when the notification is an
     * outgoing call not yet picked up; null otherwise.
     */
    fun connectingStatus(n: Notification): String? {
        if (isRinging(n)) return null
        val extras = n.extras ?: return null
        val timerRunning = runCatching { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) }
            .getOrDefault(false)
        if (timerRunning) return null
        val texts = runCatching {
            listOf(Notification.EXTRA_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_BIG_TEXT)
                .mapNotNull { extras.getCharSequence(it)?.toString() }
        }.getOrDefault(emptyList())
        return connectingStatusOf(texts)
    }

    fun connectingStatusOf(texts: List<String>): String? =
        texts.map { it.trim() }.firstOrNull { textLooksConnecting(it) }

    /** Pure text check; the whole line must be a short "dialing" status. */
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
    private fun callerNameApi28(n: Notification): String? = runCatching {
        val extras = n.extras ?: return@runCatching null
        val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable("android.callPerson", android.app.Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<android.app.Person>("android.callPerson")
        }
        person?.name?.toString()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * The app's own buttons plus the CallStyle intents that are missing from them (some apps hand the system an
     * action without a title, and those are dropped when the actions are read).
     */
    fun withCallIntents(
        n: Notification,
        actions: List<IslandNotificationAction>
    ): List<IslandNotificationAction> {
        val result = actions.toMutableList()
        fun has(role: CallActionRole) = result.any { VoipCallDetector.classifyCallAction(it.title) == role }
        if (!has(CallActionRole.Answer)) {
            pendingIntent(n, EXTRA_ANSWER_INTENT)?.let { result.add(IslandNotificationAction("Answer", it)) }
        }
        if (!has(CallActionRole.HangUp)) {
            val decline = pendingIntent(n, EXTRA_DECLINE_INTENT)
            val hangUp = pendingIntent(n, EXTRA_HANG_UP_INTENT)
            when {
                decline != null -> result.add(IslandNotificationAction("Decline", decline))
                hangUp != null -> result.add(IslandNotificationAction("Hang up", hangUp))
            }
        }
        return result
    }
}
