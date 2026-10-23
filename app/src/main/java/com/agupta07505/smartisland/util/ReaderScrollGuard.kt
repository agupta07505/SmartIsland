/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Stops taps that are really the end of a scroll in the reader text or its thumbnail column. */
class ReaderScrollTracker {
    var scrolling: Boolean = false
        private set

    /** Time of the last moment a reader list moved or changed scroll state; 0 = never. */
    var lastActiveMs: Long = 0L
        private set

    /** Counts every scroll step; a touch that sees this change was scrolled under the finger. */
    var moveCount: Long = 0L
        private set

    fun onScrollState(inProgress: Boolean, nowMs: Long) {
        scrolling = inProgress
        lastActiveMs = nowMs
    }

    fun onMoved(nowMs: Long) {
        moveCount++
        lastActiveMs = nowMs
    }

    fun touchStartsOnScroll(nowMs: Long): Boolean =
        scrolling || (lastActiveMs != 0L && nowMs - lastActiveMs < READER_SCROLL_SETTLE_MS)

    fun reset() {
        scrolling = false
        lastActiveMs = 0L
    }
}

/** A tap this soon after a reader scroll stopped is the scroll's own (a finger landing to stop the fling, or just after). */
const val READER_SCROLL_SETTLE_MS = 250L

/**
 * True when a touch that ended in a lift may count as a tap as far as the reader scroll goes: it did not start on a
 * scrolling reader, and the reader did not move while the finger was down.
 */
fun readerTapAccepted(startedOnScroll: Boolean, movesAtDown: Long, movesAtLift: Long): Boolean =
    !startedOnScroll && movesAtDown == movesAtLift
