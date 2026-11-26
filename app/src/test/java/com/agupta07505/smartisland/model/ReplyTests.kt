/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.PendingIntent
import com.agupta07505.smartisland.util.deliverReply
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class CardLinksTest {
    @Test
    fun linkNormalization() {
        assertEquals(
            listOf("https://i.airtel.in/FreeHellotune_2"),
            CardLinks.find("Download now https://i.airtel.in/FreeHellotune_2.")
        )

        assertEquals(listOf("https://en.wikipedia.org/wiki/Kotlin_(language)"),
            CardLinks.find("see https://en.wikipedia.org/wiki/Kotlin_(language)"))
        assertEquals(listOf("https://example.com/a"), CardLinks.find("(https://example.com/a)"))

        assertEquals(1, CardLinks.find("https://a.com/x and HTTPS://A.COM/X").size)

        assertEquals(emptyList<String>(), CardLinks.find("OTP 123456 is valid for 10 minutes"))
        assertEquals(emptyList<String>(), CardLinks.find("on 2026-11-22 at 10:30, Rs 1,299.00"))
        assertEquals(emptyList<String>(), CardLinks.find("card 4111 1111 1111 1111"))
        assertEquals(emptyList<String>(), CardLinks.find("order 98765432101234 shipped"))
        assertEquals(emptyList<String>(), CardLinks.find("TXN-9876543210 and #9876543210"))
        assertEquals(emptyList<String>(), CardLinks.find("v10.20.30.40 and 192.168.100.200"))

        assertEquals(emptyList<String>(), CardLinks.find("file:///sdcard/a.txt content://x/y intent://x#Intent;end"))
    }

    @Test
    fun linkActionEligibility() {
        val cases = listOf(
            "go to https://a.com/x now" to "https://a.com/x",
            "https://a.com/x and https://b.com/y" to null,
            "no link here" to null,
            "pay https://a.com/pay or see b.com" to "https://a.com/pay",
            "see a.com and b.com" to null
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, CardLinks.forCard(text, ""))
        }

        assertEquals(
            "https://a.com/pay",
            CardLinks.forCard("pay https://a.com/pay or mail help@a.com or call 1800 123 4567", "")
        )
        assertNull(CardLinks.forCard("mail help@a.com or call 1800 123 4567", ""))
    }

    @Test
    fun messageLinkSources() {
        assertEquals("https://a.com/x", CardLinks.forCard("", "see https://a.com/x"))

        assertNull(CardLinks.forCard(SENT_REPLY_LABEL + "https://a.com/x", ""))

        assertEquals("airtel.in/offer", CardLinks.forCard("visit airtel.in/offer", ""))

        assertEquals("upi://pay?pa=a", CardLinks.forCard("pay upi://pay?pa=a", ""))
        assertEquals("upi://pay?pa=a", CardLinks.normalize("upi://pay?pa=a"))

        assertEquals("help@a.com", CardLinks.forCard("mail help@a.com", ""))
        assertEquals("9876543210", CardLinks.forCard("call 9876543210", ""))
    }

    @Test
    fun normalizationSuppliesTheSchemeForEachAddressType() {
        assertEquals("https://www.a.com", CardLinks.normalize("www.a.com"))
        assertEquals("http://a.com", CardLinks.normalize("http://a.com"))
        assertEquals("https://airtel.in/offer", CardLinks.normalize("airtel.in/offer"))
        assertEquals("mailto:a@b.com", CardLinks.normalize("a@b.com"))
        assertEquals("mailto:a@b.com", CardLinks.normalize("mailto:a@b.com"))
        assertEquals("tel:+919876543210", CardLinks.normalize("+91 98765 43210"))
        assertEquals("tel:9876543210", CardLinks.normalize("98765 43210"))
        assertEquals("tel:5551234567", CardLinks.normalize("(555) 123-4567"))
    }

    @Test
    fun selectedLinkRanges() {
        assertEquals("https://b.com/y", CardLinks.inSelection("click https://b.com/y please", listOf("https://a.com")))
        assertEquals("9876543210", CardLinks.inSelection("9876543210", emptyList()))
        assertEquals("a@b.com", CardLinks.inSelection("mail a@b.com now", emptyList()))

        val partialKnown = listOf("https://i.airtel.in/FreeHellotune_2")
        assertEquals(partialKnown[0], CardLinks.inSelection("airtel.in/FreeHellotune", partialKnown))

        val outsideKnown = listOf("https://i.airtel.in/FreeHellotune_2")
        for (text in listOf("Choose your song", "ab", "hello")) {
            assertNull(text, CardLinks.inSelection(text, outsideKnown))
        }

        val longLinkKnown = listOf("https://i.airtel.in/offer/1")
        assertEquals(longLinkKnown[0], CardLinks.inSelection("airtel.in/offer", longLinkKnown))
    }

    @Test
    fun linkAndEmailDetection() {
        val cases = listOf(
            "visit airtel.in/offer" to listOf("airtel.in/offer"),
            "short link bit.ly/abc." to listOf("bit.ly/abc"),
            "see shop.example.org/a/b?x=1, thanks" to listOf("shop.example.org/a/b?x=1"),
            "go to example.com." to listOf("example.com"),
            "AIRTEL.IN" to listOf("AIRTEL.IN"),
            "example.co.in is down" to listOf("example.co.in")
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, CardLinks.find(text))
        }

        assertEquals(1, CardLinks.find("https://airtel.in/offer and airtel.in/offer").size)

        assertEquals(listOf("john.doe@example.com"), CardLinks.find("write to john.doe@example.com."))
        assertEquals(listOf("mailto:a@b.com?subject=Hi"), CardLinks.find("mailto:a@b.com?subject=Hi"))
        assertEquals(emptyList<String>(), CardLinks.find("pay to name@oksbi now"))

        assertEquals(1, CardLinks.find("support@airtel.com").size)
    }

    @Test
    fun phoneAndAppLinks() {
        assertEquals(emptyList<String>(), CardLinks.find("update to v1.2 and open file.txt, e.g. now"))
        assertEquals(emptyList<String>(), CardLinks.find("Mr.Smith/Mrs.Jones arrived, score 3.5/5"))
        assertEquals(emptyList<String>(), CardLinks.find("see path/to/readme.md and src/main.kt"))
        assertEquals(emptyList<String>(), CardLinks.find("Thanks.Visit us again, done.In a minute"))

        val cases = listOf(
            "call 9876543210 now" to listOf("9876543210"),
            "call 98765 43210." to listOf("98765 43210"),
            "call +91 98765 43210" to listOf("+91 98765 43210"),
            "dial +1 (555) 123-4567" to listOf("+1 (555) 123-4567"),
            "toll free 1800 123 4567" to listOf("1800 123 4567"),
            "office (555) 123-4567" to listOf("(555) 123-4567"),
            "tel:+919876543210" to listOf("tel:+919876543210"),
            "pay upi://pay?pa=a@b&am=10" to listOf("upi://pay?pa=a@b&am=10"),
            "open tg://resolve?domain=x." to listOf("tg://resolve?domain=x"),
            "whatsapp://send?phone=919876543210" to listOf("whatsapp://send?phone=919876543210"),
            "text sms:12345" to listOf("sms:12345")
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, CardLinks.find(text))
        }
    }


    @Test fun kindsDecideTheApp() {
        assertEquals(LinkKind.WEB, CardLinks.kind("https://a.com"))
        assertEquals(LinkKind.WEB, CardLinks.kind("www.a.com"))
        assertEquals(LinkKind.WEB, CardLinks.kind("airtel.in/offer"))
        assertEquals(LinkKind.EMAIL, CardLinks.kind("a@b.com"))
        assertEquals(LinkKind.EMAIL, CardLinks.kind("mailto:a@b.com"))
        assertEquals(LinkKind.PHONE, CardLinks.kind("98765 43210"))
        assertEquals(LinkKind.PHONE, CardLinks.kind("tel:100"))
        assertEquals(LinkKind.SMS, CardLinks.kind("sms:100"))
        assertEquals(LinkKind.APP, CardLinks.kind("upi://pay"))
        assertEquals(LinkKind.APP, CardLinks.kind("tg://resolve"))
    }


}

class OtpCodesTest {
    private fun find(text: String, title: String = "", bigText: String = "") = OtpCodes.find(title, text, bigText)

    private fun action(title: String, intent: Boolean = true, input: Boolean = false) =
        IslandNotificationAction(title, if (intent) mockk<PendingIntent>() else null, hasRemoteInput = input)

    private fun card(
        text: String,
        vararg actions: IslandNotificationAction,
        mode: IslandMode = IslandMode.Notification,
        system: IslandNotificationAction? = null
    ) = IslandNotification(
        packageName = "pkg", appName = "Messages", title = "Bank", text = text, timeMillis = 0L,
        actionIntents = actions.toList(), mode = mode, systemCopyAction = system
    )

    @Test
    fun codeSources() {
        val cases = listOf(
            "Your verification code is 483920. Valid for 10 minutes." to "483920",
            "OTP: 1234 do not share it" to "1234",
            "123456 is your verification code" to "123456"
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, find(text))
        }

        assertEquals("552211", find("552211", title = "Your login code"))
        assertEquals("552211", find("", bigText = "Your OTP is 552211"))
    }

    @Test
    fun codeFalsePositives() {
        val cases = listOf(
            "Your security code is 123 456" to "123456",
            "G-123456 is your Google verification code" to "123456",
            "OTP for txn of Rs 1500 on your card is 739201" to "739201",
            "OTP for payment of ₹1,500.00 is 739201" to "739201",
            "Your OTP request for Rs 1500 was received" to null,
            "Meet at 5, room 4821" to null,
            "Use promo code 2024 for 20% off" to null,
            "Call us on 9876543210 to verify" to null,
            "Verification pending for orders 4821 and 9917" to null,
            "${SENT_REPLY_LABEL}my OTP is 123456" to null
        )
        for ((text, expected) in cases) {
            assertEquals(text, expected, find(text))
        }
    }

    @Test fun recognisesTheAppsOwnCopyButton() {
        assertTrue(OtpCodes.isCopyAction(action("Copy code")))
        assertTrue(OtpCodes.isCopyAction(action("Copy OTP")))
        assertTrue(OtpCodes.isCopyAction(action("Copiar código")))
        assertTrue(OtpCodes.isCopyAction(action("Copy")))
        assertFalse(OtpCodes.isCopyAction(action("Copy link")))
        assertFalse(OtpCodes.isCopyAction(action("Mark as read")))
        assertFalse(OtpCodes.isCopyAction(action("Copy code", intent = false)))
        assertFalse(OtpCodes.isCopyAction(action("Copy code", input = true)))
    }

    @Test
    fun systemCopyAction() {
        val copy = action("Copy code")
        val appActionChip = OtpCodes.chipFor(card("Your OTP is 483920", action("Mark as read"), copy))
        assertSame(copy, (appActionChip as OtpChip.AppAction).action)

        val disabledChip = OtpCodes.chipFor(card("Your OTP is 483920", system = action("Copy code")), useAssistant = false)
        assertEquals("483920", (disabledChip as OtpChip.Detected).code)
    }

    @Test
    fun otpChipEligibility() {
        assertTrue(OtpCodes.chipFor(card("See the app", action("Copy code"))) is OtpChip.AppAction)

        val chip = OtpCodes.chipFor(card("Your OTP is 483920", action("Mark as read")))
        assertEquals("483920", (chip as OtpChip.Detected).code)

        assertNull(OtpCodes.chipFor(card("Just a chat message")))
        assertNull(OtpCodes.chipFor(card("Your OTP is 483920", mode = IslandMode.Timer)))

        val system = action("Copy code")
        val androidActionApp = action("Copy OTP")
        assertSame(system, (OtpCodes.chipFor(card("Your OTP is 483920", system = system)) as OtpChip.SystemAction).action)
        assertSame(androidActionApp, (OtpCodes.chipFor(card("Your OTP is 483920", androidActionApp, system = system)) as OtpChip.AppAction).action)

        assertNull(OtpCodes.chipFor(card("Your OTP is 483920", action("Copy code")), enabled = false))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReplyDeliveryTest {
    private val intent = mockk<PendingIntent>()

    private fun replyAction(pendingIntent: PendingIntent? = intent) = IslandNotificationAction(
        title = "Reply", pendingIntent = pendingIntent, remoteInputKey = "text", hasRemoteInput = true
    )

    @Test fun successfulSendReportsSuccess() {
        var sentTo: PendingIntent? = null
        assertTrue(replyAction().deliverReply { sentTo = it })
        assertEquals(intent, sentTo)
    }

    @Test fun missingActionIsNotSent() {
        var called = false
        assertFalse((null as IslandNotificationAction?).deliverReply { called = true })
        assertFalse(called)
    }

    @Test fun missingIntentIsNotSent() {
        var called = false
        assertFalse(replyAction(pendingIntent = null).deliverReply { called = true })
        assertFalse(called)
    }

    @Test fun cancelledIntentIsNotReportedAsSent() {
        assertFalse(replyAction().deliverReply { throw PendingIntent.CanceledException() })
    }

    @Test fun anyFailureWhileSendingIsNotReportedAsSent() {
        assertFalse(replyAction().deliverReply { throw SecurityException("blocked") })
    }
}

class ReplyDraftsTest {
    @Test
    fun draftIsKeptPerNotification() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.set("b", "other")
        assertEquals("hello", d.get("a"))
        assertEquals("other", d.get("b"))
        assertEquals("", d.get("c"))
    }

    @Test
    fun emptyTextRemovesTheDraft() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.set("a", "")
        assertEquals("", d.get("a"))
    }

    @Test
    fun clearEndsTheDraft() {
        val d = ReplyDrafts()
        d.set("a", "hello")
        d.clear("a")
        assertEquals("", d.get("a"))
    }

    @Test
    fun draftsOfGoneNotificationsAreForgotten() {
        val d = ReplyDrafts()
        d.set("a", "keep")
        d.set("b", "drop")
        d.retainOnly(setOf("a"))
        assertEquals("keep", d.get("a"))
        assertEquals("", d.get("b"))
    }
}

class ReplyTargetTest {
    private val pi = mockk<PendingIntent>()

    private fun action(title: String, input: Boolean, intent: PendingIntent? = pi) = IslandNotificationAction(
        title = title, pendingIntent = intent, hasRemoteInput = input
    )

    @Test fun firstActionThatTakesTextIsTheTarget() {
        val actions = listOf(action("Reply", false), action("Reply", true), action("Other", true))
        assertEquals(actions[1], actions.replyTarget())
    }

    @Test fun aPlainReplyButtonIsNoTarget() {
        assertNull(listOf(action("Reply", false), action("Mute", false)).replyTarget())
    }

    @Test fun anActionWithoutIntentIsNoTarget() {
        assertNull(listOf(action("Reply", true, intent = null)).replyTarget())
    }

    @Test fun onlyTextActionsOpenTheReplyBox() {
        assertTrue(action("Reply", true).opensReplyBox())
        assertFalse(action("Reply", false).opensReplyBox())
        assertFalse(action("Reply", true, intent = null).opensReplyBox())
        assertFalse(action("Mute", false).opensReplyBox())
    }

    @Test fun noActionsNoTarget() {
        assertNull(emptyList<IslandNotificationAction>().replyTarget())
    }
}

class SentReplyTest {
    private fun card(text: String, big: String = "") = IslandNotification(
        key = "k", packageName = "p", appName = "a", title = "Sam", text = text, timeMillis = 0L, bigText = big
    )

    @Test fun addsToShortAndLongText() {
        val c = card("Hello").withSentReply("  Hi back ")
        assertEquals("You: Hi back", c.text)
        assertEquals("Hello\nYou: Hi back", c.bigText)
    }

    @Test fun keepsExistingHistory() {
        val c = card("Two", big = "One\nTwo").withSentReply("Three")
        assertEquals("One\nTwo\nYou: Three", c.bigText)
    }

    @Test fun blankMessageChangesNothing() {
        val c = card("Hello")
        assertEquals(c, c.withSentReply("   "))
    }

    @Test fun secondReplyStacks() {
        val c = card("Hello").withSentReply("a").withSentReply("b")
        assertEquals("Hello\nYou: a\nYou: b", c.bigText)
    }

    @Test fun sendingClearsTheSuggestedChips() {
        val c = card("Hello").copy(suggestedReplies = listOf("Ok", "Thanks")).withSentReply("Ok")
        assertEquals(emptyList<String>(), c.suggestedReplies)
    }

    @Test fun repliedCardHidesItsOldSuggestionsAndTheEcho() {
        val r = RepliedCards(graceMs = 6000L)
        r.markReplied("k", nowMs = 10_000L)
        assertTrue(r.hidesSuggestions("k", postTimeMs = 9_000L))
        assertTrue(r.hidesSuggestions("k", postTimeMs = 15_000L))
    }

    @Test fun aLaterMessageOnTheSameKeyGetsSuggestionsAgain() {
        val r = RepliedCards(graceMs = 6000L)
        r.markReplied("k", nowMs = 10_000L)
        assertFalse(r.hidesSuggestions("k", postTimeMs = 16_001L))
    }

    @Test fun otherCardsAndForgottenOnesAreNotHidden() {
        val r = RepliedCards(graceMs = 6000L)
        r.markReplied("k", nowMs = 10_000L)
        assertFalse(r.hidesSuggestions("other", postTimeMs = 9_000L))
        r.forget("k")
        assertFalse(r.hidesSuggestions("k", postTimeMs = 9_000L))
    }
}

class SuggestedRepliesTest {
    @Test
    fun replyListLimits() {
        assertEquals(listOf("Sure", "On my way", "Yes"), SuggestedReplies.merge(listOf("Sure", "On my way"), listOf("Yes")))

        val merged = SuggestedReplies.merge(listOf("Ok", " ", null), listOf("ok", "OK ", "Thanks"))
        assertEquals(listOf("Ok", "Thanks"), merged)

        assertEquals(
            listOf("a", "b", "c"),
            SuggestedReplies.merge(listOf("a", "b", "c", "d"), listOf("e"))
        )

        val long = "x".repeat(SuggestedReplies.MAX_LENGTH + 1)
        assertEquals(listOf("Ok"), SuggestedReplies.merge(listOf(long, "Ok"), emptyList()))

        assertEquals(emptyList<String>(), SuggestedReplies.merge(emptyList(), emptyList()))
    }

    private fun action(hasInput: Boolean, sendable: Boolean, vararg choices: String) =
        SuggestedReplies.ActionChoices(hasInput, sendable, choices.toList())

    @Test
    fun presetReplySources() {
        val sendablePicked = SuggestedReplies.presetChoices(
            listOf(action(false, true, "No input"), action(true, true, "Yes", "No"), action(true, true, "Later"))
        )
        assertEquals(listOf<CharSequence?>("Yes", "No"), sendablePicked)

        val unavailablePicked = SuggestedReplies.presetChoices(listOf(action(true, false, "Lost"), action(true, true, "Ok")))
        assertEquals(listOf<CharSequence?>("Ok"), unavailablePicked)

        assertEquals(emptyList<CharSequence?>(), SuggestedReplies.presetChoices(listOf(action(false, true, "x"))))
        assertEquals(emptyList<CharSequence?>(), SuggestedReplies.presetChoices(emptyList()))
    }
}
