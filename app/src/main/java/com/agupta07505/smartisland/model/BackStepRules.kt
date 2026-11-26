/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

enum class BackStep { None, CloseReply, ClearSelection, CloseSiblingPicker, CloseLauncher, CardBack, Collapse }

object BackStepRules {
    fun next(
        expanded: Boolean,
        inputActive: Boolean,
        selecting: Boolean,
        pickerOpen: Boolean,
        launcherOpen: Boolean,
        cardDepth: CardDepth
    ): BackStep = when {
        !expanded -> BackStep.None
        selecting -> BackStep.ClearSelection
        // Back closes the reply box before it closes the notification card.
        inputActive -> BackStep.CloseReply
        pickerOpen -> BackStep.CloseSiblingPicker
        launcherOpen -> BackStep.CloseLauncher
        cardDepth != CardDepth.First -> BackStep.CardBack
        else -> BackStep.Collapse
    }

    /**
     * When a card opens automatically, the app underneath keeps focus. Back can still go to its previous screen.
     */
    fun windowFocusable(inputActive: Boolean, expanded: Boolean, passiveOpen: Boolean = false): Boolean =
        inputActive || (expanded && !passiveOpen)

    fun windowTakesKeyboard(inputActive: Boolean): Boolean = inputActive
}
