/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeArmingTest {
    private val t = 16f
    private fun arming(
        s: GestureSensitivity = GestureSensitivity.NORMAL,
        disarm: Float = SwipeArming.DEFAULT_DISARM_FRACTION,
        lock: SwipeLock = SwipeLock()
    ) = SwipeArming(s, t, disarm, lock)

    @Test
    fun armsOncePastTheThresholdAndReportsTheChangeOnce() {
        val a = arming()
        assertFalse(a.update(-10f, 0f, 0L))
        assertNull(a.direction)
        assertTrue(a.update(-17f, 1f, 0L))
        assertEquals(PillSwipeDirection.Left, a.direction)
        // Still armed, nothing changed.
        assertFalse(a.update(-20f, 2f, 0L))
    }

    @Test
    fun pullingBackToTheStartCancelsTheSwipe() {
        val a = arming()
        a.update(-30f, 0f, 0L)
        assertEquals(PillSwipeDirection.Left, a.direction)
        assertTrue(a.update(-3f, 0f, 0L))
        assertNull(a.direction)
    }

    @Test
    fun jitterJustUnderTheThresholdDoesNotFlipTheState() {
        val a = arming()
        a.update(-17f, 0f, 0L)
        // 14 dp is under the 16 dp threshold but above 75 % of it (12): still armed, no flicker.
        assertFalse(a.update(-14f, 0f, 0L))
        assertEquals(PillSwipeDirection.Left, a.direction)
        // 11 dp is under 75 %: cancelled.
        assertTrue(a.update(-11f, 0f, 0L))
        assertNull(a.direction)
    }

    @Test
    fun expandedStyleArmingDisarmsAtTheThreshold() {
        val a = arming(disarm = 1f)
        a.update(0f, -17f, 0L)
        assertEquals(PillSwipeDirection.Up, a.direction)
        assertTrue(a.update(0f, -15f, 0L))
        assertNull(a.direction)
    }

    @Test
    fun crossingToTheOtherSideFlipsTheDirection() {
        val a = arming()
        a.update(-30f, 0f, 0L)
        assertTrue(a.update(30f, 0f, 0L))
        assertEquals(PillSwipeDirection.Right, a.direction)
    }

    @Test
    fun lowSensitivityNeverArmsOnADiagonal() {
        val s = GestureSensitivity.LOW
        val th = s.scaled(16f)
        val a = SwipeArming(s, th)
        assertFalse(a.update(th + 4f, th + 2f, 0L))
        assertNull(a.direction)
        assertTrue(a.update(th * 2f, th * 0.3f, 0L))
        assertEquals(PillSwipeDirection.Right, a.direction)
    }

    @Test
    fun resetForgetsTheArmedDirection() {
        val a = arming()
        a.update(0f, 40f, 0L)
        assertEquals(PillSwipeDirection.Down, a.direction)
        a.reset()
        assertNull(a.direction)
    }

    @Test
    fun onlyCardSwitchingActionsBringACardIn() {
        assertTrue(swipeChangesCard(SwipeAction.NextNotification))
        assertTrue(swipeChangesCard(SwipeAction.PreviousNotification))
        assertTrue(swipeChangesCard(SwipeAction.NextPrevious))
        assertTrue(swipeChangesCard(SwipeAction.NextEvent))
        assertTrue(swipeChangesCard(SwipeAction.PreviousEvent))
        assertTrue(swipeChangesCard(SwipeAction.NextSibling))
        assertTrue(swipeChangesCard(SwipeAction.PreviousSibling))
        assertFalse(swipeChangesCard(SwipeAction.DismissCurrent))
        assertFalse(swipeChangesCard(SwipeAction.Expand))
        assertFalse(swipeChangesCard(SwipeAction.NextTrack))
        assertFalse(swipeChangesCard(SwipeAction.None))
    }

    @Test
    fun contentFollowsTheFingerButStopsAtTheLimit() {
        assertEquals(0f, pillContentFollow(0f, 40f), 0f)
        assertEquals(12f, pillContentFollow(20f, 40f), 0.001f)
        assertEquals(-12f, pillContentFollow(-20f, 40f), 0.001f)
        assertEquals(40f, pillContentFollow(500f, 40f), 0f)
        assertEquals(-40f, pillContentFollow(-500f, 40f), 0f)
    }

    @Test
    fun expandedCardFollowsTheFingerButStopsAtTheLimit() {
        assertEquals(0f, expandedCardFollow(0f, 100f), 0f)
        assertEquals(42.5f, expandedCardFollow(50f, 100f), 0.001f)
        assertEquals(-42.5f, expandedCardFollow(-50f, 100f), 0.001f)
        assertEquals(100f, expandedCardFollow(900f, 100f), 0f)
        assertEquals(-100f, expandedCardFollow(-900f, 100f), 0f)
    }

    @Test
    fun expandedCardSlideLimitScalesWithTheIslandWidthWithinBounds() {
        assertEquals(24f, expandedCardMaxSlideDp(40f), 0f)
        assertEquals(90f, expandedCardMaxSlideDp(300f), 0.001f)
        assertEquals(120f, expandedCardMaxSlideDp(900f), 0f)
    }

    // The short lock replaces the 25 % longer pull

    @Test
    fun pullingAnArmedSwipeBackStartsTheLockAndTheSameDistanceDoesNotRearmDuringIt() {
        val lock = SwipeLock()
        val a = arming(lock = lock)
        assertTrue(a.update(-17f, 0f, 0L))
        assertFalse(a.enteredLock)
        // Pulled back into the dead zone: cancelled, and the lock starts.
        assertTrue(a.update(-5f, 0f, 100L))
        assertNull(a.direction)
        assertTrue(a.enteredLock)
        assertTrue(lock.isLocked(101L))
        // Pushing out again inside the lock: no arming, no change reported.
        assertFalse(a.update(-30f, 0f, 200L))
        assertNull(a.direction)
        assertFalse(a.enteredLock)
        // The lock ends 400 ms after it started (at 500): the same push arms at once, with the normal distance.
        assertNull(a.run { update(-30f, 0f, 499L); direction })
        assertTrue(a.update(-17f, 0f, 500L))
        assertEquals(PillSwipeDirection.Left, a.direction)
    }

    @Test
    fun theLockOutlivesALiftAndANewTouch() {
        val lock = SwipeLock()
        val first = arming(lock = lock)
        first.update(-17f, 0f, 0L)
        first.update(-3f, 0f, 50L)
        assertTrue(lock.isLocked(60L))
        // A new touch gets its own SwipeArming but shares the lock.
        val second = arming(lock = lock)
        assertFalse(second.update(-30f, 0f, 200L))
        assertNull(second.direction)
        assertTrue(second.update(-30f, 0f, 450L))
        assertEquals(PillSwipeDirection.Left, second.direction)
    }

    @Test
    fun resetKeepsTheLock() {
        val lock = SwipeLock()
        val a = arming(lock = lock)
        a.update(-17f, 0f, 0L)
        a.update(0f, 0f, 10L)
        a.reset()
        assertTrue(lock.isLocked(20L))
        assertFalse(a.update(-30f, 0f, 20L))
    }

    @Test
    fun aSwipeThatWasNeverArmedDoesNotLock() {
        val lock = SwipeLock()
        val a = arming(lock = lock)
        a.update(-10f, 0f, 0L)
        a.update(-2f, 0f, 10L)
        assertFalse(a.enteredLock)
        assertFalse(lock.isLocked(20L))
        assertTrue(a.update(-17f, 0f, 30L))
    }

    @Test
    fun aDirectionThatDidNothingVisibleDoesNotLockWhenPulledBack() {
        val lock = SwipeLock()
        val a = arming(lock = lock)
        a.lockWhen = { it != PillSwipeDirection.Left }
        a.update(-17f, 0f, 0L)
        a.update(-3f, 0f, 10L)
        assertNull(a.direction)
        assertFalse(a.enteredLock)
        assertFalse(lock.isLocked(20L))
    }

    @Test
    fun flippingToTheOtherSideDoesNotLock() {
        val lock = SwipeLock()
        val a = arming(lock = lock)
        a.update(-30f, 0f, 0L)
        a.update(30f, 0f, 10L)
        assertEquals(PillSwipeDirection.Right, a.direction)
        assertFalse(lock.isLocked(20L))
    }

    @Test
    fun lockLengthIsOneConstantInTheTrySizedRange() {
        assertTrue(SwipeArming.LOCK_MS in 300L..500L)
        val lock = SwipeLock()
        lock.start(1000L)
        assertEquals(SwipeArming.LOCK_MS, lock.remainingMs(1000L))
        assertTrue(lock.isLocked(1000L + SwipeArming.LOCK_MS - 1))
        assertFalse(lock.isLocked(1000L + SwipeArming.LOCK_MS))
        assertEquals(0L, lock.remainingMs(5000L))
        lock.clear()
        assertFalse(lock.isLocked(1001L))
    }

    @Test
    fun aFreshLockIsNotLockedAtTimeZeroOrBelow() {
        val lock = SwipeLock()
        assertFalse(lock.isLocked(0L))
        assertFalse(lock.isLocked(-5L))
    }

    @Test
    fun theCueIsGoneWhenTheLockEnds() {
        // In 60 + hold + out 100 = what is left of the lock.
        assertEquals(240L, lockCueHoldMs(400L, 60L, 100L))
        assertEquals(60L + 240L + 100L, 400L)
        // A lock that is nearly over: no hold, never negative.
        assertEquals(0L, lockCueHoldMs(120L, 60L, 100L))
        assertEquals(0L, lockCueHoldMs(0L, 60L, 100L))
    }

    @Test
    fun softFollowStartsLikeTheLinearFollowAndNeverReachesTheLimit() {
        assertEquals(0f, softFollow(0f, 0.6f, 40f), 0f)
        // Small drags match the plain follow (drag x slope) closely.
        assertEquals(0.6f, softFollow(1f, 0.6f, 40f), 0.001f)
        // Odd: the other direction mirrors it.
        assertEquals(-softFollow(30f, 0.6f, 40f), softFollow(-30f, 0.6f, 40f), 0.0001f)
        // Always below the limit, and still growing (no dead stop).
        var last = 0f
        for (d in listOf(20f, 60f, 120f, 300f, 600f)) {
            val v = softFollow(d, 0.6f, 40f)
            assertTrue(v < 40.0001f && v > last)
            last = v
        }
        assertEquals(0f, softFollow(50f, 0.6f, 0f), 0f)
    }
}
