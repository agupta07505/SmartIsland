/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.HoldGesture
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.model.longPressOpensApps
import com.agupta07505.smartisland.ui.expanded.StepAwareVerticalAlignment
import com.agupta07505.smartisland.ui.expanded.StepDragState
import androidx.compose.runtime.mutableStateOf
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressContractTest {

    private fun runnerWith(onTogglePillHidden: () -> Unit): IslandActionRunner {
        // the long press reads only the pill toggle: any other action or view value fails the test
        val actions = IslandActions(
            expand = mockk(),
            card = mockk(),
            sibling = mockk(),
            idle = IdleActions({ error("not read") }, { error("not read") }, onTogglePillHidden),
            pulse = mockk(),
            system = mockk()
        )
        val wiring = IslandWiring(mutableStateOf(actions), mutableStateOf(mockk<IslandViewState>()))
        return IslandActionRunner(mockk(relaxed = true), wiring)
    }

    @Test
    fun longPressDispatch() {
        var toggleToggled = 0
        runnerWith { toggleToggled++ }.runLongPress(SwipeAction.TogglePill)
        assertEquals(1, toggleToggled)

        for (action in SwipeAction.values().filter { it != SwipeAction.TogglePill }) {
            var callbacksToggled = 0
            runnerWith { callbacksToggled++ }.runLongPress(action)
            assertEquals("long press with $action must not toggle the pill", 0, callbacksToggled)
        }
    }

    @Test
    fun longPressShortcutRouting() {
        for (landscape in listOf(false, true)) for (notch in listOf(false, true))
            for (overlaps in listOf(false, true)) for (inBand in listOf(false, true))
                for (content in listOf(false, true)) {
                    assertFalse(longPressOpensApps(landscape, notch, false, overlaps, inBand, content))
                }

        for (notch in listOf(false, true)) for (overlaps in listOf(false, true)) for (inBand in listOf(false, true)) {
            assertTrue(longPressOpensApps(true, notch, true, overlaps, inBand, pillHasContent = true))
            assertFalse(longPressOpensApps(true, notch, true, overlaps, inBand, pillHasContent = false))
        }

        for (inBand in listOf(false, true)) {
            assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = false, touchInCentreBand = inBand))
        }

        // notch mode, or a band that overlaps the cutout: the centre band runs the normal long-press action instead
        assertFalse(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = true))
        assertFalse(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = true))
        assertTrue(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = false))
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = false))
    }

    @Test
    fun longPressByTouchZone() {
        // off the glyph (cutout, centre band) the hold runs the action as before
        val off = HoldGesture.atThreshold(true, false, false, opensApps = false, action = SwipeAction.TogglePill)
        assertEquals(SwipeAction.TogglePill, off.runAction)
        assertFalse(off.glow)
        // on the glyph: no action, a glow, the swipe stays open
        for (opensApps in listOf(false, true)) {
            val glyph = HoldGesture.atThreshold(
                true, false, false, opensApps = opensApps, action = SwipeAction.TogglePill, onGlyph = true
            )
            assertEquals(null, glyph.runAction)
            assertTrue(glyph.glow && glyph.readSwipe)
        }
    }
}

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

class SwipeHapticTest {
    @Test
    fun swipeHapticOwnership() {
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.Expand))
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.Collapse))
        assertFalse(swipeNeedsOwnHaptic(SwipeAction.None))
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.DismissCurrent))
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.NextNotification))
        assertTrue(swipeNeedsOwnHaptic(SwipeAction.PlayPause))
    }

}
