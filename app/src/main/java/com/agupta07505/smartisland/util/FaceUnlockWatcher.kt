/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class FaceUnlockWatcher(
    private val scope: CoroutineScope,
    private val onUnlock: (UnlockType, String) -> Unit = { _, _ -> },
    private val onBouncer: (String) -> Unit = {},
    private val onAppUnlocked: (String) -> Unit = {},
    private val onAppLockRequired: () -> Unit = {},
    private val onPromptDismissed: () -> Unit = {},
    private val onMiuiAppLock: (MiuiAppLockEvent) -> Unit = {},
    private val onEvent: (FaceUnlockLogEvent, String) -> Unit
) {
    private var job: Job? = null
    private val miuiAppLock = MiuiAppLockLog()
    @Volatile private var process: Process? = null

    val running: Boolean get() = job?.isActive == true

    fun start() {
        if (running) return
        job = scope.launch(Dispatchers.IO) {
            var filtered = true
            var pause = RETRY_MIN_MS
            while (isActive) {
                val started = System.currentTimeMillis()
                var lines = 0
                val p = ShizukuManager.openShellStream(command(filtered))
                if (p == null) {
                    delay(RETRY_MAX_MS)
                    continue
                }
                process = p
                try {
                    BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            lines++
                            val event = FaceUnlockLog.classify(line)
                            if (event != null) {
                                val app = if (FaceUnlockLog.isAppScanStart(line)) " (app prompt, owner ${FaceUnlockLog.clientOwner(line)})" else ""
                                AppLogRecorder.d(TAG, "face $event$app: ${line.take(300)}")
                                onEvent(event, line)
                            }
                            if (FaceUnlockLog.isBouncerShown(line)) {
                                AppLogRecorder.d(TAG, "pin screen shown: ${line.take(300)}")
                                onBouncer(line)
                            }
                            if (FaceUnlockLog.isAppPromptDismissed(line)) {
                                AppLogRecorder.d(TAG, "prompt dismissed: ${line.take(300)}")
                                onPromptDismissed()
                            }
                            if (FaceUnlockLog.isAppLockRequired(line)) {
                                AppLogRecorder.d(TAG, "app lock required: ${line.take(300)}")
                                onAppLockRequired()
                            }
                            FaceUnlockLog.appUnlockedPackage(line)?.let { pkg ->
                                AppLogRecorder.d(TAG, "app unlocked $pkg: ${line.take(300)}")
                                onAppUnlocked(pkg)
                            }
                            miuiAppLock.classify(line, System.currentTimeMillis())?.let { miuiEvent ->
                                AppLogRecorder.d(TAG, "miui app lock $miuiEvent")
                                onMiuiAppLock(miuiEvent)
                            }
                            val type = FaceUnlockLog.classifyUnlock(line)
                            if (type != null && UnlockTypeTracker.record(type, System.currentTimeMillis())) {
                                AppLogRecorder.d(TAG, "unlock type ${type.label}: ${line.take(300)}")
                                onUnlock(type, line)
                            }
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    runCatching { p.destroy() }
                    process = null
                }
                if (!isActive) break
                if (filtered && lines == 0 && System.currentTimeMillis() - started < QUICK_END_MS) filtered = false
                pause = if (System.currentTimeMillis() - started > STABLE_MS) RETRY_MIN_MS else minOf(pause * 2, RETRY_MAX_MS)
                delay(pause)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { process?.destroy() }
        process = null
    }

    companion object {
        private const val TAG = "FaceUnlockWatcher"
        private const val QUICK_END_MS = 3000L
        private const val STABLE_MS = 30_000L
        private const val RETRY_MIN_MS = 2000L
        private const val RETRY_MAX_MS = 30_000L

        internal fun command(filtered: Boolean): String =
            "logcat -v brief -b main,system -T 1" +
                if (filtered) " -e '[Ff][Aa][Cc][Ee]|biometricSourceType|onAuthenticated.true|on_device_unlocked|PRIMARY_BOUNCER|unlockPackage|requireUnlock = true|onSystemEvent: 1|onDismissed: 3|unlock keyguard by fp|${MiuiAppLockLog.FILTER}'" else ""
    }
}
