/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun criticallyDampedSpringNeverOvershootsAndSettles() {
        var last = 0f
        for (ms in 0..1500 step 10) {
            val p = springProgress(ms / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING)
            assertTrue("no overshoot at $ms ms", p <= 1f + 1e-4f)
            assertTrue("monotone at $ms ms", p >= last - 1e-4f)
            last = p
        }
        assertEquals(1f, last, 0.001f)
        assertEquals(0f, springProgress(0f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING), 0.0001f)
    }

    @Test
    fun underDampedSpringOvershootsAndStillSettles() {
        val peak = (0..600 step 5).maxOf { springProgress(it / 1000f, 280f, 0.86f) }
        assertTrue(peak > 1f)
        assertEquals(1f, springProgress(3f, 280f, 0.86f), 0.001f)
    }

    @Test
    fun timeToProgressIsFoundOnTheSpringCurve() {
        val ms = springTimeToProgressMs(PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING, 0.5f)
        assertTrue(springProgress(ms / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING) >= 0.5f)
        assertTrue(springProgress((ms - 1) / 1000f, PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING) < 0.5f)
        assertEquals(0L, springTimeToProgressMs(PILL_SPRING_STIFFNESS, PILL_SPRING_DAMPING, 0f))
    }

    @Test
    fun stiffnessScalingSpeedsTheSpringUpWithoutTouchingMilliseconds() {
        val relaxed = pillSpringSettleMs(PILL_SPRING_STIFFNESS * 0.6f)
        val normal = pillSpringSettleMs(PILL_SPRING_STIFFNESS)
        val snappy = pillSpringSettleMs(PILL_SPRING_STIFFNESS * 1.7f)
        assertTrue(snappy < normal && normal < relaxed)
        assertTrue("normal settles in about 300 ms, was $normal", normal in 280L..350L)
    }
}
