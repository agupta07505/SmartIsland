/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.CutoutPlacement
import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.LandscapeCameraMemory
import com.agupta07505.smartisland.data.NoCutoutPrompt
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CutoutFitTest {
    private val screens = listOf(360f, 392f, 412f)

    @Test
    fun presetCutoutFit() {
        assertNull(CutoutFit.plan(392f, null))
        assertNull(CutoutFit.plan(392f, CutoutArea(0f, 12f, 0f, 24f)))
        assertEquals(CutoutFit.FALLBACK, CutoutFit.sizeFor(null))

        for (screen in screens) {
            val centredPlan = CutoutFit.plan(screen, CutoutArea(12f, 14f, 30f, 28f))!!
            assertEquals(PresetPlace.Centre, centredPlan.place)
            assertEquals(12f, centredPlan.xOffsetDp, 0.001f)
            assertTrue(centredPlan.widthDp >= CutoutFit.CENTRE_BASE_WIDTH_DP)
            assertTrue(centredPlan.widthDp <= QuickPresets.MAX_PILL_WIDTH_DP)
        }

        val widePlan = CutoutFit.plan(392f, CutoutArea(0f, 10f, 100f, 28f))!!
        assertTrue(widePlan.widthDp >= 100f + CutoutFit.PAD_W_DP)

        for (screen in screens) {
            val left = CutoutFit.plan(screen, styledHole(SmartIslandSettings.CUTOUT_STYLE_LEFT, screen))!!
            val right = CutoutFit.plan(screen, styledHole(SmartIslandSettings.CUTOUT_STYLE_RIGHT, screen))!!
            val styledCentre = CutoutFit.plan(screen, styledHole(SmartIslandSettings.CUTOUT_STYLE_CENTER, screen))!!
            assertEquals(PresetPlace.Left, left.place)
            assertEquals(PresetPlace.Right, right.place)
            assertEquals(PresetPlace.Centre, styledCentre.place)
            assertEquals(0f, left.xOffsetDp, 0.001f)
            assertEquals(0f, right.xOffsetDp, 0.001f)
        }

        val offsetCentre = SmartIslandSettings.CUTOUT_STYLE_CENTER
        for (screen in screens) {
            assertEquals(20f, CutoutFit.plan(screen, styledHole(offsetCentre, screen, x = 20f))!!.xOffsetDp, 0.001f)
            assertEquals(PresetPlace.Left, CutoutFit.plan(screen, styledHole(offsetCentre, screen, x = -61f))!!.place)
        }
    }

    @Test fun sideOfTheHolePicksThePreset() {
        assertEquals(PresetPlace.Centre, CutoutFit.placeOf(CutoutArea(0f, 12f, 30f, 28f)))
        assertEquals(PresetPlace.Centre, CutoutFit.placeOf(CutoutArea(-60f, 12f, 30f, 28f)))
        assertEquals(PresetPlace.Left, CutoutFit.placeOf(CutoutArea(-61f, 12f, 30f, 28f)))
        assertEquals(PresetPlace.Right, CutoutFit.placeOf(CutoutArea(150f, 12f, 30f, 28f)))
    }

    @Test
    fun cutoutSizeBounds() {
        val measuredSize = CutoutFit.sizeFor(CutoutArea(0f, 14f, 30f, 28f))
        assertEquals(28f, measuredSize.heightDp, 0.001f)
        assertEquals(14f, measuredSize.yOffsetDp, 0.001f)
        assertEquals(14f, measuredSize.cornerRadiusDp, 0.001f)
        val small = CutoutFit.sizeFor(CutoutArea(0f, 14f, 14f, 16f))
        assertEquals(CutoutFit.MIN_HEIGHT_DP, small.heightDp, 0.001f)
        assertEquals(14f + 8f, small.yOffsetDp + small.heightDp / 2f, 0.001f)

        val tall = CutoutFit.sizeFor(CutoutArea(0f, 2f, 40f, 90f))
        assertEquals(CutoutFit.MAX_HEIGHT_DP, tall.heightDp, 0.001f)
        assertEquals(17f, tall.yOffsetDp, 0.001f)
        assertEquals(30f, tall.cornerRadiusDp, 0.001f)
        val nearTop = CutoutFit.sizeFor(CutoutArea(0f, 1f, 30f, 20f))
        assertEquals(CutoutFit.MIN_Y_DP, nearTop.yOffsetDp, 0.001f)
        val tiny = CutoutFit.sizeFor(CutoutArea(0f, 0f, 14f, 4f))
        assertEquals(CutoutFit.MIN_HEIGHT_DP, tiny.heightDp, 0.001f)
        assertEquals(CutoutFit.MIN_Y_DP, tiny.yOffsetDp, 0.001f)
        assertEquals(12f, tiny.cornerRadiusDp, 0.001f)

        val hole = CutoutArea(0f, 12f, 34f, 26f)
        val plan = CutoutFit.plan(392f, hole)!!
        val plannedSize = CutoutFit.sizeFor(hole)
        assertEquals(plannedSize.heightDp, plan.heightDp, 0.001f)
        assertEquals(plannedSize.yOffsetDp, plan.yOffsetDp, 0.001f)
        assertEquals(plannedSize.cornerRadiusDp, plan.cornerRadiusDp, 0.001f)
        assertNotNull(plan)

        val styledSize = CutoutFit.sizeFor(styledHole(SmartIslandSettings.CUTOUT_STYLE_CENTER, 392f))
        assertEquals(30f, styledSize.heightDp, 0.001f)
        assertEquals(12f + 15f, styledSize.yOffsetDp + styledSize.heightDp / 2f, 0.001f)
    }

    @Test
    fun sideCutoutFit() {
        for (screen in screens) {
            val left = CutoutFit.plan(screen, CutoutArea(-(screen / 2f - 24f), 14f, 30f, 28f))!!
            val right = CutoutFit.plan(screen, CutoutArea(screen / 2f - 24f, 14f, 30f, 28f))!!
            assertEquals(PresetPlace.Left, left.place)
            assertEquals(PresetPlace.Right, right.place)
            assertEquals(0f, left.xOffsetDp, 0.001f)
            assertEquals(0f, right.xOffsetDp, 0.001f)
            assertEquals(left.widthDp, right.widthDp, 0.001f)
            assertTrue(left.widthDp >= CutoutFit.SIDE_BASE_WIDTH_DP)
        }

        val settings = SmartIslandSettings(
            cameraCutoutStyle = SmartIslandSettings.CUTOUT_STYLE_TEARDROP, cameraCutoutY = 12f,
            cameraCutoutWidth = 40f, cameraCutoutHeight = 28f
        )
        for (screen in screens) {
            val teardropPlan = CutoutFit.plan(screen, CutoutPlacement.styledArea(settings, screen))!!
            assertEquals(PresetPlace.Centre, teardropPlan.place)
            assertEquals(0f, teardropPlan.yOffsetDp, 0.001f)
            assertEquals(28f, teardropPlan.heightDp, 0.001f)
        }
    }

    @Test fun firstEntryOnlyOnFreshInstall() {
        assertTrue(CutoutFit.shouldRunOnFirstEntry(welcomeShown = false, fitDone = false, notchMode = false, isPortrait = true))
        assertFalse(CutoutFit.shouldRunOnFirstEntry(true, false, false, true))
        assertFalse(CutoutFit.shouldRunOnFirstEntry(false, true, false, true))
        assertFalse(CutoutFit.shouldRunOnFirstEntry(false, false, true, true))
        assertFalse(CutoutFit.shouldRunOnFirstEntry(false, false, false, false))
    }

    @Test
    fun presetHoleMovement() {
        val leftHole = CutoutArea(-150f, 12f, 30f, 28f)
        assertFalse(CutoutFit.presetMovesHole(leftHole, PresetPlace.Left, false))
        assertTrue(CutoutFit.presetMovesHole(leftHole, PresetPlace.Centre, false))
        assertTrue(CutoutFit.presetMovesHole(leftHole, PresetPlace.Right, false))
        assertTrue(CutoutFit.presetMovesHole(leftHole, PresetPlace.Left, true))
        assertFalse(CutoutFit.presetMovesHole(null, PresetPlace.Centre, false))
        val centreHole = CutoutArea(0f, 12f, 30f, 28f)
        assertFalse(CutoutFit.presetMovesHole(centreHole, PresetPlace.Centre, false))
        assertTrue(CutoutFit.presetMovesHole(centreHole, PresetPlace.Right, false))

        val screen = 392f
        val left = styledHole(SmartIslandSettings.CUTOUT_STYLE_LEFT, screen)
        assertFalse(CutoutFit.presetMovesHole(left, PresetPlace.Left, false))
        assertTrue(CutoutFit.presetMovesHole(left, PresetPlace.Centre, false))
        assertTrue(CutoutFit.presetMovesHole(left, PresetPlace.Right, false))
    }

    private fun styledHole(style: String, screen: Float, x: Float = 0f): CutoutArea {
        val settings = SmartIslandSettings(
            cameraCutoutStyle = style, cameraCutoutX = x, cameraCutoutY = 12f,
            cameraCutoutWidth = 30f, cameraCutoutHeight = 30f
        )
        return CutoutPlacement.styledArea(settings, screen)!!
    }
}

class CutoutPlacementTest {
    private val eps = 0.001f
    private val widthDp = 400f
    private val heightDp = 900f

    private fun placed(
        style: String = SmartIslandSettings.CUTOUT_STYLE_AUTO,
        manual: Boolean = false,
        x: Float = 0f,
        notch: Boolean = false
    ) = SmartIslandSettings(
        cameraCutoutStyle = style, cameraCutoutManual = manual, cameraCutoutX = x, cameraCutoutY = 12f,
        cameraCutoutWidth = 40f, cameraCutoutHeight = 40f, enableNotchMode = notch
    )

    @Test
    fun anchorInsetFitsTheAvailableScreen() {
        assertEquals(-160f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_LEFT, widthDp), eps)
        assertEquals(160f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_RIGHT, widthDp), eps)
        assertEquals(0f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_CENTER, widthDp), eps)
        assertEquals(0f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_AUTO, widthDp), eps)
        assertEquals(-460f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_LEFT, 1000f), eps)
        assertEquals(0f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_LEFT, 60f), eps)
    }


    @Test
    fun landscapeStyleOffsets() {
        val left = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, x = 5f), widthDp)
        assertEquals(-155f, left!!.centreXDp, eps)
        assertEquals(12f, left.topDp, eps)
        val right = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_RIGHT, x = -5f), widthDp)
        assertEquals(155f, right!!.centreXDp, eps)
        val anchoredCentre = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_CENTER, x = 7f), widthDp)
        assertEquals(7f, anchoredCentre!!.centreXDp, eps)
        assertNull(CutoutPlacement.styledArea(placed(manual = true), widthDp))

        for (style in styles) {
            val mirroredHole = CutoutPlacement.styledArea(placed(style, x = 9f), widthDp)!!
            val turned = CutoutPlacement.landscapeCamera(mirroredHole, widthDp, heightDp, reversed = false)
            val mirroredReversed = CutoutPlacement.landscapeCamera(mirroredHole, widthDp, heightDp, reversed = true)
            assertEquals(-turned.xOffsetDp, mirroredReversed.xOffsetDp, eps)
            assertEquals(widthDp, turned.centerYDp + mirroredReversed.centerYDp, eps)
            assertEquals(turned.widthDp, mirroredReversed.widthDp, eps)
            assertEquals(turned.lengthDp, mirroredReversed.lengthDp, eps)
        }

        for (style in styles) {
            val fixedSideHole = CutoutPlacement.styledArea(placed(style), widthDp)!!
            assertTrue(CutoutPlacement.landscapeCamera(fixedSideHole, widthDp, heightDp, reversed = false).xOffsetDp < 0f)
            assertTrue(CutoutPlacement.landscapeCamera(fixedSideHole, widthDp, heightDp, reversed = true).xOffsetDp > 0f)
        }

        val teardropHole = CutoutPlacement.styledArea(teardrop(), widthDp)!!
        assertEquals(0f, teardropHole.topDp, eps)
        assertEquals(40f, teardropHole.widthDp, eps)
        assertEquals(24f, teardropHole.heightDp, eps)
        val teardropCentre = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_CENTER), widthDp)!!
        assertEquals(12f, teardropCentre.topDp, eps)
        assertTrue(CutoutPlacement.isFlushWithTop(SmartIslandSettings.CUTOUT_STYLE_TEARDROP))
        assertFalse(CutoutPlacement.isFlushWithTop(SmartIslandSettings.CUTOUT_STYLE_CENTER))
        assertFalse(CutoutPlacement.isFlushWithTop(SmartIslandSettings.CUTOUT_STYLE_AUTO))
    }

    @Test
    fun cutoutAreaPlacement() {
        val manualArea = CutoutPlacement.area(placed(manual = true, x = -30f), widthDp)
        assertEquals(-30f, manualArea!!.centreXDp, eps)
        assertEquals(40f, manualArea.widthDp, eps)

        val styledArea = CutoutPlacement.area(placed(SmartIslandSettings.CUTOUT_STYLE_RIGHT, manual = true, x = 0f), widthDp)
        assertEquals(160f, styledArea!!.centreXDp, eps)

        assertNull(CutoutPlacement.area(placed(), widthDp))
        assertFalse(CutoutPlacement.isPlaced(placed()))
        assertNull(CutoutPlacement.area(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, notch = true), widthDp))
        assertFalse(CutoutPlacement.isPlaced(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, notch = true)))
        assertTrue(CutoutPlacement.isPlaced(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT)))
        assertTrue(CutoutPlacement.isPlaced(placed(manual = true)))

        val centredManual = CutoutPlacement.area(placed(manual = true, x = 11f), widthDp)!!
        val centre = CutoutPlacement.area(placed(SmartIslandSettings.CUTOUT_STYLE_CENTER, x = 11f), widthDp)!!
        assertEquals(centredManual, centre)

        assertTrue(CutoutPlacement.isPlaced(teardrop()))
        assertEquals(0f, CutoutPlacement.area(teardrop(), widthDp)!!.topDp, eps)
        assertFalse(CutoutPlacement.isPlaced(teardrop(notch = true)))
        assertNull(CutoutPlacement.area(teardrop(notch = true), widthDp))
        // the notch look still reads the style for its pulse hole
        assertEquals(0f, CutoutPlacement.styledArea(teardrop(notch = true), widthDp)!!.topDp, eps)
    }

    @Test fun centreCameraSitsOnTheMiddleOfTheSideEdgeInLandscape() {
        val hole = CutoutArea(0f, 12f, 40f, 40f)
        val turned = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = false)
        assertEquals(-418f, turned.xOffsetDp, eps)
        assertEquals(200f, turned.centerYDp, eps)
        assertEquals(40f, turned.widthDp, eps)
        assertEquals(40f, turned.lengthDp, eps)
        val reversed = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = true)
        assertEquals(418f, reversed.xOffsetDp, eps)
        assertEquals(200f, reversed.centerYDp, eps)
    }

    @Test fun cornerCameraMovesAlongTheSideEdgeWithTheTurn() {
        val hole = CutoutArea(-160f, 12f, 40f, 40f)
        val turned = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = false)
        assertEquals(-418f, turned.xOffsetDp, eps)
        assertEquals(360f, turned.centerYDp, eps)
        val reversed = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = true)
        assertEquals(418f, reversed.xOffsetDp, eps)
        assertEquals(40f, reversed.centerYDp, eps)
    }

    @Test fun landscapeSizeSwapsWithTheTurn() {
        val turned = CutoutPlacement.landscapeCamera(CutoutArea(0f, 12f, 50f, 30f), widthDp, heightDp, reversed = false)
        assertEquals(30f, turned.widthDp, eps)
        assertEquals(50f, turned.lengthDp, eps)
    }

    private val styles = listOf(
        SmartIslandSettings.CUTOUT_STYLE_LEFT,
        SmartIslandSettings.CUTOUT_STYLE_CENTER,
        SmartIslandSettings.CUTOUT_STYLE_RIGHT,
        SmartIslandSettings.CUTOUT_STYLE_TEARDROP
    )

    @Test fun landscapeAlongTheEdgeFollowsTheHolesXInBothTurns() {
        // turned 90: centreY = W/2 - x; turned 270: centreY = W/2 + x
        for (style in styles) for (x in listOf(-20f, 0f, 15f)) {
            val hole = CutoutPlacement.styledArea(placed(style, x = x), widthDp)!!
            val turned = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = false)
            val reversed = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = true)
            assertEquals(widthDp / 2f - hole.centreXDp, turned.centerYDp, eps)
            assertEquals(widthDp / 2f + hole.centreXDp, reversed.centerYDp, eps)
        }
    }

    @Test
    fun styledHoleAnchors() {
        val left = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT), widthDp)!!
        val right = CutoutPlacement.styledArea(placed(SmartIslandSettings.CUTOUT_STYLE_RIGHT), widthDp)!!
        assertEquals(
            CutoutPlacement.landscapeCamera(left, widthDp, heightDp, reversed = false).centerYDp,
            CutoutPlacement.landscapeCamera(right, widthDp, heightDp, reversed = true).centerYDp, eps
        )
        assertEquals(
            CutoutPlacement.landscapeCamera(left, widthDp, heightDp, reversed = true).centerYDp,
            CutoutPlacement.landscapeCamera(right, widthDp, heightDp, reversed = false).centerYDp, eps
        )

        val hole = CutoutPlacement.styledArea(teardrop(), widthDp)!!
        val turned = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = false)
        assertEquals(-438f, turned.xOffsetDp, eps)
        assertEquals(200f, turned.centerYDp, eps)
        assertEquals(24f, turned.widthDp, eps)
        assertEquals(40f, turned.lengthDp, eps)
        val landscapeReversed = CutoutPlacement.landscapeCamera(hole, widthDp, heightDp, reversed = true)
        assertEquals(438f, landscapeReversed.xOffsetDp, eps)
        assertEquals(200f, landscapeReversed.centerYDp, eps)
    }

    @Test fun portraitDepthToLandscapeInset() {
        val shallow = CutoutPlacement.landscapeCamera(CutoutArea(0f, 12f, 40f, 40f), widthDp, heightDp, reversed = false)
        val deep = CutoutPlacement.landscapeCamera(CutoutArea(0f, 20f, 40f, 40f), widthDp, heightDp, reversed = false)
        assertEquals(-410f, deep.xOffsetDp, eps)
        assertEquals(8f, deep.xOffsetDp - shallow.xOffsetDp, eps)
        val deepReversed = CutoutPlacement.landscapeCamera(CutoutArea(0f, 20f, 40f, 40f), widthDp, heightDp, reversed = true)
        assertEquals(410f, deepReversed.xOffsetDp, eps)
    }

    @Test
    fun cutoutStylePriority() {
        val portraitGuess = CutoutPlacement.centreGuess(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, x = 30f))
        assertEquals(0f, portraitGuess.centreXDp, eps)
        assertEquals(12f, portraitGuess.topDp, eps)
        assertEquals(40f, portraitGuess.widthDp, eps)
        assertEquals(40f, portraitGuess.heightDp, eps)

        val landscapeGuess = CutoutPlacement.centreGuess(placed())
        val turned = CutoutPlacement.landscapeCamera(landscapeGuess, widthDp, heightDp, reversed = false)
        val reversed = CutoutPlacement.landscapeCamera(landscapeGuess, widthDp, heightDp, reversed = true)
        assertEquals(widthDp / 2f, turned.centerYDp, eps)
        assertEquals(widthDp / 2f, reversed.centerYDp, eps)
        assertTrue(turned.xOffsetDp < 0f)
        assertTrue(reversed.xOffsetDp > 0f)

        val hole = CutoutPlacement.placedFor(placed(manual = true, x = 10f), widthDp)
        assertEquals(10f, hole!!.centreXDp, eps)
        assertEquals(40f, hole.widthDp, eps)
        assertEquals(40f, hole.heightDp, eps)

        val styled = CutoutPlacement.placedFor(
            placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, manual = true, x = 5f), widthDp
        )
        assertEquals(-155f, styled!!.centreXDp, eps)
        assertNull(CutoutPlacement.placedFor(placed(), widthDp))

        assertNull(CutoutPlacement.placedFor(placed(manual = true, notch = true), widthDp))
        assertEquals(
            -160f,
            CutoutPlacement.placedFor(placed(SmartIslandSettings.CUTOUT_STYLE_LEFT, notch = true), widthDp)!!.centreXDp,
            eps
        )
    }

    @Test fun radiusRangeIsHalfTheDiameterLimits() {
        assertEquals(6f, CutoutPlacement.RADIUS_RANGE_DP.start, eps)
        assertEquals(30f, CutoutPlacement.RADIUS_RANGE_DP.endInclusive, eps)
    }

    @Test
    fun horizontalOffsetRange() {
        val centredRange = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_CENTER, widthDp, 20f)
        assertEquals(-180f, centredRange.start, eps)
        assertEquals(180f, centredRange.endInclusive, eps)

        // left anchor -160: the hole's edge may reach the screen edge at 20 dp radius, so X goes from -20 to 340 (capped)
        val left = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_LEFT, widthDp, 20f)
        assertEquals(-20f, left.start, eps)
        assertEquals(300f, left.endInclusive, eps)
        val right = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_RIGHT, widthDp, 20f)
        assertEquals(-300f, right.start, eps)
        assertEquals(20f, right.endInclusive, eps)

        for (style in styles) for (screen in listOf(320f, 392f, 600f)) for (r in listOf(6f, 18f, 30f)) {
            assertTrue(0f in CutoutPlacement.xRangeDp(style, screen, r))
        }

        assertEquals(0f, CutoutPlacement.anchorXDp(SmartIslandSettings.CUTOUT_STYLE_TEARDROP, widthDp), eps)
        assertEquals(7f, CutoutPlacement.styledArea(teardrop(x = 7f), widthDp)!!.centreXDp, eps)
        val teardropRange = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_TEARDROP, widthDp, 20f)
        assertEquals(-180f, teardropRange.start, eps)
        assertEquals(180f, teardropRange.endInclusive, eps)

        val small = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_CENTER, widthDp, 6f)
        val big = CutoutPlacement.xRangeDp(SmartIslandSettings.CUTOUT_STYLE_CENTER, widthDp, 30f)
        assertTrue(big.endInclusive < small.endInclusive)
        assertTrue(big.start > small.start)
    }

    private fun teardrop(x: Float = 0f, notch: Boolean = false) =
        placed(SmartIslandSettings.CUTOUT_STYLE_TEARDROP, x = x, notch = notch)
            .copy(cameraCutoutWidth = 40f, cameraCutoutHeight = 24f)
}

class CutoutTapIntentTest {
    private fun phase(expanded: Boolean, p: Float, swap: Boolean = false) = CutoutTapIntent.phase(expanded, p, swap)

    @Test
    fun swapPhasePriority() {
        assertEquals(IslandPhase.Collapsed, phase(false, 0f))
        assertEquals(IslandPhase.Expanding, phase(true, 0f))
        assertEquals(IslandPhase.Expanding, phase(true, 0.5f))
        assertEquals(IslandPhase.Expanded, phase(true, 1f))
        assertEquals(IslandPhase.Collapsing, phase(false, 0.6f))
        assertEquals(IslandPhase.Collapsed, phase(false, CutoutTapIntent.REST_PROGRESS))
        assertEquals(IslandPhase.Expanded, phase(true, CutoutTapIntent.SETTLED_PROGRESS))

        assertEquals(IslandPhase.Swapping, phase(true, 0.3f, swap = true))
        assertEquals(IslandPhase.Swapping, phase(true, 1f, swap = true))
        assertEquals(IslandPhase.Collapsed, phase(false, 0f, swap = true))
    }

    @Test
    fun cutoutTapRouting() {
        for (zone in TapZone.values()) {
            assertEquals(TapIntent.CloseIsland, CutoutTapIntent.resolve(IslandPhase.Expanding, zone, true))
        }

        for (zone in TapZone.values()) {
            assertEquals(TapIntent.OpenIsland, CutoutTapIntent.resolve(IslandPhase.Collapsing, zone, true))
            assertEquals(TapIntent.None, CutoutTapIntent.resolve(IslandPhase.Collapsing, zone, false))
        }

        assertEquals(TapIntent.OpenApp, CutoutTapIntent.resolve(IslandPhase.Expanded, TapZone.Card, true))
        assertEquals(TapIntent.OpenIsland, CutoutTapIntent.resolve(IslandPhase.Collapsed, TapZone.Card, true))
        assertEquals(TapIntent.None, CutoutTapIntent.resolve(IslandPhase.Collapsed, TapZone.Card, false))

        for (zone in TapZone.values()) {
            assertEquals(TapIntent.SwapFlip, CutoutTapIntent.resolve(IslandPhase.Swapping, zone, true))
        }

        val random = Random(7)
        repeat(2000) {
            val expanded = random.nextBoolean()
            val progress = random.nextFloat()
            val swap = random.nextBoolean()
            val settledZone = TapZone.values()[random.nextInt(TapZone.values().size)]
            val p = CutoutTapIntent.phase(expanded, progress, swap)
            val intent = CutoutTapIntent.resolve(p, settledZone, random.nextBoolean())
            if (intent == TapIntent.OpenApp) {
                assertEquals(IslandPhase.Expanded, p)
                assertTrue(expanded && progress >= CutoutTapIntent.SETTLED_PROGRESS && !swap)
            }
        }
    }

    @Test
    fun cameraBandPutsCardsAwayOnlyAtRest() {
        assertEquals(TapIntent.PutAway, CutoutTapIntent.resolve(IslandPhase.Expanded, TapZone.CameraBand, true))
        assertEquals(TapIntent.PutAway, CutoutTapIntent.resolve(IslandPhase.Collapsed, TapZone.CameraBand, true))
        assertNotEquals(TapIntent.PutAway, CutoutTapIntent.resolve(IslandPhase.Expanding, TapZone.CameraBand, true))
        assertNotEquals(TapIntent.PutAway, CutoutTapIntent.resolve(IslandPhase.Collapsing, TapZone.CameraBand, true))
    }

    @Test
    fun turnAroundAimsForTheOppositeOfWhereTheIslandWasGoing() {
        assertEquals(false, CutoutTapIntent.targetExpanded(TapIntent.CloseIsland))
        assertEquals(true, CutoutTapIntent.targetExpanded(TapIntent.OpenIsland))
        assertNull(CutoutTapIntent.targetExpanded(TapIntent.OpenApp))
        assertNull(CutoutTapIntent.targetExpanded(TapIntent.PutAway))
        assertNull(CutoutTapIntent.targetExpanded(TapIntent.SwapFlip))
    }

    @Test
    fun shellTouchReversalBounds() {
        assertTrue(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Collapsing, 0.9f, 500f, 40f))
        assertTrue(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Expanding, 0.2f, 500f, 40f))
        assertTrue(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Expanding, 0.8f, 30f, 40f))
        assertFalse(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Expanding, 0.8f, 300f, 40f))
        assertFalse(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Expanded, 1f, 10f, 40f))
        assertFalse(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Collapsed, 0f, 10f, 40f))
        assertFalse(CutoutTapIntent.reversesOnShellTouch(IslandPhase.Swapping, 0.5f, 10f, 40f))
    }

    @Test
    fun memoryKeepsOnlyTheNewestTapAndGivesItOnce() {
        val memory = TapMemory()
        assertFalse(memory.isHeld)
        assertNull(memory.take())
        memory.remember("a")
        memory.remember("b")
        assertTrue(memory.isHeld)
        assertEquals("b", memory.take())
        assertFalse(memory.isHeld)
        assertNull(memory.take())
        memory.remember("c")
        memory.clear()
        assertNull(memory.take())
    }
}

class LandscapeCameraMemoryTest {
    private val camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 180f)
    private val withCamera = OrientationState(
        isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f, camera = camera
    )
    private val without = withCamera.copy(camera = null)

    @Test fun earlyReadTakesLastCamera() {
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera, settled = true)
        assertEquals(camera, memory.resolve(without, settled = false).camera)
    }

    @Test fun settledReadWithoutACameraIsTheTruth() {
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera, settled = true)
        assertNull(memory.resolve(without, settled = true).camera)
        assertNull(memory.resolve(without, settled = false).camera)
    }

    @Test
    fun cameraMemoryFallback() {
        assertEquals(without, LandscapeCameraMemory().resolve(without, settled = false))

        val portrait = OrientationState(isLandscape = false, screenWidthDp = 360f)
        assertEquals(portrait, LandscapeCameraMemory().resolve(portrait, settled = false))
    }

    @Test fun otherRotationOrScreenSizeDoesNotShareACamera() {
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera, settled = true)
        assertNull(memory.resolve(without.copy(reversed = true), settled = false).camera)
        assertNull(memory.resolve(without.copy(screenWidthDp = 900f), settled = false).camera)
    }

    @Test fun aNewSettledCameraReplacesTheRememberedOne() {
        // a style or offset change gives a new camera for the same rotation and screen
        val moved = LandscapeCamera(xOffsetDp = -350f, centerYDp = 300f)
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera, settled = true)
        assertEquals(moved, memory.resolve(withCamera.copy(camera = moved), settled = true).camera)
        assertEquals(moved, memory.resolve(without, settled = false).camera)
    }

    @Test fun earlyRotationReading() {
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera.copy(reversed = true), settled = true)
        assertEquals(camera, memory.resolve(without.copy(reversed = true), settled = false).camera)
        assertNull(memory.resolve(without, settled = false).camera)
    }

    @Test fun removingTheStyleOnADeviceWithNoCameraDropsTheCamera() {
        val memory = LandscapeCameraMemory()
        memory.resolve(withCamera, settled = true)
        memory.resolve(without, settled = true)
        assertNull(memory.resolve(without, settled = false).camera)
    }
}

class NoCutoutPromptTest {
    private val auto = SmartIslandSettings.CUTOUT_STYLE_AUTO

    private fun show(
        detected: Boolean = false,
        style: String = auto,
        manual: Boolean = false,
        dismissed: Boolean = false,
        portrait: Boolean = true
    ) = NoCutoutPrompt.shouldShow(detected, style, manual, dismissed, portrait)

    @Test
    fun noCutoutPromptEligibility() {
        assertTrue(show())
        assertFalse(show(detected = true))
        assertFalse(show(dismissed = true))
        assertFalse(show(portrait = false))

        listOf(
            SmartIslandSettings.CUTOUT_STYLE_LEFT,
            SmartIslandSettings.CUTOUT_STYLE_CENTER,
            SmartIslandSettings.CUTOUT_STYLE_RIGHT,
            SmartIslandSettings.CUTOUT_STYLE_TEARDROP
        ).forEach { assertFalse(show(style = it)) }
        assertFalse(show(manual = true))
    }


    @Test fun theRowIsInsideTheCardThatItOpens() {
        assertTrue(NoCutoutPrompt.ROW_KEY.startsWith(NoCutoutPrompt.CARD_KEY_PREFIX))
    }
}
