/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.data.INotificationHistoryRepository
import com.agupta07505.smartisland.data.NotificationHistoryEntry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class NotificationHistoryRecorder(
    private val scope: CoroutineScope,
    private val repository: () -> INotificationHistoryRepository
) {
    private var lastCleanupTime = 0L

    fun save(
        sbn: StatusBarNotification,
        notification: Notification,
        settings: SmartIslandSettings,
        mode: IslandMode,
        appName: String,
        title: String,
        text: String,
        timeMillis: Long,
        actions: List<IslandNotificationAction>
    ) {
        val extras = notification.extras
        scope.launch {
            runSuspendCatchingLogged(TAG, "Failed to record notification history") {
                val history = repository()
                history.saveEntry(
                    NotificationHistoryEntry(
                        notificationKey = sbn.key,
                        packageName = sbn.packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                        postTimeMillis = timeMillis,
                        category = notification.category,
                        channelId = notification.channelId,
                        mode = mode.name,
                        actionTitles = actions.map { it.title }
                    )
                )
                val now = System.currentTimeMillis()
                if (now - lastCleanupTime > 15 * 60 * 1000L) {
                    lastCleanupTime = now
                    history.cleanupOldEntries(settings.notificationHistoryRetentionHours)
                }
            }
        }
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
    }
}
