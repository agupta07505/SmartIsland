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

        listOf(true, false).forEach { isCircleLeft ->
            listOf(-130f, 0f, 130f).forEach { xOffsetDp ->
                val mainWidthPx = widthDp * density
                val circleSizePx = heightDp * density
                val compactGapPx = compactGapDp * density
                val edgePaddingPx = edgePaddingDp * density
                val groupWidthPx = mainWidthPx + compactGapPx + circleSizePx

                val desiredMainLeftPx = screenWidthPx / 2f + xOffsetDp * density - mainWidthPx / 2f
                val (minMainLeftPx, maxMainLeftPx) = when {
                    isCircleLeft -> (edgePaddingPx + circleSizePx + compactGapPx) to (screenWidthPx - edgePaddingPx - mainWidthPx).coerceAtLeast(edgePaddingPx + circleSizePx + compactGapPx)
                    else -> edgePaddingPx to (screenWidthPx - edgePaddingPx - groupWidthPx).coerceAtLeast(edgePaddingPx)
                }
                val mainLeftPx = desiredMainLeftPx.coerceIn(minMainLeftPx, maxMainLeftPx)
                val groupStartPx = if (isCircleLeft) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx
                val groupEndPx = if (!isCircleLeft) mainLeftPx + mainWidthPx + compactGapPx + circleSizePx else mainLeftPx + mainWidthPx
                val groupCenterPx = (groupStartPx + groupEndPx) / 2f
                val windowXPx = (groupCenterPx - screenWidthPx / 2f).toInt()
                val windowWidthPx = (groupWidthPx + 32f * density).toInt()

                val windowLeftPx = screenWidthPx / 2f + windowXPx - windowWidthPx / 2f
                val windowRightPx = screenWidthPx / 2f + windowXPx + windowWidthPx / 2f

                val circleLeftPx = if (isCircleLeft) mainLeftPx - compactGapPx - circleSizePx else mainLeftPx + mainWidthPx + compactGapPx
                val circleRightPx = circleLeftPx + circleSizePx

                // 1. Assert no overlap between main pill and circle (separation >= compactGapPx)
                if (isCircleLeft) {
                    val gap = mainLeftPx - circleRightPx
                    org.junit.Assert.assertTrue(
                        "Left circle must not collapse into pill, gap ($gap) >= compactGap ($compactGapPx)",
                        gap >= compactGapPx - 0.01f
                    )
                } else {
                    val gap = circleLeftPx - (mainLeftPx + mainWidthPx)
                    org.junit.Assert.assertTrue(
                        "Right circle must not collapse into pill, gap ($gap) >= compactGap ($compactGapPx)",
                        gap >= compactGapPx - 0.01f
                    )
                }

                // 2. Assert that the secondary circle is completely inside the window
                org.junit.Assert.assertTrue(
                    "Circle left ($circleLeftPx) must be >= window left ($windowLeftPx)",
                    circleLeftPx >= windowLeftPx - 0.01f
                )
                org.junit.Assert.assertTrue(
                    "Circle right ($circleRightPx) must be <= window right ($windowRightPx)",
                    circleRightPx <= windowRightPx + 0.01f
                )

                // 3. Assert circle and pill are within screen bounds
                org.junit.Assert.assertTrue(circleLeftPx >= edgePaddingPx - 0.01f)
                org.junit.Assert.assertTrue(circleRightPx <= screenWidthPx - edgePaddingPx + 0.01f)
                org.junit.Assert.assertTrue(mainLeftPx >= edgePaddingPx - 0.01f)
                org.junit.Assert.assertTrue(mainLeftPx + mainWidthPx <= screenWidthPx - edgePaddingPx + 0.01f)
            }
        }
    }

    @Test
    fun landscapeExpandedWidthIsFixedAndDoesNotFillLandscapeScreenWidth() {
        val landscapeScreenWidth = 914f
        val landscapeScreenHeight = 411f

        val portraitExpandedWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = false,
            screenWidthDp = landscapeScreenHeight, // 411dp in portrait
            screenHeightDp = landscapeScreenWidth  // 914dp in portrait
        )
        val landscapeExpandedWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = landscapeScreenWidth,  // 914dp in landscape
            screenHeightDp = landscapeScreenHeight // 411dp in landscape
        )

        // The landscape expanded width must equal the portrait compact width, NOT 95% of 914dp
        assertEquals(portraitExpandedWidth, landscapeExpandedWidth, 0.01f)
        org.junit.Assert.assertTrue(landscapeExpandedWidth < 450f)
        org.junit.Assert.assertTrue(landscapeExpandedWidth < landscapeScreenWidth * 0.95f)

        // Clamping bounds for tablets / ultra-wides
        val tabletLandscapeWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 1280f,
            screenHeightDp = 800f
        )
        assertEquals(440f, tabletLandscapeWidth, 0.01f)

        val smallLandscapeWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 640f,
            screenHeightDp = 320f
        )
        assertEquals(340f, smallLandscapeWidth, 0.01f)
    }

    @Test
    fun notchModeExpandedTopOffsetClearsHardwareNotchAndStatusBar() {
        val notchHeight = 35f
        val statusBarHeightStandard = 24f
        val statusBarHeightTall = 40f

        // Notch mode: MUST clear both the hardware notch height and status bar height with a safe gap
        val offsetStandard = com.agupta07505.smartisland.ui.calculateExpandedTopOffset(
            enableNotchMode = true,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightStandard,
            notchHeightDp = notchHeight
        )
        org.junit.Assert.assertTrue(
            "Expanded offset in notch mode ($offsetStandard) must strictly exceed notch height ($notchHeight)",
            offsetStandard >= notchHeight + com.agupta07505.smartisland.ui.NOTCH_MODE_EXPANDED_CLEARANCE_DP
        )
        org.junit.Assert.assertTrue(
            "Expanded offset in notch mode ($offsetStandard) must strictly exceed status bar ($statusBarHeightStandard)",
            offsetStandard >= statusBarHeightStandard + com.agupta07505.smartisland.ui.NOTCH_MODE_EXPANDED_CLEARANCE_DP
        )
        assertEquals(43f, offsetStandard, 0.01f)

        // Tall notch device (e.g., Pixel 3 XL or iPhone deep notch)
        val offsetTall = com.agupta07505.smartisland.ui.calculateExpandedTopOffset(
            enableNotchMode = true,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightTall,
            notchHeightDp = notchHeight
        )
        org.junit.Assert.assertTrue(
            "Expanded offset on tall status bar ($offsetTall) must clear tall status bar ($statusBarHeightTall)",
            offsetTall >= statusBarHeightTall + com.agupta07505.smartisland.ui.NOTCH_MODE_EXPANDED_CLEARANCE_DP
        )
        assertEquals(48f, offsetTall, 0.01f)

        // Non-notch mode preserves standard status bar positioning
        val normalOffset = com.agupta07505.smartisland.ui.calculateExpandedTopOffset(
            enableNotchMode = false,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightStandard
        )
        assertEquals(statusBarHeightStandard, normalOffset, 0.01f)

        val companionOffset = com.agupta07505.smartisland.ui.calculateExpandedTopOffset(
            enableNotchMode = false,
            hasCompanion = true,
            statusBarHeightDp = statusBarHeightStandard,
            circleSizeDp = 34f,
            compactGapDp = 8f
        )
        assertEquals(42f, companionOffset, 0.01f)
    }

    @Test
    fun secondaryBubbleExpandedOffsetShiftsRightInLeftCircleMode() {
        val screenCenter = 200f
        val expandedCompactX = 144f // main pill start
        val miniPillWidth = 112f
        val circleSize = 34f
        val compactGap = 8f

        val pillCenterOffset = (expandedCompactX + miniPillWidth / 2f) - screenCenter

        // Collapsed left-side circle center offset: circle is to the LEFT of pill
        val collapsedLeftCircleOffset = (expandedCompactX - compactGap - circleSize / 2f) - screenCenter

        // 1. With ONLY 2 notifications (secondaryIsPill == true):
        // In left-side circle mode, it MUST shift RIGHT to pill location
        val expandedOffsetLeftMode = com.agupta07505.smartisland.ui.calculateSecondaryExpandedOffset(
            secondaryIsPill = true,
            isCircleLeft = true,
            isFullWidth = true,
            expandedCompactX = expandedCompactX,
            screenCenter = screenCenter,
            miniPillWidth = miniPillWidth,
            circleSize = circleSize,
            compactGap = compactGap
        )
        assertEquals(pillCenterOffset, expandedOffsetLeftMode, 0.01f)
        org.junit.Assert.assertTrue(
            "Left circle must shift to the right into pill position ($expandedOffsetLeftMode > $collapsedLeftCircleOffset)",
            expandedOffsetLeftMode > collapsedLeftCircleOffset
        )

        // 2. Right-side circle mode also lands at pill location
        val expandedOffsetRightMode = com.agupta07505.smartisland.ui.calculateSecondaryExpandedOffset(
            secondaryIsPill = true,
            isCircleLeft = false,
            isFullWidth = true,
            expandedCompactX = expandedCompactX,
            screenCenter = screenCenter,
            miniPillWidth = miniPillWidth,
            circleSize = circleSize,
            compactGap = compactGap
        )
        assertEquals(pillCenterOffset, expandedOffsetRightMode, 0.01f)

        // 3. With 3+ notifications (secondaryIsPill == false):
        // Left circle stays on the left
        val threeNotifLeftOffset = com.agupta07505.smartisland.ui.calculateSecondaryExpandedOffset(
            secondaryIsPill = false,
            isCircleLeft = true,
            isFullWidth = true,
            expandedCompactX = expandedCompactX,
            screenCenter = screenCenter,
            miniPillWidth = miniPillWidth,
            circleSize = circleSize,
            compactGap = compactGap
        )
        assertEquals(collapsedLeftCircleOffset, threeNotifLeftOffset, 0.01f)
    }
}
