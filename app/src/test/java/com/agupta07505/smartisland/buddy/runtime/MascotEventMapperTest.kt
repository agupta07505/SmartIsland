/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.buddy.runtime

import com.agupta07505.smartisland.buddy.model.MascotReaction
import com.agupta07505.smartisland.buddy.model.MascotVisitKind
import com.agupta07505.smartisland.model.IslandMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotEventMapperTest {
    private fun note(key: String, mode: IslandMode, title: String = "", text: String = "", progress: Int = 0,
                     max: Int = 0, playing: Boolean = false, time: Long = 0L) =
        NoteSnapshot(key, mode, title, text, progress, max, playing, time)

    private val mapper = MascotEventMapper()
    private fun feed(vararg notes: NoteSnapshot, sec: Double = 1000.0, ms: Long = 1_000_000L) =
        mapper.onChange(notes.toList(), sec, ms)

    @Test
    fun musicStartingGrooves() {
        assertEquals(emptyList<MascotMoment>(), feed(note("m", IslandMode.Music, playing = false)))
        assertEquals(listOf(MascotMoment.Music), feed(note("m", IslandMode.Music, playing = true)))
    }

    @Test
    fun musicGroovesAtMostOnceInTenMinutes() {
        feed(note("m", IslandMode.Music, playing = true), sec = 1000.0)
        feed(note("m", IslandMode.Music, playing = false), sec = 1100.0)
        assertEquals(emptyList<MascotMoment>(), feed(note("m", IslandMode.Music, playing = true), sec = 1500.0))
        feed(note("m", IslandMode.Music, playing = false), sec = 1600.0)
        assertEquals(listOf(MascotMoment.Music), feed(note("m", IslandMode.Music, playing = true), sec = 1601.0))
    }

    @Test
    fun aSongThatKeepsPlayingDoesNotGrooveAgain() {
        feed(note("m", IslandMode.Music, playing = true), sec = 1000.0)
        assertEquals(emptyList<MascotMoment>(), feed(note("m", IslandMode.Music, playing = true), sec = 2000.0))
    }

    @Test
    fun recordingAppearingIsReady() {
        assertEquals(listOf(MascotMoment.ScreenRecording), feed(note("r", IslandMode.ScreenRecording)))
        assertEquals(emptyList<MascotMoment>(), feed(note("r", IslandMode.ScreenRecording)))
    }

    @Test
    fun timerStartingAndRunningOut() {
        val end = 1_000_000L + 60_000
        assertEquals(listOf(MascotMoment.TimerStarted), feed(note("t", IslandMode.Timer, time = end)))
        // It goes away with a second left: it ran out.
        assertEquals(listOf(MascotMoment.TimeIsUp), feed(sec = 1060.0, ms = end - 1000))
    }

    @Test
    fun aTimerCancelledEarlyIsNotTimeIsUp() {
        val end = 1_000_000L + 60_000
        feed(note("t", IslandMode.Timer, time = end))
        assertEquals(emptyList<MascotMoment>(), feed(sec = 1010.0, ms = end - 50_000))
    }

    @Test
    fun aFinishedTransferCelebrates() {
        feed(note("d", IslandMode.DownloadUpload, progress = 100, max = 100))
        assertEquals(listOf(MascotMoment.DownloadFinished), feed())
    }

    @Test
    fun aTransferThatVanishesHalfwayIsNotACelebration() {
        feed(note("d", IslandMode.DownloadUpload, progress = 40, max = 100))
        assertEquals(emptyList<MascotMoment>(), feed())
    }

    @Test
    fun aFailedTransferIsConfused() {
        feed(note("d", IslandMode.DownloadUpload, "Downloading file", "Download failed", 30, 100))
        assertEquals(listOf(MascotMoment.DownloadFailed), feed())
    }

    @Test
    fun aDownloadCompleteMessageCelebratesAndAFailureMessageConfuses() {
        assertEquals(
            listOf(MascotMoment.DownloadFinished),
            feed(note("n1", IslandMode.Notification, "Report.pdf", "Download complete"))
        )
        assertEquals(
            listOf(MascotMoment.DownloadFailed),
            feed(note("n1", IslandMode.Notification, "Report.pdf", "Download complete"),
                note("n2", IslandMode.Notification, "Photos", "Upload failed. Tap to retry"))
        )
    }

    @Test
    fun aRepeatedOrUnrelatedNotificationDoesNothing() {
        assertEquals(emptyList<MascotMoment>(), feed(note("c", IslandMode.Notification, "Alice", "Lunch?")))
        assertEquals(emptyList<MascotMoment>(), feed(note("c", IslandMode.Notification, "Alice", "Lunch?")))
    }

    @Test
    fun resetForgetsWhatItSaw() {
        feed(note("r", IslandMode.ScreenRecording))
        mapper.reset()
        assertEquals(listOf(MascotMoment.ScreenRecording), feed(note("r", IslandMode.ScreenRecording)))
    }

    @Test
    fun downloadTextClassification() {
        assertEquals(DownloadOutcome.Failed, MascotDownloadText.classify("Download failed", ""))
        assertEquals(DownloadOutcome.Failed, MascotDownloadText.classify("", "Couldn't finish the upload"))
        assertEquals(DownloadOutcome.Finished, MascotDownloadText.classify("Download complete", ""))
        assertEquals(DownloadOutcome.Finished, MascotDownloadText.classify("Backup finished", "All done"))
        assertEquals(DownloadOutcome.Failed, MascotDownloadText.classify("下载失败", ""))
        assertEquals(DownloadOutcome.Finished, MascotDownloadText.classify("下载完成", ""))
        assertNull(MascotDownloadText.classify("Alice", "Are you done with the report?"))
        assertNull(MascotDownloadText.classify("Downloading", "45%"))
        assertNull(MascotDownloadText.classify("", ""))
    }
}

class MascotCameoTest {
    private var now = 100.0
    private fun controller() = MascotController { now }

    @Test
    fun aBusyPillWithRoomPlaysAReactionAsACameo() {
        val c = controller()
        c.setCanCameo(true)
        c.post(MascotMoment.Charging)
        assertEquals(MascotVisitKind.Cameo(MascotReaction.Love), c.visit.value?.kind)
        assertNull(c.reaction.value)
    }

    @Test
    fun aBusyPillWithoutRoomWaitsForRest() {
        val c = controller()
        c.post(MascotMoment.Charging)
        assertNull(c.visit.value)
        c.setAtRest(true)
        assertEquals(MascotReaction.Love, c.reaction.value?.reaction)
        assertNull(c.visit.value)
    }

    @Test
    fun aWaitingReactionBecomesACameoWhenRoomAppears() {
        val c = controller()
        c.post(MascotMoment.DownloadFinished)
        now += 3.0
        c.setCanCameo(true)
        assertEquals(MascotVisitKind.Cameo(MascotReaction.Celebrate), c.visit.value?.kind)
    }

    @Test
    fun aCameoIsSubjectToTheGateToo() {
        val c = controller()
        c.setCanCameo(true)
        c.post(MascotMoment.Charging)
        c.endVisit(c.visit.value!!.id)
        now += 1.0
        c.post(MascotMoment.DownloadFinished)
        assertNull(c.visit.value)
    }

    @Test
    fun aTimerWatchComesOutOnlyWithRoomAndRetreatsWhenCancelled() {
        val c = controller()
        assertFalse(c.startCountdown(5f))
        c.setCanCameo(true)
        assertTrue(c.startCountdown(5f))
        assertEquals(MascotVisitKind.Countdown(5f), c.visit.value?.kind)
        c.retreat()
        assertEquals(MascotVisitKind.Retreat, c.visit.value?.kind)
    }

    @Test
    fun retreatWithNothingToWatchDoesNothing() {
        val c = controller()
        c.setCanCameo(true)
        c.retreat()
        assertNull(c.visit.value)
    }

    @Test
    fun countdownMomentsDriveTheController() {
        val c = controller()
        c.setCanCameo(true)
        c.post(MascotMoment.CountdownStart)
        assertEquals(MascotVisitKind.Countdown(5f), c.visit.value?.kind)
        c.post(MascotMoment.CountdownCancel)
        assertEquals(MascotVisitKind.Retreat, c.visit.value?.kind)
    }

    @Test
    fun crossingStartsAtRestAndLeavesTheBuddyShownWhenItEnds() {
        val c = controller()
        assertFalse(c.startCross())
        c.setAtRest(true)
        c.hideForArrival()
        assertTrue(c.startCross())
        assertTrue(c.tucked.value)
        c.endVisit(c.visit.value!!.id)
        assertFalse(c.tucked.value)
    }
}
