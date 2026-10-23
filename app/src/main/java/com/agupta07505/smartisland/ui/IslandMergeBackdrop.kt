/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings

/**
 * Bubble merge style (push, then join), drawn BEHIND the pill so the pill's own content stays on top: while the
 * bodies push into each other the pill's touching end is pressed flat (opaque islands), then the join is drawn gooey
 * (Android 12+, opaque) or with a plain bridge.
 */
@Composable
internal fun MergeBubbleBackdrop(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    secondary: IslandSecondary,
    pillExit: PillExitState,
    switchScaleAnim: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    cornerLockY: () -> Dp,
    cb: IslandCallbacks
) {
    val density = LocalDensity.current
    if (!settings.enableNotchMode && secondary.mergeBackdropShown && !cb.currentExpanded &&
        secondary.secondaryNotification != null &&
        settings.mergeStyleKind() == com.agupta07505.smartisland.util.MergeStyle.BUBBLE
    ) {
        val gooLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
        val opaqueIsland = settings.opacity >= 0.99f
        val useGoo = gooeySupported(settings.opacity) && !com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()
        val gooBlurPx = with(density) { gooeyBlurDp(layout.circleSize.value).dp.toPx() }
        // Recorded at a reduced resolution when the blur is big (see gooeyLayerScale): a quarter of the pixels to blur.
        val gooScale = gooeyLayerScale(gooBlurPx)
        val gooEffect = remember(gooBlurPx, useGoo) { if (useGoo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) gooeyRenderEffect(gooBlurPx * gooScale) else null }
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
                    // The box follows the bubble's centre, so the bubble is always centred in it.
                    val merge = secondary.secondaryMergeAnim.value
                    val distDp = (layout.animatedXOffset - secondary.secondaryOffset).value
                    val contact = mergeContactFraction(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value)
                    val gapDp = mergeGapDp(distDp, layout.safeWidth.value, secondary.secondaryBubbleWidth.value)
                    val offsetPx = mergeBubbleOffsetDp(merge, contact, gapDp, secondary.secondaryBubbleWidth.value, layout.safeWidth.value).dp.toPx()
                    val towardPill = if (distDp >= 0f) 1f else -1f
                    // Landscape exit: the secondary rides along with the pill (same slide and fade).
                    alpha = secondary.secondaryAlpha * mergeBubbleAlpha(merge) * pillExit.pillAlpha()
                    translationX = towardPill * offsetPx + pillExit.slideXDp().dp.toPx()
                    translationY = pillExit.slideYDp().dp.toPx()
                }
                .drawBehind {
                    val merge = secondary.secondaryMergeAnim.value
                    val distDp = (layout.animatedXOffset - secondary.secondaryOffset).value
                    val pillWidthDp = layout.safeWidth.value
                    val bubbleWidthDp = secondary.secondaryBubbleWidth.value
                    val contact = mergeContactFraction(distDp, pillWidthDp, bubbleWidthDp)
                    val toward = if (distDp >= 0f) 1f else -1f
                    val gapDp = mergeGapDp(distDp, pillWidthDp, bubbleWidthDp)
                    val offsetDp = mergeBubbleOffsetDp(merge, contact, gapDp, bubbleWidthDp, pillWidthDp)
                    val edgeDp = mergeEdgeDistanceFromOffsetDp(distDp, pillWidthDp, offsetDp)
                    val bubbleScale = secondary.secondaryScale * switchScaleAnim.value
                    val pillHeightPx = layout.safeHeight.toPx()
                    val flat = mergeFaceFlat(merge, contact)
                    val shapes = gooeyShapes(
                        widthPx = size.width,
                        heightPx = size.height,
                        toward = toward,
                        bubbleScaleX = bubbleScale * mergeBubbleScaleX(merge, contact, bubbleWidthDp),
                        bubbleScaleY = bubbleScale * mergeBubbleScaleY(merge, contact),
                        edgePx = edgeDp.dp.toPx(),
                        pillWidthPx = layout.safeWidth.toPx(),
                        pillHeightPx = pillHeightPx,
                        pillScaleX = mergePillScaleX(merge, contact, pillWidthDp),
                        pillScaleY = mergePillScaleY(merge, contact, layout.safeHeight.value),
                        neckHeightPx = mergeNeckHeightFactor(merge, contact) * MERGE_NECK_HEIGHT_FRACTION * size.height * bubbleScale,
                        neckInsidePx = MERGE_NECK_INSIDE_DP.dp.toPx()
                    )
                    if (gooEffect != null && gooeyBridgeActive(merge, contact)) {
                        drawGooeyMerge(
                            layer = gooLayer,
                            effect = gooEffect,
                            shapes = shapes,
                            color = layout.pillBackgroundColor.copy(alpha = 1f),
                            blurPx = gooBlurPx,
                            pillCornerPx = pillHeightPx / 2f,
                            bubbleCornerPx = secondary.secondaryBubbleCorner.toPx() * bubbleScale,
                            layerScale = gooScale
                        )
                        return@drawBehind
                    }
                    // Crisp: the pill's end pressed flat (opaque islands only, a see-through one would show the overlap)...
                    if (opaqueIsland && flat > 0f) {
                        drawPillFlatFace(
                            pill = shapes.pill,
                            color = layout.pillBackgroundColor.copy(alpha = 1f),
                            toward = toward,
                            nearCornerPx = pillHeightPx / 2f * mergeNearCornerFactor(flat),
                            farCornerPx = pillHeightPx / 2f
                        )
                    }
                    // ...and the join of an island that is not drawn gooey: the bubble body and the bridge.
                    if (mergeJoin(merge, contact) > 0f) {
                        val body = shapes.bubble
                        if (body.width > 0f && body.height > 0f) {
                            val r = minOf(secondary.secondaryBubbleCorner.toPx() * bubbleScale, minOf(body.width, body.height) / 2f)
                            drawRoundRect(
                                color = layout.pillBackgroundColor.copy(alpha = settings.opacity),
                                topLeft = androidx.compose.ui.geometry.Offset(body.left, body.top),
                                size = androidx.compose.ui.geometry.Size(body.width, body.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                            )
                        }
                    }
                    shapes.neck?.let { neck ->
                        drawRect(
                            color = layout.pillBackgroundColor.copy(alpha = settings.opacity),
                            topLeft = androidx.compose.ui.geometry.Offset(neck.left, neck.top),
                            size = androidx.compose.ui.geometry.Size(neck.width, neck.height)
                        )
                    }
                }
        )
    }
}
