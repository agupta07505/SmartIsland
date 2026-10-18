/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandNotification

/** What the alert pill says: a bold part (sender, or the count) and a regular gray part (preview, or the app). */
internal fun alertPillLabel(notification: IslandNotification): Pair<String, String> =
    if (notification.groupCount > 1) {
        "${notification.groupCount} New Messages" to "from ${notification.appName}"
    } else {
        notification.title.ifBlank { notification.appName } to notification.text
    }

/** How long a masked alert's text takes to fade in on unlock. */
internal const val LOCK_CROSSFADE_MS = 220

/** The green "unread" dot at the pill's right end. */
private val AlertDotColor = Color(0xFF34D399)

/** Overlapping circular icons, oldest at the back, for a folded group. */
@Composable
internal fun StackedAlertIcons(icons: List<Bitmap>, size: Dp = 22.dp, overlap: Dp = 9.dp) {
    Box(modifier = Modifier.size(width = size + overlap * (icons.size - 1).coerceAtLeast(0), height = size)) {
        icons.forEachIndexed { index, bitmap ->
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier
                    .offset(x = overlap * index)
                    .size(size)
                    // A black ring separates each icon from the one behind it.
                    .background(Color.Black, CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Sender in bold, then the message preview in gray, on one line that scrolls sideways when it does not
 * fit in [maxTextWidth] and fades out at the right edge, followed by the green dot.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlertPillText(notification: IslandNotification, maxTextWidth: Dp, showDot: Boolean = true) {
    val (strong, soft) = remember(notification.key, notification.groupCount, notification.title, notification.text) {
        alertPillLabel(notification)
    }
    val label = remember(strong, soft) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append(strong) }
            if (soft.isNotBlank()) {
                append("  ")
                withStyle(SpanStyle(color = Color(0xFF9CA3AF), fontWeight = FontWeight.Normal)) { append(soft) }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Unlocking reveals a masked alert's text with a crossfade (Rule_Book §8.1); other updates swap in place.
        AnimatedContent(
            targetState = label to notification.isMasked,
            contentKey = { it.second },
            transitionSpec = { fadeIn(tween(LOCK_CROSSFADE_MS)) togetherWith fadeOut(tween(LOCK_CROSSFADE_MS)) },
            label = "alertPillMask"
        ) { (shown, _) ->
            Text(
                text = shown,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .widthIn(max = maxTextWidth)
                    .fadingRightEdge(14.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1_200, velocity = 28.dp)
            )
        }
        if (showDot) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AlertDotColor))
        }
    }
}

/** Fades the last [edge] of the content to transparent, so clipped text dissolves rather than being cut. */
internal fun Modifier.fadingRightEdge(edge: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val width = edge.toPx().coerceAtMost(size.width)
        if (width > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - width,
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * The folded group's left-hand badge: the stacked icons when the alerts brought bitmaps, otherwise
 * overlapping lettered circles (one per message, at most three) so a group never looks like a single alert.
 */
@Composable
internal fun StackedAlertBadges(notification: IslandNotification, color: Color, size: Dp = 22.dp, overlap: Dp = 9.dp) {
    if (notification.groupIcons.size > 1) {
        StackedAlertIcons(icons = notification.groupIcons, size = size, overlap = overlap)
        return
    }
    val count = notification.groupCount.coerceIn(2, 3)
    val letter = notification.appName.firstOrNull()?.uppercase() ?: "S"
    Box(modifier = Modifier.size(width = size + overlap * (count - 1), height = size)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .offset(x = overlap * index)
                    .size(size)
                    .background(Color.Black, CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(text = letter, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
