/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.EdgePillTuning
import com.agupta07505.smartisland.ui.SettingsDescription
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.ui.components.CollapsibleCard
import com.agupta07505.smartisland.ui.glass.Switch
import com.agupta07505.smartisland.util.CameraCutoutDetector
import com.agupta07505.smartisland.util.EdgePillFitReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun EdgePillCard(
    edgeTuningState: MutableState<EdgePillTuning>,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    landscapeHeightDp: Float
) {
    var edgeTuning by edgeTuningState
    val save = { scope.launch { repository.setEdgePill(edgeTuning) }; Unit }
    CollapsibleCard(
        title = "Edge pill",
        stateKey = "land_edge_pill",
        summary = "Size, position, look and gaps of the vertical pill (edge pill layout only)",
        initiallyExpanded = true
    ) {
        EdgePillSizeGroup(
            edgeTuningState = edgeTuningState,
            repository = repository,
            scope = scope,
            save = save
        )
        GroupDivider()
        EdgePillPositionGroup(edgeTuningState = edgeTuningState, save = save)
        GroupDivider()
        EdgePillLookGroup(
            edgeTuningState = edgeTuningState,
            settings = settings,
            repository = repository,
            scope = scope,
            context = context,
            save = save
        )
        GroupDivider()
        EdgePillBubbleGroup(
            edgeTuningState = edgeTuningState,
            repository = repository,
            scope = scope,
            save = save
        )
        GroupDivider()
        EdgePillCameraGroup(
            edgeTuningState = edgeTuningState,
            settings = settings,
            repository = repository,
            scope = scope,
            context = context,
            landscapeHeightDp = landscapeHeightDp,
            save = save
        )
            OutlinedButton(onClick = {
                edgeTuning = EdgePillTuning()
                scope.launch { repository.setEdgePill(edgeTuning) }
            }) { Text("Reset edge pill") }
    }
}

@Composable
private fun EdgePillSizeGroup(
    edgeTuningState: MutableState<EdgePillTuning>,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    save: () -> Unit
) {
    val T = EdgePillTuning
    var edgeTuning by edgeTuningState
    GroupLabel("Size")
    SliderSettingItem(
        label = "Width",
        value = edgeTuning.widthDp, range = T.WIDTH,
        onValueChange = { edgeTuning = edgeTuning.copy(widthDp = it) }, onValueChangeFinished = save
    )
    SliderSettingItem(
        label = "Height",
        value = edgeTuning.heightDp, range = T.HEIGHT,
        onValueChange = { edgeTuning = edgeTuning.copy(heightDp = it) }, onValueChangeFinished = save
    )
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
            SettingsDescription(stringResource(R.string.edge_content_pill_same_desc))
        }
        Switch(
            checked = edgeTuning.contentSameSize,
            onCheckedChange = {
                edgeTuning = edgeTuning.copy(contentSameSize = it)
                scope.launch { repository.setEdgePill(edgeTuning) }
            }
        )
    }
    if (!edgeTuning.contentSameSize) {
        SliderSettingItem(
            label = stringResource(R.string.slider_content_pill_width),
            value = edgeTuning.contentPercent, range = T.CONTENT_PERCENT,
            suffix = "%", step = 5f,
            onValueChange = { edgeTuning = edgeTuning.copy(contentPercent = it) }, onValueChangeFinished = save
        )
    }
    SliderSettingItem(
        label = "Corner radius",
        value = edgeTuning.cornerRadiusDp, range = T.RADIUS,
        onValueChange = { edgeTuning = edgeTuning.copy(cornerRadiusDp = it) }, onValueChangeFinished = save
    )
}

@Composable
private fun EdgePillPositionGroup(
    edgeTuningState: MutableState<EdgePillTuning>,
    save: () -> Unit
) {
    val T = EdgePillTuning
    var edgeTuning by edgeTuningState
    GroupLabel("Position")
    if (!edgeTuning.flatEdge) {
        SliderSettingItem(
            label = "Distance from the screen edge",
            value = edgeTuning.xOffsetDp, range = T.X_OFFSET,
            onValueChange = { edgeTuning = edgeTuning.copy(xOffsetDp = it) }, onValueChangeFinished = save
        )
    }
    SliderSettingItem(
        label = if (edgeTuning.flatEdge) "Vertical offset (from the screen centre)" else "Vertical offset (from the camera)",
        value = edgeTuning.yOffsetDp, range = T.Y_OFFSET,
        onValueChange = { edgeTuning = edgeTuning.copy(yOffsetDp = it) }, onValueChangeFinished = save
    )
}

@Composable
private fun EdgePillLookGroup(
    edgeTuningState: MutableState<EdgePillTuning>,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    save: () -> Unit
) {
    val T = EdgePillTuning
    var edgeTuning by edgeTuningState
    GroupLabel("Look")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Flat edge (notch look)", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(
            checked = edgeTuning.flatEdge,
            onCheckedChange = {
                // each look keeps its own thickness, distance, radius and offset
                val now = edgeTuning
                val toFlat = it
                val firstVisit = if (now.cameraManual) null else
                    EdgePillFitReader.fitted(context, now.copy(flatEdge = toFlat), settings)
                scope.launch { edgeTuning = repository.switchEdgeLook(now, toFlat, firstVisit) }
            }
        )
    }
    HintText("Flat on the camera edge, rounded inside. Turning it on moves the pill to 0 dp from the edge. Each look keeps its own size and position. Independent of portrait Notch mode.")
    SliderSettingItem(
        label = "Opacity",
        value = edgeTuning.opacity * 100f,
        range = (T.OPACITY.start * 100f)..(T.OPACITY.endInclusive * 100f),
        suffix = "%", step = 5f,
        onValueChange = { edgeTuning = edgeTuning.copy(opacity = it / 100f) }, onValueChangeFinished = save
    )
}

@Composable
private fun EdgePillBubbleGroup(
    edgeTuningState: MutableState<EdgePillTuning>,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    save: () -> Unit
) {
    val T = EdgePillTuning
    var edgeTuning by edgeTuningState
    GroupLabel("Second bubble and card")
    Text("Bubble position", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    ChoiceRow(
        options = listOf(
            T.BUBBLE_AUTO.toString() to "Auto",
            T.BUBBLE_ABOVE.toString() to "Top",
            T.BUBBLE_BELOW.toString() to "Bottom"
        ),
        selected = edgeTuning.bubblePos.toString(),
        onSelect = {
            edgeTuning = edgeTuning.copy(bubblePos = it.toInt())
            scope.launch { repository.setEdgePill(edgeTuning) }
        }
    )
    HintText("Auto uses the roomier side of the pill. Bubble size follows pill width.")
    SliderSettingItem(
        label = "Gap between the pill and the bubble",
        value = edgeTuning.bubbleGapDp, range = T.BUBBLE_GAP,
        onValueChange = { edgeTuning = edgeTuning.copy(bubbleGapDp = it) }, onValueChangeFinished = save
    )
    SliderSettingItem(
        label = "Gap between the pill and the open card",
        value = edgeTuning.cardGapDp, range = T.CARD_GAP,
        onValueChange = { edgeTuning = edgeTuning.copy(cardGapDp = it) }, onValueChangeFinished = save
    )
}

@Composable
private fun EdgePillCameraGroup(
    edgeTuningState: MutableState<EdgePillTuning>,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    scope: CoroutineScope,
    context: Context,
    landscapeHeightDp: Float,
    save: () -> Unit
) {
    val T = EdgePillTuning
    var edgeTuning by edgeTuningState
    GroupLabel("Camera position")
    ChoiceRow(
        options = listOf("auto" to "Auto (detected)", "manual" to "Manual"),
        selected = if (edgeTuning.cameraManual) "manual" else "auto",
        onSelect = {
            edgeTuning = edgeTuning.copy(cameraManual = it == "manual")
            scope.launch { repository.setEdgePill(edgeTuning) }
        }
    )
    if (edgeTuning.cameraManual) {
        val cornerY = T.DEFAULT_CAMERA_Y
        val presetOf = when {
            abs(edgeTuning.cameraYDp - cornerY) < 1f -> "top"
            abs(edgeTuning.cameraYDp - landscapeHeightDp / 2f) < 1f -> "mid"
            abs(edgeTuning.cameraYDp - (landscapeHeightDp - cornerY)) < 1f -> "bottom"
            else -> ""
        }
        ChoiceRow(
            options = listOf("top" to "Top corner", "mid" to "Middle", "bottom" to "Bottom corner"),
            selected = presetOf,
            onSelect = {
                val y = when (it) {
                    "top" -> cornerY
                    "mid" -> landscapeHeightDp / 2f
                    else -> landscapeHeightDp - cornerY
                }
                edgeTuning = edgeTuning.copy(cameraYDp = y)
                scope.launch { repository.setEdgePill(edgeTuning) }
            }
        )
        SliderSettingItem(
            label = "Camera height from the top (camera on the left edge)",
            value = edgeTuning.cameraYDp, range = 0f..landscapeHeightDp,
            onValueChange = { edgeTuning = edgeTuning.copy(cameraYDp = it) }, onValueChangeFinished = save
        )
        SliderSettingItem(
            label = "Camera size along the edge",
            value = edgeTuning.cameraLengthDp, range = T.CAMERA_LENGTH,
            onValueChange = { edgeTuning = edgeTuning.copy(cameraLengthDp = it) }, onValueChangeFinished = save
        )
        HintText("Measured with the camera on the left edge; the right edge mirrors it.")
    }
    OutlinedButton(onClick = {
        // re-detect also fits the pill to the camera
        val teardrop = CameraCutoutDetector.isTeardrop(context)
        val fitted = EdgePillFitReader.fitted(context, edgeTuning.copy(flatEdge = teardrop), settings)
        if (fitted == null) {
            context.showToast("No camera cutout found")
        } else {
            val now = edgeTuning
            scope.launch {
                if (now.flatEdge != teardrop) repository.switchEdgeLook(now, teardrop, fitted)
                edgeTuning = fitted.copy(cameraManual = false)
                repository.setEdgePill(edgeTuning)
            }
        }
    }) { Text("Re-detect camera cutout") }
    HintText("Fits the pill to the cutout the system reports in landscape. Near the edge's middle it centres on the camera; near the top or bottom it hangs from it, with glyph and value on the inner side.")
}
