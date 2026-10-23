/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPillWidthTest {
    private val portrait = OrientationState(isLandscape = false, screenWidthDp = 400f)

    @Test
    fun idlePillKeepsItsWidth() {
        val s = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = false, contentPillWidthPercent = 150f)
        assertEquals(100f, s.forOrientation(portrait, hasContent = false).width, 1e-3f)
    }

    @Test
    fun contentPillStretchesByThePercent() {
        val s = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = false, contentPillWidthPercent = 150f)
        assertEquals(150f, s.forOrientation(portrait, hasContent = true).width, 1e-3f)
    }

    @Test
    fun sameAsIdleKeepsTheWidth() {
        val s = SmartIslandSettings.Default.copy(width = 100f, contentPillSameAsIdle = true, contentPillWidthPercent = 250f)
        assertEquals(100f, s.forOrientation(portrait, hasContent = true).width, 1e-3f)
    }

    @Test
    fun neverWiderThanTheScreenMinusMargins() {
        val s = SmartIslandSettings.Default.copy(width = 200f, contentPillSameAsIdle = false, contentPillWidthPercent = 250f)
        assertEquals(352f, s.forOrientation(portrait, hasContent = true).width, 1e-3f)
    }
}

class IslandExemptionTest {
    @Test
    fun listsAreSeparatePerOrientation() {
        val portrait = setOf("a.b")
        val landscape = setOf("c.d")
        org.junit.Assert.assertTrue(IslandExemption.isOff("a.b", portrait, landscape, isLandscape = false))
        org.junit.Assert.assertFalse(IslandExemption.isOff("a.b", portrait, landscape, isLandscape = true))
        org.junit.Assert.assertTrue(IslandExemption.isOff("c.d", portrait, landscape, isLandscape = true))
        org.junit.Assert.assertFalse(IslandExemption.isOff(null, portrait, landscape, isLandscape = false))
    }

    @Test
    fun landscapeContentWidthStretchesOnlyWhenAsked() {
        val s = SmartIslandSettings.Default.copy(landscapeWidth = 100f, contentPillSameAsIdleLandscape = false, contentPillWidthPercentLandscape = 200f)
        org.junit.Assert.assertEquals(200f, s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true).width, 1e-3f)
        org.junit.Assert.assertEquals(100f, s.forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true, hasCards = false).width, 1e-3f)
        org.junit.Assert.assertEquals(100f, s.copy(contentPillSameAsIdleLandscape = true).forOrientation(OrientationState(isLandscape = true, screenWidthDp = 800f), hasContent = true).width, 1e-3f)
    }
}
