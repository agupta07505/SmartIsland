/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Which app the usage stats say is in front, built from the activity events (no Android types, so it can be unit
 * tested)
 */
class UsageForegroundTracker {
    /** App in front per the events so far, null when unknown (nothing resumed, or it paused). */
    var current: String? = null
        private set

    /** Timestamp of the newest event seen, so the next query starts after it. 0 when none yet. */
    var lastEventTime: Long = 0L
        private set

    /** [ignored] packages (this app, SystemUI, keyboards) never count and never end another app's turn. */
    fun onEvent(type: Int, packageName: String?, time: Long, ignored: (String) -> Boolean) {
        if (time > lastEventTime) lastEventTime = time
        if (packageName.isNullOrEmpty() || ignored(packageName)) return
        when (type) {
            RESUMED -> current = packageName
            PAUSED, STOPPED -> if (current == packageName) current = null
        }
    }

    companion object {
        /** UsageEvents.Event.ACTIVITY_RESUMED (MOVE_TO_FOREGROUND before Android 10) */
        const val RESUMED = 1

        /** UsageEvents.Event.ACTIVITY_PAUSED (MOVE_TO_BACKGROUND before Android 10) */
        const val PAUSED = 2

        /** UsageEvents.Event.ACTIVITY_STOPPED */
        const val STOPPED = 23
    }
}

/** Reads the usage events (Usage access) into a [UsageForegroundTracker]. Without the permission it knows nothing. */
class UsageForegroundSource(
    private val context: Context,
    private val tracker: UsageForegroundTracker = UsageForegroundTracker()
) {
    private var lastQueryMs = 0L

    /**
     * App in front per the usage stats, null when unknown. Queries are rate limited, the answer is cached between them
     */
    fun current(ignored: (String) -> Boolean): String? {
        val now = System.currentTimeMillis()
        if (now - lastQueryMs >= MIN_QUERY_INTERVAL_MS) {
            lastQueryMs = now
            runCatchingLogged("UsageForegroundTracker", "Usage stats query failed") { query(now, ignored) }
        }
        return tracker.current
    }

    private fun query(now: Long, ignored: (String) -> Boolean) {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val begin = if (tracker.lastEventTime == 0L) now - FIRST_LOOKBACK_MS else tracker.lastEventTime + 1
        val events = manager.queryEvents(begin, now) ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            tracker.onEvent(event.eventType, event.packageName, event.timeStamp, ignored)
        }
    }

    private companion object {
        const val MIN_QUERY_INTERVAL_MS = 300L
        const val FIRST_LOOKBACK_MS = 2L * 60L * 60L * 1000L
    }
}
