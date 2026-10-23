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

class SendAttemptsTest {
    @Test
    fun replacedIntentThatFailsEveryWayReportsFailure() {
        val attempts = List<() -> Unit>(3) { { throw IllegalStateException("canceled") } }
        assertFalse(SendAttempts.anySucceeds(attempts))
    }

    @Test
    fun laterAttemptRescuesAFailedFirstOne() {
        var calls = 0
        val attempts = listOf<() -> Unit>(
            { calls++; throw IllegalStateException("options rejected") },
            { calls++ }
        )
        assertTrue(SendAttempts.anySucceeds(attempts))
        assertEquals(2, calls)
    }

    @Test
    fun deliveredIntentIsNotSentAgain() {
        var calls = 0
        val attempts = listOf<() -> Unit>({ calls++ }, { calls++ }, { calls++ })
        assertTrue(SendAttempts.anySucceeds(attempts))
        assertEquals(1, calls)
    }

    @Test
    fun nothingToTryIsAFailure() {
        assertFalse(SendAttempts.anySucceeds(emptyList()))
    }
}
