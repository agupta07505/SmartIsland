/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import androidx.datastore.preferences.core.MutablePreferences
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

/** Weather API key is not written, backups never hold it. */
internal fun writeSettings(prefs: MutablePreferences, settings: SmartIslandSettings) {
    settings.writePillLook(prefs)
    settings.writeEventColors(prefs)
    settings.writeShortcuts(prefs)
    settings.writeNotificationRules(prefs)
    settings.writeAutoExpand(prefs)
    settings.writeExpandedStyle(prefs)
    settings.writeAppBehaviour(prefs)
    settings.writeNotificationCooldown(prefs)
    settings.writeFaceAndStatusBar(prefs)
    settings.writeCameraCutout(prefs)
    settings.writeGestures(prefs)
    settings.writeLandscape(prefs)
    settings.writeTimings(prefs)
    settings.writeIdleBehaviour(prefs)
    settings.writeIdleWidgets(prefs)
    settings.writeWeather(prefs)
    settings.writeIdleSchedule(prefs)
    settings.writeLedTick(prefs)
}

private fun SmartIslandSettings.writePillLook(prefs: MutablePreferences) {
    prefs[SettingsKeys.Enabled] = enabled
    prefs[SettingsKeys.Width] = validDimension(
        width,
        SmartIslandSettings.Default.width,
        SmartIslandSettings.MIN_WIDTH,
        SmartIslandSettings.MAX_WIDTH
    )
    prefs[SettingsKeys.Height] = validDimension(
        height,
        SmartIslandSettings.Default.height,
        SmartIslandSettings.MIN_HEIGHT,
        SmartIslandSettings.MAX_HEIGHT
    )
    prefs[SettingsKeys.XOffset] = validDimension(
        xOffset,
        SmartIslandSettings.Default.xOffset,
        SmartIslandSettings.MIN_X_OFFSET,
        SmartIslandSettings.MAX_X_OFFSET
    )
    prefs[SettingsKeys.YOffset] = validDimension(
        yOffset,
        SmartIslandSettings.Default.yOffset,
        SmartIslandSettings.MIN_Y_OFFSET,
        SmartIslandSettings.MAX_Y_OFFSET
    )
    prefs[SettingsKeys.CornerRadius] = validDimension(
        cornerRadius,
        SmartIslandSettings.Default.cornerRadius,
        SmartIslandSettings.MIN_CORNER_RADIUS,
        SmartIslandSettings.MAX_CORNER_RADIUS
    )
    prefs[SettingsKeys.Opacity] = validDimension(
        opacity,
        SmartIslandSettings.Default.opacity,
        SmartIslandSettings.MIN_OPACITY,
        SmartIslandSettings.MAX_OPACITY
    )
    prefs[SettingsKeys.PillColor] = validColor(pillColor, SmartIslandSettings.Default.pillColor)
    prefs[SettingsKeys.OledBlack] = oledBlack
    prefs[SettingsKeys.EnableShadow] = enableShadow
    prefs[SettingsKeys.ShadowElevation] = validDimension(
        shadowElevation,
        SmartIslandSettings.Default.shadowElevation,
        SmartIslandSettings.MIN_SHADOW_ELEVATION,
        SmartIslandSettings.MAX_SHADOW_ELEVATION
    )
    prefs[SettingsKeys.EnableMusicArtworkBackground] = enableMusicArtworkBackground
    prefs[SettingsKeys.EnableNotificationBackdrop] = enableNotificationBackdrop
}

private fun SmartIslandSettings.writeEventColors(prefs: MutablePreferences) {
    prefs[SettingsKeys.BatteryColor] = validColor(batteryColor, SmartIslandSettings.Default.batteryColor)
    prefs[SettingsKeys.NotificationDotColor] = validColor(notificationDotColor,
        SmartIslandSettings.Default.notificationDotColor)
    prefs[SettingsKeys.MusicVisualizerColor] = validColor(musicVisualizerColor,
        SmartIslandSettings.Default.musicVisualizerColor)
    prefs[SettingsKeys.HotspotColor] = validColor(hotspotColor, SmartIslandSettings.Default.hotspotColor)
    prefs[SettingsKeys.CallColor] = validColor(callColor, SmartIslandSettings.Default.callColor)
    prefs[SettingsKeys.LiveActivityColor] = validColor(liveActivityColor,
        SmartIslandSettings.Default.liveActivityColor)
    prefs[SettingsKeys.TransferColor] = validColor(transferColor, SmartIslandSettings.Default.transferColor)
    prefs[SettingsKeys.NavigationColor] = validColor(navigationColor,
        SmartIslandSettings.Default.navigationColor)
    prefs[SettingsKeys.BluetoothColor] = validColor(bluetoothColor, SmartIslandSettings.Default.bluetoothColor)
    prefs[SettingsKeys.FlashlightColor] = validColor(flashlightColor,
        SmartIslandSettings.Default.flashlightColor)
    prefs[SettingsKeys.ScreenRecordingColor] = validColor(screenRecordingColor,
        SmartIslandSettings.Default.screenRecordingColor)
    prefs[SettingsKeys.TimerColor] = validColor(timerColor, SmartIslandSettings.Default.timerColor)
    prefs[SettingsKeys.StopwatchColor] = validColor(stopwatchColor, SmartIslandSettings.Default.stopwatchColor)
    prefs[SettingsKeys.EventColors] = EventColors.encode(eventColors)
}

private fun SmartIslandSettings.writeShortcuts(prefs: MutablePreferences) {
    prefs[SettingsKeys.EnableAppShortcuts] = enableAppShortcuts
    writePinnedApps(prefs, shortcutPackages)
    prefs[SettingsKeys.ShowRecentApps] = showRecentApps
    prefs[SettingsKeys.UtilityLayout] = UtilityLayout.encode(
        UtilityLayout.decode(utilityLayout)
    )
    prefs[SettingsKeys.UtilityCalculatorPackage] = utilityCalculatorPackage.take(255)
    prefs[SettingsKeys.UtilityCalculatorFloating] = utilityCalculatorFloating
}

private fun SmartIslandSettings.writeNotificationRules(prefs: MutablePreferences) {
    prefs[SettingsKeys.ShowOnLockScreen] = showOnLockScreen
    prefs[SettingsKeys.ShowInForegroundEvents] = showInForegroundEvents
        .filter { id -> IslandEvent.FOREGROUND_SWITCHABLE.any { it.id == id } }
        .toSet()
    prefs[SettingsKeys.HideInFullscreen] = hideInFullscreen
    prefs[SettingsKeys.HideInShade] = hideInShade
    prefs[SettingsKeys.LockScreenPrivacy] = lockScreenPrivacy.takeIf { it in VALID_LOCK_SCREEN_PRIVACY_VALUES }
        ?: SmartIslandSettings.Default.lockScreenPrivacy
    prefs[SettingsKeys.ShowNotificationActions] = showNotificationActions
    prefs[SettingsKeys.HideFromNotificationShade] = hideFromNotificationShade
    prefs[SettingsKeys.LiveActivitiesEnabled] = liveActivitiesEnabled
    prefs[SettingsKeys.NavigationEnabled] = navigationEnabled
    prefs[SettingsKeys.DisabledNotificationPackages] = disabledNotificationPackages.validPackages()
    prefs[SettingsKeys.DisabledSoundPackages] = disabledSoundPackages
    prefs[SettingsKeys.DisabledEvents] = disabledEvents.validPackages()
    prefs[SettingsKeys.DisabledEventsLandscape] = disabledEventsLandscape.validPackages()
    prefs[SettingsKeys.HideAllOnLockScreen] = hideAllOnLockScreen
    prefs[SettingsKeys.HideOnLockScreenEvents] = hideOnLockScreenEvents
        .filter { id -> IslandEvent.entries.any { it.id == id && it.canHideOnLockScreen } }
        .toSet()
    prefs[SettingsKeys.NotificationsEnabled] = notificationsEnabled
    prefs[SettingsKeys.HideOnLockScreenPackages] = hideOnLockScreenPackages.validPackages()
}

private fun SmartIslandSettings.writeAutoExpand(prefs: MutablePreferences) {
    prefs[SettingsKeys.AutoExpandOnNotification] = autoExpandOnNotification
    prefs[SettingsKeys.AutoExpandOnLockScreen] = autoExpandOnLockScreen
    prefs[SettingsKeys.ShowSuggestedReplies] = showSuggestedReplies
    prefs[SettingsKeys.ShowCopyCodeChip] = showCopyCodeChip
    prefs[SettingsKeys.NoExpandPackages] = noExpandPackages.validPackages()
    prefs[SettingsKeys.NoAutoExpandPackages] = noAutoExpandPackages.validPackages()
    prefs[SettingsKeys.AutoExpandPackages] = autoExpandPackages
        .asSequence()
        .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
        .filter { it !in noAutoExpandPackages }
        .toSet()
    prefs[SettingsKeys.NoAutoExpandEvents] = noAutoExpandEvents
        .filter { id -> IslandEvent.entries.any { it.id == id } }
        .toSet()
    prefs[SettingsKeys.NoAutoExpandEventsLandscape] = noAutoExpandEventsLandscape
        .filter { id -> IslandEvent.entries.any { it.id == id } }
        .toSet()
}

private fun SmartIslandSettings.writeExpandedStyle(prefs: MutablePreferences) {
    prefs[SettingsKeys.ExpandedBlurEnabled] = expandedBlurEnabled
    prefs[SettingsKeys.ExpandedBlurRadius] = expandedBlurRadius
        .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    prefs[SettingsKeys.MediaBlurEnabled] = mediaBlurEnabled
    prefs[SettingsKeys.MediaBlurRadius] = mediaBlurRadius
        .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    prefs[SettingsKeys.MediaOpacity] = validDimension(
        mediaOpacity,
        SmartIslandSettings.Default.mediaOpacity,
        SmartIslandSettings.MIN_OPACITY,
        SmartIslandSettings.MAX_OPACITY
    )
    prefs[SettingsKeys.ExpandedGradientEnabled] = expandedGradientEnabled
    prefs[SettingsKeys.ExpandedGradientColor] = validColor(expandedGradientColor,
        SmartIslandSettings.Default.expandedGradientColor)
    prefs[SettingsKeys.ExpandedGradientStrength] = expandedGradientStrength
        .coerceIn(SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH)
    prefs[SettingsKeys.ThemeMode] = themeMode
    prefs[SettingsKeys.LiquidGlassTheme] = liquidGlassTheme
    prefs[SettingsKeys.MaterialYou] = materialYou
    prefs[SettingsKeys.AccentColor] = validColor(accentColor, SmartIslandSettings.Default.accentColor)
    prefs[SettingsKeys.ExpandedTheme] =
        if (expandedTheme in SmartIslandSettings.VALID_EXPANDED_THEMES) expandedTheme else SmartIslandSettings.Default.expandedTheme
    prefs[SettingsKeys.IslandFont] = islandFont
}

private fun SmartIslandSettings.writeAppBehaviour(prefs: MutablePreferences) {
    prefs[SettingsKeys.WelcomeDialogShown] = welcomeDialogShown
    prefs[SettingsKeys.TorchStrobeAcknowledged] = torchStrobeAcknowledged
    prefs[SettingsKeys.DeviceType] = deviceType
    prefs[SettingsKeys.AllowNetworkChecks] = allowNetworkChecks
    prefs[SettingsKeys.DetectTransfersBySpeed] = detectTransfersBySpeed
    prefs[SettingsKeys.EnableNotificationHistory] = enableNotificationHistory
    prefs[SettingsKeys.NotificationHistoryRetentionHours] = notificationHistoryRetentionHours
    prefs[SettingsKeys.ShowBluetoothBattery] = showBluetoothBattery
    prefs[SettingsKeys.EnableBatteryMode] = enableBatteryMode
    prefs[SettingsKeys.DeveloperModeEnabled] = developerModeEnabled
    prefs[SettingsKeys.RecordLogs] = recordLogs
    prefs[SettingsKeys.ShowDuringDnd] = showDuringDnd
}

private fun SmartIslandSettings.writeNotificationCooldown(prefs: MutablePreferences) {
    prefs[SettingsKeys.EnableNotificationCooldown] = enableNotificationCooldown
    prefs[SettingsKeys.NotificationCooldownDurationMinutes] = notificationCooldownDurationMinutes
    prefs[SettingsKeys.NotificationCooldownThreshold] = notificationCooldownThreshold
    prefs[SettingsKeys.NotificationCooldownExcludedPackages] = notificationCooldownExcludedPackages
    prefs[SettingsKeys.NotificationCooldownSystemEvents] = notificationCooldownSystemEvents
}

private fun SmartIslandSettings.writeFaceAndStatusBar(prefs: MutablePreferences) {
    prefs[SettingsKeys.FaceUnlockMode] = faceUnlockMode
    prefs[SettingsKeys.FaceIdStyle] = faceIdStyle
    prefs[SettingsKeys.AppFaceIdEnabled] = appFaceIdEnabled
    prefs[SettingsKeys.AppFaceIdStyle] = appFaceIdStyle
    prefs[SettingsKeys.StatusBarHidden] = statusBarHidden
    prefs[SettingsKeys.StatusBarHidePersistent] = statusBarHidePersistent
    prefs[SettingsKeys.StatusBarHideNotificationIcons] = statusBarHideNotificationIcons
    prefs[SettingsKeys.StatusBarHideClock] = statusBarHideClock
    prefs[SettingsKeys.StatusBarHideAllSystemIcons] = statusBarHideAllSystemIcons
    prefs[SettingsKeys.StatusBarHiddenSystemIcons] = statusBarHiddenSystemIcons
    prefs[SettingsKeys.FingerprintAnimationEnabled] = fingerprintAnimationEnabled
}

private fun SmartIslandSettings.writeCameraCutout(prefs: MutablePreferences) {
    prefs[SettingsKeys.EnableNotchMode] = enableNotchMode
    prefs[SettingsKeys.CameraCutoutManual] = cameraCutoutManual
    prefs[SettingsKeys.CameraCutoutStyle] = cameraCutoutStyle.takeIf { it in VALID_CUTOUT_STYLES }
        ?: SmartIslandSettings.Default.cameraCutoutStyle
    prefs[SettingsKeys.CameraCutoutX] = validDimension(cameraCutoutX,
        SmartIslandSettings.Default.cameraCutoutX, SmartIslandSettings.MIN_CUTOUT_X, SmartIslandSettings.MAX_CUTOUT_X)
    prefs[SettingsKeys.CameraCutoutY] = validDimension(cameraCutoutY,
        SmartIslandSettings.Default.cameraCutoutY, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET)
    prefs[SettingsKeys.CameraCutoutWidth] = validDimension(cameraCutoutWidth,
        SmartIslandSettings.Default.cameraCutoutWidth, SmartIslandSettings.MIN_CUTOUT_WIDTH,
        SmartIslandSettings.MAX_CUTOUT_WIDTH)
    prefs[SettingsKeys.CameraCutoutHeight] = validDimension(cameraCutoutHeight,
        SmartIslandSettings.Default.cameraCutoutHeight, SmartIslandSettings.MIN_CUTOUT_HEIGHT,
        SmartIslandSettings.MAX_CUTOUT_HEIGHT)
}

private fun SmartIslandSettings.writeGestures(prefs: MutablePreferences) {
    prefs[SettingsKeys.EnableSwipeActions] = enableSwipeActions
    prefs[SettingsKeys.SwipeUpAction] = swipeUpAction
    prefs[SettingsKeys.SwipeHoldUpAction] = swipeHoldUpAction
    prefs[SettingsKeys.SwipeDownAction] = swipeDownAction
    prefs[SettingsKeys.SwipeDownCollapsedAction] = swipeDownCollapsedAction
    prefs[SettingsKeys.SwipeLeftAction] = swipeLeftAction
    prefs[SettingsKeys.SwipeRightAction] = swipeRightAction
    prefs[SettingsKeys.SwipeHorizontalCollapsedAction] = swipeHorizontalCollapsedAction
    prefs[SettingsKeys.EnablePillSwipeActions] = enablePillSwipeActions
    prefs[SettingsKeys.PillSwipeUpAction] = pillSwipeUpAction
    prefs[SettingsKeys.PillSwipeDownAction] = pillSwipeDownAction
    prefs[SettingsKeys.PillSwipeLeftAction] = pillSwipeLeftAction
    prefs[SettingsKeys.PillSwipeRightAction] = pillSwipeRightAction
    prefs[SettingsKeys.LongPressAction] = longPressAction
    prefs[SettingsKeys.HapticStrength] = hapticStrength
        .takeIf { it in HapticStrength.VALID_IDS } ?: SmartIslandSettings.Default.hapticStrength
    prefs[SettingsKeys.GestureSensitivityId] = gestureSensitivity
        .takeIf { it in GestureSensitivity.VALID_IDS } ?: SmartIslandSettings.Default.gestureSensitivity
}

private fun SmartIslandSettings.writeLandscape(prefs: MutablePreferences) {
    prefs[SettingsKeys.ShowInLandscape] = showInLandscape
    prefs[SettingsKeys.CirclePosition] = if (circlePosition in VALID_CIRCLE_POSITIONS) {
        circlePosition
    } else {
        SmartIslandSettings.CIRCLE_POSITION_RIGHT
    }
    prefs[SettingsKeys.LandscapeShape] = landscapeShape.takeIf { it in VALID_LANDSCAPE_SHAPES }
        ?: SmartIslandSettings.Default.landscapeShape
    prefs[SettingsKeys.LandscapeWidth] = landscapeWidth
    prefs[SettingsKeys.LandscapeHeight] = landscapeHeight
    prefs[SettingsKeys.LandscapeXOffset] = landscapeXOffset
    prefs[SettingsKeys.LandscapeYOffset] = landscapeYOffset
    prefs[SettingsKeys.LandscapeCornerRadius] = landscapeCornerRadius
    prefs[SettingsKeys.LandscapeOpacity] = landscapeOpacity
    prefs[SettingsKeys.LandscapeCircleSize] = landscapeCircleSize
    prefs[SettingsKeys.LandscapeCircleOpacity] = landscapeCircleOpacity
    prefs[SettingsKeys.LandscapeCircleEdgeInset] = landscapeCircleEdgeInset
    prefs[SettingsKeys.LandscapeCircleYOffset] = landscapeCircleYOffset
    prefs[SettingsKeys.LandscapeCircleXAdjust] = landscapeCircleXAdjust
    prefs[SettingsKeys.LandscapeCircleYAdjust] = landscapeCircleYAdjust
    prefs[SettingsKeys.LandscapeCirclePosition] =
        landscapeCirclePosition.takeIf { it in VALID_LANDSCAPE_POSITIONS }
        ?: SmartIslandSettings.Default.landscapeCirclePosition
    prefs[SettingsKeys.LandscapePillPosition] =
        landscapePillPosition.takeIf { it in VALID_LANDSCAPE_POSITIONS }
        ?: SmartIslandSettings.Default.landscapePillPosition
    prefs[SettingsKeys.LandscapeLayoutStyle] = LandscapeLayoutStyle.fromId(landscapeLayoutStyle)
    prefs[SettingsKeys.EdgePill] = edgePill.coerced().encode()
}

private fun SmartIslandSettings.writeTimings(prefs: MutablePreferences) {
    prefs[SettingsKeys.HideWhenIdle] = hideWhenIdle
    prefs[SettingsKeys.AutoHidePill] = autoHidePill
    prefs[SettingsKeys.AutoHideTimeoutSeconds] = autoHideTimeoutSeconds
        .coerceIn(SmartIslandSettings.MIN_AUTO_HIDE_SECONDS, SmartIslandSettings.MAX_AUTO_HIDE_SECONDS)
    prefs[SettingsKeys.IdleTimeoutNever] = idleTimeoutNever
    prefs[SettingsKeys.IdleTimeoutMs] = idleTimeoutMs
        .coerceIn(SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
    prefs[SettingsKeys.BatteryWarningMs] = batteryWarningMs
        .coerceIn(SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS)
    prefs[SettingsKeys.PulseHoldPercent] = pulseHoldPercent
        .coerceIn(SmartIslandSettings.MIN_PULSE_HOLD_PERCENT, SmartIslandSettings.MAX_PULSE_HOLD_PERCENT)
    prefs[SettingsKeys.TransferExpiryMinutes] = transferExpiryMinutes
        .coerceIn(0, SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES)
    prefs[SettingsKeys.SecondaryMergeMs] = secondaryMergeMs
        .coerceIn(SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS)
    prefs[SettingsKeys.AnimSpeedPortrait] = animSpeedPortrait
        .takeIf { it in AnimationSpeed.VALID_IDS } ?: SmartIslandSettings.Default.animSpeedPortrait
    prefs[SettingsKeys.AnimSpeedLandscape] = animSpeedLandscape
        .takeIf { it in AnimationSpeed.VALID_IDS } ?: SmartIslandSettings.Default.animSpeedLandscape
    prefs[SettingsKeys.MergeStyleId] = mergeStyle
        .takeIf { it in MergeStyle.VALID_IDS } ?: SmartIslandSettings.Default.mergeStyle
    prefs[SettingsKeys.MergeStyleLandscapeId] = mergeStyleLandscape
        .takeIf { it in MergeStyle.VALID_IDS } ?: SmartIslandSettings.Default.mergeStyleLandscape
    prefs[SettingsKeys.FloatingWindowEnabled] = floatingWindowEnabled
    prefs[SettingsKeys.FloatingWindowMode] = floatingWindowMode
        .takeIf { it in FloatingWindowMode.VALID_IDS } ?: SmartIslandSettings.Default.floatingWindowMode
}

private fun SmartIslandSettings.writeIdleBehaviour(prefs: MutablePreferences) {
    prefs[SettingsKeys.IdleWidgetsEnabled] = idleWidgetsEnabled
    prefs[SettingsKeys.IdleWidgetsEnabledLandscape] = idleWidgetsEnabledLandscape
    prefs[SettingsKeys.IdleOffAfterTapClose] = idleOffAfterTapClose
    prefs[SettingsKeys.IdleOffAfterTapCloseLandscape] = idleOffAfterTapCloseLandscape
    prefs[SettingsKeys.IdleOffAfterLongPress] = idleOffAfterLongPress
    prefs[SettingsKeys.IdleOffAfterLongPressLandscape] = idleOffAfterLongPressLandscape
    prefs[SettingsKeys.IdleOffWhenStored] = idleOffWhenStored
    prefs[SettingsKeys.ContentPillSameAsIdle] = contentPillSameAsIdle
    prefs[SettingsKeys.ContentPillWidthPercent] = contentPillWidthPercent
    prefs[SettingsKeys.IslandExemptApps] = islandExemptApps.validPackages()
    prefs[SettingsKeys.IslandExemptAppsLandscape] = islandExemptAppsLandscape.validPackages()
    prefs[SettingsKeys.IslandExemptAppsStatic] = islandExemptAppsStatic.validPackages()
    prefs[SettingsKeys.ContentPillSameAsIdleLandscape] = contentPillSameAsIdleLandscape
    prefs[SettingsKeys.ContentPillWidthPercentLandscape] = contentPillWidthPercentLandscape
    prefs[SettingsKeys.IdleOffWhenStoredLandscape] = idleOffWhenStoredLandscape
}

private fun SmartIslandSettings.writeIdleWidgets(prefs: MutablePreferences) {
    prefs[SettingsKeys.IdleGlyphTickSeconds] = IdleSlots.normalizeTickSeconds(idleGlyphTickSeconds)
    prefs[SettingsKeys.IdleWidgetIds] = IdleWidgets.normalize(idleWidgetIds)
    prefs[SettingsKeys.IdleWidgetCycleSeconds] = idleWidgetCycleSeconds.coerceIn(
        IdleWidgets.MIN_CYCLE_SECONDS,
        IdleWidgets.MAX_CYCLE_SECONDS
    )
    prefs[SettingsKeys.WidgetIslandCloseSeconds] = WidgetIslandTimer.normalize(widgetIslandCloseSeconds)
    prefs[SettingsKeys.IdleAlarmHideBeyond12h] = idleAlarmHideBeyond12h
    prefs[SettingsKeys.BatteryOnlyWhenCharging] = batteryOnlyWhenCharging
    prefs[SettingsKeys.IdleBeatOnlyOnChange] = idleBeatOnlyOnChange
    prefs[SettingsKeys.WidgetIslandBatteryMetric] = widgetIslandBatteryMetric
        .takeIf { it in WidgetIslandContent.VALID_BATTERY_METRICS } ?: WidgetIslandContent.BATTERY_WATT
    prefs[SettingsKeys.IdleGlyphFont] = idleGlyphFont.takeIf { it.isEmpty()
        || SmartIslandSettings.isValidFont(it) } ?: ""
    prefs[SettingsKeys.FontMixEnabled] = fontMixEnabled
    prefs[SettingsKeys.FontMixLight] = fontMixLight.takeIf { it.isEmpty()
        || SmartIslandSettings.isValidFont(it) } ?: ""
    prefs[SettingsKeys.FontMixRegular] = fontMixRegular.takeIf { it.isEmpty()
        || SmartIslandSettings.isValidFont(it) } ?: ""
    prefs[SettingsKeys.FontMixMedium] = fontMixMedium.takeIf { it.isEmpty()
        || SmartIslandSettings.isValidFont(it) } ?: ""
    prefs[SettingsKeys.IdleGlyphScale] = IdleSlots.normalizeGlyphScale(idleGlyphScale)
    prefs[SettingsKeys.IdleWidgetTempUnit] = idleWidgetTempUnit
        .takeIf { it in IdleWidgets.VALID_UNITS }
        ?: IdleWidgets.UNIT_AUTO
    prefs[SettingsKeys.IdleWidgetWindUnit] = idleWidgetWindUnit
        .takeIf { it in IdleWidgets.VALID_WIND_UNITS }
        ?: IdleWidgets.UNIT_AUTO
    prefs[SettingsKeys.IdleWidgetPinnedFirst] = IdleSchedule.normalizePinned(idleWidgetPinnedFirst)
}

private fun SmartIslandSettings.writeWeather(prefs: MutablePreferences) {
    prefs[SettingsKeys.WeatherLocationSource] = WeatherConfig.normalizeSource(weatherLocationSource)
    prefs[SettingsKeys.WeatherManualCity] = WeatherConfig.normalizeCity(weatherManualCity)
    prefs[SettingsKeys.WeatherManualLat] = weatherManualLat
    prefs[SettingsKeys.WeatherManualLon] = weatherManualLon
    prefs[SettingsKeys.WeatherRefreshMinutes] = WeatherConfig.normalizeRefreshMinutes(weatherRefreshMinutes)
    prefs[SettingsKeys.WeatherProvider] = WeatherConfig.normalizeProvider(weatherProvider)
    prefs[SettingsKeys.WeatherCustomUrl] = WeatherConfig.normalizeCustomUrl(weatherCustomUrl)
    // a restore does not carry the API key (backups never hold it)
    prefs[SettingsKeys.WeatherPillMode] = WeatherPill.normalize(weatherPillMode)
    prefs[SettingsKeys.WeatherIslandReadouts] = WeatherReadouts.normalize(weatherIslandReadouts)
    prefs[SettingsKeys.WeatherIconAnimated] = weatherIconAnimated
    prefs[SettingsKeys.WeatherIconPauseOnSaver] = weatherIconPauseOnSaver
    prefs[SettingsKeys.WeatherShowRule] = WeatherRules.normalizeMode(weatherShowRule)
    prefs[SettingsKeys.WeatherNotableTriggers] = WeatherRules.normalizeTriggers(weatherNotableTriggers)
    prefs[SettingsKeys.WeatherHotC] = WeatherRules.normalizeHotC(weatherHotC)
    prefs[SettingsKeys.WeatherColdC] = WeatherRules.normalizeColdC(weatherColdC)
    prefs[SettingsKeys.WeatherHideAtNight] = weatherHideAtNight
    prefs[SettingsKeys.WeatherNightStartHour] = WeatherRules.normalizeHour(weatherNightStartHour)
    prefs[SettingsKeys.WeatherNightEndHour] = WeatherRules.normalizeHour(weatherNightEndHour)
    prefs[SettingsKeys.NetSpeedThresholdKbps] = NetSpeedRules.normalize(netSpeedThresholdKbps)
    prefs[SettingsKeys.NetSpeedConnection] = NetSpeedRules.normalizeConnection(netSpeedConnection)
}

private fun SmartIslandSettings.writeIdleSchedule(prefs: MutablePreferences) {
    prefs[SettingsKeys.IdleScheduleWorkEnabled] = idleScheduleWorkEnabled
    prefs[SettingsKeys.IdleScheduleWorkHidden] = IdleSchedule.normalizeHidden(idleScheduleWorkHidden)
    prefs[SettingsKeys.IdleScheduleWorkStartHour] = IdleSchedule.normalizeHour(idleScheduleWorkStartHour)
    prefs[SettingsKeys.IdleScheduleWorkEndHour] = IdleSchedule.normalizeHour(idleScheduleWorkEndHour)
    prefs[SettingsKeys.IdleScheduleWorkWeekdaysOnly] = idleScheduleWorkWeekdaysOnly
    prefs[SettingsKeys.IdleScheduleNightEnabled] = idleScheduleNightEnabled
    prefs[SettingsKeys.IdleScheduleNightHidden] = IdleSchedule.normalizeHidden(idleScheduleNightHidden)
    prefs[SettingsKeys.IdleScheduleNightStartHour] = IdleSchedule.normalizeHour(idleScheduleNightStartHour)
    prefs[SettingsKeys.IdleScheduleNightEndHour] = IdleSchedule.normalizeHour(idleScheduleNightEndHour)
    prefs[SettingsKeys.IdleScheduleChargingEnabled] = idleScheduleChargingEnabled
    prefs[SettingsKeys.IdleScheduleChargingHidden] = IdleSchedule.normalizeHidden(idleScheduleChargingHidden)
}

private fun SmartIslandSettings.writeLedTick(prefs: MutablePreferences) {
    prefs[SettingsKeys.IdleTickStyle] = LedTick.normalizeStyle(idleTickStyle)
    prefs[SettingsKeys.LedTickPattern] = LedTick.normalizePattern(ledTickPattern)
    prefs[SettingsKeys.LedTickPaceMs] = LedTick.normalizePace(ledTickPaceMs)
    prefs[SettingsKeys.LedTickFont] = ledTickFont.takeIf { it.isEmpty()
        || SmartIslandSettings.isValidFont(it) } ?: ""
    prefs[SettingsKeys.LedTickColor] = LedTick.normalizeColor(ledTickColor)
    prefs[SettingsKeys.LedTickStagger] = ledTickStagger
}

internal fun writePinnedApps(prefs: MutablePreferences, value: List<String>) {
    prefs[SettingsKeys.ShortcutOrder] = PinnedApps.encode(value)
    prefs.remove(SettingsKeys.ShortcutPackages)
}
