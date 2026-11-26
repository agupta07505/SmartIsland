/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.CollapsedPulse.Kind
import com.agupta07505.smartisland.model.CollapsedPulse.State
import com.agupta07505.smartisland.model.CollapsedPulse.Waiting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedPulseQueueTest : CollapsedPulseTestBase() {

    @Test
    fun firstEventPulsesAtOnce() {
        val s = State().arrive(live("music"), 1_000L)
        assertEquals("music", s.pulsing?.id)
        assertEquals(1_000L + IslandTimings.LIVE_PULSE_MS, s.pulseEndsAtMs)
        assertTrue(s.queue.isEmpty())
        assertEquals(s.pulseEndsAtMs, CollapsedPulse.nextDeadlineMs(s))
    }

    @Test
    fun nothingPendingHasNoDeadline() {
        assertNull(CollapsedPulse.nextDeadlineMs(State()))
        assertFalse(CollapsedPulse.blocksExpand(State()))
    }

    @Test
    fun threeEventPulseOrder() {
        val music = entry(card("music", IslandMode.Music))
        val download = entry(card("download", IslandMode.DownloadUpload))
        val plugged = entry(card("system_battery", IslandMode.Battery, category = "battery_plugged_in"))
        val s0 = State().arrive(music, 0L).arrive(download, 0L).arrive(plugged, 0L)
        assertEquals("music", s0.pulsing?.id)
        assertEquals(listOf("download", plugged.id), s0.queued())

        val gap1 = s0.advance(IslandTimings.LIVE_PULSE_MS)
        assertNull(gap1.pulsing)
        assertTrue(gap1.inGap)
        assertEquals(IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(2), gap1.gapEndsAtMs)
        assertTrue("music" in gap1.settled)

        val second = gap1.advance(gap1.gapEndsAtMs!!)
        assertEquals("download", second.pulsing?.id)

        // time spent behind other pulses does not make a quick event stale
        val third = second.advance(second.pulseEndsAtMs + IslandTimings.PULSE_GAP_MS)
        assertEquals(plugged.id, third.pulsing?.id)
        assertTrue(third.queue.isEmpty())
    }

    @Test
    fun quickBehindOneLiveStillPlays() {
        val music = entry(card("music", IslandMode.Music))
        val plugged = entry(card("system_battery", IslandMode.Battery, category = "battery_plugged_in"))
        val s0 = State().arrive(music, 0L).arrive(plugged, 0L)
        val gap = s0.advance(IslandTimings.LIVE_PULSE_MS)
        assertEquals(listOf(plugged.id), gap.queued())

        // its turn comes at 4.05 s, well inside the stale limit
        val next = gap.advance(gap.gapEndsAtMs!!)
        assertEquals(plugged.id, next.pulsing?.id)
        assertFalse(plugged.id in next.settled)
    }

    @Test
    fun quickBehindTwoLiveStillPlays() {
        val s = State().arrive(live("a", holdMs = 4_000L), 0L)
            .arrive(live("b", rank = 40, holdMs = 4_000L), 0L)
            .arrive(quick("q"), 0L)
        val second = s.advance(4_000L + CollapsedPulse.gapMs(2))
        assertEquals("b", second.pulsing?.id)
        val gap = second.advance(second.pulseEndsAtMs)
        assertEquals("q", gap.advance(gap.gapEndsAtMs!!).pulsing?.id)
    }

    @Test
    fun quickEventThatStaysNextInLineTooLongLosesItsPulse() {
        val s = State().arrive(live("a"), 0L).arrive(quick("q"), 0L)
        val held = CollapsedPulse.hold(s, 1_000L)
        val back = CollapsedPulse.release(held, 12_000L)
        val done = back.advance(back.pulseEndsAtMs)
        assertFalse(done.busy)
        assertTrue("q" in done.settled)
    }

    @Test
    fun burstOfThreeAllPlay() {
        val s0 = State().arrive(live("a", holdMs = 1_000L), 0L)
            .arrive(live("b", rank = 40, holdMs = 1_000L), 0L)
            .arrive(quick("c", holdMs = 1_000L), 0L)
        assertEquals("a", s0.advance(999L).pulsing?.id)
        assertTrue(s0.advance(1_000L).inGap)
        assertEquals("b", s0.advance(1_450L).pulsing?.id)
        assertEquals("b", s0.advance(2_349L).pulsing?.id)
        assertEquals("c", s0.advance(2_800L).pulsing?.id)
        assertFalse(s0.advance(3_800L).busy)
    }

    @Test
    fun callCutsMusicThenItReturns() {
        val s0 = State().arrive(live("music", rank = 30), 0L)
        val s1 = s0.arrive(call(), 1_000L)
        assertEquals("call", s1.pulsing?.id)
        assertEquals(1_000L + IslandTimings.LIVE_PULSE_MS, s1.pulseEndsAtMs)
        assertFalse(s1.inGap)
        assertEquals(listOf("music"), s1.queued())
        assertTrue(s1.upNext.single().resumed)

        val gap = s1.advance(s1.pulseEndsAtMs)
        assertTrue(gap.inGap)
        val again = gap.advance(gap.gapEndsAtMs!!)
        assertEquals("music", again.pulsing?.id)
        assertEquals(gap.gapEndsAtMs!! + IslandTimings.LIVE_PULSE_MS, again.pulseEndsAtMs)
        assertTrue(again.queue.isEmpty())
    }

    @Test
    fun cutLiveGoesFirst() {
        val s = State().arrive(live("nav", rank = 10), 0L)
            .arrive(live("download", rank = 40), 10L)
            .arrive(call(), 20L)
        assertEquals(listOf("nav", "download"), s.queued())
    }

    @Test
    fun callCutsQuickEventDropsIt() {
        val s = State().arrive(quick("plug"), 0L).arrive(call(), 500L)
        assertEquals("call", s.pulsing?.id)
        assertTrue(s.queue.isEmpty())
        assertTrue("plug" in s.settled)
        assertTrue(CollapsedPulse.isRepost(s, "plug"))
    }

    @Test
    fun callCutsANotificationPulseToo() {
        val s = State().arrive(note("n"), 0L).arrive(call(), 500L)
        assertTrue(s.queue.isEmpty())
        assertTrue("n" in s.settled)
    }

    @Test
    fun callSkipsTheGap() {
        val s = State().arrive(live("music", rank = 30), 0L).arrive(quick("q"), 100L)
        val gap = s.advance(s.pulseEndsAtMs)
        assertTrue(gap.inGap)
        val withCall = gap.arrive(call(), gap.gapEndsAtMs!! - 100L)
        assertEquals("call", withCall.pulsing?.id)
        assertFalse(withCall.inGap)
        assertEquals(listOf("q"), withCall.queued())
    }

    @Test
    fun queuedCallPriority() {
        val s = State().arrive(call("c1"), 0L).arrive(live("music", rank = 30), 100L).arrive(call("c2"), 200L)
        assertEquals("c1", s.pulsing?.id)
        assertEquals(listOf("c2", "music"), s.queued())
    }

    @Test
    fun callEndStopsItsPulse() {
        val s = State().arrive(call(), 0L).arrive(live("music", rank = 30), 100L)
        val after = CollapsedPulse.cardsChanged(s, setOf("music"), 1_000L)
        assertNull(after.pulsing)
        assertEquals(1_000L + IslandTimings.PULSE_GAP_MS, after.gapEndsAtMs)
        assertFalse("call" in after.settled)
        assertEquals("music", after.advance(1_450L).pulsing?.id)
    }

    @Test
    fun callThatEndsWithNothingWaitingLeavesTheCutoutFree() {
        val s = State().arrive(call(), 0L)
        val after = CollapsedPulse.cardsChanged(s, emptySet(), 1_000L)
        assertFalse(after.busy)
    }

    @Test
    fun otherCallStatesFollowTheNormalQueue() {
        val ongoing = entry(card("call", IslandMode.IncomingCall))
        val ringing = entry(card("call2", IslandMode.IncomingCall, actions = listOf("Answer", "Decline")))
        assertEquals(Kind.Live, ongoing.kind)
        assertEquals(0, ongoing.rank)
        assertEquals(Kind.Call, ringing.kind)
        val s = State().arrive(live("music", rank = 30), 0L).arrive(ongoing, 100L)
        assertEquals("music", s.pulsing?.id)
        assertEquals(listOf("call"), s.queued())
    }

    @Test
    fun rulesDemoBlocksExpandUntilEverythingQueuedIsDone() {
        val s0 = State().arrive(live("download", rank = 40), 0L)
        assertTrue(CollapsedPulse.blocksExpand(s0))
        val s1 = s0.arrive(quick("plug", holdMs = 2_000L), 1_000L)
        assertEquals("download", s1.pulsing?.id)
        assertEquals(listOf("plug"), s1.queued())

        val gap = s1.advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue(CollapsedPulse.blocksExpand(gap))
        assertEquals(gap.gapEndsAtMs, CollapsedPulse.nextDeadlineMs(gap))

        val plug = gap.advance(gap.gapEndsAtMs!!)
        assertEquals("plug", plug.pulsing?.id)
        assertTrue(CollapsedPulse.blocksExpand(plug))

        val done = plug.advance(plug.pulseEndsAtMs)
        assertFalse(CollapsedPulse.blocksExpand(done))
        assertNull(CollapsedPulse.nextDeadlineMs(done))
    }

    @Test
    fun eventDuringTheGapOnlyQueues() {
        val s = State().arrive(live("a"), 0L).arrive(live("b", rank = 40), 10L).advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue(s.inGap)
        val s2 = s.arrive(live("c", rank = 30), IslandTimings.LIVE_PULSE_MS + 100L)
        assertNull(s2.pulsing)
        assertTrue(s2.inGap)
        assertEquals(listOf("c", "b"), s2.queued())
    }

    @Test
    fun repostOfAPulsingQueuedOrSettledEventChangesNothing() {
        val pulsing = live("a")
        val queued = quick("q")
        val s = State().arrive(pulsing, 0L).arrive(queued, 100L)
        assertEquals(s, s.arrive(pulsing, 200L))
        assertEquals(s, s.arrive(queued, 200L))

        val afterA = s.advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue("a" in afterA.settled)
        assertEquals(afterA, afterA.arrive(pulsing, IslandTimings.LIVE_PULSE_MS + 10L))
    }

    @Test
    fun quickEventPulsesAfterCardLeaves() {
        val plug = quick("plug")
        val done = State().arrive(plug, 0L).advance(2_000L)
        assertTrue("plug" in done.settled)
        assertFalse(done.busy)

        val gone = CollapsedPulse.cardsChanged(done, emptySet(), 3_000L)
        assertTrue(gone.settled.isEmpty())
        assertEquals("plug", gone.arrive(plug, 4_000L).pulsing?.id)
    }

    @Test
    fun cardsStillThereKeepTheirSettledMark() {
        val done = State().arrive(quick("plug"), 0L).advance(2_000L)
        assertEquals(done, CollapsedPulse.cardsChanged(done, setOf("plug"), 3_000L))
    }

    @Test
    fun equalRanksKeepArrivalOrder() {
        val s = State().arrive(live("base"), 0L).arrive(quick("q1"), 10L).arrive(quick("q2"), 20L)
        assertEquals(listOf("q1", "q2"), s.queued())
    }

    @Test
    fun betterRankGoesFirst() {
        val s = State().arrive(live("base", rank = 10), 0L)
            .arrive(quick("q"), 10L)
            .arrive(live("download", rank = 40), 20L)
        assertEquals(listOf("download", "q"), s.queued())
    }

    @Test
    fun newestFirstWithinRank() {
        val queue = listOf(
            Waiting(note("old"), 100L),
            Waiting(quick("quick"), 150L),
            Waiting(note("new"), 200L)
        )
        assertEquals(listOf("new", "quick", "old"), CollapsedPulse.order(queue).map { it.entry.id })
    }

    @Test
    fun fourthWaitingEventDropsTheOldestOfTheLowestPriority() {
        val s = State().arrive(live("base", rank = 30), 0L)
            .arrive(quick("q1"), 10L)
            .arrive(quick("q2"), 20L)
            .arrive(quick("q3"), 30L)
            .arrive(live("download", rank = 40), 40L)
        assertEquals(listOf("download", "q2", "q3"), s.queued())
        assertTrue("q1" in s.settled)
        assertTrue(CollapsedPulse.isRepost(s, "q1"))
    }

    @Test
    fun queueHoldsWhatFitsTheTimeBudget() {
        var s = State().arrive(live("base"), 0L)
        for (i in 1..6) s = s.arrive(quick("q$i"), i * 10L)
        assertEquals(listOf("q3", "q4", "q5", "q6"), s.queued())
        assertTrue(CollapsedPulse.queuedPlayMs(s.queue) <= IslandTimings.QUEUE_BUDGET_MS)
        assertTrue("q1" in s.settled)
        assertTrue("q2" in s.settled)
    }

    @Test
    fun shortEventsQueueDeeperThanThree() {
        var s = State().arrive(live("base"), 0L)
        for (i in 1..CollapsedPulse.QUEUE_MAX) s = s.arrive(quick("q$i", holdMs = 1_000L), i * 10L)
        assertEquals(CollapsedPulse.QUEUE_MAX, s.queue.size)
        s = s.arrive(quick("extra", holdMs = 1_000L), 100L)
        assertEquals(CollapsedPulse.QUEUE_MAX, s.queue.size)
        assertTrue("q1" in s.settled)
    }

    @Test
    fun extensionShrinksWithQueue() {
        val alone = State().arrive(live("a"), 0L)
        assertEquals(IslandTimings.PULSE_EXTEND_CAP_MS, CollapsedPulse.ensureHold(alone, 30_000L, 1_000L).pulseEndsAtMs)

        val one = alone.arrive(quick("q1"), 10L)
        assertEquals(
            IslandTimings.PULSE_EXTEND_CAP_ONE_WAITING_MS,
            CollapsedPulse.ensureHold(one, 30_000L, 1_000L).pulseEndsAtMs
        )

        val two = one.arrive(quick("q2"), 20L)
        assertEquals(two.pulseEndsAtMs, CollapsedPulse.ensureHold(two, 30_000L, 1_000L).pulseEndsAtMs)
    }

    @Test
    fun newerNotificationOfTheSameAppReplacesTheQueuedOne() {
        val s = State().arrive(live("base"), 0L)
            .arrive(note("n1", app = "chat"), 10L)
            .arrive(note("n2", app = "chat"), 20L)
        assertEquals(listOf("n2"), s.queued())
        assertTrue("n1" in s.settled)
    }

    @Test
    fun differentAppsBothWait() {
        val s = State().arrive(live("base"), 0L)
            .arrive(note("n1", app = "chat"), 10L)
            .arrive(note("n2", app = "mail"), 20L)
        assertEquals(listOf("n2", "n1"), s.queued())
    }

    @Test
    fun burstOfFiveKeepsThreeNewest() {
        var s = State().arrive(live("base"), 0L)
        for (i in 1..5) s = s.arrive(note("n$i"), i * 10L)
        assertEquals(listOf("n5", "n4", "n3"), s.queued())
        assertTrue("n1" in s.settled)
        assertTrue("n2" in s.settled)
    }

    @Test
    fun aRoutineEventLosesItsPulseBeforeAnyNotificationDoes() {
        val plugged = entry(card("system_wifi", IslandMode.Bluetooth, category = "wifi"))
        val s = State().arrive(live("base"), 0L)
            .arrive(plugged, 10L)
            .arrive(note("n1"), 20L)
            .arrive(note("n2"), 30L)
            .arrive(note("n3"), 40L)
        assertEquals(listOf("n3", "n2", "n1"), s.queued())
        assertTrue(plugged.id in s.settled)
    }

    @Test
    fun newNeverPushesOutHigherRank() {
        val s = State().arrive(live("base", rank = 10), 0L)
            .arrive(live("m1", rank = 30), 10L)
            .arrive(live("m2", rank = 30), 20L)
            .arrive(live("m3", rank = 30), 30L)
            .arrive(quick("q"), 40L)
        assertEquals(listOf("m1", "m2", "m3"), s.queued())
        assertTrue("q" in s.settled)
    }

    @Test
    fun higherRankDropsOldestLowest() {
        val s = State().arrive(live("base", rank = 10), 0L)
            .arrive(live("m", rank = 30), 10L)
            .arrive(live("d1", rank = 40), 20L)
            .arrive(live("d2", rank = 40), 30L)
            .arrive(live("nav", rank = 15), 40L)
        assertEquals(listOf("nav", "m", "d2"), s.queued())
        assertTrue("d1" in s.settled)
    }
}
