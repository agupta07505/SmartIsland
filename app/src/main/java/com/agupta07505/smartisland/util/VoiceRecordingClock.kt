/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * The voice recorder's duration (Rule_Book §4.3). Recorder notifications either run a chronometer from the
 * recording's start (`when` + `showChronometer`) or rewrite their text every second ("Recording 00:42").
 * Pure, so it is unit-testable.
 */
object VoiceRecordingClock {

    /**
     * When the recording started, in epoch ms. A chronometer base wins; otherwise the shown duration counted back
     * from [nowMs]; otherwise the start already known for this alert; otherwise now.
     */
    fun startMillis(nowMs: Long, chronometerWhenMs: Long?, shownElapsedSeconds: Long?, previousStartMs: Long?): Long = when {
        chronometerWhenMs != null && chronometerWhenMs in 1..nowMs -> chronometerWhenMs
        shownElapsedSeconds != null && shownElapsedSeconds >= 0 -> nowMs - shownElapsedSeconds * 1000L
        previousStartMs != null && previousStartMs in 1..nowMs -> previousStartMs
        else -> nowMs
    }

    /**
     * Seconds to show. While [paused] the count stands still: the shown duration when the app gives one, else
     * [frozenSeconds] (what the island showed when the pause began).
     */
    fun displaySeconds(nowMs: Long, startMs: Long, paused: Boolean, shownElapsedSeconds: Long?, frozenSeconds: Long?): Long {
        if (paused) {
            shownElapsedSeconds?.takeIf { it >= 0 }?.let { return it }
            frozenSeconds?.let { return it }
        }
        return ((nowMs - startMs) / 1000L).coerceAtLeast(0L)
    }

    /** MM:SS, or H:MM:SS past an hour. */
    fun format(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
