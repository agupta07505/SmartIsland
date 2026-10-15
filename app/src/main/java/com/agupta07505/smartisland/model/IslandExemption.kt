/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** What the island does while an exempt app is in the foreground. */
enum class ExemptMode {
    /** Not exempt: the island works as usual. */
    None,

    /** The island is totally off: no pill, no widgets, no cards, no popups, no gestures. */
    Off,

    /** Portrait only: the collapsed pill stays in its place, but it is dead: no content, no popups, no touches. */
    Static
}

/**
 * Apps that change the island while they are in the foreground (Settings > Exempt apps), with one list per orientation:
 * an app can be exempt in portrait only, in landscape only, or in both. In portrait an app can also be set to [ExemptMode.Static]
 * (the collapsed pill stays visible but does nothing); landscape only has [ExemptMode.Off]. Pure rule, so it can be unit tested.
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
