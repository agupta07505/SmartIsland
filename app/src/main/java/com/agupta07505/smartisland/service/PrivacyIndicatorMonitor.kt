/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.util.PrivacyIndicatorAlert
import com.agupta07505.smartisland.util.PrivacySensor
import com.agupta07505.smartisland.util.PrivacyUsage
import com.agupta07505.smartisland.util.runCatchingLogged

/**
 * Feeds the camera / microphone privacy indicator (Rule_Book §4.15, §4.16) into the repository as one merged
 * alert ([PrivacyIndicatorAlert]).
 *
 * Sources, best first:
 *  1. [Source.AppOps]: `AppOpsManager.startWatchingActive(OPSTR_CAMERA, OPSTR_RECORD_AUDIO)` (Android 11+). It
 *     names the app, but reports other apps only to a holder of `WATCH_APPOPS`, a signature|privileged
 *     permission a store-installed app is never granted (Shizuku's shell uid cannot grant it either:
 *     `pm grant` only grants runtime and development permissions). Used when a privileged / system install
 *     holds it.
 *  2. [Source.Fallback]: `CameraManager.AvailabilityCallback` (a camera became unavailable: some app opened it)
 *     and `AudioManager.AudioRecordingCallback` (an app is recording). Both work without permissions but do
 *     not say who: the camera is attributed to the foreground app (only a visible app may open the camera),
 *     the microphone to "An app". Camera events are ignored while the keyguard is locked, so face unlock
 *     does not light the indicator.
 */
class PrivacyIndicatorMonitor(
    private val context: Context,
    private val repository: INotificationRepository,
    private val foregroundPackage: () -> String?,
    private val isLocked: () -> Boolean
) {
    enum class Source { None, AppOps, Fallback }

    var source: Source = Source.None
        private set

    private val ownPackage = context.packageName
    private val handler = Handler(Looper.getMainLooper())
    private var usage = PrivacyUsage()
    private var startedAtMs: Long? = null

    private var appOpsListener: AppOpsManager.OnOpActiveChangedListener? = null
    private var cameraCallback: CameraManager.AvailabilityCallback? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private val unavailableCameras = HashSet<String>()

    fun start() {
        if (source != Source.None) return
        source = if (canWatchOtherApps() && startAppOps()) Source.AppOps else {
            startFallback()
            Source.Fallback
        }
        android.util.Log.d(TAG, "Privacy indicator source: $source")
    }

    fun stop() {
        if (source == Source.None) return
        runCatchingLogged(TAG, "Stopping app-op watch failed") {
            val listener = appOpsListener
            if (listener != null) context.getSystemService(AppOpsManager::class.java)?.stopWatchingActive(listener)
        }
        runCatchingLogged(TAG, "Unregistering camera callback failed") {
            cameraCallback?.let { context.getSystemService(CameraManager::class.java)?.unregisterAvailabilityCallback(it) }
        }
        runCatchingLogged(TAG, "Unregistering recording callback failed") {
            recordingCallback?.let { context.getSystemService(AudioManager::class.java)?.unregisterAudioRecordingCallback(it) }
        }
        appOpsListener = null
        cameraCallback = null
        recordingCallback = null
        unavailableCameras.clear()
        source = Source.None
        usage = PrivacyUsage()
        publish()
    }

    private fun canWatchOtherApps(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            context.checkSelfPermission(WATCH_APPOPS) == PackageManager.PERMISSION_GRANTED

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startAppOps(): Boolean = runCatchingLogged(TAG, "startWatchingActive failed") {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return@runCatchingLogged false
        val listener = AppOpsManager.OnOpActiveChangedListener { op, _, packageName, active ->
            val sensor = when (op) {
                AppOpsManager.OPSTR_CAMERA -> PrivacySensor.Camera
                AppOpsManager.OPSTR_RECORD_AUDIO -> PrivacySensor.Microphone
                else -> return@OnOpActiveChangedListener
            }
            if (packageName in IGNORED_PACKAGES) return@OnOpActiveChangedListener
            update(usage.with(sensor, packageName, active))
        }
        appOps.startWatchingActive(
            arrayOf(AppOpsManager.OPSTR_CAMERA, AppOpsManager.OPSTR_RECORD_AUDIO),
            context.mainExecutor,
            listener
        )
        appOpsListener = listener
        true
    } == true

    private fun startFallback() {
        runCatchingLogged(TAG, "Camera availability callback failed") {
            val camera = context.getSystemService(CameraManager::class.java) ?: return@runCatchingLogged
            val callback = object : CameraManager.AvailabilityCallback() {
                override fun onCameraUnavailable(cameraId: String) {
                    if (isLocked()) return
                    unavailableCameras += cameraId
                    onCameraStateChanged()
                }

                override fun onCameraAvailable(cameraId: String) {
                    if (unavailableCameras.remove(cameraId)) onCameraStateChanged()
                }
            }
            camera.registerAvailabilityCallback(callback, handler)
            cameraCallback = callback
        }
        runCatchingLogged(TAG, "Recording callback failed") {
            val audio = context.getSystemService(AudioManager::class.java) ?: return@runCatchingLogged
            val callback = object : AudioManager.AudioRecordingCallback() {
                override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                    onRecordingChanged(configs.orEmpty())
                }
            }
            audio.registerAudioRecordingCallback(callback, handler)
            recordingCallback = callback
            onRecordingChanged(audio.activeRecordingConfigurations)
        }
    }

    private fun onCameraStateChanged() {
        val inUse = unavailableCameras.isNotEmpty()
        val user = if (inUse) {
            usage.camera.firstOrNull() ?: foregroundPackage()?.takeIf { it != ownPackage } ?: PrivacyUsage.UNKNOWN_USER
        } else null
        update(usage.copy(camera = user?.let(::setOf).orEmpty()))
    }

    private fun onRecordingChanged(configs: List<AudioRecordingConfiguration>) {
        val recording = configs.any { config ->
            val silenced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && config.isClientSilenced
            !silenced && config.clientAudioSource != AUDIO_SOURCE_HOTWORD
        }
        update(usage.copy(microphone = if (recording) setOf(PrivacyUsage.UNKNOWN_USER) else emptySet()))
    }

    private fun update(next: PrivacyUsage) {
        if (next == usage) return
        usage = next
        publish()
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        if (usage.isIdle) startedAtMs = null else if (startedAtMs == null) startedAtMs = now
        val alert = PrivacyIndicatorAlert.build(usage, ownPackage, ::labelOf, now, startedAtMs)
        if (alert == null) {
            repository.removeNotification(PrivacyIndicatorAlert.KEY)
        } else {
            repository.postNotification(alert, autoExpand = false)
        }
    }

    private fun labelOf(packageName: String): String = runCatchingLogged(TAG, "Label lookup failed") {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } ?: packageName

    private companion object {
        const val TAG = "PrivacyIndicator"
        const val WATCH_APPOPS = "android.permission.WATCH_APPOPS"

        /** `MediaRecorder.AudioSource.HOTWORD` (hidden): the always-on "Hey Google" listener is not a recording. */
        const val AUDIO_SOURCE_HOTWORD = 1999

        /** The system's own face unlock / audio services, which the OS indicator does not show either. */
        val IGNORED_PACKAGES = setOf("android", "com.android.systemui")
    }
}
