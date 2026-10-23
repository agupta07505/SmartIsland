/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The island's own stopwatch (no Android types, so unit-testable). [startMs] is the wall-clock start, already moved
 * forward by earlier pauses, so running = `now - startMs`; paused keeps [frozenMs].
 */
data class StopwatchState(
    val active: Boolean = false,
    val running: Boolean = false,
    val startMs: Long = 0L,
    val frozenMs: Long = 0L,
    val laps: Int = 0
) {
    fun elapsedMs(now: Long): Long = when {
        !active -> 0L
        running -> (now - startMs).coerceAtLeast(0L)
        else -> frozenMs
    }

    fun start(now: Long): StopwatchState = StopwatchState(active = true, running = true, startMs = now)

    fun pause(now: Long): StopwatchState =
        if (active && running) copy(running = false, frozenMs = elapsedMs(now)) else this

    fun resume(now: Long): StopwatchState =
        if (active && !running) copy(running = true, startMs = now - frozenMs, frozenMs = 0L) else this

    fun lap(): StopwatchState = if (active && running) copy(laps = laps + 1) else this

    fun reset(): StopwatchState = StopwatchState()

    /** Tap on the Stopwatch tile: start it, pause it while running, resume it while paused. */
    fun toggle(now: Long): StopwatchState = when {
        !active -> start(now)
        running -> pause(now)
        else -> resume(now)
    }

    fun encode(): String = listOf(active, running, startMs, frozenMs, laps).joinToString(",")

    companion object {
        fun decode(raw: String?): StopwatchState {
            val f = raw?.split(',') ?: return StopwatchState()
            if (f.size < 5) return StopwatchState()
            val active = f[0].toBooleanStrictOrNull() ?: return StopwatchState()
            val running = f[1].toBooleanStrictOrNull() ?: return StopwatchState()
            val start = f[2].toLongOrNull() ?: return StopwatchState()
            val frozen = f[3].toLongOrNull() ?: return StopwatchState()
            val laps = f[4].toIntOrNull() ?: return StopwatchState()
            if (!active) return StopwatchState()
            return StopwatchState(true, running, start, frozen, laps.coerceAtLeast(0))
        }
    }
}
