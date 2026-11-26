/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object AutoCollapsePause {
    const val RESUME_BONUS_MS = 1800L

    /** Countdown resumes where it stopped, a little longer, never past the full time. */
    fun resumeDelayMs(leftMs: Long, fullMs: Long): Long = (leftMs + RESUME_BONUS_MS).coerceIn(0L, fullMs)

    fun outsideTouchPassesThrough(autoExpanded: Boolean, held: Boolean): Boolean = autoExpanded || held

    fun outsideTapCloses(held: Boolean, inputActive: Boolean): Boolean = !held || inputActive
}

/**
 * The pill's auto-hide countdown does not run while an event pulses (in every layout), and in landscape classic also
 * not while a finger is working the cutout. It starts again only after [SETTLE_MS] of quiet, so the pill never tucks
 * itself away under a pulse or a gesture
 */
object AutoHideHold {
    const val SETTLE_MS = 1200L

    fun applies(isLandscape: Boolean, notchMode: Boolean, edgeStyle: Boolean): Boolean =
        isLandscape && !notchMode && !edgeStyle

    fun holds(applies: Boolean, pulseBusy: Boolean, gestureActive: Boolean): Boolean =
        pulseBusy || (applies && gestureActive)

    fun settleLeftMs(heldEndedMs: Long, nowMs: Long): Long =
        (heldEndedMs + SETTLE_MS - nowMs).coerceIn(0L, SETTLE_MS)
}

/**
 * Keeps a quick card (battery, connections) on the island while its pulse still needs it. The monitors remove the card
 * on their own timer, which starts when it is posted: behind a long pulse that is before its turn. The card leaves when
 * its pulse is over, or [BUBBLE_WAIT_MS] later when a live event sits behind it. All times are passed in
 */
object QuickCardHold {
    const val BUBBLE_WAIT_MS = 3_500L

    data class Held(val card: IslandNotification, val id: String, val releaseAtMs: Long? = null)

    data class State(
        val known: Map<String, IslandNotification> = emptyMap(),
        val pulsed: Set<String> = emptySet(),
        val held: List<Held> = emptyList()
    )

    fun cards(s: State): List<IslandNotification> = s.held.map { it.card }

    fun nextDeadlineMs(s: State): Long? = s.held.mapNotNull { it.releaseAtMs }.minOrNull()

    /**
     * [raw] is every card posted now, [pulse] the pulse state before it sees this change. [kept] holds the ids a tap
     * opened the island on, they stay while the island is open
     */
    fun update(
        s: State,
        raw: List<IslandNotification>,
        pulse: CollapsedPulse.State,
        nowMs: Long,
        kept: Set<String> = emptySet()
    ): State {
        val known = raw.associateBy { it.key }
        val liveBehind = raw.any { IslandPriority.isLive(it.mode) }
        val pulsingId = pulse.pulsing?.id
        val queued = pulse.queue.mapTo(HashSet()) { it.entry.id }
        val present = (known.values + s.known.values).mapTo(HashSet()) { CollapsedPulse.entryOf(it).id } +
            s.held.map { it.id }
        val pulsed = (s.pulsed + listOfNotNull(pulsingId)).filterTo(HashSet()) { it in present }

        // a repost is the real card again, i.e. the held copy can go
        var held = s.held.filter { it.card.key !in known }
        val held0 = held.map { it.id }.toSet()
        for (card in s.known.values) {
            if (card.key in known) continue
            val entry = CollapsedPulse.entryOf(card)
            if (entry.kind != CollapsedPulse.Kind.Quick || entry.id in held0) continue
            val waits = entry.id == pulsingId || entry.id in queued || entry.id in kept
            if (waits || (entry.id in pulsed && liveBehind)) held = held + Held(card, entry.id)
        }
        held = held.mapNotNull { h ->
            when {
                // a tap opened the island on this card, it stays until the island closes
                h.id in kept -> h.copy(releaseAtMs = null)
                h.id == pulsingId -> h.copy(releaseAtMs = null)
                h.id in queued -> h
                !liveBehind -> null
                h.releaseAtMs != null -> if (nowMs >= h.releaseAtMs) null else h
                h.id in pulsed -> h.copy(releaseAtMs = nowMs + BUBBLE_WAIT_MS)
                else -> null
            }
        }
        return State(known = known, pulsed = pulsed, held = held)
    }
}
