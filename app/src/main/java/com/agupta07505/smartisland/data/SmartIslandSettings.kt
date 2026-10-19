/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotPalette
import com.agupta07505.smartisland.buddy.model.MascotShape
import com.agupta07505.smartisland.buddy.model.MascotSide
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.MascotVisitFrequency
import com.agupta07505.smartisland.ui.SpringPreset
import org.json.JSONArray
import org.json.JSONObject

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f,
    val opacity: Float = 1f,
    val pillColor: Long = 0xFF000000L,
    val batteryColor: Long = 0xFF10B981L,
    val notificationDotColor: Long = 0xFF2563EBL,
    val musicVisualizerColor: Long = 0xFFFF6B9AL,
    val hotspotColor: Long = 0xFFF59E0BL,
    val callColor: Long = 0xFF22C55EL,
    val liveActivityColor: Long = 0xFF8B5CF6L,
    val transferColor: Long = 0xFF06B6D4L,
    val navigationColor: Long = 0xFF10B981L,
    val bluetoothColor: Long = 0xFF2563EBL,
    val flashlightColor: Long = 0xFFF59E0BL,
    val screenRecordingColor: Long = 0xFFEF4444L,
    val timerColor: Long = 0xFFF59E0BL,
    val stopwatchColor: Long = 0xFF06B6D4L,
    val enableAppShortcuts: Boolean = true,
    val shortcutPackages: Set<String> = emptySet(),
    val showRecentApps: Boolean = false,
    val welcomeDialogShown: Boolean = false,
    val showOnLockScreen: Boolean = false,
    val lockScreenPrivacy: String = "AppIconOnly",
    val showNotificationActions: Boolean = true,
    val hideFromNotificationShade: Boolean = true,
    val liveActivitiesEnabled: Boolean = true,
    val navigationEnabled: Boolean = true,
    val disabledNotificationPackages: Set<String> = emptySet(),
    val disabledSoundPackages: Set<String> = emptySet(),
    val hideWhenIdle: Boolean = false,
    val autoHidePill: Boolean = false,
    val autoHideTimeoutSeconds: Int = 5,
    val showInLandscape: Boolean = false,
    val autoExpandOnNotification: Boolean = true,
    val enableShadow: Boolean = true,
    val shadowElevation: Float = 14f,
    val enableMusicArtworkBackground: Boolean = true,
    val enableNotificationBackdrop: Boolean = true,
    val deviceType: String = "AUTO",
    val enableNotificationHistory: Boolean = false,
    val notificationHistoryRetentionHours: Int = 72,
    val showBluetoothBattery: Boolean = true,
    val enableBatteryMode: Boolean = true,
    val enableNotificationCooldown: Boolean = false,
    val notificationCooldownDurationMinutes: Int = 3,
    val notificationCooldownThreshold: Int = 3,
    val notificationCooldownExcludedPackages: Set<String> = emptySet(),
    val developerModeEnabled: Boolean = true,
    val recordLogs: Boolean = false,
    val enableNotchMode: Boolean = false,
    val enableSwipeActions: Boolean = true,
    val swipeUpAction: String = "DismissCurrent",
    val swipeHoldUpAction: String = "DismissAll",
    val swipeDownAction: String = "FloatingWindow",
    val swipeDownCollapsedAction: String = "Expand",
    val swipeHorizontalCollapsedAction: String = "NextPrevious",
    val enablePillSwipeActions: Boolean = true,
    val pillSwipeUpAction: String = "DismissCurrent",
    val pillSwipeDownAction: String = "Expand",
    val pillSwipeLeftAction: String = "PreviousNotification",
    val pillSwipeRightAction: String = "NextNotification",
    val circlePosition: String = CIRCLE_POSITION_RIGHT,
    // Buddy (the resting mascot). Stored as stable ids; unknown ids fall back to the default.
    val buddyEnabled: Boolean = false,
    val buddyStyle: String = MascotStyle.Minimal.id,
    val buddyShape: String = MascotShape.Ball.id,
    val buddyPalette: String = MascotPalette.Pearl.id,
    val buddySide: String = MascotSide.Left.id,
    val buddyVisits: String = MascotVisitFrequency.Normal.id,
    /** [com.agupta07505.smartisland.ui.SpringPreset] id: how the island's size springs behave. */
    val springPreset: String = SpringPreset.Standard.id,
    /** Whether a faint glow flares while the island is moving fast ([com.agupta07505.smartisland.ui.KineticGlow]). */
    val kineticGlow: Boolean = true,
    val buddyReacts: Boolean = true,
    val buddyHidesWhenIdle: Boolean = true,
    val aodEnabled: Boolean = false,
    val secureAppPackages: Set<String> = emptySet()
) {
    val buddyLook: MascotLook
        get() = MascotLook(
            MascotStyle.fromId(buddyStyle),
            MascotShape.fromId(buddyShape),
            MascotPalette.fromId(buddyPalette)
        )
    val buddySideValue: MascotSide get() = MascotSide.fromId(buddySide)
    val buddyVisitFrequency: MascotVisitFrequency get() = MascotVisitFrequency.fromId(buddyVisits)

    fun toJson(appVersion: String = ""): String {
        val root = JSONObject()
        root.put("formatVersion", BACKUP_FORMAT_VERSION)
        root.put("app", "com.agupta07505.smartisland")
        if (appVersion.isNotBlank()) {
            root.put("appVersion", appVersion)
        }
        root.put("exportTimestamp", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("enabled", enabled)
        settingsObj.put("width", width.toDouble())
        settingsObj.put("height", height.toDouble())
        settingsObj.put("xOffset", xOffset.toDouble())
        settingsObj.put("yOffset", yOffset.toDouble())
        settingsObj.put("cornerRadius", cornerRadius.toDouble())
        settingsObj.put("opacity", opacity.toDouble())
        settingsObj.put("pillColor", pillColor)
        settingsObj.put("batteryColor", batteryColor)
        settingsObj.put("notificationDotColor", notificationDotColor)
        settingsObj.put("musicVisualizerColor", musicVisualizerColor)
        settingsObj.put("hotspotColor", hotspotColor)
        settingsObj.put("callColor", callColor)
        settingsObj.put("liveActivityColor", liveActivityColor)
        settingsObj.put("transferColor", transferColor)
        settingsObj.put("navigationColor", navigationColor)
        settingsObj.put("bluetoothColor", bluetoothColor)
        settingsObj.put("flashlightColor", flashlightColor)
        settingsObj.put("screenRecordingColor", screenRecordingColor)
        settingsObj.put("timerColor", timerColor)
        settingsObj.put("stopwatchColor", stopwatchColor)
        settingsObj.put("enableAppShortcuts", enableAppShortcuts)

        val shortcutsArray = JSONArray()
        shortcutPackages.forEach { shortcutsArray.put(it) }
        settingsObj.put("shortcutPackages", shortcutsArray)

        settingsObj.put("showRecentApps", showRecentApps)
        settingsObj.put("welcomeDialogShown", welcomeDialogShown)
        settingsObj.put("showOnLockScreen", showOnLockScreen)
        settingsObj.put("lockScreenPrivacy", lockScreenPrivacy)
        settingsObj.put("showNotificationActions", showNotificationActions)
        settingsObj.put("hideFromNotificationShade", hideFromNotificationShade)
        settingsObj.put("liveActivitiesEnabled", liveActivitiesEnabled)
        settingsObj.put("navigationEnabled", navigationEnabled)

        val disabledNotifArray = JSONArray()
        disabledNotificationPackages.forEach { disabledNotifArray.put(it) }
        settingsObj.put("disabledNotificationPackages", disabledNotifArray)

        val disabledSoundArray = JSONArray()
        disabledSoundPackages.forEach { disabledSoundArray.put(it) }
        settingsObj.put("disabledSoundPackages", disabledSoundArray)

        settingsObj.put("hideWhenIdle", hideWhenIdle)
        settingsObj.put("autoHidePill", autoHidePill)
        settingsObj.put("autoHideTimeoutSeconds", autoHideTimeoutSeconds)
        settingsObj.put("showInLandscape", showInLandscape)
        settingsObj.put("autoExpandOnNotification", autoExpandOnNotification)
        settingsObj.put("enableShadow", enableShadow)
        settingsObj.put("shadowElevation", shadowElevation.toDouble())
        settingsObj.put("enableMusicArtworkBackground", enableMusicArtworkBackground)
        settingsObj.put("enableNotificationBackdrop", enableNotificationBackdrop)
        settingsObj.put("deviceType", deviceType)
        settingsObj.put("enableNotificationHistory", enableNotificationHistory)
        settingsObj.put("notificationHistoryRetentionHours", notificationHistoryRetentionHours)
        settingsObj.put("showBluetoothBattery", showBluetoothBattery)
        settingsObj.put("enableBatteryMode", enableBatteryMode)
        settingsObj.put("enableNotificationCooldown", enableNotificationCooldown)
        settingsObj.put("notificationCooldownDurationMinutes", notificationCooldownDurationMinutes)
        settingsObj.put("notificationCooldownThreshold", notificationCooldownThreshold)

        val cooldownExcludedArray = JSONArray()
        notificationCooldownExcludedPackages.forEach { cooldownExcludedArray.put(it) }
        settingsObj.put("notificationCooldownExcludedPackages", cooldownExcludedArray)

        settingsObj.put("developerModeEnabled", developerModeEnabled)
        settingsObj.put("recordLogs", recordLogs)
        settingsObj.put("enableNotchMode", enableNotchMode)
        settingsObj.put("enableSwipeActions", enableSwipeActions)
        settingsObj.put("swipeUpAction", swipeUpAction)
        settingsObj.put("swipeHoldUpAction", swipeHoldUpAction)
        settingsObj.put("swipeDownAction", swipeDownAction)
        settingsObj.put("swipeDownCollapsedAction", swipeDownCollapsedAction)
        settingsObj.put("swipeHorizontalCollapsedAction", swipeHorizontalCollapsedAction)
        settingsObj.put("enablePillSwipeActions", enablePillSwipeActions)
        settingsObj.put("pillSwipeUpAction", pillSwipeUpAction)
        settingsObj.put("pillSwipeDownAction", pillSwipeDownAction)
        settingsObj.put("pillSwipeLeftAction", pillSwipeLeftAction)
        settingsObj.put("pillSwipeRightAction", pillSwipeRightAction)
        settingsObj.put("circlePosition", circlePosition)
        settingsObj.put("buddyEnabled", buddyEnabled)
        settingsObj.put("buddyStyle", buddyStyle)
        settingsObj.put("buddyShape", buddyShape)
        settingsObj.put("buddyPalette", buddyPalette)
        settingsObj.put("buddySide", buddySide)
        settingsObj.put("buddyVisits", buddyVisits)
        settingsObj.put("springPreset", springPreset)
        settingsObj.put("kineticGlow", kineticGlow)
        settingsObj.put("buddyReacts", buddyReacts)
        settingsObj.put("buddyHidesWhenIdle", buddyHidesWhenIdle)
        settingsObj.put("aodEnabled", aodEnabled)
        val secureAppsArray = JSONArray()
        secureAppPackages.forEach { secureAppsArray.put(it) }
        settingsObj.put("secureAppPackages", secureAppsArray)

        root.put("settings", settingsObj)
        return root.toString(2)
    }

    data class BackupMetadata(
        val formatVersion: Int,
        val appVersion: String,
        val timestamp: Long,
        val settingsCount: Int
    )

    companion object {
        const val CIRCLE_POSITION_RIGHT = "right"
        const val CIRCLE_POSITION_LEFT = "left"

        val Default = SmartIslandSettings()

        const val BACKUP_FORMAT_VERSION = 1

        const val MIN_WIDTH = 76f
        const val MAX_WIDTH = 180f
        const val MIN_HEIGHT = 24f
        const val MAX_HEIGHT = 60f
        const val MIN_X_OFFSET = -140f
        const val MAX_X_OFFSET = 140f
        const val MIN_Y_OFFSET = 0f
        const val MAX_Y_OFFSET = 80f
        const val MIN_CORNER_RADIUS = 8f
        const val MAX_CORNER_RADIUS = 40f
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 1f
        const val MIN_SHADOW_ELEVATION = 0f
        const val MAX_SHADOW_ELEVATION = 32f

        fun parseBackupMetadata(jsonString: String): BackupMetadata? {
            return runCatching {
                val root = JSONObject(jsonString)
                val formatVersion = root.optInt("formatVersion", 1)
                val appVersion = root.optString("appVersion", "Unknown")
                val timestamp = root.optLong("exportTimestamp", System.currentTimeMillis())
                val settingsObj = root.optJSONObject("settings") ?: root
                BackupMetadata(
                    formatVersion = formatVersion,
                    appVersion = appVersion,
                    timestamp = timestamp,
                    settingsCount = settingsObj.length()
                )
            }.getOrNull()
        }

        fun fromJson(jsonString: String): SmartIslandSettings {
            val root = JSONObject(jsonString)
            val obj = root.optJSONObject("settings") ?: root
            val defaults = Default

            fun safeFloat(key: String, default: Float, min: Float, max: Float): Float {
                val raw = if (obj.has(key)) obj.optDouble(key, default.toDouble()).toFloat() else default
                return if (raw.isFinite()) raw.coerceIn(min, max) else default
            }

            fun safeColor(key: String, default: Long): Long {
                return if (obj.has(key)) {
                    val color = obj.optLong(key, default)
                    if (color != 0L) color else default
                } else default
            }

            fun safeStringSet(key: String, default: Set<String>, maxItems: Int = Int.MAX_VALUE): Set<String> {
                val arr = obj.optJSONArray(key) ?: return default
                val result = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    val pkg = arr.optString(i, "").trim()
                    if (pkg.isNotBlank() && pkg.length <= 255) {
                        result.add(pkg)
                        if (result.size >= maxItems) break
                    }
                }
                return result
            }

            return SmartIslandSettings(
                enabled = obj.optBoolean("enabled", defaults.enabled),
                width = safeFloat("width", defaults.width, MIN_WIDTH, MAX_WIDTH),
                height = safeFloat("height", defaults.height, MIN_HEIGHT, MAX_HEIGHT),
                xOffset = safeFloat("xOffset", defaults.xOffset, MIN_X_OFFSET, MAX_X_OFFSET),
                yOffset = safeFloat("yOffset", defaults.yOffset, MIN_Y_OFFSET, MAX_Y_OFFSET),
                cornerRadius = safeFloat("cornerRadius", defaults.cornerRadius, MIN_CORNER_RADIUS, MAX_CORNER_RADIUS),
                opacity = safeFloat("opacity", defaults.opacity, MIN_OPACITY, MAX_OPACITY),
                pillColor = safeColor("pillColor", defaults.pillColor),
                batteryColor = safeColor("batteryColor", defaults.batteryColor),
                notificationDotColor = safeColor("notificationDotColor", defaults.notificationDotColor),
                musicVisualizerColor = safeColor("musicVisualizerColor", defaults.musicVisualizerColor),
                hotspotColor = safeColor("hotspotColor", defaults.hotspotColor),
                callColor = safeColor("callColor", defaults.callColor),
                liveActivityColor = safeColor("liveActivityColor", defaults.liveActivityColor),
                transferColor = safeColor("transferColor", defaults.transferColor),
                navigationColor = safeColor("navigationColor", defaults.navigationColor),
                bluetoothColor = safeColor("bluetoothColor", defaults.bluetoothColor),
                flashlightColor = safeColor("flashlightColor", defaults.flashlightColor),
                screenRecordingColor = safeColor("screenRecordingColor", defaults.screenRecordingColor),
                timerColor = safeColor("timerColor", defaults.timerColor),
                stopwatchColor = safeColor("stopwatchColor", defaults.stopwatchColor),
                enableAppShortcuts = obj.optBoolean("enableAppShortcuts", defaults.enableAppShortcuts),
                shortcutPackages = safeStringSet("shortcutPackages", defaults.shortcutPackages, 8),
                showRecentApps = obj.optBoolean("showRecentApps", defaults.showRecentApps),
                welcomeDialogShown = obj.optBoolean("welcomeDialogShown", defaults.welcomeDialogShown),
                showOnLockScreen = obj.optBoolean("showOnLockScreen", defaults.showOnLockScreen),
                lockScreenPrivacy = obj.optString("lockScreenPrivacy", defaults.lockScreenPrivacy).let {
                    if (it == "AppIconOnly" || it == "FullContent") it else defaults.lockScreenPrivacy
                },
                showNotificationActions = obj.optBoolean("showNotificationActions", defaults.showNotificationActions),
                hideFromNotificationShade = obj.optBoolean("hideFromNotificationShade", defaults.hideFromNotificationShade),
                liveActivitiesEnabled = obj.optBoolean("liveActivitiesEnabled", defaults.liveActivitiesEnabled),
                navigationEnabled = obj.optBoolean("navigationEnabled", defaults.navigationEnabled),
                disabledNotificationPackages = safeStringSet("disabledNotificationPackages", defaults.disabledNotificationPackages),
                disabledSoundPackages = safeStringSet("disabledSoundPackages", defaults.disabledSoundPackages),
                hideWhenIdle = obj.optBoolean("hideWhenIdle", defaults.hideWhenIdle),
                autoHidePill = obj.optBoolean("autoHidePill", defaults.autoHidePill),
                autoHideTimeoutSeconds = obj.optInt("autoHideTimeoutSeconds", defaults.autoHideTimeoutSeconds).coerceIn(1, 120),
                showInLandscape = obj.optBoolean("showInLandscape", defaults.showInLandscape),
                autoExpandOnNotification = obj.optBoolean("autoExpandOnNotification", defaults.autoExpandOnNotification),
                enableShadow = obj.optBoolean("enableShadow", defaults.enableShadow),
                shadowElevation = safeFloat("shadowElevation", defaults.shadowElevation, MIN_SHADOW_ELEVATION, MAX_SHADOW_ELEVATION),
                enableMusicArtworkBackground = obj.optBoolean("enableMusicArtworkBackground", defaults.enableMusicArtworkBackground),
                enableNotificationBackdrop = obj.optBoolean("enableNotificationBackdrop", defaults.enableNotificationBackdrop),
                deviceType = obj.optString("deviceType", defaults.deviceType),
                enableNotificationHistory = obj.optBoolean("enableNotificationHistory", defaults.enableNotificationHistory),
                notificationHistoryRetentionHours = obj.optInt("notificationHistoryRetentionHours", defaults.notificationHistoryRetentionHours).coerceIn(1, 720),
                showBluetoothBattery = obj.optBoolean("showBluetoothBattery", defaults.showBluetoothBattery),
                enableBatteryMode = obj.optBoolean("enableBatteryMode", defaults.enableBatteryMode),
                enableNotificationCooldown = obj.optBoolean("enableNotificationCooldown", defaults.enableNotificationCooldown),
                notificationCooldownDurationMinutes = obj.optInt("notificationCooldownDurationMinutes", defaults.notificationCooldownDurationMinutes).coerceIn(1, 60),
                notificationCooldownThreshold = obj.optInt("notificationCooldownThreshold", defaults.notificationCooldownThreshold).coerceIn(2, 20),
                notificationCooldownExcludedPackages = safeStringSet("notificationCooldownExcludedPackages", defaults.notificationCooldownExcludedPackages),
                developerModeEnabled = obj.optBoolean("developerModeEnabled", defaults.developerModeEnabled),
                recordLogs = obj.optBoolean("recordLogs", defaults.recordLogs),
                enableNotchMode = obj.optBoolean("enableNotchMode", defaults.enableNotchMode),
                enableSwipeActions = obj.optBoolean("enableSwipeActions", defaults.enableSwipeActions),
                swipeUpAction = obj.optString("swipeUpAction", defaults.swipeUpAction),
                swipeHoldUpAction = obj.optString("swipeHoldUpAction", defaults.swipeHoldUpAction),
                swipeDownAction = obj.optString("swipeDownAction", defaults.swipeDownAction),
                swipeDownCollapsedAction = obj.optString("swipeDownCollapsedAction", defaults.swipeDownCollapsedAction),
                swipeHorizontalCollapsedAction = obj.optString("swipeHorizontalCollapsedAction", defaults.swipeHorizontalCollapsedAction),
                enablePillSwipeActions = obj.optBoolean("enablePillSwipeActions", defaults.enablePillSwipeActions),
                pillSwipeUpAction = obj.optString("pillSwipeUpAction", defaults.pillSwipeUpAction),
                pillSwipeDownAction = obj.optString("pillSwipeDownAction", defaults.pillSwipeDownAction),
                pillSwipeLeftAction = obj.optString("pillSwipeLeftAction", defaults.pillSwipeLeftAction),
                pillSwipeRightAction = obj.optString("pillSwipeRightAction", defaults.pillSwipeRightAction),
                circlePosition = obj.optString("circlePosition", defaults.circlePosition).let {
                    if (it == CIRCLE_POSITION_LEFT || it == CIRCLE_POSITION_RIGHT) it else defaults.circlePosition
                },
                buddyEnabled = obj.optBoolean("buddyEnabled", defaults.buddyEnabled),
                buddyStyle = MascotStyle.fromId(obj.optString("buddyStyle", defaults.buddyStyle)).id,
                buddyShape = MascotShape.fromId(obj.optString("buddyShape", defaults.buddyShape)).id,
                buddyPalette = MascotPalette.fromId(obj.optString("buddyPalette", defaults.buddyPalette)).id,
                buddySide = MascotSide.fromId(obj.optString("buddySide", defaults.buddySide)).id,
                buddyVisits = MascotVisitFrequency.fromId(obj.optString("buddyVisits", defaults.buddyVisits)).id,
                springPreset = SpringPreset.fromId(obj.optString("springPreset", defaults.springPreset)).id,
                kineticGlow = obj.optBoolean("kineticGlow", defaults.kineticGlow),
                buddyReacts = obj.optBoolean("buddyReacts", defaults.buddyReacts),
                buddyHidesWhenIdle = obj.optBoolean("buddyHidesWhenIdle", defaults.buddyHidesWhenIdle),
                aodEnabled = obj.optBoolean("aodEnabled", defaults.aodEnabled),
                secureAppPackages = safeStringSet("secureAppPackages", defaults.secureAppPackages)
            )
        }
    }
}
