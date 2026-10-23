/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * What a tap on the media card's output chip does.
 */
object MediaOutputSwitcher {
    private const val ACTION = "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG"
    private const val ACTION_SYSTEM = "com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG"
    private const val SYSTEMUI = "com.android.systemui"
    private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$")

    /** The shell command for [packageName], or null when the name is not a plain package name (nothing is quoted into a shell). */
    fun broadcastCommand(packageName: String): String? {
        if (packageName.length > 255 || !PACKAGE_NAME.matches(packageName)) return null
        return "am broadcast -a $ACTION -p $SYSTEMUI --es package_name $packageName 2>&1"
    }

    fun systemBroadcastCommand(): String = "am broadcast -a $ACTION_SYSTEM -p $SYSTEMUI 2>&1"

    fun broadcastSucceeded(output: String): Boolean =
        output.contains("Broadcast completed", ignoreCase = true) && !output.contains("Exception", ignoreCase = true)

    private fun directIntent(packageName: String?): Intent {
        val usable = packageName?.takeIf { it.length <= 255 && PACKAGE_NAME.matches(it) }
        return Intent(if (usable != null) ACTION else ACTION_SYSTEM).setPackage(SYSTEMUI).apply {
            if (usable != null) putExtra("package_name", usable)
        }
    }

    /**
     * 1. Direct broadcast, when SystemUI's receiver can be seen (it is not on skins that rename it or hide the package).
     * 2. Shizuku shell broadcast (the shell can send it whatever the skin exports).
     * 3. The volume panel / Bluetooth settings.
     * Returns what was used.
     */
    suspend fun open(context: Context, packageName: String): Used {
        val direct = directIntent(packageName)
        val receiverVisible = runCatchingLogged("MediaOutputSwitcher", "receiver lookup failed") {
            context.packageManager.queryBroadcastReceivers(direct, 0).isNotEmpty()
        } == true
        if (receiverVisible) {
            val sent = runCatchingLogged("MediaOutputSwitcher", "direct broadcast failed") {
                context.sendBroadcast(direct)
                true
            } == true
            if (sent) return Used.SystemDialog
        }
        val command = broadcastCommand(packageName) ?: systemBroadcastCommand()
        if (ShizukuManager.hasPermission()) {
            val out = ShizukuManager.runShellForOutput(command).getOrNull()
            if (out != null && broadcastSucceeded(out)) return Used.SystemDialog
        }
        return if (openFallback(context)) Used.Panel else Used.Nothing
    }

    private fun openFallback(context: Context): Boolean {
        val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_VOLUME) else null
        val bluetooth = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        return if (panel != null) context.safeStartActivity(panel, errorMessage = null, fallbackIntent = bluetooth)
        else context.safeStartActivity(bluetooth)
    }

    enum class Used { SystemDialog, Panel, Nothing }
}
