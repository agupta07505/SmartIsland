/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EventColorsTest {
    private fun notif(mode: IslandMode, category: String? = null) =
        IslandNotification(key = "k", packageName = "p", appName = "a", title = "t", text = "x", timeMillis = 0L,
            mode = mode, category = category)

    @Test
    fun eventColorCodec() {
        val colors = mapOf(IslandEvent.LOW_BATTERY.id to 0xFF112233L, IslandEvent.USB_CONNECTED.id to 0xFFABCDEFL)
        assertEquals(colors, EventColors.decode(EventColors.encode(colors)))

        val decoded = EventColors.decode(setOf("nope=4278190080", "wifi=banana", "wifi", "=5", "wifi=0", "vpn=4278190335"))
        assertEquals(mapOf("vpn" to 4278190335L), decoded)
        assertEquals(emptyMap<String, Long>(), EventColors.decode(null))
    }

    @Test
    fun noColorsKeepsUsualColor() {
        val settings = SmartIslandSettings.Default
        assertSame(settings, settings.forCard(notif(IslandMode.Battery, "battery_low")))
        assertNull(settings.eventColorFor(notif(IslandMode.Battery, "battery_low")))
        assertNull(settings.cardEventColor)
    }

    @Test
    fun eventColorReachesOwnCard() {
        val settings = SmartIslandSettings.Default.copy(eventColors = mapOf(IslandEvent.LOW_BATTERY.id to 0xFF123456L))
        val low = settings.forCard(notif(IslandMode.Battery, "battery_low"))
        assertEquals(0xFF123456L, low.batteryColor)
        assertEquals(0xFF123456L, low.cardEventColor)
        // another battery event has no colour of its own and keeps the group's
        val full = settings.forCard(notif(IslandMode.Battery, "battery_full"))
        assertSame(settings, full)
        assertEquals(SmartIslandSettings.Default.batteryColor, full.batteryColor)
    }

    @Test
    fun eventColorIsolation() {
        val settings = SmartIslandSettings.Default.copy(eventColors = mapOf(IslandEvent.USB_CONNECTED.id to 0xFF778899L))
        assertEquals(0xFF778899L, settings.forCard(notif(IslandMode.Bluetooth, "usb_connected")).bluetoothColor)
        // USB out keeps the red it starts with, USB in's colour does not reach it
        assertEquals(EventColors.defaultFor(IslandEvent.USB_DISCONNECTED), settings.eventColorFor(notif(IslandMode.Bluetooth, "usb_disconnected")))
        assertNull(settings.eventColorFor(notif(IslandMode.Bluetooth, "wifi")))
    }

    @Test
    fun disconnectColorOverrides() {
        val red = 0xFFEF4444L
        val plain = SmartIslandSettings.Default
        assertEquals(red, plain.eventColorFor(notif(IslandMode.Bluetooth, "usb_disconnected")))
        assertEquals(red, plain.eventColorFor(notif(IslandMode.Bluetooth, "headphones_disconnected")))
        val delete = IslandNotification(key = "k", packageName = "p", appName = "a", title = "t", text = "x",
            timeMillis = 0L, mode = IslandMode.DownloadUpload, transferKind = TransferKind.DELETE)
        assertEquals(red, plain.eventColorFor(delete))
        assertNull(plain.eventColorFor(notif(IslandMode.Bluetooth, "usb_connected")))
        // nothing is stored for a default, so a backup of untouched settings stays empty
        assertEquals(emptyMap<String, Long>(), plain.eventColors)
        val own = plain.copy(eventColors = mapOf(IslandEvent.USB_DISCONNECTED.id to 0xFF123456L))
        assertEquals(0xFF123456L, own.eventColorFor(notif(IslandMode.Bluetooth, "usb_disconnected")))
    }

    @Test
    fun theColorIsNotPartOfWhatIsExported() {
        val settings = SmartIslandSettings.Default.copy(cardEventColor = 0xFF010203L)
        assertEquals(
            SmartIslandSettings.fromJson(SmartIslandSettings.Default.toJson()).eventColors,
            SmartIslandSettings.fromJson(settings.toJson()).eventColors
        )
    }

    @Test
    fun theColorsSurviveABackup() {
        val colors = mapOf(IslandEvent.DND.id to 0xFF0A0B0CL, IslandEvent.FACE_UNLOCK.id to 0xFFFEDCBAL)
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings.Default.copy(eventColors = colors).toJson())
        assertEquals(colors, restored.eventColors)
    }
}

class FontMixTest {

    @Test
    fun slotsCoverEveryWeightOnce() {
        val all = FontSlot.entries.flatMap { it.weights }
        assertEquals((1..9).map { it * 100 }, all.sorted())
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun representativeWeightOwnSlot() {
        FontSlot.entries.forEach { slot ->
            assertTrue(slot.representativeWeight in slot.weights)
        }
    }

    @Test
    fun anEmptySlotUsesTheIslandFont() {
        assertEquals("lora", FontMix.effectiveId("", "lora"))
        assertEquals("poppins", FontMix.effectiveId("poppins", "lora"))
        assertEquals("custom:SF Pro Display Light.otf", FontMix.effectiveId("custom:SF Pro Display Light.otf", "system"))
    }

    @Test
    fun idOfReadsTheSlotsOwnField() {
        assertEquals("a", FontMix.idOf(FontSlot.Light, "a", "b", "c"))
        assertEquals("b", FontMix.idOf(FontSlot.Regular, "a", "b", "c"))
        assertEquals("c", FontMix.idOf(FontSlot.Medium, "a", "b", "c"))
    }

    @Test
    fun mixNeedsSwitchAndOwnFont() {
        assertFalse(FontMix.isActive(enabled = false, light = "poppins", regular = "", medium = ""))
        assertFalse(FontMix.isActive(enabled = true, light = "", regular = "", medium = ""))
        assertTrue(FontMix.isActive(enabled = true, light = "", regular = "", medium = "lora"))
        assertTrue(FontMix.isActive(enabled = true, light = "poppins", regular = "lora", medium = "inter_display"))
    }
}

class StateWordsTest {
    private fun card(mode: IslandMode, category: String?, text: String = "") = IslandNotification(
        key = "k", packageName = "pkg", appName = "App", title = "Title", text = text, timeMillis = 0L,
        mode = mode, category = category
    )

    private fun connection(category: String) = card(IslandMode.Bluetooth, category)

    @Test
    fun connectionStateWords() {
        assertEquals(StateWord.Connected, StateWords.of(connection("wifi")))
        assertEquals(StateWord.Disconnected, StateWords.of(connection("wifi_disconnected")))
        assertEquals(StateWord.Connected, StateWords.of(connection("vpn")))
        assertEquals(StateWord.Disconnected, StateWords.of(connection("vpn_disconnected")))
        assertEquals(StateWord.Connected, StateWords.of(connection("usb_connected")))
        assertEquals(StateWord.Disconnected, StateWords.of(connection("usb_disconnected")))

        assertEquals(StateWord.PluggedIn, StateWords.of(connection("headphones_connected")))
        assertEquals(StateWord.Unplugged, StateWords.of(connection("headphones_disconnected")))

        val cases = listOf(
            DeviceStates.AIRPLANE_ON to StateWord.On,
            DeviceStates.AIRPLANE_OFF to StateWord.Off,
            DeviceStates.RINGER_RING to StateWord.On,
            DeviceStates.RINGER_VIBRATE to StateWord.On,
            DeviceStates.RINGER_SILENT to StateWord.On
        )
        for ((input, expected) in cases) {
            assertEquals("$input", expected, StateWords.of(input))
        }

        val kind = ModeKind.entries.first()
        for (fromRule in listOf(false, true)) {
            val on = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = fromRule, filter = 0)
            val off = ModeStatus(false, kind, DndModes.defaultName(kind), fromRule = fromRule, filter = 0)
            val started = DeviceStates.change(off, on)!!
            val ended = DeviceStates.change(on, off)!!
            assertEquals(StateWord.On, StateWords.of(started.category))
            assertEquals(StateWord.Off, StateWords.of(ended.category))
        }

        val cards = listOf(DeviceStates.airplane(true), DeviceStates.airplane(false)) +
            listOf(0, 1, 2).mapNotNull { DeviceStates.ringer(it) }
        for (c in cards) assertEquals(c.category, true, StateWords.of(c.category) != null)
    }


    @Test
    fun otherCardsKeepTheirOwnValue() {
        assertNull(StateWords.of(connection("bluetooth")))
        assertNull(StateWords.of(card(IslandMode.Bluetooth, null)))
        assertNull(StateWords.of(card(IslandMode.Battery, "battery_plugged_in", "71%")))
        assertNull(StateWords.of(card(IslandMode.Hotspot, null)))
        assertNull(StateWords.of(card(IslandMode.Timer, null)))
        assertNull(StateWords.of(card(IslandMode.Notification, "wifi")))
    }

    @Test
    fun disconnectedGlyphs() {
        assertTrue(StateWords.slashed("usb_disconnected"))
        assertTrue(StateWords.slashed("headphones_disconnected"))
        val kind = ModeKind.entries.first()
        val on = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        val off = ModeStatus(false, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        assertTrue(StateWords.slashed(DeviceStates.change(on, off)!!.category))
    }

    @Test
    fun theGlyphIsNotSlashedForTheOthers() {
        for (category in listOf("wifi", "wifi_disconnected", "vpn", "vpn_disconnected", "usb_connected",
            "headphones_connected", "bluetooth", DeviceStates.AIRPLANE_ON, DeviceStates.AIRPLANE_OFF,
            DeviceStates.RINGER_RING, DeviceStates.RINGER_VIBRATE, DeviceStates.RINGER_SILENT)) {
            assertFalse(category, StateWords.slashed(category))
        }
        assertFalse(StateWords.slashed(null))
        val kind = ModeKind.entries.first()
        val on = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        val off = ModeStatus(false, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        assertFalse(StateWords.slashed(DeviceStates.change(off, on)!!.category))
        // A plain filter that ended has its own Do Not Disturb off icon
        val filterOn = ModeStatus(true, ModeKind.Dnd, "", fromRule = false, filter = 0)
        val filterOff = ModeStatus(false, ModeKind.Dnd, "", fromRule = false, filter = 0)
        assertFalse(StateWords.slashed(DeviceStates.change(filterOn, filterOff)!!.category))
    }

    @Test
    fun connectionTitles() {
        assertEquals("VPN", StateWords.pulseTitle("VPN connected", StateWord.Connected, IslandEvent.VPN))
        assertEquals("VPN", StateWords.pulseTitle("VPN disconnected", StateWord.Disconnected, IslandEvent.VPN_DISCONNECTED))
        assertEquals("Wi-Fi", StateWords.pulseTitle("Wi-Fi - Disconnected", StateWord.Disconnected, IslandEvent.WIFI_DISCONNECTED))
        assertEquals("Earbuds", StateWords.pulseTitle("Earbuds unplugged", StateWord.Unplugged, IslandEvent.HEADPHONES_DISCONNECTED))

        assertEquals("VPN disconnected", StateWords.pulseTitle("VPN disconnected", StateWord.Connected, IslandEvent.VPN))

        assertEquals("Home Wi-Fi", StateWords.pulseTitle("Home Wi-Fi", StateWord.Connected, IslandEvent.WIFI))
        assertEquals("Pixel Buds", StateWords.pulseTitle("Pixel Buds", StateWord.PluggedIn, IslandEvent.HEADPHONES_CONNECTED))

        assertEquals("USB", StateWords.pulseTitle("", StateWord.Connected, IslandEvent.USB_CONNECTED))
        assertEquals("Headphone", StateWords.pulseTitle("Connected", StateWord.Connected, IslandEvent.HEADPHONES_CONNECTED))
        assertEquals("Wi-Fi", StateWords.pulseTitle("", StateWord.Disconnected, IslandEvent.WIFI_DISCONNECTED))

        assertEquals("Hotspot on", StateWords.pulseTitle("Hotspot on", null, IslandEvent.HOTSPOT))
        assertEquals("Airplane mode", StateWords.pulseTitle("Airplane mode", StateWord.On, IslandEvent.AIRPLANE_MODE))
    }
}
