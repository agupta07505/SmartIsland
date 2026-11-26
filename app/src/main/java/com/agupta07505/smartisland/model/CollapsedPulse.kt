/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

// Pulse queue for the collapsed pill. An event that doesn't auto-expand holds the cutout for a while,
// anything arriving meanwhile waits its turn. Pure state machine: callers pass the time in, nothing
// here reads a clock.
object CollapsedPulse {
    const val QUEUE_MAX = 6

    enum class Kind {
        Call,
        Live,
        Quick,
        Notification
    }

    /** [id] identifies the card; a repost has the same id. */
    data class Entry(
        val id: String,
        val kind: Kind,
        val rank: Int,
        val holdMs: Long,
        val app: String? = null,
        val pair: String? = null,
        val minHoldMs: Long = holdMs,
        val crowdedMinHoldMs: Long = holdMs
    )

    /** [frontSinceMs]: set while this is next up; the stale timeout counts from there. */
    data class Waiting(
        val entry: Entry,
        val arrivedAtMs: Long,
        val resumed: Boolean = false,
        val frontSinceMs: Long? = null
    )

    /**
     * [queue] stays in arrival order, [upNext] is play order. [settled] = ids that already had their
     * pulse (or lost it) and are still on the island, so a repost doesn't pulse again.
     */
    data class State(
        val pulsing: Entry? = null,
        val pulseEndsAtMs: Long = 0L,
        val pulsingGone: Boolean = false,
        val queue: List<Waiting> = emptyList(),
        val gapEndsAtMs: Long? = null,
        val settled: Set<String> = emptySet(),
        val lastPulsedId: String? = null,
        val heldSinceMs: Long? = null,
        val pulseStartedAtMs: Long = 0L,
        val morphedFromId: String? = null
    ) {
        val inGap: Boolean get() = gapEndsAtMs != null

        val busy: Boolean get() = pulsing != null || inGap

        val upNext: List<Waiting> get() = order(queue)
    }

    fun entryOf(card: IslandNotification, holdPercent: Int = 100): Entry {
        val event = IslandEvent.forNotification(card)
        val kind = when {
            card.mode == IslandMode.IncomingCall && card.isCallRinging -> Kind.Call
            card.mode == IslandMode.Notification -> Kind.Notification
            IslandPriority.isLive(card.mode) -> Kind.Live
            else -> Kind.Quick
        }
        // battery/connection cards share 1 key, so the event has to tell them apart
        val shared = card.mode == IslandMode.Battery || card.mode == IslandMode.Bluetooth
        val (baseHold, baseMin, baseCrowded) = holds(kind, event, card)
        // calls ring until answered, the speed setting must not shorten that
        val hold = if (kind == Kind.Call) baseHold else scaled(baseHold, holdPercent)
        val minHold = if (kind == Kind.Call) baseMin else scaled(baseMin, holdPercent)
        val crowdedMinHold = if (kind == Kind.Call) baseCrowded else scaled(baseCrowded, holdPercent)
        return Entry(
            id = if (shared && event != null) "${card.key}|${event.id}" else card.key,
            kind = kind,
            rank = IslandPriority.pulseRank(card),
            holdMs = hold,
            app = if (kind == Kind.Notification) card.packageName else null,
            pair = event?.let { pairOf(it) },
            minHoldMs = minHold,
            crowdedMinHoldMs = crowdedMinHold
        )
    }

    fun drawsSettled(card: IslandNotification?, pulses: Boolean): Boolean =
        !pulses || card == null || entryOf(card).kind != Kind.Quick

    private fun textFloorMs(floorMs: Long, holdMs: Long, chars: Int): Long =
        readHoldMs(minOf(floorMs, holdMs), holdMs, chars)

    fun gapMs(waiting: Int): Long {
        val cut = (waiting - 1).coerceAtLeast(0) * IslandTimings.PULSE_GAP_STEP_MS
        return (IslandTimings.PULSE_GAP_MS - cut).coerceAtLeast(IslandTimings.PULSE_GAP_MIN_MS)
    }

    fun scaled(ms: Long, percent: Int): Long = if (percent == 100) ms else ms * percent / 100

    /** Base hold plus reading time for text past the free allowance, clamped to [floorMs]..[capMs]. */
    fun readHoldMs(floorMs: Long, capMs: Long, chars: Int): Long {
        val extra = IslandTimings.READ_MS_PER_CHAR * (chars - IslandTimings.READ_FREE_CHARS).coerceAtLeast(0)
        return (floorMs + extra).coerceIn(floorMs, capMs)
    }

    fun isRepost(s: State, id: String): Boolean =
        s.pulsing?.id == id || id in s.settled || s.queue.any { it.entry.id == id }

    fun blocksExpand(s: State): Boolean = s.busy

    fun nextDeadlineMs(s: State): Long? =
        if (s.pulsing != null) s.pulseEndsAtMs.takeIf { s.heldSinceMs == null } else s.gapEndsAtMs

    /** Finger down on the pulsing pill; the pulse won't end until [release]. */
    fun hold(s: State, nowMs: Long): State =
        if (s.pulsing == null || s.heldSinceMs != null) s else s.copy(heldSinceMs = nowMs)

    fun release(s: State, nowMs: Long): State {
        val since = s.heldSinceMs ?: return s
        return s.copy(pulseEndsAtMs = s.pulseEndsAtMs + (nowMs - since).coerceAtLeast(0L), heldSinceMs = null)
    }

    fun arrive(s: State, entry: Entry, nowMs: Long): State = when {
        isRepost(s, entry.id) -> s
        entry.kind == Kind.Call && s.pulsing?.kind != Kind.Call -> preempt(s, entry, nowMs)
        morphs(s, entry) -> morph(s, entry, nowMs)
        !s.busy -> start(s, entry, nowMs)
        else -> enqueue(s, entry, nowMs)
    }

    /**
     * If an opposite event arrives, like Wi-Fi disconnecting during a connect pulse,
     * the current pulse continues with the new glyph and text. No separate pulse is queued.
     */
    fun morphs(s: State, entry: Entry): Boolean {
        val running = s.pulsing ?: return false
        return running.kind == Kind.Quick && entry.kind == Kind.Quick &&
            entry.pair != null && entry.pair == running.pair && entry.id != running.id
    }

    // Running pulse becomes [entry]: holds for the new event's time from now, no shorter than what was
    // left, and capped from the pulse start so a flapping connection can't hog the cutout forever.
    // The first event's card stays on the island without pulsing again.
    private fun morph(s: State, entry: Entry, nowMs: Long): State {
        val old = s.pulsing ?: return s
        val swapped = s.copy(
            pulsing = entry,
            pulsingGone = false,
            morphedFromId = old.id,
            settled = s.settled + old.id,
            queue = s.queue.filterNot { replaces(entry, it.entry) }
        )
        return extendMorph(swapped, nowMs + playMs(entry, swapped.queue.size), nowMs)
    }

    // Pushes a morphed pulse's end out to untilMs within the flap cap, but always leaves MORPH_MIN_MS
    // from now (until the hard end) so the final state can actually be read.
    private fun extendMorph(s: State, untilMs: Long, nowMs: Long): State {
        if (s.pulsing == null || s.heldSinceMs != null) return s
        val capMs = minOf(IslandTimings.MORPH_CAP_MS, extendCapMs(s.queue.size))
        val capped = untilMs.coerceAtMost(s.pulseStartedAtMs + capMs)
        val hardEnd = s.pulseStartedAtMs + IslandTimings.MORPH_CAP_MS + IslandTimings.MORPH_MIN_MS
        val end = maxOf(s.pulseEndsAtMs, maxOf(capped, nowMs + IslandTimings.MORPH_MIN_MS).coerceAtMost(hardEnd))
        return if (end > s.pulseEndsAtMs) s.copy(pulseEndsAtMs = end) else s
    }

    fun refresh(s: State, entry: Entry, nowMs: Long): State = when {
        s.pulsing?.id == entry.id -> extend(s.copy(pulsing = entry), nowMs + playMs(entry, s.queue.size))
        s.queue.any { it.entry.id == entry.id } -> s.copy(
            queue = s.queue.map { if (it.entry.id == entry.id) it.copy(entry = entry, arrivedAtMs = nowMs, frontSinceMs = if (it.frontSinceMs != null) nowMs else null) else it }
        )
        else -> s
    }

    /**
     * Cards whose states share 1 id (airplane on/off, DND level, mode). Flipping one is a new event,
     * not a repost: a running pulse takes the new state ([refresh]), a settled card pulses again.
     */
    fun flipsInPlace(card: IslandNotification): Boolean {
        if (card.mode != IslandMode.Bluetooth) return false
        val event = IslandEvent.forNotification(card)
        return event == IslandEvent.AIRPLANE_MODE || event == IslandEvent.DND || event == IslandEvent.MODE
    }

    fun ensureHold(s: State, needMs: Long, nowMs: Long): State = extend(s, nowMs + needMs)

    fun extendCapMs(waiting: Int): Long = when {
        waiting <= 0 -> IslandTimings.PULSE_EXTEND_CAP_MS
        waiting == 1 -> IslandTimings.PULSE_EXTEND_CAP_ONE_WAITING_MS
        else -> 0L
    }

    /** Pushes the end out to [untilMs]: never earlier, never past the cap, not while held. */
    private fun extend(s: State, untilMs: Long): State {
        if (s.pulsing == null || s.heldSinceMs != null) return s
        val end = untilMs.coerceAtMost(s.pulseStartedAtMs + extendCapMs(s.queue.size))
        return if (end > s.pulseEndsAtMs) s.copy(pulseEndsAtMs = end) else s
    }

    fun endPulse(s: State, nowMs: Long): State {
        val done = s.pulsing ?: return s
        val base = dropStale(
            s.copy(
                pulsing = null,
                pulsingGone = false,
                morphedFromId = null,
                heldSinceMs = null,
                settled = if (s.pulsingGone) s.settled else s.settled + done.id,
                lastPulsedId = if (s.pulsingGone) null else done.id
            ),
            nowMs
        )
        return base.copy(gapEndsAtMs = if (base.queue.isEmpty()) null else nowMs + gapMs(base.queue.size))
    }

    fun endGap(s: State, nowMs: Long): State {
        if (!s.inGap) return s
        val base = dropStale(s, nowMs)
        val next = order(base.queue).firstOrNull() ?: return base.copy(gapEndsAtMs = null)
        return stampFront(start(base.copy(queue = base.queue - next), next.entry, nowMs), nowMs)
    }

    fun advance(s: State, nowMs: Long): State {
        var cur = s
        while (true) {
            val gapEnd = cur.gapEndsAtMs
            cur = when {
                cur.pulsing != null && cur.heldSinceMs == null && nowMs >= cur.pulseEndsAtMs ->
                    endPulse(cur, cur.pulseEndsAtMs)
                gapEnd != null && nowMs >= gapEnd -> endGap(cur, gapEnd)
                else -> return cur
            }
        }
    }

    fun cardsChanged(s: State, alive: Set<String>, nowMs: Long): State {
        val pulsing = s.pulsing
        var next = s.copy(pulsingGone = pulsing != null && pulsing.id !in alive)
        if (next.pulsingGone && pulsing?.kind == Kind.Call) next = endPulse(next, nowMs)
        next = next.copy(
            queue = next.queue.filter { it.entry.id in alive },
            settled = next.settled.filterTo(HashSet()) { it in alive },
            lastPulsedId = next.lastPulsedId?.takeIf { it in alive }
        )
        return if (next.inGap && next.queue.isEmpty()) next.copy(gapEndsAtMs = null) else next
    }

    fun skip(s: State, id: String, nowMs: Long): State {
        val next = if (s.pulsing?.id == id) {
            endPulse(s, nowMs).let { it.copy(settled = it.settled + id) }
        } else {
            s.copy(queue = s.queue.filterNot { it.entry.id == id }, settled = s.settled + id)
        }
        return if (next.inGap && next.queue.isEmpty()) next.copy(gapEndsAtMs = null) else next
    }

    fun blocksExpandFor(s: State, id: String): Boolean =
        s.pulsing?.let { it.id != id } == true || s.queue.any { it.entry.id != id }

    const val EXPAND_TAKEOVER_MIN_RANK = 20

    // An auto-expanding card may open the island over other pulses unless one of them is urgent.
    // Music or a waiting notification shouldn't hold back a message that opens by itself; they stay as
    // cards. Urgent pulses still play out first.
    fun expandMayTakeOver(s: State, id: String): Boolean =
        (listOfNotNull(s.pulsing) + s.queue.map { it.entry }).none { it.id != id && it.rank < EXPAND_TAKEOVER_MIN_RANK }

    fun settleAll(s: State): State =
        State(settled = s.settled + listOfNotNull(s.pulsing?.id) + s.queue.map { it.entry.id })

    fun tapTarget(s: State): String? = s.pulsing?.id ?: s.lastPulsedId.takeIf { s.inGap }

    data class TapClaim(val id: String?, val state: State)

    fun claimByTap(s: State): TapClaim = TapClaim(tapTarget(s), settleAll(s))

    fun rearm(s: State, id: String): State = if (id in s.settled) s.copy(settled = s.settled - id) else s

    fun order(queue: List<Waiting>): List<Waiting> {
        val front = queue.filter { it.resumed }
        val sorted = queue.filterNot { it.resumed }.sortedBy { it.entry.rank }  // sortedBy is stable so equal ranks keep arrival order
        val newest = sorted.filter { it.entry.kind == Kind.Notification }
            .sortedByDescending { it.arrivedAtMs }
            .iterator()
        return front + sorted.map { if (it.entry.kind == Kind.Notification) newest.next() else it }
    }

    private fun playMs(entry: Entry, waiting: Int): Long {
        if (waiting <= 0 || entry.kind == Kind.Call) return entry.holdMs
        val shortened = entry.holdMs * IslandTimings.WAITING_HOLD_PERCENT / 100
        val single = maxOf(entry.minHoldMs, shortened).coerceAtMost(entry.holdMs)
        if (waiting == 1) return single
        val cut = (waiting - 1) * IslandTimings.CROWDED_HOLD_STEP_PERCENT
        val percent = (IslandTimings.WAITING_HOLD_PERCENT - cut).coerceAtLeast(IslandTimings.CROWDED_HOLD_MIN_PERCENT)
        return maxOf(entry.crowdedMinHoldMs, entry.holdMs * percent / 100).coerceAtMost(single)
    }

    private fun start(s: State, entry: Entry, nowMs: Long): State = s.copy(
        pulsing = entry,
        pulseEndsAtMs = nowMs + playMs(entry, s.queue.size),
        pulseStartedAtMs = nowMs,
        pulsingGone = false,
        morphedFromId = null,
        heldSinceMs = null,
        gapEndsAtMs = null
    )

    private fun preempt(s: State, call: Entry, nowMs: Long): State {
        val cut = s.pulsing
        val base = when {
            cut == null || s.pulsingGone -> s
            cut.kind == Kind.Live -> s.copy(queue = listOf(Waiting(cut, nowMs, resumed = true)) + s.queue)
            else -> s.copy(settled = s.settled + cut.id)
        }
        return stampFront(start(base, call, nowMs), nowMs)
    }

    private fun enqueue(s: State, entry: Entry, nowMs: Long): State {
        val replaced = s.queue.filter { replaces(entry, it.entry) }
        var queue = s.queue - replaced.toSet() + Waiting(entry, nowMs)
        var settled = s.settled + replaced.map { it.entry.id }
        while (overBudget(queue)) {
            val waiting = queue.filterNot { it.resumed }
            val lowest = waiting.maxOf { it.entry.rank }
            val loser = waiting.first { it.entry.rank == lowest }
            queue = queue - loser
            settled = settled + loser.entry.id
        }
        return stampFront(s.copy(queue = queue, settled = settled), nowMs)
    }

    fun queuedPlayMs(queue: List<Waiting>): Long =
        queue.filterNot { it.resumed }.sumOf {
            minOf(it.entry.crowdedMinHoldMs, IslandTimings.QUEUE_ENTRY_MAX_MS) + IslandTimings.PULSE_GAP_MIN_MS
        }

    private fun overBudget(queue: List<Waiting>): Boolean {
        val count = queue.count { !it.resumed }
        return count > QUEUE_MAX || (count > 1 && queuedPlayMs(queue) > IslandTimings.QUEUE_BUDGET_MS)
    }

    private fun replaces(newer: Entry, older: Entry): Boolean =
        (newer.kind == Kind.Notification && older.kind == Kind.Notification && newer.app == older.app) ||
            (newer.pair != null && newer.pair == older.pair)

    private fun dropStale(s: State, nowMs: Long): State {
        val stale = s.queue.filter {
            val front = it.frontSinceMs
            it.entry.kind == Kind.Quick && front != null && nowMs - front >= IslandTimings.QUICK_PULSE_STALE_MS
        }
        val kept = if (stale.isEmpty()) s else {
            s.copy(queue = s.queue - stale.toSet(), settled = s.settled + stale.map { it.entry.id })
        }
        return stampFront(kept, nowMs)
    }

    private fun stampFront(s: State, nowMs: Long): State {
        val head = order(s.queue).firstOrNull() ?: return s
        val queue = s.queue.map {
            when {
                it === head -> if (it.frontSinceMs == null) it.copy(frontSinceMs = nowMs) else it
                it.frontSinceMs != null -> it.copy(frontSinceMs = null)
                else -> it
            }
        }
        return if (queue == s.queue) s else s.copy(queue = queue)
    }

    private data class Holds(val hold: Long, val minHold: Long, val crowdedMinHold: Long)

    private fun holds(kind: Kind, event: IslandEvent?, card: IslandNotification): Holds {
        val chars = PulsePeak.shownChars(card)
        return when {
            kind == Kind.Notification -> {
                val floor = IslandTimings.NOTIFICATION_PULSE_MIN_MS
                val hold = readHoldMs(floor, IslandTimings.NOTIFICATION_PULSE_MAX_MS, chars)
                Holds(hold, floor, textFloorMs(IslandTimings.NOTIFICATION_CROWDED_MIN_MS, hold, chars))
            }
            kind == Kind.Live && PulsePeak.hasCustomTitle(card) -> {
                val floor = IslandTimings.LIVE_PULSE_MS
                val hold = readHoldMs(floor, IslandTimings.LIVE_PULSE_MAX_MS, chars)
                Holds(hold, floor, textFloorMs(IslandTimings.LIVE_CROWDED_MIN_MS, hold, chars))
            }
            kind == Kind.Quick -> {
                val hold = holdMs(kind, event)
                Holds(
                    hold,
                    textFloorMs(IslandTimings.QUICK_WAITING_MIN_MS, hold, chars),
                    textFloorMs(IslandTimings.QUICK_CROWDED_MIN_MS, hold, chars)
                )
            }
            kind == Kind.Live -> {
                val hold = holdMs(kind, event)
                Holds(hold, hold, textFloorMs(IslandTimings.LIVE_CROWDED_MIN_MS, hold, chars))
            }
            else -> holdMs(kind, event).let { Holds(it, it, it) }
        }
    }

    private fun holdMs(kind: Kind, event: IslandEvent?): Long = when (kind) {
        Kind.Call, Kind.Live -> IslandTimings.LIVE_PULSE_MS
        Kind.Notification -> IslandTimings.NOTIFICATION_PULSE_MIN_MS
        Kind.Quick -> when (event) {
            IslandEvent.PLUGGED_IN -> IslandTimings.PLUGGED_IN_CARD_MS
            IslandEvent.PLUGGED_OUT -> IslandTimings.UNPLUGGED_CARD_MS
            IslandEvent.CALL_ENDED -> IslandTimings.CALL_ENDED_CARD_MS
            IslandEvent.FULLY_CHARGED,
            IslandEvent.LOW_BATTERY,
            IslandEvent.BATTERY_SAVER -> IslandTimings.BATTERY_WARNING_CARD_MS
            IslandEvent.WIFI_DISCONNECTED,
            IslandEvent.VPN_DISCONNECTED,
            IslandEvent.USB_DISCONNECTED,
            IslandEvent.HEADPHONES_DISCONNECTED -> IslandTimings.DISCONNECT_CARD_MS
            else -> IslandTimings.CONNECTIVITY_CARD_MS
        }
    }

    private fun pairOf(event: IslandEvent): String? = when (event) {
        IslandEvent.PLUGGED_IN, IslandEvent.PLUGGED_OUT -> "power"
        IslandEvent.WIFI, IslandEvent.WIFI_DISCONNECTED -> "wifi"
        IslandEvent.VPN, IslandEvent.VPN_DISCONNECTED -> "vpn"
        IslandEvent.USB_CONNECTED, IslandEvent.USB_DISCONNECTED -> "usb"
        IslandEvent.HEADPHONES_CONNECTED, IslandEvent.HEADPHONES_DISCONNECTED -> "headphones"
        IslandEvent.RING, IslandEvent.VIBRATE, IslandEvent.SILENT -> "ringer"
        IslandEvent.DND, IslandEvent.MODE -> "mode"
        else -> null
    }
}
