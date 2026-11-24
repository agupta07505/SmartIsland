/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import com.agupta07505.smartisland.model.FoldPosture
import com.agupta07505.smartisland.util.StatusBarReapplier.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutModeTest {
    @Test fun switchListsTwoOrFourFold() {
        assertEquals(listOf(LayoutMode.Portrait, LayoutMode.Landscape), LayoutMode.listFor(false))
        assertEquals(
            listOf(
                LayoutMode.FoldedPortrait, LayoutMode.FoldedLandscape,
                LayoutMode.UnfoldedPortrait, LayoutMode.UnfoldedLandscape
            ),
            LayoutMode.listFor(true)
        )
    }

    @Test fun foldModeMapsPostureOrientation() {
        assertNull(LayoutMode.Portrait.posture)
        assertNull(LayoutMode.Landscape.posture)
        for (mode in LayoutMode.Fold) {
            assertEquals(mode.landscape, mode.posture!!.landscape)
        }
        assertEquals(FoldPosture.Modes, LayoutMode.Fold.map { it.posture })
    }

    @Test
    fun layoutSelectionTracksFoldableMode() {
        assertEquals(LayoutMode.FoldedPortrait, LayoutMode.resolve(LayoutMode.Portrait, foldableMode = true, unfoldedNow = false))
        assertEquals(LayoutMode.UnfoldedPortrait, LayoutMode.resolve(LayoutMode.Portrait, foldableMode = true, unfoldedNow = true))
        assertEquals(LayoutMode.FoldedLandscape, LayoutMode.resolve(LayoutMode.Landscape, foldableMode = true, unfoldedNow = false))
        assertEquals(LayoutMode.UnfoldedLandscape, LayoutMode.resolve(LayoutMode.Landscape, foldableMode = true, unfoldedNow = true))
        assertEquals(LayoutMode.Portrait, LayoutMode.resolve(LayoutMode.UnfoldedPortrait, foldableMode = false, unfoldedNow = true))
        assertEquals(LayoutMode.Landscape, LayoutMode.resolve(LayoutMode.FoldedLandscape, foldableMode = false, unfoldedNow = false))
        assertEquals(LayoutMode.UnfoldedLandscape, LayoutMode.resolve(LayoutMode.UnfoldedLandscape, foldableMode = true, unfoldedNow = false))
        assertEquals(LayoutMode.Landscape, LayoutMode.resolve(LayoutMode.Landscape, foldableMode = false, unfoldedNow = true))
    }


}

class StatusBarHideSectionTest {
    @Test
    fun statusBarStateLabel() {
        assertEquals("Off", statusBarStateLabel(false, ShizukuReadiness.Ready, Status.Applied))
        assertEquals("Needs Shizuku", statusBarStateLabel(true, ShizukuReadiness.NotRunning, Status.Off))
        assertEquals("Needs Shizuku", statusBarStateLabel(true, ShizukuReadiness.Ready, Status.ShizukuLost))
        assertEquals("Hidden", statusBarStateLabel(true, ShizukuReadiness.Ready, Status.Applied))
        assertEquals("Failed", statusBarStateLabel(true, ShizukuReadiness.Ready, Status.Failed))
        assertEquals("Applying…", statusBarStateLabel(true, ShizukuReadiness.Ready, Status.Off))
    }


}
