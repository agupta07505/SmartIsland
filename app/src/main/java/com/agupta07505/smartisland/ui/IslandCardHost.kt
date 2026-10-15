/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs

/**
 * The expanded card's host. Fades out while collapsing and is laid out at its final width the whole time. For the modes
 * in CardPrewarm it is also composed while collapsed, so the tap does not have to compose it.
 */
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
    onAppAction: (String, com.agupta07505.smartisland.model.AppShortcutAction) -> Unit = { _, _ -> },
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
    animSpeed: com.agupta07505.smartisland.util.AnimationSpeed
) {
    // A swipe that put another card in the island. The old card stays where the finger left it
    // (see islandMainGestures) and the new one comes in from the opposite side. If no card arrives in time, the
    // gesture handler eases the old one back instead.
    // Island closed (or the card faded out) in the middle of a swipe: forget every sideways move, so the next open starts centred.
    LaunchedEffect(expanded, layout.expandedShown) {
        if (!expanded && !layout.expandedShown) ui.resetCard()
    }
    // Closed with a vertical drag still on the island (the auto-collapse fired under a finger, or the gesture never got to finish):
    // the pill must not stay where the expanded card was dragged to, because there it is outside its touch region and the
    // shrinking window. Offsets the collapsed pill's own damped drag can reach are left to the gesture.
    val collapsedDragLimitPx = androidx.compose.ui.platform.LocalDensity.current.density * 12f
    LaunchedEffect(expanded) {
        if (!expanded && kotlin.math.abs(ui.dragOffset) > collapsedDragLimitPx) {
            androidx.compose.animation.core.Animatable(ui.dragOffset).animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            ) { ui.dragOffset = value }
        }
    }
    // A swipe that put another card in the island: the old card stays where the finger left it and the new one comes in
    // from the opposite side. If no card arrives in time, the gesture handler eases the old one back instead.
    val shownKey = activeNotification?.key
    val shownKeyNow by rememberUpdatedState(shownKey)
    // The outgoing card of a swipe stays on screen and leaves with the incoming one, as one strip. It is not composed a
    // second time: while a swipe is under way the card is drawn through a recorded graphics layer, and when the new card arrives
    // the layer that holds the old card's last frame is parked and drawn by the ghost node below. Two layers alternate, so the
    // frame before the switch is never overwritten.
    val layerA = rememberGraphicsLayer()
    val layerB = rememberGraphicsLayer()
    // Plain holders, read and written in draw / layout: [0] = layer drawn last (-1 = the last frame was not recorded), [1] = layer parked with the outgoing card (-1 =
    // none), [2] and [3] = width and height (px) of the card as last drawn.
    val rec = remember { intArrayOf(-1, -1, 0, 0) }
    // The card the pager drew last (layout phase): the pager can show the new card before [shownKey] changes, so parking goes by this.
    val drawnKey = remember { arrayOfNulls<String>(1) }
    // The key of the card each of the two recorded layers holds, so only a layer that holds the swiped card is ever parked (model/SwipeGhost.kt).
    val recKey = remember { arrayOfNulls<String>(2) }
    var ghostOn by remember { mutableStateOf(false) }
    // The new card is composed one frame before its swap is set up; the ghost covers that frame.
    // State only: whether the pager has already drawn the new card is decided at draw time (drawnKey is a plain value written in layout),
    // where the ghost draws the parked old card only once the switch was detected (rec[1] >= 0).
    val waitingNow = ui.pendingCardEnterSign != 0 && ui.pendingCardFromKey != null
    LaunchedEffect(shownKey) {
        val sign = ui.pendingCardEnterSign
        if (sign == 0) {
            // Not a swipe (or the wait ran out): nothing is parked.
            rec[1] = -1
            return@LaunchedEffect
        }
        // The island leans with the finger and the card stays inside it (see islandPillSurface). On the lift the island springs
        // back to the middle while the old card (the parked layer) leaves in the swipe's direction and the new card comes in from the
        // opposite side, all on ONE spring (cardPop 0 -> 1), no fade. The two cards are one card apart the whole time, so no gap opens.
        val leanFrom = ui.cardDragX
        val useGhost = rec[1] >= 0 && rec[2] > 0
        if (!useGhost) rec[1] = -1
        try {
            // The new card is still hidden here (a swipe is pending), so starting the swap at 0 first is not seen.
            ui.cardPop.snapTo(0f)
            // No suspension between these writes: the island's total lean (finger part + release part) stays the same.
            ui.cardLeanFromPx = leanFrom
            ui.cardDragX = 0f
            ui.cardSwapSign = sign
            if (useGhost) ghostOn = true
            ui.pendingCardEnterSign = 0
            ui.pendingCardFromKey = null
            ui.cardPop.animateTo(1f, swipeEaseBackSpring())
        } finally {
            // Also when the effect is cancelled half way (a second swipe, the card re-keyed, the host leaving): nothing of this swap may be
            // left behind for the next one (a card stuck off to the side, a lean that never ends, a parked layer).
            // (A second swipe that already lifted has set the snap flag for ITS swap: that one is not ours to clear.)
            if (ui.pendingCardEnterSign == 0) ui.cardSwipeSnap = false
            ghostOn = false
            rec[1] = -1
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { ui.cleanSwapState() }
        }
    }
    // Expanded content layer; it fades out while collapsing.
    // For the modes in CardPrewarm the card is also composed and measured while the island is
    // collapsed (PrewarmedCard places it only while it is shown), so the tap does not have to compose it and the
    // height the spring chases is already the real one. `cardLive` is the old condition for "the card is on screen".
    val cardLive = expanded || layout.expandedShown
    val sideBarsFadePx = LocalDensity.current.density * SIDE_BARS_FADE_DP
    // Card heights outlive the card's composition (it is rebuilt after every close), so what was measured while collapsed or on
    // an earlier visit is still there on the next open and swipe. A different card width (rotation) makes them wrong: start over.
    val heightCache = remember(layout.expandedWidth) { com.agupta07505.smartisland.ui.expanded.CardHeightCache() }
    // The card fades out while the island closes. If the list changes in that time (tapping a notification opens its app, which
    // removes the card), the fading card must not turn into the next one: it would flash open for a moment. So while the close
    // runs, the list and page of the last frame the island was open are kept. Plain holder: [0] = list, [1] = page.
    val openSnapshot = remember { arrayOfNulls<Any>(2) }
    // Only a list that has a card in it is kept: when the card goes (its app came to the front and hides it, it was cleared) the list
    // can be empty for a frame or two while the island is still open. Keeping that list would leave nothing to fade out, so the island
    // would close as an empty gray card of the old size. The last list that had a card stays, and the close fades that card instead.
    if (expanded && notifications.isNotEmpty()) {
        openSnapshot[0] = notifications
        openSnapshot[1] = selectedIndex
    }
    @Suppress("UNCHECKED_CAST")
    val closingList = openSnapshot[0] as List<IslandNotification>?
    // Still open, but nothing to show and no launcher to fall back to (an empty list draws nothing then): keep drawing the last card
    // until the island closes (a moment later) rather than an empty shell.
    val openWithoutCard = expanded && notifications.isEmpty() && !launcherOpen && closingList != null
    val freezeForClose = (!expanded && layout.expandedShown && closingList != null) || openWithoutCard
    val hostNotifications = if (freezeForClose) closingList!! else notifications
    val hostSelectedIndex = if (freezeForClose) openSnapshot[1] as Int else selectedIndex
    if (!cardLive) {
        openSnapshot[0] = null
        openSnapshot[1] = null
    }
    val cardLiveNow by rememberUpdatedState(cardLive)
    // Collapsed again (card faded out) or a different card became the active one while collapsed: start the waiting
    // card from scratch, so a reply box, a long message or an open picture of the last visit is forgotten,
    // and the new card is measured at its own page.
    var cardEpoch by remember { mutableIntStateOf(0) }
    // Plain holders (not state): [0] = the card was on screen since the last restart, [1] = key it was composed for.
    val liveSeen = remember { BooleanArray(1) }
    val composedKey = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(cardLive, activeNotification?.key, prewarmWanted) {
        val key = activeNotification?.key
        if (cardLive) {
            liveSeen[0] = true
        } else {
            // Not for the very first composition of a card that just arrived: it is already fresh.
            if (liveSeen[0] || (composedKey[0] != null && composedKey[0] != key)) cardEpoch++
            liveSeen[0] = false
        }
        composedKey[0] = key
    }
    // The media card is not composed while collapsed (see CardPrewarm), but what it needs from the session (artwork, colour, the
    // blurred background) is loaded here in the background, so the tap finds it ready and the pill does not stall on it.
    val warmContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val warmMedia = activeNotification?.takeIf { it.mode == com.agupta07505.smartisland.model.IslandMode.Music && it.mediaToken != null }
    val warmMediaSettings = remember(settings) { settings.forMedia() }
    val warmBlur = if (warmMediaSettings.expandedBlurEnabled) warmMediaSettings.expandedBlurRadius else null
    val warmKey = com.agupta07505.smartisland.util.MediaArt.keyFor(warmMedia, warmBlur)
    LaunchedEffect(warmKey, warmMedia != null) {
        val n = warmMedia ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.agupta07505.smartisland.util.MediaArt.load(warmContext, n, warmBlur)
        }
    }
    // Top-anchored while a card step follows the finger (see StepAwareVerticalAlignment), centred otherwise.
    val stepAlignment = com.agupta07505.smartisland.ui.expanded.rememberStepAwareAlignment()
    if (cardLive || prewarmWanted) {
        androidx.compose.runtime.key(cardEpoch) {
        PrewarmedCard(
            placed = { expanded || layout.expandedShown },
            modifier = Modifier
                // The card is laid out at its final width the whole time and the growing shell clips it, so the text
                // does not re-wrap and the measured heights, which the height spring chases, hold still during the
                // growth. At rest the width is the same.
                .requiredWidth(layout.expandedWidth)
                // Laid out at its natural height whatever the shell's animated height is (the shell clips it), like the width above:
                // the shell's height spring then only re-runs the shell's layout and draw, not the card's measure. Top-aligned, so a
                // shell that is briefly shorter only cuts the card's bottom.
                .wrapContentHeight(align = androidx.compose.ui.Alignment.Top, unbounded = true)
                .graphicsLayer {
                    scaleX = layout.contentScale
                    scaleY = layout.contentScale
                    translationY = layout.contentSlideY.toPx()
                    // The card stays inside the island under the finger (the island leans, see islandPillSurface). Only the swap
                    // after a lift moves it: the new card comes in from the side the old one is leaving towards.
                    translationX = if (ui.cardSwapSign != 0) {
                        com.agupta07505.smartisland.util.SwipeSideways.incomingX(ui.cardSwapSign, size.width, ui.cardPop.value)
                    } else 0f
                    // The new card is composed one frame before its swap is set up: hide that frame instead of showing it at the old spot.
                    val fromKey = ui.pendingCardFromKey
                    val waitingForEnter = ui.pendingCardEnterSign != 0 && fromKey != null && fromKey != shownKeyNow
                    alpha = if (waitingForEnter) 0f else layout.expandedAlpha
                }
                .drawWithContent {
                    // Records the card only while a swipe can need its last frame (a drag, or a swipe waiting for its card).
                    val switched = ui.pendingCardEnterSign != 0 && ui.pendingCardFromKey != null &&
                        (ui.pendingCardFromKey != shownKeyNow || (drawnKey[0] != null && drawnKey[0] != ui.pendingCardFromKey))
                    // Only until the switch has been parked (a drag, or a swipe waiting for its card). Once the outgoing layer is parked the new card
                    // slides in drawn directly, not recorded into the other layer.
                    val recording = ui.cardDragX != 0f || ui.pendingCardEnterSign != 0
                    if (!recording) {
                        // Nothing recorded this frame: a layer left from an earlier swipe must not be parked later.
                        rec[0] = -1
                        recKey[0] = null
                        recKey[1] = null
                        drawContent()
                    } else {
                        // The layer written in the frame before the switch holds the old card: park it.
                        // Only a layer that really holds the old card (the pager may have drawn the new one a frame early).
                        if (rec[1] < 0 && switched && rec[0] >= 0) {
                            rec[1] = com.agupta07505.smartisland.model.SwipeGhost.layerToPark(rec[0], recKey, ui.pendingCardFromKey)
                        }
                        val idx = if (rec[1] >= 0) 1 - rec[1] else if (rec[0] >= 0) 1 - rec[0] else 0
                        val target = if (idx == 0) layerA else layerB
                        target.record { this@drawWithContent.drawContent() }
                        recKey[idx] = drawnKey[0] ?: shownKeyNow
                        // The pager can draw the new card before shownKey changes: until the swap is set up (the effect clears the pending
                        // sign) the new card is recorded but not drawn, and the parked old card stands in for it (the ghost below).
                        val pendingFrom = ui.pendingCardFromKey
                        val holdBack = ui.pendingCardEnterSign != 0 && pendingFrom != null &&
                            (pendingFrom != shownKeyNow || (drawnKey[0] != null && drawnKey[0] != pendingFrom))
                        if (!holdBack) drawLayer(target)
                        rec[0] = idx
                        rec[2] = size.width.toInt()
                        rec[3] = size.height.toInt()
                    }
                }
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalCardShown provides cardLive) {
            IslandExpandedContent(
                notifications = hostNotifications,
                launcherApps = launcherApps,
                selectedIndex = hostSelectedIndex,
                // A waiting card must not touch the selection or the auto-collapse timer.
                onPageSelected = { index -> if (cardLiveNow) onPageSelected(index) },
                onOpenNotification = onOpenNotification,
                onLaunchApp = onLaunchApp,
                onAppAction = onAppAction,
                onCollapse = onToggleExpanded,
                statusBarHeight = layout.topBarHeight.dp,
                // Each mode owns its natural height. The launcher already
                // supplies its own loading height and must not impose that
                // minimum on compact call or battery content.
                onHeightMeasured = onHeightMeasured,
                settings = settings,
                // While waiting, only "off" gets through: a waiting card never makes the window typeable or pauses the
                // auto-collapse, but its removal still clears both.
                onReplyStateChanged = { active -> if (!active || cardLiveNow) onReplyStateChanged(active) },
                onTextExpandedChanged = { active -> if (!active || cardLiveNow) onTextExpandedChanged(active) },
                snapPageSync = { ui.cardSwipeSnap },
                // Light look side bars: faded out by the drag, hidden during the swap, back in as the new card settles.
                sideBarsAlpha = {
                    val pop = ui.cardPop.value
                    when {
                        ui.pendingCardEnterSign != 0 -> 0f
                        ui.cardSwapSign != 0 -> ((pop - SIDE_BARS_BACK_FROM) / (1f - SIDE_BARS_BACK_FROM)).coerceIn(0f, 1f)
                        else -> 1f - (abs(ui.cardDragX) / sideBarsFadePx).coerceIn(0f, 1f)
                    }
                },
                drawnCardKey = drawnKey,
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
        // The outgoing card of a swipe: the parked layer, drawn where the old card stood, leaving on the swap's spring. No touch handling.
        if (cardLive && (ghostOn || waitingNow)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val w = rec[2]
                        val h = rec[3]
                        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                        this.layout(w, h) { placeable.place(0, 0) }
                    }
                    .graphicsLayer {
                        scaleX = layout.contentScale
                        scaleY = layout.contentScale
                        translationY = layout.contentSlideY.toPx()
                        // Waiting for the new card it stays in the middle; once the swap runs it leaves in the swipe's direction. The shell
                        // clips it, so it never shows outside the island's rounded shape.
                        translationX = if (ghostOn) {
                            com.agupta07505.smartisland.util.SwipeSideways.outgoingX(ui.cardSwapSign, size.width, ui.cardPop.value)
                        } else 0f
                        alpha = layout.expandedAlpha
                    }
                    .drawBehind {
                        val i = rec[1]
                        if (i >= 0) drawLayer(if (i == 0) layerA else layerB)
                    }
            )
        }
    }
}

/** Sideways drag (dp) at which the side bars are fully faded out. */
private const val SIDE_BARS_FADE_DP = 14f

/** Swap progress at which the side bars start fading back in. */
private const val SIDE_BARS_BACK_FROM = 0.85f
