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
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import kotlinx.coroutines.delay

object HotspotUtil {
    /** Shell commands that start and stop Wi-Fi tethering on Android 11+. */
    internal const val START_COMMAND = "service call tethering 4 null s16 random"
    internal const val STOP_COMMAND = "service call tethering 5 s16 random"
    private const val TETHERING_WIFI = 0
    private const val DIRECT_STOP_CHECK_MS = 2_500L

    /**
     * Reads the platform flag when the ROM allows it, otherwise goes by the Hotspot card the listener shows while the
     * hotspot notification is up
     */
    fun isOn(context: Context): Boolean {
        val byFlag = quietly {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            WifiManager::class.java.getMethod("isWifiApEnabled").invoke(wifi) as Boolean
        }
        if (byFlag != null) return byFlag
        return quietly(false) {
            SmartIslandRepositories.notificationRepository(context).notifications.value
                .any { it.mode == IslandMode.Hotspot }
        }
    }

    /** Blocking: call it off the main thread. */
    suspend fun toggleWithShizuku(context: Context) {
        val command = if (isOn(context)) STOP_COMMAND else START_COMMAND
        ShizukuManager.runShellForOutput(command)
    }

    /** For the hotspot card the island posts itself, which has no button of its own. */
    fun turnOffFromCard(context: Context) {
        val shell = ShizukuManager.isBinderAvailable() && ShizukuManager.hasPermission() &&
            ShizukuMethodCheck.works(context, ShizukuMethodCheck.Method.Hotspot)
        if (shell) {
            UtilityActions.launch { ShizukuManager.runShellForOutput(STOP_COMMAND) }
            return
        }
        // No shell: try stopping tethering directly (needs "Modify system settings"), no Settings page involved
        if (quietly(false) { Settings.System.canWrite(context) } && stopTetheringDirect(context)) {
            UtilityActions.launch {
                delay(DIRECT_STOP_CHECK_MS)
                // The platform call fails silently on some ROMs, so only open Settings if the hotspot is still up
                if (HotspotSettingsAutomation.hotspotOn) turnOffViaSettings(context)
            }
            return
        }
        turnOffViaSettings(context)
    }

    private fun turnOffViaSettings(context: Context) {
        HotspotSettingsAutomation.requestTurnOff(SystemClock.elapsedRealtime())
        openHotspotSettings(context, floating = true)
    }

    /** The utility button without a shell: starts or stops the hotspot silently, Settings only if that did not work. */
    fun toggleWithoutShell(context: Context) {
        val turningOn = !isOn(context) && !HotspotSettingsAutomation.hotspotOn
        val canWrite = quietly(false) { Settings.System.canWrite(context) }
        val called = canWrite && if (turningOn) startTetheringDirect(context) else stopTetheringDirect(context)
        if (!called) {
            if (turningOn) openHotspotSettings(context, floating = true) else turnOffViaSettings(context)
            return
        }
        UtilityActions.launch {
            delay(DIRECT_STOP_CHECK_MS)
            val on = isOn(context) || HotspotSettingsAutomation.hotspotOn
            if (turningOn && !on) openHotspotSettings(context, floating = true)
            if (!turningOn && on) turnOffViaSettings(context)
        }
    }

    /**
     * Starts Wi-Fi tethering with the saved hotspot name and password (Android 11+), through the tethering manager,
     * which accepts WRITE_SETTINGS. True only means the call was made.
     */
    internal fun startTetheringDirect(context: Context): Boolean = quietly(false) {
        // Android 10 and below only offer an abstract callback class here, so those stay on the Settings page
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@quietly false
        val manager = context.applicationContext.getSystemService("tethering") ?: return@quietly false
        val callbackType = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
        val callback = java.lang.reflect.Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, _, _ -> null }
        manager.javaClass.getMethod("startTethering", Int::class.javaPrimitiveType, java.util.concurrent.Executor::class.java, callbackType)
            .invoke(manager, TETHERING_WIFI, java.util.concurrent.Executor { it.run() }, callback)
        true
    }

    /**
     * Asks the platform to stop Wi-Fi tethering without showing anything. Android 11+ goes through TetheringManager,
     * older versions through ConnectivityManager; both accept WRITE_SETTINGS. True only means the call was made.
     */
    internal fun stopTetheringDirect(context: Context): Boolean = quietly(false) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = app.getSystemService("tethering") ?: return@quietly false
            manager.javaClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
                .invoke(manager, TETHERING_WIFI)
        } else {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE)
            cm.javaClass.getMethod("stopTethering", Int::class.javaPrimitiveType)
                .invoke(cm, TETHERING_WIFI)
        }
        true
    }

    fun openHotspotSettings(context: Context, contentIntent: PendingIntent? = null, floating: Boolean = false) {
        if (contentIntent != null) {
            val sent = quietly(false) {
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
            }
            if (sent) return
        }

        val intents = listOf(
            // API 30+
            Intent("android.settings.WIFI_TETHER_SETTINGS"),
            // API 26+
            Intent("android.settings.TETHER_SETTINGS"),
            // xiaomi, HyperOS, MIUI
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.wifi.tether.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")),
            Intent("miui.intent.action.TETHER_SETTINGS"),
            // Samsung
            Intent("com.samsung.android.settings.WIFI_AP_SETTINGS"),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            // The utility asks for a floating window first, a page the phone cannot float opens full screen
            val launched = (floating && UtilityBridge.openIntentFloating?.invoke(Intent(intent)) == true)
                || quietly(false) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }
            if (launched) return
        }

        Toast.makeText(context, "Opening Hotspot settings...", Toast.LENGTH_SHORT).show()
    }
}
