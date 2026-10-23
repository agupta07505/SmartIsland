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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.FloatingWindowRoute
import com.agupta07505.smartisland.model.UtilityGrid
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.model.floatingWindowRoute
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.util.InAppStopwatch
import com.agupta07505.smartisland.util.InAppTimer
import com.agupta07505.smartisland.util.UtilityActions
import com.agupta07505.smartisland.util.UtilityWidgets
import kotlinx.coroutines.delay

internal val UTILITY_CELL_HEIGHT = 56.dp
internal val UTILITY_CELL_GAP = 8.dp

internal val UTILITY_SLIDER_HEIGHT = 20.dp

/** Space between a slider and the grid. Two sliders and their gaps (46 dp) make a two-row grid exactly as tall as a two-row Apps page. */
internal val UTILITY_SLIDER_GAP = 3.dp

/** Width kept free down the right edge for the settings button (and the page dots) so no cell sits under them. */
internal val UTILITY_SETTINGS_GUTTER = 32.dp

internal fun utilitiesPageHeight(usedRows: Int): Dp =
    if (usedRows <= 0) 0.dp
    else UTILITY_CELL_HEIGHT * usedRows + UTILITY_CELL_GAP * (usedRows - 1) + (UTILITY_SLIDER_HEIGHT + UTILITY_SLIDER_GAP) * 2

internal fun utilityLabelRes(type: UtilityType): Int = when (type) {
    UtilityType.MobileData -> R.string.utility_mobile_data
    UtilityType.WiFi -> R.string.utility_wifi
    UtilityType.Hotspot -> R.string.utility_hotspot
    UtilityType.Flashlight -> R.string.utility_flashlight
    UtilityType.Calculator -> R.string.utility_calculator
    UtilityType.ClearCache -> R.string.utility_clear_cache
    UtilityType.ScreenRecorder -> R.string.utility_screen_recorder
    UtilityType.Screenshot -> R.string.utility_screenshot
    UtilityType.Timer -> R.string.utility_timer
    UtilityType.Stopwatch -> R.string.utility_stopwatch
    UtilityType.LockScreen -> R.string.utility_lock_screen
    UtilityType.VoiceRecorder -> R.string.utility_voice_recorder
    UtilityType.Camera -> R.string.utility_camera
    UtilityType.Bluetooth -> R.string.utility_bluetooth
    UtilityType.AirplaneMode -> R.string.utility_airplane_mode
    UtilityType.AutoRotate -> R.string.utility_auto_rotate
    UtilityType.DarkMode -> R.string.utility_dark_mode
    UtilityType.BatterySaver -> R.string.utility_battery_saver
    UtilityType.AppWidget -> R.string.utility_app_widget
}

internal fun utilityIcon(type: UtilityType): ImageVector = when (type) {
    UtilityType.MobileData -> Icons.Rounded.SignalCellularAlt
    UtilityType.WiFi -> Icons.Rounded.Wifi
    UtilityType.Hotspot -> Icons.Rounded.WifiTethering
    UtilityType.Flashlight -> Icons.Rounded.FlashlightOn
    UtilityType.Calculator -> Icons.Rounded.Calculate
    UtilityType.ClearCache -> Icons.Rounded.CleaningServices
    UtilityType.ScreenRecorder -> Icons.Rounded.Videocam
    UtilityType.Screenshot -> Icons.Rounded.Screenshot
    UtilityType.Timer -> Icons.Rounded.Timer
    UtilityType.Stopwatch -> Icons.Rounded.AvTimer
    UtilityType.LockScreen -> Icons.Rounded.Lock
    UtilityType.VoiceRecorder -> Icons.Rounded.Mic
    UtilityType.Camera -> Icons.Rounded.CameraAlt
    UtilityType.Bluetooth -> Icons.Rounded.Bluetooth
    UtilityType.AirplaneMode -> Icons.Rounded.AirplanemodeActive
    UtilityType.AutoRotate -> Icons.Rounded.ScreenRotation
    UtilityType.DarkMode -> Icons.Rounded.DarkMode
    UtilityType.BatterySaver -> Icons.Rounded.BatterySaver
    UtilityType.AppWidget -> Icons.Rounded.Widgets
}

/**
 * The Utilities page: a [UtilityGrid.COLUMNS] x [UtilityGrid.ROWS] grid of utilities and app widgets as the user
 * arranged them. Fills its launcher window, as tall as the rows in use.
 */
@Composable
internal fun UtilitiesContainer(settings: SmartIslandSettings, modifier: Modifier = Modifier) {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    val layout = remember(settings.utilityLayout) { UtilityLayout.decode(settings.utilityLayout) }
    // The second tab exists only while the first is full; the sideways scroll follows the tab count.
    val tabCount = UtilityLayout.tabCount(layout)
    val pagerState = rememberPagerState(pageCount = { tabCount })
    var calculatorChoices by remember { mutableStateOf<List<com.agupta07505.smartisland.data.LaunchableApp>?>(null) }
    var pickingTimer by remember { mutableStateOf(false) }
    // Rearranging: a long press lifts a utility, it follows the finger, and letting go drops it on the nearest cell (or trades
    // places). [dragStart] is where the finger was when the long press landed.
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    fun openCalculator(pkg: String) {
        val floating = settings.utilityCalculatorFloating &&
            floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode) != FloatingWindowRoute.Off
        UtilityActions.launchApp(context, pkg, floating)
    }

    fun rememberCalculator(pkg: String) {
        val repository = runCatching { SmartIslandRepositories.settingsRepository(context) }.getOrNull()
        if (repository != null) UtilityActions.launch { repository.setUtilityCalculatorPackage(pkg) }
    }

    fun onTap(item: UtilityPlacement) {
        if (item.type == UtilityType.Timer) {
            pickingTimer = true
            return
        }
        if (item.type != UtilityType.Calculator) {
            UtilityActions.run(context, item.type)
            return
        }
        val saved = settings.utilityCalculatorPackage
        if (UtilityActions.isInstalled(context, saved)) {
            openCalculator(saved)
            return
        }
        // First press (or the remembered app is gone): choose a calculator once, then it is remembered.
        val choices = UtilityActions.calculatorApps(context)
        when (choices.size) {
            0 -> android.widget.Toast.makeText(context, R.string.utility_no_calculator, android.widget.Toast.LENGTH_SHORT).show()
            1 -> { rememberCalculator(choices[0].packageName); openCalculator(choices[0].packageName) }
            else -> calculatorChoices = choices
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageMaxHeight = maxHeight
        val choices = calculatorChoices
        when {
            UtilityLayout.usedRows(layout) == 0 -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.utilities_title), color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.utilities_empty_desc), color = palette.textTertiary, fontSize = 13.sp)
                Text(
                    stringResource(R.string.utilities_empty_action),
                    color = palette.accentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { UtilityActions.openPicker(context, com.agupta07505.smartisland.util.PickerRequest.UTILITY) }
                )
            }
            choices != null -> CalculatorPicker(
                choices = choices,
                onPick = { pkg ->
                    calculatorChoices = null
                    rememberCalculator(pkg)
                    openCalculator(pkg)
                },
                onCancel = { calculatorChoices = null }
            )
            pickingTimer -> TimerPicker(
                onPick = { minutes ->
                    pickingTimer = false
                    UtilityActions.startTimer(context, minutes * 60)
                },
                onCancel = { pickingTimer = false }
            )
            else -> Column(Modifier.fillMaxSize().padding(top = 2.dp, end = UTILITY_SETTINGS_GUTTER, bottom = 6.dp)) {
            // The plus takes the first empty cell. On the first tab it never makes the page taller than the window
            // already is: a cell in the rows in use, or the next row if the window has room.
            val usedRows = UtilityLayout.usedRows(layout)
            fun plusCellFor(tab: Int): Pair<Int, Int>? {
                val base = tab * UtilityGrid.ROWS
                val rowsToScan = if (tab > 0) UtilityGrid.ROWS
                else if (usedRows < UtilityGrid.ROWS && pageMaxHeight >= utilitiesPageHeight(usedRows + 1) + 8.dp) usedRows + 1 else usedRows
                return (base until base + rowsToScan).firstNotNullOfOrNull { r ->
                    (0 until UtilityGrid.COLUMNS).firstOrNull { c -> UtilityLayout.itemAt(layout, r, c) == null }?.let { c -> r to c }
                }
            }
            val gridRows = maxOf(usedRows, (plusCellFor(0)?.first ?: -1) + 1)
            val gridHeight = UTILITY_CELL_HEIGHT * gridRows + UTILITY_CELL_GAP * (gridRows - 1)
            val autoBrightness by produceState(false) {
                while (true) { value = UtilityActions.autoBrightnessEnabled(context); delay(500) }
            }
            UtilitySlider(
                icon = if (autoBrightness) Icons.Rounded.BrightnessAuto else Icons.Rounded.BrightnessMedium,
                description = stringResource(R.string.utility_auto_brightness),
                read = { UtilityActions.brightnessFraction(context) },
                write = { UtilityActions.setBrightness(context, it) },
                onIconClick = { UtilityActions.toggleAutoBrightness(context) },
                iconActive = autoBrightness
            )
            Spacer(Modifier.height(UTILITY_SLIDER_GAP))
            // The sliders sit outside the sideways scroll, so they never move with it or compete with it for a drag.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight),
                userScrollEnabled = draggedKey == null,
                // Only one page is ever on screen at a time; keeping the other composed would run its tiles' polling for nothing.
                beyondViewportPageCount = 0
            ) { tab ->
            val tabBase = tab * UtilityGrid.ROWS
            BoxWithConstraints(Modifier.fillMaxWidth().height(gridHeight)) {
                val cellWidth = (maxWidth - UTILITY_CELL_GAP * (UtilityGrid.COLUMNS - 1)) / UtilityGrid.COLUMNS
                val density = LocalDensity.current
                val pitchX = with(density) { (cellWidth + UTILITY_CELL_GAP).toPx() }
                val pitchY = with(density) { (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP).toPx() }
                // What letting go of [item], [delta] px from where it rests, would leave behind; null when nothing changes.
                // A drop that needs another row does not count (the page is only as tall as the rows in use).
                fun landing(item: UtilityPlacement, delta: Offset): List<UtilityPlacement>? {
                    val (row, col) = UtilityLayout.snapCell(item, item.col * pitchX + delta.x, UtilityGrid.localRow(item.row) * pitchY + delta.y, pitchX, pitchY)
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
                    Box(
                        Modifier
                            .offset(x = (cellWidth + UTILITY_CELL_GAP) * plusCol, y = (UTILITY_CELL_HEIGHT + UTILITY_CELL_GAP) * (plusRow - tabBase))
                            .size(cellWidth, UTILITY_CELL_HEIGHT)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.text.copy(alpha = 0.06f))
                            .clickable { UtilityActions.openPicker(context, com.agupta07505.smartisland.util.PickerRequest.UTILITY) },
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
                layout.filter { it.tab == tab }.forEach { item ->
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
                            // A widget keeps its own touches (it is a real view); it is rearranged in the app's settings.
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
                                    // The long press has already buzzed (the tile's own long click), so lifting stays quiet.
                                    onLift = { draggedKey = item.key },
                                    onMove = { pointer -> dragDelta = pointer - dragStart },
                                    onEnd = { lifted, pointer ->
                                        val result = if (lifted) landing(item, pointer - dragStart) else null
                                        draggedKey = null
                                        dragDelta = Offset.Zero
                                        if (result != null) {
                                            val repository = runCatching { SmartIslandRepositories.settingsRepository(context) }.getOrNull()
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
            }
            }
            }

            Spacer(Modifier.height(UTILITY_SLIDER_GAP))
            val ringer by produceState(android.media.AudioManager.RINGER_MODE_NORMAL) {
                while (true) { value = UtilityActions.ringerMode(context); delay(500) }
            }
            UtilitySlider(
                icon = when (ringer) {
                    android.media.AudioManager.RINGER_MODE_SILENT -> Icons.Rounded.VolumeOff
                    android.media.AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration
                    else -> Icons.Rounded.VolumeUp
                },
                description = stringResource(R.string.utility_ringer),
                read = { UtilityActions.volumeFraction(context) },
                write = { UtilityActions.setVolume(context, it) },
                onIconClick = { UtilityActions.toggleRinger(context) },
                iconActive = ringer == android.media.AudioManager.RINGER_MODE_NORMAL
            )
            }
        }
        if (tabCount > 1 && layout.isNotEmpty() && choices == null && !pickingTimer) {
            UtilityTabDots(tabCount, pagerState.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
        }
    }
}

@Composable
private fun UtilityTabDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val palette = LocalIslandPalette.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(palette.text.copy(alpha = if (index == current) 0.7f else 0.25f))
            )
        }
    }
}

@Composable
private fun UtilitySlider(
    icon: ImageVector,
    description: String,
    read: () -> Float,
    write: (Float) -> Unit,
    onIconClick: (() -> Unit)? = null,
    // A dimmed icon tells that the button's setting is off.
    iconActive: Boolean = true
) {
    val palette = LocalIslandPalette.current
    val currentRead by androidx.compose.runtime.rememberUpdatedState(read)
    val currentWrite by androidx.compose.runtime.rememberUpdatedState(write)
    var shown by remember { androidx.compose.runtime.mutableFloatStateOf(read().coerceIn(0f, 1f)) }
    var dragging by remember { mutableStateOf(false) }
    var widthPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) shown = currentRead().coerceIn(0f, 1f)
            delay(400)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(UTILITY_SLIDER_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.text.copy(alpha = 0.10f))
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var owned = false
                    fun fractionAt(x: Float): Float = if (widthPx > 0) (x / widthPx).coerceIn(0f, 1f) else shown
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed && !owned) break
                        if (!change.pressed) {
                            if (!owned) {
                                val level = fractionAt(down.position.x)
                                shown = level
                                currentWrite(level)
                                change.consume()
                            }
                            break
                        }
                        total += change.position - change.previousPosition
                        if (!owned && total.getDistance() > viewConfiguration.touchSlop) {
                            // Sideways: ours. Mostly vertical: not a slider drag, leave it to the page's swipes.
                            if (kotlin.math.abs(total.x) >= kotlin.math.abs(total.y)) {
                                owned = true
                                dragging = true
                            } else {
                                break
                            }
                        }
                        if (owned) {
                            change.consume()
                            val level = fractionAt(change.position.x)
                            shown = level
                            currentWrite(level)
                        }
                    }
                    dragging = false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = shown)
                .background(palette.accentCyan.copy(alpha = 0.28f))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(30.dp)
                .then(if (onIconClick != null) Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onIconClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (iconActive) palette.accentCyan else palette.accentCyan.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * The corner button: opens the app on the App Shortcuts configuration. Drawn outside the scrolling page so it stays
 * in the window's top-right corner.
 */
@Composable
internal fun BoxScope.UtilitiesSettingsButton() {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(28.dp)
            .clip(CircleShape)
            .background(palette.text.copy(alpha = 0.14f))
            .clickable { UtilityActions.openShortcutSettings(context) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.utilities_open_settings),
            tint = palette.text,
            modifier = Modifier.size(16.dp)
        )
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
            // A long press does nothing by itself (it only lifts the tile for dragging), so letting go after one does
            // not run the utility. The drag is applied after the click so it sees the touch first.
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
    // Widgets load their lists a moment after they appear, so the check repeats while the page shows.
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
                (view as? AppWidgetHostView)?.let { UtilityWidgets.resize(it, width.value.toInt(), height.value.toInt()) }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
        )
        if (scrollsVertically) {
            // A widget that scrolls up and down keeps every vertical swipe for itself, so the page could not be left
            // by swiping on it.
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
    if (view is android.widget.StackView) return true
    if (view.canScrollVertically(1) || view.canScrollVertically(-1)) return true
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) if (hasVerticalScroll(view.getChildAt(i))) return true
    }
    return false
}

@Composable
private fun CalculatorPicker(
    choices: List<com.agupta07505.smartisland.data.LaunchableApp>,
    onPick: (String) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalIslandPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(end = UTILITY_SETTINGS_GUTTER),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.utility_pick_calculator),
            color = palette.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            choices.forEach { app ->
                Text(
                    text = app.label,
                    color = palette.text,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.text.copy(alpha = 0.10f))
                        .clickable { onPick(app.packageName) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        Text(
            stringResource(R.string.utility_pick_cancel),
            color = palette.accentCyan,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onCancel)
        )
    }
}

@Composable
private fun TimerPicker(onPick: (Int) -> Unit, onCancel: () -> Unit) {
    val palette = LocalIslandPalette.current
    Row(
        Modifier
            .fillMaxSize()
            .padding(end = UTILITY_SETTINGS_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityActions.TIMER_CHOICES_MINUTES.forEach { minutes ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(UTILITY_CELL_HEIGHT)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accentCyan.copy(alpha = 0.22f))
                    .clickable { onPick(minutes) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.utility_timer_minutes_short, minutes),
                    color = palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.text.copy(alpha = 0.14f))
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.utility_pick_cancel),
                tint = palette.text,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
