/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 1 state machine for the collapsed cutout leaving (last card gone, idle circle takes over) and returning (a card
 * arrives)
 */
enum class PillPhase {
    None,
    Leaving,
    Entering,

    Settling
}

enum class PillTransition {
    None,

    StartExit,

    StartEnter,

    ReverseToPill,

    ReverseToIdle,

    Cancel
}

const val PILL_SPRING_DAMPING = 1f
const val PILL_SPRING_STIFFNESS = 340f

const val PILL_SPRING_SETTLED = 0.98f

fun springProgress(tSec: Float, stiffness: Float, dampingRatio: Float): Float {
    if (tSec <= 0f || stiffness <= 0f) return 0f
    val w = sqrt(stiffness.toDouble())
    val z = dampingRatio.toDouble().coerceIn(0.05, 1.0)
    val t = tSec.toDouble()
    val x = if (z >= 0.999) {
        1.0 - (1.0 + w * t) * exp(-w * t)
    } else {
        val wd = w * sqrt(1.0 - z * z)
        1.0 - exp(-z * w * t) * (cos(wd * t) + (z * w / wd) * sin(wd * t))
    }
    return x.toFloat()
}

fun springTimeToProgressMs(stiffness: Float, dampingRatio: Float, fraction: Float): Long {
    val target = fraction.coerceIn(0f, 0.995f)
    if (target <= 0f) return 0L
    var ms = 0L
    while (ms < 3000L) {
        if (springProgress(ms / 1000f, stiffness, dampingRatio) >= target) return ms
        ms++
    }
    return ms
}

fun pillSpringSettleMs(stiffness: Float): Long =
    springTimeToProgressMs(stiffness, PILL_SPRING_DAMPING, PILL_SPRING_SETTLED)

fun pillTransition(
    phase: PillPhase,
    eligible: Boolean,
    expanded: Boolean,
    hasContent: Boolean,
    hadPill: Boolean,
    hadNoContent: Boolean,
    hasRecord: Boolean
): PillTransition = when {
    phase != PillPhase.None && !eligible -> PillTransition.Cancel
    phase == PillPhase.Leaving && hasContent -> PillTransition.ReverseToPill
    phase == PillPhase.Entering && !hasContent -> PillTransition.ReverseToIdle
    phase == PillPhase.Leaving || phase == PillPhase.Entering -> PillTransition.None
    !eligible || expanded || !hasRecord -> PillTransition.None
    !hasContent && hadPill -> PillTransition.StartExit
    hasContent && hadNoContent -> PillTransition.StartEnter
    else -> PillTransition.None
}

fun pillSlideProgress(presence: Float): Float = 1f - presence.coerceIn(0f, 1f)
