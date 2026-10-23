/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.res.Resources
import android.os.PowerManager
import android.provider.MediaStore
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.widget.Toast
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.model.UtilityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The few things a utility needs from the overlay service. */
object UtilityBridge {
    @Volatile var collapse: (() -> Unit)? = null
    @Volatile var globalAction: ((Int) -> Boolean)? = null

    /** Opens the package in a floating window; returns whether it was started. */
    @Volatile var openFloating: ((String) -> Boolean)? = null

    /** Opens a system page (an intent) in a floating window; false when the floating window is off or the phone refuses it. */
    @Volatile var openIntentFloating: ((Intent) -> Boolean)? = null

    fun clear() {
        collapse = null
        globalAction = null
        openFloating = null
        openIntentFloating = null
    }
}

/** What happens when a utility tile is tapped. Everything here works from the overlay service's context. */
object UtilityActions {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())

    /** Runs [block] off the main thread, outliving the island's own composition (which ends when the island collapses). */
    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    const val EXTRA_OPEN_SECTION = "open_section"
    const val SECTION_APP_SHORTCUTS = "app_shortcuts"

    /**
     * Opens the app on the App Shortcuts configuration (where the pinned apps and the Utilities layout are edited).
     */
    fun openShortcutSettings(context: Context) {
        val intent = Intent(context.applicationContext, com.agupta07505.smartisland.MainActivity::class.java)
            .putExtra(EXTRA_OPEN_SECTION, SECTION_APP_SHORTCUTS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching { context.startActivity(intent) }
    }

    /**
     * Opens one of the island's pickers ([PickerRequest.APPS], [PickerRequest.UTILITY]) in its own small window,
     * without bringing the app up.
     */
    fun openPicker(context: Context, picker: String) {
        val kind = com.agupta07505.smartisland.ui.pickers.PickerKinds.forRequest(picker) ?: return
        runCatching { context.startActivity(com.agupta07505.smartisland.ui.pickers.PickerActivity.intentFor(context, kind)) }
            .onFailure { openShortcutSettings(context) }
    }

    private const val SCREENSHOT_DELAY_MS = 450L

    @Volatile var torchOn: Boolean = false
        private set
    @Volatile private var torchCallbackRegistered = false
    @Volatile var recording: Boolean = false
        private set

    /** Runs [type]. [calculatorPackage] is the remembered calculator app (blank until chosen). */
    fun run(context: Context, type: UtilityType, calculatorPackage: String = "") {
        val app = context.applicationContext
        when (type) {
            UtilityType.Flashlight -> toggleTorch(app)
            UtilityType.WiFi -> toggleWifi(app)
            UtilityType.MobileData -> toggleMobileData(app)
            UtilityType.Hotspot -> toggleHotspot(app)
            UtilityType.ClearCache -> clearBackground(app)
            UtilityType.Screenshot -> screenshot(app)
            UtilityType.LockScreen -> lockScreen(app)
            UtilityType.ScreenRecorder -> toggleScreenRecord(app)
            // The Utilities page asks for a length first (see startTimer); a bare run() has no length, so it does nothing.
            UtilityType.Timer -> Unit
            UtilityType.Stopwatch -> toggleStopwatch(app)
            UtilityType.Calculator -> if (calculatorPackage.isNotBlank()) launchApp(app, calculatorPackage, floating = true)
            UtilityType.VoiceRecorder -> startFirst(app, listOf(Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)))
            // The one utility that always opens full screen, never in a floating window.
            UtilityType.Camera -> startFull(app, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            UtilityType.Bluetooth -> toggleBluetooth(app)
            UtilityType.AirplaneMode -> toggleAirplane(app)
            UtilityType.AutoRotate -> toggleAutoRotate(app)
            UtilityType.DarkMode -> toggleDarkMode(app)
            UtilityType.BatterySaver -> toggleBatterySaver(app)
            UtilityType.AppWidget -> Unit
        }
    }

    private fun startFull(context: Context, intent: Intent): Boolean {
        val started = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (started) UtilityBridge.collapse?.invoke() else toast(context, R.string.utility_action_failed)
        return started
    }

    fun bluetoothEnabled(context: Context): Boolean = runCatching {
        (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true
    }.getOrDefault(false)

    private fun toggleBluetooth(context: Context) {
        if (shizukuUsable(context, ShizukuMethodCheck.Method.Shell)) {
            val verb = if (bluetoothEnabled(context)) "disable" else "enable"
            shell("svc bluetooth $verb || cmd bluetooth_manager $verb")
        } else {
            startFirst(context, listOf(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
        }
    }

    fun airplaneModeEnabled(context: Context): Boolean =
        runCatching { Settings.Global.getInt(context.applicationContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 }.getOrDefault(false)

    private fun toggleAirplane(context: Context) {
        if (shizukuUsable(context, ShizukuMethodCheck.Method.Shell)) {
            val on = !airplaneModeEnabled(context)
            val verb = if (on) "enable" else "disable"
            val value = if (on) 1 else 0
            shell(
                "cmd connectivity airplane-mode $verb || " +
                    "(settings put global airplane_mode_on $value; am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $on)"
            )
        } else {
            startFirst(context, listOf(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)))
        }
    }

    fun autoRotateEnabled(context: Context): Boolean =
        runCatching { Settings.System.getInt(context.applicationContext.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 }.getOrDefault(false)

    private fun toggleAutoRotate(context: Context) {
        val app = context.applicationContext
        if (!ensureCanWriteSettings(app)) return
        runCatching {
            Settings.System.putInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (autoRotateEnabled(app)) 0 else 1)
        }
    }

    /** The system's night mode (not this app's own theme). */
    fun darkModeEnabled(context: Context): Boolean =
        (Resources.getSystem().configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun toggleDarkMode(context: Context) {
        if (shizukuUsable(context, ShizukuMethodCheck.Method.Shell)) {
            shell("cmd uimode night ${if (darkModeEnabled(context)) "no" else "yes"}")
        } else {
            startFirst(context, listOf(Intent(Settings.ACTION_DISPLAY_SETTINGS)))
        }
    }

    fun batterySaverEnabled(context: Context): Boolean =
        runCatching { (context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode }.getOrDefault(false)

    private fun toggleBatterySaver(context: Context) {
        if (shizukuUsable(context, ShizukuMethodCheck.Method.Shell)) {
            shell("settings put global low_power ${if (batterySaverEnabled(context)) 0 else 1}")
        } else {
            startFirst(context, listOf(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)))
        }
    }

    fun autoBrightnessEnabled(context: Context): Boolean = runCatching {
        Settings.System.getInt(context.applicationContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }.getOrDefault(false)

    fun toggleAutoBrightness(context: Context) {
        val app = context.applicationContext
        if (!ensureCanWriteSettings(app)) return
        runCatching {
            Settings.System.putInt(
                app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (autoBrightnessEnabled(app)) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
        }
    }

    /** The ringer: [AudioManager.RINGER_MODE_NORMAL], [AudioManager.RINGER_MODE_VIBRATE] or [AudioManager.RINGER_MODE_SILENT]. */
    fun ringerMode(context: Context): Int = runCatching {
        (context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode
    }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)

    fun dndAccessGranted(context: Context): Boolean = runCatching {
        (context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .isNotificationPolicyAccessGranted
    }.getOrDefault(false)

    /** Cycles the ringer: ring, then vibrate, then silent, then ring again. */
    fun toggleRinger(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> {
                    if (dndAccessGranted(app)) {
                        try {
                            am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        } catch (_: SecurityException) {
                            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            toast(app, R.string.utility_ringer_needs_dnd)
                        }
                    } else {
                        am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        toast(app, R.string.utility_ringer_needs_dnd)
                    }
                }
                else -> am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }
    }

    /** Calculator apps on the device: the system default first, then any launcher app named like a calculator. */
    fun calculatorApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, LaunchableApp>()
        runCatching {
            val selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR)
            pm.queryIntentActivities(selector, 0).forEach { info ->
                found.putIfAbsent(info.activityInfo.packageName, LaunchableApp(info.activityInfo.packageName, info.loadLabel(pm).toString()))
            }
        }
        runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                if (pkg != context.packageName && (pkg.contains("calc", true) || label.contains("calc", true))) {
                    found.putIfAbsent(pkg, LaunchableApp(pkg, label))
                }
            }
        }
        return found.values.toList()
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        packageName.isNotBlank() && context.packageManager.getLaunchIntentForPackage(packageName) != null

    /** Opens [packageName], in the floating window when [floating] and the floating-window setting allows it. */
    fun launchApp(context: Context, packageName: String, floating: Boolean) {
        if (floating && UtilityBridge.openFloating?.invoke(packageName) == true) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            toast(context, R.string.utility_app_missing)
            return
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        UtilityBridge.collapse?.invoke()
    }

    /** Starts following the torch so [torchOn] is right before the first tap. Safe to call repeatedly. */
    fun watchTorch(context: Context) {
        if (torchCallbackRegistered) return
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        runCatching {
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
            }, main)
            torchCallbackRegistered = true
        }
    }

    private fun toggleTorch(context: Context) {
        watchTorch(context)
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        runCatching {
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: run { toast(context, R.string.utility_no_flash); return }
            cm.setTorchMode(id, !torchOn)
        }
    }

    private fun shizukuReady(): Boolean = ShizukuManager.isBinderAvailable() && ShizukuManager.hasPermission()

    private fun shizukuUsable(context: Context, method: ShizukuMethodCheck.Method): Boolean =
        ShizukuMethodCheck.usable(context, method)

    private fun shell(command: String) {
        scope.launch { ShizukuManager.runShellForOutput(command) }
    }

    fun wifiEnabled(context: Context): Boolean =
        runCatching { (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun toggleWifi(context: Context) {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        val enable = !wm.isWifiEnabled
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> runCatching { wm.isWifiEnabled = enable }
            shizukuUsable(context, ShizukuMethodCheck.Method.WifiData) -> shell(if (enable) "svc wifi enable" else "svc wifi disable")
            else -> openPanel(context, Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
        }
    }

    /** Whether mobile data is on. */
    fun mobileDataEnabled(context: Context): Boolean {
        val app = context.applicationContext
        val subId = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
            .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            val perSim = runCatching { Settings.Global.getInt(app.contentResolver, "mobile_data$subId") }.getOrNull()
            if (perSim != null) return perSim == 1
        }
        return runCatching { Settings.Global.getInt(app.contentResolver, "mobile_data", 1) == 1 }.getOrDefault(true)
    }

    private fun toggleMobileData(context: Context) {
        if (shizukuUsable(context, ShizukuMethodCheck.Method.WifiData)) {
            val verb = if (mobileDataEnabled(context)) "disable" else "enable"
            // `svc data` is tried first; the telephony service's own command is the fallback on ROMs where it does nothing.
            shell("svc data $verb || cmd phone data $verb")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openPanel(context, Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_DATA_ROAMING_SETTINGS)
        } else {
            startFirst(context, listOf(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS)))
        }
    }

    private fun openPanel(context: Context, panelAction: String, fallbackAction: String) {
        val shown = runCatching { context.startActivity(Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (shown) {
            UtilityBridge.collapse?.invoke()
            return
        }
        startFirst(context, listOf(Intent(fallbackAction)))
    }

    private fun clearBackground(context: Context) {
        scope.launch {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val pm = context.packageManager
            val home = runCatching {
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0).map { it.activityInfo.packageName }.toSet()
            }.getOrDefault(emptySet())
            var stopped = 0
            if (am != null) {
                runCatching {
                    pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                        .map { it.activityInfo.packageName }
                        .distinct()
                        .filter { it != context.packageName && it !in home }
                        .forEach { pkg -> am.killBackgroundProcesses(pkg); stopped++ }
                }
            }
            runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
            if (shizukuUsable(context, ShizukuMethodCheck.Method.TrimCache)) ShizukuManager.runShellForOutput("pm trim-caches 999G")
            main.post { toast(context, R.string.utility_cache_cleared) }
        }
        UtilityBridge.collapse?.invoke()
    }

    private fun screenshot(context: Context) {
        UtilityBridge.collapse?.invoke()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) { toast(context, R.string.utility_needs_android_9); return }
        main.postDelayed({
            if (UtilityBridge.globalAction?.invoke(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) != true) {
                toast(context, R.string.utility_action_failed)
            }
        }, SCREENSHOT_DELAY_MS)
    }

    private fun lockScreen(context: Context) {
        UtilityBridge.collapse?.invoke()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) { toast(context, R.string.utility_needs_android_9); return }
        if (UtilityBridge.globalAction?.invoke(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) != true) {
            toast(context, R.string.utility_action_failed)
        }
    }

    private fun toggleScreenRecord(context: Context) {
        if (!shizukuUsable(context, ShizukuMethodCheck.Method.ScreenRecorder)) {
            UtilityBridge.collapse?.invoke()
            main.postDelayed({
                if (UtilityBridge.globalAction?.invoke(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) == true) {
                    toast(context, R.string.utility_recorder_use_tile)
                }
            }, SCREENSHOT_DELAY_MS)
            return
        }
        if (recording) {
            recording = false
            shell("pkill -2 screenrecord")
        } else {
            recording = true
            // The folder comes from the system, not a typed-in "/sdcard" path (which lint rejects).
            val movies = Environment.getExternalStorageDirectory().path + "/Movies"
            shell(
                "mkdir -p $movies && nohup screenrecord --time-limit 180 " +
                    "$movies/SmartIsland_\$(date +%Y%m%d_%H%M%S).mp4 >/dev/null 2>&1 &"
            )
            main.postDelayed({ recording = false }, 180_000L)
            UtilityBridge.collapse?.invoke()
        }
    }

    private fun toggleHotspot(context: Context) {
        if (shizukuReady() && ShizukuMethodCheck.works(context, ShizukuMethodCheck.Method.Hotspot)) {
            scope.launch { HotspotUtil.toggleWithShizuku(context) }
        } else {
            HotspotUtil.openHotspotSettings(context, floating = true)
        }
        UtilityBridge.collapse?.invoke()
    }

    /** Lengths offered by the Timer tile, in minutes. */
    val TIMER_CHOICES_MINUTES: List<Int> = listOf(1, 5, 10, 15, 30)

    /**
     * Starts the island's own countdown of [seconds]; no clock app is involved. The island closes so the cutout shows
     * the countdown.
     */
    fun startTimer(context: Context, seconds: Int) {
        InAppTimer.start(context, seconds.coerceIn(1, 24 * 60 * 60))
        UtilityBridge.collapse?.invoke()
    }

    private fun toggleStopwatch(context: Context) {
        val wasActive = InAppStopwatch.isActive
        InAppStopwatch.toggle(context)
        if (!wasActive) UtilityBridge.collapse?.invoke()
    }

    private fun startFirst(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            val started = UtilityBridge.openIntentFloating?.invoke(Intent(intent)) == true ||
                runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
            if (started) {
                UtilityBridge.collapse?.invoke()
                return
            }
        }
        toast(context, R.string.utility_action_failed)
    }

    private fun toast(context: Context, res: Int) {
        main.post { Toast.makeText(context, res, Toast.LENGTH_SHORT).show() }
    }

    private const val BRIGHTNESS_MAX = 255
    private const val BRIGHTNESS_MIN = 1
    @Volatile private var lastWritePromptMs = 0L

    private fun ensureCanWriteSettings(app: Context): Boolean {
        if (Settings.System.canWrite(app)) return true
        val now = System.currentTimeMillis()
        if (now - lastWritePromptMs > 4000L) {
            lastWritePromptMs = now
            toast(app, R.string.utility_brightness_permission)
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${app.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startFirst(app, listOf(intent))
        }
        return false
    }

    /** Screen brightness as 0..1 (the stored 0..255 value). */
    fun brightnessFraction(context: Context): Float {
        val raw = runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
            .getOrDefault(BRIGHTNESS_MAX / 2)
        return (raw.coerceIn(0, BRIGHTNESS_MAX)) / BRIGHTNESS_MAX.toFloat()
    }

    /** Sets the screen brightness from a 0..1 [fraction] (never fully black). */
    fun setBrightness(context: Context, fraction: Float) {
        val app = context.applicationContext
        if (!ensureCanWriteSettings(app)) return
        val value = (fraction.coerceIn(0f, 1f) * BRIGHTNESS_MAX).toInt().coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)
        runCatching {
            val resolver = app.contentResolver
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) !=
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            }
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, -1) != value) {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
            }
        }
    }

    /** Media volume as 0..1. */
    fun volumeFraction(context: Context): Float = runCatching {
        val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        am.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }.getOrDefault(0f)

    /** Sets the media volume from a 0..1 [fraction], without the system's volume panel. */
    fun setVolume(context: Context, fraction: Float) {
        runCatching {
            val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val level = Math.round(fraction.coerceIn(0f, 1f) * max).coerceIn(0, max)
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) != level) am.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        }
    }
}
