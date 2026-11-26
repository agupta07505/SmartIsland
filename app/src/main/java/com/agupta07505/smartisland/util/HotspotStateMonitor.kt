/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class HotspotStateMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings
) {
    private var receiver: BroadcastReceiver? = null
    private var pollJob: Job? = null
    private var broadcastSeen = false
    private var on = false
    private var shown = false
    private var clients: Int? = null
    private var postedAtMs = 0L

    fun start() {
        if (receiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                broadcastSeen = true
                when (intent?.getIntExtra(EXTRA_AP_STATE, -1)) {
                    AP_ENABLED -> setOn(true)
                    AP_DISABLED, AP_FAILED -> setOn(false)
                }
            }
        }
        try {
            ContextCompat.registerReceiver(context, r, IntentFilter(ACTION_AP_STATE), ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "register AP-state receiver failed", e)
        }
        receiver = r
        HotspotSettingsAutomation.onCount = { count -> scope.launch { updateClients(count) } }
        HotspotSettingsAutomation.onToggled = {
            scope.launch {
                delay(TOGGLE_RECHECK_MS)
                setOn(withContext(Dispatchers.IO) { tetherInterfaceUp() })
            }
        }
        pollJob = scope.launch {
            setOn(withContext(Dispatchers.IO) { tetherInterfaceUp() }, startup = true)
            while (true) {
                delay(POLL_MS)
                if (!broadcastSeen) setOn(withContext(Dispatchers.IO) { tetherInterfaceUp() })
                reconcile(startup = false)
                if (on) HotspotClients.read()?.let { updateClients(it) }
            }
        }
    }

    fun stop() {
        receiver?.let { r ->
            runCatchingLogged(TAG, "unregister AP-state receiver failed") { context.unregisterReceiver(r) } }
        receiver = null
        pollJob?.cancel()
        pollJob = null
        HotspotSettingsAutomation.onCount = null
        HotspotSettingsAutomation.onToggled = null
        HotspotSettingsAutomation.hotspotOn = false
        repository.removeNotification(KEY_HOTSPOT)
        on = false
        shown = false
        clients = null
    }

    private fun setOn(value: Boolean, startup: Boolean = false) {
        if (value == on && !startup) return
        on = value
        HotspotSettingsAutomation.hotspotOn = value
        if (!value) {
            shown = false
            clients = null
        }
        reconcile(startup)
        if (value) scope.launch { HotspotClients.read()?.let { updateClients(it) } }
    }

    private fun updateClients(count: Int?) {
        if (count == clients) return
        clients = count
        if (on && shown) repository.postNotification(card(), autoExpand = false)
    }

    private fun card() = IslandNotification(
        key = KEY_HOTSPOT,
        packageName = "com.android.systemui",
        appName = "Hotspot",
        title = "Hotspot",
        text = when (val n = clients) {
            null -> "Hotspot is on"
            0 -> "No devices connected"
            1 -> "1 device connected"
            else -> "$n devices connected"
        },
        category = "hotspot",
        mode = IslandMode.Hotspot,
        timeMillis = postedAtMs
    )

    private fun reconcile(startup: Boolean) {
        if (!on) {
            repository.removeNotification(KEY_HOTSPOT)
            return
        }
        if (shown) return
        val settings = settingsProvider()
        if (!settings.isEventEnabled(IslandEvent.HOTSPOT.id)) return
        val listenerCard = repository.notifications.value.any { it.mode == IslandMode.Hotspot && it.key != KEY_HOTSPOT }
        if (listenerCard) return
        shown = true
        postedAtMs = System.currentTimeMillis()
        repository.postSystemEvent(
            card(),
            IslandEvent.HOTSPOT.id,
            settings,
            wantsExpand = !startup
        )
    }

    /** Active tether interface with a gateway-style address, i.e. x.x.x.1 on a /24. */
    private fun tetherInterfaceUp(): Boolean = quietly(false) {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { nif ->
            nif.isUp && !nif.isLoopback && isTetherInterface(nif.name) &&
                nif.interfaceAddresses.any { a ->
                    val addr = a.address as? Inet4Address
                    addr != null && addr.isSiteLocalAddress && a.networkPrefixLength.toInt() == 24 &&
                        addr.address[3] == 1.toByte()
                }
        }
    }

    companion object {
        private const val TAG = "HotspotStateMonitor"
        const val KEY_HOTSPOT = "system_hotspot"
        private const val ACTION_AP_STATE = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val EXTRA_AP_STATE = "wifi_state"
        private const val AP_DISABLED = 11
        private const val AP_ENABLED = 13
        private const val AP_FAILED = 14
        private const val POLL_MS = 8_000L
        private const val TOGGLE_RECHECK_MS = 1_200L
        private val TETHER_PREFIXES = listOf("ap", "swlan", "softap", "wlan1", "wlan2", "rndis", "ncm", "bt-pan")

        fun isTetherInterface(name: String): Boolean = TETHER_PREFIXES.any { name.startsWith(it) }
    }
}
