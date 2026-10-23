/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.CountEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Notifications for the count idle widget, kept by the notification listener service and read by the widget. */
object NotificationCountTracker {
    private val state = MutableStateFlow<List<CountEntry>>(emptyList())
    val entries: StateFlow<List<CountEntry>> = state

    fun update(entries: List<CountEntry>) {
        if (state.value != entries) state.value = entries
    }

    fun clear() {
        state.value = emptyList()
    }
}
