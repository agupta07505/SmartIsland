/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class IslandMode {
    Empty,
    Notification,
    IncomingCall,
    Music,
    Battery,
    LiveActivity,
    Navigation,
    DownloadUpload,
    Hotspot,
    Bluetooth,
    Flashlight,
    ScreenRecording,
    Timer,
    Stopwatch,
    /** A ringing alarm (Rule_Book §4.10): Critical. */
    Alarm,
    /** A voice recorder app is recording (Rule_Book §4.3): Persistent. */
    VoiceRecording,
    /** Camera and/or microphone in use (Rule_Book §4.15, §4.16), one merged alert: Persistent. */
    PrivacyIndicator
}
