/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

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

/**
 * Retry schedule for late pictures. Chat apps post the message first and the picture a moment later (link empty until
 * the download finishes, or the file is not in the app's folder yet).
 */
object PictureRetryPlan {
    /** Wait before each attempt in ms: the first right away, then growing gaps (about 30 s in all). */
    val DELAYS_MS: List<Long> = listOf(0L, 1_500L, 4_000L, 9_000L, 16_000L)

    /**
     * Worth asking again when the fast read found no picture: it carries a picture link ([hasImageLink]), a big-picture
     * style ([hasBigPicture]), or its text is only a picture label such as "Photo" ([textIsPictureLabel])
     */
    fun expectsPicture(hasImageLink: Boolean, hasBigPicture: Boolean, textIsPictureLabel: Boolean): Boolean =
        hasImageLink || hasBigPicture || textIsPictureLabel

    fun hasNextAttempt(attemptIndex: Int): Boolean = attemptIndex + 1 < DELAYS_MS.size
}
