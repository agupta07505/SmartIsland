/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

class SystemWindowBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val active: Boolean = false
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

object FullscreenRules {
    const val EXIT_MS = 0L

    const val TAP_PEEK_MS = 4000L

    const val EVENT_PEEK_MS = 3500L

    const val ROTATION_HOLD_MS = 1200L

    const val PANEL_GRACE_MS = 800L

    const val POLL_MS = 400L

    fun isStatusBar(window: SystemWindowBounds, screenWidth: Int, screenHeight: Int): Boolean =
        window.top <= 1 &&
            window.height in 1 until screenHeight / 4 &&
            window.width * 2 >= screenWidth

    fun statusBarPresent(windows: List<SystemWindowBounds>, screenWidth: Int, screenHeight: Int): Boolean =
        windows.any { isStatusBar(it, screenWidth, screenHeight) }

    /**
     * Whether one of the SystemUI windows is a panel in front (shade, quick settings, power menu): wide, taller than
     * a bar, and the one the user is in. A fullscreen app has none of these, and an invisible panel window that just
     * stays in the list is not active so it does not count
     */
    fun isSystemPanel(window: SystemWindowBounds, screenWidth: Int, screenHeight: Int): Boolean =
        window.active &&
            window.height >= screenHeight / 4 &&
            window.width * 2 >= screenWidth

    fun systemPanelPresent(windows: List<SystemWindowBounds>, screenWidth: Int, screenHeight: Int): Boolean =
        windows.any { isSystemPanel(it, screenWidth, screenHeight) }

    fun systemUiPresent(windows: List<SystemWindowBounds>, screenWidth: Int, screenHeight: Int): Boolean =
        statusBarPresent(windows, screenWidth, screenHeight) ||
            windows.any { isSystemPanel(it, screenWidth, screenHeight) }

    fun eligibleLayout(isLandscape: Boolean, landscapeLayoutStyle: String?): Boolean =
        !isLandscape || LandscapeLayoutStyle.isEdge(landscapeLayoutStyle)

    /**
     * Whether the island is hidden now. A call that rings or connects always shows, an open card is never taken away
     * under the finger, and a peek (cutout touch, new event) shows it until its time is up.
     */
    fun hidden(fullscreen: Boolean, peeking: Boolean, expanded: Boolean, callNeedsUser: Boolean): Boolean =
        fullscreen && !peeking && !expanded && !callNeedsUser

    /**
     * [hidden] plus the shade: with the "hide in shade" switch on, an open notification shade or quick settings hides the
     * island completely, whatever else would show it (a call, an open card, a peek)
     */
    fun hiddenWithShade(
        fullscreen: Boolean, peeking: Boolean, expanded: Boolean, callNeedsUser: Boolean, shadeHides: Boolean
    ): Boolean = shadeHides || hidden(fullscreen, peeking, expanded, callNeedsUser)

    /**
     * The peek ends only when nothing keeps it open: an expanded card holds it (the wait restarts after), and so does a
     * pulse that runs or waits in the gap before the next one, however long it outlasts the peek time. The island
     * closes as soon as the pulses are over
     */
    fun peekCanEnd(expanded: Boolean, pulseBusy: Boolean = false): Boolean = !expanded && !pulseBusy
}

/**
 * Turns the readings of "is the status bar on screen" into fullscreen or not.
 *
 *  - the bar missing means fullscreen at once, the bar back counts after [FullscreenRules.EXIT_MS] (none today)
 *  - until the bar has been seen once, nothing is fullscreen: a ROM that never lists it would hide the island forever
 *  - a rotation, or a reading that could not be taken, keeps the last answer and restarts the wait
 */
class FullscreenTracker {
    var fullscreen: Boolean = false
        private set

    private var seenBar = false
    private var barSinceMs = NONE
    private var holdUntilMs = NONE
    private var fullscreenAppKey: Int? = null
    private var panelSeenMs = NONE

    /** The window list is unreliable until [untilMs] (a rotation just happened). */
    fun hold(untilMs: Long) {
        holdUntilMs = untilMs
        barSinceMs = NONE
    }

    fun reset() {
        fullscreen = false
        barSinceMs = NONE
        holdUntilMs = NONE
        fullscreenAppKey = null
        panelSeenMs = NONE
    }

    /** Milliseconds until a pending exit completes (the bar is back and counting), null when none is waiting. */
    fun exitRemainingMs(nowMs: Long): Long? =
        if (fullscreen && barSinceMs != NONE) (FullscreenRules.EXIT_MS - (nowMs - barSinceMs)).coerceAtLeast(0L) else null

    /**
     * [barPresent] null is a reading that failed, nothing changes then. [appKey] identifies the app window in front
     * (null when unknown): the bar back over a different app window is a real exit, not a swipe reveal, so it counts
     * at once. Returns the current answer
     */
    fun update(barPresent: Boolean?, nowMs: Long, appKey: Int? = null, panelPresent: Boolean = false): Boolean {
        if (panelPresent) panelSeenMs = nowMs
        if (barPresent == null) {
            // the bar's wait restarts so a stale half-wait does not count
            barSinceMs = NONE
            return fullscreen
        }
        val holding = holdUntilMs != NONE && nowMs < holdUntilMs
        if (!holding) holdUntilMs = NONE
        if (barPresent) {
            seenBar = true
            if (barSinceMs == NONE) barSinceMs = nowMs
            if (fullscreen) {
                val otherApp = appKey != null && fullscreenAppKey != null && appKey != fullscreenAppKey
                if (otherApp || nowMs - barSinceMs >= FullscreenRules.EXIT_MS) {
                    fullscreen = false
                    fullscreenAppKey = null
                }
            }
        } else {
            barSinceMs = NONE
            val panelJustClosed = panelSeenMs != NONE && nowMs - panelSeenMs < FullscreenRules.PANEL_GRACE_MS
            if (!holding && seenBar && !fullscreen && !panelJustClosed) {
                fullscreen = true
                fullscreenAppKey = appKey
            }
        }
        return fullscreen
    }

    private companion object {
        const val NONE = Long.MIN_VALUE
    }
}
