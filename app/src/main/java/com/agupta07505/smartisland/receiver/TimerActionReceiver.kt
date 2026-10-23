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
import com.agupta07505.smartisland.util.InAppTimer

/** Receives the Pause / Resume / +1 min / Stop buttons of the island's own timer card, and its end-of-countdown alarm. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        InAppTimer.handle(context, intent.action)
    }
}
