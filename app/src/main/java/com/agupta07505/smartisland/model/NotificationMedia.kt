/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.graphics.Bitmap

/**
 * A picture or sticker a notification carries. [animatedBytes] holds the original file for animated images (WebP /
 * GIF) so they can play.
 */
class NotificationMedia(
    val still: Bitmap,
    val animatedBytes: ByteArray? = null,
    val isSticker: Boolean = false
) {
}
