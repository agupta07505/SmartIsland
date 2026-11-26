/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.agupta07505.smartisland.model.TransferGlyphMotion
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.util.SystemAnimScale

/** Collapsed-island glyph of a copy / delete / install card. */
@Composable
internal fun TransferKindGlyph(kind: TransferKind, color: Color, modifier: Modifier = Modifier) {
    val period = TransferGlyphMotion.periodMs(kind)
    if (period <= 0) return
    val transition = rememberInfiniteTransition(label = "transferKindGlyph")
    val loop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = period, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "transferKindGlyphPhase"
    )
    val phase = if (SystemAnimScale.animationsOff()) TransferGlyphMotion.restPhase(kind) else loop
    Canvas(modifier = modifier) {
        when (kind) {
            TransferKind.COPY_MOVE -> drawCopyGlyph(color, TransferGlyphMotion.copy(phase))
            TransferKind.DELETE -> drawDeleteGlyph(color, TransferGlyphMotion.delete(phase))
            TransferKind.INSTALL -> drawInstallGlyph(color, TransferGlyphMotion.install(phase))
            else -> Unit
        }
    }
}

private fun DrawScope.glyphStroke(): Stroke =
    Stroke(width = size.minDimension * 0.115f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawCopyGlyph(color: Color, f: TransferGlyphMotion.CopyFrame) {
    val s = size.minDimension
    val stroke = glyphStroke()
    val pageSize = Size(s * TransferGlyphMotion.COPY_PAGE_W, s * TransferGlyphMotion.COPY_PAGE_H)
    val corner = CornerRadius(s * 0.09f)

    // The first page stays put, a little dimmer
    drawRoundRect(
        color = color.copy(alpha = 0.55f),
        topLeft = Offset(s * TransferGlyphMotion.COPY_SOURCE_X, s * TransferGlyphMotion.COPY_SOURCE_Y),
        size = pageSize,
        cornerRadius = corner,
        style = stroke
    )
    if (f.alpha <= 0.001f) return

    val topLeft = Offset(s * f.x, s * f.y)
    val center = Offset(topLeft.x + pageSize.width / 2f, topLeft.y + pageSize.height / 2f)
    scale(scale = f.scale, pivot = center) {
        if (f.fill > 0.001f) {
            drawRoundRect(color = color.copy(alpha = f.fill * f.alpha), topLeft = topLeft, size = pageSize,
                cornerRadius = corner)
        }
        drawRoundRect(color = color.copy(alpha = f.alpha), topLeft = topLeft, size = pageSize, cornerRadius = corner,
            style = stroke)
        val lineColor = color.copy(alpha = f.alpha * 0.8f)
        drawLine(
            color = lineColor,
            start = Offset(topLeft.x + pageSize.width * 0.25f, topLeft.y + pageSize.height * 0.38f),
            end = Offset(topLeft.x + pageSize.width * 0.75f, topLeft.y + pageSize.height * 0.38f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
        drawLine(
            color = lineColor,
            start = Offset(topLeft.x + pageSize.width * 0.25f, topLeft.y + pageSize.height * 0.62f),
            end = Offset(topLeft.x + pageSize.width * 0.60f, topLeft.y + pageSize.height * 0.62f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawDeleteGlyph(color: Color, f: TransferGlyphMotion.DeleteFrame) {
    val s = size.minDimension
    val stroke = glyphStroke()

    // Sweep lines on the left, like the DeleteSweep icon
    drawLine(
        color = color.copy(alpha = f.sweepAlphaTop),
        start = Offset(s * (0.04f + f.sweepShiftTop), s * 0.52f),
        end = Offset(s * (0.28f + f.sweepShiftTop), s * 0.52f),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color.copy(alpha = f.sweepAlphaBottom),
        start = Offset(s * (0.04f + f.sweepShiftBottom), s * 0.72f),
        end = Offset(s * (0.22f + f.sweepShiftBottom), s * 0.72f),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round
    )

    // The page that gets thrown away
    if (f.pageAlpha > 0.001f) {
        drawRoundRect(
            color = color.copy(alpha = f.pageAlpha * 0.9f),
            topLeft = Offset(s * 0.50f, s * f.pageY),
            size = Size(s * 0.16f, s * 0.22f),
            cornerRadius = CornerRadius(s * 0.04f)
        )
    }

    // Bin body (open at the top, the lid closes it)
    val body = Path().apply {
        moveTo(s * 0.44f, s * 0.40f)
        lineTo(s * 0.48f, s * 0.90f)
        lineTo(s * 0.82f, s * 0.90f)
        lineTo(s * 0.86f, s * 0.40f)
    }
    drawPath(path = body, color = color, style = stroke)

    rotate(degrees = f.lidDegrees, pivot = Offset(s * 0.90f, s * 0.28f)) {
        drawLine(
            color = color,
            start = Offset(s * 0.38f, s * 0.28f),
            end = Offset(s * 0.90f, s * 0.28f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
        val handle = Path().apply {
            moveTo(s * 0.58f, s * 0.28f)
            lineTo(s * 0.58f, s * 0.18f)
            lineTo(s * 0.72f, s * 0.18f)
            lineTo(s * 0.72f, s * 0.28f)
        }
        drawPath(path = handle, color = color, style = stroke)
    }
}

private fun DrawScope.drawInstallGlyph(color: Color, f: TransferGlyphMotion.InstallFrame) {
    val s = size.minDimension
    val stroke = glyphStroke()
    val phoneCorner = CornerRadius(s * 0.10f)

    run {
        // the phone, like the InstallMobile icon
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.27f, s * 0.06f),
            size = Size(s * 0.46f, s * 0.88f),
            cornerRadius = phoneCorner,
            style = stroke
        )

        val screen = Path().apply {
            addRoundRect(RoundRect(s * 0.31f, s * 0.10f, s * 0.69f, s * 0.90f, s * 0.07f, s * 0.07f))
        }
        clipPath(screen) {
            // the screen fills from the bottom as the app lands
            if (f.fill > 0.001f) {
                val top = s * 0.90f - s * 0.80f * f.fill
                drawRect(color = color.copy(alpha = 0.30f), topLeft = Offset(s * 0.31f, top),
                    size = Size(s * 0.38f, s * 0.90f - top))
            }
            if (f.arrowAlpha > 0.001f) {
                translate(top = s * f.arrowDy) {
                    val arrowColor = color.copy(alpha = f.arrowAlpha)
                    drawLine(
                        color = arrowColor,
                        start = Offset(s * 0.5f, s * 0.34f),
                        end = Offset(s * 0.5f, s * 0.64f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                    val head = Path().apply {
                        moveTo(s * 0.38f, s * 0.54f)
                        lineTo(s * 0.5f, s * 0.66f)
                        lineTo(s * 0.62f, s * 0.54f)
                    }
                    drawPath(path = head, color = arrowColor, style = stroke)
                }
            }
        }
    }
}
