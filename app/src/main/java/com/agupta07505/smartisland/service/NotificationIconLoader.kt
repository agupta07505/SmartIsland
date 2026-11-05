/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.app.Person
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.agupta07505.smartisland.util.runCatchingLogged

private const val TAG = "SmartIslandNotificationListener"
private const val ICON_BITMAP_SIZE = 96
private const val LARGE_ICON_BITMAP_SIZE = 128
private const val ACTION_ICON_SIZE = 96
private const val EXTRA_ICON_SIZE = 128
private const val SUB_APP_GLYPH_INSET = 0.24f
private const val SUB_APP_FALLBACK_COLOR = 0xFF5F6368.toInt()

internal class NotificationIconLoader(private val context: Context) {
    private val appIcons = LruCache<String, Bitmap>(50)
    private val smallIcons = LruCache<String, Bitmap>(30)
    private val subAppIcons = LruCache<String, Bitmap>(30)
    private val actionIcons = object : LinkedHashMap<String, Bitmap?>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap?>?): Boolean = size > 48
    }

    fun clear() {
        appIcons.evictAll()
        smallIcons.evictAll()
        subAppIcons.evictAll()
    }

    /**
     * Android can hide the package from lookup even when its notification is visible.
     * The icon then comes from the [appInfo] attached to that notification.
     */
    fun app(packageName: String, appInfo: ApplicationInfo? = null): Bitmap? {
        appIcons.get(packageName)?.let { return it }
        return runCatchingLogged(TAG, "LoadAppIconBitmap failed") {
            val pm = context.packageManager
            val drawable = runCatchingLogged(TAG, "AppIcon by package name failed") {
                pm.getApplicationIcon(packageName)
            } ?: appInfo?.let { pm.getApplicationIcon(it) }
            drawable?.toBitmap(width = ICON_BITMAP_SIZE, height = ICON_BITMAP_SIZE)
                ?.also { appIcons.put(packageName, it) }
        }
    }

    /**
     * Quick Share showed the Google Play services icon because Play services posts its notification.
     * Draw the small notification icon in a tinted circle. Cache by package and sub-app name.
     */
    fun subApp(notification: Notification, subName: String, packageName: String): Bitmap? {
        val key = "$packageName|$subName"
        subAppIcons.get(key)?.let { return it }
        return runCatchingLogged(TAG, "LoadSubAppIconBitmap failed") {
            val glyph = notification.smallIcon?.loadDrawable(context)?.mutate() ?: return@runCatchingLogged null
            val size = ICON_BITMAP_SIZE
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG)
            fill.color = if (notification.color != Notification.COLOR_DEFAULT) notification.color else SUB_APP_FALLBACK_COLOR
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, fill)
            val inset = (size * SUB_APP_GLYPH_INSET).toInt()
            glyph.setTint(Color.WHITE)
            glyph.setBounds(inset, inset, size - inset, size - inset)
            glyph.draw(canvas)
            out
        }?.also { subAppIcons.put(key, it) }
    }

    fun small(notification: Notification, packageName: String): Bitmap? {
        smallIcons.get(packageName)?.let { return it }
        return runCatchingLogged(TAG, "LoadSmallIconBitmap failed") {
            notification.smallIcon?.loadDrawable(context)
                ?.toBitmap(width = ICON_BITMAP_SIZE, height = ICON_BITMAP_SIZE)
                ?.also { smallIcons.put(packageName, it) }
        }
    }

    fun large(notification: Notification): Bitmap? {
        val extras = notification.extras
        extras.get(Notification.EXTRA_LARGE_ICON).toBitmapOrNull()?.let { return it }
        extras.get(Notification.EXTRA_LARGE_ICON_BIG).toBitmapOrNull()?.let { return it }
        runCatchingLogged(TAG, "LoadLargeIconBitmap failed") {
            notification.getLargeIcon()?.loadDrawable(context)
                ?.toBitmap(width = LARGE_ICON_BITMAP_SIZE, height = LARGE_ICON_BITMAP_SIZE)
        }?.let { return it }
        return runCatchingLogged(TAG, "LoadMessagingStyleAvatar failed") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@runCatchingLogged null
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages.isNullOrEmpty()) return@runCatchingLogged null
            val lastMessage = messages.lastOrNull() as? Bundle
            val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lastMessage?.getParcelable("sender_person", Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                lastMessage?.getParcelable("sender_person") as? Person
            }
            sender?.icon?.loadDrawable(context)
                ?.toBitmap(width = LARGE_ICON_BITMAP_SIZE, height = LARGE_ICON_BITMAP_SIZE)
        }
    }

    fun action(action: Notification.Action, packageName: String): Bitmap? {
        val icon = action.getIcon() ?: return null
        val key = "$packageName|$icon"
        synchronized(actionIcons) {
            if (actionIcons.containsKey(key)) return actionIcons[key]
        }
        val bitmap = runCatchingLogged(TAG, "Action icon could not be loaded") {
            val needsAppContext = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                icon.type == Icon.TYPE_RESOURCE && icon.resPackage.isNullOrEmpty()
            val source = if (needsAppContext) context.createPackageContext(packageName, 0) else context
            val drawable = icon.loadDrawable(source) ?: return@runCatchingLogged null
            val out = Bitmap.createBitmap(ACTION_ICON_SIZE, ACTION_ICON_SIZE, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, ACTION_ICON_SIZE, ACTION_ICON_SIZE)
            drawable.draw(Canvas(out))
            out
        }
        synchronized(actionIcons) { actionIcons[key] = bitmap }
        return bitmap
    }

    private fun Any?.toBitmapOrNull(): Bitmap? = when (this) {
        is Bitmap -> this
        is Icon -> runCatchingLogged(TAG, "Icon toBitmapOrNull failed") {
            loadDrawable(context)?.toBitmap(width = EXTRA_ICON_SIZE, height = EXTRA_ICON_SIZE)
        }
        else -> null
    }
}
