/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.model.CollapseMotion
import com.agupta07505.smartisland.model.NotchTopEdge
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TwoShellSwap

/** Draws the outgoing shell while the incoming shell expands from the cutout. */
@Composable
internal fun SwapGhostShell(
    ui: IslandInteractionState,
    radius: (open: Float) -> Dp,
    /** 0 = flat top (notch), 1 = round, for the ghost shell [open] (1 = full card, 0 = on the cutout). */
    topRoundness: (open: Float) -> Float,
    /** Edge pill with a flat camera side: the side the camera is on (true = left), null otherwise. */
    edgeSide: Boolean? = null,
    color: () -> Color,
    opacity: () -> Float,
    /** Width spring of the collapse (the edge pill's width is its thickness and settles without overshoot). */
    widthDamping: Float = CollapseMotion.WIDTH_DAMPING
) {
    val origin = remember { floatArrayOf(0f, 0f) }
    // 1 path for the card's clip, reused every frame (no allocation while the swap runs).
    val clip = remember { Path() }
    // Real shell's footprint, cut out of the ghost's fill while the island is translucent
    val realPath = remember { Path() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { c -> c.positionInWindow().let { origin[0] = it.x; origin[1] = it.y } }
            .drawBehind {
                if (!ui.tapSwapActive) return@drawBehind
                val shell = ui.tapSwapShellRect ?: return@drawBehind
                val cutout = ui.tapSwapCutoutRect ?: return@drawBehind
                // Not drawn until the outgoing card is parked (the real shell still is the one full-size island)
                val layer = ui.tapSwapOutgoingLayer ?: return@drawBehind
                val p = ui.tapSwapP()
                val back = ui.tapSwapBack
                val alpha = TwoShellSwap.ghostAlphaFor(back, p, opacity()) * opacity()
                if (alpha <= 0f) return@drawBehind
                val open = TwoShellSwap.ghostOpenFor(back, p)
                // Width and height answer on their own, as the plain collapse's do: the height (and the corner radius and
                // top roundness, which follow it) settle without overshoot, the width goes ahead and dips a little
                val openW = TwoShellSwap.widthOpen(!back, open, widthDamping)
                fun mix(a: Float, b: Float) = a + (b - a) * open
                fun mixW(a: Float, b: Float) = a + (b - a) * openW
                val restW = TapSwap.restExtent(cutout.width, shell.width)
                val restH = TapSwap.restExtent(cutout.height, shell.height)
                val left = mixW(cutout.center.x - restW / 2f, shell.left) - origin[0]
                val top = mix(cutout.center.y - restH / 2f, shell.top) - origin[1]
                val w = mixW(restW, shell.width)
                val h = mix(restH, shell.height)
                if (w <= 0f || h <= 0f) return@drawBehind
                val r = radius(open).toPx().coerceAtLeast(0f).coerceAtMost(minOf(w, h) / 2f)
                // Notch look: the top (or the edge pill's camera side) is square on the cutout and rounds as the ghost
                // opens
                // The top edge eases to flat with the slower axis, as the plain collapse's does
                val slow = maxOf(open, openW)
                val shape = if (edgeSide != null) {
                    islandShellSideShape(r, edgeSide, NotchTopEdge.swapFlat(slow))
                } else islandShellShape(r, topRoundness(slow))
                val outline = shape.createOutline(Size(w, h), layoutDirection, this)
                val ghostColor = color().copy(alpha = alpha.coerceIn(0f, 1f))
                if (opacity() < 0.999f) {
                    // Translucent island: the ghost sits under the real (incoming) shell and two stacked translucent
                    // layers read as 1 opaque layer that vanishes when the swap settles. Leave the real shell's
                    // footprint out of the ghost's fill so each pixel is drawn once, at the island's own opacity
                    val realOpen = TwoShellSwap.realOpen(true, back, p)
                    val realOpenW = TwoShellSwap.widthOpen(back, realOpen, widthDamping)
                    fun mixReal(a: Float, b: Float) = a + (b - a) * realOpen
                    fun mixRealW(a: Float, b: Float) = a + (b - a) * realOpenW
                    val rw = mixRealW(restW, shell.width)
                    val rh = mixReal(restH, shell.height)
                    val rl = mixRealW(cutout.center.x - restW / 2f, shell.left) - origin[0]
                    val rt = mixReal(cutout.center.y - restH / 2f, shell.top) - origin[1]
                    if (rw > 0f && rh > 0f) {
                        val rr = radius(realOpen).toPx().coerceAtLeast(0f).coerceAtMost(minOf(rw, rh) / 2f)
                        val realSlow = maxOf(realOpen, realOpenW)
                        val realShape = if (edgeSide != null) {
                            islandShellSideShape(rr, edgeSide, NotchTopEdge.swapFlat(realSlow))
                        } else islandShellShape(rr, topRoundness(realSlow))
                        realPath.rewind()
                        when (val o = realShape.createOutline(Size(rw, rh), layoutDirection, this)) {
                            is Outline.Generic -> realPath.addPath(o.path)
                            is Outline.Rounded -> realPath.addRoundRect(o.roundRect)
                            is Outline.Rectangle -> realPath.addRect(o.rect)
                        }
                        realPath.translate(Offset(rl, rt))
                        clipPath(realPath, ClipOp.Difference) {
                            withTransform({ translate(left, top) }) { drawOutline(outline, ghostColor) }
                        }
                    } else {
                        withTransform({ translate(left, top) }) { drawOutline(outline, ghostColor) }
                    }
                } else {
                    withTransform({ translate(left, top) }) { drawOutline(outline, ghostColor) }
                }
                // Outgoing card, carried by the shell: shrinks with it and is gone before the shell is small
                val contentAlpha = TwoShellSwap.ghostContentAlpha(back, p, ui.tapSwapHandoffP)
                if (contentAlpha > 0f && layer.size.width > 0 && layer.size.height > 0) {
                    val sx = w / shell.width
                    val sy = h / shell.height
                    // The recorded card is not touched: the blur is an effect on its layer (1 cached object per
                    // level, set only when the level changes), so only the radius changes per frame
                    layer.alpha = contentAlpha
                    layer.setSwapBlur(ui.swapBlur.effectFor(contentAlpha, density))
                    // Kept inside the shell's own shape, the card is a plain rectangle and its corners must not show
                    val clipPath = when (outline) {
                        is Outline.Generic -> outline.path
                        is Outline.Rounded -> clip.apply { rewind(); addRoundRect(outline.roundRect) }
                        is Outline.Rectangle -> clip.apply { rewind(); addRect(outline.rect) }
                    }
                    withTransform({
                        translate(left, top)
                        clipPath(clipPath)
                    }) {
                        withTransform({
                            translate((w - layer.size.width * sx) / 2f, 0f)
                            scale(sx, sy, pivot = Offset.Zero)
                        }) { drawLayer(layer) }
                    }
                }
            }
    )
}

internal fun GraphicsLayer.resetSwapLook() {
    if (alpha != 1f) alpha = 1f
    setSwapBlur(null)
}

/** Openness of the real shell while a cutout tap swap runs (1 = full card, 0 = on the cutout), null when none runs. */
internal fun IslandInteractionState.swapRealOpenOrNull(expanded: Boolean): Float? =
    if (tapSwapActive && expanded) {
        TwoShellSwap.realOpen(tapSwapOutgoingLayer != null, tapSwapBack, tapSwapP())
    } else null

/**
 * The real shell's open amount on the slower of its two axes while a swap runs (null when none): what the top edge's
 * roundness follows, as the plain collapse's does. The corner radius follows [swapRealOpenOrNull] (the height).
 */
internal fun IslandInteractionState.swapRealSlowOpenOrNull(expanded: Boolean, widthDamping: Float): Float? =
    swapRealOpenOrNull(expanded)?.let { h -> maxOf(h, TwoShellSwap.widthOpen(tapSwapBack, h, widthDamping)) }
