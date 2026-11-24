/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.app.PendingIntent
import com.agupta07505.smartisland.model.ActionRole
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.classifyAction
import com.agupta07505.smartisland.ui.expanded.MediaControls
import com.agupta07505.smartisland.ui.expanded.leftoverActions
import com.agupta07505.smartisland.ui.expanded.offersResume
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedActionRoleTest {
    private fun notif(vararg titles: String) = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = IslandMode.Timer,
        actionIntents = titles.map { IslandNotificationAction(it, null) }
    )

    @Test
    fun actionLabels() {
        val cases = listOf(
            "Pause" to ActionRole.Pause,
            "Resume" to ActionRole.Resume,
            "Stop" to ActionRole.Stop,
            "Lap" to ActionRole.Lap,
            "Reset" to ActionRole.Reset,
            "Cancel" to ActionRole.Reset,
            "Pausa" to ActionRole.Pause,
            "Extend +1 min" to ActionRole.AddMinute
        )
        for ((line, expected) in cases) {
            assertEquals(line, expected, classifyAction(line))
        }

        listOf("+1:00", "+1 min", "+ 1m", "+1", "Add 1 minute", "Add 1 min", "ADD ONE MIN", "Extend 1 min", "+01:00")
            .forEach { assertEquals(it, ActionRole.AddMinute, classifyAction(it)) }

        listOf("+10 min", "+5:00", "+1:30", "Pause", "Stop", "Reset", "Reply").forEach {
            assertTrue(it, classifyAction(it) != ActionRole.AddMinute)
        }
    }

    @Test
    fun resumeButtonMeansPaused() {
        assertTrue(notif("Resume", "Stop").offersResume())
        assertFalse(notif("Pause", "Stop").offersResume())
        assertFalse(notif().offersResume())
    }
}

class LeftoverActionsTest {
    private val pi: PendingIntent = mockk(relaxed = true)

    private fun notif(vararg a: IslandNotificationAction) = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = IslandMode.Timer, actionIntents = a.toList()
    )

    @Test
    fun onlyRealUnshownButtons() {
        val pause = IslandNotificationAction("Pause", pi)
        val stop = IslandNotificationAction("Stop", pi)
        val snooze = IslandNotificationAction("Snooze", pi)
        val noIntent = IslandNotificationAction("Info", null)
        val left = notif(pause, stop, snooze, noIntent).leftoverActions(pause, stop)
        assertEquals(listOf(snooze), left)
    }

    @Test
    fun nullNotificationHasNone() {
        assertTrue((null as IslandNotification?).leftoverActions().isEmpty())
    }
}

class MediaControlsTest {
    private val pending = mockk<PendingIntent>(relaxed = true)

    private fun card(vararg titles: String) = IslandNotification(
        key = "m", packageName = "com.music", appName = "Music", title = "Song", text = "Artist",
        mode = IslandMode.Music, timeMillis = 1L,
        actionIntents = titles.map { IslandNotificationAction(it, pending) }
    )

    @Test
    fun noPreviousNoPreviousButton() {
        val controls = MediaControls.resolve(null, card("Pause", "Next"))
        assertFalse(controls.hasPrevious)
        assertTrue(controls.hasNext)
        assertTrue(controls.hasPlayPause)
        assertTrue(controls.leftCustom.isEmpty() && controls.rightCustom.isEmpty())
    }

    @Test
    fun extraButtonsSplitAroundTransport() {
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
