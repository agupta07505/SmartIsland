/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.util.TimerStopwatchParser
import com.agupta07505.smartisland.util.TransferDetector
import com.agupta07505.smartisland.util.TransferInput
import com.agupta07505.smartisland.util.isDownloadComplete
import com.agupta07505.smartisland.util.isScreenRecordingComplete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal class FinishedCardRemover(
    private val scope: CoroutineScope,
    private val repository: () -> INotificationRepository,
    private val pendingRemovals: ConcurrentHashMap<String, Job>,
    private val clearSuppressed: (String) -> Unit
) {
    private val notificationRepository: INotificationRepository get() = repository()

    /** A finished or failed transfer card is removed after a short linger. */
    fun scheduleFinishedTransferRemoval(
        sbn: StatusBarNotification,
        notification: Notification,
        extras: Bundle,
        isInitialSync: Boolean
    ) {
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val failed = TransferDetector.evaluate(
            TransferInput.fromNotification(notification, sbn.packageName, false)
        ).state == TransferState.FAILED
        val isDone = PostedNotificationRules.isTransferDone(failed, notification.isDownloadComplete(), progress,
            progressMax)
        if (isDone) {
            removeAfter(sbn.key, PostedNotificationRules.transferLingerMs(failed, isInitialSync))
        }
    }

    fun scheduleFinishedRecordingRemoval(sbn: StatusBarNotification, notification: Notification) {
        if (notification.isScreenRecordingComplete()) {
            removeAfter(sbn.key, IslandTimings.RECORDING_DONE_MS)
        }
    }

    fun scheduleFinishedTimerRemoval(
        sbn: StatusBarNotification,
        notification: Notification,
        timerViewRunningMs: Long?
    ) {
        val isDone = PostedNotificationRules.isTimerDone(
            TimerStopwatchParser.isTimerFinished(notification),
            timerViewRunningMs
        )
        if (isDone) {
            removeAfter(sbn.key, PostedNotificationRules.TIMER_DONE_LINGER_MS)
        }
    }

    // All 3 cases end the same way: restart any pending removal for this key, then drop the card after a linger
    private fun removeAfter(key: String, lingerMs: Long) {
        pendingRemovals.remove(key)?.cancel()
        pendingRemovals[key] = scope.launch {
            delay(lingerMs)
            clearSuppressed(key)
            notificationRepository.removeNotification(key)
        }
    }
}
