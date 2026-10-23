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

class SplitShellTest {
    @Test
    fun shellIsTheSingleIslandAtZeroAndTheWholeSplitAtOne() {
        assertEquals(400f, SplitShell.width(400f, 540f, 0f), 0.0001f)
        assertEquals(540f, SplitShell.width(400f, 540f, 1f), 0.0001f)
        // Half way along the geometry's run (the raw progress is remapped by SplitMotion.geometry).
        assertEquals(470f, SplitShell.width(400f, 540f, SplitMotion.fromGeometry(0.5f)), 0.0001f)
    }

    @Test
    fun shellIsExactlyTheSingleIslandThroughTheSlowTail() {
        // No few-pixel sliver at the close's end; the tail's progress changes nothing.
        assertEquals(400f, SplitShell.width(400f, 540f, SplitMotion.TAIL), 0f)
        assertEquals(400f, SplitShell.width(400f, 540f, SplitMotion.TAIL / 3), 0f)
        assertTrue(SplitShell.width(400f, 540f, SplitMotion.TAIL + 0.02f) > 400f)
    }

    @Test
    fun progressOutsideZeroToOneIsClamped() {
        assertEquals(400f, SplitShell.width(400f, 540f, -0.2f), 0.0001f)
        assertEquals(540f, SplitShell.width(400f, 540f, 1.3f), 0.0001f)
    }

    @Test
    fun theShellIsTheSingleIslandWhenTheSplitCountsAsGone() {
        // The hand-off to the single surface at GONE_AT has the same width on both sides.
        assertTrue(SplitShell.handoverDone(SplitShell.width(400f, 540f, SplitMotion.GONE_AT), 400f))
    }

    @Test
    fun cardIslandPlusSideIslandFillsTheShellAtEveryProgress() {
        // Card island width (as IslandExpandedContent draws it) + the side island's share = the shell's width.
        val single = 400f
        val natural = 300f
        val side = 140f
        val total = natural + side
        for (i in 0..10) {
            val raw = i / 10f
            // The islands' own widths run on the geometry of the raw progress, like the shell.
            val p = SplitMotion.geometry(raw)
            val card = single + (natural - single) * p
            assertEquals(SplitShell.width(single, total, raw), card + side * p, 0.0001f)
        }
    }

    @Test
    fun handoverEndsWhenTheOwnWidthIsBackAtTheSingleWidth() {
        assertFalse(SplitShell.handoverDone(540f, 400f))
        assertTrue(SplitShell.handoverDone(400.3f, 400f))
    }

    @Test
    fun shellHeightIsTheSingleIslandAtZeroAndTheTallerIslandAtOne() {
        assertEquals(80f, SplitShell.height(80f, 120f, 0f), 0.0001f)
        assertEquals(80f, SplitShell.height(80f, 120f, SplitMotion.TAIL), 0f)
        assertEquals(120f, SplitShell.height(80f, 120f, 1f), 0.0001f)
        assertEquals(100f, SplitShell.height(80f, 120f, SplitMotion.fromGeometry(0.5f)), 0.0001f)
    }
}
