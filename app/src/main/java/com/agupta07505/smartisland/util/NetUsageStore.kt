/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.agupta07505.smartisland.model.DailyUsage
import com.agupta07505.smartisland.model.NetConnection

/** The daily Wi-Fi / mobile data counter of the net speed island, kept in its own small preferences file (it is a count, not a setting). */
internal class NetUsageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("net_usage", Context.MODE_PRIVATE)

    fun load(): DailyUsage = DailyUsage(
        dayKey = prefs.getInt("day", 0),
        wifiBytes = prefs.getLong("wifi", 0L),
        mobileBytes = prefs.getLong("mobile", 0L)
    )

    fun save(usage: DailyUsage) {
        prefs.edit().putInt("day", usage.dayKey).putLong("wifi", usage.wifiBytes).putLong("mobile", usage.mobileBytes).apply()
    }
}

/** Whether Android's own Data Saver (Settings > Network > Data Saver) is switched on. */
internal object DataSaverReader {
    fun isOn(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        // ENABLED: Data Saver is on for this app. WHITELISTED: it is on, but this app is allowed to use data in the
        // background. Both mean the setting is on.
        return runCatching { cm.restrictBackgroundStatus != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED }.getOrDefault(false)
    }
}

/** What the device is connected to right now (needs only ACCESS_NETWORK_STATE, which the app already holds). */
internal object NetConnectionReader {
    fun read(context: Context): NetConnection {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetConnection.None
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull() ?: return NetConnection.None
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetConnection.Wifi
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetConnection.Mobile
            else -> NetConnection.Other
        }
    }
}
