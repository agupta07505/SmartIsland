/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeightSignatureTest {
    private fun card(mode: IslandMode, text: String) = IslandNotification(
        key = "k", packageName = "p", appName = "App", title = "Title", text = text, timeMillis = 0L, mode = mode
    )

    @Test
    fun aTickingLiveCardKeepsItsSignature() {
        for (mode in listOf(
            IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
            IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.DownloadUpload
        )) {
            assertEquals(mode.name, notificationHeightSignature(card(mode, "00:01")), notificationHeightSignature(card(mode, "00:02")))
        }
    }

    @Test
    fun anOrdinaryCardStillChangesItsSignatureWithItsText() {
        assertNotEquals(
            notificationHeightSignature(card(IslandMode.Notification, "short")),
            notificationHeightSignature(card(IslandMode.Notification, "a much longer message that wraps"))
        )
    }

    @Test
    fun onlyTheLiveModesAreListed() {
        assertTrue(liveCardTextDoesNotChangeHeight(IslandMode.Timer))
        assertFalse(liveCardTextDoesNotChangeHeight(IslandMode.Notification))
        assertFalse(liveCardTextDoesNotChangeHeight(IslandMode.Music))
        assertFalse(liveCardTextDoesNotChangeHeight(null))
    }
}
