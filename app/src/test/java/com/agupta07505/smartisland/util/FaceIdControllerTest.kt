/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FaceIdControllerTest {
    @Before
    fun reset() {
        FaceIdController.stop()
    }

    @After
    fun clean() {
        FaceIdController.stop()
    }

    @Test
    fun startMakesTheScanActiveAndStopClearsIt() {
        assertFalse(FaceIdController.state.value.active)
        FaceIdController.start()
        assertTrue(FaceIdController.state.value.active)
        FaceIdController.stop()
        assertFalse(FaceIdController.state.value.active)
    }

    @Test
    fun everyStartIsANewSession() {
        FaceIdController.start()
        val first = FaceIdController.state.value.session
        FaceIdController.start()
        assertEquals(first + 1, FaceIdController.state.value.session)
    }

    @Test
    fun finishOfAnOlderSessionIsIgnored() {
        FaceIdController.start()
        val old = FaceIdController.state.value.session
        FaceIdController.start()
        FaceIdController.finish(old)
        assertTrue(FaceIdController.state.value.active)
        FaceIdController.finish(FaceIdController.state.value.session)
        assertFalse(FaceIdController.state.value.active)
    }

    @Test
    fun faceUnlockIsNotACardSoItHasNoCardSwitches() {
        assertFalse(IslandEvent.FACE_UNLOCK.canSwitchAutoExpand)
        assertFalse(IslandEvent.FACE_UNLOCK.canHideOnLockScreen)
    }

    @Test
    fun aScanStartsWithNoFaceAcceptedUnlessTheDemoAsksForOne() {
        FaceIdController.start()
        assertFalse(FaceIdController.state.value.accepted)
        FaceIdController.start(accepted = true)
        assertTrue(FaceIdController.state.value.accepted)
    }

    @Test
    fun acceptMarksTheRunningScanAndIsIgnoredWhenNothingShows() {
        FaceIdController.accept()
        assertFalse(FaceIdController.state.value.accepted)
        FaceIdController.start()
        FaceIdController.accept()
        assertTrue(FaceIdController.state.value.accepted)
        FaceIdController.stop()
        assertFalse(FaceIdController.state.value.accepted)
    }

    @Test
    fun aNewScanForgetsTheLastAcceptance() {
        FaceIdController.start(accepted = true)
        FaceIdController.start()
        assertFalse(FaceIdController.state.value.accepted)
    }

    @Test
    fun aRealScanAndTheDemoShareTheSameWait() {
        FaceIdController.start()
        assertEquals(FaceIdController.DEMO_WAIT_MS, FaceIdController.state.value.waitMs)
        FaceIdController.startForUnlock()
        assertEquals(FaceIdController.REAL_WAIT_MS, FaceIdController.state.value.waitMs)
        assertFalse(FaceIdController.state.value.accepted)
        assertEquals(FaceIdController.DEMO_WAIT_MS, FaceIdController.REAL_WAIT_MS)
    }

    @Test
    fun cancelMarksTheRunningScanAndIsIgnoredWhenNothingShows() {
        FaceIdController.cancel()
        assertFalse(FaceIdController.state.value.cancelled)
        FaceIdController.startForUnlock()
        FaceIdController.cancel()
        assertTrue(FaceIdController.state.value.cancelled)
        assertTrue(FaceIdController.state.value.active)
        FaceIdController.finish(FaceIdController.state.value.session)
        assertFalse(FaceIdController.state.value.active)
        assertFalse(FaceIdController.state.value.cancelled)
    }

    @Test
    fun anAppScanWaitsForTheSystemInsteadOfTheShortTimer() {
        FaceIdController.startForApp()
        assertEquals(FaceIdController.APP_WAIT_MS, FaceIdController.state.value.waitMs)
        assertTrue(FaceIdController.APP_WAIT_MS > FaceIdController.REAL_WAIT_MS)
        assertFalse(FaceIdController.state.value.accepted)
    }

    @Test
    fun rejectMarksTheRunningScanUnlessItWasAlreadyAccepted() {
        FaceIdController.reject()
        assertFalse(FaceIdController.state.value.rejected)
        FaceIdController.startForApp()
        FaceIdController.reject()
        assertTrue(FaceIdController.state.value.rejected)
        FaceIdController.finish(FaceIdController.state.value.session)
        assertFalse(FaceIdController.state.value.rejected)
        FaceIdController.startForApp()
        FaceIdController.accept()
        FaceIdController.reject()
        assertFalse(FaceIdController.state.value.rejected)
    }

    @Test
    fun theLogWaitIsOnlyASafetyNetLongerThanTheDemoWait() {
        assertTrue(FaceIdController.LOG_WAIT_MS > FaceIdController.REAL_WAIT_MS)
    }

    @Test
    fun aNewScanIsNotCancelled() {
        FaceIdController.startForUnlock()
        FaceIdController.cancel()
        FaceIdController.startForUnlock()
        assertFalse(FaceIdController.state.value.cancelled)
    }

    @Test
    fun aScanIsSquareStyleUnlessCutoutIsAsked() {
        FaceIdController.start()
        assertEquals(FaceIdController.Style.Square, FaceIdController.state.value.style)
        FaceIdController.start(style = FaceIdController.Style.Cutout)
        assertEquals(FaceIdController.Style.Cutout, FaceIdController.state.value.style)
        FaceIdController.startForUnlock()
        assertEquals(FaceIdController.Style.Square, FaceIdController.state.value.style)
    }

    @Test
    fun unlockScanUsesTheGivenStyleAndDefaultsToSquare() {
        FaceIdController.startForUnlock()
        assertEquals(FaceIdController.Style.Square, FaceIdController.state.value.style)
        FaceIdController.startForUnlock(FaceIdController.Style.Cutout)
        assertEquals(FaceIdController.Style.Cutout, FaceIdController.state.value.style)
        assertEquals(FaceIdController.REAL_WAIT_MS, FaceIdController.state.value.waitMs)
    }

    @Test
    fun styleOfReadsTheSettingAndFallsBackToSquare() {
        assertEquals(FaceIdController.Style.Cutout, FaceIdController.styleOf("cutout"))
        assertEquals(FaceIdController.Style.Square, FaceIdController.styleOf("square"))
        assertEquals(FaceIdController.Style.SquareTick, FaceIdController.styleOf("square_tick"))
        assertEquals(FaceIdController.Style.Square, FaceIdController.styleOf("nonsense"))
    }

    @Test
    fun aNewScanResetsTheStyleOfTheLastOne() {
        FaceIdController.start(style = FaceIdController.Style.Cutout)
        FaceIdController.start()
        assertEquals(FaceIdController.Style.Square, FaceIdController.state.value.style)
    }

    @Test
    fun startFingerprintBeginsADirectFingerprintScanThatTheNextScanClears() {
        FaceIdController.startFingerprint(FaceIdController.Style.Cutout)
        val s = FaceIdController.state.value
        assertTrue(s.active && s.fingerprint && s.direct)
        assertEquals(FaceIdController.Style.Cutout, s.style)
        FaceIdController.start()
        assertFalse(FaceIdController.state.value.fingerprint)
        assertFalse(FaceIdController.state.value.direct)
    }

    @Test
    fun fingerprintMarksTheRunningScanOnceAndIsIgnoredWhenNothingShowsOrAFaceWasAccepted() {
        FaceIdController.fingerprint()
        assertFalse(FaceIdController.state.value.fingerprint)
        FaceIdController.start()
        FaceIdController.fingerprint()
        assertTrue(FaceIdController.state.value.fingerprint)
        FaceIdController.start()
        assertFalse(FaceIdController.state.value.fingerprint)
        FaceIdController.accept()
        FaceIdController.fingerprint()
        assertFalse(FaceIdController.state.value.fingerprint)
    }
}
