/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class TransferKind(val pointsUp: Boolean, val badge: String, val fallbackTitle: String) {
    DOWNLOAD(false, "Downloading", "Downloading file"),
    UPLOAD(true, "Uploading", "Uploading file"),
    BACKUP(true, "Backing up", "Backing up"),
    COPY_MOVE(false, "Copying", "Copying files"),
    DELETE(false, "Deleting", "Deleting files"),
    INSTALL(false, "Installing", "Installing app")
}

enum class TransferState(val badge: String?) {
    ACTIVE(null),
    PAUSED("Paused"),
    WAITING("Waiting"),
    FAILED("Failed"),
    COMPLETE("Done")
}

object TransferTick {
    const val HOLD_MS = 1_000L

    fun isDone(n: IslandNotification): Boolean =
        n.mode == IslandMode.DownloadUpload &&
            (n.transferState == TransferState.COMPLETE || (n.progressMax > 0 && n.progress >= n.progressMax))
}

class TransferTickHold(private val holdMs: Long = TransferTick.HOLD_MS) {
    var key: String? = null
        private set
    private var untilMs = 0L

    fun begin(key: String, nowMs: Long) {
        this.key = key
        untilMs = nowMs + holdMs
    }

    fun active(nowMs: Long): Boolean = key != null && nowMs < untilMs

    fun remainingMs(nowMs: Long): Long = if (key == null) 0L else (untilMs - nowMs).coerceAtLeast(0L)

    fun clear() {
        key = null
        untilMs = 0L
    }
}
