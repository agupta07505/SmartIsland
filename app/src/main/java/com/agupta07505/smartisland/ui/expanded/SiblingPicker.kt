/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.bounceClick
import com.agupta07505.smartisland.util.formatNotificationTime
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val HEADER_HEIGHT = 44.dp
private val ROW_HEIGHT = 56.dp
/** Landscape split layout: slightly shorter rows so the picker island stays small. */
private val COMPACT_ROW_HEIGHT = 48.dp
private val VERTICAL_PADDING = 12.dp
private val ROW_GAP = 4.dp
private const val MAX_VISIBLE_ROWS = 3

/**
 * Height of the picker for [count] cards: a header, up to three rows, the rest scrolls. Kept
 * inside the expanded island's own height range (72 to 240 dp) so the island never outgrows it.
 */
internal fun siblingPickerHeight(count: Int, compact: Boolean = false): Dp =
    HEADER_HEIGHT + (if (compact) COMPACT_ROW_HEIGHT else ROW_HEIGHT) * count.coerceIn(1, MAX_VISIBLE_ROWS) +
        ROW_GAP * (count.coerceIn(1, MAX_VISIBLE_ROWS) - 1) + VERTICAL_PADDING

/**
 * Index of the row to show first so the current card is visible without scrolling; 0 when it
 * already fits among the first [MAX_VISIBLE_ROWS] rows.
 */
internal fun siblingPickerFirstVisible(count: Int, currentPosition: Int): Int =
    if (currentPosition < MAX_VISIBLE_ROWS) 0
    else (currentPosition - MAX_VISIBLE_ROWS + 1).coerceAtMost((count - MAX_VISIBLE_ROWS).coerceAtLeast(0))

/**
 * True when a drag belongs to the picker's list rather than to an island swipe: the picker was open
 * when the touch began, the touch landed on the rows (below the header, [downYDp] from the island's
 * top) and the finger actually moved. Any direction counts: a list at its top or bottom edge
 * consumes nothing, so the island would otherwise read the drag as a swipe up/down, and sideways
 * drags over the rows must not fire the expanded left/right swipe actions either.
 */
internal fun siblingPickerOwnsDrag(
    pickerOpen: Boolean,
    downYDp: Float,
    dragX: Float,
    dragY: Float,
    // Landscape: the picker island fills only the right part of the window, so only touches there count.
    sideBySide: Boolean = false,
    downXFraction: Float = 0f,
    // Landscape split layout: the picker's inner edge as a fraction of the island's width (its
    // left edge normally, its right edge when the picker is on the left).
    pickerLeftFraction: Float = 0.5f,
    // Reverse landscape: the picker island is on the left, so touches left of its edge count.
    pickerOnLeft: Boolean = false
): Boolean = siblingPickerOwnsTouch(pickerOpen, sideBySide, downXFraction, pickerLeftFraction, pickerOnLeft) &&
    downYDp >= HEADER_HEIGHT.value &&
    (kotlin.math.abs(dragX) > DRAG_MOVE_EPSILON || kotlin.math.abs(dragY) > DRAG_MOVE_EPSILON)

/**
 * True when a touch that began at [downXFraction] of the island's width landed on the picker: it was open, and (landscape
 * split layout) the touch is on the picker's island, not on the card's. The island's own tap never runs for such a
 * touch: a tap on the picker belongs to its rows and its close button, and must not open the notification behind it.
 */
internal fun siblingPickerOwnsTouch(
    pickerOpen: Boolean,
    sideBySide: Boolean,
    downXFraction: Float,
    pickerLeftFraction: Float,
    pickerOnLeft: Boolean
): Boolean = pickerOpen &&
    (!sideBySide || if (pickerOnLeft) downXFraction <= pickerLeftFraction else downXFraction >= pickerLeftFraction)

/** A touch on a picker row is a tap only while the finger stays within this many dp (scaled by the gesture sensitivity). */
internal const val PICKER_TAP_SLOP_DP = 10f

/**
 * Taps are ignored for this long after the rows moved (the picker just opened, a card arrived or left): they were aimed
 * at where a row was before the move.
 */
internal const val PICKER_LAYOUT_SETTLE_MS = 250L

/**
 * Whether a touch on a picker row ends as a tap that selects the row.
 *
 * Not when the finger travelled further than [slopPx] ([peakTravelPx], measured on the screen, so it also counts a
 * scroll that carried the row along), when the list itself moved during the touch ([scrollDeltaPx]), when the touch began
 * on a list that was still flinging (it only stopped the list), or when the rows moved just before or during the touch.
 */
internal fun siblingRowTapAccepted(
    peakTravelPx: Float,
    slopPx: Float,
    scrollDeltaPx: Int,
    startedWhileScrolling: Boolean,
    layoutAgeAtDownMs: Long,
    layoutShiftedDuringPress: Boolean
): Boolean = !startedWhileScrolling &&
    scrollDeltaPx == 0 &&
    peakTravelPx <= slopPx &&
    layoutAgeAtDownMs >= PICKER_LAYOUT_SETTLE_MS &&
    !layoutShiftedDuringPress

/**
 * Row order that does not jump under the finger. Cards that were already listed keep their place; cards that arrived
 * since are appended at the end; cards that left are dropped. (The cards come newest first, so without this a card that
 * is re-posted while the list is being scrolled would move its row, and the tap would land on another card.)
 */
internal fun stableSiblingOrder(previous: List<String>, current: List<String>): List<String> {
    val present = current.toHashSet()
    val kept = previous.filter { it in present }
    val keptSet = kept.toHashSet()
    return kept + current.filter { it !in keptSet }
}

/**
 * True while the expanded notification's text is open for reading (it scrolls): any drag that
 * moves belongs to the text, so it cannot dismiss the card or fire a swipe action. A tap, the
 * collapse button and a tap outside still close it.
 */
internal fun readingTextOwnsDrag(readingText: Boolean, dragX: Float, dragY: Float): Boolean =
    readingText && (kotlin.math.abs(dragX) > DRAG_MOVE_EPSILON || kotlin.math.abs(dragY) > DRAG_MOVE_EPSILON)

private const val DRAG_MOVE_EPSILON = 0.5f

/** The portrait sibling list sits on the island surface; this is the least opaque that keeps its white text readable. */
internal const val SIBLING_PICKER_MIN_OPACITY = 0.85f

/**
 * What the picker's surface actually uses: fully opaque, like the card it sits next to. At 0.85 the
 * app behind the island still showed through the rows and made them hard to read.
 */
internal const val SIBLING_PICKER_SURFACE_OPACITY = 1f

/**
 * Alpha of an extra pill-coloured layer behind the picker rows so the surface plus this layer
 * reach at least [target] opacity: `1 - (1 - surface) * (1 - backing) = target`. Zero when the
 * island surface is already that opaque.
 */
internal fun siblingPickerBackingAlpha(surfaceOpacity: Float, target: Float = SIBLING_PICKER_MIN_OPACITY): Float {
    val surface = surfaceOpacity.coerceIn(0f, 1f)
    if (surface >= target) return 0f
    return (1f - (1f - target) / (1f - surface)).coerceIn(0f, 1f)
}

/**
 * The list of every card of the current app, drawn inside the expanded island (no extra window).
 * Tapping a row selects that card; the close button (or the caller's own rules) dismisses it.
 */
@Composable
fun SiblingPicker(
    siblings: List<IslandNotification>,
    currentKey: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tapSlop: Dp = PICKER_TAP_SLOP_DP.dp
) {
    if (siblings.isEmpty()) return
    val rowHeight = if (compact) COMPACT_ROW_HEIGHT else ROW_HEIGHT
    val rowPx = with(LocalDensity.current) { (rowHeight + ROW_GAP).roundToPx() }
    val tapSlopPx = with(LocalDensity.current) { tapSlop.toPx() }

    // Rows keep their place while the picker is open: a card that is re-posted must not move under the finger.
    val orderHolder = remember { arrayOf(emptyList<String>()) }
    val keys = stableSiblingOrder(orderHolder[0], siblings.map { it.key })
    orderHolder[0] = keys
    val byKey = siblings.associateBy { it.key }
    val ordered = keys.mapNotNull { byKey[it] }

    // When the rows last moved (opened, a card came or went). Taps right after that are ignored.
    val layoutShift = remember { longArrayOf(android.os.SystemClock.uptimeMillis()) }
    LaunchedEffect(keys) { layoutShift[0] = android.os.SystemClock.uptimeMillis() }

    // The rows are lazy. Only the few that are on screen are composed and measured (an app with 23 cards
    // does not compose all 23 rows in the frame the picker opens). The list starts on the row that keeps the current card in
    // view, so there is no first frame at the top and a jump after it.
    val startIndex = siblingPickerFirstVisible(
        ordered.size,
        ordered.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)
    )
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    // Once the user has touched the list, it is theirs: nothing scrolls it back to the current card.
    var touched by remember { mutableStateOf(false) }
    // Open with the current card in view when the app has more cards than fit.
    LaunchedEffect(ordered.size, currentKey) {
        if (touched) return@LaunchedEffect
        val position = ordered.indexOfFirst { it.key == currentKey }.coerceAtLeast(0)
        listState.scrollToItem(siblingPickerFirstVisible(ordered.size, position))
    }
    val appName = ordered.first().appName

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(siblingPickerHeight(ordered.size, compact))
            .padding(horizontal = 14.dp, vertical = VERTICAL_PADDING / 2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT - VERTICAL_PADDING / 2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.sibling_picker_title, appName, ordered.size),
                color = LocalIslandPalette.current.text.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LocalIslandPalette.current.tint(0.12f))
                    .bounceClick(onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.sibling_picker_close),
                    tint = LocalIslandPalette.current.text,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(ROW_GAP),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(items = ordered, key = { it.key }, contentType = { SIBLING_ROW_TYPE }) { card ->
                SiblingRow(
                    card = card,
                    isCurrent = card.key == currentKey,
                    rowHeight = rowHeight,
                    listState = listState,
                    rowPx = rowPx,
                    tapSlopPx = tapSlopPx,
                    layoutShiftStamp = { layoutShift[0] },
                    onTouchStart = { touched = true },
                    onClick = { onSelect(card.key) }
                )
            }
        }
    }
}

private const val SIBLING_ROW_TYPE = "sibling_row"

/** The list's scroll position in px, from the first visible row (rows are [rowPx] apart, the gap included). */
internal fun siblingScrollPositionPx(firstVisibleIndex: Int, firstVisibleOffsetPx: Int, rowPx: Int): Int =
    firstVisibleIndex * rowPx + firstVisibleOffsetPx

private fun LazyListState.scrollPositionPx(rowPx: Int): Int =
    siblingScrollPositionPx(firstVisibleItemIndex, firstVisibleItemScrollOffset, rowPx)

/**
 * Tap handling of a picker row. Replaces `clickable`, which still fired when a scroll began with less than the touch
 * slop of movement, when a touch only stopped a flinging list, and when the rows had just moved. A row is pressed
 * (brighter) only while the touch can still end as a tap, so the user sees a scroll take the row away.
 *
 * Only an accepted tap consumes the lift (as `clickable` does): a scroll's own lift must reach the list so it can fling.
 */
internal fun Modifier.siblingRowTap(
    listState: LazyListState,
    rowPx: Int,
    tapSlopPx: Float,
    layoutShiftStamp: () -> Long,
    onTouchStart: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
    onTap: () -> Unit
): Modifier = pointerInput(listState, rowPx, tapSlopPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onTouchStart()
        val startedWhileScrolling = listState.isScrollInProgress
        val startScroll = listState.scrollPositionPx(rowPx)
        val shiftAtDown = layoutShiftStamp()
        val ageAtDown = android.os.SystemClock.uptimeMillis() - shiftAtDown
        var travelX = 0f
        var travelY = 0f
        var peak = 0f
        var candidate = !startedWhileScrolling && ageAtDown >= PICKER_LAYOUT_SETTLE_MS
        onPressedChange(candidate)
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                // positionChange is the finger's movement on the screen, so a scroll that carries the row along still counts.
                val move = change.positionChange()
                travelX += move.x
                travelY += move.y
                peak = maxOf(peak, hypot(travelX, travelY))
                val scrolled = abs(listState.scrollPositionPx(rowPx) - startScroll)
                if (change.changedToUpIgnoreConsumed()) {
                    val accepted = !change.isConsumed && siblingRowTapAccepted(
                        peakTravelPx = peak,
                        slopPx = tapSlopPx,
                        scrollDeltaPx = scrolled,
                        startedWhileScrolling = startedWhileScrolling,
                        layoutAgeAtDownMs = ageAtDown,
                        layoutShiftedDuringPress = layoutShiftStamp() != shiftAtDown
                    )
                    if (accepted) {
                        change.consume()
                        onTap()
                    }
                    return@awaitEachGesture
                }
                val stillTap = candidate && peak <= tapSlopPx && scrolled == 0 && !change.isConsumed &&
                    layoutShiftStamp() == shiftAtDown
                if (stillTap != candidate) {
                    candidate = stillTap
                    onPressedChange(candidate)
                }
            }
        } finally {
            onPressedChange(false)
        }
    }
}

@Composable
private fun SiblingRow(
    card: IslandNotification,
    isCurrent: Boolean,
    rowHeight: Dp,
    listState: LazyListState,
    rowPx: Int,
    tapSlopPx: Float,
    layoutShiftStamp: () -> Long,
    onTouchStart: () -> Unit,
    onClick: () -> Unit
) {
    val bitmap = card.largeIcon ?: card.icon
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    // True only while this touch can still end as a tap; a scroll, a long drag or a moving list clears it.
    var pressed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clip(RoundedCornerShape(16.dp))
            // Every row is a tile like the expanded card's own elements; the current one is brighter, and a row that is
            // being pressed (and would be selected on lift) a little more.
            .background(LocalIslandPalette.current.tint((if (isCurrent) 0.18f else 0.08f) + (if (pressed) 0.10f else 0f)))
            .siblingRowTap(
                listState = listState,
                rowPx = rowPx,
                tapSlopPx = tapSlopPx,
                layoutShiftStamp = layoutShiftStamp,
                onTouchStart = onTouchStart,
                onPressedChange = { pressed = it },
                onTap = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalIslandPalette.current.tint(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title.ifBlank { card.appName },
                color = LocalIslandPalette.current.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val body = card.text.takeIf { it.isNotBlank() }
            if (body != null) {
                Text(
                    text = body,
                    color = LocalIslandPalette.current.text.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = remember(card.timeMillis) { formatNotificationTime(card.timeMillis) },
            color = LocalIslandPalette.current.text.copy(alpha = 0.65f),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
