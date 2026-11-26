/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.PillOpenDrag
import kotlin.math.abs

internal class ShownStates(
    val width: State<Dp>,
    val height: State<Dp>,
    val radius: State<Dp>,
    val topShift: State<Dp>,
    val collapsedAlpha: State<Float>,
    val expandedAlpha: State<Float>,
    val contentScale: State<Float>,
    val contentSlideY: State<Dp>,
    val expandProgress: State<Float>,
    val expandProgressY: State<Float>
)

@Composable
internal fun rememberShownStates(
    settings: SmartIslandSettings,
    expandedWidth: Dp,
    expandedHeight: Dp,
    expanded: Boolean,
    isHiding: Boolean,
    drag: PillOpenDragState?,
    bubbleSize: Float,
    pillHeightNow: Float,
    collapsedTopShift: Dp,
    anim: ShellAnimations,
    pulseWidthState: State<Dp>,
    lineExtraState: State<Float>,
    blendsNow: () -> Boolean
): ShownStates {
    val heightState = anim.sizes.height
    val radiusState = anim.sizes.radius
    val topShiftState = anim.sizes.topShift
    val collapsedAlphaState = anim.fades.collapsed
    val expandedAlphaState = anim.fades.expanded
    val contentScaleState = anim.contentScale
    val contentSlideYState = anim.contentSlideY
    val expandProgressState = anim.expandProgress
    val expandProgressYState = anim.expandProgressY
    // while a swipe down opens the island under the finger
    val shownWidthState = remember(pulseWidthState, drag, settings.width, expandedWidth, expanded, bubbleSize) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.lerp(settings.width, expandedWidth.value, d.progress).dp
            else if (blendsNow()) BubbleOrigin.edge(
                bubbleSize, expandedWidth.value, pulseWidthState.value.value, expandProgressState.value
            ).dp
            else pulseWidthState.value
        }
    }
    val shownHeightState = remember(heightState, lineExtraState, drag, settings.height, expandedHeight, expanded, bubbleSize) {
        derivedStateOf {
            val d = drag
            val height = heightState.value + lineExtraState.value.dp
            if (d != null && d.active) PillOpenDrag.lerp(pillHeightNow, expandedHeight.value, d.progress).dp
            else if (blendsNow()) BubbleOrigin.edge(
                bubbleSize, expandedHeight.value, height.value, expandProgressYState.value
            ).dp
            else height
        }
    }
    val shownRadiusState = remember(radiusState, drag, settings.cornerRadius, expanded, bubbleSize) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.lerp(settings.cornerRadius.toFloat(), 34f, d.progress).dp
            else if (blendsNow()) BubbleOrigin.radius(
                bubbleSize / 2f, 34f, radiusState.value.value, expandProgressState.value
            ).dp
            else radiusState.value
        }
    }
    val shownTopShiftState = remember(topShiftState, drag, collapsedTopShift) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.lerp(collapsedTopShift.value, 0f, d.progress).dp
            else topShiftState.value
        }
    }
    val shownCollapsedAlphaState = remember(collapsedAlphaState, drag, isHiding) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) (if (isHiding) 0f else PillOpenDrag.pillContentAlpha(d.progress))
            else collapsedAlphaState.value
        }
    }
    val shownExpandedAlphaState = remember(expandedAlphaState, drag) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.cardContentAlpha(d.progress) else expandedAlphaState.value
        }
    }
    val shownContentScaleState = remember(contentScaleState, drag) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.lerp(0.95f, 1f, d.progress) else contentScaleState.value
        }
    }
    val shownContentSlideYState = remember(contentSlideYState, drag) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) PillOpenDrag.lerp(-6f, 0f, d.progress).dp else contentSlideYState.value
        }
    }
    val shownExpandProgressState = remember(expandProgressState, drag) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) d.progress else expandProgressState.value
        }
    }
    val shownExpandProgressYState = remember(expandProgressYState, drag) {
        derivedStateOf {
            val d = drag
            if (d != null && d.active) d.progress else expandProgressYState.value
        }
    }
    return ShownStates(
        shownWidthState, shownHeightState, shownRadiusState, shownTopShiftState,
        shownCollapsedAlphaState, shownExpandedAlphaState, shownContentScaleState,
        shownContentSlideYState, shownExpandProgressState, shownExpandProgressYState
    )
}

internal fun bindPillOpenDrag(
    drag: PillOpenDragState?,
    settings: SmartIslandSettings,
    expandedWidth: Dp,
    expandedHeight: Dp,
    pillHeightNow: Float,
    anim: ShellAnimations
) {
    val widthState = anim.sizes.width
    val heightState = anim.sizes.height
    val expandedAlphaState = anim.fades.expanded
    val collapsedAlphaState = anim.fades.collapsed
    if (drag != null) {
        drag.cardHeightDp = { expandedHeight.value }
        drag.springsAtRest = { open ->
            val w = widthState.value.value
            val h = heightState.value.value
            if (open) {
                abs(w - expandedWidth.value) < SPRING_REST_DP &&
                    abs(h - expandedHeight.value) < SPRING_REST_DP &&
                    expandedAlphaState.value > 0.98f
            } else {
                abs(w - settings.width) < SPRING_REST_DP &&
                    abs(h - pillHeightNow) < SPRING_REST_DP &&
                    collapsedAlphaState.value > 0.98f
            }
        }
    }
}
