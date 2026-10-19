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

class EarbudBatteryTest {

    @Test
    fun readsLeftRightAndCase() {
        assertEquals(EarbudBattery(80, 75, 40), EarbudBattery.parse("Connected • L 80% • R 75% • Case 40%"))
        assertEquals(EarbudBattery(90, 85, null), EarbudBattery.parse("Left: 90%, Right: 85%"))
    }

    @Test
    fun singleLevelKeepsTheSingleGauge() {
        assertNull(EarbudBattery.parse("Connected • 80%"))
        assertNull(EarbudBattery.parse("Case 40%"))
        assertNull(EarbudBattery.parse(null))
    }

    @Test
    fun ignoresOutOfRangeLevels() {
        assertNull(EarbudBattery.parse("L 180% • R 75%"))
    }
}
