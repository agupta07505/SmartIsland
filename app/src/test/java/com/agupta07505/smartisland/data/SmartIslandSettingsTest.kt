/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmartIslandSettingsTest {
    @Test
    fun testDefaultSettings() {
        val settings = SmartIslandSettings.Default
        assertEquals(false, settings.enabled)
        assertEquals(112f, settings.width)
        assertEquals(34f, settings.height)
        assertEquals(0f, settings.xOffset)
        assertEquals(12f, settings.yOffset)
        assertEquals(22f, settings.cornerRadius)
        assertEquals(1f, settings.opacity)
        assertEquals(0xFF000000L, settings.pillColor)
        assertEquals(true, settings.showNotificationActions)
        assertEquals(true, settings.hideFromNotificationShade)
        assertEquals(true, settings.autoExpandOnNotification)
        assertEquals(true, settings.autoExpandOnLockScreen)
        assertEquals(true, settings.enableShadow)
        assertEquals(14f, settings.shadowElevation)
        assertEquals(true, settings.enableMusicArtworkBackground)
        assertEquals(true, settings.enableNotificationBackdrop)
        assertEquals(0xFF2563EBL, settings.bluetoothColor)
        assertEquals(0xFFF59E0BL, settings.flashlightColor)
        assertEquals(0xFFEF4444L, settings.screenRecordingColor)
        assertEquals(0xFFF59E0BL, settings.timerColor)
        assertEquals(0xFF06B6D4L, settings.stopwatchColor)
        assertEquals(true, settings.enableAppShortcuts)
        assertEquals(true, settings.allowNetworkChecks)
        assertEquals(true, settings.showBluetoothBattery)
        assertEquals(true, settings.enableBatteryMode)
        assertEquals(false, settings.enableNotificationCooldown)
        assertEquals(3, settings.notificationCooldownDurationMinutes)
        assertEquals(3, settings.notificationCooldownThreshold)
        assertEquals(emptySet<String>(), settings.notificationCooldownExcludedPackages)
        assertEquals(false, settings.enableNotificationHistory)
        assertEquals(72, settings.notificationHistoryRetentionHours)
        assertEquals(true, settings.developerModeEnabled)
        assertEquals(false, settings.recordLogs)
        assertEquals(false, settings.enableNotchMode)
        assertEquals(true, settings.enableSwipeActions)
        assertEquals("DismissCurrent", settings.swipeUpAction)
        assertEquals("DismissAll", settings.swipeHoldUpAction)
        assertEquals("None", settings.swipeDownAction)
        assertEquals("Expand", settings.swipeDownCollapsedAction)
        assertEquals("NextEvent", settings.swipeLeftAction)
        assertEquals("PreviousEvent", settings.swipeRightAction)
        assertEquals("NextPrevious", settings.swipeHorizontalCollapsedAction)
        assertEquals(true, settings.enablePillSwipeActions)
        assertEquals("DismissCurrent", settings.pillSwipeUpAction)
        assertEquals("Expand", settings.pillSwipeDownAction)
        assertEquals("NextEvent", settings.pillSwipeLeftAction)
        assertEquals("PreviousEvent", settings.pillSwipeRightAction)
        assertEquals("TogglePill", settings.longPressAction)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, settings.circlePosition)
    }

    @Test
    fun testEquality() {
        val settings1 = SmartIslandSettings(enabled = true, width = 120f)
        val settings2 = SmartIslandSettings(enabled = true, width = 120f)
        val settings3 = SmartIslandSettings(enabled = false, width = 120f)

        assertEquals(settings1, settings2)
        assertNotEquals(settings1, settings3)
    }

    @Test
    fun testToJsonAndFromJsonRoundTrip() {
        val original = SmartIslandSettings(
            enabled = true,
            width = 135f,
            height = 38f,
            xOffset = -15f,
            yOffset = 20f,
            cornerRadius = 26f,
            opacity = 0.85f,
            pillColor = 0xFF1E293BL,
            batteryColor = 0xFF22C55EL,
            notificationDotColor = 0xFF3B82F6L,
            musicVisualizerColor = 0xFFEC4899L,
            hotspotColor = 0xFFEAB308L,
            callColor = 0xFF10B981L,
            liveActivityColor = 0xFFA855F7L,
            transferColor = 0xFF06B6D4L,
            navigationColor = 0xFF14B8A6L,
            bluetoothColor = 0xFF6366F1L,
            flashlightColor = 0xFFF59E0BL,
            screenRecordingColor = 0xFFF43F5EL,
            timerColor = 0xFFFB923CL,
            stopwatchColor = 0xFF0EA5E9L,
            enableAppShortcuts = false,
            shortcutPackages = listOf("com.android.chrome", "com.spotify.music"),
            showRecentApps = true,
            welcomeDialogShown = true,
            showOnLockScreen = true,
            lockScreenPrivacy = "FullContent",
            showNotificationActions = false,
            hideFromNotificationShade = false,
            liveActivitiesEnabled = false,
            navigationEnabled = false,
            disabledNotificationPackages = setOf("com.spam.app"),
            disabledSoundPackages = setOf("com.noisy.app"),
            disabledEvents = setOf("wifi", "battery_low"),
            hideWhenIdle = true,
            autoHidePill = true,
            autoHideTimeoutSeconds = 10,
            showInLandscape = true,
            showBluetoothBattery = false,
            enableNotificationCooldown = true,
            notificationCooldownDurationMinutes = 5,
            notificationCooldownThreshold = 4,
            notificationCooldownExcludedPackages = setOf("com.priority.chat", "com.work.email"),
            autoExpandOnNotification = false,
            autoExpandOnLockScreen = false,
            enableShadow = true,
            shadowElevation = 18f,
            enableMusicArtworkBackground = false,
            enableNotificationBackdrop = false,
            deviceType = "CUSTOM",
            allowNetworkChecks = false,
            enableNotificationHistory = true,
            notificationHistoryRetentionHours = 48,
            developerModeEnabled = true,
            recordLogs = true,
            enableNotchMode = true,
            enableSwipeActions = false,
            swipeUpAction = "Collapse",
            swipeHoldUpAction = "None",
            swipeDownAction = "NotificationShade",
            swipeDownCollapsedAction = "DismissAll",
            swipeLeftAction = "PreviousEvent",
            swipeRightAction = "NextEvent",
            swipeHorizontalCollapsedAction = "None",
            enablePillSwipeActions = false,
            pillSwipeUpAction = "None",
            pillSwipeDownAction = "NotificationShade",
            pillSwipeLeftAction = "PreviousTrack",
            pillSwipeRightAction = "NextTrack",
            circlePosition = SmartIslandSettings.CIRCLE_POSITION_LEFT
        )

        val jsonString = original.toJson(appVersion = "7.0.0")
        val restored = SmartIslandSettings.fromJson(jsonString)

        assertEquals(original, restored)

        val metadata = SmartIslandSettings.parseBackupMetadata(jsonString)
        org.junit.Assert.assertNotNull(metadata)
        assertEquals("7.0.0", metadata?.appVersion)
        assertEquals(SmartIslandSettings.BACKUP_FORMAT_VERSION, metadata?.formatVersion)
    }

    @Test
    fun testSwipeActionModelAndSerialization() {
        // Test SwipeAction.fromId
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.DismissCurrent, com.agupta07505.smartisland.model.SwipeAction.fromId("DismissCurrent"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.DismissAll, com.agupta07505.smartisland.model.SwipeAction.fromId("dismissall"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.NextTrack, com.agupta07505.smartisland.model.SwipeAction.fromId("NextTrack"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.PreviousTrack, com.agupta07505.smartisland.model.SwipeAction.fromId("PreviousTrack"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.PlayPause, com.agupta07505.smartisland.model.SwipeAction.fromId("PlayPause"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.NextNotification, com.agupta07505.smartisland.model.SwipeAction.fromId("NextNotification"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.PreviousNotification, com.agupta07505.smartisland.model.SwipeAction.fromId("PreviousNotification"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.None, com.agupta07505.smartisland.model.SwipeAction.fromId("None"))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.None, com.agupta07505.smartisland.model.SwipeAction.fromId(null))
        assertEquals(com.agupta07505.smartisland.model.SwipeAction.Expand, com.agupta07505.smartisland.model.SwipeAction.fromId("UnknownAction", default = com.agupta07505.smartisland.model.SwipeAction.Expand))

        // Test disabled / custom swipe actions in JSON parsing
        val json = """
            {
                "settings": {
                    "enableSwipeActions": false,
                    "swipeUpAction": "None",
                    "swipeHoldUpAction": "Collapse",
                    "swipeDownAction": "None",
                    "swipeDownCollapsedAction": "NotificationShade",
                    "swipeLeftAction": "NextEvent",
                    "swipeRightAction": "PreviousEvent",
                    "swipeHorizontalCollapsedAction": "None",
                    "enablePillSwipeActions": false,
                    "pillSwipeUpAction": "None",
                    "pillSwipeDownAction": "FloatingWindow",
                    "pillSwipeLeftAction": "PlayPause",
                    "pillSwipeRightAction": "NextTrack"
                }
            }
        """.trimIndent()

        val settings = SmartIslandSettings.fromJson(json)
        assertEquals(false, settings.enableSwipeActions)
        assertEquals("None", settings.swipeUpAction)
        assertEquals("Collapse", settings.swipeHoldUpAction)
        assertEquals("None", settings.swipeDownAction)
        assertEquals("NotificationShade", settings.swipeDownCollapsedAction)
        assertEquals("NextEvent", settings.swipeLeftAction)
        assertEquals("PreviousEvent", settings.swipeRightAction)
        assertEquals("None", settings.swipeHorizontalCollapsedAction)
        assertEquals(false, settings.enablePillSwipeActions)
        assertEquals("None", settings.pillSwipeUpAction)
        assertEquals("FloatingWindow", settings.pillSwipeDownAction)
        assertEquals("PlayPause", settings.pillSwipeLeftAction)
        assertEquals("NextTrack", settings.pillSwipeRightAction)
    }

    @Test
    fun testFromJsonWithFlatFormat() {
        val flatJson = """
            {
                "enabled": true,
                "width": 140.0,
                "height": 40.0,
                "xOffset": 10.0,
                "yOffset": 15.0,
                "cornerRadius": 24.0,
                "opacity": 0.9,
                "pillColor": 4278190080
            }
        """.trimIndent()

        val settings = SmartIslandSettings.fromJson(flatJson)
        assertEquals(true, settings.enabled)
        assertEquals(140f, settings.width)
        assertEquals(40f, settings.height)
        assertEquals(10f, settings.xOffset)
        assertEquals(15f, settings.yOffset)
        assertEquals(24f, settings.cornerRadius)
        assertEquals(0.9f, settings.opacity)
        assertEquals(4278190080L, settings.pillColor)
        // Check defaults are preserved for unspecified fields
        assertEquals(SmartIslandSettings.Default.batteryColor, settings.batteryColor)
        assertEquals(SmartIslandSettings.Default.shortcutPackages, settings.shortcutPackages)
    }

    @Test
    fun testFromJsonWithClampingAndInvalidValues() {
        val malformedJson = """
            {
                "settings": {
                    "width": 9999.0,
                    "height": -50.0,
                    "xOffset": 5000.0,
                    "yOffset": -100.0,
                    "cornerRadius": 500.0,
                    "opacity": 10.0,
                    "shadowElevation": 100.0,
                    "autoHideTimeoutSeconds": 999,
                    "lockScreenPrivacy": "INVALID_VALUE",
                    "pillColor": 0
                }
            }
        """.trimIndent()

        val settings = SmartIslandSettings.fromJson(malformedJson)
        assertEquals(SmartIslandSettings.MAX_WIDTH, settings.width)
        assertEquals(SmartIslandSettings.MIN_HEIGHT, settings.height)
        assertEquals(SmartIslandSettings.MAX_X_OFFSET, settings.xOffset)
        assertEquals(SmartIslandSettings.MIN_Y_OFFSET, settings.yOffset)
        assertEquals(SmartIslandSettings.MAX_CORNER_RADIUS, settings.cornerRadius)
        assertEquals(SmartIslandSettings.MAX_OPACITY, settings.opacity)
        assertEquals(SmartIslandSettings.MAX_SHADOW_ELEVATION, settings.shadowElevation)
        assertEquals(120, settings.autoHideTimeoutSeconds)
        assertEquals("AppIconOnly", settings.lockScreenPrivacy)
        assertEquals(SmartIslandSettings.Default.pillColor, settings.pillColor)
    }

    @Test
    fun testCirclePositionSerializationAndDefaults() {
        val leftJson = """{"settings": {"circlePosition": "left"}}"""
        val settingsLeft = SmartIslandSettings.fromJson(leftJson)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, settingsLeft.circlePosition)

        val rightJson = """{"settings": {"circlePosition": "right"}}"""
        val settingsRight = SmartIslandSettings.fromJson(rightJson)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, settingsRight.circlePosition)

        val invalidJson = """{"settings": {"circlePosition": "top_center"}}"""
        val settingsInvalid = SmartIslandSettings.fromJson(invalidJson)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, settingsInvalid.circlePosition)
    }

    @Test
    fun longPressActionDefaultsBacksUpAndRestores() {
        assertEquals("TogglePill", SmartIslandSettings().longPressAction)
        assertEquals(
            com.agupta07505.smartisland.model.SwipeAction.TogglePill,
            com.agupta07505.smartisland.model.SwipeAction.fromId("TogglePill")
        )

        val off = SmartIslandSettings(longPressAction = "None")
        assertEquals("None", SmartIslandSettings.fromJson(off.toJson(appVersion = "1")).longPressAction)

        // A backup made before the setting existed keeps the default.
        val old = """{"settings": {"enableSwipeActions": true}}"""
        assertEquals("TogglePill", SmartIslandSettings.fromJson(old).longPressAction)
    }

    @Test
    fun timingDefaultsMatchTheOldFixedValues() {
        val d = SmartIslandSettings.Default
        assertEquals(5_000, d.idleTimeoutMs)
        assertEquals(4_000, d.batteryWarningMs)
        assertEquals(1_800, d.popupMs)
        assertEquals(1_800, d.secondaryMergeMs)
    }

    @Test
    fun timingRoundTripsThroughBackup() {
        val custom = SmartIslandSettings(
            idleTimeoutMs = 8_500,
            batteryWarningMs = 2_000,
            popupMs = 3_000,
            secondaryMergeMs = 700
        )
        val restored = SmartIslandSettings.fromJson(custom.toJson(appVersion = "1"))
        assertEquals(8_500, restored.idleTimeoutMs)
        assertEquals(2_000, restored.batteryWarningMs)
        assertEquals(3_000, restored.popupMs)
        assertEquals(700, restored.secondaryMergeMs)
    }

    @Test
    fun timingImportIsClampedAndOldBackupsKeepDefaults() {
        val wild = """{"settings": {"idleTimeoutMs": 999999, "batteryWarningMs": -5, "popupMs": 1, "secondaryMergeMs": 600000}}"""
        val clamped = SmartIslandSettings.fromJson(wild)
        assertEquals(SmartIslandSettings.MAX_IDLE_TIMEOUT_MS, clamped.idleTimeoutMs)
        assertEquals(SmartIslandSettings.MIN_BATTERY_WARNING_MS, clamped.batteryWarningMs)
        assertEquals(SmartIslandSettings.MIN_POPUP_MS, clamped.popupMs)
        assertEquals(SmartIslandSettings.MAX_SECONDARY_MERGE_MS, clamped.secondaryMergeMs)

        val old = """{"settings": {"enableSwipeActions": true}}"""
        val fromOld = SmartIslandSettings.fromJson(old)
        assertEquals(SmartIslandSettings.Default.idleTimeoutMs, fromOld.idleTimeoutMs)
        assertEquals(SmartIslandSettings.Default.popupMs, fromOld.popupMs)
    }

    @Test
    fun animationSpeedDndAndAppRulesDefaultToToday() {
        val d = SmartIslandSettings.Default
        assertEquals("relaxed", d.animSpeedPortrait)
        assertEquals("relaxed", d.animSpeedLandscape)
        assertEquals(false, d.showDuringDnd)
        assertEquals(emptySet<String>(), d.noExpandPackages)
        assertEquals(emptySet<String>(), d.noAutoExpandPackages)
    }

    @Test
    fun animationSpeedDndAndAppRulesSurviveJsonRoundTrip() {
        val custom = SmartIslandSettings.Default.copy(
            animSpeedPortrait = "snappy",
            animSpeedLandscape = "relaxed",
            showDuringDnd = true,
            noExpandPackages = setOf("com.example.a"),
            noAutoExpandPackages = setOf("com.example.b", "com.example.c")
        )
        val restored = SmartIslandSettings.fromJson(custom.toJson())
        assertEquals("snappy", restored.animSpeedPortrait)
        assertEquals("relaxed", restored.animSpeedLandscape)
        assertEquals(true, restored.showDuringDnd)
        assertEquals(setOf("com.example.a"), restored.noExpandPackages)
        assertEquals(setOf("com.example.b", "com.example.c"), restored.noAutoExpandPackages)
    }

    @Test
    fun autoExpandRulesDefaultToOffForAppsAndOnForEvents() {
        val d = SmartIslandSettings.Default
        assertEquals(emptySet<String>(), d.autoExpandPackages)
        assertEquals(emptySet<String>(), d.noAutoExpandEvents)
        assertEquals(true, d.isEventAutoExpand("battery_plugged_in"))
    }

    @Test
    fun autoExpandAppsAndEventsSurviveJsonRoundTrip() {
        val custom = SmartIslandSettings.Default.copy(
            autoExpandPackages = setOf("com.example.on"),
            noAutoExpandEvents = setOf("battery_plugged_in", "wifi")
        )
        val restored = SmartIslandSettings.fromJson(custom.toJson())
        assertEquals(setOf("com.example.on"), restored.autoExpandPackages)
        assertEquals(setOf("battery_plugged_in", "wifi"), restored.noAutoExpandEvents)
        assertEquals(false, restored.isEventAutoExpand("wifi"))
        assertEquals(true, restored.isEventAutoExpand("bluetooth"))
    }

    @Test
    fun oldBackupsWithoutTheAutoExpandKeysLoadWithTheDefaults() {
        val old = org.json.JSONObject(SmartIslandSettings.Default.toJson())
        val settings = old.optJSONObject("settings") ?: old
        settings.remove("autoExpandPackages")
        settings.remove("noAutoExpandEvents")
        val restored = SmartIslandSettings.fromJson(old.toString())
        assertEquals(emptySet<String>(), restored.autoExpandPackages)
        assertEquals(emptySet<String>(), restored.noAutoExpandEvents)
    }

    @Test
    fun anAppInBothAutoExpandSetsImportsAsOffAndUnknownEventIdsAreDropped() {
        val custom = SmartIslandSettings.Default.copy(
            autoExpandPackages = setOf("com.example.both", "com.example.on"),
            noAutoExpandPackages = setOf("com.example.both"),
            noAutoExpandEvents = setOf("wifi", "not_an_event")
        )
        val restored = SmartIslandSettings.fromJson(custom.toJson())
        assertEquals(setOf("com.example.on"), restored.autoExpandPackages)
        assertEquals(setOf("com.example.both"), restored.noAutoExpandPackages)
        assertEquals(setOf("wifi"), restored.noAutoExpandEvents)
    }

    @Test
    fun hapticStrengthDefaultsRoundTripsAndFallsBack() {
        assertEquals("normal", SmartIslandSettings.Default.hapticStrength)
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings.Default.copy(hapticStrength = "strong").toJson())
        assertEquals("strong", restored.hapticStrength)
        val bad = SmartIslandSettings.fromJson("""{"settings": {"hapticStrength": "earthquake"}}""")
        assertEquals("normal", bad.hapticStrength)
        val old = SmartIslandSettings.fromJson("""{"settings": {"width": 100}}""")
        assertEquals("normal", old.hapticStrength)
    }

    @Test
    fun gestureSensitivityDefaultsRoundTripsAndFallsBack() {
        assertEquals("normal", SmartIslandSettings.Default.gestureSensitivity)
        val restored = SmartIslandSettings.fromJson(SmartIslandSettings.Default.copy(gestureSensitivity = "low").toJson())
        assertEquals("low", restored.gestureSensitivity)
        val bad = SmartIslandSettings.fromJson("""{"settings": {"gestureSensitivity": "twitchy"}}""")
        assertEquals("normal", bad.gestureSensitivity)
        val old = SmartIslandSettings.fromJson("""{"settings": {"width": 100}}""")
        assertEquals("normal", old.gestureSensitivity)
    }

    @Test
    fun badAnimationSpeedFallsBackAndOldBackupsKeepDefaults() {
        val bad = """{"settings": {"animSpeedPortrait": "warp", "animSpeedLandscape": ""}}"""
        val parsed = SmartIslandSettings.fromJson(bad)
        assertEquals("relaxed", parsed.animSpeedPortrait)
        assertEquals("relaxed", parsed.animSpeedLandscape)
        val old = SmartIslandSettings.fromJson("""{"settings": {"width": 100}}""")
        assertEquals(false, old.showDuringDnd)
        assertEquals(emptySet<String>(), old.noExpandPackages)
    }

    @Test
    fun animationSpeedFollowsOrientation() {
        val s = SmartIslandSettings.Default.copy(animSpeedPortrait = "snappy", animSpeedLandscape = "relaxed")
        assertEquals(com.agupta07505.smartisland.util.AnimationSpeed.SNAPPY, s.animationSpeed(isLandscape = false))
        assertEquals(com.agupta07505.smartisland.util.AnimationSpeed.RELAXED, s.animationSpeed(isLandscape = true))
    }

    @Test
    fun restoreDialogCanRebuildItsStateFromTheSavedJson() {
        // The restore dialog keeps only the backup's JSON text across rotation and parses it again.
        val original = SmartIslandSettings.Default.copy(width = 130f, popupMs = 2_500)
        val json = original.toJson("1.0")
        assertEquals(130f, SmartIslandSettings.fromJson(json).width)
        assertEquals(2_500, SmartIslandSettings.fromJson(json).popupMs)
        assertEquals("1.0", SmartIslandSettings.parseBackupMetadata(json)?.appVersion)
    }

    // The media card's own blur and opacity.
    @Test
    fun oldBackupStartsMediaFromTheSharedBlurAndOpacity() {
        val json = """{"opacity": 0.7, "expandedBlurEnabled": true, "expandedBlurRadius": 30.0}"""
        val settings = SmartIslandSettings.fromJson(json)
        assertEquals(0.7f, settings.mediaOpacity, 0.001f)
        assertEquals(true, settings.mediaBlurEnabled)
        assertEquals(30f, settings.mediaBlurRadius, 0.001f)
    }

    @Test
    fun mediaValuesRoundTripAndStaySeparateFromTheShared() {
        val original = SmartIslandSettings.Default.copy(
            opacity = 0.9f, mediaOpacity = 0.5f,
            expandedBlurEnabled = false, mediaBlurEnabled = true,
            expandedBlurRadius = 10f, mediaBlurRadius = 40f
        )
        val restored = SmartIslandSettings.fromJson(original.toJson(appVersion = "1"))
        assertEquals(0.5f, restored.mediaOpacity, 0.001f)
        assertEquals(0.9f, restored.opacity, 0.001f)
        assertEquals(true, restored.mediaBlurEnabled)
        assertEquals(false, restored.expandedBlurEnabled)
        assertEquals(40f, restored.mediaBlurRadius, 0.001f)
    }

    @Test
    fun badMediaValuesAreClamped() {
        val settings = SmartIslandSettings.fromJson("""{"mediaOpacity": 0.0, "mediaBlurRadius": 500.0}""")
        assertEquals(SmartIslandSettings.MIN_OPACITY, settings.mediaOpacity, 0.001f)
        assertEquals(SmartIslandSettings.MAX_EXPANDED_BLUR, settings.mediaBlurRadius, 0.001f)
    }

    @Test
    fun forMediaSwapsInTheMediaValuesOnlyForMusicCards() {
        val s = SmartIslandSettings.Default.copy(opacity = 1f, mediaOpacity = 0.6f, expandedBlurEnabled = false, mediaBlurEnabled = true)
        assertEquals(0.6f, s.forMedia().opacity, 0.001f)
        assertEquals(true, s.forMedia().expandedBlurEnabled)
        assertEquals(1f, s.forCard(false).opacity, 0.001f)
        assertEquals(0.6f, s.forCard(true).opacity, 0.001f)
    }

    @Test
    fun islandFont_roundTripsAndFallsBackToDefault() {
        val chosen = SmartIslandSettings.Default.copy(islandFont = SmartIslandSettings.FONT_POPPINS)
        val restored = SmartIslandSettings.fromJson(chosen.toJson(appVersion = "7.0.0"))
        assertEquals(SmartIslandSettings.FONT_POPPINS, restored.islandFont)

        val bad = SmartIslandSettings.fromJson(
            chosen.toJson(appVersion = "7.0.0").replace("\"islandFont\": \"poppins\"", "\"islandFont\": \"comic\"")
        )
        assertEquals(SmartIslandSettings.Default.islandFont, bad.islandFont)
    }

    @Test
    fun islandFont_acceptsCustomIdsAndDropsRemovedFonts() {
        assertTrue(SmartIslandSettings.isValidFont(SmartIslandSettings.FONT_LORA))
        assertTrue(SmartIslandSettings.isValidFont("custom:My Font.ttf"))
        assertFalse(SmartIslandSettings.isValidFont("custom:../x.ttf"))
        assertFalse(SmartIslandSettings.isValidFont("custom:x.woff"))
        // Fonts that no longer ship with the app fall back to the default.
        assertFalse(SmartIslandSettings.isValidFont("georgia"))
        assertFalse(SmartIslandSettings.isValidFont("sf_pro_auto"))

        val custom = SmartIslandSettings.Default.copy(islandFont = "custom:My Font.ttf")
        assertEquals("custom:My Font.ttf", SmartIslandSettings.fromJson(custom.toJson(appVersion = "7.0.0")).islandFont)
        val removed = SmartIslandSettings.fromJson(
            custom.toJson(appVersion = "7.0.0").replace("custom:My Font.ttf", "sf_pro_display")
        )
        assertEquals(SmartIslandSettings.Default.islandFont, removed.islandFont)
    }

    @Test
    fun lockScreenAndNotificationSwitchesDefaultToTodaysBehaviour() {
        val d = SmartIslandSettings.Default
        assertEquals(false, d.hideAllOnLockScreen)
        assertEquals(emptySet<String>(), d.hideOnLockScreenEvents)
        assertEquals(true, d.notificationsEnabled)
        com.agupta07505.smartisland.model.IslandEvent.entries.forEach {
            assertEquals(false, d.isEventHiddenOnLockScreen(it.id))
            assertEquals(true, d.isEventEnabled(it.id))
        }
    }

    @Test
    fun anEventIsHiddenOnTheLockScreenByItsOwnSwitchOrTheUniversalOne() {
        val own = SmartIslandSettings.Default.copy(hideOnLockScreenEvents = setOf("music"))
        assertEquals(true, own.isEventHiddenOnLockScreen("music"))
        assertEquals(false, own.isEventHiddenOnLockScreen("timer"))

        val all = SmartIslandSettings.Default.copy(hideAllOnLockScreen = true)
        assertEquals(true, all.isEventHiddenOnLockScreen("timer"))
        assertEquals(true, all.isEventHiddenOnLockScreen("privacy_camera"))
    }

    @Test
    fun callsAreNeverHiddenOnTheLockScreen() {
        val all = SmartIslandSettings.Default.copy(
            hideAllOnLockScreen = true,
            hideOnLockScreenEvents = setOf("call")
        )
        assertEquals(false, all.isEventHiddenOnLockScreen("call"))
        assertEquals(false, com.agupta07505.smartisland.model.IslandEvent.CALL.canHideOnLockScreen)
    }

    @Test
    fun theUniversalNotificationsSwitchTurnsOffOnlyTheNotificationEvent() {
        val off = SmartIslandSettings.Default.copy(notificationsEnabled = false)
        assertEquals(false, off.isEventEnabled("notification"))
        assertEquals(false, off.isLandscapeEventEnabled("notification"))
        assertEquals(true, off.isEventDisabledEverywhere("notification"))
        assertEquals(true, off.isEventDisabledSomewhere("notification"))
        // Every other event keeps its own switch.
        assertEquals(true, off.isEventEnabled("music"))
        assertEquals(true, off.isEventEnabled("call"))
        assertEquals(false, off.isEventDisabledEverywhere("music"))
    }

    @Test
    fun lockScreenAndNotificationSwitchesSurviveJsonRoundTrip() {
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
    fun oldBackupsWithoutTheLockScreenKeysLoadWithTheDefaults() {
        val old = org.json.JSONObject(SmartIslandSettings.Default.copy(notificationsEnabled = false).toJson())
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
    fun liveEventsInForegroundIsOffByDefaultAndSurvivesRoundTrip() {
        assertEquals(false, SmartIslandSettings.Default.showLiveEventsInForeground)
        val custom = SmartIslandSettings.Default.copy(showLiveEventsInForeground = true)
        assertEquals(true, SmartIslandSettings.fromJson(custom.toJson()).showLiveEventsInForeground)
        val old = org.json.JSONObject(custom.toJson())
        (old.optJSONObject("settings") ?: old).remove("showLiveEventsInForeground")
        assertEquals(false, SmartIslandSettings.fromJson(old.toString()).showLiveEventsInForeground)
    }

    @Test
    fun landscapeCircleStartsInvisibleAndMayBeSetToZero() {
        assertEquals(0f, SmartIslandSettings.Default.landscapeCircleOpacity, 0f)
        assertEquals(0f, SmartIslandSettings.MIN_CIRCLE_OPACITY, 0f)
        val faded = SmartIslandSettings.Default.copy(landscapeCircleOpacity = 0.6f)
        assertEquals(0.6f, SmartIslandSettings.fromJson(faded.toJson()).landscapeCircleOpacity, 0.001f)
        // A backup that says 0 keeps 0 (it is not lifted to the 20 % floor).
        val zero = SmartIslandSettings.Default.copy(landscapeCircleOpacity = 0f)
        assertEquals(0f, SmartIslandSettings.fromJson(zero.toJson()).landscapeCircleOpacity, 0f)
    }

    @Test
    fun mergeStyleDefaultsToSlideInBothOrientations() {
        assertEquals("slide", SmartIslandSettings.Default.mergeStyle)
        assertEquals("slide", SmartIslandSettings.Default.mergeStyleLandscape)
    }

    @Test
    fun unknownEventIdsInTheLockScreenSetAreDroppedOnImport() {
        val custom = SmartIslandSettings.Default.copy(hideOnLockScreenEvents = setOf("music", "not_an_event"))
        assertEquals(setOf("music"), SmartIslandSettings.fromJson(custom.toJson()).hideOnLockScreenEvents)
    }

    @Test
    fun perAppLockScreenRuleDefaultsToNoneAndSurvivesRoundTrip() {
        assertEquals(emptySet<String>(), SmartIslandSettings.Default.hideOnLockScreenPackages)
        val custom = SmartIslandSettings.Default.copy(hideOnLockScreenPackages = setOf("com.a", "com.b"))
        assertEquals(setOf("com.a", "com.b"), SmartIslandSettings.fromJson(custom.toJson()).hideOnLockScreenPackages)
        val old = org.json.JSONObject(custom.toJson())
        (old.optJSONObject("settings") ?: old).remove("hideOnLockScreenPackages")
        assertEquals(emptySet<String>(), SmartIslandSettings.fromJson(old.toString()).hideOnLockScreenPackages)
    }

    @Test
    fun perAppLockScreenRuleNeverHidesACall() {
        val s = SmartIslandSettings.Default.copy(hideOnLockScreenPackages = setOf("com.chat"))
        assertEquals(true, s.isAppHiddenOnLockScreen("com.chat", com.agupta07505.smartisland.model.IslandMode.Notification))
        assertEquals(false, s.isAppHiddenOnLockScreen("com.chat", com.agupta07505.smartisland.model.IslandMode.IncomingCall))
        assertEquals(false, s.isAppHiddenOnLockScreen("com.other", com.agupta07505.smartisland.model.IslandMode.Notification))
    }

    @Test
    fun manualCameraCutoutSurvivesBackupAndIsClamped() {
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
        assertEquals(SmartIslandSettings.MAX_X_OFFSET, wild.cameraCutoutX, 0.01f)
    }
    @Test
    fun fontMixDefaultsToOffWithNoFontsOfItsOwn() {
        val d = SmartIslandSettings.Default
        assertEquals(false, d.fontMixEnabled)
        assertEquals("", d.fontMixLight)
        assertEquals("", d.fontMixRegular)
        assertEquals("", d.fontMixMedium)
    }

    @Test
    fun fontMixSurvivesABackupRoundTrip() {
        val s = SmartIslandSettings(
            fontMixEnabled = true,
            fontMixLight = "poppins",
            fontMixRegular = "custom:SF Pro Display Regular.otf",
            fontMixMedium = "inter_display"
        )
        val back = SmartIslandSettings.fromJson(s.toJson())
        assertEquals(true, back.fontMixEnabled)
        assertEquals("poppins", back.fontMixLight)
        assertEquals("custom:SF Pro Display Regular.otf", back.fontMixRegular)
        assertEquals("inter_display", back.fontMixMedium)
    }

    @Test
    fun fontMixDropsUnknownFontIdsFromABackup() {
        val s = SmartIslandSettings(fontMixEnabled = true, fontMixLight = "../../etc/passwd", fontMixRegular = "nonsense", fontMixMedium = "lora")
        val back = SmartIslandSettings.fromJson(s.toJson())
        assertEquals("", back.fontMixLight)
        assertEquals("", back.fontMixRegular)
        assertEquals("lora", back.fontMixMedium)
    }

    @Test
    fun anOldBackupWithoutFontMixLoadsWithItOff() {
        val old = SmartIslandSettings.fromJson(SmartIslandSettings().toJson())
        assertEquals(false, old.fontMixEnabled)
        assertEquals("", old.fontMixMedium)
    }
}
