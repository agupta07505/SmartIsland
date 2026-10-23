/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.util.CustomFonts
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.MergeStyle
import com.agupta07505.smartisland.util.GestureSensitivity
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandTimings
import org.json.JSONArray
import org.json.JSONObject

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f,
    val opacity: Float = 1f,
    val pillColor: Long = 0xFF000000L,
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
    val stopwatchColor: Long = 0xFF06B6D4L,
    val enableAppShortcuts: Boolean = true,
    /** Pinned quick-launch apps, in the order the island grid shows them. */
    val shortcutPackages: List<String> = emptyList(),
    val showRecentApps: Boolean = false,
    /** The Utilities page layout, in the form [com.agupta07505.smartisland.model.UtilityLayout] saves it. */
    val utilityLayout: String = "",
    /** The calculator app the Calculator utility opens; blank until it is chosen the first time it is tapped. */
    val utilityCalculatorPackage: String = "",
    /** Open that calculator in a floating window (when the floating window is on in the gesture settings). */
    val utilityCalculatorFloating: Boolean = true,
    val welcomeDialogShown: Boolean = false,
    val showOnLockScreen: Boolean = false,
    /**
     * Live cards (music, navigation, timers, recordings, downloads, live activities) of the app that is in the
     * foreground.
     */
    val showLiveEventsInForeground: Boolean = false,
    val lockScreenPrivacy: String = "AppIconOnly",
    val showNotificationActions: Boolean = true,
    val hideFromNotificationShade: Boolean = true,
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
    /** False: nothing opens the island by itself while the phone is locked (cards still show in the pill). */
    val autoExpandOnLockScreen: Boolean = true,
    val enableShadow: Boolean = true,
    val expandedBlurEnabled: Boolean = true,
    val expandedBlurRadius: Float = 24f,
    /**
     * The media (music) card has its own blur and opacity; old saves and backups start from the shared values above.
     */
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
    /** Show an estimated transfer card from device-wide network speed when no app notification exists. */
    val detectTransfersBySpeed: Boolean = true,
    val enableNotificationHistory: Boolean = false,
    val notificationHistoryRetentionHours: Int = 72,
    val showBluetoothBattery: Boolean = true,
    val enableBatteryMode: Boolean = true,
    val enableNotificationCooldown: Boolean = false,
    val notificationCooldownDurationMinutes: Int = 3,
    val notificationCooldownThreshold: Int = 3,
    val notificationCooldownExcludedPackages: Set<String> = emptySet(),
    /** Ids of system events (battery, Wi-Fi, VPN, Bluetooth, flashlight) whose pop-ups the anti-spam cooldown may hold back. */
    val notificationCooldownSystemEvents: Set<String> = emptySet(),
    val developerModeEnabled: Boolean = true,
    val recordLogs: Boolean = false,
    /** What starts the Face ID unlock animation's tick: "face_only" (the system's face-unlock success) or "any" (any unlock). */
    val faceUnlockMode: String = "face_only",
    /** How the Face ID unlock animation looks: "square" (opens below the cutout) or "cutout" (stays on the cutout). */
    val faceIdStyle: String = "square_tick",
    /**
     * Whether the Face ID animation also plays when an app's lock prompt (the phone's app lock) scans the face. Read
     * from the system log, so it needs Shizuku.
     */
    val appFaceIdEnabled: Boolean = true,
    /** How the app Face ID animation looks: "square_tick" (expanded), "square" (mix) or "cutout". Independent of [faceIdStyle]. */
    val appFaceIdStyle: String = "square_tick",
    /** Whether a fingerprint unlock plays the fingerprint scan animation (lock screen and app prompts). */
    val fingerprintAnimationEnabled: Boolean = true,
    /** App theme: "system" (follow the phone), "light" or "dark". */
    val themeMode: String = "system",
    /** App theme: the liquid glass look (translucent glass panels over a soft colour backdrop). Off gives the flat Material 3 look. */
    val liquidGlassTheme: Boolean = true,
    /** App theme: take the colours from the phone's wallpaper (Material You, Android 12+). Off uses [accentColor]. */
    val materialYou: Boolean = false,
    /** App theme: the accent colour (ARGB) the whole palette is built from when Material You is off. */
    val accentColor: Long = DEFAULT_ACCENT_COLOR,
    /** Look of the expanded island: "dark", "light", or "system" (follows the phone's light / dark setting). */
    val expandedTheme: String = EXPANDED_THEME_DARK,
    /** Font of the island's text: one of [VALID_FONTS] ("serif" is the phone's serif font) or a custom font id (`custom:<file name>`). */
    val islandFont: String = FONT_LORA,
    /** Mix fonts (see model/FontMix.kt): the island text uses up to three fonts, picked by the weight each piece of text asks for. */
    val fontMixEnabled: Boolean = false,
    /** Font for normal weight and lighter text; empty = the island font. A [VALID_FONTS] id or a custom font id. */
    val fontMixLight: String = "",
    /** Font for medium and semi-bold text; empty = the island font. */
    val fontMixRegular: String = "",
    /** Font for bold and heavier text; empty = the island font. */
    val fontMixMedium: String = "",
    val enableNotchMode: Boolean = false,
    /** Manual camera cutout area, for phones where the hardware detection fails. Used by the expanded island's close target. */
    val cameraCutoutManual: Boolean = false,
    /** Manual cutout centre, dp from the screen centre. */
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
    // Long press on the collapsed pill. One real action (hide / show the pill) or "None" to turn it off.
    val longPressAction: String = "TogglePill",
    val circlePosition: String = CIRCLE_POSITION_RIGHT,
    /**
     * Portrait collapsed cutout with content (a card on the pill): true keeps the idle cutout's size; false stretches
     * the pill to [contentPillWidthPercent] of its idle width when a card arrives and shrinks it back when the card
     * is put away.
     */
    val contentPillSameAsIdle: Boolean = false,
    val contentPillWidthPercent: Float = DEFAULT_CONTENT_PILL_PERCENT,
    /**
     * Landscape: the pill with a card on it against the pill that only carries the idle widgets (the landscape
     * width). On by default, so the pill the user tuned keeps its size.
     */
    val contentPillSameAsIdleLandscape: Boolean = true,
    /** Apps that switch the island totally off while they are in the foreground, per orientation (see model/IslandExemption). */
    val islandExemptApps: Set<String> = emptySet(),
    val islandExemptAppsLandscape: Set<String> = emptySet(),
    /** Portrait only: apps that keep the collapsed pill on screen but dead (no content, no popups, no touches) while in the foreground. */
    val islandExemptAppsStatic: Set<String> = emptySet(),
    val contentPillWidthPercentLandscape: Float = DEFAULT_CONTENT_PILL_PERCENT,
    // Legacy: kept so old backups still load.
    val landscapeShape: String = LANDSCAPE_SHAPE_PILL,
    val landscapeWidth: Float = 112f,
    val landscapeHeight: Float = 34f,
    val landscapeXOffset: Float = 0f,
    val landscapeYOffset: Float = 12f,
    val landscapeCornerRadius: Float = 22f,
    val landscapeOpacity: Float = 1f,
    val landscapeCircleSize: Float = 26f,
    /** Landscape idle circle (camera cutout circle) opacity. 0 = invisible (it still takes taps); starts at 0. */
    val landscapeCircleOpacity: Float = 0f,
    val landscapeCircleEdgeInset: Float = 16f,
    val landscapeCircleYOffset: Float = 12f,
    val landscapeCirclePosition: String = LANDSCAPE_POSITION_AUTO,
    // Fine tuning of the idle landscape pill, added on top of the chosen position (camera / left / right / middle).
    val landscapeCircleXAdjust: Float = 0f,
    val landscapeCircleYAdjust: Float = 0f,
    // Where the collapsed landscape pill WITH content sits (camera / left / right / middle).
    // `landscapeXOffset` is the horizontal fine tuning added on top of that position.
    val landscapePillPosition: String = LANDSCAPE_POSITION_MIDDLE,
    val disabledEventsLandscape: Set<String> = emptySet(),
    /** Collapsed island with only static cards falls back to the idle cutout after this long. */
    val idleTimeoutMs: Int = IslandTimings.STATIC_IDLE_TIMEOUT_MS.toInt(),
    /** Low battery, battery saver and fully charged cards leave the island after this long. */
    val batteryWarningMs: Int = IslandTimings.BATTERY_WARNING_CARD_MS.toInt(),
    /** A fresh short event holds the primary cutout this long when a higher-priority card is live. */
    val popupMs: Int = IslandTimings.POPUP_MS.toInt(),
    /** The collapsed secondary round cutout merges into the primary cutout after this long. */
    val secondaryMergeMs: Int = IslandTimings.SECONDARY_IDLE_MS.toInt(),
    /** A download / upload card the app has not updated for this many minutes is dropped. 0 = never. */
    val transferExpiryMinutes: Int = DEFAULT_TRANSFER_EXPIRY_MINUTES,
    val idleWidgetsEnabled: Boolean = false,
    /**
     * The landscape profile's own on / off switch (the portrait one is [idleWidgetsEnabled]). Old installs start from
     * the portrait value.
     */
    val idleWidgetsEnabledLandscape: Boolean = false,
    /** Idle widgets go off after a tap on the cutout closed the island, per orientation (see model/IdleWidgetOff). */
    val idleOffAfterTapClose: Boolean = false,
    val idleOffAfterTapCloseLandscape: Boolean = false,
    /** Idle widgets go off while a long press has the pill hidden, per orientation. */
    val idleOffAfterLongPress: Boolean = false,
    val idleOffAfterLongPressLandscape: Boolean = false,
    /** Idle widgets go off while any notification is stored on the island, per orientation. */
    val idleOffWhenStored: Boolean = false,
    val idleOffWhenStoredLandscape: Boolean = false,
    /** Seconds between the sideways ticks of the idle glyphs; shared by portrait and landscape. */
    val idleGlyphTickSeconds: Float = com.agupta07505.smartisland.model.IdleSlots.DEFAULT_TICK_SECONDS,
    /** Comma list of widget ids ([com.agupta07505.smartisland.model.IdleWidgetKind.id]); several take turns. */
    val idleWidgetIds: String = com.agupta07505.smartisland.model.IdleWidgets.DEFAULT_IDS,
    /** Seconds each widget stays before the next one, when more than one is switched on. */
    val idleWidgetCycleSeconds: Int = com.agupta07505.smartisland.model.IdleWidgets.DEFAULT_CYCLE_SECONDS,
    /** Seconds without a touch until the semi-expanded widget island closes by itself; 0 = never (see model/WidgetIslandTimer). */
    val widgetIslandCloseSeconds: Int = com.agupta07505.smartisland.model.WidgetIslandTimer.DEFAULT_SECONDS,
    /**
     * What the charging battery island shows besides percent, time to full and temperature: WATT or VOLT
     * ([com.agupta07505.smartisland.model.WidgetIslandContent.BATTERY_WATT]).
     */
    val widgetIslandBatteryMetric: String = com.agupta07505.smartisland.model.WidgetIslandContent.BATTERY_WATT,
    /** The next-alarm widget stays off the pill while the alarm is more than 12 hours away. */
    val idleAlarmHideBeyond12h: Boolean = false,
    /** The battery widget takes a turn only while charging. Off (the default): it also shows the level while not charging. */
    val batteryOnlyWhenCharging: Boolean = false,
    /**
     * The 2.5 s sideways beat of the idle glyphs only plays when a value on the widget changed. Off (the default): it
     * plays on every beat.
     */
    val idleBeatOnlyOnChange: Boolean = false,
    /** Font of the collapsed pill's idle glyphs: empty = the island font, else a [VALID_FONTS] id or a custom font id. */
    val idleGlyphFont: String = "",
    /** Size of the idle glyphs in percent of the normal size (see model/IdleSlots). */
    val idleGlyphScale: Int = com.agupta07505.smartisland.model.IdleSlots.DEFAULT_GLYPH_SCALE,
    /** AUTO (device region), C or F. */
    val idleWidgetTempUnit: String = com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO,
    val idleWidgetWindUnit: String = com.agupta07505.smartisland.model.IdleWidgets.UNIT_AUTO,
    /** Where the weather's place comes from ([com.agupta07505.smartisland.model.WeatherConfig.SOURCE_AUTO], MANUAL or LAST_KNOWN). */
    val weatherLocationSource: String = com.agupta07505.smartisland.model.WeatherConfig.SOURCE_AUTO,
    /** The city picked for the manual source (display name, blank = none) and its place, rounded to one decimal. */
    val weatherManualCity: String = "",
    val weatherManualLat: Float = 0f,
    val weatherManualLon: Float = 0f,
    /** Minutes between weather lookups; 0 = only when asked ([com.agupta07505.smartisland.model.WeatherConfig.REFRESH_CHOICES]). */
    val weatherRefreshMinutes: Int = com.agupta07505.smartisland.model.WeatherConfig.DEFAULT_REFRESH_MINUTES,
    /** [com.agupta07505.smartisland.model.WeatherConfig.PROVIDER_OPEN_METEO] or PROVIDER_CUSTOM (an Open-Meteo compatible service). */
    val weatherProvider: String = com.agupta07505.smartisland.model.WeatherConfig.PROVIDER_OPEN_METEO,
    val weatherCustomUrl: String = "",
    /** The custom provider's API key. Kept on the device only: it is left out of backups. */
    val weatherCustomKey: String = "",
    /** What the idle weather pill shows ([com.agupta07505.smartisland.model.WeatherPill]). */
    val weatherPillMode: String = com.agupta07505.smartisland.model.WeatherPill.TEMP_ICON,
    /** The extra island rows, as a comma list of [com.agupta07505.smartisland.model.WeatherReadouts] ids; empty = the plain island. */
    val weatherIslandReadouts: String = "",
    /** The weather glyph moves (sun spin, rain fall). The system "remove animations" setting always wins. */
    val weatherIconAnimated: Boolean = true,
    /** Hold the weather glyph still while Battery Saver is on. */
    val weatherIconPauseOnSaver: Boolean = true,
    /** When the weather widget takes a turn on the pill ([com.agupta07505.smartisland.model.WeatherRules.MODE_ALWAYS] or MODE_NOTABLE). */
    val weatherShowRule: String = com.agupta07505.smartisland.model.WeatherRules.MODE_ALWAYS,
    /** The triggers of "only when notable", as a comma list of [com.agupta07505.smartisland.model.WeatherRules] ids; never empty. */
    val weatherNotableTriggers: String = com.agupta07505.smartisland.model.WeatherRules.DEFAULT_TRIGGERS,
    /** Notable when at or above this temperature (Celsius). */
    val weatherHotC: Int = com.agupta07505.smartisland.model.WeatherRules.DEFAULT_HOT_C,
    /** Notable when at or below this temperature (Celsius). */
    val weatherColdC: Int = com.agupta07505.smartisland.model.WeatherRules.DEFAULT_COLD_C,
    /** Keep the weather widget off the pill between [weatherNightStartHour] and [weatherNightEndHour]. */
    val weatherHideAtNight: Boolean = false,
    val weatherNightStartHour: Int = com.agupta07505.smartisland.model.WeatherRules.DEFAULT_NIGHT_START,
    val weatherNightEndHour: Int = com.agupta07505.smartisland.model.WeatherRules.DEFAULT_NIGHT_END,
    /**
     * The net speed widget only takes a turn on the pill above this speed in KB/s (0 = Off: always in the rotation).
     */
    val netSpeedThresholdKbps: Int = com.agupta07505.smartisland.model.NetSpeedRules.DEFAULT_KBPS,
    /** The net speed widget shows on any connection, on Wi-Fi only or on mobile data only. */
    val netSpeedConnection: String = com.agupta07505.smartisland.model.NetSpeedRules.CONN_ANY,
    /**
     * Per-widget schedules: which widgets rotate during work hours, at night, or while charging. The hidden lists are
     * comma lists of widget ids.
     */
    val idleScheduleWorkEnabled: Boolean = false,
    val idleScheduleWorkHidden: String = "",
    val idleScheduleWorkStartHour: Int = com.agupta07505.smartisland.model.IdleSchedule.DEFAULT_WORK_START,
    val idleScheduleWorkEndHour: Int = com.agupta07505.smartisland.model.IdleSchedule.DEFAULT_WORK_END,
    val idleScheduleWorkWeekdaysOnly: Boolean = true,
    val idleScheduleNightEnabled: Boolean = false,
    val idleScheduleNightHidden: String = "",
    val idleScheduleNightStartHour: Int = com.agupta07505.smartisland.model.IdleSchedule.DEFAULT_NIGHT_START,
    val idleScheduleNightEndHour: Int = com.agupta07505.smartisland.model.IdleSchedule.DEFAULT_NIGHT_END,
    val idleScheduleChargingEnabled: Boolean = false,
    val idleScheduleChargingHidden: String = "",
    /** The widget that always leads the rotation (a widget id); empty = none. */
    val idleWidgetPinnedFirst: String = "",
    /**
     * How the idle glyphs change: [com.agupta07505.smartisland.model.LedTick.STYLE_SMOOTH] (the heavy tick) or
     * [com.agupta07505.smartisland.model.LedTick.STYLE_LED] (footsteps on an LED grid).
     */
    val idleTickStyle: String = com.agupta07505.smartisland.model.LedTick.STYLE_SMOOTH,
    /** Hops per change for the LED style, repeated in turn: "4, 5, 4" (see [com.agupta07505.smartisland.model.LedTick.parsePattern]). */
    val ledTickPattern: String = com.agupta07505.smartisland.model.LedTick.DEFAULT_PATTERN,
    /** Milliseconds between the hops of the LED style. */
    val ledTickPaceMs: Int = com.agupta07505.smartisland.model.LedTick.DEFAULT_PACE_MS,
    /** Font of the LED text: empty = the built-in dot font, else any island font id (drawn as dots). */
    val ledTickFont: String = com.agupta07505.smartisland.model.LedTick.FONT_BUILTIN,
    /** Colour of the LEDs: red, amber, green or white. */
    val ledTickColor: String = com.agupta07505.smartisland.model.LedTick.COLOR_RED,
    /** The value slot steps half a pace after the glyph slot (footsteps) instead of both together. */
    val ledTickStagger: Boolean = true,
    val animSpeedPortrait: String = AnimationSpeed.ID_RELAXED,
    val animSpeedLandscape: String = AnimationSpeed.ID_RELAXED,
    /** How the secondary round cutout merges into the primary one: "slide" or "bubble". */
    val mergeStyle: String = MergeStyle.ID_SLIDE,
    /** Same choice for landscape. Old backups and old installs start from the shared [mergeStyle] they had. */
    val mergeStyleLandscape: String = MergeStyle.ID_SLIDE,
    /** Keep showing cards while Do Not Disturb hides notifications from the shade. Sound stays muted. */
    val showDuringDnd: Boolean = false,
    val hapticStrength: String = HapticStrength.ID_NORMAL,
    /** Off hides the white pill (and its pull animation) and makes the Floating window swipe action do nothing. */
    val floatingWindowEnabled: Boolean = true,
    /** [com.agupta07505.smartisland.model.FloatingWindowMode] id: Android's own freeform, or LMO Freeform. */
    val floatingWindowMode: String = com.agupta07505.smartisland.model.FloatingWindowMode.ID_SYSTEM,
    val gestureSensitivity: String = GestureSensitivity.ID_NORMAL,
    // Deny sets: an app in no set gets the default. Sound uses disabledSoundPackages.
    /** Apps whose cards never open into the expanded card. */
    val noExpandPackages: Set<String> = emptySet(),
    /** Apps whose new notifications never open the card by themselves (beats the loud-notification exception). */
    val noAutoExpandPackages: Set<String> = emptySet(),
    /**
     * Apps the user switched auto-expand ON for. An app in neither set is at its default: only its loud (non-silent)
     * notifications open the card by themselves.
     */
    val autoExpandPackages: Set<String> = emptySet(),
    /** System events (event ids) that never open the card by themselves; they still show in the pill. */
    val noAutoExpandEvents: Set<String> = emptySet(),
    /**
     * True: while the phone is locked no event shows in the collapsed pill (calls excepted); the idle cutout stays.
     * Only matters when [showOnLockScreen] is on.
     */
    val hideAllOnLockScreen: Boolean = false,
    /** Event ids whose card (or privacy dot) is kept out of the pill while the phone is locked. */
    val hideOnLockScreenEvents: Set<String> = emptySet(),
    /**
     * Universal switch for app notifications. Off: the notification cards are not shown and the listener ignores the
     * notifications completely (they stay in the system shade). On by default.
     */
    val notificationsEnabled: Boolean = true,
    /** Apps whose cards stay out of the pill while the phone is locked (their notifications are untouched). */
    val hideOnLockScreenPackages: Set<String> = emptySet()
) {
    fun animationSpeed(isLandscape: Boolean): AnimationSpeed =
        AnimationSpeed.fromId(if (isLandscape) animSpeedLandscape else animSpeedPortrait)

    fun mergeStyleKind(): MergeStyle = MergeStyle.fromId(mergeStyle)

    /** Merge animation of the given orientation (the overlay reads [mergeStyle] after `forOrientation` has swapped it). */
    fun mergeStyleKind(landscape: Boolean): MergeStyle =
        MergeStyle.fromId(if (landscape) mergeStyleLandscape else mergeStyle)

    fun isEventEnabled(eventId: String): Boolean = eventId !in disabledEvents && !notificationsSwitchedOff(eventId)

    private fun notificationsSwitchedOff(eventId: String): Boolean =
        !notificationsEnabled && eventId == com.agupta07505.smartisland.model.IslandEvent.NOTIFICATION.id

    /**
     * True when the event must stay out of the pill while the phone is locked: the universal lock-screen switch or
     * the event's own. Calls are never hidden (a ringing call has to stay reachable).
     */
    /**
     * True when a card of [packageName] must stay out of the pill while the phone is locked because of the per-app
     * rule. Calls are never hidden, whatever app they come from.
     */
    fun isAppHiddenOnLockScreen(packageName: String, mode: com.agupta07505.smartisland.model.IslandMode): Boolean =
        mode != com.agupta07505.smartisland.model.IslandMode.IncomingCall && packageName in hideOnLockScreenPackages

    fun isEventHiddenOnLockScreen(eventId: String): Boolean {
        if (eventId == com.agupta07505.smartisland.model.IslandEvent.CALL.id) return false
        return hideAllOnLockScreen || eventId in hideOnLockScreenEvents
    }

    fun isEventAutoExpand(eventId: String): Boolean = eventId !in noAutoExpandEvents

    fun isLandscapeEventEnabled(eventId: String): Boolean =
        eventId !in disabledEventsLandscape && !notificationsSwitchedOff(eventId)

    /**
     * True when no profile that can be shown would display the event. The notification listener ignores such events
     * completely and leaves them in the system shade.
     */
    fun isEventDisabledEverywhere(eventId: String): Boolean =
        notificationsSwitchedOff(eventId) ||
            (eventId in disabledEvents && (!showInLandscape || eventId in disabledEventsLandscape))

    /**
     * True when at least one displayable profile has the event switched off. The listener must not pull such a
     * notification out of the system shade, or it would vanish in that orientation.
     */
    fun isEventDisabledSomewhere(eventId: String): Boolean =
        notificationsSwitchedOff(eventId) ||
            eventId in disabledEvents || (showInLandscape && eventId in disabledEventsLandscape)

    fun toJson(appVersion: String = ""): String {
        val root = JSONObject()
        root.put("formatVersion", BACKUP_FORMAT_VERSION)
        root.put("app", "com.agupta07505.smartisland")
        if (appVersion.isNotBlank()) {
            root.put("appVersion", appVersion)
        }
        root.put("exportTimestamp", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("enabled", enabled)
        settingsObj.put("width", width.toDouble())
        settingsObj.put("height", height.toDouble())
        settingsObj.put("xOffset", xOffset.toDouble())
        settingsObj.put("yOffset", yOffset.toDouble())
        settingsObj.put("cornerRadius", cornerRadius.toDouble())
        settingsObj.put("opacity", opacity.toDouble())
        settingsObj.put("pillColor", pillColor)
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
        settingsObj.put("enableAppShortcuts", enableAppShortcuts)

        val shortcutsArray = JSONArray()
        shortcutPackages.forEach { shortcutsArray.put(it) }
        settingsObj.put("shortcutPackages", shortcutsArray)

        settingsObj.put("showRecentApps", showRecentApps)
        settingsObj.put("utilityLayout", utilityLayout)
        settingsObj.put("utilityCalculatorPackage", utilityCalculatorPackage)
        settingsObj.put("utilityCalculatorFloating", utilityCalculatorFloating)
        settingsObj.put("welcomeDialogShown", welcomeDialogShown)
        settingsObj.put("showOnLockScreen", showOnLockScreen)
        settingsObj.put("showLiveEventsInForeground", showLiveEventsInForeground)
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

        settingsObj.put("hideWhenIdle", hideWhenIdle)
        settingsObj.put("autoHidePill", autoHidePill)
        settingsObj.put("autoHideTimeoutSeconds", autoHideTimeoutSeconds)
        settingsObj.put("showInLandscape", showInLandscape)
        settingsObj.put("autoExpandOnNotification", autoExpandOnNotification)
        settingsObj.put("autoExpandOnLockScreen", autoExpandOnLockScreen)
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
        settingsObj.put("fingerprintAnimationEnabled", fingerprintAnimationEnabled)
        settingsObj.put("themeMode", themeMode)
        settingsObj.put("liquidGlassTheme", liquidGlassTheme)
        settingsObj.put("materialYou", materialYou)
        settingsObj.put("accentColor", accentColor)
        settingsObj.put("expandedTheme", expandedTheme)
        settingsObj.put("islandFont", islandFont)
        settingsObj.put("enableNotchMode", enableNotchMode)
        settingsObj.put("cameraCutoutManual", cameraCutoutManual)
        settingsObj.put("cameraCutoutX", cameraCutoutX.toDouble())
        settingsObj.put("cameraCutoutY", cameraCutoutY.toDouble())
        settingsObj.put("cameraCutoutWidth", cameraCutoutWidth.toDouble())
        settingsObj.put("cameraCutoutHeight", cameraCutoutHeight.toDouble())
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

        val disabledEventsLandscapeArray = JSONArray()
        disabledEventsLandscape.forEach { disabledEventsLandscapeArray.put(it) }
        settingsObj.put("disabledEventsLandscape", disabledEventsLandscapeArray)

        settingsObj.put("idleTimeoutMs", idleTimeoutMs)
        settingsObj.put("batteryWarningMs", batteryWarningMs)
        settingsObj.put("popupMs", popupMs)
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
        settingsObj.put("idleGlyphTickSeconds", idleGlyphTickSeconds.toDouble())
        settingsObj.put("idleWidgetIds", idleWidgetIds)
        settingsObj.put("idleWidgetCycleSeconds", idleWidgetCycleSeconds)
        settingsObj.put("widgetIslandCloseSeconds", widgetIslandCloseSeconds)
        settingsObj.put("idleAlarmHideBeyond12h", idleAlarmHideBeyond12h)
        settingsObj.put("batteryOnlyWhenCharging", batteryOnlyWhenCharging)
        settingsObj.put("idleBeatOnlyOnChange", idleBeatOnlyOnChange)
        settingsObj.put("idleWidgetTempUnit", idleWidgetTempUnit)
        settingsObj.put("idleWidgetWindUnit", idleWidgetWindUnit)
        settingsObj.put("weatherLocationSource", weatherLocationSource)
        settingsObj.put("weatherManualCity", weatherManualCity)
        settingsObj.put("weatherManualLat", weatherManualLat.toDouble())
        settingsObj.put("weatherManualLon", weatherManualLon.toDouble())
        settingsObj.put("weatherRefreshMinutes", weatherRefreshMinutes)
        settingsObj.put("weatherProvider", weatherProvider)
        settingsObj.put("weatherCustomUrl", weatherCustomUrl)
        // weatherCustomKey is deliberately not written: a backup file must not carry a secret.
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
        settingsObj.put("hideAllOnLockScreen", hideAllOnLockScreen)
        val hideOnLockArray = JSONArray()
        hideOnLockScreenEvents.forEach { hideOnLockArray.put(it) }
        settingsObj.put("hideOnLockScreenEvents", hideOnLockArray)
        settingsObj.put("notificationsEnabled", notificationsEnabled)
        val hideOnLockPackagesArray = JSONArray()
        hideOnLockScreenPackages.forEach { hideOnLockPackagesArray.put(it) }
        settingsObj.put("hideOnLockScreenPackages", hideOnLockPackagesArray)

        root.put("settings", settingsObj)
        return root.toString(2)
    }

    data class BackupMetadata(
        val formatVersion: Int,
        val appVersion: String,
        val timestamp: Long,
        val settingsCount: Int
    )

    val expandedBlurMode: ExpandedBlurMode
        get() = if (expandedBlurEnabled) ExpandedBlurMode.Card else ExpandedBlurMode.Off

    /**
     * The settings the media (music) card draws with: the shared blur and opacity fields hold the media values, so
     * every existing helper (`cardBackgroundBlur`, `islandSurface`) works unchanged on this copy.
     */
    fun forMedia(): SmartIslandSettings = copy(
        opacity = mediaOpacity,
        expandedBlurEnabled = mediaBlurEnabled,
        expandedBlurRadius = mediaBlurRadius
    )

    fun forCard(media: Boolean): SmartIslandSettings = if (media) forMedia() else this

    fun weatherRules() = com.agupta07505.smartisland.model.WeatherRulesPrefs(
        mode = com.agupta07505.smartisland.model.WeatherRules.normalizeMode(weatherShowRule),
        triggers = com.agupta07505.smartisland.model.WeatherRules.normalizeTriggers(weatherNotableTriggers),
        hotC = com.agupta07505.smartisland.model.WeatherRules.normalizeHotC(weatherHotC),
        coldC = com.agupta07505.smartisland.model.WeatherRules.normalizeColdC(weatherColdC),
        hideAtNight = weatherHideAtNight,
        nightStartHour = com.agupta07505.smartisland.model.WeatherRules.normalizeHour(weatherNightStartHour),
        nightEndHour = com.agupta07505.smartisland.model.WeatherRules.normalizeHour(weatherNightEndHour)
    )

    fun idleSchedulePrefs() = com.agupta07505.smartisland.model.IdleSchedulePrefs(
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

    fun weatherPrefs() = com.agupta07505.smartisland.model.WeatherPrefs(
        source = weatherLocationSource,
        manualCity = weatherManualCity,
        manualLat = weatherManualLat,
        manualLon = weatherManualLon,
        refreshMinutes = weatherRefreshMinutes,
        provider = weatherProvider,
        customUrl = weatherCustomUrl,
        customKey = weatherCustomKey,
        pillMode = com.agupta07505.smartisland.model.WeatherPill.normalize(weatherPillMode),
        readouts = com.agupta07505.smartisland.model.WeatherReadouts.normalize(weatherIslandReadouts)
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
        /** The fonts that ship with the app. A font the user added has the id `custom:<file name>` (see [CustomFonts]). */
        val VALID_FONTS = setOf(FONT_SYSTEM, FONT_SERIF, FONT_MONOSPACE, FONT_LORA, FONT_INTER_DISPLAY, FONT_POPPINS)

        /** True for a built-in font id or a well-formed custom font id. Whether the file exists is checked when the font is drawn. */
        fun isValidFont(id: String): Boolean = id in VALID_FONTS || CustomFonts.fileNameOf(id) != null

        const val CIRCLE_POSITION_RIGHT = "right"
        const val CIRCLE_POSITION_LEFT = "left"

        const val LANDSCAPE_SHAPE_PILL = "pill"
        const val LANDSCAPE_SHAPE_CIRCLE = "circle"
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

        const val BACKUP_FORMAT_VERSION = 1

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
        const val MIN_X_OFFSET = -140f
        const val MAX_X_OFFSET = 140f
        const val MIN_Y_OFFSET = 0f
        const val MAX_Y_OFFSET = 80f
        const val MIN_CORNER_RADIUS = 8f
        const val MAX_CORNER_RADIUS = 40f
        const val MIN_OPACITY = 0.2f
        /** The landscape idle circle may go all the way to invisible; every other opacity keeps [MIN_OPACITY]. */
        const val MIN_CIRCLE_OPACITY = 0f
        const val MIN_EXPANDED_BLUR = 4f
        const val MAX_EXPANDED_BLUR = 60f
        const val MIN_GRADIENT_STRENGTH = 0.1f
        const val MAX_GRADIENT_STRENGTH = 1f
        const val MAX_OPACITY = 1f
        const val MIN_SHADOW_ELEVATION = 0f
        const val MAX_SHADOW_ELEVATION = 32f

        const val MIN_IDLE_TIMEOUT_MS = 1_000
        // The slider's last stop (one 500 ms step past 30 s) means "never": the idle timeout is off.
        const val MAX_IDLE_TIMEOUT_MS = 30_500
        const val IDLE_TIMEOUT_NEVER_MS = MAX_IDLE_TIMEOUT_MS
        const val MIN_BATTERY_WARNING_MS = 1_000
        const val MAX_BATTERY_WARNING_MS = 15_000
        const val MIN_POPUP_MS = 500
        const val MAX_POPUP_MS = 6_000
        const val MIN_SECONDARY_MERGE_MS = 500
        const val MAX_SECONDARY_MERGE_MS = 10_000
        const val MAX_TRANSFER_EXPIRY_MINUTES = 120
        const val DEFAULT_TRANSFER_EXPIRY_MINUTES = 15

        fun parseBackupMetadata(jsonString: String): BackupMetadata? {
            return runCatching {
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
            }.getOrNull()
        }

        fun fromJson(jsonString: String): SmartIslandSettings {
            val root = JSONObject(jsonString)
            val obj = root.optJSONObject("settings") ?: root
            val defaults = Default

            fun safeFloat(key: String, default: Float, min: Float, max: Float): Float {
                val raw = if (obj.has(key)) obj.optDouble(key, default.toDouble()).toFloat() else default
                return if (raw.isFinite()) raw.coerceIn(min, max) else default
            }

            // A merge style id: unknown or missing values fall back to [fallback].
            fun mergeId(key: String, fallback: String): String =
                obj.optString(key, fallback).takeIf { it in MergeStyle.VALID_IDS } ?: fallback

            fun safeColor(key: String, default: Long): Long {
                return if (obj.has(key)) {
                    val color = obj.optLong(key, default)
                    if (color != 0L) color else default
                } else default
            }

            fun safeStringSet(key: String, default: Set<String>, maxItems: Int = Int.MAX_VALUE): Set<String> {
                val arr = obj.optJSONArray(key) ?: return default
                val result = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    val pkg = arr.optString(i, "").trim()
                    if (pkg.isNotBlank() && pkg.length <= 255) {
                        result.add(pkg)
                        if (result.size >= maxItems) break
                    }
                }
                return result
            }

            return SmartIslandSettings(
                enabled = obj.optBoolean("enabled", defaults.enabled),
                width = safeFloat("width", defaults.width, MIN_WIDTH, MAX_WIDTH),
                height = safeFloat("height", defaults.height, MIN_HEIGHT, MAX_HEIGHT),
                xOffset = safeFloat("xOffset", defaults.xOffset, MIN_X_OFFSET, MAX_X_OFFSET),
                yOffset = safeFloat("yOffset", defaults.yOffset, MIN_Y_OFFSET, MAX_Y_OFFSET),
                cornerRadius = safeFloat("cornerRadius", defaults.cornerRadius, MIN_CORNER_RADIUS, MAX_CORNER_RADIUS),
                opacity = safeFloat("opacity", defaults.opacity, MIN_OPACITY, MAX_OPACITY),
                pillColor = safeColor("pillColor", defaults.pillColor),
                batteryColor = safeColor("batteryColor", defaults.batteryColor),
                notificationDotColor = safeColor("notificationDotColor", defaults.notificationDotColor),
                musicVisualizerColor = safeColor("musicVisualizerColor", defaults.musicVisualizerColor),
                hotspotColor = safeColor("hotspotColor", defaults.hotspotColor),
                callColor = safeColor("callColor", defaults.callColor),
                liveActivityColor = safeColor("liveActivityColor", defaults.liveActivityColor),
                transferColor = safeColor("transferColor", defaults.transferColor),
                navigationColor = safeColor("navigationColor", defaults.navigationColor),
                bluetoothColor = safeColor("bluetoothColor", defaults.bluetoothColor),
                flashlightColor = safeColor("flashlightColor", defaults.flashlightColor),
                screenRecordingColor = safeColor("screenRecordingColor", defaults.screenRecordingColor),
                timerColor = safeColor("timerColor", defaults.timerColor),
                stopwatchColor = safeColor("stopwatchColor", defaults.stopwatchColor),
                enableAppShortcuts = obj.optBoolean("enableAppShortcuts", defaults.enableAppShortcuts),
                shortcutPackages = obj.optJSONArray("shortcutPackages")?.let { arr ->
                    PinnedApps.normalize((0 until arr.length()).map { arr.optString(it, "") })
                } ?: defaults.shortcutPackages,
                showRecentApps = obj.optBoolean("showRecentApps", defaults.showRecentApps),
                utilityLayout = com.agupta07505.smartisland.model.UtilityLayout.encode(
                    com.agupta07505.smartisland.model.UtilityLayout.decode(obj.optString("utilityLayout", defaults.utilityLayout))
                ),
                utilityCalculatorPackage = obj.optString("utilityCalculatorPackage", defaults.utilityCalculatorPackage).take(255),
                utilityCalculatorFloating = obj.optBoolean("utilityCalculatorFloating", defaults.utilityCalculatorFloating),
                welcomeDialogShown = obj.optBoolean("welcomeDialogShown", defaults.welcomeDialogShown),
                showOnLockScreen = obj.optBoolean("showOnLockScreen", defaults.showOnLockScreen),
                showLiveEventsInForeground = obj.optBoolean("showLiveEventsInForeground", defaults.showLiveEventsInForeground),
                lockScreenPrivacy = obj.optString("lockScreenPrivacy", defaults.lockScreenPrivacy).let {
                    if (it == "AppIconOnly" || it == "FullContent") it else defaults.lockScreenPrivacy
                },
                showNotificationActions = obj.optBoolean("showNotificationActions", defaults.showNotificationActions),
                hideFromNotificationShade = obj.optBoolean("hideFromNotificationShade", defaults.hideFromNotificationShade),
                liveActivitiesEnabled = obj.optBoolean("liveActivitiesEnabled", defaults.liveActivitiesEnabled),
                navigationEnabled = obj.optBoolean("navigationEnabled", defaults.navigationEnabled),
                disabledNotificationPackages = safeStringSet("disabledNotificationPackages", defaults.disabledNotificationPackages),
                disabledSoundPackages = safeStringSet("disabledSoundPackages", defaults.disabledSoundPackages),
                disabledEvents = safeStringSet("disabledEvents", defaults.disabledEvents),
                hideWhenIdle = obj.optBoolean("hideWhenIdle", defaults.hideWhenIdle),
                autoHidePill = obj.optBoolean("autoHidePill", defaults.autoHidePill),
                autoHideTimeoutSeconds = obj.optInt("autoHideTimeoutSeconds", defaults.autoHideTimeoutSeconds).coerceIn(1, 120),
                showInLandscape = obj.optBoolean("showInLandscape", defaults.showInLandscape),
                autoExpandOnNotification = obj.optBoolean("autoExpandOnNotification", defaults.autoExpandOnNotification),
                autoExpandOnLockScreen = obj.optBoolean("autoExpandOnLockScreen", defaults.autoExpandOnLockScreen),
                enableShadow = obj.optBoolean("enableShadow", defaults.enableShadow),
                expandedBlurEnabled = obj.optBoolean("expandedBlurEnabled", defaults.expandedBlurEnabled),
                expandedBlurRadius = safeFloat("expandedBlurRadius", defaults.expandedBlurRadius, MIN_EXPANDED_BLUR, MAX_EXPANDED_BLUR),
                // Old backups have no media keys: the media card starts from the shared blur and opacity.
                mediaBlurEnabled = obj.optBoolean("mediaBlurEnabled", obj.optBoolean("expandedBlurEnabled", defaults.mediaBlurEnabled)),
                mediaBlurRadius = safeFloat(
                    "mediaBlurRadius",
                    safeFloat("expandedBlurRadius", defaults.mediaBlurRadius, MIN_EXPANDED_BLUR, MAX_EXPANDED_BLUR),
                    MIN_EXPANDED_BLUR, MAX_EXPANDED_BLUR
                ),
                mediaOpacity = safeFloat(
                    "mediaOpacity",
                    safeFloat("opacity", defaults.mediaOpacity, MIN_OPACITY, MAX_OPACITY),
                    MIN_OPACITY, MAX_OPACITY
                ),
                expandedGradientEnabled = obj.optBoolean("expandedGradientEnabled", defaults.expandedGradientEnabled),
                expandedGradientColor = safeColor("expandedGradientColor", defaults.expandedGradientColor),
                expandedGradientStrength = safeFloat("expandedGradientStrength", defaults.expandedGradientStrength, MIN_GRADIENT_STRENGTH, MAX_GRADIENT_STRENGTH),
                shadowElevation = safeFloat("shadowElevation", defaults.shadowElevation, MIN_SHADOW_ELEVATION, MAX_SHADOW_ELEVATION),
                enableMusicArtworkBackground = obj.optBoolean("enableMusicArtworkBackground", defaults.enableMusicArtworkBackground),
                enableNotificationBackdrop = obj.optBoolean("enableNotificationBackdrop", defaults.enableNotificationBackdrop),
                deviceType = obj.optString("deviceType", defaults.deviceType),
                allowNetworkChecks = obj.optBoolean("allowNetworkChecks", defaults.allowNetworkChecks),
                detectTransfersBySpeed = obj.optBoolean("detectTransfersBySpeed", defaults.detectTransfersBySpeed),
                enableNotificationHistory = obj.optBoolean("enableNotificationHistory", defaults.enableNotificationHistory),
                notificationHistoryRetentionHours = obj.optInt("notificationHistoryRetentionHours", defaults.notificationHistoryRetentionHours).coerceIn(1, 720),
                showBluetoothBattery = obj.optBoolean("showBluetoothBattery", defaults.showBluetoothBattery),
                enableBatteryMode = obj.optBoolean("enableBatteryMode", defaults.enableBatteryMode),
                enableNotificationCooldown = obj.optBoolean("enableNotificationCooldown", defaults.enableNotificationCooldown),
                notificationCooldownDurationMinutes = obj.optInt("notificationCooldownDurationMinutes", defaults.notificationCooldownDurationMinutes).coerceIn(1, 60),
                notificationCooldownThreshold = obj.optInt("notificationCooldownThreshold", defaults.notificationCooldownThreshold).coerceIn(2, 20),
                notificationCooldownExcludedPackages = safeStringSet("notificationCooldownExcludedPackages", defaults.notificationCooldownExcludedPackages),
                notificationCooldownSystemEvents = safeStringSet("notificationCooldownSystemEvents", defaults.notificationCooldownSystemEvents),
                developerModeEnabled = obj.optBoolean("developerModeEnabled", defaults.developerModeEnabled),
                recordLogs = obj.optBoolean("recordLogs", defaults.recordLogs),
                faceUnlockMode = obj.optString("faceUnlockMode", defaults.faceUnlockMode).takeIf { it in setOf("face_only", "any") } ?: defaults.faceUnlockMode,
                faceIdStyle = obj.optString("faceIdStyle", defaults.faceIdStyle).takeIf { it in setOf("square", "cutout", "square_tick") } ?: defaults.faceIdStyle,
                appFaceIdEnabled = obj.optBoolean("appFaceIdEnabled", defaults.appFaceIdEnabled),
                appFaceIdStyle = obj.optString("appFaceIdStyle", defaults.appFaceIdStyle).takeIf { it in setOf("square", "cutout", "square_tick") } ?: defaults.appFaceIdStyle,
                fingerprintAnimationEnabled = obj.optBoolean("fingerprintAnimationEnabled", defaults.fingerprintAnimationEnabled),
                themeMode = obj.optString("themeMode", defaults.themeMode).takeIf { it in setOf("system", "light", "dark") } ?: defaults.themeMode,
                liquidGlassTheme = obj.optBoolean("liquidGlassTheme", defaults.liquidGlassTheme),
                materialYou = obj.optBoolean("materialYou", defaults.materialYou),
                accentColor = safeColor("accentColor", defaults.accentColor),
                expandedTheme = obj.optString("expandedTheme", defaults.expandedTheme).takeIf { it in VALID_EXPANDED_THEMES } ?: defaults.expandedTheme,
                islandFont = obj.optString("islandFont", defaults.islandFont).takeIf { isValidFont(it) } ?: defaults.islandFont,
                enableNotchMode = obj.optBoolean("enableNotchMode", defaults.enableNotchMode),
                cameraCutoutManual = obj.optBoolean("cameraCutoutManual", defaults.cameraCutoutManual),
                cameraCutoutX = safeFloat("cameraCutoutX", defaults.cameraCutoutX, MIN_X_OFFSET, MAX_X_OFFSET),
                cameraCutoutY = safeFloat("cameraCutoutY", defaults.cameraCutoutY, MIN_Y_OFFSET, MAX_Y_OFFSET),
                cameraCutoutWidth = safeFloat("cameraCutoutWidth", defaults.cameraCutoutWidth, MIN_CUTOUT_WIDTH, MAX_CUTOUT_WIDTH),
                cameraCutoutHeight = safeFloat("cameraCutoutHeight", defaults.cameraCutoutHeight, MIN_CUTOUT_HEIGHT, MAX_CUTOUT_HEIGHT),
                enableSwipeActions = obj.optBoolean("enableSwipeActions", defaults.enableSwipeActions),
                swipeUpAction = obj.optString("swipeUpAction", defaults.swipeUpAction),
                swipeHoldUpAction = obj.optString("swipeHoldUpAction", defaults.swipeHoldUpAction),
                swipeDownAction = obj.optString("swipeDownAction", defaults.swipeDownAction),
                swipeDownCollapsedAction = obj.optString("swipeDownCollapsedAction", defaults.swipeDownCollapsedAction),
                swipeLeftAction = obj.optString("swipeLeftAction", defaults.swipeLeftAction),
                swipeRightAction = obj.optString("swipeRightAction", defaults.swipeRightAction),
                swipeHorizontalCollapsedAction = obj.optString("swipeHorizontalCollapsedAction", defaults.swipeHorizontalCollapsedAction),
                enablePillSwipeActions = obj.optBoolean("enablePillSwipeActions", defaults.enablePillSwipeActions),
                pillSwipeUpAction = obj.optString("pillSwipeUpAction", defaults.pillSwipeUpAction),
                pillSwipeDownAction = obj.optString("pillSwipeDownAction", defaults.pillSwipeDownAction),
                pillSwipeLeftAction = obj.optString("pillSwipeLeftAction", defaults.pillSwipeLeftAction),
                pillSwipeRightAction = obj.optString("pillSwipeRightAction", defaults.pillSwipeRightAction),
                longPressAction = obj.optString("longPressAction", defaults.longPressAction),
                circlePosition = obj.optString("circlePosition", defaults.circlePosition).let {
                    if (it == CIRCLE_POSITION_LEFT || it == CIRCLE_POSITION_RIGHT) it else defaults.circlePosition
                },
                landscapeShape = obj.optString("landscapeShape", defaults.landscapeShape).let {
                    if (it == LANDSCAPE_SHAPE_PILL || it == LANDSCAPE_SHAPE_CIRCLE) it else defaults.landscapeShape
                },
                landscapeWidth = safeFloat("landscapeWidth", defaults.landscapeWidth, MIN_WIDTH, MAX_WIDTH),
                landscapeHeight = safeFloat("landscapeHeight", defaults.landscapeHeight, MIN_HEIGHT, MAX_HEIGHT),
                landscapeXOffset = safeFloat("landscapeXOffset", defaults.landscapeXOffset, -MAX_LANDSCAPE_X_OFFSET, MAX_LANDSCAPE_X_OFFSET),
                landscapeYOffset = safeFloat("landscapeYOffset", defaults.landscapeYOffset, MIN_Y_OFFSET, MAX_LANDSCAPE_Y_OFFSET),
                landscapeCornerRadius = safeFloat("landscapeCornerRadius", defaults.landscapeCornerRadius, MIN_CORNER_RADIUS, MAX_CORNER_RADIUS),
                landscapeOpacity = safeFloat("landscapeOpacity", defaults.landscapeOpacity, MIN_OPACITY, MAX_OPACITY),
                landscapeCircleSize = safeFloat("landscapeCircleSize", defaults.landscapeCircleSize, MIN_CIRCLE_SIZE, MAX_CIRCLE_SIZE),
                landscapeCircleOpacity = safeFloat("landscapeCircleOpacity", defaults.landscapeCircleOpacity, MIN_CIRCLE_OPACITY, MAX_OPACITY),
                landscapeCircleEdgeInset = safeFloat("landscapeCircleEdgeInset", defaults.landscapeCircleEdgeInset, MIN_CIRCLE_EDGE_INSET, MAX_CIRCLE_EDGE_INSET),
                landscapeCircleYOffset = safeFloat("landscapeCircleYOffset", defaults.landscapeCircleYOffset, MIN_Y_OFFSET, MAX_LANDSCAPE_Y_OFFSET),
                landscapeCirclePosition = obj.optString("landscapeCirclePosition", defaults.landscapeCirclePosition).let {
                    if (it == LANDSCAPE_POSITION_AUTO || it == LANDSCAPE_POSITION_LEFT ||
                        it == LANDSCAPE_POSITION_RIGHT || it == LANDSCAPE_POSITION_MIDDLE) it else defaults.landscapeCirclePosition
                },
                landscapeCircleXAdjust = safeFloat("landscapeCircleXAdjust", defaults.landscapeCircleXAdjust, -MAX_LANDSCAPE_X_OFFSET, MAX_LANDSCAPE_X_OFFSET),
                landscapeCircleYAdjust = safeFloat("landscapeCircleYAdjust", defaults.landscapeCircleYAdjust, -MAX_LANDSCAPE_Y_OFFSET, MAX_LANDSCAPE_Y_OFFSET),
                landscapePillPosition = obj.optString("landscapePillPosition", defaults.landscapePillPosition).let {
                    if (it == LANDSCAPE_POSITION_AUTO || it == LANDSCAPE_POSITION_LEFT ||
                        it == LANDSCAPE_POSITION_RIGHT || it == LANDSCAPE_POSITION_MIDDLE) it else defaults.landscapePillPosition
                },
                disabledEventsLandscape = safeStringSet("disabledEventsLandscape", defaults.disabledEventsLandscape),
                idleTimeoutMs = obj.optInt("idleTimeoutMs", defaults.idleTimeoutMs)
                    .coerceIn(MIN_IDLE_TIMEOUT_MS, MAX_IDLE_TIMEOUT_MS),
                batteryWarningMs = obj.optInt("batteryWarningMs", defaults.batteryWarningMs)
                    .coerceIn(MIN_BATTERY_WARNING_MS, MAX_BATTERY_WARNING_MS),
                popupMs = obj.optInt("popupMs", defaults.popupMs)
                    .coerceIn(MIN_POPUP_MS, MAX_POPUP_MS),
                transferExpiryMinutes = obj.optInt("transferExpiryMinutes", defaults.transferExpiryMinutes)
                    .coerceIn(0, MAX_TRANSFER_EXPIRY_MINUTES),
                idleWidgetsEnabled = obj.optBoolean("idleWidgetsEnabled", defaults.idleWidgetsEnabled),
                idleWidgetsEnabledLandscape = obj.optBoolean(
                    "idleWidgetsEnabledLandscape",
                    obj.optBoolean("idleWidgetsEnabled", defaults.idleWidgetsEnabledLandscape)
                ),
                idleOffAfterTapClose = obj.optBoolean("idleOffAfterTapClose", defaults.idleOffAfterTapClose),
                idleOffAfterTapCloseLandscape = obj.optBoolean("idleOffAfterTapCloseLandscape", defaults.idleOffAfterTapCloseLandscape),
                idleOffAfterLongPress = obj.optBoolean("idleOffAfterLongPress", defaults.idleOffAfterLongPress),
                idleOffAfterLongPressLandscape = obj.optBoolean("idleOffAfterLongPressLandscape", defaults.idleOffAfterLongPressLandscape),
                idleOffWhenStored = obj.optBoolean("idleOffWhenStored", defaults.idleOffWhenStored),
                contentPillSameAsIdle = obj.optBoolean("contentPillSameAsIdle", defaults.contentPillSameAsIdle),
                islandExemptApps = safeStringSet("islandExemptApps", defaults.islandExemptApps),
                islandExemptAppsLandscape = safeStringSet("islandExemptAppsLandscape", defaults.islandExemptAppsLandscape),
                islandExemptAppsStatic = safeStringSet("islandExemptAppsStatic", defaults.islandExemptAppsStatic),
                contentPillSameAsIdleLandscape = obj.optBoolean("contentPillSameAsIdleLandscape", defaults.contentPillSameAsIdleLandscape),
                contentPillWidthPercentLandscape = safeFloat(
                    "contentPillWidthPercentLandscape", defaults.contentPillWidthPercentLandscape,
                    MIN_CONTENT_PILL_PERCENT, MAX_CONTENT_PILL_PERCENT
                ),
                contentPillWidthPercent = safeFloat(
                    "contentPillWidthPercent", defaults.contentPillWidthPercent, MIN_CONTENT_PILL_PERCENT, MAX_CONTENT_PILL_PERCENT
                ),
                idleOffWhenStoredLandscape = obj.optBoolean("idleOffWhenStoredLandscape", defaults.idleOffWhenStoredLandscape),
                idleGlyphTickSeconds = com.agupta07505.smartisland.model.IdleSlots.normalizeTickSeconds(
                    obj.optDouble("idleGlyphTickSeconds", defaults.idleGlyphTickSeconds.toDouble()).toFloat()
                ),
                idleWidgetIds = com.agupta07505.smartisland.model.IdleWidgets.normalize(
                    obj.optString("idleWidgetIds", defaults.idleWidgetIds)
                ),
                idleWidgetCycleSeconds = obj.optInt("idleWidgetCycleSeconds", defaults.idleWidgetCycleSeconds)
                    .coerceIn(
                        com.agupta07505.smartisland.model.IdleWidgets.MIN_CYCLE_SECONDS,
                        com.agupta07505.smartisland.model.IdleWidgets.MAX_CYCLE_SECONDS
                    ),
                idleAlarmHideBeyond12h = obj.optBoolean("idleAlarmHideBeyond12h", defaults.idleAlarmHideBeyond12h),
                batteryOnlyWhenCharging = obj.optBoolean("batteryOnlyWhenCharging", defaults.batteryOnlyWhenCharging),
                idleBeatOnlyOnChange = obj.optBoolean("idleBeatOnlyOnChange", defaults.idleBeatOnlyOnChange),
                widgetIslandCloseSeconds = com.agupta07505.smartisland.model.WidgetIslandTimer.normalize(
                    obj.optInt("widgetIslandCloseSeconds", defaults.widgetIslandCloseSeconds)
                ),
                widgetIslandBatteryMetric = obj.optString("widgetIslandBatteryMetric", defaults.widgetIslandBatteryMetric)
                    .takeIf { it in com.agupta07505.smartisland.model.WidgetIslandContent.VALID_BATTERY_METRICS } ?: defaults.widgetIslandBatteryMetric,
                idleGlyphFont = obj.optString("idleGlyphFont", defaults.idleGlyphFont)
                    .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.idleGlyphFont,
                fontMixEnabled = obj.optBoolean("fontMixEnabled", defaults.fontMixEnabled),
                fontMixLight = obj.optString("fontMixLight", defaults.fontMixLight)
                    .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixLight,
                fontMixRegular = obj.optString("fontMixRegular", defaults.fontMixRegular)
                    .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixRegular,
                fontMixMedium = obj.optString("fontMixMedium", defaults.fontMixMedium)
                    .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.fontMixMedium,
                idleGlyphScale = com.agupta07505.smartisland.model.IdleSlots.normalizeGlyphScale(
                    obj.optInt("idleGlyphScale", defaults.idleGlyphScale)
                ),
                idleWidgetTempUnit = obj.optString("idleWidgetTempUnit", defaults.idleWidgetTempUnit)
                    .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_UNITS } ?: defaults.idleWidgetTempUnit,
                idleWidgetWindUnit = obj.optString("idleWidgetWindUnit", defaults.idleWidgetWindUnit)
                    .takeIf { it in com.agupta07505.smartisland.model.IdleWidgets.VALID_WIND_UNITS } ?: defaults.idleWidgetWindUnit,
                weatherLocationSource = com.agupta07505.smartisland.model.WeatherConfig.normalizeSource(
                    obj.optString("weatherLocationSource", defaults.weatherLocationSource)
                ),
                weatherManualCity = com.agupta07505.smartisland.model.WeatherConfig.normalizeCity(
                    obj.optString("weatherManualCity", defaults.weatherManualCity)
                ),
                weatherManualLat = obj.optDouble("weatherManualLat", defaults.weatherManualLat.toDouble()).toFloat()
                    .takeIf { it.isFinite() && it in -90f..90f } ?: defaults.weatherManualLat,
                weatherManualLon = obj.optDouble("weatherManualLon", defaults.weatherManualLon.toDouble()).toFloat()
                    .takeIf { it.isFinite() && it in -180f..180f } ?: defaults.weatherManualLon,
                weatherRefreshMinutes = com.agupta07505.smartisland.model.WeatherConfig.normalizeRefreshMinutes(
                    obj.optInt("weatherRefreshMinutes", defaults.weatherRefreshMinutes)
                ),
                weatherProvider = com.agupta07505.smartisland.model.WeatherConfig.normalizeProvider(
                    obj.optString("weatherProvider", defaults.weatherProvider)
                ),
                weatherCustomUrl = com.agupta07505.smartisland.model.WeatherConfig.normalizeCustomUrl(
                    obj.optString("weatherCustomUrl", defaults.weatherCustomUrl)
                ),
                weatherPillMode = com.agupta07505.smartisland.model.WeatherPill.normalize(
                    obj.optString("weatherPillMode", defaults.weatherPillMode)
                ),
                weatherIslandReadouts = com.agupta07505.smartisland.model.WeatherReadouts.normalize(
                    obj.optString("weatherIslandReadouts", defaults.weatherIslandReadouts)
                ),
                weatherIconAnimated = obj.optBoolean("weatherIconAnimated", defaults.weatherIconAnimated),
                weatherIconPauseOnSaver = obj.optBoolean("weatherIconPauseOnSaver", defaults.weatherIconPauseOnSaver),
                weatherShowRule = com.agupta07505.smartisland.model.WeatherRules.normalizeMode(
                    obj.optString("weatherShowRule", defaults.weatherShowRule)
                ),
                weatherNotableTriggers = com.agupta07505.smartisland.model.WeatherRules.normalizeTriggers(
                    obj.optString("weatherNotableTriggers", defaults.weatherNotableTriggers)
                ),
                weatherHotC = com.agupta07505.smartisland.model.WeatherRules.normalizeHotC(
                    obj.optInt("weatherHotC", defaults.weatherHotC)
                ),
                weatherColdC = com.agupta07505.smartisland.model.WeatherRules.normalizeColdC(
                    obj.optInt("weatherColdC", defaults.weatherColdC)
                ),
                weatherHideAtNight = obj.optBoolean("weatherHideAtNight", defaults.weatherHideAtNight),
                weatherNightStartHour = com.agupta07505.smartisland.model.WeatherRules.normalizeHour(
                    obj.optInt("weatherNightStartHour", defaults.weatherNightStartHour)
                ),
                weatherNightEndHour = com.agupta07505.smartisland.model.WeatherRules.normalizeHour(
                    obj.optInt("weatherNightEndHour", defaults.weatherNightEndHour)
                ),
                netSpeedThresholdKbps = com.agupta07505.smartisland.model.NetSpeedRules.normalize(
                    obj.optInt("netSpeedThresholdKbps", defaults.netSpeedThresholdKbps)
                ),
                netSpeedConnection = com.agupta07505.smartisland.model.NetSpeedRules.normalizeConnection(
                    obj.optString("netSpeedConnection", defaults.netSpeedConnection)
                ),
                idleScheduleWorkEnabled = obj.optBoolean("idleScheduleWorkEnabled", defaults.idleScheduleWorkEnabled),
                idleScheduleWorkHidden = com.agupta07505.smartisland.model.IdleSchedule.normalizeHidden(obj.optString("idleScheduleWorkHidden", defaults.idleScheduleWorkHidden)),
                idleScheduleWorkStartHour = com.agupta07505.smartisland.model.IdleSchedule.normalizeHour(obj.optInt("idleScheduleWorkStartHour", defaults.idleScheduleWorkStartHour)),
                idleScheduleWorkEndHour = com.agupta07505.smartisland.model.IdleSchedule.normalizeHour(obj.optInt("idleScheduleWorkEndHour", defaults.idleScheduleWorkEndHour)),
                idleScheduleWorkWeekdaysOnly = obj.optBoolean("idleScheduleWorkWeekdaysOnly", defaults.idleScheduleWorkWeekdaysOnly),
                idleScheduleNightEnabled = obj.optBoolean("idleScheduleNightEnabled", defaults.idleScheduleNightEnabled),
                idleScheduleNightHidden = com.agupta07505.smartisland.model.IdleSchedule.normalizeHidden(obj.optString("idleScheduleNightHidden", defaults.idleScheduleNightHidden)),
                idleScheduleNightStartHour = com.agupta07505.smartisland.model.IdleSchedule.normalizeHour(obj.optInt("idleScheduleNightStartHour", defaults.idleScheduleNightStartHour)),
                idleScheduleNightEndHour = com.agupta07505.smartisland.model.IdleSchedule.normalizeHour(obj.optInt("idleScheduleNightEndHour", defaults.idleScheduleNightEndHour)),
                idleScheduleChargingEnabled = obj.optBoolean("idleScheduleChargingEnabled", defaults.idleScheduleChargingEnabled),
                idleScheduleChargingHidden = com.agupta07505.smartisland.model.IdleSchedule.normalizeHidden(obj.optString("idleScheduleChargingHidden", defaults.idleScheduleChargingHidden)),
                idleWidgetPinnedFirst = com.agupta07505.smartisland.model.IdleSchedule.normalizePinned(obj.optString("idleWidgetPinnedFirst", defaults.idleWidgetPinnedFirst)),
                idleTickStyle = com.agupta07505.smartisland.model.LedTick.normalizeStyle(obj.optString("idleTickStyle", defaults.idleTickStyle)),
                ledTickPattern = com.agupta07505.smartisland.model.LedTick.normalizePattern(obj.optString("ledTickPattern", defaults.ledTickPattern)),
                ledTickPaceMs = com.agupta07505.smartisland.model.LedTick.normalizePace(obj.optInt("ledTickPaceMs", defaults.ledTickPaceMs)),
                ledTickFont = obj.optString("ledTickFont", defaults.ledTickFont)
                    .takeIf { it.isEmpty() || isValidFont(it) } ?: defaults.ledTickFont,
                ledTickColor = com.agupta07505.smartisland.model.LedTick.normalizeColor(obj.optString("ledTickColor", defaults.ledTickColor)),
                ledTickStagger = obj.optBoolean("ledTickStagger", defaults.ledTickStagger),
                secondaryMergeMs = obj.optInt("secondaryMergeMs", defaults.secondaryMergeMs)
                    .coerceIn(MIN_SECONDARY_MERGE_MS, MAX_SECONDARY_MERGE_MS),
                animSpeedPortrait = obj.optString("animSpeedPortrait", defaults.animSpeedPortrait)
                    .takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedPortrait,
                animSpeedLandscape = obj.optString("animSpeedLandscape", defaults.animSpeedLandscape)
                    .takeIf { it in AnimationSpeed.VALID_IDS } ?: defaults.animSpeedLandscape,
                mergeStyle = mergeId("mergeStyle", defaults.mergeStyle),
                // An old backup has one shared choice: landscape starts from it.
                mergeStyleLandscape = mergeId("mergeStyleLandscape", mergeId("mergeStyle", defaults.mergeStyleLandscape)),
                showDuringDnd = obj.optBoolean("showDuringDnd", defaults.showDuringDnd),
                hapticStrength = obj.optString("hapticStrength", defaults.hapticStrength)
                    .takeIf { it in HapticStrength.VALID_IDS } ?: defaults.hapticStrength,
                gestureSensitivity = obj.optString("gestureSensitivity", defaults.gestureSensitivity)
                    .takeIf { it in GestureSensitivity.VALID_IDS } ?: defaults.gestureSensitivity,
                floatingWindowEnabled = obj.optBoolean("floatingWindowEnabled", defaults.floatingWindowEnabled),
                floatingWindowMode = obj.optString("floatingWindowMode", defaults.floatingWindowMode)
                    .takeIf { it in com.agupta07505.smartisland.model.FloatingWindowMode.VALID_IDS } ?: defaults.floatingWindowMode,
                noExpandPackages = safeStringSet("noExpandPackages", defaults.noExpandPackages),
                noAutoExpandPackages = safeStringSet("noAutoExpandPackages", defaults.noAutoExpandPackages),
                // An app in both sets is off: the explicit "off" wins.
                autoExpandPackages = safeStringSet("autoExpandPackages", defaults.autoExpandPackages)
                    - safeStringSet("noAutoExpandPackages", defaults.noAutoExpandPackages),
                noAutoExpandEvents = safeStringSet("noAutoExpandEvents", defaults.noAutoExpandEvents)
                    .filterTo(HashSet()) { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id } },
                hideAllOnLockScreen = obj.optBoolean("hideAllOnLockScreen", defaults.hideAllOnLockScreen),
                hideOnLockScreenEvents = safeStringSet("hideOnLockScreenEvents", defaults.hideOnLockScreenEvents)
                    .filterTo(HashSet()) { id -> com.agupta07505.smartisland.model.IslandEvent.entries.any { it.id == id } },
                notificationsEnabled = obj.optBoolean("notificationsEnabled", defaults.notificationsEnabled),
                hideOnLockScreenPackages = safeStringSet("hideOnLockScreenPackages", defaults.hideOnLockScreenPackages)
                    .filterTo(HashSet()) { it.isNotBlank() && it.length <= 255 }
            )
        }
    }
}
enum class ExpandedBlurMode { Off, Card }
