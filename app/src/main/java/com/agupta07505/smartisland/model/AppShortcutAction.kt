/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Long-press actions on an app in the quick-launch grid (a plain tap just opens the app). */
enum class AppShortcutAction { AppInfo, FloatingWindow, SplitScreen }

/** Long-press menu in display order; floating window is listed only when enabled in settings. */
fun appShortcutMenu(floatingRoute: FloatingWindowRoute): List<AppShortcutAction> = buildList {
    add(AppShortcutAction.AppInfo)
    if (floatingRoute != FloatingWindowRoute.Off) add(AppShortcutAction.FloatingWindow)
    add(AppShortcutAction.SplitScreen)
}

enum class SplitPlan {
    /** Foreground app takes one half, the chosen app the other. */
    PairWithForeground,

    /** Nothing to pair with: open the chosen app, then let Android's split-screen picker choose the second. */
    LaunchThenSystemPicker
}

/** Split plan from what is on screen; the home screen cannot be half of a split, so Android's own picker takes over. */
fun splitScreenPlan(foregroundPackage: String?, homePackages: Set<String>, targetPackage: String): SplitPlan = when {
    foregroundPackage.isNullOrEmpty() -> SplitPlan.LaunchThenSystemPicker
    foregroundPackage in homePackages -> SplitPlan.LaunchThenSystemPicker
    foregroundPackage == targetPackage -> SplitPlan.LaunchThenSystemPicker
    else -> SplitPlan.PairWithForeground
}
