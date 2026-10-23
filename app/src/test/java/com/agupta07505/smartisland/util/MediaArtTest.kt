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
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaArtTest {

    private fun music(title: String, text: String = "Artist") = IslandNotification(
        key = "music", packageName = "pkg.music", appName = "Music", title = title, text = text,
        mode = IslandMode.Music, timeMillis = 1L, mediaIsPlaying = true
    )

    @Test
    fun sameTrackSameBlurSharesOneCacheKeyEvenWhenThePositionTicks() {
        val a = music("Song A")
        val ticked = a.copy(mediaPositionMs = 42_000L)
        assertEquals(MediaArt.keyFor(a, 24f), MediaArt.keyFor(ticked, 24f))
    }

    @Test
    fun aNewTrackOrADifferentBlurNeedsItsOwnBackground() {
        val a = music("Song A")
        assertNotEquals(MediaArt.keyFor(a, 24f), MediaArt.keyFor(music("Song B"), 24f))
        assertNotEquals(MediaArt.keyFor(a, 24f), MediaArt.keyFor(a, 8f))
        assertNotEquals(MediaArt.keyFor(a, 24f), MediaArt.keyFor(a, null))
    }
}
