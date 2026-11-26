/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

object TransferGlyphMotion {
    const val COPY_PERIOD_MS = 1_700
    const val DELETE_PERIOD_MS = 1_600
    const val INSTALL_PERIOD_MS = 1_500

    const val COPY_REST_PHASE = 0.84f
    const val DELETE_REST_PHASE = 0.80f
    const val INSTALL_REST_PHASE = 0.45f

    fun hasOwnGlyph(kind: TransferKind): Boolean = periodMs(kind) > 0

    fun periodMs(kind: TransferKind): Int = when (kind) {
        TransferKind.COPY_MOVE -> COPY_PERIOD_MS
        TransferKind.DELETE -> DELETE_PERIOD_MS
        TransferKind.INSTALL -> INSTALL_PERIOD_MS
        TransferKind.DOWNLOAD, TransferKind.UPLOAD, TransferKind.BACKUP -> 0
    }

    fun restPhase(kind: TransferKind): Float = when (kind) {
        TransferKind.COPY_MOVE -> COPY_REST_PHASE
        TransferKind.DELETE -> DELETE_REST_PHASE
        TransferKind.INSTALL -> INSTALL_REST_PHASE
        else -> 0f
    }

    private fun wrap(phase: Float): Float = phase - floor(phase.toDouble()).toFloat()

    private fun bump(p: Float, up0: Float, up1: Float, down0: Float, down1: Float): Float =
        smoothstep(ramp(p, up0, up1)) - smoothstep(ramp(p, down0, down1))

    const val COPY_PAGE_W = 0.50f
    const val COPY_PAGE_H = 0.62f
    const val COPY_SOURCE_X = 0.10f
    const val COPY_SOURCE_Y = 0.06f
    const val COPY_TARGET_X = 0.38f
    const val COPY_TARGET_Y = 0.32f

    data class CopyFrame(val x: Float, val y: Float, val alpha: Float, val fill: Float, val scale: Float)

    fun copy(phase: Float): CopyFrame {
        val p = wrap(phase)
        val appear = smoothstep(ramp(p, 0f, 0.14f))
        val move = smoothstep(ramp(p, 0.14f, 0.62f))
        val fadeOut = 1f - smoothstep(ramp(p, 0.88f, 1f))
        val pulse = sin(PI * ramp(p, 0.62f, 0.82f)).toFloat()
        return CopyFrame(
            x = lerpFloat(COPY_SOURCE_X, COPY_TARGET_X, move),
            y = lerpFloat(COPY_SOURCE_Y, COPY_TARGET_Y, move),
            alpha = appear * fadeOut,
            fill = 0.4f * pulse,
            scale = 1f + 0.07f * pulse
        )
    }

    const val DELETE_LID_OPEN_DEGREES = 70f
    const val DELETE_PAGE_START_Y = -0.12f
    const val DELETE_PAGE_END_Y = 0.60f
    const val DELETE_SWEEP_BASE_ALPHA = 0.35f
    const val DELETE_SWEEP_SLIDE = 0.10f

    data class DeleteFrame(
        val lidDegrees: Float,
        val pageY: Float,
        val pageAlpha: Float,
        val sweepAlphaTop: Float,
        val sweepShiftTop: Float,
        val sweepAlphaBottom: Float,
        val sweepShiftBottom: Float
    )

    fun delete(phase: Float): DeleteFrame {
        val p = wrap(phase)
        val lidOpen = bump(p, 0.02f, 0.16f, 0.60f, 0.72f)
        val fall = ramp(p, 0.12f, 0.60f)
        val pageAlpha = smoothstep(ramp(p, 0.06f, 0.14f)) * (1f - smoothstep(ramp(p, 0.54f, 0.60f)))
        val top = bump(p, 0.62f, 0.74f, 0.88f, 0.98f)
        val bottom = bump(p, 0.66f, 0.78f, 0.90f, 1.0f)
        return DeleteFrame(
            lidDegrees = DELETE_LID_OPEN_DEGREES * lidOpen,
            pageY = lerpFloat(DELETE_PAGE_START_Y, DELETE_PAGE_END_Y, fall * fall),
            pageAlpha = pageAlpha,
            sweepAlphaTop = DELETE_SWEEP_BASE_ALPHA + (1f - DELETE_SWEEP_BASE_ALPHA) * top,
            sweepShiftTop = -DELETE_SWEEP_SLIDE * (1f - top),
            sweepAlphaBottom = DELETE_SWEEP_BASE_ALPHA + (1f - DELETE_SWEEP_BASE_ALPHA) * bottom,
            sweepShiftBottom = -DELETE_SWEEP_SLIDE * (1f - bottom)
        )
    }

    const val INSTALL_ARROW_START_DY = -0.34f

    data class InstallFrame(val arrowDy: Float, val arrowAlpha: Float, val fill: Float)

    fun install(phase: Float): InstallFrame {
        val p = wrap(phase)
        val drop = smoothstep(ramp(p, 0f, 0.5f))
        return InstallFrame(
            arrowDy = lerpFloat(INSTALL_ARROW_START_DY, 0f, drop),
            arrowAlpha = smoothstep(ramp(p, 0f, 0.14f)) * (1f - smoothstep(ramp(p, 0.86f, 1f))),
            fill = smoothstep(ramp(p, 0.45f, 0.78f)) * (1f - smoothstep(ramp(p, 0.88f, 1f)))
        )
    }
}
