/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLogRecorderTrimTest {

    @Before
    fun setUp() = AppLogRecorder.resetForTest()

    @After
    fun tearDown() = AppLogRecorder.resetForTest()

    @Test(timeout = 10_000)
    fun bufferStopsAtTheLimitInsteadOfLoopingForever() {
        AppLogRecorder.startRecordingForTest()
        repeat(3_500) { AppLogRecorder.i("Test", "entry $it") }
        assertEquals(3_000, AppLogRecorder.getLogCount())
    }

    @Test(timeout = 10_000)
    fun trimKeepsTheNewestEntries() {
        AppLogRecorder.startRecordingForTest()
        repeat(3_100) { AppLogRecorder.i("Test", "entry $it") }
        val last = AppLogRecorder.lastEntryForTest()
        assertNotNull(last)
        assertTrue(last!!.endsWith("entry 3099"))
    }

    @Test
    fun nothingIsStoredWhileRecordingIsOff() {
        AppLogRecorder.i("Test", "ignored")
        assertEquals(0, AppLogRecorder.getLogCount())
    }

    @Test
    fun belowTheLimitEveryEntryIsKept() {
        AppLogRecorder.startRecordingForTest()
        repeat(10) { AppLogRecorder.w("Test", "entry $it") }
        assertEquals(10, AppLogRecorder.getLogCount())
    }
}
