/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.view.WindowManager
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.ui.IslandViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Extra window flags the overlay adds on top of its normal ones. Pure, so it is unit-testable.
 *  - [secure]: the screen is being recorded or cast (Rule_Book 8.2): FLAG_SECURE blacks the island out.
 *  - [untouchable]: SUPPRESSED by immersive mode (10.1) or holding IDLE for face unlock (8.2):
 *    FLAG_NOT_TOUCHABLE lets every touch through to what is underneath.
 */
object OverlayWindowGuards {
    fun extraFlags(secure: Boolean, untouchable: Boolean): Int =
        (if (secure) WindowManager.LayoutParams.FLAG_SECURE else 0) or
            (if (untouchable) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
}

/**
 * The lock screen's face-unlock window (Rule_Book 8.2). When the screen turns on locked, the island holds
 * IDLE and stops intercepting touches for [WINDOW_MS] or until unlock, whichever is first, so it never
 * covers the camera indicator while the face scan runs. It is bounded on purpose: holding IDLE for the
 * whole lock screen would hide lock-screen music, timers and calls.
 */
class BiometricClearance(
    private val scope: CoroutineScope,
    private val windowMs: Long = WINDOW_MS,
    private val onChange: (Boolean) -> Unit
) {
    var active: Boolean = false
        private set
    private var job: Job? = null

    fun onScreenOn(locked: Boolean) {
        if (!locked) {
            end()
            return
        }
        job?.cancel()
        set(true)
        job = scope.launch {
            delay(windowMs)
            set(false)
        }
    }

    fun onUnlocked() = end()

    fun onScreenOff() = end()

    private fun end() {
        job?.cancel()
        job = null
        set(false)
    }

    private fun set(value: Boolean) {
        if (active == value) return
        active = value
        onChange(value)
    }

    companion object {
        /** Long enough for a face scan right after wake; a failed scan falls back to the normal lock screen. */
        const val WINDOW_MS = 5_000L
    }
}

/**
 * Owns the overlay's system-integration guards so the service only needs a few call sites:
 * screen-capture protection, immersive suppression, face-unlock clearance and the foreground secure-app
 * guess. [onWindowFlagsChanged] asks the service to re-apply its window params, which read
 * [extraWindowFlags]. [onForegroundSecure] is where the secure-app guess leaves this class.
 */
class OverlayGuardController(
    context: Context,
    windowManager: WindowManager,
    private val viewModel: IslandViewModel,
    private val notificationRepository: INotificationRepository,
    private val scope: CoroutineScope,
    private val onWindowFlagsChanged: () -> Unit,
    private val onForegroundSecure: (Boolean) -> Unit
) {
    val screenCapture = ScreenCaptureMonitor(context, windowManager)
    val immersive = ImmersiveModeMonitor(context, windowManager)
    val foregroundSecure = ForegroundSecureDetector()
    val biometric = BiometricClearance(scope) { active ->
        viewModel.setBiometricClearance(active)
        onWindowFlagsChanged()
    }

    private val jobs = ArrayList<Job>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        screenCapture.start()
        immersive.start()
        jobs += scope.launch {
            notificationRepository.notifications
                .map { list -> list.any { it.mode == IslandMode.ScreenRecording } }
                .distinctUntilChanged()
                .collect { screenCapture.setRecorderAlertActive(it) }
        }
        jobs += scope.launch {
            screenCapture.isCapturing.drop(1).collect { onWindowFlagsChanged() }
        }
        jobs += scope.launch {
            immersive.isImmersive.collect { viewModel.setImmersive(it) }
        }
        jobs += scope.launch {
            viewModel.isSuppressed.drop(1).collect { suppressed ->
                if (suppressed) viewModel.collapse()
                onWindowFlagsChanged()
            }
        }
        jobs += scope.launch {
            foregroundSecure.foregroundSecure.drop(1).collect { onForegroundSecure(it) }
        }
        // A capture that was already running when the session started.
        if (screenCapture.isCapturing.value) onWindowFlagsChanged()
    }

    fun stop() {
        if (!started) return
        started = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        screenCapture.stop()
        immersive.stop()
        viewModel.setImmersive(false)
        biometric.onScreenOff()
    }

    fun extraWindowFlags(): Int = OverlayWindowGuards.extraFlags(
        secure = screenCapture.isCapturing.value,
        untouchable = viewModel.isSuppressed.value || biometric.active
    )

    fun onForegroundPackage(packageName: String?) = foregroundSecure.onForegroundPackage(packageName)
}
