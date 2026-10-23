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

class WidgetIslandTest {
    // A 120 dp wide pill at 3x density: 360 px. Collapsed, the shell is the pill (centre 180 px).
    private val pillW = 360f

    @Test
    fun pillSplitsIntoGlyphMiddleAndValue() {
        val half = pillW * PILL_CENTER_ZONE_SHARE / 2f
        assertEquals(PillTapZone.Glyph, pillTapZone(pillW * 0.12f, pillW, pillW))
        assertEquals(PillTapZone.Glyph, pillTapZone(180f - half - 1f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f - half + 1f, pillW, pillW))
        assertEquals(PillTapZone.Middle, pillTapZone(180f + half - 1f, pillW, pillW))
        assertEquals(PillTapZone.Value, pillTapZone(180f + half + 1f, pillW, pillW))
        assertEquals(PillTapZone.Value, pillTapZone(pillW * 0.9f, pillW, pillW))
    }

    @Test
    fun middleBandMatchesTheCentreTapBand() {
        for (x in 0..360 step 6) {
            val inBand = pillCenterTapHit(x.toFloat(), 10f, pillW, pillW, 100f)
            assertEquals("x=$x", inBand, pillTapZone(x.toFloat(), pillW, pillW) == PillTapZone.Middle)
        }
    }

    @Test
    fun glyphAndValueAreSeparateActions() {
        for (stored in listOf(false, true)) {
            // Even with stored cards waiting: only the middle band shows them.
            assertEquals(WidgetTapAction.GlyphTap, idleWidgetTapAction(true, false, PillTapZone.Glyph, hasStoredCards = stored))
            assertEquals(WidgetTapAction.ValueTap, idleWidgetTapAction(true, false, PillTapZone.Value, hasStoredCards = stored))
        }
    }

    @Test
    fun middleShowsStoredCardsOrDoesNothing() {
        assertEquals(WidgetTapAction.RevealStoredCards, idleWidgetTapAction(true, false, PillTapZone.Middle, hasStoredCards = true))
        assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, false, PillTapZone.Middle, hasStoredCards = false))
    }

    @Test
    fun roundIdleCircleKeepsItsOwnBehaviour() {
        // Landscape's small idle circle is not the collapsed pill: no widget island, whatever the zone or the stored cards.
        for (zone in PillTapZone.values()) {
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, true, zone, hasStoredCards = false))
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(true, true, zone, hasStoredCards = true))
        }
    }

    @Test
    fun noWidgetOnThePillMeansNoWidgetTap() {
        for (zone in PillTapZone.values()) {
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(false, false, zone, hasStoredCards = true))
            assertEquals(WidgetTapAction.None, idleWidgetTapAction(false, true, zone, hasStoredCards = false))
        }
    }

    @Test
    fun islandIsTwiceThePillWithAFloor() {
        val wide = widgetIslandSize(pillWidthDp = 100f, pillHeightDp = 40f, screenWidthDp = 400f)
        assertEquals(200f, wide.widthDp, 0.001f)
        assertEquals(80f, wide.heightDp, 0.001f)
        // The narrowest portrait pill (76 dp) doubled is wider than the floor, but its height (28 dp) is not tall enough.
        val narrow = widgetIslandSize(pillWidthDp = 76f, pillHeightDp = 28f, screenWidthDp = 400f)
        assertEquals(152f, narrow.widthDp, 0.001f)
        assertEquals(WIDGET_ISLAND_MIN_HEIGHT_DP, narrow.heightDp, 0.001f)
        // The small landscape circle doubled is too small for an icon and a value: both axes take the floor.
        val circle = widgetIslandSize(pillWidthDp = 30f, pillHeightDp = 30f, screenWidthDp = 800f)
        assertEquals(WIDGET_ISLAND_MIN_WIDTH_DP, circle.widthDp, 0.001f)
        assertEquals(WIDGET_ISLAND_MIN_HEIGHT_DP, circle.heightDp, 0.001f)
    }

    @Test
    fun islandNeverWiderThanTheScreen() {
        val size = widgetIslandSize(pillWidthDp = 300f, pillHeightDp = 40f, screenWidthDp = 360f)
        assertEquals(360f - 2f * WIDGET_ISLAND_EDGE_MARGIN_DP, size.widthDp, 0.001f)
    }

    @Test
    fun islandKeepsThePillCentreWhenThereIsRoom() {
        assertEquals(0f, widgetIslandCentre(0f, 124f, 400f), 0.001f)
        assertEquals(40f, widgetIslandCentre(40f, 124f, 400f), 0.001f)
        assertEquals(-40f, widgetIslandCentre(-40f, 124f, 400f), 0.001f)
    }

    @Test
    fun islandIsPushedInsideTheScreenMargins() {
        // 800 dp wide screen, 124 dp island: its centre may be at most 400 - 8 - 62 = 330 dp from the middle.
        assertEquals(330f, widgetIslandCentre(395f, 124f, 800f), 0.001f)
        assertEquals(-330f, widgetIslandCentre(-395f, 124f, 800f), 0.001f)
        // An island as wide as the screen allows is centred.
        assertEquals(0f, widgetIslandCentre(50f, 384f, 400f), 0.001f)
    }

    @Test
    fun opensAreClosedByTheStatesThatEndTheIdlePill() {
        assertFalse(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = true, hasImportantCard = false, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = true, widgetAvailable = true))
        assertTrue(widgetIslandMustClose(expanded = false, hasImportantCard = false, widgetAvailable = false))
    }

    @Test
    fun closeTimerDefaultsToNeverAndRunsFromTwoToNever() {
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.DEFAULT_SECONDS)
        assertNull(WidgetIslandTimer.closeDelayMs(WidgetIslandTimer.DEFAULT_SECONDS))
        assertEquals(3_000L, WidgetIslandTimer.closeDelayMs(3))
        assertEquals(2_000L, WidgetIslandTimer.closeDelayMs(2))
        assertEquals(10_000L, WidgetIslandTimer.closeDelayMs(10))
        assertNull(WidgetIslandTimer.closeDelayMs(WidgetIslandTimer.NEVER))
    }

    @Test
    fun closeTimerValuesAreNormalised() {
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.normalize(0))
        assertEquals(WidgetIslandTimer.NEVER, WidgetIslandTimer.normalize(-5))
        // One second is below the shortest setting.
        assertEquals(WidgetIslandTimer.MIN_SECONDS, WidgetIslandTimer.normalize(1))
        assertEquals(5, WidgetIslandTimer.normalize(5))
        assertEquals(WidgetIslandTimer.MAX_SECONDS, WidgetIslandTimer.normalize(9_999))
    }

    @Test
    fun widgetIslandHoldsExpandWhileOpenAndWhileShrinking() {
        assertFalse(widgetIslandHoldsExpand(open = false, shown = false))
        assertTrue(widgetIslandHoldsExpand(open = true, shown = false))
        assertTrue(widgetIslandHoldsExpand(open = true, shown = true))
        assertTrue(widgetIslandHoldsExpand(open = false, shown = true))
    }
}
