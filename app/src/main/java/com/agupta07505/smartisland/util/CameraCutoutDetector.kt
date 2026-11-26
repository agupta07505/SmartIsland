/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.DisplayCutout
import android.view.WindowManager
import androidx.compose.runtime.mutableIntStateOf
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CutoutArea
import kotlin.math.abs

data class DetectedCutoutInfo(
    val xOffsetDp: Float,
    val yOffsetDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val hasHardwareCutout: Boolean,
    val centreXDp: Float = xOffsetDp,
    val holeWidthDp: Float = (widthDp - CameraCutoutDetector.PADDING_W_DP).coerceAtLeast(1f),
    val holeHeightDp: Float = (heightDp - CameraCutoutDetector.PADDING_H_DP).coerceAtLeast(1f)
)

object CameraCutoutDetector {

    private val generationState = mutableIntStateOf(0)
    val generation: Int get() = generationState.intValue

    fun invalidate() {
        generationState.intValue = generationState.intValue + 1
    }

    /**
     * Camera hole as best known, portrait only: the placed hole (notch mode ignores the manual area but follows a
     * style), then the detected one. With neither, [assumeCentre] guesses a hole at the screen centre, which is where
     * a hidden cutout (a notch-bar killer) usually is, so the pulse still splits around it
     */
    fun areaFor(
        settings: SmartIslandSettings,
        detected: DetectedCutoutInfo,
        isLandscape: Boolean,
        screenWidthDp: Float = 0f,
        assumeCentre: Boolean = false
    ): CutoutArea? {
        if (isLandscape) return null
        val placed = if (settings.enableNotchMode) CutoutPlacement.styledArea(settings, screenWidthDp)
        else CutoutPlacement.area(settings, screenWidthDp)
        return placed?.clampedTo(screenWidthDp)
            ?: detectedArea(detected)
            ?: CutoutPlacement.centreGuess(settings).takeIf { assumeCentre }
    }

    /** The hole the layout reads: none in notch mode, whose own placement and size rules do not know it yet. */
    fun layoutAreaFor(
        settings: SmartIslandSettings,
        detected: DetectedCutoutInfo,
        isLandscape: Boolean,
        screenWidthDp: Float = 0f
    ): CutoutArea? = areaFor(settings, detected, isLandscape, screenWidthDp).takeUnless { settings.enableNotchMode }

    /** Real size, no padding */
    fun detectedArea(detected: DetectedCutoutInfo): CutoutArea? =
        if (detected.hasHardwareCutout) {
            CutoutArea(
                detected.centreXDp,
                detected.yOffsetDp,
                detected.holeWidthDp,
                detected.holeHeightDp
            )
        } else null

    const val PADDING_W_DP = 24f
    const val PADDING_H_DP = 10f

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

        val paddingW = PADDING_W_DP
        val paddingH = PADDING_H_DP
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
            hasHardwareCutout = true,
            centreXDp = xOffsetPx / density,
            holeWidthDp = ((right - left).toFloat() / density).coerceAtLeast(1f),
            holeHeightDp = ((bottom - top).toFloat() / density).coerceAtLeast(1f)
        )
    }

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

    /** Centred cutout attached to a screen edge. */
    fun isTeardropRect(left: Int, top: Int, right: Int, bottom: Int, screenWidthPx: Int, screenHeightPx: Int): Boolean {
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0 || screenWidthPx <= 0 || screenHeightPx <= 0) return false
        val onTop = top <= 0
        val onLeft = left <= 0
        val onRight = right >= screenWidthPx
        val along: Float
        val depth: Float
        val edgeLength: Float
        val centre: Float
        when {
            onTop && !onLeft && !onRight -> {
                along = w.toFloat(); depth = h.toFloat(); edgeLength = screenWidthPx.toFloat(); centre =
                    (left + right) / 2f
            }
            (onLeft || onRight) && !onTop && bottom < screenHeightPx -> {
                along = h.toFloat(); depth = w.toFloat(); edgeLength = screenHeightPx.toFloat(); centre =
                    (top + bottom) / 2f
            }
            else -> return false
        }
        val ratio = along / depth
        return abs(centre - edgeLength / 2f) <= edgeLength * TEARDROP_CENTRE_TOLERANCE &&
            ratio in TEARDROP_MIN_RATIO..TEARDROP_MAX_RATIO &&
            along <= edgeLength * TEARDROP_MAX_SHARE
    }

    fun isTeardrop(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return quietly(false) {
            val display = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                ?.getDisplay(Display.DEFAULT_DISPLAY)
                ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val size = realSizePx(display, context)
            val rects = display.cutout?.boundingRects.orEmpty()
            rects.size == 1 && rects[0].let {
                isTeardropRect(it.left, it.top, it.right, it.bottom, size.first, size.second)
            }
        }
    }

    private fun realSizePx(display: Display, context: Context): Pair<Int, Int> {
        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(real)
        if (real.widthPixels > 0 && real.heightPixels > 0) return real.widthPixels to real.heightPixels
        val metrics = context.resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private const val TEARDROP_CENTRE_TOLERANCE = 0.08f
    private const val TEARDROP_MIN_RATIO = 1.15f
    private const val TEARDROP_MAX_RATIO = 2.5f
    private const val TEARDROP_MAX_SHARE = 0.4f

    fun detect(context: Context): DetectedCutoutInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return fallbackInfo(context)

        val metrics = context.resources.displayMetrics
        val screenWidthPx = metrics.widthPixels
        val density = metrics.density

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The display manager's own display first
            val display = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                ?.getDisplay(Display.DEFAULT_DISPLAY) ?: windowManager.defaultDisplay
            val cutout: DisplayCutout? = display?.cutout

            val boundingRects = cutout?.boundingRects
            if (!boundingRects.isNullOrEmpty()) {
                val picked = CutoutPick.pick(
                    boundingRects.map { CutoutRectPx(it.left, it.top, it.right, it.bottom) },
                    screenWidthPx, density
                )
                if (picked != null) {
                    // Reserved box is often taller than the hole
                    val pathBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val bounds = RectF()
                        cutout?.cutoutPath?.computeBounds(bounds, true)
                        if (bounds.isEmpty) null else CutoutRectPx(
                            Math.floor(bounds.left.toDouble()).toInt(), Math.floor(bounds.top.toDouble()).toInt(),
                            Math.ceil(bounds.right.toDouble()).toInt(), Math.ceil(bounds.bottom.toDouble()).toInt()
                        )
                    } else null
                    val hole = CutoutPick.refine(picked, pathBounds)
                    return calculateCutoutOffset(hole.left, hole.top, hole.right, hole.bottom, screenWidthPx, density)
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
