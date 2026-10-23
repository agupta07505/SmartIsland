/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * What an open island does when the card it is showing disappears on its own (a system event ends, an app
 * cancels its notification, an event switch turns it off).
 *
 * Only a new notification may open the island by itself, so an old card from the stack must not show up expanded
 * in its place; the island closes instead.
 */
object ExpandedSelection {

    sealed interface Outcome {
        /** Nothing to do here: the card is still there, or an older rule handles it. */
        data object Keep : Outcome

        /** Close the island: the card is gone and nothing replaced it. */
        data object Collapse : Outcome

        /** The app posted a replacement for the card that went (new key, same app): show that one. */
        data class Select(val index: Int) : Outcome
    }

    private fun isReplacedUnderNewKey(card: IslandNotification): Boolean =
        card.mode == IslandMode.IncomingCall && (card.isCallRinging || card.isCallConnecting)

    /**
     * [previous] and [current] are the lists the island shows (same order as the island), [selectedIndex] is the card
     * being shown in [previous].
     */
    fun afterSelectedGone(
        previous: List<IslandNotification>,
        selectedIndex: Int,
        current: List<IslandNotification>
    ): Outcome {
        val gone = previous.getOrNull(selectedIndex) ?: return Outcome.Keep
        if (current.any { it.key == gone.key }) return Outcome.Keep
        if (isReplacedUnderNewKey(gone)) return Outcome.Keep
        // Only an ordinary notification may be replaced by the same app's new card. A live event that ended closes the island,
        // whatever the same app (or any other) posts next.
        if (gone.mode != IslandMode.Notification) return Outcome.Collapse
        val previousKeys = previous.mapTo(HashSet()) { it.key }
        val replacement = current.indexOfFirst { it.key !in previousKeys && it.packageName == gone.packageName }
        return if (replacement >= 0) Outcome.Select(replacement) else Outcome.Collapse
    }

    /**
     * An open island that shows the screen recorder closes the moment the recording ends, whatever else is waiting in
     * the list (an old notification, one that already auto-expanded, a card that is switched off must never take the
     * recorder's place).
     */
    fun recorderEnded(previous: List<IslandNotification>, selectedIndex: Int, current: List<IslandNotification>): Boolean {
        val shown = previous.getOrNull(selectedIndex) ?: return false
        if (shown.mode != IslandMode.ScreenRecording) return false
        if (current.any { it.key == shown.key && it.mode == IslandMode.ScreenRecording }) return false
        return current.none { it.mode == IslandMode.IncomingCall && (it.isCallRinging || it.isCallConnecting) }
    }
}
