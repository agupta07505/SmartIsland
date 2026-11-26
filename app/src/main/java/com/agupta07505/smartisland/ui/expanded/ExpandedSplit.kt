/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PickerSwap
import com.agupta07505.smartisland.model.SplitMotion
import com.agupta07505.smartisland.model.SplitPhase
import com.agupta07505.smartisland.model.siblingIndices
import com.agupta07505.smartisland.ui.LocalCardShown
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal class SplitState(
    val pickerSiblings: List<IslandNotification>,
    val pickerShown: Boolean,
    val siblingsHold: MutableList<IslandNotification>,
    val splitPickerSiblings: List<IslandNotification>,
    val pictureBeside: Boolean,
    val besideSteps: CardSteps,
    val sideBySideWanted: Boolean,
    val progress: Animatable<Float, AnimationVector1D>,
    val wasPicker: BooleanArray,
    val islandIsPicture: BooleanArray,
    val besideHold: Array<CardSteps?>,
    val sideBySide: Boolean,
    val shownHeight: Dp
)

@Composable
internal fun rememberSplitState(
    notifications: List<IslandNotification>,
    pageCards: List<Int>,
    currentPage: Int,
    selectedIndex: Int,
    siblingPickerOpen: Boolean,
    sideBySidePicker: Boolean,
    settings: SmartIslandSettings,
    mediaExpansion: MediaExpansion,
    cardStepsAccess: CardStepsAccess,
    currentNotification: IslandNotification?,
    stepDrag: StepDragState,
    targetHeight: Dp,
    onHeightMeasured: (Dp) -> Unit
): SplitState {
    val pickerSiblings = if (siblingPickerOpen) {
        siblingIndices(notifications, pageCards.getOrNull(currentPage) ?: selectedIndex)
            .map { notifications[it] }
    } else emptyList()
    val pickerShown = pickerSiblings.isNotEmpty()
    val siblingsHold = remember { mutableListOf<IslandNotification>() }
    if (pickerSiblings.isNotEmpty()) {
        siblingsHold.clear()
        siblingsHold.addAll(pickerSiblings)
    }
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
    val splitProgress = remember { Animatable(0f) }
    val splitSpeed = settings.animationSpeed(sideBySidePicker)
    val splitWasPicker = remember { booleanArrayOf(false) }
    // The flag keeps the two-island layout while the island shrinks away
    val splitIslandIsPicture = remember { booleanArrayOf(false) }
    val besideHold = remember { arrayOfNulls<CardSteps>(1) }
    if (sideBySideWanted) {
        splitWasPicker[0] = true
        splitIslandIsPicture[0] = pictureBeside
        if (pictureBeside) besideHold[0] = besideSteps
    }
    val dragOwnsSplit = sideBySidePicker && stepDrag.active && !stepDrag.settled &&
        stepDrag.cardKey == currentNotification?.key &&
        (stepDrag.fromSteps?.let { stepsOpenBesideCard(it) } ?: false) != pictureBeside
    LaunchedEffect(sideBySideWanted, dragOwnsSplit) {
        if (dragOwnsSplit) {
            val opening = pictureBeside
            snapshotFlow { stepDrag.progress }.collect { p ->
                splitProgress.snapTo(SplitMotion.fromGeometry(besideDragSplitProgress(opening, p)))
            }
        } else {
            // a spring without bounce: a turn-around keeps its speed
            splitProgress.animateTo(
                targetValue = if (sideBySideWanted) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = splitSpeed.stiffness(
                        if (sideBySideWanted) PickerSwap.SPLIT_OPEN_STIFFNESS
                        else PickerSwap.SPLIT_CLOSE_STIFFNESS
                    )
                )
            )
        }
    }
    // Only the derived flag is read so the content does not recompose per frame
    val splitHolding by remember(sideBySideWanted) {
        derivedStateOf {
            SplitMotion.holdTwoIslandLayout(
                splitWasPicker[0], sideBySideWanted, splitProgress.value, stepDrag.shellSingleReady
            )
        }
    }
    // The progress is gone: the shell may switch now
    val splitGone by remember(sideBySideWanted) {
        derivedStateOf {
            splitWasPicker[0] && !sideBySideWanted && SplitMotion.phase(
                sideBySideWanted, splitProgress.value
            ) == SplitPhase.Closed
        }
    }
    LaunchedEffect(sideBySideWanted) { if (sideBySideWanted) stepDrag.shellSingleReady = false }
    // safety: if the shell never confirms (collapsed meanwhile)
    LaunchedEffect(splitGone) {
        if (splitGone) {
            delay(SplitMotion.HANDOVER_CAP_MS)
            stepDrag.shellSingleReady = true
        }
    }
    val sideBySide = sideBySideWanted || splitHolding
    val splitGoneNow = splitGone
    SideEffect {
        stepDrag.splitShown = sideBySide && !splitGoneNow
        stepDrag.splitProgressNow = if (sideBySide && splitWasPicker[0]) ({ splitProgress.value }) else null
    }
    DisposableEffect(Unit) {
        onDispose {
            stepDrag.splitShown = false
            stepDrag.splitProgressNow = null
        }
    }
    val heldBesideFull = besideHold[0]?.depth == CardDepth.Picture
    val shownHeight = when {
        pictureBeside -> pictureIslandStepHeight(fullSize = besideSteps.depth == CardDepth.Picture)
        sideBySide && splitIslandIsPicture[0] -> pictureIslandStepHeight(fullSize = heldBesideFull)
        sideBySide -> maxOf(targetHeight, siblingPickerHeight(splitPickerSiblings.size, compact = true))
            // Not 260: a notification with the reader, a chip row and the action row is NOTIFICATION_CARD_MAX_HEIGHT
            .coerceAtMost(maxOf(260.dp, NOTIFICATION_CARD_MAX_HEIGHT))
        pickerShown -> siblingPickerHeight(pickerSiblings.size)
        else -> targetHeight
    }

    SideEffect {
        stepDrag.targetDp = shownHeight.value
        stepDrag.splitSingleHeightDp = targetHeight.value
        stepDrag.splitTotalHeightDp = shownHeight.value
    }

    LaunchedEffect(shownHeight) {
        onHeightMeasured(shownHeight)
    }

    return SplitState(
        pickerSiblings, pickerShown, siblingsHold, splitPickerSiblings, pictureBeside, besideSteps,
        sideBySideWanted, splitProgress, splitWasPicker, splitIslandIsPicture, besideHold, sideBySide, shownHeight
    )
}

internal class PortraitSwap(
    val progress: Animatable<Float, AnimationVector1D>,
    val cardLayerShown: State<Boolean>,
    val pickerLayerShown: State<Boolean>
)

/** Whether the portrait picker is trading places with the card, which a cutout tap swap waits for. */
internal class PickerMotion {
    var moving by mutableStateOf(false)
}

internal val LocalPickerMotion = staticCompositionLocalOf { PickerMotion() }

@Composable
internal fun rememberPortraitSwap(
    portraitPickerOpen: Boolean,
    settings: SmartIslandSettings,
    sideBySidePicker: Boolean
): PortraitSwap {
    val swap = remember { Animatable(0f) }
    val swapSpeed = settings.animationSpeed(sideBySidePicker)
    val motion = LocalPickerMotion.current
    val cardShown = LocalCardShown.current
    val pickerOpenNow = rememberUpdatedState(portraitPickerOpen)
    // Moving from the frame the picker is asked for, before its animation has started; only the card in the island
    // reports it (a waiting card has the same picker state)
    LaunchedEffect(swap, motion, cardShown) {
        if (!cardShown) return@LaunchedEffect
        snapshotFlow { swap.isRunning || swap.value != (if (pickerOpenNow.value) 1f else 0f) }
            .collect { motion.moving = it }
    }
    DisposableEffect(motion) { onDispose { motion.moving = false } }
    LaunchedEffect(portraitPickerOpen) {
        swap.animateTo(
            targetValue = if (portraitPickerOpen) 1f else 0f,
            animationSpec = tween(
                durationMillis = swapSpeed.durationMs(PickerSwap.lengthMs(portraitPickerOpen)),
                easing = FastOutSlowInEasing
            )
        )
    }
    val cardLayerShown = remember(portraitPickerOpen) {
        derivedStateOf {
            PickerSwap.cardLayerShown(swap.value, portraitPickerOpen)
        }
    }
    val pickerLayerShown = remember(portraitPickerOpen) {
        derivedStateOf {
            PickerSwap.pickerLayerShown(swap.value, portraitPickerOpen)
        }
    }

    return PortraitSwap(swap, cardLayerShown, pickerLayerShown)
}

private class SplitDraw(
    val raw: () -> Float,
    val ease: () -> Float,
    val naturalReported: IntArray,
    val naturalCache: IntArray,
    val reflowFade: IntArray,
    val islandWidthPx: IntArray,
    val rowWidthPx: Int
)

/** Landscape, picker or picture open: card island beside a second island. */
@Composable
internal fun SplitIslands(
    splitCard: IslandNotification,
    split: SplitState,
    settings: SmartIslandSettings,
    targetHeight: Dp,
    stepDrag: StepDragState,
    mediaExpansion: MediaExpansion,
    cardStepsAccess: CardStepsAccess,
    cache: CardHeightCache,
    bottomPadding: Dp,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    onSiblingSelected: (String) -> Unit,
    onSiblingPickerClose: () -> Unit,
    pickerOnLeft: Boolean,
    splitCardMaxWidth: Dp,
    splitPickerWidth: Dp,
    singleIslandWidth: Dp,
    onSplitCardWidthMeasured: (Dp) -> Unit
) {
    val shownHeight = split.shownHeight
    val splitProgress = split.progress
    val splitWasPicker = split.wasPicker
    val sideBySideWanted = split.sideBySideWanted
    // 1 progress for both islands
    val splitRaw: () -> Float = { if (splitWasPicker[0]) splitProgress.value else 1f }
    val splitEase: () -> Float = { SplitMotion.geometry(splitRaw()) }
    val naturalReported = remember { intArrayOf(-1) }
    val naturalCache = remember(splitCard) { intArrayOf(-1, -1) }
    // [0] = 1 while the card's content fades through its one re-wrap.
    val reflowFade = remember { intArrayOf(0) }
    // Widest the card island gets during the swap (px)
    val islandWidthPx = remember { intArrayOf(0) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(shownHeight)
    ) {
        val rowWidthPx = constraints.maxWidth
        val draw = SplitDraw(splitRaw, splitEase, naturalReported, naturalCache, reflowFade, islandWidthPx, rowWidthPx)
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
                        val placeable = measurable.measure(Constraints.fixed(w, 0))
                        layout(w, 0) { placeable.place(0, 0) }
                    }
                )
            }
            val cardIsland: @Composable () -> Unit = {
                SplitCardIsland(
                    splitCard, split, draw, settings, targetHeight, mediaExpansion, cache,
                    bottomPadding, launcherApps, onOpenNotification, onLaunchApp, onAppAction, onCollapse,
                    onReplyStateChanged, onTextExpandedChanged,
                    splitCardMaxWidth, splitPickerWidth, singleIslandWidth, onSplitCardWidthMeasured
                )
            }
            val pickerIsland: @Composable () -> Unit = {
                SplitPickerIsland(
                    splitCard, split, draw, settings, targetHeight, pickerOnLeft, splitPickerWidth,
                    onSiblingSelected, onSiblingPickerClose
                )
            }
            val pictureIsland: @Composable () -> Unit = {
                SplitPictureIsland(
                    splitCard, split, draw, settings, targetHeight, stepDrag, mediaExpansion, cardStepsAccess,
                    pickerOnLeft, splitPickerWidth
                )
            }
            val sideIsland = if (split.pictureBeside || (!split.pickerShown
                && split.islandIsPicture[0])) pictureIsland else pickerIsland
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

@Composable
private fun SplitCardIsland(
    splitCard: IslandNotification,
    split: SplitState,
    draw: SplitDraw,
    settings: SmartIslandSettings,
    targetHeight: Dp,
    mediaExpansion: MediaExpansion,
    cache: CardHeightCache,
    bottomPadding: Dp,
    launcherApps: List<LaunchableApp>?,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit,
    onCollapse: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    splitCardMaxWidth: Dp,
    splitPickerWidth: Dp,
    singleIslandWidth: Dp,
    onSplitCardWidthMeasured: (Dp) -> Unit
) {
    val splitRaw = draw.raw
    val naturalReported = draw.naturalReported
    val naturalCache = draw.naturalCache
    val reflowFade = draw.reflowFade
    val islandWidthPx = draw.islandWidthPx
    val rowWidthPx = draw.rowWidthPx
    val splitWasPicker = split.wasPicker
    val sideBySideWanted = split.sideBySideWanted
    val pictureBeside = split.pictureBeside
    var pageHeights by cache.heights
    Box(
        modifier = Modifier
            // No fixed height: as tall as the card at its drawn width.
            .islandSurface(
                settings, settings.forCard(splitCard.mode == IslandMode.Music).opacity,
                shadowStrength = { SplitMotion.shadowStrength(splitRaw()) },
                palette = LocalIslandPalette.current
            )
            .layout { measurable, constraints ->
                // The natural width (what the window is sized from) is measured every time
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
                val p = SplitMotion.geometry(raw)
                val singlePx = singleIslandWidth.roundToPx()
                // The window is sized from this slot
                val swapping = splitWasPicker[0] && SplitMotion.swapping(
                    SplitMotion.phase(sideBySideWanted, raw)
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
                // the island's width runs every frame (it is only a clip)
                val finalW = natural.coerceAtMost((rowWidthPx - (SPLIT_ISLAND_GAP.roundToPx() + splitPickerWidth.roundToPx())).coerceAtLeast(1))
                    .coerceAtLeast(1)
                val wideW = maxOf(singlePx, finalW)
                islandWidthPx[0] = wideW
                val contentW = if (splitWasPicker[0]) {
                    if (SplitMotion.reflowed(raw)) finalW else wideW
                } else w
                reflowFade[0] = if (splitWasPicker[0] && wideW != finalW) 1 else 0
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = contentW, maxWidth = contentW,
                        minHeight = 0, maxHeight = Constraints.Infinity
                    )
                )
                val islandH = clampCardHeightForMode(splitCard.mode, placeable.height.toDp())
                    .roundToPx().coerceAtMost(capPx).coerceAtLeast(1)
                layout(w, islandH) { placeable.place(0, 0) }
            }
    ) {
      // the re-wrap dip is on the card's foreground only
      Box {
        CompositionLocalProvider(
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
                if (reflowFade[0] == 1) SplitMotion.reflowAlpha(splitRaw()) else 1f
            },
            // The widest the island is during the swap (a constant per card)
            backdropWidthPx = { islandWidthPx[0] }
        )
        }
      }
    }
}

@Composable
private fun SplitPickerIsland(
    splitCard: IslandNotification,
    split: SplitState,
    draw: SplitDraw,
    settings: SmartIslandSettings,
    targetHeight: Dp,
    pickerOnLeft: Boolean,
    splitPickerWidth: Dp,
    onSiblingSelected: (String) -> Unit,
    onSiblingPickerClose: () -> Unit
) {
    val splitRaw = draw.raw
    val splitEase = draw.ease
    val splitPickerSiblings = split.splitPickerSiblings
    val sideBySideWanted = split.sideBySideWanted
    SiblingPicker(
        siblings = splitPickerSiblings,
        currentKey = splitCard.key,
        // a tap while the island shrinks away belongs to nobody.
        onSelect = { key -> if (sideBySideWanted) onSiblingSelected(key) },
        onClose = { if (sideBySideWanted) onSiblingPickerClose() },
        tapSlop = GestureSensitivity.fromId(settings.gestureSensitivity).scaled(PICKER_TAP_SLOP_DP).dp,
        modifier = Modifier
            // No surface fade while it shrinks away
            .islandSurface(
                settings, maxOf(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY),
                shadowStrength = { SplitMotion.shadowStrength(splitRaw()) },
                palette = LocalIslandPalette.current
            )
            // the island grows in height with its width, from the card's height
            .splitSideClip(splitPickerWidth, pickerOnLeft, splitEase, targetHeight, heightGrow = splitEase),
        compact = true
    )
}

@Composable
private fun SplitPictureIsland(
    splitCard: IslandNotification,
    split: SplitState,
    draw: SplitDraw,
    settings: SmartIslandSettings,
    targetHeight: Dp,
    stepDrag: StepDragState,
    mediaExpansion: MediaExpansion,
    cardStepsAccess: CardStepsAccess,
    pickerOnLeft: Boolean,
    splitPickerWidth: Dp
) {
    val density = LocalDensity.current
    val splitRaw = draw.raw
    val splitEase = draw.ease
    val pictureBeside = split.pictureBeside
    val besideSteps = split.besideSteps
    val besideHold = split.besideHold
    val shownHeight = split.shownHeight
    val islandSteps = if (pictureBeside) besideSteps else (besideHold[0] ?: besideSteps)
    val shown = splitCard.media.getOrNull(islandSteps.pictureIndex ?: mediaExpansion.index)
    val selectorShown = islandSteps.depth == CardDepth.Selector
    // Like the sibling picker's island
    val islandModifier = Modifier
        .layout { measurable, constraints ->
            val naturalPx = shownHeight.roundToPx()
            val hPx = if (stepDrag.active && stepDrag.heightDp >= 0f) {
                (stepDrag.heightDp * density.density).roundToInt().coerceIn(1, maxOf(260.dp, NOTIFICATION_CARD_MAX_HEIGHT).roundToPx())
            } else naturalPx
            val placeable = measurable.measure(constraints.copy(minHeight = hPx, maxHeight = hPx))
            layout(placeable.width, hPx) { placeable.place(0, 0) }
        }
        .islandSurface(
            settings, maxOf(settings.opacity, SIBLING_PICKER_SURFACE_OPACITY),
            shadowStrength = { SplitMotion.shadowStrength(splitRaw()) },
            palette = LocalIslandPalette.current
        )
        .splitSideClip(
            splitPickerWidth, pickerOnLeft, splitEase, targetHeight,
            heightGrow = { if (stepDrag.active) 1f else splitEase() }
        )
    if (selectorShown) {
        PictureSelector(
            media = splitCard.media,
            onPick = { index ->
                cardStepsAccess.set(splitCard.key, CardSteps.pickFromSelector(islandSteps, index))
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

/**
 * The side island of the split: the list or picture keeps its full width and the island clips it as it
 * grows; its height runs from [restingHeight] to the content's height by [heightGrow].
 */
private fun Modifier.splitSideClip(
    fullWidth: Dp,
    pickerOnLeft: Boolean,
    ease: () -> Float,
    restingHeight: Dp,
    heightGrow: () -> Float
): Modifier = layout { measurable, constraints ->
    val full = fullWidth.roundToPx()
    val w = (full * ease()).roundToInt().coerceIn(0, full)
    val placeable = measurable.measure(
        Constraints(
            minWidth = full, maxWidth = full,
            minHeight = constraints.minHeight, maxHeight = constraints.maxHeight
        )
    )
    val restingPx = restingHeight.roundToPx().coerceIn(1, maxOf(1, placeable.height))
    val islandH = (restingPx + (placeable.height - restingPx) * heightGrow()).roundToInt()
    layout(w, islandH) { placeable.place(if (pickerOnLeft) w - full else 0, 0) }
}
