/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.tanh

class SwipeLock(private val lengthMs: Long = SwipeArming.LOCK_MS) {
    private var lockedUntilMs: Long = NEVER

    fun start(nowMs: Long) {
        lockedUntilMs = nowMs + lengthMs
    }

    fun isLocked(nowMs: Long): Boolean = nowMs < lockedUntilMs

    fun remainingMs(nowMs: Long): Long = if (isLocked(nowMs)) lockedUntilMs - nowMs else 0L

    fun clear() {
        lockedUntilMs = NEVER
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE
    }
}

/**
 * How long the lock cue stays fully shown: the lock's [remainingMs] minus the fade in ([inMs]) and out ([outMs]), so
 * the cue is gone the moment the lock ends. Never negative
 */
fun lockCueHoldMs(remainingMs: Long, inMs: Long, outMs: Long): Long = (remainingMs - inMs - outMs).coerceAtLeast(0L)

class SwipeArming(
    private val sensitivity: GestureSensitivity,
    private val threshold: Float,
    private val disarmFraction: Float = DEFAULT_DISARM_FRACTION,
    private val lock: SwipeLock = SwipeLock(),
    private val horizontalThreshold: Float = threshold
) {
    var direction: PillSwipeDirection? = null
        private set

    var enteredLock: Boolean = false
        private set

    /**
     * Which armed directions start the lock when pulled back. A direction that did nothing visible (a side set to
     * "None") was never "armed" for the user, so pulling it back must not lock
     */
    var lockWhen: (PillSwipeDirection) -> Boolean = { true }

    /** Feeds the drag so far at [nowMs]. True when armed / disarmed or the direction flipped. */
    fun update(dx: Float, dy: Float, nowMs: Long): Boolean {
        val before = direction
        enteredLock = false
        if (lock.isLocked(nowMs)) {
            direction = null
            return before != null
        }
        val now = sensitivity.pillSwipe(dx, dy, threshold, horizontalThreshold)
        direction = when {
            now != null -> now
            before != null && along(before, dx, dy) >=
                (if (before == PillSwipeDirection.Left
                    || before == PillSwipeDirection.Right) horizontalThreshold else threshold) * disarmFraction ->
                    before
            else -> null
        }
        if (before != null && direction == null && lockWhen(before)) {
            lock.start(nowMs)
            enteredLock = true
        }
        return direction != before
    }

    fun reset() {
        direction = null
        enteredLock = false
    }

    private fun along(d: PillSwipeDirection, dx: Float, dy: Float): Float = when (d) {
        PillSwipeDirection.Left -> -dx
        PillSwipeDirection.Right -> dx
        PillSwipeDirection.Up -> -dy
        PillSwipeDirection.Down -> dy
    }

    companion object {
        const val DEFAULT_DISARM_FRACTION = 0.75f

        const val LOCK_MS = 400L
    }
}

/** Running [action] on the collapsed pill puts a different card in it (so the new one slides in). */
fun swipeChangesCard(action: SwipeAction): Boolean = when (action) {
    SwipeAction.NextPrevious, SwipeAction.NextNotification, SwipeAction.PreviousNotification,
    SwipeAction.NextEvent, SwipeAction.PreviousEvent,
    SwipeAction.NextSibling, SwipeAction.PreviousSibling -> true
    else -> false
}

const val PILL_CONTENT_FOLLOW = 0.6f

fun softFollow(drag: Float, slope: Float, max: Float): Float =
    if (max <= 0f) 0f else max * tanh(drag * slope / max)
