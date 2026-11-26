/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface INotificationRepository {
    val notifications: StateFlow<List<IslandNotification>>
    val autoExpandEvent: SharedFlow<String>
    val resetTimerEvent: SharedFlow<Unit>
    val commands: SharedFlow<SmartIslandCommand>

    fun postNotification(notification: IslandNotification, autoExpand: Boolean = false)

    /**
     * True for a moment after [key] was posted with auto-expand: the island is about to open on it, so its pulse waits
     * for that decision instead of starting and being cut a moment later
     */
    fun expectsAutoExpand(key: String): Boolean = false

    /** The island has decided about [key], whether it opened or not: its pulse no longer waits. */
    fun settleAutoExpand(key: String) {}

    /** Changes 1 stored card in place (never adds one, never auto-expands). */
    fun updateNotification(key: String, transform: (IslandNotification) -> IslandNotification) {}
    fun removeNotification(key: String)
    fun removeNotificationsForPackage(packageName: String)
    fun removeAllNotifications()
    fun resetTimer()
    fun sendCommand(command: SmartIslandCommand)
}
