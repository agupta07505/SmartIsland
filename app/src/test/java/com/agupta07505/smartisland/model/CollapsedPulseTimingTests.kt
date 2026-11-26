/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.CollapsedPulse.Entry
import com.agupta07505.smartisland.model.CollapsedPulse.Kind
import com.agupta07505.smartisland.model.CollapsedPulse.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsedPulseTimingTest : CollapsedPulseTestBase() {

    private fun longNote(id: String) =
        Entry(id, Kind.Notification, 50, 6_000L, app = id, minHoldMs = 3_000L)

    private fun noteCard(text: String) = card("n", IslandMode.Notification).copy(text = text)

    @Test
    fun readHoldStaysAtTheFloorForShortTextAndStopsAtTheCap() {
        val floor = IslandTimings.NOTIFICATION_PULSE_MIN_MS
        val cap = IslandTimings.NOTIFICATION_PULSE_MAX_MS
        val free = IslandTimings.READ_FREE_CHARS
        assertEquals(floor, CollapsedPulse.readHoldMs(floor, cap, 0))
        assertEquals(floor, CollapsedPulse.readHoldMs(floor, cap, free))
        assertEquals(floor + IslandTimings.READ_MS_PER_CHAR * 10, CollapsedPulse.readHoldMs(floor, cap, free + 10))
        assertEquals(cap, CollapsedPulse.readHoldMs(floor, cap, 500))
    }

    @Test
    fun holdGrowsWithShownText() {
        val short = entry(noteCard("ok"))
        val mid = entry(noteCard("x".repeat(40)))
        val long = entry(noteCard("x".repeat(400)))
        assertEquals(IslandTimings.NOTIFICATION_PULSE_MIN_MS, short.holdMs)
        assertTrue(mid.holdMs > short.holdMs)
        assertTrue(long.holdMs > mid.holdMs)
        assertEquals(IslandTimings.NOTIFICATION_PULSE_MAX_MS, long.holdMs)
        assertEquals(IslandTimings.NOTIFICATION_PULSE_MIN_MS, long.minHoldMs)
    }

    @Test
    fun aLiveEventHoldsLongerOnlyWithATitleFromOutside() {
        val long = "t".repeat(60)
        val song = entry(card("m", IslandMode.Music).copy(title = long))
        assertEquals(IslandTimings.LIVE_PULSE_MAX_MS, song.holdMs)
        assertEquals(IslandTimings.LIVE_PULSE_MS, song.minHoldMs)
        assertEquals(IslandTimings.LIVE_PULSE_MS, entry(card("m", IslandMode.Music).copy(title = "Song")).holdMs)
        assertEquals(IslandTimings.LIVE_PULSE_MS, entry(card("h", IslandMode.Hotspot).copy(title = long)).holdMs)
    }

    @Test
    fun callsAndQuickEventsKeepTheirHolds() {
        val long = "t".repeat(60)
        val call = entry(card("c", IslandMode.IncomingCall, actions = listOf("Answer", "Decline")).copy(title = long))
        assertEquals(IslandTimings.LIVE_PULSE_MS, call.holdMs)
        val wifi = entry(card("w", IslandMode.Bluetooth, category = "wifi").copy(title = long))
        assertEquals(IslandTimings.CONNECTIVITY_CARD_MS, wifi.holdMs)
        assertEquals(wifi.holdMs, wifi.minHoldMs)
    }

    @Test
    fun pulseRanksFollowTheTiers() {
        assertEquals(15, entry(card("b", IslandMode.Battery, category = "battery_low")).rank)
        assertEquals(15, entry(card("w", IslandMode.Bluetooth, category = "wifi_disconnected")).rank)
        assertEquals(15, entry(card("t", IslandMode.Timer).copy(timerFinished = true)).rank)
        assertEquals(20, entry(card("t", IslandMode.Timer)).rank)
        assertEquals(45, entry(card("b", IslandMode.Battery, category = "battery_plugged_in")).rank)
        assertEquals(45, entry(card("c", IslandMode.CallEnded)).rank)
        assertEquals(60, entry(card("w", IslandMode.Bluetooth, category = "wifi")).rank)
    }

    @Test
    fun urgentEventPriority() {
        val low = entry(card("system_battery", IslandMode.Battery, category = "battery_low"))
        val plugged = entry(card("system_wifi", IslandMode.Bluetooth, category = "wifi"))
        val music = entry(card("music", IslandMode.Music))
        val note = entry(card("n", IslandMode.Notification))
        val download = entry(card("download", IslandMode.DownloadUpload))

        val urgent = State().arrive(download, 0L).arrive(music, 10L).arrive(low, 20L)
        assertEquals(listOf(low.id, "music"), urgent.queued())

        val routine = State().arrive(download, 0L).arrive(plugged, 10L).arrive(note, 20L)
        assertEquals(listOf("n", plugged.id), routine.queued())
    }

    @Test
    fun chargerPriority() {
        val plug = entry(card("system_battery", IslandMode.Battery, category = "battery_plugged_in"))
        val note = entry(card("n", IslandMode.Notification))
        val download = entry(card("download", IslandMode.DownloadUpload))

        val s = State().arrive(download, 0L).arrive(note, 10L).arrive(plug, 20L)
        assertEquals(listOf(plug.id, "n"), s.queued())
    }

    @Test
    fun aPulseWithNobodyWaitingHoldsInFull() {
        val s = State().arrive(longNote("n"), 1_000L)
        assertEquals(1_000L + 6_000L, s.pulseEndsAtMs)
    }

    @Test
    fun aPulseStartingWithOthersWaitingHoldsSeventyPercent() {
        val s = State().arrive(live("a"), 0L).arrive(longNote("n1"), 10L).arrive(longNote("n2"), 20L)
        val gapEnd = IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(2)
        val second = s.advance(gapEnd)
        assertEquals("n2", second.pulsing?.id)
        assertEquals(gapEnd + 4_200L, second.pulseEndsAtMs)

        val third = second.advance(second.pulseEndsAtMs + IslandTimings.PULSE_GAP_MS)
        assertEquals("n1", third.pulsing?.id)
        assertEquals(second.pulseEndsAtMs + IslandTimings.PULSE_GAP_MS + 6_000L, third.pulseEndsAtMs)
    }

    @Test
    fun shortenedHoldKeepsMinimum() {
        val short = Entry("n2", Kind.Notification, 50, 3_200L, app = "n2", minHoldMs = 3_000L)
        val s = State().arrive(live("a"), 0L).arrive(longNote("n1"), 10L).arrive(short, 20L)
        val gapEnd = IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(2)
        val second = s.advance(gapEnd)
        assertEquals("n2", second.pulsing?.id)
        assertEquals(gapEnd + 3_000L, second.pulseEndsAtMs)
    }

    private fun crowdedNote(id: String, holdMs: Long = 6_000L, minHoldMs: Long = 3_000L, crowdedMinHoldMs: Long = 2_000L) =
        Entry(id, Kind.Notification, 50, holdMs, app = id, minHoldMs = minHoldMs, crowdedMinHoldMs = crowdedMinHoldMs)

    private fun threeWaiting(first: Entry, second: Entry, third: Entry): State =
        State().arrive(live("a"), 0L).arrive(first, 10L).arrive(second, 20L).arrive(third, 30L)

    @Test
    fun gapShrinksWithQueue() {
        assertEquals(IslandTimings.PULSE_GAP_MS, CollapsedPulse.gapMs(0))
        assertEquals(IslandTimings.PULSE_GAP_MS, CollapsedPulse.gapMs(1))
        assertTrue(CollapsedPulse.gapMs(2) < CollapsedPulse.gapMs(1))
        assertEquals(IslandTimings.PULSE_GAP_MIN_MS, CollapsedPulse.gapMs(3))
        assertEquals(IslandTimings.PULSE_GAP_MIN_MS, CollapsedPulse.gapMs(6))
    }

    @Test
    fun pauseFollowsQueueSize() {
        val s = threeWaiting(crowdedNote("n1"), crowdedNote("n2"), crowdedNote("n3"))
        val ended = s.advance(IslandTimings.LIVE_PULSE_MS)
        assertEquals(IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(3), ended.gapEndsAtMs)
    }

    @Test
    fun holdShrinksAsMoreWait() {
        val s = threeWaiting(crowdedNote("n1"), crowdedNote("n2"), crowdedNote("n3"))
        val gapEnd = IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(3)
        val first = s.advance(gapEnd)
        assertEquals("n3", first.pulsing?.id)
        assertEquals(gapEnd + 3_300L, first.pulseEndsAtMs)

        val secondStart = first.pulseEndsAtMs + CollapsedPulse.gapMs(2)
        val second = first.advance(secondStart)
        assertEquals(secondStart + 4_200L, second.pulseEndsAtMs)

        val thirdStart = second.pulseEndsAtMs + CollapsedPulse.gapMs(1)
        assertEquals(thirdStart + 6_000L, second.advance(thirdStart).pulseEndsAtMs)
    }

    @Test
    fun theCrowdedFloorStopsTheCut() {
        val short = { id: String -> crowdedNote(id, holdMs = 3_000L, minHoldMs = 3_000L, crowdedMinHoldMs = 2_200L) }
        val s = threeWaiting(short("n1"), short("n2"), short("n3"))
        val gapEnd = IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(3)
        assertEquals(gapEnd + 2_200L, s.advance(gapEnd).pulseEndsAtMs)
    }

    @Test
    fun anEntryWithoutACrowdedFloorKeepsTheSingleCut() {
        val s = threeWaiting(longNote("n1"), longNote("n2"), longNote("n3"))
        val gapEnd = IslandTimings.LIVE_PULSE_MS + CollapsedPulse.gapMs(3)
        assertEquals(gapEnd + 4_200L, s.advance(gapEnd).pulseEndsAtMs)
    }

    @Test
    fun quickKeepsTimeForItsName() {
        val short = entry(card("w", IslandMode.Bluetooth, category = "wifi").copy(title = "Home"))
        assertEquals(IslandTimings.QUICK_WAITING_MIN_MS, short.minHoldMs)
        assertEquals(IslandTimings.QUICK_CROWDED_MIN_MS, short.crowdedMinHoldMs)

        val mid = entry(card("w", IslandMode.Bluetooth, category = "wifi").copy(title = "x".repeat(35)))
        assertEquals(IslandTimings.QUICK_CROWDED_MIN_MS + IslandTimings.READ_MS_PER_CHAR * 5, mid.crowdedMinHoldMs)

        val long = entry(card("w", IslandMode.Bluetooth, category = "wifi").copy(title = "x".repeat(60)))
        assertEquals(long.holdMs, long.minHoldMs)
        assertEquals(long.holdMs, long.crowdedMinHoldMs)
    }

    @Test
    fun longTextGetsNoCutBeyondTheFirst() {
        val long = entry(noteCard("x".repeat(400)))
        assertTrue(long.crowdedMinHoldMs >= long.holdMs * IslandTimings.WAITING_HOLD_PERCENT / 100)
        val short = entry(noteCard("ok"))
        assertEquals(IslandTimings.NOTIFICATION_CROWDED_MIN_MS, short.crowdedMinHoldMs)
    }

    @Test
    fun crowdedFloorFollowsSpeed() {
        val card = card("w", IslandMode.Bluetooth, category = "wifi").copy(title = "Home")
        val half = CollapsedPulse.entryOf(card, 50)
        assertEquals(entry(card).crowdedMinHoldMs / 2, half.crowdedMinHoldMs)
    }

    @Test
    fun aRingingCallIsNeverShortened() {
        val ringing = Entry("call", Kind.Call, 0, 3_600L, minHoldMs = 1_000L)
        val s = State().arrive(live("a"), 0L).arrive(longNote("n"), 10L).arrive(ringing, 100L)
        assertEquals("call", s.pulsing?.id)
        assertEquals(100L + 3_600L, s.pulseEndsAtMs)
    }

    @Test
    fun aHeldPulseDoesNotEndAndItsEndMovesByTheTimeHeld() {
        val s = State().arrive(live("a"), 0L)
        val held = CollapsedPulse.hold(s, 1_000L)
        assertNull(CollapsedPulse.nextDeadlineMs(held))
        assertEquals("a", held.advance(20_000L).pulsing?.id)

        val back = CollapsedPulse.release(held, 5_000L)
        assertEquals(IslandTimings.LIVE_PULSE_MS + 4_000L, back.pulseEndsAtMs)
        assertNull(back.heldSinceMs)
        assertEquals(back.pulseEndsAtMs, CollapsedPulse.nextDeadlineMs(back))
    }

    @Test
    fun holdNeedsAPulseAndTwoHoldsAreOne() {
        assertEquals(State(), CollapsedPulse.hold(State(), 10L))
        val s = State().arrive(live("a"), 0L)
        val held = CollapsedPulse.hold(s, 100L)
        assertEquals(held, CollapsedPulse.hold(held, 200L))
        assertEquals(s, CollapsedPulse.release(s, 300L))
    }

    @Test
    fun lateStartIsNotHeld() {
        val held = CollapsedPulse.hold(State().arrive(live("a"), 0L), 100L)
        val cut = held.arrive(call(), 200L)
        assertEquals("call", cut.pulsing?.id)
        assertNull(cut.heldSinceMs)
        assertEquals(200L + IslandTimings.LIVE_PULSE_MS, cut.pulseEndsAtMs)
    }

    @Test
    fun speedScalesHoldAndMinimum() {
        val note = card("n", IslandMode.Notification)
        val plugged = card("system_battery", IslandMode.Battery, category = "battery_plugged_in")
        val music = card("m", IslandMode.Music)
        val base = listOf(note, plugged, music).map { CollapsedPulse.entryOf(it) }
        val slow = listOf(note, plugged, music).map { CollapsedPulse.entryOf(it, 150) }
        val fast = listOf(note, plugged, music).map { CollapsedPulse.entryOf(it, 75) }
        for (i in base.indices) {
            assertEquals(base[i].holdMs * 3 / 2, slow[i].holdMs)
            assertEquals(base[i].minHoldMs * 3 / 2, slow[i].minHoldMs)
            assertEquals(base[i].holdMs * 3 / 4, fast[i].holdMs)
            assertEquals(base[i].minHoldMs * 3 / 4, fast[i].minHoldMs)
        }
        assertEquals(IslandTimings.NOTIFICATION_PULSE_MIN_MS * 3 / 2, slow[0].holdMs)
    }

    @Test
    fun speed100ChangesNothing() {
        val cards = listOf(
            card("n", IslandMode.Notification), card("m", IslandMode.Music),
            card("system_battery", IslandMode.Battery, category = "battery_plugged_in")
        )
        cards.forEach { assertEquals(CollapsedPulse.entryOf(it), CollapsedPulse.entryOf(it, 100)) }
    }

    @Test
    fun pulseSpeedNeverTouchesARingingCall() {
        val ringing = card("c", IslandMode.IncomingCall, actions = listOf("Answer", "Decline"))
        assertEquals(CollapsedPulse.entryOf(ringing), CollapsedPulse.entryOf(ringing, 200))
        assertEquals(CollapsedPulse.entryOf(ringing), CollapsedPulse.entryOf(ringing, 75))
    }

    @Test
    fun pulseSpeedKeepsRankIdAndKind() {
        val n = card("n", IslandMode.Notification)
        val a = CollapsedPulse.entryOf(n)
        val b = CollapsedPulse.entryOf(n, 200)
        assertEquals(a.rank, b.rank)
        assertEquals(a.id, b.id)
        assertEquals(a.kind, b.kind)
        assertEquals(a.app, b.app)
    }

    @Test
    fun aSlowedPulseStillCutsBackToItsOwnScaledMinimum() {
        // long notification at 150%: floor 4.5 s, a 6 s hold scaled to 9 s, cut to 70% = 6.3 s while another waits
        val e = Entry("n", Kind.Notification, 50, 9_000L, app = "n", minHoldMs = 4_500L)
        val s = State().arrive(note("first"), 0L).arrive(note("other"), 5L).arrive(e, 10L)
        val inGap = s.advance(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        val gapEnd = inGap.gapEndsAtMs!!
        val started = inGap.advance(gapEnd)
        assertEquals("n", started.pulsing?.id)
        assertEquals(gapEnd + 6_300L, started.pulseEndsAtMs)
    }

    @Test
    fun pulsingChatUpdate() {
        val s0 = State().arrive(note("n"), 0L)
        val again = Entry("n", Kind.Notification, 50, 5_000L, app = "n", minHoldMs = 3_000L)
        val s = CollapsedPulse.refresh(s0, again, 2_000L)
        assertEquals("n", s.pulsing?.id)
        assertEquals(7_000L, s.pulseEndsAtMs)
    }

    @Test
    fun pulseRefreshLimits() {
        val runningS0 = State().arrive(live("a", holdMs = 4_000L), 0L)
        assertEquals(4_000L, CollapsedPulse.refresh(runningS0, live("a", holdMs = 1_000L), 500L).pulseEndsAtMs)

        val capped = CollapsedPulse.refresh(State().arrive(note("n"), 0L), note("n"), 9_000L)
        assertEquals(IslandTimings.PULSE_EXTEND_CAP_MS, capped.pulseEndsAtMs)

        val queuedS0 = State().arrive(live("a"), 0L).arrive(note("n"), 100L)
        val fresh = Entry("n", Kind.Notification, 50, 5_000L, app = "n", minHoldMs = 3_000L)
        val queued = CollapsedPulse.refresh(queuedS0, fresh, 900L)
        assertEquals(5_000L, queued.queue.single().entry.holdMs)
        assertEquals(900L, queued.queue.single().arrivedAtMs)
        assertEquals(queuedS0.pulseEndsAtMs, queued.pulseEndsAtMs)

        val absentS0 = State().arrive(live("a"), 0L)
        assertEquals(absentS0, CollapsedPulse.refresh(absentS0, note("x"), 10L))

        val held = CollapsedPulse.hold(State().arrive(note("n"), 0L), 1_000L)
        val heldS = CollapsedPulse.refresh(held, Entry("n", Kind.Notification, 50, 5_000L, app = "n"), 1_500L)
        assertEquals(held.pulseEndsAtMs, heldS.pulseEndsAtMs)
        assertEquals(held.pulseEndsAtMs, CollapsedPulse.ensureHold(held, 9_000L, 1_500L).pulseEndsAtMs)
    }

    @Test
    fun theScrollNeedExtendsTheRunningPulseWithinTheCap() {
        val s0 = State().arrive(live("a"), 0L)
        assertEquals(6_000L, CollapsedPulse.ensureHold(s0, 5_000L, 1_000L).pulseEndsAtMs)
        assertEquals(s0.pulseEndsAtMs, CollapsedPulse.ensureHold(s0, 1_000L, 1_000L).pulseEndsAtMs)
        assertEquals(IslandTimings.PULSE_EXTEND_CAP_MS, CollapsedPulse.ensureHold(s0, 30_000L, 1_000L).pulseEndsAtMs)
        assertEquals(State(), CollapsedPulse.ensureHold(State(), 5_000L, 1_000L))
    }
}
