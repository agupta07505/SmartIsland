/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetUsageTest {
    private val today = NetUsage.dayKey(2026, 9, 18)

    @Test fun dayKeyIsYyyyMmDd() = assertEquals(20261018, today)

    @Test fun splitGivesMobileItsShareAndWifiTheRest() {
        assertEquals(700L to 300L, NetUsage.split(1_000, 300))
        // A mobile reading ahead of the total (the counters are read a moment apart) is clamped.
        assertEquals(0L to 1_000L, NetUsage.split(1_000, 5_000))
        // A counter reset counts as nothing.
        assertEquals(0L to 0L, NetUsage.split(-5, -5))
    }

    @Test fun addsUpWithinTheDayAndStartsAgainTomorrow() {
        var u = DailyUsage()
        u = NetUsage.add(u, today, 500, 100)
        u = NetUsage.add(u, today, 250, 0)
        assertEquals(DailyUsage(today, 750, 100), u)
        val tomorrow = NetUsage.dayKey(2026, 9, 19)
        u = NetUsage.add(u, tomorrow, 10, 20)
        assertEquals(DailyUsage(tomorrow, 10, 20), u)
    }

    @Test fun theCounterFollowsTheCurrentConnection() {
        val u = DailyUsage(today, wifiBytes = 2_000, mobileBytes = 300)
        assertEquals(2_000L, NetUsage.bytesFor(u, NetConnection.Wifi))
        assertEquals(300L, NetUsage.bytesFor(u, NetConnection.Mobile))
        assertEquals(2_000L, NetUsage.bytesFor(u, NetConnection.Other))
        assertEquals(0L, NetUsage.bytesFor(u, NetConnection.None))
    }

    @Test fun sizeTextUsesDecimalUnits() {
        assertEquals("850 KB", NetUsage.sizeText(850_000))
        assertEquals("340 MB", NetUsage.sizeText(340_000_000))
        assertEquals("1.2 GB", NetUsage.sizeText(1_200_000_000))
        assertEquals("12 GB", NetUsage.sizeText(12_400_000_000))
        assertEquals("0 KB", NetUsage.sizeText(-1))
    }

    @Test fun connectionModeFiltersTheWidget() {
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_ANY, NetConnection.None))
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.Wifi))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.Mobile))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.None))
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_MOBILE, NetConnection.Mobile))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_MOBILE, NetConnection.Wifi))
        // Anything unknown is the default (any connection).
        assertEquals(NetSpeedRules.CONN_ANY, NetSpeedRules.normalizeConnection("5G"))
        assertTrue(NetSpeedRules.connectionAllows("5G", NetConnection.Mobile))
    }
}
