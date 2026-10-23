/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryDischargeTest {
    private val hour = 3_600_000L

    @Test fun microampsGiveHoursLeft() {
        // 2000 mAh left at a 500 mA draw (500000 uA) = 4 hours.
        assertEquals(4 * hour, WidgetIslandContent.dischargeRemainingMs(2_000_000L, -500_000L))
    }

    @Test fun milliampDevicesAreReadAsMilliamps() {
        assertEquals(4 * hour, WidgetIslandContent.dischargeRemainingMs(2_000_000L, 500L))
    }

    @Test fun signOfTheCurrentDoesNotMatter() {
        assertEquals(
            WidgetIslandContent.dischargeRemainingMs(1_000_000L, 250_000L),
            WidgetIslandContent.dischargeRemainingMs(1_000_000L, -250_000L)
        )
    }

    @Test fun missingOrSillyReadingsGiveNoForecast() {
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(0L, 500_000L))
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(2_000_000L, 0L))
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(2_000_000L, Long.MIN_VALUE))
        // A tiny draw would promise weeks: not a forecast.
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(4_000_000L, 1L))
    }
}
