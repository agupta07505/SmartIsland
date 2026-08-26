/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object HotspotUtil {
    fun parseDeviceCount(title: String?, text: String?): Int {
        val fullText = "${title.orEmpty()} ${text.orEmpty()}"
        val lower = fullText.lowercase()

        if (lower.contains("no device") || lower.contains("0 device") || lower.contains("no connected") || lower.contains("0 connected")) {
            return 0
        }

        // Pattern 1: "1 device", "2 devices", "1 connected", "2 clients"
        val pattern1 = Regex("""\b(\d+)\s*(?:device|connected|client)s?\b""", RegexOption.IGNORE_CASE)
        pattern1.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Pattern 2: "devices: 1", "connected: 2", "clients: 0"
        val pattern2 = Regex("""\b(?:devices?|connected|clients?)\s*[:=]?\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        pattern2.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Pattern 3: "1 connected device"
        val pattern3 = Regex("""\b(\d+)\s+connected\s+devices?\b""", RegexOption.IGNORE_CASE)
        pattern3.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        return 0
    }

    /**
     * Opens the device's Hotspot & Tethering configuration page.
     * Uses contentIntent if available, otherwise attempts platform and OEM specific tethering intents.
     */
    fun openHotspotSettings(context: Context, contentIntent: PendingIntent? = null) {
        if (contentIntent != null) {
            val sent = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                    contentIntent.send(context, 0, null, null, null, null, options)
                } else {
                    try {
                        contentIntent.send(context, 0, null)
                    } catch (e: Exception) {
                        contentIntent.send()
                    }
                }
                true
            }.getOrDefault(false)
            if (sent) return
        }

        val intents = listOf(
            // 1. Android Standard WiFi Tethering Settings (API 30+)
            Intent("android.settings.WIFI_TETHER_SETTINGS"),
            // 2. Android Standard Tethering Settings (API 26+)
            Intent("android.settings.TETHER_SETTINGS"),
            // 3. Xiaomi / HyperOS / MIUI Component Tethering
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.wifi.tether.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")),
            Intent("miui.intent.action.TETHER_SETTINGS"),
            // 4. Samsung Hotspot Settings
            Intent("com.samsung.android.settings.WIFI_AP_SETTINGS"),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")),
            // 5. General Wireless / Network Settings Fallbacks
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            val launched = runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return
        }

        Toast.makeText(context, "Opening Hotspot settings...", Toast.LENGTH_SHORT).show()
    }
}
