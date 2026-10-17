/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.geometry

import com.agupta07505.smartisland.buddy.model.MascotEye
import com.agupta07505.smartisland.buddy.model.MascotFaceLayout
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotPoint
import com.agupta07505.smartisland.buddy.model.MascotShape
import com.agupta07505.smartisland.buddy.model.MascotSize
import com.agupta07505.smartisland.buddy.model.MascotStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Outlines in a square of `size` px from its top left corner (y grows downward). */
object MascotGeometry {
    /** Every eye is drawn with this many points, so any eye can turn into any other one point at a time. */
    const val EYE_CURVE_COUNT = 24

    private const val DENSE = 96

    fun body(shape: MascotShape, size: Float): MascotPathSpec {
        fun pt(x: Float, y: Float) = MascotPoint(x * size, y * size)
        return when (shape) {
            MascotShape.Ball ->
                MascotPathSpec.ellipseIn(MascotRect(0.08f * size, 0.12f * size, 0.92f * size, 0.96f * size))
            MascotShape.Egg -> MascotPathSpec(
                listOf(
                    PathCmd.Move(pt(0.5f, 0.10f)),
                    PathCmd.Cubic(pt(0.71f, 0.10f), pt(0.86f, 0.36f), pt(0.86f, 0.60f)),
                    PathCmd.Cubic(pt(0.86f, 0.80f), pt(0.70f, 0.96f), pt(0.5f, 0.96f)),
                    PathCmd.Cubic(pt(0.30f, 0.96f), pt(0.14f, 0.80f), pt(0.14f, 0.60f)),
                    PathCmd.Cubic(pt(0.14f, 0.36f), pt(0.29f, 0.10f), pt(0.5f, 0.10f)),
                    PathCmd.Close
                )
            )
            MascotShape.Squircle -> {
                // A superellipse: flatter sides than a rounded rectangle, no corner seam.
                val count = 72
                val exponent = 2f / 3.4f
                val pts = (0 until count).map { index ->
                    val angle = index.toFloat() / count * 2f * PI.toFloat()
                    val c = cos(angle)
                    val s = sin(angle)
                    val x = 0.5f + 0.43f * (if (c < 0) -1f else 1f) * abs(c).pow(exponent)
                    val y = 0.55f + 0.40f * (if (s < 0) -1f else 1f) * abs(s).pow(exponent)
                    pt(x, y)
                }
                MascotPathSpec.polygon(pts)
            }
            MascotShape.Pill -> MascotPathSpec.roundedRect(
                MascotRect(0.05f * size, 0.28f * size, 0.95f * size, 0.88f * size), 0.30f * size, 0.30f * size
            )
        }
    }

    /** The robot's head, ears, antenna and face plate. */
    class Robot(
        val head: MascotPathSpec,
        val ears: MascotPathSpec,
        val antenna: MascotPathSpec,
        val bulb: MascotPathSpec,
        val visor: MascotPathSpec
    )

    fun robot(size: Float): Robot {
        fun rect(x: Float, y: Float, w: Float, h: Float) =
            MascotRect(x * size, y * size, (x + w) * size, (y + h) * size)
        fun rounded(r: MascotRect, radius: Float) = MascotPathSpec.roundedRect(r, radius * size, radius * size)
        val ears = rounded(rect(0.05f, 0.50f, 0.08f, 0.20f), 0.035f) +
            rounded(rect(0.87f, 0.50f, 0.08f, 0.20f), 0.035f)
        return Robot(
            head = rounded(rect(0.12f, 0.26f, 0.76f, 0.68f), 0.20f),
            ears = ears,
            antenna = rounded(rect(0.475f, 0.12f, 0.05f, 0.16f), 0.025f),
            bulb = MascotPathSpec.ellipseIn(rect(0.43f, 0.04f, 0.14f, 0.14f)),
            visor = rounded(rect(0.20f, 0.44f, 0.60f, 0.38f), 0.15f)
        )
    }

    /**
     * How far below the middle of its box the figure's own middle sits, as a share of the box. The box
     * keeps room above a minimal body for the robot's antenna, so a body placed by its box would sit a
     * little low wherever it is meant to be centred.
     */
    fun figureOffset(look: MascotLook): Float {
        val bounds = if (look.style == MascotStyle.Robot) {
            val parts = robot(1f)
            listOf(parts.ears, parts.antenna, parts.bulb).fold(parts.head.bounds()) { acc, p -> acc.union(p.bounds()) }
        } else {
            body(look.shape, 1f).bounds()
        }
        return bounds.midY - 0.5f
    }

    /** The highlight, an ellipse leaning with the body's curve. */
    fun shine(face: MascotFaceLayout, size: Float): MascotPathSpec =
        MascotPathSpec.ellipse(
            MascotPoint(face.shineCenter.x * size, face.shineCenter.y * size),
            face.shineRadii.width * size,
            face.shineRadii.height * size,
            face.shineAngle
        )

    /** An eye's outline around [center]. [size] is an open eye's width and height; the right eye mirrors the left. */
    fun eye(eye: MascotEye, size: MascotSize, center: MascotPoint, right: Boolean): MascotPathSpec {
        val pts = eyePoints(eye, size, right).map { MascotPoint(it.x + center.x, it.y + center.y) }
        return smoothClosedPath(pts)
    }

    /** The eye's outline as [EYE_CURVE_COUNT] points, from the top of its middle line, clockwise on screen. */
    fun eyePoints(eye: MascotEye, size: MascotSize, right: Boolean): List<MascotPoint> {
        val width = max(0.5f, size.width)
        val height = max(0.5f, size.height)
        var outline: List<MascotPoint> = when (eye) {
            MascotEye.Open -> stadium(width, height, MascotPoint.Zero)
            MascotEye.Closed -> stadium(width * 1.5f, width * 0.45f, MascotPoint(0f, height * 0.12f))
            MascotEye.Squint -> stadium(width, height * 0.55f, MascotPoint(0f, height * 0.08f))
            MascotEye.Wide -> ellipsePoints(width * 0.8f, width * 0.8f, MascotPoint(0f, -height * 0.02f))
            MascotEye.Happy -> arch(span = width * 2.1f, rise = height * 0.42f, thickness = width * 0.55f, top = -height * 0.18f)
            MascotEye.Heart -> heart(width * 2.0f, width * 1.8f, MascotPoint(0f, -height * 0.02f))
            MascotEye.Sleepy -> lid(width * 1.25f, height * 0.02f, height * 0.38f)
            MascotEye.Determined -> clipBelow(
                stadium(width, height, MascotPoint.Zero),
                from = MascotPoint(-width / 2f, -height * 0.30f),
                to = MascotPoint(width / 2f, height * 0.02f)
            )
        }
        if (right) outline = outline.map { MascotPoint(-it.x, it.y) }
        return resample(outline, EYE_CURVE_COUNT)
    }

    // region Outlines

    /** A capsule standing on its round ends, or lying on them when wider. */
    private fun stadium(width: Float, height: Float, center: MascotPoint): List<MascotPoint> {
        val radius = min(width, height) / 2f
        return roundedRectPoints(
            MascotRect(center.x - width / 2f, center.y - height / 2f, center.x + width / 2f, center.y + height / 2f),
            radius
        )
    }

    private fun roundedRectPoints(rect: MascotRect, radius: Float): List<MascotPoint> {
        val corners = listOf(
            MascotPoint(rect.right - radius, rect.top + radius) to (-PI / 2).toFloat(),
            MascotPoint(rect.right - radius, rect.bottom - radius) to 0f,
            MascotPoint(rect.left + radius, rect.bottom - radius) to (PI / 2).toFloat(),
            MascotPoint(rect.left + radius, rect.top + radius) to PI.toFloat()
        )
        val steps = DENSE / 4
        val points = ArrayList<MascotPoint>()
        for ((c, start) in corners) {
            for (step in 0..steps) {
                val angle = start + step.toFloat() / steps * (PI / 2).toFloat()
                points.add(MascotPoint(c.x + radius * cos(angle), c.y + radius * sin(angle)))
            }
        }
        return points
    }

    private fun ellipsePoints(rx: Float, ry: Float, center: MascotPoint): List<MascotPoint> =
        (0 until DENSE).map { index ->
            val angle = -PI.toFloat() / 2f + index.toFloat() / DENSE * 2f * PI.toFloat()
            MascotPoint(center.x + rx * cos(angle), center.y + ry * sin(angle))
        }

    /** Eyes shut with a smile: a band bent upward, round at both ends. */
    private fun arch(span: Float, rise: Float, thickness: Float, top: Float): List<MascotPoint> {
        val half = max(0.1f, span / 2f - thickness / 2f)
        val lift = max(0.1f, rise)
        val radius = (half * half + lift * lift) / (2f * lift)
        val apex = top + thickness / 2f
        val center = MascotPoint(0f, apex + radius)
        val reach = asin(min(1f, half / radius))
        fun onArc(angle: Float, r: Float) = MascotPoint(center.x + r * sin(angle), center.y - r * cos(angle))
        val steps = DENSE / 3
        val points = ArrayList<MascotPoint>()
        for (step in 0..steps) {
            points.add(onArc(-reach + step.toFloat() / steps * reach * 2f, radius + thickness / 2f))
        }
        // The right end's round cap, from the outer arc to the inner one.
        val rightEnd = onArc(reach, radius)
        for (step in 1 until 12) {
            val angle = reach + step / 12f * PI.toFloat()
            points.add(MascotPoint(rightEnd.x + thickness / 2f * sin(angle), rightEnd.y - thickness / 2f * cos(angle)))
        }
        for (step in 0..steps) {
            points.add(onArc(reach - step.toFloat() / steps * reach * 2f, radius - thickness / 2f))
        }
        val leftEnd = onArc(-reach, radius)
        for (step in 1 until 12) {
            val angle = -reach + PI.toFloat() + step / 12f * PI.toFloat()
            points.add(MascotPoint(leftEnd.x + thickness / 2f * sin(angle), leftEnd.y - thickness / 2f * cos(angle)))
        }
        return points
    }

    private fun heart(width: Float, height: Float, center: MascotPoint): List<MascotPoint> {
        // The classic heart curve, 32 wide and about 29 tall, with y up.
        val raw = (0 until DENSE).map { index ->
            val t = index.toFloat() / DENSE * 2f * PI.toFloat()
            val x = 16f * sin(t).pow(3)
            val y = 13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t)
            MascotPoint(x, y)
        }
        val minY = raw.minOf { it.y }
        val maxY = raw.maxOf { it.y }
        val middle = (minY + maxY) / 2f
        return raw.map {
            MascotPoint(center.x + it.x / 32f * width, center.y - (it.y - middle) / (maxY - minY) * height)
        }
    }

    /** A drowsy eye: the lid's flat edge on top, the eye's round bottom below. */
    private fun lid(width: Float, top: Float, depth: Float): List<MascotPoint> {
        val half = width / 2f
        val points = ArrayList<MascotPoint>()
        val flat = DENSE / 4
        for (step in 0..flat) {
            points.add(MascotPoint(-half + step.toFloat() / flat * width, top))
        }
        val round = DENSE - flat
        for (step in 1 until round) {
            val angle = step.toFloat() / round * PI.toFloat()
            points.add(MascotPoint(half * cos(angle), top + depth * sin(angle)))
        }
        return points
    }

    /** The part of a convex outline below the line from [from] to [to]. */
    private fun clipBelow(points: List<MascotPoint>, from: MascotPoint, to: MascotPoint): List<MascotPoint> {
        // Positive below the line, with y growing downward.
        fun side(p: MascotPoint) = (to.x - from.x) * (p.y - from.y) - (to.y - from.y) * (p.x - from.x)
        val clipped = ArrayList<MascotPoint>()
        for (index in points.indices) {
            val current = points[index]
            val next = points[(index + 1) % points.size]
            val a = side(current)
            val b = side(next)
            if (a >= 0) clipped.add(current)
            if ((a >= 0) != (b >= 0)) {
                val share = a / (a - b)
                clipped.add(MascotPoint(current.x + (next.x - current.x) * share, current.y + (next.y - current.y) * share))
            }
        }
        return if (clipped.size >= 3) clipped else points
    }

    // endregion

    /**
     * [count] points evenly spaced along the outline, starting where its top crosses the middle line and
     * going clockwise on screen. Every eye then has its points in the same places, which is what lets
     * one become another.
     */
    fun resample(outline: List<MascotPoint>, count: Int): List<MascotPoint> {
        if (outline.size < 3 || count < 3) return outline
        var points = outline
        // Clockwise on screen, with y growing downward, is a positive area.
        var area = 0f
        for (index in points.indices) {
            val a = points[index]
            val b = points[(index + 1) % points.size]
            area += a.x * b.y - b.x * a.y
        }
        if (area < 0) points = points.reversed()
        // The start: the highest place where the outline crosses x = 0.
        var startIndex = -1
        var startPoint = MascotPoint.Zero
        for (index in points.indices) {
            val a = points[index]
            val b = points[(index + 1) % points.size]
            if (!((a.x <= 0 && b.x > 0) || (a.x >= 0 && b.x < 0) || a.x == 0f)) continue
            val share = if (b.x == a.x) 0f else -a.x / (b.x - a.x)
            val crossing = MascotPoint(0f, a.y + (b.y - a.y) * share)
            if (startIndex < 0 || crossing.y < startPoint.y) {
                startIndex = index
                startPoint = crossing
            }
        }
        if (startIndex < 0) return points.take(count)
        val walk = ArrayList<MascotPoint>(points.size + 2)
        walk.add(startPoint)
        for (offset in 1..points.size) walk.add(points[(startIndex + offset) % points.size])
        walk.add(startPoint)
        val lengths = FloatArray(walk.size)
        for (index in 1 until walk.size) {
            lengths[index] = lengths[index - 1] + hypot(walk[index].x - walk[index - 1].x, walk[index].y - walk[index - 1].y)
        }
        val total = lengths.last()
        if (total <= 0f) return List(count) { startPoint }
        val result = ArrayList<MascotPoint>(count)
        var segment = 1
        for (step in 0 until count) {
            val target = total * step / count
            while (segment < lengths.size - 1 && lengths[segment] < target) segment++
            val span = lengths[segment] - lengths[segment - 1]
            val share = if (span > 0f) (target - lengths[segment - 1]) / span else 0f
            val a = walk[segment - 1]
            val b = walk[segment]
            result.add(MascotPoint(a.x + (b.x - a.x) * share, a.y + (b.y - a.y) * share))
        }
        return result
    }

    /** A closed curve through every point, one cubic per point (Catmull-Rom converted to Bezier). */
    fun smoothClosedPath(points: List<MascotPoint>): MascotPathSpec {
        val count = points.size
        if (count < 3) return MascotPathSpec(emptyList())
        val cmds = ArrayList<PathCmd>(count + 2)
        cmds.add(PathCmd.Move(points[0]))
        for (index in 0 until count) {
            val previous = points[(index - 1 + count) % count]
            val current = points[index]
            val next = points[(index + 1) % count]
            val after = points[(index + 2) % count]
            val first = MascotPoint(current.x + (next.x - previous.x) / 6f, current.y + (next.y - previous.y) / 6f)
            val second = MascotPoint(next.x - (after.x - current.x) / 6f, next.y - (after.y - current.y) / 6f)
            cmds.add(PathCmd.Cubic(first, second, next))
        }
        cmds.add(PathCmd.Close)
        return MascotPathSpec(cmds)
    }
}
