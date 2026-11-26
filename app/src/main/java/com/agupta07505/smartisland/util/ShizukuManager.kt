/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

private const val TAG = "ShizukuManager"

object ShizukuManager {

    private val KNOWN_PACKAGES = listOf("moe.shizuku.privileged.api")

    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    private fun startShellProcess(command: String): Process =
        newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

    private data class ProcessOutput(
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    )

    private fun Process.captureOutput(): ProcessOutput {
        val stdout = CompletableFuture.supplyAsync {
            inputStream.bufferedReader().use { it.readText() }
        }
        val stderr = CompletableFuture.supplyAsync {
            errorStream.bufferedReader().use { it.readText() }
        }
        val exitCode = waitFor()
        return ProcessOutput(stdout.get(), stderr.get(), exitCode)
    }

    private fun Process.drainErrorStream() {
        thread(name = "Shizuku-stderr", isDaemon = true) {
            errorStream.use { stream ->
                val buffer = ByteArray(4 * 1024)
                while (stream.read(buffer) >= 0) Unit
            }
        }
    }

    /**
     * Package that declares Shizuku's API permission, or null. Any app declaring it counts (official app, fork or
     * renamed build)
     */
    fun findPackageName(context: Context): String? = quietly {
        context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0).packageName
    }

    fun isInstalled(context: Context): Boolean {
        if (findPackageName(context) != null) return true
        val byName = KNOWN_PACKAGES.any { name ->
            try {
                context.packageManager.getPackageInfo(name, 0)
                true
            } catch (t: Throwable) {
                false
            }
        }
        return byName || isBinderAvailable()
    }

    /** Catches any Throwable, including DeadObjectException and RemoteException. */
    fun isBinderAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isBinderAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        if (!isBinderAvailable()) return
        try {
            Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            AppLogRecorder.e(TAG, "Shizuku permission request failed", t)
        }
    }

    private fun runShizukuCommands(commands: List<String>): Result<String> {
        if (!hasPermission()) {
            return Result.failure(IllegalStateException("Shizuku permission not granted or service binder offline."))
        }
        return runCatching {
            val fullScript = commands.joinToString("; ")
            val process = startShellProcess(fullScript)
            val result = process.captureOutput()

            if (result.exitCode == 0) {
                "Permissions auto-granted successfully via Shizuku."
            } else {
                throw RuntimeException(
                    "Shizuku command failed (exit ${result.exitCode}): ${result.stderr} ${result.stdout}".trim()
                )
            }
        }
    }

    /**
     * Runs one shell command through Shizuku and returns its stdout. For read-only dumps such as `dumpsys`, fails when
     * Shizuku is not running or not permitted
     */
    suspend fun runShellForOutput(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext Result.failure(IllegalStateException("Shizuku permission not granted or service binder offline."))
        }
        runCatching {
            val result = startShellProcess(command).captureOutput()
            if (result.exitCode != 0) {
                throw RuntimeException(
                    "Shizuku command failed (exit ${result.exitCode}): ${result.stderr}".trim()
                )
            }
            result.stdout
        }
    }

    /**
     * Starts one shell command through Shizuku and returns the running process, so output can be read line by line
     * while it runs (a `logcat` follow)
     */
    fun openShellStream(command: String): Process? {
        if (!hasPermission()) return null
        return quietly {
            startShellProcess(command).also { it.drainErrorStream() }
        }
    }

    /**
     * Blocking. Runs 1 shell command through Shizuku and returns its raw stdout bytes (at most [maxBytes], else
     * null). Reads a picture file the app itself may not open
     */
    fun runShellForBytes(command: String, maxBytes: Int): ByteArray? {
        if (!hasPermission()) return null
        return quietly {
            require(maxBytes >= 0) { "maxBytes must not be negative" }
            val process = startShellProcess(command)
            val stderrDrain = CompletableFuture.runAsync {
                process.errorStream.use { it.copyTo(ByteArrayOutputStream()) }
            }
            val out = ByteArrayOutputStream()
            var tooBig = false
            process.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (out.size().toLong() + read > maxBytes.toLong()) {
                        tooBig = true
                        break
                    }
                    out.write(buffer, 0, read)
                }
            }
            if (tooBig) {
                process.destroy()
                process.inputStream.close()
                process.waitFor()
                stderrDrain.get()
            } else {
                val exitCode = process.waitFor()
                stderrDrain.get()
                if (exitCode != 0) throw RuntimeException("Shizuku command failed (exit $exitCode).")
            }
            if (tooBig) null else out.toByteArray()
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
        val current = quietly {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.orEmpty()
        return mergeColonSeparated(current, serviceComponent)
    }

    internal fun getMergedNotificationListeners(context: Context, listenerComponent: String): String {
        val current = quietly {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.orEmpty()
        return mergeColonSeparated(current, listenerComponent)
    }

    /** How much the 1-tap setup grants: only what the island needs, or that plus every optional permission. */
    enum class SetupScope { Necessary, All }

    private const val SETUP_PREFS = "shizuku_setup"
    private const val SETUP_SCOPE_KEY = "scope"

    fun savedScope(context: Context): SetupScope? = quietly {
        val name = context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE).getString(SETUP_SCOPE_KEY, null)
        SetupScope.entries.firstOrNull { it.name == name }
    }

    private fun saveScope(context: Context, scope: SetupScope) {
        try {
            context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE).edit()
                .putString(SETUP_SCOPE_KEY, scope.name).apply()
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "Could not save the setup scope", e)
        }
    }

    internal fun setupCommands(
        scope: SetupScope,
        pkg: String,
        accessibilityClass: String,
        notificationClass: String,
        mergedAccessibilityServices: String,
        mergedNotificationListeners: String
    ): List<String> {
        val necessary = listOf(
            // Accessibility and notification access cannot be switched on while this is still restricted
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            // Modify system settings (the brightness slider on the Utilities page)
            "appops set $pkg WRITE_SETTINGS allow",
            "appops set $pkg BIND_ACCESSIBILITY_SERVICE allow",
            "appops set $pkg POST_NOTIFICATION allow",
            "appops set $pkg AUTO_START allow",
            "appops set $pkg RUN_IN_BACKGROUND allow",
            "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "settings put secure enabled_accessibility_services $mergedAccessibilityServices",
            "settings put secure accessibility_enabled 1",
            "cmd notification allow_listener $notificationClass",
            "settings put secure enabled_notification_listeners $mergedNotificationListeners",
            "am set-standby-bucket $pkg active",
            "dumpsys deviceidle whitelist +$pkg"
        ) + runtimePermissionCommands(pkg) + sensitiveNotificationCommands(pkg)
        return if (scope == SetupScope.All) necessary + optionalCommands(pkg) else necessary
    }

    /** Permissions for optional features, separate from the required setup. */
    internal fun optionalCommands(pkg: String): List<String> = listOf(
        "appops set $pkg GET_USAGE_STATS allow",
        "cmd notification allow_dnd $pkg",
        "pm grant $pkg android.permission.ACCESS_FINE_LOCATION",
        "pm grant $pkg android.permission.ACCESS_COARSE_LOCATION"
    )

    /**
     * Repairs a stale or crashed AccessibilityService binding by switching Accessibility off and on, keeping every
     * other enabled service
     */
    suspend fun repairAccessibility(context: Context): Result<String> =
        withContext(Dispatchers.IO) {
            val pkg = context.packageName
            val accessibilityClass =
                "$pkg/${AppComponents.OVERLAY_SERVICE}"

            val shortAccessibilityClass =
                ComponentName(context.packageName, AppComponents.OVERLAY_SERVICE).flattenToShortString()

            val commands = listOf(
                "current=\$(settings get secure enabled_accessibility_services)",

                "settings put secure accessibility_enabled 0",

                // remove Smart Island in either representation, keeping every other enabled service
                "new=''; " +
                    "IFS=':'; " +
                    "for s in \$current; " +
                    "do if [ -n \"\$s\" ] && [ \"\$s\" != \"$accessibilityClass\" ] && [ \"\$s\" != \"$shortAccessibilityClass\" ]; " +
                    "then if [ -n \"\$new\" ]; " +
                    "then new=\"\$new:\$s\"; " +
                    "else new=\"\$s\"; " +
                    "fi; " +
                    "fi; " +
                    "done; " +
                    "if [ -n \"\$new\" ] && [ \"\$new\" != \"null\" ]; " +
                    "then settings put secure enabled_accessibility_services \"\$new\"; " +
                    "else settings delete secure enabled_accessibility_services; " +
                    "fi",

                // Give Android/HyperOS time to process the disable
                "sleep 2",

                "current=\$(settings get secure enabled_accessibility_services); " +
                    "if [ -n \"\$current\" ] && [ \"\$current\" != \"null\" ]; " +
                    "then current=\"\$current:$shortAccessibilityClass\"; " +
                    "else current=\"$shortAccessibilityClass\"; " +
                    "fi; " +
                    "settings put secure enabled_accessibility_services \"\$current\"",

                "settings put secure accessibility_enabled 1"
            )

            runShizukuCommands(commands)
        }

    suspend fun autoGrantNecessaryPermissions(context: Context): Result<String> = grant(context, SetupScope.Necessary)

    suspend fun autoGrantAllPermissions(context: Context): Result<String> = grant(context, SetupScope.All)

    /** Applies the scope the user chose last again (the necessary one when none yet). */
    suspend fun reapplySavedSetup(context: Context): Result<String> =
            grant(context, savedScope(context) ?: SetupScope.Necessary)

    private suspend fun grant(context: Context, scope: SetupScope): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val accessibilityClass = "$pkg/${AppComponents.OVERLAY_SERVICE}"
        val notificationClass = "$pkg/${AppComponents.LISTENER_SERVICE}"
        val commands = setupCommands(
            scope = scope,
            pkg = pkg,
            accessibilityClass = accessibilityClass,
            notificationClass = notificationClass,
            mergedAccessibilityServices = getMergedAccessibilityServices(context, accessibilityClass),
            mergedNotificationListeners = getMergedNotificationListeners(context, notificationClass)
        )
        runShizukuCommands(commands).onSuccess { saveScope(context, scope) }
    }

    /**
     * Runtime permissions of the necessary setup: nearby devices (Bluetooth connect, for the Bluetooth cards) and
     * notifications
     */
    internal fun runtimePermissionCommands(pkg: String): List<String> = listOf(
        "pm grant $pkg android.permission.BLUETOOTH_CONNECT",
        "pm grant $pkg android.permission.POST_NOTIFICATIONS"
    )

    /** Permissions the necessary setup never grants, only the "all permissions" setup does (see [optionalCommands]). */
    internal val OPTIONAL_PERMISSIONS = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    internal fun sensitiveNotificationCommands(pkg: String): List<String> = listOf(
        "pm grant $pkg android.permission.RECEIVE_SENSITIVE_NOTIFICATIONS",
        "appops set $pkg RECEIVE_SENSITIVE_NOTIFICATIONS allow"
    )

    /**
     * True when this app may read notifications Android marked as sensitive: the permission or the app op of the same
     * name is granted ([sensitiveNotificationCommands] sets one each, a ROM may honour only one)
     */
    fun isSensitiveNotificationAccessAllowed(context: Context): Boolean {
        val byPermission = context.checkSelfPermission("android.permission.RECEIVE_SENSITIVE_NOTIFICATIONS") ==
            PackageManager.PERMISSION_GRANTED
        if (byPermission) return true
        // The string form of the check needs Android 10, an unknown operation name throws, which here just means "not
        // allowed"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return quietly(false) {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.unsafeCheckOpNoThrow(
                "android:receive_sensitive_notifications",
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }
    }

    /**
     * Shell script that force-stops this app and starts it again. It runs inside Shizuku's shell, not this process, so
     * it keeps going after the force-stop kills the app
     */
    internal fun restartCommands(
        pkg: String,
        launcherActivity: String,
        accessibilityComponent: String,
        notificationComponent: String,
        accessibilityWasEnabled: Boolean,
        notificationWasEnabled: Boolean,
        mergedAccessibilityServices: String
    ): List<String> = buildList {
        add("am force-stop $pkg")
        if (accessibilityWasEnabled) {
            add("settings put secure enabled_accessibility_services $mergedAccessibilityServices")
            add("settings put secure accessibility_enabled 1")
        }
        if (notificationWasEnabled) add("cmd notification allow_listener $notificationComponent")
        add("sleep 1")
        add("am start -n $pkg/$launcherActivity")
    }

    /**
     * Grants the sensitive-notification access, then force-stops and relaunches so the listener picks it up without the
     * user restarting by hand
     */
    suspend fun grantSensitiveNotificationAccessAndRestart(
        context: Context,
        onGranted: suspend () -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val granted = runShizukuCommands(sensitiveNotificationCommands(pkg))
        if (granted.isFailure) return@withContext granted

        val accessibilityComponent = "$pkg/${AppComponents.OVERLAY_SERVICE}"
        val notificationComponent = "$pkg/${AppComponents.LISTENER_SERVICE}"
        val enabledAccessibility = quietly {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.orEmpty()
        val enabledListeners = quietly {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.orEmpty()
        val script = restartCommands(
            pkg = pkg,
            launcherActivity = AppComponents.MAIN_ACTIVITY,
            accessibilityComponent = accessibilityComponent,
            notificationComponent = notificationComponent,
            accessibilityWasEnabled = enabledAccessibility.split(':').contains(accessibilityComponent),
            notificationWasEnabled = enabledListeners.split(':').contains(notificationComponent),
            mergedAccessibilityServices = mergeColonSeparated(enabledAccessibility, accessibilityComponent)
        )
        onGranted()
        runShizukuCommands(script)
    }

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
}
