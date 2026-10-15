/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.expanded.IslandPalette
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush

/**
 * The visible island shell: animated size, the finger-follow and merge swell transform, shadow, clip, the
 * expanded-surface bounds report (for the touch region), and the background surface. Gestures are added by the caller.
 */
internal fun Modifier.islandPillSurface(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    cb: IslandCallbacks,
    ui: IslandInteractionState,
    squareTop: Boolean,
    pickerSideBySide: Boolean,
    /**
     * The expanded content is really drawing the two landscape islands (the overlay's flag AND the content's own report). The shell's
     * clip and shadow follow this, not the flag alone: the flag also sizes the window early, and a flag that is ahead of (or left behind by) the
     * content left a single card drawn with no rounded clip and no surface, a plain rectangle. Defaults to the flag.
     */
    twoIslandsDrawn: Boolean = pickerSideBySide,
    /** The shell's opacity, read while drawing so a fade between the media and the shared value does not recompose. */
    shellOpacity: () -> Float = { settings.opacity },
    /**
     * The single island reports its rect (used for the touch region) only while this is true. Landscape's two islands hand over to the
     * single island with a 160 ms surface fade, so the rect is held back until that fade is done.
     */
    reportBounds: Boolean = true,
    /** Read while drawing: true while the island closes onto the idle circle (landscape, the last card gone). The shell is not drawn then. */
    shellHidden: () -> Boolean = { false },
    /** The look of the expanded island (light or dark). */
    palette: IslandPalette = IslandPalette.Dark
): Modifier = this
        .islandAnimatedSize({
            // Landscape split: the shell's width is a direct function of the split progress (model/SplitShell.kt), so at progress 0 it
            // is the single island and the card island fills it. After the split has gone it keeps the single width until its own is there.
            val splitW = ui.stepDrag.splitShellWidthDp(
                splitNow = pickerSideBySide,
                expanded = cb.currentExpanded,
                single = layout.singleExpandedWidth.value,
                total = layout.expandedWidth.value,
                own = layout.safeWidth.value
            )
            if (splitW >= 0f) splitW.dp else layout.safeWidth
        }, {
            // A card step that follows the finger sets the height itself (no spring in between).
            val own = layout.safeHeight
            ui.stepDrag.shellHeightDp = own.value
            val dragged = ui.stepDrag.heightDp
            if (dragged >= 0f) {
                dragged.dp
            } else {
                // Landscape split: the height follows the same progress as the width (model/SplitShell.kt), not its own spring.
                val splitH = ui.stepDrag.splitShellHeightDp(
                    splitNow = pickerSideBySide,
                    expanded = cb.currentExpanded,
                    own = own.value
                )
                if (splitH >= 0f) splitH.dp else own
            }
        })
        .graphicsLayer {
            // While the split override sets the width, the island is centred (the offset of an expanded island at rest is 0); the animated
            // offset is worked out from the shell's own width, which is not the width drawn then.
            val splitW = ui.stepDrag.splitShellWidthDp(
                splitNow = pickerSideBySide,
                expanded = cb.currentExpanded,
                single = layout.singleExpandedWidth.value,
                total = layout.expandedWidth.value,
                own = layout.safeWidth.value
            )
            val baseX = if (splitW >= 0f) 0f else layout.animatedXOffset.toPx()
            translationX = baseX + (if (!cb.currentExpanded) ui.pillDragOffsetX else 0f) +
                pillExit.slideXDp().dp.toPx()
            translationY = layout.yOffset.toPx() + ui.dragOffset + pillExit.slideYDp().dp.toPx()
            // The collapsed pill swells as the secondary bubble reaches it: a small even swell for the slide
            // style; for the bubble style it is pushed taller, then rings out after taking the bubble in.
            val mergeNow = secondary.secondaryMergeAnim.value
            val contactNow = mergeContactFraction(
                (layout.animatedXOffset - secondary.secondaryOffset).value,
                layout.safeWidth.value,
                secondary.secondaryBubbleWidth.value
            )
            if (!cb.currentExpanded && settings.mergeStyleKind() == com.agupta07505.smartisland.util.MergeStyle.BUBBLE) {
                // Landscape: the pill fades while it slides out and in (pillAlpha); the idle circle is the separate dot.
                scaleX = switchScaleAnim.value * mergePillScaleX(mergeNow, contactNow, layout.safeWidth.value)
                scaleY = switchScaleAnim.value * mergePillScaleY(mergeNow, contactNow, layout.safeHeight.value)
            } else {
                val swell = 1f + 0.07f * mergeSwell(mergeNow, contactNow)
                scaleX = switchScaleAnim.value * swell
                scaleY = switchScaleAnim.value * swell
            }
            // A swipe that would fire if the finger lifted now: the pill swells a touch (1 -> 1.04). In the dead zone
            // (the finger moved, lifting does nothing) it shrinks a touch instead, so the three states look different.
            // A swipe down stretches the island downward with the finger: the dead-zone shrink fades out as it grows.
            val pull = ui.swipeDownPull
            val deadWeight = com.agupta07505.smartisland.util.SwipeDownPull.deadShrinkWeight(pull)
            val pullScaleY: Float
            if (!cb.currentExpanded) {
                val armSwell = 1f + 0.04f * ui.pillArm.value - PILL_DEAD_SHRINK * ui.deadZone.value * deadWeight
                scaleX *= armSwell * com.agupta07505.smartisland.util.SwipeDownPull.pillScaleX(pull)
                pullScaleY = com.agupta07505.smartisland.util.SwipeDownPull.pillScaleY(pull)
                scaleY *= armSwell * pullScaleY
            } else {
                val deadShrink = 1f - EXPANDED_DEAD_SHRINK * ui.deadZone.value * deadWeight
                // The floating-window handle: narrower and taller with the pull, squeezing the content (the shell's layer scales it all).
                val handle = ui.handlePull
                scaleX *= deadShrink * com.agupta07505.smartisland.util.SwipeDownPull.expandedScaleX(pull) *
                    com.agupta07505.smartisland.util.SwipeDownPull.handleScaleX(handle)
                pullScaleY = com.agupta07505.smartisland.util.SwipeDownPull.expandedScaleY(pull) *
                    com.agupta07505.smartisland.util.SwipeDownPull.handleScaleY(handle)
                scaleY *= deadShrink * pullScaleY
                // A left / right swipe leans the whole island towards the finger and stretches it a little that way; the lean the
                // finger let go with runs out on the swap's spring. The trailing edge stays, so the stretch reaches only in the swipe's direction.
                val lean = com.agupta07505.smartisland.util.SwipeSideways.totalLeanPx(ui.cardDragX, ui.cardLeanFromPx, ui.cardPop.value)
                if (lean != 0f) {
                    val leanProgress = com.agupta07505.smartisland.util.SwipeSideways.progress(lean, ui.cardSlideMaxPx)
                    val stretchX = com.agupta07505.smartisland.util.SwipeSideways.scaleX(leanProgress)
                    scaleX *= stretchX
                    scaleY *= com.agupta07505.smartisland.util.SwipeSideways.scaleY(leanProgress)
                    translationX += lean + com.agupta07505.smartisland.util.SwipeSideways.edgeAnchorShiftPx(stretchX, size.width, lean)
                }
            }
            // Keep the top edge in place: the stretch reaches down only.
            translationY += com.agupta07505.smartisland.util.SwipeDownPull.topAnchorShiftPx(pullScaleY, size.height)
            // Only the pill exit fades the layer. The swipe lock cue does not dim it, because a layer alpha covers the whole subtree
            // (the card's backdrop and content) and the card would go see-through for the 400 ms of the lock. The cue is the outline below.
            alpha = if (shellHidden()) 0f else (pillExit.shellAlpha() * layout.hidingAlpha)
        }
        .then(
            if (settings.enableShadow && settings.shadowElevation > 0f && !layout.isHiding && !twoIslandsDrawn) {
                val activeMainShadow = if (cb.currentExpanded) {
                    (settings.shadowElevation * 1.5f * palette.shadowScale).dp
                } else {
                    settings.shadowElevation.dp
                }
                Modifier.islandShellShadow({ layout.safeRadius }, squareTop, activeMainShadow, { layout.singleShadowAlpha })
            } else Modifier
        )
        // Two separate islands draw (and shadow) themselves; clipping here would cut their shadows.
        .then(if (twoIslandsDrawn) Modifier else Modifier.islandShellClip({ layout.safeRadius }, squareTop))
        .then(
            if (cb.currentExpanded && !pickerSideBySide && reportBounds) {
                // Reports the island's rect (after scale and drag) for the touch region.
                Modifier.onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    cb.currentOnExpandedSurfaceBounds(
                        android.graphics.Rect(
                            b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt()
                        )
                    )
                }
            } else Modifier
        )
        .islandShellSurface(
            radius = { layout.safeRadius },
            squareTop = squareTop,
            brush = expandedGradientBrush(settings, palette)?.takeIf { cb.currentExpanded },
            color = layout.pillBackgroundColor,
            // Light look: the shell eases from the pill colour to the pale surface as the island opens (and back on collapse).
            expandedColor = if (palette.isLight) palette.surface else null,
            expandedFraction = { layout.expandProgress },
            // The cards do not slide under the finger (the island leans with them inside it) and the two cards of a swap are one
            // card apart, so there is no strip of bare shell to cover: the shell keeps its own opacity.
            alpha = { shellOpacity() * layout.singleSurfaceAlpha }
        )
        .lockCueOutline(
            shape = { islandShellShape(layout.safeRadius.toPx(), squareTop) },
            cue = { ui.lockCue.value },
            // A white outline would vanish on the light island.
            color = if (palette.isLight) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
        )
