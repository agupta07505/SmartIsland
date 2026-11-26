/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.agupta07505.smartisland.model.BubbleOrigin
import com.agupta07505.smartisland.model.CutoutTapIntent
import com.agupta07505.smartisland.model.FlightTapHold
import com.agupta07505.smartisland.model.IslandPhase
import com.agupta07505.smartisland.model.ShellSwap
import com.agupta07505.smartisland.model.SwapAbort
import com.agupta07505.smartisland.model.SwapBurst
import com.agupta07505.smartisland.model.SwapClock
import com.agupta07505.smartisland.model.SwapCoordinator
import com.agupta07505.smartisland.model.SwapTouchGuard
import com.agupta07505.smartisland.model.SwipeLock
import com.agupta07505.smartisland.model.TapMemory
import com.agupta07505.smartisland.model.TapSwap
import com.agupta07505.smartisland.model.TwoShellSwap
import com.agupta07505.smartisland.model.lockCueHoldMs
import com.agupta07505.smartisland.ui.expanded.PickerMotion
import com.agupta07505.smartisland.ui.expanded.StepDragState
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.AppLogRecorder
import com.agupta07505.smartisland.util.ReaderScrollTracker
import com.agupta07505.smartisland.util.SwapTrace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class IslandInteractionState {
    var gestureOrigin: Offset = Offset.Zero
    var handleOwnsTouch: Boolean = false
    var onHandleHeld: ((Boolean) -> Unit)? = null
    // Dragging restarts the expanded card’s auto-collapse timer.
    var onDragActive: (() -> Unit)? = null
    var onHandleTap: (() -> Unit)? = null
    // The handle can overshoot full travel by 15%.
    var handlePull by mutableFloatStateOf(0f)
    var handleReleaseJob: Job? = null
    var dragOffset by mutableFloatStateOf(0f)
    var pillDragOffsetX by mutableFloatStateOf(0f)
    var cornerPillDragX by mutableFloatStateOf(0f)
    var cornerPillDragY by mutableFloatStateOf(0f)
    var pillContentDragX by mutableFloatStateOf(0f)
    // These 3 cues animate between 0 and 1. A fully armed swipe fires on release.
    val pillArm = Animatable(0f)
    val deadZone = Animatable(0f)
    val holdGlow = Animatable(0f)
    val chimeShow = Animatable(0f)
    var onTickHold: (Boolean) -> Unit = {}
    var tickHolds = 0
    val cornerDeadZone = Animatable(0f)
    // Pulling an armed swipe back briefly locks it.
    val swipeLock = SwipeLock()
    val lockCue = Animatable(0f)
    var lockCueJob: Job? = null
    var swipeDownPull by mutableFloatStateOf(0f)
    var cornerSwipeDownPull by mutableFloatStateOf(0f)
    val pillOpen = PillOpenDragState()
    // A touch that starts during a reader scroll must not count as a tap.
    var readerScroll: ReaderScrollTracker = ReaderScrollTracker()
    var stepDrag: StepDragState = StepDragState()
    /** px the incoming card still has to travel. */
    val pillEnter = Animatable(0f)
    var pendingEnterSign by mutableIntStateOf(0)
    /** Set by the idle widgets while they sit on the collapsed pill with no card. */
    var idleWidgetCycle: ((forward: Boolean) -> Boolean)? = null
    var idleFrameNow by mutableStateOf<IdleWidgetFrame?>(null)
    var idleCyclePaused by mutableStateOf(false)
    var widgetIslandOpen by mutableStateOf(false)
    var widgetIslandShown by mutableStateOf(false)
    /** Edge layout: the secondary bubble is out beside the pill (not merged into it). */
    var edgeBubbleApart by mutableStateOf(false)
    var edgeMergeScaleX by mutableFloatStateOf(1f)
    var edgeMergeScaleY by mutableFloatStateOf(1f)
    var widgetIslandCovers: () -> Boolean = { false }
    var onWidgetGlyphTap: (() -> Unit)? = null
    var onWidgetValueTap: (() -> Unit)? = null
    var cardDragX by mutableFloatStateOf(0f)
    val cardPop = Animatable(1f)
    /** Lean (px) the finger let go with; the swap lets it go with `1 - cardPop`. */
    var cardLeanFromPx by mutableFloatStateOf(0f)
    var cardSwapSign by mutableIntStateOf(0)
    var pendingCardEnterSign by mutableIntStateOf(0)
    var cardSwipeSnap by mutableStateOf(false)
    var pendingCardFromKey: String? = null
    var lastCardSwipeMs: Long = 0L
    var cardSlideMaxPx by mutableFloatStateOf(0f)
    var cardBackJob: Job? = null
    var pillReleaseJob: Job? = null
    /** Finger down on the collapsed pill's gesture area (the release guard waits for it to lift). */
    var pillTouchActive by mutableStateOf(false)
    var pillSettleGuardJob: Job? = null
    var cardWaitJob: Job? = null

    suspend fun resetCard() {
        cardBackJob?.cancel(); cardWaitJob?.cancel(); tapReplayJob?.cancel()
        pendingCardEnterSign = 0
        pendingCardFromKey = null
        cardSwipeSnap = false
        cardDragX = 0f
        cleanSwapState("reset")
    }

    val tapSwapActive: Boolean get() = swapBusy
    /** Whether a tap swap may run now (setting on and no 2 islands drawn side by side). */
    var tapSwapAllowed: Boolean = false
    /** Window bounds of the expanded shell (kept while no swap runs) and of the cutout beside it. */
    var shellBoundsWin: Rect? = null
    var cutoutBoundsWin: Rect? = null
    /** Captured at the tap: where the cutout sits against the shell (px) and the shell's smallest scale. */
    var tapSwapDx by mutableFloatStateOf(0f)
    var tapSwapDy by mutableFloatStateOf(0f)
    var tapSwapScaleX by mutableFloatStateOf(1f)
    var tapSwapScaleY by mutableFloatStateOf(1f)
    // Snapshot both rects on touch-down. The outgoing shell uses them throughout the swap.
    var tapSwapShellRect: Rect? = null
    var tapSwapCutoutRect: Rect? = null
    // The outgoing shell can draw once the last card frame has been captured.
    var tapSwapOutgoingLayer by mutableStateOf<androidx.compose.ui.graphics.layer.GraphicsLayer?>(null)
    /** Progress the outgoing card was handed to the ghost shell at, its fade starts from there (0: the swap starts at rest). */
    var tapSwapHandoffP: Float = 0f
    var tapSwapSpeed: AnimationSpeed = AnimationSpeed.NORMAL
    /** Blur of the card content while the swap runs (alpha-only where RenderEffect is not available). */
    val swapBlur = SwapBlurEffects()
    /**
     * Which cutout was tapped ([com.agupta07505.smartisland.model.TapSwap] ids), only that one hides during the swap
     */
    var tapSwapCutoutId: Int = 0
    var tapSwapGuardJob: Job? = null
    val tapSwapReverse: Boolean get() = swap.reverse
    /** Outgoing event is the selected one again (its card is live, no ghost), the coordinator's flag as state. */
    var tapSwapBack by mutableStateOf(false)
        private set
    val tapSwapSettling: Boolean get() = swap.phase == SwapCoordinator.Phase.Settling
    // Consume the click if touch-down already reversed the swap.
    val swapTouch = SwapTouchGuard()
    var tapSwapFromKey: String? = null
    var tapSwapToKey: String? = null
    var swapTraceFlags: String = ""

    val swap = SwapCoordinator { tapSwapBack = it }
    /** Tap rhythm of the swap, the spring is stiffer while taps come quickly. */
    val swapBurst = SwapBurst()
    /** The portrait picker's trade with the card: a swap does not start while it moves. */
    val pickerMotion = PickerMotion()
    /** The cutout tap waiting for the picker to rest, one at a time (the latest tap wins). */
    var pickerWaitJob: Job? = null
    var tapReplayJob: Job? = null
    /** [swap] is busy, as state so what latches on it recomposes when the swap ends. */
    var swapBusy by mutableStateOf(false)
        private set
    /** Told when a swap begins and ends, the view model keeps its picker out of the card changes in between. */
    var onSwapBusy: ((Boolean) -> Unit)? = null

    fun beginSwapPhase() {
        if (!swap.begin()) return
        swapBusy = true
        onSwapBusy?.invoke(true)
    }

    private fun endSwapPhase() {
        if (!swap.busy) return
        swap.end()
        swapBusy = false
        onSwapBusy?.invoke(false)
    }

    /**
     * Remembers the event the swap is heading away from, for when the swap has settled. There is 1 slot for what
     * a tap wants after the swap: the newest tap replaces a third event remembered before it
     */
    fun rememberOppositeTap() {
        val key = tapSwapOppositeKey() ?: return
        tapMemory.remember(key)
        shellSwap = shellSwap?.let { ShellSwap.withoutPending(it) }
    }

    /** Remembers a third event ([after] holds it) for when the swap has settled, replacing an older remembered tap. */
    fun rememberThirdTap(after: ShellSwap.State) {
        shellSwap = after
        tapMemory.clear()
    }

    /** Event a touch during the swap asks for: the one the swap is heading away from. */
    fun tapSwapOppositeKey(): String? = if (tapSwapReverse) tapSwapToKey else tapSwapFromKey

    var openedFromBubble by mutableStateOf(false)
    private var bubbleOriginSetMs: Long = 0L

    fun markOpenedFromBubble() {
        bubbleOriginSetMs = SystemClock.uptimeMillis()
        openedFromBubble = true
    }

    fun clearBubbleOrigin() {
        openedFromBubble = false
    }

    fun settleBubbleOrigin(expanded: Boolean, progress: Float, orientationFlipped: Boolean = false) {
        if (!openedFromBubble) return
        val age = SystemClock.uptimeMillis() - bubbleOriginSetMs
        if (!BubbleOrigin.keeps(true, expanded, progress, orientationFlipped, age)) {
            openedFromBubble = false
        }
    }

    /** Expand progress as the shell really shows it (0 = pill, 1 = card). */
    var expandProgressNow: () -> Float = { 0f }

    val tapMemory = TapMemory()

    /** Uptime (ms) until which the card takes no touch after a swap ended (see [SwapTouchGuard.POST_SWAP_LOCK_MS]). */
    var cardTouchLockUntilMs: Long = 0L

    /** The card takes no touch while a swap runs and for a moment after it: a tap meant for the cutout never opens the app. */
    fun cardTouchLocked(nowMs: Long): Boolean = SwapTouchGuard.cardLocked(tapSwapActive, nowMs, cardTouchLockUntilMs)

    fun islandPhase(expanded: Boolean): IslandPhase =
        CutoutTapIntent.phase(expanded, expandProgressNow(), tapSwapActive)
    var tapSwapSelectOld: (() -> Unit)? = null
    var tapSwapSelectNew: (() -> Unit)? = null

    /** The outgoing event was picked before the swap came back to it (see [TapSwap.selectionStep]). */
    private var tapSwapOldPicked = false
    private var swapFrames = 0
    private var oldPickedAtFrame = 0

    private fun pickOld() {
        if (tapSwapOldPicked) return
        tapSwapOldPicked = true
        oldPickedAtFrame = swapFrames
        tapSwapSelectOld?.invoke()
    }

    private fun pickNew() {
        tapSwapOldPicked = false
        tapSwapSelectNew?.invoke()
    }

    /**
     * The swap is back at its start and the outgoing event is still to be picked: picks it unless it already is and
     * returns the frames its card still needs before the live card can take over from the ghost
     */
    fun pickOldAtEnd(): Int {
        val fresh = !tapSwapOldPicked
        pickOld()
        return if (fresh) TapSwap.OLD_CARD_COMPOSE_FRAMES else TapSwap.composeFramesLeft(swapFrames - oldPickedAtFrame)
    }

    private fun resetSwapSelection() {
        tapSwapOldPicked = false
        swapFrames = 0
        oldPickedAtFrame = 0
    }

    fun syncTapSwapSelection(p: Float) {
        swapFrames++
        when (TapSwap.selectionStep(tapSwapBack, tapSwapReverse, tapSwapOldPicked, p)) {
            TapSwap.SelectStep.PickOld -> {
                SwapTrace.log("select-early") { "to=old ${swapTraceState()}" }
                pickOld()
            }
            TapSwap.SelectStep.PickNew -> {
                SwapTrace.log("select-early") { "to=new ${swapTraceState()}" }
                pickNew()
            }
            TapSwap.SelectStep.None -> Unit
        }
        val next = TapSwap.backAfter(tapSwapBack, tapSwapReverse, p)
        if (next == tapSwapBack) return
        swap.setBack(next)
        SwapTrace.log("select") { "to=${if (next) "old" else "new"} ${swapTraceState()}" }
        if (next) pickOld() else pickNew()
    }

    fun swapTraceState(): String =
        "p=${tapSwapP()} phase=${swap.phase} reverse=$tapSwapReverse back=$tapSwapBack " +
            "pending=$pendingCardEnterSign shell=$shellSwap allowed=$tapSwapAllowed"

    /** Ends a running tap swap at once: shell, cutout and card content go back to rest ([reason] is for the trace). */
    fun endTapSwap(reason: String) {
        if (tapSwapActive) {
            SwapTrace.log("end") { "reason=$reason ${swapTraceState()}" }
            cardTouchLockUntilMs = SystemClock.uptimeMillis() + SwapTouchGuard.POST_SWAP_LOCK_MS
        }
        tapSwapGuardJob?.cancel()
        tapSwapGuardJob = null
        tapSwapHandoffP = 0f
        tapSwapShellRect = null
        tapSwapCutoutRect = null
        // The parked layers are shared with the left/right swipe, hand them back as a swipe expects
        tapSwapOutgoingLayer?.resetSwapLook()
        tapSwapOutgoingLayer = null
        tapSwapSelectOld = null
        tapSwapSelectNew = null
        resetSwapSelection()
        swapTouch.reset()
        endSwapPhase()
    }

    /** Which event is where in a two-shell swap, and what a tap means (model/ShellSwap.kt), null when no swap runs. */
    var shellSwap: ShellSwap.State? = null
    /** Cutout of the remembered third tap, kept apart so it never moves the running swap. */
    var pendingCutoutId: Int = 0
    var pendingCutoutBounds: (() -> Rect?)? = null
    /** Called once a swap has run to its end and been tidied up (starts a remembered tap). */
    var onSwapSettled: (() -> Unit)? = null
    /**
     * Click on a picker button during a swap: flips the swap if the button sits on the swapping cutout, else nothing
     */
    var onPickerTapDuringSwap: ((Int) -> Unit)? = null

    /** A cutout was tapped: remember which one and where it sits (null = unknown, the plain slide runs). */
    fun markCutout(id: Int, bounds: () -> Rect?) {
        if (tapSwapActive) {
            // A tap during a swap: the running swap keeps its own cutout, this one is only for a remembered tap
            pendingCutoutId = id
            pendingCutoutBounds = bounds
            return
        }
        tapSwapCutoutId = id
        cutoutBoundsNow = bounds
        cutoutBoundsWin = bounds()
    }

    var cutoutBoundsNow: (() -> Rect?)? = null

    val flightHold = FlightTapHold()
    var onFlightLanded: ((String) -> Unit)? = null

    fun setBubbleFlight(active: Boolean) {
        flightHold.setFlying(active)?.let { onFlightLanded?.invoke(it) }
    }

    /**
     * How far the wide pulse over an open card covers cutout [id] (0..1): set by the pulse while it is drawn, it stands
     * in for the cutout on that spot (see [pulseCoverFactor])
     */
    var pulseCoverOf: (Int) -> Float = NoPulseCover

    /** What is left of cutout [id] while a pulse covers it: its own content is replaced by the pulse's, all of it. */
    fun pulseCoverFactor(id: Int): Float = 1f - pulseCoverOf(id).coerceIn(0f, 1f)

    /** Alpha of cutout [id]: hidden while a swap's shells sit on it, and while a pulse has taken its place. */
    fun cutoutSwapAlpha(id: Int): Float = swapHideAlpha(id) * pulseCoverFactor(id)

    /** Alpha of cutout [id] from the swap alone: hidden while a swap's shells sit on it. */
    fun swapHideAlpha(id: Int): Float =
        if (TapSwap.hidesWithSwap(tapSwapActive, tapSwapCutoutId, id)) {
            // No outgoing card is parked (still preparing, or none was), so no shell lands on the cutout: it stays
            if (tapSwapOutgoingLayer == null) 1f
            else TwoShellSwap.cutoutAlpha(tapSwapBack, tapSwapP())
        } else 1f

    /** The swap's progress: 0 while it prepares (nothing has moved), then the one spring's value. */
    fun tapSwapP(): Float = if (!tapSwapActive) 1f else if (pendingCardEnterSign != 0) 0f else cardPop.value

    /**
     * Stops a swap that has to end: the running motion heads for the nearest rest with the speed it has, and only
     * what is left after [SwapClock.graceMs] is put back at once
     */
    suspend fun abortTapSwap(reason: String) {
        if (!tapSwapActive) return
        AppLogRecorder.w("SwapAbort", "Tap swap aborted ($reason): ${swapTraceState()}")
        SwapTrace.log("abort") { "reason=$reason ${swapTraceState()}" }
        swap.abort()
        if (pendingCardEnterSign == 0 && cardPop.isRunning) {
            swap.setReverse(SwapAbort.settlesBack(tapSwapP()))
            // the running swap's loop picks the new direction up
            cardPop.stop()
            withTimeoutOrNull(SwapClock.graceMs(tapSwapStiffness(tapSwapSpeed))) { snapshotFlow { tapSwapActive }.first { !it } }
            if (!tapSwapActive) return
        }
        cleanSwapState(reason)
    }

    suspend fun cleanSwapState(reason: String) {
        endTapSwap(reason)
        cardLeanFromPx = 0f
        cardSwapSign = 0
        cardPop.snapTo(1f)
    }
    /** Shows the lock cue for what is left of [swipeLock]. */
    fun startLockCue(scope: CoroutineScope) {
        lockCueJob?.cancel()
        lockCueJob = scope.launch {
            val remaining = swipeLock.remainingMs(SystemClock.uptimeMillis())
            lockCue.animateTo(1f, tween(LOCK_CUE_IN_MS))
            delay(
                lockCueHoldMs(remaining, LOCK_CUE_IN_MS.toLong(), LOCK_CUE_OUT_MS.toLong())
            )
            lockCue.animateTo(0f, tween(LOCK_CUE_OUT_MS))
        }
    }

    var isAutoHidden by mutableStateOf(false)
    var userInteractionTimestamp by mutableLongStateOf(System.currentTimeMillis())

    fun touch() {
        userInteractionTimestamp = System.currentTimeMillis()
    }

    var onUserTouch: (() -> Unit)? = null

    fun userTouched() {
        touch()
        onUserTouch?.invoke()
    }

    var onSwipe: (() -> Unit)? = null

    fun swipeTouched() {
        touch()
        onSwipe?.invoke()
    }
}
