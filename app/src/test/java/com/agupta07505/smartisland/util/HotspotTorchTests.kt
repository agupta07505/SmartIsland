/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.HotspotText
import com.agupta07505.smartisland.model.TorchTiming
import com.agupta07505.smartisland.util.HotspotSettingsAutomation.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotspotClientsTest {
    @Test
    fun hotspotNeighbours() {
        val liveOutput = """
            192.168.43.12 dev ap0 lladdr aa:bb:cc:dd:ee:01 REACHABLE
            192.168.43.31 dev ap0 lladdr aa:bb:cc:dd:ee:02 STALE
            192.168.43.77 dev ap0 lladdr aa:bb:cc:dd:ee:02 DELAY
        """.trimIndent()
        assertEquals(2, HotspotClients.countFromNeighbours(liveOutput))

        val filteredOutput = """
            192.168.43.40 dev ap0  FAILED
            192.168.43.41 dev ap0  INCOMPLETE
            fe80::1 dev ap0 lladdr aa:bb:cc:dd:ee:03 REACHABLE
            192.168.1.1 dev wlan0 lladdr aa:bb:cc:dd:ee:04 REACHABLE
            192.168.43.12 dev ap0 lladdr aa:bb:cc:dd:ee:05 REACHABLE
        """.trimIndent()
        assertEquals(1, HotspotClients.countFromNeighbours(filteredOutput))

        assertEquals(0, HotspotClients.countFromNeighbours(""))

        val tetheredOutput = """
            192.168.42.129 dev rndis0 lladdr aa:bb:cc:dd:ee:06 REACHABLE
            192.168.44.2 dev bt-pan lladdr aa:bb:cc:dd:ee:07 STALE
        """.trimIndent()
        assertEquals(2, HotspotClients.countFromNeighbours(tetheredOutput))
    }
}

class HotspotSettingsAutomationTest {
    @Test
    fun hotspotClientCount() {
        val connectedPage = listOf("Hotspot & tethering", "Wi-Fi hotspot", "1 device connected", "USB tethering")
        assertEquals(1, HotspotSettingsAutomation.countOf(connectedPage))
        assertEquals(3, HotspotSettingsAutomation.countOf(listOf("Wi-Fi hotspot", "3 devices connected")))
        assertEquals(0, HotspotSettingsAutomation.countOf(listOf("Wi-Fi hotspot", "No devices connected")))

        val emptyPagePage = listOf(
            "Use Wi-Fi hotspot", "Hotspot name", "Pixel XL", "Turn off hotspot automatically",
            "When no devices are connected", "Speed & compatibility", "2.4 GHz / Compatible with most devices",
            "Connected devices", "List and manage devices connected to the hotspot"
        )
        assertNull(HotspotSettingsAutomation.countOf(emptyPagePage))
    }

    @Test
    fun hotspotSwitchSelection() {
        val enabledItems = listOf(
            Item("Wi-Fi hotspot", isSwitch = false, checked = false, enabled = true),
            Item("", isSwitch = true, checked = true, enabled = true),
            Item("", isSwitch = true, checked = false, enabled = true),
            Item("", isSwitch = true, checked = true, enabled = true)
        )
        assertEquals(1, HotspotSettingsAutomation.firstOnSwitch(enabledItems))

        val unavailableItems = listOf(
            Item("", isSwitch = true, checked = false, enabled = true),
            Item("", isSwitch = true, checked = true, enabled = false),
            Item("on", isSwitch = false, checked = true, enabled = true)
        )
        assertNull(HotspotSettingsAutomation.firstOnSwitch(unavailableItems))
    }
}

class HotspotUtilTest {

    @Test
    fun missingDeviceCount() {
        assertNull(HotspotText.parseDeviceCountOrNull("Hotspot", "Hotspot is on"))
        assertNull(HotspotText.parseDeviceCountOrNull("Hotspot", null))
        assertEquals(0, HotspotText.parseDeviceCountOrNull("Hotspot", "No devices connected"))
        assertEquals(2, HotspotText.parseDeviceCountOrNull("Hotspot", "2 devices connected"))
    }
}

class TorchCapabilitiesTest {
    @Test
    fun torchLevelSupport() {
        assertEquals(1 to 1, TorchCapabilities.levels(32, 5, 3))
        assertEquals(5 to 3, TorchCapabilities.levels(33, 5, 3))
        assertEquals(5 to 5, TorchCapabilities.levels(34, 5, null))
        assertEquals(5 to 5, TorchCapabilities.levels(34, 5, 9))
        assertEquals(1 to 1, TorchCapabilities.levels(34, null, null))
        assertEquals(1 to 1, TorchCapabilities.levels(34, 0, 0))
    }

    @Test
    fun brightnessNeedsMoreThanOneStep() {
        assertFalse(TorchInfo("0", 1, 1).supportsBrightness)
        assertTrue(TorchInfo("0", 5, 3).supportsBrightness)
    }

    @Test
    fun sosCadence() {
        val signalOn = TorchTiming.sosSteps().filter { it.on }.map { it.ms }
        assertEquals(listOf(200L, 200L, 200L, 600L, 600L, 600L, 200L, 200L, 200L), signalOn)
        val last = TorchTiming.sosSteps().last()
        assertFalse(last.on)
        assertEquals(2_000L, last.ms)

        val steps = TorchTiming.sosSteps()
        steps.zipWithNext().forEach { (a, b) -> assertTrue(a.on != b.on) }
    }

}

class TorchPatternsTest {
    @Test
    fun strobeBeatUsesHalfTheClampedFlashPeriod() {
        assertEquals(500_000_000L, TorchTiming.beatNanos(1))
        assertEquals(25_000_000L, TorchTiming.beatNanos(20))
        assertEquals(16_666_666L, TorchTiming.beatNanos(30))
        assertEquals(TorchTiming.beatNanos(1), TorchTiming.beatNanos(0))
        assertEquals(TorchTiming.beatNanos(30), TorchTiming.beatNanos(99))
    }


}
