/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide marker for state recovery (Rule_Book §7.3): when the notification listener last finished
 * pulling the OS's active notifications into the repository. The overlay reads it to snap, rather than
 * spring, to the bounds of alerts restored after a (re)start (see `RestoreSnap`).
 */
object IslandRestoreState {
    private val _syncedAtMs = MutableStateFlow<Long?>(null)

    /** Wall-clock time of the last initial sync, or null when none has finished in this process. */
    val syncedAtMs: StateFlow<Long?> = _syncedAtMs.asStateFlow()

    fun markSynced(nowMs: Long = System.currentTimeMillis()) {
        _syncedAtMs.value = nowMs
    }
}
