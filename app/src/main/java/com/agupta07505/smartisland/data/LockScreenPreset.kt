/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.model.IslandEvent

/** A named combination of lock-screen switches, null is a custom combination. */
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
            IslandEvent.entries.filter { it.canHideOnLockScreen && it != IslandEvent.NOTIFICATION }.map { it.id }
                .toSet()

        /** Preset the switches add up to, or null for a custom mix. */
        fun from(s: SmartIslandSettings): LockScreenPreset? = when {
            !s.showOnLockScreen -> Off
            s.hideAllOnLockScreen -> CallsOnly
            s.hideOnLockScreenEvents.isEmpty() -> ShowAll
            s.hideOnLockScreenEvents == hideableExceptNotifications() -> MessagesOnly
            else -> null
        }
    }
}
