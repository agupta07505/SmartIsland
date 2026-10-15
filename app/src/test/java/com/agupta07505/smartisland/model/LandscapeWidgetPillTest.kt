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

class LandscapeWidgetPillTest {

    private fun wanted(
        landscape: Boolean = true,
        enabled: Boolean = true,
        ids: String = "clock",
        pillHidden: Boolean = false,
        shadeOpen: Boolean = false,
        suppressed: Boolean = false,
        tickHold: Boolean = false,
        emptyPill: Boolean = false
    ) = landscapeWidgetPillWanted(landscape, enabled, ids, pillHidden, shadeOpen, suppressed, tickHold, emptyPill)

    @Test
    fun theEmptyPillNeedsNoWidgetsButStillLandscapeAndNoShade() {
        assertTrue(wanted(enabled = false, ids = "", emptyPill = true))
        assertTrue(wanted(pillHidden = true, emptyPill = true))
        assertFalse(wanted(landscape = false, emptyPill = true))
        assertFalse(wanted(shadeOpen = true, emptyPill = true))
    }

    @Test
    fun aTickBringsThePillInWhileItIsHiddenOrTheWidgetsAreHeldOff() {
        assertFalse(wanted(pillHidden = true))
        assertTrue(wanted(pillHidden = true, tickHold = true))
        assertTrue(wanted(suppressed = true, tickHold = true))
    }

    @Test
    fun aTickNeverBringsThePillInWhereTicksCannotPlay() {
        assertFalse(wanted(landscape = false, tickHold = true))
        assertFalse(wanted(enabled = false, pillHidden = true, tickHold = true))
        assertFalse(wanted(shadeOpen = true, pillHidden = true, tickHold = true))
    }

    @Test
    fun landscapeWithWidgetsOnWantsThePill() {
        assertTrue(wanted())
        assertTrue(wanted(ids = "clock,weather"))
    }

    @Test
    fun portraitNeverWantsIt() {
        assertFalse(wanted(landscape = false))
    }

    @Test
    fun masterSwitchOffMeansNoPill() {
        assertFalse(wanted(enabled = false))
    }

    @Test
    fun noReadableWidgetMeansNoPill() {
        assertFalse(wanted(ids = ""))
        assertFalse(wanted(ids = "nonsense,,"))
    }

    @Test
    fun hiddenPillAndOpenShadeKeepItAway() {
        assertFalse(wanted(pillHidden = true))
        assertFalse(wanted(shadeOpen = true))
    }

    @Test
    fun suppressedWidgetsMeanNoEmptyPill() {
        assertFalse(wanted(suppressed = true))
        assertFalse(wanted(ids = "clock,weather", suppressed = true))
    }

    @Test
    fun suppressionDefaultsToOffSoOldCallersKeepTheirResult() {
        assertTrue(landscapeWidgetPillWanted(true, true, "clock", false, false))
    }

    @Test
    fun tapCloseSuppressionDropsThePillUntilTheIslandOpensAgain() {
        // "Off after tap to close" on: a tap that closed the island holds the widgets (and so the pill) off.
        val held = IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false)
        assertFalse(wanted(suppressed = held))
        // The island opened again: the flag is cleared and the pill returns.
        val cleared = IdleWidgetOff.suppressed(closedByTap = false, pillHidden = false, offAfterTapClose = true, offAfterLongPress = false)
        assertTrue(wanted(suppressed = cleared))
        // Switch off: the tap changes nothing.
        val switchOff = IdleWidgetOff.suppressed(closedByTap = true, pillHidden = false, offAfterTapClose = false, offAfterLongPress = false)
        assertTrue(wanted(suppressed = switchOff))
    }

    @Test
    fun longPressSuppressionNeverLeavesAnEmptyPill() {
        val held = IdleWidgetOff.suppressed(closedByTap = false, pillHidden = true, offAfterTapClose = false, offAfterLongPress = true)
        assertFalse(wanted(suppressed = held))
    }

    @Test
    fun suppressedPillLetsTheIdleCircleBeTheCutout() {
        val pill = wanted(suppressed = true)
        val dot = IdleDotGeometry(centerXDp = -300f, topDp = 8f, sizeDp = 26f)
        assertFalse(pillOnIsland(hasCards = false, widgetPill = pill))
        // No separate dot either: with the pill gone the island's own idle circle is the cutout.
        assertFalse(shouldShowIdleDot(dot, hasCards = pillOnIsland(false, pill), isLandscape = true))
        // A card still brings the pill and the dot.
        assertTrue(pillOnIsland(hasCards = true, widgetPill = pill))
    }

    @Test
    fun pillIsOnTheIslandForACardOrTheWidgetPill() {
        assertTrue(pillOnIsland(hasCards = true, widgetPill = false))
        assertTrue(pillOnIsland(hasCards = false, widgetPill = true))
        assertTrue(pillOnIsland(hasCards = true, widgetPill = true))
        assertFalse(pillOnIsland(hasCards = false, widgetPill = false))
    }

    @Test
    fun theIdleDotFollowsThePillNotJustTheCards() {
        val dot = IdleDotGeometry(centerXDp = -300f, topDp = 8f, sizeDp = 26f)
        // No card, widget pill on: the circle is the toggle beside the pill.
        assertTrue(shouldShowIdleDot(dot, hasCards = pillOnIsland(false, true), isLandscape = true))
        // No card, no widget pill: the island's own idle circle is the dot, no separate one.
        assertFalse(shouldShowIdleDot(dot, hasCards = pillOnIsland(false, false), isLandscape = true))
        assertEquals(true, shouldShowIdleDot(dot, hasCards = pillOnIsland(true, false), isLandscape = true))
    }
}
