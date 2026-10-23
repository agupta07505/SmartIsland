/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ConnectivityEventMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var vpnCallback: ConnectivityManager.NetworkCallback? = null
    private var startedAt = 0L
    private var lastWifiNetwork: Network? = null
    private var lastWifiSsid: String? = null
    private var lastVpnNetwork: Network? = null
    private var wifiDismissJob: Job? = null
    private var vpnJob: Job? = null
    private var wifiOffJob: Job? = null
    private var vpnOffJob: Job? = null

    fun start() {
        val cm = connectivityManager ?: return
        if (wifiCallback != null) return
        startedAt = SystemClock.elapsedRealtime()

        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val wCallback = buildWifiCallback()
        wifiCallback = wCallback
        runCatchingLogged(TAG, "register Wi-Fi callback failed") { cm.registerNetworkCallback(wifiRequest, wCallback) }

        // VPN networks do not declare NOT_VPN, so the default request must be built without it.
        val vpnRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        val vCallback = buildVpnCallback()
        vpnCallback = vCallback
        runCatchingLogged(TAG, "register VPN callback failed") { cm.registerNetworkCallback(vpnRequest, vCallback) }
    }

    fun stop() {
        val cm = connectivityManager
        wifiCallback?.let { cb -> runCatchingLogged(TAG, "unregister Wi-Fi callback failed") { cm?.unregisterNetworkCallback(cb) } }
        vpnCallback?.let { cb -> runCatchingLogged(TAG, "unregister VPN callback failed") { cm?.unregisterNetworkCallback(cb) } }
        wifiCallback = null
        vpnCallback = null
        wifiDismissJob?.cancel()
        vpnJob?.cancel()
        wifiOffJob?.cancel()
        vpnOffJob?.cancel()
        repository.removeNotification(KEY_WIFI)
        repository.removeNotification(KEY_VPN)
        repository.removeNotification(KEY_WIFI_OFF)
        repository.removeNotification(KEY_VPN_OFF)
    }

    private fun isStartupReplay(): Boolean = SystemClock.elapsedRealtime() - startedAt < STARTUP_GRACE_MS

    private fun buildWifiCallback(): ConnectivityManager.NetworkCallback {
        val onCaps = { network: Network, caps: NetworkCapabilities -> handleWifi(network, caps) }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onCaps(network, caps)
                override fun onLost(network: Network) = handleWifiLost(network)
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onCaps(network, caps)
                override fun onLost(network: Network) = handleWifiLost(network)
            }
        }
    }

    private fun handleWifi(network: Network, caps: NetworkCapabilities) {
        // Track the connection whichever cards are on (a "disconnected" card needs to know what was connected);
        // only reading the name is skipped when neither card is on.
        val settings = settingsProvider()
        val connectedOn = settings.isEventEnabled(IslandEvent.WIFI.id)
        val ssid = if (connectedOn || settings.isEventEnabled(IslandEvent.WIFI_DISCONNECTED.id)) readSsid(caps) else null
        // Same network and same name: this is a routine capability update, not a new connection.
        if (network == lastWifiNetwork && (ssid == lastWifiSsid || ssid == null)) return
        val isNewNetwork = network != lastWifiNetwork
        lastWifiNetwork = network
        if (ssid != null) lastWifiSsid = ssid
        // Back on Wi-Fi: a pending or showing "disconnected" card is stale.
        if (isNewNetwork) clearWifiOff()
        if (isStartupReplay()) return
        if (!connectedOn) return
        if (!isNewNetwork && ssid == null) return
        postWifi(ssid ?: lastWifiSsid)
    }

    private fun handleWifiLost(network: Network) {
        if (network == lastWifiNetwork) {
            val lostSsid = lastWifiSsid
            lastWifiNetwork = null
            lastWifiSsid = null
            wifiDismissJob?.cancel()
            repository.removeNotification(KEY_WIFI)
            if (!isStartupReplay() && settingsProvider().isEventEnabled(IslandEvent.WIFI_DISCONNECTED.id)) {
                scheduleWifiOff(lostSsid)
            }
        }
    }

    private fun clearWifiOff() {
        wifiOffJob?.cancel()
        repository.removeNotification(KEY_WIFI_OFF)
    }

    private fun scheduleWifiOff(lostSsid: String?) {
        wifiOffJob?.cancel()
        wifiOffJob = scope.launch {
            delay(DISCONNECT_CONFIRM_MS)
            if (lastWifiNetwork != null) return@launch
            if (!settingsProvider().isEventEnabled(IslandEvent.WIFI_DISCONNECTED.id)) return@launch
            repository.postNotification(
                IslandNotification(
                    key = KEY_WIFI_OFF,
                    packageName = "com.android.systemui",
                    appName = "Wi-Fi",
                    title = lostSsid ?: "Wi-Fi",
                    text = "Disconnected",
                    category = "wifi_disconnected",
                    mode = IslandMode.Bluetooth,
                    timeMillis = System.currentTimeMillis()
                ),
                autoExpand = !NotificationCooldownManager.shouldThrottleSystemEvent(IslandEvent.WIFI_DISCONNECTED.id, settingsProvider())
            )
            delay(IslandTimings.DISCONNECT_CARD_MS)
            repository.removeNotification(KEY_WIFI_OFF)
        }
    }

    private fun postWifi(ssid: String?) {
        repository.postNotification(
            IslandNotification(
                key = KEY_WIFI,
                packageName = "com.android.systemui",
                appName = "Wi-Fi",
                title = ssid ?: "Wi-Fi",
                text = if (ssid != null) "Connected" else "Wi-Fi connected",
                category = "wifi",
                mode = IslandMode.Bluetooth,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = !NotificationCooldownManager.shouldThrottleSystemEvent(IslandEvent.WIFI.id, settingsProvider())
        )
        wifiDismissJob?.cancel()
        wifiDismissJob = scope.launch {
            delay(TRANSIENT_DISPLAY_MS)
            repository.removeNotification(KEY_WIFI)
        }
    }

    private fun readSsid(caps: NetworkCapabilities): String? {
        if (!hasLocationPermission()) {
            logSsidMiss("location permission not granted")
            return null
        }
        if (!isLocationServiceOn()) {
            logSsidMiss("location services are off")
            return null
        }
        val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cleanSsid((caps.transportInfo as? WifiInfo)?.ssid)
        } else {
            null
        }
        // Some ROMs only fill the SSID through WifiManager; ask there when the callback had none.
        val ssid = fromCaps ?: cleanSsid(legacySsid())
        if (ssid == null) logSsidMiss("the system returned an unknown SSID")
        return ssid
    }

    @Suppress("DEPRECATION")
    private fun legacySsid(): String? = runCatching {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo?.ssid
    }.getOrNull()

    private fun cleanSsid(raw: String?): String? {
        val cleaned = raw?.trim('"')?.trim()
        if (cleaned.isNullOrBlank() || cleaned == WifiManager.UNKNOWN_SSID || cleaned == "<unknown ssid>") return null
        return cleaned
    }

    private fun isLocationServiceOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
        return runCatching { lm.isLocationEnabled }.getOrDefault(true)
    }

    private var lastSsidMissReason: String? = null

    private fun logSsidMiss(reason: String) {
        if (reason == lastSsidMissReason) return
        lastSsidMissReason = reason
        runCatching { AppLogRecorder.w(TAG, "Wi-Fi name not shown: $reason") }
    }

    private fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun buildVpnCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (lastVpnNetwork == network) return
            lastVpnNetwork = network
            // A VPN is back: a pending or showing "disconnected" card is stale.
            vpnOffJob?.cancel()
            repository.removeNotification(KEY_VPN_OFF)
            if (isStartupReplay()) return
            if (!settingsProvider().isEventEnabled(IslandEvent.VPN.id)) return
            showVpn(network)
        }

        override fun onLost(network: Network) {
            if (network == lastVpnNetwork) {
                lastVpnNetwork = null
                vpnJob?.cancel()
                repository.removeNotification(KEY_VPN)
                if (!isStartupReplay() && settingsProvider().isEventEnabled(IslandEvent.VPN_DISCONNECTED.id)) {
                    scheduleVpnOff()
                }
            }
        }
    }

    private fun scheduleVpnOff() {
        vpnOffJob?.cancel()
        vpnOffJob = scope.launch {
            delay(DISCONNECT_CONFIRM_MS)
            if (lastVpnNetwork != null) return@launch
            if (!settingsProvider().isEventEnabled(IslandEvent.VPN_DISCONNECTED.id)) return@launch
            repository.postNotification(
                IslandNotification(
                    key = KEY_VPN_OFF,
                    packageName = "com.android.systemui",
                    appName = "VPN",
                    title = "VPN disconnected",
                    text = "Traffic is not protected",
                    category = "vpn_disconnected",
                    mode = IslandMode.Bluetooth,
                    timeMillis = System.currentTimeMillis()
                ),
                autoExpand = !NotificationCooldownManager.shouldThrottleSystemEvent(IslandEvent.VPN_DISCONNECTED.id, settingsProvider())
            )
            delay(IslandTimings.DISCONNECT_CARD_MS)
            repository.removeNotification(KEY_VPN_OFF)
        }
    }

    private fun showVpn(network: Network) {
        vpnJob?.cancel()
        vpnJob = scope.launch {
            postVpn("VPN connected", if (settingsProvider().allowNetworkChecks) "Finding location…" else "Secure connection active")
            if (settingsProvider().allowNetworkChecks) {
                // The tunnel needs a moment before traffic flows through it, so retry a few times.
                var location: String? = null
                for (attempt in 1..LOOKUP_ATTEMPTS) {
                    delay(if (attempt == 1) 1500L else 2000L)
                    location = lookupVpnLocation(network)
                    if (location != null) break
                }
                if (location == null) {
                    runCatching { AppLogRecorder.w(TAG, "VPN location lookup failed after $LOOKUP_ATTEMPTS attempts") }
                }
                postVpn(location ?: "VPN connected", if (location != null) "VPN connected" else "Secure connection active", autoExpand = false)
            }
            delay(TRANSIENT_DISPLAY_MS)
            repository.removeNotification(KEY_VPN)
        }
    }

    private fun postVpn(title: String, text: String, autoExpand: Boolean = true) {
        repository.postNotification(
            IslandNotification(
                key = KEY_VPN,
                packageName = "com.android.systemui",
                appName = "VPN",
                title = title,
                text = text,
                category = "vpn",
                mode = IslandMode.Bluetooth,
                timeMillis = System.currentTimeMillis()
            ),
            autoExpand = autoExpand && !NotificationCooldownManager.shouldThrottleSystemEvent(IslandEvent.VPN.id, settingsProvider())
        )
    }

    private suspend fun lookupVpnLocation(network: Network): String? = withContext(Dispatchers.IO) {
        LOOKUP_URLS.firstNotNullOfOrNull { url -> fetchLocation(network, url) }
    }

    private fun fetchLocation(network: Network, address: String): String? = runCatching {
        val conn = (network.openConnection(URL(address)) as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "SmartIsland")
        }
        try {
            if (conn.responseCode != 200) return@runCatching null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (!json.optBoolean("success", true) || json.has("error")) return@runCatching null
            val city = json.optString("city").takeIf { it.isNotBlank() }
            val country = (json.optString("country_name").takeIf { it.isNotBlank() }
                ?: json.optString("country").takeIf { it.isNotBlank() })
            listOfNotNull(city, country).joinToString(", ").takeIf { it.isNotBlank() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    companion object {
        private const val TAG = "ConnectivityEventMonitor"
        const val KEY_WIFI = "system_wifi"
        const val KEY_VPN = "system_vpn"
        const val KEY_WIFI_OFF = "system_wifi_off"
        const val KEY_VPN_OFF = "system_vpn_off"
        private const val DISCONNECT_CONFIRM_MS = 1500L
        private const val STARTUP_GRACE_MS = 2000L
        private const val TRANSIENT_DISPLAY_MS = IslandTimings.CONNECTIVITY_CARD_MS
        private const val LOOKUP_ATTEMPTS = 4
        private val LOOKUP_URLS = listOf(
            "https://ipwho.is/?fields=success,city,country",
            "https://ipapi.co/json/"
        )
    }
}
