/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Every activity the island can show, each with its own switch. `id` is persisted in
 * SmartIslandSettings.disabledEvents; `label` is the long description (Demo tab), `shortLabel` fits a Home tile.
 */
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

    CALL("call", "Incoming and ongoing calls", "Calls", "Activities"),
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
    STOPWATCH("stopwatch", "Stopwatch", "Stopwatch", "Activities"),
    // drawn by ui/FaceIdLayer.kt over the cutout, not as a card
    FACE_UNLOCK("face_unlock", "Face ID scan (face unlock animation)", "Face ID", "Activities");

    /**
     * True when the event has its own auto-expand switch on the Home tab. Calls always open the island, so they have
     * none.
     */
    val canSwitchAutoExpand: Boolean get() = this != CALL && this != FACE_UNLOCK

    /**
     * True when the event has its own "hide on the lock screen" switch. Calls never do: a ringing call must stay
     * reachable.
     */
    val canHideOnLockScreen: Boolean get() = this != CALL && this != FACE_UNLOCK

    companion object {
        /** Event for real app notifications (not system broadcasts). */
        fun forListenerMode(mode: IslandMode, transferKind: TransferKind = TransferKind.DOWNLOAD): IslandEvent? = when (mode) {
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

        /**
         * Event a posted island notification belongs to, for filtering against the user's switches; null for modes
         * that are never gated (Empty).
         */
        fun forNotification(n: IslandNotification): IslandEvent? = when (n.mode) {
            IslandMode.Empty -> null
            IslandMode.Notification -> NOTIFICATION
            IslandMode.IncomingCall -> CALL
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
                else -> BLUETOOTH
            }
            IslandMode.Flashlight -> FLASHLIGHT
            IslandMode.ScreenRecording -> SCREEN_RECORDING
            IslandMode.AudioRecording -> AUDIO_RECORDING
            IslandMode.Timer -> TIMER
            IslandMode.Stopwatch -> STOPWATCH
        }
    }
}
