/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.sections.WIDE_GRID_MIN_WIDTH_DP
import com.agupta07505.smartisland.ui.sections.eventGridColumns
import com.agupta07505.smartisland.ui.sections.gridRows
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
    fun rowsAreFilledLeftToRightAndTheLastOneMayBeShort() {
        val rows = gridRows(listOf(1, 2, 3, 4, 5), 2)
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), rows)
        assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5)), gridRows(listOf(1, 2, 3, 4, 5), 3))
    }

    @Test
    fun emptyAndOddInputsAreSafe() {
        assertTrue(gridRows(emptyList<Int>(), 2).isEmpty())
        assertEquals(listOf(listOf(1), listOf(2)), gridRows(listOf(1, 2), 0))
    }

    @Test
    fun everyItemAppearsExactlyOnce() {
        val items = (1..11).toList()
        assertEquals(items, gridRows(items, 3).flatten())
    }
}
