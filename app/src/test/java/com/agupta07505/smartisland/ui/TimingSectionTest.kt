/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.sections.formatSeconds
import com.agupta07505.smartisland.ui.sections.snapToStep
import org.junit.Assert.assertEquals
import org.junit.Test

class TimingSectionTest {
    @Test
    fun secondsDropTheDecimalWhenWhole() {
        assertEquals("5 s", formatSeconds(5_000f))
        assertEquals("1.8 s", formatSeconds(1_800f))
        assertEquals("0.5 s", formatSeconds(500f))
        assertEquals("30 s", formatSeconds(30_000f))
    }

    @Test
    fun snapRoundsToTheStepAndStaysInRange() {
        assertEquals(1_500f, snapToStep(1_620f, 500f, 1_000f, 30_000f), 0.001f)
        assertEquals(2_000f, snapToStep(1_760f, 500f, 1_000f, 30_000f), 0.001f)
        assertEquals(1_000f, snapToStep(120f, 500f, 1_000f, 30_000f), 0.001f)
        assertEquals(30_000f, snapToStep(99_000f, 500f, 1_000f, 30_000f), 0.001f)
        assertEquals(1_800f, snapToStep(1_840f, 100f, 500f, 10_000f), 0.001f)
    }
}
