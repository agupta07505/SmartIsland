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
import android.view.Surface
import android.view.WindowManager
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import kotlin.math.abs

object LandscapeCameraDetector {

    private const val CENTRED_CUTOUT_FRACTION = 0.4f

    fun fromRects(rects: List<Rect>, screenWidthPx: Int, density: Float,
            pathBounds: CutoutRectPx? = null): LandscapeCamera? {
        if (density <= 0f || screenWidthPx <= 0) return null
        val picked = rects
            .filter { it.width() > 0 && it.height() > 0 }
            .minByOrNull { minOf(it.left, screenWidthPx - it.right, it.top) }
            ?: return null
        // The reserved box is often much bigger than the camera hole
        val hole = CutoutPick.refineSide(CutoutRectPx(picked.left, picked.top, picked.right, picked.bottom), pathBounds,
            screenWidthPx)
        val rect = Rect(hole.left, hole.top, hole.right, hole.bottom)
        val centerX = (rect.left + rect.right) / 2f
        val centerY = (rect.top + rect.bottom) / 2f
        val xOffsetDp = (centerX - screenWidthPx / 2f) / density
        // a phone's camera sits on a side edge in landscape
        val halfWidthDp = screenWidthPx / 2f / density
        if (abs(xOffsetDp) < halfWidthDp * CENTRED_CUTOUT_FRACTION) return null
        return LandscapeCamera(
            xOffsetDp = xOffsetDp,
            centerYDp = centerY / density,
            widthDp = rect.width() / density,
            lengthDp = rect.height() / density
        )
    }

    fun realSizePx(context: Context): Pair<Int, Int> {
        val res = context.resources.displayMetrics
        val display = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        display?.getRealMetrics(real)
        return if (real.widthPixels > 0 && real.heightPixels > 0) real.widthPixels to real.heightPixels
        else res.widthPixels to res.heightPixels
    }

    fun sideForRotation(rotation: Int): String = when (rotation) {
        Surface.ROTATION_270 -> SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        else -> SmartIslandSettings.LANDSCAPE_POSITION_LEFT
    }

    fun read(context: Context, isLandscape: Boolean, settings: SmartIslandSettings? = null): OrientationState {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val (widthPx, heightPx) = realSizePx(context)
        val screenWidthDp = if (density > 0f) widthPx / density else 0f
        val screenHeightDp = if (density > 0f) heightPx / density else 0f
        if (!isLandscape) {
            val hole = CameraCutoutDetector.detectedArea(CameraCutoutDetector.detect(context))
            return OrientationState(
                isLandscape = false,
                screenWidthDp = screenWidthDp,
                portraitCameraWidthDp = hole?.widthDp ?: 0f,
                portraitCutout = hole,
                posture = FoldDetector.postureFor(context, minOf(screenWidthDp, screenHeightDp),
                    maxOf(screenWidthDp, screenHeightDp))
            )
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        val dmDisplay = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
        val rotation = dmDisplay?.rotation
            ?: display?.rotation
            ?: Surface.ROTATION_90

        val detected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cutout = (dmDisplay ?: display)?.cutout
            val rects = cutout?.boundingRects.orEmpty()
            val pathBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val b = RectF()
                cutout?.cutoutPath?.computeBounds(b, true)
                if (b.isEmpty) null else CutoutRectPx(
                    Math.floor(b.left.toDouble()).toInt(), Math.floor(b.top.toDouble()).toInt(),
                    Math.ceil(b.right.toDouble()).toInt(), Math.ceil(b.bottom.toDouble()).toInt()
                )
            } else null
            fromRects(rects, widthPx, density, pathBounds)
        } else {
            null
        }

        val reversed = rotation == Surface.ROTATION_270
        val portraitWidthDp = minOf(screenWidthDp, screenHeightDp)
        // the hole placed in portrait (a style or the manual hole) comes before what the system reports, as in portrait
        val camera = settings?.let { CutoutPlacement.placedFor(it, portraitWidthDp)?.clampedTo(portraitWidthDp) }?.let {
            CutoutPlacement.landscapeCamera(it, portraitWidthDp, maxOf(screenWidthDp, screenHeightDp), reversed)
        } ?: detected ?: settings?.let {
            // nothing reported (a hidden cutout): the portrait centre, turned, sits mid-edge, not on the left default
            CutoutPlacement.landscapeCamera(
                CutoutPlacement.centreGuess(it), portraitWidthDp, maxOf(screenWidthDp, screenHeightDp), reversed
            )
        }

        return OrientationState(
            isLandscape = true,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            camera = camera,
            rotationSide = sideForRotation(rotation),
            reversed = reversed,
            posture = FoldDetector.postureFor(context, maxOf(screenWidthDp, screenHeightDp),
                minOf(screenWidthDp, screenHeightDp))
        )
    }
}
