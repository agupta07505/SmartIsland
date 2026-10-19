/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandNotificationAction

/** Mirrors the `Notification.Action.SEMANTIC_ACTION_*` values the island matches on, so it stays free of Android. */
object SemanticAction {
    const val NONE = 0
    const val MUTE = 6
    const val UNMUTE = 7
    const val CALL = 10
}

/**
 * The island buttons that fire one of the source notification's own actions (alarm, call and recorder cards).
 * A button is shown only when [NotificationActionMatcher.find] finds the matching action, so a card never
 * offers something the app does not provide.
 */
enum class IslandControl(
    val keywords: List<String>,
    val semantics: Set<Int> = emptySet(),
    /** An action whose label has one of these words is never this control (e.g. "Snooze" is not "Stop"). */
    val excludes: List<String> = emptyList()
) {
    AlarmSnooze(listOf("snooze", "remind me later", "later", "rappeler", "posponer", "schlummern", "スヌーズ", "稍后提醒")),
    AlarmStop(listOf("stop", "dismiss", "turn off", "off", "done", "detener", "arrêter", "beenden", "停止", "关闭"), excludes = listOf("snooze")),
    CallAnswer(listOf("answer", "accept", "take", "pick up"), semantics = setOf(SemanticAction.CALL)),
    CallDecline(listOf("decline", "reject", "dismiss", "ignore", "hang up", "hangup", "end call", "cancel")),
    CallEnd(listOf("hang up", "hangup", "end call", "end", "disconnect", "leave", "decline")),
    CallMute(listOf("mute", "unmute", "microphone", "mic"), semantics = setOf(SemanticAction.MUTE, SemanticAction.UNMUTE)),
    CallSpeaker(listOf("speaker", "speakerphone", "loudspeaker", "audio route", "earpiece")),
    RecorderPause(listOf("pause", "pausa", "暂停", "一時停止")),
    RecorderResume(listOf("resume", "continue", "record", "start", "reanudar", "reprendre", "weiter", "继续", "再開")),
    RecorderStop(listOf("stop", "save", "done", "finish", "detener", "speichern", "停止", "保存"))
}

/** Finds a notification's action for an island control, by semantic action first and label second. Pure. */
object NotificationActionMatcher {

    fun find(actions: List<IslandNotificationAction>, control: IslandControl): IslandNotificationAction? {
        if (control.semantics.isNotEmpty()) {
            actions.firstOrNull { it.semanticAction in control.semantics }?.let { return it }
        }
        // Keywords are tried in order, so "hang up" wins over a later, looser "decline" on the same card.
        for (keyword in control.keywords) {
            actions.firstOrNull { action ->
                val label = action.title.lowercase()
                hasWord(label, keyword) && control.excludes.none { hasWord(label, it) }
            }?.let { return it }
        }
        return null
    }

    /**
     * A recorder is paused when it offers Resume and no Pause, or says "paused". Its duration then stops
     * counting (Rule_Book §4.3).
     */
    fun isRecorderPaused(actions: List<IslandNotificationAction>, title: String, text: String): Boolean {
        val shown = "$title $text".lowercase()
        if (PAUSED_WORDS.any { hasWord(shown, it) }) return true
        return find(actions, IslandControl.RecorderPause) == null && find(actions, IslandControl.RecorderResume) != null
    }

    private val PAUSED_WORDS = listOf("paused", "en pause", "pausado", "pausada", "angehalten", "已暂停", "一時停止中")

    /**
     * Whole-word match: "end" finds "End call" but not "Send"; scripts without spaces (CJK) fall back to a
     * plain substring match.
     */
    internal fun hasWord(label: String, keyword: String): Boolean {
        if (keyword.isEmpty()) return false
        if (keyword.any { it.code > 0x2E80 }) return label.contains(keyword)
        var from = 0
        while (true) {
            val at = label.indexOf(keyword, from)
            if (at < 0) return false
            val before = if (at == 0) ' ' else label[at - 1]
            val afterIndex = at + keyword.length
            val after = if (afterIndex >= label.length) ' ' else label[afterIndex]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            from = at + 1
        }
    }
}
