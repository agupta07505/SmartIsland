/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings

object LandscapeCameraDetector {

    private const val CENTRED_CUTOUT_FRACTION = 0.4f

    /** Picks the cutout closest to the top/left/right edge and converts it to centre-relative dp. */
    fun fromRects(rects: List<Rect>, screenWidthPx: Int, density: Float): LandscapeCamera? {
        if (density <= 0f || screenWidthPx <= 0) return null
        val rect = rects
            .filter { it.width() > 0 && it.height() > 0 }
            .minByOrNull { minOf(it.left, screenWidthPx - it.right, it.top) }
            ?: return null
        val centerX = (rect.left + rect.right) / 2f
        val centerY = (rect.top + rect.bottom) / 2f
        val xOffsetDp = (centerX - screenWidthPx / 2f) / density
        // A phone's camera sits on a side edge in landscape.
        val halfWidthDp = screenWidthPx / 2f / density
        if (kotlin.math.abs(xOffsetDp) < halfWidthDp * CENTRED_CUTOUT_FRACTION) return null
        return LandscapeCamera(
            xOffsetDp = xOffsetDp,
            centerYDp = centerY / density
        )
    }

    fun sideForRotation(rotation: Int): String = when (rotation) {
        Surface.ROTATION_270 -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
    }

    fun read(context: Context, isLandscape: Boolean): OrientationState {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val screenWidthDp = if (density > 0f) metrics.widthPixels / density else 0f
        val screenHeightDp = if (density > 0f) metrics.heightPixels / density else 0f
        if (!isLandscape) {
            return OrientationState(isLandscape = false, screenWidthDp = screenWidthDp)
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = runCatching { windowManager?.defaultDisplay }.getOrNull()
        // The display manager's view of the default display is the freshest source for rotation.
        val dmDisplay = runCatching {
            (context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager)
                ?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        }.getOrNull()
        val rotation = runCatching { dmDisplay?.rotation }.getOrNull()
            ?: runCatching { display?.rotation }.getOrNull()
            ?: Surface.ROTATION_90

        val camera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rects = runCatching { (dmDisplay ?: display)?.cutout?.boundingRects }.getOrNull().orEmpty()
            fromRects(rects, metrics.widthPixels, density)
        } else {
            null
        }

        return OrientationState(
            isLandscape = true,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            camera = camera,
            rotationSide = sideForRotation(rotation),
            reversed = rotation == Surface.ROTATION_270
        )
    }
}
