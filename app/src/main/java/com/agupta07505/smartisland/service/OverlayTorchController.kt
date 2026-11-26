/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.ExternalTorchDetector
import com.agupta07505.smartisland.model.ExternalTorchPattern
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.TorchCard
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.TorchController
import com.agupta07505.smartisland.util.TorchPatterns
import com.agupta07505.smartisland.util.UtilityBridge
import com.agupta07505.smartisland.util.postSystemEvent
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Follows the system flashlight for the island: the camera's torch callback, a burst of flips from another app, and
 * the island's own tile. The service owns the registration lifecycle through [start] and [stop]
 */
internal class OverlayTorchController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: SmartIslandSettingsRepository,
    private val notificationRepository: INotificationRepository,
    private val viewModelHolder: OverlayViewModelHolder,
    private val isDestroyed: () -> Boolean,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()

    private var torchCallbackRegistered = false

    fun start() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            TorchController.attach(context, viewModel.displaySettings.value.torchStrobeAcknowledged)
            scope.launch {
                runSuspendCatchingLogged(TAG, "Strobe acknowledgement collector failed") {
                    TorchController.state.map { it.acknowledged }.distinctUntilChanged().collect { acknowledged ->
                        if (acknowledged && !viewModel.displaySettings.value.torchStrobeAcknowledged) {
                            repository.setTorchStrobeAcknowledged(true)
                        }
                    }
                }
            }
            scope.launch {
                runSuspendCatchingLogged(TAG, "Torch event watcher failed") {
                    notificationRepository.notifications
                        .map { list -> list.any { it.key == TORCH_EVENT_KEY } }
                        .distinctUntilChanged()
                        .collect { shown ->
                            if (TorchCard.mustStopPattern(TorchController.state.value.pattern != null, shown)) {
                                TorchController.stopPattern()
                            }
                        }
                }
            }
            scope.launch {
                runSuspendCatchingLogged(TAG, "Torch card close watcher failed") {
                    combine(viewModel.expanded, viewModel.mode, TorchController.state) { open, mode, card ->
                        TorchCard.eventDone(TorchCard.open(mode, open), card.active, card.gateShown)
                    }
                        .distinctUntilChanged()
                        .collect { done -> if (done) notificationRepository.removeNotification(TORCH_EVENT_KEY) }
                }
            }
            // The island froze as soon as another app started strobing. Handle the torch callbacks off the main thread.
            val thread = HandlerThread("SmartIslandTorch").also { it.start() }
            torchThread = thread
            torchHandler = Handler(thread.looper)
            cameraManager?.registerTorchCallback(torchCallback, torchHandler)
            torchCallbackRegistered = true
            // The island's own flashlight tile reports its switch at the tap, the camera's callback comes later
            UtilityBridge.torchToggled = { on -> applySystemTorch(on, busy = false, lone = true) }
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "registerTorchCallback failed", e)
        }
    }

    fun stop() {
        if (!torchCallbackRegistered) return
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "unregisterTorchCallback failed", e)
        }
        torchCallbackRegistered = false
        torchHandler.removeCallbacksAndMessages(null)
        torchThread?.quitSafely()
        torchThread = null
    }

    private var torchThread: HandlerThread? = null
    private var torchHandler: Handler = Handler(Looper.getMainLooper())

    private var torchOffJob: Job? = null
    @Volatile private var cameraBusy = false

    /**
     * Runs on [torchThread]. Everything here up to the hand-over is confined to that thread: the flip clock, the burst
     * counter and the detector. Only [applySystemTorch] and the card's own state touch the main thread
     */
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(cameraId: String) {
            cameraBusy = true
        }

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val busy = cameraBusy
            if (!enabled) cameraBusy = false
            // Every flip counts here, a pattern's too: a lone flip after a quiet spell is a person, not a handover
            val now = SystemClock.uptimeMillis()
            val lone = now - lastCallbackAt >= TORCH_OFF_GRACE_MS
            lastCallbackAt = now
            // A running strobe or SOS flips the torch by itself, that is no event
            if (TorchPatterns.active != null) return
            // The card's own taps are not another app's pattern
            if (TorchController.drivesTorch()) externalDetector.reset() else trackExternalPattern(enabled, busy, now)
            // Reacting to each flip of a burst floods the island with events and freezes the app, so a burst is held
            // back until the torch has been quiet for a moment
            torchFlips = if (now - lastTorchFlipAt < TORCH_BURST_GAP_MS) torchFlips + 1 else 0
            lastTorchFlipAt = now
            torchHandler.removeCallbacks(settleRunnable)
            // A strobe or SOS already recognised is held back the same way, however slow: at 1 or 2 flashes a second the
            // flips are too far apart to look like a burst, yet each one would re-post and close the torch event
            if (torchFlips >= TORCH_BURST_FLIPS || externalPattern != null) {
                settleEnabled = enabled
                settleBusy = busy
                torchHandler.postDelayed(settleRunnable, TORCH_SETTLE_MS)
                return
            }
            scope.launch { applySystemTorch(enabled, busy, lone) }
        }
    }

    private var lastCallbackAt = 0L

    private val externalDetector = ExternalTorchDetector()
    /** Written on the torch thread, read on the main thread by [applySystemTorch]. */
    @Volatile private var externalPattern: ExternalTorchPattern? = null

    private val idleRunnable = Runnable { clearExternalPattern() }

    private var settleEnabled = false
    private var settleBusy = false
    private val settleRunnable = Runnable {
        torchFlips = 0
        val enabled = settleEnabled
        val busy = settleBusy
        scope.launch { applySystemTorch(enabled, busy) }
    }

    /** Reads each flip for a strobe or SOS from another app, the card marks its Strobe or SOS button while 1 runs. */
    private fun trackExternalPattern(enabled: Boolean, busy: Boolean, at: Long) {
        if (!TorchCard.followsSystemTorch(busy, callRinging())) {
            clearExternalPattern()
            return
        }
        val found = externalDetector.onFlip(enabled, at)
        if (found != null) TorchController.setExternalLit(enabled)
        if (found != externalPattern) {
            externalPattern = found
            TorchController.setExternal(found)
        }
        torchHandler.removeCallbacks(idleRunnable)
        if (found != null) torchHandler.postDelayed(idleRunnable, ExternalTorchDetector.idleMs(found))
    }

    private fun clearExternalPattern() {
        torchHandler.removeCallbacks(idleRunnable)
        externalDetector.reset()
        if (externalPattern != null) {
            externalPattern = null
            TorchController.setExternal(null)
            TorchController.setExternalLit(false)
        }
    }

    private var torchFlips = 0
    private var lastTorchFlipAt = 0L

    /** [lone]: the flip came after a quiet spell, so it is no gap of a strobe or a handover and needs no grace. */
    private fun applySystemTorch(enabled: Boolean, busy: Boolean, lone: Boolean = false) {
        if (isDestroyed() || !viewModelHolder.ready) return
        if (TorchPatterns.active != null) return
        if (enabled && !TorchCard.followsSystemTorch(busy, callRinging())) return
        torchOffJob?.cancel()
        if (enabled) {
            TorchController.onSystemTorch(true)
            // The island's own tile already posted it ahead of the camera's answer: posting again would only count
            // as a second arrival for the anti-spam check
            if (notificationRepository.notifications.value.any { it.key == TORCH_EVENT_KEY }) return
            notificationRepository.postSystemEvent(
                IslandNotification(
                    key = TORCH_EVENT_KEY,
                    packageName = "com.android.systemui",
                    appName = "Flashlight",
                    title = "Flashlight ON",
                    text = "Tap to turn off",
                    mode = IslandMode.Flashlight,
                    timeMillis = System.currentTimeMillis(),
                    actionIntents = listOf(
                        IslandNotificationAction("Turn Off", null)
                    )
                ),
                IslandEvent.FLASHLIGHT.id,
                viewModel.displaySettings.value
            )
        } else {
            // The torch passes through off when a pattern hands over to a steady light, that must not close the event
            val settleOff = {
                TorchController.onSystemTorch(false)
                if (TorchCard.endsEventWhenDark(viewModel.torchCardOpenNow())) {
                    notificationRepository.removeNotification(TORCH_EVENT_KEY)
                }
            }
            // A plain switch-off (the card, the tile, the shade) shows at once. Only a flip right behind another one
            // waits, as the torch passes through off when a pattern hands over and between a strobe's flashes
            if (lone && externalPattern == null) {
                settleOff()
            } else {
                torchOffJob = scope.launch {
                    delay(TORCH_OFF_GRACE_MS)
                    // Another app's strobe or SOS has dark gaps longer than the grace: the torch is still in use then
                    if (externalPattern != null) return@launch
                    settleOff()
                }
            }
        }
    }

    /** The ring's flash alert blinks the torch while a call rings; the ringtone audio mode covers silent ringing too. */
    private fun callRinging(): Boolean =
        notificationRepository.notifications.value.any { it.isCallRinging } ||
            (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.mode == AudioManager.MODE_RINGTONE

    private companion object {
        const val TAG = "SmartIslandOverlayService"
        const val TORCH_OFF_GRACE_MS = 400L
        /** Flips closer together than this belong to 1 burst, as from another app's strobe. */
        const val TORCH_BURST_GAP_MS = 250L
        /** Flips in a row inside a burst before the island stops following each one. */
        const val TORCH_BURST_FLIPS = 3
        /** How long the torch must stay quiet before the island takes up where a burst left it. */
        const val TORCH_SETTLE_MS = 500L
        const val TORCH_EVENT_KEY = "system_flashlight"
    }
}
