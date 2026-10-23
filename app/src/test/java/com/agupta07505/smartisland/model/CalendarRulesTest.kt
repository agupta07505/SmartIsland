/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRulesTest {
    private val now = 1_000_000_000L
    private val min = 60_000L

    @Test fun islandLooksAheadOneDay() {
        assertTrue(CalendarRules.islandShows(now + CalendarRules.LOOK_AHEAD_MS, now))
        assertFalse(CalendarRules.islandShows(now + CalendarRules.LOOK_AHEAD_MS + 1, now))
        assertFalse(CalendarRules.islandShows(now - 1, now))
    }

    @Test fun pickNextTakesTheSoonestThatHasNotBegun() {
        val past = NextEvent("Past", now - min, now + min)
        val later = NextEvent("Later", now + 90 * min, now + 120 * min)
        val soon = NextEvent("Soon", now + 10 * min, now + 40 * min)
        assertEquals(soon, CalendarRules.pickNext(listOf(past, later, soon), now))
        assertNull(CalendarRules.pickNext(listOf(past), now))
        assertNull(CalendarRules.pickNext(emptyList(), now))
    }

    @Test fun shortTitleCutsTidiesAndNeverReturnsNothing() {
        assertEquals("Standup", CalendarRules.shortTitle("  Standup ", 9))
        assertEquals("Team  sync".replace("  ", " "), CalendarRules.shortTitle("Team   sync", 9))
        assertEquals("Quarterly\u2026", CalendarRules.shortTitle("Quarterly planning", 10))
        assertEquals("Event", CalendarRules.shortTitle("   ", 9))
    }
}
