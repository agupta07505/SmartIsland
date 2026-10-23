/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import com.agupta07505.smartisland.ui.glass.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.agupta07505.smartisland.ui.glass.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.FloatingWindowRoute
import com.agupta07505.smartisland.model.UtilityGrid
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.model.floatingWindowRoute
import com.agupta07505.smartisland.ui.expanded.utilityIcon
import com.agupta07505.smartisland.ui.pickers.CalculatorAppDialog
import com.agupta07505.smartisland.ui.pickers.UtilityAddFlow
import com.agupta07505.smartisland.ui.expanded.utilityLabelRes
import com.agupta07505.smartisland.util.UtilityWidgets
import kotlinx.coroutines.launch

private val EDITOR_CELL_HEIGHT = 56.dp
private val EDITOR_CELL_GAP = 8.dp

/** The Utilities page editor: a picture of the grid, one tab at a time. */
@Composable
internal fun UtilitiesEditorCard(settings: SmartIslandSettings, repository: SmartIslandSettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layout = remember(settings.utilityLayout) { UtilityLayout.decode(settings.utilityLayout) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    // The tab the picture shows. Selecting an item and tapping an empty cell of the other tab moves it there.
    var editorTab by remember { mutableIntStateOf(0) }
    val tabBase = editorTab * UtilityGrid.ROWS
    var addAt by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pickingCalculator by remember { mutableStateOf(false) }
    // Rearranging: a long press on an item lifts it, it follows the finger, and letting go drops it on the nearest cell (or
    // trades places with the item it lands on).
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current

    fun save(newLayout: List<UtilityPlacement>) {
        scope.launch { repository.setUtilityLayout(newLayout) }
    }

    val selected = layout.firstOrNull { it.key == selectedKey }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.utilities_editor_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.utilities_editor_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.utilities_editor_tab_one, R.string.utilities_editor_tab_two).forEachIndexed { index, label ->
                    FilterChip(
                        selected = editorTab == index,
                        onClick = { editorTab = index },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
            if (editorTab == 1) {
                Text(
                    text = stringResource(R.string.utilities_editor_tab_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val gridHeight = EDITOR_CELL_HEIGHT * UtilityGrid.ROWS + EDITOR_CELL_GAP * (UtilityGrid.ROWS - 1)
            BoxWithConstraints(Modifier.fillMaxWidth().height(gridHeight)) {
                val cellWidth = (maxWidth - EDITOR_CELL_GAP * (UtilityGrid.COLUMNS - 1)) / UtilityGrid.COLUMNS
                val density = LocalDensity.current
                val pitchX = with(density) { (cellWidth + EDITOR_CELL_GAP).toPx() }
                val pitchY = with(density) { (EDITOR_CELL_HEIGHT + EDITOR_CELL_GAP).toPx() }
                // What letting go of [item], [delta] px from where it rests, would leave behind; null when nothing would change.
                fun landing(item: UtilityPlacement, delta: Offset): List<UtilityPlacement>? {
                    val (row, col) = UtilityLayout.snapCell(item, item.col * pitchX + delta.x, UtilityGrid.localRow(item.row) * pitchY + delta.y, pitchX, pitchY)
                    return UtilityLayout.drop(layout, item.key, row, col).takeIf { it != layout }
                }
                val liftedItem = layout.firstOrNull { it.key == draggedKey && it.tab == editorTab }
                // Empty cells first, so an item drawn over them takes the tap.
                for (localRow in 0 until UtilityGrid.ROWS) for (c in 0 until UtilityGrid.COLUMNS) {
                    val r = tabBase + localRow
                    if (UtilityLayout.itemAt(layout, r, c) == null) {
                        Box(
                            Modifier
                                .offset(x = (cellWidth + EDITOR_CELL_GAP) * c, y = (EDITOR_CELL_HEIGHT + EDITOR_CELL_GAP) * localRow)
                                .size(cellWidth, EDITOR_CELL_HEIGHT)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable {
                                    val moving = selected
                                    if (moving != null) {
                                        save(UtilityLayout.move(layout, moving.key, r, c))
                                    } else {
                                        addAt = r to c
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (liftedItem != null) {
                    landing(liftedItem, dragDelta)?.firstOrNull { it.key == liftedItem.key }?.let { spot ->
                        Box(
                            Modifier
                                .offset(x = (cellWidth + EDITOR_CELL_GAP) * spot.col, y = (EDITOR_CELL_HEIGHT + EDITOR_CELL_GAP) * (spot.row - tabBase))
                                .size(
                                    cellWidth * spot.colSpan + EDITOR_CELL_GAP * (spot.colSpan - 1),
                                    EDITOR_CELL_HEIGHT * spot.rowSpan + EDITOR_CELL_GAP * (spot.rowSpan - 1)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        )
                    }
                }
                layout.filter { it.tab == editorTab }.forEach { item ->
                    val isSelected = item.key == selectedKey
                    val isLifted = item.key == draggedKey
                    Box(
                        Modifier
                            .offset(x = (cellWidth + EDITOR_CELL_GAP) * item.col, y = (EDITOR_CELL_HEIGHT + EDITOR_CELL_GAP) * (item.row - tabBase))
                            .size(
                                cellWidth * item.colSpan + EDITOR_CELL_GAP * (item.colSpan - 1),
                                EDITOR_CELL_HEIGHT * item.rowSpan + EDITOR_CELL_GAP * (item.rowSpan - 1)
                            )
                            .zIndex(if (isLifted) 1f else 0f)
                            .graphicsLayer {
                                translationX = if (isLifted) dragDelta.x else 0f
                                translationY = if (isLifted) dragDelta.y else 0f
                                val lift = if (isLifted) 1.06f else 1f
                                scaleX = lift
                                scaleY = lift
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                if (isSelected) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedKey = if (isSelected) null else item.key }
                            // The drag sits after the click so it is the inner node and sees the touch first.
                            .pointerInput(item.key, layout, pitchX, pitchY) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedKey = item.key
                                        dragDelta = Offset.Zero
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount
                                    },
                                    onDragEnd = {
                                        val result = landing(item, dragDelta)
                                        draggedKey = null
                                        dragDelta = Offset.Zero
                                        if (result != null) save(result)
                                    },
                                    onDragCancel = {
                                        draggedKey = null
                                        dragDelta = Offset.Zero
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Icon only, like the page itself; a widget also shows its size.
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                utilityIcon(item.type),
                                contentDescription = stringResource(utilityLabelRes(item.type)),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            if (item.type == UtilityType.AppWidget) {
                                Text(
                                    "${item.colSpan} × ${item.rowSpan}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selected != null) {
                SelectedItemControls(
                    item = selected,
                    layout = layout,
                    settings = settings,
                    onRemove = {
                        if (selected.type == UtilityType.AppWidget) UtilityWidgets.deleteId(context, selected.widgetId)
                        selectedKey = null
                        save(UtilityLayout.remove(layout, selected.key))
                    },
                    onResize = { rows, cols -> save(UtilityLayout.resize(layout, selected.key, rows, cols)) },
                    onChooseCalculator = { pickingCalculator = true },
                    onCalculatorFloating = { scope.launch { repository.setUtilityCalculatorFloating(it) } },
                    onForgetCalculator = { scope.launch { repository.setUtilityCalculatorPackage("") } }
                )
            }
        }
    }

    addAt?.let { (row, col) ->
        UtilityAddFlow(
            settings = settings,
            repository = repository,
            row = row,
            col = col,
            onPlaced = { selectedKey = it },
            onClose = { addAt = null }
        )
    }

    if (pickingCalculator) CalculatorAppDialog(repository = repository, onClose = { pickingCalculator = false })
}

@Composable
private fun SelectedItemControls(
    item: UtilityPlacement,
    layout: List<UtilityPlacement>,
    settings: SmartIslandSettings,
    onRemove: () -> Unit,
    onResize: (rows: Int, cols: Int) -> Unit,
    onChooseCalculator: () -> Unit,
    onCalculatorFloating: (Boolean) -> Unit,
    onForgetCalculator: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.utilities_editor_selected_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (item.type == UtilityType.AppWidget) {
            Stepper(
                label = stringResource(R.string.utilities_editor_rows),
                value = item.rowSpan,
                canDecrease = item.rowSpan > 1,
                canIncrease = UtilityLayout.canPlace(layout, item.copy(rowSpan = item.rowSpan + 1), item.key),
                onChange = { onResize(it, item.colSpan) }
            )
            Stepper(
                label = stringResource(R.string.utilities_editor_columns),
                value = item.colSpan,
                canDecrease = item.colSpan > 1,
                canIncrease = UtilityLayout.canPlace(layout, item.copy(colSpan = item.colSpan + 1), item.key),
                onChange = { onResize(item.rowSpan, it) }
            )
        }

        if (item.type == UtilityType.Calculator) {
            val chosen = settings.utilityCalculatorPackage
            val label = remember(chosen) {
                if (chosen.isBlank()) null
                else runCatching {
                    context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(chosen, 0)).toString()
                }.getOrNull()
            }
            Text(
                text = label ?: stringResource(R.string.utilities_editor_calculator_unset),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseCalculator) { Text(stringResource(R.string.utilities_editor_calculator_app)) }
                if (chosen.isNotBlank()) {
                    TextButton(onClick = onForgetCalculator) { Text(stringResource(R.string.utilities_editor_calculator_reset)) }
                }
            }
            val floatingAvailable = floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode) != FloatingWindowRoute.Off
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.utilities_editor_calculator_floating), color = MaterialTheme.colorScheme.onSurface)
                    if (!floatingAvailable) {
                        Text(
                            stringResource(R.string.utilities_editor_calculator_floating_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.utilityCalculatorFloating, onCheckedChange = onCalculatorFloating)
            }
        }

        OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.utilities_editor_remove)) }
    }
}

@Composable
private fun Stepper(label: String, value: Int, canDecrease: Boolean, canIncrease: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(value - 1) }, enabled = canDecrease) {
                Icon(Icons.Rounded.Remove, contentDescription = null)
            }
            Text(value.toString(), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(24.dp))
            IconButton(onClick = { onChange(value + 1) }, enabled = canIncrease) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
    }
}
