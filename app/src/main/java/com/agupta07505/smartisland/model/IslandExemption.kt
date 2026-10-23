/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class ExemptMode {
    None,

    /** No pill, widgets, cards, popups or gestures. */
    Off,

    /** Portrait only: the collapsed pill stays in place but is dead (no content, popups or touches). */
    Static
}

/**
 * Apps that change the island while in the foreground (Settings > Exempt apps), per orientation. Portrait can also
 * use [ExemptMode.Static]; landscape only [ExemptMode.Off].
 */
object IslandExemption {
    fun modeFor(
        foregroundPackage: String?,
        portraitOffApps: Set<String>,
        portraitStaticApps: Set<String>,
        landscapeOffApps: Set<String>,
        isLandscape: Boolean
    ): ExemptMode {
        if (foregroundPackage.isNullOrEmpty()) return ExemptMode.None
        if (isLandscape) {
            return if (foregroundPackage in landscapeOffApps) ExemptMode.Off else ExemptMode.None
        }
        return when {
            foregroundPackage in portraitOffApps -> ExemptMode.Off
            foregroundPackage in portraitStaticApps -> ExemptMode.Static
            else -> ExemptMode.None
        }
    }

    fun isOff(
        foregroundPackage: String?,
        portraitApps: Set<String>,
        landscapeApps: Set<String>,
        isLandscape: Boolean
    ): Boolean = modeFor(foregroundPackage, portraitApps, emptySet(), landscapeApps, isLandscape) == ExemptMode.Off
}
