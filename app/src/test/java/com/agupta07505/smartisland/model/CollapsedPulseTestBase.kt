/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.CollapsedPulse.Entry
import com.agupta07505.smartisland.model.CollapsedPulse.Kind
import com.agupta07505.smartisland.model.CollapsedPulse.State

abstract class CollapsedPulseTestBase {

    protected fun card(
        key: String,
        mode: IslandMode,
        category: String? = null,
        pkg: String = "pkg.$key",
        actions: List<String> = emptyList()
    ) = IslandNotification(
        key = key,
        packageName = pkg,
        appName = key,
        title = key,
        text = "",
        timeMillis = 0L,
        category = category,
        mode = mode,
        actionIntents = actions.map { IslandNotificationAction(it, null) }
    )

    protected fun entry(card: IslandNotification) = CollapsedPulse.entryOf(card)

    protected fun live(id: String, rank: Int = 20, holdMs: Long = IslandTimings.LIVE_PULSE_MS) =
        Entry(id, Kind.Live, rank, holdMs)

    protected fun quick(id: String, holdMs: Long = 2_000L, pair: String? = null) =
        Entry(id, Kind.Quick, 50, holdMs, pair = pair)

    protected fun note(id: String, app: String = id) =
        Entry(id, Kind.Notification, 50, IslandTimings.NOTIFICATION_PULSE_MIN_MS, app = app)

    protected fun call(id: String = "call") = Entry(id, Kind.Call, 0, IslandTimings.LIVE_PULSE_MS)

    protected fun State.arrive(e: Entry, at: Long) = CollapsedPulse.arrive(this, e, at)

    protected fun State.advance(at: Long) = CollapsedPulse.advance(this, at)

    protected fun State.queued() = upNext.map { it.entry.id }

    // the pulse speed setting

}
