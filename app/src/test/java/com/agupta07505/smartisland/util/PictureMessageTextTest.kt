/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureMessageTextTest {
    @Test
    fun pictureMessagesAreDroppedAndTextMessagesKept() {
        val text = textWithoutPictureMessages(
            listOf(
                MessageLine("see you at 6", hasPicture = false),
                MessageLine("Photo", hasPicture = true),
                MessageLine("look at this caption", hasPicture = true),
                MessageLine("  nice!  ", hasPicture = false)
            )
        )
        assertEquals("see you at 6\nnice!", text)
    }

    @Test
    fun onlyPictureMessagesGivesEmptyText() {
        assertEquals("", textWithoutPictureMessages(listOf(MessageLine("Sticker", true))))
    }

    @Test
    fun keepsOnlyTheLastSixTextMessages() {
        val lines = (1..8).map { MessageLine("m$it", false) }
        assertEquals("m3\nm4\nm5\nm6\nm7\nm8", textWithoutPictureMessages(lines))
    }

    @Test
    fun bareLabelsAreRecognised() {
        assertTrue(isPictureLabel("Photo"))
        assertTrue(isPictureLabel("📷 Photo"))
        assertTrue(isPictureLabel("Sticker"))
        assertTrue(isPictureLabel("2 photos"))
        assertTrue(isPictureLabel("  "))
    }

    @Test
    fun realTextIsNotALabel() {
        assertFalse(isPictureLabel("Photo from the trip"))
        assertFalse(isPictureLabel("Lunch at noon?"))
    }

    @Test
    fun previewShowsTheLastNonBlankLine() {
        assertEquals("nice!", lastReaderLine("see you at 6\n nice! \n\n"))
        assertEquals("", lastReaderLine(""))
    }
}
