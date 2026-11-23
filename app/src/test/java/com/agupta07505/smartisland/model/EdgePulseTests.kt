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

/** Numbers follow the demo's `build` in split mode: screen 360 tall, hole 18, idle 112, reserve 72 */
class EdgePulseLengthTest {
    private fun fit(title: Float, value: Float, centre: Float = 180f) =
        EdgePulseLength.fit(112f, 18f, title, value, centre, 360f)

    @Test
    fun shortContentKeepsHalfTheSettledLengthOnEachSide() {
        val f = fit(title = 0f, value = 20f)
        assertEquals(56f, f.upperDp, 0.001f)
        assertEquals(56f, f.lowerDp, 0.001f)
        assertEquals(112f, f.lengthDp, 0.001f)
    }

    @Test
    fun theSidesAreSizedOnTheirOwn() {
        // upper: 9 + 8 + 22 + 6 + 40 + 10 = 95, lower: 9 + 12 + 10 + 4 = 35 -> held at 56
        val f = fit(title = 40f, value = 10f)
        assertEquals(95f, f.upperDp, 0.001f)
        assertEquals(56f, f.lowerDp, 0.001f)
        // the demo draws the longer value on the other side the same way
        val g = fit(title = 0f, value = 80f)
        assertEquals(56f, g.upperDp, 0.001f)
        assertEquals(9f + 12f + 80f + 4f, g.lowerDp, 0.001f)
    }

    @Test
    fun noTitleShorterGap() {
        // 9 + 8 + 22 + 6 = 45, under the half idle length of 56
        assertEquals(56f, fit(0f, 0f).upperDp, 0.001f)
        // a title of 60: 9 + 8 + 22 + 6 + 60 + 10 = 115 is more than the room 108, so it stops at the cap
        assertEquals(108f, fit(60f, 0f).upperDp, 0.001f)
    }

    @Test
    fun eachEndStopsAtReserve() {
        val f = fit(title = 300f, value = 300f)
        assertEquals(108f, f.upperDp, 0.001f)
        assertEquals(108f, f.lowerDp, 0.001f)
    }

    @Test
    fun offMiddleHoleOwnCaps() {
        val f = fit(title = 300f, value = 300f, centre = 150f)
        assertEquals(150f - 72f, f.upperDp, 0.001f)
        assertEquals(360f - 72f - 150f, f.lowerDp, 0.001f)
    }

    @Test
    fun theTitleGetsWhatTheClampLeavesIt() {
        // upper 108: 108 - 9 - 8 - 22 - 12 = 57, so a 60 dp title is cut by 3
        val f = fit(title = 60f, value = 0f)
        assertEquals(57f, f.titleMaxDp, 0.001f)
        assertTrue(f.titleMaxDp < 60f)
    }

    @Test
    fun anUnclampedTitleHasRoomForItself() {
        val f = fit(title = 40f, value = 0f)
        assertTrue(f.titleMaxDp >= 40f)
    }

    @Test
    fun theValueGetsWhatTheClampLeavesIt() {
        // lower 108: 108 - 9 - 12 - 6 = 81
        assertEquals(81f, fit(title = 0f, value = 200f).valueMaxDp, 0.001f)
    }

    @Test
    fun middleTextAllocation() {
        assertEquals(80f, fit(title = 0f, value = 80f).valueMaxDp, 0.001f)
        val valueF = fit(title = 0f, value = 80f)
        assertEquals(80f, EdgePulseLength.middleTexts(valueF.lengthDp, valueF.upperDp, 18f, 0f, 80f).valueLenDp, 0.001f)

        // hole 18 at 56 from the top of a 112 pill: title room 56 - 9 - 8 - 22 - 12 = 5, value room 56 - 9 - 12 - 6 = 29
        val ends = EdgePulseLength.middleTexts(112f, 56f, 18f, 40f, 50f)
        assertEquals(5f, ends.titleLenDp, 0.001f)
        assertEquals(29f, ends.valueLenDp, 0.001f)
        val grown = EdgePulseLength.middleTexts(200f, 95f, 18f, 40f, 50f)
        assertEquals(40f, grown.titleLenDp, 0.001f)
        assertEquals(50f, grown.valueLenDp, 0.001f)

        val artist = fit(title = 40f, value = PulseWidth.valueSideDp(14f, 40f))
        assertEquals(95f, artist.upperDp, 0.001f)
        // 9 from the hole, 4 of gap, 40, 6, 14 and the end padding: 85
        assertEquals(85f, artist.lowerDp, 0.001f)
        val artistT = EdgePulseLength.middleTexts(artist.lengthDp, artist.upperDp, 18f, 40f, 14f, 40f)
        assertEquals(40f, artistT.artistLenDp, 0.001f)
        assertEquals(14f, artistT.valueLenDp, 0.001f)

        val clipped = EdgePulseLength.middleTexts(180f, 95f, 18f, 40f, 14f, 300f)
        assertEquals(180f - 95f - 9f - 4f - 12f - 14f - 6f, clipped.artistLenDp, 0.001f)
        assertEquals(0f, EdgePulseLength.middleTexts(180f, 95f, 18f, 40f, 14f).artistLenDp, 0f)
    }

    @Test
    fun pulseLengthBounds() {
        val unknown = EdgePulseLength.fit(112f, 0f, 40f, 10f, 180f, 360f)
        assertEquals(8f + 22f + 6f + 40f + 10f, unknown.upperDp, 0.001f)

        val empty = EdgePulseLength.fit(112f, 18f, 40f, 40f, centreYDp = 60f, screenHeightDp = 360f)
        assertEquals(0f, empty.upperDp, 0.001f)
        assertEquals(0f, empty.titleMaxDp, 0.001f)

        val withBubbleBelow = EdgePulseLength.fit(112f, 18f, 300f, 300f, 180f, 360f, bubbleSpanDp = 42f, bubbleBelow = true)
        assertEquals(108f, withBubbleBelow.upperDp, 0.001f)
        assertEquals(66f, withBubbleBelow.lowerDp, 0.001f)
        val withBubbleAbove = EdgePulseLength.fit(112f, 18f, 300f, 300f, 180f, 360f, bubbleSpanDp = 42f, bubbleBelow = false)
        assertEquals(66f, withBubbleAbove.upperDp, 0.001f)
        assertEquals(108f, withBubbleAbove.lowerDp, 0.001f)

        // pill 130 long with the hole at 150: it reaches 50 above and 80 below, short content must not pull the lower end in
        val settledF = EdgePulseLength.fit(130f, 18f, 0f, 0f, 150f, 360f, settledUpperDp = 50f, settledLowerDp = 80f)
        assertEquals(50f, settledF.upperDp, 0.001f)
        assertEquals(80f, settledF.lowerDp, 0.001f)

        // the settled reach above is 0 (the hole is past the pill's top), the glyph still needs room above the hole
        val offset = EdgePulseLength.fit(112f, 18f, 0f, 0f, 150f, 360f, settledUpperDp = -4f, settledLowerDp = 116f)
        assertEquals(9f + 8f + 22f + 6f, offset.upperDp, 0.001f)
        assertEquals(116f, offset.lowerDp, 0.001f)
    }

    @Test
    fun edgeSlotLayout() {
        val centred = EdgePulseLength.slots(EdgeCameraSpot.Middle, 150f, 0f, 40f, 30f)
        assertEquals(8f, centred.glyphTopDp, 0.001f)
        assertEquals(8f + 22f + 6f, centred.titleTopDp, 0.001f)
        assertEquals(40f, centred.titleLenDp, 0.001f)
        assertEquals(150f - 12f - 30f, centred.valueTopDp, 0.001f)

        assertEquals(138f - 42f, EdgePulseLength.slots(EdgeCameraSpot.Middle, 138f, 0f, 0f, 30f).valueTopDp, 0.001f)
        assertEquals(168f - 42f, EdgePulseLength.slots(EdgeCameraSpot.Middle, 168f, 0f, 0f, 30f).valueTopDp, 0.001f)

        val upper = EdgePulseLength.slots(EdgeCameraSpot.Upper, 200f, 30f, 40f, 30f)
        assertEquals(30f, upper.glyphTopDp, 0.001f)
        assertEquals(30f + 22f + 6f, upper.titleTopDp, 0.001f)
        assertEquals(200f - 12f - 30f, upper.valueTopDp, 0.001f)

        val lower = EdgePulseLength.slots(EdgeCameraSpot.Lower, 200f, 30f, 40f, 30f)
        assertEquals(200f - 30f - 22f, lower.glyphTopDp, 0.001f)
        assertEquals(200f - 58f - 40f, lower.titleTopDp, 0.001f)
        assertEquals(12f, lower.valueTopDp, 0.001f)
        // the title ends where the glyph starts, less the gap
        assertEquals(lower.glyphTopDp - 6f, lower.titleTopDp + lower.titleLenDp, 0.001f)

        val f = fit(title = 300f, value = 300f)
        assertTrue(f.titleMaxDp < 300f)
        assertTrue(f.valueMaxDp < 300f)
        val clipped = EdgePulseLength.slots(EdgeCameraSpot.Middle, f.lengthDp, 0f, f.titleMaxDp, f.valueMaxDp)
        assertTrue(clipped.titleTopDp + clipped.titleLenDp <= f.upperDp - 9f)
    }

    @Test
    fun sideCameraTextBudget() {
        assertEquals(360f - 72f - 20f - 42f, EdgePulseLength.sideMaxDp(EdgeCameraSpot.Upper, 20f, 112f, 360f, 42f),
            0.001f)
        assertEquals(240f + 112f - 72f - 42f, EdgePulseLength.sideMaxDp(EdgeCameraSpot.Lower, 240f, 112f, 360f, 42f),
            0.001f)
    }

    @Test
    fun sidePulseSpan() {
        val up = EdgePulseLength.sideSpan(EdgeCameraSpot.Upper, 20f, 112f, 200f, 250f, 0.5f)
        assertEquals(20f, up.topDp, 0.001f)
        assertEquals(156f, up.lengthDp, 0.001f)
        val low = EdgePulseLength.sideSpan(EdgeCameraSpot.Lower, 240f, 112f, 200f, 250f, 0.5f)
        assertEquals(240f + 112f, low.topDp + low.lengthDp, 0.001f)
        assertEquals(156f, low.lengthDp, 0.001f)

        val s = EdgePulseLength.sideSpan(EdgeCameraSpot.Upper, 20f, 112f, 200f, 205f, 1.09f)
        assertEquals(205f, s.lengthDp, 0.001f)
    }

    @Test
    fun pillTopFollowsTheUpperSide() {
        val f = fit(title = 40f, value = 10f)
        assertEquals(180f - 95f, EdgePulseLength.topDp(180f, f), 0.001f)
    }

    @Test
    fun progressLineInsets() {
        assertEquals(22.4f, EdgePulseLength.lineInsetDp(112f, 24f, 0f, 3f), 0.001f)
        assertEquals(14f, EdgePulseLength.lineInsetDp(50f, 24f, 0f, 3f), 0.001f)
        assertEquals(40f, EdgePulseLength.lineInsetDp(200f, 24f, 0f, 3f), 0.001f)

        // radius 12, line 3 from the side: the curve has left 12 - sqrt(144 - 81) = 4.06, plus the 4 margin
        val inset = EdgePulseLength.lineInsetDp(40f, 24f, 12f, 3f)
        assertEquals(14f, inset, 0.001f)
        // a thick pill with a large radius pushes it further
        assertTrue(EdgePulseLength.lineInsetDp(40f, 60f, 30f, 3f) > 14f)
    }

    @Test
    fun spanRunsFromTheSettledPillToTheFit() {
        val f = fit(title = 40f, value = 80f)
        val settledTop = 180f - 56f
        val start = EdgePulseLength.span(settledTop, 112f, f, 180f, 0f, 360f)
        assertEquals(settledTop, start.topDp, 0.001f)
        assertEquals(112f, start.lengthDp, 0.001f)
        val end = EdgePulseLength.span(settledTop, 112f, f, 180f, 1f, 360f)
        assertEquals(180f - f.upperDp, end.topDp, 0.001f)
        assertEquals(f.lengthDp, end.lengthDp, 0.001f)
    }

    @Test
    fun overshootStopsAtReserve() {
        val f = fit(title = 300f, value = 300f)
        val s = EdgePulseLength.span(124f, 112f, f, 180f, 1.09f, 360f)
        assertEquals(72f, s.topDp, 0.001f)
        assertEquals(360f - 72f, s.topDp + s.lengthDp, 0.001f)
    }

    @Test
    fun sideValueKeepsToTheRoomAfterGlyphAndTitle() {
        // 200 - 30 - 22 - (6 + 60) - 14 - 12 = 56
        assertEquals(56f, EdgePulseLength.sideValueMaxDp(200f, 30f, 60f), 0.001f)
        assertEquals(122f, EdgePulseLength.sideValueMaxDp(200f, 30f, 0f), 0.001f)
        assertEquals(0f, EdgePulseLength.sideValueMaxDp(60f, 30f, 0f), 0.001f)
    }

    @Test
    fun sideValueSpace() {
        // room after glyph: 100 - 30 - 22 - 14 - 12 = 22
        val valueT = EdgePulseLength.sideTexts(100f, 30f, 40f, 10f)
        assertEquals(10f, valueT.valueLenDp, 0.001f)
        assertEquals(6f, valueT.titleLenDp, 0.001f)
        val fullValue = EdgePulseLength.sideTexts(100f, 30f, 40f, 60f)
        assertEquals(22f, fullValue.valueLenDp, 0.001f)
        assertEquals(0f, fullValue.titleLenDp, 0.001f)

        // 200 - 30 - 22 - 14 - 12 = 122 for the value side
        val artist = EdgePulseLength.sideTexts(200f, 30f, 80f, 10f, 50f)
        assertEquals(50f, artist.artistLenDp, 0.001f)
        assertEquals(122f - 10f - 6f - 50f - 6f, artist.titleLenDp, 0.001f)
        val long = EdgePulseLength.sideTexts(200f, 30f, 80f, 10f, 300f)
        assertEquals(122f - 10f - 6f, long.artistLenDp, 0.001f)
        assertEquals(0f, long.titleLenDp, 0f)
        assertEquals(EdgePulseLength.Texts(50f, 10f), EdgePulseLength.sideTexts(200f, 30f, 50f, 10f))
    }

    @Test
    fun artistBeforeValueOnTitleSide() {
        val upper = EdgePulseLength.slots(EdgeCameraSpot.Middle, 150f, 0f, 40f, 30f, artistLenDp = 20f)
        assertEquals(150f - 12f - 30f - 6f - 20f, upper.artistTopDp, 0.001f)
        // turned over, the value is at the top and the artist follows it downward
        val lower = EdgePulseLength.slots(EdgeCameraSpot.Lower, 200f, 30f, 40f, 30f, artistLenDp = 20f)
        assertEquals(12f + 30f + 6f, lower.artistTopDp, 0.001f)
    }
}

class EdgePulseOwnershipTest {
    @Test
    fun pulseSurfaceOwnership() {
        assertTrue(EdgePulseOwnership.ownsSurface(growing = true, cardOpen = false))
        assertFalse(EdgePulseOwnership.ownsSurface(growing = false, cardOpen = false))
        assertFalse(EdgePulseOwnership.ownsSurface(growing = true, cardOpen = true))
        assertFalse(EdgePulseOwnership.ownsSurface(growing = false, cardOpen = true))
    }
}

/**
 * Split pulse: the edge pill with the camera in the middle. A notification has its glyph and message above the hole
 * and its title alone below it. Same screen as [EdgePulseLengthTest]: 360 tall, hole 18, idle 112, reserve 72. The hole's
 * centre decides everything, so the notch look (flat camera-side edge) is the same numbers
 */
class EdgePulseSplitTest {
    private fun fit(
        message: Float,
        title: Float,
        centre: Float = 180f,
        bubbleSpan: Float = 0f,
        bubbleBelow: Boolean = true,
        settledUpper: Float = 56f,
        settledLower: Float = 56f
    ) = EdgePulseLength.fitSplit(
        112f, 18f, message, title, centre, 360f, bubbleSpan, bubbleBelow, settledUpper, settledLower
    )

    @Test
    fun noMessageNoTitleKeepsLength() {
        // upper: 9 + 8 + 22 + 6 = 45 and lower: 9, both under the half idle length of 56
        val f = fit(0f, 0f)
        assertEquals(56f, f.upperDp, 0.001f)
        assertEquals(56f, f.lowerDp, 0.001f)
        assertEquals(112f, f.lengthDp, 0.001f)
    }

    @Test
    fun theMessageSizesTheUpperSideAndTheTitleTheLowerOne() {
        // upper: 9 + 8 + 22 + 6 + 50 + 6 = 101, lower stays at 56
        val m = fit(message = 50f, title = 0f)
        assertEquals(101f, m.upperDp, 0.001f)
        assertEquals(56f, m.lowerDp, 0.001f)
        assertEquals(50f, m.messageMaxDp, 0.001f)
        // lower: 9 + (6 + 12) + 40 = 67, upper stays at 56
        val t = fit(message = 0f, title = 40f)
        assertEquals(56f, t.upperDp, 0.001f)
        assertEquals(67f, t.lowerDp, 0.001f)
        assertEquals(40f, t.titleMaxDp, 0.001f)
    }

    @Test
    fun theNeedierSideGrowsAndTheOtherKeepsItsHalf() {
        val f = fit(message = 40f, title = 10f)
        assertEquals(9f + 8f + 22f + 6f + 40f + 6f, f.upperDp, 0.001f)
        // lower: 9 + 18 + 10 = 37, under the half of 56
        assertEquals(56f, f.lowerDp, 0.001f)
    }

    @Test
    fun eachEndStopsAtReserve() {
        val f = fit(message = 300f, title = 300f)
        assertEquals(108f, f.upperDp, 0.001f)
        assertEquals(108f, f.lowerDp, 0.001f)
        assertEquals(108f - 9f - 8f - 22f - 6f - 6f, f.messageMaxDp, 0.001f)
        assertEquals(108f - 9f - 18f, f.titleMaxDp, 0.001f)
    }

    @Test
    fun offMiddleHoleOwnCaps() {
        // the notch look keeps the pill off the hole: the hole's own centre is what counts
        val f = fit(message = 300f, title = 300f, centre = 150f)
        assertEquals(150f - 72f, f.upperDp, 0.001f)
        assertEquals(360f - 72f - 150f, f.lowerDp, 0.001f)
    }

    @Test
    fun theBubbleKeepsItsSpanFreeOnItsSide() {
        val below = fit(message = 300f, title = 300f, bubbleSpan = 30f, bubbleBelow = true)
        assertEquals(108f, below.upperDp, 0.001f)
        assertEquals(78f, below.lowerDp, 0.001f)
        val above = fit(message = 300f, title = 300f, bubbleSpan = 30f, bubbleBelow = false)
        assertEquals(78f, above.upperDp, 0.001f)
        assertEquals(108f, above.lowerDp, 0.001f)
    }

    @Test
    fun aSideNeverEndsShorterThanTheSettledPillReachedOnIt() {
        val f = fit(message = 0f, title = 0f, settledUpper = 70f, settledLower = 40f)
        assertEquals(70f, f.upperDp, 0.001f)
        assertEquals(40f, f.lowerDp, 0.001f)
    }

    @Test
    fun splitWithoutSpace() {
        val unknownHole = EdgePulseLength.fitSplit(112f, 0f, 50f, 0f, 180f, 360f)
        assertEquals(8f + 22f + 6f + 50f + 6f, unknownHole.upperDp, 0.001f)
        assertEquals(50f, unknownHole.messageMaxDp, 0.001f)

        val noRoom = EdgePulseLength.fitSplit(112f, 18f, 50f, 50f, 60f, 360f)
        assertEquals(0f, noRoom.upperDp, 0.001f)
        assertEquals(0f, noRoom.messageMaxDp, 0.001f)
    }

    @Test
    fun slotsFollowThePillAsItIsDrawn() {
        val s = EdgePulseLength.splitSlots(200f, 100f, 18f, messageDp = 40f, titleDp = 30f)
        // the glyph after the start padding, a gap, then the message
        assertEquals(8f, s.glyphTopDp, 0.001f)
        assertEquals(8f + 22f + 6f, s.messageTopDp, 0.001f)
        assertEquals(40f, s.messageLenDp, 0.001f)
        // the title alone, a gap below the hole's edge
        assertEquals(100f + 9f + 6f, s.titleTopDp, 0.001f)
        assertEquals(30f, s.titleLenDp, 0.001f)
    }

    @Test
    fun splitSlotClipping() {
        val clipped = EdgePulseLength.splitSlots(200f, 100f, 18f, messageDp = 100f, titleDp = 100f)
        // message: 100 - 9 - 8 - 22 - 6 - 6, title: 200 - 12 - 115
        assertEquals(49f, clipped.messageLenDp, 0.001f)
        assertEquals(73f, clipped.titleLenDp, 0.001f)

        val growing = EdgePulseLength.splitSlots(120f, 60f, 18f, messageDp = 0f, titleDp = 100f)
        assertEquals(120f - 12f - (60f + 9f + 6f), growing.titleLenDp, 0.001f)
        val none = EdgePulseLength.splitSlots(87f, 60f, 18f, messageDp = 0f, titleDp = 100f)
        assertEquals(0f, none.titleLenDp, 0.001f)

        val beforeHole = EdgePulseLength.splitSlots(112f, 10f, 18f, messageDp = 50f, titleDp = 0f)
        assertEquals(0f, beforeHole.messageLenDp, 0.001f)

        val ordered = EdgePulseLength.splitSlots(200f, 100f, 18f, messageDp = 40f, titleDp = 30f)
        assertTrue(ordered.glyphTopDp < ordered.messageTopDp)
        assertTrue(ordered.messageTopDp + ordered.messageLenDp < ordered.titleTopDp)

        val plain = EdgePulseLength.splitSlots(300f, 100f, 30f, 60f, 40f)
        val icon = EdgePulseLength.splitSlots(300f, 100f, 30f, 60f, 40f, mediaIconDp = PulseWidth.MEDIA_ICON_DP)
        assertEquals(plain.messageTopDp + 18f, icon.messageTopDp, 0.001f)
        assertEquals(plain.messageTopDp, icon.iconTopDp, 0.001f)
        assertEquals(plain.glyphTopDp, icon.glyphTopDp, 0.001f)
    }

    @Test
    fun theGlyphFadesInOnceTheRoomAboveTheHoleHoldsIt() {
        // room above the hole: hole from top - 9; the glyph needs 8 + 22 = 30
        assertEquals(1f, EdgePulseLength.splitGlyphAlpha(100f, 18f), 0.001f)
        assertEquals(0.5f, EdgePulseLength.splitGlyphAlpha(9f + 26f, 18f), 0.001f)
        assertEquals(0f, EdgePulseLength.splitGlyphAlpha(9f + 10f, 18f), 0.001f)
    }

    @Test
    fun theLowerSideFixedPartsAddUp() {
        assertEquals(0f, EdgePulseLength.splitLowerFixedDp(false), 0.001f)
        assertEquals(6f + 12f, EdgePulseLength.splitLowerFixedDp(true), 0.001f)
    }
}
