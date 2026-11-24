/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonPressTest {

    @Test
    fun buttonDispatchRefusal() {
        assertNull(ButtonPress.refusal(hasIntent = true, viaView = false, liveLabels = emptyList(), title = "Pause"))
        assertNull(ButtonPress.refusal(true, true, listOf("Pause", "+1:00"), "+1:00"))
        assertEquals(ButtonPress.Refusal.ViewGone, ButtonPress.refusal(true, true, emptyList(), "Pause"))
        assertEquals(ButtonPress.Refusal.ViewGone, ButtonPress.refusal(true, true, listOf("Lap"), "Pause"))

        assertEquals(
            ButtonPress.Refusal.NoIntent,
            ButtonPress.refusal(hasIntent = false, viaView = false, liveLabels = listOf("Pause"), title = "Pause")
        )
    }

    @Test
    fun labelsAreComparedWithoutCaseOrPadding() {
        assertTrue(ButtonPress.hasLive(listOf(" Pause "), "pause"))
        assertFalse(ButtonPress.hasLive(listOf("Pause"), "Resume"))
    }
}

class ExpandedSelectionTest {
    private fun card(key: String, pkg: String = "com.chat", mode: IslandMode = IslandMode.Notification) =
        IslandNotification(key = key, packageName = pkg, appName = pkg, title = key, text = key, mode = mode, timeMillis = 1L)

    private val old = card("old")
    private val battery = card("system_battery", "com.android.systemui", IslandMode.Battery)

    @Test
    fun selectionAfterRemoval() {
        // charger card (index 1) ends while an old notification waits at index 0
        val emptyOutcome = ExpandedSelection.afterSelectedGone(listOf(old, battery), 1, listOf(old))
        assertEquals(ExpandedSelection.Outcome.Collapse, emptyOutcome)

        assertEquals(
            ExpandedSelection.Outcome.Keep,
            ExpandedSelection.afterSelectedGone(listOf(old, battery), 0, listOf(old))
        )

        assertEquals(
            ExpandedSelection.Outcome.Collapse,
            ExpandedSelection.afterSelectedGone(listOf(battery), 0, emptyList())
        )

        val replaced = card("chat-new", "com.chat")
        val replacementOutcome = ExpandedSelection.afterSelectedGone(listOf(battery, old), 1, listOf(battery, replaced))
        assertEquals(ExpandedSelection.Outcome.Select(1), replacementOutcome)

        assertEquals(ExpandedSelection.Outcome.Keep, ExpandedSelection.afterSelectedGone(listOf(old), 5, emptyList()))
    }

    @Test
    fun selectionAfterRepost() {
        assertEquals(
            ExpandedSelection.Outcome.Keep,
            ExpandedSelection.afterSelectedGone(listOf(old, battery), 1, listOf(battery))
        )

        val other = card("other-new", "com.other")
        val outcome = ExpandedSelection.afterSelectedGone(listOf(battery, old), 1, listOf(battery, other))
        assertEquals(ExpandedSelection.Outcome.Collapse, outcome)

        listOf(IslandMode.Music, IslandMode.DownloadUpload, IslandMode.IncomingCall, IslandMode.Timer).forEach { mode ->
            val closedGone = card("live", "com.live", mode)
            assertEquals(
                "mode $mode",
                ExpandedSelection.Outcome.Collapse,
                ExpandedSelection.afterSelectedGone(listOf(old, closedGone), 1, listOf(old))
            )
        }

        val endedGone = card("timer", "com.clock", IslandMode.Timer)
        val fresh = card("clock-new", "com.clock")
        assertEquals(
            ExpandedSelection.Outcome.Collapse,
            ExpandedSelection.afterSelectedGone(listOf(old, endedGone), 1, listOf(old, fresh))
        )
    }

    @Test
    fun recorderCompletion() {
        val endedRec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        // gone, with old notifications left in the list
        assertEquals(true, ExpandedSelection.recorderEnded(listOf(endedRec, old), 0, listOf(old)))
        // re-posted under the same key as the "saved" notification
        val saved = card("rec", "com.recorder", IslandMode.Notification)
        assertEquals(true, ExpandedSelection.recorderEnded(listOf(endedRec, old), 0, listOf(saved, old)))

        val runningRec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(runningRec, old), 0, listOf(runningRec, old)))
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(runningRec, old), 1, listOf(runningRec)))
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(old), 5, listOf(old)))

        val callRec = card("rec", "com.recorder", IslandMode.ScreenRecording)
        val ringing = card("call", "com.dialer", IslandMode.IncomingCall).copy(callStatusText = "Calling")
        assertEquals(false, ExpandedSelection.recorderEnded(listOf(callRec, old), 0, listOf(ringing, old)))
    }
}

class ExpandedSwipeTest {
    private fun card(key: String, pkg: String) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = 0L, mode = IslandMode.Notification
    )

    private val twoApps = listOf(card("a1", "app.one"), card("a2", "app.one"), card("b1", "app.two"))
    private val oneAppOneCard = listOf(card("a1", "app.one"))
    private val oneAppTwoCards = listOf(card("a1", "app.one"), card("a2", "app.one"))

    @Test
    fun swipeCardTargets() {
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextNotification, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.PreviousNotification, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextPrevious, twoApps, 2))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextNotification, oneAppTwoCards, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextNotification, oneAppOneCard, 0))

        // index 2 is the last app; next wraps to the first, so a card does change
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextNotification, twoApps, 2))

        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextSibling, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.PreviousSibling, twoApps, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextSibling, twoApps, 2))
        assertTrue(swipeIsNoOp(SwipeAction.NextSibling, twoApps, 2))
        assertFalse(swipeIsNoOp(SwipeAction.NextSibling, twoApps, 0))

        assertEquals(SwipeCardOutcome.Unknown, predictCardChange(SwipeAction.NextEvent, twoApps, 0))
        assertEquals(SwipeCardOutcome.Unknown, predictCardChange(SwipeAction.PreviousEvent, oneAppOneCard, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.DismissCurrent, twoApps, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextTrack, twoApps, 0))
        // only sibling swipes are ever "no-ops"; a track skip with 1 card is a real action
        assertFalse(swipeIsNoOp(SwipeAction.NextTrack, oneAppOneCard, 0))
        assertFalse(swipeIsNoOp(SwipeAction.NextNotification, oneAppOneCard, 0))
    }

}
