/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.app.PendingIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.runCatchingLogged

/**
 * One extra button of a media card (like, shuffle, cast ...), taken from the playing app: a custom action of its
 * media session or a button of its notification.
 */
data class MediaCustomControl(
    val label: String,
    val packageName: String,
    val iconRes: Int = 0,
    val iconBitmap: Bitmap? = null,
    val sessionAction: String? = null,
    val sessionExtras: Bundle? = null,
    val pendingIntent: PendingIntent? = null
)

data class ResolvedMediaControls(
    val hasPrevious: Boolean,
    val hasPlayPause: Boolean,
    val hasNext: Boolean,
    val leftCustom: List<MediaCustomControl>,
    val rightCustom: List<MediaCustomControl>
)

object MediaControls {
    private const val MAX_CUSTOM = 4

    private val PREVIOUS_WORDS = listOf("previous", "prev", "rewind")
    private val NEXT_WORDS = listOf("next", "skip", "forward")
    private val PLAY_PAUSE_WORDS = listOf("play", "pause", "resume")

    /**
     * Session first: its supported actions say whether previous / next exist (YouTube has no previous) and its custom
     * actions are the app's extra buttons.
     */
    fun resolve(state: PlaybackState?, notification: IslandNotification?): ResolvedMediaControls {
        val pkg = notification?.packageName.orEmpty()
        if (state != null && state.actions != 0L) {
            val customs = state.customActions.orEmpty().take(MAX_CUSTOM).map { action ->
                MediaCustomControl(
                    label = action.name?.toString().orEmpty(),
                    packageName = pkg,
                    iconRes = action.icon,
                    sessionAction = action.action,
                    sessionExtras = action.extras
                )
            }
            return build(
                hasPrevious = state.actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
                hasPlayPause = true,
                hasNext = state.actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
                customs = customs
            )
        }
        return fromNotification(notification, pkg)
    }

    private fun fromNotification(notification: IslandNotification?, pkg: String): ResolvedMediaControls {
        var previous = false
        var next = false
        var playPause = false
        val customs = ArrayList<MediaCustomControl>()
        for (action in notification?.actionIntents.orEmpty()) {
            if (action.pendingIntent == null) continue
            val t = action.title.lowercase()
            when {
                PREVIOUS_WORDS.any { t.contains(it) } -> previous = true
                NEXT_WORDS.any { t.contains(it) } -> next = true
                PLAY_PAUSE_WORDS.any { t.contains(it) } -> playPause = true
                customs.size < MAX_CUSTOM -> customs += MediaCustomControl(
                    label = action.title,
                    packageName = pkg,
                    iconBitmap = action.icon,
                    pendingIntent = action.pendingIntent
                )
            }
        }
        // Nothing readable at all: keep the basic transport so the card is not empty.
        if (!previous && !next && !playPause && customs.isEmpty()) {
            return build(hasPrevious = true, hasPlayPause = true, hasNext = true, customs = emptyList())
        }
        return build(previous, true, next, customs)
    }

    private fun build(
        hasPrevious: Boolean,
        hasPlayPause: Boolean,
        hasNext: Boolean,
        customs: List<MediaCustomControl>
    ): ResolvedMediaControls {
        val leftCount = (customs.size + 1) / 2
        return ResolvedMediaControls(
            hasPrevious = hasPrevious,
            hasPlayPause = hasPlayPause,
            hasNext = hasNext,
            leftCustom = customs.take(leftCount),
            rightCustom = customs.drop(leftCount)
        )
    }

    /** Draws a drawable of another app's package as a bitmap, or null when it cannot be loaded. */
    fun loadPackageIcon(context: Context, packageName: String, resId: Int): ImageBitmap? {
        if (packageName.isEmpty() || resId == 0) return null
        return runCatchingLogged("MediaControls", "Custom action icon could not be loaded") {
            val pkgContext = context.createPackageContext(packageName, 0)
            val drawable = pkgContext.resources.getDrawable(resId, pkgContext.theme) ?: return@runCatchingLogged null
            val size = 96
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        }
    }

    /** Stand-in glyph when the app's own icon cannot be read, chosen from the button's label. */
    fun fallbackIcon(label: String): ImageVector {
        val t = label.lowercase()
        return when {
            t.contains("shuffle") -> Icons.Rounded.Shuffle
            t.contains("repeat") || t.contains("loop") -> Icons.Rounded.Repeat
            t.contains("thumb") || t.contains("like") && !t.contains("unlike") -> Icons.Rounded.ThumbUp
            t.contains("favorite") || t.contains("heart") || t.contains("save") -> Icons.Rounded.FavoriteBorder
            t.contains("cast") || t.contains("connect") || t.contains("device") -> Icons.Rounded.Cast
            t.contains("liked") || t.contains("remove") -> Icons.Rounded.Favorite
            else -> Icons.Rounded.MoreHoriz
        }
    }
}

@Composable
fun CustomControlButton(
    control: MediaCustomControl,
    controller: MediaController?,
    notification: IslandNotification?,
    size: Dp,
    onUsed: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(control) {
        control.iconBitmap?.asImageBitmap()
            ?: MediaControls.loadPackageIcon(context, control.packageName, control.iconRes)
    }
    Box(
        modifier = Modifier
            .size(size)
            .bounceClick {
                onUsed()
                runCatchingLogged("MusicExpanded", "Custom media button failed") {
                    val pending = control.pendingIntent
                    if (control.sessionAction != null && controller != null) {
                        controller.transportControls.sendCustomAction(control.sessionAction, control.sessionExtras)
                    } else if (pending != null) {
                        sendIntentWithOptions(context, pending)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = control.label,
                colorFilter = ColorFilter.tint(LocalIslandPalette.current.text),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = MediaControls.fallbackIcon(control.label),
                contentDescription = control.label,
                tint = LocalIslandPalette.current.text,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
