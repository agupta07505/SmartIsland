/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The throttled requests that ask the listener service for notifications the island has not stored yet: the whole
 * shade (on expand and before a swipe) and the other cards of the same app (on expand)
 */
internal class ShadeImports(
    private val notificationRepo: INotificationRepository,
    private val notifications: StateFlow<List<IslandNotification>>,
    private val visibleNotifications: StateFlow<List<IslandNotification>>,
    private val selectedIndex: StateFlow<Int>,
    private val pulseGate: IslandPulseGate,
    private val wallClockMs: () -> Long
) {
    private var lastShadeImportMs = 0L
    private var lastExpandImportMs = 0L
    private var lastSameAppRequestPkg: String? = null
    private var lastSameAppRequestMs = 0L

    /** Before a swipe: notifications posted after the island started are already stored, so this runs rarely. */
    suspend fun importBeforeSwipe() {
        val now = wallClockMs()
        // Notifications posted after the island started are already stored
        if (now - lastShadeImportMs >= SHADE_IMPORT_INTERVAL_MS) {
            lastShadeImportMs = now
            pulseGate.suppressUntil(now + SHADE_IMPORT_PULSE_GRACE_MS)
            val before = notifications.value
            notificationRepo.sendCommand(SmartIslandCommand.ImportShadeNotifications)
            withTimeoutOrNull(SHADE_IMPORT_WAIT_MS) { notifications.first { it != before } }
            delay(40)
        }
    }

    fun requestShadeImport() {
        val now = wallClockMs()
        if (now - lastExpandImportMs < EXPAND_IMPORT_INTERVAL_MS) return
        lastExpandImportMs = now
        // Cards the import brings in are old ones from the shade, not new alerts
        pulseGate.suppressUntil(now + IMPORT_PULSE_GRACE_MS)
        notificationRepo.sendCommand(SmartIslandCommand.ImportShadeNotifications)
    }

    fun requestSameAppCards() {
        val current = visibleNotifications.value.getOrNull(selectedIndex.value) ?: return
        if (current.mode != IslandMode.Notification && current.mode != IslandMode.DownloadUpload) return
        val now = wallClockMs()
        if (current.packageName == lastSameAppRequestPkg && now - lastSameAppRequestMs < 3_000L) return
        lastSameAppRequestPkg = current.packageName
        lastSameAppRequestMs = now
        pulseGate.suppressAtLeastUntil(now + IMPORT_PULSE_GRACE_MS)
        notificationRepo.sendCommand(SmartIslandCommand.ImportSameAppNotifications(current.packageName))
    }

    private companion object {
        const val SHADE_IMPORT_INTERVAL_MS = 30_000L
        const val EXPAND_IMPORT_INTERVAL_MS = 5_000L
        const val SHADE_IMPORT_WAIT_MS = 250L
        const val SHADE_IMPORT_PULSE_GRACE_MS = 1_500L
        const val IMPORT_PULSE_GRACE_MS = 3_000L
    }
}
