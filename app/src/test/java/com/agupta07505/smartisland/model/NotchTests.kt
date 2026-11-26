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

class NotchSettledZoneTest {
    private val screenCentre = 196f
    private val width = 112f

    private fun zoneFor(hole: CutoutArea?, pillCentre: Float): CollapsedZone {
        val camera = PulsePlacement.camera(hole, screenCentre, pillCentre, width)
        return CollapsedSlots.zone(width, camera.centreInPillDp, camera.gapDp)
    }

    @Test
    fun notchTouchZone() {
        val alignedZone = zoneFor(CutoutArea(0f, 0f, 30f, 28f), screenCentre)
        assertEquals(0f, alignedZone.band.shiftDp, 0.001f)
        assertEquals(CUTOUT_TARGET_MIN_WIDTH_DP, alignedZone.band.widthDp, 0.001f)
        assertEquals(SlotSide.Both, alignedZone.side)

        // the hole is 20 dp left of the screen's centre, the notch 15 dp left of it: the hole lies 5 dp left of the notch's middle
        val shiftedZone = zoneFor(CutoutArea(-20f, 0f, 30f, 28f), screenCentre - 15f)
        assertEquals(-5f, shiftedZone.band.shiftDp, 0.001f)

        val missingZone = zoneFor(null, screenCentre)
        assertEquals(CameraBand(0f, width * PILL_CENTER_ZONE_SHARE), missingZone.band)
        assertNull(PulsePlacement.camera(null, screenCentre, screenCentre, width).sideZone)

        // the notch's window starts at the screen's top, the pill's at its offset
        val topHole = CutoutArea(0f, 0f, 30f, 28f)
        val topBand = zoneFor(topHole, screenCentre).band
        assertTrue(centreBandOverlapsCutout(topBand.shiftDp, topBand.widthDp, 0f, 34f, topHole.centreXDp, topHole.widthDp, topHole.topDp, topHole.heightDp))
        // the pill's offset (12 dp) read as the notch's top would leave a hole at the very top short of the band's height
        assertFalse(centreBandOverlapsCutout(topBand.shiftDp, topBand.widthDp, 40f, 34f, topHole.centreXDp, topHole.widthDp, topHole.topDp, topHole.heightDp))

        val outsideHole = CutoutArea(-90f, 0f, 30f, 28f)
        val outsideZone = zoneFor(outsideHole, screenCentre + 40f)
        val overlaps = centreBandOverlapsCutout(
            40f + outsideZone.band.shiftDp, outsideZone.band.widthDp, 0f, 34f, outsideHole.centreXDp, outsideHole.widthDp, outsideHole.topDp, outsideHole.heightDp
        )
        assertFalse(overlaps)
    }

    @Test
    fun theExpandedCloseZoneSitsOnTheNotchHole() {
        val hole = CutoutArea(0f, 0f, 30f, 28f)
        val zone = expandedCloseZone(
            hasHardwareCutout = true, cutoutWidthDp = hole.widthDp, cutoutHeightDp = hole.heightDp,
            cutoutXOffsetDp = hole.centreXDp, cutoutTopDp = hole.topDp, windowTopDp = 0f,
            pillWidthDp = width, pillHeightDp = 34f
        )
        assertEquals(hole.centreXDp, zone.offsetXDp!!, 0.001f)
        assertEquals(0f, zone.topDp, 0.001f)
        assertEquals(hole.heightDp, zone.heightDp, 0.001f)
    }
}

class NotchCompanionsTest {

    private fun card(key: String, pkg: String) = IslandNotification(
        key = key, packageName = pkg, appName = pkg, title = key, text = "", timeMillis = 0L,
        mode = IslandMode.Notification
    )

    @Test
    fun notchCompanions() {
        val singleList = listOf(card("a", "app.one"))
        assertEquals(NotchCompanionPlan.None, notchCompanionPlan(singleList, 0, expanded = false))
        assertEquals(NotchCompanionPlan.None, notchCompanionPlan(singleList, 0, expanded = true))

        val expandedList = listOf(card("a", "app.one"), card("b", "app.two"))
        val expandedPlan = notchCompanionPlan(expandedList, 0, expanded = true)
        assertTrue(expandedPlan.pillStays)
        assertEquals(1, expandedPlan.pillCardIndex)
        assertFalse(expandedPlan.earShown)

        val thirdList = listOf(card("a", "app.one"), card("b", "app.two"), card("c", "app.three"))
        val thirdPlan = notchCompanionPlan(thirdList, 0, expanded = true)
        assertTrue(thirdPlan.pillStays)
        assertEquals(1, thirdPlan.pillCardIndex)
        assertFalse(thirdPlan.earShown)
        assertEquals(-1, thirdPlan.earCardIndex)

        val pickerList = listOf(card("a1", "app.one"), card("a2", "app.one"))
        val pickerPlan = notchCompanionPlan(pickerList, 0, expanded = true)
        assertFalse(pickerPlan.pillStays)
        assertEquals(NotchEarContent.SiblingPicker, pickerPlan.earContent)
        assertEquals(1, pickerPlan.earCardIndex)

        val siblingsList = listOf(card("a", "app.one"), card("b1", "app.two"), card("b2", "app.two"))
        val siblingsPlan = notchCompanionPlan(siblingsList, 0, expanded = true)
        assertTrue(siblingsPlan.pillStays)
        assertFalse(siblingsPlan.earShown)
    }

    @Test
    fun pickerCompanions() {
        val pickerList = listOf(card("a1", "app.one"), card("b1", "app.two"), card("a2", "app.one"))
        val pickerPlan = notchCompanionPlan(pickerList, 0, expanded = true)
        assertTrue(pickerPlan.pillStays)
        assertEquals(1, pickerPlan.pillCardIndex)
        assertEquals(NotchEarContent.SiblingPicker, pickerPlan.earContent)
        assertEquals(2, pickerPlan.earCardIndex)

        val collapsedList = listOf(card("a", "app.one"), card("b", "app.two"))
        val collapsedPlan = notchCompanionPlan(collapsedList, 0, expanded = false)
        assertFalse(collapsedPlan.pillStays)
        assertEquals(NotchEarContent.OtherEvent, collapsedPlan.earContent)
        assertEquals(1, collapsedPlan.earCardIndex)
    }
}

class NotchTopEdgeTest {
    @Test
    fun swapCornerRadius() {
        assertEquals(0f, NotchTopEdge.swapRoundness(0f), 1e-6f)
        assertEquals(1f, NotchTopEdge.swapRoundness(1f), 1e-6f)
        assertEquals(1f, NotchTopEdge.swapRoundness(NotchTopEdge.SWAP_EASE_BELOW), 1e-6f)

        var last = -1f
        for (i in 0..100) {
            val r = NotchTopEdge.swapRoundness(i / 100f)
            assertTrue(r >= last - 1e-6f)
            last = r
        }

        for (i in 0..10) {
            val o = i / 10f
            assertEquals(1f, NotchTopEdge.swapRoundness(o) + NotchTopEdge.swapFlat(o), 1e-6f)
        }

        // the width has already closed, the height has not: the top stays round until the height is nearly closed
        assertEquals(1f, NotchTopEdge.swapRoundness(0f, NotchTopEdge.SWAP_EASE_BELOW), 1e-6f)
        assertEquals(NotchTopEdge.swapRoundness(0.2f, 0.2f), NotchTopEdge.swapRoundness(-0.04f, 0.2f), 1e-6f)
        assertEquals(NotchTopEdge.swapRoundness(0.2f, 0.2f), NotchTopEdge.swapRoundness(0.2f, 0.05f), 1e-6f)

        assertEquals(0f, NotchTopEdge.swapRoundness(0f, 0f), 1e-6f)
        assertEquals(0f, NotchTopEdge.swapRoundness(-0.04f, 0f), 1e-6f)
        assertEquals(1f, NotchTopEdge.swapRoundness(1f, 1f), 1e-6f)
        // During a swap the island stays expanded. Only the outgoing shell approaches the cutout. It meets the plain
        // collapse at the cutout and is never flatter than it on the way
        assertEquals(
            NotchTopEdge.roundness(expandProgress = 0.0f, swapOpen = null, collapsing = true),
            NotchTopEdge.roundness(expandProgress = 1f, swapOpen = 0f), 1e-6f
        )
        for (i in 0..100) {
            val o = i / 100f
            assertTrue(NotchTopEdge.swapRoundness(o) >= NotchTopEdge.collapseRoundness(o, o) - 1e-6f)
        }
    }

    @Test
    fun collapseCornerRadius() {
        for (i in 0..100) {
            val o = i / 100f
            assertTrue(NotchTopEdge.swapRoundness(o) >= NotchTopEdge.collapseRoundness(o, o) - 1e-6f)
            assertTrue(NotchTopEdge.swapRoundness(o, o * 0.5f) >= NotchTopEdge.collapseRoundness(o, o * 0.5f) - 1e-6f)
        }

        assertEquals(NotchTopEdge.collapseRoundness(0.3f, 0.3f), NotchTopEdge.collapseRoundness(-0.04f, 0.3f), 1e-6f)
        assertEquals(NotchTopEdge.collapseRoundness(0.3f, 0.3f), NotchTopEdge.collapseRoundness(0.3f, 0.1f), 1e-6f)
        assertEquals(0f, NotchTopEdge.collapseRoundness(-0.04f, 0f), 1e-6f)

        assertEquals(1f, NotchTopEdge.collapseRoundness(1f, 1f), 1e-6f)
        assertEquals(1f, NotchTopEdge.collapseRoundness(NotchTopEdge.COLLAPSE_EASE_BELOW, 0f), 1e-6f)
        assertEquals(0f, NotchTopEdge.collapseRoundness(0f, 0f), 1e-6f)

        assertEquals(0f, NotchTopEdge.collapseRoundness(0f, 0f), 1e-6f)
        // rounding starts at once: a shell a few percent open already has a visibly rounded top
        assertTrue(NotchTopEdge.collapseRoundness(0.02f, 0.02f) > 0.02f)
        assertTrue(NotchTopEdge.collapseRoundness(0.05f, 0.05f) > 0.1f)

        assertEquals(1f, NotchTopEdge.collapseRoundness(0.9f, 0.9f), 1e-6f)
        assertEquals(1f, NotchTopEdge.collapseRoundness(NotchTopEdge.COLLAPSE_EASE_BELOW, NotchTopEdge.COLLAPSE_EASE_BELOW), 1e-6f)
        assertTrue(NotchTopEdge.collapseRoundness(0.2f, 0.2f) < 1f)
    }

    @Test fun expandingTopEdge() {
        var last = -1f
        for (i in 0..100) {
            val r = NotchTopEdge.swapRoundness(i / 100f, i / 100f)
            assertTrue(r >= last - 1e-6f)
            last = r
        }
        // never a step: neighbouring frames differ by a small amount
        for (i in 1..100) {
            val d = NotchTopEdge.swapRoundness(i / 100f) - NotchTopEdge.swapRoundness((i - 1) / 100f)
            assertTrue(d < 0.1f)
        }
    }

    @Test
    fun flatTopDuringSwap() {
        for (i in 0..100) {
            val o = i / 100f
            // the swap rounds earlier than a plain collapse (SWAP_EASE_BELOW < COLLAPSE_EASE_BELOW), so it is never flatter
            assertEquals(1f - NotchTopEdge.swapRoundness(o, o), NotchTopEdge.swapFlat(o), 1e-6f)
            assertTrue(NotchTopEdge.swapFlat(o) <= 1f - NotchTopEdge.collapseRoundness(o, o) + 1e-6f)
            assertTrue(NotchTopEdge.flat(1f, o) <= NotchTopEdge.flat(o, null, true) + 1e-6f)
            assertEquals(NotchTopEdge.swapFlat(o), NotchTopEdge.flat(1f, o), 1e-6f)
        }

        assertEquals(0.4f, NotchTopEdge.roundness(0.4f, null), 1e-6f)
        assertEquals(0.6f, NotchTopEdge.flat(0.4f, null), 1e-6f)

        // a collapsing island (i.e. progress 0.2 in this case) stays at least as flat as its own progress says
        assertEquals(0.2f, NotchTopEdge.roundness(0.2f, 1f), 1e-6f)
        assertEquals(0f, NotchTopEdge.roundness(1f, 0f), 1e-6f)
        assertEquals(1f, NotchTopEdge.flat(1f, 0f), 1e-6f)
        assertEquals(0f, NotchTopEdge.flat(1f, 1f), 1e-6f)

        assertEquals(NotchTopEdge.collapseRoundness(0.2f, 0.4f), NotchTopEdge.roundness(0.2f, null, true, 0.4f), 1e-6f)
        assertEquals(1f - NotchTopEdge.collapseRoundness(0.2f, 0.4f), NotchTopEdge.flat(0.2f, null, true, 0.4f), 1e-6f)
        assertEquals(0.2f, NotchTopEdge.roundness(0.2f, null, false, 0.4f), 1e-6f)
    }

    @Test
    fun collapseEasing() {
        var last = 2f
        for (i in 100 downTo 0) {
            val p = i / 100f
            val r = NotchTopEdge.collapseRoundness(p, p)
            assertTrue(r <= last + 1e-6f)
            last = r
        }

        val e = NotchTopEdge.COLLAPSE_EASE_BELOW
        val before = NotchTopEdge.collapseRoundness(e - 0.01f, e - 0.01f)
        assertTrue(before > 0.99f && before < 1f)
    }
}

class NotchCollapseFadeTest {
    @Test
    fun appliesOnlyToNotchMusicWithArtworkBackdrop() {
        assertTrue(NotchCollapseFade.appliesTo(true, true, true))
        assertFalse(NotchCollapseFade.appliesTo(false, true, true))
        assertFalse(NotchCollapseFade.appliesTo(true, false, true))
        assertFalse(NotchCollapseFade.appliesTo(true, true, false))
    }

    @Test
    fun darknessRisesAsTheCardAlphaFalls() {
        assertEquals(0f, NotchCollapseFade.darkness(1f), 0.0001f)
        assertEquals(0.4f, NotchCollapseFade.darkness(0.6f), 0.0001f)
        assertEquals(1f, NotchCollapseFade.darkness(0f), 0.0001f)
    }

    @Test
    fun foregroundIsGoneByHalfwayAndFullWhenOpen() {
        assertEquals(1f, NotchCollapseFade.foregroundAlpha(1f), 0.0001f)
        assertEquals(0f, NotchCollapseFade.foregroundAlpha(0.5f), 0.0001f)
        assertEquals(0f, NotchCollapseFade.foregroundAlpha(0.1f), 0.0001f)
        assertTrue(NotchCollapseFade.foregroundAlpha(0.75f) in 0.4f..0.6f)
    }

    @Test
    fun layerStaysOpaqueUntilTheEnd() {
        assertEquals(1f, NotchCollapseFade.layerAlpha(0.5f), 0.0001f)
        assertEquals(1f, NotchCollapseFade.layerAlpha(NotchCollapseFade.LAYER_RELEASE_AT), 0.0001f)
        assertEquals(0f, NotchCollapseFade.layerAlpha(0f), 0.0001f)
    }
}
