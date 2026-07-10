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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings

@Composable
fun IslandExpandedContent(
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onCollapse: () -> Unit,
    statusBarHeight: Dp,
    onHeightMeasured: (Dp) -> Unit,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            EmptyExpanded()
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
                            IslandMode.Empty -> EmptyExpanded()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyExpanded() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Smart Island", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Ready for notifications", color = Color(0xFFB7C0CA), fontSize = 13.sp)
    }
}
