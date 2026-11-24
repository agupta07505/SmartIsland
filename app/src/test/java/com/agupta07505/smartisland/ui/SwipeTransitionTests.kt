/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.TapSwap
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSwapFadeTest {
    @Test
    fun onlyAnUnswipedCardReplacementFades() {
        assertTrue(cardSwapFades("a", "b", swipeEntering = false))
        assertFalse(cardSwapFades("a", "b", swipeEntering = true))
        assertFalse(cardSwapFades("a", "a", swipeEntering = false))
        assertFalse(cardSwapFades(null, "b", swipeEntering = false))
        assertFalse(cardSwapFades("a", null, swipeEntering = false))
    }

    @Test
    fun swapFadeBounds() {
        for (p in listOf(0f, 0.25f, 0.5f, 1f)) {
            assertEquals(1f, swapIncomingAlpha(p) + swapOutgoingAlpha(p), 0.001f)
        }

        assertEquals(0f, swapIncomingAlpha(-0.3f), 0.001f)
        assertEquals(0f, swapOutgoingAlpha(1.4f), 0.001f)
    }
}

class PillContentSlideTest {
    @Test
    fun slideRoomShareOfPill() {
        assertEquals(44.8f, pillContentMaxSlideDp(112f), 0.01f)
        assertEquals(56f, pillContentMaxSlideDp(400f), 0f)
        assertEquals(8f, pillContentMaxSlideDp(10f), 0f)
    }
}

class PillLeanScreenTest {
    // side camera on the left: the pill rests 13 dp from the screen edge
    private val left = 13f
    private val width = 109f
    private val screen = 393f

    @Test
    fun screenLeanLimits() {
        val leftOut = pillLeanWithinScreenDp(-24f, left, width, screen)
        assertEquals(-(left - LEAN_EDGE_MARGIN_DP), leftOut, 0.001f)

        assertEquals(left - LEAN_EDGE_MARGIN_DP, pillLeanWithinScreenDp(24f, left, width, screen), 0.001f)
        assertEquals(-5f, pillLeanWithinScreenDp(-5f, left, width, screen), 0.001f)
        assertEquals(0f, pillLeanWithinScreenDp(-10f, 2f, width, screen), 0.001f)
        assertEquals(24f, pillLeanWithinScreenDp(24f, 300f, width, 0f), 0.001f)
        assertEquals(-24f, pillLeanWithinScreenDp(-24f, 300f, width, 0f), 0.001f)

        val leftRight = screen - width - 13f
        val rightOut = pillLeanWithinScreenDp(24f, leftRight, width, screen)
        assertEquals(13f - LEAN_EDGE_MARGIN_DP, rightOut, 0.001f)
        assertEquals(-rightOut, pillLeanWithinScreenDp(-24f, leftRight, width, screen), 0.001f)
    }

}

class SecondaryBehindPillTest {
    private val screen = 393f

    @Test
    fun collapsedBubbleLayer() {
        Assert.assertTrue(secondaryBehindPill(bubbleMerge = false, expanded = false, notchMode = false))

        Assert.assertFalse(secondaryBehindPill(bubbleMerge = true, expanded = false, notchMode = false))
        Assert.assertFalse(secondaryBehindPill(bubbleMerge = false, expanded = true, notchMode = false))
        Assert.assertFalse(secondaryBehindPill(bubbleMerge = false, expanded = false, notchMode = true))
    }

    @Test
    fun bubbleFollowLimits() {
        assertEquals(-20f, bubbleFollowLeanDp(-20f, 130f, 34f, screen), 0.001f)
        assertEquals(20f, bubbleFollowLeanDp(20f, 130f, 34f, screen), 0.001f)
        assertEquals(0f, bubbleFollowLeanDp(0f, 130f, 34f, screen), 0f)

        assertEquals(-(10f - LEAN_EDGE_MARGIN_DP), bubbleFollowLeanDp(-24f, 10f, 34f, screen), 0.001f)
        val nearEdgeRight = screen - 10f - 34f
        assertEquals(10f - LEAN_EDGE_MARGIN_DP, bubbleFollowLeanDp(24f, nearEdgeRight, 34f, screen), 0.001f)

        // the pill is near the left edge, the bubble is not: a lean to the right is the bubble's own room
        assertEquals(24f, bubbleFollowLeanDp(24f, 200f, 34f, screen), 0.001f)
    }


    @Test
    fun earFollowLimits() {
        // 2x density: 24 dp of lean = 48 px, either way
        assertEquals(-48f, edgeEarFollowPx(-48f, 300f, 34f, 800f, 2f), 0.001f)
        assertEquals(48f, edgeEarFollowPx(48f, 300f, 34f, 800f, 2f), 0.001f)

        assertEquals(0f, edgeEarFollowPx(0f, 300f, 34f, 800f, 2f), 0f)
        assertEquals(0f, edgeEarFollowPx(48f, 300f, 34f, 800f, 0f), 0f)

        assertEquals(-(10f - LEAN_EDGE_MARGIN_DP) * 2f, edgeEarFollowPx(-48f, 10f, 34f, 800f, 2f), 0.001f)
        val low = 800f - 10f - 34f
        assertEquals((10f - LEAN_EDGE_MARGIN_DP) * 2f, edgeEarFollowPx(48f, low, 34f, 800f, 2f), 0.001f)

        // the pill is high on the screen, the ear is not: a lean up is the ear's own room
        assertEquals(-48f, edgeEarFollowPx(-48f, 500f, 34f, 800f, 2f), 0.001f)
    }
}

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
    fun midwayCancelLeavesNothing() = runBlocking {
        val ui = IslandInteractionState()
        dirty(ui)
        ui.cleanSwapState("test")
        assertEquals(1f, ui.cardPop.value, 0f)
        assertEquals(0f, ui.cardLeanFromPx, 0f)
        assertEquals(0, ui.cardSwapSign)
    }

    @Test
    fun cleanSwapSparesFingerState() = runBlocking {
        val ui = IslandInteractionState()
        dirty(ui)
        ui.cleanSwapState("test")
        // A second swipe may already be under way: its drag and pending card are not the cancelled swap's
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

    @Test
    fun tapSwapEndRestoresShell() {
        val ui = IslandInteractionState()
        val guard = Job()
        ui.beginSwapPhase()
        ui.tapSwapCutoutId = TapSwap.CUTOUT_SECONDARY
        ui.tapSwapGuardJob = guard
        ui.endTapSwap("test")
        assertEquals(false, ui.tapSwapActive)
        assertEquals(true, guard.isCancelled)
        assertEquals(null, ui.tapSwapGuardJob)
        assertEquals(1f, ui.cutoutSwapAlpha(TapSwap.CUTOUT_SECONDARY), 0f)
        assertEquals(1f, ui.tapSwapP(), 0f)
    }

    @Test
    fun finishedSwapStopsItsGuard() = runBlocking {
        val ui = IslandInteractionState()
        val guard = Job()
        ui.beginSwapPhase()
        ui.tapSwapGuardJob = guard
        ui.cleanSwapState("test")
        assertEquals(false, ui.tapSwapActive)
        assertEquals(true, guard.isCancelled)
    }

    @Test
    fun resetCardEndsATapSwap() = runBlocking {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.resetCard()
        assertEquals(false, ui.tapSwapActive)
    }

    @Test
    fun selectionFollowsTheSwapsDirection() {
        val ui = IslandInteractionState()
        val calls = mutableListOf<String>()
        ui.beginSwapPhase()
        ui.tapSwapSelectOld = { calls += "old" }
        ui.tapSwapSelectNew = { calls += "new" }
        ui.swap.setReverse(true)
        // the gap between the 2 fades (TapSwap.OUTGOING_FADE_END..INCOMING_FADE_START = 0.3..0.4) is where the selection switches
        ui.syncTapSwapSelection(0.6f)
        ui.syncTapSwapSelection(0.35f)
        ui.syncTapSwapSelection(0.32f)
        ui.swap.setReverse(false)
        ui.syncTapSwapSelection(0.32f)
        assertEquals(listOf("old", "new"), calls)
        assertEquals(false, ui.tapSwapBack)
    }

    @Test
    fun endingASwapForgetsItsDirection() {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.swap.setReverse(true)
        ui.swap.setBack(true)
        ui.swap.settle()
        assertEquals(true, ui.tapSwapSettling)
        ui.tapSwapSelectOld = {}
        ui.tapSwapSelectNew = {}
        ui.endTapSwap("test")
        assertEquals(false, ui.tapSwapReverse)
        assertEquals(false, ui.tapSwapBack)
        assertEquals(false, ui.tapSwapSettling)
        assertEquals(null, ui.tapSwapSelectOld)
        assertEquals(null, ui.tapSwapSelectNew)
    }

    @Test
    fun viewModelToldSwapStartEnd() {
        val ui = IslandInteractionState()
        val told = mutableListOf<Boolean>()
        ui.onSwapBusy = { told += it }
        ui.beginSwapPhase()
        assertEquals(true, ui.swapBusy)
        ui.beginSwapPhase()
        ui.endTapSwap("test")
        ui.endTapSwap("test")
        assertEquals(false, ui.swapBusy)
        assertEquals(listOf(true, false), told)
    }

    @Test
    fun idleAbortRestoresSwap() = runBlocking {
        val ui = IslandInteractionState()
        val told = mutableListOf<Boolean>()
        ui.onSwapBusy = { told += it }
        ui.beginSwapPhase()
        ui.cardPop.snapTo(0.3f)
        ui.abortTapSwap("test")
        assertEquals(false, ui.tapSwapActive)
        assertEquals(false, ui.swapBusy)
        assertEquals(1f, ui.cardPop.value, 0f)
        assertEquals(listOf(true, false), told)
    }

    @Test
    fun consumeReversingTouch(): Unit = runBlocking {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.cardPop.snapTo(0.4f)
        ui.swapTouch.down(flipped = true)
        assertEquals(true, flipTapSwap(ui, this, true))
        assertEquals(false, ui.tapSwapReverse)
        // the click was counted once: the next one is a tap of its own
        assertEquals(true, flipTapSwap(ui, this, true))
        assertEquals(true, ui.tapSwapReverse)
        ui.tapSwapGuardJob?.cancel()
    }

    @Test
    fun aSettlingSwapTakesNoFlip() = runBlocking {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.swap.run()
        ui.swap.settle()
        ui.cardPop.snapTo(0.4f)
        assertEquals(false, flipTapSwap(ui, this, false))
        assertEquals(false, ui.tapSwapReverse)
    }

    @Test
    fun anAbortingSwapTakesNoFlip() = runBlocking {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.swap.run()
        ui.swap.abort()
        ui.cardPop.snapTo(0.4f)
        assertEquals(false, flipTapSwap(ui, this, false))
        assertEquals(false, ui.tapSwapReverse)
    }

    @Test
    fun secondTouchAfterFlip(): Unit = runBlocking {
        val ui = IslandInteractionState()
        ui.beginSwapPhase()
        ui.cardPop.snapTo(0.4f)
        // first touch flipped, its click never came; the second touch goes down at once and flips too
        ui.swapTouch.down(flipped = true)
        ui.swapTouch.down(flipped = false)
        assertEquals(true, flipTapSwap(ui, this, true))
        assertEquals(true, ui.tapSwapReverse)
        ui.tapSwapGuardJob?.cancel()
    }
}
