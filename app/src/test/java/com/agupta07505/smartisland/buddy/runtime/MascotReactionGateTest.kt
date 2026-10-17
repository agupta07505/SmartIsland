/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotReactionGateTest {
    @Test
    fun firstReactionIsAlwaysAdmitted() {
        assertTrue(MascotReactionGate().admits(MascotReaction.Love, 0.0))
    }

    @Test
    fun nothingComesWithinTheSpacingOfTheLast() {
        val gate = MascotReactionGate()
        assertTrue(gate.admits(MascotReaction.Love, 100.0))
        assertFalse(gate.admits(MascotReaction.Celebrate, 102.4))
        assertTrue(gate.admits(MascotReaction.Celebrate, 102.5))
    }

    @Test
    fun theSameReactionWaitsThirtySeconds() {
        val gate = MascotReactionGate()
        assertTrue(gate.admits(MascotReaction.Love, 0.0))
        assertFalse(gate.admits(MascotReaction.Love, 29.9))
        assertTrue(gate.admits(MascotReaction.Love, 30.0))
    }

    @Test
    fun aRefusedReactionDoesNotResetTheClock() {
        val gate = MascotReactionGate()
        assertTrue(gate.admits(MascotReaction.Love, 0.0))
        assertFalse(gate.admits(MascotReaction.Love, 10.0))
        // Thirty seconds after the one that played, not after the refused one.
        assertTrue(gate.admits(MascotReaction.Love, 30.0))
    }

    @Test
    fun aDifferentReactionIsFineOnceSpacingHasPassed() {
        val gate = MascotReactionGate()
        assertTrue(gate.admits(MascotReaction.Love, 0.0))
        assertTrue(gate.admits(MascotReaction.Yawn, 3.0))
    }

    @Test
    fun resetForgetsHistory() {
        val gate = MascotReactionGate()
        assertTrue(gate.admits(MascotReaction.Love, 0.0))
        gate.reset()
        assertTrue(gate.admits(MascotReaction.Love, 0.1))
    }
}
