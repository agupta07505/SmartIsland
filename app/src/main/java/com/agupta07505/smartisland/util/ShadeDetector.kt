/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** 1 accessibility window as [ShadeDetector] needs it (no Android types, so it can be unit tested). */
data class ShadeWindow(
    val packageName: String?,
    /** Window's accessibility title (SystemUI sets "Notification shade.", "Quick settings.", "Lock screen."). */
    val title: String?,
    val isSystemType: Boolean,
    val isActive: Boolean,
    val isFocused: Boolean,
    val top: Int,
    val bottom: Int
) {
    val height: Int get() = bottom - top
}

data class ShadeTitles(
    val shade: List<String>,
    val lockScreen: List<String>
) {
    companion object {
        val ENGLISH = ShadeTitles(
            shade = listOf("Notification shade", "Quick settings"),
            lockScreen = listOf("Lock screen")
        )
    }
}

object ShadeDetector {
    const val SYSTEM_UI = "com.android.systemui"

    /** Shade window covers at least this much of the screen height, the status bar and navigation bar windows do not. */
    const val MIN_HEIGHT_FRACTION = 0.4f

    /** Titles of the window the shade lives in on older Android or on skins that name it their own way. */
    private val SHADE_HOST_TITLES = listOf("Status bar", "Notification panel", "Notifications")

    fun isShadeOpen(
        windows: List<ShadeWindow>,
        displayHeight: Int,
        keyguardLocked: Boolean,
        titles: ShadeTitles = ShadeTitles.ENGLISH
    ): Boolean = windows.any { w ->
        if (w.packageName != SYSTEM_UI || !w.isSystemType) return@any false
        val title = w.title.orEmpty()
        val tall = displayHeight > 0 && w.height >= displayHeight * MIN_HEIGHT_FRACTION
        when {
            !tall -> false
            titles.lockScreen.matches(title) -> false
            keyguardLocked -> titles.shade.matches(title)
            // Some SystemUI builds leave the shade window untitled.
            else -> (w.isActive || w.isFocused) &&
                (title.isBlank() || titles.shade.matches(title) || SHADE_HOST_TITLES.any { host ->
                    host.equals(title.substringBefore(',').trim().trimEnd('.'), ignoreCase = true)
                })
        }
    }

    private fun List<String>.matches(title: String): Boolean =
        any { it.isNotBlank() && title.contains(it, ignoreCase = true) }
}
