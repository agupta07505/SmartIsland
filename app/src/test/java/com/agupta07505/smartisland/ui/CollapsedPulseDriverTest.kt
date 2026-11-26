/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.IslandTimings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollapsedPulseDriverTest {

    private fun card(
        key: String,
        mode: IslandMode = IslandMode.Notification,
        text: String = "",
        category: String? = null,
        ringing: Boolean = false,
        quiet: Boolean = false
    ) = IslandNotification(
        key = key,
        packageName = "pkg.$key",
        appName = key,
        title = key,
        text = text,
        timeMillis = 0L,
        category = category,
        mode = mode,
        actionIntents = if (ringing) listOf(IslandNotificationAction("Answer", null)) else emptyList(),
        quietArrival = quiet
    )

    private fun TestScope.driver() = CollapsedPulseDriver(backgroundScope, { testScheduler.currentTime })

    private fun CollapsedPulseDriver.show(vararg cards: IslandNotification, canPulse: Boolean = true) =
        cardsChanged(cards.toList(), cards.toList(), canPulse)

    private fun TestScope.pass(ms: Long) {
        advanceTimeBy(ms + 1)
        runCurrent()
    }

    private fun TestScope.settledNotification(driver: CollapsedPulseDriver): IslandNotification {
        val notification = card("n")
        driver.show(notification)
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        return notification
    }

    @Test
    fun settleRunningEndsPulse() = runTest {
        val d = driver()
        val a = card("a")
        d.show(a)
        assertEquals(CollapsedPulse.entryOf(a).id, d.state.value.pulsing?.id)

        assertTrue(d.settleRunning())
        assertNull(d.state.value.pulsing)
        assertTrue(CollapsedPulse.entryOf(a).id in d.state.value.settled)
    }

    @Test
    fun settleRunningTakesQueued() = runTest {
        val d = driver()
        val a = card("a")
        val b = card("b", IslandMode.Music)
        d.show(a, b)
        assertTrue(d.settleRunning())
        assertNull(d.state.value.pulsing)
        assertTrue(d.state.value.queue.isEmpty())

        pass(IslandTimings.PULSE_GAP_MS * 4)
        assertNull(d.state.value.pulsing)
        assertTrue(CollapsedPulse.entryOf(b).id in d.state.value.settled)
    }

    @Test
    fun settleRunningIdleNoop() = runTest {
        val d = driver()
        assertFalse(d.settleRunning())
    }

    @Test
    fun repulseOfASettledCardPulsesItAgain() = runTest {
        val d = driver()
        val n = settledNotification(d)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)

        d.repulse(listOf(n), canPulse = true)
        assertEquals("n", d.state.value.pulsing?.id)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun repulseObeysCanPulse() = runTest {
        val d = driver()
        val n = settledNotification(d)

        d.repulse(listOf(n), canPulse = false)
        assertNull(d.state.value.pulsing)
        assertFalse(d.state.value.busy)
    }

    @Test
    fun repulseOfOpenCardSilent() = runTest {
        val d = driver()
        val n = settledNotification(d)

        d.repulse(listOf(n), canPulse = true, openId = CollapsedPulse.entryOf(n).id)
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun freshCardPulsesThenSettles() = runTest {
        val d = driver()
        d.show(card("n"))
        assertEquals("n", d.state.value.pulsing?.id)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun queuedEventsPlayInOrderAfterTheGap() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n"))
        assertEquals("music", d.state.value.pulsing?.id)

        pass(IslandTimings.LIVE_PULSE_MS)
        assertTrue(d.state.value.inGap)

        pass(IslandTimings.PULSE_GAP_MS)
        assertEquals("n", d.state.value.pulsing?.id)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
    }

    @Test
    fun unpulsableSettlesForGood() = runTest {
        val d = driver()
        d.show(card("n"), canPulse = false)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)

        d.show(card("n"))
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun briefHideKeepsSettled() = runTest {
        val d = driver()
        val n = settledNotification(d)

        d.cardsChanged(emptyList(), listOf(n), canPulse = true)
        assertTrue("n" in d.state.value.settled)

        d.show(n)
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun leftCardPulsesOnReturn() = runTest {
        val d = driver()
        val n = settledNotification(d)

        d.cardsChanged(emptyList(), emptyList(), canPulse = true)
        assertTrue(d.state.value.settled.isEmpty())

        d.show(n)
        assertEquals("n", d.state.value.pulsing?.id)
    }

    @Test
    fun sameRepostNoPulse() = runTest {
        val d = driver()
        d.show(card("n", text = "hello"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)

        d.show(card("n", text = "hello"))
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun newMessagePulsesAgain() = runTest {
        val d = driver()
        d.show(card("n", text = "first"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)

        d.show(card("n", text = "second"))
        assertEquals("n", d.state.value.pulsing?.id)
    }

    @Test
    fun quickValueChangeNoPulse() = runTest {
        val d = driver()
        d.show(card("b", IslandMode.Battery, text = "15%", category = "battery_plugged_in"))
        pass(IslandTimings.PLUGGED_IN_CARD_MS)
        assertFalse(d.state.value.busy)

        d.show(card("b", IslandMode.Battery, text = "14%", category = "battery_plugged_in"))
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun unpulsableChangeStaysSettled() = runTest {
        val d = driver()
        d.show(card("n", text = "first"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)

        d.show(card("n", text = "second"), canPulse = false)
        assertNull(d.state.value.pulsing)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun cardMayExpandWhenNothingPulses() = runTest {
        val d = driver()
        assertTrue(d.claimExpand(card("n")))
        assertTrue("n" in d.state.value.settled)

        d.show(card("n"))
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun firstPulsedMayExpand() = runTest {
        val d = driver()
        d.show(card("n"))
        assertTrue(d.state.value.busy)

        assertTrue(d.claimExpand(card("n")))
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun expandDuringMusicSettlesIt() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music))

        assertTrue(d.claimExpand(card("n")))
        assertFalse(d.state.value.busy)
        assertEquals(setOf("music", "n"), d.state.value.settled)
    }

    @Test
    fun queuedCardMayExpand() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n"))
        assertTrue(d.claimExpand(card("n")))
        assertTrue(d.state.value.queue.isEmpty())
        assertFalse(d.state.value.busy)
    }

    @Test
    fun urgentPulseBlocksExpansion() = runTest {
        val d = driver()
        val navigation = card("nav", IslandMode.Navigation)
        d.show(navigation)

        assertFalse(d.claimExpand(card("n")))
        assertFalse("n" in d.state.value.settled)

        d.show(navigation, card("n"))
        assertEquals(listOf("n"), d.state.value.upNext.map { it.entry.id })
    }

    @Test
    fun heldCardWaitsForRelease() = runTest {
        val d = driver()
        val n = card("n")
        d.cardsChanged(listOf(n), listOf(n), true, hold = { true })
        assertNull(d.state.value.pulsing)
        assertFalse("n" in d.state.value.settled)

        d.cardsChanged(listOf(n), listOf(n), true)
        assertEquals("n", d.state.value.pulsing?.id)
    }

    @Test
    fun heldOpenedCardNeverPulses() = runTest {
        val d = driver()
        val n = card("n")
        d.cardsChanged(listOf(n), listOf(n), true, hold = { true })
        assertTrue(d.claimExpand(n))

        d.cardsChanged(listOf(n), listOf(n), true)
        assertNull(d.state.value.pulsing)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun ringingCallExpandsTakesAll() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n1"), card("n2"))

        val call = card("call", IslandMode.IncomingCall, ringing = true)
        assertTrue(d.claimExpand(call))
        assertFalse(d.state.value.busy)
        assertTrue(d.state.value.queue.isEmpty())
        assertEquals(setOf("music", "n1", "n2", "call"), d.state.value.settled)
    }

    @Test
    fun pulsedRingingMayExpand() = runTest {
        val d = driver()
        val call = card("call", IslandMode.IncomingCall, ringing = true)
        d.show(call)
        assertEquals("call", d.state.value.pulsing?.id)

        assertTrue(d.claimExpand(call))
        assertFalse(d.state.value.busy)
    }

    @Test
    fun settleAllStopsPulsing() = runTest {
        val d = driver()
        val music = card("music", IslandMode.Music)
        val n = card("n")
        d.show(music, n)

        d.settleAll()
        assertFalse(d.state.value.busy)
        pass(IslandTimings.LIVE_PULSE_MS + IslandTimings.PULSE_GAP_MS)
        assertNull(d.state.value.pulsing)

        d.show(music, n)
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun pulseEndsByItself() = runTest {
        val d = driver()
        d.show(card("n"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS - 100)
        assertTrue(d.state.value.busy)
        pass(200)
        assertFalse(d.state.value.busy)
    }

    @Test
    fun deadlineKeptOnValueChange() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music, text = "0:01"))
        val due = CollapsedPulse.nextDeadlineMs(d.state.value)

        d.show(card("music", IslandMode.Music, text = "0:02"))
        assertEquals(due, CollapsedPulse.nextDeadlineMs(d.state.value))
    }

    @Test
    fun openCardNeverPulsesOverItself() = runTest {
        val d = driver()
        val open = card("open")
        d.cardsChanged(listOf(open), listOf(open), canPulse = true, openId = "open")

        assertNull(d.state.value.pulsing)
        assertTrue("open" in d.state.value.settled)
    }

    @Test
    fun anotherCardPulsesOverTheOpenOne() = runTest {
        val d = driver()
        val open = card("open")
        val other = card("other")
        d.cardsChanged(listOf(open, other), listOf(open, other), canPulse = true, openId = "open")

        assertEquals("other", d.state.value.pulsing?.id)
        assertTrue("open" in d.state.value.settled)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
    }

    @Test
    fun repostOfTheOpenCardStaysSettled() = runTest {
        val d = driver()
        val open = card("open", text = "one")
        d.cardsChanged(listOf(open), listOf(open), canPulse = true, openId = "open")
        val newer = card("open", text = "two")
        d.cardsChanged(listOf(newer), listOf(newer), canPulse = true, openId = "open")

        assertFalse(d.state.value.busy)
    }

    @Test
    fun quietCardSettlesDirectly() = runTest {
        val d = driver()
        d.show(card("timer", IslandMode.Timer, quiet = true))

        assertNull(d.state.value.pulsing)
        assertFalse(d.state.value.busy)
        assertTrue(d.state.value.queue.isEmpty())
        assertTrue("timer" in d.state.value.settled)
    }

    @Test
    fun loudCardAfterAQuietOneStillPulses() = runTest {
        val d = driver()
        val quiet = card("timer", IslandMode.Timer, quiet = true)
        d.show(quiet)
        d.show(quiet, card("n"))

        assertEquals("n", d.state.value.pulsing?.id)
        assertTrue("timer" in d.state.value.settled)
    }

    @Test
    fun quietCardSkipsQueue() = runTest {
        val d = driver()
        val music = card("music", IslandMode.Music)
        d.show(music)
        d.show(music, card("sw", IslandMode.Stopwatch, quiet = true))

        assertEquals("music", d.state.value.pulsing?.id)
        assertTrue(d.state.value.queue.isEmpty())
        assertTrue("sw" in d.state.value.settled)
    }

    @Test
    fun repostOfAQuietCardChangesNothing() = runTest {
        val d = driver()
        d.show(card("timer", IslandMode.Timer, text = "1:00", quiet = true))
        d.show(card("timer", IslandMode.Timer, text = "0:59", quiet = true))

        assertFalse(d.state.value.busy)
        assertTrue("timer" in d.state.value.settled)
    }

    @Test
    fun quietFlagOnRepostIgnored() = runTest {
        val d = driver()
        d.show(card("n"))
        assertEquals("n", d.state.value.pulsing?.id)

        d.show(card("n", quiet = true))
        assertEquals("n", d.state.value.pulsing?.id)
    }

    @Test
    fun quietAfterLeaveLoudWhenNew() = runTest {
        val d = driver()
        d.show(card("wifi", IslandMode.Bluetooth, quiet = true))
        d.show()
        val again = card("wifi", IslandMode.Bluetooth)
        d.show(again)

        // system cards share 1 key, their pulse id carries the event too
        assertEquals(CollapsedPulse.entryOf(again).id, d.state.value.pulsing?.id)
    }

    @Test
    fun timerFinishPulsesAfterQuiet() = runTest {
        val d = driver()
        val running = card("timer", IslandMode.Timer, quiet = true)
        d.show(running)
        assertNull(d.state.value.pulsing)

        d.show(running.copy(text = "Time's up", timerFinished = true))
        assertEquals("timer", d.state.value.pulsing?.id)
    }

    @Test
    fun finishedTimerIsRearmedOnlyOnce() = runTest {
        val d = driver()
        val running = card("timer", IslandMode.Timer, quiet = true)
        val done = running.copy(text = "Time's up", timerFinished = true)
        d.show(running)
        d.show(done)
        pass(IslandTimings.LIVE_PULSE_MS)
        assertFalse(d.state.value.busy)

        d.show(done.copy(text = "Time's up!"))
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun timerBornFinishedPulses() = runTest {
        val d = driver()
        d.show(card("timer", IslandMode.Timer, quiet = true).copy(timerFinished = true))

        assertEquals("timer", d.state.value.pulsing?.id)
    }

    @Test
    fun restartedTimerFinishesAgain() = runTest {
        val d = driver()
        val running = card("timer", IslandMode.Timer, quiet = true)
        val done = running.copy(timerFinished = true)
        d.show(running)
        d.show(done)
        pass(IslandTimings.LIVE_PULSE_MS)

        d.show(running)
        assertNull(d.state.value.pulsing)
        d.show(done)
        assertEquals("timer", d.state.value.pulsing?.id)
    }

    @Test
    fun pulseTapSelection() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n"))
        assertEquals("music", d.tapTarget())

        assertEquals("music", d.claimExpandByTap())
        assertFalse(d.state.value.busy)
        assertTrue(d.state.value.queue.isEmpty())
        assertTrue("n" in d.state.value.settled)

        // the settled card never pulses later
        pass(IslandTimings.LIVE_PULSE_MS + IslandTimings.PULSE_GAP_MS)
        assertNull(d.state.value.pulsing)
    }

    @Test
    fun gapTapClaimsLastPulsed() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n"))
        pass(IslandTimings.LIVE_PULSE_MS)
        assertTrue(d.state.value.inGap)

        assertEquals("music", d.claimExpandByTap())
        assertFalse(d.state.value.busy)
    }

    @Test
    fun pulseEndsOnFrozenClock() = runTest {
        // a clock that never moves (SystemClock in a unit test): the timer must not start itself again forever
        val d = CollapsedPulseDriver(backgroundScope, { 0L })
        d.show(card("n"))
        assertEquals("n", d.state.value.pulsing?.id)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun tapWithNothingBusyClaimsNothing() = runTest {
        val d = driver()
        d.show(card("n"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
        assertNull(d.claimExpandByTap())
    }

    @Test
    fun tapAfterRemovalClaimsNothing() = runTest {
        val d = driver()
        val music = card("music", IslandMode.Music)
        val n = card("n")
        d.show(music, n)
        pass(IslandTimings.LIVE_PULSE_MS)
        d.cardsChanged(listOf(n), listOf(n), canPulse = true)
        assertNull(d.claimExpandByTap())
    }

    @Test
    fun fingerHoldsPulseUntilLift() = runTest {
        val d = driver()
        d.show(card("n"))
        d.pause()
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS * 3)
        assertEquals("n", d.state.value.pulsing?.id)

        d.resume()
        assertTrue(d.state.value.busy)
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)
        assertTrue("n" in d.state.value.settled)
    }

    @Test
    fun aHeldPulseKeepsTheNextOneWaiting() = runTest {
        val d = driver()
        d.show(card("music", IslandMode.Music), card("n"))
        pass(1_000L)
        d.pause()
        pass(10_000L)
        assertEquals("music", d.state.value.pulsing?.id)
        assertEquals(listOf("n"), d.state.value.upNext.map { it.entry.id })

        d.resume()
        pass(IslandTimings.LIVE_PULSE_MS + IslandTimings.PULSE_GAP_MS)
        assertEquals("n", d.state.value.pulsing?.id)
    }

    @Test
    fun pauseResumeIdleNoop() = runTest {
        val d = driver()
        d.pause()
        assertNull(d.state.value.heldSinceMs)
        d.resume()
        assertFalse(d.state.value.busy)
    }

    @Test
    fun heldTapOpensLiftNoop() = runTest {
        val d = driver()
        d.show(card("n"))
        d.pause()
        assertEquals("n", d.claimExpandByTap())
        assertFalse(d.state.value.busy)
        d.resume()
        assertFalse(d.state.value.busy)
        assertNull(d.state.value.heldSinceMs)
    }

    @Test
    fun theHoldFollowsThePulseSpeedSetting() = runTest {
        var percent = 200
        val d = CollapsedPulseDriver(backgroundScope, { testScheduler.currentTime }, { percent })
        d.show(card("n"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertEquals("n", d.state.value.pulsing?.id)

        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS)
        assertFalse(d.state.value.busy)

        // the setting is read again for the next card
        percent = 75
        d.show(card("m"))
        pass(IslandTimings.NOTIFICATION_PULSE_MIN_MS * 3 / 4)
        assertFalse(d.state.value.busy)
    }
}
