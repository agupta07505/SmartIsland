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
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.agupta07505.smartisland.model.NotificationMedia
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import androidx.compose.runtime.getValue

/**
 * Which notification's picture is open at full size in the expanded island: the card key and the picture's position.
 */
class MediaExpansion(
    val key: String?,
    val index: Int,
    /** Landscape: the open picture is drawn as its own island beside the card, so the card keeps its content. */
    val besideCard: Boolean = false,
    /** Landscape: the image selector is open beside the card (no single picture is open yet). */
    val selectorOpen: Boolean = false,
    val onChange: (key: String?, index: Int) -> Unit
) {
    val isOpen: Boolean get() = key != null
}

val LocalMediaExpansion = compositionLocalOf { MediaExpansion(null, 0) { _, _ -> } }

/**
 * The open step (preview / reader / picture) of the shown card, held by the view model so the card, its arrow and the
 * swipe gestures share it (model/CardDepth.kt). Any other card reads the first state.
 */
class CardStepsAccess(
    private val state: com.agupta07505.smartisland.model.CardStepState,
    private val pictureKey: String?,
    private val pictureIndex: Int,
    val set: (key: String, steps: com.agupta07505.smartisland.model.CardSteps) -> Unit,
    val reportExpandable: (key: String, expandable: Boolean) -> Unit
) {
    fun stepsFor(key: String?): com.agupta07505.smartisland.model.CardSteps = state.steps(key, pictureKey, pictureIndex)
}

val LocalCardSteps = compositionLocalOf {
    CardStepsAccess(com.agupta07505.smartisland.model.CardStepState(), null, 0, { _, _ -> }, { _, _ -> })
}

/**
 * Where the reader text and its thumbnail column report scrolling, so the island's gesture handler can refuse a tap
 * that only stops a fling. Provided once at the top of the overlay.
 */
val LocalReaderScroll = compositionLocalOf { com.agupta07505.smartisland.util.ReaderScrollTracker() }

/**
 * Reports a reader list's scrolling to [LocalReaderScroll]: drag and fling (state) and every offset change (moves).
 * Call it next to the list's `verticalScroll`.
 */
@androidx.compose.runtime.Composable
fun ReportReaderScroll(state: androidx.compose.foundation.ScrollState) {
    val tracker = LocalReaderScroll.current
    androidx.compose.runtime.DisposableEffect(tracker) {
        // A reader that leaves the screen mid-fling must not leave the island thinking it still scrolls.
        onDispose { tracker.reset() }
    }
    androidx.compose.runtime.LaunchedEffect(state, tracker) {
        kotlinx.coroutines.coroutineScope {
            launch {
                androidx.compose.runtime.snapshotFlow { state.isScrollInProgress }.drop(1).collect {
                    tracker.onScrollState(it, android.os.SystemClock.uptimeMillis())
                }
            }
            launch {
                androidx.compose.runtime.snapshotFlow { state.value }.drop(1).collect {
                    tracker.onMoved(android.os.SystemClock.uptimeMillis())
                }
            }
        }
    }
}

/** The view model's half-typed replies; provided once at the top of the overlay (see OverlayIsland). */
val LocalReplyDrafts = compositionLocalOf { com.agupta07505.smartisland.model.ReplyDrafts() }

/** Height the full-size picture gets: the tallest an expanded island ever grows. */
internal val EXPANDED_MEDIA_HEIGHT = com.agupta07505.smartisland.model.PictureSize.FULL_DP.dp

/** Height the half-size picture step gets: halfway between the card's smallest height and [EXPANDED_MEDIA_HEIGHT]. */
internal val PICTURE_MID_HEIGHT = com.agupta07505.smartisland.model.PictureSize.MID_DP.dp

/** A notification picture. Animated stickers and GIFs play; everything else is the still image. */
@Composable
fun NotificationMediaView(
    media: NotificationMedia,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxSidePx: Int = 512
) {
    val still = androidx.compose.runtime.remember(media.still) { media.still.asImageBitmap() }
    val bytes = media.animatedBytes
    if (bytes == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        Image(bitmap = still, contentDescription = null, contentScale = contentScale, modifier = modifier)
        return
    }
    AnimatedMediaView(bytes, still, modifier, contentScale, maxSidePx)
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AnimatedMediaView(
    bytes: ByteArray,
    still: androidx.compose.ui.graphics.ImageBitmap,
    modifier: Modifier,
    contentScale: ContentScale,
    maxSidePx: Int
) {
    val drawable by produceState<Drawable?>(null, bytes, maxSidePx) {
        value = withContext(Dispatchers.Default) {
            runCatching {
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
            }.getOrNull()
        }
    }
    val current = drawable
    if (current == null) {
        // The animation is still being decoded: show the first frame meanwhile.
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
