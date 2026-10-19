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

class BatteryEstimateTest {

    @Test
    fun fastChargingNeedsMoreThanSevenAndAHalfWatts() {
        // 3 A at 9 V = 27 W.
        assertEquals(BatteryEstimate.Status.FastCharging, BatteryEstimate.status(true, false, 3_000_000, 9_000_000))
        // 1 A at the default 5 V = 5 W.
        assertEquals(BatteryEstimate.Status.Charging, BatteryEstimate.status(true, false, 1_000_000, -1))
        // Unknown charger power reads as plain charging.
        assertEquals(BatteryEstimate.Status.Charging, BatteryEstimate.status(true, false, -1, -1))
    }

    @Test
    fun fullAndDischargingWinOverChargerPower() {
        assertEquals(BatteryEstimate.Status.Full, BatteryEstimate.status(true, true, 3_000_000, 9_000_000))
        assertEquals(BatteryEstimate.Status.Discharging, BatteryEstimate.status(false, false, 3_000_000, 9_000_000))
    }

    @Test
    fun timeUntilEmptyFromChargeAndDraw() {
        // 2000 mAh left at 500 mA = 4 h.
        assertEquals(240L, BatteryEstimate.minutesUntilEmpty(2_000_000L, -500_000L))
        // Same readings from a kernel that reports mA / mAh.
        assertEquals(240L, BatteryEstimate.minutesUntilEmpty(2_000L, -500L))
    }

    @Test
    fun timeUntilEmptyIsUnknownWithoutReadings() {
        assertNull(BatteryEstimate.minutesUntilEmpty(0L, -500_000L))
        assertNull(BatteryEstimate.minutesUntilEmpty(2_000_000L, 0L))
        // 2000 mAh at 10 mA would be 200 h: implausible, so hidden.
        assertNull(BatteryEstimate.minutesUntilEmpty(2_000_000L, -10_000L))
    }

    @Test
    fun formatsHoursAndMinutes() {
        assertEquals("4 h 10 m", BatteryEstimate.formatMinutes(250L))
        assertEquals("35 m", BatteryEstimate.formatMinutes(35L))
    }
}
