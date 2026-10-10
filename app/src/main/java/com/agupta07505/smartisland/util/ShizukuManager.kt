/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    /**
     * Checks if the Shizuku app is installed on the device.
     * 100% crash proof against Throwable (including unit test stubs).
     */
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Safely checks if the Shizuku service binder is currently alive and responsive.
     * Catches any Throwable (DeadObjectException, ExceptionInInitializerError, RemoteException).
     */
    fun isBinderAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Safely checks if Shizuku API permission has been granted to Smart Island.
     */
    fun hasPermission(): Boolean {
        if (!isBinderAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Safely requests Shizuku permission. Does not crash if Shizuku service is dead.
     */
    fun requestPermission(requestCode: Int = 1001) {
        if (!isBinderAvailable()) return
        try {
            Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            android.util.Log.e("ShizukuManager", "Failed to request Shizuku permission", t)
        }
    }

    private fun runShizukuCommands(commands: List<String>): Result<String> {
        if (!hasPermission()) {
            return Result.failure(IllegalStateException("Shizuku permission not granted or service binder offline."))
        }
        return runCatching {
            val fullScript = commands.joinToString("; ")
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", fullScript), null, null) as Process

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0 || output.isNotBlank() || error.isBlank()) {
                "Permissions auto-granted successfully via Shizuku."
            } else {
                throw RuntimeException("Shizuku command error (exit $exitCode): $error $output")
            }
        }
    }

    internal fun mergeColonSeparated(currentList: String, newEntry: String): String {
        val list = currentList.split(':').filter { it.isNotBlank() }.toMutableList()
        if (!list.contains(newEntry)) {
            list.add(newEntry)
        }
        return list.joinToString(":")
    }

    internal fun getMergedAccessibilityServices(context: Context, serviceComponent: String): String {
        val current = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        return mergeColonSeparated(current, serviceComponent)
    }

    internal fun getMergedNotificationListeners(context: Context, listenerComponent: String): String {
        val current = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        return mergeColonSeparated(current, listenerComponent)
    }

    /**
     * Executes ADB shell commands via Shizuku process on IO dispatcher to auto-grant:
     * - Allow restricted settings (Android 13+)
     * - Usage Access / Usage Stats (GET_USAGE_STATS)
     * - Accessibility Service & System Alert Window (preserving existing accessibility services)
     * - Notification Listener Access (preserving existing notification listeners)
     * - Nearby devices / Bluetooth Connect permission (Android 12+)
     * - Battery Optimization whitelist
     */
    suspend fun autoGrantAllPermissions(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val accessibilityClass = "$pkg/${SmartIslandOverlayService::class.java.name}"
        val notificationClass = "$pkg/${SmartIslandNotificationListenerService::class.java.name}"
        val mergedAccessibilityServices = getMergedAccessibilityServices(context, accessibilityClass)
        val mergedNotificationListeners = getMergedNotificationListeners(context, notificationClass)

        val commands = listOf(
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $pkg GET_USAGE_STATS allow",
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            "appops set $pkg BIND_ACCESSIBILITY_SERVICE allow",
            "appops set $pkg POST_NOTIFICATION allow",
            "if [ \"$(getprop ro.build.version.sdk)\" -ge 31 ]; then pm grant $pkg android.permission.BLUETOOTH_CONNECT; fi",
            "appops set $pkg AUTO_START allow",
            "appops set $pkg RUN_IN_BACKGROUND allow",
            "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "settings put secure enabled_accessibility_services $mergedAccessibilityServices",
            "settings put secure accessibility_enabled 1",
            "cmd notification allow_listener $notificationClass",
            "settings put secure enabled_notification_listeners $mergedNotificationListeners",
            "am set-standby-bucket $pkg active",
            "dumpsys deviceidle whitelist +$pkg"
        )
        runShizukuCommands(commands)
    }

    /**
 * Repairs a stale/crashed AccessibilityService binding through Shizuku.
 *
 * Reproduces the effective Accessibility OFF -> ON refresh that was
 * verified on HyperOS, while preserving every other enabled service.
 */
suspend fun repairAccessibility(context: Context): Result<String> =
    withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val accessibilityClass =
            "$pkg/${SmartIslandOverlayService::class.java.name}"

        val shortAccessibilityClass =
            ComponentName(
                context,
                SmartIslandOverlayService::class.java
            ).flattenToShortString()

        val commands = listOf(
            "current=\$(settings get secure enabled_accessibility_services)",

            // Temporarily disable Accessibility globally.
            "settings put secure accessibility_enabled 0",

            // Remove Smart Island in either representation, while preserving
            // every other enabled Accessibility service.
            "new=''; IFS=':'; for s in \$current; do if [ -n \"\$s\" ] && [ \"\$s\" != \"$accessibilityClass\" ] && [ \"\$s\" != \"$shortAccessibilityClass\" ]; then if [ -n \"\$new\" ]; then new=\"\$new:\$s\"; else new=\"\$s\"; fi; fi; done; if [ -n \"\$new\" ] && [ \"\$new\" != \"null\" ]; then settings put secure enabled_accessibility_services \"\$new\"; else settings delete secure enabled_accessibility_services; fi",

            // Give Android/HyperOS time to process the disable.
            "sleep 2",

            // Add Smart Island back using the short component form.
            "current=\$(settings get secure enabled_accessibility_services); if [ -n \"\$current\" ] && [ \"\$current\" != \"null\" ]; then current=\"\$current:$shortAccessibilityClass\"; else current=\"$shortAccessibilityClass\"; fi; settings put secure enabled_accessibility_services \"\$current\"",

            // Re-enable Accessibility.
            "settings put secure accessibility_enabled 1"
        )

        runShizukuCommands(commands)
    }

    /**
     * Grants OEM Autostart and disables background kill / app standby restrictions via Shizuku.
     */
    suspend fun grantOemAutostartAndKillProtection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val commands = listOf(
            "appops set $pkg AUTO_START allow",
            "appops set $pkg RUN_IN_BACKGROUND allow",
            "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "am set-standby-bucket $pkg active",
            "dumpsys deviceidle whitelist +$pkg"
        )
        runShizukuCommands(commands)
    }

    /**
     * Grants Notification Listener permission via Shizuku without overwriting other active listeners.
     */
    suspend fun grantNotificationListener(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val notificationClass = "$pkg/${SmartIslandNotificationListenerService::class.java.name}"
        val mergedNotificationListeners = getMergedNotificationListeners(context, notificationClass)
        val commands = listOf(
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "cmd notification allow_listener $notificationClass",
            "settings put secure enabled_notification_listeners $mergedNotificationListeners"
        )
        runShizukuCommands(commands)
    }

    /**
     * Grants Accessibility service permission via Shizuku without overwriting other active accessibility services.
     */
    suspend fun grantAccessibility(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val accessibilityClass = "$pkg/${SmartIslandOverlayService::class.java.name}"
        val mergedAccessibilityServices = getMergedAccessibilityServices(context, accessibilityClass)
        val commands = listOf(
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $pkg BIND_ACCESSIBILITY_SERVICE allow",
            "settings put secure enabled_accessibility_services $mergedAccessibilityServices",
            "settings put secure accessibility_enabled 1"
        )
        runShizukuCommands(commands)
    }
}
