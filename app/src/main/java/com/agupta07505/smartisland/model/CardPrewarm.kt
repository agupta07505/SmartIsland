/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object CardPrewarm {
    private val modes = setOf(
        IslandMode.Notification,
        IslandMode.IncomingCall,
        IslandMode.CallEnded,
        IslandMode.LiveActivity,
        IslandMode.Navigation,
        IslandMode.DownloadUpload,
        IslandMode.Hotspot,
        IslandMode.Flashlight,
        // Pulse and clock cards: endless animations stop while the card waits, so it costs a composition, not a frame
        // loop
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording
    )

    fun shouldPrewarm(mode: IslandMode?): Boolean = mode != null && mode in modes

    fun shouldPrewarm(mode: IslandMode?, screenOn: Boolean): Boolean =
        shouldPrewarm(mode) || (screenOn && mode == IslandMode.Music)
}
