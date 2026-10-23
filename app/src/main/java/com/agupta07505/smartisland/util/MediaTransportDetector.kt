/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Recognises a media player's notification from its buttons alone (Previous / Play-Pause / Next).
 *
 * Most players use MediaStyle, but video apps, popup / picture-in-picture players and some
 * background-play clients draw their own buttons and also keep a progress bar. Without this they
 * score as downloads (a bar plus Pause / Stop buttons) and the island shows "Downloading".
 */
object MediaTransportDetector {
    private val PREVIOUS = listOf("previous", "prev", "rewind", "anterior", "précédent", "vorherige", "precedente", "上一")
    private val NEXT = listOf("next", "skip", "siguiente", "suivant", "nächste", "successivo", "下一")
    private val PLAY_PAUSE = listOf("pause", "play", "pausa", "reproducir", "lecture", "wiedergabe", "暂停", "播放")

    fun hasTransportButtons(labels: List<String>): Boolean {
        val l = labels.map { it.lowercase() }
        return l.any { t -> PREVIOUS.any { t.contains(it) } } &&
            l.any { t -> NEXT.any { t.contains(it) } } &&
            l.any { t -> PLAY_PAUSE.any { t.contains(it) } }
    }
}
