/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode

enum class AppAutoExpand {
    /** Loud notifications only */
    DEFAULT,

    /** Silent ones too */
    ALWAYS,

    NEVER
}

/** Deny sets for the big card (no tap, swipe or auto open) and sound, plus auto-expand (see [mayAutoExpand]). */
object AppEventRules {

    private fun isAppCard(mode: IslandMode): Boolean =
        mode == IslandMode.Notification || mode == IslandMode.DownloadUpload

    fun mayExpandCard(noExpandPackages: Set<String>, packageName: String, mode: IslandMode): Boolean =
        !isAppCard(mode) || packageName !in noExpandPackages

    fun maySound(mutedPackages: Set<String>, packageName: String): Boolean =
        packageName !in mutedPackages

    /** A package in both sets counts as NEVER, the explicit off wins. */
    fun autoExpandChoice(
        noAutoExpandPackages: Set<String>,
        autoExpandPackages: Set<String>,
        packageName: String
    ): AppAutoExpand = when {
        packageName in noAutoExpandPackages -> AppAutoExpand.NEVER
        packageName in autoExpandPackages -> AppAutoExpand.ALWAYS
        else -> AppAutoExpand.DEFAULT
    }

    fun mayAutoExpand(
        globalAutoExpand: Boolean,
        noAutoExpandPackages: Set<String>,
        autoExpandPackages: Set<String>,
        noExpandPackages: Set<String>,
        packageName: String,
        mode: IslandMode,
        isLoud: Boolean
    ): Boolean {
        if (!globalAutoExpand) return false
        if (!mayExpandCard(noExpandPackages, packageName, mode)) return false
        return when (autoExpandChoice(noAutoExpandPackages, autoExpandPackages, packageName)) {
            AppAutoExpand.NEVER -> false
            AppAutoExpand.ALWAYS -> true
            AppAutoExpand.DEFAULT -> isLoud
        }
    }
}
