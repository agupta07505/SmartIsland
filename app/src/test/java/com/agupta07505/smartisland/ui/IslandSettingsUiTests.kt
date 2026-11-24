/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.sections.WIDE_GRID_MIN_WIDTH_DP
import com.agupta07505.smartisland.ui.sections.eventGridColumns
import com.agupta07505.smartisland.ui.sections.formatSeconds
import com.agupta07505.smartisland.ui.sections.formatSpeed
import com.agupta07505.smartisland.ui.sections.gridRows
import com.agupta07505.smartisland.ui.sections.snapToStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventGridTest {
    @Test
    fun phonesGetTwoColumnsAndWideScreensThree() {
        assertEquals(2, eventGridColumns(360))
        assertEquals(2, eventGridColumns(WIDE_GRID_MIN_WIDTH_DP - 1))
        assertEquals(3, eventGridColumns(WIDE_GRID_MIN_WIDTH_DP))
        assertEquals(3, eventGridColumns(900))
    }

    @Test
    fun gridRowPacking() {
        val rows = gridRows(listOf(1, 2, 3, 4, 5), 2)
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), rows)
        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5)), gridRows(listOf(1, 2, 3, 4, 5), 3))

        assertTrue(gridRows(emptyList<Int>(), 2).isEmpty())
        assertEquals(listOf(listOf(1), listOf(2)), gridRows(listOf(1, 2), 0))

        val items = (1..11).toList()
        assertEquals(items, gridRows(items, 3).flatten())
    }
}

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

    @Test
    fun speedShowsAMultiplier() {
        assertEquals("1x", formatSpeed(100f))
        assertEquals("2x", formatSpeed(200f))
        assertEquals("0.75x", formatSpeed(75f))
        assertEquals("1.5x", formatSpeed(150f))
        assertEquals("1.25x", formatSpeed(125f))
    }
}
