/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode

/** What the user chose for one app's auto-expand. DEFAULT is the state of every app nobody touched. */
enum class AppAutoExpand {
    /** Only this app's loud (non-silent) notifications open the card by themselves. */
    DEFAULT,

    /** Every new notification from this app opens the card, silent ones included. */
    ALWAYS,

    /** Nothing from this app opens the card by itself, loud notifications included. */
    NEVER
}

/**
 * Per-app rules for what an app's notifications may do on the island.
 *
 * - expanded card: the app's card cannot be opened into the big card (tap, swipe or auto); a deny set
 * - sound: the island plays no alert sound for the app (the existing muted-apps set); a deny set
 * - auto-expand: off by default for every app, except for loud notifications; see [mayAutoExpand]
 */
object AppEventRules {

    private fun isAppCard(mode: IslandMode): Boolean =
        mode == IslandMode.Notification || mode == IslandMode.DownloadUpload

    fun mayExpandCard(noExpandPackages: Set<String>, packageName: String, mode: IslandMode): Boolean =
        !isAppCard(mode) || packageName !in noExpandPackages

    fun maySound(mutedPackages: Set<String>, packageName: String): Boolean =
        packageName !in mutedPackages

    /** The app's auto-expand choice. A package in both sets counts as NEVER (the explicit off wins). */
    fun autoExpandChoice(
        noAutoExpandPackages: Set<String>,
        autoExpandPackages: Set<String>,
        packageName: String
    ): AppAutoExpand = when {
        packageName in noAutoExpandPackages -> AppAutoExpand.NEVER
        packageName in autoExpandPackages -> AppAutoExpand.ALWAYS
        else -> AppAutoExpand.DEFAULT
    }

    /**
     * Decision order for a NEW notification (the caller has already established that it is new):
     * 1. the global Auto-expand switch is off -> no;
     * 2. the app never opens its card at all -> no (nothing to auto-open);
     * 3. the user switched this app's auto-expand off -> no, even for a loud notification;
     * 4. the notification is loud (not silent) -> yes;
     * 5. the user switched this app's auto-expand on -> yes;
     * 6. otherwise (the default of every app) -> no.
     */
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
