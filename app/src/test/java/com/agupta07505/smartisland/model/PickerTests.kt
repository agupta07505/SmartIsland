/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.LockScreenPreset
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.pickers.PickerKinds
import com.agupta07505.smartisland.ui.pickers.firstEmptyUtilityCell
import com.agupta07505.smartisland.util.PickerRequest
import com.agupta07505.smartisland.util.SelectionActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutActionTest {
    @Test
    fun shortcutMenuItems() {
        val expected = listOf(AppShortcutAction.AppInfo, AppShortcutAction.FloatingWindow, AppShortcutAction.SplitScreen)
        assertEquals(expected, appShortcutMenu(FloatingWindowRoute.System))
        assertEquals(expected, appShortcutMenu(FloatingWindowRoute.Lmo))

        assertEquals(
            listOf(AppShortcutAction.AppInfo, AppShortcutAction.SplitScreen),
            appShortcutMenu(FloatingWindowRoute.Off)
        )
    }

    @Test
    fun splitScreenTargets() {
        assertEquals(
            SplitPlan.PairWithForeground,
            splitScreenPlan("com.chat", setOf("com.launcher"), "com.maps")
        )

        assertEquals(
            SplitPlan.LaunchThenSystemPicker,
            splitScreenPlan("com.launcher", setOf("com.launcher", "com.other.home"), "com.maps")
        )

        assertEquals(SplitPlan.LaunchThenSystemPicker, splitScreenPlan(null, emptySet(), "com.maps"))
        assertEquals(SplitPlan.LaunchThenSystemPicker, splitScreenPlan("", emptySet(), "com.maps"))

        assertEquals(
            SplitPlan.LaunchThenSystemPicker,
            splitScreenPlan("com.maps", setOf("com.launcher"), "com.maps")
        )
    }
}

class LockScreenPresetTest {
    private val base = SmartIslandSettings.Default.copy(showOnLockScreen = true)

    @Test
    fun everyPresetReadsBackAsItself() {
        LockScreenPreset.entries.forEach { preset ->
            assertEquals(preset, LockScreenPreset.from(preset.applyTo(base)))
            assertEquals(preset, LockScreenPreset.from(preset.applyTo(base.copy(hideOnLockScreenEvents = setOf("music")))))
        }
    }

    @Test
    fun showAllVsMessagesOnly() {
        val all = LockScreenPreset.ShowAll.applyTo(base.copy(hideOnLockScreenEvents = setOf("music")))
        assertTrue(all.hideOnLockScreenEvents.isEmpty())
        assertEquals(false, all.hideAllOnLockScreen)

        val messages = LockScreenPreset.MessagesOnly.applyTo(base)
        assertEquals(false, messages.isEventHiddenOnLockScreen("notification"))
        assertEquals(false, messages.isEventHiddenOnLockScreen("call"))
        assertEquals(true, messages.isEventHiddenOnLockScreen("music"))
    }

    @Test
    fun callsOnlyKeepsCallsOffHidesIsland() {
        val calls = LockScreenPreset.CallsOnly.applyTo(base)
        assertEquals(true, calls.isEventHiddenOnLockScreen("notification"))
        assertEquals(false, calls.isEventHiddenOnLockScreen("call"))

        assertEquals(false, LockScreenPreset.Off.applyTo(base).showOnLockScreen)
        assertEquals(LockScreenPreset.ShowAll, LockScreenPreset.from(SmartIslandSettings.Default))
    }

    @Test
    fun customMixSelectsNoPreset() {
        assertNull(LockScreenPreset.from(base.copy(hideOnLockScreenEvents = setOf("music"))))
    }

    @Test
    fun presetKeepsOtherSettings() {
        val custom = base.copy(width = 140f, hideOnLockScreenPackages = setOf("com.example.a"))
        val result = LockScreenPreset.CallsOnly.applyTo(custom)
        assertEquals(140f, result.width)
        assertEquals(setOf("com.example.a"), result.hideOnLockScreenPackages)
    }
}

class PickerKindsTest {
    @Test
    fun onlyKnownPickerKindsResolve() {
        assertEquals(PickerKinds.APPS, PickerKinds.resolve("apps"))
        assertEquals(PickerKinds.UTILITY, PickerKinds.resolve("utility"))
        assertNull(PickerKinds.resolve(null))
        assertNull(PickerKinds.resolve(""))
        assertNull(PickerKinds.resolve("calculator"))
        assertNull(PickerKinds.resolve("Apps"))
    }


    @Test
    fun pickerRequestKinds() {
        assertEquals(PickerKinds.APPS, PickerKinds.forRequest(PickerRequest.APPS))
        assertEquals(PickerKinds.UTILITY, PickerKinds.forRequest(PickerRequest.UTILITY))
        assertNull(PickerKinds.forRequest(null))
        assertNull(PickerKinds.forRequest("settings"))

        listOf(PickerRequest.APPS, PickerRequest.UTILITY).forEach {
            assertEquals(PickerKinds.forRequest(it), PickerKinds.resolve(PickerKinds.forRequest(it)))
        }
    }

}

class QuickPresetsSideTest {
    private val screens = listOf(360f, 392f, 412f)
    private val holes = listOf(24f, 30f, 52f, 70f)

    private fun valueRoom(plan: PresetPlan, hole: CutoutArea, screen: Float): Float? {
        val side = SideCamera.sideOf(hole.centreXDp)
        val width = plan.fit.widthDp
        val left = SideCamera.desiredLeftDp(side, screen, hole.centreXDp, hole.widthDp, width, plan.fit.xOffsetDp)
            .coerceIn(QuickPresets.EDGE_MARGIN_DP, screen - QuickPresets.EDGE_MARGIN_DP - width)
        val centre = cameraCentreInPill(screen / 2f, hole.centreXDp, left + width / 2f, width)!!
        return SideCamera.zone(side, width, centre, hole.widthDp)!!.plan.valueMaxWidthDp
    }

    @Test
    fun presetHoleAnchor() {
        for (screen in screens) for (hw in holes) {
            val left = QuickPresets.holeCentreFor(PresetPlace.Left, screen, hw)
            val right = QuickPresets.holeCentreFor(PresetPlace.Right, screen, hw)
            assertEquals(-right, left, 0.001f)
            assertTrue(screen / 2f + left - hw / 2f >= 0f)
            assertEquals(QuickPresets.EDGE_MARGIN_DP, screen / 2f + left - SideCamera.bandWidthDp(hw) / 2f, 0.001f)
        }

        assertEquals(0f, QuickPresets.holeCentreFor(PresetPlace.Centre, 392f, 30f), 0.001f)
    }

    @Test fun automaticHoleIsNeverMoved() {
        val hole = CutoutArea(-150f, 10f, 30f, 30f)
        for (place in PresetPlace.values()) {
            assertNull(QuickPresets.plan(392f, hole, 105f, place, manualHole = false).hole)
        }
    }

    @Test fun manualCornerPreset() {
        val hole = CutoutArea(0f, 14f, 30f, 26f)
        val left = QuickPresets.plan(392f, hole, 105f, PresetPlace.Left, manualHole = true).hole
        val right = QuickPresets.plan(392f, hole, 105f, PresetPlace.Right, manualHole = true).hole
        assertNotNull(left)
        assertNotNull(right)
        assertTrue(left!!.centreXDp < -SideCamera.THRESHOLD_DP)
        assertTrue(right!!.centreXDp > SideCamera.THRESHOLD_DP)
        assertEquals(14f, left.topDp, 0.001f)
        assertEquals(30f, left.widthDp, 0.001f)
        assertEquals(26f, right.heightDp, 0.001f)
    }

    @Test fun centrePresetRestoresHole() {
        val hole = CutoutArea(-150f, 10f, 30f, 30f)
        val plan = QuickPresets.plan(392f, hole, 112f, PresetPlace.Centre, manualHole = true)
        assertEquals(0f, plan.hole!!.centreXDp, 0.001f)
        assertEquals(QuickPresets.fit(392f, plan.hole, 112f, PresetPlace.Centre), plan.fit)
    }

    @Test fun sidePlanKeepsZeroOffset() {
        for (screen in screens) for (hw in holes) for (sign in listOf(-1f, 1f)) {
            val hole = CutoutArea(sign * (screen / 2f - 26f), 10f, hw, 30f)
            val place = if (sign < 0f) PresetPlace.Left else PresetPlace.Right
            val plan = QuickPresets.plan(screen, hole, 105f, place, manualHole = false)
            assertEquals(0f, plan.fit.xOffsetDp, 0.001f)
            assertTrue("$screen $hw $sign", valueRoom(plan, hole, screen)!! >= QuickPresets.VALUE_MIN_DP)
        }
    }

    @Test fun sidePlanMirroredRight() {
        val left = QuickPresets.plan(392f, CutoutArea(-170f, 10f, 30f, 30f), 105f, PresetPlace.Left, manualHole = false)
        val right = QuickPresets.plan(392f, CutoutArea(170f, 10f, 30f, 30f), 105f, PresetPlace.Right, manualHole = false)
        assertEquals(left.fit.widthDp, right.fit.widthDp, 0.001f)
    }

    @Test fun sidePlanStaysInRange() {
        val hole = CutoutArea(-170f, 10f, 30f, 30f)
        val plan = QuickPresets.plan(392f, hole, 150f, PresetPlace.Left, manualHole = false)
        assertTrue(plan.fit.widthDp >= 150f)
        val capped = QuickPresets.plan(392f, hole, 105f, PresetPlace.Left, manualHole = false, maxWidthDp = 110f)
        assertTrue(capped.fit.widthDp <= 110f)
    }
}

class QuickPresetsTest {
    private fun valueRoom(width: Float, x: Float, screen: Float, hole: CutoutArea): Float? {
        val left = (screen / 2f + x - width / 2f).coerceIn(QuickPresets.EDGE_MARGIN_DP, screen - QuickPresets.EDGE_MARGIN_DP - width)
        val centre = cameraCentreInPill(screen / 2f, hole.centreXDp, left + width / 2f, width)
        return CollapsedSlots.plan(width, centre, hole.widthDp).valueMaxWidthDp
    }

    @Test
    fun quickPresetBounds() {
        val missingFit = QuickPresets.fit(392f, null, 105f, PresetPlace.Left)
        assertEquals(105f, missingFit.widthDp, 0.001f)
        assertTrue(missingFit.xOffsetDp < 0f)

        val hole = CutoutArea(0f, 10f, 30f, 30f)
        val centredFit = QuickPresets.fit(392f, hole, 112f, PresetPlace.Centre)
        assertEquals(0f, centredFit.xOffsetDp, 0.001f)
        assertTrue(valueRoom(centredFit.widthDp, centredFit.xOffsetDp, 392f, hole)!! >= QuickPresets.VALUE_MIN_DP)

        for (screen in listOf(360f, 392f, 412f)) for (hw in listOf(24f, 30f, 52f)) {
            val cornerLeft = CutoutArea(-(screen / 2f - 26f), 10f, hw, 30f)
            val cornerRight = CutoutArea(screen / 2f - 26f, 10f, hw, 30f)
            val lf = QuickPresets.fit(screen, cornerLeft, 105f, PresetPlace.Left)
            val rf = QuickPresets.fit(screen, cornerRight, 105f, PresetPlace.Right)
            assertTrue("left $screen $hw", valueRoom(lf.widthDp, lf.xOffsetDp, screen, cornerLeft)!! >= QuickPresets.VALUE_MIN_DP)
            assertTrue("right $screen $hw", valueRoom(rf.widthDp, rf.xOffsetDp, screen, cornerRight)!! >= QuickPresets.VALUE_MIN_DP)
            assertTrue(lf.widthDp <= QuickPresets.MAX_PILL_WIDTH_DP && rf.widthDp <= QuickPresets.MAX_PILL_WIDTH_DP)
        }

        val cappedFit = QuickPresets.fit(392f, CutoutArea(-170f, 10f, 120f, 30f), 105f, PresetPlace.Left)
        assertTrue(cappedFit.widthDp <= QuickPresets.MAX_PILL_WIDTH_DP)

        val wideScreen = 600f
        val wideLeft = QuickPresets.fit(wideScreen, null, 105f, PresetPlace.Left)
        val wideRight = QuickPresets.fit(wideScreen, null, 105f, PresetPlace.Right)
        // 600 / 2 - 105 / 2 - 8 = 239.5: past the old 140 dp cap
        assertEquals(-239.5f, wideLeft.xOffsetDp, 0.01f)
        assertEquals(239.5f, wideRight.xOffsetDp, 0.01f)
    }
}

class SelectionActionsTest {
    @Test fun textIsTrimmedAndCapped() {
        assertEquals("hello", SelectionActions.clean("  hello \n"))
        assertEquals(SelectionActions.MAX_CHARS, SelectionActions.clean("a".repeat(SelectionActions.MAX_CHARS + 50)).length)
    }
}

class SelectionBarPlacementTest {
    private fun place(selTop: Int, selBottom: Int, centreX: Int = 500) = SelectionBarPlacement.place(
        selectionCentreX = centreX, selectionTop = selTop, selectionBottom = selBottom,
        barWidth = 400, barHeight = 100,
        areaLeft = 100, areaTop = 200, areaRight = 900, areaBottom = 800,
        gap = 20, edge = 10
    )

    @Test
    fun selectionBarBounds() {
        assertEquals(SelectionBarPlacement.Spot(300, 380), place(selTop = 500, selBottom = 560))

        assertEquals(SelectionBarPlacement.Spot(300, 380), place(selTop = 300, selBottom = 360))

        // 150 would fit in the window, but it lies above the card and outside the touchable area
        assertEquals(SelectionBarPlacement.Spot(300, 420), place(selTop = 250, selBottom = 400))

        assertEquals(110, place(selTop = 500, selBottom = 560, centreX = 120).x)
        assertEquals(490, place(selTop = 500, selBottom = 560, centreX = 880).x)

        assertEquals(210, place(selTop = 200, selBottom = 800).y)
    }
}

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

    @Test fun shadeCastPowerUtilsGone() {
        val ids = UtilityType.entries.map { it.id }.toSet()
        listOf("cast", "notification_shade", "power_menu").forEach { assertFalse(it, it in ids) }
        // A layout saved with them keeps everything else
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

    @Test fun widgetsRangeOneToWholeGrid() {
        assertTrue(widget(1, 0, 0, rows = 1, cols = 1).isValid)
        assertTrue(widget(1, 0, 0, rows = 2, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 3, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 2, cols = 5).isValid)
        assertFalse(widget(1, 1, 0, rows = 2, cols = 1).isValid)
        assertFalse(widget(1, 0, 0, rows = 0, cols = 1).isValid)
    }

    @Test fun widgetNeedsItsId() {
        assertFalse(UtilityPlacement(UtilityType.AppWidget, 0, 0).isValid)
    }

    @Test fun overlapIsDetected() {
        assertTrue(widget(1, 0, 0, 2, 2).overlaps(tile(UtilityType.WiFi, 1, 1)))
        assertFalse(widget(1, 0, 0, 2, 1).overlaps(tile(UtilityType.WiFi, 0, 1)))
        assertFalse(tile(UtilityType.WiFi, 0, 0).overlaps(tile(UtilityType.Hotspot, 1, 0)))
    }

    @Test fun addRefusesOverlapDupOutside() {
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
        // A two-row widget at row 1 would sit across the 2 tabs
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
        // A 2x2 widget no longer fits the first tab, so it goes to the second
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

    @Test fun savedLayoutMarkedCurrentGrid() {
        assertTrue(UtilityLayout.encode(listOf(tile(UtilityType.WiFi, 1, 3))).startsWith("v2:"))
        assertEquals("", UtilityLayout.encode(emptyList()))
        // A marked layout is read as it is, not turned on its side
        val p = UtilityLayout.decode("v2:wifi,1,3,1,1,-1").single()
        assertEquals(1, p.row)
        assertEquals(3, p.col)
    }

    @Test fun firstGridLayoutTurned() {
        // first grid: 4 rows by 2 columns. Wi-Fi was in row 0, column 1; the widget started at row 1, column 0 and was 3 rows by 2
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

    @Test fun firstGridCellsHavePlace() {
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

    @Test fun loadForcesFixedUtilsOneCell() {
        val p = UtilityLayout.decode("wifi,0,0,2,2,-1").single()
        assertEquals(1, p.rowSpan)
        assertEquals(1, p.colSpan)
    }

    @Test fun loadingDropsAWidgetWithoutAnId() {
        assertEquals(emptyList<UtilityPlacement>(), UtilityLayout.decode("app_widget,0,0,1,1,-1"))
    }

    @Test fun retiredTileMigration() {
        val layout = UtilityLayout.decode("sim_switcher,0,0,1,1,-1;wifi,0,1,1,1,-1")
        assertEquals(listOf(UtilityType.WiFi), layout.map { it.type })
    }
    @Test
    fun utilityDropTargets() {
        val freeCellLayout = listOf(tile(UtilityType.WiFi, 0, 0))
        assertEquals(listOf(tile(UtilityType.WiFi, 1, 3)), UtilityLayout.drop(freeCellLayout, "wifi", 1, 3))
        assertEquals(freeCellLayout, UtilityLayout.drop(freeCellLayout, "wifi", 0, 0))
        assertEquals(freeCellLayout, UtilityLayout.drop(freeCellLayout, "missing", 1, 1))

        val swappedLayout = listOf(tile(UtilityType.WiFi, 0, 0), tile(UtilityType.Hotspot, 1, 2))
        val swappedOut = UtilityLayout.drop(swappedLayout, "wifi", 1, 2)
        assertEquals(tile(UtilityType.WiFi, 1, 2), swappedOut.first { it.key == "wifi" })
        assertEquals(tile(UtilityType.Hotspot, 0, 0), swappedOut.first { it.key == "hotspot" })

        // A two-wide widget cannot take the one-cell spot next to the edge of the grid, so the tile cannot trade with it
        val unchangedLayout = listOf(tile(UtilityType.WiFi, 0, 3), widget(7, 0, 1, rows = 1, cols = 2))
        assertEquals(unchangedLayout, UtilityLayout.drop(unchangedLayout, "wifi", 0, 1))
        // landing across two items is not a trade either
        val two = listOf(widget(7, 0, 0, rows = 1, cols = 2), tile(UtilityType.WiFi, 0, 2), tile(UtilityType.Hotspot, 0, 3))
        assertEquals(two, UtilityLayout.drop(two, "widget:7", 0, 2))

        val widgetLayout = listOf(tile(UtilityType.WiFi, 0, 0), widget(7, 1, 0, rows = 1, cols = 2))
        val widgetOut = UtilityLayout.drop(widgetLayout, "wifi", 1, 1)
        assertEquals(tile(UtilityType.WiFi, 1, 0), widgetOut.first { it.key == "wifi" })
        assertEquals(widget(7, 0, 0, rows = 1, cols = 2), widgetOut.first { it.key == "widget:7" })
    }

    @Test fun gridTabsKeepWidgetsOnOneTab() {
        assertEquals(2, UtilityGrid.TABS)
        assertEquals(4, UtilityGrid.TOTAL_ROWS)
        assertEquals(0, tile(UtilityType.WiFi, 1, 0).tab)
        assertEquals(1, tile(UtilityType.WiFi, 2, 0).tab)
        assertEquals(1, UtilityGrid.localRow(3))
        assertFalse(widget(1, 1, 0, rows = 2, cols = 1).isValid)
        assertTrue(widget(1, 2, 0, rows = 2, cols = 4).isValid)
        assertFalse(widget(1, 0, 0, rows = 3, cols = 1).isValid)
    }

    private fun fullFirstTab() = (0 until 2).flatMap { r -> (0 until 4).map { c -> r to c } }
        .zip(UtilityType.singles.take(8)).map { (cell, type) -> tile(type, cell.first, cell.second) }

    @Test fun secondTabNeedsFullFirst() {
        assertEquals(1, UtilityLayout.tabCount(emptyList()))
        val full = fullFirstTab()
        assertEquals(8, full.size)
        assertTrue(UtilityLayout.isTabFull(full, 0))
        assertEquals(2, UtilityLayout.tabCount(full))
        // one cell short: still a single page, even with things placed on the second tab
        val short = full.drop(1) + tile(UtilityType.Camera, 2, 0)
        assertFalse(UtilityLayout.isTabFull(short, 0))
        assertEquals(1, UtilityLayout.tabCount(short))
    }

    @Test fun widgetCountsTowardAFullFirstTab() {
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
        // y is measured from the top of the item's tab
        assertEquals(3 to 0, UtilityLayout.snapCell(second, 0f, 64f, 80f, 64f))
        assertEquals(3 to 3, UtilityLayout.snapCell(second, 5000f, 5000f, 80f, 64f))
        assertEquals(2 to 0, UtilityLayout.snapCell(second, -300f, -300f, 80f, 64f))
    }

    @Test fun snapCellRoundsNearest() {
        val item = tile(UtilityType.WiFi, 0, 0)
        assertEquals(0 to 1, UtilityLayout.snapCell(item, 40f, 10f, 80f, 64f))
        assertEquals(1 to 3, UtilityLayout.snapCell(item, 5000f, 5000f, 80f, 64f))
        assertEquals(0 to 0, UtilityLayout.snapCell(item, -300f, -300f, 80f, 64f))
        val wide = widget(7, 0, 0, rows = 1, cols = 2)
        assertEquals(0 to 2, UtilityLayout.snapCell(wide, 5000f, 0f, 80f, 64f))
    }
}

class UtilityPickerCellTest {
    private fun fill(cells: List<Pair<Int, Int>>): List<UtilityPlacement> =
        cells.mapIndexed { i, (r, c) -> UtilityPlacement(UtilityType.AppWidget, r, c, widgetId = i + 1) }

    @Test
    fun firstVacantCell() {
        assertEquals(0 to 0, firstEmptyUtilityCell(emptyList()))

        val layout = fill(listOf(0 to 0, 0 to 1, 0 to 3))
        assertEquals(0 to 2, firstEmptyUtilityCell(layout))

        val firstTabFullCells = (0 until UtilityGrid.ROWS).flatMap { r -> (0 until UtilityGrid.COLUMNS).map { c -> r to c } }
        assertEquals(UtilityGrid.ROWS to 0, firstEmptyUtilityCell(fill(firstTabFullCells)))

        val wide = UtilityPlacement(UtilityType.AppWidget, 0, 0, rowSpan = 1, colSpan = 2, widgetId = 1)
        assertEquals(0 to 2, firstEmptyUtilityCell(listOf(wide)))

        val fullGridCells = (0 until UtilityGrid.TOTAL_ROWS).flatMap { r -> (0 until UtilityGrid.COLUMNS).map { c -> r to c } }
        assertNull(firstEmptyUtilityCell(fill(fullGridCells)))
    }
}
