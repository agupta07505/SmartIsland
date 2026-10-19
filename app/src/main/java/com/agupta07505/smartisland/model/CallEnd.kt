/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class CallEndReason { ByUser, ByOther }

data class CallEnd(val reason: CallEndReason, val durationMs: Long)

object CallEnds {
    const val CARD_KEY = "call_ended"

    const val HANG_UP_WINDOW_MS = 10_000L

    /**
     * Android does not say who ended a call: only a hang-up from the island itself is known to be the user's, any
     * other end is taken as the other person's
     */
    fun reasonOf(hungUpAtMs: Long?, endedAtMs: Long): CallEndReason =
        if (hungUpAtMs != null && endedAtMs - hungUpAtMs in 0L..HANG_UP_WINDOW_MS) CallEndReason.ByUser
        else CallEndReason.ByOther

    fun canEnd(card: IslandNotification): Boolean =
        card.mode == IslandMode.IncomingCall && !card.isCallRinging && !card.isCallConnecting

    fun title(reason: CallEndReason): String = when (reason) {
        CallEndReason.ByUser -> "Call ended"
        CallEndReason.ByOther -> "Hung up"
    }

    fun pulseTitle(card: IslandNotification): String? = card.callEnd?.let { title(it.reason) }

    fun cardOf(card: IslandNotification, reason: CallEndReason, endedAtMs: Long): IslandNotification =
        card.copy(
            key = CARD_KEY,
            mode = IslandMode.CallEnded,
            text = "",
            actionIntents = emptyList(),
            callStatusText = "",
            timeMillis = endedAtMs,
            callEnd = CallEnd(reason, (endedAtMs - card.timeMillis).coerceAtLeast(0L))
        )

    fun format(durationMs: Long): String {
        val seconds = durationMs.coerceAtLeast(0L) / 1000
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds % 60)
        else "%02d:%02d".format(minutes, seconds % 60)
    }

    fun compact(durationMs: Long): String {
        val seconds = durationMs.coerceAtLeast(0L) / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h ${seconds % 3600 / 60}m"
        }
    }
}
