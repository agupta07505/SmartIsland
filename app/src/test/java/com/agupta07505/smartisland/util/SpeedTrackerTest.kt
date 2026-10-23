/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.util.SpeedTracker.Phase
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTrackerTest {
    private val mb = 1024L * 1024L

    @Test
    fun startsOnlyAfterSustainedHeavyTraffic() {
        val t = SpeedTracker()
        assertEquals(Phase.IDLE, t.onSample(1_000, 2 * mb, 0))
        assertEquals(Phase.IDLE, t.onSample(2_000, 2 * mb, 0))
        assertEquals(Phase.IDLE, t.onSample(4_000, 2 * mb, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(5_000, 2 * mb, 0))
    }

    @Test
    fun lightTrafficNeverStarts() {
        val t = SpeedTracker()
        for (s in 1..20) assertEquals(Phase.IDLE, t.onSample(s * 1_000L, 300 * 1024L, 10 * 1024L))
    }

    @Test
    fun uploadIsRecognisedByOutgoingTraffic() {
        val t = SpeedTracker()
        var phase = Phase.IDLE
        for (s in 1..6) phase = t.onSample(s * 1_000L, 100 * 1024L, 2 * mb)
        assertEquals(Phase.UPLOAD, phase)
    }

    @Test
    fun aShortStallDoesNotDropTheCard() {
        val t = SpeedTracker()
        for (s in 1..5) t.onSample(s * 1_000L, 2 * mb, 0)
        assertEquals(Phase.DOWNLOAD, t.onSample(6_000, 0, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(8_000, 0, 0))
        assertEquals(Phase.DOWNLOAD, t.onSample(9_000, 2 * mb, 0))
    }

    @Test
    fun endsAfterSixQuietSeconds() {
        val t = SpeedTracker()
        for (s in 1..5) t.onSample(s * 1_000L, 2 * mb, 0)
        assertEquals(Phase.DOWNLOAD, t.onSample(10_000, 0, 0))
        assertEquals(Phase.IDLE, t.onSample(11_000, 0, 0))
    }

    @Test
    fun formatsSpeeds() {
        assertEquals("2.3 MB/s", SpeedTracker.formatSpeed(2_411_724L))
        assertEquals("512 KB/s", SpeedTracker.formatSpeed(512L * 1024L))
    }
}
