/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Face to fingerprint. */
internal const val FP_MORPH_IN_MS = 120

/** The scan line going up, then coming back down. */
internal const val FP_SCAN_MS = 900
internal const val FP_SWEEP_UP_MS = FP_SCAN_MS / 2
internal const val FP_SWEEP_DOWN_MS = FP_SCAN_MS / 2

internal const val FP_TO_TICK_MS = FACE_TO_TICK_MS

internal const val FP_HOLD_MS = TICK_BEAT_MS

/** Share of the tick morph over which the fingerprint fades into the tick shape. */
internal const val FP_TICK_FADE = 0.35f

internal const val FP_TOTAL_MS = FP_MORPH_IN_MS + FP_SCAN_MS + FP_TO_TICK_MS + FP_HOLD_MS

internal const val FPD_FADE_IN_MS = 100

/** Scan position meaning "no scan line". */
internal const val FP_NO_SCAN = -1f

private const val FP_CENTRE_DROP = 2f
private const val FP_BAND = 9f
private const val FP_LINE_HALF_WIDTH = 36f

private const val FP_ICON_CENTRE = 12f
private const val FP_ICON_TOP = 2f
private const val FP_ICON_BOTTOM = 22f
private const val FP_ICON_HALF_W = 9.07f
private const val FP_ICON_SCALE = 3.1f

private const val FP_CY = FACE_ID_HEAD_CY + FP_CENTRE_DROP

private const val FP_TOP = FP_CY + (FP_ICON_TOP - FP_ICON_CENTRE) * FP_ICON_SCALE
private const val FP_BOTTOM = FP_CY + (FP_ICON_BOTTOM - FP_ICON_CENTRE) * FP_ICON_SCALE

private const val FP_ICON_DATA =
    "M17.81,4.47c-0.08,0 -0.16,-0.02 -0.23,-0.06C15.66,3.42 14,3 12.01,3c-1.98,0 -3.86,0.47 -5.57,1.41 " +
        "-0.24,0.13 -0.54,0.04 -0.68,-0.2 -0.13,-0.24 -0.04,-0.55 0.2,-0.68C7.82,2.52 9.86,2 12.01,2c2.13,0 " +
        "3.99,0.47 6.03,1.52 0.25,0.13 0.34,0.43 0.21,0.67 -0.09,0.18 -0.26,0.28 -0.44,0.28zM3.5,9.72c-0.1,0 " +
        "-0.2,-0.03 -0.29,-0.09 -0.23,-0.16 -0.28,-0.47 -0.12,-0.7 0.99,-1.4 2.25,-2.5 3.75,-3.27C9.98,4.04 " +
        "14,4.03 17.15,5.65c1.5,0.77 2.76,1.86 3.75,3.25 0.16,0.22 0.11,0.54 -0.12,0.7 -0.23,0.16 -0.54,0.11 " +
        "-0.7,-0.12 -0.9,-1.26 -2.04,-2.25 -3.39,-2.94 -2.87,-1.47 -6.54,-1.47 -9.4,0.01 -1.36,0.7 -2.5,1.7 " +
        "-3.4,2.96 -0.08,0.14 -0.23,0.21 -0.39,0.21zM9.75,21.79c-0.13,0 -0.26,-0.05 -0.35,-0.15 -0.87,-0.87 " +
        "-1.34,-1.43 -2.01,-2.64 -0.69,-1.23 -1.05,-2.73 -1.05,-4.34 0,-2.97 2.54,-5.39 5.66,-5.39s5.66,2.42 " +
        "5.66,5.39c0,0.28 -0.22,0.5 -0.5,0.5s-0.5,-0.22 -0.5,-0.5c0,-2.42 -2.09,-4.39 -4.66,-4.39 -2.57,0 " +
        "-4.66,1.97 -4.66,4.39 0,1.44 0.32,2.77 0.93,3.85 0.64,1.15 1.08,1.64 1.85,2.42 0.19,0.2 0.19,0.51 0,0.71 " +
        "-0.11,0.1 -0.24,0.15 -0.37,0.15zM16.92,19.94c-1.19,0 -2.24,-0.3 -3.1,-0.89 -1.49,-1.01 -2.38,-2.65 " +
        "-2.38,-4.39 0,-0.28 0.22,-0.5 0.5,-0.5s0.5,0.22 0.5,0.5c0,1.41 0.72,2.74 1.94,3.56 0.71,0.48 1.54,0.71 " +
        "2.54,0.71 0.24,0 0.64,-0.03 1.04,-0.1 0.27,-0.05 0.53,0.13 0.58,0.41 0.05,0.27 -0.13,0.53 -0.41,0.58 " +
        "-0.57,0.1 -1.06,0.12 -1.21,0.12zM14.91,22c-0.04,0 -0.09,-0.01 -0.13,-0.02 -1.59,-0.44 -2.63,-1.03 " +
        "-3.72,-2.1 -1.4,-1.39 -2.17,-3.24 -2.17,-5.22 0,-1.62 1.38,-2.94 3.08,-2.94 1.7,0 3.08,1.32 3.08,2.94 " +
        "0,1.07 0.93,1.94 2.08,1.94s2.08,-0.87 2.08,-1.94c0,-3.77 -3.25,-6.83 -7.25,-6.83 -2.84,0 -5.44,1.58 " +
        "-6.61,4.03 -0.39,0.81 -0.59,1.76 -0.59,2.8 0,0.78 0.07,2.01 0.67,3.61 0.1,0.26 -0.03,0.55 -0.29,0.64 " +
        "-0.26,0.1 -0.55,-0.04 -0.64,-0.29 -0.49,-1.31 -0.73,-2.61 -0.73,-3.96 0,-1.2 0.23,-2.29 0.68,-3.24 " +
        "1.33,-2.79 4.28,-4.6 7.51,-4.6 4.55,0 8.25,3.51 8.25,7.83 0,1.62 -1.38,2.94 -3.08,2.94s-3.08,-1.32 " +
        "-3.08,-2.94c0,-1.07 -0.93,-1.94 -2.08,-1.94s-2.08,0.87 -2.08,1.94c0,1.71 0.66,3.31 1.87,4.51 0.95,0.94 " +
        "1.86,1.46 3.27,1.85 0.27,0.07 0.42,0.35 0.35,0.61 -0.05,0.23 -0.26,0.38 -0.47,0.38z"

private val fpIcon: Path by lazy { PathParser.createPathFromPathData(FP_ICON_DATA).asComposePath() }

private const val FP_OUTLINE_POINTS = 64

internal val FP_OUTLINE: FloatArray = FloatArray(FP_OUTLINE_POINTS * 2).also {
    val ry = (FP_ICON_BOTTOM - FP_ICON_TOP) / 2f * FP_ICON_SCALE
    val rx = FP_ICON_HALF_W * FP_ICON_SCALE
    for (i in 0 until FP_OUTLINE_POINTS) {
        val a = 2.0 * PI * i / FP_OUTLINE_POINTS
        it[2 * i] = FACE_ID_HEAD_CX + rx * cos(a).toFloat()
        it[2 * i + 1] = FP_CY + ry * sin(a).toFloat()
    }
}

internal fun faceIdFingerprintTickOutlines(): Pair<FloatArray, FloatArray> {
    val from = faceIdResample(FP_OUTLINE, FACE_ID_MORPH_POINTS)
    return from to faceIdAlign(from, faceIdResample(FACE_ID_TICK, FACE_ID_MORPH_POINTS))
}

private fun DrawScope.fingerprintIcon(color: Color) {
    withTransform({
        translate(FACE_ID_HEAD_CX - FP_ICON_CENTRE * FP_ICON_SCALE, FP_CY - FP_ICON_CENTRE * FP_ICON_SCALE)
        scale(FP_ICON_SCALE, FP_ICON_SCALE, pivot = Offset.Zero)
    }) {
        drawPath(fpIcon, color)
    }
}

internal fun DrawScope.drawFingerprint(alpha: Float, scan: Float = FP_NO_SCAN) {
    if (alpha <= 0f) return
    val scanning = scan in 0f..1f
    fingerprintIcon(Color.White.copy(alpha = if (scanning) alpha * 0.4f else alpha))
    if (!scanning) return
    val y = FP_BOTTOM - (FP_BOTTOM - FP_TOP) * scan
    clipRect(
        left = FACE_ID_HEAD_CX - 40f,
        top = y - FP_BAND,
        right = FACE_ID_HEAD_CX + 40f,
        bottom = y + FP_BAND
    ) {
        fingerprintIcon(Color.White.copy(alpha = alpha))
    }
    drawLine(
        color = Color.White.copy(alpha = alpha),
        start = Offset(FACE_ID_HEAD_CX - FP_LINE_HALF_WIDTH, y),
        end = Offset(FACE_ID_HEAD_CX + FP_LINE_HALF_WIDTH, y),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}
