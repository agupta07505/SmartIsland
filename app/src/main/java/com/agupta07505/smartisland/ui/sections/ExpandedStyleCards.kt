/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.ExpandedBlurMode
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SettingsGroupCard
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.glass.Card
import com.agupta07505.smartisland.ui.glass.Switch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ExpandedStyleCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    blurRadius: MutableFloatState,
    mediaOpacity: MutableFloatState,
    mediaBlurRadius: MutableFloatState,
    gradientStrength: MutableFloatState,
    onPickColor: (String, Int, Long) -> Unit
) {
    SettingsGroupCard(spacing = 10.dp) {
        Text(
            text = stringResource(R.string.expanded_style_card_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        ExpandedThemeBlock(settings, repository, scope)

        SettingsDivider(0.2f, modifier = Modifier.padding(vertical = 2.dp))

        ExpandedBlurBlock(settings, repository, scope, blurRadius)

        SettingsDivider(0.2f, modifier = Modifier.padding(vertical = 4.dp))

        MediaLookBlock(settings, repository, scope, mediaOpacity, mediaBlurRadius)

        SettingsDivider(0.2f, modifier = Modifier.padding(vertical = 4.dp))

        GradientBlock(settings, repository, scope, gradientStrength, onPickColor)
    }
}

@Composable
private fun ExpandedThemeBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.expanded_theme_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HintText(stringResource(R.string.expanded_theme_desc))
        ExpandedThemeSelector(
            selected = settings.expandedTheme,
            onSelected = { scope.launch { repository.setExpandedTheme(it) } }
        )
    }
}

@Composable
private fun ColumnScope.ExpandedBlurBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    blurState: MutableFloatState
) {
    var localBlurRadius by blurState
    // Background blur: Off or Card blur
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.toggle_expanded_blur_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HintText(stringResource(R.string.toggle_expanded_blur_desc))
        ExpandedBlurModeSelector(
            selected = settings.expandedBlurMode,
            onSelected = { mode -> scope.launch { repository.setExpandedBlurMode(mode) } }
        )
    }
    AnimatedVisibility(visible = settings.expandedBlurMode != ExpandedBlurMode.Off) {
        SliderSettingItem(
            label = stringResource(R.string.slider_expanded_blur_radius),
            value = localBlurRadius,
            range = SmartIslandSettings.MIN_EXPANDED_BLUR..SmartIslandSettings.MAX_EXPANDED_BLUR,
            suffix = " dp",
            step = 2f,
            onValueChange = { localBlurRadius = it },
            onValueChangeFinished = { scope.launch { repository.setExpandedBlurRadius(localBlurRadius) } }
        )
    }
}

@Composable
private fun ColumnScope.MediaLookBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    opacityState: MutableFloatState,
    blurRadiusState: MutableFloatState
) {
    var localMediaOpacity by opacityState
    var localMediaBlurRadius by blurRadiusState
    // The media (music) card has its own blur and opacity
    Text(
        text = stringResource(R.string.media_look_title),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    HintText(stringResource(R.string.media_look_desc))
    SliderSettingItem(
        label = stringResource(R.string.slider_media_opacity),
        value = localMediaOpacity * 100f,
        range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
        suffix = "%",
        step = 5f,
        onValueChange = { localMediaOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY,
            SmartIslandSettings.MAX_OPACITY) },
        onValueChangeFinished = { scope.launch { repository.setMediaOpacity(localMediaOpacity) } }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.toggle_media_blur_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = settings.mediaBlurEnabled,
            onCheckedChange = { checked -> scope.launch { repository.setMediaBlurEnabled(checked) } }
        )
    }
    AnimatedVisibility(visible = settings.mediaBlurEnabled) {
        SliderSettingItem(
            label = stringResource(R.string.slider_media_blur_radius),
            value = localMediaBlurRadius,
            range = SmartIslandSettings.MIN_EXPANDED_BLUR..SmartIslandSettings.MAX_EXPANDED_BLUR,
            suffix = " dp",
            step = 2f,
            onValueChange = { localMediaBlurRadius = it },
            onValueChangeFinished = { scope.launch { repository.setMediaBlurRadius(localMediaBlurRadius) } }
        )
    }
}

@Composable
private fun ColumnScope.GradientBlock(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    strengthState: MutableFloatState,
    onPickColor: (String, Int, Long) -> Unit
) {
    var localGradientStrength by strengthState
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.toggle_expanded_gradient_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HintText(stringResource(R.string.toggle_expanded_gradient_desc))
        }
        Switch(
            checked = settings.expandedGradientEnabled,
            onCheckedChange = { checked -> scope.launch { repository.setExpandedGradientEnabled(checked) } }
        )
    }
    AnimatedVisibility(visible = settings.expandedGradientEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.color_expanded_gradient),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                ColorPresetRow(
                    selectedColor = settings.expandedGradientColor,
                    swatches = GRADIENT_PRESET_COLORS,
                    onColorSelected = { scope.launch { repository.setExpandedGradientColor(it) } },
                    onCustomClicked = { onPickColor("gradient", R.string.color_expanded_gradient,
                        settings.expandedGradientColor) }
                )
            }
            SliderSettingItem(
                label = stringResource(R.string.slider_expanded_gradient_strength),
                value = localGradientStrength * 100f,
                range = (SmartIslandSettings.MIN_GRADIENT_STRENGTH * 100f)..(SmartIslandSettings.MAX_GRADIENT_STRENGTH * 100f),
                suffix = "%",
                step = 5f,
                onValueChange = { localGradientStrength = (it / 100f) },
                onValueChangeFinished =
                    { scope.launch { repository.setExpandedGradientStrength(localGradientStrength) } }
            )
        }
    }
}

@Composable
private fun ExpandedThemeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf(
        SmartIslandSettings.EXPANDED_THEME_DARK to stringResource(R.string.expanded_theme_dark),
        SmartIslandSettings.EXPANDED_THEME_LIGHT to stringResource(R.string.expanded_theme_light),
        SmartIslandSettings.EXPANDED_THEME_SYSTEM to stringResource(R.string.expanded_theme_system)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (id, label) ->
            val isSelected = selected == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandedBlurModeSelector(selected: ExpandedBlurMode, onSelected: (ExpandedBlurMode) -> Unit) {
    val options = listOf(
        ExpandedBlurMode.Off to stringResource(R.string.blur_mode_off),
        ExpandedBlurMode.Card to stringResource(R.string.blur_mode_card)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
