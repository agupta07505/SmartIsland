/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.expanded.ActionRole
import com.agupta07505.smartisland.ui.expanded.classifyAction
import com.agupta07505.smartisland.ui.expanded.offersResume
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
    fun classifiesCommonButtonLabels() {
        assertEquals(ActionRole.Pause, classifyAction("Pause"))
        assertEquals(ActionRole.Resume, classifyAction("Resume"))
        assertEquals(ActionRole.Stop, classifyAction("Stop"))
        assertEquals(ActionRole.Lap, classifyAction("Lap"))
        assertEquals(ActionRole.Reset, classifyAction("Reset"))
        assertEquals(ActionRole.Reset, classifyAction("Cancel"))
        assertEquals(ActionRole.Pause, classifyAction("Pausa"))
    }

    @Test
    fun extendIsNotTreatedAsStop() {
        assertEquals(ActionRole.AddMinute, classifyAction("Extend +1 min"))
    }

    @Test
    fun addMinuteLabels() {
        listOf("+1:00", "+1 min", "+ 1m", "+1", "Add 1 minute", "Add 1 min", "ADD ONE MIN", "Extend 1 min", "+01:00")
            .forEach { assertEquals(it, ActionRole.AddMinute, classifyAction(it)) }
    }

    @Test
    fun addMinuteDoesNotCatchOtherButtons() {
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
