/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Late pictures. A chat app posts the message first and has the picture ready a moment later (the
 * link is empty until the download finishes, or the file is not in the app's folder yet). Reading the picture while
 * building the card, including Shizuku's slow fallbacks, would make the card itself come late, and a picture that
 * was not ready would never be asked for again.
 *
 * So the card is posted at once with whatever the fast read found, and a background job asks again on this schedule
 * (with the fallbacks) until a picture turns up, the card goes, or a newer post for the key arrives.
 */
object PictureRetryPlan {
    /** Wait before each attempt, in ms: the first one right away, then with growing gaps (about 30 s in all). */
    val DELAYS_MS: List<Long> = listOf(0L, 1_500L, 4_000L, 9_000L, 16_000L)

    /**
     * Whether the notification is worth asking about again when the fast read found no picture: it carries a picture link
     * ([hasImageLink]), a big-picture style ([hasBigPicture]), or its text is only a picture label such as "Photo"
     * ([textIsPictureLabel]). A plain text message is never retried.
     */
    fun expectsPicture(hasImageLink: Boolean, hasBigPicture: Boolean, textIsPictureLabel: Boolean): Boolean =
        hasImageLink || hasBigPicture || textIsPictureLabel

    /** True while another attempt is left after [attemptIndex] (0-based) failed. */
    fun hasNextAttempt(attemptIndex: Int): Boolean = attemptIndex + 1 < DELAYS_MS.size
}
