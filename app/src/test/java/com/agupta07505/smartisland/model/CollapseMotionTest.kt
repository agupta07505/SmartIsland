/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.AnimationSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapseMotionTest {
    @Test
    fun criticallyDampedSpringsDoNotOvershoot() {
        assertEquals(0f, CollapseMotion.overshootFraction(CollapseMotion.SETTLE_DAMPING), 0f)
        assertEquals(0f, CollapseMotion.overshootFraction(1.3f), 0f)
    }

    @Test
    fun widthBounceIsAboutFourPercent() {
        assertEquals(0.038f, CollapseMotion.overshootFraction(CollapseMotion.WIDTH_DAMPING), 0.003f)
    }

    @Test
    fun lessDampingMeansMoreOvershoot() {
        assertTrue(CollapseMotion.overshootFraction(0.6f) > CollapseMotion.overshootFraction(0.72f))
        assertTrue(CollapseMotion.overshootFraction(0.72f) > CollapseMotion.overshootFraction(0.86f))
    }

    @Test
    fun widthDipStaysSmallForTheBiggestCollapse() {
        // Card 400 dp wide collapsing to a 120 dp pill: 280 dp of travel, about 11 dp below the pill at most.
        val dip = CollapseMotion.widthDipDp(280f)
        assertEquals(10.7f, dip, 1f)
        assertTrue(dip < 15f)
        assertEquals(0f, CollapseMotion.widthDipDp(-5f), 0f)
    }

    @Test
    fun idleCirclePressCloseHasNoBounce() {
        assertEquals(0f, CollapseMotion.overshootFraction(CollapseMotion.closeWidthDamping(true)), 0f)
        assertTrue(CollapseMotion.overshootFraction(CollapseMotion.closeWidthDamping(false)) > 0f)
        assertEquals(CollapseMotion.WIDTH_DAMPING, CollapseMotion.closeWidthDamping(false), 0f)
    }

    @Test
    fun idleCirclePressCloseIsStepOneOfTheSequenceAndSettlesWithIt() {
        // The close's stiffness comes from the plan, so the shell has settled exactly when the pill may leave.
        val plan = IdleCloseSequence.plan(AnimationSpeed.NORMAL)
        val settle = springTimeToProgressMs(plan.closeStiffness, CollapseMotion.closeWidthDamping(true), 0.98f)
        assertEquals(plan.step1Ms.toDouble(), settle.toDouble(), 2.0)
        // The card's content is gone while the shell is still mostly open to the end (more than 80 % of the way closed).
        val travelled = springProgress(plan.contentOutMs / 1000f, plan.closeStiffness, CollapseMotion.closeWidthDamping(true))
        assertTrue("shell was $travelled closed when the content is gone", travelled > 0.8f)
    }

    @Test
    fun pillContentWaitsForTheWidthAndFinishesAboutWhenItSettles() {
        // Landscape base fade 280 ms: the shared collapse spring is 95 % home after about 170 ms.
        val delay = CollapseMotion.pillContentDelayMs(CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING)
        assertTrue("delay was $delay ms", delay in 155..190)
        val end = delay + CollapseMotion.pillContentInMs(280)
        assertTrue("end was $end ms", end in 300..380)
        // Portrait base fade 220 ms: finishes a little earlier, never later than the landscape one.
        assertTrue(delay + CollapseMotion.pillContentInMs(220) < end)
    }

    @Test
    fun pillContentDelayFollowsTheSpeedSetting() {
        val normal = CollapseMotion.pillContentDelayMs(CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING)
        val relaxed = CollapseMotion.pillContentDelayMs(CollapseMotion.STIFFNESS * 0.5f, CollapseMotion.WIDTH_DAMPING)
        val snappy = CollapseMotion.pillContentDelayMs(CollapseMotion.STIFFNESS * 2f, CollapseMotion.WIDTH_DAMPING)
        assertTrue(snappy < normal && normal < relaxed)
    }

    @Test
    fun cardContentFadesOutLongerThanBefore() {
        assertEquals(168, CollapseMotion.cardContentOutMs(280))
        assertEquals(132, CollapseMotion.cardContentOutMs(220))
        assertTrue(CollapseMotion.cardContentOutMs(280) > 280 / 2)
        assertEquals(1, CollapseMotion.cardContentOutMs(1))
    }

    // The pill content's wait follows how far open the island was

    @Test
    fun fromFullSizeTheWaitIsTheOldOne() {
        val stiffness = CollapseMotion.STIFFNESS
        val damping = CollapseMotion.WIDTH_DAMPING
        assertEquals(
            CollapseMotion.pillContentDelayMs(stiffness, damping),
            CollapseMotion.pillContentDelayFromMs(1f, stiffness, damping)
        )
    }

    @Test
    fun anIslandThatWasBarelyOpenDoesNotWait() {
        // Within 5 % of the full travel the width is already home.
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(0f, CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING))
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(0.05f, CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING))
    }

    @Test
    fun theLessOpenTheShorterTheWait() {
        val s = CollapseMotion.STIFFNESS
        val d = CollapseMotion.WIDTH_DAMPING
        val quarter = CollapseMotion.pillContentDelayFromMs(0.25f, s, d)
        val half = CollapseMotion.pillContentDelayFromMs(0.5f, s, d)
        val full = CollapseMotion.pillContentDelayFromMs(1f, s, d)
        assertTrue(quarter < half)
        assertTrue(half < full)
    }

    @Test
    fun theWaitIsClampedForOutOfRangeProgress() {
        val s = CollapseMotion.STIFFNESS
        val d = CollapseMotion.WIDTH_DAMPING
        assertEquals(CollapseMotion.pillContentDelayFromMs(1f, s, d), CollapseMotion.pillContentDelayFromMs(1.2f, s, d))
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(-0.3f, s, d))
    }
}
