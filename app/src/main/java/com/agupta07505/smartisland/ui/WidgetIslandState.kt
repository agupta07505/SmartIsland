/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.model.widgetIslandHoldsExpand
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The idle widget island: whether it is open or shown, the closes that skip the closing animation, and the card whose
 * auto-expand waits for it to go. [exempt] says an exempt app is in front, which keeps the widget island shut
 */
internal class WidgetIslandState(
    private val exempt: () -> Boolean,
    private val elapsedMs: () -> Long
) {
    val open = MutableStateFlow(false)

    /** Counts the closes that skip the closing animation. */
    val snap = MutableStateFlow(0)

    @Volatile var snapPending = false
        private set
    private var openedAt = 0L

    val shown = MutableStateFlow(false)

    /** The card that wanted to auto-expand while the widget island held the island. */
    var deferredExpandKey: String? = null

    fun holds(): Boolean = widgetIslandHoldsExpand(open.value, shown.value)

    fun setOpen(open: Boolean) {
        if (open && exempt()) return
        if (open) {
            snapPending = false
            openedAt = elapsedMs()
        }
        if (this.open.value != open) this.open.value = open
    }

    /** Closes the widget island for a reason outside it. */
    fun close(snap: Boolean, graceMs: Long = 0L) {
        if (!open.value) return
        if (graceMs > 0L && elapsedMs() - openedAt < graceMs) return
        if (snap) {
            snapPending = true
            this.snap.value = this.snap.value + 1
        }
        open.value = false
    }
}
