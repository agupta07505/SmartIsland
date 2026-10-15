/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * Mutable state that several of the island's behaviours share: the finger-follow drags, the pill auto-hide,
 * and the "last touched" time that restarts the auto-hide / auto-collapse timers.
 */
internal class IslandInteractionState {
    /** Where the pill's privacy dots are on screen right now (null: none shown). Read by the pill's tap handling. */
    var privacyDots: PrivacyDotsHit? = null
    /** Window position of the pill's gesture area, to turn a touch position into window pixels. */
    var gestureOrigin: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
    /** True while the floating-window handle at the bottom of the expanded island owns the touch; the island's own gestures skip it. */
    var handleOwnsTouch: Boolean = false
    /** Called with true when a finger lands on the floating-window handle and false when that touch ends (lift, cancel, handle gone). Set by the overlay. */
    var onHandleHeld: ((Boolean) -> Unit)? = null
    /** 0..1.15: how far the floating-window handle has been pulled down (see [com.agupta07505.smartisland.util.SwipeDownPull.handleScaleX]). Read in the shell's `graphicsLayer`. */
    var handlePull by mutableFloatStateOf(0f)
    var handleReleaseJob: kotlinx.coroutines.Job? = null
    /** Vertical finger-follow of the main island (expanded: drag up/down; collapsed: a damped copy of the pill swipe). */
    var dragOffset by mutableFloatStateOf(0f)
    var pillDragOffsetX by mutableFloatStateOf(0f)
    /** Finger-follow of the landscape corner pill. */
    var cornerPillDragX by mutableFloatStateOf(0f)
    var cornerPillDragY by mutableFloatStateOf(0f)
    /** How far (px) the collapsed pill's content has followed the finger sideways. */
    var pillContentDragX by mutableFloatStateOf(0f)
    /** 0..1: the pill swipe is armed (lifting now fires). Drives the small swell that says so. */
    val pillArm = Animatable(0f)
    /**
     * 0..1, the finger is in the dead zone (moved too far for a tap, not far enough for a swipe, or
     * a swipe that would do nothing): lifting now does nothing. Drives the small shrink of the island that says so.
     */
    val deadZone = Animatable(0f)
    /** 0..1: the clock widget's hourly tick (ui/HourChime.kt) covers the pill's own content. The pill's content layer fades out by it. */
    val chimeShow = Animatable(0f)
    /** Landscape: asks the view model to put the wide pill on the island for a tick while the pill is hidden ([withTickPill]). Set by the overlay. */
    var onTickHold: (Boolean) -> Unit = {}
    /** How many ticks hold the pill right now ([withTickPill]); the pill is released when it is back to 0. */
    var tickHolds = 0
    /** Same for the landscape corner pill, which is a separate touch target. */
    val cornerDeadZone = Animatable(0f)
    /** The short swipe lock (an armed swipe was pulled back). Shared by every touch on the island, so it outlives a lift. */
    val swipeLock = com.agupta07505.smartisland.util.SwipeLock()
    /** 0..1: the lock cue (a thin outline over the shell). On exactly while [swipeLock] runs; see [startLockCue]. */
    val lockCue = Animatable(0f)
    var lockCueJob: kotlinx.coroutines.Job? = null
    /**
     * How far a swipe down has pulled the island (0..1.25, see [com.agupta07505.smartisland.util.SwipeDownPull]).
     * It follows the finger, so it is set directly while the finger is down, and springs back to 0 on lift. Read in the shell's `graphicsLayer`.
     */
    var swipeDownPull by mutableFloatStateOf(0f)
    /** Same for the landscape corner pill. */
    var cornerSwipeDownPull by mutableFloatStateOf(0f)
    /** When the reader text last scrolled; a touch that starts on it is not a tap. Set by the overlay. */
    var readerScroll: com.agupta07505.smartisland.util.ReaderScrollTracker = com.agupta07505.smartisland.util.ReaderScrollTracker()
    /** A card step following the finger (set by the overlay from the composition local). */
    var stepDrag: com.agupta07505.smartisland.ui.expanded.StepDragState = com.agupta07505.smartisland.ui.expanded.StepDragState()
    /** px the incoming card still has to travel; it starts on the side opposite to the swipe and runs to 0. */
    val pillEnter = Animatable(0f)
    /** Set when a swipe changed the card: +1 = the next card comes in from the right, -1 = from the left, 0 = none waiting. */
    var pendingEnterSign by mutableIntStateOf(0)
    /**
     * Set by the idle widgets while they sit on the collapsed pill with no card: steps to the next (true) or the previous (false) widget and
     * returns whether another widget took over (false = only one widget, the swipe springs back). Null when no idle widget is on the pill.
     */
    var idleWidgetCycle: ((forward: Boolean) -> Boolean)? = null
    /**
     * The idle widget frame the collapsed pill is drawing right now (null: none). Set by [CollapsedContentLayer]; read by the pill's tap handling
     * (round or wide pill) and by the semi-expanded widget island, which needs the live widget data.
     */
    var idleFrameNow by mutableStateOf<IdleWidgetFrame?>(null)
    /**
     * The widget island's pause button: true holds the collapsed pill on its current idle widget (the automatic switch stops; swiping still steps
     * it). Lives for the session, not stored. Read by [rememberIdleWidgetFrame].
     */
    var idleCyclePaused by mutableStateOf(false)
    /** The widget island is open (set by [WidgetIslandLayer]): the pill holds its widget, a swipe on it moves the island, auto-hide waits. */
    var widgetIslandOpen by mutableStateOf(false)
    /** A tap on the idle widget's glyph slot (opens or closes the semi-expanded widget island). Also the outside tap that closes it. Set by the overlay. */
    var onWidgetGlyphTap: (() -> Unit)? = null
    /** A tap on the idle widget's value slot. Same job as [onWidgetGlyphTap] for now, a separate hook so it can differ later. Set by the overlay. */
    var onWidgetValueTap: (() -> Unit)? = null
    /**
     * How far (px, signed) the whole island leans sideways with the finger on a left / right swipe (see
     * [com.agupta07505.smartisland.util.SwipeSideways]). The card itself does not move under the finger.
     */
    var cardDragX by mutableFloatStateOf(0f)
    /**
     * The swap of a swipe that brought another card, 0 at the lift to 1 when the new card has arrived (one spring). It moves
     * the old card out, the new card in and lets the island's lean go ([cardLeanFromPx]). 1 at rest.
     */
    val cardPop = Animatable(1f)
    /** The lean (px) the finger let go with; the swap lets it go with `1 - cardPop` (see [com.agupta07505.smartisland.util.SwipeSideways.totalLeanPx]). */
    var cardLeanFromPx by mutableFloatStateOf(0f)
    /** +1 = the swap's new card comes in from the right (the old one leaves to the left), -1 = the other way, 0 = no swap running. */
    var cardSwapSign by mutableIntStateOf(0)
    /** Set when a swipe changed the expanded card: +1 = the next card comes in from the right, -1 = from the left, 0 = none waiting. */
    var pendingCardEnterSign by mutableIntStateOf(0)
    /** The card swap of a swipe is in flight (from the lift until the new card has settled): the pager must jump, not slide too. */
    var cardSwipeSnap by mutableStateOf(false)
    /** Key of the card a swipe left; until the island shows another key the card is hidden for the one frame it would flash at the old offset. */
    var pendingCardFromKey: String? = null
    /** When the last swipe switched a card (pill or expanded). The squash bounce skips switches the swipe's own slide already shows. */
    var lastCardSwipeMs: Long = 0L
    /** Largest sideways slide (px) of the expanded card; set at the start of every touch. */
    var cardSlideMaxPx by mutableFloatStateOf(0f)
    /** The ease-back and the "no card came" timer of the last expanded swipe; a new touch cancels them. */
    var cardBackJob: kotlinx.coroutines.Job? = null
    /** The one ease-back of the collapsed pill's drag (shell, lean, content, pull); a new touch cancels it so it cannot fight the finger. */
    var pillReleaseJob: kotlinx.coroutines.Job? = null
    var cardWaitJob: kotlinx.coroutines.Job? = null

    /** Forgets every sideways move of the expanded card at once (island collapsed, touch cancelled, gesture restarted). */
    suspend fun resetCard() {
        cardBackJob?.cancel(); cardWaitJob?.cancel()
        pendingCardEnterSign = 0
        pendingCardFromKey = null
        cardSwipeSnap = false
        cardDragX = 0f
        cleanSwapState()
    }

    /**
     * Puts the swap of a swipe back to rest (the swap spring at its end, no lean left over, no swap direction). Safe to call from a
     * `finally`: it is also what a swap that was cancelled half way (a second swipe, the card re-keyed, the host leaving) must leave behind,
     * so the next swipe does not start from the old one's numbers.
     */
    suspend fun cleanSwapState() {
        cardLeanFromPx = 0f
        cardSwapSign = 0
        cardPop.snapTo(1f)
    }
    /**
     * Shows the lock cue for what is left of [swipeLock]: fades in, holds, and fades out so that it is gone the moment the lock
     * ends (a new call restarts it). Call right after the lock started.
     */
    fun startLockCue(scope: kotlinx.coroutines.CoroutineScope) {
        lockCueJob?.cancel()
        lockCueJob = scope.launch {
            val remaining = swipeLock.remainingMs(android.os.SystemClock.uptimeMillis())
            lockCue.animateTo(1f, androidx.compose.animation.core.tween(LOCK_CUE_IN_MS))
            kotlinx.coroutines.delay(
                com.agupta07505.smartisland.util.lockCueHoldMs(remaining, LOCK_CUE_IN_MS.toLong(), LOCK_CUE_OUT_MS.toLong())
            )
            lockCue.animateTo(0f, androidx.compose.animation.core.tween(LOCK_CUE_OUT_MS))
        }
    }

    var isAutoHidden by mutableStateOf(false)
    var userInteractionTimestamp by mutableLongStateOf(System.currentTimeMillis())

    /** Any touch on the island restarts the auto-hide countdown. */
    fun touch() {
        userInteractionTimestamp = System.currentTimeMillis()
    }

    /** Called when a finger lands on the island itself (not the idle dot or the timers). Set by the overlay. */
    var onUserTouch: (() -> Unit)? = null

    /** A real touch on the island shell: restarts the countdowns and tells the ViewModel the user took over. */
    fun userTouched() {
        touch()
        onUserTouch?.invoke()
    }
}
