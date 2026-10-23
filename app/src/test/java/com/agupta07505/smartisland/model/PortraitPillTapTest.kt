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

class PortraitPillTapTest {
    // A 120 dp wide, 34 dp tall pill at 3x density: 360 x 102 px. Collapsed, the shell is the pill (centre 180 px).
    private val pillW = 360f
    private val pillH = 102f

    @Test
    fun centreBandIsAThirdOfThePillAroundItsCentre() {
        val half = pillW * PILL_CENTER_ZONE_SHARE / 2f
        assertTrue(pillCenterTapHit(180f, 50f, pillW, pillW, pillH))
        assertTrue(pillCenterTapHit(180f - half + 1f, 50f, pillW, pillW, pillH))
        assertTrue(pillCenterTapHit(180f + half - 1f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f - half - 1f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(180f + half + 1f, 50f, pillW, pillW, pillH))
    }

    @Test
    fun glyphSlotsAreNotTheCentre() {
        // Left glyph (about 12% in) and right glyph (about 90% in).
        assertFalse(pillCenterTapHit(pillW * 0.12f, 50f, pillW, pillW, pillH))
        assertFalse(pillCenterTapHit(pillW * 0.9f, 50f, pillW, pillW, pillH))
    }

    @Test
    fun expandedShellMeasuresFromItsOwnCentreAndOnlyTheTopRows() {
        // Expanded: the shell is 900 px wide and 600 px tall; the pill's centre band sits at the top centre.
        assertTrue(pillCenterTapHit(450f, 40f, 900f, pillW, pillH))
        assertFalse(pillCenterTapHit(450f, pillH + 10f, 900f, pillW, pillH))
        assertFalse(pillCenterTapHit(150f, 40f, 900f, pillW, pillH))
    }

    @Test
    fun degenerateSizesNeverHit() {
        assertFalse(pillCenterTapHit(0f, 0f, 0f, pillW, pillH))
        assertFalse(pillCenterTapHit(0f, 0f, pillW, 0f, pillH))
        assertFalse(pillCenterTapHit(0f, 0f, pillW, pillW, 0f))
    }

    @Test
    fun theCentreIsAnIdleControlInPortraitWithCards() {
        assertTrue(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = false, hasCards = true))
        assertFalse(portraitCenterTapIsIdleControl(isLandscape = true, notchMode = false, hasCards = true))
        assertTrue(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = true, hasCards = true))
        assertFalse(portraitCenterTapIsIdleControl(isLandscape = false, notchMode = false, hasCards = false))
    }

    @Test
    fun zoneShareIsSmallerThanTheGlyphFreeMiddle() {
        assertEquals(0.34f, PILL_CENTER_ZONE_SHARE, 0.0001f)
    }

    @Test
    fun closeZoneWithoutACutoutIsThePillCentreBand() {
        val z = expandedCloseZone(false, 60f, 40f, 5f, 8f, 12f, 120f, 34f)
        assertEquals(120f * PILL_CENTER_ZONE_SHARE, z.widthDp, 0.0001f)
        assertEquals(34f, z.heightDp, 0.0001f)
        assertEquals(null, z.offsetXDp)
        assertEquals(0f, z.topDp, 0.0001f)
    }

    @Test
    fun closeZoneOnACutoutUsesTheDetectedRectInWindowSpace() {
        // Hole detected 70 x 44 dp, 6 dp right of centre, top at 20 dp; the window sits 12 dp down: the zone starts 8 dp into it.
        val z = expandedCloseZone(true, 70f, 44f, 6f, 20f, 12f, 120f, 34f)
        assertEquals(120f * PILL_CENTER_ZONE_SHARE, z.widthDp, 0.0001f)
        assertEquals(44f, z.heightDp, 0.0001f)
        assertEquals(6f, z.offsetXDp!!, 0.0001f)
        assertEquals(8f, z.topDp, 0.0001f)
    }

    @Test
    fun closeZoneNeverStartsAboveTheWindowOrExceedsThePillCentreBand() {
        val z = expandedCloseZone(true, 150f, 40f, 0f, 4f, 12f, 112f, 34f)
        assertEquals(112f * PILL_CENTER_ZONE_SHARE, z.widthDp, 0.0001f)
        assertEquals(0f, z.topDp, 0.0001f)
    }

    @Test
    fun degenerateCutoutFallsBackToThePill() {
        val z = expandedCloseZone(true, 0f, 0f, 0f, 0f, 0f, 120f, 34f)
        assertEquals(null, z.offsetXDp)
    }

    // Band: 38 dp wide (centre 0), 34 dp tall from y = 12.
    private fun overlap(cutX: Float?, cutW: Float = 30f, cutTop: Float = 12f, cutH: Float = 24f) =
        centreBandOverlapsCutout(0f, 38f, 12f, 34f, cutX, cutW, cutTop, cutH)

    @Test
    fun centredCutoutOverlapsTheBand() = assertTrue(overlap(0f))

    @Test
    fun cutoutOnlyTouchingTheBandEdgeDoesNotCount() = assertFalse(overlap(34f)) // cutout 19..49: 0 dp

    @Test
    fun overlapOfExactlyOneDpCounts() = assertTrue(overlap(33f))   // cutout 18..48, band -19..19: 1 dp

    @Test
    fun lessThanOneDpDoesNotCount() = assertFalse(overlap(33.5f)) // cutout 18.5..48.5: 0.5 dp

    @Test
    fun cutoutAboveOrBelowTheBandDoesNotOverlap() {
        assertFalse(overlap(0f, cutTop = -40f))
        assertFalse(overlap(0f, cutTop = 60f))
    }

    @Test
    fun noKnownCutoutNeverOverlaps() = assertFalse(overlap(null))

    @Test
    fun longPressRules() {
        // Overlapping: glyphs open the apps, the middle band keeps the normal long press.
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = false))
        assertFalse(longPressOpensApps(false, false, true, centreOverlapsCutout = true, touchInCentreBand = true))
        // Not overlapping: anywhere.
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = false, touchInCentreBand = true))
        assertTrue(longPressOpensApps(false, false, true, centreOverlapsCutout = false, touchInCentreBand = false))
        // Notch mode behaves as overlapping.
        assertTrue(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = false))
        assertFalse(longPressOpensApps(false, true, true, centreOverlapsCutout = false, touchInCentreBand = true))
        // Landscape: the collapsed pill always opens the apps, whatever the overlap flags say.
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true))
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = true, touchInCentreBand = true))
        // Landscape idle cutout (no card, no widget pill): never the shortcut, only the normal long press.
        assertFalse(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true, pillHasContent = false))
        assertTrue(longPressOpensApps(true, false, true, centreOverlapsCutout = false, touchInCentreBand = true, pillHasContent = true))
        assertFalse(longPressOpensApps(true, false, false, false, false))
        // The launcher switched off: unchanged behaviour.
        assertFalse(longPressOpensApps(false, false, false, false, false))
    }

    @Test
    fun cutoutTouchTargetIsGrownToAFingerAroundItsCentre() {
        val t = cutoutTouchTarget(CutoutArea(10f, 12f, 12f, 12f))
        assertEquals(CUTOUT_TARGET_MIN_WIDTH_DP, t.widthDp, 0.001f)
        assertEquals(CUTOUT_TARGET_MIN_HEIGHT_DP, t.heightDp, 0.001f)
        assertEquals(10f, t.centreXDp, 0.001f)
        assertEquals(4f, t.topDp, 0.001f)   // centre y 18, half height 14
        assertEquals(0f, cutoutTouchTarget(CutoutArea(0f, 2f, 12f, 12f)).topDp, 0.001f)
        assertEquals(60f, cutoutTouchTarget(CutoutArea(0f, 0f, 60f, 40f)).widthDp, 0.001f)
    }
}
