/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.function.Consumer

/**
 * Tells the overlay when the screen is being recorded or cast (Rule_Book 8.2), so it can add FLAG_SECURE and
 * black the island out of the video. Three signals, any one is enough:
 *  - Android 15+: [WindowManager.addScreenRecordingCallback] (needs DETECT_SCREEN_RECORDING). It reports
 *    whether any of this app's windows is in a recording. The island window turns secure and drops out of
 *    it, but the invisible system-bar probe ([ImmersiveModeMonitor]) stays in, so the state holds steady.
 *  - Casting / mirroring: a presentation display (Cast, Wi-Fi display, HDMI) or a public virtual display (a
 *    MediaProjection mirror) is attached; see [CaptureDisplays]. The display listener catches it being added.
 *  - A screen-recorder alert on the island ([com.agupta07505.smartisland.model.IslandMode.ScreenRecording]),
 *    fed through [setRecorderAlertActive]. This covers older Android and third-party recorders.
 */
class ScreenCaptureMonitor(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val recordingApi = MutableStateFlow(false)
    private val presentationDisplay = MutableStateFlow(false)
    private val recorderAlert = MutableStateFlow(false)
    private val capturing = MutableStateFlow(false)

    /** True while any capture signal is on. */
    val isCapturing: StateFlow<Boolean> = capturing

    private val mainHandler = Handler(Looper.getMainLooper())
    private var displayManager: DisplayManager? = null
    private var recordingCallback: Consumer<Int>? = null
    private var started = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshDisplays()
        override fun onDisplayRemoved(displayId: Int) = refreshDisplays()
        override fun onDisplayChanged(displayId: Int) = refreshDisplays()
    }

    fun start() {
        if (started) return
        started = true
        runCatchingLogged(TAG, "Display listener registration failed") {
            val manager = context.getSystemService(DisplayManager::class.java)
            displayManager = manager
            manager?.registerDisplayListener(displayListener, mainHandler)
            refreshDisplays()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            registerRecordingCallback()
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatchingLogged(TAG, "Display listener removal failed") {
            displayManager?.unregisterDisplayListener(displayListener)
        }
        displayManager = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            unregisterRecordingCallback()
        }
        recordingApi.value = false
        presentationDisplay.value = false
        publish()
    }

    /** Whether a screen-recorder notification is on the island right now. */
    fun setRecorderAlertActive(active: Boolean) {
        if (recorderAlert.value == active) return
        recorderAlert.value = active
        publish()
    }

    private fun refreshDisplays() {
        val manager = displayManager ?: return
        val cast = runCatchingLogged(TAG, "Display query failed") {
            val presentation = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).isNotEmpty()
            // A MediaProjection mirror is a virtual display; ordinary apps only see the public ones.
            presentation || manager.displays.any { display ->
                CaptureDisplays.isCaptureDisplay(
                    isDefault = display.displayId == Display.DEFAULT_DISPLAY,
                    flags = display.flags,
                    type = displayType(display)
                )
            }
        } ?: false
        if (presentationDisplay.value != cast) {
            presentationDisplay.value = cast
            publish()
        }
    }

    /** `Display.getType()` is hidden; null when this build does not expose it. */
    private fun displayType(display: Display): Int? = runCatching {
        Display::class.java.getMethod("getType").invoke(display) as? Int
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun registerRecordingCallback() {
        runCatchingLogged(TAG, "Screen-recording callback registration failed") {
            val callback = Consumer<Int> { state ->
                mainHandler.post {
                    recordingApi.value = state == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
                    publish()
                }
            }
            val initial = windowManager.addScreenRecordingCallback(context.mainExecutor, callback)
            recordingCallback = callback
            recordingApi.value = initial == WindowManager.SCREEN_RECORDING_STATE_VISIBLE
            publish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun unregisterRecordingCallback() {
        val callback = recordingCallback ?: return
        recordingCallback = null
        runCatchingLogged(TAG, "Screen-recording callback removal failed") {
            windowManager.removeScreenRecordingCallback(callback)
        }
    }

    private fun publish() {
        val active = isCaptureActive(recordingApi.value, presentationDisplay.value, recorderAlert.value)
        if (capturing.value != active) {
            android.util.Log.d(TAG, "Screen capture ${if (active) "started" else "ended"}")
            capturing.value = active
        }
    }

    companion object {
        private const val TAG = "ScreenCaptureMonitor"

        /** Any one signal means the screen may be leaving the device. */
        fun isCaptureActive(recordingApi: Boolean, presentationDisplay: Boolean, recorderAlert: Boolean): Boolean =
            recordingApi || presentationDisplay || recorderAlert
    }
}

/**
 * Which attached displays mean the screen is leaving the device (Rule_Book 8.2). Pure, so it is unit-testable.
 * Apps cannot register `MediaProjectionManager` callbacks, but a projection that mirrors the screen shows up as
 * a display: Cast and Wi-Fi display as presentation or Wi-Fi displays, screen recorders and mirroring tools as
 * virtual ones. A private virtual display (owned by one app, e.g. for its own rendering) is not a mirror of
 * this screen, and Android only lists one to its owner anyway. Built-in second screens (a foldable's cover
 * display) are internal displays and never count.
 */
object CaptureDisplays {
    // Display.FLAG_* (public).
    const val FLAG_PRIVATE = 1 shl 2
    const val FLAG_PRESENTATION = 1 shl 3

    // Display.TYPE_* (hidden, read through reflection).
    const val TYPE_INTERNAL = 1
    const val TYPE_EXTERNAL = 2
    const val TYPE_WIFI = 3
    const val TYPE_OVERLAY = 4
    const val TYPE_VIRTUAL = 5

    /** [type] is null when the hidden type could not be read; then only the presentation flag counts. */
    fun isCaptureDisplay(isDefault: Boolean, flags: Int, type: Int?): Boolean {
        if (isDefault) return false
        if (flags and FLAG_PRESENTATION != 0) return true
        if (flags and FLAG_PRIVATE != 0) return false
        return type == TYPE_WIFI || type == TYPE_VIRTUAL || type == TYPE_OVERLAY || type == TYPE_EXTERNAL
    }
}
