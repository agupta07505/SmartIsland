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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.agupta07505.smartisland.buddy.model.MascotMood
import com.agupta07505.smartisland.buddy.model.MascotPath
import com.agupta07505.smartisland.buddy.runtime.MascotBlink
import kotlinx.coroutines.delay

/**
 * Everything about the buddy that changes from frame to frame. The canvas reads these inside its draw
 * lambda only, so a frame costs a draw, never a recomposition. Lengths are in px.
 */
@Stable
class MascotPose {
    /** Eye height share: 1 open, [MascotBlink.SHUT_SCALE] shut. */
    val blink = Animatable(1f)
    /** Hop height, positive up. */
    val lift = Animatable(0f)
    /** Positive flattens it on the ground, negative stretches it up; its bottom stays put. */
    val squash = Animatable(0f)
    /** Extra lean in radians on top of the face's own, clockwise. */
    val tilt = Animatable(0f)
    val gazeX = Animatable(0f)
    val gazeY = Animatable(0f)
    /** Whole-eye scale (the heart-eyes' beat). */
    val eyeScale = Animatable(1f)
    /** The robot bulb's soft glow while a reaction plays. */
    val glow = Animatable(0f)
    /** The robot bulb: 1 lit, 0 dark (asleep). */
    val bulbLit = Animatable(1f)

    // The face is a morph between two moods: progress 1 shows [faceTo] exactly.
    val morph = Animatable(1f)
    var faceFrom by mutableStateOf(MascotMood.Idle)
    var faceTo by mutableStateOf(MascotMood.Idle)

    /** The face it keeps at rest; reactions flash another over it and come back to this. */
    var baseMood by mutableStateOf(MascotMood.Idle)

    // A stroll under way: where it is along its path, read in the layout and draw phases only.
    var path by mutableStateOf<MascotPath?>(null)
    /** Seconds into [path]. */
    val visitTime = Animatable(0f)
    private val scratch = MascotPath.Sample()

    /** Where the stroll has it right now, or null when it is not walking. Reuses one sample: read it, don't keep it. */
    fun walk(): MascotPath.Sample? = path?.at(visitTime.value, scratch)

    private fun shownMood(): MascotMood = if (morph.value >= 0.5f) faceTo else faceFrom

    /** Turns to [mood] over [ms] with the reference's settle curve. */
    suspend fun morphTo(mood: MascotMood, ms: Int = 220) {
        if (ms <= 0) {
            faceFrom = mood; faceTo = mood
            morph.snapTo(1f)
            return
        }
        faceFrom = shownMood()
        faceTo = mood
        morph.snapTo(0f)
        morph.animateTo(1f, tween(ms, easing = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)))
    }

    /** Changes the face it keeps. Waking from sleepy pops and relights the robot's bulb. */
    suspend fun setBaseMood(mood: MascotMood, reduceMotion: Boolean, animated: Boolean = true) {
        if (mood == baseMood) return
        val wasSleepy = baseMood == MascotMood.Sleepy
        baseMood = mood
        val quick = reduceMotion || !animated
        morphTo(mood, if (quick) 0 else 220)
        val lit = if (mood == MascotMood.Sleepy) 0f else 1f
        if (quick) bulbLit.snapTo(lit) else bulbLit.animateTo(lit, tween(if (lit == 1f) 250 else 600))
        if (wasSleepy && mood == MascotMood.Idle && !quick) {
            squash.keys(listOf(0f, -0.14f, 0.06f, 0f), listOf(0f, 0.35f, 0.7f, 1f), 340)
        }
    }

    /** Shows [mood] for [ms]: in over 15%, held for 70%, back to the kept face over 15%. */
    suspend fun flashFace(mood: MascotMood, ms: Int, reduceMotion: Boolean) {
        if (reduceMotion) {
            morphTo(mood, 0)
            delay(ms.toLong())
            morphTo(baseMood, 0)
            return
        }
        val move = (ms * 0.15f).toInt()
        morphTo(mood, move)
        delay((ms * 0.7f).toLong())
        morphTo(baseMood, move)
    }

    /** Plays one blink (or a double blink). With [reduceMotion] the eyes just shut and open, with no motion between. */
    suspend fun blink(double: Boolean, reduceMotion: Boolean) {
        val steps = if (double) MascotBlink.double else MascotBlink.single
        if (reduceMotion) {
            blink.snapTo(MascotBlink.SHUT_SCALE)
            delay(if (double) 260 else 120)
            blink.snapTo(1f)
            return
        }
        for (step in steps) {
            val easing = if (step.scale < 1f) FastOutSlowInEasing else LinearOutSlowInEasing
            if (step.scale == blink.value) {
                delay(step.durationMs.toLong())
            } else {
                blink.animateTo(step.scale, tween(step.durationMs, easing = easing))
            }
        }
    }

    /** Takes every motion off, back to the kept face. */
    suspend fun settle() {
        blink.snapTo(1f)
        lift.snapTo(0f)
        squash.snapTo(0f)
        tilt.snapTo(0f)
        gazeX.snapTo(0f)
        gazeY.snapTo(0f)
        eyeScale.snapTo(1f)
        glow.snapTo(0f)
        morphTo(baseMood, 0)
    }
}
