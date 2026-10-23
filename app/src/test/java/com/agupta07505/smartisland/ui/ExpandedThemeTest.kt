/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.graphics.Color
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.expanded.ExpandedThemeRules
import com.agupta07505.smartisland.ui.expanded.IslandPalette
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush
import com.agupta07505.smartisland.ui.expanded.expandedSurfaceColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedThemeTest {
    @Test
    fun darkIsTheDefault() {
        assertEquals(SmartIslandSettings.EXPANDED_THEME_DARK, SmartIslandSettings.Default.expandedTheme)
    }

    @Test
    fun darkAndLightAreFixed() {
        assertFalse(ExpandedThemeRules.isLight("dark", systemDark = false))
        assertFalse(ExpandedThemeRules.isLight("dark", systemDark = true))
        assertTrue(ExpandedThemeRules.isLight("light", systemDark = false))
        assertTrue(ExpandedThemeRules.isLight("light", systemDark = true))
    }

    @Test
    fun systemFollowsThePhone() {
        assertTrue(ExpandedThemeRules.isLight("system", systemDark = false))
        assertFalse(ExpandedThemeRules.isLight("system", systemDark = true))
    }

    @Test
    fun anUnknownValueFallsBackToDark() {
        assertFalse(ExpandedThemeRules.isLight("sepia", systemDark = false))
    }

    @Test
    fun theDarkPaletteKeepsTheOriginalColours() {
        val p = IslandPalette.Dark
        assertEquals(Color.White, p.text)
        assertEquals(Color(0xFFD5DAE0), p.textSecondary)
        assertEquals(Color(0xFFB7C0CA), p.textTertiary)
        assertEquals(Color.White.copy(alpha = 0.2f), p.tint(0.2f))
        assertEquals(Color(0xFF222222), p.pick(Color(0xFF222222), Color.Red))
    }

    @Test
    fun lightTextIsReadableOnTheLightSurface() {
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
    fun theLightIslandIsDrawnInTheSurfaceNotThePillColour() {
        val settings = SmartIslandSettings.Default.copy(pillColor = 0xFF123456L)
        assertEquals(Color(0xFF123456L), expandedSurfaceColor(settings, IslandPalette.Dark))
        assertEquals(IslandPalette.Light.surface, expandedSurfaceColor(settings, IslandPalette.Light))
    }

    @Test
    fun theGradientStillWorksInLight() {
        val settings = SmartIslandSettings.Default.copy(expandedGradientEnabled = true)
        assertNotNull(expandedGradientBrush(settings, IslandPalette.Light))
    }

    @Test
    fun onlyThreeThemesAreValid() {
        assertEquals(setOf("dark", "light", "system"), SmartIslandSettings.VALID_EXPANDED_THEMES)
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
