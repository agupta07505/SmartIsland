/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoneCardGuardTest {

    private fun card(
        key: String,
        pkg: String = "app.one",
        mode: IslandMode = IslandMode.Notification,
        time: Long = 1_000L
    ) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = time, mode = mode
    )

    private val download = card("d", mode = IslandMode.DownloadUpload, time = 5_000L)

    @Test
    fun vanishedCardSelection() {
        val gone = GoneCardGuard.vanished(listOf(download, card("n")), listOf(card("n")), nowMs = 9_000L)
        assertEquals(1, gone.size)
        assertEquals("app.one", gone[0].packageName)

        val finished = card("d", mode = IslandMode.Notification, time = 6_000L)
        assertTrue(GoneCardGuard.vanished(listOf(download), listOf(finished), 9_000L).isEmpty())
        assertTrue(GoneCardGuard.vanished(listOf(download), listOf(download), 9_000L).isEmpty())

        assertTrue(GoneCardGuard.vanished(listOf(card("n")), emptyList(), 9_000L).isEmpty())
        val estimated = download.copy(isEstimatedTransfer = true)
        assertTrue(GoneCardGuard.vanished(listOf(estimated), emptyList(), 9_000L).isEmpty())
    }

    @Test
    fun repostSuppressionWindow() {
        val olderRecent = GoneCardGuard.vanished(listOf(download), emptyList(), 9_000L)
        assertTrue(GoneCardGuard.suppressesArrival(olderRecent, card("old", time = 2_000L), 9_500L))

        val newerRecent = GoneCardGuard.vanished(listOf(download), emptyList(), 9_000L)
        assertFalse(GoneCardGuard.suppressesArrival(newerRecent, card("done", time = 9_200L), 9_500L))

        val unrelatedRecent = GoneCardGuard.vanished(listOf(download), emptyList(), 9_000L)
        assertFalse(GoneCardGuard.suppressesArrival(unrelatedRecent, card("x", pkg = "app.two", time = 2_000L), 9_500L))

        val expiredRecent = GoneCardGuard.vanished(listOf(download), emptyList(), 9_000L)
        val late = 9_000L + GoneCardGuard.WINDOW_MS + 1
        assertFalse(GoneCardGuard.suppressesArrival(expiredRecent, card("old", time = 2_000L), late))
        assertTrue(GoneCardGuard.prune(expiredRecent, late).isEmpty())

        val ahead = card("d", mode = IslandMode.LiveActivity, time = 99_000L)
        val futureRecent = GoneCardGuard.vanished(listOf(ahead), emptyList(), 9_000L)
        assertFalse(GoneCardGuard.suppressesArrival(futureRecent, card("fresh", time = 9_100L), 9_200L))
    }
}

class StaleCardSweepTest {
    private fun card(key: String, mode: IslandMode) = IslandNotification(
        key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L, mode = mode
    )

    @Test
    fun staleCardRemoval() {
        val removedCards = listOf(card("timer", IslandMode.Timer), card("music", IslandMode.Music))
        assertEquals(listOf("timer"), StaleCardSweep.keysToDrop(removedCards, activeKeys = setOf("music")))

        val visibleCards = listOf(card("a", IslandMode.Stopwatch), card("b", IslandMode.Navigation))
        assertEquals(emptyList<String>(), StaleCardSweep.keysToDrop(visibleCards, setOf("a", "b")))
    }

    @Test
    fun islandOnlySystemCardsUntouched() {
        // ordinary notifications, downloads and ringing calls are cancelled from the shade by the island itself, and
        // system events have no notification: none of them can be checked against the shade
        val cards = listOf(
            card("n", IslandMode.Notification), card("d", IslandMode.DownloadUpload), card("c", IslandMode.IncomingCall),
            card("bat", IslandMode.Battery), card("hot", IslandMode.Hotspot), card("bt", IslandMode.Bluetooth),
            card("fl", IslandMode.Flashlight), card("live", IslandMode.LiveActivity)
        )
        assertEquals(emptyList<String>(), StaleCardSweep.keysToDrop(cards, activeKeys = emptySet()))
    }

    @Test
    fun recordersAreCheckedToo() {
        val cards = listOf(card("s", IslandMode.ScreenRecording), card("a", IslandMode.AudioRecording))
        assertEquals(listOf("s", "a"), StaleCardSweep.keysToDrop(cards, emptySet()))
    }

    @Test
    fun cardWithNoKeyIsLeftAlone() {
        assertEquals(emptyList<String>(), StaleCardSweep.keysToDrop(listOf(card("", IslandMode.Timer)), emptySet()))
    }

    @Test
    fun ownStopwatchKept() {
        val cards = listOf(card(InAppStopwatch.KEY, IslandMode.Stopwatch), card("clock", IslandMode.Stopwatch))
        assertEquals(listOf("clock"), StaleCardSweep.keysToDrop(cards, activeKeys = emptySet()))
    }

    @Test
    fun ownTimerKept() {
        val cards = listOf(card(InAppTimer.KEY, IslandMode.Timer), card("clock", IslandMode.Timer))
        assertEquals(listOf("clock"), StaleCardSweep.keysToDrop(cards, activeKeys = emptySet()))
    }
}

class StalePostGuardTest {

    @Test
    fun staleRingingSnapshot() {
        val guard = StalePostGuard()
        guard.notePosted("call", 100L) // ringing
        guard.notePosted("call", 200L) // ongoing
        assertTrue(guard.isStale("call", 100L))
        assertFalse(guard.isStale("call", 200L))
    }

    @Test
    fun removedSnapshotIsStale() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        assertTrue(guard.isStale("call", 200L))
    }

    @Test
    fun repostAfterRemovalIsFine() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        guard.notePosted("call", 300L)
        assertFalse(guard.isStale("call", 300L))
    }

    @Test
    fun unknownKeysNeverStale() {
        val guard = StalePostGuard()
        guard.notePosted("a", 500L)
        assertFalse(guard.isStale("b", 1L))
    }

    @Test
    fun clearForgetsEverything() {
        val guard = StalePostGuard()
        guard.notePosted("call", 200L)
        guard.noteRemoved("call", 200L)
        guard.clear()
        assertFalse(guard.isStale("call", 100L))
    }

    @Test
    fun dismissStopsSlowHandlerRevive() {
        // the user dismissed the card (noted at "now" = 1000) while an older post (950) was still being read
        val guard = StalePostGuard()
        guard.notePosted("msg", 950L)
        guard.noteRemoved("msg", 1_000L)
        assertTrue(guard.isStale("msg", 950L))
        // the app posting the same key again after the dismissal is a new notification and must show
        guard.notePosted("msg", 1_200L)
        assertFalse(guard.isStale("msg", 1_200L))
    }
}
