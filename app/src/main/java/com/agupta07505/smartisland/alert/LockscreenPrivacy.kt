/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.alert

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.LockscreenVisibility

/**
 * What the island may show of an alert while the phone is locked (Rule_Book §8.1). Pure, so the engine stays
 * unit-testable; the caller passes the lock state in.
 *
 *  - `VISIBILITY_PUBLIC`: shown as is.
 *  - `VISIBILITY_PRIVATE`: title, text and sender avatar are stripped; the pill shows the app icon and
 *    "1 New Message" (or "N New Messages" for a folded group).
 *  - `VISIBILITY_SECRET`: dropped, it holds no slot until the phone is unlocked.
 *
 * Only app messages ([IslandMode.Notification], other than calls and alarms) are masked. Media and ongoing
 * system tools (music, timer, stopwatch, recording, navigation, calls, transfers) always show in full.
 */
object LockscreenPrivacy {

    /** True when [notification] must not reach the island at all while locked. */
    fun isHiddenWhenLocked(notification: IslandNotification): Boolean =
        isMaskable(notification) && notification.lockscreenVisibility <= LockscreenVisibility.SECRET

    /** True when [notification]'s content must be stripped while locked. */
    fun needsMask(notification: IslandNotification): Boolean =
        isMaskable(notification) && notification.lockscreenVisibility == LockscreenVisibility.PRIVATE

    /** The locked copy of [notification]: app icon and a count, nothing the sender wrote. */
    fun mask(notification: IslandNotification): IslandNotification =
        if (!needsMask(notification)) notification
        else notification.copy(
            title = fallbackTitle(notification.groupCount),
            text = "",
            largeIcon = null,
            groupIcons = emptyList(),
            isMasked = true
        )

    /**
     * The visibility the island honours for one notification: the app's own `Notification.visibility`
     * ([appVisibility]), tightened by its channel's lockscreen override ([channelVisibility], null for "no
     * override"), and secret for everything when the OS shows no notifications on the lockscreen at all
     * ([osShowsNotifications] false). It never loosens what the app or the channel asked for.
     */
    fun effectiveVisibility(appVisibility: Int, channelVisibility: Int?, osShowsNotifications: Boolean): Int {
        if (!osShowsNotifications) return LockscreenVisibility.SECRET
        val app = appVisibility.coerceIn(LockscreenVisibility.SECRET, LockscreenVisibility.PUBLIC)
        val channel = channelVisibility?.coerceIn(LockscreenVisibility.SECRET, LockscreenVisibility.PUBLIC)
        return if (channel == null) app else minOf(app, channel)
    }

    /** "1 New Message", "3 New Messages". */
    fun fallbackTitle(count: Int): String =
        if (count <= 1) "1 New Message" else "$count New Messages"

    // A call or an alarm posted as a plain notification is still a system task, so it is never masked.
    private val unmaskedCategories = setOf("call", "alarm")

    private fun isMaskable(notification: IslandNotification): Boolean =
        notification.mode == IslandMode.Notification &&
            notification.category?.lowercase() !in unmaskedCategories
}
