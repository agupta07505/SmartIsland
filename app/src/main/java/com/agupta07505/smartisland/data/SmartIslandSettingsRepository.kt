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
        val BatteryColor = longPreferencesKey("battery_color")
        val NotificationDotColor = longPreferencesKey("notification_dot_color")
        val MusicVisualizerColor = longPreferencesKey("music_visualizer_color")
        val ShortcutPackages = stringSetPreferencesKey("shortcut_packages")
        val ShowRecentApps = booleanPreferencesKey("show_recent_apps")
        val WelcomeDialogShown = booleanPreferencesKey("welcome_dialog_shown")
        val ShowOnLockScreen = booleanPreferencesKey("show_on_lock_screen")
        val LockScreenPrivacy = stringPreferencesKey("lock_screen_privacy")
        val ShowNotificationActions = booleanPreferencesKey("show_notification_actions")
        val HideFromNotificationShade = booleanPreferencesKey("hide_from_notification_shade")
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
                batteryColor = validColor(prefs[Keys.BatteryColor], defaults.batteryColor),
                notificationDotColor = validColor(
                    prefs[Keys.NotificationDotColor],
                    defaults.notificationDotColor
                ),
                musicVisualizerColor = validColor(
                    prefs[Keys.MusicVisualizerColor],
                    defaults.musicVisualizerColor
                ),
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
                    ?: defaults.hideFromNotificationShade
            )
        }

    suspend fun setEnabled(value: Boolean) = editSafely { it[Keys.Enabled] = value }
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
    suspend fun setCornerRadius(value: Float) = editSafely {
        it[Keys.CornerRadius] = validDimension(
            value,
            SmartIslandSettings.Default.cornerRadius,
            SmartIslandSettings.MIN_CORNER_RADIUS,
            SmartIslandSettings.MAX_CORNER_RADIUS
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

    suspend fun resetPosition() = editSafely {
        it[Keys.Width] = SmartIslandSettings.Default.width
        it[Keys.Height] = SmartIslandSettings.Default.height
        it[Keys.XOffset] = SmartIslandSettings.Default.xOffset
        it[Keys.YOffset] = SmartIslandSettings.Default.yOffset
        it[Keys.CornerRadius] = SmartIslandSettings.Default.cornerRadius
    }

    suspend fun exportSettingsJson(): String {
        val current = settings.first()
        val json = org.json.JSONObject().apply {
            put("enabled", current.enabled)
            put("width", current.width)
            put("height", current.height)
            put("xOffset", current.xOffset)
            put("yOffset", current.yOffset)
            put("cornerRadius", current.cornerRadius)
            put("batteryColor", current.batteryColor)
            put("notificationDotColor", current.notificationDotColor)
            put("musicVisualizerColor", current.musicVisualizerColor)
            put("shortcutPackages", org.json.JSONArray(current.shortcutPackages))
            put("showRecentApps", current.showRecentApps)
            put("showOnLockScreen", current.showOnLockScreen)
            put("lockScreenPrivacy", current.lockScreenPrivacy)
            put("showNotificationActions", current.showNotificationActions)
            put("hideFromNotificationShade", current.hideFromNotificationShade)
        }
        return json.toString(2)
    }

    suspend fun importSettingsJson(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            editSafely { prefs ->
                if (json.has("enabled")) prefs[Keys.Enabled] = json.getBoolean("enabled")
                if (json.has("width")) prefs[Keys.Width] = json.getDouble("width").toFloat()
                if (json.has("height")) prefs[Keys.Height] = json.getDouble("height").toFloat()
                if (json.has("xOffset")) prefs[Keys.XOffset] = json.getDouble("xOffset").toFloat()
                if (json.has("yOffset")) prefs[Keys.YOffset] = json.getDouble("yOffset").toFloat()
                if (json.has("cornerRadius")) prefs[Keys.CornerRadius] = json.getDouble("cornerRadius").toFloat()
                if (json.has("batteryColor")) prefs[Keys.BatteryColor] = json.getLong("batteryColor")
                if (json.has("notificationDotColor")) prefs[Keys.NotificationDotColor] = json.getLong("notificationDotColor")
                if (json.has("musicVisualizerColor")) prefs[Keys.MusicVisualizerColor] = json.getLong("musicVisualizerColor")
                if (json.has("shortcutPackages")) {
                    val array = json.getJSONArray("shortcutPackages")
                    val set = mutableSetOf<String>()
                    for (i in 0 until array.length()) {
                        set.add(array.getString(i))
                    }
                    prefs[Keys.ShortcutPackages] = set
                }
                if (json.has("showRecentApps")) prefs[Keys.ShowRecentApps] = json.getBoolean("showRecentApps")
                if (json.has("showOnLockScreen")) prefs[Keys.ShowOnLockScreen] = json.getBoolean("showOnLockScreen")
                if (json.has("lockScreenPrivacy")) prefs[Keys.LockScreenPrivacy] = json.getString("lockScreenPrivacy")
                if (json.has("showNotificationActions")) prefs[Keys.ShowNotificationActions] = json.getBoolean("showNotificationActions")
                if (json.has("hideFromNotificationShade")) prefs[Keys.HideFromNotificationShade] = json.getBoolean("hideFromNotificationShade")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings JSON", e)
            false
        }
    }

    suspend fun resetAllSettings() = editSafely { prefs ->
        prefs.clear()
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

    private fun validColor(value: Long?, fallback: Long): Long {
        val color = value ?: return fallback
        val hasValidArgbBits = color in MIN_ARGB_COLOR..MAX_ARGB_COLOR
        val hasVisibleAlpha = (color ushr ALPHA_SHIFT) != 0L
        return if (hasValidArgbBits && hasVisibleAlpha) color else fallback
    }

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
