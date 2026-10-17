/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import com.agupta07505.smartisland.buddy.runtime.MascotBlink

/**
 * Everything about the buddy that changes from frame to frame. The canvas reads these inside its draw
 * lambda only, so a frame costs a draw, never a recomposition.
 */
@Stable
class MascotPose {
    /** Eye height share: 1 open, [MascotBlink.SHUT_SCALE] shut. */
    val blink = Animatable(1f)
    /** Hop height in px (used by reactions and visits). */
    val lift = Animatable(0f)
    /** Positive flattens, negative stretches. */
    val squash = Animatable(0f)
    val tilt = Animatable(0f)
    val gazeX = Animatable(0f)
    val gazeY = Animatable(0f)

    /** Plays one blink (or a double blink). With [reduceMotion] the eyes just shut and open, with no motion between. */
    suspend fun blink(double: Boolean, reduceMotion: Boolean) {
        val steps = if (double) MascotBlink.double else MascotBlink.single
        if (reduceMotion) {
            blink.snapTo(MascotBlink.SHUT_SCALE)
            kotlinx.coroutines.delay(if (double) 260 else 120)
            blink.snapTo(1f)
            return
        }
        for (step in steps) {
            val easing = if (step.scale < 1f) FastOutSlowInEasing else LinearOutSlowInEasing
            if (step.scale == blink.value) {
                kotlinx.coroutines.delay(step.durationMs.toLong())
            } else {
                blink.animateTo(step.scale, tween(step.durationMs, easing = easing))
            }
        }
    }

    suspend fun reset() {
        blink.snapTo(1f)
        lift.snapTo(0f)
        squash.snapTo(0f)
        tilt.snapTo(0f)
        gazeX.snapTo(0f)
        gazeY.snapTo(0f)
    }
}
