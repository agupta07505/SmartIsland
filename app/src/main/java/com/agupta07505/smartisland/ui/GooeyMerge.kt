/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Gooey join for the bubble merge style (Android 12+, where RenderEffect exists). The island is a solid colour, so
 * the classic trick works: draw the bubble and the pill in one layer, blur the layer, then push the alpha through a
 * steep threshold. Shapes that come close pull a liquid bridge between them and the blurred corners fillet into the
 * join. The real pill and bubble stay drawn on top, crisp; this layer only adds the bridge behind them.
 *
 * Only used at full opacity (a see-through island would show the layer twice); older Android versions and see-through
 * islands keep the drawn neck. Maths of the shapes: MergeBubble.kt ([gooeyShapes]).
 */
internal fun gooeySupported(opacity: Float): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && opacity >= 0.99f

/**
 * The blur + threshold effect for a blur radius of [blurPx], in the pixels of the layer it is set on (so already
 * multiplied by [gooeyLayerScale] when the layer is recorded smaller).
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
internal fun gooeyRenderEffect(blurPx: Float): RenderEffect {
    val radius = blurPx.coerceAtLeast(1f)
    val blur = android.graphics.RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL)
    return android.graphics.RenderEffect
        .createColorFilterEffect(GOO_THRESHOLD_FILTER, blur)
        .asComposeRenderEffect()
}

private val GOO_THRESHOLD_FILTER: ColorMatrixColorFilter by lazy(LazyThreadSafetyMode.PUBLICATION) {
    ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, GOO_ALPHA_GAIN, GOO_ALPHA_OFFSET
            )
        )
    )
}

/**
 * Draws the gooey layer behind the bubble: both [shapes] in [color] as pills, blurred and thresholded by [effect].
 * [pillCornerPx] and [bubbleCornerPx] round the two shapes (a corner larger than half the short side is clamped);
 * the bridge between them is a plain rectangle.
 *
 * [blurPx] is the blur in screen pixels; it only sizes the layer's margin. The layer is recorded at [layerScale] of its
 * size (see [gooeyLayerScale]) and drawn back up, so [effect] must have been made for `blurPx * layerScale`.
 */
internal fun DrawScope.drawGooeyMerge(
    layer: GraphicsLayer,
    effect: RenderEffect,
    shapes: GooShapes,
    color: Color,
    blurPx: Float,
    pillCornerPx: Float,
    bubbleCornerPx: Float,
    layerScale: Float = 1f
) {
    val bounds = shapes.bounds(marginPx = blurPx * GOO_MARGIN_SIGMAS)
    val left = floor(bounds.left).toInt()
    val top = floor(bounds.top).toInt()
    val width = ceil(bounds.right - left).toInt().coerceAtLeast(1)
    val height = ceil(bounds.bottom - top).toInt().coerceAtLeast(1)
    val s = layerScale.coerceIn(0.1f, 1f)
    // Same effect object every frame: only touch the layer when something really changed.
    if (layer.renderEffect !== effect) layer.renderEffect = effect
    layer.topLeft = IntOffset(left, top)
    if (s < 1f) {
        layer.pivotOffset = Offset.Zero
        layer.scaleX = 1f / s
        layer.scaleY = 1f / s
    } else {
        layer.scaleX = 1f
        layer.scaleY = 1f
    }
    val layerSize = IntSize(ceil(width * s).toInt().coerceAtLeast(1), ceil(height * s).toInt().coerceAtLeast(1))
    layer.record(this, layoutDirection, layerSize) {
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            translate(left = -left.toFloat(), top = -top.toFloat()) {
                drawGooShape(shapes.bubble, color, bubbleCornerPx)
                drawGooShape(shapes.pill, color, pillCornerPx)
                shapes.neck?.let { drawGooShape(it, color, 0f) }
            }
        }
    }
    drawLayer(layer)
}

private fun DrawScope.drawGooShape(rect: GooRect, color: Color, cornerPx: Float) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val r = min(cornerPx, min(rect.width, rect.height) / 2f).coerceAtLeast(0f)
    drawRoundRect(
        color = color,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(r, r)
    )
}

/**
 * The pill with its touching end pressed flat: drawn crisp behind the real pill while the bubble pushes into it, so
 * the end that faces the bubble loses its round corners (radius [nearCornerPx] instead of [farCornerPx]).
 */
internal fun DrawScope.drawPillFlatFace(
    pill: GooRect,
    color: Color,
    toward: Float,
    nearCornerPx: Float,
    farCornerPx: Float
) {
    if (pill.width <= 0f || pill.height <= 0f) return
    val limit = min(pill.width, pill.height) / 2f
    val near = CornerRadius(min(nearCornerPx, limit).coerceAtLeast(0f))
    val far = CornerRadius(min(farCornerPx, limit).coerceAtLeast(0f))
    val right = toward > 0f
    val path = androidx.compose.ui.graphics.Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = pill.left, top = pill.top, right = pill.right, bottom = pill.bottom,
                topLeftCornerRadius = if (right) far else near,
                topRightCornerRadius = if (right) near else far,
                bottomRightCornerRadius = if (right) near else far,
                bottomLeftCornerRadius = if (right) far else near
            )
        )
    }
    drawPath(path, color)
}
