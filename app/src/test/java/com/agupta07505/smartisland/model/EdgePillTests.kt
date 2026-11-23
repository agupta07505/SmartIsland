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

class EdgePillRulesTest {
    @Test fun zonesAreCameraMiddleAndGlyphsOnly() {
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneAt(90f, 90f))
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneAt(104f, 90f))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneAt(105f, 90f))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneAt(60f, 90f))
    }

    @Test
    fun edgeTapTargets() {
        assertEquals(EdgePillAction.ToggleCard, EdgePillRules.tap(EdgePillZone.Glyph, false))
        assertEquals(EdgePillAction.PutCardsAway, EdgePillRules.tap(EdgePillZone.Middle, false))
        assertEquals(EdgePillAction.BringCardsBack, EdgePillRules.tap(EdgePillZone.Glyph, true))
        assertEquals(EdgePillAction.BringCardsBack, EdgePillRules.tap(EdgePillZone.Middle, true))

        assertEquals(EdgePillAction.SelectOther, EdgePillRules.tap(EdgePillZone.Glyph, cardsAway = false, cardOpen = true, otherPillShown = true))
        // no other pill: a glyph tap closes the open card as before
        assertEquals(EdgePillAction.ToggleCard, EdgePillRules.tap(EdgePillZone.Glyph, cardsAway = false, cardOpen = true, otherPillShown = false))
        // a closed island opens, whatever the plan says
        assertEquals(EdgePillAction.ToggleCard, EdgePillRules.tap(EdgePillZone.Glyph, cardsAway = false, cardOpen = false, otherPillShown = true))
        // the camera middle still puts the cards away, and away cards still come back
        assertEquals(EdgePillAction.PutCardsAway, EdgePillRules.tap(EdgePillZone.Middle, cardsAway = false, cardOpen = true, otherPillShown = true))
        assertEquals(EdgePillAction.BringCardsBack, EdgePillRules.tap(EdgePillZone.Glyph, cardsAway = true, cardOpen = true, otherPillShown = true))
    }

    @Test fun holdRules() {
        assertEquals(EdgePillAction.NormalLongPress, EdgePillRules.hold(EdgePillZone.Middle))
        assertEquals(EdgePillAction.AppsShortcut, EdgePillRules.hold(EdgePillZone.Glyph))
    }

    @Test fun glyphHoldNeedsCard() {
        assertTrue(EdgePillRules.glyphHold(EdgePillZone.Glyph, cardOnPill = true, cardOpen = false,
            cardsAway = false, widgetIsland = false))
        assertFalse(EdgePillRules.glyphHold(EdgePillZone.Middle, true, false, false, false))
        assertFalse(EdgePillRules.glyphHold(EdgePillZone.Glyph, false, false, false, false))
        assertFalse(EdgePillRules.glyphHold(EdgePillZone.Glyph, true, true, false, false))
        assertFalse(EdgePillRules.glyphHold(EdgePillZone.Glyph, true, false, true, false))
        assertFalse(EdgePillRules.glyphHold(EdgePillZone.Glyph, true, false, false, true))
    }

    @Test fun aGlyphSwipeIsADragAlongTheEdgeEitherWayPastTheSlop() {
        assertTrue(EdgePillRules.glyphSwiped(-30f, 2f, 10f))
        assertTrue(EdgePillRules.glyphSwiped(30f, -2f, 10f))
        assertFalse(EdgePillRules.glyphSwiped(8f, 0f, 10f))
        assertFalse(EdgePillRules.glyphSwiped(12f, 40f, 10f))
        assertFalse(EdgePillRules.glyphSwiped(0f, 40f, 10f))
    }

    @Test fun edgeGlyphSwipeLiftsLikePortrait() {
        assertEquals(GlyphLift.Repulse, HoldGesture.glyphLift(true, true, true))
        assertEquals(GlyphLift.OpenApps, HoldGesture.glyphLift(false, true, false))
        assertEquals(GlyphLift.Nothing, HoldGesture.glyphLift(false, true, true))
    }

    @Test
    fun edgeSwipeActions() {
        assertEquals(EdgePillAction.OpenCard, liftAction(30f, 0f, cameraOnLeft = true, cardOpen = false, cardsAway = false))
        assertEquals(EdgePillAction.OpenCard, liftAction(-30f, 0f, cameraOnLeft = false, cardOpen = false, cardsAway = false))

        assertEquals(EdgePillAction.None, liftAction(30f, 0f, cameraOnLeft = true, cardOpen = true, cardsAway = false))
        assertEquals(EdgePillAction.None, liftAction(-30f, 0f, cameraOnLeft = false, cardOpen = true, cardsAway = false))

        assertEquals(EdgePillAction.CloseCard, liftAction(-30f, 0f, true, cardOpen = true, cardsAway = false))
        assertEquals(EdgePillAction.PillSwipeAction, liftAction(-30f, 0f, true, cardOpen = false, cardsAway = false))
        assertEquals(EdgePillAction.CloseCard, liftAction(30f, 0f, false, cardOpen = true, cardsAway = false))

        assertEquals(EdgePillAction.SwitchCard, liftAction(4f, 40f, true, false, false))
        assertEquals(EdgePillAction.SwitchCard, liftAction(-4f, -40f, false, true, false))

        // swiped up: the old glyphs left upward, the new ones come from the bottom (+)
        assertEquals(1, EdgePillRules.enterSign(-30f))
        assertEquals(-1, EdgePillRules.enterSign(30f))
        // same sign as the classic pill's sideways rule (left = +1)
        val dragX = -30f
        assertEquals(if (dragX < 0f) 1 else -1, EdgePillRules.enterSign(-30f))
        // the drag that fires a card switch is the one that sets a sign
        assertEquals(EdgePillAction.SwitchCard, liftAction(2f, -30f, cameraOnLeft = false, cardOpen = false, cardsAway = false))
    }

    @Test
    fun edgeReleaseThresholds() {
        assertEquals(EdgePillAction.None, liftAction(5f, 5f, true, false, false))
        assertEquals(EdgePillAction.None, liftAction(40f, 0f, true, false, cardsAway = true))

        for (sens in GestureSensitivity.entries) {
            val t = 16f * sens.distanceScale
            assertEquals(EdgePillAction.None, liftAction(0f, t - 1f, true, false, false, sens))
            assertEquals(EdgePillAction.None, liftAction(t - 1f, 0f, true, false, false, sens))
            assertEquals(EdgePillAction.SwitchCard, liftAction(0f, t + 1f, true, false, false, sens))
        }

        // along 30, across 10: 30 > 10 x 1.6, clear
        assertEquals(EdgePillAction.SwitchCard, liftAction(10f, 30f, true, false, false, GestureSensitivity.LOW))
        assertEquals(EdgePillAction.None, liftAction(20f, 30f, true, false, false, GestureSensitivity.LOW))
        // normal and high only need the longer axis to win
        assertEquals(EdgePillAction.SwitchCard, liftAction(20f, 30f, true, false, false, GestureSensitivity.NORMAL))

        // the drag loop is handed the event that crossed the tap slop (and the lift)
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 60f, true))
        assertEquals(EdgePillAction.SwitchCard, liftAction(0f, 60f, true, false, false))

        val drags = listOf(0f to 40f, 0f to -40f, 40f to 0f, -40f to 0f, 3f to 5f, 25f to 25f)
        for ((dx, dy) in drags) {
            for (left in listOf(true, false)) for (open in listOf(false, true)) for (away in listOf(false, true)) for (on in listOf(true, false)) {
                val dir = armedAfter(dx, dy, left)
                val fires = liftAction(dx, dy, left, open, away, swipesOn = on) != EdgePillAction.None
                assertEquals(if (fires) TouchZone.Armed else TouchZone.Dead, EdgePillRules.liftZone(dir, open, away, on))
            }
        }
    }

    @Test fun intoScreenFollowsTheCameraSide() {
        assertEquals(30f, EdgePillRules.intoScreenDp(30f, cameraOnLeft = true), 0.001f)
        assertEquals(30f, EdgePillRules.intoScreenDp(-30f, cameraOnLeft = false), 0.001f)
        assertEquals(-30f, EdgePillRules.intoScreenDp(30f, cameraOnLeft = false), 0.001f)
    }

    @Test fun dragInOpensUnderFinger() {
        assertTrue(EdgePillRules.opensUnderFinger(20f, 4f, cameraOnLeft = true))
        assertTrue(EdgePillRules.opensUnderFinger(-20f, 4f, cameraOnLeft = false))
        // toward the edge, along the edge, or too short: not an opening drag
        assertFalse(EdgePillRules.opensUnderFinger(-20f, 4f, cameraOnLeft = true))
        assertFalse(EdgePillRules.opensUnderFinger(8f, 2f, cameraOnLeft = true))
        assertFalse(EdgePillRules.opensUnderFinger(14f, 30f, cameraOnLeft = true))
    }

    @Test
    fun gestureSlop() {
        assertEquals(10f, EdgePillRules.tapSlopDp(GestureSensitivity.NORMAL), 0.001f)
        for (sens in GestureSensitivity.entries) {
            assertEquals(10f * sens.distanceScale, EdgePillRules.tapSlopDp(sens), 0.001f)
        }

        for (sens in GestureSensitivity.entries) {
            val slop = EdgePillRules.tapSlopDp(sens)
            assertFalse(EdgePillRules.opensUnderFinger(slop - 0.5f, 0f, true, slop))
            assertTrue(EdgePillRules.opensUnderFinger(slop + 0.5f, 0f, true, slop))
        }
    }

    @Test fun pillClearanceWaitsForTheCardToLeaveTheSpot() {
        // camera on the left: pill 0..34, card resting 8 dp from it
        fun left(cardLeft: Float) = EdgePillRules.pillClearance(true, cardLeft, cardLeft + 170f, 0f, 34f, 8f)
        assertEquals(0f, left(0f), 0.001f)
        assertEquals(0f, left(30f), 0.001f)
        assertEquals(1f, left(42f), 0.001f)
        assertEquals(1f, left(60f), 0.001f)
        assertTrue(left(38f) > 0f && left(38f) < 1f)
        // camera on the right: the mirror image
        fun right(cardRight: Float) = EdgePillRules.pillClearance(false, cardRight - 170f, cardRight, 306f, 340f, 8f)
        assertEquals(0f, right(340f), 0.001f)
        assertEquals(1f, right(298f), 0.001f)
        // a 0 gap still clears once the card is off the spot
        assertEquals(1f, EdgePillRules.pillClearance(true, 34f, 204f, 0f, 34f, 0f), 0.001f)
    }

    @Test fun glyphSlideRangeFortyPercent() {
        assertEquals(28.8f, EdgePillRules.glyphMaxSlideDp(72f), 0.001f)
        assertEquals(8f, EdgePillRules.glyphMaxSlideDp(10f), 0.001f)
        assertEquals(56f, EdgePillRules.glyphMaxSlideDp(400f), 0.001f)
    }

    @Test fun leanIsADampedCopyOfTheDragWithinItsLimits() {
        // density 1: 24 dp along the edge, 6 dp across it, 35 % of the drag to start with
        assertEquals(0f, EdgePillRules.leanAlongPx(0f, 1f), 0.001f)
        assertEquals(3.5f, EdgePillRules.leanAlongPx(10f, 1f), 0.2f)
        assertTrue(EdgePillRules.leanAlongPx(500f, 1f) <= 24f)
        assertTrue(EdgePillRules.leanAlongPx(500f, 1f) > 23f)
        assertTrue(EdgePillRules.leanAcrossPx(500f, 1f) <= 6f)
        // signed like the drag: up leans up, toward the edge leans toward it
        assertEquals(-EdgePillRules.leanAlongPx(40f, 1f), EdgePillRules.leanAlongPx(-40f, 1f), 0.001f)
        assertTrue(EdgePillRules.leanAcrossPx(-30f, 1f) < 0f)
        // the limits scale with the screen's density
        assertEquals(2f * EdgePillRules.leanAlongPx(40f, 1f), EdgePillRules.leanAlongPx(80f, 2f), 0.001f)
    }

    @Test fun incomingGlyphsStartPartWayOut() {
        assertEquals(17.28f, EdgePillRules.enterStartPx(1, 28.8f, 0.6f), 0.001f)
        assertEquals(-17.28f, EdgePillRules.enterStartPx(-1, 28.8f, 0.6f), 0.001f)
        assertEquals(0f, EdgePillRules.enterStartPx(0, 28.8f, 0.6f), 0.001f)
    }

    @Test fun enterShareOneToZero() {
        assertEquals(1f, EdgePillRules.enterShare(17.28f, 28.8f, 0.6f), 0.001f)
        assertEquals(1f, EdgePillRules.enterShare(-17.28f, 28.8f, 0.6f), 0.001f)
        assertEquals(0.5f, EdgePillRules.enterShare(8.64f, 28.8f, 0.6f), 0.001f)
        assertEquals(0f, EdgePillRules.enterShare(0f, 28.8f, 0.6f), 0.001f)
        // past the start (a spring overshoot) stays at 1
        assertEquals(1f, EdgePillRules.enterShare(40f, 28.8f, 0.6f), 0.001f)
        assertEquals(0f, EdgePillRules.enterShare(10f, 0f, 0.6f), 0.001f)
    }

    private fun armedAfter(
        dx: Float, dy: Float, cameraOnLeft: Boolean,
        sensitivity: GestureSensitivity = GestureSensitivity.NORMAL,
        via: List<Pair<Float, Float>> = emptyList()
    ): PillSwipeDirection? {
        val arming = SwipeArming(sensitivity, sensitivity.scaled(16f)).also { it.lockWhen = { false } }
        var t = 0L
        for ((x, y) in via + (dx to dy)) {
            arming.update(EdgePillRules.armAlong(y), EdgePillRules.armInto(x, cameraOnLeft), t)
            t += 16
        }
        return arming.direction
    }

    @Test
    fun edgePullDirection() {
        // along the edge feeds classic's sideways axis unchanged
        assertEquals(-30f, EdgePillRules.armAlong(-30f), 0.001f)
        assertEquals(30f, EdgePillRules.armInto(30f, cameraOnLeft = true), 0.001f)
        assertEquals(30f, EdgePillRules.armInto(-30f, cameraOnLeft = false), 0.001f)
        assertEquals(-30f, EdgePillRules.armInto(-30f, cameraOnLeft = true), 0.001f)

        // the same maths as the classic pill, fed the drag into the screen
        assertEquals(0f, SwipeDownPull.progress(EdgePillRules.armInto(5f, true), 10f, 16f), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(EdgePillRules.armInto(16f, true), 10f, 16f), 0.001f)
        assertEquals(1f, SwipeDownPull.progress(EdgePillRules.armInto(-16f, false), 10f, 16f), 0.001f)
        // toward the edge (negative) never pulls
        assertEquals(0f, SwipeDownPull.progress(EdgePillRules.armInto(-30f, true), 10f, 16f), 0.001f)
    }

    @Test
    fun swipeArmingThresholds() {
        // 16 dp at normal sensitivity
        assertNull(armedAfter(0f, 15f, true))
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 17f, true))
        assertEquals(PillSwipeDirection.Left, armedAfter(0f, -17f, false))

        // high: 11.2 dp, low: 23.2 dp (and low also wants the drag clearly one-axis)
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 12f, true, GestureSensitivity.HIGH))
        assertNull(armedAfter(0f, 12f, true, GestureSensitivity.NORMAL))
        assertNull(armedAfter(0f, 22f, true, GestureSensitivity.LOW))
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 24f, true, GestureSensitivity.LOW))

        assertEquals(PillSwipeDirection.Down, armedAfter(20f, 0f, cameraOnLeft = true))
        assertEquals(PillSwipeDirection.Down, armedAfter(-20f, 0f, cameraOnLeft = false))
        assertEquals(PillSwipeDirection.Up, armedAfter(-20f, 0f, cameraOnLeft = true))
        assertEquals(PillSwipeDirection.Up, armedAfter(20f, 0f, cameraOnLeft = false))

        assertNull(armedAfter(30f, 34f, true, GestureSensitivity.LOW))

        // up past the threshold, then all the way down
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 30f, true, via = listOf(0f to -30f)))
    }

    @Test fun aSwipePulledBackCancels() {
        // armed at 20 dp, then back to 10 dp (under 75 % of 16 dp = 12)
        assertNull(armedAfter(0f, 10f, true, via = listOf(0f to 20f)))
        // back to only 14 dp (still over 12): stays armed, like the classic pill
        assertEquals(PillSwipeDirection.Right, armedAfter(0f, 14f, true, via = listOf(0f to 20f)))
    }

    @Test
    fun armedSwipeDispatch() {
        assertEquals(EdgePillAction.SwitchCard, EdgePillRules.armedAction(PillSwipeDirection.Left, false, false))
        assertEquals(EdgePillAction.SwitchCard, EdgePillRules.armedAction(PillSwipeDirection.Right, true, false))
        assertEquals(EdgePillAction.OpenCard, EdgePillRules.armedAction(PillSwipeDirection.Down, false, false))
        assertEquals(EdgePillAction.None, EdgePillRules.armedAction(PillSwipeDirection.Down, true, false))
        assertEquals(EdgePillAction.PillSwipeAction, EdgePillRules.armedAction(PillSwipeDirection.Up, false, false))
        assertEquals(EdgePillAction.CloseCard, EdgePillRules.armedAction(PillSwipeDirection.Up, true, false))

        assertEquals(EdgePillAction.None, EdgePillRules.armedAction(null, false, false))
        for (d in PillSwipeDirection.entries) {
            assertEquals(EdgePillAction.None, EdgePillRules.armedAction(d, false, cardsAway = true))
        }

        for (d in listOf<PillSwipeDirection?>(null) + PillSwipeDirection.entries) {
            for (open in listOf(false, true)) {
                for (away in listOf(false, true)) {
                    val fires = EdgePillRules.armedAction(d, open, away) != EdgePillAction.None
                    assertEquals(
                        if (fires) TouchZone.Armed else TouchZone.Dead,
                        EdgePillRules.liftZone(d, open, away)
                    )
                }
            }
        }

        for (d in PillSwipeDirection.entries) {
            for (open in listOf(false, true)) {
                assertEquals(EdgePillAction.None, EdgePillRules.armedAction(d, open, false, swipesOn = false))
                assertEquals(TouchZone.Dead, EdgePillRules.liftZone(d, open, false, swipesOn = false))
            }
        }
        // swipes on (the default) still switch the card
        assertEquals(EdgePillAction.SwitchCard, EdgePillRules.armedAction(PillSwipeDirection.Left, false, false))

        // armed toward the edge on either camera side (portrait's swipe up)
        assertEquals(PillSwipeDirection.Up, armedAfter(-20f, 0f, cameraOnLeft = true))
        assertEquals(PillSwipeDirection.Up, armedAfter(20f, 0f, cameraOnLeft = false))
        assertEquals(EdgePillAction.PillSwipeAction, EdgePillRules.armedAction(PillSwipeDirection.Up, cardOpen = false, cardsAway = false))
        assertEquals(EdgePillAction.CloseCard, EdgePillRules.armedAction(PillSwipeDirection.Up, cardOpen = true, cardsAway = false))
    }

    @Test fun clearSwipesWorkOnEitherSide() {
        for (cardOpen in listOf(false, true)) {
            for (left in listOf(true, false)) {
                val into = if (left) 40f else -40f
                val cases = listOf(
                    Triple(0f, 40f, PillSwipeDirection.Right to EdgePillAction.SwitchCard),
                    Triple(0f, -40f, PillSwipeDirection.Left to EdgePillAction.SwitchCard),
                    Triple(into, 0f, PillSwipeDirection.Down to if (cardOpen) EdgePillAction.None else EdgePillAction.OpenCard),
                    Triple(-into, 0f, PillSwipeDirection.Up to if (cardOpen) EdgePillAction.CloseCard else EdgePillAction.PillSwipeAction)
                )
                for ((dx, dy, expected) in cases) {
                    assertEquals(expected.first, armedAfter(dx, dy, left))
                    assertEquals(expected.second, liftAction(dx, dy, left, cardOpen, false))
                }
            }
        }
    }

    @Test
    fun swipeReleaseZones() {
        assertEquals(TouchZone.Armed, EdgePillRules.liftZone(PillSwipeDirection.Right, cardOpen = false, cardsAway = false))
        assertEquals(TouchZone.Armed, EdgePillRules.liftZone(PillSwipeDirection.Down, cardOpen = false, cardsAway = false))
        assertEquals(TouchZone.Armed, EdgePillRules.liftZone(PillSwipeDirection.Up, cardOpen = true, cardsAway = false))

        // not armed: short, pulled back or too diagonal
        assertEquals(TouchZone.Dead, EdgePillRules.liftZone(null, cardOpen = false, cardsAway = false))
        // armed direction but no action
        assertEquals(TouchZone.Dead, EdgePillRules.liftZone(PillSwipeDirection.Down, cardOpen = true, cardsAway = false))
        assertEquals(TouchZone.Dead, EdgePillRules.liftZone(PillSwipeDirection.Left, cardOpen = false, cardsAway = true))
    }

    @Test fun cueScaleSwellsWhenArmed() {
        assertEquals(1f, EdgePillRules.cueScale(0f, 0f), 0.0001f)
        assertEquals(1.04f, EdgePillRules.cueScale(1f, 0f), 0.0001f)
        assertEquals(0.96f, EdgePillRules.cueScale(0f, 1f), 0.0001f)
        // halfway through the spring: halfway between rest and the cue
        assertEquals(1.02f, EdgePillRules.cueScale(0.5f, 0f), 0.0001f)
        // the classic pill's numbers: 4 % swell, 4 % shrink
        assertEquals(0.04f, EdgePillRules.ARM_SWELL, 0f)
        assertEquals(0.04f, EdgePillRules.DEAD_SHRINK, 0f)
    }

    @Test fun swipesFollowTheClassicSettings() {
        // card closed: "pill swipe actions"
        assertTrue(EdgePillRules.swipesOn(cardOpen = false, pillSwipes = true, swipeActions = false))
        assertFalse(EdgePillRules.swipesOn(cardOpen = false, pillSwipes = false, swipeActions = true))
        assertTrue(EdgePillRules.swipesOn(cardOpen = true, pillSwipes = false, swipeActions = true))
        assertFalse(EdgePillRules.swipesOn(cardOpen = true, pillSwipes = true, swipeActions = false))
    }

    @Test fun pullStretchesOnlyWhereTheDragCannotOpenTheIsland() {
        // into the screen, swipes on, card closed, nothing to follow the finger
        assertTrue(EdgePillRules.pullAllowed(true, cardOpen = false, cardsAway = false, openingPossible = false, intoDominant = true))
        // the island opens under the finger instead: no pull
        assertFalse(EdgePillRules.pullAllowed(true, cardOpen = false, cardsAway = false, openingPossible = true, intoDominant = true))
        // card open, cards put away, swipes off, or a drag along the edge: no pull
        assertFalse(EdgePillRules.pullAllowed(true, cardOpen = true, cardsAway = false, openingPossible = false, intoDominant = true))
        assertFalse(EdgePillRules.pullAllowed(true, cardOpen = false, cardsAway = true, openingPossible = false, intoDominant = true))
        assertFalse(EdgePillRules.pullAllowed(false, cardOpen = false, cardsAway = false, openingPossible = false, intoDominant = true))
        assertFalse(EdgePillRules.pullAllowed(true, cardOpen = false, cardsAway = false, openingPossible = false, intoDominant = false))
    }

    @Test fun movementPastTheTapSlopIsNeverATap() {
        val slop = EdgePillRules.TAP_SLOP_DP
        val guard = TapGuard(slop)
        guard.update(0f, slop - 1f)
        assertFalse(guard.exceeded)
        // a flick whose first (and last) event is already past the slop is not a tap
        val flick = TapGuard(slop)
        flick.update(0f, 40f)
        assertTrue(flick.exceeded)
        // past the slop and back: still not a tap (the peak counts)
        flick.update(0f, 1f)
        assertTrue(flick.exceeded)
    }

    /** Finished drag (dp, density 1) as the edge layer reads it */
    private fun liftAction(
        dx: Float, dy: Float, cameraOnLeft: Boolean, cardOpen: Boolean, cardsAway: Boolean,
        sensitivity: GestureSensitivity = GestureSensitivity.NORMAL, swipesOn: Boolean = true
    ): EdgePillAction =

        EdgePillRules.armedAction(armedAfter(dx, dy, cameraOnLeft, sensitivity), cardOpen, cardsAway, swipesOn)

    @Test fun hiddenPillKeepsAHoldTargetOnTheCameraMiddle() {
        assertTrue(EdgePillRules.hiddenTargetActive(edgeStyle = true, pillHidden = true, visibleCount = 0))
        assertFalse(EdgePillRules.hiddenTargetActive(edgeStyle = true, pillHidden = false, visibleCount = 0))
        assertFalse(EdgePillRules.hiddenTargetActive(edgeStyle = true, pillHidden = true, visibleCount = 1))
        assertFalse(EdgePillRules.hiddenTargetActive(edgeStyle = false, pillHidden = true, visibleCount = 0))
        val target = EdgePillRules.hiddenTarget(EdgeRect(left = 0f, top = 100f, width = 24f, height = 150f))
        assertEquals(40f, target.width, 0.001f)
        assertEquals(EdgePillRules.HIDDEN_TARGET_HEIGHT_DP, target.height, 0.001f)
        // centred on the pill's middle (y 175), never off the screen's left edge
        assertEquals(175f, target.top + target.height / 2f, 0.001f)
        assertEquals(0f, target.left, 0.001f)
    }

    @Test fun cameraBandTapGuardEndsATapAfterAFewDpSideways() {
        val slop = 30f
        val sideways = 9f
        // the normal guard still counts a small sideways move as a tap
        val normal = TapGuard(slop)
        normal.update(sideways + 1f, 0f)
        assertFalse(normal.exceeded)
        // on the camera band the same move is a swipe attempt
        val band = TapGuard(slop, sideways)
        band.update(sideways - 1f, 0f)
        assertFalse(band.exceeded)
        band.update(sideways + 1f, 0f)
        assertTrue(band.exceeded)
        // moving back does not make it a tap again (the peak counts)
        band.update(0f, 0f)
        assertTrue(band.exceeded)
        val vertical = TapGuard(slop, sideways)
        vertical.update(0f, slop - 1f)
        assertFalse(vertical.exceeded)
    }

    @Test
    fun autoHiddenIdleOnlyWhenAutoHideIsOnHiddenAndNoCard() {
        assertTrue(EdgePillRules.autoHiddenIdle(autoHidePill = true, isAutoHidden = true, hasCards = false))
        assertFalse(EdgePillRules.autoHiddenIdle(autoHidePill = false, isAutoHidden = true, hasCards = false))
        assertFalse(EdgePillRules.autoHiddenIdle(autoHidePill = true, isAutoHidden = false, hasCards = false))
        assertFalse(EdgePillRules.autoHiddenIdle(autoHidePill = true, isAutoHidden = true, hasCards = true))
    }
}
