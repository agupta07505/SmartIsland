/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 * Modified for Static Boat (2026)
 */

package com.agupta07505.smartisland.model

/**
 * Which cards go into the expanded vertical stack, and how many show. Pure logic, see StackRulesTest.
 * The stack view itself is not built yet (Task 8c); this decides what it would show.
 */
object StackRules {
    const val MAX_CARDS = 4
    const val STYLE_STACK = "Stack"

    data class Stack(val cards: List<IslandNotification>, val hiddenCount: Int)

    /** Live activities and a plugged-in charging card. Plain messages are handled separately (see [build]). */
    fun isStackCard(n: IslandNotification): Boolean =
        (n.mode == IslandMode.Battery && n.category == "battery_plugged_in") ||
            (n.mode != IslandMode.IncomingCall && IslandPriority.isLive(n.mode))

    /** [visible] is already priority-ordered (IslandViewModel.visibleNotifications). The newest message goes first. */
    fun build(visible: List<IslandNotification>, showAll: Boolean = false): Stack {
        val newestMessage = visible.firstOrNull { it.mode == IslandMode.Notification }
        // ponytail: only the newest message shows in the stack; older ones are reached by collapsing and cycling.
        val all = listOfNotNull(newestMessage) + visible.filter(::isStackCard)
        val shown = if (showAll) all else all.take(MAX_CARDS)
        return Stack(shown, all.size - shown.size)
    }

    /** Stack only when 2+ cards would show, no call is ringing, and the setting is on. */
    fun useStack(expandStyle: String, visible: List<IslandNotification>): Boolean =
        expandStyle == STYLE_STACK &&
            visible.none { it.mode == IslandMode.IncomingCall } &&
            build(visible).cards.size >= 2

    /** Index of the card whose window-space vertical range contains [yWindowPx], or null (gaps and outside). */
    fun hitIndex(yWindowPx: Float, bounds: List<Pair<Float, Float>>): Int? =
        bounds.indexOfFirst { (top, bottom) -> yWindowPx in top..bottom }.takeIf { it >= 0 }
}
