/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.SmartIslandSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object AppLogRecorder {
    private const val TAG = "SmartIslandLog"
    private const val MAX_IN_MEMORY_LOGS = 3000
    private const val LOG_FILE_NAME = "smartisland_debug_log.txt"
    private const val MAX_FILE_BYTES = 1_000_000L
    private const val ENTRY_SEPARATOR = '\u001E'
    private const val STATE_PREFS = "smartisland_log_state"
    private const val KEY_RECORDING = "recording"

    private val inMemoryLogs = ConcurrentLinkedQueue<String>()
    private val logCount = AtomicInteger(0)

    // Disk copy of the buffer: when the system or an OEM task killer stops the listener, the process
    // and its in-memory buffer die too. One thread keeps the file writes in order.
    private val diskExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "SmartIslandLogWriter").apply { isDaemon = true } }
    @Volatile private var logFile: File? = null

    @Volatile
    var isRecording: Boolean = false
        private set(value) {
            field = value
            if (value) MainThreadWatchdog.start() else MainThreadWatchdog.stop()
        }

    /** Safe to call on every settings emission: it only acts when the value changes. */
    fun applySetting(context: Context, enabled: Boolean) {
        attach(context)
        if (enabled != isRecording) updateRecordingState(context, enabled)
    }

    /** Call early in each process (Application and service onCreate), so a system restart of the app is itself logged. */
    fun restore(context: Context) {
        attach(context)
    }

    fun updateRecordingState(context: Context, enabled: Boolean) {
        attach(context)
        if (enabled) {
            isRecording = true
            saveRecordingFlag(context, true)
            record("I", TAG, "Diagnostic log recording started for Smart Island v${BuildConfig.VERSION_NAME} (pid ${Process.myPid()})")
            record("I", TAG, "Environment: " + runCatching { EnvironmentDiagnostics.snapshot(context) }.getOrDefault("unavailable"))
        } else {
            record("I", TAG, "Diagnostic log recording paused")
            isRecording = false
            saveRecordingFlag(context, false)
        }
    }

    private fun saveRecordingFlag(context: Context, value: Boolean) {
        runCatching {
            context.applicationContext.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_RECORDING, value).apply()
        }
    }

    @Synchronized
    private fun attach(context: Context) {
        if (logFile != null) return
        val file = runCatching { File(context.applicationContext.filesDir, LOG_FILE_NAME) }.getOrNull() ?: return
        logFile = file
        runCatching {
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
                    .split(ENTRY_SEPARATOR)
                    .map { it.trim('\n') }
                    .filter { it.isNotBlank() }
                    .takeLast(MAX_IN_MEMORY_LOGS)
                    .forEach {
                        inMemoryLogs.add(it)
                        logCount.incrementAndGet()
                    }
            }
        }
        val wasRecording = runCatching {
            context.applicationContext.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_RECORDING, false)
        }.getOrDefault(false)
        if (wasRecording) {
            isRecording = true
            record("I", TAG, "Process started (pid ${Process.myPid()}, v${BuildConfig.VERSION_NAME}); recording resumed from the last session. Earlier lines above belong to the previous process.")
        }
    }

    fun record(level: String, tag: String, message: String, tr: Throwable? = null) {
        if (!isRecording) return

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val threadName = Thread.currentThread().name
        val entry = buildString {
            append("[$timestamp] [Thread: $threadName] [$level/$tag] $message")
            if (tr != null) {
                append("\n").append(Log.getStackTraceString(tr))
            }
        }

        inMemoryLogs.add(entry)
        logCount.incrementAndGet()
        // Re-read the counter on every pass: a stale value here would spin forever once full.
        while (logCount.get() > MAX_IN_MEMORY_LOGS) {
            if (inMemoryLogs.poll() == null) break
            logCount.decrementAndGet()
        }
        appendToDisk(entry)
    }

    private fun appendToDisk(entry: String) {
        val file = logFile ?: return
        runCatching {
            diskExecutor.execute {
                runCatching {
                    if (file.exists() && file.length() > MAX_FILE_BYTES) {
                        // Keep the newest half of the file.
                        val text = file.readText(Charsets.UTF_8)
                        val cut = text.indexOf(ENTRY_SEPARATOR, text.length / 2)
                        file.writeText(if (cut >= 0) text.substring(cut + 1) else "", Charsets.UTF_8)
                    }
                    FileWriter(file, true).use { it.write(entry + ENTRY_SEPARATOR + "\n") }
                }
            }
        }
    }

    fun d(tag: String, message: String) = record("D", tag, message)
    fun i(tag: String, message: String) = record("I", tag, message)
    fun w(tag: String, message: String, tr: Throwable? = null) = record("W", tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = record("E", tag, message, tr)

    fun getLogCount(): Int = logCount.get()

    /** Writes synchronously because the process is dying, and records even when the recorder is off. */
    fun recordCrash(thread: Thread, error: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] [Thread: ${thread.name}] [E/CRASH] Uncaught exception, process is dying\n" +
            Log.getStackTraceString(error)
        inMemoryLogs.add(entry)
        logCount.incrementAndGet()
        val file = logFile ?: return
        runCatching { FileWriter(file, true).use { it.write(entry + ENTRY_SEPARATOR + "\n") } }
    }

    internal fun startRecordingForTest() {
        resetForTest()
        isRecording = true
    }

    internal fun resetForTest() {
        isRecording = false
        inMemoryLogs.clear()
        logCount.set(0)
    }

    internal fun lastEntryForTest(): String? = inMemoryLogs.lastOrNull()

    fun clear(context: Context) {
        attach(context)
        inMemoryLogs.clear()
        logCount.set(0)
        val file = logFile
        runCatching { diskExecutor.execute { runCatching { if (file != null && file.exists()) file.delete() } } }
        record("I", TAG, "Log buffer cleared by user")
    }

    fun getProcessLogcat(): String {
        return runCatching {
            val pid = Process.myPid()
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "--pid=$pid"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
                lineCount++
                if (lineCount >= 4000) break
            }
            reader.close()
            process.destroy()
            if (sb.isEmpty()) "No process logcat entries captured." else sb.toString()
        }.getOrElse { "Failed to read process logcat: ${it.localizedMessage}" }
    }

    suspend fun generateDiagnosticReport(context: Context, settings: SmartIslandSettings): String = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        val displayMetrics = context.resources.displayMetrics
        val overlayAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        val shizukuActive: Boolean = runCatching {
            ShizukuManager.isBinderAvailable() && ShizukuManager.hasPermission()
        }.getOrDefault(false)

        buildString {
            appendLine("================================================================")
            appendLine(" SMART ISLAND DIAGNOSTIC LOG REPORT")
            appendLine(" Generated at: $now")
            appendLine("================================================================")
            appendLine()
            appendLine("--- 1. APP & ENVIRONMENT ---")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (Code: ${BuildConfig.VERSION_CODE})")
            appendLine("Build Type: ${BuildConfig.BUILD_TYPE}")
            appendLine("Package Name: ${context.packageName}")
            appendLine("Target SDK: ${context.applicationInfo.targetSdkVersion}")
            appendLine("Process PID: ${Process.myPid()}")
            appendLine()
            appendLine("--- 2. DEVICE & SYSTEM ---")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Hardware: ${Build.HARDWARE}")
            appendLine("Android Release: ${Build.VERSION.RELEASE} (API Level: ${Build.VERSION.SDK_INT})")
            appendLine("Security Patch: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"}")
            appendLine("Display Metrics: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels} @ ${displayMetrics.densityDpi}dpi (density=${displayMetrics.density})")
            appendLine("OEM Protection Rules: ${OemDeviceRules.detectCurrentDevice()}")
            appendLine()
            appendLine("--- 3. PERMISSION & SERVICE STATUS ---")
            appendLine("Can Draw Overlays: $overlayAllowed")
            appendLine("Shizuku Authorized: $shizukuActive")
            appendLine("Notification Listener Enabled: ${isNotificationListenerEnabled(context)}")
            appendLine("App Usage Access: ${AppShortcutProvider.hasUsageAccess(context)}")
            appendLine("Environment now: ${runCatching { EnvironmentDiagnostics.snapshot(context) }.getOrDefault("unavailable")}")
            appendLine()
            appendLine("--- 4. SMART ISLAND SETTINGS SNAPSHOT ---")
            appendLine(settings.toJson(appVersion = BuildConfig.VERSION_NAME))
            appendLine()
            appendLine("--- 5. IN-APP RECORDED LOG BUFFER (${inMemoryLogs.size} ENTRIES) ---")
            if (inMemoryLogs.isEmpty()) {
                appendLine("(No in-app recording entries. Ensure 'Record Logs' was enabled while reproducing the issue.)")
            } else {
                inMemoryLogs.forEach { entry ->
                    appendLine(entry)
                }
            }
            appendLine()
            appendLine("--- 6. PROCESS LOGCAT CAPTURE ---")
            appendLine(getProcessLogcat())
            appendLine()
            appendLine("================================================================")
            appendLine(" END OF REPORT")
            appendLine("================================================================")
        }
    }

    suspend fun exportLogsToUri(
        context: Context,
        uri: Uri,
        settings: SmartIslandSettings
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val report = generateDiagnosticReport(context, settings)
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                outStream.write(report.toByteArray(Charsets.UTF_8))
                outStream.flush()
            } ?: throw IllegalStateException("Unable to open output stream for selected storage destination")
        }
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        return runCatching {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabled?.split(":")?.any {
                android.content.ComponentName.unflattenFromString(it)?.packageName == context.packageName
            } == true
        }.getOrDefault(false)
    }
}
