/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Pages of the quick-launch window: Apps and Recents side by side (swipe left / right), Utilities on the other level
 * (swipe up / down); the levels loop.
 */
enum class LauncherPage { Apps, Recents, Utilities }

enum class LauncherSwipe { Up, Down, Left, Right }

/**
 * Page a [swipe] leads to from [current].
 * - Up / down switches between the apps level and Utilities and loops; from Utilities it returns to
 * [lastAppsLevelPage].
 * - Left / right switches between Apps and Recents when [recentsEnabled]; Utilities has nothing sideways.
 * - Recents is a real page only while enabled; with it off, any apps-level page is Apps.
 */
fun launcherPageAfter(
    current: LauncherPage,
    swipe: LauncherSwipe,
    recentsEnabled: Boolean,
    lastAppsLevelPage: LauncherPage = LauncherPage.Apps
): LauncherPage {
    val appsLevel = if (recentsEnabled && lastAppsLevelPage == LauncherPage.Recents) LauncherPage.Recents else LauncherPage.Apps
    val here = if (current == LauncherPage.Recents && !recentsEnabled) LauncherPage.Apps else current
    return when (swipe) {
        LauncherSwipe.Up, LauncherSwipe.Down ->
            if (here == LauncherPage.Utilities) appsLevel else LauncherPage.Utilities
        LauncherSwipe.Left, LauncherSwipe.Right -> when {
            here == LauncherPage.Utilities -> here
            !recentsEnabled -> LauncherPage.Apps
            here == LauncherPage.Apps -> LauncherPage.Recents
            else -> LauncherPage.Apps
        }
    }
}

/**
 * True while the launcher is open and a moving drag belongs to it, so the island's own swipe actions must not also
 * fire. A tap is not a drag.
 */
fun launcherOwnsDrag(launcherOpen: Boolean, dragX: Float, dragY: Float): Boolean =
    launcherOpen && (kotlin.math.abs(dragX) > 0.5f || kotlin.math.abs(dragY) > 0.5f)

/** Swipe a drag of ([dx], [dy]) makes once it passes [thresholdPx] along its longer axis; null before that. */
fun launcherSwipeFor(dx: Float, dy: Float, thresholdPx: Float): LauncherSwipe? {
    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
    return when {
        ay >= ax && ay >= thresholdPx -> if (dy < 0f) LauncherSwipe.Up else LauncherSwipe.Down
        ax > ay && ax >= thresholdPx -> if (dx < 0f) LauncherSwipe.Left else LauncherSwipe.Right
        else -> null
    }
}
