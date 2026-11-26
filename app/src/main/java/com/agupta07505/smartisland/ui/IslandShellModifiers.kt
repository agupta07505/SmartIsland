/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Island shape: all four corners `radiusPx`, or square on top when [squareTop]. */
internal fun islandShellShape(radiusPx: Float, squareTop: Boolean): RoundedCornerShape =
    islandShellShape(radiusPx, if (squareTop) 0f else 1f)

internal fun islandShellShape(radiusPx: Float, topRoundness: Float): RoundedCornerShape {
    val r = CornerSize(radiusPx.coerceAtLeast(0f))
    val top = radiusPx.coerceAtLeast(0f) * topRoundness.coerceIn(0f, 1f)
    return if (top <= 0f) {
        RoundedCornerShape(topStart = ZeroCornerSize, topEnd = ZeroCornerSize, bottomEnd = r, bottomStart = r)
    } else {
        val t = CornerSize(top)
        RoundedCornerShape(topStart = t, topEnd = t, bottomEnd = r, bottomStart = r)
    }
}

internal fun Modifier.islandTopInset(topPx: Density.() -> Int): Modifier = layout { measurable, constraints ->
    val top = topPx(this).coerceAtLeast(0)
    val minH = (constraints.minHeight - top).coerceAtLeast(0)
    val maxH = if (constraints.maxHeight == Constraints.Infinity) constraints.maxHeight
    else (constraints.maxHeight - top).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minHeight = minH, maxHeight = maxH))
    val width = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = (placeable.height + top).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) { placeable.place(0, top) }
}

/** Like `width(w).height(h)`, but both are read while measuring. */
internal fun Modifier.islandAnimatedSize(width: () -> Dp, height: () ->
        Dp): Modifier = layout { measurable, constraints ->
    val w = width().roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
    val h = height().roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

internal class ShellSide(val onLeft: Boolean, val flat: () -> Float)

internal fun islandShellSideShape(radiusPx: Float, onLeft: Boolean, flat: Float): AbsoluteRoundedCornerShape {
    val round = CornerSize(radiusPx.coerceAtLeast(0f))
    val camera = CornerSize(radiusPx.coerceAtLeast(0f) * (1f - flat.coerceIn(0f, 1f)))
    return if (onLeft) {
        AbsoluteRoundedCornerShape(topLeft = camera, topRight = round, bottomRight = round, bottomLeft = camera)
    } else {
        AbsoluteRoundedCornerShape(topLeft = round, topRight = camera, bottomRight = camera, bottomLeft = round)
    }
}

/**
 * 1 corner radius (px, in the scaled shell's own screen space) capped by the SHORTER screen side, like the ghost
 * shell's `min(w, h) / 2`. Capping each axis alone would turn a short pill's corners into ellipses
 */
internal fun squeezedCorner(v: Float, size: Size, sx: Float, sy: Float): CornerRadius {
    val cap = minOf(size.width * sx, size.height * sy) / 2f
    val c = v.coerceAtMost(cap).coerceAtLeast(0f)
    return CornerRadius(c / sx, c / sy)
}

internal val NoSqueeze: () -> Offset = { Offset(1f, 1f) }

/**
 * Shell's own (x, y) scale during a tap swap. The shell is scaled as 1 layer so its corners would scale along: each
 * corner is drawn as an ellipse of radius / scale, which scales back to a true circle on screen
 */
private class SqueezedShellShape(val radiusPx: Float, val topRoundness: Float, val squeeze: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = radiusPx.coerceAtLeast(0f)
        val top = r * topRoundness.coerceIn(0f, 1f)
        val sx = squeeze.x.coerceAtLeast(0.001f)
        val sy = squeeze.y.coerceAtLeast(0.001f)
        fun corner(v: Float) = squeezedCorner(v, size, sx, sy)
        return Outline.Rounded(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                topLeft = corner(top), topRight = corner(top),
                bottomRight = corner(r), bottomLeft = corner(r)
            )
        )
    }
}

private fun shellShape(radiusPx: Float, topRoundness: Float, side: ShellSide?,
        squeeze: Offset = Offset(1f, 1f)): Shape = when {
    side != null -> islandShellSideShape(radiusPx, side.onLeft, side.flat())
    squeeze.x != 1f || squeeze.y != 1f -> SqueezedShellShape(radiusPx, topRoundness, squeeze)
    else -> islandShellShape(radiusPx, topRoundness)
}

/** Like `clip(shape)`: a new shape per run of the layer block. */
internal fun Modifier.islandShellClip(
    radius: () -> Dp, topRoundness: () -> Float, side: ShellSide? = null, squeeze: () -> Offset = NoSqueeze
): Modifier = graphicsLayer {
    shape = shellShape(radius().toPx(), topRoundness(), side, squeeze())
    clip = true
}

internal fun Modifier.islandShellShadow(
    radius: () -> Dp,
    topRoundness: () -> Float,
    elevation: Dp,
    /** 0..1, read where it is drawn */
    strength: () -> Float = { 1f },
    side: ShellSide? = null,
    squeeze: () -> Offset = NoSqueeze
): Modifier = graphicsLayer {
    shadowElevation = elevation.toPx() * strength().coerceIn(0f, 1f)
    shape = shellShape(radius().toPx(), topRoundness(), side, squeeze())
    clip = false
    ambientShadowColor = Color.Black
    spotShadowColor = Color.Black
}

/** Like `background(brush or colour, shape, alpha)` */
internal fun Modifier.islandShellSurface(
    radius: () -> Dp,
    topRoundness: () -> Float,
    brush: Brush?,
    color: Color,
    alpha: () -> Float,
    /** Light look: the colour the island has when fully open. */
    expandedColor: Color? = null,
    expandedFraction: () -> Float = { 1f },
    side: ShellSide? = null,
    squeeze: () -> Offset = NoSqueeze
): Modifier = drawBehind {
    val a = alpha()
    val r = radius().toPx().coerceAtLeast(0f)
    val sq = squeeze()
    val squeezedR = squeezedCorner(r, size, sq.x.coerceAtLeast(0.001f), sq.y.coerceAtLeast(0.001f))
    val rx = squeezedR.x
    val ry = squeezedR.y
    val topR = r * topRoundness().coerceIn(0f, 1f)
    val roundTop = topR >= r
    val fraction = if (expandedColor != null) expandedFraction().coerceIn(0f, 1f) else 1f
    val base = if (expandedColor != null) lerp(color, expandedColor, fraction) else color
    val brushAlpha = if (expandedColor != null) a * fraction else a
    val sidePath = if (side != null) {
        val path = squareTopPath.getOrSet { Path() }
        path.rewind()
        when (val o = islandShellSideShape(r, side.onLeft, side.flat()).createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> path.addRect(o.rect)
            is Outline.Rounded -> path.addRoundRect(o.roundRect)
            is Outline.Generic -> path.addPath(o.path)
        }
        path
    } else null
    fun paint(fill: Color, shellBrush: Brush?, shellBrushAlpha: Float) {
        when {
            sidePath != null ->
                if (shellBrush != null) drawPath(sidePath, shellBrush, alpha = shellBrushAlpha) else drawPath(sidePath,
                    fill)
            roundTop -> {
                val corner = CornerRadius(rx, ry)
                if (shellBrush != null) drawRoundRect(shellBrush, size = size, cornerRadius = corner,
                    alpha = shellBrushAlpha)
                else drawRoundRect(fill, size = size, cornerRadius = corner)
            }
            else -> drawEasedTopShell(topR, r, sq, fill, shellBrush, shellBrushAlpha)
        }
    }
    if (brush != null && expandedColor != null) paint(base.copy(alpha = a), null, a)
    paint(base.copy(alpha = a), brush, brushAlpha)
}

private fun DrawScope.drawEasedTopShell(topR: Float, r: Float, sq: Offset, fill: Color, brush: Brush?,
        brushAlpha: Float) {
    val sx = sq.x.coerceAtLeast(0.001f)
    val sy = sq.y.coerceAtLeast(0.001f)
    fun c(v: Float) = squeezedCorner(v, size, sx, sy)
    val path = squareTopPath.getOrSet { Path() }
    path.rewind()
    path.addRoundRect(
        RoundRect(
            rect = Rect(Offset.Zero, size),
            topLeft = c(topR), topRight = c(topR),
            bottomRight = c(r), bottomLeft = c(r)
        )
    )
    if (brush != null) drawPath(path, brush, alpha = brushAlpha) else drawPath(path, fill)
}

private val squareTopPath = ThreadLocal<Path>()

private inline fun <T : Any> ThreadLocal<T>.getOrSet(create: () -> T): T = get() ?: create().also { set(it) }

/** A glow along the shell's boundary, drawn inside the clip as a few soft strokes that fade inward. */
internal fun Modifier.holdGlowOutline(shape: Density.() -> Shape, glow: () -> Float,
        color: Color = Color.White): Modifier = drawWithContent {
    drawContent()
    val g = glow()
    if (g > 0.01f) {
        val outline = shape().createOutline(size, layoutDirection, this)
        val path = Path()
        when (outline) {
            is Outline.Rectangle -> path.addRect(outline.rect)
            is Outline.Rounded -> path.addRoundRect(outline.roundRect)
            is Outline.Generic -> path.addPath(outline.path)
        }
        val widthPx = HOLD_GLOW_WIDTH_DP.dp.toPx()
        // The clip keeps the inner half of each stroke: wide and faint first, thin and brighter last
        for (i in 3 downTo 1) {
            drawPath(
                path = path,
                color = color.copy(alpha = HOLD_GLOW_ALPHA * g / i),
                style = Stroke(width = widthPx * i / 3f * 2f)
            )
        }
    }
}

internal fun Modifier.lockCueOutline(shape: Density.() -> Shape, cue: () -> Float,
        color: Color = Color.White): Modifier = drawWithContent {
    drawContent()
    val c = cue()
    if (c > 0.01f) {
        val cueOutline = shape().createOutline(size, layoutDirection, this)
        val cuePath = Path()
        when (cueOutline) {
            is Outline.Rectangle -> cuePath.addRect(cueOutline.rect)
            is Outline.Rounded -> cuePath.addRoundRect(cueOutline.roundRect)
            is Outline.Generic -> cuePath.addPath(cueOutline.path)
        }
        drawPath(
            path = cuePath,
            color = color.copy(alpha = LOCK_CUE_OUTLINE_ALPHA * c),
            style = Stroke(width = LOCK_CUE_OUTLINE_DP.dp.toPx() * 2f)
        )
    }
}
