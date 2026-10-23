/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Half-typed replies per notification key. A draft survives Cancel, a collapse, the shade opening or another card
 * taking over, and ends when the reply is sent or the notification is gone
 */
class ReplyDrafts {
    private val drafts = HashMap<String, String>()

    /** Key of the card whose reply box must open again (typing resumes after a call took the island). */
    val resume = MutableStateFlow<String?>(null)

    val closeTick = MutableStateFlow(0)

    @Synchronized
    fun get(key: String): String = drafts[key].orEmpty()

    @Synchronized
    fun set(key: String, text: String) {
        if (text.isEmpty()) drafts.remove(key) else drafts[key] = text
    }

    @Synchronized
    fun clear(key: String) {
        drafts.remove(key)
    }

    @Synchronized
    fun retainOnly(liveKeys: Set<String>) {
        drafts.keys.retainAll(liveKeys)
    }
}

fun List<IslandNotificationAction>.replyTarget(): IslandNotificationAction? =
    firstOrNull { it.hasRemoteInput && it.pendingIntent != null }

/** Tapping this button opens the island's reply box (otherwise it fires the app's own intent like any button). */
fun IslandNotificationAction.opensReplyBox(): Boolean = hasRemoteInput && pendingIntent != null

const val SENT_REPLY_LABEL = "You: "

/**
 * Card after a reply was sent: the typed message is added to the card (short and reader text) instead of closing it and
 * getting the app's echo as a new card. A re-post of the same key replaces it
 */
fun IslandNotification.withSentReply(message: String): IslandNotification {
    val sent = message.trim()
    if (sent.isEmpty()) return this
    val line = SENT_REPLY_LABEL + sent
    val history = bigText.ifBlank { text }
    return copy(
        text = line,
        bigText = if (history.isBlank()) line else history + "\n" + line,
        suggestedReplies = emptyList(),
        systemCopyAction = null
    )
}

/**
 * Cards that were replied to. The assistant keeps the old suggestions on a notification until the app re-posts it,
 * so without this a ranking update would bring the chips back after a reply. A post made after the grace window
 * is a new message and gets its suggestions again
 */
class RepliedCards(private val graceMs: Long) {
    private val repliedAt = HashMap<String, Long>()

    @Synchronized
    fun markReplied(key: String, nowMs: Long) {
        repliedAt[key] = nowMs
    }

    @Synchronized
    fun hidesSuggestions(key: String, postTimeMs: Long): Boolean =
        repliedAt[key]?.let { postTimeMs <= it + graceMs } ?: false

    @Synchronized
    fun forget(key: String) {
        repliedAt.remove(key)
    }
}
