/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleCloseSequence
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapseMotionTest {
    @Test
    fun criticallyDampedSpringsDoNotOvershoot() {
        assertEquals(0f, CollapseMotion.overshootFraction(CollapseMotion.SETTLE_DAMPING), 0f)
        assertEquals(0f, CollapseMotion.overshootFraction(1.3f), 0f)
    }

    @Test
    fun lessDampingMeansMoreOvershoot() {
        assertTrue(CollapseMotion.overshootFraction(0.6f) > CollapseMotion.overshootFraction(0.72f))
        assertTrue(CollapseMotion.overshootFraction(0.72f) > CollapseMotion.overshootFraction(0.86f))
    }

    @Test
    fun idleCirclePressCloseHasNoBounce() {
        assertEquals(0f, CollapseMotion.overshootFraction(CollapseMotion.closeWidthDamping(true)), 0f)
        assertTrue(CollapseMotion.overshootFraction(CollapseMotion.closeWidthDamping(false)) > 0f)
        assertEquals(CollapseMotion.WIDTH_DAMPING, CollapseMotion.closeWidthDamping(false), 0f)
    }

    @Test
    fun idleCircleCloseSequence() {
        // the close's stiffness comes from the plan
        val plan = IdleCloseSequence.plan(AnimationSpeed.NORMAL)
        val settle = springTimeToProgressMs(plan.closeStiffness, CollapseMotion.closeWidthDamping(true), 0.98f)
        assertEquals(plan.step1Ms.toDouble(), settle.toDouble(), 2.0)
        val travelled = springProgress(plan.contentOutMs / 1000f, plan.closeStiffness, CollapseMotion.closeWidthDamping(true))
        assertTrue("shell was $travelled closed when the content is gone", travelled > 0.8f)
    }

    @Test
    fun cardContentFadesOutLongerThanBefore() {
        assertEquals(168, CollapseMotion.cardContentOutMs(280))
        assertEquals(132, CollapseMotion.cardContentOutMs(220))
        assertTrue(CollapseMotion.cardContentOutMs(280) > 280 / 2)
        assertEquals(1, CollapseMotion.cardContentOutMs(1))
    }

    // the pill content's wait follows how far open the island was

    @Test
    fun pillContentDelay() {
        // within 5 % of the full travel the width is already home
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(0f, CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING))
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(0.05f, CollapseMotion.STIFFNESS, CollapseMotion.WIDTH_DAMPING))

        val partialS = CollapseMotion.STIFFNESS
        val partial = CollapseMotion.WIDTH_DAMPING
        val quarter = CollapseMotion.pillContentDelayFromMs(0.25f, partialS, partial)
        val half = CollapseMotion.pillContentDelayFromMs(0.5f, partialS, partial)
        val partialFull = CollapseMotion.pillContentDelayFromMs(1f, partialS, partial)
        assertTrue(quarter < half)
        assertTrue(half < partialFull)

        val clampedS = CollapseMotion.STIFFNESS
        val clamped = CollapseMotion.WIDTH_DAMPING
        assertEquals(CollapseMotion.pillContentDelayFromMs(1f, clampedS, clamped), CollapseMotion.pillContentDelayFromMs(1.2f, clampedS, clamped))
        assertEquals(0, CollapseMotion.pillContentDelayFromMs(-0.3f, clampedS, clamped))
    }

    @Test fun edgePillCollapseHasNoBounce() {
        assertEquals(0f, CollapseMotion.overshootFraction(CollapseMotion.closeWidthDamping(false, noBounce = true)), 0f)
    }

    @Test
    fun collapseWidthCurve() {
        assertEquals(0f, CollapseMotion.widthTravel(0f), 0f)
        assertEquals(1f, CollapseMotion.widthTravel(1f), 0f)

        for (i in 0..10) assertEquals(i / 10f, CollapseMotion.widthTravel(i / 10f, CollapseMotion.SETTLE_DAMPING), 0f)

        var last = 0f
        for (i in 1..1000) {
            val v = CollapseMotion.widthTravel(i / 1000f)
            assertTrue(Math.abs(v - last) < 0.02f)
            last = v
        }
    }

    @Test
    fun widthSpringOvershoot() {
        assertTrue(CollapseMotion.widthTravel(0.5f) > 0.5f)
        var peak = 0f
        for (i in 0..1000) peak = maxOf(peak, CollapseMotion.widthTravel(i / 1000f))
        val expected = 1f + CollapseMotion.overshootFraction(CollapseMotion.WIDTH_DAMPING)
        assertEquals(expected, peak, 0.01f)
    }
}

class ContentPillWidthTest {
    private val portrait = OrientationState(isLandscape = false, screenWidthDp = 400f)

    @Test
    fun idleAndContentWidths() {
        val idle = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = false, contentPillWidthPercent = 150f)
        assertEquals(100f, idle.forOrientation(portrait, hasContent = false).width, 1e-3f)

        val stretched = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = false, contentPillWidthPercent = 150f)
        assertEquals(150f, stretched.forOrientation(portrait, hasContent = true).width, 1e-3f)

        val fixed = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = true, contentPillWidthPercent = 250f)
        assertEquals(100f, fixed.forOrientation(portrait, hasContent = true).width, 1e-3f)

        val capped = SmartIslandSettings.Default.copy(width = 200f, contentPillSameAsIdle = false, contentPillWidthPercent = 250f)
        assertEquals(352f, capped.forOrientation(portrait, hasContent = true).width, 1e-3f)
    }
}

class IslandExemptionTest {
    @Test
    fun listsAreSeparatePerOrientation() {
        val portrait = setOf("a.b")
        val landscape = setOf("c.d")
        Assert.assertTrue(IslandExemption.isOff("a.b", portrait, landscape, isLandscape = false))
        Assert.assertFalse(IslandExemption.isOff("a.b", portrait, landscape, isLandscape = true))
        Assert.assertTrue(IslandExemption.isOff("c.d", portrait, landscape, isLandscape = true))
        Assert.assertFalse(IslandExemption.isOff(null, portrait, landscape, isLandscape = false))
    }

    @Test
    fun landscapeWidthOnlyWhenAsked() {
        val s = SmartIslandSettings.Default.copy(landscapeWidth = 100f, contentPillSameAsIdleLandscape = false, contentPillWidthPercentLandscape = 200f)
        Assert.assertEquals(200f, s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true).width, 1e-3f)
        Assert.assertEquals(100f, s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true, hasCards = false).width, 1e-3f)
        Assert.assertEquals(100f, s.copy(contentPillSameAsIdleLandscape = true).forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true).width, 1e-3f)
    }
}

class ExpandMotionTest {
    // landscape expand spring: stiffness 280, damping 0.86
    private val lk = 280f
    private val lz = 0.86f
    private val pk = 400f
    private val pz = 0.8f

    @Test
    fun expandContentTiming() {
        val delay = ExpandMotion.cardInDelayMs(lk, lz)
        val end = delay + ExpandMotion.cardInMs(lk, lz)
        assertTrue("delay was $delay ms", delay in 20..45)
        // the shell is 90 % grown at about 193 ms
        assertTrue("end was $end ms", end in 170..215)
        assertTrue(end < 350)

        assertTrue(ExpandMotion.cardInDelayMs(pk, pz) < ExpandMotion.cardInDelayMs(lk, lz))
        assertTrue(
            ExpandMotion.cardInDelayMs(pk, pz) + ExpandMotion.cardInMs(pk, pz) <
                ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz)
        )

        val out = ExpandMotion.pillOutMs(lk, lz)
        val halfIn = ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz) / 2
        assertTrue("out was $out ms", out in 50..80)
        assertTrue(out < halfIn)

        val relaxed = ExpandMotion.cardInDelayMs(lk * 0.5f, lz) + ExpandMotion.cardInMs(lk * 0.5f, lz)
        val normal = ExpandMotion.cardInDelayMs(lk, lz) + ExpandMotion.cardInMs(lk, lz)
        val snappy = ExpandMotion.cardInDelayMs(lk * 2f, lz) + ExpandMotion.cardInMs(lk * 2f, lz)
        assertTrue(snappy < normal && normal < relaxed)

        assertEquals(ExpandMotion.MIN_FADE_MS, ExpandMotion.pillOutMs(1_000_000f, 1f))
        assertEquals(ExpandMotion.MIN_FADE_MS, ExpandMotion.cardInMs(1_000_000f, 1f))
    }

    @Test
    fun collapseFadesMirrorTheExpandFades() {
        // collapsing from the full card runs the expand's fades backwards
        val outDelay = ExpandMotion.cardOutDelayFromMs(1f, lk, lz)
        val outEnd = outDelay + ExpandMotion.cardOutMsFrom(1f, lk, lz)
        assertTrue("card out ends after it starts", outEnd > outDelay)
        assertEquals(ExpandMotion.pillOutMs(lk, lz), ExpandMotion.pillInMs(lk, lz))
        assertTrue(ExpandMotion.cardOutDelayFromMs(0.5f, lk, lz) <= outDelay)
        assertEquals(0, ExpandMotion.pillInDelayFromMs(0.2f, lk, lz))
    }

    @Test
    fun glideDuration() {
        for ((k, z) in listOf(pk to pz, lk to lz)) {
            val expandDone = ExpandMotion.expandDoneMs(k, z)
            val expandGlide = ExpandMotion.glideMs(k, z)
            assertEquals((expandDone * ExpandMotion.GLIDE_END_FRACTION).toInt(), expandGlide)
            // the bubble lands exactly when the island's expand is done
            assertEquals(expandDone, expandGlide)
        }

        // portrait (400 / 0.8): the expand is done at about 202 ms
        assertTrue(ExpandMotion.glideMs(pk, pz) in 190..215)
        // landscape (280 / 0.86): done at about 287 ms
        assertTrue(ExpandMotion.glideMs(lk, lz) in 270..300)
        // a softer spring (slower animation speed) gives a longer glide
        assertTrue(ExpandMotion.glideMs(pk / 4f, pz) > ExpandMotion.glideMs(pk, pz))

        assertTrue(ExpandMotion.glideMs(1_000_000f, 1f) >= ExpandMotion.MIN_GLIDE_MS)
        assertEquals(BubbleIntoCutout.BASE_MS, ExpandMotion.glideMs(0f, 0.8f))
    }
}

class ExpandOriginTest {
    private val eps = 0.001f

    private fun lerp(a: Float, b: Float, p: Float) = a + (b - a) * p

    @Test
    fun cameraEdgeDuringExpand() {
        // camera left: pill edge at -350 (the screen's edge), card edge at -318
        for (size in listOf(24f, 190f, 354f)) {
            for (p in listOf(0f, 0.3f, 0.7f, 1f)) {
                val start = ExpandOrigin.edgeStart(p, size, -350f, -318f, true)
                assertTrue(start >= -350f - eps)
                assertTrue(start <= -318f + eps)
            }
        }
        assertEquals(-350f, ExpandOrigin.edgeStart(0f, 24f, -350f, -318f, true), eps)
        assertEquals(-318f, ExpandOrigin.edgeStart(1f, 354f, -350f, -318f, true), eps)
        assertEquals(318f - 354f, ExpandOrigin.edgeStart(1f, 354f, 350f, 318f, false), eps)
        assertEquals(350f - 24f, ExpandOrigin.edgeStart(0f, 24f, 350f, 318f, false), eps)
    }

    @Test
    fun settledFromTheEndOfTheSpringOn() {
        assertTrue(ExpandOrigin.settled(1f))
        assertTrue(ExpandOrigin.settled(1.05f))
        assertTrue(!ExpandOrigin.settled(0.99f))
    }

    @Test
    fun aPillBelowTheCardMovesTheTopWhenTheCardChangesHeight() {
        // pill centre at 180, card top at 10
        val taller = ExpandOrigin.start(1f, 120f, 180f, 10f, 160f)
        val same = ExpandOrigin.start(1f, 120f, 180f, 10f, 120f)
        assertEquals(10f, same, eps)
        assertTrue(taller > same)
    }

    @Test
    fun startsOnThePillAndEndsOnTheCard() {
        // pill 120 wide centred at 200; card 400 wide starting at 100 (centre 300)
        val s0 = ExpandOrigin.start(0f, 120f, 200f, 100f, 400f)
        val s1 = ExpandOrigin.start(1f, 400f, 200f, 100f, 400f)
        assertEquals(140f, s0, eps)
        assertEquals(100f, s1, eps)
    }

    @Test
    fun pillCentreStaysPutWhileItIsInsideTheCard() {
        val cardStart = 100f
        val cardSize = 400f
        val pillCentre = 200f
        for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val size = lerp(120f, cardSize, p)
            val start = ExpandOrigin.start(p, size, pillCentre, cardStart, cardSize)
            val inIsland = ExpandOrigin.anchorInCard(pillCentre, cardStart, cardSize)
            val g = 0.5f + (inIsland - 0.5f) * p
            assertEquals(pillCentre, start + g * size, eps)
        }
    }

    @Test
    fun centredPillGrowsSymmetrically() {
        for (p in listOf(0f, 0.3f, 0.7f, 1f)) {
            val size = lerp(100f, 360f, p)
            val start = ExpandOrigin.start(p, size, 180f, 0f, 360f)
            assertEquals(180f - size / 2f, start, eps)
        }
    }

    @Test
    fun pillOutsideClampsToNearestEdge() {
        // pill centre at 40, card spans 100..500
        assertEquals(0f, ExpandOrigin.anchorInCard(40f, 100f, 400f), eps)
        assertEquals(100f, ExpandOrigin.anchorAtEnd(40f, 100f, 400f), eps)
        assertEquals(1f, ExpandOrigin.anchorInCard(900f, 100f, 400f), eps)
        assertEquals(500f, ExpandOrigin.anchorAtEnd(900f, 100f, 400f), eps)
        // both ends are still exact
        assertEquals(40f - 60f, ExpandOrigin.start(0f, 120f, 40f, 100f, 400f), eps)
        assertEquals(100f, ExpandOrigin.start(1f, 400f, 40f, 100f, 400f), eps)
    }

    @Test
    fun pillLeftOfTheCardGrowsTowardItWithoutMovingBack() {
        // pill centre 40, card starts at 100
        var last = -Float.MAX_VALUE
        for (i in 0..20) {
            val p = i / 20f
            val start = ExpandOrigin.start(p, lerp(120f, 400f, p), 40f, 100f, 400f)
            // the anchor slides while the island is still small
            assertTrue("start edge must not move back at p=$p", start >= last - 1f)
            last = maxOf(last, start)
        }
    }

    @Test
    fun pillRightOfTheCardGrowsTowardItWithoutMovingBack() {
        var last = Float.MAX_VALUE
        for (i in 0..20) {
            val p = i / 20f
            val start = ExpandOrigin.start(p, lerp(120f, 400f, p), 900f, 100f, 400f)
            assertTrue("start edge must not move back at p=$p", start <= last + eps)
            last = start
        }
    }

    @Test
    fun emptyCardFallsBackToMiddle() {
        assertEquals(0.5f, ExpandOrigin.anchorInCard(10f, 0f, 0f), eps)
    }

    /** `IslandLayout.animatedXOffset` is `start + size / 2` */
    @Test
    fun islandCentreAtRestIsThePillCentre() {
        for (pillCentre in listOf(-300f, -100f, 0f, 150f, 420f)) {
            val start = ExpandOrigin.start(0f, 112f, pillCentre, -200f, 400f)
            assertEquals(pillCentre, start + 112f / 2f, eps)
        }
    }
}

class ExpandTravelTest {
    @Test
    fun travelSlowdown() {
        assertEquals(1f, ExpandTravel.slowdown(100f, 200f, 200f, 400f), 0.0001f)
        assertEquals(ExpandTravel.MAX_SLOWDOWN, ExpandTravel.slowdown(0f, 350f, 20f, 400f), 0.0001f)
        assertEquals(1f, ExpandTravel.slowdown(0f, 60f, 60f, 400f), 0.0001f)

        val far = ExpandTravel.slowdown(40f, 300f, 60f, 400f)
        val near = ExpandTravel.slowdown(40f, 100f, 60f, 400f)
        assertTrue(far > 1f)
        assertEquals(1f, near, 0.0001f)

        val slow = ExpandTravel.slowdown(100f, 220f, 20f, 400f)
        assertTrue(slow > 1f && slow < ExpandTravel.MAX_SLOWDOWN)
    }



    @Test
    fun stiffnessFallsWithTheSquareOfTheSlowdown() {
        assertEquals(100f, ExpandTravel.stiffness(100f, 1f), 0.0001f)
        assertEquals(25f, ExpandTravel.stiffness(100f, 2f), 0.0001f)
    }
}

class ExpandedAnchorTest {
    private val screen = 400f
    private val card = 340f

    @Test
    fun expandedCardAnchor() {
        assertEquals(0f, ExpandedAnchor.centreOffsetDp(null, card, screen), 0f)
        assertEquals(0f, ExpandedAnchor.centreOffsetDp(0f, card, screen), 0f)
        assertEquals(0f, ExpandedAnchor.centreOffsetDp(CAMERA_CENTRED_TOLERANCE_DP, card, screen), 0f)

        // 200 dp card on a 400 dp screen: room for a centre up to 92 dp either side
        assertEquals(60f, ExpandedAnchor.centreOffsetDp(60f, 200f, screen), 0.001f)
        assertEquals(-60f, ExpandedAnchor.centreOffsetDp(-60f, 200f, screen), 0.001f)

        // 340 dp card: 30 dp of slack per side, less the 8 dp margin leaves 22 dp
        assertEquals(22f, ExpandedAnchor.centreOffsetDp(170f, card, screen), 0.001f)
        assertEquals(-22f, ExpandedAnchor.centreOffsetDp(-170f, card, screen), 0.001f)
        assertEquals(10f, ExpandedAnchor.centreOffsetDp(10f, card, screen), 0.001f)

        for (camera in -200..200 step 5) {
            for (width in listOf(180f, 260f, 340f, 392f, 420f)) {
                val boundedCentre = ExpandedAnchor.centreOffsetDp(camera.toFloat(), width, screen)
                val left = screen / 2f + boundedCentre - width / 2f
                val right = screen / 2f + boundedCentre + width / 2f
                if (width <= screen - 2 * ExpandedAnchor.EDGE_MARGIN_DP) {
                    assertTrue("left $left for camera $camera width $width", left >= ExpandedAnchor.EDGE_MARGIN_DP - 0.001f)
                    assertTrue("right $right for camera $camera width $width", right <= screen - ExpandedAnchor.EDGE_MARGIN_DP + 0.001f)
                } else {
                    assertEquals(0f, boundedCentre, 0f)
                }
            }
        }

        val anchoredCentre = ExpandedAnchor.centreOffsetDp(-50f, 200f, screen)
        val start = anchoredCentre - 200f / 2f
        assertEquals(-150f, start, 0.001f)
        // the camera is inside the card
        assertTrue(-50f in start..(start + 200f))
    }
}
