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

class PillEnterStepTest {

    @Test
    fun cardEntryTransition() {
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = 1, enterOffsetPx = 0f))
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = -1, enterOffsetPx = 0f))
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, 0f))
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, PILL_ENTER_REST_PX))
        assertEquals(PillEnterStep.Nothing, pillEnterStep(0, -0.2f))

        // the slide snaps to its own start, so the old offset does not matter
        assertEquals(PillEnterStep.Start, pillEnterStep(pendingEnterSign = 1, enterOffsetPx = -44f))

        // a stale offset of about -16 dp (-44 px) would leave the icon half outside the pill
        assertEquals(PillEnterStep.SettleStale, pillEnterStep(pendingEnterSign = 0, enterOffsetPx = -44f))
        assertEquals(PillEnterStep.SettleStale, pillEnterStep(pendingEnterSign = 0, enterOffsetPx = 12f))
    }

}

class PillOpenDragTest {
    @Test
    fun dragProgressBounds() {
        assertEquals(0f, PillOpenDrag.progress(5f, 10f, 100f), 0.0001f)
        assertEquals(0.5f, PillOpenDrag.progress(60f, 10f, 100f), 0.0001f)
        assertEquals(1f, PillOpenDrag.progress(500f, 10f, 100f), 0.0001f)
        // the finger coming back up lowers it again: nothing is remembered
        assertEquals(0.25f, PillOpenDrag.progress(35f, 10f, 100f), 0.0001f)
        assertEquals(0f, PillOpenDrag.progress(-20f, 10f, 100f), 0f)

        assertEquals(0f, PillOpenDrag.progress(50f, 10f, 0f), 0f)
    }

    @Test
    fun rangeFollowsTheCardHeightWithinLimits() {
        // a card 100 dp taller than the pill
        assertEquals(320f, PillOpenDrag.rangePx(34f, 134f, 2f), 0.001f)
        // a very short card still needs the minimum, a very tall one is capped
        assertEquals(PillOpenDrag.RANGE_MIN_DP, PillOpenDrag.rangePx(34f, 40f, 1f), 0.001f)
        assertEquals(PillOpenDrag.RANGE_MAX_DP, PillOpenDrag.rangePx(34f, 600f, 1f), 0.001f)
    }

    @Test
    fun commitsOnlyPastThePoint() {
        assertFalse(PillOpenDrag.commits(PillOpenDrag.COMMIT - 0.01f))
        assertTrue(PillOpenDrag.commits(PillOpenDrag.COMMIT))
        assertTrue(PillOpenDrag.commits(1f))
    }

    @Test
    fun lerpEndsAreExact() {
        assertEquals(112f, PillOpenDrag.lerp(112f, 360f, 0f), 0f)
        assertEquals(360f, PillOpenDrag.lerp(112f, 360f, 1f), 0.0001f)
        assertEquals(236f, PillOpenDrag.lerp(112f, 360f, 0.5f), 0.0001f)
    }

    @Test
    fun contentFadesDoNotCrossIntoAMuddyMiddle() {
        // the pill's content is gone before the card's content has fully arrived
        assertEquals(1f, PillOpenDrag.pillContentAlpha(0f), 0f)
        assertEquals(0f, PillOpenDrag.pillContentAlpha(PillOpenDrag.PILL_CONTENT_GONE_AT), 0f)
        assertEquals(0f, PillOpenDrag.cardContentAlpha(0f), 0f)
        assertEquals(0f, PillOpenDrag.cardContentAlpha(PillOpenDrag.CARD_CONTENT_FROM), 0f)
        assertEquals(1f, PillOpenDrag.cardContentAlpha(PillOpenDrag.CARD_CONTENT_FULL), 0f)
        assertEquals(1f, PillOpenDrag.cardContentAlpha(1f), 0f)
        assertTrue(PillOpenDrag.pillContentAlpha(0.2f) < 0.5f)
    }

    @Test
    fun cutoutDragRelease() {
        assertFalse(PillOpenDrag.peeksNextEvent(0.1f))
        assertTrue(PillOpenDrag.peeksNextEvent(PillOpenDrag.NEXT_EVENT_PEEK_AT))
        assertFalse(PillOpenDrag.nextEventOnCutout(true, false, false, 0.1f, true))
        assertTrue(PillOpenDrag.nextEventOnCutout(true, true, false, 0.3f, true))

        assertTrue(PillOpenDrag.nextEventOnCutout(true, true, false, 0.2f, true))
        assertFalse(PillOpenDrag.nextEventOnCutout(true, true, false, 0.05f, true))

        assertTrue(PillOpenDrag.nextEventOnCutout(true, true, true, 0.3f, false))
        assertFalse(PillOpenDrag.nextEventOnCutout(true, true, true, PillOpenDrag.NEXT_EVENT_PEEK_AT, false))
        assertFalse(PillOpenDrag.nextEventOnCutout(true, false, true, 0.3f, false))

        assertTrue(PillOpenDrag.nextEventOnCutout(false, false, false, 0f, true))
        assertFalse(PillOpenDrag.nextEventOnCutout(false, false, false, 0f, false))
    }

    @Test
    fun closeDragProgress() {
        assertEquals(1f, PillOpenDrag.closeProgress(0f, 10f, 100f), 0.0001f)
        assertEquals(1f, PillOpenDrag.closeProgress(-40f, 10f, 100f), 0.0001f)
        assertEquals(0.5f, PillOpenDrag.closeProgress(60f, 10f, 100f), 0.0001f)
        assertEquals(0f, PillOpenDrag.closeProgress(500f, 10f, 100f), 0.0001f)
        assertEquals(0.75f, PillOpenDrag.closeProgress(35f, 10f, 100f), 0.0001f)

        assertFalse(PillOpenDrag.closeCommits(PillOpenDrag.closeProgress(30f, 10f, 200f), 30f, 35f))
        assertTrue(PillOpenDrag.closeCommits(PillOpenDrag.closeProgress(36f, 10f, 200f), 36f, 35f))
        assertTrue(PillOpenDrag.closeCommits(PillOpenDrag.closeProgress(100f, 10f, 200f), 100f, 1000f))
        assertFalse(PillOpenDrag.closeCommits(1f, 0f, 35f))
    }
}

class PillPresenceTest {
    private fun t(
        phase: PillPhase = PillPhase.None,
        eligible: Boolean = true,
        expanded: Boolean = false,
        hasContent: Boolean = false,
        hadPill: Boolean = false,
        hadNoContent: Boolean = false,
        hasRecord: Boolean = true
    ) = pillTransition(phase, eligible, expanded, hasContent, hadPill, hadNoContent, hasRecord)

    @Test
    fun lastCardGoneStartsExit() =
        assertEquals(PillTransition.StartExit, t(hadPill = true))

    @Test
    fun cardArrivingOnIdleStartsEnter() =
        assertEquals(PillTransition.StartEnter, t(hasContent = true, hadNoContent = true))

    @Test
    fun noRecordMeansNoAnimation() {
        assertEquals(PillTransition.None, t(hadPill = true, hasRecord = false))
        assertEquals(PillTransition.None, t(hasContent = true, hadNoContent = true, hasRecord = false))
    }

    @Test
    fun newCardDuringExitReversesFromTheCurrentSpot() =
        assertEquals(PillTransition.ReverseToPill, t(phase = PillPhase.Leaving, hasContent = true))

    @Test
    fun cardGoneDuringTheReversalLeavesAgain() =
        assertEquals(PillTransition.ReverseToIdle, t(phase = PillPhase.Entering, hasContent = false))

    @Test
    fun expandingDuringExitReversesInsteadOfSnapping() =
        assertEquals(PillTransition.ReverseToPill, t(phase = PillPhase.Leaving, expanded = true, hasContent = true))

    @Test
    fun expandedNeverStartsANewExitOrEntry() {
        assertEquals(PillTransition.None, t(expanded = true, hadPill = true))
        assertEquals(PillTransition.None, t(expanded = true, hasContent = true, hadNoContent = true))
    }

    @Test
    fun leavingLandscapeCancels() {
        assertEquals(PillTransition.Cancel, t(phase = PillPhase.Leaving, eligible = false, hasContent = true))
        assertEquals(PillTransition.Cancel, t(phase = PillPhase.Entering, eligible = false))
    }

    @Test
    fun runningPhaseWithNoChangeKeepsGoing() {
        assertEquals(PillTransition.None, t(phase = PillPhase.Leaving, hasContent = false))
        assertEquals(PillTransition.None, t(phase = PillPhase.Entering, hasContent = true))
    }

    @Test
    fun settlingBehavesLikeNoPhaseSoANewCardStillEnters() {
        assertEquals(PillTransition.StartEnter, t(phase = PillPhase.Settling, hasContent = true, hadNoContent = true))
        assertEquals(PillTransition.None, t(phase = PillPhase.Settling))
    }

    @Test
    fun notEligibleNeverStarts() {
        assertEquals(PillTransition.None, t(eligible = false, hadPill = true))
        assertEquals(PillTransition.None, t(eligible = false, hasContent = true, hadNoContent = true))
    }

    @Test
    fun slideProgressIsTheSameCurveBothWays() {
        assertEquals(0f, pillSlideProgress(1f), 0.0001f)
        assertEquals(1f, pillSlideProgress(0f), 0.0001f)
        assertEquals(0.25f, pillSlideProgress(0.75f), 0.0001f)
        assertEquals(1f, pillSlideProgress(-2f), 0.0001f)
    }

    @Test
    fun springConvergence() {
        var last = 0f
        for (ms in 0..1500 step 10) {
            val p = springProgress(ms / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING)
            assertTrue("no overshoot at $ms ms", p <= 1f + 1e-4f)
            assertTrue("monotone at $ms ms", p >= last - 1e-4f)
            last = p
        }
        assertEquals(1f, last, 0.001f)
        assertEquals(0f, springProgress(0f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING), 0.0001f)

        val peak = (0..600 step 5).maxOf { springProgress(it / 1000f, 280f, 0.86f) }
        assertTrue(peak > 1f)
        assertEquals(1f, springProgress(3f, 280f, 0.86f), 0.001f)

        val curveMs = springTimeToProgressMs(PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING, 0.5f)
        assertTrue(springProgress(curveMs / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING) >= 0.5f)
        assertTrue(springProgress((curveMs - 1) / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING) < 0.5f)
        assertEquals(0L, springTimeToProgressMs(PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING, 0f))
    }

    @Test
    fun springStiffnessScaling() {
        val relaxed = pillSpringSettleMs(PILL_SPRING_STIFFNESS * 0.6f)
        val normal = pillSpringSettleMs(PILL_SPRING_STIFFNESS)
        val snappy = pillSpringSettleMs(PILL_SPRING_STIFFNESS * 1.7f)
        assertTrue(snappy < normal && normal < relaxed)
        assertTrue("normal settles in about 300 ms, was $normal", normal in 280L..350L)
    }
}

class RingerMorphTest {
    private val ring = DeviceStates.RINGER_RING
    private val vibrate = DeviceStates.RINGER_VIBRATE
    private val silent = DeviceStates.RINGER_SILENT

    @Test fun ringIsLoudestAndSilentQuietest() {
        assertEquals(2, RingerMorph.level(ring))
        assertEquals(1, RingerMorph.level(vibrate))
        assertEquals(0, RingerMorph.level(silent))
        assertNull(RingerMorph.level("wifi"))
        assertNull(RingerMorph.level(null))
    }

    @Test
    fun ringerTransitionDirection() {
        assertEquals(-1, RingerMorph.direction(ring, vibrate))
        assertEquals(-1, RingerMorph.direction(ring, silent))
        assertEquals(-1, RingerMorph.direction(vibrate, silent))
        assertEquals(1, RingerMorph.direction(silent, vibrate))
        assertEquals(1, RingerMorph.direction(silent, ring))
        assertEquals(1, RingerMorph.direction(vibrate, ring))

        assertEquals(1, RingerMorph.direction(DeviceStates.AIRPLANE_OFF, DeviceStates.AIRPLANE_ON))
        assertEquals(-1, RingerMorph.direction(DeviceStates.AIRPLANE_ON, DeviceStates.AIRPLANE_OFF))
        assertEquals(0, RingerMorph.direction(DeviceStates.AIRPLANE_ON, DeviceStates.AIRPLANE_ON))
        assertEquals(0, RingerMorph.direction(DeviceStates.RINGER_RING, DeviceStates.AIRPLANE_ON))
        assertEquals(0, RingerMorph.direction(DeviceStates.AIRPLANE_OFF, DeviceStates.RINGER_SILENT))

        assertEquals(0, RingerMorph.direction(ring, ring))
        assertEquals(0, RingerMorph.direction("wifi", "wifi_disconnected"))
        assertEquals(0, RingerMorph.direction(null, ring))
        assertEquals(0, RingerMorph.direction(ring, null))
    }
}

class SplitMotionTest {

    @Test
    fun splitPhase() {
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0f))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0.5f))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = 0.998f))
        assertEquals(SplitPhase.Open, SplitMotion.phase(wanted = true, progress = 0.999f))
        assertEquals(SplitPhase.Open, SplitMotion.phase(wanted = true, progress = 1f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = 1f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = 0.3f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT + 0.002f))
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT))
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = 0f))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = SplitMotion.TAIL / 2))
        assertEquals(SplitPhase.Opening, SplitMotion.phase(wanted = true, progress = SplitMotion.TAIL))

        // opening at 0.4, the island is no longer wanted: the same progress is now a close (no jump to 0 or 1)
        assertEquals(SplitPhase.Opening, SplitMotion.phase(true, 0.4f))
        assertEquals(SplitPhase.Closing, SplitMotion.phase(false, 0.4f))
    }


    @Test
    fun theCardSlotIsTheSwapsInEveryPhaseButOpen() {
        assertTrue(SplitMotion.swapping(SplitPhase.Opening))
        assertTrue(SplitMotion.swapping(SplitPhase.Closing))
        assertTrue(SplitMotion.swapping(SplitPhase.Closed))
        assertFalse(SplitMotion.swapping(SplitPhase.Open))
    }

    @Test
    fun reflowThreshold() {
        assertFalse(SplitMotion.reflowed(0f))
        assertFalse(SplitMotion.reflowed(0.29f))
        assertTrue(SplitMotion.reflowed(SplitMotion.REFLOW_AT))
        assertTrue(SplitMotion.reflowed(1f))
    }

    @Test
    fun reflowOpacity() {
        assertEquals(0f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(0f), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(1f), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT - SplitMotion.REFLOW_FADE_HALF), 0.0001f)
        assertEquals(1f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF), 0.0001f)
        assertEquals(0.5f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF / 2), 0.0001f)
        assertEquals(SplitMotion.reflowAlpha(0.42f), SplitMotion.reflowAlpha(0.58f), 0.0001f)

        // raw 0.5 is the dip, not geometry 0.5 (which would be raw 0.55)
        assertEquals(0f, SplitMotion.reflowAlpha(SplitMotion.REFLOW_AT), 0.0001f)
        assertTrue(SplitMotion.reflowAlpha(SplitMotion.fromGeometry(0.5f)) > 0f)
    }


    @Test
    fun splitShadow() {
        assertEquals(0f, SplitMotion.shadowStrength(0f), 0f)
        assertEquals(0f, SplitMotion.shadowStrength(0.5f), 0f)
        assertEquals(0f, SplitMotion.shadowStrength(SplitMotion.SHADOW_FROM), 0f)
        assertEquals(1f, SplitMotion.shadowStrength(1f), 0f)

        val a = SplitMotion.shadowStrength(0.93f)
        val b = SplitMotion.shadowStrength(0.97f)
        assertTrue(a in 0f..1f && b in 0f..1f && b > a)
    }

    // the slow spring tail is trimmed from the geometry

    @Test
    fun splitLayoutInterpolation() {
        assertEquals(0f, SplitMotion.geometry(0f), 0f)
        assertEquals(0f, SplitMotion.geometry(SplitMotion.TAIL / 2), 0f)
        assertEquals(0f, SplitMotion.geometry(SplitMotion.TAIL), 0f)
        assertEquals(1f, SplitMotion.geometry(1f), 0.0001f)
        assertEquals(0f, SplitMotion.geometry(-0.3f), 0f)
        assertEquals(1f, SplitMotion.geometry(1.4f), 0f)

        val mid = (SplitMotion.TAIL + 1f) / 2f
        assertEquals(0.5f, SplitMotion.geometry(mid), 0.0001f)
        assertEquals(0.25f, SplitMotion.geometry(SplitMotion.TAIL + 0.25f * (1f - SplitMotion.TAIL)), 0.0001f)
        assertTrue(SplitMotion.geometry(0.6f) > SplitMotion.geometry(0.5f))

        // no pop at the hand-off to the single surface: the islands are exactly the single island's size when the phase turns Closed
        assertEquals(SplitMotion.TAIL, SplitMotion.GONE_AT, 0f)
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT))
        assertEquals(0f, SplitMotion.geometry(SplitMotion.GONE_AT), 0f)
        assertEquals(SplitPhase.Closing, SplitMotion.phase(wanted = false, progress = SplitMotion.GONE_AT + 0.01f))
        assertTrue(SplitMotion.geometry(SplitMotion.GONE_AT + 0.01f) > 0f)

        for (i in 0..10) {
            val g = i / 10f
            assertEquals(g, SplitMotion.geometry(SplitMotion.fromGeometry(g)), 0.0001f)
        }
        assertEquals(SplitMotion.GONE_AT, SplitMotion.fromGeometry(0f), 0f)
        assertEquals(1f, SplitMotion.fromGeometry(1f), 0f)
        assertEquals(SplitPhase.Closed, SplitMotion.phase(wanted = false, progress = SplitMotion.fromGeometry(0f)))

        // they read the raw progress
        assertTrue(SplitMotion.REFLOW_AT > SplitMotion.TAIL + SplitMotion.REFLOW_FADE_HALF)
        // the re-wrap moved early (REFLOW_AT 0.3, geometry about 0.22) so it happens while the beside island is still a sliver
        assertTrue(SplitMotion.geometry(SplitMotion.REFLOW_AT) in 0.15f..0.7f)
        // the fade is over before the shadow starts, and the shadow starts after the tail
        assertTrue(SplitMotion.REFLOW_AT + SplitMotion.REFLOW_FADE_HALF < SplitMotion.SHADOW_FROM)
        assertTrue(SplitMotion.SHADOW_FROM > SplitMotion.TAIL)
    }



    @Test
    fun theTwoIslandLayoutIsHeldUntilTheShellIsReady() {
        // closing: held whatever the shell says
        assertTrue(SplitMotion.holdTwoIslandLayout(true, false, 0.5f, shellSingleReady = true))
        // progress gone but the shell is not the single island yet: still held (no flash on this frame)
        assertTrue(SplitMotion.holdTwoIslandLayout(true, false, 0f, shellSingleReady = false))
        // the shell confirmed: the content may flip
        assertFalse(SplitMotion.holdTwoIslandLayout(true, false, 0f, shellSingleReady = true))
        // never held when the island is wanted (i.e. the open layout), or when no split was ever shown
        assertFalse(SplitMotion.holdTwoIslandLayout(true, true, 0.5f, shellSingleReady = false))
        assertFalse(SplitMotion.holdTwoIslandLayout(false, false, 0f, shellSingleReady = false))
    }
}
