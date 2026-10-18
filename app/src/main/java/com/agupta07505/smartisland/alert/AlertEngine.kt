/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * Which tier of the island's rulebook an alert belongs to.
 *
 *  - [Critical]: alarms, a ringing timer, battery at 5% or less. Persists until the user acts, and
 *    outranks everything.
 *  - [Persistent] (Tier 1): calls, navigation, recording, timers, stopwatches. Persists until ended and
 *    owns the island; nothing displaces it for good.
 *  - [Background] (Tier 2): music, live activities, hotspot, transfers. Persists while active, and steps
 *    aside (to the companion bubble, or out of sight) for a Persistent task or an Ephemeral interruption.
 *  - [Ephemeral] (Tier 3): messages and other alerts, battery, Bluetooth, flashlight. Interrupts for a few
 *    seconds (see [AlertEngine]) and then hands the island back.
 *  - [Passive]: app updates, likes, promos. Never reaches the island.
 */
enum class AlertTier { Critical, Persistent, Background, Ephemeral, Passive }

/** Pure classification of an alert into an [AlertTier]; no Android state, so it is unit-testable. */
object AlertClassifier {
    /** Battery at or below this percentage is critical. */
    const val CRITICAL_BATTERY_PERCENT = 5

    private val passiveCategories = setOf("promo", "recommendation", "social")
    private val passivePackages = setOf("com.android.vending")

    fun classify(notification: IslandNotification): AlertTier {
        val category = notification.category?.lowercase()
        return when (notification.mode) {
            IslandMode.IncomingCall, IslandMode.Navigation, IslandMode.ScreenRecording,
            IslandMode.Stopwatch -> AlertTier.Persistent
            IslandMode.Timer -> if (category == "alarm") AlertTier.Critical else AlertTier.Persistent
            IslandMode.Music, IslandMode.LiveActivity, IslandMode.Hotspot,
            IslandMode.DownloadUpload, IslandMode.Empty -> AlertTier.Background
            IslandMode.Battery ->
                if (isCriticalBattery(notification)) AlertTier.Critical else AlertTier.Ephemeral
            IslandMode.Bluetooth, IslandMode.Flashlight -> AlertTier.Ephemeral
            IslandMode.Notification -> classifyNotification(notification, category)
        }
    }

    private fun classifyNotification(notification: IslandNotification, category: String?): AlertTier = when {
        // The in-app preview always shows what you ask for.
        notification.key.startsWith("demo_") -> AlertTier.Ephemeral
        category == "alarm" -> AlertTier.Critical
        category == "call" -> AlertTier.Persistent
        // A reply box means a person is waiting, even from an app that also posts "social" noise.
        notification.actionIntents.any { it.isQuickReply } -> AlertTier.Ephemeral
        category in passiveCategories || notification.packageName in passivePackages -> AlertTier.Passive
        // Uncategorised alerts are shown: hiding every app that never sets a category would be worse.
        else -> AlertTier.Ephemeral
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
 * Feed it the full list on every change with [evaluate]; it returns what the island should show, and when
 * the list next needs re-evaluating:
 *  - Passive alerts are dropped.
 *  - At most [MAX_ACTIVE] states are returned: the first is the main pill, the second the companion bubble.
 *    A third is hidden (it stays alive in the shade and comes back when a slot frees up).
 *  - Order: Critical, then Persistent (a call first, then the most recently started), then Ephemeral
 *    (newest first), then Background (newest first). So a call keeps the island over a message, and a
 *    message hijacks a music pill for its few seconds, after which the music returns.
 *  - Ephemeral alerts leave after [communicationTimeoutMs] (messages) or [transientTimeoutMs] (battery,
 *    Bluetooth, flashlight), restart on a touch ([touch]) and, for those three, restart when their state
 *    changes (a new title).
 *  - Messages from the same app group fold into one counted entry; a new message restarts the group's clock.
 *  - While locked, secret messages are dropped and private ones are masked (see [LockscreenPrivacy]).
 *
 * It is deliberately free of Android and coroutines: the caller owns the clock and the scheduling.
 */
class AlertEngine(
    val communicationTimeoutMs: Long = DEFAULT_COMMUNICATION_TIMEOUT_MS,
    val transientTimeoutMs: Long = DEFAULT_TRANSIENT_TIMEOUT_MS
) {

    class Evaluation(val visible: List<IslandNotification>, val nextExpiryAtMs: Long?)

    private val firstSeenAt = HashMap<String, Long>()
    private val stateOf = HashMap<String, String>()
    private val expired = HashSet<String>()
    private var lastTouchAt = Long.MIN_VALUE

    /** The user interacted with the island: every ephemeral alert starts its timeout over. */
    fun touch(nowMs: Long) {
        lastTouchAt = nowMs
    }

    /**
     * [isLocked] applies the lockscreen privacy rules ([LockscreenPrivacy]): secret messages are left out and
     * private ones come back stripped to the app icon and a count.
     */
    fun evaluate(raw: List<IslandNotification>, nowMs: Long, isLocked: Boolean = false): Evaluation {
        val live = raw.mapTo(HashSet()) { it.key }
        firstSeenAt.keys.retainAll(live)
        stateOf.keys.retainAll(live)
        expired.retainAll(live)

        val critical = ArrayList<IslandNotification>()
        val persistent = ArrayList<IslandNotification>()
        val background = ArrayList<IslandNotification>()
        val messages = LinkedHashMap<String, MutableList<IslandNotification>>()
        val transients = ArrayList<IslandNotification>()
        for (notification in raw) {
            // A secret alert holds no slot (and starts no clock) until the phone is unlocked.
            if (isLocked && LockscreenPrivacy.isHiddenWhenLocked(notification)) continue
            val tier = AlertClassifier.classify(notification)
            if (tier == AlertTier.Passive) continue
            firstSeenAt.getOrPut(notification.key) { nowMs }
            when (tier) {
                AlertTier.Critical -> critical += notification
                AlertTier.Persistent -> persistent += notification
                AlertTier.Background -> background += notification
                AlertTier.Ephemeral ->
                    if (notification.mode == IslandMode.Notification) {
                        if (notification.key !in expired) {
                            messages.getOrPut(groupOf(notification)) { ArrayList() } += notification
                        }
                    } else {
                        // A new state (Charging -> Low battery) is a new interruption.
                        if (stateOf.put(notification.key, notification.title).let { it != null && it != notification.title }) {
                            firstSeenAt[notification.key] = nowMs
                            expired -= notification.key
                        }
                        if (notification.key !in expired) transients += notification
                    }
                AlertTier.Passive -> Unit
            }
        }

        var nextExpiry: Long? = null
        fun consider(startedAt: Long, timeoutMs: Long): Boolean {
            val expiresAt = maxOf(startedAt, lastTouchAt) + timeoutMs
            if (nowMs >= expiresAt) return false
            nextExpiry = nextExpiry?.let { minOf(it, expiresAt) } ?: expiresAt
            return true
        }

        val ephemeral = ArrayList<Pair<Long, IslandNotification>>()
        for (members in messages.values) {
            val startedAt = members.maxOf { firstSeenAt.getValue(it.key) }
            if (consider(startedAt, communicationTimeoutMs)) {
                ephemeral += startedAt to fold(members)
            } else {
                members.forEach { expired += it.key }
            }
        }
        for (notification in transients) {
            val startedAt = firstSeenAt.getValue(notification.key)
            if (consider(startedAt, transientTimeoutMs)) {
                ephemeral += startedAt to notification
            } else {
                expired += notification.key
            }
        }

        val ordered = critical.sortedByDescending { firstSeenAt.getValue(it.key) } +
            persistent.sortedWith(
                compareByDescending<IslandNotification> { it.mode == IslandMode.IncomingCall }
                    .thenByDescending { firstSeenAt.getValue(it.key) }
            ) +
            ephemeral.sortedByDescending { it.first }.map { it.second } +
            background.sortedByDescending { firstSeenAt.getValue(it.key) }

        val visible = ordered.take(MAX_ACTIVE)
        return Evaluation(if (isLocked) visible.map(LockscreenPrivacy::mask) else visible, nextExpiry)
    }

    private fun groupOf(notification: IslandNotification): String =
        notification.packageName + "|" + (notification.groupId ?: "")

    private fun fold(members: List<IslandNotification>): IslandNotification {
        val latest = members.maxByOrNull { it.timeMillis } ?: members.last()
        if (members.size == 1) return latest
        return latest.copy(
            groupCount = members.size,
            // The most private member decides how much of the group the lockscreen may show.
            lockscreenVisibility = members.minOf { it.lockscreenVisibility },
            memberKeys = members.map { it.key },
            groupIcons = members.sortedBy { it.timeMillis }
                .mapNotNull { it.largeIcon ?: it.icon }
                .takeLast(MAX_STACKED_ICONS)
        )
    }

    companion object {
        /** Messages: exactly seven seconds. */
        const val DEFAULT_COMMUNICATION_TIMEOUT_MS = 7_000L

        /** Battery, Bluetooth, flashlight: inside the rulebook's three-to-seven-second window. */
        const val DEFAULT_TRANSIENT_TIMEOUT_MS = 5_000L

        /** The main pill plus the companion bubble. */
        const val MAX_ACTIVE = 2
        const val MAX_STACKED_ICONS = 3
    }
}
