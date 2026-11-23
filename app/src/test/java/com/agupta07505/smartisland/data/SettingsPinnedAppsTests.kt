/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedAppsTest {
    @Test
    fun pinnedAppReordering() {
        val reorderedList = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), PinnedApps.move(reorderedList, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), PinnedApps.move(reorderedList, 3, 0))

        val unchangedList = listOf("a", "b", "c")
        assertEquals(unchangedList, PinnedApps.move(unchangedList, 1, 1))
        assertEquals(unchangedList, PinnedApps.move(unchangedList, -1, 2))
        assertEquals(unchangedList, PinnedApps.move(unchangedList, 0, 3))
    }

    @Test fun moveOntoPutsTheAppInTheTargetsPlace() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "d", "a"), PinnedApps.moveOnto(list, "a", "d"))
        assertEquals(listOf("c", "a", "b", "d"), PinnedApps.moveOnto(list, "c", "a"))
        assertEquals(list, PinnedApps.moveOnto(list, "a", "missing"))
    }

    @Test fun swapTradesTwoPlacesAndLeavesTheRest() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "b", "a", "d"), PinnedApps.swap(list, "a", "c"))
        assertEquals(listOf("a", "b", "d", "c"), PinnedApps.swap(list, "d", "c"))
        assertEquals(list, PinnedApps.swap(list, "a", "a"))
        assertEquals(list, PinnedApps.swap(list, "a", "missing"))
        // A folder trades places like any other entry
        assertEquals(listOf(f("x", "y"), "b"), PinnedApps.swap(listOf("b", f("x", "y")), "b", f("x", "y")))
    }

    @Test
    fun pinnedAppCodec() {
        val list = listOf("com.z.app", "com.a.app", "com.m.app")
        assertEquals(list, PinnedApps.decode(PinnedApps.encode(list)))

        val many = (1..12).map { "pkg$it" }
        assertEquals(many.take(PinnedApps.MAX), PinnedApps.normalize(many))
        assertEquals(listOf("a", "b"), PinnedApps.normalize(listOf("a", " ", "b", "a", "")))
        assertEquals(emptyList<String>(), PinnedApps.decode(""))
        assertEquals(emptyList<String>(), PinnedApps.decode(null))
    }

    @Test fun legacySetUntilOrderSaved() {
        val legacy = linkedSetOf("x", "y")
        assertEquals(listOf("x", "y"), PinnedApps.restore(null, legacy))
        assertEquals(listOf("y", "x"), PinnedApps.restore("y\nx", legacy))
        // an empty saved order means the user unpinned everything; the old set must not come back
        assertEquals(emptyList<String>(), PinnedApps.restore("", legacy))
        assertEquals(emptyList<String>(), PinnedApps.restore(null, null))
    }

    private fun f(vararg pkgs: String) = "folder:" + pkgs.joinToString(",")

    @Test fun folderCreation() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", f("b", "d"), "c"), PinnedApps.joinFolder(list, "d", "b"))
        assertEquals(listOf(f("a", "c"), "b", "d"), PinnedApps.joinFolder(list, "c", "a"))
    }

    @Test fun appJoinsAFolderUntilItHoldsFour() {
        val list = listOf("a", f("b", "c", "d"), "e")
        assertEquals(listOf("a", f("b", "c", "d", "e")), PinnedApps.joinFolder(list, "e", f("b", "c", "d")))
        val full = listOf("a", f("b", "c", "d", "e"))
        assertEquals(full, PinnedApps.joinFolder(full, "a", f("b", "c", "d", "e")))
    }

    @Test fun foldersDoNotNestOrJoinThemselves() {
        val list = listOf(f("a", "b"), f("c", "d"), "e")
        assertEquals(list, PinnedApps.joinFolder(list, f("a", "b"), f("c", "d")))
        assertEquals(list, PinnedApps.joinFolder(list, "e", "e"))
        assertEquals(list, PinnedApps.joinFolder(list, "missing", "e"))
    }

    @Test fun folderCountsAsOneSlot() {
        val list = listOf(f("a", "b", "c", "d"), "e", "f", "g", "h", "i", "j", "k")
        assertEquals(8, PinnedApps.normalize(list).size)
        assertEquals(11, PinnedApps.flatPackages(list).size)
        // 8 slots are full, so no ninth app can be pinned, even though one of them is a folder
        assertEquals(list, PinnedApps.pin(list, "z"))
    }

    @Test fun dragOutGivesAppASlot() {
        val list = listOf("a", f("b", "c", "d"), "e")
        assertEquals(listOf("a", f("b", "d"), "c", "e"), PinnedApps.dragOut(list, f("b", "c", "d"), "c"))
    }

    @Test fun dragOutOfPairDissolves() {
        val list = listOf("a", f("b", "c"), "e")
        assertEquals(listOf("a", "c", "b", "e"), PinnedApps.dragOut(list, f("b", "c"), "b"))
    }

    @Test fun dragOutNoSlotUnpins() {
        val list = listOf(f("a", "b", "c"), "d", "e", "f", "g", "h", "i", "j")
        assertEquals(8, list.size)
        assertEquals(true, PinnedApps.dragOutUnpins(list))
        val out = PinnedApps.dragOut(list, f("a", "b", "c"), "b")
        assertEquals(listOf(f("a", "c"), "d", "e", "f", "g", "h", "i", "j"), out)
        assertEquals(false, "b" in PinnedApps.flatPackages(out))
        // 2 apps left in the folder, one dragged out of a full grid: the other becomes a plain app and the dragged one goes
        val tight = listOf(f("a", "b"), "d", "e", "f", "g", "h", "i", "j")
        assertEquals(listOf("a", "d", "e", "f", "g", "h", "i", "j"), PinnedApps.dragOut(tight, f("a", "b"), "b"))
    }

    @Test fun folderMoveReordersFolderOnly() {
        val list = listOf("x", f("a", "b", "c"), "y")
        assertEquals(listOf("x", f("c", "a", "b"), "y"), PinnedApps.moveInFolder(list, f("a", "b", "c"), 2, 0))
        assertEquals(listOf("x", f("b", "c", "a"), "y"), PinnedApps.moveInFolder(list, f("a", "b", "c"), 0, 2))
        assertEquals(list, PinnedApps.moveInFolder(list, f("a", "b", "c"), 1, 1))
        assertEquals(list, PinnedApps.moveInFolder(list, f("a", "b", "c"), 0, 3))
        assertEquals(list, PinnedApps.moveInFolder(list, f("q", "r"), 0, 1))
        assertEquals(list, PinnedApps.moveInFolder(list, "x", 0, 0))
    }

    @Test fun folderUnpinShrinksOrDissolves() {
        assertEquals(listOf(f("a", "c"), "x"), PinnedApps.unpin(listOf(f("a", "b", "c"), "x"), "b"))
        assertEquals(listOf("a", "x"), PinnedApps.unpin(listOf(f("a", "b"), "x"), "b"))
        assertEquals(listOf("x"), PinnedApps.unpin(listOf("a", "x"), "a"))
    }

    @Test fun normalizeRepairsFoldersFromBackups() {
        // one app left after de-duplication: the folder is dissolved. An app already pinned earlier is not repeated in a folder
        assertEquals(listOf("a", "b"), PinnedApps.normalize(listOf("a", f("a", "b"))))
        assertEquals(listOf(f("a", "b", "c", "d")), PinnedApps.normalize(listOf(f("a", "b", "c", "d", "e"))))
        assertEquals(emptyList<String>(), PinnedApps.normalize(listOf("folder:")))
        assertEquals(listOf(f("x", "y")), PinnedApps.decode(PinnedApps.encode(listOf(f("x", "y")))))
    }
}

class SmartIslandSettingsLockScreenAndFaceIdTest {
    @Test
    fun lockScreenDefaults() {
        val d = SmartIslandSettings.Default
        assertEquals(false, d.hideAllOnLockScreen)
        assertEquals(emptySet<String>(), d.hideOnLockScreenEvents)
        assertEquals(true, d.notificationsEnabled)
        IslandEvent.entries.forEach {
            assertEquals(false, d.isEventHiddenOnLockScreen(it.id))
            assertEquals(true, d.isEventEnabled(it.id))
        }
    }

    @Test
    fun lockScreenEventOverrides() {
        val own = SmartIslandSettings.Default.copy(hideOnLockScreenEvents = setOf("music"))
        assertEquals(true, own.isEventHiddenOnLockScreen("music"))
        assertEquals(false, own.isEventHiddenOnLockScreen("timer"))

        val all = SmartIslandSettings.Default.copy(hideAllOnLockScreen = true)
        assertEquals(true, all.isEventHiddenOnLockScreen("timer"))
    }

    @Test
    fun callAutoExpandOrientation() {
        assertTrue(SmartIslandSettings.Default.isEventAutoExpand("call"))
        val off = SmartIslandSettings.Default.copy(noAutoExpandEvents = setOf("call"))
        assertFalse(off.isEventAutoExpand("call"))
        assertTrue(off.isLandscapeEventAutoExpand("call"))
        val landscapeOff = SmartIslandSettings.Default.copy(noAutoExpandEventsLandscape = setOf("call"))
        assertTrue(landscapeOff.isEventAutoExpand("call"))
        assertFalse(landscapeOff.isLandscapeEventAutoExpand("call"))
    }

    @Test
    fun callsAreNeverHiddenOnTheLockScreen() {
        val all = SmartIslandSettings.Default.copy(
            hideAllOnLockScreen = true,
            hideOnLockScreenEvents = setOf("call")
        )
        assertEquals(false, all.isEventHiddenOnLockScreen("call"))
        assertEquals(false, IslandEvent.CALL.canHideOnLockScreen)
    }

    @Test
    fun notifSwitchOffsNotifEventOnly() {
        val off = SmartIslandSettings.Default.copy(notificationsEnabled = false)
        assertEquals(false, off.isEventEnabled("notification"))
        assertEquals(false, off.isLandscapeEventEnabled("notification"))
        assertEquals(true, off.isEventDisabledEverywhere("notification"))
        assertEquals(true, off.isEventDisabledSomewhere("notification"))
        // every other event keeps its own switch
        assertEquals(true, off.isEventEnabled("music"))
        assertEquals(true, off.isEventEnabled("call"))
        assertEquals(false, off.isEventDisabledEverywhere("music"))
    }

    @Test
    fun lockScreenSwitchBackup() {
        val custom = SmartIslandSettings.Default.copy(
            hideAllOnLockScreen = true,
            hideOnLockScreenEvents = setOf("music", "wifi"),
            notificationsEnabled = false
        )
        val restored = SmartIslandSettings.fromJson(custom.toJson())
        assertEquals(true, restored.hideAllOnLockScreen)
        assertEquals(setOf("music", "wifi"), restored.hideOnLockScreenEvents)
        assertEquals(false, restored.notificationsEnabled)
    }

    @Test
    fun legacyLockScreenDefaults() {
        val old = JSONObject(SmartIslandSettings.Default.copy(notificationsEnabled = false).toJson())
        val settings = old.optJSONObject("settings") ?: old
        settings.remove("hideAllOnLockScreen")
        settings.remove("hideOnLockScreenEvents")
        settings.remove("notificationsEnabled")
        val restored = SmartIslandSettings.fromJson(old.toString())
        assertEquals(false, restored.hideAllOnLockScreen)
        assertEquals(emptySet<String>(), restored.hideOnLockScreenEvents)
        assertEquals(true, restored.notificationsEnabled)
    }

    @Test
    fun foregroundEventBackup() {
        val d = SmartIslandSettings.Default
        assertFalse("music" in d.showInForegroundEvents)
        assertFalse("navigation" in d.showInForegroundEvents)
        assertTrue("download" in d.showInForegroundEvents)
        // timer and stopwatch have no switch
        assertFalse("timer" in d.showInForegroundEvents)
        assertFalse("stopwatch" in d.showInForegroundEvents)

        val custom = d.copy(showInForegroundEvents = setOf("music", "hotspot"))
        assertEquals(setOf("music", "hotspot"), SmartIslandSettings.fromJson(custom.toJson()).showInForegroundEvents)

        // a backup from before the per-event switches: the old "on" kept every live card, "off" or missing is the default
        fun legacy(on: Boolean?): SmartIslandSettings {
            val json = JSONObject(custom.toJson())
            val settings = json.optJSONObject("settings") ?: json
            settings.remove("showInForegroundEvents")
            if (on != null) settings.put("showLiveEventsInForeground", on)
            return SmartIslandSettings.fromJson(json.toString())
        }
        assertEquals(IslandEvent.FOREGROUND_SWITCHABLE.map { it.id }.toSet(), legacy(true).showInForegroundEvents)
        assertEquals(d.showInForegroundEvents, legacy(false).showInForegroundEvents)
        assertEquals(d.showInForegroundEvents, legacy(null).showInForegroundEvents)
    }

    @Test
    fun landscapeCircleStartsInvisible() {
        assertEquals(0f, SmartIslandSettings.Default.landscapeCircleOpacity, 0f)
        assertEquals(0f, SmartIslandSettings.MIN_CIRCLE_OPACITY, 0f)
        val faded = SmartIslandSettings.Default.copy(landscapeCircleOpacity = 0.6f)
        assertEquals(0.6f, SmartIslandSettings.fromJson(faded.toJson()).landscapeCircleOpacity, 0.001f)
        // a backup that says 0 keeps 0 (it is not lifted to the 20 % floor)
        val zero = SmartIslandSettings.Default.copy(landscapeCircleOpacity = 0f)
        assertEquals(0f, SmartIslandSettings.fromJson(zero.toJson()).landscapeCircleOpacity, 0f)
    }

    @Test
    fun importDropsUnknownLockEventIds() {
        val custom = SmartIslandSettings.Default.copy(hideOnLockScreenEvents = setOf("music", "not_an_event"))
        assertEquals(setOf("music"), SmartIslandSettings.fromJson(custom.toJson()).hideOnLockScreenEvents)
    }

    @Test
    fun perAppLockScreenBackup() {
        assertEquals(emptySet<String>(), SmartIslandSettings.Default.hideOnLockScreenPackages)
        val custom = SmartIslandSettings.Default.copy(hideOnLockScreenPackages = setOf("com.a", "com.b"))
        assertEquals(setOf("com.a", "com.b"), SmartIslandSettings.fromJson(custom.toJson()).hideOnLockScreenPackages)
        val old = JSONObject(custom.toJson())
        (old.optJSONObject("settings") ?: old).remove("hideOnLockScreenPackages")
        assertEquals(emptySet<String>(), SmartIslandSettings.fromJson(old.toString()).hideOnLockScreenPackages)
    }

    @Test
    fun perAppLockScreenRuleNeverHidesACall() {
        val s = SmartIslandSettings.Default.copy(hideOnLockScreenPackages = setOf("com.chat"))
        assertEquals(true, s.isAppHiddenOnLockScreen("com.chat", IslandMode.Notification))
        assertEquals(false, s.isAppHiddenOnLockScreen("com.chat", IslandMode.IncomingCall))
        assertEquals(false, s.isAppHiddenOnLockScreen("com.other", IslandMode.Notification))
    }

    @Test
    fun manualCutoutBackupClamped() {
        val s = SmartIslandSettings(
            cameraCutoutManual = true, cameraCutoutX = -30f, cameraCutoutY = 9f,
            cameraCutoutWidth = 44f, cameraCutoutHeight = 26f
        )
        val back = SmartIslandSettings.fromJson(s.toJson())
        assertEquals(true, back.cameraCutoutManual)
        assertEquals(-30f, back.cameraCutoutX, 0.01f)
        assertEquals(9f, back.cameraCutoutY, 0.01f)
        assertEquals(44f, back.cameraCutoutWidth, 0.01f)
        assertEquals(26f, back.cameraCutoutHeight, 0.01f)
        val wild = SmartIslandSettings.fromJson(s.copy(cameraCutoutWidth = 9999f, cameraCutoutX = 9999f).toJson())
        assertEquals(SmartIslandSettings.MAX_CUTOUT_WIDTH, wild.cameraCutoutWidth, 0.01f)
        assertEquals(SmartIslandSettings.MAX_CUTOUT_X, wild.cameraCutoutX, 0.01f)
    }

    @Test
    fun cutoutStyleBackupFallsToAuto() {
        assertEquals(SmartIslandSettings.CUTOUT_STYLE_AUTO, SmartIslandSettings.Default.cameraCutoutStyle)
        for (style in VALID_CUTOUT_STYLES) {
            val back = SmartIslandSettings.fromJson(SmartIslandSettings(cameraCutoutStyle = style).toJson())
            assertEquals(style, back.cameraCutoutStyle)
        }
        val bad = SmartIslandSettings.fromJson(SmartIslandSettings(cameraCutoutStyle = "diagonal").toJson())
        assertEquals(SmartIslandSettings.CUTOUT_STYLE_AUTO, bad.cameraCutoutStyle)
        val old = JSONObject(SmartIslandSettings(cameraCutoutManual = true, cameraCutoutX = -30f).toJson())
        (old.optJSONObject("settings") ?: old).remove("cameraCutoutStyle")
        val back = SmartIslandSettings.fromJson(old.toString())
        assertEquals(SmartIslandSettings.CUTOUT_STYLE_AUTO, back.cameraCutoutStyle)
        assertEquals(true, back.cameraCutoutManual)
        assertEquals(-30f, back.cameraCutoutX, 0.01f)
    }

    @Test
    fun appFaceIdDefaultsOn() {
        val d = SmartIslandSettings()
        assertTrue(d.appFaceIdEnabled)
        assertEquals("square_tick", d.appFaceIdStyle)
    }

    @Test
    fun appFaceIdSettingsSurviveBackup() {
        val restored = SmartIslandSettings.fromJson(
            SmartIslandSettings(faceIdStyle = "cutout", appFaceIdStyle = "square", appFaceIdEnabled = false).toJson(appVersion = "7.0.0")
        )
        assertEquals("cutout", restored.faceIdStyle)
        assertEquals("square", restored.appFaceIdStyle)
        assertFalse(restored.appFaceIdEnabled)
    }

    @Test
    fun fingerprintAnimDefaultsOn() {
        assertTrue(SmartIslandSettings().fingerprintAnimationEnabled)
        val restored = SmartIslandSettings.fromJson(
            SmartIslandSettings(fingerprintAnimationEnabled = false).toJson(appVersion = "7.0.0")
        )
        assertFalse(restored.fingerprintAnimationEnabled)
    }

    @Test
    fun oldBackupFingerprintLoadsOn() {
        val json = SmartIslandSettings().toJson(appVersion = "7.0.0").replace("fingerprintAnimationEnabled", "unknownKey")
        assertTrue(SmartIslandSettings.fromJson(json).fingerprintAnimationEnabled)
    }

    @Test
    fun unknownAppFaceStyleExpanded() {
        val json = SmartIslandSettings(appFaceIdStyle = "cutout").toJson(appVersion = "7.0.0").replace("\"cutout\"", "\"bogus\"")
        assertEquals("square_tick", SmartIslandSettings.fromJson(json).appFaceIdStyle)
    }

    @Test
    fun faceIdStyleBackupExpanded() {
        assertEquals("square_tick", SmartIslandSettings().faceIdStyle)
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings(faceIdStyle = "cutout").toJson(appVersion = "7.0.0"))
        assertEquals("cutout", restored.faceIdStyle)
    }

    @Test
    fun mixFaceIdStyleSurvivesTheBackup() {
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings(faceIdStyle = "square").toJson(appVersion = "7.0.0"))
        assertEquals("square", restored.faceIdStyle)
    }

    @Test
    fun backupUnknownFaceStyleExpanded() {
        val json = SmartIslandSettings(faceIdStyle = "cutout").toJson(appVersion = "7.0.0").replace("\"cutout\"", "\"bogus\"")
        assertEquals("square_tick", SmartIslandSettings.fromJson(json).faceIdStyle)
    }

    @Test
    fun pulseSpeedBackupRoundTrip() {
        assertEquals(100, SmartIslandSettings.Default.pulseHoldPercent)
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings(pulseHoldPercent = 150).toJson(appVersion = "1"))
        assertEquals(150, restored.pulseHoldPercent)
    }
}
