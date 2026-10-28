/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.DeviceStates
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandTimings
import com.agupta07505.smartisland.model.ModeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Posts a brief card when airplane mode, the ringer mode (ring, vibrate, silent), Do Not Disturb or a mode (Driving,
 * Bedtime ... Android 15+) changes. What was on at start is the baseline, not an event
 */
class DeviceStateEventMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings,
    /** The island shows no card while the notification shade is open, a card's time starts once it is closed. */
    private val shadeOpen: StateFlow<Boolean>? = null
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var receiver: BroadcastReceiver? = null
    private var airplaneObserver: ContentObserver? = null
    private val jobs = HashMap<String, Job>()

    private var airplaneOn = false
    private var ringerMode = RINGER_UNKNOWN
    private var mode = ModeStatus.Off

    fun start() {
        if (receiver != null) return
        airplaneOn = readAirplane()
        ringerMode = audioManager?.ringerMode ?: RINGER_UNKNOWN
        mode = DndModeReader.read(context)

        val registered = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                runCatchingLogged(TAG, "device state broadcast failed") { handle(intent) }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(registered, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(registered, filter)
            }
            receiver = registered
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "register device state receiver failed", e)
        }
        // a few devices never deliver the airplane broadcast, so also watch the setting.
        // handleAirplane skips repeats
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                runCatchingLogged(TAG, "airplane setting change failed") { handleAirplane("setting") }
            }
        }
        runCatchingLogged(TAG, "register airplane observer failed") {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, observer
            )
            airplaneObserver = observer
        }
    }

    fun stop() {
        receiver?.let { r ->
            runCatchingLogged(TAG, "unregister device state receiver failed") { context.unregisterReceiver(r) }
        }
        receiver = null
        airplaneObserver?.let { o ->
            runCatchingLogged(TAG, "unregister airplane observer failed") { context.contentResolver.unregisterContentObserver(o) }
        }
        airplaneObserver = null
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        listOf(KEY_AIRPLANE, KEY_RINGER, KEY_MODE).forEach(repository::removeNotification)
    }

    private fun handle(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> handleAirplane(
                "broadcast", if (intent.hasExtra("state")) intent.getBooleanExtra("state", false) else null
            )
            AudioManager.RINGER_MODE_CHANGED_ACTION -> handleRinger()
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> handleMode()
        }
    }

    private fun handleAirplane(source: String, broadcastState: Boolean? = null) {
        val setting = readAirplane()
        // power menu on some phones fires the broadcast before the setting is written, so reading the
        // setting here can give the old value. prefer the broadcast's state extra if there is one
        val on = broadcastState ?: setting
        if (broadcastState != null && broadcastState != setting) {
            AppLogRecorder.i(TAG, "airplane $source: broadcast says $broadcastState, setting says $setting")
        }
        if (on == airplaneOn) {
            AppLogRecorder.i(TAG, "airplane $source: no change (on=$on), ignored")
            return
        }
        airplaneOn = on
        AppLogRecorder.i(TAG, "airplane $source: on=$on")
        show(KEY_AIRPLANE, DeviceStates.airplane(on))
    }

    private fun handleRinger() {
        val current = audioManager?.ringerMode ?: return
        if (current == ringerMode) return
        ringerMode = current
        DeviceStates.ringer(current)?.let { show(KEY_RINGER, it) }
    }

    private fun handleMode() {
        val current = DndModeReader.read(context)
        val card = DeviceStates.change(mode, current)
        mode = current
        card?.let { show(KEY_MODE, it) }
    }

    private fun readAirplane(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    private fun show(key: String, card: DeviceStates.Card) {
        if (!settingsProvider().isEventEnabled(card.event.id)) {
            AppLogRecorder.i(TAG, "${card.category}: event ${card.event.id} is switched off, no card")
            return
        }
        jobs.remove(key)?.cancel()
        repository.removeNotification(key)
        jobs[key] = scope.launch {
            // Switched from the quick settings the shade is open: post once it is closed so the card arrives
            // where it can be seen and pulses, instead of arriving hidden and being used up behind the shade
            awaitShadeClosed(shadeOpen)
            AppLogRecorder.i(TAG, "posting ${card.category}")
            repository.postSystemEvent(
                IslandNotification(
                    key = key,
                    packageName = "com.android.systemui",
                    appName = card.event.shortLabel,
                    title = card.title,
                    text = card.text,
                    category = card.category,
                    mode = IslandMode.Bluetooth,
                    timeMillis = System.currentTimeMillis()
                ),
                card.event.id,
                settingsProvider()
            )
            delay(IslandTimings.CONNECTIVITY_CARD_MS)
            repository.removeNotification(key)
        }
    }

    private companion object {
        const val TAG = "DeviceStateEventMonitor"
        const val KEY_AIRPLANE = "system_airplane"
        const val KEY_RINGER = "system_ringer"
        const val KEY_MODE = "system_mode"
        const val RINGER_UNKNOWN = -1
    }
}
