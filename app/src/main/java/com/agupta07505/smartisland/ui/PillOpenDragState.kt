/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job

internal class PillOpenDragState {
    /** Shell follows [progress] instead of its springs. */
    var active by mutableStateOf(false)

    /** 0 = the pill, 1 = the open card. */
    var progress by mutableFloatStateOf(0f)

    /** Island has been [PillOpenDrag.NEXT_EVENT_PEEK_AT] away from the cutout during this drag, stays until the lift. */
    var peekNext by mutableStateOf(false)

    /** Lift is closing the island (below the commit point), [progress] is springing back to 0. */
    var closing by mutableStateOf(false)

    /** Card's height as the layout knows it right now (dp). */
    var cardHeightDp: () -> Float = { 0f }

    var springsAtRest: (open: Boolean) -> Boolean = { true }

    /** Animation that finishes (or cancels) the opening after the finger lifts. */
    var settleJob: Job? = null
}
