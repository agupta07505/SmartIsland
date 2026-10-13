/*
 * Realistic Dynamic Island – NotificationListener skeleton
 * Converts system notifications + Android 16 Live Updates into IslandNotifications
 */

package com.srcpolygot.dynamicisland.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.domain.model.IslandMode
import com.srcpolygot.dynamicisland.domain.model.IslandNotification

/**
 * Listens for notifications and maps them into the 3-slot ActiveIslandState.
 * Will later integrate Android 16 Live Updates (ProgressStyle) for first-class support.
 */
class DynamicIslandNotificationListener : NotificationListenerService() {

    // In a full implementation this would be injected / shared with the OverlayService
    private var currentState = ActiveIslandState()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = mapToIslandNotification(sbn) ?: return
        // TODO: merge into the 3-slot state (primary / secondary / tertiary)
        // and push the update to DynamicIslandOverlayService
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        // TODO: remove the matching activity from ActiveIslandState
    }

    private fun mapToIslandNotification(sbn: StatusBarNotification): IslandNotification? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val packageName = sbn.packageName

        val mode = when {
            packageName.contains("dialer", ignoreCase = true) ||
                packageName.contains("phone", ignoreCase = true) -> IslandMode.IncomingCall
            packageName.contains("music", ignoreCase = true) ||
                packageName.contains("spotify", ignoreCase = true) ||
                packageName.contains("youtube", ignoreCase = true) -> IslandMode.Music
            packageName.contains("clock", ignoreCase = true) -> IslandMode.Timer
            packageName.contains("maps", ignoreCase = true) -> IslandMode.Navigation
            else -> IslandMode.Notification
        }

        return IslandNotification(
            key = sbn.key,
            packageName = packageName,
            title = title,
            text = text,
            mode = mode,
            isOngoing = sbn.isOngoing
        )
    }
}
