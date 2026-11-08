/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.EdgeCamera
import com.agupta07505.smartisland.model.EdgeFadeSpan
import com.agupta07505.smartisland.model.EdgePillMotion
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.EdgeRect

internal const val EDGE_CLEAR_LENS = false

internal const val OPAQUE = 0.999f

internal fun edgePillShapeFor(camera: EdgeCamera): Shape {
    val t = camera.tuning
    val r = t.cornerRadiusDp.dp
    return when {
        !EdgePillRules.flatCameraSide(t.flatEdge, t.xOffsetDp) -> RoundedCornerShape(r)
        camera.onLeft -> AbsoluteRoundedCornerShape(topLeft = 0.dp, topRight = r, bottomRight = r, bottomLeft = 0.dp)
        else -> AbsoluteRoundedCornerShape(topLeft = r, topRight = 0.dp, bottomRight = 0.dp, bottomLeft = r)
    }
}

/**
 * Fades content out toward the camera hole like portrait's side camera: hidden up to the hole's far edge, full where
 * glyphs rest
 */
internal fun Modifier.edgeGlyphFade(
    span: EdgeFadeSpan?,
    motion: () -> EdgePillMotion,
    mergeScaleY: () -> Float
): Modifier =
    if (span == null) this else drawWithContent {
        val m = motion()
        val scale = (m.scaleY * mergeScaleY()).coerceAtLeast(0.01f)
        val h = size.height
        fun local(fromEndDp: Float): Float {
            val y = if (span.fromBottom) h - fromEndDp * density else fromEndDp * density
            return (EdgePillRules.holeLocalY(y, h, m.translationY, scale) / h).coerceIn(0f, 1f)
        }
        val clearAt = local(span.clearToDp)
        val solidAt = local(span.solidFromDp).let { if (span.fromBottom) minOf(it, clearAt) else maxOf(it, clearAt) }
        val clear = Color.Transparent
        val solid = Color.Black
        val mask = if (span.fromBottom) {
            Brush.verticalGradient(0f to solid, solidAt to solid, clearAt to clear, 1f to clear, startY = 0f, endY = h)
        } else {
            Brush.verticalGradient(0f to clear, clearAt to clear, solidAt to solid, 1f to solid, startY = 0f, endY = h)
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, h), Paint())
            this@drawWithContent.drawContent()
            drawRect(brush = mask, blendMode = BlendMode.DstIn)
            canvas.restore()
        }
    }

/**
 * Pill fill with the camera hole (an oval in resting coordinates) left clear, lean and scale are undone so the hole
 * stays put
 */
internal fun Modifier.edgePillFill(
    color: Color,
    enabled: Boolean,
    holeCentreXInPillDp: Float,
    holeCentreYInPillDp: Float,
    holeWidthDp: Float,
    holeLengthDp: Float,
    motion: () -> EdgePillMotion,
    mergeScaleX: () -> Float,
    mergeScaleY: () -> Float
): Modifier = drawBehind {
    if (color.alpha <= 0f) return@drawBehind
    if (!enabled) {
        drawRect(color)
        return@drawBehind
    }
    val m = motion()
    val sx = (m.scaleX * mergeScaleX()).coerceAtLeast(0.01f)
    val sy = (m.scaleY * mergeScaleY()).coerceAtLeast(0.01f)
    val cx = EdgePillRules.holeLocalX(holeCentreXInPillDp * density, size.width, m.originX, m.translationX, sx)
    val cy = EdgePillRules.holeLocalY(holeCentreYInPillDp * density, size.height, m.translationY, sy)
    val halfW = holeWidthDp * density / 2f / sx
    val halfH = holeLengthDp * density / 2f / sy
    val hole = Path().apply { addOval(Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)) }
    clipPath(hole, clipOp = ClipOp.Difference) { drawRect(color) }
}

internal fun Modifier.edgePillCutout(enabled: Boolean, pill: EdgeRect?, shape: Shape?): Modifier =
    if (!enabled || pill == null || shape == null) this else drawWithCache {
        val outline = shape.createOutline(Size(pill.width * density, pill.height * density), layoutDirection, this)
        val path = Path()
        when (outline) {
            is Outline.Rectangle -> path.addRect(outline.rect)
            is Outline.Rounded -> path.addRoundRect(outline.roundRect)
            is Outline.Generic -> path.addPath(outline.path)
        }
        path.translate(Offset(pill.left * density, pill.top * density))
        onDrawWithContent {
            clipPath(path, clipOp = ClipOp.Difference) { this@onDrawWithContent.drawContent() }
        }
    }

internal fun Modifier.edgeLensClear(camera: EdgeCamera?): Modifier =
    if (!EDGE_CLEAR_LENS || camera == null || !camera.cutoutReported
        || camera.holeLengthDp <= 0f) this else drawWithContent {
        val width = EdgePillRules.holeClearWidthDp(camera.holeWidthDp, camera.holeLengthDp) * density
        val length = camera.holeLengthDp * density
        val cx = (if (camera.onLeft) camera.centerFromEdgeDp else camera.screenWidthDp - camera.centerFromEdgeDp) * density
        val cy = camera.holeCentreYDp * density
        val hole = Path().apply {
            addOval(Rect(cx - width / 2f, cy - length / 2f, cx + width / 2f, cy + length / 2f))
        }
        clipPath(hole, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
    }
