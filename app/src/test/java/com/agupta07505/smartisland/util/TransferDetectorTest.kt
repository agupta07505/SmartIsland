/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.TransferKind
import com.agupta07505.smartisland.model.TransferState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferDetectorTest {

    private fun verdict(input: TransferInput) = TransferDetector.evaluate(input)

    @Test
    fun browserDownloadWithBarSpeedAndEta() {
        val v = verdict(
            TransferInput(
                packageName = "com.android.chrome", title = "video.bin", text = "45% · 3 min left",
                progress = 45, progressMax = 100, isOngoing = true
            )
        )
        assertTrue(v.isTransfer)
        assertEquals(45, v.percent)
        assertEquals("3 min left", v.eta)
        assertEquals(TransferState.ACTIVE, v.state)
    }

    @Test
    fun categoryProgressAloneIsStillATransfer() {
        assertTrue(verdict(TransferInput(category = "progress")).isTransfer)
    }

    @Test
    fun barPlusLooseWordIsATransferButBarAloneIsNot() {
        assertTrue(verdict(TransferInput(packageName = "com.x", title = "report.pdf", progress = 10, progressMax = 100)).isTransfer)
        assertFalse(verdict(TransferInput(packageName = "com.x", title = "Workout", progress = 10, progressMax = 100)).isTransfer)
    }

    @Test
    fun foregroundServiceThatOnlyWritesDownloadingIsATransfer() {
        assertTrue(verdict(TransferInput(packageName = "com.spotify.music", title = "Downloading 3 songs", isOngoing = true)).isTransfer)
    }

    @Test
    fun networkSpeedMeterIsNotATransfer() {
        val meter = TransferInput(packageName = "com.example.netspeed", title = "↓ 2.1 MB/s ↑ 0.3 MB/s", isOngoing = true)
        assertFalse(verdict(meter).isTransfer)
    }

    @Test
    fun chatMessageMentioningAPercentageIsNotATransfer() {
        val chat = TransferInput(category = "msg", title = "Sam", text = "I'm at 50% of the download", isConversational = true)
        assertFalse(verdict(chat).isTransfer)
    }

    @Test
    fun mediaWithoutBarGroupSummaryAndMessageSyncAreRejected() {
        assertFalse(verdict(TransferInput(isMedia = true, title = "Song download file", isOngoing = true)).isTransfer)
        assertFalse(verdict(TransferInput(category = "progress", isGroupSummary = true)).isTransfer)
        assertFalse(verdict(TransferInput(category = "progress", isMessageSync = true)).isTransfer)
    }

    @Test
    fun android16ProgressStyleWithoutBarGetsAProgressFromTheText() {
        val v = verdict(
            TransferInput(
                packageName = "com.x", title = "Uploading photos", text = "12 of 40 photos",
                isProgressStyle = true, isOngoing = true, requestedPromoted = true
            )
        )
        assertTrue(v.isTransfer)
        assertEquals(30, v.percent)
    }

    @Test
    fun sizeOfTotalGivesAPercentage() {
        val v = verdict(TransferInput(packageName = "com.x", title = "Downloading", text = "12.5 of 50 MB", isOngoing = true))
        assertTrue(v.isTransfer)
        assertEquals(25, v.percent)
    }

    @Test
    fun percentInTextIsUsedWhenThereIsNoBar() {
        val v = verdict(TransferInput(packageName = "com.x", title = "Downloading file.zip", text = "62%"))
        assertTrue(v.isTransfer)
        assertEquals(62, v.percent)
    }

    @Test
    fun channelNameHelpsOnlyTogetherWithProgressEvidence() {
        val noBar = TransferInput(packageName = "com.x", channelId = "file_downloads", title = "Working", isOngoing = true)
        assertFalse(verdict(noBar).isTransfer)
        assertTrue(verdict(noBar.copy(indeterminate = true)).isTransfer)
    }

    @Test
    fun otherLanguagesAreRecognised() {
        assertTrue(verdict(TransferInput(packageName = "com.x", title = "Descargando archivo.zip", progress = 5, progressMax = 10)).isTransfer)
        assertTrue(verdict(TransferInput(packageName = "com.x", title = "正在下载 文件", indeterminate = true)).isTransfer)
        assertTrue(verdict(TransferInput(packageName = "com.x", title = "Datei wird hochgeladen", indeterminate = true)).isTransfer)
    }

    @Test
    fun whatsAppChatBackupIsATransfer() {
        assertTrue(verdict(TransferInput(packageName = "com.whatsapp", title = "Backup in progress", text = "Backing up chats", indeterminate = true)).isTransfer)
        assertTrue(verdict(TransferInput(packageName = "com.whatsapp", title = "Backup in progress", progress = 20, progressMax = 100)).isTransfer)
    }

    @Test
    fun cloudAppWithBarIsATransfer() {
        assertTrue(verdict(TransferInput(packageName = "com.dropbox.android", title = "Working", progress = 3, progressMax = 10)).isTransfer)
    }

    @Test
    fun statesFromWordingAndButtons() {
        val base = TransferInput(packageName = "com.android.chrome", title = "video.bin", progress = 40, progressMax = 100)
        assertEquals(TransferState.PAUSED, verdict(base.copy(text = "Paused", actionLabels = listOf("Resume", "Cancel"))).state)
        assertEquals(TransferState.PAUSED, verdict(base.copy(text = "40%", actionLabels = listOf("Resume", "Cancel"))).state)
        assertEquals(TransferState.ACTIVE, verdict(base.copy(text = "40%", actionLabels = listOf("Pause", "Cancel"))).state)
        assertEquals(TransferState.FAILED, verdict(base.copy(text = "Download failed")).state)
        assertEquals(TransferState.WAITING, verdict(base.copy(text = "Waiting for network")).state)
        assertEquals(TransferState.ACTIVE, verdict(base.copy(text = "3 done, 0 failed")).state)
    }

    @Test
    fun textAnalyzerReadsSpeedPercentAndEta() {
        val f = TransferTextAnalyzer.analyze("Downloading 45% • 1.2 MB/s • 5 min left")
        assertEquals(45, f.percent)
        assertEquals("1.2 MB/s", f.speed)
        assertEquals("5 min left", f.eta)
        assertEquals("800 kbps", TransferTextAnalyzer.analyze("up 800 kbps").speed)
        assertNull(TransferTextAnalyzer.analyze("1000%").percent)
    }

    @Test
    fun classifierHandlesLanguagesCloudAppsSyncAndSeeding() {
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.x", "Descargando archivo.zip"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.x", "Subiendo foto"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.x", "Datei wird hochgeladen"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.dropbox.android", "Working"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.google.android.apps.photos", "Backing up 3 of 120"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.x", "Syncing files"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("org.proninyaroslav.libretorrent", "Seeding ubuntu.iso"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.delphicoder.flud"))
        assertTrue(TransferClassifier.isCloudApp("com.nextcloud.client"))
    }
}
