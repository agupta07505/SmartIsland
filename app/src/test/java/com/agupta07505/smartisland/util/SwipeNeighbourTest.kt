/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeNeighbourTest {
    @Test
    fun nextAndPreviousNameTheirSide() {
        assertEquals(1, swipeNeighbourHint(SwipeAction.NextNotification))
        assertEquals(1, swipeNeighbourHint(SwipeAction.NextPrevious))
        assertEquals(-1, swipeNeighbourHint(SwipeAction.PreviousNotification))
    }

    @Test
    fun siblingsAndEventsHaveNoSide() {
        assertEquals(2, swipeNeighbourHint(SwipeAction.NextSibling))
        assertEquals(2, swipeNeighbourHint(SwipeAction.PreviousEvent))
    }

    @Test
    fun theLikelyTargetIsMeasuredFirstAndIdleKeepsItsOrder() {
        assertEquals(listOf(1, -1), neighbourProbeOrder(1))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(-1))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(2))
        assertEquals(listOf(-1, 1), neighbourProbeOrder(0))
    }
}
