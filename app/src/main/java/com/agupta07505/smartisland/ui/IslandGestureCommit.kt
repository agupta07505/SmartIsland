/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.State
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.PillSwipeDirection
import com.agupta07505.smartisland.model.ShellSwap
import com.agupta07505.smartisland.model.SwapClock
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeFeel
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.swipeChangesCard
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_EVENT_MS
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_MS
import com.agupta07505.smartisland.util.SwapTrace
import com.agupta07505.smartisland.util.SwipeCardOutcome
import com.agupta07505.smartisland.util.predictCardChange
import com.agupta07505.smartisland.util.swipeIsNoOp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun <T> swipeEaseBackSpring(): SpringSpec<T> =
    spring(dampingRatio = SwipeFeel.RETURN_DAMPING_RATIO, stiffness = SwipeFeel.EASE_BACK_STIFFNESS)

/** Cutout tap swap's spring (both shells and the cards move on it), following the relaxed / normal / snappy choice. */
internal fun tapSwapSpring(speed: AnimationSpeed, boost: Float = 1f): SpringSpec<Float> =
    spring(
        dampingRatio = SwipeFeel.TAP_SWAP_DAMPING_RATIO,
        stiffness = SwapClock.boosted(tapSwapStiffness(speed), boost)
    )

/** The swap spring's stiffness at the speed choice and system animation scale: what the swap's limits are counted in. */
internal fun tapSwapStiffness(speed: AnimationSpeed): Float = speed.stiffness(SwipeFeel.TAP_SWAP_STIFFNESS)

internal fun easeBackPillMotion(ui: IslandInteractionState, scope: CoroutineScope) {
    val content0 = if (ui.pendingEnterSign == 0) ui.pillContentDragX else 0f
    val shell0 = ui.pillDragOffsetX
    val lean0 = ui.dragOffset
    val pull0 = ui.swipeDownPull
    if (content0 == 0f && shell0 == 0f && lean0 == 0f && pull0 == 0f) return
    ui.pillReleaseJob?.cancel()
    ui.pillReleaseJob = scope.launch {
        Animatable(1f).animateTo(0f, swipeEaseBackSpring()) {
            val f = value
            if (content0 != 0f && ui.pendingEnterSign == 0) ui.pillContentDragX = content0 * f
            ui.pillDragOffsetX = shell0 * f
            ui.dragOffset = lean0 * f
            ui.swipeDownPull = pull0 * f
        }
        if (content0 != 0f && ui.pendingEnterSign == 0) ui.pillContentDragX = 0f
        ui.pillDragOffsetX = 0f
        ui.dragOffset = 0f
        ui.swipeDownPull = 0f
    }
}

internal const val PILL_SETTLE_GUARD_MS = 600L

/**
 * Safety net for the release ease: if the collapsed pill's lean, lift or pull is still off 600 ms after the finger
 * lifted (nothing touching it), ease it back
 */
internal fun schedulePillSettleGuard(ui: IslandInteractionState, scope: CoroutineScope) {
    ui.pillSettleGuardJob?.cancel()
    ui.pillSettleGuardJob = scope.launch {
        delay(PILL_SETTLE_GUARD_MS)
        if (ui.pillTouchActive) return@launch
        val off = ui.pillDragOffsetX != 0f || ui.dragOffset != 0f || ui.swipeDownPull != 0f ||
            (ui.pendingEnterSign == 0 && ui.pillContentDragX != 0f)
        if (off) {
            AppLogRecorder.w(
                "PillLean",
                "release ease left the pill off: lean=${ui.pillDragOffsetX} y=${ui.dragOffset} " +
                    "content=${ui.pillContentDragX} pull=${ui.swipeDownPull}"
            )
            easeBackPillMotion(ui, scope)
        }
    }
}

internal fun <T> swipeCalmSpring(): SpringSpec<T> =
    spring(dampingRatio = SwipeFeel.RETURN_DAMPING_RATIO, stiffness = SwipeFeel.RETURN_STIFFNESS)

internal fun expandedSwipeFires(direction: PillSwipeDirection?, stepsActive: Boolean, wiring: IslandWiring): Boolean {
    if (direction == null) return false
    // preview, reader and picture steps: only swipe up and down mean something
    if (stepsActive) return direction == PillSwipeDirection.Up || direction == PillSwipeDirection.Down
    return when (direction) {
        PillSwipeDirection.Left, PillSwipeDirection.Right -> {
            val action = expandedSideAction(
                wiring.view.settings, if (direction == PillSwipeDirection.Left) -1f else 1f
            )
            action != SwipeAction.None && !swipeIsNoOp(action, wiring.view.notifications, wiring.view.selectedIndex)
        }
        else -> true
    }
}

internal fun commitPillSwipe(
    direction: PillSwipeDirection,
    dragX: Float,
    wiring: IslandWiring,
    runner: IslandActionRunner,
    ui: IslandInteractionState,
    scope: CoroutineScope,
    maxSlidePx: Float
) {
    val settings = wiring.view.settings
    ui.swipeTouched()
    val action = when (direction) {
        PillSwipeDirection.Left -> SwipeAction.fromId(settings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
        PillSwipeDirection.Right -> SwipeAction.fromId(settings.pillSwipeRightAction, SwipeAction.NextNotification)
        PillSwipeDirection.Up -> SwipeAction.fromId(settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
        PillSwipeDirection.Down -> FIXED_PILL_SWIPE_DOWN_ACTION
    }
    val sideways = direction == PillSwipeDirection.Left || direction == PillSwipeDirection.Right
    if (sideways && wiring.view.notifications.isEmpty() && ui.idleWidgetCycle != null) {
        val changed = ui.idleWidgetCycle?.invoke(direction == PillSwipeDirection.Left) == true
        if (changed) ui.pendingEnterSign = if (dragX < 0f) 1 else -1
        scope.launch {
            if (changed) delay(CARD_ENTER_WAIT_MS)
            if (!changed || ui.pendingEnterSign != 0) {
                ui.pendingEnterSign = 0
                val start = ui.pillContentDragX
                Animatable(start).animateTo(
                    0f, swipeEaseBackSpring()
                ) { ui.pillContentDragX = value }
            }
        }
        return
    }
    if (!settings.enablePillSwipeActions) return
    if (sideways && swipeChangesCard(action)) {
        ui.lastCardSwipeMs = System.currentTimeMillis()
        ui.pendingEnterSign = if (dragX < 0f) 1 else -1
        scope.launch {
            delay(CARD_ENTER_WAIT_MS)
            if (ui.pendingEnterSign != 0) {
                ui.pendingEnterSign = 0
                val start = ui.pillContentDragX
                Animatable(start).animateTo(
                    0f, swipeEaseBackSpring()
                ) { ui.pillContentDragX = value }
            }
        }
    }
    runner.run(action, wiring.view.notifications.getOrNull(wiring.view.selectedIndex))
}

internal const val EXPANDED_SWIPE_SLIDES_IN = true

internal const val PILL_SWIPE_TOUCH_GAP_MS = 250L

internal const val SWIPE_SWITCH_BOUNCE_SKIP_MS = 600L

internal fun expandedSideAction(settings: SmartIslandSettings, dragX: Float): SwipeAction =
    SwipeAction.fromId(if (dragX < 0f) settings.swipeLeftAction else settings.swipeRightAction, SwipeAction.None)

internal fun commitExpandedSwipe(
    action: SwipeAction,
    dragX: Float,
    notification: IslandNotification?,
    runner: IslandActionRunner,
    ui: IslandInteractionState,
    scope: CoroutineScope,
    cards: List<IslandNotification>,
    index: Int
) {
    // a sibling swipe with no sibling does nothing at all (no buzz either).
    if (swipeIsNoOp(action, cards, index)) return
    val outcome = if (swipeChangesCard(action)) predictCardChange(action, cards, index) else SwipeCardOutcome.Stays
    if (outcome != SwipeCardOutcome.Stays && !EXPANDED_SWIPE_SLIDES_IN) {
        ui.cardBackJob?.cancel()
        ui.cardWaitJob?.cancel()
        ui.pendingCardEnterSign = 0
        ui.pendingCardFromKey = null
        ui.cardDragX = 0f
        ui.cardSwipeSnap = true
        ui.cardWaitJob = scope.launch {
            delay(CARD_ENTER_WAIT_MS)
            ui.cardSwipeSnap = false
        }
        runner.run(action, notification)
        return
    }
    if (outcome != SwipeCardOutcome.Stays) {
        ui.pendingCardEnterSign = if (dragX < 0f) 1 else -1
        ui.pendingCardFromKey = notification?.key
        ui.lastCardSwipeMs = System.currentTimeMillis()
        ui.cardSwipeSnap = true
        // next / previous event may import from the shade first
        val waitMs = if (outcome == SwipeCardOutcome.Unknown) CARD_ENTER_WAIT_EVENT_MS else CARD_ENTER_WAIT_MS
        ui.cardWaitJob?.cancel()
        ui.cardWaitJob = scope.launch {
            delay(waitMs)
            if (ui.pendingCardEnterSign != 0) {
                ui.pendingCardEnterSign = 0
                ui.pendingCardFromKey = null
                ui.cardSwipeSnap = false
                easeCardBack(ui, scope)
            }
        }
    }
    runner.run(action, notification)
}

internal fun easeCardBack(ui: IslandInteractionState, scope: CoroutineScope) {
    val start = ui.cardDragX
    ui.cardBackJob?.cancel()
    ui.cardBackJob = scope.launch {
        Animatable(start).animateTo(
            targetValue = 0f,
            animationSpec = swipeEaseBackSpring()
        ) {
            if (ui.pendingCardEnterSign == 0) ui.cardDragX = value
        }
    }
}

internal fun beginTapCardSwap(
    ui: IslandInteractionState,
    scope: CoroutineScope,
    fromKey: String,
    forward: Boolean,
    select: () -> Unit
) {
    ui.cardBackJob?.cancel()
    ui.cardWaitJob?.cancel()
    ui.pendingCardEnterSign = if (forward) 1 else -1
    ui.pendingCardFromKey = fromKey
    ui.lastCardSwipeMs = System.currentTimeMillis()
    ui.cardSwipeSnap = true
    ui.cardWaitJob = scope.launch {
        // Record the outgoing card in a frame drawn while the swap is pending, before the key changes (the ghost
        // shell carries it). One wait can resume in the tap's own frame and skip that, giving a plain crossfade;
        // the second wait leaves a whole frame in between
        withFrameNanos { }
        withFrameNanos { }
        select()
        // A heavy card (artwork, widgets) can take longer than a plain one to compose: the wait of an unknown event
        delay(CARD_ENTER_WAIT_EVENT_MS)
        if (ui.pendingCardEnterSign != 0) {
            ui.pendingCardEnterSign = 0
            ui.pendingCardFromKey = null
            ui.cardSwipeSnap = false
            // No new card came: nothing to dip for
            AppLogRecorder.w("SwapSkip", "no new card within ${CARD_ENTER_WAIT_EVENT_MS}ms: ${ui.swapTraceState()}")
            ui.tapMemory.clear()
            ui.endTapSwap("no-card")
        }
    }
}

internal fun beginTapSwap(
    ui: IslandInteractionState,
    scope: CoroutineScope,
    fromKey: String,
    toKey: String,
    forward: Boolean,
    shell: Rect,
    cutout: Rect,
    selectOld: () -> Unit,
    selectNew: () -> Unit
) {
    ui.shellSwap = ShellSwap.tap(
        ShellSwap.State(expanded = fromKey), toKey
    )
    ui.pendingCutoutBounds = null
    ui.tapSwapFromKey = fromKey
    ui.tapSwapToKey = toKey
    ui.tapSwapSelectOld = selectOld
    ui.tapSwapSelectNew = selectNew
    ui.tapSwapDx = cutout.center.x - shell.center.x
    ui.tapSwapDy = cutout.center.y - shell.center.y
    ui.tapSwapScaleX = TapSwap.minScale(cutout.width, shell.width)
    ui.tapSwapScaleY = TapSwap.minScale(cutout.height, shell.height)
    ui.tapSwapShellRect = shell
    ui.tapSwapCutoutRect = cutout
    ui.tapSwapHandoffP = 0f
    ui.swapBurst.tap(SystemClock.uptimeMillis())
    ui.beginSwapPhase()
    SwapTrace.log("start") { "from=$fromKey to=$toKey cutout=${ui.tapSwapCutoutId} ${ui.swapTraceState()}" }
    armTapSwapGuard(ui, scope)
    // The shell stays as it is (the outgoing card on it) while the incoming card is composed; the swap's 1 spring
    // starts when the outgoing card is parked (see runCardSwap)
    beginTapCardSwap(ui, scope, fromKey, forward, selectNew)
}

private fun armTapSwapGuard(ui: IslandInteractionState, scope: CoroutineScope) {
    ui.tapSwapGuardJob?.cancel()
    ui.tapSwapGuardJob = scope.launch {
        delay(SwapClock.watchdogMs(tapSwapStiffness(ui.tapSwapSpeed)))
        ui.tapSwapGuardJob = null
        if (!ui.tapSwapActive) return@launch
        ui.tapMemory.clear()
        ui.pendingCardEnterSign = 0
        ui.pendingCardFromKey = null
        ui.cardSwipeSnap = false
        ui.abortTapSwap("watchdog")
    }
}

/**
 * Carries out what a tap during a swap asks for ([ShellSwap.intentOf]): a flip that the swap cannot take (it is
 * settling) is remembered as the newest tap instead. True when the swap was turned round (or the touch was already
 * counted for it)
 */
internal fun tapDuringSwap(
    ui: IslandInteractionState,
    scope: CoroutineScope,
    intent: ShellSwap.Intent,
    fromCutout: Boolean
): Boolean = when (intent) {
    ShellSwap.Intent.None -> false
    ShellSwap.Intent.Flip -> flipTapSwap(ui, scope, fromCutout).also { if (!it) ui.rememberOppositeTap() }
    is ShellSwap.Intent.Third -> false.also { ui.rememberThirdTap(intent.after) }
}

/** A touch while a tap swap moves turns it around, so quick touches flip it back and forth. */
internal fun flipTapSwap(ui: IslandInteractionState, scope: CoroutineScope, fromCutout: Boolean): Boolean {
    if (ui.tapSwapActive && fromCutout && ui.swapTouch.claimClick()) {
        SwapTrace.log("flip-same-touch") { ui.swapTraceState() }
        return true
    }
    if (ui.tapSwapActive && ui.pendingCardEnterSign != 0) {
        // The shell hasn't moved yet. Replay the return tap once the first card lands.
        ui.rememberOppositeTap()
        SwapTrace.log("flip-queued") { ui.swapTraceState() }
        return true
    }
    if (!TapSwap.canFlip(ui.tapSwapActive, ui.swap.refusesFlip, ui.tapSwapP())) {
        if (ui.tapSwapActive) SwapTrace.log("flip-refused") { "cutout=$fromCutout ${ui.swapTraceState()}" }
        return false
    }
    ui.swap.turnAround()
    ui.swapBurst.tap(SystemClock.uptimeMillis())
    SwapTrace.log("flip") { "cutout=$fromCutout ${ui.swapTraceState()}" }
    // The model follows the direction (a remembered tap is dropped, as a tap on the shrinking event does)
    ui.shellSwap = ui.shellSwap?.let { ShellSwap.turnedAround(it) }
    armTapSwapGuard(ui, scope)
    scope.launch { ui.cardPop.stop() }
    return true
}
