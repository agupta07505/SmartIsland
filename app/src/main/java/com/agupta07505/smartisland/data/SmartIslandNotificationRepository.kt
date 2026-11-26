/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TimerRings
import com.agupta07505.smartisland.model.TransferTick
import com.agupta07505.smartisland.model.TransferTickHold
import com.agupta07505.smartisland.util.OemDeviceRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmartIslandNotificationRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L }
) : INotificationRepository {
    private val _notifications = MutableStateFlow<List<IslandNotification>>(emptyList())
    override val notifications: StateFlow<List<IslandNotification>> = _notifications.asStateFlow()

    private val _autoExpandEvent = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val autoExpandEvent: SharedFlow<String> = _autoExpandEvent.asSharedFlow()

    private val _resetTimerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val resetTimerEvent: SharedFlow<Unit> = _resetTimerEvent.asSharedFlow()

    private val _commands = MutableSharedFlow<SmartIslandCommand>(extraBufferCapacity = 16)
    override val commands: SharedFlow<SmartIslandCommand> = _commands.asSharedFlow()

    private val lock = Any()
    private val tick = TransferTickHold()
    private val queuedPosts = LinkedHashMap<String, Pair<IslandNotification, Boolean>>()
    private var tickRemovalWaiting = false
    private var releaseJob: Job? = null

    /** Keys posted with auto-expand and until when the island is expected to open on them. */
    private val expectExpandUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun expectsAutoExpand(key: String): Boolean {
        val until = expectExpandUntil[key] ?: return false
        if (nowMs() < until) return true
        expectExpandUntil.remove(key, until)
        return false
    }

    override fun settleAutoExpand(key: String) {
        expectExpandUntil.remove(key)
    }

    private fun scheduleRelease() {
        if (releaseJob?.isActive == true) return
        releaseJob = scope.launch {
            while (true) {
                val left = synchronized(lock) { tick.remainingMs(nowMs()) }
                if (left <= 0L) break
                delay(left)
            }
            val removeKey: String?
            val posts: List<Pair<IslandNotification, Boolean>>
            synchronized(lock) {
                removeKey = if (tickRemovalWaiting) tick.key else null
                tickRemovalWaiting = false
                tick.clear()
                posts = queuedPosts.values.toList()
                queuedPosts.clear()
            }
            if (removeKey != null) removeNotification(removeKey)
            posts.forEach { (n, auto) -> postNotification(n, auto) }
        }
    }

    override fun postNotification(notification: IslandNotification, autoExpand: Boolean) {
        TimerRings.feed(notification)
        synchronized(lock) {
            val now = nowMs()
            if (tick.active(now) && notification.key != tick.key) {
                queuedPosts[notification.key] = notification to autoExpand
                scheduleRelease()
                return
            }
            val wasDone = _notifications.value.any { it.key == notification.key && TransferTick.isDone(it) }
            if (!tick.active(now) && TransferTick.isDone(notification) && !wasDone) {
                tick.begin(notification.key, now)
            }
        }
        var isNewNotification = false
        var timerBecameFinished = false
        var newMessage = false
        // before the card is visible to anyone, so its pulse can already see that the island is about to open on it
        if (autoExpand) {
            val expectNow = nowMs()
            expectExpandUntil.entries.removeIf { it.value < expectNow }
            expectExpandUntil[notification.key] = expectNow + EXPECT_EXPAND_MS
        }
        _notifications.update { existingNotifications ->
            val updated = existingNotifications.toMutableList()
            val index = updated.indexOfFirst { it.key == notification.key }
            isNewNotification = index < 0
            timerBecameFinished = false
            newMessage = false
            if (index >= 0) {
                val existing = updated[index]
                timerBecameFinished = notification.timerFinished && !existing.timerFinished
                // a chat posts every message under 1 key: a new message is an arrival too
                newMessage = notification.mode == IslandMode.Notification &&
                    existing.mode == IslandMode.Notification &&
                    (notification.timeMillis != existing.timeMillis || notification.title != existing.title ||
                        notification.text != existing.text)
                updated[index] = notification.copy(
                    icon = notification.icon ?: existing.icon,
                    largeIcon = notification.largeIcon ?: existing.largeIcon
                )
            } else {
                updated.add(notification)
            }
            updated.takeLast(MAX_STORED_NOTIFICATIONS)
        }
        // a timer that runs out is announced again although its key is the same
        if (autoExpand && (isNewNotification || timerBecameFinished || newMessage)) {
            _autoExpandEvent.tryEmit(notification.key)
        } else if (autoExpand) {
            // nothing new about it, the island does not open for it
            expectExpandUntil.remove(notification.key)
        }
    }

    override fun updateNotification(key: String, transform: (IslandNotification) -> IslandNotification) {
        _notifications.update { existing ->
            val index = existing.indexOfFirst { it.key == key }
            if (index < 0) return@update existing
            val old = existing[index]
            val new = transform(old)
            // Same card, nothing changed: keep the very same list so no collector wakes up
            if (new == old) return@update existing
            existing.toMutableList().also { it[index] = new }
        }
    }

    override fun removeNotification(key: String) {
        TimerRings.release(key)
        synchronized(lock) {
            if (tick.active(nowMs())) {
                if (key == tick.key) {
                    tickRemovalWaiting = true
                    scheduleRelease()
                    return
                }
                if (queuedPosts.remove(key) != null) return
            }
        }
        _notifications.update { notifications ->
            notifications.filterNot { it.key == key }
        }
    }

    /** Drops the package's ordinary alerts after the user opened the app. */
    override fun removeNotificationsForPackage(packageName: String) {
        val matching = if (OemDeviceRules.isMiuiOrHyperOs()) {
            _notifications.value.filter { notif ->
                notif.packageName == packageName &&
                    notif.mode != IslandMode.Music &&
                    notif.mode != IslandMode.Timer &&
                    notif.mode != IslandMode.Stopwatch &&
                    // The torch card belongs to the system UI package, opening one of its notifications must not end it
                    notif.mode != IslandMode.Flashlight
            }
        } else {
            _notifications.value.filter { it.packageName == packageName && it.mode == IslandMode.Notification }
        }
        synchronized(lock) { queuedPosts.values.removeAll { it.first.packageName == packageName } }
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
        TimerRings.releaseAll()
        synchronized(lock) {
            queuedPosts.clear()
            tickRemovalWaiting = false
            tick.clear()
        }
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
        /** How long a card posted with auto-expand keeps its pulse back while the island decides to open on it. */
        const val EXPECT_EXPAND_MS = 400L
    }
}

sealed interface SmartIslandCommand {
    data class CancelNotification(val key: String) : SmartIslandCommand
    data class ReplySent(val key: String) : SmartIslandCommand
    data class TimerStopped(val packageName: String) : SmartIslandCommand
    data class SeekTo(val packageName: String, val positionMs: Long) : SmartIslandCommand
    data class SkipNext(val packageName: String? = null) : SmartIslandCommand
    data class SkipPrevious(val packageName: String? = null) : SmartIslandCommand
    data class PlayPause(val packageName: String? = null) : SmartIslandCommand
    data object ImportShadeNotifications : SmartIslandCommand
    data object SyncShade : SmartIslandCommand
    data class ImportSameAppNotifications(val packageName: String) : SmartIslandCommand
}
