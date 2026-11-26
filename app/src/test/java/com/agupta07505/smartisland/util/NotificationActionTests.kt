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

class SilentActionGuardTest {
    @Test
    fun actionConsumption() {
        SilentActionGuard.arm("rec", now = 1000)
        assertTrue(SilentActionGuard.consume("rec", now = 1500))
        assertFalse(SilentActionGuard.consume("rec", now = 1600))

        SilentActionGuard.arm("rec", now = 1000)
        assertFalse(SilentActionGuard.consume("other", now = 1100))
        assertTrue(SilentActionGuard.consume("rec", now = 1200))

        SilentActionGuard.arm("rec", now = 1000)
        assertFalse(SilentActionGuard.consume("rec", now = 1000 + SilentActionGuard.WINDOW_MS + 1))

        SilentActionGuard.arm("rec", now = 1000)
        SilentActionGuard.disarm()
        assertFalse(SilentActionGuard.consume("rec", now = 1100))
    }
}

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
    fun effectiveVisibility() {
        assertTrue(NotificationViewInspector.isDrawn(view()))

        assertFalse(NotificationViewInspector.isDrawn(view(visibility = View.GONE)))
        assertFalse(NotificationViewInspector.isDrawn(view(visibility = View.INVISIBLE)))

        assertFalse(NotificationViewInspector.isDrawn(view(alpha = 0f)))

        assertFalse(NotificationViewInspector.isDrawn(view(parent = group(View.GONE))))
        assertTrue(NotificationViewInspector.isDrawn(view(parent = group())))
    }
}
