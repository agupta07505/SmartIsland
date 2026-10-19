/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.os.Build
import com.agupta07505.smartisland.ui.IslandViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controller for Ambient Island (AOD Emulation).
 * Keeps a minimal monochrome island visible when the screen turns off on OLED devices,
 * with periodic burn-in protection shifts (±5dp every 30 seconds).
 */
class AmbientIslandController(
    private val context: Context,
    private val viewModel: IslandViewModel,
    private val scope: CoroutineScope
) {
    var enabled: Boolean = false

    private val _isAmbientMode = MutableStateFlow(false)
    val isAmbientMode: StateFlow<Boolean> = _isAmbientMode.asStateFlow()

    private val _burnInShiftXDp = MutableStateFlow(0f)
    val burnInShiftXDp: StateFlow<Float> = _burnInShiftXDp.asStateFlow()

    private val _burnInShiftYDp = MutableStateFlow(0f)
    val burnInShiftYDp: StateFlow<Float> = _burnInShiftYDp.asStateFlow()

    private var driftJob: Job? = null

    // Predefined burn-in drift pattern (±5dp range) to protect OLED pixels from static retention
    private val driftOffsets = listOf(
        Pair(0f, 0f),
        Pair(3f, -2f),
        Pair(-4f, 3f),
        Pair(2f, 4f),
        Pair(-3f, -3f),
        Pair(4f, 1f),
        Pair(-2f, -4f),
        Pair(5f, -1f),
        Pair(-5f, 2f)
    )
    private var driftIndex = 0

    init {
        scope.launch {
            viewModel.settings.collect { s ->
                enabled = s.aodEnabled
                if (!enabled && _isAmbientMode.value) {
                    onScreenOn()
                }
            }
        }
    }

    /**
     * Checks if current hardware is likely equipped with an OLED/AMOLED panel.
     */
    fun isOledDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("samsung") ||
            manufacturer.contains("oneplus") ||
            manufacturer.contains("oppo") ||
            manufacturer.contains("vivo") ||
            manufacturer.contains("xiaomi") ||
            manufacturer.contains("google") ||
            manufacturer.contains("pixel") ||
            manufacturer.contains("nothing") ||
            manufacturer.contains("realme") ||
            manufacturer.contains("motorola") ||
            manufacturer.contains("sony")
    }

    /**
     * Called when ACTION_SCREEN_OFF is broadcast.
     */
    fun onScreenOff() {
        if (!enabled || !isOledDevice()) {
            _isAmbientMode.value = false
            driftJob?.cancel()
            driftJob = null
            viewModel.setAmbientMode(false, 0f, 0f)
            return
        }

        _isAmbientMode.value = true
        viewModel.setAmbientMode(true, 0f, 0f)
        driftJob?.cancel()
        driftJob = scope.launch {
            while (isActive) {
                delay(30_000L) // Shift every 30 seconds
                driftIndex = (driftIndex + 1) % driftOffsets.size
                val (dx, dy) = driftOffsets[driftIndex]
                _burnInShiftXDp.value = dx
                _burnInShiftYDp.value = dy
                viewModel.setAmbientMode(true, dx, dy)
            }
        }
    }

    /**
     * Called when ACTION_SCREEN_ON is broadcast.
     */
    fun onScreenOn() {
        _isAmbientMode.value = false
        driftJob?.cancel()
        driftJob = null
        _burnInShiftXDp.value = 0f
        _burnInShiftYDp.value = 0f
        viewModel.setAmbientMode(false, 0f, 0f)
    }

    fun stop() {
        onScreenOn()
    }

    companion object {
        private const val TAG = "AmbientIslandController"
    }
}
