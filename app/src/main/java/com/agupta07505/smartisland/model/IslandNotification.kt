/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class IslandNotification(
    val key: String = "",
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val icon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val actionIntents: List<IslandNotificationAction> = emptyList(),
    val category: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mediaPositionMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val mediaIsPlaying: Boolean = false,
    val mediaToken: android.media.session.MediaSession.Token? = null,
    val mode: IslandMode = IslandMode.Notification,
    val contentIntent: PendingIntent? = null
) {
    // Derived: no need to store separately
    val actions: List<String>
        get() = actionIntents.map { it.title }
}

data class IslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent?
)
