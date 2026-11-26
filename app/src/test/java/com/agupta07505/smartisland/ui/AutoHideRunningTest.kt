/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TransferState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoHideRunningTest {
    private fun card(
        mode: IslandMode,
        paused: Boolean = false,
        transfer: TransferState = TransferState.ACTIVE,
        playing: Boolean = false
    ) = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = mode, recordingPaused = paused, transferState = transfer, mediaIsPlaying = playing
    )

    @Test
    fun activeCardVisibility() {
        listOf(IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording).forEach {
            assertTrue(it.name, keepsPillVisible(listOf(card(it))))
        }

        assertTrue(keepsPillVisible(listOf(card(IslandMode.DownloadUpload))))
        listOf(TransferState.PAUSED, TransferState.WAITING, TransferState.FAILED, TransferState.COMPLETE).forEach {
            assertFalse(it.name, keepsPillVisible(listOf(card(IslandMode.DownloadUpload, transfer = it))))
        }

        assertTrue(keepsPillVisible(listOf(card(IslandMode.IncomingCall))))
        listOf(IslandMode.Notification, IslandMode.Music, IslandMode.Battery, IslandMode.Hotspot, IslandMode.Bluetooth).forEach {
            assertFalse(it.name, keepsPillVisible(listOf(card(it))))
        }
        assertFalse(keepsPillVisible(emptyList()))

        assertTrue(keepsPillVisible(listOf(card(IslandMode.Music, playing = true))))
        assertFalse(keepsPillVisible(listOf(card(IslandMode.Music, playing = false))))

        assertTrue(keepsPillVisible(listOf(card(IslandMode.Notification), card(IslandMode.Timer))))
    }

    @Test
    fun pausedCardsDoNotKeepThePill() {
        listOf(IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording).forEach {
            assertFalse(it.name, keepsPillVisible(listOf(card(it, paused = true))))
        }
    }

    @Test
    fun trackChangeResetsAutoHide() {
        val a = card(IslandMode.Music)
        assertTrue(autoHideResetKeys(listOf(a)) == autoHideResetKeys(listOf(a.copy(timeMillis = 5L, mediaPositionMs = 9L))))
        assertFalse(autoHideResetKeys(listOf(a)) == autoHideResetKeys(listOf(a.copy(title = "next"))))
    }
}
