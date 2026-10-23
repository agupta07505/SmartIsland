/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Diagnostic only, and only while the in-app log records. */
object MainThreadWatchdog {
    private const val CHECK_INTERVAL_MS = 50L
    private const val STALL_MS = 250L
    private const val STACK_FRAMES = 30

    @Volatile private var running = false
    @Volatile private var lastAnswerAt = 0L
    private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (running) return
        // No main looper (plain JVM unit tests with stubbed android.jar): nothing to watch.
        val looper = Looper.getMainLooper() ?: return
        running = true
        lastAnswerAt = SystemClock.uptimeMillis()
        val handler = Handler(looper)
        val main = looper.thread
        thread = Thread({
            var reportedStall = false
            var stallStart = 0L
            while (running) {
                val sentAt = SystemClock.uptimeMillis()
                handler.post { lastAnswerAt = SystemClock.uptimeMillis() }
                try { Thread.sleep(CHECK_INTERVAL_MS) } catch (_: InterruptedException) { return@Thread }
                val now = SystemClock.uptimeMillis()
                val waiting = now - lastAnswerAt
                if (lastAnswerAt < sentAt && waiting >= STALL_MS) {
                    if (!reportedStall) {
                        reportedStall = true
                        stallStart = lastAnswerAt
                        val stack = main.stackTrace.take(STACK_FRAMES).joinToString("\n    at ")
                        AppLogRecorder.w("MainStall", "main thread blocked for ${waiting} ms so far, running:\n    at $stack")
                    }
                } else if (reportedStall && lastAnswerAt >= sentAt) {
                    reportedStall = false
                    AppLogRecorder.w("MainStall", "main thread free again after about ${lastAnswerAt - stallStart} ms")
                }
            }
        }, "SmartIslandMainWatchdog").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }
}
