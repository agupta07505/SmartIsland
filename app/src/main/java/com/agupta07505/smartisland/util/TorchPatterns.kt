/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import com.agupta07505.smartisland.model.TorchPattern
import com.agupta07505.smartisland.model.TorchStep
import com.agupta07505.smartisland.model.TorchTiming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Blinking patterns made by switching the torch on and off, which works on every phone with a torch (no brightness
 * support needed). The strobe runs at 1 to 30 flashes a second and its rate can change while it runs.
 */
object TorchPatterns {
    private const val TAG = "TorchPatterns"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val running = MutableStateFlow<TorchPattern?>(null)
    @Volatile private var strobeHz = TorchTiming.DEFAULT_STROBE_HZ

    /** The pattern that is running, it turns null when it stops for any reason. */
    val activeState: StateFlow<TorchPattern?> = running.asStateFlow()
    val active: TorchPattern? get() = running.value

    /** Starts [pattern], replacing one that is already running. Returns false when the phone has no torch. */
    @Synchronized
    fun start(context: Context, pattern: TorchPattern, hz: Int = TorchTiming.DEFAULT_STROBE_HZ): Boolean {
        val info = TorchCapabilities.detect(context) ?: return false
        val cm = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        job?.cancel()
        strobeHz = hz.coerceIn(TorchTiming.MIN_STROBE_HZ, TorchTiming.MAX_STROBE_HZ)
        running.value = pattern
        job = scope.launch {
            val self = coroutineContext[Job]
            try {
                when (pattern) {
                    TorchPattern.Sos -> runSteps(cm, info, TorchTiming.sosSteps())
                    TorchPattern.Strobe -> runStrobe(cm, info)
                }
            } catch (e: Exception) {
                // Most often the camera was taken by another app; stop instead of retrying in a loop
                if (e !is kotlinx.coroutines.CancellationException) AppLogRecorder.e(TAG, "Torch pattern stopped", e)
            } finally {
                // Only a pattern that ends by itself switches the torch off; a stopped or replaced one leaves it to
                // whoever stopped it, or its late "off" could land after their "on"
                synchronized(this@TorchPatterns) {
                    if (job === self) {
                        job = null
                        running.value = null
                        quietly { cm.setTorchMode(info.cameraId, false) }
                    }
                }
            }
        }
        return true
    }

    /** Changes the strobe's rate without restarting it, the next beat follows the new rate. */
    fun setStrobeHz(hz: Int) {
        strobeHz = hz.coerceIn(TorchTiming.MIN_STROBE_HZ, TorchTiming.MAX_STROBE_HZ)
    }

    private suspend fun runSteps(cm: CameraManager, info: TorchInfo, steps: List<TorchStep>) {
        while (currentCoroutineContext().isActive) {
            for (step in steps) {
                if (!currentCoroutineContext().isActive) return
                setTorch(cm, info, step.on)
                delay(step.ms)
            }
        }
    }

    /** Beats are timed against a deadline, so the delay's own rounding does not add up into a slower rate. */
    private suspend fun runStrobe(cm: CameraManager, info: TorchInfo) {
        var hz = strobeHz
        var due = System.nanoTime()
        var on = true
        while (currentCoroutineContext().isActive) {
            setTorch(cm, info, on)
            on = !on
            if (strobeHz != hz) {
                hz = strobeHz
                due = System.nanoTime()
            }
            due += TorchTiming.beatNanos(hz)
            val waitMs = (due - System.nanoTime()) / 1_000_000L
            if (waitMs > 0) delay(waitMs)
        }
    }

    /**
     * Plain setTorchMode(true) lights the torch at the phone's default brightness, which is often dim. A blinking
     * pattern is for being seen, so on phones with brightness steps the "on" beat goes out at the highest step.
     */
    private fun setTorch(cm: CameraManager, info: TorchInfo, on: Boolean) {
        if (on && info.supportsBrightness && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cm.turnOnTorchWithStrengthLevel(info.cameraId, info.maxLevel)
        } else {
            cm.setTorchMode(info.cameraId, on)
        }
    }

    /** Stops the running pattern and waits until it is gone. The torch is left as it was, the caller sets it. */
    suspend fun stop() {
        val stopped = synchronized(this) {
            running.value = null
            job.also { job = null }
        }
        stopped?.cancelAndJoin()
    }
}
