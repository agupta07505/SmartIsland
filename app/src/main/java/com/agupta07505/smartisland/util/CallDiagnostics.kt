/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification

/** Every read is guarded: a malformed bundle must never break notification handling. */
object CallDiagnostics {
    fun isCallLike(n: Notification): Boolean {
        val extras = n.extras
        val text = runCatching {
            "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)}"
        }.getOrDefault("")
        return n.category == Notification.CATEGORY_CALL ||
            CallStyleExtras.isCallTemplate(n) ||
            CallStyleExtras.channelLooksLikeCall(n) ||
            n.fullScreenIntent != null ||
            VoipCallDetector.textLooksLikeCall(text) ||
            CallStyleExtras.looksRinging(n)
    }
}
