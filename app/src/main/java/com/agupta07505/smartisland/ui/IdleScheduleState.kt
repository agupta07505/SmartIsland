/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.IdleSchedule
import com.agupta07505.smartisland.model.IdleSchedulePrefs
import java.util.Calendar

private fun isChargingNow(intent: Intent?): Boolean {
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
}

/** Whether the phone is charging. The battery broadcast is only followed while [track] is true. */
@Composable
internal fun rememberIsCharging(track: Boolean): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(false) }
    DisposableEffect(context, track) {
        if (!track) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                state.value = isChargingNow(intent)
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        state.value = isChargingNow(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state
}

/** The schedule in force now ([IdleSchedule.Context]). */
@Composable
internal fun rememberIdleScheduleContext(prefs: IdleSchedulePrefs, track: Boolean): IdleSchedule.Context {
    val charging by rememberIsCharging(track = track && prefs.chargingEnabled)
    val hour by rememberLocalHour(track = track && prefs.needsClock)
    val day = remember(hour) { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) }
    return IdleSchedule.contextFor(prefs, charging, hour, day)
}
