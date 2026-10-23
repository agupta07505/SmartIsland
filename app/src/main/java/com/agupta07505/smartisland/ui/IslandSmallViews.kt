/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue

/**
 * The separate round tap target for the idle dot (a capsule in portrait, where it is as wide as the idle pill). Draws
 * nothing while [alpha] is ~0 or [dot] is null.
 */
@Composable
internal fun LandscapeIdleDotView(
    dot: com.agupta07505.smartisland.model.IdleDotGeometry?,
    alpha: () -> Float,
    topShiftDp: () -> Float,
    color: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    // The fade is read at draw time; composition only sees whether the dot is visible at all (flips once).
    val alphaNow = rememberUpdatedState(alpha)
    val visible by remember { derivedStateOf { alphaNow.value() > 0.01f } }
    if (dot == null || !visible) return
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val size = dot.sizeDp.dp
    val widthDp = dot.widthDp.dp
    val centerX = dot.centerXDp.dp
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(size)
            .graphicsLayer {
                translationX = centerX.toPx()
                translationY = (dot.topDp - topShiftDp()).dp.toPx()
                this.alpha = alphaNow.value()
            }
            .clip(CircleShape)
            .background(color.copy(alpha = dot.opacity), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnTap() }, onLongPress = { currentOnLongPress() })
            }
    )
}

/**
 * Portrait, island expanded: the glyph-free centre band of the pill, at the top of the expanded island, as a
 * transparent tap target drawn above the card (the card's own click would otherwise take the tap and open the
 * notification).
 */
@Composable
internal fun PortraitPillCenterTarget(
    enabled: Boolean,
    zoneWidth: Dp,
    zoneHeight: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier = modifier
            .width(zoneWidth)
            .height(zoneHeight)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnTap() }, onLongPress = { currentOnLongPress() })
            }
    )
}

/**
 * Where the expanded island's close target sits: on the camera cutout the device reports
 * ([com.agupta07505.smartisland.util.CameraCutoutDetector]), or on the pill's centre band when there is none.
 */
@Composable
internal fun rememberExpandedCloseZone(
    settings: com.agupta07505.smartisland.data.SmartIslandSettings,
    isLandscape: Boolean
): com.agupta07505.smartisland.model.ExpandedCloseZone {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val cutout = remember(context, configuration) { com.agupta07505.smartisland.util.CameraCutoutDetector.detect(context) }
    return remember(
        cutout, settings.width, settings.height, settings.yOffset, settings.enableNotchMode, isLandscape,
        settings.cameraCutoutManual, settings.cameraCutoutX, settings.cameraCutoutY,
        settings.cameraCutoutWidth, settings.cameraCutoutHeight
    ) {
        // The manual cutout area (Positions) replaces the detected one when hardware detection fails on a phone.
        // Notch mode has no camera hole of its own: the pill band is the close target, as without a cutout.
        val manual = settings.cameraCutoutManual && !settings.enableNotchMode
        com.agupta07505.smartisland.model.expandedCloseZone(
            hasHardwareCutout = !settings.enableNotchMode && (manual || cutout.hasHardwareCutout),
            cutoutWidthDp = if (manual) settings.cameraCutoutWidth else cutout.widthDp,
            cutoutHeightDp = if (manual) settings.cameraCutoutHeight else cutout.heightDp,
            cutoutXOffsetDp = if (manual) settings.cameraCutoutX else cutout.xOffsetDp,
            cutoutTopDp = if (manual) settings.cameraCutoutY else cutout.yOffsetDp,
            // The overlay window itself sits at the pill's Y offset in portrait (see the service's window params).
            windowTopDp = if (settings.enableNotchMode || isLandscape) 0f else settings.yOffset,
            pillWidthDp = settings.width,
            pillHeightDp = settings.height
        )
    }
}

@Composable
internal fun rememberCameraCutoutArea(
    settings: com.agupta07505.smartisland.data.SmartIslandSettings,
    isLandscape: Boolean
): com.agupta07505.smartisland.model.CutoutArea? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val cutout = remember(context, configuration) { com.agupta07505.smartisland.util.CameraCutoutDetector.detect(context) }
    return remember(
        cutout, isLandscape, settings.enableNotchMode, settings.cameraCutoutManual, settings.cameraCutoutX, settings.cameraCutoutY,
        settings.cameraCutoutWidth, settings.cameraCutoutHeight
    ) {
        com.agupta07505.smartisland.util.CameraCutoutDetector.areaFor(settings, cutout, isLandscape)
    }
}

/**
 * Portrait, island collapsed, centre band NOT overlapping the camera cutout: a transparent long-press target over the
 * camera cutout (hide / show the pill, or the configured long-press action).
 */
@Composable
internal fun CutoutLongPressTarget(
    enabled: Boolean,
    width: Dp,
    height: Dp,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { currentOnLongPress() })
            }
    )
}

/** Three short lines: the glyph of the secondary cutout while it is the sibling picker button. */
@Composable
internal fun SiblingPickerLines(open: Boolean) {
    val lineColor = Color.White.copy(alpha = if (open) 1f else 0.85f)
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(lineColor)
            )
        }
    }
}
