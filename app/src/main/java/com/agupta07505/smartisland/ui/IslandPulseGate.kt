/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.collapsedPulses
import com.agupta07505.smartisland.model.CollapsedPulse
import com.agupta07505.smartisland.model.ExemptMode
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.TorchCard
import com.agupta07505.smartisland.util.FaceIdController
import com.agupta07505.smartisland.util.ForegroundApp
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Defers arrival pulses while an exempt app or the reply box has focus. */
internal class IslandPulseGate(
    private val scope: CoroutineScope,
    private val feed: IslandCardFeed,
    private val notificationRepo: INotificationRepository,
    private val orientation: StateFlow<OrientationState>,
    private val foregroundPackage: StateFlow<String?>,
    private val expanded: StateFlow<Boolean>,
    private val isInputActive: StateFlow<Boolean>,
    private val selectedIndex: StateFlow<Int>,
    private val mode: StateFlow<IslandMode>,
    private val torchCardOpen: () -> Boolean,
    private val mayAutoExpandOnArrival: (IslandNotification, SmartIslandSettings) -> Boolean
) {
    val pulse = feed.pulse
    private val quickHold = feed.quickHold
    private val settings = feed.settings
    private val rawSettings = feed.rawSettings
    private val notifications = feed.notifications
    private val visibleUnordered = feed.visibleUnordered
    private val visibleNotifications = feed.visibleNotifications

    private fun exemptionNow(): ExemptMode = feed.exemptionNow()

    private var pulseSuppressedUntilMs = 0L

    // Settling a pulse by hand suppresses subsequent pulses briefly.
    private var settleQuietUntilMs = 0L

    private fun pulsesQuiet(): Boolean = System.currentTimeMillis() < settleQuietUntilMs

    private fun canPulseNow(): Boolean = !isInputActive.value && exemptionNow() == ExemptMode.None &&
        System.currentTimeMillis() >= pulseSuppressedUntilMs && !pulsesQuiet() && !torchCardOpen()

    // Arrival time by notification key. Used to expire deferred pulses.
    private val missedPulses = HashMap<String, Long>()

    private var pulseSeenKeys: Set<String> = emptySet()

    // Auto-expand can release a held pulse without changing the card list.
    private val pulseRecheck = MutableStateFlow(0)

    fun recheck() {
        pulseRecheck.update { it + 1 }
    }

    private fun noteMissedPulses(shown: List<IslandNotification>) {
        val now = System.currentTimeMillis()
        // an import from the shade is old news, not an arrival
        if (now < pulseSuppressedUntilMs) return
        for (card in shown) {
            if (card.key in pulseSeenKeys || card.quietArrival) continue
            // a ringing call has the island, not the pulse
            if (CollapsedPulse.entryOf(card).kind == CollapsedPulse.Kind.Call) continue
            missedPulses[card.key] = now
        }
    }

    // Replay only recent arrivals that are still visible when the gate opens.
    private fun repulseMissed() {
        if (missedPulses.isEmpty()) return
        val now = System.currentTimeMillis()
        missedPulses.entries.removeAll { now - it.value > MISSED_PULSE_MAX_AGE_MS }
        if (missedPulses.isEmpty() || !canPulseNow()) return
        val keys = missedPulses.keys.toSet()
        missedPulses.clear()
        val cards = visibleNotifications.value.filter { it.key in keys }
        if (cards.isEmpty()) return
        val openId = if (expanded.value) {
            visibleNotifications.value.getOrNull(selectedIndex.value)?.let { CollapsedPulse.entryOf(it).id }
        } else null
        pulse.repulse(cards, canPulse = true, openId = openId)
    }

    /** Returns once no Face ID scan runs (at most [FACE_ID_WAIT_MS], a stuck scan must not hold events back). */
    suspend fun awaitFaceIdIdle() {
        if (!FaceIdController.state.value.active) return
        withTimeoutOrNull(FACE_ID_WAIT_MS) { FaceIdController.state.first { !it.active } }
    }

    fun touch(held: Boolean) {
        if (held) pulse.pause() else pulse.resume()
    }

    fun scrollNeed(ms: Long) = pulse.scrollNeed(ms)

    /**
     * A tap on the pulse over an open card opens the card [id] that pulses: it stays while the island is open and
     * everything that pulses or waits settles, as for a tap on the collapsed pulse. The caller selects the card
     */
    fun cardOpened(id: String) {
        quickHold.keepWhileOpen(id)
        pulse.claimExpandByTap()
    }

    fun blocksExpand(): Boolean = CollapsedPulse.blocksExpand(pulse.state.value)

    /**
     * Where a tap or a swipe down on the pill opens the island while a pulse plays or events wait. The index
     * is the card that pulses, in the gap the one that pulsed last, else the first visible card. Null when nothing
     * blocks the expand, the island then opens on the selected card as always
     */
    fun tapIndex(): Int? {
        if (!blocksExpand()) return null
        // The bubble or companion pill already picked a card. It stays selected, even if another one is pulsing.
        takeCompanionPick()?.let { return it }
        val id = pulse.tapTarget()
        val list = visibleNotifications.value
        return list.indexOfFirst { CollapsedPulse.entryOf(it).id == id }.takeIf { it >= 0 } ?: 0
    }

    /** The card opens the island instead of pulsing, unless another event's pulse is still on. */
    fun claimExpand(card: IslandNotification): Boolean = pulse.claimExpand(card)

    /** The card a bubble or companion pill tap picked while collapsed, and when: the open that follows must keep it. */
    private var companionPickIndex = -1

    private var companionPickAtMs = 0L

    private fun takeCompanionPick(): Int? {
        val index = companionPickIndex
        val fresh = System.currentTimeMillis() - companionPickAtMs <= COMPANION_PICK_WINDOW_MS
        companionPickIndex = -1
        return index.takeIf { fresh && it in visibleNotifications.value.indices }
    }

    /**
     * A hold on the glyph, then a swipe, pulses the card on the pill again, or settles the pulse that is running.
     * The selection stays as it is.
     * It follows the same rules as the pulse after leaving an app: nothing pulses over an exempt app or typing
     */
    fun repulseCurrent() {
        // While an event pulses the same gesture settles it and every event waiting behind it, then pulses stay quiet a while
        if (pulse.settleRunning()) {
            // Briefly suppress arrivals after a manual settle.
            settleQuietUntilMs = System.currentTimeMillis() + SETTLE_QUIET_MS
            return
        }
        // Manual replay clears the suppression window.
        settleQuietUntilMs = 0L
        val card = visibleNotifications.value.getOrNull(selectedIndex.value) ?: return
        val canPulse = !isInputActive.value && exemptionNow() == ExemptMode.None &&
            System.currentTimeMillis() >= pulseSuppressedUntilMs
        pulse.repulse(listOf(card), canPulse, openId = null)
    }

    /** No event pulses until [untilMs], replacing any earlier limit. */
    fun suppressUntil(untilMs: Long) {
        pulseSuppressedUntilMs = untilMs
    }

    /** No event pulses until [untilMs], unless a later limit is already set. */
    fun suppressAtLeastUntil(untilMs: Long) {
        pulseSuppressedUntilMs = maxOf(pulseSuppressedUntilMs, untilMs)
    }

    fun noteCompanionPick(index: Int) {
        companionPickIndex = index
        companionPickAtMs = System.currentTimeMillis()
    }

    fun clearCompanionPick() {
        companionPickIndex = -1
    }

    /** The pill tap opens the island on [index]: that card is kept, everything that pulses or waits settles. False when no card is there. */
    fun commitTap(index: Int): Boolean {
        val card = visibleNotifications.value.getOrNull(index)
        if (card != null) quickHold.keepWhileOpen(CollapsedPulse.entryOf(card).id)
        pulse.claimExpandByTap()
        return card != null
    }

    /** The card pulses again on the pill, if a pulse may play now. */
    fun repulse(card: IslandNotification) {
        pulse.repulse(listOf(card), canPulseNow(), null)
    }

    /** The island closed: the cards kept for a pulse tap may go. */
    fun releaseKept() = quickHold.releaseKept()

    fun start() {
        scope.launch {
            runSuspendCatchingLogged(TAG, "Pulse collector failed") {
                combine(visibleUnordered, notifications, pulseRecheck) { shown, raw, _ -> shown to raw }
                    .collectLatest { (shown, raw) ->
                    // a Face ID scan owns the cutout: the event pulses once it is over, from the newest cards
                    awaitFaceIdIdle()
                    // nothing pulses over an exempt app or while typing, what arrives then pulses once that is over
                    val unavailable = isInputActive.value || exemptionNow() != ExemptMode.None
                    val canPulse = !unavailable && System.currentTimeMillis() >= pulseSuppressedUntilMs && !pulsesQuiet()
                    val torchOpen = torchCardOpen()
                    val openId = if (expanded.value) {
                        visibleNotifications.value.getOrNull(selectedIndex.value)?.let { CollapsedPulse.entryOf(it).id }
                    } else null
                    if (unavailable) noteMissedPulses(shown)
                    pulseSeenKeys = shown.mapTo(HashSet()) { it.key }
                    pulse.cardsChanged(
                        shown, raw, canPulse, openId,
                        mayPulse = { TorchCard.mayPulse(torchOpen, it) },
                        // a card the island is about to open on does not pulse first, only to be cut a moment later
                        hold = { notificationRepo.expectsAutoExpand(it.key) }
                    )
                    if (shown.any { notificationRepo.expectsAutoExpand(it.key) }) {
                        scope.launch {
                            delay(EXPECT_RECHECK_MS)
                            pulseRecheck.update { it + 1 }
                        }
                    }
                }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Torch card pulse collector failed") {
                // A pulse that is playing ends as the torch card opens, the events behind it stay cards
                combine(expanded, mode) { open, current -> TorchCard.open(current, open) }
                    .distinctUntilChanged()
                    .collect { open -> if (open) pulse.settleRunning() }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Pulse app-exit collector failed") {
                // Leaving an app pulses its live cards again: music always, the others only when their event does not
                // auto-expand (an auto-expanding event already announced itself by opening the island)
                var previous = foregroundPackage.value
                foregroundPackage.collect { now ->
                    val left = previous
                    previous = now
                    // an exempt app was left: what arrived while it was in front pulses now
                    repulseMissed()
                    if (left.isNullOrEmpty() || left == now) return@collect
                    val cfg = settings.value
                    val back = feed.visibleWith(now).filter {
                        it.packageName == left && ForegroundApp.isLiveMode(it.mode) &&
                            !cfg.keepsCardInForeground(it) &&
                            (it.mode == IslandMode.Music || !mayAutoExpandOnArrival(it, cfg))
                    }
                    val canPulse = !isInputActive.value && exemptionNow() == ExemptMode.None &&
                        System.currentTimeMillis() >= pulseSuppressedUntilMs && !pulsesQuiet() && !torchCardOpen()
                    val openId = if (expanded.value) {
                        visibleNotifications.value.getOrNull(selectedIndex.value)?.let { CollapsedPulse.entryOf(it).id }
                    } else null
                    pulse.repulse(back, canPulse, openId)
                }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Missed pulse collector failed") {
                // the reply box closed: what arrived while it was open pulses now
                isInputActive.collect { typing -> if (!typing) repulseMissed() }
            }
        }
        scope.launch {
            runSuspendCatchingLogged(TAG, "Pulse rotation collector failed") {
                // a rotation, or a pill moved into or out of the centre, hands the collapsed pill from 1 path to the other
                combine(rawSettings, orientation) { raw, state -> state.isLandscape to raw.collapsedPulses(state) }
                    .distinctUntilChanged()
                    .collect { pulse.settleAll() }
            }
        }
    }

    companion object {
        private const val TAG = "IslandPulseGate"

        private const val FACE_ID_WAIT_MS = 6_000L

        /** Events that arrived while nothing could pulse pulse later only if they are younger than this. */
        private const val MISSED_PULSE_MAX_AGE_MS = 60_000L

        /** A bit longer than the repository's hold on an auto-expanding card, then its pulse is looked at again. */
        private const val EXPECT_RECHECK_MS = 450L

        /** The open that follows a bubble tap comes in the same gesture, a pick older than this is stale. */
        private const val COMPANION_PICK_WINDOW_MS = 600L

        /** No event pulses this long after a pulse was settled by the hold and swipe. */
        private const val SETTLE_QUIET_MS = 5_000L
    }
}
