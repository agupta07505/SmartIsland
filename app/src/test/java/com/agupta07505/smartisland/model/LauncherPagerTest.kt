/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.model.LauncherPage.Apps
import com.agupta07505.smartisland.model.LauncherPage.Recents
import com.agupta07505.smartisland.model.LauncherPage.Utilities
import com.agupta07505.smartisland.model.LauncherSwipe.Down
import com.agupta07505.smartisland.model.LauncherSwipe.Left
import com.agupta07505.smartisland.model.LauncherSwipe.Right
import com.agupta07505.smartisland.model.LauncherSwipe.Up
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPagerTest {
    @Test fun upAndDownLoopBetweenTheAppsLevelAndUtilities() {
        assertEquals(Utilities, launcherPageAfter(Apps, Up, recentsEnabled = true))
        assertEquals(Utilities, launcherPageAfter(Apps, Down, recentsEnabled = true))
        assertEquals(Apps, launcherPageAfter(Utilities, Up, recentsEnabled = true))
        assertEquals(Apps, launcherPageAfter(Utilities, Down, recentsEnabled = true))
    }

    @Test fun utilitiesReturnsToTheAppsLevelPageThatWasLeft() {
        assertEquals(Utilities, launcherPageAfter(Recents, Up, true, lastAppsLevelPage = Recents))
        assertEquals(Recents, launcherPageAfter(Utilities, Up, true, lastAppsLevelPage = Recents))
        // Recents turned off in the meantime: back to Apps.
        assertEquals(Apps, launcherPageAfter(Utilities, Down, false, lastAppsLevelPage = Recents))
    }

    @Test fun leftAndRightSwapAppsAndRecents() {
        assertEquals(Recents, launcherPageAfter(Apps, Left, true))
        assertEquals(Recents, launcherPageAfter(Apps, Right, true))
        assertEquals(Apps, launcherPageAfter(Recents, Left, true))
        assertEquals(Apps, launcherPageAfter(Recents, Right, true))
    }

    @Test fun sidewaysDoesNothingWithoutRecentsOrOnUtilities() {
        assertEquals(Apps, launcherPageAfter(Apps, Left, false))
        assertEquals(Apps, launcherPageAfter(Recents, Right, false))
        assertEquals(Utilities, launcherPageAfter(Utilities, Left, true))
        assertEquals(Utilities, launcherPageAfter(Utilities, Right, false))
    }

    @Test fun aDragIsOwnedOnlyWhileTheLauncherIsOpenAndTheFingerMoves() {
        assertTrue(launcherOwnsDrag(true, 0f, 3f))
        assertTrue(launcherOwnsDrag(true, -2f, 0f))
        assertFalse(launcherOwnsDrag(true, 0.2f, 0.1f))
        assertFalse(launcherOwnsDrag(false, 30f, 30f))
    }

    @Test fun theLongerAxisPicksTheSwipeOncePastTheThreshold() {
        assertNull(launcherSwipeFor(10f, -20f, 48f))
        assertEquals(Up, launcherSwipeFor(8f, -60f, 48f))
        assertEquals(Down, launcherSwipeFor(-8f, 60f, 48f))
        assertEquals(Left, launcherSwipeFor(-70f, 12f, 48f))
        assertEquals(Right, launcherSwipeFor(70f, -12f, 48f))
        // A diagonal drag that is not clearly longer sideways counts as vertical.
        assertEquals(Up, launcherSwipeFor(60f, -60f, 48f))
    }
}
