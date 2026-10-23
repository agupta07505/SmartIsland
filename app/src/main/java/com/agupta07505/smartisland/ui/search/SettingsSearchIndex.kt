/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.FeatureDetailSection
import com.agupta07505.smartisland.ui.StudioTab
import com.agupta07505.smartisland.ui.sections.eventSearchKey
import com.agupta07505.smartisland.ui.sections.iconFor

internal sealed interface SearchTarget {
    /** One of the four bottom tabs. [landscape] also switches the Portrait / Landscape switch. */
    data class Tab(val tab: StudioTab, val landscape: Boolean? = null) : SearchTarget

    /** A detail screen opened from the Settings tab. */
    data class Detail(val section: FeatureDetailSection) : SearchTarget
}

/**
 * One result: the text that is matched ([item]), where it leads ([target]), the row to scroll to and flash once there
 * ([rowKey], null = just open the screen) and the icon shown in the list.
 */
internal data class SettingsSearchEntry(
    val item: SearchableSetting,
    val target: SearchTarget,
    val rowKey: String?,
    val icon: ImageVector
)

/**
 * The searchable list of settings. Titles and descriptions come from the string resources, so they
 * are searched in the language the app is shown in; [keywords] are English aliases on top of that.
 *
 * Adding a setting: give its row a `searchKey` (or `Modifier.searchAnchor`), then add one line here
 * with the same key. `SettingsSearchIndexTest` checks that ids and row keys are unique and that
 * every event on the Home grid and every detail screen can be found.
 *
 * @param str resolves a string resource id; the app passes `context::getString`, tests pass a stub.
 */
internal fun buildSettingsSearchIndex(str: (Int) -> String): List<SettingsSearchEntry> {
    val home = str(R.string.tab_studio)
    val layout = str(R.string.tab_position)
    val settingsTab = str(R.string.tab_settings)

    val notifScreen = str(R.string.card_notifications_privacy_title)
    val timingScreen = str(R.string.card_timing_title)
    val idleScreen = str(R.string.idle_widgets_title)
    val colorScreen = str(R.string.card_color_studio_title)
    val gestureScreen = str(R.string.card_gestures_guide_title)
    val shortcutScreen = str(R.string.card_app_shortcuts_title)
    val historyScreen = str(R.string.card_notification_history_title)
    val backupScreen = str(R.string.card_backup_restore_title)
    val permScreen = str(R.string.card_permissions_setup_title)
    val diagScreen = "Diagnostic log"
    val aboutScreen = str(R.string.card_about_title)

    val out = ArrayList<SettingsSearchEntry>(96)

    fun add(
        id: String,
        title: String,
        target: SearchTarget,
        screen: String,
        icon: ImageVector,
        subtitle: String = "",
        keywords: List<String> = emptyList(),
        rowKey: String? = null
    ) {
        out += SettingsSearchEntry(
            item = SearchableSetting(id, title, subtitle, keywords, screen),
            target = target,
            rowKey = rowKey,
            icon = icon
        )
    }

    fun detail(section: FeatureDetailSection) = SearchTarget.Detail(section)

    IslandEvent.entries.forEach { event ->
        add(
            id = "event.${event.id}",
            title = event.label,
            target = SearchTarget.Tab(StudioTab.Studio),
            screen = home,
            icon = iconFor(event),
            keywords = listOf(event.shortLabel, event.group, "event", "switch", "toggle", "show"),
            rowKey = eventSearchKey(event)
        )
    }

    add(
        "layout.anim_portrait", str(R.string.anim_speed_title_portrait), SearchTarget.Tab(StudioTab.Position, false),
        layout, Icons.Rounded.Tune, str(R.string.anim_speed_desc),
        listOf("animation", "speed", "relaxed", "snappy", "spring", "smooth", "fast", "slow"), "layout.anim_speed"
    )
    add(
        "layout.anim_landscape", str(R.string.anim_speed_title_landscape), SearchTarget.Tab(StudioTab.Position, true),
        layout, Icons.Rounded.Tune, str(R.string.anim_speed_desc),
        listOf("animation", "speed", "relaxed", "snappy", "spring", "smooth", "fast", "slow"), "layout.anim_speed"
    )
    add(
        "layout.merge_portrait", str(R.string.merge_style_title_portrait), SearchTarget.Tab(StudioTab.Position, false),
        layout, Icons.Rounded.Tune, str(R.string.merge_style_desc),
        listOf("merge", "bubble", "slide", "secondary", "round cutout", "animation", "liquid"), "layout.merge_style"
    )
    add(
        "layout.merge_landscape", str(R.string.merge_style_title_landscape), SearchTarget.Tab(StudioTab.Position, true),
        layout, Icons.Rounded.Tune, str(R.string.merge_style_desc),
        listOf("merge", "bubble", "slide", "secondary", "round cutout", "animation", "liquid"), "layout.merge_style"
    )
    add(
        "layout.idle_widgets", str(R.string.idle_widgets_title), SearchTarget.Tab(StudioTab.Position),
        layout, Icons.Rounded.Tune, str(R.string.idle_widgets_desc),
        listOf("widget", "weather", "clock", "time", "date", "temperature", "alarm", "battery", "speed", "idle", "empty", "cutout"),
        "layout.idle_widgets"
    )
    add(
        "layout.notch_mode", str(R.string.notch_mode_card_title), SearchTarget.Tab(StudioTab.Position, false),
        layout, Icons.Rounded.Tune, str(R.string.notch_mode_card_desc), listOf("notch", "iphone", "cutout", "pill")
    )
    add(
        "layout.circle_position", str(R.string.circle_position_card_title), SearchTarget.Tab(StudioTab.Position, false),
        layout, Icons.Rounded.Tune, str(R.string.circle_position_card_desc),
        listOf("secondary", "companion", "left", "right", "cutout")
    )
    add(
        "layout.size_position", str(R.string.precision_tuning_title), SearchTarget.Tab(StudioTab.Position, false),
        layout, Icons.Rounded.Tune, "",
        listOf("width", "height", "offset", "corner", "radius", "size", "position", "move", "pill", "portrait")
    )
    add(
        "layout.show_landscape", str(R.string.toggle_show_in_landscape_title), SearchTarget.Tab(StudioTab.Position, true),
        layout, Icons.Rounded.Tune, str(R.string.toggle_show_in_landscape_desc),
        listOf("landscape", "rotate", "horizontal", "rotation", "hide"), "layout.show_landscape"
    )
    add(
        "layout.edge_inset", str(R.string.landscape_edge_inset_title), SearchTarget.Tab(StudioTab.Position, true),
        layout, Icons.Rounded.Tune, str(R.string.landscape_edge_inset_desc),
        listOf("edge", "inset", "margin", "corner", "side", "distance", "landscape", "circle", "bar"), "layout.edge_inset"
    )
    add(
        "layout.landscape", str(R.string.tab_position), SearchTarget.Tab(StudioTab.Position, true),
        layout, Icons.Rounded.Tune, "",
        listOf("landscape", "camera", "circle", "corner bar", "inset", "rotate", "rotation", "horizontal", "width", "height", "size", "cutout with content")
    )

    add(
        "demo", str(R.string.tab_demo), SearchTarget.Tab(StudioTab.Demo), str(R.string.tab_demo),
        Icons.Rounded.FlashOn, "", listOf("sample", "test", "preview", "show card", "try")
    )

    add(
        "settings.theme", "Theme", SearchTarget.Tab(StudioTab.Settings), settingsTab,
        Icons.Rounded.Settings, "", listOf("dark", "light", "system", "mode", "night", "appearance")
    )

    val np = detail(FeatureDetailSection.NotificationRules)
    fun npToggle(key: String, title: Int, desc: Int, keywords: List<String>) =
        add("np.$key", str(title), np, notifScreen, Icons.Rounded.Notifications, str(desc), keywords, "np.$key")

    npToggle(
        "show_lock_screen", R.string.toggle_show_lock_screen_title, R.string.toggle_show_lock_screen_desc,
        listOf("lock screen", "lockscreen", "keyguard", "privacy")
    )
    npToggle(
        "notifications_enabled", R.string.toggle_notifications_enabled_title, R.string.toggle_notifications_enabled_desc,
        listOf("notifications", "master", "universal", "all notifications", "turn off", "mute")
    )
    add(
        "np.lock_screen_preset", str(R.string.lock_screen_preset_title), np, notifScreen,
        Icons.Rounded.Notifications, str(R.string.lock_screen_preset_desc),
        listOf("lock screen", "lockscreen", "preset", "messages only", "calls only", "show all", "nothing"),
        rowKey = "np.lock_screen_preset"
    )
    npToggle(
        "hide_all_lock_screen", R.string.toggle_hide_all_lock_screen_title, R.string.toggle_hide_all_lock_screen_desc,
        listOf("lock screen", "lockscreen", "keyguard", "hide", "pill", "events", "all")
    )
    add(
        "np.hide_events_lock_screen", str(R.string.lock_screen_per_event_title), np, notifScreen,
        Icons.Rounded.Notifications, str(R.string.lock_screen_per_event_desc),
        listOf("lock screen", "lockscreen", "keyguard", "hide", "per event", "pill"),
        rowKey = "np.hide_events_lock_screen"
    )
    add(
        "np.privacy_level", str(R.string.privacy_level_title), np, notifScreen, Icons.Rounded.Notifications, "",
        listOf("lock screen", "content", "hide text", "private", "sensitive")
    )
    npToggle(
        "live_events_foreground", R.string.toggle_live_events_foreground_title, R.string.toggle_live_events_foreground_desc,
        listOf("foreground", "app open", "music", "navigation", "timer", "recording", "download", "live activity", "hide")
    )
    npToggle(
        "network_access", R.string.toggle_network_access_title, R.string.toggle_network_access_desc,
        listOf("internet", "online", "vpn location", "network checks", "release")
    )
    npToggle(
        "transfer_speed", R.string.toggle_transfer_speed_title, R.string.toggle_transfer_speed_desc,
        listOf("download", "upload", "traffic", "estimate", "speed")
    )
    npToggle(
        "auto_expand", R.string.toggle_auto_expand_title, R.string.toggle_auto_expand_desc,
        listOf("expand", "open", "automatically", "new notification")
    )
    npToggle(
        "show_during_dnd", R.string.toggle_show_during_dnd_title, R.string.toggle_show_during_dnd_desc,
        listOf("dnd", "do not disturb", "silent", "priority", "focus", "quiet")
    )
    npToggle(
        "quick_actions", R.string.toggle_quick_actions_title, R.string.toggle_quick_actions_desc,
        listOf("buttons", "reply", "actions", "inline")
    )
    npToggle(
        "mirror_only", R.string.toggle_mirror_only_title, R.string.toggle_mirror_only_desc,
        listOf("shade", "status bar", "remove", "hide notification", "drawer")
    )
    npToggle(
        "hide_when_idle", R.string.toggle_auto_hide_idle_title, R.string.toggle_auto_hide_idle_desc,
        listOf("hide", "idle", "pill", "inactive", "clean", "minimal")
    )
    npToggle(
        "auto_hide_pill", R.string.toggle_auto_hide_pill_title, R.string.toggle_auto_hide_pill_desc,
        listOf("hide", "idle", "inactive", "timeout", "pill")
    )
    npToggle(
        "bluetooth_battery", R.string.toggle_bluetooth_battery_title, R.string.toggle_bluetooth_battery_desc,
        listOf("earbuds", "headphones", "airpods", "bluetooth", "battery level")
    )
    npToggle(
        "enable_cooldown", R.string.toggle_enable_cooldown_title, R.string.toggle_enable_cooldown_desc,
        listOf("spam", "cooldown", "noisy", "rate limit", "anti-spam", "duplicate")
    )
    add(
        "np.cooldown_section", str(R.string.notification_cooldown_title), np, notifScreen,
        Icons.Rounded.Notifications, str(R.string.notification_cooldown_desc),
        listOf("cooldown", "threshold", "excluded apps", "spam", "system events", "battery", "wifi", "bluetooth")
    )
    add(
        "np.app_alerts", str(R.string.app_alerts_manager_title), np, notifScreen, Icons.Rounded.Notifications,
        str(R.string.app_alerts_manager_desc),
        listOf("per app", "mute", "sound", "exclude", "expanded card", "auto-expand", "block", "apps", "rules")
    )
    add(
        "np.oem_rules", str(R.string.oem_rules_title), np, notifScreen, Icons.Rounded.Notifications, "",
        listOf("xiaomi", "samsung", "oneplus", "huawei", "autostart", "background", "device", "oem", "battery manager")
    )

    val idle = detail(FeatureDetailSection.IdleWidgets)
    add(
        "idle.screen", idleScreen, idle, idleScreen, Icons.Rounded.Timer, str(R.string.idle_widgets_desc),
        listOf("widget", "idle", "cutout", "weather", "date", "battery", "alarm", "speed", "hour tick"), "idle.screen"
    )
    add(
        "idle.enable", str(R.string.idle_widgets_enable), idle, idleScreen, Icons.Rounded.Timer, str(R.string.idle_widgets_desc),
        listOf("off", "disable", "portrait", "landscape", "switch"), "idle.enable"
    )
    add(
        "idle.off_tap", str(R.string.idle_off_after_tap_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_off_after_tap_desc), listOf("tap", "close", "camera cutout", "hide", "off"), "idle.off_tap"
    )
    add(
        "idle.off_stored", str(R.string.idle_off_when_stored_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_off_when_stored_desc), listOf("stored", "notification", "hide", "off"), "idle.off_stored"
    )
    add(
        "idle.off_long_press", str(R.string.idle_off_after_long_press_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_off_after_long_press_desc), listOf("long press", "hide", "pill", "off"), "idle.off_long_press"
    )
    add(
        "idle.select", str(R.string.idle_select_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_select_desc), listOf("select", "choose", "pick", "all", "widgets", "order", "reorder", "drag", "pin", "first"), "idle.select"
    )
    add(
        "idle.tick_timer", str(R.string.idle_tick_timer_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_tick_timer_desc), listOf("tick", "glyph", "timer", "slider", "seconds"), "idle.tick_timer"
    )
    add(
        "idle.beat_only_change", str(R.string.idle_beat_only_change_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_beat_only_change_desc), listOf("beat", "tick", "glyph", "change", "changed", "still", "quiet", "calm"), "idle.beat_only_change"
    )
    add(
        "idle.schedule", str(R.string.idle_sched_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_sched_intro), listOf("schedule", "work", "hours", "night", "charging", "rotate", "widgets"), "idle.schedule"
    )
    add(
        "idle.units", str(R.string.idle_units_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_units_note), listOf("celsius", "fahrenheit", "temperature", "unit", "wind", "kmh", "mph", "knots", "metric", "imperial"), "idle.units"
    )
    add(
        "idle.weather", str(R.string.weather_cfg_title), detail(FeatureDetailSection.WeatherSettings), idleScreen, Icons.Rounded.Timer,
        str(R.string.weather_cfg_intro), listOf("weather", "location", "city", "manual", "refresh", "interval", "provider", "open-meteo", "api", "key", "gps", "battery", "network", "pill", "feels like", "condition", "humidity", "uv", "sunrise", "sunset", "air quality", "rain", "forecast", "hourly", "animated", "icon", "battery saver", "notable", "only when", "hide at night", "night", "hot", "cold", "threshold", "severe", "alert", "storm", "smart"), "idle.weather"
    )
    add(
        "idle.calendar", str(R.string.cal_cfg_title), detail(FeatureDetailSection.CalendarSettings), idleScreen, Icons.Rounded.Timer,
        str(R.string.cal_cfg_intro), listOf("calendar", "event", "next event", "meeting", "standup", "agenda", "date", "window", "permission"), "idle.calendar"
    )
    add(
        "idle.netspeed", str(R.string.net_cfg_title), detail(FeatureDetailSection.NetSpeedSettings), idleScreen, Icons.Rounded.Timer,
        str(R.string.net_cfg_intro), listOf("network", "net", "speed", "download", "upload", "threshold", "kb/s", "mb/s", "busy", "arrow", "only when", "off", "wifi", "wi-fi", "mobile data", "data saver", "daily", "usage", "connection"), "idle.netspeed"
    )
    add(
        "idle.battery_only_charging", str(R.string.idle_battery_only_charging_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_battery_only_charging_desc), listOf("battery", "charging", "plugged", "only", "percentage", "till", "run out", "drain"), "idle.battery_only_charging"
    )
    add(
        "idle.led_tick", str(R.string.led_tick_switch_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.led_tick_switch_desc), listOf("led", "tick", "footsteps", "dots", "pattern", "pace", "font", "animation"), "idle.led_tick"
    )
    add(
        "idle.switch_timer", str(R.string.idle_switch_timer_title), idle, idleScreen, Icons.Rounded.Timer,
        str(R.string.idle_switch_timer_desc), listOf("switch", "rotate", "auto", "timer", "slider", "seconds"), "idle.switch_timer"
    )

    val timing = detail(FeatureDetailSection.Timing)
    add(
        "timing.screen", timingScreen, timing, timingScreen, Icons.Rounded.Timer, str(R.string.timing_intro),
        listOf("time", "duration", "seconds", "delay", "timers")
    )
    add(
        "timing.idle", str(R.string.timing_idle_label), timing, timingScreen, Icons.Rounded.Timer,
        str(R.string.timing_idle_hint), listOf("idle", "timeout", "away", "put away"), "timing.idle"
    )
    add(
        "timing.battery", str(R.string.timing_battery_label), timing, timingScreen, Icons.Rounded.Timer,
        str(R.string.timing_battery_hint), listOf("low battery", "warning", "length", "duration"), "timing.battery"
    )
    add(
        "timing.popup", str(R.string.timing_popup_label), timing, timingScreen, Icons.Rounded.Timer,
        str(R.string.timing_popup_hint), listOf("popup", "pop", "brief", "short events", "duration"), "timing.popup"
    )
    add(
        "timing.transfer", str(R.string.timing_transfer_label), timing, timingScreen, Icons.Rounded.Timer,
        str(R.string.timing_transfer_hint), listOf("download", "stuck", "expire", "transfer", "upload", "card stays"), "timing.transfer"
    )
    add(
        "timing.merge", str(R.string.timing_merge_label), timing, timingScreen, Icons.Rounded.Timer,
        str(R.string.timing_merge_hint), listOf("merge", "secondary", "round cutout", "delay"), "timing.merge"
    )

    val color = detail(FeatureDetailSection.ColorStudio)
    add(
        "color.screen", colorScreen, color, colorScreen, Icons.Rounded.Palette, str(R.string.card_color_studio_desc),
        listOf("colour", "theme", "gradient", "rgb", "hex", "background", "accent")
    )
    add(
        "color.opacity", str(R.string.opacity_card_title), color, colorScreen, Icons.Rounded.Palette,
        str(R.string.opacity_card_desc), listOf("transparency", "translucent", "alpha", "glass", "see through")
    )
    add(
        "color.expanded_style", str(R.string.expanded_style_card_title), color, colorScreen, Icons.Rounded.Palette,
        "", listOf("card", "style", "expanded", "look")
    )
    add(
        "color.blur", str(R.string.toggle_expanded_blur_title), color, colorScreen, Icons.Rounded.Palette,
        str(R.string.toggle_expanded_blur_desc), listOf("blur", "backdrop", "artwork", "album", "live", "frosted", "glass", "behind", "card", "off")
    )
    add(
        "color.media_look", str(R.string.media_look_title), color, colorScreen, Icons.Rounded.Palette,
        str(R.string.media_look_desc), listOf("media", "music", "player", "blur", "opacity", "artwork", "album")
    )
    add(
        "color.expanded_theme", str(R.string.expanded_theme_title), color, colorScreen, Icons.Rounded.Palette,
        str(R.string.expanded_theme_desc), listOf("light", "dark", "system", "theme", "mode", "expanded", "card", "white")
    )
    add(
        "color.gradient", str(R.string.toggle_expanded_gradient_title), color, colorScreen, Icons.Rounded.Palette,
        "", listOf("gradient", "colour", "fade")
    )

    val gestures = detail(FeatureDetailSection.GesturesGuide)
    add(
        "gestures.screen", gestureScreen, gestures, gestureScreen, Icons.Rounded.Gesture,
        str(R.string.card_gestures_guide_desc), listOf("swipe", "tap", "long press", "hold", "touch", "guide", "tour", "tutorial", "how to", "new user", "gestures list")
    )
    add(
        "gestures.sensitivity", str(R.string.gesture_sensitivity_title), gestures, gestureScreen, Icons.Rounded.Gesture,
        str(R.string.gesture_sensitivity_desc),
        listOf("sensitivity", "sensitive", "threshold", "accidental", "wrong gesture", "swipe distance", "high", "low"),
        "gestures.sensitivity"
    )
    add(
        "gestures.haptics", str(R.string.haptics_title), gestures, gestureScreen, Icons.Rounded.Gesture,
        str(R.string.haptics_desc),
        listOf("haptic", "vibration", "vibrate", "buzz", "feedback", "strength"), "gestures.haptics"
    )
    add(
        "gestures.floating_window", str(R.string.floating_window_title), gestures, gestureScreen, Icons.Rounded.Gesture,
        str(R.string.floating_window_desc),
        listOf("floating", "window", "freeform", "lmo", "pill", "handle", "pull down", "swipe down"), "gestures.floating_window"
    )
    add(
        "gestures.long_press", str(R.string.long_press_card_title), gestures, gestureScreen, Icons.Rounded.Gesture,
        str(R.string.long_press_card_desc), listOf("hold", "hide pill", "press")
    )
    add(
        "gestures.pill_swipe", str(R.string.pill_swipe_customization_title), gestures, gestureScreen,
        Icons.Rounded.Gesture, "", listOf("collapsed", "swipe left", "swipe right", "swipe up", "swipe down", "pill")
    )
    add(
        "gestures.expanded_swipe", str(R.string.expanded_swipe_customization_title), gestures, gestureScreen,
        Icons.Rounded.Gesture, "", listOf("expanded", "swipe", "pager", "dismiss all", "floating window")
    )

    val shortcuts = detail(FeatureDetailSection.AppShortcuts)
    add(
        "shortcuts.screen", shortcutScreen, shortcuts, shortcutScreen, Icons.Rounded.Apps,
        str(R.string.card_app_shortcuts_desc), listOf("launcher", "quick launch", "pinned", "apps")
    )
    add(
        "shortcuts.enable", str(R.string.toggle_enable_shortcuts_title), shortcuts, shortcutScreen, Icons.Rounded.Apps,
        "", listOf("launcher", "quick launch", "enable")
    )
    add(
        "shortcuts.recent", str(R.string.toggle_recent_apps_title), shortcuts, shortcutScreen, Icons.Rounded.Apps,
        "", listOf("recent", "recents", "history", "recently used", "swipe")
    )
    add(
        "shortcuts.pinned", str(R.string.pinned_apps_title), shortcuts, shortcutScreen, Icons.Rounded.Apps,
        "", listOf("favourite", "favorite", "pin", "apps")
    )

    val history = detail(FeatureDetailSection.NotificationHistory)
    add(
        "history.screen", historyScreen, history, historyScreen, Icons.Rounded.History,
        str(R.string.card_notification_history_desc), listOf("log", "past", "saved", "browse", "delete")
    )
    add(
        "history.enable", str(R.string.toggle_enable_history_title), history, historyScreen, Icons.Rounded.History,
        "", listOf("log", "record", "save", "enable")
    )
    add(
        "history.retention", str(R.string.retention_title), history, historyScreen, Icons.Rounded.History,
        "", listOf("auto clean", "keep", "days", "delete old", "clear")
    )

    val backup = detail(FeatureDetailSection.BackupRestore)
    add(
        "backup.export", str(R.string.backup_export_card_title), backup, backupScreen, Icons.Rounded.SettingsBackupRestore,
        str(R.string.backup_export_card_desc), listOf("json", "save", "export", "file", "copy settings")
    )
    add(
        "backup.restore", str(R.string.backup_restore_card_title), backup, backupScreen, Icons.Rounded.SettingsBackupRestore,
        str(R.string.backup_restore_card_desc), listOf("json", "import", "load", "file")
    )
    add(
        "backup.reset", str(R.string.backup_reset_card_title), backup, backupScreen, Icons.Rounded.SettingsBackupRestore,
        str(R.string.backup_reset_card_desc), listOf("factory", "default", "clear", "wipe", "restore defaults")
    )

    val perms = detail(FeatureDetailSection.PermissionsCenter)
    add(
        "perm.screen", permScreen, perms, permScreen, Icons.Rounded.Shield, str(R.string.card_permissions_setup_desc),
        listOf("access", "grant", "setup", "overlay", "allow")
    )
    add(
        "perm.accessibility", str(R.string.perm_accessibility_title), perms, permScreen, Icons.Rounded.Shield,
        "", listOf("overlay", "service", "enable", "grant")
    )
    add(
        "perm.notification", str(R.string.perm_notification_title), perms, permScreen, Icons.Rounded.Shield,
        "", listOf("listener", "access", "enable", "grant")
    )
    add(
        "perm.battery", str(R.string.perm_battery_title), perms, permScreen, Icons.Rounded.Shield,
        "", listOf("optimisation", "optimization", "background", "restrict", "unrestricted", "doze")
    )
    add(
        "perm.shizuku", str(R.string.shizuku_card_title), perms, permScreen, Icons.Rounded.Shield,
        str(R.string.shizuku_card_desc), listOf("shizuku", "adb", "one tap", "auto setup", "necessary", "all permissions", "optional", "screen sharing", "cast")
    )

    add(
        "faceid.screen", "Face ID settings", detail(FeatureDetailSection.FaceIdSettings), home, Icons.Rounded.Face,
        "Animation style, fingerprint animation and App Face ID",
        listOf("face id", "face unlock", "animation", "style", "expanded", "mix", "cutout", "app lock", "app face id", "scan", "fingerprint")
    )

    add(
        "diag.screen", diagScreen, detail(FeatureDetailSection.Diagnostics), diagScreen, Icons.Rounded.BugReport,
        "Record what the island does, to find problems",
        listOf("log", "debug", "bug", "report", "export", "record", "crash", "developer", "trace")
    )

    add(
        "about.screen", aboutScreen, detail(FeatureDetailSection.About), aboutScreen, Icons.Rounded.Info,
        str(R.string.card_about_subtitle),
        listOf("creator", "author", "original", "github", "link", "credits", "licence", "license", "gpl", "version", "animesh gupta")
    )

    val updatesScreen = str(R.string.card_updates_downloads_title)
    add(
        "updates.screen", updatesScreen, detail(FeatureDetailSection.UpdatesAndDownloads), updatesScreen, Icons.Rounded.Download,
        str(R.string.card_updates_downloads_desc),
        listOf("update", "download", "version", "github", "release", "apk", "check", "changelog")
    )

    val supportScreen = str(R.string.card_support_requests_title)
    add(
        "support.screen", supportScreen, detail(FeatureDetailSection.SupportCommunity), supportScreen, Icons.Rounded.People,
        str(R.string.card_support_requests_desc),
        listOf("support", "telegram", "community", "bug", "issue", "feedback", "feature request", "contact")
    )

    val devScreen = str(R.string.card_developer_options_title)
    add(
        "dev.screen", devScreen, detail(FeatureDetailSection.DeveloperOptions), devScreen, Icons.Rounded.Code,
        str(R.string.card_developer_options_desc),
        listOf("developer", "debug", "dev", "logs", "options", "mock", "test")
    )

    return out
}
