/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.util.Calendar
import kotlinx.coroutines.delay

private const val LOCAL_HOUR_POLL_MS = 60_000L

private fun currentLocalHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/** The device's local hour (0-23). */
@Composable
internal fun rememberLocalHour(track: Boolean): State<Int> =
    produceState(initialValue = currentLocalHour(), track) {
        value = currentLocalHour()
        if (!track) return@produceState
        while (true) {
            delay(LOCAL_HOUR_POLL_MS)
            value = currentLocalHour()
        }
    }
