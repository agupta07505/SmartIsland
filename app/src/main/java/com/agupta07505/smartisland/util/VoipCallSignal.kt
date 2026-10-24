/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import com.agupta07505.smartisland.model.VoipCallDetector
import com.agupta07505.smartisland.model.VoipNotificationFacts

/**
 * Shared "a VoIP call is live" flag. [VoipCallMonitor] writes it from the audio mode, `toIslandMode` reads it so an
 * ongoing call notification shows as a call card
 */
object VoipCallSignal {
    @Volatile var active: Boolean = false
        private set
    @Volatile var sinceMillis: Long = 0L
        private set
    /** True while some app records with the VOICE_COMMUNICATION source (confirms a real call). */
    @Volatile var voiceCapture: Boolean = false

    fun setActive(value: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        if (value && !active) sinceMillis = nowMillis
        if (!value) {
            sinceMillis = 0L
            voiceCapture = false
        }
        active = value
    }
}

fun Notification.toVoipFacts(packageName: String): VoipNotificationFacts {
    val ex = extras
    val text = "${ex?.getCharSequence(Notification.EXTRA_TITLE)} ${ex?.getCharSequence(Notification.EXTRA_TEXT)}"
    return VoipNotificationFacts(
        packageName = packageName,
        isOngoing = (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0,
        category = category,
        isCallStyle = CallStyleExtras.isCallTemplate(this),
        actionTitles = actions.orEmpty().mapNotNull { it.title?.toString() },
        showsChronometer = ex?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true,
        isMediaStyle = ex?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true ||
            category == Notification.CATEGORY_TRANSPORT,
        textLooksLikeCall = VoipCallDetector.textLooksLikeCall(text)
    )
}
