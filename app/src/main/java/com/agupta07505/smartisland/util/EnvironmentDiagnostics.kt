/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.agupta07505.smartisland.service.SmartIslandOverlayService

/**
 * One-line snapshot of the system state that decides whether the island can see a notification or a call: listener
 * and overlay access, battery restrictions, Do Not Disturb, audio mode, WhatsApp install.
 */
object EnvironmentDiagnostics {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun snapshot(context: Context): String {
        fun <T> safe(block: () -> T): String = runCatching { block().toString() }.getOrDefault("?")

        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        return buildString {
            append("listenerEnabled=").append(safe { isListenerEnabled(context) })
            append(" accessibilityEnabled=").append(safe { isAccessibilityEnabled(context) })
            append(" overlayPermission=").append(safe { Settings.canDrawOverlays(context) })
            append(" batteryOptimizationIgnored=").append(safe { power?.isIgnoringBatteryOptimizations(context.packageName) })
            append(" powerSaveMode=").append(safe { power?.isPowerSaveMode })
            append(" deviceIdle=").append(safe { power?.isDeviceIdleMode })
            append(" screenInteractive=").append(safe { power?.isInteractive })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                append(" backgroundRestricted=").append(safe { activityManager?.isBackgroundRestricted })
                append(" standbyBucket=").append(safe { standbyBucketName(usageStats?.appStandbyBucket) })
            }
            append(" notificationsEnabled=").append(safe { notificationManager?.areNotificationsEnabled() })
            append(" dndFilter=").append(safe { notificationManager?.currentInterruptionFilter })
            append(" audioMode=").append(safe { audioModeName(audio?.mode) })
            append(" ringerMode=").append(safe { audio?.ringerMode })
            append(" whatsapp=").append(whatsappStatus(context))
            append(" oem=").append(safe { OemDeviceRules.detectCurrentDevice() })
        }
    }

    fun audioModeName(mode: Int?): String = when (mode) {
        AudioManager.MODE_NORMAL -> "NORMAL"
        AudioManager.MODE_RINGTONE -> "RINGTONE"
        AudioManager.MODE_IN_CALL -> "IN_CALL"
        AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
        AudioManager.MODE_CALL_SCREENING -> "CALL_SCREENING"
        null -> "?"
        else -> "UNKNOWN($mode)"
    }

    private fun standbyBucketName(bucket: Int?): String = when (bucket) {
        UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "ACTIVE"
        UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "WORKING_SET"
        UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "FREQUENT"
        UsageStatsManager.STANDBY_BUCKET_RARE -> "RARE"
        UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "RESTRICTED"
        null -> "?"
        else -> "OTHER($bucket)"
    }

    private fun whatsappStatus(context: Context): String {
        val found = WHATSAPP_PACKAGES.mapNotNull { pkg ->
            runCatching {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(pkg, 0)
                "$pkg ${info.versionName}"
            }.getOrNull()
        }
        return if (found.isEmpty()) "not-installed-or-not-visible" else found.joinToString("; ")
    }

    private fun isListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabled?.split(":")?.any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        } == true
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandOverlayService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(":").any { ComponentName.unflattenFromString(it) == expected }
    }
}
