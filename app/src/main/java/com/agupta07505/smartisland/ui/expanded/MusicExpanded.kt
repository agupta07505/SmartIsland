/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import com.agupta07505.smartisland.R
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.ui.IslandPalette
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.WavyMusicSeekBar
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.MediaCustomAction
import com.agupta07505.smartisland.util.MediaLike
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos

@Composable
fun MusicExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onOpenApp: () -> Unit,
    onCollapse: () -> Unit,
    settings: SmartIslandSettings = SmartIslandSettings.Default
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

    // What the player itself reports: true/false when it exposes a favorite toggle (its label says
    // "Add to…" or "Remove from…") or a heart rating; null when it exposes nothing readable.
    val readReportedLike = remember(getIsHeartedReflect) {
        { ctrl: android.media.session.MediaController? ->
            val actions = ctrl?.playbackState?.customActions.orEmpty()
                .map { MediaCustomAction(it.action.orEmpty(), it.name?.toString().orEmpty()) }
            val likeAction = MediaLike.findAction(actions)
            val hearted = getIsHeartedReflect(ctrl?.metadata)
            when {
                hearted -> true
                likeAction != null -> MediaLike.isActive(likeAction)
                else -> null
            }
        }
    }

    // `reported` is the player's truth; `optimistic` bridges the moment between the tap and the
    // player confirming it, and is dropped as soon as the player reports (or fails to).
    val trackId = notification?.key to notification?.title
    var reported by remember(trackId) { mutableStateOf<Boolean?>(null) }
    var optimistic by remember(trackId) { mutableStateOf<Boolean?>(null) }
    val localIsLiked = optimistic ?: reported ?: false
    val likeScope = androidx.compose.runtime.rememberCoroutineScope()

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose {}
        val sync = {
            val now = readReportedLike(controller)
            reported = now
            if (now != null) optimistic = null
        }
        val callback = object : android.media.session.MediaController.Callback() {
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                sync()
                localIsPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
            }
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                sync()
            }
        }
        sync()
        controller.registerCallback(callback)
        onDispose {
            controller.unregisterCallback(callback)
        }
    }

    val toggleLike = {
        val wasLiked = localIsLiked
        val target = !wasLiked
        if (controller != null) {
            val actions = controller.playbackState?.customActions.orEmpty()
            val action = actions.firstOrNull {
                MediaLike.findAction(listOf(MediaCustomAction(it.action.orEmpty(), it.name?.toString().orEmpty()))) != null
            }
            optimistic = target
            runCatchingLogged("MusicExpanded", "Failed to toggle like") {
                if (action != null) {
                    // One mechanism only: the player's own toggle. Adding a rating as well would flip it twice.
                    controller.transportControls.sendCustomAction(action, null)
                } else {
                    controller.transportControls.setRating(android.media.Rating.newHeartRating(target))
                }
            }
            if (action != null) {
                // If the player never confirms, show the truth instead of a heart that only looks liked.
                likeScope.launch {
                    kotlinx.coroutines.delay(1800)
                    if (optimistic == target && reported == wasLiked) optimistic = null
                }
            }
        }
    }

    val stopSong = {
        val repo = SmartIslandRepositories.notificationRepository(context)
        if (controller != null) {
            runCatchingLogged("MusicExpanded", "Failed to stop playback") {
                controller.transportControls.stop()
            }
        } else {
            notification.sendFirstAction(context, "stop", "close", "dismiss")
        }
        notification?.key?.let { repo.removeNotification(it) }
        onCollapse()
    }

    val metadataArtwork = remember(controller, controller?.metadata) {
        val meta = controller?.metadata
        if (meta != null) {
            runCatchingLogged("MusicExpanded", "Failed to extract metadata artwork") {
                meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                    ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            }
        } else null
    }
    val artwork = notification?.largeIcon ?: metadataArtwork ?: notification?.icon
    val accent = Color(settings.musicVisualizerColor)

    // The card owns its touches: only the header opens the app. Buttons, slider and the empty
    // space between them never launch it, and each interaction restarts the auto-collapse timer.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Matte backdrop: the artwork is blurred, desaturated and darkened so a small or square cover
        // never shows compression on a wide card and text stays readable on any art.
        val blurSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.8f) }),
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        // Over-scale so the blurred edges stay outside the card.
                        scaleX = 1.25f
                        scaleY = 1.25f
                    }
                    .then(if (blurSupported) Modifier.blur(28.dp) else Modifier)
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.45f), Color(0xFF111111))))
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (blurSupported) 0.26f else 0.55f),
                            Color.Black.copy(alpha = if (blurSupported) 0.52f else 0.7f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 14.dp)
        ) {
            // Header: the one region that opens the source app.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = onOpenApp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val appIcon = notification?.icon
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = notification?.appName ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(IslandPalette.fill)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.music_open_app),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = notification?.title?.takeIf { it.isNotBlank() } ?: "Song",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = notification?.text?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Artist",
                color = IslandPalette.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )

            val seekDescription = stringResource(R.string.a11y_playback_position)
            WavyMusicSeekBar(
                progress = progress,
                isPlaying = localIsPlaying,
                onSeek = { newProgress ->
                    SmartIslandRepositories.notificationRepository(context).resetTimer()
                    if (durationMs != null && durationMs > 0) {
                        val newPosition = (newProgress * durationMs).toLong()
                        livePositionMs = newPosition
                        lastSeekTimeMs = System.currentTimeMillis()
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to seekTo position") {
                                controller.transportControls.seekTo(newPosition)
                            }
                        } else {
                            notification?.packageName?.let { pkg ->
                                SmartIslandRepositories.notificationRepository(context).sendCommand(
                                    com.agupta07505.smartisland.data.SmartIslandCommand.SeekTo(pkg, newPosition)
                                )
                            }
                        }
                    }
                },
                trackColor = Color.White.copy(alpha = 0.28f),
                waveHeight = 5.dp,
                trackThickness = 3.dp,
                thumbRadius = 6.dp,
                touchHeight = 32.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = seekDescription }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(livePositionMs), color = IslandPalette.secondaryLabel, style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(durationMs), color = IslandPalette.secondaryLabel, style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicControl(
                    icon = if (localIsLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    description = stringResource(if (localIsLiked) R.string.a11y_unlike else R.string.a11y_like),
                    tint = if (localIsLiked) IslandPalette.danger else Color.White,
                    onClick = {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        toggleLike()
                    }
                )
                MusicControl(
                    icon = Icons.Rounded.SkipPrevious,
                    description = stringResource(R.string.a11y_previous),
                    iconSize = 28.dp,
                    onClick = {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to skipToPrevious") {
                                controller.transportControls.skipToPrevious()
                            }
                        } else {
                            notification.sendFirstAction(context, "previous", "prev", "rewind")
                        }
                    }
                )
                MusicControl(
                    icon = if (localIsPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    description = stringResource(if (localIsPlaying) R.string.a11y_pause else R.string.a11y_play),
                    size = 46.dp,
                    iconSize = 28.dp,
                    filled = true,
                    onClick = {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        val targetState = !localIsPlaying
                        localIsPlaying = targetState
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to play/pause") {
                                if (targetState) controller.transportControls.play()
                                else controller.transportControls.pause()
                            }
                        } else {
                            notification.sendFirstAction(context, "play", "pause", "resume")
                        }
                    }
                )
                MusicControl(
                    icon = Icons.Rounded.SkipNext,
                    description = stringResource(R.string.a11y_next),
                    iconSize = 28.dp,
                    onClick = {
                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                        if (controller != null) {
                            runCatchingLogged("MusicExpanded", "Failed to skipToNext") {
                                controller.transportControls.skipToNext()
                            }
                        } else {
                            notification.sendFirstAction(context, "next", "skip", "forward")
                        }
                    }
                )
                MusicControl(
                    icon = Icons.Rounded.Close,
                    description = stringResource(R.string.a11y_stop_playback),
                    onClick = stopSong
                )
            }
        }
    }
}

/** Round transport button: at least 48dp, optionally filled (primary action). */
@Composable
private fun MusicControl(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    filled: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(IslandPalette.fillStrong) else Modifier)
            .bounceClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
