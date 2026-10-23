/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.expandableNotificationText
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextExpansionTest {

    @Test
    fun longerBodyReplacesShortTextWhenExpanded() {
        assertEquals("the whole message", expandableNotificationText("the whole", "the whole message"))
    }

    @Test
    fun noBodyOrSameBodyKeepsShortText() {
        assertEquals("hello", expandableNotificationText("hello", ""))
        assertEquals("hello", expandableNotificationText("hello", "   "))
        assertEquals("hello", expandableNotificationText("hello", "hello"))
    }

    @Test
    fun shorterBodyIsIgnored() {
        assertEquals("a long preview line", expandableNotificationText("a long preview line", "short"))
    }
}
