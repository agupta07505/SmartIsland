/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.buddy.runtime.MascotEvents
import com.agupta07505.smartisland.buddy.runtime.MascotMoment
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.HotspotApState
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.util.runCatchingLogged

class SystemEventReceiver(
    private val notificationRepository: INotificationRepository,
    private val settingsProvider: () -> com.agupta07505.smartisland.data.SmartIslandSettings = { com.agupta07505.smartisland.data.SmartIslandSettings.Default }
) : BroadcastReceiver() {

    private var lastBatteryPct: Int = -1
    private var lastBatteryTitle: String? = null
    private var isCurrentlyCharging: Boolean = false

    private var activeBluetoothAddress: String? = null
    private var lastBluetoothAddress: String? = null
    private var lastBluetoothConnectedTime: Long = 0L

    companion object {
        private const val BLUETOOTH_DEBOUNCE_MS = 30_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatchingLogged("SystemEventReceiver", "Broadcast callback failed") {
            android.util.Log.d("SystemEventReceiver", "onReceive action=${intent.action}")
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val deviceName = try {
                        device?.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Device"
                    } catch (e: SecurityException) {
                        "Bluetooth Device"
                    }

                    // Suppress smartwatches, health trackers, and peripherals from hijacking the Island with connection alerts
                    if (isIgnoredBluetoothDevice(device, deviceName)) {
                        android.util.Log.d("SystemEventReceiver", "Ignored Bluetooth device by wearable/peripheral filter")
                        return@runCatchingLogged
                    }

                    val address = runCatching { device?.address }.getOrNull()
                    val now = System.currentTimeMillis()

                    // Debounce: If this device is already active or reconnected within the debounce window,
                    // do not auto-expand the Island to prevent repetitive popup loops.
                    val isRecentReconnection = address != null &&
                        address == lastBluetoothAddress &&
                        lastBluetoothConnectedTime > 0L &&
                        (now - lastBluetoothConnectedTime < BLUETOOTH_DEBOUNCE_MS)
                    val autoExpand = !isRecentReconnection

                    activeBluetoothAddress = address
                    lastBluetoothAddress = address
                    lastBluetoothConnectedTime = now

                    var batteryLevel = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    if (batteryLevel !in 0..100) {
                        batteryLevel = runCatching {
                            val method = device?.javaClass?.getMethod("getBatteryLevel")
                            (method?.invoke(device) as? Int) ?: -1
                        }.getOrDefault(-1)
                    }
                    val statusText = if (batteryLevel in 0..100) {
                        "Connected • $batteryLevel%"
                    } else {
                        "Connected"
                    }
                    notificationRepository.postNotification(
                        IslandNotification(
                            key = "system_bluetooth",
                            packageName = "com.android.bluetooth",
                            appName = "Bluetooth",
                            title = deviceName,
                            text = statusText,
                            mode = IslandMode.Bluetooth,
                            progress = if (batteryLevel in 0..100) batteryLevel else 0,
                            progressMax = 100,
                            timeMillis = System.currentTimeMillis()
                        ),
                        autoExpand = autoExpand
                    )
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                    val batteryLevel = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    if (batteryLevel in 0..100) {
                        val existing = notificationRepository.notifications.value.find { it.key == "system_bluetooth" }
                        if (existing != null) {
                            notificationRepository.postNotification(
                                existing.copy(
                                    text = "Connected • $batteryLevel%",
                                    progress = batteryLevel,
                                    progressMax = 100
                                ),
                                autoExpand = false
                            )
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val disconnectedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val disconnectedAddress = runCatching { disconnectedDevice?.address }.getOrNull()

                    // Only dismiss the Bluetooth notification if the device that disconnected
                    // is our currently active Bluetooth device (or address could not be determined).
                    // This avoids removing the notification when a smartwatch/peripheral disconnects
                    // while audio earbuds are connected.
                    if (activeBluetoothAddress == null || disconnectedAddress == null || disconnectedAddress == activeBluetoothAddress) {
                        activeBluetoothAddress = null
                        notificationRepository.removeNotification("system_bluetooth")
                    }
                }
                HotspotUtil.ACTION_WIFI_AP_STATE_CHANGED -> {
                    onHotspotState(HotspotUtil.apState(intent.getIntExtra(HotspotUtil.EXTRA_WIFI_AP_STATE, -1)))
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    MascotEvents.post(MascotMoment.Charging)
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    isCurrentlyCharging = true
                    updateBatteryIsland(context, intent, autoExpand = true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    isCurrentlyCharging = false
                    updateBatteryState(context, intent, autoExpand = false)
                }
                Intent.ACTION_BATTERY_LOW -> {
                    MascotEvents.post(MascotMoment.BatteryLow)
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    updateBatteryState(context, intent, autoExpand = true)
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    updateBatteryState(context, intent, autoExpand = false)
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isPowerSave = powerManager?.isPowerSaveMode == true
                    updateBatteryState(context, intent, autoExpand = isPowerSave)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    if (!settingsProvider().enableBatteryMode) {
                        notificationRepository.removeNotification("system_battery")
                        return@runCatchingLogged
                    }
                    val charging = isCharging(intent)
                    if (charging != isCurrentlyCharging) {
                        isCurrentlyCharging = charging
                        if (charging) {
                            updateBatteryIsland(context, intent, autoExpand = true)
                        } else {
                            updateBatteryState(context, intent, autoExpand = false)
                        }
                    } else if (charging) {
                        updateBatteryIsland(context, intent, autoExpand = false)
                    } else {
                        updateBatteryState(context, intent, autoExpand = false)
                    }
                }
            }
        }
    }

    /**
     * Rule_Book 4.8. On: the island shows the hotspot even when the OS posts no notification for it (an app's
     * own hotspot notification, if any, stands in for ours). Off: every hotspot alert leaves, including a
     * tethering notification the system has not withdrawn yet.
     */
    private fun onHotspotState(state: HotspotApState) {
        when (state) {
            HotspotApState.On -> {
                val shown = notificationRepository.notifications.value.any {
                    it.mode == IslandMode.Hotspot && !it.key.startsWith("demo_")
                }
                if (!shown) {
                    notificationRepository.postNotification(
                        IslandNotification(
                            key = HotspotUtil.SYSTEM_HOTSPOT_KEY,
                            packageName = "com.android.settings",
                            appName = "Hotspot",
                            title = "Hotspot",
                            text = "On",
                            mode = IslandMode.Hotspot,
                            timeMillis = System.currentTimeMillis()
                        ),
                        autoExpand = false
                    )
                }
            }
            HotspotApState.Off -> {
                notificationRepository.notifications.value
                    .filter { it.mode == IslandMode.Hotspot && !it.key.startsWith("demo_") }
                    .forEach { notificationRepository.removeNotification(it.key) }
            }
            HotspotApState.Changing -> Unit
        }
    }

    private fun isCharging(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getBatteryIntent(context: Context, batteryIntent: Intent?): Intent? {
        return batteryIntent?.takeIf { it.hasExtra(BatteryManager.EXTRA_LEVEL) } ?: runCatchingLogged("SystemEventReceiver", "registerReceiver BATTERY_CHANGED failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
        }
    }

    private fun updateBatteryState(context: Context, intent: Intent?, autoExpand: Boolean) {
        val intentToUse = getBatteryIntent(context, intent)
        val level = intentToUse?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intentToUse?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0 && level >= 0) (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100) else 20

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode == true
        val isLowBattery = batteryPct <= 20 || intent?.action == Intent.ACTION_BATTERY_LOW

        if (isCurrentlyCharging) {
            updateBatteryIsland(context, intentToUse, autoExpand)
            return
        }

        if (isPowerSave) {
            val title = "Battery Saver ON"
            if (!autoExpand && batteryPct == lastBatteryPct && title == lastBatteryTitle) return
            lastBatteryPct = batteryPct
            lastBatteryTitle = title

            notificationRepository.postNotification(
                IslandNotification(
                    key = "system_battery",
                    packageName = "com.android.systemui",
                    appName = "System",
                    title = title,
                    text = "$batteryPct%",
                    category = "battery_saver",
                    mode = IslandMode.Battery,
                    timeMillis = System.currentTimeMillis()
                ),
                autoExpand = autoExpand
            )
        } else if (isLowBattery) {
            val title = "Low Battery"
            if (!autoExpand && batteryPct == lastBatteryPct && title == lastBatteryTitle) return
            lastBatteryPct = batteryPct
            lastBatteryTitle = title

            notificationRepository.postNotification(
                IslandNotification(
                    key = "system_battery",
                    packageName = "com.android.systemui",
                    appName = "System",
                    title = title,
                    text = "$batteryPct%",
                    category = "battery_low",
                    mode = IslandMode.Battery,
                    timeMillis = System.currentTimeMillis()
                ),
                autoExpand = autoExpand
            )
        } else {
            lastBatteryPct = -1
            lastBatteryTitle = null
            notificationRepository.removeNotification("system_battery")
        }
    }

    private fun updateBatteryIsland(context: Context, batteryIntent: Intent?, autoExpand: Boolean) {
        val intentToUse = getBatteryIntent(context, batteryIntent)
        val level = intentToUse?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intentToUse?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        if (level < 0 || scale <= 0) return
        val batteryPct = (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        val status = intentToUse?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val title = when (status) {
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            else -> "Charging"
        }

        // Skip redundant posts when both the percentage and title haven't changed.
        if (!autoExpand && batteryPct == lastBatteryPct && title == lastBatteryTitle) return
        lastBatteryPct = batteryPct
        lastBatteryTitle = title

        notificationRepository.postNotification(
            IslandNotification(
                key = "system_battery",
                packageName = "com.android.systemui",
                appName = "System",
                title = title,
                text = "$batteryPct%",
                category = "battery_charging",
                mode = IslandMode.Battery,
                icon = null,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = autoExpand
        )
    }

    @SuppressLint("MissingPermission")
    private fun isIgnoredBluetoothDevice(device: BluetoothDevice?, deviceName: String): Boolean {
        if (device == null) return false

        val bluetoothClass = try {
            device.bluetoothClass
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
        val majorClass = bluetoothClass?.majorDeviceClass
        val deviceClass = bluetoothClass?.deviceClass

        // 1. Explicitly allow Audio/Video devices (earbuds, headphones, car audio, speakers, headsets)
        if (majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            return false
        }

        // 2. Filter out Wearables, Health sensors, and Peripherals (mice, keyboards, controllers)
        if (majorClass == BluetoothClass.Device.Major.WEARABLE ||
            majorClass == BluetoothClass.Device.Major.HEALTH ||
            majorClass == BluetoothClass.Device.Major.PERIPHERAL
        ) {
            return true
        }

        if (deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH ||
            deviceClass == BluetoothClass.Device.WEARABLE_PAGER ||
            deviceClass == BluetoothClass.Device.WEARABLE_JACKET ||
            deviceClass == BluetoothClass.Device.WEARABLE_HELMET ||
            deviceClass == BluetoothClass.Device.WEARABLE_GLASSES
        ) {
            return true
        }

        // 3. Name-based heuristics for smartwatches, fitness bands, and trackers
        val lowerName = deviceName.lowercase()
        val wearableKeywords = listOf(
            "watch", "smartwatch", "wear os", "fitbit", "garmin", "amazfit",
            "whoop", "oura", "smart band", "smartband", "mi band", "miband",
            "honor band", "huawei band", "galaxy fit", "band"
        )
        if (wearableKeywords.any { lowerName.contains(it) }) {
            return true
        }

        // 4. Peripherals / HID keywords
        val peripheralKeywords = listOf("mouse", "keyboard", "trackpad", "gamepad", "controller", "stylus", "s pen")
        if (peripheralKeywords.any { lowerName.contains(it) }) {
            return true
        }

        return false
    }
}
