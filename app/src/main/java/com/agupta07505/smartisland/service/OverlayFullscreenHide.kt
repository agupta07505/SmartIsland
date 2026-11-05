/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.os.PowerManager
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.agupta07505.smartisland.model.FullscreenRules
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.FullscreenDetector
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hides the island while a fullscreen app (no status bar) is in front, in portrait and in landscape with the edge
 * pill. The island window stays, drawn at zero alpha, and only the camera cutout takes a touch
 */
internal class OverlayFullscreenHide(
    private val service: AccessibilityService,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope,
    private val viewModel: IslandViewModel,
    private val isDestroyed: () -> Boolean,
    private val isLockScreenActive: () -> Boolean,
    private val relayout: () -> Unit
) {
    private var detector: FullscreenDetector? = null

    // a touch on the camera cutout while a fullscreen app hides the island
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownAt = 0L

    fun start() {
        val detector = FullscreenDetector(
            service = service,
            windowManager = windowManager,
            wanted = { !isDestroyed() && viewModel.settingsSnapshot().hideInFullscreen },
            eligible = {
                val landscape = service.resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
                // the notification shade and quick settings are system UI, not a fullscreen app
                !isDestroyed() && !isLockScreenActive() && !viewModel.shadeOpen.value &&
                    (service.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive != false &&
                    FullscreenRules.eligibleLayout(
                        landscape, viewModel.settingsSnapshot().landscapeLayoutStyle
                    )
            },
            onChange = { fullscreen -> viewModel.setFullscreen(fullscreen) }
        )
        this.detector = detector
        detector.start()
        scope.launch {
            runSuspendCatchingLogged(TAG, "Fullscreen hide collector failed") {
                viewModel.fullscreenHidden.collectLatest {
                    if (isDestroyed()) return@collectLatest
                    // the island sinks into the camera or rises out of it on its own (ui/HideSinkModifier.kt), the
                    // touchable region and the window size follow the hidden state
                    relayout()
                }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Fullscreen setting collector failed") {
                // the switch turned off: the island comes back at once, without waiting for the next poll
                viewModel.displaySettings.map { it.hideInFullscreen }.distinctUntilChanged().collect {
                    detector.refresh()
                }
            }
        }
    }

    /** The window list changed, the status bar window comes and goes with it. */
    fun refresh() {
        detector?.refresh()
    }

    /** The window list has no status bar for a moment while the screen turns. */
    fun holdForRotation() {
        detector?.holdForRotation()
    }

    fun stop() {
        detector?.stop()
    }

    /** A short touch on the cutout shows the island for a moment, everything else is swallowed. */
    fun onHiddenTouch(view: View, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.rawX
                touchDownY = event.rawY
                touchDownAt = SystemClock.elapsedRealtime()
            }
            MotionEvent.ACTION_UP -> {
                val slop = ViewConfiguration.get(service).scaledTouchSlop
                val moved = Math.hypot(
                    (event.rawX - touchDownX).toDouble(), (event.rawY - touchDownY).toDouble()
                )
                val quick = SystemClock.elapsedRealtime() - touchDownAt <= TAP_MAX_MS
                if (quick && moved <= slop) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    viewModel.peekFromFullscreenTap()
                }
            }
        }
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
        const val TAP_MAX_MS = 350L
    }
}
