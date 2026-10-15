/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.view.View
import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationViewInspectorVisibilityTest {
    private fun view(visibility: Int = View.VISIBLE, alpha: Float = 1f, parent: ViewGroup? = null): View {
        val v = mockk<View>()
        every { v.visibility } returns visibility
        every { v.alpha } returns alpha
        every { v.parent } returns parent
        return v
    }

    private fun group(visibility: Int = View.VISIBLE): ViewGroup {
        val g = mockk<ViewGroup>()
        every { g.visibility } returns visibility
        every { g.alpha } returns 1f
        every { g.parent } returns null
        return g
    }

    @Test
    fun aVisibleViewIsDrawn() {
        assertTrue(NotificationViewInspector.isDrawn(view()))
    }

    @Test
    fun goneAndInvisibleViewsAreNotDrawn() {
        assertFalse(NotificationViewInspector.isDrawn(view(visibility = View.GONE)))
        assertFalse(NotificationViewInspector.isDrawn(view(visibility = View.INVISIBLE)))
    }

    @Test
    fun aTransparentViewIsNotDrawn() {
        assertFalse(NotificationViewInspector.isDrawn(view(alpha = 0f)))
    }

    @Test
    fun aViewInsideAHiddenParentIsNotDrawn() {
        assertFalse(NotificationViewInspector.isDrawn(view(parent = group(View.GONE))))
        assertTrue(NotificationViewInspector.isDrawn(view(parent = group())))
    }
}
