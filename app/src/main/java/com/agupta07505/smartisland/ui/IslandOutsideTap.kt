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
import com.agupta07505.smartisland.model.AutoCollapsePause

internal fun Modifier.islandOutsideTap(
    expanded: Boolean,
    wiring: IslandWiring,
    isInputActive: Boolean,
    held: Boolean,
    onReplyStateChanged: (Boolean) -> Unit
): Modifier = if (expanded) {
    fillMaxSize()
        // Keyed on the reply state: with Unit the handler kept the value from when the island opened, so a tap outside
        // with a reply box open never saw it (and never cleared it)
        .pointerInput(isInputActive, held) {
            detectTapGestures {
                // A selected text goes first, whatever else the tap would have done
                if (wiring.view.textSelecting) {
                    wiring.actions.card.clearTextSelection()
                    return@detectTapGestures
                }
                // An open picture closes first, then an open sibling picker, the next outside tap collapses as usual
                if (wiring.view.mediaExpansion.isOpen) {
                    wiring.view.mediaExpansion.onChange(null, 0)
                    return@detectTapGestures
                }
                if (wiring.view.siblingPickerOpen) {
                    wiring.actions.sibling.closePicker()
                    return@detectTapGestures
                }
                if (!AutoCollapsePause.outsideTapCloses(held, isInputActive)) return@detectTapGestures
                if (isInputActive) {
                    onReplyStateChanged(false)
                }
                wiring.actions.expand.toggle()
            }
        }
} else {
    fillMaxSize()
}
