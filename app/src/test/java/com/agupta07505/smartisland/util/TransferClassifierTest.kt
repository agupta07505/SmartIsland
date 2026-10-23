/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.TransferKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferClassifierTest {
    private val recorder = "com.google.android.apps.recorder"

    @Test
    fun recorderBackupIsATransferNotARecording() {
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Backing up recordings", false))
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Uploading to your account", false))
        assertTrue(TransferClassifier.isRecorderTransfer(recorder, "Recorder Syncing", true))
    }

    @Test
    fun liveRecordingIsStillARecording() {
        assertFalse(TransferClassifier.isRecorderTransfer(recorder, "Recorder Recording 00:12", false))
        assertFalse(TransferClassifier.isRecorderTransfer(recorder, "Recorder Transcribing", true))
    }

    @Test
    fun otherAppsAreNotRecorderTransfers() {
        assertFalse(TransferClassifier.isRecorderTransfer("com.whatsapp", "Backing up chats", true))
        assertFalse(TransferClassifier.isRecorderTransfer("com.sec.android.app.screenrecorder", "Uploading", true))
    }

    @Test
    fun recorderTransfersClassifyAsBackup() {
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify(recorder, "Recorder Backing up recordings"))
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify(recorder, "Recorder 40%"))
    }

    @Test
    fun whatsappBackupAndRestore() {
        assertEquals(TransferKind.BACKUP, TransferClassifier.classify("com.whatsapp", "Backing up chats and media"))
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.whatsapp", "Restoring backup"))
    }

    @Test
    fun downloadsUploadsAndFileOperations() {
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.android.chrome", "video.bin 12% 3 min left"))
        assertEquals(TransferKind.DOWNLOAD, TransferClassifier.classify("com.deniscerri.ytdl", "Some video Downloading"))
        assertEquals(TransferKind.UPLOAD, TransferClassifier.classify("com.google.android.apps.docs", "Uploading report.pdf"))
        assertEquals(TransferKind.COPY_MOVE, TransferClassifier.classify("com.google.android.documentsui", "Copying 3 files"))
        assertEquals(TransferKind.COPY_MOVE, TransferClassifier.classify("com.google.android.documentsui", "Working"))
        assertEquals(TransferKind.DELETE, TransferClassifier.classify("com.google.android.documentsui", "Deleting 12 files"))
        assertEquals(TransferKind.INSTALL, TransferClassifier.classify("com.android.vending", "Downloading Maps"))
        assertEquals(TransferKind.INSTALL, TransferClassifier.classify("com.some.app", "Installing update"))
    }

    @Test
    fun knownTransferAppsAreRecognised() {
        assertTrue(TransferClassifier.isKnownTransferApp("com.deniscerri.ytdl"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.brave.browser"))
        assertTrue(TransferClassifier.isKnownTransferApp("org.mozilla.firefox"))
        assertTrue(TransferClassifier.isKnownTransferApp("com.google.android.documentsui"))
        assertFalse(TransferClassifier.isKnownTransferApp("com.whatsapp"))
    }
}
