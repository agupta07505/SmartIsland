/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import androidx.compose.ui.graphics.Color
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.expanded.IslandPalette
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush
import com.agupta07505.smartisland.ui.expanded.expandedSurfaceColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDischargeTest {
    private val hour = 3_600_000L

    @Test fun currentReadingsGiveHoursLeft() {
        // 2000 mAh left at a 500 mA draw (500000 uA) = 4 hours
        assertEquals(4 * hour, WidgetIslandContent.dischargeRemainingMs(2_000_000L, -500_000L))
        assertEquals(4 * hour, WidgetIslandContent.dischargeRemainingMs(2_000_000L, 500L))
        assertEquals(
        WidgetIslandContent.dischargeRemainingMs(1_000_000L, 250_000L),
        WidgetIslandContent.dischargeRemainingMs(1_000_000L, -250_000L)
        )
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(0L, 500_000L))
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(2_000_000L, 0L))
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(2_000_000L, Long.MIN_VALUE))
        // A tiny draw would promise weeks: not a forecast
        assertEquals(-1L, WidgetIslandContent.dischargeRemainingMs(4_000_000L, 1L))
    }
}

class DeviceStatesTest {
    private fun mode(kind: ModeKind, name: String = DndModes.defaultName(kind), filter: Int = 0) =
        ModeStatus(true, kind, name, fromRule = true, filter = filter)

    @Test
    fun airplaneSaysOnAndOff() {
        val on = DeviceStates.airplane(true)
        val off = DeviceStates.airplane(false)
        assertEquals(IslandEvent.AIRPLANE_MODE, on.event)
        assertEquals("On", on.text)
        assertEquals("Off", off.text)
        assertFalse(DeviceStates.isOff(on.category))
        assertTrue(DeviceStates.isOff(off.category))
    }

    @Test
    fun eachRingerModeHasItsOwnEvent() {
        assertEquals(IslandEvent.RING, DeviceStates.ringer(DeviceStates.RINGER_MODE_NORMAL)?.event)
        assertEquals(IslandEvent.VIBRATE, DeviceStates.ringer(DeviceStates.RINGER_MODE_VIBRATE)?.event)
        assertEquals(IslandEvent.SILENT, DeviceStates.ringer(DeviceStates.RINGER_MODE_SILENT)?.event)
        assertNull(DeviceStates.ringer(-1))
    }

    @Test
    fun deviceStateKinds() {
        val dnd = DeviceStates.change(ModeStatus.Off, DndModes.fromFilter(DndModes.FILTER_PRIORITY))!!
        assertEquals(IslandEvent.DND, dnd.event)
        assertEquals(ModeKind.Dnd, DeviceStates.kindOf(dnd.category))
        val driving = DeviceStates.change(ModeStatus.Off, mode(ModeKind.Driving))!!
        assertEquals(IslandEvent.MODE, driving.event)
        assertEquals(ModeKind.Driving, DeviceStates.kindOf(driving.category))
        assertEquals("Driving", driving.title)
        assertEquals("Mode is on", driving.text)

        val silence = DeviceStates.change(ModeStatus.Off, DndModes.fromFilter(DndModes.FILTER_NONE))!!
        val alarms = DeviceStates.change(ModeStatus.Off, DndModes.fromFilter(DndModes.FILTER_ALARMS))!!
        assertEquals(IslandEvent.DND, silence.event)
        assertEquals(ModeKind.TotalSilence, DeviceStates.kindOf(silence.category))
        assertEquals(ModeKind.AlarmsOnly, DeviceStates.kindOf(alarms.category))

        assertFalse(DeviceStates.isDeviceState(null))
        assertFalse(DeviceStates.isDeviceState("wifi"))
        assertFalse(DeviceStates.isDeviceState("usb_connected"))
        assertNull(DeviceStates.kindOf("ringer_ring"))
        assertFalse(DeviceStates.isOff("wifi_disconnected"))
    }

    @Test
    fun modeChangeEvents() {
        val off = DeviceStates.change(mode(ModeKind.Bedtime), ModeStatus.Off)!!
        assertEquals(IslandEvent.MODE, off.event)
        assertEquals(ModeKind.Bedtime, DeviceStates.kindOf(off.category))
        assertTrue(DeviceStates.isOff(off.category))
        assertEquals("Off", off.text)
        val dndOff = DeviceStates.change(DndModes.fromFilter(DndModes.FILTER_PRIORITY), ModeStatus.Off)!!
        assertEquals(IslandEvent.DND, dndOff.event)
        assertTrue(DeviceStates.isOff(dndOff.category))

        assertNull(DeviceStates.change(ModeStatus.Off, ModeStatus.Off))
        assertNull(DeviceStates.change(mode(ModeKind.Driving), mode(ModeKind.Driving)))

        assertNull(DeviceStates.change(mode(ModeKind.Work, filter = 0), mode(ModeKind.Work, filter = DndModes.FILTER_PRIORITY)))

        val replacementCard = DeviceStates.change(mode(ModeKind.Work), mode(ModeKind.Driving))!!
        assertEquals(ModeKind.Driving, DeviceStates.kindOf(replacementCard.category))
        assertFalse(DeviceStates.isOff(replacementCard.category))

        val blankCard = DeviceStates.change(ModeStatus.Off, mode(ModeKind.Theater, name = ""))!!
        assertEquals("Theater", blankCard.title)
    }
}

class DndModesTest {
    @Test
    fun kindsAreToldFromNames() {
        assertEquals(ModeKind.Bedtime, DndModes.kindFromName("Bedtime mode"))
        assertEquals(ModeKind.Bedtime, DndModes.kindFromName("Sleeping"))
        assertEquals(ModeKind.Driving, DndModes.kindFromName("Driving"))
        assertEquals(ModeKind.Driving, DndModes.kindFromName("Drive mode"))
        assertEquals(ModeKind.Theater, DndModes.kindFromName("Theatre"))
        assertEquals(ModeKind.Gaming, DndModes.kindFromName("Game Mode"))
        assertEquals(ModeKind.Work, DndModes.kindFromName("Work"))
        assertEquals(ModeKind.Dnd, DndModes.kindFromName("Do not disturb"))
        assertEquals(ModeKind.Custom, DndModes.kindFromName("Yoga"))
        assertEquals(ModeKind.Custom, DndModes.kindFromName(""))
    }

    @Test
    fun modeNameFallback() {
        fun status(kind: ModeKind) = ModeStatus(true, kind, DndModes.defaultName(kind), fromRule = true)
        assertEquals(ModeKind.Driving, DndModes.pick(listOf(status(ModeKind.Bedtime), status(ModeKind.Driving)))?.kind)
        assertEquals(ModeKind.Bedtime, DndModes.pick(listOf(status(ModeKind.Dnd), status(ModeKind.Bedtime)))?.kind)
        assertEquals(ModeKind.Custom, DndModes.pick(listOf(status(ModeKind.Dnd), status(ModeKind.Custom)))?.kind)
        assertNull(DndModes.pick(emptyList()))

        assertEquals(ModeKind.values().size, ModeKind.values().map { DndModes.rank(it) }.toSet().size)
        ModeKind.values().forEach { assertTrue(DndModes.defaultName(it).isNotBlank()) }
    }

    @Test
    fun blankNameFallsBackToTheKindsName() {
        assertEquals("Bedtime", DndModes.withName(ModeKind.Bedtime, null))
        assertEquals("Bedtime", DndModes.withName(ModeKind.Bedtime, "  "))
        assertEquals("Sleep time", DndModes.withName(ModeKind.Bedtime, " Sleep time "))
    }

    @Test
    fun detailLineSaysWhatPassesThrough() {
        assertEquals("Mode is on", DndModes.detail(ModeStatus(true, ModeKind.Bedtime, "Bedtime", fromRule = true)))
        assertEquals("Only alarms can interrupt", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_ALARMS)))
        assertEquals("Nothing can interrupt", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_NONE)))
        assertEquals("Only priority interruptions", DndModes.detail(DndModes.fromFilter(DndModes.FILTER_PRIORITY)))
    }
}

class FloatingWindowModeTest {
    @Test
    fun floatingRouteHonorsSwitchAndMode() {
        assertEquals(FloatingWindowRoute.Off, floatingWindowRoute(false, FloatingWindowMode.ID_LMO))
        assertEquals(FloatingWindowRoute.Off, floatingWindowRoute(false, FloatingWindowMode.ID_SYSTEM))
        assertEquals(FloatingWindowRoute.System, floatingWindowRoute(true, FloatingWindowMode.ID_SYSTEM))
        assertEquals(FloatingWindowRoute.Lmo, floatingWindowRoute(true, FloatingWindowMode.ID_LMO))
        assertEquals(FloatingWindowRoute.System, floatingWindowRoute(true, "nonsense"))
        assertEquals(FloatingWindowRoute.System, floatingWindowRoute(true, null))
    }

    @Test
    fun defaultsAreOnAndSystem() {
        val d = SmartIslandSettings.Default
        assertTrue(d.floatingWindowEnabled)
        assertEquals(FloatingWindowMode.ID_SYSTEM, d.floatingWindowMode)
    }

    @Test
    fun settingsSurviveBackupAndBadValuesAreRepaired() {
        val s = SmartIslandSettings(floatingWindowEnabled = false, floatingWindowMode = FloatingWindowMode.ID_LMO)
        val back = SmartIslandSettings.fromJson(s.toJson(appVersion = "1"))
        assertEquals(false, back.floatingWindowEnabled)
        assertEquals(FloatingWindowMode.ID_LMO, back.floatingWindowMode)
        val bad = SmartIslandSettings.fromJson(s.copy(floatingWindowMode = "x").toJson(appVersion = "1"))
        assertEquals(FloatingWindowMode.ID_SYSTEM, bad.floatingWindowMode)
    }
}

/** Foldable recognition by model, posture from the screen's size */
class FoldPostureTest {
    private fun kind(maker: String, model: String, device: String = "", hinge: Boolean = false) =
        FoldModels.kindOf(maker, model, device, hinge).kind

    @Test
    fun foldableModels() {
        val cases = listOf(
            "SM-F946B" to FoldKind.Book,
            "SM-F966U1" to FoldKind.Book,
            "SM-F741B" to FoldKind.Flip,
            "SM-F711N" to FoldKind.Flip,
            "SM-W2024" to FoldKind.Book,
            "SM-S918B" to FoldKind.None
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, kind("samsung", text))
        }

        assertEquals(FoldKind.Book, kind("Google", "Pixel Fold"))
        assertEquals(FoldKind.Book, kind("Google", "Pixel 9 Pro Fold"))
        assertEquals(FoldKind.Flip, kind("motorola", "motorola razr 40 ultra"))
        assertEquals(FoldKind.Flip, kind("motorola", "razr+ 2024"))
        assertEquals(FoldKind.Book, kind("OPPO", "OPPO Find N3"))
        assertEquals(FoldKind.Flip, kind("OPPO", "OPPO Find N3 Flip"))
        assertEquals(FoldKind.Book, kind("HONOR", "Magic V3"))
        assertEquals(FoldKind.Book, kind("HUAWEI", "HUAWEI Mate X5"))
        assertEquals(FoldKind.Flip, kind("Xiaomi", "Xiaomi MIX Flip"))
        assertEquals(FoldKind.Book, kind("Xiaomi", "Xiaomi MIX Fold 3"))
        assertEquals(FoldKind.Book, kind("vivo", "vivo X Fold3"))
    }

    @Test
    fun hingeDetection() {
        assertEquals(FoldKind.None, kind("Google", "Pixel 9 Pro"))
        assertFalse(FoldModels.kindOf("OnePlus", "CPH2581", "", false).foldable)

        val info = FoldModels.kindOf("Acme", "ZZ-100", "zz", true)
        assertEquals(FoldKind.Other, info.kind)
        assertTrue(info.foldable)
    }

    @Test
    fun postureUsesDeviceTypeAndScreenSize() {
        assertEquals(FoldPosture.FoldedPortrait, FoldPostureRules.postureOf(FoldKind.Book, 360f, 800f))
        assertEquals(FoldPosture.FoldedLandscape, FoldPostureRules.postureOf(FoldKind.Book, 800f, 360f))
        assertEquals(FoldPosture.UnfoldedPortrait, FoldPostureRules.postureOf(FoldKind.Book, 673f, 841f))
        assertEquals(FoldPosture.UnfoldedLandscape, FoldPostureRules.postureOf(FoldKind.Book, 841f, 673f))
        assertEquals(FoldPosture.FoldedPortrait, FoldPostureRules.postureOf(FoldKind.Flip, 240f, 250f))
        assertEquals(FoldPosture.FoldedPortrait, FoldPostureRules.postureOf(FoldKind.Flip, 384f, 390f))
        assertEquals(FoldPosture.UnfoldedPortrait, FoldPostureRules.postureOf(FoldKind.Flip, 412f, 915f))
        assertEquals(FoldPosture.UnfoldedLandscape, FoldPostureRules.postureOf(FoldKind.Flip, 915f, 412f))
        assertEquals(FoldPosture.UnfoldedPortrait, FoldPostureRules.postureOf(FoldKind.Other, 600f, 720f))
        assertEquals(FoldPosture.FoldedPortrait, FoldPostureRules.postureOf(FoldKind.Other, 380f, 820f))
        assertEquals(FoldPosture.Standard, FoldPostureRules.postureOf(FoldKind.None, 412f, 915f))
        assertEquals(FoldPosture.Standard, FoldPostureRules.postureOf(FoldKind.Book, 0f, 0f))
    }

    @Test
    fun landscapeCameraInheritance() {
        assertEquals(FoldPosture.FoldedPortrait, FoldPosture.FoldedLandscape.portraitTwin)
        assertEquals(FoldPosture.UnfoldedPortrait, FoldPosture.UnfoldedLandscape.portraitTwin)
        FoldPosture.entries.filter { !it.landscape }.forEach { assertEquals(it, it.portraitTwin) }
    }
}

class FullscreenRulesTest {
    private val w = 1080
    private val h = 2400

    @Test
    fun statusBarWindowTypes() {
        assertTrue(FullscreenRules.isStatusBar(SystemWindowBounds(0, 0, 1080, 80), w, h))
        assertTrue(FullscreenRules.isStatusBar(SystemWindowBounds(0, 0, 2400, 80), 2400, 1080))

        val others = mapOf(
            "narrow sidebar" to SystemWindowBounds(0, 0, 300, 80),
            "window lower on screen" to SystemWindowBounds(0, 2300, 1080, 2400),
            "tall system window" to SystemWindowBounds(0, 0, 1080, 1200)
        )
        for ((name, bounds) in others) assertFalse(name, FullscreenRules.isStatusBar(bounds, w, h))
    }

    @Test
    fun systemPanelWindows() {
        val shade = SystemWindowBounds(0, 0, 1080, 1200, active = true)
        assertTrue(FullscreenRules.isSystemPanel(shade, w, h))
        assertTrue(FullscreenRules.systemUiPresent(listOf(shade), w, h))

        val idle = SystemWindowBounds(0, 0, 1080, 2400, active = false)
        assertFalse(FullscreenRules.isSystemPanel(idle, w, h))
        assertFalse(FullscreenRules.systemUiPresent(listOf(idle), w, h))

        assertFalse(FullscreenRules.isSystemPanel(SystemWindowBounds(0, 0, 1080, 80, active = true), w, h))
        assertFalse(FullscreenRules.isSystemPanel(SystemWindowBounds(0, 0, 300, 1200, active = true), w, h))
    }


    @Test
    fun layoutEligibility() {
        assertTrue(FullscreenRules.eligibleLayout(false, LandscapeLayoutStyle.CLASSIC))
        assertTrue(FullscreenRules.eligibleLayout(true, LandscapeLayoutStyle.EDGE))
        assertFalse(FullscreenRules.eligibleLayout(true, LandscapeLayoutStyle.CLASSIC))
    }

    @Test
    fun hiddenRules() {
        assertTrue(FullscreenRules.hidden(true, false, false, false))
        assertFalse(FullscreenRules.hidden(false, false, false, false))
        assertFalse(FullscreenRules.hidden(true, true, false, false))
        assertFalse(FullscreenRules.hidden(true, false, true, false))
        assertFalse(FullscreenRules.hidden(true, false, false, true))
    }

    @Test
    fun aPeekEndsOnlyWhenNoCardIsOpenAndNoPulseRuns() {
        assertTrue(FullscreenRules.peekCanEnd(expanded = false, pulseBusy = false))
        assertFalse(FullscreenRules.peekCanEnd(expanded = true, pulseBusy = false))
        assertFalse(FullscreenRules.peekCanEnd(expanded = false, pulseBusy = true))
        assertFalse(FullscreenRules.peekCanEnd(expanded = true, pulseBusy = true))
    }

    @Test
    fun fullscreenStartsOnceBarSeen() {
        val t = FullscreenTracker()
        assertFalse(t.update(true, 0))
        assertTrue(t.update(false, 400))
    }

    @Test
    fun neverSeenBarNeverMeansFullscreen() {
        val t = FullscreenTracker()
        assertFalse(t.update(false, 0))
        assertFalse(t.update(false, 5000))
    }

    @Test
    fun barBackLeavesFullscreenAtOnce() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(false, 400)
        assertEquals(0L, FullscreenRules.EXIT_MS)
        assertFalse(t.update(true, 1000))
    }

    @Test
    fun fullscreenReturnsWhenBarGoes() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(false, 400)
        assertFalse(t.update(true, 800))
        // the bar went away again (a swipe reveal ended): the app is fullscreen again
        assertTrue(t.update(false, 1900))
        assertFalse(t.update(true, 2000))
    }

    @Test
    fun failedReadingKeepsTheAnswer() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(false, 400)
        assertTrue(t.update(null, 800))
    }

    @Test
    fun rotationHoldKeepsTheAnswerAndSkipsAbsentBar() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.hold(1000 + FullscreenRules.ROTATION_HOLD_MS)
        assertFalse(t.update(false, 1100))
        assertFalse(t.update(false, 1500))
        assertTrue(t.update(false, 1000 + FullscreenRules.ROTATION_HOLD_MS))
    }

    @Test
    fun resetClearsFullscreen() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(false, 400)
        t.reset()
        assertEquals(false, t.fullscreen)
    }

    @Test
    fun barBackLeavesAtOnceOverTheSameAppOrAnotherOne() {
        val t = FullscreenTracker()
        t.update(true, 0, appKey = 1)
        assertTrue(t.update(false, 400, appKey = 1))
        assertFalse(t.update(true, 500, appKey = 1))
        assertTrue(t.update(false, 600, appKey = 1))
        assertFalse(t.update(true, 900, appKey = 2))
    }

    @Test
    fun barSeenDuringRotationHoldLeavesAtOnce() {
        val t = FullscreenTracker()
        t.update(true, 0, appKey = 1)
        t.update(false, 400, appKey = 1)
        t.hold(500 + FullscreenRules.ROTATION_HOLD_MS)
        assertFalse(t.update(true, 600, appKey = 1))
    }

    @Test
    fun noExitIsLeftWaitingOnceTheBarIsBack() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(false, 400)
        assertEquals(null, t.exitRemainingMs(500))
        t.update(true, 1000)
        assertEquals(null, t.exitRemainingMs(1200))
    }

    @Test
    fun gapAfterPanelIsNotFullscreen() {
        // power menu: bar gone but a panel is in front, then the panel goes inactive before the bar is listed again
        val t = FullscreenTracker()
        t.update(true, 0)
        assertFalse(t.update(true, 400, panelPresent = true))
        assertFalse(t.update(false, 800))
        assertFalse(t.update(true, 1200))
    }

    @Test
    fun missingBarStillMeansFullscreenOnceTheGraceIsOver() {
        val t = FullscreenTracker()
        t.update(true, 0)
        t.update(true, 400, panelPresent = true)
        assertFalse(t.update(false, 400 + FullscreenRules.PANEL_GRACE_MS - 1))
        assertTrue(t.update(false, 400 + FullscreenRules.PANEL_GRACE_MS))
    }

    @Test
    fun shadeTakesPriorityOverFullscreenVisibility() {
        assertTrue(FullscreenRules.hiddenWithShade(false, false, false, false, shadeHides = true))
        assertTrue(FullscreenRules.hiddenWithShade(true, true, true, true, shadeHides = true))
        assertFalse(FullscreenRules.hiddenWithShade(false, false, false, false, shadeHides = false))
        assertTrue(FullscreenRules.hiddenWithShade(true, false, false, false, shadeHides = false))
        assertFalse(FullscreenRules.hiddenWithShade(true, true, false, false, shadeHides = false))
    }

}

class NetSpeedRulesTest {
    @Test fun normalizeKeepsChoicesAndFixesTheRest() {
        NetSpeedRules.CHOICES.forEach { assertEquals(it, NetSpeedRules.normalize(it)) }
        assertEquals(NetSpeedRules.DEFAULT_KBPS, NetSpeedRules.normalize(75))
        assertEquals(NetSpeedRules.DEFAULT_KBPS, NetSpeedRules.normalize(-1))
    }

    @Test
    fun speedVisibilityThresholds() {
        assertTrue(NetSpeedRules.shouldShow(NetSpeedRules.OFF, 0, 0, wasShown = false))
        assertFalse(NetSpeedRules.shouldShow(50, 49_999, 10_000, wasShown = false))
        assertTrue(NetSpeedRules.shouldShow(50, 50_000, 0, wasShown = false))
        assertTrue(NetSpeedRules.shouldShow(1000, 0, 1_000_000, wasShown = false))
        assertTrue(NetSpeedRules.shouldShow(200, 1_000, 250_000, wasShown = false))

        // 45 KB/s: under 50 but over 80% of it (40 KB/s), so a shown widget stays and a hidden one stays out
        assertTrue(NetSpeedRules.shouldShow(50, 45_000, 0, wasShown = true))
        assertFalse(NetSpeedRules.shouldShow(50, 45_000, 0, wasShown = false))
        assertFalse(NetSpeedRules.shouldShow(50, 39_999, 0, wasShown = true))
    }

    @Test fun directionFollowsTheLargerSide() {
        assertTrue(NetSpeedRules.isUpload(1_000, 2_000))
        assertFalse(NetSpeedRules.isUpload(2_000, 1_000))
        assertFalse(NetSpeedRules.isUpload(1_000, 1_000))
        assertEquals(2_000L, NetSpeedRules.busier(1_000, 2_000))
    }
}

class NetUsageTest {
    private val today = NetUsage.dayKey(2026, 9, 18)

    @Test fun dayKeyIsYyyyMmDd() = assertEquals(20261018, today)

    @Test fun splitGivesMobileShare() {
        assertEquals(700L to 300L, NetUsage.split(1_000, 300))
        // A mobile reading ahead of the total (the counters are read a moment apart) is clamped
        assertEquals(0L to 1_000L, NetUsage.split(1_000, 5_000))
        // A counter reset counts as nothing
        assertEquals(0L to 0L, NetUsage.split(-5, -5))
    }

    @Test fun addsUpWithinTheDayAndStartsAgainTomorrow() {
        var u = DailyUsage()
        u = NetUsage.add(u, today, 500, 100)
        u = NetUsage.add(u, today, 250, 0)
        assertEquals(DailyUsage(today, 750, 100), u)
        val tomorrow = NetUsage.dayKey(2026, 9, 19)
        u = NetUsage.add(u, tomorrow, 10, 20)
        assertEquals(DailyUsage(tomorrow, 10, 20), u)
    }

    @Test fun counterFollowsTheCurrentConnection() {
        val u = DailyUsage(today, wifiBytes = 2_000, mobileBytes = 300)
        assertEquals(2_000L, NetUsage.bytesFor(u, NetConnection.Wifi))
        assertEquals(300L, NetUsage.bytesFor(u, NetConnection.Mobile))
        assertEquals(2_000L, NetUsage.bytesFor(u, NetConnection.Other))
        assertEquals(0L, NetUsage.bytesFor(u, NetConnection.None))
    }

    @Test fun sizeTextUsesDecimalUnits() {
        assertEquals("850 KB", NetUsage.sizeText(850_000))
        assertEquals("340 MB", NetUsage.sizeText(340_000_000))
        assertEquals("1.2 GB", NetUsage.sizeText(1_200_000_000))
        assertEquals("12 GB", NetUsage.sizeText(12_400_000_000))
        assertEquals("0 KB", NetUsage.sizeText(-1))
    }

    @Test fun connectionModeFiltersTheWidget() {
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_ANY, NetConnection.None))
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.Wifi))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.Mobile))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_WIFI, NetConnection.None))
        assertTrue(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_MOBILE, NetConnection.Mobile))
        assertFalse(NetSpeedRules.connectionAllows(NetSpeedRules.CONN_MOBILE, NetConnection.Wifi))
        // anything unknown is the default (any connection)
        assertEquals(NetSpeedRules.CONN_ANY, NetSpeedRules.normalizeConnection("5G"))
        assertTrue(NetSpeedRules.connectionAllows("5G", NetConnection.Mobile))
    }
}

class OledBlackTest {
    private val tinted = SmartIslandSettings.Default.copy(pillColor = 0xFF1A2B3CL, expandedGradientEnabled = true)

    @Test fun offKeepsThePillColour() {
        assertEquals(0xFF1A2B3CL, tinted.islandSurfaceColor)
        assertNotNull(expandedGradientBrush(tinted, IslandPalette.Dark))
    }

    @Test fun onIsPureBlackAndDropsTheDarkGradient() {
        val oled = tinted.copy(oledBlack = true)
        assertEquals(0xFF000000L, oled.islandSurfaceColor)
        assertEquals(Color.Black, expandedSurfaceColor(oled, IslandPalette.Dark))
        assertNull(expandedGradientBrush(oled, IslandPalette.Dark))
    }

    @Test fun lightLookIsLeftAlone() {
        val oled = tinted.copy(oledBlack = true)
        assertEquals(IslandPalette.Light.surface, expandedSurfaceColor(oled, IslandPalette.Light))
        assertNotNull(expandedGradientBrush(oled, IslandPalette.Light))
    }

    @Test fun offByDefaultAndSurvivesABackup() {
        assertEquals(false, SmartIslandSettings.Default.oledBlack)
        val back = SmartIslandSettings.fromJson(SmartIslandSettings.Default.copy(oledBlack = true).toJson(appVersion = "7.0.0"))
        assertEquals(true, back.oledBlack)
    }
}
