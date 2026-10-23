/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.MediaExpansion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaExpansionTest {
    @Test
    fun openOnlyWhenACardKeyIsSet() {
        assertFalse(MediaExpansion(null, 0) { _, _ -> }.isOpen)
        assertTrue(MediaExpansion("card", 1) { _, _ -> }.isOpen)
    }

    @Test
    fun changeReportsKeyAndIndex() {
        var seen: Pair<String?, Int>? = null
        val expansion = MediaExpansion(null, 0) { key, index -> seen = key to index }
        expansion.onChange("card", 2)
        assertEquals("card" to 2, seen)
        expansion.onChange(null, 0)
        assertEquals(null to 0, seen)
    }
}
