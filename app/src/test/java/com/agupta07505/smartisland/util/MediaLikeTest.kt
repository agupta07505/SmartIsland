/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLikeTest {
    private fun a(id: String, title: String) = MediaCustomAction(id, title)

    @Test
    fun findsFavoritesToggleAmongOtherActions() {
        val actions = listOf(a("rewind", "Rewind"), a("fav", "Add to favorites"), a("close", "Close app"))
        assertEquals("fav", MediaLike.findAction(actions)?.id)
    }

    @Test
    fun noLikeActionReturnsNull() {
        assertNull(MediaLike.findAction(listOf(a("shuffle", "Shuffle mode on/off"), a("next", "Play next song"))))
    }

    @Test
    fun dislikeIsNotATreatedAsLike() {
        assertNull(MediaLike.findAction(listOf(a("dislike", "Dislike"))))
    }

    @Test
    fun labelReportsLikedState() {
        assertFalse(MediaLike.isActive(a("fav", "Add to favorites")))
        assertFalse(MediaLike.isActive(a("like", "Like")))
        assertTrue(MediaLike.isActive(a("fav", "Remove from favorites")))
        assertTrue(MediaLike.isActive(a("unlike", "Unlike")))
        assertTrue(MediaLike.isActive(a("x", "Liked")))
    }

    @Test
    fun matchesOnActionIdWhenTitleIsGeneric() {
        assertEquals("com.app.ACTION_FAVORITE", MediaLike.findAction(listOf(a("com.app.ACTION_FAVORITE", "Heart")))?.id)
    }
}
