/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.StepAwareVerticalAlignment
import com.agupta07505.smartisland.ui.expanded.StepDragState
import org.junit.Assert.assertEquals
import org.junit.Test

class StepAwareAlignmentTest {
    @Test
    fun centresAtRestLikeCenterVertically() {
        val align = StepAwareVerticalAlignment(StepDragState())
        assertEquals(40, align.align(100, 180))
        assertEquals(-50, align.align(200, 100))
    }

    @Test
    fun pinsToTheTopWhileAStepFollowsTheFinger() {
        val state = StepDragState()
        val align = StepAwareVerticalAlignment(state)
        state.active = true
        assertEquals(0, align.align(100, 180))
        assertEquals(0, align.align(200, 100))
    }

    @Test
    fun centresAgainOnceTheStepIsHandedBack() {
        val state = StepDragState()
        val align = StepAwareVerticalAlignment(state)
        state.active = true
        state.reset()
        assertEquals(40, align.align(100, 180))
    }
}
