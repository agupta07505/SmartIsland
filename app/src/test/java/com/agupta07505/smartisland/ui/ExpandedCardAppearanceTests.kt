/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.CardSteps
import com.agupta07505.smartisland.ui.expanded.ExpandedThemeRules
import com.agupta07505.smartisland.ui.expanded.IslandPalette
import com.agupta07505.smartisland.ui.expanded.PictureArrowSide
import com.agupta07505.smartisland.ui.expanded.besideDragSplitProgress
import com.agupta07505.smartisland.ui.expanded.cardScrimScale
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush
import com.agupta07505.smartisland.ui.expanded.expandedSurfaceColor
import com.agupta07505.smartisland.ui.expanded.pictureArrowSide
import com.agupta07505.smartisland.ui.expanded.pictureIslandHeight
import com.agupta07505.smartisland.ui.expanded.pictureShownBesideCard
import com.agupta07505.smartisland.ui.expanded.stepsOpenBesideCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardStyleTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun scrimFullOrLightByGradient() {
        assertEquals(1f, cardScrimScale(base), 0.001f)
        assertEquals(0.3f, cardScrimScale(base.copy(expandedGradientEnabled = true)), 0.001f)
    }

    @Test
    fun gradientBrushOnlyWhenEnabled() {
        assertNull(expandedGradientBrush(base))
        assertNotNull(expandedGradientBrush(base.copy(expandedGradientEnabled = true)))
    }
}

class ExpandedThemeTest {
    @Test
    fun themeResolvesAgainstDarkMode() {
        assertFalse(ExpandedThemeRules.isLight("dark", systemDark = false))
        assertFalse(ExpandedThemeRules.isLight("dark", systemDark = true))
        assertTrue(ExpandedThemeRules.isLight("light", systemDark = false))
        assertTrue(ExpandedThemeRules.isLight("light", systemDark = true))
        assertTrue(ExpandedThemeRules.isLight("system", systemDark = false))
        assertFalse(ExpandedThemeRules.isLight("system", systemDark = true))
        assertFalse(ExpandedThemeRules.isLight("sepia", systemDark = false))
    }

    @Test
    fun darkPaletteKeepsTheOriginalColours() {
        val p = IslandPalette.Dark
        assertEquals(Color.White, p.text)
        assertEquals(Color(0xFFD5DAE0), p.textSecondary)
        assertEquals(Color(0xFFB7C0CA), p.textTertiary)
        assertEquals(Color.White.copy(alpha = 0.2f), p.tint(0.2f))
        assertEquals(Color(0xFF222222), p.pick(Color(0xFF222222), Color.Red))
    }

    @Test
    fun lightTextReadableOnLight() {
        val p = IslandPalette.Light
        assertTrue(contrast(p.text, p.surface) >= 7.0)
        assertTrue(contrast(p.textSecondary, p.surface) >= 7.0)
        assertTrue(contrast(p.textTertiary, p.surface) >= 4.5)
        assertTrue(contrast(p.textMuted, p.surface) >= 4.5)
        assertTrue(contrast(p.accentCyan, p.surface) >= 4.5)
        assertTrue(contrast(p.danger, p.surface) >= 4.5)
        assertTrue(contrast(p.success, p.surface) >= 4.5)
        assertTrue(contrast(p.warning, p.surface) >= 4.5)
        assertTrue(contrast(p.onInverse, p.inverseFill) >= 7.0)
        assertTrue(contrast(p.chipText, p.chipFill) >= 7.0)
    }

    @Test
    fun lightOverlaysAreDarkNotWhite() {
        val t = IslandPalette.Light.tint(0.2f)
        assertEquals(0f, t.red, 0f)
        assertTrue(t.alpha in 0.1f..0.2f)
    }

    @Test
    fun lightIslandUsesSurface() {
        val settings = SmartIslandSettings.Default.copy(pillColor = 0xFF123456L)
        assertEquals(Color(0xFF123456L), expandedSurfaceColor(settings, IslandPalette.Dark))
        assertEquals(IslandPalette.Light.surface, expandedSurfaceColor(settings, IslandPalette.Light))
    }

    @Test
    fun gradientStillWorksInLight() {
        val settings = SmartIslandSettings.Default.copy(expandedGradientEnabled = true)
        assertNotNull(expandedGradientBrush(settings, IslandPalette.Light))
    }

    private fun contrast(a: Color, b: Color): Double {
        fun lum(c: Color): Double {
            fun ch(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }
            return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
        }
        val l1 = lum(a)
        val l2 = lum(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }
}

class PictureIslandTest {
    @Test
    fun picturePickerVisibility() {
        assertTrue(pictureShownBesideCard(true, "card", 1, "card", 2))

        assertFalse(pictureShownBesideCard(false, "card", 0, "card", 2))

        assertFalse(pictureShownBesideCard(true, null, 0, "card", 2))
        assertFalse(pictureShownBesideCard(true, "other", 0, "card", 2))
        assertFalse(pictureShownBesideCard(true, "card", 2, "card", 2))
        assertFalse(pictureShownBesideCard(true, "card", 0, "card", 0))

        assertTrue(pictureShownBesideCard(true, null, 0, "card", 3, selectorOpen = true))
        assertFalse(pictureShownBesideCard(true, null, 0, "card", 1, selectorOpen = true))
        assertFalse(pictureShownBesideCard(false, null, 0, "card", 3, selectorOpen = true))
        assertFalse(pictureShownBesideCard(true, null, 0, null, 3, selectorOpen = true))
    }

    @Test
    fun islandHeightBounds() {
        assertEquals(200.dp, pictureIslandHeight(120.dp))
        assertEquals(230.dp, pictureIslandHeight(230.dp))
        assertEquals(260.dp, pictureIslandHeight(300.dp))
    }

    @Test
    fun besideIslandOpenSelectorSizes() {
        assertFalse(stepsOpenBesideCard(CardSteps.First))
        assertFalse(stepsOpenBesideCard(CardSteps(preview = true)))
        assertFalse(stepsOpenBesideCard(CardSteps(reader = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, selector = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, pictureIndex = 0, pictureHalf = true)))
        assertTrue(stepsOpenBesideCard(CardSteps(reader = true, pictureIndex = 0)))
    }

    @Test
    fun dragRunsUpOpenDownClose() {
        assertEquals(0f, besideDragSplitProgress(opening = true, dragProgress = 0f), 0f)
        assertEquals(0.3f, besideDragSplitProgress(opening = true, dragProgress = 0.3f), 0.0001f)
        assertEquals(1f, besideDragSplitProgress(opening = false, dragProgress = 0f), 0f)
        assertEquals(0.7f, besideDragSplitProgress(opening = false, dragProgress = 0.3f), 0.0001f)
        assertEquals(0f, besideDragSplitProgress(opening = false, dragProgress = 1.4f), 0f)
    }

    @Test
    fun arrowsSitOnTheSideAwayFromTheCamera() {
        // pickerOnLeft = camera on the right, so the picture island (and its arrows) are on the left
        assertEquals(PictureArrowSide.Left, pictureArrowSide(isLandscape = true, pickerOnLeft = true))
        assertEquals(PictureArrowSide.Right, pictureArrowSide(isLandscape = true, pickerOnLeft = false))
        assertEquals(PictureArrowSide.Inside, pictureArrowSide(isLandscape = false, pickerOnLeft = false))
    }
}
