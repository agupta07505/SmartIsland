/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The two swipe-down gestures are not configurable (the settings screen does not list them). A swipe down on the
 * collapsed pill always expands the island, and a swipe down on the expanded island does nothing (a card with expandable text or a picture still opens it).
 * The stored `pillSwipeDownAction` / `swipeDownAction` values are ignored.
 */
val FIXED_PILL_SWIPE_DOWN_ACTION: SwipeAction get() = SwipeAction.Expand
val FIXED_EXPANDED_SWIPE_DOWN_ACTION: SwipeAction get() = SwipeAction.None

enum class SwipeAction(val id: String) {
    DismissCurrent("DismissCurrent"),
    DismissAll("DismissAll"),
    Collapse("Collapse"),
    OpenApp("OpenApp"),
    FloatingWindow("FloatingWindow"),
    NotificationShade("NotificationShade"),
    NextPrevious("NextPrevious"),
    NextNotification("NextNotification"),
    PreviousNotification("PreviousNotification"),
    /** Next stored event: any card the island holds plus loud (non-silent) notifications still in the shade. */
    NextEvent("NextEvent"),
    PreviousEvent("PreviousEvent"),
    /** Expanded island only: next card of the SAME app as the current one (wraps inside the app). */
    NextSibling("NextSibling"),
    PreviousSibling("PreviousSibling"),
    NextTrack("NextTrack"),
    PreviousTrack("PreviousTrack"),
    PlayPause("PlayPause"),
    Expand("Expand"),
    /** Long press on the collapsed pill: hide it (idle pill shown) or bring it back. */
    TogglePill("TogglePill"),
    None("None");

    companion object {
        fun fromId(id: String?, default: SwipeAction = None): SwipeAction {
            if (id.isNullOrBlank()) return default
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: default
        }
    }
}
