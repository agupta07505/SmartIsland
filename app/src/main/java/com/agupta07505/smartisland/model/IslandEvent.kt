/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class IslandEvent(val id: String, val label: String, val shortLabel: String, val group: String) {
    PLUGGED_IN("battery_plugged_in", "Charger plugged in (brief card)", "Plugged in", "Power"),
    PLUGGED_OUT("battery_plugged_out", "Charger unplugged", "Unplugged", "Power"),
    FULLY_CHARGED("battery_full", "Fully charged", "Full", "Power"),
    LOW_BATTERY("battery_low", "Low battery", "Low battery", "Power"),
    BATTERY_SAVER("battery_saver", "Battery saver", "Saver", "Power"),

    WIFI("wifi", "Wi-Fi connected (shows network name)", "Wi-Fi", "Connectivity"),
    VPN("vpn", "VPN connected (shows location)", "VPN", "Connectivity"),
    WIFI_DISCONNECTED("wifi_disconnected", "Wi-Fi disconnected", "Wi-Fi lost", "Connectivity"),
    VPN_DISCONNECTED("vpn_disconnected", "VPN disconnected (traffic no longer protected)", "VPN lost", "Connectivity"),
    BLUETOOTH("bluetooth", "Bluetooth connected", "Bluetooth", "Connectivity"),
    USB_CONNECTED("usb_connected", "USB device plugged in", "USB in", "Connectivity"),
    USB_DISCONNECTED("usb_disconnected", "USB device unplugged", "USB out", "Connectivity"),
    HEADPHONES_CONNECTED("headphones_connected", "Wired Headphone plugged in", "Headphone in", "Connectivity"),
    HEADPHONES_DISCONNECTED("headphones_disconnected", "Wired Headphone unplugged", "Headphone out", "Connectivity"),
    HOTSPOT("hotspot", "Hotspot", "Hotspot", "Connectivity"),

    AIRPLANE_MODE("airplane_mode", "Airplane mode on or off", "Airplane", "Modes"),
    RING("ring_mode", "Ring mode", "Ring", "Modes"),
    VIBRATE("vibrate_mode", "Vibrate mode", "Vibrate", "Modes"),
    SILENT("silent_mode", "Silent mode", "Silent", "Modes"),
    DND("dnd", "Do Not Disturb on or off", "DND", "Modes"),
    MODE("mode", "Modes such as Driving or Bedtime (Android 15 and up)", "Mode", "Modes"),

    CALL("call", "Incoming and ongoing calls", "Calls", "Activities"),
    CALL_ENDED("call_ended", "Call ended or hung up (with the call length)", "Call end", "Activities"),
    MUSIC("music", "Music player", "Music", "Activities"),
    NOTIFICATION("notification", "App notifications", "Notifications", "Activities"),
    NAVIGATION("navigation", "Navigation", "Navigation", "Activities"),
    LIVE_ACTIVITY("live_activity", "Live activities", "Live", "Activities"),
    DOWNLOAD("download", "Downloads", "Downloads", "Activities"),
    UPLOAD("upload", "Uploads", "Uploads", "Activities"),
    BACKUP("backup", "Backups and restores", "Backups", "Activities"),
    FILE_COPY_MOVE("file_copy_move", "File copy and move", "Copy / move", "Activities"),
    FILE_DELETE("file_delete", "File deletion", "Delete", "Activities"),
    APP_INSTALL("app_install", "App installs and updates", "Installs", "Activities"),
    FLASHLIGHT("flashlight", "Flashlight", "Flashlight", "Activities"),
    SCREEN_RECORDING("screen_recording", "Screen recording", "Screen rec.", "Activities"),
    AUDIO_RECORDING("audio_recording", "Audio recording", "Audio rec.", "Activities"),
    TIMER("timer", "Timer", "Timer", "Activities"),
    TIMER_FINISHED("timer_finished", "Timer finished", "Timer done", "Activities"),
    STOPWATCH("stopwatch", "Stopwatch", "Stopwatch", "Activities"),
    FACE_UNLOCK("face_unlock", "Face ID scan (face unlock animation)", "Face ID", "Activities");

    val canSwitchAutoExpand: Boolean get() = this != FACE_UNLOCK

    /**
     * True when the event has its own "hide on the lock screen" switch. Calls never do, a ringing call must stay
     * reachable
     */
    val canHideOnLockScreen: Boolean get() = this != CALL && this != FACE_UNLOCK

    companion object {
        /** Live events that can stay on the island while their own app is open. Timer and stopwatch always hide there. */
        val FOREGROUND_SWITCHABLE: List<IslandEvent> = listOf(
            MUSIC, NAVIGATION, SCREEN_RECORDING, AUDIO_RECORDING, HOTSPOT, LIVE_ACTIVITY,
            DOWNLOAD, UPLOAD, BACKUP, FILE_COPY_MOVE, FILE_DELETE, APP_INSTALL
        )

        /** Music and navigation stay hidden in their own app by default, the rest are shown. */
        val DEFAULT_SHOW_IN_FOREGROUND_IDS: Set<String> = FOREGROUND_SWITCHABLE
            .filter { it != MUSIC && it != NAVIGATION }
            .mapTo(LinkedHashSet()) { it.id }

        val AUTO_EXPAND_BY_DEFAULT: Set<IslandEvent> = setOf(CALL, NOTIFICATION, LOW_BATTERY, VPN_DISCONNECTED, TIMER_FINISHED)

        /**
         * Default of the "no auto-expand" sets: every switchable event outside [AUTO_EXPAND_BY_DEFAULT]. Built from
         * [entries], so an event added later starts off.
         */
        val DEFAULT_NO_AUTO_EXPAND_IDS: Set<String> = entries
            .filter { it.canSwitchAutoExpand && it !in AUTO_EXPAND_BY_DEFAULT }
            .mapTo(LinkedHashSet()) { it.id }

        fun forListenerMode(mode: IslandMode, transferKind: TransferKind = TransferKind.DOWNLOAD): IslandEvent? =
                when (mode) {
            IslandMode.Notification -> NOTIFICATION
            IslandMode.IncomingCall -> CALL
            IslandMode.Music -> MUSIC
            IslandMode.LiveActivity -> LIVE_ACTIVITY
            IslandMode.Navigation -> NAVIGATION
            IslandMode.DownloadUpload -> forTransfer(transferKind)
            IslandMode.Hotspot -> HOTSPOT
            IslandMode.ScreenRecording -> SCREEN_RECORDING
            IslandMode.AudioRecording -> AUDIO_RECORDING
            IslandMode.Timer -> TIMER
            IslandMode.Stopwatch -> STOPWATCH
            else -> null
        }

        fun forTransfer(kind: TransferKind): IslandEvent = when (kind) {
            TransferKind.DOWNLOAD -> DOWNLOAD
            TransferKind.UPLOAD -> UPLOAD
            TransferKind.BACKUP -> BACKUP
            TransferKind.COPY_MOVE -> FILE_COPY_MOVE
            TransferKind.DELETE -> FILE_DELETE
            TransferKind.INSTALL -> APP_INSTALL
        }

        fun forNotification(n: IslandNotification): IslandEvent? = when (n.mode) {
            IslandMode.Empty -> null
            IslandMode.Notification -> NOTIFICATION
            IslandMode.IncomingCall -> CALL
            IslandMode.CallEnded -> CALL_ENDED
            IslandMode.Music -> MUSIC
            IslandMode.Battery -> when (n.category) {
                "battery_plugged_in" -> PLUGGED_IN
                "battery_plugged_out" -> PLUGGED_OUT
                "battery_full" -> FULLY_CHARGED
                "battery_saver" -> BATTERY_SAVER
                "battery_low" -> LOW_BATTERY
                else -> PLUGGED_IN
            }
            IslandMode.LiveActivity -> LIVE_ACTIVITY
            IslandMode.Navigation -> NAVIGATION
            IslandMode.DownloadUpload ->
                if (n.transferKind == TransferKind.DOWNLOAD && n.key.contains("upload", ignoreCase = true)) {
                    UPLOAD
                } else {
                    forTransfer(n.transferKind)
                }
            IslandMode.Hotspot -> HOTSPOT
            IslandMode.Bluetooth -> when (n.category) {
                "wifi" -> WIFI
                "vpn" -> VPN
                "wifi_disconnected" -> WIFI_DISCONNECTED
                "vpn_disconnected" -> VPN_DISCONNECTED
                "usb_connected" -> USB_CONNECTED
                "usb_disconnected" -> USB_DISCONNECTED
                "headphones_connected" -> HEADPHONES_CONNECTED
                "headphones_disconnected" -> HEADPHONES_DISCONNECTED
                else -> DeviceStates.eventOf(n.category) ?: BLUETOOTH
            }
            IslandMode.Flashlight -> FLASHLIGHT
            IslandMode.ScreenRecording -> SCREEN_RECORDING
            IslandMode.AudioRecording -> AUDIO_RECORDING
            IslandMode.Timer -> if (n.timerFinished) TIMER_FINISHED else TIMER
            IslandMode.Stopwatch -> STOPWATCH
        }
    }
}
