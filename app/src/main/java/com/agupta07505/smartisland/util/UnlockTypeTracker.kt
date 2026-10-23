/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UnlockRecord(val type: UnlockType, val atMillis: Long)

object UnlockTypeTracker {
    private val _last = MutableStateFlow<UnlockRecord?>(null)
    val last: StateFlow<UnlockRecord?> = _last.asStateFlow()

    fun record(type: UnlockType, nowMillis: Long): Boolean {
        val prev = _last.value
        if (prev != null && prev.type == type && nowMillis - prev.atMillis in 0..FaceUnlockLog.SAME_UNLOCK_MS) return false
        _last.value = UnlockRecord(type, nowMillis)
        return true
    }
}
