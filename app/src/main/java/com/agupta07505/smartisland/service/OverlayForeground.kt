/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.util.runCatchingLogged

internal class OverlayForeground(
    private val service: Service,
    private val isDestroyed: () -> Boolean
) {
    private var started = false

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the Smart Island overlay running"
            setShowBadge(false)
        }
        val nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.createNotificationChannel(channel)
    }

    fun ensureStarted() {
        if (started || isDestroyed()) return
        runCatchingLogged(TAG, "startForeground failed") {
            service.startForeground(NOTIFICATION_ID, buildNotification())
            started = true
        }
    }

    fun stopSafely() {
        if (!started) return
        runCatchingLogged(TAG, "stopForeground failed") {
            ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        started = false
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(service, CHANNEL_ID)
            .setContentTitle("Smart Island is active")
            .setContentText("Tap to open Smart Island")
            .setSmallIcon(R.drawable.ic_stat_smart_island)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
        const val NOTIFICATION_ID = 8105
        const val CHANNEL_ID = "smart_island_overlay"
        const val CHANNEL_NAME = "Smart Island overlay"
    }
}
