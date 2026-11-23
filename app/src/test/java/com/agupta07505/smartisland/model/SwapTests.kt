/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.SwapCoordinator.Flags
import com.agupta07505.smartisland.model.SwapCoordinator.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapCoordinatorTest {
    @Test
    fun aSwapGoesThroughItsPhasesInOrder() {
        val swap = SwapCoordinator()
        assertEquals(Phase.Idle, swap.phase)
        assertFalse(swap.busy)
        assertTrue(swap.begin())
        assertEquals(Phase.Preparing, swap.phase)
        swap.run()
        assertEquals(Phase.Running, swap.phase)
        swap.settle()
        assertEquals(Phase.Settling, swap.phase)
        assertTrue(swap.busy)
        swap.end()
        assertEquals(Phase.Idle, swap.phase)
        assertFalse(swap.busy)
    }

    @Test
    fun secondSwapWaitsWhileBusy() {
        val swap = SwapCoordinator()
        assertTrue(swap.begin())
        assertFalse(swap.begin())
        swap.run()
        assertFalse(swap.begin())
        swap.end()
        assertTrue(swap.begin())
    }

    @Test
    fun phasesOutOfOrderAreIgnored() {
        val swap = SwapCoordinator()
        swap.run()
        swap.settle()
        swap.abort()
        assertEquals(Phase.Idle, swap.phase)
        swap.begin()
        swap.settle()
        assertEquals(Phase.Settling, swap.phase)
        swap.run()
        assertEquals(Phase.Settling, swap.phase)
    }

    @Test
    fun abortFromAnyBusyPhaseEndsIdle() {
        for (until in 0..2) {
            val swap = SwapCoordinator()
            swap.begin()
            if (until >= 1) swap.run()
            if (until >= 2) swap.settle()
            swap.abort()
            assertEquals(Phase.Aborting, swap.phase)
            assertTrue(swap.busy)
            swap.end()
            assertFalse(swap.busy)
        }
    }

    @Test
    fun onlySettlingOrAbortingRefusesFlip() {
        val swap = SwapCoordinator()
        assertFalse(swap.refusesFlip)
        swap.begin()
        assertFalse(swap.refusesFlip)
        swap.run()
        assertFalse(swap.refusesFlip)
        swap.settle()
        assertTrue(swap.refusesFlip)
        swap.abort()
        assertTrue(swap.refusesFlip)
        swap.end()
        assertFalse(swap.refusesFlip)
        swap.begin()
        swap.abort()
        assertTrue(swap.refusesFlip)
    }

    @Test
    fun anAbortedSwapStaysAbortingWhenItsLoopReachesItsEnd() {
        val swap = SwapCoordinator()
        swap.begin()
        swap.run()
        swap.abort()
        swap.settle()
        assertEquals(Phase.Aborting, swap.phase)
    }

    @Test
    fun flagsFollowTheLayoutWhileIdle() {
        val swap = SwapCoordinator()
        val a = Flags(tapSwapAllowed = true, twoIslandsDrawn = false)
        val b = Flags(tapSwapAllowed = false, twoIslandsDrawn = true)
        assertEquals(a, swap.flags(a, busy = false))
        assertEquals(b, swap.flags(b, busy = false))
    }

    @Test
    fun flagsStayAsTheyWereWhenTheSwapBeganUntilItEnds() {
        val swap = SwapCoordinator()
        val before = Flags(tapSwapAllowed = true, twoIslandsDrawn = false)
        val during = Flags(tapSwapAllowed = false, twoIslandsDrawn = true)
        swap.flags(before, busy = false)
        swap.begin()
        assertEquals(before, swap.flags(during, busy = true))
        assertEquals(before, swap.flags(during, busy = true))
        swap.end()
        assertEquals(during, swap.flags(during, busy = false))
    }
}

class SwapPickerPolicyTest {
    private fun resolve(
        open: Boolean = true,
        anchor: String? = "a",
        shown: String? = "a",
        siblings: Boolean = true,
        busy: Boolean = false,
        settledNow: Boolean = false
    ) = SwapPickerPolicy.resolve(open, anchor, shown, siblings, busy, settledNow)

    @Test
    fun closedPickerStaysClosed() {
        assertEquals(SwapPickerPolicy.Result(false, null), resolve(open = false))
    }

    @Test
    fun pickerAfterSwap() {
        assertFalse(resolve(shown = "b").open)
        assertTrue(resolve(shown = "b", siblings = false, busy = true).open)
        assertFalse(resolve(shown = "b", siblings = false, settledNow = true).open)
    }

    @Test
    fun pickerAnchorDuringSwap() {
        assertEquals(SwapPickerPolicy.Result(true, "a"), resolve(shown = "b", busy = true))
        assertEquals(SwapPickerPolicy.Result(true, "a"), resolve(shown = "a", busy = true))
    }

    @Test
    fun pickerAfterSettle() {
        assertEquals(SwapPickerPolicy.Result(true, "b"), resolve(shown = "b", settledNow = true))
    }

    @Test
    fun aSwapThatEndsOnTheSameCardLeavesThePickerAlone() {
        assertEquals(SwapPickerPolicy.Result(true, "a"), resolve(shown = "a", settledNow = true))
    }

    @Test
    fun midSessionPickerAnchorsToCard() {
        assertEquals(SwapPickerPolicy.Result(true, "a"), resolve(anchor = null, shown = "a"))
    }
}

class SwapAbortTest {
    @Test
    fun interruptedSwapDirection() {
        assertTrue(SwapAbort.settlesBack(0f))
        assertTrue(SwapAbort.settlesBack(0.49f))
        assertFalse(SwapAbort.settlesBack(0.5f))
        assertFalse(SwapAbort.settlesBack(1f))
    }
}

class SwapPickerOpenTest {
    @Test
    fun deferredPickerEligibility() {
        assertTrue(SwapPickerPolicy.opensAfterSwap(pending = true, settledNow = true, expanded = true, hasSiblings = true))
        assertFalse(SwapPickerPolicy.opensAfterSwap(pending = true, settledNow = false, expanded = true, hasSiblings = true))
        assertFalse(SwapPickerPolicy.opensAfterSwap(pending = false, settledNow = true, expanded = true, hasSiblings = true))
        assertFalse(SwapPickerPolicy.opensAfterSwap(pending = true, settledNow = true, expanded = true, hasSiblings = false))
        assertFalse(SwapPickerPolicy.opensAfterSwap(pending = true, settledNow = true, expanded = false, hasSiblings = true))
    }

    @Test
    fun directionEndsWithSwap() {
        val swap = SwapCoordinator()
        swap.turnAround()
        assertFalse(swap.reverse)
        swap.begin()
        swap.turnAround()
        assertTrue(swap.reverse)
        swap.turnAround()
        assertFalse(swap.reverse)
        swap.setReverse(true)
        swap.end()
        assertFalse(swap.reverse)
        swap.begin()
        assertFalse(swap.reverse)
    }

    @Test
    fun backFlagTransitions() {
        val told = mutableListOf<Boolean>()
        val swap = SwapCoordinator { told += it }
        swap.setBack(true)
        assertFalse(swap.back)
        swap.begin()
        swap.setBack(false)
        swap.setBack(true)
        swap.setBack(true)
        assertTrue(swap.back)
        swap.settle()
        swap.setBack(false)
        swap.setBack(true)
        swap.end()
        assertFalse(swap.back)
        swap.end()
        assertEquals(listOf(true, false, true, false), told)
    }
}

class SwapTouchGuardTest {
    @Test
    fun flipTouchClickIsSameTouch() {
        val g = SwapTouchGuard()
        g.down(flipped = true)
        assertTrue(g.claimClick())
    }

    @Test
    fun aClickIsCountedOnce() {
        val g = SwapTouchGuard()
        g.down(flipped = true)
        assertTrue(g.claimClick())
        assertFalse(g.claimClick())
    }

    @Test
    fun aClickWithoutAFlipIsANewTap() {
        val g = SwapTouchGuard()
        g.down(flipped = false)
        assertFalse(g.claimClick())
    }

    @Test
    fun aFastSecondTouchIsNeverTakenForTheFirstOnesClick() {
        val g = SwapTouchGuard()
        g.down(flipped = true)
        // the first touch's click never came, the next touch goes down at once
        g.down(flipped = true)
        assertTrue(g.claimClick())
        g.down(flipped = false)
        assertFalse(g.claimClick())
    }

    @Test
    fun aClickWithNoTouchBeforeItIsATap() {
        assertFalse(SwapTouchGuard().claimClick())
    }

    @Test
    fun resetDropsAnArmedGuard() {
        val g = SwapTouchGuard()
        g.down(flipped = true)
        g.reset()
        assertFalse(g.claimClick())
    }

    @Test
    fun postSwapTouchLock() {
        assertTrue(SwapTouchGuard.cardLocked(swapActive = true, nowMs = 1_000, lockUntilMs = 0))

        val endedAt = 5_000L
        val coolingDownUntil = endedAt + SwapTouchGuard.POST_SWAP_LOCK_MS
        assertTrue(SwapTouchGuard.cardLocked(swapActive = false, nowMs = endedAt, lockUntilMs = coolingDownUntil))
        assertTrue(SwapTouchGuard.cardLocked(swapActive = false, nowMs = coolingDownUntil - 1, lockUntilMs = coolingDownUntil))

        val unlockedUntil = 5_000L + SwapTouchGuard.POST_SWAP_LOCK_MS
        assertFalse(SwapTouchGuard.cardLocked(swapActive = false, nowMs = unlockedUntil, lockUntilMs = unlockedUntil))
        assertFalse(SwapTouchGuard.cardLocked(swapActive = false, nowMs = unlockedUntil + 1_000, lockUntilMs = unlockedUntil))

        assertFalse(SwapTouchGuard.cardLocked(swapActive = false, nowMs = 10, lockUntilMs = 0))
    }
}

class SwipeGhostTest {
    @Test
    fun parkedSwipeLayer() {
        assertEquals(0, SwipeGhost.layerToPark(0, arrayOf("rec", "rec"), "rec"))
        assertEquals(1, SwipeGhost.layerToPark(1, arrayOf("rec", "rec"), "rec"))

        // the bug: layer 1 (drawn last) holds the media card; the Recording card is in layer 0
        assertEquals(0, SwipeGhost.layerToPark(1, arrayOf("rec", "media"), "rec"))
        assertEquals(1, SwipeGhost.layerToPark(0, arrayOf("media", "rec"), "rec"))

        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf("media", "media"), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf(null, null), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(-1, arrayOf("rec", "rec"), "rec"))
        assertEquals(-1, SwipeGhost.layerToPark(0, arrayOf("rec", "rec"), null))
    }
}

class TapSwapTest {
    private fun ok(
        expanded: Boolean = true, key: String? = "a", from: Int = 0, to: Int = 1, count: Int = 2,
        swapping: Boolean = false, drag: Float = 0f, running: Boolean = false
    ) = TapSwap.eligible(expanded, key, from, to, count, swapping, drag, running)

    @Test
    fun swapsOnlyWhenExpandedAndIdle() {
        assertTrue(ok())
        assertFalse(ok(expanded = false))
        assertFalse(ok(key = null))
        assertFalse(ok(to = 0))
        assertFalse(ok(to = 2))
        assertFalse(ok(swapping = true))
        assertFalse(ok(drag = 4f))
        assertFalse(ok(running = true))
    }

    @Test
    fun minimumShellScale() {
        // a cutout bigger than the floor: the exact cutout size
        assertEquals(0.5f, TapSwap.minScale(150f, 300f), 0.0001f)
        // a small cutout: its own size, no floor above it
        assertEquals(0.1f, TapSwap.minScale(30f, 300f), 0.0001f)
        // only a degenerate size is guarded
        assertEquals(TapSwap.MIN_SHELL_SCALE, TapSwap.minScale(1f, 300f), 0.0001f)
        assertEquals(1f, TapSwap.minScale(400f, 300f), 0.0001f)
        assertEquals(1f, TapSwap.minScale(10f, 0f), 0.0001f)

        for ((cutout, shell) in listOf(30f to 300f, 150f to 300f, 400f to 300f)) {
            assertEquals(
                shell * TwoShellSwap.scaleAt(TapSwap.minScale(cutout, shell), 0f),
                TapSwap.restExtent(cutout, shell), 0.0001f
            )
        }
    }

    @Test
    fun contentHiddenWhileShellOnCutout() {
        assertEquals(1f, TapSwap.outgoingContentAlpha(0f), 0.0001f)
        assertEquals(0f, TapSwap.outgoingContentAlpha(0.35f), 0.0001f)
        assertEquals(0f, TapSwap.incomingContentAlpha(0.35f), 0.0001f)
        assertEquals(1f, TapSwap.incomingContentAlpha(1f), 0.0001f)
        assertEquals(0f, TapSwap.outgoingContentAlpha(1f), 0.0001f)
    }

    @Test
    fun everyCutoutHasItsOwnId() {
        val ids = listOf(
            TapSwap.CUTOUT_SECONDARY, TapSwap.CUTOUT_PILL_SPOT, TapSwap.CUTOUT_EDGE_EAR,
            TapSwap.CUTOUT_EDGE_THIRD, TapSwap.CUTOUT_EDGE_PILL
        )
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it > 0 })
    }

    @Test
    fun cutoutIsNamedBySpotSoThePickerNeverHidesInItsPlace() {
        // the pill's spot: the secondary pill holds it, or (with same-app siblings) the tertiary pill does
        assertEquals(TapSwap.CUTOUT_PILL_SPOT, TapSwap.secondaryCutoutId(holdsPillSpot = true))
        // the secondary as the picker button / a round bubble is not the cutout
        assertEquals(TapSwap.CUTOUT_SECONDARY, TapSwap.secondaryCutoutId(holdsPillSpot = false))
        // tapped the pill's spot: whoever holds it hides, the picker button does not
        val tapped = TapSwap.CUTOUT_PILL_SPOT
        assertTrue(TapSwap.hidesWithSwap(true, tapped, TapSwap.secondaryCutoutId(true)))
        assertTrue(TapSwap.hidesWithSwap(true, tapped, TapSwap.CUTOUT_PILL_SPOT))
        assertFalse(TapSwap.hidesWithSwap(true, tapped, TapSwap.secondaryCutoutId(false)))
        // nothing hides when no swap runs
        assertFalse(TapSwap.hidesWithSwap(false, tapped, TapSwap.CUTOUT_PILL_SPOT))
    }

    @Test
    fun swapCutoutVisibility() {
        assertEquals(0f, TapSwap.cutoutAlpha(0f), 0.0001f)
        assertEquals(0f, TapSwap.cutoutAlpha(0.6f), 0.0001f)
        assertEquals(1f, TapSwap.cutoutAlpha(1f), 0.0001f)

        assertEquals(0f, TapSwap.incomingContentAlpha(0f), 0f)
        assertEquals(1f, TapSwap.outgoingContentAlpha(0f), 0f)
        assertEquals(0f, TapSwap.cutoutAlpha(0f), 0f)
    }

    @Test
    fun swapContentHandoff() {
        assertTrue(TapSwap.OUTGOING_FADE_END <= TapSwap.INCOMING_FADE_START)
        // the old island lands as the cutout pill before the incoming card is fully open, after the ghost's card is gone
        assertTrue(TapSwap.CUTOUT_BACK_PROGRESS < TapSwap.INCOMING_FADE_END)
        assertTrue(TwoShellSwap.GHOST_CONTENT_FADE_END <= TapSwap.CUTOUT_BACK_PROGRESS)
        assertTrue(TapSwap.OUTGOING_FADE_START < TapSwap.OUTGOING_FADE_END)
        assertTrue(TapSwap.INCOMING_FADE_START < TapSwap.INCOMING_FADE_END)
        // the gap the selection switches in is wide enough to be hit by a frame
        assertTrue(TapSwap.INCOMING_FADE_START - TapSwap.OUTGOING_FADE_END >= 0.08f)
    }

    @Test
    fun swapIsDroppedAfterAboutASecondAndAHalfAtLeast() {
        assertEquals(1500L, TapSwap.MAX_SWAP_MS)
    }

    @Test
    fun tapWaitsForPickerTrade() {
        assertTrue(TapSwap.heldByPicker(expanded = true, pickerMoving = true, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByPicker(expanded = true, pickerMoving = false, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByPicker(expanded = false, pickerMoving = true, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByPicker(expanded = true, pickerMoving = true, fromIndex = 1, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByPicker(expanded = true, pickerMoving = true, fromIndex = 0, toIndex = 2, cardCount = 2))
    }

    @Test
    fun touchFlipsOnlyAMovingSwap() {
        assertTrue(TapSwap.canFlip(active = true, settling = false, p = 0.3f))
        assertTrue(TapSwap.canFlip(active = true, settling = false, p = 0f))
        assertFalse(TapSwap.canFlip(active = false, settling = false, p = 0.3f))
        assertFalse(TapSwap.canFlip(active = true, settling = true, p = 0.3f))
        assertFalse(TapSwap.canFlip(active = true, settling = false, p = 1f))
    }

    @Test
    fun outgoingReturnsOnlyWithoutContent() {
        // turning around while the shell shrinks and its content is still visible waits for the end
        assertFalse(TapSwap.backAfter(back = false, reverse = true, p = 0.2f))
        // inside the gap between the 2 fades
        assertTrue(TapSwap.backAfter(back = false, reverse = true, p = 0.35f))
        assertTrue(TapSwap.backAfter(back = false, reverse = true, p = TapSwap.OUTGOING_FADE_END))
        assertTrue(TapSwap.backAfter(back = false, reverse = true, p = TapSwap.INCOMING_FADE_START))
        // the incoming content is already showing: no switch
        assertFalse(TapSwap.backAfter(back = false, reverse = true, p = 0.8f))
        // going on again: the incoming event is selected once the outgoing content is gone
        assertFalse(TapSwap.backAfter(back = true, reverse = false, p = 0.35f))
        assertFalse(TapSwap.backAfter(back = true, reverse = false, p = 0.9f))
        assertTrue(TapSwap.backAfter(back = true, reverse = false, p = 0.2f))
        assertTrue(TapSwap.backAfter(back = true, reverse = true, p = 0.1f))
        assertFalse(TapSwap.backAfter(back = false, reverse = false, p = 0.35f))
    }

    @Test
    fun tapDuringTheBubbleFlightIsHeld() {
        assertTrue(TapSwap.heldByFlight(expanded = true, flightRunning = true, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByFlight(expanded = true, flightRunning = false, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByFlight(expanded = false, flightRunning = true, fromIndex = 0, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByFlight(expanded = true, flightRunning = true, fromIndex = 1, toIndex = 1, cardCount = 2))
        assertFalse(TapSwap.heldByFlight(expanded = true, flightRunning = true, fromIndex = 0, toIndex = 2, cardCount = 2))
        assertFalse(TapSwap.heldByFlight(expanded = true, flightRunning = true, fromIndex = 0, toIndex = -1, cardCount = 2))
    }

    @Test
    fun heldTapReplaysOnlyForACardThatIsStillThereAndNotOpen() {
        val keys = listOf("a", "b", "c")
        assertEquals(1, TapSwap.replayIndex("b", keys, 0))
        assertEquals(2, TapSwap.replayIndex("c", keys, 1))
        assertNull(TapSwap.replayIndex("b", keys, 1))
        assertNull(TapSwap.replayIndex("gone", keys, 0))
        assertNull(TapSwap.replayIndex(null, keys, 0))
        assertNull(TapSwap.replayIndex("a", emptyList(), 0))
    }

    @Test
    fun actionButtonFadeWindow() {
        assertEquals(0f, TapSwap.actionsAlpha(TapSwap.ACTIONS_FADE_START), 0.0001f)
        assertEquals(1f, TapSwap.actionsAlpha(TapSwap.ACTIONS_FADE_END), 0.0001f)
        assertEquals(1f, TapSwap.actionsAlpha(1f), 0.0001f)
        assertTrue(TapSwap.ACTIONS_FADE_START >= TapSwap.INCOMING_FADE_END - 0.1f)
        assertTrue(TapSwap.ACTIONS_FADE_END <= 1f)
        assertTrue(TapSwap.CUTOUT_BACK_PROGRESS < TapSwap.ACTIONS_FADE_END)
    }

    @Test
    fun aSwapTurnedRoundEarlyPicksTheOutgoingEventAtOnce() {
        val step = { back: Boolean, reverse: Boolean, picked: Boolean, p: Float ->
            TapSwap.selectionStep(back, reverse, picked, p)
        }
        // turned round while the incoming card is still invisible: pick the outgoing card now
        assertEquals(TapSwap.SelectStep.PickOld, step(false, true, false, 0.1f))
        assertEquals(TapSwap.SelectStep.PickOld, step(false, true, false, TapSwap.INCOMING_FADE_START - 0.01f))
        // the incoming card is on screen: the gap's own switch (backAfter) decides
        assertEquals(TapSwap.SelectStep.None, step(false, true, false, TapSwap.INCOMING_FADE_START))
        assertEquals(TapSwap.SelectStep.None, step(false, true, false, 0.9f))
        // picked once only
        assertEquals(TapSwap.SelectStep.None, step(false, true, true, 0.1f))
        // already back, or heading for the incoming event with nothing picked early: nothing to do
        assertEquals(TapSwap.SelectStep.None, step(true, true, true, 0.1f))
        assertEquals(TapSwap.SelectStep.None, step(false, false, false, 0.1f))
    }

    @Test
    fun turnedAgainPicksIncoming() {
        assertEquals(TapSwap.SelectStep.PickNew, TapSwap.selectionStep(false, false, true, 0.2f))
        // once back is set the gap's switch (backAfter) does it
        assertEquals(TapSwap.SelectStep.None, TapSwap.selectionStep(true, false, true, 0.2f))
    }

    @Test
    fun theOutgoingCardOnlyWaitsForTheFramesItStillNeeds() {
        assertEquals(TapSwap.OLD_CARD_COMPOSE_FRAMES, TapSwap.composeFramesLeft(0))
        assertEquals(1, TapSwap.composeFramesLeft(1))
        assertEquals(0, TapSwap.composeFramesLeft(2))
        assertEquals(0, TapSwap.composeFramesLeft(40))
        assertEquals(TapSwap.OLD_CARD_COMPOSE_FRAMES, TapSwap.composeFramesLeft(-3))
    }
}
