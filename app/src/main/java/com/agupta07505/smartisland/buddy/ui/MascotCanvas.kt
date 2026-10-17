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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.buddy.geometry.MascotGeometry
import com.agupta07505.smartisland.buddy.model.MascotColor
import com.agupta07505.smartisland.buddy.model.MascotEye
import com.agupta07505.smartisland.buddy.model.MascotLook
import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotPoint
import com.agupta07505.smartisland.buddy.model.MascotSize
import com.agupta07505.smartisland.buddy.model.MascotStyle
import com.agupta07505.smartisland.buddy.model.face

/** Everything about one look at one size that does not change per frame. */
private class MascotShapes(look: MascotLook, size: Float, mood: MascotMood) {
    val robot = look.style == MascotStyle.Robot
    val face = look.face
    val body: Path
    val ears: Path
    val antenna: Path
    val bulb: Path
    val visor: Path
    val shine: Path
    val leftEye: Path
    val rightEye: Path
    val leftCenter = Offset(face.leftEye.x * size, face.leftEye.y * size)
    val rightCenter = Offset(face.rightEye.x * size, face.rightEye.y * size)
    val figureShift = MascotGeometry.figureOffset(look) * size
    val expression = mood.expression

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
        val eyeSize = MascotSize(face.eyeSize.width * size, face.eyeSize.height * size)
        // Eyes are built around their own centre so a blink can squash them about it.
        leftEye = MascotGeometry.eye(expression.left, eyeSize, MascotPoint.Zero, right = false).toPath()
        rightEye = MascotGeometry.eye(expression.right, eyeSize, MascotPoint.Zero, right = true).toPath()
    }
}

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
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.Idle
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val shapes = remember(look, sizePx, mood) { MascotShapes(look, sizePx, mood) }
    val light = look.palette.light.toColor()
    val shade = look.palette.shade.toColor()
    Canvas(modifier = modifier.size(size)) {
        drawBuddy(shapes, light, shade, look.palette.light.toColor(), pose, sizePx)
    }
}

private fun DrawScope.drawBuddy(
    s: MascotShapes,
    light: Color,
    shade: Color,
    lit: Color,
    pose: MascotPose,
    size: Float
) {
    // Lift the figure by its own offset so its middle sits at the box's middle.
    translate(left = 0f, top = -s.figureShift) {
        // Squash and tilt are about the figure's base, as in the reference rig.
        val pivot = Offset(size / 2f, size * 0.94f)
        translate(top = -pose.lift.value) {
            rotate(degrees = Math.toDegrees(pose.tilt.value.toDouble()).toFloat(), pivot = pivot) {
                val squash = pose.squash.value
                scale(scaleX = 1f + squash, scaleY = 1f - squash, pivot = pivot) {
                    if (s.robot) {
                        drawPath(s.ears, shade)
                        drawPath(s.antenna, shade)
                        drawPath(s.bulb, lit)
                    }
                    drawPath(s.body, Brush.verticalGradient(listOf(light, shade), startY = 0f, endY = size))
                    if (s.robot) {
                        drawPath(s.visor, MascotColor.Visor.toColor())
                    } else {
                        drawPath(s.shine, MascotColor.Shine.toColor())
                    }
                    val gaze = Offset(
                        (s.expression.gaze.x * size) + pose.gazeX.value,
                        (s.expression.gaze.y * size) + pose.gazeY.value
                    )
                    val eyeColor = if (s.robot) lit else MascotColor.Ink.toColor()
                    drawEye(s.leftEye, s.leftCenter + gaze, pose.blink.value, eyeColor, s.robot, size)
                    drawEye(s.rightEye, s.rightCenter + gaze, pose.blink.value, eyeColor, s.robot, size)
                }
            }
        }
    }
}

private fun DrawScope.drawEye(path: Path, center: Offset, blink: Float, color: Color, glow: Boolean, size: Float) {
    translate(left = center.x, top = center.y) {
        scale(scaleX = 1f, scaleY = blink, pivot = Offset.Zero) {
            if (glow) {
                // A lit visor eye glows softly; a thin translucent halo stands in for the reference's shadow.
                drawPath(path, color.copy(alpha = 0.35f), style = Stroke(width = (size * 0.05f).coerceAtLeast(1f)))
            }
            drawPath(path, color)
        }
    }
}
