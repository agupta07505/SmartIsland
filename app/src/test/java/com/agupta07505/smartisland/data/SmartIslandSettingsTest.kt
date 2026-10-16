/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
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
        assertEquals("FloatingWindow", settings.swipeDownAction)
        assertEquals("Expand", settings.swipeDownCollapsedAction)
        assertEquals("NextPrevious", settings.swipeHorizontalCollapsedAction)
        assertEquals(true, settings.enablePillSwipeActions)
        assertEquals("DismissCurrent", settings.pillSwipeUpAction)
        assertEquals("Expand", settings.pillSwipeDownAction)
        assertEquals("PreviousNotification", settings.pillSwipeLeftAction)
        assertEquals("NextNotification", settings.pillSwipeRightAction)
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
            shortcutPackages = setOf("com.android.chrome", "com.spotify.music"),
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
            enableShadow = true,
            shadowElevation = 18f,
            enableMusicArtworkBackground = false,
            enableNotificationBackdrop = false,
            deviceType = "CUSTOM",
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
    fun testLegacyNetworkFlagInBackupIsIgnored() {
        val legacy = """{"settings": {"allowNetworkChecks": true, "enabled": true}}"""
        val restored = SmartIslandSettings.fromJson(legacy)
        assertEquals(true, restored.enabled)
        assertEquals(false, restored.toJson().contains("allowNetworkChecks"))
    }

    @Test
    fun testHistoryDisabledByDefaultWithBoundedRetention() {
        val defaults = SmartIslandSettings.Default
        assertEquals(false, defaults.enableNotificationHistory)
        assertEquals(72, defaults.notificationHistoryRetentionHours)
        val clamped = SmartIslandSettings.fromJson("""{"settings": {"notificationHistoryRetentionHours": 100000}}""")
        assertEquals(720, clamped.notificationHistoryRetentionHours)
    }
}
