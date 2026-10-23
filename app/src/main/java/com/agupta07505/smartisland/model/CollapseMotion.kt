/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

object CollapseMotion {
    const val STIFFNESS = 300f

    const val WIDTH_DAMPING = 0.72f

    const val SETTLE_DAMPING = 1f

    const val IDLE_PRESS_DAMPING = 1f

    fun closeWidthDamping(idlePress: Boolean, noBounce: Boolean = false): Float =
        if (idlePress || noBounce) IDLE_PRESS_DAMPING else WIDTH_DAMPING

    fun overshootFraction(damping: Float): Float {
        if (damping >= 1f) return 0f
        val z = damping.coerceAtLeast(0.01f)
        return exp(-PI.toFloat() * z / sqrt(1f - z * z))
    }

    /**
     * How far the width has travelled (0..1, a little over 1 while it overshoots) when the height, which settles
     * without overshoot, has travelled [heightTravel]. Both are springs of the same stiffness, so the width's travel
     * is a fixed function of the height's: the swap runs 1 clock and draws both axes from it. [widthDamping] at 1
     * or more is the same spring as the height's (no stagger, no overshoot).
     */
    fun widthTravel(heightTravel: Float, widthDamping: Float = WIDTH_DAMPING): Float {
        val h = heightTravel.coerceIn(0f, 1f)
        if (widthDamping >= 1f) return h
        val x = h * WIDTH_TABLE_STEPS
        val i = x.toInt().coerceAtMost(WIDTH_TABLE_STEPS - 1)
        val f = x - i
        return widthTable[i] + (widthTable[i + 1] - widthTable[i]) * f
    }

    private const val WIDTH_TABLE_STEPS = 256

    private val widthTable: FloatArray by lazy {
        val z = WIDTH_DAMPING.toDouble()
        val wd = sqrt(1.0 - z * z)
        FloatArray(WIDTH_TABLE_STEPS + 1) { i ->
            if (i == 0) 0f
            else if (i == WIDTH_TABLE_STEPS) 1f
            else {
                val h = i.toDouble() / WIDTH_TABLE_STEPS
                var lo = 0.0
                var hi = 60.0
                repeat(60) {
                    val mid = (lo + hi) / 2
                    if (1.0 - (1.0 + mid) * exp(-mid) < h) lo = mid else hi = mid
                }
                val t = (lo + hi) / 2
                (1.0 - exp(-z * t) * (cos(wd * t) + z / wd * sin(wd * t))).toFloat()
            }
        }
    }

    const val PILL_CONTENT_HOLD_PROGRESS = 0.95f

    const val PILL_CONTENT_IN_SHARE = 0.6f

    const val CARD_CONTENT_OUT_SHARE = 0.6f

    fun pillContentDelayFromMs(expandProgress: Float, stiffness: Float, damping: Float): Int {
        val remaining = 1f - PILL_CONTENT_HOLD_PROGRESS
        val start = expandProgress.coerceIn(0f, 1f)
        if (start <= remaining) return 0
        return springTimeToProgressMs(stiffness, damping, 1f - remaining / start).toInt()
    }

    fun pillContentInMs(baseMs: Int): Int = (baseMs * PILL_CONTENT_IN_SHARE).toInt().coerceAtLeast(1)

    fun cardContentOutMs(baseMs: Int): Int = (baseMs * CARD_CONTENT_OUT_SHARE).toInt().coerceAtLeast(1)
}
