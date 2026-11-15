/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CutoutSize
import com.agupta07505.smartisland.model.PresetPlace
import com.agupta07505.smartisland.model.PresetPlan
import com.agupta07505.smartisland.ui.SettingsCard
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.CameraCutoutDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 60 dp for a teardrop whose phone has no detected cutout and whose width is still the default, else null. */
internal fun teardropDefaultWidthDp(settings: SmartIslandSettings, context: Context): Float? =
    if (settings.cameraCutoutStyle == SmartIslandSettings.CUTOUT_STYLE_TEARDROP &&
        settings.cameraCutoutWidth == SmartIslandSettings.Default.cameraCutoutWidth &&
        !CameraCutoutDetector.detect(context).hasHardwareCutout
    ) CutoutPlacement.TEARDROP_DEFAULT_WIDTH_DP else null

private suspend fun applyPreset(
    repository: SmartIslandSettingsRepository,
    plan: PresetPlan,
    place: PresetPlace,
    size: CutoutSize,
    settings: SmartIslandSettings,
    teardropWidthDp: Float? = null
) {
    val cutoutStyle = settings.cameraCutoutStyle
    plan.hole?.let { hole ->
        if (cutoutStyle != SmartIslandSettings.CUTOUT_STYLE_AUTO) {
            // a style wins over a manual area and measures X from its anchor, so the preset picks the style
            repository.setCameraCutoutStyle(
                when (place) {
                    PresetPlace.Left -> SmartIslandSettings.CUTOUT_STYLE_LEFT
                    PresetPlace.Right -> SmartIslandSettings.CUTOUT_STYLE_RIGHT
                    PresetPlace.Centre ->
                        if (cutoutStyle == SmartIslandSettings.CUTOUT_STYLE_TEARDROP) cutoutStyle
                        else SmartIslandSettings.CUTOUT_STYLE_CENTER
                }
            )
            if (cutoutStyle == SmartIslandSettings.CUTOUT_STYLE_TEARDROP && place == PresetPlace.Centre &&
                teardropWidthDp != null
            ) {
                repository.setCameraCutoutArea(
                    SmartIslandSettings.Default.cameraCutoutX, settings.cameraCutoutY, teardropWidthDp,
                    settings.cameraCutoutHeight
                )
            }
        } else {
            repository.setCameraCutoutArea(hole.centreXDp, hole.topDp, hole.widthDp, hole.heightDp)
            repository.setCameraCutoutManual(true)
        }
    }
    repository.setPosition(
        width = plan.fit.widthDp,
        height = size.heightDp,
        xOffset = plan.fit.xOffsetDp,
        yOffset = size.yOffsetDp
    )
    repository.setCornerRadius(size.cornerRadiusDp)
}

@Composable
internal fun QuickPresetsCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    configuration: Configuration,
    screenWidthDp: Float,
    centrePlan: PresetPlan,
    widePlan: PresetPlan,
    leftPlan: PresetPlan,
    rightPlan: PresetPlan,
    holeSize: CutoutSize,
    cameraSide: CameraSide,
    sideActive: Boolean,
    onRedetect: () -> Unit
) {
    SettingsCard {
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

            CentreWidePresetRow(
                settings = settings,
                repository = repository,
                scope = scope,
                context = context,
                centrePlan = centrePlan,
                widePlan = widePlan,
                holeSize = holeSize,
                sideActive = sideActive
            )

            SidePresetRow(
                settings = settings,
                repository = repository,
                scope = scope,
                context = context,
                leftPlan = leftPlan,
                rightPlan = rightPlan,
                holeSize = holeSize,
                cameraSide = cameraSide,
                sideActive = sideActive
            )

            if (!settings.enableNotchMode) {
                RedetectCutoutRow(configuration = configuration, onRedetect = onRedetect)
            }

            CirclePositionRow(
                settings = settings,
                repository = repository,
                scope = scope,
                cameraSide = cameraSide,
                sideActive = sideActive
            )
        }
    }
}

@Composable
private fun CentreWidePresetRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    centrePlan: PresetPlan,
    widePlan: PresetPlan,
    holeSize: CutoutSize,
    sideActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // On a wide hole the Centre and Wide fits come out the same width
        val plansCoincide = abs(centrePlan.fit.widthDp - widePlan.fit.widthDp) < 8f
        val wideLook = abs(settings.height - (holeSize.heightDp + 4f)) < 1.5f
        val centreLayoutNow = !sideActive && abs(settings.xOffset) < 5f
        val isWideSelected = centreLayoutNow && abs(settings.width - widePlan.fit.widthDp) < 8f
            && (!plansCoincide || wideLook)
        val isCenterSelected = centreLayoutNow && abs(settings.width - centrePlan.fit.widthDp) < 8f && !isWideSelected
        PresetChipItem(
            title = stringResource(R.string.preset_center_hole),
            icon = Icons.Rounded.CenterFocusStrong,
            isSelected = isCenterSelected,
            onClick = {
                scope.launch {
                    applyPreset(
                        repository, centrePlan, PresetPlace.Centre, holeSize, settings,
                        teardropWidthDp = teardropDefaultWidthDp(settings, context)
                    )
                }
                context.showToast(context.getString(R.string.toast_applied_center_preset))
            },
            modifier = Modifier.weight(1f)
        )

        PresetChipItem(
            title = stringResource(R.string.preset_wide_island),
            icon = Icons.Rounded.FitScreen,
            isSelected = isWideSelected,
            onClick = {
                scope.launch {
                    val size = CutoutSize(
                        heightDp = holeSize.heightDp + 4f,
                        yOffsetDp = holeSize.yOffsetDp + 2f,
                        cornerRadiusDp = holeSize.cornerRadiusDp + 2f
                    )
                    applyPreset(
                        repository, widePlan, PresetPlace.Centre, size, settings,
                        teardropWidthDp = teardropDefaultWidthDp(settings, context)
                    )
                }
                context.showToast(context.getString(R.string.toast_applied_wide_preset))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SidePresetRow(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    leftPlan: PresetPlan,
    rightPlan: PresetPlan,
    holeSize: CutoutSize,
    cameraSide: CameraSide,
    sideActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isLeftSelected = if (sideActive) {
            cameraSide == CameraSide.Left && abs(settings.width - leftPlan.fit.widthDp) < 8f
        } else settings.xOffset < -30f
        PresetChipItem(
            title = stringResource(R.string.preset_left_corner),
            icon = Icons.AutoMirrored.Rounded.AlignHorizontalLeft,
            isSelected = isLeftSelected,
            onClick = {
                scope.launch {
                    applyPreset(repository, leftPlan, PresetPlace.Left, holeSize, settings)
                }
                context.showToast(
                    context.getString(R.string.toast_applied_left_preset, leftPlan.fit.xOffsetDp.toInt())
                )
            },
            modifier = Modifier.weight(1f)
        )

        val isRightSelected = if (sideActive) {
            cameraSide == CameraSide.Right && abs(settings.width - rightPlan.fit.widthDp) < 8f
        } else settings.xOffset > 30f
        PresetChipItem(
            title = stringResource(R.string.preset_right_corner),
            icon = Icons.AutoMirrored.Rounded.AlignHorizontalRight,
            isSelected = isRightSelected,
            onClick = {
                scope.launch {
                    applyPreset(repository, rightPlan, PresetPlace.Right, holeSize, settings)
                }
                context.showToast(
                    context.getString(R.string.toast_applied_right_preset, rightPlan.fit.xOffsetDp.toInt())
                )
            },
            modifier = Modifier.weight(1f)
        )
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
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha =
            0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
