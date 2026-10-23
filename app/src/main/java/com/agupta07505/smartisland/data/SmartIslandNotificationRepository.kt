/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
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

    override fun updateNotification(key: String, transform: (IslandNotification) -> IslandNotification) {
        _notifications.update { existing ->
            val index = existing.indexOfFirst { it.key == key }
            if (index < 0) return@update existing
            val old = existing[index]
            val new = transform(old)
            // Same card, nothing changed: keep the very same list so no collector wakes up.
            if (new == old) return@update existing
            existing.toMutableList().also { it[index] = new }
        }
    }

    override fun removeNotification(key: String) {
        _notifications.update { notifications ->
            notifications.filterNot { it.key == key }
        }
    }

    /**
     * Drops the package's ordinary alerts after the user opened the app.
     *
     * Only [IslandMode.Notification] cards are touched. Ongoing activities (timer, stopwatch,
     * recorders, calls, navigation, downloads, live activities, music) keep running after the app
     * opens, and their notification buttons must stay alive, so they are neither removed from the
     * island nor cancelled from the system shade.
     */
    override fun removeNotificationsForPackage(packageName: String) {
        val matching = if (com.agupta07505.smartisland.util.OemDeviceRules.isMiuiOrHyperOs()) {
            _notifications.value.filter { notif ->
                notif.packageName == packageName &&
                    notif.mode != IslandMode.Music &&
                    notif.mode != IslandMode.Timer &&
                    notif.mode != IslandMode.Stopwatch
            }
        } else {
            _notifications.value.filter { it.packageName == packageName && it.mode == IslandMode.Notification }
        }
        if (matching.isEmpty()) return

        val keysToRemove = matching.mapTo(mutableSetOf()) { it.key }
        _notifications.update { list ->
            list.filterNot { it.key in keysToRemove }
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

    private companion object {
        const val MAX_STORED_NOTIFICATIONS = 50
    }
}

sealed interface SmartIslandCommand {
    data class CancelNotification(val key: String) : SmartIslandCommand
    /** A reply was sent from the island: the app cancelling or re-posting that card right after must not close it. */
    data class ReplySent(val key: String) : SmartIslandCommand
    /** A timer was stopped from the island: what was remembered for that app's paused timer is no longer valid. */
    data class TimerStopped(val packageName: String) : SmartIslandCommand
    data class SeekTo(val packageName: String, val positionMs: Long) : SmartIslandCommand
    data class SkipNext(val packageName: String? = null) : SmartIslandCommand
    data class SkipPrevious(val packageName: String? = null) : SmartIslandCommand
    data class PlayPause(val packageName: String? = null) : SmartIslandCommand
    /** Pull loud (non-silent) notifications that sit in the system shade into the island. */
    data object ImportShadeNotifications : SmartIslandCommand
    /** The overlay started: import what the shade already holds (persistent cards and loud notifications) that the island misses. */
    data object SyncShade : SmartIslandCommand
    /**
     * The island was expanded on a card from [packageName]: pull that app's other cards from the shade (including
     * finished downloads) so the secondary cutout can show them.
     */
    data class ImportSameAppNotifications(val packageName: String) : SmartIslandCommand
}
