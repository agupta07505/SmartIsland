/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

enum class IslandVisibilityMode {
    AlwaysVisible,
    ShowOnlyWhenActive;

    companion object {
        fun fromStorageValue(value: String?): IslandVisibilityMode = when (value) {
            ShowOnlyWhenActive.name -> ShowOnlyWhenActive
            else -> AlwaysVisible
        }
    }

    fun storageValue(): String = name
}

enum class AutoHideDuration(val millis: Long, val label: String) {
    Never(-1L, "Never hide"),
    Seconds2(2000L, "2 seconds"),
    Seconds3(3000L, "3 seconds"),
    Seconds5(5000L, "5 seconds"),
    Seconds8(8000L, "8 seconds"),
    Seconds10(10000L, "10 seconds"),
    Seconds15(15000L, "15 seconds"),
    Seconds30(30000L, "30 seconds");

    companion object {
        fun fromStorageValue(value: String?): AutoHideDuration = when (value) {
            Seconds2.name -> Seconds2
            Seconds3.name -> Seconds3
            Seconds5.name -> Seconds5
            Seconds8.name -> Seconds8
            Seconds10.name -> Seconds10
            Seconds15.name -> Seconds15
            Seconds30.name -> Seconds30
            else -> Never
        }

        fun fromMillis(value: Long): AutoHideDuration = when (value) {
            Seconds2.millis -> Seconds2
            Seconds3.millis -> Seconds3
            Seconds5.millis -> Seconds5
            Seconds8.millis -> Seconds8
            Seconds10.millis -> Seconds10
            Seconds15.millis -> Seconds15
            Seconds30.millis -> Seconds30
            else -> Never
        }
    }

    fun storageValue(): String = name
}

data class SmartIslandSettings(
    val enabled: Boolean = false,
    val width: Float = 112f,
    val height: Float = 34f,
    val xOffset: Float = 0f,
    val yOffset: Float = 12f,
    val cornerRadius: Float = 22f,
    val batteryColor: Long = 0xFF10B981L,
    val notificationDotColor: Long = 0xFF2563EBL,
    val musicVisualizerColor: Long = 0xFFFF6B9AL,
    val shortcutPackages: Set<String> = emptySet(),
    val showRecentApps: Boolean = false,
    val welcomeDialogShown: Boolean = false,
    val showOnLockScreen: Boolean = false,
    val lockScreenPrivacy: String = "AppIconOnly",
    val showNotificationActions: Boolean = true,
    val hideFromNotificationShade: Boolean = false,
    val visibilityMode: IslandVisibilityMode = IslandVisibilityMode.AlwaysVisible,
    val autoHideDuration: AutoHideDuration = AutoHideDuration.Never,
    val hideWhenForegroundAppMatches: Boolean = false
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
    }
}
