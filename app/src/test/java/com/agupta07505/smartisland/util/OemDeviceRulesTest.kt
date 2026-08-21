/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OemDeviceRulesTest {

    @Test
    fun testResolveEffectiveDevice() {
        assertEquals(OemDeviceType.SAMSUNG, OemDeviceRules.resolveEffectiveDevice("SAMSUNG"))
        assertEquals(OemDeviceType.XIAOMI_REDMI_POCO, OemDeviceRules.resolveEffectiveDevice("XIAOMI_REDMI_POCO"))
        assertEquals(OemDeviceType.VIVO_IQOO, OemDeviceRules.resolveEffectiveDevice("VIVO_IQOO"))
        assertEquals(OemDeviceType.REALME_OPPO_ONEPLUS, OemDeviceRules.resolveEffectiveDevice("REALME_OPPO_ONEPLUS"))
        assertEquals(OemDeviceType.GOOGLE_PIXEL, OemDeviceRules.resolveEffectiveDevice("GOOGLE_PIXEL"))
        assertEquals(OemDeviceType.HUAWEI_HONOR, OemDeviceRules.resolveEffectiveDevice("HUAWEI_HONOR"))
        assertEquals(OemDeviceType.MOTOROLA, OemDeviceRules.resolveEffectiveDevice("MOTOROLA"))
        assertEquals(OemDeviceType.TRANSSION, OemDeviceRules.resolveEffectiveDevice("TRANSSION"))
    }

    @Test
    fun testSamsungScreenRecordingIdentification() {
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.screenrecorder", "Recording screen", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.smartcapture", "Smart Capture", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.sec.android.app.voicenote", "Voice recording", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isScreenRecording("com.samsung.android.app.screenrecorder", "Screen recorder", OemDeviceType.SAMSUNG))
    }

    @Test
    fun testXiaomiScreenRecordingIdentification() {
        assertTrue(OemDeviceRules.isScreenRecording("com.miui.screenrecorder", "Screen Recorder", OemDeviceType.XIAOMI_REDMI_POCO))
        assertTrue(OemDeviceRules.isScreenRecording("com.miui.soundrecorder", "Sound Recorder", OemDeviceType.XIAOMI_REDMI_POCO))
    }

    @Test
    fun testVivoScreenRecordingIdentification() {
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.screenrecorder", "S-Capture", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.videocapture", "Video capture", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isScreenRecording("com.vivo.smartshot", "Smart shot", OemDeviceType.VIVO_IQOO))
    }

    @Test
    fun testRealmeOppoScreenRecordingIdentification() {
        assertTrue(OemDeviceRules.isScreenRecording("com.oplus.screenrecorder", "Screen recording", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isScreenRecording("com.coloros.screenrecorder", "Screen recorder", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isScreenRecording("com.realme.screenrecorder", "Screen recording", OemDeviceType.REALME_OPPO_ONEPLUS))
    }

    @Test
    fun testInCallPackagesForDifferentOems() {
        assertTrue(OemDeviceRules.isInCallPackage("com.samsung.android.incallui", OemDeviceType.SAMSUNG))
        assertTrue(OemDeviceRules.isInCallPackage("com.miui.incallui", OemDeviceType.XIAOMI_REDMI_POCO))
        assertTrue(OemDeviceRules.isInCallPackage("com.vivo.incallui", OemDeviceType.VIVO_IQOO))
        assertTrue(OemDeviceRules.isInCallPackage("com.oplus.incallui", OemDeviceType.REALME_OPPO_ONEPLUS))
        assertTrue(OemDeviceRules.isInCallPackage("com.google.android.dialer", OemDeviceType.GOOGLE_PIXEL))
        assertFalse(OemDeviceRules.isInCallPackage("com.whatsapp.camera", OemDeviceType.SAMSUNG))
    }

    @Test
    fun testHotspotDetection() {
        assertTrue(OemDeviceRules.isHotspot("com.samsung.android.app.mobilehotspot", "Hotspot active"))
        assertTrue(OemDeviceRules.isHotspot("com.android.settings", "Portable hotspot enabled"))
        assertTrue(OemDeviceRules.isHotspot("com.miui.securitycenter", "Tethering active"))
        assertFalse(OemDeviceRules.isHotspot("com.spotify.music", "Playing music"))
    }

    @Test
    fun testIsNonNavigationContent() {
        assertTrue(OemDeviceRules.isNonNavigationContent("com.sec.android.app.screenrecorder", "Recording screen 05:20 left"))
        assertTrue(OemDeviceRules.isNonNavigationContent("com.samsung.android.incallui", "Incoming call from Mom"))
        assertTrue(OemDeviceRules.isNonNavigationContent("com.android.settings", "Mobile hotspot active"))
        assertFalse(OemDeviceRules.isNonNavigationContent("com.google.android.apps.maps", "In 300 m turn left onto Main St"))
    }

    @Test
    fun testScreenRecordingCompletion() {
        val completedNotif = mockk<Notification>()
        completedNotif.flags = 0 // Not ongoing
        assertTrue(OemDeviceRules.isScreenRecordingComplete(completedNotif, "Recording saved • Tap to view"))

        val activeNotif = mockk<Notification>()
        activeNotif.flags = Notification.FLAG_ONGOING_EVENT
        assertFalse(OemDeviceRules.isScreenRecordingComplete(activeNotif, "Recording screen • 01:23"))
    }
}
