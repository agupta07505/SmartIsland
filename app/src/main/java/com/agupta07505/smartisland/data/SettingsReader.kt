/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import androidx.datastore.preferences.core.Preferences
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
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.HapticStrength

/**
 * Every value is checked, a bad or missing one falls back to its default
 * [teardropPhone] is the notch default for teardrop cutouts
 */
internal fun readSettings(prefs: Preferences, teardropPhone: Boolean): SmartIslandSettings =
    SmartIslandSettings.Default
        .readPillLook(prefs)
        .readEventColors(prefs)
        .readShortcuts(prefs)
        .readNotificationRules(prefs)
        .readAutoExpand(prefs)
        .readExpandedStyle(prefs)
        .readAppBehaviour(prefs)
        .readNotificationCooldown(prefs)
        .readFaceAndStatusBar(prefs)
        .readCameraCutout(prefs, teardropPhone)
        .readGestures(prefs)
        .readLandscape(prefs, teardropPhone)
        .readTimings(prefs)
        .readIdleBehaviour(prefs)
        .readIdleWidgets(prefs)
        .readWeather(prefs)
        .readIdleSchedule(prefs)
        .readLedTick(prefs)

private fun SmartIslandSettings.readPillLook(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        enabled = prefs[SettingsKeys.Enabled] ?: defaults.enabled,
        width = validDimension(
            prefs[SettingsKeys.Width],
            defaults.width,
            SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH
        ),
        height = validDimension(
            prefs[SettingsKeys.Height],
            defaults.height,
            SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT
        ),
        xOffset = validDimension(
            prefs[SettingsKeys.XOffset],
            defaults.xOffset,
            SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET
        ),
        yOffset = validDimension(
            prefs[SettingsKeys.YOffset],
            defaults.yOffset,
            SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET
        ),
        cornerRadius = validDimension(
            prefs[SettingsKeys.CornerRadius],
            defaults.cornerRadius,
            SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS
        ),
        opacity = validDimension(
            prefs[SettingsKeys.Opacity],
            defaults.opacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        ),
        pillColor = validColor(prefs[SettingsKeys.PillColor], defaults.pillColor),
        oledBlack = prefs[SettingsKeys.OledBlack] ?: defaults.oledBlack,
        enableShadow = prefs[SettingsKeys.EnableShadow] ?: defaults.enableShadow,
        shadowElevation = validDimension(
            prefs[SettingsKeys.ShadowElevation],
            defaults.shadowElevation,
            SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION
        ),
        enableMusicArtworkBackground = prefs[SettingsKeys.EnableMusicArtworkBackground]
            ?: defaults.enableMusicArtworkBackground,
        enableNotificationBackdrop = prefs[SettingsKeys.EnableNotificationBackdrop]
            ?: defaults.enableNotificationBackdrop
    )
}

private fun SmartIslandSettings.readEventColors(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        modeColors = ModeColors(
            batteryColor = validColor(prefs[SettingsKeys.BatteryColor], defaults.batteryColor),
            notificationDotColor = validColor(
                prefs[SettingsKeys.NotificationDotColor],
                defaults.notificationDotColor
            ),
            musicVisualizerColor = validColor(
                prefs[SettingsKeys.MusicVisualizerColor],
                defaults.musicVisualizerColor
            ),
            hotspotColor = validColor(prefs[SettingsKeys.HotspotColor], defaults.hotspotColor),
            callColor = validColor(prefs[SettingsKeys.CallColor], defaults.callColor),
            liveActivityColor = validColor(prefs[SettingsKeys.LiveActivityColor], defaults.liveActivityColor),
            transferColor = validColor(prefs[SettingsKeys.TransferColor], defaults.transferColor),
            navigationColor = validColor(prefs[SettingsKeys.NavigationColor], defaults.navigationColor),
            bluetoothColor = validColor(prefs[SettingsKeys.BluetoothColor], defaults.bluetoothColor),
            flashlightColor = validColor(prefs[SettingsKeys.FlashlightColor], defaults.flashlightColor),
            screenRecordingColor = validColor(prefs[SettingsKeys.ScreenRecordingColor], defaults.screenRecordingColor),
            timerColor = validColor(prefs[SettingsKeys.TimerColor], defaults.timerColor),
            stopwatchColor = validColor(prefs[SettingsKeys.StopwatchColor], defaults.stopwatchColor)
        ),
        eventColors = EventColors.decode(prefs[SettingsKeys.EventColors])
    )
}

private fun SmartIslandSettings.readShortcuts(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        enableAppShortcuts = prefs[SettingsKeys.EnableAppShortcuts] ?: defaults.enableAppShortcuts,
        shortcutPackages = if (prefs[SettingsKeys.ShortcutOrder] == null
            && prefs[SettingsKeys.ShortcutPackages] == null) {
            defaults.shortcutPackages
        } else {
            PinnedApps.restore(prefs[SettingsKeys.ShortcutOrder], prefs[SettingsKeys.ShortcutPackages])
        },
        showRecentApps = prefs[SettingsKeys.ShowRecentApps] ?: defaults.showRecentApps,
        utilityLayout = prefs[SettingsKeys.UtilityLayout] ?: defaults.utilityLayout,
        utilityCalculatorPackage = prefs[SettingsKeys.UtilityCalculatorPackage] ?: defaults.utilityCalculatorPackage,
        utilityCalculatorFloating = prefs[SettingsKeys.UtilityCalculatorFloating] ?: defaults.utilityCalculatorFloating
    )
}

/** The old single switch meant all or nothing: on keeps every live card, off (or unset) is the new default. */
private fun legacyForegroundEvents(legacyOn: Boolean?, defaults: SmartIslandSettings): Set<String> =
    if (legacyOn == true) IslandEvent.FOREGROUND_SWITCHABLE.mapTo(LinkedHashSet()) { it.id }
    else defaults.showInForegroundEvents

private fun SmartIslandSettings.readNotificationRules(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        showOnLockScreen = prefs[SettingsKeys.ShowOnLockScreen] ?: defaults.showOnLockScreen,
        showInForegroundEvents = prefs[SettingsKeys.ShowInForegroundEvents]
            ?.filter { id -> IslandEvent.FOREGROUND_SWITCHABLE.any { it.id == id } }
            ?.toSet()
            ?: legacyForegroundEvents(prefs[SettingsKeys.ShowLiveEventsInForeground], defaults),
        hideInFullscreen = prefs[SettingsKeys.HideInFullscreen] ?: defaults.hideInFullscreen,
        hideInShade = prefs[SettingsKeys.HideInShade] ?: defaults.hideInShade,
        lockScreenPrivacy = prefs[SettingsKeys.LockScreenPrivacy]
            ?.takeIf { it in VALID_LOCK_SCREEN_PRIVACY_VALUES }
            ?: defaults.lockScreenPrivacy,
        showNotificationActions = prefs[SettingsKeys.ShowNotificationActions]
            ?: defaults.showNotificationActions,
        hideFromNotificationShade = prefs[SettingsKeys.HideFromNotificationShade]
            ?: defaults.hideFromNotificationShade,
        liveActivitiesEnabled = prefs[SettingsKeys.LiveActivitiesEnabled]
            ?: defaults.liveActivitiesEnabled,
        navigationEnabled = prefs[SettingsKeys.NavigationEnabled]
            ?: defaults.navigationEnabled,
        disabledNotificationPackages = prefs[SettingsKeys.DisabledNotificationPackages]?.validPackages()
            ?: defaults.disabledNotificationPackages,
        disabledSoundPackages = prefs[SettingsKeys.DisabledSoundPackages]?.validPackages()
            ?: defaults.disabledSoundPackages,
        disabledEvents = prefs[SettingsKeys.DisabledEvents]
            ?.filter { it.isNotBlank() && it.length <= 64 }
            ?.toSet()
            ?: defaults.disabledEvents,
        disabledEventsLandscape = prefs[SettingsKeys.DisabledEventsLandscape]
            ?.filter { it.isNotBlank() && it.length <= 64 }
            ?.toSet()
            ?: defaults.disabledEventsLandscape,
        hideAllOnLockScreen = prefs[SettingsKeys.HideAllOnLockScreen] ?: defaults.hideAllOnLockScreen,
        hideOnLockScreenEvents = prefs[SettingsKeys.HideOnLockScreenEvents]
            ?.filter { id -> IslandEvent.entries.any { it.id == id } }
            ?.toSet()
            ?: defaults.hideOnLockScreenEvents,
        notificationsEnabled = prefs[SettingsKeys.NotificationsEnabled] ?: defaults.notificationsEnabled,
        hideOnLockScreenPackages = prefs[SettingsKeys.HideOnLockScreenPackages]?.validPackages()
            ?: defaults.hideOnLockScreenPackages
    )
}

private fun SmartIslandSettings.readAutoExpand(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        autoExpandOnNotification = prefs[SettingsKeys.AutoExpandOnNotification] ?: defaults.autoExpandOnNotification,
        autoExpandOnLockScreen = prefs[SettingsKeys.AutoExpandOnLockScreen] ?: defaults.autoExpandOnLockScreen,
        showSuggestedReplies = prefs[SettingsKeys.ShowSuggestedReplies] ?: defaults.showSuggestedReplies,
        showCopyCodeChip = prefs[SettingsKeys.ShowCopyCodeChip] ?: defaults.showCopyCodeChip,
        noExpandPackages = prefs[SettingsKeys.NoExpandPackages]?.validPackages()
            ?: defaults.noExpandPackages,
        noAutoExpandPackages = prefs[SettingsKeys.NoAutoExpandPackages]?.validPackages()
            ?: defaults.noAutoExpandPackages,
        // A disabled package can still appear in the saved allowlist.
        autoExpandPackages = (prefs[SettingsKeys.AutoExpandPackages]?.validPackages()
            ?: defaults.autoExpandPackages) -
            (prefs[SettingsKeys.NoAutoExpandPackages] ?: emptySet()),
        noAutoExpandEvents = prefs[SettingsKeys.NoAutoExpandEvents]
            ?.filter { id -> IslandEvent.entries.any { it.id == id } }
            ?.toSet()
            ?: defaults.noAutoExpandEvents,
        // Never set in landscape yet, start from the old shared list so nothing changes for existing users.
        noAutoExpandEventsLandscape = (prefs[SettingsKeys.NoAutoExpandEventsLandscape]
            ?: prefs[SettingsKeys.NoAutoExpandEvents])
            ?.filter { id -> IslandEvent.entries.any { it.id == id } }
            ?.toSet()
            ?: defaults.noAutoExpandEventsLandscape
    )
}

private fun SmartIslandSettings.readExpandedStyle(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        expandedBlurEnabled = prefs[SettingsKeys.ExpandedBlurEnabled] ?: defaults.expandedBlurEnabled,
        expandedBlurRadius = (prefs[SettingsKeys.ExpandedBlurRadius] ?: defaults.expandedBlurRadius)
            .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR),
        // media keys fall back to the shared values until the owner sets them
        mediaBlurEnabled = prefs[SettingsKeys.MediaBlurEnabled] ?: prefs[SettingsKeys.ExpandedBlurEnabled]
            ?: defaults.mediaBlurEnabled,
        mediaBlurRadius = (prefs[SettingsKeys.MediaBlurRadius] ?: prefs[SettingsKeys.ExpandedBlurRadius]
            ?: defaults.mediaBlurRadius)
            .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR),
        mediaOpacity = validDimension(
            prefs[SettingsKeys.MediaOpacity] ?: prefs[SettingsKeys.Opacity],
            defaults.mediaOpacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        ),
        expandedGradientEnabled = prefs[SettingsKeys.ExpandedGradientEnabled] ?: defaults.expandedGradientEnabled,
        expandedGradientColor = validColor(prefs[SettingsKeys.ExpandedGradientColor], defaults.expandedGradientColor),
        expandedGradientStrength = (prefs[SettingsKeys.ExpandedGradientStrength] ?: defaults.expandedGradientStrength)
            .coerceIn(SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH),
        themeMode = prefs[SettingsKeys.ThemeMode] ?: defaults.themeMode,
        liquidGlassTheme = prefs[SettingsKeys.LiquidGlassTheme] ?: defaults.liquidGlassTheme,
        materialYou = prefs[SettingsKeys.MaterialYou] ?: defaults.materialYou,
        accentColor = validColor(prefs[SettingsKeys.AccentColor], defaults.accentColor),
        expandedTheme = prefs[SettingsKeys.ExpandedTheme]?.takeIf { it in SmartIslandSettings.VALID_EXPANDED_THEMES }
            ?: defaults.expandedTheme,
        islandFont = prefs[SettingsKeys.IslandFont]?.takeIf { SmartIslandSettings.isValidFont(it) }
            ?: defaults.islandFont
    )
}

private fun SmartIslandSettings.readAppBehaviour(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        welcomeDialogShown = prefs[SettingsKeys.WelcomeDialogShown] ?: defaults.welcomeDialogShown,
        torchStrobeAcknowledged = prefs[SettingsKeys.TorchStrobeAcknowledged] ?: defaults.torchStrobeAcknowledged,
        deviceType = prefs[SettingsKeys.DeviceType] ?: defaults.deviceType,
        allowNetworkChecks = prefs[SettingsKeys.AllowNetworkChecks] ?: defaults.allowNetworkChecks,
        detectTransfersBySpeed = prefs[SettingsKeys.DetectTransfersBySpeed] ?: defaults.detectTransfersBySpeed,
        enableNotificationHistory = prefs[SettingsKeys.EnableNotificationHistory] ?: defaults.enableNotificationHistory,
        notificationHistoryRetentionHours = prefs[SettingsKeys.NotificationHistoryRetentionHours]
            ?: defaults.notificationHistoryRetentionHours,
        showBluetoothBattery = prefs[SettingsKeys.ShowBluetoothBattery] ?: defaults.showBluetoothBattery,
        enableBatteryMode = prefs[SettingsKeys.EnableBatteryMode] ?: defaults.enableBatteryMode,
        developerModeEnabled = prefs[SettingsKeys.DeveloperModeEnabled] ?: defaults.developerModeEnabled,
        recordLogs = prefs[SettingsKeys.RecordLogs] ?: defaults.recordLogs,
        showDuringDnd = prefs[SettingsKeys.ShowDuringDnd] ?: defaults.showDuringDnd
    )
}

private fun SmartIslandSettings.readNotificationCooldown(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        enableNotificationCooldown = prefs[SettingsKeys.EnableNotificationCooldown]
            ?: defaults.enableNotificationCooldown,
        notificationCooldownDurationMinutes = prefs[SettingsKeys.NotificationCooldownDurationMinutes]
            ?: defaults.notificationCooldownDurationMinutes,
        notificationCooldownThreshold = prefs[SettingsKeys.NotificationCooldownThreshold]
            ?: defaults.notificationCooldownThreshold,
        notificationCooldownExcludedPackages = prefs[SettingsKeys.NotificationCooldownExcludedPackages]
            ?: defaults.notificationCooldownExcludedPackages,
        notificationCooldownSystemEvents = prefs[SettingsKeys.NotificationCooldownSystemEvents]
            ?: defaults.notificationCooldownSystemEvents
    )
}

private fun SmartIslandSettings.readFaceAndStatusBar(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        faceUnlockMode = prefs[SettingsKeys.FaceUnlockMode]?.takeIf { it in setOf("face_only", "any") }
            ?: defaults.faceUnlockMode,
        faceIdStyle = prefs[SettingsKeys.FaceIdStyle]?.takeIf { it in setOf("square", "cutout", "square_tick") }
            ?: defaults.faceIdStyle,
        appFaceIdEnabled = prefs[SettingsKeys.AppFaceIdEnabled] ?: defaults.appFaceIdEnabled,
        appFaceIdStyle = prefs[SettingsKeys.AppFaceIdStyle]?.takeIf { it in setOf("square", "cutout", "square_tick") }
            ?: defaults.appFaceIdStyle,
        statusBarHidden = prefs[SettingsKeys.StatusBarHidden] ?: defaults.statusBarHidden,
        statusBarHidePersistent = prefs[SettingsKeys.StatusBarHidePersistent] ?: defaults.statusBarHidePersistent,
        statusBarHideNotificationIcons = prefs[SettingsKeys.StatusBarHideNotificationIcons]
            ?: defaults.statusBarHideNotificationIcons,
        statusBarHideClock = prefs[SettingsKeys.StatusBarHideClock] ?: defaults.statusBarHideClock,
        statusBarHideAllSystemIcons = prefs[SettingsKeys.StatusBarHideAllSystemIcons]
            ?: defaults.statusBarHideAllSystemIcons,
        statusBarHiddenSystemIcons = prefs[SettingsKeys.StatusBarHiddenSystemIcons]
            ?: defaults.statusBarHiddenSystemIcons,
        fingerprintAnimationEnabled = prefs[SettingsKeys.FingerprintAnimationEnabled]
            ?: defaults.fingerprintAnimationEnabled
    )
}

private fun SmartIslandSettings.readCameraCutout(prefs: Preferences, teardropPhone: Boolean): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        enableNotchMode = prefs[SettingsKeys.EnableNotchMode] ?: teardropPhone,
        cameraCutoutManual = prefs[SettingsKeys.CameraCutoutManual] ?: defaults.cameraCutoutManual,
        cameraCutoutStyle = prefs[SettingsKeys.CameraCutoutStyle]?.takeIf { it in VALID_CUTOUT_STYLES }
            ?: defaults.cameraCutoutStyle,
        cameraCutoutX = validDimension(prefs[SettingsKeys.CameraCutoutX], defaults.cameraCutoutX,
            SmartIslandSettings.MIN_CUTOUT_X, SmartIslandSettings.MAX_CUTOUT_X),
        cameraCutoutY = validDimension(prefs[SettingsKeys.CameraCutoutY], defaults.cameraCutoutY,
            SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET),
        cameraCutoutWidth = validDimension(prefs[SettingsKeys.CameraCutoutWidth], defaults.cameraCutoutWidth,
            SmartIslandSettings.MIN_CUTOUT_WIDTH, SmartIslandSettings.MAX_CUTOUT_WIDTH),
        cameraCutoutHeight = validDimension(prefs[SettingsKeys.CameraCutoutHeight], defaults.cameraCutoutHeight,
            SmartIslandSettings.MIN_CUTOUT_HEIGHT, SmartIslandSettings.MAX_CUTOUT_HEIGHT)
    )
}

private fun SmartIslandSettings.readGestures(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        enableSwipeActions = prefs[SettingsKeys.EnableSwipeActions] ?: defaults.enableSwipeActions,
        swipeUpAction = prefs[SettingsKeys.SwipeUpAction] ?: defaults.swipeUpAction,
        swipeHoldUpAction = prefs[SettingsKeys.SwipeHoldUpAction] ?: defaults.swipeHoldUpAction,
        swipeDownAction = prefs[SettingsKeys.SwipeDownAction] ?: defaults.swipeDownAction,
        swipeDownCollapsedAction = prefs[SettingsKeys.SwipeDownCollapsedAction] ?: defaults.swipeDownCollapsedAction,
        swipeLeftAction = prefs[SettingsKeys.SwipeLeftAction] ?: defaults.swipeLeftAction,
        swipeRightAction = prefs[SettingsKeys.SwipeRightAction] ?: defaults.swipeRightAction,
        swipeHorizontalCollapsedAction = prefs[SettingsKeys.SwipeHorizontalCollapsedAction]
            ?: defaults.swipeHorizontalCollapsedAction,
        enablePillSwipeActions = prefs[SettingsKeys.EnablePillSwipeActions] ?: defaults.enablePillSwipeActions,
        pillSwipeUpAction = prefs[SettingsKeys.PillSwipeUpAction] ?: defaults.pillSwipeUpAction,
        pillSwipeDownAction = prefs[SettingsKeys.PillSwipeDownAction] ?: defaults.pillSwipeDownAction,
        pillSwipeLeftAction = prefs[SettingsKeys.PillSwipeLeftAction] ?: defaults.pillSwipeLeftAction,
        pillSwipeRightAction = prefs[SettingsKeys.PillSwipeRightAction] ?: defaults.pillSwipeRightAction,
        longPressAction = prefs[SettingsKeys.LongPressAction] ?: defaults.longPressAction,
        hapticStrength = prefs[SettingsKeys.HapticStrength]
            ?.takeIf { it in HapticStrength.VALID_IDS } ?: defaults.hapticStrength,
        gestureSensitivity = prefs[SettingsKeys.GestureSensitivityId]
            ?.takeIf { it in GestureSensitivity.VALID_IDS } ?: defaults.gestureSensitivity
    )
}

private fun SmartIslandSettings.readLandscape(prefs: Preferences, teardropPhone: Boolean): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        showInLandscape = prefs[SettingsKeys.ShowInLandscape] ?: defaults.showInLandscape,
        circlePosition = prefs[SettingsKeys.CirclePosition]?.takeIf { it in VALID_CIRCLE_POSITIONS }
            ?: defaults.circlePosition,
        landscapeShape = prefs[SettingsKeys.LandscapeShape]?.takeIf { it in VALID_LANDSCAPE_SHAPES }
            ?: defaults.landscapeShape,
        landscapeWidth = validDimension(prefs[SettingsKeys.LandscapeWidth], defaults.landscapeWidth,
            SmartIslandSettings.MIN_WIDTH, SmartIslandSettings.MAX_WIDTH),
        landscapeHeight = validDimension(prefs[SettingsKeys.LandscapeHeight], defaults.landscapeHeight,
            SmartIslandSettings.MIN_HEIGHT, SmartIslandSettings.MAX_HEIGHT),
        landscapeXOffset = validDimension(prefs[SettingsKeys.LandscapeXOffset], defaults.landscapeXOffset,
            -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
        landscapeYOffset = validDimension(prefs[SettingsKeys.LandscapeYOffset], defaults.landscapeYOffset,
            SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
        landscapeCornerRadius = validDimension(prefs[SettingsKeys.LandscapeCornerRadius],
            defaults.landscapeCornerRadius, SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS),
        landscapeOpacity = validDimension(prefs[SettingsKeys.LandscapeOpacity], defaults.landscapeOpacity,
            SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY),
        landscapeCircleSize = validDimension(prefs[SettingsKeys.LandscapeCircleSize], defaults.landscapeCircleSize,
            SmartIslandSettings.MIN_CIRCLE_SIZE, SmartIslandSettings.MAX_CIRCLE_SIZE),
        landscapeCircleOpacity = validDimension(prefs[SettingsKeys.LandscapeCircleOpacity],
            defaults.landscapeCircleOpacity, SmartIslandSettings.MIN_CIRCLE_OPACITY, SmartIslandSettings.MAX_OPACITY),
        landscapeCircleEdgeInset = validDimension(prefs[SettingsKeys.LandscapeCircleEdgeInset],
            defaults.landscapeCircleEdgeInset, SmartIslandSettings.MIN_CIRCLE_EDGE_INSET,
            SmartIslandSettings.MAX_CIRCLE_EDGE_INSET),
        landscapeCircleYOffset = validDimension(prefs[SettingsKeys.LandscapeCircleYOffset],
            defaults.landscapeCircleYOffset, SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
        landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_AUTO,
        landscapePillPosition = prefs[SettingsKeys.LandscapePillPosition]?.takeIf { it in VALID_LANDSCAPE_POSITIONS }
            ?: defaults.landscapePillPosition,
        landscapeLayoutStyle = LandscapeLayoutStyle.fromId(prefs[SettingsKeys.LandscapeLayoutStyle]),
        edgePill = EdgePillTuning.decode(prefs[SettingsKeys.EdgePill], flatEdgeDefault = teardropPhone),
        landscapeCircleXAdjust = validDimension(prefs[SettingsKeys.LandscapeCircleXAdjust],
            defaults.landscapeCircleXAdjust, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET,
            SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
        landscapeCircleYAdjust = validDimension(prefs[SettingsKeys.LandscapeCircleYAdjust],
            defaults.landscapeCircleYAdjust, -SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
            SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    )
}

private fun SmartIslandSettings.readTimings(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        hideWhenIdle = prefs[SettingsKeys.HideWhenIdle] ?: defaults.hideWhenIdle,
        autoHidePill = prefs[SettingsKeys.AutoHidePill] ?: defaults.autoHidePill,
        autoHideTimeoutSeconds = prefs[SettingsKeys.AutoHideTimeoutSeconds] ?: defaults.autoHideTimeoutSeconds,
        idleTimeoutMs = (prefs[SettingsKeys.IdleTimeoutMs] ?: defaults.idleTimeoutMs).let { raw ->
            // stored before the Never chip
            if (prefs[SettingsKeys.IdleTimeoutNever] == null
                && raw >= SmartIslandSettings.LEGACY_IDLE_TIMEOUT_NEVER_MS) defaults.idleTimeoutMs
            else raw.coerceIn(SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
        },
        idleTimeoutNever = prefs[SettingsKeys.IdleTimeoutNever]
            ?: ((prefs[SettingsKeys.IdleTimeoutMs]
                ?: defaults.idleTimeoutMs) >= SmartIslandSettings.LEGACY_IDLE_TIMEOUT_NEVER_MS),
        batteryWarningMs = (prefs[SettingsKeys.BatteryWarningMs] ?: defaults.batteryWarningMs)
            .coerceIn(SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS),
        pulseHoldPercent = (prefs[SettingsKeys.PulseHoldPercent] ?: defaults.pulseHoldPercent)
            .coerceIn(SmartIslandSettings.MIN_PULSE_HOLD_PERCENT, SmartIslandSettings.MAX_PULSE_HOLD_PERCENT),
        transferExpiryMinutes = (prefs[SettingsKeys.TransferExpiryMinutes] ?: defaults.transferExpiryMinutes)
            .coerceIn(0, SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES),
        secondaryMergeMs = (prefs[SettingsKeys.SecondaryMergeMs] ?: defaults.secondaryMergeMs)
            .coerceIn(SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS),
        animSpeedPortrait = prefs[SettingsKeys.AnimSpeedPortrait]
            ?.takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedPortrait,
        animSpeedLandscape = prefs[SettingsKeys.AnimSpeedLandscape]
            ?.takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedLandscape,
        mergeStyle = prefs[SettingsKeys.MergeStyleId]
            ?.takeIf { it in MergeStyle.VALID_IDS } ?: defaults.mergeStyle,
        // Installs from before the split have only the shared key
        mergeStyleLandscape = prefs[SettingsKeys.MergeStyleLandscapeId]
            ?.takeIf { it in MergeStyle.VALID_IDS }
            ?: prefs[SettingsKeys.MergeStyleId]?.takeIf { it in MergeStyle.VALID_IDS }
            ?: defaults.mergeStyleLandscape,
        floatingWindowEnabled = prefs[SettingsKeys.FloatingWindowEnabled] ?: defaults.floatingWindowEnabled,
        floatingWindowMode = prefs[SettingsKeys.FloatingWindowMode]
            ?.takeIf { it in FloatingWindowMode.VALID_IDS } ?: defaults.floatingWindowMode
    )
}

private fun SmartIslandSettings.readIdleBehaviour(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        idleWidgetsEnabled = prefs[SettingsKeys.IdleWidgetsEnabled] ?: defaults.idleWidgetsEnabled,
        idleWidgetsEnabledLandscape = prefs[SettingsKeys.IdleWidgetsEnabledLandscape]
            ?: prefs[SettingsKeys.IdleWidgetsEnabled] ?: defaults.idleWidgetsEnabledLandscape,
        idleOffAfterTapClose = prefs[SettingsKeys.IdleOffAfterTapClose] ?: defaults.idleOffAfterTapClose,
        idleOffAfterTapCloseLandscape = prefs[SettingsKeys.IdleOffAfterTapCloseLandscape]
            ?: defaults.idleOffAfterTapCloseLandscape,
        idleOffAfterLongPress = prefs[SettingsKeys.IdleOffAfterLongPress] ?: defaults.idleOffAfterLongPress,
        idleOffAfterLongPressLandscape = prefs[SettingsKeys.IdleOffAfterLongPressLandscape]
            ?: defaults.idleOffAfterLongPressLandscape,
        idleOffWhenStored = prefs[SettingsKeys.IdleOffWhenStored] ?: defaults.idleOffWhenStored,
        contentPillSameAsIdle = prefs[SettingsKeys.ContentPillSameAsIdle] ?: defaults.contentPillSameAsIdle,
        islandExemptApps = prefs[SettingsKeys.IslandExemptApps]?.validPackages() ?: defaults.islandExemptApps,
        islandExemptAppsLandscape = prefs[SettingsKeys.IslandExemptAppsLandscape]?.validPackages()
            ?: defaults.islandExemptAppsLandscape,
        islandExemptAppsStatic = prefs[SettingsKeys.IslandExemptAppsStatic]?.validPackages()
            ?: defaults.islandExemptAppsStatic,
        contentPillSameAsIdleLandscape = prefs[SettingsKeys.ContentPillSameAsIdleLandscape]
            ?: defaults.contentPillSameAsIdleLandscape,
        contentPillWidthPercentLandscape = (prefs[SettingsKeys.ContentPillWidthPercentLandscape]
            ?: defaults.contentPillWidthPercentLandscape).let {
            if (it.isNaN()) defaults.contentPillWidthPercentLandscape
            else it.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
        },
        contentPillWidthPercent = (prefs[SettingsKeys.ContentPillWidthPercent] ?: defaults.contentPillWidthPercent)
            .let {
            if (it.isNaN()) defaults.contentPillWidthPercent
            else it.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
        },
        idleOffWhenStoredLandscape = prefs[SettingsKeys.IdleOffWhenStoredLandscape]
            ?: defaults.idleOffWhenStoredLandscape
    )
}

private fun SmartIslandSettings.readIdleWidgets(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        idleGlyphTickSeconds = IdleSlots.normalizeTickSeconds(
            prefs[SettingsKeys.IdleGlyphTickSeconds] ?: defaults.idleGlyphTickSeconds
        ),
        idleWidgetIds = IdleWidgets.normalize(
            prefs[SettingsKeys.IdleWidgetIds] ?: defaults.idleWidgetIds
        ),
        idleWidgetCycleSeconds = (prefs[SettingsKeys.IdleWidgetCycleSeconds] ?: defaults.idleWidgetCycleSeconds)
            .coerceIn(
                IdleWidgets.MIN_CYCLE_SECONDS,
                IdleWidgets.MAX_CYCLE_SECONDS
            ),
        idleAlarmHideBeyond12h = prefs[SettingsKeys.IdleAlarmHideBeyond12h] ?: defaults.idleAlarmHideBeyond12h,
        batteryOnlyWhenCharging = prefs[SettingsKeys.BatteryOnlyWhenCharging] ?: defaults.batteryOnlyWhenCharging,
        idleBeatOnlyOnChange = prefs[SettingsKeys.IdleBeatOnlyOnChange] ?: defaults.idleBeatOnlyOnChange,
        widgetIslandCloseSeconds = WidgetIslandTimer.normalize(
            prefs[SettingsKeys.WidgetIslandCloseSeconds] ?: defaults.widgetIslandCloseSeconds
        ),
        widgetIslandBatteryMetric = (prefs[SettingsKeys.WidgetIslandBatteryMetric]
            ?: defaults.widgetIslandBatteryMetric)
            .takeIf { it in WidgetIslandContent.VALID_BATTERY_METRICS } ?: defaults.widgetIslandBatteryMetric,
        idleGlyphFont = prefs[SettingsKeys.IdleGlyphFont]?.takeIf { it.isEmpty()
            || SmartIslandSettings.isValidFont(it) } ?: defaults.idleGlyphFont,
        fontMixEnabled = prefs[SettingsKeys.FontMixEnabled] ?: defaults.fontMixEnabled,
        fontMixLight = prefs[SettingsKeys.FontMixLight]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) }
            ?: defaults.fontMixLight,
        fontMixRegular = prefs[SettingsKeys.FontMixRegular]?.takeIf { it.isEmpty()
            || SmartIslandSettings.isValidFont(it) } ?: defaults.fontMixRegular,
        fontMixMedium = prefs[SettingsKeys.FontMixMedium]?.takeIf { it.isEmpty()
            || SmartIslandSettings.isValidFont(it) } ?: defaults.fontMixMedium,
        idleGlyphScale = IdleSlots.normalizeGlyphScale(
            prefs[SettingsKeys.IdleGlyphScale] ?: defaults.idleGlyphScale
        ),
        idleWidgetTempUnit = (prefs[SettingsKeys.IdleWidgetTempUnit] ?: defaults.idleWidgetTempUnit)
            .takeIf { it in IdleWidgets.VALID_UNITS } ?: defaults.idleWidgetTempUnit,
        idleWidgetWindUnit = (prefs[SettingsKeys.IdleWidgetWindUnit] ?: defaults.idleWidgetWindUnit)
            .takeIf { it in IdleWidgets.VALID_WIND_UNITS } ?: defaults.idleWidgetWindUnit,
        idleWidgetPinnedFirst = IdleSchedule.normalizePinned(prefs[SettingsKeys.IdleWidgetPinnedFirst]
            ?: defaults.idleWidgetPinnedFirst)
    )
}

private fun SmartIslandSettings.readWeather(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        weatherLocationSource = WeatherConfig.normalizeSource(
            prefs[SettingsKeys.WeatherLocationSource] ?: defaults.weatherLocationSource
        ),
        weatherManualCity = WeatherConfig.normalizeCity(
            prefs[SettingsKeys.WeatherManualCity] ?: defaults.weatherManualCity
        ),
        weatherManualLat = (prefs[SettingsKeys.WeatherManualLat] ?: defaults.weatherManualLat)
            .takeIf { it.isFinite() && it in -90f..90f } ?: defaults.weatherManualLat,
        weatherManualLon = (prefs[SettingsKeys.WeatherManualLon] ?: defaults.weatherManualLon)
            .takeIf { it.isFinite() && it in -180f..180f } ?: defaults.weatherManualLon,
        weatherRefreshMinutes = WeatherConfig.normalizeRefreshMinutes(
            prefs[SettingsKeys.WeatherRefreshMinutes] ?: defaults.weatherRefreshMinutes
        ),
        weatherProvider = WeatherConfig.normalizeProvider(
            prefs[SettingsKeys.WeatherProvider] ?: defaults.weatherProvider
        ),
        weatherCustomUrl = WeatherConfig.normalizeCustomUrl(
            prefs[SettingsKeys.WeatherCustomUrl] ?: defaults.weatherCustomUrl
        ),
        weatherCustomKey = WeatherConfig.normalizeKey(
            prefs[SettingsKeys.WeatherCustomKey] ?: defaults.weatherCustomKey
        ),
        weatherPillMode = WeatherPill.normalize(
            prefs[SettingsKeys.WeatherPillMode] ?: defaults.weatherPillMode
        ),
        weatherIslandReadouts = WeatherReadouts.normalize(
            prefs[SettingsKeys.WeatherIslandReadouts] ?: defaults.weatherIslandReadouts
        ),
        weatherIconAnimated = prefs[SettingsKeys.WeatherIconAnimated] ?: defaults.weatherIconAnimated,
        weatherIconPauseOnSaver = prefs[SettingsKeys.WeatherIconPauseOnSaver] ?: defaults.weatherIconPauseOnSaver,
        weatherShowRule = WeatherRules.normalizeMode(
            prefs[SettingsKeys.WeatherShowRule] ?: defaults.weatherShowRule
        ),
        weatherNotableTriggers = WeatherRules.normalizeTriggers(
            prefs[SettingsKeys.WeatherNotableTriggers] ?: defaults.weatherNotableTriggers
        ),
        weatherHotC = WeatherRules.normalizeHotC(
            prefs[SettingsKeys.WeatherHotC] ?: defaults.weatherHotC
        ),
        weatherColdC = WeatherRules.normalizeColdC(
            prefs[SettingsKeys.WeatherColdC] ?: defaults.weatherColdC
        ),
        weatherHideAtNight = prefs[SettingsKeys.WeatherHideAtNight] ?: defaults.weatherHideAtNight,
        weatherNightStartHour = WeatherRules.normalizeHour(
            prefs[SettingsKeys.WeatherNightStartHour] ?: defaults.weatherNightStartHour
        ),
        weatherNightEndHour = WeatherRules.normalizeHour(
            prefs[SettingsKeys.WeatherNightEndHour] ?: defaults.weatherNightEndHour
        ),
        netSpeedThresholdKbps = NetSpeedRules.normalize(
            prefs[SettingsKeys.NetSpeedThresholdKbps] ?: defaults.netSpeedThresholdKbps
        ),
        netSpeedConnection = NetSpeedRules.normalizeConnection(
            prefs[SettingsKeys.NetSpeedConnection] ?: defaults.netSpeedConnection
        )
    )
}

private fun SmartIslandSettings.readIdleSchedule(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        idleScheduleWorkEnabled = prefs[SettingsKeys.IdleScheduleWorkEnabled] ?: defaults.idleScheduleWorkEnabled,
        idleScheduleWorkHidden = IdleSchedule.normalizeHidden(prefs[SettingsKeys.IdleScheduleWorkHidden]
            ?: defaults.idleScheduleWorkHidden),
        idleScheduleWorkStartHour = IdleSchedule.normalizeHour(prefs[SettingsKeys.IdleScheduleWorkStartHour]
            ?: defaults.idleScheduleWorkStartHour),
        idleScheduleWorkEndHour = IdleSchedule.normalizeHour(prefs[SettingsKeys.IdleScheduleWorkEndHour]
            ?: defaults.idleScheduleWorkEndHour),
        idleScheduleWorkWeekdaysOnly = prefs[SettingsKeys.IdleScheduleWorkWeekdaysOnly]
            ?: defaults.idleScheduleWorkWeekdaysOnly,
        idleScheduleNightEnabled = prefs[SettingsKeys.IdleScheduleNightEnabled] ?: defaults.idleScheduleNightEnabled,
        idleScheduleNightHidden = IdleSchedule.normalizeHidden(prefs[SettingsKeys.IdleScheduleNightHidden]
            ?: defaults.idleScheduleNightHidden),
        idleScheduleNightStartHour = IdleSchedule.normalizeHour(prefs[SettingsKeys.IdleScheduleNightStartHour]
            ?: defaults.idleScheduleNightStartHour),
        idleScheduleNightEndHour = IdleSchedule.normalizeHour(prefs[SettingsKeys.IdleScheduleNightEndHour]
            ?: defaults.idleScheduleNightEndHour),
        idleScheduleChargingEnabled = prefs[SettingsKeys.IdleScheduleChargingEnabled]
            ?: defaults.idleScheduleChargingEnabled,
        idleScheduleChargingHidden = IdleSchedule.normalizeHidden(prefs[SettingsKeys.IdleScheduleChargingHidden]
            ?: defaults.idleScheduleChargingHidden)
    )
}

private fun SmartIslandSettings.readLedTick(prefs: Preferences): SmartIslandSettings {
    val defaults = SmartIslandSettings.Default
    return copy(
        idleTickStyle = LedTick.normalizeStyle(prefs[SettingsKeys.IdleTickStyle] ?: defaults.idleTickStyle),
        ledTickPattern = LedTick.normalizePattern(prefs[SettingsKeys.LedTickPattern] ?: defaults.ledTickPattern),
        ledTickPaceMs = LedTick.normalizePace(prefs[SettingsKeys.LedTickPaceMs] ?: defaults.ledTickPaceMs),
        ledTickFont = prefs[SettingsKeys.LedTickFont]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) }
            ?: defaults.ledTickFont,
        ledTickColor = LedTick.normalizeColor(prefs[SettingsKeys.LedTickColor] ?: defaults.ledTickColor),
        ledTickStagger = prefs[SettingsKeys.LedTickStagger] ?: defaults.ledTickStagger
    )
}
