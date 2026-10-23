/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyDraftsTest {
    @Test
    fun aDraftIsKeptPerNotification() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.set("b", "other")
        assertEquals("hello", d.get("a"))
        assertEquals("other", d.get("b"))
        assertEquals("", d.get("c"))
    }

    @Test
    fun emptyTextRemovesTheDraft() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.set("a", "")
        assertEquals("", d.get("a"))
    }

    @Test
    fun clearEndsTheDraft() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.clear("a")
        assertEquals("", d.get("a"))
    }

    @Test
    fun draftsOfGoneNotificationsAreForgotten() {
        val d = ReplyDrafts()
        d.set("a", "keep")
        d.set("b", "drop")
        d.retainOnly(setOf("a"))
        assertEquals("keep", d.get("a"))
        assertEquals("", d.get("b"))
    }
}
