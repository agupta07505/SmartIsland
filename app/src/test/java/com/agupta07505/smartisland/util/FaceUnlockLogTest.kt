/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceUnlockLogTest {
    @Test
    fun successLinesAreFoundOnlyWhenTheyMentionAFace() {
        assertEquals(
            FaceUnlockLogEvent.Success,
            FaceUnlockLog.classify("I/FaceAuthenticationClient( 1234): onAuthenticated, authenticated=true, userId: 0")
        )
        assertEquals(FaceUnlockLogEvent.Success, FaceUnlockLog.classify("D/FaceService: onAuthenticationSucceeded"))
        // The same wording for a fingerprint is not a face unlock.
        assertNull(FaceUnlockLog.classify("I/FingerprintService: onAuthenticationSucceeded"))
        assertNull(FaceUnlockLog.classify("I/Keyguard: authenticated=true"))
    }

    @Test
    fun aRejectedFaceIsAFailureNotASuccess() {
        assertEquals(
            FaceUnlockLogEvent.Failure,
            FaceUnlockLog.classify("I/FaceAuthenticationClient( 1234): onAuthenticated, authenticated=false")
        )
        assertEquals(FaceUnlockLogEvent.Failure, FaceUnlockLog.classify("W/FaceService: onAuthenticationFailed"))
        assertEquals(FaceUnlockLogEvent.Failure, FaceUnlockLog.classify("E/FaceService: face authentication error: timed out"))
    }

    @Test
    fun startLinesAreFound() {
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify("D/FaceService: startAuthentication, sensorId=0"))
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify("D/FaceAuthenticationClient: startHalOperation"))
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify("I/KeyguardFace: started face authentication"))
        assertNull(FaceUnlockLog.classify("D/SomethingElse: startAuthentication"))
    }

    @Test
    fun unrelatedLinesAreIgnored() {
        assertNull(FaceUnlockLog.classify(""))
        assertNull(FaceUnlockLog.classify("D/Facebook: pushed a notification"))
        assertNull(FaceUnlockLog.classify("I/Interface: face of the clock changed"))
    }

    @Test
    fun captureKeepsOnlyLinesAfterTheLastMarkerThatLookRelevant() {
        val dump = listOf(
            "10-16 08:00:00.000 I/FaceService: before the marker",
            "10-16 08:00:01.000 I/${FaceUnlockLog.CAPTURE_TAG}: ${FaceUnlockLog.CAPTURE_MARKER_TEXT}",
            "10-16 08:00:02.000 D/Unrelated: nothing here",
            "10-16 08:00:03.000 D/FaceService: startAuthentication",
            "10-16 08:00:04.000 D/KeyguardViewMediator: keyguard unlock",
            ""
        ).joinToString("\n")
        val lines = FaceUnlockLog.extractCapture(dump)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("startAuthentication"))
        val counts = FaceUnlockLog.summarize(lines)
        assertEquals(1, counts[FaceUnlockLogEvent.Start])
    }

    @Test
    fun captureWithoutAMarkerTakesTheWholeDumpAndCapsIt() {
        val dump = (1..50).joinToString("\n") { "I/FaceService: line $it" }
        val lines = FaceUnlockLog.extractCapture(dump, maxLines = 10)
        assertEquals(10, lines.size)
        assertTrue(lines.last().endsWith("line 50"))
    }

    // Lines from a real capture (Xiaomi Redmi 9, Android 16, face.sense service)

    private val client = "{[825] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, " +
        "owner=com.android.systemui, cookie=0, requestId=129, userId=0}"

    @Test
    fun theWordFaceInsideOtherWordsIsNotAFace() {
        assertNull(FaceUnlockLog.classify("10-16 11:33:26.499   952   952 D SurfaceFlinger: setPhysicalDisplayPowerMode(OFF)"))
        assertNull(FaceUnlockLog.classify("10-16 11:33:24.704   458 14126 W libc    : Unable to set property \"ctl.interface_start\" to \"aidl/stats\": PROP_ERROR_HANDLE_CONTROL_MESSAGE (0x20)"))
        assertNull(FaceUnlockLog.classify("10-16 11:33:26.974 32339 32339 W TypefaceCache: MISS: CacheKey(res=fonts/google_sans_flex.ttf)"))
        assertNull(FaceUnlockLog.classify("10-16 11:33:35.142  2007  2007 I PowerManagerService: onFlip(): Face up."))
    }

    @Test
    fun faceHousekeepingClientsAreNotFailures() {
        // Every finger or PIN unlock prints these; they are not failures.
        for (what in listOf("Added", "Polled", "Started")) {
            assertNull(
                FaceUnlockLog.classify(
                    "10-16 11:33:32.425  2007  2007 D BiometricScheduler: [$what] {[822] com.android.server.biometrics.sensors.face.sense.FaceResetLockoutClient, proto=12, owner=android, cookie=0, requestId=-1, userId=0}"
                )
            )
        }
        assertNull(FaceUnlockLog.classify("10-16 11:33:38.319  2007  3623 D BiometricDeferredQueue: Resetting face lockout for sensor: 1008, user: 0"))
        assertNull(FaceUnlockLog.classify("10-16 11:33:32.424  2007  2007 D BiometricScheduler: [Started] {[821] com.android.server.biometrics.sensors.face.sense.FaceUpdateActiveUserClient, proto=1, owner=android}"))
    }

    private val matched = "10-16 16:59:02.794  2007  2007 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:1, Owner: com.android.systemui, isBP: false, listener: com.android.server.biometrics.sensors.ClientMonitorCallbackConverter@f2f7e81, requireConfirmation: false, user: 0, clientMonitor: {[1181] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.systemui, cookie=0, requestId=201, userId=0}"
    private val timedOut = "10-16 16:58:57.286  2007  2007 V Biometrics/AuthenticationClient: onAuthenticated(false), ID:0, Owner: com.android.systemui, isBP: false, listener: com.android.server.biometrics.sensors.ClientMonitorCallbackConverter@a6e0392, requireConfirmation: false, user: 0, clientMonitor: {[1179] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.systemui, cookie=0, requestId=200, userId=0}"
    private val finishedAfterTimeout = "10-16 16:58:57.293  2007  2007 D BiometricScheduler: [Finishing] {[1179] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.systemui, cookie=0, requestId=200, userId=0}, success: true"
    private val fingerprintMatched = "10-16 16:46:32.904  2007  3690 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:744165582, Owner: com.android.systemui, isBP: false, listener: com.android.server.biometrics.sensors.ClientMonitorCallbackConverter@9131d9e, requireConfirmation: false, user: 0, clientMonitor: {[1160] com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintAuthenticationClient, proto=3, owner=com.android.systemui, cookie=0, requestId=279, userId=0}"

    @Test
    fun aFaceScanThatTimesOutIsNotASuccess() {
        assertEquals(FaceUnlockLogEvent.Failure, FaceUnlockLog.classify(timedOut))
        assertNull(FaceUnlockLog.classify(finishedAfterTimeout))
        assertNull(FaceUnlockLog.classifyUnlock(finishedAfterTimeout))
        assertFalse(FaceUnlockLog.isStrongSuccess(timedOut))
        assertFalse(FaceUnlockLog.isStrongSuccess(finishedAfterTimeout))
        assertEquals(emptyMap<UnlockType, Int>(), FaceUnlockLog.summarizeUnlocks(listOf(timedOut, finishedAfterTimeout)))
    }

    @Test
    fun aFaceMatchIsTheAuthenticatedTrueLineWithAFaceId() {
        assertEquals(FaceUnlockLogEvent.Success, FaceUnlockLog.classify(matched))
        assertTrue(FaceUnlockLog.isStrongSuccess(matched))
        assertEquals(UnlockType.Face, FaceUnlockLog.classifyUnlock(matched))
        // A finger matching prints the same line for its own client: it is not a face.
        assertNull(FaceUnlockLog.classify(fingerprintMatched))
    }

    @Test
    fun aTimeoutThenARealMatchCountsOneFaceUnlock() {
        val lines = listOf(timedOut, finishedAfterTimeout, matched)
        assertEquals(mapOf(UnlockType.Face to 1), FaceUnlockLog.summarizeUnlocks(lines))
    }

    @Test
    fun swipingUpToThePinScreenIsRecognisedFromTheTransitionLine() {
        val started = "10-16 17:03:04.774 32339 32339 I KeyguardTransitionRepository: STARTED transition: TransitionStep(from=LOCKSCREEN, to=PRIMARY_BOUNCER, value=0.0, transitionState=STARTED, ownerName=FromLockscreenTransitionInteractor(#listenForLockscreenToPrimaryBouncerDragging))"
        val info = "10-16 17:03:04.774 32339 32339 D KeyguardTransitionRepository: (Internal) Setting current transition info: TransitionInfo(ownerName=FromLockscreenTransitionInteractor(#listenForLockscreenToPrimaryBouncerDragging), from=LOCKSCREEN, to=PRIMARY_BOUNCER, animator=null)"
        val finished = "10-16 17:03:05.022 32339 32339 I KeyguardTransitionRepository: FINISHED transition: TransitionStep(from=LOCKSCREEN, to=PRIMARY_BOUNCER, value=1.0, transitionState=FINISHED, ownerName=FromLockscreenTransitionInteractor(#listenForLockscreenToPrimaryBouncerDragging))"
        val closing = "10-16 17:03:07.022 32339 32339 I KeyguardTransitionRepository: STARTED transition: TransitionStep(from=PRIMARY_BOUNCER, to=GONE, value=0.0, transitionState=STARTED, ownerName=FromPrimaryBouncerTransitionInteractor)"
        assertTrue(FaceUnlockLog.isBouncerShown(started))
        assertFalse(FaceUnlockLog.isBouncerShown(info))
        assertFalse(FaceUnlockLog.isBouncerShown(finished))
        assertFalse(FaceUnlockLog.isBouncerShown(closing))
        assertEquals("pin-screen:Shown", FaceUnlockLog.annotation(started))
        assertNull(FaceUnlockLog.classify(started))
        assertNull(FaceUnlockLog.classifyUnlock(started))
        // The live log filter has to let the line through.
        assertTrue(FaceUnlockWatcher.command(true).contains("PRIMARY_BOUNCER"))
    }

    @Test
    fun theSchedulerStartsAndEndsAFaceSession() {
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify("10-16 11:33:35.678  2007  2007 D BiometricScheduler: [Started] $client"))
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify("10-16 11:33:35.678  2007  2007 V BiometricSchedulerOperation: started: $client, State: 2"))
        assertEquals(FaceUnlockLogEvent.End, FaceUnlockLog.classify("10-16 11:33:37.713  2007  2007 D BiometricScheduler: [Finishing] $client, success: false"))
        assertNull(FaceUnlockLog.classify("10-16 11:33:37.713  2007  2007 D BiometricScheduler: [Finishing] $client, success: true"))
    }

    @Test
    fun aRealLockoutErrorIsAFailure() {
        assertEquals(FaceUnlockLogEvent.Failure, FaceUnlockLog.classify("D/FaceService: onLockoutPermanent"))
        assertEquals(FaceUnlockLogEvent.Failure, FaceUnlockLog.classify("D/BiometricStatusRepositoryImpl: authenticationState updated: Failed(biometricSourceType=FACE, userId=0)"))
    }

    @Test
    fun unlockTypesAreRead() {
        assertEquals(
            UnlockType.Fingerprint,
            FaceUnlockLog.classifyUnlock("10-16 11:33:32.428 32339 32352 D BiometricStatusRepositoryImpl: authenticationState updated: Succeeded(biometricSourceType=FINGERPRINT, isStrongBiometric=true, requestReason=DeviceEntryAuthentication, userId=0)")
        )
        assertEquals(
            UnlockType.Face,
            FaceUnlockLog.classifyUnlock("D/BiometricStatusRepositoryImpl: authenticationState updated: Succeeded(biometricSourceType=FACE, isStrongBiometric=false, userId=0)")
        )
        assertEquals(UnlockType.Face, FaceUnlockLog.classifyUnlock(matched))
        assertEquals(
            UnlockType.Credential,
            FaceUnlockLog.classifyUnlock("10-16 11:33:38.317   517  4612 I keystore2: system/security/keystore2/src/authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=true)")
        )
    }

    @Test
    fun notAnUnlockLinesGiveNoType() {
        assertNull(FaceUnlockLog.classifyUnlock("10-16 11:33:38.987   517  4612 I keystore2: authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=false)"))
        assertNull(FaceUnlockLog.classifyUnlock("10-16 11:33:35.107   517  4612 I keystore2: authorization.rs:153 - on_device_locked(user_id=0, unlocking_sids=[1], weak_unlock_enabled=true)"))
        assertNull(FaceUnlockLog.classifyUnlock("10-16 11:33:37.713  2007  2007 D BiometricScheduler: [Finishing] $client, success: false"))
        assertNull(FaceUnlockLog.classifyUnlock("D/BiometricStatusRepositoryImpl: authenticationState updated: Failed(biometricSourceType=FINGERPRINT, userId=0)"))
    }

    @Test
    fun oneUnlockPrintingTwoLinesCountsOnce() {
        val lines = listOf(
            "10-16 11:33:35.100 D BiometricStatusRepositoryImpl: authenticationState updated: Succeeded(biometricSourceType=FACE, isStrongBiometric=false, userId=0)",
            "10-16 11:33:35.101 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:1, clientMonitor: $client",
            "10-16 11:33:38.317 I keystore2: authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=true)",
            "10-16 11:33:50.000 D BiometricStatusRepositoryImpl: authenticationState updated: Succeeded(biometricSourceType=FINGERPRINT, userId=0)"
        )
        val counts = FaceUnlockLog.summarizeUnlocks(lines)
        assertEquals(1, counts[UnlockType.Face])
        assertEquals(1, counts[UnlockType.Credential])
        assertEquals(1, counts[UnlockType.Fingerprint])
    }

    @Test
    fun captureDropsPerFrameNoiseAndKeepsTheSessionLines() {
        assertTrue(!FaceUnlockLog.isCaptureCandidate("10-16 11:33:36.067  5702 14038 I mgulk   : face not found"))
        assertTrue(!FaceUnlockLog.isCaptureCandidate("10-16 11:33:36.009  5702 14039 D FaceAuthenticationController: Camera frame arrival"))
        assertTrue(!FaceUnlockLog.isCaptureCandidate("10-16 11:33:26.499   952   952 D SurfaceFlinger: Setting power mode 0"))
        assertTrue(FaceUnlockLog.isCaptureCandidate("10-16 11:33:35.678  2007  2007 D BiometricScheduler: [Started] $client"))
        assertTrue(FaceUnlockLog.isCaptureCandidate("10-16 11:33:38.317   517  4612 I keystore2: authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=true)"))
    }

    @Test
    fun captureLinesCarryWhatTheDetectorFound() {
        assertEquals("face:Start", FaceUnlockLog.annotation("10-16 11:33:35.678  2007  2007 D BiometricScheduler: [Started] $client"))
        assertEquals(
            "unlock:Credential",
            FaceUnlockLog.annotation("I keystore2: authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=true)")
        )
        assertNull(FaceUnlockLog.annotation("D/Unrelated: nothing"))
    }

    @Test
    fun aRealFaceUnlockIsRecognisedEndToEnd() {
        // From a capture where the face unlocked the phone (Redmi 9, face.sense).
        val face = "{[880] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.systemui, cookie=0, requestId=139, userId=0}"
        val lines = listOf(
            "10-16 13:31:18.541  2007  2007 D BiometricScheduler: [Started] $face",
            "10-16 13:31:19.771  2007  2007 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:1, Owner: com.android.systemui, clientMonitor: $face",
            "10-16 13:31:19.772  2007  2007 D BiometricScheduler: [Finishing] $face, success: true",
            "10-16 13:31:19.783   517  4612 I keystore2: authorization.rs:126 - on_device_unlocked(user_id=0, password.is_some()=false)"
        )
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify(lines[0]))
        assertEquals(FaceUnlockLogEvent.Success, FaceUnlockLog.classify(lines[1]))
        assertNull(FaceUnlockLog.classify(lines[2]))
        assertNull(FaceUnlockLog.classifyUnlock(lines[3]))
        assertEquals(mapOf(UnlockType.Face to 1), FaceUnlockLog.summarizeUnlocks(lines))
    }

    @Test
    fun onlyTheFaceClientAndSystemUiSuccessLinesAreStrong() {
        assertTrue(FaceUnlockLog.isStrongSuccess("V Biometrics/AuthenticationClient: onAuthenticated(true), ID:1, clientMonitor: FaceAuthenticationClient"))
        assertFalse(FaceUnlockLog.isStrongSuccess("I BiometricScheduler: [Finishing] FaceAuthenticationClient, success: true"))
        assertTrue(FaceUnlockLog.isStrongSuccess("D KeyguardUpdateMonitor: Succeeded(biometricSourceType=FACE, userId=0)"))
        assertFalse(FaceUnlockLog.isStrongSuccess("D FaceService: onAuthenticated authenticated=true"))
        assertFalse(FaceUnlockLog.isStrongSuccess("I Face: unlock succeeded"))
        assertFalse(FaceUnlockLog.isStrongSuccess("I BiometricScheduler: [Finishing] FaceAuthenticationClient, success: false"))
    }

    private val appStart = "10-16 23:42:23.571  2007  2007 D BiometricScheduler: [Started] {[1561] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.settings, cookie=1322052366, requestId=15, userId=0}"
    private val lockStart = "10-16 17:03:04.774  2007  2007 D BiometricScheduler: [Started] {[1500] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.systemui, cookie=1, requestId=3, userId=0}"
    private val appUnlocked = "10-16 23:42:28.310  2007  6040 D AppLockManagerService: unlockPackage: packageName = com.google.android.gm, userId = 0"
    private val appRequired = "10-16 23:42:23.522  2007  5173 D AppLockManagerService: requireUnlock = true"
    private val appNotRequired = "10-16 23:42:22.279  2007  5748 D AppLockManagerService: requireUnlock = false"

    @Test
    fun aFaceClientStartedForAnAppPromptIsAnAppScan() {
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify(appStart))
        assertEquals("com.android.settings", FaceUnlockLog.clientOwner(appStart))
        assertTrue(FaceUnlockLog.isAppScanStart(appStart))
    }

    @Test
    fun aFaceClientStartedForTheKeyguardIsNotAnAppScan() {
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify(lockStart))
        assertFalse(FaceUnlockLog.isAppScanStart(lockStart))
    }

    @Test
    fun aStartLineWithoutAnOwnerIsNotAnAppScan() {
        assertFalse(FaceUnlockLog.isAppScanStart("D FaceAuthenticationClient: startAuthentication"))
    }

    @Test
    fun theAppLockUnlockedLineNamesThePackage() {
        assertEquals("com.google.android.gm", FaceUnlockLog.appUnlockedPackage(appUnlocked))
        assertNull(FaceUnlockLog.appUnlockedPackage(appRequired))
    }

    @Test
    fun theAppLockLinesAlsoMatchTheBriefLogFormat() {
        assertTrue(FaceUnlockLog.isAppLockRequired("D/AppLockManagerService( 2007): requireUnlock = true"))
        assertEquals("com.google.android.gm", FaceUnlockLog.appUnlockedPackage("D/AppLockManagerService( 2007): unlockPackage: packageName = com.google.android.gm, userId = 0"))
        assertTrue(FaceUnlockWatcher.command(true).contains("requireUnlock = true"))
    }

    @Test
    fun theRealDismissLinesMarkAClosedPrompt() {
        assertTrue(FaceUnlockLog.isAppPromptDismissed("10-17 08:20:49.934  2007  3623 D BiometricService: onSystemEvent: 1"))
        assertTrue(FaceUnlockLog.isAppPromptDismissed("10-17 08:20:50.360  2007  3623 E BiometricService: onDismissed: 3, AuthSession is not current"))
        assertTrue(FaceUnlockLog.isAppPromptDismissed("E/BiometricService( 2007): onDismissed: 3, AuthSession is not current"))
        assertFalse(FaceUnlockLog.isAppPromptDismissed("D BiometricService: onSystemEvent: 10"))
        assertFalse(FaceUnlockLog.isAppPromptDismissed("E BiometricService: onDismissed: 1"))
        val command = FaceUnlockWatcher.command(true)
        assertTrue(command.contains("onSystemEvent: 1") && command.contains("onDismissed: 3"))
    }

    @Test
    fun onlyRequireUnlockTrueMarksALockedApp() {
        assertTrue(FaceUnlockLog.isAppLockRequired(appRequired))
        assertFalse(FaceUnlockLog.isAppLockRequired(appNotRequired))
    }

    @Test
    fun theCaptureAnnotatesAppPromptLines() {
        assertTrue(FaceUnlockLog.annotation(appStart)!!.contains("app-scan:Start(com.android.settings)"))
        assertTrue(FaceUnlockLog.annotation(appUnlocked)!!.contains("app-lock:Unlocked(com.google.android.gm)"))
        assertTrue(FaceUnlockLog.annotation(appRequired)!!.contains("app-lock:Required"))
    }

    @Test
    fun theLogFollowKeepsTheAppUnlockedLine() {
        assertTrue(FaceUnlockWatcher.command(true).contains("unlockPackage"))
    }

    @Test
    fun theRealAppPromptFaceMatchLineIsAStrongSuccess() {
        val line = "10-17 00:04:05.855  2007  2007 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:1, Owner: com.android.settings, isBP: true, listener: com.android.server.biometrics.sensors.ClientMonitorCallbackConverter@67743bd, requireConfirmation: false, user: 0, clientMonitor: {[1582] com.android.server.biometrics.sensors.face.sense.FaceAuthenticationClient, proto=3, owner=com.android.settings, cookie=1880327456, requestId=17, userId=0}   <<< face:Success unlock:Face"
        assertEquals(FaceUnlockLogEvent.Success, FaceUnlockLog.classify(line))
        assertTrue(FaceUnlockLog.isStrongSuccess(line))
        assertEquals(UnlockType.Face, FaceUnlockLog.classifyUnlock(line))
    }

    private val miuiStart = "10-16 23:54:38.960  3929  4071 D miui.face.FaceService: authenticate, packName: com.android.systemui receiver:android.os.BinderProxy@c2cac28 timeout:5000 mCurrentMonitor: com.android.systemui"
    private val miuiSuccess = "10-16 23:54:39.680  2514  2514 D miui_face: face unlock success and keyguard dismiss"
    private val miuiNotAScan = "10-16 23:54:37.893  2514  2514 E miui_face: start face unlock fail, mKeyguardShowing =false;isDeviceInteractive =false;isSwitchingUser =false;isKeyguardGoingAway =false"

    @Test
    fun theMiuiFaceServiceScanRequestForTheKeyguardIsAStartButNotAnAppScan() {
        assertEquals(FaceUnlockLogEvent.Start, FaceUnlockLog.classify(miuiStart))
        assertFalse(FaceUnlockLog.isAppScanStart(miuiStart))
    }

    @Test
    fun aMiuiFaceServiceScanRequestForAnotherPackageIsAnAppScan() {
        val line = miuiStart.replace("packName: com.android.systemui", "packName: com.miui.securitycenter")
        assertTrue(FaceUnlockLog.isAppScanStart(line))
    }

    @Test
    fun theMiuiSuccessLineIsSeenAndMiuiFailLinesAreNotScans() {
        assertEquals(FaceUnlockLogEvent.Success, FaceUnlockLog.classify(miuiSuccess))
        assertNull(FaceUnlockLog.classify(miuiNotAScan))
    }

    @Test
    fun theMiuiSuccessLineIsAStrongSuccessAndTheStartLineIsAStart() {
        assertTrue(FaceUnlockLog.isStrongSuccess(miuiSuccess))
        assertEquals(UnlockType.Face, FaceUnlockLog.classifyUnlock(miuiSuccess))
        assertEquals(
            FaceUnlockLogEvent.Start,
            FaceUnlockLog.classify("10-17 09:30:03.045  2514  2514 V KeyguardUpdateMonitor: startListeningForFace(): 0")
        )
    }

    @Test
    fun aMiuiFaceErrorEndsTheScanExceptTheCancelThatComesWithAnUnlock() {
        val cancelled = "10-17 09:30:05.731  3929  4071 D miui.face.FaceService: onError  curMonitor:com.miui.face.util.ClientMonitor@ccc5e00, vendorCode:0, errorId:5"
        val timedOut = cancelled.replace("errorId:5", "errorId:3")
        assertNull(FaceUnlockLog.classify(cancelled))
        assertEquals(FaceUnlockLogEvent.End, FaceUnlockLog.classify(timedOut))
    }

    @Test
    fun theMiuiFaceHelpLinesOfAScanInTheDarkAreNotEvents() {
        assertNull(FaceUnlockLog.classify("10-17 09:30:03.344  3929  4071 D miui.face.FaceService: onAcquired  curMonitor:com.miui.face.util.ClientMonitor@ccc5e00 msgId:30"))
        assertNull(FaceUnlockLog.classify("10-17 09:30:03.344  3929  4055 D miui.face.FaceService: onFaceHelp help:30"))
    }

    private val appFingerprintMatched = "10-17 15:53:32.388  2007  3690 V Biometrics/AuthenticationClient: onAuthenticated(true), ID:744165582, Owner: com.android.settings, isBP: true, listener: com.android.server.biometrics.sensors.ClientMonitorCallbackConverter@a1bdb1f, requireConfirmation: false, user: 0, clientMonitor: {[2243] com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintAuthenticationClient, proto=3, owner=com.android.settings, cookie=1467723854, requestId=65, userId=0}"

    @Test
    fun aFingerMatchIsFoundFromTheAuthenticatedTrueLineOfTheFingerprintClient() {
        assertTrue(FaceUnlockLog.isFingerprintMatch(fingerprintMatched))
        assertTrue(FaceUnlockLog.isFingerprintMatch(appFingerprintMatched))
        assertEquals(UnlockType.Fingerprint, FaceUnlockLog.classifyUnlock(fingerprintMatched))
        assertEquals(UnlockType.Fingerprint, FaceUnlockLog.classifyUnlock(appFingerprintMatched))
        assertFalse(FaceUnlockLog.isFingerprintMatch(matched))
        assertFalse(FaceUnlockLog.isFingerprintMatch(timedOut))
        assertNull(FaceUnlockLog.classify(appFingerprintMatched))
        assertTrue(FaceUnlockLog.annotation(appFingerprintMatched)!!.contains("unlock:Fingerprint"))
    }

    @Test
    fun aFingerMatchAndTheSystemUiLineThatFollowsItCountOnce() {
        val lines = listOf(
            fingerprintMatched,
            "10-16 16:46:32.910 D BiometricStatusRepositoryImpl: authenticationState updated: Succeeded(biometricSourceType=FINGERPRINT, isStrongBiometric=true, userId=0)"
        )
        assertEquals(mapOf(UnlockType.Fingerprint to 1), FaceUnlockLog.summarizeUnlocks(lines))
    }

    @Test
    fun theLogFollowKeepsTheAuthenticatedTrueLine() {
        assertTrue(FaceUnlockWatcher.command(true).contains("onAuthenticated.true"))
    }

    private val miuiKeyguardFingerprint = "10-17 13:56:20.741  2514  2514 W miui_keyguard: unlock keyguard by fp"

    @Test
    fun aHyperOsKeyguardFingerprintUnlockIsAFingerMatch() {
        assertTrue(FaceUnlockLog.isFingerprintMatch(miuiKeyguardFingerprint))
        assertTrue(FaceUnlockLog.isFingerprintMatch("W/miui_keyguard( 2514): unlock keyguard by fp"))
        assertEquals(UnlockType.Fingerprint, FaceUnlockLog.classifyUnlock(miuiKeyguardFingerprint))
        assertNull(FaceUnlockLog.classify(miuiKeyguardFingerprint))
        assertFalse(FaceUnlockLog.isFingerprintMatch("10-17 14:16:46.375  2514  2514 W miui_keyguard: unlock keyguard by face"))
        assertTrue(FaceUnlockWatcher.command(true).contains("unlock keyguard by fp"))
    }
}
