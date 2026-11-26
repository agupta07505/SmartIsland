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
import android.os.SystemClock
import com.agupta07505.smartisland.model.ExternalTorchPattern
import com.agupta07505.smartisland.model.TorchCardState
import com.agupta07505.smartisland.model.TorchOutput
import com.agupta07505.smartisland.model.TorchPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * The torch card's state and the one place that drives the torch from it. The card taps and drags call the functions
 * below; each one moves [state] on and the new [TorchOutput] reaches the hardware in order, the newest request winning
 * when they come faster than the camera answers.
 */
object TorchController {
    private const val TAG = "TorchController"

    /** The camera reports each switch a moment after the card asked for it, that report is the card's own echo. */
    private const val ECHO_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val current = MutableStateFlow(TorchCardState())
    private val requests = Channel<TorchOutput>(Channel.CONFLATED)
    private var appContext: Context? = null

    @Volatile private var driven: TorchOutput = TorchOutput.Off

    @Volatile private var driving = false
    @Volatile private var ownDriveUntil = 0L
    @Volatile private var echo: Boolean? = null
    @Volatile private var reconcileJob: Job? = null

    /** Whether the card is switching the torch now or just did, so the camera's reports are its own echoes. */
    fun drivesTorch(): Boolean = driving || SystemClock.uptimeMillis() < ownDriveUntil

    val state: StateFlow<TorchCardState> = current.asStateFlow()

    private val fingerDown = MutableStateFlow(false)
    private val externalLitFlow = MutableStateFlow(false)

    /** Whether the torch is lit right now while another app runs a strobe or SOS, for the beam to flash with it. */
    val externalLit: StateFlow<Boolean> = externalLitFlow.asStateFlow()

    /** Another app's strobe or SOS started or ended. The card follows, nothing is sent to the torch. */
    fun setExternal(pattern: ExternalTorchPattern?) {
        current.update { it.withExternal(pattern) }
    }

    fun setExternalLit(lit: Boolean) {
        externalLitFlow.value = lit
    }

    /** A finger is on the card or the strobe warning is up: an auto-opened card waits while this is true. */
    val holding: StateFlow<Boolean> = combine(fingerDown, current) { down, card -> down || card.gateShown }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun setFingerDown(down: Boolean) {
        fingerDown.value = down
    }

    /** Reads the phone's torch once and starts following it. Later calls only refresh the acknowledgement. */
    @Synchronized
    fun attach(context: Context, acknowledged: Boolean) {
        if (appContext != null) {
            current.update { it.copy(acknowledged = acknowledged) }
            return
        }
        val app = context.applicationContext
        appContext = app
        val info = TorchCapabilities.detect(app)
        val level = TorchCapabilities.currentLevel(app) ?: info?.defaultLevel ?: 1
        val first = TorchCardState.initial(info?.maxLevel ?: 1, level, UtilityActions.torchOn, acknowledged)
        current.value = first
        driven = first.output
        scope.launch {
            runSuspendCatchingLogged(TAG, "Torch output loop failed") {
                for (request in requests) drive(app, request)
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Torch pattern watcher failed") {
                TorchPatterns.activeState.collect {
                    // Only a pattern that was started and has since died on its own (the camera was taken) is news,
                    // so the live value is read, a stale "stopped" from an earlier tap must not clear a new pattern
                    if (TorchPatterns.active == null && driven.isPattern) current.update { it.patternEnded() }
                }
            }
        }
    }

    fun powerTap() = change { it.powerTap() }
    fun sosTap() = change { it.sosTap() }
    fun strobeTap() = change { it.strobeTap() }
    fun back() = change { it.back() }
    fun gateCancel() = change { it.gateCancel() }
    fun gateAccept() = change { it.gateAccept() }
    fun setHz(hz: Int) = change { it.withHz(hz) }
    fun setLevel(level: Int) = change { it.withLevel(level) }

    /** The torch changed from outside the card: the card follows, the torch is already doing it. */
    fun onSystemTorch(enabled: Boolean) {
        if (drivesTorch()) {
            // An echo of the card's own switch would put an older state back over a newer tap: the last one is
            // applied once the card has been quiet
            echo = enabled
            reconcileAfterDrive()
            return
        }
        val next = current.updateAndGet { it.systemTorch(enabled) }
        if (!next.output.isPattern) driven = next.output
    }

    @Synchronized
    private fun reconcileAfterDrive() {
        reconcileJob?.cancel()
        reconcileJob = scope.launch {
            while (drivesTorch()) delay(ECHO_MS / 2)
            val last = echo ?: return@launch
            echo = null
            onSystemTorch(last)
        }
    }

    /** The strobe or SOS ends and the torch goes dark, the card goes back to Off. Nothing happens when none runs. */
    fun stopPattern() {
        if (current.value.pattern != null) change { it.patternEnded() }
    }

    /** The card was swapped for another: a running strobe or SOS ends and the torch stays lit. */
    fun leaveCard() {
        val now = current.value
        if (now.pattern != null || now.gateShown) change { it.leftForAnotherCard() }
    }

    private fun change(transform: (TorchCardState) -> TorchCardState) {
        var before: TorchOutput = TorchOutput.Off
        val next = current.updateAndGet { before = it.output; transform(it) }
        if (next.output != before) ownDriveUntil = SystemClock.uptimeMillis() + ECHO_MS
        requests.trySend(next.output)
    }

    private suspend fun drive(context: Context, output: TorchOutput) {
        if (output == driven) return
        driving = true
        try {
            // Leaving a pattern counts as done before it stops, so its stop is not taken for a pattern that died
            if (!output.isPattern) driven = output
            runSuspendCatchingLogged(TAG, "Could not set the torch") {
                when (output) {
                    TorchOutput.Off -> {
                        TorchPatterns.stop()
                        setSteady(context, null)
                    }
                    is TorchOutput.Steady -> {
                        TorchPatterns.stop()
                        setSteady(context, output.level)
                    }
                    TorchOutput.Sos -> TorchPatterns.start(context, TorchPattern.Sos)
                    is TorchOutput.Strobe ->
                        if (TorchPatterns.active == TorchPattern.Strobe) TorchPatterns.setStrobeHz(output.hz)
                        else TorchPatterns.start(context, TorchPattern.Strobe, output.hz)
                }
            }
            driven = output
            ownDriveUntil = SystemClock.uptimeMillis() + ECHO_MS
        } finally {
            driving = false
        }
    }

    /** A null [level] switches the torch off. Phones with brightness steps light it at the asked step. */
    private fun setSteady(context: Context, level: Int?) {
        val info = TorchCapabilities.detect(context) ?: return
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        when {
            level == null -> cm.setTorchMode(info.cameraId, false)
            info.supportsBrightness && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                cm.turnOnTorchWithStrengthLevel(info.cameraId, level.coerceIn(1, info.maxLevel))
            else -> cm.setTorchMode(info.cameraId, true)
        }
    }

    private val TorchOutput.isPattern: Boolean
        get() = this is TorchOutput.Sos || this is TorchOutput.Strobe
}
