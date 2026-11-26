/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.Notification
import android.media.AudioManager

enum class CallActionRole { Answer, HangUp, Mute, Unmute, Speaker, Other }

/** Plain values read from a notification, so the scoring can be unit tested. */
data class VoipNotificationFacts(
    val packageName: String,
    val isOngoing: Boolean,
    val category: String?,
    val isCallStyle: Boolean,
    val actionTitles: List<String>,
    val showsChronometer: Boolean,
    val isMediaStyle: Boolean,
    val textLooksLikeCall: Boolean
)

object VoipCallDetector {
    private const val THRESHOLD = 4

    fun isVoipAudioMode(mode: Int): Boolean = mode == AudioManager.MODE_IN_COMMUNICATION

    private val UNMUTE_WORDS = listOf(
        "unmute", "stummschaltung aufheben", "reactivar micr", "activar micr", "réactiver le micro",
        "取消静音", "ミュート解除", "включить микрофон"
    )
    private val MUTE_WORDS = listOf(
        "mute", "silenciar", "stumm", "couper le micro", "静音", "ミュート", "отключить микрофон", "म्यूट"
    )
    private val SPEAKER_WORDS = listOf(
        "speaker", "loudspeaker", "earpiece", "handset", "altavoz", "lautsprecher", "haut-parleur",
        "扬声器", "スピーカー", "динамик"
    )
    private val ANSWER_WORDS = listOf(
        "answer", "accept", "take", "pick up", "responder", "contestar", "annehmen", "décrocher",
        "接听", "応答", "ответить", "answer call", "accept call", "answer the call", "take call",
        "take the call", "answer video call", "answer audio call", "pick up call", "接听电话", "接听通话",
        "responder llamada", "contestar llamada", "anruf annehmen", "ответить на звонок"
    )
    private val HANGUP_WORDS = listOf(
        "hang up", "hangup", "end call", "end", "leave", "disconnect", "decline", "reject", "hang",
        "colgar", "rechazar", "finalizar", "auflegen", "ablehnen", "raccrocher", "refuser",
        "挂断", "拒绝", "切断", "拒否", "завершить", "отклонить"
    )
    private val SPEAKER_ACTIVE_WORDS = listOf(
        "earpiece", "handset", "speaker off", "speakerphone off", "turn off speaker", "switch to phone"
    )
    private val CALL_TEXT_WORDS = listOf(
        "ongoing call", "in call", "call in progress", "voice call", "video call", "calling",
        "llamada", "appel", "anruf", "通话", "通話", "звонок"
    )

    private fun containsWord(text: String, word: String): Boolean {
        if (word.any { it.code > 127 }) return text.contains(word)
        return Regex("(?<![\\p{L}])" + Regex.escape(word) + "(?![\\p{L}])").containsMatchIn(text)
    }

    private fun matches(text: String, words: List<String>): Boolean = words.any { containsWord(text, it) }

    fun classifyCallAction(title: String): CallActionRole {
        val t = title.lowercase().trim()
        return when {
            matches(t, UNMUTE_WORDS) -> CallActionRole.Unmute
            matches(t, MUTE_WORDS) -> CallActionRole.Mute
            matches(t, SPEAKER_WORDS) -> CallActionRole.Speaker
            t in ANSWER_WORDS -> CallActionRole.Answer
            matches(t, HANGUP_WORDS) -> CallActionRole.HangUp
            else -> CallActionRole.Other
        }
    }

    fun isSpeakerOnLabel(title: String): Boolean = matches(title.lowercase(), SPEAKER_ACTIVE_WORDS)

    fun textLooksLikeCall(text: String): Boolean = matches(text.lowercase(), CALL_TEXT_WORDS)

    fun score(f: VoipNotificationFacts): Int {
        if (f.isMediaStyle) return 0
        val roles = f.actionTitles.map { classifyCallAction(it) }.toSet()
        var s = 0
        if (f.category == Notification.CATEGORY_CALL) s += 4
        if (f.isCallStyle) s += 4
        if (CallActionRole.HangUp in roles) s += 3
        if (CallActionRole.Mute in roles || CallActionRole.Unmute in roles) s += 3
        if (CallActionRole.Speaker in roles) s += 2
        if (CallActionRole.Answer in roles) s += 3
        if (f.isOngoing) {
            s += 1
            if (f.showsChronometer) s += 2
            if (f.textLooksLikeCall) s += 2
        }
        return s
    }

    fun explain(f: VoipNotificationFacts): String {
        if (f.isMediaStyle) return "score=0 (media style is never a call) threshold=$THRESHOLD"
        val roles = f.actionTitles.map { classifyCallAction(it) }
        return "score=${score(f)} threshold=$THRESHOLD category=${f.category} callStyle=${f.isCallStyle}" +
            " ongoing=${f.isOngoing} chronometer=${f.showsChronometer} callText=${f.textLooksLikeCall}" +
            " roles=${roles.joinToString(prefix = "[", postfix = "]")}" +
            " buttons=${f.actionTitles.joinToString(prefix = "[", postfix = "]") { "'$it'" }}"
    }

    fun isVoipCall(f: VoipNotificationFacts): Boolean = score(f) >= THRESHOLD

    fun pickBest(candidates: List<Pair<String, VoipNotificationFacts>>): String? =
        candidates
            .map { (key, facts) -> key to score(facts) }
            .filter { it.second >= THRESHOLD }
            .maxByOrNull { it.second }
            ?.first
}
