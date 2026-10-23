/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeSwapStateTest {
    private fun dirty(ui: IslandInteractionState) = runBlocking {
        ui.cardPop.snapTo(0.4f)
        ui.cardLeanFromPx = 12f
        ui.cardSwapSign = -1
        ui.cardDragX = 7f
        ui.pendingCardEnterSign = 1
        ui.pendingCardFromKey = "a"
        ui.cardSwipeSnap = true
    }

    @Test
    fun aSwapCancelledHalfWayLeavesNothingBehind() = runBlocking {
        val ui = IslandInteractionState()
        dirty(ui)
        ui.cleanSwapState()
        assertEquals(1f, ui.cardPop.value, 0f)
        assertEquals(0f, ui.cardLeanFromPx, 0f)
        assertEquals(0, ui.cardSwapSign)
    }

    @Test
    fun cleaningTheSwapDoesNotTouchTheFingersOwnState() = runBlocking {
        val ui = IslandInteractionState()
        dirty(ui)
        ui.cleanSwapState()
        // A second swipe may already be under way: its drag and pending card are not the cancelled swap's.
        assertEquals(7f, ui.cardDragX, 0f)
        assertEquals(1, ui.pendingCardEnterSign)
    }

    @Test
    fun resetCardForgetsEverySidewaysMove() = runBlocking {
        val ui = IslandInteractionState()
        dirty(ui)
        ui.resetCard()
        assertEquals(1f, ui.cardPop.value, 0f)
        assertEquals(0f, ui.cardLeanFromPx, 0f)
        assertEquals(0, ui.cardSwapSign)
        assertEquals(0f, ui.cardDragX, 0f)
        assertEquals(0, ui.pendingCardEnterSign)
        assertEquals(null, ui.pendingCardFromKey)
        assertEquals(false, ui.cardSwipeSnap)
    }
}
