/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.ui.IslandPalette
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R

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
    modifier: Modifier = Modifier,
    onReplyStateChanged: (Boolean) -> Unit = {}
) {
    if (notifications.isEmpty()) {
        if (!settings.enableAppShortcuts) {
            return
        }
        val density = LocalDensity.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .onSizeChanged {
                    val measuredHeight = with(density) { it.height.toDp() }
                    if (measuredHeight > 0.dp) {
                        val clamped = measuredHeight.coerceIn(LARGE_IDLE_MIN_HEIGHT, LARGE_IDLE_MAX_HEIGHT)
                        onHeightMeasured(clamped)
                    }
                }
        ) {
            if (settings.enableNotificationBackdrop) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(settings.notificationDotColor).copy(alpha = 0.22f),
                                    Color(settings.notificationDotColor).copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, 0f),
                                radius = 700f
                            )
                        )
                )
            }
            IdleDashboard(
                apps = launcherApps,
                onLaunchApp = onLaunchApp,
                onCollapse = onCollapse,
                onInputStateChanged = onReplyStateChanged
            )
        }
        return
    }

    val density = LocalDensity.current
    var pageHeights by remember { mutableStateOf(emptyMap<String, Dp>()) }

    // Clean up stale keys not present in notifications
    val activeKeys = remember(notifications) { notifications.map { it.key }.toSet() }
    LaunchedEffect(activeKeys) {
        pageHeights = pageHeights.filterKeys { it in activeKeys }
    }

    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, notifications.lastIndex),
        pageCount = { notifications.size }
    )

    // Sync external selectedIndex updates ONLY when not currently user-scrolling,
    // preventing gesture interrupts mid-swipe that cause half-page frozen states.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in notifications.indices &&
            !pagerState.isScrollInProgress &&
            pagerState.currentPage != selectedIndex
        ) {
            pagerState.animateScrollToPage(selectedIndex)
        }
    }

    // Sync settled page updates back to caller ONLY when scroll has settled
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage in notifications.indices) {
            onPageSelected(pagerState.settledPage)
        }
    }

    // Safety net: Ensure pager never stays stuck at a non-zero offset fraction when scroll finishes
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction != 0f) {
            val targetPage = pagerState.settledPage.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
            if (targetPage in notifications.indices) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val bottomPadding = 16.dp

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {

        // Interpolate height between pages based on swipe progress
        // FIX: Clamp battery/call heights to match music/notification style to avoid
        // glitch where battery/call expand more (taller) than other modes.
        val currentPage = pagerState.currentPage
        val offsetFraction = pagerState.currentPageOffsetFraction
        val currentNotification = notifications.getOrNull(currentPage)

        fun clampHeightForMode(notif: IslandNotification?, height: Dp): Dp {
            return when (notif?.mode) {
                IslandMode.Battery, IslandMode.IncomingCall -> height.coerceIn(72.dp, 125.dp)
                IslandMode.Notification -> height.coerceIn(95.dp, 145.dp)
                IslandMode.Music -> height.coerceIn(150.dp, 260.dp)
                IslandMode.LiveActivity -> height.coerceIn(140.dp, 205.dp)
                IslandMode.Navigation -> height.coerceIn(135.dp, 195.dp)
                IslandMode.DownloadUpload -> height.coerceIn(120.dp, 195.dp)
                IslandMode.Hotspot -> height.coerceIn(120.dp, 195.dp)
                IslandMode.Bluetooth -> height.coerceIn(72.dp, 130.dp)
                IslandMode.Flashlight -> height.coerceIn(72.dp, 130.dp)
                IslandMode.ScreenRecording -> height.coerceIn(72.dp, 130.dp)
                IslandMode.Timer -> height.coerceIn(72.dp, 130.dp)
                IslandMode.Stopwatch -> height.coerceIn(72.dp, 130.dp)
                else -> height.coerceIn(80.dp, 160.dp)
            }
        }

        val currentPageHeightRaw = currentNotification?.let { pageHeights[it.key] }
        val currentPageHeight = currentPageHeightRaw?.let { clampHeightForMode(currentNotification, it) }
            ?: com.agupta07505.smartisland.ui.defaultEstimatedHeightForMode(currentNotification?.mode)

        val targetHeight = run {
            val nextPage = if (offsetFraction > 0f) {
                (currentPage + 1).coerceAtMost(notifications.lastIndex)
            } else if (offsetFraction < 0f) {
                (currentPage - 1).coerceAtLeast(0)
            } else {
                currentPage
            }
            val nextNotification = notifications.getOrNull(nextPage)
            val nextHeightRaw = nextNotification?.let { pageHeights[it.key] }
            val nextHeight = (nextHeightRaw?.let { clampHeightForMode(nextNotification, it) }
                ?: com.agupta07505.smartisland.ui.defaultEstimatedHeightForMode(nextNotification?.mode))
            val fraction = kotlin.math.abs(offsetFraction)
            (currentPageHeight + (nextHeight - currentPageHeight) * fraction).coerceIn(72.dp, 300.dp)
        }

        LaunchedEffect(targetHeight) {
            onHeightMeasured(targetHeight)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(targetHeight)
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
                                if (pageHeights[notification.key] != heightDp) {
                                    pageHeights = pageHeights.toMutableMap().apply { put(notification.key, heightDp) }
                                }
                            }
                            .then(
                                // The music card is interactive: only its header opens the app.
                                if (notification.mode == IslandMode.Music) Modifier
                                else Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onOpenNotification(notification)
                                }
                            )
                    ) {
                        if (settings.enableNotificationBackdrop && notification.mode != IslandMode.Music) {
                            NotificationBackdrop(
                                notification = notification,
                                settings = settings,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        AnimatedContent(
                            targetState = notification.mode,
                            transitionSpec = { islandContentTransition() },
                            label = "islandModeContent"
                        ) { mode ->
                        when (mode) {
                            IslandMode.Notification -> NotificationExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                showActions = settings.showNotificationActions,
                                settings = settings,
                                onReplyStateChanged = onReplyStateChanged
                            )
                            IslandMode.IncomingCall -> IncomingCallExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Music -> MusicExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenApp = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Battery -> BatteryExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                settings = settings
                            )
                            IslandMode.LiveActivity -> LiveActivityExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Navigation -> NavigationExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.DownloadUpload -> DownloadExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Hotspot -> HotspotExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Bluetooth -> BluetoothExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Flashlight -> FlashlightExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.ScreenRecording -> ScreenRecordingExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Timer -> TimerExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Stopwatch -> StopwatchExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = { onOpenNotification(notification) },
                                onCollapse = onCollapse,
                                settings = settings
                            )
                            IslandMode.Empty -> IdleDashboard(
                                apps = launcherApps,
                                onLaunchApp = onLaunchApp,
                                onCollapse = onCollapse,
                                onInputStateChanged = onReplyStateChanged
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationBackdrop(
    notification: IslandNotification,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    val artwork = notification.largeIcon ?: notification.icon
    val imageBitmap = remember(artwork) {
        if (artwork != null && !artwork.isRecycled) {
            runCatching { artwork.asImageBitmap() }.getOrNull()
        } else null
    }

    val accentColor = when (notification.mode) {
        IslandMode.Battery -> Color(settings.batteryColor)
        IslandMode.IncomingCall -> Color(settings.callColor)
        IslandMode.LiveActivity -> Color(settings.liveActivityColor)
        IslandMode.Navigation -> Color(settings.navigationColor)
        IslandMode.DownloadUpload -> Color(settings.transferColor)
        IslandMode.Hotspot -> Color(settings.hotspotColor)
        IslandMode.Bluetooth -> Color(settings.bluetoothColor)
        IslandMode.Flashlight -> Color(settings.flashlightColor)
        IslandMode.ScreenRecording -> Color(settings.screenRecordingColor)
        IslandMode.Timer -> Color(settings.timerColor)
        IslandMode.Stopwatch -> Color(settings.stopwatchColor)
        else -> Color(settings.notificationDotColor)
    }

    Box(modifier = modifier) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.38f }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 700f
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 750f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }
    }
}


private const val ISLAND_CONTENT_SPRING_DAMPING = 0.86f
private const val ISLAND_CONTENT_SPRING_STIFFNESS = 224f
internal val LARGE_IDLE_MIN_HEIGHT = 120.dp
internal val LARGE_IDLE_MAX_HEIGHT = 320.dp

/**
 * Cross-fade with a short vertical slide, sized by the same light spring the island shell uses, so
 * resizing never clips content mid-transition.
 */
private fun <S> AnimatedContentTransitionScope<S>.islandContentTransition(): ContentTransform =
    ((fadeIn(spring(ISLAND_CONTENT_SPRING_DAMPING, ISLAND_CONTENT_SPRING_STIFFNESS)) +
        slideInVertically(spring(ISLAND_CONTENT_SPRING_DAMPING, ISLAND_CONTENT_SPRING_STIFFNESS)) { it / 12 }) togetherWith
        (fadeOut(spring(1f, 305f)) + slideOutVertically(spring(1f, 305f)) { -it / 12 })) using
        SizeTransform(clip = false)
