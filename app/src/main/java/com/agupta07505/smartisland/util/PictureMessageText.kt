/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** One chat message of a notification: its text and whether it carries a picture or sticker. */
internal data class MessageLine(val text: String, val hasPicture: Boolean)

private const val MAX_READER_LINES = 6
private const val MAX_READER_CHARS = 1500

/** The chat text the reader shows next to a message's pictures: only the messages that carry no picture. */
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
 * True when [text] is only the app's label for a picture ("Photo", "📷 Photo", "Sticker", "2 photos"), not real
 * content. Used for picture notifications that are not chat messages.
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
