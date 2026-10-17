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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotReaction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays [values] at [times] (shares of [ms], first 0) on this animatable. A last time under 1 holds
 * the last value until the end, as the reference's keyframes do.
 */
internal suspend fun Animatable<Float, AnimationVector1D>.keys(
    values: List<Float>,
    times: List<Float>,
    ms: Int,
    easing: Easing = FastOutSlowInEasing
) {
    snapTo(values[0])
    val spec = keyframes<Float> {
        durationMillis = ms
        for (i in 1 until values.size) {
            val at = (times[i] * ms).toInt().coerceIn(1, ms)
            if (at < ms) values[i] at at using easing
        }
    }
    animateTo(values.last(), spec)
}

private suspend fun MascotPose.hop(h: Float) = coroutineScope {
    launch { lift.keys(listOf(0f, h, 0f, h * 0.3f, 0f), listOf(0f, 0.32f, 0.6f, 0.78f, 1f), 620) }
    launch {
        squash.keys(
            listOf(0f, -0.12f, 0f, 0.14f, 0f, 0.06f, 0f),
            listOf(0f, 0.12f, 0.3f, 0.62f, 0.72f, 0.84f, 1f), 620
        )
    }
}

private suspend fun MascotPose.pop() =
    squash.keys(listOf(0f, -0.14f, 0.06f, 0f), listOf(0f, 0.35f, 0.7f, 1f), 340)

private suspend fun MascotPose.wobble() =
    tilt.keys(listOf(0f, 0.12f, -0.1f, 0.06f, 0f), listOf(0f, 0.25f, 0.5f, 0.75f, 1f), 500)

private suspend fun MascotPose.heartbeat() =
    eyeScale.keys(listOf(1f, 1.3f, 1f, 1.22f, 1f), listOf(0f, 0.2f, 0.45f, 0.65f, 1f), 700)

/** A slow stretch up and back, as a yawn or waking up. */
private suspend fun MascotPose.stretch(ms: Int) =
    squash.keys(listOf(0f, -0.12f, -0.12f, 0.05f, 0f), listOf(0f, 0.35f, 0.6f, 0.82f, 1f), ms)

private suspend fun MascotPose.glowBulb(ms: Int) =
    glow.keys(listOf(0f, 1f, 1f, 0f), listOf(0f, 0.12f, 0.7f, 1f), ms)

/**
 * A short reaction over the face it keeps. [lift] is how high a hop may take it, in px (the pill's
 * height limits it). With [reduceMotion] it only changes face, as the reference does.
 */
suspend fun MascotPose.react(reaction: MascotReaction, lift: Float, reduceMotion: Boolean) {
    // Whatever it is, it wakes up for it (a wake-up does its own waking below).
    val dozed = baseMood == MascotMood.Sleepy
    if (dozed && reaction != MascotReaction.WakeUp) setBaseMood(MascotMood.Idle, reduceMotion)
    val lengthMs = (reaction.lengthSec * 1000f).toInt()
    try {
        if (reduceMotion) {
            when (reaction) {
                MascotReaction.Celebrate, MascotReaction.WakeUp -> flashFace(MascotMood.Happy, 1000, true)
                MascotReaction.Love -> flashFace(MascotMood.Love, 1200, true)
                MascotReaction.Surprised, MascotReaction.Flash -> flashFace(MascotMood.Surprised, 800, true)
                MascotReaction.Confused -> flashFace(MascotMood.Confused, 1100, true)
                MascotReaction.Yawn, MascotReaction.Hush -> flashFace(MascotMood.Sleepy, 1100, true)
                MascotReaction.Perk, MascotReaction.Bounce -> flashFace(MascotMood.Alert, 1000, true)
                MascotReaction.Ready -> flashFace(MascotMood.Determined, 1000, true)
                MascotReaction.Groove -> flashFace(MascotMood.Happy, 1400, true)
            }
            return
        }
        coroutineScope {
            launch { glowBulb(lengthMs) }
            when (reaction) {
                MascotReaction.Celebrate -> {
                    launch { hop(lift) }
                    launch { flashFace(MascotMood.Happy, 1000, false) }
                }
                MascotReaction.Love -> {
                    launch { flashFace(MascotMood.Love, 1400, false) }
                    launch { delay(220); heartbeat() }
                }
                MascotReaction.Surprised -> {
                    launch { flashFace(MascotMood.Surprised, 900, false) }
                    launch { hop(lift * 0.7f) }
                }
                MascotReaction.Flash -> {
                    // Squeezed shut, then wide open, as a camera's flash goes off.
                    launch { blink.keys(listOf(1f, 0.06f, 0.06f, 1.18f, 1f), listOf(0f, 0.18f, 0.42f, 0.7f, 1f), 450) }
                    launch { pop() }
                }
                MascotReaction.Confused -> {
                    launch { flashFace(MascotMood.Confused, 1200, false) }
                    launch { wobble() }
                }
                MascotReaction.WakeUp -> {
                    // Heavy eyes, a stretch, and a glad face.
                    if (dozed) {
                        launch {
                            setBaseMood(MascotMood.Idle, false, animated = false)
                            bulbLit.snapTo(0f)
                            delay(350)
                            bulbLit.animateTo(1f, androidx.compose.animation.core.tween(300))
                        }
                    }
                    launch {
                        flashFace(MascotMood.Sleepy, 700, false)
                        flashFace(MascotMood.Happy, 900, false)
                    }
                    launch { delay(350); stretch(600) }
                }
                MascotReaction.Yawn -> {
                    launch { flashFace(MascotMood.Sleepy, 1200, false) }
                    launch { delay(100); stretch(900) }
                }
                MascotReaction.Perk -> {
                    launch { flashFace(MascotMood.Alert, 1000, false) }
                    launch { hop(lift * 0.55f) }
                }
                MascotReaction.Bounce -> {
                    val h = maxOf(1f, lift * 0.6f)
                    launch { flashFace(MascotMood.Alert, lengthMs, false) }
                    launch {
                        this@react.lift.keys(
                            listOf(0f, h, 0f, h * 0.7f, 0f, h * 0.15f, 0f),
                            listOf(0f, 0.14f, 0.32f, 0.46f, 0.63f, 0.75f, 0.88f), 950
                        )
                    }
                    launch {
                        squash.keys(
                            listOf(0f, -0.12f, 0f, 0.14f, -0.1f, 0f, 0.12f, 0f, 0.04f, 0f),
                            listOf(0f, 0.04f, 0.14f, 0.32f, 0.36f, 0.46f, 0.63f, 0.7f, 0.8f, 0.88f), 950
                        )
                    }
                }
                MascotReaction.Hush -> {
                    // Eyes shut for a moment as it ducks, then back.
                    launch { blink.keys(listOf(1f, 0.1f, 0.1f, 1f), listOf(0f, 0.22f, 0.78f, 1f), 950) }
                    launch { squash.keys(listOf(0f, 0.1f, 0.1f, -0.03f, 0f), listOf(0f, 0.25f, 0.7f, 0.88f, 1f), 950) }
                }
                MascotReaction.Ready -> {
                    launch { flashFace(MascotMood.Determined, 1000, false) }
                    // A nod: down a little, up past where it was, and settled.
                    launch { delay(120); squash.keys(listOf(0f, 0.1f, -0.06f, 0f), listOf(0f, 0.3f, 0.65f, 1f), 500) }
                }
                MascotReaction.Groove -> {
                    // Smiling eyes, a bob on every beat and a lean one way and the other.
                    launch { flashFace(MascotMood.Happy, lengthMs, false) }
                    val beats = 4
                    val steps = beats * 2
                    val h = maxOf(1f, lift * 0.45f)
                    val times = (0..steps).map { it.toFloat() / steps }
                    val ups = (0..steps).map { if (it % 2 == 1) h else 0f }
                    val leans = (0..steps).map { if (it % 2 == 1) (if (it % 4 == 1) 0.11f else -0.11f) else 0f }
                    val squashes = (0..steps).map {
                        if (it % 2 == 1) -0.05f else if (it == 0 || it == steps) 0f else 0.07f
                    }
                    launch { this@react.lift.keys(ups, times, lengthMs) }
                    launch { tilt.keys(leans, times, lengthMs) }
                    launch { squash.keys(squashes, times, lengthMs) }
                }
            }
        }
    } finally {
        // A reaction cut short (a newer one, or the island needing the room) must leave no motion behind.
        withContext(NonCancellable) { settle() }
    }
}
