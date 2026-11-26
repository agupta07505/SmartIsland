/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ScreenRotation
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.FloatingWindowRoute
import com.agupta07505.smartisland.model.UtilityGrid
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityPlacement
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.model.floatingWindowRoute
import com.agupta07505.smartisland.ui.UtilityNavigation
import com.agupta07505.smartisland.util.PickerRequest
import com.agupta07505.smartisland.util.UtilityActions
import com.agupta07505.smartisland.util.quietly

internal val UTILITY_CELL_HEIGHT = 56.dp

internal val UTILITY_CELL_GAP = 8.dp

internal val UTILITY_SLIDER_HEIGHT = 20.dp

/** 2 sliders and their gaps (46 dp) make a two-row grid exactly as tall as a two-row Apps page. */
internal val UTILITY_SLIDER_GAP = 3.dp

/** Kept free for the settings button and page dots so no cell sits under them. */
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

/** [UtilityGrid.COLUMNS] x [UtilityGrid.ROWS] grid as the user arranged it, fills its launcher window. */
@Composable
internal fun UtilitiesContainer(settings: SmartIslandSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val layout = remember(settings.utilityLayout) { UtilityLayout.decode(settings.utilityLayout) }
    // second tab exists only while the first is full
    val tabCount = UtilityLayout.tabCount(layout)
    val pagerState = rememberPagerState(pageCount = { tabCount })
    var calculatorChoices by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    var pickingTimer by remember { mutableStateOf(false) }
    // Long press lifts a utility, letting go drops it on the nearest cell (or trades places)
    // [dragStart] is where the finger was when the long press landed
    val draggedKeyState = remember { mutableStateOf<String?>(null) }
    val dragStartState = remember { mutableStateOf(Offset.Zero) }
    val dragDeltaState = remember { mutableStateOf(Offset.Zero) }

    fun openCalculator(pkg: String) {
        val floating = settings.utilityCalculatorFloating &&
            floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode) != FloatingWindowRoute.Off
        UtilityActions.launchApp(context, pkg, floating)
    }

    fun rememberCalculator(pkg: String) {
        val repository = quietly { SmartIslandRepositories.settingsRepository(context) }
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
        // First press (or the app is gone): choose a calculator once, then it is remembered
        val choices = UtilityActions.calculatorApps(context)
        when (choices.size) {
            0 -> Toast.makeText(context, R.string.utility_no_calculator, Toast.LENGTH_SHORT).show()
            1 -> { rememberCalculator(choices[0].packageName); openCalculator(choices[0].packageName) }
            else -> calculatorChoices = choices
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageMaxHeight = maxHeight
        val choices = calculatorChoices
        when {
            UtilityLayout.usedRows(layout) == 0 -> UtilitiesEmpty()
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
            else -> UtilitiesPage(
                layout = layout,
                pagerState = pagerState,
                pageMaxHeight = pageMaxHeight,
                draggedKeyState = draggedKeyState,
                dragStartState = dragStartState,
                dragDeltaState = dragDeltaState,
                onTap = { item -> onTap(item) }
            )
        }
        if (tabCount > 1 && layout.isNotEmpty() && choices == null && !pickingTimer) {
            UtilityTabDots(tabCount, pagerState.currentPage,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
        }
    }
}

@Composable
private fun BoxScope.UtilitiesEmpty() {
    val palette = LocalIslandPalette.current
    val context = LocalContext.current
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.utilities_title), color = palette.text, fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.utilities_empty_desc), color = palette.textTertiary, fontSize = 13.sp)
        Text(
            stringResource(R.string.utilities_empty_action),
            color = palette.accentCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { UtilityNavigation.openPicker(context, PickerRequest.UTILITY) }
        )
    }
}

@Composable
private fun UtilitiesPage(
    layout: List<UtilityPlacement>,
    pagerState: PagerState,
    pageMaxHeight: Dp,
    draggedKeyState: MutableState<String?>,
    dragStartState: MutableState<Offset>,
    dragDeltaState: MutableState<Offset>,
    onTap: (UtilityPlacement) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(top = 2.dp, end = UTILITY_SETTINGS_GUTTER, bottom = 6.dp)) {
        // Takes the first empty cell, on the first tab it never makes the page taller than the window already is
        val usedRows = UtilityLayout.usedRows(layout)
        fun plusCellFor(tab: Int): Pair<Int, Int>? {
            val base = tab * UtilityGrid.ROWS
            val rowsToScan = if (tab > 0) UtilityGrid.ROWS
            else if (usedRows < UtilityGrid.ROWS
                && pageMaxHeight >= utilitiesPageHeight(usedRows + 1) + 8.dp) usedRows + 1 else usedRows
            return (base until base + rowsToScan).firstNotNullOfOrNull { r ->
                (0 until UtilityGrid.COLUMNS).firstOrNull { c -> UtilityLayout.itemAt(layout, r, c) == null }
                    ?.let { c -> r to c }
            }
        }
        val gridRows = maxOf(usedRows, (plusCellFor(0)?.first ?: -1) + 1)
        val gridHeight = UTILITY_CELL_HEIGHT * gridRows + UTILITY_CELL_GAP * (gridRows - 1)
        UtilityBrightnessSlider()
        Spacer(Modifier.height(UTILITY_SLIDER_GAP))
        // Sliders sit outside the sideways scroll so they never compete with it for a drag
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight),
                userScrollEnabled = draggedKeyState.value == null,
                // Keeping the other page composed would run its tiles' polling for nothing
                beyondViewportPageCount = 0
            ) { tab ->
                UtilityGridPage(
                    tab = tab,
                    layout = layout,
                    gridHeight = gridHeight,
                    plusCellFor = ::plusCellFor,
                    draggedKeyState = draggedKeyState,
                    dragStartState = dragStartState,
                    dragDeltaState = dragDeltaState,
                    onTap = onTap
                )
            }
        }

        Spacer(Modifier.height(UTILITY_SLIDER_GAP))
        UtilityRingerSlider()
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

/** Corner button: opens the app on the App Shortcuts configuration. */
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
            .clickable { UtilityNavigation.openShortcutSettings(context) },
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
