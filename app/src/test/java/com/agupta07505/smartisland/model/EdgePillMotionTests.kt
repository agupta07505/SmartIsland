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

class EdgePillMotionTest {
    @Test fun pillIsCentredOnTheCamera() {
        assertEquals(54f, EdgePillRules.pillTopDp(90f, 72f), 0.001f)
        assertEquals(10f, EdgePillRules.pillTopDp(46f, 72f), 0.001f)
    }

    @Test fun cardSitsEightDpFromPill() {
        assertEquals(32f, EdgePillRules.cardLeftDp(340f, 24f, 170f, cameraOnLeft = true), 0.001f)
        assertEquals(138f, EdgePillRules.cardLeftDp(340f, 24f, 170f, cameraOnLeft = false), 0.001f)
    }

    @Test fun layoutStyleFallsBackToEdge() {
        assertEquals(LandscapeLayoutStyle.EDGE, LandscapeLayoutStyle.fromId(null))
        assertEquals(LandscapeLayoutStyle.EDGE, LandscapeLayoutStyle.fromId("wobble"))
        assertEquals(LandscapeLayoutStyle.CLASSIC, LandscapeLayoutStyle.fromId("classic"))
        assertTrue(LandscapeLayoutStyle.isEdge("edge"))
        assertFalse(LandscapeLayoutStyle.isEdge("classic"))
    }

    @Test fun edgePillLandscapeSprings() {
        assertTrue(EdgePillRules.pillStretches(edgeLayoutOn = true, widgetPill = false))
        assertTrue(EdgePillRules.pillStretches(edgeLayoutOn = true, widgetPill = true))
        assertTrue(EdgePillRules.pillStretches(edgeLayoutOn = false, widgetPill = true))
        assertFalse(EdgePillRules.pillStretches(edgeLayoutOn = false, widgetPill = false))
    }

    @Test fun cameraSideIsFlatOnlyWithTheSwitchOnAndTheEdgeTouched() {
        assertTrue(EdgePillRules.flatCameraSide(true, 0f))
        assertTrue(EdgePillRules.flatCameraSide(true, 0.5f))
        assertFalse(EdgePillRules.flatCameraSide(true, 4f))
        assertFalse(EdgePillRules.flatCameraSide(false, 0f))
    }

    @Test fun shellCameraSideIsFlatAsThePillAndRoundWhenOpen() {
        assertEquals(1f, EdgePillRules.shellFlat(0f), 0.001f)
        assertEquals(0.5f, EdgePillRules.shellFlat(0.5f), 0.001f)
        assertEquals(0f, EdgePillRules.shellFlat(1f), 0.001f)
        assertEquals(0f, EdgePillRules.shellFlat(1.2f), 0.001f)
        assertEquals(1f, EdgePillRules.shellFlat(-0.2f), 0.001f)
    }

    @Test
    fun shellOwnsTheSurfaceUntilTheLastCardFinishesClosing() {
        assertTrue(EdgePillRules.shellIsSurface(layoutOn = true, cardsAway = false, hasCards = true, expanded = false, shellWide = false))
        assertTrue(EdgePillRules.shellIsSurface(layoutOn = true, cardsAway = false, hasCards = true, expanded = true, shellWide = true))
        assertTrue(EdgePillRules.shellIsSurface(layoutOn = true, cardsAway = false, hasCards = false, expanded = false, shellWide = true))
        assertFalse(EdgePillRules.shellIsSurface(layoutOn = true, cardsAway = false, hasCards = false, expanded = false, shellWide = false))
        assertFalse(EdgePillRules.shellIsSurface(layoutOn = true, cardsAway = true, hasCards = false, expanded = false, shellWide = false))
        assertFalse(EdgePillRules.shellIsSurface(layoutOn = false, cardsAway = false, hasCards = true, expanded = true, shellWide = true))
    }

    @Test
    fun pillSurfaceOpacity() {
        assertEquals(0.8f, EdgePillRules.pillSurfaceAlpha(false, otherAtPill = false, clearance = 0f, opacity = 0.8f), 0f)
        assertEquals(0.8f, EdgePillRules.pillSurfaceAlpha(false, otherAtPill = true, clearance = 0f, opacity = 0.8f), 0f)

        assertEquals(0f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = false, clearance = 1f, opacity = 1f), 0f)
        assertEquals(0f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = false, clearance = 0f, opacity = 1f), 0f)

        assertEquals(0f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0f, opacity = 1f), 0f)
        assertEquals(0.25f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.25f, opacity = 1f), 0f)
        assertEquals(0.5f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.5f, opacity = 1f), 0f)
        assertEquals(0.9f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 1f, opacity = 0.9f), 0f)
        assertEquals(0.9f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 7f, opacity = 0.9f), 0f)

        // fullAt = GLIDE_CLEARANCE (0.5)
        assertEquals(0.5f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.25f, opacity = 1f, fullAt = 0.5f), 1e-6f)
        assertEquals(1f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.5f, opacity = 1f, fullAt = 0.5f), 1e-6f)
        assertEquals(0.8f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.9f, opacity = 0.8f, fullAt = 0.5f), 1e-6f)
        assertEquals(0f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0f, opacity = 0.8f, fullAt = 0.5f), 0f)

        assertEquals(1f, EdgePillRules.pillSurfaceAlpha(true, otherAtPill = true, clearance = 0.5f, opacity = 1f, fullAt = 0f), 0f)
    }

    private fun motion(onLeft: Boolean = true, across: Float = 0f, along: Float = 0f, arm: Float = 0f, dead: Float = 0f, pull: Float = 0f, pulse: Float = 1f) =

        EdgePillRules.pillMotion(onLeft, across, along, arm, dead, pull, pulse)

    @Test fun aPillAtRestHasNoMotion() {
        val m = motion()
        assertEquals(0f, m.translationX, 0f)
        assertEquals(0f, m.translationY, 0f)
        assertEquals(1f, m.scaleX, 0f)
        assertEquals(1f, m.scaleY, 0f)
    }

    @Test
    fun edgePillTransforms() {
        assertEquals(6f, motion(onLeft = true, across = 6f).translationX, 0f)
        assertEquals(-6f, motion(onLeft = false, across = 6f).translationX, 0f)

        assertEquals(-12f, motion(onLeft = true, along = -12f).translationY, 0f)
        assertEquals(-12f, motion(onLeft = false, along = -12f).translationY, 0f)
        assertEquals(0f, motion(along = -12f).translationX, 0f)

        assertEquals(0f, motion(onLeft = true).originX, 0f)
        assertEquals(1f, motion(onLeft = false).originX, 0f)

        val m = motion(pulse = 1.125f)
        assertEquals(1f, m.scaleX, 0f)
        assertEquals(1.125f, m.scaleY, 1e-6f)

        assertEquals(EdgePillRules.cueScale(1f, 0f), motion(arm = 1f).scaleX, 1e-6f)
        assertEquals(EdgePillRules.cueScale(1f, 0f), motion(arm = 1f).scaleY, 1e-6f)
        assertEquals(EdgePillRules.cueScale(0f, 1f), motion(dead = 1f).scaleX, 1e-6f)
    }

    @Test
    fun anchoredPullScale() {
        val m = motion(dead = 1f, pull = 1f)
        assertEquals(SwipeDownPull.pillScaleY(1f), m.scaleX, 1e-6f)
        assertEquals(SwipeDownPull.pillScaleX(1f), m.scaleY, 1e-6f)
        assertTrue(m.scaleX > m.scaleY)

        val s = SwipeDownPull.pillScaleY(1f)
        assertEquals(
            SwipeDownPull.topAnchorShiftPx(s, 80f),
            EdgePillRules.anchoredSideShiftPx(s, 80f, cameraOnLeft = true), 0f
        )
    }

    @Test
    fun shadowTracksPillOpacity() {
        assertEquals(1f, EdgePillRules.shellShadowShare(false, 0f), 0f)
        assertEquals(1f, EdgePillRules.shellShadowShare(false, 1f), 0f)
        assertEquals(0f, EdgePillRules.shellShadowShare(true, 0f), 0f)
        assertEquals(0.4f, EdgePillRules.shellShadowShare(true, 0.4f), 1e-6f)
        assertEquals(1f, EdgePillRules.shellShadowShare(true, 1f), 0f)
        assertEquals(1f, EdgePillRules.shellShadowShare(true, 3f), 0f)
        assertEquals(0f, EdgePillRules.shellShadowShare(true, -1f), 0f)
    }


    @Test
    fun exitAnimation() {
        // in place: no slide, solid
        assertEquals(0f, EdgePillRules.exitSlidePx(true, 40f, 1f), 0f)
        assertEquals(-40f, EdgePillRules.exitSlidePx(true, 40f, 0f), 0.0001f)
        assertEquals(40f, EdgePillRules.exitSlidePx(false, 40f, 0f), 0.0001f)
        assertEquals(-20f, EdgePillRules.exitSlidePx(true, 40f, 0.5f), 0.0001f)
        assertEquals(1f, EdgePillRules.exitAlpha(1f), 0.0001f)
        assertEquals(0f, EdgePillRules.exitAlpha(0f), 0.0001f)
        assertEquals(0f, EdgePillRules.exitAlpha(0.15f), 0.0001f)

        assertEquals(1f, EdgePillRules.exitAlpha(0.8f), 0.0001f)
        assertTrue(EdgePillRules.exitAlpha(0.35f) in 0.01f..0.99f)
    }

    @Test
    fun travelClearsThePillItsGapAndAMargin() {
        assertEquals(10f + 24f + PILL_EXIT_MARGIN_DP, EdgePillRules.exitTravelDp(24f, 10f), 0.0001f)
        assertEquals(24f + PILL_EXIT_MARGIN_DP, EdgePillRules.exitTravelDp(24f, -5f), 0.0001f)
    }

    @Test
    fun hideRunsWhileHeldLayerSlides() {
        assertTrue(EdgePillRules.hideRunning(edgeStyle = true, pillHidden = true, hasCards = false, layerAlive = true))
        // slide finished, cards on the island, not hidden, or not the edge layout
        assertFalse(EdgePillRules.hideRunning(true, true, hasCards = false, layerAlive = false))
        assertFalse(EdgePillRules.hideRunning(true, true, hasCards = true, layerAlive = true))
        assertFalse(EdgePillRules.hideRunning(true, false, hasCards = false, layerAlive = true))
        assertFalse(EdgePillRules.hideRunning(false, true, hasCards = false, layerAlive = true))
    }

    @Test
    fun holdHidesIdleCutout() {
        assertTrue(EdgePillRules.idleHidden(edgeStyle = true, pillHidden = true, hasCards = false))
        assertFalse(EdgePillRules.idleHidden(edgeStyle = true, pillHidden = true, hasCards = true))
        assertFalse(EdgePillRules.idleHidden(edgeStyle = true, pillHidden = false, hasCards = false))
        assertFalse(EdgePillRules.idleHidden(edgeStyle = false, pillHidden = true, hasCards = false))
    }

    @Test
    fun holdTogglesStoredPill() {
        assertTrue(EdgePillRules.cardsAwayShown(cardsAway = true, pillHidden = false))
        assertFalse(EdgePillRules.cardsAwayShown(cardsAway = true, pillHidden = true))
        assertFalse(EdgePillRules.cardsAwayShown(cardsAway = false, pillHidden = false))
    }

    @Test
    fun theIdleDotStandsInForACardsEndButNotForAUserHide() {
        assertTrue(EdgePillRules.idleDotStandsIn(exiting = true, hideRunning = false))
        assertFalse(EdgePillRules.idleDotStandsIn(exiting = true, hideRunning = true))
        assertFalse(EdgePillRules.idleDotStandsIn(exiting = false, hideRunning = false))
    }

    @Test
    fun theBubbleMergeTimerStopsWhileHiding() {
        assertTrue(EdgePillRules.mergeTimerRuns(canMerge = true, hiding = false))
        assertFalse(EdgePillRules.mergeTimerRuns(canMerge = true, hiding = true))
        assertFalse(EdgePillRules.mergeTimerRuns(canMerge = false, hiding = false))
    }

    @Test
    fun glyphDragOffset() {
        // finger had dragged the glyphs 12 px up (negative); the card changes, swipe up gives sign +1
        assertEquals(17.28f, EdgePillRules.glyphSlidePx(true, 1, 28.8f, 0.6f, -12f, 0f), 0.001f)
        assertEquals(-17.28f, EdgePillRules.glyphSlidePx(true, -1, 28.8f, 0.6f, 12f, 0f), 0.001f)

        assertEquals(-12f, EdgePillRules.glyphSlidePx(false, 0, 28.8f, 0.6f, -12f, 0f), 0.001f)
        assertEquals(5f, EdgePillRules.glyphSlidePx(false, 0, 28.8f, 0.6f, 0f, 5f), 0.001f)
        // a swipe is waiting for its card but the card has not changed yet: still the finger's drag
        assertEquals(-12f, EdgePillRules.glyphSlidePx(false, 1, 28.8f, 0.6f, -12f, 0f), 0.001f)
    }

    @Test fun firstFrameIsDim() {
        assertEquals(1f, EdgePillRules.glyphEnterShare(true, 0f, 28.8f, 0.6f), 0.001f)
        assertEquals(0f, EdgePillRules.glyphEnterShare(false, 0f, 28.8f, 0.6f), 0.001f)
        assertEquals(0.5f, EdgePillRules.glyphEnterShare(false, 8.64f, 28.8f, 0.6f), 0.001f)
    }

    @Test fun holeBandStaysOnTheRealHoleWhileThePillLeansAndScales() {
        // resting pill: the band is where it is
        assertEquals(30f, EdgePillRules.holeLocalY(30f, 72f, 0f, 1f), 0.001f)
        // pill leaned 10 px down: the same screen spot is 10 px higher in the pill
        assertEquals(20f, EdgePillRules.holeLocalY(30f, 72f, 10f, 1f), 0.001f)
        // pill stretched 2x about its middle (36): a spot 12 px above the middle is 6 px above it in the pill
        assertEquals(30f, EdgePillRules.holeLocalY(24f, 72f, 0f, 2f), 0.001f)
    }

    // look-aware origin, anchored-side shift, counter-scale (both camera sides)


    private fun lookMotion(onLeft: Boolean, flat: Boolean, across: Float = 0f, pull: Float = 0f, widthPx: Float = 100f) =

        EdgePillRules.pillMotion(onLeft, across, 0f, 0f, 0f, pull, 1f, flat = flat, widthPx = widthPx)

    @Test fun flatLookScalesAboutEdge() {
        assertEquals(0f, EdgePillRules.originX(cameraOnLeft = true, flat = true), 0f)
        assertEquals(1f, EdgePillRules.originX(cameraOnLeft = false, flat = true), 0f)
        assertEquals(0.5f, EdgePillRules.originX(cameraOnLeft = true, flat = false), 0f)
        assertEquals(0.5f, EdgePillRules.originX(cameraOnLeft = false, flat = false), 0f)
        assertEquals(0.5f, lookMotion(true, flat = false).originX, 0f)
        assertEquals(1f, lookMotion(false, flat = true).originX, 0f)
    }

    @Test fun theAnchoredSideShiftIsHalfTheAddedWidthIntoTheScreen() {
        // 12 % wider than 100 px: 6 px
        assertEquals(6f, EdgePillRules.anchoredSideShiftPx(1.12f, 100f, cameraOnLeft = true), 1e-4f)
        assertEquals(-6f, EdgePillRules.anchoredSideShiftPx(1.12f, 100f, cameraOnLeft = false), 1e-4f)
        assertEquals(0f, EdgePillRules.anchoredSideShiftPx(1f, 100f, cameraOnLeft = true), 0f)
        assertEquals(0f, EdgePillRules.anchoredSideShiftPx(1f, 100f, cameraOnLeft = false), 0f)
    }

    @Test
    fun edgeLookTransforms() {
        val width = 100f
        // pill from 0..100 on a left camera: the scale about the middle, then the shift
        val left = lookMotion(onLeft = true, flat = false, pull = 1f, widthPx = width)
        val leftEdge = 50f - 50f * left.scaleX + left.translationX
        assertEquals(0f, leftEdge, 1e-3f)
        // right camera: the right edge stays
        val right = lookMotion(onLeft = false, flat = false, pull = 1f, widthPx = width)
        val rightEdge = 50f + 50f * right.scaleX + right.translationX
        assertEquals(100f, rightEdge, 1e-3f)

        assertEquals(0f, lookMotion(true, flat = true, pull = 1f).translationX, 0f)
        assertEquals(0f, lookMotion(false, flat = true, pull = 1f).translationX, 0f)
        // about the edge, the edge stays put by itself
        val m = lookMotion(true, flat = true, pull = 1f)
        assertEquals(0f, 0f * m.scaleX + m.translationX, 0f)

        assertEquals(6f + 4f, lookMotion(true, flat = false, across = 4f, pull = 1f).translationX, 1e-4f)
        assertEquals(-(6f + 4f), lookMotion(false, flat = false, across = 4f, pull = 1f).translationX, 1e-4f)

        assertEquals(0f, lookMotion(true, flat = false, pull = 0f).translationX, 0f)
        assertEquals(0f, lookMotion(true, flat = false, pull = 1f, widthPx = 0f).translationX, 0f)
        // the old call shape keeps the flat behaviour
        assertEquals(0f, motion(onLeft = true, pull = 1f).translationX, 0f)

        val l = lookMotion(true, flat = false, pull = 0.7f)
        val r = lookMotion(false, flat = false, pull = 0.7f)
        assertEquals(l.scaleX, r.scaleX, 0f)
        assertEquals(l.scaleY, r.scaleY, 0f)
        assertEquals(l.translationX, -r.translationX, 1e-5f)
    }

    @Test
    fun glyphScaleCompensation() {
        assertEquals(1f, EdgePillRules.counterScale(1f), 0f)
        assertEquals(0.5f, EdgePillRules.counterScale(2f), 0f)
        val inverse = lookMotion(true, flat = false, pull = 1f)
        assertEquals(1f, EdgePillRules.counterScale(inverse.scaleX) * inverse.scaleX, 1e-5f)
        assertEquals(1f, EdgePillRules.counterScale(inverse.scaleY) * inverse.scaleY, 1e-5f)
        // pulse, cue and pull together
        val busy = EdgePillRules.pillMotion(false, 0f, 0f, 1f, 0f, 1f, 1.125f)
        assertEquals(1f, EdgePillRules.counterScale(busy.scaleY) * busy.scaleY, 1e-5f)

        val resting = EdgePillRules.pillMotion(true, 0f, 0f, 0f, 0f, 0f, 1f)
        assertEquals(1f, resting.cue, 1e-5f)
        assertEquals(1f, EdgePillRules.glyphScale(resting.scaleX, resting.cue) * resting.scaleX, 1e-5f)

        val armed = EdgePillRules.pillMotion(true, 0f, 0f, 1f, 0f, 0f, 1f)
        assertEquals(1f + EdgePillRules.ARM_SWELL, armed.cue, 1e-5f)
        assertEquals(armed.cue, EdgePillRules.glyphScale(armed.scaleX, armed.cue) * armed.scaleX, 1e-5f)
        assertEquals(armed.cue, EdgePillRules.glyphScale(armed.scaleY, armed.cue) * armed.scaleY, 1e-5f)

        val pulsing = EdgePillRules.pillMotion(true, 0f, 0f, 1f, 0f, 0f, 1.1f)
        assertEquals(pulsing.cue, EdgePillRules.glyphScale(pulsing.scaleY, pulsing.cue) * pulsing.scaleY, 1e-5f)

        val deadZone = EdgePillRules.pillMotion(true, 0f, 0f, 0f, 1f, 0f, 1f)
        assertEquals(1f - EdgePillRules.DEAD_SHRINK, deadZone.cue, 1e-5f)
    }

    @Test fun theCounterScaleNeverDividesByZero() {
        assertEquals(100f, EdgePillRules.counterScale(0f), 1e-3f)
        assertEquals(100f, EdgePillRules.counterScale(-1f), 1e-3f)
    }

    @Test
    fun cameraLocalCoordinates() {
        assertEquals(30f, EdgePillRules.holeLocalX(30f, 80f, 0f, 0f, 1f), 1e-3f)
        assertEquals(70f, EdgePillRules.holeLocalY(70f, 200f, 0f, 1f), 1e-3f)

        // origin at the left edge, scaled 2x: a screen x of 40 is 20 inside the pill's own coordinates
        assertEquals(20f, EdgePillRules.holeLocalX(40f, 80f, 0f, 0f, 2f), 1e-3f)
        // a lean of 10 px moves the pill, so the hole is 10 px less inside it
        assertEquals(20f, EdgePillRules.holeLocalX(30f, 80f, 0f, 10f, 1f), 1e-3f)
    }

    @Test fun clearWidthFallsBackToHole() {
        assertEquals(12f, EdgePillRules.holeClearWidthDp(12f, 20f), 1e-3f)
        assertEquals(20f, EdgePillRules.holeClearWidthDp(0f, 20f), 1e-3f)
    }

    @Test
    fun sideCameraCapsHorizontalFollow() {
        assertEquals(-30f, EdgePillRules.followPx(-30f, null), 0.001f)
        assertEquals(8f, EdgePillRules.followPx(30f, 8f), 0.001f)
        assertEquals(-8f, EdgePillRules.followPx(-30f, 8f), 0.001f)
        assertEquals(3f, EdgePillRules.followPx(3f, 8f), 0.001f)
        assertEquals(0f, EdgePillRules.followPx(30f, 0f), 0.001f)
    }

}
