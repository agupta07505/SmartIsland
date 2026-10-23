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
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.ModeStatus
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.NotificationSummary
import com.agupta07505.smartisland.model.CityResult
import com.agupta07505.smartisland.model.WeatherConfig
import com.agupta07505.smartisland.model.WeatherPrefs
import com.agupta07505.smartisland.model.WeatherRefresh
import com.agupta07505.smartisland.model.WeatherDisplay
import com.agupta07505.smartisland.model.WeatherExtras
import com.agupta07505.smartisland.model.WeatherParser
import com.agupta07505.smartisland.model.WeatherReadouts
import com.agupta07505.smartisland.model.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** The live values the idle widgets show, as Compose state. Filled by [rememberIdleWidgetData]. */
class IdleWidgetData internal constructor() {
    /** Wall clock, refreshed every minute (and when the time or zone changes). */
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
        internal set

    /** Battery level 0..100, or -1 until it is known. */
    var batteryPercent by mutableIntStateOf(-1)
        internal set
    var charging by mutableStateOf(false)
        internal set

    /** Battery voltage in mV, 0 = unknown. For the widget island. */
    var batteryVoltageMv by mutableIntStateOf(0)
        internal set

    /** Battery temperature in tenths of a degree Celsius, [NO_TEMPERATURE] = unknown. */
    var batteryTempTenthsC by mutableIntStateOf(NO_TEMPERATURE)
        internal set

    /** Time until the battery is full (ms), -1 = the system cannot tell. */
    var chargeTimeRemainingMs by mutableLongStateOf(-1L)
        internal set

    /** The battery's current reading as the device reports it, Long.MIN_VALUE = unsupported. */
    var batteryCurrentRaw by mutableLongStateOf(Long.MIN_VALUE)
        internal set

    /** When the battery is expected to run out (epoch ms) while it is not charging, 0 = no forecast. */
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

    /** What the device is connected to (read with the speed), and today's traffic counted per connection, for the net speed island. */
    var connection by mutableStateOf(com.agupta07505.smartisland.model.NetConnection.None)
        internal set
    var usageToday by mutableStateOf(com.agupta07505.smartisland.model.DailyUsage())
        internal set

    /** The next calendar event within 24 hours, for the date widget; null without the permission or without an event. */
    var nextEvent by mutableStateOf<com.agupta07505.smartisland.model.NextEvent?>(null)
        internal set

    /** True while Android's Data Saver is switched on; the net speed island says so. */
    var dataSaver by mutableStateOf(false)
        internal set

    /** The two directions on their own (bytes per second), for the widget island. */
    var downBytesPerSecond by mutableLongStateOf(0L)
        internal set
    var upBytesPerSecond by mutableLongStateOf(0L)
        internal set

    var weather by mutableStateOf<WeatherSnapshot?>(null)
        internal set

    /** Unread messages and mail, in total and per app (see [NotificationCountTracker]). */
    var notificationSummary by mutableStateOf(NotificationSummary.Empty)
        internal set

    /** The Do Not Disturb mode that is on, or [ModeStatus.Off]. */
    var mode by mutableStateOf(ModeStatus.Off)
        internal set

    companion object {
        const val NO_TEMPERATURE = Int.MIN_VALUE
    }
}

/**
 * Starts what the [kinds] need and stops it again when [active] turns false (a card is on the island, the screen is
 * off, the widgets are switched off). Weather refreshes only with [allowNetwork].
 */
@Composable
fun rememberIdleWidgetData(
    kinds: List<IdleWidgetKind>,
    active: Boolean,
    allowNetwork: Boolean,
    /** Where the weather's place comes from, how often it is asked and which service answers (Settings > Idle widgets > Weather). */
    weather: WeatherPrefs = WeatherPrefs()
): IdleWidgetData {
    val context = LocalContext.current.applicationContext
    val data = remember { IdleWidgetData() }
    val wantsBattery = active && IdleWidgetKind.Battery in kinds
    val wantsAlarm = active && IdleWidgetKind.NextAlarm in kinds
    val wantsTime = active && (IdleWidgetKind.Clock in kinds || IdleWidgetKind.LiveClock in kinds || IdleWidgetKind.Date in kinds || wantsAlarm)
    val wantsSpeed = active && IdleWidgetKind.NetSpeed in kinds
    val wantsWind = IdleWidgetKind.Wind in kinds
    val wantsWeather = active && allowNetwork && (IdleWidgetKind.Weather in kinds || wantsWind)
    val wantsNotifications = active && IdleWidgetKind.Notifications in kinds
    val wantsMode = active && IdleWidgetKind.Modes in kinds

    LaunchedEffect(wantsNotifications) {
        if (!wantsNotifications) {
            data.notificationSummary = NotificationSummary.Empty
            return@LaunchedEffect
        }
        NotificationCountTracker.entries.collect { data.notificationSummary = NotificationCounts.summarize(it) }
    }

    // Do Not Disturb: the system announces filter and policy changes; a mode switching to another one keeps the
    // filter, so a slow check backs the broadcasts up.
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
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(wantsMode) {
        if (!wantsMode) return@LaunchedEffect
        while (true) {
            delay(MODE_POLL_MS)
            val now = withContext(Dispatchers.Default) { DndModeReader.read(context) }
            if (now != data.mode) data.mode = now
        }
    }

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
        // Whatever the clock showed while the widgets were off is stale.
        data.nowMs = System.currentTimeMillis()
        if (wantsAlarm) data.nextAlarmMs = nextAlarmMs(context)
        // A sticky broadcast (the battery one) answers the registration itself, so the level is right at once.
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (wantsBattery && sticky != null && sticky.action == Intent.ACTION_BATTERY_CHANGED) readBattery(context, data, sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // The calendar is read once a minute (and the permission checked each time, so allowing it takes effect within a minute).
    val wantsCalendar = active && IdleWidgetKind.Date in kinds
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

    LaunchedEffect(wantsSpeed) {
        if (!wantsSpeed) {
            data.speedBytesPerSecond = 0L
            data.downBytesPerSecond = 0L
            data.upBytesPerSecond = 0L
            data.connection = com.agupta07505.smartisland.model.NetConnection.None
            data.dataSaver = false
            return@LaunchedEffect
        }
        val store = NetUsageStore(context)
        fun todayKey(): Int = java.util.Calendar.getInstance().let {
            com.agupta07505.smartisland.model.NetUsage.dayKey(it.get(java.util.Calendar.YEAR), it.get(java.util.Calendar.MONTH), it.get(java.util.Calendar.DAY_OF_MONTH))
        }
        fun mobileTotal(): Long {
            val r = TrafficStats.getMobileRxBytes()
            val t = TrafficStats.getMobileTxBytes()
            return if (r == TrafficStats.UNSUPPORTED.toLong() || t == TrafficStats.UNSUPPORTED.toLong()) -1L else r + t
        }
        var usage = com.agupta07505.smartisland.model.NetUsage.rolled(store.load(), todayKey())
        data.usageToday = usage
        data.connection = NetConnectionReader.read(context)
        data.dataSaver = DataSaverReader.isOn(context)
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastMobile = mobileTotal()
        var lastAt = SystemClock.elapsedRealtime()
        var unsaved = 0
        try {
            while (true) {
                delay(SPEED_SAMPLE_MS)
                val rx = TrafficStats.getTotalRxBytes()
                val tx = TrafficStats.getTotalTxBytes()
                val mobile = mobileTotal()
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
                    // The daily counter: what the device moved since the last sample, split between mobile data and the rest (Wi-Fi).
                    val total = (rx - lastRx).coerceAtLeast(0L) + (tx - lastTx).coerceAtLeast(0L)
                    val mobileDelta = if (mobile >= 0L && lastMobile >= 0L) mobile - lastMobile else 0L
                    val (wifiDelta, mobileShare) = com.agupta07505.smartisland.model.NetUsage.split(total, mobileDelta)
                    val next = com.agupta07505.smartisland.model.NetUsage.add(usage, todayKey(), wifiDelta, mobileShare)
                    if (next != usage) {
                        usage = next
                        data.usageToday = usage
                        unsaved++
                    }
                    // Written every half minute (and when this stops), so a restart loses at most that.
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

    // A refresh asked for in Settings bumps this, which restarts the loop below so the new reading shows at once.
    val refreshNonce by IdleWeather.refreshNonce.collectAsState()
    LaunchedEffect(wantsWeather, wantsWind, weather, refreshNonce) {
        if (!wantsWeather) return@LaunchedEffect
        // A reading fetched for another city or provider is not shown; the stored one only counts while it matches the choices.
        data.weather = IdleWeather.cachedFor(context, weather)
        while (true) {
            val latest = IdleWeather.cachedFor(context, weather)
            if (latest != null && latest.fetchedAtMs > (data.weather?.fetchedAtMs ?: 0L)) data.weather = latest
            val age = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
            // A stored reading can lack what the choices need now (the wind, feels like, an island row): it is asked again once, not more
            // than every retry period, and never in Manual (shouldFetch decides).
            val shown = data.weather
            val missingData = (wantsWind && (shown?.windKmh ?: -1f) < 0f) ||
                (shown != null && WeatherDisplay.missingData(weather.pillMode, weather.effectiveReadouts(), shown))
            if (WeatherRefresh.shouldFetch(weather.refreshMs, shown != null, age, missingData)) {
                val fresh = IdleWeather.fetch(context, weather)
                if (fresh != null) data.weather = fresh
            }
            val shownAge = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
            // A failed lookup is retried in a few minutes; a good one waits for the next refresh (Manual just looks
            // at the stored reading now and then).
            delay(WeatherRefresh.waitMs(weather.refreshMs, data.weather != null, shownAge))
        }
    }
    return data
}

private const val SPEED_SAMPLE_MS = 2000L

private const val CALENDAR_POLL_MS = 60_000L


private const val USAGE_SAVE_EVERY = 15
private const val MODE_POLL_MS = 5000L

private fun readBattery(context: Context, data: IdleWidgetData, intent: Intent) {
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
    // The current feeds the power readout (charging) and the run-out forecast (not charging).
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    data.batteryCurrentRaw = if (manager == null) Long.MIN_VALUE else {
        runCatching { manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrDefault(Long.MIN_VALUE)
    }
    if (data.charging) {
        data.chargeTimeRemainingMs = if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { manager.computeChargeTimeRemaining() }.getOrDefault(-1L)
        } else {
            -1L
        }
        data.batteryEmptyAtMs = 0L
    } else {
        data.chargeTimeRemainingMs = -1L
        // When the battery runs out: the system's own prediction (Android 13 and up) when it has one, else charge
        // left over the present draw.
        var remainingMs = -1L
        if (Build.VERSION.SDK_INT >= 33) {
            val power = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            remainingMs = runCatching { power?.batteryDischargePrediction?.toMillis() ?: -1L }.getOrDefault(-1L)
        }
        if (remainingMs <= 0L && manager != null) {
            val counterUah = runCatching { manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) }.getOrDefault(0L)
            remainingMs = com.agupta07505.smartisland.model.WidgetIslandContent.dischargeRemainingMs(counterUah, data.batteryCurrentRaw)
        }
        data.batteryEmptyAtMs = if (remainingMs > 0L) System.currentTimeMillis() + remainingMs else 0L
    }
}

private fun nextAlarmMs(context: Context): Long =
    runCatching { (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock?.triggerTime }
        .getOrNull() ?: 0L

/**
 * Weather for the weather widget: Open-Meteo by default (no key, no account), or a compatible service the person set
 * up.
 */
object IdleWeather {
    /** The refresh time of the default choice; the real period is [WeatherPrefs.refreshMs]. */
    const val REFRESH_MS = WeatherConfig.DEFAULT_REFRESH_MINUTES * 60 * 1000L
    const val RETRY_MS = WeatherRefresh.RETRY_MS
    private const val TAG = "IdleWeather"
    private const val PREFS = "idle_widget_weather"
    private const val GPS_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    private const val IP_MAX_AGE_MS = 6 * 60 * 60 * 1000L

    /**
     * Bumped when a reading was fetched on request (Settings > Refresh now), so whoever shows the weather looks at
     * the stored one again.
     */
    val refreshNonce = MutableStateFlow(0)

    fun notifyRefreshed() = refreshNonce.update { it + 1 }

    fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** The stored reading, whatever it was fetched for. Prefer [cachedFor]. */
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
            // Readings stored before these fields existed count as not detailed.
            detailed = p.getBoolean("det", false),
            extras = WeatherExtras.fromJson(p.getString("ex", null))
        )
    }

    /** The stored reading, only while it was fetched for the same place choice and provider as [prefs]; null otherwise. */
    fun cachedFor(context: Context, prefs: WeatherPrefs): WeatherSnapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = p.getString("sig", null) ?: WeatherConfig.LEGACY_SIGNATURE
        if (stored != WeatherConfig.signature(prefs)) return null
        return cached(context)
    }

    /** Keeps the last known coarse location (rounded). True when a place was stored. Call it while the app is in front. */
    @SuppressLint("MissingPermission")
    fun captureLocation(context: Context): Boolean {
        if (!hasLocationPermission(context)) return false
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val best = runCatching {
            lm.getProviders(true).mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("gps_lat", round1(best.latitude))
            .putFloat("gps_lon", round1(best.longitude))
            .putLong("gps_at", System.currentTimeMillis())
            .apply()
        return true
    }

    fun hasStoredPlace(context: Context): Boolean = storedGps(context, Long.MAX_VALUE) != null || storedIp(context, Long.MAX_VALUE) != null

    /** Stores and returns the reading; null when there is no place or the service did not answer. */
    suspend fun fetch(context: Context, prefs: WeatherPrefs = WeatherPrefs()): WeatherSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val place = resolvePlace(context, prefs)
            if (place == null) {
                AppLogRecorder.w(TAG, "No place for the weather (source ${prefs.source})")
                return@runCatching null
            }
            val url = WeatherConfig.forecastUrl(prefs, place.first, place.second)
            if (url == null) {
                AppLogRecorder.w(TAG, "The custom weather provider has no valid https address")
                return@runCatching null
            }
            val json = httpGet(url) ?: return@runCatching null
            val readouts = prefs.effectiveReadouts()
            var snapshot = WeatherParser.parseForecast(json, readouts, System.currentTimeMillis()) ?: return@runCatching null
            // Air quality is a second request to Open-Meteo's own service. A failure only leaves that row out; the weather still counts.
            if (WeatherReadouts.AQI in readouts) {
                val aqi = runCatching {
                    httpGet(WeatherConfig.airQualityUrl(place.first, place.second))?.let { WeatherParser.parseAqi(it) }
                }.getOrNull()
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

    /**
     * City search for the manual source (Open-Meteo's geocoding service, no key). Empty when the text is too short,
     * offline or nothing matches.
     */
    suspend fun searchCities(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        val url = WeatherConfig.geocodingUrl(query, Locale.getDefault().language) ?: return@withContext emptyList()
        runCatching { httpGet(url)?.let { WeatherConfig.parseCities(it) } ?: emptyList() }.getOrElse {
            AppLogRecorder.w(TAG, "City search failed", it)
            emptyList()
        }
    }

    private fun resolvePlace(context: Context, prefs: WeatherPrefs): Pair<Float, Float>? = when (prefs.source) {
        WeatherConfig.SOURCE_MANUAL ->
            if (WeatherConfig.hasManualPlace(prefs)) prefs.manualLat to prefs.manualLon else null
        // No location reading and no internet place lookup: only what an earlier reading left behind.
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
