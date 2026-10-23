/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationProfileTest {
    private val base = SmartIslandSettings(
        width = 120f,
        disabledEvents = setOf("music"),
        disabledEventsLandscape = setOf("call"),
        enableNotchMode = true
    )

    @Test
    fun portraitIsReturnedUnchanged() {
        assertEquals(base, base.forOrientation(OrientationState(isLandscape = false)))
    }

    @Test
    fun landscapeUsesItsOwnEventSwitchesAndDropsNotchMode() {
        val out = base.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f))
        assertEquals(setOf("call"), out.disabledEvents)
        assertFalse(out.enableNotchMode)
    }

    @Test
    fun cameraModeSitsOnTheReportedNotch() {
        val s = base.copy(landscapeCircleSize = 26f)
        val state = OrientationState(
            isLandscape = true,
            screenWidthDp = 800f,
            camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 20f)
        )
        val out = s.forOrientation(state)
        assertEquals(-350f, out.xOffset, 0.01f)
        assertEquals(7f, out.yOffset, 0.01f)
        assertEquals(26f, out.width, 0.01f)
        assertEquals(13f, out.cornerRadius, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, out.circlePosition)
    }

    @Test
    fun cameraModeKeepsTheNotchHeightFarDownTheEdge() {
        val s = base.copy(landscapeCircleSize = 26f)
        val out = s.forOrientation(
            OrientationState(
                isLandscape = true, screenWidthDp = 800f,
                camera = LandscapeCamera(xOffsetDp = 380f, centerYDp = 200f)
            )
        )
        assertEquals(380f, out.xOffset, 0.01f)
        assertEquals(187f, out.yOffset, 0.01f)
        assertTrue(out.disabledEvents.contains("call"))
    }

    @Test
    fun fineTuneMovesTheIdlePillFromWhereverItSits() {
        val s = base.copy(
            landscapeCircleSize = 26f,
            landscapeCircleXAdjust = 5f,
            landscapeCircleYAdjust = -10f
        )
        val out = s.forOrientation(
            OrientationState(
                isLandscape = true, screenWidthDp = 800f,
                camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 100f)
            )
        )
        assertEquals(-345f, out.xOffset, 0.01f)
        assertEquals(77f, out.yOffset, 0.01f)
        // Never above the top edge.
        val high = s.copy(landscapeCircleYAdjust = -200f).forOrientation(
            OrientationState(isLandscape = true, screenWidthDp = 800f, camera = LandscapeCamera(-350f, 20f))
        )
        assertEquals(0f, high.yOffset, 0.01f)
    }

    @Test
    fun forcedRightPlacesCircleOnRightEdgeWithInset() {
        val s = base.copy(
            landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_RIGHT,
            landscapeCircleSize = 20f,
            landscapeCircleEdgeInset = 10f
        )
        val out = s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f))
        assertEquals(380f, out.xOffset, 0.01f)
        assertEquals(s.landscapeCircleYOffset, out.yOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, out.circlePosition)
    }

    @Test
    fun forcedLeftMirrorsAndTopMiddleIsCentred() {
        val left = base.copy(
            landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
            landscapeCircleSize = 20f, landscapeCircleEdgeInset = 10f
        ).forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f))
        assertEquals(-380f, left.xOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, left.circlePosition)

        val middle = base.copy(
            landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            circlePosition = SmartIslandSettings.CIRCLE_POSITION_LEFT
        ).forOrientation(
            OrientationState(isLandscape = true, screenWidthDp = 800f, camera = LandscapeCamera(380f, 200f))
        )
        assertEquals(0f, middle.xOffset, 0.01f)
        assertEquals(base.landscapeCircleYOffset, middle.yOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, middle.circlePosition)
    }

    @Test
    fun landscapeSettingsSurviveJsonRoundTrip() {
        val s = base.copy(
            landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeCircleSize = 30f,
            landscapeCircleXAdjust = 12f,
            landscapeCircleYAdjust = -4f
        )
        val back = SmartIslandSettings.fromJson(s.toJson())
        assertEquals(s.landscapeCirclePosition, back.landscapeCirclePosition)
        assertEquals(30f, back.landscapeCircleSize, 0.01f)
        assertEquals(12f, back.landscapeCircleXAdjust, 0.01f)
        assertEquals(-4f, back.landscapeCircleYAdjust, 0.01f)
        assertEquals(setOf("call"), back.disabledEventsLandscape)
    }

    private val portrait = SmartIslandSettings(width = 112f, height = 34f, cornerRadius = 22f)
    private val landscapeState = OrientationState(
        isLandscape = true,
        screenWidthDp = 800f,
        camera = LandscapeCamera(xOffsetDp = -380f, centerYDp = 200f)
    )

    @Test
    fun contentPillUsesTheLandscapeSizeOffsetsAndRadius() {
        val s = portrait.copy(
            landscapeWidth = 150f, landscapeHeight = 40f,
            landscapeXOffset = -120f, landscapeYOffset = 30f,
            landscapeCornerRadius = 18f, landscapeOpacity = 0.8f
        )
        val out = s.forOrientation(landscapeState, hasContent = true)
        assertEquals(150f, out.width, 0.01f)
        assertEquals(40f, out.height, 0.01f)
        assertEquals(-120f, out.xOffset, 0.01f)
        assertEquals(30f, out.yOffset, 0.01f)
        assertEquals(18f, out.cornerRadius, 0.01f)
        assertEquals(0.8f, out.opacity, 0.001f)
        assertFalse(out.enableNotchMode)
    }

    @Test
    fun contentPillDefaultsToTopMiddleAndPicksTheCompanionSideFromItsPosition() {
        val centred = portrait.copy(circlePosition = SmartIslandSettings.CIRCLE_POSITION_LEFT)
            .forOrientation(landscapeState, hasContent = true)
        assertEquals(0f, centred.xOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, centred.circlePosition)

        val leftEdge = portrait.copy(landscapeXOffset = -300f).forOrientation(landscapeState, hasContent = true)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, leftEdge.circlePosition)
        val rightEdge = portrait.copy(landscapeXOffset = 300f).forOrientation(landscapeState, hasContent = true)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, rightEdge.circlePosition)
    }

    @Test
    fun portraitNeverBecomesALandscapePill() {
        val state = OrientationState(isLandscape = false)
        // Portrait only ever stretches the pill's width for content (the content pill width setting); nothing else moves.
        val out = portrait.forOrientation(state, hasContent = true)
        assertEquals(portrait.withContentPillWidth(state.screenWidthDp), out)
        assertEquals(portrait, out.copy(width = portrait.width))
        // With the content pill set to match the idle one, portrait is returned untouched.
        val same = portrait.copy(contentPillSameAsIdle = true)
        assertEquals(same, same.forOrientation(state, hasContent = true))
    }

    @Test
    fun companionCutoutIsAsBigAsTheShortSide() {
        val bar = portrait.forOrientation(landscapeState, hasContent = true)
        assertEquals(34f, collapsedCompanionSizeDp(bar), 0.01f)
        assertEquals(34f, collapsedCompanionSizeDp(portrait), 0.01f)
    }

    @Test
    fun contentPillCanSitInTheLeftCornerRightCornerOrOnTheCamera() {
        val state = OrientationState(
            isLandscape = true,
            screenWidthDp = 800f,
            camera = LandscapeCamera(xOffsetDp = -300f, centerYDp = 20f)
        )
        val s = portrait.copy(landscapeWidth = 100f, landscapeCircleEdgeInset = 16f)
        // 400 - 16 inset - 50 half width = 334 from the centre.
        val left = s.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT)
            .forOrientation(state, hasContent = true)
        assertEquals(-334f, left.xOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, left.circlePosition)
        val right = s.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_RIGHT)
            .forOrientation(state, hasContent = true)
        assertEquals(334f, right.xOffset, 0.01f)
        val middle = s.copy(landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE)
            .forOrientation(state, hasContent = true)
        assertEquals(0f, middle.xOffset, 0.01f)
    }

    @Test
    fun contentPillHorizontalOffsetIsAddedOnTopOfThePosition() {
        val state = OrientationState(isLandscape = true, screenWidthDp = 800f)
        val out = portrait.copy(
            landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeXOffset = 40f
        ).forOrientation(state, hasContent = true)
        assertEquals(40f, out.xOffset, 0.01f)
    }

    @Test
    fun centreTopCameraSitsMidHeightOnTheSideEdgeWhenNoCutoutIsReported() {
        // Landscape phone 800 x 360 dp, camera on the left (rotation says left), no cutout reported.
        val state = OrientationState(
            isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f,
            rotationSide = SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        )
        val idle = portrait.copy(landscapeCircleSize = 26f, landscapeCircleEdgeInset = 10f).forOrientation(state)
        assertEquals(-377f, idle.xOffset, 0.01f)
        assertEquals(167f, idle.yOffset, 0.01f) // 360 / 2 - 26 / 2
    }

    // Reverse landscape (display rotation 270): the authored profile is mirrored

    @Test
    fun reverseLandscapeMirrorsAForcedLeftIdlePill() {
        val s = portrait.copy(
            landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
            landscapeCircleSize = 26f, landscapeCircleEdgeInset = 10f, landscapeCircleXAdjust = 5f
        )
        val normal = s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f))
        val reverse = s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f, reversed = true))
        assertEquals(-normal.xOffset, reverse.xOffset, 0.01f)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_RIGHT, normal.circlePosition)
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, reverse.circlePosition)
    }

    @Test
    fun reverseLandscapeKeepsTheRealCameraButMirrorsFineTuning() {
        val s = portrait.copy(landscapeCircleSize = 26f, landscapeCircleXAdjust = 6f)
        val reverse = s.forOrientation(
            OrientationState(
                isLandscape = true, screenWidthDp = 800f, reversed = true,
                camera = LandscapeCamera(xOffsetDp = 380f, centerYDp = 200f)
            )
        )
        assertEquals(374f, reverse.xOffset, 0.01f) // 380 on the real notch, tuning mirrored
        assertEquals(SmartIslandSettings.CIRCLE_POSITION_LEFT, reverse.circlePosition)
    }

    @Test
    fun reverseLandscapeMirrorsTheContentPill() {
        val s = portrait.copy(
            landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT,
            landscapeWidth = 150f, landscapeXOffset = 10f
        )
        val normal = s.forOrientation(landscapeState, hasContent = true)
        val reverse = s.forOrientation(landscapeState.copy(reversed = true), hasContent = true)
        assertEquals(-normal.xOffset, reverse.xOffset, 0.01f)
    }

    @Test
    fun pickerIsOppositeTheCamera() {
        val s = portrait.copy(landscapeCirclePosition = SmartIslandSettings.LANDSCAPE_POSITION_LEFT)
        val normal = OrientationState(isLandscape = true, screenWidthDp = 800f)
        assertEquals(SmartIslandSettings.LANDSCAPE_POSITION_LEFT, s.landscapeCameraSide(normal))
        assertEquals(SmartIslandSettings.LANDSCAPE_POSITION_RIGHT, s.landscapeCameraSide(normal.copy(reversed = true)))
    }
}
