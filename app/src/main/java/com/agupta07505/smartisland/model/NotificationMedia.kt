/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.graphics.Bitmap

class NotificationMedia(
    val still: Bitmap,
    val animatedBytes: ByteArray? = null,
    val isSticker: Boolean = false,
    /**
     * Belongs to the newest message of the notification. A chat keeps older pictures in its history, so a new text
     * message must not pulse with the icon of a sticker or photo sent before it
     */
    val inLatestMessage: Boolean = true
) {
    fun inMessage(latest: Boolean): NotificationMedia =
        if (latest == inLatestMessage) this else NotificationMedia(still, animatedBytes, isSticker, latest)
}
