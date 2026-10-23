/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

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
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.NotificationCooldownManager
import com.agupta07505.smartisland.util.runCatchingLogged

class SystemEventReceiver(
    private val notificationRepository: INotificationRepository,
    private val settingsProvider: () -> com.agupta07505.smartisland.data.SmartIslandSettings = { com.agupta07505.smartisland.data.SmartIslandSettings.Default },
    private val isBatteryCardExpanded: () -> Boolean = { false }
) : BroadcastReceiver() {

    private var lastBatteryPct: Int = -1
    private var lastBatteryTitle: String? = null
    private var isCurrentlyCharging: Boolean = false
    private var chargingKnown: Boolean = false
    private var unpluggedCardUntil: Long = 0L
    private var pluggedInCardUntil: Long = 0L
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var activeBluetoothAddress: String? = null
    private var lastBluetoothAddress: String? = null
    private var lastBluetoothConnectedTime: Long = 0L

    private fun mayExpand(event: IslandEvent, wantsExpand: Boolean): Boolean =
        wantsExpand && !NotificationCooldownManager.shouldThrottleSystemEvent(event.id, settingsProvider())

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun postUnlessExpanded(delayMs: Long, action: () -> Unit) {
        mainHandler.postDelayed(object : Runnable {
            override fun run() {
                if (isBatteryCardExpanded()) mainHandler.postDelayed(this, EXPANDED_RECHECK_MS) else action()
            }
        }, delayMs)
    }

    companion object {
        private const val BLUETOOTH_DEBOUNCE_MS = 30_000L
        private const val EXPANDED_RECHECK_MS = 500L
        private const val UNPLUGGED_DISPLAY_MS = com.agupta07505.smartisland.util.IslandTimings.UNPLUGGED_CARD_MS
        private const val PLUGGED_IN_DISPLAY_MS = com.agupta07505.smartisland.util.IslandTimings.PLUGGED_IN_CARD_MS
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatchingLogged("SystemEventReceiver", "Broadcast callback failed") {
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

                    // Smartwatches, trackers and peripherals must not hijack the island with connection alerts.
                    if (isIgnoredBluetoothDevice(device, deviceName)) {
                        return@runCatchingLogged
                    }

                    val address = runCatching { device?.address }.getOrNull()
                    val now = System.currentTimeMillis()

                    // Debounce: a device already active or reconnected within the window does not auto-expand (no popup loops).
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
                        autoExpand = mayExpand(IslandEvent.BLUETOOTH, autoExpand)
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

                    // Only dismiss when the disconnected device is the active one (or unknown), so a smartwatch
                    // disconnecting does not remove the card while earbuds are connected.
                    if (activeBluetoothAddress == null || disconnectedAddress == null || disconnectedAddress == activeBluetoothAddress) {
                        activeBluetoothAddress = null
                        notificationRepository.removeNotification("system_bluetooth")
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    // ACTION_BATTERY_CHANGED may already have announced this plug-in.
                    val alreadyAnnounced = chargingKnown && isCurrentlyCharging
                    chargingKnown = true
                    isCurrentlyCharging = true
                    if (!alreadyAnnounced) {
                        updateBatteryIsland(context, intent, autoExpand = true, event = "plugged_in")
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    chargingKnown = true
                    isCurrentlyCharging = false
                    postUnplugged(context, intent)
                }
                Intent.ACTION_BATTERY_LOW -> {
                    updateBatteryState(context, intent, autoExpand = true)
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    updateBatteryState(context, intent, autoExpand = false)
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isPowerSave = powerManager?.isPowerSaveMode == true
                    updateBatteryState(context, intent, autoExpand = isPowerSave)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val charging = isCharging(intent)
                    if (!chargingKnown) {
                        // Sticky broadcast on registration: adopt the current state silently, not as a new plug-in or unplug event.
                        chargingKnown = true
                        isCurrentlyCharging = charging
                        if (charging) {
                            updateBatteryIsland(context, intent, autoExpand = false)
                        } else {
                            updateBatteryState(context, intent, autoExpand = false)
                        }
                    } else if (charging != isCurrentlyCharging) {
                        isCurrentlyCharging = charging
                        if (charging) {
                            updateBatteryIsland(context, intent, autoExpand = true, event = "plugged_in")
                        } else {
                            postUnplugged(context, intent)
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

    private fun isCharging(intent: Intent): Boolean {
        // A connected charger counts even when the status says NOT_CHARGING (charge limit or heat protection pause charging).
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (plugged > 0) return true
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

    private fun postUnplugged(context: Context, intent: Intent?) {
        val intentToUse = getBatteryIntent(context, intent)
        val level = intentToUse?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intentToUse?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (scale > 0 && level >= 0) (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100) else 0
        unpluggedCardUntil = System.currentTimeMillis() + UNPLUGGED_DISPLAY_MS
        pluggedInCardUntil = 0L
        lastBatteryPct = -1
        lastBatteryTitle = null
        notificationRepository.postNotification(
            IslandNotification(
                key = "system_battery",
                packageName = "com.android.systemui",
                appName = "System",
                title = "Unplugged",
                text = "$pct%",
                category = "battery_plugged_out",
                mode = IslandMode.Battery,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = mayExpand(IslandEvent.PLUGGED_OUT, true)
        )
        postUnlessExpanded(UNPLUGGED_DISPLAY_MS + 100L) {
            if (!isCurrentlyCharging) updateBatteryState(context, null, autoExpand = false)
        }
    }

    private fun scheduleWarningRemoval(category: String) {
        postUnlessExpanded(settingsProvider().batteryWarningMs.toLong()) {
            val card = notificationRepository.notifications.value.firstOrNull { it.key == "system_battery" }
            if (card?.category == category) notificationRepository.removeNotification("system_battery")
        }
    }

    private fun updateBatteryState(context: Context, intent: Intent?, autoExpand: Boolean) {
        if (System.currentTimeMillis() < unpluggedCardUntil) return
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
            // A short warning: announced once per state, not again for every percent that follows.
            if (!autoExpand && title == lastBatteryTitle) return
            lastBatteryPct = batteryPct
            lastBatteryTitle = title
            scheduleWarningRemoval("battery_saver")

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
                autoExpand = mayExpand(IslandEvent.BATTERY_SAVER, autoExpand)
            )
        } else if (isLowBattery) {
            val title = "Low Battery"
            // A short warning: announced once per state, not again for every percent that follows.
            if (!autoExpand && title == lastBatteryTitle) return
            lastBatteryPct = batteryPct
            lastBatteryTitle = title
            scheduleWarningRemoval("battery_low")

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
                autoExpand = mayExpand(IslandEvent.LOW_BATTERY, autoExpand)
            )
        } else {
            // A battery card that is open in the expanded island stays; the held timer clears it after it closes.
            if (isBatteryCardExpanded()) return
            lastBatteryPct = -1
            lastBatteryTitle = null
            notificationRepository.removeNotification("system_battery")
        }
    }

    private fun updateBatteryIsland(context: Context, batteryIntent: Intent?, autoExpand: Boolean, event: String? = null) {
        if (event != "plugged_in" && System.currentTimeMillis() < pluggedInCardUntil) return
        val intentToUse = getBatteryIntent(context, batteryIntent)
        val level = intentToUse?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intentToUse?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        if (level < 0 || scale <= 0) return
        val batteryPct = (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        val status = intentToUse?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isFull = status == BatteryManager.BATTERY_STATUS_FULL
        val isPlugEvent = event == "plugged_in" && !isFull
        if (!isFull && !isPlugEvent) {
            // Charging, not full, no plug event: no card. Clears the brief plugged-in card once it is over,
            // unless that card is open in the expanded island (the held timer clears it after the island closes).
            if (isBatteryCardExpanded()) return
            lastBatteryPct = -1
            lastBatteryTitle = null
            notificationRepository.removeNotification("system_battery")
            return
        }
        val title = if (isFull) "Fully Charged" else "Plugged in"
        val category = if (isFull) "battery_full" else "battery_plugged_in"
        val cooldownEvent = if (isFull) IslandEvent.FULLY_CHARGED else IslandEvent.PLUGGED_IN

        if (!autoExpand && batteryPct == lastBatteryPct && title == lastBatteryTitle) return
        lastBatteryPct = batteryPct
        lastBatteryTitle = title
        if (isFull) scheduleWarningRemoval("battery_full")

        notificationRepository.postNotification(
            IslandNotification(
                key = "system_battery",
                packageName = "com.android.systemui",
                appName = "System",
                title = title,
                text = "$batteryPct%",
                category = category,
                mode = IslandMode.Battery,
                icon = null,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = mayExpand(cooldownEvent, autoExpand)
        )

        if (isPlugEvent) {
            pluggedInCardUntil = System.currentTimeMillis() + PLUGGED_IN_DISPLAY_MS
            postUnlessExpanded(PLUGGED_IN_DISPLAY_MS + 100L) {
                // Still on the charger: the brief card is over, so clear it (or show "fully charged").
                if (isCurrentlyCharging) {
                    pluggedInCardUntil = 0L
                    lastBatteryPct = -1
                    lastBatteryTitle = null
                    updateBatteryIsland(context, null, autoExpand = false)
                }
            }
        }
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

        // Audio/video devices (earbuds, headphones, car audio, speakers, headsets) are allowed.
        if (majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            return false
        }

        // Wearables, health sensors and peripherals (mice, keyboards, controllers) are filtered out.
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

        // Name heuristics for smartwatches, fitness bands and trackers.
        val lowerName = deviceName.lowercase()
        val wearableKeywords = listOf(
            "watch", "smartwatch", "wear os", "fitbit", "garmin", "amazfit",
            "whoop", "oura", "smart band", "smartband", "mi band", "miband",
            "honor band", "huawei band", "galaxy fit", "band"
        )
        if (wearableKeywords.any { lowerName.contains(it) }) {
            return true
        }

        // HID keywords.
        val peripheralKeywords = listOf("mouse", "keyboard", "trackpad", "gamepad", "controller", "stylus", "s pen")
        if (peripheralKeywords.any { lowerName.contains(it) }) {
            return true
        }

        return false
    }
}
