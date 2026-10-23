/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalRight
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Smartphone
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.bounceClick
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun PositionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    onNavigateToBackup: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var localWidth by remember(settings.width) { mutableFloatStateOf(settings.width) }
    var localContentPercent by remember(settings.contentPillWidthPercent) { mutableFloatStateOf(settings.contentPillWidthPercent) }
    var localHeight by remember(settings.height) { mutableFloatStateOf(settings.height) }
    var localXOffset by remember(settings.xOffset) { mutableFloatStateOf(settings.xOffset) }
    var localYOffset by remember(settings.yOffset) { mutableFloatStateOf(settings.yOffset) }
    var localCutoutX by remember(settings.cameraCutoutX) { mutableFloatStateOf(settings.cameraCutoutX) }
    var localCutoutY by remember(settings.cameraCutoutY) { mutableFloatStateOf(settings.cameraCutoutY) }
    var localCutoutWidth by remember(settings.cameraCutoutWidth) { mutableFloatStateOf(settings.cameraCutoutWidth) }
    var localCutoutHeight by remember(settings.cameraCutoutHeight) { mutableFloatStateOf(settings.cameraCutoutHeight) }
    var localCornerRadius by remember(settings.cornerRadius) { mutableFloatStateOf(settings.cornerRadius) }
    var localShadowElevation by remember(settings.shadowElevation) { mutableFloatStateOf(settings.shadowElevation) }

    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val calculatedLeftX = (-(screenWidthDp / 2f - 40f)).coerceIn(
        SmartIslandSettings.MIN_X_OFFSET,
        -30f
    )
    val calculatedRightX = ((screenWidthDp / 2f - 40f)).coerceIn(
        30f,
        SmartIslandSettings.MAX_X_OFFSET
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isNotch = settings.enableNotchMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isNotch) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .bounceClick {
                        if (isNotch) {
                            scope.launch {
                                repository.setEnableNotchMode(false)
                                repository.setPosition(
                                    width = 112f,
                                    height = 34f,
                                    xOffset = 0f,
                                    yOffset = 10f
                                )
                                repository.setCornerRadius(20f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_notch_mode_disabled), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = if (!isNotch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.preset_compact_pill),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!isNotch) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isNotch) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isNotch) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .bounceClick {
                        if (!isNotch) {
                            scope.launch {
                                repository.setEnableNotchMode(true)
                                repository.setPosition(
                                    width = 175f,
                                    height = 35f,
                                    xOffset = 0f,
                                    yOffset = 0f
                                )
                                repository.setCornerRadius(20f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_notch_mode_enabled), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitScreen,
                        contentDescription = null,
                        tint = if (isNotch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.notch_mode_card_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNotch) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNotch) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        CollapsibleCard(
            title = "Quick presets",
            stateKey = "pos_presets_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "One-tap layouts for common camera cutouts",
            initiallyExpanded = false,
            framed = false
        ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.presets_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${screenWidthDp.toInt()} dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isCenterSelected = abs(settings.xOffset) < 5f && abs(settings.width - 112f) < 8f
                    PresetChipItem(
                        title = stringResource(R.string.preset_center_hole),
                        icon = Icons.Rounded.CenterFocusStrong,
                        isSelected = isCenterSelected,
                        onClick = {
                            scope.launch {
                                repository.setPosition(
                                    width = 112f,
                                    height = 34f,
                                    xOffset = 0f,
                                    yOffset = 10f
                                )
                                repository.setCornerRadius(20f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_applied_center_preset), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isWideSelected = abs(settings.xOffset) < 5f && abs(settings.width - 150f) < 8f
                    PresetChipItem(
                        title = stringResource(R.string.preset_wide_island),
                        icon = Icons.Rounded.FitScreen,
                        isSelected = isWideSelected,
                        onClick = {
                            scope.launch {
                                repository.setPosition(
                                    width = 150f,
                                    height = 38f,
                                    xOffset = 0f,
                                    yOffset = 12f
                                )
                                repository.setCornerRadius(22f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_applied_wide_preset), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLeftSelected = settings.xOffset < -30f
                    PresetChipItem(
                        title = stringResource(R.string.preset_left_corner),
                        icon = Icons.AutoMirrored.Rounded.AlignHorizontalLeft,
                        isSelected = isLeftSelected,
                        onClick = {
                            scope.launch {
                                repository.setPosition(
                                    width = 105f,
                                    height = 34f,
                                    xOffset = calculatedLeftX,
                                    yOffset = 10f
                                )
                                repository.setCornerRadius(20f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_applied_left_preset, calculatedLeftX.toInt()), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isRightSelected = settings.xOffset > 30f
                    PresetChipItem(
                        title = stringResource(R.string.preset_right_corner),
                        icon = Icons.AutoMirrored.Rounded.AlignHorizontalRight,
                        isSelected = isRightSelected,
                        onClick = {
                            scope.launch {
                                repository.setPosition(
                                    width = 105f,
                                    height = 34f,
                                    xOffset = calculatedRightX,
                                    yOffset = 10f
                                )
                                repository.setCornerRadius(20f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_applied_right_preset, calculatedRightX.toInt()), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!settings.enableNotchMode) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.circle_position_card_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.circle_position_card_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val isLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLeft) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .bounceClick {
                                        scope.launch { repository.setCirclePosition(SmartIslandSettings.CIRCLE_POSITION_LEFT) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.circle_position_left),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLeft) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isLeft) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .bounceClick {
                                        scope.launch { repository.setCirclePosition(SmartIslandSettings.CIRCLE_POSITION_RIGHT) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.circle_position_right),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isLeft) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        CollapsibleCard(
            title = "Exempt apps",
            stateKey = "pos_exempt_apps",
            summary = "Apps that switch the island totally off in portrait",
            initiallyExpanded = false
        ) {
            ExemptAppsCard(settings = settings, repository = repository, landscape = false)
        }

        CollapsibleCard(
            title = "Size and position",
            stateKey = "pos_tuning_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "Width, height, offsets and corner radius",
            initiallyExpanded = false,
            framed = false
        ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.precision_tuning_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SliderSettingItem(
                    label = stringResource(R.string.slider_island_width),
                    value = localWidth,
                    range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                    onValueChange = { localWidth = it },
                    onValueChangeFinished = { scope.launch { repository.setWidth(localWidth) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_island_height),
                    value = localHeight,
                    range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                    onValueChange = { localHeight = it },
                    onValueChangeFinished = { scope.launch { repository.setHeight(localHeight) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Collapsed cutout with content: how wide the pill stretches to when a card is on it.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.content_pill_same_title),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.content_pill_same_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.contentPillSameAsIdle,
                        onCheckedChange = { scope.launch { repository.setContentPillSameAsIdle(false, it) } }
                    )
                }
                if (!settings.contentPillSameAsIdle) {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_content_pill_width),
                        value = localContentPercent,
                        range = SmartIslandSettings.MIN_CONTENT_PILL_PERCENT..SmartIslandSettings.MAX_CONTENT_PILL_PERCENT,
                        suffix = "%",
                        step = 5f,
                        onValueChange = { localContentPercent = it },
                        onValueChangeFinished = { scope.launch { repository.setContentPillWidthPercent(false, localContentPercent) } }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_horizontal_offset),
                    value = localXOffset,
                    range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                    onValueChange = { localXOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setXOffset(localXOffset) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                if (settings.enableNotchMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.slider_vertical_offset),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.y_offset_locked_notch_mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "0 dp",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_vertical_offset),
                        value = localYOffset,
                        range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                        onValueChange = { localYOffset = it },
                        onValueChangeFinished = { scope.launch { repository.setYOffset(localYOffset) } }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_corner_radius),
                    value = localCornerRadius,
                    range = SmartIslandSettings.MIN_CORNER_RADIUS..SmartIslandSettings.MAX_CORNER_RADIUS,
                    onValueChange = { localCornerRadius = it },
                    onValueChangeFinished = { scope.launch { repository.setCornerRadius(localCornerRadius) } }
                )
            }
        }
        }

        // Manual camera cutout area (portrait), for phones where the hardware detection fails.
        if (!settings.enableNotchMode) {
        CollapsibleCard(
            title = stringResource(R.string.camera_cutout_title),
            stateKey = "pos_camera_cutout",
            summary = stringResource(R.string.camera_cutout_summary),
            initiallyExpanded = false,
            framed = false
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                            Text(
                                text = stringResource(R.string.camera_cutout_manual_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        val saveArea = { scope.launch { repository.setCameraCutoutArea(localCutoutX, localCutoutY, localCutoutWidth, localCutoutHeight); Unit } }
                        SliderSettingItem(
                            label = stringResource(R.string.camera_cutout_x),
                            value = localCutoutX,
                            range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                            onValueChange = { localCutoutX = it },
                            onValueChangeFinished = { saveArea() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        SliderSettingItem(
                            label = stringResource(R.string.camera_cutout_y),
                            value = localCutoutY,
                            range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                            onValueChange = { localCutoutY = it },
                            onValueChangeFinished = { saveArea() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        SliderSettingItem(
                            label = stringResource(R.string.camera_cutout_width),
                            value = localCutoutWidth,
                            range = SmartIslandSettings.MIN_CUTOUT_WIDTH..SmartIslandSettings.MAX_CUTOUT_WIDTH,
                            onValueChange = { localCutoutWidth = it },
                            onValueChangeFinished = { saveArea() }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        SliderSettingItem(
                            label = stringResource(R.string.camera_cutout_height),
                            value = localCutoutHeight,
                            range = SmartIslandSettings.MIN_CUTOUT_HEIGHT..SmartIslandSettings.MAX_CUTOUT_HEIGHT,
                            onValueChange = { localCutoutHeight = it },
                            onValueChangeFinished = { saveArea() }
                        )
                    }
                }
            }
        }
        }

        CollapsibleCard(
            title = "Shadow",
            stateKey = "pos_look_${if (settings.enableNotchMode) "notch" else "pill"}",
            summary = "Drop shadow under the island (opacity and colour are in Colors)",
            initiallyExpanded = false,
            framed = false
        ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
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
                        Text(
                            text = stringResource(R.string.toggle_drop_shadow_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
    }
}

@Composable
internal fun PresetChipItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        label = "presetBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        label = "presetBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "presetIconTint"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .bounceClick(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}