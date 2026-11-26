/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallEndTest {
    private fun call(
        mode: IslandMode = IslandMode.IncomingCall,
        actions: List<String> = listOf("Hang up"),
        status: String = "",
        timeMillis: Long = 1_000L
    ) = IslandNotification(
        key = "call-key", packageName = "pkg", appName = "Phone", title = "Alex", text = "Ongoing call",
        timeMillis = timeMillis, mode = mode, callStatusText = status,
        actionIntents = actions.map { IslandNotificationAction(it, null) }
    )

    private fun ended(reason: CallEndReason = CallEndReason.ByUser, durationMs: Long = 204_000L) =
        CallEnds.cardOf(call(timeMillis = 0L), reason, durationMs).copy(callEnd = CallEnd(reason, durationMs))

    @Test
    fun hangUpTimingDecidesWhoEnded() {
        assertEquals(CallEndReason.ByUser, CallEnds.reasonOf(hungUpAtMs = 10_000L, endedAtMs = 11_200L))
        assertEquals(CallEndReason.ByUser, CallEnds.reasonOf(10_000L, 10_000L + CallEnds.HANG_UP_WINDOW_MS))
        assertEquals(CallEndReason.ByOther, CallEnds.reasonOf(null, 11_200L))
        assertEquals(CallEndReason.ByOther, CallEnds.reasonOf(10_000L, 10_000L + CallEnds.HANG_UP_WINDOW_MS + 1))
        assertEquals(CallEndReason.ByOther, CallEnds.reasonOf(hungUpAtMs = 12_000L, endedAtMs = 11_000L))
    }


    @Test
    fun onlyAnAnsweredCallLeavesACardBehind() {
        assertTrue(CallEnds.canEnd(call()))
        assertFalse(CallEnds.canEnd(call(actions = listOf("Answer", "Decline"))))
        assertFalse(CallEnds.canEnd(call(status = "Calling...")))
        assertFalse(CallEnds.canEnd(call(mode = IslandMode.Notification)))
    }

    @Test
    fun endedCallIdentity() {
        val elapsedCard = CallEnds.cardOf(call(timeMillis = 5_000L), CallEndReason.ByOther, endedAtMs = 65_000L)
        assertEquals(CallEnds.CARD_KEY, elapsedCard.key)
        assertEquals(IslandMode.CallEnded, elapsedCard.mode)
        assertEquals("Alex", elapsedCard.title)
        assertEquals("pkg", elapsedCard.packageName)
        assertEquals(CallEnd(CallEndReason.ByOther, 60_000L), elapsedCard.callEnd)
        assertTrue(elapsedCard.actionIntents.isEmpty())
        assertFalse(elapsedCard.isCallRinging)
        assertFalse(elapsedCard.isCallConnecting)

        val backwardsCard = CallEnds.cardOf(call(timeMillis = 9_000L), CallEndReason.ByUser, endedAtMs = 4_000L)
        assertEquals(0L, backwardsCard.callEnd?.durationMs)
    }

    @Test
    fun lengthReadsMinutesThenHours() {
        assertEquals("00:00", CallEnds.format(0L))
        assertEquals("00:45", CallEnds.format(45_900L))
        assertEquals("03:24", CallEnds.format(204_000L))
        assertEquals("59:59", CallEnds.format(3_599_000L))
        assertEquals("1:02:09", CallEnds.format(3_729_000L))
        assertEquals("00:00", CallEnds.format(-5L))
    }

    @Test
    fun theCompactLengthKeepsTheLargestUnit() {
        assertEquals("45s", CallEnds.compact(45_000L))
        assertEquals("3m", CallEnds.compact(204_000L))
        assertEquals("1h 2m", CallEnds.compact(3_729_000L))
    }

    @Test
    fun endedCallPresentation() {
        assertEquals(IslandEvent.CALL_ENDED, IslandEvent.forNotification(ended()))
        assertEquals(IslandEvent.CALL, IslandEvent.forNotification(call()))

        assertFalse(IslandPriority.isLive(IslandMode.CallEnded))
        assertTrue(IslandPriority.rank(ended()) > IslandPriority.rank(call()))

        assertEquals("Hung up", CallEnds.pulseTitle(ended(CallEndReason.ByOther)))
        assertEquals("Call ended", CallEnds.pulseTitle(ended(CallEndReason.ByUser)))
        assertNull(CallEnds.pulseTitle(call()))

        val peak = PulsePeak.of(ended(CallEndReason.ByOther, 204_000L), nowMs = 0L)
        assertEquals("Hung up", peak.title)
        assertEquals(PulsePeak.ValueKind.Text, peak.valueKind)
        assertEquals("03:24", peak.value)
        assertNull(peak.progress)

        val entry = CollapsedPulse.entryOf(ended())
        assertEquals(CollapsedPulse.Kind.Quick, entry.kind)
        assertEquals(IslandTimings.CALL_ENDED_CARD_MS, entry.holdMs)
    }

    @Test
    fun theCardDoesNotAutoExpandByDefault() {
        assertFalse(IslandEvent.CALL_ENDED in IslandEvent.AUTO_EXPAND_BY_DEFAULT)
        assertTrue(IslandEvent.CALL_ENDED.id in IslandEvent.DEFAULT_NO_AUTO_EXPAND_IDS)
    }
}

class CardDepthTest {

    private fun walkDeeper(media: Int, text: Boolean): List<CardDepth> {
        val out = mutableListOf(CardDepth.First)
        var s = CardSteps.First
        while (true) {
            s = CardSteps.deeper(s, media, text) ?: break
            out += s.depth
        }
        return out
    }

    @Test
    fun cardDepthTraversal() {
        assertEquals(listOf(CardDepth.First, CardDepth.Reader, CardDepth.PictureMid, CardDepth.Picture), walkDeeper(1, false))
        // the arrow does the same: straight to the reader, never the preview
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps.First, 1).depth)
        assertEquals(
        listOf(CardDepth.First, CardDepth.Preview, CardDepth.Reader, CardDepth.Selector),
        walkDeeper(3, false)
        )
        assertEquals(CardDepth.Preview, CardSteps.arrow(CardSteps.First, 3).depth)
        assertEquals(listOf(CardDepth.First, CardDepth.Reader), walkDeeper(0, true))
        assertEquals(listOf(CardDepth.First), walkDeeper(0, false))

        assertEquals(
            listOf(CardDepth.First, CardDepth.Reader, CardDepth.PictureMid, CardDepth.Picture),
            walkDeeper(1, false)
        )

        // the caller passes 0 for it: no preview, no picture steps, only a cut-off text opens the reader
        assertEquals(listOf(CardDepth.First), walkDeeper(0, false))
        assertEquals(listOf(CardDepth.First, CardDepth.Reader), walkDeeper(0, true))
    }

    @Test
    fun theOnlyPictureOpensFromTheReaderAtHalfSize() {
        val picture = CardSteps.deeper(CardSteps(reader = true), 1, false)!!
        assertEquals(0, picture.pictureIndex)
        assertEquals(CardDepth.PictureMid, picture.depth)
        assertNull(CardSteps.deeper(CardSteps.deeper(picture, 1, false)!!, 1, false))
    }

    @Test
    fun severalImagesOpenTheSelectorAndOnlyATapOpensOne() {
        assertEquals(
            listOf(CardDepth.First, CardDepth.Preview, CardDepth.Reader, CardDepth.Selector),
            walkDeeper(3, false)
        )
        val selector = CardSteps.deeper(CardSteps(reader = true), 3, false)!!
        assertNull(CardSteps.deeper(selector, 3, false))
        val full = CardSteps.pickFromSelector(selector, 1)
        assertEquals(CardDepth.Picture, full.depth)
        assertEquals(1, full.pictureIndex)
        // swipe up: full -> selector -> preview
        val back = CardSteps.back(full, 3)!!
        assertEquals(CardDepth.Selector, back.depth)
        assertEquals(CardDepth.Preview, CardSteps.back(back, 3)!!.depth)
    }

    @Test
    fun oneDragMovesExactlyOneStepEachWay() {
        val reader = CardSteps(reader = true)
        val mid = CardSteps.deeper(reader, 1, false)!!
        val full = CardSteps.deeper(mid, 1, false)!!
        assertEquals(CardDepth.PictureMid, mid.depth)
        assertEquals(CardDepth.Picture, full.depth)
        assertEquals(mid, CardSteps.back(full, 1))
        assertEquals(CardDepth.Reader, CardSteps.back(mid, 1)!!.depth)
        // the go-back of a short pull undoes exactly what the drag opened
        assertEquals(mid, CardSteps.back(CardSteps.deeper(mid, 1, false)!!, 1))
    }

    @Test
    fun halfPictureHeight() {
        assertEquals(177.5f, PictureSize.MID_DP, 0.001f)
        assertEquals(PictureSize.FULL_DP, PictureSize.heightDp(false), 0f)
        assertEquals(PictureSize.MID_DP, PictureSize.heightDp(true), 0f)
    }

    @Test
    fun theHalfFlagOnlyCountsWhileThePictureOfThatCardIsOpen() {
        val state = CardStepState(key = "a", reader = true, pictureHalf = true)
        assertEquals(CardDepth.PictureMid, state.steps("a", "a", 0).depth)
        assertEquals(CardDepth.Reader, state.steps("a", null, 0).depth)
        assertEquals(CardDepth.Picture, state.steps("b", "b", 0).depth)
    }

    @Test
    fun swipeUpWalksBackOneStepAtATime() {
        val picture = CardSteps(reader = true, preview = true, pictureIndex = 2)
        val half = CardSteps.back(picture, 3)!!
        assertEquals(CardDepth.PictureMid, half.depth)
        val reader = CardSteps.back(half, 3)!!
        assertEquals(CardDepth.Reader, reader.depth)
        assertEquals(CardDepth.Preview, CardSteps.back(reader, 3)!!.depth)
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(preview = true), 3)!!.depth)
        assertNull(CardSteps.back(CardSteps.First, 3))
    }

    @Test fun backFromReaderGoesToFirstState() {
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(reader = true), 1)!!.depth)
        assertEquals(CardDepth.First, CardSteps.back(CardSteps(reader = true), 0)!!.depth)
        assertEquals(CardDepth.First, CardSteps.arrow(CardSteps(reader = true), 3).depth)
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps(preview = true), 3).depth)
        assertEquals(CardDepth.Reader, CardSteps.arrow(CardSteps.First, 0).depth)
    }

    @Test
    fun cardStepIsolation() {
        val state = CardStepState(key = "a", reader = true)
        assertEquals(CardDepth.Reader, state.steps("a", null, 0).depth)
        assertEquals(CardDepth.First, state.steps("b", null, 0).depth)
        assertEquals(CardDepth.First, state.steps(null, null, 0).depth)
    }

    @Test
    fun openPictureMakesPictureStepOnlyForItsOwnCard() {
        val state = CardStepState(key = "a", reader = true)
        assertEquals(CardDepth.Picture, state.steps("a", "a", 1).depth)
        assertEquals(1, state.steps("a", "a", 1).pictureIndex)
        assertEquals(CardDepth.Reader, state.steps("a", "b", 1).depth)
    }
}

class CardPrewarmTest {
    @Test
    fun prewarmEligibility() {
        val cases = listOf(
            IslandMode.Notification to true,
            IslandMode.IncomingCall to true,
            IslandMode.DownloadUpload to true,
            IslandMode.Empty to false
        )
        for ((input, expected) in cases) {
            assertEquals("$input", expected, CardPrewarm.shouldPrewarm(input))
        }
        assertFalse(CardPrewarm.shouldPrewarm(null))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Music, screenOn = true))
        assertFalse(CardPrewarm.shouldPrewarm(IslandMode.Music, screenOn = false))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Notification, screenOn = false))
        assertTrue(CardPrewarm.shouldPrewarm(IslandMode.Timer, screenOn = false))
        assertFalse(CardPrewarm.shouldPrewarm(IslandMode.Battery, screenOn = true))
        assertFalse(CardPrewarm.shouldPrewarm(null, screenOn = true))

        listOf(
            IslandMode.Music, IslandMode.Battery, IslandMode.Bluetooth
        ).forEach { assertFalse(it.name, CardPrewarm.shouldPrewarm(it)) }

        listOf(
            IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
            IslandMode.Navigation, IslandMode.LiveActivity, IslandMode.Hotspot, IslandMode.Flashlight
        ).forEach { assertTrue(it.name, CardPrewarm.shouldPrewarm(it)) }
    }

}

class DisplayKeyTest {
    private val base = DisplayKey(1080, 2400, 0, 420)

    @Test
    fun anyChangeIsANewKey() {
        // turned upside down: same size
        assertNotEquals(base, base.copy(rotation = 2))
        // landscape, a folded or unfolded panel, a display size change
        assertNotEquals(base, base.copy(widthPx = 2400, heightPx = 1080, rotation = 1))
        assertNotEquals(base, base.copy(heightPx = 2640))
        assertNotEquals(base, base.copy(densityDpi = 480))
    }

    @Test
    fun openIslandReAnchorsInsideTheNewScreen() {
        // anchored far right on a 400 dp portrait screen
        val before = ExpandedAnchor.centreOffsetDp(170f, 300f, 400f)
        val after = ExpandedAnchor.centreOffsetDp(170f, 300f, 360f)
        assertEquals(42f, before, 0.001f)
        assertEquals(22f, after, 0.001f)
        // losing the camera (i.e. landscape with no cutout) puts the island back in the middle
        assertEquals(0f, ExpandedAnchor.centreOffsetDp(null, 300f, 360f), 0f)
    }
}
