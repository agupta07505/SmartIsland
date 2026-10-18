/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.appShortcutMenu
import com.agupta07505.smartisland.model.floatingWindowRoute
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.agupta07505.smartisland.ui.IslandText as Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.ui.LocalCardShown
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import com.agupta07505.smartisland.model.LauncherPage
import com.agupta07505.smartisland.model.LauncherSwipe
import com.agupta07505.smartisland.model.launcherPageAfter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.agupta07505.smartisland.data.PinnedApps
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.util.GestureSensitivity
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.stringResource
import com.agupta07505.smartisland.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Card heights kept by the host so they outlive the card's own composition (the host rebuilds the card each time the island closes). */
class CardHeightCache {
    val heights = androidx.compose.runtime.mutableStateOf(emptyMap<String, Dp>())
    /** Content signature of each card at measure time; a changed card is measured again. */
    val signatures = mutableMapOf<String, Int>()
}

/** Pager bookkeeping between compositions: written by effects, never read to draw. */
internal class PagerSyncState {
    var committedGroups: List<String>? = null

    var draggedByFinger = false

    var shapeChangedDuringDrag = false
}

@Composable
fun IslandExpandedContent(
    notifications: List<IslandNotification>,
    launcherApps: List<LaunchableApp>?,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, com.agupta07505.smartisland.model.AppShortcutAction) -> Unit = { _, _ -> },
    onCollapse: () -> Unit,
    statusBarHeight: Dp,
    onHeightMeasured: (Dp) -> Unit,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onTextExpandedChanged: (Boolean) -> Unit = {},
    siblingPickerOpen: Boolean = false,
    onSiblingSelected: (String) -> Unit = {},
    onSiblingPickerClose: () -> Unit = {},
    sideBySidePicker: Boolean = false,
    pickerOnLeft: Boolean = false,
    // The measured card width is reported back so the overlay can size its window to card + gap + picker.
    splitCardMaxWidth: Dp = 560.dp,
    splitPickerWidth: Dp = 320.dp,
    singleIslandWidth: Dp = 420.dp,
    onSplitCardWidthMeasured: (Dp) -> Unit = {},
    // True while a swipe swaps the card itself: the pager jumps to the page.
    snapPageSync: () -> Boolean = { false },
    sideBarsAlpha: () -> Float = { 1f },
    // Written during layout: the key of the card the pager draws this frame (the host parks a swipe's outgoing card by it).
    drawnCardKey: Array<String?>? = null,
    heightCache: CardHeightCache? = null
) {
    if (notifications.isEmpty()) {
        if (!settings.enableAppShortcuts) {
            return
        }
        val density = LocalDensity.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .onSizeChanged {
                    val measuredHeight = with(density) { it.height.toDp() }
                    if (measuredHeight > 0.dp) {
                        val clamped = measuredHeight.coerceIn(80.dp, 300.dp)
                        onHeightMeasured(clamped)
                    }
                }
        ) {
            if (settings.enableNotificationBackdrop) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(settings.notificationDotColor).copy(alpha = 0.22f),
                                    Color(settings.notificationDotColor).copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, 0f),
                                radius = 700f
                            )
                        )
                )
            }
            EmptyExpanded(settings = settings, apps = launcherApps, onLaunchApp = onLaunchApp, onAppAction = onAppAction)
        }
        return
    }

    val density = LocalDensity.current
    val ownHeightCache = remember { CardHeightCache() }
    val cardHeightCache = heightCache ?: ownHeightCache
    var pageHeights by cardHeightCache.heights
    val mediaExpansion = LocalMediaExpansion.current
    // Landscape draws the picture as its own island, so the card keeps its normal height there.
    val mediaOpenKey = if (sideBySidePicker) null else mediaExpansion.key
    val cardStepsAccess = LocalCardSteps.current

    val activeKeys = remember(notifications) { notifications.map { it.key }.toSet() }
    LaunchedEffect(activeKeys) {
        pageHeights = pageHeights.filterKeys { it in activeKeys }
    }

    // One page per app; siblings are reached through the sibling picker and gestures.
    val pageCards = com.agupta07505.smartisland.model.appGroupPageIndices(notifications, selectedIndex)
    val selectedPage = com.agupta07505.smartisland.model.appGroupPageOf(notifications, selectedIndex)
        .coerceIn(0, (pageCards.size - 1).coerceAtLeast(0))
    fun cardAt(page: Int): IslandNotification? =
        pageCards.getOrNull(page)?.let { notifications.getOrNull(it) }

    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { pageCards.size }
    )

    // A measured height is forgotten when the card's content changes, so the probe measures it again.
    val heightSignatures = cardHeightCache.signatures
    LaunchedEffect(notifications) {
        val shownKey = notifications.getOrNull(selectedIndex)?.key
        val stale = ArrayList<String>()
        notifications.forEach { n ->
            val signature = notificationHeightSignature(n)
            val before = heightSignatures.put(n.key, signature)
            if (before != null && before != signature && n.key != shownKey) stale += n.key
        }
        heightSignatures.keys.retainAll(notifications.mapTo(HashSet()) { it.key })
        if (stale.isNotEmpty()) pageHeights = pageHeights.filterKeys { it !in stale }
    }

    val pageGroups = com.agupta07505.smartisland.model.PagerSelection.pageGroups(notifications, selectedIndex)
    val pagerSync = remember { PagerSyncState() }
    val shapeChanged = com.agupta07505.smartisland.model.PagerSelection.shapeChanged(pagerSync.committedGroups, pageGroups)
    androidx.compose.runtime.SideEffect {
        if (shapeChanged) {
            if (pagerState.isScrollInProgress && pagerSync.draggedByFinger) {
                // A finger is on the pager: do not pull it away; the drag's result is dropped when it ends (below).
                pagerSync.shapeChangedDuringDrag = true
            } else {
                val target = com.agupta07505.smartisland.model.PagerSelection.jumpTarget(pagerState.currentPage, selectedPage, pageCards.size)
                if (target != null) pagerState.requestScrollToPage(target)
            }
        }
        pagerSync.committedGroups = pageGroups
    }
    val onPageSelectedNow = androidx.compose.runtime.rememberUpdatedState(onPageSelected)
    val pageCardsNow = androidx.compose.runtime.rememberUpdatedState(pageCards)
    val notificationsNow = androidx.compose.runtime.rememberUpdatedState(notifications)
    val selectedIndexNow = androidx.compose.runtime.rememberUpdatedState(selectedIndex)
    val selectedPageNow = androidx.compose.runtime.rememberUpdatedState(selectedPage)
    val pageGroupsNow = androidx.compose.runtime.rememberUpdatedState(pageGroups)

    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) pagerSync.draggedByFinger = true
        }
    }

    // Restarts when the pages change shape, which cancels a running scroll aimed at the old page numbers.
    LaunchedEffect(selectedPage, pageCards.size, pageGroups, siblingPickerOpen) {
        if (shapeChanged) return@LaunchedEffect
        // Sync external selectedIndex updates only when not user-scrolling, so a gesture is never interrupted mid-
        // swipe.
        if (!siblingPickerOpen &&
            selectedPage in pageCards.indices &&
            !pagerState.isScrollInProgress &&
            pagerState.currentPage != selectedPage
        ) {
            if (snapPageSync()) pagerState.scrollToPage(selectedPage) else pagerState.animateScrollToPage(selectedPage)
        }
    }

    // A page a finger dragged to becomes the open card; any other scroll writes nothing back.
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.isScrollInProgress }.collect { moving ->
            if (moving) return@collect
            val byFinger = pagerSync.draggedByFinger
            val shapeChangedDuringDrag = pagerSync.shapeChangedDuringDrag
            pagerSync.draggedByFinger = false
            pagerSync.shapeChangedDuringDrag = false
            val cards = pageCardsNow.value
            val settled = pagerState.settledPage
            if (byFinger && !com.agupta07505.smartisland.model.PagerSelection.dragStillValid(shapeChangedDuringDrag)) {
                // The pages moved under the finger: show the shown card's page again and write nothing.
                com.agupta07505.smartisland.util.AppLogRecorder.i(
                    "IslandPager",
                    "Pager drag dropped: the cards changed during the drag (settledPage=$settled, selectedPage=${selectedPageNow.value})"
                )
                com.agupta07505.smartisland.model.PagerSelection.jumpTarget(settled, selectedPageNow.value, cards.size)
                    ?.let { pagerState.requestScrollToPage(it) }
                return@collect
            }
            if (com.agupta07505.smartisland.model.PagerSelection.mayWriteBack(byFinger, settled, cards.size)) {
                val newIndex = cards[settled]
                val list = notificationsNow.value
                val oldKey = list.getOrNull(selectedIndexNow.value)?.key
                val newKey = list.getOrNull(newIndex)?.key
                if (oldKey != newKey) {
                    com.agupta07505.smartisland.util.AppLogRecorder.i(
                        "IslandPager",
                        com.agupta07505.smartisland.model.PagerSelection.selectionLogLine(
                            oldKey, newKey, settled, cards.size, selectedPageNow.value, pageGroupsNow.value
                        )
                    )
                }
                onPageSelectedNow.value(newIndex)
            }
        }
    }

    // Never leave the pager stuck at a non-zero offset fraction when a scroll finishes.
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction != 0f) {
            val targetPage = pagerState.settledPage.coerceIn(0, (pageCards.size - 1).coerceAtLeast(0))
            if (targetPage in pageCards.indices) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val bottomPadding = 16.dp

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {

        // Interpolate height between pages by swipe progress; battery/call heights are clamped to match the other cards.
        val offsetFraction = pagerState.currentPageOffsetFraction
        // A swipe action selects the new card and the pager follows a few frames later, so while the pager is idle the height
        // follows the selection at once; while it moves it interpolates between the two pages.
        val pagerIdle = !pagerState.isScrollInProgress && offsetFraction == 0f
        val currentPage = if (pagerIdle && !siblingPickerOpen && selectedPage in pageCards.indices) selectedPage else pagerState.currentPage
        val currentNotification = cardAt(currentPage)

        fun clampHeightForMode(notif: IslandNotification?, height: Dp): Dp {
            if (notif != null && notif.mode == IslandMode.Notification) {
                val shownSteps = cardStepsAccess.stepsFor(notif.key)
                return when {
                    notif.key == mediaOpenKey -> if (shownSteps.pictureHalf) PICTURE_MID_HEIGHT else EXPANDED_MEDIA_HEIGHT
                    !sideBySidePicker && shownSteps.depth == com.agupta07505.smartisland.model.CardDepth.Selector -> PICTURE_MID_HEIGHT
                    else -> clampCardHeightForMode(notif.mode, height)
                }
            }
            return clampCardHeightForMode(notif?.mode, height)
        }

        val currentPageHeightRaw = currentNotification?.let { pageHeights[it.key] }
        val currentPageHeight = currentPageHeightRaw?.let { clampHeightForMode(currentNotification, it) }
            ?: com.agupta07505.smartisland.ui.defaultEstimatedHeightForMode(currentNotification?.mode)

        val naturalTargetHeight = run {
            val nextPage = if (offsetFraction > 0f) {
                (currentPage + 1).coerceAtMost(pageCards.lastIndex)
            } else if (offsetFraction < 0f) {
                (currentPage - 1).coerceAtLeast(0)
            } else {
                currentPage
            }
            val nextNotification = cardAt(nextPage)
            val nextHeightRaw = nextNotification?.let { pageHeights[it.key] }
            val nextHeight = (nextHeightRaw?.let { clampHeightForMode(nextNotification, it) }
                ?: com.agupta07505.smartisland.ui.defaultEstimatedHeightForMode(nextNotification?.mode))
            val fraction = kotlin.math.abs(offsetFraction)
            (currentPageHeight + (nextHeight - currentPageHeight) * fraction).coerceIn(72.dp, 260.dp)
        }
        // The gesture supplies the dragged step's height (the shell reads it directly), so nothing here recomposes per frame.
        val stepDrag = LocalStepDrag.current
        val targetHeight = naturalTargetHeight

        val pickerSiblings = if (siblingPickerOpen) {
            com.agupta07505.smartisland.model.siblingIndices(notifications, pageCards.getOrNull(currentPage) ?: selectedIndex)
                .map { notifications[it] }
        } else emptyList()
        val pickerShown = pickerSiblings.isNotEmpty()
        val siblingsHold = remember { mutableListOf<IslandNotification>() }
        if (pickerSiblings.isNotEmpty()) {
            siblingsHold.clear()
            siblingsHold.addAll(pickerSiblings)
        }
        // Rows the picker island keeps while it shrinks away (the open flag is already false).
        val splitPickerSiblings = if (pickerShown) pickerSiblings else siblingsHold.toList()
        val pictureBeside = pictureShownBesideCard(
            isLandscape = sideBySidePicker,
            openKey = mediaExpansion.key,
            openIndex = mediaExpansion.index,
            cardKey = currentNotification?.key,
            cardMediaCount = currentNotification?.media?.size ?: 0,
            selectorOpen = mediaExpansion.selectorOpen
        )
        val besideSteps = cardStepsAccess.stepsFor(currentNotification?.key)
        val sideBySideWanted = pictureBeside || (pickerShown && sideBySidePicker)
        // ONE progress for the two islands (0 = card alone, 1 = card and picker), on the same curve and lengths as the portrait swap.
        // A close runs it back down while the overlay holds its window and surface.
        val splitProgress = remember { androidx.compose.animation.core.Animatable(0f) }
        val splitSpeed = settings.animationSpeed(sideBySidePicker)
        val splitWasPicker = remember { booleanArrayOf(false) }
        // The flag keeps the two-island layout while the island shrinks away, and remembers which island it was.
        val splitIslandIsPicture = remember { booleanArrayOf(false) }
        val besideHold = remember { arrayOfNulls<com.agupta07505.smartisland.model.CardSteps>(1) }
        if (sideBySideWanted) {
            splitWasPicker[0] = true
            splitIslandIsPicture[0] = pictureBeside
            if (pictureBeside) besideHold[0] = besideSteps
        }
        // A finger drag that opens or closes the picture island runs the split progress with the finger; the lift hands over
        // to the tween from wherever the progress is.
        val dragOwnsSplit = sideBySidePicker && stepDrag.active && !stepDrag.settled &&
            stepDrag.cardKey == currentNotification?.key &&
            (stepDrag.fromSteps?.let { stepsOpenBesideCard(it) } ?: false) != pictureBeside
        LaunchedEffect(sideBySideWanted, dragOwnsSplit) {
            if (dragOwnsSplit) {
                val opening = pictureBeside
                androidx.compose.runtime.snapshotFlow { stepDrag.progress }.collect { p ->
                    splitProgress.snapTo(com.agupta07505.smartisland.model.SplitMotion.fromGeometry(besideDragSplitProgress(opening, p)))
                }
            } else {
                // A spring without bounce: a turn-around keeps its speed. The overlay waits for [StepDragState.splitShown] to go false.
                splitProgress.animateTo(
                    targetValue = if (sideBySideWanted) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = splitSpeed.stiffness(
                            if (sideBySideWanted) com.agupta07505.smartisland.model.PickerSwap.SPLIT_OPEN_STIFFNESS
                            else com.agupta07505.smartisland.model.PickerSwap.SPLIT_CLOSE_STIFFNESS
                        )
                    )
                )
            }
        }
        // Only the derived flag is read so the content does not recompose per frame. The two-island layout is kept until the
        // shell confirms the single island (model/SplitMotion.holdTwoIslandLayout).
        val splitHolding by remember(sideBySideWanted) {
            androidx.compose.runtime.derivedStateOf {
                com.agupta07505.smartisland.model.SplitMotion.holdTwoIslandLayout(
                    splitWasPicker[0], sideBySideWanted, splitProgress.value, stepDrag.shellSingleReady
                )
            }
        }
        // The progress is gone: the shell may switch now.
        val splitGone by remember(sideBySideWanted) {
            androidx.compose.runtime.derivedStateOf {
                splitWasPicker[0] && !sideBySideWanted && com.agupta07505.smartisland.model.SplitMotion.phase(
                    sideBySideWanted, splitProgress.value
                ) == com.agupta07505.smartisland.model.SplitPhase.Closed
            }
        }
        LaunchedEffect(sideBySideWanted) { if (sideBySideWanted) stepDrag.shellSingleReady = false }
        // Safety: if the shell never confirms (collapsed meanwhile), flip anyway after a short cap.
        LaunchedEffect(splitGone) {
            if (splitGone) {
                kotlinx.coroutines.delay(com.agupta07505.smartisland.model.SplitMotion.HANDOVER_CAP_MS)
                stepDrag.shellSingleReady = true
            }
        }
        val sideBySide = sideBySideWanted || splitHolding
        val splitGoneNow = splitGone
        // Tell the overlay when the two islands are really gone (it keeps its window and surfaces until then).
        androidx.compose.runtime.SideEffect {
            stepDrag.splitShown = sideBySide && !splitGoneNow
            // The shell takes its width from the same progress while the two islands are drawn (model/SplitShell.kt).
            stepDrag.splitProgressNow = if (sideBySide && splitWasPicker[0]) ({ splitProgress.value }) else null
        }
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose {
                stepDrag.splitShown = false
                stepDrag.splitProgressNow = null
            }
        }
        val heldBesideFull = besideHold[0]?.depth == com.agupta07505.smartisland.model.CardDepth.Picture
        val shownHeight = when {
            pictureBeside -> pictureIslandStepHeight(fullSize = besideSteps.depth == com.agupta07505.smartisland.model.CardDepth.Picture)
            sideBySide && splitIslandIsPicture[0] -> pictureIslandStepHeight(fullSize = heldBesideFull)
            sideBySide -> maxOf(targetHeight, siblingPickerHeight(splitPickerSiblings.size, compact = true)).coerceAtMost(260.dp)
            pickerShown -> siblingPickerHeight(pickerSiblings.size)
            else -> targetHeight
        }

        androidx.compose.runtime.SideEffect {
            if (stepDrag.active && !stepDrag.settled && kotlin.math.abs(stepDrag.targetDp - shownHeight.value) > 1f) {
                android.util.Log.d(
                    "StepHeight",
                    "target ${stepDrag.targetDp} -> ${shownHeight.value} from=${stepDrag.fromDp} progress=${stepDrag.progress} shell=${stepDrag.shellHeightDp}"
                )
            }
            stepDrag.targetDp = shownHeight.value
            stepDrag.splitSingleHeightDp = targetHeight.value
            stepDrag.splitTotalHeightDp = shownHeight.value
        }

        LaunchedEffect(shownHeight) {
            onHeightMeasured(shownHeight)
        }

        // Portrait / single island: card and picker trade places by size on ONE progress (0 = card, 1 = picker;
        // model/PickerSwap.kt).
        val portraitPickerOpen = pickerShown && !sideBySide
        val swap = remember { androidx.compose.animation.core.Animatable(0f) }
        val swapSpeed = settings.animationSpeed(sideBySidePicker)
        LaunchedEffect(portraitPickerOpen) {
            swap.animateTo(
                targetValue = if (portraitPickerOpen) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = swapSpeed.durationMs(com.agupta07505.smartisland.model.PickerSwap.lengthMs(portraitPickerOpen)),
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
        val cardLayerShown by remember(portraitPickerOpen) {
            androidx.compose.runtime.derivedStateOf {
                com.agupta07505.smartisland.model.PickerSwap.cardLayerShown(swap.value, portraitPickerOpen)
            }
        }
        val pickerLayerShown by remember(portraitPickerOpen) {
            androidx.compose.runtime.derivedStateOf {
                com.agupta07505.smartisland.model.PickerSwap.pickerLayerShown(swap.value, portraitPickerOpen)
            }
        }

        // Landscape with the picker open: two islands with a gap. The pager is not composed; each island draws its own surface
        // because the overlay switches the single surface off.
        val splitCard = if (sideBySide) currentNotification else null
        if (splitCard != null) {
            // ONE progress for both islands: the picker is [splitPickerWidth] x progress wide and the active island
            // gives up the same width.
            val splitRaw: () -> Float = { if (splitWasPicker[0]) splitProgress.value else 1f }
            val splitEase: () -> Float = { com.agupta07505.smartisland.model.SplitMotion.geometry(splitRaw()) }
            val naturalReported = remember { intArrayOf(-1) }
            // The card's natural width, measured once per card rather than per frame. [0] = width (px), [1] = height it was measured at.
            val naturalCache = remember(splitCard) { intArrayOf(-1, -1) }
            // [0] = 1 while the card's content fades through its one re-wrap (set in layout, read when drawing).
            val reflowFade = remember { intArrayOf(0) }
            // Widest the card island gets during the swap (px); the backdrop uses it to fill the island while the content is narrower.
            val islandWidthPx = remember { intArrayOf(0) }
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shownHeight)
            ) {
            val rowWidthPx = constraints.maxWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shownHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                val gapSpacer: @Composable () -> Unit = {
                    Spacer(
                        Modifier.layout { measurable, _ ->
                            val w = (SPLIT_ISLAND_GAP.roundToPx() * splitEase()).roundToInt()
                            val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, 0))
                            layout(w, 0) { placeable.place(0, 0) }
                        }
                    )
                }
                val cardIsland: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        // No fixed height: as tall as the card at its drawn width.
                        .islandSurface(
                            settings, settings.forCard(splitCard.mode == IslandMode.Music).opacity,
                            shadowStrength = { com.agupta07505.smartisland.model.SplitMotion.shadowStrength(splitRaw()) },
                            palette = LocalIslandPalette.current
                        )
                        .layout { measurable, constraints ->
                            // The natural width (what the window is sized from) is measured every time; the drawn
                            // width runs from the single island's width to the natural width, capped by what the
                            // picker leaves of the window.
                            val minPx = SPLIT_CARD_MIN_WIDTH.roundToPx()
                            val maxPx = splitCardMaxWidth.roundToPx().coerceAtLeast(minPx)
                            val capPx = constraints.maxHeight
                            val h = targetHeight.roundToPx().coerceAtMost(capPx)
                            if (naturalCache[0] < 0 || naturalCache[1] != h) {
                                naturalCache[0] = measurable.maxIntrinsicWidth(h)
                                naturalCache[1] = h
                            }
                            val natural = naturalCache[0].coerceIn(minPx, maxPx)
                            val raw = splitRaw()
                            val p = com.agupta07505.smartisland.model.SplitMotion.geometry(raw)
                            val singlePx = singleIslandWidth.roundToPx()
                            // The window is sized from this slot.
                            val swapping = splitWasPicker[0] && com.agupta07505.smartisland.model.SplitMotion.swapping(
                                com.agupta07505.smartisland.model.SplitMotion.phase(sideBySideWanted, raw)
                            )
                            val slot = if (swapping) {
                                maxOf(natural, singlePx - SPLIT_ISLAND_GAP.roundToPx() - splitPickerWidth.roundToPx())
                            } else natural
                            if (naturalReported[0] != slot) {
                                naturalReported[0] = slot
                                onSplitCardWidthMeasured(slot.toDp())
                            }
                            val sidePx = (SPLIT_ISLAND_GAP.roundToPx() + splitPickerWidth.roundToPx()) * p
                            val w = (singlePx + (natural - singlePx) * p).roundToInt()
                                .coerceAtMost((rowWidthPx - sidePx).roundToInt())
                                .coerceAtLeast(1)
                            // The island's width runs every frame (it is only a clip), but the card is measured at
                            // one of two fixed widths so its text re-wraps once, not per frame.
                            val finalW = natural.coerceAtMost((rowWidthPx - (SPLIT_ISLAND_GAP.roundToPx() + splitPickerWidth.roundToPx())).coerceAtLeast(1))
                                .coerceAtLeast(1)
                            val wideW = maxOf(singlePx, finalW)
                            islandWidthPx[0] = wideW
                            val contentW = if (splitWasPicker[0]) {
                                if (com.agupta07505.smartisland.model.SplitMotion.reflowed(raw)) finalW else wideW
                            } else w
                            reflowFade[0] = if (splitWasPicker[0] && wideW != finalW) 1 else 0
                            // Measured at its natural height at this width: forcing the single-island height cuts off
                            // the last lines and buttons once the text re-wraps narrower.
                            val placeable = measurable.measure(
                                androidx.compose.ui.unit.Constraints(
                                    minWidth = contentW, maxWidth = contentW,
                                    minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity
                                )
                            )
                            val islandH = clampCardHeightForMode(splitCard.mode, placeable.height.toDp())
                                .roundToPx().coerceAtMost(capPx).coerceAtLeast(1)
                            layout(w, islandH) { placeable.place(0, 0) }
                        }
                ) {
                  // The re-wrap dip is on the card's foreground only, so the backdrop and scrim stay and the island
                  // is never a bare translucent slab.
                  androidx.compose.foundation.layout.Box {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalMediaExpansion provides MediaExpansion(
                            key = mediaExpansion.key,
                            index = mediaExpansion.index,
                            besideCard = pictureBeside,
                            onChange = mediaExpansion.onChange
                        )
                    ) {
                    ExpandedCardPage(
                        notification = splitCard,
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
                            if (pageHeights[splitCard.key] != heightDp) {
                                pageHeights = pageHeights.toMutableMap().apply { put(splitCard.key, heightDp) }
                            }
                        },
                        foregroundAlpha = {
                            if (reflowFade[0] == 1) com.agupta07505.smartisland.model.SplitMotion.reflowAlpha(splitRaw()) else 1f
                        },
                        // The widest the island is during the swap (a constant per card), so the backdrop is not
                        // measured again every frame.
                        backdropWidthPx = { islandWidthPx[0] }
                    )
                    }
                  }
                }
                }
                val pickerIsland: @Composable () -> Unit = {
                SiblingPicker(
                    siblings = splitPickerSiblings,
                    currentKey = splitCard.key,
                    // A tap while the island shrinks away belongs to nobody: the picker is already closing.
                    onSelect = { key -> if (sideBySideWanted) onSiblingSelected(key) },
                    onClose = { if (sideBySideWanted) onSiblingPickerClose() },
                    tapSlop = GestureSensitivity.fromId(settings.gestureSensitivity).scaled(PICKER_TAP_SLOP_DP).dp,
                    modifier = Modifier
                        // No surface fade while it shrinks away: a layer alpha turned the black island grey and see-
                        // through over a light page. The island is clipped by its shrinking width instead.
                        .islandSurface(
                            settings, maxOf(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY),
                            shadowStrength = { com.agupta07505.smartisland.model.SplitMotion.shadowStrength(splitRaw()) },
                            palette = LocalIslandPalette.current
                        )
                        .layout { measurable, constraints ->
                            // The list keeps its full width (no re-wrap) and is clipped by the island, anchored to the card's side.
                            val full = splitPickerWidth.roundToPx()
                            val w = (full * splitEase()).roundToInt().coerceIn(0, full)
                            val placeable = measurable.measure(
                                androidx.compose.ui.unit.Constraints(
                                    minWidth = full, maxWidth = full,
                                    minHeight = constraints.minHeight, maxHeight = constraints.maxHeight
                                )
                            )
                            // The island grows in height with its width, from the card's height, so a few pixels of
                            // width are never a full-height black sliver.
                            val pickerCardH = targetHeight.roundToPx().coerceIn(1, maxOf(1, placeable.height))
                            val islandH = (pickerCardH + (placeable.height - pickerCardH) * splitEase()).roundToInt()
                            layout(w, islandH) { placeable.place(if (pickerOnLeft) w - full else 0, 0) }
                        },
                    compact = true
                )
                }
                val pictureIsland: @Composable () -> Unit = {
                    // While it shrinks away (the step is already back) the island keeps the step it had.
                    val islandSteps = if (pictureBeside) besideSteps else (besideHold[0] ?: besideSteps)
                    val shown = splitCard.media.getOrNull(islandSteps.pictureIndex ?: mediaExpansion.index)
                    val selectorShown = islandSteps.depth == com.agupta07505.smartisland.model.CardDepth.Selector
                    // Like the sibling picker's island: content keeps its full width, clipped by the island and
                    // anchored to the card's side.
                    val islandModifier = Modifier
                        .layout { measurable, constraints ->
                            val naturalPx = shownHeight.roundToPx()
                            val hPx = if (stepDrag.active && stepDrag.heightDp >= 0f) {
                                (stepDrag.heightDp * density.density).roundToInt().coerceIn(1, 260.dp.roundToPx())
                            } else naturalPx
                            val placeable = measurable.measure(constraints.copy(minHeight = hPx, maxHeight = hPx))
                            layout(placeable.width, hPx) { placeable.place(0, 0) }
                        }
                        .islandSurface(
                            settings, maxOf(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY),
                            shadowStrength = { com.agupta07505.smartisland.model.SplitMotion.shadowStrength(splitRaw()) },
                            palette = LocalIslandPalette.current
                        )
                        .layout { measurable, constraints ->
                            val full = splitPickerWidth.roundToPx()
                            val w = (full * splitEase()).roundToInt().coerceIn(0, full)
                            val placeable = measurable.measure(
                                androidx.compose.ui.unit.Constraints(
                                    minWidth = full, maxWidth = full,
                                    minHeight = constraints.minHeight, maxHeight = constraints.maxHeight
                                )
                            )
                            // Grow in height with width, except while a finger drives the step (its height is 1:1 with the finger).
                            val sideCardH = targetHeight.roundToPx().coerceIn(1, maxOf(1, placeable.height))
                            val sideGrow = if (stepDrag.active) 1f else splitEase()
                            val islandH = (sideCardH + (placeable.height - sideCardH) * sideGrow).roundToInt()
                            layout(w, islandH) { placeable.place(if (pickerOnLeft) w - full else 0, 0) }
                        }
                    if (selectorShown) {
                        PictureSelector(
                            media = splitCard.media,
                            onPick = { index ->
                                cardStepsAccess.set(splitCard.key, com.agupta07505.smartisland.model.CardSteps.pickFromSelector(islandSteps, index))
                            },
                            modifier = islandModifier
                        )
                    } else if (shown != null) {
                        ExpandedMediaViewer(
                            media = shown,
                            onClose = { mediaExpansion.onChange(null, 0) },
                            modifier = islandModifier
                        )
                    }
                }
                val sideIsland = if (pictureBeside || (!pickerShown && splitIslandIsPicture[0])) pictureIsland else pickerIsland
                if (pickerOnLeft) {
                    sideIsland()
                    gapSpacer()
                    cardIsland()
                } else {
                    cardIsland()
                    gapSpacer()
                    sideIsland()
                }
            }
            }
        }

        // Single-island layout: the card layer, with the picker layer on top while the swap runs or the picker is open.
        if (!sideBySide) Box(modifier = Modifier.fillMaxWidth()) {
        if (cardLayerShown) Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(shownHeight)
                .graphicsLayer {
                    val p = swap.value
                    alpha = com.agupta07505.smartisland.model.PickerSwap.cardAlpha(p)
                    scaleX = com.agupta07505.smartisland.model.PickerSwap.cardScale(p)
                    scaleY = com.agupta07505.smartisland.model.PickerSwap.cardScale(p)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                },
            // Centred at rest; top-anchored while a card step follows the finger, so the card does not slide down inside the
            // finger-driven shell height and snap back (StepAwareVerticalAlignment).
            verticalAlignment = rememberStepAwareAlignment()
        ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(targetHeight)
        ) {
            // A left / right swipe action replaces the pager's own card-to-card swiping, so the
            // gesture reaches the island instead of being swallowed by the pager.
            val horizontalSwipeActionsActive = settings.enableSwipeActions &&
                (com.agupta07505.smartisland.model.SwipeAction.fromId(settings.swipeLeftAction) != com.agupta07505.smartisland.model.SwipeAction.None ||
                    com.agupta07505.smartisland.model.SwipeAction.fromId(settings.swipeRightAction) != com.agupta07505.smartisland.model.SwipeAction.None)
            // Light look only: while the island's height catches up with a shorter card, the pale shell shows as a
            // strip under the page's backdrop.
            val overscanPalette = LocalIslandPalette.current
            if (overscanPalette.isLight && currentNotification != null) {
                val overscanFromDp = pageHeights[currentNotification.key] ?: targetHeight
                val overscanHasPicture = notificationHasPictureBackdrop(currentNotification, settings)
                val overscanScrimScale = cardScrimScale(settings)
                val overscanColor = if (overscanHasPicture) {
                    Color.Black.copy(alpha = 0.85f * overscanScrimScale)
                } else {
                    overscanPalette.scrim.copy(alpha = 0.45f)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(
                                color = overscanColor,
                                topLeft = Offset(0f, overscanFromDp.toPx()),
                                size = androidx.compose.ui.geometry.Size(size.width, 300.dp.toPx())
                            )
                            // The same at the sides: while the island opens, the shell is briefly wider than the page
                            // and a pale sliver shows at both ends of a dark picture card.

                            val sideBars = sideBarsAlpha()
                            if (overscanHasPicture && sideBars > 0f) {
                                val pageHeightPx = overscanFromDp.toPx()
                                val sideWidthPx = 48.dp.toPx()
                                val dim = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f *
                                overscanScrimScale * sideBars),
                                        Color.Black.copy(alpha = 0.85f *
                                overscanScrimScale * sideBars)
                                    ),
                                    startY = 0f,
                                    endY = pageHeightPx
                                )
                                drawRect(
                                    brush = dim,
                                    topLeft = Offset(-sideWidthPx, 0f),
                                    size = androidx.compose.ui.geometry.Size(sideWidthPx, pageHeightPx)
                                )
                                drawRect(
                                    brush = dim,
                                    topLeft = Offset(size.width, 0f),
                                    size = androidx.compose.ui.geometry.Size(sideWidthPx, pageHeightPx)
                                )
                            }
                        }
                )
            }
            HorizontalPager(
                state = pagerState,
                // Preview, reader and an open picture also stop the sideways card swipe (only swipe up / down work there).
                userScrollEnabled = !horizontalSwipeActionsActive && LocalCardSteps.current
                    .stepsFor(notifications.getOrNull(pagerState.currentPage)?.key).depth ==
                    com.agupta07505.smartisland.model.CardDepth.First,
                modifier = Modifier
                    .fillMaxWidth()
                    // unbounded: pages measure at natural height even with an explicit parent height. Top-aligned, so a briefly shorter
                    // shell cuts only the page's bottom.
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
            // Cards next to the shown one are measured while the island is idle, so a swipe to one already knows its height.
            val cardShown = LocalCardShown.current
            var probeReady by remember { mutableStateOf(false) }
            LaunchedEffect(cardShown) {
                probeReady = false
                // Collapsed (pre-composed): measure at once, nobody is waiting for an opening. Open: wait, so the opening is never slowed.
                if (cardShown) kotlinx.coroutines.delay(NEIGHBOUR_PROBE_DELAY_MS)
                probeReady = true
            }
            // A left / right swipe that may bring a card is being dragged: measure the neighbours now, the likely target first.
            val swipeHint = stepDrag.swipeNeighbour
            if ((probeReady || (cardShown && swipeHint != 0)) && !siblingPickerOpen) {
                // Open: the two cards next to the shown one. Collapsed: every card of the stack, so whichever one is
                // swiped to already has its height.
                val toMeasure = (if (cardShown) {
                    com.agupta07505.smartisland.util.neighbourProbeOrder(swipeHint).mapNotNull { cardAt(pagerState.currentPage + it) }
                } else {
                    pageCards.indices.filter { it != pagerState.currentPage }.mapNotNull { cardAt(it) }
                }).firstOrNull { pageHeights[it.key] == null }
                if (toMeasure != null) {
                    androidx.compose.runtime.key(toMeasure.key) {
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
        }
        if (pickerLayerShown) {
            SiblingPicker(
                siblings = if (pickerShown) pickerSiblings else siblingsHold.toList(),
                currentKey = currentNotification?.key,
                // A tap during the fade-out belongs to nobody: the picker is already closing.
                onSelect = { key -> if (portraitPickerOpen) onSiblingSelected(key) },
                onClose = { if (portraitPickerOpen) onSiblingPickerClose() },
                tapSlop = GestureSensitivity.fromId(settings.gestureSensitivity).scaled(PICKER_TAP_SLOP_DP).dp,
                // The list is white text straight on the island surface; a see-through surface made it hard to read.
                modifier = Modifier.graphicsLayer {
                    val p = swap.value
                    alpha = com.agupta07505.smartisland.model.PickerSwap.pickerAlpha(p)
                    scaleX = com.agupta07505.smartisland.model.PickerSwap.pickerScale(p)
                    scaleY = com.agupta07505.smartisland.model.PickerSwap.pickerScale(p)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }.background(
                    expandedSurfaceColor(settings, LocalIslandPalette.current).copy(
                        alpha = siblingPickerBackingAlpha(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY)
                    )
                )
            )
        }
        }
    }
}

/**
 * The shell height range of a card by mode, for a card at its first step (the pager adds the step cases for a notification).
 */
internal fun clampCardHeightForMode(mode: IslandMode?, height: Dp): Dp = when (mode) {
    IslandMode.Battery, IslandMode.IncomingCall -> height.coerceIn(72.dp, 125.dp)
    IslandMode.Notification -> height.coerceIn(95.dp, 240.dp)
    IslandMode.Music -> height.coerceIn(115.dp, 180.dp)
    IslandMode.LiveActivity -> height.coerceIn(140.dp, 205.dp)
    IslandMode.Navigation -> height.coerceIn(NAVIGATION_EXPANDED_HEIGHT, NAVIGATION_EXPANDED_HEIGHT + 20.dp)
    IslandMode.DownloadUpload -> height.coerceIn(120.dp, 195.dp)
    IslandMode.Hotspot -> height.coerceIn(120.dp, 195.dp)
    IslandMode.Bluetooth -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Flashlight -> height.coerceIn(72.dp, 130.dp)
    IslandMode.ScreenRecording -> height.coerceIn(72.dp, 130.dp)
    IslandMode.AudioRecording -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Timer -> height.coerceIn(72.dp, 130.dp)
    IslandMode.Stopwatch -> height.coerceIn(72.dp, 130.dp)
    else -> height.coerceIn(80.dp, 160.dp)
}

/** The colour a card's backdrop is built on (its mode's colour from the settings). */
internal fun cardAccentColor(notification: IslandNotification, settings: SmartIslandSettings): Color =
    when (notification.mode) {
        IslandMode.Battery -> Color(settings.batteryColor)
        IslandMode.IncomingCall -> Color(settings.callColor)
        IslandMode.LiveActivity -> Color(settings.liveActivityColor)
        IslandMode.Navigation -> Color(settings.navigationColor)
        IslandMode.DownloadUpload -> Color(settings.transferColor)
        IslandMode.Hotspot -> Color(settings.hotspotColor)
        IslandMode.Bluetooth -> connectivityAccent(notification.category, settings.bluetoothColor)
        IslandMode.Flashlight -> Color(settings.flashlightColor)
        IslandMode.ScreenRecording, IslandMode.AudioRecording -> Color(settings.screenRecordingColor)
        IslandMode.Timer -> Color(settings.timerColor)
        IslandMode.Stopwatch -> Color(settings.stopwatchColor)
        else -> Color(settings.notificationDotColor)
    }

/** One expanded card (backdrop plus the mode's own content), shared by the pager and the split layout. */
@Composable
internal fun ExpandedCardPage(
    modifier: Modifier = Modifier,
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, com.agupta07505.smartisland.model.AppShortcutAction) -> Unit = { _, _ -> },
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    onHeightChanged: (Dp) -> Unit,
    // The landscape split dips only the card's foreground (text, buttons, pictures) through its re-wrap; the backdrop
    // and scrim stay at full strength so the island is never a bare translucent slab.
    foregroundAlpha: (() -> Float)? = null,
    // The landscape split measures the card at one of two fixed widths while its island is wider for part of the
    // swap.
    backdropWidthPx: (() -> Int)? = null
) {
    val density = LocalDensity.current
    // A card with a picture behind it (the sender's photo, an album cover) keeps the picture's own look: black dim
    // and white text on either theme.
    val hasPicture = notificationHasPictureBackdrop(notification, settings)
    androidx.compose.runtime.CompositionLocalProvider(
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
                    .then(
                        if (backdropWidthPx != null) {
                            Modifier.layout { measurable, constraints ->
                                // Reports the page's size, lays the backdrop out wider; the island's clip cuts it at
                                // its edge.
                                if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
                                    val plain = measurable.measure(constraints)
                                    layout(plain.width, plain.height) { plain.place(0, 0) }
                                } else {
                                    val target = maxOf(constraints.maxWidth, backdropWidthPx()).coerceAtLeast(1)
                                    val placeable = measurable.measure(
                                        androidx.compose.ui.unit.Constraints.fixed(target, constraints.maxHeight)
                                    )
                                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                                }
                            }
                        } else Modifier
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // The music card draws its own artwork backdrop inside, so it applies the alpha to its foreground itself.
                    if (foregroundAlpha != null && notification.mode != IslandMode.Music) {
                        Modifier.graphicsLayer { alpha = foregroundAlpha().coerceIn(0f, 1f) }
                    } else Modifier
                )
        ) {
            when (notification.mode) {
                IslandMode.Notification -> {
                    // While a step follows the finger, the step being left stays drawn on top and fades out
                    // as the new one fades in (both are the same card, one frozen at the old step).
                    val stepDrag = LocalStepDrag.current
                    val stepping = stepDrag.active && !stepDrag.settled && stepDrag.cardKey == notification.key
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = if (stepDrag.active && !stepDrag.settled && stepDrag.cardKey == notification.key) {
                                com.agupta07505.smartisland.model.StepDrag.newStepAlpha(stepDrag.progress)
                            } else 1f
                        }
                    ) {
                        NotificationExpanded(
                            notification = notification,
                            bottomPadding = bottomPadding,
                            onOpenNotification = { onOpenNotification(notification) },
                            onCollapse = onCollapse,
                            showActions = settings.showNotificationActions,
                            settings = settings,
                            onReplyStateChanged = onReplyStateChanged,
                            onTextExpandedChanged = onTextExpandedChanged
                        )
                    }
                    val oldSteps = stepDrag.fromSteps
                    if (stepping && oldSteps != null) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                // The old step is never taller than the shell (it is measured at its own size but
                                // laid out, and so clipped, at the shell's current height), and it is gone within the
                                // first part of the move, so two copies of the text are not drawn at two heights for
                                // long.
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val shellPx = if (stepDrag.heightDp >= 0f) (stepDrag.heightDp * density.density).roundToInt() else placeable.height
                                    val h = minOf(placeable.height, shellPx.coerceAtLeast(0))
                                    layout(placeable.width, h) { placeable.place(0, 0) }
                                }
                                .clipToBounds()
                                .graphicsLayer { alpha = com.agupta07505.smartisland.model.StepDrag.oldStepAlpha(stepDrag.progress) }
                        ) {
                            NotificationExpanded(
                                notification = notification,
                                bottomPadding = bottomPadding,
                                onOpenNotification = {},
                                onCollapse = {},
                                showActions = settings.showNotificationActions,
                                settings = settings,
                                frozen = true,
                                stepsOverride = oldSteps
                            )
                        }
                    }
                }
                IslandMode.IncomingCall -> IncomingCallExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Music -> MusicExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    settings = settings,
                    foregroundAlpha = foregroundAlpha
                )
                IslandMode.Battery -> BatteryExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    settings = settings
                )
                IslandMode.LiveActivity -> LiveActivityExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Navigation -> NavigationExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.DownloadUpload -> DownloadExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Hotspot -> HotspotExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Bluetooth -> BluetoothExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Flashlight -> FlashlightExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.ScreenRecording -> ScreenRecordingExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.AudioRecording -> AudioRecordingExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Timer -> TimerExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Stopwatch -> StopwatchExpanded(
                    notification = notification,
                    bottomPadding = bottomPadding,
                    onOpenNotification = { onOpenNotification(notification) },
                    onCollapse = onCollapse,
                    settings = settings
                )
                IslandMode.Empty -> EmptyExpanded(
                    settings = settings,
                    apps = launcherApps,
                    onLaunchApp = onLaunchApp,
                    onAppAction = onAppAction
                )
            }
        }
    }
    }
}

/**
 * True when the card draws a picture behind itself (a notification's large icon or icon as a blurred backdrop). The
 * music card has its own artwork rule and is handled inside it.
 */
internal fun notificationHasPictureBackdrop(notification: IslandNotification, settings: SmartIslandSettings): Boolean {
    if (!settings.enableNotificationBackdrop || notification.mode == IslandMode.Music) return false
    val artwork = notification.largeIcon ?: notification.icon
    return artwork != null && !artwork.isRecycled
}

/** Saves a new order for the pinned apps (set by the overlay service, which owns the settings repository). */
internal val LocalReorderPinnedApps = androidx.compose.runtime.staticCompositionLocalOf<(List<String>) -> Unit> { {} }

private fun launcherRows(count: Int): Int = ((count + 3) / 4).coerceIn(1, 2)

private val LAUNCHER_TILE_HEIGHT = 76.dp
private val LAUNCHER_ROW_GAP = 14.dp

private val LAUNCHER_PAGE_EXTRA = 8.dp
private val LAUNCHER_FOLDER_PANEL_HEIGHT = 108.dp

private const val FOLDER_DWELL_MS = 1500L

@Composable
private fun EmptyExpanded(
    settings: SmartIslandSettings,
    apps: List<LaunchableApp>?,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(unbounded = true)
            .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ClockBatteryHeader()
        Spacer(Modifier.height(10.dp))
        val loadedApps = apps
        if (loadedApps == null) {
            // Keep the configured launcher visually clean while PackageManager and
            // UsageStats are queried. In particular, do not flash the setup state.
            Spacer(Modifier.height(84.dp))
        } else {
            val pinnedOrder = settings.shortcutPackages
            // The pinned entries come first, then the recent apps (see AppShortcutProvider.shortcuts).
            val pinnedApps = loadedApps.filter { it.packageName in pinnedOrder }
            val recentApps = if (settings.showRecentApps) {
                loadedApps.filter { it.packageName !in pinnedOrder }.take(PinnedApps.MAX)
            } else {
                emptyList()
            }
            val utilityLayout = remember(settings.utilityLayout) { com.agupta07505.smartisland.model.UtilityLayout.decode(settings.utilityLayout) }
            // The Utilities page is only as tall as the rows in use. While the Calculator has no remembered app, its one-time
            // picker needs a little more room than a single row.
            val utilityRows = com.agupta07505.smartisland.model.UtilityLayout.usedRows(utilityLayout).let { used ->
                val needsPicker = settings.utilityCalculatorPackage.isBlank() &&
                    utilityLayout.any { it.type == com.agupta07505.smartisland.model.UtilityType.Calculator }
                if (needsPicker) maxOf(used, 3) else used
            }
            // One window height for every page, so swiping never resizes the island: tall enough for the fullest page.
            val rows = maxOf(launcherRows(pinnedApps.size), if (settings.showRecentApps) launcherRows(recentApps.size) else 1)
            // A little extra under every page: room for the page dots, which would be cut off at the window's bottom edge.
            val pageHeight = maxOf(
                LAUNCHER_TILE_HEIGHT * rows + LAUNCHER_ROW_GAP * (rows - 1),
                utilitiesPageHeight(utilityRows),
                if (pinnedOrder.any(PinnedApps::isFolder)) LAUNCHER_FOLDER_PANEL_HEIGHT else 0.dp
            ) + LAUNCHER_PAGE_EXTRA
            // The plus tile (pin more apps) takes the first empty slot, but never makes the window taller: it is only shown when the
            // grid with the plus still fits the height the window has anyway.
            val plusRows = launcherRows(pinnedApps.size + 1)
            val canAddPinned = pinnedOrder.size < PinnedApps.MAX &&
                LAUNCHER_TILE_HEIGHT * plusRows + LAUNCHER_ROW_GAP * (plusRows - 1) + LAUNCHER_PAGE_EXTRA <= pageHeight
            LauncherPager(
                recentsEnabled = settings.showRecentApps,
                pageHeight = pageHeight,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    LauncherPage.Apps ->
                        if (pinnedApps.isEmpty() && !(pinnedOrder.isEmpty() && canAddPinned)) {
                            val nothingPinned = pinnedOrder.isEmpty()
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                if (nothingPinned) {
                                    Text(stringResource(R.string.shortcuts_title), color = LocalIslandPalette.current.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        stringResource(R.string.shortcuts_desc),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        stringResource(R.string.quick_launch_open_settings),
                                        color = LocalIslandPalette.current.accentCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(top = 16.dp)
                                            .clickable { onLaunchApp(context.packageName) }
                                    )
                                } else {
                                    Text(
                                        stringResource(R.string.quick_launch_apps_unavailable),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            LauncherGrid(pinnedApps, settings, onLaunchApp, onAppAction, canAdd = canAddPinned)
                        }
                    LauncherPage.Recents ->
                        if (recentApps.isEmpty()) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                if (com.agupta07505.smartisland.data.AppShortcutProvider.hasUsageAccess(context)) {
                                    Text(
                                        stringResource(R.string.recent_apps_empty),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    // Recent apps come from usage access; without it the page can only say so and offer the setting.
                                    Text(
                                        stringResource(R.string.toggle_recent_apps_desc_needed),
                                        color = LocalIslandPalette.current.textTertiary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        stringResource(R.string.recent_apps_grant_access),
                                        color = LocalIslandPalette.current.accentCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .clickable {
                                                runCatching {
                                                    context.startActivity(
                                                        android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        } else {
                            LauncherGrid(recentApps, settings, onLaunchApp, onAppAction)
                        }
                    LauncherPage.Utilities -> UtilitiesContainer(settings)
                }
            }
        }
    }
}

@Composable
private fun LauncherPager(
    recentsEnabled: Boolean,
    pageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable (LauncherPage) -> Unit
) {
    var page by remember { mutableStateOf(LauncherPage.Apps) }
    var lastAppsLevelPage by remember { mutableStateOf(LauncherPage.Apps) }
    // The way the last swipe went, read by the transition so the new page comes in from the side the finger moved away from.
    var swipe by remember { mutableStateOf(LauncherSwipe.Up) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { LAUNCHER_SWIPE_THRESHOLD.toPx() }
    val shownPage = if (page == LauncherPage.Recents && !recentsEnabled) LauncherPage.Apps else page
    val onSwipe by androidx.compose.runtime.rememberUpdatedState<(LauncherSwipe) -> Unit> { direction ->
        val target = launcherPageAfter(shownPage, direction, recentsEnabled, lastAppsLevelPage)
        if (target != shownPage) {
            if (shownPage != LauncherPage.Utilities) lastAppsLevelPage = shownPage
            swipe = direction
            page = target
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .height(pageHeight)
            .clipToBounds()
            .launcherSwipes(thresholdPx) { onSwipe(it) }
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = shownPage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                // Icons scroll with the swipe: the old page leaves the way the finger went, the new one follows it in.
                val spec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                    durationMillis = LAUNCHER_SCROLL_MS,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
                val fade = androidx.compose.animation.core.tween<Float>(durationMillis = LAUNCHER_SCROLL_MS)
                val enter: androidx.compose.animation.EnterTransition
                val exit: androidx.compose.animation.ExitTransition
                when (swipe) {
                    LauncherSwipe.Up -> {
                        enter = androidx.compose.animation.slideInVertically(spec) { it } + androidx.compose.animation.fadeIn(fade)
                        exit = androidx.compose.animation.slideOutVertically(spec) { -it } + androidx.compose.animation.fadeOut(fade)
                    }
                    LauncherSwipe.Down -> {
                        enter = androidx.compose.animation.slideInVertically(spec) { -it } + androidx.compose.animation.fadeIn(fade)
                        exit = androidx.compose.animation.slideOutVertically(spec) { it } + androidx.compose.animation.fadeOut(fade)
                    }
                    LauncherSwipe.Left -> {
                        enter = androidx.compose.animation.slideInHorizontally(spec) { it } + androidx.compose.animation.fadeIn(fade)
                        exit = androidx.compose.animation.slideOutHorizontally(spec) { -it } + androidx.compose.animation.fadeOut(fade)
                    }
                    LauncherSwipe.Right -> {
                        enter = androidx.compose.animation.slideInHorizontally(spec) { -it } + androidx.compose.animation.fadeIn(fade)
                        exit = androidx.compose.animation.slideOutHorizontally(spec) { it } + androidx.compose.animation.fadeOut(fade)
                    }
                }
                enter togetherWith exit
            },
            label = "launcherPage"
        ) { target ->
            Box(Modifier.fillMaxSize()) { content(target) }
        }
        LauncherPageDots(shownPage, recentsEnabled)
        // Outside the swiping page, so it stays in the window's top-right corner while pages scroll.
        if (shownPage == LauncherPage.Utilities) UtilitiesSettingsButton()
    }
}

private val LAUNCHER_SWIPE_THRESHOLD = 40.dp
private const val LAUNCHER_SCROLL_MS = 280

@Composable
private fun BoxScope.LauncherPageDots(page: LauncherPage, recentsEnabled: Boolean) {
    val palette = LocalIslandPalette.current
    @Composable
    fun Dot(active: Boolean) = Box(
        Modifier
            .size(4.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(palette.text.copy(alpha = if (active) 0.7f else 0.25f))
    )
    Column(
        modifier = Modifier.align(Alignment.CenterEnd),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Dot(page != LauncherPage.Utilities)
        Dot(page == LauncherPage.Utilities)
    }
    if (recentsEnabled && page != LauncherPage.Utilities) {
        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Dot(page == LauncherPage.Apps)
            Dot(page == LauncherPage.Recents)
        }
    }
}

private fun Modifier.launcherSwipes(thresholdPx: Float, onSwipe: (LauncherSwipe) -> Unit): Modifier =
    pointerInput(thresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = Offset.Zero
            var fired = false
            var owned = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                if (change.isConsumed && !owned) break
                total += change.position - change.previousPosition
                if (!owned && total.getDistance() > viewConfiguration.touchSlop) owned = true
                if (owned) change.consume()
                if (!fired) {
                    val direction = com.agupta07505.smartisland.model.launcherSwipeFor(total.x, total.y, thresholdPx)
                    if (direction != null) {
                        fired = true
                        onSwipe(direction)
                    }
                }
            }
        }
    }

@Composable
private fun LauncherGrid(
    apps: List<LaunchableApp>,
    settings: SmartIslandSettings,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    // Pinned page with a free slot: a plus tile follows the apps and opens the in-window picker. Rows that are not full are centred.
    canAdd: Boolean = false
) {
    val context = LocalContext.current
    // The app whose long-press actions are showing. Reset whenever the list of apps changes underneath it.
    var menuApp by remember(apps) { mutableStateOf<LaunchableApp?>(null) }
    // Dragging the pinned apps. A long press opens the actions; dragging on from it (past the touch slop) swaps the
    // menu for a floating copy of the icon.
    val reorderPinned = LocalReorderPinnedApps.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val pinnedOrder = settings.shortcutPackages
    val slotBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    var gridOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var draggedApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var dragCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dropTarget by remember { mutableStateOf<String?>(null) }
    var dropIntoFolder by remember { mutableStateOf(false) }
    var openFolder by remember(apps) { mutableStateOf<String?>(null) }
    var folderPanelBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // The folder timer: it starts over whenever the dragged app moves onto another app, and arms the folder once it runs out.
    androidx.compose.runtime.LaunchedEffect(draggedApp?.packageName, dropTarget) {
        dropIntoFolder = false
        val dragged = draggedApp?.packageName ?: return@LaunchedEffect
        val target = dropTarget ?: return@LaunchedEffect
        if (!PinnedApps.canJoin(pinnedOrder, dragged, target)) return@LaunchedEffect
        kotlinx.coroutines.delay(FOLDER_DWELL_MS)
        dropIntoFolder = true
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }
    Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { gridOrigin = it.positionInRoot() }
            ) {
                Column(
                    // The pinned page (the one with the plus tile) sits in the middle of the window, so a short grid is not left at the top
                    // when another page (Utilities) makes the window taller.
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (canAdd) Modifier.align(Alignment.Center) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // A null entry is the plus tile.
                    val tiles: List<LaunchableApp?> = if (canAdd) apps + listOf<LaunchableApp?>(null) else apps
                    tiles.chunked(4).forEach { rowApps ->
                        val centred = canAdd && rowApps.size < 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (centred) Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally) else Arrangement.SpaceEvenly
                        ) {
                            rowApps.forEach { appOrNull ->
                                if (appOrNull == null) {
                                    AddAppTile(onClick = { com.agupta07505.smartisland.util.UtilityActions.openPicker(context, com.agupta07505.smartisland.util.PickerRequest.APPS) })
                                    return@forEach
                                }
                                val app: LaunchableApp = appOrNull
                                val pkg = app.packageName
                                val isFolder = PinnedApps.isFolder(pkg)
                                ShortcutApp(
                                    app = app,
                                    onClick = { if (isFolder) openFolder = pkg else onLaunchApp(pkg) },
                                    // A folder has no actions of its own; its apps have them once it is open.
                                    onLongClick = if (isFolder) null else ({ menuApp = app }),
                                    highlighted = dropTarget == pkg && !dropIntoFolder,
                                    joinHint = dropTarget == pkg && dropIntoFolder,
                                    dimmed = draggedApp?.packageName == pkg,
                                    modifier = Modifier.onGloballyPositioned { slotBounds[pkg] = it.boundsInRoot() },
                                    // Only pinned entries can be moved; the recent apps after them just keep the
                                    // long-press menu.
                                    gestureModifier =
                                            if (pkg !in pinnedOrder) Modifier else Modifier.longPressDrag(
                                                key = listOf(pkg, pinnedOrder),
                                                topLeft = { slotBounds[pkg]?.topLeft ?: androidx.compose.ui.geometry.Offset.Zero },
                                                onLift = {
                                                    menuApp = null
                                                    draggedApp = app
                                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                },
                                                onMove = { pointer ->
                                                    dragCenter = pointer
                                                    val target = pinnedOrder.firstOrNull { other ->
                                                        other != pkg && slotBounds[other]?.contains(pointer) == true
                                                    }
                                                    // Moving onto another app starts the folder timer over (the effect above).
                                                    if (target != dropTarget) dropIntoFolder = false
                                                    dropTarget = target
                                                },
                                                onEnd = { lifted, _ ->
                                                    val target = dropTarget
                                                    if (lifted && target != null) {
                                                        reorderPinned(
                                                            if (dropIntoFolder) PinnedApps.joinFolder(pinnedOrder, pkg, target)
                                                            else PinnedApps.swap(pinnedOrder, pkg, target)
                                                        )
                                                    }
                                                    draggedApp = null
                                                    dropTarget = null
                                                    dropIntoFolder = false
                                                },
                                                onCancel = {
                                                    draggedApp = null
                                                    dropTarget = null
                                                    dropIntoFolder = false
                                                }
                                            )
                                )
                            }
                            if (!centred) repeat(4 - rowApps.size) { Box(Modifier.size(width = 64.dp, height = 1.dp)) }
                        }
                    }
                }
                // The open folder: its apps on a panel over the grid. Tapping outside the apps closes it; dragging an app out of the
                // panel and letting go takes it out of the folder.
                openFolder?.takeIf { it in pinnedOrder }?.let { folder ->
                    val palette = LocalIslandPalette.current
                    val folderApps = remember(folder) {
                        PinnedApps.members(folder).mapNotNull { memberPkg ->
                            runCatching {
                                val pm = context.packageManager
                                LaunchableApp(memberPkg, pm.getApplicationLabel(pm.getApplicationInfo(memberPkg, 0)).toString())
                            }.getOrNull()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surface.copy(alpha = 0.97f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { openFolder = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .onGloballyPositioned { folderPanelBounds = it.boundsInRoot() }
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.text.copy(alpha = 0.08f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            folderApps.forEach { member ->
                                val memberPkg = member.packageName
                                ShortcutApp(
                                    app = member,
                                    onClick = { onLaunchApp(memberPkg) },
                                    onLongClick = { menuApp = member },
                                    dimmed = draggedApp?.packageName == memberPkg,
                                    modifier = Modifier.onGloballyPositioned { slotBounds[memberPkg] = it.boundsInRoot() },
                                    gestureModifier = Modifier
                                        .longPressDrag(
                                            key = listOf(memberPkg, folder, pinnedOrder, folderPanelBounds),
                                            topLeft = { slotBounds[memberPkg]?.topLeft ?: androidx.compose.ui.geometry.Offset.Zero },
                                            onLift = {
                                                menuApp = null
                                                draggedApp = member
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            },
                                            onMove = { pointer -> dragCenter = pointer },
                                            onEnd = { lifted, pointer ->
                                                draggedApp = null
                                                if (lifted && !folderPanelBounds.contains(pointer)) {
                                                    if (PinnedApps.dragOutUnpins(pinnedOrder)) {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            context.getString(R.string.pinned_folder_full_unpinned, member.label),
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    reorderPinned(PinnedApps.dragOut(pinnedOrder, folder, memberPkg))
                                                    openFolder = null
                                                }
                                            },
                                            onCancel = { draggedApp = null }
                                        )
                                )
                            }
                        }
                    }
                }
                // The icon being dragged, following the finger above the grid.
                draggedApp?.let { app ->
                    ShortcutApp(
                        app = app,
                        onClick = null,
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    (dragCenter.x - gridOrigin.x - 32.dp.toPx()).roundToInt(),
                                    (dragCenter.y - gridOrigin.y - 38.dp.toPx()).roundToInt()
                                )
                            }
                            .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                    )
                }
                // Drawn over the grid (not in a popup window: the island's own window is the only one this overlay owns), so
                // the island keeps its measured height and the touches stay inside its touchable region.
                menuApp?.let { app ->
                    AppActionPanel(
                        app = app,
                        actions = appShortcutMenu(floatingWindowRoute(settings.floatingWindowEnabled, settings.floatingWindowMode)),
                        onAction = { action ->
                            menuApp = null
                            onAppAction(app.packageName, action)
                        },
                        onDismiss = { menuApp = null },
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
}

@Composable
private fun AddAppTile(onClick: () -> Unit) {
    val palette = LocalIslandPalette.current
    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.text.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.pin_apps_add),
                tint = palette.text,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.pin_apps_add),
            color = palette.text,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

/** Adds a long-press-then-drag gesture. */
internal fun Modifier.longPressDrag(
    key: Any?,
    topLeft: () -> androidx.compose.ui.geometry.Offset,
    onLift: () -> Unit,
    onMove: (androidx.compose.ui.geometry.Offset) -> Unit,
    onEnd: (lifted: Boolean, pointer: androidx.compose.ui.geometry.Offset) -> Unit,
    onCancel: () -> Unit,
    /** Fires when the long press lands, with the finger's position there (before the drag has moved at all). */
    onStart: (androidx.compose.ui.geometry.Offset) -> Unit = {}
): Modifier = pointerInput(key) {
    var start = androidx.compose.ui.geometry.Offset.Zero
    var pointer = androidx.compose.ui.geometry.Offset.Zero
    var lifted = false
    detectDragGesturesAfterLongPress(
        onDragStart = { local ->
            pointer = topLeft() + local
            start = pointer
            lifted = false
            onStart(start)
        },
        onDrag = { change, amount ->
            change.consume()
            pointer += amount
            if (!lifted && (pointer - start).getDistance() > viewConfiguration.touchSlop) {
                lifted = true
                onLift()
            }
            if (lifted) onMove(pointer)
        },
        onDragEnd = { onEnd(lifted, pointer) },
        onDragCancel = onCancel
    )
}

@Composable
private fun ShortcutApp(
    app: LaunchableApp,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    joinHint: Boolean = false,
    dimmed: Boolean = false,
    /** Extra gestures (the drag), applied AFTER the click handling so they are the inner node. */
    gestureModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFolder = PinnedApps.isFolder(app.packageName)
    val icon = remember(app.packageName) {
        if (isFolder) null else runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = modifier
            .size(width = 64.dp, height = 76.dp)
            .graphicsLayer {
                alpha = if (dimmed) 0.3f else 1f
                scaleX = if (joinHint) 1.1f else 1f
                scaleY = if (joinHint) 1.1f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    joinHint -> LocalIslandPalette.current.text.copy(alpha = 0.3f)
                    highlighted -> LocalIslandPalette.current.text.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
            .then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFolder) {
            FolderTile(PinnedApps.members(app.packageName))
        } else if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier.size(44.dp)
            )
        }
        Text(
            text = if (isFolder) stringResource(R.string.pinned_folder_label) else app.label,
            color = LocalIslandPalette.current.text,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun FolderTile(members: List<String>) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(LocalIslandPalette.current.text.copy(alpha = 0.14f))
            .padding(4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            members.take(PinnedApps.MAX_FOLDER).chunked(2).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    rowMembers.forEach { memberPkg ->
                        val mini = remember(memberPkg) {
                            runCatching {
                                context.packageManager.getApplicationIcon(memberPkg)
                                    .toBitmap(width = 48, height = 48)
                                    .asImageBitmap()
                            }.getOrNull()
                        }
                        if (mini != null) {
                            Image(bitmap = mini, contentDescription = null, modifier = Modifier.size(17.dp))
                        } else {
                            Spacer(Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppActionPanel(
    app: LaunchableApp,
    actions: List<AppShortcutAction>,
    onAction: (AppShortcutAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIslandPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface.copy(alpha = 0.97f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShortcutApp(app = app, onClick = onDismiss)
        actions.forEach { action ->
            AppActionChip(action = action, onClick = { onAction(action) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppActionChip(action: AppShortcutAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalIslandPalette.current
    val icon: ImageVector
    val label: Int
    when (action) {
        AppShortcutAction.AppInfo -> { icon = Icons.Outlined.Info; label = R.string.shortcut_action_app_info }
        AppShortcutAction.FloatingWindow -> { icon = Icons.Outlined.PictureInPictureAlt; label = R.string.shortcut_action_floating_window }
        AppShortcutAction.SplitScreen -> { icon = Icons.Outlined.VerticalSplit; label = R.string.shortcut_action_split_screen }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(label),
            color = palette.text,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun NotificationBackdrop(
    notification: IslandNotification,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    val artwork = notification.largeIcon ?: notification.icon
    // The blur is baked into a small picture once (drawn back up with bilinear filtering) instead of a live render effect
    // on the picture, which re-ran its blur on every frame of the card's grow animation.
    val blurRadiusDp = if (settings.expandedBlurEnabled) settings.expandedBlurRadius else null
    val imageBitmap = remember(artwork, blurRadiusDp) {
        if (artwork != null && !artwork.isRecycled) {
            runCatching {
                (com.agupta07505.smartisland.util.MediaArt.blurredBackdrop(artwork, blurRadiusDp) ?: artwork).asImageBitmap()
            }.getOrNull()
        } else null
    }

    val accentColor = cardAccentColor(notification, settings)
    val palette = LocalIslandPalette.current
    // A picture keeps its own look (black dim, white text) on either theme; only the picture-less glow follows the palette.
    val scrimColor = palette.scrim

    Box(modifier = modifier) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.38f }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f * cardScrimScale(settings)),
                                Color.Black.copy(alpha = 0.85f * cardScrimScale(settings))
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 700f
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.35f),
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 750f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                scrimColor.copy(alpha = if (palette.isLight) 0.45f else 0.65f)
                            )
                        )
                    )
            )
        }
    }
}

/** What decides a card's height; when it changes, the height measured earlier no longer holds. */
internal fun notificationHeightSignature(n: IslandNotification): Int =
    if (liveCardTextDoesNotChangeHeight(n.mode)) {
        // A live card's text ticks (a clock, a progress, a call timer), which made its signature change every second
        // and its measured height forgotten again and again.
        listOf(n.mode, n.media.size, n.actionIntents.map { it.title }).hashCode()
    } else {
        listOf(n.mode, n.title, n.text, n.bigText, n.readerText, n.media.size, n.callStatusText, n.actionIntents.map { it.title }).hashCode()
    }

/** Modes whose text changes while the card is open without changing its height (timers, recorders, a call, navigation, downloads). */
internal fun liveCardTextDoesNotChangeHeight(mode: IslandMode?): Boolean = when (mode) {
    IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
    IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.DownloadUpload -> true
    else -> false
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
    // Throw-away copies of the state a card shares with the island: the reader-scroll tracker (the card's removal
    // resets it, which would make the shown card's fling look finished and let a finger that only stops it count as a
    // tap) and the reply drafts (a pending draft would open a reply box and ask for the keyboard in a card nobody
    // sees).
    val isolatedReaderScroll = remember { com.agupta07505.smartisland.util.ReaderScrollTracker() }
    val isolatedReplyDrafts = remember { com.agupta07505.smartisland.model.ReplyDrafts() }
    androidx.compose.ui.layout.SubcomposeLayout { constraints ->
        if (constraints.maxWidth == androidx.compose.ui.unit.Constraints.Infinity) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val fixedWidth = androidx.compose.ui.unit.Constraints(
            minWidth = constraints.maxWidth,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = androidx.compose.ui.unit.Constraints.Infinity
        )
        subcompose(Unit) {
            androidx.compose.runtime.CompositionLocalProvider(
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


@Composable
private fun ClockBatteryHeader() {
    val context = LocalContext.current
    val palette = LocalIslandPalette.current

    // Hora: atualiza no comeco de cada minuto (respeita formato 12h/24h do sistema)
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = android.text.format.DateFormat.getTimeFormat(context)
        while (true) {
            val now = System.currentTimeMillis()
            timeText = format.format(java.util.Date(now))
            kotlinx.coroutines.delay(60_000L - (now % 60_000L) + 50L)
        }
    }

    // Bateria: broadcast sticky, atualiza em tempo real enquanto a ilha esta expandida
    var level by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        fun apply(intent: Intent?) {
            intent ?: return
            val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && scale > 0) level = (l * 100) / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = apply(intent)
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        apply(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val batteryColor = when {
        charging -> palette.success
        level in 0..15 -> palette.danger
        else -> palette.text
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeText,
            color = palette.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (level >= 0) {
            Text(
                text = (if (charging) "\u26A1 " else "") + "$level%",
                color = batteryColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
