package com.agupta07505.smartisland.util

import android.graphics.Point
import android.os.Build
import android.view.WindowManager

/**
 * Where the physical front camera sits on the current display, relative to the screen center.
 * The collapsed island is always centered on it, and collapsed content keeps out of [holeWidthDp].
 */
data class CameraAnchor(val xOffsetDp: Float, val holeWidthDp: Float)

object CameraAnchorResolver {
    /**
     * Reads the top display cutout of the current display. A display without a top cutout
     * (e.g. a foldable's under-display inner screen) anchors to the screen center.
     * Returns null below API 29, where the cutout cannot be read outside a window.
     */
    @Suppress("DEPRECATION")
    fun resolve(windowManager: WindowManager, density: Float): CameraAnchor? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || density <= 0f) return null
        val display = windowManager.defaultDisplay ?: return null
        val size = Point().also { display.getRealSize(it) }
        val rect = display.cutout?.boundingRectTop
        if (rect == null || rect.isEmpty || size.x <= 0) return CameraAnchor(0f, 0f)
        return CameraAnchor(
            xOffsetDp = (rect.exactCenterX() - size.x / 2f) / density,
            holeWidthDp = rect.width() / density
        )
    }
}
