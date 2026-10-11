/*
 * Realistic Dynamic Island Fork
 * Package: com.srcpolygot.dynamicisland
 */

package com.srcpolygot.dynamicisland.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

object IslandAnimationSpecs {

    val sizeSpring = spring<Dp>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

    val sizeSpringFloat = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

    val heightSpring = spring<Dp>(
        dampingRatio = 0.76f,
        stiffness = 520f
    )

    val alphaTween = tween<Float>(
        durationMillis = 190,
        easing = FastOutSlowInEasing
    )

    val secondaryScaleSpring = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = 480f
    )

    val secondarySizeSpring = spring<Dp>(
        dampingRatio = 0.75f,
        stiffness = 520f
    )

    val switchBounceSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 650f
    )
}
