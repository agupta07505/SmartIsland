package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction

@Composable
fun IslandExpandedContent(
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onCollapse: () -> Unit,
    statusBarHeight: Dp,
    onHeightMeasured: (Dp) -> Unit,
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

    val bottomPadding = if (notifications.size > 1) 36.dp else 16.dp

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
                            IslandMode.Empty -> EmptyExpanded()
                        }
                    }
                }
            }

            // Three dots indicator at the bottom center when more than one notification exists
            if (notifications.size > 1) {
                Row(
                    Modifier
                        .height(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(notifications.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(6.dp)
                        )
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

@Composable
private fun NotificationExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenNotification: () -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = notification?.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity",
                    color = Color(0xFFD5DAE0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }

            // Time text on top right
            Text(
                text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
                color = Color(0xFFB7C0CA),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Bottom Section: Action buttons (left) and Down Arrow Button (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left part of Bottom Section: Action Buttons Row
            if (notification != null && notification.actionIntents.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    notification.actionIntents.forEach { action ->
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE2E8F0)) // light grey background matching the Telegram button
                                .bounceClick {
                                    if (action.pendingIntent != null) {
                                        triggerAction(context, notification.packageName, action.pendingIntent, action.title, notification.contentIntent)
                                    } else {
                                        Toast.makeText(context, "Clicked: ${action.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    SmartIslandOverlayService.removeNotification(notification.key)
                                    SmartIslandNotificationListenerService.cancelSystemNotification(notification.key)
                                    onCollapse()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.title,
                                color = Color(0xFF1F2937), // dark grey text
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Down Arrow Button on bottom right
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .bounceClick { onOpenNotification() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Open App",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun IncomingCallExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 12.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = notification?.title?.takeIf { it.isNotBlank() }
                ?: notification?.text?.takeIf { it.isNotBlank() }
                ?: "Incoming call",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        CircleActionButton(
            color = Color(0xFFE11D48),
            icon = Icons.Rounded.Close,
            onClick = { 
                notification.sendFirstAction(context, "decline", "reject", "hang", "end") 
                onCollapse()
            }
        )
        CircleActionButton(
            color = Color(0xFF79C943),
            icon = Icons.Rounded.Call,
            onClick = { 
                notification.sendFirstAction(context, "answer", "accept") 
                onCollapse()
            }
        )
    }
}

@Composable
private fun MusicExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp
) {
    val context = LocalContext.current
    val positionMs = notification?.mediaPositionMs
    val durationMs = notification?.mediaDurationMs
    
    var livePositionMs by remember(positionMs, notification?.mediaIsPlaying) {
        mutableStateOf(positionMs)
    }

    if (notification?.mediaIsPlaying == true && positionMs != null) {
        LaunchedEffect(notification) {
            val startTime = android.os.SystemClock.elapsedRealtime()
            val startPosition = positionMs
            while (true) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
                livePositionMs = startPosition + elapsed
                kotlinx.coroutines.delay(500)
            }
        }
    }

    val progress = when {
        durationMs != null && durationMs > 0 && livePositionMs != null ->
            (livePositionMs!!.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        notification?.progressMax?.let { it > 0 } == true ->
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val artwork = notification?.largeIcon ?: notification?.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B9A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    notification?.title?.takeIf { it.isNotBlank() } ?: "Song",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    notification?.text?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Artist",
                    color = Color(0xFFD5DAE0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatDuration(livePositionMs), color = Color.White, fontSize = 10.sp)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(3.dp), color = Color.White, trackColor = Color(0xFF667085))
            Text(formatDuration(durationMs), color = Color.White, fontSize = 10.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { notification.sendFirstAction(context, "previous", "prev", "rewind") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .bounceClick { notification.sendFirstAction(context, "play", "pause", "resume") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification?.mediaIsPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { notification.sendFirstAction(context, "next", "skip", "forward") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

private fun IslandNotification?.sendFirstAction(context: Context, vararg keywords: String) {
    val action = this?.actionIntents?.firstOrNull { action ->
        keywords.any { keyword -> action.title.contains(keyword, ignoreCase = true) }
    } ?: return
    if (action.pendingIntent != null) {
        triggerAction(context, this.packageName, action.pendingIntent, action.title, this.contentIntent)
        if (this.mode != IslandMode.Music) {
            SmartIslandOverlayService.removeNotification(this.key)
            SmartIslandNotificationListenerService.cancelSystemNotification(this.key)
        }
        SmartIslandOverlayService.resetTimer()
    }
}

private fun formatDuration(valueMs: Long?): String {
    val totalSeconds = valueMs?.takeIf { it >= 0 }?.div(1000) ?: return "--:--"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun triggerAction(context: Context, packageName: String, actionIntent: PendingIntent?, actionTitle: String, contentIntent: PendingIntent?) {
    if (actionIntent == null) return

    // If it is a Reply action, since typing inside the overlay window is blocked by focus rules,
    // trigger the main notification's content intent to open the target chat directly!
    if (actionTitle.contains("reply", ignoreCase = true) && contentIntent != null) {
        sendIntentWithOptions(context, contentIntent)
    } else {
        sendIntentWithOptions(context, actionIntent)
    }
}

private fun sendIntentWithOptions(context: Context, pendingIntent: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val options = ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            .toBundle()
        runCatching {
            pendingIntent.send(context, 0, null, null, null, null, options)
        }
    } else {
        runCatching {
            pendingIntent.send()
        }
    }
}
