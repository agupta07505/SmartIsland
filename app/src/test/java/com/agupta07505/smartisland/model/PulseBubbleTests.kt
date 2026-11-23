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

class BubbleIntoCutoutTest {
    @Test
    fun lengthIsTheSlideMerges() {
        assertEquals(MergeStyle.SLIDE.baseDurationMs, BubbleIntoCutout.BASE_MS)
    }

    @Test
    fun bubbleContentIsGoneBeforeTheHandoff() {
        assertEquals(1f, BubbleIntoCutout.contentAlpha(0f), 0.0001f)
        assertEquals(0f, BubbleIntoCutout.contentAlpha(BubbleIntoCutout.HANDOFF_START), 0.0001f)
        assertEquals(0f, BubbleIntoCutout.contentAlpha(1f), 0.0001f)
    }

    @Test
    fun cutoutMorphBounds() {
        assertEquals(0f, BubbleIntoCutout.shape(0f), 0.0001f)
        assertEquals(0f, BubbleIntoCutout.shape(BubbleIntoCutout.SHAPE_START), 0.0001f)
        assertEquals(1f, BubbleIntoCutout.shape(1f), 0.0001f)
        assertTrue(BubbleIntoCutout.shape(0.7f) in 0.01f..0.99f)
        assertEquals(0f, BubbleIntoCutout.shape(-1f), 0.0001f)
        assertEquals(1f, BubbleIntoCutout.shape(2f), 0.0001f)

        var last = 0f
        for (i in 0..100) {
            val s = BubbleIntoCutout.shape(i / 100f)
            assertTrue(s >= last)
            last = s
        }
    }

    @Test
    fun glideCurveIsEaseOut() {
        // control points stay inside 0..1 on x and end flat on y (no overshoot, a gentle landing), and the curve is
        // quick off the mark: the first control point sits on the start, the second near the end
        assertEquals(0f, BubbleIntoCutout.GLIDE_X1, 0f)
        assertTrue(BubbleIntoCutout.GLIDE_X2 <= 0.35f)
        assertTrue(BubbleIntoCutout.GLIDE_X1 in 0f..1f && BubbleIntoCutout.GLIDE_X2 in 0f..1f)
        assertEquals(1f, BubbleIntoCutout.GLIDE_Y2, 0f)
        assertEquals(0f, BubbleIntoCutout.GLIDE_Y1, 0f)
    }

    @Test
    fun glideStretch() {
        assertEquals(1f, BubbleIntoCutout.stretch(0f), 0.0001f)
        assertEquals(1f + BubbleIntoCutout.MAX_STRETCH, BubbleIntoCutout.stretch(1f), 0.0001f)
        assertTrue(BubbleIntoCutout.MAX_STRETCH in 0.01f..0.2f)
        // out of range is clamped
        assertEquals(1f, BubbleIntoCutout.stretch(-1f), 0.0001f)
        assertEquals(BubbleIntoCutout.stretch(1f), BubbleIntoCutout.stretch(3f), 0.0001f)
        var last = 1f
        for (i in 0..100) {
            val settled = BubbleIntoCutout.stretch(i / 100f)
            assertTrue(settled >= last)
            last = settled
        }

        for (remaining in listOf(0f, 0.3f, 1f)) {
            val constantArea = BubbleIntoCutout.stretch(remaining)
            assertEquals(1f, constantArea * BubbleIntoCutout.squash(constantArea), 0.0001f)
        }
    }

    @Test
    fun bubbleFlightEligibility() {
        assertTrue(BubbleIntoCutout.flies(expanded = true, bubbleWasOut = true, cutoutTakesEvent = true))
        assertTrue(!BubbleIntoCutout.flies(expanded = false, bubbleWasOut = true, cutoutTakesEvent = true))
        assertTrue(!BubbleIntoCutout.flies(expanded = true, bubbleWasOut = false, cutoutTakesEvent = true))
        assertTrue(!BubbleIntoCutout.flies(expanded = true, bubbleWasOut = true, cutoutTakesEvent = false))
    }

    @Test
    fun pickerAppearsAsBubbleLeaves() {
        assertEquals(0f, BubbleIntoCutout.pickerArrival(0f), 0.0001f)
        assertEquals(0f, BubbleIntoCutout.pickerArrival(BubbleIntoCutout.PICKER_ARRIVE_START), 0.0001f)
        assertEquals(1f, BubbleIntoCutout.pickerArrival(BubbleIntoCutout.PICKER_ARRIVE_END), 0.0001f)
        assertEquals(1f, BubbleIntoCutout.pickerArrival(1f), 0.0001f)
        assertEquals(0f, BubbleIntoCutout.pickerArrival(-1f), 0.0001f)
        assertTrue(BubbleIntoCutout.PICKER_ARRIVE_START < BubbleIntoCutout.PICKER_ARRIVE_END)
    }

    @Test
    fun thePickerGrowsFromASmallCircleAndNeverShrinks() {
        assertEquals(0.3f, BubbleIntoCutout.pickerScale(0f), 0.0001f)
        assertEquals(1f, BubbleIntoCutout.pickerScale(1f), 0.0001f)
        var last = 0f
        for (i in 0..100) {
            val s = BubbleIntoCutout.pickerScale(i / 100f)
            assertTrue(s >= last)
            last = s
        }
    }
}

class BubbleOriginTest {
    @Test
    fun onlyTapOnCollapsedBubbleStarts() {
        assertTrue(BubbleOrigin.startsFromBubble(expanded = false, onBubble = true, selectsBubbleEvent = true))
        assertFalse(BubbleOrigin.startsFromBubble(expanded = true, onBubble = true, selectsBubbleEvent = true))
        assertFalse(BubbleOrigin.startsFromBubble(expanded = false, onBubble = false, selectsBubbleEvent = true))
        assertFalse(BubbleOrigin.startsFromBubble(expanded = false, onBubble = true, selectsBubbleEvent = false))
    }

    @Test
    fun bubbleEdgeInterpolation() {
        assertEquals(10f, BubbleOrigin.edge(bubble = 10f, expanded = 100f, normal = 40f, progress = 0f), 0.0001f)
        assertEquals(100f, BubbleOrigin.edge(bubble = 10f, expanded = 100f, normal = 40f, progress = 1f), 0.0001f)
        assertEquals(55f, BubbleOrigin.edge(bubble = 10f, expanded = 100f, normal = 40f, progress = 0.5f), 0.0001f)

        for (w in listOf(0f, 0.3f, 1f)) {
            assertEquals(100f, BubbleOrigin.edge(10f, 100f, 100f, 1f, w), 0.0001f)
        }

        assertEquals(40f, BubbleOrigin.edge(10f, 100f, 40f, 0f, weight = 0f), 0.0001f)
        assertEquals(70f, BubbleOrigin.edge(10f, 100f, 70f, 0.5f, weight = 0f), 0.0001f)

        assertEquals(25f, BubbleOrigin.edge(10f, 100f, 40f, 0f, weight = 0.5f), 0.0001f)

        assertEquals(10f, BubbleOrigin.edge(10f, 100f, 40f, -0.5f), 0.0001f)
    }

    @Test
    fun radiusStartsAtTheBubblesCornerAndEndsOnTheCards() {
        assertEquals(15f, BubbleOrigin.radius(15f, 32f, 14f, 0f), 0.0001f)
        assertEquals(32f, BubbleOrigin.radius(15f, 32f, 32f, 1f), 0.0001f)
    }

    @Test
    fun opensNeedsFlagAndExpanded() {
        assertTrue(BubbleOrigin.opens(true, true))
        assertFalse(BubbleOrigin.opens(true, false))
        assertFalse(BubbleOrigin.opens(false, true))
    }

    @Test
    fun shellBlendsOnlyAtBubbleSpot() {
        assertTrue(BubbleOrigin.blends(true, true, edgeLayout = false, fingerLed = false))
        assertFalse(BubbleOrigin.blends(true, true, edgeLayout = true, fingerLed = false))
        assertFalse(BubbleOrigin.blends(true, true, edgeLayout = false, fingerLed = true))
        assertFalse(BubbleOrigin.blends(true, false, edgeLayout = false, fingerLed = false))
    }

    @Test
    fun bubbleContentHandsOverToTheCard() {
        assertEquals(1f, BubbleOrigin.contentAlpha(0f), 0.0001f)
        assertEquals(0f, BubbleOrigin.contentAlpha(BubbleOrigin.CONTENT_FADE_END), 0.0001f)
        assertEquals(0f, BubbleOrigin.contentAlpha(1f), 0.0001f)
    }

    @Test
    fun theBlendIsOnOnlyUntilTheCardIsOpen() {
        assertTrue(BubbleOrigin.active(true, 0f))
        assertTrue(BubbleOrigin.active(true, 0.9f))
        assertFalse(BubbleOrigin.active(true, 1f))
        assertFalse(BubbleOrigin.active(false, 0.5f))
    }

    @Test
    fun flagSurvivesTapAndOpening() {
        assertTrue(BubbleOrigin.keeps(true, expanded = false, progress = 0f, orientationFlipped = false, ageMs = 0L))
        assertTrue(BubbleOrigin.keeps(true, expanded = true, progress = 0.4f, orientationFlipped = false, ageMs = 100L))
    }

    @Test
    fun bubbleOriginRetention() {
        assertFalse(BubbleOrigin.keeps(true, expanded = true, progress = 1f, orientationFlipped = false, ageMs = 300L))

        assertFalse(BubbleOrigin.keeps(true, expanded = false, progress = 0.5f, orientationFlipped = false, ageMs = 100L))

        assertFalse(BubbleOrigin.keeps(true, expanded = true, progress = 0.5f, orientationFlipped = true, ageMs = 50L))

        assertFalse(
            BubbleOrigin.keeps(true, expanded = false, progress = 0f, orientationFlipped = false, ageMs = BubbleOrigin.STALE_MS + 1)
        )

        assertFalse(BubbleOrigin.keeps(false, expanded = true, progress = 0.5f, orientationFlipped = false, ageMs = 0L))
    }
}

class PulseBubbleTest {

    private fun card(key: String, mode: IslandMode, category: String? = null) = IslandNotification(
        key = key,
        packageName = "pkg.$key",
        appName = key,
        title = key,
        text = "",
        timeMillis = 0L,
        category = category,
        mode = mode
    )

    private val music = card("music", IslandMode.Music)
    private val download = card("download", IslandMode.DownloadUpload)
    private val plugged = card("system_battery", IslandMode.Battery, category = "battery_plugged_in")

    private fun idOf(c: IslandNotification) = CollapsedPulse.entryOf(c).id

    @Test
    fun pulseBasisSelection() {
        assertEquals(0, PulseBubble.basisIndex(listOf(music, download), null, 0))
        assertEquals(1, PulseBubble.basisIndex(listOf(music, download), null, 1))

        val belowTopCards = listOf(music, plugged)
        val belowTopBasis = PulseBubble.basisIndex(belowTopCards, idOf(plugged), 0)
        assertEquals(1, belowTopBasis)
        // the bubble then picks the best card besides the pulsing one
        assertEquals(0, secondaryCardIndex(belowTopCards, belowTopBasis, expanded = false))

        val topCards = listOf(music, download, plugged)
        val topBasis = PulseBubble.basisIndex(topCards, idOf(music), 0)
        assertEquals(0, topBasis)
        assertEquals(1, secondaryCardIndex(topCards, topBasis, expanded = false))

        assertEquals(1, PulseBubble.basisIndex(listOf(music, download), "gone", 1))

        val unplugged = card("system_battery", IslandMode.Battery, category = "battery_plugged_out")
        val sharedKeyCards = listOf(music, unplugged)
        assertEquals(1, PulseBubble.basisIndex(sharedKeyCards, idOf(unplugged), 0))
    }

    @Test
    fun mergesOnlyWhenNothingPulses() {
        assertTrue(PulseBubble.mayMerge(expanded = false, hidden = false, bubblePresent = true, pulseBusy = false))
        assertFalse(PulseBubble.mayMerge(expanded = false, hidden = false, bubblePresent = true, pulseBusy = true))
        assertFalse(PulseBubble.mayMerge(expanded = true, hidden = false, bubblePresent = true, pulseBusy = false))
        assertFalse(PulseBubble.mayMerge(expanded = false, hidden = true, bubblePresent = true, pulseBusy = false))
        assertFalse(PulseBubble.mayMerge(expanded = false, hidden = false, bubblePresent = false, pulseBusy = false))
    }

    @Test
    fun theBubbleSwellsWhenAPulseStartsOrItsCardChanges() {
        assertTrue(PulseBubble.swells("plug", null, "music", "music"))
        assertTrue(PulseBubble.swells("plug", "plug", "download", "music"))
        assertFalse(PulseBubble.swells("plug", "plug", "music", "music"))
        assertFalse(PulseBubble.swells(null, "plug", "music", "music"))
        assertFalse(PulseBubble.swells("plug", null, null, null))
        // an opposite event that took over the running pulse does not swell it again
        assertFalse(PulseBubble.swells("unplug", "plug", "music", "music", morphedFromId = "plug"))
        assertTrue(PulseBubble.swells("unplug", "plug", "download", "music", morphedFromId = "plug"))
        assertTrue(PulseBubble.swells("other", "plug", "music", "music", morphedFromId = "x"))
    }

    @Test
    fun theSwellScalesFromRestToThePeak() {
        assertEquals(1f, PulseBubble.scale(0f), 0.0001f)
        assertEquals(PulseBubble.SWELL_SCALE, PulseBubble.scale(1f), 0.0001f)
        assertEquals(1.11f, PulseBubble.scale(0.5f), 0.0001f)
    }

    @Test
    fun theElementNearestToTheBubbleDependsOnTheCameraSide() {
        // centre, left camera and notch: glyph at the left end, value at the right end
        assertTrue(PulseBubble.nearestIsGlyph(bubbleOnLeft = true, cameraSide = CameraSide.Centre))
        assertFalse(PulseBubble.nearestIsGlyph(bubbleOnLeft = false, cameraSide = CameraSide.Centre))
        assertTrue(PulseBubble.nearestIsGlyph(bubbleOnLeft = true, cameraSide = CameraSide.Left))
        assertFalse(PulseBubble.nearestIsGlyph(bubbleOnLeft = false, cameraSide = CameraSide.Left))
        // right camera mirrors the row
        assertFalse(PulseBubble.nearestIsGlyph(bubbleOnLeft = true, cameraSide = CameraSide.Right))
        assertTrue(PulseBubble.nearestIsGlyph(bubbleOnLeft = false, cameraSide = CameraSide.Right))
    }

    @Test
    fun edgeStripBubbleNeighbour() {
        // glyph above the value: a bubble below has the value nearest, a bubble above the glyph
        assertFalse(PulseBubble.nearestIsGlyphOnStrip(8f, 90f, 20f, bubbleBelow = true))
        assertTrue(PulseBubble.nearestIsGlyphOnStrip(8f, 90f, 20f, bubbleBelow = false))
        // a camera at the lower end turns the pill over: glyph below the value
        assertTrue(PulseBubble.nearestIsGlyphOnStrip(90f, 8f, 20f, bubbleBelow = true))
        assertFalse(PulseBubble.nearestIsGlyphOnStrip(90f, 8f, 20f, bubbleBelow = false))
        // no value, the glyph is the only one
        assertTrue(PulseBubble.nearestIsGlyphOnStrip(8f, 90f, 0f, bubbleBelow = true))
    }

    @Test
    fun theSpanIsTheBubbleAndItsGap() {
        assertEquals(42f, PulseBubble.spanDp(34f, 8f), 0.0001f)
    }

    @Test
    fun bubbleShift() {
        assertEquals(30f, PulseBubble.shiftDp(60f, CameraSide.Centre, bubbleOnLeft = false), 0.0001f)
        assertEquals(-30f, PulseBubble.shiftDp(60f, CameraSide.Centre, bubbleOnLeft = true), 0.0001f)

        // 60 dp wider, centre 24 dp left: the left edge is 54 dp out, the right edge 6 dp out
        assertEquals(-54f, PulseBubble.shiftDp(60f, CameraSide.Centre, bubbleOnLeft = true, centreShiftDp = -24f), 0.0001f)
        assertEquals(6f, PulseBubble.shiftDp(60f, CameraSide.Centre, bubbleOnLeft = false, centreShiftDp = -24f), 0.0001f)

        assertEquals(60f, PulseBubble.shiftDp(60f, CameraSide.Left, bubbleOnLeft = false, centreShiftDp = 12f), 0.0001f)

        // the circle is always on the side away from the camera, which is the side the pill grows to
        assertEquals(60f, PulseBubble.shiftDp(60f, CameraSide.Left, bubbleOnLeft = false), 0.0001f)
        assertEquals(-60f, PulseBubble.shiftDp(60f, CameraSide.Right, bubbleOnLeft = true), 0.0001f)

        assertEquals(0f, PulseBubble.shiftDp(0f, CameraSide.Left, false), 0.0001f)
        assertEquals(0f, PulseBubble.shiftDp(-3f, CameraSide.Centre, true), 0.0001f)
    }

    @Test
    fun theBubbleStaysUnmergedThroughAWholeQueueOfPulses() {
        var state = CollapsedPulse.arrive(CollapsedPulse.State(), CollapsedPulse.entryOf(plugged), 0L)
        state = CollapsedPulse.arrive(state, CollapsedPulse.entryOf(download), 100L)
        assertEquals(1, state.queue.size)
        var merged = false
        var steps = 0
        while (true) {
            val due = CollapsedPulse.nextDeadlineMs(state) ?: break
            // every moment up to the next transition: the bubble may not merge
            merged = merged || PulseBubble.mayMerge(false, false, true, state.busy)
            state = CollapsedPulse.advance(state, due)
            steps++
            assertTrue(steps < 10)
        }
        assertFalse(merged)
        // pulses and gap are over: the idle timer may start
        assertTrue(PulseBubble.mayMerge(false, false, true, state.busy))
    }

    @Test
    fun fixedEdgeBubble() {
        assertEquals(0f, PulseBubble.shiftDp(60f, CameraSide.Left, bubbleOnLeft = true), 0.0001f)
        assertEquals(0f, PulseBubble.shiftDp(60f, CameraSide.Right, bubbleOnLeft = false), 0.0001f)
        assertTrue(PulseBubble.onFixedSide(CameraSide.Left, true))
        assertFalse(PulseBubble.onFixedSide(CameraSide.Left, false))
        assertFalse(PulseBubble.onFixedSide(CameraSide.Right, true))
        assertFalse(PulseBubble.onFixedSide(CameraSide.Centre, true))
        assertFalse(PulseBubble.onFixedSide(CameraSide.Centre, false))
    }
}
