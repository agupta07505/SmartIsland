/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Ported from Vorssaint (c) 2026 Vorssaint, GPL-3.0-or-later, https://github.com/vorssaint/vorssaint-utils
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.model.IslandMode

/** The parts of an island notification the buddy cares about, so the mapping is testable on the JVM. */
data class NoteSnapshot(
    val key: String,
    val mode: IslandMode,
    val title: String = "",
    val text: String = "",
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mediaIsPlaying: Boolean = false,
    /** For a timer: when it ends, in epoch milliseconds. */
    val timeMillis: Long = 0L
)

enum class DownloadOutcome { Finished, Failed }

/**
 * Reads a transfer's result from the words of a notification. Deliberately small and English-leaning, with
 * the app's other locales' common words; an unknown wording just means no reaction.
 */
object MascotDownloadText {
    private val subjects = listOf(
        "download", "upload", "transfer", "backup", "back up", "sync", "install", "export", "import",
        "baixa", "envio", "transferência", "下载", "上传", "传输", "备份"
    )
    private val failures = listOf(
        "failed", "failure", "couldn't", "could not", "unable to", "unsuccessful", "interrupted", "error",
        "falhou", "falha", "erro", "失败", "出错", "错误"
    )
    private val successes = listOf(
        "complete", "finished", "done", "successful", "downloaded", "uploaded", "saved",
        "concluíd", "finalizad", "完成", "已下载", "已上传"
    )

    /** The outcome the words describe, or null when they are not about a transfer finishing or failing. */
    fun classify(title: String, text: String): DownloadOutcome? {
        val words = "$title $text".lowercase()
        if (subjects.none { it in words }) return null
        if (failures.any { it in words }) return DownloadOutcome.Failed
        if (successes.any { it in words }) return DownloadOutcome.Finished
        return null
    }
}

/**
 * Turns changes in the island's notifications into things the buddy reacts to. Feed it every new list; it
 * remembers the last one. Single-threaded.
 */
class MascotEventMapper {
    private var previous: Map<String, NoteSnapshot> = emptyMap()
    private var lastGrooveSec = Double.NEGATIVE_INFINITY

    fun reset() {
        previous = emptyMap()
    }

    fun onChange(notes: List<NoteSnapshot>, nowSec: Double, nowMillis: Long): List<MascotMoment> {
        val current = notes.associateBy { it.key }
        val moments = ArrayList<MascotMoment>()
        for ((key, note) in current) {
            val before = previous[key]
            when (note.mode) {
                IslandMode.Music ->
                    if (note.mediaIsPlaying && before?.mediaIsPlaying != true &&
                        nowSec - lastGrooveSec > GROOVE_INTERVAL_SEC
                    ) {
                        lastGrooveSec = nowSec
                        moments += MascotMoment.Music
                    }
                IslandMode.ScreenRecording -> if (before == null) moments += MascotMoment.ScreenRecording
                IslandMode.Timer -> if (before == null) moments += MascotMoment.TimerStarted
                IslandMode.Notification ->
                    // A "download complete" (or failed) message replaces the progress one in many apps.
                    if (before == null) {
                        when (MascotDownloadText.classify(note.title, note.text)) {
                            DownloadOutcome.Finished -> moments += MascotMoment.DownloadFinished
                            DownloadOutcome.Failed -> moments += MascotMoment.DownloadFailed
                            null -> Unit
                        }
                    }
                else -> Unit
            }
        }
        for ((key, old) in previous) {
            if (key in current) continue
            when (old.mode) {
                IslandMode.Timer ->
                    // It ran out (rather than being cancelled) when its end was at hand.
                    if (old.timeMillis > 0 && old.timeMillis <= nowMillis + TIMER_END_SLACK_MS) {
                        moments += MascotMoment.TimeIsUp
                    }
                IslandMode.DownloadUpload -> {
                    val outcome = MascotDownloadText.classify(old.title, old.text)
                    val complete = old.progressMax > 0 && old.progress * 100 >= old.progressMax * 95
                    when {
                        outcome == DownloadOutcome.Failed -> moments += MascotMoment.DownloadFailed
                        complete || outcome == DownloadOutcome.Finished -> moments += MascotMoment.DownloadFinished
                    }
                }
                else -> Unit
            }
        }
        previous = current
        return moments
    }

    companion object {
        /** After a song starts, the buddy bobs along at most once in this long. */
        const val GROOVE_INTERVAL_SEC = 600.0
        /** A timer that disappears within this long of its end has finished. */
        const val TIMER_END_SLACK_MS = 2500L
    }
}
