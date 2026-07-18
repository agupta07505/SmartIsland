/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.WavyMusicSeekBar
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun MusicExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp
) {
    val context = LocalContext.current
    val positionMs = notification?.mediaPositionMs
    val durationMs = notification?.mediaDurationMs
    
    val controller = remember(notification?.mediaToken) {
        notification?.mediaToken?.let { token ->
            runCatchingLogged("MusicExpanded", "Failed to create MediaController") {
                android.media.session.MediaController(context, token)
            }
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

    var lastSeekTimeMs by remember { mutableStateOf(0L) }

    var localIsPlaying by remember(notification?.key, notification?.mediaIsPlaying) {
        mutableStateOf(notification?.mediaIsPlaying == true)
    }

    var livePositionMs by remember(positionMs, controller) {
        mutableStateOf(getEstimatedPosition(controller, positionMs))
    }

    if (localIsPlaying) {
        LaunchedEffect(controller, positionMs) {
            while (true) {
                if (System.currentTimeMillis() - lastSeekTimeMs > 1500L) {
                    livePositionMs = getEstimatedPosition(controller, positionMs)
                }
                kotlinx.coroutines.delay(30) // 30ms for 33fps smooth progress sliding
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
                var mode = -1
                
                // 1. Try AndroidX repeat mode extras first (highly accurate for modern apps like Spotify/YT Music)
                val extras = ctrl.extras
                if (extras != null && extras.containsKey("androidx.media.MediaSessionCompat.Extras.KEY_REPEAT_MODE")) {
                    mode = extras.getInt("androidx.media.MediaSessionCompat.Extras.KEY_REPEAT_MODE", -1)
                }
                
                // 2. Try standard framework repeat mode if extras not found or invalid
                if (mode == -1) {
                    mode = runCatchingLogged("MusicExpanded", "Failed to getRepeatMode via reflection") {
                        val method = ctrl.javaClass.getMethod("getRepeatMode")
                        method.invoke(ctrl) as Int
                    } ?: -1
                }
                
                // 3. Try custom action states fallback
                if (mode == -1 || mode == 0) {
                    val customActions = ctrl.playbackState?.customActions.orEmpty()
                    val activeRepeatAction = customActions.firstOrNull { action ->
                        val actionName = action.action.lowercase()
                        actionName.contains("repeat") || actionName.contains("loop")
                    }
                    if (activeRepeatAction != null) {
                        val title = activeRepeatAction.name.toString().lowercase()
                        if (title.contains("one") || title.contains("single") || title.contains("track")) {
                            mode = 1 // loop one
                        } else if (title.contains("all") || title.contains("playlist") || title.contains("on") || title.contains("enable")) {
                            mode = 2 // loop all
                        } else {
                            mode = 0
                        }
                    }
                }
                if (mode == -1) 0 else mode
            } else {
                0
            }
        }
    }

    val getIsHeartedReflect = remember {
        { metadata: android.media.MediaMetadata? ->
            if (metadata != null) {
                runCatchingLogged("MusicExpanded", "Failed to get isHearted rating") {
                    val rating = metadata.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)
                    if (rating != null) {
                        val method = rating.javaClass.getMethod("isHearted")
                        method.invoke(rating) as Boolean
                    } else {
                        false
                    }
                } ?: false
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
    var localIsLiked by remember(notification?.key, isLiked) {
        mutableStateOf(isLiked)
    }
    var repeatMode by remember(notification?.key) { mutableStateOf(0) }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose {}
        val callback = object : android.media.session.MediaController.Callback() {
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                repeatMode = getRepeatModeReflect(controller)
                isLiked = resolveLikeState(controller)
            }
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                isLiked = resolveLikeState(controller)
            }
            override fun onExtrasChanged(extras: android.os.Bundle?) {
                repeatMode = getRepeatModeReflect(controller)
            }
            override fun onQueueChanged(queue: MutableList<android.media.session.MediaSession.QueueItem>?) {
                repeatMode = getRepeatModeReflect(controller)
            }
            // Public method invoked by platform dynamically at runtime via reflection
            @Suppress("unused")
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
        val newLike = !localIsLiked
        localIsLiked = newLike
        if (controller != null) {
            runCatchingLogged("MusicExpanded", "Failed to toggleLike / setRating") {
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
            }
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
            var standardInvoked = false
            runCatchingLogged("MusicExpanded", "Failed to setRepeatMode standard") {
                // 1. Try framework standard repeat mode setter via reflection
                val transportControls = controller.transportControls
                val method = transportControls.javaClass.getMethod("setRepeatMode", Int::class.javaPrimitiveType)
                method.invoke(transportControls, nextMode)
                standardInvoked = true
            }
            
            if (!standardInvoked) {
                runCatchingLogged("MusicExpanded", "Failed to setRepeatMode custom action fallback") {
                    // 2. Custom repeat toggle action fallback (only when standard method cannot be resolved)
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
                }
            }
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
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B9A))
                        .padding(8.dp)
                )
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
                isPlaying = localIsPlaying,
                onSeek = { newProgress ->
                    if (durationMs != null && durationMs > 0) {
                        val newPosition = (newProgress * durationMs).toLong()
                        livePositionMs = newPosition
                        lastSeekTimeMs = System.currentTimeMillis()
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        val token = notification.mediaToken
                        if (token != null) {
                            runCatchingLogged("MusicExpanded", "Failed to seekTo position") {
                                val ctrl = android.media.session.MediaController(context, token)
                                ctrl.transportControls.seekTo(newPosition)
                            }
                        } else {
                            SmartIslandRepositories.notificationRepository(context).sendCommand(
                                com.agupta07505.smartisland.data.SmartIslandCommand.SeekTo(notification.packageName, newPosition)
                            )
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
                    .bounceClick {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        toggleLike()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (localIsLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (localIsLiked) Color(0xFFFF4B72) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            
            // 2. Skip Previous Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to skipToPrevious") {
                                controller.transportControls.skipToPrevious()
                            }
                        } else {
                            notification.sendFirstAction(context, "previous", "prev", "rewind")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            
            // 3. Play/Pause Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .bounceClick {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        val targetState = !localIsPlaying
                        localIsPlaying = targetState
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to play/pause") {
                                if (targetState) {
                                    controller.transportControls.play()
                                } else {
                                    controller.transportControls.pause()
                                }
                            }
                        } else {
                            notification.sendFirstAction(context, "play", "pause", "resume")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (localIsPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
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
                    .bounceClick {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to skipToNext") {
                                controller.transportControls.skipToNext()
                            }
                        } else {
                            notification.sendFirstAction(context, "next", "skip", "forward")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            
            // 5. Song Loop Button (right of skip next)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        toggleLoop()
                    },
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
