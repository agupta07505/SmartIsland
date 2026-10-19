/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalRight
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.Dimens
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.components.GroupedRow
import com.agupta07505.smartisland.ui.components.GroupedSection
import com.agupta07505.smartisland.ui.components.SegmentedControl
import com.agupta07505.smartisland.ui.components.SwitchRow
import com.agupta07505.smartisland.ui.semantic
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The Layout tab (Rule_Book 11.3): six inset grouped containers with all-caps headers, in this order:
 * Form Factor, Quick Presets, Geometry & Sizing, Aesthetics, Behavior and Motion Physics. Binary choices are
 * segmented controls, layout presets are one scrolling row of compact chips, and sliders carry no preset
 * buttons (their value sits on the label line, see [SliderSettingItem]).
 */
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
    var localHeight by remember(settings.height) { mutableFloatStateOf(settings.height) }
    var localXOffset by remember(settings.xOffset) { mutableFloatStateOf(settings.xOffset) }
    var localYOffset by remember(settings.yOffset) { mutableFloatStateOf(settings.yOffset) }
    var localCornerRadius by remember(settings.cornerRadius) { mutableFloatStateOf(settings.cornerRadius) }
    var localOpacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
    var localShadowElevation by remember(settings.shadowElevation) { mutableFloatStateOf(settings.shadowElevation) }
    var showPillColorDialog by remember { mutableStateOf(false) }

    if (showPillColorDialog) {
        RgbColorPickerDialog(
            title = stringResource(R.string.color_pill_background),
            initialColor = settings.pillColor,
            onDismiss = { showPillColorDialog = false },
            onSave = { color ->
                showPillColorDialog = false
                scope.launch { repository.setPillColor(color) }
            }
        )
    }

    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val calculatedLeftX = (-(screenWidthDp / 2f - 40f)).coerceIn(SmartIslandSettings.MIN_X_OFFSET, -30f)
    val calculatedRightX = (screenWidthDp / 2f - 40f).coerceIn(30f, SmartIslandSettings.MAX_X_OFFSET)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
    ) {
        // FORM FACTOR: pill or notch, and which side the companion bubble sits on.
        GroupedSection(header = stringResource(R.string.layout_header_form_factor).uppercase()) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                SegmentedControl(
                    options = listOf(
                        false to stringResource(R.string.layout_form_factor_pill),
                        true to stringResource(R.string.layout_form_factor_notch)
                    ),
                    selected = settings.enableNotchMode,
                    onSelect = { notch ->
                        if (notch == settings.enableNotchMode) return@SegmentedControl
                        scope.launch {
                            repository.setEnableNotchMode(notch)
                            if (notch) {
                                repository.setPosition(width = 175f, height = 35f, xOffset = 0f, yOffset = 0f)
                            } else {
                                repository.setPosition(width = 112f, height = 34f, xOffset = 0f, yOffset = 10f)
                            }
                            repository.setCornerRadius(20f)
                        }
                        Toast.makeText(
                            context,
                            context.getString(if (notch) R.string.toast_notch_mode_enabled else R.string.toast_notch_mode_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            if (!settings.enableNotchMode) {
                GroupedRow(
                    title = stringResource(R.string.layout_companion_side),
                    showChevron = false,
                    trailing = {
                        SegmentedControl(
                            options = listOf(
                                SmartIslandSettings.CIRCLE_POSITION_LEFT to stringResource(R.string.layout_companion_left),
                                SmartIslandSettings.CIRCLE_POSITION_RIGHT to stringResource(R.string.layout_companion_right)
                            ),
                            selected = settings.circlePosition,
                            onSelect = { side -> scope.launch { repository.setCirclePosition(side) } },
                            modifier = Modifier.width(150.dp)
                        )
                    }
                )
            }
        }

        // QUICK PRESETS: one horizontally scrolling row of compact chips.
        GroupedSection(header = stringResource(R.string.layout_header_quick_presets).uppercase()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    title = stringResource(R.string.preset_center_hole),
                    icon = Icons.Rounded.CenterFocusStrong,
                    isSelected = abs(settings.xOffset) < 5f && abs(settings.width - 112f) < 8f,
                    onClick = {
                        scope.launch {
                            repository.setPosition(width = 112f, height = 34f, xOffset = 0f, yOffset = 10f)
                            repository.setCornerRadius(20f)
                        }
                        Toast.makeText(context, context.getString(R.string.toast_applied_center_preset), Toast.LENGTH_SHORT).show()
                    }
                )
                PresetChip(
                    title = stringResource(R.string.preset_wide_island),
                    icon = Icons.Rounded.FitScreen,
                    isSelected = abs(settings.xOffset) < 5f && abs(settings.width - 150f) < 8f,
                    onClick = {
                        scope.launch {
                            repository.setPosition(width = 150f, height = 38f, xOffset = 0f, yOffset = 12f)
                            repository.setCornerRadius(22f)
                        }
                        Toast.makeText(context, context.getString(R.string.toast_applied_wide_preset), Toast.LENGTH_SHORT).show()
                    }
                )
                PresetChip(
                    title = stringResource(R.string.preset_left_corner),
                    icon = Icons.AutoMirrored.Rounded.AlignHorizontalLeft,
                    isSelected = settings.xOffset < -30f,
                    onClick = {
                        scope.launch {
                            repository.setPosition(width = 105f, height = 34f, xOffset = calculatedLeftX, yOffset = 10f)
                            repository.setCornerRadius(20f)
                        }
                        Toast.makeText(context, context.getString(R.string.toast_applied_left_preset, calculatedLeftX.toInt()), Toast.LENGTH_SHORT).show()
                    }
                )
                PresetChip(
                    title = stringResource(R.string.preset_right_corner),
                    icon = Icons.AutoMirrored.Rounded.AlignHorizontalRight,
                    isSelected = settings.xOffset > 30f,
                    onClick = {
                        scope.launch {
                            repository.setPosition(width = 105f, height = 34f, xOffset = calculatedRightX, yOffset = 10f)
                            repository.setCornerRadius(20f)
                        }
                        Toast.makeText(context, context.getString(R.string.toast_applied_right_preset, calculatedRightX.toInt()), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // GEOMETRY & SIZING: one continuous container of sliders.
        GroupedSection(header = stringResource(R.string.layout_header_geometry).uppercase()) {
            SliderSettingItem(
                label = stringResource(R.string.slider_island_width),
                value = localWidth,
                range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                onValueChange = { localWidth = it },
                onValueChangeFinished = { scope.launch { repository.setWidth(localWidth) } }
            )
            SliderSettingItem(
                label = stringResource(R.string.slider_island_height),
                value = localHeight,
                range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                onValueChange = { localHeight = it },
                onValueChangeFinished = { scope.launch { repository.setHeight(localHeight) } }
            )
            SliderSettingItem(
                label = stringResource(R.string.slider_horizontal_offset),
                value = localXOffset,
                range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                onValueChange = { localXOffset = it },
                onValueChangeFinished = { scope.launch { repository.setXOffset(localXOffset) } }
            )
            if (settings.enableNotchMode) {
                GroupedRow(
                    title = stringResource(R.string.slider_vertical_offset),
                    subtitle = stringResource(R.string.layout_y_offset_locked),
                    value = "0 dp",
                    showChevron = false
                )
            } else {
                SliderSettingItem(
                    label = stringResource(R.string.slider_vertical_offset),
                    value = localYOffset,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                    onValueChange = { localYOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setYOffset(localYOffset) } }
                )
            }
            SliderSettingItem(
                label = stringResource(R.string.slider_corner_radius),
                value = localCornerRadius,
                range = SmartIslandSettings.MIN_CORNER_RADIUS..SmartIslandSettings.MAX_CORNER_RADIUS,
                onValueChange = { localCornerRadius = it },
                onValueChangeFinished = { scope.launch { repository.setCornerRadius(localCornerRadius) } }
            )
        }

        // AESTHETICS: opacity, pill color and the drop shadow.
        GroupedSection(header = stringResource(R.string.layout_header_aesthetics).uppercase()) {
            SliderSettingItem(
                label = stringResource(R.string.slider_island_opacity),
                value = localOpacity * 100f,
                range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                suffix = "%",
                step = 5f,
                onValueChange = { localOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
                onValueChangeFinished = { scope.launch { repository.setOpacity(localOpacity) } }
            )
            val pillColor = Color(settings.pillColor)
            GroupedRow(
                title = stringResource(R.string.color_pill_background),
                subtitle = stringResource(R.string.color_pill_background_desc),
                onClick = { showPillColorDialog = true },
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pillColor,
                        border = BorderStroke(1.dp, MaterialTheme.semantic.separator)
                    ) {
                        Text(
                            text = String.format("#%06X", (settings.pillColor and 0xFFFFFFL)),
                            color = if (pillColor.red * 0.299 + pillColor.green * 0.587 + pillColor.blue * 0.114 > 0.5) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            )
            SwitchRow(
                title = stringResource(R.string.layout_drop_shadow),
                subtitle = stringResource(R.string.toggle_drop_shadow_desc),
                checked = settings.enableShadow,
                onCheckedChange = { checked -> scope.launch { repository.setEnableShadow(checked) } }
            )
            if (settings.enableShadow) {
                SliderSettingItem(
                    label = stringResource(R.string.slider_shadow_elevation),
                    value = localShadowElevation,
                    range = SmartIslandSettings.MIN_SHADOW_ELEVATION..SmartIslandSettings.MAX_SHADOW_ELEVATION,
                    onValueChange = { localShadowElevation = it },
                    onValueChangeFinished = { scope.launch { repository.setShadowElevation(localShadowElevation) } }
                )
            }
        }

        // BEHAVIOR
        GroupedSection(header = stringResource(R.string.layout_header_behavior).uppercase()) {
            SwitchRow(
                title = stringResource(R.string.toggle_show_in_landscape_title),
                subtitle = stringResource(R.string.toggle_show_in_landscape_desc),
                checked = settings.showInLandscape,
                onCheckedChange = { checked -> scope.launch { repository.setShowInLandscape(checked) } }
            )
            SwitchRow(
                title = stringResource(R.string.toggle_auto_expand_title),
                subtitle = stringResource(R.string.toggle_auto_expand_desc),
                checked = settings.autoExpandOnNotification,
                onCheckedChange = { checked -> scope.launch { repository.setAutoExpandOnNotification(checked) } }
            )
        }

        // MOTION PHYSICS: presets as one list, the kinetic glow in its own group below.
        MotionSettings(settings = settings, repository = repository)
    }
}

/**
 * Compact layout-preset chip for the scrolling row: icon and label in a 36dp capsule. Selected is the accent
 * at 15% with accent text and a checkmark, so it never relies on color alone.
 */
@Composable
private fun PresetChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.semantic.fill)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Rounded.Check else icon,
            contentDescription = null,
            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
