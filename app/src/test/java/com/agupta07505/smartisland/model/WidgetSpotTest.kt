/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.ui.pickers.widgetSpot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSpotTest {
    private fun fill(cells: List<Pair<Int, Int>>): List<UtilityPlacement> =
        cells.mapIndexed { i, (r, c) -> UtilityPlacement(UtilityType.AppWidget, r, c, widgetId = i + 1) }

    private fun allCells(): List<Pair<Int, Int>> =
        (0 until UtilityGrid.TOTAL_ROWS).flatMap { r -> (0 until UtilityGrid.COLUMNS).map { c -> r to c } }

    @Test
    fun theTappedCellIsUsedWhenItIsFree() {
        assertEquals(0 to 2, widgetSpot(fill(listOf(0 to 0)), 0, 2, widgetId = 99))
    }

    @Test
    fun aTakenCellFallsBackToTheFirstFreeOne() {
        assertEquals(0 to 1, widgetSpot(fill(listOf(0 to 0)), 0, 0, widgetId = 99))
    }

    @Test
    fun aFullGridHasNoSpot() {
        assertNull(widgetSpot(fill(allCells()), 0, 0, widgetId = 99))
    }

    @Test
    fun theLastFreeCellIsFoundWhereverItIs() {
        val cells = allCells().dropLast(1)
        val last = allCells().last()
        assertEquals(last, widgetSpot(fill(cells), 0, 0, widgetId = 99))
    }
}
