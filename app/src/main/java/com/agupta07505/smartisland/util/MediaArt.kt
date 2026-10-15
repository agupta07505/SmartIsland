/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.expanded.mediaIsExplicit

/**
 * Everything the media card needs from the session's metadata, worked out OFF the main thread.
 *
 * Reading the metadata, copying the artwork, sampling its colour, reading the audio output and (with blur on) blurring the
 * full-size artwork inside composition on the tap that opens the island would leave the collapsed pill looking dead for
 * seconds (worse with the media blur on). So this is loaded in the background while the pill is still collapsed
 * ([load] from `IslandCardHost`), kept in a small cache, and the card only picks the finished result up.
 */
object MediaArt {

    /** [image] is the background picture, already reduced (and, when [blurred], already blurred): drawn as a plain image. */
    class Result(
        val image: ImageBitmap?,
        val tint: ArtworkTint?,
        val hasArtwork: Boolean,
        val explicit: Boolean,
        /** True when the picture is the session's own (full size); false for the notification's small picture or its app icon, which may still be replaced. */
        val fromSession: Boolean = false,
        /**
         * True when the picture is only the app's own icon (no session picture, no large icon). It is a stand-in, never worth showing
         * over a real cover: the card keeps the previous cover until the new one is read ([lastGood]).
         */
        val isFallbackIcon: Boolean = false
    )

    /**
     * Longest edge of a sharp (not blurred) background picture. The card is about 1000 px wide on a phone and the picture is cropped
     * to fill it, so 360 px was drawn about 3x too big and looked pixelated. A picture smaller than this is never enlarged here.
     */
    private const val MAX_EDGE_PX = 1080

    /** Longest edge a picture is blurred at: a blur hides detail, so it stays small (and fast). */
    private const val BLUR_WORK_EDGE_PX = 128

    /**
     * Blur radius in working pixels per dp of the setting. The picture is worked at 128 px, so 1.4 px per dp makes the low end of the
     * slider already visibly soft and the high end (60 dp, capped at half the picture) a smooth wash of colour: the whole slider shows.
     */
    private const val BLUR_PX_PER_DP = 1.4f

    private const val CACHE_SIZE = 6

    private val cache = object : LinkedHashMap<String, Result>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Result>?): Boolean = size > CACHE_SIZE
    }

    /** Same key = same result: the card, the app's session, its track and the blur it was made for. */
    fun keyFor(n: IslandNotification?, blurRadiusDp: Float?): String =
        "${n?.key}|${n?.title}|${n?.text}|${blurRadiusDp?.toInt() ?: -1}"

    fun cached(key: String): Result? = synchronized(cache) { cache[key] }

    /**
     * The last real cover (not the app icon) each card showed, per card and blur. A track change posts the title first and the picture
     * a moment later; until it arrives the card keeps this one instead of drawing the app's icon enlarged as the backdrop.
     */
    private val lastGoodCovers = object : LinkedHashMap<String, Result>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Result>?): Boolean = size > CACHE_SIZE
    }

    private fun goodKey(cardKey: String, blurRadiusDp: Float?) = "$cardKey|${blurRadiusDp?.toInt() ?: -1}"

    fun lastGood(cardKey: String, blurRadiusDp: Float?): Result? = synchronized(lastGoodCovers) { lastGoodCovers[goodKey(cardKey, blurRadiusDp)] }

    /** The small round picture (collapsed pill, secondary cutout): the large icon, else the one last shown for this card, else the app icon. */
    private val lastGlyphs = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > CACHE_SIZE
    }

    fun glyphBitmap(n: IslandNotification): Bitmap? {
        val large = n.largeIcon
        synchronized(lastGlyphs) {
            if (large != null) {
                lastGlyphs[n.key] = large
                return large
            }
            return lastGlyphs[n.key] ?: n.icon
        }
    }

    /**
     * What the card can show at once, without any metadata read: the notification's own 128 px picture (not blurred) and its
     * colour. Only used for the moment before [load] has finished.
     */
    fun quick(n: IslandNotification?, blurRadiusDp: Float? = null): Result {
        // No picture of its own yet (a track change, the picture comes a moment later): the cover shown before, not the app icon.
        if (n != null && n.largeIcon == null) lastGood(n.key, blurRadiusDp)?.let { return it }
        val small = n?.largeIcon ?: n?.icon
        return Result(
            image = small?.let { runCatchingLogged("MediaArt", "quick artwork failed") { it.asImageBitmap() } },
            tint = small?.let { ArtworkPalette.fromBitmap(it) },
            hasArtwork = small != null,
            explicit = false,
            isFallbackIcon = n != null && n.largeIcon == null && n.icon != null
        )
    }

    /**
     * Blocking: call from a background thread. [blurRadiusDp] null = no blur (the picture is only reduced), else the radius the
     * media blur setting asks for.
     */
    fun load(context: Context, n: IslandNotification, blurRadiusDp: Float?): Result {
        val key = keyFor(n, blurRadiusDp)
        cached(key)?.let { return it }
        val controller = n.mediaToken?.let { token ->
            runCatchingLogged("MediaArt", "Failed to create MediaController") { MediaController(context, token) }
        }
        val meta = runCatchingLogged("MediaArt", "Failed to read the metadata") { controller?.metadata }
        val sessionArt = meta?.let { m ->
            runCatchingLogged("MediaArt", "Failed to extract metadata artwork") {
                m.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            }
        }
        // The session's own artwork first (full size, what the shade draws); the notification's large icon is only 128 px.
        val source = sessionArt ?: n.largeIcon ?: n.icon
        val readable = source?.let { toReadable(it) }
        // Diagnostic: what size each source really is (the session's own picture vs the notification's 128 px one).
        AppLogRecorder.i(
            "MediaArt",
            "artwork sizes session=${sessionArt?.let { "${it.width}x${it.height}" } ?: "none"} " +
                "largeIcon=${n.largeIcon?.let { "${it.width}x${it.height}" } ?: "none"} " +
                "icon=${n.icon?.let { "${it.width}x${it.height}" } ?: "none"} " +
                "used=${if (sessionArt != null) "session" else if (n.largeIcon != null) "largeIcon" else if (n.icon != null) "icon" else "none"} " +
                "pkg=${n.packageName} blur=$blurRadiusDp"
        )
        val result = Result(
            image = readable?.let { reduce(it, blurRadiusDp)?.asImageBitmap() },
            tint = readable?.let { ArtworkPalette.fromBitmap(it) },
            hasArtwork = source != null,
            explicit = meta?.let { mediaIsExplicit(it) } == true,
            fromSession = sessionArt != null,
            isFallbackIcon = sessionArt == null && n.largeIcon == null && n.icon != null
        )
        if (!result.isFallbackIcon && result.hasArtwork) {
            synchronized(lastGoodCovers) { lastGoodCovers[goodKey(n.key, blurRadiusDp)] = result }
        }
        // The session often posts the title before its picture: a result without the session's picture is not kept.
        if (sessionArt != null) synchronized(cache) { cache[key] = result }
        return result
    }

    /**
     * A notification card's own backdrop picture, reduced once so that drawing it back up is the blur (no render effect on the
     * picture every frame of the card's animation). Cheap: the picture is a notification icon. Null radius = unchanged picture.
     */
    fun blurredBackdrop(source: Bitmap, blurRadiusDp: Float?): Bitmap? {
        if (blurRadiusDp == null) return null
        val readable = toReadable(source) ?: return null
        return reduce(readable, blurRadiusDp)
    }

    private fun toReadable(bitmap: Bitmap): Bitmap? =
        if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap

    /**
     * Reduces the picture to at most [MAX_EDGE_PX]. With a blur radius the picture is shrunk to [BLUR_WORK_EDGE_PX] and really blurred
     * (three box-blur passes, close to a gaussian), once, here: drawn back up with bilinear filtering it is smooth. The old way only
     * shrank the picture and stretched it back, which showed big pixel blocks instead of a blur.
     */
    private fun reduce(source: Bitmap, blurRadiusDp: Float?): Bitmap? = runCatchingLogged("MediaArt", "Failed to reduce the artwork") {
        val longest = maxOf(source.width, source.height).coerceAtLeast(1)
        val edge = if (blurRadiusDp == null) MAX_EDGE_PX else BLUR_WORK_EDGE_PX
        val scale = minOf(1f, edge.toFloat() / longest)
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (w == source.width && h == source.height) source else Bitmap.createScaledBitmap(source, w, h, true)
        if (blurRadiusDp == null) return@runCatchingLogged scaled
        val radiusPx = (blurRadiusDp * BLUR_PX_PER_DP).toInt().coerceIn(1, maxOf(1, minOf(w, h) / 2))
        blur(scaled, radiusPx)
    }

    /** Box-blurs [src] (radius [radius] px, three passes) into a new bitmap. */
    internal fun blur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)
        repeat(3) {
            boxPass(px, tmp, w, h, radius, true)
            boxPass(tmp, px, w, h, radius, false)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** One running-sum box pass along rows ([horizontal]) or columns; edges repeat their outermost pixel. */
    private fun boxPass(inp: IntArray, out: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val step = if (horizontal) 1 else w
        val lineStep = if (horizontal) w else 1
        val div = 2 * r + 1
        for (l in 0 until lines) {
            val base = l * lineStep
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = inp[base + i.coerceIn(0, len - 1) * step]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (i in 0 until len) {
                out[base + i * step] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = inp[base + (i + r + 1).coerceAtMost(len - 1) * step]
                val sub = inp[base + (i - r).coerceAtLeast(0) * step]
                a += (add ushr 24) - (sub ushr 24)
                rr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                b += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}
