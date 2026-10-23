/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.app.PendingIntent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.expanded.MediaControls
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaControlsTest {
    private val pending = mockk<PendingIntent>(relaxed = true)

    private fun card(vararg titles: String) = IslandNotification(
        key = "m", packageName = "com.music", appName = "Music", title = "Song", text = "Artist",
        mode = IslandMode.Music, timeMillis = 1L,
        actionIntents = titles.map { IslandNotificationAction(it, pending) }
    )

    @Test
    fun notificationWithoutPreviousShowsNoPreviousButton() {
        val controls = MediaControls.resolve(null, card("Pause", "Next"))
        assertFalse(controls.hasPrevious)
        assertTrue(controls.hasNext)
        assertTrue(controls.hasPlayPause)
        assertTrue(controls.leftCustom.isEmpty() && controls.rightCustom.isEmpty())
    }

    @Test
    fun extraNotificationButtonsBecomeCustomButtonsSplitAroundTheTransport() {
        val controls = MediaControls.resolve(null, card("Like", "Previous", "Pause", "Next", "Shuffle", "Cast"))
        assertTrue(controls.hasPrevious && controls.hasNext && controls.hasPlayPause)
        assertEquals(listOf("Like", "Shuffle"), controls.leftCustom.map { it.label })
        assertEquals(listOf("Cast"), controls.rightCustom.map { it.label })
    }

    @Test
    fun unreadableCardKeepsBasicTransport() {
        val controls = MediaControls.resolve(null, card())
        assertTrue(controls.hasPrevious && controls.hasPlayPause && controls.hasNext)
    }

    @Test
    fun buttonsWithoutAnIntentAreIgnored() {
        val n = card().copy(actionIntents = listOf(IslandNotificationAction("Like", null)))
        val controls = MediaControls.resolve(null, n)
        assertTrue(controls.leftCustom.isEmpty() && controls.rightCustom.isEmpty())
    }
}
