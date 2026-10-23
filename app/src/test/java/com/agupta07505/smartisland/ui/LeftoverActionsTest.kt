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
import com.agupta07505.smartisland.ui.expanded.leftoverActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.mockk.mockk

class LeftoverActionsTest {
    private val pi: PendingIntent = mockk(relaxed = true)

    private fun notif(vararg a: IslandNotificationAction) = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
        mode = IslandMode.Timer, actionIntents = a.toList()
    )

    @Test
    fun returnsOnlyRealButtonsNotAlreadyShown() {
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
