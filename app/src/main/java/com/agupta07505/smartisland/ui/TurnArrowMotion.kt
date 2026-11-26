/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.agupta07505.smartisland.util.TurnArrow
import com.agupta07505.smartisland.util.TurnDirection

/** The navigation arrow as it is drawn now: its rotation (degrees) and the pop it gives when the maneuver changes. */
internal class TurnArrowMotion(val angleDeg: Float, val scale: Float)

/**
 * Follows [direction] as the navigation app updates it: the arrow swings to the new turn by the shortest way on a
 * spring and pops once, so a change on the small glyph is seen. The first direction is shown as it is
 */
@Composable
internal fun rememberTurnArrowMotion(direction: TurnDirection): TurnArrowMotion {
    val target = remember { floatArrayOf(direction.arrowAngleDeg) }
    val angleTarget = remember(direction) { TurnArrow.nextAngle(target[0], direction).also { target[0] = it } }
    val angle by animateFloatAsState(
        targetValue = angleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "turnArrowAngle"
    )
    val pop = remember { Animatable(1f) }
    val shown = remember { arrayOfNulls<TurnDirection>(1) }
    LaunchedEffect(direction) {
        val previous = shown[0]
        shown[0] = direction
        if (previous != null && previous != direction) {
            pop.animateTo(TURN_ARROW_POP_SCALE, tween(TURN_ARROW_POP_MS))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    return TurnArrowMotion(angle, pop.value)
}

private const val TURN_ARROW_POP_SCALE = 1.3f
private const val TURN_ARROW_POP_MS = 90
