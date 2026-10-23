/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationVeilTest {
    @Test
    fun settledReadIsTheSumOfTheGaps() {
        assertEquals(0L, RotationVeil.REFRESH_GAPS_MS.first())
        assertEquals(600L, RotationVeil.SETTLED_READ_AT_MS)
    }

    @Test
    fun normalRotationLiftsJustAfterTheSettledRead() {
        val lastRead = RotationVeil.SETTLED_READ_AT_MS
        val total = lastRead + RotationVeil.liftDelayMs(lastRead)
        assertEquals(lastRead + RotationVeil.MARGIN_MS, total)
        assertTrue("hidden at most the cap", total <= RotationVeil.MAX_HIDDEN_MS)
    }

    @Test
    fun liftDelayIsCappedByTheMaximum() {
        assertEquals(40L, RotationVeil.liftDelayMs(660L))
        assertEquals(0L, RotationVeil.liftDelayMs(700L))
        assertEquals(0L, RotationVeil.liftDelayMs(900L))
    }

    @Test
    fun fadeIsOpaqueWhenShownGoneWhenHiddenAndNeverBackwards() {
        assertEquals(1f, RotationVeil.alpha(1f), 0.0001f)
        assertEquals(0f, RotationVeil.alpha(0f), 0.0001f)
        var last = -1f
        for (i in 0..20) {
            val a = RotationVeil.alpha(i / 20f)
            assertTrue("alpha must not fall as the veil lifts", a >= last - 0.0001f)
            last = a
        }
    }
}
