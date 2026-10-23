/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FaceUnlockCoordinatorTest {
    private class Fake : FaceUnlockCoordinator.Output {
        var scanning = false
        var starts = 0
        var lastWaitMs = -1
        var appStarts = 0
        var earlyAppStarts = 0
        var anchors = 0
        var accepts = 0
        var cancels = 0
        var rejects = 0
        var fingerprints = 0
        var directFingerprints = 0
        var appFingerprints = 0
        val queued = ArrayList<Pair<Long, () -> Unit>>()

        override fun startScan() { scanning = true; starts++ }
        override fun startScan(waitMs: Int) { lastWaitMs = waitMs; startScan() }
        override fun startAppScan() { scanning = true; appStarts++ }
        override fun startAppScanEarly() { startAppScan(); earlyAppStarts++ }
        override fun anchor() { anchors++ }
        override fun accept() { accepts++ }
        override fun fingerprint() { fingerprints++ }
        override fun startFingerprintScan() { scanning = true; directFingerprints++ }
        override fun startAppFingerprintScan() { scanning = true; appFingerprints++ }
        override fun cancel() { scanning = false; cancels++ }
        override fun reject() { rejects++ }
        override fun isScanning() = scanning
        override fun later(delayMs: Long, block: () -> Unit) { queued.add(delayMs to block) }

        fun runLater() { val now = queued.toList(); queued.clear(); now.forEach { it.second() } }
        fun scanEnds() { scanning = false }
    }

    private lateinit var out: Fake
    private lateinit var c: FaceUnlockCoordinator

    @Before
    fun setUp() {
        out = Fake()
        c = FaceUnlockCoordinator(out)
    }

    private fun faceOnly(logAvailable: Boolean = true) =
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = logAvailable)

    @Test
    fun aStartLineWhileLockedStartsTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 100)
        assertEquals(2, out.starts)
    }

    @Test
    fun withoutAnyStartLineTheScreenOnStartsTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertEquals(1, out.starts)
    }

    @Test
    fun onceAStartLineWasSeenTheScreenOnAloneDoesNotStartIt() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 10_000)
        assertEquals(0, out.starts)
    }

    @Test
    fun aStartLineJustBeforeTheScreenOnStartsItWhenTheScreenComesOn() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        assertEquals(0, out.starts)
        c.onScreenOn(locked = true, now = 1500)
        assertEquals(1, out.starts)
    }

    @Test
    fun anOldStartLineDoesNotStartItLater() {
        faceOnly()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 20_000)
        // The log is trusted from now on, so nothing starts by the screen alone.
        assertEquals(0, out.starts)
    }

    @Test
    fun theScanPlaysAgainWhenTheFaceComesBack() {
        faceOnly()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 5)
        assertEquals(1, out.starts)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 9000)
        assertEquals(2, out.starts)
    }

    @Test
    fun aStartLineWithTheScreenAlreadyWakingStartsTheScanAtOnce() {
        faceOnly()
        c.onScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        assertEquals(1, out.starts)
    }

    @Test
    fun theLateScreenOnBroadcastDoesNotStopOrRestartTheScanTheStartLineBegan() {
        faceOnly()
        c.onScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onScreenOn(locked = true, now = 1750)
        assertEquals(1, out.starts)
        assertEquals(0, out.cancels)
        assertTrue(out.scanning)
    }

    @Test
    fun aFaceThatMatchesBeforeTheScreenOnBroadcastStillPlaysTheTick() {
        faceOnly()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Success, 1573, strong = true)
        assertEquals(1, out.starts)
        assertEquals(1, out.accepts)
        c.onScreenOn(locked = false, now = 1750)
        assertEquals(0, out.cancels)
        assertEquals(1, out.starts)
    }

    @Test
    fun aLooseSuccessBeforeTheScreenOnBroadcastDoesNotStartAScan() {
        faceOnly()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Success, 1573, strong = false)
        assertEquals(0, out.starts)
        assertEquals(0, out.accepts)
    }

    @Test
    fun withTheLogTheScanWaitsForTheSystemInsteadOfAFixedTimer() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertEquals(FaceIdController.LOG_WAIT_MS, out.lastWaitMs)
    }

    @Test
    fun theFailureEndingIsCountedFromTheStartLineNotFromTheAnimation() {
        faceOnly(logAvailable = false)
        c.onScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        assertEquals(FaceIdController.REAL_WAIT_MS + FaceUnlockCoordinator.SCREEN_ON_LAG_MS, out.lastWaitMs)
        out.scanEnds()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 5000)
        c.onScreenOn(locked = true, now = 5750)
        assertEquals(FaceIdController.REAL_WAIT_MS, out.lastWaitMs)
    }

    @Test
    fun noRestartWhileAScanIsShowing() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Start, 50)
        assertEquals(1, out.starts)
    }

    @Test
    fun faceOnlyAcceptsOnTheSuccessLineAndTheUnlockThatFollowsLeavesItAlone() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000)
        assertEquals(1, out.accepts)
        c.onUserPresent(1500)
        assertEquals(1, out.accepts)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun theUnlockMayComeBeforeTheSuccessLine() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(1000)
        c.onLog(FaceUnlockLogEvent.Success, 1100)
        assertEquals(1, out.accepts)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun aSwipeLongAfterTheSuccessLineStillPlaysTheTickAndIsNotCancelled() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000)
        assertEquals(1, out.accepts)
        c.onUserPresent(1000 + FaceUnlockCoordinator.CONFIRM_WINDOW_MS + 1)
        out.runLater()
        assertEquals(1, out.accepts)
        assertEquals(0, out.cancels)
    }

    @Test
    fun aLooseSuccessLineNeedsTheUnlockBroadcastToo() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        assertEquals(0, out.accepts)
        c.onUserPresent(1500)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aLooseSuccessLineWithNoUnlockNeverPlaysTheTick() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        out.runLater()
        assertEquals(0, out.accepts)
        assertTrue(out.scanning)
    }

    @Test
    fun aLooseSuccessLineTooFarFromTheUnlockIsNotConfirmed() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        c.onUserPresent(1000 + FaceUnlockCoordinator.CONFIRM_WINDOW_MS + 1)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aFingerprintTypeAfterTheFaceWasAcceptedDoesNotCancelTheTick() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onUnlockType(UnlockType.Fingerprint, 200)
        assertTrue(out.scanning)
        assertEquals(0, out.cancels)
        assertEquals(0, out.fingerprints)
    }

    @Test
    fun theNextScanCanBeAcceptedAgain() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        out.scanEnds()
        c.onScreenOff()
        c.onScreenOn(locked = true, now = 10_000)
        c.onLog(FaceUnlockLogEvent.Success, 10_100)
        assertEquals(2, out.accepts)
    }

    @Test
    fun aFingerprintUnlockCancelsTheFaceOnlyScanQuietly() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(2000)
        assertEquals(0, out.accepts)
        assertEquals(0, out.cancels)
        out.runLater()
        assertEquals(0, out.accepts)
        assertEquals(1, out.cancels)
    }

    @Test
    fun anyUnlockAcceptsOnTheUnlockAlone() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(900)
        assertEquals(1, out.accepts)
    }

    @Test
    fun withoutLogAccessFaceOnlyFallsBackToAnyUnlock() {
        faceOnly(logAvailable = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(900)
        assertEquals(1, out.accepts)
    }

    @Test
    fun theScreenTurningOffCancelsTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onScreenOff()
        assertEquals(1, out.cancels)
        assertFalse(out.scanning)
    }

    @Test
    fun nothingHappensWhenFaceIdIsOff() {
        c.configure(enabled = false, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Start, 10)
        c.onUserPresent(20)
        assertEquals(0, out.starts)
        assertEquals(0, out.accepts)
    }

    @Test
    fun turningTheEventOffCancelsARunningScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = true)
        assertEquals(1, out.cancels)
    }

    @Test
    fun anUnlockedScreenOnDoesNotStartAScan() {
        faceOnly()
        c.onScreenOn(locked = false, now = 0)
        assertEquals(0, out.starts)
    }

    @Test
    fun modeIdsRoundTrip() {
        assertEquals(FaceUnlockMode.Any, FaceUnlockMode.fromId("any"))
        assertEquals(FaceUnlockMode.FaceOnly, FaceUnlockMode.fromId("face_only"))
        assertEquals(FaceUnlockMode.FaceOnly, FaceUnlockMode.fromId("junk"))
        assertTrue(FaceUnlockCoordinator.CONFIRM_GRACE_MS < FaceUnlockCoordinator.CONFIRM_WINDOW_MS)
    }

    @Test
    fun aFingerprintUnlockTurnsTheScanIntoTheFingerprintAnimation() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        assertTrue(out.scanning)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.cancels)
        assertEquals(0, out.accepts)
        // The unlock broadcast that follows finds nothing to do.
        c.onUserPresent(150)
        assertEquals(0, out.accepts)
        assertTrue(out.queued.isEmpty())
    }

    @Test
    fun aPinUnlockEndsTheScanAtOnce() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Credential, 100)
        assertFalse(out.scanning)
        assertEquals(0, out.accepts)
    }

    @Test
    fun swipingUpToThePinScreenEndsTheScanAtOnce() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onBouncerShown()
        assertFalse(out.scanning)
        assertEquals(1, out.cancels)
        assertEquals(0, out.accepts)
        // The unlock broadcast after typing the PIN finds nothing to do.
        c.onUserPresent(3000)
        assertEquals(0, out.accepts)
        assertTrue(out.queued.isEmpty())
    }

    @Test
    fun thePinScreenDoesNotCancelAnAcceptedFace() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        assertEquals(1, out.accepts)
        c.onBouncerShown()
        assertEquals(0, out.cancels)
    }

    @Test
    fun anAnyUnlockKeepsTheScanWhenThePinScreenOpens() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOn(locked = true, now = 0)
        c.onBouncerShown()
        assertTrue(out.scanning)
        c.onUserPresent(3000)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aFaceUnlockTypeDoesNotCancelTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onUnlockType(UnlockType.Face, 100)
        assertTrue(out.scanning)
        c.onUserPresent(200)
        assertEquals(1, out.accepts)
    }

    @Test
    fun anAnyUnlockPlaysTheFingerprintAnimationForAFingerAndIgnoresAPin() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Credential, 50)
        assertTrue(out.scanning)
        assertEquals(0, out.cancels)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        assertTrue(out.scanning)
        assertEquals(1, out.fingerprints)
        // the unlock broadcast that follows must not play the face tick on top
        c.onUserPresent(200)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aSessionEndLineDoesNotWipeAWaitingSuccess() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onLog(FaceUnlockLogEvent.End, 110)
        c.onUserPresent(300)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aFingerprintUnlockFromTheDarkScreenPlaysTheFingerprintAnimationAtOnce() {
        faceOnly()
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.starts)
        assertEquals(1, out.directFingerprints)
        // the unlock broadcast that follows must not play the face tick on top
        c.onUserPresent(1300)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aFingerprintUnlockFromTheDarkScreenAlsoPlaysWhenAnyUnlockIsAllowed() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(1, out.directFingerprints)
        assertEquals(0, out.starts)
    }

    @Test
    fun aFingerprintUnlockWithTheFeatureOffPlaysNothing() {
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = true)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.directFingerprints)
        assertFalse(out.scanning)
    }

    @Test
    fun aLaterScreenOnStillStartsTheScan() {
        faceOnly()
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1000 + FaceUnlockCoordinator.NON_FACE_LEAD_MS + 1)
        assertTrue(out.scanning)
    }

    private fun appOn() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
    }

    @Test
    fun aDismissedPromptCancelsARunningAppScanThatWasNotAccepted() {
        appOn()
        c.onAppLockRequired()
        c.onLog(FaceUnlockLogEvent.Start, 500, fromApp = true)
        c.onAppPromptDismissed()
        assertEquals(1, out.cancels)
    }

    @Test
    fun aDismissedPromptCancelsTheEarlyScanBeforeItsStartLine() {
        appOn()
        c.onAppLockRequired()
        c.onAppPromptDismissed()
        assertEquals(1, out.cancels)
    }

    @Test
    fun aDismissedPromptDoesNotCancelAnAcceptedScanOrWhenNothingRuns() {
        appOn()
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 100, strong = true)
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
    }

    @Test
    fun aDismissedPromptDoesNotEndTheLockScreenScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
    }

    @Test
    fun theAppLockLineStartsTheAppScanBeforeItsStartLine() {
        appOn()
        c.onAppLockRequired()
        assertEquals(1, out.appStarts)
        c.onLog(FaceUnlockLogEvent.Start, 500, fromApp = true)
        assertEquals(1, out.appStarts)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun theEarlyAppScanIsCancelledWhenNoStartLineFollows() {
        appOn()
        c.onAppLockRequired()
        out.runLater()
        assertEquals(1, out.cancels)
    }

    @Test
    fun aStaleEndLineBeforeTheStartLineDoesNotCancelTheEarlyAppScan() {
        appOn()
        c.onAppLockRequired()
        c.onLog(FaceUnlockLogEvent.End, 100)
        assertEquals(0, out.cancels)
        c.onLog(FaceUnlockLogEvent.Start, 500, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 3000)
        out.runLater()
        assertEquals(0, out.cancels)
        assertEquals(1, out.rejects)
    }

    @Test
    fun aFingerprintBeforeTheStartLineTurnsTheEarlyAppScanIntoTheFingerprint() {
        appOn()
        c.onAppLockRequired()
        c.onUnlockType(UnlockType.Fingerprint, 200)
        assertEquals(1, out.fingerprints)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun theAppLockLineDoesNothingWhenAppFaceIdIsOffLockedOrAlreadyScanning() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = false)
        c.syncState(screenOn = true, locked = false)
        c.onAppLockRequired()
        assertEquals(0, out.appStarts)
        appOn()
        c.syncState(screenOn = true, locked = true)
        c.onAppLockRequired()
        assertEquals(0, out.appStarts)
        c.syncState(screenOn = true, locked = false)
        c.onAppLockRequired()
        c.onAppLockRequired()
        assertEquals(1, out.appStarts)
    }

    @Test
    fun anAppStartLineWhileUnlockedStartsTheAppScanNotTheLockScreenOne() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(1, out.appStarts)
        assertEquals(0, out.starts)
    }

    @Test
    fun anAppStartLineDoesNothingWhenAppFaceIdIsOff() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = false)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(0, out.appStarts)
    }

    @Test
    fun anAppStartLineOnTheLockScreenIsIgnored() {
        appOn()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(0, out.appStarts)
    }

    @Test
    fun appFaceIdWorksWithTheLockScreenSwitchOff() {
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = false, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(1, out.appStarts)
    }

    @Test
    fun aStrongSuccessLineAcceptsTheAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 800, strong = true)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aLooseSuccessLineDoesNotAcceptTheAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 800, strong = false)
        assertEquals(0, out.accepts)
    }

    @Test
    fun theAppLockUnlockedLineAcceptsAScanThatIsStillRunning() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onAppUnlocked(900)
        assertEquals(1, out.accepts)
    }

    @Test
    fun theAppLockUnlockedLineDoesNothingWithoutAScan() {
        appOn()
        c.onAppUnlocked(900)
        assertEquals(0, out.accepts)
    }

    @Test
    fun theFaceClientClosingWithoutAMatchPlaysTheNoFaceEndingOfTheAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        assertEquals(0, out.rejects)
        out.runLater()
        assertEquals(1, out.rejects)
        assertEquals(0, out.cancels)
    }

    @Test
    fun aSuccessLineRightAfterTheEndLineStillWinsTheAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        c.onLog(FaceUnlockLogEvent.Success, 2100, strong = true)
        out.runLater()
        assertEquals(1, out.accepts)
        assertEquals(0, out.rejects)
    }

    @Test
    fun aDismissedPromptAfterTheEndLineCancelsInsteadOfPlayingTheNoFaceEnding() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        c.onAppPromptDismissed()
        out.runLater()
        assertEquals(1, out.cancels)
        assertEquals(0, out.rejects)
    }

    @Test
    fun aPinUnlockEndsTheAppScanEvenInAnyUnlockMode() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onUnlockType(UnlockType.Credential, 3000)
        assertEquals(1, out.cancels)
        c.onAppUnlocked(3400)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aFingerprintUnlockJustBeforeTheAppUnlockedLineDoesNotAccept() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        assertEquals(1, out.fingerprints)
        c.onAppUnlocked(1100)
        assertEquals(0, out.accepts)
    }

    @Test
    fun turningAppFaceIdOffCancelsARunningAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = false)
        assertEquals(1, out.cancels)
    }

    @Test
    fun theUnlockBroadcastAndPinScreenDoNotTouchAnAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onBouncerShown()
        c.onUserPresent(500)
        assertEquals(0, out.cancels)
        assertTrue(out.scanning)
    }

    @Test
    fun aStartLineSeenLongAgoNoLongerSilencesTheScreenOnStart() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOff()
        c.onScreenOn(locked = true, now = FaceUnlockCoordinator.START_TRUST_MS + 1)
        assertEquals(1, out.starts)
    }

    @Test
    fun aFingerprintLineArrivingAfterTheScreenOnStillPlaysTheFingerprintAnimation() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 10_000)
        assertEquals(0, out.starts)
        c.onUnlockType(UnlockType.Fingerprint, 10_020)
        assertEquals(1, out.directFingerprints)
        c.onUserPresent(10_300)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aFingerprintOnTheLockScreenAfterTheScanEndedPlaysTheFingerprintAnimation() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onUnlockType(UnlockType.Fingerprint, 8000)
        assertEquals(1, out.directFingerprints)
    }

    @Test
    fun aFingerprintWhileUnlockedAndLongAfterScreenOnPlaysNothing() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onUserPresent(500)
        c.onUnlockType(UnlockType.Fingerprint, 8000)
        assertEquals(0, out.directFingerprints)
    }

    @Test
    fun theScreenTurningOffStopsTheRunningScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertTrue(out.scanning)
        c.onScreenOff()
        assertFalse(out.scanning)
    }

    @Test
    fun aScanLeftOverFromBeforeTheScreenOffIsStoppedAtScreenOn() {
        faceOnly()
        out.scanning = true
        c.onScreenOn(locked = true, now = 100)
        assertEquals(1, out.cancels)
    }

    @Test
    fun withTheFingerprintAnimationOffAFingerprintEndsTheScanQuietly() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 500)
        assertEquals(0, out.fingerprints)
        assertEquals(1, out.cancels)
        assertEquals(0, out.accepts)
    }

    @Test
    fun withTheFingerprintAnimationOffTheDarkScreenFingerprintPlaysNothing() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.directFingerprints)
        assertEquals(0, out.starts)
    }

    @Test
    fun withTheFingerprintAnimationOffAnyUnlockStillPlaysTheTickOnTheBroadcast() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 500)
        assertEquals(0, out.fingerprints)
        c.onUserPresent(600)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aFailureLineRejectsTheLockScreenScanAtOnce() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        assertTrue(out.scanning)
        c.onLog(FaceUnlockLogEvent.Failure, 3030)
        assertEquals(1, out.rejects)
        assertEquals(0, out.cancels)
    }

    @Test
    fun aFailureLineDoesNotRejectTheFingerprintAnimationOrAnAcceptedFace() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        c.onLog(FaceUnlockLogEvent.Failure, 200)
        assertEquals(0, out.rejects)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Success, 1200, strong = true)
        c.onLog(FaceUnlockLogEvent.Failure, 1300)
        assertEquals(0, out.rejects)
    }

    @Test
    fun aFailureLineWithNoScanShowingDoesNothingAndCancelsAWaitingStart() {
        faceOnly()
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Failure, 1200)
        c.onScreenOn(locked = true, now = 1300)
        assertEquals(0, out.starts)
        assertEquals(0, out.rejects)
    }

    @Test
    fun aFailureLineRejectsTheAppScanOnceItsOwnStartLineCame() {
        appOn()
        c.onAppLockRequired()
        c.onLog(FaceUnlockLogEvent.Failure, 100)
        assertEquals(0, out.rejects)
        c.onLog(FaceUnlockLogEvent.Start, 200, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3200)
        assertEquals(1, out.rejects)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun aFailureLineDoesNotRejectAnAcceptedAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 900, strong = true)
        c.onLog(FaceUnlockLogEvent.Failure, 1000)
        assertEquals(0, out.rejects)
    }

    @Test
    fun aFingerprintAfterTheAppFaceFailedPlaysTheFingerprintAnimationAtOnce() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3000)
        out.scanEnds()
        c.onUnlockType(UnlockType.Fingerprint, 5000)
        assertEquals(1, out.appFingerprints)
        assertEquals(0, out.directFingerprints)
    }

    @Test
    fun aFingerprintInAnAppPromptThatIsGoneDoesNotPlay() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        out.scanEnds()
        c.onAppPromptDismissed()
        c.onUnlockType(UnlockType.Fingerprint, 5000)
        assertEquals(0, out.appFingerprints)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 6000, fromApp = true)
        out.scanEnds()
        out.runLater()
        c.onUnlockType(UnlockType.Fingerprint, 70_000)
        assertEquals(0, out.appFingerprints)
    }

    @Test
    fun aFingerprintTurnsTheRunningAppScanIntoTheFingerprintWhateverItIsShowing() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3000)
        c.onUnlockType(UnlockType.Fingerprint, 3100)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.appFingerprints)
    }

    @Test
    fun aFingerprintOnTheLockScreenAfterAnAppPromptNeverPlaysTheAppAnimation() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3000)
        out.scanEnds()
        c.onScreenOff()
        c.onScreenOn(locked = true, now = 8000)
        out.scanEnds()
        c.onUnlockType(UnlockType.Fingerprint, 10_000)
        assertEquals(0, out.appFingerprints)
        assertEquals(1, out.directFingerprints)
    }

    @Test
    fun aStrongSuccessAfterTheScanWasLostStillPlaysTheTick() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Success, 1200, strong = true)
        assertEquals(2, out.starts)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aLateStrongSuccessDoesNotPlayTheTickAgain() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onLog(FaceUnlockLogEvent.Success, 800, strong = true)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Success, 1500, strong = true)
        assertEquals(1, out.starts)
        assertEquals(1, out.accepts)
    }

    @Test
    fun aStrongSuccessLongAfterTheStartLineIsIgnoredWithoutAScan() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Success, 10_000, strong = true)
        assertEquals(1, out.starts)
        assertEquals(0, out.accepts)
    }

    @Test
    fun aFingerDuringTheFaceScanSwitchesToTheFingerprintAnimationAtOnce() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        assertEquals(1, out.starts)
        c.onUnlockType(UnlockType.Fingerprint, 400)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.cancels)
    }

    @Test
    fun aFingerFromTheDarkScreenStartsTheFingerprintScanBeforeTheScreenOnBroadcast() {
        faceOnly()
        c.onScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        assertEquals(1, out.directFingerprints)
    }

    @Test
    fun theScreenOnBroadcastAfterThatDoesNotStopTheFingerprintScan() {
        faceOnly()
        c.onScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        c.onScreenOn(locked = false, now = 900)
        assertTrue(out.scanning)
        assertEquals(0, out.cancels)
    }

    @Test
    fun theEndOfTheFaceSessionWithoutAMatchRejectsTheScanSoon() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onLog(FaceUnlockLogEvent.End, 4900)
        assertEquals(0, out.rejects)
        out.runLater()
        assertEquals(1, out.rejects)
    }

    @Test
    fun aMatchRightAfterTheEndKeepsTheScan() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onLog(FaceUnlockLogEvent.End, 900)
        c.onLog(FaceUnlockLogEvent.Success, 950, strong = true)
        out.runLater()
        assertEquals(0, out.rejects)
        assertEquals(1, out.accepts)
    }

    @Test
    fun anEarlyAppScanAnchorsWhenTheRealScanStarts() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
        c.onAppLockRequired()
        assertEquals(1, out.earlyAppStarts)
        assertEquals(0, out.anchors)
        c.onLog(FaceUnlockLogEvent.Start, 1000L, strong = true, fromApp = true)
        assertEquals(1, out.anchors)
        assertEquals(1, out.appStarts)
    }
}
