/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface INotificationRepository {
    val notifications: StateFlow<List<IslandNotification>>
    val autoExpandEvent: SharedFlow<String>
    val resetTimerEvent: SharedFlow<Unit>
    val commands: SharedFlow<SmartIslandCommand>

    fun postNotification(notification: IslandNotification, autoExpand: Boolean = false)
    fun removeNotification(key: String)
    fun resetTimer()
    fun sendCommand(command: SmartIslandCommand)
    fun showDemo(mode: IslandMode)
}
