/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.model.NotificationMedia
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Reads the pictures a notification shows in the shade: the image of a chat message (MessagingStyle data URI, which
 * is how WhatsApp sends photos, stickers and animated stickers) and the big picture of picture-style notifications.
 */
class NotificationMediaReader(private val context: Context) {
    private val cache = LruCache<String, NotificationMedia>(24)

    /**
     * [allowFallbacks] false is the fast read for building the card: the normal open of the picture link only, no
     * Shizuku shell and no folder search (those take seconds).
     */
    fun read(
        key: String, notification: Notification, shadeText: String, packageName: String = "",
        allowFallbacks: Boolean = true
    ): List<NotificationMedia> {
        val extras = notification.extras ?: return emptyList()
        val fromMessages = readMessages(key, extras, shadeText, packageName, notification.`when`, allowFallbacks)
        if (fromMessages.isNotEmpty()) return fromMessages
        return listOfNotNull(readBigPicture(key, extras, shadeText, packageName, notification.`when`, allowFallbacks))
    }

    /** True when the notification carries a picture link, a big picture, or only a picture label as text (see PictureRetryPlan). */
    fun expectsPicture(notification: Notification, shadeText: String): Boolean {
        val extras = notification.extras ?: return false
        val hasLink = runCatching { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }.getOrNull()
            ?.filterIsInstance<Bundle>()?.takeLast(MAX_MESSAGES_LOOKED_AT)
            ?.any { b ->
                val mime = runCatching { b.getString(MESSAGE_TYPE) }.getOrNull()
                messageUri(b) != null && (mime == null || mime.startsWith("image/"))
            } == true
        val hasBig = runCatching {
            extras.containsKey(Notification.EXTRA_PICTURE) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && extras.containsKey(Notification.EXTRA_PICTURE_ICON))
        }.getOrDefault(false)
        return PictureRetryPlan.expectsPicture(hasLink, hasBig, shadeText.isNotBlank() && isPictureLabel(shadeText))
    }

    private fun readMessages(
        key: String, extras: Bundle, shadeText: String, packageName: String, postedAtMs: Long, allowFallbacks: Boolean
    ): List<NotificationMedia> {
        val bundles = runCatching { extras.getParcelableArray(Notification.EXTRA_MESSAGES) }.getOrNull()
            ?.filterIsInstance<Bundle>()
            ?: return emptyList()
        val result = mutableListOf<NotificationMedia>()
        for (bundle in bundles.takeLast(MAX_MESSAGES_LOOKED_AT)) {
            val uri = messageUri(bundle) ?: continue
            val mime = runCatching { bundle.getString(MESSAGE_TYPE) }.getOrNull()
            if (mime != null && !mime.startsWith("image/")) continue
            val cacheKey = "$key|$uri"
            val sticker = isStickerMessage(bundle, mime, shadeText)
            val media = cache.get(cacheKey)
                ?: loadWithFallbacks(uri, mime, shadeText, packageName, postedAtMs, sticker, allowFallbacks)?.also { cache.put(cacheKey, it) }
            if (media != null) result.add(media)
        }
        return result.takeLast(MAX_MEDIA)
    }

    private fun messageUri(bundle: Bundle): Uri? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(MESSAGE_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable<Uri>(MESSAGE_URI)
        }
    }.getOrNull()

    private fun isStickerMessage(bundle: Bundle, mime: String?, shadeText: String): Boolean {
        val own = runCatching { bundle.getCharSequence(MESSAGE_TEXT)?.toString() }.getOrNull().orEmpty()
        return own.contains("sticker", ignoreCase = true) ||
            (own.isBlank() && shadeText.contains("sticker", ignoreCase = true)) ||
            mime == "image/webp"
    }

    private fun loadUri(uri: Uri, mime: String?, shadeText: String, stickerHint: Boolean = false): NotificationMedia? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { readLimited(it) }
        }.onFailure {
            // Some apps do not let a notification listener open their picture links.
            AppLogRecorder.w(TAG, "Picture ${uri.scheme}://${uri.authority} could not be opened: ${it.javaClass.simpleName}")
        }.getOrNull() ?: run {
            AppLogRecorder.w(TAG, "Picture ${uri.scheme}://${uri.authority} gave no data (empty, or over ${MAX_BYTES / 1024 / 1024} MB)")
            return null
        }
        val sticker = stickerHint || shadeText.contains("sticker", ignoreCase = true) || mime == "image/webp"
        return decode(bytes, sticker) ?: run {
            AppLogRecorder.w(TAG, "Picture ${uri.scheme}://${uri.authority} opened (${bytes.size} bytes) but could not be decoded")
            null
        }
    }

    private fun loadWithFallbacks(
        uri: Uri, mime: String?, shadeText: String, packageName: String, postedAtMs: Long, stickerHint: Boolean = false,
        allowFallbacks: Boolean = true
    ): NotificationMedia? {
        loadUri(uri, mime, shadeText, stickerHint)?.let { return it }
        // The fast read for the card stops here; the late-picture job comes back with the fallbacks.
        if (!allowFallbacks) return null
        if (!ShizukuManager.hasPermission()) {
            AppLogRecorder.w(TAG, "Picture fallbacks skipped for $packageName: Shizuku is not available")
            return null
        }
        loadViaShell(uri, mime, shadeText, stickerHint)?.let { return it }
        return loadFromAppFolder(packageName, postedAtMs, shadeText, stickerHint)
    }

    private fun loadViaShell(uri: Uri, mime: String?, shadeText: String, stickerHint: Boolean = false): NotificationMedia? {
        val command = when (uri.scheme) {
            "content" -> "content read --user ${android.os.Process.myUid() / 100000} --uri ${shellQuote(uri.toString())}"
            "file" -> uri.path?.let { "cat ${shellQuote(it)}" }
            else -> null
        } ?: return null
        val bytes = ShizukuManager.runShellForBytes(command, MAX_BYTES)
        if (bytes == null || bytes.isEmpty()) {
            AppLogRecorder.w(TAG, "Picture ${uri.scheme}://${uri.authority} not readable through the shell either")
            return null
        }
        val sticker = stickerHint || shadeText.contains("sticker", ignoreCase = true) || mime == "image/webp"
        return decode(bytes, sticker)
            ?.also { AppLogRecorder.i(TAG, "Picture ${uri.authority} read through Shizuku (${bytes.size} bytes)") }
    }

    private fun loadFromAppFolder(packageName: String, postedAtMs: Long, shadeText: String, stickerHint: Boolean = false): NotificationMedia? {
        if (packageName.isBlank()) return null
        val sticker = stickerHint || shadeText.contains("sticker", ignoreCase = true)
        val roots = buildList {
            add("/storage/emulated/0/Android/media/$packageName")
            add("/storage/emulated/0/Android/data/$packageName/files")
            add("/storage/emulated/0/Android/data/$packageName/cache")
            if (packageName == "com.whatsapp") add("/storage/emulated/0/WhatsApp")
            if (packageName == "com.whatsapp.w4b") add("/storage/emulated/0/WhatsApp Business")
        }.joinToString(" ") { shellQuote(it) }
        val ageMinutes = ((System.currentTimeMillis() - postedAtMs) / 60_000L).coerceIn(0L, 120L) + 3L
        // Stickers: WhatsApp reuses the file when the same sticker arrives again, so it can be old.
        val listCommand = if (sticker) {
            "for r in $roots; do [ -d \"\$r\" ] && find \"\$r\" -type f -ipath '*sticker*' -size +0 " +
                "-exec stat -c '%Y|%X|%Z|%n' {} + 2>/dev/null; done"
        } else {
            "for r in $roots; do [ -d \"\$r\" ] && find \"\$r\" -type f " +
                "\\( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' -o -iname '*.gif' \\) " +
                "-mmin -$ageMinutes -size +2k -exec stat -c '%Y|%X|%Z|%n' {} + 2>/dev/null; done"
        }
        val listing = ShizukuManager.runShellForBytes(listCommand, 256 * 1024)?.toString(Charsets.UTF_8).orEmpty()
        val newest = PictureFolderPick.pick(listing, postedAtMs, sticker)
        if (newest == null) {
            val seen = PictureFolderPick.parse(listing, sticker).size
            AppLogRecorder.w(TAG, "Picture folder fallback found no recent ${if (sticker) "sticker" else "picture"} for $packageName ($seen files listed)")
            return null
        }
        val path = newest.path
        val bytes = ShizukuManager.runShellForBytes("cat ${shellQuote(path)}", MAX_BYTES)
        if (bytes == null || bytes.isEmpty()) {
            AppLogRecorder.w(TAG, "Picture folder fallback could not read the file")
            return null
        }
        return decode(bytes, sticker || path.contains("sticker", ignoreCase = true))
            ?.also { AppLogRecorder.i(TAG, "Picture for $packageName read from its media folder (${bytes.size} bytes, sticker=$sticker)") }
            ?: run {
                AppLogRecorder.w(TAG, "Picture folder fallback opened ${bytes.size} bytes but could not decode them")
                null
            }
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun readLimited(input: java.io.InputStream): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BYTES) return null
        }
        return out.toByteArray()
    }

    private fun readBigPicture(
        key: String, extras: Bundle, shadeText: String, packageName: String, postedAtMs: Long, allowFallbacks: Boolean
    ): NotificationMedia? {
        val cacheKey = "$key|big"
        cache.get(cacheKey)?.let { return it }
        val icons = mutableListOf<Icon>()
        val picture: Bitmap? = runCatching {
            when (val raw = extras.get(Notification.EXTRA_PICTURE)) {
                is Bitmap -> raw
                is Icon -> { icons += raw; raw.loadDrawable(context)?.toBitmap() }
                else -> null
            }
        }.getOrNull() ?: runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_PICTURE_ICON, Icon::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable<Icon>(Notification.EXTRA_PICTURE_ICON)
                }
                if (icon != null) icons += icon
                icon?.loadDrawable(context)?.toBitmap()
            } else null
        }.getOrNull()
        if (picture != null) return NotificationMedia(picture).also { cache.put(cacheKey, it) }
        // The picture is a link the app refuses to the listener: same fallbacks as a chat picture.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            for (icon in icons) {
                val uri = runCatching { if (icon.type == Icon.TYPE_URI || icon.type == Icon.TYPE_URI_ADAPTIVE_BITMAP) icon.uri else null }.getOrNull() ?: continue
                loadWithFallbacks(uri, null, shadeText, packageName, postedAtMs, false, allowFallbacks)?.let {
                    cache.put(cacheKey, it)
                    return it
                }
            }
        }
        return null
    }

    private fun decode(bytes: ByteArray, sticker: Boolean): NotificationMedia? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
            val animated = drawable is AnimatedImageDrawable
            val still = (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
            NotificationMedia(still, if (animated) bytes else null, sticker)
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let { NotificationMedia(it, null, sticker) }
        }
    }.getOrNull()

    private companion object {
        const val TAG = "NotificationMediaReader"
        // Keys of Notification.MessagingStyle.Message.toBundle().
        const val MESSAGE_URI = "uri"
        const val MESSAGE_TYPE = "type"
        const val MESSAGE_TEXT = "text"
        const val MAX_MESSAGES_LOOKED_AT = 6
        const val MAX_MEDIA = 4
        const val MAX_BYTES = 4 * 1024 * 1024
        const val MAX_SIDE = 1024
    }
}
