/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager

/**
 * The physical display glass the overlay is laid out against (Rule_Book §9.3). All overlay layout math (centre
 * X, offsets, width limits) reads its size from here, so split-screen or a freeform app window never shifts
 * the island: on Android 11+ it is `WindowManager.currentWindowMetrics.bounds`, falling back to the
 * resources' display metrics on older releases or if the call fails.
 */
object DisplayBounds {

    fun bounds(context: Context): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fromMetrics = runCatching {
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                    ?.currentWindowMetrics?.bounds
            }.getOrNull()
            if (fromMetrics != null && fromMetrics.width() > 0 && fromMetrics.height() > 0) return Rect(fromMetrics)
        }
        val metrics = context.resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    fun widthPx(context: Context): Int = bounds(context).width()

    fun heightPx(context: Context): Int = bounds(context).height()

    /** Display width in dp. */
    fun widthDp(context: Context): Float {
        val density = context.resources.displayMetrics.density
        return if (density > 0f) widthPx(context) / density else 0f
    }
}
