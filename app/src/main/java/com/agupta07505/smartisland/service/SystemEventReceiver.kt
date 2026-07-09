/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.util.runCatchingLogged
import android.os.Build

class SystemEventReceiver(
    private val notificationRepository: INotificationRepository
) : BroadcastReceiver() {

    private var lastBatteryPct: Int = -1
    private var isCurrentlyCharging: Boolean = false

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                isCurrentlyCharging = true
                // Auto-expand ONCE when the charger is plugged in.
                updateBatteryIsland(context, autoExpand = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                isCurrentlyCharging = false
                lastBatteryPct = -1
                notificationRepository.removeNotification("system_battery")
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                // Only refresh the % silently; never auto-expand on ticks.
                if (isCharging(intent) && isCurrentlyCharging) {
                    updateBatteryIsland(context, autoExpand = false)
                }
            }
        }
    }

    private fun isCharging(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun updateBatteryIsland(context: Context, autoExpand: Boolean) {
        val batteryStatus: Intent? = runCatchingLogged("SystemEventReceiver", "registerReceiver BATTERY_CHANGED failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        // Skip redundant posts when the percentage hasn't changed.
        if (!autoExpand && batteryPct == lastBatteryPct) return
        lastBatteryPct = batteryPct

        val title = when (status) {
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            else -> "Charging"
        }

        notificationRepository.postNotification(
            IslandNotification(
                key = "system_battery",
                packageName = "com.android.systemui",
                appName = "System",
                title = title,
                text = "$batteryPct%",
                mode = IslandMode.Battery,
                icon = null,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = autoExpand
        )
    }
}
