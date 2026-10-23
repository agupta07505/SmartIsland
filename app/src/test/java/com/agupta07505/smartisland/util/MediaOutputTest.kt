/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaOutputTest {
    private val speaker = OutputCandidate(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Pixel 8")
    private val buds = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "  Galaxy Buds  ")

    private fun label(o: MediaOutput) = MediaOutputRules.label(o, "This phone", "Cast", "Wired", "Wired headphone")

    @Test
    fun nothingConnectedOrUnknownDevicesShowThePhone() {
        assertEquals(MediaOutput.Phone, MediaOutputRules.resolve(false, null, emptyList()))
        assertEquals(MediaOutput.Phone, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_TELEPHONY, "x"))))
    }

    @Test
    fun phoneSpeakerNeverShowsTheModelName() {
        val out = MediaOutputRules.resolve(false, null, listOf(speaker))
        assertEquals(MediaOutputKind.Phone, out.kind)
        assertEquals("This phone", label(out))
    }

    @Test
    fun pairedDeviceBeatsTheSpeakerAndKeepsItsTrimmedName() {
        val out = MediaOutputRules.resolve(false, null, listOf(speaker, buds))
        assertEquals(MediaOutputKind.Bluetooth, out.kind)
        assertEquals("Galaxy Buds", out.name)
    }

    @Test
    fun priorityIsBluetoothThenWiredThenUsbThenHdmi() {
        val wired = OutputCandidate(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, null)
        val usb = OutputCandidate(AudioDeviceInfo.TYPE_USB_HEADSET, "DAC")
        val hdmi = OutputCandidate(AudioDeviceInfo.TYPE_HDMI, "TV")
        assertEquals(MediaOutputKind.Headphones, MediaOutputRules.resolve(false, null, listOf(hdmi, usb, wired)).kind)
        assertEquals(MediaOutputKind.Usb, MediaOutputRules.resolve(false, null, listOf(hdmi, usb)).kind)
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.resolve(false, null, listOf(wired, buds)).kind)
    }

    @Test
    fun routedDeviceWinsATie() {
        val a = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "First")
        val b = OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "Second")
        assertEquals("First", MediaOutputRules.resolve(false, null, listOf(a, b)).name)
    }

    @Test
    fun bleDevicesCountAsBluetooth() {
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.kindOf(26))
        assertEquals(MediaOutputKind.Bluetooth, MediaOutputRules.kindOf(27))
    }

    @Test
    fun remoteSessionIsCastWhateverIsPlugged() {
        val out = MediaOutputRules.resolve(true, null, listOf(buds))
        assertEquals(MediaOutputKind.Cast, out.kind)
        assertEquals("Cast", label(out))
        assertEquals("Living room", MediaOutputRules.resolve(true, "Living room", emptyList()).name)
    }

    @Test
    fun labelFallsBackPerKindAndLongNamesAreCut() {
        assertEquals("Bluetooth", label(MediaOutput(MediaOutputKind.Bluetooth, null)))
        assertEquals("Wired headphone", label(MediaOutput(MediaOutputKind.Headphones, null)))
        assertEquals("Wired", label(MediaOutput(MediaOutputKind.Usb, null)))
        val cut = label(MediaOutput(MediaOutputKind.Bluetooth, "A very long speaker name indeed"))
        assertEquals(MediaOutputRules.MAX_LABEL_CHARS, cut.length)
        assertEquals('…', cut.last())
    }

    @Test
    fun blankOrLiteralNullNamesAreDropped() {
        assertEquals(null, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "  "))).name)
        assertEquals(null, MediaOutputRules.resolve(false, null, listOf(OutputCandidate(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "null"))).name)
    }

    @Test
    fun castTargetNameComesFromTheRouteWithTheSessionsControlId() {
        val routes = listOf("a" to "Living room TV", "b" to "Kitchen speaker")
        assertEquals("Kitchen speaker", MediaOutputRules.remoteRouteName("b", routes))
    }

    @Test
    fun castTargetNameIsNullWithoutAMatchOrAnId() {
        val routes = listOf("a" to "Living room TV")
        assertEquals(null, MediaOutputRules.remoteRouteName("zzz", routes))
        assertEquals(null, MediaOutputRules.remoteRouteName("", routes))
        assertEquals(null, MediaOutputRules.remoteRouteName(null, routes))
        assertEquals(null, MediaOutputRules.remoteRouteName("a", listOf("a" to "  ")))
    }

    @Test
    fun theHeadphoneJackShowsThePlainWordNotTheDriversId() {
        val jack = OutputCandidate(AudioDeviceInfo.TYPE_WIRED_HEADSET, "h2w")
        val out = MediaOutputRules.resolve(false, null, listOf(jack))
        assertEquals(MediaOutputKind.Headphones, out.kind)
        assertEquals("Wired headphone", label(out))
    }
}
