/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.buddy.model.MascotPalette
import com.agupta07505.smartisland.buddy.model.MascotShape
import com.agupta07505.smartisland.buddy.model.MascotSide
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.MascotVisitFrequency
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
        val ShortcutPackages = stringSetPreferencesKey("shortcut_packages")
        val ShowRecentApps = booleanPreferencesKey("show_recent_apps")
        val WelcomeDialogShown = booleanPreferencesKey("welcome_dialog_shown")
        val ShowOnLockScreen = booleanPreferencesKey("show_on_lock_screen")
        val LockScreenPrivacy = stringPreferencesKey("lock_screen_privacy")
        val ShowNotificationActions = booleanPreferencesKey("show_notification_actions")
        val HideFromNotificationShade = booleanPreferencesKey("hide_from_notification_shade")
        val LiveActivitiesEnabled = booleanPreferencesKey("live_activities_enabled")
        val NavigationEnabled = booleanPreferencesKey("navigation_enabled")
        val DisabledNotificationPackages = stringSetPreferencesKey("disabled_notification_packages")
        val DisabledSoundPackages = stringSetPreferencesKey("disabled_sound_packages")
        val HideWhenIdle = booleanPreferencesKey("hide_when_idle")
        val AutoHidePill = booleanPreferencesKey("auto_hide_pill")
        val AutoHideTimeoutSeconds = intPreferencesKey("auto_hide_timeout_seconds")
        val ShowInLandscape = booleanPreferencesKey("show_in_landscape")
        val AutoExpandOnNotification = booleanPreferencesKey("auto_expand_on_notification")
        val EnableShadow = booleanPreferencesKey("enable_shadow")
        val ShadowElevation = floatPreferencesKey("shadow_elevation")
        val EnableMusicArtworkBackground = booleanPreferencesKey("enable_music_artwork_background")
        val EnableNotificationBackdrop = booleanPreferencesKey("enable_notification_backdrop")
        val DeviceType = stringPreferencesKey("device_type")
        val EnableNotificationHistory = booleanPreferencesKey("enable_notification_history")
        val NotificationHistoryRetentionHours = intPreferencesKey("notification_history_retention_hours")
        val ShowBluetoothBattery = booleanPreferencesKey("show_bluetooth_battery")
        val EnableBatteryMode = booleanPreferencesKey("enable_battery_mode")
        val EnableNotificationCooldown = booleanPreferencesKey("enable_notification_cooldown")
        val NotificationCooldownDurationMinutes = intPreferencesKey("notification_cooldown_duration_minutes")
        val NotificationCooldownThreshold = intPreferencesKey("notification_cooldown_threshold")
        val NotificationCooldownExcludedPackages = stringSetPreferencesKey("notification_cooldown_excluded_packages")
        val DeveloperModeEnabled = booleanPreferencesKey("developer_mode_enabled")
        val RecordLogs = booleanPreferencesKey("record_logs")
        val EnableNotchMode = booleanPreferencesKey("enable_notch_mode")
        val EnableSwipeActions = booleanPreferencesKey("enable_swipe_actions")
        val SwipeUpAction = stringPreferencesKey("swipe_up_action")
        val SwipeHoldUpAction = stringPreferencesKey("swipe_hold_up_action")
        val SwipeDownAction = stringPreferencesKey("swipe_down_action")
        val SwipeDownCollapsedAction = stringPreferencesKey("swipe_down_collapsed_action")
        val SwipeHorizontalCollapsedAction = stringPreferencesKey("swipe_horizontal_collapsed_action")
        val EnablePillSwipeActions = booleanPreferencesKey("enable_pill_swipe_actions")
        val PillSwipeUpAction = stringPreferencesKey("pill_swipe_up_action")
        val PillSwipeDownAction = stringPreferencesKey("pill_swipe_down_action")
        val PillSwipeLeftAction = stringPreferencesKey("pill_swipe_left_action")
        val PillSwipeRightAction = stringPreferencesKey("pill_swipe_right_action")
        val CirclePosition = stringPreferencesKey("circle_position")
        val BuddyEnabled = booleanPreferencesKey("buddy_enabled")
        val BuddyStyle = stringPreferencesKey("buddy_style")
        val BuddyShape = stringPreferencesKey("buddy_shape")
        val BuddyPalette = stringPreferencesKey("buddy_palette")
        val BuddySide = stringPreferencesKey("buddy_side")
        val BuddyVisits = stringPreferencesKey("buddy_visits")
        val SpringPreset = stringPreferencesKey("spring_preset")
        val KineticGlow = booleanPreferencesKey("kinetic_glow")
        val BuddyReacts = booleanPreferencesKey("buddy_reacts")
        val BuddyHidesWhenIdle = booleanPreferencesKey("buddy_hides_when_idle")
        val AodEnabled = booleanPreferencesKey("aod_enabled")
        val SecureAppPackages = stringSetPreferencesKey("secure_app_packages")
    }

    val settings: Flow<SmartIslandSettings> = context.smartIslandDataStore.data
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
                shortcutPackages = prefs[Keys.ShortcutPackages]
                    ?.asSequence()
                    ?.filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
                    ?.take(MAX_SHORTCUTS)
                    ?.toSet()
                    ?: defaults.shortcutPackages,
                showRecentApps = prefs[Keys.ShowRecentApps] ?: defaults.showRecentApps,
                welcomeDialogShown = prefs[Keys.WelcomeDialogShown] ?: defaults.welcomeDialogShown,
                showOnLockScreen = prefs[Keys.ShowOnLockScreen] ?: defaults.showOnLockScreen,
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
                hideWhenIdle = prefs[Keys.HideWhenIdle] ?: defaults.hideWhenIdle,
                autoHidePill = prefs[Keys.AutoHidePill] ?: defaults.autoHidePill,
                autoHideTimeoutSeconds = prefs[Keys.AutoHideTimeoutSeconds] ?: defaults.autoHideTimeoutSeconds,
                showInLandscape = prefs[Keys.ShowInLandscape] ?: defaults.showInLandscape,
                autoExpandOnNotification = prefs[Keys.AutoExpandOnNotification] ?: defaults.autoExpandOnNotification,
                enableShadow = prefs[Keys.EnableShadow] ?: defaults.enableShadow,
                shadowElevation = validDimension(
                    prefs[Keys.ShadowElevation],
                    defaults.shadowElevation,
                    SmartIslandSettings.MIN_SHADOW_ELEVATION,
                    SmartIslandSettings.MAX_SHADOW_ELEVATION
                ),
                enableMusicArtworkBackground = prefs[Keys.EnableMusicArtworkBackground] ?: defaults.enableMusicArtworkBackground,
                enableNotificationBackdrop = prefs[Keys.EnableNotificationBackdrop] ?: defaults.enableNotificationBackdrop,
                deviceType = prefs[Keys.DeviceType] ?: defaults.deviceType,
                enableNotificationHistory = prefs[Keys.EnableNotificationHistory] ?: defaults.enableNotificationHistory,
                notificationHistoryRetentionHours = prefs[Keys.NotificationHistoryRetentionHours] ?: defaults.notificationHistoryRetentionHours,
                showBluetoothBattery = prefs[Keys.ShowBluetoothBattery] ?: defaults.showBluetoothBattery,
                enableBatteryMode = prefs[Keys.EnableBatteryMode] ?: defaults.enableBatteryMode,
                enableNotificationCooldown = prefs[Keys.EnableNotificationCooldown] ?: defaults.enableNotificationCooldown,
                notificationCooldownDurationMinutes = prefs[Keys.NotificationCooldownDurationMinutes] ?: defaults.notificationCooldownDurationMinutes,
                notificationCooldownThreshold = prefs[Keys.NotificationCooldownThreshold] ?: defaults.notificationCooldownThreshold,
                notificationCooldownExcludedPackages = prefs[Keys.NotificationCooldownExcludedPackages] ?: defaults.notificationCooldownExcludedPackages,
                developerModeEnabled = prefs[Keys.DeveloperModeEnabled] ?: defaults.developerModeEnabled,
                recordLogs = prefs[Keys.RecordLogs] ?: defaults.recordLogs,
                enableNotchMode = prefs[Keys.EnableNotchMode] ?: defaults.enableNotchMode,
                enableSwipeActions = prefs[Keys.EnableSwipeActions] ?: defaults.enableSwipeActions,
                swipeUpAction = prefs[Keys.SwipeUpAction] ?: defaults.swipeUpAction,
                swipeHoldUpAction = prefs[Keys.SwipeHoldUpAction] ?: defaults.swipeHoldUpAction,
                swipeDownAction = prefs[Keys.SwipeDownAction] ?: defaults.swipeDownAction,
                swipeDownCollapsedAction = prefs[Keys.SwipeDownCollapsedAction] ?: defaults.swipeDownCollapsedAction,
                swipeHorizontalCollapsedAction = prefs[Keys.SwipeHorizontalCollapsedAction] ?: defaults.swipeHorizontalCollapsedAction,
                enablePillSwipeActions = prefs[Keys.EnablePillSwipeActions] ?: defaults.enablePillSwipeActions,
                pillSwipeUpAction = prefs[Keys.PillSwipeUpAction] ?: defaults.pillSwipeUpAction,
                pillSwipeDownAction = prefs[Keys.PillSwipeDownAction] ?: defaults.pillSwipeDownAction,
                pillSwipeLeftAction = prefs[Keys.PillSwipeLeftAction] ?: defaults.pillSwipeLeftAction,
                pillSwipeRightAction = prefs[Keys.PillSwipeRightAction] ?: defaults.pillSwipeRightAction,
                circlePosition = prefs[Keys.CirclePosition]?.takeIf { it in VALID_CIRCLE_POSITIONS } ?: defaults.circlePosition,
                buddyEnabled = prefs[Keys.BuddyEnabled] ?: defaults.buddyEnabled,
                buddyStyle = MascotStyle.fromId(prefs[Keys.BuddyStyle]).id,
                buddyShape = MascotShape.fromId(prefs[Keys.BuddyShape]).id,
                buddyPalette = MascotPalette.fromId(prefs[Keys.BuddyPalette]).id,
                buddySide = MascotSide.fromId(prefs[Keys.BuddySide]).id,
                buddyVisits = MascotVisitFrequency.fromId(prefs[Keys.BuddyVisits]).id,
                springPreset = com.agupta07505.smartisland.ui.SpringPreset.fromId(prefs[Keys.SpringPreset]).id,
                kineticGlow = prefs[Keys.KineticGlow] ?: defaults.kineticGlow,
                buddyReacts = prefs[Keys.BuddyReacts] ?: defaults.buddyReacts,
                buddyHidesWhenIdle = prefs[Keys.BuddyHidesWhenIdle] ?: defaults.buddyHidesWhenIdle,
                aodEnabled = prefs[Keys.AodEnabled] ?: defaults.aodEnabled,
                secureAppPackages = prefs[Keys.SecureAppPackages] ?: defaults.secureAppPackages
            )
        }

    suspend fun setDeviceType(value: String) = editSafely { it[Keys.DeviceType] = value }
    suspend fun setEnabled(value: Boolean) = editSafely { it[Keys.Enabled] = value }
    suspend fun setEnableShadow(value: Boolean) = editSafely { it[Keys.EnableShadow] = value }
    suspend fun setShadowElevation(value: Float) = editSafely {
        it[Keys.ShadowElevation] = validDimension(
            value,
            SmartIslandSettings.Default.shadowElevation,
            SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION
        )
    }
    suspend fun setEnableMusicArtworkBackground(value: Boolean) = editSafely { it[Keys.EnableMusicArtworkBackground] = value }
    suspend fun setEnableNotificationBackdrop(value: Boolean) = editSafely { it[Keys.EnableNotificationBackdrop] = value }
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
    suspend fun setShortcutPackages(value: Set<String>) = editSafely {
        it[Keys.ShortcutPackages] = value
            .asSequence()
            .filter { packageName ->
                packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH
            }
            .take(MAX_SHORTCUTS)
            .toSet()
    }
    suspend fun setShowRecentApps(value: Boolean) = editSafely {
        it[Keys.ShowRecentApps] = value
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
    suspend fun setLiveActivitiesEnabled(value: Boolean) = editSafely {
        it[Keys.LiveActivitiesEnabled] = value
    }
    suspend fun setNavigationEnabled(value: Boolean) = editSafely {
        it[Keys.NavigationEnabled] = value
    }
    suspend fun setDisabledNotificationPackages(value: Set<String>) = editSafely {
        it[Keys.DisabledNotificationPackages] = value
            .asSequence()
            .filter { pkg -> pkg.isNotBlank() && pkg.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
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
    suspend fun setEnableNotificationHistory(value: Boolean) = editSafely {
        it[Keys.EnableNotificationHistory] = value
    }
    suspend fun setNotificationHistoryRetentionHours(value: Int) = editSafely {
        it[Keys.NotificationHistoryRetentionHours] = value
    }
    suspend fun setShowBluetoothBattery(value: Boolean) = editSafely {
        it[Keys.ShowBluetoothBattery] = value
    }
    suspend fun setEnableBatteryMode(value: Boolean) = editSafely {
        it[Keys.EnableBatteryMode] = value
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
    suspend fun setDeveloperModeEnabled(value: Boolean) = editSafely {
        it[Keys.DeveloperModeEnabled] = value
    }
    suspend fun setRecordLogs(value: Boolean) = editSafely {
        it[Keys.RecordLogs] = value
    }
    suspend fun setEnableNotchMode(value: Boolean) = editSafely {
        it[Keys.EnableNotchMode] = value
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
    suspend fun setSwipeDownCollapsedAction(value: String) = editSafely {
        it[Keys.SwipeDownCollapsedAction] = value
    }
    suspend fun setSwipeHorizontalCollapsedAction(value: String) = editSafely {
        it[Keys.SwipeHorizontalCollapsedAction] = value
    }
    suspend fun setEnablePillSwipeActions(value: Boolean) = editSafely {
        it[Keys.EnablePillSwipeActions] = value
    }
    suspend fun setPillSwipeUpAction(value: String) = editSafely {
        it[Keys.PillSwipeUpAction] = value
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
    suspend fun setCirclePosition(value: String) = editSafely {
        it[Keys.CirclePosition] = if (value in VALID_CIRCLE_POSITIONS) value else SmartIslandSettings.CIRCLE_POSITION_RIGHT
    }
    suspend fun setBuddyEnabled(value: Boolean) = editSafely { it[Keys.BuddyEnabled] = value }
    suspend fun setBuddyStyle(value: String) = editSafely { it[Keys.BuddyStyle] = MascotStyle.fromId(value).id }
    suspend fun setBuddyShape(value: String) = editSafely { it[Keys.BuddyShape] = MascotShape.fromId(value).id }
    suspend fun setBuddyPalette(value: String) = editSafely { it[Keys.BuddyPalette] = MascotPalette.fromId(value).id }
    suspend fun setBuddySide(value: String) = editSafely { it[Keys.BuddySide] = MascotSide.fromId(value).id }
    suspend fun setBuddyVisits(value: String) = editSafely { it[Keys.BuddyVisits] = MascotVisitFrequency.fromId(value).id }
    suspend fun setSpringPreset(value: String) = editSafely { it[Keys.SpringPreset] = com.agupta07505.smartisland.ui.SpringPreset.fromId(value).id }
    suspend fun setKineticGlow(value: Boolean) = editSafely { it[Keys.KineticGlow] = value }
    suspend fun setBuddyReacts(value: Boolean) = editSafely { it[Keys.BuddyReacts] = value }
    suspend fun setBuddyHidesWhenIdle(value: Boolean) = editSafely { it[Keys.BuddyHidesWhenIdle] = value }
    suspend fun toggleNotificationCooldownExcludedPackage(packageName: String) = editSafely { prefs ->
        val current = prefs[Keys.NotificationCooldownExcludedPackages] ?: emptySet()
        prefs[Keys.NotificationCooldownExcludedPackages] = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
    }

    suspend fun resetPosition() = editSafely {
        it[Keys.Width] = SmartIslandSettings.Default.width
        it[Keys.Height] = SmartIslandSettings.Default.height
        it[Keys.XOffset] = SmartIslandSettings.Default.xOffset
        it[Keys.YOffset] = SmartIslandSettings.Default.yOffset
        it[Keys.CornerRadius] = SmartIslandSettings.Default.cornerRadius
        it[Keys.Opacity] = SmartIslandSettings.Default.opacity
        it[Keys.PillColor] = SmartIslandSettings.Default.pillColor
        it[Keys.ShadowElevation] = SmartIslandSettings.Default.shadowElevation
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
        prefs[Keys.ShortcutPackages] = settings.shortcutPackages
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .take(MAX_SHORTCUTS)
            .toSet()
        prefs[Keys.ShowRecentApps] = settings.showRecentApps
        prefs[Keys.WelcomeDialogShown] = settings.welcomeDialogShown
        prefs[Keys.ShowOnLockScreen] = settings.showOnLockScreen
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
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            .toSet()
        prefs[Keys.HideWhenIdle] = settings.hideWhenIdle
        prefs[Keys.AutoHidePill] = settings.autoHidePill
        prefs[Keys.AutoHideTimeoutSeconds] = settings.autoHideTimeoutSeconds.coerceIn(1, 120)
        prefs[Keys.ShowInLandscape] = settings.showInLandscape
        prefs[Keys.AutoExpandOnNotification] = settings.autoExpandOnNotification
        prefs[Keys.EnableShadow] = settings.enableShadow
        prefs[Keys.ShadowElevation] = validDimension(
            settings.shadowElevation,
            SmartIslandSettings.Default.shadowElevation,
            SmartIslandSettings.MIN_SHADOW_ELEVATION,
            SmartIslandSettings.MAX_SHADOW_ELEVATION
        )
        prefs[Keys.EnableMusicArtworkBackground] = settings.enableMusicArtworkBackground
        prefs[Keys.EnableNotificationBackdrop] = settings.enableNotificationBackdrop
        prefs[Keys.DeviceType] = settings.deviceType
        prefs[Keys.EnableNotificationHistory] = settings.enableNotificationHistory
        prefs[Keys.NotificationHistoryRetentionHours] = settings.notificationHistoryRetentionHours
        prefs[Keys.ShowBluetoothBattery] = settings.showBluetoothBattery
        prefs[Keys.EnableBatteryMode] = settings.enableBatteryMode
        prefs[Keys.EnableNotificationCooldown] = settings.enableNotificationCooldown
        prefs[Keys.NotificationCooldownDurationMinutes] = settings.notificationCooldownDurationMinutes
        prefs[Keys.NotificationCooldownThreshold] = settings.notificationCooldownThreshold
        prefs[Keys.NotificationCooldownExcludedPackages] = settings.notificationCooldownExcludedPackages
        prefs[Keys.DeveloperModeEnabled] = settings.developerModeEnabled
        prefs[Keys.RecordLogs] = settings.recordLogs
        prefs[Keys.EnableNotchMode] = settings.enableNotchMode
        prefs[Keys.EnableSwipeActions] = settings.enableSwipeActions
        prefs[Keys.SwipeUpAction] = settings.swipeUpAction
        prefs[Keys.SwipeHoldUpAction] = settings.swipeHoldUpAction
        prefs[Keys.SwipeDownAction] = settings.swipeDownAction
        prefs[Keys.SwipeDownCollapsedAction] = settings.swipeDownCollapsedAction
        prefs[Keys.SwipeHorizontalCollapsedAction] = settings.swipeHorizontalCollapsedAction
        prefs[Keys.EnablePillSwipeActions] = settings.enablePillSwipeActions
        prefs[Keys.PillSwipeUpAction] = settings.pillSwipeUpAction
        prefs[Keys.PillSwipeDownAction] = settings.pillSwipeDownAction
        prefs[Keys.PillSwipeLeftAction] = settings.pillSwipeLeftAction
        prefs[Keys.PillSwipeRightAction] = settings.pillSwipeRightAction
        prefs[Keys.CirclePosition] = if (settings.circlePosition in VALID_CIRCLE_POSITIONS) {
            settings.circlePosition
        } else {
            SmartIslandSettings.CIRCLE_POSITION_RIGHT
        }
        prefs[Keys.BuddyEnabled] = settings.buddyEnabled
        prefs[Keys.BuddyStyle] = MascotStyle.fromId(settings.buddyStyle).id
        prefs[Keys.BuddyShape] = MascotShape.fromId(settings.buddyShape).id
        prefs[Keys.BuddyPalette] = MascotPalette.fromId(settings.buddyPalette).id
        prefs[Keys.BuddySide] = MascotSide.fromId(settings.buddySide).id
        prefs[Keys.BuddyVisits] = MascotVisitFrequency.fromId(settings.buddyVisits).id
        prefs[Keys.SpringPreset] = com.agupta07505.smartisland.ui.SpringPreset.fromId(settings.springPreset).id
        prefs[Keys.KineticGlow] = settings.kineticGlow
        prefs[Keys.BuddyReacts] = settings.buddyReacts
        prefs[Keys.BuddyHidesWhenIdle] = settings.buddyHidesWhenIdle
        prefs[Keys.AodEnabled] = settings.aodEnabled
        prefs[Keys.SecureAppPackages] = settings.secureAppPackages
    }

    suspend fun setAodEnabled(value: Boolean) = editSafely { it[Keys.AodEnabled] = value }
    suspend fun setSecureAppPackages(packages: Set<String>) = editSafely { it[Keys.SecureAppPackages] = packages }
    suspend fun addSecureAppPackage(packageName: String) = editSafely {
        val current = it[Keys.SecureAppPackages]?.toMutableSet() ?: mutableSetOf()
        current.add(packageName)
        it[Keys.SecureAppPackages] = current
    }
    suspend fun removeSecureAppPackage(packageName: String) = editSafely {
        val current = it[Keys.SecureAppPackages]?.toMutableSet() ?: mutableSetOf()
        current.remove(packageName)
        it[Keys.SecureAppPackages] = current
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

    private fun validColor(value: Long?, fallback: Long): Long =
        value?.takeIf { it != 0L } ?: fallback

    private companion object {
        const val TAG = "SmartIslandSettings"
        const val MAX_SHORTCUTS = 8
        const val MAX_PACKAGE_NAME_LENGTH = 255
        const val MIN_ARGB_COLOR = 0x01000000L
        const val MAX_ARGB_COLOR = 0xFFFFFFFFL
        const val ALPHA_SHIFT = 24
        val VALID_LOCK_SCREEN_PRIVACY_VALUES = setOf("AppIconOnly", "FullContent")
        val VALID_CIRCLE_POSITIONS = setOf(
            SmartIslandSettings.CIRCLE_POSITION_RIGHT,
            SmartIslandSettings.CIRCLE_POSITION_LEFT
        )
    }
}
