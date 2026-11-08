/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.IdleDotGeometry
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.EdgePillRules
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.ShellSwap
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TapZone
import com.agupta07505.smartisland.model.cutoutTouchTarget
import com.agupta07505.smartisland.model.portraitCenterTapIsIdleControl
import com.agupta07505.smartisland.util.SwapTrace
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_MS
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@NonRestartableComposable
@Composable
internal fun BoxScope.IdleTouchTargets(
    settings: SmartIslandSettings,
    layout: IslandLayout,
    expanded: Boolean,
    isLandscape: Boolean,
    hasCards: Boolean,
    pickerSideBySide: Boolean,
    cutoutArea: CutoutArea?,
    centreOverlapsCutout: Boolean,
    landscapeDot: IdleDotGeometry?,
    edgeLayoutOn: Boolean,
    edgeHideRunning: Boolean,
    edgeExiting: Boolean,
    closingOntoIdle: () -> Boolean,
    pillExit: PillExitState,
    secondary: IslandSecondary,
    ui: IslandInteractionState,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    scope: CoroutineScope,
    context: Context
) {
    // portrait: no separate idle pill
    val closeZone = rememberExpandedCloseZone(settings, isLandscape, layout.cameraBand,
        layout.collapsedMainOffset.value)
    // collapsed, centre band clear of the camera cutout
    val cutoutTarget = cutoutArea?.let { cutoutTouchTarget(it) }
    CutoutLongPressTarget(
        enabled = !expanded && !isLandscape && cutoutTarget != null && !centreOverlapsCutout,
        width = (cutoutTarget?.widthDp ?: 0f).dp,
        height = (cutoutTarget?.heightDp ?: 0f).dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .graphicsLayer {
                translationX = (cutoutTarget?.centreXDp ?: 0f).dp.toPx()
                val windowTopDp = if (settings.enableNotchMode) 0f else settings.yOffset
                translationY = ((cutoutTarget?.topDp ?: 0f) - windowTopDp).coerceAtLeast(0f).dp.toPx()
            },
        onLongPress = {
            ui.touch()
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Hold)
            val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
            if (longPress != SwipeAction.None) {
                runner.runLongPress(longPress)
            }
        }
    )
    PortraitPillCenterTarget(
        enabled = expanded && portraitCenterTapIsIdleControl(
            isLandscape = isLandscape,
            notchMode = settings.enableNotchMode,
            hasCards = hasCards
        ) && !pickerSideBySide,
        zoneWidth = closeZone.widthDp.dp,
        zoneHeight = closeZone.heightDp.dp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .graphicsLayer {
                // On a detected camera cutout: its own spot
                translationX = (closeZone.offsetXDp?.dp ?: layout.collapsedMainOffset).toPx()
                translationY = closeZone.topDp.dp.toPx()
            },
        onTap = {
            ui.touch()
            val intent = routeIslandTap(ui, wiring, scope, TapZone.CameraBand)
        },
        onLongPress = {
            ui.touch()
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Hold)
            val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
            if (longPress != SwipeAction.None) {
                runner.runLongPress(longPress)
            }
        }
    )

    // Landscape idle dot; portrait has none, its pill centre is the control
    LandscapeIdleDotView(
        dot = landscapeDot,
        alpha = {
            if (edgeHideRunning) 0f
            else if (closingOntoIdle() || EdgePillRules.idleDotStandsIn(edgeExiting, edgeHideRunning)) 1f
            // Edge layout with the pill on screen
            else if (edgeLayoutOn) 0f
            else pillExit.dotAlpha(secondary.idleDot.alpha)
        },
        topShiftDp = { layout.topShift.value },
        color = layout.pillBackgroundColor,
        onTap = {
            ui.touch()
            wiring.actions.idle.dotTap()
        },
        onLongPress = {
            ui.touch()
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Hold)
            val longPress = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill)
            if (longPress != SwipeAction.None) {
                runner.runLongPress(longPress)
            }
        }
    )
}

@Composable
internal fun rememberCardSelect(
    ui: IslandInteractionState,
    scope: CoroutineScope,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    safeIndex: Int,
    onPageSelected: (Int) -> Unit
): (Int) -> Unit {
    val pageExpandedNow = rememberUpdatedState(expanded)
    val pageCardsNow = rememberUpdatedState(notifications)
    val pageIndexNow = rememberUpdatedState(safeIndex)
    val pageSelectNow = rememberUpdatedState(onPageSelected)
    val selectFromPill: (Int) -> Unit = remember(ui, scope) {
        lateinit var again: (Int) -> Unit
        again = select@{ index ->
            val cards = pageCardsNow.value
            val from = pageIndexNow.value
            val fromKey = cards.getOrNull(from)?.key
            if (ui.tapSwapActive) {
                val tappedKey = cards.getOrNull(index)?.key
                tapDuringSwap(ui, scope, ShellSwap.intentOf(ui.shellSwap, tappedKey), true)
                return@select
            }
            if (TapSwap.heldByFlight(
                    pageExpandedNow.value, ui.flightHold.flying, from, index, cards.size
                )
            ) {
                ui.flightHold.hold(cards[index].key)
                return@select
            }
            if (TapSwap.heldByPicker(pageExpandedNow.value, ui.pickerMotion.moving, from, index, cards.size)) {
                // The picker's trade with the card finishes first, then the tap is taken as it stands then
                ui.pickerWaitJob?.cancel()
                ui.pickerWaitJob = scope.launch {
                    snapshotFlow { ui.pickerMotion.moving }.first { !it }
                    again(index)
                }
                return@select
            }
            if (!pageExpandedNow.value || fromKey == null || index == from || index !in cards.indices) {
                if (index != from) {
                    AppLogRecorder.w(
                        "SwapSkip", "in place: expanded=${pageExpandedNow.value} fromKey=${fromKey != null} index=$index"
                    )
                }
                pageSelectNow.value(index)
            } else if (ui.pendingCardEnterSign != 0 || ui.cardDragX != 0f) {
                // A card change is still moving: the tap runs once it has finished, as a normal tap
                val tappedKey = cards[index].key
                ui.tapReplayJob?.cancel()
                ui.tapReplayJob = scope.launch {
                    val finished = withTimeoutOrNull(CARD_ENTER_WAIT_MS * 3) {
                        snapshotFlow { ui.pendingCardEnterSign == 0 && ui.cardDragX == 0f }.first { it }
                    } != null
                    val target = pageCardsNow.value.indexOfFirst { it.key == tappedKey }
                    if (target < 0 || !pageExpandedNow.value || target == pageIndexNow.value) return@launch
                    ui.cutoutBoundsNow?.invoke()?.let { ui.cutoutBoundsWin = it }
                    if (finished) again(target) else {
                        AppLogRecorder.w("SwapSkip", "in place: earlier card change did not finish in time")
                        pageSelectNow.value(target)
                    }
                }
            } else {
                val shell = ui.shellBoundsWin
                val cutout = ui.cutoutBoundsWin
                val swap = ui.tapSwapAllowed && shell != null && cutout != null && shell.width > 1f && shell.height > 1f
                    &&
                    TapSwap.eligible(
                        true, fromKey, from, index, cards.size, ui.pendingCardEnterSign != 0, ui.cardDragX,
                            ui.tapSwapActive
                    )
                if (swap) {
                    val toKey = cards[index].key
                    val pick = { key: String ->
                        val i = pageCardsNow.value.indexOfFirst { it.key == key }
                        SwapTrace.log("pick") { "key=$key index=$i size=${pageCardsNow.value.size}" }
                        if (i >= 0) pageSelectNow.value(i)
                    }
                    beginTapSwap(
                        ui, scope, fromKey, toKey, index > from, shell!!, cutout!!,
                        selectOld = { pick(fromKey) }, selectNew = { pick(toKey) }
                    )
                } else {
                    // No swap possible (2 islands side by side, no bounds): the card changes in place
                    // TODO: sometimes lands here on a normal tap and the cards blink instead of swapping; find which
                    // SwapSkip case it is (bounds missing, swap still running) and fix that one
                    AppLogRecorder.w(
                        "SwapSkip",
                        "in place: allowed=${ui.tapSwapAllowed} shell=${shell != null} cutout=${cutout != null} " +
                            "shellSize=${shell?.width}x${shell?.height} state=${ui.swapTraceState()}"
                    )
                    ui.cardSwipeSnap = true
                    pageSelectNow.value(index)
                    scope.launch {
                        delay(CARD_ENTER_WAIT_MS)
                        if (ui.pendingCardEnterSign == 0) ui.cardSwipeSnap = false
                    }
                }
            }
        }
        again
    }
    ui.onPickerTapDuringSwap = { id ->
        if (TapSwap.hidesWithSwap(ui.tapSwapActive, ui.tapSwapCutoutId, id)) {
            tapDuringSwap(ui, scope, ShellSwap.Intent.Flip, true)
        }
    }
    // Two shells: a third event tapped during a swap starts its own swap once the running one has settled.
    ui.onSwapSettled = settled@{
        val next = ui.shellSwap?.let { ShellSwap.nextAfterSettle(it) }
        ui.shellSwap = null
        val pendingId = ui.pendingCutoutId
        val pendingBounds = ui.pendingCutoutBounds
        ui.pendingCutoutBounds = null
        val remembered = ui.tapMemory.take()
        if (next == null) {
            if (remembered != null) {
                scope.launch {
                    withFrameNanos { }
                    withFrameNanos { }
                    val index = pageCardsNow.value.indexOfFirst { it.key == remembered }
                    if (index < 0 || !pageExpandedNow.value || ui.tapSwapActive
                        || index == pageIndexNow.value) return@launch
                    ui.cutoutBoundsNow?.invoke()?.let { ui.cutoutBoundsWin = it }
                    selectFromPill(index)
                }
            }
            return@settled
        }
        scope.launch {
            // shell and cutouts are measured again at rest before the next swap reads them
            withFrameNanos { }
            withFrameNanos { }
            val index = pageCardsNow.value.indexOfFirst { it.key == next }
            if (index < 0 || !pageExpandedNow.value || ui.tapSwapActive || index == pageIndexNow.value) return@launch
            if (pendingBounds != null) ui.markCutout(pendingId, pendingBounds)
            selectFromPill(index)
        }
    }
    ui.onFlightLanded = { key ->
        val index = TapSwap.replayIndex(
            key, pageCardsNow.value.map { it.key }, pageIndexNow.value
        )
        if (index != null && pageExpandedNow.value) {
            ui.cutoutBoundsNow?.invoke()?.let { ui.cutoutBoundsWin = it }
            selectFromPill(index)
        }
    }
    return selectFromPill
}

@NonRestartableComposable
@Composable
internal fun WireInteractionCallbacks(
    ui: IslandInteractionState,
    onUserEngaged: () -> Unit,
    onSwipeActive: () -> Unit,
    onDragActive: () -> Unit,
    onHandleHeld: (Boolean) -> Unit,
    onHandleTap: () -> Unit,
    onTickPillHold: (Boolean) -> Unit
) {
    val onUserEngagedState = rememberUpdatedState(onUserEngaged)
    ui.onUserTouch = { onUserEngagedState.value() }
    val onSwipeActiveState = rememberUpdatedState(onSwipeActive)
    ui.onSwipe = { onSwipeActiveState.value() }
    val onDragActiveState = rememberUpdatedState(onDragActive)
    ui.onDragActive = { onDragActiveState.value() }
    val onHandleHeldState = rememberUpdatedState(onHandleHeld)
    ui.onHandleHeld = { held -> onHandleHeldState.value(held) }
    val onHandleTapState = rememberUpdatedState(onHandleTap)
    ui.onHandleTap = { onHandleTapState.value() }
    val onTickPillHoldState = rememberUpdatedState(onTickPillHold)
    ui.onTickHold = { hold -> onTickPillHoldState.value(hold) }
}
