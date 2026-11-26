/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.RingerMorph
import com.agupta07505.smartisland.util.SystemAnimScale

/**
 * The look of an opposite event taking over a running pulse ([com.agupta07505.smartisland.model.CollapsedPulse.morphs]):
 * the glyph changes its tint (and slash or icon), the pulse's texts cross-fade, nothing slides in again
 */
internal const val PULSE_MORPH_MS = 260

/** Whether [this] card can be one half of an opposite pair, the system cards of the battery and the connections. */
internal fun IslandNotification.canMorph(): Boolean = mode == IslandMode.Battery || mode == IslandMode.Bluetooth

/** The animation of 1 morph step, a snap when the system animations are off. */
@Composable
internal fun <T> pulseMorphSpec(): FiniteAnimationSpec<T> =
    if (SystemAnimScale.animationsOff()) snap() else tween(PULSE_MORPH_MS)

/** [target] eased to over the morph, the first value is shown as it is. */
@Composable
internal fun morphColor(target: Color): Color =
    animateColorAsState(targetValue = target, animationSpec = pulseMorphSpec(), label = "PulseMorphColor").value

/**
 * The direction the card's morph goes, [RingerMorph.direction] from the category it had to the one it has now:
 * 1 louder, -1 quieter, 0 for any other change. Read it in the same composition that gets the new category
 */
@Composable
internal fun rememberMorphDirection(category: String?): Int {
    val last = remember { arrayOf(category) }
    val direction = RingerMorph.direction(last[0], category)
    SideEffect { last[0] = category }
    return direction
}

/** A louder step rises (the new content comes up from below, the old leaves upward), a quieter one sinks. */
private fun directionalTransition(
    direction: Int,
    fade: FiniteAnimationSpec<Float>,
    slide: FiniteAnimationSpec<IntOffset>
): ContentTransform {
    val rising = direction > 0
    val enter = fadeIn(fade) + scaleIn(fade, if (rising) 0.8f else 1.2f) +
        slideInVertically(slide) { h -> if (rising) h / 2 else -h / 2 }
    val exit = fadeOut(fade) + scaleOut(fade, if (rising) 1.2f else 0.8f) +
        slideOutVertically(slide) { h -> if (rising) -h / 2 else h / 2 }
    return enter togetherWith exit
}

/**
 * Cross-fades to [target] when it changes and [enabled]; otherwise draws it as it is, with no wrapper. A non-zero
 * [direction] ([rememberMorphDirection]) makes the change rise or sink instead of only fading
 */
@Composable
internal fun <T> MorphSwap(
    target: T,
    enabled: Boolean = true,
    direction: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    if (!enabled) {
        content(target)
        return
    }
    if (direction == 0) {
        Crossfade(
            targetState = target,
            modifier = modifier,
            animationSpec = pulseMorphSpec(),
            label = "PulseMorphSwap"
        ) { shown -> content(shown) }
        return
    }
    val fade = pulseMorphSpec<Float>()
    val slide = pulseMorphSpec<IntOffset>()
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = { directionalTransition(direction, fade, slide) },
        label = "PulseMorphDirectional"
    ) { shown -> content(shown) }
}
