/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

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
import com.agupta07505.smartisland.util.runCatchingLogged

class SystemEventReceiver(
    private val notificationRepository: INotificationRepository
) : BroadcastReceiver() {

    private var lastBatteryPct: Int = -1
    private var lastBatteryTitle: String? = null
    private var isCurrentlyCharging: Boolean = false

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
                    val batteryLevel = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
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
                            timeMillis = System.currentTimeMillis()
                        ),
                        autoExpand = true
                    )
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    notificationRepository.removeNotification("system_bluetooth")
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCurrentlyCharging = true
                    updateBatteryIsland(context, intent, autoExpand = true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCurrentlyCharging = false
                    updateBatteryState(context, intent, autoExpand = false)
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

    private fun isCharging(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getBatteryIntent(context: Context, batteryIntent: Intent?): Intent? {
        return batteryIntent?.takeIf { it.hasExtra(BatteryManager.EXTRA_LEVEL) } ?: runCatchingLogged("SystemEventReceiver", "registerReceiver BATTERY_CHANGED failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_EXPORTED)
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
}
