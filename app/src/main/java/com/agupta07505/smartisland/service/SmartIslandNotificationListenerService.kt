package com.agupta07505.smartisland.service

import android.app.Notification
import android.graphics.Bitmap
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.model.IslandNotification

class SmartIslandNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val notification = sbn.notification
        val extras = notification.extras
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
                actions = notification.actions?.mapNotNull { it.title?.toString() }.orEmpty()
            )
        )
    }

    private fun loadAppIconBitmap(packageName: String): Bitmap? {
        return runCatching {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96)
        }.getOrNull()
    }
}
