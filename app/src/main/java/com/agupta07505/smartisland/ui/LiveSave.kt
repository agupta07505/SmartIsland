/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.agupta07505.smartisland.util.AppLogRecorder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

/** Live saving while a slider is dragged. */
@Composable
fun <T> rememberLiveSaver(intervalMs: Long = LIVE_SAVE_INTERVAL_MS, write: suspend (T) -> Unit): (T) -> Unit {
    val queue = remember { Channel<T>(Channel.CONFLATED) }
    val currentWrite = rememberUpdatedState(write)
    LaunchedEffect(queue) {
        for (value in queue) {
            try {
                currentWrite.value(value)
            } catch (e: Exception) {
                AppLogRecorder.e("LiveSave", "Live setting write failed", e)
            }
            delay(intervalMs)
        }
    }
    return remember(queue) { { value: T -> queue.trySend(value); Unit } }
}

const val LIVE_SAVE_INTERVAL_MS = 50L
