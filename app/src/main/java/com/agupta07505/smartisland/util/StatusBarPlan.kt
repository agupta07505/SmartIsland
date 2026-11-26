/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class StatusBarSystemIcon(val key: String, val slots: List<String>) {
    Battery("battery", listOf("battery")),
    Wifi("wifi", listOf("wifi")),
    MobileSignal("mobile_signal", listOf("mobile", "phone_signal")),
    MobileData("mobile_data", listOf("data_connection")),
    Bluetooth("bluetooth", listOf("bluetooth", "bluetooth_handsfree_battery")),
    Alarm("alarm", listOf("alarm_clock")),
    Vpn("vpn", listOf("vpn")),
    Airplane("airplane", listOf("airplane")),
    Hotspot("hotspot", listOf("hotspot")),
    Location("location", listOf("location", "gps")),
    DoNotDisturb("dnd", listOf("zen")),
    Cast("cast", listOf("cast")),
    Nfc("nfc", listOf("nfc")),
    Vibrate("volume", listOf("volume")),
    Mute("mute", listOf("mute")),
    Headset("headset", listOf("headset", "wireless_headset")),
    Privacy("privacy", listOf("micphone", "camera", "privacy_mode", "call_record")),
    Sync("sync", listOf("sync_failing", "sync_active")),
    WorkProfile("work_profile", listOf("managed_profile")),
    AutoRotate("rotate", listOf("rotate")),
    Firewall("firewall", listOf("firewall")),
    Ethernet("ethernet", listOf("ethernet")),
    DataSaver("data_saver", listOf("data_saver")),
    CallStrength("call_strength", listOf("call_strength", "no_calling")),
    Roaming("roaming", listOf("roaming", "roaming_indicator")),
    HdCalling("hd_calling", listOf("volte", "hd")),
    WifiCalling("wifi_calling", listOf("vowifi", "wifi_calling"));

    companion object {
        fun fromKeys(keys: Collection<String>): Set<StatusBarSystemIcon> =
            entries.filterTo(LinkedHashSet()) { it.key in keys }
    }
}

data class StatusBarPlan(
    val notificationIcons: Boolean = true,
    val clock: Boolean = true,
    val allSystemIcons: Boolean = true,
    val systemIcons: Set<String> = emptySet()
) {
    private val chosenIcons: Set<StatusBarSystemIcon>
        get() = if (allSystemIcons) StatusBarSystemIcon.entries.toSet() else StatusBarSystemIcon.fromKeys(systemIcons)

    /** Flags for `cmd statusbar send-disable-flag` */
    val disableFlags: List<String>
        get() = buildList {
            if (allSystemIcons) add("system-icons")
            if (clock) add("clock")
            if (notificationIcons) add("notification-icons")
        }

    /** Icon slots for the `icon_blacklist` setting */
    val iconBlacklist: List<String>
        get() = buildList {
            if (clock) addAll(CLOCK_SLOTS)
            chosenIcons.forEach { addAll(it.slots) }
        }.distinct()

    /** Immersive policy hides the whole bar. */
    val immersive: Boolean get() = notificationIcons && clock && allSystemIcons

    val isEmpty: Boolean get() = disableFlags.isEmpty() && iconBlacklist.isEmpty()

    companion object {
        val Everything = StatusBarPlan()
        val CLOCK_SLOTS = listOf("clock", "time")
    }
}
