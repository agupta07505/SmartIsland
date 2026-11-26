/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.util.IdleCloseSequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandViewModelIdleTest : IslandViewModelTestBase() {

    @Test
    fun staticCardsIdleOutAfterTimeout() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // a pulse running holds the idle timeout, it counts (the default 5 s) once the pulse is over
        letPulseFinish()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(IslandTimings.STATIC_IDLE_TIMEOUT_MS + IslandCardFeed.IDLE_TIMER_EXTRA_MS)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(IslandMode.Empty, viewModel.mode.value)
        // the card itself is kept; only the island view is idle
        assertEquals(1, notifRepo.notifications.value.size)
    }

    @Test
    fun staticNotificationRetention() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a"))
        notifRepo.postNotification(
            IslandNotification(
                key = "timer", packageName = "com.clock", appName = "Clock", title = "Timer", text = "1:00",
                mode = IslandMode.Timer, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun pausedMediaIdleTimeout() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = false))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(1, notifRepo.notifications.value.size)

        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // paused again: a fresh countdown starts
        notifRepo.postNotification(music(playing = false))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun pausedIdleOutRunningStay() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        fun stopwatch(paused: Boolean) = IslandNotification(
            key = "sw", packageName = "com.clock", appName = "Clock", title = "Stopwatch", text = "0:12",
            mode = IslandMode.Stopwatch, recordingPaused = paused, timeMillis = 1L
        )
        notifRepo.postNotification(stopwatch(paused = false))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        notifRepo.postNotification(stopwatch(paused = true))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun runningKeepsPausedMedia() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = false))
        notifRepo.postNotification(
            IslandNotification(
                key = "nav", packageName = "com.maps", appName = "Maps", title = "Turn left", text = "200 m",
                mode = IslandMode.Navigation, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun updatedStaticReappears() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        notifRepo.postNotification(chat("a", "second"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun idleCutoutStoresLiveCards() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        fun nav(text: String) = IslandNotification(
            key = "nav", packageName = "com.maps", appName = "Maps", title = "Turn left", text = text,
            mode = IslandMode.Navigation, timeMillis = 1L
        )
        notifRepo.postNotification(nav("200 m"))
        notifRepo.postNotification(music(playing = true))
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, viewModel.visibleNotifications.value.size)

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)

        // a live card that only ticks stays away; a brand new event shows
        notifRepo.postNotification(nav("150 m"))
        notifRepo.postNotification(chat("b", "new"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("b"), viewModel.visibleNotifications.value.map { it.key })
    }

    @Test
    fun idleTapBringsCardsBack() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun tapBackLiveNoAutoTimer() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        viewModel.onIdleDotTapped() // reveal by the user
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // the user opened it: a playing (live) card stays as long as he wants
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    private fun hotspot() = IslandNotification(
        key = "hot", packageName = "android", appName = "Hotspot", title = "Hotspot", text = "1 device",
        mode = IslandMode.Hotspot, timeMillis = 1L
    )

    @Test
    fun selfArrivedLiveIdlesOut() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(hotspot())
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        // the timer counts once the card's pulse is over
        letPulseFinish()
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(IslandTimings.STATIC_IDLE_TIMEOUT_MS + IslandCardFeed.IDLE_TIMER_EXTRA_MS)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)
        // it is only put away: still in the repository
        assertEquals(1, notifRepo.notifications.value.size)
        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun selfArrivedPlayingStays() = runTest {
        // playing media is running like a timer: only a paused one idles out
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun autoCollapseWaitsOnPull() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a", "first"))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // the finger lands on the pull and stays there well past the 5 s countdown
        viewModel.setAutoCollapseHeld(true)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(20_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        // letting go starts the countdown again from zero
        viewModel.setAutoCollapseHeld(false)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(4_500L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        testDispatcher.scheduler.advanceTimeBy(1_000L)
        testDispatcher.scheduler.runCurrent()
        Assert.assertFalse(viewModel.expanded.value)
    }

    @Test
    fun liveTimerWaitsWhenOpen() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        viewModel.setAutoCollapsePaused(true)
        testDispatcher.scheduler.runCurrent()

        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun callsNeverTimeOut() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(
            IslandNotification(
                key = "call", packageName = "com.dialer", appName = "Phone", title = "Ann", text = "Ongoing call",
                mode = IslandMode.IncomingCall, timeMillis = 1L
            )
        )
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(60_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
    }

    @Test
    fun idleTapWhenOpenCollapses() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }

    @Test
    fun idleTapClosesInTwoSteps() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()
        val plan = IdleCloseSequence.plan(
            viewModel.settings.value.animationSpeed(viewModel.orientation.value.isLandscape)
        )

        // the island closes (and the secondary merges)
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.expanded.value)
        assertTrue(viewModel.idleClosing.value)
        assertFalse(viewModel.idleLeaving.value)
        assertTrue(viewModel.idleTapActive.value)
        testDispatcher.scheduler.advanceTimeBy(plan.step1Ms / 2L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertTrue(viewModel.idleClosing.value)

        // the cards stay through this phase, i.e. they only go once it ends
        testDispatcher.scheduler.advanceTimeBy(plan.step1Ms / 2L + 20L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertFalse(viewModel.idleClosing.value)
        assertTrue(viewModel.idleLeaving.value)

        // everything is back to rest once the exit has played
        testDispatcher.scheduler.advanceTimeBy(plan.step2Ms + IslandCardFeed.IDLE_TAP_ACTIVE_MARGIN_MS + 100L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleLeaving.value)
        assertFalse(viewModel.idleTapActive.value)
    }

    @Test
    fun reopenInStepOneKeepsCards() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(50L)
        testDispatcher.scheduler.runCurrent()
        viewModel.expand()
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.expanded.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.idleClosing.value)
        assertFalse(viewModel.idleLeaving.value)
    }

    @Test
    fun idleTapCollapsedExitSpring() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()

        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleClosing.value)
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.idleLeaving.value)
    }

    @Test
    fun revealTapFlag() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)

        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.idleTapActive.value)
        assertEquals(1, viewModel.visibleNotifications.value.size)
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)
    }

    @Test
    fun swipeRevealNoTapFlag() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()

        viewModel.revealIdleCards(tapAnimation = false)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.idleTapActive.value)
    }

    @Test
    fun switchReachesPutAwayCards() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(appCard("a1", "app.a"))
        notifRepo.postNotification(music(playing = true))
        testDispatcher.scheduler.runCurrent()
        viewModel.onIdleDotTapped()
        testDispatcher.scheduler.advanceTimeBy(3_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())

        // a card that arrives later is the only one on the island, the 2 put away are still entries
        notifRepo.postNotification(appCard("b1", "app.b"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("b1"), viewModel.visibleNotifications.value.map { it.key })

        assertTrue(viewModel.switchNotification(forward = true))
        testDispatcher.scheduler.advanceTimeBy(500L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, viewModel.visibleNotifications.value.size)
        assertNotEquals("b1", viewModel.visibleNotifications.value[viewModel.selectedIndex.value].key)
    }

    private fun transfer(kind: TransferKind, state: TransferState) =
        IslandNotification(
            key = "t-$kind", packageName = "com.files", appName = "Files", title = "file.zip", text = "",
            mode = IslandMode.DownloadUpload, transferKind = kind, transferState = state, timeMillis = 1L
        )

    @Test
    fun pausedTransfersIdleOut() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        val kinds = TransferKind.DOWNLOAD to TransferKind.UPLOAD
        val active = TransferState.ACTIVE
        val paused = TransferState.PAUSED

        notifRepo.postNotification(transfer(kinds.first, active))
        notifRepo.postNotification(transfer(kinds.second, active))
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.advanceTimeBy(5_900L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.visibleNotifications.value.size)

        notifRepo.postNotification(transfer(kinds.first, paused))
        notifRepo.postNotification(transfer(kinds.second, paused))
        testDispatcher.scheduler.runCurrent()
        letPulseFinish()
        // the 2 cards pulse one after the other and the idle timeout counts after the last pulse
        testDispatcher.scheduler.advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertEquals(2, notifRepo.notifications.value.size)
    }

    @Test
    fun idleCutoutRestoresCards() = runTest {
        val (viewModel, notifRepo) = newViewModel()
        notifRepo.postNotification(chat("a"))
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.hasIdleStoredCards.value)

        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
        assertTrue(viewModel.hasIdleStoredCards.value)

        viewModel.revealIdleCards()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.visibleNotifications.value.size)
        assertFalse(viewModel.hasIdleStoredCards.value)

        // normal behaviour: the idle countdown runs again
        testDispatcher.scheduler.advanceTimeBy(11_100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.visibleNotifications.value.isEmpty())
    }
}
