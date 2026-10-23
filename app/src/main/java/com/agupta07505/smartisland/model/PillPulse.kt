/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Arrival pulse of the collapsed cutout: at most one per [MIN_GAP_MS], so a burst of cards pulses once. */
object PillPulse {
    const val MIN_GAP_MS = 2_000L

    fun allowed(nowMs: Long, lastPulseMs: Long): Boolean = nowMs - lastPulseMs >= MIN_GAP_MS
}
