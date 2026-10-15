/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitMotionTest {

    @Test
    fun wantedIsOpeningUntilTheProgressIsFull() {
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0f))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0.5f))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0.998f))
        assertEquals(SplitPhase.Open, SplitMotion.phase(wanted = true, progress = 0.999f))
        assertEquals(SplitPhase.Open, SplitMotion.phase(wanted = true, progress = 1f))
    }

    @Test
    fun notWantedIsClosingUntilTheProgressIsGone() {
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = 1f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = 0.3f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT + 0.002f))
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT))
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = 0f))
    }

    @Test
    fun aCloseThatStartsMidOpenTurnsAroundInPlace() {
        // Opening at 0.4, the island is no longer wanted: the same progress is now a close (no jump to 0 or 1).
        assertEquals(SplitPhase.Opening, SplitMotion.phase(true, 0.4f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(false, 0.4f))
    }

    @Test
    fun theTwoIslandLayoutIsComposedInEveryPhaseButClosed() {
        assertFalse(SplitMotion.twoIslandLayout(SplitPhase.Closed))
        assertTrue(SplitMotion.twoIslandLayout(SplitPhase.Opening))
        assertTrue(SplitMotion.twoIslandLayout(SplitPhase.Open))
        assertTrue(SplitMotion.twoIslandLayout(SplitPhase.Closing))
    }

    @Test
    fun theCardSlotIsTheSwapsInEveryPhaseButOpen() {
        assertTrue(SplitMotion.swapping(SplitPhase.Opening))
        assertTrue(SplitMotion.swapping(SplitPhase.Closing))
        assertTrue(SplitMotion.swapping(SplitPhase.Closed))
        assertFalse(SplitMotion.swapping(SplitPhase.Open))
    }

    @Test
    fun theContentReflowsOnceAtTheSameProgressOpeningAndClosing() {
        assertFalse(SplitMotion.reflowed(0f))
        assertFalse(SplitMotion.reflowed(0.29f))
        assertTrue(SplitMotion.reflowed(SplitMotion.REFLOW_AT))
        assertTrue(SplitMotion.reflowed(1f))
    }

    @Test
    fun theReflowFadeDipsToZeroAtTheSwitchAndIsFullAwayFromIt() {
        assertEquals(0f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(0f), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(1f), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT - SplitMotion.REFLOW_FADE_HALF), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF), 0.0001f)
        assertEquals(0.5f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF / 2), 0.0001f)
    }

    @Test
    fun theReflowFadeIsSymmetricAroundTheSwitch() {
        assertEquals(SplitMotion.reflowAlpha(0.42f), SplitMotion.reflowAlpha(0.58f), 0.0001f)
    }

    @Test
    fun theIslandShadowsAreOffWhileTheSplitMovesAndFullAtRest() {
        assertEquals(0f, SplitMotion.shadowStrength(0f), 0f)
        assertEquals(0f, SplitMotion.shadowStrength(0.5f), 0f)
        assertEquals(0f, SplitMotion.shadowStrength(SplitMotion.SHADOW_FROM), 0f)
        assertEquals(1f, SplitMotion.shadowStrength(1f), 0f)
    }

    @Test
    fun theShadowComesInSmoothlyOverTheLastPartOfTheOpening() {
        val a = SplitMotion.shadowStrength(0.93f)
        val b = SplitMotion.shadowStrength(0.97f)
        assertTrue(a in 0f..1f && b in 0f..1f && b > a)
    }

    // --- The slow spring tail is trimmed from the geometry ---

    @Test
    fun theGeometryIsZeroThroughTheTailAndOneAtTheFullProgress() {
        assertEquals(0f, SplitMotion.geometry(0f), 0f)
        assertEquals(0f, SplitMotion.geometry(SplitMotion.TAIL / 2), 0f)
        assertEquals(0f, SplitMotion.geometry(SplitMotion.TAIL), 0f)
        assertEquals(1f, SplitMotion.geometry(1f), 0.0001f)
    }

    @Test
    fun theGeometryRunsStraightBetweenTheTailAndTheFullProgress() {
        val mid = (SplitMotion.TAIL + 1f) / 2f
        assertEquals(0.5f, SplitMotion.geometry(mid), 0.0001f)
        assertEquals(0.25f, SplitMotion.geometry(SplitMotion.TAIL + 0.25f * (1f - SplitMotion.TAIL)), 0.0001f)
        assertTrue(SplitMotion.geometry(0.6f) > SplitMotion.geometry(0.5f))
    }

    @Test
    fun theGeometryStaysInsideZeroToOneForAProgressOutsideIt() {
        assertEquals(0f, SplitMotion.geometry(-0.3f), 0f)
        assertEquals(1f, SplitMotion.geometry(1.4f), 0f)
    }

    @Test
    fun theSplitIsGoneAtTheSameProgressWhereTheGeometryReachesZero() {
        // No pop at the hand-off to the single surface: the islands are exactly the single island's size when the phase turns Closed.
        assertEquals(SplitMotion.TAIL, SplitMotion.GONE_AT, 0f)
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT))
        assertEquals(0f, SplitMotion.geometry(SplitMotion.GONE_AT), 0f)
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT + 0.01f))
        assertTrue(SplitMotion.geometry(SplitMotion.GONE_AT + 0.01f) > 0f)
    }

    @Test
    fun theOpeningIsStillOpeningThroughTheTail() {
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = SplitMotion.TAIL / 2))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = SplitMotion.TAIL))
    }

    @Test
    fun aFingerDrivenProgressIsOneToOneWithTheGeometry() {
        for (i in 0..10) {
            val g = i / 10f
            assertEquals(g, SplitMotion.geometry(SplitMotion.fromGeometry(g)), 0.0001f)
        }
        assertEquals(SplitMotion.GONE_AT, SplitMotion.fromGeometry(0f), 0f)
        assertEquals(1f, SplitMotion.fromGeometry(1f), 0f)
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.fromGeometry(0f)))
    }

    @Test
    fun theReflowAndTheShadowStillSitInsideTheMovingPartOfTheProgress() {
        // They read the raw progress. The re-wrap point is well inside the geometry's run (after the tail, before the end), so the
        // islands are neither at the single island's size nor at the split's final size when the lines switch.
        assertTrue(SplitMotion.REFLOW_AT > SplitMotion.TAIL + SplitMotion.REFLOW_FADE_HALF)
        // The re-wrap moved early (REFLOW_AT 0.3, geometry about 0.22) so it happens while the beside island is still a sliver.
        assertTrue(SplitMotion.geometry(SplitMotion.REFLOW_AT) in 0.15f..0.7f)
        // The fade is over before the shadow starts, and the shadow starts after the tail.
        assertTrue(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF < SplitMotion.SHADOW_FROM)
        assertTrue(SplitMotion.SHADOW_FROM > SplitMotion.TAIL)
    }

    @Test
    fun theForegroundAlphaIsStillReadFromTheRawProgress() {
        // Raw 0.5 is the dip, not geometry 0.5 (which would be raw 0.55).
        assertEquals(0f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT), 0.0001f)
        assertTrue(SplitMotion.reflowAlpha(SplitMotion.fromGeometry(0.5f)) > 0f)
    }

    @Test
    fun theTwoIslandLayoutIsHeldUntilTheShellIsReady() {
        // Closing: held whatever the shell says.
        assertTrue(SplitMotion.holdTwoIslandLayout(true, false, 0.5f, shellSingleReady = true))
        // Progress gone but the shell is not the single island yet: still held (no flash on this frame).
        assertTrue(SplitMotion.holdTwoIslandLayout(true, false, 0f, shellSingleReady = false))
        // The shell confirmed: the content may flip.
        assertFalse(SplitMotion.holdTwoIslandLayout(true, false, 0f, shellSingleReady = true))
        // Never held when the island is wanted (that is the open layout), or when no split was ever shown.
        assertFalse(SplitMotion.holdTwoIslandLayout(true, true, 0.5f, shellSingleReady = false))
        assertFalse(SplitMotion.holdTwoIslandLayout(false, false, 0f, shellSingleReady = false))
    }
}
