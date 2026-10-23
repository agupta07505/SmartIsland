/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

/** Collapse motion shared by portrait and landscape, for a collapse that lands on a visible pill. */
object CollapseMotion {
    const val STIFFNESS = 300f

    /** Width spring: about 4 % overshoot. */
    const val WIDTH_DAMPING = 0.72f

    /** Height, corner radius and vertical progress: no overshoot. */
    const val SETTLE_DAMPING = 1f

    /** Idle-circle close: no bounce; stiffness is [IdleCloseSequence.Plan.closeStiffness]. */
    const val IDLE_PRESS_DAMPING = 1f

    fun closeWidthDamping(idlePress: Boolean): Float = if (idlePress) IDLE_PRESS_DAMPING else WIDTH_DAMPING

    /** Overshoot as a fraction of travel (0 when not underdamped). */
    fun overshootFraction(damping: Float): Float {
        if (damping >= 1f) return 0f
        val z = damping.coerceAtLeast(0.01f)
        return exp(-PI.toFloat() * z / sqrt(1f - z * z))
    }

    fun widthDipDp(travelDp: Float): Float = travelDp.coerceAtLeast(0f) * overshootFraction(WIDTH_DAMPING)

    /** Pill content fades back in once the width spring has covered this share of its travel. */
    const val PILL_CONTENT_HOLD_PROGRESS = 0.95f

    /** Pill content fade-in length, as a share of the base fade. */
    const val PILL_CONTENT_IN_SHARE = 0.6f

    /** Card content fade-out length, as a share of the base fade. */
    const val CARD_CONTENT_OUT_SHARE = 0.6f

    /**
     * Delay before the pill content fades in: time the speed-scaled width spring needs to reach
     * [PILL_CONTENT_HOLD_PROGRESS].
     */
    fun pillContentDelayMs(stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, PILL_CONTENT_HOLD_PROGRESS).toInt()

    /** Same delay when the collapse starts at [expandProgress] (0 = pill, 1 = card), e.g. a tap on a card just opened. */
    fun pillContentDelayFromMs(expandProgress: Float, stiffness: Float, damping: Float): Int {
        val remaining = 1f - PILL_CONTENT_HOLD_PROGRESS
        val start = expandProgress.coerceIn(0f, 1f)
        if (start <= remaining) return 0
        return springTimeToProgressMs(stiffness, damping, 1f - remaining / start).toInt()
    }

    fun pillContentInMs(baseMs: Int): Int = (baseMs * PILL_CONTENT_IN_SHARE).toInt().coerceAtLeast(1)

    /** The idle-circle close takes its fade from [IdleCloseSequence.Plan.contentOutMs] instead. */
    fun cardContentOutMs(baseMs: Int): Int = (baseMs * CARD_CONTENT_OUT_SHARE).toInt().coerceAtLeast(1)
}
