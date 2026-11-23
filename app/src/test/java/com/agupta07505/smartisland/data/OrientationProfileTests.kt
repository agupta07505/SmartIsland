/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.agupta07505.smartisland.model.FoldPosture
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldProfilesTest {
    private val width = floatPreferencesKey("width")
    private val height = floatPreferencesKey("height")
    private val landscapeWidth = floatPreferencesKey("landscape_width")
    private val theme = stringPreferencesKey("theme_mode")
    private val cameraX = floatPreferencesKey("camera_cutout_x")
    private val cameraStyle = stringPreferencesKey("camera_cutout_style")
    private val foldedLandscape = FoldPosture.FoldedLandscape
    private val events = stringSetPreferencesKey("disabled_events")
    private val unfoldedPortrait = FoldPosture.UnfoldedPortrait
    private val foldedPortrait = FoldPosture.FoldedPortrait
    private val unfoldedLandscape = FoldPosture.UnfoldedLandscape

    private fun stored() = mutablePreferencesOf(width to 112f, height to 34f, landscapeWidth to 90f, theme to "dark")

    @Test fun anUneditedModeReadsTheNormalValues() {
        val v = FoldProfiles.view(stored(), unfoldedPortrait)
        assertEquals(112f, v[width])
        assertEquals(34f, v[height])
        assertEquals("dark", v[theme])
    }

    @Test fun orientationKeyOwnership() {
        assertTrue("width" in FoldProfiles.scopedKeys(unfoldedPortrait))
        assertFalse("landscape_width" in FoldProfiles.scopedKeys(unfoldedPortrait))
        assertTrue("landscape_width" in FoldProfiles.scopedKeys(unfoldedLandscape))
        assertFalse("width" in FoldProfiles.scopedKeys(unfoldedLandscape))
        assertTrue(FoldProfiles.PortraitKeys.intersect(FoldProfiles.LandscapeKeys).isEmpty())
    }

    @Test fun anEditStaysInsideItsMode() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, unfoldedPortrait) { it[width] = 150f }

        assertEquals(150f, FoldProfiles.view(raw, unfoldedPortrait)[width])
        // the normal value and the other modes are untouched
        assertEquals(112f, raw[width])
        assertEquals(112f, FoldProfiles.view(raw, foldedPortrait)[width])
        assertTrue(FoldProfiles.hasOwnValues(raw, unfoldedPortrait))
        assertFalse(FoldProfiles.hasOwnValues(raw, foldedPortrait))
    }

    @Test fun firstModeEditFreezesDefaults() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, unfoldedPortrait) { it[width] = 150f }
        // the normal height changes afterwards
        raw[height] = 60f
        assertEquals(34f, FoldProfiles.view(raw, unfoldedPortrait)[height])
        // a mode nobody edited still follows
        assertEquals(60f, FoldProfiles.view(raw, foldedPortrait)[height])
    }

    @Test fun sharedKeysUseNormalSettings() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, unfoldedPortrait) { it[theme] = "light" }
        assertEquals("light", raw[theme])
        assertEquals("light", FoldProfiles.view(raw, foldedPortrait)[theme])
    }

    @Test fun landscapeSparesPortraitKeys() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, unfoldedLandscape) { it[landscapeWidth] = 130f; it[width] = 99f }
        val v = FoldProfiles.view(raw, unfoldedLandscape)
        assertEquals(130f, v[landscapeWidth])
        // `width` is a portrait key: a landscape mode writes it to the normal settings
        assertEquals(99f, raw[width])
        assertEquals(90f, FoldProfiles.view(raw, unfoldedPortrait)[landscapeWidth])
    }

    @Test fun modeValueRemoval() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, unfoldedPortrait) { it.remove(height) }
        assertNull(FoldProfiles.view(raw, unfoldedPortrait)[height])
        assertEquals(34f, raw[height])
    }

    @Test fun setsAreStoredPerMode() = runTest {
        val raw = mutablePreferencesOf(events to setOf("call"))
        FoldProfiles.edit(raw, foldedPortrait) { it[events] = setOf("call", "timer") }
        FoldProfiles.edit(raw, unfoldedPortrait) { it[events] = emptySet() }
        assertEquals(setOf("call", "timer"), FoldProfiles.view(raw, foldedPortrait)[events])
        assertEquals(emptySet<String>(), FoldProfiles.view(raw, unfoldedPortrait)[events])
        assertEquals(setOf("call"), raw[events])
    }

    @Test fun theViewNeverCarriesAnotherModesKeys() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, foldedPortrait) { it[width] = 140f }
        val v = FoldProfiles.view(raw, unfoldedPortrait)
        assertTrue(v.asMap().keys.none { FoldProfiles.isProfileName(it.name) })
    }

    @Test fun clearAllDropsEveryModesOwnValues() = runTest {
        val raw = stored()
        FoldProfiles.edit(raw, foldedPortrait) { it[width] = 140f }
        FoldProfiles.edit(raw, unfoldedLandscape) { it[landscapeWidth] = 130f }
        FoldProfiles.clearAll(raw)
        assertTrue(raw.asMap().keys.none { FoldProfiles.isProfileName(it.name) })
        assertEquals(112f, raw[width])
        assertEquals(112f, FoldProfiles.view(raw, foldedPortrait)[width])
    }

    @Test fun cameraPerPortraitModeWithStyle() {
        FoldProfiles.CameraKeys.forEach { assertTrue(it in FoldProfiles.PortraitKeys) }
        assertTrue(FoldProfiles.CameraKeys.none { it in FoldProfiles.LandscapeKeys })
    }

    @Test fun landscapeReadsPortraitCamera() = runTest {
        val raw = stored()
        raw[cameraX] = 0f
        raw[cameraStyle] = "auto"
        FoldProfiles.edit(raw, foldedPortrait) { it[cameraX] = 30f; it[cameraStyle] = "left" }
        FoldProfiles.edit(raw, unfoldedPortrait) { it[cameraX] = -12f; it[cameraStyle] = "right" }

        val folded = FoldProfiles.view(raw, foldedLandscape)
        assertEquals(30f, folded[cameraX])
        assertEquals("left", folded[cameraStyle])
        val unfolded = FoldProfiles.view(raw, unfoldedLandscape)
        assertEquals(-12f, unfolded[cameraX])
        assertEquals("right", unfolded[cameraStyle])
    }

    @Test fun uneditedLandscapeCamera() = runTest {
        val raw = stored()
        raw[cameraX] = 8f
        FoldProfiles.edit(raw, unfoldedPortrait) { it[cameraX] = -12f }
        assertEquals(8f, FoldProfiles.view(raw, foldedLandscape)[cameraX])
    }

    @Test fun landscapeCameraEditToPortrait() = runTest {
        val raw = stored()
        raw[cameraX] = 8f
        FoldProfiles.edit(raw, foldedLandscape) { it[cameraX] = 21f }

        assertEquals(21f, FoldProfiles.view(raw, foldedPortrait)[cameraX])
        assertEquals(21f, FoldProfiles.view(raw, foldedLandscape)[cameraX])
        assertEquals(8f, raw[cameraX])
        assertEquals(8f, FoldProfiles.view(raw, unfoldedPortrait)[cameraX])
        assertTrue(FoldProfiles.hasOwnValues(raw, foldedPortrait))
    }
}

class OrientationProfileTest {
    private val base = SmartIslandSettings(
        width = 120f,
        disabledEvents = setOf("music"),
        disabledEventsLandscape = setOf("call"),
        enableNotchMode = true
    )

    @Test
    fun orientationOverrides() {
        assertEquals(base, base.forOrientation(OrientationState(isLandscape = false)))

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
    fun cameraFollowsBelowOldLimit() {
        val s = base.copy(landscapeCircleSize = 26f)
        val out = s.forOrientation(
            OrientationState(
                isLandscape = true, screenWidthDp = 1000f, screenHeightDp = 461f,
                camera = LandscapeCamera(xOffsetDp = -480f, centerYDp = 415f)
            )
        )
        assertEquals(402f, out.yOffset, 0.01f)
    }

    @Test
    fun verticalOffsetMovesLowCamera() {
        val state = OrientationState(
            isLandscape = true, screenWidthDp = 1000f, screenHeightDp = 461f,
            camera = LandscapeCamera(xOffsetDp = -480f, centerYDp = 415f)
        )
        val s = base.copy(landscapeCircleSize = 26f)
        assertEquals(412f, s.copy(landscapeCircleYAdjust = 10f).forOrientation(state).yOffset, 0.01f)
        assertEquals(362f, s.copy(landscapeCircleYAdjust = -40f).forOrientation(state).yOffset, 0.01f)
    }

    @Test
    fun noCutoutCircleStartsMidEdge() {
        val state = OrientationState(isLandscape = true, screenWidthDp = 1000f, screenHeightDp = 461f)
        val s = base.copy(landscapeCircleSize = 26f)
        assertEquals(461f / 2f - 13f, s.forOrientation(state).yOffset, 0.01f)
        assertEquals(461f / 2f - 13f + 40f, s.copy(landscapeCircleYAdjust = 40f).forOrientation(state).yOffset, 0.01f)
    }

    @Test
    fun cameraStaysInScreenHeight() {
        val s = base.copy(landscapeCircleSize = 26f)
        val out = s.forOrientation(
            OrientationState(
                isLandscape = true, screenWidthDp = 1000f, screenHeightDp = 461f,
                camera = LandscapeCamera(xOffsetDp = -480f, centerYDp = 600f)
            )
        )
        assertEquals(435f, out.yOffset, 0.01f)
    }

    @Test
    fun cameraKeepsNotchHeightLow() {
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
    fun fineTuneMovesIdlePill() {
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
        // never above the top edge
        val high = s.copy(landscapeCircleYAdjust = -200f).forOrientation(
            OrientationState(isLandscape = true, screenWidthDp = 800f, camera = LandscapeCamera(-350f, 20f))
        )
        assertEquals(0f, high.yOffset, 0.01f)
    }

    @Test
    fun forcedRightCircleInset() {
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
    fun forcedLeftMirrorsTopCentred() {
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
    fun landscapeJsonRoundTrip() {
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
    fun contentPillUsesLandscapeSize() {
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
    fun contentPillDefaults() {
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
        val out = portrait.forOrientation(state, hasContent = true)
        assertEquals(portrait.withContentPillWidth(state.screenWidthDp), out)
        assertEquals(portrait, out.copy(width = portrait.width))
        // with the content pill set to match the idle one, portrait is returned untouched
        val same = portrait.copy(contentPillSameAsIdle = true)
        assertEquals(same, same.forOrientation(state, hasContent = true))
    }

    @Test
    fun companionCutoutShortSide() {
        val bar = portrait.forOrientation(landscapeState, hasContent = true)
        assertEquals(34f, collapsedCompanionSizeDp(bar), 0.01f)
        assertEquals(34f, collapsedCompanionSizeDp(portrait), 0.01f)
    }

    @Test
    fun contentPillAnchors() {
        val state = OrientationState(
            isLandscape = true,
            screenWidthDp = 800f,
            camera = LandscapeCamera(xOffsetDp = -300f, centerYDp = 20f)
        )
        val s = portrait.copy(landscapeWidth = 100f, landscapeCircleEdgeInset = 16f)
        // 400 - 16 inset - 50 half width = 334 from the centre
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
    fun contentPillHorizontalOffset() {
        val state = OrientationState(isLandscape = true, screenWidthDp = 800f)
        val out = portrait.copy(
            landscapePillPosition = SmartIslandSettings.LANDSCAPE_POSITION_MIDDLE,
            landscapeXOffset = 40f
        ).forOrientation(state, hasContent = true)
        assertEquals(40f, out.xOffset, 0.01f)
    }

    @Test
    fun centreTopCameraMidHeight() {
        // landscape phone 800 x 360 dp, camera on the left (rotation says left)
        val state = OrientationState(
            isLandscape = true, screenWidthDp = 800f, screenHeightDp = 360f,
            rotationSide = SmartIslandSettings.LANDSCAPE_POSITION_LEFT
        )
        val idle = portrait.copy(landscapeCircleSize = 26f, landscapeCircleEdgeInset = 10f).forOrientation(state)
        assertEquals(-377f, idle.xOffset, 0.01f)
        assertEquals(167f, idle.yOffset, 0.01f) // 360 / 2 - 26 / 2
    }

    // reverse landscape (display rotation 270): the authored profile is mirrored

    @Test
    fun reverseMirrorsForcedLeftIdle() {
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
    fun reverseLandscapeCamera() {
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
    fun reverseMirrorsContentPill() {
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
