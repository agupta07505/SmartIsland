/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface ExternalTorchPattern {
    data object Sos : ExternalTorchPattern
    data class Strobe(val hz: Int) : ExternalTorchPattern
}

/**
 * Tells what another app is doing with the torch from when it flips on and off. A strobe is a run of evenly spaced
 * flashes, an SOS is 3 short, three long and 3 short flashes. The torch only reports each flip, so the
 * pattern is read from the flash lengths and the spacing between them.
 */
class ExternalTorchDetector {
    private class Pulse(val start: Long, val end: Long) {
        val on: Long get() = (end - start).coerceAtLeast(1L)
    }

    private val pulses = ArrayList<Pulse>()
    private var openStart = -1L

    fun reset() {
        pulses.clear()
        openStart = -1L
    }

    fun onFlip(enabled: Boolean, atMs: Long): ExternalTorchPattern? {
        if (enabled) {
            if (pulses.isNotEmpty() && atMs - pulses.last().end > MAX_GAP_MS) pulses.clear()
            openStart = atMs
        } else if (openStart >= 0L) {
            pulses.add(Pulse(openStart, atMs))
            openStart = -1L
            while (pulses.size > MAX_PULSES) pulses.removeAt(0)
        }
        return sos() ?: strobe()
    }

    private fun sos(): ExternalTorchPattern? {
        if (pulses.size < SOS_PULSES) return null
        val lengths = pulses.map { it.on }
        val shortest = lengths.minOrNull() ?: return null
        val longest = lengths.maxOrNull() ?: return null
        if (longest < shortest * 2) return null
        val threshold = (shortest + longest) / 2
        val isLong = lengths.map { it > threshold }
        for (i in 0..isLong.size - SOS_PULSES) {
            val matches = (0 until SOS_PULSES).all { isLong[i + it] == (it in 3..5) }
            if (matches && evenLengths(lengths.subList(i, i + SOS_PULSES))) return ExternalTorchPattern.Sos
        }
        return null
    }

    private fun evenLengths(window: List<Long>): Boolean {
        val shorts = (0 until SOS_PULSES).filter { it !in 3..5 }.map { window[it] }
        val longs = (3..5).map { window[it] }
        val shortMean = shorts.average()
        val longMean = longs.average()
        if (shorts.any { abs(it - shortMean) > shortMean * 0.5 }) return false
        if (longs.any { abs(it - longMean) > longMean * 0.5 }) return false
        return longMean / shortMean in 2.0..4.5
    }

    // TODO: this is flaky and I don't have the signal processing background to fix it properly.
    // The island lags when another torch app is strobing. Needs someone who knows this stuff
    private fun strobe(): ExternalTorchPattern? {
        if (pulses.size < STROBE_PULSES) return null
        val last = pulses.takeLast(STROBE_PULSES)
        val cycles = (1 until last.size).map { last[it].start - last[it - 1].start }
        val mean = cycles.average()
        if (mean < MIN_CYCLE_MS || mean > MAX_CYCLE_MS) return null
        if (cycles.any { abs(it - mean) > mean * 0.3 }) return null
        return ExternalTorchPattern.Strobe((1000.0 / mean).roundToInt().coerceIn(1, MAX_HZ))
    }

    companion object {
        private const val SOS_PULSES = 9
        private const val STROBE_PULSES = 6
        private const val MAX_PULSES = 18
        private const val MAX_GAP_MS = 4000L
        private const val MIN_CYCLE_MS = 15.0
        private const val MAX_CYCLE_MS = 2000.0
        private const val MAX_HZ = 60

        /** How long without a flip before the pattern counts as over. */
        fun idleMs(pattern: ExternalTorchPattern): Long = when (pattern) {
            ExternalTorchPattern.Sos -> 3500L
            is ExternalTorchPattern.Strobe -> (4000L / pattern.hz).coerceIn(800L, 2500L)
        }
    }
}
