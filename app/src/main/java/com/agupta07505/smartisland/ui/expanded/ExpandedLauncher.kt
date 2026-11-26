/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.PinnedApps
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.LauncherPage
import com.agupta07505.smartisland.model.LauncherSwipe
import com.agupta07505.smartisland.model.UtilityLayout
import com.agupta07505.smartisland.model.UtilityType
import com.agupta07505.smartisland.model.launcherPageAfter
import com.agupta07505.smartisland.model.launcherSwipeFor
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.ui.IslandText as Text

internal val LocalReorderPinnedApps = staticCompositionLocalOf<(List<String>) -> Unit> { {} }

private fun launcherRows(count: Int): Int = ((count + 3) / 4).coerceIn(1, 2)

private val LAUNCHER_TILE_HEIGHT = 76.dp

private val LAUNCHER_ROW_GAP = 14.dp

private val LAUNCHER_PAGE_EXTRA = 8.dp

private val LAUNCHER_FOLDER_PANEL_HEIGHT = 108.dp

internal const val FOLDER_DWELL_MS = 1500L

@Composable
internal fun EmptyExpanded(
    settings: SmartIslandSettings,
    apps: List<LaunchableApp>?,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val loadedApps = apps
        if (loadedApps == null) {
            // Keep the configured launcher visually clean while PackageManager and
            // UsageStats are queried. In particular, do not flash the setup state.
            Spacer(Modifier.height(84.dp))
        } else {
            val pinnedOrder = settings.shortcutPackages
            val pinnedApps = loadedApps.filter { it.packageName in pinnedOrder }
            val recentApps = if (settings.showRecentApps) {
                loadedApps.filter { it.packageName !in pinnedOrder }.take(PinnedApps.MAX)
            } else {
                emptyList()
            }
            val utilityLayout = remember(settings.utilityLayout) { UtilityLayout.decode(settings.utilityLayout) }
            // the Utilities page is only as tall as the rows in use
            val utilityRows = UtilityLayout.usedRows(utilityLayout).let { used ->
                val needsPicker = settings.utilityCalculatorPackage.isBlank() &&
                    utilityLayout.any { it.type == UtilityType.Calculator }
                if (needsPicker) maxOf(used, 3) else used
            }
            // 1 window height for every page, so swiping never resizes the island
            val rows = maxOf(launcherRows(pinnedApps.size),
                if (settings.showRecentApps) launcherRows(recentApps.size) else 1)
            // a little extra under every page
            val pageHeight = maxOf(
                LAUNCHER_TILE_HEIGHT * rows + LAUNCHER_ROW_GAP * (rows - 1),
                utilitiesPageHeight(utilityRows),
                if (pinnedOrder.any(PinnedApps::isFolder)) LAUNCHER_FOLDER_PANEL_HEIGHT else 0.dp
            ) + LAUNCHER_PAGE_EXTRA
            // The plus tile (pin more apps) takes the first empty slot
            val plusRows = launcherRows(pinnedApps.size + 1)
            val canAddPinned = pinnedOrder.size < PinnedApps.MAX &&
                LAUNCHER_TILE_HEIGHT * plusRows + LAUNCHER_ROW_GAP * (plusRows - 1) + LAUNCHER_PAGE_EXTRA <= pageHeight
            LauncherPager(
                recentsEnabled = settings.showRecentApps,
                pageHeight = pageHeight,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    LauncherPage.Apps ->
                        if (pinnedApps.isEmpty() && !(pinnedOrder.isEmpty() && canAddPinned)) {
                            val nothingPinned = pinnedOrder.isEmpty()
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                if (nothingPinned) {
                                    Text(stringResource(R.string.shortcuts_title),
                                        color = LocalIslandPalette.current.text, fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.shortcuts_desc),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        stringResource(R.string.quick_launch_open_settings),
                                        color = LocalIslandPalette.current.accentCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(top = 16.dp)
                                            .clickable { onLaunchApp(context.packageName) }
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.quick_launch_apps_unavailable),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            LauncherGrid(pinnedApps, settings, onLaunchApp, onAppAction, canAdd = canAddPinned)
                        }
                    LauncherPage.Recents ->
                        if (recentApps.isEmpty()) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                if (AppShortcutProvider.hasUsageAccess(context)) {
                                    Text(
                                        stringResource(R.string.recent_apps_empty),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    // Recent apps come from usage access
                                    Text(
                                        stringResource(R.string.toggle_recent_apps_desc_needed),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        stringResource(R.string.recent_apps_grant_access),
                                        color = LocalIslandPalette.current.accentCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .clickable {
                                                runCatchingLogged("ExpandedLauncher", "Could not open the usage access settings") {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        } else {
                            LauncherGrid(recentApps, settings, onLaunchApp, onAppAction)
                        }
                    LauncherPage.Utilities -> UtilitiesContainer(settings)
                }
            }
        }
    }
}

@Composable
private fun LauncherPager(
    recentsEnabled: Boolean,
    pageHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (LauncherPage) -> Unit
) {
    var page by remember { mutableStateOf(LauncherPage.Apps) }
    var lastAppsLevelPage by remember { mutableStateOf(LauncherPage.Apps) }
    var swipe by remember { mutableStateOf(LauncherSwipe.Up) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { LAUNCHER_SWIPE_THRESHOLD.toPx() }
    val shownPage = if (page == LauncherPage.Recents && !recentsEnabled) LauncherPage.Apps else page
    val onSwipe by rememberUpdatedState<(LauncherSwipe) -> Unit> { direction ->
        val target = launcherPageAfter(shownPage, direction, recentsEnabled, lastAppsLevelPage)
        if (target != shownPage) {
            if (shownPage != LauncherPage.Utilities) lastAppsLevelPage = shownPage
            swipe = direction
            page = target
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .height(pageHeight)
            .clipToBounds()
            .launcherSwipes(thresholdPx) { onSwipe(it) }
    ) {
        AnimatedContent(
            targetState = shownPage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                // Icons scroll with the swipe
                val spec = tween<IntOffset>(
                    durationMillis = LAUNCHER_SCROLL_MS,
                    easing = FastOutSlowInEasing
                )
                val fade = tween<Float>(durationMillis = LAUNCHER_SCROLL_MS)
                val enter: EnterTransition
                val exit: ExitTransition
                when (swipe) {
                    LauncherSwipe.Up -> {
                        enter = slideInVertically(spec) { it } + fadeIn(fade)
                        exit = slideOutVertically(spec) { -it } + fadeOut(fade)
                    }
                    LauncherSwipe.Down -> {
                        enter = slideInVertically(spec) { -it } + fadeIn(fade)
                        exit = slideOutVertically(spec) { it } + fadeOut(fade)
                    }
                    LauncherSwipe.Left -> {
                        enter = slideInHorizontally(spec) { it } + fadeIn(fade)
                        exit = slideOutHorizontally(spec) { -it } + fadeOut(fade)
                    }
                    LauncherSwipe.Right -> {
                        enter = slideInHorizontally(spec) { -it } + fadeIn(fade)
                        exit = slideOutHorizontally(spec) { it } + fadeOut(fade)
                    }
                }
                enter togetherWith exit
            },
            label = "launcherPage"
        ) { target ->
            Box(Modifier.fillMaxSize()) { content(target) }
        }
        LauncherPageDots(shownPage, recentsEnabled)
        if (shownPage == LauncherPage.Utilities) UtilitiesSettingsButton()
    }
}

private val LAUNCHER_SWIPE_THRESHOLD = 40.dp

private const val LAUNCHER_SCROLL_MS = 280

@Composable
private fun BoxScope.LauncherPageDots(page: LauncherPage, recentsEnabled: Boolean) {
    val palette = LocalIslandPalette.current
    @Composable
    fun Dot(active: Boolean) = Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(palette.text.copy(alpha = if (active) 0.7f else 0.25f))
    )
    Column(
        modifier = Modifier.align(Alignment.CenterEnd),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Dot(page != LauncherPage.Utilities)
        Dot(page == LauncherPage.Utilities)
    }
    if (recentsEnabled && page != LauncherPage.Utilities) {
        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Dot(page == LauncherPage.Apps)
            Dot(page == LauncherPage.Recents)
        }
    }
}

private fun Modifier.launcherSwipes(thresholdPx: Float, onSwipe: (LauncherSwipe) -> Unit): Modifier =
    pointerInput(thresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = Offset.Zero
            var fired = false
            var owned = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                if (change.isConsumed && !owned) break
                total += change.position - change.previousPosition
                if (!owned && total.getDistance() > viewConfiguration.touchSlop) owned = true
                if (owned) change.consume()
                if (!fired) {
                    val direction = launcherSwipeFor(total.x, total.y, thresholdPx)
                    if (direction != null) {
                        fired = true
                        onSwipe(direction)
                    }
                }
            }
        }
    }
