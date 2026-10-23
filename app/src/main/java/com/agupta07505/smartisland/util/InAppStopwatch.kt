/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.StopwatchState
import com.agupta07505.smartisland.receiver.StopwatchActionReceiver

/**
 * A stopwatch the island runs itself, shown as the normal Stopwatch card. Its buttons are broadcasts to
 * [StopwatchActionReceiver].
 */
object InAppStopwatch {
    const val KEY = "smartisland.stopwatch"

    const val ACTION_LAP = "com.agupta07505.smartisland.stopwatch.LAP"
    const val ACTION_TOGGLE = "com.agupta07505.smartisland.stopwatch.TOGGLE"
    const val ACTION_RESET = "com.agupta07505.smartisland.stopwatch.RESET"
    const val ACTION_NOOP = "com.agupta07505.smartisland.stopwatch.NOOP"

    private const val PREFS = "in_app_stopwatch"
    private const val PREF_STATE = "state"

    // The labels stay English on purpose: the expanded card tells Lap, Pause, Resume and Reset apart by these words.
    private const val LABEL_LAP = "Lap"
    private const val LABEL_PAUSE = "Pause"
    private const val LABEL_RESUME = "Resume"
    private const val LABEL_RESET = "Reset"

    @Volatile var state: StopwatchState = StopwatchState()
        private set

    /** True while a stopwatch exists (running or paused): the Stopwatch tile lights up. */
    val isActive: Boolean get() = state.active

    /** Safe to call repeatedly. */
    fun restore(context: Context) {
        val app = context.applicationContext
        val saved = StopwatchState.decode(prefs(app).getString(PREF_STATE, null))
        if (!saved.active) return
        state = saved
        post(app)
    }

    fun toggle(context: Context) = change(context) { it.toggle(System.currentTimeMillis()) }

    fun lap(context: Context) = change(context) { it.lap() }

    fun reset(context: Context) {
        val app = context.applicationContext
        state = StopwatchState()
        save(app)
        runCatching { SmartIslandRepositories.notificationRepository(app).removeNotification(KEY) }
    }

    fun handle(context: Context, action: String?) {
        when (action) {
            ACTION_LAP -> lap(context)
            ACTION_TOGGLE -> toggle(context)
            ACTION_RESET -> reset(context)
            else -> Unit
        }
    }

    private fun change(context: Context, transform: (StopwatchState) -> StopwatchState) {
        val app = context.applicationContext
        val next = transform(state)
        if (next == state) return
        state = next
        save(app)
        post(app)
    }

    private fun post(app: Context) {
        val s = state
        if (!s.active) return
        val now = System.currentTimeMillis()
        val elapsed = s.elapsedMs(now)
        val actions = buildList {
            if (s.running) add(IslandNotificationAction(LABEL_LAP, broadcast(app, ACTION_LAP)))
            add(IslandNotificationAction(if (s.running) LABEL_PAUSE else LABEL_RESUME, broadcast(app, ACTION_TOGGLE)))
            add(IslandNotificationAction(LABEL_RESET, broadcast(app, ACTION_RESET)))
        }
        val card = IslandNotification(
            key = KEY,
            packageName = app.packageName,
            appName = "Stopwatch",
            title = "Stopwatch",
            text = if (s.laps > 0) "Lap ${s.laps}" else "Running",
            timeMillis = now - elapsed,
            actionIntents = actions,
            // A tap on the card does nothing; without this it would open this app.
            contentIntent = broadcast(app, ACTION_NOOP),
            mode = IslandMode.Stopwatch,
            recordingFrozenMs = if (s.running) null else elapsed,
            recordingPaused = !s.running
        )
        runCatching { SmartIslandRepositories.notificationRepository(app).postNotification(card, autoExpand = false) }
    }

    private fun broadcast(app: Context, action: String): PendingIntent {
        val intent = Intent(app, StopwatchActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(app, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun save(app: Context) {
        prefs(app).edit().putString(PREF_STATE, state.encode()).apply()
    }

    private fun prefs(app: Context) = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
