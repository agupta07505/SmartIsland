/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TransferState
import com.agupta07505.smartisland.model.TransferTickHold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryEntryTest {

    @Test
    fun entryCreationAndDefaults() {
        val entry = NotificationHistoryEntry(
            id = 1L,
            notificationKey = "key_1",
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            title = "Alice",
            text = "Hello!",
            postTimeMillis = 1700000000000L
        )

        assertEquals(1L, entry.id)
        assertEquals("key_1", entry.notificationKey)
        assertEquals("org.telegram.messenger", entry.packageName)
        assertEquals("Telegram", entry.appName)
        assertEquals("Alice", entry.title)
        assertEquals("Hello!", entry.text)
        assertEquals(null, entry.subText)
        assertEquals(1700000000000L, entry.postTimeMillis)
        assertEquals(null, entry.category)
        assertEquals(null, entry.channelId)
        assertEquals("Notification", entry.mode)
        assertEquals(emptyList<String>(), entry.actionTitles)
        assertFalse(entry.wasOpened)
        assertFalse(entry.wasDismissed)
    }

    @Test
    fun entryCopyWithModifiedStatus() {
        val entry = NotificationHistoryEntry(
            id = 2L,
            notificationKey = "key_2",
            packageName = "com.spotify.music",
            appName = "Spotify",
            title = "Song Name",
            text = "Artist Name",
            postTimeMillis = 1700000000000L,
            actionTitles = listOf("Play", "Next"),
            mode = "Music"
        )

        val updated = entry.copy(wasOpened = true)
        assertTrue(updated.wasOpened)
        assertEquals(2L, updated.id)
        assertEquals("Music", updated.mode)
        assertEquals(listOf("Play", "Next"), updated.actionTitles)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransferTickHoldTest {

    /** The repository on the test scheduler's clock, so the one-second hold passes in virtual time */
    private fun TestScope.repository() =
        SmartIslandNotificationRepository(scope = backgroundScope, nowMs = { CLOCK_START + currentTime })

    private fun card(key: String, mode: IslandMode, progress: Int = 0, max: Int = 0, state: TransferState = TransferState.ACTIVE) =
        IslandNotification(
            key = key, packageName = "com.test", appName = "App", title = "t", text = "x",
            mode = mode, timeMillis = 0L, progress = progress, progressMax = max, transferState = state
        )

    @Test
    fun holdLastsOneSecond() {
        val hold = TransferTickHold()
        hold.begin("d", 100L)
        assertTrue(hold.active(1_099L))
        assertFalse(hold.active(1_100L))
        assertEquals(400L, hold.remainingMs(700L))
    }

    @Test
    fun otherCardWaitsForTheTickThenAppears() = runTest {
        val repo = repository()
        repo.postNotification(card("d", IslandMode.DownloadUpload, 100, 100, TransferState.COMPLETE))
        repo.postNotification(card("n", IslandMode.Notification))
        assertEquals(listOf("d"), repo.notifications.value.map { it.key })
        advanceTimeBy(1_300L)
        runCurrent()
        assertEquals(listOf("d", "n"), repo.notifications.value.map { it.key })
    }

    @Test
    fun removalOfTheFinishedCardWaitsForTheTick() = runTest {
        val repo = repository()
        repo.postNotification(card("d", IslandMode.DownloadUpload, 100, 100))
        repo.removeNotification("d")
        assertEquals(listOf("d"), repo.notifications.value.map { it.key })
        advanceTimeBy(1_300L)
        runCurrent()
        assertTrue(repo.notifications.value.isEmpty())
    }

    @Test
    fun cardRemovedWhileWaitingNeverShows() = runTest {
        val repo = repository()
        repo.postNotification(card("d", IslandMode.DownloadUpload, 100, 100))
        repo.postNotification(card("n", IslandMode.Notification))
        repo.removeNotification("n")
        advanceTimeBy(1_300L)
        runCurrent()
        assertEquals(listOf("d"), repo.notifications.value.map { it.key })
    }
}

private const val CLOCK_START = 10_000L
