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

class SmartIslandNotificationRepository {
    private val _notifications = MutableStateFlow<List<IslandNotification>>(emptyList())
    val notifications: StateFlow<List<IslandNotification>> = _notifications.asStateFlow()

    private val _autoExpandEvent = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val autoExpandEvent: SharedFlow<String> = _autoExpandEvent.asSharedFlow()

    private val _resetTimerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val resetTimerEvent: SharedFlow<Unit> = _resetTimerEvent.asSharedFlow()

    private val _commands = MutableSharedFlow<SmartIslandCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<SmartIslandCommand> = _commands.asSharedFlow()

    fun postNotification(notification: IslandNotification, autoExpand: Boolean = false) {
        val current = _notifications.value.toMutableList()
        val index = current.indexOfFirst { it.key == notification.key }
        if (index >= 0) {
            current[index] = notification
        } else {
            current.add(notification)
        }
        _notifications.value = current
        if (autoExpand) {
            _autoExpandEvent.tryEmit(notification.key)
        }
    }

    fun removeNotification(key: String) {
        val current = _notifications.value.filter { it.key != key }
        _notifications.value = current
    }

    fun resetTimer() {
        _resetTimerEvent.tryEmit(Unit)
    }

    fun sendCommand(command: SmartIslandCommand) {
        _commands.tryEmit(command)
    }

    fun showDemo(mode: IslandMode) {
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
            IslandMode.Empty -> null
        }
        if (demoNotification != null) {
            postNotification(demoNotification, autoExpand = true)
        }
    }
}

sealed interface SmartIslandCommand {
    data class CancelNotification(val key: String) : SmartIslandCommand
    data class SeekTo(val packageName: String, val positionMs: Long) : SmartIslandCommand
}
