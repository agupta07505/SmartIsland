/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

enum class SplitPhase { Closed, Opening, Open, Closing }

object SplitMotion {
    const val OPEN_AT = 0.999f

    const val TAIL = 0.1f

    /**
     * Counts as gone at or below this share, where [geometry] reaches 0, so the hand-off to the single surface has no
     * pop
     */
    const val GONE_AT = TAIL

    /**
     * Progress the islands' sizes are drawn from: 0 up to [TAIL], then straight to 1. Reflow, shadow and phase read the
     * raw progress
     */
    fun geometry(progress: Float): Float = ((progress - TAIL) / (1f - TAIL)).coerceIn(0f, 1f)

    fun fromGeometry(g: Float): Float = TAIL + g.coerceIn(0f, 1f) * (1f - TAIL)

    fun phase(wanted: Boolean, progress: Float): SplitPhase = when {
        wanted -> if (progress >= OPEN_AT) SplitPhase.Open else SplitPhase.Opening
        progress > GONE_AT -> SplitPhase.Closing
        else -> SplitPhase.Closed
    }

    const val HANDOVER_CAP_MS = 120L

    /**
     * Content keeps the two-island layout while closing, until the shell reports its single surface is back
     * ([shellSingleReady])
     */
    fun holdTwoIslandLayout(wasSplit: Boolean, wanted: Boolean, progress: Float, shellSingleReady: Boolean): Boolean =
        wasSplit && !wanted && (phase(wanted, progress) == SplitPhase.Closing || !shellSingleReady)

    fun swapping(phase: SplitPhase): Boolean = phase != SplitPhase.Open

    const val REFLOW_AT = 0.3f

    const val REFLOW_FADE_HALF = 0.06f

    fun reflowed(progress: Float): Boolean = progress >= REFLOW_AT

    fun reflowAlpha(progress: Float): Float =
        (abs(progress - REFLOW_AT) / REFLOW_FADE_HALF).coerceIn(0f, 1f)

    /**
     * Shadows are the costliest draw while the split moves, so they come in over the last part of an opening and go out
     * over the first part of a closing
     */
    const val SHADOW_FROM = 0.9f

    fun shadowStrength(progress: Float): Float = ((progress - SHADOW_FROM) / (1f - SHADOW_FROM)).coerceIn(0f, 1f)
}

/**
 * Landscape split (card island plus picker or picture island): the shell's size as a direct function of the single
 * split progress
 */
object SplitShell {
    fun width(single: Float, total: Float, progress: Float): Float =
            single + (total - single) * SplitMotion.geometry(progress)

    fun height(single: Float, total: Float, progress: Float): Float =
            single + (total - single) * SplitMotion.geometry(progress)

    fun handoverDone(own: Float, single: Float): Boolean = abs(own - single) <= HANDOVER_TOLERANCE_DP

    const val HANDOVER_MAX_MS = 600L

    const val HANDOVER_TOLERANCE_DP = 0.5f
}
