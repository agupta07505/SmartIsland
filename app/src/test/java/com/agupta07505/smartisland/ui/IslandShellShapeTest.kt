/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shell shape built in the draw phase keeps the requested corner radius and square top. */
class IslandShellShapeTest {
    private val density = Density(1f)

    private fun rounded(radiusPx: Float, squareTop: Boolean, w: Float, h: Float): Outline.Rounded {
        val outline = islandShellShape(radiusPx, squareTop).createOutline(Size(w, h), LayoutDirection.Ltr, density)
        assertTrue(outline is Outline.Rounded)
        return outline as Outline.Rounded
    }

    @Test
    fun allCornersEqualWhenNotSquareTop() {
        val r = rounded(radiusPx = 10f, squareTop = false, w = 112f, h = 34f).roundRect
        assertEquals(10f, r.topLeftCornerRadius.x, 0.001f)
        assertEquals(10f, r.topRightCornerRadius.x, 0.001f)
        assertEquals(10f, r.bottomLeftCornerRadius.x, 0.001f)
        assertEquals(10f, r.bottomRightCornerRadius.x, 0.001f)
    }

    @Test
    fun squareTopKeepsTopSquareAndBottomRound() {
        val r = rounded(radiusPx = 22f, squareTop = true, w = 112f, h = 34f).roundRect
        assertEquals(0f, r.topLeftCornerRadius.x, 0.001f)
        assertEquals(0f, r.topRightCornerRadius.x, 0.001f)
        // Not squeezed to half the height: only the bottom corners share the 34 px.
        assertEquals(22f, r.bottomLeftCornerRadius.x, 0.001f)
        assertEquals(22f, r.bottomRightCornerRadius.x, 0.001f)
    }

    @Test
    fun aRadiusLargerThanHalfTheHeightIsScaledDownLikeBefore() {
        val r = rounded(radiusPx = 22f, squareTop = false, w = 112f, h = 34f).roundRect
        assertEquals(17f, r.topLeftCornerRadius.x, 0.001f)
    }

    @Test
    fun negativeRadiusCountsAsZero() {
        val outline = islandShellShape(-4f, squareTop = false).createOutline(Size(50f, 30f), LayoutDirection.Ltr, density)
        // All corners zero: the shape reports a plain rectangle.
        assertTrue(outline is Outline.Rectangle)
    }
}
