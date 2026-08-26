/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.CompactNotificationShape
import com.agupta07505.smartisland.ui.compactNotificationShapes
import org.junit.Assert.assertEquals
import org.junit.Test

class IslandOverlayLayoutTest {

    @Test
    fun compactIndicatorsMatchNotificationMatrix() {
        assertEquals(
            listOf(CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 2, expanded = false)
        )
        assertEquals(
            listOf(CompactNotificationShape.MiniPill),
            compactNotificationShapes(notificationCount = 2, expanded = true)
        )
        assertEquals(
            listOf(CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 3, expanded = false)
        )
        assertEquals(
            listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 3, expanded = true)
        )
    }

    @Test
    fun splitModeCircleFitsWithinWindowBounds() {
        val density = 2.75f
        val screenWidthPx = 1080f
        val widthDp = 112f
        val heightDp = 34f
        val compactGapDp = 8f
        val edgePaddingDp = 8f

        listOf(-130f, 0f, 130f).forEach { xOffsetDp ->
            val mainWidthPx = widthDp * density
            val circleSizePx = heightDp * density
            val compactGapPx = compactGapDp * density
            val edgePaddingPx = edgePaddingDp * density
            val groupWidthPx = mainWidthPx + compactGapPx + circleSizePx

            val desiredMainLeftPx = screenWidthPx / 2f + xOffsetDp * density - mainWidthPx / 2f
            val maxMainLeftPx = (screenWidthPx - groupWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
            val mainLeftPx = desiredMainLeftPx.coerceIn(edgePaddingPx, maxMainLeftPx)
            val groupCenterPx = mainLeftPx + groupWidthPx / 2f
            val windowXPx = (groupCenterPx - screenWidthPx / 2f).toInt()
            val windowWidthPx = (groupWidthPx + 32f * density).toInt()

            val windowLeftPx = screenWidthPx / 2f + windowXPx - windowWidthPx / 2f
            val windowRightPx = screenWidthPx / 2f + windowXPx + windowWidthPx / 2f

            val circleLeftPx = mainLeftPx + mainWidthPx + compactGapPx
            val circleRightPx = circleLeftPx + circleSizePx

            // Assert that the secondary circle is completely inside the window with at least 8px margin
            org.junit.Assert.assertTrue(
                "Circle left ($circleLeftPx) must be >= window left ($windowLeftPx)",
                circleLeftPx >= windowLeftPx
            )
            org.junit.Assert.assertTrue(
                "Circle right ($circleRightPx) must be <= window right ($windowRightPx)",
                circleRightPx <= windowRightPx
            )
        }
    }
}
