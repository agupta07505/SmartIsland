/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Retry schedule for late pictures. A chat app posts the message first and the picture a moment later (the link is
 * empty until the download finishes, or the file is not in the app's folder yet).
 */
object PictureRetryPlan {
    /** Wait before each attempt, in ms: the first one right away, then with growing gaps (about 30 s in all). */
    val DELAYS_MS: List<Long> = listOf(0L, 1_500L, 4_000L, 9_000L, 16_000L)

    /**
     * Whether the notification is worth asking about again when the fast read found no picture: it carries a picture
     * link ([hasImageLink]), a big-picture style ([hasBigPicture]), or its text is only a picture label such as
     * "Photo" ([textIsPictureLabel]).
     */
    fun expectsPicture(hasImageLink: Boolean, hasBigPicture: Boolean, textIsPictureLabel: Boolean): Boolean =
        hasImageLink || hasBigPicture || textIsPictureLabel

    fun hasNextAttempt(attemptIndex: Int): Boolean = attemptIndex + 1 < DELAYS_MS.size
}
