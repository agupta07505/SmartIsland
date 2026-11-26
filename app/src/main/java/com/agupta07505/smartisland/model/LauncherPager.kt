/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs

/**
 * Pages of the quick-launch window: Apps and Recents side by side (swipe left / right), Utilities on the other level
 * (swipe up / down), the levels loop
 */
enum class LauncherPage { Apps, Recents, Utilities }

enum class LauncherSwipe { Up, Down, Left, Right }

/**
 * Page a [swipe] leads to from [current]. Up / down loops between the apps level and Utilities (Utilities returns to
 * [lastAppsLevelPage]), left / right switches between Apps and Recents, a real page only while [recentsEnabled],
 * Utilities has nothing sideways
 */
fun launcherPageAfter(
    current: LauncherPage,
    swipe: LauncherSwipe,
    recentsEnabled: Boolean,
    lastAppsLevelPage: LauncherPage = LauncherPage.Apps
): LauncherPage {
    val appsLevel = if (recentsEnabled
        && lastAppsLevelPage == LauncherPage.Recents) LauncherPage.Recents else LauncherPage.Apps
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
 * fire. A tap is not a drag
 */
fun launcherOwnsDrag(launcherOpen: Boolean, dragX: Float, dragY: Float): Boolean =
    launcherOpen && (abs(dragX) > 0.5f || abs(dragY) > 0.5f)

fun launcherSwipeFor(dx: Float, dy: Float, thresholdPx: Float): LauncherSwipe? {
    val ax = abs(dx)
    val ay = abs(dy)
    return when {
        ay >= ax && ay >= thresholdPx -> if (dy < 0f) LauncherSwipe.Up else LauncherSwipe.Down
        ax > ay && ax >= thresholdPx -> if (dx < 0f) LauncherSwipe.Left else LauncherSwipe.Right
        else -> null
    }
}

object LauncherClose {
    const val HOLD_MS = 600L

    fun shown(open: Boolean, expanded: Boolean, closing: Boolean): Boolean = (open && expanded) || closing

    fun holdUntil(nowMs: Long): Long = nowMs + HOLD_MS
}
