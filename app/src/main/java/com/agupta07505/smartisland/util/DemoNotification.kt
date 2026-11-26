/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Real Android notification for the Demo tab. It goes through the system and the listener service,
 * so it shows the same path as a notification from any other app.
 */
object DemoNotification {
    const val TAG = "smartisland_demo"
    private const val CHANNEL_ID = "smartisland_demo_channel"
    private const val NOTIFICATION_ID = 7101

    fun isDemo(sbn: StatusBarNotification): Boolean = sbn.tag == TAG
    fun isDemoKey(key: String): Boolean = key.contains("|$TAG|")

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** False when the notification permission is missing, so the caller can ask for it. */
    fun post(context: Context): Boolean {
        if (!canPost(context)) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Demo notifications", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val text = "Are we still on for tonight?"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Sam")
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text I booked the table for 8, let me know if that works.")
            )
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .build()
        nm.notify(TAG, NOTIFICATION_ID, notification)
        return true
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(TAG, NOTIFICATION_ID)
    }
}
