/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotificationMedia
import com.agupta07505.smartisland.ui.bounceClick

// row height (the picture fills it) and widest picture in the preview step
internal val PREVIEW_ROW_HEIGHT = 96.dp
private val PREVIEW_MEDIA_MAX_WIDTH = 140.dp

@Composable
internal fun ReaderThumbnails(
    notification: IslandNotification,
    mediaList: List<NotificationMedia>,
    stepMediaCount: Int,
    steps: CardSteps,
    cardSteps: CardStepsAccess,
    frozen: Boolean
) {
    val thumbScrollState = rememberScrollState()
    if (!frozen) ReportReaderScroll(thumbScrollState)
    Column(
        modifier = Modifier
            .heightIn(max = EXPANDED_TEXT_MAX_HEIGHT)
            .verticalScroll(thumbScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mediaList.forEachIndexed { index, media ->
            MediaThumbnail(
                media = media,
                onExpand = when {
                    frozen -> null
                    // The picture is the tap target: a sticker does nothing, 1 picture opens at half size, several
                    // open the selector
                    stepMediaCount == 0 -> null
                    stepMediaCount >= 2 -> ({
                        cardSteps.set(
                            notification.key,
                            CardSteps(reader = true, preview = steps.preview, selector = true)
                        )
                    })
                    else -> ({
                        cardSteps.set(
                            notification.key,
                            CardSteps(
                                reader = true, preview = steps.preview, pictureIndex = index, pictureHalf = true
                            )
                        )
                    })
                }
            )
        }
    }
}

@Composable
internal fun PreviewPicture(last: NotificationMedia) {
    val ratio = (last.still.width.toFloat() / last.still.height.coerceAtLeast(1)).coerceIn(0.7f, 1.6f)
    NotificationMediaView(
        media = last,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = PREVIEW_MEDIA_MAX_WIDTH)
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(14.dp)),
        contentScale = if (last.isSticker) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        },
        maxSidePx = 384
    )
}

@Composable
private fun MediaThumbnail(media: NotificationMedia, onExpand: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LocalIslandPalette.current.tint(0.08f))
            .then(if (onExpand != null) Modifier.bounceClick(onExpand) else Modifier)
    ) {
        NotificationMediaView(
            media = media,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (media.isSticker) {
                ContentScale.Fit
            } else {
                ContentScale.Crop
            },
            maxSidePx = 192
        )
    }
}

/**
 * Largest picture size, corner arrow closes it, a tap on the picture does nothing so it cannot open the app by accident
 */
@Composable
internal fun ExpandedMediaViewer(
    media: NotificationMedia,
    onClose: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(EXPANDED_MEDIA_HEIGHT)
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
        NotificationMediaView(
            media = media,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Fit,
            maxSidePx = 1024
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .bounceClick(onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Close picture",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
