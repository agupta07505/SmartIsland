/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Cards composed and measured while the island is collapsed, so opening it is cheap (see `PrewarmedCard`). */
object CardPrewarm {
    private val modes = setOf(
        IslandMode.Notification,
        IslandMode.IncomingCall,
        IslandMode.LiveActivity,
        IslandMode.Navigation,
        IslandMode.DownloadUpload,
        IslandMode.Hotspot,
        IslandMode.Flashlight,
        // Pulse and clock cards: their endless animations stop while the card waits, so they cost a composition, not a
        // frame loop.
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording
    )

    fun shouldPrewarm(mode: IslandMode?): Boolean = mode != null && mode in modes

    /** Also the media card, pre-composed only while the screen is on; its loops pause while it is not shown. */
    fun shouldPrewarm(mode: IslandMode?, screenOn: Boolean): Boolean =
        shouldPrewarm(mode) || (screenOn && mode == IslandMode.Music)
}
