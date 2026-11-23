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

class PulsePeakTest {
    private fun card(mode: IslandMode, title: String = "", text: String = "", category: String? = null,
            progress: Int = 0, progressMax: Int = 0, key: String = "k") = IslandNotification(
        key = key, packageName = "pkg", appName = "App", title = title, text = text, timeMillis = 0L,
        mode = mode, category = category, progress = progress, progressMax = progressMax
    )

    @Test
    fun progressLineValues() {
        val batteryPeak = PulsePeak.of(card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in"), 0L)
        assertEquals("Charging", batteryPeak.title)
        assertEquals("71%", batteryPeak.value)
        assertEquals(0.71f, batteryPeak.progress!!, 0.001f)

        assertNull(PulsePeak.of(card(IslandMode.Battery, "Battery saver", "On", "battery_saver"), 0L).progress)

        val bluetoothPeak = PulsePeak.of(card(IslandMode.Bluetooth, "Buds Pro", "Connected \u00b7 82%", "bluetooth"), 0L)
        assertEquals(0.82f, bluetoothPeak.progress!!, 0.001f)

        val transferPeak = PulsePeak.of(card(IslandMode.DownloadUpload, "report.zip", "", progress = 64, progressMax = 100), 0L)
        assertEquals("64%", transferPeak.value)
        assertEquals(0.64f, transferPeak.progress!!, 0.001f)

        val timer = card(IslandMode.Timer, "Timer", key = "0|com.clock|9|null|1").copy(timeMillis = 60_000L)
        val timerPeak = PulsePeak.of(timer, 0L)
        assertEquals(PulsePeak.ValueKind.Clock, timerPeak.valueKind)
        assertEquals(1f, timerPeak.progress!!, 0.001f)
    }

    @Test
    fun hiddenNotificationKeepsSettled() {
        val message = card(IslandMode.Notification, "Bob", "Hey there")
        assertTrue(PulsePeak.keepsSettledLook(message, redacted = true))
        assertFalse(PulsePeak.keepsSettledLook(message, redacted = false))
        assertFalse(PulsePeak.keepsSettledLook(null, redacted = true))
    }

    @Test
    fun otherEventsKeepTheirPulseWhenHidden() {
        val battery = card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in")
        assertFalse(PulsePeak.keepsSettledLook(battery, redacted = true))
    }

    @Test
    fun lineTakesRingOffGlyph() {
        val battery = card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in")
        val buds = card(IslandMode.Bluetooth, "Buds Pro", "Connected \u00b7 82%", "bluetooth")
        assertTrue(PulsePeak.hidesGlyphRing(battery, 0L))
        assertTrue(PulsePeak.hidesGlyphRing(buds, 0L))
    }

    @Test
    fun aPulseWithoutALineLeavesTheRing() {
        val saver = card(IslandMode.Battery, "Battery saver", "On", "battery_saver")
        val wifi = card(IslandMode.Bluetooth, "Home", "Connected", "wifi")
        assertFalse(PulsePeak.hidesGlyphRing(saver, 0L))
        assertFalse(PulsePeak.hidesGlyphRing(wifi, 0L))
        assertFalse(PulsePeak.hidesGlyphRing(null, 0L))
    }

    @Test
    fun progressLinePillHeight() {
        val battery = card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in")
        assertTrue(PulsePeak.drawsLine(battery, 0L))
        assertEquals(0f, PulsePeak.extraHeightDp(battery, 0L), 0.0001f)
    }

    @Test
    fun aPulseWithoutALineKeepsTheHeight() {
        val saver = card(IslandMode.Battery, "Battery saver", "On", "battery_saver")
        assertEquals(0f, PulsePeak.extraHeightDp(saver, 0L), 0.0001f)
        assertEquals(0f, PulsePeak.extraHeightDp(null, 0L), 0.0001f)
    }

    @Test
    fun emptyTitleFallsBackToTheEventName() {
        val peak = PulsePeak.of(card(IslandMode.Battery, "", "15%", "battery_low"), 0L)
        assertEquals("Low battery", peak.title)
    }

    @Test
    fun noProgressNoLine() {
        for (category in listOf("wifi", "wifi_disconnected", "vpn", "vpn_disconnected", "usb_connected",
            "usb_disconnected", "headphones_connected", "headphones_disconnected")) {
            val peak = PulsePeak.of(card(IslandMode.Bluetooth, "Home", "Connected", category), 0L)
            assertNull(category, peak.progress)
        }
    }

    @Test
    fun musicLineIsPositionOverDuration() {
        val music = card(IslandMode.Music, "Song").copy(mediaPositionMs = 50_000L, mediaDurationMs = 200_000L)
        val peak = PulsePeak.of(music, 0L)
        assertEquals(PulsePeak.ValueKind.Bars, peak.valueKind)
        assertEquals(0.25f, peak.progress!!, 0.001f)
        assertNull(PulsePeak.of(music.copy(mediaDurationMs = null), 0L).progress)
    }

    @Test
    fun eventPulseContent() {
        val cases = listOf(
            card(IslandMode.Music, "Hello", "Adele") to "Adele",
            card(IslandMode.Music, "Hello") to "",
            card(IslandMode.Notification, "Bob", "Hey") to "",
            card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in") to "",
            card(IslandMode.IncomingCall, "Mom", "Ringing...") to ""
        )
        for ((notification, expected) in cases) {
            assertEquals("$notification", expected, PulsePeak.of(notification, 0L).artist)
        }

        for (mode in listOf(IslandMode.IncomingCall, IslandMode.Hotspot, IslandMode.Flashlight, IslandMode.Stopwatch,
            IslandMode.ScreenRecording, IslandMode.AudioRecording, IslandMode.Notification)) {
            assertNull(mode.name, PulsePeak.of(card(mode, "x", "y"), 0L).progress)
        }

        val peak = PulsePeak.of(card(IslandMode.Notification, "Chat", "A long message"), 0L)
        assertEquals(PulsePeak.ValueKind.None, peak.valueKind)
        assertEquals("", peak.value)
        assertEquals("A long message", peak.message)

        assertEquals("", PulsePeak.of(card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in"), 0L).message)
        assertEquals("", PulsePeak.of(card(IslandMode.IncomingCall, "Mom", "Ringing..."), 0L).message)

        assertFalse(PulsePeak.hasProgressLine(IslandEvent.TIMER_FINISHED))
        val done = card(IslandMode.Timer, "Timer", "Time's up").copy(timerFinished = true)
        assertNull(PulsePeak.of(done, 0L).progress)
    }

    @Test
    fun lineRulesMatchThePlan() {
        assertTrue(PulsePeak.hasProgressLine(IslandEvent.NAVIGATION))
        assertTrue(PulsePeak.hasProgressLine(IslandEvent.TIMER))
        assertFalse(PulsePeak.hasProgressLine(IslandEvent.STOPWATCH))
        assertFalse(PulsePeak.hasProgressLine(IslandEvent.CALL))
        assertFalse(PulsePeak.hasProgressLine(null))
    }

    @Test
    fun mediaKindComesFromTheMediaList() {
        assertEquals(PulsePeak.MediaKind.None, PulsePeak.mediaKindOf(emptyList()))
        assertEquals(PulsePeak.MediaKind.Picture, PulsePeak.mediaKindOf(listOf(false)))
        assertEquals(PulsePeak.MediaKind.Sticker, PulsePeak.mediaKindOf(listOf(true, false)))
    }

    @Test
    fun noPicturesNoMediaKind() {
        val peak = PulsePeak.of(card(IslandMode.Notification, "Bob", "Photo"), 0L)
        assertEquals(PulsePeak.MediaKind.None, peak.mediaKind)
    }

    @Test
    fun deviceTitleOverrides() {
        val cases = listOf(
            card(IslandMode.Bluetooth, "Home Wi-Fi", "Connected", "wifi") to true,
            card(IslandMode.Bluetooth, FallbackTitles.WIFI, "Wi-Fi connected", "wifi") to false,
            card(IslandMode.Bluetooth, "Home Wi-Fi", "Disconnected", "wifi_disconnected") to true,
            card(IslandMode.Bluetooth, FallbackTitles.WIFI, "Disconnected", "wifi_disconnected") to false,
            card(IslandMode.Bluetooth, "Buds Pro", "Connected", "bluetooth") to true,
            card(IslandMode.Bluetooth, "Card reader", "Connected", "usb_connected") to true,
            card(IslandMode.Bluetooth, FallbackTitles.USB_HOST, "Connected", "usb_connected") to false,
            card(IslandMode.Bluetooth, FallbackTitles.USB_DEVICE, "Disconnected", "usb_disconnected") to false,
            card(IslandMode.Bluetooth, "USB-C Earbuds", "Connected", "headphones_connected") to true,
            card(IslandMode.Bluetooth, FallbackTitles.WIRED_HEADPHONE, "Connected", "headphones_connected") to false,
            card(IslandMode.Music, "Midnight City", "M83") to true,
            card(IslandMode.IncomingCall, "Alex Morgan", "Incoming call") to true,
            card(IslandMode.Navigation, "Turn left onto Main St", "250 m") to true,
            card(IslandMode.LiveActivity, "Order arriving", "Courier is 5 min away") to true,
            card(IslandMode.DownloadUpload, "holiday-video.mp4", "62%") to true,
            card(IslandMode.ScreenRecording, "Screen Recorder", "Recording") to true,
            card(IslandMode.AudioRecording, "Voice Memos", "Recording") to true,
            card(IslandMode.ScreenRecording, "", "Recording") to false
        )
        for ((notification, expected) in cases) {
            assertEquals("$notification", expected, PulsePeak.hasCustomTitle(notification))
        }

        val renamed = ModeStatus(true, ModeKind.Driving, "Road trip", fromRule = true)
        assertTrue(PulsePeak.hasCustomTitle(stateCard(DeviceStates.change(ModeStatus.Off, renamed)!!)))
        assertTrue(PulsePeak.hasCustomTitle(stateCard(DeviceStates.change(renamed, ModeStatus.Off)!!)))
        assertFalse(PulsePeak.hasCustomTitle(stateCard(DeviceStates.airplane(true))))
        assertFalse(PulsePeak.hasCustomTitle(stateCard(DeviceStates.airplane(false))))
        listOf(DeviceStates.RINGER_MODE_NORMAL, DeviceStates.RINGER_MODE_VIBRATE, DeviceStates.RINGER_MODE_SILENT)
            .forEach { assertFalse(PulsePeak.hasCustomTitle(stateCard(DeviceStates.ringer(it)!!))) }
        listOf(DndModes.FILTER_PRIORITY, DndModes.FILTER_ALARMS, DndModes.FILTER_NONE).forEach {
            val filter = DndModes.fromFilter(it)
            assertFalse(PulsePeak.hasCustomTitle(stateCard(DeviceStates.change(ModeStatus.Off, filter)!!)))
            assertFalse(PulsePeak.hasCustomTitle(stateCard(DeviceStates.change(filter, ModeStatus.Off)!!)))
        }
    }

    @Test
    fun pulseTitleOverrides() {
        val cases = listOf(
            card(IslandMode.Bluetooth, "Frankfurt, Germany", "VPN connected", "vpn") to true,
            card(IslandMode.Bluetooth, FallbackTitles.VPN, "Secure connection active", "vpn") to false,
            card(IslandMode.Bluetooth, "VPN disconnected", "Traffic is not protected", "vpn_disconnected") to false,
            card(IslandMode.DownloadUpload, FallbackTitles.DOWNLOADING, "Network activity") to false,
            card(IslandMode.Music, "", "M83") to false,
            card(IslandMode.Bluetooth, " ", "Connected", "bluetooth") to false,
            card(IslandMode.Timer, "Boil the pasta", "09:30") to true,
            card(IslandMode.Timer, FallbackTitles.TIMER, "09:30") to false,
            card(IslandMode.Timer, "Boil the pasta", "Time's up").copy(timerFinished = true) to true,
            card(IslandMode.Timer, FallbackTitles.TIMER, "Time's up").copy(timerFinished = true) to false,
            card(IslandMode.Stopwatch, "Interval run", "Running") to true,
            card(IslandMode.Stopwatch, FallbackTitles.STOPWATCH, "Running") to false,
            card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in") to false,
            card(IslandMode.Battery, "Low Battery", "15%", "battery_low") to false,
            card(IslandMode.Hotspot, "Hotspot", "Hotspot is on", "hotspot") to false,
            card(IslandMode.Notification, "Sam", "Are we still on for tonight?") to false,
            card(IslandMode.Empty, "x") to false
        )
        for ((notification, expected) in cases) {
            assertEquals("$notification", expected, PulsePeak.hasCustomTitle(notification))
        }
    }

    private fun stateCard(state: DeviceStates.Card) =
        card(IslandMode.Bluetooth, state.title, state.text, state.category)

    @Test
    fun shownCharsTitlePlusClipped() {
        val long = "x".repeat(400)
        assertEquals(3 + PulsePeak.MESSAGE_MAX_CHARS, PulsePeak.shownChars(card(IslandMode.Notification, "Bob", long)))
        assertEquals(3 + 5, PulsePeak.shownChars(card(IslandMode.Notification, "Bob", "Hello")))
        assertEquals(3 + PulsePeak.MESSAGE_MAX_CHARS, PulsePeak.shownChars(card(IslandMode.Music, "Bob", long)))
        assertEquals(3 + 5, PulsePeak.shownChars(card(IslandMode.Music, "Bob", "Adele")))
        assertEquals(3, PulsePeak.shownChars(card(IslandMode.Hotspot, "Bob", long)))
        assertEquals(
            IslandEvent.LOW_BATTERY.shortLabel.length,
            PulsePeak.shownChars(card(IslandMode.Battery, "", "", "battery_low"))
        )
    }

    @Test
    fun connectionPulseText() {
        val cases = mapOf(
            "wifi" to StateWord.Connected, "wifi_disconnected" to StateWord.Disconnected,
            "vpn" to StateWord.Connected, "vpn_disconnected" to StateWord.Disconnected,
            "usb_connected" to StateWord.Connected, "usb_disconnected" to StateWord.Disconnected,
            "headphones_connected" to StateWord.PluggedIn, "headphones_disconnected" to StateWord.Unplugged
        )
        for ((category, word) in cases) {
            val peak = PulsePeak.of(card(IslandMode.Bluetooth, "Home", "Connected", category), 0L)
            assertEquals(category, PulsePeak.ValueKind.State, peak.valueKind)
            assertEquals(category, word, peak.stateWord)
            assertEquals(category, "", peak.value)
        }

        val buds = PulsePeak.of(card(IslandMode.Bluetooth, "Buds Pro", "Connected \u00b7 82%", "bluetooth"), 0L)
        assertNull(buds.stateWord)
        assertEquals(PulsePeak.ValueKind.Reading, buds.valueKind)
        assertEquals("82%", buds.value)
        val battery = PulsePeak.of(card(IslandMode.Battery, "Charging", "71%", "battery_plugged_in"), 0L)
        assertNull(battery.stateWord)
        assertEquals("71%", battery.value)
        val message = PulsePeak.of(card(IslandMode.Notification, "Bob", "Hey", "wifi"), 0L)
        assertNull(message.stateWord)
        assertEquals(PulsePeak.ValueKind.None, message.valueKind)
    }

    @Test
    fun eventPulseLabels() {
        val peak = PulsePeak.of(card(IslandMode.Bluetooth, "VPN disconnected", "Traffic is not protected", "vpn_disconnected"), 0L)
        assertEquals(StateWord.Disconnected, peak.stateWord)
        assertEquals("", peak.value)

        val on = PulsePeak.of(card(IslandMode.Bluetooth, "Airplane mode", "On", DeviceStates.AIRPLANE_ON), 0L)
        val deviceStateOff = PulsePeak.of(card(IslandMode.Bluetooth, "Airplane mode", "Off", DeviceStates.AIRPLANE_OFF), 0L)
        assertEquals(StateWord.On, on.stateWord)
        assertEquals(StateWord.Off, deviceStateOff.stateWord)
        assertEquals(PulsePeak.ValueKind.State, on.valueKind)

        val song = PulsePeak.of(card(IslandMode.Music, "Song", "Some Artist"), 0L)
        assertEquals("Some Artist", song.artist)
        val artistDp = 90f
        val barsDp = 14f
        val fit = PulseWidth.fitPerSide(120f, 30f, 40f, PulseWidth.valueSideDp(barsDp, artistDp, SLOT_PAIR_GAP_DP), 400f)
        // The layer sees the hole from the moved centre, so the shift is the settled one less the pill's own move
        val rooms = PulseWidth.rooms(fit.widthDp, 30f, -fit.centreShiftDp)
        val reserved = PulseWidth.artistMaxDp(rooms.rightDp - SLOT_END_PADDING_DP, barsDp, SLOT_PAIR_GAP_DP)
        assertEquals(artistDp, reserved, 0.001f)

        val vpn = PulsePeak.of(card(IslandMode.Bluetooth, "VPN connected", "Secure connection active", "vpn"), 0L)
        assertEquals("VPN", vpn.title)
        assertEquals(StateWord.Connected, vpn.stateWord)
        val lost = PulsePeak.of(card(IslandMode.Bluetooth, "", "", "usb_disconnected"), 0L)
        assertEquals("USB", lost.title)
        assertEquals(StateWord.Disconnected, lost.stateWord)

        val torch = PulsePeak.of(card(IslandMode.Flashlight, "Flashlight ON", "Tap to turn off"), 0L)
        assertEquals("Flashlight", torch.title)
    }

    @Test
    fun stateWordValueSlots() {
        val categories = listOf("wifi", "wifi_disconnected", "vpn", "vpn_disconnected", "usb_connected",
            "usb_disconnected", "headphones_connected", "headphones_disconnected", DeviceStates.AIRPLANE_ON,
            DeviceStates.AIRPLANE_OFF, DeviceStates.RINGER_RING, DeviceStates.RINGER_VIBRATE,
            DeviceStates.RINGER_SILENT)
        for (category in categories) {
            val peak = PulsePeak.of(card(IslandMode.Bluetooth, "Title", "Text", category), 0L)
            assertTrue(category, peak.hasValue)
        }
        assertFalse(PulsePeak.of(card(IslandMode.Notification, "Bob", "Hey"), 0L).hasValue)
        assertTrue(PulsePeak.of(card(IslandMode.Bluetooth, "Buds", "Connected", "bluetooth"), 0L).hasValue)
    }

    @Test
    fun stateWordEventTypes() {
        val wordEvents = setOf(
            IslandEvent.WIFI, IslandEvent.WIFI_DISCONNECTED, IslandEvent.VPN, IslandEvent.VPN_DISCONNECTED,
            IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED, IslandEvent.HEADPHONES_CONNECTED,
            IslandEvent.HEADPHONES_DISCONNECTED, IslandEvent.AIRPLANE_MODE, IslandEvent.RING, IslandEvent.VIBRATE,
            IslandEvent.SILENT, IslandEvent.DND, IslandEvent.MODE
        )
        val kind = ModeKind.entries.first()
        val on = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        val off = ModeStatus(false, kind, DndModes.defaultName(kind), fromRule = true, filter = 0)
        val filterOn = ModeStatus(true, ModeKind.Dnd, "", fromRule = false, filter = 0)
        val categories = listOf("wifi", "wifi_disconnected", "vpn", "vpn_disconnected", "usb_connected",
            "usb_disconnected", "headphones_connected", "headphones_disconnected", "bluetooth", DeviceStates.AIRPLANE_ON,
            DeviceStates.AIRPLANE_OFF, DeviceStates.RINGER_RING, DeviceStates.RINGER_VIBRATE, DeviceStates.RINGER_SILENT,
            DeviceStates.change(off, on)!!.category, DeviceStates.change(on, off)!!.category,
            DeviceStates.change(ModeStatus(false), filterOn)!!.category)
        for (category in categories) {
            val c = card(IslandMode.Bluetooth, "Title", "Text", category)
            assertEquals(category, IslandEvent.forNotification(c) in wordEvents, StateWords.of(c) != null)
        }
    }

    @Test
    fun longArtistGetsLeftoverRoom() {
        val barsDp = 14f
        val max = PulseWidth.MaxWidth(150f, 100f)
        val fit = PulseWidth.fitPerSide(120f, 30f, 40f, PulseWidth.valueSideDp(barsDp, 300f, SLOT_PAIR_GAP_DP), max)
        val rooms = PulseWidth.rooms(fit.widthDp, 30f, -fit.centreShiftDp)
        val reserved = PulseWidth.artistMaxDp(rooms.rightDp - SLOT_END_PADDING_DP, barsDp, SLOT_PAIR_GAP_DP)
        // 100 to the right edge, less the hole's half, the end padding, both gaps and the bars
        assertEquals(100f - 15f - SLOT_END_PADDING_DP - 2f * SLOT_PAIR_GAP_DP - barsDp, reserved, 0.001f)
    }

    @Test
    fun lineSitsOnPillEdge() {
        assertEquals(0f, PulsePeak.LINE_BAND_DP, 0.0001f)
        // the line and its gap stay under the glyph's slot even in a 29 dp pill, so it never touches the glyph
        val room = (29f - SLOT_GLYPH_SIZE_DP) / 2f
        assertTrue(PulsePeak.LINE_EDGE_GAP_DP + PulsePeak.LINE_HEIGHT_DP <= room)
    }
}
