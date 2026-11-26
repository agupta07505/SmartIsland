/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.CityResult
import com.agupta07505.smartisland.model.DailyUsage
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.ModeStatus
import com.agupta07505.smartisland.model.NetConnection
import com.agupta07505.smartisland.model.NetUsage
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.NotificationSummary
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherExtras
import com.agupta07505.smartisland.model.WeatherParser
import com.agupta07505.smartisland.model.WeatherPrefs
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherRefresh
import com.agupta07505.smartisland.model.WeatherSnapshot
import com.agupta07505.smartisland.model.WidgetIslandContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

class IdleWidgetData internal constructor() {
    /** Wall clock, refreshed every minute (and when the time or zone changes). */
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
        internal set

    /** Battery level 0..100, or -1 until it is known. */
    var batteryPercent by mutableIntStateOf(-1)
        internal set
    var charging by mutableStateOf(false)
        internal set

    /** Battery voltage in mV, 0 = unknown. */
    var batteryVoltageMv by mutableIntStateOf(0)
        internal set

    /** Battery temperature in tenths of a degree Celsius, [NO_TEMPERATURE] = unknown. */
    var batteryTempTenthsC by mutableIntStateOf(NO_TEMPERATURE)
        internal set

    /** Time until the battery is full (ms), -1 = the system cannot tell. */
    var chargeTimeRemainingMs by mutableLongStateOf(-1L)
        internal set

    var batteryCurrentRaw by mutableLongStateOf(Long.MIN_VALUE)
        internal set

    /** When the battery is expected to run out (epoch ms) while it is not charging. */
    var batteryEmptyAtMs by mutableLongStateOf(0L)
        internal set

    /** When the next alarm rings (epoch ms), 0 = none. */
    var nextAlarmMs by mutableLongStateOf(0L)
        internal set

    /** Device-wide traffic in the last couple of seconds, bytes per second. */
    var speedBytesPerSecond by mutableLongStateOf(0L)
        internal set
    var speedIsUpload by mutableStateOf(false)
        internal set

    /** What the device is connected to (read with the speed). */
    var connection by mutableStateOf(NetConnection.None)
        internal set
    var usageToday by mutableStateOf(DailyUsage())
        internal set

    /** Next calendar event within 24 hours, for the date widget. */
    var nextEvent by mutableStateOf<com.agupta07505.smartisland.model.NextEvent?>(null)
        internal set

    /** True while Android's Data Saver is switched on; the net speed island says so. */
    var dataSaver by mutableStateOf(false)
        internal set

    /** 2 directions on their own (bytes per second), for the widget island. */
    var downBytesPerSecond by mutableLongStateOf(0L)
        internal set
    var upBytesPerSecond by mutableLongStateOf(0L)
        internal set

    var weather by mutableStateOf<WeatherSnapshot?>(null)
        internal set

    /** Unread messages and mail, in total and per app (see [NotificationCountTracker]). */
    var notificationSummary by mutableStateOf(NotificationSummary.Empty)
        internal set

    /** Do Not Disturb mode that is on, or [ModeStatus.Off]. */
    var mode by mutableStateOf(ModeStatus.Off)
        internal set

    companion object {
        const val NO_TEMPERATURE = Int.MIN_VALUE
    }
}

@Composable
fun rememberIdleWidgetData(
    kinds: List<IdleWidgetKind>,
    active: Boolean,
    allowNetwork: Boolean,
    /** Where the weather's place comes from */
    weather: WeatherPrefs = WeatherPrefs()
): IdleWidgetData {
    val context = LocalContext.current.applicationContext
    val data = remember { IdleWidgetData() }
    val wantsBattery = active && IdleWidgetKind.Battery in kinds
    val wantsAlarm = active && IdleWidgetKind.NextAlarm in kinds
    val wantsTime = active && (IdleWidgetKind.Clock in kinds || IdleWidgetKind.LiveClock in kinds
        || IdleWidgetKind.Date in kinds || wantsAlarm)
    val wantsSpeed = active && IdleWidgetKind.NetSpeed in kinds
    val wantsWind = IdleWidgetKind.Wind in kinds
    val wantsWeather = active && allowNetwork && (IdleWidgetKind.Weather in kinds || wantsWind)
    val wantsNotifications = active && IdleWidgetKind.Notifications in kinds
    val wantsMode = active && IdleWidgetKind.Modes in kinds

    NotificationSummaryEffect(data, wantsNotifications)
    ModeEffects(context, data, wantsMode)
    ClockBatteryAlarmEffect(context, data, wantsTime, wantsBattery, wantsAlarm)
    CalendarEffect(context, data, wantsCalendar = active && IdleWidgetKind.Date in kinds)
    NetSpeedEffect(context, data, wantsSpeed)
    WeatherEffect(context, data, wantsWeather, wantsWind, weather)
    return data
}

@Composable
@NonRestartableComposable
private fun NotificationSummaryEffect(data: IdleWidgetData, wantsNotifications: Boolean) {
    LaunchedEffect(wantsNotifications) {
        if (!wantsNotifications) {
            data.notificationSummary = NotificationSummary.Empty
            return@LaunchedEffect
        }
        NotificationCountTracker.entries.collect { data.notificationSummary = NotificationCounts.summarize(it) }
    }
}

@Composable
@NonRestartableComposable
private fun ModeEffects(context: Context, data: IdleWidgetData, wantsMode: Boolean) {
    // Do Not Disturb: the system announces filter and policy changes
    DisposableEffect(context, wantsMode) {
        if (!wantsMode) {
            data.mode = ModeStatus.Off
            return@DisposableEffect onDispose { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                data.mode = DndModeReader.read(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        data.mode = DndModeReader.read(context)
        onDispose { runCatchingLogged("IdleWidgetData", "Could not unregister the mode receiver") { context.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(wantsMode) {
        if (!wantsMode) return@LaunchedEffect
        while (true) {
            delay(MODE_POLL_MS)
            val now = withContext(Dispatchers.Default) { DndModeReader.read(context) }
            if (now != data.mode) data.mode = now
        }
    }
}

@Composable
@NonRestartableComposable
private fun ClockBatteryAlarmEffect(
    context: Context,
    data: IdleWidgetData,
    wantsTime: Boolean,
    wantsBattery: Boolean,
    wantsAlarm: Boolean
) {
    DisposableEffect(context, wantsTime, wantsBattery, wantsAlarm) {
        if (!wantsTime && !wantsBattery && !wantsAlarm) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                        data.nowMs = System.currentTimeMillis()
                    Intent.ACTION_BATTERY_CHANGED -> readBattery(context, data, intent)
                    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> data.nextAlarmMs = nextAlarmMs(context)
                }
            }
        }
        val filter = IntentFilter().apply {
            if (wantsTime) {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            if (wantsBattery) addAction(Intent.ACTION_BATTERY_CHANGED)
            if (wantsAlarm) addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        // Whatever the clock showed while the widgets were off is stale
        data.nowMs = System.currentTimeMillis()
        if (wantsAlarm) data.nextAlarmMs = nextAlarmMs(context)
        // a sticky broadcast (the battery one) answers the registration itself
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (wantsBattery && sticky != null && sticky.action == Intent.ACTION_BATTERY_CHANGED) readBattery(context, data,
            sticky)
        onDispose { runCatchingLogged("IdleWidgetData", "Could not unregister the battery receiver") { context.unregisterReceiver(receiver) } }
    }
}

@Composable
@NonRestartableComposable
private fun CalendarEffect(context: Context, data: IdleWidgetData, wantsCalendar: Boolean) {
    LaunchedEffect(wantsCalendar) {
        if (!wantsCalendar) {
            data.nextEvent = null
            return@LaunchedEffect
        }
        while (true) {
            val event = withContext(Dispatchers.IO) { CalendarReader.nextEvent(context, System.currentTimeMillis()) }
            if (event != data.nextEvent) data.nextEvent = event
            delay(CALENDAR_POLL_MS)
        }
    }
}

@Composable
@NonRestartableComposable
private fun NetSpeedEffect(context: Context, data: IdleWidgetData, wantsSpeed: Boolean) {
    LaunchedEffect(wantsSpeed) {
        if (!wantsSpeed) {
            data.speedBytesPerSecond = 0L
            data.downBytesPerSecond = 0L
            data.upBytesPerSecond = 0L
            data.connection = NetConnection.None
            data.dataSaver = false
            return@LaunchedEffect
        }
        sampleNetSpeed(context, data)
    }
}

private fun todayDayKey(): Int = Calendar.getInstance().let {
    NetUsage.dayKey(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH))
}

private fun mobileTrafficTotal(): Long {
    val r = TrafficStats.getMobileRxBytes()
    val t = TrafficStats.getMobileTxBytes()
    return if (r == TrafficStats.UNSUPPORTED.toLong() || t == TrafficStats.UNSUPPORTED.toLong()) -1L else r + t
}

/** Samples traffic every [SPEED_SAMPLE_MS] until cancelled, the daily counter is saved when this stops. */
private suspend fun sampleNetSpeed(context: Context, data: IdleWidgetData) {
    val store = NetUsageStore(context)
    var usage = NetUsage.rolled(store.load(), todayDayKey())
    data.usageToday = usage
    data.connection = NetConnectionReader.read(context)
    data.dataSaver = DataSaverReader.isOn(context)
    var lastRx = TrafficStats.getTotalRxBytes()
    var lastTx = TrafficStats.getTotalTxBytes()
    var lastMobile = mobileTrafficTotal()
    var lastAt = SystemClock.elapsedRealtime()
    var unsaved = 0
    try {
        while (true) {
            delay(SPEED_SAMPLE_MS)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val mobile = mobileTrafficTotal()
            val at = SystemClock.elapsedRealtime()
            val seconds = (at - lastAt).coerceAtLeast(1L) / 1000.0
            val connection = NetConnectionReader.read(context)
            if (connection != data.connection) data.connection = connection
            val saver = DataSaverReader.isOn(context)
            if (saver != data.dataSaver) data.dataSaver = saver
            if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
                data.speedBytesPerSecond = 0L
                data.downBytesPerSecond = 0L
                data.upBytesPerSecond = 0L
            } else {
                val down = ((rx - lastRx).coerceAtLeast(0L) / seconds).toLong()
                val up = ((tx - lastTx).coerceAtLeast(0L) / seconds).toLong()
                data.speedIsUpload = up > down
                data.downBytesPerSecond = down
                data.upBytesPerSecond = up
                data.speedBytesPerSecond = maxOf(down, up)
                val total = (rx - lastRx).coerceAtLeast(0L) + (tx - lastTx).coerceAtLeast(0L)
                val mobileDelta = if (mobile >= 0L && lastMobile >= 0L) mobile - lastMobile else 0L
                val (wifiDelta, mobileShare) = NetUsage.split(total, mobileDelta)
                val next = NetUsage.add(usage, todayDayKey(), wifiDelta, mobileShare)
                if (next != usage) {
                    usage = next
                    data.usageToday = usage
                    unsaved++
                }
                // Written every half minute (and when this stops)
                if (unsaved >= USAGE_SAVE_EVERY) {
                    store.save(usage)
                    unsaved = 0
                }
            }
            lastRx = rx
            lastTx = tx
            lastMobile = mobile
            lastAt = at
        }
    } finally {
        if (unsaved > 0) store.save(usage)
    }
}

@Composable
@NonRestartableComposable
private fun WeatherEffect(
    context: Context,
    data: IdleWidgetData,
    wantsWeather: Boolean,
    wantsWind: Boolean,
    weather: WeatherPrefs
) {
    val refreshNonce by IdleWeather.refreshNonce.collectAsState()
    LaunchedEffect(wantsWeather, wantsWind, weather, refreshNonce) {
        if (!wantsWeather) return@LaunchedEffect
        keepWeatherFresh(context, data, wantsWind, weather)
    }
}

/** Shows the cached reading, refetches when due or when it lacks what the choices need. Runs until cancelled. */
private suspend fun keepWeatherFresh(
    context: Context,
    data: IdleWidgetData,
    wantsWind: Boolean,
    weather: WeatherPrefs
) {
    // a reading fetched for another city or provider is not shown
    data.weather = IdleWeather.cachedFor(context, weather)
    while (true) {
        val latest = IdleWeather.cachedFor(context, weather)
        if (latest != null && latest.fetchedAtMs > (data.weather?.fetchedAtMs ?: 0L)) data.weather = latest
        val age = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
        // a stored reading can lack what the choices need now
        val shown = data.weather
        val missingData = (wantsWind && (shown?.windKmh ?: -1f) < 0f) ||
            (shown != null && WeatherDisplay.missingData(weather.pillMode, weather.effectiveReadouts(), shown))
        if (WeatherRefresh.shouldFetch(weather.refreshMs, shown != null, age, missingData)) {
            val fresh = IdleWeather.fetch(context, weather)
            if (fresh != null) data.weather = fresh
        }
        val shownAge = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
        // a failed lookup is retried in a few minutes
        delay(WeatherRefresh.waitMs(weather.refreshMs, data.weather != null, shownAge))
    }
}

private const val SPEED_SAMPLE_MS = 2000L

private const val CALENDAR_POLL_MS = 60_000L

private const val USAGE_SAVE_EVERY = 15
private const val MODE_POLL_MS = 5000L

internal fun readBattery(context: Context, data: IdleWidgetData, intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level >= 0 && scale > 0) data.batteryPercent = (level * 100 / scale).coerceIn(0, 100)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    data.charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    data.batteryVoltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).coerceAtLeast(0)
    data.batteryTempTenthsC = if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
        intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, IdleWidgetData.NO_TEMPERATURE)
    } else {
        IdleWidgetData.NO_TEMPERATURE
    }
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    data.batteryCurrentRaw = if (manager == null) Long.MIN_VALUE else {
        manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }
    if (data.charging) {
        data.chargeTimeRemainingMs = if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.computeChargeTimeRemaining()
        } else {
            -1L
        }
        data.batteryEmptyAtMs = 0L
    } else {
        data.chargeTimeRemainingMs = -1L
        var remainingMs = -1L
        if (Build.VERSION.SDK_INT >= 33) {
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            remainingMs = power?.batteryDischargePrediction?.toMillis() ?: -1L
        }
        if (remainingMs <= 0L && manager != null) {
            val counterUah = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            remainingMs = WidgetIslandContent.dischargeRemainingMs(counterUah, data.batteryCurrentRaw)
        }
        data.batteryEmptyAtMs = if (remainingMs > 0L) System.currentTimeMillis() + remainingMs else 0L
    }
}

private fun nextAlarmMs(context: Context): Long =
    quietly { (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock?.triggerTime } ?: 0L

object IdleWeather {
    const val RETRY_MS = WeatherRefresh.RETRY_MS
    private const val TAG = "IdleWeather"
    private const val PREFS = "idle_widget_weather"
    private const val GPS_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    private const val IP_MAX_AGE_MS = 6 * 60 * 60 * 1000L

    /** Bumped when a reading was fetched on request (Settings > Refresh now). */
    val refreshNonce = MutableStateFlow(0)

    fun notifyRefreshed() = refreshNonce.update { it + 1 }

    fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Stored reading, whatever it was fetched for. */
    fun cached(context: Context): WeatherSnapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("at")) return null
        return WeatherSnapshot(
            temperatureC = p.getFloat("t", 0f),
            weatherCode = p.getInt("c", 3),
            isDay = p.getBoolean("d", true),
            fetchedAtMs = p.getLong("at", 0L),
            windKmh = p.getFloat("w", -1f),
            windDirectionDeg = p.getInt("wd", -1),
            feelsLikeC = p.getFloat("fl", Float.NaN),
            // Readings stored before these fields existed count as not detailed
            detailed = p.getBoolean("det", false),
            extras = WeatherExtras.fromJson(p.getString("ex", null))
        )
    }

    fun cachedFor(context: Context, prefs: WeatherPrefs): WeatherSnapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = p.getString("sig", null) ?: WeatherConfig.LEGACY_SIGNATURE
        if (stored != WeatherConfig.signature(prefs)) return null
        return cached(context)
    }

    /** Keeps the last known coarse location (rounded). */
    @SuppressLint("MissingPermission")
    fun captureLocation(context: Context): Boolean {
        if (!hasLocationPermission(context)) return false
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val best = quietly {
            lm.getProviders(true).mapNotNull { provider -> quietly { lm.getLastKnownLocation(provider) } }
                .maxByOrNull { it.time }
        } ?: return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("gps_lat", round1(best.latitude))
            .putFloat("gps_lon", round1(best.longitude))
            .putLong("gps_at", System.currentTimeMillis())
            .apply()
        return true
    }

    fun hasStoredPlace(context: Context): Boolean = storedGps(context, Long.MAX_VALUE) != null
            || storedIp(context, Long.MAX_VALUE) != null

    suspend fun fetch(context: Context, prefs: WeatherPrefs = WeatherPrefs()): WeatherSnapshot? =
            withContext(Dispatchers.IO) {
        runSuspendCatching {
            val place = resolvePlace(context, prefs)
            if (place == null) {
                AppLogRecorder.w(TAG, "No place for the weather (source ${prefs.source})")
                return@runSuspendCatching null
            }
            val url = WeatherConfig.forecastUrl(prefs, place.first, place.second)
            if (url == null) {
                AppLogRecorder.w(TAG, "The custom weather provider has no valid https address")
                return@runSuspendCatching null
            }
            val json = httpGet(url) ?: return@runSuspendCatching null
            val readouts = prefs.effectiveReadouts()
            var snapshot = WeatherParser.parseForecast(json, readouts, System.currentTimeMillis())
                ?: return@runSuspendCatching null
            // Air quality is a second request to Open-Meteo's own service
            if (WeatherReadouts.AQI in readouts) {
                val aqi = quietly {
                    httpGet(WeatherConfig.airQualityUrl(place.first, place.second))?.let { WeatherParser.parseAqi(it) }
                }
                val extras = snapshot.extras
                if (aqi != null && extras != null) snapshot = snapshot.copy(extras = extras.copy(aqi = aqi))
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("t", snapshot.temperatureC)
                .putInt("c", snapshot.weatherCode)
                .putBoolean("d", snapshot.isDay)
                .putLong("at", snapshot.fetchedAtMs)
                .putFloat("w", snapshot.windKmh)
                .putInt("wd", snapshot.windDirectionDeg)
                .putFloat("fl", snapshot.feelsLikeC)
                .putBoolean("det", snapshot.detailed)
                .putString("ex", snapshot.extras?.toJson())
                .putString("sig", WeatherConfig.signature(prefs))
                .apply()
            snapshot
        }.getOrElse {
            AppLogRecorder.w(TAG, "Weather lookup failed", it)
            null
        }
    }

    /** City search for the manual source (Open-Meteo's geocoding service, no key). */
    suspend fun searchCities(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        val url = WeatherConfig.geocodingUrl(query, Locale.getDefault().language) ?: return@withContext emptyList()
        runSuspendCatching { httpGet(url)?.let { WeatherConfig.parseCities(it) } ?: emptyList() }.getOrElse {
            AppLogRecorder.w(TAG, "City search failed", it)
            emptyList()
        }
    }

    private fun resolvePlace(context: Context, prefs: WeatherPrefs): Pair<Float, Float>? = when (prefs.source) {
        WeatherConfig.SOURCE_MANUAL ->
            if (WeatherConfig.hasManualPlace(prefs)) prefs.manualLat to prefs.manualLon else null
        // No location reading and no internet place lookup
        WeatherConfig.SOURCE_LAST_KNOWN -> storedGps(context, Long.MAX_VALUE) ?: storedIp(context, Long.MAX_VALUE)
        else -> {
            captureLocation(context)
            storedGps(context, GPS_MAX_AGE_MS) ?: ipPlace(context)
        }
    }

    private fun storedGps(context: Context, maxAgeMs: Long): Pair<Float, Float>? {
        if (!hasLocationPermission(context)) return null
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("gps_at")) return null
        if (System.currentTimeMillis() - p.getLong("gps_at", 0L) > maxAgeMs) return null
        return p.getFloat("gps_lat", 0f) to p.getFloat("gps_lon", 0f)
    }

    private fun storedIp(context: Context, maxAgeMs: Long): Pair<Float, Float>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("ip_at")) return null
        if (System.currentTimeMillis() - p.getLong("ip_at", 0L) > maxAgeMs) return null
        return p.getFloat("ip_lat", 0f) to p.getFloat("ip_lon", 0f)
    }

    private fun ipPlace(context: Context): Pair<Float, Float>? {
        storedIp(context, IP_MAX_AGE_MS)?.let { return it }
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val body = httpGet("https://ipwho.is/") ?: return null
        val json = JSONObject(body)
        if (!json.optBoolean("success", true) || !json.has("latitude") || !json.has("longitude")) return null
        val lat = round1(json.optDouble("latitude"))
        val lon = round1(json.optDouble("longitude"))
        p.edit().putFloat("ip_lat", lat).putFloat("ip_lon", lon).putLong("ip_at", System.currentTimeMillis()).apply()
        return lat to lon
    }

    private fun httpGet(address: String): String? {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "SmartIsland")
        return try {
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun round1(value: Double): Float = WeatherConfig.round1(value)
}
