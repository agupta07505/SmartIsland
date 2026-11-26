/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.DeviceStates
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.components.BatteryVisual
import com.agupta07505.smartisland.ui.components.batteryVisualColor
import com.agupta07505.smartisland.ui.components.resolveBatteryVisual
import com.agupta07505.smartisland.ui.components.vivid
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryVisualTest {
    @Test
    fun batteryCategoryPriority() {
        assertEquals(BatteryVisual.PluggedIn, resolveBatteryVisual("battery_plugged_in", "Plugged in", 15f))
        assertEquals(BatteryVisual.PluggedOut, resolveBatteryVisual("battery_plugged_out", "Unplugged", 80f))
        assertEquals(BatteryVisual.Full, resolveBatteryVisual("battery_full", "Fully Charged", 100f))
        assertEquals(BatteryVisual.Low, resolveBatteryVisual("battery_low", "Low Battery", 15f))
        assertEquals(BatteryVisual.Saver, resolveBatteryVisual("battery_saver", "Battery Saver ON", 60f))

        assertEquals(BatteryVisual.PluggedOut, resolveBatteryVisual("battery_plugged_out", "Unplugged", 12f))

        assertEquals(BatteryVisual.Saver, resolveBatteryVisual(null, "Battery Saver ON", 60f))
        assertEquals(BatteryVisual.Low, resolveBatteryVisual(null, "Battery", 10f))
        assertEquals(BatteryVisual.PluggedIn, resolveBatteryVisual(null, "Battery", 60f))
    }

    @Test
    fun batteryColorContrast() {
        val user = Color(0xFF10B981)
        // PluggedIn and Full show the user's colour, brightened (see vividKeepsHueAndRaisesBrightness)
        assertEquals(vivid(user), batteryVisualColor(BatteryVisual.PluggedIn, user))
        assertEquals(vivid(user), batteryVisualColor(BatteryVisual.Full, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.PluggedOut, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.Low, user))
        assertNotEquals(user, batteryVisualColor(BatteryVisual.Saver, user))

        val dull = Color(0xFF10B981)
        val bright = vivid(dull)
        assertEquals(0.96f, maxOf(bright.red, bright.green, bright.blue), 0.01f)
        // same hue family: green still leads, blue is still above red
        assertTrue(bright.green > bright.blue && bright.blue > bright.red)
        // already bright stays bright; grey stays grey; alpha is kept
        assertEquals(1f, maxOf(vivid(Color.White).red, 0f), 0.001f)
        assertEquals(vivid(Color(0xFF808080)).red, vivid(Color(0xFF808080)).blue, 0.001f)
        // colour alpha is stored in 8 bits (0.5 becomes 128/255), so allow 1 step of rounding
        assertEquals(0.5f, vivid(dull.copy(alpha = 0.5f)).alpha, 0.005f)
    }
}

class CollapsedValueIsIconTest {
    private fun card(mode: IslandMode, category: String?, text: String = "", progress: Int = 0, progressMax: Int = 0) =
        IslandNotification(
            key = "k", packageName = "pkg", appName = "App", title = "Title", text = text, timeMillis = 0L,
            mode = mode, category = category, progress = progress, progressMax = progressMax
        )

    private val settings = SmartIslandSettings()

    @Test
    fun collapsedValueGlyph() {
        for (category in listOf("wifi", "wifi_disconnected", "vpn", "vpn_disconnected", "usb_connected",
            "usb_disconnected", "headphones_connected", "headphones_disconnected", DeviceStates.AIRPLANE_ON,
            DeviceStates.AIRPLANE_OFF, DeviceStates.RINGER_RING, DeviceStates.RINGER_VIBRATE,
            DeviceStates.RINGER_SILENT)) {
            assertFalse(category, collapsedValueIsIcon(card(IslandMode.Bluetooth, category), settings))
        }

        assertTrue(collapsedValueIsIcon(card(IslandMode.Bluetooth, "bluetooth"), settings))

        val buds = card(IslandMode.Bluetooth, "bluetooth", progress = 82, progressMax = 100)
        assertFalse(collapsedValueIsIcon(buds, SmartIslandSettings(showBluetoothBattery = true)))
        assertTrue(collapsedValueIsIcon(buds, SmartIslandSettings(showBluetoothBattery = false)))

        assertTrue(collapsedValueIsIcon(card(IslandMode.DownloadUpload, null), settings))
        assertFalse(collapsedValueIsIcon(card(IslandMode.Battery, "battery_plugged_in", "71%"), settings))
    }
}

class FaceIdShapesTest {
    @Test
    fun resampleGivesRequestedPoints() {
        val square = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val out = faceIdResample(square, 8)
        assertEquals(16, out.size)
        // perimeter 40, so one point every 5 units, starting at the first corner
        assertEquals(0f, out[0], 1e-4f)
        assertEquals(0f, out[1], 1e-4f)
        assertEquals(5f, out[2], 1e-4f)
        assertEquals(0f, out[3], 1e-4f)
        assertEquals(10f, out[4], 1e-4f)
        assertEquals(0f, out[5], 1e-4f)
    }

    @Test
    fun alignKeepsTheSamePointsInANewOrder() {
        val (face, tick) = faceIdMorphOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, face.size)
        assertEquals(face.size, tick.size)
        val raw = faceIdResample(FACE_ID_TICK, FACE_ID_MORPH_POINTS)
        val rawSorted = raw.toList().chunked(2).map { it[0] to it[1] }.sortedWith(compareBy({ it.first }, { it.second }))
        val alignedSorted = tick.toList().chunked(2).map { it[0] to it[1] }.sortedWith(compareBy({ it.first }, { it.second }))
        assertEquals(rawSorted, alignedSorted)
    }

    @Test
    fun alignedTickNearOutlineStart() {
        val (face, tick) = faceIdMorphOutlines()
        var total = 0f
        for (i in 0 until FACE_ID_MORPH_POINTS) {
            val dx = face[2 * i] - tick[2 * i]
            val dy = face[2 * i + 1] - tick[2 * i + 1]
            total += hypot(dx, dy)
        }
        assertTrue("average travel was ${total / FACE_ID_MORPH_POINTS}", total / FACE_ID_MORPH_POINTS < 45f)
    }

    @Test
    fun blendEndsAreTheTwoOutlines() {
        val (face, tick) = faceIdMorphOutlines()
        val out = FloatArray(face.size)
        faceIdBlend(face, tick, 0f, out)
        assertArrayEquals(face, out, 1e-4f)
        faceIdBlend(face, tick, 1f, out)
        assertArrayEquals(tick, out, 1e-4f)
        faceIdBlend(face, tick, 0.5f, out)
        assertEquals((face[10] + tick[10]) / 2f, out[10], 1e-4f)
    }

    @Test
    fun tickInsideFaceCentred() {
        val xs = FACE_ID_TICK.filterIndexed { i, _ -> i % 2 == 0 }
        val ys = FACE_ID_TICK.filterIndexed { i, _ -> i % 2 == 1 }
        assertTrue(xs.min() > 65.8f && xs.max() < 154.2f)
        assertTrue(ys.min() > 60.8f && ys.max() < 136.1f)
        assertEquals(FACE_ID_HEAD_CX, (xs.min() + xs.max()) / 2f, 3f)
    }

    @Test
    fun clipKeepsOnlyOneSideOfTheCut() {
        val left = faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, keepLeft = true)
        val right = faceIdClipX(FACE_ID_TICK, FACE_ID_TICK_SPLIT_X, keepLeft = false)
        assertTrue(left.size >= 6 && right.size >= 6)
        assertTrue(left.filterIndexed { i, _ -> i % 2 == 0 }.all { it <= FACE_ID_TICK_SPLIT_X + 1e-3f })
        assertTrue(right.filterIndexed { i, _ -> i % 2 == 0 }.all { it >= FACE_ID_TICK_SPLIT_X - 1e-3f })
    }

    private val u = 1.25f

    private fun pillTrack() = FaceIdEdgeTrack(
        cx = FACE_ID_HEAD_CX, cy = 17f * u, w = 112f * u, h = 34f * u, r = 17f * u,
        thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
    )

    private fun outlineOf(track: FaceIdEdgeTrack, left: Boolean, turn: Float): FloatArray {
        val out = FloatArray(FACE_ID_ARC_OUTLINE_POINTS * 2)
        if (left) track.leftOutlineInto(turn, out) else track.rightOutlineInto(turn, out)
        return out
    }

    private fun xs(p: FloatArray) = p.filterIndexed { i, _ -> i % 2 == 0 }
    private fun ys(p: FloatArray) = p.filterIndexed { i, _ -> i % 2 == 1 }

    @Test
    fun leftArcBounds() {
        val arc = outlineOf(pillTrack(), left = true, turn = 0f)
        val x = xs(arc)
        val y = ys(arc)
        assertTrue(x.min() >= FACE_ID_HEAD_CX - 56f * u)
        assertTrue(y.min() >= 0f && y.max() <= 34f * u)
        assertTrue(x.max() < FACE_ID_HEAD_CX - 10f)
        assertTrue(x.max() > FACE_ID_HEAD_CX - 56f * u + 17f * u)
        assertTrue(y.min() < 2f && y.max() > 34f * u - 2f)
    }

    @Test
    fun rightArcIsTheLeftArcMirrored() {
        val track = pillTrack()
        val left = outlineOf(track, left = true, turn = 0f)
        val right = outlineOf(track, left = false, turn = 0f)
        assertEquals(2f * FACE_ID_HEAD_CX - xs(left).min(), xs(right).max(), 0.05f)
        assertEquals(2f * FACE_ID_HEAD_CX - xs(left).max(), xs(right).min(), 0.05f)
        assertEquals(ys(left).min(), ys(right).min(), 0.05f)
        assertEquals(ys(left).max(), ys(right).max(), 0.05f)
    }

    @Test
    fun turningHalfWayRoundSwapsTheTwoArcs() {
        val track = pillTrack()
        val half = track.perimeter / 2f
        val leftTurned = outlineOf(track, left = true, turn = -half)
        val rightAtRest = outlineOf(track, left = false, turn = 0f)
        assertEquals(xs(rightAtRest).min(), xs(leftTurned).min(), 0.1f)
        assertEquals(xs(rightAtRest).max(), xs(leftTurned).max(), 0.1f)
        assertEquals(ys(rightAtRest).min(), ys(leftTurned).min(), 0.1f)
        assertEquals(ys(rightAtRest).max(), ys(leftTurned).max(), 0.1f)
        // a full perimeter brings an arc back to where it started
        val round = outlineOf(track, left = true, turn = -track.perimeter)
        assertArrayEquals(outlineOf(track, left = true, turn = 0f), round, 0.01f)
    }

    @Test
    fun trackPointsStayOnTheCutoutAndWrap() {
        val track = pillTrack()
        val p = FloatArray(2)
        track.pointAt(0f, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-3f)
        val topY = p[1]
        track.pointAt(track.perimeter, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-3f)
        assertEquals(topY, p[1], 1e-3f)
        // half way round from the top middle is the bottom middle
        track.pointAt(track.perimeter / 2f, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-2f)
        assertEquals(34f * u - topY, p[1], 1e-2f)
        for (k in 0 until 40) {
            track.pointAt(track.perimeter * k / 40f, p, 0)
            assertTrue(p[0] >= FACE_ID_HEAD_CX - 56f * u && p[0] <= FACE_ID_HEAD_CX + 56f * u)
            assertTrue(p[1] >= 0f && p[1] <= 34f * u)
        }
    }

    @Test
    fun roundCutoutStillGetsTwoSeparateArcs() {
        val size = 26f * u
        val track = FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX, cy = size / 2f, w = size, h = size, r = size / 2f,
            thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
        )
        assertTrue(track.arcLength <= track.perimeter / 2f - 3f * track.thickness + 1e-3f)
        val left = outlineOf(track, left = true, turn = 0f)
        val right = outlineOf(track, left = false, turn = 0f)
        assertTrue(xs(left).max() < FACE_ID_HEAD_CX)
        assertTrue(xs(right).min() > FACE_ID_HEAD_CX)
    }

    @Test
    fun splitOutlinesPairHalfSideArc() {
        val split = faceIdSplitOutlines(pillTrack())
        assertEquals(FACE_ID_SPLIT_POINTS * 2, split.leftFrom.size)
        assertEquals(split.leftFrom.size, split.leftTo.size)
        assertEquals(split.rightFrom.size, split.rightTo.size)
        assertTrue(xs(split.leftTo).max() < FACE_ID_HEAD_CX)
        assertTrue(xs(split.rightTo).min() > FACE_ID_HEAD_CX)
        var total = 0f
        for (i in 0 until FACE_ID_SPLIT_POINTS) {
            total += hypot(split.leftFrom[2 * i] - split.leftTo[2 * i], split.leftFrom[2 * i + 1] - split.leftTo[2 * i + 1])
        }
        assertTrue("average travel was ${total / FACE_ID_SPLIT_POINTS}", total / FACE_ID_SPLIT_POINTS < 110f)
    }

    @Test
    fun mediumIsTheDefaultArcThickness() {
        assertEquals(3f, FaceIdArcThickness.Medium.dp, 0f)
        assertTrue(FaceIdArcThickness.Thin.dp < FaceIdArcThickness.Medium.dp)
        assertTrue(FaceIdArcThickness.Medium.dp < FaceIdArcThickness.Thick.dp)
    }

    @Test
    fun arcThickAlongSideThinTips() {
        val track = pillTrack()
        val full = track.thickness / 2f
        val n = FACE_ID_ARC_SEGMENTS_FOR_TEST
        val mid = track.halfWidthAt(n / 2)
        assertEquals(full, mid, 1e-3f)
        var previous = mid
        for (k in n / 2..n) {
            val w = track.halfWidthAt(k)
            assertTrue("width grew at $k", w <= previous + 1e-4f)
            assertTrue(w <= full + 1e-4f)
            previous = w
        }
        assertEquals(full * FACE_ID_ARC_TIP_WIDTH, track.halfWidthAt(n), 1e-3f)
        assertEquals(full * FACE_ID_ARC_TIP_WIDTH, track.halfWidthAt(0), 1e-3f)
        // mirror image around the middle
        for (k in 0..n) assertEquals(track.halfWidthAt(k), track.halfWidthAt(n - k), 1e-3f)
        // no sudden step between neighbours
        for (k in 1..n) assertTrue(abs(track.halfWidthAt(k) - track.halfWidthAt(k - 1)) < full * 0.2f)
    }

    @Test
    fun thickPartCoversStraightSide() {
        val track = pillTrack()
        assertTrue(track.halfWidthAt(FACE_ID_ARC_SEGMENTS_FOR_TEST / 4) < track.thickness / 2f)
        val tall = FaceIdEdgeTrack(
            cx = FACE_ID_HEAD_CX, cy = 40f * u, w = 112f * u, h = 80f * u, r = 12f * u,
            thickness = FaceIdArcThickness.Medium.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u
        )
        assertEquals(tall.thickness / 2f, tall.halfWidthAt(FACE_ID_ARC_SEGMENTS_FOR_TEST / 2 + 2), 1e-3f)
    }

    @Test
    fun taperedArcStaysInsideTheCutout() {
        val arc = outlineOf(pillTrack(), left = true, turn = 0f)
        assertTrue(xs(arc).min() >= FACE_ID_HEAD_CX - 56f * u)
        assertTrue(ys(arc).min() >= 0f && ys(arc).max() <= 34f * u)
    }

    private fun notchTrack(radius: Float = 22f) = FaceIdEdgeTrack(
        cx = FACE_ID_HEAD_CX, cy = 17f * u, w = 112f * u, h = 34f * u, r = radius * u,
        thickness = FaceIdArcThickness.Sliver.dp * u, reach = FACE_ID_ARC_REACH_DP * u, inset = FACE_ID_ARC_INSET_DP * u,
        flatTop = true, hiddenRise = FACE_ID_NOTCH_RISE_DP * u
    )

    @Test
    fun cutoutCornerFollowsShape() {
        // four round corners: at most half the shorter side
        assertEquals(17f, faceIdCutoutCornerRadius(22f, 112f, 34f, flatTop = false), 1e-4f)
        assertEquals(5f, faceIdCutoutCornerRadius(5f, 112f, 34f, flatTop = false), 1e-4f)
        assertEquals(13f, faceIdCutoutCornerRadius(40f, 26f, 26f, flatTop = false), 1e-4f)
        // flat top: the bottom corners may use the whole height
        assertEquals(22f, faceIdCutoutCornerRadius(22f, 112f, 34f, flatTop = true), 1e-4f)
        assertEquals(34f, faceIdCutoutCornerRadius(60f, 112f, 34f, flatTop = true), 1e-4f)
        assertEquals(10f, faceIdCutoutCornerRadius(60f, 20f, 34f, flatTop = true), 1e-4f)
        assertEquals(0f, faceIdCutoutCornerRadius(-3f, 112f, 34f, flatTop = true), 1e-4f)
    }

    @Test
    fun compactTrackRadius() {
        val raw = outlineOf(FaceIdEdgeTrack(FACE_ID_HEAD_CX, 17f * u, 112f * u, 34f * u, 22f * u, 1.25f * u, 14f * u, 0.5f * u), true, 0f)
        val clamped = outlineOf(FaceIdEdgeTrack(FACE_ID_HEAD_CX, 17f * u, 112f * u, 34f * u, 17f * u, 1.25f * u, 14f * u, 0.5f * u), true, 0f)
        assertArrayEquals(clamped, raw, 1e-3f)
    }

    @Test
    fun compactArcsSwapHalfPerimeter() {
        val track = pillTrack()
        assertEquals(track.perimeter / 2f, track.leftSwap, 1e-2f)
        assertEquals(track.perimeter / 2f, track.rightSwap, 1e-2f)
    }

    @Test
    fun notchTrackRisesStaysInNotch() {
        val track = notchTrack()
        val p = FloatArray(2)
        track.pointAt(0f, p, 0)
        assertEquals(FACE_ID_HEAD_CX, p[0], 1e-3f)
        assertTrue("top middle at ${p[1]}", p[1] < 0f)
        var visibleLowest = 0f
        for (k in 0 until 400) {
            track.pointAt(track.perimeter * k / 400f, p, 0)
            assertTrue(p[0] >= FACE_ID_HEAD_CX - 56f * u - 1e-3f && p[0] <= FACE_ID_HEAD_CX + 56f * u + 1e-3f)
            assertTrue(p[1] <= 34f * u)
            if (p[1] > visibleLowest) visibleLowest = p[1]
        }
        assertTrue(visibleLowest > 34f * u - 2f * u)
    }

    @Test
    fun notchTrackNoTopCorners() {
        // the top of the screen is flat
        val track = notchTrack()
        val p = FloatArray(2)
        val sideX = 56f * u - FACE_ID_ARC_INSET_DP * u - 0.625f * u
        for (k in 0 until 400) {
            track.pointAt(track.perimeter * k / 400f, p, 0)
            if (p[1] >= 0f && p[1] <= 10f * u) {
                assertEquals("point $k at ${p[0]}, ${p[1]}", sideX, abs(p[0] - FACE_ID_HEAD_CX), 0.05f)
            }
        }
    }

    @Test
    fun notchBottomRealRadius() {
        val big = notchTrack(22f)
        val small = notchTrack(17f)
        assertTrue(big.perimeter < small.perimeter)
        assertTrue(big.arcLength != small.arcLength)
        // a radius past the height is held at the height, whatever more is asked for
        assertEquals(notchTrack(34f).perimeter, notchTrack(60f).perimeter, 1e-3f)
    }

    @Test
    fun notchArcsStartAtEdge() {
        val track = notchTrack()
        val left = outlineOf(track, left = true, turn = 0f)
        val right = outlineOf(track, left = false, turn = 0f)
        assertTrue(ys(left).min() < 0f)
        assertTrue(ys(left).max() <= 34f * u)
        assertTrue(xs(left).min() >= FACE_ID_HEAD_CX - 56f * u)
        assertTrue(xs(left).max() < FACE_ID_HEAD_CX)
        assertTrue(xs(right).min() > FACE_ID_HEAD_CX)
        assertEquals(ys(left).min(), ys(right).min(), 0.05f)
        assertEquals(ys(left).max(), ys(right).max(), 0.05f)
        assertEquals(2f * FACE_ID_HEAD_CX - xs(left).min(), xs(right).max(), 0.05f)
    }

    @Test
    fun notchArcsSwapOwnRoute() {
        val track = notchTrack()
        assertTrue(track.leftSwap > 0f && track.rightSwap > 0f)
        assertEquals(track.perimeter, track.leftSwap + track.rightSwap, 1e-2f)
        val rest = outlineOf(track, left = true, turn = 0f)
        val rightRest = outlineOf(track, left = false, turn = 0f)
        assertArrayEquals(rightRest, outlineOf(track, left = true, turn = -track.leftSwap), 0.05f)
        assertArrayEquals(rest, outlineOf(track, left = false, turn = -track.rightSwap), 0.05f)
    }

    @Test
    fun notchArcsNeverTouch() {
        val track = notchTrack()
        val room = minOf(track.leftSwap, track.rightSwap)
        assertTrue(track.arcLength <= room - 3f * track.thickness + 1e-3f)
    }

    // distance inward from the edge of a rounded rectangle
    private fun insideEdge(x: Float, y: Float, cx: Float, cy: Float, a: Float, b: Float, rad: Float): Float {
        val qx = abs(x - cx) - (a - rad)
        val qy = abs(y - cy) - (b - rad)
        val sd = hypot(maxOf(qx, 0f), maxOf(qy, 0f)) + minOf(maxOf(qx, qy), 0f) - rad
        return -sd
    }

    @Test
    fun cornerSliverGap() {
        // default pill: 112 x 34 with a 22 radius, which is held to 17
        val track = pillTrack()
        for (left in listOf(true, false)) {
            for (turn in listOf(0f, -track.perimeter / 8f, -track.perimeter / 3f)) {
                val arc = outlineOf(track, left = left, turn = turn)
                for (i in 0..FACE_ID_ARC_SEGMENTS_FOR_TEST) {
                    val gap = insideEdge(arc[2 * i], arc[2 * i + 1], FACE_ID_HEAD_CX, 17f * u, 56f * u, 17f * u, 17f * u)
                    assertEquals("point $i turn $turn", FACE_ID_ARC_INSET_DP * u, gap, 0.02f)
                }
            }
        }
    }

    @Test
    fun sliverStaysOffCutoutEdge() {
        val track = pillTrack()
        val arc = outlineOf(track, left = true, turn = 0f)
        for (i in 0 until arc.size / 2) {
            assertTrue(insideEdge(arc[2 * i], arc[2 * i + 1], FACE_ID_HEAD_CX, 17f * u, 56f * u, 17f * u, 17f * u) >= FACE_ID_ARC_INSET_DP * u - 0.02f)
        }
    }

    private val FACE_ID_ARC_SEGMENTS_FOR_TEST = 48

    @Test
    fun crossGlyphBounds() {
        assertEquals(24, FACE_ID_CROSS.size)
        var sx = 0f
        var sy = 0f
        for (i in 0 until 12) {
            val dx = FACE_ID_CROSS[2 * i] - FACE_ID_HEAD_CX
            val dy = FACE_ID_CROSS[2 * i + 1] - FACE_ID_HEAD_CY
            sx += dx
            sy += dy
            // every corner is well inside the 110 wide blue head
            assertTrue(hypot(dx, dy) < 40f)
        }
        assertEquals(0f, sx, 1e-3f)
        assertEquals(0f, sy, 1e-3f)
    }

    @Test
    fun crossMorphMatchesFacePoints() {
        val (face, cross) = faceIdCrossMorphOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, face.size)
        assertEquals(face.size, cross.size)
    }

    @Test
    fun fingerprintTickMorphPoints() {
        val (from, to) = faceIdFingerprintTickOutlines()
        assertEquals(FACE_ID_MORPH_POINTS * 2, from.size)
        assertEquals(from.size, to.size)
    }
}

class IslandShellShapeTest {
    private val density = Density(1f)

    private fun rounded(radiusPx: Float, squareTop: Boolean, w: Float, h: Float): Outline.Rounded {
        val outline = islandShellShape(radiusPx, squareTop).createOutline(Size(w, h), LayoutDirection.Ltr, density)
        assertTrue(outline is Outline.Rounded)
        return outline as Outline.Rounded
    }

    @Test
    fun shellCornerClamping() {
        val roundedR = rounded(radiusPx = 10f, squareTop = false, w = 112f, h = 34f).roundRect
        assertEquals(10f, roundedR.topLeftCornerRadius.x, 0.001f)
        assertEquals(10f, roundedR.topRightCornerRadius.x, 0.001f)
        assertEquals(10f, roundedR.bottomLeftCornerRadius.x, 0.001f)
        assertEquals(10f, roundedR.bottomRightCornerRadius.x, 0.001f)

        val squareTopR = rounded(radiusPx = 22f, squareTop = true, w = 112f, h = 34f).roundRect
        assertEquals(0f, squareTopR.topLeftCornerRadius.x, 0.001f)
        assertEquals(0f, squareTopR.topRightCornerRadius.x, 0.001f)
        // not squeezed to half the height: only the bottom corners share the 34 px
        assertEquals(22f, squareTopR.bottomLeftCornerRadius.x, 0.001f)
        assertEquals(22f, squareTopR.bottomRightCornerRadius.x, 0.001f)

        val oversized = rounded(radiusPx = 22f, squareTop = false, w = 112f, h = 34f).roundRect
        assertEquals(17f, oversized.topLeftCornerRadius.x, 0.001f)
    }

    @Test
    fun shellCornerBounds() {
        val outline = islandShellShape(-4f, squareTop = false).createOutline(Size(50f, 30f), LayoutDirection.Ltr, density)
        // all corners 0: the shape reports a plain rectangle
        assertTrue(outline is Outline.Rectangle)

        val half = islandShellShape(20f, 0.5f).createOutline(Size(200f, 120f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(10f, half.roundRect.topLeftCornerRadius.x, 0.001f)
        assertEquals(10f, half.roundRect.topRightCornerRadius.x, 0.001f)
        assertEquals(20f, half.roundRect.bottomLeftCornerRadius.x, 0.001f)
        assertEquals(20f, half.roundRect.bottomRightCornerRadius.x, 0.001f)

        val over = islandShellShape(20f, 3f).createOutline(Size(200f, 120f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(20f, over.roundRect.topLeftCornerRadius.x, 0.001f)
        val under = islandShellShape(20f, -1f).createOutline(Size(200f, 120f), LayoutDirection.Ltr, density) as Outline.Rounded
        assertEquals(0f, under.roundRect.topLeftCornerRadius.x, 0.001f)
    }

    @Test
    fun squeezedCornerTrueCircle() {
        // a 300x100 shell squeezed to 0.35 on both axes: 105x35 on screen, so the pill's radius caps at 17.5
        val size = Size(300f, 100f)
        val capped = squeezedCorner(40f, size, 0.35f, 0.35f)
        assertEquals(17.5f, capped.x * 0.35f, 0.001f)
        assertEquals(17.5f, capped.y * 0.35f, 0.001f)
        // a radius that fits is kept (and is a circle on screen even for unequal scales)
        val fits = squeezedCorner(10f, size, 0.4f, 0.5f)
        assertEquals(10f, fits.x * 0.4f, 0.001f)
        assertEquals(10f, fits.y * 0.5f, 0.001f)
        // unscaled: the plain half-height cap
        assertEquals(50f, squeezedCorner(80f, size, 1f, 1f).x, 0.001f)
    }
}
