/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.model.LabAction
import com.agupta07505.smartisland.model.LabCard
import com.agupta07505.smartisland.model.LabOverride
import com.agupta07505.smartisland.model.LabStage
import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.model.labStepsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the lab asks the island's ViewModel to do (the ViewModel lives in the overlay service). */
enum class LabCommand { EXPAND, COLLAPSE, OPEN_PICKER, CLOSE_PICKER, TAP_IDLE_DOT }

data class LabProgress(
    val running: Boolean = false,
    val waitingForLandscape: Boolean = false,
    val stageIndex: Int = 0,
    val stageCount: Int = 0,
    val stage: LabStage? = null,
    val stepTitle: String = "",
    val finished: Boolean = false
)

/**
 * Plays the scripted landscape situations (model/LandscapeDemoPlan.kt) on the real island.
 *
 * Nothing is stored: the lab only sets [override], which the ViewModel and the overlay service apply on top of the
 * stored settings while it is set. The cards are ordinary demo cards (keys start with [DemoEvents.KEY_PREFIX]).
 * It plays only in landscape; started in portrait it waits until the phone is turned.
 */
object LandscapeDemo {
    private const val KEY = DemoEvents.KEY_PREFIX + "lab_"
    private const val NOOP_ACTION = "com.agupta07505.smartisland.action.DEMO_NOOP"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val _override = MutableStateFlow<LabOverride?>(null)

    val override: StateFlow<LabOverride?> = _override.asStateFlow()

    private val _commands = MutableSharedFlow<LabCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<LabCommand> = _commands.asSharedFlow()

    private val _progress = MutableStateFlow(LabProgress())
    val progress: StateFlow<LabProgress> = _progress.asStateFlow()

    /** Kept up to date by the ViewModel from the real display state. */
    val deviceLandscape = MutableStateFlow(false)

    fun applyTo(raw: SmartIslandSettings): SmartIslandSettings = _override.value?.applyTo(raw) ?: raw

    fun applyTo(state: OrientationState): OrientationState = _override.value?.applyTo(state) ?: state

    fun send(command: LabCommand) {
        _commands.tryEmit(command)
    }

    fun start(context: Context, repo: INotificationRepository, stages: List<LabStage>, quick: Boolean) {
        stop(repo)
        if (stages.isEmpty()) return
        val appContext = context.applicationContext
        job = scope.launch {
            val steps = labStepsFor(quick)
            try {
                _progress.value = LabProgress(running = true, waitingForLandscape = !deviceLandscape.value, stageCount = stages.size)
                if (!deviceLandscape.value) deviceLandscape.first { it }
                for ((index, stage) in stages.withIndex()) {
                    _override.value = LabOverride(stage)
                    // The island needs a moment to take the new layout before the first card.
                    DemoEvents.clearAll(repo)
                    delay(700)
                    for (step in steps) {
                        // Turned back to portrait: wait for landscape again, the script resumes from this step.
                        if (!deviceLandscape.value) {
                            _progress.value = _progress.value.copy(waitingForLandscape = true)
                            deviceLandscape.first { it }
                            delay(700)
                        }
                        _progress.value = LabProgress(
                            running = true, stageIndex = index, stageCount = stages.size,
                            stage = stage, stepTitle = step.title
                        )
                        for (action in step.actions) perform(appContext, repo, action)
                        delay(step.holdMs)
                    }
                }
                _progress.value = LabProgress(finished = true, stageCount = stages.size)
            } finally {
                DemoEvents.clearAll(repo)
                send(LabCommand.COLLAPSE)
                _override.value = null
                if (_progress.value.running) _progress.value = LabProgress()
            }
        }
    }

    fun stop(repo: INotificationRepository?) {
        job?.cancel()
        job = null
        _override.value = null
        _progress.value = LabProgress()
        repo?.let { DemoEvents.clearAll(it) }
    }

    private suspend fun perform(context: Context, repo: INotificationRepository, action: LabAction) {
        when (action) {
            is LabAction.Post -> repo.postNotification(build(context, action.card), autoExpand = action.autoExpand)
            is LabAction.Remove -> repo.removeNotification(keyFor(action.card))
            LabAction.RemoveAll -> DemoEvents.clearAll(repo)
            LabAction.Expand -> send(LabCommand.EXPAND)
            LabAction.Collapse -> send(LabCommand.COLLAPSE)
            LabAction.OpenPicker -> send(LabCommand.OPEN_PICKER)
            LabAction.ClosePicker -> send(LabCommand.CLOSE_PICKER)
            LabAction.TapIdleDot -> send(LabCommand.TAP_IDLE_DOT)
            is LabAction.SetMerge -> _override.value = _override.value?.copy(mergeStyle = action.style)
            is LabAction.Wait -> delay(action.ms)
        }
    }

    internal fun keyFor(card: LabCard): String = KEY + card.name.lowercase()

    private fun noop(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(NOOP_ACTION).setPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun build(context: Context, card: LabCard): IslandNotification {
        val now = System.currentTimeMillis()
        val key = keyFor(card)
        val pi = noop(context)
        fun button(title: String) = IslandNotificationAction(title = title, pendingIntent = pi)
        fun chat(title: String, text: String) = IslandNotification(
            key = key, packageName = "com.demo.chat", appName = "Chat", title = title, text = text,
            mode = IslandMode.Notification, timeMillis = now
        )
        return when (card) {
            LabCard.MUSIC -> IslandNotification(
                key = key, packageName = "com.spotify.music", appName = "Music",
                title = "Midnight City", text = "M83",
                actionIntents = listOf(button("Previous"), button("Pause"), button("Next")),
                mediaPositionMs = 92_000L, mediaDurationMs = 243_000L, mediaIsPlaying = true,
                mode = IslandMode.Music, timeMillis = now
            )
            LabCard.MESSAGE -> IslandNotification(
                key = key, packageName = "com.google.android.apps.messaging", appName = "Messages",
                title = "Sam", text = "Are we still on for tonight?",
                bigText = "Are we still on for tonight? I booked the table for 8.",
                mode = IslandMode.Notification, timeMillis = now
            )
            LabCard.DOWNLOAD -> IslandNotification(
                key = key, packageName = "com.android.chrome", appName = "Chrome",
                title = "holiday-video.mp4", text = "62% • 4.2 MB/s", progress = 62, progressMax = 100,
                mode = IslandMode.DownloadUpload, transferKind = TransferKind.DOWNLOAD,
                transferDetail = "4.2 MB/s • 3 min left", timeMillis = now
            )
            LabCard.SIBLING_A -> chat("Ana", "Lunch tomorrow?")
            LabCard.SIBLING_B -> chat("Ben", "Sent you the file")
            LabCard.SIBLING_C -> chat("Cleo", "Call me when you can")
            LabCard.CALL -> IslandNotification(
                key = key, packageName = "com.google.android.dialer", appName = "Phone",
                title = "Alex Morgan", text = "Incoming call",
                actionIntents = listOf(button("Decline"), button("Answer")),
                mode = IslandMode.IncomingCall, timeMillis = now
            )
            LabCard.PLUG -> IslandNotification(
                key = key, packageName = "com.android.systemui", appName = "System",
                title = "Plugged in", text = "78%", category = "battery_plugged_in",
                mode = IslandMode.Battery, timeMillis = now
            )
        }
    }
}
