/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoEventsTest {

    @Test
    fun everyEventIsListed() {
        assertEquals(IslandEvent.entries.size, DemoEvents.available.size)
    }

    @Test
    fun sampleKeysAreRecognisedAndUnique() {
        val keys = DemoEvents.available.map { DemoEvents.keyFor(it) }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { DemoEvents.isDemoKey(it) })
        assertFalse(DemoEvents.isDemoKey("system_battery"))
    }

    @Test
    fun batteryWarningSamplesFollowTheConfiguredLength() {
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.LOW_BATTERY, 2_500L))
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.BATTERY_SAVER, 2_500L))
        assertEquals(2_500L, DemoEvents.lifetimeMs(IslandEvent.FULLY_CHARGED, 2_500L))
        // Other brief samples keep their own fixed lengths.
        assertEquals(IslandTimings.PLUGGED_IN_CARD_MS, DemoEvents.lifetimeMs(IslandEvent.PLUGGED_IN, 2_500L))
        assertEquals(0L, DemoEvents.lifetimeMs(IslandEvent.MUSIC, 2_500L))
    }
}
