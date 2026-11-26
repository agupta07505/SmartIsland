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

    private fun faceOnlyWithScreenOff() {
        faceOnly()
        c.onScreenOff()
    }

    @Test
    fun startLineWhileLockedStartsTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 100)
        assertEquals(2, out.starts)
    }

    @Test
    fun screenOnStartsWithoutLine() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertEquals(1, out.starts)
    }

    @Test
    fun screenOnAfterLineNoStart() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 10_000)
        assertEquals(0, out.starts)
    }

    @Test
    fun startBeforeScreenOn() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        assertEquals(0, out.starts)
        c.onScreenOn(locked = true, now = 1500)
        assertEquals(1, out.starts)
    }

    @Test
    fun oldStartLineDoesNotStartItLater() {
        faceOnlyWithScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 20_000)
        // the log is trusted from now on, so nothing starts by the screen alone
        assertEquals(0, out.starts)
    }

    @Test
    fun scanPlaysAgainWhenTheFaceComesBack() {
        faceOnlyWithScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOn(locked = true, now = 5)
        assertEquals(1, out.starts)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Start, 9000)
        assertEquals(2, out.starts)
    }

    @Test
    fun scanDuringWake() {
        faceOnlyWithScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        assertEquals(1, out.starts)
    }

    @Test
    fun lateScreenOnLeavesRunningScanAlone() {
        faceOnlyWithScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onScreenOn(locked = true, now = 1750)
        assertEquals(1, out.starts)
        assertEquals(0, out.cancels)
        assertTrue(out.scanning)
    }

    @Test
    fun earlyFaceMatch() {
        faceOnlyWithScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Success, 1573, strong = true)
        assertEquals(1, out.starts)
        assertEquals(1, out.accepts)
        c.onScreenOn(locked = false, now = 1750)
        assertEquals(0, out.cancels)
        assertEquals(1, out.starts)
    }

    @Test
    fun earlyWeakMatch() {
        faceOnlyWithScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Success, 1573, strong = false)
        assertEquals(0, out.starts)
        assertEquals(0, out.accepts)
    }

    @Test
    fun logDrivenScanTimeout() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertEquals(FaceIdController.LOG_WAIT_MS, out.lastWaitMs)
    }

    @Test
    fun scanTimeoutOrigin() {
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
    fun faceOnlyAcceptsOnSuccessLine() {
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
    fun unlockMayComeBeforeTheSuccessLine() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(1000)
        c.onLog(FaceUnlockLogEvent.Success, 1100)
        assertEquals(1, out.accepts)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun lateSwipeAfterSuccessLineStillTicks() {
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
    fun looseSuccessNeedsUnlock() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        assertEquals(0, out.accepts)
        c.onUserPresent(1500)
        assertEquals(1, out.accepts)
    }

    @Test
    fun looseSuccessNoUnlockNoTick() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        out.runLater()
        assertEquals(0, out.accepts)
        assertTrue(out.scanning)
    }

    @Test
    fun looseSuccessFarFromUnlock() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 1000, strong = false)
        c.onUserPresent(1000 + FaceUnlockCoordinator.CONFIRM_WINDOW_MS + 1)
        assertEquals(0, out.accepts)
    }

    @Test
    fun lateFingerprintAfterFaceMatch() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onUnlockType(UnlockType.Fingerprint, 200)
        assertTrue(out.scanning)
        assertEquals(0, out.cancels)
        assertEquals(0, out.fingerprints)
    }

    @Test
    fun nextScanCanBeAcceptedAgain() {
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
    fun fingerprintCancelsFaceQuietly() {
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
    fun noLogAccessFallsToAnyUnlock() {
        faceOnly(logAvailable = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUserPresent(900)
        assertEquals(1, out.accepts)
    }

    @Test
    fun screenTurningOffCancelsTheScan() {
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
    fun eventOffCancelsScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = true)
        assertEquals(1, out.cancels)
    }

    @Test
    fun unlockedScreenOnDoesNotStartAScan() {
        faceOnly()
        c.onScreenOn(locked = false, now = 0)
        assertEquals(0, out.starts)
    }

    @Test
    fun modeIdsRoundTrip() {
        assertEquals(FaceUnlockMode.Any, FaceUnlockMode.fromId("any"))
        assertEquals(FaceUnlockMode.FaceOnly, FaceUnlockMode.fromId("face_only"))
        assertEquals(FaceUnlockMode.FaceOnly, FaceUnlockMode.fromId("junk"))
    }

    @Test
    fun fingerprintReplacesScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        assertTrue(out.scanning)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.cancels)
        assertEquals(0, out.accepts)
        // the unlock broadcast that follows finds nothing to do
        c.onUserPresent(150)
        assertEquals(0, out.accepts)
        assertTrue(out.queued.isEmpty())
    }

    @Test
    fun pinUnlockEndsTheScanAtOnce() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Credential, 100)
        assertFalse(out.scanning)
        assertEquals(0, out.accepts)
    }

    @Test
    fun pinSwipeEndsScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onBouncerShown()
        assertFalse(out.scanning)
        assertEquals(1, out.cancels)
        assertEquals(0, out.accepts)
        // the unlock broadcast after typing the PIN finds nothing to do
        c.onUserPresent(3000)
        assertEquals(0, out.accepts)
        assertTrue(out.queued.isEmpty())
    }

    @Test
    fun pinKeepsAcceptedFace() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        assertEquals(1, out.accepts)
        c.onBouncerShown()
        assertEquals(0, out.cancels)
    }

    @Test
    fun anyUnlockKeepsScanOnPin() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOn(locked = true, now = 0)
        c.onBouncerShown()
        assertTrue(out.scanning)
        c.onUserPresent(3000)
        assertEquals(1, out.accepts)
    }

    @Test
    fun faceUnlockTypeDoesNotCancelTheScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onUnlockType(UnlockType.Face, 100)
        assertTrue(out.scanning)
        c.onUserPresent(200)
        assertEquals(1, out.accepts)
    }

    @Test
    fun anyUnlockPlaysForFingerIgnoresPin() {
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
    fun sessionEndKeepsWaitingSuccess() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onLog(FaceUnlockLogEvent.Success, 100)
        c.onLog(FaceUnlockLogEvent.End, 110)
        c.onUserPresent(300)
        assertEquals(1, out.accepts)
    }

    @Test
    fun darkScreenFingerprintPlaysAtOnce() {
        faceOnlyWithScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.starts)
        assertEquals(1, out.directFingerprints)
        c.onUserPresent(1300)
        assertEquals(0, out.accepts)
    }

    @Test
    fun darkFingerprintAnyUnlock() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(1, out.directFingerprints)
        assertEquals(0, out.starts)
    }

    @Test
    fun fingerprintFeatureOffSilent() {
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = true)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.directFingerprints)
        assertFalse(out.scanning)
    }

    @Test
    fun laterScreenOnStillStartsTheScan() {
        faceOnlyWithScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1000 + FaceUnlockCoordinator.NON_FACE_LEAD_MS + 1)
        assertTrue(out.scanning)
    }

    private fun appOn() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
    }

    @Test
    fun promptDismissalCancelsScan() {
        appOn()
        c.onAppLockRequired()
        c.onLog(FaceUnlockLogEvent.Start, 500, fromApp = true)
        c.onAppPromptDismissed()
        assertEquals(1, out.cancels)
    }

    @Test
    fun dismissCancelsEarlyScan() {
        appOn()
        c.onAppLockRequired()
        c.onAppPromptDismissed()
        assertEquals(1, out.cancels)
    }

    @Test
    fun promptDismissalAfterAcceptance() {
        appOn()
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 100, strong = true)
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
    }

    @Test
    fun dismissKeepsLockScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        c.onAppPromptDismissed()
        assertEquals(0, out.cancels)
    }

    @Test
    fun appLockLineStartsEarly() {
        appOn()
        c.onAppLockRequired()
        assertEquals(1, out.appStarts)
        c.onLog(FaceUnlockLogEvent.Start, 500, fromApp = true)
        assertEquals(1, out.appStarts)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun earlyAppScanDropsNoStart() {
        appOn()
        c.onAppLockRequired()
        out.runLater()
        assertEquals(1, out.cancels)
    }

    @Test
    fun staleEndBeforeAppScan() {
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
    fun fingerprintBeforeScanStart() {
        appOn()
        c.onAppLockRequired()
        c.onUnlockType(UnlockType.Fingerprint, 200)
        assertEquals(1, out.fingerprints)
        out.runLater()
        assertEquals(0, out.cancels)
    }

    @Test
    fun appLockLineIgnoredOffLocked() {
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
    fun appStartUnlockedStarts() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(1, out.appStarts)
        assertEquals(0, out.starts)
    }

    @Test
    fun appStartFaceIdOffNoop() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = false)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(0, out.appStarts)
    }

    @Test
    fun appStartOnLockIgnored() {
        appOn()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(0, out.appStarts)
    }

    @Test
    fun appFaceIdLockSwitchOff() {
        c.configure(enabled = false, mode = FaceUnlockMode.FaceOnly, logAvailable = false, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        assertEquals(1, out.appStarts)
    }

    @Test
    fun strongSuccessLineAcceptsTheAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 800, strong = true)
        assertEquals(1, out.accepts)
    }

    @Test
    fun looseSuccessSkipsAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 800, strong = false)
        assertEquals(0, out.accepts)
    }

    @Test
    fun appUnlockedAcceptsRunning() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onAppUnlocked(900)
        assertEquals(1, out.accepts)
    }

    @Test
    fun appUnlockedNoScanNoop() {
        appOn()
        c.onAppUnlocked(900)
        assertEquals(0, out.accepts)
    }

    @Test
    fun clientCloseNoMatchSilent() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        assertEquals(0, out.rejects)
        out.runLater()
        assertEquals(1, out.rejects)
        assertEquals(0, out.cancels)
    }

    @Test
    fun successAfterEndStillWins() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        c.onLog(FaceUnlockLogEvent.Success, 2100, strong = true)
        out.runLater()
        assertEquals(1, out.accepts)
        assertEquals(0, out.rejects)
    }

    @Test
    fun dismissedPromptAfterEndLineCancels() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.End, 2000)
        c.onAppPromptDismissed()
        out.runLater()
        assertEquals(1, out.cancels)
        assertEquals(0, out.rejects)
    }

    @Test
    fun pinUnlockEndsAppScan() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true, appEnabled = true)
        c.syncState(screenOn = true, locked = false)
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onUnlockType(UnlockType.Credential, 3000)
        assertEquals(1, out.cancels)
        c.onAppUnlocked(3400)
        assertEquals(0, out.accepts)
    }

    @Test
    fun fingerprintBeforeAppUnlock() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        assertEquals(1, out.fingerprints)
        c.onAppUnlocked(1100)
        assertEquals(0, out.accepts)
    }

    @Test
    fun appFaceIdOffCancelsScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, appEnabled = false)
        assertEquals(1, out.cancels)
    }

    @Test
    fun unlockAndPinSpareAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onBouncerShown()
        c.onUserPresent(500)
        assertEquals(0, out.cancels)
        assertTrue(out.scanning)
    }

    @Test
    fun oldStartLineNoSilence() {
        faceOnly()
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onScreenOff()
        c.onScreenOn(locked = true, now = FaceUnlockCoordinator.START_TRUST_MS + 1)
        assertEquals(1, out.starts)
    }

    @Test
    fun lateFingerprintAnimates() {
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
    fun lockFingerprintAfterEndPlays() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onUnlockType(UnlockType.Fingerprint, 8000)
        assertEquals(1, out.directFingerprints)
    }

    @Test
    fun lateUnlockedFingerprint() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        out.scanEnds()
        c.onUserPresent(500)
        c.onUnlockType(UnlockType.Fingerprint, 8000)
        assertEquals(0, out.directFingerprints)
    }

    @Test
    fun screenTurningOffStopsTheRunningScan() {
        faceOnly()
        c.onScreenOn(locked = true, now = 0)
        assertTrue(out.scanning)
        c.onScreenOff()
        assertFalse(out.scanning)
    }

    @Test
    fun staleScanAfterWake() {
        faceOnly()
        out.scanning = true
        c.onScreenOn(locked = true, now = 100)
        assertEquals(1, out.cancels)
    }

    @Test
    fun fingerprintAnimationDisabled() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 500)
        assertEquals(0, out.fingerprints)
        assertEquals(1, out.cancels)
        assertEquals(0, out.accepts)
    }

    @Test
    fun animationOffDarkSilent() {
        c.configure(enabled = true, mode = FaceUnlockMode.FaceOnly, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOff()
        c.onUnlockType(UnlockType.Fingerprint, 1000)
        c.onScreenOn(locked = true, now = 1140)
        assertEquals(0, out.directFingerprints)
        assertEquals(0, out.starts)
    }

    @Test
    fun animationOffStillTicksOnBroadcast() {
        c.configure(enabled = true, mode = FaceUnlockMode.Any, logAvailable = true, fingerprintEnabled = false)
        c.onScreenOn(locked = true, now = 0)
        c.onUnlockType(UnlockType.Fingerprint, 500)
        assertEquals(0, out.fingerprints)
        c.onUserPresent(600)
        assertEquals(1, out.accepts)
    }

    @Test
    fun failureRejectsLockScan() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        assertTrue(out.scanning)
        c.onLog(FaceUnlockLogEvent.Failure, 3030)
        assertEquals(1, out.rejects)
        assertEquals(0, out.cancels)
    }

    @Test
    fun failureSparesFingerAndFace() {
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
    fun failureNoScanCancelsWait() {
        faceOnlyWithScreenOff()
        c.onLog(FaceUnlockLogEvent.Start, 1000)
        c.onLog(FaceUnlockLogEvent.Failure, 1200)
        c.onScreenOn(locked = true, now = 1300)
        assertEquals(0, out.starts)
        assertEquals(0, out.rejects)
    }

    @Test
    fun failureRejectsStartedAppScan() {
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
    fun failureKeepsAcceptedApp() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Success, 900, strong = true)
        c.onLog(FaceUnlockLogEvent.Failure, 1000)
        assertEquals(0, out.rejects)
    }

    @Test
    fun fingerprintAfterAppFailPlays() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3000)
        out.scanEnds()
        c.onUnlockType(UnlockType.Fingerprint, 5000)
        assertEquals(1, out.appFingerprints)
        assertEquals(0, out.directFingerprints)
    }

    @Test
    fun goneAppPromptFingerSilent() {
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
    fun fingerprintTakesAppScan() {
        appOn()
        c.onLog(FaceUnlockLogEvent.Start, 0, fromApp = true)
        c.onLog(FaceUnlockLogEvent.Failure, 3000)
        c.onUnlockType(UnlockType.Fingerprint, 3100)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.appFingerprints)
    }

    @Test
    fun lockFingerprintSkipsApp() {
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
    fun strongSuccessAfterLossTicks() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Success, 1200, strong = true)
        assertEquals(2, out.starts)
        assertEquals(1, out.accepts)
    }

    @Test
    fun lateStrongNoSecondTick() {
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
    fun staleFaceMatch() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        out.scanEnds()
        c.onLog(FaceUnlockLogEvent.Success, 10_000, strong = true)
        assertEquals(1, out.starts)
        assertEquals(0, out.accepts)
    }

    @Test
    fun fingerDuringFaceScanSwitchesAtOnce() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        assertEquals(1, out.starts)
        c.onUnlockType(UnlockType.Fingerprint, 400)
        assertEquals(1, out.fingerprints)
        assertEquals(0, out.cancels)
    }

    @Test
    fun darkFingerStartsEarly() {
        faceOnlyWithScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        assertEquals(1, out.directFingerprints)
    }

    @Test
    fun fingerprintSurvivesScreenOn() {
        faceOnlyWithScreenOff()
        c.syncState(screenOn = true, locked = true)
        c.onUnlockType(UnlockType.Fingerprint, 100)
        c.onScreenOn(locked = false, now = 900)
        assertTrue(out.scanning)
        assertEquals(0, out.cancels)
    }

    @Test
    fun faceEndNoMatchRejects() {
        faceOnly()
        c.syncState(screenOn = true, locked = true)
        c.onLog(FaceUnlockLogEvent.Start, 0)
        c.onLog(FaceUnlockLogEvent.End, 4900)
        assertEquals(0, out.rejects)
        out.runLater()
        assertEquals(1, out.rejects)
    }

    @Test
    fun matchRightAfterTheEndKeepsTheScan() {
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
    fun earlyAppScanAnchors() {
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
