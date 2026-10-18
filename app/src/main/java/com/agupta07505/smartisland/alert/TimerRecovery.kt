/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

/** Which clock tool an anchor belongs to. */
enum class TimerKind(val code: Char) { Timer('T'), Stopwatch('S') }

/**
 * The absolute moment a clock tool is pinned to (Rule_Book §7.3): a timer's end time, or a stopwatch's start
 * time, as Unix epoch milliseconds. Never a relative "5 minutes left".
 */
data class TimerAnchor(val kind: TimerKind, val epochMs: Long) {
    /** "T:1716382920000", the form kept in SharedPreferences. */
    fun encode(): String = "${kind.code}:$epochMs"

    companion object {
        fun decode(raw: String?): TimerAnchor? {
            if (raw == null || raw.length < 3 || raw[1] != ':') return null
            val kind = TimerKind.entries.firstOrNull { it.code == raw[0] } ?: return null
            val epoch = raw.substring(2).toLongOrNull() ?: return null
            return TimerAnchor(kind, epoch)
        }
    }
}

/** What the island should use for a recovered clock tool. */
data class RecoveredTimer(val epochMs: Long, val promoteToCritical: Boolean)

/**
 * Rebuilds timers and stopwatches after the process was killed (Rule_Book §7.3). Pure, so it is unit-testable;
 * the listener passes in the stored anchor, what it can read from the notification now, and the clock.
 */
object TimerRecovery {

    /** Anchors that moved by less than this are the same anchor (a notification re-posted with a rounded time). */
    const val ANCHOR_JITTER_MS = 1_000L

    /**
     * [stored] is the anchor saved before the process died (or null), [freshEpochMs] the anchor read from the
     * notification just now, [isRestore] true while rehydrating after a (re)start, [paused] true for a paused
     * timer or stopwatch (its anchor is meaningless and keeps moving).
     *
     * While restoring, a stored anchor wins over the freshly parsed one, which is rebuilt from text that may
     * be minutes old. A timer whose end has already passed is promoted to Critical (Tier 0) so it rings.
     */
    fun recover(
        stored: TimerAnchor?,
        kind: TimerKind,
        freshEpochMs: Long,
        nowMs: Long,
        isRestore: Boolean,
        paused: Boolean = false
    ): RecoveredTimer {
        val useStored = isRestore && !paused && stored != null && stored.kind == kind && stored.epochMs > 0L
        val epoch = if (useStored) stored!!.epochMs else freshEpochMs
        val promote = isRestore && !paused && kind == TimerKind.Timer && epoch in 1..nowMs
        return RecoveredTimer(epoch, promote)
    }

    /** Whether [next] is worth writing over [previous] (skips rewriting the same anchor on every tick). */
    fun shouldPersist(previous: TimerAnchor?, next: TimerAnchor): Boolean =
        previous == null || previous.kind != next.kind ||
            kotlin.math.abs(previous.epochMs - next.epochMs) >= ANCHOR_JITTER_MS
}
