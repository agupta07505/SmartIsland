/*
 * Realistic Dynamic Island – Overlay Service skeleton
 * Draws the island over the punch-hole using WindowManager + Accessibility
 */

package com.srcpolygot.dynamicisland.service

import android.accessibilityservice.AccessibilityService
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.srcpolygot.dynamicisland.domain.model.ActiveIslandState
import com.srcpolygot.dynamicisland.ui.overlay.IslandOverlay

/**
 * AccessibilityService that hosts the Dynamic Island overlay.
 * This is the system entry point that keeps the island alive above other apps
 * and correctly positioned over the camera cutout.
 */
class DynamicIslandOverlayService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private var currentState = ActiveIslandState()

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // In a full implementation we would create the ComposeView here
        // and add it with TYPE_ACCESSIBILITY_OVERLAY or TYPE_APPLICATION_OVERLAY.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We primarily react to notification events via the separate
        // NotificationListenerService. This method can be used for
        // additional gesture or window change detection if needed.
    }

    override fun onInterrupt() {
        // Required override
    }

    fun updateState(state: ActiveIslandState) {
        currentState = state
        // TODO: recompose the overlay with the new state
    }

    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
        }
    }

    override fun onDestroy() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        super.onDestroy()
    }
}
