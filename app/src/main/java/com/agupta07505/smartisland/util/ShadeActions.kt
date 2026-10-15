/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.expanded.ActionRole
import com.agupta07505.smartisland.ui.expanded.classifyAction

object ShadeActions {

    const val SHADE_ACTION_LIMIT = 3

    private val CLOCK_ROLES = setOf(
        ActionRole.Pause, ActionRole.Resume, ActionRole.Stop, ActionRole.Reset, ActionRole.Lap, ActionRole.AddMinute
    )

    private val END_WORD = Regex("""(?<![\p{L}])end(?![\p{L}])""")

    private fun roleOf(title: String): ActionRole {
        val role = classifyAction(title)
        return if (role == ActionRole.Other && END_WORD.containsMatchIn(title.lowercase())) ActionRole.Stop else role
    }

    fun enforceState(actions: List<IslandNotificationAction>, kind: TimerStopwatchParser.ClockKind): List<IslandNotificationAction> {
        val roles = actions.map { roleOf(it.title) }
        val offersPause = ActionRole.Pause in roles
        val offersResume = ActionRole.Resume in roles
        if (offersPause == offersResume) return actions
        val banned: Set<ActionRole> = when {
            offersPause && kind == TimerStopwatchParser.ClockKind.Timer ->
                setOf(ActionRole.Stop, ActionRole.Reset, ActionRole.Resume, ActionRole.Lap)
            offersPause ->
                setOf(ActionRole.Stop, ActionRole.Reset, ActionRole.Resume, ActionRole.AddMinute)
            ActionRole.Reset in roles ->
                setOf(ActionRole.Pause, ActionRole.Lap, ActionRole.AddMinute, ActionRole.Stop)
            else -> setOf(ActionRole.Pause, ActionRole.Lap, ActionRole.AddMinute)
        }
        return actions.filter { roleOf(it.title) !in banned }
    }

    fun mirror(
        standard: List<IslandNotificationAction>,
        view: List<IslandNotificationAction>,
        kind: TimerStopwatchParser.ClockKind? = null
    ): List<IslandNotificationAction> {
        if (kind == null) return mirrorUnchecked(standard, view)
        // Filter before the shade limit so a hidden button cannot push out a shown one.
        val ownButtons = enforceState(standard.filter { it.pendingIntent != null }, kind)
        val shown = mirrorUnchecked(
            ownButtons,
            enforceState(view.filter { roleOf(it.title) in CLOCK_ROLES }, kind)
        )
        return withMissingAddMinute(shown, ownButtons, kind)
    }

    private fun withMissingAddMinute(
        shown: List<IslandNotificationAction>,
        own: List<IslandNotificationAction>,
        kind: TimerStopwatchParser.ClockKind
    ): List<IslandNotificationAction> {
        if (kind != TimerStopwatchParser.ClockKind.Timer || !offersPause(shown)) return shown
        if (shown.any { roleOf(it.title) == ActionRole.AddMinute }) return shown
        val addMinute = own.firstOrNull { roleOf(it.title) == ActionRole.AddMinute } ?: return shown
        return shown + addMinute
    }

    private fun mirrorUnchecked(
        standard: List<IslandNotificationAction>,
        view: List<IslandNotificationAction>
    ): List<IslandNotificationAction> {
        val real = standard.filter { it.pendingIntent != null }
        val fromView = view.filter { roleOf(it.title) in CLOCK_ROLES }

        if (fromView.isNotEmpty()) {
            // Prefer the app's own intent over the view proxy.
            val used = HashSet<Int>()
            return fromView.map { shown ->
                val match = real.withIndex().firstOrNull { (i, a) -> i !in used && sameButton(a, shown) }
                if (match != null) {
                    used.add(match.index)
                    match.value
                } else {
                    shown
                }
            }.distinctBy { roleOf(it.title) to it.title.trim().lowercase() }
        }

        return real
            .take(SHADE_ACTION_LIMIT)
            .distinctBy { it.title.trim().lowercase() }
    }

    private fun sameButton(a: IslandNotificationAction, b: IslandNotificationAction): Boolean {
        if (a.title.trim().equals(b.title.trim(), ignoreCase = true)) return true
        val role = roleOf(a.title)
        return role in CLOCK_ROLES && role == roleOf(b.title)
    }

    fun offersPause(actions: List<IslandNotificationAction>): Boolean =
        actions.any { roleOf(it.title) == ActionRole.Pause }

    fun offersResume(actions: List<IslandNotificationAction>): Boolean =
        actions.any { roleOf(it.title) == ActionRole.Resume }
}
