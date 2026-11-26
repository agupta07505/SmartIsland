/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.model.IslandEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Scripted arrivals for the collapsed pulse, played from the Demo tab. */
enum class PulseScenario(val title: String, val hint: String) {
    BURST("Burst", "Five events in a second: the queue keeps the best two behind the first"),
    CALL_OVER_MUSIC("Call over music", "Music pulses, then a ringing call cuts it and opens the island"),
    RULES("Rules", "A live event, then a message, and pairs that replace each other in the queue")
}

object PulseDemo {
    private class Step(val event: IslandEvent, val afterMs: Long, val autoExpand: Boolean = false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private fun stepsOf(scenario: PulseScenario): List<Step> = when (scenario) {
        PulseScenario.BURST -> listOf(
            Step(IslandEvent.NOTIFICATION, 0L),
            Step(IslandEvent.PLUGGED_IN, 250L),
            Step(IslandEvent.WIFI, 250L),
            Step(IslandEvent.HEADPHONES_CONNECTED, 250L),
            Step(IslandEvent.VPN, 250L)
        )
        PulseScenario.CALL_OVER_MUSIC -> listOf(
            Step(IslandEvent.MUSIC, 0L),
            Step(IslandEvent.CALL, 2_500L, autoExpand = true)
        )
        PulseScenario.RULES -> listOf(
            Step(IslandEvent.MUSIC, 0L),
            Step(IslandEvent.NOTIFICATION, 1_500L),
            Step(IslandEvent.PLUGGED_IN, 400L),
            Step(IslandEvent.PLUGGED_OUT, 400L),
            Step(IslandEvent.WIFI, 400L),
            Step(IslandEvent.WIFI_DISCONNECTED, 400L)
        )
    }

    fun play(context: Context, repo: INotificationRepository, scenario: PulseScenario) {
        stop(repo)
        val appContext = context.applicationContext
        job = scope.launch {
            for (step in stepsOf(scenario)) {
                delay(step.afterMs)
                DemoEvents.show(appContext, repo, step.event, autoExpand = step.autoExpand)
            }
        }
    }

    fun stop(repo: INotificationRepository?) {
        job?.cancel()
        job = null
        repo?.let { DemoEvents.clearAll(it) }
    }
}
