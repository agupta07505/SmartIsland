/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.EventColors
import org.json.JSONArray
import org.json.JSONObject

internal fun SmartIslandSettings.putPillLook(settingsObj: JSONObject) {
settingsObj.put("enabled", enabled)
settingsObj.put("width", width.toDouble())
settingsObj.put("height", height.toDouble())
settingsObj.put("xOffset", xOffset.toDouble())
settingsObj.put("yOffset", yOffset.toDouble())
settingsObj.put("cornerRadius", cornerRadius.toDouble())
settingsObj.put("opacity", opacity.toDouble())
settingsObj.put("pillColor", pillColor)
settingsObj.put("oledBlack", oledBlack)
settingsObj.put("batteryColor", batteryColor)
settingsObj.put("notificationDotColor", notificationDotColor)
settingsObj.put("musicVisualizerColor", musicVisualizerColor)
settingsObj.put("hotspotColor", hotspotColor)
settingsObj.put("callColor", callColor)
settingsObj.put("liveActivityColor", liveActivityColor)
settingsObj.put("transferColor", transferColor)
settingsObj.put("navigationColor", navigationColor)
settingsObj.put("bluetoothColor", bluetoothColor)
settingsObj.put("flashlightColor", flashlightColor)
settingsObj.put("screenRecordingColor", screenRecordingColor)
settingsObj.put("timerColor", timerColor)
settingsObj.put("stopwatchColor", stopwatchColor)
val eventColorsObj = JSONObject()
EventColors.clean(eventColors).forEach { (id, color) -> eventColorsObj.put(id, color) }
settingsObj.put("eventColors", eventColorsObj)
}

internal fun SmartIslandSettings.putShortcutsAndNotificationRules(settingsObj: JSONObject) {
settingsObj.put("enableAppShortcuts", enableAppShortcuts)

val shortcutsArray = JSONArray()
shortcutPackages.forEach { shortcutsArray.put(it) }
settingsObj.put("shortcutPackages", shortcutsArray)

settingsObj.put("showRecentApps", showRecentApps)
settingsObj.put("utilityLayout", utilityLayout)
settingsObj.put("utilityCalculatorPackage", utilityCalculatorPackage)
settingsObj.put("utilityCalculatorFloating", utilityCalculatorFloating)
settingsObj.put("welcomeDialogShown", welcomeDialogShown)
settingsObj.put("torchStrobeAcknowledged", torchStrobeAcknowledged)
settingsObj.put("showOnLockScreen", showOnLockScreen)
val showInForegroundArray = JSONArray()
showInForegroundEvents.forEach { showInForegroundArray.put(it) }
settingsObj.put("showInForegroundEvents", showInForegroundArray)
settingsObj.put("hideInFullscreen", hideInFullscreen)
settingsObj.put("hideInShade", hideInShade)
settingsObj.put("lockScreenPrivacy", lockScreenPrivacy)
settingsObj.put("showNotificationActions", showNotificationActions)
settingsObj.put("hideFromNotificationShade", hideFromNotificationShade)
settingsObj.put("liveActivitiesEnabled", liveActivitiesEnabled)
settingsObj.put("navigationEnabled", navigationEnabled)

val disabledNotifArray = JSONArray()
disabledNotificationPackages.forEach { disabledNotifArray.put(it) }
settingsObj.put("disabledNotificationPackages", disabledNotifArray)

val disabledSoundArray = JSONArray()
disabledSoundPackages.forEach { disabledSoundArray.put(it) }
settingsObj.put("disabledSoundPackages", disabledSoundArray)

val disabledEventsArray = JSONArray()
disabledEvents.forEach { disabledEventsArray.put(it) }
settingsObj.put("disabledEvents", disabledEventsArray)

}

internal fun SmartIslandSettings.putDisplayAndExpandedStyle(settingsObj: JSONObject) {
settingsObj.put("hideWhenIdle", hideWhenIdle)
settingsObj.put("autoHidePill", autoHidePill)
settingsObj.put("autoHideTimeoutSeconds", autoHideTimeoutSeconds)
settingsObj.put("showInLandscape", showInLandscape)
settingsObj.put("autoExpandOnNotification", autoExpandOnNotification)
settingsObj.put("autoExpandOnLockScreen", autoExpandOnLockScreen)
settingsObj.put("showSuggestedReplies", showSuggestedReplies)
settingsObj.put("showCopyCodeChip", showCopyCodeChip)
settingsObj.put("enableShadow", enableShadow)
settingsObj.put("expandedBlurEnabled", expandedBlurEnabled)
settingsObj.put("expandedBlurRadius", expandedBlurRadius.toDouble())
settingsObj.put("mediaBlurEnabled", mediaBlurEnabled)
settingsObj.put("mediaBlurRadius", mediaBlurRadius.toDouble())
settingsObj.put("mediaOpacity", mediaOpacity.toDouble())
settingsObj.put("expandedGradientEnabled", expandedGradientEnabled)
settingsObj.put("expandedGradientColor", expandedGradientColor)
settingsObj.put("expandedGradientStrength", expandedGradientStrength.toDouble())
settingsObj.put("shadowElevation", shadowElevation.toDouble())
settingsObj.put("enableMusicArtworkBackground", enableMusicArtworkBackground)
settingsObj.put("enableNotificationBackdrop", enableNotificationBackdrop)
}

internal fun SmartIslandSettings.putAppBehaviour(settingsObj: JSONObject) {
settingsObj.put("deviceType", deviceType)
settingsObj.put("allowNetworkChecks", allowNetworkChecks)
settingsObj.put("detectTransfersBySpeed", detectTransfersBySpeed)
settingsObj.put("enableNotificationHistory", enableNotificationHistory)
settingsObj.put("notificationHistoryRetentionHours", notificationHistoryRetentionHours)
settingsObj.put("showBluetoothBattery", showBluetoothBattery)
settingsObj.put("enableBatteryMode", enableBatteryMode)
settingsObj.put("enableNotificationCooldown", enableNotificationCooldown)
settingsObj.put("notificationCooldownDurationMinutes", notificationCooldownDurationMinutes)
settingsObj.put("notificationCooldownThreshold", notificationCooldownThreshold)

val cooldownExcludedArray = JSONArray()
notificationCooldownExcludedPackages.forEach { cooldownExcludedArray.put(it) }
settingsObj.put("notificationCooldownExcludedPackages", cooldownExcludedArray)
val cooldownSystemEventsArray = JSONArray()
notificationCooldownSystemEvents.forEach { cooldownSystemEventsArray.put(it) }
settingsObj.put("notificationCooldownSystemEvents", cooldownSystemEventsArray)

settingsObj.put("developerModeEnabled", developerModeEnabled)
settingsObj.put("recordLogs", recordLogs)
settingsObj.put("faceUnlockMode", faceUnlockMode)
settingsObj.put("faceIdStyle", faceIdStyle)
settingsObj.put("appFaceIdEnabled", appFaceIdEnabled)
settingsObj.put("appFaceIdStyle", appFaceIdStyle)
settingsObj.put("statusBarHidden", statusBarHidden)
settingsObj.put("statusBarHidePersistent", statusBarHidePersistent)
settingsObj.put("statusBarHideNotificationIcons", statusBarHideNotificationIcons)
settingsObj.put("statusBarHideClock", statusBarHideClock)
settingsObj.put("statusBarHideAllSystemIcons", statusBarHideAllSystemIcons)
val statusBarIconsArray = JSONArray()
statusBarHiddenSystemIcons.forEach { statusBarIconsArray.put(it) }
settingsObj.put("statusBarHiddenSystemIcons", statusBarIconsArray)
settingsObj.put("fingerprintAnimationEnabled", fingerprintAnimationEnabled)
}

internal fun SmartIslandSettings.putThemeAndCutout(settingsObj: JSONObject) {
settingsObj.put("themeMode", themeMode)
settingsObj.put("liquidGlassTheme", liquidGlassTheme)
settingsObj.put("materialYou", materialYou)
settingsObj.put("accentColor", accentColor)
settingsObj.put("expandedTheme", expandedTheme)
settingsObj.put("islandFont", islandFont)
settingsObj.put("enableNotchMode", enableNotchMode)
settingsObj.put("cameraCutoutManual", cameraCutoutManual)
settingsObj.put("cameraCutoutStyle", cameraCutoutStyle)
settingsObj.put("cameraCutoutX", cameraCutoutX.toDouble())
settingsObj.put("cameraCutoutY", cameraCutoutY.toDouble())
settingsObj.put("cameraCutoutWidth", cameraCutoutWidth.toDouble())
settingsObj.put("cameraCutoutHeight", cameraCutoutHeight.toDouble())
}

internal fun SmartIslandSettings.putGestures(settingsObj: JSONObject) {
settingsObj.put("enableSwipeActions", enableSwipeActions)
settingsObj.put("swipeUpAction", swipeUpAction)
settingsObj.put("swipeHoldUpAction", swipeHoldUpAction)
settingsObj.put("swipeDownAction", swipeDownAction)
settingsObj.put("swipeDownCollapsedAction", swipeDownCollapsedAction)
settingsObj.put("swipeLeftAction", swipeLeftAction)
settingsObj.put("swipeRightAction", swipeRightAction)
settingsObj.put("swipeHorizontalCollapsedAction", swipeHorizontalCollapsedAction)
settingsObj.put("enablePillSwipeActions", enablePillSwipeActions)
settingsObj.put("pillSwipeUpAction", pillSwipeUpAction)
settingsObj.put("pillSwipeDownAction", pillSwipeDownAction)
settingsObj.put("pillSwipeLeftAction", pillSwipeLeftAction)
settingsObj.put("pillSwipeRightAction", pillSwipeRightAction)
settingsObj.put("longPressAction", longPressAction)
}

internal fun SmartIslandSettings.putLandscape(settingsObj: JSONObject) {
settingsObj.put("circlePosition", circlePosition)
settingsObj.put("landscapeShape", landscapeShape)
settingsObj.put("landscapeWidth", landscapeWidth.toDouble())
settingsObj.put("landscapeHeight", landscapeHeight.toDouble())
settingsObj.put("landscapeXOffset", landscapeXOffset.toDouble())
settingsObj.put("landscapeYOffset", landscapeYOffset.toDouble())
settingsObj.put("landscapeCornerRadius", landscapeCornerRadius.toDouble())
settingsObj.put("landscapeOpacity", landscapeOpacity.toDouble())
settingsObj.put("landscapeCircleSize", landscapeCircleSize.toDouble())
settingsObj.put("landscapeCircleOpacity", landscapeCircleOpacity.toDouble())
settingsObj.put("landscapeCircleEdgeInset", landscapeCircleEdgeInset.toDouble())
settingsObj.put("landscapeCircleYOffset", landscapeCircleYOffset.toDouble())
settingsObj.put("landscapeCirclePosition", landscapeCirclePosition)
settingsObj.put("landscapeCircleXAdjust", landscapeCircleXAdjust.toDouble())
settingsObj.put("landscapeCircleYAdjust", landscapeCircleYAdjust.toDouble())
settingsObj.put("landscapePillPosition", landscapePillPosition)
settingsObj.put("landscapeLayoutStyle", landscapeLayoutStyle)
settingsObj.put("edgePill", edgePill.coerced().encode())

val disabledEventsLandscapeArray = JSONArray()
disabledEventsLandscape.forEach { disabledEventsLandscapeArray.put(it) }
settingsObj.put("disabledEventsLandscape", disabledEventsLandscapeArray)
}

internal fun SmartIslandSettings.putTimingsAndIdleSwitches(settingsObj: JSONObject) {
settingsObj.put("idleTimeoutMs", idleTimeoutMs)
settingsObj.put("idleTimeoutNever", idleTimeoutNever)
settingsObj.put("batteryWarningMs", batteryWarningMs)
settingsObj.put("pulseHoldPercent", pulseHoldPercent)
settingsObj.put("secondaryMergeMs", secondaryMergeMs)
settingsObj.put("transferExpiryMinutes", transferExpiryMinutes)
settingsObj.put("idleWidgetsEnabled", idleWidgetsEnabled)
settingsObj.put("idleWidgetsEnabledLandscape", idleWidgetsEnabledLandscape)
settingsObj.put("idleOffAfterTapClose", idleOffAfterTapClose)
settingsObj.put("idleOffAfterTapCloseLandscape", idleOffAfterTapCloseLandscape)
settingsObj.put("idleOffAfterLongPress", idleOffAfterLongPress)
settingsObj.put("idleOffAfterLongPressLandscape", idleOffAfterLongPressLandscape)
settingsObj.put("idleOffWhenStored", idleOffWhenStored)
settingsObj.put("contentPillSameAsIdle", contentPillSameAsIdle)
settingsObj.put("islandExemptApps", JSONArray().also { a -> islandExemptApps.forEach { a.put(it) } })
settingsObj.put("islandExemptAppsLandscape", JSONArray().also { a -> islandExemptAppsLandscape.forEach { a.put(it) } })
settingsObj.put("islandExemptAppsStatic", JSONArray().also { a -> islandExemptAppsStatic.forEach { a.put(it) } })
settingsObj.put("contentPillSameAsIdleLandscape", contentPillSameAsIdleLandscape)
settingsObj.put("contentPillWidthPercentLandscape", contentPillWidthPercentLandscape.toDouble())
settingsObj.put("contentPillWidthPercent", contentPillWidthPercent.toDouble())
settingsObj.put("idleOffWhenStoredLandscape", idleOffWhenStoredLandscape)
}

internal fun SmartIslandSettings.putIdleWidgets(settingsObj: JSONObject) {
settingsObj.put("idleGlyphTickSeconds", idleGlyphTickSeconds.toDouble())
settingsObj.put("idleWidgetIds", idleWidgetIds)
settingsObj.put("idleWidgetCycleSeconds", idleWidgetCycleSeconds)
settingsObj.put("widgetIslandCloseSeconds", widgetIslandCloseSeconds)
settingsObj.put("idleAlarmHideBeyond12h", idleAlarmHideBeyond12h)
settingsObj.put("batteryOnlyWhenCharging", batteryOnlyWhenCharging)
settingsObj.put("idleBeatOnlyOnChange", idleBeatOnlyOnChange)
settingsObj.put("idleWidgetTempUnit", idleWidgetTempUnit)
settingsObj.put("idleWidgetWindUnit", idleWidgetWindUnit)
}

internal fun SmartIslandSettings.putWeatherAndNetSpeed(settingsObj: JSONObject) {
settingsObj.put("weatherLocationSource", weatherLocationSource)
settingsObj.put("weatherManualCity", weatherManualCity)
settingsObj.put("weatherManualLat", weatherManualLat.toDouble())
settingsObj.put("weatherManualLon", weatherManualLon.toDouble())
settingsObj.put("weatherRefreshMinutes", weatherRefreshMinutes)
settingsObj.put("weatherProvider", weatherProvider)
settingsObj.put("weatherCustomUrl", weatherCustomUrl)
// weatherCustomKey is deliberately not written
settingsObj.put("weatherPillMode", weatherPillMode)
settingsObj.put("weatherIslandReadouts", weatherIslandReadouts)
settingsObj.put("weatherIconAnimated", weatherIconAnimated)
settingsObj.put("weatherIconPauseOnSaver", weatherIconPauseOnSaver)
settingsObj.put("weatherShowRule", weatherShowRule)
settingsObj.put("weatherNotableTriggers", weatherNotableTriggers)
settingsObj.put("weatherHotC", weatherHotC)
settingsObj.put("weatherColdC", weatherColdC)
settingsObj.put("weatherHideAtNight", weatherHideAtNight)
settingsObj.put("weatherNightStartHour", weatherNightStartHour)
settingsObj.put("weatherNightEndHour", weatherNightEndHour)
settingsObj.put("netSpeedThresholdKbps", netSpeedThresholdKbps)
settingsObj.put("netSpeedConnection", netSpeedConnection)
}

internal fun SmartIslandSettings.putIdleScheduleAndLedTick(settingsObj: JSONObject) {
settingsObj.put("idleScheduleWorkEnabled", idleScheduleWorkEnabled)
settingsObj.put("idleScheduleWorkHidden", idleScheduleWorkHidden)
settingsObj.put("idleScheduleWorkStartHour", idleScheduleWorkStartHour)
settingsObj.put("idleScheduleWorkEndHour", idleScheduleWorkEndHour)
settingsObj.put("idleScheduleWorkWeekdaysOnly", idleScheduleWorkWeekdaysOnly)
settingsObj.put("idleScheduleNightEnabled", idleScheduleNightEnabled)
settingsObj.put("idleScheduleNightHidden", idleScheduleNightHidden)
settingsObj.put("idleScheduleNightStartHour", idleScheduleNightStartHour)
settingsObj.put("idleScheduleNightEndHour", idleScheduleNightEndHour)
settingsObj.put("idleScheduleChargingEnabled", idleScheduleChargingEnabled)
settingsObj.put("idleScheduleChargingHidden", idleScheduleChargingHidden)
settingsObj.put("idleWidgetPinnedFirst", idleWidgetPinnedFirst)
settingsObj.put("idleTickStyle", idleTickStyle)
settingsObj.put("ledTickPattern", ledTickPattern)
settingsObj.put("ledTickPaceMs", ledTickPaceMs)
settingsObj.put("ledTickFont", ledTickFont)
settingsObj.put("ledTickColor", ledTickColor)
settingsObj.put("ledTickStagger", ledTickStagger)
settingsObj.put("idleGlyphFont", idleGlyphFont)
settingsObj.put("fontMixEnabled", fontMixEnabled)
settingsObj.put("fontMixLight", fontMixLight)
settingsObj.put("fontMixRegular", fontMixRegular)
settingsObj.put("fontMixMedium", fontMixMedium)
settingsObj.put("idleGlyphScale", idleGlyphScale)
}

internal fun SmartIslandSettings.putMotionAndInput(settingsObj: JSONObject) {
settingsObj.put("widgetIslandBatteryMetric", widgetIslandBatteryMetric)
settingsObj.put("animSpeedPortrait", animSpeedPortrait)
settingsObj.put("animSpeedLandscape", animSpeedLandscape)
settingsObj.put("mergeStyle", mergeStyle)
settingsObj.put("mergeStyleLandscape", mergeStyleLandscape)
settingsObj.put("showDuringDnd", showDuringDnd)
settingsObj.put("hapticStrength", hapticStrength)
settingsObj.put("gestureSensitivity", gestureSensitivity)
settingsObj.put("floatingWindowEnabled", floatingWindowEnabled)
settingsObj.put("floatingWindowMode", floatingWindowMode)
}

internal fun SmartIslandSettings.putAutoExpand(settingsObj: JSONObject) {
val noExpandArray = JSONArray()
noExpandPackages.forEach { noExpandArray.put(it) }
settingsObj.put("noExpandPackages", noExpandArray)

val noAutoExpandArray = JSONArray()
noAutoExpandPackages.forEach { noAutoExpandArray.put(it) }
settingsObj.put("noAutoExpandPackages", noAutoExpandArray)
val autoExpandArray = JSONArray()
autoExpandPackages.forEach { autoExpandArray.put(it) }
settingsObj.put("autoExpandPackages", autoExpandArray)
val noAutoExpandEventsArray = JSONArray()
noAutoExpandEvents.forEach { noAutoExpandEventsArray.put(it) }
settingsObj.put("noAutoExpandEvents", noAutoExpandEventsArray)
val noAutoExpandEventsLandscapeArray = JSONArray()
noAutoExpandEventsLandscape.forEach { noAutoExpandEventsLandscapeArray.put(it) }
settingsObj.put("noAutoExpandEventsLandscape", noAutoExpandEventsLandscapeArray)
settingsObj.put("hideAllOnLockScreen", hideAllOnLockScreen)
val hideOnLockArray = JSONArray()
hideOnLockScreenEvents.forEach { hideOnLockArray.put(it) }
settingsObj.put("hideOnLockScreenEvents", hideOnLockArray)
settingsObj.put("notificationsEnabled", notificationsEnabled)
val hideOnLockPackagesArray = JSONArray()
hideOnLockScreenPackages.forEach { hideOnLockPackagesArray.put(it) }
settingsObj.put("hideOnLockScreenPackages", hideOnLockPackagesArray)
}
