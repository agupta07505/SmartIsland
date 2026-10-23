/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.components.batterySwellScale
import com.agupta07505.smartisland.ui.components.pluggedInTurnMs
import com.agupta07505.smartisland.ui.components.pluggedInTurns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryCutoutBadgeTest {

    @Test
    fun theRingTurnsOnceForAShortCardAndTwiceForALongOne() {
        assertEquals(1, pluggedInTurns(1_000L))
        assertEquals(1, pluggedInTurns(2_000L))
        assertEquals(1, pluggedInTurns(4_000L))
        assertEquals(2, pluggedInTurns(5_000L))
        assertEquals(2, pluggedInTurns(30_000L))
    }

    @Test
    fun allTurnsFinishInsideTheCardTime() {
        for (cardMs in longArrayOf(1_000L, 2_000L, 4_000L, 6_000L, 10_000L)) {
            val total = pluggedInTurns(cardMs) * pluggedInTurnMs(cardMs)
            assertTrue("cardMs=$cardMs total=$total", total <= cardMs)
        }
    }

    @Test
    fun theSwellStartsAndEndsAtNormalSizeAndPeaksInTheMiddle() {
        assertEquals(1f, batterySwellScale(0f), 0.0001f)
        assertEquals(1f, batterySwellScale(1f), 0.001f)
        assertEquals(1.2f, batterySwellScale(0.5f), 0.0001f)
    }
}
