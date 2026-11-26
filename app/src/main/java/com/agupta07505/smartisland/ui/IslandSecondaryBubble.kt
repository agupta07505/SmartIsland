/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.BubbleIntoCutout
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.MergeStyle
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.model.SwapTouchGuard
import com.agupta07505.smartisland.model.nextSameAppIndex

private const val BUBBLE_ORIGIN_MERGE_MAX = 0.05f

private fun GraphicsLayerScope.applySecondaryBubbleLayer(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    ui: IslandInteractionState,
    wiring: IslandWiring,
    bubbleMerge: Boolean,
    screenWidthDp: Float
) {
    // Merge = 0 normal bubble, 1 fused into the primary cutout
    val merge = if (wiring.view.expanded) 0f else secondary.merge.anim.value
    val dist = (layout.animatedXOffset - secondary.offsets.state.value).toPx()
    val distDp = (layout.animatedXOffset - secondary.offsets.state.value).value
    val contact = mergeContactFraction(distDp, layout.safeWidth.value, secondary.anims.bubbleWidth.value.value)
    val travel = mergeTravelDp(distDp, layout.safeWidth.value, secondary.anims.bubbleWidth.value.value).dp.toPx()
    val towardPill = if (dist >= 0f) 1f else -1f
    // Landscape exit: the secondary rides along with the pill (same slide and fade)
    val joined = bubbleMerge && merge > 0f && mergeJoin(merge, contact) > 0f
    alpha = if (joined) 0f else (secondary.anims.secondaryAlpha.value * mergeBubbleAlpha(merge) * pillExit.pillAlpha() * layout.hidingFade *
        ui.cutoutSwapAlpha(secondary.cutoutId) * secondary.pickerArrival())
    val bubbleScale = secondary.anims.secondaryScale.value * switchScaleAnim.value * secondary.pickerScale() * secondary.swellScale
    var moveX = travel * merge
    if (bubbleMerge) {
        // Bubble style: push, then join (ui/MergeBubble.kt)
        transformOrigin = TransformOrigin.Center
        val gapDp = mergeGapDp(distDp, layout.safeWidth.value, secondary.anims.bubbleWidth.value.value)
        moveX = mergeBubbleOffsetDp(merge, contact, gapDp, secondary.anims.bubbleWidth.value.value, layout.safeWidth.value)
            .dp.toPx()
        scaleX = bubbleScale * mergeBubbleScaleX(merge, contact, secondary.anims.bubbleWidth.value.value)
        scaleY = bubbleScale * mergeBubbleScaleY(merge, contact)
    } else {
        // Pivot on the side away from the pill so the stretch reaches toward it
        transformOrigin = TransformOrigin(if (towardPill > 0f) 0f else 1f, 0.5f)
        val stretch = mergeStretch(merge, contact)
        scaleX = bubbleScale * (1f + stretch)
        scaleY = bubbleScale * (1f - stretch * 0.5f)
    }
    // Gliding into the cutout: a light stretch along the travel
    val glide = secondary.glideStretch()
    if (glide != 1f) {
        scaleX *= glide
        scaleY *= BubbleIntoCutout.squash(glide)
    }
    // The swipe's lean carries the bubble with the pill
    val leanPx = secondaryLeanPx(ui, layout, secondary, settings, wiring.view.expanded, screenWidthDp, density)
    // The wide pulse grows the pill toward the bubble, which rides out with the growing edge instead of being covered
    val pulseShiftPx = if (wiring.view.expanded) 0f else {
        PulseBubble.shiftDp(
            layout.widePulseGrowDp, layout.pulseAnchorSide, layout.isCircleLeft, layout.pulseCentreShiftDp
        ).dp.toPx()
    }
    translationX = towardPill * moveX + pillExit.slideXDp().dp.toPx() + leanPx + pulseShiftPx
    translationY = pillExit.slideYDp().dp.toPx()
}

private fun onSecondaryBubbleTap(
    ui: IslandInteractionState,
    wiring: IslandWiring,
    secondary: IslandSecondary,
    onPageSelected: (Int) -> Unit,
    cutoutBounds: Array<Rect?>,
    landscape: Boolean
) {
    // a real touch (not just a timer restart)
    ui.userTouched()
    if (wiring.view.expanded && secondary.cards.sameAppCompanion) {
        // Bubble turned into the picker button mid swap, the tap still belongs to the swap
        if (ui.tapSwapActive) {
            ui.onPickerTapDuringSwap?.invoke(secondary.cutoutId)
            return
        }
        // Expanded and the card has siblings. Right after a swap the tap was aimed at the cutout, not at the picker
        if (wiring.view.siblingPickerOpen) wiring.actions.sibling.closePicker()
        else if (!ui.cardTouchLocked(SystemClock.uptimeMillis())) wiring.actions.sibling.openPicker()
        return
    }
    if (turnAroundOnCutoutTap(ui, wiring, secondary.cards.secondaryIndex, onPageSelected)) return
    if (secondary.cards.secondaryIndex >= 0) {
        ui.markCutout(secondary.cutoutId) { cutoutBounds[0] }
        // The bubble tap used to reopen the active card. Select the bubble’s event before opening.
        onPageSelected(secondary.cards.secondaryIndex)
    }
    if (!wiring.view.expanded) {
        if (landscape && BubbleOrigin.startsFromBubble(
                expanded = false,
                onBubble = secondary.merge.anim.value < BUBBLE_ORIGIN_MERGE_MAX,
                selectsBubbleEvent = secondary.cards.secondaryIndex >= 0 &&
                    nextSameAppIndex(wiring.view.notifications, secondary.cards.secondaryIndex) < 0
            )
        ) ui.markOpenedFromBubble()
        wiring.actions.expand.toggle()
    }
}

@Composable
@NonRestartableComposable
private fun SecondaryGlyph(
    notification: IslandNotification,
    settings: SmartIslandSettings,
    layout: IslandLayout,
    sideCamera: Boolean
) {
    IslandCollapsedContent(
        mode = notification.mode,
        notification = notification,
        collapsedAlpha = 1f,
        settings = settings,
        pillWidthDp = if (sideCamera) settings.width else 0f,
        cameraGapDp = if (sideCamera) layout.cameraGapDp else 0f,
        cameraCentreInPillDp = if (sideCamera) layout.cameraCentreInPillDp else null,
        cameraSide = layout.cameraSide,
        sidePlan = if (sideCamera) layout.sideZone?.plan else null
    )
}

/** Bubble's face: icon and glyph, each crossfading with the card it replaces. */
@Composable
@NonRestartableComposable
private fun SecondaryBubbleFace(
    notification: IslandNotification,
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    ui: IslandInteractionState,
    sideCamera: Boolean,
    landscape: Boolean,
    bubbleIconAlpha: () -> Float
) {
    val swap = rememberCardSwap(
        notification,
        swipeEntering = ui.openedFromBubble,
        durationMs = settings.animationSpeed(landscape).durationMs(CARD_SWAP_FADE_MS)
    )
    val outgoing = swap.outgoing
    if (outgoing != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha =
                    swapOutgoingAlpha(swap.progress()) * BubbleIntoCutout.iconAlpha(secondary.secondaryPillProgress) * bubbleIconAlpha() },
            contentAlignment = Alignment.Center
        ) {
            SecondaryBubbleContent(notification = outgoing, settings = settings)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val share = if (swap.crossing) swapIncomingAlpha(swap.progress()) else 1f
                alpha = share * BubbleIntoCutout.iconAlpha(secondary.secondaryPillProgress) * bubbleIconAlpha()
            },
        contentAlignment = Alignment.Center
    ) {
        SecondaryBubbleContent(
            notification = notification,
            settings = settings
        )
    }
    if (outgoing != null) {
        Box(
            modifier = Modifier
                .requiredWidth(layout.miniPillWidth)
                .height(layout.circleSize)
                .graphicsLayer { alpha =
                    swapOutgoingAlpha(swap.progress()) * BubbleIntoCutout.glyphAlpha(secondary.secondaryPillProgress) },
            contentAlignment = Alignment.Center
        ) {
            SecondaryGlyph(outgoing, settings, layout, sideCamera)
        }
    }
    Box(
        modifier = Modifier
            .requiredWidth(layout.miniPillWidth)
            .height(layout.circleSize)
            .graphicsLayer {
                val share = if (swap.crossing) swapIncomingAlpha(swap.progress()) else 1f
                alpha = share * BubbleIntoCutout.glyphAlpha(secondary.secondaryPillProgress)
            },
        contentAlignment = Alignment.Center
    ) {
        SecondaryGlyph(notification, settings, layout, sideCamera)
    }
}

/** Collapsed: the secondary circle */
@Composable
internal fun SecondaryBubble(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: Animatable<Float, AnimationVector1D>,
    cornerLockY: () -> Dp,
    notifications: List<IslandNotification>,
    onPageSelected: (Int) -> Unit,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    screenWidthDp: Float = 0f
) {
    if (secondary.shown.secondary.value &&
        secondary.cards.secondaryNotification != null
    ) {
        val bubbleMerge = settings.mergeStyleKind() == MergeStyle.BUBBLE
        // where the cutout sits (a tap swap dips the shell to it).
        val cutoutBounds = remember { arrayOfNulls<Rect>(1) }
        val downTap = remember { SwapTouchGuard() }
        val sideCamera = layout.cameraSide != CameraSide.Centre && layout.sideZone != null
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        // The outline is built where it is drawn (graphicsLayer blocks below)
        val bubbleShape: () -> Shape = {
            val merging = bubbleMerge && !wiring.view.expanded
            if (settings.enableNotchMode && !merging) {
                notchCutoutShape(secondary.anims.bubbleCorner.value, layout.circleSize, secondary.secondaryPillProgress)
            } else bubbleMergeShape(
                bubbleMerge = merging,
                merge = secondary.merge.anim.value,
                distDp = (layout.animatedXOffset - secondary.offsets.state.value).value,
                pillWidthDp = layout.safeWidth.value,
                bubbleWidthDp = secondary.anims.bubbleWidth.value.value,
                cornerDp = secondary.anims.bubbleCorner.value
            )
        }
        val bubbleIconAlpha: () -> Float = {
            if (bubbleMerge && !wiring.view.expanded) {
                val distDp = (layout.animatedXOffset - secondary.offsets.state.value).value
                mergeIconAlpha(
                    secondary.merge.anim.value,
                    mergeContactFraction(distDp, layout.safeWidth.value, secondary.anims.bubbleWidth.value.value)
                )
            } else 1f
        }
        Box(
            modifier = Modifier
                .zIndex(if (secondaryBehindPill(bubbleMerge, wiring.view.expanded, settings.enableNotchMode)) -1f else 0f)
                .absoluteOffset {
                    IntOffset(
                        secondary.offsets.state.value.roundToPx(),
                        cornerLockY().roundToPx()
                    )
                }
                .islandAnimatedSize({ secondary.anims.bubbleWidth.value }, { layout.circleSize })
                .graphicsLayer {
                    applySecondaryBubbleLayer(
                        settings, layout, secondary, pillExit, switchScaleAnim, ui, wiring, bubbleMerge, screenWidthDp
                    )
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f) {
                        Modifier.graphicsLayer {
                            shadowElevation =
                                (settings.shadowElevation * 0.85f * (1f - secondary.merge.anim.value)).dp.toPx()
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
                .onGloballyPositioned { coords ->
                    if (!ui.tapSwapActive) cutoutBounds[0] = coords.boundsInWindow()
                }
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity))
                .cutoutTapOnDown(
                    guard = downTap,
                    enabled = {
                        wiring.view.expanded && !ui.tapSwapActive && !secondary.shown.cornerPill.value &&
                            !secondary.cards.sameAppCompanion && secondary.cards.secondaryIndex >= 0
                    }
                ) {
                    onSecondaryBubbleTap(ui, wiring, secondary, onPageSelected, cutoutBounds, landscape)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !secondary.shown.cornerPill.value
                ) {
                    if (downTap.claimClick()) return@clickable
                    onSecondaryBubbleTap(ui, wiring, secondary, onPageSelected, cutoutBounds, landscape)
                },
            contentAlignment = Alignment.Center
        ) {
            if (secondary.cards.sameAppCompanion) {
                SiblingPickerLines(open = wiring.view.siblingPickerOpen)
            } else if (secondary.cards.secondaryNotification != null) {
                SecondaryBubbleFace(
                    secondary.cards.secondaryNotification, settings, layout, secondary, ui, sideCamera, landscape,
                        bubbleIconAlpha
                )
            }
        }
    }
}

private fun pickerGlideShape(
    progress: Float,
    circleSize: Dp,
    cornerRadius: Dp,
    notchMode: Boolean
): Shape {
    val round = lerp(circleSize / 2f, cornerRadius, progress)
    return if (notchMode) notchCutoutShape(round, circleSize, progress) else RoundedCornerShape(round)
}

/** Notch cutout's shape: the top corners go from round to square as the bubble widens into the pill. */
private fun notchCutoutShape(round: Dp, circleSize: Dp, pillProgress: Float): Shape {
    val flat = lerp(circleSize / 2f, 0.dp, pillProgress.coerceIn(0f, 1f))
    return RoundedCornerShape(topStart = flat, topEnd = flat, bottomEnd = round, bottomStart = round)
}

/** Bubble's glide to the cutout while the sibling picker takes its old spot, drawn as portrait's bubble is. */
@Composable
internal fun PickerGlideBubble(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    cornerLockY: () -> Dp
) {
    val card = secondary.cards.tertiaryNotification
    if (secondary.picker.run && card != null) {
        val flight = secondary.picker.flight
        val widen = secondary.picker.shape
        val sideCamera = layout.cameraSide != CameraSide.Centre && layout.sideZone != null
        Box(
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        lerp(secondary.picker.startOffset, secondary.picker.endOffset, flight.value).roundToPx(),
                        cornerLockY().roundToPx()
                    )
                }
                .islandAnimatedSize(
                    { lerp(layout.circleSize, layout.miniPillWidth, widen.value) },
                    { layout.circleSize }
                )
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f) {
                        Modifier.graphicsLayer {
                            shadowElevation = (settings.shadowElevation * 0.85f).dp.toPx()
                            shape = pickerGlideShape(
                                widen.value, layout.circleSize, settings.cornerRadius.dp, settings.enableNotchMode
                            )
                            clip = false
                            ambientShadowColor = Color.Black
                            spotShadowColor = Color.Black
                        }
                    } else Modifier
                )
                .graphicsLayer {
                    shape = pickerGlideShape(
                        widen.value, layout.circleSize, settings.cornerRadius.dp, settings.enableNotchMode
                    )
                    clip = true
                }
                .background(layout.pillBackgroundColor.copy(alpha = settings.opacity)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - widen.value },
                contentAlignment = Alignment.Center
            ) {
                SecondaryBubbleContent(notification = card, settings = settings)
            }
            Box(
                modifier = Modifier
                    .requiredWidth(layout.miniPillWidth)
                    .height(layout.circleSize)
                    .graphicsLayer { alpha = widen.value },
                contentAlignment = Alignment.Center
            ) {
                IslandCollapsedContent(
                    mode = card.mode,
                    notification = card,
                    collapsedAlpha = 1f,
                    settings = settings,
                    pillWidthDp = if (sideCamera) settings.width else 0f,
                    cameraGapDp = if (sideCamera) layout.cameraGapDp else 0f,
                    cameraCentreInPillDp = if (sideCamera) layout.cameraCentreInPillDp else null,
                    cameraSide = layout.cameraSide,
                    sidePlan = if (sideCamera) layout.sideZone?.plan else null
                )
            }
        }
    }
}

/** Sideways lean (px) the bubble takes from the pill it sits beside: main pill collapsed, corner pill expanded. */
internal fun secondaryLeanPx(
    ui: IslandInteractionState,
    layout: IslandLayout,
    secondary: IslandSecondary,
    settings: SmartIslandSettings,
    expanded: Boolean,
    screenWidthDp: Float,
    density: Float
): Float {
    val pillLeanDp = when {
        !expanded -> pillLeanWithinScreenDp(
            ui.pillDragOffsetX / density, layout.collapsedMainLeft.value, settings.width, screenWidthDp
        )
        secondary.shown.cornerPill.value -> ui.cornerPillDragX / density
        else -> 0f
    }
    return bubbleFollowLeanDp(
        pillLeanDp, secondary.offsets.state.value.value, secondary.anims.bubbleWidth.value.value, screenWidthDp
    ) * density
}
