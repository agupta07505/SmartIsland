/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import kotlin.math.abs

/**
 * Elapsed time of a screen / voice recording, tracked from the recorder's own notification.
 *
 * Why not just use `Notification.when`: recorders re-post their notification every second (or on
 * every pause / resume), and `when` then moves with it, so a timer built on it restarts, runs ahead
 * or keeps counting after the recording is saved. Instead the clock reads, in this order:
 *   1. the time written in the notification ("Currently recording 00:26"),
 *   2. the notification's chronometer base,
 *   3. its own previous value.
 * While paused or finished the value is frozen, so it never moves.
 */
object RecordingClock {
    /** [startMillis] is `now - elapsed` while running; [frozenMs] is non-null while paused / finished. */
    data class State(val startMillis: Long, val frozenMs: Long?) {
        fun elapsedMs(nowMillis: Long): Long = (frozenMs ?: (nowMillis - startMillis)).coerceAtLeast(0L)
    }

    private val TIME = Regex("""(?<![\d:])(?:(\d{1,2}):)?([0-5]?\d):([0-5]\d)(?![\d:])""")
    private const val JITTER_MS = 1_500L

    private val RESUME_WORDS = listOf(
        "resume", "continue", "unpause", "reanudar", "reprendre", "weiter", "riprendi", "continuar",
        "继续", "再開", "возобновить"
    )
    private val PAUSE_WORDS = listOf("pause", "pausa", "pausar", "pausieren", "暂停", "一時停止", "пауза")
    private val PAUSED_TEXT = listOf(
        "paused", "pausado", "pausada", "en pause", "pausiert", "in pausa", "已暂停", "一時停止中", "приостановлен"
    )

    /** First "mm:ss" or "h:mm:ss" found in the given texts, in milliseconds. */
    fun parseElapsedMs(vararg texts: String?): Long? {
        for (text in texts) {
            if (text.isNullOrBlank()) continue
            val m = TIME.find(text) ?: continue
            val hours = m.groupValues[1].toLongOrNull() ?: 0L
            val minutes = m.groupValues[2].toLongOrNull() ?: continue
            val seconds = m.groupValues[3].toLongOrNull() ?: continue
            return ((hours * 60 + minutes) * 60 + seconds) * 1000L
        }
        return null
    }

    /** A stopped chronometer prints elapsed plus the age of its post. */
    fun withoutPostAge(readingMs: Long?, ageMs: Long): Long? =
        readingMs?.minus(ageMs.coerceAtLeast(0L))?.takeIf { it > 0L }

    fun frozenReading(paused: Boolean, shownMs: Long?, parsedMs: Long?): Long? =
        if (paused && shownMs != null && shownMs > 0L) shownMs else parsedMs

    /** Paused when a "Resume" button is offered (and no "Pause" one), or the text says "paused". */
    fun isPaused(actionTitles: List<String>, text: String): Boolean {
        val titles = actionTitles.map { it.lowercase() }
        val offersPause = titles.any { t -> PAUSE_WORDS.any { t.contains(it) } }
        val offersResume = titles.any { t -> RESUME_WORDS.any { t.contains(it) } }
        if (offersResume && !offersPause) return true
        if (offersPause) return false
        val lower = text.lowercase()
        return PAUSED_TEXT.any { lower.contains(it) }
    }

    /**
     * The state a post continues from: the card's own, or, for a paused post, the app's last one. A running post
     * never borrows another card's clock.
     */
    fun continuation(own: State?, lastForApp: State?, paused: Boolean): State? =
        own ?: lastForApp?.takeIf { paused }

    /**
     * Paused state that survives a clock app's cancel-and-repost (the new post may show neither "Resume" nor "paused").
     * Counts as running again only once the app offers "Pause".
     */
    fun resolvePaused(detected: Boolean, lastForApp: State?, actionTitles: List<String>): Boolean {
        if (detected) return true
        if (lastForApp?.frozenMs == null) return false
        val offersPause = actionTitles.any { t -> PAUSE_WORDS.any { t.lowercase().contains(it) } }
        return !offersPause
    }

    /**
     * The next clock state. [exactFrozenMs] ([TimerClock.elapsedFromChronometer]) wins while paused; [exactStart] means
     * [chronometerBaseMs] is exact, so the start is not smoothed towards the previous one.
     */
    fun next(
        previous: State?,
        nowMillis: Long,
        paused: Boolean,
        finished: Boolean,
        parsedMs: Long?,
        chronometerBaseMs: Long?,
        fallbackFrozenMs: Long? = null,
        exactFrozenMs: Long? = null,
        exactStart: Boolean = false
    ): State {
        val previousElapsed = previous?.elapsedMs(nowMillis)
        if (paused && !finished && exactFrozenMs != null && exactFrozenMs >= 0L) {
            return State(startMillis = nowMillis - exactFrozenMs, frozenMs = exactFrozenMs)
        }
        if (paused && !finished && previous?.frozenMs != null) {
            return previous
        }
        if (paused || finished) {
            // A paused post often prints 00:00.
            val frozen = parsedMs?.takeIf { it > 0L }
                ?: previousElapsed?.takeIf { it > 0L }
                ?: fallbackFrozenMs?.takeIf { it > 0L }
                ?: parsedMs
                ?: previousElapsed
                ?: 0L
            return State(startMillis = nowMillis - frozen, frozenMs = frozen)
        }
        val elapsed = parsedMs
            ?: chronometerBaseMs?.let { nowMillis - it }?.takeIf { it >= 0 }
            ?: previousElapsed
            ?: 0L
        val candidateStart = nowMillis - elapsed
        // Keep the old start unless it moved beyond whole-second jitter.
        val start = if (!exactStart && previous != null && previous.frozenMs == null &&
            abs(previous.startMillis - candidateStart) <= JITTER_MS
        ) previous.startMillis else candidateStart
        return State(startMillis = start, frozenMs = null)
    }

    /** True when a recorder's notification is a paused recording (still ongoing in spirit). */
    fun looksPaused(n: Notification): Boolean {
        val titles = runCatching { n.actions.orEmpty().map { it.title?.toString().orEmpty() } }
            .getOrDefault(emptyList())
        val ex = n.extras
        val text = runCatching {
            "${ex?.getCharSequence(Notification.EXTRA_TITLE)} ${ex?.getCharSequence(Notification.EXTRA_TEXT)} " +
                "${ex?.getCharSequence(Notification.EXTRA_BIG_TEXT)}"
        }.getOrDefault("")
        return isPaused(titles, text)
    }
}
