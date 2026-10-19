/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Island's own countdown timer. Running ends at [endMs] (wall-clock), paused keeps [remainingMs], [finished] shows
 * "Time's up" until stopped
 */
data class TimerState(
    val active: Boolean = false,
    val running: Boolean = false,
    val endMs: Long = 0L,
    val remainingMs: Long = 0L,
    val finished: Boolean = false,
    val totalMs: Long = 0L
) {
    fun remainingAt(now: Long): Long = when {
        !active || finished -> 0L
        running -> (endMs - now).coerceAtLeast(0L)
        else -> remainingMs
    }

    fun isDue(now: Long): Boolean = active && running && !finished && now >= endMs

    fun start(now: Long, lengthMs: Long): TimerState {
        val length = lengthMs.coerceIn(MIN_MS, MAX_MS)
        return TimerState(active = true, running = true, endMs = now + length, totalMs = length)
    }

    fun pause(now: Long): TimerState =
        if (active && running && !finished) copy(running = false, remainingMs = remainingAt(now), endMs = 0L) else this

    fun resume(now: Long): TimerState =
        if (active && !running && !finished) copy(running = true, endMs = now + remainingMs, remainingMs = 0L) else this

    fun toggle(now: Long): TimerState = if (running) pause(now) else resume(now)

    fun addMinute(now: Long): TimerState = when {
        !active || finished -> this
        running -> copy(endMs = minOf(endMs + MINUTE_MS, now + MAX_MS), totalMs = grownTotal(now))
        else -> copy(remainingMs = minOf(remainingMs + MINUTE_MS, MAX_MS), totalMs = grownTotal(now))
    }

    private fun grownTotal(now: Long): Long {
        if (totalMs <= 0L) return 0L
        val grown = totalMs + MINUTE_MS
        return minOf(maxOf(grown, remainingAt(now) + MINUTE_MS), MAX_MS)
    }

    fun finish(): TimerState = if (active && !finished) copy(running = false, endMs = 0L, remainingMs = 0L,
            finished = true) else this

    fun reset(): TimerState = TimerState()

    fun encode(): String = listOf(active, running, endMs, remainingMs, finished, totalMs).joinToString(",")

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
            val total = f.getOrNull(5)?.toLongOrNull() ?: 0L
            return TimerState(true, running, end.coerceAtLeast(0L), left.coerceIn(0L, MAX_MS), finished,
                total.coerceIn(0L, MAX_MS))
        }
    }
}

object TimerRings {
    private val memory = TimerRingMemory()

    fun feed(card: IslandNotification, nowMs: Long = System.currentTimeMillis()) {
        if (card.mode != IslandMode.Timer) return
        val remaining = remainingMs(card, nowMs) ?: return
        synchronized(memory) { memory.feed(TimerRingMemory.idOf(card.key), remaining, card.timerTotalMs, nowMs) }
    }

    fun release(key: String, nowMs: Long = System.currentTimeMillis()) {
        synchronized(memory) { memory.release(TimerRingMemory.idOf(key), nowMs) }
    }

    fun releaseAll(nowMs: Long = System.currentTimeMillis()) {
        synchronized(memory) { memory.releaseAll(nowMs) }
    }

    fun fraction(card: IslandNotification, paused: Boolean = card.recordingPaused,
            nowMs: Long = System.currentTimeMillis()): Float? {
        val remaining = remainingMs(card, nowMs, paused) ?: return null
        val id = TimerRingMemory.idOf(card.key)
        return synchronized(memory) {
            if (!memory.has(id, nowMs)) memory.feed(id, remaining, card.timerTotalMs, nowMs)
            memory.fraction(id, remaining, nowMs)
        }
    }

    fun remainingMs(card: IslandNotification, nowMs: Long, paused: Boolean = card.recordingPaused): Long? {
        val left = if (paused) {
            card.recordingFrozenMs ?: card.timerRemainingSeconds?.times(1000L)
        } else if (card.timeMillis > 0L) {
            card.timeMillis - nowMs
        } else {
            null
        }
        return left?.takeIf { it >= 0L }
    }
}

class TimerRingMemory {
    private class Entry(var totalMs: Long = 0L, var finished: Boolean = false, var releasedAtMs: Long = NOT_RELEASED)

    private val timers = HashMap<String, Entry>()

    fun feed(id: String, remainingMs: Long, knownTotalMs: Long?, nowMs: Long) {
        prune(nowMs)
        val old = timers[id]
        val entry = if (old == null || (old.finished && remainingMs > 0L)) Entry().also { timers[id] = it } else old
        entry.releasedAtMs = NOT_RELEASED
        entry.finished = remainingMs <= 0L
        entry.totalMs = maxOf(entry.totalMs, knownTotalMs ?: 0L, remainingMs)
    }

    fun release(id: String, nowMs: Long) {
        timers[id]?.releasedAtMs = nowMs
    }

    fun releaseAll(nowMs: Long) {
        timers.values.forEach { it.releasedAtMs = nowMs }
    }

    fun has(id: String, nowMs: Long): Boolean {
        prune(nowMs)
        return timers.containsKey(id)
    }

    fun fraction(id: String, remainingMs: Long, nowMs: Long): Float? {
        prune(nowMs)
        val total = timers[id]?.totalMs ?: return null
        if (total <= 0L) return null
        return (remainingMs.coerceAtLeast(0L).toFloat() / total).coerceIn(0f, 1f)
    }

    private fun prune(nowMs: Long) {
        timers.values.removeAll { it.releasedAtMs != NOT_RELEASED && nowMs - it.releasedAtMs > GRACE_MS }
    }

    companion object {
        const val GRACE_MS = 3_000L
        private const val NOT_RELEASED = -1L

        fun idOf(key: String): String {
            val parts = key.split('|')
            return if (parts.size >= 5) parts[1] + "|" + parts[2] else key
        }
    }
}

/**
 * Island's own stopwatch. [startMs] is the wall-clock start, already moved forward by earlier pauses, so running = `now
 * - startMs`, paused keeps [frozenMs]
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
