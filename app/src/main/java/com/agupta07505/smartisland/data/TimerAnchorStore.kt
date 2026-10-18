/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data

import android.content.Context
import android.content.SharedPreferences
import com.agupta07505.smartisland.alert.TimerAnchor
import com.agupta07505.smartisland.alert.TimerRecovery
import com.agupta07505.smartisland.util.runCatchingLogged

/**
 * Absolute timer / stopwatch anchors per notification key (Rule_Book §7.3), so a timer survives the process
 * being killed. SharedPreferences rather than DataStore because the listener reads it synchronously while it
 * rehydrates, before the overlay draws. Values are [TimerAnchor.encode]d ("T:<end epoch ms>").
 */
class TimerAnchorStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(key: String): TimerAnchor? =
        runCatchingLogged(TAG, "Reading timer anchor failed") { TimerAnchor.decode(prefs.getString(key, null)) }

    /** Saves [anchor] for [key] unless it is (within a second) the anchor already saved. */
    fun put(key: String, anchor: TimerAnchor) {
        runCatchingLogged(TAG, "Saving timer anchor failed") {
            if (TimerRecovery.shouldPersist(get(key), anchor)) prefs.edit().putString(key, anchor.encode()).apply()
        }
    }

    fun remove(key: String) {
        runCatchingLogged(TAG, "Removing timer anchor failed") {
            if (prefs.contains(key)) prefs.edit().remove(key).apply()
        }
    }

    /** Drops anchors for timers that are no longer in the shade. */
    fun retainOnly(liveKeys: Set<String>) {
        runCatchingLogged(TAG, "Pruning timer anchors failed") {
            val stale = prefs.all.keys.filterNot { it in liveKeys }
            if (stale.isNotEmpty()) prefs.edit().apply { stale.forEach { remove(it) } }.apply()
        }
    }

    private companion object {
        const val TAG = "TimerAnchorStore"
        const val PREFS_NAME = "smart_island_timer_anchors"
    }
}
