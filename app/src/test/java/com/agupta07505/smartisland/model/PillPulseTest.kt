/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillPulseTest {
    @Test
    fun pulsesOnceInTwoSeconds() {
        assertTrue(PillPulse.allowed(10_000L, 10_000L - PillPulse.MIN_GAP_MS))
        assertFalse(PillPulse.allowed(10_000L, 9_000L))
        assertFalse(PillPulse.allowed(10_000L, 10_000L))
        assertTrue(PillPulse.allowed(12_000L, 10_000L))
    }
}
