/*
 * Realistic Dynamic Island Fork
 * Full feature parity target: iPhone 18 Pro Max Dynamic Island on Android 16
 */

package com.srcpolygot.dynamicisland.domain.model

/**
 * All supported Dynamic Island modes.
 * Matches + extends the set of activities Apple shows on iPhone 18 Pro Max
 * (up to 3 simultaneous Live Activities).
 */
enum class IslandMode {
    Empty,

    // Core Live Activities
    Notification,
    IncomingCall,
    OngoingCall,
    Music,
    Timer,
    Stopwatch,
    Battery,
    Charging,
    Navigation,
    LiveActivity,          // generic (delivery, ride, sports, flight…)

    // System / Privacy
    CameraInUse,
    MicrophoneInUse,
    Flashlight,
    ScreenRecording,
    VoiceMemo,
    Hotspot,
    Bluetooth,
    AirDrop,               // Nearby Share equivalent
    LowPowerMode,

    // Extra useful on Android
    DownloadUpload,
    Workout
}
