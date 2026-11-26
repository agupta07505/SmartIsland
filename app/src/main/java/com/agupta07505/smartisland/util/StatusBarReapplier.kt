/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StatusBarReapplier(
    private val scope: CoroutineScope,
    private val interactive: () -> Boolean,
    private val hasShizuku: () -> Boolean = { ShizukuManager.hasPermission() },
    private val hide: suspend (StatusBarPlan) -> Result<Unit> = { StatusBarHider.hide(it) },
    private val isHidden: suspend (StatusBarPlan) -> Boolean? = { StatusBarHider.isHidden(it) },
    private val restore: suspend () -> Result<Unit> = { StatusBarHider.restore() }
) {

    enum class Status { Off, Applied, ShizukuLost, Failed }

    private val _status = MutableStateFlow(Status.Off)
    val status: StateFlow<Status> = _status.asStateFlow()

    private fun setStatus(value: Status) {
        _status.value = value
        _shared.value = value
    }

    private var desired: Boolean? = null
    private var plan: StatusBarPlan = StatusBarPlan.Everything
    private var pendingRestore = false
    private var passJob: Job? = null
    private var watchJob: Job? = null

    fun onSettings(hidden: Boolean, plan: StatusBarPlan = StatusBarPlan.Everything) {
        val before = desired
        val planChanged = plan != this.plan
        this.plan = plan
        desired = hidden
        if (hidden) {
            pendingRestore = false
            if (before != true) trigger(if (before == null) "service start" else "switched on")
            else if (planChanged) trigger("elements changed")
            if (interactive()) startWatcher()
        } else {
            stopWatcher()
            passJob?.cancel()
            // Only an on to off change restores
            if (before == true) {
                setStatus(Status.Off)
                scope.launch { restoreNow("switched off") }
            }
        }
    }

    /** Applies the hide again in 4 passes (0, 350, 750 and 1400 ms). */
    fun trigger(reason: String) {
        if (desired != true) {
            if (pendingRestore) scope.launch { restoreNow("retry ($reason)") }
            return
        }
        passJob?.cancel()
        passJob = scope.launch { runPasses(reason) }
    }

    fun startWatcher() {
        if (desired != true || watchJob?.isActive == true) return
        watchJob = scope.launch {
            while (isActive && desired == true) {
                delay(WATCH_INTERVAL_MS)
                if (desired != true) break
                if (!interactive()) continue
                if (passJob?.isActive == true) continue
                check()
            }
        }
    }

    fun stopWatcher() {
        watchJob?.cancel()
        watchJob = null
    }

    private suspend fun runPasses(reason: String) {
        AppLogRecorder.i(TAG, "reapply: $reason")
        for (gap in PASS_GAPS_MS) {
            if (gap > 0) delay(gap)
            if (desired != true) return
            if (!applyOnce()) {
                // No point in the remaining passes without Shizuku
                if (_status.value == Status.ShizukuLost) return
            }
        }
    }

    private suspend fun check() {
        if (!hasShizuku()) {
            markLost()
            return
        }
        val needs = _status.value != Status.Applied || isHidden(plan) != true
        if (needs) {
            AppLogRecorder.i(TAG, "watcher: hide missing, applying again")
            applyOnce()
        }
    }

    private suspend fun applyOnce(): Boolean {
        if (!hasShizuku()) {
            markLost()
            return false
        }
        val result = hide(plan)
        return if (result.isSuccess) {
            setStatus(Status.Applied)
            true
        } else {
            setStatus(Status.Failed)
            false
        }
    }

    private fun markLost() {
        if (_status.value != Status.ShizukuLost) {
            AppLogRecorder.w(TAG, "Shizuku not available, the status bar hide cannot be kept until it is back")
        }
        setStatus(Status.ShizukuLost)
    }

    private suspend fun restoreNow(reason: String) {
        if (desired == true) return
        if (!hasShizuku()) {
            // Switched off while Shizuku is away: the next trigger puts the phone back
            pendingRestore = true
            AppLogRecorder.w(TAG, "restore postponed ($reason): Shizuku not available")
            return
        }
        val result = restore()
        pendingRestore = result.isFailure
        if (result.isSuccess) AppLogRecorder.i(TAG, "restored ($reason)")
    }

    companion object {
        private const val TAG = "StatusBarReapply"

        /** Pauses between the passes: they land at 0, 350, 750 and 1400 ms. */
        val PASS_GAPS_MS: LongArray = longArrayOf(0L, 350L, 400L, 650L)

        const val WATCH_INTERVAL_MS = 30_000L

        private val _shared = MutableStateFlow(Status.Off)

        val shared: StateFlow<Status> = _shared.asStateFlow()

        fun clearShared() {
            _shared.value = Status.Off
        }

        /** Waits for the Shizuku binder, which comes up late after a boot. */
        suspend fun awaitShizuku(
            timeoutMs: Long,
            pollMs: Long = 500L,
            hasShizuku: () -> Boolean = { ShizukuManager.hasPermission() }
        ): Boolean {
            var waited = 0L
            while (true) {
                if (hasShizuku()) return true
                if (waited >= timeoutMs) return false
                delay(pollMs)
                waited += pollMs
            }
        }
    }
}
