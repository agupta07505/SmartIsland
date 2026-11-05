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
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import com.agupta07505.smartisland.data.LandscapeCameraMemory
import com.agupta07505.smartisland.data.RotationMorph
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.model.RotationVeil
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.LandscapeCameraDetector
import com.agupta07505.smartisland.util.SystemAnimScale
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Display listener, rotation veil, morph window and the settling orientation refresh
 * The service owns the window, so the update comes in as [applyWindowParams]
 */
internal class OverlayRotationController(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val viewModelHolder: OverlayViewModelHolder,
    private val isDestroyed: () -> Boolean,
    private val invalidateCutout: () -> Unit,
    private val applyWindowParams: (SmartIslandSettings) -> Unit,
) {
    private val viewModel: IslandViewModel get() = viewModelHolder.get()

    private var displayListener: DisplayManager.DisplayListener? = null
    internal var lastRotation: Int = -1
    private var lastDisplaySize: Pair<Int, Int> = 0 to 0
    internal var lastConfigOrientation: Int = Configuration.ORIENTATION_UNDEFINED

    // Rotation veil (model/RotationVeil.kt)
    private var rotationVeilActive = false
    private var rotationVeilStartedAt = 0L
    private var rotationVeilCapJob: Job? = null

    internal fun beginRotationVeil(quarterTurn: Boolean) {
        if (isDestroyed() || !viewModelHolder.ready) return
        viewModel.widgetIsland.close(snap = true)
        // "Remove animations": no fade, so nothing is hidden either
        if (SystemAnimScale.animationsOff()) return
        // edge pill layout: the pill sits on the camera in both orientations
        if (LandscapeLayoutStyle.isEdge(viewModel.displaySettings.value.landscapeLayoutStyle)) {
            beginRotationMorphWindow(quarterTurn)
            return
        }
        if (!rotationVeilActive) {
            rotationVeilActive = true
            rotationVeilStartedAt = SystemClock.elapsedRealtime()
            // Safety net: whatever the refresh does, the island is back after the maximum
            rotationVeilCapJob?.cancel()
            rotationVeilCapJob = scope.launch {
                delay(RotationVeil.MAX_HIDDEN_MS)
                endRotationVeil()
            }
        }
        viewModel.rotationVeil.value = true
    }

    private var rotationMorphCapJob: Job? = null

    private fun beginRotationMorphWindow(quarterTurn: Boolean) {
        viewModel.rotationTargetReady.value = false
        if (quarterTurn) viewModel.rotationQuarterHold.value = true
        viewModel.rotationMorphActive.value = true
        rotationMorphCapJob?.cancel()
        // Safety net: if the overlay never starts (or never ends) a morph
        rotationMorphCapJob = scope.launch {
            delay(RotationMorph.MAX_ACTIVE_MS)
            if (viewModelHolder.ready) {
                viewModel.rotationMorphActive.value = false
                viewModel.rotationQuarterHold.value = false
            }
        }
    }

    private fun endRotationVeil() {
        rotationVeilCapJob?.cancel()
        rotationVeilCapJob = null
        if (!rotationVeilActive) return
        rotationVeilActive = false
        if (viewModelHolder.ready) viewModel.rotationVeil.value = false
    }

    internal fun registerRotationListener() {
        if (displayListener != null) return
        runCatchingLogged(TAG, "registerRotationListener failed") {
            val dm = service.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            // Start from the real rotation, so the first display event
            lastRotation = dm.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: -1
            lastDisplaySize =
                service.resources.displayMetrics.widthPixels to service.resources.displayMetrics.heightPixels
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId != Display.DEFAULT_DISPLAY) return
                    runCatchingLogged(TAG, "rotation change failed") {
                        if (isDestroyed() || !viewModelHolder.ready) return@runCatchingLogged
                        val rotation = dm.getDisplay(Display.DEFAULT_DISPLAY)?.rotation
                            ?: return@runCatchingLogged
                        val metrics = service.resources.displayMetrics
                        val size = metrics.widthPixels to metrics.heightPixels
                        val resized = size != lastDisplaySize
                        lastDisplaySize = size
                        if (resized) invalidateCutout()
                        if (rotation == lastRotation) {
                            if (resized) scheduleOrientationRefresh()
                            return@runCatchingLogged
                        }
                        val quarterTurn = (rotation - lastRotation) and 1 == 1
                        lastRotation = rotation
                        invalidateCutout()
                        beginRotationVeil(quarterTurn)
                        scheduleOrientationRefresh()
                    }
                }
            }
            dm.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
            displayListener = listener
        }
    }

    private val cameraMemory = LandscapeCameraMemory()

    private var orientationRefreshJob: Job? = null

    internal fun scheduleOrientationRefresh() {
        orientationRefreshJob?.cancel()
        orientationRefreshJob = scope.launch {
            // The first read after a turn can still have the old bounds. Give them a chance to settle.
            for ((index, waitMs) in RotationVeil.REFRESH_GAPS_MS.withIndex()) {
                if (waitMs > 0L) delay(waitMs)
                if (isDestroyed() || !viewModelHolder.ready) {
                    endRotationVeil()
                    return@launch
                }
                try {
                    refreshOrientation(settled = index == RotationVeil.REFRESH_GAPS_MS.lastIndex)
                    // Applied directly too, so the window never waits on the settings flow
                    val raw = viewModel.orientation.value
                    val effective = viewModel.settingsSnapshot().forOrientation(
                        raw, hasContent = viewModel.visibleNotifications.value.isNotEmpty()
                    )
                    applyWindowParams(effective)
                } catch (e: Exception) {
                    AppLogRecorder.e(TAG, "orientation refresh failed", e)
                }
            }
            if (rotationVeilActive) {
                val hiddenFor = SystemClock.elapsedRealtime() - rotationVeilStartedAt
                delay(RotationVeil.liftDelayMs(hiddenFor))
                endRotationVeil()
            }
        }
    }

    internal fun unregisterRotationListener() {
        orientationRefreshJob?.cancel()
        orientationRefreshJob = null
        rotationMorphCapJob?.cancel()
        rotationMorphCapJob = null
        if (viewModelHolder.ready) viewModel.rotationMorphActive.value = false
        endRotationVeil()
        val listener = displayListener ?: return
        displayListener = null
        try {
            (service.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(listener)
        } catch (e: Exception) {
            AppLogRecorder.e(TAG, "unregisterRotationListener failed", e)
        }
    }

    internal fun screenWidthPxNow(): Int =
        if (service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            LandscapeCameraDetector.realSizePx(service).first
        else service.resources.displayMetrics.widthPixels

    internal fun refreshOrientation(settled: Boolean = true) {
        if (!viewModelHolder.ready) return
        val isLandscape = service.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        val state = cameraMemory.resolve(
            LandscapeCameraDetector.read(service, isLandscape, viewModel.settingsSnapshot()), settled
        )
        viewModel.orientation.value = state
        if (RotationMorph.targetReady(state, settled)) viewModel.rotationTargetReady.value = true
    }

    private companion object {
        const val TAG = "SmartIslandOverlayService"
    }
}
