/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.buddy.geometry.MascotGeometry
import com.agupta07505.smartisland.buddy.model.MascotColor
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotPoint
import com.agupta07505.smartisland.buddy.model.MascotSize
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.face

/** One mood's two eye outlines, as the 24 points every eye shares and as ready-made paths. */
private class EyeSet(val left: List<MascotPoint>, val right: List<MascotPoint>) {
    val leftPath: Path = MascotGeometry.smoothClosedPath(left).toPath()
    val rightPath: Path = MascotGeometry.smoothClosedPath(right).toPath()
}

/** Everything about one look at one size that does not change per frame. */
private class MascotShapes(look: MascotLook, size: Float) {
    val robot = look.style == MascotStyle.Robot
    val face = look.face
    val body: Path
    val ears: Path
    val antenna: Path
    val bulb: Path
    val visor: Path
    val shine: Path
    val leftCenter = Offset(face.leftEye.x * size, face.leftEye.y * size)
    val rightCenter = Offset(face.rightEye.x * size, face.rightEye.y * size)
    val figureShift = MascotGeometry.figureOffset(look) * size
    private val eyeSize = MascotSize(face.eyeSize.width * size, face.eyeSize.height * size)

    // Built once per mood, on first use; a mood the buddy never shows costs nothing.
    private val eyes = HashMap<MascotMood, EyeSet>()

    fun eyesFor(mood: MascotMood): EyeSet = eyes.getOrPut(mood) {
        val e = mood.expression
        EyeSet(
            MascotGeometry.eyePoints(e.left, eyeSize, right = false),
            MascotGeometry.eyePoints(e.right, eyeSize, right = true)
        )
    }

    init {
        if (robot) {
            val parts = MascotGeometry.robot(size)
            body = parts.head.toPath()
            ears = parts.ears.toPath()
            antenna = parts.antenna.toPath()
            bulb = parts.bulb.toPath()
            visor = parts.visor.toPath()
            shine = Path()
        } else {
            body = MascotGeometry.body(look.shape, size).toPath()
            ears = Path(); antenna = Path(); bulb = Path(); visor = Path()
            shine = MascotGeometry.shine(face, size).toPath()
        }
    }
}

private fun lerpPoints(a: List<MascotPoint>, b: List<MascotPoint>, t: Float): List<MascotPoint> =
    a.indices.map { MascotPoint(a[it].x + (b[it].x - a[it].x) * t, a[it].y + (b[it].y - a[it].y) * t) }

/**
 * Draws the buddy in a square of [size]. [pose] is read only inside the draw scope. The figure is
 * centred on its own middle (not its box's, which keeps headroom for the robot's antenna) so it lines
 * up with the pill's centreline.
 */
@Composable
fun MascotCanvas(
    look: MascotLook,
    pose: MascotPose,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val shapes = remember(look, sizePx) { MascotShapes(look, sizePx) }
    val light = look.palette.light.toColor()
    val shade = look.palette.shade.toColor()
    Canvas(modifier = modifier.size(size)) {
        drawBuddy(shapes, light, shade, pose, sizePx)
    }
}

private fun DrawScope.drawBuddy(
    s: MascotShapes,
    light: Color,
    shade: Color,
    pose: MascotPose,
    size: Float
) {
    val progress = pose.morph.value
    val from = pose.faceFrom
    val to = pose.faceTo
    val fromExpr = from.expression
    val toExpr = to.expression
    val gazeX = fromExpr.gaze.x + (toExpr.gaze.x - fromExpr.gaze.x) * progress
    val gazeY = fromExpr.gaze.y + (toExpr.gaze.y - fromExpr.gaze.y) * progress
    val tilt = fromExpr.tilt + (toExpr.tilt - fromExpr.tilt) * progress + pose.tilt.value
    // A stroll adds its own hop, landing squash and sideways look (its x is placed by the layer).
    val walk = pose.walk()
    val walkLift = (walk?.lift ?: 0f) * density
    val walkSquash = walk?.squash ?: 0f
    val walkGaze = (walk?.gaze ?: 0f) * size

    // Lift the figure by its own offset so its middle sits at the box's middle.
    translate(left = 0f, top = -s.figureShift) {
        // Squash and tilt are about the figure's base, as in the reference rig.
        val pivot = Offset(size / 2f, size * 0.92f)
        translate(top = -(pose.lift.value + walkLift)) {
            rotate(degrees = Math.toDegrees(tilt.toDouble()).toFloat(), pivot = pivot) {
                val squash = pose.squash.value + walkSquash
                scale(scaleX = 1f + 0.8f * squash, scaleY = 1f - squash, pivot = pivot) {
                    if (s.robot) {
                        val bulbColor = lerp(shade, light, pose.bulbLit.value)
                        drawPath(s.ears, shade)
                        drawPath(s.antenna, shade)
                        if (pose.glow.value > 0f) {
                            drawCircle(
                                light.copy(alpha = 0.55f * pose.glow.value),
                                radius = size * 0.13f,
                                center = Offset(size * 0.5f, size * 0.11f)
                            )
                        }
                        drawPath(s.bulb, bulbColor)
                    }
                    drawPath(s.body, Brush.verticalGradient(listOf(light, shade), startY = 0f, endY = size))
                    if (s.robot) {
                        drawPath(s.visor, MascotColor.Visor.toColor())
                    } else {
                        drawPath(s.shine, MascotColor.Shine.toColor())
                    }
                    val gaze = Offset(gazeX * size + pose.gazeX.value + walkGaze, gazeY * size + pose.gazeY.value)
                    val eyeColor = if (s.robot) light else MascotColor.Ink.toColor()
                    val leftPath: Path
                    val rightPath: Path
                    if (progress >= 1f || from == to) {
                        val set = s.eyesFor(to)
                        leftPath = set.leftPath
                        rightPath = set.rightPath
                    } else {
                        val a = s.eyesFor(from)
                        val b = s.eyesFor(to)
                        leftPath = MascotGeometry.smoothClosedPath(lerpPoints(a.left, b.left, progress)).toPath()
                        rightPath = MascotGeometry.smoothClosedPath(lerpPoints(a.right, b.right, progress)).toPath()
                    }
                    drawEye(leftPath, s.leftCenter + gaze, pose, eyeColor, s.robot, size)
                    drawEye(rightPath, s.rightCenter + gaze, pose, eyeColor, s.robot, size)
                }
            }
        }
    }
}

private fun DrawScope.drawEye(path: Path, center: Offset, pose: MascotPose, color: Color, glow: Boolean, size: Float) {
    translate(left = center.x, top = center.y) {
        val beat = pose.eyeScale.value
        scale(scaleX = beat, scaleY = beat * pose.blink.value, pivot = Offset.Zero) {
            if (glow) {
                // A lit visor eye glows softly; a thin translucent halo stands in for the reference's shadow.
                drawPath(path, color.copy(alpha = 0.35f), style = Stroke(width = (size * 0.05f).coerceAtLeast(1f)))
            }
            drawPath(path, color)
        }
    }
}
