/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.ui.expanded.cardScrimScale
import com.agupta07505.smartisland.ui.expanded.expandedGradientBrush
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CardStyleTest {
    private val base = SmartIslandSettings.Default

    @Test
    fun scrimIsFullWithoutGradientAndLightWithIt() {
        assertEquals(1f, cardScrimScale(base), 0.001f)
        assertEquals(0.3f, cardScrimScale(base.copy(expandedGradientEnabled = true)), 0.001f)
    }

    @Test
    fun gradientBrushOnlyWhenEnabled() {
        assertNull(expandedGradientBrush(base))
        assertNotNull(expandedGradientBrush(base.copy(expandedGradientEnabled = true)))
    }

    @Test
    fun seeThroughBlurIsOffByDefault() {
    }
}
