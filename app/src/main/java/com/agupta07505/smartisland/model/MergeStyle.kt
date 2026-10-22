/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class MergeStyle(override val id: String, val baseDurationMs: Int) : StableId {
    SLIDE("slide", 420),
    BUBBLE("bubble", 900);

    companion object {
        const val ID_SLIDE = "slide"
        const val ID_BUBBLE = "bubble"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): MergeStyle = enumById(id, SLIDE)
    }
}
