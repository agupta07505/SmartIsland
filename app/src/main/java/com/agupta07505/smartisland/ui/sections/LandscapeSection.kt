/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Landscape profile editor: shape, size, position and opacity are independent of portrait. Writes only the
 * `landscape*` settings; portrait fields are never touched here.
 */
@Composable
fun LandscapeSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    // Height of the screen in landscape is its short side, whichever way the phone is held now.
    val configuration = LocalConfiguration.current
    val landscapeHeightDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
    // Half the landscape width (the long side): the offsets can reach either edge of the screen.
    val landscapeHalfWidthDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp) / 2f

    var circleSize by remember(settings.landscapeCircleSize) { mutableFloatStateOf(settings.landscapeCircleSize) }
    var circleOpacity by remember(settings.landscapeCircleOpacity) { mutableFloatStateOf(settings.landscapeCircleOpacity) }
    var circleInset by remember(settings.landscapeCircleEdgeInset) { mutableFloatStateOf(settings.landscapeCircleEdgeInset) }
    var circleY by remember(settings.landscapeCircleYOffset) { mutableFloatStateOf(settings.landscapeCircleYOffset) }

    var circleXAdjust by remember(settings.landscapeCircleXAdjust) { mutableFloatStateOf(settings.landscapeCircleXAdjust) }
    var circleYAdjust by remember(settings.landscapeCircleYAdjust) { mutableFloatStateOf(settings.landscapeCircleYAdjust) }

    var pillWidth by remember(settings.landscapeWidth) { mutableFloatStateOf(settings.landscapeWidth) }
    var contentPercent by remember(settings.contentPillWidthPercentLandscape) { mutableFloatStateOf(settings.contentPillWidthPercentLandscape) }
    var pillHeight by remember(settings.landscapeHeight) { mutableFloatStateOf(settings.landscapeHeight) }
    var pillX by remember(settings.landscapeXOffset) { mutableFloatStateOf(settings.landscapeXOffset) }
    var pillY by remember(settings.landscapeYOffset) { mutableFloatStateOf(settings.landscapeYOffset) }
    var pillOpacity by remember(settings.landscapeOpacity) { mutableFloatStateOf(settings.landscapeOpacity) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The only "show in landscape" switch in the app.
        Box(modifier = Modifier.searchAnchor("layout.show_landscape")) {
            LandscapeCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.toggle_show_in_landscape_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.toggle_show_in_landscape_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.showInLandscape,
                        onCheckedChange = { scope.launch { repository.setShowInLandscape(it) } }
                    )
                }
            }
        }

        // Distance from the screen's side edge: one number shared by the idle circle and the bar.
        Box(modifier = Modifier.searchAnchor("layout.edge_inset")) {
            LandscapeCard {
                GroupLabel(stringResource(R.string.landscape_edge_inset_title))
                SliderSettingItem(
                    label = stringResource(R.string.landscape_edge_inset_label),
                    value = circleInset,
                    range = SmartIslandSettings.MIN_CIRCLE_EDGE_INSET..SmartIslandSettings.MAX_CIRCLE_EDGE_INSET,
                    onValueChange = { circleInset = it },
                    onValueChangeFinished = { scope.launch { repository.setLandscapeCircleEdgeInset(circleInset) } }
                )
                HintText(stringResource(R.string.landscape_edge_inset_desc))
            }
        }

        CollapsibleCard(
            title = "Idle cutout",
            stateKey = "land_idle",
            summary = "The small circle shown when nothing is on the island",
            initiallyExpanded = false
        ) {
            GroupLabel("Position")
            ChoiceRow(
                options = listOf(
                    SmartIslandSettings.LANDSCAPE_POSITION_AUTO to "Camera",
                    SmartIslandSettings.LANDSCAPE_POSITION_LEFT to "Left",
                    SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE to "Middle",
                    SmartIslandSettings.LANDSCAPE_POSITION_RIGHT to "Right"
                ),
                selected = settings.landscapeCirclePosition,
                onSelect = { scope.launch { repository.setLandscapeCirclePosition(it) } }
            )
            HintText("Camera sits on the camera cutout, or the top corner on the camera's side if none is reported.")

            GroupDivider()
            GroupLabel("Size and look")
            SliderSettingItem(
                label = "Size",
                value = circleSize,
                range = SmartIslandSettings.MIN_CIRCLE_SIZE..SmartIslandSettings.MAX_CIRCLE_SIZE,
                onValueChange = { circleSize = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapeCircleSize(circleSize) } }
            )
            SliderSettingItem(
                label = "Opacity",
                value = circleOpacity * 100f,
                range = (SmartIslandSettings.MIN_CIRCLE_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                suffix = "%",
                step = 5f,
                onValueChange = {
                    circleOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_CIRCLE_OPACITY, SmartIslandSettings.MAX_OPACITY)
                },
                onValueChangeFinished = { scope.launch { repository.setLandscapeCircleOpacity(circleOpacity) } }
            )

            GroupDivider()
            GroupLabel("Fine tuning")
            SliderSettingItem(
                label = "Horizontal offset",
                value = circleXAdjust.coerceIn(-landscapeHalfWidthDp, landscapeHalfWidthDp),
                range = -landscapeHalfWidthDp..landscapeHalfWidthDp,
                onValueChange = { circleXAdjust = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapeCircleXAdjust(circleXAdjust) } }
            )
            if (settings.landscapeCirclePosition == SmartIslandSettings.LANDSCAPE_POSITION_AUTO) {
                // On the camera the height comes from the cutout itself; this only nudges it.
                SliderSettingItem(
                    label = "Vertical fine-tune (from camera)",
                    value = circleYAdjust,
                    range = -VERTICAL_TUNE_RANGE_DP..VERTICAL_TUNE_RANGE_DP,
                    onValueChange = { circleYAdjust = it },
                    onValueChangeFinished = { scope.launch { repository.setLandscapeCircleYAdjust(circleYAdjust) } }
                )
            } else {
                SliderSettingItem(
                    label = "Vertical offset",
                    value = circleY,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
                    onValueChange = { circleY = it },
                    onValueChangeFinished = { scope.launch { repository.setLandscapeCircleYOffset(circleY) } }
                )
            }
            if (settings.landscapeCirclePosition == SmartIslandSettings.LANDSCAPE_POSITION_LEFT ||
                settings.landscapeCirclePosition == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            ) {
                OutlinedButton(onClick = {
                    circleY = (landscapeHeightDp / 2f - circleSize / 2f)
                        .coerceIn(SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
                    scope.launch { repository.setLandscapeCircleYOffset(circleY) }
                }) { Text("Centre on the side edge") }
            }
        }

        CollapsibleCard(
            title = "Cutout with content",
            stateKey = "land_content",
            summary = "The bar shown while a notification or event is on the island",
            initiallyExpanded = false
        ) {
            GroupLabel("Position")
            ChoiceRow(
                options = listOf(
                    SmartIslandSettings.LANDSCAPE_POSITION_LEFT to "Left",
                    SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE to "Top middle",
                    SmartIslandSettings.LANDSCAPE_POSITION_RIGHT to "Right"
                ),
                // An older "camera" choice behaves as top middle.
                selected = settings.landscapePillPosition.takeIf {
                    it == SmartIslandSettings.LANDSCAPE_POSITION_LEFT || it == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
                } ?: SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
                onSelect = { scope.launch { repository.setLandscapePillPosition(it) } }
            )
            HintText("In a corner the opened card moves up near the screen edge; in the middle it drops just below the bar.")

            GroupDivider()
            GroupLabel("Size")
            SliderSettingItem(
                label = "Width",
                value = pillWidth,
                range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                onValueChange = { pillWidth = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(pillWidth, settings.landscapeHeight, settings.landscapeXOffset, settings.landscapeYOffset) } }
            )
            SliderSettingItem(
                label = "Height",
                value = pillHeight,
                range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                onValueChange = { pillHeight = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth, pillHeight, settings.landscapeXOffset, settings.landscapeYOffset) } }
            )

            GroupDivider()
            GroupLabel("Size with a card")
            HintText("The pill that only carries the idle widgets has the width above. With a card on it, it can stretch wider and shrinks back when the card is put away.")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Same size as idle cutout", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = settings.contentPillSameAsIdleLandscape,
                    onCheckedChange = { scope.launch { repository.setContentPillSameAsIdle(true, it) } }
                )
            }
            if (!settings.contentPillSameAsIdleLandscape) {
                SliderSettingItem(
                    label = "Cutout width with content",
                    value = contentPercent,
                    range = SmartIslandSettings.MIN_CONTENT_PILL_PERCENT..SmartIslandSettings.MAX_CONTENT_PILL_PERCENT,
                    suffix = "%",
                    step = 5f,
                    onValueChange = { contentPercent = it },
                    onValueChangeFinished = { scope.launch { repository.setContentPillWidthPercent(true, contentPercent) } }
                )
            }

            GroupDivider()
            GroupLabel("Fine tuning")
            SliderSettingItem(
                label = "Horizontal offset",
                value = pillX.coerceIn(-landscapeHalfWidthDp, landscapeHalfWidthDp),
                range = -landscapeHalfWidthDp..landscapeHalfWidthDp,
                onValueChange = { pillX = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth, settings.landscapeHeight, pillX, settings.landscapeYOffset) } }
            )
            SliderSettingItem(
                label = "Vertical offset",
                value = pillY,
                range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
                onValueChange = { pillY = it },
                onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth, settings.landscapeHeight, settings.landscapeXOffset, pillY) } }
            )
            if (settings.landscapePillPosition == SmartIslandSettings.LANDSCAPE_POSITION_LEFT ||
                settings.landscapePillPosition == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            ) {
                OutlinedButton(onClick = {
                    pillY = (landscapeHeightDp / 2f - settings.landscapeHeight / 2f)
                        .coerceIn(SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
                    scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth, settings.landscapeHeight, settings.landscapeXOffset, pillY) }
                }) { Text("Centre on the side edge") }
            }

            GroupDivider()
            GroupLabel("Look")
            SliderSettingItem(
                label = "Opacity",
                value = pillOpacity * 100f,
                range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                suffix = "%",
                step = 5f,
                onValueChange = {
                    pillOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY)
                },
                onValueChangeFinished = { scope.launch { repository.setLandscapeOpacity(pillOpacity) } }
            )
        }

        CollapsibleCard(
            title = "Exempt apps",
            stateKey = "land_exempt_apps",
            summary = "Apps that switch the island totally off in landscape",
            initiallyExpanded = false
        ) {
            ExemptAppsCard(settings = settings, repository = repository, landscape = true)
        }
    }
}

@Composable
private fun LandscapeCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun ChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val VERTICAL_TUNE_RANGE_DP = 60f
