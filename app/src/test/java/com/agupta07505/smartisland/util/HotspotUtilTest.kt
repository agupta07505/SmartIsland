/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HotspotUtilTest {

    @Test
    fun testParseDeviceCount_whenNoDevicesConnected_returnsZero() {
        assertEquals(0, HotspotUtil.parseDeviceCount("Mobile Hotspot", "Sharing mobile data"))
        assertEquals(0, HotspotUtil.parseDeviceCount("Hotspot active", null))
        assertEquals(0, HotspotUtil.parseDeviceCount("Mobile Hotspot", "No devices connected"))
        assertEquals(0, HotspotUtil.parseDeviceCount("Mobile Hotspot", "0 devices connected"))
        assertEquals(0, HotspotUtil.parseDeviceCount("Personal Hotspot", "Portable hotspot is on"))
    }

    @Test
    fun testParseDeviceCount_whenDevicesConnected_returnsCount() {
        assertEquals(1, HotspotUtil.parseDeviceCount("Mobile Hotspot", "1 device connected"))
        assertEquals(2, HotspotUtil.parseDeviceCount("Mobile Hotspot", "Active • 2 devices connected"))
        assertEquals(3, HotspotUtil.parseDeviceCount("Personal Hotspot", "Connected devices: 3"))
        assertEquals(1, HotspotUtil.parseDeviceCount("Hotspot", "1 connected"))
    }
}
