/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification

/** Collapsed: the secondary circle. Expanded with 2 cards: the same item morphs into a full-size pill. */
@Composable
internal fun SecondaryBubble(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    cornerLockY: () -> Dp,
    notifications: List<IslandNotification>,
    onPageSelected: (Int) -> Unit,
    cb: IslandCallbacks,
    ui: IslandInteractionState
) {
    if (!settings.enableNotchMode && secondary.secondaryShown &&
        secondary.secondaryNotification != null
    ) {
        val bubbleMerge = settings.mergeStyleKind() == com.agupta07505.smartisland.util.MergeStyle.BUBBLE
        // The outline is built where it is drawn (graphicsLayer blocks below), so the springs behind it do not
        // recompose this function on every frame.
        val bubbleShape: () -> androidx.compose.ui.graphics.Shape = {
            bubbleMergeShape(
                bubbleMerge = bubbleMerge && !cb.currentExpanded,
                merge = secondary.secondaryMergeAnim.value,
                distDp = (layout.animatedXOffset - secondary.secondaryOffset).value,
                pillWidthDp = layout.safeWidth.value,
                bubbleWidthDp = secondary.secondaryBubbleWidth.value,
                cornerDp = secondary.secondaryBubbleCorner
            )
        }
        // The bubble's icon is gone before the join (bubble style only).
        val bubbleIconAlpha: () -> Float = {
            if (bubbleMerge && !cb.currentExpanded) {
                val distDp = (layout.animatedXOffset - secondary.secondaryOffset).value
                mergeIconAlpha(
                    secondary.secondaryMergeAnim.value,
                    mergeContactFraction(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value)
                )
            } else 1f
        }
        Box(
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        secondary.secondaryOffset.roundToPx(),
                        cornerLockY().roundToPx()
                    )
                }
                .islandAnimatedSize({ secondary.secondaryBubbleWidth }, { layout.circleSize })
                .graphicsLayer {
                    // merge = 0 normal bubble, 1 fused into the primary cutout.
                    val merge = if (cb.currentExpanded) 0f else secondary.secondaryMergeAnim.value
                    val dist = (layout.animatedXOffset - secondary.secondaryOffset).toPx()
                    val distDp = (layout.animatedXOffset - secondary.secondaryOffset).value
                    val contact = mergeContactFraction(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value)
                    val travel = mergeTravelDp(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value).dp.toPx()
                    val towardPill = if (dist >= 0f) 1f else -1f
                    // Landscape exit: the secondary rides along with the pill (same slide and fade).
                    val joined = bubbleMerge && merge > 0f && mergeJoin(merge, contact) > 0f
                    alpha = if (joined) 0f else (secondary.secondaryAlpha * mergeBubbleAlpha(merge) * pillExit.pillAlpha() * layout.hidingAlpha)
                    val bubbleScale = secondary.secondaryScale * switchScaleAnim.value
                    var moveX = travel * merge
                    if (bubbleMerge) {
                        // Bubble style: push, then join (ui/MergeBubble.kt). The bubble scales about its centre:
                        // pressed narrower and taller by the push, then drawn into the pill and shrunk away.
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                        val gapDp = mergeGapDp(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value)
                        moveX = mergeBubbleOffsetDp(merge, contact, gapDp, secondary.secondaryBubbleWidth.value, layout.safeWidth.value).dp.toPx()
                        scaleX = bubbleScale * mergeBubbleScaleX(merge, contact, secondary.secondaryBubbleWidth.value)
                        scaleY = bubbleScale * mergeBubbleScaleY(merge, contact)
                    } else {
                        // Pivot on the side away from the pill so the stretch reaches toward it.
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (towardPill > 0f) 0f else 1f, 0.5f)
                        val stretch = mergeStretch(merge, contact)
                        scaleX = bubbleScale * (1f + stretch)
                        scaleY = bubbleScale * (1f - stretch * 0.5f)
                    }
                    translationX = towardPill * moveX + pillExit.slideXDp().dp.toPx()
                    translationY = pillExit.slideYDp().dp.toPx()
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f) {
                        Modifier.graphicsLayer {
                            shadowElevation = (settings.shadowElevation * 0.85f * (1f - secondary.secondaryMergeAnim.value)).dp.toPx()
                            shape = bubbleShape()
                            clip = false
                            ambientShadowColor = Color.Black
                            spotShadowColor = Color.Black
                        }
                    } else Modifier
                )
                .graphicsLayer {
                    shape = bubbleShape()
                    clip = true
                }
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // A real touch (not just a timer restart): an auto-expanded island that was never touched would otherwise
                    // close again 0.8 s after this tap.
                    ui.userTouched()
                    if (cb.currentExpanded && secondary.sameAppCompanion) {
                        // Expanded and the card has siblings: the cutout opens the list of
                        // them (a second tap closes it) instead of cycling to the next one.
                        if (cb.currentSiblingPickerOpen) cb.currentOnSiblingPickerClose()
                        else cb.currentOnOpenSiblingPicker()
                        return@clickable
                    }
                    if (secondary.secondaryIndex >= 0) {
                        onPageSelected(secondary.secondaryIndex)
                    }
                    if (!cb.currentExpanded) {
                        cb.currentOnToggle()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (secondary.sameAppCompanion) {
                // Expanded and the card has siblings: this cutout is the picker button, so it shows
                // three lines (a list) instead of repeating a card's own icon.
                SiblingPickerLines(open = cb.currentSiblingPickerOpen)
            } else if (secondary.secondaryNotification != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - secondary.secondaryPillProgress) * bubbleIconAlpha() },
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryBubbleContent(
                        notification = secondary.secondaryNotification,
                        settings = settings
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredWidth(layout.miniPillWidth)
                        .height(layout.circleSize)
                        .graphicsLayer { alpha = secondary.secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    IslandCollapsedContent(
                        mode = secondary.secondaryNotification.mode,
                        notification = secondary.secondaryNotification,
                        collapsedAlpha = 1f,
                        settings = settings
                    )
                }
            }
        }
    }
}
