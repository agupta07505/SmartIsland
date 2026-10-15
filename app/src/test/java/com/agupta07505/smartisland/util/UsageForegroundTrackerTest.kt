/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageForegroundTrackerTest {
    private val ignored: (String) -> Boolean = { it == "me" || it == "ime" }

    private fun UsageForegroundTracker.resume(pkg: String, t: Long) = onEvent(UsageForegroundTracker.RESUMED, pkg, t, ignored)
    private fun UsageForegroundTracker.pause(pkg: String, t: Long) = onEvent(UsageForegroundTracker.PAUSED, pkg, t, ignored)

    @Test fun resumedAppIsInFrontUntilItPauses() {
        val t = UsageForegroundTracker()
        t.resume("chatgpt", 1)
        assertEquals("chatgpt", t.current)
        t.pause("chatgpt", 2)
        assertNull(t.current)
    }

    @Test fun switchingAppsFollowsTheNewestResume() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.pause("a", 2)
        t.resume("b", 2)
        assertEquals("b", t.current)
    }

    @Test fun aLatePauseOfTheOldAppDoesNotEndTheNewOne() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.resume("b", 2)
        t.pause("a", 3)
        assertEquals("b", t.current)
    }

    @Test fun stoppedEndsTheTurnToo() {
        val t = UsageForegroundTracker()
        t.resume("a", 1)
        t.onEvent(UsageForegroundTracker.STOPPED, "a", 2, ignored)
        assertNull(t.current)
    }

    @Test fun ignoredPackagesNeverCountButAdvanceTheClock() {
        val t = UsageForegroundTracker()
        t.resume("chatgpt", 1)
        t.resume("me", 5)
        t.resume("ime", 6)
        assertEquals("chatgpt", t.current)
        assertEquals(6L, t.lastEventTime)
    }

    @Test fun missingPackageIsSkipped() {
        val t = UsageForegroundTracker()
        t.onEvent(UsageForegroundTracker.RESUMED, null, 1, ignored)
        t.onEvent(UsageForegroundTracker.RESUMED, "", 2, ignored)
        assertNull(t.current)
    }
}
