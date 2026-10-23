/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Fills the overlay window. While expanded, a tap anywhere outside the island closes things in order: an open
 * picture, then an open sibling picker, then the island itself.
 */
internal fun Modifier.islandOutsideTap(
    expanded: Boolean,
    cb: IslandCallbacks,
    isInputActive: Boolean,
    onReplyStateChanged: (Boolean) -> Unit
): Modifier = if (expanded) {
    fillMaxSize()
        // Keyed on the reply state: with Unit the handler kept the value from the moment the island opened, so a tap
        // outside while a reply box was open never saw it (and never cleared it).
        .pointerInput(isInputActive) {
            detectTapGestures {
                // An open picture closes first, then an open sibling picker; the next outside tap
                // collapses as usual.
                if (cb.currentMediaExpansion.isOpen) {
                    cb.currentMediaExpansion.onChange(null, 0)
                    return@detectTapGestures
                }
                if (cb.currentSiblingPickerOpen) {
                    cb.currentOnSiblingPickerClose()
                    return@detectTapGestures
                }
                if (isInputActive) {
                    onReplyStateChanged(false)
                }
                cb.currentOnToggle()
            }
        }
} else {
    fillMaxSize()
}
