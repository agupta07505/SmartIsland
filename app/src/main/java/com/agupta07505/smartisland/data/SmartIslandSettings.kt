/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.EdgePillTuning
import com.agupta07505.smartisland.model.EventColors
import com.agupta07505.smartisland.model.FloatingWindowMode
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSchedulePrefs
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.NetSpeedRules
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPill
import com.agupta07505.smartisland.model.WeatherPrefs
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRules
import com.agupta07505.smartisland.model.WeatherRulesPrefs
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.WidgetIslandTimer
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.CustomFonts
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.StatusBarPlan
import com.agupta07505.smartisland.util.quietly
import org.json.JSONObject

data class ModeColors(
    val batteryColor: Long = 0xFF10B981L,
    val notificationDotColor: Long = 0xFF2563EBL,
    val musicVisualizerColor: Long = 0xFFFF6B9AL,
    val hotspotColor: Long = 0xFFF59E0BL,
    val callColor: Long = 0xFF22C55EL,
    val liveActivityColor: Long = 0xFF8B5CF6L,
    val transferColor: Long = 0xFF06B6D4L,
    val navigationColor: Long = 0xFF10B981L,
    val bluetoothColor: Long = 0xFF2563EBL,
    val flashlightColor: Long = 0xFFF59E0BL,
    val screenRecordingColor: Long = 0xFFEF4444L,
    val timerColor: Long = 0xFFF59E0BL,
    val stopwatchColor: Long = 0xFF06B6D4L
)

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f,
    val opacity: Float = 1f,
    val pillColor: Long = 0xFF000000L,
    val oledBlack: Boolean = false,
    val modeColors: ModeColors = ModeColors(),
    val enableAppShortcuts: Boolean = true,
    val shortcutPackages: List<String> = emptyList(),
    val showRecentApps: Boolean = false,
    val utilityLayout: String = "",
    val utilityCalculatorPackage: String = "",
    val utilityCalculatorFloating: Boolean = true,
    val welcomeDialogShown: Boolean = false,
    val torchStrobeAcknowledged: Boolean = false,
    val showOnLockScreen: Boolean = true,
    /** Live events whose card stays on the island while its own app is in front, see [IslandEvent.FOREGROUND_SWITCHABLE]. */
    val showInForegroundEvents: Set<String> = IslandEvent.DEFAULT_SHOW_IN_FOREGROUND_IDS,
    /** Hides the island while a fullscreen app has no status bar (portrait, landscape with the edge pill). */
    val hideInFullscreen: Boolean = true,
    val hideInShade: Boolean = true,
    val lockScreenPrivacy: String = "AppIconOnly",
    val showNotificationActions: Boolean = true,
    val hideFromNotificationShade: Boolean = false,
    val liveActivitiesEnabled: Boolean = true,
    val navigationEnabled: Boolean = true,
    val disabledNotificationPackages: Set<String> = emptySet(),
    val disabledSoundPackages: Set<String> = emptySet(),
    val disabledEvents: Set<String> = emptySet(),
    val hideWhenIdle: Boolean = false,
    val autoHidePill: Boolean = false,
    val autoHideTimeoutSeconds: Int = 5,
    val showInLandscape: Boolean = true,
    val autoExpandOnNotification: Boolean = true,
    val autoExpandOnLockScreen: Boolean = true,
    val showSuggestedReplies: Boolean = true,
    val showCopyCodeChip: Boolean = true,
    val enableShadow: Boolean = true,
    val expandedBlurEnabled: Boolean = true,
    val expandedBlurRadius: Float = 24f,
    val mediaBlurEnabled: Boolean = false,
    val mediaBlurRadius: Float = 24f,
    val mediaOpacity: Float = 1f,
    val expandedGradientEnabled: Boolean = false,
    val expandedGradientColor: Long = 0xFF2563EBL,
    val expandedGradientStrength: Float = 0.6f,
    val shadowElevation: Float = 14f,
    val enableMusicArtworkBackground: Boolean = true,
    val enableNotificationBackdrop: Boolean = true,
    val deviceType: String = "AUTO",
    val allowNetworkChecks: Boolean = true,
    val detectTransfersBySpeed: Boolean = true,
    val enableNotificationHistory: Boolean = false,
    val notificationHistoryRetentionHours: Int = 72,
    val showBluetoothBattery: Boolean = true,
    val enableBatteryMode: Boolean = true,
    val enableNotificationCooldown: Boolean = false,
    val notificationCooldownDurationMinutes: Int = 3,
    val notificationCooldownThreshold: Int = 3,
    val notificationCooldownExcludedPackages: Set<String> = emptySet(),
    val notificationCooldownSystemEvents: Set<String> = emptySet(),
    val developerModeEnabled: Boolean = true,
    val recordLogs: Boolean = false,
    val faceUnlockMode: String = "face_only",
    val faceIdStyle: String = "square_tick",
    val appFaceIdEnabled: Boolean = true,
    val appFaceIdStyle: String = "square_tick",
    val statusBarHidden: Boolean = false,
    /** True if the hide is reapplied after unlock, screen on and boot. */
    val statusBarHidePersistent: Boolean = true,
    val statusBarHideNotificationIcons: Boolean = true,
    val statusBarHideClock: Boolean = true,
    val statusBarHideAllSystemIcons: Boolean = true,
    val statusBarHiddenSystemIcons: Set<String> = emptySet(),
    val fingerprintAnimationEnabled: Boolean = true,
    /** App theme: "system" (follow the phone), "light" or "dark". */
    val themeMode: String = "system",
    val liquidGlassTheme: Boolean = true,
    val materialYou: Boolean = false,
    val accentColor: Long = DEFAULT_ACCENT_COLOR,
    val expandedTheme: String = EXPANDED_THEME_DARK,
    val islandFont: String = FONT_INTER_DISPLAY,
    val fontMixEnabled: Boolean = false,
    /** Font for normal weight and lighter text; empty = the island font. */
    val fontMixLight: String = "",
    /** Font for medium and semi-bold text; empty = the island font. */
    val fontMixRegular: String = "",
    /** Font for bold and heavier text; empty = the island font. */
    val fontMixMedium: String = "",
    val enableNotchMode: Boolean = false,
    /** Manual camera cutout area, for phones where the hardware detection fails. */
    val cameraCutoutManual: Boolean = false,
    /** Camera hole anchor; auto follows detection, the others override it. */
    val cameraCutoutStyle: String = CUTOUT_STYLE_AUTO,
    /** Cutout centre, dp from the style's anchor (the screen centre for auto and center). */
    val cameraCutoutX: Float = 0f,
    /** Manual cutout top edge, dp from the top of the screen. */
    val cameraCutoutY: Float = 12f,
    val cameraCutoutWidth: Float = 40f,
    val cameraCutoutHeight: Float = 24f,
    val enableSwipeActions: Boolean = true,
    val swipeUpAction: String = "DismissCurrent",
    val swipeHoldUpAction: String = "DismissAll",
    val swipeDownAction: String = "None",
    val swipeDownCollapsedAction: String = "Expand",
    val swipeLeftAction: String = "NextEvent",
    val swipeRightAction: String = "PreviousEvent",
    val swipeHorizontalCollapsedAction: String = "NextPrevious",
    val enablePillSwipeActions: Boolean = true,
    val pillSwipeUpAction: String = "DismissCurrent",
    val pillSwipeDownAction: String = "Expand",
    val pillSwipeLeftAction: String = "NextEvent",
    val pillSwipeRightAction: String = "PreviousEvent",
    val longPressAction: String = "TogglePill",
    val circlePosition: String = CIRCLE_POSITION_RIGHT,
    /** Portrait collapsed cutout with content (a card on the pill). */
    val contentPillSameAsIdle: Boolean = false,
    val contentPillWidthPercent: Float = DEFAULT_CONTENT_PILL_PERCENT,
    val contentPillSameAsIdleLandscape: Boolean = true,
    val islandExemptApps: Set<String> = emptySet(),
    val islandExemptAppsLandscape: Set<String> = emptySet(),
    val islandExemptAppsStatic: Set<String> = emptySet(),
    val contentPillWidthPercentLandscape: Float = DEFAULT_CONTENT_PILL_PERCENT,
    // legacy: kept so old backups still load
    val landscapeShape: String = LANDSCAPE_SHAPE_PILL,
    val landscapeWidth: Float = 112f,
    val landscapeHeight: Float = 34f,
    val landscapeXOffset: Float = 0f,
    val landscapeYOffset: Float = 12f,
    val landscapeCornerRadius: Float = 22f,
    val landscapeOpacity: Float = 1f,
    val landscapeCircleSize: Float = 26f,
    val landscapeCircleOpacity: Float = 0f,
    val landscapeCircleEdgeInset: Float = 16f,
    val landscapeCircleYOffset: Float = 12f,
    val landscapeCirclePosition: String = LANDSCAPE_POSITION_AUTO,
    val landscapeCircleXAdjust: Float = 0f,
    val landscapeCircleYAdjust: Float = 0f,
    val landscapePillPosition: String = LANDSCAPE_POSITION_MIDDLE,
    val landscapeLayoutStyle: String = LandscapeLayoutStyle.DEFAULT,
    val edgePill: EdgePillTuning = EdgePillTuning(),
    val disabledEventsLandscape: Set<String> = emptySet(),
    val foldDisabledSomewhere: Set<String> = emptySet(),
    val idleTimeoutMs: Int = IslandTimings.STATIC_IDLE_TIMEOUT_MS.toInt(),
    val idleTimeoutNever: Boolean = false,
    val batteryWarningMs: Int = IslandTimings.BATTERY_WARNING_CARD_MS.toInt(),
    /** Multiplier on every pulse hold of the collapsed pill, in percent (100 = as designed). */
    val pulseHoldPercent: Int = DEFAULT_PULSE_HOLD_PERCENT,
    val secondaryMergeMs: Int = IslandTimings.SECONDARY_IDLE_MS.toInt(),
    val transferExpiryMinutes: Int = DEFAULT_TRANSFER_EXPIRY_MINUTES,
    val idleWidgetsEnabled: Boolean = false,
    val idleWidgetsEnabledLandscape: Boolean = false,
    val idleOffAfterTapClose: Boolean = false,
    val idleOffAfterTapCloseLandscape: Boolean = false,
    /** Idle widgets go off while a long press has the pill hidden, per orientation. */
    val idleOffAfterLongPress: Boolean = false,
    val idleOffAfterLongPressLandscape: Boolean = false,
    val idleOffWhenStored: Boolean = false,
    val idleOffWhenStoredLandscape: Boolean = false,
    val idleGlyphTickSeconds: Float = IdleSlots.DEFAULT_TICK_SECONDS,
    val idleWidgetIds: String = IdleWidgets.DEFAULT_IDS,
    val idleWidgetCycleSeconds: Int = IdleWidgets.DEFAULT_CYCLE_SECONDS,
    val widgetIslandCloseSeconds: Int = WidgetIslandTimer.DEFAULT_SECONDS,
    /** What the charging battery island shows besides percent. */
    val widgetIslandBatteryMetric: String = WidgetIslandContent.BATTERY_WATT,
    val idleAlarmHideBeyond12h: Boolean = false,
    val batteryOnlyWhenCharging: Boolean = false,
    val idleBeatOnlyOnChange: Boolean = false,
    val idleGlyphFont: String = "",
    /** Size of the idle glyphs in percent of the normal size (see model/IdleSlots). */
    val idleGlyphScale: Int = IdleSlots.DEFAULT_GLYPH_SCALE,
    /** AUTO (device region), C or F */
    val idleWidgetTempUnit: String = IdleWidgets.UNIT_AUTO,
    val idleWidgetWindUnit: String = IdleWidgets.UNIT_AUTO,
    val weatherLocationSource: String = WeatherConfig.SOURCE_AUTO,
    val weatherManualCity: String = "",
    val weatherManualLat: Float = 0f,
    val weatherManualLon: Float = 0f,
    val weatherRefreshMinutes: Int = WeatherConfig.DEFAULT_REFRESH_MINUTES,
    val weatherProvider: String = WeatherConfig.PROVIDER_OPEN_METEO,
    val weatherCustomUrl: String = "",
    val weatherCustomKey: String = "",
    val weatherPillMode: String = WeatherPill.TEMP_ICON,
    val weatherIslandReadouts: String = "",
    val weatherIconAnimated: Boolean = true,
    val weatherIconPauseOnSaver: Boolean = true,
    val weatherShowRule: String = WeatherRules.MODE_ALWAYS,
    val weatherNotableTriggers: String = WeatherRules.DEFAULT_TRIGGERS,
    /** Notable when at or above this temperature (Celsius). */
    val weatherHotC: Int = WeatherRules.DEFAULT_HOT_C,
    /** Notable when at or below this temperature (Celsius). */
    val weatherColdC: Int = WeatherRules.DEFAULT_COLD_C,
    val weatherHideAtNight: Boolean = false,
    val weatherNightStartHour: Int = WeatherRules.DEFAULT_NIGHT_START,
    val weatherNightEndHour: Int = WeatherRules.DEFAULT_NIGHT_END,
    val netSpeedThresholdKbps: Int = NetSpeedRules.DEFAULT_KBPS,
    val netSpeedConnection: String = NetSpeedRules.CONN_ANY,
    /** Per-widget schedules: which widgets rotate during work hours, at night. */
    val idleScheduleWorkEnabled: Boolean = false,
    val idleScheduleWorkHidden: String = "",
    val idleScheduleWorkStartHour: Int = IdleSchedule.DEFAULT_WORK_START,
    val idleScheduleWorkEndHour: Int = IdleSchedule.DEFAULT_WORK_END,
    val idleScheduleWorkWeekdaysOnly: Boolean = true,
    val idleScheduleNightEnabled: Boolean = false,
    val idleScheduleNightHidden: String = "",
    val idleScheduleNightStartHour: Int = IdleSchedule.DEFAULT_NIGHT_START,
    val idleScheduleNightEndHour: Int = IdleSchedule.DEFAULT_NIGHT_END,
    val idleScheduleChargingEnabled: Boolean = false,
    val idleScheduleChargingHidden: String = "",
    /** Widget that always leads the rotation (a widget id); empty = none. */
    val idleWidgetPinnedFirst: String = "",
    val idleTickStyle: String = LedTick.STYLE_SMOOTH,
    /** Hops per change for the LED style, repeated in turn. */
    val ledTickPattern: String = LedTick.DEFAULT_PATTERN,
    val ledTickPaceMs: Int = LedTick.DEFAULT_PACE_MS,
    /** Font of the LED text: empty = the built-in dot font. */
    val ledTickFont: String = LedTick.FONT_BUILTIN,
    /** Colour of the LEDs: red, amber, green or white. */
    val ledTickColor: String = LedTick.COLOR_RED,
    val ledTickStagger: Boolean = true,
    val animSpeedPortrait: String = AnimationSpeed.ID_RELAXED,
    val animSpeedLandscape: String = AnimationSpeed.ID_RELAXED,
    /** Merge style of the secondary round cutout into the primary one: "slide" or "bubble". */
    val mergeStyle: String = MergeStyle.ID_SLIDE,
    val mergeStyleLandscape: String = MergeStyle.ID_SLIDE,
    /** Keep showing cards while Do Not Disturb hides notifications from the shade. */
    val showDuringDnd: Boolean = false,
    val hapticStrength: String = HapticStrength.ID_NORMAL,
    val floatingWindowEnabled: Boolean = true,
    /** [com.agupta07505.smartisland.model.FloatingWindowMode] id */
    val floatingWindowMode: String = FloatingWindowMode.ID_SYSTEM,
    val gestureSensitivity: String = GestureSensitivity.ID_NORMAL,
    // Deny sets: an app in no set gets the default
    val noExpandPackages: Set<String> = emptySet(),
    val noAutoExpandPackages: Set<String> = emptySet(),
    val autoExpandPackages: Set<String> = emptySet(),
    val noAutoExpandEvents: Set<String> = IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS,
    /** Landscape twin of [noAutoExpandEvents], the overlay reads 1 set and [forOrientation] swaps it in. */
    val noAutoExpandEventsLandscape: Set<String> = IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS,
    /**
     * The colour the user gave an event of its own ([IslandEvent.id] to ARGB). An event that is not in it keeps the
     * colour it always had: its group's colour above, or its fixed one
     */
    val eventColors: Map<String, Long> = emptyMap(),
    /**
     * Never stored. Set only on the copy a card is drawn with ([forCard]) when the card's event has a colour of its
     * own, so the parts that hold a fixed colour of their own (a low battery's red, USB's magenta) take it too
     */
    val cardEventColor: Long? = null,
    val hideAllOnLockScreen: Boolean = false,
    val hideOnLockScreenEvents: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = true,
    val hideOnLockScreenPackages: Set<String> = emptySet(),
    /** Set by forOrientation when the display uses the camera dot. Never stored. */
    val idleCutout: Boolean = false
) {
    val batteryColor: Long get() = modeColors.batteryColor
    val notificationDotColor: Long get() = modeColors.notificationDotColor
    val musicVisualizerColor: Long get() = modeColors.musicVisualizerColor
    val hotspotColor: Long get() = modeColors.hotspotColor
    val callColor: Long get() = modeColors.callColor
    val liveActivityColor: Long get() = modeColors.liveActivityColor
    val transferColor: Long get() = modeColors.transferColor
    val navigationColor: Long get() = modeColors.navigationColor
    val bluetoothColor: Long get() = modeColors.bluetoothColor
    val flashlightColor: Long get() = modeColors.flashlightColor
    val screenRecordingColor: Long get() = modeColors.screenRecordingColor
    val timerColor: Long get() = modeColors.timerColor
    val stopwatchColor: Long get() = modeColors.stopwatchColor

    fun animationSpeed(isLandscape: Boolean): AnimationSpeed =
        AnimationSpeed.fromId(if (isLandscape) animSpeedLandscape else animSpeedPortrait)

    fun mergeStyleKind(): MergeStyle = MergeStyle.fromId(mergeStyle)

    fun mergeStyleKind(landscape: Boolean): MergeStyle =
        MergeStyle.fromId(if (landscape) mergeStyleLandscape else mergeStyle)

    fun statusBarPlan(): StatusBarPlan = StatusBarPlan(
        notificationIcons = statusBarHideNotificationIcons,
        clock = statusBarHideClock,
        allSystemIcons = statusBarHideAllSystemIcons,
        systemIcons = statusBarHiddenSystemIcons
    )

    fun keepsCardInForeground(card: IslandNotification): Boolean =
        IslandEvent.forNotification(card)?.id in showInForegroundEvents

    fun isEventEnabled(eventId: String): Boolean = eventId !in disabledEvents && !notificationsSwitchedOff(eventId)

    private fun notificationsSwitchedOff(eventId: String): Boolean =
        !notificationsEnabled && eventId == IslandEvent.NOTIFICATION.id

    /** Event must stay out of the pill while the phone is locked. */
    fun isAppHiddenOnLockScreen(packageName: String, mode: IslandMode): Boolean =
        mode != IslandMode.IncomingCall && packageName in hideOnLockScreenPackages

    fun isEventHiddenOnLockScreen(eventId: String): Boolean {
        if (eventId == IslandEvent.CALL.id) return false
        return hideAllOnLockScreen || eventId in hideOnLockScreenEvents
    }

    fun isEventAutoExpand(eventId: String): Boolean = eventId !in noAutoExpandEvents

    /**
     * The colour of the event of [notification]: the one the user gave it, else the one it starts with
     * ([EventColors.defaultFor]), null when it has neither (it then keeps its group's colour)
     */
    fun eventColorFor(notification: IslandNotification?): Long? {
        if (notification == null) return null
        // the events that start with a colour are all of these two modes, every other card pays nothing here
        val mayStartColored = notification.mode == IslandMode.Bluetooth || notification.mode == IslandMode.DownloadUpload
        if (eventColors.isEmpty() && !mayStartColored) return null
        val event = IslandEvent.forNotification(notification) ?: return null
        return eventColors[event.id] ?: EventColors.defaultFor(event)
    }

    /**
     * These settings as 1 card of [notification] draws with them: when its event has a colour of its own, the colour
     * its group has is that one and [cardEventColor] is set. Without one this is the same object, so a card pays
     * nothing for the feature
     */
    fun forCard(notification: IslandNotification?): SmartIslandSettings {
        val color = eventColorFor(notification) ?: return this
        val colors = when (notification!!.mode) {
            IslandMode.Battery -> modeColors.copy(batteryColor = color)
            IslandMode.IncomingCall, IslandMode.CallEnded -> modeColors.copy(callColor = color)
            IslandMode.Music -> modeColors.copy(musicVisualizerColor = color)
            IslandMode.LiveActivity -> modeColors.copy(liveActivityColor = color)
            IslandMode.Navigation -> modeColors.copy(navigationColor = color)
            IslandMode.DownloadUpload -> modeColors.copy(transferColor = color)
            IslandMode.Hotspot -> modeColors.copy(hotspotColor = color)
            IslandMode.Bluetooth -> modeColors.copy(bluetoothColor = color)
            IslandMode.Flashlight -> modeColors.copy(flashlightColor = color)
            IslandMode.ScreenRecording, IslandMode.AudioRecording -> modeColors.copy(screenRecordingColor = color)
            IslandMode.Timer -> modeColors.copy(timerColor = color)
            IslandMode.Stopwatch -> modeColors.copy(stopwatchColor = color)
            IslandMode.Notification -> modeColors.copy(notificationDotColor = color)
            IslandMode.Empty -> modeColors
        }
        return copy(modeColors = colors, cardEventColor = color)
    }

    fun isLandscapeEventAutoExpand(eventId: String): Boolean = eventId !in noAutoExpandEventsLandscape

    fun isLandscapeEventEnabled(eventId: String): Boolean =
        eventId !in disabledEventsLandscape && !notificationsSwitchedOff(eventId)

    /** No profile that can be shown would display the event. */
    fun isEventDisabledEverywhere(eventId: String): Boolean =
        notificationsSwitchedOff(eventId) ||
            (eventId in disabledEvents && (!showInLandscape || eventId in disabledEventsLandscape))

    /** At least 1 displayable profile has the event switched off. */
    fun isEventDisabledSomewhere(eventId: String): Boolean =
        notificationsSwitchedOff(eventId) ||
            eventId in disabledEvents || (showInLandscape && eventId in disabledEventsLandscape) ||
            eventId in foldDisabledSomewhere

    val islandSurfaceColor: Long get() = if (oledBlack) 0xFF000000L else pillColor

    fun toJson(appVersion: String = ""): String = toBackupJson(appVersion)

    data class BackupMetadata(
        val formatVersion: Int,
        val appVersion: String,
        val timestamp: Long,
        val settingsCount: Int
    )

    val expandedBlurMode: ExpandedBlurMode
        get() = if (expandedBlurEnabled) ExpandedBlurMode.Card else ExpandedBlurMode.Off

    fun forMedia(): SmartIslandSettings = copy(
        opacity = mediaOpacity,
        expandedBlurEnabled = mediaBlurEnabled,
        expandedBlurRadius = mediaBlurRadius
    )

    fun forCard(media: Boolean): SmartIslandSettings = if (media) forMedia() else this

    fun weatherRules() = WeatherRulesPrefs(
        mode = WeatherRules.normalizeMode(weatherShowRule),
        triggers = WeatherRules.normalizeTriggers(weatherNotableTriggers),
        hotC = WeatherRules.normalizeHotC(weatherHotC),
        coldC = WeatherRules.normalizeColdC(weatherColdC),
        hideAtNight = weatherHideAtNight,
        nightStartHour = WeatherRules.normalizeHour(weatherNightStartHour),
        nightEndHour = WeatherRules.normalizeHour(weatherNightEndHour)
    )

    fun idleSchedulePrefs() = IdleSchedulePrefs(
        workEnabled = idleScheduleWorkEnabled,
        workHidden = idleScheduleWorkHidden,
        workStartHour = idleScheduleWorkStartHour,
        workEndHour = idleScheduleWorkEndHour,
        workWeekdaysOnly = idleScheduleWorkWeekdaysOnly,
        nightEnabled = idleScheduleNightEnabled,
        nightHidden = idleScheduleNightHidden,
        nightStartHour = idleScheduleNightStartHour,
        nightEndHour = idleScheduleNightEndHour,
        chargingEnabled = idleScheduleChargingEnabled,
        chargingHidden = idleScheduleChargingHidden,
        pinnedFirst = idleWidgetPinnedFirst
    )

    fun weatherPrefs() = WeatherPrefs(
        source = weatherLocationSource,
        manualCity = weatherManualCity,
        manualLat = weatherManualLat,
        manualLon = weatherManualLon,
        refreshMinutes = weatherRefreshMinutes,
        provider = weatherProvider,
        customUrl = weatherCustomUrl,
        customKey = weatherCustomKey,
        pillMode = WeatherPill.normalize(weatherPillMode),
        readouts = WeatherReadouts.normalize(weatherIslandReadouts)
    )

    companion object {
        const val DEFAULT_ACCENT_COLOR = 0xFF4A8CFFL

        const val EXPANDED_THEME_DARK = "dark"
        const val EXPANDED_THEME_LIGHT = "light"
        const val EXPANDED_THEME_SYSTEM = "system"
        val VALID_EXPANDED_THEMES = setOf(EXPANDED_THEME_DARK, EXPANDED_THEME_LIGHT, EXPANDED_THEME_SYSTEM)
        const val FONT_SYSTEM = "system"
        const val FONT_SERIF = "serif"
        const val FONT_MONOSPACE = "monospace"
        const val FONT_LORA = "lora"
        const val FONT_INTER_DISPLAY = "inter_display"
        const val FONT_POPPINS = "poppins"
        val VALID_FONTS = setOf(FONT_SYSTEM, FONT_SERIF, FONT_MONOSPACE, FONT_LORA, FONT_INTER_DISPLAY, FONT_POPPINS)

        fun isValidFont(id: String): Boolean = id in VALID_FONTS || CustomFonts.fileNameOf(id) != null

        const val CIRCLE_POSITION_RIGHT = "right"
        const val CIRCLE_POSITION_LEFT = "left"

        const val LANDSCAPE_SHAPE_PILL = "pill"
        const val LANDSCAPE_SHAPE_CIRCLE = "circle"
        const val CUTOUT_STYLE_AUTO = "auto"
        const val CUTOUT_STYLE_LEFT = "left"
        const val CUTOUT_STYLE_CENTER = "center"
        const val CUTOUT_STYLE_RIGHT = "right"
        const val CUTOUT_STYLE_TEARDROP = "teardrop"
        const val LANDSCAPE_POSITION_AUTO = "auto"
        const val LANDSCAPE_POSITION_LEFT = "left"
        const val LANDSCAPE_POSITION_RIGHT = "right"
        const val LANDSCAPE_POSITION_MIDDLE = "middle"

        const val MIN_CIRCLE_SIZE = 16f
        const val MAX_CIRCLE_SIZE = 56f
        const val MIN_CIRCLE_EDGE_INSET = 0f
        const val MAX_CIRCLE_EDGE_INSET = 120f
        const val MAX_LANDSCAPE_Y_OFFSET = 240f
        const val MAX_LANDSCAPE_X_OFFSET = 700f

        val Default = SmartIslandSettings()

        /**
         * 2: the auto-expand sets have a new default. A backup below 2 restores with that default and
         * ignores the sets it holds.
         */
        const val BACKUP_FORMAT_VERSION = 2

        const val MIN_CONTENT_PILL_PERCENT = 100f
        const val MAX_CONTENT_PILL_PERCENT = 250f
        const val DEFAULT_CONTENT_PILL_PERCENT = 110f
        const val MIN_WIDTH = 76f
        const val MAX_WIDTH = 180f
        const val MIN_HEIGHT = 24f
        const val MAX_HEIGHT = 60f
        const val MIN_CUTOUT_WIDTH = 12f
        const val MAX_CUTOUT_WIDTH = 120f
        const val MIN_CUTOUT_HEIGHT = 12f
        const val MAX_CUTOUT_HEIGHT = 60f
        const val MIN_CUTOUT_X = -300f
        const val MAX_CUTOUT_X = 300f
        const val MIN_X_OFFSET = -300f
        const val MAX_X_OFFSET = 300f
        const val MIN_Y_OFFSET = 0f
        const val MAX_Y_OFFSET = 80f
        const val MIN_CORNER_RADIUS = 8f
        const val MAX_CORNER_RADIUS = 40f
        const val MIN_OPACITY = 0.2f
        /** Landscape idle circle may go all the way to invisible. */
        const val MIN_CIRCLE_OPACITY = 0f
        const val MIN_EXPANDED_BLUR = 4f
        const val MAX_EXPANDED_BLUR = 60f
        const val MIN_GRADIENT_STRENGTH = 0.1f
        const val MAX_GRADIENT_STRENGTH = 1f
        const val MAX_OPACITY = 1f
        const val MIN_SHADOW_ELEVATION = 0f
        const val MAX_SHADOW_ELEVATION = 32f

        const val MIN_IDLE_TIMEOUT_MS = 1_000
        const val MAX_IDLE_TIMEOUT_MS = 30_000
        /** Before the Never chip existed */
        const val LEGACY_IDLE_TIMEOUT_NEVER_MS = 30_500
        const val MIN_AUTO_HIDE_SECONDS = 1
        const val MAX_AUTO_HIDE_SECONDS = 60
        const val MIN_BATTERY_WARNING_MS = 1_000
        const val MAX_BATTERY_WARNING_MS = 15_000
        const val MIN_PULSE_HOLD_PERCENT = 75
        const val MAX_PULSE_HOLD_PERCENT = 200
        const val DEFAULT_PULSE_HOLD_PERCENT = 100
        const val MIN_SECONDARY_MERGE_MS = 500
        const val MAX_SECONDARY_MERGE_MS = 10_000
        const val MAX_TRANSFER_EXPIRY_MINUTES = 120
        const val DEFAULT_TRANSFER_EXPIRY_MINUTES = 15

        fun parseBackupMetadata(jsonString: String): BackupMetadata? {
            return quietly {
                val root = JSONObject(jsonString)
                val formatVersion = root.optInt("formatVersion", 1)
                val appVersion = root.optString("appVersion", "Unknown")
                val timestamp = root.optLong("exportTimestamp", System.currentTimeMillis())
                val settingsObj = root.optJSONObject("settings") ?: root
                BackupMetadata(
                    formatVersion = formatVersion,
                    appVersion = appVersion,
                    timestamp = timestamp,
                    settingsCount = settingsObj.length()
                )
            }
        }

        fun fromJson(jsonString: String): SmartIslandSettings = settingsFromJson(jsonString)
    }
}
enum class ExpandedBlurMode { Off, Card }
