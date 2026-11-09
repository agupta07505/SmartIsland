/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.agupta07505.smartisland.model.PulseBubble
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.delay

private val SWELL_EASING = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** The pulse's peak starts this long after the pulse, the swell goes with it. */
private const val SWELL_START_DELAY_MS = 60

/** 1 swell shared by the bubble and the element of the wide pulse nearest to it. */
internal class BubbleSwell {
    val progress = Animatable(0f)

    /** 1 at rest, [PulseBubble.SWELL_SCALE] at the peak. */
    val scale: State<Float> = derivedStateOf { PulseBubble.scale(progress.value) }
}

@Composable
internal fun rememberBubbleSwell(): BubbleSwell = remember { BubbleSwell() }

/** Plays the swell when a pulse starts or the card the bubble holds changes under it; speed and reduced motion apply. */
@Composable
internal fun BubbleSwellEffect(
    swell: BubbleSwell,
    pulsingId: String?,
    heldKey: String?,
    animSpeed: AnimationSpeed,
    /** The id the running pulse took over from as an opposite event, null otherwise. */
    morphedFromId: String? = null
) {
    val last = remember { arrayOfNulls<String>(2) }
    LaunchedEffect(pulsingId, heldKey) {
        val runs = PulseBubble.swells(pulsingId, last[0], heldKey, last[1], morphedFromId)
        last[0] = pulsingId
        last[1] = heldKey
        val spec = tween<Float>(animSpeed.durationMs(PulseBubble.SWELL_MS), easing = SWELL_EASING)
        if (!runs) {
            // a swell cut short by the end of the pulse still comes back to rest
            if (swell.progress.value != 0f) swell.progress.animateTo(0f, spec)
            return@LaunchedEffect
        }
        delay(animSpeed.durationMs(SWELL_START_DELAY_MS).toLong())
        swell.progress.animateTo(1f, spec)
        swell.progress.animateTo(0f, spec)
    }
}
