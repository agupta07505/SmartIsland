/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTransportDetectorTest {
    @Test
    fun popupPlayerButtonsAreRecognisedAsAPlayer() {
        assertTrue(MediaTransportDetector.hasTransportButtons(listOf("Previous", "Pause", "Next", "Stop")))
    }

    @Test
    fun downloadButtonsAreNotAPlayer() {
        assertFalse(MediaTransportDetector.hasTransportButtons(listOf("Pause", "Cancel")))
        assertFalse(MediaTransportDetector.hasTransportButtons(emptyList()))
    }
}
