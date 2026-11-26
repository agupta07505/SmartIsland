/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Whether the overlay accessibility service is switched on in the system settings. */
internal fun Context.isOverlayAccessibilityServiceEnabled(): Boolean {
    val expected = ComponentName(packageName, AppComponents.OVERLAY_SERVICE)
    val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}
