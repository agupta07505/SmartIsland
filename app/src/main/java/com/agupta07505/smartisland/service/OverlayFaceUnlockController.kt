/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandEvent
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.FaceUnlockCoordinator
import com.agupta07505.smartisland.util.FaceUnlockLog
import com.agupta07505.smartisland.util.FaceUnlockLogEvent
import com.agupta07505.smartisland.util.FaceUnlockMode
import com.agupta07505.smartisland.util.FaceUnlockWatcher
import com.agupta07505.smartisland.util.MiuiAppLockEvent
import com.agupta07505.smartisland.util.ShizukuManager
import com.agupta07505.smartisland.util.UnlockType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wires the Face ID scan to the island: the output the coordinator draws through, the system-log watcher and the
 * screen events the service forwards. The service owns the receiver registration and calls [start] and [stop]
 */
internal class OverlayFaceUnlockController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val viewModelHolder: OverlayViewModelHolder,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()

    private var watcher: FaceUnlockWatcher? = null

    // Face ID scan: driven by screen events, the unlock broadcast and the system log
    private val coordinator = FaceUnlockCoordinator(
        object : FaceUnlockCoordinator.Output {
            override fun startScan() = startScan(FaceIdController.REAL_WAIT_MS)
            override fun startScan(waitMs: Int) {
                AppLogRecorder.i("FaceUnlock", "scan started (gives the system up to ${waitMs} ms to answer)")
                FaceIdController.startForUnlock(
                    FaceIdController.styleOf(viewModel.settings.value.faceIdStyle),
                    waitMs
                )
            }
            override fun startAppScan() {
                AppLogRecorder.i("FaceUnlock", "app scan started (app Face ID style)")
                FaceIdController.startForApp(
                    FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle)
                )
            }
            override fun startAppScanEarly() {
                AppLogRecorder.i("FaceUnlock", "app scan started early (blinking waits for the real scan)")
                FaceIdController.startForApp(
                    FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle),
                    anchored = false
                )
            }
            override fun anchor() = FaceIdController.anchor()
            override fun accept() {
                AppLogRecorder.i("FaceUnlock", "face accepted: tick plays")
                FaceIdController.accept()
            }
            override fun reject() {
                AppLogRecorder.i("FaceUnlock", "scan ended without a match: no-face ending plays")
                FaceIdController.reject()
            }
            override fun fingerprint() {
                AppLogRecorder.i("FaceUnlock", "fingerprint unlock: fingerprint animation plays")
                FaceIdController.fingerprint()
            }
            override fun startFingerprintScan() {
                AppLogRecorder.i("FaceUnlock", "fingerprint unlock from the dark screen: fingerprint animation plays")
                FaceIdController.startFingerprint(
                    FaceIdController.styleOf(viewModel.settings.value.faceIdStyle)
                )
            }
            override fun startAppFingerprintScan() {
                AppLogRecorder.i("FaceUnlock", "fingerprint unlock in an app prompt: fingerprint animation plays")
                FaceIdController.startFingerprint(
                    FaceIdController.styleOf(viewModel.settings.value.appFaceIdStyle)
                )
            }
            override fun cancel() {
                AppLogRecorder.i("FaceUnlock", "scan cancelled")
                FaceIdController.cancel()
            }
            override fun stop() {
                AppLogRecorder.i("FaceUnlock", "scan stopped")
                FaceIdController.stop()
            }

            override fun isScanning(): Boolean = FaceIdController.state.value.active
            override fun later(delayMs: Long, block: () -> Unit) {
                scope.launch {
                    delay(delayMs)
                    block()
                }
            }
            override fun note(text: String) {
                AppLogRecorder.i("FaceUnlock", text)
            }
        }
    )

    private fun handleMiuiAppLock(event: MiuiAppLockEvent) {
        val now = SystemClock.elapsedRealtime()
        when (event) {
            MiuiAppLockEvent.PromptShown,
            MiuiAppLockEvent.ScanStart -> {
                syncStateFromSystem()
                if (event == MiuiAppLockEvent.PromptShown) {
                    coordinator.onAppLockRequired()
                } else {
                    coordinator.onLog(
                        FaceUnlockLogEvent.Start,
                        now,
                        strong = true,
                        fromApp = true
                    )
                }
            }
            MiuiAppLockEvent.FaceMatched -> coordinator.onAppUnlocked(now)
            MiuiAppLockEvent.FingerprintMatched ->
                coordinator.onUnlockType(UnlockType.Fingerprint, now)
        }
    }

    fun configure(effective: SmartIslandSettings) {
        val on = effective.enabled &&
            effective.isEventEnabled(IslandEvent.FACE_UNLOCK.id)
        val shizuku = ShizukuManager.hasPermission()
        val logAvailable = on && shizuku
        // App scans show up only in the system log, so they need Shizuku
        val appOn = effective.enabled && effective.appFaceIdEnabled && shizuku
        coordinator.configure(
            enabled = on,
            mode = FaceUnlockMode.fromId(effective.faceUnlockMode),
            logAvailable = logAvailable,
            appEnabled = appOn,
            fingerprintEnabled = effective.fingerprintAnimationEnabled
        )
        val current = watcher
        if ((logAvailable || appOn) && current != null) current.start() else current?.stop()
        val summary = "lockScreen=$on appFaceId=${effective.appFaceIdEnabled} fingerprint=${effective.fingerprintAnimationEnabled} shizuku=$shizuku appOn=$appOn " +
            "watcherRunning=${current?.running == true} mode=${effective.faceUnlockMode}"
        if (summary != lastConfig) {
            lastConfig = summary
            AppLogRecorder.i("FaceUnlock", "config: $summary")
        }
    }

    private var lastConfig: String? = null

    fun start(locked: Boolean) {
        watcher = FaceUnlockWatcher(
            scope,
            onUnlock = { type, _ ->
                scope.launch {
                    syncStateFromSystem()
                    coordinator.onUnlockType(type, SystemClock.elapsedRealtime())
                }
            },
            onBouncer = {
                scope.launch { coordinator.onBouncerShown() }
            },
            onAppLockRequired = {
                // "requireUnlock = true" is also logged when a locked app posts a notification
                AppLogRecorder.i(
                    "FaceUnlock",
                    "app lock line seen: waiting for the real prompt before showing the scanner"
                )
            },
            onPromptDismissed = {
                scope.launch { coordinator.onAppPromptDismissed() }
            },
            onAppUnlocked = {
                scope.launch { coordinator.onAppUnlocked(SystemClock.elapsedRealtime()) }
            },
            onMiuiAppLock = { miuiEvent ->
                scope.launch { handleMiuiAppLock(miuiEvent) }
            }
        ) { event, line ->
            val strong = FaceUnlockLog.isStrongSuccess(line)
            val fromApp = FaceUnlockLog.isAppScanStart(line)
            scope.launch {
                if (fromApp || event == FaceUnlockLogEvent.Start) {
                    // the screen-on broadcast trails the scan: read the real state
                    val (interactive, keyguardLocked) = syncStateFromSystem()
                    if (!fromApp) {
                        AppLogRecorder.i(
                            "FaceUnlock",
                            "face start line: interactive=$interactive locked=$keyguardLocked"
                        )
                    }
                }
                coordinator.onLog(event, SystemClock.elapsedRealtime(), strong, fromApp)
            }
        }
        coordinator.syncState(
            screenOn = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive != false,
            locked = locked
        )
    }

    fun stop() {
        watcher?.stop()
        watcher = null
    }

    fun onScreenOn(locked: Boolean) {
        AppLogRecorder.i("FaceUnlock", "screen-on broadcast received: locked=$locked")
        // Shizuku may have come up since the last check
        configure(viewModel.displaySettings.value)
        coordinator.onScreenOn(locked, SystemClock.elapsedRealtime())
    }

    fun onScreenOff() {
        // HyperOS can deliver this late, after the screen has woken again
        val stillOn = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == true
        if (!stillOn) coordinator.onScreenOff()
    }

    fun onUserPresent() = coordinator.onUserPresent(SystemClock.elapsedRealtime())

    private fun syncStateFromSystem(): Pair<Boolean, Boolean> {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val interactive = power?.isInteractive != false
        val keyguardLocked = keyguard?.isKeyguardLocked == true
        coordinator.syncState(screenOn = interactive, locked = keyguardLocked)
        return interactive to keyguardLocked
    }
}
