/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryEntryTest {

    @Test
    fun testEntryCreationAndDefaults() {
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
    fun testEntryCopyWithModifiedStatus() {
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
