/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * How much an alert deserves the island.
 *
 *  - [Critical] (P0): alarms, a ringing timer, battery at 5% or less. Stays until the user acts.
 *  - [Live] (P1): incoming calls, active navigation, and the other running tasks (music, timers, ...).
 *    Stays while the task is active.
 *  - [Communication] (P2): direct messages and other time-sensitive pings. Leaves the island after
 *    [AlertEngine.communicationTimeoutMs]; any touch restarts that clock.
 *  - [Passive] (P3): app updates, likes, promos. Never reaches the island.
 */
enum class AlertPriority { Critical, Live, Communication, Passive }

/** Pure classification of an alert into an [AlertPriority]; no Android state, so it is unit-testable. */
object AlertClassifier {
    /** Battery at or below this percentage is critical. */
    const val CRITICAL_BATTERY_PERCENT = 5

    private val passiveCategories = setOf("promo", "recommendation", "social")
    private val passivePackages = setOf("com.android.vending")

    fun classify(notification: IslandNotification): AlertPriority {
        val category = notification.category?.lowercase()
        return when (notification.mode) {
            IslandMode.IncomingCall, IslandMode.Navigation -> AlertPriority.Live
            IslandMode.Battery ->
                if (isCriticalBattery(notification)) AlertPriority.Critical else AlertPriority.Live
            IslandMode.Timer ->
                if (category == "alarm") AlertPriority.Critical else AlertPriority.Live
            IslandMode.Notification -> classifyNotification(notification, category)
            else -> AlertPriority.Live
        }
    }

    private fun classifyNotification(notification: IslandNotification, category: String?): AlertPriority = when {
        // The in-app preview always shows what you ask for.
        notification.key.startsWith("demo_") -> AlertPriority.Communication
        category == "alarm" -> AlertPriority.Critical
        category == "call" -> AlertPriority.Live
        // A reply box means a person is waiting, even from an app that also posts "social" noise.
        notification.actionIntents.any { it.isQuickReply } -> AlertPriority.Communication
        category in passiveCategories || notification.packageName in passivePackages -> AlertPriority.Passive
        // Uncategorised alerts are shown: hiding every app that never sets a category would be worse.
        else -> AlertPriority.Communication
    }

    private fun isCriticalBattery(notification: IslandNotification): Boolean {
        if (notification.title.contains("charging", ignoreCase = true)) return false
        val percent = Regex("(\\d{1,3})\\s*%").find(notification.text)?.groupValues?.get(1)?.toIntOrNull()
            ?: return false
        return percent <= CRITICAL_BATTERY_PERCENT
    }
}

/**
 * The rules engine between the raw alert list and the island.
 *
 * Feed it the full list on every change with [evaluate]; it returns what the island should show, in
 * order, and when the list next needs re-evaluating:
 *  - P3 alerts are dropped;
 *  - P0 sorts first, then P1, then P2, so a call keeps the primary island and a message that arrives
 *    during it becomes a secondary alert behind it;
 *  - P2 alerts from the same app group are folded into one entry that carries a count;
 *  - a P2 group leaves after [communicationTimeoutMs] with no new message and no touch ([touch]).
 *
 * It is deliberately free of Android and coroutines: the caller owns the clock and the scheduling.
 */
class AlertEngine(val communicationTimeoutMs: Long = DEFAULT_COMMUNICATION_TIMEOUT_MS) {

    class Evaluation(val visible: List<IslandNotification>, val nextExpiryAtMs: Long?)

    private val firstSeenAt = HashMap<String, Long>()
    private val expired = HashSet<String>()
    private var lastTouchAt = Long.MIN_VALUE

    /** The user interacted with the island: every P2 group on it starts its timeout over. */
    fun touch(nowMs: Long) {
        lastTouchAt = nowMs
    }

    fun evaluate(raw: List<IslandNotification>, nowMs: Long): Evaluation {
        val live = raw.mapTo(HashSet()) { it.key }
        firstSeenAt.keys.retainAll(live)
        expired.retainAll(live)

        val critical = ArrayList<IslandNotification>()
        val running = ArrayList<IslandNotification>()
        val communications = LinkedHashMap<String, MutableList<IslandNotification>>()
        for (notification in raw) {
            when (AlertClassifier.classify(notification)) {
                AlertPriority.Passive -> Unit
                AlertPriority.Critical -> critical += notification
                AlertPriority.Live -> running += notification
                AlertPriority.Communication -> {
                    firstSeenAt.getOrPut(notification.key) { nowMs }
                    if (notification.key !in expired) {
                        communications.getOrPut(groupOf(notification)) { ArrayList() } += notification
                    }
                }
            }
        }

        var nextExpiry: Long? = null
        val groups = ArrayList<IslandNotification>()
        for (members in communications.values) {
            val startedAt = maxOf(members.maxOf { firstSeenAt.getValue(it.key) }, lastTouchAt)
            val expiresAt = startedAt + communicationTimeoutMs
            if (nowMs >= expiresAt) {
                members.forEach { expired += it.key }
                continue
            }
            nextExpiry = nextExpiry?.let { minOf(it, expiresAt) } ?: expiresAt
            groups += fold(members)
        }
        groups.sortByDescending { it.timeMillis }

        return Evaluation(critical + running + groups, nextExpiry)
    }

    private fun groupOf(notification: IslandNotification): String =
        notification.packageName + "|" + (notification.groupId ?: "")

    private fun fold(members: List<IslandNotification>): IslandNotification {
        val latest = members.maxByOrNull { it.timeMillis } ?: members.last()
        if (members.size == 1) return latest
        return latest.copy(
            groupCount = members.size,
            memberKeys = members.map { it.key },
            groupIcons = members.sortedBy { it.timeMillis }
                .mapNotNull { it.largeIcon ?: it.icon }
                .takeLast(MAX_STACKED_ICONS)
        )
    }

    companion object {
        const val DEFAULT_COMMUNICATION_TIMEOUT_MS = 7_000L
        const val MAX_STACKED_ICONS = 3
    }
}
