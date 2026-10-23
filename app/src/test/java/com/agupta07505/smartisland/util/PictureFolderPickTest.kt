/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PictureFolderPickTest {
    private val posted = 1_000_000_000_000L // ms
    private val now = posted / 1000

    private fun line(m: Long, a: Long, c: Long, path: String) = "$m|$a|$c|$path"

    @Test
    fun aReusedStickerFileCountsThroughItsNewestTime() {
        // modified a week ago, but accessed / changed when the sticker arrived again
        val listing = line(now - 600_000, now - 5, now - 5, "/x/WhatsApp Stickers/abc.webp")
        val pick = PictureFolderPick.pick(listing, posted, sticker = true)
        assertNotNull(pick)
        assertEquals("/x/WhatsApp Stickers/abc.webp", pick!!.path)
    }

    @Test
    fun aStickerWithNoExtensionIsAccepted() {
        val listing = line(now, now, now, "/x/WhatsApp Stickers/3f9c2a")
        assertNotNull(PictureFolderPick.pick(listing, posted, sticker = true))
    }

    @Test
    fun aStickerMustLiveInAStickerFolder() {
        val listing = line(now, now, now, "/x/WhatsApp Images/IMG-1.webp")
        assertNull(PictureFolderPick.pick(listing, posted, sticker = true))
    }

    @Test
    fun sentStickersAndThumbnailsAreSkipped() {
        val listing = listOf(
            line(now, now, now, "/x/WhatsApp Stickers/Sent/a.webp"),
            line(now, now, now, "/x/WhatsApp Stickers/thumb/b.webp")
        ).joinToString("\n")
        assertNull(PictureFolderPick.pick(listing, posted, sticker = true))
    }

    @Test
    fun aPhotoStillNeedsARecentModifiedTime() {
        val listing = line(now - 600_000, now - 5, now - 5, "/x/WhatsApp Images/IMG-1.jpg")
        assertNull(PictureFolderPick.pick(listing, posted, sticker = false))
    }

    @Test
    fun theNewestFileWins() {
        val listing = listOf(
            line(now - 30, now, now, "/x/WhatsApp Stickers/old.webp"),
            line(now - 3, now, now, "/x/WhatsApp Stickers/new.webp")
        ).joinToString("\n")
        assertEquals("/x/WhatsApp Stickers/new.webp", PictureFolderPick.pick(listing, posted, sticker = true)!!.path)
    }

    @Test
    fun brokenLinesAreIgnored() {
        assertEquals(0, PictureFolderPick.parse("garbage\n12|x", sticker = true).size)
    }
}
