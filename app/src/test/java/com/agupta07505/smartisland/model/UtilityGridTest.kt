/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityGridTest {
    private fun tile(type: UtilityType, row: Int, col: Int) = UtilityPlacement(type, row, col)
    private fun widget(id: Int, row: Int, col: Int, rows: Int = 1, cols: Int = 1) =
        UtilityPlacement(UtilityType.AppWidget, row, col, rows, cols, widgetId = id)

    @Test fun gridIsTwoRowsByFourColumns() {
        assertEquals(2, UtilityGrid.ROWS)
        assertEquals(4, UtilityGrid.COLUMNS)
    }

    @Test fun everyRequestedUtilityExists() {
        val ids = UtilityType.entries.map { it.id }.toSet()
        listOf(
            "mobile_data", "wifi", "hotspot", "flashlight", "calculator", "clear_cache",
            "screen_recorder", "screenshot", "timer", "stopwatch", "lock_screen", "app_widget",
            "voice_recorder", "camera", "bluetooth",
            "airplane_mode", "auto_rotate", "dark_mode", "battery_saver"
        ).forEach { assertTrue(it, it in ids) }
    }

    @Test fun theShadeCastAndPowerMenuUtilitiesAreGone() {
        val ids = UtilityType.entries.map { it.id }.toSet()
        listOf("cast", "notification_shade", "power_menu").forEach { assertFalse(it, it in ids) }
        // A layout saved with them keeps everything else.
        val layout = UtilityLayout.decode("v2:power_menu,0,0,1,1,-1;wifi,0,1,1,1,-1;cast,0,2,1,1,-1")
        assertEquals(listOf(UtilityType.WiFi), layout.map { it.type })
    }

    @Test fun onlyWidgetsResize() {
        assertTrue(UtilityType.AppWidget.resizable)
        assertTrue(UtilityType.singles.none { it.resizable })
    }

    @Test fun itemsStayInsideTheGrid() {
        assertTrue(tile(UtilityType.WiFi, 1, 3).isValid)
        assertTrue(tile(UtilityType.WiFi, 2, 0).isValid)
        assertTrue(tile(UtilityType.WiFi, 3, 3).isValid)
        assertFalse(tile(UtilityType.WiFi, 4, 0).isValid)
        assertFalse(tile(UtilityType.WiFi, 0, 4).isValid)
        assertFalse(tile(UtilityType.WiFi, -1, 0).isValid)
    }

    @Test fun widgetsRangeFromOneByOneToTheWholeGrid() {
        assertTrue(widget(1, 0, 0, rows = 1, cols = 1).isValid)
        assertTrue(widget(1, 0, 0, rows = 2, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 3, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 2, cols = 5).isValid)
        assertFalse(widget(1, 1, 0, rows = 2, cols = 1).isValid)
        assertFalse(widget(1, 0, 0, rows = 0, cols = 1).isValid)
    }

    @Test fun aWidgetNeedsItsId() {
        assertFalse(UtilityPlacement(UtilityType.AppWidget, 0, 0).isValid)
    }

    @Test fun overlapIsDetected() {
        assertTrue(widget(1, 0, 0, 2, 2).overlaps(tile(UtilityType.WiFi, 1, 1)))
        assertFalse(widget(1, 0, 0, 2, 1).overlaps(tile(UtilityType.WiFi, 0, 1)))
        assertFalse(tile(UtilityType.WiFi, 0, 0).overlaps(tile(UtilityType.Hotspot, 1, 0)))
    }

    @Test fun addRefusesOverlapsDuplicatesAndOutOfGrid() {
        val one = UtilityLayout.add(emptyList(), tile(UtilityType.WiFi, 0, 0))
        assertEquals(1, one.size)
        assertEquals(one, UtilityLayout.add(one, tile(UtilityType.WiFi, 1, 0)))
        assertEquals(one, UtilityLayout.add(one, tile(UtilityType.Hotspot, 0, 0)))
        assertEquals(one, UtilityLayout.add(one, tile(UtilityType.Hotspot, 4, 0)))
        assertEquals(2, UtilityLayout.add(one, tile(UtilityType.Hotspot, 0, 1)).size)
    }

    @Test fun addingTheSameWidgetTwiceDoesNothing() {
        val one = UtilityLayout.add(emptyList(), widget(7, 0, 0))
        assertEquals(one, UtilityLayout.add(one, widget(7, 1, 0)))
        assertEquals(2, UtilityLayout.add(one, widget(8, 1, 0)).size)
    }

    @Test fun moveRefusesCollisionsAndOutOfGrid() {
        val layout = listOf(widget(1, 0, 0, 2, 1), tile(UtilityType.WiFi, 0, 1))
        assertEquals(layout, UtilityLayout.move(layout, "widget:1", 0, 1))
        // A two-row widget at row 1 would sit across the two tabs.
        assertEquals(layout, UtilityLayout.move(layout, "widget:1", 1, 0))
    }

    @Test fun moveToAFreeSpotWorks() {
        val layout = listOf(widget(1, 0, 0, 2, 1), tile(UtilityType.WiFi, 0, 1))
        val moved = UtilityLayout.move(layout, "widget:1", 0, 2)
        val w = moved.first { it.key == "widget:1" }
        assertEquals(0, w.row)
        assertEquals(2, w.col)
        assertEquals(2, w.rowSpan)
    }

    @Test fun resizeGrowsOnlyIntoFreeCells() {
        val layout = listOf(widget(1, 0, 0), tile(UtilityType.WiFi, 0, 1))
        assertEquals(layout, UtilityLayout.resize(layout, "widget:1", 1, 2))
        assertEquals(2, UtilityLayout.resize(layout, "widget:1", 2, 1).first { it.key == "widget:1" }.rowSpan)
        assertEquals(layout, UtilityLayout.resize(layout, "widget:1", 3, 1))
    }

    @Test fun fixedUtilitiesCannotBeResized() {
        val layout = listOf(tile(UtilityType.WiFi, 0, 0))
        assertEquals(layout, UtilityLayout.resize(layout, "wifi", 2, 1))
    }

    @Test fun firstFreeSpotReadsRowByRow() {
        assertEquals(0 to 0, UtilityLayout.firstFreeSpot(emptyList()))
        val layout = listOf(tile(UtilityType.WiFi, 0, 0))
        assertEquals(0 to 1, UtilityLayout.firstFreeSpot(layout))
        val fullRow = layout + tile(UtilityType.Hotspot, 0, 1) + tile(UtilityType.Flashlight, 0, 2) + tile(UtilityType.Calculator, 0, 3)
        assertEquals(1 to 0, UtilityLayout.firstFreeSpot(fullRow))
        assertEquals(1 to 0, UtilityLayout.firstFreeSpot(fullRow, rowSpan = 1, colSpan = 4))
        // A 2x2 widget no longer fits the first tab, so it goes to the second.
        assertEquals(2 to 0, UtilityLayout.firstFreeSpot(fullRow, rowSpan = 2, colSpan = 2))
        assertEquals(2 to 0, UtilityLayout.firstFreeSpot(listOf(widget(1, 0, 0, 2, 4))))
        assertNull(UtilityLayout.firstFreeSpot(listOf(widget(1, 0, 0, 2, 4), widget(2, 2, 0, 2, 4))))
    }

    @Test fun usedRowsIsTheDeepestBottomEdge() {
        assertEquals(0, UtilityLayout.usedRows(emptyList()))
        assertEquals(1, UtilityLayout.usedRows(listOf(tile(UtilityType.WiFi, 0, 0))))
        assertEquals(2, UtilityLayout.usedRows(listOf(tile(UtilityType.WiFi, 0, 0), widget(1, 1, 1, 1, 3))))
    }

    @Test fun itemAtFindsTheCoveringItem() {
        val layout = listOf(widget(1, 0, 0, 2, 2))
        assertEquals("widget:1", UtilityLayout.itemAt(layout, 1, 1)?.key)
        assertNull(UtilityLayout.itemAt(layout, 2, 0))
    }

    @Test fun addableListsWhatIsNotOnTheGridYet() {
        val layout = listOf(tile(UtilityType.WiFi, 0, 0))
        val addable = UtilityLayout.addable(layout)
        assertFalse(UtilityType.WiFi in addable)
        assertTrue(UtilityType.Flashlight in addable)
        assertFalse(UtilityType.AppWidget in addable)
    }

    @Test fun saveAndLoadRoundTrips() {
        val layout = listOf(
            tile(UtilityType.WiFi, 0, 0),
            tile(UtilityType.Calculator, 0, 1),
            widget(42, 1, 0, 1, 4)
        )
        assertEquals(layout, UtilityLayout.decode(UtilityLayout.encode(layout)))
    }

    @Test fun aSavedLayoutIsMarkedAsBelongingToTheCurrentGrid() {
        assertTrue(UtilityLayout.encode(listOf(tile(UtilityType.WiFi, 1, 3))).startsWith("v2:"))
        assertEquals("", UtilityLayout.encode(emptyList()))
        // A marked layout is read as it is, not turned on its side.
        val p = UtilityLayout.decode("v2:wifi,1,3,1,1,-1").single()
        assertEquals(1, p.row)
        assertEquals(3, p.col)
    }

    @Test fun aLayoutFromTheFirstGridIsTurnedOnItsSide() {
        // First grid: 4 rows by 2 columns. Wi-Fi was in row 0, column 1; the widget started at row 1, column 0 and was 3 rows by 2.
        val layout = UtilityLayout.decode("wifi,0,1,1,1,-1;app_widget,1,0,3,2,7")
        val wifi = layout.first { it.key == "wifi" }
        assertEquals(1, wifi.row)
        assertEquals(0, wifi.col)
        val widget = layout.first { it.key == "widget:7" }
        assertEquals(0, widget.row)
        assertEquals(1, widget.col)
        assertEquals(2, widget.rowSpan)
        assertEquals(3, widget.colSpan)
    }

    @Test fun everyCellOfTheFirstGridStillHasAPlace() {
        val old = (0 until 4).flatMap { r -> (0 until 2).map { c -> r to c } }
        val types = UtilityType.singles.take(old.size)
        val raw = old.zip(types).joinToString(";") { (cell, type) -> "${type.id},${cell.first},${cell.second},1,1,-1" }
        assertEquals(8, UtilityLayout.decode(raw).size)
    }

    @Test fun loadingBadDataKeepsOnlyWhatIsValid() {
        assertEquals(emptyList<UtilityPlacement>(), UtilityLayout.decode(null))
        assertEquals(emptyList<UtilityPlacement>(), UtilityLayout.decode(""))
        assertEquals(emptyList<UtilityPlacement>(), UtilityLayout.decode("nonsense"))
        val raw = listOf(
            "wifi,0,0,1,1,-1",
            "wifi,1,0,1,1,-1",
            "hotspot,0,0,1,1,-1",
            "bogus,2,0,1,1,-1",
            "flashlight,9,9,1,1,-1",
            "timer,x,0,1,1,-1",
            "stopwatch,3,1,1,1,-1"
        ).joinToString(";")
        assertEquals(listOf("wifi", "stopwatch"), UtilityLayout.decode(raw).map { it.key })
    }

    @Test fun loadingForcesFixedUtilitiesToOneCell() {
        val p = UtilityLayout.decode("wifi,0,0,2,2,-1").single()
        assertEquals(1, p.rowSpan)
        assertEquals(1, p.colSpan)
    }

    @Test fun loadingDropsAWidgetWithoutAnId() {
        assertEquals(emptyList<UtilityPlacement>(), UtilityLayout.decode("app_widget,0,0,1,1,-1"))
    }

    @Test fun aSavedLayoutWithTheRemovedSimSwitcherLosesOnlyThatTile() {
        val layout = UtilityLayout.decode("sim_switcher,0,0,1,1,-1;wifi,0,1,1,1,-1")
        assertEquals(listOf(UtilityType.WiFi), layout.map { it.type })
    }
    @Test fun droppingOnAFreeCellMovesTheItem() {
        val layout = listOf(tile(UtilityType.WiFi, 0, 0))
        assertEquals(listOf(tile(UtilityType.WiFi, 1, 3)), UtilityLayout.drop(layout, "wifi", 1, 3))
        assertEquals(layout, UtilityLayout.drop(layout, "wifi", 0, 0))
        assertEquals(layout, UtilityLayout.drop(layout, "missing", 1, 1))
    }

    @Test fun droppingOnAnotherItemTradesPlaces() {
        val layout = listOf(tile(UtilityType.WiFi, 0, 0), tile(UtilityType.Hotspot, 1, 2))
        val out = UtilityLayout.drop(layout, "wifi", 1, 2)
        assertEquals(tile(UtilityType.WiFi, 1, 2), out.first { it.key == "wifi" })
        assertEquals(tile(UtilityType.Hotspot, 0, 0), out.first { it.key == "hotspot" })
    }

    @Test fun aTradeThatDoesNotFitChangesNothing() {
        // A two-wide widget cannot take the one-cell spot next to the edge of the grid, so the tile cannot trade with it.
        val layout = listOf(tile(UtilityType.WiFi, 0, 3), widget(7, 0, 1, rows = 1, cols = 2))
        assertEquals(layout, UtilityLayout.drop(layout, "wifi", 0, 1))
        // Landing across two items is not a trade either.
        val two = listOf(widget(7, 0, 0, rows = 1, cols = 2), tile(UtilityType.WiFi, 0, 2), tile(UtilityType.Hotspot, 0, 3))
        assertEquals(two, UtilityLayout.drop(two, "widget:7", 0, 2))
    }

    @Test fun aWidgetTradesWithATileWhenBothFit() {
        val layout = listOf(tile(UtilityType.WiFi, 0, 0), widget(7, 1, 0, rows = 1, cols = 2))
        val out = UtilityLayout.drop(layout, "wifi", 1, 1)
        assertEquals(tile(UtilityType.WiFi, 1, 0), out.first { it.key == "wifi" })
        assertEquals(widget(7, 0, 0, rows = 1, cols = 2), out.first { it.key == "widget:7" })
    }

    @Test fun gridHasTwoTabsOfTwoRows() {
        assertEquals(2, UtilityGrid.TABS)
        assertEquals(4, UtilityGrid.TOTAL_ROWS)
        assertEquals(0, tile(UtilityType.WiFi, 1, 0).tab)
        assertEquals(1, tile(UtilityType.WiFi, 2, 0).tab)
        assertEquals(1, UtilityGrid.localRow(3))
    }

    @Test fun aWidgetCannotSitAcrossTwoTabs() {
        assertFalse(widget(1, 1, 0, rows = 2, cols = 1).isValid)
        assertTrue(widget(1, 2, 0, rows = 2, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 3, cols = 1).isValid)
    }

    private fun fullFirstTab() = (0 until 2).flatMap { r -> (0 until 4).map { c -> r to c } }
        .zip(UtilityType.singles.take(8)).map { (cell, type) -> tile(type, cell.first, cell.second) }

    @Test fun theSecondTabOnlyExistsOnceTheFirstIsFull() {
        assertEquals(1, UtilityLayout.tabCount(emptyList()))
        val full = fullFirstTab()
        assertEquals(8, full.size)
        assertTrue(UtilityLayout.isTabFull(full, 0))
        assertEquals(2, UtilityLayout.tabCount(full))
        // One cell short: still a single page, even with things placed on the second tab.
        val short = full.drop(1) + tile(UtilityType.Camera, 2, 0)
        assertFalse(UtilityLayout.isTabFull(short, 0))
        assertEquals(1, UtilityLayout.tabCount(short))
    }

    @Test fun aWidgetCountsTowardAFullFirstTab() {
        val layout = listOf(widget(1, 0, 0, rows = 2, cols = 4))
        assertTrue(UtilityLayout.isTabFull(layout, 0))
        assertEquals(2, UtilityLayout.tabCount(layout))
    }

    @Test fun pageHeightFollowsTheFirstTabOnly() {
        assertEquals(0, UtilityLayout.usedRows(listOf(tile(UtilityType.WiFi, 2, 0))))
        assertEquals(2, UtilityLayout.usedRows(fullFirstTab() + tile(UtilityType.Camera, 3, 0)))
    }

    @Test fun snapCellKeepsAnItemInsideItsOwnTab() {
        val second = tile(UtilityType.WiFi, 3, 0)
        // y is measured from the top of the item's tab.
        assertEquals(3 to 0, UtilityLayout.snapCell(second, 0f, 64f, 80f, 64f))
        assertEquals(3 to 3, UtilityLayout.snapCell(second, 5000f, 5000f, 80f, 64f))
        assertEquals(2 to 0, UtilityLayout.snapCell(second, -300f, -300f, 80f, 64f))
    }

    @Test fun snapCellRoundsToTheNearestCellInsideTheGrid() {
        val item = tile(UtilityType.WiFi, 0, 0)
        assertEquals(0 to 1, UtilityLayout.snapCell(item, 40f, 10f, 80f, 64f))
        assertEquals(1 to 3, UtilityLayout.snapCell(item, 5000f, 5000f, 80f, 64f))
        assertEquals(0 to 0, UtilityLayout.snapCell(item, -300f, -300f, 80f, 64f))
        val wide = widget(7, 0, 0, rows = 1, cols = 2)
        assertEquals(0 to 2, UtilityLayout.snapCell(wide, 5000f, 0f, 80f, 64f))
    }
}
