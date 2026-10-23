/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.Bundle

private const val MAX_BIG_TEXT_CHARS = 1500
private const val MAX_MESSAGE_LINES = 6

/**
 * The text the shade shows when a notification is expanded: the BigText body, an inbox list, or the latest chat
 * messages. Empty when the notification has nothing beyond its one-line text.
 */
internal fun Notification.expandedText(): String {
    val extras: Bundle = extras ?: return ""
    val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
    if (big.isNotEmpty()) return big.take(MAX_BIG_TEXT_CHARS)

    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        ?.mapNotNull { it?.toString()?.trim()?.takeIf { line -> line.isNotEmpty() } }
        .orEmpty()
    if (lines.isNotEmpty()) return lines.joinToString("\n").take(MAX_BIG_TEXT_CHARS)

    // Chat apps: the last few messages of the conversation.
    @Suppress("DEPRECATION")
    val messages = runCatching { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }.getOrNull()
    if (!messages.isNullOrEmpty()) {
        val texts = messages
            .filterIsInstance<Bundle>()
            .mapNotNull { it.getCharSequence("text")?.toString()?.trim()?.takeIf { text -> text.isNotEmpty() } }
            .takeLast(MAX_MESSAGE_LINES)
        if (texts.size > 1) return texts.joinToString("\n").take(MAX_BIG_TEXT_CHARS)
    }
    return ""
}

/**
 * The text the picture reader keeps next to a notification's pictures. Chat apps: the messages that carry no picture
 * (a picture message's label and caption are left out, the thumbnail stands for it).
 */
internal fun Notification.readerTextWithoutPictures(shortText: String): String {
    val extras: Bundle = extras ?: return ""
    @Suppress("DEPRECATION")
    val messages = runCatching { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }.getOrNull()
        ?.filterIsInstance<Bundle>()
        .orEmpty()
    if (messages.isNotEmpty()) {
        return textWithoutPictureMessages(
            messages.map { message ->
                val mime = runCatching { message.getString("type") }.getOrNull()
                val hasPicture = runCatching { message.containsKey("uri") }.getOrDefault(false) &&
                    (mime == null || mime.startsWith("image/"))
                MessageLine(
                    text = runCatching { message.getCharSequence("text")?.toString() }.getOrNull().orEmpty(),
                    hasPicture = hasPicture
                )
            }
        )
    }
    val text = expandedText().ifBlank { shortText }
    return if (isPictureLabel(text)) "" else text
}
