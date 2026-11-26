/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CameraBand
import com.agupta07505.smartisland.model.CameraSide
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.CutoutTapIntent
import com.agupta07505.smartisland.model.GestureSensitivity
import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.ShellSwap
import com.agupta07505.smartisland.model.SideCamera
import com.agupta07505.smartisland.model.SlotSide
import com.agupta07505.smartisland.model.StepDrag
import com.agupta07505.smartisland.model.StepDragHaptic
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.SwipeArming
import com.agupta07505.smartisland.model.SwipeSideways
import com.agupta07505.smartisland.model.TapGuard
import com.agupta07505.smartisland.model.TapZone
import com.agupta07505.smartisland.model.TorchCard
import com.agupta07505.smartisland.model.cameraBandFor
import com.agupta07505.smartisland.model.inMovedPill
import com.agupta07505.smartisland.model.longPressOpensApps
import com.agupta07505.smartisland.model.pillCenterTapHit
import com.agupta07505.smartisland.model.pillTapZone
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsTouch
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.SwapTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun Modifier.islandMainGestures(
    settings: SmartIslandSettings,
    isInputActive: Boolean,
    isLandscape: Boolean,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    runner: IslandActionRunner,
    scope: CoroutineScope,
    context: Context,
    pickerLeftFraction: State<Float>,
    pickerOnLeft: State<Boolean>,
    centreOverlapsCutout: Boolean = false,
    pillHasContent: Boolean = true,
    cameraBand: CameraBand? = null,
    /** Which side of the camera the glyph strip is on (see [cameraBand]). */
    slotSide: SlotSide = SlotSide.Both,
    cameraSide: CameraSide = CameraSide.Centre,
    /** How far the wide pulse has moved the pill's centre right of the settled one (dp), read when a touch lands. */
    pulseCentreShiftDp: () -> Float = { 0f },
    /** An event pulses on the pill now, read when a touch lands: a hold anywhere on it is the settle gesture. */
    pulseRunning: () -> Boolean = { false }
): Modifier {
    val displayMetrics = context.resources.displayMetrics
    return this
        .onGloballyPositioned { ui.gestureOrigin = it.positionInWindow() }
        .composed {
            // Sample the latest hit zones at touch-down. A layout change needn't restart pointer input.
            val live = rememberUpdatedState(
                GestureGeometry(centreOverlapsCutout, pillHasContent, cameraBand, slotSide, cameraSide)
            )
            val pulseRunningNow = rememberUpdatedState(pulseRunning)
            pointerInput(displayMetrics.density, isInputActive, isLandscape) {
                if (isInputActive) {
                    // Let the reply field handle its own touches. An unconsumed card tap can still clear selection.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val selecting = wiring.view.textSelecting
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        if (selecting && (up.position - down.position).getDistance() <= viewConfiguration.touchSlop) {
                            wiring.actions.card.clearTextSelection()
                        }
                    }
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (ui.handleOwnsTouch) {
                        // The floating-window handle owns this touch
                        waitForAllUp()
                        return@awaitEachGesture
                    }
                    ui.userTouched()
                    if (ui.tapSwapActive) {
                        // The open picker's rows and header wait for the swap to settle, they take no part in it
                        val onPicker = wiring.view.expanded && siblingPickerOwnsTouch(
                            wiring.view.siblingPickerOpen, isLandscape,
                            if (size.width > 0) down.position.x / size.width else 0f,
                            pickerLeftFraction.value, pickerOnLeft.value
                        )
                        if (onPicker) {
                            SwapTrace.log("touch-inert") { ui.swapTraceState() }
                            ui.swapTouch.down(flipped = false)
                        } else {
                            // Cleared by the next touch, not at release: the click can come after it
                            ui.swapTouch.down(flipped = tapDuringSwap(ui, scope, ShellSwap.Intent.Flip, false))
                        }
                        waitForAllUp()
                        return@awaitEachGesture
                    }
                    if (reverseShellOnTouch(down, settings, wiring, ui, displayMetrics.density)) return@awaitEachGesture
                    // a new touch takes the card over
                    ui.cardBackJob?.cancel()
                    ui.cardWaitJob?.cancel()
                    ui.pillReleaseJob?.cancel()
                    ui.pillSettleGuardJob?.cancel()
                    ui.pillTouchActive = true
                    val geometry = live.value
                    val touch = TouchState(
                        settings, isLandscape, wiring, ui, runner, scope, context, pickerLeftFraction, pickerOnLeft,
                        geometry.centreOverlapsCutout, geometry.pillHasContent, geometry.cameraBand,
                        geometry.slotSide, geometry.cameraSide,
                        downPosition = down.position, shellWidthPx = size.width.toFloat(),
                        pulseCentreShiftDp = pulseCentreShiftDp(),
                        pulseRunning = pulseRunningNow.value()
                    )
                    ui.cardSlideMaxPx = SwipeSideways.maxLeanPx(displayMetrics.density)
                    val holdsPulse = !touch.wasExpandedAtStart
                    if (holdsPulse) wiring.actions.pulse.touch(true)
                    touch.startHold()
                    val pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (touch.onPointerChange(change)) break
                        }
                        touch.endLoop()
                    } finally {
                        touch.closeTouch()
                        if (holdsPulse) wiring.actions.pulse.touch(false)
                    }
                    touch.finishGesture()
                }
            }
        }
}

private data class GestureGeometry(
    val centreOverlapsCutout: Boolean,
    val pillHasContent: Boolean,
    val cameraBand: CameraBand?,
    val slotSide: SlotSide,
    val cameraSide: CameraSide
)

private suspend fun AwaitPointerEventScope.waitForAllUp() {
    do {
        val event = awaitPointerEvent(PointerEventPass.Final)
    } while (event.changes.any { it.pressed })
}

/** A touch on a shell that is mid-turn reverses it, returns true when it took the touch. */
private suspend fun AwaitPointerEventScope.reverseShellOnTouch(
    down: PointerInputChange,
    settings: SmartIslandSettings,
    wiring: IslandWiring,
    ui: IslandInteractionState,
    density: Float
): Boolean {
    val phaseAtDown = ui.islandPhase(wiring.view.expanded)
    if (!CutoutTapIntent.reversesOnShellTouch(phaseAtDown, ui.expandProgressNow(), down.position.y,
        settings.height * density)) {
        return false
    }
    val turnSlop = GestureSensitivity.fromId(wiring.view.settings.gestureSensitivity).scaled(10f) * density
    val turnGuard = TapGuard(turnSlop)
    do {
        val turnEvent = awaitPointerEvent(PointerEventPass.Final)
        turnEvent.changes.firstOrNull { it.id == down.id }?.let {
            turnGuard.update(it.position.x - down.position.x, it.position.y - down.position.y)
        }
    } while (turnEvent.changes.any { it.pressed })
    if (!turnGuard.exceeded) {
        val turnIntent = CutoutTapIntent.resolve(
            phaseAtDown, TapZone.Card, wiring.view.notifications.isNotEmpty()
        )
        CutoutTapIntent.targetExpanded(turnIntent)?.let { wiring.actions.expand.setExpanded(it) }
    }
    return true
}

internal class TouchState(
    val settings: SmartIslandSettings,
    val isLandscape: Boolean,
    val wiring: IslandWiring,
    val ui: IslandInteractionState,
    val runner: IslandActionRunner,
    val scope: CoroutineScope,
    val context: Context,
    val pickerLeftFraction: State<Float>,
    val pickerOnLeft: State<Boolean>,
    val centreOverlapsCutout: Boolean,
    val pillHasContent: Boolean,
    cameraBand: CameraBand?,
    val slotSide: SlotSide,
    cameraSide: CameraSide,
    val downPosition: Offset,
    val shellWidthPx: Float,
    pulseCentreShiftDp: Float = 0f,
    val pulseRunning: Boolean = false
) {
    val density = context.resources.displayMetrics.density

    /** A text was already selected when this touch landed: a tap drops it and does nothing else. */
    val selectingAtStart = wiring.view.textSelecting
    val sensitivity = GestureSensitivity.fromId(wiring.view.settings.gestureSensitivity)
    val tapSlop = sensitivity.scaled(10f) * density
    private val band = cameraBand ?: cameraBandFor(settings.width, null, 0f)
    val bandShiftPx = if (wiring.view.expanded) band.shiftDp * density else {
        SideCamera.bandShiftInShellDp(
            cameraSide, band.inMovedPill(pulseCentreShiftDp).shiftDp, shellWidthPx / density, settings.width
        ) * density
    }
    val bandWidthPx = band.widthDp * density
    private val downOnCameraBand = !isLandscape && !wiring.view.expanded &&
        abs(downPosition.x - (shellWidthPx / 2f + bandShiftPx)) <= bandWidthPx / 2f
    private val bandSidewaysSlopPx = sensitivity.scaled(TapGuard.CAMERA_BAND_SIDEWAYS_SLOP_DP) * density
    val tapGuard = TapGuard(tapSlop, if (downOnCameraBand) minOf(tapSlop, bandSidewaysSlopPx) else tapSlop)
    val pillThresholdPx = sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP) * density

    // The collapsed pill never locks
    val pillArming = SwipeArming(sensitivity, pillThresholdPx).also { it.lockWhen = { false } }
    val expandedArming = SwipeArming(
        sensitivity, sensitivity.scaled(SWIPE_THRESHOLD_DP) * density, disarmFraction = 1f,
        lock = ui.swipeLock,
        horizontalThreshold = sensitivity.scaled(EXPANDED_SIDE_SWIPE_THRESHOLD_DP) * density
    )
    val maxSlidePx = pillContentMaxSlideDp(settings.width) * density
    var crossedOnce = false
    val pressTimeMs = System.currentTimeMillis()
    val wasExpandedAtStart = wiring.view.expanded

    /** The open card is the torch card: it cannot be moved, only a swipe up set to Collapse closes it. */
    val torchOpenAtStart = TorchCard.open(wiring.view.notifications.getOrNull(wiring.view.selectedIndex)?.mode, wasExpandedAtStart)

    val pickerOpenAtStart = wiring.view.siblingPickerOpen
    val readingTextAtStart = wiring.view.readingExpandedText
    val launcherAtStart = wasExpandedAtStart && wiring.view.settings.enableAppShortcuts &&
        wiring.view.notifications.isEmpty()
    val readerTracker = ui.readerScroll
    val readerStartedOnScroll = wasExpandedAtStart && readerTracker.touchStartsOnScroll(SystemClock.uptimeMillis())
    val readerMovesAtDown = readerTracker.moveCount

    // preview, reader or an open picture
    val stepsActive = wasExpandedAtStart && wiring.view.settings.enableSwipeActions &&
        wiring.view.cardDepth != CardDepth.First

    init {
        expandedArming.lockWhen = { dir -> expandedSwipeFires(dir, stepsActive, wiring) }
    }

    // Landscape image selector: no tap
    val selectorLocked = wasExpandedAtStart && wiring.view.cardDepth == CardDepth.Selector
    val cardFollows = wasExpandedAtStart && wiring.view.settings.enableSwipeActions && !stepsActive &&
        !pickerOpenAtStart
    val downYDp = downPosition.y / density
    val downXFraction = if (shellWidthPx > 0f) downPosition.x / shellWidthPx else 0f

    // Down landed in the lock after a swap (or in one): a tap meant for the cutout must not open the app on release
    val cardLockedAtStart = wasExpandedAtStart && ui.cardTouchLocked(SystemClock.uptimeMillis())

    // The touch landed on the open sibling picker (rows or header)
    val pickerTouchAtStart = wasExpandedAtStart && siblingPickerOwnsTouch(
        pickerOpenAtStart, isLandscape, downXFraction, pickerLeftFraction.value, pickerOnLeft.value
    )
    private var deadShown = false
    var isHoldRegistered = false

    // The apps shortcut is armed by the hold, but opens only when the finger lifts
    var appsShortcutPending = false

    // A hold on the glyph keeps the touch open: lift opens the apps shortcut, a swipe pulses the current card again
    var glyphHoldArmed = false
    var glyphSwiped = false
    var dragAccumulatorY = 0f
    var dragAccumulatorX = 0f
    val axisLock = AxisLock(tapSlop)
    var rawDragX = 0f
    var rawDragY = 0f
    var isDragging = false
    var pillGestureTriggered = false

    // Set when a child (the expanded notification text) scrolls vertically
    var childScrolled = false

    // How far the finger moved while the reader's own scroll had first claim on the drag
    var readerProbeX = 0f
    var readerProbeY = 0f

    // a vertical drag opens (or closes) a card step WITH the finger. 0 = not started
    var stepMode = 0

    // One buzz per step drag: at the far end of the drag
    val stepHaptic = StepDragHaptic()
    val stepRangePx = StepDrag.RANGE_DP * density
    var stepTarget = 0f
    var stepSmoothJob: Job? = null
    var pillOpenMode = 0
    var pillOpenRangePx = 0f
    var pillOpenArmed = false

    // Swipe up set to Collapse: the open island shrinks with the finger. 0 = undecided, 1 = following, 2 = off
    var pillCloseMode = 0
    var pillCloseRangePx = 0f
    var pillCloseArmed = false
    val pillCloseThresholdPx = sensitivity.scaled(SWIPE_THRESHOLD_DP) * density

    /** Idle widgets on the pill with no card. */
    fun idleWidgetSwipe(): Boolean = wiring.view.notifications.isEmpty() && ui.idleWidgetCycle != null

    fun showDeadZone(on: Boolean) {
        if (on == deadShown) return
        deadShown = on
        scope.launch { ui.deadZone.animateTo(if (on) 1f else 0f, swipeCalmSpring()) }
    }

    fun pillCloseAllowed(): Boolean {
        val swipeUp = SwipeAction.fromId(wiring.view.settings.swipeUpAction, SwipeAction.DismissCurrent)
        val open = ui.pillOpen
        return wasExpandedAtStart && swipeUp == SwipeAction.Collapse && wiring.view.settings.enableSwipeActions &&
            !stepsActive && !pickerOpenAtStart && !launcherAtStart && !isHoldRegistered &&
            wiring.view.notifications.isNotEmpty() && !ui.widgetIslandOpen && !ui.widgetIslandShown &&
            !ui.swipeLock.isLocked(SystemClock.uptimeMillis()) &&
            !open.active && open.springsAtRest(true) && ui.cardPop.value >= 1f && ui.pendingCardEnterSign == 0 &&
            ui.cardLeanFromPx == 0f && !ui.stepDrag.active
    }

    /** A pill with a card on it, free of the other pill jobs. */
    fun pillOpenAllowed(): Boolean = !wasExpandedAtStart && !pillGestureTriggered && !isHoldRegistered &&
        wiring.view.settings.enablePillSwipeActions && wiring.view.notifications.isNotEmpty() &&
        !ui.widgetIslandOpen && !ui.widgetIslandShown && !(wiring.view.settings.autoHidePill && ui.isAutoHidden)

    lateinit var holdJob: Job
    var lastDragResetMs = 0L
    var loopDone = false

    fun startHold() {
        holdJob = launchHold()
    }

    private fun launchHold(): Job = scope.launch {
        delay(HOLD_GESTURE_THRESHOLD_MS)
        isHoldRegistered = true
        // portrait: the Open Apps shortcut (see longPressOpensApps)
        val inCentreBand = pillCenterTapHit(
            xPx = downPosition.x, yPx = downPosition.y, shellWidthPx = shellWidthPx,
            pillWidthPx = settings.width * density,
            pillHeightPx = settings.height * density,
            bandShiftPx = bandShiftPx, bandWidthPx = bandWidthPx
        )
        val opensApps = longPressOpensApps(
            isLandscape = isLandscape,
            notchMode = wiring.view.settings.enableNotchMode,
            shortcutsEnabled = wiring.view.settings.enableAppShortcuts,
            centreOverlapsCutout = centreOverlapsCutout,
            touchInCentreBand = inCentreBand,
            pillHasContent = pillHasContent
        )
        val onGlyph = pillHasContent && wiring.view.notifications.isNotEmpty() &&
            pillOpenMode == 0 && !ui.widgetIslandOpen && !ui.widgetIslandShown &&
            !(wiring.view.settings.autoHidePill && ui.isAutoHidden) &&
            HoldGesture.holdStartsSwipe(
                pillTapZone(
                    downPosition.x, shellWidthPx, settings.width * density,
                    bandShiftPx = bandShiftPx, bandWidthPx = bandWidthPx, side = slotSide
                ),
                pulseRunning
            )
        val hold = HoldGesture.atThreshold(
            collapsedAtStart = !wasExpandedAtStart,
            dragging = isDragging,
            pillGestureTriggered = pillGestureTriggered,
            opensApps = opensApps,
            action = SwipeAction.fromId(wiring.view.settings.longPressAction, SwipeAction.TogglePill),
            onGlyph = onGlyph
        )
        if (hold.readSwipe) {
            // the touch stays open for the swipe: the lift decides between the card and the apps shortcut
            glyphHoldArmed = true
            appsShortcutPending = hold.armAppsShortcut
        } else if (hold.armAppsShortcut) {
            pillGestureTriggered = true
            appsShortcutPending = true
        }
        if (hold.buzz) {
            IslandHaptics.play(context, HapticStrength.fromId(wiring.view.settings.hapticStrength), HapticKind.Hold)
        }
        if (hold.glow) scope.launch { ui.holdGlow.animateTo(1f, tween(HOLD_GLOW_IN_MS)) }
        hold.runAction?.let { action ->
            pillGestureTriggered = true
            runner.runLongPress(action)
        }
    }
}
