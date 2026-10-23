/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureRetryPlanTest {
    @Test
    fun theFirstAttemptIsImmediateAndTheGapsGrow() {
        val d = PictureRetryPlan.DELAYS_MS
        assertEquals(0L, d.first())
        assertTrue(d.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun theScheduleEndsWithinAboutHalfAMinute() {
        assertTrue(PictureRetryPlan.DELAYS_MS.sum() in 20_000L..40_000L)
    }

    @Test
    fun aPlainTextMessageIsNeverRetried() {
        assertFalse(PictureRetryPlan.expectsPicture(hasImageLink = false, hasBigPicture = false, textIsPictureLabel = false))
    }

    @Test
    fun aPictureLinkABigPictureOrAPictureLabelIsRetried() {
        assertTrue(PictureRetryPlan.expectsPicture(true, false, false))
        assertTrue(PictureRetryPlan.expectsPicture(false, true, false))
        assertTrue(PictureRetryPlan.expectsPicture(false, false, true))
    }

    @Test
    fun hasNextAttemptStopsAtTheLastEntry() {
        val last = PictureRetryPlan.DELAYS_MS.lastIndex
        assertTrue(PictureRetryPlan.hasNextAttempt(0))
        assertFalse(PictureRetryPlan.hasNextAttempt(last))
    }
}
