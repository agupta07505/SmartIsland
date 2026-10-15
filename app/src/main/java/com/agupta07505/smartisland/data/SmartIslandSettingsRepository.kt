/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.MergeStyle

private val Context.smartIslandDataStore by preferencesDataStore(
    name = "smart_island_settings",
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.e("SmartIslandSettings", "Settings were corrupted; restoring safe defaults")
        emptyPreferences()
    }
)

class SmartIslandSettingsRepository(private val context: Context) {
    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val Width = floatPreferencesKey("width")
        val Height = floatPreferencesKey("height")
        val XOffset = floatPreferencesKey("x_offset")
        val YOffset = floatPreferencesKey("y_offset")
        val CornerRadius = floatPreferencesKey("corner_radius")
        val Opacity = floatPreferencesKey("opacity")
        val PillColor = longPreferencesKey("pill_color")
        val BatteryColor = longPreferencesKey("battery_color")
        val NotificationDotColor = longPreferencesKey("notification_dot_color")
        val MusicVisualizerColor = longPreferencesKey("music_visualizer_color")
        val HotspotColor = longPreferencesKey("hotspot_color")
        val CallColor = longPreferencesKey("call_color")
        val LiveActivityColor = longPreferencesKey("live_activity_color")
        val TransferColor = longPreferencesKey("transfer_color")
        val NavigationColor = longPreferencesKey("navigation_color")
        val BluetoothColor = longPreferencesKey("bluetooth_color")
        val FlashlightColor = longPreferencesKey("flashlight_color")
        val ScreenRecordingColor = longPreferencesKey("screen_recording_color")
        val TimerColor = longPreferencesKey("timer_color")
        val StopwatchColor = longPreferencesKey("stopwatch_color")
        val EnableAppShortcuts = booleanPreferencesKey("enable_app_shortcuts")
        /** Legacy unordered set. Read once to migrate, then removed whenever the ordered list below is written. */
        val ShortcutPackages = stringSetPreferencesKey("shortcut_packages")
        val ShortcutOrder = stringPreferencesKey("shortcut_order")
        val ShowRecentApps = booleanPreferencesKey("show_recent_apps")
        val UtilityLayout = stringPreferencesKey("utility_layout")
        val UtilityCalculatorPackage = stringPreferencesKey("utility_calculator_package")
        val UtilityCalculatorFloating = booleanPreferencesKey("utility_calculator_floating")
        val WelcomeDialogShown = booleanPreferencesKey("welcome_dialog_shown")
        val ShowOnLockScreen = booleanPreferencesKey("show_on_lock_screen")
        val ShowLiveEventsInForeground = booleanPreferencesKey("show_live_events_in_foreground")
        val LockScreenPrivacy = stringPreferencesKey("lock_screen_privacy")
        val ShowNotificationActions = booleanPreferencesKey("show_notification_actions")
        val HideFromNotificationShade = booleanPreferencesKey("hide_from_notification_shade")
        val LiveActivitiesEnabled = booleanPreferencesKey("live_activities_enabled")
        val NavigationEnabled = booleanPreferencesKey("navigation_enabled")
        val DisabledNotificationPackages = stringSetPreferencesKey("disabled_notification_packages")
        val DisabledSoundPackages = stringSetPreferencesKey("disabled_sound_packages")
        val DisabledEvents = stringSetPreferencesKey("disabled_events")
        val HideWhenIdle = booleanPreferencesKey("hide_when_idle")
        val AutoHidePill = booleanPreferencesKey("auto_hide_pill")
        val AutoHideTimeoutSeconds = intPreferencesKey("auto_hide_timeout_seconds")
        val ShowInLandscape = booleanPreferencesKey("show_in_landscape")
        val AutoExpandOnNotification = booleanPreferencesKey("auto_expand_on_notification")
        val AutoExpandOnLockScreen = booleanPreferencesKey("auto_expand_on_lock_screen")
        val EnableShadow = booleanPreferencesKey("enable_shadow")
        val ExpandedBlurEnabled = booleanPreferencesKey("expanded_blur_enabled")
        val ExpandedBlurRadius = floatPreferencesKey("expanded_blur_radius")
        val MediaBlurEnabled = booleanPreferencesKey("media_blur_enabled")
        val MediaBlurRadius = floatPreferencesKey("media_blur_radius")
        val MediaOpacity = floatPreferencesKey("media_opacity")
        val ExpandedGradientEnabled = booleanPreferencesKey("expanded_gradient_enabled")
        val ExpandedGradientColor = longPreferencesKey("expanded_gradient_color")
        val ExpandedGradientStrength = floatPreferencesKey("expanded_gradient_strength")
        val ShadowElevation = floatPreferencesKey("shadow_elevation")
        val EnableMusicArtworkBackground = booleanPreferencesKey("enable_music_artwork_background")
        val EnableNotificationBackdrop = booleanPreferencesKey("enable_notification_backdrop")
        val DeviceType = stringPreferencesKey("device_type")
        val AllowNetworkChecks = booleanPreferencesKey("allow_network_checks")
        val DetectTransfersBySpeed = booleanPreferencesKey("detect_transfers_by_speed")
        val EnableNotificationHistory = booleanPreferencesKey("enable_notification_history")
        val NotificationHistoryRetentionHours = intPreferencesKey("notification_history_retention_hours")
        val ShowBluetoothBattery = booleanPreferencesKey("show_bluetooth_battery")
        val EnableBatteryMode = booleanPreferencesKey("enable_battery_mode")
        val EnableNotificationCooldown = booleanPreferencesKey("enable_notification_cooldown")
        val NotificationCooldownDurationMinutes = intPreferencesKey("notification_cooldown_duration_minutes")
        val NotificationCooldownThreshold = intPreferencesKey("notification_cooldown_threshold")
        val NotificationCooldownExcludedPackages = stringSetPreferencesKey("notification_cooldown_excluded_packages")
        val NotificationCooldownSystemEvents = stringSetPreferencesKey("notification_cooldown_system_events")
        val DeveloperModeEnabled = booleanPreferencesKey("developer_mode_enabled")
        val RecordLogs = booleanPreferencesKey("record_logs")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val LiquidGlassTheme = booleanPreferencesKey("liquid_glass_theme")
        val MaterialYou = booleanPreferencesKey("material_you")
        val AccentColor = longPreferencesKey("accent_color")
        val ExpandedTheme = stringPreferencesKey("expanded_theme")
        val IslandFont = stringPreferencesKey("island_font")
        val EnableNotchMode = booleanPreferencesKey("enable_notch_mode")
        val CameraCutoutManual = booleanPreferencesKey("camera_cutout_manual")
        val CameraCutoutX = floatPreferencesKey("camera_cutout_x")
        val CameraCutoutY = floatPreferencesKey("camera_cutout_y")
        val CameraCutoutWidth = floatPreferencesKey("camera_cutout_width")
        val CameraCutoutHeight = floatPreferencesKey("camera_cutout_height")
        val EnableSwipeActions = booleanPreferencesKey("enable_swipe_actions")
        val SwipeUpAction = stringPreferencesKey("swipe_up_action")
        val SwipeHoldUpAction = stringPreferencesKey("swipe_hold_up_action")
        val SwipeDownAction = stringPreferencesKey("swipe_down_action")
        val SwipeDownCollapsedAction = stringPreferencesKey("swipe_down_collapsed_action")
        val SwipeLeftAction = stringPreferencesKey("swipe_left_action")
        val SwipeRightAction = stringPreferencesKey("swipe_right_action")
        val SwipeHorizontalCollapsedAction = stringPreferencesKey("swipe_horizontal_collapsed_action")
        val EnablePillSwipeActions = booleanPreferencesKey("enable_pill_swipe_actions")
        val PillSwipeUpAction = stringPreferencesKey("pill_swipe_up_action")
        val PillSwipeDownAction = stringPreferencesKey("pill_swipe_down_action")
        val PillSwipeLeftAction = stringPreferencesKey("pill_swipe_left_action")
        val PillSwipeRightAction = stringPreferencesKey("pill_swipe_right_action")
        val LongPressAction = stringPreferencesKey("long_press_action")
        val CirclePosition = stringPreferencesKey("circle_position")
        val LandscapeShape = stringPreferencesKey("landscape_shape")
        val LandscapeWidth = floatPreferencesKey("landscape_width")
        val LandscapeHeight = floatPreferencesKey("landscape_height")
        val LandscapeXOffset = floatPreferencesKey("landscape_x_offset")
        val LandscapeYOffset = floatPreferencesKey("landscape_y_offset")
        val LandscapeCornerRadius = floatPreferencesKey("landscape_corner_radius")
        val LandscapeOpacity = floatPreferencesKey("landscape_opacity")
        val LandscapeCircleSize = floatPreferencesKey("landscape_circle_size")
        val LandscapeCircleOpacity = floatPreferencesKey("landscape_circle_opacity")
        val LandscapeCircleEdgeInset = floatPreferencesKey("landscape_circle_edge_inset")
        val LandscapeCircleYOffset = floatPreferencesKey("landscape_circle_y_offset")
        val LandscapeCirclePosition = stringPreferencesKey("landscape_circle_position")
        val LandscapePillPosition = stringPreferencesKey("landscape_pill_position")
        val LandscapeCircleXAdjust = floatPreferencesKey("landscape_circle_x_adjust")
        val LandscapeCircleYAdjust = floatPreferencesKey("landscape_circle_y_adjust")
        val DisabledEventsLandscape = stringSetPreferencesKey("disabled_events_landscape")
        val IdleTimeoutMs = intPreferencesKey("idle_timeout_ms")
        val BatteryWarningMs = intPreferencesKey("battery_warning_ms")
        val PopupMs = intPreferencesKey("popup_ms")
        val TransferExpiryMinutes = intPreferencesKey("transfer_expiry_minutes")
        val IdleWidgetsEnabled = booleanPreferencesKey("idle_widgets_enabled")
        val IdleWidgetsEnabledLandscape = booleanPreferencesKey("idle_widgets_enabled_landscape")
        val IdleOffAfterTapClose = booleanPreferencesKey("idle_off_after_tap_close")
        val IdleOffAfterTapCloseLandscape = booleanPreferencesKey("idle_off_after_tap_close_landscape")
        val IdleOffAfterLongPress = booleanPreferencesKey("idle_off_after_long_press")
        val IdleOffAfterLongPressLandscape = booleanPreferencesKey("idle_off_after_long_press_landscape")
        val ContentPillSameAsIdle = booleanPreferencesKey("content_pill_same_as_idle")
        val ContentPillWidthPercent = floatPreferencesKey("content_pill_width_percent")
        val IslandExemptApps = stringSetPreferencesKey("island_exempt_apps")
        val IslandExemptAppsLandscape = stringSetPreferencesKey("island_exempt_apps_landscape")
        val IslandExemptAppsStatic = stringSetPreferencesKey("island_exempt_apps_static")
        val ContentPillSameAsIdleLandscape = booleanPreferencesKey("content_pill_same_as_idle_landscape")
        val ContentPillWidthPercentLandscape = floatPreferencesKey("content_pill_width_percent_landscape")
        val IdleOffWhenStored = booleanPreferencesKey("idle_off_when_stored")
        val IdleOffWhenStoredLandscape = booleanPreferencesKey("idle_off_when_stored_landscape")
        val IdleGlyphTickSeconds = floatPreferencesKey("idle_glyph_tick_seconds")
        val IdleWidgetIds = stringPreferencesKey("idle_widget_ids")
        val IdleWidgetCycleSeconds = intPreferencesKey("idle_widget_cycle_seconds")
        val WidgetIslandCloseSeconds = intPreferencesKey("widget_island_close_seconds")
        val IdleAlarmHideBeyond12h = booleanPreferencesKey("idle_alarm_hide_beyond_12h")
        val IdleWidgetTempUnit = stringPreferencesKey("idle_widget_temp_unit")
        val IdleWidgetWindUnit = stringPreferencesKey("idle_widget_wind_unit")
        val IdleTickStyle = stringPreferencesKey("idle_tick_style")
        val LedTickPattern = stringPreferencesKey("led_tick_pattern")
        val LedTickPaceMs = intPreferencesKey("led_tick_pace_ms")
        val LedTickFont = stringPreferencesKey("led_tick_font")
        val LedTickColor = stringPreferencesKey("led_tick_color")
        val LedTickStagger = booleanPreferencesKey("led_tick_stagger")
        val IdleGlyphFont = stringPreferencesKey("idle_glyph_font")
        val FontMixEnabled = booleanPreferencesKey("font_mix_enabled")
        val FontMixLight = stringPreferencesKey("font_mix_light")
        val FontMixRegular = stringPreferencesKey("font_mix_regular")
        val FontMixMedium = stringPreferencesKey("font_mix_medium")
        val IdleGlyphScale = intPreferencesKey("idle_glyph_scale")
        val WidgetIslandBatteryMetric = stringPreferencesKey("widget_island_battery_metric")
        val SecondaryMergeMs = intPreferencesKey("secondary_merge_ms")
        val AnimSpeedPortrait = stringPreferencesKey("anim_speed_portrait")
        val AnimSpeedLandscape = stringPreferencesKey("anim_speed_landscape")
        val MergeStyleId = stringPreferencesKey("merge_style")
        val MergeStyleLandscapeId = stringPreferencesKey("merge_style_landscape")
        val ShowDuringDnd = booleanPreferencesKey("show_during_dnd")
        val HapticStrength = stringPreferencesKey("haptic_strength")
        val GestureSensitivityId = stringPreferencesKey("gesture_sensitivity")
        val FloatingWindowEnabled = booleanPreferencesKey("floating_window_enabled")
        val FloatingWindowMode = stringPreferencesKey("floating_window_mode")
        val NoExpandPackages = stringSetPreferencesKey("no_expand_packages")
        val NoAutoExpandPackages = stringSetPreferencesKey("no_auto_expand_packages")
        val AutoExpandPackages = stringSetPreferencesKey("auto_expand_packages")
        val NoAutoExpandEvents = stringSetPreferencesKey("no_auto_expand_events")
        val HideAllOnLockScreen = booleanPreferencesKey("hide_all_on_lock_screen")
        val HideOnLockScreenEvents = stringSetPreferencesKey("hide_on_lock_screen_events")
        val NotificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val HideOnLockScreenPackages = stringSetPreferencesKey("hide_on_lock_screen_packages")
        /** Set once the landscape shape was moved to the top-middle pill (landscape overhaul L2). */
        val LandscapePillMigrated = booleanPreferencesKey("landscape_pill_migrated_l2")
    }

    /**
     * One-time: landscape used to default to the camera circle, and a saved "circle" would keep
     * the old look. Moves it to the top-middle pill once; the user can pick the circle again
     * afterwards and it then stays.
     */
    private suspend fun migrateLandscapeShapeOnce() {
        val prefs = runCatching { context.smartIslandDataStore.data.first() }.getOrNull() ?: return
        if (prefs[Keys.LandscapePillMigrated] == true) return
        editSafely {
            if (it[Keys.LandscapePillMigrated] == true) return@editSafely
            it[Keys.LandscapeShape] = SmartIslandSettings.LANDSCAPE_SHAPE_PILL
            it[Keys.LandscapePillMigrated] = true
        }
    }

    val settings: Flow<SmartIslandSettings> = context.smartIslandDataStore.data
        .onStart { migrateLandscapeShapeOnce() }
        .catch { error ->
            if (error is IOException) {
                Log.e(TAG, "Unable to read settings; using safe defaults", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val defaults = SmartIslandSettings.Default
            SmartIslandSettings(
                enabled = prefs[Keys.Enabled] ?: defaults.enabled,
                width = validDimension(
                    prefs[Keys.Width],
                    defaults.width,
                    SmartIslandSettings.MIN_WIDTH,
                    SmartIslandSettings.MAX_WIDTH
                ),
                height = validDimension(
                    prefs[Keys.Height],
                    defaults.height,
                    SmartIslandSettings.MIN_HEIGHT,
                    SmartIslandSettings.MAX_HEIGHT
                ),
                xOffset = validDimension(
                    prefs[Keys.XOffset],
                    defaults.xOffset,
                    SmartIslandSettings.MIN_X_OFFSET,
                    SmartIslandSettings.MAX_X_OFFSET
                ),
                yOffset = validDimension(
                    prefs[Keys.YOffset],
                    defaults.yOffset,
                    SmartIslandSettings.MIN_Y_OFFSET,
                    SmartIslandSettings.MAX_Y_OFFSET
                ),
                cornerRadius = validDimension(
                    prefs[Keys.CornerRadius],
                    defaults.cornerRadius,
                    SmartIslandSettings.MIN_CORNER_RADIUS,
                    SmartIslandSettings.MAX_CORNER_RADIUS
                ),
                opacity = validDimension(
                    prefs[Keys.Opacity],
                    defaults.opacity,
                    SmartIslandSettings.MIN_OPACITY,
                    SmartIslandSettings.MAX_OPACITY
                ),
                pillColor = validColor(prefs[Keys.PillColor], defaults.pillColor),
                batteryColor = validColor(prefs[Keys.BatteryColor], defaults.batteryColor),
                notificationDotColor = validColor(
                    prefs[Keys.NotificationDotColor],
                    defaults.notificationDotColor
                ),
                musicVisualizerColor = validColor(
                    prefs[Keys.MusicVisualizerColor],
                    defaults.musicVisualizerColor
                ),
                hotspotColor = validColor(prefs[Keys.HotspotColor], defaults.hotspotColor),
                callColor = validColor(prefs[Keys.CallColor], defaults.callColor),
                liveActivityColor = validColor(prefs[Keys.LiveActivityColor], defaults.liveActivityColor),
                transferColor = validColor(prefs[Keys.TransferColor], defaults.transferColor),
                navigationColor = validColor(prefs[Keys.NavigationColor], defaults.navigationColor),
                bluetoothColor = validColor(prefs[Keys.BluetoothColor], defaults.bluetoothColor),
                flashlightColor = validColor(prefs[Keys.FlashlightColor], defaults.flashlightColor),
                screenRecordingColor = validColor(prefs[Keys.ScreenRecordingColor], defaults.screenRecordingColor),
                timerColor = validColor(prefs[Keys.TimerColor], defaults.timerColor),
                stopwatchColor = validColor(prefs[Keys.StopwatchColor], defaults.stopwatchColor),
                enableAppShortcuts = prefs[Keys.EnableAppShortcuts] ?: defaults.enableAppShortcuts,
                shortcutPackages = if (prefs[Keys.ShortcutOrder] == null && prefs[Keys.ShortcutPackages] == null) {
                    defaults.shortcutPackages
                } else {
                    PinnedApps.restore(prefs[Keys.ShortcutOrder], prefs[Keys.ShortcutPackages])
                },
                showRecentApps = prefs[Keys.ShowRecentApps] ?: defaults.showRecentApps,
                utilityLayout = prefs[Keys.UtilityLayout] ?: defaults.utilityLayout,
                utilityCalculatorPackage = prefs[Keys.UtilityCalculatorPackage] ?: defaults.utilityCalculatorPackage,
                utilityCalculatorFloating = prefs[Keys.UtilityCalculatorFloating] ?: defaults.utilityCalculatorFloating,
                welcomeDialogShown = prefs[Keys.WelcomeDialogShown] ?: defaults.welcomeDialogShown,
                showOnLockScreen = prefs[Keys.ShowOnLockScreen] ?: defaults.showOnLockScreen,
                showLiveEventsInForeground = prefs[Keys.ShowLiveEventsInForeground] ?: defaults.showLiveEventsInForeground,
                lockScreenPrivacy = prefs[Keys.LockScreenPrivacy]
                    ?.takeIf { it in VALID_LOCK_SCREEN_PRIVACY_VALUES }
                    ?: defaults.lockScreenPrivacy,
                showNotificationActions = prefs[Keys.ShowNotificationActions]
                    ?: defaults.showNotificationActions,
                hideFromNotificationShade = prefs[Keys.HideFromNotificationShade]
                    ?: defaults.hideFromNotificationShade,
                liveActivitiesEnabled = prefs[Keys.LiveActivitiesEnabled]
                    ?: defaults.liveActivitiesEnabled,
                navigationEnabled = prefs[Keys.NavigationEnabled]
                    ?: defaults.navigationEnabled,
                disabledNotificationPackages = prefs[Keys.DisabledNotificationPackages]
                    ?.asSequence()
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.disabledNotificationPackages,
                disabledSoundPackages = prefs[Keys.DisabledSoundPackages]
                    ?.asSequence()
                    ?.filter { pkg -> pkg.isNotBlank() && pkg.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.disabledSoundPackages,
                disabledEvents = prefs[Keys.DisabledEvents]
                    ?.filter { it.isNotBlank() && it.length <= 64 }
                    ?.toSet()
                    ?: defaults.disabledEvents,
                hideWhenIdle = prefs[Keys.HideWhenIdle] ?: defaults.hideWhenIdle,
                autoHidePill = prefs[Keys.AutoHidePill] ?: defaults.autoHidePill,
                autoHideTimeoutSeconds = prefs[Keys.AutoHideTimeoutSeconds] ?: defaults.autoHideTimeoutSeconds,
                showInLandscape = prefs[Keys.ShowInLandscape] ?: defaults.showInLandscape,
                autoExpandOnNotification = prefs[Keys.AutoExpandOnNotification] ?: defaults.autoExpandOnNotification,
                autoExpandOnLockScreen = prefs[Keys.AutoExpandOnLockScreen] ?: defaults.autoExpandOnLockScreen,
                enableShadow = prefs[Keys.EnableShadow] ?: defaults.enableShadow,
                expandedBlurEnabled = prefs[Keys.ExpandedBlurEnabled] ?: defaults.expandedBlurEnabled,
                expandedBlurRadius = (prefs[Keys.ExpandedBlurRadius] ?: defaults.expandedBlurRadius)
                    .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR),
                // Media keys fall back to the shared values until the owner sets them.
                mediaBlurEnabled = prefs[Keys.MediaBlurEnabled] ?: prefs[Keys.ExpandedBlurEnabled] ?: defaults.mediaBlurEnabled,
                mediaBlurRadius = (prefs[Keys.MediaBlurRadius] ?: prefs[Keys.ExpandedBlurRadius] ?: defaults.mediaBlurRadius)
                    .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR),
                mediaOpacity = validDimension(
                    prefs[Keys.MediaOpacity] ?: prefs[Keys.Opacity],
                    defaults.mediaOpacity,
                    SmartIslandSettings.MIN_OPACITY,
                    SmartIslandSettings.MAX_OPACITY
                ),
                expandedGradientEnabled = prefs[Keys.ExpandedGradientEnabled] ?: defaults.expandedGradientEnabled,
                expandedGradientColor = validColor(prefs[Keys.ExpandedGradientColor], defaults.expandedGradientColor),
                expandedGradientStrength = (prefs[Keys.ExpandedGradientStrength] ?: defaults.expandedGradientStrength)
                    .coerceIn(SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH),
                shadowElevation = validDimension(
                    prefs[Keys.ShadowElevation],
                    defaults.shadowElevation,
                    SmartIslandSettings.MIN_SHADOW_ELEVATION,
                    SmartIslandSettings.MAX_SHADOW_ELEVATION
                ),
                enableMusicArtworkBackground = prefs[Keys.EnableMusicArtworkBackground] ?: defaults.enableMusicArtworkBackground,
                enableNotificationBackdrop = prefs[Keys.EnableNotificationBackdrop] ?: defaults.enableNotificationBackdrop,
                deviceType = prefs[Keys.DeviceType] ?: defaults.deviceType,
                allowNetworkChecks = prefs[Keys.AllowNetworkChecks] ?: defaults.allowNetworkChecks,
                detectTransfersBySpeed = prefs[Keys.DetectTransfersBySpeed] ?: defaults.detectTransfersBySpeed,
                enableNotificationHistory = prefs[Keys.EnableNotificationHistory] ?: defaults.enableNotificationHistory,
                notificationHistoryRetentionHours = prefs[Keys.NotificationHistoryRetentionHours] ?: defaults.notificationHistoryRetentionHours,
                showBluetoothBattery = prefs[Keys.ShowBluetoothBattery] ?: defaults.showBluetoothBattery,
                enableBatteryMode = prefs[Keys.EnableBatteryMode] ?: defaults.enableBatteryMode,
                enableNotificationCooldown = prefs[Keys.EnableNotificationCooldown] ?: defaults.enableNotificationCooldown,
                notificationCooldownDurationMinutes = prefs[Keys.NotificationCooldownDurationMinutes] ?: defaults.notificationCooldownDurationMinutes,
                notificationCooldownThreshold = prefs[Keys.NotificationCooldownThreshold] ?: defaults.notificationCooldownThreshold,
                notificationCooldownExcludedPackages = prefs[Keys.NotificationCooldownExcludedPackages] ?: defaults.notificationCooldownExcludedPackages,
                notificationCooldownSystemEvents = prefs[Keys.NotificationCooldownSystemEvents] ?: defaults.notificationCooldownSystemEvents,
                developerModeEnabled = prefs[Keys.DeveloperModeEnabled] ?: defaults.developerModeEnabled,
                recordLogs = prefs[Keys.RecordLogs] ?: defaults.recordLogs,
                themeMode = prefs[Keys.ThemeMode] ?: defaults.themeMode,
                liquidGlassTheme = prefs[Keys.LiquidGlassTheme] ?: defaults.liquidGlassTheme,
                materialYou = prefs[Keys.MaterialYou] ?: defaults.materialYou,
                accentColor = validColor(prefs[Keys.AccentColor], defaults.accentColor),
                expandedTheme = prefs[Keys.ExpandedTheme]?.takeIf { it in SmartIslandSettings.VALID_EXPANDED_THEMES } ?: defaults.expandedTheme,
                islandFont = prefs[Keys.IslandFont]?.takeIf { SmartIslandSettings.isValidFont(it) } ?: defaults.islandFont,
                enableNotchMode = prefs[Keys.EnableNotchMode] ?: defaults.enableNotchMode,
                cameraCutoutManual = prefs[Keys.CameraCutoutManual] ?: defaults.cameraCutoutManual,
                cameraCutoutX = validDimension(prefs[Keys.CameraCutoutX], defaults.cameraCutoutX, SmartIslandSettings.MIN_X_OFFSET, SmartIslandSettings.MAX_X_OFFSET),
                cameraCutoutY = validDimension(prefs[Keys.CameraCutoutY], defaults.cameraCutoutY, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET),
                cameraCutoutWidth = validDimension(prefs[Keys.CameraCutoutWidth], defaults.cameraCutoutWidth, SmartIslandSettings.MIN_CUTOUT_WIDTH, SmartIslandSettings.MAX_CUTOUT_WIDTH),
                cameraCutoutHeight = validDimension(prefs[Keys.CameraCutoutHeight], defaults.cameraCutoutHeight, SmartIslandSettings.MIN_CUTOUT_HEIGHT, SmartIslandSettings.MAX_CUTOUT_HEIGHT),
                enableSwipeActions = prefs[Keys.EnableSwipeActions] ?: defaults.enableSwipeActions,
                swipeUpAction = prefs[Keys.SwipeUpAction] ?: defaults.swipeUpAction,
                swipeHoldUpAction = prefs[Keys.SwipeHoldUpAction] ?: defaults.swipeHoldUpAction,
                swipeDownAction = prefs[Keys.SwipeDownAction] ?: defaults.swipeDownAction,
                swipeDownCollapsedAction = prefs[Keys.SwipeDownCollapsedAction] ?: defaults.swipeDownCollapsedAction,
                swipeLeftAction = prefs[Keys.SwipeLeftAction] ?: defaults.swipeLeftAction,
                swipeRightAction = prefs[Keys.SwipeRightAction] ?: defaults.swipeRightAction,
                swipeHorizontalCollapsedAction = prefs[Keys.SwipeHorizontalCollapsedAction] ?: defaults.swipeHorizontalCollapsedAction,
                enablePillSwipeActions = prefs[Keys.EnablePillSwipeActions] ?: defaults.enablePillSwipeActions,
                pillSwipeUpAction = prefs[Keys.PillSwipeUpAction] ?: defaults.pillSwipeUpAction,
                pillSwipeDownAction = prefs[Keys.PillSwipeDownAction] ?: defaults.pillSwipeDownAction,
                pillSwipeLeftAction = prefs[Keys.PillSwipeLeftAction] ?: defaults.pillSwipeLeftAction,
                pillSwipeRightAction = prefs[Keys.PillSwipeRightAction] ?: defaults.pillSwipeRightAction,
                longPressAction = prefs[Keys.LongPressAction] ?: defaults.longPressAction,
                circlePosition = prefs[Keys.CirclePosition]?.takeIf { it in VALID_CIRCLE_POSITIONS } ?: defaults.circlePosition,
                landscapeShape = prefs[Keys.LandscapeShape]?.takeIf { it in VALID_LANDSCAPE_SHAPES } ?: defaults.landscapeShape,
                landscapeWidth = validDimension(prefs[Keys.LandscapeWidth], defaults.landscapeWidth, SmartIslandSettings.MIN_WIDTH, SmartIslandSettings.MAX_WIDTH),
                landscapeHeight = validDimension(prefs[Keys.LandscapeHeight], defaults.landscapeHeight, SmartIslandSettings.MIN_HEIGHT, SmartIslandSettings.MAX_HEIGHT),
                landscapeXOffset = validDimension(prefs[Keys.LandscapeXOffset], defaults.landscapeXOffset, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
                landscapeYOffset = validDimension(prefs[Keys.LandscapeYOffset], defaults.landscapeYOffset, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
                landscapeCornerRadius = validDimension(prefs[Keys.LandscapeCornerRadius], defaults.landscapeCornerRadius, SmartIslandSettings.MIN_CORNER_RADIUS, SmartIslandSettings.MAX_CORNER_RADIUS),
                landscapeOpacity = validDimension(prefs[Keys.LandscapeOpacity], defaults.landscapeOpacity, SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY),
                landscapeCircleSize = validDimension(prefs[Keys.LandscapeCircleSize], defaults.landscapeCircleSize, SmartIslandSettings.MIN_CIRCLE_SIZE, SmartIslandSettings.MAX_CIRCLE_SIZE),
                landscapeCircleOpacity = validDimension(prefs[Keys.LandscapeCircleOpacity], defaults.landscapeCircleOpacity, SmartIslandSettings.MIN_CIRCLE_OPACITY, SmartIslandSettings.MAX_OPACITY),
                landscapeCircleEdgeInset = validDimension(prefs[Keys.LandscapeCircleEdgeInset], defaults.landscapeCircleEdgeInset, SmartIslandSettings.MIN_CIRCLE_EDGE_INSET, SmartIslandSettings.MAX_CIRCLE_EDGE_INSET),
                landscapeCircleYOffset = validDimension(prefs[Keys.LandscapeCircleYOffset], defaults.landscapeCircleYOffset, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
                landscapeCirclePosition = prefs[Keys.LandscapeCirclePosition]?.takeIf { it in VALID_LANDSCAPE_POSITIONS } ?: defaults.landscapeCirclePosition,
                landscapePillPosition = prefs[Keys.LandscapePillPosition]?.takeIf { it in VALID_LANDSCAPE_POSITIONS } ?: defaults.landscapePillPosition,
                landscapeCircleXAdjust = validDimension(prefs[Keys.LandscapeCircleXAdjust], defaults.landscapeCircleXAdjust, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET),
                landscapeCircleYAdjust = validDimension(prefs[Keys.LandscapeCircleYAdjust], defaults.landscapeCircleYAdjust, -SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET),
                disabledEventsLandscape = prefs[Keys.DisabledEventsLandscape]
                    ?.filter { it.isNotBlank() && it.length <= 64 }
                    ?.toSet()
                    ?: defaults.disabledEventsLandscape,
                idleTimeoutMs = (prefs[Keys.IdleTimeoutMs] ?: defaults.idleTimeoutMs)
                    .coerceIn(SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS),
                batteryWarningMs = (prefs[Keys.BatteryWarningMs] ?: defaults.batteryWarningMs)
                    .coerceIn(SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS),
                popupMs = (prefs[Keys.PopupMs] ?: defaults.popupMs)
                    .coerceIn(SmartIslandSettings.MIN_POPUP_MS, SmartIslandSettings.MAX_POPUP_MS),
                transferExpiryMinutes = (prefs[Keys.TransferExpiryMinutes] ?: defaults.transferExpiryMinutes)
                    .coerceIn(0, SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES),
                idleWidgetsEnabled = prefs[Keys.IdleWidgetsEnabled] ?: defaults.idleWidgetsEnabled,
                // Installs from before the per-orientation switch start landscape from the one they had.
                idleWidgetsEnabledLandscape = prefs[Keys.IdleWidgetsEnabledLandscape]
                    ?: prefs[Keys.IdleWidgetsEnabled] ?: defaults.idleWidgetsEnabledLandscape,
                idleOffAfterTapClose = prefs[Keys.IdleOffAfterTapClose] ?: defaults.idleOffAfterTapClose,
                idleOffAfterTapCloseLandscape = prefs[Keys.IdleOffAfterTapCloseLandscape] ?: defaults.idleOffAfterTapCloseLandscape,
                idleOffAfterLongPress = prefs[Keys.IdleOffAfterLongPress] ?: defaults.idleOffAfterLongPress,
                idleOffAfterLongPressLandscape = prefs[Keys.IdleOffAfterLongPressLandscape] ?: defaults.idleOffAfterLongPressLandscape,
                idleOffWhenStored = prefs[Keys.IdleOffWhenStored] ?: defaults.idleOffWhenStored,
                contentPillSameAsIdle = prefs[Keys.ContentPillSameAsIdle] ?: defaults.contentPillSameAsIdle,
                islandExemptApps = prefs[Keys.IslandExemptApps]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }?.toSet() ?: defaults.islandExemptApps,
                islandExemptAppsLandscape = prefs[Keys.IslandExemptAppsLandscape]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }?.toSet() ?: defaults.islandExemptAppsLandscape,
                islandExemptAppsStatic = prefs[Keys.IslandExemptAppsStatic]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }?.toSet() ?: defaults.islandExemptAppsStatic,
                contentPillSameAsIdleLandscape = prefs[Keys.ContentPillSameAsIdleLandscape] ?: defaults.contentPillSameAsIdleLandscape,
                contentPillWidthPercentLandscape = (prefs[Keys.ContentPillWidthPercentLandscape] ?: defaults.contentPillWidthPercentLandscape).let {
                    if (it.isNaN()) defaults.contentPillWidthPercentLandscape
                    else it.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
                },
                contentPillWidthPercent = (prefs[Keys.ContentPillWidthPercent] ?: defaults.contentPillWidthPercent).let {
                    if (it.isNaN()) defaults.contentPillWidthPercent
                    else it.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
                },
                idleOffWhenStoredLandscape = prefs[Keys.IdleOffWhenStoredLandscape] ?: defaults.idleOffWhenStoredLandscape,
                idleGlyphTickSeconds = com.agupta07505.smartisland.model.IdleSlots.normalizeTickSeconds(
                    prefs[Keys.IdleGlyphTickSeconds] ?: defaults.idleGlyphTickSeconds
                ),
                idleWidgetIds = com.agupta07505.smartisland.model.IdleWidgets.normalize(
                    prefs[Keys.IdleWidgetIds] ?: defaults.idleWidgetIds
                ),
                idleWidgetCycleSeconds = (prefs[Keys.IdleWidgetCycleSeconds] ?: defaults.idleWidgetCycleSeconds)
                    .coerceIn(
                        com.agupta07505.smartisland.model.IdleWidgets.MIN_CYCLE_SECONDS,
                        com.agupta07505.smartisland.model.IdleWidgets.MAX_CYCLE_SECONDS
                    ),
                idleAlarmHideBeyond12h = prefs[Keys.IdleAlarmHideBeyond12h] ?: defaults.idleAlarmHideBeyond12h,
                widgetIslandCloseSeconds = com.agupta07505.smartisland.model.WidgetIslandTimer.normalize(
                    prefs[Keys.WidgetIslandCloseSeconds] ?: defaults.widgetIslandCloseSeconds
                ),
                widgetIslandBatteryMetric = (prefs[Keys.WidgetIslandBatteryMetric] ?: defaults.widgetIslandBatteryMetric)
                    .takeIf { it in com.agupta07505.smartisland.model.WidgetIslandContent.VALID_BATTERY_METRICS } ?: defaults.widgetIslandBatteryMetric,
                idleGlyphFont = prefs[Keys.IdleGlyphFont]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: defaults.idleGlyphFont,
                fontMixEnabled = prefs[Keys.FontMixEnabled] ?: defaults.fontMixEnabled,
                fontMixLight = prefs[Keys.FontMixLight]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: defaults.fontMixLight,
                fontMixRegular = prefs[Keys.FontMixRegular]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: defaults.fontMixRegular,
                fontMixMedium = prefs[Keys.FontMixMedium]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: defaults.fontMixMedium,
                idleGlyphScale = com.agupta07505.smartisland.model.IdleSlots.normalizeGlyphScale(
                    prefs[Keys.IdleGlyphScale] ?: defaults.idleGlyphScale
                ),
                idleWidgetTempUnit = (prefs[Keys.IdleWidgetTempUnit] ?: defaults.idleWidgetTempUnit)
                    .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_UNITS } ?: defaults.idleWidgetTempUnit,
                idleWidgetWindUnit = (prefs[Keys.IdleWidgetWindUnit] ?: defaults.idleWidgetWindUnit)
                    .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_WIND_UNITS } ?: defaults.idleWidgetWindUnit,
                idleTickStyle = com.agupta07505.smartisland.model.LedTick.normalizeStyle(prefs[Keys.IdleTickStyle] ?: defaults.idleTickStyle),
                ledTickPattern = com.agupta07505.smartisland.model.LedTick.normalizePattern(prefs[Keys.LedTickPattern] ?: defaults.ledTickPattern),
                ledTickPaceMs = com.agupta07505.smartisland.model.LedTick.normalizePace(prefs[Keys.LedTickPaceMs] ?: defaults.ledTickPaceMs),
                ledTickFont = prefs[Keys.LedTickFont]?.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: defaults.ledTickFont,
                ledTickColor = com.agupta07505.smartisland.model.LedTick.normalizeColor(prefs[Keys.LedTickColor] ?: defaults.ledTickColor),
                ledTickStagger = prefs[Keys.LedTickStagger] ?: defaults.ledTickStagger,
                secondaryMergeMs = (prefs[Keys.SecondaryMergeMs] ?: defaults.secondaryMergeMs)
                    .coerceIn(SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS),
                animSpeedPortrait = prefs[Keys.AnimSpeedPortrait]
                    ?.takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedPortrait,
                animSpeedLandscape = prefs[Keys.AnimSpeedLandscape]
                    ?.takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedLandscape,
                mergeStyle = prefs[Keys.MergeStyleId]
                    ?.takeIf { it in MergeStyle.VALID_IDS } ?: defaults.mergeStyle,
                // Installs from before the split have only the shared key: landscape keeps what they chose.
                mergeStyleLandscape = prefs[Keys.MergeStyleLandscapeId]
                    ?.takeIf { it in MergeStyle.VALID_IDS }
                    ?: prefs[Keys.MergeStyleId]?.takeIf { it in MergeStyle.VALID_IDS }
                    ?: defaults.mergeStyleLandscape,
                showDuringDnd = prefs[Keys.ShowDuringDnd] ?: defaults.showDuringDnd,
                hapticStrength = prefs[Keys.HapticStrength]
                    ?.takeIf { it in com.agupta07505.smartisland.util.HapticStrength.VALID_IDS } ?: defaults.hapticStrength,
                gestureSensitivity = prefs[Keys.GestureSensitivityId]
                    ?.takeIf { it in com.agupta07505.smartisland.util.GestureSensitivity.VALID_IDS } ?: defaults.gestureSensitivity,
                floatingWindowEnabled = prefs[Keys.FloatingWindowEnabled] ?: defaults.floatingWindowEnabled,
                floatingWindowMode = prefs[Keys.FloatingWindowMode]
                    ?.takeIf { it in com.agupta07505.smartisland.model.FloatingWindowMode.VALID_IDS } ?: defaults.floatingWindowMode,
                noExpandPackages = prefs[Keys.NoExpandPackages]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.noExpandPackages,
                noAutoExpandPackages = prefs[Keys.NoAutoExpandPackages]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.noAutoExpandPackages,
                // An app in both sets is off: the explicit "off" wins.
                autoExpandPackages = (prefs[Keys.AutoExpandPackages]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.autoExpandPackages) -
                    (prefs[Keys.NoAutoExpandPackages] ?: emptySet()),
                noAutoExpandEvents = prefs[Keys.NoAutoExpandEvents]
                    ?.filter { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id } }
                    ?.toSet()
                    ?: defaults.noAutoExpandEvents,
                hideAllOnLockScreen = prefs[Keys.HideAllOnLockScreen] ?: defaults.hideAllOnLockScreen,
                hideOnLockScreenEvents = prefs[Keys.HideOnLockScreenEvents]
                    ?.filter { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id } }
                    ?.toSet()
                    ?: defaults.hideOnLockScreenEvents,
                notificationsEnabled = prefs[Keys.NotificationsEnabled] ?: defaults.notificationsEnabled,
                hideOnLockScreenPackages = prefs[Keys.HideOnLockScreenPackages]
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.toSet()
                    ?: defaults.hideOnLockScreenPackages
            )
        }

    suspend fun setDeviceType(value: String) = editSafely { it[Keys.DeviceType] = value }
    suspend fun setEnabled(value: Boolean) = editSafely { it[Keys.Enabled] = value }
    suspend fun setEnableShadow(value: Boolean) = editSafely { it[Keys.EnableShadow] = value }
    suspend fun setExpandedBlurEnabled(value: Boolean) = editSafely { it[Keys.ExpandedBlurEnabled] = value }
    /**
     * Picks Off or Card blur.
     * The media card inherits the shared blur until the owner sets its own, so those two are pinned to what the
     * media card shows now first; changing this choice then never changes the media card.
     */
    suspend fun setExpandedBlurMode(mode: ExpandedBlurMode) = editSafely {
        if (it[Keys.MediaBlurEnabled] == null) {
            it[Keys.MediaBlurEnabled] = it[Keys.ExpandedBlurEnabled] ?: SmartIslandSettings.Default.mediaBlurEnabled
        }
        if (it[Keys.MediaBlurRadius] == null) {
            it[Keys.MediaBlurRadius] = (it[Keys.ExpandedBlurRadius] ?: SmartIslandSettings.Default.mediaBlurRadius)
                .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
        }
        it[Keys.ExpandedBlurEnabled] = mode == ExpandedBlurMode.Card
    }
    suspend fun setMediaBlurEnabled(value: Boolean) = editSafely { it[Keys.MediaBlurEnabled] = value }
    suspend fun setMediaBlurRadius(value: Float) = editSafely {
        it[Keys.MediaBlurRadius] = value.coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    }
    suspend fun setMediaOpacity(value: Float) = editSafely {
        it[Keys.MediaOpacity] = validDimension(
            value,
            SmartIslandSettings.Default.mediaOpacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        )
    }
    suspend fun setExpandedBlurRadius(value: Float) = editSafely {
        it[Keys.ExpandedBlurRadius] = value.coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
    }
    suspend fun setExpandedGradientEnabled(value: Boolean) = editSafely { it[Keys.ExpandedGradientEnabled] = value }
    suspend fun setExpandedGradientColor(value: Long) = editSafely {
        it[Keys.ExpandedGradientColor] = validColor(value, SmartIslandSettings.Default.expandedGradientColor)
    }
    suspend fun setExpandedGradientStrength(value: Float) = editSafely {
        it[Keys.ExpandedGradientStrength] = value.coerceIn(SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH)
    }
    suspend fun setShadowElevation(value: Float) = editSafely {
        it[Keys.ShadowElevation] = validDimension(
            value,
            SmartIslandSettings.Default.shadowElevation,
            SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION
        )
    }
    suspend fun setWidth(value: Float) = editSafely {
        it[Keys.Width] = validDimension(
            value,
            SmartIslandSettings.Default.width,
            SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH
        )
    }
    suspend fun setHeight(value: Float) = editSafely {
        it[Keys.Height] = validDimension(
            value,
            SmartIslandSettings.Default.height,
            SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT
        )
    }
    suspend fun setXOffset(value: Float) = editSafely {
        it[Keys.XOffset] = validDimension(
            value,
            SmartIslandSettings.Default.xOffset,
            SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET
        )
    }
    suspend fun setYOffset(value: Float) = editSafely {
        it[Keys.YOffset] = validDimension(
            value,
            SmartIslandSettings.Default.yOffset,
            SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET
        )
    }
    suspend fun setPosition(
        width: Float,
        height: Float,
        xOffset: Float,
        yOffset: Float
    ) = editSafely {
        it[Keys.Width] = validDimension(width, SmartIslandSettings.Default.width, SmartIslandSettings.MIN_WIDTH, SmartIslandSettings.MAX_WIDTH)
        it[Keys.Height] = validDimension(height, SmartIslandSettings.Default.height, SmartIslandSettings.MIN_HEIGHT, SmartIslandSettings.MAX_HEIGHT)
        it[Keys.XOffset] = validDimension(xOffset, SmartIslandSettings.Default.xOffset, SmartIslandSettings.MIN_X_OFFSET, SmartIslandSettings.MAX_X_OFFSET)
        it[Keys.YOffset] = validDimension(yOffset, SmartIslandSettings.Default.yOffset, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET)
    }
    suspend fun setCornerRadius(value: Float) = editSafely {
        it[Keys.CornerRadius] = validDimension(
            value,
            SmartIslandSettings.Default.cornerRadius,
            SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS
        )
    }
    suspend fun setOpacity(value: Float) = editSafely {
        it[Keys.Opacity] = validDimension(
            value,
            SmartIslandSettings.Default.opacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        )
    }
    suspend fun setPillColor(value: Long) = editSafely {
        it[Keys.PillColor] = validColor(value, SmartIslandSettings.Default.pillColor)
    }
    suspend fun setBatteryColor(value: Long) = editSafely {
        it[Keys.BatteryColor] = validColor(value, SmartIslandSettings.Default.batteryColor)
    }
    suspend fun setNotificationDotColor(value: Long) = editSafely {
        it[Keys.NotificationDotColor] = validColor(
            value,
            SmartIslandSettings.Default.notificationDotColor
        )
    }
    suspend fun setMusicVisualizerColor(value: Long) = editSafely {
        it[Keys.MusicVisualizerColor] = validColor(
            value,
            SmartIslandSettings.Default.musicVisualizerColor
        )
    }
    suspend fun setHotspotColor(value: Long) = editSafely {
        it[Keys.HotspotColor] = validColor(value, SmartIslandSettings.Default.hotspotColor)
    }
    suspend fun setCallColor(value: Long) = editSafely {
        it[Keys.CallColor] = validColor(value, SmartIslandSettings.Default.callColor)
    }
    suspend fun setLiveActivityColor(value: Long) = editSafely {
        it[Keys.LiveActivityColor] = validColor(value, SmartIslandSettings.Default.liveActivityColor)
    }
    suspend fun setTransferColor(value: Long) = editSafely {
        it[Keys.TransferColor] = validColor(value, SmartIslandSettings.Default.transferColor)
    }
    suspend fun setNavigationColor(value: Long) = editSafely {
        it[Keys.NavigationColor] = validColor(value, SmartIslandSettings.Default.navigationColor)
    }
    suspend fun setBluetoothColor(value: Long) = editSafely {
        it[Keys.BluetoothColor] = validColor(value, SmartIslandSettings.Default.bluetoothColor)
    }
    suspend fun setFlashlightColor(value: Long) = editSafely {
        it[Keys.FlashlightColor] = validColor(value, SmartIslandSettings.Default.flashlightColor)
    }
    suspend fun setScreenRecordingColor(value: Long) = editSafely {
        it[Keys.ScreenRecordingColor] = validColor(value, SmartIslandSettings.Default.screenRecordingColor)
    }
    suspend fun setTimerColor(value: Long) = editSafely {
        it[Keys.TimerColor] = validColor(value, SmartIslandSettings.Default.timerColor)
    }
    suspend fun setStopwatchColor(value: Long) = editSafely {
        it[Keys.StopwatchColor] = validColor(value, SmartIslandSettings.Default.stopwatchColor)
    }
    suspend fun setEnableAppShortcuts(value: Boolean) = editSafely {
        it[Keys.EnableAppShortcuts] = value
    }
    suspend fun setShortcutPackages(value: List<String>) = editSafely {
        writePinnedApps(it, value)
    }
    suspend fun setShowRecentApps(value: Boolean) = editSafely {
        it[Keys.ShowRecentApps] = value
    }
    suspend fun setUtilityLayout(value: List<com.agupta07505.smartisland.model.UtilityPlacement>) = editSafely {
        it[Keys.UtilityLayout] = com.agupta07505.smartisland.model.UtilityLayout.encode(value)
    }
    suspend fun setUtilityCalculatorPackage(value: String) = editSafely {
        it[Keys.UtilityCalculatorPackage] = value.take(255)
    }
    suspend fun setUtilityCalculatorFloating(value: Boolean) = editSafely {
        it[Keys.UtilityCalculatorFloating] = value
    }
    suspend fun setWelcomeDialogShown(value: Boolean) = editSafely {
        it[Keys.WelcomeDialogShown] = value
    }

    /**
     * Reads the REAL persisted value (awaits the first on-disk emission) instead of
     * relying on the SmartIslandSettings.Default placeholder that State flows emit on the
     * first frame. Used to decide whether the welcome dialog should show.
     */
    suspend fun isWelcomeDialogShown(): Boolean = settings.first().welcomeDialogShown

    suspend fun setShowOnLockScreen(value: Boolean) = editSafely {
        it[Keys.ShowOnLockScreen] = value
    }
    suspend fun setShowLiveEventsInForeground(value: Boolean) = editSafely {
        it[Keys.ShowLiveEventsInForeground] = value
    }
    suspend fun setLockScreenPrivacy(value: String) = editSafely {
        it[Keys.LockScreenPrivacy] = value.takeIf { privacy ->
            privacy in VALID_LOCK_SCREEN_PRIVACY_VALUES
        } ?: SmartIslandSettings.Default.lockScreenPrivacy
    }
    suspend fun setShowNotificationActions(value: Boolean) = editSafely {
        it[Keys.ShowNotificationActions] = value
    }
    suspend fun setHideFromNotificationShade(value: Boolean) = editSafely {
        it[Keys.HideFromNotificationShade] = value
    }
    suspend fun setDisabledNotificationPackages(value: Set<String>) = editSafely {
        it[Keys.DisabledNotificationPackages] = value
            .asSequence()
            .filter { pkg -> pkg.isNotBlank() && pkg.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
    }
    suspend fun setEventEnabled(eventId: String, enabled: Boolean) = editSafely {
        val current = it[Keys.DisabledEvents] ?: emptySet()
        it[Keys.DisabledEvents] = if (enabled) current - eventId else current + eventId
    }

    suspend fun setDisabledSoundPackages(value: Set<String>) = editSafely {
        it[Keys.DisabledSoundPackages] = value
            .asSequence()
            .filter { pkg -> pkg.isNotBlank() && pkg.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
    }
    suspend fun setHideWhenIdle(value: Boolean) = editSafely {
        it[Keys.HideWhenIdle] = value
    }
    suspend fun setAutoHidePill(value: Boolean) = editSafely {
        it[Keys.AutoHidePill] = value
    }
    suspend fun setAutoHideTimeoutSeconds(value: Int) = editSafely {
        it[Keys.AutoHideTimeoutSeconds] = value.coerceIn(1, 120)
    }
    suspend fun setShowInLandscape(value: Boolean) = editSafely {
        it[Keys.ShowInLandscape] = value
    }
    suspend fun setAutoExpandOnNotification(value: Boolean) = editSafely {
        it[Keys.AutoExpandOnNotification] = value
    }
    suspend fun setAutoExpandOnLockScreen(value: Boolean) = editSafely {
        it[Keys.AutoExpandOnLockScreen] = value
    }
    suspend fun setIdleTimeoutMs(value: Int) = editSafely {
        it[Keys.IdleTimeoutMs] = value.coerceIn(SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
    }
    suspend fun setBatteryWarningMs(value: Int) = editSafely {
        it[Keys.BatteryWarningMs] = value.coerceIn(SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS)
    }
    suspend fun setPopupMs(value: Int) = editSafely {
        it[Keys.PopupMs] = value.coerceIn(SmartIslandSettings.MIN_POPUP_MS, SmartIslandSettings.MAX_POPUP_MS)
    }
    suspend fun setTransferExpiryMinutes(value: Int) = editSafely {
        it[Keys.TransferExpiryMinutes] = value.coerceIn(0, SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES)
    }
    suspend fun setSecondaryMergeMs(value: Int) = editSafely {
        it[Keys.SecondaryMergeMs] = value.coerceIn(SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS)
    }
    suspend fun setAnimSpeedPortrait(value: String) = editSafely {
        it[Keys.AnimSpeedPortrait] = if (value in AnimationSpeed.VALID_IDS) value else SmartIslandSettings.Default.animSpeedPortrait
    }
    suspend fun setAnimSpeedLandscape(value: String) = editSafely {
        it[Keys.AnimSpeedLandscape] = if (value in AnimationSpeed.VALID_IDS) value else SmartIslandSettings.Default.animSpeedLandscape
    }
    suspend fun setMergeStyle(value: String) = editSafely {
        it[Keys.MergeStyleId] = if (value in MergeStyle.VALID_IDS) value else SmartIslandSettings.Default.mergeStyle
    }
    suspend fun setMergeStyleLandscape(value: String) = editSafely {
        it[Keys.MergeStyleLandscapeId] = if (value in MergeStyle.VALID_IDS) value else SmartIslandSettings.Default.mergeStyleLandscape
    }
    suspend fun setShowDuringDnd(value: Boolean) = editSafely {
        it[Keys.ShowDuringDnd] = value
    }
    suspend fun setHapticStrength(value: String) = editSafely {
        it[Keys.HapticStrength] = if (value in com.agupta07505.smartisland.util.HapticStrength.VALID_IDS) value
        else SmartIslandSettings.Default.hapticStrength
    }
    suspend fun setFloatingWindowEnabled(value: Boolean) = editSafely {
        it[Keys.FloatingWindowEnabled] = value
    }
    suspend fun setFloatingWindowMode(value: String) = editSafely {
        it[Keys.FloatingWindowMode] = if (value in com.agupta07505.smartisland.model.FloatingWindowMode.VALID_IDS) value
        else SmartIslandSettings.Default.floatingWindowMode
    }
    suspend fun setGestureSensitivity(value: String) = editSafely {
        it[Keys.GestureSensitivityId] = if (value in com.agupta07505.smartisland.util.GestureSensitivity.VALID_IDS) value
        else SmartIslandSettings.Default.gestureSensitivity
    }
    suspend fun setNoExpandPackages(value: Set<String>) = editSafely {
        it[Keys.NoExpandPackages] = value
            .asSequence()
            .filter { pkg -> pkg.isNotBlank() && pkg.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
    }
    /**
     * Sets one app's auto-expand choice in a single write, so the two sets never disagree.
     * DEFAULT: in neither set (loud notifications open the card, silent ones do not). ALWAYS: in the allow set.
     * NEVER: in the deny set (beats the loud exception).
     */
    suspend fun setAppAutoExpand(packageName: String, choice: com.agupta07505.smartisland.util.AppAutoExpand) = editSafely {
        if (packageName.isBlank() || packageName.length > MAX_PACKAGE_NAME_LENGTH) return@editSafely
        val allow = (it[Keys.AutoExpandPackages] ?: emptySet()) - packageName
        val deny = (it[Keys.NoAutoExpandPackages] ?: emptySet()) - packageName
        when (choice) {
            com.agupta07505.smartisland.util.AppAutoExpand.DEFAULT -> {
                it[Keys.AutoExpandPackages] = allow
                it[Keys.NoAutoExpandPackages] = deny
            }
            com.agupta07505.smartisland.util.AppAutoExpand.ALWAYS -> {
                it[Keys.AutoExpandPackages] = allow + packageName
                it[Keys.NoAutoExpandPackages] = deny
            }
            com.agupta07505.smartisland.util.AppAutoExpand.NEVER -> {
                it[Keys.AutoExpandPackages] = allow
                it[Keys.NoAutoExpandPackages] = deny + packageName
            }
        }
    }

    /** One system event's own auto-expand switch (off = the event only shows in the pill). */
    suspend fun setEventAutoExpand(eventId: String, enabled: Boolean) = editSafely {
        if (com.agupta07505.smartisland.model.IslandEvent.entries.none { e -> e.id == eventId }) return@editSafely
        val current = it[Keys.NoAutoExpandEvents] ?: emptySet()
        it[Keys.NoAutoExpandEvents] = if (enabled) current - eventId else current + eventId
    }

    /** One switch that keeps every event (calls excepted) out of the pill while the phone is locked. */
    suspend fun setHideAllOnLockScreen(value: Boolean) = editSafely { it[Keys.HideAllOnLockScreen] = value }

    /** One event's own lock-screen switch (true = the event is hidden in the pill while locked). */
    suspend fun setEventHiddenOnLockScreen(eventId: String, hidden: Boolean) = editSafely {
        if (com.agupta07505.smartisland.model.IslandEvent.entries.none { e -> e.id == eventId && e.canHideOnLockScreen }) {
            return@editSafely
        }
        val current = it[Keys.HideOnLockScreenEvents] ?: emptySet()
        it[Keys.HideOnLockScreenEvents] = if (hidden) current + eventId else current - eventId
    }

    /** Sets the lock-screen switches together from one preset, in one write. */
    suspend fun applyLockScreenPreset(preset: com.agupta07505.smartisland.model.LockScreenPreset) = editSafely {
        when (preset) {
            com.agupta07505.smartisland.model.LockScreenPreset.ShowAll -> {
                it[Keys.ShowOnLockScreen] = true
                it[Keys.HideAllOnLockScreen] = false
                it[Keys.HideOnLockScreenEvents] = emptySet()
            }
            com.agupta07505.smartisland.model.LockScreenPreset.MessagesOnly -> {
                it[Keys.ShowOnLockScreen] = true
                it[Keys.HideAllOnLockScreen] = false
                it[Keys.HideOnLockScreenEvents] = com.agupta07505.smartisland.model.IslandEvent.entries
                    .filter { e -> e.canHideOnLockScreen && e != com.agupta07505.smartisland.model.IslandEvent.NOTIFICATION }
                    .map { e -> e.id }
                    .toSet()
            }
            com.agupta07505.smartisland.model.LockScreenPreset.CallsOnly -> {
                it[Keys.ShowOnLockScreen] = true
                it[Keys.HideAllOnLockScreen] = true
                it[Keys.HideOnLockScreenEvents] = emptySet()
            }
            com.agupta07505.smartisland.model.LockScreenPreset.Off -> it[Keys.ShowOnLockScreen] = false
        }
    }

    /** One app's lock-screen rule (true = its cards stay out of the pill while the phone is locked). */
    suspend fun setAppHiddenOnLockScreen(packageName: String, hidden: Boolean) = editSafely {
        if (packageName.isBlank() || packageName.length > MAX_PACKAGE_NAME_LENGTH) return@editSafely
        val current = it[Keys.HideOnLockScreenPackages] ?: emptySet()
        it[Keys.HideOnLockScreenPackages] = if (hidden) current + packageName else current - packageName
    }

    /** The universal switch for app notifications (off = no notification cards, the listener ignores them). */
    suspend fun setNotificationsEnabled(value: Boolean) = editSafely { it[Keys.NotificationsEnabled] = value }

    /** Puts the four timers back to their built-in values in one write. */
    suspend fun resetTimings() = editSafely {
        val d = SmartIslandSettings.Default
        it[Keys.IdleTimeoutMs] = d.idleTimeoutMs
        it[Keys.BatteryWarningMs] = d.batteryWarningMs
        it[Keys.PopupMs] = d.popupMs
        it[Keys.SecondaryMergeMs] = d.secondaryMergeMs
        it[Keys.TransferExpiryMinutes] = d.transferExpiryMinutes
    }
    suspend fun setIdleWidgetsEnabled(value: Boolean) = editSafely { it[Keys.IdleWidgetsEnabled] = value }
    suspend fun setIdleWidgetsEnabledLandscape(value: Boolean) = editSafely { it[Keys.IdleWidgetsEnabledLandscape] = value }
    suspend fun setIdleOffAfterTapClose(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) Keys.IdleOffAfterTapCloseLandscape else Keys.IdleOffAfterTapClose] = value
    }
    /** One app's exemption (true = the island is totally off while that app is in the foreground), for one orientation. */
    suspend fun setIslandExemptApp(landscape: Boolean, packageName: String, exempt: Boolean) = editSafely {
        if (packageName.isBlank() || packageName.length > MAX_PACKAGE_NAME_LENGTH) return@editSafely
        val key = if (landscape) Keys.IslandExemptAppsLandscape else Keys.IslandExemptApps
        val current = it[key] ?: emptySet()
        it[key] = if (exempt) current + packageName else current - packageName
        // Portrait: an app is either "off" or "static", never both. Adding it (as off) or removing it clears the static entry.
        if (!landscape) it[Keys.IslandExemptAppsStatic] = (it[Keys.IslandExemptAppsStatic] ?: emptySet()) - packageName
    }

    /**
     * Portrait only: switches an exempt app between "island off" (static = false) and "static pill" (static = true).
     * The app ends up in exactly one of the two lists.
     */
    suspend fun setIslandExemptStatic(packageName: String, static: Boolean) = editSafely {
        if (packageName.isBlank() || packageName.length > MAX_PACKAGE_NAME_LENGTH) return@editSafely
        val off = it[Keys.IslandExemptApps] ?: emptySet()
        val stat = it[Keys.IslandExemptAppsStatic] ?: emptySet()
        if (static) {
            it[Keys.IslandExemptApps] = off - packageName
            it[Keys.IslandExemptAppsStatic] = stat + packageName
        } else {
            it[Keys.IslandExemptAppsStatic] = stat - packageName
            it[Keys.IslandExemptApps] = off + packageName
        }
    }
    suspend fun setContentPillSameAsIdle(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) Keys.ContentPillSameAsIdleLandscape else Keys.ContentPillSameAsIdle] = value
    }
    suspend fun setContentPillWidthPercent(landscape: Boolean, value: Float) = editSafely {
        it[if (landscape) Keys.ContentPillWidthPercentLandscape else Keys.ContentPillWidthPercent] = if (value.isNaN()) SmartIslandSettings.DEFAULT_CONTENT_PILL_PERCENT
        else value.coerceIn(SmartIslandSettings.MIN_CONTENT_PILL_PERCENT, SmartIslandSettings.MAX_CONTENT_PILL_PERCENT)
    }
    suspend fun setIdleOffWhenStored(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) Keys.IdleOffWhenStoredLandscape else Keys.IdleOffWhenStored] = value
    }
    suspend fun setIdleOffAfterLongPress(landscape: Boolean, value: Boolean) = editSafely {
        it[if (landscape) Keys.IdleOffAfterLongPressLandscape else Keys.IdleOffAfterLongPress] = value
    }
    suspend fun setIdleGlyphTickSeconds(value: Float) = editSafely {
        it[Keys.IdleGlyphTickSeconds] = com.agupta07505.smartisland.model.IdleSlots.normalizeTickSeconds(value)
    }
    suspend fun setIdleWidgetIds(value: String) = editSafely {
        it[Keys.IdleWidgetIds] = com.agupta07505.smartisland.model.IdleWidgets.normalize(value)
    }
    suspend fun setIdleWidgetCycleSeconds(value: Int) = editSafely {
        it[Keys.IdleWidgetCycleSeconds] = value.coerceIn(
            com.agupta07505.smartisland.model.IdleWidgets.MIN_CYCLE_SECONDS,
            com.agupta07505.smartisland.model.IdleWidgets.MAX_CYCLE_SECONDS
        )
    }
    suspend fun setIdleAlarmHideBeyond12h(value: Boolean) = editSafely { it[Keys.IdleAlarmHideBeyond12h] = value }
    suspend fun setWidgetIslandCloseSeconds(value: Int) = editSafely {
        it[Keys.WidgetIslandCloseSeconds] = com.agupta07505.smartisland.model.WidgetIslandTimer.normalize(value)
    }
    suspend fun setWidgetIslandBatteryMetric(value: String) = editSafely {
        it[Keys.WidgetIslandBatteryMetric] = value.takeIf { v -> v in com.agupta07505.smartisland.model.WidgetIslandContent.VALID_BATTERY_METRICS } ?: com.agupta07505.smartisland.model.WidgetIslandContent.BATTERY_WATT
    }
    /** Mix fonts: the switch and the three slot fonts in one write, so the island never sees a half-changed mix. */
    suspend fun setFontMix(enabled: Boolean, light: String, regular: String, medium: String) = editSafely {
        fun valid(id: String) = id.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
        it[Keys.FontMixEnabled] = enabled
        it[Keys.FontMixLight] = valid(light)
        it[Keys.FontMixRegular] = valid(regular)
        it[Keys.FontMixMedium] = valid(medium)
    }
    suspend fun setIdleGlyphFont(value: String) = editSafely {
        it[Keys.IdleGlyphFont] = value.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
    }
    suspend fun setIdleGlyphScale(value: Int) = editSafely {
        it[Keys.IdleGlyphScale] = com.agupta07505.smartisland.model.IdleSlots.normalizeGlyphScale(value)
    }
    suspend fun setIdleTickStyle(value: String) = editSafely {
        it[Keys.IdleTickStyle] = com.agupta07505.smartisland.model.LedTick.normalizeStyle(value)
    }
    suspend fun setLedTickPattern(value: String) = editSafely {
        it[Keys.LedTickPattern] = com.agupta07505.smartisland.model.LedTick.normalizePattern(value)
    }
    suspend fun setLedTickPaceMs(value: Int) = editSafely {
        it[Keys.LedTickPaceMs] = com.agupta07505.smartisland.model.LedTick.normalizePace(value)
    }
    suspend fun setLedTickFont(value: String) = editSafely {
        it[Keys.LedTickFont] = value.takeIf { v -> v.isEmpty() || SmartIslandSettings.isValidFont(v) } ?: ""
    }
    suspend fun setLedTickColor(value: String) = editSafely {
        it[Keys.LedTickColor] = com.agupta07505.smartisland.model.LedTick.normalizeColor(value)
    }
    suspend fun setLedTickStagger(enabled: Boolean) = editSafely {
        it[Keys.LedTickStagger] = enabled
    }
    suspend fun setIdleWidgetTempUnit(value: String) = editSafely {
        it[Keys.IdleWidgetTempUnit] = value.takeIf { v -> v in com.agupta07505.smartisland.model.IdleWidgets.VALID_UNITS }
            ?: com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO
    }
    suspend fun setIdleWidgetWindUnit(value: String) = editSafely {
        it[Keys.IdleWidgetWindUnit] = value.takeIf { v -> v in com.agupta07505.smartisland.model.IdleWidgets.VALID_WIND_UNITS }
            ?: com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO
    }
    suspend fun setAllowNetworkChecks(value: Boolean) = editSafely {
        it[Keys.AllowNetworkChecks] = value
    }
    suspend fun setDetectTransfersBySpeed(value: Boolean) = editSafely {
        it[Keys.DetectTransfersBySpeed] = value
    }
    suspend fun setEnableNotificationHistory(value: Boolean) = editSafely {
        it[Keys.EnableNotificationHistory] = value
    }
    suspend fun setNotificationHistoryRetentionHours(value: Int) = editSafely {
        it[Keys.NotificationHistoryRetentionHours] = value
    }
    suspend fun setShowBluetoothBattery(value: Boolean) = editSafely {
        it[Keys.ShowBluetoothBattery] = value
    }
    suspend fun setEnableNotificationCooldown(value: Boolean) = editSafely {
        it[Keys.EnableNotificationCooldown] = value
    }
    suspend fun setNotificationCooldownDurationMinutes(value: Int) = editSafely {
        it[Keys.NotificationCooldownDurationMinutes] = value.coerceIn(1, 60)
    }
    suspend fun setNotificationCooldownThreshold(value: Int) = editSafely {
        it[Keys.NotificationCooldownThreshold] = value.coerceIn(2, 20)
    }
    suspend fun setNotificationCooldownExcludedPackages(packages: Set<String>) = editSafely {
        it[Keys.NotificationCooldownExcludedPackages] = packages
    }
    suspend fun setThemeMode(value: String) = editSafely {
        it[Keys.ThemeMode] = value
    }
    suspend fun setLiquidGlassTheme(value: Boolean) = editSafely { it[Keys.LiquidGlassTheme] = value }
    suspend fun setMaterialYou(value: Boolean) = editSafely { it[Keys.MaterialYou] = value }
    suspend fun setAccentColor(value: Long) = editSafely {
        it[Keys.AccentColor] = validColor(value, SmartIslandSettings.Default.accentColor)
    }
    suspend fun setExpandedTheme(value: String) = editSafely {
        it[Keys.ExpandedTheme] = if (value in SmartIslandSettings.VALID_EXPANDED_THEMES) value else SmartIslandSettings.Default.expandedTheme
    }
    suspend fun setIslandFont(value: String) = editSafely {
        it[Keys.IslandFont] = if (SmartIslandSettings.isValidFont(value)) value else SmartIslandSettings.Default.islandFont
    }
    suspend fun setRecordLogs(value: Boolean) = editSafely {
        it[Keys.RecordLogs] = value
    }
    suspend fun setEnableNotchMode(value: Boolean) = editSafely {
        it[Keys.EnableNotchMode] = value
    }
    suspend fun setCameraCutoutManual(value: Boolean) = editSafely {
        it[Keys.CameraCutoutManual] = value
    }
    /** Saves the manual camera cutout area in one write (centre X from the screen centre, top Y, width, height; all dp). */
    suspend fun setCameraCutoutArea(x: Float, y: Float, width: Float, height: Float) = editSafely {
        val d = SmartIslandSettings.Default
        it[Keys.CameraCutoutX] = validDimension(x, d.cameraCutoutX, SmartIslandSettings.MIN_X_OFFSET, SmartIslandSettings.MAX_X_OFFSET)
        it[Keys.CameraCutoutY] = validDimension(y, d.cameraCutoutY, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET)
        it[Keys.CameraCutoutWidth] = validDimension(width, d.cameraCutoutWidth, SmartIslandSettings.MIN_CUTOUT_WIDTH, SmartIslandSettings.MAX_CUTOUT_WIDTH)
        it[Keys.CameraCutoutHeight] = validDimension(height, d.cameraCutoutHeight, SmartIslandSettings.MIN_CUTOUT_HEIGHT, SmartIslandSettings.MAX_CUTOUT_HEIGHT)
    }
    suspend fun setEnableSwipeActions(value: Boolean) = editSafely {
        it[Keys.EnableSwipeActions] = value
    }
    suspend fun setSwipeUpAction(value: String) = editSafely {
        it[Keys.SwipeUpAction] = value
    }
    suspend fun setSwipeHoldUpAction(value: String) = editSafely {
        it[Keys.SwipeHoldUpAction] = value
    }
    suspend fun setSwipeDownAction(value: String) = editSafely {
        it[Keys.SwipeDownAction] = value
    }
    suspend fun setSwipeLeftAction(value: String) = editSafely {
        it[Keys.SwipeLeftAction] = value
    }
    suspend fun setSwipeRightAction(value: String) = editSafely {
        it[Keys.SwipeRightAction] = value
    }
    suspend fun setEnablePillSwipeActions(value: Boolean) = editSafely {
        it[Keys.EnablePillSwipeActions] = value
    }
    suspend fun setPillSwipeUpAction(value: String) = editSafely {
        it[Keys.PillSwipeUpAction] = value
    }
    suspend fun setLongPressAction(value: String) = editSafely {
        it[Keys.LongPressAction] = value
    }
    suspend fun setPillSwipeDownAction(value: String) = editSafely {
        it[Keys.PillSwipeDownAction] = value
    }
    suspend fun setPillSwipeLeftAction(value: String) = editSafely {
        it[Keys.PillSwipeLeftAction] = value
    }
    suspend fun setPillSwipeRightAction(value: String) = editSafely {
        it[Keys.PillSwipeRightAction] = value
    }
    suspend fun setLandscapeEventEnabled(eventId: String, enabled: Boolean) = editSafely {
        val current = it[Keys.DisabledEventsLandscape] ?: emptySet()
        it[Keys.DisabledEventsLandscape] = if (enabled) current - eventId else current + eventId
    }
    suspend fun setLandscapeCirclePosition(value: String) = editSafely {
        it[Keys.LandscapeCirclePosition] = if (value in VALID_LANDSCAPE_POSITIONS) value else SmartIslandSettings.Default.landscapeCirclePosition
    }
    suspend fun setLandscapePillPosition(value: String) = editSafely {
        it[Keys.LandscapePillPosition] = if (value in VALID_LANDSCAPE_POSITIONS) value else SmartIslandSettings.Default.landscapePillPosition
    }
    suspend fun setLandscapePillGeometry(
        width: Float,
        height: Float,
        xOffset: Float,
        yOffset: Float
    ) = editSafely {
        val d = SmartIslandSettings.Default
        it[Keys.LandscapeWidth] = validDimension(width, d.landscapeWidth, SmartIslandSettings.MIN_WIDTH, SmartIslandSettings.MAX_WIDTH)
        it[Keys.LandscapeHeight] = validDimension(height, d.landscapeHeight, SmartIslandSettings.MIN_HEIGHT, SmartIslandSettings.MAX_HEIGHT)
        it[Keys.LandscapeXOffset] = validDimension(xOffset, d.landscapeXOffset, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET)
        it[Keys.LandscapeYOffset] = validDimension(yOffset, d.landscapeYOffset, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    }
    suspend fun setLandscapeOpacity(value: Float) = editSafely {
        it[Keys.LandscapeOpacity] = validDimension(value, SmartIslandSettings.Default.landscapeOpacity, SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY)
    }
    suspend fun setLandscapeCircleSize(value: Float) = editSafely {
        it[Keys.LandscapeCircleSize] = validDimension(value, SmartIslandSettings.Default.landscapeCircleSize, SmartIslandSettings.MIN_CIRCLE_SIZE, SmartIslandSettings.MAX_CIRCLE_SIZE)
    }
    suspend fun setLandscapeCircleOpacity(value: Float) = editSafely {
        it[Keys.LandscapeCircleOpacity] = validDimension(value, SmartIslandSettings.Default.landscapeCircleOpacity, SmartIslandSettings.MIN_CIRCLE_OPACITY, SmartIslandSettings.MAX_OPACITY)
    }
    suspend fun setLandscapeCircleEdgeInset(value: Float) = editSafely {
        it[Keys.LandscapeCircleEdgeInset] = validDimension(value, SmartIslandSettings.Default.landscapeCircleEdgeInset, SmartIslandSettings.MIN_CIRCLE_EDGE_INSET, SmartIslandSettings.MAX_CIRCLE_EDGE_INSET)
    }
    suspend fun setLandscapeCircleYOffset(value: Float) = editSafely {
        it[Keys.LandscapeCircleYOffset] = validDimension(value, SmartIslandSettings.Default.landscapeCircleYOffset, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    }
    suspend fun setLandscapeCircleXAdjust(value: Float) = editSafely {
        it[Keys.LandscapeCircleXAdjust] = validDimension(value, SmartIslandSettings.Default.landscapeCircleXAdjust, -SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_X_OFFSET)
    }
    suspend fun setLandscapeCircleYAdjust(value: Float) = editSafely {
        it[Keys.LandscapeCircleYAdjust] = validDimension(value, SmartIslandSettings.Default.landscapeCircleYAdjust, -SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
    }
    suspend fun setCirclePosition(value: String) = editSafely {
        it[Keys.CirclePosition] = if (value in VALID_CIRCLE_POSITIONS) value else SmartIslandSettings.CIRCLE_POSITION_RIGHT
    }
    suspend fun setNotificationCooldownSystemEvents(eventIds: Set<String>) = editSafely {
        it[Keys.NotificationCooldownSystemEvents] = eventIds
    }
    suspend fun toggleNotificationCooldownSystemEvent(eventId: String) = editSafely { prefs ->
        val current = prefs[Keys.NotificationCooldownSystemEvents] ?: emptySet()
        prefs[Keys.NotificationCooldownSystemEvents] = if (eventId in current) current - eventId else current + eventId
    }
    suspend fun toggleNotificationCooldownExcludedPackage(packageName: String) = editSafely { prefs ->
        val current = prefs[Keys.NotificationCooldownExcludedPackages] ?: emptySet()
        prefs[Keys.NotificationCooldownExcludedPackages] = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
    }

    suspend fun restoreSettings(settings: SmartIslandSettings) = editSafely { prefs ->
        prefs[Keys.Enabled] = settings.enabled
        prefs[Keys.Width] = validDimension(
            settings.width,
            SmartIslandSettings.Default.width,
            SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH
        )
        prefs[Keys.Height] = validDimension(
            settings.height,
            SmartIslandSettings.Default.height,
            SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT
        )
        prefs[Keys.XOffset] = validDimension(
            settings.xOffset,
            SmartIslandSettings.Default.xOffset,
            SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET
        )
        prefs[Keys.YOffset] = validDimension(
            settings.yOffset,
            SmartIslandSettings.Default.yOffset,
            SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET
        )
        prefs[Keys.CornerRadius] = validDimension(
            settings.cornerRadius,
            SmartIslandSettings.Default.cornerRadius,
            SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS
        )
        prefs[Keys.Opacity] = validDimension(
            settings.opacity,
            SmartIslandSettings.Default.opacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        )
        prefs[Keys.PillColor] = validColor(settings.pillColor, SmartIslandSettings.Default.pillColor)
        prefs[Keys.BatteryColor] = validColor(settings.batteryColor, SmartIslandSettings.Default.batteryColor)
        prefs[Keys.NotificationDotColor] = validColor(settings.notificationDotColor, SmartIslandSettings.Default.notificationDotColor)
        prefs[Keys.MusicVisualizerColor] = validColor(settings.musicVisualizerColor, SmartIslandSettings.Default.musicVisualizerColor)
        prefs[Keys.HotspotColor] = validColor(settings.hotspotColor, SmartIslandSettings.Default.hotspotColor)
        prefs[Keys.CallColor] = validColor(settings.callColor, SmartIslandSettings.Default.callColor)
        prefs[Keys.LiveActivityColor] = validColor(settings.liveActivityColor, SmartIslandSettings.Default.liveActivityColor)
        prefs[Keys.TransferColor] = validColor(settings.transferColor, SmartIslandSettings.Default.transferColor)
        prefs[Keys.NavigationColor] = validColor(settings.navigationColor, SmartIslandSettings.Default.navigationColor)
        prefs[Keys.BluetoothColor] = validColor(settings.bluetoothColor, SmartIslandSettings.Default.bluetoothColor)
        prefs[Keys.FlashlightColor] = validColor(settings.flashlightColor, SmartIslandSettings.Default.flashlightColor)
        prefs[Keys.ScreenRecordingColor] = validColor(settings.screenRecordingColor, SmartIslandSettings.Default.screenRecordingColor)
        prefs[Keys.TimerColor] = validColor(settings.timerColor, SmartIslandSettings.Default.timerColor)
        prefs[Keys.StopwatchColor] = validColor(settings.stopwatchColor, SmartIslandSettings.Default.stopwatchColor)
        prefs[Keys.EnableAppShortcuts] = settings.enableAppShortcuts
        writePinnedApps(prefs, settings.shortcutPackages)
        prefs[Keys.ShowRecentApps] = settings.showRecentApps
        prefs[Keys.UtilityLayout] = com.agupta07505.smartisland.model.UtilityLayout.encode(
            com.agupta07505.smartisland.model.UtilityLayout.decode(settings.utilityLayout)
        )
        prefs[Keys.UtilityCalculatorPackage] = settings.utilityCalculatorPackage.take(255)
        prefs[Keys.UtilityCalculatorFloating] = settings.utilityCalculatorFloating
        prefs[Keys.WelcomeDialogShown] = settings.welcomeDialogShown
        prefs[Keys.ShowOnLockScreen] = settings.showOnLockScreen
        prefs[Keys.ShowLiveEventsInForeground] = settings.showLiveEventsInForeground
        prefs[Keys.LockScreenPrivacy] = settings.lockScreenPrivacy.takeIf { it in VALID_LOCK_SCREEN_PRIVACY_VALUES }
            ?: SmartIslandSettings.Default.lockScreenPrivacy
        prefs[Keys.ShowNotificationActions] = settings.showNotificationActions
        prefs[Keys.HideFromNotificationShade] = settings.hideFromNotificationShade
        prefs[Keys.LiveActivitiesEnabled] = settings.liveActivitiesEnabled
        prefs[Keys.NavigationEnabled] = settings.navigationEnabled
        prefs[Keys.DisabledNotificationPackages] = settings.disabledNotificationPackages
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.DisabledSoundPackages] = settings.disabledSoundPackages
        prefs[Keys.DisabledEvents] = settings.disabledEvents
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.HideWhenIdle] = settings.hideWhenIdle
        prefs[Keys.AutoHidePill] = settings.autoHidePill
        prefs[Keys.AutoHideTimeoutSeconds] = settings.autoHideTimeoutSeconds.coerceIn(1, 120)
        prefs[Keys.ShowInLandscape] = settings.showInLandscape
        prefs[Keys.AutoExpandOnNotification] = settings.autoExpandOnNotification
        prefs[Keys.AutoExpandOnLockScreen] = settings.autoExpandOnLockScreen
        prefs[Keys.EnableShadow] = settings.enableShadow
        prefs[Keys.ExpandedBlurEnabled] = settings.expandedBlurEnabled
        prefs[Keys.ExpandedBlurRadius] = settings.expandedBlurRadius
            .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
        prefs[Keys.MediaBlurEnabled] = settings.mediaBlurEnabled
        prefs[Keys.MediaBlurRadius] = settings.mediaBlurRadius
            .coerceIn(SmartIslandSettings.MIN_EXPANDED_BLUR, SmartIslandSettings.MAX_EXPANDED_BLUR)
        prefs[Keys.MediaOpacity] = validDimension(
            settings.mediaOpacity,
            SmartIslandSettings.Default.mediaOpacity,
            SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY
        )
        prefs[Keys.ExpandedGradientEnabled] = settings.expandedGradientEnabled
        prefs[Keys.ExpandedGradientColor] = validColor(settings.expandedGradientColor, SmartIslandSettings.Default.expandedGradientColor)
        prefs[Keys.ExpandedGradientStrength] = settings.expandedGradientStrength
            .coerceIn(SmartIslandSettings.MIN_GRADIENT_STRENGTH, SmartIslandSettings.MAX_GRADIENT_STRENGTH)
        prefs[Keys.ShadowElevation] = validDimension(
            settings.shadowElevation,
            SmartIslandSettings.Default.shadowElevation,
            SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION
        )
        prefs[Keys.EnableMusicArtworkBackground] = settings.enableMusicArtworkBackground
        prefs[Keys.EnableNotificationBackdrop] = settings.enableNotificationBackdrop
        prefs[Keys.DeviceType] = settings.deviceType
        prefs[Keys.AllowNetworkChecks] = settings.allowNetworkChecks
        prefs[Keys.DetectTransfersBySpeed] = settings.detectTransfersBySpeed
        prefs[Keys.EnableNotificationHistory] = settings.enableNotificationHistory
        prefs[Keys.NotificationHistoryRetentionHours] = settings.notificationHistoryRetentionHours
        prefs[Keys.ShowBluetoothBattery] = settings.showBluetoothBattery
        prefs[Keys.EnableBatteryMode] = settings.enableBatteryMode
        prefs[Keys.EnableNotificationCooldown] = settings.enableNotificationCooldown
        prefs[Keys.NotificationCooldownDurationMinutes] = settings.notificationCooldownDurationMinutes
        prefs[Keys.NotificationCooldownThreshold] = settings.notificationCooldownThreshold
        prefs[Keys.NotificationCooldownExcludedPackages] = settings.notificationCooldownExcludedPackages
        prefs[Keys.NotificationCooldownSystemEvents] = settings.notificationCooldownSystemEvents
        prefs[Keys.DeveloperModeEnabled] = settings.developerModeEnabled
        prefs[Keys.RecordLogs] = settings.recordLogs
        prefs[Keys.ThemeMode] = settings.themeMode
        prefs[Keys.LiquidGlassTheme] = settings.liquidGlassTheme
        prefs[Keys.MaterialYou] = settings.materialYou
        prefs[Keys.AccentColor] = validColor(settings.accentColor, SmartIslandSettings.Default.accentColor)
        prefs[Keys.ExpandedTheme] = if (settings.expandedTheme in SmartIslandSettings.VALID_EXPANDED_THEMES) settings.expandedTheme else SmartIslandSettings.Default.expandedTheme
        prefs[Keys.IslandFont] = settings.islandFont
        prefs[Keys.EnableNotchMode] = settings.enableNotchMode
        prefs[Keys.CameraCutoutManual] = settings.cameraCutoutManual
        prefs[Keys.CameraCutoutX] = validDimension(settings.cameraCutoutX, SmartIslandSettings.Default.cameraCutoutX, SmartIslandSettings.MIN_X_OFFSET, SmartIslandSettings.MAX_X_OFFSET)
        prefs[Keys.CameraCutoutY] = validDimension(settings.cameraCutoutY, SmartIslandSettings.Default.cameraCutoutY, SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_Y_OFFSET)
        prefs[Keys.CameraCutoutWidth] = validDimension(settings.cameraCutoutWidth, SmartIslandSettings.Default.cameraCutoutWidth, SmartIslandSettings.MIN_CUTOUT_WIDTH, SmartIslandSettings.MAX_CUTOUT_WIDTH)
        prefs[Keys.CameraCutoutHeight] = validDimension(settings.cameraCutoutHeight, SmartIslandSettings.Default.cameraCutoutHeight, SmartIslandSettings.MIN_CUTOUT_HEIGHT, SmartIslandSettings.MAX_CUTOUT_HEIGHT)
        prefs[Keys.EnableSwipeActions] = settings.enableSwipeActions
        prefs[Keys.SwipeUpAction] = settings.swipeUpAction
        prefs[Keys.SwipeHoldUpAction] = settings.swipeHoldUpAction
        prefs[Keys.SwipeDownAction] = settings.swipeDownAction
        prefs[Keys.SwipeDownCollapsedAction] = settings.swipeDownCollapsedAction
        prefs[Keys.SwipeLeftAction] = settings.swipeLeftAction
        prefs[Keys.SwipeRightAction] = settings.swipeRightAction
        prefs[Keys.SwipeHorizontalCollapsedAction] = settings.swipeHorizontalCollapsedAction
        prefs[Keys.EnablePillSwipeActions] = settings.enablePillSwipeActions
        prefs[Keys.PillSwipeUpAction] = settings.pillSwipeUpAction
        prefs[Keys.PillSwipeDownAction] = settings.pillSwipeDownAction
        prefs[Keys.PillSwipeLeftAction] = settings.pillSwipeLeftAction
        prefs[Keys.PillSwipeRightAction] = settings.pillSwipeRightAction
        prefs[Keys.LongPressAction] = settings.longPressAction
        prefs[Keys.CirclePosition] = if (settings.circlePosition in VALID_CIRCLE_POSITIONS) {
            settings.circlePosition
        } else {
            SmartIslandSettings.CIRCLE_POSITION_RIGHT
        }
        prefs[Keys.LandscapeShape] = settings.landscapeShape.takeIf { it in VALID_LANDSCAPE_SHAPES }
            ?: SmartIslandSettings.Default.landscapeShape
        prefs[Keys.LandscapeWidth] = settings.landscapeWidth
        prefs[Keys.LandscapeHeight] = settings.landscapeHeight
        prefs[Keys.LandscapeXOffset] = settings.landscapeXOffset
        prefs[Keys.LandscapeYOffset] = settings.landscapeYOffset
        prefs[Keys.LandscapeCornerRadius] = settings.landscapeCornerRadius
        prefs[Keys.LandscapeOpacity] = settings.landscapeOpacity
        prefs[Keys.LandscapeCircleSize] = settings.landscapeCircleSize
        prefs[Keys.LandscapeCircleOpacity] = settings.landscapeCircleOpacity
        prefs[Keys.LandscapeCircleEdgeInset] = settings.landscapeCircleEdgeInset
        prefs[Keys.LandscapeCircleYOffset] = settings.landscapeCircleYOffset
        prefs[Keys.LandscapeCircleXAdjust] = settings.landscapeCircleXAdjust
        prefs[Keys.LandscapeCircleYAdjust] = settings.landscapeCircleYAdjust
        prefs[Keys.LandscapeCirclePosition] = settings.landscapeCirclePosition.takeIf { it in VALID_LANDSCAPE_POSITIONS }
            ?: SmartIslandSettings.Default.landscapeCirclePosition
        prefs[Keys.LandscapePillPosition] = settings.landscapePillPosition.takeIf { it in VALID_LANDSCAPE_POSITIONS }
            ?: SmartIslandSettings.Default.landscapePillPosition
        prefs[Keys.DisabledEventsLandscape] = settings.disabledEventsLandscape
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.IdleTimeoutMs] = settings.idleTimeoutMs
            .coerceIn(SmartIslandSettings.MIN_IDLE_TIMEOUT_MS, SmartIslandSettings.MAX_IDLE_TIMEOUT_MS)
        prefs[Keys.BatteryWarningMs] = settings.batteryWarningMs
            .coerceIn(SmartIslandSettings.MIN_BATTERY_WARNING_MS, SmartIslandSettings.MAX_BATTERY_WARNING_MS)
        prefs[Keys.PopupMs] = settings.popupMs
            .coerceIn(SmartIslandSettings.MIN_POPUP_MS, SmartIslandSettings.MAX_POPUP_MS)
        prefs[Keys.TransferExpiryMinutes] = settings.transferExpiryMinutes
            .coerceIn(0, SmartIslandSettings.MAX_TRANSFER_EXPIRY_MINUTES)
        prefs[Keys.IdleWidgetsEnabled] = settings.idleWidgetsEnabled
        prefs[Keys.IdleWidgetsEnabledLandscape] = settings.idleWidgetsEnabledLandscape
        prefs[Keys.IdleOffAfterTapClose] = settings.idleOffAfterTapClose
        prefs[Keys.IdleOffAfterTapCloseLandscape] = settings.idleOffAfterTapCloseLandscape
        prefs[Keys.IdleOffAfterLongPress] = settings.idleOffAfterLongPress
        prefs[Keys.IdleOffAfterLongPressLandscape] = settings.idleOffAfterLongPressLandscape
        prefs[Keys.IdleOffWhenStored] = settings.idleOffWhenStored
        prefs[Keys.ContentPillSameAsIdle] = settings.contentPillSameAsIdle
        prefs[Keys.ContentPillWidthPercent] = settings.contentPillWidthPercent
        prefs[Keys.IslandExemptApps] = settings.islandExemptApps.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }.toSet()
        prefs[Keys.IslandExemptAppsLandscape] = settings.islandExemptAppsLandscape
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }.toSet()
        prefs[Keys.IslandExemptAppsStatic] = settings.islandExemptAppsStatic
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }.toSet()
        prefs[Keys.ContentPillSameAsIdleLandscape] = settings.contentPillSameAsIdleLandscape
        prefs[Keys.ContentPillWidthPercentLandscape] = settings.contentPillWidthPercentLandscape
        prefs[Keys.IdleOffWhenStoredLandscape] = settings.idleOffWhenStoredLandscape
        prefs[Keys.IdleGlyphTickSeconds] = com.agupta07505.smartisland.model.IdleSlots.normalizeTickSeconds(settings.idleGlyphTickSeconds)
        prefs[Keys.IdleWidgetIds] = com.agupta07505.smartisland.model.IdleWidgets.normalize(settings.idleWidgetIds)
        prefs[Keys.IdleWidgetCycleSeconds] = settings.idleWidgetCycleSeconds.coerceIn(
            com.agupta07505.smartisland.model.IdleWidgets.MIN_CYCLE_SECONDS,
            com.agupta07505.smartisland.model.IdleWidgets.MAX_CYCLE_SECONDS
        )
        prefs[Keys.WidgetIslandCloseSeconds] = com.agupta07505.smartisland.model.WidgetIslandTimer.normalize(settings.widgetIslandCloseSeconds)
        prefs[Keys.IdleAlarmHideBeyond12h] = settings.idleAlarmHideBeyond12h
        prefs[Keys.WidgetIslandBatteryMetric] = settings.widgetIslandBatteryMetric
            .takeIf { it in com.agupta07505.smartisland.model.WidgetIslandContent.VALID_BATTERY_METRICS } ?: com.agupta07505.smartisland.model.WidgetIslandContent.BATTERY_WATT
        prefs[Keys.IdleGlyphFont] = settings.idleGlyphFont.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: ""
        prefs[Keys.FontMixEnabled] = settings.fontMixEnabled
        prefs[Keys.FontMixLight] = settings.fontMixLight.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: ""
        prefs[Keys.FontMixRegular] = settings.fontMixRegular.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: ""
        prefs[Keys.FontMixMedium] = settings.fontMixMedium.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: ""
        prefs[Keys.IdleGlyphScale] = com.agupta07505.smartisland.model.IdleSlots.normalizeGlyphScale(settings.idleGlyphScale)
        prefs[Keys.IdleWidgetTempUnit] = settings.idleWidgetTempUnit
            .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_UNITS }
            ?: com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO
        prefs[Keys.IdleWidgetWindUnit] = settings.idleWidgetWindUnit
            .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_WIND_UNITS }
            ?: com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO
        prefs[Keys.IdleTickStyle] = com.agupta07505.smartisland.model.LedTick.normalizeStyle(settings.idleTickStyle)
        prefs[Keys.LedTickPattern] = com.agupta07505.smartisland.model.LedTick.normalizePattern(settings.ledTickPattern)
        prefs[Keys.LedTickPaceMs] = com.agupta07505.smartisland.model.LedTick.normalizePace(settings.ledTickPaceMs)
        prefs[Keys.LedTickFont] = settings.ledTickFont.takeIf { it.isEmpty() || SmartIslandSettings.isValidFont(it) } ?: ""
        prefs[Keys.LedTickColor] = com.agupta07505.smartisland.model.LedTick.normalizeColor(settings.ledTickColor)
        prefs[Keys.LedTickStagger] = settings.ledTickStagger
        prefs[Keys.SecondaryMergeMs] = settings.secondaryMergeMs
            .coerceIn(SmartIslandSettings.MIN_SECONDARY_MERGE_MS, SmartIslandSettings.MAX_SECONDARY_MERGE_MS)
        prefs[Keys.AnimSpeedPortrait] = settings.animSpeedPortrait
            .takeIf { it in AnimationSpeed.VALID_IDS } ?: SmartIslandSettings.Default.animSpeedPortrait
        prefs[Keys.AnimSpeedLandscape] = settings.animSpeedLandscape
            .takeIf { it in AnimationSpeed.VALID_IDS } ?: SmartIslandSettings.Default.animSpeedLandscape
        prefs[Keys.MergeStyleId] = settings.mergeStyle
            .takeIf { it in MergeStyle.VALID_IDS } ?: SmartIslandSettings.Default.mergeStyle
        prefs[Keys.MergeStyleLandscapeId] = settings.mergeStyleLandscape
            .takeIf { it in MergeStyle.VALID_IDS } ?: SmartIslandSettings.Default.mergeStyleLandscape
        prefs[Keys.ShowDuringDnd] = settings.showDuringDnd
        prefs[Keys.HapticStrength] = settings.hapticStrength
            .takeIf { it in com.agupta07505.smartisland.util.HapticStrength.VALID_IDS } ?: SmartIslandSettings.Default.hapticStrength
        prefs[Keys.FloatingWindowEnabled] = settings.floatingWindowEnabled
        prefs[Keys.FloatingWindowMode] = settings.floatingWindowMode
            .takeIf { it in com.agupta07505.smartisland.model.FloatingWindowMode.VALID_IDS } ?: SmartIslandSettings.Default.floatingWindowMode
        prefs[Keys.GestureSensitivityId] = settings.gestureSensitivity
            .takeIf { it in com.agupta07505.smartisland.util.GestureSensitivity.VALID_IDS } ?: SmartIslandSettings.Default.gestureSensitivity
        prefs[Keys.NoExpandPackages] = settings.noExpandPackages
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.NoAutoExpandPackages] = settings.noAutoExpandPackages
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.AutoExpandPackages] = settings.autoExpandPackages
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .filter { it !in settings.noAutoExpandPackages }
            .toSet()
        prefs[Keys.NoAutoExpandEvents] = settings.noAutoExpandEvents
            .filter { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id } }
            .toSet()
        prefs[Keys.HideAllOnLockScreen] = settings.hideAllOnLockScreen
        prefs[Keys.HideOnLockScreenEvents] = settings.hideOnLockScreenEvents
            .filter { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id && it.canHideOnLockScreen } }
            .toSet()
        prefs[Keys.NotificationsEnabled] = settings.notificationsEnabled
        prefs[Keys.HideOnLockScreenPackages] = settings.hideOnLockScreenPackages
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
    }

    suspend fun resetAllSettings() = restoreSettings(SmartIslandSettings.Default.copy(welcomeDialogShown = true))

    private suspend fun editSafely(transform: suspend (MutablePreferences) -> Unit) {
        try {
            context.smartIslandDataStore.edit(transform)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to persist settings", error)
        }
    }

    private fun validDimension(
        value: Float?,
        fallback: Float,
        min: Float,
        max: Float
    ): Float = value?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback

    /** Saves the pinned apps in order and drops the legacy unordered set, so the two can never disagree. */
    private fun writePinnedApps(prefs: MutablePreferences, value: List<String>) {
        prefs[Keys.ShortcutOrder] = PinnedApps.encode(value)
        prefs.remove(Keys.ShortcutPackages)
    }

    private fun validColor(value: Long?, fallback: Long): Long =
        value?.takeIf { it != 0L } ?: fallback

    private companion object {
        const val TAG = "SmartIslandSettings"
        const val MAX_SHORTCUTS = 8
        const val MAX_PACKAGE_NAME_LENGTH = 255
        val VALID_LOCK_SCREEN_PRIVACY_VALUES = setOf("AppIconOnly", "FullContent")
        val VALID_CIRCLE_POSITIONS = setOf(
            SmartIslandSettings.CIRCLE_POSITION_RIGHT,
            SmartIslandSettings.CIRCLE_POSITION_LEFT
        )
        val VALID_LANDSCAPE_SHAPES = setOf(
            SmartIslandSettings.LANDSCAPE_SHAPE_PILL,
            SmartIslandSettings.LANDSCAPE_SHAPE_CIRCLE
        )
        val VALID_LANDSCAPE_POSITIONS = setOf(
            SmartIslandSettings.LANDSCAPE_POSITION_AUTO,
            SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
            SmartIslandSettings.LANDSCAPE_POSITION_RIGHT,
            SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE
        )
    }
}