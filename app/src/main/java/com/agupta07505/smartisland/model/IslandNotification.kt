/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.PendingIntent
import android.graphics.Bitmap
import android.media.session.MediaSession

data class IslandNotification(
    val key: String = "",
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val icon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val smallIcon: Bitmap? = null,
    val actionIntents: List<IslandNotificationAction> = emptyList(),
    val category: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mode: IslandMode = IslandMode.Notification,
    val contentIntent: PendingIntent? = null,
    val lastPostedMs: Long = 0L,
    val isSilent: Boolean = false,
    /**
     * The card arrives on the pill without a pulse (the island's own timer and stopwatch, a throttled repeat of a
     * system event). Read once, when the key is new; a repost of the same key never changes it
     */
    val quietArrival: Boolean = false,

    val mediaPositionMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val mediaIsPlaying: Boolean = false,
    val mediaToken: MediaSession.Token? = null,

    val transferKind: TransferKind = TransferKind.DOWNLOAD,
    val transferState: TransferState = TransferState.ACTIVE,
    val transferDetail: String = "",
    val isEstimatedTransfer: Boolean = false,

    /** Elapsed time while paused or finished (recorder and stopwatch), so the timer does not move. */
    val recordingFrozenMs: Long? = null,
    val recordingPaused: Boolean = false,
    val stopwatchElapsedSeconds: Long? = null,
    /** Android's monotonic chronometer base while the stopwatch runs. */
    val stopwatchChronometerBase: Long? = null,
    val timerRemainingSeconds: Long? = null,
    val timerTotalMs: Long? = null,
    /** Google / AOSP / third-party clock: exact times shown like the shade. False for OEM clocks, which round. */
    val exactClock: Boolean = false,
    val timerFinished: Boolean = false,

    val callStatusText: String = "",
    val callEnd: CallEnd? = null,

    val media: List<NotificationMedia> = emptyList(),
    val bigText: String = "",
    /** Text for the picture reader: the notification text without the picture messages' labels and captions. */
    val readerText: String = "",

    /** One-tap chips: the replies the app gives and the ones Android's assistant suggests. */
    val suggestedReplies: List<String> = emptyList(),
    /** A Copy code action Android's assistant added, it is not among the app's own actions. */
    val systemCopyAction: IslandNotificationAction? = null
) {
    val isLoud: Boolean
        get() = !isSilent

    val isUpload: Boolean
        get() = transferKind.pointsUp

    val actions: List<String>
        get() = actionIntents.map { it.title }

    val isCallConnecting: Boolean
        get() = callStatusText.isNotBlank()

    val isCallRinging: Boolean
        get() = actionIntents.any { action ->
            VoipCallDetector.classifyCallAction(action.title) ==
                CallActionRole.Answer
        }
}

data class IslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent?,
    val remoteInputKey: String? = null,
    val hasRemoteInput: Boolean = false,
    val icon: Bitmap? = null,
    val viaView: Boolean = false
)
