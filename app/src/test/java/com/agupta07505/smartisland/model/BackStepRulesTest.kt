/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStepRulesTest {
    private fun next(
        expanded: Boolean = true,
        typing: Boolean = false,
        selecting: Boolean = false,
        picker: Boolean = false,
        launcher: Boolean = false,
        depth: CardDepth = CardDepth.First
    ) = BackStepRules.next(expanded, typing, selecting, picker, launcher, depth)

    @Test
    fun backStackOrder() {
        assertEquals(BackStep.None, next(expanded = false, selecting = true, depth = CardDepth.Reader))

        assertEquals(BackStep.CloseReply, next(typing = true))
        assertEquals(BackStep.CloseReply, next(typing = true, picker = true, depth = CardDepth.Reader))
        // a selection inside the reply box goes first, then the box
        assertEquals(BackStep.ClearSelection, next(typing = true, selecting = true))
        // once the box is shut the next Back collapses the island as before
        assertEquals(BackStep.Collapse, next(typing = false))

        assertEquals(BackStep.ClearSelection, next(selecting = true, picker = true, depth = CardDepth.Reader))

        assertEquals(BackStep.CloseSiblingPicker, next(picker = true, launcher = true, depth = CardDepth.Reader))
        assertEquals(BackStep.CloseLauncher, next(launcher = true, depth = CardDepth.Reader))
        assertEquals(BackStep.CardBack, next(depth = CardDepth.Reader))
        assertEquals(BackStep.CardBack, next(depth = CardDepth.Picture))
        assertEquals(BackStep.CardBack, next(depth = CardDepth.Preview))
        assertEquals(BackStep.Collapse, next())
    }

    @Test fun windowTakesFocusWhenOpenOrTyping() {
        assertFalse(BackStepRules.windowFocusable(inputActive = false, expanded = false))
        assertTrue(BackStepRules.windowFocusable(inputActive = false, expanded = true))
        assertTrue(BackStepRules.windowFocusable(inputActive = true, expanded = false))
        assertFalse(BackStepRules.windowFocusable(inputActive = false, expanded = true, passiveOpen = true))
        // the keyboard comes with typing alone
        assertFalse(BackStepRules.windowTakesKeyboard(inputActive = false))
        assertTrue(BackStepRules.windowTakesKeyboard(inputActive = true))
    }
}
