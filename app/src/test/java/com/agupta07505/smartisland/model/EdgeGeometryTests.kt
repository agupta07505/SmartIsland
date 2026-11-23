/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.LandscapeCamera
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.edgeCamera
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.data.withEdgePill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgePillPortraitDefaultsTest {
    private val eps = 0.01f
    private val w = 800f
    private val h = 360f

    private fun cam(manual: EdgePillTuning, left: Boolean = true, x: Float? = null, y: Float? = null) =
        EdgePillLayoutMath.resolveCamera(
            cameraXOffsetDp = x, cameraCenterYDp = y, rotationOnLeft = left,
            screenWidthDp = w, screenHeightDp = h, tuning = manual
        )

    @Test fun edgePillStartsPortraitSize() {
        val portrait = SmartIslandSettings.Default
        val t = EdgePillTuning()
        assertEquals(portrait.width, t.heightDp, eps)        // length = portrait's width
        assertEquals(portrait.height, t.widthDp, eps)        // thickness = portrait's height
        assertEquals(portrait.cornerRadius, t.cornerRadiusDp, eps)
        assertEquals(112f, t.heightDp, eps)
        assertEquals(34f, t.widthDp, eps)
        assertEquals(22f, t.cornerRadiusDp, eps)
    }

    @Test fun theOneTimeResetMovesTheSizeAndKeepsTheRest() {
        val saved = EdgePillTuning.decode("pw=40;ph=170;r=6;x=7;y=-30;o=0.8;bg=4;fe=1;cs=1;cp=150")
        val moved = saved.withPortraitSizes()
        assertEquals(34f, moved.widthDp, eps)
        assertEquals(112f, moved.heightDp, eps)
        assertEquals(22f, moved.cornerRadiusDp, eps)
        assertEquals(0f, moved.xOffsetDp, eps) // the saved look is flat (fe=1)
        assertEquals(-30f, moved.yOffsetDp, eps)
        assertEquals(0.8f, moved.opacity, eps)
        assertEquals(4f, moved.bubbleGapDp, eps)
        assertTrue(moved.flatEdge)
        assertTrue(moved.contentSameSize)
        assertEquals(150f, moved.contentPercent, eps)
    }

    @Test fun oldSavedPillBecomesPortraitSize() {
        val moved = EdgePillTuning.decode("pw=32;ph=150;r=20").withPortraitSizes()
        assertEquals(EdgePillTuning(), moved)
    }

    @Test fun cameraFieldsRoundTripAndHaveDefaults() {
        val t = EdgePillTuning(cameraManual = true, cameraYDp = 40f, cameraLengthDp = 20f)
        assertEquals(t, EdgePillTuning.decode(t.encode()))
        val old = EdgePillTuning.decode("pw=34;ph=112")
        assertFalse(old.cameraManual)
        assertEquals(EdgePillTuning.DEFAULT_CAMERA_Y, old.cameraYDp, eps)
        assertEquals(EdgePillTuning.DEFAULT_CAMERA_LENGTH, old.cameraLengthDp, eps)
    }

    @Test fun cameraFieldsAreClamped() {
        val t = EdgePillTuning.decode("cm=1;cy=9999;cl=0")
        assertEquals(EdgePillTuning.CAMERA_Y.endInclusive, t.cameraYDp, eps)
        assertEquals(EdgePillTuning.CAMERA_LENGTH.start, t.cameraLengthDp, eps)
        assertEquals(EdgePillTuning.DEFAULT_CAMERA_Y, EdgePillTuning.decode("cy=NaN").cameraYDp, eps)
    }

    @Test fun manualCameraWinsOverTheDetectedOne() {
        val t = EdgePillTuning(cameraManual = true, cameraYDp = 40f, cameraLengthDp = 20f)
        val c = cam(t, left = true, x = -350f, y = 200f)
        assertTrue(c.cutoutReported)
        assertEquals(40f, c.holeCentreYDp, eps)
        assertEquals(20f, c.holeLengthDp, eps)
        assertEquals(EdgeCameraSpot.Upper, c.spot)
        assertTrue(c.sideLayout)
        assertTrue(c.onLeft)
    }

    @Test fun manualCameraWithoutDetection() {
        val c = cam(EdgePillTuning(cameraManual = true, cameraYDp = 16f))
        assertTrue(c.cutoutReported)
        assertEquals(EdgeCameraSpot.Upper, c.spot)
    }

    @Test fun manualHeightMirrorsOnRight() {
        val t = EdgePillTuning(cameraManual = true, cameraYDp = 40f)
        val c = cam(t, left = false)
        assertFalse(c.onLeft)
        assertEquals(h - 40f, c.holeCentreYDp, eps)
        assertEquals(EdgeCameraSpot.Lower, c.spot)
    }

    @Test fun manualMiddleKeepsTheCentredLayout() {
        val c = cam(EdgePillTuning(cameraManual = true, cameraYDp = h / 2f))
        assertEquals(EdgeCameraSpot.Middle, c.spot)
        assertFalse(c.sideLayout)
    }

    @Test fun manualCameraInTheFlatLookKeepsTheCentredLayout() {
        val c = cam(EdgePillTuning(cameraManual = true, cameraYDp = 16f, flatEdge = true))
        assertEquals(EdgeCameraSpot.Upper, c.spot)
        assertFalse(c.sideLayout)
    }

    @Test fun autoModeIgnoresTheManualNumbers() {
        val c = cam(EdgePillTuning(cameraManual = false, cameraYDp = 16f), left = true, x = -350f, y = 180f)
        assertEquals(180f, c.holeCentreYDp, eps)
        assertEquals(EdgeCameraSpot.Middle, c.spot)
    }
}

class EdgeHoleCentreTest {
    private fun layout(camera: EdgeCamera) = EdgePillLayoutMath.compute(camera, hasSecondary = false)

    private fun flat(holeYDp: Float) = EdgePillLayoutMath.resolveCamera(-474f, holeYDp, true, 1000f, 360f,
        cameraWidthDp = 20f, tuning = EdgePillTuning(flatEdge = true), cameraLengthDp = 18f)

    @Test
    fun notchCameraCentre() {
        val upper = layout(flat(150f))
        assertEquals(180f, upper.pill.top + upper.pill.height / 2f, 0.001f)
        assertEquals(150f, EdgePillLayoutMath.holeCentreYDp(upper), 0.001f)

        val lower = layout(flat(230f))
        assertEquals(180f, lower.pill.top + lower.pill.height / 2f, 0.001f)
        assertEquals(230f, EdgePillLayoutMath.holeCentreYDp(lower), 0.001f)

        val centred = layout(flat(180f))
        assertEquals(centred.pill.top + centred.pill.height / 2f, EdgePillLayoutMath.holeCentreYDp(centred), 0.001f)

        val camera = EdgePillLayoutMath.resolveCamera(-474f, 180f, true, 1000f, 360f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(), cameraLengthDp = 18f)
        val compact = layout(camera)
        assertEquals(compact.pill.top + compact.pill.height / 2f, EdgePillLayoutMath.holeCentreYDp(compact), 0.001f)

        val missing = layout(EdgeCamera(true, 200f, 700f, 400f))
        assertEquals(missing.pill.top + missing.pill.height / 2f, EdgePillLayoutMath.holeCentreYDp(missing), 0.001f)
    }

    @Test
    fun cameraHeightInPill() {
        val offsetCamera = EdgePillLayoutMath.resolveCamera(-474f, 180f, true, 1000f, 360f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(yOffsetDp = 30f), cameraLengthDp = 18f)
        val offset = layout(offsetCamera)
        assertEquals(210f, offset.pill.top + offset.pill.height / 2f, 0.001f)
        assertEquals(180f, EdgePillLayoutMath.holeCentreYDp(offset), 0.001f)

        val sideCamera = EdgePillLayoutMath.resolveCamera(-474f, 60f, true, 1000f, 360f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(), cameraLengthDp = 18f)
        val side = layout(sideCamera)
        assertEquals(60f, EdgePillLayoutMath.holeCentreYDp(side), 0.001f)

        val hidden = layout(flat(150f))
        val target = EdgePillRules.hiddenTarget(hidden.pill, EdgePillLayoutMath.holeCentreYDp(hidden))
        assertEquals(150f, target.top + target.height / 2f, 0.001f)
    }
}

class EdgeValueRoomTest {
    private fun layout(holeYDp: Float, flatEdge: Boolean = true, yOffsetDp: Float = 0f) = EdgePillLayoutMath.compute(
        EdgePillLayoutMath.resolveCamera(-474f, holeYDp, true, 1000f, 360f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(flatEdge = flatEdge, yOffsetDp = yOffsetDp), cameraLengthDp = 18f),
        hasSecondary = false
    )

    private fun room(l: EdgePillLayout) = EdgePillLayoutMath.valueAlong(l)

    @Test
    fun valueSlotBounds() {
        val existing = layout(150f)
        val existingExpectedTop = (150f - existing.pill.top) + 18f / 2f + EdgePulseLength.VALUE_HOLE_GAP_DP
        val along = room(existing)
        assertEquals(existing.pill.height - SLOT_END_PADDING_DP - existingExpectedTop, along.lengthDp, 0.001f)
        assertEquals(existing.pill.height - SLOT_END_PADDING_DP, along.topDp + along.lengthDp, 0.001f)

        val middle = room(layout(180f)).lengthDp
        assertEquals(middle + 30f, room(layout(150f)).lengthDp, 0.001f)
        assertEquals(
            (middle - 50f).coerceAtLeast(EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP),
            room(layout(230f)).lengthDp, 0.001f
        )

        val compact = layout(180f, flatEdge = false)
        val oldTop = compact.pill.height / 2f + 18f / 2f + EdgePulseLength.VALUE_HOLE_GAP_DP
        assertEquals(compact.pill.height - SLOT_END_PADDING_DP - oldTop, room(compact).lengthDp, 0.001f)

        val offset = layout(180f, flatEdge = false, yOffsetDp = 30f)
        val offsetExpectedTop = (180f - offset.pill.top) + 18f / 2f + EdgePulseLength.VALUE_HOLE_GAP_DP
        assertEquals(offset.pill.height - SLOT_END_PADDING_DP - offsetExpectedTop, room(offset).lengthDp, 0.001f)

        assertTrue(room(layout(330f)).lengthDp >= EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP)
    }

    @Test fun noReportedHoleKeepsOldRoom() {
        val l = EdgePillLayoutMath.compute(EdgeCamera(true, 200f, 700f, 400f), hasSecondary = false)
        val oldTop = l.pill.height / 2f + l.camera.holeLengthDp / 2f + EdgePulseLength.VALUE_HOLE_GAP_DP
        assertEquals(l.pill.height - SLOT_END_PADDING_DP - oldTop, room(l).lengthDp, 0.001f)
    }
}

class EdgeSettledSlotsOffTheHoleTest {
    private val h = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP
    private val holeLength = 18f

    private fun layout(holeYDp: Float, flatEdge: Boolean = true, yOffsetDp: Float = 0f) = EdgePillLayoutMath.compute(
        EdgePillLayoutMath.resolveCamera(-474f, holeYDp, true, 1000f, 360f, cameraWidthDp = 20f,
            tuning = EdgePillTuning(flatEdge = flatEdge, yOffsetDp = yOffsetDp), cameraLengthDp = holeLength),
        hasSecondary = false
    )

    private fun inset(l: EdgePillLayout) = (l.camera.tuning.cornerRadiusDp * 0.35f).coerceIn(4f, 12f)

    private fun holeStart(l: EdgePillLayout) = EdgePillLayoutMath.holeCentreYDp(l) - l.pill.top - holeLength / 2f

    private fun clearOfHole(l: EdgePillLayout, top: Float, gap: Float): Boolean =
        top + h <= holeStart(l) - gap + 0.001f || top >= holeStart(l) + holeLength + gap - 0.001f

    @Test
    fun slotsAroundHole() {
        val centred = layout(180f)
        val centredSlots = EdgePillLayoutMath.glyphSlots(centred)
        assertEquals(inset(centred), centredSlots.glyphTop, 0.001f)
        assertEquals(centred.pill.height - inset(centred) - h, centredSlots.valueTop, 0.001f)

        val compact = layout(180f, flatEdge = false)
        val compactSlots = EdgePillLayoutMath.glyphSlots(compact)
        assertEquals(inset(compact), compactSlots.glyphTop, 0.001f)
        assertEquals(compact.pill.height - inset(compact) - h, compactSlots.valueTop, 0.001f)

        val upper = layout(150f)
        val upperSlots = EdgePillLayoutMath.glyphSlots(upper)
        assertEquals(holeStart(upper) + holeLength + EdgePulseLength.GLYPH_HOLE_GAP_DP, upperSlots.glyphTop, 0.001f)
        assertEquals(upper.pill.height - inset(upper) - h, upperSlots.valueTop, 0.001f)

        val lower = layout(210f)
        val lowerSlots = EdgePillLayoutMath.glyphSlots(lower)
        assertEquals(holeStart(lower) - EdgePulseLength.VALUE_HOLE_GAP_DP - h, lowerSlots.valueTop, 0.001f)
        assertEquals(inset(lower), lowerSlots.glyphTop, 0.001f)

        val middle = layout(185f)
        val middleSlots = EdgePillLayoutMath.glyphSlots(middle)
        assertEquals(inset(middle), middleSlots.glyphTop, 0.001f)
        assertEquals(middle.pill.height - inset(middle) - h, middleSlots.valueTop, 0.001f)
        assertTrue(middleSlots.glyphTop + h <= holeStart(middle))
        assertTrue(middleSlots.valueTop >= holeStart(middle) + holeLength)
    }

    @Test fun noReportedHoleSlotsAtEnds() {
        val l = EdgePillLayoutMath.compute(EdgeCamera(true, 200f, 700f, 400f), hasSecondary = false)
        val slots = EdgePillLayoutMath.glyphSlots(l)
        assertEquals(inset(l), slots.glyphTop, 0.001f)
        assertEquals(l.pill.height - inset(l) - h, slots.valueTop, 0.001f)
    }

    @Test fun neitherSlotIsEverOnTheHoleWhereverItSitsInThePill() {
        var y = 110f
        while (y <= 250f) {
            val l = layout(y)
            val slots = EdgePillLayoutMath.glyphSlots(l)
            assertTrue("glyph at $y", clearOfHole(l, slots.glyphTop, EdgePulseLength.GLYPH_HOLE_GAP_DP))
            assertTrue("value at $y", clearOfHole(l, slots.valueTop, EdgePulseLength.VALUE_HOLE_GAP_DP))
            assertTrue("glyph inside the pill at $y", slots.glyphTop >= 0f && slots.glyphTop + h <= l.pill.height + 0.001f)
            assertTrue("value inside the pill at $y", slots.valueTop >= 0f && slots.valueTop + h <= l.pill.height + 0.001f)
            y += 5f
        }
    }

    @Test fun anOffsetPillKeepsItsSlotsOffTheHole() {
        val l = layout(180f, flatEdge = false, yOffsetDp = 30f)
        val slots = EdgePillLayoutMath.glyphSlots(l)
        assertTrue(clearOfHole(l, slots.glyphTop, EdgePulseLength.GLYPH_HOLE_GAP_DP))
        assertTrue(clearOfHole(l, slots.valueTop, EdgePulseLength.VALUE_HOLE_GAP_DP))
    }
}

class EdgeHoleTouchTest {
    private fun layout(holeYDp: Float, flatEdge: Boolean = true, hasSecondary: Boolean = false) =
        EdgePillLayoutMath.compute(
            EdgePillLayoutMath.resolveCamera(-474f, holeYDp, true, 1000f, 360f, cameraWidthDp = 20f,
                tuning = EdgePillTuning(flatEdge = flatEdge), cameraLengthDp = 18f),
            hasSecondary = hasSecondary
        )

    private fun zone(l: EdgePillLayout, y: Float) =
        EdgePillRules.zoneInPill(y, l.pill, null, EdgePillLayoutMath.holeCentreYDp(l))

    @Test
    fun cameraTouchTargets() {
        val aligned = layout(150f)
        assertEquals(EdgePillZone.Middle, zone(aligned, 150f))
        assertEquals(EdgePillZone.Glyph, zone(aligned, aligned.pill.top + aligned.pill.height / 2f))

        val below = layout(215f)
        assertEquals(EdgePillZone.Middle, zone(below, 215f))
        assertEquals(EdgePillZone.Glyph, zone(below, below.pill.top + below.pill.height / 2f))

        val compact = layout(180f, flatEdge = false)
        assertEquals(EdgePillZone.Middle, zone(compact, 180f))
        assertEquals(EdgePillZone.Glyph, zone(compact, compact.pill.top + 6f))
        assertEquals(EdgePillRules.zoneInPill(190f, compact.pill, null), zone(compact, 190f))

        val outside = layout(60f)
        val middle = outside.pill.top + outside.pill.height / 2f
        assertEquals(EdgePillZone.Middle, zone(outside, middle))
        assertEquals(EdgePillZone.Glyph, zone(outside, outside.pill.top + 6f))

        val held = layout(150f)
        assertFalse(EdgePillRules.glyphHold(zone(held, 150f), true, false, false, false))
        assertTrue(EdgePillRules.glyphHold(zone(held, held.pill.top + held.pill.height / 2f), true, false, false, false))
    }

    @Test
    fun holeTouchLayout() {
        val noHole = layout(180f)
        val middle = noHole.pill.top + noHole.pill.height / 2f
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(middle, noHole.pill, null))

        val high = layout(150f, hasSecondary = true)
        assertTrue(high.secondary!!.top >= high.pill.bottom)
        val low = layout(210f, hasSecondary = true)
        assertTrue(low.secondary!!.bottom <= low.pill.top)

        val cameraSide = layout(150f, flatEdge = false, hasSecondary = true)
        assertTrue(cameraSide.secondary!!.top >= cameraSide.pill.bottom)
        val m = layout(210f, flatEdge = false, hasSecondary = true)
        assertTrue(m.secondary!!.bottom <= m.pill.top)
    }
}

class EdgeIsolationTest {
    private val state = OrientationState(
        isLandscape = true,
        screenWidthDp = 800f,
        screenHeightDp = 360f,
        camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 180f)
    )

    private fun settings(style: String, notch: Boolean = false) = SmartIslandSettings(
        landscapeLayoutStyle = style,
        enableNotchMode = notch,
        landscapeXOffset = 25f,
        landscapeYOffset = 40f,
        landscapeHeight = 50f,
        landscapeCornerRadius = 9f,
        landscapeOpacity = 0.5f,
        edgePill = EdgePillTuning(heightDp = 100f, cornerRadiusDp = 15f, opacity = 0.8f, contentSameSize = true)
    )

    @Test
    fun edgeWithACardUsesOnlyItsOwnGeometry() {
        val s = settings(LandscapeLayoutStyle.EDGE).forOrientation(state, hasContent = true, hasCards = true)
        val cam = state.edgeCamera(EdgePillTuning(heightDp = 100f, cornerRadiusDp = 15f, opacity = 0.8f, contentSameSize = true))!!
        assertEquals(cam.pillWidthDp, s.width, 0.001f)
        assertEquals(100f, s.height, 0.001f)
        assertEquals(15f, s.cornerRadius, 0.001f)
        assertEquals(0.8f, s.opacity, 0.001f)
        assertEquals(0f, s.xOffset, 0.001f)
        assertEquals(0f, s.yOffset, 0.001f)
        assertFalse(s.enableNotchMode)
    }

    @Test
    fun classicKeepsLandscapeFields() {
        val s = settings(LandscapeLayoutStyle.CLASSIC).forOrientation(state, hasContent = true, hasCards = true)
        assertEquals(50f, s.height, 0.001f)
        assertEquals(9f, s.cornerRadius, 0.001f)
        assertEquals(0.5f, s.opacity, 0.001f)
        assertEquals(40f, s.yOffset, 0.001f)
    }

    @Test
    fun widgetPillWithoutACardStaysClassicInEdgeMode() {
        val s = settings(LandscapeLayoutStyle.EDGE).forOrientation(state, hasContent = true, hasCards = false)
        assertEquals(50f, s.height, 0.001f)
        assertEquals(40f, s.yOffset, 0.001f)
    }

    @Test
    fun portraitNotchModeNoLongerChangesTheEdgeLayout() {
        val off = settings(LandscapeLayoutStyle.EDGE, notch = false).forOrientation(state, hasContent = true, hasCards = true)
        val on = settings(LandscapeLayoutStyle.EDGE, notch = true).forOrientation(state, hasContent = true, hasCards = true)
        assertEquals(off, on)
    }

    @Test
    fun classicOffsetsLeaveEdgeAlone() {
        val a = settings(LandscapeLayoutStyle.EDGE).forOrientation(state, hasContent = true, hasCards = true)
        val b = settings(LandscapeLayoutStyle.EDGE).copy(landscapeXOffset = -60f, landscapeYOffset = 90f, landscapeHeight = 70f)
            .forOrientation(state, hasContent = true, hasCards = true)
        // the raw landscape fields stay on the object (the classic layout reads them)
        assertEquals(a.width, b.width, 0.001f)
        assertEquals(a.height, b.height, 0.001f)
        assertEquals(a.cornerRadius, b.cornerRadius, 0.001f)
        assertEquals(a.opacity, b.opacity, 0.001f)
        assertEquals(a.xOffset, b.xOffset, 0.001f)
        assertEquals(a.yOffset, b.yOffset, 0.001f)
    }

    @Test
    fun withEdgePillResetsEveryClassicOffset() {
        val cam = state.edgeCamera(EdgePillTuning())!!
        val s = SmartIslandSettings(xOffset = 30f, yOffset = 30f, enableNotchMode = true).withEdgePill(cam)
        assertEquals(0f, s.xOffset, 0.001f)
        assertEquals(0f, s.yOffset, 0.001f)
        assertFalse(s.enableNotchMode)
    }
}

class EdgeSideLayoutTest {
    private val eps = 0.01f
    private val w = 800f
    private val h = 360f
    private val tuning = EdgePillTuning(heightDp = 112f, widthDp = 34f)

    private fun cam(
        y: Float?,
        left: Boolean = true,
        t: EdgePillTuning = tuning,
        withContent: Boolean = false,
        width: Float = 14f,
        length: Float = 14f
    ) = EdgePillLayoutMath.resolveCamera(
        cameraXOffsetDp = if (y == null) null else if (left) -350f else 350f,
        cameraCenterYDp = y,
        rotationOnLeft = left,
        screenWidthDp = w,
        screenHeightDp = h,
        cameraWidthDp = width,
        tuning = t,
        withContent = withContent,
        cameraLengthDp = length
    )

    @Test fun cornerCameraIsUpperAndKeepsItsRealHeight() {
        val c = cam(16f)
        assertEquals(EdgeCameraSpot.Upper, c.spot)
        assertTrue(c.sideLayout)
        assertEquals(16f, c.holeCentreYDp, eps)
        assertEquals(36f, c.centerYDp, eps)
    }

    @Test fun lowerCornerCameraIsLower() = assertEquals(EdgeCameraSpot.Lower, cam(344f).spot)

    @Test fun middleCameraKeepsTheCentredLayout() {
        val c = cam(180f)
        assertEquals(EdgeCameraSpot.Middle, c.spot)
        assertFalse(c.sideLayout)
        assertEquals(124f, EdgePillLayoutMath.compute(c, hasSecondary = false).pill.top, eps)
        assertNull(EdgePillLayoutMath.sideZone(EdgePillLayoutMath.compute(c, hasSecondary = false)))
    }

    @Test fun noCutoutKeepsTheCentredLayout() {
        val c = cam(null)
        assertFalse(c.cutoutReported)
        assertEquals(EdgeCameraSpot.Middle, c.spot)
        assertFalse(c.sideLayout)
    }

    @Test fun flatLookCentresOnTheScreenEvenWithTheCameraInACorner() {
        val c = cam(16f, t = tuning.copy(flatEdge = true))
        assertEquals(EdgeCameraSpot.Upper, c.spot)
        assertFalse(c.sideLayout)
        val layout = EdgePillLayoutMath.compute(c, hasSecondary = false)
        assertEquals(c.screenHeightDp / 2f, layout.pill.top + layout.pill.height / 2f, eps)
        assertEquals(tuning.cornerRadiusDp * 0.35f, EdgePillLayoutMath.glyphSlots(layout).glyphTop, eps) // the classic inset (35% of the radius)
    }

    @Test fun holeLengthFallsBackToTheWidth() {
        assertEquals(20f, cam(16f, width = 20f, length = 0f).holeLengthDp, eps)
        assertEquals(18f, cam(16f, width = 20f, length = 18f).holeLengthDp, eps)
    }

    @Test fun orientationStateCarriesTheHoleLength() {
        val state = OrientationState(
            isLandscape = true, screenWidthDp = w, screenHeightDp = h,
            camera = LandscapeCamera(xOffsetDp = -350f, centerYDp = 16f, widthDp = 10f, lengthDp = 18f)
        )
        val c = state.edgeCamera(tuning)!!
        assertEquals(18f, c.holeLengthDp, eps)
        assertTrue(c.cutoutReported)
    }

    @Test fun cornerPillClearance() {
        val layout = EdgePillLayoutMath.compute(cam(16f), hasSecondary = true)
        assertEquals(0f, layout.pill.top, eps)
        assertEquals(0f, layout.pill.left, eps)
        assertEquals(112f, layout.pill.height, eps)
        val slots = EdgePillLayoutMath.glyphSlots(layout)
        assertTrue("glyph starts past the hole's edge", slots.glyphTop >= 16f + 7f - eps)
        assertTrue(slots.valueTop >= slots.glyphTop + slots.height + SideCamera.PAIR_GAP_DP - eps)
        assertTrue(slots.valueTop + slots.height <= layout.pill.height + eps)
        val band = EdgePillLayoutMath.sideZone(layout)!!
        // the band stops where the glyph starts
        assertTrue(band.bandTopDp + band.bandHeightDp <= maxOf(slots.glyphTop, band.bandTopDp + EdgeSideCamera.MIN_BAND_DP) + eps)
        assertTrue("the hole is inside the band", 16f + 7f <= band.bandTopDp + band.bandHeightDp + eps)
    }

    @Test fun theSettledValueRunsFromTheFarEndBackToTheGlyph() {
        val upper = EdgePillLayoutMath.compute(cam(16f), hasSecondary = false)
        val slots = EdgePillLayoutMath.glyphSlots(upper)
        val along = EdgePillLayoutMath.valueAlong(upper)
        assertTrue(along.fromBottom)
        assertEquals(upper.pill.height - SLOT_END_PADDING_DP, along.topDp + along.lengthDp, eps)
        assertTrue("clear of the glyph", along.topDp >= slots.glyphTop + slots.height + SLOT_PAIR_GAP_DP - eps)
        // the lower camera is the same turned over: far end at the top
        val lower = EdgePillLayoutMath.compute(cam(344f), hasSecondary = false)
        val turned = EdgePillLayoutMath.valueAlong(lower)
        val glyph = EdgePillLayoutMath.glyphSlots(lower)
        assertFalse(turned.fromBottom)
        assertEquals(SLOT_END_PADDING_DP, turned.topDp, eps)
        assertTrue("clear of the glyph", turned.topDp + turned.lengthDp <= glyph.glyphTop - SLOT_PAIR_GAP_DP + eps)
    }

    @Test
    fun secondaryEdgeLayout() {
        val settledLayout = EdgePillLayoutMath.compute(cam(16f), hasSecondary = false)
        assertTrue(EdgePillLayoutMath.valueAlong(settledLayout).lengthDp >= EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP)

        val bubbleLayout = EdgePillLayoutMath.compute(cam(16f), hasSecondary = true)
        val b = bubbleLayout.secondary!!
        assertEquals(bubbleLayout.pill.bottom + tuning.bubbleGapDp, b.top, eps)
        assertEquals(bubbleLayout.pill.left + (bubbleLayout.pill.width - b.width) / 2f, b.left, eps)
        assertEquals(bubbleLayout.pill.width, b.width, eps)

        val reversedLayout = EdgePillLayoutMath.compute(cam(16f, left = false), hasSecondary = true)
        assertEquals(w - 34f, reversedLayout.pill.left, eps)
        assertEquals(w - 34f, reversedLayout.secondary!!.left, eps)
        assertEquals(0f, reversedLayout.pill.top, eps)

        val lowerLayout = EdgePillLayoutMath.compute(cam(344f), hasSecondary = true)
        assertEquals(h - 112f, lowerLayout.pill.top, eps)
        assertEquals(lowerLayout.pill.top - tuning.bubbleGapDp - lowerLayout.secondary!!.height, lowerLayout.secondary!!.top, eps)
        val slots = EdgePillLayoutMath.glyphSlots(lowerLayout)
        val holeLocal = 344f - lowerLayout.pill.top
        assertTrue("glyph is above the hole", slots.glyphTop + slots.height <= holeLocal - 7f + eps)
        assertTrue("value is further from the camera than the glyph", slots.valueTop < slots.glyphTop)

        val rest = EdgePillLayoutMath.compute(cam(64f), hasSecondary = false).pill
        val grown = EdgePillLayoutMath.compute(cam(64f, withContent = true), hasSecondary = false).pill
        assertTrue(grown.height > rest.height)
        assertEquals(rest.top, grown.top, eps)
    }

    @Test fun lowerPillGrowsUpwardWithContentAndKeepsItsBottom() {
        val rest = EdgePillLayoutMath.compute(cam(300f), hasSecondary = false).pill
        val grown = EdgePillLayoutMath.compute(cam(300f, withContent = true), hasSecondary = false).pill
        assertTrue(grown.height > rest.height)
        assertEquals(rest.bottom, grown.bottom, eps)
    }

    @Test fun bigHoleLengthensPill() {
        val layout = EdgePillLayoutMath.compute(cam(16f, length = 60f, width = 60f), hasSecondary = false)
        assertTrue(layout.pill.height > 112f)
        val slots = EdgePillLayoutMath.glyphSlots(layout)
        assertTrue(slots.valueTop + slots.height <= layout.pill.height + eps)
    }

    @Test fun cardPlacementFollowsTheHangingPill() {
        val c = cam(344f)
        val placement = EdgePillLayoutMath.cardPlacement(c, cardWidthDp = 350f)
        val layout = EdgePillLayoutMath.compute(c, hasSecondary = false)
        assertEquals(layout.pill.top + layout.pill.height / 2f, placement.pillCentreYDp, eps)
        assertNotNull(placement)
    }
}

class EdgeSideCameraTest {
    private val eps = 0.001f
    private val screenH = 240f
    private val slotH = EdgePillLayoutMath.GLYPH_SLOT_HEIGHT_DP

    @Test fun spotOfPicksSpotFromHoleHeight() {
        assertEquals(EdgeCameraSpot.Middle, EdgeSideCamera.spotOf(null, screenH))
        assertEquals(EdgeCameraSpot.Middle, EdgeSideCamera.spotOf(120f, screenH))
        assertEquals(EdgeCameraSpot.Middle, EdgeSideCamera.spotOf(60f, screenH))
        assertEquals(EdgeCameraSpot.Middle, EdgeSideCamera.spotOf(180f, screenH))
        assertEquals(EdgeCameraSpot.Upper, EdgeSideCamera.spotOf(59.99f, screenH))
        assertEquals(EdgeCameraSpot.Lower, EdgeSideCamera.spotOf(180.01f, screenH))
        assertEquals(EdgeCameraSpot.Upper, EdgeSideCamera.spotOf(16f, screenH))
        assertEquals(EdgeCameraSpot.Lower, EdgeSideCamera.spotOf(226f, screenH))
        assertEquals(EdgeCameraSpot.Middle, EdgeSideCamera.spotOf(10f, 0f))
    }

    @Test fun onlyTheCompactLookOffTheMiddleIsActive() {
        assertTrue(EdgeSideCamera.isActive(false, EdgeCameraSpot.Upper))
        assertTrue(EdgeSideCamera.isActive(false, EdgeCameraSpot.Lower))
        assertFalse(EdgeSideCamera.isActive(false, EdgeCameraSpot.Middle))
        assertFalse(EdgeSideCamera.isActive(true, EdgeCameraSpot.Upper))
        assertFalse(EdgeSideCamera.isActive(true, EdgeCameraSpot.Lower))
    }

    @Test
    fun fittedEdgeLength() {
        // 112 dp is portrait's default pill width
        assertTrue(EdgeSideCamera.minLengthDp(14f) <= 112f + eps)
        assertEquals(112f, EdgeSideCamera.fittedLengthDp(112f, 14f), eps)

        assertTrue(EdgeSideCamera.fittedLengthDp(112f, 60f) > 112f)
        assertEquals(150f, EdgeSideCamera.fittedLengthDp(150f, 14f), eps)

        val onAnchor = EdgeSideCamera.fittedLengthDp(112f, 40f)
        val moved = EdgeSideCamera.fittedLengthDp(112f, 40f, EdgeSideCamera.holeFromEndDp(EdgeCameraSpot.Lower, 40f, 12f))
        assertTrue(moved > onAnchor)

        for (spot in listOf(EdgeCameraSpot.Upper, EdgeCameraSpot.Lower)) {
            for (offset in listOf(-10f, 0f, 6f, 12f)) {
                val hole = 40f
                val len = EdgeSideCamera.fittedLengthDp(112f, hole, EdgeSideCamera.holeFromEndDp(spot, hole, offset))
                val cy = if (spot == EdgeCameraSpot.Upper) 40f else screenH - 40f
                val top = EdgeSideCamera.desiredTopDp(spot, screenH, cy, hole, len, offset)
                val z = EdgeSideCamera.zone(spot, len, cy - top, hole)!!
                assertTrue("value stays inside $spot $offset", z.valueTopDp + slotH <= len + eps)
            }
        }
    }

    @Test fun holeFromEndFollowsTheOffsetPerSpot() {
        assertEquals(20f + 8f, EdgeSideCamera.holeFromEndDp(EdgeCameraSpot.Lower, 14f, 8f), eps)
        assertEquals(20f - 8f, EdgeSideCamera.holeFromEndDp(EdgeCameraSpot.Upper, 14f, 8f), eps)
    }

    @Test
    fun cameraAnchorBounds() {
        val top = EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 64f, 14f, 112f, 0f)
        assertEquals(64f - 20f, top, eps)

        assertEquals(0f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 14f, 14f, 112f, 0f), eps)
        assertEquals(screenH - 112f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Lower, screenH, 226f, 14f, 112f, 0f), eps)

        val anchor = EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 90f, 14f, 112f, 0f)
        assertEquals(anchor + 40f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 90f, 14f, 112f, 40f), eps)
        assertEquals(anchor - 20f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 90f, 14f, 112f, -20f), eps)
        val lower = EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Lower, screenH, 176f, 14f, 112f, 0f)
        assertEquals(lower - 30f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Lower, screenH, 176f, 14f, 112f, -30f), eps)

        assertEquals(screenH - 112f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Upper, screenH, 90f, 14f, 112f, 999f), eps)
        assertEquals(0f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Lower, screenH, 176f, 14f, 112f, -999f), eps)

        assertEquals(120f - 56f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Middle, screenH, 120f, 14f, 112f, 0f), eps)
        assertEquals(120f - 56f + 10f, EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Middle, screenH, 120f, 14f, 112f, 10f), eps)
    }

    @Test fun lowerPillBottomCoversTheHole() {
        val top = EdgeSideCamera.desiredTopDp(EdgeCameraSpot.Lower, screenH, 176f, 14f, 112f, 0f)
        assertEquals(176f + 20f, top + 112f, eps)
    }

    @Test fun middleHasNoZone() = assertNull(EdgeSideCamera.zone(EdgeCameraSpot.Middle, 112f, 56f, 14f))

    @Test fun upperZoneOrderIsBandGlyphValueFromTheTop() {
        val z = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 20f, 14f)!!
        assertEquals(0f, z.bandTopDp, eps)
        // the glyph hugs the hole (2 dp past its edge)
        assertEquals(20f + 7f + EdgeSideCamera.GLYPH_HOLE_GAP_DP, z.glyphTopDp, eps)
        assertEquals(z.glyphTopDp, z.bandTopDp + z.bandHeightDp, eps)
        assertTrue(z.bandHeightDp >= 14f)
        assertTrue(z.valueTopDp >= z.glyphTopDp + slotH + SideCamera.PAIR_GAP_DP - eps)
        assertTrue(z.valueTopDp + slotH <= 112f - SLOT_END_PADDING_DP + eps)
    }

    @Test
    fun lowerCameraMirrorsUpperTouchZones() {
        val up = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 28f, 14f)!!
        val low = EdgeSideCamera.zone(EdgeCameraSpot.Lower, 112f, 112f - 28f, 14f)!!
        assertEquals(112f - (up.bandTopDp + up.bandHeightDp), low.bandTopDp, eps)
        assertEquals(up.bandHeightDp, low.bandHeightDp, eps)
        assertEquals(112f - (up.glyphTopDp + slotH), low.glyphTopDp, eps)
        assertEquals(112f - (up.valueTopDp + slotH), low.valueTopDp, eps)

        // the corner case from the screenshot
        val clear = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 16f, 14f)!!
        assertTrue("glyph must start past the hole's edge", clear.glyphTopDp >= 16f + 7f - eps)
        assertTrue(clear.glyphTopDp >= SLOT_START_PADDING_DP - eps)

        val nearEnd = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 14f, 14f)!!
        assertEquals(EdgeSideCamera.MIN_BAND_DP, nearEnd.bandHeightDp, eps)
        assertTrue("overlap with the glyph stays small", nearEnd.bandTopDp + nearEnd.bandHeightDp - nearEnd.glyphTopDp <= 6f)

        for (cam in listOf(4f, 14f, 28f, 40f)) {
            val covered = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, cam, 14f)!!
            assertTrue("cam=$cam", covered.bandTopDp <= maxOf(cam - 7f, 0f) + eps && covered.bandTopDp + covered.bandHeightDp >= cam + 7f - eps)
        }

        val outside = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, -40f, 14f)!!
        assertEquals(0f, outside.bandTopDp, eps)
        assertTrue(outside.bandHeightDp > 0f)
        assertTrue(outside.glyphTopDp >= outside.bandTopDp + outside.bandHeightDp + SideCamera.CAP_PADDING_DP - eps)
    }

    @Test
    fun partialHoleBounds() {
        val clipped = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 2f, 14f)!!
        assertTrue(clipped.glyphTopDp >= SLOT_START_PADDING_DP - eps)
        // the camera middle wins the overlap: the band keeps the full target height
        assertEquals(EdgeSideCamera.MIN_BAND_DP, clipped.bandHeightDp, eps)

        val centred = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 20f, 14f)!!
        assertNull(EdgeSideCamera.fadeSpan(EdgeCameraSpot.Middle, 112f, 56f, 14f, centred))

        val clear = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 60f, 14f)!!
        val span = EdgeSideCamera.fadeSpan(EdgeCameraSpot.Upper, 112f, 60f, 14f, clear)!!
        assertTrue(span.clearToDp < span.solidFromDp || span.solidFromDp == 0f)

        val bounded = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 20f, 14f)!!
        assertEquals(0f, EdgeSideCamera.followLimitDp(EdgeCameraSpot.Upper, 112f, 20f, 14f, bounded), eps)
        val far = EdgeSideZone(EdgeCameraSpot.Upper, 0f, 40f, 50f, 80f)
        assertEquals(50f - (20f + 7f + EdgeSideCamera.GLYPH_HOLE_GAP_DP), EdgeSideCamera.followLimitDp(EdgeCameraSpot.Upper, 112f, 20f, 14f, far), eps)

        val unlimited = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 20f, 14f)!!
        assertEquals(0f, EdgeSideCamera.followLimitDp(EdgeCameraSpot.Middle, 112f, 56f, 14f, unlimited), eps)
    }

    @Test fun slotClearance() {
        for (spot in listOf(EdgeCameraSpot.Upper, EdgeCameraSpot.Lower)) {
            for (hole in listOf(8f, 14f, 24f, 40f, 56f)) {
                val len = EdgeSideCamera.fittedLengthDp(112f, hole)
                var cy = 4f
                while (cy <= screenH - 4f) {
                    if (EdgeSideCamera.spotOf(cy, screenH) == spot) {
                        val top = EdgeSideCamera.desiredTopDp(spot, screenH, cy, hole, len, 0f)
                        val z = EdgeSideCamera.zone(spot, len, cy - top, hole)
                        assertNotNull(z)
                        z!!
                        val tag = "spot=$spot hole=$hole cy=$cy"
                        assertTrue("glyph in pill $tag", z.glyphTopDp >= -eps && z.glyphTopDp + slotH <= len + eps)
                        assertTrue("value in pill $tag", z.valueTopDp >= -eps && z.valueTopDp + slotH <= len + eps)
                        val bandBottom = z.bandTopDp + z.bandHeightDp
                        fun overlapsBand(t: Float) = t < bandBottom - eps && t + slotH > z.bandTopDp + eps
                        // the band stops at the glyph
                        val glyphOverlap = if (spot == EdgeCameraSpot.Upper) bandBottom - z.glyphTopDp else z.glyphTopDp + slotH - z.bandTopDp
                        assertTrue("glyph overlap $tag", glyphOverlap <= eps || z.bandHeightDp <= EdgeSideCamera.MIN_BAND_DP + eps)
                        assertTrue("band length $tag", z.bandHeightDp >= EdgeSideCamera.MIN_BAND_DP - eps)
                        assertFalse("value in band $tag", overlapsBand(z.valueTopDp))
                        val glyphEnd = z.glyphTopDp + slotH
                        val valueEnd = z.valueTopDp + slotH
                        val gap = if (spot == EdgeCameraSpot.Upper) z.valueTopDp - glyphEnd else z.glyphTopDp - valueEnd
                        assertTrue("pair gap $tag", gap >= SideCamera.PAIR_GAP_DP - eps)
                    }
                    cy += 4f
                }
            }
        }
    }

    @Test
    fun bubbleVerticalBounds() {
        assertEquals(36f + 112f + 8f, EdgeSideCamera.bubbleTopDp(EdgeCameraSpot.Upper, 36f, 112f, 34f, 8f, screenH), eps)
        assertEquals(100f - 8f - 34f, EdgeSideCamera.bubbleTopDp(EdgeCameraSpot.Lower, 100f, 112f, 34f, 8f, screenH), eps)

        assertEquals(120f - 8f - 34f, EdgeSideCamera.bubbleTopDp(EdgeCameraSpot.Upper, 120f, 112f, 34f, 8f, screenH), eps)
        assertEquals(10f + 112f + 8f, EdgeSideCamera.bubbleTopDp(EdgeCameraSpot.Lower, 10f, 112f, 34f, 8f, screenH), eps)
    }

    @Test fun bubbleIsOnThePillCentreLine() {
        // left edge: pill at 0, 34 wide: same size bubble at 0
        assertEquals(0f, EdgeSideCamera.bubbleLeftDp(0f, 34f, 34f, 800f), eps)
        assertEquals(3f, EdgeSideCamera.bubbleLeftDp(0f, 40f, 34f, 800f), eps)
        // right edge
        assertEquals(800f - 34f, EdgeSideCamera.bubbleLeftDp(800f - 34f, 34f, 34f, 800f), eps)
        assertEquals(800f - 34f, EdgeSideCamera.bubbleLeftDp(800f - 34f, 34f, 34f, 800f), eps)
    }

    @Test
    fun cameraFadeRange() {
        val z = EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 20f, 14f)!!
        val upperSpan = EdgeSideCamera.fadeSpan(EdgeCameraSpot.Upper, 112f, 20f, 14f, z)!!
        assertFalse(upperSpan.fromBottom)
        assertEquals(27f, upperSpan.clearToDp, eps)
        assertEquals(z.glyphTopDp, upperSpan.solidFromDp, eps)

        val up = EdgeSideCamera.fadeSpan(EdgeCameraSpot.Upper, 112f, 28f, 14f, EdgeSideCamera.zone(EdgeCameraSpot.Upper, 112f, 28f, 14f)!!)!!
        val lowZone = EdgeSideCamera.zone(EdgeCameraSpot.Lower, 112f, 112f - 28f, 14f)!!
        val low = EdgeSideCamera.fadeSpan(EdgeCameraSpot.Lower, 112f, 112f - 28f, 14f, lowZone)!!
        assertTrue(low.fromBottom)
        assertEquals(up.clearToDp, low.clearToDp, eps)
        assertEquals(up.solidFromDp, low.solidFromDp, eps)

        val far = EdgeSideZone(EdgeCameraSpot.Upper, 0f, 40f, 50f, 80f)
        val followingSpan = EdgeSideCamera.fadeSpan(EdgeCameraSpot.Upper, 112f, 20f, 14f, far)!!
        // 50 - limit = the dead zone's end: a glyph that follows the finger is never faded
        assertEquals(20f + 7f + EdgeSideCamera.GLYPH_HOLE_GAP_DP, followingSpan.solidFromDp, eps)
    }

    @Test fun followLimitMirrorsForALowerCamera() {
        val up = EdgeSideZone(EdgeCameraSpot.Upper, 0f, 40f, 50f, 80f)
        val low = EdgeSideZone(EdgeCameraSpot.Lower, 72f, 40f, 112f - 50f - slotH, 112f - 80f - slotH)
        assertEquals(
            EdgeSideCamera.followLimitDp(EdgeCameraSpot.Upper, 112f, 20f, 14f, up),
            EdgeSideCamera.followLimitDp(EdgeCameraSpot.Lower, 112f, 92f, 14f, low), eps
        )
    }

}

class EdgeSideGesturesTest {
    private val eps = 0.01f
    private val tuning = EdgePillTuning(heightDp = 112f, widthDp = 34f)

    private fun cam(y: Float, left: Boolean = true, flat: Boolean = false, withContent: Boolean = false) =
        EdgePillLayoutMath.resolveCamera(
            cameraXOffsetDp = if (left) -350f else 350f,
            cameraCenterYDp = y,
            rotationOnLeft = left,
            screenWidthDp = 800f,
            screenHeightDp = 360f,
            cameraWidthDp = 14f,
            tuning = tuning.copy(flatEdge = flat),
            withContent = withContent,
            cameraLengthDp = 14f
        )

    private fun layout(y: Float, flat: Boolean = false) = EdgePillLayoutMath.compute(cam(y, flat = flat), hasSecondary = false)

    @Test fun cornerHoleIsTheCameraMiddleAndTheGlyphIsNot() {
        val l = layout(16f)
        val side = EdgePillLayoutMath.sideZone(l)
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(16f, l.pill, side))
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(0.5f, l.pill, side))
        val glyph = EdgePillLayoutMath.glyphSlots(l)
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneInPill(l.pill.top + glyph.glyphTop + glyph.height / 2f + 4f, l.pill, side))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneInPill(l.pill.top + glyph.valueTop + glyph.height / 2f, l.pill, side))
    }

    @Test fun sideCameraTouchCentre() {
        val l = layout(64f) // pill top 36, centre 92; the hole is at 64
        val side = EdgePillLayoutMath.sideZone(l)
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(64f, l.pill, side))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneInPill(l.pill.top + l.pill.height / 2f + 10f, l.pill, side))
    }

    @Test fun lowerCameraBandIsAtTheBottomEnd() {
        val l = layout(344f)
        val side = EdgePillLayoutMath.sideZone(l)
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(344f, l.pill, side))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneInPill(l.pill.top + 10f, l.pill, side))
    }

    @Test fun centredLayoutKeepsItsClassicZone() {
        val l = layout(180f)
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(180f, l.pill, null))
        assertEquals(EdgePillZone.Glyph, EdgePillRules.zoneInPill(180f + 20f, l.pill, null))
        val flat = layout(16f, flat = true)
        assertEquals(EdgePillZone.Middle, EdgePillRules.zoneInPill(flat.pill.top + flat.pill.height / 2f, flat.pill, EdgePillLayoutMath.sideZone(flat)))
    }

    @Test fun tapAndHoldReadTheBandZone() {
        assertEquals(EdgePillAction.PutCardsAway, EdgePillRules.tap(EdgePillZone.Middle, cardsAway = false))
        assertEquals(EdgePillAction.NormalLongPress, EdgePillRules.hold(EdgePillZone.Middle))
        assertEquals(EdgePillAction.AppsShortcut, EdgePillRules.hold(EdgePillZone.Glyph))
    }

    @Test
    fun hiddenPillBounds() {
        val side = layout(64f)
        val target = EdgePillRules.hiddenTarget(side.pill, EdgePillLayoutMath.holeCentreYDp(side))
        assertEquals(64f, target.top + target.height / 2f, eps)

        val centred = layout(180f)
        assertEquals(EdgePillRules.hiddenTarget(centred.pill).top, EdgePillRules.hiddenTarget(centred.pill, EdgePillLayoutMath.holeCentreYDp(centred)).top, eps)

        val nearTop = layout(6f)
        assertTrue(EdgePillRules.hiddenTarget(nearTop.pill, EdgePillLayoutMath.holeCentreYDp(nearTop)).top >= 0f)
    }

    @Test fun cameraBandStaysPutWhileTheCardOpens() {
        for (y in listOf(64f, 300f)) {
            val rest = EdgePillLayoutMath.compute(cam(y), hasSecondary = false)
            val open = EdgePillLayoutMath.compute(cam(y, withContent = true), hasSecondary = false)
            val a = EdgePillLayoutMath.sideZone(rest)!!
            val b = EdgePillLayoutMath.sideZone(open)!!
            assertEquals("band start y=$y", rest.pill.top + a.bandTopDp, open.pill.top + b.bandTopDp, eps)
            assertEquals("band length y=$y", a.bandHeightDp, b.bandHeightDp, eps)
        }
    }

    @Test fun cardPlacementCentreFollowsTheGrownPill() {
        val c = cam(64f, withContent = true)
        val placement = EdgePillLayoutMath.cardPlacement(c, cardWidthDp = 350f)
        val l = EdgePillLayoutMath.compute(c, hasSecondary = false)
        assertEquals(l.pill.top + l.pill.height / 2f, placement.pillCentreYDp, eps)
    }
}
