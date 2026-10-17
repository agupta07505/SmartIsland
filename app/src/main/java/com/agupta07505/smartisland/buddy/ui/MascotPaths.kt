/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.ui

import androidx.compose.ui.graphics.Path
import com.agupta07505.smartisland.buddy.geometry.MascotPathSpec
import com.agupta07505.smartisland.buddy.geometry.PathCmd
import com.agupta07505.smartisland.buddy.model.MascotColor
import androidx.compose.ui.graphics.Color

/** Replays path commands into a Compose [Path]. */
fun MascotPathSpec.toPath(): Path {
    val path = Path()
    for (cmd in commands) {
        when (cmd) {
            is PathCmd.Move -> path.moveTo(cmd.to.x, cmd.to.y)
            is PathCmd.Line -> path.lineTo(cmd.to.x, cmd.to.y)
            is PathCmd.Cubic -> path.cubicTo(cmd.c1.x, cmd.c1.y, cmd.c2.x, cmd.c2.y, cmd.to.x, cmd.to.y)
            PathCmd.Close -> path.close()
        }
    }
    return path
}

fun MascotColor.toColor(): Color = Color(red, green, blue, alpha)
