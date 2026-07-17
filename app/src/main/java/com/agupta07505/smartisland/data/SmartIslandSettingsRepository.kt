/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.smartIslandDataStore by preferencesDataStore(name = "smart_island_settings")

class SmartIslandSettingsRepository(private val context: Context) {
    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val Width = floatPreferencesKey("width")
        val Height = floatPreferencesKey("height")
        val XOffset = floatPreferencesKey("x_offset")
        val YOffset = floatPreferencesKey("y_offset")
        val CornerRadius = floatPreferencesKey("corner_radius")
        val BatteryColor = longPreferencesKey("battery_color")
        val NotificationDotColor = longPreferencesKey("notification_dot_color")
        val MusicVisualizerColor = longPreferencesKey("music_visualizer_color")
        val ShortcutPackages = stringSetPreferencesKey("shortcut_packages")
        val ShowRecentApps = booleanPreferencesKey("show_recent_apps")
        val WelcomeDialogShown = booleanPreferencesKey("welcome_dialog_shown")
        val ShowOnLockScreen = booleanPreferencesKey("show_on_lock_screen")
        val LockScreenPrivacy = stringPreferencesKey("lock_screen_privacy")
    }

    val settings: Flow<SmartIslandSettings> = context.smartIslandDataStore.data.map { prefs ->
        SmartIslandSettings(
            enabled = prefs[Keys.Enabled] ?: SmartIslandSettings.Default.enabled,
            width = prefs[Keys.Width] ?: SmartIslandSettings.Default.width,
            height = prefs[Keys.Height] ?: SmartIslandSettings.Default.height,
            xOffset = prefs[Keys.XOffset] ?: SmartIslandSettings.Default.xOffset,
            yOffset = prefs[Keys.YOffset] ?: SmartIslandSettings.Default.yOffset,
            cornerRadius = prefs[Keys.CornerRadius] ?: SmartIslandSettings.Default.cornerRadius,
            batteryColor = prefs[Keys.BatteryColor] ?: SmartIslandSettings.Default.batteryColor,
            notificationDotColor = prefs[Keys.NotificationDotColor] ?: SmartIslandSettings.Default.notificationDotColor,
            musicVisualizerColor = prefs[Keys.MusicVisualizerColor] ?: SmartIslandSettings.Default.musicVisualizerColor,
            shortcutPackages = prefs[Keys.ShortcutPackages] ?: SmartIslandSettings.Default.shortcutPackages,
            showRecentApps = prefs[Keys.ShowRecentApps] ?: SmartIslandSettings.Default.showRecentApps,
            welcomeDialogShown = prefs[Keys.WelcomeDialogShown] ?: SmartIslandSettings.Default.welcomeDialogShown,
            showOnLockScreen = prefs[Keys.ShowOnLockScreen] ?: SmartIslandSettings.Default.showOnLockScreen,
            lockScreenPrivacy = prefs[Keys.LockScreenPrivacy] ?: SmartIslandSettings.Default.lockScreenPrivacy
        )
    }

    suspend fun setEnabled(value: Boolean) = context.smartIslandDataStore.edit { it[Keys.Enabled] = value }
    suspend fun setWidth(value: Float) = context.smartIslandDataStore.edit { it[Keys.Width] = value }
    suspend fun setHeight(value: Float) = context.smartIslandDataStore.edit { it[Keys.Height] = value }
    suspend fun setXOffset(value: Float) = context.smartIslandDataStore.edit { it[Keys.XOffset] = value }
    suspend fun setYOffset(value: Float) = context.smartIslandDataStore.edit { it[Keys.YOffset] = value }
    suspend fun setCornerRadius(value: Float) = context.smartIslandDataStore.edit { it[Keys.CornerRadius] = value }
    suspend fun setBatteryColor(value: Long) = context.smartIslandDataStore.edit { it[Keys.BatteryColor] = value }
    suspend fun setNotificationDotColor(value: Long) = context.smartIslandDataStore.edit { it[Keys.NotificationDotColor] = value }
    suspend fun setMusicVisualizerColor(value: Long) = context.smartIslandDataStore.edit { it[Keys.MusicVisualizerColor] = value }
    suspend fun setShortcutPackages(value: Set<String>) = context.smartIslandDataStore.edit {
        it[Keys.ShortcutPackages] = value
    }
    suspend fun setShowRecentApps(value: Boolean) = context.smartIslandDataStore.edit {
        it[Keys.ShowRecentApps] = value
    }
    suspend fun setWelcomeDialogShown(value: Boolean) = context.smartIslandDataStore.edit {
        it[Keys.WelcomeDialogShown] = value
    }

    /**
     * Reads the REAL persisted value (awaits the first on-disk emission) instead of
     * relying on the SmartIslandSettings.Default placeholder that State flows emit on the
     * first frame. Used to decide whether the welcome dialog should show.
     */
    suspend fun isWelcomeDialogShown(): Boolean =
        context.smartIslandDataStore.data.first()[Keys.WelcomeDialogShown] ?: false
    suspend fun setShowOnLockScreen(value: Boolean) = context.smartIslandDataStore.edit {
        it[Keys.ShowOnLockScreen] = value
    }
    suspend fun setLockScreenPrivacy(value: String) = context.smartIslandDataStore.edit {
        it[Keys.LockScreenPrivacy] = value
    }

    suspend fun resetPosition() = context.smartIslandDataStore.edit {
        it[Keys.Width] = SmartIslandSettings.Default.width
        it[Keys.Height] = SmartIslandSettings.Default.height
        it[Keys.XOffset] = SmartIslandSettings.Default.xOffset
        it[Keys.YOffset] = SmartIslandSettings.Default.yOffset
        it[Keys.CornerRadius] = SmartIslandSettings.Default.cornerRadius
    }
}
