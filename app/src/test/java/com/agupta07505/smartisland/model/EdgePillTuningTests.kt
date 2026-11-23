/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val LEGACY = EdgePillTuning(heightDp = 72f, widthDp = 24f)

class EdgePillFitHoleTest {
    private val smallHole = CutoutArea(0f, 8f, 60f, 14f)

    @Test
    fun fallbackHoleFit() {
        val detectedFit = EdgePillFit.fitPortrait(EdgePillTuning(), smallHole)
        assertEquals(EdgePillTuning.WIDTH.start, detectedFit.widthDp, 0.01f)
        assertEquals(84f, detectedFit.heightDp, 0.01f)

        val assumedFit = EdgePillFit.fitPortrait(EdgePillTuning(), smallHole, assumed = true)
        assertEquals(EdgePillTuning.DEFAULT_WIDTH, assumedFit.widthDp, 0.01f)
        assertEquals(EdgePillTuning.DEFAULT_HEIGHT, assumedFit.heightDp, 0.01f)

        val flatFit = EdgePillFit.fitPortrait(EdgePillTuning(flatEdge = true), smallHole, assumed = true)
        assertEquals(60f, flatFit.widthDp, 0.01f)

        val big = CutoutArea(0f, 8f, 100f, 50f)
        val oversizedFit = EdgePillFit.fitPortrait(EdgePillTuning(), big, assumed = true)
        assertEquals(50f, oversizedFit.widthDp, 0.01f)
        assertTrue(oversizedFit.heightDp >= 124f)
    }

    @Test
    fun teardropPhoneStartsWithTheFlatWidth() {
        assertEquals(60f, EdgePillTuning.decode(null, flatEdgeDefault = true).widthDp, 0.01f)
        assertEquals(EdgePillTuning.DEFAULT_WIDTH, EdgePillTuning.decode(null, flatEdgeDefault = false).widthDp, 0.01f)
    }
}

class EdgePillTuningTest {
    private val hole = EdgePillLayoutMath.resolveCamera(-474f, 231f, true, 1000f, 462f, cameraWidthDp = 20f, tuning = LEGACY)

    @Test
    fun tuningCodec() {
        val roundTrip = EdgePillTuning(widthDp = 40f, heightDp = 90f, xOffsetDp = 4f, yOffsetDp = -10f, cornerRadiusDp = 15f,
            opacity = 0.8f, bubbleGapDp = 4f, cardGapDp = 12f)
        assertEquals(roundTrip, EdgePillTuning.decode(roundTrip.encode()))

        assertEquals(EdgePillTuning(), EdgePillTuning.decode(null))
        assertEquals(EdgePillTuning(), EdgePillTuning.decode("garbage;;=;ph=abc"))
        val wild = EdgePillTuning.decode("ph=9999;o=-4;pw=NaN;y=500")
        assertEquals(EdgePillTuning.HEIGHT.endInclusive, wild.heightDp, 0.001f)
        assertEquals(EdgePillTuning.OPACITY.start, wild.opacity, 0.001f)
        assertEquals(EdgePillTuning.DEFAULT_WIDTH, wild.widthDp, 0.001f)
        assertEquals(EdgePillTuning.Y_OFFSET.endInclusive, wild.yOffsetDp, 0.001f)

        assertEquals(EdgePillTuning.decode("ph=90"), EdgePillTuning.decode("ph=90;z=40"))

        val bubble = EdgePillTuning(bubblePos = EdgePillTuning.BUBBLE_ABOVE)
        assertEquals(bubble, EdgePillTuning.decode(bubble.encode()))
        assertEquals(EdgePillTuning.BUBBLE_AUTO, EdgePillTuning.decode("bp=9").bubblePos)

        val d = EdgePillTuning()
        assertTrue(!d.contentSameSize)
        assertEquals(EdgePillTuning.DEFAULT_CONTENT_PERCENT, d.contentPercent, 0.001f)
        // a saved value from before the setting existed gets the default (stretch)
        assertTrue(!EdgePillTuning.decode("pw=32;ph=150").contentSameSize)
        val content = EdgePillTuning(contentSameSize = true, contentPercent = 140f)
        assertEquals(content, EdgePillTuning.decode(content.encode()))
        assertEquals(EdgePillTuning.CONTENT_PERCENT.endInclusive, EdgePillTuning.decode("cp=9999").contentPercent, 0.001f)
        assertEquals(EdgePillTuning.CONTENT_PERCENT.start, EdgePillTuning.decode("cp=3").contentPercent, 0.001f)
        assertEquals(EdgePillTuning.DEFAULT_CONTENT_PERCENT, EdgePillTuning.decode("cp=NaN").contentPercent, 0.001f)
    }

    @Test fun defaultsKeepTheOldLayout() {
        val l = EdgePillLayoutMath.compute(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = true)
        assertEquals(0f, l.pill.left, 0.001f)
        assertEquals(64f, l.pill.top, 0.001f)
        assertEquals(72f, l.pill.height, 0.001f)
        assertEquals(24f, l.pill.width, 0.001f)
        assertEquals(64f + 72f + 8f, l.secondary!!.top, 0.001f)
    }

    @Test fun defaultsFollowPortraitSliders() {
        // the edge pill is the portrait pill turned
        val d = EdgePillLayoutMath.compute(EdgeCamera(true, 200f, 700f, 400f), hasSecondary = false)
        assertEquals(112f, d.pill.height, 0.001f) // portrait's default pill width
        assertEquals(34f, d.pill.width, 0.001f) // portrait's default pill height
        val s = SmartIslandSettings
        assertEquals(s.MIN_WIDTH, EdgePillTuning.HEIGHT.start, 0.001f)
        assertEquals(s.MAX_WIDTH, EdgePillTuning.HEIGHT.endInclusive, 0.001f)
        assertEquals(s.MIN_HEIGHT, EdgePillTuning.WIDTH.start, 0.001f)
        assertEquals(s.MAX_HEIGHT, EdgePillTuning.WIDTH.endInclusive, 0.001f)
    }

    @Test fun heightAndWidthChangeThePill() {
        val cam = hole.copy(tuning = EdgePillTuning(heightDp = 100f, widthDp = 40f))
        val l = EdgePillLayoutMath.compute(cam, hasSecondary = false)
        assertEquals(100f, l.pill.height, 0.001f)
        assertEquals(40f, l.pill.width, 0.001f)
        assertEquals(231f, l.pill.top + 50f, 0.001f)
    }

    @Test fun verticalOffsetKeepsPillOnScreen() {
        val down = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(yOffsetDp = 40f, heightDp = 72f)), hasSecondary = false)
        assertEquals(231f + 40f - 36f, down.pill.top, 0.001f)
        val far = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(yOffsetDp = 120f, heightDp = 160f)), hasSecondary = false)
        assertTrue(far.pill.bottom <= 462f + 0.001f)
        val up = EdgePillLayoutMath.compute(
            EdgeCamera(true, 50f, 1000f, 462f, tuning = EdgePillTuning(yOffsetDp = -120f, heightDp = 100f)), hasSecondary = false
        )
        assertEquals(0f, up.pill.top, 0.001f)
    }

    @Test fun xOffsetMovesThePillIntoTheScreenOnBothEdges() {
        val left = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(xOffsetDp = 10f)), hasSecondary = false)
        assertEquals(10f, left.pill.left, 0.001f)
        val rightCam = EdgePillLayoutMath.resolveCamera(474f, 231f, false, 1000f, 462f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(xOffsetDp = 10f))
        val right = EdgePillLayoutMath.compute(rightCam, hasSecondary = false)
        assertEquals(1000f - 10f, right.pill.right, 0.001f)
    }

    @Test fun cardGapFollowsThePillOffset() {
        val cam = hole.copy(tuning = EdgePillTuning(xOffsetDp = 10f, cardGapDp = 20f))
        val p = EdgePillLayoutMath.cardPlacement(cam, cardWidthDp = 400f)
        val cardLeft = p.cardCentreOffsetDp + 500f - 200f
        assertEquals(10f + cam.pillWidthDp + 20f, cardLeft, 0.001f)
    }

    @Test fun cardIsCentredOnThePillWhenItIsMoved() {
        val cam = hole.copy(tuning = EdgePillTuning(yOffsetDp = 30f))
        assertEquals(231f + 30f, EdgePillLayoutMath.cardPlacement(cam, 300f).pillCentreYDp, 0.001f)
    }

    @Test fun bubbleSizeFollowsThePillWidthAndGapApplies() {
        val cam = hole.copy(tuning = EdgePillTuning(widthDp = 40f, bubbleGapDp = 4f))
        val l = EdgePillLayoutMath.compute(cam, hasSecondary = true)
        val size = EdgePillTuning.bubbleSizeFor(l.pill.width)
        assertEquals(size, l.secondary!!.width, 0.001f)
        assertEquals(size, l.secondary!!.height, 0.001f)
        assertEquals(l.pill.top - 4f - size, l.secondary!!.top, 0.001f)
        val wider = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(widthDp = 56f)), hasSecondary = true)
        assertTrue(wider.secondary!!.width > size)
    }

    @Test fun bubbleMatchesThePillWidth() {
        for (w in listOf(24f, 32f, 44f, 60f)) {
            val l = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(widthDp = w)), hasSecondary = true)
            assertEquals(l.pill.width, l.secondary!!.width, 0.001f)
            assertEquals(l.pill.width, l.secondary!!.height, 0.001f)
        }
    }

    @Test fun holeStaysInsideThePillAtAnyOffset() {
        val cam = hole.copy(tuning = EdgePillTuning(xOffsetDp = 6f))
        val l = EdgePillLayoutMath.compute(cam, hasSecondary = false)
        assertEquals(26f - 6f, EdgePillLayoutMath.holeLocalX(l), 0.001f)
        assertEquals(231f - l.pill.top, EdgePillLayoutMath.holeLocalY(l), 0.001f)
    }

    @Test fun glyphsSitAtThePillEndsAndFollowItsSize() {
        val short = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(heightDp = 80f)), hasSecondary = false)
        val tall = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(heightDp = 170f)), hasSecondary = false)
        val a = EdgePillLayoutMath.glyphSlots(short)
        val b = EdgePillLayoutMath.glyphSlots(tall)
        assertEquals(a.glyphTop, b.glyphTop, 0.001f)
        assertEquals(80f - a.valueTop, 170f - b.valueTop, 0.001f)
        assertTrue(b.valueTop - b.glyphTop > a.valueTop - a.glyphTop)
    }

    @Test fun glyphsMoveWithThePillNotTheCamera() {
        val base = EdgePillLayoutMath.compute(hole, hasSecondary = false)
        val moved = EdgePillLayoutMath.compute(hole.copy(tuning = LEGACY.copy(yOffsetDp = 60f, xOffsetDp = 8f)), hasSecondary = false)
        assertEquals(EdgePillLayoutMath.glyphSlots(base), EdgePillLayoutMath.glyphSlots(moved))
    }

    @Test fun glyphsAreCentredAcrossThePillWidth() {
        for (w in listOf(24f, 40f, 60f)) {
            val l = EdgePillLayoutMath.compute(hole.copy(tuning = EdgePillTuning(widthDp = w)), hasSecondary = false)
            val s = EdgePillLayoutMath.glyphSlots(l)
            assertEquals(w / 2f, s.centreX, 0.001f)
        }
    }

    @Test fun flatEdgeIsItsOwnChoiceAndDefaultsOff() {
        val t = EdgePillTuning(flatEdge = true)
        assertEquals(t, EdgePillTuning.decode(t.encode()))
        assertTrue(EdgePillTuning.decode(t.encode()).flatEdge)
        assertTrue(!EdgePillTuning.decode(null).flatEdge)
        assertTrue(!EdgePillTuning.decode("w=5").flatEdge)
        assertTrue(!EdgePillTuning.decode("fe=NaN").flatEdge)
    }

    @Test fun contentLengthNeedsSameSizeOff() {
        val same = EdgePillTuning(heightDp = 100f, contentSameSize = true, contentPercent = 200f)
        assertEquals(100f, same.contentHeightDp(400f), 0.001f)
        val stretch = EdgePillTuning(heightDp = 100f, contentSameSize = false, contentPercent = 150f)
        assertEquals(150f, stretch.contentHeightDp(400f), 0.001f)
        // kept inside the screen (24 dp margin each side)
        assertEquals(280f - 48f, stretch.copy(contentPercent = 250f).contentHeightDp(280f), 0.001f)
        assertEquals(250f, stretch.copy(contentPercent = 250f).contentHeightDp(400f), 0.001f)
        assertEquals(100f, stretch.copy(contentPercent = 250f).contentHeightDp(90f), 0.001f)
    }

    @Test fun cameraLengthFollowsTheCardNotTheWidth() {
        val t = EdgePillTuning(heightDp = 100f, widthDp = 32f, contentSameSize = false, contentPercent = 150f,
            bubblePos = EdgePillTuning.BUBBLE_BELOW)
        val bare = hole.copy(tuning = t)
        val card = hole.copy(tuning = t, withContent = true)
        assertEquals(100f, bare.pillHeightDp, 0.001f)
        assertEquals(150f, card.pillHeightDp, 0.001f)
        val l = EdgePillLayoutMath.compute(card, hasSecondary = true)
        assertEquals(150f, l.pill.height, 0.001f)
        assertEquals(32f, l.pill.width, 0.001f)
        // still centred on the camera, and the bubble clears the longer pill
        assertEquals(231f, l.pill.top + 75f, 0.001f)
        assertEquals(l.pill.bottom + t.bubbleGapDp, l.secondary!!.top, 0.001f)
        assertEquals(l.pill.top + 75f, EdgePillLayoutMath.cardPlacement(card, 300f).pillCentreYDp, 0.001f)
    }

    @Test fun valueSlotFollowsThePillThicknessAndItsRoundedEnd() {
        val m = EdgePillLayoutMath
        // a straight stretch of the pill: thickness less the padding
        assertEquals(30f, m.valueWidthDp(34f, 72f, 8f, 40f, 22f), 0.001f)
        // wider pill, wider slot
        assertEquals(56f, m.valueWidthDp(60f, 72f, 8f, 40f, 22f), 0.001f)
        // a fully round end close to the pill's tip is narrower than the pill
        val tip = m.valueWidthDp(34f, 40f, 17f, 2f, 22f)
        assertTrue(tip < 30f)
        // never below the minimum
        assertEquals(m.VALUE_MIN_WIDTH_DP, m.valueWidthDp(18f, 72f, 9f, 0f, 22f), 0.001f)
    }
}

class EdgePillLayoutTest {
    @Test
    fun resolvedCameraGeometry() {
        val reportedLeft = EdgePillLayoutMath.resolveCamera(-300f, 90f, rotationOnLeft = false, screenWidthDp = 700f, screenHeightDp = 340f, tuning = LEGACY)
        assertTrue(reportedLeft.onLeft)
        val right = EdgePillLayoutMath.resolveCamera(300f, 90f, rotationOnLeft = true, screenWidthDp = 700f, screenHeightDp = 340f, tuning = LEGACY)
        assertTrue(!right.onLeft)

        val missingCam = EdgePillLayoutMath.resolveCamera(null, null, rotationOnLeft = true, screenWidthDp = 700f, screenHeightDp = 340f, tuning = LEGACY)
        assertTrue(missingCam.onLeft)
        assertEquals(170f, missingCam.centerYDp, 0.001f)

        val clampedCam = EdgePillLayoutMath.resolveCamera(-300f, 4f, true, 700f, 340f, tuning = LEGACY)
        assertEquals(36f, clampedCam.centerYDp, 0.001f)
        val low = EdgePillLayoutMath.resolveCamera(-300f, 400f, true, 700f, 340f, tuning = LEGACY)
        assertEquals(304f, low.centerYDp, 0.001f)

        val base = EdgePillLayoutMath.resolveCamera(-474f, 231f, true, 1000f, 462f, cameraWidthDp = 20f, tuning = LEGACY)
        val moved = base.copy(tuning = LEGACY.copy(xOffsetDp = 15f))
        val a = EdgePillLayoutMath.compute(base, hasSecondary = true).secondary!!
        val b = EdgePillLayoutMath.compute(moved, hasSecondary = true).secondary!!
        assertEquals(a.left + 15f, b.left, 0.001f)

        assertEquals(24f, EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY).pillWidthDp, 0.001f)
        val noCutout = EdgePillLayoutMath.resolveCamera(null, null, true, 700f, 340f, tuning = LEGACY)
        assertEquals(24f, noCutout.pillWidthDp, 0.001f)
        assertEquals(60f, EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY.copy(widthDp = 500f)).pillWidthDp, 0.001f)
    }

    @Test
    fun edgePillPlacement() {
        val flush = EdgePillLayoutMath.compute(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = false)
        assertEquals(0f, flush.pill.left, 0.001f)
        assertEquals(64f, flush.pill.top, 0.001f)
        assertNull(flush.secondary)
        val flushR = EdgePillLayoutMath.compute(EdgeCamera(false, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = false)
        assertEquals(676f, flushR.pill.left, 0.001f)

        val upper = EdgePillLayoutMath.compute(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = true)
        val s = upper.secondary!!
        assertEquals(upper.pill.bottom + 8f, s.top, 0.001f)
        assertEquals(0f, s.left, 0.001f)

        val t = LEGACY.copy(xOffsetDp = 20f)
        val inset = EdgePillLayoutMath.compute(EdgeCamera(true, 100f, 700f, 340f, tuning = t), hasSecondary = true)
        assertEquals(20f, inset.pill.left, 0.001f)
        assertEquals(20f, inset.secondary!!.left, 0.001f)
        val insetR = EdgePillLayoutMath.compute(EdgeCamera(false, 250f, 700f, 340f, tuning = t), hasSecondary = true)
        assertEquals(700f - 24f - 20f, insetR.pill.left, 0.001f)
        assertEquals(700f - 24f - 20f, insetR.secondary!!.left, 0.001f)

        val widthCam = EdgePillLayoutMath.resolveCamera(-474f, 231f, true, 1000f, 462f, cameraWidthDp = 20f, tuning = LEGACY.copy(widthDp = 42f))
        assertEquals(26f, widthCam.centerFromEdgeDp, 0.001f)
        assertEquals(42f, widthCam.pillWidthDp, 0.001f)
        val widthLayout = EdgePillLayoutMath.compute(widthCam, hasSecondary = false)
        assertEquals(26f, EdgePillLayoutMath.holeLocalX(widthLayout), 0.001f)
        assertEquals(42f, widthLayout.pill.width, 0.001f)
        assertTrue(widthLayout.pill.left <= 26f - 10f)
        assertTrue(widthLayout.pill.right >= 26f + 10f)

        val rightCam = EdgePillLayoutMath.resolveCamera(474f, 231f, false, 1000f, 462f, cameraWidthDp = 20f, tuning = LEGACY.copy(widthDp = 42f))
        val rightLayout = EdgePillLayoutMath.compute(rightCam, hasSecondary = false)
        assertEquals(1000f - 42f, rightLayout.pill.left, 0.001f)
        assertEquals(42f - 26f, EdgePillLayoutMath.holeLocalX(rightLayout), 0.001f)
    }

    @Test
    fun secondaryBubbleBounds() {
        val lower = EdgePillLayoutMath.compute(EdgeCamera(false, 250f, 700f, 340f, tuning = LEGACY), hasSecondary = true)
        val s = lower.secondary!!
        assertEquals(lower.pill.top - 8f - EdgePillTuning.bubbleSizeFor(lower.pill.width), s.top, 0.001f)
        assertEquals(676f, s.left, 0.001f)

        val bounded = EdgePillLayoutMath.compute(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = true)
        val b = EdgePillLayoutMath.touchBounds(bounded)
        assertEquals(0f, b.left, 0.001f)
        assertTrue(b.right >= bounded.pill.right)
        assertTrue(b.bottom >= bounded.secondary!!.bottom)
        assertTrue(b.top >= 0f && b.bottom <= 340f)
    }

    @Test fun cameraSideEdgeStaysPut() {
        val half = 350f
        val left = EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY)
        val right = EdgeCamera(false, 100f, 700f, 340f, tuning = LEGACY)
        for (w in listOf(300f, 420f, 560f)) {
            val l = EdgePillLayoutMath.cardPlacement(left, w).cardCentreOffsetDp
            val r = EdgePillLayoutMath.cardPlacement(right, w).cardCentreOffsetDp
            assertEquals(32f, l - w / 2f + half, 0.001f)
            assertEquals(700f - 32f, r + w / 2f + half, 0.001f)
        }
    }

    @Test fun placementReportsTheCameraSideEdges() {
        val left = EdgePillLayoutMath.cardPlacement(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), cardWidthDp = 350f)
        assertEquals(-350f, left.pillEdgeOffsetDp, 0.001f)
        assertEquals(-350f + 32f, left.cardEdgeOffsetDp, 0.001f)
        val right = EdgePillLayoutMath.cardPlacement(EdgeCamera(false, 100f, 700f, 340f, tuning = LEGACY), cardWidthDp = 350f)
        assertEquals(350f, right.pillEdgeOffsetDp, 0.001f)
        assertEquals(350f - 32f, right.cardEdgeOffsetDp, 0.001f)
    }

    @Test fun cardSitsEightDpFromThePillOnTheCameraSide() {
        val left = EdgePillLayoutMath.cardPlacement(EdgeCamera(true, 100f, 700f, 340f, tuning = LEGACY), cardWidthDp = 350f)
        assertEquals(24f + 8f + 175f - 350f, left.cardCentreOffsetDp, 0.001f)
        assertEquals(12f - 350f, left.pillCentreOffsetDp, 0.001f)
        assertEquals(100f, left.pillCentreYDp, 0.001f)
        val right = EdgePillLayoutMath.cardPlacement(EdgeCamera(false, 100f, 700f, 340f, tuning = LEGACY), cardWidthDp = 350f)
        assertEquals(700f - 24f - 8f - 175f - 350f, right.cardCentreOffsetDp, 0.001f)
        assertEquals(700f - 12f - 350f, right.pillCentreOffsetDp, 0.001f)
    }

    @Test fun cardNeverTouchesThePill() {
        for (onLeft in listOf(true, false)) {
            val cam = EdgeCamera(onLeft, 170f, 800f, 360f, tuning = LEGACY)
            val p = EdgePillLayoutMath.cardPlacement(cam, cardWidthDp = 400f)
            val half = 400f
            val cardLeft = p.cardCentreOffsetDp + half - 200f
            val pillLeft = if (onLeft) 0f else 800f - 24f
            val gap = if (onLeft) cardLeft - (pillLeft + 24f) else pillLeft - (cardLeft + 400f)
            assertEquals(8f, gap, 0.001f)
        }
    }

    @Test fun cardStaysEightDpFromAWiderPill() {
        val cam = EdgePillLayoutMath.resolveCamera(-474f, 231f, true, 1000f, 462f, cameraWidthDp = 20f, tuning = LEGACY.copy(widthDp = 42f))
        val p = EdgePillLayoutMath.cardPlacement(cam, cardWidthDp = 400f)
        val cardLeft = p.cardCentreOffsetDp + 500f - 200f
        assertEquals(cam.pillWidthDp + 8f, cardLeft, 0.001f)
    }
}

class EdgePillFitTest {
    private val eps = 0.001f

    @Test
    fun compactAndFlatFit() {
        val compact = EdgePillFit.fit(EdgePillTuning(yOffsetDp = 30f), centreFromEdgeDp = 30f, acrossDp = 40f)
        assertEquals(40f, compact.widthDp, eps)
        assertEquals(10f, compact.xOffsetDp, eps)
        assertEquals(0f, compact.yOffsetDp, eps)
        assertEquals(20f, compact.cornerRadiusDp, eps)

        val small = EdgePillFit.fit(EdgePillTuning(), centreFromEdgeDp = 14f, acrossDp = 12f)
        assertEquals(24f, small.widthDp, eps)
        assertEquals(2f, small.xOffsetDp, eps)

        assertEquals(0f, EdgePillFit.fit(EdgePillTuning(), 10f, 40f).xOffsetDp, eps)

        val flat = EdgePillFit.fit(EdgePillTuning(flatEdge = true, xOffsetDp = 9f), 30f, 40f)
        assertEquals(0f, flat.xOffsetDp, eps)
        assertEquals(50f, flat.widthDp, eps)

        val missing = EdgePillTuning(widthDp = 33f)
        assertEquals(missing, EdgePillFit.fitPortrait(missing, null))
        assertEquals(missing, EdgePillFit.fit(missing, 0f, 0f))
    }

    @Test
    fun portraitFitDimensions() {
        val portrait = EdgePillFit.fitPortrait(EdgePillTuning(), CutoutArea(centreXDp = 0f, topDp = 10f, widthDp = 14f, heightDp = 40f))
        assertEquals(40f, portrait.widthDp, eps)
        assertEquals(10f, portrait.xOffsetDp, eps)

        val longHole = EdgePillFit.fitPortrait(EdgePillTuning(), CutoutArea(0f, 10f, 150f, 40f))
        assertEquals(174f, longHole.heightDp, eps)
        assertEquals(40f, longHole.widthDp, eps)

        val fitted = EdgePillFit.fitPortrait(EdgePillTuning(), CutoutArea(0f, 10f, 75f, 32f))
        assertEquals(99f, fitted.heightDp, eps)

        val tinyHole = EdgePillFit.fitPortrait(EdgePillTuning(), CutoutArea(0f, 10f, 14f, 40f))
        assertEquals(EdgePillTuning.HEIGHT.start, tinyHole.heightDp, eps)

        val land = EdgePillFit.fitLandscape(EdgePillTuning(), -470f, 40f, 1000f, cameraLengthDp = 150f)
        val port = EdgePillFit.fitPortrait(EdgePillTuning(), CutoutArea(0f, 10f, 150f, 40f))
        assertEquals(port, land)
    }

    @Test fun landscapeReadingMatches() {
        val t = EdgePillFit.fitLandscape(EdgePillTuning(), cameraXOffsetDp = -470f, cameraWidthDp = 40f, screenWidthDp = 1000f)
        assertEquals(40f, t.widthDp, eps)
        assertEquals(10f, t.xOffsetDp, eps)
        assertEquals(t, EdgePillFit.fitLandscape(EdgePillTuning(), 470f, 40f, 1000f))
    }

    @Test fun hugeHoleLengthIsCapped() {
        assertEquals(EdgePillTuning.HEIGHT.endInclusive, EdgePillFit.lengthFor(400f), eps)
    }

    @Test
    fun flatCameraFit() {
        val flat = EdgePillTuning(flatEdge = true, xOffsetDp = 7f, yOffsetDp = 10f).coerced()
        assertEquals(0f, flat.xOffsetDp, eps)
        val flatCam = EdgePillLayoutMath.resolveCamera(-470f, 400f, true, 1000f, 462f, cameraWidthDp = 40f, tuning = flat, cameraLengthDp = 14f)
        val flatPill = EdgePillLayoutMath.compute(flatCam, hasSecondary = false).pill
        assertEquals(0f, flatPill.left, eps)
        assertEquals(231f + 10f, flatPill.top + flatPill.height / 2f, eps)

        for (cameraX in listOf(-470f, 470f)) for (y in -120..120 step 20) {
            val separated = EdgePillTuning(flatEdge = true, yOffsetDp = y.toFloat())
            val separatedCam = EdgePillLayoutMath.resolveCamera(cameraX, 40f, cameraX < 0f, 1000f, 411f, cameraWidthDp = 40f, tuning = separated, cameraLengthDp = 14f)
            val l = EdgePillLayoutMath.compute(separatedCam, hasSecondary = true)
            val b = l.secondary!!
            assertTrue("offset $y", b.bottom <= l.pill.top + eps || b.top >= l.pill.bottom - eps)
            assertTrue(b.top >= -eps && b.bottom <= 411f + eps)
            assertEquals(l.pill.left + l.pill.width / 2f, b.left + b.width / 2f, eps)
        }
    }
}

class EdgePillThickenedTest {
    private fun layout(onLeft: Boolean, secondary: Boolean = false) =
        EdgePillLayoutMath.compute(EdgeCamera(onLeft, 100f, 700f, 340f, tuning = LEGACY), hasSecondary = secondary)

    @Test
    fun edgeThickness() {
        val leftBase = layout(onLeft = true)
        val leftThick = EdgePillLayoutMath.thickened(leftBase, 2f)
        assertEquals(leftBase.pill.left, leftThick.pill.left, 0.001f)
        assertEquals(leftBase.pill.width + 2f, leftThick.pill.width, 0.001f)
        assertEquals(leftBase.pill.right + 2f, leftThick.pill.right, 0.001f)

        val rightBase = layout(onLeft = false)
        val rightThick = EdgePillLayoutMath.thickened(rightBase, 2f)
        assertEquals(rightBase.pill.right, rightThick.pill.right, 0.001f)
        assertEquals(rightBase.pill.left - 2f, rightThick.pill.left, 0.001f)
        assertEquals(rightBase.pill.width + 2f, rightThick.pill.width, 0.001f)

        for (onLeft in listOf(true, false)) {
            val fixedBase = layout(onLeft, secondary = true)
            val fixedThick = EdgePillLayoutMath.thickened(fixedBase, 2f)
            assertEquals(fixedBase.pill.top, fixedThick.pill.top, 0.001f)
            assertEquals(fixedBase.pill.height, fixedThick.pill.height, 0.001f)
            assertEquals(fixedBase.secondary, fixedThick.secondary)
        }

        val unchangedBase = layout(onLeft = false)
        assertEquals(unchangedBase, EdgePillLayoutMath.thickened(unchangedBase, 0f))
    }
}
