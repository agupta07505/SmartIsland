/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

object IslandTimings {
    const val PLUGGED_IN_CARD_MS = 2_000L

    const val UNPLUGGED_CARD_MS = 2_200L

    const val BATTERY_WARNING_CARD_MS = 4_000L

    const val CONNECTIVITY_CARD_MS = 2_500L

    const val DISCONNECT_CARD_MS = 3_200L

    /** Finished transfer (download, backup, copy, ...) lingers this long. */
    const val TRANSFER_DONE_MS = 2_500L

    const val TRANSFER_DONE_IMPORTED_MS = 60_000L

    const val CALL_ENDED_CARD_MS = 2_500L

    /** Failed transfer lingers a little longer so the failure can be read. */
    const val TRANSFER_FAILED_MS = 5_000L

    const val RECORDING_DONE_MS = 0L

    const val LIVE_PULSE_MS = 3_600L

    const val NOTIFICATION_PULSE_MIN_MS = 3_000L

    const val NOTIFICATION_PULSE_MAX_MS = 6_000L

    const val LIVE_PULSE_MAX_MS = 5_000L

    const val READ_MS_PER_CHAR = 65L

    const val READ_FREE_CHARS = 30

    const val WAITING_HOLD_PERCENT = 70L

    const val CROWDED_HOLD_STEP_PERCENT = 15L

    const val CROWDED_HOLD_MIN_PERCENT = 40L

    const val QUICK_WAITING_MIN_MS = 1_900L

    const val QUICK_CROWDED_MIN_MS = 1_400L

    const val LIVE_CROWDED_MIN_MS = 2_600L

    const val NOTIFICATION_CROWDED_MIN_MS = 2_200L

    const val PULSE_GAP_MS = 450L

    const val PULSE_GAP_STEP_MS = 100L

    const val PULSE_GAP_MIN_MS = 300L

    /**
     * A quick event loses its pulse when it has been next in line this long without getting its turn (a finger held
     * the pulse, say). Time spent behind other pulses does not count
     */
    const val QUICK_PULSE_STALE_MS = 11_000L

    /** A running pulse with nothing waiting never lasts past this from its start because of a refresh or scrolling text. */
    const val PULSE_EXTEND_CAP_MS = 10_000L

    const val PULSE_EXTEND_CAP_ONE_WAITING_MS = 6_000L

    const val QUEUE_BUDGET_MS = 10_000L

    const val QUEUE_ENTRY_MAX_MS = 3_000L

    /** An opposite event that takes over a pulse holds at least this long, so the last state is always readable. */
    const val MORPH_MIN_MS = 1_500L

    const val MORPH_CAP_MS = 6_000L

    const val STATIC_IDLE_TIMEOUT_MS = 5_000L

    /** While collapsed, the secondary cutout merges into the primary after this long without interaction. */
    const val SECONDARY_IDLE_MS = 1_800L
}
