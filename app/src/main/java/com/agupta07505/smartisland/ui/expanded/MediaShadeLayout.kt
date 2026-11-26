/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.session.MediaController
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.ArtworkTint
import com.agupta07505.smartisland.util.LabColor
import com.agupta07505.smartisland.util.MediaOutput
import com.agupta07505.smartisland.util.MediaOutputKind
import com.agupta07505.smartisland.util.MediaOutputReader
import com.agupta07505.smartisland.util.RemoteDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.agupta07505.smartisland.ui.IslandText as Text

/* Building blocks of the media card laid out like the notification shade's media player. */

internal const val MEDIA_SHADE_MAX_CUSTOM = 2

internal const val MEDIA_SHADE_SCRIM_TOP = 0.40f
internal const val MEDIA_SHADE_SCRIM_BOTTOM = 0.55f

/**
 * Corner radii of the media card's buttons (play button 16 -> 12 dp while playing, paused stays a full circle at 24 dp,
 * chip 13 -> 9 dp)
 */
internal val PLAY_CORNER_PLAYING = 12.dp
internal val PLAY_CORNER_PAUSED = 24.dp
internal val CHIP_CORNER = 9.dp

internal data class MediaShadeColors(val container: Color, val onContainer: Color)

/**
 * Android's shade tones: container is the artwork colour at lightness 90, glyph the same hue at 10, real chroma kept,
 * so a vivid cover gives vivid buttons
 */
internal fun mediaShadeColors(
    settings: SmartIslandSettings,
    tint: ArtworkTint?,
    hasArtwork: Boolean,
    /** Tones swap on the light island (dark container, pale glyph) to stand out on the washed artwork. */
    light: Boolean = false
): MediaShadeColors {
    val containerTone = if (light) SHADE_CONTAINER_TONE_LIGHT else SHADE_CONTAINER_TONE
    val glyphTone = if (light) SHADE_GLYPH_TONE_LIGHT else SHADE_GLYPH_TONE
    if (tint != null) {
        val chroma = tint.chroma.toDouble().coerceIn(SHADE_MIN_CHROMA, SHADE_MAX_CHROMA)
        return MediaShadeColors(
            container = toneColor(containerTone, chroma, tint.labHue),
            onContainer = toneColor(glyphTone, chroma.coerceAtMost(SHADE_GLYPH_MAX_CHROMA), tint.labHue)
        )
    }
    if (hasArtwork) {
        return MediaShadeColors(
            container = toneColor(containerTone, 0.0, 0f),
            onContainer = toneColor(glyphTone, 0.0, 0f)
        )
    }
    val base = Color(settings.musicVisualizerColor)
    return MediaShadeColors(
        container = if (light) lerp(base, Color.Black, 0.55f) else lerp(base, Color.White, 0.80f),
        onContainer = if (light) lerp(base, Color.White, 0.90f) else lerp(base, Color.Black, 0.65f)
    )
}

/** Shade's tones: container lightness 90, glyph lightness 10 (CIELAB L*). */
internal const val SHADE_CONTAINER_TONE = 90.0
internal const val SHADE_GLYPH_TONE = 10.0

/** Light island: container at lightness 30, glyph 95, both clear 4.5:1. */
internal const val SHADE_CONTAINER_TONE_LIGHT = 30.0
internal const val SHADE_GLYPH_TONE_LIGHT = 95.0

/** CIELAB C* bounds of the artwork colour, the shade gave 4.5 for a dark cover and 69 for a vivid one. */
internal const val SHADE_MIN_CHROMA = 4.0
internal const val SHADE_MAX_CHROMA = 70.0

internal const val SHADE_GLYPH_MAX_CHROMA = 22.0

internal fun toneColor(tone: Double, chroma: Double, hueDeg: Float): Color {
    val rgb = LabColor.lchToSrgb(tone, chroma, hueDeg.toDouble())
    return Color(rgb[0], rgb[1], rgb[2])
}

/** Eased over 350 ms so a track change fades instead of snapping. */
@Composable
internal fun animatedShadeColors(target: MediaShadeColors): MediaShadeColors {
    val container by animateColorAsState(target.container, tween(350), label = "mediaShadeContainer")
    val onContainer by animateColorAsState(target.onContainer, tween(350), label = "mediaShadeOnContainer")
    return MediaShadeColors(container, onContainer)
}

/** App icon, top left, with no disc behind it. */
@Composable
internal fun MediaAppBadge(
    smallIcon: ImageBitmap?,
    launcherIcon: ImageBitmap?,
    colors: MediaShadeColors,
    modifier: Modifier = Modifier
) {
    val shade = animatedShadeColors(colors)
    Box(
        modifier = modifier.size(26.dp),
        contentAlignment = Alignment.Center
    ) {
        if (smallIcon != null) {
            Image(
                bitmap = smallIcon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(shade.container),
                modifier = Modifier.size(22.dp)
            )
        } else if (launcherIcon != null) {
            Image(
                bitmap = launcherIcon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = shade.container,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

internal fun mediaOutputIcon(output: MediaOutput): ImageVector =
    if (output.kind == MediaOutputKind.Cast) {
        when (output.device) {
            RemoteDevice.Tv -> Icons.Rounded.Tv
            RemoteDevice.Speaker -> Icons.Rounded.Speaker
            RemoteDevice.Computer -> Icons.Rounded.Computer
            RemoteDevice.Phone -> Icons.Rounded.PhoneAndroid
            RemoteDevice.Tablet -> Icons.Rounded.TabletAndroid
            RemoteDevice.Car -> Icons.Rounded.DirectionsCar
            RemoteDevice.Generic -> Icons.Rounded.Cast
        }
    } else mediaOutputIcon(output.kind)

internal fun mediaOutputIcon(kind: MediaOutputKind): ImageVector =
    when (kind) {
        MediaOutputKind.Phone -> Icons.Rounded.PhoneAndroid
        MediaOutputKind.Bluetooth -> Icons.Rounded.Bluetooth
        MediaOutputKind.Headphones -> Icons.Rounded.Headphones
        MediaOutputKind.Usb -> Icons.Rounded.Usb
        MediaOutputKind.Hdmi -> Icons.Rounded.Tv
        MediaOutputKind.Cast -> Icons.Rounded.Cast
    }

/** Read when the card opens, then kept current (device connecting or leaving, cast move). */
@Composable
internal fun rememberMediaOutput(controller: MediaController?): MediaOutput {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // Binder call, run in the background so the opening tap does not wait for it.
    var output by remember(controller) {
        mutableStateOf(lastMediaOutput ?: MediaOutput.Phone)
    }
    DisposableEffect(controller) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val refresh = {
            scope.launch {
                val read = withContext(Dispatchers.Default) {
                    MediaOutputReader.read(context, controller)
                }
                lastMediaOutput = read
                output = read
            }
            Unit
        }
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
        }
        val sessionCallback = object : MediaController.Callback() {
            override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) = refresh()
        }
        audio?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        controller?.registerCallback(sessionCallback)
        refresh()
        onDispose {
            audio?.unregisterAudioDeviceCallback(deviceCallback)
            controller?.unregisterCallback(sessionCallback)
        }
    }
    return output
}

private var lastMediaOutput: MediaOutput? = null

/** Top right, [onClick] null draws it without a tap action. */
@Composable
internal fun MediaOutputChip(
    label: String,
    colors: MediaShadeColors,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.PhoneAndroid
) {
    val shade = animatedShadeColors(colors)
    val tappable = if (onClick != null) modifier.bounceClick { onClick() } else modifier
    Row(
        modifier = tappable
            .height(22.dp)
            .clip(RoundedCornerShape(CHIP_CORNER))
            .background(shade.container)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = shade.onContainer,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label.uppercase(),
            color = shade.onContainer,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ExplicitBadge(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .border(1.2.dp, color, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "E", color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
    }
}

/** Rounded square while playing, circle while paused. */
@Composable
internal fun MediaPlayPauseButton(
    isPlaying: Boolean,
    colors: MediaShadeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shade = animatedShadeColors(colors)
    val corner by animateDpAsState(targetValue = if (isPlaying) PLAY_CORNER_PLAYING else PLAY_CORNER_PAUSED,
        label = "mediaPlayCorner")
    Box(
        modifier = modifier
            .size(48.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(corner))
            .background(shade.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = shade.onContainer,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
internal fun MediaShadeIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = LocalIslandPalette.current.text,
            modifier = Modifier.size(24.dp))
    }
}
