/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Timer arithmetic matching the shade's truncating chronometer. */
object TimerClock {
    private const val MAX_PLAUSIBLE_MS = 100L * 3600L * 1000L

    fun secondsLeft(remainingMs: Long): Long = remainingMs.coerceAtLeast(0L) / 1000L

    fun displaySeconds(remainingMs: Long, exact: Boolean, legacyBiasMs: Long): Long =
        if (exact) secondsLeft(remainingMs) else ((remainingMs + legacyBiasMs) / 1000L).coerceAtLeast(0L)

    fun msToNextTick(remainingMs: Long): Long = (remainingMs.coerceAtLeast(0L) % 1000L) + 5L

    fun targetFromShownSeconds(nowMs: Long, shownSec: Long): Long = nowMs + shownSec * 1000L + 999L

    /** Remaining time a paused countdown chronometer had when posted; null if the printed text disagrees. */
    fun frozenFromChronometer(baseElapsedMs: Long, postElapsedMs: Long, shownSec: Long?, applyDelayMs: Long): Long? {
        val corrected = baseElapsedMs - postElapsedMs
        if (corrected <= 0L || corrected > MAX_PLAUSIBLE_MS) return null
        if (shownSec != null) {
            val delay = applyDelayMs.coerceAtLeast(0L)
            val lo = shownSec * 1000L + delay - 1000L
            val hi = shownSec * 1000L + delay + 1999L
            if (corrected !in lo..hi) return null
        }
        return corrected
    }

    /** Elapsed time a paused stopwatch had when posted; null if the printed text disagrees. */
    fun elapsedFromChronometer(baseElapsedMs: Long, postElapsedMs: Long, shownSec: Long?, applyDelayMs: Long): Long? {
        val corrected = postElapsedMs - baseElapsedMs
        if (corrected < 0L || corrected > MAX_PLAUSIBLE_MS) return null
        if (shownSec != null) {
            val delay = applyDelayMs.coerceAtLeast(0L)
            val lo = shownSec * 1000L - delay - 1000L
            val hi = shownSec * 1000L - delay + 1999L
            if (corrected !in lo..hi) return null
        }
        return corrected
    }

    fun msToNextTickUp(elapsedMs: Long): Long = 1000L - (elapsedMs.coerceAtLeast(0L) % 1000L) + 5L
}
