/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuMethodCheckTest {
    @Test
    fun aProbePassesOnlyWhenItPrintsTheMarker() {
        assertTrue(ShizukuMethodCheck.probePassed("SI_OK\n"))
        assertFalse(ShizukuMethodCheck.probePassed(""))
        assertFalse(ShizukuMethodCheck.probePassed(null))
        assertFalse(ShizukuMethodCheck.probePassed("not found"))
    }

    @Test
    fun everyMethodHasAProbeExceptTheHotspotWhichIsTestedForReal() {
        ShizukuMethodCheck.Method.entries.forEach { method ->
            if (method == ShizukuMethodCheck.Method.Hotspot) {
                assertNull(ShizukuMethodCheck.probeCommand(method))
            } else {
                val command = ShizukuMethodCheck.probeCommand(method)
                assertNotNull(command)
                assertTrue(command!!.contains(ShizukuMethodCheck.OK_MARKER))
            }
        }
    }

    @Test
    fun methodIdsAreUnique() {
        val ids = ShizukuMethodCheck.Method.entries.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}
