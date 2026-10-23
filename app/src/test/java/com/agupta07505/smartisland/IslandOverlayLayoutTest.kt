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

                org.junit.Assert.assertTrue(
                    "Circle left ($circleLeftPx) must be >= window left ($windowLeftPx)",
                    circleLeftPx >= windowLeftPx - 0.01f
                )
                org.junit.Assert.assertTrue(
                    "Circle right ($circleRightPx) must be <= window right ($windowRightPx)",
                    circleRightPx <= windowRightPx + 0.01f
                )

                org.junit.Assert.assertTrue(circleLeftPx >= edgePaddingPx - 0.01f)
                org.junit.Assert.assertTrue(circleRightPx <= screenWidthPx - edgePaddingPx + 0.01f)
                org.junit.Assert.assertTrue(mainLeftPx >= edgePaddingPx - 0.01f)
                org.junit.Assert.assertTrue(mainLeftPx + mainWidthPx <= screenWidthPx - edgePaddingPx + 0.01f)
            }
        }
    }

    @Test
    fun landscapeExpandedWidthIsAboutHalfTheScreenAndDoesNotFillIt() {
        val landscapeScreenWidth = 914f
        val landscapeScreenHeight = 411f

        val landscapeExpandedWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = landscapeScreenWidth,
            screenHeightDp = landscapeScreenHeight
        )
        // Half of the screen width (the heads-up-notification strip), never 95% of it.
        assertEquals(457f, landscapeExpandedWidth, 0.01f)
        org.junit.Assert.assertTrue(landscapeExpandedWidth < landscapeScreenWidth * 0.95f)

        // Clamping bounds for tablets / ultra-wides and small screens
        val tabletLandscapeWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 1280f,
            screenHeightDp = 800f
        )
        assertEquals(560f, tabletLandscapeWidth, 0.01f)

        val smallLandscapeWidth = com.agupta07505.smartisland.ui.calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 640f,
            screenHeightDp = 320f
        )
        assertEquals(340f, smallLandscapeWidth, 0.01f)

        // Portrait is untouched.
        assertEquals(
            411f * 0.95f,
            com.agupta07505.smartisland.ui.calculateExpandedWidth(false, 411f, 914f),
            0.01f
        )
    }

    @Test
    fun landscapeSplitLayoutIsCardPlusGapPlusPicker() {
        fun w(sw: Float, card: Float?, picker: Boolean = true, landscape: Boolean = true) =
            com.agupta07505.smartisland.ui.calculateExpandedWidth(
                landscape, sw, 411f, pickerSideBySide = picker, splitCardWidthDp = card
            )
        // 914 dp wide: picker 30% = 274 (inside 240..300), gap 14, so a measured 260 dp card -> 548.
        assertEquals(274.2f, com.agupta07505.smartisland.ui.splitPickerWidthDp(914f), 0.01f)
        assertEquals(548.2f, w(914f, 260f), 0.01f)
        // Not measured yet: the widest the card may get = 62% of the screen minus gap and picker.
        assertEquals(278.5f, com.agupta07505.smartisland.ui.splitCardMaxWidthDp(914f), 0.1f)
        assertEquals(914f * 0.62f, w(914f, null), 0.1f)
        // A tiny card is held at the minimum card width, a huge one at the maximum.
        assertEquals(240f + 14f + 274.2f, w(914f, 80f), 0.01f)
        assertEquals(278.5f + 14f + 274.2f, w(914f, 900f), 0.2f)
        // Tablet: picker capped at 300; the group stays about 62% of the screen.
        assertEquals(300f, com.agupta07505.smartisland.ui.splitPickerWidthDp(1280f), 0.01f)
        assertEquals(1280f * 0.62f, w(1280f, null), 0.1f)
        // Small screen: never wider than the screen minus the margin.
        assertEquals(484f, w(500f, null), 0.01f)
        // Picker closed, and portrait, are unchanged.
        assertEquals(457f, w(914f, 260f, picker = false), 0.01f)
        assertEquals(411f * 0.95f, w(411f, 260f, landscape = false), 0.01f)
    }

    @Test
    fun landscapeSplitGroupCoversAboutAThirdOfTheScreen() {
        // 914 x 411 dp screen, three picker rows (compact picker height 200 dp).
        val width = com.agupta07505.smartisland.ui.calculateExpandedWidth(true, 914f, 411f, pickerSideBySide = true)
        val height = com.agupta07505.smartisland.ui.expanded.siblingPickerHeight(3, compact = true).value
        val share = width * height / (914f * 411f)
        org.junit.Assert.assertTrue("share $share", share in 0.26f..0.34f)
    }

    @Test
    fun landscapeExpandedIslandSitsALittleHigherAndNeverAboveTheScreen() {
        fun top(sb: Float, landscape: Boolean, companion: Boolean = false) =
            com.agupta07505.smartisland.ui.calculateExpandedTopOffset(
                enableNotchMode = false, hasCompanion = companion, statusBarHeightDp = sb, isLandscape = landscape
            )
        assertEquals(24f, top(24f, landscape = false), 0.01f)
        assertEquals(10f, top(24f, landscape = true), 0.01f)
        assertEquals(0f, top(6f, landscape = true), 0.01f)
    }

    @Test
    fun pickerIslandStartsWhereTheRightPartOfTheWindowBegins() {
        // 548 dp window, 274 dp picker: it starts at about 50% of the width.
        assertEquals(0.5f, com.agupta07505.smartisland.ui.splitPickerLeftFraction(548f, 914f), 0.01f)
        // Unknown width falls back to 50 %.
        assertEquals(0.5f, com.agupta07505.smartisland.ui.splitPickerLeftFraction(0f, 914f), 0.0001f)
    }

    @Test
    fun collapsedCutoutsAreLoweredInsideTheLandscapeWindowOnly() {
        // Landscape: the window starts at the top, the cutouts are lowered by yOffset.
        assertEquals(205f, com.agupta07505.smartisland.ui.collapsedTopShiftDp(true, false, 205f), 0.01f)
        // Portrait and notch mode: the window itself is offset, nothing is lowered inside it.
        assertEquals(0f, com.agupta07505.smartisland.ui.collapsedTopShiftDp(false, false, 12f), 0.01f)
        assertEquals(0f, com.agupta07505.smartisland.ui.collapsedTopShiftDp(true, true, 12f), 0.01f)
        assertEquals(0f, com.agupta07505.smartisland.ui.collapsedTopShiftDp(true, false, -4f), 0.01f)
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
            offsetStandard >= notchHeight + 8f
        )
        org.junit.Assert.assertTrue(
            "Expanded offset in notch mode ($offsetStandard) must strictly exceed status bar ($statusBarHeightStandard)",
            offsetStandard >= statusBarHeightStandard + 8f
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
            offsetTall >= statusBarHeightTall + 8f
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

        // With only 2 notifications (secondaryIsPill == true) a left-side circle shifts right to the pill location.
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

        // A right-side circle also lands at the pill location.
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

        // With 3+ notifications (secondaryIsPill == false) the left circle stays on the left.
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

    @Test
    fun landscapeExpandedCardSitsBelowThePillRowAndStaysOnScreen() {
        // 10 dp base + 32 dp pill + 8 dp gap = 50 dp, plenty of room on a 411 dp tall screen.
        assertEquals(
            50f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 200f),
            0.01f
        )
        // A tall card is held back so its bottom plus one gap still fits: 411 - 340 - 8 = 63 -> 50 fits.
        assertEquals(
            50f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 340f),
            0.01f
        )
        // A taller one is clamped to 411 - 380 - 8 = 23 dp.
        assertEquals(
            23f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 380f),
            0.01f
        )
        // Never above the base offset, even when the card is taller than the screen.
        assertEquals(
            10f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 500f),
            0.01f
        )
    }

    @Test
    fun landscapeCardMovesUpForACornerPillAndDownForAMiddlePill() {
        // Card 300..700 on an 800 dp wide screen. A pill in the left corner is clear of it.
        val corner = com.agupta07505.smartisland.ui.landscapePillOverlapsCard(16f, 128f, 300f, 700f, 8f)
        assertEquals(false, corner)
        // A pill in the middle sits on the card.
        val middle = com.agupta07505.smartisland.ui.landscapePillOverlapsCard(344f, 456f, 300f, 700f, 8f)
        assertEquals(true, middle)

        // Corner: just the edge gap from the top, whatever the pill's own offset.
        assertEquals(
            10f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopDp(false, 0f, 80f, 8f, 10f, 411f, 200f),
            0.01f
        )
        // Middle: one gap under the pill (12 shift + 34 height = 46, +8 = 54).
        assertEquals(
            54f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopDp(true, 0f, 46f, 8f, 10f, 411f, 200f),
            0.01f
        )
        // Middle on a short screen is held back so the card still fits: 411 - 380 - 8 = 23.
        assertEquals(
            23f,
            com.agupta07505.smartisland.ui.landscapeExpandedTopDp(true, 0f, 46f, 8f, 10f, 411f, 380f),
            0.01f
        )
    }
}
