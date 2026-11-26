/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object FallbackTitles {
    const val WIFI = "Wi-Fi"
    const val VPN = "VPN connected"
    const val USB_HOST = "Computer"
    const val USB_DEVICE = "USB device"
    const val WIRED_HEADPHONE = "Wired Headphone"
    const val DOWNLOADING = "Downloading"
    const val UPLOADING = "Uploading"
    const val TIMER = "Timer"
    const val STOPWATCH = "Stopwatch"

    fun of(event: IslandEvent): Set<String> = when (event) {
        IslandEvent.WIFI, IslandEvent.WIFI_DISCONNECTED -> setOf(WIFI)
        IslandEvent.VPN -> setOf(VPN)
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED -> setOf(USB_HOST, USB_DEVICE)
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED -> setOf(WIRED_HEADPHONE)
        IslandEvent.DOWNLOAD -> setOf(DOWNLOADING)
        IslandEvent.UPLOAD -> setOf(UPLOADING)
        IslandEvent.TIMER, IslandEvent.TIMER_FINISHED -> setOf(TIMER)
        IslandEvent.STOPWATCH -> setOf(STOPWATCH)
        else -> emptySet()
    }
}
