/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerSwapTest {
    @Test
    fun pickerCardOpacity() {
        assertEquals(1f, PickerSwap.cardAlpha(0f), 0.0001f)
        assertEquals(0f, PickerSwap.pickerAlpha(0f), 0.0001f)
        assertEquals(0f, PickerSwap.cardAlpha(1f), 0.0001f)
        assertEquals(1f, PickerSwap.pickerAlpha(1f), 0.0001f)
        assertEquals(1f, PickerSwap.cardScale(0f), 0.0001f)
        assertEquals(PickerSwap.END_SCALE, PickerSwap.cardScale(1f), 0.0001f)
        assertEquals(PickerSwap.END_SCALE, PickerSwap.pickerScale(0f), 0.0001f)
        assertEquals(1f, PickerSwap.pickerScale(1f), 0.0001f)

        var p = 0f
        while (p <= 1f) {
            assertTrue(PickerSwap.cardAlpha(p) + PickerSwap.pickerAlpha(p) <= 1.0001f)
            p += 0.01f
        }

        // 1 progress value drives both directions, so there is nothing direction specific to compare but the length
        assertTrue(PickerSwap.lengthMs(opening = false) <= PickerSwap.lengthMs(opening = true))
        assertEquals(PickerSwap.cardAlpha(0.4f), PickerSwap.cardAlpha(0.4f), 0f)
    }

    @Test
    fun layersStayComposedOnlyWhileTheyAreVisibleOrResting() {
        assertTrue(PickerSwap.cardLayerShown(progress = 0f, pickerOpen = false))
        assertFalse(PickerSwap.pickerLayerShown(progress = 0f, pickerOpen = false))
        // closing: the picker is no longer "open" but is still fading out; the card is already back
        assertTrue(PickerSwap.pickerLayerShown(progress = 0.5f, pickerOpen = false))
        assertTrue(PickerSwap.cardLayerShown(progress = 0.5f, pickerOpen = false))
        // open and settled: only the picker
        assertFalse(PickerSwap.cardLayerShown(progress = 1f, pickerOpen = true))
        assertTrue(PickerSwap.pickerLayerShown(progress = 1f, pickerOpen = true))
    }
}

class ShellStateGuardTest {
    private fun reason(
        expanded: Boolean = true,
        atRest: Boolean = true,
        overlayFlag: Boolean = false,
        splitWanted: Boolean = false,
        drawn: Boolean = false,
        alpha: Float = 1f,
        radius: Float = 28f
    ) = ShellStateGuard.stuckReason(expanded, atRest, overlayFlag, splitWanted, drawn, alpha, radius)

    @Test
    fun clipAndSurfaceNeedFlagAndContent() {
        assertTrue(ShellStateGuard.twoIslandsDrawn(true, true))
        assertFalse(ShellStateGuard.twoIslandsDrawn(true, false))
        assertFalse(ShellStateGuard.twoIslandsDrawn(false, true))
        assertFalse(ShellStateGuard.twoIslandsDrawn(false, false))
    }

    @Test
    fun healthyShellsNeedNoRecovery() {
        assertNull(reason())
        assertNull(reason(overlayFlag = true, splitWanted = true, drawn = true, alpha = 0f))
        assertNull(reason(expanded = false, alpha = 0f, radius = 0f))
        assertNull(reason(atRest = false, alpha = 0f, radius = 0f))
    }

    @Test
    fun singleShellRecovery() {
        val stuck = reason(overlayFlag = true, splitWanted = false, drawn = true, alpha = 0f)
        assertEquals(ShellStateGuard.REASON_HOLD, stuck)
        assertTrue(ShellStateGuard.canForceSingle(stuck))

        val missingSurface = reason(alpha = 0f)
        assertEquals(ShellStateGuard.REASON_NO_SURFACE, missingSurface)
        assertTrue(ShellStateGuard.canForceSingle(missingSurface))
        // still fading in: not yet counted as back
        assertEquals(ShellStateGuard.REASON_NO_SURFACE, reason(alpha = 0.9f))
        assertNull(reason(alpha = 0.99f))

        val squareCorners = reason(radius = 0f)
        assertEquals(ShellStateGuard.REASON_NO_RADIUS, squareCorners)
        assertFalse(ShellStateGuard.canForceSingle(squareCorners))
        assertFalse(ShellStateGuard.canForceSingle(null))
    }

    @Test
    fun holdGetsItsOwnCloseOnTopOfTheRestTime() {
        assertEquals(ShellStateGuard.REST_MS, ShellStateGuard.waitMs(false, 240))
        assertEquals(ShellStateGuard.REST_MS + 240L, ShellStateGuard.waitMs(true, 240))
    }
}

class ShellSwapTest {
    private val idle = ShellSwap.State(expanded = "a")

    @Test
    fun activeSwapTaps() {
        val initial = ShellSwap.tap(idle, "b")
        assertEquals("a", initial.expanded)
        assertEquals("b", initial.incoming)
        assertEquals(1f, initial.target, 0f)
        assertTrue(initial.swapping)

        assertEquals(idle, ShellSwap.tap(idle, "a"))

        val reversed = ShellSwap.tap(ShellSwap.tap(idle, "b"), "a")
        assertEquals(0f, reversed.target, 0f)
        assertEquals("b", reversed.incoming)
        val again = ShellSwap.tap(reversed, "b")
        assertEquals(1f, again.target, 0f)

        var queued = ShellSwap.tap(idle, "b")
        queued = ShellSwap.tap(queued, "c")
        queued = ShellSwap.tap(queued, "d")
        assertEquals("d", queued.pending)
        assertEquals("b", queued.incoming)
        assertEquals(1f, queued.target, 0f)

        val settled = ShellSwap.settle(ShellSwap.tap(idle, "b"), 1f)
        assertEquals(ShellSwap.State(expanded = "b"), settled)
        assertFalse(settled.swapping)
    }

    @Test
    fun swapTapRouting() {
        val growing = ShellSwap.tap(idle, "b")
        val repeated = ShellSwap.tap(growing, "b")
        assertEquals(growing, repeated)
        assertEquals(ShellSwap.TapEffect.None, ShellSwap.effect(growing, repeated))

        val reversed = ShellSwap.tap(growing, "a")
        assertEquals(ShellSwap.TapEffect.Retarget, ShellSwap.effect(growing, reversed))

        val pending = ShellSwap.tap(growing, "c")
        assertEquals(ShellSwap.TapEffect.Remember, ShellSwap.effect(growing, pending))
        assertEquals(ShellSwap.TapEffect.None, ShellSwap.effect(pending, ShellSwap.tap(pending, "c")))

        assertEquals(ShellSwap.TapEffect.None, ShellSwap.effect(idle, ShellSwap.tap(idle, "a")))
    }

    @Test
    fun rapidTapResolution() {
        var burst = idle
        val keys = listOf("b", "a", "b", "a", "b")
        keys.forEach { burst = ShellSwap.tap(burst, it) }
        assertEquals(1f, burst.target, 0f)
        assertEquals("b", burst.incoming)

        var queued = idle
        listOf("b", "c", "a", "b", "c", "d").forEach { queued = ShellSwap.tap(queued, it) }
        // b starts, c remembered, a turns around (drops c), b again, c remembered, d replaces c
        assertEquals(1f, queued.target, 0f)
        assertEquals("d", queued.pending)
    }

    @Test
    fun aTurnedAroundSwapLeavesThingsAsTheyWere() {
        val s = ShellSwap.settle(ShellSwap.tap(ShellSwap.tap(idle, "b"), "a"), 0f)
        assertEquals(idle, s)
    }

    @Test
    fun aSwapStillRunningIsNotSettled() {
        val s = ShellSwap.tap(idle, "b")
        assertEquals(s, ShellSwap.settle(s, 0.6f))
        assertFalse(ShellSwap.settled(s, 0.999f - 0.01f))
        assertTrue(ShellSwap.settled(s, 1f))
    }

    @Test
    fun theRememberedTapStartsWhenTheSwapHasSettled() {
        var s = ShellSwap.tap(idle, "b")
        s = ShellSwap.tap(s, "c")
        s = ShellSwap.settle(s, 1f)
        assertEquals("b", s.expanded)
        assertEquals("c", s.incoming)
        assertEquals(1f, s.target, 0f)
        assertNull(s.pending)
    }

    @Test
    fun pendingSwapTaps() {
        var stale = ShellSwap.tap(idle, "b")
        stale = ShellSwap.tap(stale, "c")
        stale = stale.copy(pending = "b")
        assertEquals(ShellSwap.State(expanded = "b"), ShellSwap.settle(stale, 1f))

        val settled = ShellSwap.tap(ShellSwap.tap(idle, "b"), "c")
        assertEquals("c", ShellSwap.nextAfterSettle(settled))

        assertNull(ShellSwap.nextAfterSettle(ShellSwap.tap(idle, "b")))

        val latestRunning = ShellSwap.tap(ShellSwap.State(expanded = "a"), "b")
        val withThird = ShellSwap.tap(latestRunning, "c")
        assertEquals("c", withThird.pending)
        val newer = ShellSwap.withoutPending(withThird)
        assertEquals(null, newer.pending)
        // the swap itself is untouched: same direction, same events
        assertEquals(withThird.target, newer.target)
        assertEquals(withThird.incoming, newer.incoming)
        assertEquals(null, ShellSwap.nextAfterSettle(newer.copy(target = 1f)))

        val emptyRunning = ShellSwap.tap(ShellSwap.State(expanded = "a"), "b")
        assertEquals(emptyRunning, ShellSwap.withoutPending(emptyRunning))
    }

    @Test
    fun turningAroundDropsTheRememberedTap() {
        var s = ShellSwap.tap(idle, "b")
        s = ShellSwap.tap(s, "c")
        s = ShellSwap.tap(s, "a")
        assertNull(s.pending)
    }

    @Test
    fun queuedTapDestination() {
        // swap a -> b, third tap on... b (the event that ends up expanded): nothing to start
        val stale = ShellSwap.State(expanded = "a", incoming = "b", target = 1f, pending = "b")
        assertNull(ShellSwap.nextAfterSettle(stale))

        // turned around (target 0): "a" stays expanded, so a remembered "a" has nothing to start
        val current = ShellSwap.State(expanded = "a", incoming = "b", target = 0f, pending = "a")
        assertNull(ShellSwap.nextAfterSettle(current))
        // ... but a remembered "c" still starts
        assertEquals("c", ShellSwap.nextAfterSettle(current.copy(pending = "c")))
    }

    @Test
    fun shellTouchReversesSwap() {
        val s = ShellSwap.tap(ShellSwap.tap(idle, "b"), "c")
        val t = ShellSwap.turnedAround(s)
        assertEquals(0f, t.target, 0f)
        assertNull(t.pending)
        assertEquals(1f, ShellSwap.turnedAround(t).target, 0f)
    }

    private val running = ShellSwap.tap(ShellSwap.State(expanded = "a"), "b")

    @Test
    fun tapDestinations() {
        assertEquals(ShellSwap.Intent.Flip, ShellSwap.intentOf(running, "a"))

        assertEquals(ShellSwap.Intent.None, ShellSwap.intentOf(running, "b"))

        val turned = ShellSwap.turnedAround(running)
        // headed back to "a": a second tap on "a" is a no-op, only a tap on "b" turns it again
        assertEquals(ShellSwap.Intent.None, ShellSwap.intentOf(turned, "a"))
        assertEquals(ShellSwap.Intent.Flip, ShellSwap.intentOf(turned, "b"))

        val intent = ShellSwap.intentOf(running, "c")
        assertEquals(ShellSwap.Intent.Third(ShellSwap.tap(running, "c")), intent)
        assertEquals("c", (intent as ShellSwap.Intent.Third).after.pending)

        assertEquals(ShellSwap.Intent.Flip, ShellSwap.intentOf(running, null))
        assertEquals(ShellSwap.Intent.Flip, ShellSwap.intentOf(null, "a"))
        assertEquals(ShellSwap.Intent.Flip, ShellSwap.intentOf(null, null))
    }
}

class SplitShellTest {
    @Test
    fun splitShellWidth() {
        assertEquals(400f, SplitShell.width(400f, 540f, 0f), 0.0001f)
        assertEquals(540f, SplitShell.width(400f, 540f, 1f), 0.0001f)
        // half way along the geometry's run (the raw progress is remapped by SplitMotion.geometry)
        assertEquals(470f, SplitShell.width(400f, 540f, SplitMotion.fromGeometry(0.5f)), 0.0001f)

        // no few-pixel sliver at the close's end; the tail's progress changes nothing
        assertEquals(400f, SplitShell.width(400f, 540f, SplitMotion.TAIL), 0f)
        assertEquals(400f, SplitShell.width(400f, 540f, SplitMotion.TAIL / 3), 0f)
        assertTrue(SplitShell.width(400f, 540f, SplitMotion.TAIL + 0.02f) > 400f)

        assertEquals(400f, SplitShell.width(400f, 540f, -0.2f), 0.0001f)
        assertEquals(540f, SplitShell.width(400f, 540f, 1.3f), 0.0001f)

        // the hand-off to the single surface at GONE_AT has the same width on both sides
        assertTrue(SplitShell.handoverDone(SplitShell.width(400f, 540f, SplitMotion.GONE_AT), 400f))

        // card island width (as IslandExpandedContent draws it) + the side island's share = the shell's width
        val combinedSingle = 400f
        val natural = 300f
        val side = 140f
        val total = natural + side
        for (i in 0..10) {
            val combinedRaw = i / 10f
            // the islands' own widths run on the geometry of the raw progress, like the shell
            val p = SplitMotion.geometry(combinedRaw)
            val card = combinedSingle + (natural - combinedSingle) * p
            assertEquals(SplitShell.width(combinedSingle, total, combinedRaw), card + side * p, 0.0001f)
        }
    }

    @Test
    fun handoverEndsWhenTheOwnWidthIsBackAtTheSingleWidth() {
        assertFalse(SplitShell.handoverDone(540f, 400f))
        assertTrue(SplitShell.handoverDone(400.3f, 400f))
    }

    @Test
    fun shellHeightEndpoints() {
        assertEquals(80f, SplitShell.height(80f, 120f, 0f), 0.0001f)
        assertEquals(80f, SplitShell.height(80f, 120f, SplitMotion.TAIL), 0f)
        assertEquals(120f, SplitShell.height(80f, 120f, 1f), 0.0001f)
        assertEquals(100f, SplitShell.height(80f, 120f, SplitMotion.fromGeometry(0.5f)), 0.0001f)
    }
}
