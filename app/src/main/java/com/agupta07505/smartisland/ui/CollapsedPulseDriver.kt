/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Glue between the view model and the CollapsedPulse state machine: feeds it cards, fires its
// transitions on time and holds the state. Main thread only. nowMs is the clock every time in the
// model comes from, injected so tests can fake it.
internal class CollapsedPulseDriver(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long,
    // pulse speed setting in percent, read fresh whenever an entry is built
    private val holdPercent: () -> Int = { 100 }
) {
    private val _state = MutableStateFlow(CollapsedPulse.State())
    val state: StateFlow<CollapsedPulse.State> = _state

    private var timer: Job? = null
    private var scheduledFor: Long? = null

    // content hash from when each notification last got its turn; a different one means a new message
    private val contents = HashMap<String, Int>()

    // timer cards already seen finished, so only the flip to finished pulses again
    private val finished = HashSet<String>()

    /**
     * Cards changed. [visible] = on the island, [raw] = everything still posted (cards the island only
     * hides temporarily keep their settled mark). Fresh cards pulse if [canPulse], else get settled.
     * The card the open island is showing ([openId]) never pulses over itself.
     */
    fun cardsChanged(
        visible: List<IslandNotification>,
        raw: List<IslandNotification>,
        canPulse: Boolean,
        openId: String? = null,
        mayPulse: (IslandNotification) -> Boolean = { true },
        /** Card about to auto-expand: neither pulses nor settles until that's decided. */
        hold: (IslandNotification) -> Boolean = { false }
    ) {
        val now = nowMs()
        val shown = visible.map { CollapsedPulse.entryOf(it, holdPercent()) to it }
        val rawIds = raw.mapTo(HashSet<String>()) { CollapsedPulse.entryOf(it).id }
        contents.keys.retainAll(rawIds)
        finished.retainAll(rawIds)
        update(now) { s ->
            // settled marks stay for cards that are merely hidden, the rest go with their card
            var next = CollapsedPulse.cardsChanged(s, shown.mapTo(HashSet<String>()) { it.first.id } +
                s.settled.filter { it in rawIds }, now)
            for ((entry, card) in shown) {
                if (hold(card) && !CollapsedPulse.isRepost(next, entry.id)) continue
                val content = card.contentToken()
                val before = contents.put(entry.id, content)
                val changed = before != null && before != content
                // quiet cards settle on arrival; only a brand-new key counts as arriving
                val quiet = before == null && card.quietArrival && !card.timerFinished
                // Changed content pulses again for notifications and music (new track). Airplane / DND / mode
                // cards count too: when they flip, it's the opposite event under the same id.
                val refreshes = changed && (entry.kind == CollapsedPulse.Kind.Notification ||
                    card.mode == IslandMode.Music || CollapsedPulse.flipsInPlace(card))
                if (refreshes) next = CollapsedPulse.rearm(next, entry.id)
                // a timer running out pulses again regardless of the app's own switch
                val becameFinished = card.timerFinished && finished.add(entry.id) && before != null
                if (!card.timerFinished) finished.remove(entry.id)
                if (becameFinished) next = CollapsedPulse.rearm(next, entry.id)
                next = if (canPulse && entry.id != openId && !quiet && mayPulse(card)) {
                    // new text on a card that's pulsing/waiting extends its pulse instead of being ignored
                    if (refreshes && CollapsedPulse.isRepost(next, entry.id)) CollapsedPulse.refresh(next, entry, now)
                    else CollapsedPulse.arrive(next, entry, now)
                } else {
                    if (CollapsedPulse.isRepost(next, entry.id)) next else CollapsedPulse.skip(next, entry.id, now)
                }
            }
            next
        }
    }

    /**
     * [card] wants to open the island. Ringing calls always get it and take everything else down with
     * them. Anything else gets it unless an urgent event is pulsing/waiting; lower-priority pulses
     * give way and settle (their cards stay). On false the card pulses instead.
     */
    fun claimExpand(card: IslandNotification): Boolean {
        val now = nowMs()
        val entry = CollapsedPulse.entryOf(card, holdPercent())
        var granted = false
        update(now) { s ->
            when {
                entry.kind == CollapsedPulse.Kind.Call -> {
                    granted = true
                    CollapsedPulse.skip(CollapsedPulse.settleAll(s), entry.id, now)
                }
                // urgent pulse (navigation, low battery, finished timer) plays out first, then this card pulses
                CollapsedPulse.blocksExpandFor(s, entry.id) && !CollapsedPulse.expandMayTakeOver(s, entry.id) -> s
                else -> {
                    granted = true
                    // everything else gives way: island opens on this card, theirs stay as cards
                    val clear = if (CollapsedPulse.blocksExpandFor(s, entry.id)) CollapsedPulse.settleAll(s) else s
                    CollapsedPulse.skip(clear, entry.id, now)
                }
            }
        }
        return granted
    }

    /** Card a pill tap would open; null if none pulsed and the caller should pick. */
    fun tapTarget(): String? = CollapsedPulse.tapTarget(CollapsedPulse.advance(_state.value, nowMs()))

    /** Tap on the pulsing pill: returns the card to open, settles everything else. */
    fun claimExpandByTap(): String? {
        var id: String? = null
        update(nowMs()) { s ->
            val claim = CollapsedPulse.claimByTap(s)
            id = claim.id
            claim.state
        }
        return id
    }

    /**
     * User left an app, so its live [cards] are back on the island and get another pulse. Same as
     * [cardsChanged]: only pulses if [canPulse], otherwise they stay re-armed for next time.
     */
    fun repulse(cards: List<IslandNotification>, canPulse: Boolean, openId: String? = null) {
        if (cards.isEmpty()) return
        val now = nowMs()
        update(now) { s ->
            var next = s
            for (card in cards) {
                val entry = CollapsedPulse.entryOf(card, holdPercent())
                next = CollapsedPulse.rearm(next, entry.id)
                if (canPulse && entry.id != openId) next = CollapsedPulse.arrive(next, entry, now)
            }
            next
        }
    }

    fun pause() {
        val now = nowMs()
        update(now) { CollapsedPulse.hold(it, now) }
    }

    fun resume() {
        val now = nowMs()
        update(now) { CollapsedPulse.release(it, now) }
    }

    /** Text needs [ms] more to scroll; hold the pulse that long, within the cap. */
    fun scrollNeed(ms: Long) {
        val now = nowMs()
        update(now) { CollapsedPulse.ensureHold(it, ms, now) }
    }

    /** Ends the running pulse and everything queued (cards stay, no pulses). Returns true if something was running, i.e. the gesture was consumed. */
    fun settleRunning(): Boolean {
        var settled = false
        update(nowMs()) { s ->
            if (s.pulsing == null) s else {
                settled = true
                CollapsedPulse.settleAll(s)
            }
        }
        return settled
    }

    /** Clear everything, e.g. on rotation to landscape. */
    fun settleAll() = update(nowMs()) { CollapsedPulse.settleAll(it) }

    private fun update(now: Long, transform: (CollapsedPulse.State) -> CollapsedPulse.State) {
        _state.value = transform(CollapsedPulse.advance(_state.value, now))
        val due = CollapsedPulse.nextDeadlineMs(_state.value)
        if (due == scheduledFor) return
        timer?.cancel()
        scheduledFor = due
        timer = due?.let {
            scope.launch {
                delay((it - nowMs()).coerceAtLeast(0L))
                scheduledFor = null
                // Treat the deadline as reached even if nowMs() disagrees. A lagging clock (or a frozen one in
                // unit tests) would see the deadline still ahead and reschedule the same timer forever.
                update(maxOf(nowMs(), it)) { s -> s }
            }
        }
    }

    private fun IslandNotification.contentToken() =
        if (mode == IslandMode.Music) Pair(title, text).hashCode() else Triple(timeMillis, title, text).hashCode()
}
