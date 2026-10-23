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
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedSwipeTest {
    private fun card(key: String, pkg: String) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = 0L, mode = IslandMode.Notification
    )

    private val twoApps = listOf(card("a1", "app.one"), card("a2", "app.one"), card("b1", "app.two"))
    private val oneAppOneCard = listOf(card("a1", "app.one"))
    private val oneAppTwoCards = listOf(card("a1", "app.one"), card("a2", "app.one"))

    @Test
    fun nextAndPreviousNotificationChangeTheCardOnlyWithAnotherApp() {
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextNotification, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.PreviousNotification, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextPrevious, twoApps, 2))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextNotification, oneAppTwoCards, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextNotification, oneAppOneCard, 0))
    }

    @Test
    fun theLastCardWrapsToTheFirstApp() {
        // Index 2 is the last app; next wraps to the first, so a card does change.
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextNotification, twoApps, 2))
    }

    @Test
    fun siblingSwipesChangeTheCardOnlyWhenThereIsASibling() {
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.NextSibling, twoApps, 0))
        assertEquals(SwipeCardOutcome.Changes, predictCardChange(SwipeAction.PreviousSibling, twoApps, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextSibling, twoApps, 2))
        assertTrue(swipeIsNoOp(SwipeAction.NextSibling, twoApps, 2))
        assertFalse(swipeIsNoOp(SwipeAction.NextSibling, twoApps, 0))
    }

    @Test
    fun eventSwipesAreDecidedLaterAndOtherActionsKeepTheCard() {
        assertEquals(SwipeCardOutcome.Unknown, predictCardChange(SwipeAction.NextEvent, twoApps, 0))
        assertEquals(SwipeCardOutcome.Unknown, predictCardChange(SwipeAction.PreviousEvent, oneAppOneCard, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.DismissCurrent, twoApps, 0))
        assertEquals(SwipeCardOutcome.Stays, predictCardChange(SwipeAction.NextTrack, twoApps, 0))
        // Only sibling swipes are ever "no-ops"; a track skip with one card is a real action.
        assertFalse(swipeIsNoOp(SwipeAction.NextTrack, oneAppOneCard, 0))
        assertFalse(swipeIsNoOp(SwipeAction.NextNotification, oneAppOneCard, 0))
    }

    @Test
    fun aCardThatStaysOnlyGivesALittle() {
        assertEquals(0f, expandedCardRubberBand(0f, 2f), 0f)
        assertEquals(8f, expandedCardRubberBand(40f, 2f), 0.001f)
        assertEquals(-16f, expandedCardRubberBand(-900f, 2f), 0.001f)
        assertTrue(expandedCardRubberBand(60f, 3f) < expandedCardFollow(60f, 300f))
    }

    @Test
    fun eventSwipesWaitLongerForTheirCard() {
        assertTrue(CARD_ENTER_WAIT_EVENT_MS > CARD_ENTER_WAIT_MS)
    }
}
