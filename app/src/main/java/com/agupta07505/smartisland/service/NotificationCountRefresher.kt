/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.CountEntry
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.util.NotificationCountTracker
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

internal class NotificationCountRefresher(
    private val scope: CoroutineScope,
    private val ownPackage: String,
    private val appLabels: AppLabelCache,
    private val isConnected: () -> Boolean,
    private val readShade: () -> List<StatusBarNotification>?,
    private val islandNotifications: () -> List<IslandNotification>
) {
    private val refreshJob = AtomicReference<Job?>(null)

    fun schedule() {
        // callers run on several threads: the swap leaves exactly 1 pending refresh
        refreshJob.getAndSet(scope.launch {
            delay(DEBOUNCE_MS)
            refresh()
        })?.cancel()
    }

    private fun refresh() {
        if (!isConnected()) return
        val shade = runCatchingLogged(TAG, "Count: reading the shade failed") { readShade() }
            .orEmpty()
            .filter { it.packageName != ownPackage }
            .map { it.toCountEntry() }
        // the island may have cancelled its shade copy (hide from shade)
        val island = islandNotifications()
            .filter { it.mode == IslandMode.Notification && it.packageName != ownPackage }
            .map {
                CountEntry(
                    key = it.key,
                    packageName = it.packageName,
                    appLabel = it.appName.ifBlank { appLabels.labelOf(it.packageName) },
                    category = it.category
                )
            }
        NotificationCountTracker.update(
            NotificationCounts.merge(shade, island)
        )
    }

    private fun StatusBarNotification.toCountEntry(): CountEntry {
        val n = notification
        val flags = n?.flags ?: 0
        return CountEntry(
            key = key,
            packageName = packageName,
            appLabel = appLabels.labelOf(packageName),
            category = n?.category,
            isMessagingStyle =
                n?.extras?.getString(Notification.EXTRA_TEMPLATE) == Notification.MessagingStyle::class.java.name,
            ongoing = (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0,
            groupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
        )
    }

    private companion object {
        const val TAG = "SmartIslandNotificationListener"
        const val DEBOUNCE_MS = 150L
    }
}
