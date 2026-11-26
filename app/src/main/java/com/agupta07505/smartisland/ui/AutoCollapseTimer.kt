/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.AutoCollapsePause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The countdown that collapses an expanded island, with the 3 flags that hold it ([paused] while the expanded text
 * is read, [held] by a touch, [pinned] by the white pill). What else blocks the countdown is the view model's to say,
 * through [blocked]; [onExpire] runs when it ends unblocked
 */
internal class AutoCollapseTimer(
    private val scope: CoroutineScope,
    private val wallClockMs: () -> Long,
    private val blocked: () -> Boolean,
    private val onExpire: () -> Unit
) {
    val paused = MutableStateFlow(false)
    val held = MutableStateFlow(false)
    val pinned = MutableStateFlow(false)

    private var job: Job? = null
    private var leftMs = DELAY_MS
    private var deadlineMs = 0L

    val running: Boolean get() = job?.isActive == true

    /** What is left of the countdown when it resumes, the full delay when it was nearly over. */
    fun resumeDelayMs(): Long = AutoCollapsePause.resumeDelayMs(leftMs, DELAY_MS)

    /** The next countdown starts over from the full delay. */
    fun resetLeft() {
        leftMs = DELAY_MS
    }

    fun start(delayMs: Long = DELAY_MS) {
        job?.cancel()
        if (blocked()) return
        leftMs = delayMs
        deadlineMs = wallClockMs() + delayMs
        job = scope.launch {
            delay(delayMs)
            // Checked again against the live state
            if (blocked()) return@launch
            onExpire()
        }
    }

    fun stop() {
        if (job?.isActive == true) {
            leftMs = (deadlineMs - wallClockMs()).coerceAtLeast(0L)
        }
        job?.cancel()
        job = null
    }

    companion object {
        const val DELAY_MS = 5000L
    }
}
