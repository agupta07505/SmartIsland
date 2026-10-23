/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderScrollGuardTest {
    @Test
    fun aReaderThatNeverScrolledAllowsTaps() {
        val t = ReaderScrollTracker()
        assertFalse(t.touchStartsOnScroll(1_000L))
    }

    @Test
    fun aTouchOnAFlingingReaderIsNotATap() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        assertTrue(t.touchStartsOnScroll(5_000L)) // still flinging, however long
    }

    @Test
    fun aTouchJustAfterTheFlingStoppedIsNotATapButALaterOneIs() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        t.onScrollState(false, 2_000L)
        assertTrue(t.touchStartsOnScroll(2_000L + READER_SCROLL_SETTLE_MS - 1))
        assertFalse(t.touchStartsOnScroll(2_000L + READER_SCROLL_SETTLE_MS))
    }

    @Test
    fun anOffsetChangeCountsAsActivity() {
        val t = ReaderScrollTracker()
        t.onMoved(3_000L)
        assertTrue(t.touchStartsOnScroll(3_100L))
        assertFalse(t.touchStartsOnScroll(3_400L))
    }

    @Test
    fun resetForgetsAScrollThatLeftTheScreen() {
        val t = ReaderScrollTracker()
        t.onScrollState(true, 1_000L)
        t.reset()
        assertFalse(t.touchStartsOnScroll(1_050L))
    }

    @Test
    fun theTapNeedsAQuietReaderAtDownAndNoMovementUnderTheFinger() {
        assertTrue(readerTapAccepted(startedOnScroll = false, movesAtDown = 4, movesAtLift = 4))
        assertFalse(readerTapAccepted(startedOnScroll = true, movesAtDown = 4, movesAtLift = 4))
        assertFalse(readerTapAccepted(startedOnScroll = false, movesAtDown = 4, movesAtLift = 5))
    }
}
