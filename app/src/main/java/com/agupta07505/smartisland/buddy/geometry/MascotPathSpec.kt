/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.geometry

import com.agupta07505.smartisland.buddy.model.MascotPoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * A path as plain data. Compose's `Path` is write-only and cannot be measured or morphed, so geometry
 * stays as commands in pure Kotlin (testable on the JVM) and the UI layer replays them into a `Path`.
 */
sealed interface PathCmd {
    data class Move(val to: MascotPoint) : PathCmd
    data class Line(val to: MascotPoint) : PathCmd
    data class Cubic(val c1: MascotPoint, val c2: MascotPoint, val to: MascotPoint) : PathCmd
    data object Close : PathCmd
}

data class MascotRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val midY: Float get() = (top + bottom) / 2f

    fun union(other: MascotRect) = MascotRect(
        minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom)
    )
}

class MascotPathSpec(val commands: List<PathCmd>) {
    operator fun plus(other: MascotPathSpec) = MascotPathSpec(commands + other.commands)

    /** Tight bounds, measured along the curves rather than their control points. */
    fun bounds(): MascotRect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        fun add(p: MascotPoint) {
            minX = minOf(minX, p.x); maxX = maxOf(maxX, p.x)
            minY = minOf(minY, p.y); maxY = maxOf(maxY, p.y)
        }
        var current = MascotPoint.Zero
        for (cmd in commands) {
            when (cmd) {
                is PathCmd.Move -> { current = cmd.to; add(current) }
                is PathCmd.Line -> { current = cmd.to; add(current) }
                is PathCmd.Cubic -> {
                    val steps = 24
                    for (i in 1..steps) {
                        val t = i / steps.toFloat()
                        val u = 1 - t
                        add(
                            MascotPoint(
                                u * u * u * current.x + 3 * u * u * t * cmd.c1.x + 3 * u * t * t * cmd.c2.x + t * t * t * cmd.to.x,
                                u * u * u * current.y + 3 * u * u * t * cmd.c1.y + 3 * u * t * t * cmd.c2.y + t * t * t * cmd.to.y
                            )
                        )
                    }
                    current = cmd.to
                }
                PathCmd.Close -> Unit
            }
        }
        if (minX > maxX) return MascotRect(0f, 0f, 0f, 0f)
        return MascotRect(minX, minY, maxX, maxY)
    }

    companion object {
        /** Control-point distance for a quarter circle drawn as one cubic. */
        private const val KAPPA = 0.5522847498f

        fun polygon(points: List<MascotPoint>, closed: Boolean = true): MascotPathSpec {
            if (points.isEmpty()) return MascotPathSpec(emptyList())
            val cmds = ArrayList<PathCmd>(points.size + 2)
            cmds.add(PathCmd.Move(points[0]))
            for (i in 1 until points.size) cmds.add(PathCmd.Line(points[i]))
            if (closed) cmds.add(PathCmd.Close)
            return MascotPathSpec(cmds)
        }

        /** An ellipse around [center], optionally leaned by [angle] radians, drawn as four cubics. */
        fun ellipse(center: MascotPoint, rx: Float, ry: Float, angle: Float = 0f): MascotPathSpec {
            val cosA = cos(angle)
            val sinA = sin(angle)
            fun p(x: Float, y: Float) = MascotPoint(center.x + x * cosA - y * sinA, center.y + x * sinA + y * cosA)
            val kx = rx * KAPPA
            val ky = ry * KAPPA
            return MascotPathSpec(
                listOf(
                    PathCmd.Move(p(rx, 0f)),
                    PathCmd.Cubic(p(rx, ky), p(kx, ry), p(0f, ry)),
                    PathCmd.Cubic(p(-kx, ry), p(-rx, ky), p(-rx, 0f)),
                    PathCmd.Cubic(p(-rx, -ky), p(-kx, -ry), p(0f, -ry)),
                    PathCmd.Cubic(p(kx, -ry), p(rx, -ky), p(rx, 0f)),
                    PathCmd.Close
                )
            )
        }

        fun ellipseIn(rect: MascotRect): MascotPathSpec =
            ellipse(MascotPoint((rect.left + rect.right) / 2f, rect.midY), rect.width / 2f, rect.height / 2f)

        /** A rounded rectangle with corner radii ([rx], [ry]), clamped to half the sides. */
        fun roundedRect(rect: MascotRect, rx: Float, ry: Float): MascotPathSpec {
            val cx = minOf(rx, rect.width / 2f)
            val cy = minOf(ry, rect.height / 2f)
            val kx = cx * KAPPA
            val ky = cy * KAPPA
            val l = rect.left
            val t = rect.top
            val r = rect.right
            val b = rect.bottom
            return MascotPathSpec(
                listOf(
                    PathCmd.Move(MascotPoint(l + cx, t)),
                    PathCmd.Line(MascotPoint(r - cx, t)),
                    PathCmd.Cubic(MascotPoint(r - cx + kx, t), MascotPoint(r, t + cy - ky), MascotPoint(r, t + cy)),
                    PathCmd.Line(MascotPoint(r, b - cy)),
                    PathCmd.Cubic(MascotPoint(r, b - cy + ky), MascotPoint(r - cx + kx, b), MascotPoint(r - cx, b)),
                    PathCmd.Line(MascotPoint(l + cx, b)),
                    PathCmd.Cubic(MascotPoint(l + cx - kx, b), MascotPoint(l, b - cy + ky), MascotPoint(l, b - cy)),
                    PathCmd.Line(MascotPoint(l, t + cy)),
                    PathCmd.Cubic(MascotPoint(l, t + cy - ky), MascotPoint(l + cx - kx, t), MascotPoint(l + cx, t)),
                    PathCmd.Close
                )
            )
        }
    }
}
