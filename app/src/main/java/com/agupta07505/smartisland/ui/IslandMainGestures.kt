/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.pointer.positionChange
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.StepDrag
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.launcherOwnsDrag
import com.agupta07505.smartisland.ui.expanded.readingTextOwnsDrag
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsDrag
import com.agupta07505.smartisland.ui.expanded.siblingPickerOwnsTouch
import com.agupta07505.smartisland.util.AxisLock
import com.agupta07505.smartisland.util.GestureSensitivity
import com.agupta07505.smartisland.util.HapticKind
import com.agupta07505.smartisland.util.HapticStrength
import com.agupta07505.smartisland.util.IslandHaptics
import com.agupta07505.smartisland.util.PillSwipeDirection
import com.agupta07505.smartisland.util.SwipeArming
import com.agupta07505.smartisland.util.SwipeFeel
import com.agupta07505.smartisland.util.SwipeDownPull
import com.agupta07505.smartisland.util.TapGuard
import com.agupta07505.smartisland.util.readerTapAccepted
import com.agupta07505.smartisland.util.deadZoneCue
import com.agupta07505.smartisland.util.stepFollowsFinger
import com.agupta07505.smartisland.model.CardDepth
import com.agupta07505.smartisland.model.pillCenterTapHit
import com.agupta07505.smartisland.model.WidgetTapAction
import com.agupta07505.smartisland.model.idleWidgetTapAction
import com.agupta07505.smartisland.model.pillTapZone
import com.agupta07505.smartisland.model.portraitCenterTapIsIdleControl
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_EVENT_MS
import com.agupta07505.smartisland.util.CARD_ENTER_WAIT_MS
import com.agupta07505.smartisland.util.SwipeCardOutcome
import com.agupta07505.smartisland.util.PILL_CONTENT_FOLLOW
import com.agupta07505.smartisland.util.softFollow
import com.agupta07505.smartisland.util.predictCardChange
import com.agupta07505.smartisland.util.swipeIsNoOp
import com.agupta07505.smartisland.util.swipeChangesCard
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * All touch handling on the island shell itself: tap, hold, drag with finger-follow, the pill swipe actions
 * (collapsed) and the swipe-up / down / sideways actions (expanded).
 */
internal fun Modifier.islandMainGestures(
    settings: SmartIslandSettings,
    isInputActive: Boolean,
    isLandscape: Boolean,
    cb: IslandCallbacks,
    ui: IslandInteractionState,
    runner: IslandActionRunner,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    pickerLeftFraction: State<Float>,
    pickerOnLeft: State<Boolean>,
    centreOverlapsCutout: Boolean = false,
    pillHasContent: Boolean = true
): Modifier {
    val displayMetrics = context.resources.displayMetrics
    return this
        .onGloballyPositioned { ui.gestureOrigin = it.positionInWindow() }
        .pointerInput(displayMetrics.density, isInputActive, isLandscape, centreOverlapsCutout, pillHasContent) {
            if (isInputActive) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (ui.handleOwnsTouch) {
                    // The floating-window handle owns this touch: wait for every finger to lift and do nothing here.
                    do {
                        val handleEvent = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
                    } while (handleEvent.changes.any { it.pressed })
                    return@awaitEachGesture
                }
                ui.userTouched()
                // A new touch takes the card over: an ease-back or "no card came" timer from the last swipe must not fight the finger.
                ui.cardBackJob?.cancel()
                ui.cardWaitJob?.cancel()
                ui.pillReleaseJob?.cancel()
                ui.pillDragOffsetX = 0f
                ui.dragOffset = 0f
                // Read once per touch so the distances do not change under the finger.
                val sensitivity = GestureSensitivity.fromId(cb.currentSettings.gestureSensitivity)
                val tapSlop = sensitivity.scaled(10f) * displayMetrics.density
                // This touch is a tap only while the finger stays within the slop (peak distance, on both axes).
                val tapGuard = TapGuard(tapSlop)
                // Collapsed pill swipes fire when the finger lifts, and only if still past the threshold (a swipe
                // pulled back towards the start cancels).
                val pillThresholdPx = sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP) * displayMetrics.density
                // The collapsed pill never locks: its own lock is never started, and the expanded island's lock does not reach it.
                val pillArming = SwipeArming(sensitivity, pillThresholdPx).also { it.lockWhen = { false } }
                // Idle widgets on the pill with no card: a sideways swipe steps through them, whether or not the pill swipe actions are on.
                val idleWidgetSwipe: () -> Boolean = { cb.currentNotifications.isEmpty() && ui.idleWidgetCycle != null }
                val expandedArming = SwipeArming(
                    sensitivity, sensitivity.scaled(SWIPE_THRESHOLD_DP) * displayMetrics.density, disarmFraction = 1f,
                    lock = ui.swipeLock,
                    horizontalThreshold = sensitivity.scaled(EXPANDED_SIDE_SWIPE_THRESHOLD_DP) * displayMetrics.density
                )
                val maxSlidePx = pillContentMaxSlideDp(settings.width) * displayMetrics.density
                // The drag went past the threshold at some point: lifting is then never a tap, even if it came back.
                var crossedOnce = false
                val pressTimeMs = System.currentTimeMillis()
                val wasExpandedAtStart = cb.currentExpanded
                // Sibling picker open when the touch began, and where in the island it landed.
                val pickerOpenAtStart = cb.currentSiblingPickerOpen
                val readingTextAtStart = cb.currentReadingExpandedText
                // The quick-launch window (an open island with no cards) swipes between its own pages, so any drag in
                // it is the launcher's: swipe up / down switch Apps and Utilities, left / right switch Apps and
                // Recents.
                val launcherAtStart = wasExpandedAtStart && cb.currentSettings.enableAppShortcuts &&
                    cb.currentNotifications.isEmpty()
                // Reader-scroll tap guard: a touch that begins on a reader text that is still flinging (or only just
                // stopped), or that sees the reader move under the finger, is never a tap, so stopping a fling cannot
                // open the notification.
                val readerTracker = ui.readerScroll
                val readerStartedOnScroll = wasExpandedAtStart && readerTracker.touchStartsOnScroll(android.os.SystemClock.uptimeMillis())
                val readerMovesAtDown = readerTracker.moveCount
                // Preview, reader or an open picture: the island's normal swipe actions are off there.
                val stepsActive = wasExpandedAtStart && cb.currentSettings.enableSwipeActions &&
                    cb.currentCardDepth != CardDepth.First
                // Only a swipe that showed itself as armed (the tick played) locks swiping when it is pulled back; a side
                // set to "None" or a swipe that would do nothing was never armed for the user.
                expandedArming.lockWhen = { dir -> expandedSwipeFires(dir, stepsActive, cb) }
                // Landscape image selector: no tap (the island's own, the one that opens the app) and no swipe action
                // registers there; a tap on one of its pictures belongs to the picture, and swipe up (one step back)
                // is the only gesture.
                val selectorLocked = wasExpandedAtStart && cb.currentCardDepth == CardDepth.Selector
                // On a plain expanded card the card follows the finger sideways (not under the sibling picker, not in
                // the preview / reader / picture steps, which have their own swipes).
                val cardMaxSlidePx = com.agupta07505.smartisland.util.SwipeSideways.maxLeanPx(displayMetrics.density)
                ui.cardSlideMaxPx = cardMaxSlidePx
                val cardFollows = wasExpandedAtStart && cb.currentSettings.enableSwipeActions && !stepsActive &&
                    !pickerOpenAtStart
                val downYDp = down.position.y / displayMetrics.density
                val downXFraction = if (size.width > 0) down.position.x / size.width else 0f
                // The touch landed on the open sibling picker (rows or header).
                val pickerTouchAtStart = wasExpandedAtStart && siblingPickerOwnsTouch(
                    pickerOpenAtStart, isLandscape, downXFraction, pickerLeftFraction.value, pickerOnLeft.value
                )
                // Dead-zone indicator: the island shrinks while the finger has left the tap area and lifting would do nothing.
                var deadShown = false
                fun showDeadZone(on: Boolean) {
                    if (on == deadShown) return
                    deadShown = on
                    scope.launch { ui.deadZone.animateTo(if (on) 1f else 0f, swipeCalmSpring()) }
                }
                var isHoldRegistered = false
                var dragAccumulatorY = 0f
                var dragAccumulatorX = 0f
                // Axis lock: once the finger passes the tap slop, the longer axis owns the rest of this touch and the
                // other axis is ignored, so a swipe up / down never picks up sideways movement (and the other way
                // round).
                val axisLock = AxisLock(tapSlop)
                var rawDragX = 0f
                var rawDragY = 0f
                var isDragging = false
                var pillGestureTriggered = false
                // Set when a child (the expanded notification text) scrolls vertically, so
                // that scroll is not also treated as an island swipe.
                var childScrolled = false
                // The island closed while this touch (which began on the expanded card) was still down.
                var closedUnderFinger = false
                // A vertical drag opens (or closes) a card step WITH the finger. 0 = not started, 1 = a step deeper
                // is open under the finger, -1 = a step back is, 2 = nothing to open here (the plain swipe rules
                // apply on lift).
                var stepMode = 0
                // One buzz per step drag: at the far end of the drag, or at the lift if the step finished without reaching it.
                val stepHaptic = com.agupta07505.smartisland.model.StepDragHaptic()
                val stepRangePx = StepDrag.RANGE_DP * displayMetrics.density
                // Smoothing: the finger sets the target progress, a per-frame ease (only while a step drag is open)
                // moves the shown progress and height toward it.
                var stepTarget = 0f
                var stepSmoothJob: kotlinx.coroutines.Job? = null

                val holdJob = scope.launch {
                    kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                    isHoldRegistered = true
                    // The hold buzz is skipped when this long press opens the app shortcuts (quiet launcher); every
                    // other hold still buzzes.
                    var holdBuzzed = false
                    var openedApps = false
                    fun holdBuzz() {
                        if (holdBuzzed) return
                        holdBuzzed = true
                        IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Hold)
                    }
                    // Long press on the collapsed pill (also on the idle pill, which is what
                    // a hidden pill shows). Fires at once, while the finger is still down.
                    if (!wasExpandedAtStart && !isDragging && !pillGestureTriggered) {
                        // Portrait: the Open Apps shortcut (see longPressOpensApps); otherwise the normal long-press action.
                        val inCentreBand = pillCenterTapHit(
                            xPx = down.position.x, yPx = down.position.y, shellWidthPx = size.width.toFloat(),
                            pillWidthPx = settings.width * displayMetrics.density,
                            pillHeightPx = settings.height * displayMetrics.density
                        )
                        val opensApps = com.agupta07505.smartisland.model.longPressOpensApps(
                            isLandscape = isLandscape,
                            notchMode = cb.currentSettings.enableNotchMode,
                            shortcutsEnabled = cb.currentSettings.enableAppShortcuts,
                            centreOverlapsCutout = centreOverlapsCutout,
                            touchInCentreBand = inCentreBand,
                            pillHasContent = pillHasContent
                        )
                        val longPress = SwipeAction.fromId(cb.currentSettings.longPressAction, SwipeAction.TogglePill)
                        if (opensApps) {
                            // Blocks any swipe action for the rest of this touch. No buzz: opening the shortcuts is silent.
                            pillGestureTriggered = true
                            openedApps = true
                            cb.currentOnOpenAppsLauncher()
                        } else {
                            holdBuzz()
                            if (longPress != SwipeAction.None) {
                                // Blocks any swipe action for the rest of this touch.
                                pillGestureTriggered = true
                                runner.runLongPress(longPress)
                            }
                        }
                    }
                    // Any other hold (expanded island, a drag, a swipe already fired) keeps its buzz.
                    if (!openedApps) holdBuzz()
                }

                val pointerId = down.id
                var loopDone = false
                try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                    val released = change.changedToUp() || (pickerTouchAtStart && change.changedToUpIgnoreConsumed())
                    if (!change.pressed && !released) break

                    // A picker row consumes its own (accepted) tap; the island must still see that lift, or this loop would wait for
                    // the next touch and the hold buzz would fire 300 ms later.
                    if (released) {
                        change.consume()
                        holdJob.cancel()
                        if (childScrolled) break
                        // A plain tap on the picker was the picker's (its row or its button handled it); nothing else runs.
                        if (pickerTouchAtStart && !isDragging) break
                        // The island closed under the finger (auto-collapse, the shade, a call): nothing runs on a card that is gone,
                        // and it is not a tap either.
                        if (wasExpandedAtStart && !cb.currentExpanded) {
                            closedUnderFinger = true
                            break
                        }
                        val totalElapsedMs = System.currentTimeMillis() - pressTimeMs
                        val currentNotification = cb.currentNotifications.getOrNull(cb.currentSelectedIndex)

                        if (wasExpandedAtStart) {
                            // While swiping is locked (an armed swipe was pulled back a moment ago, possibly in an earlier touch)
                            // lifting fires nothing. A tap is not affected.
                            val swipeLocked = ui.swipeLock.isLocked(android.os.SystemClock.uptimeMillis())
                            val swipeUpThreshold = -sensitivity.scaled(SWIPE_THRESHOLD_DP) * displayMetrics.density
                            val swipeDownThreshold = sensitivity.scaled(SWIPE_THRESHOLD_DP) * displayMetrics.density
                            val isHorizontalSwipe = isDragging && !swipeLocked && cb.currentSettings.enableSwipeActions &&
                                abs(dragAccumulatorX) > sensitivity.scaled(EXPANDED_SIDE_SWIPE_THRESHOLD_DP) * displayMetrics.density &&
                                sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)
                            val stepVertical = stepsActive && isDragging && !swipeLocked && sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)
                            val hapticStrength = HapticStrength.fromId(cb.currentSettings.hapticStrength)
                            if (stepMode == 1 || stepMode == -1) {
                                // The step is already open under the finger. Past the commit point it finishes,
                                // short of it the step goes back to where it was.
                                stepSmoothJob?.cancel()
                                val committed = StepDrag.commits(stepTarget)
                                val openedMode = stepMode
                                stepMode = 0
                                if (stepHaptic.onLift(committed)) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
                                val settleMs = cb.currentSettings.animationSpeed(isLandscape).durationMs(if (committed) StepDrag.SETTLE_MS else StepDrag.GO_BACK_SETTLE_MS)
                                val startProgress = ui.stepDrag.progress
                                scope.launch {
                                    androidx.compose.animation.core.Animatable(startProgress).animateTo(
                                        targetValue = if (committed) 1f else 0f,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = settleMs,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        )
                                    ) {
                                        ui.stepDrag.progress = value
                                        ui.stepDrag.heightDp = ui.stepDrag.currentHeightDp()
                                    }
                                    if (!committed) {
                                        // Back to the old height: now the old step comes back (same height, so no jump).
                                        if (openedMode == 1) cb.currentOnCardBack() else cb.currentOnCardDeeper()
                                    }
                                    // Hand the height back to the shell's own spring only once it has caught up (it has been heading to
                                    // the shown step's height all along), so there is no pop at the hand-over.
                                    val settled = ui.stepDrag.heightDp
                                    var waited = 0L
                                    ui.stepDrag.progress = if (committed) 1f else 0f
                                    // The two copies of the card show the same step now: drop the frozen one and make the real card
                                    // visible and tappable at once; only the height is still being handed over below.
                                    ui.stepDrag.settled = true
                                    while (waited < StepDrag.HANDOVER_MAX_MS && abs(ui.stepDrag.shellHeightDp - settled) > StepDrag.HANDOVER_TOLERANCE_DP) {
                                        kotlinx.coroutines.delay(16)
                                        waited += 16
                                    }
                                    ui.stepDrag.reset()
                                }
                            } else if (stepVertical && dragAccumulatorY > swipeDownThreshold) {
                                if (cb.currentOnCardDeeper()) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
                            } else if (stepVertical && dragAccumulatorY < swipeUpThreshold) {
                                if (cb.currentOnCardBack()) IslandHaptics.play(context, hapticStrength, HapticKind.Swipe)
                            } else if (stepsActive && isHorizontalSwipe) {
                                // Sideways swipes do nothing in these steps, and are not mistaken for a tap either.
                            } else if (isHorizontalSwipe) {
                                // Expanded left / right swipe. Cards only reach here while a horizontal
                                // action is set (the pager stops swiping on its own then); a side left
                                // on "None" does nothing but is not mistaken for a tap either.
                                val action = if (dragAccumulatorX < 0f) {
                                    SwipeAction.fromId(cb.currentSettings.swipeLeftAction, SwipeAction.None)
                                } else {
                                    SwipeAction.fromId(cb.currentSettings.swipeRightAction, SwipeAction.None)
                                }
                                commitExpandedSwipe(
                                    action, dragAccumulatorX, currentNotification, runner, ui, scope,
                                    cb.currentNotifications, cb.currentSelectedIndex
                                )
                            } else if (!stepsActive && isDragging && !swipeLocked && cb.currentSettings.enableSwipeActions && dragAccumulatorY < swipeUpThreshold &&
                                sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)) {
                                val isHold = isHoldRegistered || totalElapsedMs >= HOLD_GESTURE_THRESHOLD_MS
                                val actionStr = if (isHold) cb.currentSettings.swipeHoldUpAction else cb.currentSettings.swipeUpAction
                                val action = SwipeAction.fromId(actionStr, if (isHold) SwipeAction.DismissAll else SwipeAction.DismissCurrent)
                                runner.run(action, currentNotification)
                            } else if (!stepsActive && isDragging && !swipeLocked && cb.currentSettings.enableSwipeActions && dragAccumulatorY > swipeDownThreshold &&
                                sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)) {
                                val action = com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION
                                // A card with something deeper (a cut-off text, a picture, a sticker) opens it first, whatever
                                // the swipe-down action is set to; the action only runs when there is nothing deeper to open.
                                if (cb.currentOnCardDeeper()) {
                                    IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Swipe)
                                } else {
                                    runner.run(action, currentNotification)
                                }
                            } else if (!tapGuard.exceeded && !pickerTouchAtStart && !selectorLocked &&
                                readerTapAccepted(readerStartedOnScroll, readerMovesAtDown, readerTracker.moveCount)) {
                                // A tap: the finger never left the tap area, and it did not just stop a scrolling
                                // reader.
                                if (!isHoldRegistered) {
                                    if (currentNotification != null) {
                                        cb.currentOnOpenNotification(currentNotification)
                                    } else {
                                        SmartIslandRepositories.notificationRepository(context).resetTimer()
                                    }
                                }
                            }
                        } else {
                            if (!pillGestureTriggered) {
                                val isPillSwipeEnabled = cb.currentSettings.enablePillSwipeActions || idleWidgetSwipe()
                                val fireDirection = if (isPillSwipeEnabled) pillArming.direction else null

                                if (fireDirection != null) {
                                    pillGestureTriggered = true
                                    commitPillSwipe(fireDirection, dragAccumulatorX, cb, runner, ui, scope, maxSlidePx)
                                } else if ((isPillSwipeEnabled && crossedOnce) || tapGuard.exceeded) {
                                    // Pulled back towards the start, too diagonal, or simply moved too far for a tap: no gesture,
                                    // and not a tap either (the dead zone: a drag under the swipe threshold does not expand the pill).
                                } else {
                                    if (!isHoldRegistered) {
                                        // What a tap does on an idle widget pill (glyph slot, value slot, middle
                                        // band), see model/WidgetIsland.kt.
                                        val widgetTap = if (cb.currentNotifications.isEmpty()) {
                                            idleWidgetTapAction(
                                                widgetOnPill = ui.idleWidgetCycle != null && ui.idleFrameNow != null,
                                                compact = ui.idleFrameNow?.compact == true,
                                                zone = pillTapZone(down.position.x, size.width.toFloat(), settings.width * displayMetrics.density),
                                                hasStoredCards = cb.currentHasIdleStoredCards
                                            )
                                        } else {
                                            WidgetTapAction.None
                                        }
                                        if (settings.autoHidePill && ui.isAutoHidden) {
                                            ui.isAutoHidden = false
                                            ui.touch()
                                        } else if (widgetTap == WidgetTapAction.GlyphTap && ui.onWidgetGlyphTap != null) {
                                            // Idle widget pill, glyph slot: opens the semi-expanded widget island, or
                                            // closes it when it is open.
                                            ui.onWidgetGlyphTap?.invoke()
                                        } else if (widgetTap == WidgetTapAction.ValueTap && ui.onWidgetValueTap != null) {
                                            // Idle widget pill, value slot: same as the glyph slot, with its own
                                            // hook. The middle band shows the stored cards (next branch).
                                            ui.onWidgetValueTap?.invoke()
                                        } else if (cb.currentNotifications.isEmpty() && cb.currentHasIdleStoredCards) {
                                            cb.currentOnRevealIdleCards()
                                        } else if (portraitCenterTapIsIdleControl(isLandscape, settings.enableNotchMode, cb.currentNotifications.isNotEmpty()) &&
                                            pillCenterTapHit(
                                                xPx = down.position.x, yPx = down.position.y, shellWidthPx = size.width.toFloat(),
                                                pillWidthPx = settings.width * displayMetrics.density,
                                                pillHeightPx = settings.height * displayMetrics.density
                                            )
                                        ) {
                                            // The empty middle of the collapsed pill puts every card away (a glyph still expands).
                                            cb.currentOnIdleDotTap()
                                        } else if (isLandscape && !settings.enableNotchMode && cb.currentNotifications.isEmpty()) {
                                            cb.currentOnIdleDotTap()
                                        } else if (cb.currentNotifications.isNotEmpty()) {
                                            // (An empty island does not open the shortcuts on a tap: that is a long press.)
                                            cb.currentOnToggle()
                                        }
                                    }
                                }
                            }
                        }
                        break
                    } else {
                        // The island closed under the finger (auto-collapse, the shade, a call) while it was being dragged:
                        // the rest of this touch is not an expanded drag any more. Stop feeding the expanded-size offset
                        // (up to 100 dp) to the collapsed pill, which would push it out of its touch region and the window.
                        if (wasExpandedAtStart && !cb.currentExpanded) {
                            closedUnderFinger = true
                            holdJob.cancel()
                            break
                        }
                        val dragAmountY = change.positionChange().y
                        val dragAmountX = change.positionChange().x
                        if (wasExpandedAtStart && (childScrolled ||
                                (change.isConsumed && abs(dragAmountY) > abs(dragAmountX)) ||
                                // A drag on the picker rows belongs to the list even at its top/bottom
                                // edge, where the list consumes nothing, and a sideways drag there must
                                // not fire the left/right swipe actions either.
                                siblingPickerOwnsDrag(
                                    pickerOpenAtStart, downYDp, dragAmountX, dragAmountY,
                                    sideBySide = isLandscape, downXFraction = downXFraction,
                                    pickerLeftFraction = pickerLeftFraction.value,
                                    pickerOnLeft = pickerOnLeft.value) ||
                                // Same for the expanded notification's text while it is open for reading.
                                readingTextOwnsDrag(readingTextAtStart && !stepsActive, dragAmountX, dragAmountY) ||
                                // The launcher window's own page swipes.
                                launcherOwnsDrag(launcherAtStart, dragAmountX, dragAmountY))
                        ) {
                            childScrolled = true
                            holdJob.cancel()
                            continue
                        }
                        if (abs(dragAmountY) > 0.5f || abs(dragAmountX) > 0.5f) {
                            isDragging = true
                            rawDragX += dragAmountX
                            rawDragY += dragAmountY
                            if (axisLock.update(rawDragX, rawDragY)) {
                                // Drop what the other axis gathered before the lock.
                                if (axisLock.axis == AxisLock.Axis.Vertical) dragAccumulatorX = 0f else dragAccumulatorY = 0f
                            }
                            dragAccumulatorY += axisLock.filterY(dragAmountY)
                            dragAccumulatorX += axisLock.filterX(dragAmountX)
                            tapGuard.update(dragAccumulatorX, dragAccumulatorY)
                            // A card step follows the finger: the first clear vertical drag opens the step (deeper on
                            // a drag down, back on a drag up), the drag distance runs its progress, and the lift
                            // decides below.
                            if (wasExpandedAtStart && cb.currentSettings.enableSwipeActions && !pickerOpenAtStart && stepMode == 0 &&
                                !ui.swipeLock.isLocked(android.os.SystemClock.uptimeMillis()) &&
                                sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY) && abs(dragAccumulatorY) > tapSlop
                            ) {
                                val depthNow = cb.currentCardDepth
                                // Landscape too: the island beside the card grows and shrinks with the finger (IslandExpandedContent).
                                stepMode = when {
                                    dragAccumulatorY > 0f -> if (cb.currentOnCardDeeper()) 1 else 2
                                    depthNow != CardDepth.First -> if (cb.currentOnCardBack()) -1 else 2
                                    else -> 2
                                }
                                if (stepFollowsFinger(stepMode)) {
                                    // A step decides on lift by its own progress, so the swipe arming starts over and stays off.
                                    expandedArming.reset()
                                    ui.stepDrag.fromDp = ui.stepDrag.shellHeightDp
                                    ui.stepDrag.progress = 0f
                                    ui.stepDrag.cardKey = cb.currentNotifications.getOrNull(cb.currentSelectedIndex)?.key
                                    ui.stepDrag.heightDp = ui.stepDrag.fromDp
                                    ui.stepDrag.active = true
                                    stepTarget = 0f
                                    stepSmoothJob?.cancel()
                                    stepSmoothJob = scope.launch {
                                        var lastNanos = 0L
                                        while (true) {
                                            androidx.compose.runtime.withFrameNanos { now ->
                                                val dtMs = if (lastNanos == 0L) 16f else ((now - lastNanos) / 1_000_000f).coerceIn(1f, 50f)
                                                lastNanos = now
                                                val shown = ui.stepDrag.progress
                                                val next = StepDrag.smoothed(shown, stepTarget, dtMs)
                                                if (next != shown) {
                                                    ui.stepDrag.progress = next
                                                    ui.stepDrag.heightDp = ui.stepDrag.currentHeightDp()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (stepMode == 1 || stepMode == -1) {
                                stepTarget = StepDrag.progress(dragAccumulatorY * stepMode, tapSlop, stepRangePx)
                                if (stepHaptic.onProgress(stepTarget)) {
                                    IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Swipe)
                                }
                            }
                            // Swipe down stretches the island downward with the finger (only where a swipe down means something).
                            val pullAllowed = if (wasExpandedAtStart) {
                                cb.currentSettings.enableSwipeActions && !pickerOpenAtStart &&
                                    (stepsActive || com.agupta07505.smartisland.model.FIXED_EXPANDED_SWIPE_DOWN_ACTION != SwipeAction.None)
                            } else {
                                !pillGestureTriggered && cb.currentSettings.enablePillSwipeActions &&
                                    com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION != SwipeAction.None
                            } && sensitivity.isVertical(dragAccumulatorX, dragAccumulatorY)
                            ui.swipeDownPull = if (pullAllowed && stepMode != 1 && stepMode != -1) {
                                SwipeDownPull.progress(
                                    dragAccumulatorY, tapSlop,
                                    (if (wasExpandedAtStart) sensitivity.scaled(SWIPE_THRESHOLD_DP) else sensitivity.scaled(PILL_SWIPE_THRESHOLD_DP)) *
                                        displayMetrics.density
                                )
                            } else 0f
                            change.consume()
                            if (abs(dragAccumulatorY) > 5f * displayMetrics.density || abs(dragAccumulatorX) > 5f * displayMetrics.density) {
                                holdJob.cancel()
                            }
                            if (wasExpandedAtStart) {
                                // While a card step follows the finger, lifting commits or cancels by the step's own
                                // progress (40 % of its range), not by the swipe threshold.
                                if (cb.currentSettings.enableSwipeActions && !stepFollowsFinger(stepMode) &&
                                    expandedArming.update(dragAccumulatorX, dragAccumulatorY, android.os.SystemClock.uptimeMillis())
                                ) {
                                    // Tick when lifting would fire. A side set to "None" does nothing, so it gets no tick.
                                    val dir = expandedArming.direction
                                    val live = if (stepsActive) {
                                        // Only up and down mean something in the preview / reader / picture steps.
                                        dir == PillSwipeDirection.Up || dir == PillSwipeDirection.Down
                                    } else when (dir) {
                                        PillSwipeDirection.Left -> SwipeAction.fromId(cb.currentSettings.swipeLeftAction, SwipeAction.None).let {
                                            it != SwipeAction.None && !swipeIsNoOp(it, cb.currentNotifications, cb.currentSelectedIndex)
                                        }
                                        PillSwipeDirection.Right -> SwipeAction.fromId(cb.currentSettings.swipeRightAction, SwipeAction.None).let {
                                            it != SwipeAction.None && !swipeIsNoOp(it, cb.currentNotifications, cb.currentSelectedIndex)
                                        }
                                        else -> true
                                    }
                                    if (expandedArming.enteredLock) {
                                        // Pulled back: a dull buzz instead of a tick, and the lock cue.
                                        IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Lock)
                                        ui.startLockCue(scope)
                                    } else if (live) {
                                        IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Merge)
                                    }
                                }
                                // The island leans sideways with the finger, only on a side that has an action (a
                                // side set to "None" does nothing, so the island does not move there).
                                ui.cardDragX = if (cardFollows && sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)) {
                                    val sideAction = expandedSideAction(cb.currentSettings, dragAccumulatorX)
                                    when {
                                        sideAction == SwipeAction.None || swipeIsNoOp(sideAction, cb.currentNotifications, cb.currentSelectedIndex) -> 0f
                                        // A swipe that will bring another card leans fully; one that leaves the card
                                        // only gives a little (rubber band).
                                        swipeChangesCard(sideAction) &&
                                            predictCardChange(sideAction, cb.currentNotifications, cb.currentSelectedIndex) != SwipeCardOutcome.Stays -> {
                                            // The neighbouring cards are measured now, not after the idle delay (see NeighbourHeightProbe).
                                            val hint = com.agupta07505.smartisland.util.swipeNeighbourHint(sideAction)
                                            if (ui.stepDrag.swipeNeighbour != hint) ui.stepDrag.swipeNeighbour = hint
                                            com.agupta07505.smartisland.util.SwipeSideways.leanPx(dragAccumulatorX, displayMetrics.density)
                                        }
                                        else -> com.agupta07505.smartisland.util.SwipeSideways.leanPx(
                                            dragAccumulatorX, displayMetrics.density, com.agupta07505.smartisland.util.SwipeSideways.STAY_SHARE
                                        )
                                    }
                                } else 0f
                                // A swipe down only leans the island a little; a swipe up follows the finger.
                                val swipeUpAvailable = cb.currentSettings.enableSwipeActions &&
                                    SwipeAction.fromId(cb.currentSettings.swipeUpAction, SwipeAction.DismissCurrent).let {
                                        it != SwipeAction.None && !swipeIsNoOp(it, cb.currentNotifications, cb.currentSelectedIndex)
                                    }
                                ui.dragOffset = if (dragAccumulatorY > 0f) {
                                    SwipeDownPull.expandedDownOffsetPx(dragAccumulatorY, displayMetrics.density)
                                } else if (stepMode == 1 || stepMode == -1 || stepsActive || !swipeUpAvailable) {
                                    0f
                                } else {
                                    dragAccumulatorY.coerceAtLeast(-DRAG_MAX_OFFSET_DP * displayMetrics.density)
                                }
                            } else {
                                ui.pillDragOffsetX = softFollow(dragAccumulatorX, 0.35f, 24f * displayMetrics.density)
                                ui.dragOffset = softFollow(
                                    dragAccumulatorY, 0.35f, SwipeFeel.PILL_LEAN_Y_MAX_DP * displayMetrics.density
                                )

                                val pillHorizontal = sensitivity.isHorizontal(dragAccumulatorX, dragAccumulatorY)
                                ui.pillContentDragX = if (pillHorizontal) {
                                    softFollow(dragAccumulatorX, PILL_CONTENT_FOLLOW, maxSlidePx)
                                } else 0f
                                // A swipe on the pill keeps the secondary cutout's auto-merge timer (and the auto-
                                // hide timer) restarting while the finger moves, at most every
                                // PILL_SWIPE_TOUCH_GAP_MS (each touch() restarts the countdown coroutines, so it is
                                // not done on every move event).
                                if (pillHorizontal && System.currentTimeMillis() - ui.userInteractionTimestamp >= PILL_SWIPE_TOUCH_GAP_MS) {
                                    ui.touch()
                                }

                                if (abs(dragAccumulatorX) >= pillThresholdPx || abs(dragAccumulatorY) >= pillThresholdPx) {
                                    crossedOnce = true
                                }
                                if (!pillGestureTriggered && (cb.currentSettings.enablePillSwipeActions || idleWidgetSwipe())) {
                                    if (pillArming.update(dragAccumulatorX, dragAccumulatorY, android.os.SystemClock.uptimeMillis())) {
                                        holdJob.cancel()
                                        IslandHaptics.play(context, HapticStrength.fromId(cb.currentSettings.hapticStrength), HapticKind.Merge)
                                        val armed = pillArming.direction != null
                                        scope.launch { ui.pillArm.animateTo(if (armed) 1f else 0f, swipeCalmSpring()) }
                                    }
                                }
                            }
                            // Where the touch stands now: still a tap, in the dead zone (lifting does nothing), or armed (lifting fires).
                            val liftFires = if (wasExpandedAtStart) {
                                cb.currentSettings.enableSwipeActions &&
                                    expandedSwipeFires(expandedArming.direction, stepsActive, cb)
                            } else {
                                !pillGestureTriggered && (cb.currentSettings.enablePillSwipeActions || idleWidgetSwipe()) && pillArming.direction != null
                            }
                            showDeadZone(!pillGestureTriggered && deadZoneCue(tapGuard.exceeded, liftFires, stepFollowsFinger(stepMode)))
                        }
                    }
                }

                loopDone = true
                // A touch that ended some other way (the island closed under the finger) leaves no step half-open on screen.
                if (stepMode == 1 || stepMode == -1) {
                    stepSmoothJob?.cancel()
                    stepMode = 0
                    ui.stepDrag.reset()
                }
                } finally {
                    // The gesture was cancelled (the handler restarted on a rotation, the window changed): nothing may stay
                    // half-moved, and the hold must not fire later.
                    if (!loopDone) {
                        holdJob.cancel()
                        // A step that was following the finger goes back to where it was.
                        if (stepMode == 1 || stepMode == -1) {
                            stepSmoothJob?.cancel()
                            if (stepMode == 1) cb.currentOnCardBack() else cb.currentOnCardDeeper()
                            stepMode = 0
                            ui.stepDrag.reset()
                        }
                        ui.pillContentDragX = 0f
                        ui.pillDragOffsetX = 0f
                        ui.dragOffset = 0f
                        ui.cardDragX = 0f
                        ui.swipeDownPull = 0f
                        ui.pendingCardEnterSign = 0
                        ui.pendingCardFromKey = null
                        ui.pendingEnterSign = 0
                        ui.cardSwipeSnap = false
                        scope.launch {
                            ui.deadZone.snapTo(0f)
                            ui.pillArm.snapTo(0f)
                        }
                    }
                }

                holdJob.cancel()
                // The drag is over: the neighbour probe goes back to its idle timing.
                ui.stepDrag.swipeNeighbour = 0
                // The incoming card (if the swipe changed it) takes over the content; otherwise it eases back.
                easeBackPillMotion(ui, scope)
                // Expanded card: the incoming card (if the swipe changed it) takes over from where the finger left it;
                // otherwise (cancelled, or an action that keeps the card) it eases back.
                if (ui.pendingCardEnterSign == 0 && ui.cardDragX != 0f) {
                    easeCardBack(ui, scope)
                }
                if (ui.pillArm.value != 0f) {
                    scope.launch { ui.pillArm.animateTo(0f, swipeCalmSpring()) }
                }
                if (ui.deadZone.value != 0f) {
                    scope.launch { ui.deadZone.animateTo(0f, swipeCalmSpring()) }
                }
            }
        }
}

/** The spring a swipe's drag returns to rest with: no overshoot (see [SwipeFeel.EASE_BACK_STIFFNESS]). */
internal fun <T> swipeEaseBackSpring(): androidx.compose.animation.core.SpringSpec<T> =
    spring(dampingRatio = SwipeFeel.RETURN_DAMPING_RATIO, stiffness = SwipeFeel.EASE_BACK_STIFFNESS)

private fun easeBackPillMotion(ui: IslandInteractionState, scope: CoroutineScope) {
    val content0 = if (ui.pendingEnterSign == 0) ui.pillContentDragX else 0f
    val shell0 = ui.pillDragOffsetX
    val lean0 = ui.dragOffset
    val pull0 = ui.swipeDownPull
    if (content0 == 0f && shell0 == 0f && lean0 == 0f && pull0 == 0f) return
    ui.pillReleaseJob?.cancel()
    ui.pillReleaseJob = scope.launch {
        androidx.compose.animation.core.Animatable(1f).animateTo(0f, swipeEaseBackSpring()) {
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

/** The spring every swipe's temporary state (arm swell, dead-zone shrink) lets go with: no overshoot. */
internal fun <T> swipeCalmSpring(): androidx.compose.animation.core.SpringSpec<T> =
    spring(dampingRatio = SwipeFeel.RETURN_DAMPING_RATIO, stiffness = SwipeFeel.RETURN_STIFFNESS)

private fun expandedSwipeFires(direction: PillSwipeDirection?, stepsActive: Boolean, cb: IslandCallbacks): Boolean {
    if (direction == null) return false
    // Preview, reader and picture steps: only swipe up and down mean something.
    if (stepsActive) return direction == PillSwipeDirection.Up || direction == PillSwipeDirection.Down
    return when (direction) {
        PillSwipeDirection.Left, PillSwipeDirection.Right -> {
            val action = expandedSideAction(
                cb.currentSettings, if (direction == PillSwipeDirection.Left) -1f else 1f
            )
            action != SwipeAction.None && !swipeIsNoOp(action, cb.currentNotifications, cb.currentSelectedIndex)
        }
        else -> true
    }
}

private fun commitPillSwipe(
    direction: PillSwipeDirection,
    dragX: Float,
    cb: IslandCallbacks,
    runner: IslandActionRunner,
    ui: IslandInteractionState,
    scope: CoroutineScope,
    maxSlidePx: Float
) {
    val settings = cb.currentSettings
    // Every swipe that fires restarts the secondary cutout's auto-merge timer (and the auto-hide timer) from now.
    ui.touch()
    val action = when (direction) {
        PillSwipeDirection.Left -> SwipeAction.fromId(settings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
        PillSwipeDirection.Right -> SwipeAction.fromId(settings.pillSwipeRightAction, SwipeAction.NextNotification)
        PillSwipeDirection.Up -> SwipeAction.fromId(settings.pillSwipeUpAction, SwipeAction.DismissCurrent)
        PillSwipeDirection.Down -> com.agupta07505.smartisland.model.FIXED_PILL_SWIPE_DOWN_ACTION
    }
    val sideways = direction == PillSwipeDirection.Left || direction == PillSwipeDirection.Right
    // Idle widgets on the pill with no card: left = the next widget, right = the previous one.
    if (sideways && cb.currentNotifications.isEmpty() && ui.idleWidgetCycle != null) {
        val changed = ui.idleWidgetCycle?.invoke(direction == PillSwipeDirection.Left) == true
        if (changed) ui.pendingEnterSign = if (dragX < 0f) 1 else -1
        scope.launch {
            if (changed) kotlinx.coroutines.delay(CARD_ENTER_WAIT_MS)
            if (!changed || ui.pendingEnterSign != 0) {
                ui.pendingEnterSign = 0
                val start = ui.pillContentDragX
                androidx.compose.animation.core.Animatable(start).animateTo(
                    0f, swipeEaseBackSpring()
                ) { ui.pillContentDragX = value }
            }
        }
        return
    }
    // Up and down (and a sideways swipe on a pill that has cards) keep the pill swipe actions, which are off when
    // only the widgets asked for the swipe.
    if (!settings.enablePillSwipeActions) return
    if (sideways && swipeChangesCard(action)) {
        ui.lastCardSwipeMs = System.currentTimeMillis()
        // Swiped left: the old card left to the left, the new one comes from the right (and the other way round).
        ui.pendingEnterSign = if (dragX < 0f) 1 else -1
        scope.launch {
            kotlinx.coroutines.delay(CARD_ENTER_WAIT_MS)
            if (ui.pendingEnterSign != 0) {
                ui.pendingEnterSign = 0
                val start = ui.pillContentDragX
                androidx.compose.animation.core.Animatable(start).animateTo(
                    0f, swipeEaseBackSpring()
                ) { ui.pillContentDragX = value }
            }
        }
    }
    runner.run(action, cb.currentNotifications.getOrNull(cb.currentSelectedIndex))
}

/**
 * True = the expanded card's swipe slides the new card in from the side (heavier); false = the new card replaces the
 * old one at once and the switch squash plays (lighter).
 */
internal const val EXPANDED_SWIPE_SLIDES_IN = true

/** While a sideways swipe on the collapsed pill moves, the island's timers are restarted at most this often (ms). */
internal const val PILL_SWIPE_TOUCH_GAP_MS = 250L

/** A card switch this soon after a swipe is the swipe's own (the squash bounce skips it). */
internal const val SWIPE_SWITCH_BOUNCE_SKIP_MS = 600L

private fun expandedSideAction(settings: com.agupta07505.smartisland.data.SmartIslandSettings, dragX: Float): SwipeAction =
    SwipeAction.fromId(if (dragX < 0f) settings.swipeLeftAction else settings.swipeRightAction, SwipeAction.None)

private fun commitExpandedSwipe(
    action: SwipeAction,
    dragX: Float,
    notification: com.agupta07505.smartisland.model.IslandNotification?,
    runner: IslandActionRunner,
    ui: IslandInteractionState,
    scope: CoroutineScope,
    cards: List<com.agupta07505.smartisland.model.IslandNotification>,
    index: Int
) {
    // A sibling swipe with no sibling does nothing at all (no buzz either); the card just eases back.
    if (swipeIsNoOp(action, cards, index)) return
    val outcome = if (swipeChangesCard(action)) predictCardChange(action, cards, index) else SwipeCardOutcome.Stays
    if (outcome != SwipeCardOutcome.Stays && !EXPANDED_SWIPE_SLIDES_IN) {
        // Instant swap.
        ui.cardBackJob?.cancel()
        ui.cardWaitJob?.cancel()
        ui.pendingCardEnterSign = 0
        ui.pendingCardFromKey = null
        ui.cardDragX = 0f
        ui.cardSwipeSnap = true
        ui.cardWaitJob = scope.launch {
            kotlinx.coroutines.delay(CARD_ENTER_WAIT_MS)
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
        // Next / previous event may import from the shade first, so it gets longer to bring a card.
        val waitMs = if (outcome == SwipeCardOutcome.Unknown) CARD_ENTER_WAIT_EVENT_MS else CARD_ENTER_WAIT_MS
        ui.cardWaitJob?.cancel()
        ui.cardWaitJob = scope.launch {
            kotlinx.coroutines.delay(waitMs)
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

private fun easeCardBack(ui: IslandInteractionState, scope: CoroutineScope) {
    val start = ui.cardDragX
    ui.cardBackJob?.cancel()
    ui.cardBackJob = scope.launch {
        androidx.compose.animation.core.Animatable(start).animateTo(
            targetValue = 0f,
            animationSpec = swipeEaseBackSpring()
        ) {
            if (ui.pendingCardEnterSign == 0) ui.cardDragX = value
        }
    }
}
