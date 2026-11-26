/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchCollapseFade
import com.agupta07505.smartisland.ui.LocalCardShown
import com.agupta07505.smartisland.ui.LocalExpandedAlpha
import com.agupta07505.smartisland.ui.WavyMusicSeekBar
import com.agupta07505.smartisland.util.MediaArt
import com.agupta07505.smartisland.util.MediaOutput
import com.agupta07505.smartisland.util.MediaOutputRules
import com.agupta07505.smartisland.util.MediaOutputSwitcher
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.agupta07505.smartisland.ui.IslandText as Text

private const val TAG = "MusicExpanded"

private val MUSIC_COVER_SIZE = 48.dp

private val MUSIC_COVER_SHAPE = SquircleShape()

private const val MUSIC_COVER_FADE_MS = 180

@Composable
fun MusicExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    settings: SmartIslandSettings = SmartIslandSettings.Default,
    // The landscape split dips the card's FOREGROUND through its re-wrap
    foregroundAlpha: (() -> Float)? = null
) {
    val context = LocalContext.current
    val positionMs = notification?.mediaPositionMs
    val durationMs = notification?.mediaDurationMs

    val controller = remember(notification?.mediaToken) {
        notification?.mediaToken?.let { token ->
            runCatchingLogged(TAG, "Failed to create MediaController") {
                MediaController(context, token)
            }
        }
    }

    var playbackState by remember(controller) { mutableStateOf(controller?.playbackState) }
    val playbackStateNow = rememberUpdatedState(playbackState)
    val getEstimatedPosition = remember {
        { fallbackPos: Long? ->
            val state = playbackStateNow.value
            if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                (state.position + (elapsed * state.playbackSpeed).toLong()).coerceAtLeast(0L)
            } else {
                state?.position ?: fallbackPos ?: 0L
            }
        }
    }

    val lastSeekTimeMs = remember { longArrayOf(0L) }

    var localIsPlaying by remember(notification?.key, notification?.mediaIsPlaying) {
        mutableStateOf(notification?.mediaIsPlaying == true)
    }

    val livePosition = remember(positionMs, controller) { mutableLongStateOf(getEstimatedPosition(positionMs)) }

    // a pre-composed card that waits collapsed (LocalCardShown = false) ticks slowly
    val cardShown by rememberUpdatedState(LocalCardShown.current)
    if (localIsPlaying) {
        LaunchedEffect(controller, positionMs) {
            while (true) {
                if (System.currentTimeMillis() - lastSeekTimeMs[0] > 1500L) {
                    livePosition.longValue = getEstimatedPosition(positionMs)
                }
                delay(if (cardShown) 33 else WAITING_CARD_TICK_MS)
            }
        }
    }

    val progressNow: () -> Float = {
        when {
            durationMs != null && durationMs > 0 ->
                (livePosition.longValue.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            notification?.progressMax?.let { it > 0 } == true ->
                (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose {}
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playbackState = state
            }
        }
        playbackState = controller.playbackState
        controller.registerCallback(callback)
        onDispose { controller.unregisterCallback(callback) }
    }
    val mediaOutput = rememberMediaOutput(controller)
    val outputScope = rememberCoroutineScope()
    val controls = remember(playbackState, notification?.actionIntents) {
        MediaControls.resolve(playbackState, notification)
    }

    val mediaSettings = remember(settings) { settings.forMedia() }
    val blurRadiusDp = if (mediaSettings.expandedBlurEnabled) mediaSettings.expandedBlurRadius else null
    val artKey = MediaArt.keyFor(notification, blurRadiusDp)
    // on a track change the title shows up first, the art comes a bit later
    val art by rememberMusicArt(notification, blurRadiusDp, artKey)
    val artworkImage = art.image
    val artwork = if (art.hasArtwork) art else null
    // backdrop is a small blurred bitmap, the cover needs the sharp one
    val coverArt by if (blurRadiusDp == null) {
        rememberUpdatedState(art)
    } else {
        rememberMusicArt(notification, null, MediaArt.keyFor(notification, null))
    }
    // The app's own icon is only a stand-in for a cover, it shows the placeholder instead
    val coverImage = if (coverArt.hasArtwork && !coverArt.isFallbackIcon) coverArt.image else null
    // Artwork behind the card keeps its own look
    val showsArtworkBackdrop = settings.enableMusicArtworkBackground && artworkImage != null
    val palette = if (showsArtworkBackdrop) IslandPalette.Dark else LocalIslandPalette.current

    val notchFadeAlpha = LocalExpandedAlpha.current
    CompositionLocalProvider(LocalIslandPalette provides palette) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            if (settings.enableMusicArtworkBackground && artworkImage != null) {
                MusicArtworkBackdrop(artworkImage, notchFadeAlpha)
            }

            val resetTimer = { SmartIslandRepositories.notificationRepository(context).resetTimer() }
            val artworkTint = art.tint
            val shadeColors = remember(artworkTint, artwork != null, settings.musicVisualizerColor, palette.isLight) {
                mediaShadeColors(settings, artworkTint, artwork != null, palette.isLight)
            }
            val isExplicit = art.explicit
            val launcherIcon = remember(notification?.icon) { notification?.icon?.asImageBitmap() }
            val smallIcon = remember(notification?.smallIcon) { notification?.smallIcon?.asImageBitmap() }
            val customs = (controls.leftCustom + controls.rightCustom).take(MEDIA_SHADE_MAX_CUSTOM)
            // No labels for streams without a length: the bar cannot seek there either
            val hasDuration = durationMs != null && durationMs > 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .then(
                        if (foregroundAlpha != null || notchFadeAlpha != null) {
                            Modifier.graphicsLayer {
                                val own = foregroundAlpha?.invoke()?.coerceIn(0f, 1f) ?: 1f
                                val notch = notchFadeAlpha?.let { NotchCollapseFade.foregroundAlpha(it()) } ?: 1f
                                alpha = own * notch
                            }
                        } else Modifier
                    )
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding)
            ) {
                MusicHeaderRow(smallIcon, launcherIcon, shadeColors, mediaOutput, notification, outputScope)
                Spacer(Modifier.height(16.dp))
                MusicTitleRow(
                    cover = coverImage,
                    notification = notification,
                    controller = controller,
                    controls = controls,
                    palette = palette,
                    colors = shadeColors,
                    isExplicit = isExplicit,
                    isPlaying = localIsPlaying,
                    onPlayingChange = { localIsPlaying = it },
                    resetTimer = resetTimer
                )
                Spacer(Modifier.height(4.dp))
                MusicControlsRow(
                    notification = notification,
                    controller = controller,
                    controls = controls,
                    palette = palette,
                    progressNow = progressNow,
                    isPlaying = localIsPlaying,
                    durationMs = durationMs,
                    livePosition = livePosition,
                    lastSeekTimeMs = lastSeekTimeMs,
                    hasDuration = hasDuration,
                    customs = customs,
                    resetTimer = resetTimer
                )
            }
        }
    }
}

@Composable
private fun rememberMusicArt(notification: IslandNotification?, blurRadiusDp: Float?,
        artKey: String): State<MediaArt.Result> {
    val context = LocalContext.current
    return produceState(
        initialValue = MediaArt.cached(artKey) ?: MediaArt.quick(notification, blurRadiusDp),
        artKey,
        notification?.largeIcon
    ) {
        val n = notification ?: return@produceState
        if (n.mediaToken == null) return@produceState
        MediaArt.cached(artKey)?.let { value = it; return@produceState }
        // The app's icon is only a stand-in
        val hadCover = MediaArt.lastGood(n.key, blurRadiusDp) != null
        var loaded = withContext(Dispatchers.Default) {
            MediaArt.load(context.applicationContext, n, blurRadiusDp)
        }
        if (!loaded.isFallbackIcon || !hadCover) value = loaded
        for (waitMs in MEDIA_ART_RETRY_MS) {
            if (loaded.fromSession) break
            delay(waitMs)
            loaded = withContext(Dispatchers.Default) {
                MediaArt.load(context.applicationContext, n, blurRadiusDp)
            }
            if (loaded.fromSession || !loaded.isFallbackIcon) value = loaded
        }
        // The retries are over and there is still no picture
        if (loaded.isFallbackIcon) value = loaded
    }
}

@Composable
private fun BoxScope.MusicArtworkBackdrop(artworkImage: ImageBitmap, notchFadeAlpha: (() -> Float)?) {
    // shell overshoots on its spring, so the backdrop is a bit taller than the card
    Box(modifier = Modifier.matchParentSize().artworkBleed(MEDIA_BACKDROP_BLEED)) {
        Image(
            bitmap = artworkImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = Modifier.matchParentSize()
                // .blur(18.dp) // softer artwork behind the card
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = MEDIA_SHADE_SCRIM_TOP),
                            Color.Black.copy(alpha = MEDIA_SHADE_SCRIM_BOTTOM)
                        )
                    )
                )
        )
    }
    Image(
        bitmap = artworkImage,
        contentDescription = "Album artwork background",
        contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
        modifier = Modifier.matchParentSize()
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = MEDIA_SHADE_SCRIM_TOP),
                        Color.Black.copy(alpha = MEDIA_SHADE_SCRIM_BOTTOM)
                    )
                )
            )
    )
    if (notchFadeAlpha != null) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = NotchCollapseFade.darkness(notchFadeAlpha()) }
                .background(Color.Black)
        )
    }
}

@Composable
private fun MusicHeaderRow(
    smallIcon: ImageBitmap?,
    launcherIcon: ImageBitmap?,
    colors: MediaShadeColors,
    mediaOutput: MediaOutput,
    notification: IslandNotification?,
    outputScope: CoroutineScope
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaAppBadge(smallIcon, launcherIcon, colors)
        MediaOutputChip(
            label = MediaOutputRules.label(
                mediaOutput,
                phoneLabel = stringResource(R.string.media_output_this_phone),
                castLabel = stringResource(R.string.media_output_cast),
                wiredLabel = stringResource(R.string.media_output_wired),
                headphonesLabel = stringResource(R.string.media_output_wired_headphones)
            ),
            colors = colors,
            onClick = {
                val pkg = notification?.packageName
                if (pkg != null) {
                    outputScope.launch {
                        MediaOutputSwitcher.open(context.applicationContext, pkg)
                    }
                }
            },
            icon = mediaOutputIcon(mediaOutput)
        )
    }
}

@Composable
private fun MusicTitleRow(
    cover: ImageBitmap?,
    notification: IslandNotification?,
    controller: MediaController?,
    controls: ResolvedMediaControls,
    palette: IslandPalette,
    colors: MediaShadeColors,
    isExplicit: Boolean,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    resetTimer: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicCover(cover, palette)
        Column(Modifier.weight(1f)) {
            Text(
                notification?.title?.takeIf { it.isNotBlank() } ?: "Song",
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExplicit) ExplicitBadge(palette.textSecondary)
                Text(
                    notification?.text?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Artist",
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }
        }
        if (controls.hasPlayPause) {
            MediaPlayPauseButton(
                isPlaying = isPlaying,
                colors = colors,
                onClick = {
                    resetTimer()
                    val targetState = !isPlaying
                    onPlayingChange(targetState)
                    if (controller != null) {
                        runCatchingLogged(TAG, "Failed to play/pause") {
                            if (targetState) controller.transportControls.play()
                            else controller.transportControls.pause()
                        }
                    } else {
                        notification.sendFirstAction(context, "play", "pause", "resume")
                    }
                }
            )
        }
    }
}

// keeps its space even with no cover, otherwise the title jumps when the art loads late
@Composable
private fun MusicCover(cover: ImageBitmap?, palette: IslandPalette) {
    Crossfade(
        targetState = cover,
        animationSpec = tween(MUSIC_COVER_FADE_MS),
        modifier = Modifier.size(MUSIC_COVER_SIZE).clip(MUSIC_COVER_SHAPE),
        label = "musicCover"
    ) { image ->
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.size(MUSIC_COVER_SIZE)
            )
        } else {
            Box(
                modifier = Modifier.size(MUSIC_COVER_SIZE).background(palette.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(MUSIC_COVER_SIZE / 2)
                )
            }
        }
    }
}

@Composable
private fun MusicControlsRow(
    notification: IslandNotification?,
    controller: MediaController?,
    controls: ResolvedMediaControls,
    palette: IslandPalette,
    progressNow: () -> Float,
    isPlaying: Boolean,
    durationMs: Long?,
    livePosition: MutableLongState,
    lastSeekTimeMs: LongArray,
    hasDuration: Boolean,
    customs: List<MediaCustomControl>,
    resetTimer: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (controls.hasPrevious) {
            MediaShadeIconButton(Icons.Rounded.SkipPrevious, "Previous", onClick = {
                resetTimer()
                if (controller != null) {
                    runCatchingLogged(TAG, "Failed to skipToPrevious") {
                        controller.transportControls.skipToPrevious()
                    }
                } else {
                    notification.sendFirstAction(context, "previous", "prev", "rewind")
                }
            })
        }
        MusicSeekColumn(
            palette = palette,
            progressNow = progressNow,
            isPlaying = isPlaying,
            durationMs = durationMs,
            livePosition = livePosition,
            lastSeekTimeMs = lastSeekTimeMs,
            hasDuration = hasDuration,
            controller = controller,
            notification = notification,
            resetTimer = resetTimer
        )
        if (controls.hasNext) {
            MediaShadeIconButton(Icons.Rounded.SkipNext, "Next", onClick = {
                resetTimer()
                if (controller != null) {
                    runCatchingLogged(TAG, "Failed to skipToNext") {
                        controller.transportControls.skipToNext()
                    }
                } else {
                    notification.sendFirstAction(context, "next", "skip", "forward")
                }
            })
        }
        customs.forEach { custom ->
            CustomControlButton(custom, controller, notification, 40.dp, resetTimer)
        }
    }
}

@Composable
private fun RowScope.MusicSeekColumn(
    palette: IslandPalette,
    progressNow: () -> Float,
    isPlaying: Boolean,
    durationMs: Long?,
    livePosition: MutableLongState,
    lastSeekTimeMs: LongArray,
    hasDuration: Boolean,
    controller: MediaController?,
    notification: IslandNotification?,
    resetTimer: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.weight(1f)) {
        if (hasDuration) Spacer(Modifier.height(12.dp))
        WavyMusicSeekBar(
            progress = progressNow,
            isPlaying = isPlaying,
            onSeek = { newProgress ->
                if (durationMs != null && durationMs > 0) {
                    val newPosition = (newProgress * durationMs).toLong()
                    livePosition.longValue = newPosition
                    lastSeekTimeMs[0] = System.currentTimeMillis()
                    resetTimer()
                    if (controller != null) {
                        runCatchingLogged(TAG, "Failed to seekTo position") {
                            controller.transportControls.seekTo(newPosition)
                        }
                    } else {
                        notification?.packageName?.let { pkg ->
                            SmartIslandRepositories.notificationRepository(context).sendCommand(
                                SmartIslandCommand.SeekTo(pkg, newPosition)
                            )
                        }
                    }
                }
            },
            trackColor = palette.text.copy(alpha = 0.3f),
            waveColor = palette.text,
            thumbColor = palette.text,
            pillThumb = true,
        )
        if (hasDuration) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicPositionText(livePosition)
                Text(
                    formatDuration(durationMs),
                    color = palette.text.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MusicPositionText(position: LongState) {
    val seconds by remember(position) { derivedStateOf { position.longValue / 1000L } }
    Text(
        formatDuration(seconds * 1000L),
        color = LocalIslandPalette.current.text.copy(alpha = 0.75f),
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
}

private val MEDIA_ART_RETRY_MS = longArrayOf(400L, 1_200L, 3_000L)

private const val WAITING_CARD_TICK_MS = 250L

private val MEDIA_BACKDROP_BLEED = 16.dp

private fun Modifier.artworkBleed(bleed: Dp): Modifier = this.layout { measurable, constraints ->
    // An intrinsic measurement passes an unbounded max, the bleed would overflow to a negative size
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val plain = measurable.measure(constraints)
        return@layout layout(plain.width, plain.height) { plain.place(0, 0) }
    }
    val b = bleed.roundToPx()
    val w = constraints.maxWidth
    val h = constraints.maxHeight
    // The card follows the shell's width overshoot itself, only the bottom needs to reach past it
    val placeable = measurable.measure(Constraints.fixed(w, h + b))
    layout(w, h) { placeable.place(0, 0) }
}
