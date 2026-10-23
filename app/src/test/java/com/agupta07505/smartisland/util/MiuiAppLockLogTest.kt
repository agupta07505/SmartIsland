/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MiuiAppLockLogTest {
    private lateinit var log: MiuiAppLockLog

    @Before
    fun setUp() {
        log = MiuiAppLockLog()
    }

    private val appScan = "D/miui.face.FaceService( 3929): authenticate, packName: com.miui.securitycenter receiver:android.os.BinderProxy@b0a901b timeout:5000 mCurrentMonitor: null"
    private val keyguardScan = "D/miui.face.FaceService( 3929): authenticate, packName: com.android.systemui receiver:android.os.BinderProxy@c2cac28 timeout:5000 mCurrentMonitor: com.android.systemui"
    private val prompt = "D/KeyguardEditorHelper( 2514): onTopActivityMayChanged, topActivity=ComponentInfo{com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl}; mState=IDEL"
    private val otherTop = "D/KeyguardEditorHelper( 2514): onTopActivityMayChanged, topActivity=ComponentInfo{com.miui.gallery/com.miui.gallery.activity.HomePageActivity}; mState=IDEL"
    private val faceMatch = "D/ConfirmAccessControl(13282): onFaceAuthenticated"
    private val faceHelp = "I/ConfirmAccessControl(13282):  onFaceHelp tip:Couldn't recognize youhelpCode = 0"
    private val token = "D/Biometrics/AuthenticationClient( 1786): addAuthToken: 0"
    private val appFingerprintDone = "D/BiometricScheduler/Fingerprint21( 1786): [Finishing] {[1403] com.android.server.biometrics.sensors.fingerprint.hidl.FingerprintAuthenticationClient, proto=3, owner=com.miui.securitycenter, cookie=0, requestId=608, userId=0}, success: true"
    private val appFingerprintCancelled = appFingerprintDone.replace("success: true", "success: false")
    private val keyguardFingerprintDone = appFingerprintDone.replace("com.miui.securitycenter", "com.android.systemui")

    @Test
    fun aFaceRequestFromAnotherPackageIsAnAppScan() {
        assertEquals(MiuiAppLockEvent.ScanStart, log.classify(appScan, 0))
    }

    @Test
    fun aFaceRequestFromTheKeyguardIsNotAnAppScan() {
        assertNull(log.classify(keyguardScan, 0))
    }

    @Test
    fun theLockPromptActivityShowsThePrompt() {
        assertEquals(MiuiAppLockEvent.PromptShown, log.classify(prompt, 0))
        assertNull(log.classify(otherTop, 0))
    }

    @Test
    fun onlyTheAuthenticatedLineIsAFaceMatch() {
        assertEquals(MiuiAppLockEvent.FaceMatched, log.classify(faceMatch, 0))
        assertNull(log.classify(faceHelp, 0))
    }

    @Test
    fun aFingerprintMatchNeedsTheTokenLineJustBefore() {
        assertNull(log.classify(token, 100))
        assertEquals(MiuiAppLockEvent.FingerprintMatched, log.classify(appFingerprintDone, 117))
    }

    @Test
    fun aFinishedFingerprintClientWithoutATokenIsNotAMatch() {
        assertNull(log.classify(appFingerprintDone, 117))
    }

    @Test
    fun aCancelledFingerprintClientIsNotAMatch() {
        log.classify(token, 100)
        assertNull(log.classify(appFingerprintCancelled, 117))
    }

    @Test
    fun anOldTokenDoesNotCount() {
        log.classify(token, 100)
        assertNull(log.classify(appFingerprintDone, 2500))
    }

    @Test
    fun aKeyguardFingerprintNeverCountsAndUsesUpTheToken() {
        log.classify(token, 100)
        assertNull(log.classify(keyguardFingerprintDone, 117))
        assertNull(log.classify(appFingerprintDone, 300))
    }

    @Test
    fun oneTokenGivesOneMatch() {
        log.classify(token, 100)
        assertEquals(MiuiAppLockEvent.FingerprintMatched, log.classify(appFingerprintDone, 117))
        assertNull(log.classify(appFingerprintDone, 130))
    }

    @Test
    fun theDeviceLogCommandKeepsTheLinesTheDetectorReads() {
        val command = FaceUnlockWatcher.command(true)
        assertTrue(command.contains(MiuiAppLockLog.FILTER))
        val filter = Regex(MiuiAppLockLog.FILTER)
        listOf(prompt, faceMatch, token, appFingerprintDone, appScan).forEach { assertTrue(filter.containsMatchIn(it)) }
    }

    @Test
    fun theGeneralLockScreenAndAppLockLinesGiveNoMiuiEvent() {
        val lines = listOf(
            "D/AppLockManagerService( 2007): requireUnlock = true",
            "D/AppLockManagerService( 2007): unlockPackage: packageName = com.google.android.gm, userId = 0",
            "D/BiometricScheduler( 2007): [Started] {[1561] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.settings, cookie=1322052366, requestId=15, userId=0}",
            "V/KeyguardUpdateMonitor( 2514): startListeningForFace(): 0",
            "D/miui_face( 2514): face unlock success and keyguard dismiss",
            "D/KeyguardUpdateMonitor( 2514): Succeeded(biometricSourceType=FINGERPRINT, isStrongBiometric=true)"
        )
        lines.forEach { assertNull(log.classify(it, 0)) }
    }
}
