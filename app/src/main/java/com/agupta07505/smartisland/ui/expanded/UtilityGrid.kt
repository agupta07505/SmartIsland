/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.StackView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.UtilityGrid
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.ui.UtilityNavigation
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.util.InAppStopwatch
import com.agupta07505.smartisland.util.InAppTimer
import com.agupta07505.smartisland.util.PickerRequest
import com.agupta07505.smartisland.util.UtilityActions
import com.agupta07505.smartisland.util.UtilityWidgets
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.delay

@Composable
internal fun UtilityGridPage(
    tab: Int,
    layout: List<UtilityPlacement>,
    gridHeight: Dp,
    plusCellFor: (Int) -> Pair<Int, Int>?,
    draggedKeyState: MutableState<String?>,
    dragStartState: MutableState<Offset>,
    dragDeltaState: MutableState<Offset>,
    onTap: (UtilityPlacement) -> Unit
) {
    val palette = LocalIslandPalette.current
    val draggedKey by draggedKeyState
    val dragDelta by dragDeltaState
    val tabBase = tab * UtilityGrid.ROWS
    BoxWithConstraints(Modifier.fillMaxWidth().height(gridHeight)) {
        val cellWidth = (maxWidth - UTILITY_CELL_GAP * (UtilityGrid.COLUMNS - 1)) / UtilityGrid.COLUMNS
        val density = LocalDensity.current
        val pitchX = with(density) { (cellWidth + UTILITY_CELL_GAP).toPx() }
        val pitchY = with(density) { (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP).toPx() }
        // What letting go of [item], [delta] px from where it rests, would leave behind, null when nothing changes
        // A drop that needs another row does not count (the page is only as tall as the rows in use)
        fun landing(item: UtilityPlacement, delta: Offset): List<UtilityPlacement>? {
            val (row, col) = UtilityLayout.snapCell(item, item.col * pitchX + delta.x,
                UtilityGrid.localRow(item.row) * pitchY + delta.y, pitchX, pitchY)
            val result = UtilityLayout.drop(layout, item.key, row, col)
            return result.takeIf { it != layout && UtilityLayout.usedRows(it) <= UtilityLayout.usedRows(layout) }
        }
        val liftedItem = layout.firstOrNull { it.key == draggedKey && it.tab == tab }
        if (liftedItem != null) {
            landing(liftedItem, dragDelta)?.firstOrNull { it.key == liftedItem.key }?.let { spot ->
                Box(
                    Modifier
                        .offset(
                            x = (cellWidth + UTILITY_CELL_GAP) * spot.col,
                            y = (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP) * (spot.row - tabBase)
                        )
                        .size(
                            cellWidth * spot.colSpan + UTILITY_CELL_GAP * (spot.colSpan - 1),
                            UTILITY_CELL_HEIGHT * spot.rowSpan + UTILITY_CELL_GAP * (spot.rowSpan - 1)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accentCyan.copy(alpha = 0.16f))
                )
            }
        }
        plusCellFor(tab)?.let { (plusRow, plusCol) ->
            UtilityPlusCell(cellWidth, plusRow, plusCol, tabBase)
        }
        layout.filter { it.tab == tab }.forEach { item ->
            UtilityPageItem(
                item = item,
                cellWidth = cellWidth,
                tabBase = tabBase,
                layout = layout,
                pitchX = pitchX,
                pitchY = pitchY,
                landing = { placement, delta -> landing(placement, delta) },
                draggedKeyState = draggedKeyState,
                dragStartState = dragStartState,
                dragDeltaState = dragDeltaState,
                onTap = onTap
            )
        }
    }
}

@Composable
private fun UtilityPlusCell(cellWidth: Dp, plusRow: Int, plusCol: Int, tabBase: Int) {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    Box(
        Modifier
            .offset(x = (cellWidth + UTILITY_CELL_GAP) * plusCol,
                y = (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP) * (plusRow - tabBase))
            .size(cellWidth, UTILITY_CELL_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.text.copy(alpha = 0.06f))
            .clickable { UtilityNavigation.openPicker(context, PickerRequest.UTILITY) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.pin_apps_add),
            tint = palette.text.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun UtilityPageItem(
    item: UtilityPlacement,
    cellWidth: Dp,
    tabBase: Int,
    layout: List<UtilityPlacement>,
    pitchX: Float,
    pitchY: Float,
    landing: (UtilityPlacement, Offset) -> List<UtilityPlacement>?,
    draggedKeyState: MutableState<String?>,
    dragStartState: MutableState<Offset>,
    dragDeltaState: MutableState<Offset>,
    onTap: (UtilityPlacement) -> Unit
) {
    val context = LocalContext.current
    var draggedKey by draggedKeyState
    var dragStart by dragStartState
    var dragDelta by dragDeltaState
    val width = cellWidth * item.colSpan + UTILITY_CELL_GAP * (item.colSpan - 1)
    val height = UTILITY_CELL_HEIGHT * item.rowSpan + UTILITY_CELL_GAP * (item.rowSpan - 1)
    val isLifted = item.key == draggedKey
    Box(
        Modifier
            .offset(
                x = (cellWidth + UTILITY_CELL_GAP) * item.col,
                y = (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP) * (item.row - tabBase)
            )
            .size(width, height)
            .zIndex(if (isLifted) 1f else 0f)
            .graphicsLayer {
                translationX = if (isLifted) dragDelta.x else 0f
                translationY = if (isLifted) dragDelta.y else 0f
                val lift = if (isLifted) 1.08f else 1f
                scaleX = lift
                scaleY = lift
            }
    ) {
        if (item.type == UtilityType.AppWidget) {
            // A widget is a real view and keeps its own touches, it is rearranged in the app's settings
            WidgetTile(item.widgetId, width, height)
        } else {
            UtilityTile(
                item.type,
                onClick = { onTap(item) },
                gestureModifier = Modifier.longPressDrag(
                    key = listOf(item.key, layout, pitchX, pitchY),
                    topLeft = { Offset.Zero },
                    onStart = { start ->
                        dragStart = start
                        dragDelta = Offset.Zero
                    },
                    // The tile's own long click already buzzed
                    onLift = { draggedKey = item.key },
                    onMove = { pointer -> dragDelta = pointer - dragStart },
                    onEnd = { lifted, pointer ->
                        val result = if (lifted) landing(item, pointer - dragStart) else null
                        draggedKey = null
                        dragDelta = Offset.Zero
                        if (result != null) {
                            val repository = quietly { SmartIslandRepositories.settingsRepository(context) }
                            if (repository != null) UtilityActions.launch { repository.setUtilityLayout(result) }
                        }
                    },
                    onCancel = {
                        draggedKey = null
                        dragDelta = Offset.Zero
                    }
                )
            )
        }
    }
}

@Composable
private fun UtilityTile(type: UtilityType, onClick: () -> Unit, gestureModifier: Modifier = Modifier) {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    val active = utilityActive(type, context)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) palette.accentCyan.copy(alpha = 0.28f) else palette.text.copy(alpha = 0.10f))
            // A long press only lifts the tile, so letting go after one does not run the utility
            // The drag is applied after the click so it sees the touch first
            .combinedClickable(onClick = onClick, onLongClick = {})
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = utilityIcon(type),
            contentDescription = stringResource(utilityLabelRes(type)),
            tint = palette.accentCyan,
            modifier = Modifier.size(26.dp)
        )
    }
}

private val SWITCH_LIKE_UTILITIES = setOf(
    UtilityType.Flashlight, UtilityType.WiFi, UtilityType.MobileData, UtilityType.ScreenRecorder, UtilityType.Stopwatch,
    UtilityType.Timer, UtilityType.Hotspot, UtilityType.Bluetooth, UtilityType.AirplaneMode, UtilityType.AutoRotate,
    UtilityType.DarkMode, UtilityType.BatterySaver
)

@Composable
private fun utilityActive(type: UtilityType, context: Context): Boolean {
    if (type !in SWITCH_LIKE_UTILITIES) return false
    val on by produceState(false, type) {
        if (type == UtilityType.Flashlight) UtilityActions.watchTorch(context)
        while (true) {
            value = when (type) {
                UtilityType.Flashlight -> UtilityActions.torchOn
                UtilityType.WiFi -> UtilityActions.wifiEnabled(context)
                UtilityType.MobileData -> UtilityActions.mobileDataEnabled(context)
                UtilityType.ScreenRecorder -> UtilityActions.recording
                UtilityType.Stopwatch -> InAppStopwatch.isActive
                UtilityType.Timer -> InAppTimer.isActive
                UtilityType.Hotspot -> HotspotUtil.isOn(context)
                UtilityType.Bluetooth -> UtilityActions.bluetoothEnabled(context)
                UtilityType.AirplaneMode -> UtilityActions.airplaneModeEnabled(context)
                UtilityType.AutoRotate -> UtilityActions.autoRotateEnabled(context)
                UtilityType.DarkMode -> UtilityActions.darkModeEnabled(context)
                UtilityType.BatterySaver -> UtilityActions.batterySaverEnabled(context)
                else -> false
            }
            delay(500)
        }
    }
    return on
}

@Composable
private fun WidgetTile(widgetId: Int, width: Dp, height: Dp) {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        UtilityWidgets.acquire(context)
        onDispose { UtilityWidgets.release(context) }
    }
    val available = remember(widgetId) { UtilityWidgets.info(context, widgetId) != null }
    if (!available) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.text.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.utility_widget_unavailable), color = palette.textTertiary, fontSize = 12.sp)
        }
        return
    }
    var hostView by remember(widgetId) { mutableStateOf<View?>(null) }
    // Widgets load their lists a moment after they appear, so the check repeats while the page shows
    val scrollsVertically by produceState(false, hostView) {
        while (true) {
            value = hostView?.let(::hasVerticalScroll) ?: false
            delay(500)
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> (UtilityWidgets.createView(ctx, widgetId) ?: View(ctx)).also { hostView = it } },
            update = { view ->
                (view as? AppWidgetHostView)?.let { UtilityWidgets.resize(it, width.value.toInt(),
                    height.value.toInt()) }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
        )
        if (scrollsVertically) {
            // A widget that scrolls vertically keeps every vertical swipe, so the page could not be left by swiping on
            // it
            Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(WIDGET_SWIPE_EDGE).pageSwipeEdge())
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(WIDGET_SWIPE_EDGE).pageSwipeEdge())
        }
    }
}

private val WIDGET_SWIPE_EDGE = 18.dp

private fun Modifier.pageSwipeEdge(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
        } while (event.changes.any { it.pressed })
    }
}

private fun hasVerticalScroll(view: View): Boolean {
    if (view is StackView) return true
    if (view.canScrollVertically(1) || view.canScrollVertically(-1)) return true
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) if (hasVerticalScroll(view.getChildAt(i))) return true
    }
    return false
}
