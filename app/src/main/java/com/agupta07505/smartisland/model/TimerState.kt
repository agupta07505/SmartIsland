/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The island's own countdown timer (no Android types, so unit-testable). Running ends at [endMs] (wall-clock); paused
 * keeps [remainingMs]; [finished] shows "Time's up" until stopped.
 */
data class TimerState(
    val active: Boolean = false,
    val running: Boolean = false,
    val endMs: Long = 0L,
    val remainingMs: Long = 0L,
    val finished: Boolean = false
) {
    fun remainingAt(now: Long): Long = when {
        !active || finished -> 0L
        running -> (endMs - now).coerceAtLeast(0L)
        else -> remainingMs
    }

    fun isDue(now: Long): Boolean = active && running && !finished && now >= endMs

    fun start(now: Long, lengthMs: Long): TimerState {
        val length = lengthMs.coerceIn(MIN_MS, MAX_MS)
        return TimerState(active = true, running = true, endMs = now + length)
    }

    fun pause(now: Long): TimerState =
        if (active && running && !finished) copy(running = false, remainingMs = remainingAt(now), endMs = 0L) else this

    fun resume(now: Long): TimerState =
        if (active && !running && !finished) copy(running = true, endMs = now + remainingMs, remainingMs = 0L) else this

    fun toggle(now: Long): TimerState = if (running) pause(now) else resume(now)

    /** Adds a minute to a running or paused timer, up to the longest length; a finished timer is left alone. */
    fun addMinute(now: Long): TimerState = when {
        !active || finished -> this
        running -> copy(endMs = minOf(endMs + MINUTE_MS, now + MAX_MS))
        else -> copy(remainingMs = minOf(remainingMs + MINUTE_MS, MAX_MS))
    }

    fun finish(): TimerState = if (active && !finished) copy(running = false, endMs = 0L, remainingMs = 0L, finished = true) else this

    fun reset(): TimerState = TimerState()

    fun encode(): String = listOf(active, running, endMs, remainingMs, finished).joinToString(",")

    companion object {
        const val MINUTE_MS = 60_000L
        const val MIN_MS = 1_000L
        const val MAX_MS = 24 * 60 * MINUTE_MS

        fun decode(raw: String?): TimerState {
            val f = raw?.split(',') ?: return TimerState()
            if (f.size < 5) return TimerState()
            val active = f[0].toBooleanStrictOrNull() ?: return TimerState()
            val running = f[1].toBooleanStrictOrNull() ?: return TimerState()
            val end = f[2].toLongOrNull() ?: return TimerState()
            val left = f[3].toLongOrNull() ?: return TimerState()
            val finished = f[4].toBooleanStrictOrNull() ?: return TimerState()
            if (!active) return TimerState()
            return TimerState(true, running, end.coerceAtLeast(0L), left.coerceIn(0L, MAX_MS), finished)
        }
    }
}
