/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SmartIslandNotificationRepository : INotificationRepository {
    private val _notifications = MutableStateFlow<List<IslandNotification>>(emptyList())
    override val notifications: StateFlow<List<IslandNotification>> = _notifications.asStateFlow()

    private val _autoExpandEvent = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val autoExpandEvent: SharedFlow<String> = _autoExpandEvent.asSharedFlow()

    private val _resetTimerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val resetTimerEvent: SharedFlow<Unit> = _resetTimerEvent.asSharedFlow()

    private val _commands = MutableSharedFlow<SmartIslandCommand>(extraBufferCapacity = 16)
    override val commands: SharedFlow<SmartIslandCommand> = _commands.asSharedFlow()

    override fun postNotification(notification: IslandNotification, autoExpand: Boolean) {
        var isNewNotification = false
        _notifications.update { existingNotifications ->
            val updated = existingNotifications.toMutableList()
            val index = updated.indexOfFirst { it.key == notification.key }
            if (index >= 0) {
                val existing = updated[index]
                updated[index] = notification.copy(
                    icon = notification.icon ?: existing.icon,
                    largeIcon = notification.largeIcon ?: existing.largeIcon
                )
            } else {
                isNewNotification = true
                updated.add(notification)
            }
            updated.takeLast(MAX_STORED_NOTIFICATIONS)
        }
        if (autoExpand && isNewNotification) {
            _autoExpandEvent.tryEmit(notification.key)
        }
    }

    override fun removeNotification(key: String) {
        _notifications.update { notifications ->
            notifications.filterNot { it.key == key }
        }
    }

    override fun removeNotificationsForPackage(packageName: String) {
        val matching = _notifications.value.filter { it.packageName == packageName && it.mode != IslandMode.Music }
        if (matching.isEmpty()) return
        _notifications.update { list ->
            list.filterNot { it.packageName == packageName && it.mode != IslandMode.Music }
        }
        for (notif in matching) {
            _commands.tryEmit(SmartIslandCommand.CancelNotification(notif.key))
        }
    }

    override fun removeAllNotifications() {
        _notifications.update { emptyList() }
    }

    override fun resetTimer() {
        _resetTimerEvent.tryEmit(Unit)
    }

    override fun sendCommand(command: SmartIslandCommand) {
        _commands.tryEmit(command)
    }

    override fun showDemo(mode: IslandMode) {
        val demoNotification = when (mode) {
            IslandMode.Notification -> IslandNotification(
                key = "demo_notif",
                packageName = "org.telegram.messenger",
                appName = "Telegram",
                title = "Alice Smith",
                text = "Hey! Are we still meeting for lunch today?",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Notification,
                actionIntents = listOf(
                    IslandNotificationAction("Reply", null),
                    IslandNotificationAction("Mark Read", null)
                )
            )
            IslandMode.IncomingCall -> IslandNotification(
                key = "demo_call",
                packageName = "com.google.android.dialer",
                appName = "Phone",
                title = "John Doe",
                text = "Incoming Call",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.IncomingCall,
                actionIntents = listOf(
                    IslandNotificationAction("Decline", null),
                    IslandNotificationAction("Answer", null)
                )
            )
            IslandMode.Music -> IslandNotification(
                key = "demo_music",
                packageName = "com.spotify.music",
                appName = "Spotify",
                title = "Starlight",
                text = "Muse - Black Holes and Revelations",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Music,
                mediaIsPlaying = true,
                mediaDurationMs = 240000L,
                mediaPositionMs = 45000L,
                actionIntents = listOf(
                    IslandNotificationAction("Previous", null),
                    IslandNotificationAction("Play", null),
                    IslandNotificationAction("Next", null)
                )
            )
            IslandMode.Battery -> IslandNotification(
                key = "demo_battery",
                packageName = "com.android.systemui",
                appName = "System",
                title = "Charging",
                text = "85%",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Battery
            )
            IslandMode.LiveActivity -> IslandNotification(
                key = "demo_live_activity",
                packageName = "com.ubercab",
                appName = "Uber",
                title = "Driver is on the way",
                text = "Arriving in 8 mins • 2.5 km away",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.LiveActivity,
                progress = 40,
                progressMax = 100,
                actionIntents = listOf(
                    IslandNotificationAction("Call Driver", null),
                    IslandNotificationAction("Share Trip", null)
                )
            )
            IslandMode.Navigation -> IslandNotification(
                key = "demo_navigation",
                packageName = "com.google.android.apps.maps",
                appName = "Google Maps",
                title = "Turn left onto MG Road",
                text = "In 300 m • 24 min remaining (8.5 km)",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Navigation,
                actionIntents = listOf(
                    IslandNotificationAction("Mute Voice", null),
                    IslandNotificationAction("Exit", null)
                )
            )
            IslandMode.DownloadUpload -> {
                val hasDownload = _notifications.value.any { it.key == "demo_download" }
                if (!hasDownload) {
                    IslandNotification(
                        key = "demo_download",
                        packageName = "com.android.chrome",
                        appName = "Chrome",
                        title = "video_2026_download.mp4",
                        text = "Downloading... 145 MB of 280 MB • 52%",
                        timeMillis = System.currentTimeMillis(),
                        mode = IslandMode.DownloadUpload,
                        progress = 52,
                        progressMax = 100,
                        actionIntents = listOf(
                            IslandNotificationAction("Pause", null),
                            IslandNotificationAction("Cancel", null)
                        )
                    )
                } else {
                    IslandNotification(
                        key = "demo_upload",
                        packageName = "com.google.android.apps.docs",
                        appName = "Google Drive",
                        title = "presentation_2026.pdf",
                        text = "Uploading... 85 MB of 120 MB • 70%",
                        timeMillis = System.currentTimeMillis(),
                        mode = IslandMode.DownloadUpload,
                        progress = 70,
                        progressMax = 100,
                        actionIntents = listOf(
                            IslandNotificationAction("Cancel", null)
                        )
                    )
                }
            }
            IslandMode.Hotspot -> IslandNotification(
                key = "demo_hotspot",
                packageName = "com.android.settings",
                appName = "Mobile Hotspot",
                title = "Mobile Hotspot",
                text = "Active • 2 devices connected",
                timeMillis = System.currentTimeMillis(),
                mode = IslandMode.Hotspot,
                actionIntents = listOf(
                    IslandNotificationAction("Hotspot Settings", null)
                )
            )
            IslandMode.Empty -> null
        }
        if (demoNotification != null) {
            postNotification(demoNotification, autoExpand = true)
        }
    }

    override fun clearTestNotifications() {
        _notifications.update { list ->
            val demoFiltered = list.filterNot { it.key.startsWith("demo_") }
            if (demoFiltered.size == list.size) {
                emptyList()
            } else {
                demoFiltered
            }
        }
    }

    private companion object {
        const val MAX_STORED_NOTIFICATIONS = 50
    }
}

sealed interface SmartIslandCommand {
    data class CancelNotification(val key: String) : SmartIslandCommand
    data class SeekTo(val packageName: String, val positionMs: Long) : SmartIslandCommand
}
