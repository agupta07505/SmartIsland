/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CutoutArea
import com.agupta07505.smartisland.model.EdgePillTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCutoutDetectorTest {

    @Test
    fun centerCameraCutoutOffset() {
        val screenWidthPx = 1080
        val density = 2.75f // 1080p density ~ 440 dpi

        // a center punch hole at top-center (x: 490 to 590, y: 0 to 80)
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 490,
            top = 0,
            right = 590,
            bottom = 80,
            screenWidthPx = screenWidthPx,
            density = density
        )

        assertTrue(result.hasHardwareCutout)
        // center X = 540, Screen Center X = 540 -> xOffsetPx = 0 -> xOffsetDp = 0
        assertEquals(0f, result.xOffsetDp, 0.01f)
        assertEquals(0f, result.yOffsetDp, 0.01f)
        // width = 100px / 2.75 + 24dp = ~60.36dp -> clamped to MIN_WIDTH 76dp
        assertEquals(76f, result.widthDp, 0.01f)
    }

    @Test
    fun leftCameraCutoutOffset() {
        val screenWidthPx = 1080
        val density = 2.5f

        // a punch hole on the left (x: 50 to 130, y: 10 to 90)
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 50,
            top = 10,
            right = 130,
            bottom = 90,
            screenWidthPx = screenWidthPx,
            density = density
        )

        assertTrue(result.hasHardwareCutout)
        assertEquals(-180f, result.xOffsetDp, 0.01f)
        assertEquals(4f, result.yOffsetDp, 0.01f) // 10 / 2.5 = 4dp
    }

    @Test
    fun smallHoleKeepsItsRealWidth() {
        val result = CameraCutoutDetector.calculateCutoutOffset(
            left = 500, top = 20, right = 580, bottom = 100, screenWidthPx = 1080, density = 2.75f
        )
        assertEquals(80f / 2.75f, result.holeWidthDp, 0.01f)
        assertEquals(80f / 2.75f, result.holeHeightDp, 0.01f)
        assertEquals(76f, result.widthDp, 0.01f)
    }

    private val leftHole = CameraCutoutDetector.calculateCutoutOffset(
        left = 50, top = 10, right = 130, bottom = 90, screenWidthPx = 1080, density = 2.5f
    )
    private val noHole = DetectedCutoutInfo(0f, 12f, 112f, 34f, false)

    @Test
    fun notchAreaOverrides() {
        val portraitPill = CameraCutoutDetector.areaFor(SmartIslandSettings(), leftHole, false, 432f)
        val notch = CameraCutoutDetector.areaFor(SmartIslandSettings(enableNotchMode = true), leftHole, false, 432f)
        assertTrue(portraitPill != null)
        assertEquals(portraitPill, notch)

        assertNull(CameraCutoutDetector.areaFor(SmartIslandSettings(), leftHole, true, 432f))
        assertNull(CameraCutoutDetector.areaFor(SmartIslandSettings(enableNotchMode = true), leftHole, true, 432f))
        assertNull(CameraCutoutDetector.areaFor(SmartIslandSettings(enableNotchMode = true), noHole, false, 432f))

        val staleSettings = SmartIslandSettings(
            enableNotchMode = true, cameraCutoutManual = true,
            cameraCutoutX = 300f, cameraCutoutY = 8f, cameraCutoutWidth = 30f, cameraCutoutHeight = 30f
        )
        assertEquals(
            CameraCutoutDetector.areaFor(SmartIslandSettings(), leftHole, false, 400f),
            CameraCutoutDetector.areaFor(staleSettings, leftHole, false, 400f)
        )
        assertNull(CameraCutoutDetector.areaFor(staleSettings, noHole, false, 400f))

        val manualSettings = SmartIslandSettings(
            cameraCutoutManual = true,
            cameraCutoutX = 300f, cameraCutoutY = 8f, cameraCutoutWidth = 30f, cameraCutoutHeight = 30f
        )
        assertEquals(CutoutArea(200f, 8f, 30f, 30f), CameraCutoutDetector.areaFor(manualSettings, noHole, false, 400f))

        val layoutPill = SmartIslandSettings()
        assertEquals(
            CameraCutoutDetector.areaFor(layoutPill, leftHole, false, 432f),
            CameraCutoutDetector.layoutAreaFor(layoutPill, leftHole, false, 432f)
        )
        assertNull(CameraCutoutDetector.layoutAreaFor(SmartIslandSettings(enableNotchMode = true), leftHole, false, 432f))
        assertNull(CameraCutoutDetector.layoutAreaFor(layoutPill, leftHole, true, 432f))
    }


    private fun styled(style: String, x: Float = 0f, notch: Boolean = false) = SmartIslandSettings(
        cameraCutoutStyle = style, cameraCutoutX = x, cameraCutoutY = 12f,
        cameraCutoutWidth = 40f, cameraCutoutHeight = 40f, enableNotchMode = notch
    )

    @Test
    fun cameraStyleOverrides() {
        val styledArea = CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_RIGHT), leftHole, false, 400f)
        assertEquals(CutoutArea(160f, 12f, 40f, 40f), styledArea)

        val missingArea = CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_LEFT, x = 5f), noHole, false, 400f)
        assertEquals(CutoutArea(-155f, 12f, 40f, 40f), missingArea)

        val notchNotch = styled(SmartIslandSettings.CUTOUT_STYLE_LEFT, x = 5f, notch = true)
        assertEquals(CutoutArea(-155f, 12f, 40f, 40f), CameraCutoutDetector.areaFor(notchNotch, leftHole, false, 400f))
        assertEquals(CutoutArea(-155f, 12f, 40f, 40f), CameraCutoutDetector.areaFor(notchNotch, noHole, false, 400f))

        val landscapeNotch = styled(SmartIslandSettings.CUTOUT_STYLE_AUTO, notch = true)
        assertNull(CameraCutoutDetector.areaFor(landscapeNotch, noHole, true, 400f, assumeCentre = true))
        assertNull(CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_AUTO), noHole, true, 400f, assumeCentre = true))

        assertNull(CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_LEFT), leftHole, true, 400f))
    }

    @Test
    fun styledCameraBounds() {
        assertEquals(
            CameraCutoutDetector.detectedArea(leftHole),
            CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_AUTO), leftHole, false, 400f)
        )
        assertNull(CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_AUTO), noHole, false, 400f))

        val notch = styled(SmartIslandSettings.CUTOUT_STYLE_LEFT, notch = true)
        assertNull(CameraCutoutDetector.layoutAreaFor(notch, leftHole, false, 400f))
        assertNull(CameraCutoutDetector.layoutAreaFor(notch, noHole, false, 400f))
    }

    @Test
    fun compactHoleGeometry() {
        val area = CameraCutoutDetector.areaFor(styled(SmartIslandSettings.CUTOUT_STYLE_RIGHT, x = 100f), noHole, false, 400f)
        assertEquals(200f, area!!.centreXDp, 0.001f)

        val detectedNotch = styled(SmartIslandSettings.CUTOUT_STYLE_AUTO, notch = true)
        assertEquals(
            CameraCutoutDetector.areaFor(SmartIslandSettings(), leftHole, false, 400f),
            CameraCutoutDetector.areaFor(detectedNotch, leftHole, false, 400f, assumeCentre = true)
        )

        val assumedNotch = styled(SmartIslandSettings.CUTOUT_STYLE_AUTO, notch = true)
        assertNull(CameraCutoutDetector.areaFor(assumedNotch, noHole, false, 400f))
        assertEquals(CutoutArea(0f, 12f, 40f, 40f), CameraCutoutDetector.areaFor(assumedNotch, noHole, false, 400f, assumeCentre = true))

        val compactAuto = styled(SmartIslandSettings.CUTOUT_STYLE_AUTO)
        assertNull(CameraCutoutDetector.areaFor(compactAuto, noHole, false, 400f))
        assertEquals(CutoutArea(0f, 12f, 40f, 40f), CameraCutoutDetector.areaFor(compactAuto, noHole, false, 400f, assumeCentre = true))

        val actualAuto = styled(SmartIslandSettings.CUTOUT_STYLE_AUTO)
        assertEquals(
            CameraCutoutDetector.detectedArea(leftHole),
            CameraCutoutDetector.areaFor(actualAuto, leftHole, false, 400f, assumeCentre = true)
        )
    }
}

class CutoutPickTest {
    private val screen = 1080
    private val density = 2.5f

    @Test
    fun cutoutSelection() {
        assertNull(CutoutPick.pick(emptyList(), screen, density))
        assertNull(CutoutPick.pick(listOf(CutoutRectPx(0, 0, 0, 0)), screen, density))

        val r = CutoutRectPx(500, 0, 580, 80)
        assertEquals(r, CutoutPick.pick(listOf(r), screen, density))

        val a = CutoutRectPx(420, 0, 500, 80)
        val b = CutoutRectPx(560, 0, 640, 80) // 60 px = 24 dp apart
        assertEquals(CutoutRectPx(420, 0, 640, 80), CutoutPick.pick(listOf(b, a), screen, density))

        val corner = CutoutRectPx(60, 0, 140, 80)
        val middle = CutoutRectPx(480, 0, 560, 80)
        assertEquals(middle, CutoutPick.pick(listOf(corner, middle), screen, density))

        val side = CutoutRectPx(0, 600, 60, 700)
        val top = CutoutRectPx(60, 0, 140, 80)
        assertEquals(top, CutoutPick.pick(listOf(side, top), screen, density))
    }

    @Test
    fun separatedHoles() {
        val low = CutoutRectPx(0, 900, 60, 1000)
        val high = CutoutRectPx(0, 300, 60, 400)
        assertEquals(high, CutoutPick.pick(listOf(low, high), screen, density))

        val a = CutoutRectPx(240, 0, 300, 60)
        val b = CutoutRectPx(780, 0, 880, 100)
        // centres 270 and 830: distances 270 and 290 from 540
        assertEquals(a, CutoutPick.pick(listOf(a, b), screen, density))
    }

    @Test
    fun refinedHoleBounds() {
        val intersectingBox = CutoutRectPx(400, 0, 680, 110)
        val path = CutoutRectPx(500, 20, 580, 100)
        assertEquals(path, CutoutPick.refine(intersectingBox, path))

        val disjointBox = CutoutRectPx(500, 0, 580, 60)
        assertEquals(disjointBox, CutoutPick.refine(disjointBox, CutoutRectPx(0, 300, 40, 340)))
        assertEquals(disjointBox, CutoutPick.refine(disjointBox, null))

        val tall = CutoutRectPx(500, 0, 580, 300)
        assertEquals(CutoutRectPx(500, 0, 580, 80), CutoutPick.refine(tall, null))
    }

    @Test
    fun sideHoleRefinement() {
        val leftBox = CutoutRectPx(0, 100, 160, 160) // 160 deep, 60 long along the edge
        assertEquals(CutoutRectPx(0, 100, 60, 160), CutoutPick.refineSide(leftBox, null, 2400))

        val rightBox = CutoutRectPx(2240, 100, 2400, 160)
        assertEquals(CutoutRectPx(2340, 100, 2400, 160), CutoutPick.refineSide(rightBox, null, 2400))

        val pathBox = CutoutRectPx(0, 100, 160, 160)
        assertEquals(CutoutRectPx(20, 105, 70, 155), CutoutPick.refineSide(pathBox, CutoutRectPx(20, 105, 70, 155), 2400))

        val shallowBox = CutoutRectPx(0, 100, 50, 160)
        assertEquals(shallowBox, CutoutPick.refineSide(shallowBox, null, 2400))
    }
}

class TeardropDetectionTest {
    @Test
    fun teardropRequiresACentredDropAtTheCameraEdge() {
        assertTrue(CameraCutoutDetector.isTeardropRect(490, 0, 590, 70, 1080, 2400))
        assertFalse(CameraCutoutDetector.isTeardropRect(500, 0, 580, 80, 1080, 2400))
        assertFalse(CameraCutoutDetector.isTeardropRect(500, 20, 580, 100, 1080, 2400))
        assertFalse(CameraCutoutDetector.isTeardropRect(60, 0, 160, 70, 1080, 2400))
        assertFalse(CameraCutoutDetector.isTeardropRect(300, 0, 780, 90, 1080, 2400))
        assertTrue(CameraCutoutDetector.isTeardropRect(0, 490, 70, 590, 2400, 1080))
    }

    @Test fun flatEdgeDefaultOnlyAppliesWithoutASavedValue() {
        assertTrue(EdgePillTuning.decode(null, flatEdgeDefault = true).flatEdge)
        assertTrue(EdgePillTuning.decode("pw=32", flatEdgeDefault = true).flatEdge)
        assertFalse(EdgePillTuning.decode("fe=0", flatEdgeDefault = true).flatEdge)
        assertFalse(EdgePillTuning.decode(null).flatEdge)
    }
}
