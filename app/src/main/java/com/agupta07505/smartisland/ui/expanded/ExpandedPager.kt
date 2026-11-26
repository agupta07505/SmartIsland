/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PagerSelection
import com.agupta07505.smartisland.model.ReplyDrafts
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.TorchCardSize
import com.agupta07505.smartisland.model.appGroupPageIndices
import com.agupta07505.smartisland.model.appGroupPageOf
import com.agupta07505.smartisland.ui.LocalCardShown
import com.agupta07505.smartisland.ui.defaultEstimatedHeightForMode
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ReaderScrollTracker
import com.agupta07505.smartisland.util.neighbourProbeOrder
import kotlinx.coroutines.delay
import kotlin.math.abs

internal class ExpandedPager(
    val state: PagerState,
    val pageCards: List<Int>,
    val selectedPage: Int,
    private val notifications: List<IslandNotification>
) {
    fun cardAt(page: Int): IslandNotification? =
        pageCards.getOrNull(page)?.let { notifications.getOrNull(it) }
}

@Composable
internal fun rememberExpandedPager(
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    siblingPickerOpen: Boolean,
    onPageSelected: (Int) -> Unit,
    snapPageSync: () -> Boolean
): ExpandedPager {
    // 1 page per app; siblings are reached through the sibling picker and gestures
    val pageCards = appGroupPageIndices(notifications, selectedIndex)
    val selectedPage = appGroupPageOf(notifications, selectedIndex)
        .coerceIn(0, (pageCards.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { pageCards.size }
    )

    val pageGroups = PagerSelection.pageGroups(notifications, selectedIndex)
    val pagerSync = remember { PagerSyncState() }
    val shapeChanged = PagerSelection.shapeChanged(pagerSync.committedGroups, pageGroups)
    SideEffect {
        if (shapeChanged) {
            if (pagerState.isScrollInProgress && pagerSync.draggedByFinger) {
                // a finger is on the pager: do not pull it away
                pagerSync.shapeChangedDuringDrag = true
            } else {
                val target = PagerSelection.jumpTarget(pagerState.currentPage, selectedPage, pageCards.size)
                if (target != null) pagerState.requestScrollToPage(target)
            }
        }
        pagerSync.committedGroups = pageGroups
    }
    val onPageSelectedNow = rememberUpdatedState(onPageSelected)
    val pageCardsNow = rememberUpdatedState(pageCards)
    val selectedPageNow = rememberUpdatedState(selectedPage)

    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) pagerSync.draggedByFinger = true
        }
    }

    // the card is not on screen yet, or has only just appeared
    val cardShownForSync = LocalCardShown.current
    val wasShownBefore = remember { BooleanArray(1) }
    val appearing = !cardShownForSync || !wasShownBefore[0]
    SideEffect { wasShownBefore[0] = cardShownForSync }

    // Restarts when the pages change shape
    LaunchedEffect(selectedPage, pageCards.size, pageGroups, siblingPickerOpen) {
        if (shapeChanged) return@LaunchedEffect
        if ((!siblingPickerOpen || snapPageSync()) &&
            selectedPage in pageCards.indices &&
            !pagerState.isScrollInProgress &&
            pagerState.currentPage != selectedPage
        ) {
            if (PagerSelection.jumpsToPage(snapPageSync() || appearing, pagerState.currentPage, selectedPage)) {
                pagerState.scrollToPage(selectedPage)
            } else {
                pagerState.animateScrollToPage(selectedPage)
            }
        }
    }

    // a page a finger dragged to becomes the open card
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { moving ->
            if (moving) return@collect
            val byFinger = pagerSync.draggedByFinger
            val shapeChangedDuringDrag = pagerSync.shapeChangedDuringDrag
            pagerSync.draggedByFinger = false
            pagerSync.shapeChangedDuringDrag = false
            val cards = pageCardsNow.value
            val settled = pagerState.settledPage
            if (byFinger && !PagerSelection.dragStillValid(shapeChangedDuringDrag)) {
                // The pages moved under the finger
                AppLogRecorder.i(
                    "IslandPager",
                    "Pager drag dropped: the cards changed during the drag (settledPage=$settled, selectedPage=${selectedPageNow.value})"
                )
                PagerSelection.jumpTarget(settled, selectedPageNow.value, cards.size)
                    ?.let { pagerState.requestScrollToPage(it) }
                return@collect
            }
            if (PagerSelection.mayWriteBack(byFinger, settled, cards.size)) {
                val newIndex = cards[settled]
                onPageSelectedNow.value(newIndex)
            }
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction != 0f) {
            val targetPage = pagerState.settledPage.coerceIn(0, (pageCards.size - 1).coerceAtLeast(0))
            if (targetPage in pageCards.indices) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    return ExpandedPager(pagerState, pageCards, selectedPage, notifications)
}

internal class PagerHeight(
    val page: Int,
    val notification: IslandNotification?,
    val targetHeight: Dp
)

/** The tallest height the shell is aimed at: the torch card's maximum, the other cards stay under 260 dp. */
private val PAGER_TARGET_MAX_HEIGHT = maxOf(260.dp, TorchCardSize.MAX_HEIGHT_DP.dp)

@Composable
internal fun rememberPagerHeight(
    pager: ExpandedPager,
    pageHeights: Map<String, Dp>,
    cardStepsAccess: CardStepsAccess,
    mediaOpenKey: String?,
    sideBySidePicker: Boolean,
    siblingPickerOpen: Boolean
): PagerHeight {
    val pagerState = pager.state
    val pageCards = pager.pageCards
    val selectedPage = pager.selectedPage
    fun cardAt(page: Int): IslandNotification? = pager.cardAt(page)

    val offsetFraction = pagerState.currentPageOffsetFraction
    // a swipe action selects the new card and the pager follows a few frames later
    val pagerIdle = !pagerState.isScrollInProgress && offsetFraction == 0f
    val currentPage =
        if (pagerIdle && !siblingPickerOpen
        && selectedPage in pageCards.indices) selectedPage else pagerState.currentPage
    val currentNotification = cardAt(currentPage)

    fun clampHeightForMode(notif: IslandNotification?, height: Dp): Dp {
        if (notif != null && notif.mode == IslandMode.Notification) {
            val shownSteps = cardStepsAccess.stepsFor(notif.key)
            return when {
                notif.key == mediaOpenKey -> if (shownSteps.pictureHalf) PICTURE_MID_HEIGHT else EXPANDED_MEDIA_HEIGHT
                !sideBySidePicker && shownSteps.depth == CardDepth.Selector -> PICTURE_MID_HEIGHT
                else -> clampCardHeightForMode(notif.mode, height)
            }
        }
        return clampCardHeightForMode(notif?.mode, height)
    }

    // The last measured height of each card: a card that is measured again holds it meanwhile, a card that arrives
    // takes its mode's estimate and never the height of another card
    val lastMeasuredDp = remember { mutableMapOf<String, Float>() }
    lastMeasuredDp.keys.retainAll(pageCards.indices.mapNotNull { cardAt(it)?.key }.toSet())
    fun heightOrHold(notif: IslandNotification?): Dp {
        val raw = notif?.let { pageHeights[it.key] }
        if (raw != null) return clampHeightForMode(notif, raw)
        val estimate = defaultEstimatedHeightForMode(notif?.mode)
        val held = notif?.let { lastMeasuredDp[it.key] }
        return if (notif == null) estimate else clampHeightForMode(notif, maxOf(estimate, (held ?: 0f).dp))
    }

    val currentPageHeight = heightOrHold(currentNotification)

    val naturalTargetHeight = run {
        val nextPage = if (offsetFraction > 0f) {
            (currentPage + 1).coerceAtMost(pageCards.lastIndex)
        } else if (offsetFraction < 0f) {
            (currentPage - 1).coerceAtLeast(0)
        } else {
            currentPage
        }
        val nextNotification = cardAt(nextPage)
        val nextHeight = heightOrHold(nextNotification)
        val fraction = abs(offsetFraction)
        // Each card is already held to its own range (clampCardHeightForMode), this only keeps the blend between 2
        // of them sane. The ceiling is the tallest card, the torch: with 260 dp the shell cut it off short
        val blended = (currentPageHeight + (nextHeight - currentPageHeight) * fraction)
            .coerceIn(72.dp, PAGER_TARGET_MAX_HEIGHT)
        val bothMeasured = currentNotification?.let { pageHeights[it.key] } != null &&
            nextNotification?.let { pageHeights[it.key] } != null
        if (bothMeasured && fraction == 0f && currentNotification != null) {
            lastMeasuredDp[currentNotification.key] = blended.value
        }
        blended
    }

    return PagerHeight(currentPage, currentNotification, naturalTargetHeight)
}

@Composable
internal fun CardPager(
    pager: ExpandedPager,
    notifications: List<IslandNotification>,
    settings: SmartIslandSettings,
    drawnCardKey: Array<String?>?,
    cache: CardHeightCache,
    bottomPadding: Dp,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
) {
    val pagerState = pager.state
    fun cardAt(page: Int): IslandNotification? = pager.cardAt(page)
    var pageHeights by cache.heights
    // a left / right swipe action replaces the pager's own card-to-card swiping
    val horizontalSwipeActionsActive = settings.enableSwipeActions &&
        (SwipeAction.fromId(settings.swipeLeftAction) != SwipeAction.None ||
            SwipeAction.fromId(settings.swipeRightAction) != SwipeAction.None)
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = !horizontalSwipeActionsActive && LocalCardSteps.current
            .stepsFor(notifications.getOrNull(pagerState.currentPage)?.key).depth ==
            CardDepth.First,
        modifier = Modifier
            .fillMaxWidth()
            // unbounded: pages measure at natural height even with an explicit parent height
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
    ) { page ->
        val notification = cardAt(page)
        if (notification != null) {
            ExpandedCardPage(
                modifier = if (drawnCardKey == null) Modifier else Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        drawnCardKey[0] = notification.key
                        placeable.place(0, 0)
                    }
                },
                notification = notification,
                bottomPadding = bottomPadding,
                settings = settings,
                launcherApps = launcherApps,
                onOpenNotification = onOpenNotification,
                onLaunchApp = onLaunchApp,
                onAppAction = onAppAction,
                onCollapse = onCollapse,
                onReplyStateChanged = onReplyStateChanged,
                onTextExpandedChanged = onTextExpandedChanged,
                onHeightChanged = { heightDp ->
                    if (pageHeights[notification.key] != heightDp) {
                        pageHeights = pageHeights.toMutableMap().apply { put(notification.key, heightDp) }
                    }
                }
            )
        }
    }
}

@Composable
internal fun NeighbourProbes(
    pager: ExpandedPager,
    cache: CardHeightCache,
    stepDrag: StepDragState,
    siblingPickerOpen: Boolean,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?
) {
    val pagerState = pager.state
    val pageCards = pager.pageCards
    fun cardAt(page: Int): IslandNotification? = pager.cardAt(page)
    var pageHeights by cache.heights
    // cards next to the shown one are measured while the island is idle
    val cardShown = LocalCardShown.current
    var probeReady by remember { mutableStateOf(false) }
    LaunchedEffect(cardShown) {
        probeReady = false
        // Collapsed (pre-composed): measure at once, nobody is waiting for an opening
        if (cardShown) delay(NEIGHBOUR_PROBE_DELAY_MS)
        probeReady = true
    }
    // a left / right swipe that may bring a card is being dragged
    val swipeHint = stepDrag.swipeNeighbour
    if ((probeReady || (cardShown && swipeHint != 0)) && !siblingPickerOpen) {
        // open: the 2 cards next to the shown one
        val toMeasure = (if (cardShown) {
            neighbourProbeOrder(swipeHint).mapNotNull { cardAt(pagerState.currentPage + it) }
        } else {
            pageCards.indices.filter { it != pagerState.currentPage }.mapNotNull { cardAt(it) }
        }).firstOrNull { pageHeights[it.key] == null }
        if (toMeasure != null) {
            key(toMeasure.key) {
                NeighbourHeightProbe(
                    notification = toMeasure,
                    bottomPadding = bottomPadding,
                    settings = settings,
                    launcherApps = launcherApps,
                    onHeight = { heightDp ->
                        if (pageHeights[toMeasure.key] != heightDp) {
                            pageHeights = pageHeights.toMutableMap().apply { put(toMeasure.key, heightDp) }
                        }
                    }
                )
            }
        }
    }
}

/** 1 expanded card (backdrop plus the mode's own content). */
@Composable
internal fun ExpandedCardPage(
    modifier: Modifier = Modifier,
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit = { _, _ -> },
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    onHeightChanged: (Dp) -> Unit,
    // the landscape split dips only the card's foreground
    foregroundAlpha: (() -> Float)? = null,
    backdropWidthPx: (() -> Int)? = null
) {
    val density = LocalDensity.current
    val hasPicture = notificationHasPictureBackdrop(notification, settings)
    CompositionLocalProvider(
        LocalIslandPalette provides if (hasPicture) IslandPalette.Dark else LocalIslandPalette.current
    ) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
            .onSizeChanged { size -> onHeightChanged(with(density) { size.height.toDp() }) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onOpenNotification(notification)
            }
    ) {
        if (settings.enableNotificationBackdrop && notification.mode != IslandMode.Music) {
            NotificationBackdrop(
                notification = notification,
                settings = settings,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (backdropWidthPx != null) Modifier.widenedBackdrop(backdropWidthPx) else Modifier)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // The music card draws its own artwork backdrop inside
                    if (foregroundAlpha != null && notification.mode != IslandMode.Music) {
                        Modifier.graphicsLayer { alpha = foregroundAlpha().coerceIn(0f, 1f) }
                    } else Modifier
                )
        ) {
            ModeCardContent(
                notification = notification,
                bottomPadding = bottomPadding,
                settings = settings,
                launcherApps = launcherApps,
                onOpenNotification = onOpenNotification,
                onLaunchApp = onLaunchApp,
                onAppAction = onAppAction,
                onCollapse = onCollapse,
                onReplyStateChanged = onReplyStateChanged,
                onTextExpandedChanged = onTextExpandedChanged,
                foregroundAlpha = foregroundAlpha
            )
        }
    }
    }
}

private const val NEIGHBOUR_PROBE_DELAY_MS = 350L

@Composable
private fun NeighbourHeightProbe(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?,
    onHeight: (Dp) -> Unit
) {
    // throw-away copies of the state a card shares with the island.
    val isolatedReaderScroll = remember { ReaderScrollTracker() }
    val isolatedReplyDrafts = remember { ReplyDrafts() }
    SubcomposeLayout { constraints ->
        if (constraints.maxWidth == Constraints.Infinity) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val fixedWidth = Constraints(
            minWidth = constraints.maxWidth,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )
        subcompose(Unit) {
            CompositionLocalProvider(
                LocalCardShown provides false,
                LocalReaderScroll provides isolatedReaderScroll,
                LocalReplyDrafts provides isolatedReplyDrafts
            ) {
                ExpandedCardPage(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    settings = settings,
                    launcherApps = launcherApps,
                    onOpenNotification = {},
                    onLaunchApp = {},
                    onCollapse = {},
                    onReplyStateChanged = {},
                    onTextExpandedChanged = {},
                    onHeightChanged = onHeight
                )
            }
        }.forEach { it.measure(fixedWidth) }
        layout(0, 0) {}
    }
}
