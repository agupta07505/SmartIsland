/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape split (card island plus picker or picture island): the shell's size as a direct function of the ONE split
 * progress.
 */
object SplitShell {
    /** Shell width while the split is drawn. */
    fun width(single: Float, total: Float, progress: Float): Float = single + (total - single) * SplitMotion.geometry(progress)

    /**
     * Shell height while the split is drawn: the single island's height at progress 0, the taller of the two at 1,
     * following the same [SplitMotion.geometry] as the width.
     */
    fun height(single: Float, total: Float, progress: Float): Float = single + (total - single) * SplitMotion.geometry(progress)

    fun handoverDone(own: Float, single: Float): Boolean = kotlin.math.abs(own - single) <= HANDOVER_TOLERANCE_DP

    /** The override is let go after this long even if the shell's width never got there (a safety, not a timing). */
    const val HANDOVER_MAX_MS = 600L

    const val HANDOVER_TOLERANCE_DP = 0.5f
}
