/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * A notification as the count widget sees it, from the shade or from a card the island holds (it may have cancelled
 * its shade copy). Collecting: util/NotificationCountTracker.kt.
 */
data class CountEntry(
    val key: String,
    val packageName: String,
    val appLabel: String,
    /** [android.app.Notification.category] ("msg", "email", "call", ...); null when the app sets none. */
    val category: String? = null,
    /** Uses MessagingStyle (chat apps that set no category still do). */
    val isMessagingStyle: Boolean = false,
    /** Ongoing / foreground-service notification: never a message waiting to be read. */
    val ongoing: Boolean = false,
    /** The app's group summary repeats its children, so counting it would count every message twice. */
    val groupSummary: Boolean = false
)

data class AppCount(val packageName: String, val label: String, val count: Int)

/**
 * All counted notifications: the total for the pill's glyph and per-app counts (most first) for the semi-expanded
 * island.
 */
data class NotificationSummary(val total: Int, val apps: List<AppCount>) {
    companion object {
        val Empty = NotificationSummary(0, emptyList())
    }
}

/** What the semi-expanded island lists: the first few apps and the notifications in the apps that did not fit. */
data class NotificationRows(val shown: List<AppCount>, val moreNotifications: Int, val moreApps: Int)

object NotificationCounts {
    const val CATEGORY_MESSAGE = "msg"
    const val CATEGORY_EMAIL = "email"

    /** The count badge shows at most this ("99+"). */
    const val BADGE_MAX = 99

    /** The island lists this many apps; the rest are summed in one "+N more" line. */
    const val ISLAND_MAX_ROWS = 3

    private val NON_MESSAGE_CATEGORIES = setOf(
        "call", "progress", "service", "transport", "sys", "status", "alarm", "navigation",
        "stopwatch", "workout", "location_sharing", "recommendation", "promo", "reminder", "event", "err"
    )

    /**
     * Messaging and mail apps that post without a category or MessagingStyle. Fallback only: they still pass the
     * ongoing / group-summary / non-message checks of [isCounted].
     */
    val KNOWN_MESSAGING_PACKAGES: Set<String> = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram",
        "org.thoughtcrime.securesms", "com.facebook.orca", "com.facebook.mlite", "com.discord", "com.Slack", "com.microsoft.teams",
        "com.google.android.apps.dynamite", "com.viber.voip", "jp.naver.line.android", "com.tencent.mm", "com.skype.raider",
        "com.instagram.android", "com.snapchat.android", "im.vector.app", "com.beeper.android", "app.threema.messenger", "ch.threema.app",
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms", "com.oneplus.mms", "com.miui.mms",
        "com.coloros.mms", "com.android.messaging",
        "com.google.android.gm", "com.google.android.gm.lite", "com.microsoft.office.outlook", "com.yahoo.mobile.client.android.mail",
        "ch.protonmail.android", "com.samsung.android.email.provider", "com.fsck.k9", "net.thunderbird.android", "com.fsck.k9.material",
        "com.my.mail", "ru.yandex.mail", "com.tutanota.android", "de.tutao.tutanota", "com.readdle.spark", "me.bluemail.mail",
        "com.android.email", "com.oneplus.email", "com.fastmail.mobile", "com.apple.android.mail"
    )

    /**
     * Whether [entry] is a message or mail waiting to be read: not ongoing, not a group summary, and a message by
     * category, style or app.
     */
    fun isCounted(entry: CountEntry): Boolean {
        if (entry.ongoing || entry.groupSummary) return false
        val category = entry.category
        if (category == CATEGORY_MESSAGE || category == CATEGORY_EMAIL) return true
        if (entry.isMessagingStyle) return true
        if (category != null && category in NON_MESSAGE_CATEGORIES) return false
        return entry.packageName in KNOWN_MESSAGING_PACKAGES
    }

    /**
     * The shade's notifications plus the island's cards, one per key (a card kept after cancelling its shade copy has
     * no twin; one both hold counts once).
     */
    fun merge(shade: List<CountEntry>, island: List<CountEntry>): List<CountEntry> {
        val byKey = LinkedHashMap<String, CountEntry>()
        shade.forEach { byKey[it.key] = it }
        island.forEach { byKey.putIfAbsent(it.key, it) }
        return byKey.values.toList()
    }

    /** Counts notifications that pass [isCounted] per app: biggest first, ties in label order. */
    fun summarize(entries: List<CountEntry>): NotificationSummary {
        val counted = entries.filter { isCounted(it) }
        if (counted.isEmpty()) return NotificationSummary.Empty
        val apps = counted.groupBy { it.packageName }
            .map { (pkg, list) -> AppCount(pkg, list.first().appLabel.ifBlank { pkg }, list.size) }
            .sortedWith(compareByDescending<AppCount> { it.count }.thenBy { it.label.lowercase() })
        return NotificationSummary(counted.size, apps)
    }

    /** Pill count glyph text: "" for none, "7", "99+". */
    fun badgeText(total: Int): String = when {
        total <= 0 -> ""
        total > BADGE_MAX -> "$BADGE_MAX+"
        else -> total.toString()
    }

    /** Apps the island lists and what is left for its "+N more" line. */
    fun rows(summary: NotificationSummary, maxRows: Int = ISLAND_MAX_ROWS): NotificationRows {
        val shown = summary.apps.take(maxRows)
        val rest = summary.apps.drop(maxRows)
        return NotificationRows(shown, rest.sumOf { it.count }, rest.size)
    }

    private const val ISLAND_PADDING_DP = 20f
    private const val ISLAND_ROW_DP = 20f
    private const val ISLAND_MORE_DP = 14f

    /** Height (dp) the semi-expanded island needs for [rows]: the rows, the "+N more" line if any, and padding. */
    fun islandContentHeightDp(rows: NotificationRows): Float =
        ISLAND_PADDING_DP + rows.shown.size * ISLAND_ROW_DP + if (rows.moreApps > 0) ISLAND_MORE_DP else 0f
}
