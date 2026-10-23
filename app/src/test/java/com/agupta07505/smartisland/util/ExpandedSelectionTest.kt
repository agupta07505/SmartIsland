/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpandedSelectionTest {
    private fun card(key: String, pkg: String = "com.chat", mode: IslandMode = IslandMode.Notification) =
        IslandNotification(key = key, packageName = pkg, appName = pkg, title = key, text = key, mode = mode, timeMillis = 1L)

    private val old = card("old")
    private val battery = card("system_battery", "com.android.systemui", IslandMode.Battery)

    @Test
    fun theShownCardGoneAndNothingReplacingItClosesTheIsland() {
        // Charger card (index 1) ends while an old notification waits at index 0.
        val outcome = ExpandedSelection.afterSelectedGone(listOf(old, battery), 1, listOf(old))
        assertEquals(ExpandedSelection.Outcome.Collapse, outcome)
    }

    @Test
    fun theShownCardStillThereChangesNothing() {
        assertEquals(
            ExpandedSelection.Outcome.Keep,
            ExpandedSelection.afterSelectedGone(listOf(old, battery), 1, listOf(battery))
        )
    }

    @Test
    fun anotherCardGoingAwayChangesNothing() {
        assertEquals(
            ExpandedSelection.Outcome.Keep,
            ExpandedSelection.afterSelectedGone(listOf(old, battery), 0, listOf(old))
        )
    }

    @Test
    fun theLastCardGoingAwayClosesTheIsland() {
        assertEquals(
            ExpandedSelection.Outcome.Collapse,
            ExpandedSelection.afterSelectedGone(listOf(battery), 0, emptyList())
        )
    }

    @Test
    fun aNewCardFromTheSameAppTakesTheGoneCardsPlace() {
        val replaced = card("chat-new", "com.chat")
        val outcome = ExpandedSelection.afterSelectedGone(listOf(battery, old), 1, listOf(battery, replaced))
        assertEquals(ExpandedSelection.Outcome.Select(1), outcome)
    }

    @Test
    fun aNewCardFromAnotherAppDoesNotTakeThePlace() {
        val other = card("other-new", "com.other")
        val outcome = ExpandedSelection.afterSelectedGone(listOf(battery, old), 1, listOf(battery, other))
        assertEquals(ExpandedSelection.Outcome.Collapse, outcome)
    }

    @Test
    fun musicTransfersAndEndedCallsCloseTheIsland() {
        listOf(IslandMode.Music, IslandMode.DownloadUpload, IslandMode.IncomingCall, IslandMode.Timer).forEach { mode ->
            val gone = card("live", "com.live", mode)
            assertEquals(
                "mode $mode",
                ExpandedSelection.Outcome.Collapse,
                ExpandedSelection.afterSelectedGone(listOf(old, gone), 1, listOf(old))
            )
        }
    }

    @Test
    fun aLiveEventThatEndedIsNotReplacedByTheSameAppsNewCard() {
        val gone = card("timer", "com.clock", IslandMode.Timer)
        val fresh = card("clock-new", "com.clock")
        assertEquals(
            ExpandedSelection.Outcome.Collapse,
            ExpandedSelection.afterSelectedGone(listOf(old, gone), 1, listOf(old, fresh))
        )
    }

    @Test
    fun anOutOfRangeSelectionIsLeftAlone() {
        assertEquals(ExpandedSelection.Outcome.Keep, ExpandedSelection.afterSelectedGone(listOf(old), 5, emptyList()))
    }

    @Test
    fun theEndedRecorderClosesTheIslandWhateverWaits() {
        val rec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        // Gone, with old notifications left in the list.
        assertEquals(true, ExpandedSelection.recorderEnded(listOf(rec, old), 0, listOf(old)))
        // Re-posted under the same key as the "saved" notification.
        val saved = card("rec", "com.recorder", IslandMode.Notification)
        assertEquals(true, ExpandedSelection.recorderEnded(listOf(rec, old), 0, listOf(saved, old)))
    }

    @Test
    fun aRunningRecorderOrAnotherShownCardIsNotARecorderEnd() {
        val rec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(rec, old), 0, listOf(rec, old)))
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(rec, old), 1, listOf(rec)))
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(old), 5, listOf(old)))
    }

    @Test
    fun aCallIsTheExceptionToTheRecorderClose() {
        val rec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        val ringing = card("call", "com.dialer", IslandMode.IncomingCall).copy(callStatusText = "Calling")
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(rec, old), 0, listOf(ringing, old)))
    }
}
