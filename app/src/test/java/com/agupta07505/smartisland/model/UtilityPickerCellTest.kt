/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.ui.pickers.firstEmptyUtilityCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilityPickerCellTest {
    private fun fill(cells: List<Pair<Int, Int>>): List<UtilityPlacement> =
        cells.mapIndexed { i, (r, c) -> UtilityPlacement(UtilityType.AppWidget, r, c, widgetId = i + 1) }

    @Test
    fun anEmptyGridOpensOnTheTopLeftCell() {
        assertEquals(0 to 0, firstEmptyUtilityCell(emptyList()))
    }

    @Test
    fun theFirstGapInReadingOrderIsChosen() {
        val layout = fill(listOf(0 to 0, 0 to 1, 0 to 3))
        assertEquals(0 to 2, firstEmptyUtilityCell(layout))
    }

    @Test
    fun aFullFirstTabMovesOnToTheSecond() {
        val cells = (0 until UtilityGrid.ROWS).flatMap { r -> (0 until UtilityGrid.COLUMNS).map { c -> r to c } }
        assertEquals(UtilityGrid.ROWS to 0, firstEmptyUtilityCell(fill(cells)))
    }

    @Test
    fun aWideWidgetCoversTheCellsItSpans() {
        val wide = UtilityPlacement(UtilityType.AppWidget, 0, 0, rowSpan = 1, colSpan = 2, widgetId = 1)
        assertEquals(0 to 2, firstEmptyUtilityCell(listOf(wide)))
    }

    @Test
    fun aFullGridHasNoCell() {
        val cells = (0 until UtilityGrid.TOTAL_ROWS).flatMap { r -> (0 until UtilityGrid.COLUMNS).map { c -> r to c } }
        assertNull(firstEmptyUtilityCell(fill(cells)))
    }
}
