/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.view.WindowManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen-capture FLAG_SECURE, touch drop, face-unlock clearance and the secure-app guess (Rule_Book 8.2, 10.1). */
@OptIn(ExperimentalCoroutinesApi::class)
class OverlayGuardsTest {

    @Test
    fun extraFlagsAddSecureAndNotTouchableIndependently() {
        assertEquals(0, OverlayWindowGuards.extraFlags(secure = false, untouchable = false))
        assertEquals(WindowManager.LayoutParams.FLAG_SECURE, OverlayWindowGuards.extraFlags(secure = true, untouchable = false))
        assertEquals(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, OverlayWindowGuards.extraFlags(secure = false, untouchable = true))
        assertEquals(
            WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            OverlayWindowGuards.extraFlags(secure = true, untouchable = true)
        )
    }

    @Test
    fun anyCaptureSignalCountsAsCapture() {
        assertFalse(ScreenCaptureMonitor.isCaptureActive(recordingApi = false, presentationDisplay = false, recorderAlert = false))
        assertTrue(ScreenCaptureMonitor.isCaptureActive(recordingApi = true, presentationDisplay = false, recorderAlert = false))
        assertTrue(ScreenCaptureMonitor.isCaptureActive(recordingApi = false, presentationDisplay = true, recorderAlert = false))
        assertTrue(ScreenCaptureMonitor.isCaptureActive(recordingApi = false, presentationDisplay = false, recorderAlert = true))
    }

    @Test
    fun secureAppGuessUsesTheBuiltInListAndExtras() {
        assertTrue(ForegroundSecureDetector.isSecurePackage("com.x8bit.bitwarden"))
        assertTrue(ForegroundSecureDetector.isSecurePackage("com.netflix.mediaclient"))
        assertFalse(ForegroundSecureDetector.isSecurePackage("com.android.chrome"))
        assertFalse(ForegroundSecureDetector.isSecurePackage(null))
        assertTrue(ForegroundSecureDetector.isSecurePackage("com.my.bank", setOf("com.my.bank")))

        val detector = ForegroundSecureDetector()
        detector.onForegroundPackage("com.netflix.mediaclient")
        assertTrue(detector.foregroundSecure.value)
        detector.onForegroundPackage("com.android.chrome")
        assertFalse(detector.foregroundSecure.value)
    }

    @Test
    fun biometricClearanceRunsForItsWindowAfterALockedWake() {
        val scope = TestScope()
        val changes = ArrayList<Boolean>()
        val clearance = BiometricClearance(scope, windowMs = 5_000L) { changes += it }

        clearance.onScreenOn(locked = true)
        assertTrue(clearance.active)
        scope.advanceTimeBy(4_999L)
        scope.runCurrent()
        assertTrue(clearance.active)
        scope.advanceTimeBy(2L)
        scope.runCurrent()
        assertFalse(clearance.active)
        assertEquals(listOf(true, false), changes)
    }

    @Test
    fun biometricClearanceEndsOnUnlockAndNeverStartsUnlocked() {
        val scope = TestScope()
        val clearance = BiometricClearance(scope, windowMs = 5_000L) { }

        clearance.onScreenOn(locked = false)
        assertFalse(clearance.active)

        clearance.onScreenOn(locked = true)
        clearance.onUnlocked()
        assertFalse(clearance.active)
        // The cancelled timer does not flip it back later.
        scope.advanceTimeBy(10_000L)
        scope.runCurrent()
        assertFalse(clearance.active)

        clearance.onScreenOn(locked = true)
        clearance.onScreenOff()
        assertFalse(clearance.active)
    }

    @Test
    fun castingAndMirroringDisplaysCountAsCaptureButBuiltInScreensDoNot() {
        // The phone's own screen, and a foldable's cover screen, never count.
        assertFalse(CaptureDisplays.isCaptureDisplay(isDefault = true, flags = CaptureDisplays.FLAG_PRESENTATION, type = CaptureDisplays.TYPE_INTERNAL))
        assertFalse(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = 0, type = CaptureDisplays.TYPE_INTERNAL))
        // Cast / Wi-Fi display / HDMI as presentation displays, even when the hidden type cannot be read.
        assertTrue(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = CaptureDisplays.FLAG_PRESENTATION, type = null))
        assertTrue(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = 0, type = CaptureDisplays.TYPE_WIFI))
        // A public MediaProjection mirror (virtual display) is a capture; a private one is someone's own canvas.
        assertTrue(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = 0, type = CaptureDisplays.TYPE_VIRTUAL))
        assertFalse(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = CaptureDisplays.FLAG_PRIVATE, type = CaptureDisplays.TYPE_VIRTUAL))
        // Unknown type and no presentation flag: not enough to black the island out.
        assertFalse(CaptureDisplays.isCaptureDisplay(isDefault = false, flags = 0, type = null))
    }

    @Test
    fun foregroundServiceTypeMatchesTheManifestOnEveryApiLevel() {
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            SmartIslandOverlayService.overlayForegroundServiceType(34)
        )
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            SmartIslandOverlayService.overlayForegroundServiceType(36)
        )
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
            SmartIslandOverlayService.overlayForegroundServiceType(29)
        )
        assertEquals(
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
            SmartIslandOverlayService.overlayForegroundServiceType(33)
        )
        assertEquals(0, SmartIslandOverlayService.overlayForegroundServiceType(26))
    }
}
