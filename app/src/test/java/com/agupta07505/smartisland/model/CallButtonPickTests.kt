/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallButtonPickTests {
    private fun pick(role: CallActionRole, vararg titles: String) = CallButtonPick.pick(titles.toList(), role)

    @Test
    fun threeButtonsWithVoicemailExtra() {
        val t = arrayOf("Decline", "Answer with voicemail", "Answer")
        assertEquals(0, pick(CallActionRole.HangUp, *t))
        assertEquals(2, pick(CallActionRole.Answer, *t))
    }

    @Test
    fun declineWithMessageDoesNotBeatPlainDecline() {
        val t = arrayOf("Decline with message", "Decline", "Answer")
        assertEquals(1, pick(CallActionRole.HangUp, *t))
        assertEquals(2, pick(CallActionRole.Answer, *t))
    }

    @Test
    fun twoAnswerLikeButtonsStillGiveOneAnswer() {
        val t = arrayOf("Decline", "Answer video call", "Answer")
        assertEquals(2, pick(CallActionRole.Answer, *t))
    }

    @Test
    fun onlyDeclineWithMessageIsStillUsedAsLastResort() {
        assertEquals(0, pick(CallActionRole.HangUp, "Decline with message", "Answer"))
    }

    @Test
    fun missingRoleGivesNull() {
        assertNull(pick(CallActionRole.Answer, "Decline", "Reply"))
        assertNull(pick(CallActionRole.Mute, "Mute"))
    }

    @Test
    fun localizedPlainTitles() {
        assertEquals(1, pick(CallActionRole.HangUp, "Rechazar con mensaje", "挂断"))
    }
}
