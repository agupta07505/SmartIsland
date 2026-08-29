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
        val EnableMusicArtworkBackground = booleanPreferencesKey("enable_music_artwork_background")
        val DeviceType = stringPreferencesKey("device_type")
        val AllowNetworkChecks = booleanPreferencesKey("allow_network_checks")
        val EnableNotificationHistory = booleanPreferencesKey("enable_notification_history")
        val NotificationHistoryRetentionHours = intPreferencesKey("notification_history_retention_hours")
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
                enableMusicArtworkBackground = prefs[Keys.EnableMusicArtworkBackground] ?: defaults.enableMusicArtworkBackground,
                deviceType = prefs[Keys.DeviceType] ?: defaults.deviceType,
                allowNetworkChecks = prefs[Keys.AllowNetworkChecks] ?: defaults.allowNetworkChecks,
                enableNotificationHistory = prefs[Keys.EnableNotificationHistory] ?: defaults.enableNotificationHistory,
                notificationHistoryRetentionHours = prefs[Keys.NotificationHistoryRetentionHours] ?: defaults.notificationHistoryRetentionHours
            )
        }

    suspend fun setDeviceType(value: String) = editSafely { it[Keys.DeviceType] = value }
    suspend fun setEnabled(value: Boolean) = editSafely { it[Keys.Enabled] = value }
    suspend fun setEnableShadow(value: Boolean) = editSafely { it[Keys.EnableShadow] = value }
    suspend fun setEnableMusicArtworkBackground(value: Boolean) = editSafely { it[Keys.EnableMusicArtworkBackground] = value }
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
    suspend fun setAllowNetworkChecks(value: Boolean) = editSafely {
        it[Keys.AllowNetworkChecks] = value
    }
    suspend fun setEnableNotificationHistory(value: Boolean) = editSafely {
        it[Keys.EnableNotificationHistory] = value
    }
    suspend fun setNotificationHistoryRetentionHours(value: Int) = editSafely {
        it[Keys.NotificationHistoryRetentionHours] = value
    }

    suspend fun resetPosition() = editSafely {
        it[Keys.Width] = SmartIslandSettings.Default.width
        it[Keys.Height] = SmartIslandSettings.Default.height
        it[Keys.XOffset] = SmartIslandSettings.Default.xOffset
        it[Keys.YOffset] = SmartIslandSettings.Default.yOffset
        it[Keys.CornerRadius] = SmartIslandSettings.Default.cornerRadius
        it[Keys.Opacity] = SmartIslandSettings.Default.opacity
    }

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
    }
}
