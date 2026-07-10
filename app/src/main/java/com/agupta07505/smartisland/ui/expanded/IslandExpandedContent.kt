/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.LaunchableApp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun IslandExpandedContent(
    notifications: List<IslandNotification>,
    launcherApps: List<LaunchableApp>?,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onCollapse: () -> Unit,
    statusBarHeight: Dp,
    onHeightMeasured: (Dp) -> Unit,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) {
        val density = LocalDensity.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                // The island starts at collapsed height. Unbounded measurement is
                // required here so the launcher can discover its natural height.
                .wrapContentHeight(unbounded = true)
                .onSizeChanged {
                    val measuredHeight = with(density) { it.height.toDp() }
                    if (measuredHeight > 0.dp) onHeightMeasured(measuredHeight)
                }
        ) {
            EmptyExpanded(settings = settings, apps = launcherApps, onLaunchApp = onLaunchApp)
        }
        return
    }

    val density = LocalDensity.current
    var pageHeights by remember { mutableStateOf(emptyMap<Int, Dp>()) }

    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, notifications.lastIndex),
        pageCount = { notifications.size }
    )

    // Sync external selectedIndex updates
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in notifications.indices && pagerState.currentPage != selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    // Sync pager page updates back to caller
    LaunchedEffect(pagerState.currentPage) {
        onPageSelected(pagerState.currentPage)
    }

    val bottomPadding = 16.dp

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {

        // Interpolate height between pages based on swipe progress
        val currentPage = pagerState.currentPage
        val offsetFraction = pagerState.currentPageOffsetFraction
        val currentPageHeight = pageHeights[currentPage]
        val targetHeight = if (currentPageHeight != null) {
            val nextPage = if (offsetFraction > 0f) {
                (currentPage + 1).coerceAtMost(notifications.lastIndex)
            } else if (offsetFraction < 0f) {
                (currentPage - 1).coerceAtLeast(0)
            } else {
                currentPage
            }
            val nextHeight = pageHeights[nextPage] ?: currentPageHeight
            val fraction = kotlin.math.abs(offsetFraction)
            currentPageHeight + (nextHeight - currentPageHeight) * fraction
        } else {
            null
        }

        LaunchedEffect(targetHeight) {
            if (targetHeight != null) {
                onHeightMeasured(targetHeight)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (targetHeight != null) Modifier.height(targetHeight) else Modifier.wrapContentHeight()
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    // unbounded = true: pages measure at natural height even when parent Box has explicit height
                    .wrapContentHeight(unbounded = true)
            ) { page ->
                val notification = notifications.getOrNull(page)
                if (notification != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .onSizeChanged { size ->
                                val heightDp = with(density) { size.height.toDp() }
                                if (pageHeights[page] != heightDp) {
                                    pageHeights = pageHeights.toMutableMap().apply { put(page, heightDp) }
                                }
                            }
                    ) {
                        when (notification.mode) {
                            IslandMode.Notification -> NotificationExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse
                            )
                            IslandMode.IncomingCall -> IncomingCallExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onCollapse = onCollapse
                            )
                            IslandMode.Music -> MusicExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding
                            )
                            IslandMode.Battery -> BatteryExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                settings = settings
                            )
                            IslandMode.Empty -> EmptyExpanded(
                                settings = settings,
                                apps = launcherApps,
                                onLaunchApp = onLaunchApp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyExpanded(
    settings: SmartIslandSettings,
    apps: List<LaunchableApp>?,
    onLaunchApp: (String) -> Unit
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
        val hasConfiguration = settings.shortcutPackages.isNotEmpty() || settings.showRecentApps

        if (loadedApps == null) {
            // Keep the configured launcher visually clean while PackageManager and
            // UsageStats are queried. In particular, do not flash the setup state.
            Spacer(Modifier.height(84.dp))
        } else if (loadedApps.isEmpty() && !hasConfiguration) {
            Text("Quick launch", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Choose shortcuts in the Smart Island app",
                color = Color(0xFFB7C0CA),
                fontSize = 13.sp
            )
            Text(
                "Open Smart Island settings",
                color = Color(0xFF67E8F9),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable { onLaunchApp(context.packageName) }
            )
        } else if (loadedApps.isEmpty()) {
            Text(
                "Selected apps are unavailable. Update App shortcuts in Smart Island.",
                color = Color(0xFFB7C0CA),
                fontSize = 13.sp
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                loadedApps.chunked(4).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowApps.forEach { app ->
                            ShortcutApp(app = app, onClick = { onLaunchApp(app.packageName) })
                        }
                        repeat(4 - rowApps.size) { Box(Modifier.size(width = 64.dp, height = 1.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutApp(app: LaunchableApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier.size(44.dp)
            )
        }
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
