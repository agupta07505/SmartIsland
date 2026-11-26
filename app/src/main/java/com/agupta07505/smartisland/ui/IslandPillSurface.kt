/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CollapseMotion
import com.agupta07505.smartisland.model.EdgePillMotion
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.SwipeDownPull
import com.agupta07505.smartisland.model.SwipeSideways
import com.agupta07505.smartisland.model.TwoShellSwap
import com.agupta07505.smartisland.ui.expanded.IslandPalette
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush

internal fun Modifier.islandPillSurface(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    topRoundness: () -> Float,
    pickerSideBySide: Boolean,
    twoIslandsDrawn: Boolean = pickerSideBySide,
    shellOpacity: () -> Float = { settings.opacity },
    reportBounds: Boolean = true,
    shellHidden: () -> Boolean = { false },
    palette: IslandPalette = IslandPalette.Dark,
    edgeSide: ShellSide? = null,
    edgeCentreDp: ((Float) -> Float)? = null,
    /** Edge layout, the shell is the island's 1 surface. */
    edgeMotion: (() -> EdgePillMotion)? = null,
    /** Screen width, to keep a swipe's lean on screen (0 = unknown). */
    screenWidthDp: Float = 0f,
    /** Shell's corner radius, the edge pill's swap runs it down to the pill's own corner. */
    shellRadius: () -> Dp = { layout.safeRadius }
): Modifier {
    // Swap's own scale on the shell, so corners can be drawn squeezed back to true circles
    val swapSqueeze: () -> Offset = {
        if (ui.tapSwapActive && wiring.view.expanded) {
            val open = TwoShellSwap.realOpen(
                ui.tapSwapOutgoingLayer != null, ui.tapSwapBack, ui.tapSwapP()
            )
            val openW = TwoShellSwap.widthOpen(ui.tapSwapBack, open, swapWidthDamping(edgeMotion != null))
            Offset(
                TwoShellSwap.scaleAt(ui.tapSwapScaleX, openW),
                TwoShellSwap.scaleAt(ui.tapSwapScaleY, open)
            )
        } else Offset(1f, 1f)
    }
    // the shell's width while the split override sets it, else -1
    val splitWidthDp: () -> Float = {
        ui.stepDrag.splitShellWidthDp(
            splitNow = pickerSideBySide,
            expanded = wiring.view.expanded,
            single = layout.singleExpandedWidth.value,
            total = layout.expandedWidth.value,
            own = layout.safeWidth.value
        )
    }
    val layer = ShellLayer(
        settings, layout, secondary, pillExit, switchScaleAnim, wiring, ui, splitWidthDp,
        edgeCentreDp, edgeMotion, screenWidthDp, shellHidden
    )
    return this
        .islandAnimatedSize({
            val splitW = splitWidthDp()
            if (splitW >= 0f) splitW.dp else layout.safeWidth
        }, {
            val own = layout.safeHeight
            ui.stepDrag.shellHeightDp = own.value
            val dragged = ui.stepDrag.heightDp
            if (dragged >= 0f) {
                dragged.dp
            } else {
                val splitH = ui.stepDrag.splitShellHeightDp(
                    splitNow = pickerSideBySide,
                    expanded = wiring.view.expanded,
                    own = own.value
                )
                if (splitH >= 0f) splitH.dp else own
            }
        })
        .graphicsLayer { applyShellLayer(layer) }
        .then(
            if (settings.enableShadow && settings.shadowElevation > 0f && !layout.isHiding && !twoIslandsDrawn) {
                Modifier.shellShadow(
                    settings, layout, wiring, palette, edgeSide, edgeMotion != null,
                    shellRadius, topRoundness, swapSqueeze
                )
            } else Modifier
        )
        // 2 separate islands draw (and shadow) themselves
        .then(if (twoIslandsDrawn) Modifier else Modifier.islandShellClip(shellRadius, topRoundness, edgeSide,
            swapSqueeze))
        .then(
            if (wiring.view.expanded && !pickerSideBySide && reportBounds) {
                Modifier.reportShellBounds(wiring, ui)
            } else Modifier
        )
        .islandShellSurface(
            radius = shellRadius,
            topRoundness = topRoundness,
            brush = expandedGradientBrush(settings, palette)?.takeIf { wiring.view.expanded },
            color = layout.pillBackgroundColor,
            expandedColor = if (palette.isLight) palette.surface else null,
            expandedFraction = { layout.expandProgress },
            alpha = { shellOpacity() * layout.singleSurfaceAlpha },
            side = edgeSide,
            squeeze = swapSqueeze
        )
        .holdGlowOutline(
            shape = {
                if (edgeSide != null) islandShellSideShape(shellRadius().toPx(), edgeSide.onLeft, edgeSide.flat())
                else islandShellShape(shellRadius().toPx(), topRoundness())
            },
            glow = { ui.holdGlow.value },
            color = if (palette.isLight) Color.Black else Color.White
        )
        .lockCueOutline(
            shape = {
                if (edgeSide != null) islandShellSideShape(shellRadius().toPx(), edgeSide.onLeft, edgeSide.flat())
                else islandShellShape(shellRadius().toPx(), topRoundness())
            },
            cue = { ui.lockCue.value },
            // a white outline would vanish on the light island
            color = if (palette.isLight) Color.Black else Color.White
        )
}

private class ShellLayer(
    val settings: SmartIslandSettings,
    val layout: IslandLayout,
    val secondary: IslandSecondary,
    val pillExit: PillExitState,
    val switchScaleAnim: Animatable<Float, AnimationVector1D>,
    val wiring: IslandWiring,
    val ui: IslandInteractionState,
    val splitWidthDp: () -> Float,
    val edgeCentreDp: ((Float) -> Float)?,
    val edgeMotion: (() -> EdgePillMotion)?,
    val screenWidthDp: Float,
    val shellHidden: () -> Boolean
)

private fun GraphicsLayerScope.applyShellLayer(l: ShellLayer) {
    val splitW = l.splitWidthDp()
    val baseX = if (splitW >= 0f) (l.edgeCentreDp?.invoke(splitW) ?: 0f).dp.toPx() else l.layout.animatedXOffset.toPx()
    val edgeCollapsed = l.edgeMotion != null && !l.wiring.view.expanded
    val edgeNow = if (edgeCollapsed && l.layout.expandProgress <= 0.001f) l.edgeMotion?.invoke() else null
    transformOrigin = if (edgeNow != null) TransformOrigin(edgeNow.originX, 0.5f) else TransformOrigin.Center
    placeShell(l, baseX, edgeCollapsed, edgeNow)
    swellForSecondary(l)
    // a swipe that would fire if the finger lifted now
    val pullScaleY = scaleForPull(l, edgeCollapsed, edgeNow)
    // a cutout tap: 2 shells trade places (the incoming one grows out of the cutout)
    if (l.ui.tapSwapActive && l.wiring.view.expanded && !edgeCollapsed) {
        val swapP = l.ui.tapSwapP()
        // The real shell is the incoming one and grows out of the cutout (the ghost collapses into it). It stays full
        // size until the outgoing card is parked on the ghost, and once the swap is turned around it holds the outgoing
        // event again and collapses instead
        val open = TwoShellSwap.realOpen(
            l.ui.tapSwapOutgoingLayer != null, l.ui.tapSwapBack, swapP
        )
        // width answers ahead of the height (the plain collapse's stagger), the edge pill's width does not
        val openW = TwoShellSwap.widthOpen(l.ui.tapSwapBack, open, swapWidthDamping(l.edgeMotion != null))
        scaleX *= TwoShellSwap.scaleAt(l.ui.tapSwapScaleX, openW)
        scaleY *= TwoShellSwap.scaleAt(l.ui.tapSwapScaleY, open)
        translationX += TwoShellSwap.shiftAt(l.ui.tapSwapDx, openW)
        translationY += TwoShellSwap.shiftAt(l.ui.tapSwapDy, open)
    }
    // Keep the top edge in place: the stretch reaches down only
    translationY += SwipeDownPull.topAnchorShiftPx(pullScaleY, size.height)
    // Only the pill exit fades the layer
    alpha = if (l.shellHidden()) 0f else (l.pillExit.shellAlpha() * l.layout.hidingFade)
}

private fun GraphicsLayerScope.placeShell(
    l: ShellLayer,
    baseX: Float,
    edgeCollapsed: Boolean,
    edgeNow: EdgePillMotion?
) {
    val pillLeanPx = if (!l.wiring.view.expanded && !edgeCollapsed) {
        // Keep the lean on screen and the same both ways (a side camera pins the pill near the edge)
        pillLeanWithinScreenDp(
            l.ui.pillDragOffsetX / density, l.layout.collapsedMainLeft.value, l.settings.width, l.screenWidthDp
        ) * density
    } else 0f
    translationX = baseX + pillLeanPx +
        (edgeNow?.translationX ?: 0f) + l.pillExit.slideXDp().dp.toPx()
    translationY = l.layout.yOffset.toPx() + (if (edgeCollapsed) (edgeNow?.translationY ?: 0f) else l.ui.dragOffset) +
        l.pillExit.slideYDp().dp.toPx()
}

private fun GraphicsLayerScope.swellForSecondary(l: ShellLayer) {
    val secondary = l.secondary
    val layout = l.layout
    val mergeNow = secondary.merge.anim.value
    val contactNow = mergeContactFraction(
        (layout.animatedXOffset - secondary.offsets.state.value).value,
        layout.safeWidth.value,
        secondary.anims.bubbleWidth.value.value
    )
    if (!l.wiring.view.expanded && l.settings.mergeStyleKind() == MergeStyle.BUBBLE) {
        // landscape: the pill fades while it slides out and in (pillAlpha)
        val still = secondary.merge.idleCloseMerge
        scaleX = if (still) 1f else mergePillScaleX(mergeNow, contactNow, layout.safeWidth.value)
        scaleY = l.switchScaleAnim.value * (if (still) 1f else mergePillScaleY(mergeNow, contactNow,
            layout.safeHeight.value))
    } else {
        val swell = if (secondary.merge.idleCloseMerge) 1f
            else 1f + 0.07f * mergeSwell(mergeNow, contactNow)
        scaleX = swell
        scaleY = l.switchScaleAnim.value * swell
    }
}

/** Scale of a swipe pull, dead zone, floating-window handle and card lean, returns the pull's vertical scale. */
private fun GraphicsLayerScope.scaleForPull(
    l: ShellLayer,
    edgeCollapsed: Boolean,
    edgeNow: EdgePillMotion?
): Float {
    val ui = l.ui
    val pull = ui.swipeDownPull
    val deadWeight = SwipeDownPull.deadShrinkWeight(pull)
    val pullScaleY: Float
    if (edgeCollapsed) {
        // the pill's own scale, about its middle along the edge: no top anchor
        scaleX = edgeNow?.scaleX ?: 1f
        scaleY = edgeNow?.scaleY ?: 1f
        pullScaleY = 1f
    } else if (!l.wiring.view.expanded) {
        val armSwell = 1f + 0.04f * ui.pillArm.value - PILL_DEAD_SHRINK * ui.deadZone.value * deadWeight
        scaleX *= armSwell * SwipeDownPull.pillScaleX(pull)
        pullScaleY = SwipeDownPull.pillScaleY(pull)
        scaleY *= armSwell * pullScaleY
    } else {
        val deadShrink = 1f - EXPANDED_DEAD_SHRINK * ui.deadZone.value * deadWeight
        val handle = ui.handlePull
        scaleX *= deadShrink * SwipeDownPull.expandedScaleX(pull) *
            SwipeDownPull.handleScaleX(handle)
        pullScaleY = SwipeDownPull.expandedScaleY(pull) *
            SwipeDownPull.handleScaleY(handle)
        scaleY *= deadShrink * pullScaleY
        val lean = SwipeSideways.totalLeanPx(ui.cardDragX, ui.cardLeanFromPx, ui.cardPop.value)
        if (lean != 0f) {
            val leanProgress = SwipeSideways.progress(lean, ui.cardSlideMaxPx)
            val stretchX = SwipeSideways.scaleX(leanProgress)
            scaleX *= stretchX
            scaleY *= SwipeSideways.scaleY(leanProgress)
            translationX += lean + SwipeSideways.edgeAnchorShiftPx(stretchX, size.width, lean)
        }
    }
    return pullScaleY
}

private fun Modifier.shellShadow(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    wiring: IslandWiring,
    palette: IslandPalette,
    edgeSide: ShellSide?,
    isEdgeLayout: Boolean,
    shellRadius: () -> Dp,
    topRoundness: () -> Float,
    swapSqueeze: () -> Offset
): Modifier {
    val activeMainShadow = if (wiring.view.expanded) {
        (settings.shadowElevation * 1.5f * palette.shadowScale).dp
    } else {
        settings.shadowElevation.dp
    }
    // a translucent edge pill casts no shadow while collapsed
    val edgeTranslucent = isEdgeLayout && settings.opacity < 0.999f
    return islandShellShadow(
        shellRadius, topRoundness, activeMainShadow,
        { layout.singleShadowAlpha * EdgePillRules.shellShadowShare(edgeTranslucent, layout.expandProgress) },
        edgeSide,
        swapSqueeze
    )
}

private fun Modifier.reportShellBounds(wiring: IslandWiring, ui: IslandInteractionState): Modifier =
    onGloballyPositioned { coords ->
        val b = coords.boundsInWindow()
        // the shell at rest (a tap swap moves it)
        if (!ui.tapSwapActive) ui.shellBoundsWin = b
        wiring.actions.expand.reportSurfaceBounds(
            Rect(
                b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt()
            )
        )
    }

/** The swap's width spring: the plain collapse's, except on the edge pill whose width is its thickness. */
internal fun swapWidthDamping(edgeLayout: Boolean): Float =
    if (edgeLayout) CollapseMotion.SETTLE_DAMPING else CollapseMotion.WIDTH_DAMPING
