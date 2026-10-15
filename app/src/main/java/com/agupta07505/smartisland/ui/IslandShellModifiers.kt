/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * The island shell's size, top inset, corner radius, shadow and surface read their animated values
 * INSIDE the layout / draw phase (these lambdas are state-observed there), not in composition. While the island grows or
 * shrinks the springs then only re-run layout and draw; IslandOverlayView and the card content are not recomposed for every
 * frame of the size animation. Every function keeps the maths of the modifier it replaces.
 */

/** The island shape: all four corners `radiusPx`, or (notch mode, collapsed) square top corners. Same result as the old `mainShape`. */
internal fun islandShellShape(radiusPx: Float, squareTop: Boolean): RoundedCornerShape {
    val r = CornerSize(radiusPx.coerceAtLeast(0f))
    return if (squareTop) {
        RoundedCornerShape(topStart = ZeroCornerSize, topEnd = ZeroCornerSize, bottomEnd = r, bottomStart = r)
    } else {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = r)
    }
}

/** Like `padding(top = ...)`, but the inset is read while measuring. [topPx] returns the inset in pixels (negative counts as 0). */
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

/** Like `width(w).height(h)`, but both are read while measuring, so the animation does not recompose. */
internal fun Modifier.islandAnimatedSize(width: () -> Dp, height: () -> Dp): Modifier = layout { measurable, constraints ->
    val w = width().roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
    val h = height().roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Like `clip(shape)`: a new shape per run of the layer block, so a radius change always re-resolves the outline. */
internal fun Modifier.islandShellClip(radius: () -> Dp, squareTop: Boolean): Modifier = graphicsLayer {
    shape = islandShellShape(radius().toPx(), squareTop)
    clip = true
}

/** Like `shadow(elevation, shape, clip = false, ambientColor = Black, spotColor = Black)`. */
internal fun Modifier.islandShellShadow(
    radius: () -> Dp,
    squareTop: Boolean,
    elevation: Dp,
    /** 0..1, read where it is drawn: the shadow follows the surface while that fades in (otherwise a bare shadow shows as a dark box). */
    strength: () -> Float = { 1f }
): Modifier = graphicsLayer {
    shadowElevation = elevation.toPx() * strength().coerceIn(0f, 1f)
    shape = islandShellShape(radius().toPx(), squareTop)
    clip = false
    ambientShadowColor = Color.Black
    spotShadowColor = Color.Black
}

/**
 * Like `background(brush or colour, shape, alpha)`: the outline comes from the same shape as the clip, built in the draw block.
 * With a [brush] the island is filled with it at [alpha]; otherwise with [color] at [alpha].
 */
internal fun Modifier.islandShellSurface(
    radius: () -> Dp,
    squareTop: Boolean,
    brush: Brush?,
    color: Color,
    alpha: () -> Float,
    /**
     * The light look: the colour the island has when fully open. The shell eases from [color] (the pill) to it by [expandedFraction]
     * (0 = pill, 1 = open, read while drawing), so a light card grows out of a black pill instead of flashing white. Null = no change.
     */
    expandedColor: Color? = null,
    expandedFraction: () -> Float = { 1f }
): Modifier = drawBehind {
    val a = alpha()
    val r = radius().toPx().coerceAtLeast(0f)
    val fraction = if (expandedColor != null) expandedFraction().coerceIn(0f, 1f) else 1f
    val base = if (expandedColor != null) androidx.compose.ui.graphics.lerp(color, expandedColor, fraction) else color
    // With a gradient the brush is drawn over the eased base colour and fades in with the expansion, so the open light island has no pop.
    val brushAlpha = if (expandedColor != null) a * fraction else a
    if (brush != null && expandedColor != null) {
        if (!squareTop) drawRoundRect(base.copy(alpha = a), size = size, cornerRadius = CornerRadius(r, r))
        else drawSquareTopShell(r, base.copy(alpha = a), null, a)
    }
    if (!squareTop) {
        // The normal shell is a plain rounded rectangle, so it is drawn directly. No Outline or Path is built
        // on each frame of the size animation (no allocation per frame); the radius is clamped to half the short side by the canvas.
        val corner = CornerRadius(r, r)
        if (brush != null) drawRoundRect(brush, size = size, cornerRadius = corner, alpha = brushAlpha)
        else drawRoundRect(base.copy(alpha = a), size = size, cornerRadius = corner)
    } else {
        // Notch mode, collapsed: square top corners. One reused path (rewound each frame) instead of a new one.
        drawSquareTopShell(r, base.copy(alpha = a), brush, brushAlpha)
    }
}

/** The notch-mode shell: [brush] at [brushAlpha] when given, else [fill]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSquareTopShell(r: Float, fill: Color, brush: Brush?, brushAlpha: Float) {
    val path = squareTopPath.getOrSet { Path() }
    path.rewind()
    path.addRoundRect(
        RoundRect(
            rect = Rect(Offset.Zero, size),
            topLeft = CornerRadius.Zero, topRight = CornerRadius.Zero,
            bottomRight = CornerRadius(r, r), bottomLeft = CornerRadius(r, r)
        )
    )
    if (brush != null) drawPath(path, brush, alpha = brushAlpha) else drawPath(path, fill)
}

/** The notch-mode shell path, kept per thread (drawing runs on one thread) so each frame only rewinds it. */
private val squareTopPath = ThreadLocal<Path>()

private inline fun <T : Any> ThreadLocal<T>.getOrSet(create: () -> T): T = get() ?: create().also { set(it) }

/**
 * The thin outline of the swipe-lock cue, drawn over the content in the shape [shape]. [cue] (0..1) is read at draw time,
 * so the fade never recomposes anything. Half of the stroke falls outside a clipped shell, so it is drawn twice as wide.
 */
internal fun Modifier.lockCueOutline(shape: Density.() -> Shape, cue: () -> Float, color: Color = Color.White): Modifier = drawWithContent {
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
