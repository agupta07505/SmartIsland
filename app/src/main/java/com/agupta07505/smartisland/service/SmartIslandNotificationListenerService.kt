package com.agupta07505.smartisland.service

import android.app.Notification
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

class SmartIslandNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        val extras = notification.extras
        val mode = notification.toIslandMode()
        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)

        SmartIslandOverlayService.updateNotification(
            IslandNotification(
                packageName = sbn.packageName,
                appName = appName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
                timeMillis = sbn.postTime,
                icon = loadAppIconBitmap(sbn.packageName),
                actions = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty(),
                category = notification.category,
                progress = extras.getInt(Notification.EXTRA_PROGRESS, 0),
                progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            ),
            mode
        )
    }

    private fun Notification.toIslandMode(): IslandMode {
        return when (category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_MISSED_CALL -> IslandMode.IncomingCall
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_PROGRESS -> IslandMode.Music
            else -> {
                val hasMediaAction = actions?.any { action ->
                    val label = action.title?.toString()?.lowercase().orEmpty()
                    label.contains("play") ||
                        label.contains("pause") ||
                        label.contains("next") ||
                        label.contains("previous")
                } == true
                if (hasMediaAction) IslandMode.Music else IslandMode.Notification
            }
        }
    }

    private fun loadAppIconBitmap(packageName: String): Bitmap? {
        return runCatching {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96)
        }.getOrNull()
    }
}
