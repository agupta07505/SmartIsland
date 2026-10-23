/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Auto-dismiss times of short island events. */
object IslandTimings {
    const val PLUGGED_IN_CARD_MS = 2_000L

    const val UNPLUGGED_CARD_MS = 2_200L

    /** Low battery, battery saver and fully charged warnings leave the island after this long. */
    const val BATTERY_WARNING_CARD_MS = 4_000L

    /** Wi-Fi and VPN cards. */
    const val CONNECTIVITY_CARD_MS = 2_500L

    /** Wi-Fi and VPN "disconnected" cards: a little longer than the connected ones, since they are the ones to notice. */
    const val DISCONNECT_CARD_MS = 3_200L

    /** A finished transfer (download, backup, copy, ...) lingers this long. */
    const val TRANSFER_DONE_MS = 2_500L

    /** How long a finished download pulled in from the shade for the same-app cutout stays on the island. */
    const val TRANSFER_DONE_IMPORTED_MS = 60_000L

    /** A failed transfer lingers a little longer so the failure can be read. */
    const val TRANSFER_FAILED_MS = 5_000L

    /** A finished screen/audio recording leaves at once. */
    const val RECORDING_DONE_MS = 0L

    /**
     * A short event (notification, battery, connection card) holds the primary cutout this long when a higher-
     * priority card is live, then the priority order applies again.
     */
    const val POPUP_MS = 1_800L

    /** When only ordinary notification cards are left, the island falls back to the idle cutout after this long. */
    const val STATIC_IDLE_TIMEOUT_MS = 5_000L

    /** While collapsed, the secondary round cutout merges into the primary one after this long without interaction. */
    const val SECONDARY_IDLE_MS = 1_800L
}
