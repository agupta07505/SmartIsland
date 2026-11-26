/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandTimings

internal object PostedNotificationRules {
    /** Island makes a sound only when it replaces the system notification. */
    fun shouldPlayIslandSound(
        isInitialSync: Boolean,
        removedFromShade: Boolean,
        isNewNotif: Boolean,
        mode: IslandMode,
        shouldIslandOnly: Boolean,
        isRecordingEvent: Boolean
    ): Boolean =
        !isInitialSync && removedFromShade && isNewNotif &&
            (mode == IslandMode.Notification || shouldIslandOnly) &&
            mode != IslandMode.DownloadUpload && !isRecordingEvent

    fun isReplacedMusicCard(
        cardMode: IslandMode,
        cardPackage: String,
        cardKey: String,
        postedPackage: String,
        postedKey: String
    ): Boolean = cardMode == IslandMode.Music && cardPackage == postedPackage && cardKey != postedKey

    fun isTransferDone(failed: Boolean, complete: Boolean, progress: Int, progressMax: Int): Boolean =
        failed || complete || (progressMax > 0 && progress >= progressMax)

    fun transferLingerMs(failed: Boolean, isInitialSync: Boolean): Long = when {
        failed -> IslandTimings.TRANSFER_FAILED_MS
        isInitialSync -> IslandTimings.TRANSFER_DONE_IMPORTED_MS
        else -> IslandTimings.TRANSFER_DONE_MS
    }

    /** A timer whose shade view still counts down is not finished. */
    fun isTimerDone(finished: Boolean, viewRunningMs: Long?): Boolean =
        finished && (viewRunningMs ?: 0L) <= 0L

    const val TIMER_DONE_LINGER_MS = 5000L
}
