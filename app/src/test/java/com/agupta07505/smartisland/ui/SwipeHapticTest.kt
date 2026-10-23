/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.SwipeAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeHapticTest {
    @Test
    fun expandAndCollapseLeaveTheBuzzToTheStateChange() {
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.Expand))
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.Collapse))
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.None))
    }

    @Test
    fun otherActionsBuzzOnce() {
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.DismissCurrent))
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.NextNotification))
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.PlayPause))
    }
}
