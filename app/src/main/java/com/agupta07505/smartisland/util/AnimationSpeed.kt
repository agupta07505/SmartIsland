/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

enum class AnimationSpeed(val id: String, val stiffnessScale: Float, val durationScale: Float) {
    RELAXED("relaxed", 0.6f, 1.45f),
    NORMAL("normal", 1f, 1f),
    SNAPPY("snappy", 1.7f, 0.65f);

    fun userStiffness(base: Float): Float = base * stiffnessScale

    fun userDurationMs(baseMs: Int): Int = (baseMs * durationScale).toInt().coerceAtLeast(1)

    /** The user's choice, then the part of the system animation scale that Compose does not apply itself. */
    fun stiffness(base: Float): Float = SystemAnimScale.scaledStiffness(userStiffness(base), SystemAnimScale.extraScale())

    fun durationMs(baseMs: Int): Int = SystemAnimScale.scaledDurationMs(userDurationMs(baseMs), SystemAnimScale.extraScale())

    /** For a value that already includes the user's choice; adds only the system part (see [SystemAnimScale.extraScale]). */
    fun systemStiffness(userStiffness: Float): Float = SystemAnimScale.scaledStiffness(userStiffness, SystemAnimScale.extraScale())

    fun systemDurationMs(userMs: Int): Int = SystemAnimScale.scaledDurationMs(userMs, SystemAnimScale.extraScale())

    companion object {
        const val ID_RELAXED = "relaxed"
        const val ID_NORMAL = "normal"
        const val ID_SNAPPY = "snappy"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): AnimationSpeed = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
