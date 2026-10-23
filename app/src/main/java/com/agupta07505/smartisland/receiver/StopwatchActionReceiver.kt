/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.util.InAppStopwatch

/** Receives the Lap / Pause / Resume / Reset buttons of the island's own stopwatch card. */
class StopwatchActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        InAppStopwatch.handle(context, intent.action)
    }
}
