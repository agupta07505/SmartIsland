/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.AppShortcutAction
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.NotchCollapseFade
import com.agupta07505.smartisland.model.SwipeGhost
import com.agupta07505.smartisland.model.SwipeSideways
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TwoShellSwap
import com.agupta07505.smartisland.ui.expanded.CardHeightCache
import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import com.agupta07505.smartisland.ui.expanded.LocalPickerMotion
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.MediaArt
import com.agupta07505.smartisland.util.SwapTrace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs

private class CardSwapHolders(val layerA: GraphicsLayer, val layerB: GraphicsLayer) {
    // Plain holders, read and written in draw / layout
    val rec = intArrayOf(-1, -1, 0, 0)
    // The card the pager drew last (layout phase)
    val drawnKey = arrayOfNulls<String>(1)
    val recKey = arrayOfNulls<String>(2)
    var ghostOn by mutableStateOf(false)
}

@Composable
private fun rememberCardSwapHolders(): CardSwapHolders {
    // The outgoing card of a swipe stays on screen and leaves with the incoming one
    val layerA = rememberGraphicsLayer()
    val layerB = rememberGraphicsLayer()
    return remember { CardSwapHolders(layerA, layerB) }
}

private class HostedCards(val notifications: List<IslandNotification>, val selectedIndex: Int)

@Composable
private fun rememberHostedCards(
    expanded: Boolean,
    expandedShown: Boolean,
    cardLive: Boolean,
    notifications: List<IslandNotification>,
    launcherOpen: Boolean,
    selectedIndex: Int
): HostedCards {
    // the card fades out while the island closes
    val openSnapshot = remember { arrayOfNulls<Any>(2) }
    if (expanded && notifications.isNotEmpty()) {
        openSnapshot[0] = notifications
        openSnapshot[1] = selectedIndex
    }
    @Suppress("UNCHECKED_CAST")
    val closingList = openSnapshot[0] as List<IslandNotification>?
    val openWithoutCard = expanded && notifications.isEmpty() && !launcherOpen && closingList != null
    val freezeForClose = (!expanded && expandedShown && closingList != null) || openWithoutCard
    val hostNotifications = if (freezeForClose) closingList!! else notifications
    val hostSelectedIndex = if (freezeForClose) openSnapshot[1] as Int else selectedIndex
    if (!cardLive) {
        openSnapshot[0] = null
        openSnapshot[1] = null
    }
    return HostedCards(hostNotifications, hostSelectedIndex)
}

@Composable
private fun rememberCardEpoch(cardLive: Boolean, activeKey: String?, prewarmWanted: Boolean): Int {
    var cardEpoch by remember { mutableIntStateOf(0) }
    // plain holders (not state): [0] = the card was on screen since the last restart.
    val liveSeen = remember { BooleanArray(1) }
    val composedKey = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(cardLive, activeKey, prewarmWanted) {
        val key = activeKey
        if (cardLive) {
            liveSeen[0] = true
        } else {
            // not for the very first composition of a card that just arrived
            if (liveSeen[0] || (composedKey[0] != null && composedKey[0] != key)) cardEpoch++
            liveSeen[0] = false
        }
        composedKey[0] = key
    }
    return cardEpoch
}

@Composable
@NonRestartableComposable
private fun SettleCollapsedDrag(expanded: Boolean, ui: IslandInteractionState) {
    val collapsedDragLimitPx = LocalDensity.current.density * 12f
    LaunchedEffect(expanded) {
        if (!expanded && abs(ui.dragOffset) > collapsedDragLimitPx) {
            Animatable(ui.dragOffset).animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { ui.dragOffset = value }
        }
    }
}

// the media card is not composed while collapsed
@Composable
@NonRestartableComposable
private fun WarmMediaArt(activeNotification: IslandNotification?, settings: SmartIslandSettings) {
    val warmContext = LocalContext.current.applicationContext
    val warmMedia = activeNotification?.takeIf { it.mode == IslandMode.Music && it.mediaToken != null }
    val warmMediaSettings = remember(settings) { settings.forMedia() }
    val warmBlur = if (warmMediaSettings.expandedBlurEnabled) warmMediaSettings.expandedBlurRadius else null
    val warmKey = MediaArt.keyFor(warmMedia, warmBlur)
    LaunchedEffect(warmKey, warmMedia != null) {
        val n = warmMedia ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            MediaArt.load(warmContext, n, warmBlur)
        }
    }
}

/**
 * The card takes no touch while a swap runs and for a moment after it ([SwapTouchGuard.POST_SWAP_LOCK_MS]): a quick
 * tap meant for the swap can never press a button on a card that is fading, or open the app in the gap between 2
 * swaps. The touch is consumed before the card's children see it and stays available to the island's own gesture
 * (which flips the swap)
 */
private fun Modifier.inertDuringSwap(ui: IslandInteractionState): Modifier = pointerInput(ui) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (ui.cardTouchLocked(SystemClock.uptimeMillis())) event.changes.forEach { it.consume() }
        }
    }
}

private suspend fun CoroutineScope.runCardSwap(
    ui: IslandInteractionState,
    holders: CardSwapHolders,
    shownKeyNow: String?
) {
    val rec = holders.rec
    val layerA = holders.layerA
    val layerB = holders.layerB
    val sign = ui.pendingCardEnterSign
    if (sign == 0) {
        // Not a swipe (or the wait ran out): nothing is parked
        rec[1] = -1
        return
    }
    // A tap swap whose new card is not selected yet waits: the key change starts it
    if (ui.tapSwapActive && ui.pendingCardFromKey != null && ui.pendingCardFromKey == shownKeyNow) return
    // the swap ran to its end (not dropped or cancelled half way)
    var swapFinished = false
    val traced = SwapTrace.enabled()
    // The island leans with the finger and the card stays inside it
    val leanFrom = ui.cardDragX
    val useGhost = rec[1] >= 0 && rec[2] > 0
    if (!useGhost && ui.tapSwapActive) {
        AppLogRecorder.w("SwapSkip", "no parked outgoing card (rec1=${rec[1]} rec2=${rec[2]}): crossfade on the full shell")
    }
    if (!useGhost) rec[1] = -1
    try {
        // The new card is still hidden here (a swipe is pending). A tap swap's shell has not moved yet, so its 1
        // spring starts from rest
        ui.cardPop.snapTo(0f)
        if (ui.tapSwapActive) ui.tapSwapHandoffP = 0f
        // No suspension between these writes
        ui.cardLeanFromPx = leanFrom
        ui.cardDragX = 0f
        ui.cardSwapSign = sign
        if (useGhost) holders.ghostOn = true
        // Two shells: the parked outgoing card goes to the ghost shell, which carries it into the cutout
        if (useGhost && ui.tapSwapActive) {
            ui.tapSwapOutgoingLayer = if (rec[1] == 0) layerA else layerB
        }
        ui.pendingCardEnterSign = 0
        ui.pendingCardFromKey = null
        if (ui.tapSwapActive) {
            ui.swap.run()
            ui.swapBlur.beginSwap()
            while (true) {
                val target = if (ui.tapSwapReverse) 0f else 1f
                // a flip is a pause between 2 runs, not a late frame
                ui.swapBlur.restart()
                try {
                    val swapSpec = tapSwapSpring(ui.tapSwapSpeed, ui.swapBurst.boostAt(SystemClock.uptimeMillis()))
                    ui.cardPop.animateTo(target, swapSpec) {
                        ui.swapBlur.frame(System.nanoTime())
                        ui.syncTapSwapSelection(value)
                        if (traced) SwapTrace.emit("frame", "p=$value v=$velocity target=$target")
                    }
                    if (ui.tapSwapReverse == (target == 0f)) break
                } catch (e: CancellationException) {
                    ensureActive()
                }
            }
            ui.swap.settle()
            if (ui.tapSwapReverse && !ui.tapSwapBack) {
                // The outgoing card was usually picked while the shell travelled: only the frames it still needs
                repeat(ui.pickOldAtEnd()) { withFrameNanos { } }
                ui.swap.setBack(true)
                withFrameNanos { }
            }
            swapFinished = true
            ui.swapBlur.endSwap()
        } else {
            ui.cardPop.animateTo(1f, swipeEaseBackSpring())
        }
    } finally {
        // also when the effect is cancelled half way
        if (ui.pendingCardEnterSign == 0) ui.cardSwipeSnap = false
        holders.ghostOn = false
        rec[1] = -1
        withContext(NonCancellable) { ui.cleanSwapState(if (swapFinished) "settled" else "cancelled") }
        if (swapFinished) ui.onSwapSettled?.invoke()
    }
}

private fun GraphicsLayerScope.applyHostedCardLayer(
    layout: IslandLayout,
    ui: IslandInteractionState,
    shownKeyState: State<String?>,
    artworkFadesToBlack: Boolean
) {
    val shownKeyNow = shownKeyState.value
    // The shell's spring overshoots its final width for a few frames, the card follows so the shell's own surface never
    // shows beside it (nothing changes while the shell is narrower)
    val fullWidth = layout.expandedWidth.value
    val shellStretch = if (fullWidth > 0f) (layout.safeWidth.value / fullWidth).coerceIn(1f, 1.25f) else 1f
    scaleX = layout.contentScale * shellStretch
    scaleY = layout.contentScale
    translationY = layout.contentSlideY.toPx()
    translationX = if (ui.cardSwapSign != 0 && !ui.tapSwapActive) {
        SwipeSideways.incomingX(ui.cardSwapSign, size.width, ui.cardPop.value)
    } else 0f
    val fromKey = ui.pendingCardFromKey
    val waitingForEnter = ui.pendingCardEnterSign != 0 && fromKey != null && fromKey != shownKeyNow
    // Before the key changes the card on screen still is the outgoing one.
    val stillOutgoing = ui.pendingCardEnterSign != 0 && fromKey != null && fromKey == shownKeyNow
    val tapIn = if (ui.tapSwapActive) {
        TwoShellSwap.realContentAlpha(
            ui.tapSwapOutgoingLayer != null, ui.tapSwapBack, stillOutgoing, ui.tapSwapP()
        )
    } else 1f
    // The live card softens as it fades and sharpens as it comes in, on the same alpha as the fade
    renderEffect = if (ui.tapSwapActive && !waitingForEnter) ui.swapBlur.effectFor(tapIn, density) else null
    alpha = tapIn * (if (waitingForEnter) 0f
    else if (artworkFadesToBlack) NotchCollapseFade.layerAlpha(layout.expandedAlpha)
    else layout.expandedAlpha)
}

private fun ContentDrawScope.recordSwapCard(
    ui: IslandInteractionState,
    holders: CardSwapHolders,
    shownKeyState: State<String?>
) {
    val rec = holders.rec
    val recKey = holders.recKey
    val drawnKey = holders.drawnKey
    val layerA = holders.layerA
    val layerB = holders.layerB
    val shownKeyNow = shownKeyState.value
    // Records the card only while a swipe can need its last frame
    val switched = ui.pendingCardEnterSign != 0 && ui.pendingCardFromKey != null &&
        (ui.pendingCardFromKey != shownKeyNow || (drawnKey[0] != null && drawnKey[0] != ui.pendingCardFromKey))
    // Only until the switch has been parked (a drag, or a swipe waiting for its card)
    val recording = ui.cardDragX != 0f || ui.pendingCardEnterSign != 0
    if (!recording) {
        rec[0] = -1
        recKey[0] = null
        recKey[1] = null
        drawContent()
    } else {
        // The layer written in the frame before the switch holds the old card: park it
        if (rec[1] < 0 && switched && rec[0] >= 0) {
            rec[1] = SwipeGhost.layerToPark(rec[0], recKey, ui.pendingCardFromKey)
        }
        // layerA / layerB are shared with the cutout tap swap, which dims them for its ghost shell: a plain swipe must
        // never inherit that look (it showed as blurred, flickering text)
        if (!ui.tapSwapActive) {
            layerA.resetSwapLook()
            layerB.resetSwapLook()
        }
        val idx = if (rec[1] >= 0) 1 - rec[1] else if (rec[0] >= 0) 1 - rec[0] else 0
        val target = if (idx == 0) layerA else layerB
        target.record { this@recordSwapCard.drawContent() }
        recKey[idx] = drawnKey[0] ?: shownKeyNow
        // the pager can draw the new card before shownKey changes
        val pendingFrom = ui.pendingCardFromKey
        val holdBack = ui.pendingCardEnterSign != 0 && pendingFrom != null &&
            (pendingFrom != shownKeyNow || (drawnKey[0] != null && drawnKey[0] != pendingFrom))
        if (!holdBack) drawLayer(target)
        rec[0] = idx
        rec[2] = size.width.toInt()
        rec[3] = size.height.toInt()
    }
}

private fun sideBarsAlphaOf(ui: IslandInteractionState, fadePx: Float): Float {
    val pop = ui.cardPop.value
    return when {
        ui.pendingCardEnterSign != 0 -> 0f
        ui.cardSwapSign != 0 -> ((pop - SIDE_BARS_BACK_FROM) / (1f - SIDE_BARS_BACK_FROM)).coerceIn(0f, 1f)
        else -> 1f - (abs(ui.cardDragX) / fadePx).coerceIn(0f, 1f)
    }
}

@Composable
@NonRestartableComposable
private fun OutgoingCardGhost(layout: IslandLayout, ui: IslandInteractionState, holders: CardSwapHolders) {
    val rec = holders.rec
    val layerA = holders.layerA
    val layerB = holders.layerB
    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val w = rec[2]
                val h = rec[3]
                val placeable = measurable.measure(Constraints.fixed(w, h))
                this.layout(w, h) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                scaleX = layout.contentScale
                scaleY = layout.contentScale
                translationY = layout.contentSlideY.toPx()
                // Waiting for the new card it stays in the middle
                translationX = if (holders.ghostOn && !ui.tapSwapActive) {
                    SwipeSideways.outgoingX(ui.cardSwapSign, size.width, ui.cardPop.value)
                } else 0f
                // once the ghost shell carries the outgoing card of a tap swap it is drawn there, not here
                alpha = layout.expandedAlpha * (if (ui.tapSwapActive && ui.tapSwapOutgoingLayer != null) 0f else 1f)
            }
            .drawBehind {
                val i = rec[1]
                if (i >= 0) drawLayer(if (i == 0) layerA else layerB)
            }
    )
}

@Composable
internal fun ExpandedCardHost(
    expanded: Boolean,
    layout: IslandLayout,
    settings: SmartIslandSettings,
    notifications: List<IslandNotification>,
    activeNotification: IslandNotification?,
    prewarmWanted: Boolean,
    launcherApps: List<LaunchableApp>?,
    launcherOpen: Boolean = false,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppAction: (String, AppShortcutAction) -> Unit = { _, _ -> },
    onToggleExpanded: () -> Unit,
    onReplyStateChanged: (Boolean) -> Unit,
    onTextExpandedChanged: (Boolean) -> Unit,
    siblingPickerOpen: Boolean,
    onSiblingSelected: (String) -> Unit,
    onSiblingPickerClose: () -> Unit,
    isLandscape: Boolean,
    pickerOnLeft: Boolean,
    configuration: Configuration,
    onHeightMeasured: (Dp) -> Unit,
    onSplitCardWidthMeasured: (Float) -> Unit,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed
) {
    // a swipe that put another card in the island
    LaunchedEffect(expanded, layout.expandedShown) {
        if (!expanded && !layout.expandedShown) ui.resetCard()
    }
    LaunchedEffect(expanded) { if (!expanded) { ui.tapMemory.clear(); ui.endTapSwap("collapsed") } }
    SettleCollapsedDrag(expanded, ui)
    val shownKey = activeNotification?.key
    val shownKeyNow = rememberUpdatedState(shownKey)
    val holders = rememberCardSwapHolders()
    // The new card is composed 1 frame before its swap is set up
    val waitingNow = ui.pendingCardEnterSign != 0 && ui.pendingCardFromKey != null
    val targetKey = if (ui.tapSwapActive) ui.tapSwapToKey else null
    // Selection can change again on the way back. Keep the same spring for the whole tap swap.
    LaunchedEffect(targetKey, if (targetKey == null) shownKey else null) {
        if (targetKey != null) {
            snapshotFlow { shownKeyNow.value }.first { it == targetKey }
        }
        runCardSwap(ui, holders, shownKeyNow.value)
    }
    val cardLive = expanded || layout.expandedShown
    val sideBarsFadePx = LocalDensity.current.density * SIDE_BARS_FADE_DP
    // Card heights outlive the card's composition (it is rebuilt after every close)
    val heightCache = remember(layout.expandedWidth) { CardHeightCache() }
    val hosted = rememberHostedCards(expanded, layout.expandedShown, cardLive, notifications, launcherOpen,
        selectedIndex)
    val hostNotifications = hosted.notifications
    val hostSelectedIndex = hosted.selectedIndex
    val cardLiveNow by rememberUpdatedState(cardLive)
    val cardEpoch = rememberCardEpoch(cardLive, activeNotification?.key, prewarmWanted)
    WarmMediaArt(activeNotification, settings)
    // A tap swap brings in a neighbouring event: its artwork is decoded before the tap, not after it
    if (expanded) {
        WarmMediaArt(hostNotifications.getOrNull(hostSelectedIndex - 1), settings)
        WarmMediaArt(hostNotifications.getOrNull(hostSelectedIndex + 1), settings)
    }
    val artworkFadesToBlack = NotchCollapseFade.appliesTo(
        notchMode = settings.enableNotchMode,
        isMusic = hostNotifications.getOrNull(hostSelectedIndex)?.mode == IslandMode.Music,
        artworkBackdrop = settings.enableMusicArtworkBackground
    )
    if (cardLive || prewarmWanted) {
        key(cardEpoch) {
        PrewarmedCard(
            placed = { expanded || layout.expandedShown },
            modifier = Modifier
                .requiredWidth(layout.expandedWidth)
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .graphicsLayer { applyHostedCardLayer(layout, ui, shownKeyNow, artworkFadesToBlack) }
                .drawWithContent { recordSwapCard(ui, holders, shownKeyNow) }
                .inertDuringSwap(ui)
        ) {
            CompositionLocalProvider(
                LocalPickerMotion provides ui.pickerMotion,
                LocalCardShown provides cardLive,
                LocalExpandedAlpha provides (if (artworkFadesToBlack) ({ layout.expandedAlpha }) else null),
                LocalSwapActionsAlpha provides ({
                    if (ui.tapSwapActive && ui.tapSwapOutgoingLayer != null && !ui.tapSwapBack) {
                        TapSwap.actionsAlpha(ui.tapSwapP())
                    } else 1f
                })
            ) {
            IslandExpandedContent(
                notifications = hostNotifications,
                launcherApps = launcherApps,
                selectedIndex = hostSelectedIndex,
                // a waiting card must not touch the selection or the auto-collapse timer
                onPageSelected = { index -> if (cardLiveNow) onPageSelected(index) },
                onOpenNotification = onOpenNotification,
                onLaunchApp = onLaunchApp,
                onAppAction = onAppAction,
                onCollapse = onToggleExpanded,
                statusBarHeight = layout.topBarHeight.dp,
                // Each mode owns its natural height
                onHeightMeasured = onHeightMeasured,
                settings = settings,
                // While waiting, only "off" gets through
                onReplyStateChanged = { active -> if (!active || cardLiveNow) onReplyStateChanged(active) },
                onTextExpandedChanged = { active -> if (!active || cardLiveNow) onTextExpandedChanged(active) },
                snapPageSync = { ui.cardSwipeSnap },
                // Light look side bars: faded out by the drag, hidden during the swap
                sideBarsAlpha = { sideBarsAlphaOf(ui, sideBarsFadePx) },
                drawnCardKey = holders.drawnKey,
                heightCache = heightCache,
                siblingPickerOpen = siblingPickerOpen,
                onSiblingSelected = onSiblingSelected,
                onSiblingPickerClose = onSiblingPickerClose,
                sideBySidePicker = isLandscape,
                pickerOnLeft = pickerOnLeft,
                splitCardMaxWidth = splitCardMaxWidthDp(configuration.screenWidthDp.toFloat()).dp,
                splitPickerWidth = splitPickerWidthDp(configuration.screenWidthDp.toFloat()).dp,
                singleIslandWidth = calculateExpandedWidth(
                    isLandscape = true,
                    screenWidthDp = configuration.screenWidthDp.toFloat(),
                    screenHeightDp = configuration.screenHeightDp.toFloat()
                ).dp,
                onSplitCardWidthMeasured = { onSplitCardWidthMeasured(it.value) }
            )
            }
        }
        }
        if (cardLive && (holders.ghostOn || waitingNow)) OutgoingCardGhost(layout, ui, holders)
    }
}

private const val SIDE_BARS_FADE_DP = 14f

private const val SIDE_BARS_BACK_FROM = 0.85f
