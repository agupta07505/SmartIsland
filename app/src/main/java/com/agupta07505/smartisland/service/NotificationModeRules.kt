/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import com.agupta07505.smartisland.model.IslandMode

/** Decisions the notification listener makes from a mode, a removal reason or an importance alone. */
internal object NotificationModeRules {
    /** Whether the island replaces the system notification for this mode; [isRinging] is read for a call only. */
    fun isIslandOnly(mode: IslandMode, isRinging: () -> Boolean): Boolean {
        if (mode == IslandMode.IncomingCall) {
            if (!isRinging()) return false
        }
        if (mode == IslandMode.Music || mode == IslandMode.Navigation || mode == IslandMode.Timer
            || mode == IslandMode.Stopwatch ||
            mode == IslandMode.ScreenRecording || mode == IslandMode.AudioRecording
        ) {
            return false
        }
        return true
    }

    /** A group summary, apart from a call: apps like WhatsApp post 1 per conversation beside the message. */
    fun isGroupSummary(flags: Int, isCall: Boolean): Boolean =
        (flags and Notification.FLAG_GROUP_SUMMARY) != 0 && !isCall

    fun isPersistent(mode: IslandMode): Boolean =
        mode == IslandMode.Music ||
            mode == IslandMode.IncomingCall ||
            mode == IslandMode.Timer ||
            mode == IslandMode.Stopwatch ||
            mode == IslandMode.Navigation ||
            mode == IslandMode.LiveActivity ||
            mode == IslandMode.DownloadUpload ||
            mode == IslandMode.Hotspot ||
            mode == IslandMode.ScreenRecording ||
            mode == IslandMode.AudioRecording

    /** Wait before a removed card leaves the island, [cardMode] null when the island holds no such card. */
    fun removalDelayMs(cardMode: IslandMode?): Long =
        if (cardMode == IslandMode.ScreenRecording || cardMode == IslandMode.AudioRecording) 0L else 350L

    fun removalReasonName(reason: Int): String = when (reason) {
        NotificationListenerService.REASON_CLICK -> "click"
        NotificationListenerService.REASON_CANCEL -> "cancel"
        NotificationListenerService.REASON_CANCEL_ALL -> "cancel-all"
        NotificationListenerService.REASON_LISTENER_CANCEL -> "listener-cancel"
        NotificationListenerService.REASON_APP_CANCEL -> "app-cancel"
        NotificationListenerService.REASON_TIMEOUT -> "timeout"
        else -> "other"
    }

    /** Below the default importance, an unspecified one is not silent. */
    fun isSilentImportance(importance: Int): Boolean =
        importance != NotificationManager.IMPORTANCE_UNSPECIFIED &&
            importance < NotificationManager.IMPORTANCE_DEFAULT
}
