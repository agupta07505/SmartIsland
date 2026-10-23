/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDragTest {
    @Test
    fun progressRunsFromSlopOverTheRange() {
        assertEquals(0f, StepDrag.progress(5f, 10f, 100f), 0.0001f)
        assertEquals(0.5f, StepDrag.progress(60f, 10f, 100f), 0.0001f)
        assertEquals(1f, StepDrag.progress(500f, 10f, 100f), 0.0001f)
    }

    @Test
    fun noRangeNeverDividesByZero() {
        assertEquals(0f, StepDrag.progress(50f, 10f, 0f), 0f)
    }

    @Test
    fun commitsOnlyPastThePoint() {
        assertFalse(StepDrag.commits(StepDrag.COMMIT_PROGRESS - 0.01f))
        assertTrue(StepDrag.commits(StepDrag.COMMIT_PROGRESS))
    }

    @Test
    fun theTwoStepsTradePlacesWithinTheFirstPartOfTheMove() {
        assertEquals(1f, StepDrag.oldStepAlpha(0f), 0.0001f)
        assertEquals(0f, StepDrag.newStepAlpha(0f), 0.0001f)
        assertEquals(0f, StepDrag.oldStepAlpha(StepDrag.CROSSFADE_END), 0.0001f)
        assertEquals(1f, StepDrag.newStepAlpha(StepDrag.CROSSFADE_END), 0.0001f)
        assertEquals(0f, StepDrag.oldStepAlpha(1f), 0f)
        assertEquals(1f, StepDrag.newStepAlpha(1f), 0f)
    }

    @Test
    fun theTwoAlphasAlwaysAddUpToOne() {
        for (i in 0..20) {
            val p = i / 20f
            assertEquals(1f, StepDrag.oldStepAlpha(p) + StepDrag.newStepAlpha(p), 0.0001f)
        }
    }

    @Test
    fun heightRunsWithTheProgress() {
        assertEquals(100f, StepDrag.heightDp(100f, 200f, 0f), 0.0001f)
        assertEquals(150f, StepDrag.heightDp(100f, 200f, 0.5f), 0.0001f)
        assertEquals(200f, StepDrag.heightDp(100f, 200f, 1f), 0.0001f)
    }

    @Test
    fun aDragBuzzesOnceWhenItReachesTheEndAndNotAgainAtTheLift() {
        val h = StepDragHaptic()
        assertFalse(h.onProgress(0.5f))
        assertFalse(h.onProgress(0.99f))
        assertTrue(h.onProgress(1f))
        // Staying at the end, pulling back and returning: still only the one buzz.
        assertFalse(h.onProgress(1f))
        assertFalse(h.onProgress(0.3f))
        assertFalse(h.onProgress(1f))
        assertFalse(h.onLift(committed = true))
    }

    @Test
    fun aStepFinishedBeforeTheEndBuzzesOnceAtTheLift() {
        val h = StepDragHaptic()
        h.onProgress(0.6f)
        assertTrue(h.onLift(committed = true))
        assertFalse(h.onLift(committed = true))
    }

    @Test
    fun aShortPullThatGoesBackNeverBuzzesAtTheLift() {
        val h = StepDragHaptic()
        h.onProgress(0.2f)
        assertFalse(h.onLift(committed = false))
    }

    @Test
    fun smoothingMovesTowardTheTargetWithoutOvershoot() {
        var shown = 0f
        var last = shown
        repeat(60) {
            shown = StepDrag.smoothed(shown, 1f, 16f)
            assertTrue(shown >= last)
            assertTrue(shown <= 1f)
            last = shown
        }
        assertEquals(1f, shown, 0.0001f)
    }

    @Test
    fun smoothingTakesTheSameTimeAtAnyFrameRate() {
        var at60 = 0f
        repeat(6) { at60 = StepDrag.smoothed(at60, 1f, 1000f / 60f) }
        var at120 = 0f
        repeat(12) { at120 = StepDrag.smoothed(at120, 1f, 1000f / 120f) }
        assertEquals(at60, at120, 0.01f)
    }

    @Test
    fun smoothingLandsExactlyWhenCloseAndDoesNothingWithoutTime() {
        assertEquals(0.5f, StepDrag.smoothed(0.4995f, 0.5f, 16f), 0f)
        assertEquals(0.3f, StepDrag.smoothed(0.3f, 0.9f, 0f), 0f)
    }

    @Test
    fun smoothingFollowsAPullBackToo() {
        val next = StepDrag.smoothed(0.8f, 0.2f, 16f)
        assertTrue(next < 0.8f && next > 0.2f)
    }
}
