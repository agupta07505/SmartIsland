/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class TorchPattern { Sos, Strobe }

data class TorchStep(val on: Boolean, val ms: Long)

object TorchTiming {
    private const val UNIT_MS = 200L
    private const val SOS_REST_MS = 2_000L
    const val MIN_STROBE_HZ = 1
    const val MAX_STROBE_HZ = 30
    const val DEFAULT_STROBE_HZ = 3
    private const val NANOS_PER_SECOND = 1_000_000_000L

    /** ... --- ... in Morse: dot = 1 unit, dash = 3, gap inside a letter = 1, between letters = 3, then a rest. */
    internal fun sosSteps(): List<TorchStep> {
        val dot = TorchStep(true, UNIT_MS)
        val dash = TorchStep(true, UNIT_MS * 3)
        val gap = TorchStep(false, UNIT_MS)
        val letterGap = TorchStep(false, UNIT_MS * 3)
        val steps = ArrayList<TorchStep>()
        fun letter(symbol: TorchStep) {
            repeat(3) { i ->
                steps += symbol
                if (i < 2) steps += gap
            }
        }
        letter(dot); steps += letterGap
        letter(dash); steps += letterGap
        letter(dot)
        steps += TorchStep(false, SOS_REST_MS)
        return steps
    }

    internal fun beatNanos(hz: Int): Long = NANOS_PER_SECOND / (2 * hz.coerceIn(MIN_STROBE_HZ, MAX_STROBE_HZ))
}

object TorchPatternClock {
    private val sosEnds: List<Long> by lazy {
        var total = 0L
        TorchTiming.sosSteps().map { total += it.ms; total }
    }
    private val sosOn: List<Boolean> by lazy { TorchTiming.sosSteps().map { it.on } }

    fun lit(pattern: TorchPattern, hz: Int, elapsedMs: Long): Boolean = when (pattern) {
        TorchPattern.Strobe -> (elapsedMs * hz.coerceIn(TorchCardState.MIN_HZ, TorchCardState.MAX_HZ) * 2 / 1000) % 2 == 0L
        TorchPattern.Sos -> {
            val at = elapsedMs.coerceAtLeast(0L) % sosEnds.last()
            sosOn[sosEnds.indexOfFirst { at < it }]
        }
    }
}
