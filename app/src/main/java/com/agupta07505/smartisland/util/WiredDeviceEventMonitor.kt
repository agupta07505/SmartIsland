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
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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

/**
 * Posts a brief island card when a USB device (an OTG drive, keyboard, accessory, ...) or a pair of wired headphones
 * (3.5 mm or USB-C) is plugged in or unplugged.
 */
class WiredDeviceEventMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var usbReceiver: BroadcastReceiver? = null
    private var audioCallback: AudioDeviceCallback? = null
    private var startedAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val wiredIds = HashSet<Int>()
    private var usbJob: Job? = null

    private var hostConnected = false
    private var headphonesJob: Job? = null

    fun start() {
        if (usbReceiver != null) return
        startedAt = SystemClock.elapsedRealtime()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                runCatchingLogged(TAG, "USB broadcast failed") { handleUsb(intent) }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction(ACTION_USB_STATE)
        }
        runCatchingLogged(TAG, "register USB receiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
            usbReceiver = receiver
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    addedDevices.filter(::isWiredOutput).forEach { handleHeadphones(it, plugged = true) }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    removedDevices.filter(::isWiredOutput).forEach { handleHeadphones(it, plugged = false) }
                }
            }
            audioCallback = callback
            runCatchingLogged(TAG, "register audio device callback failed") {
                audioManager?.registerAudioDeviceCallback(callback, handler)
            }
        }
    }

    fun stop() {
        usbReceiver?.let { r -> runCatchingLogged(TAG, "unregister USB receiver failed") { context.unregisterReceiver(r) } }
        usbReceiver = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioCallback?.let { cb ->
                runCatchingLogged(TAG, "unregister audio device callback failed") { audioManager?.unregisterAudioDeviceCallback(cb) }
            }
        }
        audioCallback = null
        usbJob?.cancel()
        headphonesJob?.cancel()
        wiredIds.clear()
        hostConnected = false
        repository.removeNotification(KEY_USB)
        repository.removeNotification(KEY_HEADPHONES)
    }

    private fun isStartupReplay(): Boolean = SystemClock.elapsedRealtime() - startedAt < STARTUP_GRACE_MS

    private fun isWiredOutput(device: AudioDeviceInfo): Boolean =
        device.isSink && device.type in WIRED_TYPES

    private fun handleHostState(intent: Intent) {
        val connected = intent.getBooleanExtra("connected", false)
        if (connected == hostConnected) return
        hostConnected = connected
        if (isStartupReplay()) return
        val event = if (connected) IslandEvent.USB_CONNECTED else IslandEvent.USB_DISCONNECTED
        if (!settingsProvider().isEventEnabled(event.id)) return
        post(
            key = KEY_USB,
            appName = "USB",
            title = "Computer",
            plugged = connected,
            category = if (connected) "usb_connected" else "usb_disconnected",
            event = event,
            current = usbJob
        ).also { usbJob = it }
    }

    private fun handleUsb(intent: Intent) {
        if (intent.action == ACTION_USB_STATE) {
            handleHostState(intent)
            return
        }
        val attached = when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED, UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> true
            UsbManager.ACTION_USB_DEVICE_DETACHED, UsbManager.ACTION_USB_ACCESSORY_DETACHED -> false
            else -> return
        }
        val device: UsbDevice? = if (intent.action?.contains("DEVICE") == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
        } else null
        // A USB sound device is the headphones card's job.
        if (device != null && isAudioDevice(device)) return
        if (isStartupReplay()) return
        val event = if (attached) IslandEvent.USB_CONNECTED else IslandEvent.USB_DISCONNECTED
        if (!settingsProvider().isEventEnabled(event.id)) return
        val name = runCatching { device?.productName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "USB device"
        post(
            key = KEY_USB,
            appName = "USB",
            title = name,
            plugged = attached,
            category = if (attached) "usb_connected" else "usb_disconnected",
            event = event,
            current = usbJob
        ).also { usbJob = it }
    }

    private fun isAudioDevice(device: UsbDevice): Boolean =
        (0 until device.interfaceCount).any { device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_AUDIO }

    private fun handleHeadphones(device: AudioDeviceInfo, plugged: Boolean) {
        val changed = if (plugged) wiredIds.add(device.id) else wiredIds.remove(device.id)
        if (!changed) return
        if (isStartupReplay()) return
        val event = if (plugged) IslandEvent.HEADPHONES_CONNECTED else IslandEvent.HEADPHONES_DISCONNECTED
        if (!settingsProvider().isEventEnabled(event.id)) return
        val name = device.productName?.toString()?.takeIf { it.isNotBlank() && !isGenericName(it) } ?: "Wired Headphone"
        post(
            key = KEY_HEADPHONES,
            appName = "Headphone",
            title = name,
            plugged = plugged,
            category = if (plugged) "headphones_connected" else "headphones_disconnected",
            event = event,
            current = headphonesJob
        ).also { headphonesJob = it }
    }

    private fun isGenericName(name: String): Boolean =
        name.equals(Build.MODEL, ignoreCase = true) || name.trim().equals("h2w", ignoreCase = true)

    private fun post(
        key: String,
        appName: String,
        title: String,
        plugged: Boolean,
        category: String,
        event: IslandEvent,
        current: Job?
    ): Job {
        current?.cancel()
        repository.removeNotification(key)
        return scope.launch {
            repository.postNotification(
                IslandNotification(
                    key = key,
                    packageName = "com.android.systemui",
                    appName = appName,
                    title = title,
                    text = if (plugged) "Connected" else "Disconnected",
                    category = category,
                    mode = IslandMode.Bluetooth,
                    timeMillis = System.currentTimeMillis()
                ),
                autoExpand = !NotificationCooldownManager.shouldThrottleSystemEvent(event.id, settingsProvider())
            )
            delay(if (plugged) IslandTimings.CONNECTIVITY_CARD_MS else IslandTimings.DISCONNECT_CARD_MS)
            repository.removeNotification(key)
        }
    }

    private companion object {
        const val TAG = "WiredDeviceEventMonitor"
        const val KEY_USB = "system_usb"
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
        const val KEY_HEADPHONES = "system_headphones"
        /** The audio callback reports everything already plugged in when it registers; that is not news. */
        const val STARTUP_GRACE_MS = 2_000L

        val WIRED_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET
        )
    }
}
