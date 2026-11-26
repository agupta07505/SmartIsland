/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AutoHideHold
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TransferState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

@Composable
internal fun AutoHideEffects(
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    /** Landscape classic: the cutout's event pulses (or waits in the gap between two pulses). */
    pulseBusy: Boolean,
    holdLayout: Boolean
) {
    // A new card, or a music card on a new track, shows the pill again, the same track reposted (re-entering the app)
    // does not
    LaunchedEffect(autoHideResetKeys(notifications), selectedIndex) {
        if (notifications.isEmpty()) return@LaunchedEffect
        ui.isAutoHidden = false
        ui.touch()
    }

    val somethingRunning = keepsPillVisible(notifications)
    val holding = AutoHideHold.holds(holdLayout, pulseBusy, ui.pillTouchActive || ui.pillOpen.active)
    val heldEndedMs = remember { LongArray(1) }

    LaunchedEffect(expanded, settings.autoHidePill, settings.autoHideTimeoutSeconds, ui.userInteractionTimestamp,
        ui.widgetIslandOpen, somethingRunning, holding) {
        // An open widget island has its own close timer
        if (expanded || ui.widgetIslandOpen || !settings.autoHidePill || somethingRunning) {
            ui.isAutoHidden = false
            return@LaunchedEffect
        }
        if (holding) {
            // A hidden pill stays as it is, its own tap reveals it
            try {
                awaitCancellation()
            } finally {
                heldEndedMs[0] = System.currentTimeMillis()
            }
        }
        delay(AutoHideHold.settleLeftMs(heldEndedMs[0], System.currentTimeMillis()))
        val timeoutMs = (settings.autoHideTimeoutSeconds.coerceAtLeast(1) * 1000L)
        delay(timeoutMs)
        ui.isAutoHidden = true
    }
}

internal fun autoHideResetKeys(notifications: List<IslandNotification>): List<String> = notifications.map { n ->
    if (n.mode == IslandMode.Music) "${n.key}|${n.title}|${n.text}" else n.key
}

internal fun keepsPillVisible(notifications: List<IslandNotification>): Boolean = notifications.any { n ->
    when (n.mode) {
        IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording ->
            !n.recordingPaused
        IslandMode.DownloadUpload -> n.transferState == TransferState.ACTIVE
        IslandMode.IncomingCall -> true
        // Playing media stays, a refresh or the next track must not restart the auto-hide
        IslandMode.Music -> n.mediaIsPlaying
        else -> false
    }
}
