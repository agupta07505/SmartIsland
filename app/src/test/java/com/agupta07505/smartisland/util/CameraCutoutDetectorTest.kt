/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCutoutDetectorTest {

    @Test
    fun testCenterCameraCutoutOffset() {
        val screenWidthPx = 1080
        val density = 2.75f // 1080p density ~ 440 dpi

        // A center punch hole at top-center (x: 490 to 590, y: 0 to 80)
        val cutoutRect = Rect(490, 0, 590, 80)
        val result = CameraCutoutDetector.calculateCutoutOffset(cutoutRect, screenWidthPx, density)

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
        val cutoutRect = Rect(50, 10, 130, 90)
        val result = CameraCutoutDetector.calculateCutoutOffset(cutoutRect, screenWidthPx, density)

        assertTrue(result.hasHardwareCutout)
        // Center X = 90, Screen Center X = 540 -> xOffsetPx = -450 -> xOffsetDp = -180 -> clamped to MIN_X_OFFSET (-140)
        assertEquals(-140f, result.xOffsetDp, 0.01f)
        assertEquals(4f, result.yOffsetDp, 0.01f) // 10 / 2.5 = 4dp
    }
}
