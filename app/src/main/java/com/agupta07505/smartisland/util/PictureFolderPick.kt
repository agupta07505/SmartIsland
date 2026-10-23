/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Picks the picture a chat notification is about from a listing of an app's media folders
 * (lines of `modified|accessed|changed|path`, times in seconds).
 *
 * WhatsApp reuses one file per sticker, so a sticker file can be older than its notification. For a sticker the newest of
 * the three times counts, extension-less files are accepted, and only folders with "sticker" in the path qualify.
 */
internal object PictureFolderPick {
    class Candidate(val seconds: Long, val path: String, val modified: Long = seconds)

    private val SKIPPED = listOf("avatar", "profile", "thumb", "icon", "/sent/", "/private/", "emoji", "logo")

    fun parse(listing: String, sticker: Boolean): List<Candidate> = listing.lineSequence().mapNotNull { line ->
        val parts = line.split('|', limit = 4)
        if (parts.size < 4) return@mapNotNull null
        val modified = parts[0].trim().toLongOrNull() ?: return@mapNotNull null
        val accessed = parts[1].trim().toLongOrNull() ?: 0L
        val changed = parts[2].trim().toLongOrNull() ?: 0L
        val seconds = if (sticker) maxOf(modified, accessed, changed) else modified
        Candidate(seconds, parts[3], modified)
    }.toList()

    fun pick(listing: String, postedAtMs: Long, sticker: Boolean): Candidate? =
        parse(listing, sticker)
            .filter { it.seconds * 1000L >= postedAtMs - 120_000L }
            .filter { c -> SKIPPED.none { c.path.contains(it, ignoreCase = true) } }
            .filter { c -> !sticker || c.path.contains("sticker", ignoreCase = true) }
            .maxWithOrNull(compareBy<Candidate>({ it.seconds }, { it.modified })) // equal newest times: the later modified wins
}
