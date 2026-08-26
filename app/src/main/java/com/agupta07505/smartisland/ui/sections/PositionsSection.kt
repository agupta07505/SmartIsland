/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PositionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    var localWidth by remember(settings.width) { mutableFloatStateOf(settings.width) }
    var localHeight by remember(settings.height) { mutableFloatStateOf(settings.height) }
    var localXOffset by remember(settings.xOffset) { mutableFloatStateOf(settings.xOffset) }
    var localYOffset by remember(settings.yOffset) { mutableFloatStateOf(settings.yOffset) }
    var localCornerRadius by remember(settings.cornerRadius) { mutableFloatStateOf(settings.cornerRadius) }
    var localOpacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }

    // Dynamically calculate responsive notch coordinates for the current device screen
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val calculatedLeftX = (-(screenWidthDp / 2f - 40f)).coerceIn(
        SmartIslandSettings.MIN_X_OFFSET,
        -30f
    )
    val calculatedRightX = ((screenWidthDp / 2f - 40f)).coerceIn(
        30f,
        SmartIslandSettings.MAX_X_OFFSET
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Quick Responsive Layout Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quick Layout Presets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instant 1-tap positioning calibrated to your device screen width (${screenWidthDp.toInt()} dp)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Presets 2x2 Responsive Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Center Hole & Wide Island
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isCenterSelected = abs(settings.xOffset) < 5f && abs(settings.width - 112f) < 8f
                        PresetCardItem(
                            title = "Center Hole",
                            subtitle = "Centered punch hole camera",
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
                                Toast.makeText(context, "Applied Center Hole preset", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val isWideSelected = abs(settings.xOffset) < 5f && abs(settings.width - 150f) < 8f
                        PresetCardItem(
                            title = "Wide Island",
                            subtitle = "Spacious expanded layout",
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
                                Toast.makeText(context, "Applied Wide Island preset", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Left Corner & Right Corner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isLeftSelected = settings.xOffset < -30f
                        PresetCardItem(
                            title = "Left Corner",
                            subtitle = "Left-aligned punch hole",
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
                                Toast.makeText(context, "Applied Left Corner preset (${calculatedLeftX.toInt()}dp)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val isRightSelected = settings.xOffset > 30f
                        PresetCardItem(
                            title = "Right Corner",
                            subtitle = "Right-aligned camera hole",
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
                                Toast.makeText(context, "Applied Right Corner preset (+${calculatedRightX.toInt()}dp)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Compact Pill
                    val isCompactSelected = abs(settings.xOffset) < 5f && abs(settings.width - 92f) < 6f
                    PresetCardItem(
                        title = "Compact Pill",
                        subtitle = "Minimal discreet footprint for small status bars",
                        icon = Icons.Rounded.Smartphone,
                        isSelected = isCompactSelected,
                        onClick = {
                            scope.launch {
                                repository.setPosition(
                                    width = 92f,
                                    height = 30f,
                                    xOffset = 0f,
                                    yOffset = 8f
                                )
                                repository.setCornerRadius(18f)
                            }
                            Toast.makeText(context, "Applied Compact Pill preset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Card 2: Precision Dimensions & Offsets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "Precision Sizing & Fine Tuning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fine-tune dimension millimeters and offsets with precision controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                SliderSettingItem(
                    label = "Island Width",
                    value = localWidth,
                    range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                    onValueChange = { localWidth = it },
                    onValueChangeFinished = { scope.launch { repository.setWidth(localWidth) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Island Height",
                    value = localHeight,
                    range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                    onValueChange = { localHeight = it },
                    onValueChangeFinished = { scope.launch { repository.setHeight(localHeight) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Horizontal (X) Offset",
                    value = localXOffset,
                    range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                    onValueChange = { localXOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setXOffset(localXOffset) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Vertical (Y) Offset",
                    value = localYOffset,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                    onValueChange = { localYOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setYOffset(localYOffset) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = "Corner Radius",
                    value = localCornerRadius,
                    range = SmartIslandSettings.MIN_CORNER_RADIUS..SmartIslandSettings.MAX_CORNER_RADIUS,
                    onValueChange = { localCornerRadius = it },
                    onValueChangeFinished = { scope.launch { repository.setCornerRadius(localCornerRadius) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SliderSettingItem(
                        label = "Island Opacity & Transparency",
                        value = (localOpacity * 100f),
                        range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                        suffix = "%",
                        step = 5f,
                        onValueChange = { localOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
                        onValueChangeFinished = { scope.launch { repository.setOpacity(localOpacity) } }
                    )

                    // Quick Opacity Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1.0f to "100%",
                            0.85f to "85%",
                            0.70f to "70%",
                            0.50f to "50%"
                        ).forEach { (targetVal, label) ->
                            val isSelected = abs(localOpacity - targetVal) < 0.04f
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        localOpacity = targetVal
                                        scope.launch { repository.setOpacity(targetVal) }
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Island Drop Shadow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Add a soft ambient drop shadow for a floating depth effect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.enableShadow,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setEnableShadow(checked) }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch { repository.resetPosition() }
                        Toast.makeText(context, "Position reset to factory defaults", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Factory Position", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PresetCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "presetBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "presetBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "presetIconTint"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
