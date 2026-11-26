/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.CutoutFit
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.util.CameraCutoutDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun LayoutModeSwitch(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    configuration: Configuration,
    screenWidthDp: Float
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
                            // Back to the compact pill's own saved layout
                            val fallback = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                CutoutFit.plan(
                                    screenWidthDp,
                                    CameraCutoutDetector.detectedArea(
                                        CameraCutoutDetector.detect(context)
                                    )
                                )
                            } else null
                            repository.switchLayoutMode(toNotch = false, compactFallback = fallback)
                        }
                        context.showToast(context.getString(R.string.toast_notch_mode_disabled))
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
                            repository.switchLayoutMode(toNotch = true)
                        }
                        context.showToast(context.getString(R.string.toast_notch_mode_enabled))
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
}

@Composable
internal fun CirclePositionRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    cameraSide: CameraSide,
    sideActive: Boolean
) {
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
            HintText(stringResource(if (sideActive) R.string.circle_position_side_camera_note else R.string.circle_position_card_desc))
        }

        val isLeft = SideCamera.circleLeft(
            cameraSide, settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT
        )
        Row(
            modifier = Modifier
                .alpha(if (sideActive) 0.38f else 1f)
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
                        if (!sideActive) scope.launch { repository.setCirclePosition(SmartIslandSettings.CIRCLE_POSITION_LEFT) }
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
                        if (!sideActive) scope.launch { repository.setCirclePosition(SmartIslandSettings.CIRCLE_POSITION_RIGHT) }
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

@Composable
internal fun SizeAndPositionCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    screenWidthDp: Float,
    presetHole: CutoutArea?,
    sideActive: Boolean,
    localWidthState: MutableFloatState,
    localHeightState: MutableFloatState,
    localXOffsetState: MutableFloatState,
    localYOffsetState: MutableFloatState,
    localContentPercentState: MutableFloatState,
    localCornerRadiusState: MutableFloatState,
    livePill: (Float, Float, Float, Float) -> Unit,
    savePill: () -> Unit
) {
    var localWidth by localWidthState
    var localHeight by localHeightState
    var localXOffset by localXOffsetState
    var localYOffset by localYOffsetState
    var localCornerRadius by localCornerRadiusState

    SettingsCard {
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
                onValueChange = { localWidth = it; livePill(it, localHeight, localXOffset, localYOffset) },
                onValueChangeFinished = { savePill() }
            )
            SettingsDivider(0.2f)

            SliderSettingItem(
                label = stringResource(R.string.slider_island_height),
                value = localHeight,
                range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                onValueChange = { localHeight = it; livePill(localWidth, it, localXOffset, localYOffset) },
                onValueChangeFinished = { savePill() }
            )
            SettingsDivider(0.2f)

            ContentPillWidthRows(
                settings = settings,
                repository = repository,
                scope = scope,
                localContentPercentState = localContentPercentState
            )
            SettingsDivider(0.2f)

            val xNudge = SideCamera.nudgeRangeDp(presetHole?.widthDp ?: 0f)
            SliderSettingItem(
                label = stringResource(R.string.slider_horizontal_offset),
                value = if (sideActive) localXOffset.coerceIn(-xNudge.coerceAtLeast(1f),
                    xNudge.coerceAtLeast(1f)) else localXOffset,
                range = if (sideActive) {
                    -xNudge.coerceAtLeast(1f)..xNudge.coerceAtLeast(1f)
                } else {
                    (-(screenWidthDp / 2f)).coerceIn(SmartIslandSettings.MIN_X_OFFSET, 0f)..(screenWidthDp / 2f)
                        .coerceIn(0f, SmartIslandSettings.MAX_X_OFFSET)
                },
                enabled = !sideActive || xNudge >= 1f,
                onValueChange = { localXOffset = it; livePill(localWidth, localHeight, it, localYOffset) },
                onValueChangeFinished = { savePill() }
            )
            if (sideActive) {
                HintText(stringResource(R.string.x_offset_side_camera_note))
            }
            SettingsDivider(0.2f)

            if (settings.enableNotchMode) {
                LockedVerticalOffsetRow()
            } else {
                SliderSettingItem(
                    label = stringResource(R.string.slider_vertical_offset),
                    value = localYOffset,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                    onValueChange = { localYOffset = it; livePill(localWidth, localHeight, localXOffset, it) },
                    onValueChangeFinished = { savePill() }
                )
            }
            SettingsDivider(0.2f)

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

@Composable
private fun ContentPillWidthRows(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    localContentPercentState: MutableFloatState
) {
    var localContentPercent by localContentPercentState

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
            SettingsDescription(stringResource(R.string.content_pill_same_desc))
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
            onValueChangeFinished = { scope.launch { repository.setContentPillWidthPercent(false,
                localContentPercent) } }
        )
    }
}

@Composable
private fun LockedVerticalOffsetRow() {
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
}
