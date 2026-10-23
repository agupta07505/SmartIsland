/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.ArtworkTint
import com.agupta07505.smartisland.util.runCatchingLogged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/* Building blocks of the media card laid out like the notification shade's media player. */

/** The shade shows at most this many of the app's own buttons (like, shuffle, cast ...) after "next". */
internal const val MEDIA_SHADE_MAX_CUSTOM = 2

private const val EXPLICIT_KEY = "android.media.IS_EXPLICIT"

/** The dim over the artwork: a little lighter on top, a little darker where the bar and buttons are. */
internal const val MEDIA_SHADE_SCRIM_TOP = 0.40f
internal const val MEDIA_SHADE_SCRIM_BOTTOM = 0.55f

/**
 * Corner radii of the media card's buttons (play button 16 -> 12 dp while playing; paused stays a full circle at 24
 * dp; chip 13 -> 9 dp).
 */
internal val PLAY_CORNER_PLAYING = 12.dp
internal val PLAY_CORNER_PAUSED = 24.dp
internal val CHIP_CORNER = 9.dp

internal data class MediaShadeColors(val container: Color, val onContainer: Color)

/**
 * Android's shade colours built from tones: the container is the artwork's colour at lightness 90, the glyph the same
 * hue at 10, with the colour's real chroma, so a vivid cover gives a vivid button and a dull one a dull button.
 */
internal fun mediaShadeColors(
    settings: SmartIslandSettings,
    tint: ArtworkTint?,
    hasArtwork: Boolean,
    /** The light island: the tones swap (a dark container with a pale glyph) so the buttons stand out on the pale, washed artwork. */
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

/** The shade's tones: container lightness 90, glyph lightness 10 (CIELAB L*). */
internal const val SHADE_CONTAINER_TONE = 90.0
internal const val SHADE_GLYPH_TONE = 10.0

/** The same tones on the light island: a deep container (lightness 30) with a pale glyph (95); both clear 4.5:1 against each other. */
internal const val SHADE_CONTAINER_TONE_LIGHT = 30.0
internal const val SHADE_GLYPH_TONE_LIGHT = 95.0

/** Chroma bounds (CIELAB C*) of the artwork colour; the shade gave 4.5 for a dark cover and 69 for a vivid one. */
internal const val SHADE_MIN_CHROMA = 4.0
internal const val SHADE_MAX_CHROMA = 70.0

/** The glyph's chroma stays low even for a vivid cover. */
internal const val SHADE_GLYPH_MAX_CHROMA = 22.0

internal fun toneColor(tone: Double, chroma: Double, hueDeg: Float): Color {
    val rgb = com.agupta07505.smartisland.util.LabColor.lchToSrgb(tone, chroma, hueDeg.toDouble())
    return Color(rgb[0], rgb[1], rgb[2])
}

/** The colours eased over 350 ms, so a track change fades the buttons instead of snapping. */
@Composable
internal fun animatedShadeColors(target: MediaShadeColors): MediaShadeColors {
    val container by animateColorAsState(target.container, tween(350), label = "mediaShadeContainer")
    val onContainer by animateColorAsState(target.onContainer, tween(350), label = "mediaShadeOnContainer")
    return MediaShadeColors(container, onContainer)
}

internal fun mediaIsExplicit(meta: android.media.MediaMetadata): Boolean =
    runCatchingLogged("MediaShade", "Failed to read the explicit flag") {
        meta.description?.extras?.getLong(EXPLICIT_KEY, 0L) == 1L || meta.getLong(EXPLICIT_KEY) == 1L
    } == true

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

internal fun mediaOutputIcon(kind: com.agupta07505.smartisland.util.MediaOutputKind): androidx.compose.ui.graphics.vector.ImageVector =
    when (kind) {
        com.agupta07505.smartisland.util.MediaOutputKind.Phone -> Icons.Rounded.PhoneAndroid
        com.agupta07505.smartisland.util.MediaOutputKind.Bluetooth -> Icons.Rounded.Bluetooth
        com.agupta07505.smartisland.util.MediaOutputKind.Headphones -> Icons.Rounded.Headphones
        com.agupta07505.smartisland.util.MediaOutputKind.Usb -> Icons.Rounded.Usb
        com.agupta07505.smartisland.util.MediaOutputKind.Hdmi -> Icons.Rounded.Tv
        com.agupta07505.smartisland.util.MediaOutputKind.Cast -> Icons.Rounded.Cast
    }

/** Where the music plays now: read when the card opens, then kept current while shown (a device connecting or leaving, a cast move). */
@Composable
internal fun rememberMediaOutput(controller: android.media.session.MediaController?): com.agupta07505.smartisland.util.MediaOutput {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Reading the audio routes is a binder call: it runs in the background (the card must not wait for it on the tap
    // that opens it).
    var output by androidx.compose.runtime.remember(controller) {
        androidx.compose.runtime.mutableStateOf(lastMediaOutput ?: com.agupta07505.smartisland.util.MediaOutput.Phone)
    }
    androidx.compose.runtime.DisposableEffect(controller) {
        val audio = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val refresh = {
            scope.launch {
                val read = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    com.agupta07505.smartisland.util.MediaOutputReader.read(context, controller)
                }
                lastMediaOutput = read
                output = read
            }
            Unit
        }
        val deviceCallback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) = refresh()
        }
        val sessionCallback = object : android.media.session.MediaController.Callback() {
            override fun onAudioInfoChanged(info: android.media.session.MediaController.PlaybackInfo) = refresh()
        }
        audio?.registerAudioDeviceCallback(deviceCallback, android.os.Handler(android.os.Looper.getMainLooper()))
        controller?.registerCallback(sessionCallback)
        refresh()
        onDispose {
            audio?.unregisterAudioDeviceCallback(deviceCallback)
            controller?.unregisterCallback(sessionCallback)
        }
    }
    return output
}

private var lastMediaOutput: com.agupta07505.smartisland.util.MediaOutput? = null

/** Audio output chip, top right: the output's glyph and name. [onClick] null draws it without a tap action. */
@Composable
internal fun MediaOutputChip(
    label: String,
    colors: MediaShadeColors,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.PhoneAndroid
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

/** Big tinted play / pause button: a rounded square while playing, a circle while paused. */
@Composable
internal fun MediaPlayPauseButton(
    isPlaying: Boolean,
    colors: MediaShadeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shade = animatedShadeColors(colors)
    val corner by animateDpAsState(targetValue = if (isPlaying) PLAY_CORNER_PLAYING else PLAY_CORNER_PAUSED, label = "mediaPlayCorner")
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
        Icon(imageVector = icon, contentDescription = description, tint = LocalIslandPalette.current.text, modifier = Modifier.size(24.dp))
    }
}
