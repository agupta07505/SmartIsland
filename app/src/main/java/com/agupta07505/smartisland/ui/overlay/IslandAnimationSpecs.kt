/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp

/**
 * Centralised animation specifications used by the Dynamic Island overlay.
 * Keeps the main composable thinner and makes tuning easier.
 */
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
