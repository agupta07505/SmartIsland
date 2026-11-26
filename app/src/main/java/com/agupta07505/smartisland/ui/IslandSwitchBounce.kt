/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agupta07505.smartisland.model.SwipeFeel

/**
 * Small squash-and-bounce when the user switches cards (not when a swipe's own slide did it, see [skipBounce]), pill
 * and cutouts multiply it into their scale
 */
@Composable
internal fun rememberSwitchScaleAnim(selectedIndex: Int, skipBounce: () -> Boolean =
        { false }): Animatable<Float, AnimationVector1D> {
    val switchScaleAnim = remember { Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }
    var lastSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (isInitialComposition) {
            isInitialComposition = false
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (lastSelectedIndex != selectedIndex) {
            lastSelectedIndex = selectedIndex
            // A card that slid in from a swipe already has its motion, the squash on top only muddies it
            if (skipBounce()) return@LaunchedEffect
            switchScaleAnim.animateTo(
                targetValue = SwipeFeel.SWITCH_SQUASH_SCALE,
                animationSpec = tween(40, easing = FastOutSlowInEasing)
            )
            switchScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = SwipeFeel.SWITCH_RETURN_DAMPING_RATIO,
                    stiffness = SwipeFeel.SWITCH_RETURN_STIFFNESS
                )
            )
        }
    }
    return switchScaleAnim
}
