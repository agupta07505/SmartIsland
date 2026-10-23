/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SentReplyTest {
    private fun card(text: String, big: String = "") = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "Sam", text = text, timeMillis = 0L, bigText = big
    )

    @Test fun addsToShortAndLongText() {
        val c = card("Hello").withSentReply("  Hi back ")
        assertEquals("You: Hi back", c.text)
        assertEquals("Hello\nYou: Hi back", c.bigText)
    }

    @Test fun keepsExistingHistory() {
        val c = card("Two", big = "One\nTwo").withSentReply("Three")
        assertEquals("One\nTwo\nYou: Three", c.bigText)
    }

    @Test fun blankMessageChangesNothing() {
        val c = card("Hello")
        assertEquals(c, c.withSentReply("   "))
    }

    @Test fun secondReplyStacks() {
        val c = card("Hello").withSentReply("a").withSentReply("b")
        assertEquals("Hello\nYou: a\nYou: b", c.bigText)
    }
}
