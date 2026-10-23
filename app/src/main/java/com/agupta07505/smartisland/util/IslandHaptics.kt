/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Never throws: a phone without a motor does nothing. */
object IslandHaptics {
    fun play(context: Context, strength: HapticStrength, kind: HapticKind) {
        val spec = strength.spec(kind) ?: return
        runCatching {
            val vibrator = vibratorOf(context) ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && strength == HapticStrength.NORMAL) {
                val id = when (spec.effect) {
                    Effect.Tick -> VibrationEffect.EFFECT_TICK
                    Effect.Click -> VibrationEffect.EFFECT_CLICK
                    Effect.HeavyClick -> VibrationEffect.EFFECT_HEAVY_CLICK
                }
                vibrator.vibrate(VibrationEffect.createPredefined(id))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Light and Strong scale the amplitude, which predefined effects cannot do.
                val amplitude = if (vibrator.hasAmplitudeControl()) spec.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(spec.durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(spec.durationMs)
            }
        }
    }

    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
