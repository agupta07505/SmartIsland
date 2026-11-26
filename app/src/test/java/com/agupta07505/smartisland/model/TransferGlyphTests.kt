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

class TransferGlyphMotionTest {
    private val eps = 0.001f

    @Test
    fun onlyCopyDeleteInstallHaveOwnGlyph() {
        assertTrue(TransferGlyphMotion.hasOwnGlyph(TransferKind.COPY_MOVE))
        assertTrue(TransferGlyphMotion.hasOwnGlyph(TransferKind.DELETE))
        assertTrue(TransferGlyphMotion.hasOwnGlyph(TransferKind.INSTALL))
        assertFalse(TransferGlyphMotion.hasOwnGlyph(TransferKind.DOWNLOAD))
        assertFalse(TransferGlyphMotion.hasOwnGlyph(TransferKind.UPLOAD))
        assertFalse(TransferGlyphMotion.hasOwnGlyph(TransferKind.BACKUP))
    }

    @Test
    fun copyPageSlidesFromSourceToTargetAndFadesAtBothEnds() {
        val start = TransferGlyphMotion.copy(0f)
        assertEquals(0f, start.alpha, eps)
        assertEquals(TransferGlyphMotion.COPY_SOURCE_X, start.x, eps)
        val landed = TransferGlyphMotion.copy(0.70f)
        assertEquals(TransferGlyphMotion.COPY_TARGET_X, landed.x, eps)
        assertEquals(TransferGlyphMotion.COPY_TARGET_Y, landed.y, eps)
        assertEquals(1f, landed.alpha, eps)
        assertTrue(landed.fill > 0.2f)
        assertEquals(0f, TransferGlyphMotion.copy(0.9999f).alpha, 0.01f)
        var lastX = -1f
        for (i in 14..62) {
            val x = TransferGlyphMotion.copy(i / 100f).x
            assertTrue(x >= lastX)
            lastX = x
        }
    }

    @Test
    fun deleteAnimationFrames() {
        assertEquals(0f, TransferGlyphMotion.delete(0f).lidDegrees, eps)
        assertEquals(TransferGlyphMotion.DELETE_LID_OPEN_DEGREES, TransferGlyphMotion.delete(0.30f).lidDegrees, eps)
        assertEquals(0f, TransferGlyphMotion.delete(0.76f).lidDegrees, eps)
        var lastY = -1f
        for (i in 12..60) {
            val y = TransferGlyphMotion.delete(i / 100f).pageY
            assertTrue(y >= lastY)
            lastY = y
        }
        assertEquals(0f, TransferGlyphMotion.delete(0.65f).pageAlpha, eps)

        val calm = TransferGlyphMotion.delete(0.10f)
        val flick = TransferGlyphMotion.delete(0.80f)
        assertEquals(TransferGlyphMotion.DELETE_SWEEP_BASE_ALPHA, calm.sweepAlphaTop, eps)
        assertEquals(1f, flick.sweepAlphaTop, eps)
        assertEquals(0f, flick.sweepShiftTop, eps)
        assertTrue(calm.sweepShiftTop < 0f)

        assertEquals(TransferGlyphMotion.copy(0.25f), TransferGlyphMotion.copy(1.25f))
        assertEquals(TransferGlyphMotion.delete(0.25f).pageY, TransferGlyphMotion.delete(2.25f).pageY, eps)
        assertEquals(TransferGlyphMotion.install(0.25f).fill, TransferGlyphMotion.install(1.25f).fill, eps)

        assertEquals(1f, TransferGlyphMotion.copy(TransferGlyphMotion.COPY_REST_PHASE).alpha, eps)
        assertEquals(0f, TransferGlyphMotion.delete(TransferGlyphMotion.DELETE_REST_PHASE).lidDegrees, eps)
        assertEquals(1f, TransferGlyphMotion.install(TransferGlyphMotion.INSTALL_REST_PHASE).arrowAlpha, eps)
    }

    @Test
    fun installArrowDropsInAndScreenFills() {
        assertEquals(TransferGlyphMotion.INSTALL_ARROW_START_DY, TransferGlyphMotion.install(0f).arrowDy, eps)
        assertEquals(0f, TransferGlyphMotion.install(0.5f).arrowDy, eps)
        assertEquals(0f, TransferGlyphMotion.install(0.30f).fill, 0.5f)
        assertEquals(1f, TransferGlyphMotion.install(0.80f).fill, eps)
        assertEquals(0f, TransferGlyphMotion.install(0.9999f).fill, 0.01f)
    }
}

class TwoShellSwapTest {
    @Test
    fun shellSwapGeometry() {
        assertEquals(0f, TwoShellSwap.grow(0f), 0f)
        assertEquals(1f, TwoShellSwap.grow(1f), 0f)
        assertEquals(0.5f, TwoShellSwap.grow(0.5f), 1e-6f)

        // no second ease on top of the spring: equal steps of progress are equal steps of size
        assertEquals(0.1f, TwoShellSwap.grow(0.1f), 1e-6f)
        assertEquals(0.25f, TwoShellSwap.grow(0.25f), 1e-6f)
        assertEquals(0.9f, TwoShellSwap.grow(0.9f), 1e-6f)
        assertEquals(0.9f, TwoShellSwap.ghostOpen(0.1f), 1e-6f)
        for (i in 1..9) {
            val step = TwoShellSwap.grow((i + 1) / 10f) - TwoShellSwap.grow(i / 10f)
            assertEquals(0.1f, step, 1e-6f)
        }

        for (i in 0..20) {
            val complementary = i / 20f
            assertEquals(1f, TwoShellSwap.grow(complementary) + TwoShellSwap.ghostOpen(complementary), 1e-6f)
        }

        val reversed = 0.62f
        val forwardGrow = TwoShellSwap.grow(reversed)
        val backGrow = TwoShellSwap.grow(reversed)
        assertEquals(forwardGrow, backGrow, 0f)
        assertEquals(TwoShellSwap.ghostOpen(reversed), 1f - backGrow, 1e-6f)

        assertEquals(TwoShellSwap.grow(0.3f), TwoShellSwap.realOpen(true, false, 0.3f), 0f)
        assertEquals(TwoShellSwap.ghostOpen(0.3f), TwoShellSwap.realOpen(true, true, 0.3f), 0f)
    }

    @Test
    fun growIsMonotonicAndClamped() {
        var last = -1f
        for (i in 0..100) {
            val g = TwoShellSwap.grow(i / 100f)
            assertTrue(g >= last)
            last = g
        }
        assertEquals(0f, TwoShellSwap.grow(-3f), 0f)
        assertEquals(1f, TwoShellSwap.grow(4f), 0f)
    }

    @Test fun ghostStartsFullAndMovesToTheCutout() {
        assertEquals(1f, TwoShellSwap.ghostOpen(0f), 0f)
        assertEquals(0f, TwoShellSwap.ghostOpen(1f), 0f)
        assertEquals(1f, TwoShellSwap.scaleAt(0.2f, 1f), 0f)
        assertEquals(0.2f, TwoShellSwap.scaleAt(0.2f, 0f), 1e-6f)
        assertEquals(0f, TwoShellSwap.shiftAt(-120f, 1f), 0f)
        assertEquals(-120f, TwoShellSwap.shiftAt(-120f, 0f), 0f)
        // out of range is held at the ends
        assertEquals(1f, TwoShellSwap.scaleAt(0.2f, 5f), 0f)
        assertEquals(0.2f, TwoShellSwap.scaleAt(0.2f, -5f), 1e-6f)
    }

    @Test
    fun ghostFadeTiming() {
        assertEquals(1f, TwoShellSwap.ghostAlphaFor(false, 0f), 0f)
        assertEquals(1f, TwoShellSwap.ghostAlphaFor(false, TapSwap.CUTOUT_BACK_PROGRESS), 0f)
        assertEquals(0f, TwoShellSwap.ghostAlphaFor(false, 1f), 0f)

        for (opacity in listOf(1f, 0.8f, 0.5f, 0.2f)) {
            var p = 0f
            while (p <= 1f) {
                val g = TwoShellSwap.ghostAlphaFor(false, p, opacity)
                val c = TapSwap.cutoutAlpha(p)
                val covered = 1f - (1f - opacity * g) * (1f - opacity * c)
                if (c < 0.999f) assertEquals("p=$p o=$opacity", opacity, covered, 0.002f)
                p += 0.01f
            }
        }

        assertEquals(0f, TwoShellSwap.ghostAlphaFor(false, 1f), 0f)
        assertEquals(1f, TwoShellSwap.ghostAlphaFor(false, 0f), 0f)
        assertEquals(0f, TwoShellSwap.ghostAlphaFor(true, 0f), 0f)
        assertEquals(1f, TwoShellSwap.ghostAlphaFor(true, 1f), 0f)
    }

    @Test
    fun shellBeforeGhostReady() {
        for (i in 0..10) assertEquals(1f, TwoShellSwap.realOpen(false, false, i / 10f), 0f)
        for (i in 0..10) assertEquals(1f, TwoShellSwap.realOpen(false, true, i / 10f), 0f)
    }

    @Test
    fun theShellNeverDipsBeforeItGrows() {
        // preparing (not ready) at rest, then the parked swap starts from the same size on the ghost
        val preparing = TwoShellSwap.realOpen(false, false, 0f)
        val ghostAtStart = TwoShellSwap.ghostOpenFor(false, 0f)
        assertEquals(preparing, ghostAtStart, 0f)
        var last = ghostAtStart
        for (i in 0..20) {
            val g = TwoShellSwap.ghostOpenFor(false, i / 20f)
            assertTrue(g <= last + 1e-6f)
            last = g
        }
        var grown = 0f
        for (i in 0..20) {
            val r = TwoShellSwap.realOpen(true, false, i / 20f)
            assertTrue(r >= grown - 1e-6f)
            grown = r
        }
    }

    @Test
    fun theTwoShellsAreAlwaysComplementsInBothDirections() {
        for (back in listOf(false, true)) for (i in 0..20) {
            val p = i / 20f
            assertEquals(1f, TwoShellSwap.realOpen(true, back, p) + TwoShellSwap.ghostOpenFor(back, p), 1e-6f)
        }
    }

    @Test
    fun tradingRolesKeepsSilhouette() {
        // the bigger of the 2 shells is what shows: it is the same just before and just after they trade
        for (i in 0..20) {
            val p = i / 20f
            val before = maxOf(TwoShellSwap.realOpen(true, false, p), TwoShellSwap.ghostOpenFor(false, p))
            val after = maxOf(TwoShellSwap.realOpen(true, true, p), TwoShellSwap.ghostOpenFor(true, p))
            assertEquals(before, after, 1e-6f)
        }
    }

    @Test
    fun ghostCardFadeWindow() {
        assertEquals(1f, TwoShellSwap.ghostContentAlpha(false, 0f), 0f)
        // the ghost's outgoing card lingers past the real shell's gap, into the incoming fade-in
        assertTrue(TwoShellSwap.ghostContentAlpha(false, TapSwap.OUTGOING_FADE_END) > 0f)
        assertTrue(TwoShellSwap.ghostContentAlpha(false, TapSwap.INCOMING_FADE_START + 0.05f) > 0f)
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(false, TwoShellSwap.GHOST_CONTENT_FADE_END), 1e-6f)
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(false, TapSwap.INCOMING_FADE_END), 1e-6f)
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(true, 0.1f), 0f)
    }

    @Test
    fun swapContentOpacity() {
        assertEquals(0f, TwoShellSwap.realContentAlpha(true, false, false, TapSwap.INCOMING_FADE_START), 1e-6f)
        assertEquals(1f, TwoShellSwap.realContentAlpha(true, false, false, TapSwap.INCOMING_FADE_END), 1e-6f)

        for (i in 0..20) assertEquals(0f, TwoShellSwap.realContentAlpha(true, false, true, i / 20f), 0f)

        for (i in 0..20) {
            val p = i / 20f
            assertEquals(1f, TwoShellSwap.realContentAlpha(false, false, true, p), 0f)
            assertEquals(TapSwap.incomingContentAlpha(p), TwoShellSwap.realContentAlpha(false, false, false, p), 0f)
        }

        val handOver = 0f
        val before = TwoShellSwap.realContentAlpha(false, false, true, handOver)
        val after = TwoShellSwap.ghostContentAlpha(false, handOver, handOver)
        assertEquals(before, after, 0f)

        assertEquals(1f, TwoShellSwap.realContentAlpha(true, true, false, 0f), 0f)
        assertEquals(0f, TwoShellSwap.realContentAlpha(true, true, false, TapSwap.OUTGOING_FADE_END), 1e-6f)
    }

    @Test
    fun theGhostCardFadesFromTheProgressItWasHandedOverAt() {
        val from = 0f
        assertEquals(1f, TwoShellSwap.ghostContentAlpha(false, from, from), 0f)
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(false, TwoShellSwap.GHOST_CONTENT_FADE_END, from), 1e-6f)
        assertEquals(1f, TwoShellSwap.ghostContentAlpha(false, 0f, from), 0f)
        var last = 1f
        for (i in 0..100) {
            val a = TwoShellSwap.ghostContentAlpha(false, i / 100f, from)
            assertTrue(a <= last + 1e-6f)
            last = a
        }
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(true, from, from), 0f)
    }

    @Test
    fun aHandOverPastTheFadeEndLeavesNothingOnTheGhost() {
        val from = TwoShellSwap.GHOST_CONTENT_FADE_END + 0.1f
        assertEquals(0f, TwoShellSwap.ghostContentAlpha(false, from, from), 0f)
    }

    @Test
    fun theRealCutoutComesBackWhereTheGhostLands() {
        assertEquals(0f, TwoShellSwap.cutoutAlpha(false, 0.5f), 0f)
        assertEquals(1f, TwoShellSwap.cutoutAlpha(false, 1f), 0f)
        assertEquals(0f, TwoShellSwap.cutoutAlpha(true, 0.5f), 0f)
        assertEquals(1f, TwoShellSwap.cutoutAlpha(true, 0f), 0f)
    }

    @Test
    fun cardsCrossFadeWithoutGap() {
        // at every progress between the first fade and the last at least one card is visible
        var p = TapSwap.OUTGOING_FADE_START
        while (p < TapSwap.INCOMING_FADE_END) {
            val ghost = TwoShellSwap.ghostContentAlpha(false, p)
            val real = TwoShellSwap.realContentAlpha(true, false, false, p)
            assertTrue("blank at p=$p", maxOf(ghost, real) > 0f)
            p += 0.01f
        }
        // the incoming card starts while the ghost's card is still well visible (a real overlap, not just a touch)
        assertTrue(TwoShellSwap.ghostContentAlpha(false, TapSwap.INCOMING_FADE_START) > 0.25f)
        assertTrue(TwoShellSwap.GHOST_CONTENT_FADE_END - TapSwap.INCOMING_FADE_START >= 0.2f)
        // the ghost's card is gone before the incoming card is fully in and before the cutout returns
        assertTrue(TwoShellSwap.GHOST_CONTENT_FADE_END <= TapSwap.INCOMING_FADE_END)
        assertTrue(TwoShellSwap.GHOST_CONTENT_FADE_END <= TapSwap.CUTOUT_BACK_PROGRESS)
    }

    @Test
    fun radiusRunsFromTheCutoutsToTheCards() {
        assertEquals(12f, TwoShellSwap.radiusAt(12f, 34f, 0f), 0.0001f)
        assertEquals(34f, TwoShellSwap.radiusAt(12f, 34f, 1f), 0.0001f)
        assertEquals(23f, TwoShellSwap.radiusAt(12f, 34f, 0.5f), 0.0001f)
        assertEquals(34f, TwoShellSwap.radiusAt(12f, 34f, 2f), 0.0001f)
    }

    @Test
    fun shellWidthInterpolation() {
        assertEquals(1f, TwoShellSwap.widthOpen(true, 1f), 0f)
        assertEquals(0f, TwoShellSwap.widthOpen(true, 0f), 0f)
        assertTrue(TwoShellSwap.widthOpen(true, 0.5f) < 0.5f)
        // the dip under the cutout's width stays a few percent of the travel
        var low = 0f
        for (i in 0..200) low = minOf(low, TwoShellSwap.widthOpen(true, i / 200f))
        assertTrue(low > -0.06f)

        assertEquals(0f, TwoShellSwap.widthOpen(false, 0f), 0f)
        assertEquals(1f, TwoShellSwap.widthOpen(false, 1f), 0f)
        assertTrue(TwoShellSwap.widthOpen(false, 0.5f) > 0.5f)
        for (i in 0..200) assertTrue(TwoShellSwap.widthOpen(false, i / 200f) <= 1f)

        for (i in 0..10) {
            val o = i / 10f
            assertEquals(o, TwoShellSwap.widthOpen(false, o, CollapseMotion.SETTLE_DAMPING), 1e-6f)
            assertEquals(o, TwoShellSwap.widthOpen(true, o, CollapseMotion.SETTLE_DAMPING), 1e-6f)
        }
    }
}
