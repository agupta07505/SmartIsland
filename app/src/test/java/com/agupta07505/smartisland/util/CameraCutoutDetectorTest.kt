/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCutoutDetectorTest {

    @Test
    fun testCenterCameraCutoutOffset() {
        val screenWidthPx = 1080
        val density = 2.75f // 1080p density ~ 440 dpi

        // A center punch hole at top-center (x: 490 to 590, y: 0 to 80)
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 490,
            top = 0,
            right = 590,
            bottom = 80,
            screenWidthPx = screenWidthPx,
            density = density
        )

        assertTrue(result.hasHardwareCutout)
        // Center X = 540, Screen Center X = 540 -> xOffsetPx = 0 -> xOffsetDp = 0
        assertEquals(0f, result.xOffsetDp, 0.01f)
        assertEquals(0f, result.yOffsetDp, 0.01f)
        // width = 100px / 2.75 + 24dp = ~60.36dp -> clamped to MIN_WIDTH 76dp
        assertEquals(76f, result.widthDp, 0.01f)
    }

    @Test
    fun testLeftCameraCutoutOffset() {
        val screenWidthPx = 1080
        val density = 2.5f

        // A punch hole on the left (x: 50 to 130, y: 10 to 90)
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 50,
            top = 10,
            right = 130,
            bottom = 90,
            screenWidthPx = screenWidthPx,
            density = density
        )

        assertTrue(result.hasHardwareCutout)
        // Center X = 90, Screen Center X = 540 -> xOffsetPx = -450 -> xOffsetDp = -180 -> clamped to MIN_X_OFFSET (-140)
        assertEquals(-140f, result.xOffsetDp, 0.01f)
        assertEquals(4f, result.yOffsetDp, 0.01f) // 10 / 2.5 = 4dp
    }

    @Test
    fun testRawCutoutMeasurementsAreUnpaddedAndUnclamped() {
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 50, top = 10, right = 130, bottom = 90, screenWidthPx = 1080, density = 2.5f
        )
        // 80px / 2.5 = 32dp: the real hole, not the padded pill width.
        assertEquals(32f, result.rawWidthDp, 0.01f)
        // Centre 90px vs 540px, -450px / 2.5 = -180dp, even though xOffsetDp is clamped to -140.
        assertEquals(-180f, result.rawCenterOffsetDp, 0.01f)
        assertEquals(-140f, result.xOffsetDp, 0.01f)
    }

    @Test
    fun testNoCutoutHasNoRawMeasurements() {
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 0, top = 0, right = 10, bottom = 10, screenWidthPx = 1080, density = 0f
        )
        assertEquals(false, result.hasHardwareCutout)
        assertEquals(0f, result.rawWidthDp, 0f)
        assertEquals(0f, result.rawCenterOffsetDp, 0f)
    }

    // ---- Insets and landscape anchoring ----

    @Test
    fun theTopCutoutIsTheHighestRealRect() {
        val boxes = listOf(CutoutBox(0, 900, 80, 1000), CutoutBox(490, 0, 590, 80), CutoutBox(10, 0, 10, 0))
        assertEquals(CutoutBox(490, 0, 590, 80), CameraCutoutDetector.topCutout(boxes))
        assertEquals(null, CameraCutoutDetector.topCutout(emptyList()))
    }

    @Test
    fun inLandscapeTheHoleOnASideBezelIsTheAnchor() {
        // 2400 x 1080 landscape, hole on the left edge, vertically centred.
        val left = CameraCutoutDetector.lateralAnchor(listOf(CutoutBox(0, 490, 80, 590)), screenWidthPx = 2400)
        assertEquals(LateralSide.Left, left?.side)
        assertEquals(540f, left!!.centerYPx, 0.01f)
        val right = CameraCutoutDetector.lateralAnchor(listOf(CutoutBox(2320, 490, 2400, 590)), screenWidthPx = 2400)
        assertEquals(LateralSide.Right, right?.side)
        // A top-centre hole (portrait) is not lateral.
        assertEquals(null, CameraCutoutDetector.lateralAnchor(listOf(CutoutBox(490, 0, 590, 80)), screenWidthPx = 1080))
    }

    @Test
    fun recordedInsetsOnlyApplyToTheDisplaySizeTheyWereMeasuredOn() {
        CutoutInsets.update(listOf(CutoutBox(490, 0, 590, 80)), 1080, 2400)
        assertEquals(listOf(CutoutBox(490, 0, 590, 80)), CutoutInsets.boxesFor(1080, 2400))
        // Rotated: stale until new insets arrive.
        assertEquals(null, CutoutInsets.boxesFor(2400, 1080))
        // An empty report (a window away from the cutout) keeps what was known.
        CutoutInsets.update(emptyList(), 1080, 2400)
        assertEquals(listOf(CutoutBox(490, 0, 590, 80)), CutoutInsets.boxesFor(1080, 2400))
    }
}
