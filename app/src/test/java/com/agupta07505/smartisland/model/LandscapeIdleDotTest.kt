/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeIdleDotTest {

    private val settings = SmartIslandSettings(landscapeCircleSize = 26f)
    private val cameraState = OrientationState(
        isLandscape = true,
        screenWidthDp = 800f,
        screenHeightDp = 360f,
        camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 20f)
    )

    @Test
    fun portraitHasNoSeparateIdlePill() {
        // The pill's own glyph-free centre is the control, see PortraitPillTapTest.
        val portrait = SmartIslandSettings(width = 120f, height = 34f, xOffset = 12f, yOffset = 9f, opacity = 0.9f)
        assertNull(idleDot(portrait, OrientationState(isLandscape = false)))
    }

    @Test
    fun noPortraitDotInNotchMode() {
        assertNull(idleDot(SmartIslandSettings(enableNotchMode = true), OrientationState(isLandscape = false)))
    }

    @Test
    fun landscapeDotIsRoundWhateverTheWidth() {
        val dot = idleDot(settings, cameraState)!!
        assertEquals(dot.sizeDp, dot.widthDp, 0.001f)
    }

    @Test
    fun portraitNeverDrawsTheDot() {
        val dot = IdleDotGeometry(centerXDp = 0f, topDp = 0f, sizeDp = 34f, widthDp = 120f)
        assertFalse(shouldShowIdleDot(dot, hasCards = true, isLandscape = false))
        assertFalse(shouldShowIdleDot(dot, hasCards = false, isLandscape = false))
    }

    @Test
    fun landscapeDotIsThereCollapsedOrExpanded() {
        assertTrue(shouldShowIdleDot(dot, hasCards = true, isLandscape = true))
    }

    @Test
    fun dotSitsOnTheReportedCameraCutout() {
        val dot = idleDot(settings, cameraState)
        assertNotNull(dot)
        assertEquals(-350f, dot!!.centerXDp, 0.01f)
        assertEquals(7f, dot.topDp, 0.01f)
        assertEquals(26f, dot.sizeDp, 0.01f)
        assertEquals(33f, dot.bottomDp, 0.01f)
    }

    @Test
    fun dotMatchesTheIdleCutoutOfForOrientation() {
        val idle = settings.forOrientation(cameraState, hasContent = false)
        val dot = idleDot(settings, cameraState)!!
        assertEquals(idle.xOffset, dot.centerXDp, 0.001f)
        assertEquals(idle.yOffset, dot.topDp, 0.001f)
        assertEquals(idle.opacity, dot.opacity, 0.001f)
    }

    private val dot = IdleDotGeometry(centerXDp = -350f, topDp = 7f, sizeDp = 26f)

    @Test
    fun shownWhenACardIsCollapsedAndTheCutoutIsFarAway() {
        // Pill group in the middle: 340..460 dp from the left edge, 40 dp tall.
        assertTrue(shouldShowIdleDot(dot, true))
    }

    @Test
    fun hiddenWhenIdleBecauseTheCutoutItselfIsTheDot() {
        assertFalse(shouldShowIdleDot(dot, false))
    }

    @Test
    fun shownWhileExpandedToo() {
        // Same rule expanded or collapsed: with a card on the island the dot is there to close it.
        assertTrue(shouldShowIdleDot(dot, true))
    }

    @Test
    fun hiddenWhenNoGeometry() {
        assertFalse(shouldShowIdleDot(null, true))
    }

    @Test
    fun stillShownWhenItSitsOnTheCollapsedPill() {
        // Permanent: it is drawn above the pill and stays pressable wherever the pill is.
        assertTrue(shouldShowIdleDot(dot, true))
    }

    @Test
    fun tapPutsCardsAwayWhenThereAreCards() {
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(hasCards = true, hasIdleStoredCards = false))
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(hasCards = true, hasIdleStoredCards = true))
    }

    @Test
    fun tapBringsStoredCardsBackWhenIdle() {
        assertEquals(IdleDotTap.RevealStored, idleDotTapAction(hasCards = false, hasIdleStoredCards = true))
    }

    @Test
    fun tapOnAnEmptyIdleDotDoesNothingButIsHandled() {
        assertEquals(IdleDotTap.None, idleDotTapAction(hasCards = false, hasIdleStoredCards = false))
    }

    @Test
    fun tapOnAnEmptyLandscapeCutoutShowsTheEmptyPill() {
        assertEquals(
            IdleDotTap.ShowEmptyPill,
            idleDotTapAction(hasCards = false, hasIdleStoredCards = false, emptyPillAvailable = true)
        )
    }

    @Test
    fun tapOnTheShownEmptyPillHidesIt() {
        assertEquals(
            IdleDotTap.HideEmptyPill,
            idleDotTapAction(hasCards = false, hasIdleStoredCards = false, emptyPillAvailable = true, emptyPillShown = true)
        )
    }

    @Test
    fun cardsAndStoredCardsKeepTheirTapOverTheEmptyPill() {
        assertEquals(IdleDotTap.PutAwayNow, idleDotTapAction(true, false, emptyPillAvailable = true, emptyPillShown = true))
        assertEquals(IdleDotTap.RevealStored, idleDotTapAction(false, true, emptyPillAvailable = true))
    }
}
