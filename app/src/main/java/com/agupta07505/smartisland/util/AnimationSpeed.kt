/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.StableId
import com.agupta07505.smartisland.model.enumById

enum class AnimationSpeed(override val id: String, val stiffnessScale: Float, val durationScale: Float) : StableId {
    RELAXED("relaxed", 0.6f, 1.45f),
    NORMAL("normal", 1f, 1f),
    SNAPPY("snappy", 1.7f, 0.65f);

    fun userStiffness(base: Float): Float = base * stiffnessScale

    fun userDurationMs(baseMs: Int): Int = (baseMs * durationScale).toInt().coerceAtLeast(1)

    /** User's choice plus the part of the system animation scale Compose skips. */
    fun stiffness(base: Float): Float = SystemAnimScale.scaledStiffness(userStiffness(base),
            SystemAnimScale.extraScale())

    fun durationMs(baseMs: Int): Int = SystemAnimScale.scaledDurationMs(userDurationMs(baseMs),
            SystemAnimScale.extraScale())

    /** Adds only the system part, [userStiffness] already has the user's choice. */
    fun systemStiffness(userStiffness: Float): Float = SystemAnimScale.scaledStiffness(userStiffness,
            SystemAnimScale.extraScale())

    fun systemDurationMs(userMs: Int): Int = SystemAnimScale.scaledDurationMs(userMs, SystemAnimScale.extraScale())

    companion object {
        const val ID_RELAXED = "relaxed"
        const val ID_NORMAL = "normal"
        const val ID_SNAPPY = "snappy"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): AnimationSpeed = enumById(id, NORMAL)
    }
}
