/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Picks the one Answer and the one Decline button for the island out of a dialer's buttons, which may be three or
 * more ("Decline", "Answer", "Decline with message"). Works on titles so it can be unit tested.
 */
object CallButtonPick {
    private val PLAIN_ANSWER = setOf("answer", "accept", "answer call", "accept call", "answer the call", "take call")
    private val PLAIN_DECLINE = setOf(
        "decline", "reject", "hang up", "hangup", "end call", "end", "dismiss", "decline call", "reject call"
    )
    // an extra that is not the plain decline, e.g. "Decline with message"
    private val EXTRA_WORDS = listOf("message", "sms", "text", "reply", "voicemail", "busy", "later", "silence")

    /** Index into [titles] of the best button for [role] (Answer or HangUp), or null when there is none. */
    fun pick(titles: List<String>, role: CallActionRole): Int? {
        if (role != CallActionRole.Answer && role != CallActionRole.HangUp) return null
        val plain = if (role == CallActionRole.Answer) PLAIN_ANSWER else PLAIN_DECLINE
        val candidates = titles.withIndex()
            .filter { VoipCallDetector.classifyCallAction(it.value) == role }
        if (candidates.isEmpty()) return null
        fun rank(title: String): Int {
            val t = title.lowercase().trim()
            return when {
                t in plain -> 0
                EXTRA_WORDS.none { t.contains(it) } -> 1
                else -> 2
            }
        }
        // lowest rank wins, then the shorter title, then the earlier button
        return candidates.minWithOrNull(
            compareBy<IndexedValue<String>>({ rank(it.value) }, { it.value.length }, { it.index })
        )?.index
    }
}
