/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.AnimationSpeed
import kotlin.math.exp
import kotlin.math.max

/** Base length (ms, before the speed setting) of the secondary cutout returning from its merge. */
const val SECONDARY_RETURN_BASE_MS = 460

/**
 * The idle-circle tap as a two-step close exactly as long as the opening it mirrors. Same numbers in portrait and
 * landscape, each with its own animation speed.
 */
object IdleCloseSequence {
    /**
     * How many times longer step 1 (expanded close + secondary merge) is than step 2 (the pill leaving, as long as
     * its arrival). Above 1 the island closes more slowly than the pill slides out.
     */
    const val STEP1_OVER_STEP2 = 1.5f

    /** No step is shorter than this, so a spring has something to run on. */
    const val MIN_STEP_MS = 60

    /** The card's content is gone after this share of step 1 (the shell is still closing under it). */
    const val CONTENT_OUT_SHARE_OF_STEP1 = 0.7f

    /** A spring counts as arrived at this share of its travel (same bar as [PILL_SPRING_SETTLED]). */
    const val SETTLED = PILL_SPRING_SETTLED

    /** What the close plays: lengths in ms and the stiffness of the two critically damped springs (user speed applied). */
    data class Plan(
        val totalMs: Int,
        val step1Ms: Int,
        val step2Ms: Int,
        /** Stiffness of the expanded island's close (width, height, radius, progress); critically damped. */
        val closeStiffness: Float,
        /** Stiffness of the pill's exit spring; critically damped. */
        val exitStiffness: Float,
        val contentOutMs: Int
    )

    /** Real length (ms, user speed) of the pill's slide-in spring; its slide-out gets the same. */
    fun pillArrivalMs(speed: AnimationSpeed): Int =
        pillSpringSettleMs(speed.userStiffness(PILL_SPRING_STIFFNESS)).toInt()

    /**
     * Real length (ms, user speed) of the idle-circle tap's opening: the pill's arrival spring. The secondary
     * cutout's return runs alongside, so the longer wins.
     */
    fun openingMs(speed: AnimationSpeed): Int =
        max(pillArrivalMs(speed), speed.userDurationMs(SECONDARY_RETURN_BASE_MS))

    /** The close for [speed]: step 2 as long as the pill's arrival, step 1 [STEP1_OVER_STEP2] times that. */
    fun plan(speed: AnimationSpeed): Plan {
        val step2 = pillArrivalMs(speed).coerceAtLeast(MIN_STEP_MS)
        val step1 = (step2 * STEP1_OVER_STEP2).toInt().coerceAtLeast(MIN_STEP_MS)
        return Plan(
            totalMs = step1 + step2,
            step1Ms = step1,
            step2Ms = step2,
            closeStiffness = criticalStiffnessForSettleMs(step1),
            exitStiffness = criticalStiffnessForSettleMs(step2),
            contentOutMs = (step1 * CONTENT_OUT_SHARE_OF_STEP1).toInt().coerceAtLeast(1)
        )
    }

    /**
     * Stiffness of a critically damped spring (mass 1) that is [SETTLED] of the way home after [ms] ms. Closed form
     * x(t) = 1 - (1 + wt) e^(-wt), so the time is a fixed multiple of 1 / sqrt(stiffness).
     */
    fun criticalStiffnessForSettleMs(ms: Int): Float {
        val x = criticalSettleX(SETTLED)
        val w = x / (ms.coerceAtLeast(1) / 1000.0)
        return (w * w).toFloat()
    }

    private fun criticalSettleX(fraction: Float): Double {
        val target = 1.0 - fraction.toDouble().coerceIn(0.0, 0.999)
        var lo = 0.0
        var hi = 30.0
        repeat(60) {
            val mid = (lo + hi) / 2.0
            // (1 + x) e^-x is the share of travel still to go; it falls from 1 to 0.
            if ((1.0 + mid) * exp(-mid) > target) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }
}
