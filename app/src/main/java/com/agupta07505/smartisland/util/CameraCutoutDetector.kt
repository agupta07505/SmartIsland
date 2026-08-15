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
import android.view.DisplayCutout
import android.view.WindowManager
import com.agupta07505.smartisland.data.SmartIslandSettings

data class DetectedCutoutInfo(
    val xOffsetDp: Float,
    val yOffsetDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val hasHardwareCutout: Boolean
)

object CameraCutoutDetector {

    /**
     * Calculates the physical pill position and dimensions matching the device's camera cutout coordinates.
     */
    fun calculateCutoutOffset(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        screenWidthPx: Int,
        density: Float
    ): DetectedCutoutInfo {
        if (density <= 0f) {
            return DetectedCutoutInfo(0f, 12f, 112f, 34f, false)
        }
        val cutoutCenterX = (left + right) / 2f
        val screenCenterX = screenWidthPx / 2f
        val xOffsetPx = cutoutCenterX - screenCenterX
        val xOffsetDp = (xOffsetPx / density).coerceIn(
            SmartIslandSettings.MIN_X_OFFSET,
            SmartIslandSettings.MAX_X_OFFSET
        )

        val yOffsetDp = (top.toFloat() / density).coerceIn(
            SmartIslandSettings.MIN_Y_OFFSET,
            SmartIslandSettings.MAX_Y_OFFSET
        )

        val paddingW = 24f
        val paddingH = 10f
        val widthDp = ((right - left).toFloat() / density + paddingW).coerceIn(
            SmartIslandSettings.MIN_WIDTH,
            SmartIslandSettings.MAX_WIDTH
        )
        val heightDp = ((bottom - top).toFloat() / density + paddingH).coerceIn(
            SmartIslandSettings.MIN_HEIGHT,
            SmartIslandSettings.MAX_HEIGHT
        )

        return DetectedCutoutInfo(
            xOffsetDp = xOffsetDp,
            yOffsetDp = yOffsetDp,
            widthDp = widthDp,
            heightDp = heightDp,
            hasHardwareCutout = true
        )
    }

    /**
     * Calculates the physical pill position and dimensions matching the device's camera cutout rect.
     */
    fun calculateCutoutOffset(
        rect: Rect,
        screenWidthPx: Int,
        density: Float
    ): DetectedCutoutInfo = calculateCutoutOffset(
        left = rect.left,
        top = rect.top,
        right = rect.right,
        bottom = rect.bottom,
        screenWidthPx = screenWidthPx,
        density = density
    )

    /**
     * Detects hardware camera cutout on the current device using WindowInsets / DisplayCutout API.
     */
    fun detect(context: Context): DetectedCutoutInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return fallbackInfo(context)

        val metrics = context.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels
        val density = metrics.density

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val display = runCatching { windowManager.defaultDisplay }.getOrNull()
            val cutout: DisplayCutout? = try {
                display?.cutout
            } catch (e: Throwable) {
                null
            }

            val boundingRects = cutout?.boundingRects
            if (!boundingRects.isNullOrEmpty()) {
                val topCutout = boundingRects.minByOrNull { it.top }
                if (topCutout != null && topCutout.height() > 0) {
                    return calculateCutoutOffset(topCutout, screenWidthPx, density)
                }
            }
        }

        return fallbackInfo(context)
    }

    private fun fallbackInfo(context: Context): DetectedCutoutInfo {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val heightPx = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        val density = context.resources.displayMetrics.density
        val heightDp = if (density > 0f) heightPx / density else 24f
        val yOffsetDp = (heightDp * 0.3f).coerceIn(0f, 30f)

        return DetectedCutoutInfo(
            xOffsetDp = 0f,
            yOffsetDp = yOffsetDp,
            widthDp = 112f,
            heightDp = 34f,
            hasHardwareCutout = false
        )
    }
}
