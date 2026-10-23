/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class IslandNotification(
    val key: String = "",
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val icon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val actionIntents: List<IslandNotificationAction> = emptyList(),
    val category: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mediaPositionMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val mediaIsPlaying: Boolean = false,
    val mediaToken: android.media.session.MediaSession.Token? = null,
    val mode: IslandMode = IslandMode.Notification,
    val contentIntent: PendingIntent? = null,
    /** What a DownloadUpload card is doing (download, upload, backup, copy, delete, install). */
    val transferKind: TransferKind = TransferKind.DOWNLOAD,
    /** Paused, waiting for network, failed ... (DownloadUpload cards only). */
    val transferState: TransferState = TransferState.ACTIVE,
    /** "4.2 MB/s · 3 min left", read from the notification text or measured for estimated cards. */
    val transferDetail: String = "",
    /** True for cards the island made itself from network speed (no app notification behind them). */
    val isEstimatedTransfer: Boolean = false,
    /** Recorder and stopwatch cards: elapsed time while paused or finished (the timer must not move). */
    val recordingFrozenMs: Long? = null,
    /** Recorder, stopwatch and timer cards: paused (the app offers "Resume"). */
    val recordingPaused: Boolean = false,
    /**
     * A standard notification the system lists as "Silent" (importance below DEFAULT). Swipe-to-switch-event skips
     * these; ongoing live cards are never flagged.
     */
    val isSilent: Boolean = false,
    /**
     * Call cards only: the app's own status line of an outgoing call not yet picked up ("Calling...", "Dialing"),
     * worded as the notification does. Replaces the timer while set.
     */
    val callStatusText: String = "",
    /** Pictures, stickers and animated stickers the shade shows (plain notifications only). */
    val media: List<NotificationMedia> = emptyList(),
    /** What the shade shows when expanded (BigText, inbox lines, chat messages); empty if none. */
    val bigText: String = "",
    /**
     * Text for the picture reader: the notification text without the picture messages' own labels and captions; empty
     * when only pictures.
     */
    val readerText: String = "",
    /** Wall-clock time the app last posted this card (0 = unknown); transfer cards expire when it goes quiet. */
    val lastPostedMs: Long = 0L,
    /** Music cards: the notification's own monochrome small icon, which the shade tints; null if unavailable. */
    val smallIcon: Bitmap? = null,
    /** Frozen elapsed seconds while the Clock stopwatch is paused. */
    val stopwatchElapsedSeconds: Long? = null,
    /** Android's monotonic chronometer base while the stopwatch runs. */
    val stopwatchChronometerBase: Long? = null,
    /** Frozen remaining seconds while a Clock timer is paused. */
    val timerRemainingSeconds: Long? = null,
    /**
     * Timer cards from a Google / AOSP / third-party clock: [timeMillis] and [recordingFrozenMs] are exact and shown
     * like the shade; false for OEM clocks, which keep their rounding.
     */
    val exactClock: Boolean = false
) {
    /**
     * Not silent (importance DEFAULT or higher); live and system cards count as loud. Loud cards keep auto-expanding
     * even if the app's own auto-expand was never switched on.
     */
    val isLoud: Boolean
        get() = !isSilent

    /** True when the transfer leaves the phone (upload, backup). */
    val isUpload: Boolean
        get() = transferKind.pointsUp

    val actions: List<String>
        get() = actionIntents.map { it.title }

    /** True while an outgoing call is dialing or ringing at the other end. */
    val isCallConnecting: Boolean
        get() = callStatusText.isNotBlank()

    val isCallRinging: Boolean
        get() = actionIntents.any { action ->
            com.agupta07505.smartisland.util.VoipCallDetector.classifyCallAction(action.title) ==
                com.agupta07505.smartisland.util.CallActionRole.Answer
        }
}

data class IslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent?,
    val isQuickReply: Boolean = false,
    val remoteInputKey: String? = null,
    /** The button's own icon; only read for media cards, so their extra buttons look like the app's. */
    val icon: android.graphics.Bitmap? = null,
    /** True when [pendingIntent] is the island's proxy for a button in the notification's custom view. */
    val viaView: Boolean = false
)
