/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Engine that opens the floating window.
 * - [System]: Android's own freeform windowing (needs ROM / developer-options support).
 * - [Lmo]: LMO Freeform (`com.libremobileos.freeform`) via its exported START_FREEFORM broadcast.
 */
enum class FloatingWindowMode(val id: String) {
    System("system"),
    Lmo("lmo");

    companion object {
        const val ID_SYSTEM = "system"
        const val ID_LMO = "lmo"
        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): FloatingWindowMode = entries.firstOrNull { it.id == id } ?: System
    }
}

/** Floating-window outcome decided from settings alone (no Android types, so unit-testable). */
enum class FloatingWindowRoute { Off, System, Lmo }

fun floatingWindowRoute(enabled: Boolean, modeId: String?): FloatingWindowRoute = when {
    !enabled -> FloatingWindowRoute.Off
    FloatingWindowMode.fromId(modeId) == FloatingWindowMode.Lmo -> FloatingWindowRoute.Lmo
    else -> FloatingWindowRoute.System
}
