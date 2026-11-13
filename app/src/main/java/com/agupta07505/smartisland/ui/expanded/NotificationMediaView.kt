/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.agupta07505.smartisland.model.CardStepState
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.NotificationMedia
import com.agupta07505.smartisland.model.PictureSize
import com.agupta07505.smartisland.model.ReplyDrafts
import com.agupta07505.smartisland.util.ReaderScrollTracker
import com.agupta07505.smartisland.util.quietly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max

class MediaExpansion(
    val key: String?,
    val index: Int,
    /** Landscape, the picture is its own island so the card keeps its content. */
    val besideCard: Boolean = false,
    /** Landscape, selector open beside the card, no picture open yet. */
    val selectorOpen: Boolean = false,
    val onChange: (key: String?, index: Int) -> Unit
) {
    val isOpen: Boolean get() = key != null
}

val LocalMediaExpansion = compositionLocalOf { MediaExpansion(null, 0) { _, _ -> } }

/**
 * Held by the view model so the card, its arrow and the swipe gestures share it (model/CardDepth.kt)
 * Any other card reads the first state
 */
class CardStepsAccess(
    private val state: CardStepState,
    private val pictureKey: String?,
    private val pictureIndex: Int,
    val set: (key: String, steps: CardSteps) -> Unit,
    val reportExpandable: (key: String, expandable: Boolean) -> Unit
) {
    fun stepsFor(key: String?): CardSteps = state.steps(key, pictureKey, pictureIndex)
}

val LocalCardSteps = compositionLocalOf {
    CardStepsAccess(CardStepState(), null, 0, { _, _ -> }, { _, _ -> })
}

/**
 * Reader text and thumbnails report scrolling so the gesture handler can refuse a tap that only stops a fling
 * Provided once at the top of the overlay
 */
val LocalReaderScroll = compositionLocalOf { ReaderScrollTracker() }

/** Reports drag and fling (state) and every offset change (moves), call it next to the list's `verticalScroll`. */
@Composable
fun ReportReaderScroll(state: ScrollState) {
    val tracker = LocalReaderScroll.current
    DisposableEffect(tracker) {
        // A reader that leaves the screen mid-fling must not leave the island thinking it still scrolls
        onDispose { tracker.reset() }
    }
    LaunchedEffect(state, tracker) {
        coroutineScope {
            launch {
                snapshotFlow { state.isScrollInProgress }.drop(1).collect {
                    tracker.onScrollState(it, SystemClock.uptimeMillis())
                }
            }
            launch {
                snapshotFlow { state.value }.drop(1).collect {
                    tracker.onMoved(SystemClock.uptimeMillis())
                }
            }
        }
    }
}

/** Provided once at the top of the overlay (see OverlayIsland). */
val LocalReplyDrafts = compositionLocalOf { ReplyDrafts() }

internal val EXPANDED_MEDIA_HEIGHT = PictureSize.FULL_DP.dp

internal val PICTURE_MID_HEIGHT = PictureSize.MID_DP.dp

/** Stickers and GIFs play, everything else is the still image. */
@Composable
fun NotificationMediaView(
    media: NotificationMedia,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxSidePx: Int = 512
) {
    val still = remember(media.still) { media.still.asImageBitmap() }
    val bytes = media.animatedBytes
    if (bytes == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        Image(bitmap = still, contentDescription = null, contentScale = contentScale, modifier = modifier)
        return
    }
    AnimatedMediaView(bytes, still, modifier, contentScale, maxSidePx)
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AnimatedMediaView(
    bytes: ByteArray,
    still: ImageBitmap,
    modifier: Modifier,
    contentScale: ContentScale,
    maxSidePx: Int
) {
    val drawable by produceState<Drawable?>(null, bytes, maxSidePx) {
        value = withContext(Dispatchers.Default) {
            quietly {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                    val longest = max(info.size.width, info.size.height)
                    if (longest > maxSidePx) {
                        val scale = maxSidePx.toFloat() / longest
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }
        }
    }
    val current = drawable
    if (current == null) {
        // The animation is still being decoded: show the first frame meanwhile
        Image(bitmap = still, contentDescription = null, contentScale = contentScale, modifier = modifier)
        return
    }
    DisposableEffect(current) {
        (current as? AnimatedImageDrawable)?.let {
            it.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            it.start()
        }
        onDispose { (current as? AnimatedImageDrawable)?.stop() }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = if (contentScale == ContentScale.Crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            }
        },
        update = { it.setImageDrawable(current) }
    )
}
