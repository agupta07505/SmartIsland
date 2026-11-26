/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import com.agupta07505.smartisland.model.CallEnds
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.VoipCallDetector
import java.util.concurrent.ConcurrentHashMap

object CallCardRules {

    /** Ringing replaces nothing, a second call may arrive while one is running. */
    fun supersededKeys(
        existing: List<IslandNotification>,
        newKey: String,
        packageName: String,
        newIsRinging: Boolean
    ): List<String> =
        if (newIsRinging) {
            emptyList()
        } else {
            existing
                .filter { it.mode == IslandMode.IncomingCall && it.packageName == packageName && it.key != newKey }
                .map { it.key }
        }
}

/** Hang-ups the user tapped on the island, so the end of that call can say "Call ended" (the user's own end) instead of "Hung up". */
object CallHangUps {
    private val marks = ConcurrentHashMap<String, Long>()

    fun mark(key: String, atMs: Long = System.currentTimeMillis()) {
        marks.values.removeAll { atMs - it > CallEnds.HANG_UP_WINDOW_MS }
        marks[key] = atMs
    }

    /** The time the user tapped hang-up on the card [key], once, null if they did not. */
    fun consume(key: String): Long? = marks.remove(key)
}

/** Every read is guarded: a malformed bundle must never break notification handling. */
object CallDiagnostics {
    fun isCallLike(n: Notification): Boolean {
        val extras = n.extras
        val text = quietly("") {
            "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)}"
        }
        return n.category == Notification.CATEGORY_CALL ||
            CallStyleExtras.isCallTemplate(n) ||
            CallStyleExtras.channelLooksLikeCall(n) ||
            n.fullScreenIntent != null ||
            VoipCallDetector.textLooksLikeCall(text) ||
            CallStyleExtras.looksRinging(n)
    }
}
