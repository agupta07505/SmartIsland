/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.NoCutoutPrompt
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.CutoutFit
import com.agupta07505.smartisland.model.PresetPlace
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.search.searchAnchor
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.CutoutAdjust
import com.agupta07505.smartisland.util.DetectedCutoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun RedetectCutoutDialog(
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    screenWidthDp: Float,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.redetect_cutout)) },
        text = { Text(stringResource(R.string.redetect_cutout_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                scope.launch {
                    // Drop every cached hole first
                    CameraCutoutDetector.invalidate()
                    val area = CameraCutoutDetector.detectedArea(
                        CameraCutoutDetector.detect(context)
                    )
                    val fit = CutoutFit.plan(screenWidthDp, area)
                    if (fit == null) {
                        context.showToast(context.getString(R.string.toast_redetect_none))
                    } else {
                        repository.applyCutoutFit(fit, edgeHole = area,
                            teardrop = CameraCutoutDetector.isTeardrop(context))
                        CameraCutoutDetector.invalidate()
                        val placeName = context.getString(
                            when (fit.place) {
                                PresetPlace.Left -> R.string.preset_left_corner
                                PresetPlace.Right -> R.string.preset_right_corner
                                PresetPlace.Centre -> R.string.preset_center_hole
                            }
                        )
                        context.showToast(context.getString(R.string.toast_redetect_done, placeName))
                    }
                }

            }) { Text(stringResource(R.string.redetect_cutout_yes)) }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
internal fun RedetectCutoutRow(
    configuration: Configuration,
    onRedetect: () -> Unit
) {
    val isPortraitNow = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    FilledTonalButton(
        onClick = onRedetect,
        enabled = isPortraitNow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.redetect_cutout))
    }
    SettingsDescription(stringResource(if (isPortraitNow) R.string.redetect_cutout_desc else R.string.redetect_cutout_rotate))
}

private const val STYLES_PER_ROW = 3

@Composable
internal fun CameraCutoutCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    screenWidthDp: Float,
    detected: DetectedCutoutInfo,
    localCutoutXState: MutableFloatState,
    localCutoutYState: MutableFloatState,
    localCutoutWidthState: MutableFloatState,
    localCutoutHeightState: MutableFloatState,
    cutoutDraggingState: MutableState<Boolean>,
    liveCutoutArea: (CutoutArea) -> Unit
) {
    var localCutoutX by localCutoutXState
    var localCutoutY by localCutoutYState
    var localCutoutWidth by localCutoutWidthState
    var localCutoutHeight by localCutoutHeightState
    var cutoutDragging by cutoutDraggingState
    val style = settings.cameraCutoutStyle
    val auto = style == SmartIslandSettings.CUTOUT_STYLE_AUTO
    val notch = settings.enableNotchMode

    // the island fades while a slider is dragged (the overlay reads this); never left on
    DisposableEffect(Unit) { onDispose { CutoutAdjust.dragging.value = false } }

    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (!detected.hasHardwareCutout && auto) {
                Text(
                    text = stringResource(R.string.cutout_no_detect_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .padding(10.dp)
                )
                Spacer(Modifier.height(10.dp))
            } else if (detected.hasHardwareCutout) {
                val place = stringResource(
                    when (SideCamera.sideOf(detected.centreXDp)) {
                        CameraSide.Left -> R.string.preset_left_corner
                        CameraSide.Right -> R.string.preset_right_corner
                        CameraSide.Centre -> R.string.preset_center_hole
                    }
                )
                HintText(stringResource(R.string.cutout_detected_line, place, detected.holeWidthDp.toInt()))
                Spacer(Modifier.height(8.dp))
            }
            Column(modifier = Modifier.searchAnchor(NoCutoutPrompt.ROW_KEY)) {
                Text(
                    text = stringResource(R.string.cutout_style_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SmartIslandSettings.CUTOUT_STYLE_AUTO to stringResource(
                            if (detected.hasHardwareCutout) R.string.cutout_style_auto
                            else R.string.cutout_style_auto_not_detected
                        ),
                        SmartIslandSettings.CUTOUT_STYLE_LEFT to stringResource(R.string.cutout_style_left),
                        SmartIslandSettings.CUTOUT_STYLE_CENTER to stringResource(R.string.cutout_style_center),
                        SmartIslandSettings.CUTOUT_STYLE_RIGHT to stringResource(R.string.cutout_style_right),
                        SmartIslandSettings.CUTOUT_STYLE_TEARDROP to stringResource(R.string.cutout_style_teardrop)
                    ).chunked(STYLES_PER_ROW).forEach { rowStyles ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowStyles.forEach { (value, label) ->
                                val selected = style == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .bounceClick {
                                            scope.launch {
                                                repository.setCameraCutoutStyle(value)
                                                val d = SmartIslandSettings.Default
                                                if (value == SmartIslandSettings.CUTOUT_STYLE_TEARDROP &&
                                                    !detected.hasHardwareCutout &&
                                                    settings.cameraCutoutWidth == d.cameraCutoutWidth
                                                ) {
                                                    repository.setCameraCutoutArea(
                                                        d.cameraCutoutX, settings.cameraCutoutY,
                                                        CutoutPlacement.TEARDROP_DEFAULT_WIDTH_DP,
                                                        settings.cameraCutoutHeight
                                                    )
                                                }
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                            // a short last row keeps the width of the others
                            repeat(STYLES_PER_ROW - rowStyles.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            val saveArea = {
                scope.launch {
                    repository.setCameraCutoutArea(localCutoutX, localCutoutY, localCutoutWidth, localCutoutHeight)
                    cutoutDragging = false
                    CutoutAdjust.dragging.value = false
                }
                Unit
            }
            val liveArea = { x: Float, y: Float, w: Float, h: Float ->
                cutoutDragging = true
                CutoutAdjust.dragging.value = true
                liveCutoutArea(CutoutArea(x, y, w, h))
            }

            if (auto && !detected.hasHardwareCutout) {
                Spacer(Modifier.height(8.dp))
                HintText(stringResource(R.string.cutout_assumed_note))
            }

            if (auto && !notch) {
                // the manual area: an absolute hole, kept for existing setups (notch mode ignores it)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.camera_cutout_manual_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HintText(stringResource(R.string.camera_cutout_manual_desc))
                    }
                    Switch(
                        checked = settings.cameraCutoutManual,
                        onCheckedChange = { checked -> scope.launch { repository.setCameraCutoutManual(checked) } }
                    )
                }
                if (settings.cameraCutoutManual) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    SliderSettingItem(
                        label = stringResource(R.string.camera_cutout_x),
                        value = localCutoutX,
                        range = (-(screenWidthDp / 2f)).coerceIn(SmartIslandSettings.MIN_CUTOUT_X, 0f)..(screenWidthDp / 2f)
                            .coerceIn(0f, SmartIslandSettings.MAX_CUTOUT_X),
                        onValueChange = { localCutoutX = it; liveArea(it, localCutoutY, localCutoutWidth,
                            localCutoutHeight) },
                        onValueChangeFinished = { saveArea() }
                    )
                    SettingsDivider(0.2f)
                    SliderSettingItem(
                        label = stringResource(R.string.camera_cutout_y),
                        value = localCutoutY,
                        range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                        onValueChange = { localCutoutY = it; liveArea(localCutoutX, it, localCutoutWidth,
                            localCutoutHeight) },
                        onValueChangeFinished = { saveArea() }
                    )
                    SettingsDivider(0.2f)
                    SliderSettingItem(
                        label = stringResource(R.string.camera_cutout_width),
                        value = localCutoutWidth,
                        range = SmartIslandSettings.MIN_CUTOUT_WIDTH..SmartIslandSettings.MAX_CUTOUT_WIDTH,
                        onValueChange = { localCutoutWidth = it; liveArea(localCutoutX, localCutoutY, it,
                            localCutoutHeight) },
                        onValueChangeFinished = { saveArea() }
                    )
                    SettingsDivider(0.2f)
                    SliderSettingItem(
                        label = stringResource(R.string.camera_cutout_height),
                        value = localCutoutHeight,
                        range = SmartIslandSettings.MIN_CUTOUT_HEIGHT..SmartIslandSettings.MAX_CUTOUT_HEIGHT,
                        onValueChange = { localCutoutHeight = it; liveArea(localCutoutX, localCutoutY,
                            localCutoutWidth, it) },
                        onValueChangeFinished = { saveArea() }
                    )
                }
            }

            if (!auto) {
                // a style: X from its anchor; a teardrop hangs from the top edge and has a width and a depth,
                // the others a Y and 1 radius (the hole is round)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                val teardrop = CutoutPlacement.isFlushWithTop(style)
                val radiusRange = CutoutPlacement.RADIUS_RANGE_DP
                val radius = (localCutoutWidth / 2f).coerceIn(radiusRange)
                // keep the hole on the screen
                val xRange = CutoutPlacement.xRangeDp(
                    style, screenWidthDp, if (teardrop) localCutoutWidth / 2f else radius
                )
                SliderSettingItem(
                    label = stringResource(R.string.cutout_x_offset),
                    value = localCutoutX.coerceIn(xRange),
                    range = xRange,
                    onValueChange = { localCutoutX = it; liveArea(it, localCutoutY, localCutoutWidth,
                        localCutoutHeight) },
                    onValueChangeFinished = { saveArea() }
                )
                SettingsDivider(0.2f)
                if (teardrop) {
                    SliderSettingItem(
                        label = stringResource(R.string.cutout_teardrop_width),
                        value = localCutoutWidth,
                        range = SmartIslandSettings.MIN_CUTOUT_WIDTH..SmartIslandSettings.MAX_CUTOUT_WIDTH,
                        onValueChange = { localCutoutWidth = it; liveArea(localCutoutX, localCutoutY, it,
                            localCutoutHeight) },
                        onValueChangeFinished = { saveArea() }
                    )
                    SettingsDivider(0.2f)
                    SliderSettingItem(
                        label = stringResource(R.string.cutout_teardrop_depth),
                        value = localCutoutHeight,
                        range = SmartIslandSettings.MIN_CUTOUT_HEIGHT..SmartIslandSettings.MAX_CUTOUT_HEIGHT,
                        onValueChange = { localCutoutHeight = it; liveArea(localCutoutX, localCutoutY,
                            localCutoutWidth, it) },
                        onValueChangeFinished = { saveArea() }
                    )
                } else {
                    SliderSettingItem(
                        label = stringResource(R.string.camera_cutout_y),
                        value = localCutoutY,
                        range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                        onValueChange = { localCutoutY = it; liveArea(localCutoutX, it, localCutoutWidth,
                            localCutoutHeight) },
                        onValueChangeFinished = { saveArea() }
                    )
                    SettingsDivider(0.2f)
                    SliderSettingItem(
                        label = stringResource(R.string.cutout_size_radius),
                        value = radius,
                        range = radiusRange,
                        onValueChange = {
                            localCutoutWidth = it * 2f
                            localCutoutHeight = it * 2f
                            liveArea(localCutoutX, localCutoutY, it * 2f, it * 2f)
                        },
                        onValueChangeFinished = { saveArea() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShadowCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    localShadowElevationState: MutableFloatState
) {
    var localShadowElevation by localShadowElevationState

    SettingsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.toggle_drop_shadow_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HintText(stringResource(R.string.toggle_drop_shadow_desc))
                }
                Switch(
                    checked = settings.enableShadow,
                    onCheckedChange = { checked ->
                        scope.launch { repository.setEnableShadow(checked) }
                    }
                )
            }

            AnimatedVisibility(visible = settings.enableShadow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_shadow_elevation),
                        value = localShadowElevation,
                        range = SmartIslandSettings.MIN_SHADOW_ELEVATION..SmartIslandSettings.MAX_SHADOW_ELEVATION,
                        suffix = " dp",
                        step = 1f,
                        onValueChange = { localShadowElevation = it },
                        onValueChangeFinished = {
                            scope.launch { repository.setShadowElevation(localShadowElevation) }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            6f to stringResource(R.string.shadow_subtle),
                            10f to stringResource(R.string.shadow_medium),
                            14f to stringResource(R.string.shadow_standard),
                            22f to stringResource(R.string.shadow_deep)
                        ).forEach { (targetVal, label) ->
                            val isSelected = abs(localShadowElevation - targetVal) < 0.5f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .bounceClick {
                                        localShadowElevation = targetVal
                                        scope.launch { repository.setShadowElevation(targetVal) }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${targetVal.toInt()}dp",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
