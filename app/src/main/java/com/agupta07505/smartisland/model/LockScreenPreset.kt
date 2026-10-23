/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.SmartIslandSettings

/**
 * One choice that sets the lock-screen switches together. Not stored: [from] reads it back from the switches; null (a
 * custom mix) selects no button.
 * - [ShowAll]: the island shows every event on the lock screen.
 * - [MessagesOnly]: only app notifications (calls are never hidden).
 * - [CallsOnly]: no event but calls; the idle cutout stays.
 * - [Off]: the island is hidden on the lock screen, calls included.
 */
enum class LockScreenPreset { ShowAll, MessagesOnly, CallsOnly, Off;

    /** Settings this preset stands for, built on [base]; every other setting is kept. */
    fun applyTo(base: SmartIslandSettings): SmartIslandSettings = when (this) {
        ShowAll -> base.copy(showOnLockScreen = true, hideAllOnLockScreen = false, hideOnLockScreenEvents = emptySet())
        MessagesOnly -> base.copy(
            showOnLockScreen = true,
            hideAllOnLockScreen = false,
            hideOnLockScreenEvents = hideableExceptNotifications()
        )
        CallsOnly -> base.copy(showOnLockScreen = true, hideAllOnLockScreen = true, hideOnLockScreenEvents = emptySet())
        Off -> base.copy(showOnLockScreen = false)
    }

    companion object {
        private fun hideableExceptNotifications(): Set<String> =
            IslandEvent.entries.filter { it.canHideOnLockScreen && it != IslandEvent.NOTIFICATION }.map { it.id }.toSet()

        /** The preset the switches add up to, or null for a custom mix. */
        fun from(s: SmartIslandSettings): LockScreenPreset? = when {
            !s.showOnLockScreen -> Off
            s.hideAllOnLockScreen -> CallsOnly
            s.hideOnLockScreenEvents.isEmpty() -> ShowAll
            s.hideOnLockScreenEvents == hideableExceptNotifications() -> MessagesOnly
            else -> null
        }
    }
}
