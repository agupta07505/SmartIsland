/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeStyleTest {
    @Test
    fun unknownIdsFallBackToSlide() {
        assertEquals(MergeStyle.SLIDE, MergeStyle.fromId(null))
        assertEquals(MergeStyle.SLIDE, MergeStyle.fromId("jelly"))
        assertEquals(MergeStyle.BUBBLE, MergeStyle.fromId("bubble"))
    }

    @Test
    fun mergesAreShortAndBubbleIsTheLongerOne() {
        assertEquals(420, MergeStyle.SLIDE.baseDurationMs)
        assertEquals(900, MergeStyle.BUBBLE.baseDurationMs)
        assertTrue(MergeStyle.BUBBLE.baseDurationMs > MergeStyle.SLIDE.baseDurationMs)
    }

    @Test
    fun defaultIsSlide() {
        assertEquals(MergeStyle.SLIDE, SmartIslandSettings.Default.mergeStyleKind())
    }

    @Test
    fun roundTripsThroughBackup() {
        val custom = SmartIslandSettings(mergeStyle = MergeStyle.ID_BUBBLE)
        val restored = SmartIslandSettings.fromJson(custom.toJson(appVersion = "1"))
        assertEquals(MergeStyle.ID_BUBBLE, restored.mergeStyle)
    }

    @Test
    fun badOrMissingBackupValueKeepsDefault() {
        val bad = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "wobble"}}""")
        assertEquals(MergeStyle.ID_SLIDE, bad.mergeStyle)
        val old = SmartIslandSettings.fromJson("""{"settings": {"enableSwipeActions": true}}""")
        assertEquals(MergeStyle.ID_SLIDE, old.mergeStyle)
        assertEquals(MergeStyle.ID_SLIDE, old.mergeStyleLandscape)
    }

    @Test
    fun landscapeHasItsOwnChoiceAndRoundTrips() {
        val custom = SmartIslandSettings(mergeStyle = MergeStyle.ID_SLIDE, mergeStyleLandscape = MergeStyle.ID_BUBBLE)
        val restored = SmartIslandSettings.fromJson(custom.toJson(appVersion = "1"))
        assertEquals(MergeStyle.ID_SLIDE, restored.mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, restored.mergeStyleLandscape)
        assertEquals(MergeStyle.SLIDE, restored.mergeStyleKind(landscape = false))
        assertEquals(MergeStyle.BUBBLE, restored.mergeStyleKind(landscape = true))
    }

    @Test
    fun oldBackupWithOneSharedChoiceStartsLandscapeFromIt() {
        val old = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "bubble"}}""")
        assertEquals(MergeStyle.ID_BUBBLE, old.mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, old.mergeStyleLandscape)
    }

    @Test
    fun badLandscapeValueFallsBackToTheSharedChoiceThenTheDefault() {
        val bad = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "bubble", "mergeStyleLandscape": "wobble"}}""")
        assertEquals(MergeStyle.ID_BUBBLE, bad.mergeStyleLandscape)
        val both = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "x", "mergeStyleLandscape": "y"}}""")
        assertEquals(MergeStyle.ID_SLIDE, both.mergeStyleLandscape)
    }

    @Test
    fun theOverlayReadsTheChoiceOfTheOrientationItIsIn() {
        val s = SmartIslandSettings(mergeStyle = MergeStyle.ID_SLIDE, mergeStyleLandscape = MergeStyle.ID_BUBBLE)
        val landscape = OrientationState(isLandscape = true, screenWidthDp = 800f)
        assertEquals(MergeStyle.ID_SLIDE, s.forOrientation(OrientationState()).mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, s.forOrientation(landscape).mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, s.forOrientation(landscape, hasContent = true).mergeStyle)
    }
}
