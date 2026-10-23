/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Watches the system audio mode. VoIP apps switch it to MODE_IN_COMMUNICATION for the whole call, which needs no
 * permission.
 */
class VoipCallMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var modeListener: Any? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private var pollJob: Job? = null
    private var endJob: Job? = null
    private var lastLoggedMode: Int? = null

    fun start() {
        val am = audioManager ?: run {
            AppLogRecorder.w(TAG, "No AudioManager: VoIP detection by audio mode is unavailable")
            return
        }
        AppLogRecorder.i(TAG, "VoIP monitor started (sdk=${Build.VERSION.SDK_INT}, ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "mode listener" else "polling"})")
        if (modeListener != null || pollJob != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { mode -> handleMode(mode) }
            modeListener = listener
            runCatchingLogged(TAG, "addOnModeChangedListener failed") {
                am.addOnModeChangedListener(ContextCompat.getMainExecutor(context), listener)
            }
        } else {
            pollJob = scope.launch {
                while (true) {
                    handleMode(am.mode)
                    delay(POLL_MS)
                }
            }
        }

        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                val capture = configs.orEmpty().any {
                    it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION
                }
                if (capture != VoipCallSignal.voiceCapture) {
                    AppLogRecorder.i(
                        TAG,
                        "Voice capture (VOICE_COMMUNICATION) ${VoipCallSignal.voiceCapture} -> $capture" +
                            " activeRecordings=${configs.orEmpty().map { it.clientAudioSource }} callSignalActive=${VoipCallSignal.active}"
                    )
                    VoipCallSignal.voiceCapture = capture
                    if (VoipCallSignal.active) onChanged()
                }
            }
        }
        recordingCallback = callback
        runCatchingLogged(TAG, "registerAudioRecordingCallback failed") {
            am.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
        }

        handleMode(am.mode)
    }

    fun stop() {
        val am = audioManager
        endJob?.cancel()
        endJob = null
        pollJob?.cancel()
        pollJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { l ->
                runCatchingLogged(TAG, "removeOnModeChangedListener failed") { am?.removeOnModeChangedListener(l) }
            }
        }
        modeListener = null
        recordingCallback?.let { cb ->
            runCatchingLogged(TAG, "unregisterAudioRecordingCallback failed") { am?.unregisterAudioRecordingCallback(cb) }
        }
        recordingCallback = null
        VoipCallSignal.setActive(false)
    }

    private fun handleMode(mode: Int) {
        val inCall = VoipCallDetector.isVoipAudioMode(mode)
        if (mode != lastLoggedMode) {
            lastLoggedMode = mode
            AppLogRecorder.i(
                TAG,
                "Audio mode -> ${EnvironmentDiagnostics.audioModeName(mode)} (voip=$inCall callSignalActive=${VoipCallSignal.active})"
            )
        }
        if (inCall) {
            endJob?.cancel()
            endJob = null
            if (!VoipCallSignal.active) {
                AppLogRecorder.i(TAG, "VoIP call signal ON (audio mode IN_COMMUNICATION)")
                VoipCallSignal.setActive(true)
                onChanged()
            }
        } else if (VoipCallSignal.active && endJob == null) {
            AppLogRecorder.i(TAG, "VoIP audio ended, waiting ${END_DEBOUNCE_MS}ms before clearing the call signal")
            endJob = scope.launch {
                delay(END_DEBOUNCE_MS)
                endJob = null
                if (audioManager?.mode?.let { VoipCallDetector.isVoipAudioMode(it) } != true) {
                    AppLogRecorder.i(TAG, "VoIP call signal OFF")
                    VoipCallSignal.setActive(false)
                    onChanged()
                } else {
                    AppLogRecorder.i(TAG, "VoIP audio came back during the debounce, call signal stays ON")
                }
            }
        }
    }

    private companion object {
        const val TAG = "VoipCallMonitor"
        const val POLL_MS = 1500L
        const val END_DEBOUNCE_MS = 1500L
    }
}
