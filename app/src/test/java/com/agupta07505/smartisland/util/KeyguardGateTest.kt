package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyguardGateTest {

    @Test
    fun sensitiveActionsWaitForUnlockAndSafeOnesNeverDo() {
        listOf(GatedAction.Reply, GatedAction.MarkAsRead, GatedAction.OpenApp, GatedAction.AppAction).forEach {
            assertTrue(it.name, it.requiresUnlock(isLocked = true))
            assertFalse(it.name, it.requiresUnlock(isLocked = false))
        }
        listOf(GatedAction.MediaControl, GatedAction.TimerControl).forEach {
            assertFalse(it.name, it.requiresUnlock(isLocked = true))
        }
    }

    @Test
    fun theCachedActionRunsOnlyAfterASuccessfulDismissal() {
        val gate = PendingActionGate<String>()
        gate.defer("reply")
        assertTrue(gate.hasPending)
        assertEquals("reply", gate.onDismissSucceeded())
        // Handed out once.
        assertNull(gate.onDismissSucceeded())
    }

    @Test
    fun aCancelledUnlockDropsTheAction() {
        val gate = PendingActionGate<String>()
        gate.defer("mark read")
        gate.onDismissFailed()
        assertFalse(gate.hasPending)
        assertNull(gate.onDismissSucceeded())
    }

    @Test
    fun theNewestRequestWins() {
        val gate = PendingActionGate<String>()
        gate.defer("open app")
        gate.defer("reply")
        assertEquals("reply", gate.onDismissSucceeded())
    }

    @Test
    fun aReplyRequestIsConsumedOnceForItsKey() {
        val reply = PendingReply()
        reply.request("k1")
        assertFalse(reply.consume("other"))
        assertTrue(reply.consume("k1"))
        assertFalse(reply.consume("k1"))
        assertFalse(reply.consume(null))
    }
}
