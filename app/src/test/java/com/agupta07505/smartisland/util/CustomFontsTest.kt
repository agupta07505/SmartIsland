/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFontsTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun sniffsTrueTypeAndOpenTypeHeaders() {
        assertEquals("ttf", CustomFonts.sniffExtension(bytes(0x00, 0x01, 0x00, 0x00, 0x00)))
        assertEquals("ttf", CustomFonts.sniffExtension("true".toByteArray()))
        assertEquals("otf", CustomFonts.sniffExtension("OTTO".toByteArray()))
    }

    @Test
    fun rejectsEverythingElse() {
        assertNull(CustomFonts.sniffExtension(bytes()))
        assertNull(CustomFonts.sniffExtension(bytes(0x00, 0x01, 0x00)))
        assertNull(CustomFonts.sniffExtension("%PDF-1.7".toByteArray()))
        assertNull(CustomFonts.sniffExtension("wOFF".toByteArray()))
        assertNull(CustomFonts.sniffExtension("ttcf".toByteArray()))
    }

    @Test
    fun fileNameFromDisplayNameIsSafeAndKeepsTheRealFormat() {
        assertEquals("My Font.ttf", CustomFonts.safeFileName("My Font.otf", "ttf"))
        assertEquals("Roboto-Bold.otf", CustomFonts.safeFileName("Roboto-Bold.ttf", "otf"))
        assertEquals("Custom font.ttf", CustomFonts.safeFileName(null, "ttf"))
        assertEquals("Custom font.ttf", CustomFonts.safeFileName("...", "ttf"))
        assertEquals("a_b_c.ttf", CustomFonts.safeFileName("a/b\\c.ttf", "ttf"))
        assertTrue(CustomFonts.safeFileName("x".repeat(300) + ".ttf", "ttf").length <= 64)
        assertEquals("Caf.otf", CustomFonts.safeFileName("Caf\u00e9.ttf", "otf"))
    }

    @Test
    fun everyGeneratedNameIsAcceptedAsAnId() {
        listOf("My Font.otf", "a/b.ttf", "../../etc/passwd", "...", "\u5b57\u4f53.ttf", "x".repeat(300)).forEach { display ->
            val name = CustomFonts.safeFileName(display, "ttf")
            assertEquals(name, CustomFonts.fileNameOf(CustomFonts.idFor(name)))
        }
    }

    @Test
    fun uniqueNameAddsACounter() {
        assertEquals("A.ttf", CustomFonts.uniqueFileName("A.ttf", emptySet()))
        assertEquals("A (2).ttf", CustomFonts.uniqueFileName("A.ttf", setOf("A.ttf")))
        assertEquals("A (3).ttf", CustomFonts.uniqueFileName("A.ttf", setOf("A.ttf", "A (2).ttf")))
    }

    @Test
    fun idsMustBeWellFormed() {
        assertEquals("My Font.ttf", CustomFonts.fileNameOf("custom:My Font.ttf"))
        assertNull(CustomFonts.fileNameOf("lora"))
        assertNull(CustomFonts.fileNameOf("custom:"))
        assertNull(CustomFonts.fileNameOf("custom:../secret.ttf"))
        assertNull(CustomFonts.fileNameOf("custom:dir/font.ttf"))
        assertNull(CustomFonts.fileNameOf("custom:font.woff"))
        assertNull(CustomFonts.fileNameOf("custom:..ttf"))
        assertTrue(CustomFonts.isCustomId("custom:x.ttf"))
        assertFalse(CustomFonts.isCustomId("poppins"))
    }

    @Test
    fun labelDropsTheExtension() {
        assertEquals("My Font", CustomFonts.labelOf("My Font.ttf"))
        assertEquals("a.b", CustomFonts.labelOf("a.b.otf"))
    }
}
