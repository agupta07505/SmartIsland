/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.CompactNotificationShape
import com.agupta07505.smartisland.ui.calculateExpandedTopOffset
import com.agupta07505.smartisland.ui.calculateExpandedWidth
import com.agupta07505.smartisland.ui.calculateSecondaryExpandedOffset
import com.agupta07505.smartisland.ui.collapsedTopShiftDp
import com.agupta07505.smartisland.ui.compactNotificationShapes
import com.agupta07505.smartisland.ui.expanded.siblingPickerHeight
import com.agupta07505.smartisland.ui.landscapeExpandedTopDp
import com.agupta07505.smartisland.ui.landscapeExpandedTopOffsetDp
import com.agupta07505.smartisland.ui.landscapePillOverlapsCard
import com.agupta07505.smartisland.ui.singleCardTopBelowCutoutDp
import com.agupta07505.smartisland.ui.splitCardMaxWidthDp
import com.agupta07505.smartisland.ui.splitPickerLeftFraction
import com.agupta07505.smartisland.ui.splitPickerWidthDp
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class IslandOverlayLayoutTest {

    @Test
    fun compactIndicatorMatrix() {
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
            listOf(CompactNotificationShape.MiniPill),
            compactNotificationShapes(notificationCount = 3, expanded = true)
        )
    }

    @Test
    fun splitCircleFitsWindow() {
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
                    Assert.assertTrue(
                        "Left circle must not collapse into pill, gap ($gap) >= compactGap ($compactGapPx)",
                        gap >= compactGapPx - 0.01f
                    )
                } else {
                    val gap = circleLeftPx - (mainLeftPx + mainWidthPx)
                    Assert.assertTrue(
                        "Right circle must not collapse into pill, gap ($gap) >= compactGap ($compactGapPx)",
                        gap >= compactGapPx - 0.01f
                    )
                }

                Assert.assertTrue(
                    "Circle left ($circleLeftPx) must be >= window left ($windowLeftPx)",
                    circleLeftPx >= windowLeftPx - 0.01f
                )
                Assert.assertTrue(
                    "Circle right ($circleRightPx) must be <= window right ($windowRightPx)",
                    circleRightPx <= windowRightPx + 0.01f
                )

                Assert.assertTrue(circleLeftPx >= edgePaddingPx - 0.01f)
                Assert.assertTrue(circleRightPx <= screenWidthPx - edgePaddingPx + 0.01f)
                Assert.assertTrue(mainLeftPx >= edgePaddingPx - 0.01f)
                Assert.assertTrue(mainLeftPx + mainWidthPx <= screenWidthPx - edgePaddingPx + 0.01f)
            }
        }
    }

    @Test
    fun landscapeExpandedWidth() {
        val landscapeScreenWidth = 914f
        val landscapeScreenHeight = 411f

        val landscapeExpandedWidth = calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = landscapeScreenWidth,
            screenHeightDp = landscapeScreenHeight
        )
        // half of the screen width (the heads-up-notification strip), never 95% of it
        assertEquals(457f, landscapeExpandedWidth, 0.01f)
        Assert.assertTrue(landscapeExpandedWidth < landscapeScreenWidth * 0.95f)

        // clamping bounds for tablets / ultra-wides and small screens
        val tabletLandscapeWidth = calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 1280f,
            screenHeightDp = 800f
        )
        assertEquals(560f, tabletLandscapeWidth, 0.01f)

        val smallLandscapeWidth = calculateExpandedWidth(
            isLandscape = true,
            screenWidthDp = 640f,
            screenHeightDp = 320f
        )
        assertEquals(340f, smallLandscapeWidth, 0.01f)

        // portrait is untouched
        assertEquals(
            411f * 0.95f,
            calculateExpandedWidth(false, 411f, 914f),
            0.01f
        )
    }

    @Test
    fun landscapeSplitCardGapPicker() {
        fun w(sw: Float, card: Float?, picker: Boolean = true, landscape: Boolean = true) =
            calculateExpandedWidth(
                landscape, sw, 411f, pickerSideBySide = picker, splitCardWidthDp = card
            )
        // 914 dp wide: picker 30% = 274 (inside 240..300), gap 14, so a measured 260 dp card -> 548
        assertEquals(274.2f, splitPickerWidthDp(914f), 0.01f)
        assertEquals(548.2f, w(914f, 260f), 0.01f)
        // not measured yet: the widest the card may get = 62% of the screen minus gap and picker
        assertEquals(278.5f, splitCardMaxWidthDp(914f), 0.1f)
        assertEquals(914f * 0.62f, w(914f, null), 0.1f)
        // A tiny card is held at the minimum card width, a huge one at the maximum
        assertEquals(240f + 14f + 274.2f, w(914f, 80f), 0.01f)
        assertEquals(278.5f + 14f + 274.2f, w(914f, 900f), 0.2f)
        // tablet: picker capped at 300; the group stays about 62% of the screen
        assertEquals(300f, splitPickerWidthDp(1280f), 0.01f)
        assertEquals(1280f * 0.62f, w(1280f, null), 0.1f)
        // small screen: never wider than the screen minus the margin
        assertEquals(484f, w(500f, null), 0.01f)
        // picker closed, and portrait, are unchanged
        assertEquals(457f, w(914f, 260f, picker = false), 0.01f)
        assertEquals(411f * 0.95f, w(411f, 260f, landscape = false), 0.01f)
    }

    @Test
    fun landscapeSplitThirdOfScreen() {
        // 914 x 411 dp screen, 3 picker rows (compact picker height 200 dp)
        val width = calculateExpandedWidth(true, 914f, 411f, pickerSideBySide = true)
        val height = siblingPickerHeight(3, compact = true).value
        val share = width * height / (914f * 411f)
        Assert.assertTrue("share $share", share in 0.26f..0.34f)
    }

    @Test
    fun landscapeExpandedSitsHigher() {
        fun top(sb: Float, landscape: Boolean, companion: Boolean = false) =
            calculateExpandedTopOffset(
                enableNotchMode = false, hasCompanion = companion, statusBarHeightDp = sb, isLandscape = landscape
            )
        assertEquals(42f, top(24f, landscape = false), 0.01f)
        assertEquals(48f, top(48f, landscape = false), 0.01f)
        assertEquals(10f, top(24f, landscape = true), 0.01f)
        assertEquals(0f, top(6f, landscape = true), 0.01f)
    }

    @Test
    fun pickerStartsAtRightPart() {
        // 548 dp window, 274 dp picker: it starts at about 50% of the width
        assertEquals(0.5f, splitPickerLeftFraction(548f, 914f), 0.01f)
        // unknown width falls back to 50 %
        assertEquals(0.5f, splitPickerLeftFraction(0f, 914f), 0.0001f)
    }

    @Test
    fun landscapeCutoutOffset() {
        // landscape: the window starts at the top, the cutouts are lowered by yOffset
        assertEquals(205f, collapsedTopShiftDp(true, false, 205f), 0.01f)
        // portrait and notch mode: the window itself is offset, nothing is lowered inside it
        assertEquals(0f, collapsedTopShiftDp(false, false, 12f), 0.01f)
        assertEquals(0f, collapsedTopShiftDp(true, true, 12f), 0.01f)
        assertEquals(0f, collapsedTopShiftDp(true, false, -4f), 0.01f)
    }

    @Test
    fun notchExpandedClearance() {
        val notchHeight = 35f
        val statusBarHeightStandard = 24f
        val statusBarHeightTall = 40f

        // notch mode: must clear both the hardware notch height and status bar height with a safe gap
        val offsetStandard = calculateExpandedTopOffset(
            enableNotchMode = true,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightStandard,
            notchHeightDp = notchHeight
        )
        Assert.assertTrue(
            "Expanded offset in notch mode ($offsetStandard) must strictly exceed notch height ($notchHeight)",
            offsetStandard >= notchHeight + 8f
        )
        Assert.assertTrue(
            "Expanded offset in notch mode ($offsetStandard) must strictly exceed status bar ($statusBarHeightStandard)",
            offsetStandard >= statusBarHeightStandard + 8f
        )
        assertEquals(43f, offsetStandard, 0.01f)

        // tall notch device (e.g., Pixel 3 XL or iPhone deep notch)
        val offsetTall = calculateExpandedTopOffset(
            enableNotchMode = true,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightTall,
            notchHeightDp = notchHeight
        )
        Assert.assertTrue(
            "Expanded offset on tall status bar ($offsetTall) must clear tall status bar ($statusBarHeightTall)",
            offsetTall >= statusBarHeightTall + 8f
        )
        assertEquals(48f, offsetTall, 0.01f)

        // non-notch mode preserves standard status bar positioning
        val normalOffset = calculateExpandedTopOffset(
            enableNotchMode = false,
            hasCompanion = false,
            statusBarHeightDp = statusBarHeightStandard
        )
        assertEquals(42f, normalOffset, 0.01f)

        val companionOffset = calculateExpandedTopOffset(
            enableNotchMode = false,
            hasCompanion = true,
            statusBarHeightDp = statusBarHeightStandard,
            circleSizeDp = 34f,
            compactGapDp = 8f
        )
        assertEquals(42f, companionOffset, 0.01f)
    }

    @Test
    fun loneCardBelowCutout() {
        assertEquals(40f, singleCardTopBelowCutoutDp(48f, 12f), 0.01f)
        assertEquals(0f, singleCardTopBelowCutoutDp(6f, 20f), 0.01f)
    }

    @Test
    fun leftCircleBubbleOffset() {
        val screenCenter = 200f
        val expandedCompactX = 144f // main pill start
        val miniPillWidth = 112f
        val circleSize = 34f
        val compactGap = 8f

        val pillCenterOffset = (expandedCompactX + miniPillWidth / 2f) - screenCenter

        // collapsed left-side circle center offset: circle is to the LEFT of pill
        val collapsedLeftCircleOffset = (expandedCompactX - compactGap - circleSize / 2f) - screenCenter

        // with only 2 notifications (secondaryIsPill == true) a left-side circle shifts right to the pill location
        val expandedOffsetLeftMode = calculateSecondaryExpandedOffset(
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
        Assert.assertTrue(
            "Left circle must shift to the right into pill position ($expandedOffsetLeftMode > $collapsedLeftCircleOffset)",
            expandedOffsetLeftMode > collapsedLeftCircleOffset
        )

        // A right-side circle also lands at the pill location
        val expandedOffsetRightMode = calculateSecondaryExpandedOffset(
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

        // with 3+ notifications (secondaryIsPill == false) the left circle stays on the left
        val threeNotifLeftOffset = calculateSecondaryExpandedOffset(
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
    fun landscapeExpandedCardBounds() {
        // 10 dp base + 32 dp pill + 8 dp gap = 50 dp, plenty of room on a 411 dp tall screen
        assertEquals(
            50f,
            landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 200f),
            0.01f
        )
        // A tall card is held back so its bottom plus 1 gap still fits: 411 - 340 - 8 = 63 -> 50 fits
        assertEquals(
            50f,
            landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 340f),
            0.01f
        )
        // A taller one is clamped to 411 - 380 - 8 = 23 dp
        assertEquals(
            23f,
            landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 380f),
            0.01f
        )
        // never above the base offset, even when the card is taller than the screen
        assertEquals(
            10f,
            landscapeExpandedTopOffsetDp(10f, 32f, 8f, 411f, 500f),
            0.01f
        )
    }

    @Test
    fun landscapeCardOffset() {
        // card 300..700 on an 800 dp wide screen. A pill in the left corner is clear of it
        val corner = landscapePillOverlapsCard(16f, 128f, 300f, 700f, 8f)
        assertEquals(false, corner)
        // A pill in the middle sits on the card
        val middle = landscapePillOverlapsCard(344f, 456f, 300f, 700f, 8f)
        assertEquals(true, middle)

        // corner: just the edge gap from the top, whatever the pill's own offset
        assertEquals(
            10f,
            landscapeExpandedTopDp(false, 0f, 80f, 8f, 10f, 411f, 200f),
            0.01f
        )
        // middle: one gap under the pill (12 shift + 34 height = 46, +8 = 54)
        assertEquals(
            54f,
            landscapeExpandedTopDp(true, 0f, 46f, 8f, 10f, 411f, 200f),
            0.01f
        )
        // middle on a short screen is held back so the card still fits: 411 - 380 - 8 = 23
        assertEquals(
            23f,
            landscapeExpandedTopDp(true, 0f, 46f, 8f, 10f, 411f, 380f),
            0.01f
        )
    }
}
