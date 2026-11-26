/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.data.SmartIslandSettings.Companion.BACKUP_FORMAT_VERSION
import org.json.JSONObject

internal fun SmartIslandSettings.toBackupJson(appVersion: String): String {
    val root = JSONObject()
    root.put("formatVersion", BACKUP_FORMAT_VERSION)
    root.put("app", "com.agupta07505.smartisland")
    if (appVersion.isNotBlank()) {
        root.put("appVersion", appVersion)
    }
    root.put("exportTimestamp", System.currentTimeMillis())

    val settingsObj = JSONObject()
    putPillLook(settingsObj)
    putShortcutsAndNotificationRules(settingsObj)
    putDisplayAndExpandedStyle(settingsObj)
    putAppBehaviour(settingsObj)
    putThemeAndCutout(settingsObj)
    putGestures(settingsObj)
    putLandscape(settingsObj)
    putTimingsAndIdleSwitches(settingsObj)
    putIdleWidgets(settingsObj)
    putWeatherAndNetSpeed(settingsObj)
    putIdleScheduleAndLedTick(settingsObj)
    putMotionAndInput(settingsObj)
    putAutoExpand(settingsObj)

    root.put("settings", settingsObj)
    return root.toString(2)
}

internal fun settingsFromJson(jsonString: String): SmartIslandSettings {
    val root = JSONObject(jsonString)
    val reader = BackupReader(root.optJSONObject("settings") ?: root)

    return SmartIslandSettings.Default
        .readJsonPillLookAndColors(reader)
        .readJsonShortcutsAndNotificationRules(reader)
        .readJsonDisplayAndExpandedStyle(reader)
        .readJsonAppBehaviour(reader)
        .readJsonThemeAndCutout(reader)
        .readJsonGestures(reader)
        .readJsonLandscape(reader)
        .readJsonTimingsAndIdleSwitches(reader)
        .readJsonIdleWidgets(reader)
        .readJsonWeatherAndNetSpeed(reader)
        .readJsonIdleScheduleAndLedTick(reader)
        .readJsonMotionAndInput(reader)
        .readJsonAutoExpand(reader, root.optInt("formatVersion", 1))
}
