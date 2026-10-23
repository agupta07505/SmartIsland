/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.MERGE_PUSH_DEPTH_DP
import com.agupta07505.smartisland.ui.mergeBubbleAlpha
import com.agupta07505.smartisland.ui.mergeBubbleOffsetDp
import com.agupta07505.smartisland.ui.mergeBubbleScaleX
import com.agupta07505.smartisland.ui.mergeBubbleScaleY
import com.agupta07505.smartisland.ui.mergeEdgeDistanceFromOffsetDp
import com.agupta07505.smartisland.ui.mergeFaceFlat
import com.agupta07505.smartisland.ui.mergeGapDp
import com.agupta07505.smartisland.ui.mergeIconAlpha
import com.agupta07505.smartisland.ui.mergeJoin
import com.agupta07505.smartisland.ui.mergeNearCornerFactor
import com.agupta07505.smartisland.ui.mergeNeckHeightFactor
import com.agupta07505.smartisland.ui.mergePillScaleX
import com.agupta07505.smartisland.ui.mergePillScaleY
import com.agupta07505.smartisland.ui.mergePush
import com.agupta07505.smartisland.ui.mergeEdgeDistanceDp
import com.agupta07505.smartisland.ui.mergeContactFraction
import com.agupta07505.smartisland.ui.mergeStretch
import com.agupta07505.smartisland.ui.mergeSwell
import com.agupta07505.smartisland.ui.mergeTravelDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeAnimationTest {
    // Pill 120 wide, bubble 36 wide, centres 110 apart: edge gap = 110 - 60 - 18 = 32.
    private val dist = 110f
    private val pill = 120f
    private val bubble = 36f

    @Test
    fun travelStopsInsidePillSide_notAtCentre() {
        val travel = mergeTravelDp(dist, pill, bubble)
        assertEquals(32f + 36f * 1.15f, travel, 0.001f)
        assertTrue("must not reach the pill centre", travel < dist)
    }

    @Test
    fun bubbleEndsFullyInsideThePill() {
        // Bubble centre ends (travel - gap - bubble/2) inside the pill edge; its far edge must also be inside.
        val travel = mergeTravelDp(dist, pill, bubble)
        val centreInside = travel - 32f - bubble / 2f
        assertTrue(centreInside - bubble / 2f >= 0f)
    }

    @Test
    fun narrowPillNeverSinksPastItsFarSide() {
        val travel = mergeTravelDp(40f, 30f, 36f)
        val gap = (40f - 15f - 18f).coerceAtLeast(0f)
        assertTrue(travel - gap <= 30f / 2f + 36f / 2f + 0.001f)
    }

    @Test
    fun negativeDistanceUsesSameTravel() {
        assertEquals(mergeTravelDp(dist, pill, bubble), mergeTravelDp(-dist, pill, bubble), 0.001f)
    }

    @Test
    fun contactHappensBeforeTheEnd() {
        val c = mergeContactFraction(dist, pill, bubble)
        assertTrue(c > 0f && c < 1f)
    }

    @Test
    fun stretchPeaksAtContactAndIsZeroAtEnds() {
        val c = mergeContactFraction(dist, pill, bubble)
        assertEquals(0f, mergeStretch(0f, c), 0.001f)
        assertEquals(0f, mergeStretch(1f, c), 0.001f)
        assertTrue(mergeStretch(c, c) > mergeStretch(c * 0.5f, c))
    }

    @Test
    fun swellOnlyAfterContact() {
        val c = mergeContactFraction(dist, pill, bubble)
        assertEquals(0f, mergeSwell(c * 0.5f, c), 0.001f)
        assertTrue(mergeSwell((c + 1f) / 2f, c) > 0.9f)
    }

    @Test
    fun bubbleStaysSolidUntilItIsInsideThePill() {
        assertEquals(1f, mergeBubbleAlpha(0.85f), 0.001f)
        assertEquals(0f, mergeBubbleAlpha(1f), 0.001f)
    }

    @Test
    fun edgeDistanceIsHalfABubbleAtContact() {
        val travel = mergeTravelDp(dist, pill, bubble)
        val c = mergeContactFraction(dist, pill, bubble)
        assertEquals(bubble / 2f, mergeEdgeDistanceDp(dist, pill, travel, c), 0.001f)
        assertTrue(mergeEdgeDistanceDp(dist, pill, travel, 1f) < 0f)
        assertEquals(
            mergeEdgeDistanceDp(dist, pill, travel, 0.5f),
            mergeEdgeDistanceDp(-dist, pill, travel, 0.5f),
            0.001f
        )
    }

    @Test
    fun aCutoutAlreadyTouchingThePillIsNotStretchedAtRest() {
        // contact 0: the round cutout beside a wide pill. It is not stretched at merge 0.
        assertEquals(0f, mergeStretch(0f, 0f), 0.001f)
        assertEquals(0f, mergeStretch(1f, 0f), 0.001f)
        assertTrue(mergeStretch(0.2f, 0f) > mergeStretch(0.05f, 0f))
        assertTrue(mergeStretch(0.2f, 0f) > mergeStretch(0.8f, 0f))
    }

    @Test
    fun stretchStaysInRangeForEveryContact() {
        for (c in listOf(0f, 0.1f, 0.5f, 0.99f, 1f)) {
            var m = 0f
            while (m <= 1f) {
                val v = mergeStretch(m, c)
                assertTrue("m=$m c=$c v=$v", v in 0f..0.3001f)
                m += 0.05f
            }
            assertEquals(0f, mergeStretch(0f, c), 0.001f)
        }
    }

    // push, then join

    private val gap = 32f
    private val contact = mergeContactFraction(dist, pill, bubble)

    @Test
    fun gapIsTheEmptySpaceBetweenTheFacingEdges() {
        assertEquals(gap, mergeGapDp(dist, pill, bubble), 0.001f)
        assertEquals(gap, mergeGapDp(-dist, pill, bubble), 0.001f)
        assertEquals(0f, mergeGapDp(70f, pill, bubble), 0f)
    }

    @Test
    fun pushIsZeroUntilContactAndOneWhenItIsOver() {
        assertEquals(0f, mergePush(0f, contact), 0f)
        assertEquals(0f, mergePush(contact, contact), 0f)
        assertEquals(1f, mergePush(1f, contact), 0.0001f)
        assertTrue(mergePush(contact + 0.05f, contact) in 0.01f..0.99f)
    }

    @Test
    fun joinStartsOnlyAfterThePushAndEndsAtOne() {
        assertEquals(0f, mergeJoin(contact, contact), 0f)
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(0f, mergeJoin(pushEnd, contact), 0.0001f)
        assertTrue(mergeJoin(pushEnd + 0.05f, contact) > 0f)
        assertEquals(1f, mergeJoin(1f, contact), 0.0001f)
    }

    @Test
    fun iconIsGoneBeforeTheJoinStarts() {
        assertEquals(1f, mergeIconAlpha(0f, contact), 0f)
        assertEquals(1f, mergeIconAlpha(contact, contact), 0f)
        val joinStart = contact + (1f - contact) * 0.37f
        assertEquals(0f, mergeIconAlpha(joinStart, contact), 0.0001f)
    }

    @Test
    fun bubbleReachesThePillEdgeAtContactThenIsPressedIn() {
        // Approach is a straight line to the touching position (half a bubble short of the pill's edge).
        assertEquals(0f, mergeBubbleOffsetDp(0f, contact, gap, bubble, pill), 0f)
        assertEquals(gap, mergeBubbleOffsetDp(contact, contact, gap, bubble, pill), 0.001f)
        assertEquals(
            bubble / 2f,
            mergeEdgeDistanceFromOffsetDp(dist, pill, mergeBubbleOffsetDp(contact, contact, gap, bubble, pill)),
            0.001f
        )
        // End of push: pressed in by 0.7 of the depth (face in by the full depth, body 0.6 narrower).
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(gap + 0.7f * MERGE_PUSH_DEPTH_DP, mergeBubbleOffsetDp(pushEnd, contact, gap, bubble, pill), 0.001f)
    }

    @Test
    fun bubbleEndsInsideThePillAndNeverBeyondItsFarSide() {
        val end = mergeBubbleOffsetDp(1f, contact, gap, bubble, pill)
        assertTrue(mergeEdgeDistanceFromOffsetDp(dist, pill, end) < 0f)
        assertEquals(gap + bubble / 2f + 14f, end, 0.001f)
        // A pill narrower than 28 dp: the bubble's centre stops at the pill's middle.
        val narrow = mergeBubbleOffsetDp(1f, 0.5f, 10f, 20f, 20f)
        assertEquals(10f + 10f + 10f, narrow, 0.001f)
    }

    @Test
    fun offsetNeverGoesBackwards() {
        var last = -1f
        for (i in 0..200) {
            val v = mergeBubbleOffsetDp(i / 200f, contact, gap, bubble, pill)
            assertTrue("step $i: $v < $last", v >= last - 0.0001f)
            last = v
        }
    }

    @Test
    fun bubbleIsPressedNarrowerAndTallerThenShrinksAway() {
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(1f, mergeBubbleScaleX(0f, contact, bubble), 0f)
        assertEquals(1f, mergeBubbleScaleY(contact, contact), 0f)
        assertEquals(1f - 0.6f * MERGE_PUSH_DEPTH_DP / bubble, mergeBubbleScaleX(pushEnd, contact, bubble), 0.001f)
        assertEquals(1.06f, mergeBubbleScaleY(pushEnd, contact), 0.001f)
        assertEquals(0f, mergeBubbleScaleX(1f, contact, bubble), 0.0001f)
        assertEquals(0f, mergeBubbleScaleY(1f, contact), 0.0001f)
    }

    @Test
    fun facesGoFlatInThePushAndRoundOffEarlyInTheJoin() {
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(0f, mergeFaceFlat(contact, contact), 0f)
        assertEquals(1f, mergeFaceFlat(pushEnd, contact), 0.0001f)
        assertEquals(0f, mergeFaceFlat(1f, contact), 0.0001f)
        assertEquals(1f, mergeNearCornerFactor(0f), 0f)
        assertEquals(0.35f, mergeNearCornerFactor(1f), 0.0001f)
    }

    @Test
    fun pillIsPushedTallerThenSwellsAndSettles() {
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(1f, mergePillScaleY(contact, contact, 34f), 0f)
        assertEquals(1.08f, mergePillScaleY(pushEnd, contact, 34f), 0.001f)
        // Joining: it swells past the pushed height, then rings out to its normal size.
        assertTrue(mergePillScaleY(pushEnd + 0.03f, contact, 34f) > 1.08f)
        assertEquals(1f, mergePillScaleY(1f, contact, 34f), 0.01f)
        assertEquals(1f, mergePillScaleX(contact, contact, 120f), 0f)
        assertEquals(1f, mergePillScaleX(1f, contact, 120f), 0.01f)
    }

    @Test
    fun bridgeIsOpenAtTheStartOfTheJoinAndClosedAfterwards() {
        val pushEnd = contact + (1f - contact) * 0.37f
        assertEquals(0f, mergeNeckHeightFactor(pushEnd, contact), 0f)
        assertTrue(mergeNeckHeightFactor(pushEnd + 0.02f, contact) > 0.8f)
        assertEquals(0f, mergeNeckHeightFactor(1f, contact), 0.0001f)
        for (i in 0..100) assertTrue(mergeNeckHeightFactor(i / 100f, contact) in 0f..1f)
    }

    @Test
    fun aCutoutAlreadyTouchingThePillStartsPushingAtOnce() {
        assertEquals(0f, mergeBubbleOffsetDp(0f, 0f, 0f, bubble, pill), 0f)
        assertTrue(mergePush(0.1f, 0f) > 0f)
        assertEquals(0f, mergePush(0f, 0f), 0f)
    }
}
