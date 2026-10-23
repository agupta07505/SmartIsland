/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.SmartIslandNotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCardPatchTest {
    private fun card(title: String = "Old song", text: String = "Old artist") = IslandNotification(
        key = "music", packageName = "com.player", appName = "Player", title = title, text = text,
        timeMillis = 1L, mode = IslandMode.Music, mediaPositionMs = 5_000L, mediaDurationMs = 200_000L, mediaIsPlaying = true
    )

    private fun snap(title: String? = "New song", artist: String? = "New artist", playing: Boolean = true) =
        MediaSnapshot("com.player", title, artist, null, 180_000L, 0L, playing)

    @Test
    fun aTrackChangeReplacesTitleArtistAndTimes() {
        val out = MediaCardPatch.apply(card(), snap())
        assertEquals("New song", out.title)
        assertEquals("New artist", out.text)
        assertEquals(180_000L, out.mediaDurationMs)
        assertEquals(0L, out.mediaPositionMs)
    }

    @Test
    fun aNewTrackWithoutArtistDoesNotKeepThePreviousArtist() {
        val out = MediaCardPatch.apply(card(), snap(artist = null))
        assertEquals("New song", out.title)
        assertEquals("", out.text)
    }

    @Test
    fun aPlayStateChangeKeepsTheTrack() {
        val out = MediaCardPatch.apply(card(title = "Same", text = "Same artist"), snap(title = "Same", artist = null, playing = false))
        assertEquals("Same", out.title)
        assertEquals("Same artist", out.text)
        assertFalse(out.mediaIsPlaying)
    }

    @Test
    fun onlyMusicCardsOfThePackageAreTargeted() {
        val s = snap()
        assertTrue(MediaCardPatch.appliesTo(card(), s))
        assertFalse(MediaCardPatch.appliesTo(card().copy(packageName = "other"), s))
        assertFalse(MediaCardPatch.appliesTo(card().copy(mode = IslandMode.Notification), s))
    }

    @Test
    fun repositoryUpdateChangesOneCardInPlaceAndIgnoresUnknownKeys() {
        val repo = SmartIslandNotificationRepository()
        repo.postNotification(card())
        repo.updateNotification("music") { MediaCardPatch.apply(it, snap()) }
        assertEquals("New song", repo.notifications.value.single().title)
        repo.updateNotification("missing") { it.copy(title = "x") }
        assertEquals(1, repo.notifications.value.size)
    }

    @Test
    fun repositoryUpdateWithNoChangeKeepsTheSameList() {
        val repo = SmartIslandNotificationRepository()
        repo.postNotification(card())
        val before = repo.notifications.value
        repo.updateNotification("music") { it }
        assertSame(before, repo.notifications.value)
        assertNull(repo.notifications.value.single().largeIcon)
    }
}
