/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.DisplayKey
import com.agupta07505.smartisland.model.ExpandedCloseZone
import com.agupta07505.smartisland.model.expandedCloseZone
import com.agupta07505.smartisland.util.CameraCutoutDetector

/** Separate round tap target for the idle dot. */
@Composable
internal fun LandscapeIdleDotView(
    dot: IdleDotGeometry?,
    alpha: () -> Float,
    topShiftDp: () -> Float,
    color: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    // The fade is read at draw time
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

/** Portrait, island expanded: the glyph-free centre band of the pill. */
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

/** Where the expanded island's close target sits. */
@Composable
internal fun rememberExpandedCloseZone(
    settings: SmartIslandSettings,
    isLandscape: Boolean,
    cameraBand: CameraBand? = null,
    pillCentreXDp: Float = 0f
): ExpandedCloseZone {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val cutoutGeneration = CameraCutoutDetector.generation
    val cutout = remember(context, configuration, cutoutGeneration) { CameraCutoutDetector.detect(context) }
    return remember(
        cutout, settings.width, settings.height, settings.yOffset, settings.enableNotchMode, isLandscape,
        settings.cameraCutoutManual, settings.cameraCutoutStyle, settings.cameraCutoutX, settings.cameraCutoutY,
        settings.cameraCutoutWidth, settings.cameraCutoutHeight, cameraBand, pillCentreXDp, configuration.screenWidthDp
    ) {
        val placed = CutoutPlacement.area(settings, configuration.screenWidthDp.toFloat())
        expandedCloseZone(
            // The notch has no hole in landscape, the placed area is not its own
            hasHardwareCutout = (placed != null || cutout.hasHardwareCutout) &&
                !(settings.enableNotchMode && isLandscape),
            cutoutWidthDp = placed?.widthDp ?: cutout.widthDp,
            cutoutHeightDp = placed?.heightDp ?: cutout.heightDp,
            cutoutXOffsetDp = placed?.centreXDp ?: cutout.centreXDp,
            cutoutTopDp = placed?.topDp ?: cutout.yOffsetDp,
            windowTopDp = if (settings.enableNotchMode || isLandscape) 0f else settings.yOffset,
            pillWidthDp = settings.width,
            pillHeightDp = settings.height,
            cameraBand = if (isLandscape) null else cameraBand,
            pillCentreXDp = pillCentreXDp,
            screenWidthDp = configuration.screenWidthDp.toFloat()
        )
    }
}

@Composable
internal fun rememberCameraCutoutArea(
    settings: SmartIslandSettings,
    isLandscape: Boolean,
    withNotchHole: Boolean = false,
    assumeCentre: Boolean = false
): CutoutArea? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val metrics = context.resources.displayMetrics
    val displayKey = DisplayKey(
        metrics.widthPixels, metrics.heightPixels,
        (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: -1,
        metrics.densityDpi
    )
    val cutoutGeneration = CameraCutoutDetector.generation
    val cutout = remember(context, configuration, displayKey, cutoutGeneration) { CameraCutoutDetector.detect(context) }
    return remember(
        cutout, isLandscape, withNotchHole, assumeCentre, settings.enableNotchMode, settings.cameraCutoutManual,
            settings.cameraCutoutStyle, settings.cameraCutoutX, settings.cameraCutoutY,
        settings.cameraCutoutWidth, settings.cameraCutoutHeight, configuration.screenWidthDp
    ) {
        val screenWidthDp = configuration.screenWidthDp.toFloat()
        if (withNotchHole) CameraCutoutDetector.areaFor(settings, cutout, isLandscape, screenWidthDp, assumeCentre)
        else CameraCutoutDetector.layoutAreaFor(settings, cutout, isLandscape, screenWidthDp)
    }
}

/** Portrait, island collapsed, centre band not overlapping the camera cutout. */
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
