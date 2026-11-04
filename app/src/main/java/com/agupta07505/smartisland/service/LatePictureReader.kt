/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.NotificationMediaReader
import com.agupta07505.smartisland.util.PictureRetryPlan
import com.agupta07505.smartisland.util.StalePostGuard
import com.agupta07505.smartisland.util.readerTextWithoutPictures
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads the pictures of a card whose notification had none yet (an app that attaches the image a moment after the
 * post): retries on the [PictureRetryPlan] delays and updates the card when one is found. 1 job per key
 */
internal class LatePictureReader(
    private val scope: CoroutineScope,
    private val callPostGuard: StalePostGuard,
    private val notificationRepository: () -> INotificationRepository,
    private val mediaReader: () -> NotificationMediaReader
) {
    private val jobs = ConcurrentHashMap<String, Job>()

    fun schedule(sbn: StatusBarNotification, notification: Notification, notifText: String) {
        val key = sbn.key
        jobs.remove(key)?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            runSuspendCatchingLogged(TAG, "Late picture job failed") {
                for (waitMs in PictureRetryPlan.DELAYS_MS) {
                    delay(waitMs)
                    if (callPostGuard.isStale(key, sbn.postTime)) return@runSuspendCatchingLogged
                    if (notificationRepository().notifications.value.none { it.key == key }) return@runSuspendCatchingLogged
                    val found = runCatchingLogged(TAG, "Late picture read failed") {
                        mediaReader().read(key, notification, notifText, sbn.packageName, allowFallbacks = true)
                    }.orEmpty()
                    if (found.isNotEmpty()) {
                        val reader = runCatchingLogged(TAG, "Late picture reader text failed") {
                            notification.readerTextWithoutPictures(notifText)
                        }.orEmpty()
                        notificationRepository().updateNotification(key) { it.copy(media = found, readerText = reader) }
                        return@runSuspendCatchingLogged
                    }
                }
                AppLogRecorder.w(TAG, "no picture for $key after the last retry")
            }
        }
        jobs[key] = job
        job.invokeOnCompletion { jobs.remove(key, job) }
    }

    /** The card is gone or its notification was removed: what was still waiting for its picture stops. */
    fun cancel(key: String) {
        jobs.remove(key)?.cancel()
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }
}
