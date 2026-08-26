/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f,
    val opacity: Float = 1f,
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
    val shortcutPackages: Set<String> = emptySet(),
    val showRecentApps: Boolean = false,
    val welcomeDialogShown: Boolean = false,
    val showOnLockScreen: Boolean = false,
    val lockScreenPrivacy: String = "AppIconOnly",
    val showNotificationActions: Boolean = true,
    val hideFromNotificationShade: Boolean = true,
    val liveActivitiesEnabled: Boolean = true,
    val navigationEnabled: Boolean = true,
    val disabledNotificationPackages: Set<String> = emptySet(),
    val disabledSoundPackages: Set<String> = emptySet(),
    val hideWhenIdle: Boolean = false,
    val autoExpandOnNotification: Boolean = true,
    val enableShadow: Boolean = true,
    val enableMusicArtworkBackground: Boolean = true,
    val deviceType: String = "AUTO",
    val allowNetworkChecks: Boolean = true,
    val enableNotificationHistory: Boolean = false,
    val notificationHistoryRetentionHours: Int = 72
) {
    companion object {
        val Default = SmartIslandSettings()

        const val MIN_WIDTH = 76f
        const val MAX_WIDTH = 180f
        const val MIN_HEIGHT = 24f
        const val MAX_HEIGHT = 60f
        const val MIN_X_OFFSET = -140f
        const val MAX_X_OFFSET = 140f
        const val MIN_Y_OFFSET = 0f
        const val MAX_Y_OFFSET = 80f
        const val MIN_CORNER_RADIUS = 8f
        const val MAX_CORNER_RADIUS = 40f
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 1f
    }
}
