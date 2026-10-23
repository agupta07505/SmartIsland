/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class HapticKind { Expand, Collapse, Swipe, Hold, Merge, Lock }

/** Amplitude is 1..255; [effect] is preferred on Android 10+. */
data class HapticSpec(val durationMs: Long, val amplitude: Int, val effect: Effect)

/** Mirrors the system's predefined effects so this file stays free of Android classes. */
enum class Effect { Tick, Click, HeavyClick }

/**
 * [OFF] never vibrates. The other two scale the amplitude of the fallback buzz; on Android 10+ the system's own
 * tick/click effects are used instead.
 */
enum class HapticStrength(val id: String, val scale: Float) {
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
            // Dull and slightly longer, unlike the crisp tick.
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

        fun fromId(id: String?): HapticStrength = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
