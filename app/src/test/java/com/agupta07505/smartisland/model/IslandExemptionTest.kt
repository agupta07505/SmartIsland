package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandExemptModeTest {
    private val off = setOf("a.off")
    private val stat = setOf("a.static")
    private val land = setOf("a.land", "a.static")

    private fun mode(pkg: String?, landscape: Boolean) = IslandExemption.modeFor(pkg, off, stat, land, landscape)

    @Test fun portraitOffAndStatic() {
        assertEquals(ExemptMode.Off, mode("a.off", false))
        assertEquals(ExemptMode.Static, mode("a.static", false))
        assertEquals(ExemptMode.None, mode("a.land", false))
    }

    @Test fun landscapeNeverStatic() {
        assertEquals(ExemptMode.Off, mode("a.land", true))
        assertEquals(ExemptMode.Off, mode("a.static", true))
        assertEquals(ExemptMode.None, mode("a.off", true))
    }

    @Test fun noForeground() {
        assertEquals(ExemptMode.None, mode(null, false))
        assertEquals(ExemptMode.None, mode("", true))
    }
}
