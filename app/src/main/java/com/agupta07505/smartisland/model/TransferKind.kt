/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * What a progress card (IslandMode.DownloadUpload) is doing. Each kind has its own Home switch and wording on the
 * card; `pointsUp` picks the arrow direction of the transfer glyph.
 */
enum class TransferKind(val pointsUp: Boolean, val badge: String, val fallbackTitle: String) {
    DOWNLOAD(false, "Downloading", "Downloading file"),
    UPLOAD(true, "Uploading", "Uploading file"),
    BACKUP(true, "Backing up", "Backing up"),
    COPY_MOVE(false, "Copying", "Copying files"),
    DELETE(false, "Deleting", "Deleting files"),
    INSTALL(false, "Installing", "Installing app")
}
