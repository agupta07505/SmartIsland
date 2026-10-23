/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Test

class StaleCardSweepTest {
    private fun card(key: String, mode: IslandMode) = IslandNotification(
        key = key, packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L, mode = mode
    )

    @Test
    fun aTimerWhoseNotificationIsGoneIsDropped() {
        val cards = listOf(card("timer", IslandMode.Timer), card("music", IslandMode.Music))
        assertEquals(listOf("timer"), StaleCardSweep.keysToDrop(cards, activeKeys = setOf("music")))
    }

    @Test
    fun cardsStillInTheShadeStay() {
        val cards = listOf(card("a", IslandMode.Stopwatch), card("b", IslandMode.Navigation))
        assertEquals(emptyList<String>(), StaleCardSweep.keysToDrop(cards, setOf("a", "b")))
    }

    @Test
    fun islandOnlyAndSystemCardsAreNeverTouched() {
        // Ordinary notifications, downloads and ringing calls are cancelled from the shade by the island itself, and
        // system events have no notification: none of them can be checked against the shade.
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
    fun aCardWithNoKeyIsLeftAlone() {
        assertEquals(emptyList<String>(), StaleCardSweep.keysToDrop(listOf(card("", IslandMode.Timer)), emptySet()))
    }

    @Test
    fun theIslandsOwnStopwatchHasNoShadeNotificationAndIsKept() {
        val cards = listOf(card(InAppStopwatch.KEY, IslandMode.Stopwatch), card("clock", IslandMode.Stopwatch))
        assertEquals(listOf("clock"), StaleCardSweep.keysToDrop(cards, activeKeys = emptySet()))
    }

    @Test
    fun theIslandsOwnTimerHasNoShadeNotificationAndIsKept() {
        val cards = listOf(card(InAppTimer.KEY, IslandMode.Timer), card("clock", IslandMode.Timer))
        assertEquals(listOf("clock"), StaleCardSweep.keysToDrop(cards, activeKeys = emptySet()))
    }
}
