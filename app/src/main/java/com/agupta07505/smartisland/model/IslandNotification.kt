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
    val contentIntent: PendingIntent? = null,
    /** The app's own group id (`Notification.getGroup()`), or null; alerts sharing one are folded together. */
    val groupId: String? = null,
    /** How many alerts this entry stands for; above 1 only on a grouped entry built by the alert engine. */
    val groupCount: Int = 1,
    /** Keys of every alert folded into this entry (empty for a plain, ungrouped alert). */
    val memberKeys: List<String> = emptyList(),
    /** Up to three sender avatars or app icons of the folded alerts, newest last, for the stacked pill. */
    val groupIcons: List<Bitmap> = emptyList(),
    /**
     * The app's lockscreen visibility (`Notification.visibility`), one of [LockscreenVisibility]. Internal
     * alerts (battery, Bluetooth, flashlight, demos) are public.
     */
    val lockscreenVisibility: Int = LockscreenVisibility.PUBLIC,
    /** True on a copy the alert engine stripped for the lockscreen: title is a fallback, text is empty. */
    val isMasked: Boolean = false
) {
    // Derived: no need to store separately
    val actions: List<String>
        get() = actionIntents.map { it.title }

    val isCallRinging: Boolean
        get() = actionIntents.any { action ->
            val label = action.title.lowercase()
            label.contains("answer") || label.contains("accept") || label.contains("take")
        }
}

/** Mirrors `Notification.VISIBILITY_*` so the alert engine can stay free of Android. */
object LockscreenVisibility {
    const val PUBLIC = 1
    const val PRIVATE = 0
    const val SECRET = -1
}

data class IslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent?,
    val isQuickReply: Boolean = false,
    val remoteInputKey: String? = null
)
