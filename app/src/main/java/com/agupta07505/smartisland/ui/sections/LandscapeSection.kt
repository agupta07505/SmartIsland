/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.LandscapeLayoutStyle
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.ui.search.searchAnchor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Landscape profile editor: shape, size */
@Composable
fun LandscapeSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Height of the screen in landscape is its short side
    val configuration = LocalConfiguration.current
    val landscapeHeightDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
    // Half the landscape width (the long side)
    val landscapeHalfWidthDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp) / 2f
    val circleYRange = (landscapeHeightDp / 2f).coerceIn(60f, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)

    val edgeLayout = LandscapeLayoutStyle.isEdge(settings.landscapeLayoutStyle)
    val edgeTuningState = remember(settings.edgePill) { mutableStateOf(settings.edgePill) }

    val circleSizeState = remember(settings.landscapeCircleSize) { mutableFloatStateOf(settings.landscapeCircleSize) }
    val circleOpacityState =
        remember(settings.landscapeCircleOpacity) { mutableFloatStateOf(settings.landscapeCircleOpacity) }
    val circleInsetState =
        remember(settings.landscapeCircleEdgeInset) { mutableFloatStateOf(settings.landscapeCircleEdgeInset) }

    val circleXAdjustState =
        remember(settings.landscapeCircleXAdjust) { mutableFloatStateOf(settings.landscapeCircleXAdjust) }
    val circleYAdjustState =
        remember(settings.landscapeCircleYAdjust) { mutableFloatStateOf(settings.landscapeCircleYAdjust) }

    val pillWidthState = remember(settings.landscapeWidth) { mutableFloatStateOf(settings.landscapeWidth) }
    val contentPercentState =
        remember(settings.contentPillWidthPercentLandscape) { mutableFloatStateOf(settings.contentPillWidthPercentLandscape) }
    val pillHeightState = remember(settings.landscapeHeight) { mutableFloatStateOf(settings.landscapeHeight) }
    val pillXState = remember(settings.landscapeXOffset) { mutableFloatStateOf(settings.landscapeXOffset) }
    val pillYState = remember(settings.landscapeYOffset) { mutableFloatStateOf(settings.landscapeYOffset) }
    val pillOpacityState = remember(settings.landscapeOpacity) { mutableFloatStateOf(settings.landscapeOpacity) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShowInLandscapeCard(settings = settings, repository = repository, scope = scope)

        LayoutStyleCard(settings = settings, repository = repository, scope = scope)

        // edge pill only: its own sliders
        if (edgeLayout) {
            EdgePillCard(
                edgeTuningState = edgeTuningState,
                settings = settings,
                repository = repository,
                scope = scope,
                context = context,
                landscapeHeightDp = landscapeHeightDp
            )
        }

        EdgeInsetCard(
            circleInsetState = circleInsetState,
            repository = repository,
            scope = scope,
            edgeLayout = edgeLayout
        )

        IdleCutoutCard(
            circleSizeState = circleSizeState,
            circleOpacityState = circleOpacityState,
            circleXAdjustState = circleXAdjustState,
            circleYAdjustState = circleYAdjustState,
            repository = repository,
            scope = scope,
            landscapeHalfWidthDp = landscapeHalfWidthDp,
            circleYRange = circleYRange
        )

        ContentPillCard(
            pillWidthState = pillWidthState,
            contentPercentState = contentPercentState,
            pillHeightState = pillHeightState,
            pillXState = pillXState,
            pillYState = pillYState,
            pillOpacityState = pillOpacityState,
            settings = settings,
            repository = repository,
            scope = scope,
            edgeLayout = edgeLayout,
            landscapeHeightDp = landscapeHeightDp,
            landscapeHalfWidthDp = landscapeHalfWidthDp
        )

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
private fun ShowInLandscapeCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
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
}

@Composable
private fun LayoutStyleCard(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope
) {
    LandscapeCard {
        GroupLabel(stringResource(R.string.landscape_layout_style_title))
        ChoiceRow(
            options = listOf(
                LandscapeLayoutStyle.CLASSIC to stringResource(R.string.landscape_layout_style_classic),
                LandscapeLayoutStyle.EDGE to stringResource(R.string.landscape_layout_style_edge)
            ),
            selected = settings.landscapeLayoutStyle,
            onSelect = { scope.launch { repository.setLandscapeLayoutStyle(it) } }
        )
        HintText(stringResource(R.string.landscape_layout_style_desc))
    }
}

@Composable
private fun EdgeInsetCard(
    circleInsetState: MutableFloatState,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    edgeLayout: Boolean
) {
    var circleInset by circleInsetState
    Box(modifier = Modifier.searchAnchor("layout.edge_inset")) {
        LandscapeCard {
            GroupLabel(if (edgeLayout) "Idle circle and bar distance" else stringResource(R.string.landscape_edge_inset_title))
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
}

@Composable
private fun IdleCutoutCard(
    circleSizeState: MutableFloatState,
    circleOpacityState: MutableFloatState,
    circleXAdjustState: MutableFloatState,
    circleYAdjustState: MutableFloatState,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    landscapeHalfWidthDp: Float,
    circleYRange: Float
) {
    var circleSize by circleSizeState
    var circleOpacity by circleOpacityState
    var circleXAdjust by circleXAdjustState
    var circleYAdjust by circleYAdjustState
    CollapsibleCard(
        title = "Idle cutout",
        stateKey = "land_idle",
        summary = "The small circle shown when nothing is on the island",
        initiallyExpanded = false
    ) {
        HintText("Sits on the camera cutout. With no cutout it starts mid-edge; use the vertical offset to adjust.")

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
                circleOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_CIRCLE_OPACITY,
                    SmartIslandSettings.MAX_OPACITY)
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
        SliderSettingItem(
            label = "Vertical offset (from the camera, or the edge middle)",
            value = circleYAdjust.coerceIn(-circleYRange, circleYRange),
            range = -circleYRange..circleYRange,
            onValueChange = { circleYAdjust = it },
            onValueChangeFinished = { scope.launch { repository.setLandscapeCircleYAdjust(circleYAdjust) } }
        )
    }
}

@Composable
private fun ContentPillCard(
    pillWidthState: MutableFloatState,
    contentPercentState: MutableFloatState,
    pillHeightState: MutableFloatState,
    pillXState: MutableFloatState,
    pillYState: MutableFloatState,
    pillOpacityState: MutableFloatState,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    edgeLayout: Boolean,
    landscapeHeightDp: Float,
    landscapeHalfWidthDp: Float
) {
    var pillWidth by pillWidthState
    var contentPercent by contentPercentState
    var pillHeight by pillHeightState
    var pillX by pillXState
    var pillY by pillYState
    var pillOpacity by pillOpacityState
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
            // An older "camera" choice behaves as top middle
            selected = settings.landscapePillPosition.takeIf {
                it == SmartIslandSettings.LANDSCAPE_POSITION_LEFT || it == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
            } ?: SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            onSelect = { scope.launch { repository.setLandscapePillPosition(it) } }
        )
        if (!edgeLayout) {
            HintText("Corner: the opened card moves up near the screen edge. Middle: it drops just below the bar.")
        }

        GroupDivider()
        GroupLabel("Size")
        SliderSettingItem(
            label = "Width",
            value = pillWidth,
            range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
            onValueChange = { pillWidth = it },
            onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(pillWidth,
                settings.landscapeHeight, settings.landscapeXOffset, settings.landscapeYOffset) } }
        )
        SliderSettingItem(
            label = "Height",
            value = pillHeight,
            range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
            onValueChange = { pillHeight = it },
            onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth,
                pillHeight, settings.landscapeXOffset, settings.landscapeYOffset) } }
        )

        if (!edgeLayout) {
            GroupDivider()
            GroupLabel("Size with a card")
            HintText("Idle-widget pill uses the width above. With a card it can stretch wider, then shrinks back.")
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
                    onValueChangeFinished = { scope.launch { repository.setContentPillWidthPercent(true,
                        contentPercent) } }
                )
            }

        }

        GroupDivider()
        GroupLabel("Fine tuning")
        SliderSettingItem(
            label = "Horizontal offset",
            value = pillX.coerceIn(-landscapeHalfWidthDp, landscapeHalfWidthDp),
            range = -landscapeHalfWidthDp..landscapeHalfWidthDp,
            onValueChange = { pillX = it },
            onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth,
                settings.landscapeHeight, pillX, settings.landscapeYOffset) } }
        )
        SliderSettingItem(
            label = "Vertical offset",
            value = pillY,
            range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET,
            onValueChange = { pillY = it },
            onValueChangeFinished = { scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth,
                settings.landscapeHeight, settings.landscapeXOffset, pillY) } }
        )
        if (settings.landscapePillPosition == SmartIslandSettings.LANDSCAPE_POSITION_LEFT ||
            settings.landscapePillPosition == SmartIslandSettings.LANDSCAPE_POSITION_RIGHT
        ) {
            OutlinedButton(onClick = {
                pillY = (landscapeHeightDp / 2f - settings.landscapeHeight / 2f)
                    .coerceIn(SmartIslandSettings.MIN_Y_OFFSET, SmartIslandSettings.MAX_LANDSCAPE_Y_OFFSET)
                scope.launch { repository.setLandscapePillGeometry(settings.landscapeWidth, settings.landscapeHeight,
                    settings.landscapeXOffset, pillY) }
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
}
