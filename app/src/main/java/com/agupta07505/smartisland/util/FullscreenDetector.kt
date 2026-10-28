/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityWindowInfo
import com.agupta07505.smartisland.model.FullscreenRules
import com.agupta07505.smartisland.model.FullscreenTracker
import com.agupta07505.smartisland.model.SystemWindowBounds

/**
 * Is the app in front fullscreen (no status bar)? Answered from the accessibility service's window list, a fullscreen
 * app leaves no status bar window at the top (FullscreenRules has the rule and its timings).
 *
 * Polls while [wanted] is true, plus a [refresh] whenever the window list changes. Not [eligible] (a layout the hide
 * does not cover, a locked or dark screen) means it asks the list nothing and reports "not fullscreen". A reading
 * that fails reports nothing new, the island stays as it is
 */
class FullscreenDetector(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
    /** The hide is on in the settings. */
    private val wanted: () -> Boolean,
    /** The layout, lock and screen state allow the hide right now. */
    private val eligible: () -> Boolean,
    private val onChange: (Boolean) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val tracker = FullscreenTracker()
    private var running = false
    private var reported = false
    private var appKey: Int? = null
    private var panelPresent = false

    private val exitCheck = Runnable { refresh() }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            refresh()
            handler.postDelayed(this, if (wanted() && eligible()) FullscreenRules.POLL_MS else IDLE_POLL_MS)
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        handler.removeCallbacks(exitCheck)
        tracker.reset()
        report(false)
    }

    /** A turn of the screen: the window list is not read for a moment, the bar is gone from it while it settles. */
    fun holdForRotation() {
        tracker.hold(SystemClock.elapsedRealtime() + FullscreenRules.ROTATION_HOLD_MS)
    }

    /** Reads once now, also called when the window list changed so the answer does not wait for the next poll. */
    fun refresh() {
        if (!running) return
        if (!wanted() || !eligible()) {
            tracker.reset()
            report(false)
            return
        }
        appKey = null
        panelPresent = false
        val present = runCatchingLogged(TAG, "Status bar could not be read") { readStatusBarPresent() }
        val now = SystemClock.elapsedRealtime()
        report(tracker.update(present, now, appKey, panelPresent))
        // the exit wait ends at a known moment: check then, not at the next poll
        handler.removeCallbacks(exitCheck)
        tracker.exitRemainingMs(now)?.let { handler.postDelayed(exitCheck, it + 20L) }
    }

    private fun report(fullscreen: Boolean) {
        if (reported == fullscreen) return
        reported = fullscreen
        AppLogRecorder.i(TAG, if (fullscreen) "fullscreen app in front" else "status bar back")
        onChange(fullscreen)
    }

    /** True or false from the window list, null when the list is not there to read (the island stays as it is). */
    private fun readStatusBarPresent(): Boolean? {
        val windows = service.windows.orEmpty()
        if (windows.isEmpty()) return null
        val screen = screenBoundsOf(service, windowManager)
        val bounds = Rect()
        val systemWindows = windows.filter { it.type == AccessibilityWindowInfo.TYPE_SYSTEM }.map {
            it.getBoundsInScreen(bounds)
            SystemWindowBounds(bounds.left, bounds.top, bounds.right, bounds.bottom, it.isActive || it.isFocused)
        }
        // the app window in front: the bar back over another one is a real exit, a swipe reveal keeps the same window
        appKey = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused) }?.id
        panelPresent = FullscreenRules.systemPanelPresent(systemWindows, screen.width(), screen.height())
        return FullscreenRules.systemUiPresent(systemWindows, screen.width(), screen.height())
    }

    private companion object {
        const val TAG = "FullscreenDetector"
        const val IDLE_POLL_MS = 2000L
    }
}

/** The screen's bounds: the current window metrics, which are already in the screen's own pixels. */
internal fun screenBoundsOf(service: AccessibilityService, windowManager: WindowManager): Rect =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds
    } else {
        val metrics = service.resources.displayMetrics
        Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }
