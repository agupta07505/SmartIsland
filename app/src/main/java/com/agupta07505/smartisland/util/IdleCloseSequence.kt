/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.PILL_SPRING_SETTLED
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.pillSpringSettleMs
import com.agupta07505.smartisland.model.springTimeToProgressMs
import kotlin.math.exp
import kotlin.math.max

const val SECONDARY_RETURN_BASE_MS = 460

object IdleCloseSequence {
    const val STEP1_OVER_STEP2 = 1.5f

    /** No step is shorter than this, so a spring has something to run on. */
    const val MIN_STEP_MS = 60

    const val CONTENT_OUT_SHARE_OF_STEP1 = 0.7f

    const val SETTLED = PILL_SPRING_SETTLED

    data class Plan(
        val totalMs: Int,
        val step1Ms: Int,
        val step2Ms: Int,
        /** Stiffness of the expanded island's close (width, height, radius, progress). */
        val closeStiffness: Float,
        /** Stiffness of the pill's exit spring; critically damped. */
        val exitStiffness: Float,
        val contentOutMs: Int
    )

    fun pillArrivalMs(speed: AnimationSpeed): Int =
        pillSpringSettleMs(speed.userStiffness(PILL_SPRING_STIFFNESS)).toInt()

    /** Real length (ms, user speed) of the idle-circle tap's opening. */
    fun openingMs(speed: AnimationSpeed): Int =
        max(pillArrivalMs(speed), speed.userDurationMs(SECONDARY_RETURN_BASE_MS))

    /** Close for [speed]: step 2 as long as the pill's arrival. */
    fun plan(speed: AnimationSpeed, mirrorSpring: Pair<Float, Float>? = null): Plan {
        val step2 = pillArrivalMs(speed).coerceAtLeast(MIN_STEP_MS)
        val step1 = if (mirrorSpring != null) {
            springTimeToProgressMs(mirrorSpring.first, mirrorSpring.second, SETTLED).toInt().coerceAtLeast(MIN_STEP_MS)
        } else {
            (step2 * STEP1_OVER_STEP2).toInt().coerceAtLeast(MIN_STEP_MS)
        }
        return Plan(
            totalMs = step1 + step2,
            step1Ms = step1,
            step2Ms = step2,
            closeStiffness = criticalStiffnessForSettleMs(step1),
            exitStiffness = criticalStiffnessForSettleMs(step2),
            contentOutMs = (step1 * CONTENT_OUT_SHARE_OF_STEP1).toInt().coerceAtLeast(1)
        )
    }

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
            // (1 + x) e^-x is the share of travel still to go; it falls from 1 to 0
            if ((1.0 + mid) * exp(-mid) > target) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }
}
