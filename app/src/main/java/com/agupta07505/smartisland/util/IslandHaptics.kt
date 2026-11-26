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
import com.agupta07505.smartisland.model.StableId
import com.agupta07505.smartisland.model.enumById

/** Never throws: a phone without a motor does nothing. */
object IslandHaptics {
    fun play(context: Context, strength: HapticStrength, kind: HapticKind) {
        val spec = strength.spec(kind) ?: return
        runCatchingLogged("IslandHaptics", "Haptic effect failed") {
            val vibrator = vibratorOf(context) ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && strength == HapticStrength.NORMAL) {
                val id = when (spec.effect) {
                    Effect.Tick -> VibrationEffect.EFFECT_TICK
                    Effect.Click -> VibrationEffect.EFFECT_CLICK
                    Effect.HeavyClick -> VibrationEffect.EFFECT_HEAVY_CLICK
                }
                vibrator.vibrate(VibrationEffect.createPredefined(id))
            } else {
                // light and Strong scale the amplitude, which predefined effects cannot do
                val amplitude = if (vibrator.hasAmplitudeControl()) spec.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(spec.durationMs, amplitude))
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

enum class HapticKind { Expand, Collapse, Swipe, Hold, Merge, Lock }

/** Amplitude is 1..255; [effect] is preferred on Android 10+. */
data class HapticSpec(val durationMs: Long, val amplitude: Int, val effect: Effect)

/** Mirrors the system's predefined effects so this file stays free of Android classes. */
enum class Effect { Tick, Click, HeavyClick }

/** [OFF] never vibrates. */
enum class HapticStrength(override val id: String, val scale: Float) : StableId {
    OFF("off", 0f),
    LIGHT("light", 0.55f),
    NORMAL("normal", 1f),
    STRONG("strong", 1.5f);

    fun spec(kind: HapticKind): HapticSpec? {
        if (this == OFF) return null
        val (duration, amplitude, effect) = when (kind) {
            HapticKind.Expand -> Triple(22L, 150, Effect.Click)
            HapticKind.Collapse -> Triple(14L, 110, Effect.Tick)
            HapticKind.Swipe -> Triple(12L, 130, Effect.Tick)
            HapticKind.Hold -> Triple(60L, 200, Effect.HeavyClick)
            HapticKind.Merge -> Triple(10L, 90, Effect.Tick)
            // Dull and slightly longer, unlike the crisp tick
            HapticKind.Lock -> Triple(28L, 100, Effect.Click)
        }
        return HapticSpec(duration, (amplitude * scale).toInt().coerceIn(1, 255), effect)
    }

    companion object {
        const val ID_OFF = "off"
        const val ID_LIGHT = "light"
        const val ID_NORMAL = "normal"
        const val ID_STRONG = "strong"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): HapticStrength = enumById(id, NORMAL)
    }
}
