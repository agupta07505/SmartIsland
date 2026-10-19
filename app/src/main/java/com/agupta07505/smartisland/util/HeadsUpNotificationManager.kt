/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Utility to programmatically suppress Android heads-up notification (HUN) banners
 * using `Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED`.
 *
 * Requires WRITE_SECURE_SETTINGS permission via ADB or Shizuku:
 * `adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS`
 */
object HeadsUpNotificationManager {

    private const val KEY = "heads_up_notifications_enabled"
    private const val TAG = "HeadsUpNotificationManager"

    fun adbGrantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    fun hasWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /**
     * Checks if heads-up notifications are currently suppressed (value == 0).
     */
    fun isHunSuppressed(context: Context): Boolean {
        return try {
            val enabled = Settings.Global.getInt(context.contentResolver, KEY, 1)
            enabled == 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Suppresses (0) or restores (1) system heads-up notifications.
     * Returns true on success, false if permission missing or failed.
     */
    fun setHunSuppressed(context: Context, suppress: Boolean): Boolean {
        if (!hasWriteSecureSettings(context)) return false
        return runCatchingLogged(TAG, "Setting heads_up_notifications_enabled failed") {
            val value = if (suppress) 0 else 1
            Settings.Global.putInt(context.contentResolver, KEY, value)
            true
        } ?: false
    }
}
