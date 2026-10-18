/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.agupta07505.smartisland.BuildConfig
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Detects true immersive mode (Rule_Book 10.1): a full-screen app (video, camera, game) has hidden the
 * status bar. Other apps' window flags cannot be read, but every window is told whether the system bars
 * are showing, so this adds an invisible, untouchable 1px-wide accessibility-overlay probe and watches it:
 *  - Android 11+: the probe's window insets, `isVisible(WindowInsets.Type.statusBars())`.
 *  - Older: the global system UI visibility, `SYSTEM_UI_FLAG_FULLSCREEN`.
 *
 * Entering immersive waits [ENTER_DEBOUNCE_MS] so an app switch that flickers the bars does not blink the
 * island; leaving is immediate.
 */
class ImmersiveModeMonitor(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val immersive = MutableStateFlow(false)

    /** True while the status bar is hidden by the foreground app. */
    val isImmersive: StateFlow<Boolean> = immersive

    private val mainHandler = Handler(Looper.getMainLooper())
    private var probe: View? = null
    private val enterImmersive = Runnable { immersive.value = true }

    fun start() {
        if (probe != null) return
        runCatchingLogged(TAG, "Immersive probe could not be added") {
            val view = View(context).apply {
                isFocusable = false
                isClickable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.setOnApplyWindowInsetsListener { _, insets ->
                    onStatusBarVisible(statusBarVisible(insets))
                    insets
                }
            } else {
                @Suppress("DEPRECATION")
                view.setOnSystemUiVisibilityChangeListener { visibility ->
                    onStatusBarVisible(visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0)
                }
            }
            windowManager.addView(view, probeParams())
            probe = view
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(enterImmersive)
        immersive.value = false
        val view = probe ?: return
        probe = null
        runCatchingLogged(TAG, "Immersive probe could not be removed") {
            if (view.isAttachedToWindow) windowManager.removeViewImmediate(view)
        }
    }

    /**
     * Some ROMs (One UI) leave the status-bar source out of the insets sent to overlays layered above it, and
     * `isVisible` then reads false forever. A missing source tells nothing, so only a status bar that is
     * present but hidden counts as immersive.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun statusBarVisible(insets: WindowInsets): Boolean {
        val type = WindowInsets.Type.statusBars()
        val present = insets.getInsetsIgnoringVisibility(type).top > 0
        val visible = insets.isVisible(type)
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, "status bar present=$present visible=$visible")
        return !present || visible
    }

    private fun onStatusBarVisible(visible: Boolean) {
        mainHandler.removeCallbacks(enterImmersive)
        if (visible) {
            immersive.value = false
        } else if (!immersive.value) {
            mainHandler.postDelayed(enterImmersive, ENTER_DEBOUNCE_MS)
        }
    }

    private fun probeParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            1,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Keep the frame over the bars so it is not re-laid out each time they hide.
                setFitInsetsTypes(0)
            }
        }

    companion object {
        private const val TAG = "ImmersiveModeMonitor"
        const val ENTER_DEBOUNCE_MS = 400L
    }
}
