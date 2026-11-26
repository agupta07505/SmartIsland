/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.CollapsedPulse.Kind
import com.agupta07505.smartisland.model.CollapsedPulse.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedPulseSettleTest : CollapsedPulseTestBase() {

    @Test
    fun quickCardHasNoSettledLook() {
        assertFalse(CollapsedPulse.drawsSettled(card("b", IslandMode.Battery), pulses = true))
        assertFalse(CollapsedPulse.drawsSettled(card("bt", IslandMode.Bluetooth), pulses = true))
    }

    @Test
    fun settledCardAppearance() {
        assertTrue(CollapsedPulse.drawsSettled(card("m", IslandMode.Music), pulses = true))
        assertTrue(CollapsedPulse.drawsSettled(card("n", IslandMode.Notification), pulses = true))
        assertTrue(CollapsedPulse.drawsSettled(null, pulses = true))
        assertTrue(CollapsedPulse.drawsSettled(card("b", IslandMode.Battery), pulses = false))
    }

    @Test
    fun cardKindsFollowEventAndCall() {
        assertEquals(Kind.Notification, entry(card("n", IslandMode.Notification)).kind)
        assertEquals(Kind.Live, entry(card("m", IslandMode.Music)).kind)
        assertEquals(Kind.Live, entry(card("t", IslandMode.Timer)).kind)
        assertEquals(Kind.Quick, entry(card("b", IslandMode.Battery)).kind)
        assertEquals(Kind.Quick, entry(card("bt", IslandMode.Bluetooth)).kind)
    }

    @Test
    fun ranksComeFromIslandPriority() {
        assertEquals(30, entry(card("m", IslandMode.Music).copy(mediaIsPlaying = true)).rank)
        assertEquals(31, entry(card("m", IslandMode.Music)).rank)
        assertEquals(40, entry(card("d", IslandMode.DownloadUpload)).rank)
        assertEquals(50, entry(card("n", IslandMode.Notification)).rank)
    }

    @Test
    fun sharedKeyGetsOneIdPerEvent() {
        val plugged = entry(card("system_battery", IslandMode.Battery, category = "battery_plugged_in"))
        val low = entry(card("system_battery", IslandMode.Battery, category = "battery_low"))
        assertNotEquals(plugged.id, low.id)
        assertEquals(plugged.id, entry(card("system_battery", IslandMode.Battery, category = "battery_plugged_in")).id)
        assertEquals("n", entry(card("n", IslandMode.Notification)).id)
    }

    @Test
    fun holdsComeFromIslandTimings() {
        fun hold(mode: IslandMode, category: String? = null) = entry(card("k", mode, category)).holdMs
        assertEquals(IslandTimings.PLUGGED_IN_CARD_MS, hold(IslandMode.Battery, "battery_plugged_in"))
        assertEquals(IslandTimings.UNPLUGGED_CARD_MS, hold(IslandMode.Battery, "battery_plugged_out"))
        assertEquals(IslandTimings.BATTERY_WARNING_CARD_MS, hold(IslandMode.Battery, "battery_low"))
        assertEquals(IslandTimings.BATTERY_WARNING_CARD_MS, hold(IslandMode.Battery, "battery_full"))
        assertEquals(IslandTimings.BATTERY_WARNING_CARD_MS, hold(IslandMode.Battery, "battery_saver"))
        assertEquals(IslandTimings.CONNECTIVITY_CARD_MS, hold(IslandMode.Bluetooth))
        assertEquals(IslandTimings.CONNECTIVITY_CARD_MS, hold(IslandMode.Bluetooth, "wifi"))
        assertEquals(IslandTimings.CONNECTIVITY_CARD_MS, hold(IslandMode.Bluetooth, "usb_connected"))
        assertEquals(IslandTimings.DISCONNECT_CARD_MS, hold(IslandMode.Bluetooth, "wifi_disconnected"))
        assertEquals(IslandTimings.DISCONNECT_CARD_MS, hold(IslandMode.Bluetooth, "vpn_disconnected"))
        assertEquals(IslandTimings.DISCONNECT_CARD_MS, hold(IslandMode.Bluetooth, "usb_disconnected"))
        assertEquals(IslandTimings.DISCONNECT_CARD_MS, hold(IslandMode.Bluetooth, "headphones_disconnected"))
        assertEquals(IslandTimings.LIVE_PULSE_MS, hold(IslandMode.Music))
        assertEquals(IslandTimings.LIVE_PULSE_MS, hold(IslandMode.DownloadUpload))
        assertEquals(IslandTimings.NOTIFICATION_PULSE_MIN_MS, hold(IslandMode.Notification))
    }

    @Test
    fun oppositesShareAPair() {
        fun pair(mode: IslandMode, category: String) = entry(card("k", mode, category)).pair
        assertEquals(pair(IslandMode.Battery, "battery_plugged_in"), pair(IslandMode.Battery, "battery_plugged_out"))
        assertEquals(pair(IslandMode.Bluetooth, "wifi"), pair(IslandMode.Bluetooth, "wifi_disconnected"))
        assertEquals(pair(IslandMode.Bluetooth, "vpn"), pair(IslandMode.Bluetooth, "vpn_disconnected"))
        assertEquals(pair(IslandMode.Bluetooth, "usb_connected"), pair(IslandMode.Bluetooth, "usb_disconnected"))
        assertEquals(
            pair(IslandMode.Bluetooth, "headphones_connected"),
            pair(IslandMode.Bluetooth, "headphones_disconnected")
        )
        assertNotEquals(pair(IslandMode.Bluetooth, "wifi"), pair(IslandMode.Bluetooth, "vpn"))
        assertEquals(pair(IslandMode.Bluetooth, "airplane_on"), pair(IslandMode.Bluetooth, "airplane_off"))
        assertEquals(pair(IslandMode.Bluetooth, "ringer_ring"), pair(IslandMode.Bluetooth, "ringer_silent"))
        assertEquals(pair(IslandMode.Bluetooth, "filter_dnd"), pair(IslandMode.Bluetooth, "mode_driving_off"))
        assertNotEquals(pair(IslandMode.Bluetooth, "airplane_on"), pair(IslandMode.Bluetooth, "ringer_ring"))
        assertNull(pair(IslandMode.Battery, "battery_low"))
        assertNull(pair(IslandMode.Bluetooth, "bluetooth"))
    }

    @Test
    fun onlyNotificationsCarryTheirApp() {
        assertEquals("pkg.chat", entry(card("n", IslandMode.Notification, pkg = "pkg.chat")).app)
        assertNull(entry(card("m", IslandMode.Music)).app)
    }

    @Test
    fun skippedPulseState() {
        val active = State().arrive(quick("q"), 0L)
        val activeAfter = CollapsedPulse.skip(active, "q", 500L)
        assertFalse(activeAfter.busy)
        assertTrue("q" in activeAfter.settled)
        assertEquals(activeAfter, activeAfter.arrive(quick("q"), 600L))

        val queued = State().arrive(live("a"), 0L).arrive(quick("q"), 10L)
        val queuedAfter = CollapsedPulse.skip(queued, "q", 20L)
        assertEquals("a", queuedAfter.pulsing?.id)
        assertTrue(queuedAfter.queue.isEmpty())
        assertTrue("q" in queuedAfter.settled)

        val early = CollapsedPulse.skip(State(), "q", 0L)
        assertEquals(early, early.arrive(quick("q"), 10L))

        val last = State().arrive(live("a"), 0L).arrive(quick("q"), 10L).advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue(last.inGap)
        assertFalse(CollapsedPulse.skip(last, "q", 3_700L).busy)

        val gone = CollapsedPulse.cardsChanged(State().arrive(live("timer"), 0L), emptySet(), 100L)
        assertTrue("timer" in CollapsedPulse.skip(gone, "timer", 200L).settled)
    }

    @Test
    fun expandWaitsForOthersPulses() {
        val own = State().arrive(quick("q"), 0L)
        assertFalse(CollapsedPulse.blocksExpandFor(own, "q"))
        assertTrue(CollapsedPulse.blocksExpandFor(own, "other"))

        val queued = State().arrive(live("a"), 0L).arrive(quick("q"), 10L)
        assertTrue(CollapsedPulse.blocksExpandFor(queued, "q"))
        assertFalse(CollapsedPulse.blocksExpandFor(State(), "q"))
    }

    @Test
    fun autoExpandPreemption() {
        val s = State().arrive(live("music", rank = 30), 0L).arrive(note("n1"), 10L)
        assertTrue(CollapsedPulse.blocksExpandFor(s, "n"))
        assertTrue(CollapsedPulse.expandMayTakeOver(s, "n"))
        assertTrue(CollapsedPulse.expandMayTakeOver(State(), "n"))
    }

    @Test
    fun anUrgentPulseStillHoldsAnAutoExpandingCardBack() {
        val navigation = State().arrive(live("nav", rank = 10), 0L)
        assertFalse(CollapsedPulse.expandMayTakeOver(navigation, "n"))
        val lowBattery = State().arrive(live("music", rank = 30), 0L).arrive(live("batt", rank = 15), 10L)
        assertFalse(CollapsedPulse.expandMayTakeOver(lowBattery, "n"))
        // its own pulse is no reason
        assertTrue(CollapsedPulse.expandMayTakeOver(State().arrive(live("n", rank = 10), 0L), "n"))
    }

    @Test
    fun settleAllClearsTheQueueAndKeepsEveryCardSettled() {
        val s = State().arrive(live("a"), 0L).arrive(quick("q1"), 10L).arrive(note("n"), 20L)
        val after = CollapsedPulse.settleAll(s)
        assertFalse(after.busy)
        assertTrue(after.queue.isEmpty())
        assertEquals(setOf("a", "q1", "n"), after.settled)
        assertNull(CollapsedPulse.nextDeadlineMs(after))
    }

    @Test
    fun settleAllKeepsWhatWasSettledBefore() {
        val s = State(settled = setOf("old")).arrive(live("a"), 0L)
        assertEquals(setOf("old", "a"), CollapsedPulse.settleAll(s).settled)
    }

    @Test
    fun rearmLetsASettledCardPulseAgain() {
        val s = State().arrive(note("n"), 0L).advance(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertTrue("n" in s.settled)
        val again = CollapsedPulse.rearm(s, "n").arrive(note("n"), 3_000L)
        assertEquals("n", again.pulsing?.id)
    }

    @Test
    fun rearmUnsettledChangesNothing() {
        val s = State().arrive(note("n"), 0L)
        assertEquals(s, CollapsedPulse.rearm(s, "n"))
    }

    @Test
    fun pulseTapDuringGap() {
        val pulsing = State().arrive(live("download", rank = 40), 0L).arrive(quick("plug", holdMs = 2_000L), 1_000L)
        assertEquals("download", CollapsedPulse.tapTarget(pulsing))

        val gap = pulsing.advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue(gap.inGap)
        assertEquals("download", CollapsedPulse.tapTarget(gap))
        assertEquals("download", gap.lastPulsedId)
    }

    @Test
    fun tapTargetIsNullWhenNothingPulsedYet() {
        assertNull(CollapsedPulse.tapTarget(State()))
    }

    @Test
    fun tapClaimSettlesAll() {
        val s = State().arrive(live("download", rank = 40), 0L)
            .arrive(quick("plug", holdMs = 2_000L), 1_000L)
            .arrive(note("n"), 1_500L)
        val claim = CollapsedPulse.claimByTap(s)
        assertEquals("download", claim.id)
        assertNull(claim.state.pulsing)
        assertTrue(claim.state.queue.isEmpty())
        assertFalse(claim.state.busy)
        assertTrue(setOf("download", "plug", "n").all { it in claim.state.settled })
        assertFalse(CollapsedPulse.blocksExpand(claim.state))
    }

    @Test
    fun tapClaimInGapOpensLastCard() {
        val gap = State().arrive(live("download", rank = 40), 0L)
            .arrive(quick("plug", holdMs = 2_000L), 1_000L)
            .advance(IslandTimings.LIVE_PULSE_MS)
        val claim = CollapsedPulse.claimByTap(gap)
        assertEquals("download", claim.id)
        assertFalse(claim.state.busy)
        assertTrue("plug" in claim.state.settled)
    }

    @Test
    fun lastPulsedCardThatIsGoneIsNotOpened() {
        val gap = State().arrive(live("download", rank = 40), 0L)
            .arrive(quick("plug", holdMs = 2_000L), 1_000L)
            .advance(IslandTimings.LIVE_PULSE_MS)
        val gone = CollapsedPulse.cardsChanged(gap, setOf("plug"), IslandTimings.LIVE_PULSE_MS)
        assertNull(gone.lastPulsedId)
        assertNull(CollapsedPulse.claimByTap(gone).id)
    }

    @Test
    fun aPulseThatEndedBecauseItsCardLeftIsNotRemembered() {
        val s = State().arrive(live("download", rank = 40), 0L)
        val gone = CollapsedPulse.cardsChanged(s, emptySet(), 1_000L)
        val ended = CollapsedPulse.endPulse(gone, 2_000L)
        assertNull(ended.lastPulsedId)
    }

    @Test
    fun settledCardTapTarget() {
        var s = State().arrive(live("base"), 0L)
        for (i in 1..4) s = s.arrive(note("n$i"), i * 10L)
        assertTrue("n1" in s.settled)
        val claim = CollapsedPulse.claimByTap(s)
        assertEquals("base", claim.id)
        assertTrue(claim.state.queue.isEmpty())
        assertTrue("n1" in claim.state.settled)
        assertTrue(CollapsedPulse.isRepost(claim.state, "n1"))
    }
}

class CollapsedPulseTransitionsTest : CollapsedPulseTestBase() {

    @Test
    fun laterOppositeReplacesQueued() {
        val s = State().arrive(live("base"), 0L)
            .arrive(quick("in", pair = "power"), 10L)
            .arrive(quick("out", pair = "power"), 20L)
        assertEquals(listOf("out"), s.queued())
    }

    @Test
    fun anOppositeEventMorphsThePulsingOneInsteadOfWaiting() {
        val s = State().arrive(quick("in", holdMs = 2_000L, pair = "power"), 1_000L)
            .arrive(quick("out", holdMs = 2_200L, pair = "power"), 1_500L)
        assertEquals("out", s.pulsing?.id)
        assertTrue(s.queue.isEmpty())
        assertTrue("in" in s.settled)
        // the pulse holds for the new event's own time from the moment it arrived
        assertEquals(1_500L + 2_200L, s.pulseEndsAtMs)
        assertEquals(1_000L, s.pulseStartedAtMs)
        assertEquals("in", s.morphedFromId)
        assertNull(s.advance(s.pulseEndsAtMs).morphedFromId)
    }

    @Test
    fun theMorphNeverShortensWhatWasLeft() {
        val s = State().arrive(quick("in", holdMs = 3_200L, pair = "wifi"), 0L)
            .arrive(quick("out", holdMs = 2_000L, pair = "wifi"), 100L)
        assertEquals("out", s.pulsing?.id)
        assertEquals(3_200L, s.pulseEndsAtMs)
    }

    @Test
    fun aFlappingPairCannotHoldThePulsePastTheMorphCap() {
        var s = State().arrive(quick("a", holdMs = 3_000L, pair = "wifi"), 0L)
        var now = 0L
        repeat(7) { i ->
            now += 1_000L
            s = s.advance(now).arrive(quick("flap$i", holdMs = 3_000L, pair = "wifi"), now)
        }
        assertEquals("flap6", s.pulsing?.id)
        assertEquals(IslandTimings.MORPH_CAP_MS + IslandTimings.MORPH_MIN_MS, s.pulseEndsAtMs)
    }

    @Test
    fun morphKeepsLastStateForMinimum() {
        val s = State().arrive(quick("a", holdMs = 2_000L, pair = "wifi"), 0L)
            .arrive(quick("b", holdMs = 1_000L, pair = "wifi"), 1_800L)
        assertEquals("b", s.pulsing?.id)
        assertEquals(1_800L + IslandTimings.MORPH_MIN_MS, s.pulseEndsAtMs)
    }

    @Test
    fun aMorphKeepsTheOtherEventsWaiting() {
        val s = State().arrive(quick("in", pair = "power"), 0L)
            .arrive(quick("vpn", pair = "vpn"), 10L)
            .arrive(quick("out", pair = "power"), 20L)
        assertEquals("out", s.pulsing?.id)
        assertEquals(listOf("vpn"), s.queued())
    }

    @Test
    fun onlyPairedQuickEventsMorph() {
        val playing = State().arrive(live("music"), 0L)
        assertFalse(CollapsedPulse.morphs(playing, quick("out", pair = "power")))
        val unpaired = State().arrive(quick("low"), 0L)
        assertFalse(CollapsedPulse.morphs(unpaired, quick("out")))
        val other = State().arrive(quick("wifi", pair = "wifi"), 0L)
        assertFalse(CollapsedPulse.morphs(other, quick("vpn", pair = "vpn")))
        assertFalse(CollapsedPulse.morphs(other, quick("wifi", pair = "wifi")))
        assertFalse(CollapsedPulse.morphs(State(), quick("out", pair = "wifi")))
    }

    @Test
    fun oppositeAfterEndPlaysOwnPulse() {
        val s = State().arrive(quick("in", holdMs = 2_000L, pair = "power"), 0L).advance(2_000L)
        val next = s.arrive(quick("out", pair = "power"), 2_100L)
        assertNull(s.pulsing)
        assertEquals("out", next.pulsing?.id)
        assertEquals(2_100L, next.pulseStartedAtMs)
    }

    @Test
    fun onlyStateCardsWithOneIdForBothStatesFlipInPlace() {
        for (category in listOf("airplane_on", "airplane_off", "filter_dnd", "mode_driving", "mode_driving_off")) {
            assertTrue(category, CollapsedPulse.flipsInPlace(card("k", IslandMode.Bluetooth, category)))
        }
        for (category in listOf("wifi", "wifi_disconnected", "ringer_ring", "bluetooth")) {
            assertFalse(category, CollapsedPulse.flipsInPlace(card("k", IslandMode.Bluetooth, category)))
        }
        assertFalse(CollapsedPulse.flipsInPlace(card("b", IslandMode.Battery, "battery_plugged_in")))
        assertFalse(CollapsedPulse.flipsInPlace(card("n", IslandMode.Notification)))
    }

    @Test
    fun airplanePulseIdentity() {
        val on = entry(card("system_state", IslandMode.Bluetooth, "airplane_on"))
        val off = entry(card("system_state", IslandMode.Bluetooth, "airplane_off"))
        assertEquals(on.id, off.id)
        val running = State().arrive(on, 0L)
        val flipped = CollapsedPulse.refresh(running, off, 2_000L)
        assertEquals(on.id, flipped.pulsing?.id)
        assertTrue(flipped.pulseEndsAtMs > running.pulseEndsAtMs)
        assertEquals(2_000L + off.holdMs, flipped.pulseEndsAtMs)
        // the cap holds for a flipping switch too
        val late = CollapsedPulse.refresh(flipped, on, 9_500L)
        assertEquals(IslandTimings.PULSE_EXTEND_CAP_MS, late.pulseEndsAtMs)
    }

    @Test
    fun differentPairsDoNotReplaceEachOther() {
        val s = State().arrive(live("base"), 0L)
            .arrive(quick("wifi", pair = "wifi"), 10L)
            .arrive(quick("vpn", pair = "vpn"), 20L)
        assertEquals(listOf("wifi", "vpn"), s.queued())
    }

    @Test
    fun theEventACallCutDoesNotCountAgainstTheQueueLimit() {
        val s = State().arrive(live("music", rank = 30), 0L)
            .arrive(quick("q1"), 10L)
            .arrive(quick("q2"), 20L)
            .arrive(quick("q3"), 25L)
            .arrive(call(), 30L)
        assertEquals(listOf("music", "q1", "q2", "q3"), s.queued())
    }

    @Test
    fun staleQuickPulse() {
        val hold = IslandTimings.QUICK_PULSE_STALE_MS - IslandTimings.PULSE_GAP_MS + 1L
        val s = State().arrive(live("base", holdMs = hold), 0L).arrive(quick("q"), 0L)
        val gap = s.advance(hold)
        assertTrue(gap.inGap)
        val idle = gap.advance(gap.gapEndsAtMs!!)
        assertFalse(idle.busy)
        assertTrue(idle.queue.isEmpty())
        assertTrue("q" in idle.settled)
    }

    @Test
    fun quickUnderFiveSecondsPulses() {
        val s = State().arrive(live("base", holdMs = 4_000L), 0L).arrive(quick("q"), 0L)
        val gap = s.advance(4_000L)
        assertEquals("q", gap.advance(gap.gapEndsAtMs!!).pulsing?.id)
    }

    @Test
    fun liveAndNotificationsNeverExpire() {
        val s = State().arrive(live("base", holdMs = 10_000L), 0L)
            .arrive(note("n"), 0L)
            .arrive(live("download", rank = 40), 0L)
        val gap = s.advance(10_000L)
        val next = gap.advance(gap.gapEndsAtMs!!)
        assertEquals("download", next.pulsing?.id)
        val gap2 = next.advance(next.pulseEndsAtMs)
        assertEquals("n", gap2.advance(gap2.gapEndsAtMs!!).pulsing?.id)
    }

    @Test
    fun staleEntryDoesNotStartTheGap() {
        val hold = IslandTimings.QUICK_PULSE_STALE_MS
        val s = State().arrive(live("base", holdMs = hold), 0L).arrive(quick("q"), 0L)
        val done = s.advance(hold)
        assertFalse(done.busy)
        assertNull(done.gapEndsAtMs)
    }

    @Test
    fun quickEventJustInsideTheStaleLimitStillGetsItsGap() {
        val hold = IslandTimings.QUICK_PULSE_STALE_MS - 1L
        val s = State().arrive(live("base", holdMs = hold), 0L).arrive(quick("q"), 0L)
        val done = s.advance(hold)
        assertTrue(done.inGap)
        assertEquals(listOf("q"), done.queued())
    }

    @Test
    fun staleLimitCoversLivePulseAndGap() {
        assertTrue(IslandTimings.QUICK_PULSE_STALE_MS > IslandTimings.LIVE_PULSE_MS + IslandTimings.PULSE_GAP_MS)
    }

    @Test
    fun queuedCardThatIsGoneLeavesTheQueue() {
        val s = State().arrive(live("base"), 0L).arrive(quick("q1"), 10L).arrive(quick("q2"), 20L)
        val after = CollapsedPulse.cardsChanged(s, setOf("base", "q2"), 100L)
        assertEquals(listOf("q2"), after.queued())
        assertFalse("q1" in after.settled)
    }

    @Test
    fun gapEndsWhenTheLastWaitingCardIsGone() {
        val s = State().arrive(live("a"), 0L).arrive(quick("q"), 10L).advance(IslandTimings.LIVE_PULSE_MS)
        assertTrue(s.inGap)
        val after = CollapsedPulse.cardsChanged(s, setOf("a"), 3_700L)
        assertFalse(after.busy)
    }

    @Test
    fun pulsingFinishesHoldWhenCardEnds() {
        val s = State().arrive(live("timer"), 0L).arrive(quick("q"), 10L)
        val gone = CollapsedPulse.cardsChanged(s, setOf("q"), 1_000L)
        assertEquals("timer", gone.pulsing?.id)
        assertEquals(s.pulseEndsAtMs, gone.pulseEndsAtMs)

        val next = gone.advance(gone.pulseEndsAtMs)
        assertNull(next.pulsing)
        assertFalse("timer" in next.settled)
        assertTrue(next.inGap)
    }

    @Test
    fun advanceRunsLateTransitionsFromTheirOwnDeadlines() {
        val s = State().arrive(live("a", holdMs = 1_000L), 0L).arrive(live("b", rank = 40, holdMs = 10_000L), 0L)
        val late = s.advance(5_000L)
        assertEquals("b", late.pulsing?.id)
        assertEquals(1_000L + IslandTimings.PULSE_GAP_MS + 10_000L, late.pulseEndsAtMs)
    }

    @Test
    fun advanceEarlyChangesNothing() {
        val s = State().arrive(live("a"), 0L).arrive(quick("q"), 10L)
        assertEquals(s, s.advance(IslandTimings.LIVE_PULSE_MS - 1L))
    }

    @Test
    fun endsDoNothingWhenIdle() {
        assertEquals(State(), CollapsedPulse.endPulse(State(), 1_000L))
        assertEquals(State(), CollapsedPulse.endGap(State(), 1_000L))
    }
}
