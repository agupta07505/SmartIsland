/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.agupta07505.smartisland.service.SmartIslandOverlayService

/**
 * The two grants the island cannot run without (Rule_Book §7.1), checked on every app foreground.
 *
 * The overlay is drawn by an accessibility service (`TYPE_ACCESSIBILITY_OVERLAY`), so "may draw the overlay"
 * means that service is enabled; `Settings.canDrawOverlays` is reported too, for diagnostics, but a
 * `SYSTEM_ALERT_WINDOW` grant alone cannot start the island.
 */
data class PermissionStatus(
    val overlayServiceEnabled: Boolean,
    val canDrawOverlays: Boolean,
    val notificationListenerEnabled: Boolean
) {
    val overlayGranted: Boolean get() = overlayServiceEnabled

    /** True when the user has to go back through setup. */
    val needsSetup: Boolean get() = !overlayGranted || !notificationListenerEnabled
}

object PermissionGate {

    fun check(context: Context): PermissionStatus = PermissionStatus(
        overlayServiceEnabled = isOverlayServiceEnabled(context),
        canDrawOverlays = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false),
        notificationListenerEnabled = runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
    )

    fun isOverlayServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandOverlayService::class.java)
        val enabled = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull() ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
