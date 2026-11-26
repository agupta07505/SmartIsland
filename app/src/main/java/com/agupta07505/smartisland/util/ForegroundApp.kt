/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/** One accessibility window as [ForegroundApp] needs it (no Android types, so it can be unit tested). */
data class AppWindow(
    val packageName: String?,
    /** `AccessibilityWindowInfo.TYPE_APPLICATION` */
    val isApplicationType: Boolean,
    val isActive: Boolean,
    val isFocused: Boolean
)

/**
 * Foreground app and the cards it hides. Uses the focused or active application window, the latest window event may
 * belong to a dialog or overlay
 */
object ForegroundApp {

    /**
     * Package of the window the user is in, null when none (the shade, a transition). [ignored] is true for this app,
     * SystemUI and keyboards
     */
    fun pick(windows: List<AppWindow>, ignored: (String) -> Boolean): String? {
        val candidates = windows.filter { w ->
            w.isApplicationType && !w.packageName.isNullOrEmpty() && !ignored(w.packageName)
        }
        return (candidates.firstOrNull { it.isFocused } ?: candidates.firstOrNull { it.isActive })?.packageName
    }

    /** True when [mode] is a live card (music, navigation, a timer, a recording, a download, a live activity). */
    fun isLiveMode(mode: IslandMode): Boolean = when (mode) {
        IslandMode.Music,
        IslandMode.Navigation,
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording,
        IslandMode.Hotspot,
        IslandMode.LiveActivity,
        IslandMode.DownloadUpload -> true
        else -> false
    }

    fun hidesCard(mode: IslandMode, cardPackage: String, foregroundPackage: String?): Boolean =
        !foregroundPackage.isNullOrEmpty() && cardPackage == foregroundPackage && isLiveMode(mode)

    /** An answered call hides in its own app where the timer is visible. Ringing and dialing stay visible. */
    fun hidesCard(card: IslandNotification, foregroundPackage: String?): Boolean =
        hidesCard(card.mode, card.packageName, foregroundPackage) ||
            (!foregroundPackage.isNullOrEmpty() && card.packageName == foregroundPackage &&
                card.mode == IslandMode.IncomingCall && !card.isCallRinging && !card.isCallConnecting)
}
