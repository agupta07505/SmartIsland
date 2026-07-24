/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OemAutostartUtil {

    /**
     * Attempts to open manufacturer-specific Autostart / Background App Management screen
     * to prevent OEM power managers (MIUI Powerkeeper, ColorOS, Samsung Care, Vivo)
     * from killing background accessibility services.
     * 100% crash-proof against un-mocked JVM stubs and missing OEM intents.
     */
    fun openAutostartSettings(context: Context): Boolean {
        return try {
            val brand = (Build.BRAND ?: "").lowercase()
            val manufacturer = (Build.MANUFACTURER ?: "").lowercase()

            val intents = mutableListOf<Intent>()

            if (brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") || manufacturer.contains("xiaomi")) {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")))
            }

            if (brand.contains("oppo") || brand.contains("realme") || manufacturer.contains("oppo") || manufacturer.contains("realme")) {
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            }

            if (brand.contains("vivo") || manufacturer.contains("vivo") || brand.contains("iqoo")) {
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
            }

            if (brand.contains("samsung") || manufacturer.contains("samsung")) {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity")))
            }

            if (brand.contains("huawei") || brand.contains("honor") || manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
            }

            if (brand.contains("asus") || manufacturer.contains("asus")) {
                intents.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")))
                intents.add(Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).putExtra("flag", 1))
            }

            // Generic fallback to App Info / Battery Settings
            try {
                intents.add(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            } catch (_: Throwable) { }

            for (intent in intents) {
                if (context.safeStartActivity(intent, errorMessage = null)) {
                    return true
                }
            }

            false
        } catch (_: Throwable) {
            false
        }
    }
}
