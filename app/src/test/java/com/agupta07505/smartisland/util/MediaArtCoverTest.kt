/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandNotification
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaArtCoverTest {
    @Test
    fun noRememberedCoverMeansNothingToHold() {
        assertNull(MediaArt.lastGood("never-seen-card", null))
    }

    @Test
    fun quickWithoutAnyPictureIsNotAFallbackIcon() {
        // A card with neither a large icon nor an app icon has nothing to draw, so it is not the app-icon stand-in.
        val n = IslandNotification(key = "k1", packageName = "p", appName = "App", title = "t", text = "x", timeMillis = 0L)
        assertTrue(!MediaArt.quick(n).isFallbackIcon)
    }
}
