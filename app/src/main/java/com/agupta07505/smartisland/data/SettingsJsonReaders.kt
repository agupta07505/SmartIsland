/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.BACKUP_FORMAT_VERSION
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.CIRCLE_POSITION_LEFT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.CIRCLE_POSITION_RIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_POSITION_AUTO
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_POSITION_LEFT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_POSITION_MIDDLE
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_POSITION_RIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_SHAPE_CIRCLE
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LANDSCAPE_SHAPE_PILL
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.LEGACY_IDLE_TIMEOUT_NEVER_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_AUTO_HIDE_SECONDS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_BATTERY_WARNING_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CIRCLE_EDGE_INSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CIRCLE_SIZE
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CONTENT_PILL_PERCENT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CORNER_RADIUS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CUTOUT_HEIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CUTOUT_WIDTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_CUTOUT_X
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_EXPANDED_BLUR
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_GRADIENT_STRENGTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_HEIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_IDLE_TIMEOUT_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_LANDSCAPE_X_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_LANDSCAPE_Y_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_OPACITY
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_PULSE_HOLD_PERCENT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_SECONDARY_MERGE_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_SHADOW_ELEVATION
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_TRANSFER_EXPIRY_MINUTES
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_WIDTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_X_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MAX_Y_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_AUTO_HIDE_SECONDS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_BATTERY_WARNING_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CIRCLE_EDGE_INSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CIRCLE_OPACITY
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CIRCLE_SIZE
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CONTENT_PILL_PERCENT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CORNER_RADIUS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CUTOUT_HEIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CUTOUT_WIDTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_CUTOUT_X
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_EXPANDED_BLUR
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_GRADIENT_STRENGTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_HEIGHT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_IDLE_TIMEOUT_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_OPACITY
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_PULSE_HOLD_PERCENT
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_SECONDARY_MERGE_MS
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_SHADOW_ELEVATION
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_WIDTH
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_X_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.MIN_Y_OFFSET
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.VALID_EXPANDED_THEMES
import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.isValidFont
import com.agupta07505.smartisland.model.EdgePillTuning
import com.agupta07505.smartisland.model.EventColors
import com.agupta07505.smartisland.model.FloatingWindowMode
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.HapticStrength
import org.json.JSONObject

internal class BackupReader(val obj: JSONObject) {
    fun safeFloat(key: String, default: Float, min: Float, max: Float): Float {
        val raw = if (obj.has(key)) obj.optDouble(key, default.toDouble()).toFloat() else default
        return if (raw.isFinite()) raw.coerceIn(min, max) else default
    }

    // a merge style id: unknown or missing values fall back to [fallback]
    fun mergeId(key: String, fallback: String): String =
        obj.optString(key, fallback).takeIf { it in MergeStyle.VALID_IDS } ?: fallback

    /** The colours given to single events; an unknown event or a bad colour is dropped, a missing object means none. */
    fun readEventColorsObject(key: String = "eventColors"): Map<String, Long> {
        val colors = obj.optJSONObject(key) ?: return emptyMap()
        val parsed = LinkedHashMap<String, Long>()
        for (id in colors.keys()) {
            val color = colors.optLong(id, 0L)
            if (color != 0L) parsed[id] = color
        }
        return EventColors.clean(parsed)
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
}

internal fun SmartIslandSettings.readJsonPillLookAndColors(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        enabled = obj.optBoolean("enabled", defaults.enabled),
        width = safeFloat("width", defaults.width, MIN_WIDTH, MAX_WIDTH),
        height = safeFloat("height", defaults.height, MIN_HEIGHT, MAX_HEIGHT),
        xOffset = safeFloat("xOffset", defaults.xOffset, MIN_X_OFFSET, MAX_X_OFFSET),
        yOffset = safeFloat("yOffset", defaults.yOffset, MIN_Y_OFFSET, MAX_Y_OFFSET),
        cornerRadius = safeFloat("cornerRadius", defaults.cornerRadius, MIN_CORNER_RADIUS, MAX_CORNER_RADIUS),
        opacity = safeFloat("opacity", defaults.opacity, MIN_OPACITY, MAX_OPACITY),
        pillColor = safeColor("pillColor", defaults.pillColor),
        oledBlack = obj.optBoolean("oledBlack", defaults.oledBlack),
        modeColors = ModeColors(
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
            stopwatchColor = safeColor("stopwatchColor", defaults.stopwatchColor)
        ),
        eventColors = readEventColorsObject()
    )
}

internal fun SmartIslandSettings.readJsonShortcutsAndNotificationRules(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        enableAppShortcuts = obj.optBoolean("enableAppShortcuts", defaults.enableAppShortcuts),
        shortcutPackages = obj.optJSONArray("shortcutPackages")?.let { arr ->
            PinnedApps.normalize((0 until arr.length()).map { arr.optString(it, "") })
        } ?: defaults.shortcutPackages,
        showRecentApps = obj.optBoolean("showRecentApps", defaults.showRecentApps),
        utilityLayout = UtilityLayout.encode(
            UtilityLayout.decode(obj.optString("utilityLayout", defaults.utilityLayout))
        ),
        utilityCalculatorPackage = obj.optString("utilityCalculatorPackage", defaults.utilityCalculatorPackage)
            .take(255),
        utilityCalculatorFloating = obj.optBoolean("utilityCalculatorFloating", defaults.utilityCalculatorFloating),
        welcomeDialogShown = obj.optBoolean("welcomeDialogShown", defaults.welcomeDialogShown),
        torchStrobeAcknowledged = obj.optBoolean("torchStrobeAcknowledged", defaults.torchStrobeAcknowledged),
        showOnLockScreen = obj.optBoolean("showOnLockScreen", defaults.showOnLockScreen),
        showInForegroundEvents = when {
            obj.has("showInForegroundEvents") ->
                safeStringSet("showInForegroundEvents", defaults.showInForegroundEvents)
                    .filterTo(LinkedHashSet()) { id -> IslandEvent.FOREGROUND_SWITCHABLE.any { it.id == id } }
            // a backup from before the per-event switches
            obj.optBoolean("showLiveEventsInForeground", false) ->
                IslandEvent.FOREGROUND_SWITCHABLE.mapTo(LinkedHashSet()) { it.id }
            else -> defaults.showInForegroundEvents
        },
        hideInFullscreen = obj.optBoolean("hideInFullscreen", defaults.hideInFullscreen),
        hideInShade = obj.optBoolean("hideInShade", defaults.hideInShade),
        lockScreenPrivacy = obj.optString("lockScreenPrivacy", defaults.lockScreenPrivacy).let {
            if (it == "AppIconOnly" || it == "FullContent") it else defaults.lockScreenPrivacy
        },
        showNotificationActions = obj.optBoolean("showNotificationActions", defaults.showNotificationActions),
        hideFromNotificationShade = obj.optBoolean("hideFromNotificationShade", defaults.hideFromNotificationShade),
        liveActivitiesEnabled = obj.optBoolean("liveActivitiesEnabled", defaults.liveActivitiesEnabled),
        navigationEnabled = obj.optBoolean("navigationEnabled", defaults.navigationEnabled),
        disabledNotificationPackages = safeStringSet("disabledNotificationPackages",
            defaults.disabledNotificationPackages),
        disabledSoundPackages = safeStringSet("disabledSoundPackages", defaults.disabledSoundPackages),
        disabledEvents = safeStringSet("disabledEvents", defaults.disabledEvents)
    )
}

internal fun SmartIslandSettings.readJsonDisplayAndExpandedStyle(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        hideWhenIdle = obj.optBoolean("hideWhenIdle", defaults.hideWhenIdle),
        autoHidePill = obj.optBoolean("autoHidePill", defaults.autoHidePill),
        autoHideTimeoutSeconds = obj.optInt("autoHideTimeoutSeconds", defaults.autoHideTimeoutSeconds)
            .coerceIn(MIN_AUTO_HIDE_SECONDS, MAX_AUTO_HIDE_SECONDS),
        showInLandscape = obj.optBoolean("showInLandscape", defaults.showInLandscape),
        autoExpandOnNotification = obj.optBoolean("autoExpandOnNotification", defaults.autoExpandOnNotification),
        autoExpandOnLockScreen = obj.optBoolean("autoExpandOnLockScreen", defaults.autoExpandOnLockScreen),
        showSuggestedReplies = obj.optBoolean("showSuggestedReplies", defaults.showSuggestedReplies),
        showCopyCodeChip = obj.optBoolean("showCopyCodeChip", defaults.showCopyCodeChip),
        enableShadow = obj.optBoolean("enableShadow", defaults.enableShadow),
        expandedBlurEnabled = obj.optBoolean("expandedBlurEnabled", defaults.expandedBlurEnabled),
        expandedBlurRadius = safeFloat("expandedBlurRadius", defaults.expandedBlurRadius, MIN_EXPANDED_BLUR,
            MAX_EXPANDED_BLUR),
        // Old backups have no media keys
        mediaBlurEnabled = obj.optBoolean("mediaBlurEnabled",
            obj.optBoolean("expandedBlurEnabled", defaults.mediaBlurEnabled)),
        mediaBlurRadius = safeFloat(
            "mediaBlurRadius",
            safeFloat("expandedBlurRadius", defaults.mediaBlurRadius, MIN_EXPANDED_BLUR, MAX_EXPANDED_BLUR),
            MIN_EXPANDED_BLUR, MAX_EXPANDED_BLUR
        ),
        mediaOpacity = safeFloat(
            "mediaOpacity",
            safeFloat("opacity", defaults.mediaOpacity, MIN_OPACITY, MAX_OPACITY),
            MIN_OPACITY, MAX_OPACITY
        ),
        expandedGradientEnabled = obj.optBoolean("expandedGradientEnabled", defaults.expandedGradientEnabled),
        expandedGradientColor = safeColor("expandedGradientColor", defaults.expandedGradientColor),
        expandedGradientStrength = safeFloat("expandedGradientStrength", defaults.expandedGradientStrength,
            MIN_GRADIENT_STRENGTH, MAX_GRADIENT_STRENGTH),
        shadowElevation = safeFloat("shadowElevation", defaults.shadowElevation, MIN_SHADOW_ELEVATION,
            MAX_SHADOW_ELEVATION),
        enableMusicArtworkBackground = obj.optBoolean("enableMusicArtworkBackground",
            defaults.enableMusicArtworkBackground),
        enableNotificationBackdrop = obj.optBoolean("enableNotificationBackdrop", defaults.enableNotificationBackdrop)
    )
}

internal fun SmartIslandSettings.readJsonAppBehaviour(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        deviceType = obj.optString("deviceType", defaults.deviceType),
        allowNetworkChecks = obj.optBoolean("allowNetworkChecks", defaults.allowNetworkChecks),
        detectTransfersBySpeed = obj.optBoolean("detectTransfersBySpeed", defaults.detectTransfersBySpeed),
        enableNotificationHistory = obj.optBoolean("enableNotificationHistory", defaults.enableNotificationHistory),
        notificationHistoryRetentionHours = obj.optInt("notificationHistoryRetentionHours",
            defaults.notificationHistoryRetentionHours).coerceIn(1, 720),
        showBluetoothBattery = obj.optBoolean("showBluetoothBattery", defaults.showBluetoothBattery),
        enableBatteryMode = obj.optBoolean("enableBatteryMode", defaults.enableBatteryMode),
        enableNotificationCooldown = obj.optBoolean("enableNotificationCooldown", defaults.enableNotificationCooldown),
        notificationCooldownDurationMinutes = obj.optInt("notificationCooldownDurationMinutes",
            defaults.notificationCooldownDurationMinutes).coerceIn(1, 60),
        notificationCooldownThreshold = obj.optInt("notificationCooldownThreshold",
            defaults.notificationCooldownThreshold).coerceIn(2, 20),
        notificationCooldownExcludedPackages = safeStringSet("notificationCooldownExcludedPackages",
            defaults.notificationCooldownExcludedPackages),
        notificationCooldownSystemEvents = safeStringSet("notificationCooldownSystemEvents",
            defaults.notificationCooldownSystemEvents),
        developerModeEnabled = obj.optBoolean("developerModeEnabled", defaults.developerModeEnabled),
        recordLogs = obj.optBoolean("recordLogs", defaults.recordLogs),
        faceUnlockMode = obj.optString("faceUnlockMode", defaults.faceUnlockMode)
            .takeIf { it in setOf("face_only", "any") } ?: defaults.faceUnlockMode,
        faceIdStyle = obj.optString("faceIdStyle", defaults.faceIdStyle)
            .takeIf { it in setOf("square", "cutout", "square_tick") } ?: defaults.faceIdStyle,
        appFaceIdEnabled = obj.optBoolean("appFaceIdEnabled", defaults.appFaceIdEnabled),
        appFaceIdStyle = obj.optString("appFaceIdStyle", defaults.appFaceIdStyle)
            .takeIf { it in setOf("square", "cutout", "square_tick") } ?: defaults.appFaceIdStyle,
        statusBarHidden = obj.optBoolean("statusBarHidden", defaults.statusBarHidden),
        statusBarHidePersistent = obj.optBoolean("statusBarHidePersistent", defaults.statusBarHidePersistent),
        statusBarHideNotificationIcons = obj.optBoolean("statusBarHideNotificationIcons",
            defaults.statusBarHideNotificationIcons),
        statusBarHideClock = obj.optBoolean("statusBarHideClock", defaults.statusBarHideClock),
        statusBarHideAllSystemIcons = obj.optBoolean("statusBarHideAllSystemIcons",
            defaults.statusBarHideAllSystemIcons),
        statusBarHiddenSystemIcons = safeStringSet("statusBarHiddenSystemIcons", defaults.statusBarHiddenSystemIcons),
        fingerprintAnimationEnabled = obj.optBoolean("fingerprintAnimationEnabled",
            defaults.fingerprintAnimationEnabled)
    )
}

internal fun SmartIslandSettings.readJsonThemeAndCutout(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        themeMode = obj.optString("themeMode", defaults.themeMode).takeIf { it in setOf("system", "light", "dark") }
            ?: defaults.themeMode,
        liquidGlassTheme = obj.optBoolean("liquidGlassTheme", defaults.liquidGlassTheme),
        materialYou = obj.optBoolean("materialYou", defaults.materialYou),
        accentColor = safeColor("accentColor", defaults.accentColor),
        expandedTheme = obj.optString("expandedTheme", defaults.expandedTheme).takeIf { it in VALID_EXPANDED_THEMES }
            ?: defaults.expandedTheme,
        islandFont = obj.optString("islandFont", defaults.islandFont).takeIf { isValidFont(it) } ?: defaults.islandFont,
        enableNotchMode = obj.optBoolean("enableNotchMode", defaults.enableNotchMode),
        cameraCutoutManual = obj.optBoolean("cameraCutoutManual", defaults.cameraCutoutManual),
        cameraCutoutStyle = obj.optString("cameraCutoutStyle", defaults.cameraCutoutStyle)
            .takeIf { it in VALID_CUTOUT_STYLES } ?: defaults.cameraCutoutStyle,
        cameraCutoutX = safeFloat("cameraCutoutX", defaults.cameraCutoutX, MIN_CUTOUT_X, MAX_CUTOUT_X),
        cameraCutoutY = safeFloat("cameraCutoutY", defaults.cameraCutoutY, MIN_Y_OFFSET, MAX_Y_OFFSET),
        cameraCutoutWidth = safeFloat("cameraCutoutWidth", defaults.cameraCutoutWidth, MIN_CUTOUT_WIDTH,
            MAX_CUTOUT_WIDTH),
        cameraCutoutHeight = safeFloat("cameraCutoutHeight", defaults.cameraCutoutHeight, MIN_CUTOUT_HEIGHT,
            MAX_CUTOUT_HEIGHT)
    )
}

internal fun SmartIslandSettings.readJsonGestures(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        enableSwipeActions = obj.optBoolean("enableSwipeActions", defaults.enableSwipeActions),
        swipeUpAction = obj.optString("swipeUpAction", defaults.swipeUpAction),
        swipeHoldUpAction = obj.optString("swipeHoldUpAction", defaults.swipeHoldUpAction),
        swipeDownAction = obj.optString("swipeDownAction", defaults.swipeDownAction),
        swipeDownCollapsedAction = obj.optString("swipeDownCollapsedAction", defaults.swipeDownCollapsedAction),
        swipeLeftAction = obj.optString("swipeLeftAction", defaults.swipeLeftAction),
        swipeRightAction = obj.optString("swipeRightAction", defaults.swipeRightAction),
        swipeHorizontalCollapsedAction = obj.optString("swipeHorizontalCollapsedAction",
            defaults.swipeHorizontalCollapsedAction),
        enablePillSwipeActions = obj.optBoolean("enablePillSwipeActions", defaults.enablePillSwipeActions),
        pillSwipeUpAction = obj.optString("pillSwipeUpAction", defaults.pillSwipeUpAction),
        pillSwipeDownAction = obj.optString("pillSwipeDownAction", defaults.pillSwipeDownAction),
        pillSwipeLeftAction = obj.optString("pillSwipeLeftAction", defaults.pillSwipeLeftAction),
        pillSwipeRightAction = obj.optString("pillSwipeRightAction", defaults.pillSwipeRightAction),
        longPressAction = obj.optString("longPressAction", defaults.longPressAction)
    )
}

internal fun SmartIslandSettings.readJsonLandscape(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        circlePosition = obj.optString("circlePosition", defaults.circlePosition).let {
            if (it == CIRCLE_POSITION_LEFT || it == CIRCLE_POSITION_RIGHT) it else defaults.circlePosition
        },
        landscapeShape = obj.optString("landscapeShape", defaults.landscapeShape).let {
            if (it == LANDSCAPE_SHAPE_PILL || it == LANDSCAPE_SHAPE_CIRCLE) it else defaults.landscapeShape
        },
        landscapeWidth = safeFloat("landscapeWidth", defaults.landscapeWidth, MIN_WIDTH, MAX_WIDTH),
        landscapeHeight = safeFloat("landscapeHeight", defaults.landscapeHeight, MIN_HEIGHT, MAX_HEIGHT),
        landscapeXOffset = safeFloat("landscapeXOffset", defaults.landscapeXOffset, -MAX_LANDSCAPE_X_OFFSET,
            MAX_LANDSCAPE_X_OFFSET),
        landscapeYOffset = safeFloat("landscapeYOffset", defaults.landscapeYOffset, MIN_Y_OFFSET,
            MAX_LANDSCAPE_Y_OFFSET),
        landscapeCornerRadius = safeFloat("landscapeCornerRadius", defaults.landscapeCornerRadius, MIN_CORNER_RADIUS,
            MAX_CORNER_RADIUS),
        landscapeOpacity = safeFloat("landscapeOpacity", defaults.landscapeOpacity, MIN_OPACITY, MAX_OPACITY),
        landscapeCircleSize = safeFloat("landscapeCircleSize", defaults.landscapeCircleSize, MIN_CIRCLE_SIZE,
            MAX_CIRCLE_SIZE),
        landscapeCircleOpacity = safeFloat("landscapeCircleOpacity", defaults.landscapeCircleOpacity,
            MIN_CIRCLE_OPACITY, MAX_OPACITY),
        landscapeCircleEdgeInset = safeFloat("landscapeCircleEdgeInset", defaults.landscapeCircleEdgeInset,
            MIN_CIRCLE_EDGE_INSET, MAX_CIRCLE_EDGE_INSET),
        landscapeCircleYOffset = safeFloat("landscapeCircleYOffset", defaults.landscapeCircleYOffset, MIN_Y_OFFSET,
            MAX_LANDSCAPE_Y_OFFSET),
        landscapeCirclePosition = obj.optString("landscapeCirclePosition", defaults.landscapeCirclePosition).let {
            if (it == LANDSCAPE_POSITION_AUTO || it == LANDSCAPE_POSITION_LEFT ||
                it == LANDSCAPE_POSITION_RIGHT
                    || it == LANDSCAPE_POSITION_MIDDLE) it else defaults.landscapeCirclePosition
        },
        landscapeCircleXAdjust = safeFloat("landscapeCircleXAdjust", defaults.landscapeCircleXAdjust,
            -MAX_LANDSCAPE_X_OFFSET, MAX_LANDSCAPE_X_OFFSET),
        landscapeCircleYAdjust = safeFloat("landscapeCircleYAdjust", defaults.landscapeCircleYAdjust,
            -MAX_LANDSCAPE_Y_OFFSET, MAX_LANDSCAPE_Y_OFFSET),
        landscapePillPosition = obj.optString("landscapePillPosition", defaults.landscapePillPosition).let {
            if (it == LANDSCAPE_POSITION_AUTO || it == LANDSCAPE_POSITION_LEFT ||
                it == LANDSCAPE_POSITION_RIGHT
                    || it == LANDSCAPE_POSITION_MIDDLE) it else defaults.landscapePillPosition
        },
        landscapeLayoutStyle = LandscapeLayoutStyle.fromId(
            obj.optString("landscapeLayoutStyle", defaults.landscapeLayoutStyle)
        ),
        edgePill = EdgePillTuning.decode(obj.optString("edgePill", "")),
        disabledEventsLandscape = safeStringSet("disabledEventsLandscape", defaults.disabledEventsLandscape)
    )
}

internal fun SmartIslandSettings.readJsonTimingsAndIdleSwitches(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        idleTimeoutMs = obj.optInt("idleTimeoutMs", defaults.idleTimeoutMs).let { raw ->
            // a legacy "never" is kept as the Never chip
            if (!obj.has("idleTimeoutNever") && raw >= LEGACY_IDLE_TIMEOUT_NEVER_MS) defaults.idleTimeoutMs
            else raw.coerceIn(MIN_IDLE_TIMEOUT_MS, MAX_IDLE_TIMEOUT_MS)
        },
        idleTimeoutNever = if (obj.has("idleTimeoutNever")) obj.optBoolean("idleTimeoutNever", false)
        else obj.optInt("idleTimeoutMs", defaults.idleTimeoutMs) >= LEGACY_IDLE_TIMEOUT_NEVER_MS,
        batteryWarningMs = obj.optInt("batteryWarningMs", defaults.batteryWarningMs)
            .coerceIn(MIN_BATTERY_WARNING_MS, MAX_BATTERY_WARNING_MS),
        // a backup from before the setting has no key and imports as 100
        pulseHoldPercent = obj.optInt("pulseHoldPercent", defaults.pulseHoldPercent)
            .coerceIn(MIN_PULSE_HOLD_PERCENT, MAX_PULSE_HOLD_PERCENT),
        transferExpiryMinutes = obj.optInt("transferExpiryMinutes", defaults.transferExpiryMinutes)
            .coerceIn(0, MAX_TRANSFER_EXPIRY_MINUTES),
        idleWidgetsEnabled = obj.optBoolean("idleWidgetsEnabled", defaults.idleWidgetsEnabled),
        idleWidgetsEnabledLandscape = obj.optBoolean(
            "idleWidgetsEnabledLandscape",
            obj.optBoolean("idleWidgetsEnabled", defaults.idleWidgetsEnabledLandscape)
        ),
        idleOffAfterTapClose = obj.optBoolean("idleOffAfterTapClose", defaults.idleOffAfterTapClose),
        idleOffAfterTapCloseLandscape = obj.optBoolean("idleOffAfterTapCloseLandscape",
            defaults.idleOffAfterTapCloseLandscape),
        idleOffAfterLongPress = obj.optBoolean("idleOffAfterLongPress", defaults.idleOffAfterLongPress),
        idleOffAfterLongPressLandscape = obj.optBoolean("idleOffAfterLongPressLandscape",
            defaults.idleOffAfterLongPressLandscape),
        idleOffWhenStored = obj.optBoolean("idleOffWhenStored", defaults.idleOffWhenStored),
        contentPillSameAsIdle = obj.optBoolean("contentPillSameAsIdle", defaults.contentPillSameAsIdle),
        islandExemptApps = safeStringSet("islandExemptApps", defaults.islandExemptApps),
        islandExemptAppsLandscape = safeStringSet("islandExemptAppsLandscape", defaults.islandExemptAppsLandscape),
        islandExemptAppsStatic = safeStringSet("islandExemptAppsStatic", defaults.islandExemptAppsStatic),
        contentPillSameAsIdleLandscape = obj.optBoolean("contentPillSameAsIdleLandscape",
            defaults.contentPillSameAsIdleLandscape),
        contentPillWidthPercentLandscape = safeFloat(
            "contentPillWidthPercentLandscape", defaults.contentPillWidthPercentLandscape,
            MIN_CONTENT_PILL_PERCENT, MAX_CONTENT_PILL_PERCENT
        ),
        contentPillWidthPercent = safeFloat(
            "contentPillWidthPercent", defaults.contentPillWidthPercent, MIN_CONTENT_PILL_PERCENT,
                MAX_CONTENT_PILL_PERCENT
        ),
        idleOffWhenStoredLandscape = obj.optBoolean("idleOffWhenStoredLandscape", defaults.idleOffWhenStoredLandscape)
    )
}

internal fun SmartIslandSettings.readJsonIdleWidgets(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        idleGlyphTickSeconds = IdleSlots.normalizeTickSeconds(
            obj.optDouble("idleGlyphTickSeconds", defaults.idleGlyphTickSeconds.toDouble()).toFloat()
        ),
        idleWidgetIds = IdleWidgets.normalize(
            obj.optString("idleWidgetIds", defaults.idleWidgetIds)
        ),
        idleWidgetCycleSeconds = obj.optInt("idleWidgetCycleSeconds", defaults.idleWidgetCycleSeconds)
            .coerceIn(
                IdleWidgets.MIN_CYCLE_SECONDS,
                IdleWidgets.MAX_CYCLE_SECONDS
            ),
        idleAlarmHideBeyond12h = obj.optBoolean("idleAlarmHideBeyond12h", defaults.idleAlarmHideBeyond12h),
        batteryOnlyWhenCharging = obj.optBoolean("batteryOnlyWhenCharging", defaults.batteryOnlyWhenCharging),
        idleBeatOnlyOnChange = obj.optBoolean("idleBeatOnlyOnChange", defaults.idleBeatOnlyOnChange),
        widgetIslandCloseSeconds = WidgetIslandTimer.normalize(
            obj.optInt("widgetIslandCloseSeconds", defaults.widgetIslandCloseSeconds)
        ),
        widgetIslandBatteryMetric = obj.optString("widgetIslandBatteryMetric", defaults.widgetIslandBatteryMetric)
            .takeIf { it in WidgetIslandContent.VALID_BATTERY_METRICS } ?: defaults.widgetIslandBatteryMetric,
        idleGlyphFont = obj.optString("idleGlyphFont", defaults.idleGlyphFont)
            .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.idleGlyphFont,
        fontMixEnabled = obj.optBoolean("fontMixEnabled", defaults.fontMixEnabled),
        fontMixLight = obj.optString("fontMixLight", defaults.fontMixLight)
            .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixLight,
        fontMixRegular = obj.optString("fontMixRegular", defaults.fontMixRegular)
            .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixRegular,
        fontMixMedium = obj.optString("fontMixMedium", defaults.fontMixMedium)
            .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixMedium,
        idleGlyphScale = IdleSlots.normalizeGlyphScale(
            obj.optInt("idleGlyphScale", defaults.idleGlyphScale)
        ),
        idleWidgetTempUnit = obj.optString("idleWidgetTempUnit", defaults.idleWidgetTempUnit)
            .takeIf { it in IdleWidgets.VALID_UNITS } ?: defaults.idleWidgetTempUnit,
        idleWidgetWindUnit = obj.optString("idleWidgetWindUnit", defaults.idleWidgetWindUnit)
            .takeIf { it in IdleWidgets.VALID_WIND_UNITS } ?: defaults.idleWidgetWindUnit
    )
}

internal fun SmartIslandSettings.readJsonWeatherAndNetSpeed(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        weatherLocationSource = WeatherConfig.normalizeSource(
            obj.optString("weatherLocationSource", defaults.weatherLocationSource)
        ),
        weatherManualCity = WeatherConfig.normalizeCity(
            obj.optString("weatherManualCity", defaults.weatherManualCity)
        ),
        weatherManualLat = obj.optDouble("weatherManualLat", defaults.weatherManualLat.toDouble()).toFloat()
            .takeIf { it.isFinite() && it in -90f..90f } ?: defaults.weatherManualLat,
        weatherManualLon = obj.optDouble("weatherManualLon", defaults.weatherManualLon.toDouble()).toFloat()
            .takeIf { it.isFinite() && it in -180f..180f } ?: defaults.weatherManualLon,
        weatherRefreshMinutes = WeatherConfig.normalizeRefreshMinutes(
            obj.optInt("weatherRefreshMinutes", defaults.weatherRefreshMinutes)
        ),
        weatherProvider = WeatherConfig.normalizeProvider(
            obj.optString("weatherProvider", defaults.weatherProvider)
        ),
        weatherCustomUrl = WeatherConfig.normalizeCustomUrl(
            obj.optString("weatherCustomUrl", defaults.weatherCustomUrl)
        ),
        weatherPillMode = WeatherPill.normalize(
            obj.optString("weatherPillMode", defaults.weatherPillMode)
        ),
        weatherIslandReadouts = WeatherReadouts.normalize(
            obj.optString("weatherIslandReadouts", defaults.weatherIslandReadouts)
        ),
        weatherIconAnimated = obj.optBoolean("weatherIconAnimated", defaults.weatherIconAnimated),
        weatherIconPauseOnSaver = obj.optBoolean("weatherIconPauseOnSaver", defaults.weatherIconPauseOnSaver),
        weatherShowRule = WeatherRules.normalizeMode(
            obj.optString("weatherShowRule", defaults.weatherShowRule)
        ),
        weatherNotableTriggers = WeatherRules.normalizeTriggers(
            obj.optString("weatherNotableTriggers", defaults.weatherNotableTriggers)
        ),
        weatherHotC = WeatherRules.normalizeHotC(
            obj.optInt("weatherHotC", defaults.weatherHotC)
        ),
        weatherColdC = WeatherRules.normalizeColdC(
            obj.optInt("weatherColdC", defaults.weatherColdC)
        ),
        weatherHideAtNight = obj.optBoolean("weatherHideAtNight", defaults.weatherHideAtNight),
        weatherNightStartHour = WeatherRules.normalizeHour(
            obj.optInt("weatherNightStartHour", defaults.weatherNightStartHour)
        ),
        weatherNightEndHour = WeatherRules.normalizeHour(
            obj.optInt("weatherNightEndHour", defaults.weatherNightEndHour)
        ),
        netSpeedThresholdKbps = NetSpeedRules.normalize(
            obj.optInt("netSpeedThresholdKbps", defaults.netSpeedThresholdKbps)
        ),
        netSpeedConnection = NetSpeedRules.normalizeConnection(
            obj.optString("netSpeedConnection", defaults.netSpeedConnection)
        )
    )
}

internal fun SmartIslandSettings.readJsonIdleScheduleAndLedTick(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        idleScheduleWorkEnabled = obj.optBoolean("idleScheduleWorkEnabled", defaults.idleScheduleWorkEnabled),
        idleScheduleWorkHidden = IdleSchedule.normalizeHidden(obj.optString("idleScheduleWorkHidden",
            defaults.idleScheduleWorkHidden)),
        idleScheduleWorkStartHour = IdleSchedule.normalizeHour(obj.optInt("idleScheduleWorkStartHour",
            defaults.idleScheduleWorkStartHour)),
        idleScheduleWorkEndHour = IdleSchedule.normalizeHour(obj.optInt("idleScheduleWorkEndHour",
            defaults.idleScheduleWorkEndHour)),
        idleScheduleWorkWeekdaysOnly = obj.optBoolean("idleScheduleWorkWeekdaysOnly",
            defaults.idleScheduleWorkWeekdaysOnly),
        idleScheduleNightEnabled = obj.optBoolean("idleScheduleNightEnabled", defaults.idleScheduleNightEnabled),
        idleScheduleNightHidden = IdleSchedule.normalizeHidden(obj.optString("idleScheduleNightHidden",
            defaults.idleScheduleNightHidden)),
        idleScheduleNightStartHour = IdleSchedule.normalizeHour(obj.optInt("idleScheduleNightStartHour",
            defaults.idleScheduleNightStartHour)),
        idleScheduleNightEndHour = IdleSchedule.normalizeHour(obj.optInt("idleScheduleNightEndHour",
            defaults.idleScheduleNightEndHour)),
        idleScheduleChargingEnabled = obj.optBoolean("idleScheduleChargingEnabled",
            defaults.idleScheduleChargingEnabled),
        idleScheduleChargingHidden = IdleSchedule.normalizeHidden(obj.optString("idleScheduleChargingHidden",
            defaults.idleScheduleChargingHidden)),
        idleWidgetPinnedFirst = IdleSchedule.normalizePinned(obj.optString("idleWidgetPinnedFirst",
            defaults.idleWidgetPinnedFirst)),
        idleTickStyle = LedTick.normalizeStyle(obj.optString("idleTickStyle", defaults.idleTickStyle)),
        ledTickPattern = LedTick.normalizePattern(obj.optString("ledTickPattern", defaults.ledTickPattern)),
        ledTickPaceMs = LedTick.normalizePace(obj.optInt("ledTickPaceMs", defaults.ledTickPaceMs)),
        ledTickFont = obj.optString("ledTickFont", defaults.ledTickFont)
            .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.ledTickFont,
        ledTickColor = LedTick.normalizeColor(obj.optString("ledTickColor", defaults.ledTickColor)),
        ledTickStagger = obj.optBoolean("ledTickStagger", defaults.ledTickStagger)
    )
}

internal fun SmartIslandSettings.readJsonMotionAndInput(r: BackupReader): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    copy(
        secondaryMergeMs = obj.optInt("secondaryMergeMs", defaults.secondaryMergeMs)
            .coerceIn(MIN_SECONDARY_MERGE_MS, MAX_SECONDARY_MERGE_MS),
        animSpeedPortrait = obj.optString("animSpeedPortrait", defaults.animSpeedPortrait)
            .takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedPortrait,
        animSpeedLandscape = obj.optString("animSpeedLandscape", defaults.animSpeedLandscape)
            .takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedLandscape,
        mergeStyle = mergeId("mergeStyle", defaults.mergeStyle),
        // An old backup has 1 shared choice: landscape starts from it
        mergeStyleLandscape = mergeId("mergeStyleLandscape", mergeId("mergeStyle", defaults.mergeStyleLandscape)),
        showDuringDnd = obj.optBoolean("showDuringDnd", defaults.showDuringDnd),
        hapticStrength = obj.optString("hapticStrength", defaults.hapticStrength)
            .takeIf { it in HapticStrength.VALID_IDS } ?: defaults.hapticStrength,
        gestureSensitivity = obj.optString("gestureSensitivity", defaults.gestureSensitivity)
            .takeIf { it in GestureSensitivity.VALID_IDS } ?: defaults.gestureSensitivity,
        floatingWindowEnabled = obj.optBoolean("floatingWindowEnabled", defaults.floatingWindowEnabled),
        floatingWindowMode = obj.optString("floatingWindowMode", defaults.floatingWindowMode)
            .takeIf { it in FloatingWindowMode.VALID_IDS } ?: defaults.floatingWindowMode
    )
}

/**
 * [formatVersion] is the backup's own version. Below 2 the stored event sets are ignored: they were written when every
 * event opened the island, so they hold no information about what the person chose.
 */
internal fun SmartIslandSettings.readJsonAutoExpand(r: BackupReader, formatVersion: Int = BACKUP_FORMAT_VERSION): SmartIslandSettings = with(r) {
    val defaults = SmartIslandSettings.Default
    val keepEventSets = formatVersion >= 2
    copy(
        noExpandPackages = safeStringSet("noExpandPackages", defaults.noExpandPackages),
        noAutoExpandPackages = safeStringSet("noAutoExpandPackages", defaults.noAutoExpandPackages),
        // Keep explicit exclusions when importing overlapping lists.
        autoExpandPackages = safeStringSet("autoExpandPackages", defaults.autoExpandPackages)
            - safeStringSet("noAutoExpandPackages", defaults.noAutoExpandPackages),
        noAutoExpandEvents = if (keepEventSets) {
            safeStringSet("noAutoExpandEvents", defaults.noAutoExpandEvents)
                .filterTo(LinkedHashSet()) { id -> IslandEvent.entries.any { it.id == id } }
        } else defaults.noAutoExpandEvents,
        noAutoExpandEventsLandscape = if (keepEventSets) {
            safeStringSet("noAutoExpandEventsLandscape", defaults.noAutoExpandEventsLandscape)
                .filterTo(LinkedHashSet()) { id -> IslandEvent.entries.any { it.id == id } }
        } else defaults.noAutoExpandEventsLandscape,
        hideAllOnLockScreen = obj.optBoolean("hideAllOnLockScreen", defaults.hideAllOnLockScreen),
        hideOnLockScreenEvents = safeStringSet("hideOnLockScreenEvents", defaults.hideOnLockScreenEvents)
            .filterTo(LinkedHashSet()) { id -> IslandEvent.entries.any { it.id == id } },
        notificationsEnabled = obj.optBoolean("notificationsEnabled", defaults.notificationsEnabled),
        hideOnLockScreenPackages = safeStringSet("hideOnLockScreenPackages", defaults.hideOnLockScreenPackages)
            .filterTo(LinkedHashSet()) { it.isNotBlank() && it.length <= 255 }
    )
}
