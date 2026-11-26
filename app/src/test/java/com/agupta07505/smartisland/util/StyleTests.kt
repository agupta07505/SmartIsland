/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.data.OrientationState
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.forOrientation
import com.agupta07505.smartisland.model.MergeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFontsTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun fontSignatures() {
        assertEquals("ttf", CustomFonts.sniffExtension(bytes(0x00, 0x01, 0x00, 0x00, 0x00)))
        assertEquals("ttf", CustomFonts.sniffExtension("true".toByteArray()))
        assertEquals("otf", CustomFonts.sniffExtension("OTTO".toByteArray()))

        assertNull(CustomFonts.sniffExtension(bytes()))
        assertNull(CustomFonts.sniffExtension(bytes(0x00, 0x01, 0x00)))
        assertNull(CustomFonts.sniffExtension("%PDF-1.7".toByteArray()))
        assertNull(CustomFonts.sniffExtension("wOFF".toByteArray()))
        assertNull(CustomFonts.sniffExtension("ttcf".toByteArray()))
    }

    @Test
    fun fontFileNames() {
        assertEquals("My Font.ttf", CustomFonts.safeFileName("My Font.otf", "ttf"))
        assertEquals("Roboto-Bold.otf", CustomFonts.safeFileName("Roboto-Bold.ttf", "otf"))
        assertEquals("Custom font.ttf", CustomFonts.safeFileName(null, "ttf"))
        assertEquals("Custom font.ttf", CustomFonts.safeFileName("...", "ttf"))
        assertEquals("a_b_c.ttf", CustomFonts.safeFileName("a/b\\c.ttf", "ttf"))
        assertTrue(CustomFonts.safeFileName("x".repeat(300) + ".ttf", "ttf").length <= 64)
        assertEquals("Caf.otf", CustomFonts.safeFileName("Caf\u00e9.ttf", "otf"))

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

class GitHubApiServiceTest {

    @Test
    fun versionComparesMajorMinorPatch() {
        assertTrue(GitHubApiService.isNewerVersion("v6.0.0", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("6.0", "5.2.1"))
        assertTrue(GitHubApiService.isNewerVersion("v5.1.0", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("5.2", "5.1.9"))
        assertTrue(GitHubApiService.isNewerVersion("v5.0.1", "5.0.0"))
        assertTrue(GitHubApiService.isNewerVersion("5.0.2", "5.0.1"))
        assertFalse(GitHubApiService.isNewerVersion("v5.0.0", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("5.0", "5.0"))
        assertFalse(GitHubApiService.isNewerVersion("v4.9.9", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("5.0.0", "5.0.1"))
        assertFalse(GitHubApiService.isNewerVersion("4.0", "5.0"))
        assertTrue(GitHubApiService.isNewerVersion("v5.1.0-beta", "5.0.0"))
        assertFalse(GitHubApiService.isNewerVersion("v5.0.0-rc1", "5.0.0"))
    }

}

class MergeStyleTest {
    @Test
    fun unknownIdsFallBackToSlide() {
        assertEquals(MergeStyle.SLIDE, MergeStyle.fromId(null))
        assertEquals(MergeStyle.SLIDE, MergeStyle.fromId("jelly"))
        assertEquals(MergeStyle.BUBBLE, MergeStyle.fromId("bubble"))
    }

    @Test
    fun mergesShortBubbleLonger() {
        assertEquals(420, MergeStyle.SLIDE.baseDurationMs)
        assertEquals(900, MergeStyle.BUBBLE.baseDurationMs)
        assertTrue(MergeStyle.BUBBLE.baseDurationMs > MergeStyle.SLIDE.baseDurationMs)
    }

    @Test
    fun mergeStyleBackup() {
        val restoredCustom = SmartIslandSettings(mergeStyle = MergeStyle.ID_BUBBLE)
        val restoredRestored = SmartIslandSettings.fromJson(restoredCustom.toJson(appVersion = "1"))
        assertEquals(MergeStyle.ID_BUBBLE, restoredRestored.mergeStyle)

        val missingBad = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "wobble"}}""")
        assertEquals(MergeStyle.ID_SLIDE, missingBad.mergeStyle)
        val missingOld = SmartIslandSettings.fromJson("""{"settings": {"enableSwipeActions": true}}""")
        assertEquals(MergeStyle.ID_SLIDE, missingOld.mergeStyle)
        assertEquals(MergeStyle.ID_SLIDE, missingOld.mergeStyleLandscape)

        val landscapeCustom = SmartIslandSettings(mergeStyle = MergeStyle.ID_SLIDE, mergeStyleLandscape = MergeStyle.ID_BUBBLE)
        val landscapeRestored = SmartIslandSettings.fromJson(landscapeCustom.toJson(appVersion = "1"))
        assertEquals(MergeStyle.ID_SLIDE, landscapeRestored.mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, landscapeRestored.mergeStyleLandscape)
        assertEquals(MergeStyle.SLIDE, landscapeRestored.mergeStyleKind(landscape = false))
        assertEquals(MergeStyle.BUBBLE, landscapeRestored.mergeStyleKind(landscape = true))

        val legacyOld = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "bubble"}}""")
        assertEquals(MergeStyle.ID_BUBBLE, legacyOld.mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, legacyOld.mergeStyleLandscape)

        val invalidBad = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "bubble", "mergeStyleLandscape": "wobble"}}""")
        assertEquals(MergeStyle.ID_BUBBLE, invalidBad.mergeStyleLandscape)
        val both = SmartIslandSettings.fromJson("""{"settings": {"mergeStyle": "x", "mergeStyleLandscape": "y"}}""")
        assertEquals(MergeStyle.ID_SLIDE, both.mergeStyleLandscape)
    }

    @Test
    fun overlayReadsOrientationChoice() {
        val s = SmartIslandSettings(mergeStyle = MergeStyle.ID_SLIDE, mergeStyleLandscape = MergeStyle.ID_BUBBLE)
        val landscape = OrientationState(isLandscape = true, screenWidthDp = 800f)
        assertEquals(MergeStyle.ID_SLIDE, s.forOrientation(OrientationState()).mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, s.forOrientation(landscape).mergeStyle)
        assertEquals(MergeStyle.ID_BUBBLE, s.forOrientation(landscape, hasContent = true).mergeStyle)
    }
}
