/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity",
                    color = Color(0xFFD5DAE0),
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            // Time text on top right using the internal helper in IslandCollapsedContent
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
        if (notification?.actionIntents?.any { action ->
                action.title.contains("answer", ignoreCase = true) ||
                action.title.contains("accept", ignoreCase = true)
            } == true) {
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
}

@Composable
private fun MusicExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp
) {
    val context = LocalContext.current
    val positionMs = notification?.mediaPositionMs
    val durationMs = notification?.mediaDurationMs
    
    val controller = remember(notification?.mediaToken) {
        notification?.mediaToken?.let { token ->
            runCatching { android.media.session.MediaController(context, token) }.getOrNull()
        }
    }

    val getEstimatedPosition = remember {
        { ctrl: android.media.session.MediaController?, fallbackPos: Long? ->
            val state = ctrl?.playbackState
            if (state != null && state.state == android.media.session.PlaybackState.STATE_PLAYING) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                (state.position + (elapsed * state.playbackSpeed).toLong()).coerceAtLeast(0L)
            } else {
                state?.position ?: fallbackPos ?: 0L
            }
        }
    }

    var livePositionMs by remember(positionMs, controller) {
        mutableStateOf(getEstimatedPosition(controller, positionMs))
    }

    if (notification?.mediaIsPlaying == true) {
        LaunchedEffect(controller, positionMs) {
            while (true) {
                livePositionMs = getEstimatedPosition(controller, positionMs)
                kotlinx.coroutines.delay(500)
            }
        }
    }

    val progress = when {
        durationMs != null && durationMs > 0 ->
            (livePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        notification?.progressMax?.let { it > 0 } == true ->
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    val getRepeatModeReflect = remember {
        { ctrl: android.media.session.MediaController? ->
            if (ctrl != null) {
                runCatching {
                    val method = ctrl.javaClass.getMethod("getRepeatMode")
                    method.invoke(ctrl) as Int
                }.getOrDefault(0)
            } else {
                0
            }
        }
    }

    val getIsHeartedReflect = remember {
        { metadata: android.media.MediaMetadata? ->
            if (metadata != null) {
                runCatching {
                    val rating = metadata.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)
                    if (rating != null) {
                        val method = rating.javaClass.getMethod("isHearted")
                        method.invoke(rating) as Boolean
                    } else {
                        false
                    }
                }.getOrDefault(false)
            } else {
                false
            }
        }
    }

    val resolveLikeState = remember(getIsHeartedReflect) {
        { ctrl: android.media.session.MediaController? ->
            if (ctrl != null) {
                val ratingIsHearted = getIsHeartedReflect(ctrl.metadata)
                if (ratingIsHearted) {
                    true
                } else {
                    val customActions = ctrl.playbackState?.customActions.orEmpty()
                    customActions.any { action ->
                        val actionName = action.action.lowercase()
                        val title = action.name.toString().lowercase()
                        actionName.contains("unlike") || actionName.contains("remove") ||
                        title.contains("unlike") || title.contains("remove")
                    }
                }
            } else {
                false
            }
        }
    }

    var isLiked by remember(notification?.key) { mutableStateOf(false) }
    var repeatMode by remember(notification?.key) { mutableStateOf(0) }

    androidx.compose.runtime.DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose {}
        val callback = object : android.media.session.MediaController.Callback() {
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                repeatMode = getRepeatModeReflect(controller)
                isLiked = resolveLikeState(controller)
            }
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                isLiked = resolveLikeState(controller)
            }
            // Public method invoked by platform dynamically at runtime via reflection
            fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
        }
        repeatMode = getRepeatModeReflect(controller)
        isLiked = resolveLikeState(controller)
        controller.registerCallback(callback)
        onDispose {
            controller.unregisterCallback(callback)
        }
    }

    val toggleLike = {
        val newLike = !isLiked
        isLiked = newLike
        if (controller != null) {
            try {
                controller.transportControls.setRating(
                    android.media.Rating.newHeartRating(newLike)
                )
                val customActions = controller.playbackState?.customActions.orEmpty()
                val likeAction = customActions.firstOrNull { action ->
                    val actionName = action.action.lowercase()
                    val title = action.name.toString().lowercase()
                    actionName.contains("like") || actionName.contains("favorite") ||
                    title.contains("like") || title.contains("favorite")
                }
                if (likeAction != null) {
                    controller.transportControls.sendCustomAction(likeAction.action, null)
                }
            } catch (_: Exception) {}
        }
    }

    val toggleLoop = {
        val nextMode = when (repeatMode) {
            0 -> 1 // REPEAT_MODE_NONE -> REPEAT_MODE_ONE (Repeat One)
            1 -> 2 // REPEAT_MODE_ONE -> REPEAT_MODE_ALL (Repeat All)
            2 -> 0 // REPEAT_MODE_ALL -> REPEAT_MODE_NONE (None)
            else -> 1
        }
        repeatMode = nextMode
        if (controller != null) {
            try {
                // 1. Set Repeat Mode via reflection
                val transportControls = controller.transportControls
                val method = transportControls.javaClass.getMethod("setRepeatMode", Int::class.javaPrimitiveType)
                method.invoke(transportControls, nextMode)
                
                // 2. Custom repeat toggle action fallback
                val customActions = controller.playbackState?.customActions.orEmpty()
                val repeatAction = customActions.firstOrNull { action ->
                    val actionName = action.action.lowercase()
                    val title = action.name.toString().lowercase()
                    actionName.contains("repeat") || actionName.contains("loop") ||
                    title.contains("repeat") || title.contains("loop")
                }
                if (repeatAction != null) {
                    controller.transportControls.sendCustomAction(repeatAction.action, null)
                }
            } catch (_: Exception) {}
        }
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
            WavyMusicSeekBar(
                progress = progress,
                isPlaying = notification?.mediaIsPlaying == true,
                onSeek = { newProgress ->
                    if (durationMs != null && durationMs > 0) {
                        val newPosition = (newProgress * durationMs).toLong()
                        livePositionMs = newPosition
                        val token = notification.mediaToken
                        if (token != null) {
                            runCatching {
                                val controller = android.media.session.MediaController(context, token)
                                controller.transportControls.seekTo(newPosition)
                            }
                        } else {
                            SmartIslandNotificationListenerService.seekTo(notification.packageName, newPosition)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Text(formatDuration(durationMs), color = Color.White, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Song Like Button (left of skip previous)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { toggleLike() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4B72) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            
            // 2. Skip Previous Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { notification.sendFirstAction(context, "previous", "prev", "rewind") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            
            // 3. Play/Pause Button
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
            
            // 4. Skip Next Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { notification.sendFirstAction(context, "next", "skip", "forward") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            
            // 5. Song Loop Button (right of skip next)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { toggleLoop() },
                contentAlignment = Alignment.Center
            ) {
                val tintColor = when (repeatMode) {
                    1, 2 -> Color(0xFF1DB954) // spotify green loop active
                    else -> Color.White
                }
                Icon(
                    imageVector = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Loop",
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
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
