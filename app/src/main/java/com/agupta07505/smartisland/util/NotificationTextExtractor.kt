/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.os.Bundle

internal object NotificationTextExtractor {
    fun extractFullText(notification: Notification): String {
        val extras = notification.extras ?: return ""
        fun text(key: String) = quietly { extras.getCharSequence(key)?.toString() }.orEmpty()
        val textLines = quietly {
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ") { it.toString() }
        }.orEmpty()
        return listOf(
            text(Notification.EXTRA_TITLE),
            text(Notification.EXTRA_TEXT),
            text(Notification.EXTRA_SUB_TEXT),
            text(Notification.EXTRA_BIG_TEXT),
            text(Notification.EXTRA_INFO_TEXT),
            text(Notification.EXTRA_SUMMARY_TEXT),
            quietly { notification.tickerText?.toString() }.orEmpty(),
            textLines
        ).joinToString(" ").lowercase()
    }

    fun extractActionLabels(notification: Notification): List<String> =
        (quietly { notification.actions } ?: emptyArray()).mapNotNull {
            quietly { it?.title?.toString()?.lowercase() }
        }
}

private const val MAX_BIG_TEXT_CHARS = 1500
private const val MAX_MESSAGE_LINES = 6

/** Text the shade shows when expanded: the BigText body, an inbox list or the latest chat messages. */
internal fun Notification.expandedText(): String {
    val extras: Bundle = extras ?: return ""
    val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
    if (big.isNotEmpty()) return big.take(MAX_BIG_TEXT_CHARS)

    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        ?.mapNotNull { it?.toString()?.trim()?.takeIf { line -> line.isNotEmpty() } }
        .orEmpty()
    if (lines.isNotEmpty()) return lines.joinToString("\n").take(MAX_BIG_TEXT_CHARS)

    // Chat apps: the last few messages of the conversation
    @Suppress("DEPRECATION")
    val messages = quietly { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }
    if (!messages.isNullOrEmpty()) {
        val texts = messages
            .filterIsInstance<Bundle>()
            .mapNotNull { it.getCharSequence("text")?.toString()?.trim()?.takeIf { text -> text.isNotEmpty() } }
            .takeLast(MAX_MESSAGE_LINES)
        if (texts.size > 1) return texts.joinToString("\n").take(MAX_BIG_TEXT_CHARS)
    }
    return ""
}

internal fun Notification.readerTextWithoutPictures(shortText: String): String {
    val extras: Bundle = extras ?: return ""
    @Suppress("DEPRECATION")
    val messages = quietly { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }
        ?.filterIsInstance<Bundle>()
        .orEmpty()
    if (messages.isNotEmpty()) {
        return textWithoutPictureMessages(
            messages.map { message ->
                val mime = quietly { message.getString("type") }
                val hasPicture = quietly(false) { message.containsKey("uri") } &&
                    (mime == null || mime.startsWith("image/"))
                MessageLine(
                    text = quietly { message.getCharSequence("text")?.toString() }.orEmpty(),
                    hasPicture = hasPicture
                )
            }
        )
    }
    val text = expandedText().ifBlank { shortText }
    return if (isPictureLabel(text)) "" else text
}

internal data class MessageLine(val text: String, val hasPicture: Boolean)

private const val MAX_READER_LINES = 6
private const val MAX_READER_CHARS = 1500

/** Chat text shown next to a message's pictures: only the messages that carry no picture. */
internal fun textWithoutPictureMessages(messages: List<MessageLine>): String =
    messages
        .filter { !it.hasPicture }
        .map { it.text.trim() }
        .filter { it.isNotEmpty() }
        .takeLast(MAX_READER_LINES)
        .joinToString("\n")
        .take(MAX_READER_CHARS)

private val PICTURE_WORDS = setOf(
    "photo", "image", "picture", "pic", "sticker", "animated sticker", "gif", "photo message", "image message",
    "foto", "imagen", "imagem", "immagine", "bild", "image reçue", "photo reçue", "figurinha", "autocollant", "sticker animé",
    "фото", "изображение", "стикер"
)
private val PICTURE_COUNT = Regex("^\\d+\\s+(new\\s+)?(photos|images|pictures|stickers|fotos|bilder|images)$")

/**
 * True when [text] is only the app's label for a picture ("Photo", "📷 Photo", "Sticker", "2 photos"), not real content.
 * For picture notifications that are not chat messages
 */
internal fun isPictureLabel(text: String): Boolean {
    val cleaned = text.trim()
        .trimStart { !it.isLetterOrDigit() }
        .trimEnd { !it.isLetterOrDigit() }
        .lowercase()
    if (cleaned.isEmpty()) return true
    return cleaned in PICTURE_WORDS || PICTURE_COUNT.matches(cleaned)
}

internal fun lastReaderLine(text: String): String =
    text.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }.orEmpty()

/**
 * Whether a notification's text is the chat app's label for a sticker ("Sticker", "Animated sticker", with an emoji
 * or a sender in front), not any text that mentions the word. A photo with the caption "my sticker collection" is a
 * photo
 */
object StickerLabel {
    private val LABEL = Regex("(animated\\s+)?stickers?", RegexOption.IGNORE_CASE)

    fun isLabel(text: String?): Boolean = text.orEmpty().lineSequence().any { line ->
        val part = line.substringAfterLast(':').trim().trimStart { !it.isLetterOrDigit() }.trimEnd()
        LABEL.matches(part)
    }
}
