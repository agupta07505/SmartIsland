package com.agupta07505.smartisland.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
    }

    val settings: Flow<SmartIslandSettings> = context.smartIslandDataStore.data.map { prefs ->
        SmartIslandSettings(
            enabled = prefs[Keys.Enabled] ?: SmartIslandSettings.Default.enabled,
            width = prefs[Keys.Width] ?: SmartIslandSettings.Default.width,
            height = prefs[Keys.Height] ?: SmartIslandSettings.Default.height,
            xOffset = prefs[Keys.XOffset] ?: SmartIslandSettings.Default.xOffset,
            yOffset = prefs[Keys.YOffset] ?: SmartIslandSettings.Default.yOffset,
            cornerRadius = prefs[Keys.CornerRadius] ?: SmartIslandSettings.Default.cornerRadius
        )
    }

    suspend fun setEnabled(value: Boolean) = context.smartIslandDataStore.edit { it[Keys.Enabled] = value }
    suspend fun setWidth(value: Float) = context.smartIslandDataStore.edit { it[Keys.Width] = value }
    suspend fun setHeight(value: Float) = context.smartIslandDataStore.edit { it[Keys.Height] = value }
    suspend fun setXOffset(value: Float) = context.smartIslandDataStore.edit { it[Keys.XOffset] = value }
    suspend fun setYOffset(value: Float) = context.smartIslandDataStore.edit { it[Keys.YOffset] = value }
    suspend fun setCornerRadius(value: Float) = context.smartIslandDataStore.edit { it[Keys.CornerRadius] = value }

    suspend fun resetPosition() = context.smartIslandDataStore.edit {
        it[Keys.Width] = SmartIslandSettings.Default.width
        it[Keys.Height] = SmartIslandSettings.Default.height
        it[Keys.XOffset] = SmartIslandSettings.Default.xOffset
        it[Keys.YOffset] = SmartIslandSettings.Default.yOffset
        it[Keys.CornerRadius] = SmartIslandSettings.Default.cornerRadius
    }
}
